package com.nu.art.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nu.art.belog.BeLogged;
import com.nu.art.belog.DefaultLogClient;
import com.nu.art.core.exceptions.runtime.BadImplementationException;
import com.nu.art.core.interfaces.Serializer;
import com.nu.art.modular.core.ModuleManager;
import com.nu.art.modular.core.ModuleManagerBuilder;
import com.nu.art.modular.core.ModulesPack;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Type;

import static com.nu.art.storage.Test_Storage.JsonSerializer._Serializer;

public class Test_Storage {

	private ModuleManager moduleManager;

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

	static class Pack
		extends ModulesPack {

		Pack() {
			super(PreferencesModule.class);
		}

		@Override
		protected void init() {
			getModule(PreferencesModule.class).setStorageFolder("build/test/storage");
		}
	}

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() {
		BeLogged.getInstance().addClient(new DefaultLogClient());

		moduleManager = new ModuleManagerBuilder().addModulePacks(Pack.class).build();
	}

	@Test
	public void test_StringPrefsPersistence() {
		String defaultValue = "PAH";
		StringPreference pref = new StringPreference("stam", defaultValue);
		String value = "ashpa";

		genericPersistenceValidation(pref, defaultValue, value);
	}

	@Test
	public void test_CustomPrefsPersistence() {
		Model defaultValue = new Model("EMPTY");
		CustomPreference<Model> pref = new CustomPreference<>(_Serializer, "stam", Model.class, defaultValue);
		Model value = new Model("ZEVEL");

		genericPersistenceValidation(pref, defaultValue, value);
	}

	@Test
	public void test_CustomPrefsMem() {
		Model defaultValue = new Model("EMPTY");
		CustomPreference<Model> pref = new CustomPreference<>(_Serializer, "stam", Model.class, defaultValue);
		Model value = new Model("ZEVEL");

		setAndValidate(pref, value);
		deleteAndValidate(pref, defaultValue);

		sleepFor(10000);
	}

	private <T> void genericPersistenceValidation(IPreferenceKey<T> pref, T defaultValue, T value) {
		setAndValidate(pref, value);
		sleepFor(300);

		moduleManager.getModule(PreferencesModule.class).clearMemCache();
		validate(pref, value);

		deleteAndValidate(pref, defaultValue);
		sleepFor(300);

		moduleManager.getModule(PreferencesModule.class).clearMemCache();
		validate(pref, defaultValue);

		sleepFor(10000);
	}

	private void sleepFor(int intervale) {
		try {
			Thread.sleep(intervale);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void test_StringPrefsMem() {
		String defaultValue = "PAH";
		StringPreference pref = new StringPreference("stam", defaultValue);
		String value = "ashpa";

		setAndValidate(pref, value);
		deleteAndValidate(pref, defaultValue);

		sleepFor(10000);
	}

	private <T> void setAndValidate(IPreferenceKey<T> pref, T value) {
		pref.set(value);
		validate(pref, value);
	}

	private <T> void deleteAndValidate(IPreferenceKey<T> pref, T defaultValue) {
		pref.delete();
		validate(pref, defaultValue);
	}

	private <T> void validate(IPreferenceKey<T> pref, T value) {
		T got = pref.get();
		if (!got.equals(value))
			throw new BadImplementationException("didn't receive expected value: " + value + " - got: " + got);
	}

	class Model {

		Model(String value) {
			this.value = value;
		}

		Model() {
		}

		String value;

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;

			if (o == null || getClass() != o.getClass())
				return false;

			Model model = (Model) o;

			return value != null ? value.equals(model.value) : model.value == null;
		}

		@Override
		public int hashCode() {
			return value != null ? value.hashCode() : 0;
		}

		@Override
		public String toString() {
			return "Model{" + "value='" + value + '\'' + '}';
		}
	}
}
