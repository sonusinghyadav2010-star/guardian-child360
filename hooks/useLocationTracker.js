
import { useEffect, useState } from 'react';
import { NativeEventEmitter, NativeModules } from 'react-native';
import SharedPreferences from '../services/SharedPreferences';

const { LocationModule, FirestoreModule } = NativeModules;

const useLocationTracker = () => {
  const [deviceId, setDeviceId] = useState(null);

  useEffect(() => {
    const getDeviceId = async () => {
      const id = await SharedPreferences.getString('deviceId');
      setDeviceId(id);
    };

    getDeviceId();

    const eventEmitter = new NativeEventEmitter(LocationModule);
    const locationListener = eventEmitter.addListener('onLocationUpdate', (event) => {
      if (deviceId) {
        FirestoreModule.updateLocation(deviceId, event.latitude, event.longitude);
      }
    });

    return () => {
      locationListener.remove();
    };
  }, [deviceId]);
};

export default useLocationTracker;
