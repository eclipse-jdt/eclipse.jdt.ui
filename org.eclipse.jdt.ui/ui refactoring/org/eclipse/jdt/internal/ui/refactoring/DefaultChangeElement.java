/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.internal.corext.refactoring.base.IChange;

public class DefaultChangeElement extends ChangeElement {
	
	private IChange fChange;
	private ChangeElement[] fChildren;

	/**
	 * Creates a new <code>ChangeElement</code> for the given
	 * change.
	 * 
	 * @param parent the change element's parent or <code>null
	 * 	</code> if the change element doesn't have a parent
	 * @param change the actual change. Argument must not be
	 * 	<code>null</code>
	 */
	public DefaultChangeElement(ChangeElement parent, IChange change) {
		super(parent);
		fChange= change;
		Assert.isNotNull(fChange);
	}

	/**
	 * Returns the underlying <code>IChange</code> object.
	 * 
	 * @return the underlying change
	 */
	public IChange getChange() {
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
		int result= fChange.isActive() ? ACTIVE : INACTIVE;
		if (fChildren != null) {
			for (int i= 0; i < fChildren.length; i++) {
				result= ACTIVATION_TABLE[fChildren[i].getActive()][result];
				if (result == PARTLY_ACTIVE)
					break;
			}
		}
		return result;
	}
	
	/* non Java-doc
	 * @see ChangeElement.getChildren
	 */	
	public ChangeElement[] getChildren() {
		return fChildren;
	}
	
	/**
	 * Sets the children.
	 * 
	 * @param the children of this node. Must not be <code>null</code>
	 */
	public void setChildren(ChangeElement[] children) {
		Assert.isNotNull(children);
		fChildren= children;
	}		
}

