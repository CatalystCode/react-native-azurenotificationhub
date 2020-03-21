/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#ifndef RCTAzureNotificationHandler_h
#define RCTAzureNotificationHandler_h

#import "React/RCTEventEmitter.h"

@interface RCTAzureNotificationHandler : NSObject

// Class initializer
- (nullable instancetype)initWithEventEmitter:(nonnull RCTEventEmitter *)eventEmitter;

// Handle local notifications
- (void)localNotificationReceived:(nonnull NSNotification *)notification;

// Handle remote notifications
- (void)remoteNotificationReceived:(nonnull NSNotification *)notification;

// Handle successful registration for remote notifications
- (void)remoteNotificationRegistered:(nonnull NSNotification *)notification;

// Handle registration error for remote notifications
- (void)remoteNotificationRegisteredError:(nonnull NSNotification *)notification;

// Handle successful registration for Azure Notification Hub
- (void)azureNotificationHubRegistered:(nonnull NSNotification *)notification;

// Handle registration error for Azure Notification Hub
- (void)azureNotificationHubRegisteredError:(nonnull NSNotification *)notification;

@end

#endif /* RCTAzureNotificationHandler_h */
