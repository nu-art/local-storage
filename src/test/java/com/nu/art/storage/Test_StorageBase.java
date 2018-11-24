package com.nu.art.storage;

import com.nu.art.core.exceptions.runtime.BadImplementationException;
import com.nu.art.storage.Test_Setup.PrefModel;

import org.junit.Test;

public abstract class Test_StorageBase {

	@Test
	public void test_CustomPrefsMem() {
		testModel(Test_Setup.getCustomModel());
	}

	@Test
	public void test_EnumPrefsMem() {
		testModel(Test_Setup.getEnumModel());
	}

	@Test
	public void test_LongPrefsMem() {
		testModel(Test_Setup.getLongModel());
	}

	@Test
	public void test_FloatPrefsMem() {
		testModel(Test_Setup.getFloatModel());
	}

	@Test
	public void test_DoublePrefsMem() {
		testModel(Test_Setup.getDoubleModel());
	}

	@Test
	public void test_StringPrefsMem() {
		testModel(Test_Setup.getStringModel());
	}

	@Test
	public void test_IntegerPrefsMem() {
		testModel(Test_Setup.getIntegerModel());
	}

	protected void sleepFor(int interval) {
		try {
			Thread.sleep(interval);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	protected <T> void setAndValidate(PreferenceKey<?, T> pref, T value) {
		pref.set(value);
		validate(pref, value);
	}

	protected <T> void deleteAndValidate(PreferenceKey<?, T> pref, T defaultValue) {
		pref.delete();
		validate(pref, defaultValue);
	}

	protected <T> void validate(PreferenceKey<?, T> pref, T value) {
		T got = pref.get();
		if (!got.equals(value))
			throw new BadImplementationException("didn't receive expected value: " + value + " - got: " + got);
	}

	protected abstract <T> void testModel(PrefModel<T> model);
}
