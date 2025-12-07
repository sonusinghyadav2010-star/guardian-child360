import React from 'react';
import { View, Button, StyleSheet, Alert } from 'react-native';
import ForegroundService from '../services/ForegroundService';
import CameraService from '../services/CameraService';
import LocationService from '../services/LocationService';

const ControlScreen = () => {
  const handleStartForegroundService = async () => {
    try {
      const result = await ForegroundService.startService();
      Alert.alert('Success', result);
    } catch (error) {
      console.error(error);
      Alert.alert('Error', 'Failed to start foreground service.');
    }
  };

  const handleStopForegroundService = async () => {
    try {
      const result = await ForegroundService.stopService();
      Alert.alert('Success', result);
    } catch (error) {
      console.error(error);
      Alert.alert('Error', 'Failed to stop foreground service.');
    }
  };

  const handleStartCamera = async () => {
    try {
      const result = await CameraService.startCamera();
      Alert.alert('Success', result);
    } catch (error) {
      console.error(error);
      Alert.alert('Error', 'Failed to start camera.');
    }
  };

  const handleStopCamera = async () => {
    try {
      const result = await CameraService.stopCamera();
      Alert.alert('Success', result);
    } catch (error) {
      console.error(error);
      Alert.alert('Error', 'Failed to stop camera.');
    }
  };

  const handleStartLocation = async () => {
    try {
      const result = await LocationService.startLocationTracking();
      Alert.alert('Success', result);
    } catch (error) {
      console.error(error);
      Alert.alert('Error', 'Failed to start location tracking.');
    }
  };

  const handleStopLocation = async () => {
    try {
      const result = await LocationService.stopLocationTracking();
      Alert.alert('Success', result);
    } catch (error) {
      console.error(error);
      Alert.alert('Error', 'Failed to stop location tracking.');
    }
  };

  return (
    <View style={styles.container}>
      <Button title="Start Foreground Service" onPress={handleStartForegroundService} />
      <Button title="Stop Foreground Service" onPress={handleStopForegroundService} />
      <View style={styles.separator} />
      <Button title="Start Camera" onPress={handleStartCamera} />
      <Button title="Stop Camera" onPress={handleStopCamera} />
      <View style={styles.separator} />
      <Button title="Start Location Tracking" onPress={handleStartLocation} />
      <Button title="Stop Location Tracking" onPress={handleStopLocation} />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  separator: {
    marginVertical: 10,
  },
});

export default ControlScreen;
