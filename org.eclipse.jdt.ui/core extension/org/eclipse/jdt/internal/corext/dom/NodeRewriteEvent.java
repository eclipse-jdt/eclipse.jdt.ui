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
package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.jdt.core.dom.ASTNode;

/**
 *
 */
public class NodeRewriteEvent extends RewriteEvent {
	
	private Object fOriginalValue;
	private Object fNewValue;
		
	public NodeRewriteEvent(Object originalValue, Object newValue) {
		fOriginalValue= originalValue;
		fNewValue= newValue;
	}
		
	/**
	 * @return Returns the new value.
	 */
	public Object getNewValue() {
		return fNewValue;
	}
	
	/**
	 * @return Returns the original value.
	 */
	public Object getOriginalValue() {
		return fOriginalValue;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.dom.RewriteEvent#getChangeKind()
	 */
	public int getChangeKind() {
		if (fOriginalValue == fNewValue) {
			return UNCHANGED;
		}
		if (fOriginalValue == null) {
			return INSERTED;
		}
		if (fNewValue == null) {
			return REMOVED;
		}
		if (fOriginalValue.equals(fNewValue)) {
			return UNCHANGED;
		}
		return REPLACED;
	}
		

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.dom.RewriteEvent#isListRewrite()
	 */
	public boolean isListRewrite() {
		return false;
	}

	/*
	 * Sets a new value for the new node. Internal access only.
	 * @param newEntry The new value to set.
	 */
	/* package */ void setNewValue(ASTNode newEntry) {
		fNewValue= newEntry;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.dom.RewriteEvent#getChildren()
	 */
	public RewriteEvent[] getChildren() {
		return null;
	}
	

}
