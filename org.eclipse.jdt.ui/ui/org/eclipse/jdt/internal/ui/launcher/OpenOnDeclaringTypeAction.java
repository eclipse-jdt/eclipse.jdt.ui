/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */

package org.eclipse.jdt.internal.ui.launcher;

import org.eclipse.debug.core.DebugException;import org.eclipse.debug.core.model.IDebugElement;

import org.eclipse.jdt.debug.core.IJavaStackFrame;

/**
 * An example action that if an element has <code>IJavaStackFrameProperties</code>
 * prints the name of the declaring type name to system out.
 */
public class OpenOnDeclaringTypeAction extends StackFrameAction {
	
	protected String getTypeNameToOpen(IDebugElement frame) throws DebugException {
		return ((IJavaStackFrame)frame).getDeclaringTypeName();
	}
}
