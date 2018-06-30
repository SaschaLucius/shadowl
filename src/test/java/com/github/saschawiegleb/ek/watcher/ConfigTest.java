package com.github.saschawiegleb.ek.watcher;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ConfigTest {

	@Test
	public void test() {
		assertThat(Config.PORT.getInt()).isEqualTo(8080);
	}

}
