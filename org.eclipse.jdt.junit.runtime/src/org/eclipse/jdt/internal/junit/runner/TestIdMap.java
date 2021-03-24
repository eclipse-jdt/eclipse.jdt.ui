/*******************************************************************************
 * Copyright (c) 2006, 2021 IBM Corporation and others.
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

package org.eclipse.jdt.internal.junit.runner;

import java.util.HashMap;

public class TestIdMap {
	private HashMap<ITestIdentifier, String> fIdMap= new HashMap<>();

	private int fNextId= 1;

	public String getTestId(ITestIdentifier identifier) {
		Object id= fIdMap.get(identifier);
		if (id != null)
			return (String) id;
		String newId= Integer.toString(fNextId++);
		fIdMap.put(identifier, newId);
		return newId;
	}

	public String getTestId(ITestReference ref) { // not used
		return getTestId(ref.getIdentifier());
	}
}
