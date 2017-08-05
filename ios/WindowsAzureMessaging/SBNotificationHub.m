//----------------------------------------------------------------
//  Copyright (c) Microsoft Corporation. All rights reserved.
//----------------------------------------------------------------

#import "SBNotificationHub.h"
#import "SBNotificationHubHelper.h"
#import "SBTokenProvider.h"
#import "SBRegistrationParser.h"
#import "SBURLConnection.h"
#import "SBLocalStorage.h"
#import "SBRegistration.h"
#import "SBTemplateRegistration.h"
#import <UIKit/UIKit.h>

typedef void (^SBCompletion)(NSError*);

@interface SBThreadParameter : NSObject

@property (copy, nonatomic) NSArray* parameters;
@property (copy, nonatomic) NSString* deviceToken;
@property (copy, nonatomic) SBCompletion completion;
@property (nonatomic) BOOL isMainThread;

@end

@implementation SBThreadParameter
@synthesize parameters, completion, isMainThread, deviceToken;

@end

@implementation SBNotificationHub

NSString* const currentVersion = @"v0.1.6";
NSString* const _APIVersion = @"2013-04";
NSString* const _UserAgentTemplate = @"NOTIFICATIONHUBS/%@(api-origin=IosSdk; os=%@; os_version=%@;)";

- (SBNotificationHub*) initWithConnectionString:(NSString*) connectionString notificationHubPath:(NSString*)notificationHubPath
{
    self = [super init];
    
    if(!connectionString || !notificationHubPath)
    {
        return nil;
    }
    
    if( self){
        NSDictionary* connnectionDictionary = [SBNotificationHubHelper parseConnectionString:connectionString];
        
        NSString* endPoint = [connnectionDictionary objectForKey:@"endpoint"];
        if(endPoint)
        {
            self->_serviceEndPoint = [[NSURL alloc] initWithString:endPoint];
        }
        
        if(self->_serviceEndPoint == nil || [self->_serviceEndPoint host] == nil)
        {
            NSLog(@"%@",@"Endpoint is missing or not in URL format in connectionString.");
            return nil;
        }
        
        self->_path = notificationHubPath;
        tokenProvider = [[SBTokenProvider alloc] initWithConnectionDictinary:connnectionDictionary];
        
        if(tokenProvider == nil)
        {
            return nil;
        }
        
        storageManager = [[SBLocalStorage alloc] initWithNotificationHubPath:notificationHubPath];
        
        if(storageManager == nil)
        {
            return nil;
        }
    }
    
    return self;
}

- (NSString *)convertDeviceToken:(NSData *)deviceTokenData
{
    NSString* newDeviceToken = [[[[[deviceTokenData description]
                                   stringByReplacingOccurrencesOfString:@"<"withString:@""]
                                  stringByReplacingOccurrencesOfString:@">" withString:@""]
                                 stringByReplacingOccurrencesOfString: @" " withString: @""] uppercaseString];
    return newDeviceToken;
}

- (void) registerNativeWithDeviceToken:(NSData*)deviceTokenData tags:(NSSet*)tags completion:(void (^)(NSError* error))completion;
{
    if( deviceTokenData == nil)
    {
        if(completion)
        {
            completion([SBNotificationHubHelper errorForNullDeviceToken]);
        }
        
        return;
    }
    
    NSString* deviceToken = [self convertDeviceToken:deviceTokenData];
    NSString* name = [SBRegistration Name];
    NSString* payload = [SBRegistration payloadWithDeviceToken:deviceToken tags:tags];
    
    if( storageManager.isRefreshNeeded)
    {
        NSString* refreshDeviceToken = [self getRefreshDeviceTokenWithNewDeviceToken:deviceToken];
        [self retrieveAllRegistrationsWithDeviceToken:refreshDeviceToken completion:^(NSArray * regs, NSError * error)
        {
            if( error == nil)
            {
                [storageManager refreshFinishedWithDeviceToken:refreshDeviceToken];
                [self createOrUpdateWith:name payload:payload deviceToken:deviceToken completion:completion];
            }
            else
            {
                if( completion)
                {
                    completion(error);
                }
            }
        }];
    }
    else
    {
        [self createOrUpdateWith:name payload:payload deviceToken:deviceToken completion:completion];
    }
}

