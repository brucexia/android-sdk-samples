package com.affectiva.cameradetectordemo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.affectiva.android.affdex.sdk.Frame;
import com.affectiva.android.affdex.sdk.detector.CameraDetector;
import com.affectiva.android.affdex.sdk.detector.Detector;
import com.affectiva.android.affdex.sdk.detector.Face;
import com.affectiva.cameradetectordemo.convexhull.FastConvexHull;
import com.affectiva.cameradetectordemo.geomath.Geomath;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.ContentValues.TAG;

/**
 * This is a very bare sample app to demonstrate the usage of the CameraDetector object from Affectiva.
 * It displays statistics on frames per second, percentage of time a face was detected, and the user's smile score.
 * <p>
 * The app shows off the maneuverability of the SDK by allowing the user to start and stop the SDK and also hide the camera SurfaceView.
 * <p>
 * For use with SDK 2.02
 */
public class MainActivity extends Activity implements Detector.ImageListener, CameraDetector.CameraEventListener {

    final String LOG_TAG = "CameraDetectorDemo";

    Button startSDKButton;
    Button surfaceViewVisibilityButton;
    TextView smileTextView;
    TextView ageTextView;
    TextView ethnicityTextView;
    ToggleButton toggleButton;

    SurfaceView cameraPreview;

    boolean isCameraBack = false;
    boolean isSDKStarted = false;

    RelativeLayout mainLayout;

    CameraDetector detector;

    int previewWidth = 0;
    int previewHeight = 0;

    List<PointF> edgePoints;

    PointF centerOfFace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        smileTextView = (TextView) findViewById(R.id.smile_textview);
        ageTextView = (TextView) findViewById(R.id.age_textview);
        ethnicityTextView = (TextView) findViewById(R.id.ethnicity_textview);

