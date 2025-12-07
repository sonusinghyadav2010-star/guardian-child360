import { NativeModules } from 'react-native';

const { SharedPreferencesModule } = NativeModules;

const SharedPreferences = {
  setString: (key, value) => {
    return SharedPreferencesModule.setString(key, value);
  },
  getString: (key) => {
    return SharedPreferencesModule.getString(key);
  },
};

export default SharedPreferences;
