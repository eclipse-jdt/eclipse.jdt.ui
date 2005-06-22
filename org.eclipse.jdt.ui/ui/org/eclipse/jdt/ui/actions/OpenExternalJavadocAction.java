/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.actions;

import java.io.File;
import java.net.URL;
import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.OpenBrowserUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * This action opens the selected element's Javadoc in an external 
 * browser. 
 * <p>
 * The action is applicable to selections containing elements of 
 * type <code>IJavaElement</code>.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class OpenExternalJavadocAction extends SelectionDispatchAction {
		
	private JavaEditor fEditor;
	
	/**
	 * Creates a new <code>OpenExternalJavadocAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing additional context information for this action
	 */ 
	public OpenExternalJavadocAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.OpenExternalJavadocAction_label); 
		setDescription(ActionMessages.OpenExternalJavadocAction_description); 
		setToolTipText(ActionMessages.OpenExternalJavadocAction_tooltip); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.OPEN_EXTERNAL_JAVADOC_ACTION);
	}
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the Java editor
	 */
	public OpenExternalJavadocAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
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
		if (selection.size() != 1)
			return false;
		return selection.getFirstElement() instanceof IJavaElement;
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(ITextSelection selection) {
		IJavaElement element= SelectionConverter.getInput(fEditor);
		if (!ActionUtil.isProcessable(getShell(), element))
			return;
		
		try {
			run(SelectionConverter.codeResolveOrInput(fEditor, getShell(), getDialogTitle(), ActionMessages.OpenExternalJavadocAction_select_element));  
		} catch(JavaModelException e) {
			ExceptionHandler.handle(e, getShell(), getDialogTitle(), ActionMessages.OpenExternalJavadocAction_code_resolve_failed); 
		}
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(IStructuredSelection selection) {
		if (!checkEnabled(selection))
			return;
		IJavaElement element= (IJavaElement)selection.getFirstElement();
		if (!ActionUtil.isProcessable(getShell(), element))
			return;			
		run(element);
	}
	
	/*
	 * No Javadoc since the method isn't meant to be public but is
	 * since the beginning
	 */
	public void run(IJavaElement element) {
		if (element == null)
			return;
		Shell shell= getShell();
		try {
			String labelName= JavaElementLabels.getElementLabel(element, JavaElementLabels.ALL_DEFAULT);
			
			URL baseURL= JavaUI.getJavadocBaseLocation(element);
			if (baseURL == null) {
				IPackageFragmentRoot root= JavaModelUtil.getPackageFragmentRoot(element);
				if (root != null && root.getKind() == IPackageFragmentRoot.K_BINARY) {
					String message= ActionMessages.OpenExternalJavadocAction_libraries_no_location;	 
					showMessage(shell, MessageFormat.format(message, new String[] { labelName, root.getElementName() }), false);
				} else {
					IJavaElement annotatedElement= element.getJavaProject();
					String message= ActionMessages.OpenExternalJavadocAction_source_no_location;	 
					showMessage(shell, MessageFormat.format(message, new String[] { labelName, annotatedElement.getElementName() }), false);
				}
				return;
			}
			if ("file".equals(baseURL.getProtocol())) { //$NON-NLS-1$
				URL noRefURL= JavaUI.getJavadocLocation(element, false);
				if (!(new File(noRefURL.getFile())).isFile()) {
					String message= ActionMessages.OpenExternalJavadocAction_no_entry; 
					showMessage(shell, MessageFormat.format(message, new String[] { labelName, noRefURL.toExternalForm() }), false);
					return;
				}
			}
		
			URL url= JavaUI.getJavadocLocation(element, true);
			if (url != null) {
				OpenBrowserUtil.open(url, shell.getDisplay(), getTitle());
			} 		
		} catch (CoreException e) {
			JavaPlugin.log(e);
			showMessage(shell, ActionMessages.OpenExternalJavadocAction_opening_failed, true); 
		}
	}
	
	private static void showMessage(final Shell shell, final String message, final boolean isError) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				if (isError) {
					MessageDialog.openError(shell, getTitle(), message); //$NON-NLS-1$
				} else {
					MessageDialog.openInformation(shell, getTitle(), message); //$NON-NLS-1$
				}
			}
		});
	}
	
	private static String getTitle() {
		return ActionMessages.OpenExternalJavadocAction_dialog_title; 
	}
	
	/**
	 * Note: this method is for internal use only. Clients should not call this method.
	 * 
	 * @return the dialog default title
	 */
	protected String getDialogTitle() {
		return getTitle();
	}	
}
		
