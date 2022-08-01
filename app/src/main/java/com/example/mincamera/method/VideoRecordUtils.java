package com.example.mincamera.method;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VideoRecordUtils {
    public static Size WH_720X480 = new Size(720,480);

    private MediaRecorder mediaRecorder;
    private SurfaceView surfaceView;
    private CameraDevice mCameraDevice;
    List<Surface> surfaces = new ArrayList<>();
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mCameraCaptureSession;
    private Size size;

    /**
     * @Author:YZM
     * @Description:初始化MediaRecorder，接収Camera有关参数
     */
    public void create(SurfaceView surfaceView, CameraDevice cameraDevice, Size size){
        this.surfaceView = surfaceView;
        this.size = size;
        mCameraDevice = cameraDevice;
        try{
            //创建录制请求
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
        }catch(CameraAccessException e){
            e.printStackTrace();
        }
        mediaRecorder = new MediaRecorder();
    }

    /**
     * @Author:YZM
     * @Description:停止录制
     */
    public void stopRecord(){
        mediaRecorder.release();
        mediaRecorder = null;
        mediaRecorder = new MediaRecorder();
        surfaces.clear();
    }

    /**
     * @Author:YZM
     * @Description:开始录制
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void startRecord(Activity activity, Handler handler){
        //为mediaRecord设置一系列的属性
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);//设置视频编码格式
        mediaRecorder.setVideoSize(size.getWidth(),size.getHeight());//设置视频的宽和高
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);//设置音频编码格式
        mediaRecorder.setVideoFrameRate(30);//设置帧率
        mediaRecorder.setVideoEncodingBitRate(1920 * 1080);//设置分辨率
        mediaRecorder.setOrientationHint(90);//设置录制方向

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        String fname = "video_" + sdf.format(new Date()) + ".mp4";
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + File.separator + fname);
        Log.d("保存路径", "startRecord: "+file.getAbsolutePath());


        /*android 10*/
        //设置保存参数到ContentValues中
        ContentValues contentValues = new ContentValues();
        //设置文件名
        contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, fname);
        //兼容Android Q和以下版本
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //android Q中不再使用DATA字段，而用RELATIVE_PATH代替
            //RELATIVE_PATH是相对路径不是绝对路径
            //关于系统文件夹可以到系统自带的文件管理器中查看，不可以写没存在的名字
            contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM");
            //contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Music/sample");
        } else {
            contentValues.put(MediaStore.Video.Media.DATA, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath());
        }
        //设置文件类型
        contentValues.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        //执行insert操作，向系统文件夹中添加文件
        //EXTERNAL_CONTENT_URI代表外部存储器，该值不变
        Uri uri = activity.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = activity.getContentResolver().query(uri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            file = new File(cursor.getString(column_index));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.d("保存路径", "startRecord: "+file);
        //设置视频录制文件的保存路径
        mediaRecorder.setOutputFile(file);
        try{
            //初始化mediaRecord，然后开启
            mediaRecorder.prepare();
            mediaRecorder.start();
        }catch(IOException e){
            e.printStackTrace();
        }

        //设置CaptureRequest
        Surface previewSurface = surfaceView.getHolder().getSurface();
        surfaces.add(previewSurface);
        mPreviewBuilder.addTarget(previewSurface);
        Surface recordSurface = mediaRecorder.getSurface();
        surfaces.add(recordSurface);
        mPreviewBuilder.addTarget(recordSurface);
        try{
            //创建会话session
            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    //录像时同时开启预览，使得一直有画面
                    mCameraCaptureSession = session;
                    try {
                        mCameraCaptureSession.setRepeatingRequest(mPreviewBuilder.build(),null,handler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            },handler);
        }catch(CameraAccessException e){
            e.printStackTrace();
        }
    }
}
