# codegen-loader

A clojure library that implements a classloader that dynamically compiles java
files rather than loading from .class files.

See [this post](https://atamis.me/posts/2019-05-28-codegen-loader/) for more
information.

## Usage

From the `atamis.codegen-loader` namespace.

`make-mem-file`: creates a new `SimpleJavaFileObject` for a `classname` and
`kind` with the uri "mem:///" backed by a memory buffer a
(`ByteArrayOutputStream`).

`output-capture-file-manager`: Creates an output capturing memory backed
  forwarding file manager. See ForwardingJavaFileManager for more details.


`java-compile`: Compiles the class at `classname`, and returns an atom-wrapped mapping of
  output file names to `SimpleFileObjects`. By calling

       (.toByteArray (.openOutputStream file))

  You can get the byte contents of the compiled Java class.
  
`codegen-classloader`: Returns a class loader that can dynamically
compile Java classes as necessary.

## License

Copyright © 2019 Andrew Amis

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
