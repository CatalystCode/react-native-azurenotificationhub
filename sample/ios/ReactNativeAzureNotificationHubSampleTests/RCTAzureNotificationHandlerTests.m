/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#import <XCTest/XCTest.h>

#import <RNAzureNotificationHub/RCTAzureNotificationHub.h>
#import <RNAzureNotificationHub/RCTAzureNotificationHandler.h>
#import <RNAzureNotificationHub/RCTAzureNotificationHubUtil.h>

#import "React/RCTEventEmitter.h"

@import OCMock;

@interface RCTAzureNotificationHandlerTests : XCTestCase
@end

@implementation RCTAzureNotificationHandlerTests
{
@private
    RCTAzureNotificationHandler *_notificationHandler;
    NSMutableDictionary *_userInfo;
    id _eventEmitter;
}

- (void)setUp
{
    [super setUp];
    
    _eventEmitter = OCMClassMock([RCTEventEmitter class]);
    _notificationHandler = [[RCTAzureNotificationHandler alloc] initWithEventEmitter:_eventEmitter];
    NSArray *keys = [NSArray arrayWithObjects:@"userInfoKey", nil];
    NSArray *objects = [NSArray arrayWithObjects:@"userInfoObject", nil];
    _userInfo = [[NSMutableDictionary alloc] initWithObjects:objects forKeys:keys];
}

- (void)testLocalNotificationReceived
{
    NSNotification* notification = [NSNotification notificationWithName:@"Notification" object:_userInfo userInfo:_userInfo];
    
    [_notificationHandler localNotificationReceived:notification];
    
    OCMVerify([_eventEmitter sendEventWithName:RCTLocalNotificationReceived
                                          body:_userInfo]);
}

- (void)testRemoteNotificationReceived
{
    NSNotification* notification = [NSNotification notificationWithName:@"Notification" object:_userInfo userInfo:_userInfo];
    _userInfo[RCTUserInfoRemote] = @YES;
    
    [_notificationHandler remoteNotificationReceived:notification];
    
    OCMVerify([_eventEmitter sendEventWithName:RCTRemoteNotificationReceived
                                          body:_userInfo]);
}

- (void)testRemoteNotificationRegistered
{
    NSNotification* notification = [NSNotification notificationWithName:@"Notification" object:_userInfo userInfo:_userInfo];
    
    [_notificationHandler remoteNotificationRegistered:notification];
    
    OCMVerify([_eventEmitter sendEventWithName:RCTRemoteNotificationRegistered
                                          body:_userInfo]);
}

- (void)testRemoteNotificationRegisteredError
{
    NSDictionary *errorUserInfo = [[NSDictionary alloc]
                                   initWithObjectsAndKeys:NSLocalizedDescriptionKey, NSLocalizedDescriptionKey, nil];
    
    NSError* error = [NSError errorWithDomain:@"Error domain"
                                         code:100
                                     userInfo:errorUserInfo];
    
    NSArray *keys = [NSArray arrayWithObjects:RCTUserInfoError, nil];
    NSArray *objects = [NSArray arrayWithObjects:error, nil];
    NSDictionary *userInfo = [[NSMutableDictionary alloc] initWithObjects:objects forKeys:keys];
    NSNotification* notification = [NSNotification notificationWithName:@"Notification" object:userInfo userInfo:userInfo];
    NSDictionary *expectedErrorDetails = @{
        @"message": NSLocalizedDescriptionKey,
        @"code": [NSNumber numberWithInt:100],
        @"details": errorUserInfo
    };
    
    [_notificationHandler remoteNotificationRegisteredError:notification];
    
    OCMVerify([_eventEmitter sendEventWithName:RCTRemoteNotificationRegisteredError
                                          body:expectedErrorDetails]);
}

- (void)testAzureNotificationHubRegistered
{
    NSNotification* notification = [NSNotification notificationWithName:@"Notification" object:_userInfo userInfo:_userInfo];
    
    [_notificationHandler azureNotificationHubRegistered:notification];
    
    OCMVerify([_eventEmitter sendEventWithName:RCTAzureNotificationHubRegistered
                                          body:_userInfo]);
}

- (void)testAzureNotificationHubRegisteredError
{
    NSDictionary *errorUserInfo = [[NSDictionary alloc]
                                   initWithObjectsAndKeys:NSLocalizedDescriptionKey, NSLocalizedDescriptionKey, nil];
    
    NSError* error = [NSError errorWithDomain:@"Error domain"
                                         code:100
                                     userInfo:errorUserInfo];
    
    NSArray *keys = [NSArray arrayWithObjects:RCTUserInfoError, nil];
    NSArray *objects = [NSArray arrayWithObjects:error, nil];
    NSDictionary *userInfo = [[NSMutableDictionary alloc] initWithObjects:objects forKeys:keys];
    NSNotification* notification = [NSNotification notificationWithName:@"Notification" object:userInfo userInfo:userInfo];
    NSDictionary *expectedErrorDetails = @{
        @"message": NSLocalizedDescriptionKey,
        @"code": [NSNumber numberWithInt:100],
        @"details": errorUserInfo
    };
    
    [_notificationHandler azureNotificationHubRegisteredError:notification];
    
    OCMVerify([_eventEmitter sendEventWithName:RCTAzureNotificationHubRegisteredError
                                          body:expectedErrorDetails]);
}

@end
