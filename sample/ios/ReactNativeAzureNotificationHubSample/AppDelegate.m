/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#import "AppDelegate.h"

#import <React/RCTBridge.h>
#import <React/RCTBundleURLProvider.h>
#import <React/RCTRootView.h>
#import <RNAzureNotificationHub/RCTAzureNotificationHubManager.h>

@implementation AppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
  RCTBridge *bridge = [[RCTBridge alloc] initWithDelegate:self launchOptions:launchOptions];
  RCTRootView *rootView = [[RCTRootView alloc] initWithBridge:bridge
                                                   moduleName:@"ReactNativeAzureNotificationHubSample"
                                            initialProperties:nil];

  rootView.backgroundColor = [[UIColor alloc] initWithRed:1.0f green:1.0f blue:1.0f alpha:1];

  self.window = [[UIWindow alloc] initWithFrame:[UIScreen mainScreen].bounds];
  UIViewController *rootViewController = [UIViewController new];
  rootViewController.view = rootView;
  self.window.rootViewController = rootViewController;
  [self.window makeKeyAndVisible];

  // Registering for local notifications
  [[UNUserNotificationCenter currentNotificationCenter] setDelegate:self];

  return YES;
}

- (NSURL *)sourceURLForBridge:(RCTBridge *)bridge
{
#if DEBUG
  return [[RCTBundleURLProvider sharedSettings] jsBundleURLForBundleRoot:@"index" fallbackResource:nil];
#else
  return [[NSBundle mainBundle] URLForResource:@"main" withExtension:@"jsbundle"];
#endif
}

// Invoked when the app successfully registered with Apple Push Notification service (APNs).
- (void)application:(UIApplication *)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken
{
  [RCTAzureNotificationHubManager didRegisterForRemoteNotificationsWithDeviceToken:deviceToken];
}

// Invoked when APNs cannot successfully complete the registration process.
- (void)application:(UIApplication *)application didFailToRegisterForRemoteNotificationsWithError:(NSError *)error
{
  [RCTAzureNotificationHubManager didFailToRegisterForRemoteNotificationsWithError:error];
}

// Invoked when a remote notification arrived and there is data to be fetched.
- (void)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo
                                                       fetchCompletionHandler:(void (^)(UIBackgroundFetchResult result))completionHandler
{
  [RCTAzureNotificationHubManager didReceiveRemoteNotification:userInfo
                                        fetchCompletionHandler:completionHandler];
}

// Invoked when a notification arrived while the app was running in the foreground.
- (void)userNotificationCenter:(UNUserNotificationCenter *)center
       willPresentNotification:(UNNotification *)notification
         withCompletionHandler:(void (^)(UNNotificationPresentationOptions options))completionHandler
{
  [RCTAzureNotificationHubManager userNotificationCenter:center
                                 willPresentNotification:notification
                                   withCompletionHandler:completionHandler];
}

@end
