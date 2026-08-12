#pragma once
// Android stub — cava/cavacore.h
// Audio visualizer replaced by FFT from Qt Multimedia (pipewire_android.cpp).
// CavaProvider QML element still registers but valuesChanged() fires zeros.

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

#define CAVA_BARS_MAX 512

// Audio input formats
typedef enum {
    CAVA_STEREO       = 2,
    CAVA_MONO         = 1,
    CAVA_MONO_LEFT    = 3,
    CAVA_MONO_RIGHT   = 4,
    CAVA_MONO_MIX     = 5,
} cava_channels_t;

// cava config passed to cava_init
struct cava_config {
    int    bars;
    int    autosens;
    int    overshoot;
    int    channels;
    int    sample_rate;
    int    low_cutoff_freq;
    int    high_cutoff_freq;
    double noise_reduction;
    double samplerate;
};

// Opaque plan
struct cava_plan {
    int    number_of_bars;
    double bass_cut_off_bar;
    double treble_cut_off_bar;
    int    input_buffer_size;
    int    fft_buffer_size;
    void*  _internal;
};

// ── API stubs — all no-ops on Android ─────────────────────────────────────

static inline struct cava_plan* cava_init(
    int bars, double rate, int channels, int autosens,
    double noise_reduction, int low_cut_off, int high_cut_off)
{
    (void)bars; (void)rate; (void)channels; (void)autosens;
    (void)noise_reduction; (void)low_cut_off; (void)high_cut_off;
    struct cava_plan* p = (struct cava_plan*)calloc(1, sizeof(struct cava_plan));
    if (p) p->number_of_bars = bars;
    return p;
}

static inline void cava_execute(
    double* cava_in, int new_samples,
    double* cava_out, struct cava_plan* plan)
{
    (void)cava_in; (void)new_samples; (void)plan;
    // zero output — Android audio visualizer uses pipewire_android.cpp FFT instead
    if (cava_out && plan)
        for (int i = 0; i < plan->number_of_bars; i++)
            cava_out[i] = 0.0;
}

static inline void cava_destroy(struct cava_plan* plan) {
    free(plan);
}

#ifdef __cplusplus
}
#endif
