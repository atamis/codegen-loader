(ns atamis.codegen-loader
  (:require [clojure.string :as string])
  (:import [javax.tools ToolProvider SimpleJavaFileObject JavaFileObject ForwardingJavaFileManager StandardLocation]
           [java.io ByteArrayOutputStream]
           [java.net URI]
           )
  )

(defn make-mem-file
  "Creates a new `SimpleJavaFileObject` for a `classname` and
  `kind` with the uri \"mem:///\" backed by a memory buffer a
  (`ByteArrayOutputStream`). "
  [class-name kind]
  (let [os (ByteArrayOutputStream.)
        new-cn (URI/create (str "mem:///"
                                (string/replace class-name "." "/")
                                (.extension kind)))]
    (proxy [SimpleJavaFileObject] [new-cn kind]
      (openOutputStream [] os))))

(defn output-capture-file-manager
  "Creates an output capturing memory backed forwarding file manager. See
  ForwardingJavaFileManager for more details."
  [file-manager]
  (let [outputs (atom {})
        proxy (proxy [ForwardingJavaFileManager] [file-manager]
                (getJavaFileForOutput [location className kind sibling]
                  (let [output-file (make-mem-file className kind)]
                    (swap! outputs assoc className output-file)
                    output-file)))]
    [outputs proxy]
    ))

(defn java-compile
  "Compiles the class at `classname`, and returns an atom-wrapped mapping of
  output file names to `SimpleFileObjects`. By calling

       (.toByteArray (.openOutputStream file))

  You can get the byte contents of the compiled Java class."
  [classname]
  (let [compiler (javax.tools.ToolProvider/getSystemJavaCompiler)
        [outputs file-manager] (output-capture-file-manager (.getStandardFileManager compiler nil nil nil))
        ;; This is nil if the file wasn't found.
        file (.getJavaFileForInput file-manager StandardLocation/CLASS_PATH classname
                                   javax.tools.JavaFileObject$Kind/SOURCE
                                   )
        task (.getTask compiler         ; compiler
                       nil              ; writer out
                       file-manager     ; file manager
                       nil              ; diagnostic listener
                       nil              ; options
                       nil              ; classes for annotations
                       [file]           ; compilation units
                       )
        _ (.call task)]
    [outputs file-manager]))

#_
(gen-class
 :name "atamis.codegen-loader.CodeGenClassLoader"
 ;; :extends ClassLoader
 :extends clojure.lang.DynamicClassLoader
 :prefix "codegen-classloader-"
 :exposes-methods {findClass super-classloader-findClass
                   }
 )

#_
(defn codegen-classloader-findClass
  [this classname]
  (let [[outputs _] (java-compile classname)]
    (if-let [class-file (@outputs classname)]
      (let [buffer (-> class-file .openOutputStream .toByteArray)]
        (.defineClass this classname buffer nil)
        )
      (.super-classloader-findClass this classname))))

(defn codegen-classloader
  "Returns a class loader that can dynamically compile Java classes as
  necessary."
  []
  #_(atamis.codegen-loader.CodeGenClassLoader.)
  (proxy [clojure.lang.DynamicClassLoader] []
    (findClass [classname]
      (let [[outputs _] (java-compile classname)]
        (if-let [class-file (@outputs classname)]
          (let [buffer (-> class-file .openOutputStream .toByteArray)]
            (proxy-super defineClass classname buffer nil)
            )
          (proxy-super findClass classname)))
      )
    ))

(comment
  (.setContextClassLoader (Thread/currentThread) (codegen-classloader))

  (let [cl (codegen-classloader)
        class (.findClass cl "org.example.Test")
        constructor (first (.getConstructors class))
        t (.newInstance constructor (into-array []))
        method (.getMethod class "test" (make-array Class 0))
        ]
    (.invoke method t (make-array Class 0))
    )

  )
