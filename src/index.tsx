import { NativeModules } from 'react-native';

type RfidModuleType = {
  // 初始化模块
  init(): Promise<any>;

  // 查找并点亮Id
  find(filter:string): Promise<any>;

  //开始查找RFID
  startScanRFID(): Promise<any>;

  //停止查找RFID
  stop(): Promise<any>;
};

const { RfidModule } = NativeModules;

export default RfidModule as RfidModuleType;
