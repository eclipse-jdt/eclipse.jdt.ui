/*
 * (c) Copyright 2002 IBM Corporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

import org.eclipse.jface.action.Action;

import org.eclipse.ui.IWorkbenchWindow;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.util.OpenTypeHierarchyUtil;

/**
 * An action to open a type hierarchy either in its own perspective or
 * in the type hierarchy view part of the current perspective.
 */
public class OpenHierarchyAction extends Action {

	private IJavaElement[] fElements;
	private IWorkbenchWindow fWindow;

	public OpenHierarchyAction(IWorkbenchWindow window, IJavaElement[] elements) {
		fElements= elements;
		fWindow= window;
		setText(JavaUIMessages.getString("OpenHierarchyPerspectiveItem.menulabel")); //$NON-NLS-1$
		setAccelerator(SWT.F4);
	}

	public void runWithEvent(Event event) {
		OpenTypeHierarchyUtil.open(fElements, fWindow, event.stateMask);
	}
}
