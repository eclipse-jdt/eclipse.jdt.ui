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
package org.eclipse.jdt.ui.tests.refactoring.infra;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;

public class MockWorkbenchSite implements IWorkbenchSite {
	
	private ISelectionProvider fProvider;
	public MockWorkbenchSite(Object[] elements){
		fProvider= new MockSelectionProvider(elements);
	}
	public IWorkbenchPage getPage() {
		return null;
	}

	public ISelectionProvider getSelectionProvider() {
		return fProvider;
	}

	public Shell getShell() {
		return new Shell();
	}

	public IWorkbenchWindow getWorkbenchWindow() {
		return null;
	}

	public void setSelectionProvider(ISelectionProvider provider) {
		fProvider= provider;
	}
}
