# autumn jank

a game for [Autumn Lisp Game Jam 2026](https://itch.io/jam/autumn-lisp-game-jam-2026/) 

## build

need jank, babashka
```sh
# download sdl, prep devtime goodies (still hardcoded for windows)
bb prep

# run the game
bb jank

# run the game and the repl
bb repl-dev

# AOT compile
bb compile

# run the compiled game
bb lets play

# AOT compile enabling profiling for Windows Performance Recorder
bb compile :profile?

# ship
bb lets ship
```

## profiling

```sh
bb compile :profile?

./target/potion-inspector---profile.exe

(need separate admin cmd)
wpr -start CPU

# play the game

wpr -stop jank.etl

```
open jank.etl with [Profiler Explorer](https://github.com/microsoft/profile-explorer)

## exp

woes
1. the virus (It Works On My Machine™)
2. the 3 missing dlls (It Works On My Machine™)
3. the multithreaded texture load (It Works On My Machine™)
4. the GC Pressure perf drop (might or might not be odoyle related)
    - clang is awesome, WPR is awesome, too awesome even that we bikeshed profiling so often
    - `bb compile :profile?`
5. the stutter on the first new Sequence
6. error on compile that is not unwinded (delete _cache, windows woes)
7. the mismatch float int type crash during repl (windows woes)
8. the extra.hpp ld.lld: error: duplicate symbol when AOT compile (lack of c++ knowledge)
    - seems like it's not about static vs inline? the error is from a c header (par_streamlines.h) will investigate further
9. fail to build inside Steam Linux Runtime 4 (lack of c++ knowledge)
10. naming binding the same name as type, runtime error
11. libunwind:      pc not in table, pc=0x14D23563F3F (windows woes, ikappakiさん、頑張れ！！ https://github.com/lhames/llvm-project/pull/5 , https://github.com/llvm/llvm-project/issues/163503)
12. sometimes I need to manually delete target/_cache or it compiles error (maybe lein jank do something about this?)
13. we thought we need to do this
```clojure
(defn get-choreo-vec2-duration
  [^(:* (choreograph.Sequence cpp/vec2)) choreo-vec2]
  (.getDuration (cpp/* choreo-vec2)))
```
but we can do this
```clojure
(defn get-choreo-vec2-duration
  [^(:* (choreograph.Sequence cpp/vec2)) choreo-vec2]
  (.getDuration choreo-vec2))
```
