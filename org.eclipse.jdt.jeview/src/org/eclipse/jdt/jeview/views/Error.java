/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
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
package org.eclipse.jdt.jeview.views;

import java.util.Objects;

public class Error extends JEAttribute {
	public static final String ERROR= "ERROR";

	private final JEAttribute fParent;
	private final String fName;
	private final Exception fException;

	public Error(JEAttribute parent, String name, Exception exception) {
		fParent= parent;
		fName= name;
		fException= exception;
	}

	@Override
	public JEAttribute getParent() {
		return fParent;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || !obj.getClass().equals(getClass())) {
			return false;
		}

		Error other= (Error) obj;
		if (!Objects.equals(fParent, other.fParent)) {
			return false;
		}

		if (!Objects.equals(fName, other.fName)) {
			return false;
		}

		if (!Objects.equals(fException, other.fException)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return (fParent != null ? fParent.hashCode() : 0)
				+ (fName != null ? fName.hashCode() : 0)
				+ (fException != null ? fException.hashCode() : 0);
	}

	@Override
	public JEAttribute[] getChildren() {
		return EMPTY;
	}

	@Override
	public String getLabel() {
		return (fName == null ? "" : fName + ": ") + fException.toString();
	}

	public Exception getException() {
		return fException;
	}

	@Override
	public Object getWrappedObject() {
		return fException;
	}
}
