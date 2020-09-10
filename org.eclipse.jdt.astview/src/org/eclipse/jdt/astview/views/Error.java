/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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

package org.eclipse.jdt.astview.views;

import java.util.Objects;

import org.eclipse.swt.graphics.Image;


public class Error extends ExceptionAttribute {

	private final Object fParent;
	private final String fLabel;

	public Error(Object parent, String label, Throwable thrownException) {
		fParent= parent;
		fLabel= label;
		fException= thrownException;
	}

	@Override
	public Object[] getChildren() {
		return EMPTY;
	}

	@Override
	public Image getImage() {
		return null;
	}

	@Override
	public String getLabel() {
		return fLabel;
	}

	@Override
	public Object getParent() {
		return fParent;
	}

	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
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

		if (!Objects.equals(fLabel, other.fLabel)) {
			return false;
		}

		return true;
	}

	/*
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return (fParent != null ? fParent.hashCode() : 0)
				+ (fLabel != null ? fLabel.hashCode() : 0);
	}

}
