package com.example.readingassistant2020.Utils;

import com.example.readingassistant2020.Retrofit.IUploadAPI;
import com.example.readingassistant2020.Retrofit.RetrofitClient;

public class ApiUtils {
    private ApiUtils() {}
    public static final String BASE_URL = "http://192.168.1.103:5000/";

    public static IUploadAPI getAPIService() {
        return RetrofitClient.getClient(BASE_URL).create(IUploadAPI.class);
    }
}
