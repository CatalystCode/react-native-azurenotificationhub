package com.azure.reactnative.notificationhub;

import android.content.Intent;
import android.util.Log;
import com.google.firebase.iid.FirebaseInstanceIdService;

public class ReactNativeInstanceIDService extends FirebaseInstanceIdService {

    private static final String TAG = "MyInstanceIDService";

    @Override
    public void onTokenRefresh() {

        Log.i(TAG, "Refreshing GCM Registration Token");

        Intent intent = new Intent(this, ReactNativeRegistrationIntentService.class);
        startService(intent);
    }
};