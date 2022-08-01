package com.example.mincamera.util;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.DisplayMetrics;
import android.util.Size;
import android.view.WindowManager;

import java.util.Arrays;
import java.util.List;

public class Utils {
    /**
     * @Author:YZM
     * @Description:获得屏幕高度
     */
    public static Size loadWinSize(Context ctx) {
        WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        if (wm != null) {
            wm.getDefaultDisplay().getMetrics(outMetrics);
        }
        return new Size(outMetrics.widthPixels, outMetrics.heightPixels);
    }
    /**
     * @Author:YZM
     * @Description:计算适合的图片尺寸
     */
    public static Size fitPhotoSize(StreamConfigurationMap map, Size mWinSize){
        // 获取摄像头支持的最大尺寸
        List<Size> sizes = Arrays.asList(map.getOutputSizes(ImageFormat.JPEG));
        int minIndex = 0;//差距最小的索引
        int minDx = Integer.MAX_VALUE;
        int minDy = Integer.MAX_VALUE;
        int[] dxs = new int[sizes.size()];
        int justW = mWinSize.getHeight() * 2;//相机默认是横向的，so
        int justH = mWinSize.getWidth() * 2;
        for (int i = 0; i < sizes.size(); i++) {
            dxs[i] = sizes.get(i).getWidth() - justW;
        }
        for (int i = 0; i < dxs.length; i++) {
            int abs = Math.abs(dxs[i]);
            if (abs < minDx) {
                minIndex = i;//获取高的最适索引
                minDx = abs;
            }
        }
        for (int i = 0; i < sizes.size(); i++) {
            Size size = sizes.get(i);
            if (size.getWidth() == sizes.get(minIndex).getWidth()) {
                int dy = Math.abs(justH - size.getHeight());
                if (dy < minDy) {
                    minIndex = i;//获取宽的最适索引
                    minDy = dy;
                }
            }
        }
        return sizes.get(minIndex);
    }
}
