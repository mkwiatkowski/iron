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

(deftest snippet-from-post-test
  (let [snippet-from-post (ns-resolve 'iron 'snippet-from-post)]
    (is (= "the post title" (snippet-from-post {"regular-title" "the post title"})) "post with regular-title")
    (is (= "<html>the post body</html>" (snippet-from-post {"regular-body" "the post body"})) "post with regular-body")
    (is (= "the-post-slug" (snippet-from-post {"url-with-slug" "http://time-loop.tumblr.com/post/12345/the-post-slug"})) "post with url-with-slug")))

(run-tests)
