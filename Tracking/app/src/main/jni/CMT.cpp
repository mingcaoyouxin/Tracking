#include "CMT.h"
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include<android/log.h>

namespace cmt {

void CMT::initialize(const Mat im_gray, const cv::Rect rect)
{
    //FILE_LOG(logDEBUG) << "CMT::initialize() call";
    openFeaturesControl = true;
    openGlobalControl = true;
    //Remember initial size
    size_initial = rect.size();

    //Remember initial image �洢��ʼ�Ҷ�ͼ��
    im_prev = im_gray;

    //Compute center of rect ����������������λ��
    Point2f center = Point2f(rect.x + rect.width/2.0, rect.y + rect.height/2.0);

    //Initialize rotated bounding box
    bb_rot = RotatedRect(center, size_initial, 0.0);

    global_match_open = true;
    is_track_valid = true;
    initial_active_points_num = -1;

    int initial_feature_threshold;
    if(openFeaturesControl) {
        initial_feature_threshold = 1;
    } else {
        initial_feature_threshold = 10;
    }

    //Initialize detector and descriptor
#if (CV_MAJOR_VERSION >= 3 && CV_VERSION_MINOR >=2) || (CV_MAJOR_VERSION >= 4)
    // opencv 3.2 ���ϰ汾
    detector = cv::FastFeatureDetector::create(initial_feature_threshold);
    descriptor = cv::BRISK::create();

    //detector = cv::ORB::create();
    //descriptor = cv::ORB::create();
#else
    //Initialize detector and descriptor ��ʼ�������FAST��������BRISK
    detector = FeatureDetector::create(str_detector);
    descriptor = DescriptorExtractor::create(str_descriptor);

#endif

    //Get initial keypoints in whole image and compute their descriptors
    vector<KeyPoint> keypoints;
    detector->detect(im_gray, keypoints); // ����ʼͼ������йؼ���

    if(openFeaturesControl) {
        int expectedKeyPointNum = 50;
        int expectedThreshold = 1;
        const size_t numSize = 200;
        int num[numSize + 1] = {0};
        int fgNum = 0;

        for (size_t i = 0; i < keypoints.size(); i++) {
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
        if (fgNum > expectedKeyPointNum) {
            int sum = 0;
            for (size_t i = 0; i < numSize + 1; i++) {
                sum += num[i];
                if (sum > fgNum - expectedKeyPointNum) {
                    expectedThreshold = i;
                    break;
                }
            }
            LOGE("expectedThreshold = %d\n", expectedThreshold);
            detector->clear();
            keypoints.clear();
            detector = cv::FastFeatureDetector::create(expectedThreshold);
            detector->detect(im_gray, keypoints);
        }
    }



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

    //Create foreground classes ����ǰ����
    vector<int> classes_fg;
    classes_fg.reserve(keypoints_fg.size());
    for (size_t i = 0; i < keypoints_fg.size(); i++)
    {
        classes_fg.push_back(i);
    }

    //Compute foreground/background features ����ǰ���ͱ���������
    Mat descs_fg;
    Mat descs_bg;
    descriptor->compute(im_gray, keypoints_fg, descs_fg);
    descriptor->compute(im_gray, keypoints_bg, descs_bg);

    //Only now is the right time to convert keypoints to points, as compute() might remove some keypoints ���ؼ���ת��Ϊ��洢
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

    //Create normalized points �������滯�ĵ㣬������ǰ���Ĺؼ��㵽ǰ�����е����λ����Ϊ���滯�ĵ������
    vector<Point2f> points_normalized;
    for (size_t i = 0; i < points_fg.size(); i++)
    {
        points_normalized.push_back(points_fg[i] - center);
    }

    //Initialize matcher ��ʼ��ƥ����
    matcher.initialize(points_normalized, descs_fg, classes_fg, descs_bg, center);

    //Initialize consensus ��ʼ��һ����
    consensus.initialize(points_normalized);

    //Create initial set of active keypoints ������ʼ�Ļ�㣬��ǰ���ؼ��������
    for (size_t i = 0; i < keypoints_fg.size(); i++)
    {
        points_active.push_back(keypoints_fg[i].pt);
        classes_active = classes_fg;
    }
    initial_active_points_num = points_active.size();
}

void CMT::processFrame(Mat im_gray) {
    //Track keypoints
    Rect preObjRect = bb_rot.boundingRect2f();
    LOGD("CMTRectWatch bb_rot.center:%f,%f  boundingRect(x,y,w,h):%d,%d,%d,%d",bb_rot.center.x,bb_rot.center.y,preObjRect.x,preObjRect.y,preObjRect.width,preObjRect.height);
    cv::Range rcols,rrows;
    rcols.start = (int)(preObjRect.x > 0.5 * preObjRect.width? preObjRect.x-0.5 * preObjRect.width : 0);
    rcols.end = (int)(preObjRect.x + 1.5 * preObjRect.width > im_gray.cols - 1? im_gray.cols-1 : preObjRect.x + 1.5 * preObjRect.width);
    rrows.start = (int)(preObjRect.y > 0.5 * preObjRect.height? preObjRect.y-0.5 * preObjRect.height : 0);
    rrows.end = (int)(preObjRect.y + 1.5 * preObjRect.height > im_gray.rows - 1? im_gray.rows-1 : preObjRect.y + 1.5 * preObjRect.height);
    Mat im_prev_patch(im_prev,rrows,rcols);
    Mat im_gray_patch(im_gray,rrows,rcols);
    LOGD("CMTRectWatch imgray:%d,%d  imgray_patch:%d,%d  rcols:%d,%d  rrows:%d,%d",
         im_gray.cols,im_gray.rows,im_gray_patch.cols,im_gray_patch.rows,rcols.start,rcols.end,rrows.start,rrows.end);
    double startCTime = now_ms();
    vector<Point2f> points_tracked;
    vector<unsigned char> status;
    // ���ù���������ؼ���ĵ�ǰλ�á�
    vector<Point2f> points_active_relative;
    Point2f patchCornerPoint = Point2f(rcols.start,rrows.start);
    for(Point2f point:points_active){
        Point2f pr = point - patchCornerPoint;
        points_active_relative.push_back(pr);
    }
    vector<Point2f> points_tracked_relative;
    tracker.track(im_prev_patch, im_gray_patch, points_active_relative, points_tracked_relative, status);
//    tracker.track(im_prev_patch, im_gray_patch, points_active, points_tracked, status);
    LOGD("CMTTIME processFrame points_active.size() %d, points_tracked.size() %d,  trackerTime :%.3f\n",  points_active.size(), points_tracked_relative.size(), (now_ms()-startCTime)*1000.0/CLOCKS_PER_SEC);
    //FILE_LOG(logDEBUG) << points_tracked.size() << " tracked points.";
    points_tracked.clear();
    for(Point2f point:points_tracked_relative){
        Point2f pr = point + patchCornerPoint;
        points_tracked.push_back(pr);
    }
    //keep only successful classes
    vector<int> classes_tracked;
    for (size_t i = 0; i < classes_active.size(); i++)
    {
        if (status[i])
        {
            classes_tracked.push_back(classes_active[i]);
        }

    }


    //Detect keypoints, compute descriptors
    vector<KeyPoint> keypoints;
    detector->clear();
    descriptor->clear();
    Mat descriptors;
    vector<Point2f> points_fused;
    vector<int> classes_fused;




    if(global_match_open) {
        double detectTime = now_ms();
        detector->detect(im_gray, keypoints);

        //FILE_LOG(logDEBUG) << keypoints.size() << " keypoints found.";
        LOGD("CMTTIME processFrame  detectTime keypoints detect %d coss :%.3f\n",keypoints.size(),
             (now_ms()-detectTime)*1000.0/CLOCKS_PER_SEC);


        double descriptorTime = now_ms();
        descriptor->compute(im_gray, keypoints, descriptors);
        LOGD("CMTTIME processFrame descriptorTime :%.3f\n", (now_ms()-descriptorTime)*1000.0/CLOCKS_PER_SEC);

        //Match keypoints globally ��ȫ�ֺ�֮ǰ�����ݿ�ƥ�������㣬�����ƥ���������
        double matchGlobalTime = now_ms();
        vector<Point2f> points_matched_global;
        vector<int> classes_matched_global;
        matcher.matchGlobal(keypoints, descriptors, points_matched_global, classes_matched_global);
        LOGE("CMTTIME processFrame points_matched_global.size()= %d matchGlobalTime :%.3f\n",
             points_matched_global.size(), (now_ms()-matchGlobalTime)*1000.0/CLOCKS_PER_SEC);
        //FILE_LOG(logDEBUG) << points_matched_global.size() << " points matched globally.";

        //Fuse tracked and globally matched points
        //�ںϸ��ٺ�ƥ��ĵ�
        double fusionTime = now_ms();
        fusion.preferFirst(points_tracked, classes_tracked, points_matched_global,
                           classes_matched_global,
                           points_fused, classes_fused);
        LOGE("CMTTIME processFrame points_fused.size()= %d \n", points_fused.size());
        //FILE_LOG(logDEBUG) << points_fused.size() << " points fused.";
    } else {
        double detectTime = now_ms();
        detector->detect(im_gray_patch, keypoints);
        //FILE_LOG(logDEBUG) << keypoints.size() << " keypoints found.";
        LOGD("CMTTIME processFrame  detectTime keypoints detect %d coss :%.3f\n",keypoints.size(),
             (now_ms()-detectTime)*1000.0/CLOCKS_PER_SEC);

        double descriptorTime = now_ms();
        descriptor->compute(im_gray_patch, keypoints, descriptors);
        LOGD("CMTTIME processFrame descriptorTime :%.3f\n", (now_ms()-descriptorTime)*1000.0/CLOCKS_PER_SEC);
        for(size_t i = 0;i<keypoints.size();i++){
            keypoints.at(i).pt.x += rcols.start;
            keypoints.at(i).pt.y += rrows.start;
        }
        points_fused = points_tracked;
        classes_fused = classes_tracked;
    }

    // ������ת�������������յ��ںϵ�
    //Estimate scale and rotation from the fused points
    double matchLocalTime = now_ms();
    float scale;
    float rotation;
    consensus.estimateScaleRotation(points_fused, classes_fused, scale, rotation);

    //FILE_LOG(logDEBUG) << "scale " << scale << ", " << "rotation " << rotation;

    //Find inliers and the center of their votes
    //����һ���ԣ���ȡ��صĵ�inliers������
    Point2f center;
    vector<Point2f> points_inlier;
    vector<int> classes_inlier;
    consensus.findConsensus(points_fused, classes_fused, scale, rotation,
            center, points_inlier, classes_inlier);

    //FILE_LOG(logDEBUG) << points_inlier.size() << " inlier points.";
    //FILE_LOG(logDEBUG) << "center " << center;

    //Match keypoints locally �ֲ�ƥ��
    vector<Point2f> points_matched_local;
    vector<int> classes_matched_local;
    matcher.matchLocal(keypoints, descriptors, center, scale, rotation, points_matched_local, classes_matched_local);

    //FILE_LOG(logDEBUG) << points_matched_local.size() << " points matched locally.";
    LOGD("CMTTIME processFrame points_matched_local.size()= %d matchLocalTime :%.3f\n",
         points_matched_local.size(), (now_ms()-matchLocalTime)*1000.0/CLOCKS_PER_SEC);

    if(openGlobalControl){
        vector<Point2f> temp_points_active;
        vector<int> temp_classes_active;
        fusion.preferFirst(points_matched_local, classes_matched_local, points_inlier, classes_inlier, temp_points_active, temp_classes_active);
        LOGI("CMTTIME processFrame final fused points :%d, initial_active_points_num = :%d \n",
             temp_points_active.size(), initial_active_points_num);
        is_track_valid = !(center.x - bb_rot.size.width / 2 < 0
                           || center.x + bb_rot.size.width / 2 > im_prev.cols
                           || center.y - bb_rot.size.height / 2 < 0
                           || center.y + bb_rot.size.height / 2 > im_prev.rows
                           || temp_points_active.size() < initial_active_points_num / 3);


        global_match_open = !is_track_valid;
        if(is_track_valid) {
            bb_rot = RotatedRect(center,  size_initial * scale, rotation/CV_PI * 180);
            im_prev = im_gray;
            points_active.clear();
            classes_active.clear();
            points_active = temp_points_active;
            classes_active = temp_classes_active;
        }

    } else {
    //Clear active points
        points_active.clear();
        classes_active.clear();

        //Fuse locally matched points and inliers
        // �ںϾֲ�ƥ��ĵ��inliers
        fusion.preferFirst(points_matched_local, classes_matched_local, points_inlier, classes_inlier, points_active, classes_active);
    //    points_active = points_fused;
    //    classes_active = classes_fused;

    //FILE_LOG(logDEBUG) << points_active.size() << " final fused points.";
        LOGI("CMTTIME processFrame final fused points :%d, initial_active_points_num = :%d \n",
             points_active.size(), initial_active_points_num);

        global_match_open = true;
        is_track_valid = true;
        bb_rot = RotatedRect(center/*+Point2f(rcols.start,rrows.start)*/,  size_initial * scale, rotation/CV_PI * 180);
        im_prev = im_gray;
    }

    LOGE("CMTTIME processFrame:%.3f\n",(now_ms()-startCTime)*1000.0/CLOCKS_PER_SEC);
}

} /* namespace CMT */
