/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.core.refactoring;

/**
 * An abstract default implementation for a change object - suitable for subclassing. This class manages
 * the change's active status.
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public abstract class Change implements IChange {

	private boolean fIsActive= true;

	/* (Non-Javadoc)
	 * Method declared in IChange.
	 */
	public void aboutToPerform() {
		// do nothing.
	}
	 
	/* (Non-Javadoc)
	 * Method declared in IChange.
	 */
	public void performed() {
		// do nothing.
	} 
	
	/* (Non-Javadoc)
	 * Method declared in IChange.
	 */
	public void setActive(boolean active) {
		fIsActive= active;
	}
	
	/* (Non-Javadoc)
	 * Method declared in IChange.
	 */
	public boolean isActive() {
		return fIsActive;
	}
}