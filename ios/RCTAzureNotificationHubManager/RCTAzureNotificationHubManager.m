/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#import <WindowsAzureMessaging/WindowsAzureMessaging.h>

#import "React/RCTBridge.h"
#import "React/RCTConvert.h"
#import "React/RCTEventDispatcher.h"
#import "React/RCTUtils.h"

#import "RCTAzureNotificationHub.h"
#import "RCTAzureNotificationHubManager.h"
#import "RCTAzureNotificationHubUtil.h"

// The default error code to use as the `code` property for callback error objects
RCT_EXTERN NSString *const RCTErrorUnspecified;

static RCTAzureNotificationHandler *notificationHandler;

@implementation RCTAzureNotificationHubManager
{
@private
    // The Notification Hub connection string
    NSString *_connectionString;

    // The Notification Hub name
    NSString *_hubName;

    // The Notification Hub tags
    NSSet *_tags;

    // The template name
    NSString *_templateName;

    // The template JSON blob
    NSString *_template;

    // The Promise resolve block
    RCTPromiseResolveBlock _requestPermissionsResolveBlock;

    // The Promise reject block
    RCTPromiseRejectBlock _requestPermissionsRejectBlock;
}

RCT_EXPORT_MODULE()

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

- (NSArray<NSString *> *)supportedEvents
{
    return @[RCTLocalNotificationReceived,
             RCTRemoteNotificationReceived,
             RCTRemoteNotificationRegistered,
             RCTRemoteNotificationRegisteredError,
             RCTAzureNotificationHubRegistered,
             RCTAzureNotificationHubRegisteredError];
}

- (void)startObserving
{
    // Initialize notification handler
    notificationHandler = [[RCTAzureNotificationHandler alloc] initWithEventEmitter:self];
    
    [[NSNotificationCenter defaultCenter] addObserver:notificationHandler
                                             selector:@selector(localNotificationReceived:)
                                                 name:RCTLocalNotificationReceived
                                               object:nil];
    
    [[NSNotificationCenter defaultCenter] addObserver:notificationHandler
                                             selector:@selector(remoteNotificationReceived:)
                                                 name:RCTRemoteNotificationReceived
                                               object:nil];
    
    [[NSNotificationCenter defaultCenter] addObserver:notificationHandler
                                             selector:@selector(remoteNotificationRegistered:)
                                                 name:RCTRemoteNotificationRegistered
                                               object:nil];
    
    [[NSNotificationCenter defaultCenter] addObserver:notificationHandler
                                             selector:@selector(remoteNotificationRegisteredError:)
                                                 name:RCTRemoteNotificationRegisteredError
                                               object:nil];
    
    [[NSNotificationCenter defaultCenter] addObserver:notificationHandler
                                             selector:@selector(azureNotificationHubRegistered:)
                                                 name:RCTAzureNotificationHubRegistered
                                               object:nil];
    
    [[NSNotificationCenter defaultCenter] addObserver:notificationHandler
                                             selector:@selector(azureNotificationHubRegisteredError:)
                                                 name:RCTAzureNotificationHubRegisteredError
                                               object:nil];
}

