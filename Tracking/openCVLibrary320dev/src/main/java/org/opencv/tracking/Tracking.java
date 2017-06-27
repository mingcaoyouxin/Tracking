
//
// This file is auto-generated. Please don't modify it!
//
package org.opencv.tracking;

import java.lang.String;
import java.util.ArrayList;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect2d;
import org.opencv.core.Rect2d;

public class Tracking {

    //
    // C++:  Rect2d selectROI(Mat img, bool fromCenter = true)
    //

    //javadoc: selectROI(img, fromCenter)
    public static Rect2d selectROI(Mat img, boolean fromCenter)
    {
        
        Rect2d retVal = new Rect2d(selectROI_0(img.nativeObj, fromCenter));
        
        return retVal;
    }

    //javadoc: selectROI(img)
    public static Rect2d selectROI(Mat img)
    {
        
        Rect2d retVal = new Rect2d(selectROI_1(img.nativeObj));
        
        return retVal;
    }


    //
    // C++:  Rect2d selectROI(String windowName, Mat img, bool showCrossair = true, bool fromCenter = true)
    //

    //javadoc: selectROI(windowName, img, showCrossair, fromCenter)
    public static Rect2d selectROI(String windowName, Mat img, boolean showCrossair, boolean fromCenter)
    {
        
        Rect2d retVal = new Rect2d(selectROI_2(windowName, img.nativeObj, showCrossair, fromCenter));
        
        return retVal;
    }

    //javadoc: selectROI(windowName, img)
    public static Rect2d selectROI(String windowName, Mat img)
    {
        
        Rect2d retVal = new Rect2d(selectROI_3(windowName, img.nativeObj));
        
        return retVal;
    }


    //
    // C++:  void selectROI(String windowName, Mat img, vector_Rect2d boundingBox, bool fromCenter = true)
    //

    //javadoc: selectROI(windowName, img, boundingBox, fromCenter)
    public static void selectROI(String windowName, Mat img, MatOfRect2d boundingBox, boolean fromCenter)
    {
        Mat boundingBox_mat = boundingBox;
        selectROI_4(windowName, img.nativeObj, boundingBox_mat.nativeObj, fromCenter);
        
        return;
    }

    //javadoc: selectROI(windowName, img, boundingBox)
    public static void selectROI(String windowName, Mat img, MatOfRect2d boundingBox)
    {
        Mat boundingBox_mat = boundingBox;
        selectROI_5(windowName, img.nativeObj, boundingBox_mat.nativeObj);
        
        return;
    }




    // C++:  Rect2d selectROI(Mat img, bool fromCenter = true)
    private static native double[] selectROI_0(long img_nativeObj, boolean fromCenter);
    private static native double[] selectROI_1(long img_nativeObj);

    // C++:  Rect2d selectROI(String windowName, Mat img, bool showCrossair = true, bool fromCenter = true)
    private static native double[] selectROI_2(String windowName, long img_nativeObj, boolean showCrossair, boolean fromCenter);
    private static native double[] selectROI_3(String windowName, long img_nativeObj);

    // C++:  void selectROI(String windowName, Mat img, vector_Rect2d boundingBox, bool fromCenter = true)
    private static native void selectROI_4(String windowName, long img_nativeObj, long boundingBox_mat_nativeObj, boolean fromCenter);
    private static native void selectROI_5(String windowName, long img_nativeObj, long boundingBox_mat_nativeObj);

}
