/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
package org.eclipse.jdt.internal.ui.launcher;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.debug.core.model.IDebugElement;

import org.eclipse.jdt.debug.core.IJavaStackFrame;

/**
 * A generic example Java properties action. Subclass and provide
 * specific action and enabling code.
 */
public abstract class StackFrameAction extends OpenTypeAction {
	
	public boolean isEnabledFor(Object element) {
		return element instanceof IAdaptable && ((IAdaptable) element).getAdapter(IJavaStackFrame.class) != null;
	}
		
	protected IDebugElement getDebugElement(IAdaptable element) {
		return (IDebugElement)element.getAdapter(IJavaStackFrame.class);
	}
}
