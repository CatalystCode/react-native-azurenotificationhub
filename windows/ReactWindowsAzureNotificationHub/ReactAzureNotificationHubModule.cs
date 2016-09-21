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
        private const string ErrorInvalidArguments = "E_INVALID_ARGUMENTS";

        private string _connectionString;
        private string _hubName;

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
        public async void register(JObject config, IPromise promise)
        {
            _connectionString = config.Value<string>("connectionString");
            _hubName = config.Value<string>("hubName");
            var tags = config["tags"]?.ToObject<string[]>();

            AssertArguments(promise);

            try
            {
                var channel = await PushNotificationChannelManager.CreatePushNotificationChannelForApplicationAsync().AsTask().ConfigureAwait(false);
                var hub = new NotificationHub(_hubName, _connectionString);
                var result = await hub.RegisterNativeAsync(channel.Uri, tags).ConfigureAwait(false);
                if (result.RegistrationId != null)
                {
                    promise.Resolve(result.RegistrationId);
                }
                else
                {
                    promise.Reject(RegistrationFailure, "Registration was not successful.");
                }
            }
            catch (Exception ex)
            {
                promise.Reject(ex);
            }
        }

        [ReactMethod]
        public async void unregister(IPromise promise)
        {
            AssertArguments(promise);

            try
            {
                var hub = new NotificationHub(_hubName, _connectionString);
                await hub.UnregisterNativeAsync();
                promise.Resolve(true);
            }
            catch (Exception ex)
            {
                promise.Reject(ex);
            }
        }

        private void AssertArguments(IPromise promise)
        {
            if (_connectionString == null)
            {
                promise.Reject(ErrorInvalidArguments, "Connection string cannot be null.");
            }

            if (_hubName == null)
            {
                promise.Reject(ErrorInvalidArguments, "Hub name cannot be null.");
            }
        }
    }
}
