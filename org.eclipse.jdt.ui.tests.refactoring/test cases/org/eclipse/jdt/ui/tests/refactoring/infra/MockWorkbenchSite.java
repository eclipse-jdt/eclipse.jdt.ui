package org.eclipse.jdt.ui.tests.refactoring.infra;

import java.util.List;

import org.eclipse.core.runtime.PlatformObject;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;

import org.eclipse.jdt.internal.corext.Assert;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class MockWorkbenchSite extends PlatformObject implements IWorkbenchSite {
	
	private ISelectionProvider fProvider;
	
	public MockWorkbenchSite(ISelectionProvider provider){
		setSelectionProvider(provider);
	}
	
	public MockWorkbenchSite(Object[] elements){
		this(new SimpleSelectionProvider(elements));
	}
	
	public MockWorkbenchSite(List elements){
		this(new SimpleSelectionProvider(elements));
	}
	
	public IWorkbenchPage getPage() {
		return null;
	}

	public ISelectionProvider getSelectionProvider() {
		return fProvider;
	}

	public Shell getShell() {
		return JavaPlugin.getActiveWorkbenchShell();
	}

	public IWorkbenchWindow getWorkbenchWindow() {
		return null;
	}
	
	public void setSelectionProvider(ISelectionProvider provider) {
		Assert.isNotNull(provider);
		fProvider= provider;
	}
}
