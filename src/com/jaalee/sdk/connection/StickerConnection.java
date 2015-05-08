package com.jaalee.sdk.connection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.jaalee.sdk.Sticker;
import com.jaalee.sdk.Utils;
import com.jaalee.sdk.internal.HashCode;
import com.jaalee.sdk.utils.L;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Establishes connection to the beacon and reads its characteristics. Exposes
 * methods to change beacon's UUID, major, minor, advertising interval and
 * broadcasting power values. You can only change those values only if there is
 * established connection to beacon. It is very important to close() connection
 * when it is not needed.
 */

public class StickerConnection {

	public static Set<Integer> ALLOWED_POWER_LEVELS = Collections
			.unmodifiableSet(new HashSet(Arrays.asList(new Integer[] {
					Integer.valueOf(-30), Integer.valueOf(-20),
					Integer.valueOf(-16), Integer.valueOf(-12),
					Integer.valueOf(-8), Integer.valueOf(-4),
					Integer.valueOf(0), Integer.valueOf(4) })));
	private final Context context;
	private final BluetoothDevice device;
	private final ConnectionCallback connectionCallback;
	private final Handler handler;
	private final BluetoothGattCallback bluetoothGattCallback;
	// private final Runnable timeoutHandler;

	private boolean mStartWritePass = false;// 是否是Jaalee新版的固件
	public boolean mCurrentIsJaaleeNewBeacon = false;// 是否是Jaalee新版的固件
	private String mPassword;
	private boolean mPassWordWriteSuccess = false;;

	private final StickerStateService mBeaconStateService;
	private final StickerTxPower mBeaconTxPowerService;
	private final StickerLinkLossService mStickerLinkLossService;

	private final StickerNameService mNameService;
	private final BatteryLifeService mBatteryService;
	private final JaaleeService mBeaconService;
	private final AlertService mAlertService;

	private final Map<UUID, BluetoothService> uuidToService;
	private boolean didReadCharacteristics;
	private LinkedList<BluetoothGattCharacteristic> toFetch;
	private long aAuth;
	private long bAuth;
	private BluetoothGatt bluetoothGatt;
	private boolean mCurrentAuthed = false;

	public StickerConnection(Context context, Sticker beacon,
			ConnectionCallback connectionCallback) {
		mCurrentIsJaaleeNewBeacon = false;
		this.context = context;
		this.mCurrentAuthed = false;
		this.device = deviceFromBeacon(beacon);
		this.toFetch = new LinkedList<BluetoothGattCharacteristic>();
		this.handler = new Handler();
		this.connectionCallback = connectionCallback;
		this.bluetoothGattCallback = createBluetoothGattCallback();
		// this.timeoutHandler = createTimeoutHandler();
		this.mNameService = new StickerNameService();
		this.mBatteryService = new BatteryLifeService();
		this.mBeaconService = new JaaleeService();
		this.mAlertService = new AlertService();
		this.mBeaconStateService = new StickerStateService();
		this.mBeaconTxPowerService = new StickerTxPower();
		this.mStickerLinkLossService = new StickerLinkLossService();

		this.uuidToService = new HashMap<UUID, BluetoothService>();
		this.uuidToService
				.put(JaaleeUuid.STICKER_BATTERY_LIFE, mBatteryService);
		this.uuidToService.put(JaaleeUuid.JAALEE_STICKER_SERVICE,
				mBeaconService);
		this.uuidToService.put(JaaleeUuid.STICKER_ALERT, mAlertService);
		this.uuidToService.put(JaaleeUuid.STICKER_NAME, mNameService);
		this.uuidToService.put(JaaleeUuid.STICKER_STATE_SERVICE,
				mBeaconStateService);
		this.uuidToService.put(JaaleeUuid.STICKER_TX_POWER_SERVICE,
				mBeaconTxPowerService);
	}

	private BluetoothDevice deviceFromBeacon(Sticker beacon) {
		BluetoothManager bluetoothManager = (BluetoothManager) this.context
				.getSystemService("bluetooth");
		BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
		return bluetoothAdapter.getRemoteDevice(beacon.getMacAddress());
	}

