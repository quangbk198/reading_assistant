package com.example.readingassistant2020;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.readingassistant2020.Retrofit.IUploadAPI;
import com.example.readingassistant2020.Utils.ApiUtils;
import com.example.readingassistant2020.Utils.ProgressRequestBody;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ImageToText extends AsyncTask <Bitmap, String, String> {
    private static final String FOLDER_NAME = "Photo";
    public static final String FILE_NAME = "imageDewarp_";
    public static final String ROOT_FOLDER = "ReadingAssistant2020";

    IUploadAPI mService;
    String result;
    final Locale loc = new Locale("vi");

    private ProgressDialog progressDialog;
    private Context context;
    private TextView textView;
    private TextToSpeech toSpeech;

    public ImageToText(Context context, TextView textView, TextToSpeech toSpeech) {
        this.context = context;
        this.textView = textView;
        this.toSpeech = toSpeech;
        progressDialog = new ProgressDialog(context);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mService = ApiUtils.getAPIService();
        progressDialog.setMessage("Processing! Please wait.....");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    @Override
    protected String doInBackground(Bitmap... bitmaps) {
        Bitmap dewarp = dewarpImage(bitmaps[0], Bitmap.Config.ARGB_8888);
        FileOutputStream fileOutputStream = null;
        File imageDewarp = createImageFile();

        try {
            fileOutputStream = new FileOutputStream(imageDewarp);
            dewarp.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Upload ảnh lên server
        final ProgressRequestBody requestBody = new ProgressRequestBody(imageDewarp);
        final MultipartBody.Part body = MultipartBody.Part.createFormData("image", imageDewarp.getName(), requestBody);

        mService.uploadFile(body).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                textView.setText(response.body().toString());
                result = textView.getText().toString().trim();
                progressDialog.dismiss();
                toSpeech.speak(result, TextToSpeech.QUEUE_FLUSH, null);
//                toSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
//                    @Override
//                    public void onInit(int status) {
//                        if (status != TextToSpeech.ERROR) {
//                            toSpeech.setLanguage(loc);
//                            toSpeech.speak(result, TextToSpeech.QUEUE_FLUSH, null);
//                        }
//                    }
//                });
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(context, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });

        return result;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

    }

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

    private native Bitmap dewarpImage(Bitmap bitmap, Bitmap.Config argb8888);
}
