<div align="center">
  <h1>msgpack-clj</h1>

  <p>
    High performance Clojure bindings for msgpack-java.
  </p>

[![Clojars Project](https://img.shields.io/clojars/v/io.julienvincent/msgpack.svg)](https://clojars.org/io.julienvincent/msgpack)

</div>

This is a wrapper around the official Java implementation for msgpack
([msgpack-java](https://github.com/msgpack/msgpack-java)) which has a larger community and is generally well maintained.

This implementation tries to be as low-overhead as possible, providing the high-performance translations from
msgpack-java into clojure datatypes.

It supports packing to/from `byte-arrays` as well as directly to `InputStreams` and `OutputStreams`. It also exposes an
API for transforming clojure map keys during pack/unpack, commonly used for stringifying/keywordizing map keys.

## Index

- **[Features](#features)**
- **[Documentation](https://cljdoc.org/d/io.julienvincent/msgpack)**
- **[Examples](#examples)**
- **[Micro Benchmarks](#micro-benchmarks)**

## Features

- Serialize to/from byte-arrays
- Serialize to/from Input/Output streams
- Transform map keys during pack/unpack using a provided key-fn
- Provide datatype extensions [TODO]

## Documentation

Please find the documentation at **https://cljdoc.org/d/io.julienvincent/msgpack**

## Examples

```clojure
(ns user
  (:require
   [io.julienvincent.msgpack :refer [pack unpack keywordize-keys-mapper]]))

;; Pack into a byte-array
(pack (pack {:a 1 "b" [1 2 3]})) ;; => {"a" 1 "b" [1 2 3]}

;; Specify a custom key-fn
(unpack (pack {:a 1} {:key-fn str})) ;; => {":a" 1}

;; Keywordize keys when unpacking
(unpack (pack {:a 1}) {:key-fn keywordize-keys-mapper}) ;; => {:a 1}

;; Pack directly into a stream
(with-open [stream (output-stream "test.dat")]
  (pack-stream stream {:a 1}))

;; Unpack from a stream
(with-open [stream (input-stream "test.dat")]
  (unpack stream)) ;; => {"a" 1}
```

## Micro Benchmarks

This is a very un-scientific section and is just meant to give ball-park insight into the general performance of the
project. Take it with a large pinch of salt.

All benchmarks were run using https://github.com/hugoduncan/criterium/ and with the below dataset (click to expand).

<details>
  <summary><code><b>Data Set</b></code></summary>

Just a bit of random data to pack/unpack for benchmarking

```clojure
{:string-key "Hello, World!"
 :integer-value 42
 :decimal-number 3.14159
 :boolean-value true
 :nil-value nil
 :nested-map {:name "John Doe"
              :age 30
              :active? true
              :preferences {:theme "dark"
                            :notifications true}}
 :simple-vector [1 2 3 4 5]
 :mixed-vector ["string" 42 true {:key "value"} ["nested-vector"]]
 :nested-vectors [[1 2 3] [4 5 6] [7 8 9]]
 :array-of-maps [{:id 1
                  :type "user"
                  :data {:email "user1@example.com"
                         :tags ["active" "premium"]}}
                 {:id 2
                  :type "admin"
                  :data {:email "admin@example.com"
                         :permissions ["read" "write" "delete"]}}]
 :deep-nesting {:level1 {:level2 {:level3 {:value "deeply nested"
                                           :numbers [1 2 3]
                                           :map {:a 1 :b 2}}}}}
 :edge-cases {:empty-string ""
              :empty-vector []
              :empty-map {}}}
```

</details>

### msgpack-clj

<details>
  <summary><code><b>Pack Benchmark</b></code></summary>

```bash
aarch64 Mac OS X 14.3 12 cpu(s)
OpenJDK 64-Bit Server VM 21.0.4+7-LTS
Runtime arguments: -XX:-OmitStackTraceInFastThrow -Djdk.attach.allowAttachSelf -XX:+UseG1GC -XX:-OmitStackTraceInFastThrow -Djdk.attach.allowAttachSelf -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints -Dclojure.basis=.cpcache/3981153520.basis
Evaluation count : 22717080 in 60 samples of 378618 calls.
      Execution time sample mean : 2.623925 µs
             Execution time mean : 2.623680 µs
Execution time sample std-deviation : 15.917052 ns
    Execution time std-deviation : 16.022018 ns
   Execution time lower quantile : 2.603043 µs ( 2.5%)
   Execution time upper quantile : 2.659101 µs (97.5%)
                   Overhead used : 1.370423 ns

Found 2 outliers in 60 samples (3.3333 %)
	low-severe	 2 (3.3333 %)
 Variance from outliers : 1.6389 % Variance is slightly inflated by outliers
```

</details>

<details>
  <summary><code><b>Unpack Benchmark</b></code></summary>

```bash
aarch64 Mac OS X 14.3 12 cpu(s)
OpenJDK 64-Bit Server VM 21.0.4+7-LTS
Runtime arguments: -XX:-OmitStackTraceInFastThrow -Djdk.attach.allowAttachSelf -XX:+UseG1GC -XX:-OmitStackTraceInFastThrow -Djdk.attach.allowAttachSelf -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints -Dclojure.basis=.cpcache/3981153520.basis
Evaluation count : 15104760 in 60 samples of 251746 calls.
      Execution time sample mean : 3.972117 µs
             Execution time mean : 3.972168 µs
Execution time sample std-deviation : 13.345495 ns
    Execution time std-deviation : 13.700262 ns
   Execution time lower quantile : 3.945115 µs ( 2.5%)
   Execution time upper quantile : 3.998236 µs (97.5%)
                   Overhead used : 1.370423 ns

Found 1 outliers in 60 samples (1.6667 %)
	low-severe	 1 (1.6667 %)
 Variance from outliers : 1.6389 % Variance is slightly inflated by outliers
```

</details>

### msgpack

https://github.com/edma2/clojure-msgpack

Probably the defacto msgpack implementation in Clojure at the moment.

<details>
  <summary><code><b>Pack Benchmark</b></code></summary>

```bash
aarch64 Mac OS X 14.3 12 cpu(s)
OpenJDK 64-Bit Server VM 21.0.4+7-LTS
Runtime arguments: -XX:-OmitStackTraceInFastThrow -Djdk.attach.allowAttachSelf -XX:+UseG1GC -XX:-OmitStackTraceInFastThrow -Djdk.attach.allowAttachSelf -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints -Dclojure.basis=.cpcache/3981153520.basis
Evaluation count : 5306220 in 60 samples of 88437 calls.
      Execution time sample mean : 11.301867 µs
             Execution time mean : 11.303139 µs
Execution time sample std-deviation : 76.691707 ns
    Execution time std-deviation : 80.190344 ns
   Execution time lower quantile : 11.216297 µs ( 2.5%)
   Execution time upper quantile : 11.556942 µs (97.5%)
                   Overhead used : 1.370423 ns

Found 5 outliers in 60 samples (8.3333 %)
	low-severe	 1 (1.6667 %)
	low-mild	 4 (6.6667 %)
 Variance from outliers : 1.6389 % Variance is slightly inflated by outliers
```

</details>

<details>
  <summary><code><b>Unpack Benchmark</b></code></summary>

```bash
aarch64 Mac OS X 14.3 12 cpu(s)
OpenJDK 64-Bit Server VM 21.0.4+7-LTS
Runtime arguments: -XX:-OmitStackTraceInFastThrow -Djdk.attach.allowAttachSelf -XX:+UseG1GC -XX:-OmitStackTraceInFastThrow -Djdk.attach.allowAttachSelf -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints -Dclojure.basis=.cpcache/3981153520.basis
Evaluation count : 14837700 in 60 samples of 247295 calls.
      Execution time sample mean : 4.032280 µs
             Execution time mean : 4.032327 µs
Execution time sample std-deviation : 11.766116 ns
    Execution time std-deviation : 11.920007 ns
   Execution time lower quantile : 4.007854 µs ( 2.5%)
   Execution time upper quantile : 4.052157 µs (97.5%)
                   Overhead used : 1.370423 ns
```

</details>

<br />

As this implementation doesn't support stringifying/keywordizing map keys the data was prepared in advance using
`clojure.walk/stringify-keys` so that all keys were pre-transformed from keywords to strings.

In a real-world application we would need to do these transformations before/after pack/unpack in the hot-path - so we
should probably show those benchmarks here too:

<details>
  <summary><code><b>Pack Benchmark</b></code></summary>

```clojure
(-> data
    walk/stringify-keys
    msgpack/pack)
```

```bash
aarch64 Mac OS X 14.3 12 cpu(s)
OpenJDK 64-Bit Server VM 21.0.4+7-LTS
Runtime arguments: -XX:-OmitStackTraceInFastThrow -Djdk.attach.allowAttachSelf -XX:+UseG1GC -XX:-OmitStackTraceInFastThrow -Djdk.attach.allowAttachSelf -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints -Dclojure.basis=.cpcache/3981153520.basis
Evaluation count : 1661580 in 60 samples of 27693 calls.
      Execution time sample mean : 36.262594 µs
             Execution time mean : 36.265141 µs
Execution time sample std-deviation : 299.940999 ns
    Execution time std-deviation : 304.709889 ns
   Execution time lower quantile : 35.891097 µs ( 2.5%)
   Execution time upper quantile : 37.058554 µs (97.5%)
                   Overhead used : 1.370423 ns

Found 6 outliers in 60 samples (10.0000 %)
	low-severe	 4 (6.6667 %)
	low-mild	 2 (3.3333 %)
 Variance from outliers : 1.6389 % Variance is slightly inflated by outliers
```

</details>

<details>
  <summary><code><b>Unpack Benchmark</b></code></summary>

```clojure
(-> packed
    msgpack/unpack
    walk/keywordize-keys)
```

```bash
aarch64 Mac OS X 14.3 12 cpu(s)
OpenJDK 64-Bit Server VM 21.0.4+7-LTS
Runtime arguments: -XX:-OmitStackTraceInFastThrow -Djdk.attach.allowAttachSelf -XX:+UseG1GC -XX:-OmitStackTraceInFastThrow -Djdk.attach.allowAttachSelf -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints -Dclojure.basis=.cpcache/3981153520.basis
Evaluation count : 2117700 in 60 samples of 35295 calls.
      Execution time sample mean : 28.528210 µs
             Execution time mean : 28.526936 µs
Execution time sample std-deviation : 239.538394 ns
    Execution time std-deviation : 240.243441 ns
   Execution time lower quantile : 28.200709 µs ( 2.5%)
   Execution time upper quantile : 28.975086 µs (97.5%)
                   Overhead used : 1.370423 ns
```

</details>

<br />

Now we really see the impact, and how the `:key-fn` from `msgpack-clj` can make a very significant real-world
difference.

### jsonista

https://github.com/metosin/jsonista

This is **NOT** a msgpack implementation, but it is the most common cousin serialization format in use and jsonista is
one of (if not the) most performant Clojure implementations out there.

This benchmark is mostly just included as an interesting datapoint. Surprisingly, json serialization and deserialization
is still significantly faster than this implementation!

<details>
  <summary><code><b>Serialize Benchmark</b></code></summary>

```bash
aarch64 Mac OS X 14.3 12 cpu(s)
OpenJDK 64-Bit Server VM 21.0.4+7-LTS
Runtime arguments: -XX:-OmitStackTraceInFastThrow -Djdk.attach.allowAttachSelf -XX:+UseG1GC -XX:-OmitStackTraceInFastThrow -Djdk.attach.allowAttachSelf -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints -Dclojure.basis=.cpcache/3981153520.basis
Evaluation count : 31685760 in 60 samples of 528096 calls.
      Execution time sample mean : 1.893252 µs
             Execution time mean : 1.893249 µs
Execution time sample std-deviation : 5.264472 ns
    Execution time std-deviation : 5.323145 ns
   Execution time lower quantile : 1.881864 µs ( 2.5%)
   Execution time upper quantile : 1.904438 µs (97.5%)
                   Overhead used : 1.370423 ns

Found 6 outliers in 60 samples (10.0000 %)
	low-severe	 3 (5.0000 %)
	low-mild	 3 (5.0000 %)
 Variance from outliers : 1.6389 % Variance is slightly inflated by outliers
```

</details>

<details>
  <summary><code><b>Deserialize Benchmark</b></code></summary>

```bash
aarch64 Mac OS X 14.3 12 cpu(s)
OpenJDK 64-Bit Server VM 21.0.4+7-LTS
Runtime arguments: -XX:-OmitStackTraceInFastThrow -Djdk.attach.allowAttachSelf -XX:+UseG1GC -XX:-OmitStackTraceInFastThrow -Djdk.attach.allowAttachSelf -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints -Dclojure.basis=.cpcache/3981153520.basis
Evaluation count : 18559500 in 60 samples of 309325 calls.
      Execution time sample mean : 3.242291 µs
             Execution time mean : 3.242349 µs
Execution time sample std-deviation : 20.885299 ns
    Execution time std-deviation : 21.384995 ns
   Execution time lower quantile : 3.217228 µs ( 2.5%)
   Execution time upper quantile : 3.288841 µs (97.5%)
                   Overhead used : 1.370423 ns

Found 4 outliers in 60 samples (6.6667 %)
	low-severe	 3 (5.0000 %)
	low-mild	 1 (1.6667 %)
 Variance from outliers : 1.6389 % Variance is slightly inflated by outliers
```

</details>
