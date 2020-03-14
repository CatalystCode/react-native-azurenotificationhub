package com.reactnativeazurenotificationhubsample;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static com.azure.reactnative.notificationhub.ReactNativeConstants.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.powermock.api.mockito.PowerMockito.when;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.azure.reactnative.notificationhub.ReactNativeNotificationHubUtil;
import com.azure.reactnative.notificationhub.ReactNativeNotificationHubModule;
import com.azure.reactnative.notificationhub.ReactNativeNotificationsHandler;
import com.azure.reactnative.notificationhub.ReactNativeRegistrationIntentService;
import com.azure.reactnative.notificationhub.ReactNativeUtil;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.microsoft.windowsazure.messaging.NotificationHub;

/**
 * Unit tests for ReactNativeNotificationHubModule.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
        LocalBroadcastManager.class,
        ReactNativeNotificationHubUtil.class,
        ReactNativeUtil.class,
        ReactNativeNotificationsHandler.class,
        ReactNativeRegistrationIntentService.class,
        GoogleApiAvailability.class
})
public class ReactNativeNotificationHubModuleTest {
    @Mock
    ReactApplicationContext mReactApplicationContext;

    @Mock
    LocalBroadcastManager mLocalBroadcastManager;

    @Mock
    Promise mPromise;

    @Mock
    GoogleApiAvailability mGoogleApiAvailability;

    @Mock
    ReadableMap mConfig;

    @Mock
    ReadableArray mTags;

    @Mock
    ReactNativeNotificationHubUtil mNotificationHubUtil;

    @Mock
    NotificationHub mNotificationHub;

    ReactNativeNotificationHubModule mHubModule;

    @Before
    public void setUp() {
        // Reset mocks
        reset(mPromise);
        reset(mConfig);
        reset(mTags);
        reset(mNotificationHubUtil);
        reset(mReactApplicationContext);

        // Prepare mock objects
        PowerMockito.mockStatic(LocalBroadcastManager.class);
        when(LocalBroadcastManager.getInstance(mReactApplicationContext)).thenReturn(mLocalBroadcastManager);
        PowerMockito.mockStatic(ReactNativeNotificationHubUtil.class);
        when(ReactNativeNotificationHubUtil.getInstance()).thenReturn(mNotificationHubUtil);
        PowerMockito.mockStatic(ReactNativeUtil.class);
        PowerMockito.mockStatic(ReactNativeNotificationsHandler.class);
        PowerMockito.mockStatic(ReactNativeRegistrationIntentService.class);
        PowerMockito.mockStatic(GoogleApiAvailability.class);
        when(GoogleApiAvailability.getInstance()).thenReturn(mGoogleApiAvailability);

        mHubModule = new ReactNativeNotificationHubModule(mReactApplicationContext);
    }

    @Test
    public void testInitialization() {
        Assert.assertNotNull(mHubModule);
        verify(mLocalBroadcastManager, times(2)).registerReceiver(
                any(BroadcastReceiver.class), any(IntentFilter.class));
        verify(mReactApplicationContext, times(1)).addLifecycleEventListener(
                any(ReactNativeNotificationHubModule.class));
    }

    @Test
    public void testGetName() {
        Assert.assertEquals(mHubModule.getName(), AZURE_NOTIFICATION_HUB_NAME);
    }

    @Test
    public void testSetIsForeground() {
        mHubModule.setIsForeground(true);
        verify(mNotificationHubUtil, times(1)).setAppIsForeground(true);

        mHubModule.setIsForeground(false);
        verify(mNotificationHubUtil, times(1)).setAppIsForeground(false);
    }

    @Test
    public void testGetIsForeground() {
        mHubModule.getIsForeground();

        verify(mNotificationHubUtil, times(1)).getAppIsForeground();
    }

    @Test
    public void testRegisterMissingConnectionString() {
        when(mConfig.getString(KEY_REGISTRATION_CONNECTIONSTRING)).thenReturn(null);

        mHubModule.register(mConfig, mPromise);

        verify(mPromise, times(1)).reject(
                ERROR_INVALID_ARGUMENTS,
                ERROR_INVALID_CONNECTION_STRING);
    }

    @Test
    public void testRegisterMissingHubName() {
        when(mConfig.getString(KEY_REGISTRATION_CONNECTIONSTRING)).thenReturn("Connection String");
        when(mConfig.getString(KEY_REGISTRATION_HUBNAME)).thenReturn(null);

        mHubModule.register(mConfig, mPromise);

        verify(mPromise, times(1)).reject(
                ERROR_INVALID_ARGUMENTS,
                ERROR_INVALID_HUBNAME);
    }

    @Test
    public void testRegisterMissingSenderID() {
        when(mConfig.getString(KEY_REGISTRATION_CONNECTIONSTRING)).thenReturn("Connection String");
        when(mConfig.getString(KEY_REGISTRATION_HUBNAME)).thenReturn("Hub Name");
        when(mConfig.getString(KEY_REGISTRATION_SENDERID)).thenReturn(null);

        mHubModule.register(mConfig, mPromise);

        verify(mPromise, times(1)).reject(
                ERROR_INVALID_ARGUMENTS,
                ERROR_INVALID_SENDER_ID);
    }

    @Test
    public void testRegisterHasChannelName() {
        final String channelName = "Channel Name";

        when(mConfig.getString(KEY_REGISTRATION_CONNECTIONSTRING)).thenReturn("Connection String");
        when(mConfig.getString(KEY_REGISTRATION_HUBNAME)).thenReturn("Hub Name");
        when(mConfig.getString(KEY_REGISTRATION_SENDERID)).thenReturn("Sender ID");
        when(mConfig.hasKey(KEY_REGISTRATION_CHANNELNAME)).thenReturn(true);
        when(mConfig.getString(KEY_REGISTRATION_CHANNELNAME)).thenReturn(channelName);

        mHubModule.register(mConfig, mPromise);

        verify(mNotificationHubUtil, times(1)).setChannelName(
                any(ReactContext.class), eq(channelName));
    }

    @Test
    public void testRegisterHasChannelImportance() {
        final int channelImportance = 1;

        when(mConfig.getString(KEY_REGISTRATION_CONNECTIONSTRING)).thenReturn(KEY_REGISTRATION_CONNECTIONSTRING);
        when(mConfig.getString(KEY_REGISTRATION_HUBNAME)).thenReturn(KEY_REGISTRATION_HUBNAME);
        when(mConfig.getString(KEY_REGISTRATION_SENDERID)).thenReturn(KEY_REGISTRATION_SENDERID);
        when(mConfig.hasKey(KEY_REGISTRATION_CHANNELIMPORTANCE)).thenReturn(true);
        when(mConfig.getInt(KEY_REGISTRATION_CHANNELIMPORTANCE)).thenReturn(channelImportance);

        mHubModule.register(mConfig, mPromise);

        verify(mNotificationHubUtil, times(1)).setChannelImportance(
                any(ReactContext.class), eq(channelImportance));
    }

    @Test
    public void testRegisterHasChannelShowBadge() {
        final boolean channelShowBadge = true;

        when(mConfig.getString(KEY_REGISTRATION_CONNECTIONSTRING)).thenReturn(KEY_REGISTRATION_CONNECTIONSTRING);
        when(mConfig.getString(KEY_REGISTRATION_HUBNAME)).thenReturn(KEY_REGISTRATION_HUBNAME);
        when(mConfig.getString(KEY_REGISTRATION_SENDERID)).thenReturn(KEY_REGISTRATION_SENDERID);
        when(mConfig.hasKey(KEY_REGISTRATION_CHANNELSHOWBADGE)).thenReturn(true);
        when(mConfig.getBoolean(KEY_REGISTRATION_CHANNELSHOWBADGE)).thenReturn(channelShowBadge);

        mHubModule.register(mConfig, mPromise);

        verify(mNotificationHubUtil, times(1)).setChannelShowBadge(
                any(ReactContext.class), eq(channelShowBadge));
    }

    @Test
    public void testRegisterHasChannelEnableLights() {
        final boolean channelEnableLights = true;

        when(mConfig.getString(KEY_REGISTRATION_CONNECTIONSTRING)).thenReturn(KEY_REGISTRATION_CONNECTIONSTRING);
        when(mConfig.getString(KEY_REGISTRATION_HUBNAME)).thenReturn(KEY_REGISTRATION_HUBNAME);
        when(mConfig.getString(KEY_REGISTRATION_SENDERID)).thenReturn(KEY_REGISTRATION_SENDERID);
        when(mConfig.hasKey(KEY_REGISTRATION_CHANNELENABLELIGHTS)).thenReturn(true);
        when(mConfig.getBoolean(KEY_REGISTRATION_CHANNELENABLELIGHTS)).thenReturn(channelEnableLights);

        mHubModule.register(mConfig, mPromise);

        verify(mNotificationHubUtil, times(1)).setChannelEnableLights(
                any(ReactContext.class), eq(channelEnableLights));
    }

    @Test
    public void testRegisterHasChannelEnableVibration() {
        final boolean channelEnableVibration = true;

        when(mConfig.getString(KEY_REGISTRATION_CONNECTIONSTRING)).thenReturn(KEY_REGISTRATION_CONNECTIONSTRING);
        when(mConfig.getString(KEY_REGISTRATION_HUBNAME)).thenReturn(KEY_REGISTRATION_HUBNAME);
        when(mConfig.getString(KEY_REGISTRATION_SENDERID)).thenReturn(KEY_REGISTRATION_SENDERID);
        when(mConfig.hasKey(KEY_REGISTRATION_CHANNELENABLEVIBRATION)).thenReturn(true);
        when(mConfig.getBoolean(KEY_REGISTRATION_CHANNELENABLEVIBRATION)).thenReturn(channelEnableVibration);

        mHubModule.register(mConfig, mPromise);

        verify(mNotificationHubUtil, times(1)).setChannelEnableVibration(
                any(ReactContext.class), eq(channelEnableVibration));
    }

    @Test
    public void testRegisterSuccessfully() {
        final String connectionString = "Connection String";
        final String hubName = "Hub Name";
        final String senderID = "Sender ID";
        final String[] tags = { "Tag" };

        when(mConfig.getString(KEY_REGISTRATION_CONNECTIONSTRING)).thenReturn(connectionString);
        when(mConfig.getString(KEY_REGISTRATION_HUBNAME)).thenReturn(hubName);
        when(mConfig.getString(KEY_REGISTRATION_SENDERID)).thenReturn(senderID);
        when(mConfig.hasKey(KEY_REGISTRATION_TAGS)).thenReturn(true);
        when(mConfig.isNull(KEY_REGISTRATION_TAGS)).thenReturn(false);
        when(mConfig.getArray(KEY_REGISTRATION_TAGS)).thenReturn(mTags);
        when(mTags.size()).thenReturn(1);
        when(mTags.getString(0)).thenReturn(tags[0]);
        when(mGoogleApiAvailability.isGooglePlayServicesAvailable(any())).thenReturn(
                ConnectionResult.SUCCESS);

        mHubModule.register(mConfig, mPromise);

        verify(mNotificationHubUtil, times(1)).setConnectionString(
                any(ReactContext.class), eq(connectionString));
        verify(mNotificationHubUtil, times(1)).setHubName(
                any(ReactContext.class), eq(hubName));
        verify(mNotificationHubUtil, times(1)).setSenderID(
                any(ReactContext.class), eq(senderID));
        verify(mNotificationHubUtil, times(1)).setTags(
                any(ReactContext.class), eq(tags));
        verify(mPromise, times(0)).reject(anyString(), anyString());

        PowerMockito.verifyStatic(ReactNativeRegistrationIntentService.class);
        ReactNativeRegistrationIntentService.enqueueWork(eq(mReactApplicationContext), any(Intent.class));
    }

    @Test
    public void testRegisterFailed() {
        final String[] tags = { "Tag" };

        when(mConfig.getString(KEY_REGISTRATION_CONNECTIONSTRING)).thenReturn("Connection String");
        when(mConfig.getString(KEY_REGISTRATION_HUBNAME)).thenReturn("Hub Name");
        when(mConfig.getString(KEY_REGISTRATION_SENDERID)).thenReturn("Sender ID");
        when(mConfig.hasKey(KEY_REGISTRATION_TAGS)).thenReturn(true);
        when(mConfig.isNull(KEY_REGISTRATION_TAGS)).thenReturn(false);
        when(mConfig.getArray(KEY_REGISTRATION_TAGS)).thenReturn(mTags);
        when(mTags.size()).thenReturn(1);
        when(mTags.getString(0)).thenReturn(tags[0]);
        when(mGoogleApiAvailability.isGooglePlayServicesAvailable(any())).thenReturn(
                ConnectionResult.INTERNAL_ERROR);
        when(mGoogleApiAvailability.isUserResolvableError(anyInt())).thenReturn(false);

        mHubModule.register(mConfig, mPromise);

        verify(mPromise, times(1)).reject(
                ERROR_PLAY_SERVICES,
                ERROR_PLAY_SERVICES_UNSUPPORTED);
    }

    @Test
    public void testUnregisterSuccessfully() throws Exception {
        when(mNotificationHubUtil.getConnectionString(any(ReactContext.class))).thenReturn("Connection String");
        when(mNotificationHubUtil.getHubName(any(ReactContext.class))).thenReturn("Hub Name");
        when(mNotificationHubUtil.getRegistrationID(any(ReactContext.class))).thenReturn("registrationId");
        when(ReactNativeUtil.createNotificationHub(
                anyString(), anyString(), any(ReactContext.class))).thenReturn(mNotificationHub);

        mHubModule.unregister(mPromise);

        verify(mPromise, times(0)).reject(anyString(), anyString());
        verify(mNotificationHub, times(1)).unregister();
        verify(mNotificationHubUtil, times(1)).setRegistrationID(
                any(ReactContext.class), eq(null));
    }

    @Test
    public void testUnregisterNoRegistration() throws Exception {
        when(mNotificationHubUtil.getConnectionString(any(ReactContext.class))).thenReturn("Connection String");
        when(mNotificationHubUtil.getHubName(any(ReactContext.class))).thenReturn("Hub Name");
        when(mNotificationHubUtil.getRegistrationID(any(ReactContext.class))).thenReturn(null);
        when(ReactNativeUtil.createNotificationHub(
                anyString(), anyString(), any(ReactContext.class))).thenReturn(mNotificationHub);

        mHubModule.unregister(mPromise);

        verify(mPromise, times(1)).reject(
                ERROR_NOT_REGISTERED,
                ERROR_NOT_REGISTERED_DESC);
    }

    @Test
    public void testUnregisterThrowException() throws Exception {
        final Exception unhandledException = new Exception("Unhandled exception");

        when(mNotificationHubUtil.getConnectionString(any(ReactContext.class))).thenReturn("Connection String");
        when(mNotificationHubUtil.getHubName(any(ReactContext.class))).thenReturn("Hub Name");
        when(mNotificationHubUtil.getRegistrationID(any(ReactContext.class))).thenReturn("registrationId");
        when(ReactNativeUtil.createNotificationHub(
                anyString(), anyString(), any(ReactContext.class))).thenReturn(mNotificationHub);
        doThrow(unhandledException).when(mNotificationHub).unregister();

        mHubModule.unregister(mPromise);

        verify(mPromise, times(1)).reject(
                ERROR_NOTIFICATION_HUB,
                unhandledException);
    }

    @Test
    public void testOnHostResume() {
        Activity activity = PowerMockito.mock(Activity.class);
        Intent intent = PowerMockito.mock(Intent.class);
        Bundle bundle = PowerMockito.mock(Bundle.class);
        when(mReactApplicationContext.getCurrentActivity()).thenReturn(activity);
        when(activity.getIntent()).thenReturn(intent);
        when(intent.getBundleExtra(KEY_INTENT_NOTIFICATION)).thenReturn(bundle);

        mHubModule.onHostResume();

        verify(mNotificationHubUtil, times(1)).setAppIsForeground(true);
        verify(intent, times(1)).removeExtra(KEY_INTENT_NOTIFICATION);
        verify(bundle, times(1)).putBoolean(KEY_REMOTE_NOTIFICATION_FOREGROUND, false);
        verify(bundle, times(1)).putBoolean(KEY_REMOTE_NOTIFICATION_USER_INTERACTION, true);
        verify(bundle, times(1)).putBoolean(KEY_REMOTE_NOTIFICATION_COLDSTART, true);
        PowerMockito.verifyStatic(ReactNativeNotificationsHandler.class);
        ReactNativeNotificationsHandler.sendBroadcast(eq(mReactApplicationContext), eq(bundle), anyLong());
    }

    @Test
    public void testOnHostPause() {
        mHubModule.onHostPause();

        verify(mNotificationHubUtil, times(1)).setAppIsForeground(false);
    }

    @Test
    public void testOnNewIntent() {
        Intent intent = PowerMockito.mock(Intent.class);
        Bundle bundle = PowerMockito.mock(Bundle.class);
        when(intent.getBundleExtra(KEY_INTENT_NOTIFICATION)).thenReturn(bundle);

        mHubModule.onNewIntent(intent);

        verify(bundle, times(1)).putBoolean(KEY_REMOTE_NOTIFICATION_FOREGROUND, false);
        verify(bundle, times(1)).putBoolean(KEY_REMOTE_NOTIFICATION_USER_INTERACTION, true);
        PowerMockito.verifyStatic(ReactNativeNotificationsHandler.class);
        ReactNativeNotificationsHandler.sendBroadcast(eq(mReactApplicationContext), eq(bundle), anyLong());
    }
}
