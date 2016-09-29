//----------------------------------------------------------------
//  Copyright (c) Microsoft Corporation. All rights reserved.
//----------------------------------------------------------------

#import "SBTemplateRegistration.h"
#import "SBNotificationHubHelper.h"

@implementation SBTemplateRegistration

NSString* const templateRegistrationFormat = @"<entry xmlns=\"http://www.w3.org/2005/Atom\"><content type=\"text/xml\"><AppleTemplateRegistrationDescription xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://schemas.microsoft.com/netservices/2010/10/servicebus/connect\">%@<DeviceToken>%@</DeviceToken><BodyTemplate><![CDATA[%@]]></BodyTemplate>%@%@<TemplateName>%@</TemplateName></AppleTemplateRegistrationDescription></content></entry>";

@synthesize bodyTemplate, expiry, templateName;

+ (NSString*) payloadWithDeviceToken:(NSString*)deviceToken bodyTemplate:(NSString*)bodyTemplate expiryTemplate:(NSString*)expiryTemplate priorityTemplate:(NSString*)priorityTemplate tags:(NSSet*)tags templateName:(NSString *)templateName
{
    NSString* expiryFullString = @"";
    if(expiryTemplate && [expiryTemplate length]>0)
    {
        expiryFullString = [NSString stringWithFormat:@"<Expiry>%@</Expiry>",expiryTemplate];
    }
    
    NSString* priorityFullString = @"";
    if(priorityTemplate && [priorityTemplate length]>0)
    {
        priorityFullString = [NSString stringWithFormat:@"<Priority>%@</Priority>",priorityTemplate];
    }
    
    NSString* tagNode = @"";
    NSString* tagString = [SBNotificationHubHelper convertTagSetToString:tags];
    if( [tagString length]>0)
    {
        tagNode = [NSString stringWithFormat:@"<Tags>%@</Tags>", tagString];
    }
    
    return [NSString stringWithFormat:templateRegistrationFormat, tagNode, deviceToken, bodyTemplate, expiryFullString, priorityFullString, templateName];
}

@end
