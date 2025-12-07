import React from 'react';
import { View, Text, Button, StyleSheet } from 'react-native';
import { Stack, useNavigation, useRouter } from 'expo-router';

export default function OnboardingScreen() {
  const router = useRouter();
  const navigation = useNavigation();

  return (
    <View style={styles.container}>
      <Stack.Screen options={{ title: 'Onboarding' }} />
      <Text style={styles.title}>Welcome to Guardian Child App</Text>
      <Text style={styles.subtitle}>Let's get you paired with your parent.</Text>
      <Button
        title="Start Pairing"
        onPress={() => navigation.navigate('permissions-status')} 
      />
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
    marginBottom: 10,
  },
  subtitle: {
    fontSize: 16,
    textAlign: 'center',
    marginBottom: 30,
  },
});