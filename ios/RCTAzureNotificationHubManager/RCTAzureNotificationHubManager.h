/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#import "React/RCTEventEmitter.h"

#import "RCTAzureNotificationHandler.h"

@import UserNotifications;

@interface RCTAzureNotificationHubManager : RCTEventEmitter

// Invoked from AppDelegate when the app successfully registered with Apple Push Notification service (APNs).
+ (void)didRegisterForRemoteNotificationsWithDeviceToken:(nonnull NSData *)deviceToken;

// Invoked from AppDelegate when APNs cannot successfully complete the registration process.
+ (void)didFailToRegisterForRemoteNotificationsWithError:(nonnull NSError *)error;

// Invoked from AppDelegate when a remote notification arrived and there is data to be fetched.
+ (void)didReceiveRemoteNotification:(nonnull NSDictionary *)userInfo
              fetchCompletionHandler:(void (__unused ^_Nonnull)(UIBackgroundFetchResult result))completionHandler;

// Invoked from AppDelegate when a notification arrived while the app was running in the foreground.
+ (void)userNotificationCenter:(nonnull __unused UNUserNotificationCenter *)center
       willPresentNotification:(nonnull UNNotification *)notification
         withCompletionHandler:(void (__unused ^_Nonnull)(UNNotificationPresentationOptions options))completionHandler;

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
        resolver:(nonnull __unused RCTPromiseResolveBlock)resolve
        rejecter:(nonnull RCTPromiseRejectBlock)reject;

// Register template
- (void)registerTemplate:(nonnull NSString *)deviceToken
                  config:(nonnull NSDictionary *)config
                resolver:(nonnull __unused RCTPromiseResolveBlock)resolve
                rejecter:(nonnull RCTPromiseRejectBlock)reject;

// Unregister with Azure Notification Hub
- (void)unregister:(nonnull RCTPromiseResolveBlock)resolve
          rejecter:(nonnull RCTPromiseRejectBlock)reject;

// Unregister template
- (void)unregisterTemplate:(nonnull NSString *)templateName
                  resolver:(nonnull RCTPromiseResolveBlock)resolve
                  rejecter:(nonnull RCTPromiseRejectBlock)reject;

// Set notification handler
- (void)setNotificationHandler:(nonnull RCTAzureNotificationHandler *)handler;

@end