- (void) registerTemplateWithDeviceToken:(NSData*)deviceTokenData name:(NSString*)name jsonBodyTemplate:(NSString*)bodyTemplate expiryTemplate:(NSString*)expiryTemplate tags:(NSSet*)tags completion:(void (^)(NSError* error))completion;
{
    [self registerTemplateWithDeviceToken:deviceTokenData name:name jsonBodyTemplate:bodyTemplate expiryTemplate:expiryTemplate priorityTemplate:nil tags:tags completion:completion];
}

- (void) registerTemplateWithDeviceToken:(NSData*)deviceTokenData name:(NSString*)name jsonBodyTemplate:(NSString*)bodyTemplate expiryTemplate:(NSString*)expiryTemplate priorityTemplate:(NSString*)priorityTemplate tags:(NSSet*)tags completion:(void (^)(NSError* error))completion;
{
    if( deviceTokenData == nil)
    {
        if(completion)
        {
            completion([SBNotificationHubHelper errorForNullDeviceToken]);
        }
        
        return;
    }
    
    NSString* deviceToken = [self convertDeviceToken:deviceTokenData];
    
    NSError* error;
    if( [self verifyTemplateName:name error:&error] == FALSE)
    {
        if(completion)
        {
            completion(error);
        }
        
        return;
    }
    
    NSString* payload = [SBTemplateRegistration payloadWithDeviceToken:deviceToken bodyTemplate:bodyTemplate expiryTemplate:expiryTemplate priorityTemplate:priorityTemplate tags:tags templateName:name];
    
    if( storageManager.isRefreshNeeded)
    {
        NSString* refreshDeviceToken = [self getRefreshDeviceTokenWithNewDeviceToken:deviceToken];
        [self retrieveAllRegistrationsWithDeviceToken:refreshDeviceToken completion:^(NSArray * regs, NSError * error)
         {
             if( error == nil)
             {
                 [storageManager refreshFinishedWithDeviceToken:refreshDeviceToken];
                 [self createOrUpdateWith:name payload:payload deviceToken:deviceToken completion:completion];
             }
             else
             {
                 if( completion)
                 {
                     completion(error);
                 }
             }
         }];
    }
    else
    {
        [self createOrUpdateWith:name payload:payload  deviceToken:deviceToken completion:completion];
    }
}

- (void)createOrUpdateWith:(NSString *)name payload:(NSString *)payload deviceToken:(NSString *)deviceToken completion:(void (^)(NSError *))completion
{
    StoredRegistrationEntry* cached = [storageManager getStoredRegistrationEntryWithRegistrationName:name];
    if(cached == nil)
    {
        [self createRegistrationIdAndUpsert:name payload:payload deviceToken:deviceToken completion:^(NSError *error)
         {
             if( error != nil && [error code]== 410)
             {
                 
                 [self createRegistrationIdAndUpsert:name payload:payload deviceToken:deviceToken completion:completion];
             }
             else
             {
                 if(completion)
                 {
                     completion(error);
                 }
             }
         }];
    }
    else
    {
        [self upsertRegistrationWithName:name registrationId:cached.RegistrationId payload:payload completion:^(NSError *error)
        {
            if( error != nil && [error code]== 410)
            {
                
                [self createRegistrationIdAndUpsert:name payload:payload deviceToken:deviceToken completion:completion];
            }
            else
            {
                if( completion)
                {
                    completion(error);
                }
            }
        }];
    }
}

