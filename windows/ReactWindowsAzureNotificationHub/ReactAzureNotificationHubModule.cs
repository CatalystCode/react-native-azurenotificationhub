using Microsoft.WindowsAzure.Messaging;
using Newtonsoft.Json.Linq;
using ReactNative.Bridge;
using System;
using Windows.Networking.PushNotifications;

namespace ReactWindowsAzureNotificationHub
{
    class ReactAzureNotificationHubModule : ReactContextNativeModuleBase
    {
        private const string RegistrationFailure = "E_REGISTRATION_FAILED";

        public ReactAzureNotificationHubModule(ReactContext reactContext) 
            : base(reactContext)
        {
        }

        public override string Name
        {
            get
            {
                return "AzureNotificationHub";
            }
        }

        [ReactMethod]
        public async void register(JObject config, IPromise callback)
        {
            try
            {
                var hubName = config.Value<string>("hubName");
                var connectionString = config.Value<string>("connectionString");
                var channel = await PushNotificationChannelManager.CreatePushNotificationChannelForApplicationAsync().AsTask().ConfigureAwait(false);
                var hub = new NotificationHub(hubName, connectionString);
                var result = await hub.RegisterNativeAsync(channel.Uri).ConfigureAwait(false);

                if (result.RegistrationId != null)
                {
                    callback.Resolve(result.RegistrationId);
                }
                else
                {
                    callback.Reject(RegistrationFailure, "Registration was not successful.");
                }
            }
            catch (Exception ex)
            {
                callback.Reject(ex);
            }
        }

        [ReactMethod]
        public async void unregister(JObject config, IPromise callback)
        {
            try
            {
                var hubName = config.Value<string>("hubName");
                var connectionString = config.Value<string>("connectionString");
                var hub = new NotificationHub(hubName, connectionString);
                await hub.UnregisterNativeAsync();
                callback.Resolve(true);
            }
            catch (Exception ex)
            {
                callback.Reject(ex);
            }
        }
    }
}
