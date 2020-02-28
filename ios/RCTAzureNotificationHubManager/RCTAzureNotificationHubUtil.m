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
+ (UIUserNotificationType)getNotificationTypesWithPermissions:(nullable NSDictionary *)permissions
{
    UIUserNotificationType types = UIUserNotificationTypeNone;
    if (permissions)
    {
        if ([RCTConvert BOOL:permissions[RCTNotificationTypeAlert]])
        {
            types |= UIUserNotificationTypeAlert;
        }
        
        if ([RCTConvert BOOL:permissions[RCTNotificationTypeBadge]])
        {
            types |= UIUserNotificationTypeBadge;
        }
        
        if ([RCTConvert BOOL:permissions[RCTNotificationTypeSound]])
        {
            types |= UIUserNotificationTypeSound;
        }
    }
    else
    {
        types = UIUserNotificationTypeAlert | UIUserNotificationTypeBadge | UIUserNotificationTypeSound;
    }
    
    return types;
}

// Run block on the main thread
+ (void)runOnMainThread:(dispatch_block_t)block
{
    dispatch_async(dispatch_get_main_queue(), block);
}

@end
