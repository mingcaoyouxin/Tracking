package me.jessyan.camerafilters.filter;

import android.content.Context;
import android.opengl.GLES20;

import me.jessyan.camerafilters.R;
import me.jessyan.camerafilters.util.GlUtil;

import java.nio.FloatBuffer;

/**
 * Created by shengwenhui on 16/3/3.
 */
public class CameraFilterMosaic extends CameraFilter {
    private int muTexsizeLoc;

    private static final float Texsize_array[] = {
            255, 255,
    };

    public CameraFilterMosaic(Context context, boolean isUseQiniu) {
        super(context, isUseQiniu);
    }

    @Override
    protected int createProgram(Context applicationContext, boolean isUseQiniu) {
        return GlUtil.createProgram(applicationContext, isUseQiniu ? R.raw.vertex_shader_qiniu : R.raw.vertex_shader,
                R.raw.fragment_shader_mosaic);
    }

    @Override
    protected void getGLSLValues() {
        super.getGLSLValues();

        muTexsizeLoc = GLES20.glGetUniformLocation(mProgramHandle, "TexSize");
    }

    @Override
    protected void bindGLSLValues(float[] mvpMatrix, FloatBuffer vertexBuffer, int coordsPerVertex,
                                  int vertexStride, float[] texMatrix, FloatBuffer texBuffer, int texStride) {
        super.bindGLSLValues(mvpMatrix, vertexBuffer, coordsPerVertex, vertexStride, texMatrix,
                texBuffer, texStride);

        GLES20.glUniform2fv(muTexsizeLoc, 1, Texsize_array, 0);
    }
}