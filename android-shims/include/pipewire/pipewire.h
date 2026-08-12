#pragma once
// Android stub — pipewire/pipewire.h
// Real PipeWire API replaced by Qt Multimedia (see pipewire_android.cpp)

#include <stdint.h>
#include <stddef.h>
#include <stdlib.h>

#ifdef __cplusplus
extern "C" {
#endif

// ── Forward declarations ───────────────────────────────────────────────────
struct pw_main_loop;
struct pw_context;
struct pw_core;
struct pw_stream;
struct pw_properties;
struct pw_loop;
struct pw_buffer;
struct spa_pod;
struct spa_hook;
struct spa_dict;
struct spa_source;
struct spa_buffer;
struct spa_command;
struct spa_pod_builder;

// ── Stream state ───────────────────────────────────────────────────────────
typedef enum {
    PW_STREAM_STATE_ERROR       = -1,
    PW_STREAM_STATE_UNCONNECTED =  0,
    PW_STREAM_STATE_CONNECTING  =  1,
    PW_STREAM_STATE_PAUSED      =  2,
    PW_STREAM_STATE_STREAMING   =  3,
} pw_stream_state;

struct pw_stream_control { const char* name; uint32_t flags; float def, min, max; const float* values; uint32_t n_values; uint32_t max_values; };

// ── pw_buffer ──────────────────────────────────────────────────────────────
struct pw_buffer {
    struct spa_buffer* buffer;
    void*    user_data;
    uint64_t size;
    uint64_t requested;
};

// ── Stream events (version 2) ──────────────────────────────────────────────
#define PW_VERSION_STREAM_EVENTS 2

struct pw_stream_events {
    uint32_t version;
    void (*destroy)(void* data);
    void (*state_changed)(void* data, pw_stream_state old, pw_stream_state state, const char* error);
    void (*control_info)(void* data, uint32_t id, const struct pw_stream_control* control);
    void (*io_changed)(void* data, uint32_t id, void* area, uint32_t size);
    void (*param_changed)(void* data, uint32_t id, const struct spa_pod* param);
    void (*add_buffer)(void* data, struct pw_buffer* buffer);
    void (*remove_buffer)(void* data, struct pw_buffer* buffer);
    void (*process)(void* data);
    void (*drained)(void* data);
    void (*command)(void* data, const struct spa_command* command);
    void (*trigger_done)(void* data);
};

// ── Constants ──────────────────────────────────────────────────────────────
#define PW_KEY_MEDIA_TYPE     "media.type"
#define PW_KEY_MEDIA_CATEGORY "media.category"
#define PW_KEY_MEDIA_ROLE     "media.role"
#define PW_KEY_APP_NAME       "application.name"
#define PW_KEY_NODE_LATENCY   "node.latency"
#define PW_KEY_TARGET_OBJECT  "target.object"
#define PW_DIRECTION_INPUT    0
#define PW_DIRECTION_OUTPUT   1
#define PW_ID_ANY             ((uint32_t)0xffffffff)
#define PW_STREAM_FLAG_AUTOCONNECT   (1 << 0)
#define PW_STREAM_FLAG_MAP_BUFFERS   (1 << 1)
#define PW_STREAM_FLAG_RT_PROCESS    (1 << 4)

// ── API ────────────────────────────────────────────────────────────────────
void                 pw_init(int* argc, char*** argv);
void                 pw_deinit(void);
struct pw_main_loop* pw_main_loop_new(const struct spa_dict* props);
void                 pw_main_loop_destroy(struct pw_main_loop* loop);
int                  pw_main_loop_run(struct pw_main_loop* loop);
int                  pw_main_loop_quit(struct pw_main_loop* loop);
struct pw_loop*      pw_main_loop_get_loop(struct pw_main_loop* loop);
struct pw_context*   pw_context_new(struct pw_main_loop* loop, struct pw_properties* props, size_t user_data_size);
void                 pw_context_destroy(struct pw_context* ctx);
struct pw_core*      pw_context_connect(struct pw_context* ctx, struct pw_properties* props, size_t user_data_size);
int                  pw_core_disconnect(struct pw_core* core);
struct pw_stream*    pw_stream_new(struct pw_core* core, const char* name, struct pw_properties* props);
void                 pw_stream_destroy(struct pw_stream* stream);
int                  pw_stream_connect(struct pw_stream* stream, int direction, uint32_t target_id, uint32_t flags, const struct spa_pod** params, uint32_t n_params);
int                  pw_stream_disconnect(struct pw_stream* stream);
struct pw_buffer*    pw_stream_dequeue_buffer(struct pw_stream* stream);
int                  pw_stream_queue_buffer(struct pw_stream* stream, struct pw_buffer* buffer);
void                 pw_stream_add_listener(struct pw_stream* stream, struct spa_hook* listener, const struct pw_stream_events* events, void* data);
const char*          pw_stream_state_as_string(pw_stream_state state);
struct pw_properties* pw_properties_new(const char* key, ...);
struct pw_properties* pw_properties_new_dict(const struct spa_dict* dict);
void                 pw_properties_free(struct pw_properties* props);

// ── spa_hook (needed by pw_stream_add_listener) ───────────────────────────
struct spa_hook_list { struct spa_hook* first; };
struct spa_hook {
    struct spa_hook* next;
    struct spa_hook* prev;
    const void*      funcs;
    void*            data;
    struct spa_hook_list* list;
    void (*removed)(struct spa_hook* hook);
};
static inline void spa_hook_list_init(struct spa_hook_list* list) { list->first = NULL; }

#ifdef __cplusplus
}
#endif
