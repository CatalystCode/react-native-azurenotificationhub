//----------------------------------------------------------------
//  Copyright (c) Microsoft Corporation. All rights reserved.
//----------------------------------------------------------------

#import "SBTokenProvider.h"
#import "SBNotificationHubHelper.h"
#import "SBURLConnection.h"

@implementation SBTokenProvider

const int defaultTimeToExpireinMins = 20;

@synthesize timeToExpireinMins;

- (SBTokenProvider*) initWithConnectionDictinary: (NSDictionary*) connectionDictionary
{
    self = [super init];
    
    if( self){
        if(![self initMembersWithDictionary:connectionDictionary])
        {
            return nil;
        }
    }
    
    return self;
}

- (BOOL)initMembersWithDictionary:(NSDictionary*) connectionDictionary
{
    self->timeToExpireinMins = defaultTimeToExpireinMins;
    
    NSString* endpoint = [connectionDictionary objectForKey:@"endpoint"];
    if( endpoint)
    {
        self->_serviceEndPoint = [[NSURL alloc] initWithString:endpoint];
    }
    
    NSString* stsendpoint = [connectionDictionary objectForKey:@"stsendpoint"];
    if( stsendpoint)
    {
        self->_stsHostName = [[NSURL alloc] initWithString:stsendpoint];
    }
    
    self->_sharedAccessKey = [connectionDictionary objectForKey:@"sharedaccesskey"];
    self->_sharedAccessKeyName = [connectionDictionary objectForKey:@"sharedaccesskeyname"];
    self->_sharedSecret = [connectionDictionary objectForKey:@"sharedsecretvalue"];
    self->_sharedSecretIssurer = [connectionDictionary objectForKey:@"sharedsecretissuer"];
    
    // validation
    if(self->_serviceEndPoint == nil ||
       [self->_serviceEndPoint host] == nil)
    {
        NSLog(@"%@",@"Endpoint is missing or not in URL format in connectionString.");
        return FALSE;
    }
    
    if((self->_sharedAccessKey == nil || self->_sharedAccessKeyName == nil) &&
       self->_sharedSecret == nil)
    {
        NSLog(@"%@",@"Security information is missing in connectionString.");
        return FALSE;
    }
    
    if(self->_stsHostName == nil)
    {
        NSString* nameSpace = [[[self->_serviceEndPoint host] componentsSeparatedByString:@"."] objectAtIndex:0];
        self->_stsHostName = [[NSURL alloc] initWithString:[NSString stringWithFormat:@"https://%@-sb.accesscontrol.windows.net",nameSpace]];
    }
    else
    {
        if([self->_stsHostName host] == nil)
        {
            NSLog(@"%@",@"StsHostname is not in URL format in connectionString.");
            return FALSE;
        }
        
        self->_stsHostName = [[NSURL alloc] initWithString:[NSString stringWithFormat:@"https://%@",[self->_stsHostName host]]];
    }
    
    if(self->_sharedSecret && !self->_sharedSecretIssurer )
    {
        self->_sharedSecretIssurer = @"owner";
    }
    
    return TRUE;
}

- (BOOL) setTokenWithRequest:(NSMutableURLRequest*)request error:(NSError**)error
{
    NSString *token;
        
    if( [self->_sharedAccessKey length] > 0)
    {
        token = [self PrepareSharedAccessTokenWithUrl:[request URL]];
    }
    else
    {
        NSMutableURLRequest *stsRequest = [self PrepareSharedSecretTokenWithUrl:[request URL] ];
        NSHTTPURLResponse* response = nil;
        NSError* requestError;
        NSData* data = [[[SBURLConnection alloc] init] sendSynchronousRequest:stsRequest returningResponse:&response error:&requestError];
    
        if(requestError && error)
        {
            NSLog(@"Fail to request token:");
            NSLog(@"Response:%@",[[NSString alloc]initWithData:data encoding:NSUTF8StringEncoding]);
        
            (*error) = requestError;
            return FALSE;
        }
        else
        {
            NSInteger statusCode = [response statusCode];
            if( statusCode != 200 && statusCode != 201)
            {
                NSString* msg = [NSString stringWithFormat:@"Fail to request token. Response:%@",[[NSString alloc]initWithData:data encoding:NSUTF8StringEncoding]];
                NSLog(@"%@",msg);
            
                if(error)
                {
                    (*error) = [SBNotificationHubHelper errorWithMsg:msg code:statusCode];
                }
        
                return FALSE;
            }
        }
    
        token = [SBTokenProvider ExtractToken:data];
    }
    
    [request addValue: token forHTTPHeaderField: @"Authorization"];
    return TRUE;
}

- (NSString *)PrepareSharedAccessTokenWithUrl:(NSURL*)url
{
    // time to live in seconds
    NSTimeInterval interval = [[NSDate date] timeIntervalSince1970];
    int totalSeconds = interval + self->timeToExpireinMins*60;
    NSString* expiresOn = [NSString stringWithFormat:@"%d", totalSeconds];
    
    NSString* audienceUri = [url absoluteString];
    audienceUri = [[audienceUri lowercaseString] stringByReplacingOccurrencesOfString:@"https://" withString:@"http://"];
    audienceUri = [[SBNotificationHubHelper urlEncode:audienceUri] lowercaseString];
    
    NSString* signature = [SBNotificationHubHelper signString:[audienceUri stringByAppendingFormat:@"\n%@",expiresOn] withKey:self->_sharedAccessKey];
    signature = [SBNotificationHubHelper urlEncode:signature];
    
    NSString* token = [NSString stringWithFormat:@"SharedAccessSignature sr=%@&sig=%@&se=%@&skn=%@", audienceUri, signature, expiresOn, self->_sharedAccessKeyName];
    
    return token;
}

