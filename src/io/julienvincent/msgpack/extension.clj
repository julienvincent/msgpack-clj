(ns io.julienvincent.msgpack.extension
  (:import
   org.msgpack.core.ExtensionTypeHeader
   org.msgpack.core.MessagePacker
   org.msgpack.core.MessageUnpacker))

(defprotocol MsgpackExtension
  (can-pack? [_ value]
    "Test if this extension can pack the provided data type")
  (can-unpack? [_ type-byte]
    "Test if this extension can unpack the provided `type-byte`")

  (pack [_ ^MessagePacker packer value opts]
    "Pack the give `value` using the provided `packer`")
  (unpack [_ ^MessageUnpacker unpacker ^ExtensionTypeHeader header opts]
    "Unpack the custom data type as specified by the extension `header`
    using the provided `unpacker`"))

(def ^:no-doc ?MsgpackExtension
  [:or
   bytes?
   [:fn {:error/message "Should a type implementing MsgpackExtension"}
    (partial satisfies? MsgpackExtension)]])
