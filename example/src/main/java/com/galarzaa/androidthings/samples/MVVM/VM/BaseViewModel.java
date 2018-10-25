package com.galarzaa.androidthings.samples.MVVM.VM;

import android.content.Context;

import com.galarzaa.androidthings.samples.MVVM.View.IView;
import com.galarzaa.androidthings.samples.NPNConstants;
import com.galarzaa.androidthings.samples.Network.ApiResponseListener;
import com.galarzaa.androidthings.samples.Network.VolleyRemoteApiClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Le Trong Nhan on 19/06/2018.
 */

public class BaseViewModel<T extends IView> {

    T view;
    protected Context mContext;

    BaseViewModel() {

    }

    BaseViewModel(Context context) {
        mContext = context;
    }

    public void attach(T view, Context context) {
        this.view = view;
        this.mContext = context;
    }

    public void detach() {
        view = null;
    }

    protected void requestGETWithURL(String url, ApiResponseListener<String> listener) {
        VolleyRemoteApiClient.createInstance(mContext);
        Map<String, String> header = new HashMap<>();
        header.put(NPNConstants.apiHeaderKey, NPNConstants.apiHeaderValue);
        VolleyRemoteApiClient.getInstance().get(url, header, listener);
    }

    protected void requestPOSTWithURL(String url, String params, ApiResponseListener<String> listener) {
        VolleyRemoteApiClient.createInstance(mContext);
        Map<String, String> header = new HashMap<>();
        header.put(NPNConstants.apiHeaderKey, NPNConstants.apiHeaderValue);
        header.put("Content-Type", "application/x-www-form-urlencoded");
        VolleyRemoteApiClient.getInstance().post(url, params, header, listener);
    }
}
