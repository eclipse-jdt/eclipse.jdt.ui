/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
