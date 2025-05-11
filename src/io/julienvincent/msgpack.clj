(ns io.julienvincent.msgpack
  (:import
   clojure.lang.PersistentArrayMap
   clojure.lang.PersistentHashMap
   clojure.lang.PersistentVector
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
    (reduce-kv (fn [acc key value]
                 (pack- packer (cond-> key key-fn key-fn) opts)
                 (pack- packer value opts)
                 acc)
               nil
               value)))

(defn- pack-vector [packer value opts]
  (let [len (count value)]
    (MessagePacker/.packArrayHeader packer len)
    (reduce (fn [acc element]
              (pack- packer element opts)
              acc)
            nil
            value)))

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
    (instance? BigInteger value) (MessagePacker/.packBigInteger packer value)
    (instance? Short value) (MessagePacker/.packShort packer value)

    (float? value) (MessagePacker/.packFloat packer value)
    (string? value) (MessagePacker/.packString packer value)
    (boolean? value) (MessagePacker/.packBoolean packer value)
    (nil? value) (MessagePacker/.packNil packer)

    (map? value) (pack-map packer value opts)
    (vector? value) (pack-vector packer value opts)
    (bytes? value) (pack-bytes packer value)

    (or (sequential? value)
        (set? value)) (pack-sequential packer value opts)

    :else (throw (IllegalArgumentException.
                  (str "Unsupported datatype of type " (type value))))))

(defn stringify-keys-mapper [k]
  (if (keyword? k)
    (name k)
    k))

(defn keywordize-keys-mapper [k]
  (keyword k))

(def ^:private default-pack-opts
  {:key-fn stringify-keys-mapper})

(def ^:no-doc ?PackOpts
  [:map
   [:key-fn {:optional true} [:function
                              [:-> :any :any]]]])

(defn pack
  "Pack the given `value` into a msgpack byte-array.

  Accepts an optional `opts` map containing:

  - **`:key-fn`** - A function accepting the key of a map being packed. Can be
    used to efficiently cast map keys to new types. Defaults to the
    [[stringify-keys-mapper]] fn.

  Example:

  ```clojure
  (pack {:a 1} {:key-fn stringify-keys-mapper})
  (pack [1 2 3])
  ```"
  {:malli/schema [:function
                  [:-> :any bytes?]
                  [:-> :any [:maybe ?PackOpts] bytes?]]}
  ([value] (pack value nil))
  ([value opts]
   (let [packer (MessagePack/newDefaultBufferPacker)
         opts (or default-pack-opts opts)]
     (pack- packer value opts)
     (MessageBufferPacker/.toByteArray packer))))

(def ^:no-doc ?OutputStream
  [:fn {:error/message "Should be an instance of OutputStream"}
   (partial instance? OutputStream)])

(defn pack-stream
  "Like [[pack]] but writes the packed bytes directly into a given `stream`."
  {:malli/schema [:function
                  [:-> ?OutputStream :any :nil]
                  [:-> ?OutputStream :any [:maybe ?PackOpts] :nil]]}
  ([^OutputStream stream value] (pack-stream stream value nil))
  ([^OutputStream stream value opts]
   (let [packer (MessagePack/newDefaultPacker stream)
         opts (or opts default-pack-opts)]
     (pack- packer value opts)
     (MessagePacker/.flush packer)
     nil)))

(declare ^:private -unpack)

(defn- unpack-array [unpacker opts]
  (let [length (MessageUnpacker/.unpackArrayHeader unpacker)
        arr (object-array length)]
    (loop [i 0]
      (if (< i length)
        (do (aset arr i (-unpack unpacker opts))
            (recur (inc i)))
        (PersistentVector/adopt arr)))))

(defn- unpack-map [unpacker opts]
  (let [length (MessageUnpacker/.unpackMapHeader unpacker)
        key-fn (:key-fn opts)]
    (if (< 8 length)
      ;; When associng into a transient PersistentArrayMap, created using
      ;; (transient {}), clojure will convert the map into a PersistentHashMap
      ;; when the size grows past 8 entries.
      ;;
      ;; As we know the size will be greater than 8 up front we can create a
      ;; transient PHM directly instead to avoid this re-allocation.  
      (loop [map (transient PersistentHashMap/EMPTY)
             i 0]
        (if (< i length)
          (recur (assoc! map
                         (cond-> (-unpack unpacker opts)
                           key-fn key-fn)
                         (-unpack unpacker opts))
                 (inc i))
          (persistent! map)))

      ;; For maps with 8 or fewer elements it's faster to allocate an object
      ;; array and later wrap it with a PersistentArrayMap when we are done
      ;; unpacking into it. 
      (let [length (* length 2)
            arr (object-array length)]
        (loop [i 0]
          (if (< i length)
            (do (aset arr i (cond-> (-unpack unpacker opts)
                              key-fn key-fn))
                (aset arr (inc i) (-unpack unpacker opts))
                (recur (+ i 2)))
            (PersistentArrayMap. arr)))))))

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

    :else (throw (IllegalArgumentException.
                  (str "Unsupported resource type " (type resource))))))

(def ^:no-doc ?UnpackOpts
  [:map
   [:key-fn {:optional true} [:function
                              [:-> :any :any]]]])

(def ^:no-doc ?Resource
  [:or
   bytes?
   [:fn {:error/message "Should be an instance of InputStream"}
    (partial instance? InputStream)]])

(defn unpack
  "Unpack a msgpack value from a given `resource`.

  The `resource` can be any one of:

  - A byte-array
  - A type implementing [[java.io.InputStream]]

  Accepts an optional `opts` map containing:

  - **`:key-fn`** - A function accepting the key of a map being unpacked. Can
    be used to efficiently cast map keys to new types.

  Example:

  ```clojure
  (unpack (pack {:a 1})) ;; => {\"a\" 1}
  (unpack (pack {:a 1}) {:key-fn keywordize-keys-mapper}) ;; => {:a 1}
  (unpack (io/input-stream (pack 1))) ;; => 1
  ```"
  {:malli/schema [:function
                  [:-> ?Resource :any]
                  [:-> ?Resource [:maybe ?UnpackOpts] :any]]}
  ([resource] (unpack resource {}))
  ([resource opts]
   (let [unpacker (into-unpacker resource)]
     (-unpack unpacker opts))))
