import { Rum, type ReactNativeConfiguration } from 'opentelemetry-react-native';
import { Button, StyleSheet, Text, View } from 'react-native';
import { OtelWrapper } from '../../src/OtelWrapper';
import { context, trace } from '@opentelemetry/api';

const RumConfig: ReactNativeConfiguration = {
  beaconEndpoint: 'https://otelcol.dev.mymtnlite.com.gh/v1/traces',
  // beaconEndpoint: 'http://10.0.2.2:4318/v1/traces',
  applicationName: 'mtn-app',
  allowInsecureBeacon: true,
  developmentEnvironment: 'local-development',
  debug: true,
  globalAttributes: {
    'app.version': '1.1.5',
    'globalAttr1': '42',
    'globalAttr2': 42,
    'service.name': 'my-mtn-app',
  },
};
export default function App() {
  const tracer = trace.getTracer('home');

  const createSpan = () => {
    const parent = tracer.startSpan('clickToFetch');
    parent.setAttributes({
      component: 'user-interaction',
      event_type: 'click',
      label: 'Make custom span',
    });
    const ctx = trace.setSpan(context.active(), parent);

    context.with(ctx, async () => {
      await rnFetch();
    });

    parent.end();
  };

  const rnFetch = async () => {
    try {
      const url =
        'https://raw.githubusercontent.com/signalfx/splunk-otel-react-native/main/package.json';
      await fetch(url);
    } catch (error) {
      console.error(error);
    }
  };
  const workflowSpan = () => {
    const now = Date.now();
    const span = tracer.startSpan('click', { startTime: now });
    span.setAttributes({
      'component': 'user-interaction',
      'workflow.name': 'CUSTOM_SPAN_1',
    });
    span.end(now + 5000);
  };

  return (
    <OtelWrapper configuration={RumConfig}>
      <View style={styles.container}>
        <Text>{'https://otelcol.dev.mymtnlite.com.gh'}</Text>
        <Button
          title="Go to Details Screen"
          accessibilityLabel="goToDetailScreen"
          testID="goToDetailScreen"
          onPress={() => {}}
        />
        <Button title="Nested fetch custom span" onPress={createSpan} />
        <Button
          title="RN fetch GET"
          onPress={rnFetch}
          accessibilityLabel="fetch"
          testID="fetch"
        />
        <Button
          title="fetch JSON"
          onPress={rnFetch}
          accessibilityLabel="fetchJSON"
          testID="fetchJSON"
        />
        <Button title="Workflow span" onPress={workflowSpan} />
        <Button
          title="New session"
          onPress={Rum._generatenewSessionId}
          accessibilityLabel="newSession"
          testID="newSession"
        />
        <Button
          accessibilityLabel="crash"
          testID="crash"
          title="Crash"
          onPress={Rum._testNativeCrash}
        />
        {/* <Button
        title="JS error"
        onPress={throwError}
        testID="jsError"
        accessibilityLabel="jsError"
      /> */}
      </View>
    </OtelWrapper>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
