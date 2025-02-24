import { diag, trace, type Attributes } from '@opentelemetry/api';
import { isUrlIgnored } from '@opentelemetry/core';
import {
  ATTR_HTTP_REQUEST_METHOD,
  ATTR_HTTP_RESPONSE_STATUS_CODE,
  ATTR_URL_FULL,
} from '@opentelemetry/semantic-conventions';

import { COMPONENT } from '../attributeNames';
import { captureTraceParent } from '../serverTiming';

const ATTRIBUTE_PROP = '_mtnXHRAttributes';

interface XhrConfig {
  ignoreUrls: Array<string | RegExp> | undefined;
}

interface InstrumentedXMLHttpRequest extends XMLHttpRequest {
  [ATTRIBUTE_PROP]: Attributes;
}

export function instrumentXHR(config: XhrConfig) {
  const originalOpen = XMLHttpRequest.prototype.open;
  const originalSend = XMLHttpRequest.prototype.send;
  const tracer = trace.getTracer('xhr');

  XMLHttpRequest.prototype.open = function (
    this: InstrumentedXMLHttpRequest,
    ...args
  ) {
    const attributes = {
      [ATTR_HTTP_REQUEST_METHOD]: args[0],
      [ATTR_URL_FULL]: args[1],
      [COMPONENT]: 'http',
    };

    diag.debug(`XHR url: ${args[1]}, ignoreUrls: ${config.ignoreUrls}`);
    if (isUrlIgnored(args[1], config.ignoreUrls)) {
      diag.debug('XHR: ignoring span as url matches ignored url');
    } else {
      this[ATTRIBUTE_PROP] = attributes;
    }

    originalOpen.apply(this, args);
  };

  XMLHttpRequest.prototype.send = function (
    this: InstrumentedXMLHttpRequest,
    ...args
  ) {
    const attrs = this[ATTRIBUTE_PROP];
    if (attrs) {
      const spanName = `HTTP ${(
        attrs[ATTR_HTTP_REQUEST_METHOD]! as string
      ).toUpperCase()}`;

      const span = tracer.startSpan(spanName, {
        attributes: attrs,
      });

      this.addEventListener('readystatechange', () => {
        if (this.readyState === XMLHttpRequest.HEADERS_RECEIVED) {
          const headers = this.getAllResponseHeaders().toLowerCase();
          if (headers.indexOf('server-timing') !== -1) {
            const st = this.getResponseHeader('server-timing');
            if (st !== null) {
              captureTraceParent(st, span);
            }
          }
        }
        if (this.readyState === XMLHttpRequest.DONE) {
          span.setAttribute(ATTR_HTTP_RESPONSE_STATUS_CODE, this.status);
          span.end();
        }
      });
    }
    originalSend.apply(this, args);
  };
}
