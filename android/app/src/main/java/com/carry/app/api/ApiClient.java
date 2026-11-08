package com.carry.app.api;

import android.content.Context;

import com.carry.app.utils.SessionManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static Retrofit retrofit = null;
    private static ApiService apiService = null;
    private static String currentBaseUrl = null;

    public static synchronized ApiService getService(Context context) {
        SessionManager session = SessionManager.getInstance(context);
        String baseUrl = session.getBaseUrl();

        if (apiService == null || !baseUrl.equals(currentBaseUrl)) {
            currentBaseUrl = baseUrl;

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            Interceptor authInterceptor = chain -> {
                Request original = chain.request();
                Request.Builder builder = original.newBuilder()
                        .header("Accept", "application/json");

                String token = session.getToken();
                if (token != null && !token.trim().isEmpty()) {
                    builder.header("Authorization", "Bearer " + token);
                }

                Response response = chain.proceed(builder.build());
                if (response.code() == 401 || (response.code() == 403 && (session.isTokenExpired() || original.url().encodedPath().contains("/api/users/")))) {
                    session.logout();
                    new com.carry.app.data.firebase.FirebaseAuthGatewayImpl().signOut();
                }
                return response;
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(authInterceptor)
                    .addInterceptor(logging)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();

            apiService = retrofit.create(ApiService.class);
        }

        return apiService;
    }

    public static synchronized void reset() {
        retrofit = null;
        apiService = null;
        currentBaseUrl = null;
    }
}
