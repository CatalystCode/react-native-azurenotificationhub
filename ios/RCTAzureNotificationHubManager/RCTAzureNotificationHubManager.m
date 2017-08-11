/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#import <WindowsAzureMessaging/WindowsAzureMessaging.h>
#import "RCTAzureNotificationHubManager.h"

#import "React/RCTBridge.h"
#import "React/RCTConvert.h"
#import "React/RCTEventDispatcher.h"
#import "React/RCTUtils.h"

// The default error code to use as the `code` property for callback error objects
RCT_EXTERN NSString *const RCTErrorUnspecified;

// Events
NSString *const RCTLocalNotificationReceived                    = @"LocalNotificationReceived";
NSString *const RCTRemoteNotificationReceived                   = @"RemoteNotificationReceived";
NSString *const RCTRemoteNotificationsRegistered                = @"RemoteNotificationsRegistered";
NSString *const RCTAzureNotificationHubRegistered               = @"AzureNotificationHubRegistered";
NSString *const RCTRegisterUserNotificationSettings             = @"RegisterUserNotificationSettings";

// Errors
NSString *const RCTErrorUnableToRequestPermissions              = @"E_UNABLE_TO_REQUEST_PERMISSIONS";
NSString *const RCTErrorRemoteNotificationRegistrationFailed    = @"E_FAILED_TO_REGISTER_FOR_REMOTE_NOTIFICATIONS";
NSString *const RCTErrorAzureNotificationHubRegistrationFailed  = @"E_FAILED_TO_REGISTER_FOR_AZURE_NOTIFICATION_HUB";
NSString *const RCTRegistrationFailure                          = @"E_REGISTRATION_FAILED";
NSString *const RCTErrorInvalidArguments                        = @"E_INVALID_ARGUMENTS";

// Keys
NSString *const RCTConnectionStringKey                          = @"connectionString";
NSString *const RCTHubNameKey                                   = @"hubName";
NSString *const RCTTagsKey                                      = @"tags";

@implementation RCTConvert (UILocalNotification)

+ (UILocalNotification *)UILocalNotification:(id)json
{
    NSDictionary<NSString *, id> *details = [self NSDictionary:json];
    UILocalNotification *notification = [UILocalNotification new];
    notification.fireDate = [RCTConvert NSDate:details[@"fireDate"]] ?: [NSDate date];
    notification.alertBody = [RCTConvert NSString:details[@"alertBody"]];
    notification.alertAction = [RCTConvert NSString:details[@"alertAction"]];
    notification.soundName = [RCTConvert NSString:details[@"soundName"]] ?: UILocalNotificationDefaultSoundName;
    notification.userInfo = [RCTConvert NSDictionary:details[@"userInfo"]];
    notification.category = [RCTConvert NSString:details[@"category"]];
    
    if (details[@"applicationIconBadgeNumber"])
    {
        notification.applicationIconBadgeNumber = [RCTConvert NSInteger:details[@"applicationIconBadgeNumber"]];
    }
    
    return notification;
}

@end

@implementation RCTAzureNotificationHubManager
{
@private
    RCTPromiseResolveBlock _requestPermissionsResolveBlock;
    
    // The Notification Hub connection string
    NSString *_connectionString;
    
    // The Notification Hub name
    NSString *_hubName;
    
    // The Notification Hub tags
    NSSet *_tags;
}

static NSDictionary *RCTFormatLocalNotification(UILocalNotification *notification)
{
    NSMutableDictionary *formattedLocalNotification = [NSMutableDictionary dictionary];
    if (notification.fireDate)
    {
        NSDateFormatter *formatter = [NSDateFormatter new];
        [formatter setDateFormat:@"yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ"];
        NSString *fireDateString = [formatter stringFromDate:notification.fireDate];
        formattedLocalNotification[@"fireDate"] = fireDateString;
    }
    
    formattedLocalNotification[@"alertAction"] = RCTNullIfNil(notification.alertAction);
    formattedLocalNotification[@"alertBody"] = RCTNullIfNil(notification.alertBody);
    formattedLocalNotification[@"applicationIconBadgeNumber"] = @(notification.applicationIconBadgeNumber);
    formattedLocalNotification[@"category"] = RCTNullIfNil(notification.category);
    formattedLocalNotification[@"soundName"] = RCTNullIfNil(notification.soundName);
    formattedLocalNotification[@"userInfo"] = RCTNullIfNil(RCTJSONClean(notification.userInfo));
    formattedLocalNotification[@"remote"] = @NO;
    return formattedLocalNotification;
}

RCT_EXPORT_MODULE()

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

