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
+ (void)didRegisterUserNotificationSettings:(nonnull UIUserNotificationSettings *)notificationSettings;

// Required for the register event, invoked from AppDelegate
+ (void)didRegisterForRemoteNotificationsWithDeviceToken:(nonnull NSData *)deviceToken;

// Required for the notification event, invoked from AppDelegate
+ (void)didReceiveRemoteNotification:(nonnull NSDictionary *)notification;

// Required for the localNotification event, invoked from AppDelegate
+ (void)didReceiveLocalNotification:(nonnull UILocalNotification *)notification;

// Required for the registrationError event, invoked from AppDelegate
+ (void)didFailToRegisterForRemoteNotificationsWithError:(nonnull NSError *)error;

// Set application icon badge number
- (void)setApplicationIconBadgeNumber:(NSInteger)number;

// Get application icon badge number
- (void)getApplicationIconBadgeNumber:(nonnull RCTResponseSenderBlock)callback;

// Request notification permissions
- (void)requestPermissions:(nonnull NSDictionary *)permissions
                  resolver:(nonnull RCTPromiseResolveBlock)resolve
                  rejecter:(nonnull RCTPromiseRejectBlock)reject;

// Abandon notification permissions
- (void)abandonPermissions;

// Check notification permissions
- (void)checkPermissions:(nonnull RCTResponseSenderBlock)callback;

// Present local notification
- (void)presentLocalNotification:(nonnull UILocalNotification *)notification;

// Schedule local notification
- (void)scheduleLocalNotification:(nonnull UILocalNotification *)notification;

// Cancel all local notifications
- (void)cancelAllLocalNotifications;

// Cancel local notifications
- (void)cancelLocalNotifications:(nonnull NSDictionary<NSString *, id> *)userInfo;

// Get initial notification
- (void)getInitialNotification:(nonnull RCTPromiseResolveBlock)resolve
                        reject:(nonnull __unused RCTPromiseRejectBlock)reject;

// Get scheduled local notifications
- (void)getScheduledLocalNotifications:(nonnull RCTResponseSenderBlock)callback;

// Register with Azure Notification Hub
- (void)register:(nonnull NSString *)deviceToken
          config:(nonnull NSDictionary *)config
        resolver:(nonnull RCTPromiseResolveBlock)resolve
        rejecter:(nonnull RCTPromiseRejectBlock)reject;

// Unregister with Azure Notification Hub
- (void)unregister:(nonnull RCTPromiseResolveBlock)resolve
          rejecter:(nonnull RCTPromiseRejectBlock)reject;

@end
