(ns io.julienvincent.msgpack-test
  (:require
   [clojure.test :refer [deftest is]]
   [io.julienvincent.msgpack :as msgpack])
  (:import
   java.io.PipedInputStream
   java.io.PipedOutputStream))

(deftest pack-unpack-int
  (let [packed (msgpack/pack (int 1))]
    (is (= [1] (vec packed)))
    (is (= 1 (msgpack/unpack packed)))
    (is (instance? Integer (msgpack/unpack packed)))))

(deftest pack-unpack-long
  (let [packed (msgpack/pack 100000)]
    (is (= [-50 0 1 -122 -96] (vec packed)))
    (is (= 100000 (msgpack/unpack packed)))
    (is (instance? Long (msgpack/unpack packed)))))

(deftest pack-unpack-bigint
  (let [packed (msgpack/pack (java.math.BigInteger/valueOf 1000000000000))]
    (is (= [-49 0 0 0 -24 -44 -91 16 0] (vec packed)))
    (is (= 1000000000000 (msgpack/unpack packed)))
    (is (instance? java.math.BigInteger (msgpack/unpack packed)))))

(deftest pack-unpack-string
  (let [packed (msgpack/pack "abc")]
    (is (= "abc" (msgpack/unpack packed)))))

(deftest pack-unpack-boolean
  (is (= true (msgpack/unpack (msgpack/pack true))))
  (is (= false (msgpack/unpack (msgpack/pack false)))))

(deftest pack-unpack-vector
  (is (= [] (msgpack/unpack (msgpack/pack []))))
  (is (= [1] (msgpack/unpack (msgpack/pack [1])))))

(deftest pack-unpack-sequential
  (is (= [] (msgpack/unpack (msgpack/pack '()))))
  (is (= [1] (msgpack/unpack (msgpack/pack '(1))))))

(deftest pack-unpack-set
  (is (= [] (msgpack/unpack (msgpack/pack #{}))))
  (is (= [1] (msgpack/unpack (msgpack/pack #{1})))))

(deftest pack-unpack-map
  (is (= {} (msgpack/unpack (msgpack/pack {}))))
  (is (= {"a" 1} (msgpack/unpack (msgpack/pack {"a" 1})))))

(deftest pack-unpack-binary
  (let [value (msgpack/unpack (msgpack/pack (byte-array 0)))]
    (is (bytes? value))
    (is (= [] (vec value))))
  (is (= [1 2 3] (vec (msgpack/unpack (msgpack/pack (byte-array [1 2 3])))))))

(deftest stringify-map-keys
  (is (= {"a" 1} (msgpack/unpack (msgpack/pack {:a 1})))))

(deftest keywordize-map-keys
  (is (= {:a 1} (msgpack/unpack (msgpack/pack {:a 1})
                                {:key-fn msgpack/keywordize-keys-mapper}))))

(deftest pack-unpack-stream
  (let [source (PipedInputStream.)
        sink (PipedOutputStream. source)]
    (msgpack/pack-stream sink {:a 1})
    (is {"a" 1} (msgpack/unpack source))))