        toggleButton = (ToggleButton) findViewById(R.id.front_back_toggle_button);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isCameraBack = isChecked;
                switchCamera(isCameraBack ? CameraDetector.CameraType.CAMERA_BACK : CameraDetector.CameraType.CAMERA_FRONT);
            }
        });

        startSDKButton = (Button) findViewById(R.id.sdk_start_button);
        startSDKButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSDKStarted) {
                    isSDKStarted = false;
                    stopDetector();
                    startSDKButton.setText("Start Camera");
                } else {
                    isSDKStarted = true;
                    startDetector();
                    startSDKButton.setText("Stop Camera");
                }
            }
        });
        startSDKButton.setText("Start Camera");

        //We create a custom SurfaceView that resizes itself to match the aspect ratio of the incoming camera frames
        mainLayout = (RelativeLayout) findViewById(R.id.main_layout);
        cameraPreview = new MySurfaceView(this);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        cameraPreview.setLayoutParams(params);
        mainLayout.addView(cameraPreview, 0);

        surfaceViewVisibilityButton = (Button) findViewById(R.id.surfaceview_visibility_button);
        surfaceViewVisibilityButton.setText("HIDE SURFACE VIEW");
        surfaceViewVisibilityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cameraPreview.getVisibility() == View.VISIBLE) {
                    cameraPreview.setVisibility(View.INVISIBLE);
                    surfaceViewVisibilityButton.setText("SHOW SURFACE VIEW");
                } else {
                    cameraPreview.setVisibility(View.VISIBLE);
                    surfaceViewVisibilityButton.setText("HIDE SURFACE VIEW");
                }
            }
        });

        detector = new CameraDetector(this, CameraDetector.CameraType.CAMERA_FRONT, cameraPreview);
        detector.setDetectSmile(true);
        detector.setDetectAge(true);
        detector.setDetectEthnicity(true);
        detector.setImageListener(this);
        detector.setOnCameraEventListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isSDKStarted) {
            startDetector();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopDetector();
    }

    void startDetector() {
        if (!detector.isRunning()) {
            detector.start();
        }
    }

    void stopDetector() {
        if (detector.isRunning()) {
            detector.stop();
        }
    }

    void switchCamera(CameraDetector.CameraType type) {
        detector.setCameraType(type);
    }

    Frame currentFrame;

    @Override
    public void onImageResults(List<Face> list, Frame frame, float v) {
        currentFrame = frame;
        if (list == null)
            return;
        if (list.size() == 0) {
            smileTextView.setText("NO FACE");
            ageTextView.setText("");
            ethnicityTextView.setText("");
        } else {
            Face face = list.get(0);
            FastConvexHull fastConvexHull = new FastConvexHull();
            ArrayList<PointF> tmp = new ArrayList<>(Arrays.asList(face.getFacePoints()));
            edgePoints = tmp;//fastConvexHull.execute(tmp);
            centerOfFace = Geomath.centroid(face.getFacePoints());
            byte[] pixels = ((Frame.ByteArrayFrame)frame).getByteArray();
            Log.d("onDraw", String.format("frameWidth %d, frameHeight %d, bytes %d, color type %s", frame.getWidth(), frame.getHeight(), pixels.length
            ,((Frame.ByteArrayFrame)frame).getColorFormat()));

            cameraPreview.invalidate();
            smileTextView.setText(String.format("SMILE\n%.2f", face.expressions.getSmile()));
            switch (face.appearance.getAge()) {
                case AGE_UNKNOWN:
                    ageTextView.setText("");
                    break;
                case AGE_UNDER_18:
                    ageTextView.setText(R.string.age_under_18);
                    break;
                case AGE_18_24:
                    ageTextView.setText(R.string.age_18_24);
                    break;
                case AGE_25_34:
                    ageTextView.setText(R.string.age_25_34);
                    break;
                case AGE_35_44:
                    ageTextView.setText(R.string.age_35_44);
                    break;
                case AGE_45_54:
                    ageTextView.setText(R.string.age_45_54);
                    break;
                case AGE_55_64:
                    ageTextView.setText(R.string.age_55_64);
                    break;
                case AGE_65_PLUS:
                    ageTextView.setText(R.string.age_over_64);
                    break;
            }

            switch (face.appearance.getEthnicity()) {
                case UNKNOWN:
                    ethnicityTextView.setText("");
                    break;
                case CAUCASIAN:
                    ethnicityTextView.setText(R.string.ethnicity_caucasian);
                    break;
                case BLACK_AFRICAN:
                    ethnicityTextView.setText(R.string.ethnicity_black_african);
                    break;
                case EAST_ASIAN:
                    ethnicityTextView.setText(R.string.ethnicity_east_asian);
                    break;
                case SOUTH_ASIAN:
                    ethnicityTextView.setText(R.string.ethnicity_south_asian);
                    break;
                case HISPANIC:
                    ethnicityTextView.setText(R.string.ethnicity_hispanic);
                    break;
            }

        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public void onCameraSizeSelected(int width, int height, Frame.ROTATE rotate) {
        if (rotate == Frame.ROTATE.BY_90_CCW || rotate == Frame.ROTATE.BY_90_CW) {
            previewWidth = height;
            previewHeight = width;
        } else {
            previewHeight = height;
            previewWidth = width;
        }
        cameraPreview.requestLayout();
    }

    class MySurfaceView extends SurfaceView implements SurfaceHolder.Callback {
        public MySurfaceView(Context context) {
            super(context);
            getHolder().addCallback(this);
        }

        @Override
        public void onMeasure(int widthSpec, int heightSpec) {
            int measureWidth = MeasureSpec.getSize(widthSpec);
            int measureHeight = MeasureSpec.getSize(heightSpec);
            int width;
            int height;
            if (previewHeight == 0 || previewWidth == 0) {
                width = measureWidth;
                height = measureHeight;
            } else {
                float viewAspectRatio = (float) measureWidth / measureHeight;
                float cameraPreviewAspectRatio = (float) previewWidth / previewHeight;

                if (cameraPreviewAspectRatio > viewAspectRatio) {
                    width = measureWidth;
                    height = (int) (measureWidth / cameraPreviewAspectRatio);
                } else {
                    width = (int) (measureHeight * cameraPreviewAspectRatio);
                    height = measureHeight;
                }
            }
            setMeasuredDimension(width, height);
        }

        TimerTask timerTask;
        boolean timedout;

        @Override
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (centerOfFace != null) {
//                float scaling = getScaling(this, currentFrame, canvas);
                int frameWidth = currentFrame.getWidth();
                int frameHeight = currentFrame.getHeight();
                int canvasWidth = canvas.getWidth();
                int canvasHeight = canvas.getHeight();
                int scaledWidth;
                int scaledHeight;
                int topOffset = 0;
                int leftOffset = 0;
                float radius = (float) canvasWidth / 100f;
                Frame.ROTATE frameRot = currentFrame.getTargetRotation();

                Bitmap bitmap;
                if (currentFrame instanceof Frame.BitmapFrame) {
                    bitmap = ((Frame.BitmapFrame)currentFrame).getBitmap();
                } else { //frame is ByteArrayFrame
                    byte[] pixels = ((Frame.ByteArrayFrame)currentFrame).getByteArray();
                    ByteBuffer buffer = ByteBuffer.wrap(pixels);
                    Log.d("onDraw", String.format("frameWidth %d, frameHeight %d, bytes %d", frameWidth, frameHeight, pixels.length));
//                    bitmap = BitmapFactory.decodeByteArray(pixels,0,pixels.length);
//                    Bitmap.createBitmap(frameWidth, frameHeight, new BuildConfig(currentFrame.getColorFormat()));
//                    Bitmap.createBitmap(frameWidth, frameHeight, Bitmap.Config.ARGB_8888);
//                    bitmap.copyPixelsFromBuffer(buffer);
//                    bitmap.recycle();
                }
                if (frameRot == Frame.ROTATE.BY_90_CCW || frameRot == Frame.ROTATE.BY_90_CW) {
                    int temp = frameWidth;
                    frameWidth = frameHeight;
                    frameHeight = temp;
                }

                float frameAspectRatio = (float) frameWidth / (float) frameHeight;
                float canvasAspectRatio = (float) canvasWidth / (float) canvasHeight;
                if (frameAspectRatio > canvasAspectRatio) { //width should be the same
                    scaledWidth = canvasWidth;
                    scaledHeight = (int) ((float) canvasWidth / frameAspectRatio);
                    topOffset = (canvasHeight - scaledHeight) / 2;
                } else { //height should be the same
                    scaledHeight = canvasHeight;
                    scaledWidth = (int) ((float) canvasHeight * frameAspectRatio);
                    leftOffset = (canvasWidth - scaledWidth) / 2;
                }

                float scaling = (float) scaledWidth / (float) currentFrame.getWidth();

                Log.d("onDraw", String.format("calculated scaling %f, leftOffset %d, topOffset %d", scaling, leftOffset, topOffset));
                if (timedout) {
                    Paint paint = new Paint();
                    paint.setColor(getColor(android.R.color.holo_red_dark));
                    paint.setStrokeWidth(50);
                    for (PointF pointF : edgePoints) {
                        canvas.drawPoint(pointF.x * scaling + leftOffset, pointF.y * scaling + topOffset, paint);
                    }
                } else {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                     bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.img_angrybird_head);
//                    canvas.drawBitmap(bitmap, centerOfFace.x, centerOfFace.y, null);
                    Paint paint = new Paint();
                    paint.setColor(getColor(android.R.color.holo_orange_dark));
                    paint.setStrokeWidth(50);
                    canvas.drawPoint(centerOfFace.x,centerOfFace.y,paint);
                    if (timerTask == null) {
                        timerTask = new TimerTask() {
                            @Override
                            public void run() {
                                timerTask = null;
                                timedout = true;
                            }
                        };
                        new Timer().schedule(timerTask, 5000);
                    }
                }
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            setWillNotDraw(false);
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

        }
    }
}
