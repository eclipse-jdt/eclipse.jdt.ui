/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
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
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

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
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

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
		setText(ActionMessages.getString("OpenExternalJavadocAction.label")); //$NON-NLS-1$
		setDescription(ActionMessages.getString("OpenExternalJavadocAction.description")); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("OpenExternalJavadocAction.tooltip")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.OPEN_EXTERNAL_JAVADOC_ACTION);
	}
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
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
			run(SelectionConverter.codeResolveOrInput(fEditor, getShell(), getDialogTitle(), ActionMessages.getString("OpenExternalJavadocAction.select_element")));  //$NON-NLS-1$
		} catch(JavaModelException e) {
			ExceptionHandler.handle(e, getShell(), getDialogTitle(), ActionMessages.getString("OpenExternalJavadocAction.code_resolve_failed")); //$NON-NLS-1$
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
	
	public void run(IJavaElement element) {
		if (element == null)
			return;
		Shell shell= getShell();
		try {
			String labelName= JavaElementLabels.getElementLabel(element, JavaElementLabels.M_PARAMETER_TYPES);
			
			URL baseURL= JavaUI.getJavadocBaseLocation(element);
			if (baseURL == null) {
				IPackageFragmentRoot root= JavaModelUtil.getPackageFragmentRoot(element);
				if (root != null && root.getKind() == IPackageFragmentRoot.K_BINARY) {
					String message= ActionMessages.getString("OpenExternalJavadocAction.libraries.no_location");	 //$NON-NLS-1$
					showMessage(shell, MessageFormat.format(message, new String[] { labelName, root.getElementName() }), false);
				} else {
					IJavaElement annotatedElement= element.getJavaProject();
					String message= ActionMessages.getString("OpenExternalJavadocAction.source.no_location");	 //$NON-NLS-1$
					showMessage(shell, MessageFormat.format(message, new String[] { labelName, annotatedElement.getElementName() }), false);
				}
				return;
			}
			if ("file".equals(baseURL.getProtocol())) { //$NON-NLS-1$
				URL noRefURL= JavaUI.getJavadocLocation(element, false);
				if (!(new File(noRefURL.getFile())).isFile()) {
					String message= ActionMessages.getString("OpenExternalJavadocAction.no_entry"); //$NON-NLS-1$
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
			showMessage(shell, ActionMessages.getString("OpenExternalJavadocAction.opening_failed"), true); //$NON-NLS-1$
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
		return ActionMessages.getString("OpenExternalJavadocAction.dialog.title"); //$NON-NLS-1$
	}
	
	/**
	 * Note: this method is for internal use only. Clients should not call this method.
	 */
	protected String getDialogTitle() {
		return getTitle();
	}	
}
		