	/**
	 * Starts connection flow to device with password.
	 * 
	 * @param password
	 *            The password
	 */
	public boolean connectStickerWithPassword(String password) {
		if (password == null || password.length() != 6) {
			Log.e("JAALEE Sticker", "JAALEE Sticker:"
					+ "The pass password not Available");
			return false;
		}
		mPassword = password;
		L.d("Trying to connect to GATT");
		this.didReadCharacteristics = true;
		this.mCurrentIsJaaleeNewBeacon = false;
		StickerNameService.mCurrentIsJaaleeNewBeacon = false;
		StickerTxPower.mCurrentIsJaaleeNewBeacon = false;
		StickerStateService.mCurrentIsJaaleeNewBeacon = false;
		JaaleeService.mCurrentIsJaaleeNewBeacon = false;
		mPassWordWriteSuccess = false;
		this.mCurrentAuthed = false;

		this.mStartWritePass = false;
		this.bluetoothGatt = this.device.connectGatt(this.context, false,
				this.bluetoothGattCallback);
		// this.handler.postDelayed(this.timeoutHandler,
		// TimeUnit.SECONDS.toMillis(10L));
		return true;
	}

	/**
	 * Disconnect with the beacon
	 */
	public void disconnect() {
		if (this.bluetoothGatt != null) {
			this.bluetoothGatt.disconnect();
			this.mCurrentAuthed = false;
			this.bluetoothGatt.close();
		}
		// this.handler.removeCallbacks(this.timeoutHandler);
	}

	/**
	 * 
	 * @return Return true if the beacon is connected
	 */
	public boolean isConnected() {
		BluetoothManager bluetoothManager = (BluetoothManager) this.context
				.getSystemService("bluetooth");
		int connectionState = bluetoothManager.getConnectionState(this.device,
				7);
		// return (connectionState == 2) && (this.didReadCharacteristics);
		return (connectionState == 2);
	}

	/**
	 * Call current Sticker
	 */
	public void CallSticker() {
		if ((!isConnected())
				|| (!this.mAlertService
						.hasCharacteristic(JaaleeUuid.STICKER_ALERT_CHAR))) {
			L.w("Not connected to sticker. Discarding changing proximity UUID.");
			// writeCallback.onError();
			return;
		}
		BluetoothGattCharacteristic AlertChar = this.mAlertService
				.beforeCharacteristicWrite(JaaleeUuid.STICKER_ALERT_CHAR, null);
		byte[] arrayOfByte = new byte[1];
		arrayOfByte[0] = 1;
		AlertChar.setValue(arrayOfByte);
		this.bluetoothGatt.writeCharacteristic(AlertChar);
	}

	public boolean ReadRemoteRssi() {
		if (!isConnected()) {
			L.w("Not connected to sticker. Discarding changing proximity UUID.");
			// writeCallback.onError();
			return false;
		}

		return this.bluetoothGatt.readRemoteRssi();
	}

	private void BeaconKeepConnect(WriteCallback writeCallback) {
		if ((!isConnected())
				|| (!this.mBeaconService
						.hasCharacteristic(JaaleeUuid.STICKER_KEEP_CONNECT_CHAR))) {
			L.w("Not connected to beacon. Discarding changing proximity UUID.");
			writeCallback.onError();
			return;
		}
		BluetoothGattCharacteristic uuidChar = this.mBeaconService
				.beforeCharacteristicWrite(
						JaaleeUuid.STICKER_KEEP_CONNECT_CHAR, writeCallback);

		byte[] arrayOfByte;
		if (this.mStartWritePass) {
			// Log.i("NRF  TEST121", "NRF  NRF写入");
			arrayOfByte = HashCode.fromString(
					mPassword.replaceAll("-", "").toLowerCase()).asBytes();
		} else {
			// Log.i("NRF  TEST121", "NRF  TI写入");
			arrayOfByte = new byte[1];
			arrayOfByte[0] = 1;
		}

		uuidChar.setValue(arrayOfByte);

		this.bluetoothGatt.writeCharacteristic(uuidChar);
	}

