 package com.jaalee.sdk;
 
 import android.bluetooth.BluetoothAdapter;
 import android.bluetooth.BluetoothDevice;
 import android.bluetooth.BluetoothManager;
 import android.content.BroadcastReceiver;
 import android.content.Context;
 import android.content.Intent;
 import android.content.IntentFilter;
 import android.util.Log;

import com.jaalee.sdk.connection.JaaleeDefine;
import com.jaalee.sdk.internal.HashCode;
import com.jaalee.sdk.internal.Preconditions;
/**
 * @author JAALEE, Inc
 * 
 * @Support dev@jaalee.com
 * @Sales: sales@jaalee.com
 * 
 * @see http://www.jaalee.com/
 */
 public class Utils
 {
//	 private static final String TAG = Utils.class.getSimpleName();
//	 private static final int MANUFACTURER_SPECIFIC_DATA = 255;
 
	 public static Sticker beaconFromLeScan(BluetoothDevice device, int rssi, byte[] scanRecord)
	 {
		 
//		 if (rssi < -60)
//		 {
//			 return null;
//		 }
		 
		 if ("WWW.JAALEE.COM".equalsIgnoreCase(device.getName()))
		 {
			 return new Sticker(device.getName(), device.getAddress(), rssi);
		 }
		 
		 String scanRecordAsHex = HashCode.fromBytes(scanRecord).toString();
		 
		 if (scanRecordAsHex.length() < 40)
		 {
			 return null;
		 }
		 
		 String temp = String.format("%s", new Object[] { scanRecordAsHex.substring(18, 26)});		
		 
		 if (!temp.equalsIgnoreCase("6c6a0206"))
		 {
			 return null;
		 }
		 temp = String.format("%s", new Object[] { scanRecordAsHex.substring(26, 38)});
		 
//		 Log.i("JAALEE", "JAALEE"+ temp);
		 if (temp.contains("000000"))
		 {
			 return null;
		 }
		 return new Sticker(device.getName(), device.getAddress(), rssi);
	 }
 

	 public static int parseInt(String numberAsString)
   {
	   try
	   {
		   return Integer.parseInt(numberAsString); 
	   }
	   catch (NumberFormatException e) {
	   }
	   return 0;
   }
 
   public static int normalize16BitUnsignedInt(int value)
   {
	   return Math.max(1, Math.min(value, 65535));
   }
 
   public static void restartBluetooth(Context context, final RestartCompletedListener listener)
   {
	   BluetoothManager bluetoothManager = (BluetoothManager)context.getSystemService("bluetooth");
	   final BluetoothAdapter adapter = bluetoothManager.getAdapter();
 
	   IntentFilter intentFilter = new IntentFilter("android.bluetooth.adapter.action.STATE_CHANGED");
	   context.registerReceiver(new BroadcastReceiver()
	   {
		   public void onReceive(Context context, Intent intent) 
		   {
			   if ("android.bluetooth.adapter.action.STATE_CHANGED".equals(intent.getAction())) 
			   {
				   int state = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", -1);
				   if (state == 10) 
				   {
					   adapter.enable();//hrb  спринй
				   } 
				   else if (state == 12) 
				   {
					   context.unregisterReceiver(this);
					   listener.onRestartCompleted();
				   }
			   }
		   }
	   }
	   , intentFilter);
 
	   adapter.disable();
   }
 
   private static int unsignedByteToInt(byte value)
   {
	   return value & 0xFF;
   }
 }
