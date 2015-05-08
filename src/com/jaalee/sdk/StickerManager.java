 package com.jaalee.sdk;
 
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Region;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import com.jaalee.sdk.internal.Preconditions;
import com.jaalee.sdk.service.ScanPeriodData;
import com.jaalee.sdk.service.StickerService;
import com.jaalee.sdk.utils.L;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;


 public class StickerManager
 {
//   private static final String ANDROID_MANIFEST_CONDITIONS_MSG = "AndroidManifest.xml does not contain android.permission.BLUETOOTH or android.permission.BLUETOOTH_ADMIN permissions. BeaconService may be also not declared in AndroidManifest.xml.";
   private final Context context;
   private final InternalServiceConnection serviceConnection;
   private final Messenger incomingMessenger;
   private Messenger serviceMessenger;
   private DiscoverListener discoverListener;
   private ErrorListener errorListener;
   private ServiceReadyCallback callback;
   private ScanPeriodData foregroundScanPeriod;
   private ScanPeriodData backgroundScanPeriod;
 
   public StickerManager(Context context)
   {
      this.context = ((Context)Preconditions.checkNotNull(context));
      this.serviceConnection = new InternalServiceConnection();
      this.incomingMessenger = new Messenger(new IncomingHandler());
   }
 
   /**
    * @return Returns true if device supports Bluetooth Low Energy.
    */
   public boolean hasBluetooth()
   {
     return this.context.getPackageManager().hasSystemFeature("android.hardware.bluetooth_le");
   }
   
   
 /**
  * 
  * @return Returns true if device supports Bluetooth Low Energy.
  */
   public boolean isBluetoothEnabled()
   {
	   if (!checkPermissionsAndService()) {
		   L.e("AndroidManifest.xml does not contain android.permission.BLUETOOTH or android.permission.BLUETOOTH_ADMIN permissions. BeaconService may be also not declared in AndroidManifest.xml.");
	       return false;
     }
	   BluetoothManager bluetoothManager = (BluetoothManager)this.context.getSystemService("bluetooth");
	   BluetoothAdapter adapter = bluetoothManager.getAdapter();
	   return (adapter != null) && (adapter.isEnabled());
   }
 
   /**
    * 
    * @return Checks if required Bluetooth permissions are set (android.permission.BLUETOOTH and android.permission.BLUETOOTH_ADMIN) along with BeaconService in AndroidManifest.xml.
    */
   public boolean checkPermissionsAndService()
   {
	   PackageManager pm = this.context.getPackageManager();
	   int bluetoothPermission = pm.checkPermission("android.permission.BLUETOOTH", this.context.getPackageName());
	   int bluetoothAdminPermission = pm.checkPermission("android.permission.BLUETOOTH_ADMIN", this.context.getPackageName());
 
	    Intent intent = new Intent(this.context, StickerService.class);
	     List<ResolveInfo> resolveInfo = pm.queryIntentServices(intent, 65536);
 
	     return (bluetoothPermission == 0) && (bluetoothAdminPermission == 0) && (resolveInfo.size() > 0);
   }
 
   /**
    * Connects to BeaconService.
    * @param callback Callback to be invoked when connection is made to service.
    */
   public void connect(ServiceReadyCallback callback)
   {
	   if (!checkPermissionsAndService()) 
	   {
		   L.e("AndroidManifest.xml does not contain android.permission.BLUETOOTH or android.permission.BLUETOOTH_ADMIN permissions. BeaconService may be also not declared in AndroidManifest.xml.");
	   }
	   this.callback = ((ServiceReadyCallback)Preconditions.checkNotNull(callback, "callback cannot be null"));
	   if (isConnectedToService()) {
		   callback.onServiceReady();
	   }
	   boolean bound = this.context.bindService(new Intent(this.context, StickerService.class), this.serviceConnection, 1);
 
	   if (!bound)
		   L.wtf("Could not bind service");
   }
 
   /**
    * Disconnects from BeaconService. If there were any ranging or monitoring in progress, they will be stopped. This should be typically called in onDestroy method.
    */
   public void disconnect()
   {
	   if (!isConnectedToService()) 
	   {
		   L.i("Not disconnecting because was not connected to service");
		   return;
	   }
	   
	   //hrb спринй
	   try {
		internalStopDiscover();
	   } catch (RemoteException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	   }
	  
	   this.context.unbindService(this.serviceConnection);
	   this.serviceMessenger = null;
   }

   /**
    * Sets new ranging listener. Old one will be discarded.
    * @param listener The listener
    */
   public void setDiscoverListener(DiscoverListener listener)
   {
	   this.discoverListener = ((DiscoverListener)Preconditions.checkNotNull(listener, "listener cannot be null"));
   }
 
   /**
    * Sets new error listener. Old one will be discarded.
    * @param listener The new listener
    */
   public void setErrorListener(ErrorListener listener)
   {
	   this.errorListener = listener;
	   if ((isConnectedToService()) && (listener != null))
		   registerErrorListenerInService();
   }
 
   /**
    * Changes defaults scanning periods when ranging is performed. Default values: scanPeriod=1s, waitTime=0s
    * @param scanPeriodMillis  How long to perform Bluetooth Low Energy scanning?
    * @param waitTimeMillis  How long to wait until performing next scanning?
    */
   public void setForegroundScanPeriod(long scanPeriodMillis, long waitTimeMillis)
   {
	   if (isConnectedToService())
		   setScanPeriod(new ScanPeriodData(scanPeriodMillis, waitTimeMillis), 10);
     else
    	 this.foregroundScanPeriod = new ScanPeriodData(scanPeriodMillis, waitTimeMillis);
   }
   
   
 /**
  * Changes defaults scanning periods when monitoring is performed. Default values: scanPeriod=5s, waitTime=25s
  * @param scanPeriodMillis How long to perform Bluetooth Low Energy scanning?
  * @param waitTimeMillis How long to wait until performing next scanning?
  */
   public void setBackgroundScanPeriod(long scanPeriodMillis, long waitTimeMillis)
   {
	   if (isConnectedToService())
		   setScanPeriod(new ScanPeriodData(scanPeriodMillis, waitTimeMillis), 9);
	   else
    	 this.backgroundScanPeriod = new ScanPeriodData(scanPeriodMillis, waitTimeMillis);
   }
 
   
   private void setScanPeriod(ScanPeriodData scanPeriodData, int msgId)
   {
	   Message scanPeriodMsg = Message.obtain(null, msgId);
	   scanPeriodMsg.obj = scanPeriodData;
	   try {
	       this.serviceMessenger.send(scanPeriodMsg);
	   } catch (RemoteException e) {
    	 L.e("Error while setting scan periods: " + msgId);
	   }
   }
 
   private void registerErrorListenerInService() {
	   Message registerMsg = Message.obtain(null, 7);
	   registerMsg.replyTo = this.incomingMessenger;
	   try {
		   this.serviceMessenger.send(registerMsg);
	   } catch (RemoteException e) {
		   L.e("Error while registering error listener");
	   }
   }
 
   
   /**
    * Starts ranging given range. Ranging results will be delivered to listener registered via setRangingListener(RangingListener). If given region is already ranged, this is no-op.
    * @param region Region to range.
    * @throws RemoteException  If communication with service failed.
    */
   public void startDiscover()
     throws RemoteException
   {
	   	if (!isConnectedToService()) {
	   		L.i("Not starting ranging, not connected to service");
	   		return;
	   	}

	   	Message startRangingMsg = Message.obtain(null, 1);
	   	startRangingMsg.replyTo = this.incomingMessenger;
	   	try {
	   		this.serviceMessenger.send(startRangingMsg);
	   	} catch (RemoteException e) {
	   		L.e("Error while starting ranging", e);
	   		throw e;
	   	}
   }
 
   public void stopDiscover() throws RemoteException {
	   if (!isConnectedToService()) {
		   L.i("Not stopping ranging, not connected to service");
		   return;
	   }
	   internalStopDiscover();
   }
 
   private void internalStopDiscover() throws RemoteException {
	   Message stopRangingMsg = Message.obtain(null, 2);
	   try {
		   this.serviceMessenger.send(stopRangingMsg);
		   } catch (RemoteException e) {
			   L.e("Error while stopping ranging", e);
			   throw e;
			   }
	   }
 
   private boolean isConnectedToService() {
	   return this.serviceMessenger != null;
   }
 
   private class IncomingHandler extends Handler
   {
     private IncomingHandler()
     {
     }
 
     public void handleMessage(Message msg)
     {
    	 switch (msg.what) {
    	 case 3:
    		 if (StickerManager.this.discoverListener != null) {
	         Sticker rangingResult = (Sticker)msg.obj;
	         StickerManager.this.discoverListener.onStickersDiscovered(rangingResult);
    		 }break;
    	 case 6:
    		 break;
    	 case 8:
    		 if (StickerManager.this.errorListener != null) {
    			 Integer errorId = (Integer)msg.obj;
    			 StickerManager.this.errorListener.onError(errorId);
    		 }
    		 break;
    	 default:
    		 L.d("Unknown message: " + msg);
    		 break;
    	 }
     }
   }
 
   private class InternalServiceConnection
     implements ServiceConnection
   {
     private InternalServiceConnection()
     {
     }
 
     public void onServiceConnected(ComponentName name, IBinder service)
     {
    	 StickerManager.this.serviceMessenger = new Messenger(service);
    	 if (StickerManager.this.errorListener != null) {
    		 StickerManager.this.registerErrorListenerInService();
    	 }
    	 if (StickerManager.this.foregroundScanPeriod != null) {
    		 StickerManager.this.setScanPeriod(StickerManager.this.foregroundScanPeriod, 9);
    		 StickerManager.this.foregroundScanPeriod = null;
    	 }
    	 if (StickerManager.this.backgroundScanPeriod != null) {
    		 StickerManager.this.setScanPeriod(StickerManager.this.backgroundScanPeriod, 10);
    		 StickerManager.this.backgroundScanPeriod = null;
    	 }
    	 if (StickerManager.this.callback != null) {
    		 StickerManager.this.callback.onServiceReady();
    		 StickerManager.this.callback = null;
    	 }
     }
 
     public void onServiceDisconnected(ComponentName name)
     {
    	 L.e("Service disconnected, crashed? " + name);
    	 StickerManager.this.serviceMessenger = null;
     }
   }
 
 }

