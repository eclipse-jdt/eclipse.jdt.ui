/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange.EditChange;

/* package */ class TextEditChangeElement extends ChangeElement {
	
	private static final ChangeElement[] fgChildren= new ChangeElement[0];
	
	private EditChange fChange;
	
	public TextEditChangeElement(ChangeElement parent, EditChange change) {
		super(parent);
		fChange= change;
		Assert.isNotNull(fChange);
	}
	
	/**
	 * Returns the <code>TextEditChange</code> managed by this node.
	 * 
	 * @return the <code>TextEditChange</code>
	 */
	public EditChange getTextEditChange() {
		return fChange;
	}
		
	/* non Java-doc
	 * @see ChangeElement#setActive
	 */
	public void setActive(boolean active) {
		fChange.setActive(active);
	}
	
	/* non Java-doc
	 * @see ChangeElement.getActive
	 */
	public int getActive() {
		return fChange.isActive() ? ACTIVE : INACTIVE;
	}
	
	/* non Java-doc
	 * @see ChangeElement.getChildren
	 */
	public ChangeElement[] getChildren() {
		return fgChildren;
	}
}

