package com.nu.art.storage;

import com.nu.art.core.interfaces.Getter;

public interface IPreferenceKey<ItemType>
	extends Getter<ItemType> {

	void set(ItemType value);

	void set(ItemType value, boolean printToLog);

	ItemType get();

	ItemType get(boolean printToLog);

	void delete();

	void clearExpiration();
}
