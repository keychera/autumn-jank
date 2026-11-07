(ns build.lets
  (:require
   [babashka.fs :as fs]
   [babashka.http-client :as http]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.build.api :as b])
  (:import
   [java.io File]))

(defonce deps-basis (delay (b/create-basis {:project "deps.edn" :aliases [:release]})))
(defonce repl-dev-basis (delay (b/create-basis {:project "deps.edn" :aliases [:repl-dev]})))
(def target-dir (delay (or (-> @deps-basis :jank :target-dir) "target")))

(defn log [& stuff]
  (apply println "[bermain@jank]" stuff))

;; jank pseudo-deps-edn, powered by https://github.com/babashka/tools.bbuild

(defn ->jank-deps-edn [basis]
  (let [{:keys [jank classpath]} basis
        no-clojure-classpath
        (into [] (remove #(and (str/includes? % "org")
                               (str/includes? % "clojure")))
              (keys classpath))]
    (assoc jank :module-path (str/join File/pathSeparator no-clojure-classpath))))

(defn get-main-module [deps-edn]
  (-> deps-edn :argmap :main str))

(defn ->flags [flag coll]
  (into [] (mapcat (fn [entry] [flag entry])) coll))

(def wherejank
  (try (edn/read-string (slurp "wherejank.edn"))
       (catch Throwable _ {:home nil :bin "jank"})))

(defn jank-command
  ([jank-deps-edn] (jank-command jank-deps-edn nil {}))
  ([jank-deps-edn command {:keys [main-module extra]}]
   (let [{:keys [module-path include-dirs library-dirs linked-libraries]} jank-deps-edn]
     (into []
           (remove nil?)
           (concat
            [(or (:bin wherejank) "jank") "--module-path" module-path]
            (->flags "-I" include-dirs)
            (->flags "-L" library-dirs)
            (->flags "-l" linked-libraries)
            [command main-module]
            extra)))))

(defn prep-kondo
  [{}]
  (try
    (let [{:keys [module-path]} (->jank-deps-edn @deps-basis)]
      (b/process {:command-args ["clj-kondo" "--lint" module-path
                                 "--dependencies" "--copy-configs" "--skip-lint"]}))
    (catch Throwable err
      (log "error when running clj-kondo! cause:" (:cause (Throwable->map err))))))

(defn tell-clangd ;; about our project
  [{}]
  (spit "compile_flags.txt"
        (->> (concat (jank-command (->jank-deps-edn @deps-basis))
                     ["-I" (str (:home wherejank) "/compiler+runtime/include/cpp")])
             (drop 3)
             (str/join "\n"))))

(defn download [url to-file]
  (with-open [input-stream (:body (http/get url {:as :stream}))]
    (io/copy input-stream (io/file to-file))))

(def dot-libs ".libs")

(def sdl3-dep
  {:download-url "https://github.com/libsdl-org/SDL/releases/download/release-3.4.2/SDL3-devel-3.4.2-mingw.tar.gz"
   :target-tar-gz "sdl3.tar.gz"
   :release-dir "SDL3-3.4.2/x86_64-w64-mingw32"
   :dll-file "bin/SDL3.dll"})

(def sdl3-image-dep
  {:download-url "https://github.com/libsdl-org/SDL_image/releases/download/release-3.4.0/SDL3_image-devel-3.4.0-mingw.tar.gz"
   :target-tar-gz "sdl3-image.tar.gz"
   :release-dir "SDL3_image-3.4.0/x86_64-w64-mingw32"
   :dll-file "bin/SDL3_image.dll"})

(def sdl3-ttf-dep
  {:download-url "https://github.com/libsdl-org/SDL_ttf/releases/download/release-3.2.2/SDL3_ttf-devel-3.2.2-mingw.tar.gz"
   :target-tar-gz "sdl3-ttf.tar.gz"
   :release-dir "SDL3_ttf-3.2.2/x86_64-w64-mingw32"
   :dll-file "bin/SDL3_ttf.dll"})

(def sdl3-mixer-dep
  {:download-url "https://github.com/libsdl-org/SDL_mixer/releases/download/release-3.2.4/SDL3_mixer-devel-3.2.4-mingw.tar.gz"
   :target-tar-gz "sdl3-mixer.tar.gz"
   :release-dir "SDL3_mixer-3.2.4/x86_64-w64-mingw32"
   :dll-file "bin/SDL3_mixer.dll"})

