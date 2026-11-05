#include <SDL3/SDL.h>
#include <choreograph/Choreograph.h>
#include <par_streamlines.h>
#include <vector>

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

// parsl -> SDL
static std::vector<SDL_Vertex>
parsl_mesh_to_SDL_Vertices(const parsl_mesh *mesh) {
  std::vector<SDL_Vertex> sdl_vertices(mesh->num_vertices);

  for (uint32_t i = 0; i < mesh->num_vertices; ++i) {
    sdl_vertices[i].position.x = mesh->positions[i].x;
    sdl_vertices[i].position.y = mesh->positions[i].y;
    sdl_vertices[i].color = SDL_FColor{0.0f, 0.0f, 0.0f, 255};
    sdl_vertices[i].tex_coord = SDL_FPoint{0.0f, 0.0f};
  }

  return sdl_vertices;
}

static std::vector<int> parsl_mesh_to_indices(const parsl_mesh *mesh) {
  int num_indices = mesh->num_triangles * 3;
  std::vector<int> sdl_indices(num_indices);

  for (int i = 0; i < num_indices; ++i) {
    sdl_indices[i] = static_cast<int>(mesh->triangle_indices[i]);
  }
  return sdl_indices;
}

// https://prideout.net/blog/par_streamlines/#basic-usage
inline parsl_position square_vertices[] = {
    {50, 150}, {200, 100}, {400, 200}, {400, 100}};

inline uint16_t spine_lengths[] = {4};

inline parsl_context *create_sl_context(float thickness) {
  parsl_context *sl_context = parsl_create_context({.thickness = thickness});
  return sl_context;
}
