import { setGlobalAttributes } from './globalAttributes';
import { SCREEN_NAME, LAST_SCREEN_NAME } from './attributeNames';
import { trace, diag, type Tracer } from '@opentelemetry/api';

let currentRouteName: string = 'none';
let tracer: Tracer;

export function getCurrentView() {
  return currentRouteName;
}

export function startNavigationTracking(navigationRef: any) {
  if (navigationRef) {
    tracer = trace.getTracer('uiChanges');
    const startingRoute = navigationRef.getCurrentRoute();
    if (startingRoute) {
      currentRouteName = startingRoute.name;
      createUiSpan(currentRouteName);
    }

    navigationRef.addListener('state', () => {
      const previous = currentRouteName;
      const route = navigationRef.getCurrentRoute();
      if (route) {
        currentRouteName = route.name;
        createUiSpan(currentRouteName, previous);
      }
    });
  } else {
    diag.debug('Navigation: navigationRef missing');
  }
}

function createUiSpan(current: string, previous?: string) {
  setGlobalAttributes({ [SCREEN_NAME]: current });
  // global attrs will get appended to this span anyways
  const span = tracer.startSpan('Created');
  span.setAttribute('component', 'ui');
  if (previous) {
    span.setAttribute(LAST_SCREEN_NAME, previous);
  }
  span.end();
}
