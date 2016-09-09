using ReactNative;
using ReactNative.Modules.Core;
using ReactNative.Shell;
using ReactWindowsAzureNotificationHub;
using System.Collections.Generic;

namespace Playground
{
    class MainPage : ReactPage
    {
        public override string MainComponentName
        {
            get
            {
                return "Playground";
            }
        }

        public override string JavaScriptMainModuleName
        {
            get
            {
                return "Playground";
            }
        }

        public override List<IReactPackage> Packages
        {
            get
            {
                return new List<IReactPackage>
                {
                    new MainReactPackage(),
                    new ReactAzureNotificationHubPackage(),
                };
            }
        }

        public override bool UseDeveloperSupport
        {
            get
            {
                return true;
            }
        }
    }
}