	public void ReadBatteryLevel(ReadCallback readCallback) {
		if (!isConnected()) {
			L.w("Not connected to beacon. Discarding reading battery level.");
			readCallback.onError();
			return;
		}
		BluetoothGattCharacteristic uuidChar = this.mBatteryService
				.beforeCharacteristicRead(JaaleeUuid.STICKER_BATTERY_LIFE_CHAR,
						readCallback);
		this.bluetoothGatt.readCharacteristic(uuidChar);
	}

	public void writeStickerTxPower(int state, WriteCallback writeCallback) {
		if (!isConnected()) {
			L.w("Not connected to beacon. Discarding changing state.");
			writeCallback.onError();
			return;
		}

		byte[] arrayOfByte = new byte[1];

		if (mCurrentIsJaaleeNewBeacon) {
			switch (state) {
			case JaaleeDefine.JAALEE_TX_POWER_4_DBM:
				arrayOfByte[0] = 1;
				break;
			case JaaleeDefine.JAALEE_TX_POWER_0_DBM:
				arrayOfByte[0] = 2;
				break;
			case JaaleeDefine.JAALEE_TX_POWER_MINUS_4_DBM:
				arrayOfByte[0] = 3;
				break;
			case JaaleeDefine.JAALEE_TX_POWER_MINUS_8_DBM:
				arrayOfByte[0] = 4;
				break;
			case JaaleeDefine.JAALEE_TX_POWER_MINUS_12_DBM:
				arrayOfByte[0] = 5;
				break;
			case JaaleeDefine.JAALEE_TX_POWER_MINUS_16_DBM:
				arrayOfByte[0] = 6;
				break;
			case JaaleeDefine.JAALEE_TX_POWER_MINUS_20_DBM:
				arrayOfByte[0] = 7;
				break;
			case JaaleeDefine.JAALEE_TX_POWER_MINUS_30_DBM:
				arrayOfByte[0] = 8;
				break;
			case JaaleeDefine.JAALEE_TX_POWER_MINUS_40_DBM:
				arrayOfByte[0] = 9;
				break;
			default:
				arrayOfByte[0] = 1;
				break;
			}
		} else {
			switch (state) {
			case JaaleeDefine.JAALEE_TX_POWER_0_DBM:
				arrayOfByte[0] = 0;
				break;
			case JaaleeDefine.JAALEE_TX_POWER_MINUS_6_DBM:
				arrayOfByte[0] = 1;
				break;
			case JaaleeDefine.JAALEE_TX_POWER_MINUS_23_DBM:
				arrayOfByte[0] = 2;
				break;
			default:
				arrayOfByte[0] = 0;
				break;
			}
		}

		final BluetoothGattCharacteristic Char = this.mBeaconTxPowerService
				.beforeCharacteristicWrite(JaaleeUuid.STICKER_TX_POWER_CHAR,
						writeCallback);

		if (mCurrentIsJaaleeNewBeacon) {
			Char.setValue(arrayOfByte);
			this.bluetoothGatt.writeCharacteristic(Char);
		} else {
			byte[] PasswordBytes = HashCode.fromString(mPassword.toLowerCase())
					.asBytes();
			Char.setValue(this.arraycat(PasswordBytes, arrayOfByte));
			this.bluetoothGatt.writeCharacteristic(Char);
			new Handler().postDelayed(new Runnable() {
				public void run() {
					StickerConnection.this.bluetoothGatt
							.readCharacteristic(Char);
				}
			}, 3000);

		}
	}

