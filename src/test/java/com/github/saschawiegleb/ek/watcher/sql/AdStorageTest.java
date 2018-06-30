package com.github.saschawiegleb.ek.watcher.sql;

import org.junit.Test;

public class AdStorageTest {

	@Test
	public void test() {
		AdStorage.loadDriver();
		AdStorage adStorage = new AdStorage();
		adStorage.createTables();
	}

}
