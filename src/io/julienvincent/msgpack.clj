(ns io.julienvincent.msgpack
  (:import
   clojure.lang.PersistentArrayMap
   java.io.InputStream
   java.io.OutputStream
   org.msgpack.core.MessageBufferPacker
   org.msgpack.core.MessageFormat
   org.msgpack.core.MessagePack
   org.msgpack.core.MessagePacker
   org.msgpack.core.MessageUnpacker
   org.msgpack.value.ValueType))

(set! *warn-on-reflection* true)

(declare ^:private pack-)

(defn- pack-map [packer ^PersistentArrayMap value opts]
  (MessagePacker/.packMapHeader packer (count value))
  (let [key-fn (:key-fn opts)]
    (reduce-kv (fn [_ key value]
                 (pack- packer (cond-> key key-fn key-fn) opts)
                 (pack- packer value opts)
                 nil)
               nil
               value)))

(defn- pack-vector [packer value opts]
  (let [len (count value)]
    (MessagePacker/.packArrayHeader packer len)
    (dotimes [i len]
      ;; Using the vector-as-a-function accessor is one of the fastest ways of
      ;; accessing a value from a vector. It is constant time and has very
      ;; little overhead.
      (pack- packer (value i) opts))))

(defn- pack-bytes [packer ^bytes value]
  (let [len (alength value)]
    (MessagePacker/.packBinaryHeader packer len)
    (MessagePacker/.writePayload packer value)))

(defn- pack-sequential [packer value opts]
  (MessagePacker/.packArrayHeader packer (count value))
  (doseq [element value]
    (pack- packer element opts)))

(defn- pack- [packer value opts]
  (cond
    (instance? Integer value) (MessagePacker/.packInt packer value)
    (instance? Long value) (MessagePacker/.packLong packer value)
    (instance? java.math.BigInteger value) (MessagePacker/.packBigInteger packer value)
    (float? value) (MessagePacker/.packFloat packer value)
    (string? value) (MessagePacker/.packString packer value)
    (boolean? value) (MessagePacker/.packBoolean packer value)
    (nil? value) (MessagePacker/.packNil packer)

    (map? value) (pack-map packer value opts)
    (vector? value) (pack-vector packer value opts)
    (bytes? value) (pack-bytes packer value)

    (or (sequential? value)
        (set? value)) (pack-sequential packer value opts)

    :else (throw (ex-info "Unsupported datatype" {:type (type value)}))))

(defn stringify-keys-mapper [k]
  (if (keyword? k)
    (name k)
    k))

(defn keywordize-keys-mapper [k]
  (keyword k))

(def ^:private default-pack-opts
  {:key-fn stringify-keys-mapper})

(defn pack
  ([value] (pack value {}))
  ([value opts]
   (let [packer (MessagePack/newDefaultBufferPacker)
         opts (merge default-pack-opts opts)]
     (pack- packer value opts)
     (MessageBufferPacker/.toByteArray packer))))

(defn pack-stream
  ([^OutputStream stream value] (pack-stream stream value {}))
  ([^OutputStream stream value opts]
   (let [packer (MessagePack/newDefaultPacker stream)
         opts (merge default-pack-opts opts)]
     (pack- packer value opts)
     (MessagePacker/.flush packer)
     nil)))

(declare ^:private -unpack)

(defn- unpack-array [unpacker opts]
  (let [length (MessageUnpacker/.unpackArrayHeader unpacker)]
    (loop [arr (transient [])
           i 0]
      (if (< i length)
        (recur (conj! arr (-unpack unpacker opts))
               (inc i))
        (persistent! arr)))))

(defn- unpack-map [unpacker opts]
  (let [length (MessageUnpacker/.unpackMapHeader unpacker)
        key-fn (:key-fn opts)]
    (loop [map (transient {})
           i 0]
      (if (< i length)
        (recur (assoc! map
                       (cond-> (-unpack unpacker opts)
                         key-fn key-fn)
                       (-unpack unpacker opts))
               (inc i))
        (persistent! map)))))

(defn- unpack-binary [unpacker]
  (let [length (MessageUnpacker/.unpackBinaryHeader unpacker)
        buf (byte-array length)]
    (MessageUnpacker/.readPayload unpacker buf)
    buf))

(defn- unpack-number [unpacker format]
  (condp = format
    MessageFormat/UINT64 (MessageUnpacker/.unpackBigInteger unpacker)
    MessageFormat/INT64 (MessageUnpacker/.unpackLong unpacker)
    MessageFormat/UINT32 (MessageUnpacker/.unpackLong unpacker)
    (MessageUnpacker/.unpackInt unpacker)))

(defn- -unpack [unpacker opts]
  (when (MessageUnpacker/.hasNext unpacker)
    (let [format (MessageUnpacker/.getNextFormat unpacker)
          type (MessageFormat/.getValueType format)]
      (condp = type
        ValueType/NIL (MessageUnpacker/.unpackNil unpacker)
        ValueType/FLOAT (MessageUnpacker/.unpackFloat unpacker)
        ValueType/STRING (MessageUnpacker/.unpackString unpacker)
        ValueType/BOOLEAN (MessageUnpacker/.unpackBoolean unpacker)

        ValueType/INTEGER (unpack-number unpacker format)
        ValueType/BINARY (unpack-binary unpacker)
        ValueType/ARRAY (unpack-array unpacker opts)
        ValueType/MAP (unpack-map unpacker opts)))))

(defn- into-unpacker [resource]
  (cond
    (instance? InputStream resource)
    (MessagePack/newDefaultUnpacker ^InputStream resource)

    (bytes? resource)
    (MessagePack/newDefaultUnpacker ^bytes resource)

    :else (throw (ex-info "Unsupported resource type" {:type (type resource)}))))

(defn unpack
  ([resource] (unpack resource {}))
  ([resource opts]
   (let [unpacker (into-unpacker resource)]
     (-unpack unpacker opts))))
