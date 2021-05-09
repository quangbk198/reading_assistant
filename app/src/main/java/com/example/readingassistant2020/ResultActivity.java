package com.example.readingassistant2020;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.UUID;

public class ResultActivity extends AppCompatActivity {
    static {
        System.loadLibrary("native-lib");
    }

    TextToSpeech toSpeech;
    final Locale loc = new Locale("vi");        // setup for texttospeech
    String utteranceId = UUID.randomUUID().toString();   // setup for texttospeech

    private String pathImageOriginal;

    Button btnReplay, btnCap;
    TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        initWidget();
        initTextToSpeech();
        getPathImageOriginal();

        Bitmap bitmap = BitmapFactory.decodeFile(pathImageOriginal);

        int w1 = bitmap.getWidth();
        int h1 = bitmap.getHeight();

        if (w1 > h1) {
            bitmap = RotateBitmap(bitmap, 90);
        }
        new ImageToText(ResultActivity.this, tvResult, toSpeech).execute(bitmap);

        clickView();
    }

    @Override
    protected void onPause() {
        if (toSpeech != null) {
            toSpeech.stop();
            toSpeech.shutdown();
        }

        super.onPause();
    }

    private void clickView() {
        btnCap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ResultActivity.this, CaptureImageActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btnReplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String result = tvResult.getText().toString().trim();
                toSpeech.speak(result, TextToSpeech.QUEUE_FLUSH, null);
            }
        });
    }

    private void initWidget() {
        btnCap = findViewById(R.id.buttonContinueCapture);
        btnReplay = findViewById(R.id.buttonReplay);
        tvResult = findViewById(R.id.textViewResult);
        tvResult.setMovementMethod(new ScrollingMovementMethod());      // scroll text in textview
    }

    private void getPathImageOriginal() {
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            pathImageOriginal = bundle.getString("PathImageOriginal");
        }
    }

    private void initTextToSpeech() {
        toSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    toSpeech.setLanguage(loc);
                }
            }
        });
    }

    public static Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
}