- (void)createRegistrationIdAndUpsert:(NSString*)name payload:(NSString*)payload deviceToken:(NSString*)deviceToken completion:(void (^)(NSError*))completion
{
    NSURL *requestUri = [self composeCreateRegistrationIdUri];
    [self registrationOperationWithRequestUri:requestUri payload:@"" httpMethod:@"POST" ETag:@"" completion:^(NSHTTPURLResponse *response, NSData *data, NSError *error) {
        if(!error)
        {
            NSURL* locationUrl = [[NSURL alloc] initWithString:[[response allHeaderFields] objectForKey:@"Location"]];
            NSString *registrationId = [self extractRegistrationIdFromLocationUri:locationUrl];
            [storageManager updateWithRegistrationName:name registrationId:registrationId  eTag:@"*" deviceToken:deviceToken];
            [self upsertRegistrationWithName:name registrationId:registrationId payload:payload completion:completion];
        }
        else
        {
            if(completion)
            {
                completion(error);
            }
        }
    }];
}

- (void)upsertRegistrationWithName:(NSString*)name registrationId:(NSString*)registrationId payload:(NSString*)payload completion:(void (^)(NSError*))completion
{
    NSURL *requestUri = [self composeRegistrationUriWithRegistrationId: registrationId];
    [self registrationOperationWithRequestUri:requestUri payload:payload httpMethod:@"PUT" ETag:@"" completion:^(NSHTTPURLResponse *response1, NSData *data, NSError *error) {
        
        if(!error)
        {
            [self parseResultAndUpdateWithName:name data:data error:&error];
        }
        
        if(completion)
        {
            completion(error);
        }
    }];
}

- (void)parseResultAndUpdateWithName:(NSString *)name data:(NSData *)data error:(NSError **)error
{
    NSError* parseError;
    NSArray *registrations = [SBRegistrationParser parseRegistrations:data error:&parseError];
    if( !parseError)
    {
        [storageManager updateWithRegistrationName:name registration:(SBRegistration*)[registrations objectAtIndex:0]];
    }
    else if( error)
    {
        (*error) = parseError;
    }
}

- (void) retrieveAllRegistrationsWithDeviceToken:(NSString*)deviceToken completion:(void (^)(NSArray*, NSError*))completion
{
    NSURL *requestUri = [self composeRetrieveAllRegistrationsUriWithDeviceToken:deviceToken];
    [self registrationOperationWithRequestUri:requestUri payload:@"" httpMethod:@"GET" ETag:@"" completion:^(NSHTTPURLResponse *response, NSData *data, NSError *error) {
        
        if(error)
        {
            if([error code]==404)
            {
                if(completion)
                {
                    completion(nil,nil);
                }
                return;
            }
            else
            {
                if(completion)
                {
                    completion(nil,error);
                }
                return;
            }
        }
        
        NSError *parseError;
        NSArray *registrations = [SBRegistrationParser parseRegistrations:data error:&parseError];
        if( parseError)
        {
            if(completion)
            {
                completion(nil,parseError);
            }
            
            return;
        }

        for (SBRegistration* retrieved in registrations)
        {
            [storageManager updateWithRegistration:retrieved];
        }
        
        if(completion)
        {
            completion(registrations,nil);
        }
    }];
}

- (void) unregisterNativeWithCompletion:(void (^)(NSError*))completion
{
    [self deleteRegistrationWithName:[SBRegistration Name] completion:completion];
}

- (void) unregisterTemplateWithName:(NSString*)name completion:(void (^)(NSError*))completion
{
    NSError* error;
    if( [self verifyTemplateName:name error:&error] == FALSE)
    {
        if(completion)
        {
            completion(error);
        }
        
        return;
    }
    
    
    [self deleteRegistrationWithName:name completion:completion];
}

- (void) deleteRegistrationWithName:(NSString*)templateName completion:(void (^)(NSError*))completion
{
    StoredRegistrationEntry* cached = [storageManager getStoredRegistrationEntryWithRegistrationName:templateName];
    if(cached == nil)
    {
        if(completion)
        {
            completion(nil);
        }
        
        return;
    }
    
    NSURL *requestUri = [self composeRegistrationUriWithRegistrationId: cached.RegistrationId];
    [self registrationOperationWithRequestUri:requestUri payload:@"" httpMethod:@"DELETE" ETag:@"*" completion:^(NSHTTPURLResponse *response, NSData *data, NSError *error) {
        
        if( error == nil || [error code] == 404 )
        {
            [storageManager deleteWithRegistrationName:templateName];
            error = nil;
        }
        
        if(completion)
        {
            completion(error);
        }
    }];
}

