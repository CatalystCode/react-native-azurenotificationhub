//----------------------------------------------------------------
//  Copyright (c) Microsoft Corporation. All rights reserved.
//----------------------------------------------------------------

#import "SBURLConnection.h"
#import "SBNotificationHubHelper.h"

@implementation SBURLConnection

StaticHandleBlock _staticHandler;

+ (void) setStaticHandler:(StaticHandleBlock)staticHandler
{
    _staticHandler = staticHandler;
}

- (void) sendRequest: (NSURLRequest*) request completion:(void (^)(NSHTTPURLResponse*,NSData*,NSError*))completion;
{
    if( self){
        self->_request = request;
        self->_completion = completion;
    }
 
    if( _staticHandler != nil)
    {
        SBStaticHandlerResponse* mockResponse = _staticHandler(request);
        if( mockResponse != nil)
        {
            if(completion)
            {
                NSHTTPURLResponse* response = [[NSHTTPURLResponse alloc] initWithURL:[request URL] statusCode:200 HTTPVersion:nil headerFields:mockResponse.Headers];
                completion(response,mockResponse.Data,nil);
            }
            return;
        }
    }
    
    NSURLConnection *theConnection = [[NSURLConnection alloc] initWithRequest:request delegate:self];
    if(!theConnection && completion)
    {
        NSString* msg = [NSString stringWithFormat:@"Initiate request failed for %@",[request description]];
        completion(nil,nil,[SBNotificationHubHelper errorWithMsg:msg code:-1]);
    }
}

- (NSData *)sendSynchronousRequest:(NSURLRequest *)request returningResponse:(NSURLResponse **)response error:(NSError **)error
{
    if( _staticHandler != nil)
    {
        SBStaticHandlerResponse* mockResponse  = _staticHandler(request);
        if( mockResponse != nil)
        {
            (*response) = [[NSHTTPURLResponse alloc] initWithURL:[request URL] statusCode:200 HTTPVersion:nil headerFields:mockResponse.Headers];
            
            return mockResponse.Data;
        }
    }
    
    return [NSURLConnection sendSynchronousRequest:request returningResponse:response error:error];
}

- (void)connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response
{
    if(!self->_completion)
    {
        return;
    }
    
    self->_response = (NSHTTPURLResponse*)response;
    
    NSInteger statusCode = [self->_response statusCode];
    if( statusCode != 200 && statusCode != 201)
    {
        if(statusCode != 404)
        {
            NSLog(@"URLRequest failed:");
            NSLog(@"URL:%@",[[self->_request URL] absoluteString]);
            NSLog(@"Headers:%@",[self->_request allHTTPHeaderFields]);
        }
        
        NSString* msg = [NSString stringWithFormat:@"URLRequest failed for %@ with status code: %@",[self->_request description], [NSHTTPURLResponse localizedStringForStatusCode:statusCode]];

        self->_completion(self->_response,nil,[SBNotificationHubHelper errorWithMsg:msg code:statusCode]);
        self->_completion = nil;
        return;
    }
    
    if([[self->_request HTTPMethod] isEqualToString:@"DELETE"])
    {
        self->_completion(self->_response,nil,nil);
        self->_completion =nil;
        return;
    }

    // if it's create registration id request, we need to execute callback here. 
    if([[self->_request HTTPMethod] isEqualToString:@"POST"] && [[self->_response URL].path rangeOfString:@"/registrationids" options:NSCaseInsensitiveSearch].location != NSNotFound)
    {
        self->_completion(self->_response,nil,nil);
        self->_completion =nil;
        return;
    }
    
    _data = [[NSMutableData alloc]init];
}

- (void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)data
{
    [_data appendData:data];
}

-(void) connectionDidFinishLoading:(NSURLConnection *)connection
{
    if( self->_completion)
    {
        self->_completion(self->_response,_data,nil);
        self->_completion = nil;
    }
    _data = nil;

}

- (void)connection:(NSURLConnection*)connection didFailWithError:(NSError *)error
{
    if(self->_completion)
    {
        self->_completion(self->_response,nil,error);
        self->_completion = nil;
    }
}

@end
