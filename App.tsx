import React, { useState, useEffect } from 'react';
import { SafeAreaView, StyleSheet, NativeModules } from 'react-native';
import ControlScreen from './components/ControlScreen';
import PermissionSetupScreen from './screens/PermissionSetupScreen';
import useLocationTracker from './hooks/useLocationTracker';

const { PermissionsModule } = NativeModules;

function App() {
  const [areAllPermissionsGranted, setAreAllPermissionsGranted] = useState(false);
  useLocationTracker();

  useEffect(() => {
    const checkPermissions = async () => {
      const location = await PermissionsModule.isLocationPermissionGranted();
      const camera = await PermissionsModule.isCameraPermissionGranted();
      const notifications = await PermissionsModule.isNotificationPermissionGranted();
      const overlay = await PermissionsModule.isOverlayPermissionGranted();
      const accessibility = await PermissionsModule.isAccessibilityPermissionGranted();
      const deviceAdmin = await PermissionsModule.isDeviceAdminPermissionGranted();
      const notificationListener = await PermissionsModule.isNotificationListenerPermissionGranted();

      setAreAllPermissionsGranted(
        location &&
        camera &&
        notifications &&
        overlay &&
        accessibility &&
        deviceAdmin &&
        notificationListener
      );
    };

    checkPermissions();
  }, []);

  return (
    <SafeAreaView style={styles.container}>
      {areAllPermissionsGranted ? (
        <ControlScreen />
      ) : (
        <PermissionSetupScreen onPermissionsGranted={() => setAreAllPermissionsGranted(true)} />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
});

export default App;
