//----------------------------------------------------------------
//  Copyright (c) Microsoft Corporation. All rights reserved.
//----------------------------------------------------------------

#import "SBConnectionString.h"
#import "SBNotificationHubHelper.h"

@implementation SBConnectionString

NSString* const defaultListenSasRuleName = @"DefaultListenSharedAccessSignature";
NSString* const defaultFullSasRuleName = @"DefaultFullSharedAccessSignature";


+ (NSString*) stringWithEndpoint:(NSURL*)endpoint issuer:(NSString*) issuer issuerSecret:(NSString*)secret
{
    if(!endpoint || !issuer || !secret)
    {
        NSLog(@"endpoint/issuer/secret can't be null.");
        return nil;
    }
    
    endpoint = [SBNotificationHubHelper modifyEndpoint:endpoint scheme:@"sb"];
    
    NSString* endpointUri = [endpoint absoluteString];
    if([[endpointUri lowercaseString] hasPrefix:@"endpoint="])
    {
        return [NSString stringWithFormat:@"%@;SharedSecretIssuer=%@;SharedSecretValue=%@",endpointUri,issuer,secret];
    }
    else
    {
        return [NSString stringWithFormat:@"Endpoint=%@;SharedSecretIssuer=%@;SharedSecretValue=%@",endpointUri,issuer,secret];
    }
}

+ (NSString*) stringWithEndpoint:(NSURL*)endpoint fullAccessSecret:(NSString*)fullAccessSecret
{
    return [SBConnectionString stringWithEndpoint:endpoint sharedAccessKeyName:defaultFullSasRuleName accessSecret:fullAccessSecret];
}

+ (NSString*) stringWithEndpoint:(NSURL*)endpoint listenAccessSecret:(NSString*)listenAccessSecret
{
    return [SBConnectionString stringWithEndpoint:endpoint sharedAccessKeyName:defaultListenSasRuleName accessSecret:listenAccessSecret];
}

+ (NSString*) stringWithEndpoint:(NSURL*)endpoint sharedAccessKeyName:(NSString*)keyName accessSecret:(NSString*)secret
{
    if(!endpoint || !keyName || !secret)
    {
        NSLog(@"endpoint/keyName/secret can't be null.");
        return nil;
    }
    
    endpoint = [SBNotificationHubHelper modifyEndpoint:endpoint scheme:@"sb"];
    
    NSString* endpointUri = [endpoint absoluteString];
    
    if([[endpointUri lowercaseString] hasPrefix:@"endpoint="])
    {
        return [NSString stringWithFormat:@"%@;SharedAccessKeyName=%@;SharedAccessKey=%@",endpointUri,keyName,secret];
    }
    else
    {
        return [NSString stringWithFormat:@"Endpoint=%@;SharedAccessKeyName=%@;SharedAccessKey=%@",endpointUri,keyName,secret];
    }
}

@end
