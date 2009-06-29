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
  (let [snippet-from-post (ns-resolve 'iron 'snippet-from-post)
        title "the post title"
        expected-title title
        body "the post body"
        expected-body (format "<html>%s</html>" body)
        slug "http://time-loop.tumblr.com/post/12345/the-post-slug"
        expected-slug "the-post-slug"]
    (is (= expected-title (snippet-from-post {"regular-title" title})) "post with regular-title")
    (is (= expected-body (snippet-from-post {"regular-body" body})) "post with regular-body")
    (is (= expected-slug (snippet-from-post {"url-with-slug" slug})) "post with url-with-slug")
    (is (= expected-title (snippet-from-post {"regular-title" title, "regular-body" body, "url-with-slug" slug})) "prefers regular-title over anything")
    (is (= expected-body (snippet-from-post {"regular-title" "", "regular-body" body, "url-with-slug" slug})) "prefers regular-body over url-with-slug")
    (is (= expected-slug (snippet-from-post {"regular-body" "", "regular-body" "", "url-with-slug" slug})) "uses url-with-slug when everything else is empty")))

(run-tests)
