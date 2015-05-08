package com.jaalee.sdk.connection;

import android.bluetooth.BluetoothGatt;

/**
 * Callback used to indicate state of the connection to the beacon.
 *
 */
public abstract interface ConnectionCallback
{
	/**
	 * Invoked when connection to beacon is established.
	 * @param paramBeaconCharacteristics Beacon's characteristics.
	 */
	public abstract void onAuthenticated(StickerCharacteristics paramBeaconCharacteristics);

	
	/**
	 * Invoked when there was a problem within authentication to the beacon.
	 */
	public abstract void onAuthenticationError();

	/**
	 * Connection was closed to the beacon. Can happen when beacon is out of range.
	 */
	public abstract void onDisconnected();
	
	public abstract void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status);
}

