/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
 *     David Saff (saff@mit.edu) - initial API and implementation
 *             (bug 102632: [JUnit] Support for JUnit 4.)
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.launcher;

import org.eclipse.jdt.core.IType;

public class TestSearchResult {

	private final IType[] fTypes;
	private final ITestKind fTestKind;

	public TestSearchResult(IType[] types, ITestKind testKind) {
		fTypes = types;
		fTestKind = testKind;
	}

	public IType[] getTypes() {
		return fTypes;
	}

	public ITestKind getTestKind() {
		return fTestKind;
	}

	boolean isEmpty() {
		return getTypes().length <= 0;
	}
}
