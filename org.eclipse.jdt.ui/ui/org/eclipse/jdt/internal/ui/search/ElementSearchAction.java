/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;

import org.eclipse.search.ui.SearchUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
 
/**
 * Abstract class for all search actions which work on java elements.
 */
public abstract class ElementSearchAction extends JavaElementAction {

	// LRU working sets
	static int LRU_WORKINGSET_LIST_SIZE= 3;
	private static LRUWorkingSetList fgLRUWorkingSets;
		
	// Settings store
	private static final String DIALOG_SETTINGS_KEY= "JavaElementSearchActions"; //$NON-NLS-1$
	private static final String STORE_LRU_WORKING_SET_NAMES= "lastUsedWorkingSetNames"; //$NON-NLS-1$
	private static IDialogSettings fgSettingsStore;

	public ElementSearchAction(String label, Class[] validTypes) {
		super(label, validTypes);
	}

	protected void run(IJavaElement element) {
		SearchUI.activateSearchResultView();
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		JavaSearchOperation op= null;
		try {
			op= makeOperation(element);
			if (op == null)
				return;
		} catch (JavaModelException ex) {
			ExceptionHandler.handle(ex, shell, SearchMessages.getString("Search.Error.search.title"), SearchMessages.getString("Search.Error.search.message")); //$NON-NLS-1$ //$NON-NLS-2$
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
			ExceptionHandler.handle(ex, shell, SearchMessages.getString("Search.Error.setDescription.title"), SearchMessages.getString("Search.Error.setDescription.message")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		try {
			new ProgressMonitorDialog(shell).run(true, true, op);
		} catch (InvocationTargetException ex) {
			ExceptionHandler.handle(ex, shell, SearchMessages.getString("Search.Error.search.title"), SearchMessages.getString("Search.Error.search.message")); //$NON-NLS-1$ //$NON-NLS-2$
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
					ExceptionHandler.handle(ex, shell, SearchMessages.getString("Search.Error.setDescription.title"), SearchMessages.getString("Search.Error.setDescription.message")); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}				
		}
	}

	protected JavaSearchOperation makeOperation(IJavaElement element) throws JavaModelException {
		IType type= getType(element);
		return new JavaSearchOperation(JavaPlugin.getWorkspace(), element, getLimitTo(), getScope(type), getScopeDescription(type), getCollector());
	};

	protected abstract int getLimitTo();

	protected IJavaSearchScope getScope(IType element) throws JavaModelException {
		return SearchEngine.createWorkspaceScope();
	}

	protected JavaSearchResultCollector getCollector() {
		return new JavaSearchResultCollector();
	}
	
	protected String getScopeDescription(IType type) {
		return SearchMessages.getString("WorkspaceScope"); //$NON-NLS-1$
	}

	protected IType getType(IJavaElement element) {
		IType type= null;
		if (element.getElementType() == IJavaElement.TYPE)
			type= (IType)element;
		else if (element instanceof IMember)
			type= ((IMember)element).getDeclaringType();
		if (type != null) {
			ICompilationUnit cu= type.getCompilationUnit();
			if (cu == null)
				return type;
				
			IType wcType= (IType)getWorkingCopy(type);
			if (wcType != null)
				return wcType;
			else
				return type;
		}
		return null;
	}
	
	/**
	 * Tries to find the given element in a workingcopy.
	 */
	protected static IJavaElement getWorkingCopy(IJavaElement input) {
		try {
			if (input instanceof ICompilationUnit)
				return EditorUtility.getWorkingCopy((ICompilationUnit)input);
			else
				return EditorUtility.getWorkingCopy(input, true);
		} catch (JavaModelException ex) {
		}
		return null;
	}
	
	public static void updateLRUWorkingSet(IWorkingSet workingSet) {
		getLRUWorkingSets().add(workingSet);

		// Store LRU working sets
		String[] lruWorkingSetNames= new String[LRU_WORKINGSET_LIST_SIZE];
		Iterator iter= fgLRUWorkingSets.iterator();
		int i= 0;
		while (iter.hasNext())
			lruWorkingSetNames[i++]= ((IWorkingSet)iter.next()).getName();
		fgSettingsStore.put(STORE_LRU_WORKING_SET_NAMES, lruWorkingSetNames);
	}

	static LRUWorkingSetList getLRUWorkingSets() {
		if (fgLRUWorkingSets == null) {
			// Read LRU working sets from store
			fgLRUWorkingSets= new LRUWorkingSetList(LRU_WORKINGSET_LIST_SIZE);
			fgSettingsStore= JavaPlugin.getDefault().getDialogSettings().getSection(DIALOG_SETTINGS_KEY);
			if (fgSettingsStore == null)
				fgSettingsStore= JavaPlugin.getDefault().getDialogSettings().addNewSection(DIALOG_SETTINGS_KEY);
			String[] lruWorkingSetNames= fgSettingsStore.getArray(STORE_LRU_WORKING_SET_NAMES);
			if (lruWorkingSetNames != null) {
				for (int i= lruWorkingSetNames.length - 1; i >= 0; i--) {
					IWorkingSet workingSet= PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(lruWorkingSetNames[i]);
					if (workingSet != null)
						fgLRUWorkingSets.add(workingSet);
				}
			}
		}
		return fgLRUWorkingSets;
	}
}
