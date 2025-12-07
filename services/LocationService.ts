import { NativeModules } from 'react-native';

const { LocationModule } = NativeModules;

interface ILocationService {
  startLocationTracking(): Promise<string>;
  stopLocationTracking(): Promise<string>;
}

const LocationService: ILocationService = {
  startLocationTracking: () => {
    return LocationModule.startLocationTracking();
  },
  stopLocationTracking: () => {
    return LocationModule.stopLocationTracking();
  },
};

export default LocationService;
