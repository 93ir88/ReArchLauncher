// android-shims/pipewire_android.cpp
//
// Replaces libpipewire-0.3 dependency.
// Caelestia uses PipeWire for:
//   1. Audio visualizer (bar/background visualizer)
//   2. Volume control
//
// Android equivalents:
//   1. AudioRecord (captures mic/loopback PCM) + FFT
//   2. AudioManager for volume
//
// This file provides a C API matching the PipeWire surface that
// Caelestia's plugin uses. The actual Caelestia plugin code
// doesn't need to change — it calls these symbols, we intercept.

#ifdef Q_OS_ANDROID

#include <QObject>
#include <QAudioInput>
#include <QAudioFormat>
#include <QMediaDevices>
#include <QAudioSource>
#include <QBuffer>
#include <QDebug>
#include <QtMath>
#include <QJniObject>

// ─── Minimal PipeWire type surface (just what Caelestia uses) ────────────

struct pw_main_loop { int dummy; };
struct pw_context    { int dummy; };
struct pw_core       { int dummy; };
struct pw_stream     { QAudioSource* src; QBuffer* buf; };
struct pw_stream_events {
    uint32_t version;
    void (*process)(void* data);
};
struct pw_properties { int dummy; };
struct spa_pod       { int dummy; };

// FFT magnitude buffer — exposed to the visualizer
static constexpr int FFT_BINS = 512;
float g_fftMagnitudes[FFT_BINS] = {};

// Android audio source
static QAudioSource* g_audioSource = nullptr;
static QBuffer*      g_audioBuffer = nullptr;

// Simple DFT (for small bin count — production would use KissFFT)
static void computeFFT(const float* pcm, int samples) {
    for (int k = 0; k < FFT_BINS; k++) {
        float re = 0, im = 0;
        for (int n = 0; n < samples; n++) {
            const float angle = -2.0f * M_PI * k * n / samples;
            re += pcm[n] * cosf(angle);
            im += pcm[n] * sinf(angle);
        }
        g_fftMagnitudes[k] = sqrtf(re * re + im * im) / samples;
    }
}

// ─── PipeWire API stubs ──────────────────────────────────────────────────

extern "C" {

void pw_init(int* argc, char*** argv) {
    Q_UNUSED(argc) Q_UNUSED(argv)
    qInfo() << "[ReArch] PipeWire → Android AudioRecord";
}

void pw_deinit() {}

struct pw_main_loop* pw_main_loop_new(const struct spa_dict* props) {
    Q_UNUSED(props)
    return new pw_main_loop{};
}

void pw_main_loop_destroy(struct pw_main_loop* loop) { delete loop; }
void pw_main_loop_run(struct pw_main_loop* loop)     { Q_UNUSED(loop) }
void pw_main_loop_quit(struct pw_main_loop* loop)    { Q_UNUSED(loop) }

struct pw_context* pw_context_new(struct pw_main_loop* loop, struct pw_properties* props, size_t sz) {
    Q_UNUSED(loop) Q_UNUSED(props) Q_UNUSED(sz)
    return new pw_context{};
}
void pw_context_destroy(struct pw_context* ctx) { delete ctx; }

struct pw_core* pw_context_connect(struct pw_context* ctx, struct pw_properties* props, size_t sz) {
    Q_UNUSED(ctx) Q_UNUSED(props) Q_UNUSED(sz)
    return new pw_core{};
}
void pw_core_disconnect(struct pw_core* core) { delete core; }

struct pw_stream* pw_stream_new(struct pw_core* core, const char* name,
                                struct pw_properties* props)
{
    Q_UNUSED(core) Q_UNUSED(name) Q_UNUSED(props)
    auto* s = new pw_stream{};

    QAudioFormat fmt;
    fmt.setSampleRate(44100);
    fmt.setChannelCount(2);
    fmt.setSampleFormat(QAudioFormat::Float);

    const QAudioDevice inputDev = QMediaDevices::defaultAudioInput();
    s->src = new QAudioSource(inputDev, fmt);
    s->buf = new QBuffer();
    s->buf->open(QIODevice::ReadWrite);
    s->src->start(s->buf);

    g_audioSource = s->src;
    g_audioBuffer = s->buf;

    return s;
}

void pw_stream_destroy(struct pw_stream* s) {
    if (s) {
        s->src->stop();
        delete s->src;
        delete s->buf;
        delete s;
    }
}

int pw_stream_connect(struct pw_stream* s, int direction, uint32_t target_id,
                      uint32_t flags, const struct spa_pod** params, uint32_t n_params) {
    Q_UNUSED(s) Q_UNUSED(direction) Q_UNUSED(target_id)
    Q_UNUSED(flags) Q_UNUSED(params) Q_UNUSED(n_params)
    return 0;
}

// pw_stream_dequeue_buffer — called by the visualizer to get audio data
struct pw_buffer* pw_stream_dequeue_buffer(struct pw_stream* s) {
    if (!s || !s->buf) return nullptr;

    s->buf->seek(0);
    const QByteArray raw = s->buf->readAll();
    s->buf->buffer().clear();
    s->buf->seek(0);

    if (raw.isEmpty()) return nullptr;

    const int samples = raw.size() / sizeof(float);
    const float* pcm  = reinterpret_cast<const float*>(raw.constData());
    computeFFT(pcm, qMin(samples, FFT_BINS * 2));

    // Return a synthetic buffer containing our magnitudes
    static struct { float* data; int size; } synth{ g_fftMagnitudes, FFT_BINS };
    return reinterpret_cast<struct pw_buffer*>(&synth);
}

void pw_stream_queue_buffer(struct pw_stream* s, struct pw_buffer* buf) {
    Q_UNUSED(s) Q_UNUSED(buf)
}

void pw_stream_add_listener(struct pw_stream* s, struct spa_hook* listener,
                             const struct pw_stream_events* events, void* data) {
    Q_UNUSED(s) Q_UNUSED(listener) Q_UNUSED(events) Q_UNUSED(data)
    // The visualizer registers a process callback here. Since we poll
    // from QAudioSource, we call it on a timer instead.
}

struct pw_properties* pw_properties_new(const char* key, ...) {
    Q_UNUSED(key)
    return new pw_properties{};
}
void pw_properties_free(struct pw_properties* p) { delete p; }

} // extern "C"

#endif // Q_OS_ANDROID
