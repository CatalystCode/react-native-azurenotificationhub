package com.reactnativeazurenotificationhubsample;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static com.azure.reactnative.notificationhub.ReactNativeNotificationHubUtil.*;
import static com.azure.reactnative.notificationhub.ReactNativeConstants.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.azure.reactnative.notificationhub.ReactNativeUtil;
import com.facebook.react.bridge.ReactApplicationContext;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Unit tests for ReactNativeUtil.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
        LocalBroadcastManager.class,
        RingtoneManager.class,
        Uri.class,
        IntentFactory.class,
        PendingIntent.class,
        Log.class
})
public class ReactNativeUtilTest {

    @Mock
    ReactApplicationContext mReactApplicationContext;

    @Mock
    Bundle mBundle;

    @Before
    public void setUp() {
        // Reset mocks
        reset(mBundle);

        // Prepare mock objects
        PowerMockito.mockStatic(RingtoneManager.class);
        PowerMockito.mockStatic(Uri.class);
        PowerMockito.mockStatic(IntentFactory.class);
        PowerMockito.mockStatic(PendingIntent.class);
        PowerMockito.mockStatic(Log.class);
    }

    @Test
    public void testConvertBundleToJSON() throws  Exception {
        HashSet<String> keys = new HashSet<>(Arrays.asList("Key 1", "Key 2"));
        when(mBundle.keySet()).thenReturn(keys);
        when(mBundle.get("Key 1")).thenReturn("Value 1");
        when(mBundle.get("Key 2")).thenReturn("Value 2");
        String expectedJsonString = "{\"Key 1\":\"Value 1\",\"Key 2\":\"Value 2\"}";

        JSONObject json = ReactNativeUtil.convertBundleToJSON(mBundle);

        Assert.assertEquals(json.toString(), expectedJsonString);
    }

    @Test
    public void testCreateBroadcastIntent() throws Exception {
        final String action = "action";
        final String jsonString = "{\"Key 1\":\"Value 1\",\"Key 2\":\"Value 2\"}";
        final JSONObject json = new JSONObject(jsonString);

        Intent intent = PowerMockito.mock(Intent.class);
        when(IntentFactory.createIntent(action)).thenReturn(intent);

        ReactNativeUtil.createBroadcastIntent(action, json);

        PowerMockito.verifyStatic(IntentFactory.class);
        IntentFactory.createIntent(action);
        verify(intent, times(1)).putExtra(
                "event", DEVICE_NOTIF_EVENT);
        verify(intent, times(1)).putExtra("data", jsonString);
    }

    @Test
    public void testGetMainActivityClass() throws Exception {
        final String className = "com.reactnativeazurenotificationhubsample.MainActivity";

        PackageManager packageManager = PowerMockito.mock(PackageManager.class);
        Intent launchIntent = PowerMockito.mock(Intent.class);
        ComponentName component = PowerMockito.mock(ComponentName.class);
        when(mReactApplicationContext.getPackageName()).thenReturn("com.reactnativeazurenotificationhubsample");
        when(mReactApplicationContext.getPackageManager()).thenReturn(packageManager);
        when(packageManager.getLaunchIntentForPackage(any())).thenReturn(launchIntent);
        when(launchIntent.getComponent()).thenReturn(component);
        when(component.getClassName()).thenReturn(className);

        Class activityClass = ReactNativeUtil.getMainActivityClass(mReactApplicationContext);

        Assert.assertEquals(activityClass.getName(), className);
    }

    @Test
    public void testGetMainActivityClassError() {
        final String className = "com.reactnativeazurenotificationhubsample.InvalidMainActivity";

        PackageManager packageManager = PowerMockito.mock(PackageManager.class);
        Intent launchIntent = PowerMockito.mock(Intent.class);
        ComponentName component = PowerMockito.mock(ComponentName.class);
        when(mReactApplicationContext.getPackageName()).thenReturn("com.reactnativeazurenotificationhubsample");
        when(mReactApplicationContext.getPackageManager()).thenReturn(packageManager);
        when(packageManager.getLaunchIntentForPackage(any())).thenReturn(launchIntent);
        when(launchIntent.getComponent()).thenReturn(component);
        when(component.getClassName()).thenReturn(className);

        Class activityClass = ReactNativeUtil.getMainActivityClass(mReactApplicationContext);

        Assert.assertNull(activityClass);
    }

