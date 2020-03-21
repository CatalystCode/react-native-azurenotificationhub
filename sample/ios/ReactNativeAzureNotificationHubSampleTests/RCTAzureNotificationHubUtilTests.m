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

@import OCMock;
@import UserNotifications;

@interface RCTAzureNotificationHubUtilTests : XCTestCase
@end

@implementation RCTAzureNotificationHubUtilTests

- (void)testFormatUNNotification
{
    UNMutableNotificationContent *content = [[UNMutableNotificationContent alloc] init];
    id notificationRequestMock = OCMClassMock([UNNotificationRequest class]);
    OCMStub([notificationRequestMock content]).andReturn(content);
    OCMStub([notificationRequestMock identifier]).andReturn( @"identifier");
    id notificationMock = OCMClassMock([UNNotification class]);
    OCMStub([notificationMock request]).andReturn(notificationRequestMock);

    NSArray *keys = [NSArray arrayWithObjects:@"userInfoKey", nil];
    NSArray *objects = [NSArray arrayWithObjects:@"userInfoObject", nil];
    NSDictionary *info = [[NSDictionary alloc] initWithObjects:objects forKeys:keys];
    content.title = @"title";
    content.threadIdentifier = @"threadIdentifier";
    content.body = @"body";
    content.badge = @(1);
    content.categoryIdentifier = @"category";
    content.sound = @"sound";
    content.userInfo = info;
    NSDictionary *expectedNotification = @{
        @"identifier": @"identifier",
        @"title": @"title",
        @"thread-id": @"threadIdentifier",
        @"userInfo": @{ @"userInfoKey": @"userInfoObject" },
        @"alertBody": @"body",
        @"applicationIconBadgeNumber": [NSNumber numberWithInt:1],
        @"category": @"category",
        @"soundName": @"sound"
    };

    NSDictionary *formattedNotification = [RCTAzureNotificationHubUtil formatUNNotification:notificationMock];

    XCTAssertEqualObjects(formattedNotification, expectedNotification);
}

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
    XCTAssertEqual([RCTAzureNotificationHubUtil getNotificationTypesWithPermissions:permissions], 0);
    
    [permissions setValue:@YES forKey:RCTNotificationTypeAlert];
    XCTAssertEqual([RCTAzureNotificationHubUtil getNotificationTypesWithPermissions:permissions],
                   UNAuthorizationOptionAlert);
    
    [permissions setValue:@YES forKey:RCTNotificationTypeBadge];
    XCTAssertEqual([RCTAzureNotificationHubUtil getNotificationTypesWithPermissions:permissions],
                   UNAuthorizationOptionAlert | UNAuthorizationOptionBadge);
    
    [permissions setValue:@YES forKey:RCTNotificationTypeSound];
    XCTAssertEqual([RCTAzureNotificationHubUtil getNotificationTypesWithPermissions:permissions],
                   UNAuthorizationOptionAlert | UNAuthorizationOptionBadge | UNAuthorizationOptionSound);
}

@end
