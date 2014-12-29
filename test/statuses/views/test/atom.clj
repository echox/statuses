(ns statuses.views.test.atom
  (:require [clj-time.core :refer [date-time]]
            [clojure.test :refer [deftest is]]
            [statuses.views.atom :refer [feed]]))

(defn- feed-with
  "Returns a feed with the given entries"
  [entries]
  (let [base-uri "http://localhost:8080/statuses"]
    (feed entries base-uri (str base-uri "/updates?format=atom"))))

(defn- feed-with-entry
  "Creates a new feed with one entry from the given entry template."
  [entry-template]
  (feed-with [(merge
                {:id 0, :author "", :text "", :time (date-time 2000 1 1 0 0 0 0)}
                entry-template)]))

(deftest test-feed
  (let [feed (feed-with [{:time (date-time 2014 11 20 9 39 30 001)}])]
    (is (= (get feed 2)
           [:title "innoQ Status updates"])
        "feed title is: innoQ Status updates")

    (is (= (get feed 3)
           [:id "http://localhost:8080/statuses/updates?format=atom"])
        "feed id is uri of feed")

    (is (= (get feed 4)
           [:updated "2014-11-20T09:39:30.001Z"])
        "feed updated is in rfc3339 format: yyyy-MM-dd'T'hh:mm:ss.S'Z'")

    (is (= (get feed 5)
           [:link {:rel "self"
                   :href "http://localhost:8080/statuses/updates?format=atom"
                   :type "application/atom+xml"}])
        "feed self link is atom type and points to feed uri")

    (is (= (get feed 6)
           [:author
            [:name "innoQ"]
            [:uri "http://localhost:8080/statuses/updates"]])
        "feed author has name and correct uri")))

(deftest test-feed-entry
  (letfn [(entry[feed] (get feed 7))]
    (is (= (get (entry (feed-with-entry {:author "bar"})) 1)
           [:title "Posted by @bar"])
      "title is: Posted by @author")

    (is (= (get (entry (feed-with-entry {:author "bar"})) 2)
           [:author
            [:name "bar"]
            [:uri "http://localhost:8080/statuses/updates?author=bar"]])
      "author contains correct name and uri")

    (is (= (get (entry (feed-with-entry {:text "@bar: http://www.test.de"})) 3)
           [:content {:type "html"}
            "@&lt;a href=&quot;/statuses/updates?author=bar&quot;&gt;bar&lt;/a&gt;: &lt;a href=&quot;http://www.test.de&quot;&gt;http://www.test.de&lt;/a&gt;"])
      "content is linkified and escaped")

    (is (= (get (entry (feed-with-entry {:id 1337})) 4)
           [:id "tag:innoq.com,2012:statuses/1337"])
      "id is 'tag:innoq.com,2012:statuses/id'")

    (is (= (get (entry (feed-with-entry {:time (date-time 2014 11 20 9 39 30 001)})) 5)
           [:published "2014-11-20T09:39:30.001Z"])
      "published is in rfc3339 format: yyyy-MM-dd'T'hh:mm:ss.S'Z'")

    (is (= (get (entry (feed-with-entry {:time (date-time 2014 11 20 9 39 30 001)})) 6)
           [:updated "2014-11-20T09:39:30.001Z"])
      "updated is in rfc3339 format: yyyy-MM-dd'T'hh:mm:ss.S'Z'")

    (is (= (get (entry (feed-with-entry {:id 1337})) 7)
           [:link {:rel "alternate"
                   :type "text/html"
                   :href "http://localhost:8080/statuses/updates/1337"}])
      "alternate link is text/html and points to correct status")

    (is (= (get (entry (feed-with-entry {})) 8) nil)
      "no link to related if entry is not member of a conversation")

    (is (= (get (entry (feed-with-entry {:conversation 1337})) 8)
           [:link {:rel"related"
                   :type "text/html"
                   :href "http://localhost:8080/statuses/conversations/1337"}])
      "related link is text/html and points to correct conversation")

    (is (= (get (entry (feed-with-entry {})) 9) nil)
      "no link to prev if entry is no reply-to")

    (is (= (get (entry (feed-with-entry {:in-reply-to 1337})) 9)
           [:link {:rel "prev"
                   :type "text/html"
                   :href "http://localhost:8080/statuses/updates/1337"}])
      "prev link is text/html and points to correct status")))

