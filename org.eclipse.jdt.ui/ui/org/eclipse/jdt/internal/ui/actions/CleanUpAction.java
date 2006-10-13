/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringExecutionStarter;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.browsing.LogicalPackage;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.util.ElementValidator;

public class CleanUpAction extends SelectionDispatchAction {

	private JavaEditor fEditor;
	private IPreferenceChangeListener fPreferenceChangeListener;

	public CleanUpAction(IWorkbenchSite site) {
		super(site); 
		setToolTipText(ActionMessages.CleanUpAction_tooltip); 
		setDescription(ActionMessages.CleanUpAction_description);
		
		fPreferenceChangeListener= new IPreferenceChangeListener() {
			public void preferenceChange(PreferenceChangeEvent event) {
				if (event.getKey().equals(CleanUpConstants.SHOW_CLEAN_UP_WIZARD)) {
					updateActionLabel();
				}
			}
		};
		new InstanceScope().getNode(JavaUI.ID_PLUGIN).addPreferenceChangeListener(fPreferenceChangeListener);

		updateActionLabel();

//		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.ORGANIZE_IMPORTS_ACTION);
	}
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the Java editor
	 */
	public CleanUpAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(getCompilationUnit(fEditor) != null);
	}
		
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(ITextSelection selection) {
		ICompilationUnit cu= getCompilationUnit(fEditor);
		if (cu != null) {
			run(cu);
		}
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(IStructuredSelection selection) {
		ICompilationUnit[] cus= getCompilationUnits(selection);
		if (cus.length == 0) {
			MessageDialog.openInformation(getShell(), ActionMessages.CleanUpAction_EmptySelection_title, ActionMessages.CleanUpAction_EmptySelection_description);
		} else if (cus.length == 1) {
			run(cus[0]);
		} else {
			runOnMultiple(cus);
		}
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void selectionChanged(ITextSelection selection) {
		setEnabled(getCompilationUnit(fEditor) != null);
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(isEnabled(selection));
	}
	
	private boolean isEnabled(IStructuredSelection selection) {
		Object[] selected= selection.toArray();
		for (int i= 0; i < selected.length; i++) {
			try {
				if (selected[i] instanceof IJavaElement) {
					IJavaElement elem= (IJavaElement) selected[i];
					if (elem.exists()) {
						switch (elem.getElementType()) {
							case IJavaElement.TYPE:
								return elem.getParent().getElementType() == IJavaElement.COMPILATION_UNIT; // for browsing perspective
							case IJavaElement.COMPILATION_UNIT:
								return true;
							case IJavaElement.IMPORT_CONTAINER:
								return true;
							case IJavaElement.PACKAGE_FRAGMENT:
							case IJavaElement.PACKAGE_FRAGMENT_ROOT:
								IPackageFragmentRoot root= (IPackageFragmentRoot) elem.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
								return (root.getKind() == IPackageFragmentRoot.K_SOURCE);
							case IJavaElement.JAVA_PROJECT:
								// https://bugs.eclipse.org/bugs/show_bug.cgi?id=65638
								return true;
						}
					}
				} else if (selected[i] instanceof LogicalPackage) {
					return true;
				}
			} catch (JavaModelException e) {
				if (!e.isDoesNotExist()) {
					JavaPlugin.log(e);
				}
			}
		}
		return false;
	}
	
	/**
	 * Note: This method is for internal use only. Clients should not call this method.
	 * @param cu The compilation unit to process
	 */
	public void run(ICompilationUnit cu) {
		if (!ElementValidator.check(cu, getShell(), ActionMessages.CleanUpAction_ErrorDialogTitle, fEditor != null)) 
			return;
		if (!ActionUtil.isProcessable(getShell(), cu))
			return;
		
		try {
			RefactoringExecutionStarter.startCleanupRefactoring(new ICompilationUnit[] {cu}, showWizard(), getShell());
		} catch (InvocationTargetException e) {
			JavaPlugin.log(e);
			if (e.getCause() instanceof CoreException)
				showUnexpectedError((CoreException)e.getCause());
		} catch (JavaModelException e) {
			showUnexpectedError(e);
        }
		return;
	}

	/**
	 * Perform on multiple compilation units. No editors are opened.
	 * @param cus The compilation units to run on
	 */
	public void runOnMultiple(final ICompilationUnit[] cus) {
		String message= ActionMessages.CleanUpAction_MultiStateErrorTitle; 
		final MultiStatus status= new MultiStatus(JavaUI.ID_PLUGIN, IStatus.OK, message, null);
		
		for (int i= 0; i < cus.length; i++) {
			testOnBuildPath(cus[i], status);
		}
		
		if (!status.isOK()) {
			String title= ActionMessages.CleanUpAction_ErrorDialogTitle; 
			ErrorDialog.openError(getShell(), title, null, status);
			return;
		}
			
		try {
			RefactoringExecutionStarter.startCleanupRefactoring(cus, showWizard(), getShell());
		} catch (InvocationTargetException e) {
			JavaPlugin.log(e);
			if (e.getCause() instanceof CoreException)
				showUnexpectedError((CoreException)e.getCause());
		} catch (JavaModelException e) {
			showUnexpectedError(e);
		}
		return;
	}

	private void showUnexpectedError(CoreException e) {
		String message2= Messages.format(ActionMessages.CleanUpAction_UnexpectedErrorMessage, e.getStatus().getMessage()); 
		IStatus status= new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR, message2, null);
		String title= ActionMessages.CleanUpAction_ErrorDialogTitle; 
		ErrorDialog.openError(getShell(), title, null, status);
	}
	
	private boolean testOnBuildPath(ICompilationUnit cu, MultiStatus status) {
		IJavaProject project= cu.getJavaProject();
		if (!project.isOnClasspath(cu)) {
			String cuLocation= cu.getPath().makeRelative().toString();
			String message= Messages.format(ActionMessages.CleanUpAction_CUNotOnBuildpathMessage, cuLocation); 
			status.add(new Status(IStatus.INFO, JavaUI.ID_PLUGIN, IStatus.ERROR, message, null));
			return false;
		}
		return true;
	}
	
	private ICompilationUnit[] getCompilationUnits(IStructuredSelection selection) {
		HashSet result= new HashSet();
		Object[] selected= selection.toArray();
		for (int i= 0; i < selected.length; i++) {
			try {
				if (selected[i] instanceof IJavaElement) {
					IJavaElement elem= (IJavaElement) selected[i];
					if (elem.exists()) {
					
						switch (elem.getElementType()) {
							case IJavaElement.TYPE:
								if (elem.getParent().getElementType() == IJavaElement.COMPILATION_UNIT) {
									result.add(elem.getParent());
								}
								break;						
							case IJavaElement.COMPILATION_UNIT:
								result.add(elem);
								break;
							case IJavaElement.IMPORT_CONTAINER:
								result.add(elem.getParent());
								break;							
							case IJavaElement.PACKAGE_FRAGMENT:
								collectCompilationUnits((IPackageFragment) elem, result);
								break;
							case IJavaElement.PACKAGE_FRAGMENT_ROOT:
								collectCompilationUnits((IPackageFragmentRoot) elem, result);
								break;
							case IJavaElement.JAVA_PROJECT:
								IPackageFragmentRoot[] roots= ((IJavaProject) elem).getPackageFragmentRoots();
								for (int k= 0; k < roots.length; k++) {
									collectCompilationUnits(roots[k], result);
								}
								break;			
						}
					}
				} else if (selected[i] instanceof LogicalPackage) {
					IPackageFragment[] packageFragments= ((LogicalPackage)selected[i]).getFragments();
					for (int k= 0; k < packageFragments.length; k++) {
						IPackageFragment pack= packageFragments[k];
						if (pack.exists()) {
							collectCompilationUnits(pack, result);
						}
					}
				}
			} catch (JavaModelException e) {
				if (JavaModelUtil.isExceptionToBeLogged(e))
					JavaPlugin.log(e);
			}
		}
		return (ICompilationUnit[]) result.toArray(new ICompilationUnit[result.size()]);
	}
	
	private void collectCompilationUnits(IPackageFragment pack, Collection result) throws JavaModelException {
		result.addAll(Arrays.asList(pack.getCompilationUnits()));
	}
	
	private void collectCompilationUnits(IPackageFragmentRoot root, Collection result) throws JavaModelException {
		if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
			IJavaElement[] children= root.getChildren();
			for (int i= 0; i < children.length; i++) {
				collectCompilationUnits((IPackageFragment) children[i], result);
			}
		}
	}	
	
	private static ICompilationUnit getCompilationUnit(JavaEditor editor) {
		IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
		ICompilationUnit cu= manager.getWorkingCopy(editor.getEditorInput());
		return cu;
	}

	private boolean showWizard() {
		InstanceScope instanceScope= new InstanceScope();
		IEclipsePreferences instanceNode= instanceScope.getNode(JavaUI.ID_PLUGIN);
		if (instanceNode.get(CleanUpConstants.SHOW_CLEAN_UP_WIZARD, null) != null)
			return instanceNode.getBoolean(CleanUpConstants.SHOW_CLEAN_UP_WIZARD, true);
		
		DefaultScope defaultScope= new DefaultScope();
		IEclipsePreferences defaultNode= defaultScope.getNode(JavaUI.ID_PLUGIN);
		return defaultNode.getBoolean(CleanUpConstants.SHOW_CLEAN_UP_WIZARD, true);
    }

	private void updateActionLabel() {
	    if (showWizard()) {
			setText(ActionMessages.CleanUpAction_labelWizard);
		} else {
			setText(ActionMessages.CleanUpAction_label);
		}
    }

	public void dispose() {
		new InstanceScope().getNode(JavaUI.ID_PLUGIN).removePreferenceChangeListener(fPreferenceChangeListener);
		fPreferenceChangeListener= null;
    }
}
