/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.leaks;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class JavaLeakTestActionDelegate implements IWorkbenchWindowActionDelegate, IEditorActionDelegate {

	public IWorkbenchWindow fWindow;
	public IEditorPart fEditor;

	public JavaLeakTestActionDelegate() {

	}

	public void init(IWorkbenchWindow window) {
		fWindow= window;
	}

	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		fEditor= targetEditor;
	}

	public void run(IAction action) {
		System.out.println("running JavaLeakTestActionDelegate");
	}

	public void dispose() {
		fWindow= null;
		fEditor= null;
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}
}
