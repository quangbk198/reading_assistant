package com.example.readingassistant2020;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class CaptureImageActivity extends AppCompatActivity {
    static {
        System.loadLibrary("native-lib");
    }

    CharSequence nghieng_len = "nghiêng lên";
    CharSequence nghieng_xuong = "nghiêng xuống ";
    CharSequence nghieng_trai = "nghiêng trái ";
    CharSequence nghieng_phai = "nghiêng phải ";
    CharSequence sang_trai = "đưa sang trái";
    CharSequence sang_phai = "đưa sang phải";
    CharSequence len_tren = "tiến ra trước";
    CharSequence xuong_duoi = "lùi ra sau";
    CharSequence nang_len = "nâng lên";
    CharSequence ha_xuong = "hạ xuống";

    private TextToSpeech toSpeech;

    private static final String FOLDER_NAME = "Photo";
    public static final String FILE_NAME = "image_";
    public static final String ROOT_FOLDER = "ReadingAssistant2020";

    private static final  int STATE_PREVIEW = 0;
    private static final  int STATE_WAIT_LOCK = 1;
    private int mCaptureState = STATE_PREVIEW;

    private TextureView mTextureView;   //Bước 1: Khởi tạo textureView

    // Bước 2: Khởi tạo SurfaceTextureListener để lắng nghe sự kiện của textureView
    // TextureView.SurfaceTextureListener sẽ xử lý một số sự kiện trên lifecycle của textureview
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            // Khi TextureSurface sẵn sàng, gọi hàm openCamera
            setupCamera(width, height);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    // Bước 4: Khởi tạo cameradevice và cameradevicestatecallback
    // CameraDevice chính ra camera vật lý trên thiết bị điện thoại. Trong thuộc tính này, chúng ta sẽ lưu ID
    // của cameradevice hiện tại
    private CameraDevice mCameraDevice;     // A representation of a single camera connected to an Android device

    // CameraDevice.StateCallback được gọi khi CameraDevice thay đổi trạng thái của nó
    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
            finish();
        }
    };

    // Bước 9: Khai báo 1 thread mới
    private HandlerThread mBackgroundHandlerThread;     // An additional thread for running tasks that shouldn't block the UI
    private Handler mBackgroundHandler;                 // A Handler for running tasks in the background

    // Bước 7: Khai báo cameraID
    private String mCameraID;   // cameraID được lấy trong bước 8
    private Size mPreviewSize;

    // Bước 21: Khai báo mImageSize, mImageReader và mOnImageAvailableListener
    private Size mImageSize;

    // The ImageReader class allows direct application access to image data rendered into a Surface
    private ImageReader mImageReader;       // An ImageReader that handles still image capture
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new
            ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    // reader.acquireLatestImage(): Acquire the latest Image from the ImageReader's queue,
                    // dropping older images. Returns null if no new image is available
                    mBackgroundHandler.post(new ImageSaver(reader.acquireLatestImage()));
                }
            };

    public static Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
    int action;
    final Locale loc = new Locale("vi");        // setup for texttospeech
    String utteranceId = UUID.randomUUID().toString();   // setup for texttospeech

    private File mImageFileName;            // Bước 27
    // Bước 32: Tạo class ImageSaver
    private class ImageSaver implements Runnable {
        private final Image mImage;

        public ImageSaver(Image mImage) {
            this.mImage = mImage;
        }

        @Override
        public void run() {
            ByteBuffer byteBuffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            mImage.close();

            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            Mat mymat = new Mat();
            int w1 = bitmap.getWidth();
            int h1 = bitmap.getHeight();

            if (w1 > h1) {
                bitmap = RotateBitmap(bitmap, 90);
            }

            Utils.bitmapToMat(bitmap, mymat);

            // Lấy hướng dẫn chụp ảnh
            action = getAction(mymat.nativeObj);
            toSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.ERROR) {
                        toSpeech.setLanguage(loc);
                        switch (action) {
                            case 1:
                                toSpeech.speak(nghieng_len, TextToSpeech.QUEUE_FLUSH,null, utteranceId);
                                break;
                            case 2:
                                toSpeech.speak(nghieng_xuong, TextToSpeech.QUEUE_FLUSH,null, utteranceId);
                                break;
                            case 3:
                                toSpeech.speak(nghieng_trai, TextToSpeech.QUEUE_FLUSH,null, utteranceId);
                                break;
                            case 4:
                                toSpeech.speak(nghieng_phai, TextToSpeech.QUEUE_FLUSH,null, utteranceId);
                                break;
                            case 5:
                                toSpeech.speak(sang_trai, TextToSpeech.QUEUE_FLUSH,null, utteranceId);
                                break;
                            case 6:
                                toSpeech.speak(sang_phai, TextToSpeech.QUEUE_FLUSH,null, utteranceId);
                                break;
                            case 7:
                                toSpeech.speak(len_tren, TextToSpeech.QUEUE_FLUSH,null, utteranceId);
                                break;
                            case 8:
                                toSpeech.speak(xuong_duoi, TextToSpeech.QUEUE_FLUSH,null, utteranceId);
                                break;
                            case 9:
                                toSpeech.speak(nang_len, TextToSpeech.QUEUE_FLUSH,null, utteranceId);
                                break;
                            case 10:
                                toSpeech.speak(ha_xuong, TextToSpeech.QUEUE_FLUSH,null, utteranceId);
                                break;
                            default:
                                break;
                        }
                    }
                }
            });

            if (action == 11) {
                FileOutputStream fileOutputStream = null;
                mImageFileName = createImageFile();

                try {
                    fileOutputStream = new FileOutputStream(mImageFileName);
                    fileOutputStream.write(bytes);
                    fileOutputStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Intent intent = new Intent(CaptureImageActivity.this, ResultActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("PathImageOriginal", mImageFileName.getAbsolutePath());
                intent.putExtras(bundle);
                startActivity(intent);
                finish();
            }
        }
        private native int getAction(long inp);
    }

    //Bước 23: Khai báo mPreviewCaptureSession, mPreviewCaptureCallback
    // CameraCaptureSession: A configured capture session for a CameraDevice, used for capturing images from the camera
    // or reprocessing images captured from the camera in the same session previously
    private CameraCaptureSession mPreviewCaptureSession;
    // Callback này dc sử dụng trong startPreview().
    // Hàm này chỉ đơn giản là hiển thị hình ảnh thu về từ Camera
    private CameraCaptureSession.CaptureCallback mPreviewCaptureCallback = new
            CameraCaptureSession.CaptureCallback() {
                private void process(CaptureResult captureResult) {
                    switch (mCaptureState) {
                        case STATE_PREVIEW:
                            // Do nothing when the camera preview is working normally.
                            break;
                        case STATE_WAIT_LOCK:
                            Integer afState = captureResult.get(CaptureResult.CONTROL_AF_STATE);
                            if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ) {
                                unlockFocus();
                                startStillCaptureRequest();     // Bước 31
                            }
                            break;
                    }
                }

                @Override
                public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                }

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    process(result);
                }

                @Override
                public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                    super.onCaptureFailed(session, request, failure);
                }
            };

    private CaptureRequest.Builder mCaptureRequestBuilder;  // Bước 19: CaptureRequest.Builder for the camera preview


    // Bước 12
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_image);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        mTextureView = findViewById(R.id.textureViewCamera);
    }

    @Override
    protected void onResume() {         //Bước 3: override hàm onResume
        super.onResume();
        startBackgroundThread();
        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (mTextureView.isAvailable()) {
            setupCamera(mTextureView.getWidth(), mTextureView.getHeight());
            connectCamera();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    //Bước 6: override hàm onPause
    @Override
    protected void onPause() {
        mTimer.cancel();
        closeCamera();
        stopBackgroundThread();
        if (toSpeech != null) {
            toSpeech.stop();
            toSpeech.shutdown();
        }
        super.onPause();
    }

    // Bước 8: Khai báo hàm setupCamera()
    private void setupCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                // CameraCharacteristics: The properties describing a CameraDevice.
                // These properties are fixed for a given CameraDevice,
                // and can be queried through the CameraManager interface with getCameraCharacteristics(String)

                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;       // Sử dụng camera sau
                }
                // Bước 16
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                // Bước 17
                mPreviewSize = getPreferredPreviewsSize(map.getOutputSizes(SurfaceTexture.class), width, height);

                // Bước 22
                mImageSize = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new Comparator<Size>() {
                            @Override
                            public int compare(Size lhs, Size rhs) {
                                return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
                            }
                        }
                );
                mImageReader = ImageReader.newInstance(
                        mImageSize.getWidth(),
                        mImageSize.getHeight(),
                        ImageFormat.JPEG,
                        1);
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);
                //

                mCameraID = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // Bước 15: Khai báo hàm getPreferredPreviewsSize
    private Size getPreferredPreviewsSize(Size[] mapSize, int width, int height) {
        List<Size> collectorSize = new ArrayList<>();
        for (Size option : mapSize) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    collectorSize.add(option);
                }
            } else {
                if (option.getWidth() > height && option.getHeight() > width) {
                    collectorSize.add(option);
                }
            }
        }
        if (collectorSize.size() > 0) {
            return Collections.min(collectorSize, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getHeight() * rhs.getWidth());
                }
            });
        }
        return mapSize[0];
    }

    // Bước 18: Khai báo hàm connectCamera();
    Timer mTimer;
    private Handler mHandlerTakePicture;
    private void connectCamera() {
        // CameraManager: A system service manager for detecting, characterizing, and connecting to CameraDevices.
        // You can get an instance of this class by calling Context.getSystemService()

        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            cameraManager.openCamera(mCameraID, mCameraDeviceStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        // Bước 26: Tạo timer để cứ sau 5s là lock focus chụp ảnh 1 lần
        mHandlerTakePicture = new Handler();
        mTimer = new Timer();
        TimerTask mTimerTask = new TimerTask() {
            @Override
            public void run() {
                mHandlerTakePicture.post(new Runnable() {
                    @Override
                    public void run() {
                        lockFocus();        // Take photo
                    }
                });
            }
        };
        mTimer.schedule(mTimerTask, 3000, 6000);
    }

    // Bước 20: Khai báo hàm startPreview()
    private void startPreview() {
        try {
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());     // Set the default size of the image buffers.
            Surface previewSurface = new Surface(surfaceTexture);       // This is the output Surface we need to start preview.

            // We set up a CaptureRequest.Builder with the output Surface.
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            if (mCameraDevice == null) {
                                return;
                            }
                            try {
                                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                mPreviewCaptureSession = session;       // Bước 24
                                mPreviewCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(),
                                        mPreviewCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Toast.makeText(CaptureImageActivity.this, "Unable to setup camera preview", Toast.LENGTH_SHORT).show();
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // Bước 29:
    private void startStillCaptureRequest() {
        try {
            CaptureRequest.Builder captureStill = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureStill.addTarget(mImageReader.getSurface());

            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureStill.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            captureStill.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            CameraCaptureSession.CaptureCallback stillCaptureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);

                    unlockFocus();
                }
            };
            mPreviewCaptureSession.capture(captureStill.build(), stillCaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    //Bước 5: Khai báo hàm closeCamera
    private void closeCamera() {
        if (mPreviewCaptureSession != null) {
            mPreviewCaptureSession.close();
            mPreviewCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if(mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    // Bước 10: Khai báo hàm startBackgroundThread()
    private void startBackgroundThread() {
        mBackgroundHandlerThread = new HandlerThread("Camera2VideoImage");
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
    }

    // Bước 11: Khai báo hàm stopBackgroundThread()
    private void stopBackgroundThread() {
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Bước 25: Tạo hàm lockFocus(): Lock the focus as the first step for a still image capture
    private void lockFocus() {
        try {
            mCaptureState = STATE_WAIT_LOCK;
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
            mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(), mPreviewCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // Bước 30
    private void unlockFocus() {
        try {
            mCaptureState = STATE_PREVIEW;
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);

            mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(),
                    mPreviewCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // Bước 28: Tạo hàm createImageFile() để tạo folder lưu trữ hình ảnh chụp được vào bộ nhớ điện thoại
    private File createImageFile() {
        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() +
                File.separator + ROOT_FOLDER +
                File.separator + FOLDER_NAME;
        File dir = new File(dirPath);

        if (!dir.exists()) {
            dir.mkdirs();
        }
        String fileName = FILE_NAME + String.valueOf(System.currentTimeMillis()) + ".jpg";
        return new File(dir, fileName);
    }
}