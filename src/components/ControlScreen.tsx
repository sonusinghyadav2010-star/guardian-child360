import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, DeviceEventEmitter, NativeModules, Button, Image } from 'react-native';

const { LocationModule, CameraModule, AudioModule, FirestoreModule } = NativeModules;

function ControlScreen() {
  const [location, setLocation] = useState<{ latitude: number; longitude: number } | null>(null);
  const [capturedImage, setCapturedImage] = useState<string | null>(null);

  useEffect(() => {
    LocationModule.startLocationTracking();

    const appOpenedSubscription = DeviceEventEmitter.addListener('onAppOpened', (event) => {
      console.log('App Opened:', event.packageName);
    });

    const notificationSubscription = DeviceEventEmitter.addListener('onNotificationReceived', (event) => {
      console.log('Notification Received:', event);
    });

    const locationSubscription = DeviceEventEmitter.addListener('onLocationUpdate', (event) => {
      console.log('Location Update:', event);
      setLocation({ latitude: event.latitude, longitude: event.longitude });
      FirestoreModule.updateLocation(event.latitude, event.longitude);
    });

    return () => {
      LocationModule.stopLocationTracking();
      appOpenedSubscription.remove();
      notificationSubscription.remove();
      locationSubscription.remove();
    };
  }, []);

  const handleCaptureImage = async () => {
    try {
      const base64Image = await CameraModule.captureImage();
      setCapturedImage(`data:image/jpeg;base64,${base64Image}`);
    } catch (error) {
      console.error('Error capturing image:', error);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Guardian Child</Text>
      <Text>Monitoring in the background...</Text>
      {location && (
        <Text>
          Current Location: {location.latitude}, {location.longitude}
        </Text>
      )}
      <Button title="Capture Image" onPress={handleCaptureImage} />
      {capturedImage && (
        <Image source={{ uri: capturedImage }} style={styles.image} />
      )}
      <View style={styles.buttonContainer}>
        <Button title="Start Audio Recording" onPress={() => AudioModule.startAudioRecording()} />
        <Button title="Stop Audio Recording" onPress={() => AudioModule.stopAudioRecording()} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 20,
  },
  image: {
    width: 200,
    height: 150,
    marginTop: 20,
    resizeMode: 'contain',
  },
  buttonContainer: {
    marginTop: 20,
  },
});

export default ControlScreen;
