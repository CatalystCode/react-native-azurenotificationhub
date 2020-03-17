/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow
 */

import React, { Component } from 'react';
import { NativeEventEmitter } from 'react-native';
import {
  StyleSheet,
  Text,
  View,
  TouchableOpacity,
} from 'react-native';

const NotificationHub = require('react-native-azurenotificationhub');
const PushNotificationEmitter = new NativeEventEmitter(NotificationHub);

const EVENT_AZURE_NOTIFICATION_HUB_REGISTERED         = 'azureNotificationHubRegistered';
const EVENT_AZURE_NOTIFICATION_HUB_REGISTERED_ERROR   = 'azureNotificationHubRegisteredError';
const EVENT_REMOTE_NOTIFICATION_RECEIVED              = 'remoteNotificationReceived';

const connectionString                                = 'Endpoint=sb://rn-anh.servicebus.windows.net/;SharedAccessKeyName=DefaultFullSharedAccessSignature;SharedAccessKey=12345';
const hubName                                         = 'azurenotificationhub';
const senderID                                        = '12345';
const tags                                            = [];
const channelName                                     = 'Channel Name';
const channelImportance                               = 3;
const channelShowBadge                                = true;
const channelEnableLights                             = true;
const channelEnableVibration                          = true;
const template                                        = '{\"data\":{\"message\":\"$(message)\"}}';
const templateName                                    = 'Template Name';

export default class App extends Component {
  constructor(props) {
    super(props);
    PushNotificationEmitter.addListener(EVENT_REMOTE_NOTIFICATION_RECEIVED, this._onRemoteNotification);
  }

  register() {
    PushNotificationEmitter.addListener(EVENT_AZURE_NOTIFICATION_HUB_REGISTERED, this._onAzureNotificationHubRegistered);
    PushNotificationEmitter.addListener(EVENT_AZURE_NOTIFICATION_HUB_REGISTERED_ERROR, this._onAzureNotificationHubRegisteredError);

    NotificationHub.register({
      connectionString,
      hubName,
      senderID,
      tags,
      channelName,
      channelImportance,
      channelShowBadge,
      channelEnableLights,
      channelEnableVibration
    })
    .then((res) => console.warn(res))
    .catch(reason => console.warn(reason));
  }

  registerTemplate() {
    PushNotificationEmitter.addListener(EVENT_AZURE_NOTIFICATION_HUB_REGISTERED, this._onAzureNotificationHubRegistered);
    PushNotificationEmitter.addListener(EVENT_AZURE_NOTIFICATION_HUB_REGISTERED_ERROR, this._onAzureNotificationHubRegisteredError);

    NotificationHub.registerTemplate({
      connectionString,
      hubName,
      senderID,
      template,
      templateName,
      tags,
      channelName,
      channelImportance,
      channelShowBadge,
      channelEnableLights,
      channelEnableVibration
    })
    .then((res) => console.warn(res))
    .catch(reason => console.warn(reason));
  }

  getInitialNotification() {
    NotificationHub.getInitialNotification()
    .then((res) => console.warn(res))
    .catch(reason => console.warn(reason));
  }

  getUUID() {
    NotificationHub.getUUID(false)
    .then((res) => console.warn(res))
    .catch(reason => console.warn(reason));
  }

  isNotificationEnabledOnOSLevel() {
    NotificationHub.isNotificationEnabledOnOSLevel()
    .then((res) => console.warn(res))
    .catch(reason => console.warn(reason));
  }

  unregister() {
    NotificationHub.unregister()
    .then((res) => console.warn(res))
    .catch(reason => console.warn(reason));
  }

  unregisterTemplate() {
    NotificationHub.unregisterTemplate(templateName)
    .then((res) => console.warn(res))
    .catch(reason => console.warn(reason));
  }

  render() {
    return (
      <View style={styles.container}>
        <TouchableOpacity onPress={this.register.bind(this)}>
         <View style={styles.button}>
           <Text style={styles.buttonText}>
             Register
           </Text>
         </View>
       </TouchableOpacity>
       <TouchableOpacity onPress={this.registerTemplate.bind(this)}>
         <View style={styles.button}>
           <Text style={styles.buttonText}>
             Register Template
           </Text>
         </View>
       </TouchableOpacity>
       <TouchableOpacity onPress={this.getInitialNotification.bind(this)}>
         <View style={styles.button}>
           <Text style={styles.buttonText}>
            Get initial notification
           </Text>
         </View>
       </TouchableOpacity>
       <TouchableOpacity onPress={this.getUUID.bind(this)}>
         <View style={styles.button}>
           <Text style={styles.buttonText}>
            Get UUID
           </Text>
         </View>
       </TouchableOpacity>
       <TouchableOpacity onPress={this.isNotificationEnabledOnOSLevel.bind(this)}>
         <View style={styles.button}>
           <Text style={styles.buttonText}>
            Check if notification is enabled
           </Text>
         </View>
       </TouchableOpacity>
       <TouchableOpacity onPress={this.unregister.bind(this)}>
         <View style={styles.button}>
           <Text style={styles.buttonText}>
             Unregister
           </Text>
         </View>
       </TouchableOpacity>
       <TouchableOpacity onPress={this.unregisterTemplate.bind(this)}>
         <View style={styles.button}>
           <Text style={styles.buttonText}>
             Unregister Template
           </Text>
         </View>
       </TouchableOpacity>
      </View>
    );
  }

  _onAzureNotificationHubRegistered(registrationID) {
    console.warn('RegistrationID: ' + registrationID);
  }

  _onAzureNotificationHubRegisteredError(error) {
    console.warn('Error: ' + error);
  }

  _onRemoteNotification(notification) {
    console.warn(notification);
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  welcome: {
    fontSize: 20,
    textAlign: 'center',
    margin: 10,
  },
  instructions: {
    textAlign: 'center',
    color: '#333333',
    marginBottom: 5,
  },
});
