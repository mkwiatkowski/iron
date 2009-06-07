(ns iron
  (:import
   (javax.swing JFrame JLabel JTextField)
   (java.awt GridLayout)
   (java.awt.event ActionListener))
  (:use clojure.contrib.import-static))

(import-static javax.accessibility.AccessibleText SENTENCE)

(defn action-listener [obj func]
  (.addActionListener
    obj (proxy [ActionListener] []
          (actionPerformed [evt] (func evt)))))

(defn configure-gui [frame label field]
  (action-listener field
    (fn [e]
      (.setText label (.getActionCommand e))))
  (doto frame
    (.setLayout (GridLayout.))
    (.add field)
    (.add label)
    (.pack)
    (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
    (.setVisible true)))

(defn main []
  (let [frame (JFrame. "Snake")
        label (JLabel. "Hello world")
        field (JTextField. 20)]
    (configure-gui frame label field)))

(if *command-line-args* (main))