- (void) unregisterAllWithDeviceToken:(NSData*)deviceTokenData completion:(void (^)(NSError*))completion
{
    if( deviceTokenData == nil)
    {
        if(completion)
        {
            completion([SBNotificationHubHelper errorForNullDeviceToken]);
        }
        
        return;
    }
    
    NSString* deviceToken = [self convertDeviceToken:deviceTokenData];

    SBThreadParameter* parameter = [[SBThreadParameter alloc]init];
    parameter.deviceToken = deviceToken;
    parameter.completion = completion;
    parameter.isMainThread = [[NSThread currentThread] isMainThread];
    [self performSelectorInBackground:@selector(deleteAllRegistrationThread:) withObject:parameter];
}

- (void) deleteAllRegistrationThread:(SBThreadParameter*)parameter
{
    NSError* error;
    NSArray* registrations = [self retrieveAllRegistrationsWithDeviceToken:parameter.deviceToken error:&error];
    if(registrations.count != 0)
    {
        for(SBRegistration* reg in registrations)
        {
            NSString* name = [SBNotificationHubHelper nameOfRegistration:reg];
            [self deleteRegistrationWithName:name error:&error];
            if(error)
            {
                break;
            }
        }
    }
    
    if (!error)
    {
        // Remove any registrations that are only in the client and not on server
        [storageManager deleteAllRegistrations];
    }
    
    if(parameter.completion)
    {
        if( parameter.isMainThread)
        {
            // callback on main thread
            dispatch_async(dispatch_get_main_queue(), ^{parameter.completion(error);});
        }
        else
        {
            parameter.completion(error);
        }
    }
}

- (BOOL) registerNativeWithDeviceToken:(NSData*)deviceTokenData tags:(NSSet*)tags error:(NSError**)error
{
    if( deviceTokenData == nil)
    {
        if(error)
        {
            *error = [SBNotificationHubHelper errorForNullDeviceToken];
        }
        
        return FALSE;
    }
    
    NSString* deviceToken = [self convertDeviceToken:deviceTokenData];
    
    NSString* name =[SBRegistration Name];
    NSString* payload = [SBRegistration payloadWithDeviceToken:deviceToken tags:tags];
    
    if( storageManager.isRefreshNeeded)
    {
        NSString* refreshDeviceToken = [self getRefreshDeviceTokenWithNewDeviceToken:deviceToken];
        
        NSError* retrieveError;
        [self retrieveAllRegistrationsWithDeviceToken:refreshDeviceToken error:&retrieveError];
        
        if( retrieveError )
        {
            if( error)
            {
                *error = retrieveError;
            }
            
            return FALSE;
        }
        
        [storageManager refreshFinishedWithDeviceToken:refreshDeviceToken];
    }
    
    return [self createorUpdateWith:name payload:payload deviceToken:deviceToken error:error];
}

- (BOOL) registerTemplateWithDeviceToken:(NSData*)deviceTokenData name:(NSString*)templateName jsonBodyTemplate:(NSString*)bodyTemplate expiryTemplate:(NSString*)expiryTemplate tags:(NSSet*)tags error:(NSError**)error
{
    return [self registerTemplateWithDeviceToken:deviceTokenData name:templateName jsonBodyTemplate:bodyTemplate expiryTemplate:expiryTemplate priorityTemplate:nil tags:tags error:error];
}