	// 是否允许断开连接是报警
	public void EnableStickerRingWhenDisconnect(int state) {
		if (!isConnected()) {
			L.w("Not connected to beacon. Discarding changing state.");
			return;
		}

		byte[] arrayOfByte = new byte[1];

		if (mCurrentIsJaaleeNewBeacon) {
			switch (state) {
			case JaaleeDefine.JAALEE_ENABLE_RING_WHEN_DISCONNECT:
				arrayOfByte[0] = 2;
				break;
			case JaaleeDefine.JAALEE_DISABLE_RING_WHEN_DISCONNECT:
				arrayOfByte[0] = 1;
				break;
			default:
				arrayOfByte[0] = 2;
				break;
			}
		} else {
			switch (state) {
			case JaaleeDefine.JAALEE_ENABLE_RING_WHEN_DISCONNECT:
				arrayOfByte[0] = 0;
				break;
			case JaaleeDefine.JAALEE_DISABLE_RING_WHEN_DISCONNECT:
				arrayOfByte[0] = 1;
				break;
			default:
				arrayOfByte[0] = 0;
				break;
			}
		}

		final BluetoothGattCharacteristic Char = this.mStickerLinkLossService
				.beforeCharacteristicWrite(JaaleeUuid.STICKER_LINK_LOSS_CHAR);

		Char.setValue(arrayOfByte);
		this.bluetoothGatt.writeCharacteristic(Char);
	}

	// 修改名字
	/**
	 * Config Sticker's device name
	 * 
	 * @param name
	 *            the name of the beacon,like "jaalee"
	 * @param writeCallback
	 *            Callback to be invoked when write is completed.
	 */
	public void writeStickerName(String name, WriteCallback writeCallback) {
		if (!isConnected()) {
			L.w("Not connected to beacon. Discarding changing state.");
			writeCallback.onError();
			return;
		}

		if (name.length() > 15) {
			L.w("the lenth of the name should be less than 15.");
			writeCallback.onError();
			return;
		}
		
		final BluetoothGattCharacteristic Char;
		
		if (mCurrentIsJaaleeNewBeacon)
		{
			Char = this.mNameService
					.beforeCharacteristicWrite(JaaleeUuid.STICKER_NEW_NAME_CHAR,
							writeCallback);
		}
		else
		{
			Char = this.mNameService
					.beforeCharacteristicWrite(JaaleeUuid.STICKER_NAME_CHAR,
							writeCallback);
		}


		byte[] value = name.getBytes();

		if (mCurrentIsJaaleeNewBeacon) {
			Char.setValue(value);
			this.bluetoothGatt.writeCharacteristic(Char);
		} else {
			byte[] PasswordBytes = HashCode.fromString(mPassword.toLowerCase())
					.asBytes();
			Char.setValue(this.arraycat(PasswordBytes, value));
			this.bluetoothGatt.writeCharacteristic(Char);
			new Handler().postDelayed(new Runnable() {
				public void run() {
					StickerConnection.this.bluetoothGatt
							.readCharacteristic(Char);
				}
			}, 3000);

		}
	}

	private Runnable createTimeoutHandler() {
		return new Runnable() {
			public void run() {
				L.v("Timeout while authenticating");
				if (!StickerConnection.this.didReadCharacteristics) {
					if (StickerConnection.this.bluetoothGatt != null) {
						StickerConnection.this.bluetoothGatt.disconnect();
						StickerConnection.this.bluetoothGatt.close();
						StickerConnection.this.bluetoothGatt = null;
					}
					StickerConnection.this.notifyAuthenticationError();
				}
			}
		};
	}

