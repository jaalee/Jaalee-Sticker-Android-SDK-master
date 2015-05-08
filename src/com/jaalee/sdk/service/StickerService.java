 package com.jaalee.sdk.service;
 
import java.util.concurrent.TimeUnit;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;

import com.jaalee.sdk.Sticker;
import com.jaalee.sdk.Utils;
import com.jaalee.sdk.internal.Preconditions;
import com.jaalee.sdk.utils.L;

 public class StickerService extends Service
 {
	 public static final int MSG_START_RANGING = 1;
	 public static final int MSG_STOP_RANGING = 2;
	 public static final int MSG_RANGING_RESPONSE = 3;
	 public static final int MSG_START_MONITORING = 4;
	 public static final int MSG_STOP_MONITORING = 5;
	 public static final int MSG_MONITORING_RESPONSE = 6;
	 public static final int MSG_REGISTER_ERROR_LISTENER = 7;
	 public static final int MSG_ERROR_RESPONSE = 8;
	 public static final int MSG_SET_FOREGROUND_SCAN_PERIOD = 9;
	 public static final int MSG_SET_BACKGROUND_SCAN_PERIOD = 10;
	 public static final int ERROR_COULD_NOT_START_LOW_ENERGY_SCANNING = -1;
	 static final long EXPIRATION_MILLIS = TimeUnit.SECONDS.toMillis(10L);
	 private static final String SCAN_START_ACTION_NAME = "startScan";
	 private static final String AFTER_SCAN_ACTION_NAME = "afterScan";
	 private static final Intent SCAN_START_INTENT = new Intent("startScan");
	 private static final Intent AFTER_SCAN_INTENT = new Intent("afterScan");
	 private final Messenger messenger;
	 private final BluetoothAdapter.LeScanCallback leScanCallback;
	 private BluetoothAdapter adapter;
	 private AlarmManager alarmManager;
	 private HandlerThread handlerThread;
	 private Handler handler;
	 private Runnable afterScanCycleTask;
	 private boolean scanning;
	 private Messenger errorReplyTo;
	 private BroadcastReceiver bluetoothBroadcastReceiver;
	 private BroadcastReceiver scanStartBroadcastReceiver;
	 private PendingIntent scanStartBroadcastPendingIntent;
	 private BroadcastReceiver afterScanBroadcastReceiver;
	 private PendingIntent afterScanBroadcastPendingIntent;
	 private ScanPeriodData foregroundScanPeriod;
	 private ScanPeriodData backgroundScanPeriod;
	 
	 private Messenger replyToDiscover;
 
	 public StickerService()
	 {
		 this.messenger = new Messenger(new IncomingHandler());
 
		 this.leScanCallback = new InternalLeScanCallback();
 
		 this.foregroundScanPeriod = new ScanPeriodData(TimeUnit.SECONDS.toMillis(1L), TimeUnit.SECONDS.toMillis(0L));
 
		 this.backgroundScanPeriod = new ScanPeriodData(TimeUnit.SECONDS.toMillis(5L), TimeUnit.SECONDS.toMillis(30L));
	 }
 
	 public void onCreate()
	 {
		 super.onCreate();
		 L.i("Creating service");
 
		 this.alarmManager = ((AlarmManager)getSystemService("alarm"));
		 BluetoothManager bluetoothManager = (BluetoothManager)getSystemService("bluetooth");
		 this.adapter = bluetoothManager.getAdapter();
 
		 this.handlerThread = new HandlerThread("BeaconServiceThread", 10);
		 this.handlerThread.start();
		 this.handler = new Handler(this.handlerThread.getLooper());
 
		 this.bluetoothBroadcastReceiver = createBluetoothBroadcastReceiver();
		 this.scanStartBroadcastReceiver = createScanStartBroadcastReceiver();
		 this.afterScanBroadcastReceiver = createAfterScanBroadcastReceiver();
		 registerReceiver(this.bluetoothBroadcastReceiver, new IntentFilter("android.bluetooth.adapter.action.STATE_CHANGED"));
		 registerReceiver(this.scanStartBroadcastReceiver, new IntentFilter("startScan"));
		 registerReceiver(this.afterScanBroadcastReceiver, new IntentFilter("afterScan"));
		 this.afterScanBroadcastPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, AFTER_SCAN_INTENT, 0);
		 this.scanStartBroadcastPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, SCAN_START_INTENT, 0);
	 }
 
	 public void onDestroy()
	 {
		 L.i("Service destroyed");
		 unregisterReceiver(this.bluetoothBroadcastReceiver);
		 unregisterReceiver(this.scanStartBroadcastReceiver);
		 unregisterReceiver(this.afterScanBroadcastReceiver);
 
		 if (this.adapter != null) {
	       stopScanning();
		 }
 
	     removeAfterScanCycleCallback();
	     this.handlerThread.quit();
 
	     super.onDestroy();
	 }
 
	 public IBinder onBind(Intent intent)
	 {
		 return this.messenger.getBinder();
	 }
 
	 private void startDiscover(Messenger replyTo) {
		 this.replyToDiscover = replyTo; 
		 checkNotOnUiThread();
		 Preconditions.checkNotNull(this.adapter, "Bluetooth adapter cannot be null");
		 startScanning();
	 }
 
	 private void StopDiscover() {
		 checkNotOnUiThread();
	    	 removeAfterScanCycleCallback();
	    	 stopScanning();
	 }
 
	 private void startScanning() {
		 if (this.scanning) {
			 L.d("Scanning already in progress, not starting one more");
			 return;
		 }
	     if (!this.adapter.isEnabled()) {
	    	 L.d("Bluetooth is disabled, not starting scanning");
	    	 return;
	     }
	     if (!this.adapter.startLeScan(this.leScanCallback)) {
	    	 L.wtf("Bluetooth adapter did not start le scan");
	    	 sendError(Integer.valueOf(-1));
	    	 return;
	     }
	     this.scanning = true;
	     removeAfterScanCycleCallback();
	     setAlarm(this.afterScanBroadcastPendingIntent, scanPeriodTimeMillis());
	 }
 
	 private void stopScanning()
	 {
		 try {
			 this.scanning = false;
			 this.adapter.stopLeScan(this.leScanCallback);
		 } catch (Exception e) {
			 L.wtf("BluetoothAdapter throws unexpected exception", e);
		 }
	 }
 
	 private void sendError(Integer errorId) {
		 if (this.errorReplyTo != null) {
			 Message errorMsg = Message.obtain(null, 8);
			 errorMsg.obj = errorId;
			 try {
				 this.errorReplyTo.send(errorMsg);
			 } catch (RemoteException e) {
				 L.e("Error while reporting message, funny right?", e);
			 }
		 }
	 }
 
	 private long scanPeriodTimeMillis() {
		 return this.foregroundScanPeriod.scanPeriodMillis;	    
	 }
 
	 private long scanWaitTimeMillis()
	 {
		 return this.foregroundScanPeriod.waitTimeMillis;
	 }
 
	 private void setAlarm(PendingIntent pendingIntent, long delayMillis)
	 {
		 this.alarmManager.set(2, SystemClock.elapsedRealtime() + delayMillis, pendingIntent);
	 }
 
	 private void checkNotOnUiThread()
	 {
		 //Preconditions.checkArgument(Looper.getMainLooper().getThread() != Thread.currentThread(), "This cannot be run on UI thread, starting BLE scan can be expensive");
 
		 Preconditions.checkNotNull(Boolean.valueOf(this.handlerThread.getLooper() == Looper.myLooper()), "It must be executed on service's handlerThread");
	 }
 
	 private BroadcastReceiver createBluetoothBroadcastReceiver()
	 {
		 return new BroadcastReceiver()
		 {
			 public void onReceive(Context context, Intent intent) {
				 if ("android.bluetooth.adapter.action.STATE_CHANGED".equals(intent.getAction())) {
					 int state = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", -1);
					 if (state == 10)
						 StickerService.this.handler.post(new Runnable()
						 {
							 public void run() {
								 L.i("Bluetooth is OFF: stopping scanning");
								 StickerService.this.removeAfterScanCycleCallback();
								 StickerService.this.stopScanning();
							 }
						 });
					 else if (state == 12)
						 StickerService.this.handler.post(new Runnable()
						 {
							 public void run() {
								 StickerService.this.startScanning();
							 }
						 });
				 }
			 }
		 };
	 }
 
	 private void removeAfterScanCycleCallback()
	 {
		 this.handler.removeCallbacks(this.afterScanCycleTask);
		 this.alarmManager.cancel(this.afterScanBroadcastPendingIntent);
	     this.alarmManager.cancel(this.scanStartBroadcastPendingIntent);
	 }
 
	 private BroadcastReceiver createAfterScanBroadcastReceiver() {
		 return new BroadcastReceiver()
		 {
			 public void onReceive(Context context, Intent intent) {
				 StickerService.this.handler.post(StickerService.this.afterScanCycleTask);
			 }
		 };
	 }
 
	 private BroadcastReceiver createScanStartBroadcastReceiver() {
		 return new BroadcastReceiver()
		 {
			 public void onReceive(Context context, Intent intent) {
				 StickerService.this.handler.post(new Runnable()
				 {
					 public void run() {
						 StickerService.this.startScanning();
					 }
				 });
			 }
		 };
	 }
 
	 private class InternalLeScanCallback implements BluetoothAdapter.LeScanCallback
	 {
		 private InternalLeScanCallback()
		 {
		 }
 
		 public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord)
		 {
			 StickerService.this.checkNotOnUiThread();
			 Sticker sticker = Utils.beaconFromLeScan(device, rssi, scanRecord);
			 
			 if (sticker == null) {
				 //L.v("Parse error: " + HashCode.fromBytes(scanRecord).toString());
				 return;
			 }
			 
			 //if (!JaaleeBeacons.isJaaleeBeacon(sticker))
			 //{
				 //Log.e("JAALEE", "Current Beacon not an Jaalee Beacon");
			 //	 return;
			 //}
			 
//			 Log.i("HELLOTEST:", "Beacon Beacon:"+beacon.getProximityUUID());
			 //通知找到Sticker了
			 Message rangingResponseMsg = Message.obtain(null, 3);
			 rangingResponseMsg.obj = sticker;
			 try {
				replyToDiscover.send(rangingResponseMsg);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		 }
	 }
 
	 private class IncomingHandler extends Handler
	 {
		 private IncomingHandler()
		 {
		 }
 
		 public void handleMessage(Message msg)
		 {
			 final int what = msg.what;
			 final Object obj = msg.obj;
			 final Messenger replyTo = msg.replyTo;
			 StickerService.this.handler.post(new Runnable()
			 {
				 public void run() {
					 switch (what) {
					 case 1:						 
						 StickerService.this.startDiscover(replyTo);
						 break;
					 case 2:
						 StickerService.this.StopDiscover();
						 break;
					 case 4:
						 break;
					 case 5:
						 break;
					 case 7:
						 StickerService.this.errorReplyTo = replyTo;
						 break;
					 case 9:
						 L.d("Setting foreground scan period: " + StickerService.this.foregroundScanPeriod);
						 StickerService.this.foregroundScanPeriod = ((ScanPeriodData)obj);
						 break;
					 case 10:
						 L.d("Setting background scan period: " + StickerService.this.backgroundScanPeriod);
						 StickerService.this.backgroundScanPeriod = ((ScanPeriodData)obj);
						 break;
					 case 3:
					 case 6:
					 case 8:
					 default:
						 L.d("Unknown message: what=" + what + " obj=" + obj);
					 }
				 }
			 });
		 }
	 }
 }
