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