    @Test
    public void testGetNotificationCompatPriority() {
        Assert.assertEquals(ReactNativeUtil.getNotificationCompatPriority("max"), NotificationCompat.PRIORITY_MAX);
        Assert.assertEquals(ReactNativeUtil.getNotificationCompatPriority("Max"), NotificationCompat.PRIORITY_MAX);
        Assert.assertEquals(ReactNativeUtil.getNotificationCompatPriority("high"), NotificationCompat.PRIORITY_HIGH);
        Assert.assertEquals(ReactNativeUtil.getNotificationCompatPriority("High"), NotificationCompat.PRIORITY_HIGH);
        Assert.assertEquals(ReactNativeUtil.getNotificationCompatPriority("low"), NotificationCompat.PRIORITY_LOW);
        Assert.assertEquals(ReactNativeUtil.getNotificationCompatPriority("Low"), NotificationCompat.PRIORITY_LOW);
        Assert.assertEquals(ReactNativeUtil.getNotificationCompatPriority("min"), NotificationCompat.PRIORITY_MIN);
        Assert.assertEquals(ReactNativeUtil.getNotificationCompatPriority("Min"), NotificationCompat.PRIORITY_MIN);
        Assert.assertEquals(ReactNativeUtil.getNotificationCompatPriority("normal"), NotificationCompat.PRIORITY_DEFAULT);
        Assert.assertEquals(ReactNativeUtil.getNotificationCompatPriority("Normal"), NotificationCompat.PRIORITY_DEFAULT);
        Assert.assertEquals(ReactNativeUtil.getNotificationCompatPriority("default"), NotificationCompat.PRIORITY_DEFAULT);
        Assert.assertEquals(ReactNativeUtil.getNotificationCompatPriority("Default"), NotificationCompat.PRIORITY_DEFAULT);
        Assert.assertEquals(ReactNativeUtil.getNotificationCompatPriority(null), NotificationCompat.PRIORITY_DEFAULT);
    }

