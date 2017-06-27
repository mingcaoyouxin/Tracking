package com.tracking.utils;

import android.app.Activity;
import android.graphics.Point;
import android.hardware.Camera;
import android.view.Display;

import com.tracking.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by kevinxing on 2016/4/22.
 */
public class CameraUtil {

    private final static float CAMERA_RATIO_4_3 = 1.33f;
    private final static float CAMERA_RATIO_16_9 = 1.77f;

    /**
     * 获取到合适比例的取景大小
     * @param currentActivity
     * @param sizes 注意理解这里的Size定义，对于相机来说，通常width代表的是屏幕的长边（因为相机以横屏定义参数）
     * @return
     */
    public static Camera.Size getOptimalPreviewSize(Activity currentActivity, List<Camera.Size> sizes) {
        if (sizes == null || sizes.isEmpty()) {
            return null;
        }
        // Use a very small tolerance because we want an exact match.
        double ASPECT_TOLERANCE = 0.02;
        int optimalSizeIndex = -1;

        // Because of bugs of overlay and layout, we sometimes will try to
        // layout the viewfinder in the portrait orientation and thus get the
        // wrong size of preview surface. When we change the preview size, the
        // new overlay will be created before the old one closed, which causes
        // an exception. For now, just get the screen size.
        Point point = getDefaultDisplaySize(currentActivity, new Point());
        int screenHeight = Math.max(point.x, point.y);
        int screenWidth = Math.min(point.x, point.y);

        ArrayList<CandidateSize> candidateList = new ArrayList<>();

        // Try to find an size match 4:3 ratio
        int length = sizes.size();
        for (int i = 0; i < length; i++) {
            Camera.Size size = sizes.get(i);
            double ratio = (double) Math.max(size.width, size.height) / Math.min(size.width, size.height);
            if (Math.abs(ratio - CAMERA_RATIO_4_3) > ASPECT_TOLERANCE) {
                continue;
            }
            candidateList.add(new CandidateSize(i, size));
        }

        // try to find an size match 16:9 ratio
        if (candidateList.isEmpty()) {
            for (int i = 0; i < length; i++) {
                Camera.Size size = sizes.get(i);
                double ratio = (double) Math.max(size.width, size.height) / Math.min(size.width, size.height);
                if (Math.abs(ratio - CAMERA_RATIO_16_9) > ASPECT_TOLERANCE) {
                    continue;
                }
                candidateList.add(new CandidateSize(i, size));
            }
        }

        if (!candidateList.isEmpty()) {
            //从大到小排序
            Collections.sort(candidateList, new Comparator<CandidateSize>() {
                @Override
                public int compare(CandidateSize candidateSize, CandidateSize candidateSize2) {
                    return candidateSize2.size.width - candidateSize.size.width;
                }
            });

            int minBottomHeight = currentActivity.getResources().getDimensionPixelSize(R.dimen.bottom_bar_min_h);
            for (int i = 0; i < candidateList.size(); i++) {
                CandidateSize candidateSize = candidateList.get(i);
                int height = Math.max(candidateSize.size.width, candidateSize.size.height);
                int width = Math.min(candidateSize.size.width, candidateSize.size.height);
                /**
                 * Preview区域宽高没必要和屏幕宽高在数值上完全相等，保持相同比例即可；
                 * 否则相同比例的高分辨率PreviewSize可能会被过滤掉，影响输出照片质量；
                 */
                //int remainH = screenHeight - height;
                int scaledHeight = screenWidth * height / width;
                int remainH = screenHeight - scaledHeight;
                if (remainH > minBottomHeight) {
                    optimalSizeIndex = candidateList.get(i).index;
                    break;
                }
            }
        }
        return optimalSizeIndex == -1 ? null : sizes.get(optimalSizeIndex);
    }

    public static Camera.Size getOptimalPreviewSize_16_9(Activity currentActivity, List<Camera.Size> sizes) {
        if (sizes == null || sizes.isEmpty()) {
            return null;
        }
        // Use a very small tolerance because we want an exact match.
        double ASPECT_TOLERANCE = 0.02;
        int optimalSizeIndex = -1;

        // Because of bugs of overlay and layout, we sometimes will try to
        // layout the viewfinder in the portrait orientation and thus get the
        // wrong size of preview surface. When we change the preview size, the
        // new overlay will be created before the old one closed, which causes
        // an exception. For now, just get the screen size.
        Point point = getDefaultDisplaySize(currentActivity, new Point());
        int screenHeight = Math.max(point.x, point.y);
        int screenWidth = Math.min(point.x, point.y);

        ArrayList<CandidateSize> candidateList = new ArrayList<>();

        // Try to find an size match 4:3 ratio
        int length = sizes.size();

        // try to find an size match 16:9 ratio
        if (candidateList.isEmpty()) {
            for (int i = 0; i < length; i++) {
                Camera.Size size = sizes.get(i);
                double ratio = (double) Math.max(size.width, size.height) / Math.min(size.width, size.height);
                if (Math.abs(ratio - CAMERA_RATIO_16_9) > ASPECT_TOLERANCE) {
                    continue;
                }
                candidateList.add(new CandidateSize(i, size));
            }
        }

        if (!candidateList.isEmpty()) {
            //从大到小排序
            Collections.sort(candidateList, new Comparator<CandidateSize>() {
                @Override
                public int compare(CandidateSize candidateSize, CandidateSize candidateSize2) {
                    return candidateSize2.size.width - candidateSize.size.width;
                }
            });

        }
        return !candidateList.isEmpty() && !sizes.isEmpty() ? sizes.get(candidateList.get(0).index) : null;
    }

