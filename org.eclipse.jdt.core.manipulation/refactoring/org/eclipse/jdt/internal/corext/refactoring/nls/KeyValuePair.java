/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.refactoring.nls;

public class KeyValuePair {

	public String fKey;
	public String fValue;

	public KeyValuePair(String key, String value) {
		fKey= key;
		fValue= value;
	}

	public String getKey() {
		return fKey;
	}

	public void setKey(String key) {
		fKey= key;
	}

	public String getValue() {
		return fValue;
	}

	public void setValue(String value) {
		fValue= value;
	}
}
