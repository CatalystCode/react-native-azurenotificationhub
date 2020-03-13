package com.reactnativeazurenotificationhubsample;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import static com.azure.reactnative.notificationhub.NotificationHubUtil.*;
import static com.azure.reactnative.notificationhub.Constants.*;
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

import com.azure.reactnative.notificationhub.NotificationHubUtil;
import com.facebook.react.bridge.ReactApplicationContext;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Unit tests for ReactNativeNotificationHubModule.
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
public class NotificationHubUtilTest {

    @Mock
    ReactApplicationContext mReactApplicationContext;

    @Mock
    SharedPreferences mSharedPreferences;

    @Mock
    SharedPreferences.Editor mEditor;

    @Mock
    Bundle mBundle;

    NotificationHubUtil mHubUtil;

    @Before
    public void setUp() {
        // Reset mocks
        reset(mBundle);
        reset(mSharedPreferences);
        reset(mEditor);

        // Prepare mock objects
        PowerMockito.mockStatic(RingtoneManager.class);
        PowerMockito.mockStatic(Uri.class);
        PowerMockito.mockStatic(IntentFactory.class);
        PowerMockito.mockStatic(PendingIntent.class);
        PowerMockito.mockStatic(Log.class);
        when(mReactApplicationContext.getSharedPreferences(
                SHARED_PREFS_NAME, Context.MODE_PRIVATE)).thenReturn(mSharedPreferences);
        when(mSharedPreferences.edit()).thenReturn(mEditor);

        mHubUtil = NotificationHubUtil.getInstance();
    }

    @Test
    public void testGetConnectionString() {
        mHubUtil.getConnectionString(mReactApplicationContext);

        verify(mSharedPreferences, times(1)).getString(
                KEY_FOR_PREFS_CONNECTIONSTRING, null);
    }

    @Test
    public void testSetConnectionString() {
        final String connectionString = "Connection String";

        mHubUtil.setConnectionString(mReactApplicationContext, connectionString);

        verify(mEditor, times(1)).putString(
                KEY_FOR_PREFS_CONNECTIONSTRING, connectionString);
        verify(mEditor, times(1)).apply();
    }

    @Test
    public void testGetHubName() {
        mHubUtil.getHubName(mReactApplicationContext);

        verify(mSharedPreferences, times(1)).getString(
                KEY_FOR_PREFS_HUBNAME, null);
    }

    @Test
    public void testSetHubName() {
        final String hubName = "Hub Name";

        mHubUtil.setHubName(mReactApplicationContext, hubName);

        verify(mEditor, times(1)).putString(
                KEY_FOR_PREFS_HUBNAME, hubName);
        verify(mEditor, times(1)).apply();
    }

    @Test
    public void testGetRegistrationID() {
        mHubUtil.getRegistrationID(mReactApplicationContext);

        verify(mSharedPreferences, times(1)).getString(
                KEY_FOR_PREFS_REGISTRATIONID, null);
    }

    @Test
    public void testSetRegistrationID() {
        final String registrationID = "RegistrationID";

        mHubUtil.setRegistrationID(mReactApplicationContext, registrationID);

        verify(mEditor, times(1)).putString(
                KEY_FOR_PREFS_REGISTRATIONID, registrationID);
        verify(mEditor, times(1)).apply();
    }

    @Test
    public void testGetFCMToken() {
        mHubUtil.getFCMToken(mReactApplicationContext);

        verify(mSharedPreferences, times(1)).getString(
                KEY_FOR_PREFS_FCMTOKEN, null);
    }

    @Test
    public void testSetFCMToken() {
        final String token = "FCMToken";

        mHubUtil.setFCMToken(mReactApplicationContext, token);

        verify(mEditor, times(1)).putString(
                KEY_FOR_PREFS_FCMTOKEN, token);
        verify(mEditor, times(1)).apply();
    }

    @Test
    public void testGetTagsNull() {
        when(mSharedPreferences.getStringSet(
                KEY_FOR_PREFS_TAGS, null)).thenReturn(null);

        String[] tags = mHubUtil.getTags(mReactApplicationContext);

        verify(mSharedPreferences, times(1)).getStringSet(
                KEY_FOR_PREFS_TAGS, null);
        Assert.assertNull(tags);
    }

