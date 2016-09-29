//
//  AzureNotificationHub.h
//  AzureNotificationHub
//
//  Created by Phong Cao on 9/28/16.
//  Copyright © 2016 Microsoft. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "RCTBridgeModule.h"

RCT_EXTERN NSString *const RCTAppDidRegisterForRemoteNotifications;
RCT_EXTERN NSString *const RCTAppDidReceiveRemoteNotification;
RCT_EXTERN NSString *const RCTDeviceToken;
RCT_EXTERN NSString *const RCTRemoteNotificationUserInfo;

@interface AzureNotificationHub : NSObject <RCTBridgeModule>

@end
