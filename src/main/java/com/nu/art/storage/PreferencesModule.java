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

	public interface Storage {

		/**
		 * Will clear the preference
		 */
		void clear();

		/**
		 * Will clear the mem cache preference
		 */
		void clearMemCache();

		/**
		 * Will save the preference - synchronously
		 */
		void save();
	}

	public interface StorageListener {

		void onSavingError(IOException e);

		void onLoadingError(IOException e);
	}

	final class StorageImpl
		implements Storage {

		@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
		private final HashMap<String, Object> temp = new HashMap<>();
		private final HashMap<String, Object> data = new HashMap<>();
		private boolean loaded;
		private String name;
		private File storageFile;

		private StorageImpl(String name) {
			this.name = name;
		}

		final StorageImpl setStorageFile(File storageFile) {
			this.storageFile = storageFile;
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
			_save();
		}

		final void remove(String key) {
			synchronized (data) {
				data.remove(key);
			}
			_save();
		}

		public final void clear() {
			clearMemCache();
			_save(0);
			savingHandler.post(new Runnable() {
				@Override
				public void run() {
					synchronized (data) {
						loaded = false;
					}
				}
			});
		}

		public final void clearMemCache() {
			synchronized (data) {
				if (DebugFlag.isEnabled())
					logInfo("Clearing mem cache for: '" + name + "'");

				data.clear();
			}
		}

		private Runnable save = new RunnableMonitor(new Runnable() {
			@Override
			public void run() {
				try {
					if (DebugFlag.isEnabled())
						logInfo("Saving: " + name);
					temp.clear();
					temp.putAll(data);
					File tempFile = new File(storageFile.getParentFile(), storageFile.getName() + ".tmp");

					FileTools.writeToFile(gson.toJson(temp), tempFile, Charsets.UTF_8);
					FileTools.delete(storageFile);
					FileTools.renameFile(tempFile, storageFile);

					//					logInfo("Saved: " + name);
				} catch (final IOException e) {
					dispatchModuleEvent("Error saving shared preferences: " + name, StorageListener.class, new Processor<StorageListener>() {
						@Override
						public void process(StorageListener listener) {
							listener.onSavingError(e);
						}
					});
				} finally {
					temp.clear();
				}
			}
		});

		@Override
		public void save() {
			save.run();
		}

		private void _save() {
			_save(100);
		}

		private void _save(int delay) {
			savingHandler.remove(save);
			savingHandler.post(delay, save);
		}

		@SuppressWarnings("unchecked")
		private void load() {
			synchronized (data) {
				if (loaded)
					return;
			}

			try {
				if (DebugFlag.isEnabled())
					logInfo("Loading: " + name);

				String textRead = FileTools.readFullyAsString(storageFile, Charsets.UTF_8);
				HashMap map = gson.fromJson(textRead, HashMap.class);
				if (map != null) {
					logInfo("Loaded Storage: " + name + " from: " + storageFile);//, new WhoCalledThis("load storage"));
					synchronized (data) {
						data.putAll(map);
						loaded = true;
					}
				}
			} catch (final IOException e) {
				dispatchModuleEvent("Error loading shared preferences '" + name + "' from: " + storageFile.getAbsolutePath(), StorageListener.class, new Processor<StorageListener>() {
					@Override
					public void process(StorageListener listener) {
						listener.onLoadingError(e);
					}
				});
			}
		}
	}

	static final String DefaultStorageGroup = "DefaultStorage";

	static final String EXPIRES_POSTFIX = "-Expires";

	private Gson gson = new Gson();
	private final HashMap<String, StorageImpl> storageMap = new HashMap<>();
	private JavaHandler savingHandler;
	private File storageDefaultFolder;

	private PreferencesModule() {}

	@Override
	protected void init() {
		if (storageDefaultFolder == null)
			throw new ImplementationMissingException("MUST set storage root folder");

		if (!storageDefaultFolder.exists()) {
			try {
				FileTools.mkDir(storageDefaultFolder);
			} catch (IOException e) {
				throw new ImplementationMissingException("Unable to create root storage folder: " + storageDefaultFolder.getAbsolutePath());
			}
		}

		savingHandler = new JavaHandler().start("shared-preferences");
	}

	public final void defineGroup(String name, File pathToFile) {
		createStorageGroupImpl(name, pathToFile);
	}

	public final void setStorageFolder(String storageFolder) {
		setStorageFolder(new File(storageFolder));
	}

	public final void setStorageFolder(File storageFolder) {
		this.storageDefaultFolder = storageFolder;
	}

	public void setGson(Gson gson) {
		this.gson = gson;
	}

	public void clear() {
		synchronized (storageMap) {
			for (StorageImpl storage : storageMap.values()) {
				storage.clear();
			}
		}
	}

	public void clearMemCache() {
		synchronized (storageMap) {
			for (StorageImpl storage : storageMap.values()) {
				storage.clearMemCache();
			}
		}
	}

	private StorageImpl createStorageGroupImpl(String name, File pathToFile) {
		StorageImpl prefs = new StorageImpl(name).setStorageFile(pathToFile);
		prefs.load();
		storageMap.put(name, prefs);
		return prefs;
	}

	public final Storage getStorage(String storageGroup) {
		StorageImpl preferences = storageMap.get(storageGroup);
		if (preferences == null) {
			File pathToFile = new File(storageDefaultFolder, storageGroup);
			storageMap.put(storageGroup, preferences = createStorageGroupImpl(storageGroup, pathToFile));
		} else
			preferences.load();

		return preferences;
	}

	public static class JsonSerializer
		extends Serializer<Object, String> {

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
