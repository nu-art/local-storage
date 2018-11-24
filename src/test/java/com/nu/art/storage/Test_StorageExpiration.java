package com.nu.art.storage;

import com.nu.art.storage.Test_Setup.PrefModel;

import org.junit.BeforeClass;

import static com.nu.art.storage.Test_Setup.moduleManager;

public class Test_StorageExpiration
	extends Test_StorageBase {

	@BeforeClass
	@SuppressWarnings("unchecked")
	public static void setUp() {
		Test_Setup.init();
	}

	@Override
	protected <T> void testModel(PrefModel<T> model) {
		model.pref.setExpires(1000);
		setAndValidate(model.pref, model.value);
		sleepFor(300);

		validate(model.pref, model.value);
		sleepFor(300);
		moduleManager.getModule(PreferencesModule.class).clearMemCache();

		validate(model.pref, model.value);
		sleepFor(500);

		validate(model.pref, model.defaultValue);
	}
}
