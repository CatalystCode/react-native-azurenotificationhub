package com.azure.reactnative.notificationhub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;
import javax.annotation.Nullable;


public class ReactNativeBackgroundTaskService extends HeadlessJsTaskService {
    private static final String TAG = "ReactNativeAzureNotificationBackgroundTask";
    @Override
    protected @Nullable HeadlessJsTaskConfig getTaskConfig(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null && extras.getString("taskName") != null) {
            Log.i(TAG, "Running background task");
            return new HeadlessJsTaskConfig(
                    extras.getString("taskName"),
                    Arguments.fromBundle(extras),
                    5000,
                    false
            );
        } else {
            Log.w(TAG, "Failed to create background task config");
            return null;
        }
    }

}
