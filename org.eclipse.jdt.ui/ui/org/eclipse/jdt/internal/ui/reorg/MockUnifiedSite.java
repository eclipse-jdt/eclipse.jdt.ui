package org.eclipse.jdt.internal.ui.reorg;

import java.util.List;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

import org.eclipse.jdt.ui.actions.UnifiedSite;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.ui.JavaPlugin;

public class MockUnifiedSite extends UnifiedSite {
	
	private ISelectionProvider fProvider;
	
	public MockUnifiedSite(ISelectionProvider provider){
		Assert.isNotNull(provider);
		fProvider= provider;		
	}
	
	public MockUnifiedSite(Object[] elements){
		this(new SimpleSelectionProvider(elements));
	}
	
	public MockUnifiedSite(List elements){
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

}
