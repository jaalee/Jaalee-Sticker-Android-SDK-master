package com.jaalee.sdk.connection;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
/**
 * @author JAALEE, Inc
 * 
 * @Support dev@jaalee.com
 * @Sales: sales@jaalee.com
 * 
 * @see http://www.jaalee.com/
 */

public class BatteryLifeService
  implements BluetoothService
{
	private final HashMap<UUID, BluetoothGattCharacteristic> characteristics = new HashMap();
	private final HashMap<UUID, ReadCallback> writeCallbacks = new HashMap();
	
	public void processGattServices(List<BluetoothGattService> services)
	{
		for (BluetoothGattService service : services)
			if (JaaleeUuid.STICKER_BATTERY_LIFE.equals(service.getUuid())) {
				this.characteristics.put(JaaleeUuid.STICKER_BATTERY_LIFE_CHAR, service.getCharacteristic(JaaleeUuid.STICKER_BATTERY_LIFE_CHAR));
			}
	}
	
	public Integer getBatteryPercent() {
		return this.characteristics.containsKey(JaaleeUuid.STICKER_BATTERY_LIFE_CHAR) ? Integer.valueOf(getUnsignedByte(((BluetoothGattCharacteristic)this.characteristics.get(JaaleeUuid.STICKER_BATTERY_LIFE_CHAR)).getValue())) : null;
	}
	
	public BluetoothGattCharacteristic beforeCharacteristicRead(UUID uuid, ReadCallback callback) {
		if (this.writeCallbacks.containsKey(uuid))
		{
			this.writeCallbacks.remove(uuid);
		}
		this.writeCallbacks.put(uuid, callback);
		return (BluetoothGattCharacteristic)this.characteristics.get(uuid);
	}

	public void update(BluetoothGattCharacteristic characteristic)
	{
		this.characteristics.put(characteristic.getUuid(), characteristic);		
		ReadCallback readCallback = (ReadCallback)this.writeCallbacks.remove(characteristic.getUuid());
		if (readCallback != null)
		{
			readCallback.onReadIntegerValue(getBatteryPercent());
		}
	}

	public Collection<BluetoothGattCharacteristic> getAvailableCharacteristics() {
		List chars = new ArrayList(this.characteristics.values());
		chars.removeAll(Collections.singleton(null));
		return chars;
	}
	
	private static int getUnsignedByte(byte[] bytes) {
		return unsignedByteToInt(bytes[0]);
	}
	private static int unsignedByteToInt(byte value)
	{
		return value & 0xFF;
	}
	
	@Override
	public void onCharacteristicWrite(
			BluetoothGattCharacteristic paramBluetoothGattCharacteristic,
			int state) {
		// TODO Auto-generated method stub
		
	}
}