	private BluetoothGattCallback createBluetoothGattCallback() {
		return new BluetoothGattCallback() {
			public void onConnectionStateChange(BluetoothGatt gatt, int status,
					int newState) {
				if (newState == 2) {
					L.d("Connected to GATT server, discovering services: "
							+ gatt.discoverServices());
				} else if ((newState == 0)
						&& (!StickerConnection.this.didReadCharacteristics)) {
					L.w("Disconnected from GATT server");
					StickerConnection.this.notifyAuthenticationError();
				} else if (newState == 0) {
					L.w("Disconnected from GATT server");
					StickerConnection.this.notifyDisconnected();
				}
			}

			@Override
			public void onReadRemoteRssi(BluetoothGatt gatt, int rssi,
					int status) {

				StickerConnection.this.onReadRemoteRssi(gatt, rssi, status);
			}

			public void onCharacteristicRead(BluetoothGatt gatt,
					BluetoothGattCharacteristic characteristic, int status) {
				if (status == 0) {

					if (JaaleeUuid.STICKER_KEEP_CONNECT_CHAR
							.equals(characteristic.getUuid())) {
						// 获取到了值
						// keep connect
						if (characteristic.getValue().length == 3) {
							StickerConnection.this.mStartWritePass = true;
						} else {
							StickerConnection.this.mStartWritePass = false;
							;
						}

						if (!mPassWordWriteSuccess) {
							StickerConnection.this.handler.postDelayed(
									new Runnable() {
										@Override
										public void run() {
											// TODO Auto-generated method stub
											StickerConnection.this
													.BeaconKeepConnect(new WriteCallback() {

														@Override
														public void onSuccess() {
															// TODO
															// Auto-generated
															// method stub
															// Log.i("BeaconConnection",
															// "Keep Connect Successful");
															mPassWordWriteSuccess = true;
														}

														@Override
														public void onError() {
															// TODO
															// Auto-generated
															// method stub
															// Log.i("BeaconConnection",
															// "Keep Connect Failed");
														}
													});

										}
									}, TimeUnit.SECONDS.toMillis(1L));
						}
					}
					BluetoothService temp = ((BluetoothService) StickerConnection.this.uuidToService
							.get(characteristic.getService().getUuid()));
					if (temp != null) {
						temp.update(characteristic);
					}
					if (mPassWordWriteSuccess) {
						StickerConnection.this.readCharacteristics(gatt);
					}

				} else {
					L.w("Failed to read characteristic");
					StickerConnection.this.toFetch.clear();
					StickerConnection.this.notifyAuthenticationError();
				}
			}

			public void onCharacteristicWrite(BluetoothGatt gatt,
					BluetoothGattCharacteristic characteristic, int status) {
				BluetoothService temp = ((BluetoothService) StickerConnection.this.uuidToService
						.get(characteristic.getService().getUuid()));
				if (temp != null) {
					temp.onCharacteristicWrite(characteristic, status);
				}

				if (JaaleeUuid.STICKER_KEEP_CONNECT_CHAR.equals(characteristic
						.getUuid())) {
					StickerConnection.this.onAuthenticationCompleted(gatt);
				}
			}

			public void onServicesDiscovered(BluetoothGatt gatt, int status) {
				if (status == 0) {
					L.v("Services discovered");
					// Log.i("NRF  TEST121", "NRF  TEST121");
					StickerConnection.this.processDiscoveredServices(gatt
							.getServices());
					// BeaconConnection.this.startAuthentication(gatt);

					for (BluetoothGattService service : gatt.getServices()) {
						if (JaaleeUuid.STICKER_STATE_SERVICE.equals(service
								.getUuid())) {
							mCurrentIsJaaleeNewBeacon = true;// Nordic Sticker
							StickerNameService.mCurrentIsJaaleeNewBeacon = true;
							StickerTxPower.mCurrentIsJaaleeNewBeacon = true;
							StickerStateService.mCurrentIsJaaleeNewBeacon = true;
							JaaleeService.mCurrentIsJaaleeNewBeacon = true;

						} else if (JaaleeUuid.JAALEE_STICKER_SERVICE
								.equals(service.getUuid())) {
							// 读取特征值
							StickerConnection.this.handler.postDelayed(
									new Runnable() {
										@Override
										public void run() {
											// TODO Auto-generated method stub
											BluetoothGattCharacteristic keepChar = StickerConnection.this.mBeaconService
													.getKeepUUIDChar();
											StickerConnection.this.bluetoothGatt
													.readCharacteristic(keepChar);
											// Log.i("NRF  TEST121",
											// "NRF  TEST122");
										}
									}, TimeUnit.SECONDS.toMillis(1L));
						}
					}

				} else {
					L.w("Could not discover services, status: " + status);
					StickerConnection.this.notifyAuthenticationError();
				}
			}
		};
	}

	private void notifyAuthenticationError() {
		// this.handler.removeCallbacks(this.timeoutHandler);
		this.connectionCallback.onAuthenticationError();
	}

	private void notifyDisconnected() {
		this.connectionCallback.onDisconnected();
	}