- (BOOL) registerTemplateWithDeviceToken:(NSData*)deviceTokenData name:(NSString*)templateName jsonBodyTemplate:(NSString*)bodyTemplate expiryTemplate:(NSString*)expiryTemplate priorityTemplate:(NSString*)priorityTemplate tags:(NSSet*)tags error:(NSError**)error
{
    if( deviceTokenData == nil)
    {
        if(error)
        {
            *error = [SBNotificationHubHelper errorForNullDeviceToken];
        }
        
        return FALSE;
    }
    
    NSString* deviceToken = [self convertDeviceToken:deviceTokenData];
    
    if( [self verifyTemplateName:templateName error:error] == FALSE)
    {
        return FALSE;
    }
    
    NSString* payload = [SBTemplateRegistration payloadWithDeviceToken:deviceToken bodyTemplate:bodyTemplate expiryTemplate:expiryTemplate priorityTemplate:priorityTemplate tags:tags templateName:templateName];
    
    if( storageManager.isRefreshNeeded)
    {
        NSString* refreshDeviceToken = [self getRefreshDeviceTokenWithNewDeviceToken:deviceToken];
        
        NSError* retrieveError;
        [self retrieveAllRegistrationsWithDeviceToken:refreshDeviceToken error:&retrieveError];
        
        if( retrieveError )
        {
            if( error)
            {
                *error = retrieveError;
            }
            
            return FALSE;
        }
        
        [storageManager refreshFinishedWithDeviceToken:refreshDeviceToken];
    }
    
    return [self createorUpdateWith:templateName payload:payload deviceToken:deviceToken error:error];
}

- (BOOL)createorUpdateWith:(NSString *)name payload:(NSString *)payload deviceToken:(NSString *)deviceToken error:(NSError**)error
{
    StoredRegistrationEntry* cached = [storageManager getStoredRegistrationEntryWithRegistrationName:name];
    NSString *registrationId;
    if(cached == nil)
    {
        NSError *createRegistrationError;
        registrationId = [self createRegistrationId:&createRegistrationError];
        if (createRegistrationError)
        {
            if (error)
            {
                *error = createRegistrationError;
            }
            
            return false;
        }
        
        [storageManager updateWithRegistrationName:name registrationId:registrationId eTag:@"*" deviceToken:deviceToken];
    }
    else
    {
        registrationId = cached.RegistrationId;
    }
    
    NSError *upsertRegistrationError;
    BOOL result = [self upsertRegistrationWithName:name registrationId:registrationId payload:payload error:&upsertRegistrationError];
    if( upsertRegistrationError != nil && [upsertRegistrationError code] == 410)
    {
        // if we get 410 from service, we will recreate registration id and will try to do upsert
        NSError *retrieveRegistrationError;
        registrationId = [self createRegistrationId:&retrieveRegistrationError];
        if (retrieveRegistrationError)
        {
            if (error)
            {
                *error = retrieveRegistrationError;
            }
            
            return false;
        }
        
        [storageManager updateWithRegistrationName:name registrationId:registrationId eTag:@"*" deviceToken:deviceToken];
        NSError *operationError;
        result = [self upsertRegistrationWithName:name registrationId:registrationId payload:payload error:&operationError];
        if (operationError)
        {
            if (error)
            {
                *error = operationError;
            }
            
        }
        
        return result;
    }
    
    if(error)
    {
        (*error) = upsertRegistrationError;
    }
    
    return result;
}

- (NSString*)createRegistrationId:(NSError**)error
{
    NSURL *requestUri = [self composeCreateRegistrationIdUri];
    NSHTTPURLResponse* response=nil;
    NSData* data;
    NSError* operationError;
    [self registrationOperationWithRequestUri:requestUri payload:@"" httpMethod:@"POST" ETag:@"" response:&response responseData:&data error:&operationError];
    
    if(operationError == nil)
    {
        NSURL* locationUrl = [[NSURL alloc] initWithString:[[response allHeaderFields] objectForKey:@"Location"]];
        return [self extractRegistrationIdFromLocationUri:locationUrl];
    }
    
    if(error)
    {
        *error = operationError;
    }
    
    return nil;
}

