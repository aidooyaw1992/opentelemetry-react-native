import type { Attributes } from '@opentelemetry/api';
import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'opentelemetry-react-native' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const RNOpentelemetry = NativeModules.RNOpentelemetry
  ? NativeModules.RNOpentelemetry
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export type AppStartInfo = {
  appStart?: number;
  moduleStart: number; // as a backup
  isColdStart?: boolean;
};

export interface NativeSdKConfiguration {
  beaconEndpoint?: string;
  rumAccessToken?: string;
  globalAttributes?: Attributes;
  enableDiskBuffering?: boolean;
  limitDiskUsageMegabytes?: number;
  truncationCheckpoint?: number;
  resource?: Attributes;
}

export function multiply(a: number, b: number): Promise<number> {
  return RNOpentelemetry.multiply(a, b);
}

export function initializeNativeSdk(
  config: NativeSdKConfiguration
): Promise<AppStartInfo> {
  return RNOpentelemetry.initializeRUM(config);
}

export function exportSpansToNative(spans: any): Promise<any> {
  return RNOpentelemetry.export(spans);
}

export function setNativeSessionId(id: string): Promise<boolean> {
  return RNOpentelemetry.setSessionId(id);
}

export function testNativeCrash() {
  return RNOpentelemetry.nativeCrash();
}
