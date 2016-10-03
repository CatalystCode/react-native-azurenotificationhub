//----------------------------------------------------------------
//  Copyright (c) Microsoft Corporation. All rights reserved.
//----------------------------------------------------------------

#import "SBRegistrationParser.h"
#import "SBTemplateRegistration.h"
#import "SBNotificationHubHelper.h"

@implementation SBRegistrationParser

- (SBRegistrationParser*) initParserWithResult:(NSMutableArray*)result
{
    self = [super init];
    _allRegistrations = result;
    return self;
}

+ (NSArray *)parseRegistrations:(NSData *)data error:(NSError **)error
{
    NSMutableArray* registrations = [[NSMutableArray alloc]init];
    
    NSXMLParser* xmlParser = [[NSXMLParser alloc] initWithData:data];
    
    SBRegistrationParser* registrationDelegates = [[SBRegistrationParser alloc] initParserWithResult:registrations];
    [xmlParser setDelegate:(id)registrationDelegates];
    
    if(![xmlParser parse])
    {
        if(error)
        {
            NSString* msg = [NSString stringWithFormat:@"Fail to parse registration:%@",[[NSString alloc]initWithData:data encoding:NSUTF8StringEncoding]];
            //NSLog(@"%@",msg);
            
            (*error)=[SBNotificationHubHelper errorWithMsg:msg code:-1];
        }
    }
    
    return registrations;
}

- (void) parser: (NSXMLParser*)parser didStartElement:(NSString *)elementName namespaceURI:(NSString *)namespaceURI qualifiedName:(NSString *)qName attributes:(NSDictionary *)attributeDict
{    
    if([elementName isEqualToString:@"AppleRegistrationDescription"])
    {
        _currentRegistration = [[SBRegistration alloc] init];
        return;
    }
    
    if([elementName isEqualToString:@"AppleTemplateRegistrationDescription"])
    {
        _currentRegistration = [[SBTemplateRegistration alloc] init];
        return;
    }

}

- (void) parser:(NSXMLParser*)parser foundCharacters:(NSString *)string
{
    if(!_currentElementValue)
    {
        _currentElementValue = [[NSMutableString alloc] initWithString:string];
    }
    else
    {
        [_currentElementValue appendString:string];
    }
}

- (void) parser :(NSXMLParser*)parser didEndElement:(NSString *)elementName namespaceURI:(NSString *)namespaceURI qualifiedName:(NSString *)qName
{
    if([elementName isEqualToString:@"feed"])
    {
        return;
    }
    
    if([elementName isEqualToString:@"AppleRegistrationDescription"] ||
       [elementName isEqualToString:@"AppleTemplateRegistrationDescription"])
    {
        [_allRegistrations addObject:_currentRegistration];
        _currentRegistration = nil;
        return;
    }
    
    if( [elementName isEqualToString:@"ExpirationTime"])
    {
        NSDateFormatter* formatter = [[NSDateFormatter alloc] init];
        [formatter setDateFormat:@"yyyy'-'MM'-'dd'T'HH':'mm':'ss.SSS'Z'"];
        [formatter setTimeZone:[NSTimeZone timeZoneForSecondsFromGMT:0]];
        _currentRegistration.expiresAt = [formatter dateFromString:_currentElementValue];
    }
    else if([elementName isEqualToString:@"RegistrationId"])
    {
        _currentRegistration.registrationId = _currentElementValue;
    }
    else if([elementName isEqualToString:@"ETag"])
    {
        _currentRegistration.ETag = _currentElementValue;
    }
    else if([elementName isEqualToString:@"DeviceToken"])
    {
        _currentRegistration.deviceToken = _currentElementValue;
    }
    else if( [elementName isEqualToString:@"Tags"])
    {
        NSArray *allTags = [_currentElementValue componentsSeparatedByString:@","];
        _currentRegistration.tags = [[NSSet alloc] initWithArray:allTags];
    }
    else if( [elementName isEqualToString:@"Expiry"])
    {
        ((SBTemplateRegistration*)_currentRegistration).expiry = _currentElementValue;
    }
    else if( [elementName isEqualToString:@"BodyTemplate"])
    {
        ((SBTemplateRegistration*)_currentRegistration).bodyTemplate = _currentElementValue;
    }
    else if( [elementName isEqualToString:@"TemplateName"])
    {
        ((SBTemplateRegistration*)_currentRegistration).templateName = _currentElementValue;
    }
    
    _currentElementValue=nil;
}

@end
