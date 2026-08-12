#pragma once
// Android stub — spa/param/audio/format-utils.h

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

struct spa_pod;
struct spa_pod_builder;

typedef enum {
    SPA_AUDIO_FORMAT_UNKNOWN = 0,
    SPA_AUDIO_FORMAT_S8      = 1,
    SPA_AUDIO_FORMAT_U8      = 2,
    SPA_AUDIO_FORMAT_S16     = 3,
    SPA_AUDIO_FORMAT_U16     = 4,
    SPA_AUDIO_FORMAT_S32     = 5,
    SPA_AUDIO_FORMAT_F32     = 11,
    SPA_AUDIO_FORMAT_F64     = 13,
    SPA_AUDIO_FORMAT_S16_LE  = SPA_AUDIO_FORMAT_S16,
} spa_audio_format;

#define SPA_AUDIO_MAX_CHANNELS 64

struct spa_audio_info_raw {
    uint32_t format;
    uint32_t flags;
    uint32_t rate;
    uint32_t channels;
    uint32_t position[SPA_AUDIO_MAX_CHANNELS];
};

struct spa_audio_info {
    uint32_t media_subtype;
    union {
        struct spa_audio_info_raw raw;
    } info;
};

static inline int spa_format_audio_raw_parse(const struct spa_pod* format, struct spa_audio_info_raw* info) {
    (void)format; (void)info; return -1;
}
static inline struct spa_pod* spa_format_audio_raw_build(struct spa_pod_builder* b, uint32_t id, struct spa_audio_info_raw* info) {
    (void)b; (void)id; (void)info; return NULL;
}

#ifdef __cplusplus
}
#endif
