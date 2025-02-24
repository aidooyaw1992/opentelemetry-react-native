import { useEffect, type PropsWithChildren } from 'react';
import { Rum, type ReactNativeConfiguration } from './rum';

type Props = PropsWithChildren<{
  configuration: ReactNativeConfiguration;
}>;

let isInitialized = false;

export const OtelWrapper = ({ children, configuration }: Props) => {
  useEffect(() => {
    Rum.finishAppStart();
  }, []);

  if (!isInitialized) {
    Rum.init(configuration);
    isInitialized = true;
  } else {
    console.log('Already initialized');
  }

  return children;
};
