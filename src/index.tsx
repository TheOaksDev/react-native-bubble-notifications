import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-bubble-notifications' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo managed workflow\n';

const BubbleNotifications = NativeModules.BubbleNotifications
  ? NativeModules.BubbleNotifications
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

type Trip = {
  state: number;
  origin: string;
  dest: string;
  duration: string;
  distance: string;
  fare: string;
  assignmentId: string;
};

export const setConfig = (name = 'Driver', rating = '5.0') => {
  return BubbleNotifications.setDriverInfo(name, rating);
};

export const showBubble = (x = 50, y = 100) => {
  return BubbleNotifications.showFloatingBubble(x, y);
};

export const hideBubble = () => {
  return BubbleNotifications.hideFloatingBubble();
};

export const checkBubblePermissions = () => {
  return BubbleNotifications.checkPermission();
};

export const requestBubblePermissions = () => {
  return BubbleNotifications.requestPermission();
};

export const initializeBubble = () => {
  return BubbleNotifications.initialize();
};

export const destoryBubble = () => {
  return BubbleNotifications.destroy();
};

export const loadData = (trip: Trip) => {
  return BubbleNotifications.loadData(trip);
};

export const loadDataAndExpand = (trip: Trip) => {
  return BubbleNotifications.loadDataAndExpand(trip);
};

export const getBubbleState = () => {
  return BubbleNotifications.getState();
};

export const resetBubbleData = () => {
  return BubbleNotifications.resetBubbleDataFromReact();
};
