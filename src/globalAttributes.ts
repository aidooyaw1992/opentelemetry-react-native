import type { Attributes } from '@opentelemetry/api';
import type { ResourceAttributes } from '@opentelemetry/resources';
import {
  ATTR_TELEMETRY_SDK_NAME,
  ATTR_TELEMETRY_SDK_VERSION,
} from '@opentelemetry/semantic-conventions';
import { Platform } from 'react-native';
import { SCREEN_NAME } from './attributeNames';

import { getSessionId } from './session';
import { VERSION } from './version';

let globalAttributes: Attributes = {};

export function getResource(): ResourceAttributes {
  let resourcesAttrs: Record<string, string> = {
    [SCREEN_NAME]: 'unknown',
    [ATTR_TELEMETRY_SDK_NAME]: 'otlp',
    [ATTR_TELEMETRY_SDK_VERSION]: VERSION,
    // Splunk specific attributes
    ' otel.rumVersion': VERSION,
    'service.name': 'my- app',
  };

  if (Platform.OS === 'ios') {
    resourcesAttrs['os.name'] = 'iOS';
    resourcesAttrs['os.version'] = `${Platform.Version}`;
  } else {
    resourcesAttrs['os.name'] = 'Android';
    resourcesAttrs['os.type'] = 'Linux';
    resourcesAttrs['os.version'] = `${Platform.Version}`;
  }

  return resourcesAttrs;
}

globalAttributes = {
  ...getResource(),
};

export function setGlobalAttributes(attrs: object) {
  globalAttributes = Object.assign(globalAttributes, attrs);
  // setNativeGlobalAttributes(globalAttributes);
}

export function getGlobalAttributes(): Attributes {
  return Object.assign(globalAttributes, {
    'splunk.rumSessionId': getSessionId(),
  });
}
