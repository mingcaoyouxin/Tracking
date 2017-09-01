#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>

#include "CMT.h"
#include <vector>

using namespace std;
using namespace cv;
using namespace cmt;
/*using namespace tld;*/

extern "C" {

typedef enum {
	NV21 = 0,
	RGB,
	RGBA
} DataType;
/*
JNIEXPORT void JNICALL Java_com_tracking_preview_TrackerManager_FindFeatures(JNIEnv*, jobject, jlong addrGray, jlong addrRgba);
//JNIEXPORT void JNICALL Java_com_tracking_preview_TrackerManager_OpenTLD(JNIEnv*, jobject, jlong addrGray, jlong addrRgba,jlong x,jlong y,jlong width,jlong height);*/

bool CMTinitiated=false;
  //TLD * etld=NULL ;
  CMT * cmt1=new CMT();
  long rect[4];

uint8_t *g_dataBuff = NULL;
int g_dataSize = 0;
char *g_debufInfo = NULL;
int CmtWidth = 400;
int CmtHeight = 300;

void allocateDataBuff(int size) {
	if (g_dataSize >= size) {
		return;
	}
	if (g_dataBuff != NULL) {
		delete[] g_dataBuff;
		g_dataBuff = NULL;
	}
	g_dataBuff = new uint8_t[size];
	g_dataSize = size;
}


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
	Mat resultMat, tmp,img,grayMat;
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
/*
JNIEXPORT void JNICALL Java_com_tracking_preview_TrackerManager_FindFeatures(JNIEnv*, jobject, jlong addrGray, jlong addrRgba)
{
    Mat& mGr  = *(Mat*)addrGray;
    Mat& mRgb = *(Mat*)addrRgba;
    vector<KeyPoint> v;

    //FastFeatureDetector detector(50);
    //detector.detect(mGr, v);
    cv::Ptr<cv::FeatureDetector> fast =
            cv::FastFeatureDetector::create (50, true,cv::FastFeatureDetector::TYPE_9_16 );

    fast -> detect(mGr, v);
    for( unsigned int i = 0; i < v.size(); i++ )
    {
        const KeyPoint& kp = v[i];
        circle(mRgb, Point(kp.pt.x, kp.pt.y), 10, Scalar(255,0,0,255));
    }
}*/

JNIEXPORT jboolean JNICALL
Java_com_tracking_preview_TrackerManager_openTrack(JNIEnv *env, jobject, jbyteArray yuvData,
		jint dataType, jlong x, jlong y, jlong width, jlong height, jint imageWidth, jint imageHeight)
{

	jboolean initBoolean = JNI_FALSE;
	int len = env->GetArrayLength(yuvData);
	allocateDataBuff(len);
	caluateResizeSize(imageWidth, imageHeight);

	env->GetByteArrayRegion(yuvData, 0, len, reinterpret_cast<jbyte *>(g_dataBuff));
	Mat addrGray = getTrackMat(g_dataBuff, dataType, imageWidth, imageHeight);

	if (cmt1!=NULL)
	{
		delete cmt1;
	}
	cmt1 = new CMT();
	Mat& im_gray  = addrGray;
	//Point p1(x,y);
	//Point p2(x+width,y+height);

	float rateX = CmtWidth * 1.0f / imageWidth;
	float rateY = CmtHeight * 1.0f / imageHeight;
	Point p1(x * rateX, y * rateY);
	Point p2((x + width) * rateX, (y + height) * rateY);
	Rect rect = Rect(p1, p2);

	CMTinitiated=false;
	//Rect rect(p1, p2);
	cmt1->initialize(im_gray, rect);
	CMTinitiated=true;
	initBoolean = JNI_TRUE;


	return initBoolean;
}



JNIEXPORT void JNICALL Java_com_tracking_preview_TrackerManager_processTrack(JNIEnv *env, jobject, jbyteArray yuvData,
																			 jint dataType,jint imageWidth, jint imageHeight)
{
	if (!CMTinitiated)
		return;
	int len = env->GetArrayLength(yuvData);
	allocateDataBuff(len);
	env->GetByteArrayRegion(yuvData, 0, len, reinterpret_cast<jbyte *>(g_dataBuff));
	Mat addrGray = getTrackMat(g_dataBuff, dataType, imageWidth, imageHeight);
	Mat& im_gray  = addrGray;
	cmt1->processFrame(im_gray);
}


/*JNIEXPORT void JNICALL Java_com_tracking_preview_TrackerManager_OpenCMT(JNIEnv*, jobject, jlong addrGray, jlong addrRgba,jlong x, jlong y, jlong width, jlong height)
{

	 if (cmt1!=NULL)
	 {
		 delete cmt1;
	 }
	 cmt1 = new CMT();
	 Mat& im_gray  = *(Mat*)addrGray;
	 Point p1(x,y);
	 Point p2(x+width,y+height);

    CMTinitiated=false;
    Rect rect(p1, p2);
	cmt1->initialize(im_gray, rect);
    CMTinitiated=true;

}

JNIEXPORT void JNICALL Java_com_tracking_preview_TrackerManager_ProcessCMT(JNIEnv*, jobject, jlong addrGray, jlong addrRgba)
{
	if (!CMTinitiated)
		return;
	Mat& im_gray  = *(Mat*)addrGray;
	cmt1->processFrame(im_gray);

}*/


JNIEXPORT jintArray JNICALL Java_com_tracking_preview_TrackerManager_CMTgetRect(JNIEnv *env, jobject, jint imageWidth, jint imageHeight)
{

	if (!CMTinitiated)
		return NULL;

	jintArray result;
	result = env->NewIntArray(8);

	jint fill[8];

	{
		float rateX = imageWidth * 1.0f / CmtWidth;
		float rateY = imageHeight * 1.0f / CmtHeight;

        Point2f point2f[4];
        cmt1->bb_rot.points(point2f);
		fill[0]=point2f[0].x;
		fill[1]=point2f[0].y;
		fill[2]=point2f[1].x * rateX;
		fill[3]=point2f[1].y * rateY;
		fill[4]=point2f[2].x;
		fill[5]=point2f[2].y;
		fill[6]=point2f[3].x * rateX;
		fill[7]=point2f[3].y * rateY;
		env->SetIntArrayRegion(result, 0, 8, fill);

		return result;
	}

	return NULL;
}

JNIEXPORT jboolean JNICALL Java_com_tracking_preview_TrackerManager_CMTisTrackValid(JNIEnv *env){
	return (jboolean) cmt1->is_track_valid;
}





}