- (void)stopObserving
{
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

// Set notification handler
- (void)setNotificationHandler:(nonnull RCTAzureNotificationHandler *)handler
{
    notificationHandler = handler;
}

+ (void)didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken
{
    NSString *hexString = [RCTAzureNotificationHubUtil convertDeviceTokenToString:deviceToken];
    [[NSNotificationCenter defaultCenter] postNotificationName:RCTRemoteNotificationRegistered
                                                        object:notificationHandler
                                                      userInfo:@{RCTUserInfoDeviceToken: [hexString copy]}];
}

+ (void)didFailToRegisterForRemoteNotificationsWithError:(NSError *)error
{
    [[NSNotificationCenter defaultCenter] postNotificationName:RCTRemoteNotificationRegisteredError
                                                        object:notificationHandler
                                                      userInfo:@{RCTUserInfoError: error}];
}

+ (void)didReceiveRemoteNotification:(nonnull NSDictionary *)userInfo
              fetchCompletionHandler:(void (__unused ^_Nonnull)(UIBackgroundFetchResult result))completionHandler
{
    [[NSNotificationCenter defaultCenter] postNotificationName:RCTRemoteNotificationReceived
                                                        object:notificationHandler
                                                      userInfo:userInfo];
}

+ (void)userNotificationCenter:(nonnull __unused UNUserNotificationCenter *)center
       willPresentNotification:(nonnull UNNotification *)notification
         withCompletionHandler:(void (__unused ^_Nonnull)(UNNotificationPresentationOptions options))completionHandler
{
    [[NSNotificationCenter defaultCenter] postNotificationName:RCTLocalNotificationReceived
                                                        object:notificationHandler
                                                      userInfo:[RCTAzureNotificationHubUtil formatUNNotification:notification]];
}

// Update the application icon badge number on the home screen
RCT_EXPORT_METHOD(setApplicationIconBadgeNumber:(NSInteger)number)
{
    RCTSharedApplication().applicationIconBadgeNumber = number;
}

// Get the current application icon badge number on the home screen
RCT_EXPORT_METHOD(getApplicationIconBadgeNumber:(RCTResponseSenderBlock)callback)
{
    callback(@[@(RCTSharedApplication().applicationIconBadgeNumber)]);
}

RCT_EXPORT_METHOD(requestPermissions:(NSDictionary *)permissions
                            resolver:(RCTPromiseResolveBlock)resolve
                            rejecter:(RCTPromiseRejectBlock)reject)
{
    if (RCTRunningInAppExtension())
    {
        reject(RCTErrorUnableToRequestPermissions, nil, RCTErrorWithMessage(RCTErrorUnableToRequestPermissionsAppExt));
        return;
    }

    if (_requestPermissionsResolveBlock != nil)
    {
        RCTLogError(@"%@", RCTErrorUnableToRequestPermissionsTwice);
        return;
    }

    _requestPermissionsResolveBlock = resolve;
    _requestPermissionsRejectBlock = reject;

    UNAuthorizationOptions options = [RCTAzureNotificationHubUtil getNotificationTypesWithPermissions:permissions];
    UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
    [center requestAuthorizationWithOptions:options
                          completionHandler:^(BOOL granted, NSError * _Nullable error) {
        if (error != nil)
        {
            reject(RCTErrorUnableToRequestPermissions, nil, RCTErrorWithMessage(error.localizedDescription));
        }
        else if (!granted)
        {
            reject(RCTErrorUnableToRequestPermissions, nil, RCTErrorWithMessage(RCTErrorUnableToRequestPermissionsUserReject));
        }
        else
        {
            [RCTAzureNotificationHubUtil runOnMainThread:^{
                [[UIApplication sharedApplication] registerForRemoteNotifications];
            }];

            NSDictionary *notificationTypes = @{
                RCTNotificationTypeAlert: @((options & UNAuthorizationOptionAlert) > 0),
                RCTNotificationTypeSound: @((options & UNAuthorizationOptionSound) > 0),
                RCTNotificationTypeBadge: @((options & UNAuthorizationOptionBadge) > 0),
            };

            resolve(notificationTypes);
        }

        _requestPermissionsResolveBlock = nil;
        _requestPermissionsRejectBlock = nil;
    }];
}

RCT_EXPORT_METHOD(abandonPermissions)
{
    [RCTSharedApplication() unregisterForRemoteNotifications];
}

RCT_EXPORT_METHOD(checkPermissions:(RCTResponseSenderBlock)callback)
{
    if (RCTRunningInAppExtension())
    {
        callback(@[@{RCTNotificationTypeAlert: @NO, RCTNotificationTypeBadge: @NO, RCTNotificationTypeSound: @NO}]);
        return;
    }

    UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];

    [center getNotificationSettingsWithCompletionHandler:^(UNNotificationSettings * _Nonnull settings) {
        callback(@[@{
                       RCTNotificationTypeAlert: @(settings.notificationCenterSetting == UNNotificationSettingEnabled
                           || settings.lockScreenSetting == UNNotificationSettingEnabled
                           || settings.alertSetting == UNNotificationSettingEnabled),
                       RCTNotificationTypeBadge: @(settings.badgeSetting == UNNotificationSettingEnabled),
                       RCTNotificationTypeSound: @(settings.soundSetting == UNNotificationSettingEnabled)
        }]);
    }];
}

RCT_EXPORT_METHOD(presentLocalNotification:(UILocalNotification *)notification)
{
    [RCTSharedApplication() presentLocalNotificationNow:notification];
}

RCT_EXPORT_METHOD(scheduleLocalNotification:(UILocalNotification *)notification)
{
    [RCTSharedApplication() scheduleLocalNotification:notification];
}

RCT_EXPORT_METHOD(cancelAllLocalNotifications)
{
    [RCTSharedApplication() cancelAllLocalNotifications];
}

