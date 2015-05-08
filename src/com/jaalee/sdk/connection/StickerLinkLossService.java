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
public class StickerLinkLossService
  implements BluetoothService
{
	private final HashMap<UUID, BluetoothGattCharacteristic> characteristics = new HashMap();
	
	static boolean mCurrentIsJaaleeNewBeacon = false;//是否是Jaalee新版的固件
	
	public void processGattServices(List<BluetoothGattService> services)
	{
		for (BluetoothGattService service : services)
			if (JaaleeUuid.STICKER_LINK_LOSS_SERVICE.equals(service.getUuid())) {
				if (service.getCharacteristic(JaaleeUuid.STICKER_LINK_LOSS_CHAR) != null)
				{
					this.characteristics.put(JaaleeUuid.STICKER_LINK_LOSS_CHAR, service.getCharacteristic(JaaleeUuid.STICKER_LINK_LOSS_CHAR));	
				}
			}
	}

	public boolean hasCharacteristic(UUID uuid)
	{
		return this.characteristics.containsKey(uuid);
	}
	
	public void update(BluetoothGattCharacteristic characteristic)
	{
		this.characteristics.put(characteristic.getUuid(), characteristic);
	}
	
	public BluetoothGattCharacteristic beforeCharacteristicWrite(UUID uuid) {
		return (BluetoothGattCharacteristic)this.characteristics.get(uuid);
	}
 
	public void onCharacteristicWrite(BluetoothGattCharacteristic characteristic, int status) {
	}

	public Collection<BluetoothGattCharacteristic> getAvailableCharacteristics() {
		List chars = new ArrayList(this.characteristics.values());
		chars.removeAll(Collections.singleton(null));
		return chars;
	}

}
