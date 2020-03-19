/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#import "RCTAzureNotificationHub.h"
#import "RCTAzureNotificationHandler.h"

@implementation RCTAzureNotificationHandler
{
@private
    RCTEventEmitter *_eventEmitter;
}

// Class initializer
- (nullable instancetype)initWithEventEmitter:(nonnull RCTEventEmitter *)eventEmitter
{
    // Initialize superclass.
    self = [super init];
    
    // Handle errors.
    if (!self)
    {
        return nil;
    }
    
    _eventEmitter = eventEmitter;
    
    return self;
}

// Handle local notifications
- (void)localNotificationReceived:(nonnull NSNotification *)notification
{
    [_eventEmitter sendEventWithName:RCTLocalNotificationReceived
                                body:notification.userInfo];
}

// Handle remote notifications
- (void)remoteNotificationReceived:(nonnull NSNotification *)notification
{
    NSMutableDictionary *userInfo = [notification.userInfo mutableCopy];
    userInfo[RCTUserInfoRemote] = @YES;
    [_eventEmitter sendEventWithName:RCTRemoteNotificationReceived
                                body:userInfo];
}

// Handle successful registration for remote notifications
- (void)remoteNotificationRegistered:(nonnull NSNotification *)notification
{
    [_eventEmitter sendEventWithName:RCTRemoteNotificationRegistered
                                body:notification.userInfo];
}

// Handle registration error for remote notifications
- (void)remoteNotificationRegisteredError:(nonnull NSNotification *)notification
{
    NSError *error = notification.userInfo[RCTUserInfoError];
    NSDictionary *errorDetails = @{
        @"message": error.localizedDescription,
        @"code": @(error.code),
        @"details": error.userInfo,
    };
    [_eventEmitter sendEventWithName:RCTRemoteNotificationRegisteredError
                                body:errorDetails];
}

// Handle successful registration for Azure Notification Hub
- (void)azureNotificationHubRegistered:(nonnull NSNotification *)notification
{
    [_eventEmitter sendEventWithName:RCTAzureNotificationHubRegistered
                                body:notification.userInfo];
}

// Handle registration error for Azure Notification Hub
- (void)azureNotificationHubRegisteredError:(nonnull NSNotification *)notification
{
    NSError *error = notification.userInfo[RCTUserInfoError];
    NSDictionary *errorDetails = @{
        @"message": error.localizedDescription,
        @"code": @(error.code),
        @"details": error.userInfo,
    };
    
    [_eventEmitter sendEventWithName:RCTAzureNotificationHubRegisteredError
                                body:errorDetails];
}

@end
