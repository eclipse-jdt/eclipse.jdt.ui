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
package org.eclipse.jdt.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.SearchUI;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.preferences.WorkInProgressPreferencePage;
import org.eclipse.jdt.internal.ui.search.ElementQuerySpecification;
import org.eclipse.jdt.internal.ui.search.JavaSearchDescription;
import org.eclipse.jdt.internal.ui.search.JavaSearchOperation;
import org.eclipse.jdt.internal.ui.search.JavaSearchQuery;
import org.eclipse.jdt.internal.ui.search.JavaSearchResultCollector;
import org.eclipse.jdt.internal.ui.search.SearchMessages;
import org.eclipse.jdt.internal.ui.search.SearchUtil;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Abstract class for Java search actions.
 * <p>
 * Note: This class is for internal use only. Clients should not use this class.
 * </p>
 * 
 * @since 2.0
 */
public abstract class FindAction extends SelectionDispatchAction {

	// A dummy which can't be selected in the UI
	private static final IJavaElement RETURN_WITHOUT_BEEP= JavaCore.create(JavaPlugin.getWorkspace().getRoot());
		
	private Class[] fValidTypes;
	private JavaEditor fEditor;	


	FindAction(IWorkbenchSite site, String label, Class[] validTypes) {
		super(site);
		setText(label);
		fValidTypes= validTypes;
	}

	FindAction(JavaEditor editor, String label, Class[] validTypes) {
		this (editor.getEditorSite(), label, validTypes);
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}

	private boolean canOperateOn(IStructuredSelection sel) {
		return sel != null && !sel.isEmpty() && canOperateOn(getJavaElement(sel, true));
	}
		
	boolean canOperateOn(IJavaElement element) {
		if (fValidTypes == null || fValidTypes.length == 0)
			return false;

		if (element != null) {
			for (int i= 0; i < fValidTypes.length; i++) {
				if (fValidTypes[i].isInstance(element)) {
					if (element.getElementType() == IJavaElement.PACKAGE_FRAGMENT)
						return hasChildren((IPackageFragment)element);
					else
						return true;
				}
			}
		}
		return false;
	}
	
	private boolean hasChildren(IPackageFragment packageFragment) {
		try {
			return packageFragment.hasChildren();
		} catch (JavaModelException ex) {
			return false;
		}
	}

	private IJavaElement getJavaElement(IJavaElement o, boolean silent) {
		if (o == null)
			return null;
		switch (o.getElementType()) {
			case IJavaElement.COMPILATION_UNIT:
				if (silent)
					return (ICompilationUnit)o;
				else
					return findType((ICompilationUnit)o, silent);
			case IJavaElement.CLASS_FILE:
				return findType((IClassFile)o);
			default:
				return o;				
		}
	}

	private IJavaElement getJavaElement(IMarker marker, boolean silent) {
		return getJavaElement(SearchUtil.getJavaElement(marker), silent);
	}


	private IJavaElement getJavaElement(Object o, boolean silent) {
		if (o instanceof IJavaElement)
			return getJavaElement((IJavaElement)o, silent);
		else if (o instanceof IMarker)
			return getJavaElement((IMarker)o, silent);
		else if (o instanceof ISelection)
			return getJavaElement((IStructuredSelection)o, silent);
		else if (SearchUtil.isISearchResultViewEntry(o))
			return getJavaElement(SearchUtil.getJavaElement(o), silent);
		return null;
	}

	IJavaElement getJavaElement(IStructuredSelection selection, boolean silent) {
		if (selection.size() == 1)
			// Selection only enabled if one element selected.
			return getJavaElement(selection.getFirstElement(), silent);
		return null;
	}

	private void showOperationUnavailableDialog() {
		MessageDialog.openInformation(getShell(), SearchMessages.getString("JavaElementAction.operationUnavailable.title"), getOperationUnavailableMessage()); //$NON-NLS-1$
	}	

	String getOperationUnavailableMessage() {
		return SearchMessages.getString("JavaElementAction.operationUnavailable.generic"); //$NON-NLS-1$
	}

