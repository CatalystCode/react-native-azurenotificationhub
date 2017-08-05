/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#import "React/RCTEventEmitter.h"

@interface RCTAzureNotificationHubManager : RCTEventEmitter

// Required to register for notifications, invoked from AppDelegate
+ (void)didRegisterUserNotificationSettings:(UIUserNotificationSettings *)notificationSettings;

// Required for the register event, invoked from AppDelegate
+ (void)didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken;

// Required for the notification event, invoked from AppDelegate
+ (void)didReceiveRemoteNotification:(NSDictionary *)notification;

// Required for the localNotification event, invoked from AppDelegate
+ (void)didReceiveLocalNotification:(UILocalNotification *)notification;

// Required for the registrationError event, invoked from AppDelegate
+ (void)didFailToRegisterForRemoteNotificationsWithError:(NSError *)error;

@end
