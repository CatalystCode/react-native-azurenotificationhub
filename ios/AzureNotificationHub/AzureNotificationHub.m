//
//  AzureNotificationHub.m
//  AzureNotificationHub
//
//  Created by Phong Cao on 9/28/16.
//  Copyright © 2016 Microsoft. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <WindowsAzureMessaging/WindowsAzureMessaging.h>
#import "AzureNotificationHub.h"
#import "RCTLog.h"

// The default error code to use as the `code` property for callback error objects
RCT_EXTERN NSString *const RCTErrorUnspecified;

// Notifications registration
NSString *const RCTAppDidRegisterForRemoteNotifications     = @"RCTApplicationDidRegisterForRemoteNotifications";
NSString *const RCTAppDidReceiveRemoteNotification          = @"RCTAppDidReceiveRemoteNotification";
NSString *const RCTDeviceToken                              = @"RCTDeviceToken";
NSString *const RCTRemoteNotificationUserInfo               = @"RCTRemoteNotificationUserInfo";
NSString *const RCTRegistrationFailure                      = @"E_REGISTRATION_FAILED";
NSString *const RCTErrorInvalidArguments                    = @"E_INVALID_ARGUMENTS";

// Keys
NSString *const RCTConnectionStringKey                      = @"connectionString";
NSString *const RCTHubNameKey                               = @"hubName";

@implementation AzureNotificationHub
{
@private
    // The device token
    NSData *_deviceToken;
    
    // The Notification Hub connection string
    NSString *_connectionString;
    
    // The Notification Hub name
    NSString *_hubName;
}

RCT_EXPORT_MODULE();

- (id)init
{
    // Initialize superclass.
    self = [super init];
    
    // Handle errors.
    if (!self)
    {
        return nil;
    }
    
    // Get the default notification center
    NSNotificationCenter * notificationCenter = [NSNotificationCenter defaultCenter];
    
    // Add the notification observers
    [notificationCenter addObserver:self
                           selector:@selector(didRegisterForRemoteNotificationsWithDeviceTokenCallback:)
                               name:RCTAppDidRegisterForRemoteNotifications
                             object:nil];
    
    [notificationCenter addObserver:self
                           selector:@selector(didReceiveRemoteNotificationCallback:)
                               name:RCTAppDidReceiveRemoteNotification
                             object:nil];
    
    // Register the device handle with the Apple Push Notification service
    UIUserNotificationSettings *settings = [UIUserNotificationSettings settingsForTypes:UIUserNotificationTypeSound |
                                                                                        UIUserNotificationTypeAlert |
                                                                                        UIUserNotificationTypeBadge
                                                                             categories:nil];
    [[UIApplication sharedApplication] registerUserNotificationSettings:settings];
    [[UIApplication sharedApplication] registerForRemoteNotifications];
    
    // Done.
    return self;
}

RCT_EXPORT_METHOD(register:(nonnull NSDictionary *)config
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    // Store the connection string and hub name
    _connectionString = [config objectForKey:RCTConnectionStringKey];
    _hubName = [config objectForKey:RCTHubNameKey];
    
    // Check arguments
    if (![self assertArguments:reject])
    {
        return;
    }
    
    // Initialize hub
    SBNotificationHub* hub = [[SBNotificationHub alloc] initWithConnectionString:_connectionString
                                                             notificationHubPath:_hubName];
    
    // Register for native notifications
    dispatch_async(dispatch_get_main_queue(), ^
    {
        [hub registerNativeWithDeviceToken:_deviceToken
                                      tags:nil
                                completion:^(NSError* error)
                                {
                                    if (error != nil)
                                    {
                                        reject(RCTRegistrationFailure, @"Registration was not successful.", nil);
                                    }
                                    else
                                    {
                                        resolve(_deviceToken);
                                    }
                                }];
    });
}

RCT_EXPORT_METHOD(unregister:(RCTPromiseResolveBlock)resolve
                    rejecter:(RCTPromiseRejectBlock)reject)
{
    // Check arguments
    if (![self assertArguments:reject])
    {
        return;
    }
    
    // Initialize hub
    SBNotificationHub* hub = [[SBNotificationHub alloc] initWithConnectionString:_connectionString
                                                             notificationHubPath:_hubName];
    
    // Unregister for native notifications
    dispatch_async(dispatch_get_main_queue(), ^
    {
       [hub unregisterNativeWithCompletion:^(NSError* error)
            {
                if (error != nil)
                {
                    reject(RCTErrorUnspecified, @"Unregister error", error);
                }
                else
                {
                    resolve(@YES);
                }
            }];
    });
}

- (void)didRegisterForRemoteNotificationsWithDeviceTokenCallback:(NSNotification *)notification
{
    RCTLogInfo(@"Device handle registered");
    
    // Save the device token
    _deviceToken = [[notification userInfo] objectForKey:RCTDeviceToken];
}

- (void)didReceiveRemoteNotificationCallback:(NSNotification *)notification
{
    NSDictionary *userInfo = [[notification userInfo] objectForKey:RCTRemoteNotificationUserInfo];
    RCTLogInfo(@"%@", userInfo);
}

- (bool)assertArguments:(RCTPromiseRejectBlock)reject
{
    if (_deviceToken == nil)
    {
        reject(RCTRegistrationFailure, @"The device handle isn't registered with the Apple Push Notification service", nil);
        return false;
    }
    
    if (_connectionString == nil)
    {
        reject(RCTRegistrationFailure, @"Connection string cannot be null.", nil);
        return false;
    }
    
    if (_hubName == nil)
    {
        reject(RCTErrorInvalidArguments, @"Hub name cannot be null.", nil);
        return false;
    }
    
    return true;
}

@end
