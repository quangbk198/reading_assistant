package com.example.readingassistant2020.Retrofit;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface IUploadAPI {
    @Multipart
    @POST("/upload/tesseractocr")
    Call<String> uploadFile(@Part MultipartBody.Part file);
}
