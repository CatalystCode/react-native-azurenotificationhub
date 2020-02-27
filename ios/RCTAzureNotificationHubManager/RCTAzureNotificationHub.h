/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#ifndef RCTAzureNotificationHub_h
#define RCTAzureNotificationHub_h

// Notification Hub events
extern NSString *const RCTLocalNotificationReceived;
extern NSString *const RCTRemoteNotificationReceived;
extern NSString *const RCTRemoteNotificationRegistered;
extern NSString *const RCTRemoteNotificationRegisteredError;
extern NSString *const RCTAzureNotificationHubRegistered;
extern NSString *const RCTAzureNotificationHubRegisteredError;
extern NSString *const RCTUserNotificationSettingsRegistered;

// Config keys used when registering
extern NSString *const RCTConnectionStringKey;
extern NSString *const RCTHubNameKey;
extern NSString *const RCTTagsKey;

// User info
extern NSString *const RCTUserInfoNotificationSettings;
extern NSString *const RCTUserInfoDeviceToken;
extern NSString *const RCTUserInfoRemote;
extern NSString *const RCTUserInfoResolveBlock;
extern NSString *const RCTUserInfoRejectBlock;
extern NSString *const RCTUserInfoSuccess;
extern NSString *const RCTUserInfoError;

// Notification types
extern NSString *const RCTNotificationTypeBadge;
extern NSString *const RCTNotificationTypeSound;
extern NSString *const RCTNotificationTypeAlert;

// Errors
extern NSString *const RCTErrorUnableToRequestPermissions;
extern NSString *const RCTErrorUnableToRequestPermissionsDetails;
extern NSString *const RCTErrorUnableToRequestPermissionsTwice;
extern NSString *const RCTErrorInvalidArguments;
extern NSString *const RCTErrorMissingConnectionString;
extern NSString *const RCTErrorMissingHubName;

#endif /* RCTAzureNotificationHub_h */
