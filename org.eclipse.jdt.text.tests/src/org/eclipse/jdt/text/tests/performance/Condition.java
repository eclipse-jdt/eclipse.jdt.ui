/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.text.tests.performance;

public abstract class Condition {

	public abstract boolean isTrue();
	
	public boolean busyWaitFor(long maxTime) {
		long maxEndTime= System.currentTimeMillis() + maxTime;
		while (System.currentTimeMillis() < maxEndTime)
			if (isTrue())
				return System.currentTimeMillis() < maxEndTime;
		return false;
	}
}