- (BOOL)createRegistrationWithName:(NSString*)name payload:(NSString*)payload error:(NSError**)error
{
    NSURL *requestUri = [self composeRegistrationUriWithRegistrationId: @""];
    
    NSHTTPURLResponse* response=nil;
    NSData* data;
    NSError* operationError;
    BOOL result = [self registrationOperationWithRequestUri:requestUri payload:payload httpMethod:@"POST" ETag:@"" response:&response responseData:&data error:&operationError];
    
    if( operationError == nil)
    {
        NSArray *registrations = [SBRegistrationParser parseRegistrations:data error:&operationError];
        if( operationError == nil)
        {
            [storageManager updateWithRegistrationName:name registration:(SBRegistration*)[registrations objectAtIndex:0]];
        }
    }
    
    if(error)
    {
        *error = operationError;
    }
    
    return result;
}

- (BOOL)upsertRegistrationWithName:(NSString*)name registrationId:(NSString*)registrationId payload:(NSString*)payload error:(NSError**)error
{
    NSURL *requestUri = [self composeRegistrationUriWithRegistrationId: registrationId];
    
    NSHTTPURLResponse* response=nil;
    NSData* data;
    NSError* operationError;
    BOOL result = [self registrationOperationWithRequestUri:requestUri payload:payload httpMethod:@"PUT" ETag:@"" response:&response responseData:&data error:&operationError];
    
    if( operationError == nil)
    {
        NSArray *registrations = [SBRegistrationParser parseRegistrations:data error:&operationError];
        if( operationError == nil)
        {
            [storageManager updateWithRegistrationName:name registration:(SBRegistration*)[registrations objectAtIndex:0]];
        }
    }
    
    if(error)
    {
        *error = operationError;
    }
    
    return result;
}


// This function will retrieve all registrations and update local storage with them.
- (NSArray*) retrieveAllRegistrationsWithDeviceToken:(NSString*)deviceToken error:(NSError**)error
{
    NSURL *requestUri = [self composeRetrieveAllRegistrationsUriWithDeviceToken:deviceToken];
    
    NSHTTPURLResponse* response=nil;
    NSData* data;
    NSError* operationError;
    [self registrationOperationWithRequestUri:requestUri payload:@"" httpMethod:@"GET" ETag:@"" response:&response responseData:&data error:&operationError];
    
    if(operationError)
    {
        if([operationError code]==404)
        {
            //no registrations
            return nil;
        }
        
        if( error)
        {
            (*error) = operationError;
        }
        
        return nil;
    }
    
    NSError *parseError;
    NSArray *registrations = [SBRegistrationParser parseRegistrations:data error:&parseError];    
    if(parseError)
    {
        if( error)
        {
            (*error) = parseError;
        }
    }
    else
    {
        [storageManager deleteAllRegistrations];
        for (SBRegistration* retrieved in registrations)
        {
            [storageManager updateWithRegistration:retrieved];
        }
    }
    
    return registrations;
}

- (BOOL) unregisterNativeWithError:(NSError**)error
{
    return [self deleteRegistrationWithName:[SBRegistration Name] error:error];
}

- (BOOL) unregisterTemplateWithName:(NSString*)name error:(NSError**)error
{
    if( [self verifyTemplateName:name error:error] == FALSE)
    {
        return FALSE;
    }
    
    return [self deleteRegistrationWithName:name error:error];
}

- (BOOL) deleteRegistrationWithName:(NSString*)name error:(NSError**)error
{
    StoredRegistrationEntry* cached = [storageManager getStoredRegistrationEntryWithRegistrationName:name];
    if(cached == nil)
    {   
        return true;
    }
    
    NSURL *requestUri = [self composeRegistrationUriWithRegistrationId: cached.RegistrationId];
    
    NSURLResponse* response=nil;
    NSData* data;
    NSError* operationError;
    BOOL result = [self registrationOperationWithRequestUri:requestUri payload:@"" httpMethod:@"DELETE" ETag:@"*" response:&response responseData:&data error:&operationError];
    
    if( operationError == nil || [operationError code] == 404 )
    {
        // don't return error for not-found
        [storageManager deleteWithRegistrationName:name];
        error = nil;
    }
    
    if(error != nil)
    {
        *error = operationError;
    }
    
    return result;
}

