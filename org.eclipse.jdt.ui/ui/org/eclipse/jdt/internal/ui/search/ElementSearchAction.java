/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;import java.lang.reflect.InvocationTargetException;import java.util.ResourceBundle;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.search.ui.SearchUI;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
 
/**
 * Abstract class for all search actions which work on java elements.
 */
public abstract class ElementSearchAction extends JavaElementAction {

	public ElementSearchAction(String label, Class[] validTypes) {
		super(label, validTypes);
	}

	public void run(IJavaElement element) {
		SearchUI.activateSearchResultView();
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		JavaSearchOperation op= null;
		ResourceBundle bundle= JavaPlugin.getResourceBundle();
		try {
			op= makeOperation(element);
		} catch (JavaModelException ex) {
			ExceptionHandler.handle(ex, shell, bundle, "Search.Error.search.");
			return;
		}
		IWorkspaceDescription workspaceDesc= JavaPlugin.getWorkspace().getDescription();
		boolean isAutoBuilding= workspaceDesc.isAutoBuilding();
		if (isAutoBuilding) {
			// disable auto-build during search operation
			workspaceDesc.setAutoBuilding(false);
			try {
				JavaPlugin.getWorkspace().setDescription(workspaceDesc);
			}
			catch (CoreException ex) {
				ExceptionHandler.handle(ex, shell, bundle, "Search.Error.setDescription.");
			}
		}
		try {
			new ProgressMonitorDialog(shell).run(true, true, op);
		} catch (InvocationTargetException ex) {
			ExceptionHandler.handle(ex, shell, bundle, "Search.Error.search.");
		} catch(InterruptedException e) {
		} finally {
			if (isAutoBuilding) {
				// enable auto-building again
				workspaceDesc= JavaPlugin.getWorkspace().getDescription();
				workspaceDesc.setAutoBuilding(true);
				try {
					JavaPlugin.getWorkspace().setDescription(workspaceDesc);
				}
				catch (CoreException ex) {
					ExceptionHandler.handle(ex, shell, bundle, "Search.Error.setDescription.");
				}
			}				
		}
	}

	protected JavaSearchOperation makeOperation(IJavaElement element) throws JavaModelException {
		return new JavaSearchOperation(JavaPlugin.getWorkspace(), element, getLimitTo(), getScope(), getCollector());
	};

	protected abstract int getLimitTo();

	protected IJavaSearchScope getScope() {
		return SearchEngine.createWorkspaceScope();
	}

	protected JavaSearchResultCollector getCollector() {
		return new JavaSearchResultCollector();
	}
}
