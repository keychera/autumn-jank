(require
 '[jank.build.util :as util]
 '[jank.build.pkg-config :refer [pkg-config]])

(util/define "PAR_STREAMLINES_IMPLEMENTATION")

(cond
  (util/windows?)
  (let [assume-share-lib-location "../.libs"
        project-dir (:src-dir *input*)]
    (doseq [[lib-name lib-dir]
            [["SDL3" "SDL3-3.4.2/x86_64-w64-mingw32"]
             ["SDL3_image" "SDL3_image-3.4.0/x86_64-w64-mingw32"]
             ["SDL3_mixer" "SDL3_mixer-3.2.4/x86_64-w64-mingw32"]
             ["SDL3_ttf" "SDL3_ttf-3.2.2/x86_64-w64-mingw32"]]]
      (util/include-dir (str project-dir "/" assume-share-lib-location "/" lib-dir "/include"))
      (util/link-dir (str project-dir "/" assume-share-lib-location "/" lib-dir "/bin"))
      (util/link-library lib-name)))

  (util/linux?)
  ;; assuming Steam Linux Runtime 4 or distro with sdl3 already available
  (do (util/link-dir "/usr/lib/gcc/x86_64-linux-gnu/14") ;; required for sdl-ttf atm, jank compiler doesnt search this path yet
      (pkg-config *input* "sdl3")
      (pkg-config *input* "sdl3-image")
      (pkg-config *input* "sdl3-mixer")
      (pkg-config *input* "sdl3-ttf"))

  :else
  (throw (ex-info (str "project not set up for" (System/getProperty "os.name") "yet") {})))