    @Test
    public void testGetTags() {
        mHubUtil.getTags(mReactApplicationContext);

        verify(mSharedPreferences, times(1)).getStringSet(
                KEY_FOR_PREFS_TAGS, null);
    }

    @Test
    public void testSetTags() {
        final String[] tags = new String[]{"Tag1", "Tag2"};

        mHubUtil.setTags(mReactApplicationContext, tags);

        verify(mEditor, times(1)).putStringSet(
                KEY_FOR_PREFS_TAGS, new HashSet<>(Arrays.asList(tags)));
        verify(mEditor, times(1)).apply();
    }

    @Test
    public void testSetTagsNull() {
        mHubUtil.setTags(mReactApplicationContext, null);

        verify(mEditor, times(1)).putStringSet(
                KEY_FOR_PREFS_TAGS, null);
        verify(mEditor, times(1)).apply();
    }

    @Test
    public void testGetSenderID() {
        mHubUtil.getSenderID(mReactApplicationContext);

        verify(mSharedPreferences, times(1)).getString(
                KEY_FOR_PREFS_SENDERID, null);
    }

    @Test
    public void testSetSenderID() {
        final String tags = "Tags";

        mHubUtil.setSenderID(mReactApplicationContext, tags);

        verify(mEditor, times(1)).putString(
                KEY_FOR_PREFS_SENDERID, tags);
        verify(mEditor, times(1)).apply();
    }

    @Test
    public void testGetChannelImportance() {
        mHubUtil.getChannelImportance(mReactApplicationContext);

        verify(mSharedPreferences, times(1)).getInt(
                KEY_FOR_PREFS_CHANNELIMPORTANCE, 0);
    }

    @Test
    public void testSetChannelImportance() {
        final int channelImportance = 1;

        mHubUtil.setChannelImportance(mReactApplicationContext, channelImportance);

        verify(mEditor, times(1)).putInt(
                KEY_FOR_PREFS_CHANNELIMPORTANCE, channelImportance);
        verify(mEditor, times(1)).apply();
    }

    @Test
    public void testHasChannelImportance() {
        mHubUtil.hasChannelImportance(mReactApplicationContext);

        verify(mSharedPreferences, times(1)).contains(KEY_FOR_PREFS_CHANNELIMPORTANCE);
    }

    @Test
    public void testGetChannelShowBadge() {
        mHubUtil.getChannelShowBadge(mReactApplicationContext);

        verify(mSharedPreferences, times(1)).getBoolean(
                KEY_FOR_PREFS_CHANNELSHOWBADGE, false);
    }

    @Test
    public void testSetChannelShowBadge() {
        final boolean channelShowBadge = true;

        mHubUtil.setChannelShowBadge(mReactApplicationContext, channelShowBadge);

        verify(mEditor, times(1)).putBoolean(
                KEY_FOR_PREFS_CHANNELSHOWBADGE, channelShowBadge);
        verify(mEditor, times(1)).apply();
    }

    @Test
    public void testHasChannelShowBadge() {
        mHubUtil.hasChannelShowBadge(mReactApplicationContext);

        verify(mSharedPreferences, times(1)).contains(KEY_FOR_PREFS_CHANNELSHOWBADGE);
    }

    @Test
    public void testGetChannelEnableLights() {
        mHubUtil.getChannelEnableLights(mReactApplicationContext);

        verify(mSharedPreferences, times(1)).getBoolean(
                KEY_FOR_PREFS_CHANNELENABLELIGHTS, false);
    }

    @Test
    public void testSetChannelEnableLights() {
        final boolean channelEnableLights = true;

        mHubUtil.setChannelEnableLights(mReactApplicationContext, channelEnableLights);

        verify(mEditor, times(1)).putBoolean(
                KEY_FOR_PREFS_CHANNELENABLELIGHTS, channelEnableLights);
        verify(mEditor, times(1)).apply();
    }

