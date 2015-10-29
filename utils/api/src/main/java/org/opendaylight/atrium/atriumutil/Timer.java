/*
 * This module is inserted by wipro and it is taken from 
 * https://github.com/opennetworkinglab/onos/blob/onos-1.2/utils/misc/src/main/java/org/onlab/util/Timer.java
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.atrium.atriumutil;

import org.jboss.netty.util.HashedWheelTimer;

/**
 * Hashed-wheel timer singleton. Care must be taken to shutdown the timer only
 * when the VM is ready to exit.
 */
public final class Timer {

	private static volatile HashedWheelTimer timer;

	// Ban public construction
	private Timer() {
	}

	/**
	 * Returns the singleton hashed-wheel timer.
	 *
	 * @return hashed-wheel timer
	 */
	public static HashedWheelTimer getTimer() {
		if (Timer.timer == null) {
			initTimer();
		}
		return Timer.timer;
	}

	private static synchronized void initTimer() {
		if (Timer.timer == null) {
			HashedWheelTimer hwTimer = new HashedWheelTimer();
			hwTimer.start();
			Timer.timer = hwTimer;
		}
	}

}