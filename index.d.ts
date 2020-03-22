declare namespace AzureNotificationHub {
    export interface RegistrationConfig {
        connectionString: string;
        hubName: string;
        senderID: string;
        tags?: string[];
        channelName: string;
        channelImportance: boolean;
        channelShowBadge: boolean;
        channelEnableLights: boolean;
        channelEnableVibration: boolean;
    }

    export interface TemplateRegistrationConfig extends RegistrationConfig {
        templateName: string;
        template: string;
    }

    export interface RegistrationResponse {
        uuid: string;
    }
}

declare class AzureNotificationHub {
    static register(config: AzureNotificationHub.RegistrationConfig): Promise<AzureNotificationHub.RegistrationResponse>;
    static registerTemplate(config: AzureNotificationHub.TemplateRegistrationConfig): Promise<AzureNotificationHub.RegistrationResponse>;
    static unregister(): Promise<void>;
    static unregisterTemplate(templateName: string): Promise<void>;
    static getUUID(autoGen: boolean): Promise<string>;
    static getInitialNotification<T>(): Promise<T>;
    static isNotificationEnabledOnOSLevel(): Promise<boolean>;
}

export = AzureNotificationHub;
