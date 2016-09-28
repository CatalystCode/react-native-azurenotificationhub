//
//  AzureNotificationHub.m
//  AzureNotificationHub
//
//  Created by Phong Cao on 9/28/16.
//  Copyright © 2016 Microsoft. All rights reserved.
//

#import "AzureNotificationHub.h"
#import "RCTLog.h"

@implementation AzureNotificationHub

RCT_EXPORT_MODULE();

RCT_EXPORT_METHOD(register:(nonnull NSDictionary *)config
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    RCTLogInfo(@"Register!!!");
}

RCT_EXPORT_METHOD(unregister:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    RCTLogInfo(@"Un-register!!!");
}

@end
