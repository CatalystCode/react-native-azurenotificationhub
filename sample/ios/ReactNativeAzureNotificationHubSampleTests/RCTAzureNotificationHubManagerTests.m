/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#import <XCTest/XCTest.h>
#import <WindowsAzureMessaging/WindowsAzureMessaging.h>

#import <RNAzureNotificationHub/RCTAzureNotificationHub.h>
#import <RNAzureNotificationHub/RCTAzureNotificationHubManager.h>
#import <RNAzureNotificationHub/RCTAzureNotificationHubUtil.h>

@import OCMock;

@interface RCTAzureNotificationHubManagerTests : XCTestCase
@end

@implementation RCTAzureNotificationHubManagerTests
{
@private
    RCTAzureNotificationHubManager *_hubManager;
    NSMutableDictionary *_config;
    RCTPromiseResolveBlock _resolver;
    RCTPromiseRejectBlock _rejecter;
    id _hub;
    id _hubUtilClass;
}

- (void)setUp
{
    [super setUp];
    _hubManager = [[RCTAzureNotificationHubManager alloc] init];
    _config = [[NSMutableDictionary alloc] init];
    _resolver = ^(id result) {};
    _rejecter = ^(NSString *code, NSString *message, NSError *error) {};
    
    _hub = OCMClassMock([SBNotificationHub class]);
    _hubUtilClass = OCMClassMock([RCTAzureNotificationHubUtil class]);
    OCMStub([_hubUtilClass createAzureNotificationHub:OCMOCK_ANY
                                              hubName:OCMOCK_ANY]).andReturn(_hub);
}

- (void)testRegisterNoConnectionString
{
    __block NSString *errorMsg;
    _rejecter = ^(NSString *code, NSString *message, NSError *error)
    {
        errorMsg = message;
    };
    
    [_hubManager register:@""
                   config:_config
                 resolver:_resolver
                 rejecter:_rejecter];
    
    XCTAssertEqualObjects(errorMsg, RCTErrorMissingConnectionString);
}

- (void)testRegisterNoHubName
{
    __block NSString *errorMsg;
    _rejecter = ^(NSString *code, NSString *message, NSError *error)
    {
        errorMsg = message;
    };
    
    [_config setObject:@"Connection String"
                forKey:@"connectionString"];
    
    [_hubManager register:@""
                   config:_config
                 resolver:_resolver
                 rejecter:_rejecter];
    
    XCTAssertEqualObjects(errorMsg, RCTErrorMissingHubName);
}

- (void)testRegisterSuccessfully
{
    __block bool hasError = false;
    _rejecter = ^(NSString *code, NSString *message, NSError *error)
    {
        hasError = true;
    };
    
    [_config setObject:@"Connection String" forKey:@"connectionString"];
    [_config setObject:@"Hub Name" forKey:@"hubName"];
    
    OCMExpect([_hubUtilClass createAzureNotificationHub:OCMOCK_ANY
                                                hubName:OCMOCK_ANY]);
    
    OCMExpect([_hub registerNativeWithDeviceToken:OCMOCK_ANY
                                             tags:OCMOCK_ANY
                                       completion:OCMOCK_ANY]);
    
    [_hubManager register:@""
                   config:_config
                 resolver:_resolver
                 rejecter:_rejecter];
    
    XCTAssertEqual(hasError, false);
    OCMVerify([_hubUtilClass createAzureNotificationHub:OCMOCK_ANY
                                                hubName:OCMOCK_ANY]);
    OCMVerifyAllWithDelay(_hub, 0.5);
}

@end
