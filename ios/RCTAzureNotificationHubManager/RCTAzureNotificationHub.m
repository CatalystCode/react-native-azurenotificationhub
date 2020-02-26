/**
* Copyright (c) 2015-present, Facebook, Inc.
* All rights reserved.
*
* This source code is licensed under the BSD-style license found in the
* LICENSE file in the root directory of this source tree. An additional grant
* of patent rights can be found in the PATENTS file in the same directory.
*/

// Notification Hub events
NSString *const RCTLocalNotificationReceived                    = @"localNotificationReceived";
NSString *const RCTRemoteNotificationReceived                   = @"remoteNotificationReceived";
NSString *const RCTRemoteNotificationRegistered                 = @"remoteNotificationRegistered";
NSString *const RCTRemoteNotificationRegisteredError            = @"remoteNotificationRegisteredError";
NSString *const RCTAzureNotificationHubRegistered               = @"azureNotificationHubRegistered";
NSString *const RCTAzureNotificationHubRegisteredError          = @"azureNotificationHubRegisteredError";
NSString *const RCTUserNotificationSettingsRegistered           = @"userNotificationSettingsRegistered";

// Config keys used when registering
NSString *const RCTConnectionStringKey                          = @"connectionString";
NSString *const RCTHubNameKey                                   = @"hubName";
NSString *const RCTTagsKey                                      = @"tags";

// User info
NSString *const RCTUserInfoNotificationSettings                 = @"notificationSettings";
NSString *const RCTUserInfoDeviceToken                          = @"deviceToken";
NSString *const RCTUserInfoRemote                               = @"remote";
NSString *const RCTUserInfoSuccess                              = @"success";
NSString *const RCTUserInfoError                                = @"error";

// Notification types
NSString *const RCTNotificationTypeBadge                        = @"badge";
NSString *const RCTNotificationTypeSound                        = @"sound";
NSString *const RCTNotificationTypeAlert                        = @"alert";

// Errors
NSString *const RCTErrorUnableToRequestPermissions              = @"Unabled to request permissions";
NSString *const RCTErrorUnableToRequestPermissionsDetails       = @"Requesting push notifications is currently unavailable in an app extension";
NSString *const RCTErrorUnableToRequestPermissionsTwice         = @"Cannot call requestPermissions twice before the first has returned.";
NSString *const RCTErrorInvalidArguments                        = @"Invalid arguments";
NSString *const RCTErrorMissingConnectionString                 = @"Connection string cannot be null.";
NSString *const RCTErrorMissingHubName                          = @"Hub name cannot be null.";
