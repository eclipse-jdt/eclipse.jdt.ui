/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui.actions;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.core.resources.IResource;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.OpenActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jdt.internal.ui.preferences.AppearancePreferencePage;
import org.eclipse.jdt.internal.ui.preferences.JavaBasePreferencePage;
/**
 * This action reveals the currently selected Java element in the packages
 * view. The Java element can be represeented by either
 * <ul>
 * 	<li>a text selection inside a Java editor, or </li>
 * 	<li>a structured selection of a view part showing Java elements</li>
 * </ul>
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class ShowInPackageViewAction extends SelectionDispatchAction {
	
	private JavaEditor fEditor;
	
	/**
	 * Creates a new <code>ShowInPackageViewAction</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public ShowInPackageViewAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.getString("ShowInPackageViewAction.label")); //$NON-NLS-1$
		setDescription(ActionMessages.getString("ShowInPackageViewAction.description")); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("ShowInPackageViewAction.tooltip")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.SHOW_IN_PACKAGEVIEW_ACTION);	
	}
	
	/**
	 * Creates a new <code>ShowInPackageViewAction</code>.
	 * <p>
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * </p>
	 */
	public ShowInPackageViewAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	protected void selectionChanged(ITextSelection selection) {
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		setEnabled(checkEnabled(selection));
	}
	
	private boolean checkEnabled(IStructuredSelection selection) {
		if (selection.size() != 1)
			return false;
		return selection.getFirstElement() instanceof IJavaElement;
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	protected void run(ITextSelection selection) {
		try {
			IJavaElement element= SelectionConverter.getElementAtOffset(fEditor);
			if (element != null)
				run(element);
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			String message= ActionMessages.getString("ShowInPackageViewAction.error.message"); //$NON-NLS-1$
			ErrorDialog.openError(getShell(), getDialogTitle(), message, e.getStatus());
		}	
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	protected void run(IStructuredSelection selection) {
		if (!checkEnabled(selection))
			return;
		run((IJavaElement)selection.getFirstElement());
	}
	
	private void run(IJavaElement element) {
		if (element == null)
			return;
		boolean showMembers= JavaPlugin.getDefault().getPreferenceStore().getBoolean(AppearancePreferencePage.SHOW_CU_CHILDREN);
		PackageExplorerPart view= PackageExplorerPart.openInActivePerspective();
		if (view != null) {
			if (reveal(view, element))
				return;
			if (showMembers && element instanceof IMember) {
				// Since the packages view shows working copy elements it can happen that we try to show an original element
				// in the packages view. Fall back to working copy element
				IMember member= (IMember)element;
				// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=20047
				if (!isWorkingCopyElement(member)) {
					try {
						element= EditorUtility.getWorkingCopy(member);
						if (reveal(view, element))
							return;
					} catch (JavaModelException e) {
					}
				}
			}
			element= getVisibleParent(element);
			if (element != null) {
				if (reveal(view, element))
					return;
				IResource resource= null;
				try {
					resource= element.getCorrespondingResource();
				} catch (JavaModelException e) {
				}
				if (resource != null) {
					if (reveal(view, resource))
						return;
				}
			}
			MessageDialog.openInformation(getShell(), getDialogTitle(), ActionMessages.getString("ShowInPackageViewAction.not_found")); //$NON-NLS-1$
		}
	}

	private boolean reveal(PackageExplorerPart view, Object element) {
		// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=19104
		if (element == null)
			return false;
		view.selectReveal(new StructuredSelection(element));
		if (element.equals(getSelectedElement(view)))
			return true;
		return false;
	}

	private Object getSelectedElement(PackageExplorerPart view) {
		return ((IStructuredSelection)view.getSite().getSelectionProvider().getSelection()).getFirstElement();
	}
	
	private IJavaElement getVisibleParent(IJavaElement element) {
		// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=19104
		if (element == null)
			return null;
		switch (element.getElementType()) {
			case IJavaElement.IMPORT_DECLARATION:
			case IJavaElement.PACKAGE_DECLARATION:
			case IJavaElement.IMPORT_CONTAINER:
			case IJavaElement.TYPE:
			case IJavaElement.METHOD:
			case IJavaElement.FIELD:
			case IJavaElement.INITIALIZER:
				// select parent cu/classfile
				element= (IJavaElement)element.getOpenable();
				break;
			case IJavaElement.JAVA_MODEL:
				element= null;
				break;
		}
		if (element.getElementType() == IJavaElement.COMPILATION_UNIT) {
			ICompilationUnit unit= (ICompilationUnit)element;
			if (unit.isWorkingCopy())
				element= unit.getOriginalElement();
		}
		return element;
	}
	
	/*
	 * Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=20047
	 */
	private boolean isWorkingCopyElement(IMember member) {
		ICompilationUnit unit= member.getCompilationUnit();
		if (unit == null)
			return false;
		return unit.isWorkingCopy();
	}

	private static String getDialogTitle() {
		return ActionMessages.getString("ShowInPackageViewAction.dialog.title"); //$NON-NLS-1$
	}
}