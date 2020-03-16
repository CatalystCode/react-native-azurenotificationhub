package com.reactnativeazurenotificationhubsample;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
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
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static com.azure.reactnative.notificationhub.ReactNativeUtil.*;

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
        Arguments.class,
        BitmapFactory.class,
        UrlWrapper.class,
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
        PowerMockito.mockStatic(Arguments.class);
        PowerMockito.mockStatic(BitmapFactory.class);
        PowerMockito.mockStatic(UrlWrapper.class);
        PowerMockito.mockStatic(Log.class);
    }

    @Test
    public void testConvertBundleToJSON() throws Exception {
        HashSet<String> keys = new HashSet<>(Arrays.asList("Key 1", "Key 2"));
        when(mBundle.keySet()).thenReturn(keys);
        when(mBundle.get("Key 1")).thenReturn("Value 1");
        when(mBundle.get("Key 2")).thenReturn("Value 2");
        String expectedJsonString = "{\"Key 1\":\"Value 1\",\"Key 2\":\"Value 2\"}";

        JSONObject json = convertBundleToJSON(mBundle);

        Assert.assertEquals(json.toString(), expectedJsonString);
    }

    @Test
    public void testConvertBundleToMapNullArgument() {
        WritableMap expectedMap = PowerMockito.mock(WritableMap.class);
        when(Arguments.createMap()).thenReturn(expectedMap);

        WritableMap map = convertBundleToMap(null);

        Assert.assertEquals(map, expectedMap);
        verify(expectedMap, times(0)).putNull(anyString());
        verify(expectedMap, times(0)).putMap(anyString(), any());
        verify(expectedMap, times(0)).putString(anyString(), anyString());
        verify(expectedMap, times(0)).putDouble(anyString(), anyDouble());
        verify(expectedMap, times(0)).putDouble(anyString(), anyFloat());
        verify(expectedMap, times(0)).putInt(anyString(), anyInt());
        verify(expectedMap, times(0)).putBoolean(anyString(), anyBoolean());
        verify(expectedMap, times(0)).putNull(anyString());
    }

    @Test
    public void testConvertBundleToMap() {
        final String key = "Key";
        final HashSet<String> keys = new HashSet<>(Arrays.asList(key));

        WritableMap expectedMap = PowerMockito.mock(WritableMap.class);
        when(Arguments.createMap()).thenReturn(expectedMap);

        when(mBundle.keySet()).thenReturn(keys);
        when(mBundle.get(key)).thenReturn(null);
        convertBundleToMap(mBundle);
        verify(expectedMap, times(1)).putNull(key);

        reset(expectedMap);
        when(mBundle.keySet()).thenReturn(keys);
        when(mBundle.get(key)).thenReturn(PowerMockito.mock(Bundle.class));
        convertBundleToMap(mBundle);
        verify(expectedMap, times(1)).putMap(eq(key), any(WritableMap.class));

        reset(expectedMap);
        when(mBundle.keySet()).thenReturn(keys);
        final String strValue = "Value";
        when(mBundle.get(key)).thenReturn(strValue);
        convertBundleToMap(mBundle);
        verify(expectedMap, times(1)).putString(key, strValue);

        reset(expectedMap);
        when(mBundle.keySet()).thenReturn(keys);
        final Float floatValue = new Float(1);
        when(mBundle.get(key)).thenReturn(floatValue);
        convertBundleToMap(mBundle);
        verify(expectedMap, times(1)).putDouble(key, floatValue.doubleValue());

        reset(expectedMap);
        when(mBundle.keySet()).thenReturn(keys);
        final Double doubleValue = new Double(1);
        when(mBundle.get(key)).thenReturn(doubleValue);
        convertBundleToMap(mBundle);
        verify(expectedMap, times(1)).putDouble(key, doubleValue);

        reset(expectedMap);
        when(mBundle.keySet()).thenReturn(keys);
        final Integer integerValue = new Integer(1);
        when(mBundle.get(key)).thenReturn(integerValue);
        convertBundleToMap(mBundle);
        verify(expectedMap, times(1)).putInt(key, integerValue);

        reset(expectedMap);
        when(mBundle.keySet()).thenReturn(keys);
        final Long longValue = new Long(1);
        when(mBundle.get(key)).thenReturn(longValue);
        convertBundleToMap(mBundle);
        verify(expectedMap, times(1)).putInt(key, longValue.intValue());

        reset(expectedMap);
        when(mBundle.keySet()).thenReturn(keys);
        final Short shortValue = new Short("1");
        when(mBundle.get(key)).thenReturn(shortValue);
        convertBundleToMap(mBundle);
        verify(expectedMap, times(1)).putInt(key, shortValue.intValue());

        reset(expectedMap);
        when(mBundle.keySet()).thenReturn(keys);
        final Byte byteValue = new Byte("1");
        when(mBundle.get(key)).thenReturn(byteValue);
        convertBundleToMap(mBundle);
        verify(expectedMap, times(1)).putInt(key, byteValue.intValue());

        reset(expectedMap);
        when(mBundle.keySet()).thenReturn(keys);
        final Boolean booleanValue = new Boolean(true);
        when(mBundle.get(key)).thenReturn(booleanValue);
        convertBundleToMap(mBundle);
        verify(expectedMap, times(1)).putBoolean(key, booleanValue);

        reset(expectedMap);
        when(mBundle.keySet()).thenReturn(keys);
        when(mBundle.get(key)).thenReturn(new HashMap<>());
        convertBundleToMap(mBundle);
        verify(expectedMap, times(1)).putNull(key);
    }

    @Test
    public void testCreateBroadcastIntent() throws Exception {
        final String action = "action";

        Intent intent = PowerMockito.mock(Intent.class);
        when(IntentFactory.createIntent(action)).thenReturn(intent);

        createBroadcastIntent(action, mBundle);

        PowerMockito.verifyStatic(IntentFactory.class);
        IntentFactory.createIntent(action);
        verify(intent, times(1)).putExtra(
                KEY_INTENT_EVENT_NAME, EVENT_REMOTE_NOTIFICATION_RECEIVED);
        verify(intent, times(1)).putExtra(
                KEY_INTENT_EVENT_TYPE, INTENT_EVENT_TYPE_BUNDLE);
        verify(intent, times(1)).putExtras(mBundle);
    }

    @Test
    public void testEmitEventNoCatalystInstance() {
        when(mReactApplicationContext.hasActiveCatalystInstance()).thenReturn(false);

        emitEvent(mReactApplicationContext, "event", "data");

        verify(mReactApplicationContext, times(1)).hasActiveCatalystInstance();
        verify(mReactApplicationContext, times(0)).getJSModule(any());
    }

    @Test
    public void testEmitEvent() {
        when(mReactApplicationContext.hasActiveCatalystInstance()).thenReturn(true);
        DeviceEventManagerModule.RCTDeviceEventEmitter emitter = PowerMockito.mock(
                DeviceEventManagerModule.RCTDeviceEventEmitter.class);
        when(mReactApplicationContext.getJSModule(any())).thenReturn(emitter);

        emitEvent(mReactApplicationContext, "event", "data");

        verify(mReactApplicationContext, times(1)).hasActiveCatalystInstance();
        verify(mReactApplicationContext, times(1)).getJSModule(any());
        verify(emitter, times(1)).emit("event", "data");
    }

    @Test
    public void testEmitIntent() {
        when(mReactApplicationContext.hasActiveCatalystInstance()).thenReturn(true);
        DeviceEventManagerModule.RCTDeviceEventEmitter emitter = PowerMockito.mock(
                DeviceEventManagerModule.RCTDeviceEventEmitter.class);
        when(mReactApplicationContext.getJSModule(any())).thenReturn(emitter);
        when(Arguments.createMap()).thenReturn(PowerMockito.mock(WritableMap.class));

        Intent intent = PowerMockito.mock(Intent.class);
        when(intent.getStringExtra(KEY_INTENT_EVENT_NAME)).thenReturn("event");
        when(intent.getStringExtra(KEY_INTENT_EVENT_TYPE)).thenReturn(INTENT_EVENT_TYPE_BUNDLE);
        when(intent.getExtras()).thenReturn(mBundle);
        emitIntent(mReactApplicationContext, intent);
        verify(mReactApplicationContext, times(1)).hasActiveCatalystInstance();
        verify(mReactApplicationContext, times(1)).getJSModule(any());
        verify(emitter, times(1)).emit(eq("event"), any(WritableMap.class));

        reset(intent);
        when(intent.getStringExtra(KEY_INTENT_EVENT_NAME)).thenReturn("event");
        when(intent.getStringExtra(KEY_INTENT_EVENT_TYPE)).thenReturn(INTENT_EVENT_TYPE_STRING);
        when(intent.getStringExtra(KEY_INTENT_EVENT_STRING_DATA)).thenReturn("data");
        emitIntent(mReactApplicationContext, intent);
        verify(mReactApplicationContext, times(2)).hasActiveCatalystInstance();
        verify(mReactApplicationContext, times(2)).getJSModule(any());
        verify(emitter, times(1)).emit("event", "data");
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

        Class activityClass = getMainActivityClass(mReactApplicationContext);

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

        Class activityClass = getMainActivityClass(mReactApplicationContext);

        Assert.assertNull(activityClass);
    }

    @Test
    public void testGetNotificationCompatPriority() {
        Assert.assertEquals(getNotificationCompatPriority("max"), NotificationCompat.PRIORITY_MAX);
        Assert.assertEquals(getNotificationCompatPriority("Max"), NotificationCompat.PRIORITY_MAX);
        Assert.assertEquals(getNotificationCompatPriority("high"), NotificationCompat.PRIORITY_HIGH);
        Assert.assertEquals(getNotificationCompatPriority("High"), NotificationCompat.PRIORITY_HIGH);
        Assert.assertEquals(getNotificationCompatPriority("low"), NotificationCompat.PRIORITY_LOW);
        Assert.assertEquals(getNotificationCompatPriority("Low"), NotificationCompat.PRIORITY_LOW);
        Assert.assertEquals(getNotificationCompatPriority("min"), NotificationCompat.PRIORITY_MIN);
        Assert.assertEquals(getNotificationCompatPriority("Min"), NotificationCompat.PRIORITY_MIN);
        Assert.assertEquals(getNotificationCompatPriority("normal"), NotificationCompat.PRIORITY_DEFAULT);
        Assert.assertEquals(getNotificationCompatPriority("Normal"), NotificationCompat.PRIORITY_DEFAULT);
        Assert.assertEquals(getNotificationCompatPriority("default"), NotificationCompat.PRIORITY_DEFAULT);
        Assert.assertEquals(getNotificationCompatPriority("Default"), NotificationCompat.PRIORITY_DEFAULT);
        Assert.assertEquals(getNotificationCompatPriority(null), NotificationCompat.PRIORITY_DEFAULT);
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

        int smallIconResId = getSmallIcon(mBundle, res, "Package Name");

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

        int smallIconResId = getSmallIcon(mBundle, res, "Package Name");

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

        int smallIconResId = getSmallIcon(mBundle, res, "Package Name");

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

        int smallIconResId = getSmallIcon(mBundle, res, "Package Name");

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

        int largeIconResId = getLargeIcon(mBundle, null, res, "Package Name");

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

        int largeIconResId = getLargeIcon(mBundle, largeIcon, res, "Package Name");

        Assert.assertTrue(largeIconResId == expectedLargeIconResId);
        verify(res, times(1)).getIdentifier(largeIcon, RESOURCE_DEF_TYPE_MIPMAP, packageName);
        verify(res, times(0)).getIdentifier(RESOURCE_NAME_LAUNCHER, RESOURCE_DEF_TYPE_MIPMAP, packageName);
    }

    @Test
    public void testGetSoundUriNull() {
        Uri expectedSoundUri = PowerMockito.mock(Uri.class);
        when(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).thenReturn(expectedSoundUri);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_SOUND_NAME)).thenReturn(null);

        Uri soundUri = getSoundUri(mReactApplicationContext, mBundle);

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

        Uri soundUri = getSoundUri(mReactApplicationContext, mBundle);

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

        getSoundUri(mReactApplicationContext, mBundle);

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

        getSoundUri(mReactApplicationContext, mBundle);

        verifyStatic(Uri.class);
        Uri.parse("android.resource://" + packageName + "/" + rawSoundResourceID);
    }

    @Test
    public void testCreateNotificationIntent() {
        Intent intent = PowerMockito.mock(Intent.class);
        when(IntentFactory.createIntent(eq(mReactApplicationContext), any())).thenReturn(intent);

        createNotificationIntent(mReactApplicationContext, mBundle, null);

        PowerMockito.verifyStatic(IntentFactory.class);
        IntentFactory.createIntent(mReactApplicationContext, null);
        verify(intent, times(1)).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        verify(intent, times(1)).putExtra(KEY_NOTIFICATION_PAYLOAD_TYPE, mBundle);
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

        processNotificationActions(
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

        processNotificationActions(
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

        processNotificationActions(
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

        processNotificationActions(
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

    @Test
    public void testGetBundleFromIntent() {
        Intent intent = PowerMockito.mock(Intent.class);
        when(intent.hasExtra(KEY_NOTIFICATION_PAYLOAD_TYPE)).thenReturn(true);
        getBundleFromIntent(intent);
        verify(intent, times(1)).getBundleExtra(KEY_NOTIFICATION_PAYLOAD_TYPE);

        reset(intent);
        when(intent.hasExtra(KEY_NOTIFICATION_PAYLOAD_TYPE)).thenReturn(false);
        when(intent.hasExtra(KEY_REMOTE_NOTIFICATION_ID)).thenReturn(true);
        getBundleFromIntent(intent);
        verify(intent, times(1)).getExtras();

        reset(intent);
        when(intent.hasExtra(KEY_NOTIFICATION_PAYLOAD_TYPE)).thenReturn(false);
        when(intent.hasExtra(KEY_REMOTE_NOTIFICATION_ID)).thenReturn(false);
        getBundleFromIntent(intent);
        verify(intent, times(0)).getBundleExtra(KEY_NOTIFICATION_PAYLOAD_TYPE);
        verify(intent, times(0)).getExtras();
    }

    @Test
    public void testRemoveNotificationFromIntent() {
        Intent intent = PowerMockito.mock(Intent.class);
        when(intent.hasExtra(KEY_NOTIFICATION_PAYLOAD_TYPE)).thenReturn(true);
        removeNotificationFromIntent(intent);
        verify(intent, times(1)).removeExtra(KEY_NOTIFICATION_PAYLOAD_TYPE);
        verify(intent, times(0)).removeExtra(KEY_REMOTE_NOTIFICATION_ID);

        reset(intent);
        when(intent.hasExtra(KEY_REMOTE_NOTIFICATION_ID)).thenReturn(true);
        removeNotificationFromIntent(intent);
        verify(intent, times(0)).removeExtra(KEY_NOTIFICATION_PAYLOAD_TYPE);
        verify(intent, times(1)).removeExtra(KEY_REMOTE_NOTIFICATION_ID);
    }

    @Test
    public void testFetchImage() throws Exception {
        final String urlString = "http://somedomain.com/someimage.png";

        HttpURLConnection connection = PowerMockito.mock(HttpURLConnection.class);
        when(UrlWrapper.openConnection(urlString)).thenReturn(connection);
        InputStream input = PowerMockito.mock(InputStream.class);
        when(connection.getInputStream()).thenReturn(input);
        Bitmap expectedBitmap = PowerMockito.mock(Bitmap.class);
        when(BitmapFactory.decodeStream(input)).thenReturn(expectedBitmap);

        Bitmap bitmap = ReactNativeUtil.fetchImage(urlString);

        Assert.assertEquals(bitmap, expectedBitmap);
    }
}
