package com.reactnativeazurenotificationhubsample;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static com.azure.reactnative.notificationhub.ReactNativeConstants.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import com.azure.reactnative.notificationhub.ReactNativeNotificationHubUtil;
import com.facebook.react.bridge.ReactApplicationContext;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Unit tests for ReactNativeNotificationHubModule.
 */
@RunWith(PowerMockRunner.class)
public class ReactNativeNotificationHubUtilTest {

    @Mock
    ReactApplicationContext mReactApplicationContext;

    @Mock
    SharedPreferences mSharedPreferences;

    @Mock
    SharedPreferences.Editor mEditor;

    @Mock
    Bundle mBundle;

    ReactNativeNotificationHubUtil mHubUtil;

    @Before
    public void setUp() {
        // Reset mocks
        reset(mBundle);
        reset(mSharedPreferences);
        reset(mEditor);

        // Prepare mock objects
        when(mReactApplicationContext.getSharedPreferences(
                SHARED_PREFS_NAME, Context.MODE_PRIVATE)).thenReturn(mSharedPreferences);
        when(mSharedPreferences.edit()).thenReturn(mEditor);

        mHubUtil = ReactNativeNotificationHubUtil.getInstance();
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
    public void testGetChannelName() {
        mHubUtil.getChannelName(mReactApplicationContext);

        verify(mSharedPreferences, times(1)).getString(
                KEY_FOR_PREFS_CHANNELNAME, null);
    }

    @Test
    public void testSetChannelName() {
        final String channelName = "Channel Name";

        mHubUtil.setChannelName(mReactApplicationContext, channelName);

        verify(mEditor, times(1)).putString(
                KEY_FOR_PREFS_CHANNELNAME, channelName);
        verify(mEditor, times(1)).apply();
    }

    @Test
    public void testHasChannelName() {
        mHubUtil.hasChannelName(mReactApplicationContext);

        verify(mSharedPreferences, times(1)).contains(KEY_FOR_PREFS_CHANNELNAME);
    }

    @Test
    public void testGetChannelDescription() {
        mHubUtil.getChannelDescription(mReactApplicationContext);

        verify(mSharedPreferences, times(1)).getString(
                KEY_FOR_PREFS_CHANNELDESCRIPTION, null);
    }

    @Test
    public void testSetChannelDescription() {
        final String channelDescription = "Channel Description";

        mHubUtil.setChannelDescription(mReactApplicationContext, channelDescription);

        verify(mEditor, times(1)).putString(
                KEY_FOR_PREFS_CHANNELDESCRIPTION, channelDescription);
        verify(mEditor, times(1)).apply();
    }

    @Test
    public void testHasChannelDescription() {
        mHubUtil.hasChannelDescription(mReactApplicationContext);

        verify(mSharedPreferences, times(1)).contains(KEY_FOR_PREFS_CHANNELDESCRIPTION);
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
    public void testGetTemplateName() {
        mHubUtil.getTemplateName(mReactApplicationContext);

        verify(mSharedPreferences, times(1)).getString(
                KEY_FOR_PREFS_TEMPLATENAME, null);
    }

    @Test
    public void testSetTemplateName() {
        final String templateName = "Template Name";

        mHubUtil.setTemplateName(mReactApplicationContext, templateName);

        verify(mEditor, times(1)).putString(
                KEY_FOR_PREFS_TEMPLATENAME, templateName);
        verify(mEditor, times(1)).apply();
    }

    @Test
    public void testGetTemplate() {
        mHubUtil.getTemplate(mReactApplicationContext);

        verify(mSharedPreferences, times(1)).getString(
                KEY_FOR_PREFS_TEMPLATE, null);
    }

    @Test
    public void testSetTemplate() {
        final String template = "Template";

        mHubUtil.setTemplate(mReactApplicationContext, template);

        verify(mEditor, times(1)).putString(
                KEY_FOR_PREFS_TEMPLATE, template);
        verify(mEditor, times(1)).apply();
    }

    @Test
    public void testIsTemplated() {
        mHubUtil.isTemplated(mReactApplicationContext);

        verify(mSharedPreferences, times(1)).getBoolean(
                KEY_FOR_PREFS_ISTEMPLATE, false);
    }

    @Test
    public void testSetTemplated() {
        final boolean isTemplated = true;

        mHubUtil.setTemplated(mReactApplicationContext, isTemplated);

        verify(mEditor, times(1)).putBoolean(
                KEY_FOR_PREFS_ISTEMPLATE, isTemplated);
        verify(mEditor, times(1)).apply();
    }

    @Test
    public void testGetUUID() {
        mHubUtil.getUUID(mReactApplicationContext);

        verify(mSharedPreferences, times(1)).getString(
                KEY_FOR_PREFS_UUID, null);
    }

    @Test
    public void testSetUUID() {
        final String uuid = "uuid";

        mHubUtil.setUUID(mReactApplicationContext, uuid);

        verify(mEditor, times(1)).putString(
                KEY_FOR_PREFS_UUID, uuid);
        verify(mEditor, times(1)).apply();
    }
}