	private IJavaElement findType(ICompilationUnit cu, boolean silent) {
		IType[] types= null;
		try {					
			types= cu.getAllTypes();
		} catch (JavaModelException ex) {
			// silent mode
			ExceptionHandler.log(ex, SearchMessages.getString("JavaElementAction.error.open.message")); //$NON-NLS-1$
			if (silent)
				return RETURN_WITHOUT_BEEP;
			else
				return null;
		}
		if (types.length == 1 || (silent && types.length > 0))
			return types[0];
		if (silent)
			return RETURN_WITHOUT_BEEP;
		if (types.length == 0)
			return null;
		String title= SearchMessages.getString("JavaElementAction.typeSelectionDialog.title"); //$NON-NLS-1$
		String message = SearchMessages.getString("JavaElementAction.typeSelectionDialog.message"); //$NON-NLS-1$
		int flags= (JavaElementLabelProvider.SHOW_DEFAULT);						

		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), new JavaElementLabelProvider(flags));
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.setElements(types);
		
		if (dialog.open() == Window.OK)
			return (IType)dialog.getFirstResult();
		else
			return RETURN_WITHOUT_BEEP;
	}

	private IType findType(IClassFile cf) {
		IType mainType;
		try {					
			mainType= cf.getType();
		} catch (JavaModelException ex) {
			ExceptionHandler.log(ex, SearchMessages.getString("JavaElementAction.error.open.message")); //$NON-NLS-1$
			return null;
		}
		return mainType;
	}
	
	/* 
	 * Method declared on SelectionChangedAction.
	 */
	public void run(IStructuredSelection selection) {
		IJavaElement element= getJavaElement(selection, false);
		if (element == null || !element.exists()) {
			showOperationUnavailableDialog();
			return;
		} 
		else if (element == RETURN_WITHOUT_BEEP)
			return;
		
		run(element);
	}

	/* 
	 * Method declared on SelectionChangedAction.
	 */
	public void run(ITextSelection selection) {
		if (!ActionUtil.isProcessable(getShell(), fEditor))
			return;
		try {
			String title= SearchMessages.getString("SearchElementSelectionDialog.title"); //$NON-NLS-1$
			String message= SearchMessages.getString("SearchElementSelectionDialog.message"); //$NON-NLS-1$
			
			IJavaElement[] elements= SelectionConverter.codeResolve(fEditor);
			if (elements.length > 0 && canOperateOn(elements[0])) {
					IJavaElement element= elements[0];
					if (elements.length > 1)
						element= SelectionConverter.codeResolve(fEditor, getShell(), title, message);
					if (element != null)
						run(element);
			}
			else
				showOperationUnavailableDialog();
		} catch (JavaModelException ex) {
			JavaPlugin.log(ex);
			String title= SearchMessages.getString("Search.Error.search.title"); //$NON-NLS-1$
			String message= SearchMessages.getString("Search.Error.codeResolve"); //$NON-NLS-1$
			ErrorDialog.openError(getShell(), title, message, ex.getStatus());
		}
	}

	/* 
	 * Method declared on SelectionChangedAction.
	 */
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(canOperateOn(selection));
	}

	/* 
	 * Method declared on SelectionChangedAction.
	 */
	public void selectionChanged(ITextSelection selection) {
	}

	public void run(IJavaElement element) {
		
		if (!ActionUtil.isProcessable(getShell(), element))
			return;
		
		if (JavaPlugin.getDefault().getPreferenceStore().getBoolean(WorkInProgressPreferencePage.PREF_BGSEARCH)) {
			try {
				performNewSearch(element);
			} catch (JavaModelException ex) {
				ExceptionHandler.handle(ex, getShell(), SearchMessages.getString("Search.Error.search.title"), SearchMessages.getString("Search.Error.search.message")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} else {
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
				// means it's cancelled
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
	}

	private void performNewSearch(IJavaElement element) throws JavaModelException {
		JavaSearchQuery query= createJob(element);
		if (query != null) {
			NewSearchUI.activateSearchResultView();
			NewSearchUI.runQuery(query);
		}
	}

	protected JavaSearchQuery createJob(IJavaElement element) throws JavaModelException {
		return new JavaSearchQuery(new ElementQuerySpecification(element, getLimitTo(), getScope(element), getScopeDescription(element)));
	}

	protected Object createSearchDescription(IJavaElement element) {
		IType type= getType(element);
		return new JavaSearchDescription(getLimitTo(), element, null, getScopeDescription(type));
	}

	JavaSearchOperation makeOperation(IJavaElement element) throws JavaModelException {
		return new JavaSearchOperation(JavaPlugin.getWorkspace(), element, getLimitTo(), getScope(element), getScopeDescription(element), getCollector());
	}

	abstract int getLimitTo();


	JavaSearchResultCollector getCollector() {
		return new JavaSearchResultCollector();
	}
	
	String getScopeDescription(IJavaElement element) {
		return SearchMessages.getString("WorkspaceScope"); //$NON-NLS-1$
	}

	IJavaSearchScope getScope(IJavaElement element) throws JavaModelException {
		return SearchEngine.createWorkspaceScope();
	}

	IType getType(IJavaElement element) {
		if (element == null)
			return null;
		
		IType type= null;
		if (element.getElementType() == IJavaElement.TYPE)
			type= (IType)element;
		else if (element instanceof IMember)
			type= ((IMember)element).getDeclaringType();
		else if (element instanceof ILocalVariable) {
			type= (IType)element.getAncestor(IJavaElement.TYPE);
		}
		if (type != null) {
			ICompilationUnit cu= type.getCompilationUnit();
			if (cu == null)
				return type;
				
			IType wcType= (IType) getWorkingCopy(type);
			if (wcType != null)
				return wcType;
			else
				return type;
		}
		return null;
	}
	
	/**
	 * Tries to find the given element in a working copy.
	 */
	private IJavaElement getWorkingCopy(IJavaElement input) {
		// TODO: With new working copy story: original == working copy.
		// Note that the previous code could result in a reconcile as side effect. Should check if that
		// is still required.
		return input;
	}
}
