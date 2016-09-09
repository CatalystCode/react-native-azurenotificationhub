import {
  AppRegistry,
  NativeModules,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';

const NotificationHub = NativeModules.AzureNotificationHub;
const {connectionString, hubName} = require('./config');

import React, {Component} from 'react';

class Playground extends Component
{
  register()
  {
    NotificationHub.register({connectionString, hubName})
      .catch(reason => console.warn(reason));
  }

  unregister()
  {
    NotificationHub.unregister({connectionString, hubName})
      .catch(reason => console.warn(reason));
  }

  render()
  {
    return (
     <View style={styles.container}>
       <TouchableOpacity onPress={this.register.bind(this)}>
         <View style={styles.button}>
           <Text style={styles.buttonText}>
             Register
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
     </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  button: {
    backgroundColor: 'blue',
    borderRadius: 5,
    padding: 10,
    margin: 2,
    minWidth: 200,
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
  },
});

AppRegistry.registerComponent('Playground', () => Playground);
