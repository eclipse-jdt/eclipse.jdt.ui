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

import java.util.Iterator;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaStatusConstants;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.OpenActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.packageview.PackagesMessages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * This action opens a java editor on an element represented by either
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
public class OpenAction extends SelectionDispatchAction {
	
	private JavaEditor fEditor;
	
	/**
	 * Creates a new <code>OpenAction</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public OpenAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.getString("OpenAction.label")); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("OpenAction.tooltip")); //$NON-NLS-1$
		setDescription(ActionMessages.getString("OpenAction.description")); //$NON-NLS-1$		
	}
	
	/**
	 * Creates a new <code>OpenAction</code>.
	 * <p>
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * </p>
	 */
	public OpenAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setText(ActionMessages.getString("OpenAction.declaration.label")); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	protected void selectionChanged(ITextSelection selection) {
		setEnabled(fEditor != null);
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		setEnabled(checkEnabled(selection));
	}
	
	private boolean checkEnabled(IStructuredSelection selection) {
		if (selection.isEmpty())
			return false;
		for (Iterator iter= selection.iterator(); iter.hasNext();) {
			Object element= (Object)iter.next();
			if (element instanceof IImportDeclaration && !((IImportDeclaration)element).isOnDemand())
				continue;
			if (element instanceof ISourceReference)
				continue;
			if (element instanceof IResource)
				continue;
			if (element instanceof IStorage)
				continue;
			return false;
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	protected void run(ITextSelection selection) {
		try {
			run(SelectionConverter.codeResolveOrInput(fEditor));
		} catch (JavaModelException e) {
			showError(e);
		}
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	protected void run(IStructuredSelection selection) {
		if (!checkEnabled(selection))
			return;
		run(selection.toArray());
	}
	
	private void run(Object[] elements) {
		if (elements == null)
			return;
		for (int i= 0; i < elements.length; i++) {
			Object element= elements[i];
			try {
				OpenActionUtil.open(element);
			} catch (JavaModelException e) {
				JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(),
					JavaStatusConstants.INTERNAL_ERROR, ActionMessages.getString("OpenAction.error.message"), e)); //$NON-NLS-1$
				
				ErrorDialog.openError(getShell(), 
					getDialogTitle(),
					ActionMessages.getString("OpenAction.error.messageProblems"),  //$NON-NLS-1$
					e.getStatus());
			
			} catch (PartInitException x) {
								
				String name= null;
				
				if (element instanceof IJavaElement) {
					name= ((IJavaElement) element).getElementName();
				} else if (element instanceof IStorage) {
					name= ((IStorage) element).getName();
				} else if (element instanceof IResource) {
					name= ((IResource) element).getName();
				}
				
				if (name != null) {
					MessageDialog.openError(getShell(),
						ActionMessages.getString("OpenAction.error.messageProblems"),  //$NON-NLS-1$
						ActionMessages.getFormattedString("OpenAction.error.messageArgs",  //$NON-NLS-1$
							new String[] { name, x.getMessage() } ));			
				}
			}		
		}
	}
	
	private String getDialogTitle() {
		return ActionMessages.getString("OpenAction.error.title"); //$NON-NLS-1$
	}
	
	private void showError(CoreException e) {
		ExceptionHandler.handle(e, getShell(), getDialogTitle(), ActionMessages.getString("OpenAction.error.message")); //$NON-NLS-1$
	}
}