- (void)startObserving
{
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(handleLocalNotificationReceived:)
                                                 name:RCTLocalNotificationReceived
                                               object:nil];
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(handleRemoteNotificationReceived:)
                                                 name:RCTRemoteNotificationReceived
                                               object:nil];
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(handleRemoteNotificationsRegistered:)
                                                 name:RCTRemoteNotificationsRegistered
                                               object:nil];
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(handleRemoteNotificationRegistrationError:)
                                                 name:RCTErrorRemoteNotificationRegistrationFailed
                                               object:nil];
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(handleAzureNotificationHubRegistered:)
                                                 name:RCTAzureNotificationHubRegistered
                                               object:nil];
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(handleAzureNotificationHubRegistrationError:)
                                                 name:RCTErrorAzureNotificationHubRegistrationFailed
                                               object:nil];
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(handleRegisterUserNotificationSettings:)
                                                 name:RCTRegisterUserNotificationSettings
                                               object:nil];
}

- (void)stopObserving
{
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

- (NSArray<NSString *> *)supportedEvents
{
    return @[@"localNotificationReceived",
             @"remoteNotificationReceived",
             @"remoteNotificationsRegistered",
             @"remoteNotificationRegistrationError",
             @"azureNotificationHubRegistered",
             @"azureNotificationHubRegistrationError"];
}

+ (void)didRegisterUserNotificationSettings:(__unused UIUserNotificationSettings *)notificationSettings
{
    if ([UIApplication instancesRespondToSelector:@selector(registerForRemoteNotifications)])
    {
        [[UIApplication sharedApplication] registerForRemoteNotifications];
        [[NSNotificationCenter defaultCenter] postNotificationName:RCTRegisterUserNotificationSettings
                                                            object:self
                                                          userInfo:@{@"notificationSettings": notificationSettings}];
    }
}

+ (void)didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken
{
    NSMutableString *hexString = [NSMutableString string];
    NSUInteger deviceTokenLength = deviceToken.length;
    const unsigned char *bytes = deviceToken.bytes;
    for (NSUInteger i = 0; i < deviceTokenLength; i++)
    {
        [hexString appendFormat:@"%02x", bytes[i]];
    }
    
    [[NSNotificationCenter defaultCenter] postNotificationName:RCTRemoteNotificationsRegistered
                                                        object:self
                                                      userInfo:@{@"deviceToken" : [hexString copy]}];
}

+ (void)didFailToRegisterForRemoteNotificationsWithError:(NSError *)error
{
    [[NSNotificationCenter defaultCenter] postNotificationName:RCTErrorRemoteNotificationRegistrationFailed
                                                        object:self
                                                      userInfo:@{@"error": error}];
}

+ (void)didReceiveRemoteNotification:(NSDictionary *)notification
{
    [[NSNotificationCenter defaultCenter] postNotificationName:RCTRemoteNotificationReceived
                                                        object:self
                                                      userInfo:notification];
}

+ (void)didReceiveLocalNotification:(UILocalNotification *)notification
{
    [[NSNotificationCenter defaultCenter] postNotificationName:RCTLocalNotificationReceived
                                                        object:self
                                                      userInfo:RCTFormatLocalNotification(notification)];
}

- (void)handleLocalNotificationReceived:(NSNotification *)notification
{
    [self sendEventWithName:@"localNotificationReceived"
                       body:notification.userInfo];
}

- (void)handleRemoteNotificationReceived:(NSNotification *)notification
{
    NSMutableDictionary *userInfo = [notification.userInfo mutableCopy];
    userInfo[@"remote"] = @YES;
    [self sendEventWithName:@"remoteNotificationReceived"
                       body:userInfo];
}

- (void)handleRemoteNotificationsRegistered:(NSNotification *)notification
{
    [self sendEventWithName:@"remoteNotificationsRegistered"
                       body:notification.userInfo];
}

- (void)handleRemoteNotificationRegistrationError:(NSNotification *)notification
{
    NSError *error = notification.userInfo[@"error"];
    NSDictionary *errorDetails = @{
                                    @"message": error.localizedDescription,
                                    @"code": @(error.code),
                                    @"details": error.userInfo,
                                  };
    [self sendEventWithName:@"remoteNotificationRegistrationError"
                       body:errorDetails];
}

- (void)handleAzureNotificationHubRegistered:(NSNotification *)notification
{
    [self sendEventWithName:@"azureNotificationHubRegistered"
                       body:notification.userInfo];
}

- (void)handleAzureNotificationHubRegistrationError:(NSNotification *)notification
{
    NSError *error = notification.userInfo[@"error"];
    NSDictionary *errorDetails = @{
                                    @"message": error.localizedDescription,
                                    @"code": @(error.code),
                                    @"details": error.userInfo,
                                  };
    
    [self sendEventWithName:@"azureNotificationHubRegistrationError"
                       body:errorDetails];
}

- (void)handleRegisterUserNotificationSettings:(NSNotification *)notification
{
    if (_requestPermissionsResolveBlock == nil)
    {
        return;
    }
    
    UIUserNotificationSettings *notificationSettings = notification.userInfo[@"notificationSettings"];
    NSDictionary *notificationTypes = @{
                                         @"alert": @((notificationSettings.types & UIUserNotificationTypeAlert) > 0),
                                         @"sound": @((notificationSettings.types & UIUserNotificationTypeSound) > 0),
                                         @"badge": @((notificationSettings.types & UIUserNotificationTypeBadge) > 0),
                                       };
    
    _requestPermissionsResolveBlock(notificationTypes);
    _requestPermissionsResolveBlock = nil;
}

/**
 * Update the application icon badge number on the home screen
 */
RCT_EXPORT_METHOD(setApplicationIconBadgeNumber:(NSInteger)number)
{
    RCTSharedApplication().applicationIconBadgeNumber = number;
}

/**
 * Get the current application icon badge number on the home screen
 */
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
        reject(RCTErrorUnableToRequestPermissions, nil, RCTErrorWithMessage(@"Requesting push notifications is currently unavailable in an app extension"));
        return;
    }
    
    if (_requestPermissionsResolveBlock != nil)
    {
        RCTLogError(@"Cannot call requestPermissions twice before the first has returned.");
        return;
    }
    
    _requestPermissionsResolveBlock = resolve;
    
    UIUserNotificationType types = UIUserNotificationTypeNone;
    if (permissions)
    {
        if ([RCTConvert BOOL:permissions[@"alert"]])
        {
            types |= UIUserNotificationTypeAlert;
        }
        
        if ([RCTConvert BOOL:permissions[@"badge"]])
        {
            types |= UIUserNotificationTypeBadge;
        }
        
        if ([RCTConvert BOOL:permissions[@"sound"]])
        {
            types |= UIUserNotificationTypeSound;
        }
    }
    else
    {
        types = UIUserNotificationTypeAlert | UIUserNotificationTypeBadge | UIUserNotificationTypeSound;
    }
    
    UIApplication *app = RCTSharedApplication();
    if ([app respondsToSelector:@selector(registerUserNotificationSettings:)])
    {
        UIUserNotificationSettings *notificationSettings =
        [UIUserNotificationSettings settingsForTypes:(NSUInteger)types categories:nil];
        [app registerUserNotificationSettings:notificationSettings];
    }
    else
    {
        [app registerForRemoteNotificationTypes:(NSUInteger)types];
    }
}

