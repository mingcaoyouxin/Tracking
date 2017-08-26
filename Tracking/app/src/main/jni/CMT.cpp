#include "CMT.h"

#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include<android/log.h>
#include <opencv2/core/core.hpp>

namespace cmt {

#ifndef LOG_TAG
#define LOG_TAG "TRACKING_JNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG ,__VA_ARGS__) // 定义LOGD类型
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG ,__VA_ARGS__) // 定义LOGI类型
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,LOG_TAG ,__VA_ARGS__) // 定义LOGW类型
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG ,__VA_ARGS__) // 定义LOGE类型
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL,LOG_TAG ,__VA_ARGS__) // 定义LOGF类型
#endif

void CMT::initialize(const Mat im_gray, const Rect rect)
{
    //FILE_LOG(logDEBUG) << "CMT::initialize() call";

    //Remember initial size
    size_initial = rect.size();

    //Remember initial image
    im_prev = im_gray;

    //Compute center of rect
    Point2f center = Point2f(rect.x + rect.width/2.0, rect.y + rect.height/2.0);

    //Initialize rotated bounding box
    bb_rot = RotatedRect(center, size_initial, 0.0);

    global_match_open = true;
    is_track_valid = true;
    initial_active_points_num = -1;

    //Initialize detector and descriptor
#if CV_MAJOR_VERSION > 2
    detector = cv::FastFeatureDetector::create();
    descriptor = cv::BRISK::create();
#else
    detector = FeatureDetector::create(str_detector);
    descriptor = DescriptorExtractor::create(str_descriptor);
#endif

    //Get initial keypoints in whole image and compute their descriptors
    vector<KeyPoint> keypoints;
    detector->detect(im_gray, keypoints);

    //Divide keypoints into foreground and background keypoints according to selection
    vector<KeyPoint> keypoints_fg;
    vector<KeyPoint> keypoints_bg;

    for (size_t i = 0; i < keypoints.size(); i++)
    {
        KeyPoint k = keypoints[i];
        Point2f pt = k.pt;

        if (pt.x > rect.x && pt.y > rect.y && pt.x < rect.br().x && pt.y < rect.br().y)
        {
            keypoints_fg.push_back(k);
        }

        else
        {
            keypoints_bg.push_back(k);
        }

    }

    //Create foreground classes
    vector<int> classes_fg;
    classes_fg.reserve(keypoints_fg.size());
    for (size_t i = 0; i < keypoints_fg.size(); i++)
    {
        classes_fg.push_back(i);
    }

    //Compute foreground/background features
    Mat descs_fg;
    Mat descs_bg;
    descriptor->compute(im_gray, keypoints_fg, descs_fg);
    descriptor->compute(im_gray, keypoints_bg, descs_bg);

    //Only now is the right time to convert keypoints to points, as compute() might remove some keypoints
    vector<Point2f> points_fg;
    vector<Point2f> points_bg;

    for (size_t i = 0; i < keypoints_fg.size(); i++)
    {
        points_fg.push_back(keypoints_fg[i].pt);
    }

    //FILE_LOG(logDEBUG) << points_fg.size() << " foreground points.";
    LOGE("CMT initialize points_fg.size() :%d\n",points_fg.size());

    for (size_t i = 0; i < keypoints_bg.size(); i++)
    {
        points_bg.push_back(keypoints_bg[i].pt);
    }

    //Create normalized points
    vector<Point2f> points_normalized;
    for (size_t i = 0; i < points_fg.size(); i++)
    {
        points_normalized.push_back(points_fg[i] - center);
    }

    //Initialize matcher
    matcher.initialize(points_normalized, descs_fg, classes_fg, descs_bg, center);

    //Initialize consensus
    consensus.initialize(points_normalized);

    //Create initial set of active keypoints
    for (size_t i = 0; i < keypoints_fg.size(); i++)
    {
        points_active.push_back(keypoints_fg[i].pt);
        classes_active = classes_fg;
    }

    //FILE_LOG(logDEBUG) << "CMT::initialize() return";
}

