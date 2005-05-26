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
package org.eclipse.jdt.internal.junit.wizards;

public class TestSuiteClassListRange {

	private final int fStart;
	private final int fEnd;

	public TestSuiteClassListRange(int start, int end) {
		fStart = start;
		fEnd = end;
	}

	public int getEnd() {
		return fEnd;
	}

	public int getStart() {
		return fStart;
	}

}
