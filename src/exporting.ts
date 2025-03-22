import {
  ExportResultCode,
  hrTimeToMilliseconds,
  type ExportResult,
} from '@opentelemetry/core';
import type { ReadableSpan, SpanExporter } from '@opentelemetry/sdk-trace-web';
import { exportSpansToNative } from './native';
import { diag } from '@opentelemetry/api';

export default class NativeSpanExporter implements SpanExporter {
  export(
    spans: ReadableSpan[],
    resultCallback: (result: ExportResult) => void
  ): void {
    exportSpansToNative(spans.map(this.toNativeSpan));
    resultCallback({ code: ExportResultCode.SUCCESS });
  }

  shutdown(): Promise<void> {
    return Promise.resolve();
  }
  forceFlush?(): Promise<void> {
    return Promise.resolve();
  }

  toNativeSpan(span: ReadableSpan): object {
    const spanContext = span.spanContext();

    const nSpan = {
      name: span.name,
      tracerName: span.instrumentationLibrary.name,
      startTime: hrTimeToMilliseconds(span.startTime),
      endTime: hrTimeToMilliseconds(span.endTime),
      parentSpanId: span.parentSpanId,
      attributes: span.attributes,
      ...spanContext,
    };
    console.log('span', nSpan);
    diag.debug('Exporting:toNativeSpan: ', nSpan.name, span.attributes);
    return nSpan;
  }
}
