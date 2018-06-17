package com.github.saschawiegleb.ek.watcher;

import java.util.concurrent.TimeUnit;

public class SleepTimer {

	private static double SLEEP_TIMER = 90.0d;
	
	public static double getTimer() {
		return SLEEP_TIMER;
	}

	public static void resizeAndSleep(int count) {
		resizeSleepTimer(count);
		try {
			TimeUnit.SECONDS.sleep((long) SLEEP_TIMER);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static void resizeSleepTimer(int count) {
		switch (count) {
		case 0:
			SLEEP_TIMER += 1.0d;
			break;
		case 1:
			SLEEP_TIMER += 0.8d;
			break;
		case 2:
			SLEEP_TIMER += 0.6d;
			break;
		case 3:
			SLEEP_TIMER += 0.4d;
			break;
		case 4:
			SLEEP_TIMER += 0.2d;
			break;
		case 5:
			// SLEEP_TIMER += 0;
			break;
		case 6:
			SLEEP_TIMER -= 0.2d;
			break;
		case 7:
			SLEEP_TIMER -= 0.4d;
			break;
		case 8:
			SLEEP_TIMER -= 0.6d;
			break;
		case 9:
			SLEEP_TIMER -= 0.8d;
			break;
		case 10:
			SLEEP_TIMER -= 1.0d;
			break;
		default:
			SLEEP_TIMER -= 1.0d;
			break;
		}

		System.out.println(SLEEP_TIMER);
	}
}
