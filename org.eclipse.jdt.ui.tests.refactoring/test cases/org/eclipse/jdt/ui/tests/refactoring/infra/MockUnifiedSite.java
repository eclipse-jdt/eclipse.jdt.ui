package org.eclipse.jdt.ui.tests.refactoring.infra;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

import org.eclipse.jdt.ui.actions.UnifiedSite;

public class MockUnifiedSite extends UnifiedSite {
	private ISelectionProvider fProvider;
	public MockUnifiedSite(Object[] elements){
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

}
