//----------------------------------------------------------------
//  Copyright (c) Microsoft Corporation. All rights reserved.
//----------------------------------------------------------------

#import <Foundation/Foundation.h>
#import "SBRegistration.h"

@interface SBTemplateRegistration : SBRegistration

@property (copy, nonatomic) NSString* bodyTemplate;
@property (copy, nonatomic) NSString* expiry;
@property (copy, nonatomic) NSString* templateName;

+ (NSString*) payloadWithDeviceToken:(NSString*)deviceToken bodyTemplate:(NSString*)bodyTemplate expiryTemplate:(NSString*)expiryTemplate priorityTemplate:(NSString*)priorityTemplate tags:(NSSet*)tags templateName:(NSString *)templateName;

@end