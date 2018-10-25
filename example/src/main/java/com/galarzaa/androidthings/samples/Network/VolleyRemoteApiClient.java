package com.galarzaa.androidthings.samples.Network;

import android.content.Context;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import java.io.File;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by Le Trong Nhan on 08/06/2018.
 */

public class VolleyRemoteApiClient implements RemoteApiClient {
    private static final String TAG_URL = "URL";
    private static final String TAG_RESPONSE = "RESPONSE";
    private static VolleyRemoteApiClient mInstance;
    private static RequestQueue mRequestQueue;
    private final int MY_SOCKET_TIMEOUT_MS = 15000;

    private VolleyRemoteApiClient(Context context) {
        enableHttpsRequest();
        mRequestQueue = Volley.newRequestQueue(context);
    }

    public static void createInstance(Context context) {
        if (mInstance == null) {
            mInstance = new VolleyRemoteApiClient(context);
        }
    }

    public static VolleyRemoteApiClient getInstance() {
        return mInstance;
    }

    @Override
    public void get(String requestUrl, ApiResponseListener<String> apiResponseListener) {
        //
    }

    @Override
    public void get(String requestUrl, Map<String, String> header, final ApiResponseListener<String> listener) {
        Log.d(TAG_URL, requestUrl);
        final StringRequest request = new StringRequest(Request.Method.GET, requestUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG_RESPONSE, response);
                listener.onSuccess(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onError(error.getMessage());
            }
        });
        request.setHeaders(header);
        request.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        mRequestQueue.add(request);
    }

    @Override
    public void post(String requestUrl, String params, final ApiResponseListener<String> apiResponseListener) {
        Log.d(TAG_URL, requestUrl);
        Log.d(TAG_URL, params);
        StringRequest request = new StringRequest(Request.Method.POST, requestUrl, params, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG_RESPONSE, response);
                apiResponseListener.onSuccess(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                apiResponseListener.onError(error.getMessage());
            }
        });

        mRequestQueue.add(request);
    }

    @Override
    public void multipartRequest(String requestUrl, File imageFile, final ApiResponseListener<String> apiResponseListener) {

    }

    @Override
    public void put(String requestUrl, final ApiResponseListener<String> apiResponseListener) {

    }

    @Override
    public void put(String requestUrl, String params, final ApiResponseListener<String> apiResponseListener) {

    }

    @Override
    public void post(String requestUrl, final ApiResponseListener<String> apiResponseListener) {
        Log.d(TAG_URL, requestUrl);
        StringRequest request = new StringRequest(Request.Method.POST, requestUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG_RESPONSE, response);
                apiResponseListener.onSuccess(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                apiResponseListener.onError(error.getMessage());
            }
        });

        mRequestQueue.add(request);
    }

    @Override
    public void delete(String requestUrl, final ApiResponseListener<String> apiResponseListener) {
        Log.d(TAG_URL, requestUrl);
        StringRequest request = new StringRequest(Request.Method.DELETE, requestUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG_RESPONSE, response);
                apiResponseListener.onSuccess(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                apiResponseListener.onError(error.getMessage());
            }
        });

        mRequestQueue.add(request);
    }

    @Override
    public void post(String requestUrl, String params, Map<String, String> header, final ApiResponseListener<String> apiResponseListener) {
        Log.d(TAG_URL, requestUrl);
        StringRequest request = new StringRequest(Request.Method.POST, requestUrl, params, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG_RESPONSE, response);
                apiResponseListener.onSuccess(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                apiResponseListener.onError(error.getMessage());
            }
        });

        request.setHeaders(header);
        mRequestQueue.add(request);
    }


    private void enableHttpsRequest() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {

                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {

                        }

                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[0];
                        }
                    }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }
            });
        } catch (Exception e) {
        }
    }
}