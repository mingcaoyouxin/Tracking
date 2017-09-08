#include <jni.h>
#include <opencv2/core/core.hpp>

#include "CMTProxy.h"

extern "C" {

bool CMTinitiated=false;
cmtproxyspace::CMTProxy * cmtProxy=new cmtproxyspace::CMTProxy();
uint8_t *g_dataBuff = NULL;
int g_dataSize = 0;

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


JNIEXPORT jboolean JNICALL
Java_com_tracking_preview_TrackerManager_openTrack(JNIEnv *env, jobject, jbyteArray yuvData,
		jint dataType, jlong x, jlong y, jlong width, jlong height, jint imageWidth, jint imageHeight)
{
	CMTinitiated = false;
	int len = env->GetArrayLength(yuvData);
	allocateDataBuff(len);
	env->GetByteArrayRegion(yuvData, 0, len, reinterpret_cast<jbyte *>(g_dataBuff));

	if (cmtProxy!=NULL)
	{
		delete cmtProxy;
	}
	cmtProxy = new cmtproxyspace::CMTProxy();
	cmtProxy->init(g_dataBuff, dataType, x, y, width, height, imageWidth, imageHeight);
	CMTinitiated = true;
	return JNI_TRUE;
}



JNIEXPORT void JNICALL Java_com_tracking_preview_TrackerManager_processTrack(JNIEnv *env, jobject, jbyteArray yuvData,
																			 jint dataType,jint imageWidth, jint imageHeight)
{
	if (!CMTinitiated)
		return;
	int len = env->GetArrayLength(yuvData);
	allocateDataBuff(len);
	env->GetByteArrayRegion(yuvData, 0, len, reinterpret_cast<jbyte *>(g_dataBuff));
	cmtProxy->trackFrame(g_dataBuff, dataType, imageWidth, imageHeight);
}



JNIEXPORT jfloatArray JNICALL Java_com_tracking_preview_TrackerManager_CMTgetRect(JNIEnv *env, jobject, jint imageWidth, jint imageHeight)
{

	if (!CMTinitiated)
		return NULL;

	jfloatArray result;
	result = env->NewFloatArray(8);

	jfloat fill[8];

	{
		float* rect = cmtProxy->getResultRect(imageWidth, imageHeight);
		fill[0]=rect[0];
		fill[1]=rect[1];
		fill[2]=rect[2];
		fill[3]=rect[3];
		fill[4]=rect[4];
		fill[5]=rect[5];
		fill[6]=rect[6];
		fill[7]=rect[7];
		env->SetFloatArrayRegion(result, 0, 8, fill);
		return result;
	}

	return NULL;
}

JNIEXPORT jboolean JNICALL Java_com_tracking_preview_TrackerManager_CMTisTrackValid(JNIEnv *env){
	return (jboolean) cmtProxy->isTrackValid();
}





}

