package com.example.mylibrary;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.zxing.client.android.AutoScannerView;
import com.google.zxing.client.android.camera.CameraManager;

import java.io.IOException;
import java.util.List;

public class ImageActivity extends AppCompatActivity implements View.OnClickListener
        , SurfaceHolder.Callback, MediaRecorder.OnErrorListener {

    //存放照片的文件夹
    private SurfaceView mSurfaceView;
    private ImageView startBtn;
    private ImageView lightBtn;
    private ImageView tag_start;
    private ImageView exitBtn;
    private ImageView switchCamera;
    private ImageView iv_show;
    private TextView txt_title;
    private LinearLayout ll_confirm;
    private LinearLayout ll_cancle;


    private AnimationDrawable anim;
    private LinearLayout lay_tool;
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private boolean isStarting = false;
    List<int[]> mFpsRange;
    private Camera.Size optimalSize;
    private Camera.Parameters parameters;
    private boolean isFlashLightOn = false;
    //摄像头默认是后置， 0：前置， 1：后置
    private int cameraPosition = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);// 去掉标题栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);// 设置全屏
        super.onCreate(savedInstanceState);
        setContentView(R.layout.img_main);
        initView();
    }

    private void initView() {

        txt_title = (TextView) findViewById(R.id.txt_title);
        lightBtn = (ImageView) findViewById(R.id.lightBtn);
        tag_start = (ImageView) findViewById(R.id.tag_start);
        lay_tool = (LinearLayout) findViewById(R.id.lay_tool);
        exitBtn = (ImageView) findViewById(R.id.exitBtn);
        switchCamera = (ImageView) findViewById(R.id.switchCamera);
        ll_cancle = (LinearLayout) findViewById(R.id.ll_cancle);
        ll_confirm = (LinearLayout) findViewById(R.id.ll_confirm);
        iv_show = (ImageView) findViewById(R.id.iv_show);

        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        mSurfaceHolder = mSurfaceView.getHolder();// 取得holder
        mSurfaceHolder.addCallback(this); // holder加入回调接口
        mSurfaceHolder.setKeepScreenOn(true);

        startBtn = (ImageView) findViewById(R.id.startBtn);
        startBtn.setOnClickListener(this);

        exitBtn.setOnClickListener(this);
        switchCamera.setOnClickListener(this);
        lightBtn.setOnClickListener(this);
        ll_cancle.setOnClickListener(this);
        ll_confirm.setOnClickListener(this);

        AlphaAnimation alphaAnimation = (AlphaAnimation) AnimationUtils.loadAnimation(this, R.anim.txt_alpha);
        txt_title.startAnimation(alphaAnimation);


    }

    /**
     * 释放摄像头资源
     */
    private void freeCameraResource() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.lock();
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * 初始化
     */
    private void initRecord() {
        try {
            // Use the same size for recording profile.
            CamcorderProfile mProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
            mProfile.videoFrameWidth = optimalSize.width;
            mProfile.videoFrameHeight = optimalSize.height;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void switchCamera() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();//得到摄像头的个数
        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, cameraInfo);//得到每一个摄像头的信息
            if (cameraPosition == 1) {
                //现在是后置，变更为前置
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置
                    mCamera.stopPreview();//停掉原来摄像头的预览
                    mCamera.release();//释放资源
                    mCamera = null;//取消原来摄像头
                    mCamera = Camera.open(i);//打开当前选中的摄像头
                    try {
                        mCamera.setDisplayOrientation(90);
                        mCamera.setPreviewDisplay(mSurfaceHolder);//通过surfaceview显示取景画面
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    mCamera.startPreview();//开始预览
                    cameraPosition = 0;
                    break;
                }
            } else {
                //现在是前置， 变更为后置
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置
                    mCamera.stopPreview();//停掉原来摄像头的预览
                    mCamera.release();//释放资源
                    mCamera = null;//取消原来摄像头
                    mCamera = Camera.open(i);//打开当前选中的摄像头
                    try {
                        mCamera.setDisplayOrientation(90);
                        mCamera.setPreviewDisplay(mSurfaceHolder);//通过surfaceview显示取景画面
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mCamera.setParameters(parameters);// 设置相机参数
                    mCamera.startPreview();//开始预览
                    cameraPosition = 1;
                    break;
                }
            }

        }

        mCamera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] bytes, Camera camera) {
            }
        });
    }


    @Override
    protected void onStop() {
        super.onStop();
        stop();
    }

    /**
     * 停止拍摄
     */
    public void stop() {
        stopRecord();
        freeCameraResource();
    }

    /**
     * 停止录制
     */
    public void stopRecord() {
        isStarting = false;
        tag_start.setVisibility(View.GONE);
        anim.stop();
        lay_tool.setVisibility(View.VISIBLE);
    }


    @Override
    public void onBackPressed() {
        stop();
        finish();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.exitBtn) {
            stop();
            finish();

        } else if (i == R.id.lightBtn) {
            flashLightToggle();

        } else if (i == R.id.switchCamera) {
            switchCamera();

        } else if (i == R.id.startBtn) {
            takePic();

        } else if (i == R.id.ll_cancle) {

        } else if (i == R.id.ll_confirm) {

        }
    }

    /**
     * 拍照
     */
    private void takePic() {
        mCamera.takePicture(null, null, new TakePictureCallback());
    }

    private final class TakePictureCallback implements Camera.PictureCallback {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            byte[] tempData = data;
            camera.startPreview();
            if (tempData != null && tempData.length > 0) {
                toggleLL();
                Bitmap bitmap = BitmapFactory.decodeByteArray(tempData, 0, tempData.length);
                Matrix matrix = new Matrix();
                matrix.reset();
                matrix.setRotate(90);
                Bitmap newBitmap =
                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix,
                                true);
                iv_show.setVisibility(View.VISIBLE);
                mSurfaceView.setVisibility(View.GONE);
                iv_show.setImageBitmap(newBitmap);
            }
        }
    }

    public void toggleLL() {
        if (ll_confirm.getVisibility() == View.VISIBLE) {
            ll_confirm.setVisibility(View.GONE);
        } else {
            ll_confirm.setVisibility(View.VISIBLE);
        }

        if (ll_cancle.getVisibility() == View.VISIBLE) {
            ll_cancle.setVisibility(View.GONE);
        } else {
            ll_cancle.setVisibility(View.VISIBLE);
        }
    }
    private void flashLightToggle() {
        try {
            if (isFlashLightOn) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                mCamera.setParameters(parameters);
                isFlashLightOn = false;
            } else {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                mCamera.setParameters(parameters);
                isFlashLightOn = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mCamera != null) {
            freeCameraResource();
        }
        try {
            mCamera = Camera.open();
            if (mCamera == null)
                return;
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewDisplay(mSurfaceHolder);
            parameters = mCamera.getParameters();// 获得相机参数

            List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
            List<Camera.Size> mSupportedVideoSizes = parameters.getSupportedVideoSizes();
            optimalSize = CameraHelper.getOptimalVideoSize(mSupportedVideoSizes,
                    mSupportedPreviewSizes, height, width);

            parameters.setPreviewSize(optimalSize.width, optimalSize.height); // 设置预览图像大小
            parameters.setPictureSize(optimalSize.width, optimalSize.height);

            parameters.set("orientation", "portrait");

            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes.contains("continuous-video")) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }
            mFpsRange = parameters.getSupportedPreviewFpsRange();
            mCamera.setParameters(parameters);// 设置相机参数
            mCamera.startPreview();// 开始预览
        } catch (Exception io) {
            io.printStackTrace();
        }
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        freeCameraResource();
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        try {
            if (mr != null)
                mr.reset();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