RCT_EXPORT_METHOD(abandonPermissions)
{
    [RCTSharedApplication() unregisterForRemoteNotifications];
}

RCT_EXPORT_METHOD(checkPermissions:(RCTResponseSenderBlock)callback)
{
    if (RCTRunningInAppExtension())
    {
        callback(@[@{@"alert": @NO, @"badge": @NO, @"sound": @NO}]);
        return;
    }
    
    NSUInteger types = [RCTSharedApplication() currentUserNotificationSettings].types;
    callback(@[@{
                   @"alert": @((types & UIUserNotificationTypeAlert) > 0),
                   @"badge": @((types & UIUserNotificationTypeBadge) > 0),
                   @"sound": @((types & UIUserNotificationTypeSound) > 0),
                }]);
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
        initialNotification[@"remote"] = @YES;
        resolve(initialNotification);
    }
    else if (initialLocalNotification)
    {
        resolve(RCTFormatLocalNotification(initialLocalNotification));
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
        [formattedScheduledLocalNotifications addObject:RCTFormatLocalNotification(notification)];
    }
    
    callback(@[formattedScheduledLocalNotifications]);
}

RCT_EXPORT_METHOD(register:(nonnull NSString *)deviceToken
                    config:(nonnull NSDictionary *)config
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    // Store the connection string, hub name and tags
    _connectionString = [config objectForKey:RCTConnectionStringKey];
    _hubName = [config objectForKey:RCTHubNameKey];
    _tags = [config objectForKey:RCTTagsKey];

    // Check arguments
    if (![self assertArguments:reject])
    {
        return;
    }
    
    // Initialize hub
    SBNotificationHub *hub = [[SBNotificationHub alloc] initWithConnectionString:_connectionString
                                                             notificationHubPath:_hubName];
    
    // Register for native notifications
    dispatch_async(dispatch_get_main_queue(), ^
    {
       [hub registerNativeWithDeviceToken:deviceToken
                                     tags:_tags
                               completion:^(NSError* error)
        {
            if (error != nil)
            {
                [[NSNotificationCenter defaultCenter] postNotificationName:RCTErrorAzureNotificationHubRegistrationFailed
                                                                    object:self
                                                                  userInfo:@{@"error": error}];
            }
            else
            {
                [[NSNotificationCenter defaultCenter] postNotificationName:RCTAzureNotificationHubRegistered
                                                                    object:self
                                                                  userInfo:@{@"success": @YES}];
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
    SBNotificationHub *hub = [[SBNotificationHub alloc] initWithConnectionString:_connectionString
                                                             notificationHubPath:_hubName];
    
    // Unregister for native notifications
    dispatch_async(dispatch_get_main_queue(), ^
    {
       [hub unregisterNativeWithCompletion:^(NSError *error)
        {
            if (error != nil)
            {
                [[NSNotificationCenter defaultCenter] postNotificationName:RCTErrorUnspecified
                                                                    object:self
                                                                  userInfo:@{@"error": error}];
            }
        }];
    });
}

- (bool)assertArguments:(RCTPromiseRejectBlock)reject
{
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
