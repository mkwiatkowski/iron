(ns iron
  (:require
   (org.danlarkin [json :as json]))
  (:import
   (java.io FileReader)
   (java.awt GridLayout)
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

(defn starts-with? [#^String str substr]
  (.startsWith str substr))

(defn read-json-file [filepath]
  (call-and-measure
   (format "Reading json file %s" filepath)
   #(json/decode-from-reader (FileReader. filepath))))

(defn search-init [state display tumblr-filepath]
  {:tumblr (read-json-file tumblr-filepath)
   :display display})

(defn query [state text]
  (when (:tumblr state)
    (let [results (filter #(and (= (:type %) "regular") (starts-with? (:regular-title %) text))
                          (:tumblr state))]
      (when (not (empty? results))
        (send (:display state) update-results (:regular-title (first results))))))
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

(defn configure-gui [state frame label field]
  (document-listener field
    (fn [method e]
      (send (:search state) query (document-text (.getDocument e)))))
  (doto frame
    (.setLayout (GridLayout.))
    (.add field)
    (.add label)
    (.pack)
    (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
    (.setVisible true)))

(defn display-init [state search]
  (let [frame (JFrame. "Iron")
        label (JLabel. "Hello world")
        field (JTextField. 20)
        state {:label label :search search}]
    (configure-gui state frame label field)
    state))

(defn update-results [state text]
  (.setText (:label state) text)
  state)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Main
;;
(defn main []
  (let [search-agent (agent {})
        display-agent (agent {})]
    (send display-agent display-init search-agent)
    (send search-agent search-init display-agent (second *command-line-args*))))

(if *command-line-args* (main))
