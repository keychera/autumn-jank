#pragma once
#include <SDL3/SDL.h>
#include <choreograph/Choreograph.h>

using namespace choreograph;

template <typename T, typename... Args>
void thenHold(Sequence<T> &sequence, Args &&...args) {
  sequence.template then<Hold>(std::forward<Args>(args)...);
}

template <typename T, typename... Args>
void thenRampTo(Sequence<T> &sequence, Args &&...args) {
  sequence.template then<RampTo>(std::forward<Args>(args)...);
}

// from choreograph samples
struct vec2 {
  vec2(double x = 0.0f, double y = 0.0f) : x(x), y(y) {}

  vec2 &operator+=(const vec2 &o) {
    *this = *this + o;
    return *this;
  }
  vec2 operator+(const vec2 &o) const { return {x + o.x, y + o.y}; }
  vec2 operator-(const vec2 &o) const { return {x - o.x, y - o.y}; }
  vec2 operator*(double s) const { return {x * s, y * s}; }
  vec2 operator*(const vec2 &o) const { return {x * o.x, y * o.y}; }

  double x, y;
};
