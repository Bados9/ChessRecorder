package chess.jiruska.chessrecorder;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class RecordingActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_RESULT = 0;
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAIT_LOCK = 1;
    private static final int COLOR_RED = 0;
    private static final int COLOR_GREEN = 1;
    private int templateColor = COLOR_RED;
    private int captureState = STATE_PREVIEW;
    private ImageView templateWindow;
    private TextureView textureView;
    private TextView textView;

    private TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
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

    private CameraDevice cameraDevice;
    private CameraDevice.StateCallback cameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            startPreview();
            //Toast.makeText(getApplicationContext(), "Pripojena kamera", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null;
        }
    };
    private HandlerThread backgroundHandlerThread;
    private Handler backgroundHandler;
    private String cameraId;
    private Size previewSize;
    private CameraCaptureSession previewCaptureSession;
    private CameraCaptureSession.CaptureCallback previewCaptureCallback = new
            CameraCaptureSession.CaptureCallback() {

                private void process(CaptureResult captureResult) {
                    switch(captureState){
                        case STATE_PREVIEW:
                            break;
                        case STATE_WAIT_LOCK:
                            captureState = STATE_PREVIEW;
                            Integer afState = captureResult.get(CaptureResult.CONTROL_AF_STATE);
                            if(afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                                    afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                                Toast.makeText(getApplicationContext(), "AF Locked..", Toast.LENGTH_SHORT).show();
                                Log.d("LOCKING", "SUCCESFULL");
                                captureImage();
                            }
                            break;
                    }
                }

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    process(result);
                }
            };
    private CaptureRequest.Builder captureRequestBuilder;

    private Size imageSize;
    private ImageReader imageReader;
    private final ImageReader.OnImageAvailableListener onImageAvailableListener = new
            ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    backgroundHandler.post(new ImageProcessor(reader.acquireNextImage(), manager));
                }
            };

    private ImageButton recordButton;
    private boolean isRecording = false;

    private static SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private static class CompareSizeByArea implements Comparator<Size> {

        @Override
        public int compare(Size o1, Size o2) {
            return Long.signum((long) o1.getWidth() * o1.getHeight() /
                    (long) o2.getWidth() * o2.getHeight());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(this.getSupportActionBar()).hide();
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_recording);
        textView = findViewById(R.id.textView);
        textureView = findViewById(R.id.textureView);
        recordButton = findViewById((R.id.recordButton));
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isRecording){
                    isRecording = false;
                    recordButton.setImageResource(R.drawable.rec_btn_start);
                    stopRecording();
                } else {
                    isRecording = true;
                    recordButton.setImageResource(R.drawable.rec_btn_stop);
                    startRecording();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        startBackgroundThread();
        if (textureView.isAvailable()) {
            setupCamera(textureView.getWidth(), textureView.getHeight());
            connectCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CAMERA_RESULT){
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(getApplicationContext(), "Application will not run without camera services", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onPause() {
        closeCamera();

        stopBackgroundThread();
        super.onPause();
    }

    private int totalRotation;

    private void setupCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        System.out.println("camera setup");
        try {
            for (String cameraID : Objects.requireNonNull(cameraManager).getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraID);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                totalRotation = sensorToDeviceRotation(cameraCharacteristics, deviceOrientation);
                boolean swapRotation = totalRotation == 90 || totalRotation == 270;
                int rotatedWidth = width;
                int rotatedHeight = height;
                if (swapRotation) {
                    rotatedWidth = height;
                    rotatedHeight = width;
                }

                previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);
                imageSize = chooseOptimalSize(map.getOutputSizes(ImageFormat.JPEG), rotatedWidth, rotatedHeight);
                configureTransform(textureView.getWidth(), textureView.getHeight());
                imageReader = ImageReader.newInstance(imageSize.getWidth(), imageSize.getHeight(), ImageFormat.JPEG, 1);
                imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler);
                cameraId = cameraID;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == textureView || null == previewSize) {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / previewSize.getHeight(),
                    (float) viewWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        textureView.setTransform(matrix);
    }

    private void connectCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
                    Toast.makeText(this, "This app requires camera permissions", Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_RESULT);
                return;
            }
            cameraManager.openCamera(cameraId, cameraDeviceStateCallback, backgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startPreview(){
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);

        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);

            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            previewCaptureSession = session;
                            try {
                                previewCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Toast.makeText(getApplicationContext(), "Unable to setup camera", Toast.LENGTH_SHORT).show();
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera(){
        if(cameraDevice != null){
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private void startBackgroundThread(){
        backgroundHandlerThread = new HandlerThread("camera2Thread");
        backgroundHandlerThread.start();
        backgroundHandler = new Handler(backgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread(){
        backgroundHandlerThread.quitSafely();
        try {
            backgroundHandlerThread.join();
            backgroundHandlerThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation){
        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height){
        List<Size> bigEnough = new ArrayList<>();
        for(Size option : choices){
            if(option.getHeight() == option.getWidth() * height / width &&
                    option.getWidth() >= width && option.getHeight() >= height){
                bigEnough.add(option);
            }
        }
        if(bigEnough.size()>0){
            return Collections.min(bigEnough, new CompareSizeByArea());
        } else {
            return choices[0];
        }
    }

    private void lockFocus() {
        captureState = STATE_WAIT_LOCK;
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
        try {
            previewCaptureSession.capture(captureRequestBuilder.build(), previewCaptureCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void captureImage(){
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(imageReader.getSurface());

            previewCaptureSession.capture(captureRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Handler handler = new Handler();
    int delay = 1000;
    private RecorderManager manager;
    private GameWriter writer;
    private boolean changedColor = false;

    private void startRecording(){
        manager = new RecorderManager(getApplicationContext());
        writer = new GameWriter(getApplicationContext());
        templateWindow = findViewById(R.id.imageView);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                lockFocus();
                //System.out.println("Vola se manager getMove");
                if (manager.isBoardDetected()){
                    if (templateColor == COLOR_RED){
                        templateColor = COLOR_GREEN;
                        changedColor = true;
                        templateWindow.setImageResource(R.drawable.template_green);
                        delay = 5000;
                    }
                    //System.out.println("zmenit barvu na zelenou");
                    String move = manager.getMove();
                    textView.append("\n"+move);
                    writer.writeMove(move);
                    handler.postDelayed(this, delay);
                } else {
                    if (templateColor == COLOR_GREEN){
                        templateColor = COLOR_RED;
                        templateWindow.setImageResource(R.drawable.template_red);
                        delay = 1000;
                    }
                    //System.out.println("nechat cervene");
                    handler.postDelayed(this, delay);
                }
            }
        },1);
    }

    private void stopRecording(){
        handler.removeCallbacksAndMessages(null);
        String moves = writer.getMoves();
        Intent intent = new Intent(this, GameSaveActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("moves", moves);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private class ImageProcessor implements Runnable {

        private final Image image;
        private RecorderManager manager;
        public ImageProcessor(Image img, RecorderManager m){
            image = img;
            manager = m;
        }
        @Override
        public void run(){
            ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            //System.out.println("Vola se manager newState");
            //System.out.println("aaa ... vlakno id = " + Thread.currentThread().getId());
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    manager.newState(bytes);
                }
            });
            image.close();

        }
    }
}
