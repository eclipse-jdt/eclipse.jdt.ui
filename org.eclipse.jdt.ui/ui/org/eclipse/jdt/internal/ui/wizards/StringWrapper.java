/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards;

public class StringWrapper {

	private String fString;

	public StringWrapper(String string) {
		fString= string;
	}

	public void setString(String string) {
		fString= string;
	}

	public String getString() {
		return fString;
	}

	public String toString() {
		return getString();
	}

	public int hashCode() {
		return fString.hashCode();
	}

	public boolean equals(Object obj) {
		if (obj != null && getClass().equals(obj.getClass()))
			return ((StringWrapper) obj).fString.equals(fString);
		return false;
	}
}
