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

RCT_EXTERN NSString *const RCTErrorUnspecified;

static NSString *const RCTTestDeviceToken = @"Device Token";
static NSString *const RCTTestConnectionString = @"Connection String";
static NSString *const RCTTestHubName = @"Hub Name";
static NSString *const RCTTestTemplate = @"Template";
static NSString *const RCTTestTemplateName = @"Template Name";

id sharedApplicationMock;

@implementation RCTAzureNotificationHubManagerTests
{
@private
    RCTAzureNotificationHubManager *_hubManager;
    NSMutableDictionary *_config;
    RCTPromiseResolveBlock _resolver;
    RCTPromiseRejectBlock _rejecter;
    NSSet *_tags;
    UILocalNotification *_localNotification;
    id _notificationMock;
    id _hubMock;
    id _hubUtilMock;
    id _notificationHandlerMock;
}

+ (void)setUp
{
    sharedApplicationMock = OCMPartialMock([UIApplication sharedApplication]);
}

- (void)setUp
{
    [super setUp];

    _hubManager = [[RCTAzureNotificationHubManager alloc] init];
    _config = [[NSMutableDictionary alloc] init];
    _resolver = ^(id result) {};
    _rejecter = ^(NSString *code, NSString *message, NSError *error) {};
    _tags = [[NSSet alloc] initWithArray:@[ @"Tag" ]];
    _localNotification = [[UILocalNotification alloc] init];
    NSArray *keys = [NSArray arrayWithObjects:@"Title", @"Message", nil];
    NSArray *objects = [NSArray arrayWithObjects:@"Title", @"Message", nil];
    NSDictionary *info = [[NSDictionary alloc] initWithObjects:objects forKeys:keys];
    [_localNotification setUserInfo:info];

    _notificationMock = OCMClassMock([UNNotification class]);
    UNMutableNotificationContent *content = [[UNMutableNotificationContent alloc] init];
    id notificationRequestMock = OCMClassMock([UNNotificationRequest class]);
    OCMStub([notificationRequestMock content]).andReturn(content);
    OCMStub([notificationRequestMock identifier]).andReturn( @"identifier");
    OCMStub([_notificationMock request]).andReturn(notificationRequestMock);
    content.userInfo = info;

    _hubMock = OCMClassMock([SBNotificationHub class]);
    _hubUtilMock = OCMClassMock([RCTAzureNotificationHubUtil class]);
    OCMStub([_hubUtilMock createAzureNotificationHub:OCMOCK_ANY
                                             hubName:OCMOCK_ANY]).andReturn(_hubMock);
    _notificationHandlerMock = OCMClassMock([RCTAzureNotificationHandler class]);
    [_hubManager setNotificationHandler:_notificationHandlerMock];

    void (^runOnMainThread)(NSInvocation *) = ^(NSInvocation *invocation)
    {
        __unsafe_unretained dispatch_block_t block = nil;
        [invocation getArgument:&block atIndex:2]; // First argument starts at 2
        block();
    };

    OCMStub([_hubUtilMock runOnMainThread:OCMOCK_ANY]).andDo(runOnMainThread);
}

