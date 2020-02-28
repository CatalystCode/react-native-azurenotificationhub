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

@implementation RCTAzureNotificationHubManagerTests
{
@private
    RCTAzureNotificationHubManager *_hubManager;
    NSMutableDictionary *_config;
    RCTPromiseResolveBlock _resolver;
    RCTPromiseRejectBlock _rejecter;
    NSSet *_tags;
    UILocalNotification *_notification;
    id _hubMock;
    id _hubUtilMock;
}

- (void)setUp
{
    [super setUp];
    
    _hubManager = [[RCTAzureNotificationHubManager alloc] init];
    _config = [[NSMutableDictionary alloc] init];
    _resolver = ^(id result) {};
    _rejecter = ^(NSString *code, NSString *message, NSError *error) {};
    _tags = [[NSSet alloc] initWithArray:@[ @"Tag" ]];
    _notification = [[UILocalNotification alloc] init];
    NSArray *keys = [NSArray arrayWithObjects:@"Title", @"Message", nil];
    NSArray *objects = [NSArray arrayWithObjects:@"Title", @"Message", nil];
    NSDictionary *info = [[NSDictionary alloc] initWithObjects:objects forKeys:keys];
    [_notification setUserInfo:info];

    _hubMock = OCMClassMock([SBNotificationHub class]);
    _hubUtilMock = OCMClassMock([RCTAzureNotificationHubUtil class]);
    OCMStub([_hubUtilMock createAzureNotificationHub:OCMOCK_ANY
                                             hubName:OCMOCK_ANY]).andReturn(_hubMock);
    
    void (^runOnMainThread)(NSInvocation *) = ^(NSInvocation *invocation)
    {
        __unsafe_unretained dispatch_block_t block = nil;
        [invocation getArgument:&block atIndex:2]; // First argument starts at 2
        [block invoke];
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
    
    OCMVerify([_hubUtilMock createAzureNotificationHub:RCTTestConnectionString
                                               hubName:RCTTestHubName]);
    
    OCMVerify([_hubMock registerNativeWithDeviceToken:RCTTestDeviceToken
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
    
    XCTAssertEqualObjects(errorMsg, RCTErrorMissingConnectionString);
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
    
    OCMVerify([_hubMock unregisterNativeWithCompletion:OCMOCK_ANY]);
}

- (void)testSetApplicationIconBadgeNumber
{
    NSInteger badgeNumber = 1;
    id sharedApplicationMock = OCMPartialMock([UIApplication sharedApplication]);
    
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
    
    id sharedApplicationMock = OCMPartialMock([UIApplication sharedApplication]);
    OCMStub([sharedApplicationMock applicationIconBadgeNumber]).andReturn(badgeNumber);
    
    [_hubManager setApplicationIconBadgeNumber:badgeNumber];
    [_hubManager getApplicationIconBadgeNumber:block];
}

- (void)testRequestPermissions
{
    id sharedApplicationMock = OCMPartialMock([UIApplication sharedApplication]);
    NSArray *keys = [NSArray arrayWithObjects:RCTNotificationTypeAlert, RCTNotificationTypeBadge, RCTNotificationTypeSound, nil];
    NSArray *objects = [NSArray arrayWithObjects:@YES, @YES, @YES, nil];
    NSDictionary *permissions = [[NSDictionary alloc] initWithObjects:objects forKeys:keys];
    UIUserNotificationType types = [RCTAzureNotificationHubUtil getNotificationTypesWithPermissions:permissions];
    UIUserNotificationSettings *expectedSettings = [UIUserNotificationSettings settingsForTypes:(NSUInteger)types
                                                                                     categories:nil];
    
    [_hubManager requestPermissions:permissions resolver:_resolver rejecter:_rejecter];
    
    OCMVerify([sharedApplicationMock registerUserNotificationSettings:expectedSettings]);
}

- (void)testRequestPermissionsTwice
{
    NSDictionary *permissions = [[NSDictionary alloc] init];
    [_hubManager requestPermissions:permissions resolver:_resolver rejecter:_rejecter];
    
    OCMReject([_hubUtilMock getNotificationTypesWithPermissions:permissions]);
    
    [_hubManager requestPermissions:permissions resolver:_resolver rejecter:_rejecter];
}

- (void)testAbandonPermissions
{
    id sharedApplicationMock = OCMPartialMock([UIApplication sharedApplication]);
    
    [_hubManager abandonPermissions];
    
    OCMVerify([sharedApplicationMock unregisterForRemoteNotifications]);
}

- (void)testCheckPermissions
{
    NSArray *keys = [NSArray arrayWithObjects:RCTNotificationTypeAlert, RCTNotificationTypeBadge, RCTNotificationTypeSound, nil];
    NSArray *objects = [NSArray arrayWithObjects:@YES, @NO, @YES, nil];
    NSDictionary *permissions = [[NSDictionary alloc] initWithObjects:objects forKeys:keys];
    UIUserNotificationType types = [RCTAzureNotificationHubUtil getNotificationTypesWithPermissions:permissions];
    id settingsMock = OCMClassMock([UIUserNotificationSettings class]);
    OCMStub([settingsMock types]).andReturn(types);
    id sharedApplicationMock = OCMPartialMock([UIApplication sharedApplication]);
    OCMStub([sharedApplicationMock currentUserNotificationSettings]).andReturn(settingsMock);
    
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
    id sharedApplicationMock = OCMPartialMock([UIApplication sharedApplication]);
    id notificationMock = OCMClassMock([UILocalNotification class]);
    
    [_hubManager presentLocalNotification:notificationMock];
    
    OCMVerify([sharedApplicationMock presentLocalNotificationNow:notificationMock]);
}

- (void)testScheduleLocalNotification
{
    id sharedApplicationMock = OCMPartialMock([UIApplication sharedApplication]);
    id notificationMock = OCMClassMock([UILocalNotification class]);
    
    [_hubManager scheduleLocalNotification:notificationMock];
    
    OCMVerify([sharedApplicationMock scheduleLocalNotification:notificationMock]);
}

- (void)testCancelAllLocalNotifications
{
    id sharedApplicationMock = OCMPartialMock([UIApplication sharedApplication]);

    [_hubManager cancelAllLocalNotifications];
    
    OCMVerify([sharedApplicationMock cancelAllLocalNotifications]);
}

- (void)testCancelLocalNotificationsMatched
{
    NSMutableArray *notifications = [[NSMutableArray alloc] init];
    [notifications addObject:_notification];
    id sharedApplicationMock = OCMPartialMock([UIApplication sharedApplication]);
    OCMStub([sharedApplicationMock scheduledLocalNotifications]).andReturn(notifications);
    
    [_hubManager cancelLocalNotifications:[_notification userInfo]];
    
    OCMVerify([sharedApplicationMock cancelLocalNotification:_notification]);
}

- (void)testCancelLocalNotificationsNotMatched
{
    NSMutableArray *notifications = [[NSMutableArray alloc] init];
    [notifications addObject:_notification];
    id sharedApplicationMock = OCMPartialMock([UIApplication sharedApplication]);
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
    NSArray *launchOptionsObjects = [NSArray arrayWithObjects: _notification, nil];
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
    
    XCTAssertEqualObjects(notification, [RCTAzureNotificationHubUtil formatLocalNotification:_notification]);
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
    [notifications addObject:_notification];
    id sharedApplicationMock = OCMPartialMock([UIApplication sharedApplication]);
    OCMStub([sharedApplicationMock scheduledLocalNotifications]).andReturn(notifications);
    RCTResponseSenderBlock callback = ^(NSArray *response)
    {
        XCTAssertEqualObjects(response[0][0], [RCTAzureNotificationHubUtil formatLocalNotification:_notification]);
    };
    
    [_hubManager getScheduledLocalNotifications:callback];
}

@end
