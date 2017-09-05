#ifndef CMT_H

#define CMT_H

#include "common.h"
#include "Consensus.h"
#include "Fusion.h"
#include "Matcher.h"
#include "Tracker.h"

#include <opencv2/features2d/features2d.hpp>

using cv::FeatureDetector;
using cv::DescriptorExtractor;

using cv::RotatedRect;
using cv::Size2f;

namespace cmt
{

class CMT
{
public:
    CMT() : str_detector("FAST"), str_descriptor("BRISK") {};
    void initialize(const Mat im_gray, const cv::Rect rect);
    void processFrame(const Mat im_gray);

    Fusion fusion;   // �����ں���
    Matcher matcher; // ����ƥ����
    Tracker tracker; // ������
    Consensus consensus;  // һ�¼����

    string str_detector;
    string str_descriptor;

    vector<Point2f> points_active; //public for visualization purposes ��Ч������
    RotatedRect bb_rot;

    bool is_track_valid;

private:
    cv::Ptr<FeatureDetector> detector;
    cv::Ptr<DescriptorExtractor> descriptor;

    Size2f size_initial;  // ��ʼ��С

    vector<int> classes_active;

    float theta;

    Mat im_prev;

    bool global_match_open;

    int initial_active_points_num;
};

} /* namespace CMT */

#endif /* end of include guard: CMT_H */
