(ns iron
  (:require
   [clojure.contrib.json.read :as json])
  (:import
   (java.io File)
   (java.awt AWTException GridLayout SystemTray TrayIcon)
   (java.awt.event MouseEvent MouseListener)
   (javax.imageio ImageIO)
   (javax.swing JFrame JLabel JTextField)
   (javax.swing.event DocumentListener)))

;; TODO: Circular dependency hack.
(def update-results)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Search agent
;;
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

(defn search-init [state display tumblr-filepath]
  {:tumblr (read-json-file tumblr-filepath)
   :display display})

(defn containing-text [text collection]
  (filter #(and (= (% "type") "regular")
                (.contains (% "regular-title") text))
          collection))

(defn query [state text]
  (when (:tumblr state)
    (let [results (containing-text text (:tumblr state))]
      (when (not (empty? results))
        (send (:display state) update-results ((first results) "regular-title")))))
  state)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Display agent
;;
(defn document-listener [field func]
  (.addDocumentListener
   (.getDocument field)
   (proxy [DocumentListener] []
     (changedUpdate [e] (func :changed e))
     (insertUpdate [e] (func :insert e))
     (removeUpdate [e] (func :remove e)))))

(defn document-text [doc]
  (.getText doc 0 (.getLength doc)))

(defn toggle-visible [frame]
  (.setVisible frame (not (.isVisible frame))))

(defn display-init [_ search]
  (let [frame (JFrame. "Iron")
        label (JLabel. "Hello world")
        field (JTextField. 20)
        state {:frame frame :label label :search search}]
    (document-listener field
      (fn [_ e]
        (send (:search state) query (document-text (.getDocument e)))))
    (doto frame
      (.setLayout (GridLayout.))
      (.add field)
      (.add label)
      (.pack)
      (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
      (.setVisible true))
    state))

(defn update-results [state text]
  (.setText (:label state) text)
  state)

(defn toggle-display [state]
  (toggle-visible (:frame state))
  state)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; System tray agent
;;
(defn add-mouse-listener [object func]
  (.addMouseListener
   object
   (proxy [MouseListener] []
     (mouseClicked  [e] (func :clicked e))
     (mouseEntered  [e] (func :entered e))
     (mouseExited   [e] (func :exited e))
     (mousePressed  [e] (func :pressed e))
     (mouseReleased [e] (func :released e)))))
(defn on-left-mouse-button-clicked [object func]
  (add-mouse-listener object (fn [type e] (if (and (= type :clicked) (= (.getButton e) MouseEvent/BUTTON1)) (func)))))

(defn tray-init [state display]
  (if (SystemTray/isSupported)
    (let [tray (SystemTray/getSystemTray)
          image (ImageIO/read (File. "logo.png"))
          icon (TrayIcon. image "Tip text")]
      (on-left-mouse-button-clicked icon #(send display toggle-display))
      (try
       (.add tray icon)
       (catch AWTException e
         (println "Unable to add to system tray: " + e))))
    (println "No system tray."))
  state)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Main
;;
(defn main
  ([progname]
     (printf "Usage: %s tumblr-json-file%n" progname) (flush)
     (System/exit 0))
  ([_ tumblr-filepath]
     (let [search-agent (agent {})
           display-agent (agent {})
           tray-agent (agent {})]
       (send display-agent display-init search-agent)
       (send search-agent search-init display-agent tumblr-filepath)
       (send tray-agent tray-init display-agent))))

;; Hack until clojure allows to differentiate between running a file as
;; a script and loading it from another module.
(if (= "iron.clj" (first *command-line-args*))
  (apply main *command-line-args*))
