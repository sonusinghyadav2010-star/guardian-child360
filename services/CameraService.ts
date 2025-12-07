import { NativeModules } from 'react-native';

const { CameraModule } = NativeModules;

interface ICameraService {
  startCamera(): Promise<string>;
  stopCamera(): Promise<string>;
}

const CameraService: ICameraService = {
  startCamera: () => {
    return CameraModule.startCamera();
  },
  stopCamera: () => {
    return CameraModule.stopCamera();
  },
};

export default CameraService;