    @Test
    public void testHasChannelEnableVibration() {
        mHubUtil.hasChannelEnableVibration(mReactApplicationContext);

        verify(mSharedPreferences, times(1)).contains(KEY_FOR_PREFS_CHANNELENABLEVIBRATION);
    }

    @Test
    public void testGetChannelEnableVibration() {
        mHubUtil.getChannelEnableVibration(mReactApplicationContext);

        verify(mSharedPreferences, times(1)).getBoolean(
                KEY_FOR_PREFS_CHANNELENABLEVIBRATION, false);
    }

    @Test
    public void testSetChannelEnableVibration() {
        final boolean channelEnableVibration = true;

        mHubUtil.setChannelEnableVibration(mReactApplicationContext, channelEnableVibration);

        verify(mEditor, times(1)).putBoolean(
                KEY_FOR_PREFS_CHANNELENABLEVIBRATION, channelEnableVibration);
        verify(mEditor, times(1)).apply();
    }

    @Test
    public void testHasChannelEnableLights() {
        mHubUtil.hasChannelEnableLights(mReactApplicationContext);

        verify(mSharedPreferences, times(1)).contains(KEY_FOR_PREFS_CHANNELENABLELIGHTS);
    }

    @Test
    public void testConvertBundleToJSON() throws  Exception {
        HashSet<String> keys = new HashSet<>(Arrays.asList("Key 1", "Key 2"));
        when(mBundle.keySet()).thenReturn(keys);
        when(mBundle.get("Key 1")).thenReturn("Value 1");
        when(mBundle.get("Key 2")).thenReturn("Value 2");
        String expectedJsonString = "{\"Key 1\":\"Value 1\",\"Key 2\":\"Value 2\"}";

        JSONObject json = mHubUtil.convertBundleToJSON(mBundle);

        Assert.assertEquals(json.toString(), expectedJsonString);
    }

    @Test
    public void testCreateBroadcastIntent() throws Exception {
        final String action = "action";
        final String jsonString = "{\"Key 1\":\"Value 1\",\"Key 2\":\"Value 2\"}";
        final JSONObject json = new JSONObject(jsonString);

        Intent intent = PowerMockito.mock(Intent.class);
        when(IntentFactory.createIntent(action)).thenReturn(intent);

        mHubUtil.createBroadcastIntent(action, json);

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

        Class activityClass = mHubUtil.getMainActivityClass(mReactApplicationContext);

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

        Class activityClass = mHubUtil.getMainActivityClass(mReactApplicationContext);

        Assert.assertNull(activityClass);
    }

    @Test
    public void testGetNotificationCompatPriority() {
        Assert.assertEquals(mHubUtil.getNotificationCompatPriority("max"), NotificationCompat.PRIORITY_MAX);
        Assert.assertEquals(mHubUtil.getNotificationCompatPriority("Max"), NotificationCompat.PRIORITY_MAX);
        Assert.assertEquals(mHubUtil.getNotificationCompatPriority("high"), NotificationCompat.PRIORITY_HIGH);
        Assert.assertEquals(mHubUtil.getNotificationCompatPriority("High"), NotificationCompat.PRIORITY_HIGH);
        Assert.assertEquals(mHubUtil.getNotificationCompatPriority("low"), NotificationCompat.PRIORITY_LOW);
        Assert.assertEquals(mHubUtil.getNotificationCompatPriority("Low"), NotificationCompat.PRIORITY_LOW);
        Assert.assertEquals(mHubUtil.getNotificationCompatPriority("min"), NotificationCompat.PRIORITY_MIN);
        Assert.assertEquals(mHubUtil.getNotificationCompatPriority("Min"), NotificationCompat.PRIORITY_MIN);
        Assert.assertEquals(mHubUtil.getNotificationCompatPriority("normal"), NotificationCompat.PRIORITY_DEFAULT);
        Assert.assertEquals(mHubUtil.getNotificationCompatPriority("Normal"), NotificationCompat.PRIORITY_DEFAULT);
        Assert.assertEquals(mHubUtil.getNotificationCompatPriority("default"), NotificationCompat.PRIORITY_DEFAULT);
        Assert.assertEquals(mHubUtil.getNotificationCompatPriority("Default"), NotificationCompat.PRIORITY_DEFAULT);
        Assert.assertEquals(mHubUtil.getNotificationCompatPriority(null), NotificationCompat.PRIORITY_DEFAULT);
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

        int smallIconResId = mHubUtil.getSmallIcon(mBundle, res, "Package Name");

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

        int smallIconResId = mHubUtil.getSmallIcon(mBundle, res, "Package Name");

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

        int smallIconResId = mHubUtil.getSmallIcon(mBundle, res, "Package Name");

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

        int smallIconResId = mHubUtil.getSmallIcon(mBundle, res, "Package Name");

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

        int largeIconResId = mHubUtil.getLargeIcon(mBundle, null, res, "Package Name");

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

        int largeIconResId = mHubUtil.getLargeIcon(mBundle, largeIcon, res, "Package Name");

        Assert.assertTrue(largeIconResId == expectedLargeIconResId);
        verify(res, times(1)).getIdentifier(largeIcon, RESOURCE_DEF_TYPE_MIPMAP, packageName);
        verify(res, times(0)).getIdentifier(RESOURCE_NAME_LAUNCHER, RESOURCE_DEF_TYPE_MIPMAP, packageName);
    }

