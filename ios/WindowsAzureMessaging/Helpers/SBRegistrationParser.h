//----------------------------------------------------------------
//  Copyright (c) Microsoft Corporation. All rights reserved.
//----------------------------------------------------------------

#import "SBRegistration.h"

@interface SBRegistrationParser : NSObject{
    @private
    NSMutableArray *_allRegistrations;
    NSMutableString *_currentElementValue;
    SBRegistration *_currentRegistration;
}

- (SBRegistrationParser*) initParserWithResult:(NSMutableArray*)result;

+ (NSArray *)parseRegistrations:(NSData *)data error:(NSError **)error;
@end
