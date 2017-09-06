#include "CMT.h"

#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include<android/log.h>
#include <opencv2/core/core.hpp>
#include <opencv2/core/version.hpp>

namespace cmt {

void CMT::initialize(const Mat im_gray, const cv::Rect rect)
{

    //Remember initial size 存储跟踪区域的初始大小
    size_initial = rect.size();

    //Remember initial image 存储初始灰度图像
    im_prev = im_gray;

    //Compute center of rect 计算跟踪区域的中心位置
    Point2f center = Point2f(rect.x + rect.width/2.0, rect.y + rect.height/2.0);

    //Initialize rotated bounding box
    bb_rot = RotatedRect(center, size_initial, 0.0);

    global_match_open = true;
    is_track_valid = true;
    initial_active_points_num = -1;

    //Initialize detector and descriptor
#if (CV_MAJOR_VERSION >= 3 && CV_VERSION_MINOR >=2) || (CV_MAJOR_VERSION >= 4)
    // opencv 3.2 以上版本
    detector = cv::FastFeatureDetector::create(1);
    descriptor = cv::BRISK::create();
#else
    //Initialize detector and descriptor 初始化检测器FAST和描述器BRISK
    detector = FeatureDetector::create(str_detector);
    descriptor = DescriptorExtractor::create(str_descriptor);

#endif

    //Get initial keypoints in whole image and compute their descriptors
    vector<KeyPoint> keypoints;
    detector->detect(im_gray, keypoints); // 检测初始图像的所有关键点

    // 固定特征点数量
    int expectedKeyPointNum = 50;
    int expectedThreshold = 1;
    const size_t numSize = 100;
    int num[numSize + 1] = {0};
    int fgNum = 0;

    for(size_t i = 0; i < keypoints.size(); i++) {
        KeyPoint k = keypoints[i];
        Point2f pt = k.pt;

        if (pt.x > rect.x && pt.y > rect.y && pt.x < rect.br().x && pt.y < rect.br().y) {
            fgNum++;
            int r = (int) keypoints[i].response;
            if (r > 0 && r < numSize) {
                num[r]++;
            } else if (r >= numSize) {
                num[numSize]++;
            } else {
                num[0]++;
            }
        }
    }
    if(fgNum > expectedKeyPointNum) {
        int sum = 0;
        for (size_t i = 0; i < numSize + 1; i++) {
            sum += num[i];
            if (sum > fgNum - expectedKeyPointNum) {
                expectedThreshold = i;
                break;
            }
        }

        detector->clear();
        keypoints.clear();
        detector = cv::FastFeatureDetector::create(expectedThreshold);
        detector->detect(im_gray, keypoints);
    }
    //Divide keypoints into foreground and background keypoints according to selection 分离出前景和背景的关键点，前景即跟踪框内
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

    //Create foreground classes 创建前景类
    vector<int> classes_fg;
    classes_fg.reserve(keypoints_fg.size());
    for (size_t i = 0; i < keypoints_fg.size(); i++)
    {
        classes_fg.push_back(i);
    }

    //Compute foreground/background features 计算前景和背景的特征
    Mat descs_fg;
    Mat descs_bg;
    descriptor->compute(im_gray, keypoints_fg, descs_fg);
    descriptor->compute(im_gray, keypoints_bg, descs_bg);

    //Only now is the right time to convert keypoints to points, as compute() might remove some keypoints 将关键点转换为点存储
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

    //Create normalized points 创建正规化的点，即计算前景的关键点到前景框中的相对位置作为正规化的点的坐标
    vector<Point2f> points_normalized;
    for (size_t i = 0; i < points_fg.size(); i++)
    {
        points_normalized.push_back(points_fg[i] - center);
    }

    //Initialize matcher 初始化匹配器
    matcher.initialize(points_normalized, descs_fg, classes_fg, descs_bg, center);

    //Initialize consensus 初始化一致器
    consensus.initialize(points_normalized);

    //Create initial set of active keypoints 创建初始的活动点，即前景关键点的坐标
    for (size_t i = 0; i < keypoints_fg.size(); i++)
    {
        points_active.push_back(keypoints_fg[i].pt);
        classes_active = classes_fg;
    }
}

void CMT::processFrame(Mat im_gray) {
    //Track keypoints
    double startCTime = now_ms();
    vector<Point2f> points_tracked;
    vector<unsigned char> status;
    // 利用光流法计算关键点的当前位置。
    tracker.track(im_prev, im_gray, points_active, points_tracked, status);
    LOGD("CMTTIME processFrame points_tracked.size() %d,  trackerTime :%.3f\n", points_tracked.size(), (now_ms()-startCTime)*1000.0/CLOCKS_PER_SEC);
    //FILE_LOG(logDEBUG) << points_tracked.size() << " tracked points.";

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
        //Match keypoints globally 在全局和之前的数据库匹配特征点，计算出匹配的特征点
        double matchGlobalTime = now_ms();
        vector<Point2f> points_matched_global;
        vector<int> classes_matched_global;
        matcher.matchGlobal(keypoints, descriptors, points_matched_global, classes_matched_global);
        LOGE("CMTTIME processFrame points_matched_global.size()= %d matchGlobalTime :%.3f\n",
             points_matched_global.size(), (now_ms()-matchGlobalTime)*1000.0/CLOCKS_PER_SEC);
        //FILE_LOG(logDEBUG) << points_matched_global.size() << " points matched globally.";

        //Fuse tracked and globally matched points
        //融合跟踪和匹配的点
        double fusionTime = now_ms();
        fusion.preferFirst(points_tracked, classes_tracked, points_matched_global,
                           classes_matched_global,
                           points_fused, classes_fused);
        LOGE("CMTTIME processFrame points_fused.size()= %d \n", points_fused.size());
        //FILE_LOG(logDEBUG) << points_fused.size() << " points fused.";
    } else {
        points_fused = points_tracked;
        classes_fused = classes_tracked;
    }

