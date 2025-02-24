// TODO workaround for otel which uses timeOrigin
if (!global.performance.timeOrigin) {
  (global as any).performance.timeOrigin = Date.now() - performance.now();
}

export * from './rum';
export * from './OtelWrapper';
