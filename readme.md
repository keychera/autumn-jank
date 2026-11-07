# autumn jank

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
```

## profiling

wpr -start CPU

wpr -stop jank.etl


## exp

woes
1. the virus (It Works On My Machine™)
2. the 3 missing dlls (It Works On My Machine™)
3. the multithreaded texture load (It Works On My Machine™)
4. the GC Pressure perf drop (might or might not be odoyle related)
5. the stutter on the first new Sequence
6. error on compile that is not unwinded (delete _cache, windows woes)
7. the mismatch float int type crash during repl (windows woes)
8. the extra.hpp ld.lld: error: duplicate symbol when AOT compile (lack of c++ knowledge)
9. fail to build inside Steam Linux Runtime 4 (lack of c++ knowledge)
