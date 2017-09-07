#include "CMTProxy.h"

#include <opencv2/imgproc/imgproc.hpp>
#include "CMT.h"

namespace cmtproxyspace {
    extern "C" {
    typedef enum {
        NV21 = 0,
        RGB,
        RGBA
    } DataType;
    cmt::CMT *cmt0 = new cmt::CMT();
    int CmtWidth = 400;
    int CmtHeight = 300;


    void caluateResizeSize(int width, int height) {
        float rateX = 1.0f;
        float rateY = 1.0f;
        CmtWidth = width;
        CmtHeight = height;
        if (width >= 600 || height >= 600) {
            rateX = 400 * 1.0f / width;
            rateY = 400 * 1.0f / height;
            if (rateX < rateY) {
                CmtWidth = 400;
                CmtHeight = (int) (height * rateX);
                rateY = rateX;
            } else {
                CmtHeight = 400;
                CmtWidth = (int) (width * rateY);
                rateX = rateY;
            }
        }
    }


    Mat getTrackMat(uint8_t *buf, int dataType, int width, int height) {
        Mat resultMat, tmp, img, grayMat;
        switch (dataType) {
            case NV21:
                tmp = Mat(height + height / 2, width, CV_8UC1, buf);
                //cv::cvtColor(tmp, resultMat, CV_YUV420sp2GRAY);
                cv::cvtColor(tmp, img, CV_YUV420sp2GRAY);
                if (CmtWidth == width && CmtHeight == height) {
                    cv::cvtColor(img, resultMat, CV_GRAY2RGBA);
                } else {
                    cv::resize(img, grayMat, cv::Size(CmtWidth, CmtHeight));
                    cv::cvtColor(grayMat, resultMat, CV_GRAY2RGBA);
                }
                break;
        }
        return resultMat;
    }

    void CMTProxy::init(uint8_t *buf, int dataType, long x, long y,
                        long width, long height, long imageWidth, long imageHeight) {
        caluateResizeSize(imageWidth, imageHeight);
        Mat addrGray = getTrackMat(buf, dataType, imageWidth, imageHeight);
        Mat &im_gray = addrGray;
        float rateX = CmtWidth * 1.0f / imageWidth;
        float rateY = CmtHeight * 1.0f / imageHeight;
        cv::Point p1(x * rateX, y * rateY);
        cv::Point p2((x + width) * rateX, (y + height) * rateY);
        Rect rect = Rect(p1, p2);

        if (cmt0 != NULL) {
            delete cmt0;
        }
        cmt0 = new cmt::CMT();
        cmt0->initialize(im_gray, rect);
    }

    void CMTProxy::trackFrame(uint8_t *buf, int dataType, int imageWidth, int imageHeight) {
        Mat addrGray = getTrackMat(buf, dataType, imageWidth, imageHeight);
        Mat &im_gray = addrGray;
        cmt0->processFrame(im_gray);
    }


    int *CMTProxy::getResultRect(const int imageWidth, const int imageHeight) {
        float rateX = imageWidth * 1.0f / CmtWidth;
        float rateY = imageHeight * 1.0f / CmtHeight;

        Point2f point2f[4];
        cmt0->bb_rot.points(point2f);

        int *fill = new int[8];
        fill[0] = point2f[0].x;
        fill[1] = point2f[0].y;
        fill[2] = point2f[1].x * rateX;
        fill[3] = point2f[1].y * rateY;
        fill[4] = point2f[2].x;
        fill[5] = point2f[2].y;
        fill[6] = point2f[3].x * rateX;
        fill[7] = point2f[3].y * rateY;
        return fill;
    }

    bool CMTProxy::isTrackValid() {
        return cmt0->is_track_valid;
    }

    }
}