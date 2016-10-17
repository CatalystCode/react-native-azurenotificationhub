//----------------------------------------------------------------
//  Copyright (c) Microsoft Corporation. All rights reserved.
//----------------------------------------------------------------

#import "SBRegistration.h"
#import "SBNotificationHubHelper.h"

@implementation SBRegistration

NSString* const nativeRegistrationName = @"$Default";

NSString* const nativeRegistrationFormat = @"<entry xmlns=\"http://www.w3.org/2005/Atom\"><content type=\"text/xml\"><AppleRegistrationDescription xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://schemas.microsoft.com/netservices/2010/10/servicebus/connect\">%@<DeviceToken>%@</DeviceToken></AppleRegistrationDescription></content></entry>";

@synthesize ETag, expiresAt, tags, deviceToken, registrationId;

+ (NSString*) Name
{
    return nativeRegistrationName;
}

+ (NSString*) payloadWithDeviceToken:(NSString*)deviceToken tags:(NSSet*)tags
{
    NSString* tagNode = @"";
    NSString* tagString = [SBNotificationHubHelper convertTagSetToString:tags];
    if( [tagString length]>0)
    {
        tagNode = [NSString stringWithFormat:@"<Tags>%@</Tags>", tagString];
    }
    
    return [NSString stringWithFormat:nativeRegistrationFormat, tagNode, deviceToken];
}

@end

