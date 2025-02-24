import {
  context,
  diag,
  DiagConsoleLogger,
  DiagLogLevel,
  trace,
  type Attributes,
  type Span,
} from '@opentelemetry/api';
import { _globalThis } from '@opentelemetry/core';
import {
  BatchSpanProcessor,
  ConsoleSpanExporter,
  SimpleSpanProcessor,
  WebTracerProvider,
} from '@opentelemetry/sdk-trace-web';
import { Platform } from 'react-native';
import { instrumentErrors } from './instrumentations/errors';
import { instrumentXHR } from './instrumentations/xhr';
import {
  initializeNativeSdk,
  setNativeSessionId,
  testNativeCrash,
  type AppStartInfo,
  type NativeSdKConfiguration,
} from './native';

import NativeSpanExporter from './exporting';
import { getResource } from './globalAttributes';
import { _generatenewSessionId, getSessionId } from './session';

export interface ReactNativeConfiguration {
  beaconEndpoint?: string;
  applicationName: string;
  developmentEnvironment: string;
  debug?: boolean;
  allowInsecureBeacon?: boolean;
  ignoreUrls?: Array<string | RegExp>;
  /** Sets attributes added to every Span. */
  globalAttributes?: Attributes;
  bufferTimeout?: number;
  bufferSize?: number;
  appStartEnabled?: boolean;
}

export interface RumType {
  appStartSpan?: Span;
  appStartEnd: number | null;
  finishAppStart: () => void;
  init: (options: ReactNativeConfiguration) => RumType | undefined;
  provider?: WebTracerProvider;
  _testNativeCrash: () => void;
  // updateLocation: (latitude: number, longitude: number) => void;

  // reportError: (err: any, isFatal?: boolean) => void;
  // setGlobalAttributes: (attributes: Attributes) => void;
  _generatenewSessionId: () => void;
}

const DEFAULT_CONFIG = {
  appStartEnabled: true,
  enableDiskBuffering: true,
};

let appStartInfo: AppStartInfo | null = null;
let isInitialized = false;

export const Rum: RumType = {
  appStartEnd: null,
  finishAppStart() {
    if (this.appStartSpan?.isRecording()) {
      this.appStartSpan.end();
    } else {
      this.appStartEnd = Date.now();
      diag.debug('AppStart: end called without start');
    }
  },

  init(configuration: ReactNativeConfiguration) {
    const config = {
      ...DEFAULT_CONFIG,
      ...configuration,
    };

    diag.setLogger(
      new DiagConsoleLogger(),
      config?.debug ? DiagLogLevel.DEBUG : DiagLogLevel.ERROR
    );

    if (isInitialized) {
      diag.warn('SplunkRum already init()ed.');
      return;
    }

    //by default wants to use otlp
    if (!('OTEL_TRACES_EXPORTER' in _globalThis)) {
      (_globalThis as any).OTEL_TRACES_EXPORTER = 'none';
    }

    const clientInit = Date.now();
    if (!config.applicationName) {
      diag.error('applicationName name is required.');
      return;
    }

    if (!config.beaconEndpoint) {
      diag.error('Either realm or beaconEndpoint is required.');
      return;
    }

    const nativeSdkConf: NativeSdKConfiguration = {};
    if (config.beaconEndpoint) {
      if (
        !config.beaconEndpoint.startsWith('https') &&
        !config.allowInsecureBeacon
      ) {
        diag.error(
          'Not using https is unsafe, if you want to force it use allowInsecureBeacon option.'
        );
        return;
      }

      nativeSdkConf.beaconEndpoint = config.beaconEndpoint;
    }

    const sessionId = getSessionId();

    nativeSdkConf.resource = {
      'service.name': config.applicationName,
      ...getResource(),
    };

    nativeSdkConf.globalAttributes = {
      // ...config.globalAttributes,
      'mtn.rumSessionId': sessionId,
    };

    if (config.developmentEnvironment) {
      nativeSdkConf.resource['deployment.environment'] =
        config.developmentEnvironment;
    }

    // make sure native crashreporter has correct attributes
    // setGlobalAttributes(nativeSdkConf.globalAttributes);

    const provider = new WebTracerProvider({
      spanProcessors: [
        new SimpleSpanProcessor(new ConsoleSpanExporter()),
        new BatchSpanProcessor(new NativeSpanExporter()),
      ],
    });

    provider.register();
    this.provider = provider;

    const clientInitEnd = Date.now();

    instrumentXHR({ ignoreUrls: config.ignoreUrls });
    instrumentErrors();

    const nativeInit = Date.now();

    diag.debug(
      'Initializing with: ',
      config.applicationName,
      nativeSdkConf.resource
    );

    initializeNativeSdk(nativeSdkConf)
      .then((nativeAppStart) => {
        console.log('nativeAppStart', nativeAppStart);

        appStartInfo = nativeAppStart;
        if (Platform.OS === 'ios') {
          appStartInfo.isColdStart = appStartInfo?.isColdStart || true;
          appStartInfo.appStart =
            appStartInfo?.appStart ?? appStartInfo?.moduleStart;
        }
        setNativeSessionId(getSessionId());

        if (config.appStartEnabled) {
          const tracer = provider.getTracer('AppStart');
          const nativeInitEnd = Date.now();

          this.appStartSpan = tracer.startSpan('AppStart', {
            startTime: appStartInfo?.appStart,
            attributes: {
              'component': 'appstart',
              'start.type': appStartInfo?.isColdStart ? 'cold' : 'warm',
            },
          });

          //FIXME no need to have native init span probably
          const ctx = trace.setSpan(context.active(), this.appStartSpan);

          context.with(ctx, () => {
            tracer
              .startSpan('SplunkRum.nativeInit', { startTime: nativeInit })
              .end(nativeInitEnd);
            tracer
              .startSpan('SplunkRum.jsInit', { startTime: clientInit })
              .end(clientInitEnd);
          });

          if (this.appStartEnd !== null) {
            diag.debug('AppStart: using manual end');
            this.appStartSpan.end(this.appStartEnd);
          }
        }
      })
      .catch((e) => {
        console.log('Error initializing SplunkRum', e);
      });

    isInitialized = true;
    return this;
  },

  _generatenewSessionId: _generatenewSessionId,
  _testNativeCrash: testNativeCrash,
  // reportError: reportError,
  // setGlobalAttributes: setGlobalAttributes,
  // updateLocation: updateLocation,
};

// function updateLocation(latitude: number, longitude: number) {
//   // setGlobalAttributes({
//   //   [LOCATION_LATITUDE]: latitude,
//   //   [LOCATION_LONGITUDE]: longitude,
//   // });
// }
