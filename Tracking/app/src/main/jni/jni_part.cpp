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
JNIEXPORT void JNICALL Java_com_tracking_preview_TrackerManager_FindFeatures(JNIEnv*, jobject, jlong addrGray, jlong addrRgba);
//JNIEXPORT void JNICALL Java_com_tracking_preview_TrackerManager_OpenTLD(JNIEnv*, jobject, jlong addrGray, jlong addrRgba,jlong x,jlong y,jlong width,jlong height);

bool CMTinitiated=false;
  //TLD * etld=NULL ;
  CMT * cmt1=new CMT();

  long rect[4];

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
}
/*

JNIEXPORT void JNICALL Java_com_tracking_preview_TrackerManager_OpenTLD(JNIEnv*, jobject, jlong addrGray, jlong addrRgba,
		jlong x, jlong y, jlong width, jlong height)
{

	if (etld!=NULL)
	{
		etld->release();
		delete etld;
	}
    etld = new TLD();
    Mat& mGr  = *(Mat*)addrGray;
    Mat& mRgb = *(Mat*)addrRgba;

    int t=  mRgb.cols;
    etld->detectorCascade->imgWidth =mGr.cols;
    etld->detectorCascade->imgHeight = mGr.rows;
    etld->detectorCascade->imgWidthStep = mGr.step;

    Rect r;
    r.x= x;//mGr.size().width/2-mGr.size().width/4;
    r.y= y;// mGr.size().height/2-mGr.size().height/4;
    r.width= width;//mGr.size().width/2;
    r.height= height;//mGr.size().height/2;
    etld->selectObject(mGr,& r );

}



JNIEXPORT jintArray JNICALL Java_com_tracking_preview_TrackerManager_getRect(JNIEnv *env, jobject)
{

	 jintArray result;
	 result = env->NewIntArray(4);

	 if (etld->currBB == NULL) {
	     return NULL;
	 }

	jint fill[4];
	if (etld->currBB!=NULL)
	{
		fill[0]=etld->currBB->x;
		fill[1]=etld->currBB->y;
		fill[2]=etld->currBB->width;
		fill[3]=etld->currBB->height;
		env->SetIntArrayRegion(result, 0, 4, fill);
		return result;
	}

	return NULL;

}


JNIEXPORT void JNICALL Java_com_tracking_preview_TrackerManager_ProcessTLD(JNIEnv*, jobject, jlong addrGray, jlong addrRgba)
{
	 Mat& mRgb = *(Mat*)addrRgba;

	 etld->processImage(mRgb);

	 if (etld->currBB!=NULL)
	 {
		 Rect r;
		 r.x=etld->currBB->x;
		 r.y=etld->currBB->y;
		 r.width= etld->currBB->width;
		 r.height= etld->currBB->height;

		 rectangle(mRgb ,r,Scalar(0,0,255,0),5);
		*//*   for(size_t i = 0; i < etld->detectorCascade->detectionResult->fgList->size(); i++)
		                {
		                    Rect r = etld->detectorCascade->detectionResult->fgList->at(i);
		                    rectangle(mRgb, r, Scalar(255,0,0,0), 1);
		                }
		                *//*
	 }
	 else
	 {
		 Rect r;
		 r.x=mRgb.size().width/2;
		 r.y=mRgb.size().height/2;;
		 r.width= 100;
	     r.height= 100;
	     rectangle(mRgb ,r,Scalar(0,0,0,255),5);
	 }

}*/


JNIEXPORT void JNICALL Java_com_tracking_preview_TrackerManager_OpenCMT(JNIEnv*, jobject, jlong addrGray, jlong addrRgba,jlong x, jlong y, jlong width, jlong height)
{

//	 if (cmt!=NULL)
//	 {
//		 delete cmt;
//	 }
//	 cmt = new CMT();
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
	//Mat& img  = *(Mat*)addrRgba;
	Mat& im_gray  = *(Mat*)addrGray;

	cmt1->processFrame(im_gray);

//	        for(int i = 0; i<cmt->trackedKeypoints.size(); i++)
//	            cv::circle(img, cmt->trackedKeypoints[i].first.pt, 3, cv::Scalar(255,255,255));
/*	        cv::line(img, cmt1->topLeft, cmt1->topRight, cv::Scalar(255,255,255));
	        cv::line(img, cmt1->topRight, cmt1->bottomRight, cv::Scalar(255,255,255));
	        cv::line(img, cmt1->bottomRight, cmt1->bottomLeft, cv::Scalar(255,255,255));
	        cv::line(img, cmt1->bottomLeft, cmt1->topLeft, cv::Scalar(255,255,255));*/

}


JNIEXPORT jintArray JNICALL Java_com_tracking_preview_TrackerManager_CMTgetRect(JNIEnv *env, jobject)
{

	if (!CMTinitiated)
		return NULL;

	 jintArray result;
	 result = env->NewIntArray(8);



	jint fill[8];

	{
        Point2f point2f[4];
        cmt1->bb_rot.points(point2f);
		fill[0]=point2f[0].x;
		fill[1]=point2f[0].y;
		fill[2]=point2f[1].x;
		fill[3]=point2f[1].y;
		fill[4]=point2f[2].x;
		fill[5]=point2f[2].y;
		fill[6]=point2f[3].x;
		fill[7]=point2f[3].y;
		env->SetIntArrayRegion(result, 0, 8, fill);
		return result;
	}

	return NULL;

}

JNIEXPORT jboolean JNICALL Java_com_tracking_preview_TrackerManager_CMTisTrackValid(JNIEnv *env){
	return (jboolean) cmt1->is_track_valid;
}
/*
JNIEXPORT void JNICALL Java_com_tracking_preview_TrackerManager_TLDSave(JNIEnv *env,jobject,jstring path)
{

	 if (etld==NULL)
		 return;

	  const char *str = env->GetStringUTFChars(path, 0);

	  etld->writeToFile(str);

	  env->ReleaseStringUTFChars( path, str);
}

JNIEXPORT void JNICALL Java_com_tracking_preview_TrackerManager_TLDLoad(JNIEnv *env,jobject,jstring Path)
{
*//*
	if (etld!=NULL)
		{
			etld->release();
			delete etld;
		}
	    etld = new TLD();
*//*

	  const char *str = env->GetStringUTFChars(Path, 0);

	  etld->readFromFile(str);

	  env->ReleaseStringUTFChars( Path, str);

}*/

/*
JNIEXPORT void JNICALL Java_com_tracking_preview_TrackerManager_CMTSave(JNIEnv *env,jobject,jstring path)
{

	if (!CMTinitiated)
			return ;

	  const char *str = env->GetStringUTFChars(path, 0);

	  cmt1->Save(str);

	  env->ReleaseStringUTFChars( path, str);
}*/


/*JNIEXPORT void JNICALL Java_com_tracking_preview_TrackerManager_CMTLoad(JNIEnv *env,jobject,jstring path)
{



	  const char *str = env->GetStringUTFChars(path, 0);

	  cmt1=new CMT();

	  cmt1->Load(str);

	  env->ReleaseStringUTFChars( path, str);

	  CMTinitiated=true;
}*/
}

