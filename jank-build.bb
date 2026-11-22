(require
 '[babashka.process :as proc]
 '[jank.build.util :as util]
 '[jank.build.cmake :refer [cmake]]
 '[jank.build.pkg-config :refer [parse-prefixed pkg-config has-static-lib?]])

(util/define "PAR_STREAMLINES_IMPLEMENTATION")
(util/define "SDL_MAIN_HANDLED")

(def sdl-to-install
  ; flags kinda mimicking https://github.com/Ravbug/sdl3-sample/blob/main/CMakeLists.txt 
  [{:git-dep    "sdl3"
    :lib-name   "SDL3"
    :extra-DEFS {"SDL_WAYLAND"      false
                 "SDL_VULKAN"       false
                 "SDL_METAL"        false
                 "SDL_GPU"          false
                 "SDL_CAMERA"       false
                 "SDL_POWER"        false
                 "SDL_SENSOR"       false
                 "SDL_DIALOG"       false
                 "SDL_TRAY"         false
                 "SDL_TESTS"        false
                 "SDL_EXAMPLES"     false}}
   {:git-dep    "sdl3-image"
    :lib-name   "SDL3_image"
    :extra-DEFS {"SDLIMAGE_VENDORED"   true
                 "SDLIMAGE_ANI"        false
                 "SDLIMAGE_AVIF"       false
                 "SDLIMAGE_BMP"        false
                 "SDLIMAGE_GIF"        false
                 "SDLIMAGE_JPG"        false
                 "SDLIMAGE_JXL"        false
                 "SDLIMAGE_LBM"        false
                 "SDLIMAGE_PCX"        false
                 "SDLIMAGE_PNG"        true
                 "SDLIMAGE_PNM"        false
                 "SDLIMAGE_QOI"        false
                 "SDLIMAGE_SVG"        false
                 "SDLIMAGE_TGA"        false
                 "SDLIMAGE_TIF"        false
                 "SDLIMAGE_WEBP"       false
                 "SDLIMAGE_XCF"        false
                 "SDLIMAGE_XPM"        false
                 "SDLIMAGE_XV"         false
                ; # SDL3 includes built-in support for loading PNGs.
                 "SDLIMAGE_PNG_LIBPNG" false}}
   {:git-dep    "sdl3-mixer"
    :lib-name   "SDL3_mixer"
    :extra-DEFS {"SDLMIXER_VENDORED"          true
                 "SDLMIXER_TESTS"             false
                 "SDLMIXER_EXAMPLES"          false
                 "SDLMIXER_AIFF"              false
                 "SDLMIXER_WAVE"              false
                 "SDLMIXER_VOC"               false
                 "SDLMIXER_AU"                false
                 "SDLMIXER_FLAC"              false
                 "SDLMIXER_GME"               false
                 "SDLMIXER_MOD"               false
                 "SDLMIXER_MP3"               false
                 "SDLMIXER_MIDI"              false
                 "SDLMIXER_OPUS"              false
                 "SDLMIXER_VORBIS_STB"        true
                 "SDLMIXER_VORBIS_VORBISFILE" false
                 "SDLMIXER_WAVPACK"           false}}
   {:git-dep    "sdl3-ttf"
    :lib-name   "SDL3_ttf"
    :extra-DEFS {"SDLTTF_VENDORED" true
                 "SDLTTF_PLUTOSVG" false}}])

(defn pkg-config-from-build
  "`pkg-config` Copied from jank.build.pkg-config, 
   with modification to make it uses installed lib during building"
  [build-input pc-name]
  (let [pkg-config-path (str (:out-dir build-input) "/lib/pkgconfig")
        pc-cmd    (cond-> ["pkg-config" pc-name "--libs" "--cflags"]
                    (:static? build-input) (conj "--static"))
        pc-opts   {:extra-env {"PKG_CONFIG_PATH" pkg-config-path}
                   :out       :string}
        pc-output (->> pc-cmd (apply proc/shell pc-opts) :out)
        link-dirs (parse-prefixed "-L" pc-output)]
    (run! util/link-dir link-dirs)
    (run! util/include-dir (parse-prefixed "-I" pc-output))
    (run! #(if (and (:static? build-input) (has-static-lib? % link-dirs))
             (util/link-static-library %)
             (util/link-library %))
          (parse-prefixed "-l" pc-output))))

(defn sdl-static []
  (let [shared-DEFS {"CMAKE_BUILD_TYPE"     "Release"
                     "CMAKE_INSTALL_LIBDIR" "lib"
                     "BUILD_SHARED_LIBS"    false}]
    (doseq [{:keys [git-dep lib-name extra-DEFS]} sdl-to-install]
      (let [input (-> *input*
                      (update :src-dir str "/git-deps/" git-dep)
                      (update :build-dir str "/BUILD-" lib-name))]
        (cmake input {:defines (merge shared-DEFS extra-DEFS)})

        (util/include-dir "include")
        (util/link-dir "lib")
        (util/link-library lib-name)))))

(cond
  (util/windows?)
  #_(let [assume-share-lib-location "../.libs"
          project-dir               (:src-dir *input*)]
      (doseq [[lib-name lib-dir]
              [["SDL3" "SDL3-3.4.2/x86_64-w64-mingw32"]
               ["SDL3_image" "SDL3_image-3.4.0/x86_64-w64-mingw32"]
               ["SDL3_mixer" "SDL3_mixer-3.2.4/x86_64-w64-mingw32"]
               ["SDL3_ttf" "SDL3_ttf-3.2.2/x86_64-w64-mingw32"]]]
        (util/include-dir (str project-dir "/" assume-share-lib-location "/" lib-dir "/include"))
        (util/link-dir (str project-dir "/" assume-share-lib-location "/" lib-dir "/bin"))
        (util/link-library lib-name)))
  (do
    (sdl-static)
    (pkg-config-from-build *input* "sdl3")
    (pkg-config-from-build *input* "sdl3-image")
    (pkg-config-from-build *input* "sdl3-mixer")
    (pkg-config-from-build *input* "sdl3-ttf"))

  (util/linux?)
  ;; assume using distrobox
  (let [container-id (System/getenv "CONTAINER_ID")]
    (case container-id
      "steamrt4"
      (do (pkg-config *input* "sdl3")
          (pkg-config *input* "sdl3-image")
          (pkg-config *input* "sdl3-mixer")
          (pkg-config *input* "sdl3-ttf"))

      ; this was tested working on ubuntu 24.04, 26.04
      (sdl-static)))

  :else
  (throw (ex-info (str "project not set up for" (System/getProperty "os.name") "yet") {})))
