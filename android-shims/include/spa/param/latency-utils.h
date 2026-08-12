#pragma once
// Android stub — spa/param/latency-utils.h

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

struct spa_pod;
struct spa_pod_builder;

struct spa_latency_info {
    uint32_t direction;
    uint64_t min_quantum;
    uint64_t max_quantum;
    uint64_t min_rate;
    uint64_t max_rate;
    uint64_t min_ns;
    uint64_t max_ns;
};

static inline struct spa_pod* spa_latency_build(struct spa_pod_builder* b, uint32_t id, const struct spa_latency_info* info) {
    (void)b; (void)id; (void)info; return NULL;
}
static inline int spa_latency_parse(const struct spa_pod* pod, struct spa_latency_info* info) {
    (void)pod; (void)info; return -1;
}

#ifdef __cplusplus
}
#endif
