package com.azure.reactnative.notificationhub;

public final class ReactNativeConstants {
    // Notification
    public static final String AZURE_NOTIFICATION_HUB_NAME = "AzureNotificationHub";
    public static final String NOTIFICATION_CHANNEL_ID = "rn-push-notification-channel-id";
    public static final String KEY_NOTIFICATION_PAYLOAD_TYPE = "notification";
    public static final String KEY_DATA_PAYLOAD_TYPE = "data";

    // Notification hub events
    public static final String EVENT_REMOTE_NOTIFICATION_RECEIVED = "remoteNotificationReceived";
    public static final String EVENT_AZURE_NOTIFICATION_HUB_REGISTERED = "azureNotificationHubRegistered";
    public static final String EVENT_AZURE_NOTIFICATION_HUB_REGISTERED_ERROR = "azureNotificationHubRegisteredError";

    // Registration's keys
    public static final String KEY_REGISTRATION_CONNECTIONSTRING = "connectionString";
    public static final String KEY_REGISTRATION_HUBNAME = "hubName";
    public static final String KEY_REGISTRATION_SENDERID = "senderID";
    public static final String KEY_REGISTRATION_TAGS = "tags";
    public static final String KEY_REGISTRATION_CHANNELNAME = "channelName";
    public static final String KEY_REGISTRATION_CHANNELDESCRIPTION = "channelDescription";
    public static final String KEY_REGISTRATION_CHANNELIMPORTANCE = "channelImportance";
    public static final String KEY_REGISTRATION_CHANNELSHOWBADGE = "channelShowBadge";
    public static final String KEY_REGISTRATION_CHANNELENABLELIGHTS = "channelEnableLights";
    public static final String KEY_REGISTRATION_CHANNELENABLEVIBRATION = "channelEnableVibration";
    public static final String KEY_REGISTRATION_TEMPLATENAME = "templateName";
    public static final String KEY_REGISTRATION_TEMPLATE = "template";
    public static final String KEY_REGISTRATION_ISTEMPLATE = "isTemplate";

    // Shared prefs used in NotificationHubUtil
    public static final String SHARED_PREFS_NAME = "com.azure.reactnative.notificationhub.NotificationHubUtil";
    public static final String KEY_FOR_PREFS_REGISTRATIONID = "AzureNotificationHub_registrationID";
    public static final String KEY_FOR_PREFS_CONNECTIONSTRING = "AzureNotificationHub_connectionString";
    public static final String KEY_FOR_PREFS_HUBNAME = "AzureNotificationHub_hubName";
    public static final String KEY_FOR_PREFS_FCMTOKEN = "AzureNotificationHub_FCMToken";
    public static final String KEY_FOR_PREFS_TAGS = "AzureNotificationHub_Tags";
    public static final String KEY_FOR_PREFS_SENDERID = "AzureNotificationHub_senderID";
    public static final String KEY_FOR_PREFS_CHANNELNAME = "AzureNotificationHub_channelName";
    public static final String KEY_FOR_PREFS_CHANNELDESCRIPTION = "AzureNotificationHub_channelDescription";
    public static final String KEY_FOR_PREFS_CHANNELIMPORTANCE = "AzureNotificationHub_channelImportance";
    public static final String KEY_FOR_PREFS_CHANNELSHOWBADGE = "AzureNotificationHub_channelShowBadge";
    public static final String KEY_FOR_PREFS_CHANNELENABLELIGHTS = "AzureNotificationHub_channelEnableLights";
    public static final String KEY_FOR_PREFS_CHANNELENABLEVIBRATION = "AzureNotificationHub_channelEnableVibration";
    public static final String KEY_FOR_PREFS_TEMPLATENAME = "AzureNotificationHub_templateName";
    public static final String KEY_FOR_PREFS_TEMPLATE = "AzureNotificationHub_template";
    public static final String KEY_FOR_PREFS_ISTEMPLATE = "AzureNotificationHub_isTemplate";
    public static final String KEY_FOR_PREFS_UUID = "AzureNotificationHub_UUID";

