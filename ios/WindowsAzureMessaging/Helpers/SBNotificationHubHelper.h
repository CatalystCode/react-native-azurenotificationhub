//----------------------------------------------------------------
//  Copyright (c) Microsoft Corporation. All rights reserved.
//----------------------------------------------------------------

#import <Foundation/Foundation.h>
#import "SBRegistration.h"

@interface SBNotificationHubHelper : NSObject

+ (NSString*) urlEncode: (NSString*)urlString;
+ (NSString*) urlDecode: (NSString*)urlString;

+ (NSString*) createHashWithData:(NSData*)data;

+ (NSString*) signString: (NSString*)str withKey:(NSString*) key;
+ (NSString*) signString: (NSString*)str withKeyData:(const char*) cKey keyLength:(NSInteger) keyLength;

+ (NSData*) fromBase64: (NSString*) str;
+ (NSString*) toBase64: (unsigned char*) data length:(NSInteger) length;

+ (NSString*) convertTagSetToString:(NSSet*)tagSet;

+ (NSError*) errorWithMsg:(NSString*)msg code:(NSInteger)code;

+ (NSError*) errorForNullDeviceToken;

+ (NSError*) errorForReservedTemplateName;

+ (NSError*) errorForInvalidTemplateName;

+ (NSError*) registrationNotFoundError;

+ (NSDictionary*) parseConnectionString:(NSString*) connectionString;

+ (NSURL*) modifyEndpoint:(NSURL*)endPoint scheme:(NSString*)scheme;

+ (NSString*) nameOfRegistration:(SBRegistration*)registration;

@end


