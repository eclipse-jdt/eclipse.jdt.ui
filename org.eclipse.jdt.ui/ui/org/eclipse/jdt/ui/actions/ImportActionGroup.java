/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui.actions;

import java.util.Iterator;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.ExportResourcesAction;
import org.eclipse.ui.actions.ImportResourcesAction;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.ui.IContextMenuConstants;

/**
 * Action group to add the Import and Export action to a view part's
 * context menu.
 * 
 * @sine 2.0
 */
public class ImportActionGroup extends ActionGroup {

	private static final String GROUP_IMPORT= "group.import"; //$NON-NLS-1$
	
	private static final int FAILED= 1 << 0;
	private static final int IMPORT= 1 << 1;
	private static final int EXPORT= 1 << 2;
	private static final int IMPORT_EXPORT= IMPORT | EXPORT;

	private ImportResourcesAction fImportAction;
	private ExportResourcesAction fExportAction;

	/**
	 * Creates a new <code>ImportActionGroup</code>.
	 * 
	 * @param part the view part that owns this action group
	 */
	public ImportActionGroup(IViewPart part) {
		IWorkbench workbench = part.getSite().getWorkbenchWindow().getWorkbench();
		fImportAction= new ImportResourcesAction(workbench);
		fExportAction= new ExportResourcesAction(workbench);			
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillContextMenu(IMenuManager menu) {
		IStructuredSelection selection= getStructuredSelection();
		if (selection == null)
			return;
			
		int mode= checkSelection(selection);
		if ((mode & FAILED) == 0) {
			menu.appendToGroup(IContextMenuConstants.GROUP_REORGANIZE, new Separator(GROUP_IMPORT));
			if ((mode  & IMPORT) != 0)
				menu.appendToGroup(GROUP_IMPORT, fImportAction);
			if ((mode & EXPORT) != 0)
				menu.appendToGroup(GROUP_IMPORT, fExportAction);
		}
		super.fillContextMenu(menu);
	}
	
	private void appendToGroup(IMenuManager menu, IAction action) {
		if (action.isEnabled())
			menu.appendToGroup(GROUP_IMPORT, action);
	}
	
	private IStructuredSelection getStructuredSelection() {
		ISelection selection= getContext().getSelection();
		if (selection instanceof IStructuredSelection)
			return (IStructuredSelection)selection;
		return null;
	}
	
	private int checkSelection(IStructuredSelection selection) {
		int result= 0;
		for (Iterator iter= selection.iterator(); iter.hasNext();) {
			Object element= (Object) iter.next();
			if (element instanceof IJavaElement) {
				int type= ((IJavaElement)element).getElementType();
				switch (type) {
					case IJavaElement.JAVA_PROJECT:
					case IJavaElement.PACKAGE_FRAGMENT_ROOT:
					case IJavaElement.PACKAGE_FRAGMENT:
						result= result | IMPORT_EXPORT;
						break;
					case IJavaElement.COMPILATION_UNIT:
						result= result | EXPORT;
						break;
					default:
						return FAILED;
				}
			} else if (element instanceof IContainer) {
				result= result | IMPORT_EXPORT;
			} else if (element instanceof IFile) {
				result= result | EXPORT;
			} else {
				return FAILED;
			}
		}
		return result;
	}
}
