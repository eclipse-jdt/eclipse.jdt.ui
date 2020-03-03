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
package org.eclipse.jdt.jeview.views;


/**
 *
 */
public abstract class JEAttribute {

	protected static final JEAttribute[] EMPTY= new JEAttribute[0];

	public abstract JEAttribute getParent();
	public abstract JEAttribute[] getChildren();
	public abstract String getLabel();

	public abstract Object getWrappedObject();

	@Override
	public abstract boolean equals(Object obj);

	@Override
	public abstract int hashCode();

}
