/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#ifndef RCTAzureNotificationHubUtil_h
#define RCTAzureNotificationHubUtil_h

#import <WindowsAzureMessaging/WindowsAzureMessaging.h>

@import UserNotifications;

@interface RCTAzureNotificationHubUtil : NSObject

// Format UNNotification
+ (nonnull NSDictionary *)formatUNNotification:(nonnull UNNotification *)notification;

// Format local notification
+ (nonnull NSDictionary *)formatLocalNotification:(nonnull UILocalNotification *)notification;

// Create Azure Notification Hub
+ (nonnull SBNotificationHub *)createAzureNotificationHub:(nonnull NSString *)connectionString
                                                  hubName:(nonnull NSString *)hubName;

// Convert device token to string
+ (nonnull NSString *)convertDeviceTokenToString:(nonnull NSData *)deviceToken;

// Get notification types with permissions
+ (UNAuthorizationOptions)getNotificationTypesWithPermissions:(nullable NSDictionary *)permissions;

// Run block on the main thread
+ (void)runOnMainThread:(nonnull dispatch_block_t)block;

@end

#endif /* RCTAzureNotificationHubUtil_h */