- (BOOL) unregisterAllWithDeviceToken:(NSData*)deviceTokenData error:(NSError**)error
{
    if( deviceTokenData == nil)
    {
        if(error)
        {
            *error = [SBNotificationHubHelper errorForNullDeviceToken];
        }
        
        return FALSE;
    }
    
    NSString* deviceToken = [self convertDeviceToken:deviceTokenData];
    
    NSError* operationError;
    NSArray* registrations = [self retrieveAllRegistrationsWithDeviceToken:deviceToken error:&operationError];
    
    if( operationError )
    {
        if(error)
        {
            *error = operationError;
        }
        
        return FALSE;
    }
    
    for (SBRegistration* reg in registrations) {
        NSString* name = [SBNotificationHubHelper nameOfRegistration:reg];
        [self deleteRegistrationWithName:name error:&operationError];
        if(operationError)
        {
            if(error)
            {
                *error = operationError;
            }
            
            return FALSE;
        }
    }
    
    // Remove any registrations that are only in the client and not on server
    [storageManager deleteAllRegistrations];
    
    return TRUE;
}

- (BOOL) verifyTemplateName:(NSString*)name error:(NSError**)error
{
     if( [name isEqualToString: [SBRegistration Name]])
     {
         if(error)
         {
             *error = [SBNotificationHubHelper errorForReservedTemplateName];
         }
         
         return FALSE;
     }
    
    NSRange range = [name rangeOfString:@":"];
    if (range.length > 0)
    {
        if(error)
        {
            *error = [SBNotificationHubHelper errorForInvalidTemplateName];
        }
    }
    
    return TRUE;
}

- (void) registrationOperationWithRequestUri:(NSURL*)requestUri payload:(NSString*)payload httpMethod:(NSString*) httpMethod ETag:(NSString*)etag completion:(void (^)(NSHTTPURLResponse *response, NSData *data, NSError *error))completion
{
    NSMutableURLRequest *theRequest = [self PrepareUrlRequest:requestUri httpMethod:httpMethod ETag:etag payload:payload];
    
    [tokenProvider setTokenWithRequest:theRequest completion:^(NSError *error) {
        if(error)
        {
            if(completion)
            {
                completion(nil,nil,error);
            }
            
            return;
        }
        
        [[[SBURLConnection alloc] init] sendRequest:theRequest completion:completion];
    } ];
}

- (BOOL) registrationOperationWithRequestUri:(NSURL*)requestUri payload:(NSString*)payload httpMethod:(NSString*) httpMethod ETag:(NSString*)etag response:(NSHTTPURLResponse**)response responseData:(NSData**)responseData error:(NSError**)error
{
    NSMutableURLRequest *theRequest = [self PrepareUrlRequest:requestUri httpMethod:httpMethod ETag:etag payload:payload];
    
    [tokenProvider setTokenWithRequest:theRequest error:error];
    if(*error != nil)
    {
        return FALSE;
    }
    
    //send synchronously
    (*responseData) = [[[SBURLConnection alloc] init] sendSynchronousRequest:theRequest returningResponse:response error:error];
    
    if(*error != nil)
    {
        NSLog(@"Fail to perform registration operation.");
        NSLog(@"%@",[theRequest description]);
        NSLog(@"Headers:%@",[theRequest allHTTPHeaderFields]);
        NSLog(@"Error Response:%@",[[NSString alloc]initWithData:(*responseData) encoding:NSUTF8StringEncoding]);
        
        return FALSE;
    }
    else
    {
        NSInteger statusCode = [(*response) statusCode];
        if( statusCode != 200 && statusCode != 201)
        {
            NSString* responseString = [[NSString alloc]initWithData:(*responseData) encoding:NSUTF8StringEncoding];
            
            if(statusCode != 404)
            {
                NSLog(@"Fail to perform registration operation.");
                NSLog(@"%@",[theRequest description]);
                NSLog(@"Headers:%@",[theRequest allHTTPHeaderFields]);
                NSLog(@"Error Response:%@",responseString);
            }
          
            if(error)
            {
                NSString* msg = [NSString stringWithFormat:@"Fail to perform registration operation. Response:%@",responseString];
                
                (*error) = [SBNotificationHubHelper errorWithMsg:msg code:statusCode];
            }
             
            return FALSE;
        }
    }
    
    return TRUE;
}

