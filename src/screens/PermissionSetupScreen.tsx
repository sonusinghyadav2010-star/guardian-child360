import React, { useState, useEffect, useCallback } from 'react';
import { View, Text, Button, StyleSheet, NativeModules, AppState } from 'react-native';
import { request, PERMISSIONS, RESULTS } from 'react-native-permissions';
import SharedPreferences from '../../services/SharedPreferences';

const { PermissionsModule } = NativeModules;

const usePermissionCheck = (permissionCheckFunction) => {
  const [isGranted, setIsGranted] = useState(false);

  const checkPermission = useCallback(async () => {
    const granted = await permissionCheckFunction();
    setIsGranted(granted);
  }, [permissionCheckFunction]);

  useEffect(() => {
    checkPermission();
    const subscription = AppState.addEventListener("change", (nextAppState) => {
      if (nextAppState === "active") {
        checkPermission();
      }
    });

    return () => {
      subscription.remove();
    };
  }, [checkPermission]);

  return isGranted;
};

const PermissionItem = ({ name, request, isGranted }) => (
  <View style={styles.permissionContainer}>
    <Text>{name}</Text>
    <Button title={isGranted ? "Granted" : "Enable"} onPress={request} disabled={isGranted} />
  </View>
);

function PermissionSetupScreen({ onPermissionsGranted }) {
  const allPermissions = [
    {
      name: 'Location',
      request: () => request(PERMISSIONS.ANDROID.ACCESS_FINE_LOCATION),
      isGranted: usePermissionCheck(() => PermissionsModule.isLocationPermissionGranted()),
    },
    {
      name: 'Camera',
      request: () => request(PERMISSIONS.ANDROID.CAMERA),
      isGranted: usePermissionCheck(() => PermissionsModule.isCameraPermissionGranted()),
    },
    {
      name: 'Notifications',
      request: () => request(PERMISSIONS.ANDROID.POST_NOTIFICATIONS),
      isGranted: usePermissionCheck(() => PermissionsModule.isNotificationPermissionGranted()),
    },
    {
      name: 'Overlay',
      request: PermissionsModule.requestOverlayPermission,
      isGranted: usePermissionCheck(PermissionsModule.isOverlayPermissionGranted),
    },
    {
      name: 'Accessibility',
      request: PermissionsModule.requestAccessibilityPermission,
      isGranted: usePermissionCheck(PermissionsModule.isAccessibilityPermissionGranted),
    },
    {
      name: 'Device Admin',
      request: PermissionsModule.requestDeviceAdminPermission,
      isGranted: usePermissionCheck(PermissionsModule.isDeviceAdminPermissionGranted),
    },
    {
      name: 'Notification Listener',
      request: PermissionsModule.requestNotificationListenerPermission,
      isGranted: usePermissionCheck(PermissionsModule.isNotificationListenerPermissionGranted),
    },
  ];

  const allGranted = allPermissions.every((p) => p.isGranted);

  useEffect(() => {
    const setDeviceId = async () => {
      if (allGranted) {
        let deviceId = await SharedPreferences.getString('deviceId');
        if (!deviceId) {
          deviceId = 'device-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
          await SharedPreferences.setString('deviceId', deviceId);
        }
      }
    };

    setDeviceId();
  }, [allGranted]);

  return (
    <View style={styles.container}>
      <Text style={styles.title}>App Permissions</Text>
      {allPermissions.map((p, i) => (
        <PermissionItem key={i} {...p} />
      ))}
      <Button title="Done" onPress={onPermissionsGranted} disabled={!allGranted} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 20,
  },
  permissionContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    width: '100%',
    paddingVertical: 10,
    borderBottomWidth: 1,
    borderBottomColor: '#ccc',
  },
});

export default PermissionSetupScreen;