    public static Camera.Size getOptimalPreviewSizeFullScreen(Activity currentActivity, List<Camera.Size> sizes) {
        if (sizes == null || sizes.isEmpty()) {
            return null;
        }
        // Use a very small tolerance because we want an exact match.
        double ASPECT_TOLERANCE = 0.02;
        int optimalSizeIndex = -1;

        // Because of bugs of overlay and layout, we sometimes will try to
        // layout the viewfinder in the portrait orientation and thus get the
        // wrong size of preview surface. When we change the preview size, the
        // new overlay will be created before the old one closed, which causes
        // an exception. For now, just get the screen size.
        Point point = getDefaultDisplaySize(currentActivity, new Point());
        int screenHeight = Math.max(point.x, point.y);
        int screenWidth = Math.min(point.x, point.y);

        ArrayList<CandidateSize> candidateList = new ArrayList<>();

        // try to find an size match 16:9 ratio

        int length = sizes.size();
        for (int i = 0; i < length; i++) {
            Camera.Size size = sizes.get(i);
            double ratio = (double) Math.max(size.width, size.height) / Math.min(size.width, size.height);
            if (Math.abs(ratio - CAMERA_RATIO_16_9) > ASPECT_TOLERANCE) {
                continue;
            }
            candidateList.add(new CandidateSize(i, size));
        }

        if (candidateList.isEmpty()) {
            // Try to find an size match 4:3 ratio
            for (int i = 0; i < length; i++) {
                Camera.Size size = sizes.get(i);
                double ratio = (double) Math.max(size.width, size.height) / Math.min(size.width, size.height);
                if (Math.abs(ratio - CAMERA_RATIO_4_3) > ASPECT_TOLERANCE) {
                    continue;
                }
                candidateList.add(new CandidateSize(i, size));
            }

        }

        if (!candidateList.isEmpty()) {
            //从大到小排序
            Collections.sort(candidateList, new Comparator<CandidateSize>() {
                @Override
                public int compare(CandidateSize candidateSize, CandidateSize candidateSize2) {
                    return candidateSize2.size.width - candidateSize.size.width;
                }
            });

            int minBottomHeight = currentActivity.getResources().getDimensionPixelSize(R.dimen.bottom_bar_min_h);
            for (int i = 0; i < candidateList.size(); i++) {
                CandidateSize candidateSize = candidateList.get(i);
                int height = Math.max(candidateSize.size.width, candidateSize.size.height);
                int width = Math.min(candidateSize.size.width, candidateSize.size.height);
                /**
                 * Preview区域宽高没必要和屏幕宽高在数值上完全相等，保持相同比例即可；
                 * 否则相同比例的高分辨率PreviewSize可能会被过滤掉，影响输出照片质量；
                 */
                //int remainH = screenHeight - height;
                int scaledHeight = screenWidth * height / width;
                int remainH = screenHeight - scaledHeight;
                if (remainH > minBottomHeight) {
                    optimalSizeIndex = candidateList.get(i).index;
                    break;
                }
            }
        }
        return optimalSizeIndex == -1 ? null : sizes.get(optimalSizeIndex);
    }

    public static Point getDefaultDisplaySize(Activity activity, Point size) {
        Display d = activity.getWindowManager().getDefaultDisplay();
        d.getSize(size);
        return size;
    }

    public static int findFrontCamera(){
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras(); // get cameras number

        for ( int camIdx = 0; camIdx < cameraCount;camIdx++ ) {
            Camera.getCameraInfo( camIdx, cameraInfo ); // get camerainfo
            if ( cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT ) {
                // 代表摄像头的方位，目前有定义值两个分别为CAMERA_FACING_FRONT前置和CAMERA_FACING_BACK后置
                return camIdx;
            }
        }
        return -1;
    }

    public static int findBackCamera(){
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras(); // get cameras number

        for ( int camIdx = 0; camIdx < cameraCount;camIdx++ ) {
            Camera.getCameraInfo( camIdx, cameraInfo ); // get camerainfo
            if ( cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK ) {
                // 代表摄像头的方位，目前有定义值两个分别为CAMERA_FACING_FRONT前置和CAMERA_FACING_BACK后置
                return camIdx;
            }
        }
        return -1;
    }

    /**
     * 候选大小，包含index信息
     */
    private static class CandidateSize {
        public int index;
        public Camera.Size size;

        public CandidateSize(int _index, Camera.Size _size) {
            index = _index;
            size = _size;
        }

        @Override
        public String toString() {
            return "CandidateSize{" +
                    "index=" + index +
                    ", size=" + size +
                    '}';
        }
    }
}
