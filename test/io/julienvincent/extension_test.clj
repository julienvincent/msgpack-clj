(ns io.julienvincent.extension-test
  (:require
   [clojure.test :refer [deftest is]]
   [io.julienvincent.msgpack :as msgpack]
   [io.julienvincent.msgpack.extension.clojure :as extension.clojure]))

(deftest pack-unpack-keyword
  (let [opts {:extensions extension.clojure/extensions}]
    (is (= :a (msgpack/unpack (msgpack/pack :a opts) opts)))))

(deftest pack-unpack-namespaced-keyword
  (let [opts {:extensions extension.clojure/extensions}]
    (is (= :a/a (msgpack/unpack (msgpack/pack :a/a opts) opts)))))

(deftest pack-unpack-symbol
  (let [opts {:extensions extension.clojure/extensions}]
    (is (= 'a (msgpack/unpack (msgpack/pack 'a opts) opts)))))

(deftest pack-unpack-set
  (let [opts {:extensions extension.clojure/extensions}]
    (is (= #{:a 1} (msgpack/unpack (msgpack/pack #{:a 1} opts) opts)))))