    @Test
    public void testGetSmallIconNoIconNoMipmapNoLauncher() {
        final String packageName = "Package Name";
        final int expectedSmallIconResId = android.R.drawable.ic_dialog_info;

        Resources res = PowerMockito.mock(Resources.class);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_SMALL_ICON)).thenReturn(null);
        when(res.getIdentifier(RESOURCE_NAME_NOTIFICATION, RESOURCE_DEF_TYPE_MIPMAP, packageName))
                .thenReturn(0);
        when(res.getIdentifier(RESOURCE_NAME_LAUNCHER, RESOURCE_DEF_TYPE_MIPMAP, packageName))
                .thenReturn(0);

        int smallIconResId = ReactNativeUtil.getSmallIcon(mBundle, res, "Package Name");

        Assert.assertTrue(smallIconResId == expectedSmallIconResId);
        verify(mBundle, times(1)).getString(KEY_REMOTE_NOTIFICATION_SMALL_ICON);
        verify(res, times(1)).getIdentifier(RESOURCE_NAME_NOTIFICATION, RESOURCE_DEF_TYPE_MIPMAP, packageName);
        verify(res, times(1)).getIdentifier(RESOURCE_NAME_LAUNCHER, RESOURCE_DEF_TYPE_MIPMAP, packageName);
    }

    @Test
    public void testGetSmallIconNoIconNoMipmapHasLauncher() {
        final String packageName = "Package Name";
        final int expectedSmallIconResId = 1;

        Resources res = PowerMockito.mock(Resources.class);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_SMALL_ICON)).thenReturn(null);
        when(res.getIdentifier(RESOURCE_NAME_NOTIFICATION, RESOURCE_DEF_TYPE_MIPMAP, packageName))
                .thenReturn(0);
        when(res.getIdentifier(RESOURCE_NAME_LAUNCHER, RESOURCE_DEF_TYPE_MIPMAP, packageName))
                .thenReturn(expectedSmallIconResId);

        int smallIconResId = ReactNativeUtil.getSmallIcon(mBundle, res, "Package Name");

        Assert.assertTrue(smallIconResId == expectedSmallIconResId);
        verify(mBundle, times(1)).getString(KEY_REMOTE_NOTIFICATION_SMALL_ICON);
        verify(res, times(1)).getIdentifier(RESOURCE_NAME_NOTIFICATION, RESOURCE_DEF_TYPE_MIPMAP, packageName);
        verify(res, times(1)).getIdentifier(RESOURCE_NAME_LAUNCHER, RESOURCE_DEF_TYPE_MIPMAP, packageName);
    }

    @Test
    public void testGetSmallIconNoIconHasMipmap() {
        final String packageName = "Package Name";
        final int expectedSmallIconResId = 2;

        Resources res = PowerMockito.mock(Resources.class);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_SMALL_ICON)).thenReturn(null);
        when(res.getIdentifier(RESOURCE_NAME_NOTIFICATION, RESOURCE_DEF_TYPE_MIPMAP, packageName))
                .thenReturn(expectedSmallIconResId);

        int smallIconResId = ReactNativeUtil.getSmallIcon(mBundle, res, "Package Name");

        Assert.assertTrue(smallIconResId == expectedSmallIconResId);
        verify(mBundle, times(1)).getString(KEY_REMOTE_NOTIFICATION_SMALL_ICON);
        verify(res, times(1)).getIdentifier(RESOURCE_NAME_NOTIFICATION, RESOURCE_DEF_TYPE_MIPMAP, packageName);
        verify(res, times(0)).getIdentifier(RESOURCE_NAME_LAUNCHER, RESOURCE_DEF_TYPE_MIPMAP, packageName);
    }

    @Test
    public void testGetSmallIconHasIcon() {
        final String packageName = "Package Name";
        final String smallIcon = "Small Icon";
        final int expectedSmallIconResId = 3;

        Resources res = PowerMockito.mock(Resources.class);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_SMALL_ICON)).thenReturn(smallIcon);
        when(res.getIdentifier(smallIcon, RESOURCE_DEF_TYPE_MIPMAP, packageName))
                .thenReturn(expectedSmallIconResId);

        int smallIconResId = ReactNativeUtil.getSmallIcon(mBundle, res, "Package Name");

        Assert.assertTrue(smallIconResId == expectedSmallIconResId);
        verify(mBundle, times(1)).getString(KEY_REMOTE_NOTIFICATION_SMALL_ICON);
        verify(res, times(0)).getIdentifier(RESOURCE_NAME_NOTIFICATION, RESOURCE_DEF_TYPE_MIPMAP, packageName);
        verify(res, times(0)).getIdentifier(RESOURCE_NAME_LAUNCHER, RESOURCE_DEF_TYPE_MIPMAP, packageName);
    }

    @Test
    public void testGetLargeIconNoIcon() {
        final String packageName = "Package Name";
        final int expectedLargeIconResId = 1;

        Resources res = PowerMockito.mock(Resources.class);
        when(res.getIdentifier(RESOURCE_NAME_LAUNCHER, RESOURCE_DEF_TYPE_MIPMAP, packageName))
                .thenReturn(expectedLargeIconResId);

        int largeIconResId = ReactNativeUtil.getLargeIcon(mBundle, null, res, "Package Name");

        Assert.assertTrue(largeIconResId == expectedLargeIconResId);
        verify(res, times(1)).getIdentifier(RESOURCE_NAME_LAUNCHER, RESOURCE_DEF_TYPE_MIPMAP, packageName);
    }

    @Test
    public void testGetLargeIconHasIcon() {
        final String packageName = "Package Name";
        final String largeIcon = "Large Icon";
        final int expectedLargeIconResId = 1;

        Resources res = PowerMockito.mock(Resources.class);
        when(res.getIdentifier(largeIcon, RESOURCE_DEF_TYPE_MIPMAP, packageName))
                .thenReturn(expectedLargeIconResId);

        int largeIconResId = ReactNativeUtil.getLargeIcon(mBundle, largeIcon, res, "Package Name");

        Assert.assertTrue(largeIconResId == expectedLargeIconResId);
        verify(res, times(1)).getIdentifier(largeIcon, RESOURCE_DEF_TYPE_MIPMAP, packageName);
        verify(res, times(0)).getIdentifier(RESOURCE_NAME_LAUNCHER, RESOURCE_DEF_TYPE_MIPMAP, packageName);
    }

    @Test
    public void testGetSoundUriNull() {
        Uri expectedSoundUri = PowerMockito.mock(Uri.class);
        when(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).thenReturn(expectedSoundUri);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_SOUND_NAME)).thenReturn(null);

        Uri soundUri = ReactNativeUtil.getSoundUri(mReactApplicationContext, mBundle);

        Assert.assertEquals(soundUri, expectedSoundUri);
        verifyStatic(Uri.class, times(0));
        Uri.parse(anyString());
    }

    @Test
    public void testGetSoundUriDefault() {
        final String soundName = "default";

        Uri expectedSoundUri = PowerMockito.mock(Uri.class);
        when(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).thenReturn(expectedSoundUri);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_SOUND_NAME)).thenReturn(soundName);

        Uri soundUri = ReactNativeUtil.getSoundUri(mReactApplicationContext, mBundle);

        Assert.assertEquals(soundUri, expectedSoundUri);
        verifyStatic(Uri.class, times(0));
        Uri.parse(anyString());
    }

    @Test
    public void testGetSoundUriNotRaw() {
        final String packageName = "com.package";
        final String soundName = "sound.wav";
        final String rawSoundName = "sound";
        final int soundResourceID = 1;

        Uri defaultSoundUri = PowerMockito.mock(Uri.class);
        when(mReactApplicationContext.getPackageName()).thenReturn(packageName);
        when(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).thenReturn(defaultSoundUri);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_SOUND_NAME)).thenReturn(soundName);
        Resources res = PowerMockito.mock(Resources.class);
        when(res.getIdentifier(soundName, RESOURCE_DEF_TYPE_RAW, packageName)).thenReturn(0);
        when(res.getIdentifier(rawSoundName, RESOURCE_DEF_TYPE_RAW, packageName)).thenReturn(soundResourceID);
        when(mReactApplicationContext.getResources()).thenReturn(res);

        ReactNativeUtil.getSoundUri(mReactApplicationContext, mBundle);

        verifyStatic(Uri.class);
        Uri.parse("android.resource://" + packageName + "/" + soundResourceID);
    }

    @Test
    public void testGetSoundUriRaw() {
        final String packageName = "com.package";
        final String soundName = "sound";
        final int rawSoundResourceID = 2;

        Uri defaultSoundUri = PowerMockito.mock(Uri.class);
        when(mReactApplicationContext.getPackageName()).thenReturn(packageName);
        when(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).thenReturn(defaultSoundUri);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_SOUND_NAME)).thenReturn(soundName);
        Resources res = PowerMockito.mock(Resources.class);
        when(res.getIdentifier(soundName, RESOURCE_DEF_TYPE_RAW, packageName)).thenReturn(rawSoundResourceID);
        when(mReactApplicationContext.getResources()).thenReturn(res);

        ReactNativeUtil.getSoundUri(mReactApplicationContext, mBundle);

        verifyStatic(Uri.class);
        Uri.parse("android.resource://" + packageName + "/" + rawSoundResourceID);
    }

    @Test
    public void testCreateNotificationIntent() {
        Intent intent = PowerMockito.mock(Intent.class);
        when(IntentFactory.createIntent(eq(mReactApplicationContext), any())).thenReturn(intent);

        ReactNativeUtil.createNotificationIntent(mReactApplicationContext, mBundle, null);

        PowerMockito.verifyStatic(IntentFactory.class);
        IntentFactory.createIntent(mReactApplicationContext, null);
        verify(intent, times(1)).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        verify(intent, times(1)).putExtra(KEY_INTENT_NOTIFICATION, mBundle);
        verify(mBundle, times(1)).putBoolean(
                KEY_REMOTE_NOTIFICATION_FOREGROUND, true);
        verify(mBundle, times(1)).putBoolean(
                KEY_REMOTE_NOTIFICATION_USER_INTERACTION, false);
        verify(mBundle, times(1)).putBoolean(
                KEY_REMOTE_NOTIFICATION_COLDSTART, false);
    }

    @Test
    public void testProcessNotificationActionsNoActions() {
        final int notificationID = 1;

        NotificationCompat.Builder notificationBuilder = PowerMockito.mock(NotificationCompat.Builder.class);
        Intent intent = PowerMockito.mock(Intent.class);
        when(IntentFactory.createIntent()).thenReturn(intent);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_ACTIONS)).thenReturn(null);

        ReactNativeUtil.processNotificationActions(
                mReactApplicationContext, mBundle, notificationBuilder, notificationID);

        PowerMockito.verifyStatic(IntentFactory.class, times(0));
        IntentFactory.createIntent();
    }

    @Test
    public void testProcessNotificationActionsParseError() {
        final int notificationID = 1;
        final String jsonString = "Invalid JSON format";

        NotificationCompat.Builder notificationBuilder = PowerMockito.mock(NotificationCompat.Builder.class);
        Intent intent = PowerMockito.mock(Intent.class);
        when(IntentFactory.createIntent()).thenReturn(intent);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_ACTIONS)).thenReturn(jsonString);

        ReactNativeUtil.processNotificationActions(
                mReactApplicationContext, mBundle, notificationBuilder, notificationID);

        PowerMockito.verifyStatic(IntentFactory.class, times(0));
        IntentFactory.createIntent();
        PowerMockito.verifyStatic(Log.class);
        Log.e(eq(ReactNativeUtil.TAG), eq(ERROR_COVERT_ACTIONS), any(JSONException.class));
    }

    @Test
    public void testProcessNotificationActionsGetActionError() {
        final int notificationID = 1;
        final String jsonString = "[1]";

        NotificationCompat.Builder notificationBuilder = PowerMockito.mock(NotificationCompat.Builder.class);
        Intent intent = PowerMockito.mock(Intent.class);
        when(IntentFactory.createIntent()).thenReturn(intent);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_ACTIONS)).thenReturn(jsonString);

        ReactNativeUtil.processNotificationActions(
                mReactApplicationContext, mBundle, notificationBuilder, notificationID);

        PowerMockito.verifyStatic(IntentFactory.class, times(0));
        IntentFactory.createIntent();
        PowerMockito.verifyStatic(Log.class);
        Log.e(eq(ReactNativeUtil.TAG), eq(ERROR_GET_ACTIONS_ARRAY), any(JSONException.class));
    }

    @Test
    public void testProcessNotificationActions() {
        final int notificationID = 1;
        final String jsonString = "[\"Action\",\"Action\"]";

        NotificationCompat.Builder notificationBuilder = PowerMockito.mock(NotificationCompat.Builder.class);
        Intent intent = PowerMockito.mock(Intent.class);
        when(IntentFactory.createIntent()).thenReturn(intent);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_ACTIONS)).thenReturn(jsonString);

        ReactNativeUtil.processNotificationActions(
                mReactApplicationContext, mBundle, notificationBuilder, notificationID);

        PowerMockito.verifyStatic(IntentFactory.class, times(2));
        IntentFactory.createIntent();
        PowerMockito.verifyStatic(PendingIntent.class, times(2));
        PendingIntent.getBroadcast(eq(mReactApplicationContext), eq(notificationID), any(),
                eq(PendingIntent.FLAG_UPDATE_CURRENT));
        verify(mBundle, times(2)).putString(
                KEY_REMOTE_NOTIFICATION_ACTION, "Action");
        verify(notificationBuilder, times(2)).addAction(
                eq(0), eq("Action"), any());
    }
}