void CMT::processFrame(Mat im_gray) {

    //FILE_LOG(logDEBUG) << "CMT::processFrame() call";

    //Track keypoints
    double startCTime = now_ms();
    vector<Point2f> points_tracked;
    vector<unsigned char> status;
    tracker.track(im_prev, im_gray, points_active, points_tracked, status);
    LOGD("CMTTIME processFrame  trackerTime :%.3f\n", (now_ms()-startCTime)*1000.0/CLOCKS_PER_SEC);
    //FILE_LOG(logDEBUG) << points_tracked.size() << " tracked points.";
    //LOGD("CMT processFrame points_tracked.size() :%d\n",points_tracked.size());

    //keep only successful classes
    vector<int> classes_tracked;
    for (size_t i = 0; i < classes_active.size(); i++)
    {
        if (status[i])
        {
            classes_tracked.push_back(classes_active[i]);
        }

    }

    double detectTime = now_ms();
    //Detect keypoints, compute descriptors
    vector<KeyPoint> keypoints;
    detector->detect(im_gray, keypoints);

    //FILE_LOG(logDEBUG) << keypoints.size() << " keypoints found.";
    LOGD("CMTTIME processFrame  detectTime keypoints detect %d coss :%.3f\n",keypoints.size(),
         (now_ms()-detectTime)*1000.0/CLOCKS_PER_SEC);

    Mat descriptors;
    double descriptorTime = now_ms();
    descriptor->compute(im_gray, keypoints, descriptors);
    LOGD("CMTTIME processFrame  descriptorTime :%.3f\n", (now_ms()-descriptorTime)*1000.0/CLOCKS_PER_SEC);
    vector<Point2f> points_fused;
    vector<int> classes_fused;

    if(global_match_open) {
        //Match keypoints globally
        double matchGlobalTime = now_ms();
        vector<Point2f> points_matched_global;
        vector<int> classes_matched_global;
        matcher.matchGlobal(keypoints, descriptors, points_matched_global, classes_matched_global);
        LOGE("CMTTIME processFrame points_matched_global.size()= %d matchGlobalTime :%.3f\n",
             points_matched_global.size(), (now_ms()-matchGlobalTime)*1000.0/CLOCKS_PER_SEC);
        //FILE_LOG(logDEBUG) << points_matched_global.size() << " points matched globally.";

        //Fuse tracked and globally matched points
        double fusionTime = now_ms();
        fusion.preferFirst(points_tracked, classes_tracked, points_matched_global,
                           classes_matched_global,
                           points_fused, classes_fused);
        LOGE("CMTTIME processFrame points_fused.size()= %d fusionTime :%.3f\n",
             points_fused.size(), (now_ms()-fusionTime)*1000.0/CLOCKS_PER_SEC);
        //FILE_LOG(logDEBUG) << points_fused.size() << " points fused.";
    } else {
        points_fused = points_tracked;
        classes_fused = classes_tracked;
    }

    double matchLocalTime = now_ms();
    //Estimate scale and rotation from the fused points
    float scale;
    float rotation;
    consensus.estimateScaleRotation(points_fused, classes_fused, scale, rotation);

    //FILE_LOG(logDEBUG) << "scale " << scale << ", " << "rotation " << rotation;

    //Find inliers and the center of their votes
    Point2f center;
    vector<Point2f> points_inlier;
    vector<int> classes_inlier;
    consensus.findConsensus(points_fused, classes_fused, scale, rotation,
            center, points_inlier, classes_inlier);

    //FILE_LOG(logDEBUG) << points_inlier.size() << " inlier points.";
    //FILE_LOG(logDEBUG) << "center " << center;

    //Match keypoints locally
    vector<Point2f> points_matched_local;
    vector<int> classes_matched_local;
    matcher.matchLocal(keypoints, descriptors, center, scale, rotation, points_matched_local, classes_matched_local);

    //FILE_LOG(logDEBUG) << points_matched_local.size() << " points matched locally.";
    LOGE("CMTTIME processFrame points_matched_local.size()= %d matchLocalTime :%.3f\n",
         points_matched_local.size(), (now_ms()-matchLocalTime)*1000.0/CLOCKS_PER_SEC);

    //Clear active points
    points_active.clear();
    classes_active.clear();

    //Fuse locally matched points and inliers
    fusion.preferFirst(points_matched_local, classes_matched_local, points_inlier, classes_inlier, points_active, classes_active);
//    points_active = points_fused;
//    classes_active = classes_fused;

    //FILE_LOG(logDEBUG) << points_active.size() << " final fused points.";

    //TODO: Use theta to suppress result
    bb_rot = RotatedRect(center,  size_initial * scale, rotation/CV_PI * 180);

    //Remember current image
    im_prev = im_gray;

    if(initial_active_points_num < 0){
        initial_active_points_num = points_active.size();
    }

    //satyTest
    global_match_open = center.x - bb_rot.size.width / 2  < 0
                        || center.x + bb_rot.size.width / 2 > im_prev.cols
                        || center.y - bb_rot.size.height / 2 < 0
                        || center.y + bb_rot.size.height / 2 > im_prev.rows
                        || points_active.size() < initial_active_points_num/3;

    is_track_valid = !global_match_open;

    LOGD("CMTTIME processFrame:%.3f\n",(now_ms()-startCTime)*1000.0/CLOCKS_PER_SEC);
    //FILE_LOG(logDEBUG) << "CMT::processFrame() return";
}

} /* namespace CMT */