    // Remote notification payload
    public static final String KEY_REMOTE_NOTIFICATION_MESSAGE = "message";
    public static final String KEY_REMOTE_NOTIFICATION_BODY = "body";
    public static final String KEY_REMOTE_NOTIFICATION_ID = "google.message_id";
    public static final String KEY_REMOTE_NOTIFICATION_TITLE = "title";
    public static final String KEY_REMOTE_NOTIFICATION_PRIORITY = "google.original_priority";
    public static final String KEY_REMOTE_NOTIFICATION_TICKER = "ticker";
    public static final String KEY_REMOTE_NOTIFICATION_AUTO_CANCEL = "autoCancel";
    public static final String KEY_REMOTE_NOTIFICATION_GROUP = "group";
    public static final String KEY_REMOTE_NOTIFICATION_LARGE_ICON = "largeIcon";
    public static final String KEY_REMOTE_NOTIFICATION_SUB_TEXT = "subText";
    public static final String KEY_REMOTE_NOTIFICATION_NUMBER = "number";
    public static final String KEY_REMOTE_NOTIFICATION_SMALL_ICON = "smallIcon";
    public static final String KEY_REMOTE_NOTIFICATION_BIG_TEXT = "bigText";
    public static final String KEY_REMOTE_NOTIFICATION_PLAY_SOUND = "playSound";
    public static final String KEY_REMOTE_NOTIFICATION_SOUND_NAME = "soundName";
    public static final String KEY_REMOTE_NOTIFICATION_ONGOING = "ongoing";
    public static final String KEY_REMOTE_NOTIFICATION_COLOR = "color";
    public static final String KEY_REMOTE_NOTIFICATION_VIBRATE = "vibrate";
    public static final String KEY_REMOTE_NOTIFICATION_VIBRATION = "vibration";
    public static final String KEY_REMOTE_NOTIFICATION_FOREGROUND = "foreground";
    public static final String KEY_REMOTE_NOTIFICATION_ACTIONS = "actions";
    public static final String KEY_REMOTE_NOTIFICATION_ACTION = "action";
    public static final String KEY_REMOTE_NOTIFICATION_TAG = "tag";
    public static final String KEY_REMOTE_NOTIFICATION_USER_INTERACTION = "userInteraction";
    public static final String KEY_REMOTE_NOTIFICATION_COLDSTART = "coldstart";
    public static final String KEY_REMOTE_NOTIFICATION_AVATAR_URL = "avatarUrl";

    // Remote notification payload's priority
    public static final String REMOTE_NOTIFICATION_PRIORITY_MAX = "max";
    public static final String REMOTE_NOTIFICATION_PRIORITY_HIGH = "high";
    public static final String REMOTE_NOTIFICATION_PRIORITY_LOW = "low";
    public static final String REMOTE_NOTIFICATION_PRIORITY_MIN = "min";
    public static final String REMOTE_NOTIFICATION_PRIORITY_NORMAL = "normal";

    // Intent
    public static final String KEY_INTENT_EVENT_NAME = "eventName";
    public static final String KEY_INTENT_EVENT_TYPE = "eventType";
    public static final String KEY_INTENT_EVENT_STRING_DATA = "eventStringData";
    public static final String INTENT_EVENT_TYPE_STRING = "eventTypeString";
    public static final String INTENT_EVENT_TYPE_BUNDLE = "eventTypeBundle";

    // Resources
    public static final String RESOURCE_DEF_TYPE_MIPMAP = "mipmap";
    public static final String RESOURCE_DEF_TYPE_RAW = "raw";
    public static final String RESOURCE_NAME_NOTIFICATION = "ic_notification";
    public static final String RESOURCE_NAME_LAUNCHER = "ic_launcher";

    // Promise
    public static final String KEY_PROMISE_RESOLVE_UUID = "uuid";
    public static final String AZURE_NOTIFICATION_HUB_UNREGISTERED = "Unregistered successfully";

    // Errors
    public static final String ERROR_NO_ACTIVITY_CLASS = "No activity class found for the notification";
    public static final String ERROR_NO_MESSAGE = "No message specified for the notification";
    public static final String ERROR_COVERT_ACTIONS = "Exception while converting actions to JSON object.";
    public static final String ERROR_GET_ACTIONS_ARRAY = "Exception while getting action from actionsArray.";
    public static final String ERROR_SEND_PUSH_NOTIFICATION = "failed to send push notification";
    public static final String ERROR_ACTIVITY_CLASS_NOT_FOUND = "Activity class not found";
    public static final String ERROR_INVALID_ARGUMENTS = "E_INVALID_ARGUMENTS";
    public static final String ERROR_INVALID_CONNECTION_STRING = "Connection string cannot be null.";
    public static final String ERROR_INVALID_HUBNAME = "Hub name cannot be null.";
    public static final String ERROR_INVALID_SENDER_ID = "Sender ID cannot be null.";
    public static final String ERROR_INVALID_TEMPLATE_NAME = "Template Name cannot be null.";
    public static final String ERROR_INVALID_TEMPLATE = "Template cannot be null.";
    public static final String ERROR_PLAY_SERVICES = "E_PLAY_SERVICES";
    public static final String ERROR_PLAY_SERVICES_DISABLED = "User must enable Google Play Services.";
    public static final String ERROR_PLAY_SERVICES_UNSUPPORTED = "This device is not supported by Google Play Services.";
    public static final String ERROR_NOTIFICATION_HUB = "E_NOTIFICATION_HUB";
    public static final String ERROR_NOT_REGISTERED = "E_NOT_REGISTERED";
    public static final String ERROR_NOT_REGISTERED_DESC = "No registration to Azure Notification Hub.";
    public static final String ERROR_FETCH_IMAGE = "Error while fetching image.";
    public static final String ERROR_GET_INIT_NOTIFICATION = "E_GET_INIT_NOTIF";
    public static final String ERROR_ACTIVITY_IS_NULL = "Current activity is null";
    public static final String ERROR_INTENT_EXTRAS_IS_NULL = "Intent get extras is null";
    public static final String ERROR_ACTIVITY_INTENT_IS_NULL = "Activity intent is null";
    public static final String ERROR_GET_UUID = "E_GET_UUID";
    public static final String ERROR_NO_UUID_SET = "No uuid set";

    private ReactNativeConstants() {
    }
}