RCT_EXPORT_METHOD(cancelLocalNotifications:(NSDictionary<NSString *, id> *)userInfo)
{
    for (UILocalNotification *notification in [UIApplication sharedApplication].scheduledLocalNotifications)
    {
        __block BOOL matchesAll = YES;
        NSDictionary<NSString *, id> *notificationInfo = notification.userInfo;
        // Note: we do this with a loop instead of just `isEqualToDictionary:`
        // because we only require that all specified userInfo values match the
        // notificationInfo values - notificationInfo may contain additional values
        // which we don't care about.
        [userInfo enumerateKeysAndObjectsUsingBlock:^(NSString *key, id obj, BOOL *stop)
        {
            if (![notificationInfo[key] isEqual:obj])
            {
                matchesAll = NO;
                *stop = YES;
            }
        }];
        
        if (matchesAll)
        {
            [[UIApplication sharedApplication] cancelLocalNotification:notification];
        }
    }
}

RCT_EXPORT_METHOD(getInitialNotification:(RCTPromiseResolveBlock)resolve
                                  reject:(__unused RCTPromiseRejectBlock)reject)
{
    NSMutableDictionary<NSString *, id> *initialNotification =
    [self.bridge.launchOptions[UIApplicationLaunchOptionsRemoteNotificationKey] mutableCopy];
    
    UILocalNotification *initialLocalNotification =
    self.bridge.launchOptions[UIApplicationLaunchOptionsLocalNotificationKey];
    
    if (initialNotification)
    {
        initialNotification[RCTUserInfoRemote] = @YES;
        resolve(initialNotification);
    }
    else if (initialLocalNotification)
    {
        resolve([RCTAzureNotificationHubUtil formatLocalNotification:initialLocalNotification]);
    }
    else
    {
        resolve((id)kCFNull);
    }
}

RCT_EXPORT_METHOD(getScheduledLocalNotifications:(RCTResponseSenderBlock)callback)
{
    NSArray<UILocalNotification *> *scheduledLocalNotifications = [UIApplication sharedApplication].scheduledLocalNotifications;
    NSMutableArray<NSDictionary *> *formattedScheduledLocalNotifications = [NSMutableArray new];
    for (UILocalNotification *notification in scheduledLocalNotifications)
    {
        [formattedScheduledLocalNotifications addObject:[RCTAzureNotificationHubUtil formatLocalNotification:notification]];
    }
    
    callback(@[formattedScheduledLocalNotifications]);
}

RCT_EXPORT_METHOD(register:(nonnull NSString *)deviceToken
                    config:(nonnull NSDictionary *)config
                  resolver:(nonnull __unused RCTPromiseResolveBlock)resolve
                  rejecter:(nonnull RCTPromiseRejectBlock)reject)
{
    // Store the connection string, hub name and tags
    _connectionString = [config objectForKey:RCTConnectionStringKey];
    _hubName = [config objectForKey:RCTHubNameKey];
    _tags = [config objectForKey:RCTTagsKey];

    // Check arguments
    if (_connectionString == nil)
    {
        reject(RCTErrorInvalidArguments, RCTErrorMissingConnectionString, nil);
        return;
    }

    if (_hubName == nil)
    {
        reject(RCTErrorInvalidArguments, RCTErrorMissingHubName, nil);
        return;
    }

    // Initialize hub
    SBNotificationHub *hub = [RCTAzureNotificationHubUtil createAzureNotificationHub:_connectionString
                                                                             hubName:_hubName];

    // Register for native notifications
    [RCTAzureNotificationHubUtil runOnMainThread:^
    {
        [hub registerNativeWithDeviceToken:deviceToken
                                      tags:_tags
                                completion:^(NSError* error)
        {
            if (error != nil)
            {
                [[NSNotificationCenter defaultCenter] postNotificationName:RCTAzureNotificationHubRegisteredError
                                                                    object:notificationHandler
                                                                  userInfo:@{RCTUserInfoError: error}];
            }
            else
            {
                [[NSNotificationCenter defaultCenter] postNotificationName:RCTAzureNotificationHubRegistered
                                                                    object:notificationHandler
                                                                  userInfo:@{RCTUserInfoSuccess: @YES}];
            }
        }];
    }];
}

