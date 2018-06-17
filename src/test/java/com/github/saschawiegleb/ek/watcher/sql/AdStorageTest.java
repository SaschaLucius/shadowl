package com.github.saschawiegleb.ek.watcher.sql;

import static org.junit.Assert.fail;

import org.junit.Test;

public class AdStorageTest {

	@Test
	public void test() {
		AdStorage.loadDriver();
		AdStorage.createTables();

		fail("Not yet implemented");
	}

}
