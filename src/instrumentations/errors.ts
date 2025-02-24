import { trace } from '@opentelemetry/api';

const STACK_LIMIT = 4096;
const MESSAGE_LIMIT = 1024;

export function instrumentErrors() {
  ErrorUtils.setGlobalHandler((err: any, isFatal?: boolean) => {
    reportError(err, isFatal);
  });
}

export function reportError(err: any, isFatal?: boolean) {
  const tracer = trace.getTracer('error');
  const msg = err.message || err.toString();

  const attributes = {
    'exception.isFatal': isFatal,
    'exception.message': limitLen(msg, MESSAGE_LIMIT),
    'exception.object': useful(err.name)
      ? err.name
      : err.constructor && err.constructor.name
        ? err.constructor.name
        : 'Error',
    'exception': true, //TODO do we use this?
    'component': 'error',
  };

  if (err.stack && useful(err.stack)) {
    (attributes as any)['exception.stacktrace'] = limitLen(
      err.stack.toString(),
      STACK_LIMIT
    );
  }
  tracer.startSpan('error', { attributes }).end();
}

function limitLen(s: string, cap: number): string {
  if (s.length > cap) {
    return s.substring(0, cap);
  } else {
    return s;
  }
}

function useful(s: any) {
  return s && s.trim() !== '' && !s.startsWith('[object') && s !== 'error';
}