- (NSURL*) composeRetrieveAllRegistrationsUriWithDeviceToken:(NSString*)deviceToken
{
    NSString* fullPath = [NSString stringWithFormat:@"%@%@/Registrations/?$filter=deviceToken+eq+'%@'&api-version=%@", [self->_serviceEndPoint absoluteString],self->_path, deviceToken, _APIVersion];
    
    return[[NSURL alloc] initWithString: fullPath];
}

- (NSURL*) composeRegistrationUriWithRegistrationId:(NSString*)registrationId
{
    if( registrationId == nil)
    {
        registrationId = @"";
    }
    
    NSString* fullPath = [NSString stringWithFormat:@"%@%@/Registrations/%@?api-version=%@", [self->_serviceEndPoint absoluteString],self->_path, registrationId, _APIVersion];
   
    return[[NSURL alloc] initWithString: fullPath];
}

- (NSURL*) composeCreateRegistrationIdUri
{
    NSString* fullPath = [NSString stringWithFormat:@"%@%@/registrationids/?api-version=%@", [self->_serviceEndPoint absoluteString],self->_path, _APIVersion];
    
    return[[NSURL alloc] initWithString: fullPath];
}

- (NSMutableURLRequest *)PrepareUrlRequest:(NSURL *)uri httpMethod:(NSString *)httpMethod ETag:(NSString*)etag payload:(NSString *)payload {
    NSMutableURLRequest *theRequest;
    theRequest = [NSMutableURLRequest requestWithURL:uri
                                         cachePolicy:NSURLRequestReloadIgnoringLocalCacheData
                                     timeoutInterval:60.0];
    [theRequest setHTTPMethod:httpMethod];
    
    if( [payload hasPrefix:@"{"])
    {
        [theRequest setValue:@"application/json" forHTTPHeaderField:@"Content-Type"];   
    }
    else
    {
        [theRequest setValue:@"application/xml" forHTTPHeaderField:@"Content-Type"];
    }
    
    if( etag != nil && [etag length]>0)
    {
        if( ![etag isEqualToString:@"*"])
        {
            etag = [NSString stringWithFormat:@"\"%@\"",etag];
        }
        
        [theRequest addValue: etag forHTTPHeaderField: @"If-Match"];
    }
    
    if( [payload length]>0){
        NSString* requestBody = [NSString stringWithFormat:@"%@",payload];
        [theRequest setHTTPBody:[requestBody dataUsingEncoding:NSUTF8StringEncoding]];
    }
    
	
	NSString *userAgent = [NSString stringWithFormat: _UserAgentTemplate, _APIVersion, [[UIDevice currentDevice] systemName], [[UIDevice currentDevice] systemVersion]];
	[theRequest addValue:userAgent forHTTPHeaderField:@"User-Agent"];
	
    return theRequest;
}

// if there is deviceToken from localstorage, we should use it.
- (NSString*) getRefreshDeviceTokenWithNewDeviceToken:(NSString*)newDeviceToken
{
    NSString* deviceToken = [storageManager deviceToken];
    if( deviceToken == nil || [deviceToken length]==0 )
    {
        return newDeviceToken;
    }
    else
    {
        return deviceToken;
    }
}

+ (NSString*) version
{
    return currentVersion;
}

-(NSString*) extractRegistrationIdFromLocationUri:(NSURL*) locationUrl
{
    NSMutableCharacterSet *trimCharacterSet = [NSMutableCharacterSet whitespaceAndNewlineCharacterSet];
    [trimCharacterSet addCharactersInString:@"/"];
    NSString *regisrationIdPath = [locationUrl.path stringByTrimmingCharactersInSet:trimCharacterSet];
    NSRange lastIndex = [regisrationIdPath rangeOfString:@"/" options:NSBackwardsSearch];
    NSString *registrationId = [regisrationIdPath substringFromIndex:lastIndex.location+1];
    
    return registrationId;
}

@end
