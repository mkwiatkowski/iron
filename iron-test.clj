(ns iron.test
  (:use
   [clojure.test :only (deftest is run-tests testing)]
   [iron :only (containing-text)]))

(defn- regular-post [title]
  {"type" "regular", "regular-title" title})

(deftest containing-text-test
  (testing "empty collection"
    (is (empty? (containing-text "foobar" []))))
  (testing "searching in attributes of a regular post"
    (doseq [attr ["url-with-slug" "regular-title" "regular-body"]]
      (let [clojure {attr "Clojure rocks!", "type" "regular"}
            tumblr {attr "My tumblr is the best.", "type" "regular"}
            haskell {attr "Haskell rocks!"}
            empty {}]
        (is (= [tumblr] (containing-text "tumblr" [clojure tumblr])))
        (is (= [clojure] (containing-text "clojure" [clojure tumblr])) "is case-insensitive by default")
        (is (= [] (containing-text "Tumblr" [clojure tumblr])) "is case-sensitive when a uppercase character appears in a query")
        (is (= [clojure haskell] (containing-text "rock" [clojure tumblr haskell])) "disregards type")
        (is (= [haskell] (containing-text "haskell" [haskell empty])) "ignores elements without regular-title")))))

(run-tests)
