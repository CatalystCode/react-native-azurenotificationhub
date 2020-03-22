declare namespace IOSNotification {
    export interface RegistrationInfo {
        success: boolean;
        registrationId?: string;
    }

    export interface RegistrationConfig {
        connectionString: string;
        hubName: string;
        tags?: string[];
    }

    export interface IOSAlert {
        body?: string;
        action?: string;
        title?: string;
        hasAction?: boolean;
        alertLaunchImage?: string;
        category?: string;

        [key: string]: any;
    }

    export interface TemplateRegistrationConfig extends RegistrationConfig {
        // JSON serialized template
        template: string;
        // Unique name for this template
        templateName: string;
    }

    export interface RegistrationError {
        message: string;
        code: number;
        details: any;
    }

    export interface AllowedPermissions {
        alert: boolean;
        badge: boolean;
        sound: boolean;
    }

    type NotificationUserInfo = {[key: string]: string | number | null};
    type PushNotificationEventName =
        /**
         * Fired when a remote notification is received. The handler will be invoked
         * with an instance of `IOSNotification`.
         */
        'notification' |
        /**
         * Fired when a local notification is received. The handler will be invoked
         * with an instance of `IOSNotification`.
         */
        'localNotification' |
        /**
         * Fired when the user registers for remote notifications. The handler will be
         * invoked with a hex string representing the deviceToken.
         */
        'register' |
        /**
         * Fired when the user fails to register for remote notifications. Typically
         * occurs when APNS is having issues, or the device is a simulator. The
         * handler will be invoked with {message: string, code: number, details: any}.
         */
        'registrationError' |
        /**
         * Fired when the user registers for Azure notification hub. The handler will be
         * invoked with the connection string and hub name.
         */
        'registerAzureNotificationHub' |
        /**
         * Fired when the user fails to register for Azure notification hub.
         */
        'azureNotificationHubRegistrationError';

    interface LocalNotification {
        alertBody: string;
        alertAction: string;
        soundName?: string;
        category?: string;
        userInfo?: NotificationUserInfo;
        applicationIconBadgeNumber?: number;
        remote?: boolean;
    }

    interface FutureLocalNotification extends LocalNotification {
        fireDate: Date;
    }
}
// TODO: Generic the data if possible. Figure out registrationInfo

declare class IOSNotification {
    private _data: any;
    private _alert: string | IOSNotification.IOSAlert;
    private _sound: string;
    private _badgeCount: number;

    static presentLocalNotification(details: IOSNotification.LocalNotification): void;
    static scheduleLocalNotification(details: IOSNotification.FutureLocalNotification): void;
    static cancelAllLocalNotifications(): void;
    static setApplicationIconBadgeNumber(number: number): void;
    static getApplicationIconBadgeNumber(callback: (number) => void): void;
    static cancelLocalNotifications(userInfo?: Partial<IOSNotification.NotificationUserInfo>): void;
    static getScheduledLocalNotifications(callback: (notifications: IOSNotification.FutureLocalNotification[]) => void): void;

    static addEventListener(name: 'notification', cb: (notification: IOSNotification) => void): void;
    static addEventListener(name: 'localNotification', cb: (notification: IOSNotification) => void): void;
    static addEventListener(name: 'register', cb: (deviceToken: string) => void): void;
    static addEventListener(name: 'registrationError', cb: (err: IOSNotification.RegistrationError) => void): void;
    static addEventListener(name: 'registerAzureNotificationHub', cb: (registrationInfo: IOSNotification.RegistrationInfo) => void): void;
    static addEventListener(name: 'azureNotificationHubRegistrationError', cb: (err: IOSNotification.RegistrationError) => void): void;
    static removeEventListener(type: IOSNotification.PushNotificationEventName, handler: Function): void;

    static requestPermissions(permissions?: Partial<IOSNotification.AllowedPermissions>): Promise<IOSNotification.AllowedPermissions>;
    static abandonPermissions(): void;
    static checkPermissions(callback: (permissions: IOSNotification.AllowedPermissions) => void): void;

    static register(deviceToken: string, config: IOSNotification.RegistrationConfig): void;
    static registerTemplate(deviceToken: string, config: IOSNotification.TemplateRegistrationConfig): void;
    static unregister(): Promise<void>;
    static unregisterTemplate(templateName: string): Promise<void>;

    static getInitialNotification(): Promise<IOSNotification>;

    public getAlert(): string | IOSNotification.IOSAlert;
    public getMessage(): string | IOSNotification.IOSAlert;
    public getSound(): string;
    public getBadgeCount(): number;
    public getData(): any;
}

export = IOSNotification;
