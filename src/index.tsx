import { NativeModules } from 'react-native';

type RfidModuleType = {
  multiply(a: number, b: number): Promise<number>;
};

const { RfidModule } = NativeModules;

export default RfidModule as RfidModuleType;
