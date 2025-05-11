(ns io.julienvincent.msgpack.extension.clojure
  (:require
   [io.julienvincent.msgpack :as msgpack]
   [io.julienvincent.msgpack.extension :as extension])
  (:import
   org.msgpack.core.ExtensionTypeHeader
   org.msgpack.core.MessagePacker
   org.msgpack.core.MessageUnpacker))

(def keyword-ext
  (reify extension/MsgpackExtension
    (can-pack? [_ value]
      (instance? clojure.lang.Keyword value))
    (can-unpack? [_ type-byte]
      (= 3 type-byte))

    (pack [_ packer value _]
      (let [data (String/.getBytes (subs (str value) 1))
            len (alength data)]
        (MessagePacker/.packExtensionTypeHeader packer 3 len)
        (MessagePacker/.writePayload packer data)))

    (unpack [_ unpacker header _]
      (let [length (ExtensionTypeHeader/.getLength header)
            bytes (byte-array length)]
        (MessageUnpacker/.readPayload unpacker bytes)
        (keyword (String. bytes))))))

(def symbol-ext
  (reify extension/MsgpackExtension
    (can-pack? [_ value]
      (instance? clojure.lang.Symbol value))
    (can-unpack? [_ type-byte]
      (= 4 type-byte))

    (pack [_ packer value _]
      (let [data (String/.getBytes (name value))
            len (alength data)]
        (MessagePacker/.packExtensionTypeHeader packer 4 len)
        (MessagePacker/.writePayload packer data)))

    (unpack [_ unpacker header _]
      (let [length (ExtensionTypeHeader/.getLength header)
            bytes (byte-array length)]
        (MessageUnpacker/.readPayload unpacker bytes)
        (symbol (String. bytes))))))

(def set-ext
  (reify extension/MsgpackExtension
    (can-pack? [_ value]
      (instance? clojure.lang.IPersistentSet value))
    (can-unpack? [_ type-byte]
      (= 5 type-byte))

    (pack [_ packer value opts]
      (let [data (msgpack/pack (vec value) opts)
            len (alength data)]
        (MessagePacker/.packExtensionTypeHeader packer 5 len)
        (MessagePacker/.writePayload packer data)))

    (unpack [_ unpacker header opts]
      (let [length (ExtensionTypeHeader/.getLength header)
            bytes (byte-array length)]
        (MessageUnpacker/.readPayload unpacker bytes)
        (set (msgpack/unpack bytes opts))))))

(def extensions
  "Custom msgpack extensions for common Clojure data types"
  [keyword-ext
   symbol-ext
   set-ext])
