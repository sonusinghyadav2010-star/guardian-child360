import firestore from '@react-native-firebase/firestore';
import { AppRegistry } from 'react-native';
import { getUniqueId } from 'react-native-device-info';
import {
    takePhoto,
    getLocation,
    startCameraStream,
    stopCameraStream,
    getUsageStats,
    getNotificationsSnapshot,
    playAlarm,
    vibrateDevice,
    startScreenStream,
    stopScreenStream,
    resetWebRTC
} from './CommandExecutor';

const commandHandlers = {
    takePhoto,
    getLocation,
    startCameraStream,
    stopCameraStream,
    startScreenStream,
    stopScreenStream,
    resetWebRTC,
    getUsageStats,
    getNotificationsSnapshot,
    playAlarm,
    vibrateDevice,
    ping: async () => 'alive'
};

const RemoteCommandListener = async (taskData) => {
    const { commandId, command, payload } = taskData;
    const deviceId = await getUniqueId();

    try {
        if (commandHandlers[command]) {
            const result = await commandHandlers[command](payload);
            await firestore()
                .collection('childDevices')
                .doc(deviceId)
                .collection('responses')
                .doc(commandId)
                .set({
                    result,
                    status: 'completed',
                    timestamp: firestore.FieldValue.serverTimestamp(),
                });
        } else {
            throw new Error(`Unsupported command: ${command}`);
        }
    } catch (error) {
        await firestore()
            .collection('childDevices')
            .doc(deviceId)
            .collection('responses')
            .doc(commandId)
            .set({
                error: error.message,
                status: 'failed',
                timestamp: firestore.FieldValue.serverTimestamp(),
            });
    }
};

AppRegistry.registerHeadlessTask('RemoteCommandListener', () => RemoteCommandListener);

export default RemoteCommandListener;
