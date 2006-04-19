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
package org.eclipse.jdt.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IEditorStatusLine;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.IJavaStatusConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.OpenActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * This action opens a Java editor on a Java element or file.
 * <p>
 * The action is applicable to selections containing elements of
 * type <code>ICompilationUnit</code>, <code>IMember</code>
 * or <code>IFile</code>.
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
	 * Creates a new <code>OpenAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public OpenAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.OpenAction_label); 
		setToolTipText(ActionMessages.OpenAction_tooltip); 
		setDescription(ActionMessages.OpenAction_description); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.OPEN_ACTION);
	}
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the Java editor
	 */
	public OpenAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setText(ActionMessages.OpenAction_declaration_label); 
		setEnabled(EditorUtility.getEditorInputJavaElement(fEditor, false) != null);
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void selectionChanged(ITextSelection selection) {
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(checkEnabled(selection));
	}
	
	private boolean checkEnabled(IStructuredSelection selection) {
		if (selection.isEmpty())
			return false;
		for (Iterator iter= selection.iterator(); iter.hasNext();) {
			Object element= iter.next();
			if (element instanceof ISourceReference)
				continue;
			if (element instanceof IFile)
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
	public void run(ITextSelection selection) {
		if (!isProcessable())
			return;
		try {
			IJavaElement[] elements= SelectionConverter.codeResolveForked(fEditor, false);
			if (elements == null || elements.length == 0) {
				IEditorStatusLine statusLine= (IEditorStatusLine) fEditor.getAdapter(IEditorStatusLine.class);
				if (statusLine != null)
					statusLine.setMessage(true, ActionMessages.OpenAction_error_messageBadSelection, null); 
				getShell().getDisplay().beep();
				return;
			}
			IJavaElement element= elements[0];
			if (elements.length > 1)
				element= OpenActionUtil.selectJavaElement(elements, getShell(), getDialogTitle(), ActionMessages.OpenAction_select_element);

			int type= element.getElementType();
			if (type == IJavaElement.JAVA_PROJECT || type == IJavaElement.PACKAGE_FRAGMENT_ROOT || type == IJavaElement.PACKAGE_FRAGMENT)
				element= EditorUtility.getEditorInputJavaElement(fEditor, false);
			run(new Object[] {element} );
		} catch (InvocationTargetException e) {
			showError(e);
		} catch (InterruptedException e) {
			// ignore
		}
	}

	private boolean isProcessable() {
		if (fEditor != null) {
			IJavaElement je= EditorUtility.getEditorInputJavaElement(fEditor, false);
			if (je instanceof ICompilationUnit && !JavaModelUtil.isPrimary((ICompilationUnit)je))
				return true; // can process non-primary working copies
		}
		return ActionUtil.isProcessable(getShell(), fEditor);
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(IStructuredSelection selection) {
		if (!checkEnabled(selection))
			return;
		run(selection.toArray());
	}
	
	/**
	 * Note: this method is for internal use only. Clients should not call this method.
	 * 
	 * @param elements the elements to process
	 */
	public void run(Object[] elements) {
		if (elements == null)
			return;
		for (int i= 0; i < elements.length; i++) {
			Object element= elements[i];
			try {
				element= getElementToOpen(element);
				boolean activateOnOpen= fEditor != null ? true : OpenStrategy.activateOnOpen();
				OpenActionUtil.open(element, activateOnOpen);
			} catch (JavaModelException e) {
				JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(),
					IJavaStatusConstants.INTERNAL_ERROR, ActionMessages.OpenAction_error_message, e)); 
				
				ErrorDialog.openError(getShell(), 
					getDialogTitle(),
					ActionMessages.OpenAction_error_messageProblems,  
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
						ActionMessages.OpenAction_error_messageProblems,  
						Messages.format(ActionMessages.OpenAction_error_messageArgs,  
							new String[] { name, x.getMessage() } ));			
				}
			}		
		}
	}
	
	/**
	 * Note: this method is for internal use only. Clients should not call this method.
	 * 
	 * @param object the element to open
	 * @return the real element to open
	 * @throws JavaModelException if an error occurs while accessing the Java model
	 */
	public Object getElementToOpen(Object object) throws JavaModelException {
		return object;
	}	
	
	private String getDialogTitle() {
		return ActionMessages.OpenAction_error_title; 
	}
	
	private void showError(InvocationTargetException e) {
		ExceptionHandler.handle(e, getShell(), getDialogTitle(), ActionMessages.OpenAction_error_message); 
	}
}
