#include "common.h"
#include "Consensus.h"
#include "Fusion.h"
#include "Matcher.h"
#include "Tracker.h"

#include <opencv2/features2d/features2d.hpp>

using cv::RotatedRect;
using cv::Size2f;
namespace cmtproxyspace {
    class CMTProxy {
    public:
        void init(uint8_t *buf, const int dataType, const long x, const long y,
                  const long w, const long h, const long width, const long height);

        void trackFrame(uint8_t *buf,
                        int dataType, int imageWidth, int imageHeight);

        float *getResultRect(const int imageWidth, const int imageHeight);

        bool isTrackValid();
        /*Fusion fusion;   // 数据融合器
        Matcher matcher; // 特征匹配器
        Tracker tracker; // 跟踪器
        Consensus consensus;  // 一致检查器

        string str_detector;
        string str_descriptor;

        vector<Point2f> points_active; //public for visualization purposes 有效特征点
        RotatedRect bb_rot;

        bool is_track_valid;*/

    private:

        /*cmt::CMT *cmt0;
        cv::Ptr<FeatureDetector> detector;
        cv::Ptr<DescriptorExtractor> descriptor;

        Size2f size_initial;  // 初始大小

        vector<int> classes_active;

        float theta;

        Mat im_prev;

        bool global_match_open;

        int initial_active_points_num;*/
    };
}
