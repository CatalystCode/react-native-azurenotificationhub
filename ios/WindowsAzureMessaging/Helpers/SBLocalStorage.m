//----------------------------------------------------------------
//  Copyright (c) Microsoft Corporation. All rights reserved.
//----------------------------------------------------------------

#import "SBLocalStorage.h"
#import "SBNotificationHub.h"
#import "SBNotificationHubHelper.h"
#import "SBRegistration.h"
#import "SBTemplateRegistration.h"

@implementation SBLocalStorage

@synthesize isRefreshNeeded, deviceToken;

static const NSString* storageVersion = @"v1.0.0";

- (SBLocalStorage*) initWithNotificationHubPath: (NSString*) notificationHubPath
{
    self = [super init];
    
    if( self){
        self->_path = notificationHubPath;
        
        self->_versionKey = [NSString stringWithFormat:@"%@-version", notificationHubPath];
        self->_deviceTokenKey = [NSString stringWithFormat:@"%@-deviceToken", notificationHubPath];
        self->_registrationsKey =[NSString stringWithFormat:@"%@-registrations", notificationHubPath];

        [self readContent];
    }
    
    return self;
}


- (StoredRegistrationEntry*) getStoredRegistrationEntryWithRegistrationName:(NSString*) registrationName
{
    StoredRegistrationEntry* reg = [self->_registrations objectForKey:registrationName];
    return reg;
}

- (void) updateWithRegistration: (SBRegistration*) registration
{
    for (NSString* key in [self->_registrations allKeys])
    {
        StoredRegistrationEntry* reg = [self->_registrations objectForKey:key];
        if(  [reg.RegistrationId isEqualToString: registration.registrationId])
        {
            [self updateWithRegistrationName:key registration:registration];
            return;
        }
    }
    
    //  If this is not in the cache then it probably is wiped out from client (due to reinstall)
    if( [registration class] == [SBTemplateRegistration class])
    {
        if (((SBTemplateRegistration*)registration).templateName != nil)
        {
            //  In this specific case we just use the template name that is sent from server to recreate the config.
            [self updateWithRegistrationName:((SBTemplateRegistration*)registration).templateName registration:registration];
        }
        else
        {
            // This is a server created registration
            [self updateWithRegistrationName:registration.registrationId registration:registration];
        }
    }
    else
    {
        // This is most likely SDK created registration. But if the server creates a default registration then we are in merky waters here 
        [self updateWithRegistrationName:SBRegistration.Name registration:registration];
    }
    
}

- (void) updateWithRegistrationName: (NSString*) registrationName registration:(SBRegistration*) registration
{
    if( registration == nil)
    {
        [self deleteWithRegistrationName:registrationName];
        return;
    }
    
    StoredRegistrationEntry* reg = [[StoredRegistrationEntry alloc] init];
    
    reg.RegistrationName = [SBNotificationHubHelper nameOfRegistration:registration];
    reg.registrationId = registration.registrationId;
    reg.ETag = registration.ETag;
    
    [self->_registrations setValue:reg forKey:reg.RegistrationName];
    
    self->deviceToken = [registration deviceToken];
    
    [self flush];
}

- (void) updateWithRegistrationName:(NSString*) registrationName registrationId:(NSString*) registrationId eTag:(NSString*) eTag deviceToken:(NSString*) devToken
{
    StoredRegistrationEntry* reg = [[StoredRegistrationEntry alloc] init];
    
    reg.RegistrationName = registrationName;
    reg.registrationId = registrationId;
    reg.ETag = eTag;
    
    [self->_registrations setValue:reg forKey:reg.RegistrationName];
    
    self->deviceToken = devToken;
    
    [self flush];
}

- (void) deleteWithRegistrationName: (NSString*) registrationName
{
    [self->_registrations removeObjectForKey:registrationName];
    [self flush];
}

- (void) deleteAllRegistrations
{
    [self->_registrations removeAllObjects];
    [self flush];
}

- (void) readContent
{
    self->_registrations = [[NSMutableDictionary alloc] init];
    
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    
    self->deviceToken = [defaults objectForKey:self->_deviceTokenKey];
    
    NSString* version = [defaults objectForKey:self->_versionKey];
    isRefreshNeeded = version == nil || ![version isEqualToString: storageVersion];
    if( isRefreshNeeded )
    {
        return;
    }
    
    NSArray* registrations = [defaults objectForKey:self->_registrationsKey];
    for (NSString* regStr in registrations)
    {
        StoredRegistrationEntry* reg = [[StoredRegistrationEntry alloc] initWithString:regStr];
        [self->_registrations setValue:reg forKey:reg.RegistrationName];
    }
}

- (void) flush
{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
   
    [defaults setObject:self->deviceToken forKey:self->_deviceTokenKey];
    [defaults setObject:storageVersion forKey:self->_versionKey];
    
    NSMutableArray* registrations = [[NSMutableArray alloc] init];
    for (NSString* key in [self->_registrations allKeys])
    {
        StoredRegistrationEntry* reg = [self->_registrations objectForKey:key];
        [registrations addObject: [reg toString]];
    }
    
    if( [registrations count] == 0)
    {
        [defaults removeObjectForKey:self->_registrationsKey];
    }
    else
    {
        [defaults setObject:registrations forKey:self->_registrationsKey];
        
    }

    [defaults synchronize];
}

- (void) refreshFinishedWithDeviceToken:(NSString*)newDeviceToken
{
    isRefreshNeeded = FALSE;
    
    if( ![self->deviceToken isEqualToString:newDeviceToken])
    {
        self->deviceToken = newDeviceToken;
        [self flush];
    }
}

@end
