//----------------------------------------------------------------
//  Copyright (c) Microsoft Corporation. All rights reserved.
//----------------------------------------------------------------


#import "SBStoredRegistrationEntry.h"

@implementation StoredRegistrationEntry
@synthesize RegistrationName, RegistrationId, ETag;

- (StoredRegistrationEntry*) initWithString:(NSString*)string
{
    self = [super init];
    
    if(string == nil)
    {
        return nil;
    }
    
    if( self){
        NSArray *entries = [string componentsSeparatedByString:@":"];
        if( [entries count] == 3)
        {
            self->RegistrationName = [entries objectAtIndex:0];
            self->RegistrationId = [entries objectAtIndex:1];
            self->ETag = [entries objectAtIndex:2];
        }
    }
    
    return self;
}

- (NSString*) toString
{
    return [NSString stringWithFormat:@"%@:%@:%@",self->RegistrationName,self->RegistrationId, self->ETag];
}

@end