(defn download-sdl-dep
  [{:keys [release-dir target-tar-gz]}]
  (let [sdl-devel (str dot-libs "/" target-tar-gz)
        sdl-dir   (str dot-libs "/" release-dir)]
    (io/make-parents sdl-devel)
    (download "https://github.com/libsdl-org/SDL_mixer/releases/download/release-3.2.4/SDL3_mixer-devel-3.2.4-mingw.tar.gz" sdl-devel)
    (b/process {:command-args ["tar" "-xzvf" sdl-devel "-C" dot-libs]})
    (log "download-sdl3-ttf done")
    (log (str "  include dir: " sdl-dir "/include"))
    (log (str "  bin dir: " sdl-dir "/bin"))))

(defn gather-shared-libs [to-here]
  (doseq [sdl-dep [sdl3-dep sdl3-image-dep sdl3-ttf-dep sdl3-mixer-dep]]
    (fs/copy (str dot-libs "/" (:release-dir sdl-dep) "/" (:dll-file sdl-dep))
             (str to-here "/" (str/replace-first (:dll-file sdl-dep) #"bin/" "")) {:replace-existing true}))
  (doseq [file (fs/glob (str dot-libs "/missing-pieces") "*.dll")]
    (fs/copy file to-here {:replace-existing true})))

(defn prep [args]
  (prep-kondo args)
  (tell-clangd args)
  (doseq [sdl-dep [sdl3-dep sdl3-image-dep sdl3-ttf-dep sdl3-mixer-dep]]
    (download-sdl-dep sdl-dep)))

(defn clean [& _]
  (println "cleaning target...")
  (b/delete {:path @target-dir}))

(defn jank
  {:org.babashka/cli {:exec-args {:profile? false
                                  :args nil}
                      :coerce {:profile? :bool}}}
  [{:keys [args profile?]}]
  (log "setting up jank pseudo deps.edn project" args)
  (let [jedn (->jank-deps-edn @deps-basis)
        jcmd (jank-command
              jedn "run-main"
              {:main-module (get-main-module @deps-basis)
               :extra
               (flatten
                [(when profile? ["--profile"])
                 "-DPAR_STREAMLINES_IMPLEMENTATION"
                 "--" args])})]
    (log jcmd)
    (b/process
     (let [JANK_EXTRA_FLAGS (str/join (cond-> []
                                        profile? (conj "-gcodeview")))]
       {:env {"JANK_EXTRA_FLAGS" JANK_EXTRA_FLAGS}
        :command-args jcmd}))))

(defn repl-dev
  [{:keys [args]}]
  (log "setting up jank pseudo deps.edn project" args)
  (let [jedn (->jank-deps-edn @repl-dev-basis)
        jcmd (jank-command jedn "repl" {:main-module (get-main-module @repl-dev-basis)
                                        :extra ["-DPAR_STREAMLINES_IMPLEMENTATION"]})]
    (log jcmd)
    (b/process {:command-args jcmd})))

(defn compile-jank
  {:org.babashka/cli {:exec-args {:profile? false}
                      :coerce {:profile? :bool}}}
  [{:keys [profile?] :as args}]
  (println "compile-jank with" args)
  (let [jedn (->jank-deps-edn @deps-basis)
        jout (cond-> (-> @deps-basis :jank :compile-out)
               :profile? (str "---profile"))
        jcmd (jank-command jedn "compile" {:main-module (get-main-module @deps-basis)
                                           :extra ["-DPAR_STREAMLINES_IMPLEMENTATION"
                                                   "-DSDL_MAIN_HANDLED"
                                                   "--name" jout "-O3"]})]
    (log jcmd)
    (let [JANK_EXTRA_FLAGS
          (str/join " "
                    (cond-> []
                      ;; https://discourse.llvm.org/t/generate-coff-symbols-even-when-generating-pdb/80488/2
                      profile? (conj "-g -gcodeview"
                                     "-fuse-ld=lld"
                                     "-Wl,/debug:full"
                                     "-Wl,/pdbaltpath:%_PDB%")
                      (fs/windows?) (conj "-Wl,-subsystem,windows")))]
      (b/process {:env {"JANK_EXTRA_FLAGS" JANK_EXTRA_FLAGS}
                  :command-args jcmd}))
    (when (fs/windows?)
      (gather-shared-libs @target-dir))))

(defn play [& _]
  (let [main-exe (str @target-dir "/" (-> @deps-basis :jank :compile-out) ".exe")]
    (b/process {:command-args [main-exe]})))

(defn ship [& _]
  (let [jout (-> @deps-basis :jank :compile-out)
        🚢   (str "ship/" jout)
        exe  (str 🚢 "/" jout ".exe")]
    (io/make-parents exe)
    (gather-shared-libs 🚢)
    (fs/copy-tree "res" (str 🚢 "/res") {:replace-existing true})
    (fs/copy (str @target-dir "/" jout ".exe") exe {:replace-existing true})
    (fs/zip (str 🚢 ".zip") 🚢 {:root "ship"})))
