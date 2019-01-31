/*
 * cyborg-core is an extendable  module based framework for Android.
 *
 * Copyright (C) 2018  Adam van der Kruk aka TacB0sS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nu.art.storage;

import com.nu.art.core.interfaces.Serializer;
import com.nu.art.storage.PreferencesModule.SharedPrefs;

import java.lang.reflect.Type;

import static com.nu.art.storage.PreferencesModule.EXPIRES_POSTFIX;
import static com.nu.art.storage.PreferencesModule.JsonSerializer._Serializer;

@SuppressWarnings("UnusedReturnValue")
public final class CustomPreference<ItemType>
	extends PreferenceKey<CustomPreference<ItemType>, ItemType> {

	private ItemType cache;
	private Type itemType;
	private Serializer<Object, String> serializer = _Serializer;

	public CustomPreference() {}

	public CustomPreference(String key, ItemType defaultValue) {
		super(key, defaultValue);
	}

	public CustomPreference(String key, Type itemType, ItemType defaultValue) {
		super(key, defaultValue);
		setItemType(itemType);
	}

	public CustomPreference<ItemType> setItemType(Type itemType) {
		this.itemType = itemType;
		return this;
	}

	public CustomPreference<ItemType> setSerializer(Serializer<Object, String> serializer) {
		this.serializer = serializer;
		return this;
	}

	public CustomPreference<ItemType> setItemType(Class<ItemType> itemType, Serializer<Object, String> serializer) {
		this.itemType = itemType;
		this.serializer = serializer;
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected ItemType _get(SharedPrefs preferences, String key, ItemType defaultValue) {
		if (cache != null)
			return cache;

		String value = preferences.get(key, null);
		if (value != null)
			try {
				return cache = (ItemType) serializer.deserialize(value, itemType);
			} catch (Exception e) {
				logError("Error while deserializing item type: " + itemType, e);
			}

		return cache = (ItemType) serializer.deserialize(serializer.serialize(defaultValue), itemType);
	}

	@Override
	protected boolean areEquals(ItemType s1, ItemType s2) {
		return false;
	}

	public void set(final String value) {
		set(value, false);
	}

	public void set(final String value, boolean printToLog) {
		final SharedPrefs preferences = getPreferences();
		if (printToLog)
			logInfo("+----+ SET: " + key + ": " + value);

		preferences.put(key, value);
		if (expires != -1)
			preferences.put(key + EXPIRES_POSTFIX, System.currentTimeMillis());
	}

	@Override
	protected void _set(SharedPrefs preferences, String key, ItemType value) {
		String valueAsString = value == null ? null : serializer.serialize(value);
		preferences.put(key, valueAsString);
	}

	public void delete() {
		super.delete();
		cache = null;
	}
}
