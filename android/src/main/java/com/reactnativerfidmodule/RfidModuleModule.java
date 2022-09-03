package com.reactnativerfidmodule;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.rscja.deviceapi.entity.UHFTAGInfo;
import com.rscja.deviceapi.exception.ConfigurationException;
import com.rscja.deviceapi.interfaces.ConnectionStatus;
import com.rscja.deviceapi.interfaces.IUHF;
import com.rscja.deviceapi.RFIDWithUHFUART;

import java.util.ArrayList;
import java.util.List;

@ReactModule(name = RfidModuleModule.NAME)
public class RfidModuleModule extends ReactContextBaseJavaModule {
    public static final String NAME = "RfidModule";
    private static ReactApplicationContext reactcontext;
    public RFIDWithUHFUART uhf;
    public boolean isScanning = false;
    public boolean light = false;
    private Handler handler;
    private List<UHFTAGInfo> tempDatas = new ArrayList<>();

    final int FLAG_START = 0;
    final int FLAG_STOP = 1;
    final int FLAG_UHFINFO = 3;
    final int FLAG_SUCCESS = 10;
    final int FLAG_FAIL = 11;

    final int FLAG_START_LIGHT = 15;
    final int FLAG_STOP_LIGHT = 16;

    public RfidModuleModule(ReactApplicationContext reactContext) {
      super(reactContext);
      reactcontext=reactContext;

      handler = new Handler(Looper.getMainLooper()){
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void handleMessage(@NonNull Message msg) {
          switch (msg.what) {
            case FLAG_STOP:
              if (msg.arg1 == FLAG_SUCCESS) {
                WritableMap payload = Arguments.createMap();
                sendEvent(reactcontext, "StopScanRfid", payload);
              } else {
//                Utils.playSound(2);
              }
              break;
            case FLAG_UHFINFO:
              UHFTAGInfo list = ( UHFTAGInfo) msg.obj;
              emitRfid(list);
              break;
            case FLAG_START:
              if (msg.arg1 == FLAG_SUCCESS) {
                WritableMap payload = Arguments.createMap();
                sendEvent(reactcontext, "StartScanRfid", payload);
              } else {
                int i = 0;
//                Utils.playSound(2);
              }
              break;
            case FLAG_START_LIGHT:
              if (msg.arg1 == FLAG_SUCCESS) {
                WritableMap payload = Arguments.createMap();
                sendEvent(reactcontext, "onStartLight", payload);
              }
              break;
            case FLAG_STOP_LIGHT:
              if (msg.arg1 == FLAG_SUCCESS) {
                WritableMap payload = Arguments.createMap();
                sendEvent(reactcontext, "onStopLight", payload);
              }
              break;
          }
        }
      };
    }

    @Override
    @NonNull
    public String getName() {
        return NAME;
    }

    private void sendEvent(ReactContext reactContext, String eventName, @Nullable WritableMap params) {
      reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
    }

  @ReactMethod
    public void init(Promise promise) throws ConfigurationException {
      try {
        uhf = RFIDWithUHFUART.getInstance();
        uhf.init(reactcontext);
      } catch (Exception ex) {
        promise.reject("002",ex.getMessage());
        return;
      }
      promise.resolve("");
    }

    @ReactMethod
    public void startLightUp(String filterPtr,Promise promise) throws ConfigurationException {
      if (uhf==null){
        promise.reject("001","没有连接模块");
        return;
      }
      else
      {
        light = false;

        new LightThread(filterPtr).start();

        promise.resolve("");
      }
  }

    @ReactMethod
    public void stoplightUp(String filterPtr,Promise promise) throws ConfigurationException {
      light = false;

      Message msg = handler.obtainMessage(FLAG_STOP_LIGHT);
      msg.arg1 = FLAG_SUCCESS;
      handler.sendMessage(msg);
    }

    @ReactMethod
    public void startScanRFID() {
      isScanning = true;
      new TagThread().start();
    }

    @ReactMethod
    private void stopScanRFID() {
      isScanning = false;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void emitRfid(UHFTAGInfo uhfInfo) {
      UHFTAGInfo exists = tempDatas.stream().filter(temp -> temp.getEPC().equals(uhfInfo.getEPC())).findAny().orElse(null);;
      if (exists == null){
        tempDatas.add(uhfInfo);

        WritableMap payload = Arguments.createMap();
        payload.putString("rfid_tag", uhfInfo.getEPC());
        payload.putString("rssi", uhfInfo.getRssi());
        sendEvent(reactcontext, "findRfid", payload);
      }
    }

    @Override
    public void onCatalystInstanceDestroy() {
      super.onCatalystInstanceDestroy();
      uhf.free();
    }

    class TagThread extends Thread {
      public void run() {
        Message msg = handler.obtainMessage(FLAG_START);
        if (uhf.startInventoryTag()) {
          msg.arg1 = FLAG_SUCCESS;
        } else {
          msg.arg1 = FLAG_FAIL;
          isScanning=false;
        }
        handler.sendMessage(msg);
        while (isScanning) {
          UHFTAGInfo uhfInfo = getUHFInfo();
          if(uhfInfo==null){
            SystemClock.sleep(1);
          }else{
//            Utils.playSound(1);
            handler.sendMessage(handler.obtainMessage(FLAG_UHFINFO, uhfInfo));
          }
        }
        stopInventory();
      }
    }

    class LightThread extends Thread {
        private String filterPtr = "";
        public LightThread(String filterPtr){
          this.filterPtr= filterPtr;
        }
        public void run() {
          Message msg = handler.obtainMessage(FLAG_START_LIGHT);
          msg.arg1 = FLAG_SUCCESS;
          handler.sendMessage(msg);

          while (light) {
            String pwdStr = "00000000";
            int filterCnt = filterPtr.length() * 4;
            uhf.readData(pwdStr,IUHF.Bank_EPC,32,filterCnt,filterPtr,IUHF.Bank_RESERVED,4,1);
          }
        }
    }

    private void stopInventory(){
      boolean result = uhf.stopInventory();
      ConnectionStatus connectionStatus = uhf.getConnectStatus();
      Message msg = handler.obtainMessage(FLAG_STOP);
      if (!result || connectionStatus == ConnectionStatus.DISCONNECTED) {
        msg.arg1 = FLAG_FAIL;
      } else {
        msg.arg1 = FLAG_SUCCESS;
      }
      handler.sendMessage(msg);
    }

    private synchronized UHFTAGInfo getUHFInfo() {
      return uhf.readTagFromBuffer();
    }
}
