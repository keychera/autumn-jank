(require
 '[jank.build.util :as util]
 '[jank.build.pkg-config :refer [pkg-config]])

(util/define "PAR_STREAMLINES_IMPLEMENTATION")

(cond
  (util/windows?)
  (let [project-dir (:src-dir *input*)]
    (doseq [include-dir ["../.libs/SDL3-3.4.2/x86_64-w64-mingw32/include"
                         "../.libs/SDL3_image-3.4.0/x86_64-w64-mingw32/include"
                         "../.libs/SDL3_ttf-3.2.2/x86_64-w64-mingw32/include"
                         "../.libs/SDL3_mixer-3.2.4/x86_64-w64-mingw32/include"]]
      (util/include-dir (str project-dir "/" include-dir))
      (util/link-dir (str project-dir "/" include-dir "/bin"))))
  
  (util/linux?)
  ;; assuming Steam Linux Runtime 4 or distro with sdl3 already available
  (do (util/link-dir "/usr/lib/gcc/x86_64-linux-gnu/14") ;; required for sdl-ttf atm, jank compiler doesnt search this path yet
      (pkg-config *input* "sdl3")
      (pkg-config *input* "sdl3-image")
      (pkg-config *input* "sdl3-mixer")
      (pkg-config *input* "sdl3-ttf"))
  
  :else
  (throw (ex-info (str "project not set up for" (System/getProperty "os.name") "yet") {})))
