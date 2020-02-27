/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#import <XCTest/XCTest.h>
#import <RNAzureNotificationHub/RCTAzureNotificationHub.h>
#import <RNAzureNotificationHub/RCTAzureNotificationHubManager.h>

@import OCMockito;

@interface RCTAzureNotificationHubManagerTests : XCTestCase
@end

@implementation RCTAzureNotificationHubManagerTests
{
  RCTAzureNotificationHubManager *hubManager;
  NSMutableDictionary *config;
  RCTPromiseResolveBlock resolver;
  RCTPromiseRejectBlock rejecter;
}

- (void)setUp
{
  [super setUp];
  hubManager = [[RCTAzureNotificationHubManager alloc] init];
  config = [[NSMutableDictionary alloc] init];
  resolver = ^(id result) {};
  rejecter = ^(NSString *code, NSString *message, NSError *error) {};
}

- (void)testRegisterNoConnectionString
{
  __block NSString *errorMsg;
  rejecter = ^(NSString *code, NSString *message, NSError *error)
  {
    errorMsg = message;
  };

  [hubManager register:@""
                config:config
              resolver:resolver
              rejecter:rejecter];

  XCTAssertEqualObjects(errorMsg, RCTErrorMissingConnectionString);
}

- (void)testRegisterNoHubName
{
  __block NSString *errorMsg;
  rejecter = ^(NSString *code, NSString *message, NSError *error)
  {
    errorMsg = message;
  };

  [config setObject:@"Connection String" forKey:@"connectionString"];
  [hubManager register:@""
                config:config
              resolver:resolver
              rejecter:rejecter];

  XCTAssertEqualObjects(errorMsg, RCTErrorMissingHubName);
}

- (void)testRegisterSuccessfully
{
  __block bool hasError = false;
  rejecter = ^(NSString *code, NSString *message, NSError *error)
  {
    hasError = true;
  };

  [config setObject:@"Connection String" forKey:@"connectionString"];
  [config setObject:@"Hub Name" forKey:@"hubName"];
  [hubManager register:@""
                config:config
              resolver:resolver
              rejecter:rejecter];

  XCTAssertEqual(hasError, false);
}

@end
