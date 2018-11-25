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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nu.art.core.exceptions.runtime.BadImplementationException;
import com.nu.art.core.exceptions.runtime.ImplementationMissingException;
import com.nu.art.core.file.Charsets;
import com.nu.art.core.generics.Processor;
import com.nu.art.core.interfaces.Serializer;
import com.nu.art.core.tools.FileTools;
import com.nu.art.core.utils.JavaHandler;
import com.nu.art.core.utils.ThreadMonitor.RunnableMonitor;
import com.nu.art.modular.core.Module;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;

@SuppressWarnings( {
	                   "unused",
	                   "WeakerAccess"
                   })
public final class PreferencesModule
	extends Module {

	public interface PreferencesListener {

		void onSavingError(IOException e);

		void onLoadingError(IOException e);
	}

	final class SharedPrefs {

		@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
		private final HashMap<String, Object> temp = new HashMap<>();
		private final HashMap<String, Object> data = new HashMap<>();
		private String name;
		private File pathToFile;

		private SharedPrefs(String name) {
			this.name = name;
		}

		final SharedPrefs setPathToFile(File pathToFile) {
			this.pathToFile = pathToFile;
			return this;
		}

		boolean get(String key, boolean defaultValue) {
			Object value = get(key);
			if (value == null)
				return defaultValue;

			if (value instanceof Boolean)
				return (boolean) value;

			throw new BadImplementationException("Expected type boolean for key: '" + key + "', but was: " + value.getClass());
		}

		long get(String key, long defaultValue) {
			Object value = get(key);
			if (value == null)
				return defaultValue;

			if (value instanceof Number)
				return ((Number) value).longValue();

			throw new BadImplementationException("Expected type long for key: '" + key + "', but was: " + value.getClass());
		}

		int get(String key, int defaultValue) {
			Object value = get(key);
			if (value == null)
				return defaultValue;

			if (value instanceof Number)
				return ((Number) value).intValue();

			throw new BadImplementationException("Expected type int for key: '" + key + "', but was: " + value.getClass());
		}

		float get(String key, float defaultValue) {
			Object value = get(key);
			if (value == null)
				return defaultValue;

			if (value instanceof Number)
				return ((Number) value).floatValue();

			throw new BadImplementationException("Expected type float for key: '" + key + "', but was: " + value.getClass());
		}

		double get(String key, double defaultValue) {
			Object value = get(key);
			if (value == null)
				return defaultValue;

			if (value instanceof Number)
				return ((Number) value).doubleValue();

			throw new BadImplementationException("Expected type double for key: '" + key + "', but was: " + value.getClass());
		}

		final String get(String key, String defaultValue) {
			Object value = get(key);
			if (value == null)
				return defaultValue;

			if (value instanceof String)
				return (String) value;

			throw new BadImplementationException("Expected type String for key: '" + key + "', but was: " + value.getClass());
		}

		final Object get(String key) {
			synchronized (data) {
				return data.get(key);
			}
		}

		final void put(String key, Object value) {
			synchronized (data) {
				data.put(key, value);
			}
			save();
		}

		final void remove(String key) {
			synchronized (data) {
				data.remove(key);
			}
			save();
		}

		final void clear() {
			synchronized (data) {
				data.clear();
			}
			save();
		}

		Runnable save = new RunnableMonitor(new Runnable() {
			@Override
			public void run() {
				try {
					//					logInfo("Saving: " + name);
					temp.clear();
					temp.putAll(data);
					FileTools.writeToFile(gson.toJson(data), pathToFile, Charsets.UTF_8);
					//					logInfo("Saved: " + name);
				} catch (final IOException e) {
					dispatchModuleEvent("Error saving shared preferences: " + name, PreferencesListener.class, new Processor<PreferencesListener>() {
						@Override
						public void process(PreferencesListener listener) {
							listener.onSavingError(e);
						}
					});
				}
			}
		});

		private void save() {
			savingHandler.remove(save);
			savingHandler.post(100, save);
		}

		@SuppressWarnings("unchecked")
		private void load() {
			try {
				//				logInfo("Loading: " + name);
				String textRead = FileTools.readFullyAsString(pathToFile, Charsets.UTF_8);
				HashMap map = gson.fromJson(textRead, HashMap.class);
				if (map != null) {
					logInfo("Loaded Storage: " + name + " from: " + pathToFile);
					data.putAll(map);
				}
			} catch (final IOException e) {
				dispatchModuleEvent("Error loading shared preferences: " + name, PreferencesListener.class, new Processor<PreferencesListener>() {
					@Override
					public void process(PreferencesListener listener) {
						listener.onLoadingError(e);
					}
				});
			}
		}
	}

	static final String DefaultStorageGroup = "DefaultStorage";

	static final String EXPIRES_POSTFIX = "-Expires";

	private Gson gson = new Gson();
	private HashMap<String, SharedPrefs> preferencesMap = new HashMap<>();
	private JavaHandler savingHandler;
	private File storageFolder;

	private PreferencesModule() {}

	public void setStorageFolder(String storageFolder) {
		this.storageFolder = new File(storageFolder);
	}

	@Override
	protected void init() {
		if (storageFolder == null)
			throw new ImplementationMissingException("MUST set storage root folder");

		if (!storageFolder.exists()) {
			try {
				FileTools.mkDir(storageFolder);
			} catch (IOException e) {
				throw new ImplementationMissingException("Unable to create root storage folder: " + storageFolder.getAbsolutePath());
			}
		}

		savingHandler = new JavaHandler().start("shared-preferences");
	}

	public void setGson(Gson gson) {
		this.gson = gson;
	}

	public void deleteEverything() {
		for (String key : preferencesMap.keySet()) {
			dropPreferences(key);
		}
	}

	public void clearMemCache() {
		preferencesMap.clear();
	}

	public void dropPreferences(final String storageGroup) {
		getPreferences(storageGroup).clear();
	}

	private SharedPrefs createStorageGroupImpl(String name, File pathToFile) {
		SharedPrefs prefs = new SharedPrefs(name).setPathToFile(pathToFile);
		prefs.load();
		preferencesMap.put(name, prefs);
		return prefs;
	}

	final SharedPrefs getPreferences(String storageGroup) {
		SharedPrefs sharedPreferences = preferencesMap.get(storageGroup);
		if (sharedPreferences == null) {
			preferencesMap.put(storageGroup, sharedPreferences = createStorageGroupImpl(storageGroup, new File(storageFolder, storageGroup)));
		}

		return sharedPreferences;
	}

	public static class JsonSerializer
		implements Serializer<Object, String> {

		public static final JsonSerializer _Serializer = new JsonSerializer();

		public static final Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

		@Override
		public String serialize(Object o) {
			return gson.toJson(o);
		}

		@Override
		public Object deserialize(String from, Type toType) {
			return gson.fromJson(from, toType);
		}
	}
}