	private void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
		this.connectionCallback.onReadRemoteRssi(gatt, rssi, status);
	}

	private void processDiscoveredServices(List<BluetoothGattService> services) {

		this.mNameService.processGattServices(services);
		this.mBeaconService.processGattServices(services);
		this.mAlertService.processGattServices(services);
		this.mBatteryService.processGattServices(services);
		this.mBeaconStateService.processGattServices(services);
		this.mBeaconTxPowerService.processGattServices(services);
		this.mStickerLinkLossService.processGattServices(services);

		this.toFetch.clear();
		this.toFetch.addAll(this.mBeaconService.getAvailableCharacteristics());
		this.toFetch.addAll(this.mNameService.getAvailableCharacteristics());
		// this.toFetch.addAll(this.mAlertService.getAvailableCharacteristics());
		this.toFetch.addAll(this.mBatteryService.getAvailableCharacteristics());
		this.toFetch.addAll(this.mBeaconStateService
				.getAvailableCharacteristics());
		this.toFetch.addAll(this.mBeaconTxPowerService
				.getAvailableCharacteristics());
	}

	// private void startAuthentication(BluetoothGatt gatt)
	// {
	// if (!this.mNameService.isAvailable()) {
	// L.w("Authentication service is not available on the beacon");
	// notifyAuthenticationError();
	// return;
	// }
	// this.aAuth = AuthMath.randomUnsignedInt();
	// BluetoothGattCharacteristic seedChar =
	// this.mNameService.getAuthSeedCharacteristic();
	// seedChar.setValue(AuthMath.firstStepSecret(this.aAuth), 20, 0);
	// gatt.writeCharacteristic(seedChar);
	// }

	private void onSeedWriteCompleted(final BluetoothGatt gatt,
			final BluetoothGattCharacteristic characteristic) {
		this.handler.postDelayed(new Runnable() {
			public void run() {
				gatt.readCharacteristic(characteristic);
			}
		}, 500L);
	}

	private void onBeaconSeedResponse(BluetoothGatt gatt,
			BluetoothGattCharacteristic characteristic) {
		// Integer intValue = characteristic.getIntValue(20, 0);
		// this.bAuth =
		// UnsignedInteger.fromIntBits(intValue.intValue()).longValue();
		// String macAddress = this.device.getAddress().replace(":", "");
		// BluetoothGattCharacteristic vectorChar =
		// this.mNameService.getAuthVectorCharacteristic();
		// vectorChar.setValue(AuthMath.secondStepSecret(this.aAuth, this.bAuth,
		// macAddress));
		// gatt.writeCharacteristic(vectorChar);
	}

	private void onAuthenticationCompleted(final BluetoothGatt gatt) {
		this.handler.postDelayed(new Runnable() {
			public void run() {
				StickerConnection.this.readCharacteristics(gatt);
			}
		}, 500L);
	}

	private void readCharacteristics(BluetoothGatt gatt) {
		if (this.toFetch.size() != 0) {
			BluetoothGattCharacteristic temp = (BluetoothGattCharacteristic) this.toFetch
					.poll();
			// Log.i("NRF  TEST121", "NRF:" + temp.getUuid());
			gatt.readCharacteristic(temp);

		} else if (this.bluetoothGatt != null && !this.mCurrentAuthed) {
			onAuthenticated();
			this.mCurrentAuthed = true;
		}
	}

	private void onAuthenticated() {
		L.v("Authenticated to beacon");
		// this.handler.removeCallbacks(this.timeoutHandler);
		this.didReadCharacteristics = true;
		this.connectionCallback.onAuthenticated(new StickerCharacteristics(
				this.mBeaconService, this.mNameService,
				this.mBeaconTxPowerService, this.mBeaconStateService,
				this.mBatteryService));
	}

	byte[] arraycat(byte[] buf1, byte[] buf2) {
		byte[] bufret = null;
		int len1 = 0;
		int len2 = 0;
		if (buf1 != null)
			len1 = buf1.length;
		if (buf2 != null)
			len2 = buf2.length;
		if (len1 + len2 > 0)
			bufret = new byte[len1 + len2];
		if (len1 > 0)
			System.arraycopy(buf1, 0, bufret, 0, len1);
		if (len2 > 0)
			System.arraycopy(buf2, 0, bufret, len1, len2);
		return bufret;
	}

}