    // 估计旋转和缩放利用最终的融合点
    //Estimate scale and rotation from the fused points
    double matchLocalTime = now_ms();
    float scale;
    float rotation;
    consensus.estimateScaleRotation(points_fused, classes_fused, scale, rotation);

    //FILE_LOG(logDEBUG) << "scale " << scale << ", " << "rotation " << rotation;

    //Find inliers and the center of their votes
    //计算一致性，获取相关的点inliers和中心
    Point2f center;
    vector<Point2f> points_inlier;
    vector<int> classes_inlier;
    consensus.findConsensus(points_fused, classes_fused, scale, rotation,
            center, points_inlier, classes_inlier);

    //FILE_LOG(logDEBUG) << points_inlier.size() << " inlier points.";
    //FILE_LOG(logDEBUG) << "center " << center;

    //Match keypoints locally 局部匹配
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
    // 融合局部匹配的点和inliers
    fusion.preferFirst(points_matched_local, classes_matched_local, points_inlier, classes_inlier, points_active, classes_active);
//    points_active = points_fused;
//    classes_active = classes_fused;

    //FILE_LOG(logDEBUG) << points_active.size() << " final fused points.";
    LOGI("CMTTIME processFrame final fused points :%d, initial_active_points_num = :%d \n",
         points_active.size(), initial_active_points_num);

    //TODO: Use theta to suppress result
    // 计算出新的跟踪窗口
    bb_rot = RotatedRect(center,  size_initial * scale, rotation/CV_PI * 180);

    //Remember current image 更新上一帧图像
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
//    global_match_open = true;
//
//    is_track_valid = true;

    LOGD("CMTTIME processFrame:%.3f\n",(now_ms()-startCTime)*1000.0/CLOCKS_PER_SEC);
}

} /* namespace CMT */