- (void)testRegisterNoConnectionString
{
    __block NSString *errorMsg;
    _rejecter = ^(NSString *code, NSString *message, NSError *error)
    {
        errorMsg = message;
    };

    [_hubManager register:RCTTestDeviceToken
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

    [_config setObject:RCTTestConnectionString forKey:RCTConnectionStringKey];
    [_hubManager register:RCTTestDeviceToken
                   config:_config
                 resolver:_resolver
                 rejecter:_rejecter];

    XCTAssertEqualObjects(errorMsg, RCTErrorMissingHubName);
}

- (void)testRegisterNativeError
{
    [_config setObject:RCTTestConnectionString forKey:RCTConnectionStringKey];
    [_config setObject:RCTTestHubName forKey:RCTHubNameKey];
    [_config setObject:_tags forKey:RCTTagsKey];

    NSError *error = [NSError errorWithDomain:@"Mock Error" code:0 userInfo:nil];
    void (^registerNativeWithDeviceToken)(NSInvocation *) = ^(NSInvocation *invocation)
    {
        __unsafe_unretained RCTNativeCompletion completion = nil;
        [invocation getArgument:&completion atIndex:4]; // First argument starts at 2
        completion(error);
    };

    OCMStub([_hubMock registerNativeWithDeviceToken:OCMOCK_ANY
                                               tags:OCMOCK_ANY
                                         completion:OCMOCK_ANY]).andDo(registerNativeWithDeviceToken);

    id defaultCenterMock = OCMPartialMock([NSNotificationCenter defaultCenter]);

    [_hubManager register:RCTTestDeviceToken
                   config:_config
                 resolver:_resolver
                 rejecter:_rejecter];

    OCMVerify([_hubUtilMock runOnMainThread:OCMOCK_ANY]);

    OCMVerify([_hubUtilMock createAzureNotificationHub:RCTTestConnectionString
                                               hubName:RCTTestHubName]);

    OCMVerify([_hubMock registerNativeWithDeviceToken:RCTTestDeviceToken
                                                 tags:_tags
                                           completion:OCMOCK_ANY]);

    OCMVerify([defaultCenterMock postNotificationName:RCTAzureNotificationHubRegisteredError
                                               object:OCMOCK_ANY
                                             userInfo:@{RCTUserInfoError: error}]);
}

- (void)testRegisterNativeSuccessfully
{
    [_config setObject:RCTTestConnectionString forKey:RCTConnectionStringKey];
    [_config setObject:RCTTestHubName forKey:RCTHubNameKey];
    [_config setObject:_tags forKey:RCTTagsKey];

    void (^registerNativeWithDeviceToken)(NSInvocation *) = ^(NSInvocation *invocation)
    {
        __unsafe_unretained RCTNativeCompletion completion = nil;
        [invocation getArgument:&completion atIndex:4]; // First argument starts at 2
        completion(nil);
    };

    OCMStub([_hubMock registerNativeWithDeviceToken:OCMOCK_ANY
                                               tags:OCMOCK_ANY
                                         completion:OCMOCK_ANY]).andDo(registerNativeWithDeviceToken);

    id defaultCenterMock = OCMPartialMock([NSNotificationCenter defaultCenter]);

    [_hubManager register:RCTTestDeviceToken
                   config:_config
                 resolver:_resolver
                 rejecter:_rejecter];

    OCMVerify([_hubUtilMock runOnMainThread:OCMOCK_ANY]);

    OCMVerify([_hubUtilMock createAzureNotificationHub:RCTTestConnectionString
                                               hubName:RCTTestHubName]);

    OCMVerify([_hubMock registerNativeWithDeviceToken:RCTTestDeviceToken
                                                 tags:_tags
                                           completion:OCMOCK_ANY]);

    OCMVerify([defaultCenterMock postNotificationName:RCTAzureNotificationHubRegistered
                                               object:OCMOCK_ANY
                                             userInfo:@{RCTUserInfoSuccess: @YES}]);
}

- (void)testRegisterTemplateNoConnectionString
{
    __block NSString *errorMsg;
    _rejecter = ^(NSString *code, NSString *message, NSError *error)
    {
        errorMsg = message;
    };

    [_hubManager registerTemplate:RCTTestDeviceToken
                           config:_config
                         resolver:_resolver
                         rejecter:_rejecter];

    XCTAssertEqualObjects(errorMsg, RCTErrorMissingConnectionString);
}

- (void)testRegisterTemplateNoHubName
{
    __block NSString *errorMsg;
    _rejecter = ^(NSString *code, NSString *message, NSError *error)
    {
        errorMsg = message;
    };

    [_config setObject:RCTTestConnectionString forKey:RCTConnectionStringKey];
    [_hubManager registerTemplate:RCTTestDeviceToken
                           config:_config
                         resolver:_resolver
                         rejecter:_rejecter];

    XCTAssertEqualObjects(errorMsg, RCTErrorMissingHubName);
}

- (void)testRegisterTemplateNoTemplateName
{
    __block NSString *errorMsg;
    _rejecter = ^(NSString *code, NSString *message, NSError *error)
    {
        errorMsg = message;
    };

    [_config setObject:RCTTestConnectionString forKey:RCTConnectionStringKey];
    [_config setObject:RCTTestHubName forKey:RCTHubNameKey];
    [_hubManager registerTemplate:RCTTestDeviceToken
                           config:_config
                         resolver:_resolver
                         rejecter:_rejecter];

    XCTAssertEqualObjects(errorMsg, RCTErrorMissingTemplateName);
}

- (void)testRegisterTemplateNoTemplate
{
    __block NSString *errorMsg;
    _rejecter = ^(NSString *code, NSString *message, NSError *error)
    {
        errorMsg = message;
    };

    [_config setObject:RCTTestConnectionString forKey:RCTConnectionStringKey];
    [_config setObject:RCTTestHubName forKey:RCTHubNameKey];
    [_config setObject:RCTTestTemplateName forKey:RCTTemplateNameKey];
    [_hubManager registerTemplate:RCTTestDeviceToken
                           config:_config
                         resolver:_resolver
                         rejecter:_rejecter];

    XCTAssertEqualObjects(errorMsg, RCTErrorMissingTemplate);
}

- (void)testRegisterTemplateNativeError
{
    [_config setObject:RCTTestConnectionString forKey:RCTConnectionStringKey];
    [_config setObject:RCTTestHubName forKey:RCTHubNameKey];
    [_config setObject:_tags forKey:RCTTagsKey];
    [_config setObject:RCTTestTemplateName forKey:RCTTemplateNameKey];
    [_config setObject:RCTTestTemplate forKey:RCTTemplateKey];

    NSError *error = [NSError errorWithDomain:@"Mock Error" code:0 userInfo:nil];
    void (^registerTemplateNativeWithDeviceToken)(NSInvocation *) = ^(NSInvocation *invocation)
    {
        __unsafe_unretained RCTNativeCompletion completion = nil;
        [invocation getArgument:&completion atIndex:7]; // First argument starts at 2
        completion(error);
    };

    OCMStub([_hubMock registerTemplateWithDeviceToken:RCTTestDeviceToken
                                                 name:RCTTestTemplateName
                                     jsonBodyTemplate:RCTTestTemplate
                                       expiryTemplate:nil
                                                 tags:_tags
                                           completion:OCMOCK_ANY]).andDo(registerTemplateNativeWithDeviceToken);

    id defaultCenterMock = OCMPartialMock([NSNotificationCenter defaultCenter]);

    [_hubManager registerTemplate:RCTTestDeviceToken
                           config:_config
                         resolver:_resolver
                         rejecter:_rejecter];

    OCMVerify([_hubUtilMock runOnMainThread:OCMOCK_ANY]);

    OCMVerify([_hubUtilMock createAzureNotificationHub:RCTTestConnectionString
                                               hubName:RCTTestHubName]);

    OCMVerify([_hubMock registerTemplateWithDeviceToken:RCTTestDeviceToken
                                                   name:RCTTestTemplateName
                                       jsonBodyTemplate:RCTTestTemplate
                                         expiryTemplate:nil
                                                   tags:_tags
                                             completion:OCMOCK_ANY]);

    OCMVerify([defaultCenterMock postNotificationName:RCTAzureNotificationHubRegisteredError
                                               object:OCMOCK_ANY
                                             userInfo:@{RCTUserInfoError: error}]);
}

- (void)testRegisterTemplateNativeSuccessfully
{
    [_config setObject:RCTTestConnectionString forKey:RCTConnectionStringKey];
    [_config setObject:RCTTestHubName forKey:RCTHubNameKey];
    [_config setObject:_tags forKey:RCTTagsKey];
    [_config setObject:RCTTestTemplateName forKey:RCTTemplateNameKey];
    [_config setObject:RCTTestTemplate forKey:RCTTemplateKey];

    void (^registerTemplateNativeWithDeviceToken)(NSInvocation *) = ^(NSInvocation *invocation)
    {
        __unsafe_unretained RCTNativeCompletion completion = nil;
        [invocation getArgument:&completion atIndex:7]; // First argument starts at 2
        completion(nil);
    };

    OCMStub([_hubMock registerTemplateWithDeviceToken:RCTTestDeviceToken
                                                 name:RCTTestTemplateName
                                     jsonBodyTemplate:RCTTestTemplate
                                       expiryTemplate:nil
                                                 tags:_tags
                                           completion:OCMOCK_ANY]).andDo(registerTemplateNativeWithDeviceToken);

    id defaultCenterMock = OCMPartialMock([NSNotificationCenter defaultCenter]);

    [_hubManager registerTemplate:RCTTestDeviceToken
                           config:_config
                         resolver:_resolver
                         rejecter:_rejecter];

    OCMVerify([_hubUtilMock runOnMainThread:OCMOCK_ANY]);

    OCMVerify([_hubUtilMock createAzureNotificationHub:RCTTestConnectionString
                                               hubName:RCTTestHubName]);

    OCMVerify([_hubMock registerTemplateWithDeviceToken:RCTTestDeviceToken
                                                   name:RCTTestTemplateName
                                       jsonBodyTemplate:RCTTestTemplate
                                         expiryTemplate:nil
                                                   tags:_tags
                                             completion:OCMOCK_ANY]);

    OCMVerify([defaultCenterMock postNotificationName:RCTAzureNotificationHubRegistered
                                               object:OCMOCK_ANY
                                             userInfo:@{RCTUserInfoSuccess: @YES}]);
}

- (void)testUnregisterNoRegistration
{
    __block NSString *errorMsg;
    _rejecter = ^(NSString *code, NSString *message, NSError *error)
    {
        errorMsg = message;
    };

    [_hubManager unregister:_resolver rejecter:_rejecter];

    XCTAssertEqualObjects(errorMsg, RCTErrorUnableToUnregisterNoRegistration);
}

- (void)testUnregisterNativeError
{
    [_config setObject:RCTTestConnectionString forKey:RCTConnectionStringKey];
    [_config setObject:RCTTestHubName forKey:RCTHubNameKey];
    [_config setObject:_tags forKey:RCTTagsKey];

    NSError *error = [NSError errorWithDomain:@"Mock Error" code:0 userInfo:nil];
    void (^unregisterNativeWithCompletion)(NSInvocation *) = ^(NSInvocation *invocation)
    {
        __unsafe_unretained RCTNativeCompletion completion = nil;
        [invocation getArgument:&completion atIndex:2]; // First argument starts at 2
        completion(error);
    };

    OCMStub([_hubMock unregisterNativeWithCompletion:OCMOCK_ANY]).andDo(unregisterNativeWithCompletion);
    id defaultCenterMock = OCMPartialMock([NSNotificationCenter defaultCenter]);

    [_hubManager register:RCTTestDeviceToken
                   config:_config
                 resolver:_resolver
                 rejecter:_rejecter];

    [_hubManager unregister:_resolver rejecter:_rejecter];

    OCMVerify([_hubUtilMock runOnMainThread:OCMOCK_ANY]);
    OCMVerify([_hubMock unregisterNativeWithCompletion:OCMOCK_ANY]);
    OCMVerify([defaultCenterMock postNotificationName:RCTErrorUnspecified
                                               object:OCMOCK_ANY
                                             userInfo:@{RCTUserInfoError: error}]);
}

- (void)testUnregisterNativeSuccessfully
{
    [_config setObject:RCTTestConnectionString forKey:RCTConnectionStringKey];
    [_config setObject:RCTTestHubName forKey:RCTHubNameKey];
    [_config setObject:_tags forKey:RCTTagsKey];

    void (^unregisterNativeWithCompletion)(NSInvocation *) = ^(NSInvocation *invocation)
    {
        __unsafe_unretained RCTNativeCompletion completion = nil;
        [invocation getArgument:&completion atIndex:2]; // First argument starts at 2
        completion(nil);
    };

    OCMStub([_hubMock unregisterNativeWithCompletion:OCMOCK_ANY]).andDo(unregisterNativeWithCompletion);
    id defaultCenterMock = OCMPartialMock([NSNotificationCenter defaultCenter]);
    OCMReject([defaultCenterMock postNotificationName:OCMOCK_ANY object:OCMOCK_ANY userInfo:OCMOCK_ANY]);

    [_hubManager register:RCTTestDeviceToken
                   config:_config
                 resolver:_resolver
                 rejecter:_rejecter];

    [_hubManager unregister:_resolver rejecter:_rejecter];

    OCMVerify([_hubUtilMock runOnMainThread:OCMOCK_ANY]);
    OCMVerify([_hubMock unregisterNativeWithCompletion:OCMOCK_ANY]);
    OCMReject([defaultCenterMock postNotificationName:RCTErrorUnspecified
                                               object:OCMOCK_ANY
                                             userInfo:OCMOCK_ANY]);
}

- (void)testUnregisterTemplateNoRegistration
{
    __block NSString *errorMsg;
    _rejecter = ^(NSString *code, NSString *message, NSError *error)
    {
        errorMsg = message;
    };

    [_hubManager unregisterTemplate:RCTTestTemplateName
                           resolver:_resolver
                           rejecter:_rejecter];

    XCTAssertEqualObjects(errorMsg, RCTErrorUnableToUnregisterNoRegistration);
}

- (void)testUnregisterTemplateNativeError
{
    [_config setObject:RCTTestConnectionString forKey:RCTConnectionStringKey];
    [_config setObject:RCTTestHubName forKey:RCTHubNameKey];
    [_config setObject:_tags forKey:RCTTagsKey];
    [_config setObject:RCTTestTemplateName forKey:RCTTemplateNameKey];
    [_config setObject:RCTTestTemplate forKey:RCTTemplateKey];

    NSError *error = [NSError errorWithDomain:@"Mock Error" code:0 userInfo:nil];
    void (^unregisterTemplateNativeWithCompletion)(NSInvocation *) = ^(NSInvocation *invocation)
    {
        __unsafe_unretained RCTNativeCompletion completion = nil;
        [invocation getArgument:&completion atIndex:3]; // First argument starts at 2
        completion(error);
    };

    OCMStub([_hubMock unregisterTemplateWithName:RCTTestTemplateName
                                      completion:OCMOCK_ANY]).andDo(unregisterTemplateNativeWithCompletion);

    id defaultCenterMock = OCMPartialMock([NSNotificationCenter defaultCenter]);

    [_hubManager registerTemplate:RCTTestDeviceToken
                           config:_config
                         resolver:_resolver
                         rejecter:_rejecter];

    [_hubManager unregisterTemplate:RCTTestTemplateName
                           resolver:_resolver
                           rejecter:_rejecter];

    OCMVerify([_hubUtilMock runOnMainThread:OCMOCK_ANY]);

    OCMVerify([_hubMock unregisterTemplateWithName:RCTTestTemplateName
                                        completion:OCMOCK_ANY]);

    OCMVerify([defaultCenterMock postNotificationName:RCTErrorUnspecified
                                               object:OCMOCK_ANY
                                             userInfo:@{RCTUserInfoError: error}]);
}

- (void)testUnregisterTemplateNativeSuccessfully
{
    [_config setObject:RCTTestConnectionString forKey:RCTConnectionStringKey];
    [_config setObject:RCTTestHubName forKey:RCTHubNameKey];
    [_config setObject:_tags forKey:RCTTagsKey];
    [_config setObject:RCTTestTemplateName forKey:RCTTemplateNameKey];
    [_config setObject:RCTTestTemplate forKey:RCTTemplateKey];

    void (^unregisterTemplateNativeWithCompletion)(NSInvocation *) = ^(NSInvocation *invocation)
    {
        __unsafe_unretained RCTNativeCompletion completion = nil;
        [invocation getArgument:&completion atIndex:3]; // First argument starts at 2
        completion(nil);
    };

    OCMStub([_hubMock unregisterTemplateWithName:RCTTestTemplateName
                                      completion:OCMOCK_ANY]).andDo(unregisterTemplateNativeWithCompletion);

    id defaultCenterMock = OCMPartialMock([NSNotificationCenter defaultCenter]);

    [_hubManager registerTemplate:RCTTestDeviceToken
                           config:_config
                         resolver:_resolver
                         rejecter:_rejecter];

    [_hubManager unregisterTemplate:RCTTestTemplateName
                           resolver:_resolver
                           rejecter:_rejecter];

    OCMVerify([_hubUtilMock runOnMainThread:OCMOCK_ANY]);

    OCMVerify([_hubMock unregisterTemplateWithName:RCTTestTemplateName
                                        completion:OCMOCK_ANY]);

    OCMReject([defaultCenterMock postNotificationName:RCTErrorUnspecified
                                               object:OCMOCK_ANY
                                             userInfo:OCMOCK_ANY]);
}

- (void)testSetApplicationIconBadgeNumber
{
    NSInteger badgeNumber = 1;

    [_hubManager setApplicationIconBadgeNumber:badgeNumber];

    OCMVerify([sharedApplicationMock setApplicationIconBadgeNumber:badgeNumber]);
}

- (void)testGetApplicationIconBadgeNumber
{
    NSInteger badgeNumber = 1;
    RCTResponseSenderBlock block = ^(NSArray *response)
    {
        XCTAssertEqual([response[0] longValue], badgeNumber);
    };

    OCMStub([sharedApplicationMock applicationIconBadgeNumber]).andReturn(badgeNumber);

    [_hubManager setApplicationIconBadgeNumber:badgeNumber];
    [_hubManager getApplicationIconBadgeNumber:block];
}

- (void)testRequestPermissions
{
    NSArray *keys = [NSArray arrayWithObjects:RCTNotificationTypeAlert, RCTNotificationTypeBadge, RCTNotificationTypeSound, nil];
    NSArray *objects = [NSArray arrayWithObjects:@YES, @YES, @YES, nil];
    NSDictionary *permissions = [[NSDictionary alloc] initWithObjects:objects forKeys:keys];
    UNAuthorizationOptions expectedOptions = [RCTAzureNotificationHubUtil getNotificationTypesWithPermissions:permissions];
    id center = OCMPartialMock([UNUserNotificationCenter currentNotificationCenter]);
    void (^requestAuthorizationCompletionHandler)(NSInvocation *) = ^(NSInvocation *invocation)
    {
        __unsafe_unretained RCTNotificationCompletion completion = nil;
        [invocation getArgument:&completion atIndex:3]; // First argument starts at 2
        completion(true, nil);
    };

    OCMStub([center requestAuthorizationWithOptions:expectedOptions
                                  completionHandler:OCMOCK_ANY]).andDo(requestAuthorizationCompletionHandler);

    [_hubManager requestPermissions:permissions resolver:_resolver rejecter:_rejecter];

    OCMVerify([center requestAuthorizationWithOptions:expectedOptions
                                    completionHandler:OCMOCK_ANY]);

    OCMVerify([_hubUtilMock runOnMainThread:OCMOCK_ANY]);
    OCMVerify([sharedApplicationMock registerForRemoteNotifications]);
}

- (void)testAbandonPermissions
{
    [_hubManager abandonPermissions];

    OCMVerify([sharedApplicationMock unregisterForRemoteNotifications]);
}

- (void)testCheckPermissions
{
    NSArray *keys = [NSArray arrayWithObjects:RCTNotificationTypeAlert, RCTNotificationTypeBadge, RCTNotificationTypeSound, nil];
    NSArray *objects = [NSArray arrayWithObjects:@YES, @NO, @YES, nil];
    NSDictionary *permissions = [[NSDictionary alloc] initWithObjects:objects forKeys:keys];
    id center = OCMPartialMock([UNUserNotificationCenter currentNotificationCenter]);
    UNNotificationSettings *settings = [UNNotificationSettings alloc];
    [settings setValue:@(UNNotificationSettingEnabled) forKey:@"notificationCenterSetting"];
    [settings setValue:@(UNNotificationSettingDisabled) forKey:@"badgeSetting"];
    [settings setValue:@(UNNotificationSettingEnabled) forKey:@"soundSetting"];
    void (^getNotificationSettingsWithCompletionHandler)(NSInvocation *) = ^(NSInvocation *invocation)
    {
        __unsafe_unretained RCTNotificationSettingsCompletion completion = nil;
        [invocation getArgument:&completion atIndex:2]; // First argument starts at 2
        completion(settings);
    };

    OCMStub([center getNotificationSettingsWithCompletionHandler:OCMOCK_ANY]).andDo(getNotificationSettingsWithCompletionHandler);

    NSArray *expectedResponse = @[@{
                                       RCTNotificationTypeAlert: @YES,
                                       RCTNotificationTypeBadge: @NO,
                                       RCTNotificationTypeSound: @YES,
                                   }];

    RCTResponseSenderBlock callback = ^(NSArray *response)
    {
        XCTAssertEqualObjects(response, expectedResponse);
    };

    [_hubManager checkPermissions:callback];
}

- (void)testPresentLocalNotification
{
    id notificationMock = OCMClassMock([UILocalNotification class]);

    [_hubManager presentLocalNotification:notificationMock];

    OCMVerify([sharedApplicationMock presentLocalNotificationNow:notificationMock]);
}

- (void)testScheduleLocalNotification
{
    id notificationMock = OCMClassMock([UILocalNotification class]);

    [_hubManager scheduleLocalNotification:notificationMock];

    OCMVerify([sharedApplicationMock scheduleLocalNotification:notificationMock]);
}

- (void)testCancelAllLocalNotifications
{
    [_hubManager cancelAllLocalNotifications];

    OCMVerify([sharedApplicationMock cancelAllLocalNotifications]);
}

- (void)testCancelLocalNotificationsMatched
{
    NSMutableArray *notifications = [[NSMutableArray alloc] init];
    [notifications addObject:_localNotification];
    OCMStub([sharedApplicationMock scheduledLocalNotifications]).andReturn(notifications);
    UNNotificationRequest *request = [_notificationMock request];
    UNMutableNotificationContent *content = [request content];
    NSDictionary *userInfo = content.userInfo;

    [_hubManager cancelLocalNotifications:userInfo];

    OCMVerify([sharedApplicationMock cancelLocalNotification:_localNotification]);
}

- (void)testCancelLocalNotificationsNotMatched
{
    NSMutableArray *notifications = [[NSMutableArray alloc] init];
    [notifications addObject:_localNotification];
    OCMStub([sharedApplicationMock scheduledLocalNotifications]).andReturn(notifications);
    OCMReject([sharedApplicationMock cancelLocalNotification:OCMOCK_ANY]);

    NSArray *keys = [NSArray arrayWithObjects:@"Different Title", @"Different Message", nil];
    NSArray *objects = [NSArray arrayWithObjects:@"Title", @"Message", nil];
    NSDictionary *info = [[NSDictionary alloc] initWithObjects:objects forKeys:keys];
    [_hubManager cancelLocalNotifications:info];
}

- (void)testGetInitialNotificationRemoteNotification
{
    NSString *remoteNotificationKey = @"Remote Notification Key";
    NSString *remoteNotificationObject = @"Remote Notification Object";
    NSArray *launchOptionsKeys = [NSArray arrayWithObjects:UIApplicationLaunchOptionsRemoteNotificationKey, nil];
    NSArray *launchOptionsObjects = [NSArray arrayWithObjects:[[NSMutableDictionary alloc]
                                                                    initWithObjects:@[ remoteNotificationObject ]
                                                                            forKeys:@[ remoteNotificationKey ]], nil];

    NSDictionary *launchOptionsMock = [[NSDictionary alloc] initWithObjects:launchOptionsObjects
                                                                    forKeys:launchOptionsKeys];

    id bridgeMock = OCMClassMock([RCTBridge class]);
    OCMStub([bridgeMock launchOptions]).andReturn(launchOptionsMock);
    _hubManager.bridge = bridgeMock;
    __block bool userInfoRemote = false;
    __block NSString *notification = nil;
    _resolver = ^(NSMutableDictionary<NSString *, id> *initialNotification)
    {
        userInfoRemote = initialNotification[RCTUserInfoRemote];
        notification = initialNotification[remoteNotificationKey];
    };

    [_hubManager getInitialNotification:_resolver reject:_rejecter];

    XCTAssertEqual(userInfoRemote, true);
    XCTAssertEqualObjects(notification, remoteNotificationObject);
}

- (void)testGetInitialNotificationLocalNotification
{
    NSArray *launchOptionsKeys = [NSArray arrayWithObjects:UIApplicationLaunchOptionsLocalNotificationKey, nil];
    NSArray *launchOptionsObjects = [NSArray arrayWithObjects: _localNotification, nil];
    NSDictionary *launchOptionsMock = [[NSDictionary alloc] initWithObjects:launchOptionsObjects
                                                                    forKeys:launchOptionsKeys];

    id bridgeMock = OCMClassMock([RCTBridge class]);
    OCMStub([bridgeMock launchOptions]).andReturn(launchOptionsMock);
    _hubManager.bridge = bridgeMock;
    __block NSDictionary *notification = nil;
    _resolver = ^(NSDictionary *initialLocalNotification)
    {
        notification = initialLocalNotification;
    };

    [_hubManager getInitialNotification:_resolver reject:_rejecter];

    XCTAssertEqualObjects(notification, [RCTAzureNotificationHubUtil formatLocalNotification:_localNotification]);
}

- (void)testGetInitialNotificationNoNotification
{
    id bridgeMock = OCMClassMock([RCTBridge class]);
    OCMStub([bridgeMock launchOptions]).andReturn([[NSDictionary alloc] init]);
    _hubManager.bridge = bridgeMock;
    __block id notification = nil;
    _resolver = ^(id result)
    {
        notification = result;
    };

    [_hubManager getInitialNotification:_resolver reject:_rejecter];

    XCTAssertEqualObjects(notification, (id)kCFNull);
}

- (void)testGetScheduledLocalNotifications
{
    NSMutableArray<UILocalNotification *> *notifications = [[NSMutableArray alloc] init];
    [notifications addObject:_localNotification];
    OCMStub([sharedApplicationMock scheduledLocalNotifications]).andReturn(notifications);
    __block NSMutableArray *scheduledNotifications = nil;
    RCTResponseSenderBlock callback = ^(NSArray *result)
    {
        scheduledNotifications = result;
    };

    [_hubManager getScheduledLocalNotifications:callback];

    XCTAssertEqualObjects(scheduledNotifications[0][0], [RCTAzureNotificationHubUtil formatLocalNotification:_localNotification]);
}

- (void)testStartObserving
{
    id defaultCenterMock = OCMPartialMock([NSNotificationCenter defaultCenter]);

    [_hubManager startObserving];

    OCMVerify([defaultCenterMock addObserver:OCMOCK_ANY
                                    selector:[OCMArg anySelector]
                                        name:RCTLocalNotificationReceived
                                      object:nil]);

    OCMVerify([defaultCenterMock addObserver:OCMOCK_ANY
                                    selector:[OCMArg anySelector]
                                        name:RCTRemoteNotificationReceived
                                      object:nil]);

    OCMVerify([defaultCenterMock addObserver:OCMOCK_ANY
                                    selector:[OCMArg anySelector]
                                        name:RCTRemoteNotificationRegistered
                                      object:nil]);

    OCMVerify([defaultCenterMock addObserver:OCMOCK_ANY
                                    selector:[OCMArg anySelector]
                                        name:RCTRemoteNotificationRegisteredError
                                      object:nil]);

    OCMVerify([defaultCenterMock addObserver:OCMOCK_ANY
                                    selector:[OCMArg anySelector]
                                        name:RCTAzureNotificationHubRegistered
                                      object:nil]);

    OCMVerify([defaultCenterMock addObserver:OCMOCK_ANY
                                    selector:[OCMArg anySelector]
                                        name:RCTAzureNotificationHubRegisteredError
                                      object:nil]);
}

- (void)testStopObserving
{
    id defaultCenterMock = OCMPartialMock([NSNotificationCenter defaultCenter]);

    [_hubManager stopObserving];

    OCMVerify([defaultCenterMock removeObserver:OCMOCK_ANY]);
}

- (void)testSupportedEvents
{
    NSArray *expectedSupportedEvents = @[RCTLocalNotificationReceived,
                                         RCTRemoteNotificationReceived,
                                         RCTRemoteNotificationRegistered,
                                         RCTRemoteNotificationRegisteredError,
                                         RCTAzureNotificationHubRegistered,
                                         RCTAzureNotificationHubRegisteredError];

    NSArray *supportedEvents = [_hubManager supportedEvents];

    XCTAssertEqualObjects(supportedEvents, expectedSupportedEvents);
}

- (void)testDidRegisterForRemoteNotificationsWithDeviceToken
{
    id defaultCenterMock = OCMPartialMock([NSNotificationCenter defaultCenter]);
    NSData *deviceToken = [@"Device Token" dataUsingEncoding:NSASCIIStringEncoding];
    NSString *hexString = [RCTAzureNotificationHubUtil convertDeviceTokenToString:deviceToken];

    [RCTAzureNotificationHubManager didRegisterForRemoteNotificationsWithDeviceToken:deviceToken];

    OCMVerify([defaultCenterMock postNotificationName:RCTRemoteNotificationRegistered
                                               object:OCMOCK_ANY
                                             userInfo:@{RCTUserInfoDeviceToken: [hexString copy]}]);
}

- (void)testDidFailToRegisterForRemoteNotificationsWithError
{
    id defaultCenterMock = OCMPartialMock([NSNotificationCenter defaultCenter]);
    NSError *error = [NSError errorWithDomain:@"Mock Error" code:0 userInfo:nil];

    [RCTAzureNotificationHubManager didFailToRegisterForRemoteNotificationsWithError:error];

    OCMVerify([defaultCenterMock postNotificationName:RCTRemoteNotificationRegisteredError
                                               object:OCMOCK_ANY
                                             userInfo:@{RCTUserInfoError: error}]);
}

- (void)testDidReceiveRemoteNotification
{
    id defaultCenterMock = OCMPartialMock([NSNotificationCenter defaultCenter]);
    NSDictionary *userInfo = [[NSDictionary alloc] initWithObjectsAndKeys:@"key", @"value", nil];

    [RCTAzureNotificationHubManager didReceiveRemoteNotification:userInfo
                                          fetchCompletionHandler:nil];

    OCMVerify([defaultCenterMock postNotificationName:RCTRemoteNotificationReceived
                                               object:OCMOCK_ANY
                                             userInfo:userInfo]);
}

- (void)testUserNotificationCenter
{
    id defaultCenterMock = OCMPartialMock([NSNotificationCenter defaultCenter]);
    id notificationMock = OCMClassMock([UNNotification class]);

    [RCTAzureNotificationHubManager userNotificationCenter:nil
                                   willPresentNotification:notificationMock
                                     withCompletionHandler:nil];

    OCMVerify([RCTAzureNotificationHubUtil formatUNNotification:notificationMock]);
    OCMVerify([defaultCenterMock postNotificationName:RCTLocalNotificationReceived
                                               object:OCMOCK_ANY
                                             userInfo:OCMOCK_ANY]);
}

@end
