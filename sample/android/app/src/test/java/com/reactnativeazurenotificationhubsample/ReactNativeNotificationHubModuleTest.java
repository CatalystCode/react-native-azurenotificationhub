package com.reactnativeazurenotificationhubsample;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
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

import com.azure.reactnative.notificationhub.NotificationHubUtil;
import com.azure.reactnative.notificationhub.ReactNativeNotificationHubModule;
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
        NotificationHubUtil.class,
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
    NotificationHubUtil mNotificationHubUtil;

    @Mock
    NotificationHub mNotificationHub;

    ReactNativeNotificationHubModule mHubModule;

    @Before
    public void setUp() {
        // Prepare mock objects
        PowerMockito.mockStatic(LocalBroadcastManager.class);
        when(LocalBroadcastManager.getInstance(mReactApplicationContext)).thenReturn(mLocalBroadcastManager);
        PowerMockito.mockStatic(NotificationHubUtil.class);
        when(NotificationHubUtil.getInstance()).thenReturn(mNotificationHubUtil);
        PowerMockito.mockStatic(GoogleApiAvailability.class);
        when(GoogleApiAvailability.getInstance()).thenReturn(mGoogleApiAvailability);

        // Reset mocks
        reset(mPromise);
        reset(mConfig);
        reset(mTags);

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
        Assert.assertEquals(mHubModule.getName(), ReactNativeNotificationHubModule.AZURE_NOTIFICATION_HUB_NAME);
    }

    @Test
    public void testRegisterMissingConnectionString() {
        when(mConfig.getString("connectionString")).thenReturn(null);

        mHubModule.register(mConfig, mPromise);

        verify(mPromise, times(1)).reject(
                ReactNativeNotificationHubModule.ERROR_INVALID_ARGUMENTS,
                ReactNativeNotificationHubModule.ERROR_INVALID_CONNECTION_STRING);
    }

    @Test
    public void testRegisterMissingHubName() {
        when(mConfig.getString("connectionString")).thenReturn("connectionString");
        when(mConfig.getString("hubName")).thenReturn(null);

        mHubModule.register(mConfig, mPromise);

        verify(mPromise, times(1)).reject(
                ReactNativeNotificationHubModule.ERROR_INVALID_ARGUMENTS,
                ReactNativeNotificationHubModule.ERROR_INVALID_HUBNAME);
    }

    @Test
    public void testRegisterMissingSenderID() {
        when(mConfig.getString("connectionString")).thenReturn("connectionString");
        when(mConfig.getString("hubName")).thenReturn("hubName");
        when(mConfig.getString("senderID")).thenReturn(null);

        mHubModule.register(mConfig, mPromise);

        verify(mPromise, times(1)).reject(
                ReactNativeNotificationHubModule.ERROR_INVALID_ARGUMENTS,
                ReactNativeNotificationHubModule.ERROR_INVALID_SENDER_ID);
    }

    @Test
    public void testRegisterSuccessfully() {
        String[] tags = {"tags"};

        when(mConfig.getString("connectionString")).thenReturn("connectionString");
        when(mConfig.getString("hubName")).thenReturn("hubName");
        when(mConfig.getString("senderID")).thenReturn("senderID");
        when(mConfig.hasKey("tags")).thenReturn(true);
        when(mConfig.isNull("tags")).thenReturn(false);
        when(mConfig.getArray("tags")).thenReturn(mTags);
        when(mTags.size()).thenReturn(1);
        when(mTags.getString(0)).thenReturn(tags[0]);
        when(mGoogleApiAvailability.isGooglePlayServicesAvailable(anyObject())).thenReturn(
                ConnectionResult.SUCCESS);

        mHubModule.register(mConfig, mPromise);

        verify(mNotificationHubUtil, times(1)).setConnectionString(
                any(ReactContext.class), eq("connectionString"));
        verify(mNotificationHubUtil, times(1)).setHubName(
                any(ReactContext.class), eq("hubName"));
        verify(mNotificationHubUtil, times(1)).setTags(
                any(ReactContext.class), eq(tags));
        verify(mPromise, times(0)).reject(anyString(), anyString());
    }

    @Test
    public void testRegisterFailed() {
        String[] tags = {"tags"};
        when(mConfig.getString("connectionString")).thenReturn("connectionString");
        when(mConfig.getString("hubName")).thenReturn("hubName");
        when(mConfig.getString("senderID")).thenReturn("senderID");
        when(mConfig.hasKey("tags")).thenReturn(true);
        when(mConfig.isNull("tags")).thenReturn(false);
        when(mConfig.getArray("tags")).thenReturn(mTags);
        when(mTags.size()).thenReturn(1);
        when(mTags.getString(0)).thenReturn(tags[0]);
        when(mGoogleApiAvailability.isGooglePlayServicesAvailable(anyObject())).thenReturn(
                ConnectionResult.INTERNAL_ERROR);
        when(mGoogleApiAvailability.isUserResolvableError(anyInt())).thenReturn(false);

        mHubModule.register(mConfig, mPromise);

        verify(mPromise, times(1)).reject(
                ReactNativeNotificationHubModule.ERROR_PLAY_SERVICES,
                ReactNativeNotificationHubModule.ERROR_PLAY_SERVICES_UNSUPPORTED);
    }

    @Test
    public void testUnregisterSuccessfully() throws Exception {
        when(mNotificationHubUtil.getConnectionString(any(ReactContext.class))).thenReturn("connectionString");
        when(mNotificationHubUtil.getHubName(any(ReactContext.class))).thenReturn("hubName");
        when(mNotificationHubUtil.getRegistrationID(any(ReactContext.class))).thenReturn("registrationId");
        when(mNotificationHubUtil.createNotificationHub(
                anyString(), anyString(), any(ReactContext.class))).thenReturn(mNotificationHub);

        mHubModule.unregister(mPromise);

        verify(mPromise, times(0)).reject(anyString(), anyString());
        verify(mNotificationHub, times(1)).unregister();
        verify(mNotificationHubUtil, times(1)).setRegistrationID(
                any(ReactContext.class), eq(null));
    }

    @Test
    public void testUnregisterNoRegistration() throws Exception {
        when(mNotificationHubUtil.getConnectionString(any(ReactContext.class))).thenReturn("connectionString");
        when(mNotificationHubUtil.getHubName(any(ReactContext.class))).thenReturn("hubName");
        when(mNotificationHubUtil.getRegistrationID(any(ReactContext.class))).thenReturn(null);
        when(mNotificationHubUtil.createNotificationHub(
                anyString(), anyString(), any(ReactContext.class))).thenReturn(mNotificationHub);

        mHubModule.unregister(mPromise);

        verify(mPromise, times(1)).reject(
                ReactNativeNotificationHubModule.ERROR_NOT_REGISTERED,
                ReactNativeNotificationHubModule.ERROR_NOT_REGISTERED_DESC);
    }

    @Test
    public void testUnregisterThrowException() throws Exception {
        Exception unhandledException = new Exception("Unhandled exception");
        when(mNotificationHubUtil.getConnectionString(any(ReactContext.class))).thenReturn("connectionString");
        when(mNotificationHubUtil.getHubName(any(ReactContext.class))).thenReturn("hubName");
        when(mNotificationHubUtil.getRegistrationID(any(ReactContext.class))).thenReturn("registrationId");
        when(mNotificationHubUtil.createNotificationHub(
                anyString(), anyString(), any(ReactContext.class))).thenReturn(mNotificationHub);
        doThrow(unhandledException).when(mNotificationHub).unregister();

        mHubModule.unregister(mPromise);

        verify(mPromise, times(1)).reject(
                ReactNativeNotificationHubModule.ERROR_NOTIFICATION_HUB,
                unhandledException);
    }
}