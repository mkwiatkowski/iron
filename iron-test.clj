(ns iron.test
  (:use
   [clojure.test :only (deftest is run-tests testing)]
   [iron :only (containing-text)]))

(defn- regular-post [title]
  {"type" "regular", "regular-title" title})

(deftest containing-text-test
  (testing "empty collection"
    (is (empty? (containing-text "foobar" []))))
  (testing "searching in regular-title"
    (let [clojure (regular-post "Clojure rocks!")
          tumblr (regular-post "My tumblr is the best.")]
      (is (= [tumblr] (containing-text "tumblr" [clojure tumblr])))
      (is (= [clojure] (containing-text "clojure" [clojure tumblr])) "is case-insensitive by default")
      (is (= [] (containing-text "Tumblr" [clojure tumblr])) "is case-sensitive when a uppercase character appears in a query"))))

(run-tests)
