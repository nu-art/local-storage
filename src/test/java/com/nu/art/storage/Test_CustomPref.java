package com.nu.art.storage;

import com.nu.art.core.exceptions.runtime.BadImplementationException;
import com.nu.art.storage.Test_Setup.PrefModel;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;

import static com.nu.art.storage.Test_Setup.moduleManager;
import static com.nu.art.storage.Test_Utils.sleepFor;

public class Test_CustomPref {

	@BeforeClass
	@SuppressWarnings("unchecked")
	public static void setUp() {
		Test_Setup.init();
	}

	@Test
	public void test_CustomPrefsStateful() {
		PrefModel<HashMap> model = Test_Setup.getCustomModelStateful();
		HashMap hashMap = model.pref.get();
		hashMap.put("pah", "zevel");
		model.pref.set(hashMap);

		sleepFor(300);

		moduleManager.getModule(PreferencesModule.class).clearMemCache();
		Object value = model.pref.get().get("pah");
		if (value == null)
			throw new BadImplementationException("did not save map correctly");

		if (!"zevel".equals(value))
			throw new BadImplementationException("did not save map correctly");

		model.pref.delete();

		value = model.pref.get().get("pah");
		if (value != null)
			throw new BadImplementationException("expected empty map.. but found value");

		if ("zevel".equals(value))
			throw new BadImplementationException("expected empty map.. but found value");
	}
}
