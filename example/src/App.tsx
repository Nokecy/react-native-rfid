import * as React from 'react';

import { StyleSheet, View, Text, DeviceEventEmitter, TouchableOpacity } from 'react-native';
import RfidModule from 'react-native-rfid-module';

export default function App() {
  React.useEffect(() => {
    RfidModule.init().then(() => {

    });

    DeviceEventEmitter.addListener('findRfid', res => {
      console.log("ReadRFIDListenner", res.rfid_tag)
    })

    DeviceEventEmitter.addListener('StartScanRfid', res => {
      console.log("StartScanRfid", "StartScanRfid")
    })

    DeviceEventEmitter.addListener('StopScanRfid', res => {
      console.log("StopScanRfid", "StopScanRfid")
    })
  }, []);

  return (
    <View style={styles.container}>
      <Text>Result: {"abc"}</Text>

      <TouchableOpacity onPress={()=>{
        RfidModule.startScanRFID();
      }}>
        <Text>Result: {"点击开始搜索"}</Text>
      </TouchableOpacity>



      <TouchableOpacity onPress={()=>{
        RfidModule.find("202208241035");
      }}>
        <Text>Result: {"点击开始搜索"}</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