- (NSMutableURLRequest *)PrepareSharedSecretTokenWithUrl:(NSURL*)url
{
    NSString* audienceUri = [[url absoluteString] lowercaseString];
    NSString* query = [url query];
    if(query)
    {
        audienceUri = [audienceUri substringToIndex:([audienceUri length] - [query length]-1) ];
    }
    
    audienceUri = [audienceUri stringByReplacingOccurrencesOfString:@"https://" withString:@"http://"];
    audienceUri = [SBNotificationHubHelper urlEncode:audienceUri];
    
    //compute simpleWebToken
    NSString* issurerStr = [NSString stringWithFormat:@"Issuer=%@", self->_sharedSecretIssurer];
    NSData* secretBase64 = [SBNotificationHubHelper fromBase64:self->_sharedSecret];
    
    NSString* signature = [SBNotificationHubHelper signString:issurerStr withKeyData:secretBase64.bytes keyLength:secretBase64.length];
    signature = [SBNotificationHubHelper urlEncode:signature];
    
    NSString* simpleWebToken = [NSString stringWithFormat:@"Issuer=%@&HMACSHA256=%@",self->_sharedSecretIssurer,signature];
    simpleWebToken = [SBNotificationHubHelper urlEncode:simpleWebToken];
    
    NSString* requestBody = [NSString stringWithFormat:@"wrap_scope=%@&wrap_assertion_format=SWT&wrap_assertion=%@", audienceUri, simpleWebToken];
    
    //send request
    NSURL* stsUri = [[NSURL alloc] initWithString:[NSString stringWithFormat:@"%@/WRAPv0.9/",_stsHostName]];
    NSMutableURLRequest *stsRequest=[NSMutableURLRequest requestWithURL:stsUri
                                                            cachePolicy:NSURLRequestReloadIgnoringLocalCacheData
                                                        timeoutInterval:60.0];
    [stsRequest setHTTPMethod:@"POST"];
    [stsRequest setValue:@"application/x-www-form-urlencoded" forHTTPHeaderField:@"Content-Type"];
    [stsRequest setValue:@"0" forHTTPHeaderField:@"ContentLength"];
    [stsRequest setHTTPBody:[requestBody dataUsingEncoding:NSUTF8StringEncoding]];
    return stsRequest;
}

- (void) setTokenWithRequest:(NSMutableURLRequest*)request completion:(void (^)(NSError*))completion
{
    if( [self->_sharedAccessKey length] > 0)
    {
        [self setSharedAccessTokenWithRequest:request completion:completion];
    }
    else
    {
        [self setSharedSecretTokenAsync:request completion:completion];
    }
}

- (void) setSharedAccessTokenWithRequest: (NSMutableURLRequest*)request completion:(void (^)(NSError*))completion
{
    NSString *token = [self PrepareSharedAccessTokenWithUrl:[request URL]];
    [request addValue: token forHTTPHeaderField: @"Authorization"];
    
    if(completion)
    {
        completion(nil);
    }
}

- (void) setSharedSecretTokenAsync: (NSMutableURLRequest*)request completion:(void (^)(NSError*))completion
{
    NSMutableURLRequest *stsRequest = [self PrepareSharedSecretTokenWithUrl:[request URL]];
    
    [[[SBURLConnection alloc] init] sendRequest:stsRequest completion:^(NSHTTPURLResponse *response, NSData *data, NSError *error){
        if(error)
        {
            if(completion)
            {
                completion(error);
            }
            return;
        }
        
        NSString* responseString = @"";
        if(data)
        {
            responseString = [[NSString alloc]initWithData:data encoding:NSUTF8StringEncoding];
        }
        
        
        NSInteger statusCode = [response statusCode];
        if( statusCode != 200 && statusCode != 201)
        {
            NSLog(@"Fail to retrieve token.");
            NSLog(@"%@",[request description]);
            NSLog(@"Headers:%@",[request allHTTPHeaderFields]);
            NSLog(@"Error Response:%@",responseString);
 
            if(completion)
            {
                NSString* msg = [NSString stringWithFormat:@"Fail to retrieve token. Response:%@",responseString];
                completion([SBNotificationHubHelper errorWithMsg:msg code:statusCode]);
            }
            return;
        }
        
        NSString* token = [SBTokenProvider ExtractToken:data];
        
        if(completion)
        {
            if( [token length] == 0)
            {
                NSString* msg = [NSString stringWithFormat:@"Fail to parse token. Response:%@",responseString];
                completion([SBNotificationHubHelper errorWithMsg:msg code:-1]);
            }
            else
            {
                [request addValue: token forHTTPHeaderField: @"Authorization"];
                completion(nil);
            }
        }
    } ];
}

+ (NSString *)ExtractToken:(NSData *)data
{
    NSString *expireInSeconds;
    NSString *token;
    NSString* rawStr= [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    NSArray *fields = [rawStr componentsSeparatedByString:@"&"];
    
    //Check size..
    if([fields count] != 2)
    {
        NSLog(@"Wrong format of received token:%@",rawStr);
        return @"";
    }
    
    for (NSString* item in fields) {
        NSArray *subItems = [item componentsSeparatedByString:@"="];
        NSString* key = [subItems objectAtIndex:0];
        NSString* value = [SBNotificationHubHelper urlDecode:[subItems objectAtIndex:1]];
        if([key isEqualToString:@"wrap_access_token"])
        {
            token = [NSString stringWithFormat:@"WRAP access_token=\"%@\"",value];
        }
        else
        {
            expireInSeconds = value;
        }
    }
    
    return token;
}

@end



