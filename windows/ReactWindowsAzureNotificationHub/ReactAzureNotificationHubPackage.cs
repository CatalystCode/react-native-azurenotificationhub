using ReactNative.Bridge;
using ReactNative.Modules.Core;
using ReactNative.UIManager;
using System;
using System.Collections.Generic;

namespace ReactWindowsAzureNotificationHub
{
    public class ReactAzureNotificationHubPackage : IReactPackage
    {
        public IReadOnlyList<Type> CreateJavaScriptModulesConfig()
        {
            return new List<Type>(0);
        }

        public IReadOnlyList<INativeModule> CreateNativeModules(ReactContext reactContext)
        {
            return new List<INativeModule>
            {
                new ReactAzureNotificationHubModule(reactContext),
            };
        }

        public IReadOnlyList<IViewManager> CreateViewManagers(ReactContext reactContext)
        {
            return new List<IViewManager>(0);
        }
    }
}
