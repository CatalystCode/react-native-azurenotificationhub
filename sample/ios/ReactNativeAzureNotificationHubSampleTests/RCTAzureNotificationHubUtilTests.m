/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#import <XCTest/XCTest.h>
#import <WindowsAzureMessaging/WindowsAzureMessaging.h>

#import <RNAzureNotificationHub/RCTAzureNotificationHub.h>
#import <RNAzureNotificationHub/RCTAzureNotificationHubUtil.h>

@interface RCTAzureNotificationHubUtilTests : XCTestCase
@end

@implementation RCTAzureNotificationHubUtilTests

- (void)testFormatLocalNotification
{
    UILocalNotification *notification = [[UILocalNotification alloc] init];
    NSArray *keys = [NSArray arrayWithObjects:@"userInfoKey", nil];
    NSArray *objects = [NSArray arrayWithObjects:@"userInfoObject", nil];
    NSDictionary *info = [[NSDictionary alloc] initWithObjects:objects forKeys:keys];
    notification.userInfo = info;
    notification.alertAction = @"alertAction";
    notification.alertBody = @"alertBody";
    notification.applicationIconBadgeNumber = 1;
    notification.category = @"category";
    notification.soundName = @"soundName";
    NSDictionary *expectedNotification = @{
        @"userInfo": @{ @"userInfoKey": @"userInfoObject" },
        @"alertAction": @"alertAction",
        @"alertBody": @"alertBody",
        @"applicationIconBadgeNumber": [NSNumber numberWithInt:1],
        @"category": @"category",
        @"soundName": @"soundName",
        @"remote": @NO
    };
    
    NSDictionary *formattedNotification = [RCTAzureNotificationHubUtil formatLocalNotification:notification];
    
    XCTAssertEqualObjects(formattedNotification, expectedNotification);
}

- (void)testConvertDeviceTokenToString
{
    NSString *deviceToken = @"Device Token";
    NSData *deviceTokenData = [deviceToken dataUsingEncoding:NSASCIIStringEncoding];
    NSString *expectedString = @"44657669636520546f6b656e";
    
    NSString *convertedString = [RCTAzureNotificationHubUtil convertDeviceTokenToString:deviceTokenData];
    
    XCTAssertEqualObjects(convertedString, expectedString);
}

- (void)testGetNotificationTypesWithPermissions
{
    NSMutableDictionary *permissions = [[NSMutableDictionary alloc] init];
    XCTAssertEqual([RCTAzureNotificationHubUtil getNotificationTypesWithPermissions:permissions],
                   UIUserNotificationTypeNone);
    
    [permissions setValue:@YES forKey:RCTNotificationTypeAlert];
    XCTAssertEqual([RCTAzureNotificationHubUtil getNotificationTypesWithPermissions:permissions],
                   UIUserNotificationTypeAlert);
    
    [permissions setValue:@YES forKey:RCTNotificationTypeBadge];
    XCTAssertEqual([RCTAzureNotificationHubUtil getNotificationTypesWithPermissions:permissions],
                   UIUserNotificationTypeAlert | UIUserNotificationTypeBadge);
    
    [permissions setValue:@YES forKey:RCTNotificationTypeSound];
    XCTAssertEqual([RCTAzureNotificationHubUtil getNotificationTypesWithPermissions:permissions],
                   UIUserNotificationTypeAlert | UIUserNotificationTypeBadge | UIUserNotificationTypeSound);
}

@end
