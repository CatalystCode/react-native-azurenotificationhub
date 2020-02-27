//
//  RCTAzureNotificationHubUtil.h
//  Pods
//
//  Created by Phong Cao on 2/27/20.
//

#ifndef RCTAzureNotificationHubUtil_h
#define RCTAzureNotificationHubUtil_h

#import <WindowsAzureMessaging/WindowsAzureMessaging.h>

@interface RCTAzureNotificationHubUtil : NSObject

// Format local notification
+ (nonnull NSDictionary *)formatLocalNotification:(nonnull UILocalNotification *)notification;

// Create Azure Notification Hub
+ (nonnull SBNotificationHub *)createAzureNotificationHub:(nonnull NSString *)connectionString
                                                  hubName:(nonnull NSString *)hubName;

// Convert device token to string
+ (nonnull NSString *)convertDeviceTokenToString:(nonnull NSData *)deviceToken;

// Get notification types with permissions
+ (UIUserNotificationType)getNotificationTypesWithPermissions:(nullable NSDictionary *)permissions;

@end

#endif /* RCTAzureNotificationHubUtil_h */
