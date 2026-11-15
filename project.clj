(defproject chera/autumn-jank "0.1-SNAPSHOT"
  :plugins [[org.jank-lang/lein-jank "2026.10-3"]]
  :middleware [leiningen.jank/middleware]
  :main bermain.game 
  :source-paths ["src" "git-deps/odoyle-jank/src"] 
  :jank {:include-dirs ["src-cpp" 
                        "git-deps/choreograph/src"
                        "git-deps/par"]}
  :build-dependencies [[org.jank-lang.commons/jank-build "2026.10-1"]]
  :profiles {:base    {:source-paths ["src-dev"]
                       :main bermain.dev
                       :jank {:target-dir         "target/debug"
                              :optimization-level 0}}
             :release {:jank {:target-dir         "target/release"
                              :optimization-level 3}}})
