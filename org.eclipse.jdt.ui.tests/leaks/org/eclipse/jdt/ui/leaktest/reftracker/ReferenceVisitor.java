/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ui.leaktest.reftracker;


public class ReferenceVisitor {

	/**
	 * Visits a referenced object
	 * @param object the referenced object
	 * @param clazz the class of the reference object
	 * @param firstVisit if set this is the first visit
	 * @return <code>true</code> if the references from this object should be
	 * visited, and <code>false</code> if the children of this node should
	 * be skipped. If this is not the first visit, children will never be visited, regardless of the
	 * boolean returned.
	 */
	public boolean visit(ReferencedObject object, Class clazz, boolean firstVisit) {
		return true;
	}
}
