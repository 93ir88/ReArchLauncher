#pragma once
// Android stub — aubio/aubio.h
// BPM detection replaced by no-op. BeatTracker QML element still registers
// but bpm() always returns 0 and beat() never fires on Android.

#include <stdint.h>
#include <stdlib.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef float  smpl_t;
typedef double dmpl_t;
typedef unsigned int uint_t;
typedef int sint_t;

// ── fvec_t ─────────────────────────────────────────────────────────────────
typedef struct {
    uint_t  length;
    smpl_t* data;
} fvec_t;

static inline fvec_t* new_fvec(uint_t length) {
    fvec_t* v = (fvec_t*)malloc(sizeof(fvec_t));
    if (!v) return NULL;
    v->length = length;
    v->data   = (smpl_t*)calloc(length, sizeof(smpl_t));
    return v;
}
static inline void del_fvec(fvec_t* v) {
    if (v) { free(v->data); free(v); }
}
static inline smpl_t fvec_get_sample(const fvec_t* v, uint_t i) {
    return (v && i < v->length) ? v->data[i] : 0.0f;
}

// ── aubio_tempo_t ──────────────────────────────────────────────────────────
typedef struct _aubio_tempo_t aubio_tempo_t;

static inline aubio_tempo_t* new_aubio_tempo(
    const char* method, uint_t buf_size, uint_t hop_size, uint_t samplerate)
{
    (void)method; (void)buf_size; (void)hop_size; (void)samplerate;
    return (aubio_tempo_t*)malloc(1); // non-null sentinel
}
static inline void del_aubio_tempo(aubio_tempo_t* t) { free(t); }
static inline void aubio_tempo_do(aubio_tempo_t* t, fvec_t* input, fvec_t* output) {
    (void)t; (void)input;
    if (output && output->length > 0) output->data[0] = 0.0f;
}
static inline smpl_t aubio_tempo_get_bpm(const aubio_tempo_t* t) { (void)t; return 0.0f; }
static inline smpl_t aubio_tempo_get_confidence(const aubio_tempo_t* t) { (void)t; return 0.0f; }
static inline uint_t aubio_tempo_get_last(const aubio_tempo_t* t) { (void)t; return 0; }
static inline smpl_t aubio_tempo_get_period_s(const aubio_tempo_t* t) { (void)t; return 0.0f; }
static inline uint_t aubio_tempo_set_threshold(aubio_tempo_t* t, smpl_t threshold) { (void)t; (void)threshold; return 0; }
static inline uint_t aubio_tempo_set_silence(aubio_tempo_t* t, smpl_t silence) { (void)t; (void)silence; return 0; }

#ifdef __cplusplus
}
#endif
