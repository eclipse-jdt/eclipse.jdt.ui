/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.launcher;

import org.eclipse.debug.core.DebugException;import org.eclipse.debug.core.model.IDebugElement;

import org.eclipse.jdt.debug.core.IJavaStackFrame;

/**
 * An example action that if an element has <code>IJavaStackFrameProperties</code>
 * prints the name of the receiving type name to system out.
 */
public class OpenOnReceivingTypeAction extends StackFrameAction {

	protected String getTypeNameToOpen(IDebugElement frame) throws DebugException {
		return ((IJavaStackFrame)frame).getReceivingTypeName();
	}
}
