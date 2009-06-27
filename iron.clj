(ns iron
  (:require
   [clojure.contrib.json.read :as json])
  (:use
   [clojure.contrib.swing-utils :only (do-swing)])
  (:import
   (java.io File)
   (java.awt AWTException SystemTray TrayIcon)
   (java.awt.event KeyEvent KeyListener MouseEvent MouseListener)
   (javax.imageio ImageIO)
   (javax.swing JFrame JLabel JTextField BoxLayout)
   (javax.swing.event DocumentListener)))

;; TODO: Circular dependency hack.
(def update-results)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Search agent
;;
(def *search-agent* (agent nil))

(defn call-and-measure [message fun]
  (printf "%s... " message) (flush)
  (let [start (. System (nanoTime))
        ret (fun)]
    (println
     (format "done in %.3f seconds."
             (/ (double (- (. System (nanoTime)) start)) 1000000000.0)))
    ret))

(defn read-json-file [filepath]
  (call-and-measure
   (format "Reading json file %s" filepath)
   #(json/read-json (slurp filepath))))

(defn containing-text [text collection]
  (filter #(and (= (% "type") "regular")
                (.contains #^String (% "regular-title") text))
          collection))

(defn search-init [state tumblr-filepath]
  {:tumblr (read-json-file tumblr-filepath)})

(defn query [state text]
  (when (:tumblr state)
    (let [results (containing-text text (:tumblr state))]
      (update-results (map #(% "regular-title") results))))
  state)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Display agent
;;
(def *max-number-of-results* 10)

(defn- add-document-listener [field func]
  (.addDocumentListener
   (.getDocument field)
   (proxy [DocumentListener] []
     (changedUpdate [e] (func :changed e))
     (insertUpdate [e] (func :insert e))
     (removeUpdate [e] (func :remove e)))))

(defn- add-key-listener [object func]
  (.addKeyListener
   object
   (proxy [KeyListener] []
     (keyPressed [e] (func :pressed e))
     (keyReleased [e] (func :released e))
     (keyTyped [e] (func :typed e)))))

(defn- on-escape-pressed [object func]
  (add-key-listener
   object
   (fn [type e] (if (and (= type :pressed)
                         (= (.getKeyCode e) KeyEvent/VK_ESCAPE)) (func)))))

(defn- document-text [doc]
  (.getText doc 0 (.getLength doc)))

(defn- toggle-visible [frame]
  (.setVisible frame (not (.isVisible frame))))

(def main-frame (ref nil))
(def result-labels (ref []))

(defn- clear-results-list []
  (doseq [label @result-labels]
    (.remove @main-frame label))
  (ref-set result-labels []))

(defn- populate-results-list [results]
  (let [labels (map #(JLabel. %) results)]
    (doseq [label labels]
      (.add @main-frame label))
    (.pack @main-frame)
    (ref-set result-labels labels)))

(defn display-init []
  (do-swing
   (let [frame (JFrame. "Iron")
         pane (.getContentPane frame)
         field (JTextField. 30)]
     (add-document-listener field
       (fn [_ e]
         (send *search-agent* query (document-text (.getDocument e)))))
     (on-escape-pressed field #(toggle-visible frame))
     (doto pane
       (.setLayout (BoxLayout. pane BoxLayout/Y_AXIS))
       (.add field))
     (doto frame
       (.setUndecorated true)
       (.pack)
       (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
       (.setLocationRelativeTo nil)
       (.setVisible true))
     (dosync
      (ref-set main-frame frame)))))

(defn update-results [results]
  (do-swing
   (dosync
    (clear-results-list)
    (populate-results-list (take *max-number-of-results* results)))))

(defn toggle-display []
  (do-swing
   (toggle-visible @main-frame)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; System tray agent
;;
(defn- add-mouse-listener [object func]
  (.addMouseListener
   object
   (proxy [MouseListener] []
     (mouseClicked  [e] (func :clicked e))
     (mouseEntered  [e] (func :entered e))
     (mouseExited   [e] (func :exited e))
     (mousePressed  [e] (func :pressed e))
     (mouseReleased [e] (func :released e)))))

(defn- on-left-mouse-button-clicked [object func]
  (add-mouse-listener
   object
   (fn [type e] (if (and (= type :clicked)
                         (= (.getButton e) MouseEvent/BUTTON1)) (func)))))

(defn tray-init []
  (do-swing
   (if (SystemTray/isSupported)
     (let [tray (SystemTray/getSystemTray)
           image (ImageIO/read (File. "logo.png"))
           icon (TrayIcon. image "Tip text")]
       (on-left-mouse-button-clicked icon toggle-display)
       (try
        (.add tray icon)
        (catch AWTException e
          (println "Unable to add to system tray: " + e))))
     (println "No system tray."))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Main
;;
(defn main
  ([progname]
     (printf "Usage: %s tumblr-json-file%n" progname) (flush)
     (System/exit 0))
  ([_ tumblr-filepath]
     (send *search-agent* search-init tumblr-filepath)
     (display-init)
     (tray-init)))

;; Hack until clojure allows to differentiate between running a file as
;; a script and loading it from another module.
(if (= "iron.clj" (first *command-line-args*))
  (apply main *command-line-args*))
