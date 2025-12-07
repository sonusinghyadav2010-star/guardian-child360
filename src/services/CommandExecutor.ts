
import { NativeModules } from 'react-native';
import { startLiveCamera, stopLiveCamera, startScreenShare, stopScreenShare, webrtcReset } from './WebRTCCommands';

const { CommandExecutor, UsageStatsModule } = NativeModules;

export const takePhoto = async () => {
    return await CommandExecutor.execute('takePhoto');
};

export const getLocation = async () => {
    return await CommandExecutor.execute('getLocation');
};

export const startCameraStream = async () => {
    return await startLiveCamera();
};

export const stopCameraStream = async () => {
    return await stopLiveCamera();
};

export const startScreenStream = async () => {
    return await startScreenShare();
}

export const stopScreenStream = async () => {
    return await stopScreenShare();
}

export const resetWebRTC = async () => {
    return await webrtcReset();
}

export const getUsageStats = async () => {
    return await UsageStatsModule.getRecentApps();
};

export const getNotificationsSnapshot = async () => {
    // This would ideally be implemented in a native module
    // For now, we'll just return a placeholder
    return { status: "not implemented" };
};

export const playAlarm = async () => {
    return await CommandExecutor.execute('playAlarm');
};

export const vibrateDevice = async () => {
    return await CommandExecutor.execute('vibrateDevice');
};
