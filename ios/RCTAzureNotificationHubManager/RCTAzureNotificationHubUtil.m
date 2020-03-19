/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#import "React/RCTConvert.h"
#import "React/RCTUtils.h"

#import "RCTAzureNotificationHub.h"
#import "RCTAzureNotificationHubUtil.h"

@implementation RCTAzureNotificationHubUtil

// Format UNNotification
+ (nonnull NSDictionary *)formatUNNotification:(nonnull UNNotification *)notification
{
    NSMutableDictionary *formattedNotification = [NSMutableDictionary dictionary];
    UNNotificationContent *content = notification.request.content;

    formattedNotification[@"identifier"] = notification.request.identifier;

    if (notification.date)
    {
        NSDateFormatter *formatter = [NSDateFormatter new];
        [formatter setDateFormat:@"yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ"];
        NSString *dateString = [formatter stringFromDate:notification.date];
        formattedNotification[@"date"] = dateString;
    }

    formattedNotification[@"title"] = RCTNullIfNil(content.title);
    formattedNotification[@"thread-id"] = RCTNullIfNil(content.threadIdentifier);
    formattedNotification[@"alertBody"] = RCTNullIfNil(content.body);
    formattedNotification[@"applicationIconBadgeNumber"] = RCTNullIfNil(content.badge);
    formattedNotification[@"category"] = RCTNullIfNil(content.categoryIdentifier);
    formattedNotification[@"soundName"] = RCTNullIfNil(content.sound);
    formattedNotification[@"userInfo"] = RCTNullIfNil(RCTJSONClean(content.userInfo));

    return formattedNotification;
}

// Format local notification
+ (nonnull NSDictionary *)formatLocalNotification:(nonnull UILocalNotification *)notification
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

// Create Azure Notification Hub
+ (nonnull SBNotificationHub *)createAzureNotificationHub:(nonnull NSString *)connectionString
                                                  hubName:(nonnull NSString *)hubName
{
    SBNotificationHub *hub = [[SBNotificationHub alloc] initWithConnectionString:connectionString
                                                             notificationHubPath:hubName];
    
    return hub;
}

// Convert device token to string
+ (nonnull NSString *)convertDeviceTokenToString:(nonnull NSData *)deviceToken
{
    NSMutableString *hexString = [NSMutableString string];
    NSUInteger deviceTokenLength = deviceToken.length;
    const unsigned char *bytes = deviceToken.bytes;
    for (NSUInteger i = 0; i < deviceTokenLength; i++)
    {
        [hexString appendFormat:@"%02x", bytes[i]];
    }
    
    return hexString;
}

// Get notification types with permissions
+ (UNAuthorizationOptions)getNotificationTypesWithPermissions:(nullable NSDictionary *)permissions
{
    UNAuthorizationOptions types = 0;
    if (permissions)
    {
        if ([RCTConvert BOOL:permissions[RCTNotificationTypeAlert]])
        {
            types |= UNAuthorizationOptionAlert;
        }
        
        if ([RCTConvert BOOL:permissions[RCTNotificationTypeBadge]])
        {
            types |= UNAuthorizationOptionBadge;
        }
        
        if ([RCTConvert BOOL:permissions[RCTNotificationTypeSound]])
        {
            types |= UNAuthorizationOptionSound;
        }
    }
    else
    {
        types = UNAuthorizationOptionAlert | UNAuthorizationOptionBadge | UNAuthorizationOptionSound;
    }
    
    return types;
}

// Run block on the main thread
+ (void)runOnMainThread:(nonnull dispatch_block_t)block
{
    dispatch_async(dispatch_get_main_queue(), block);
}

@end
