(ns iron
  (:require
   (org.danlarkin [json :as json]))
  (:import
   (java.io FileReader)
   (java.awt GridLayout)
   (javax.swing JFrame JLabel JTextField)
   (javax.swing.event DocumentListener)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Search agent
;;
(def search-agent (agent {}))

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
   #(json/decode-from-reader (FileReader. filepath))))

(defn search-init [db tumblr-filepath]
  {:tumblr (read-json-file tumblr-filepath)})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Display agent
;;
(def display-agent (agent nil))

(defn document-listener [field func]
  (.addDocumentListener
   (.getDocument field)
   (proxy [DocumentListener] []
     (changedUpdate [e] (func :changed e))
     (insertUpdate [e] (func :insert e))
     (removeUpdate [e] (func :remove e)))))

(defn document-text [doc]
  (.getText doc 0 (.getLength doc)))

(defn configure-gui [frame label field]
  (document-listener field
    (fn [method e]
      (.setText label (document-text (.getDocument e)))))
  (doto frame
    (.setLayout (GridLayout.))
    (.add field)
    (.add label)
    (.pack)
    (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
    (.setVisible true)))

(defn display-init [_]
  (let [frame (JFrame. "Iron")
        label (JLabel. "Hello world")
        field (JTextField. 20)]
    (configure-gui frame label field)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Main
;;
(defn main []
  (send display-agent display-init)
  (send search-agent search-init (second *command-line-args*)))

(if *command-line-args* (main))