    @Test
    public void testGetSoundUriNull() {
        Uri expectedSoundUri = PowerMockito.mock(Uri.class);
        when(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).thenReturn(expectedSoundUri);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_SOUND_NAME)).thenReturn(null);

        Uri soundUri = mHubUtil.getSoundUri(mReactApplicationContext, mBundle);

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

        Uri soundUri = mHubUtil.getSoundUri(mReactApplicationContext, mBundle);

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

        mHubUtil.getSoundUri(mReactApplicationContext, mBundle);

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

        mHubUtil.getSoundUri(mReactApplicationContext, mBundle);

        verifyStatic(Uri.class);
        Uri.parse("android.resource://" + packageName + "/" + rawSoundResourceID);
    }

    @Test
    public void testCreateNotificationIntent() {
        Intent intent = PowerMockito.mock(Intent.class);
        when(IntentFactory.createIntent(eq(mReactApplicationContext), any())).thenReturn(intent);

        mHubUtil.createNotificationIntent(mReactApplicationContext, mBundle, null);

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

        mHubUtil.processNotificationActions(
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

        mHubUtil.processNotificationActions(
                mReactApplicationContext, mBundle, notificationBuilder, notificationID);

        PowerMockito.verifyStatic(IntentFactory.class, times(0));
        IntentFactory.createIntent();
        PowerMockito.verifyStatic(Log.class);
        Log.e(eq(TAG), eq(ERROR_COVERT_ACTIONS), any(JSONException.class));
    }

    @Test
    public void testProcessNotificationActionsGetActionError() {
        final int notificationID = 1;
        final String jsonString = "[1]";

        NotificationCompat.Builder notificationBuilder = PowerMockito.mock(NotificationCompat.Builder.class);
        Intent intent = PowerMockito.mock(Intent.class);
        when(IntentFactory.createIntent()).thenReturn(intent);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_ACTIONS)).thenReturn(jsonString);

        mHubUtil.processNotificationActions(
                mReactApplicationContext, mBundle, notificationBuilder, notificationID);

        PowerMockito.verifyStatic(IntentFactory.class, times(0));
        IntentFactory.createIntent();
        PowerMockito.verifyStatic(Log.class);
        Log.e(eq(TAG), eq(ERROR_GET_ACTIONS_ARRAY), any(JSONException.class));
    }

    @Test
    public void testProcessNotificationActions() {
        final int notificationID = 1;
        final String jsonString = "[\"Action\",\"Action\"]";

        NotificationCompat.Builder notificationBuilder = PowerMockito.mock(NotificationCompat.Builder.class);
        Intent intent = PowerMockito.mock(Intent.class);
        when(IntentFactory.createIntent()).thenReturn(intent);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_ACTIONS)).thenReturn(jsonString);

        mHubUtil.processNotificationActions(
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
