#ifndef COMMON_H

#define COMMON_H

#include <limits>
#include <string>
#include <vector>

#include <opencv2/core/core.hpp>

#include "logging/log.h"

using cv::Mat;
using cv::Point2f;
using cv::Rect;
using cv::Size2f;
using std::numeric_limits;
using std::string;
using std::vector;

namespace cmt
{
    float median(vector<float> & A);
    Point2f rotate(const Point2f v, const float angle);
    template<class T>
    int sgn(T x)
    {
        if (x >=0) return 1;
        else return -1;
    }

#ifndef LOG_TAG
#define LOG_TAG "TRACKING_JNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG ,__VA_ARGS__) // ����LOGD����
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG ,__VA_ARGS__) // ����LOGI����
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,LOG_TAG ,__VA_ARGS__) // ����LOGW����
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG ,__VA_ARGS__) // ����LOGE����
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL,LOG_TAG ,__VA_ARGS__) // ����LOGF����
#endif
} /* namespace cmt */

extern "C" {

static double now_ms(void) {
    struct timespec res;
    clock_gettime(CLOCK_MONOTONIC, &res);
    return (1000.0 * res.tv_sec + (double) res.tv_nsec / 1e6)*1000;
}

}


#endif /* end of include guard: COMMON_H */