RCT_EXPORT_METHOD(registerTemplate:(nonnull NSString *)deviceToken
                            config:(nonnull NSDictionary *)config
                          resolver:(nonnull __unused RCTPromiseResolveBlock)resolve
                          rejecter:(nonnull RCTPromiseRejectBlock)reject)
{
    // Store the connection string, hub name, tags, template name and template
    _connectionString = [config objectForKey:RCTConnectionStringKey];
    _hubName = [config objectForKey:RCTHubNameKey];
    _tags = [config objectForKey:RCTTagsKey];
    _templateName = [config objectForKey:RCTTemplateNameKey];
    _template = [config objectForKey:RCTTemplateKey];

    // Check arguments
    if (_connectionString == nil)
    {
        reject(RCTErrorInvalidArguments, RCTErrorMissingConnectionString, nil);
        return;
    }

    if (_hubName == nil)
    {
        reject(RCTErrorInvalidArguments, RCTErrorMissingHubName, nil);
        return;
    }

    if (_templateName == nil)
    {
        reject(RCTErrorInvalidArguments, RCTErrorMissingTemplateName, nil);
        return;
    }

    if (_template == nil)
    {
        reject(RCTErrorInvalidArguments, RCTErrorMissingTemplate, nil);
        return;
    }

    // Initialize hub
    SBNotificationHub *hub = [RCTAzureNotificationHubUtil createAzureNotificationHub:_connectionString
                                                                             hubName:_hubName];

    // Register for native notifications
    [RCTAzureNotificationHubUtil runOnMainThread:^
    {
        [hub registerTemplateWithDeviceToken:deviceToken
                                        name:_templateName
                            jsonBodyTemplate:_template
                              expiryTemplate:nil
                                        tags:_tags
                                  completion:^(NSError* error)
         {
            if (error != nil)
            {
                [[NSNotificationCenter defaultCenter] postNotificationName:RCTAzureNotificationHubRegisteredError
                                                                    object:notificationHandler
                                                                  userInfo:@{RCTUserInfoError: error}];
            }
            else
            {
                [[NSNotificationCenter defaultCenter] postNotificationName:RCTAzureNotificationHubRegistered
                                                                    object:notificationHandler
                                                                  userInfo:@{RCTUserInfoSuccess: @YES}];
            }
        }];
    }];
}

RCT_EXPORT_METHOD(unregister:(nonnull RCTPromiseResolveBlock)resolve
                    rejecter:(nonnull RCTPromiseRejectBlock)reject)
{
    // Check arguments
    if (_connectionString == nil || _hubName == nil)
    {
        reject(RCTErrorUnableToUnregister, RCTErrorUnableToUnregisterNoRegistration, nil);
        return;
    }

    // Initialize hub
    SBNotificationHub *hub = [RCTAzureNotificationHubUtil createAzureNotificationHub:_connectionString
                                                                             hubName:_hubName];

    // Unregister for native notifications
    [RCTAzureNotificationHubUtil runOnMainThread:^
    {
        [hub unregisterNativeWithCompletion:^(NSError *error)
        {
            if (error != nil)
            {
                [[NSNotificationCenter defaultCenter] postNotificationName:RCTErrorUnspecified
                                                                    object:notificationHandler
                                                                  userInfo:@{RCTUserInfoError: error}];

                reject(RCTErrorUnableToUnregister, [error localizedDescription], nil);
            }
            else
            {
                _connectionString = nil;
                resolve(RCTPromiseResolveUnregiseredSuccessfully);
            }
        }];
    }];
}

RCT_EXPORT_METHOD(unregisterTemplate:(nonnull NSString *)templateName
                            resolver:(nonnull RCTPromiseResolveBlock)resolve
                            rejecter:(nonnull RCTPromiseRejectBlock)reject)
{
    // Check arguments
    if (_connectionString == nil || _hubName == nil || _template == nil)
    {
        reject(RCTErrorUnableToUnregister, RCTErrorUnableToUnregisterNoRegistration, nil);
        return;
    }

    // Initialize hub
    SBNotificationHub *hub = [RCTAzureNotificationHubUtil createAzureNotificationHub:_connectionString
                                                                             hubName:_hubName];

    // Unregister for native notifications
    [RCTAzureNotificationHubUtil runOnMainThread:^
    {
        [hub unregisterTemplateWithName:templateName completion:^(NSError *error)
         {
            if (error != nil)
            {
                [[NSNotificationCenter defaultCenter] postNotificationName:RCTErrorUnspecified
                                                                    object:notificationHandler
                                                                  userInfo:@{RCTUserInfoError: error}];

                reject(RCTErrorUnableToUnregister, [error localizedDescription], nil);
            }
            else
            {
                resolve(RCTPromiseResolveUnregiseredSuccessfully);
            }
        }];
    }];
}

@end
