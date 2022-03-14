declare namespace AzureNotificationHub {
    export interface RegistrationConfig {
        connectionString: string;
        hubName: string;
        senderID: string;
        tags: string[];
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

    export interface NotificationData {
        message: string;
        body?: string;
        title: string;
        ticker?: string;
        autoCancel?: boolean;
        group?: string;
        largeIcon?: string;
        subText?: string;
        number?: string;
        smallIcon?: string;
        bigText?: string;
        playSound?: boolean;
        soundName?: string;
        ongoing?: boolean;
        color?: string;
        vibrate?: boolean;
        vibration?: string;
        foreground?: boolean;
        fullScreenIntent?: boolean;
        actions?: string;
        action?: string;
        tag?: string;
        avatarUrl?: string;
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
    static scheduleLocalNotification(notification: NotificationData, whenMs: string): Promise<number>;
    static cancelScheduledNotification(notificationId: number);
}

export = AzureNotificationHub;
