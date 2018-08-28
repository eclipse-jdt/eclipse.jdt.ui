/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.text.tests.performance;

public abstract class Condition {

	public abstract boolean isTrue();

	public boolean isStrict() {
		return false;
	}

	public boolean busyWaitFor(long maxTime) {
		return busyWaitFor(maxTime, isStrict());
	}

	public boolean busyWaitFor(long maxTime, boolean strict) {
		long maxEndTime= System.currentTimeMillis() + maxTime;
		while (System.currentTimeMillis() < maxEndTime)
			if (isTrue())
				return !strict || System.currentTimeMillis() < maxEndTime;
		return false;
	}
}
