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

import org.eclipse.help.IHelp;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.javadoc.JavaDocLocations;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

/**
 * This action opens the Javadoc in an external broder if the selected element 
 * is represented by either
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
public class OpenExternalJavadocAction extends SelectionDispatchAction {
	
	private static boolean webBrowserOpened = false;
		
	private JavaEditor fEditor;
	
	/**
	 * Creates a new <code>OpenExternalJavadocAction</code>.
	 * 
	 * @param site the site providing additional context information for
	 * 	this action
	 */ 
	public OpenExternalJavadocAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.getString("OpenExternalJavadocAction.label")); //$NON-NLS-1$
		setDescription(ActionMessages.getString("OpenExternalJavadocAction.description")); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("OpenExternalJavadocAction.tooltip")); //$NON-NLS-1$
	}
	
	/**
	 * Creates a new <code>OpenExternalJavadocAction</code>.
	 * <p>
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * </p>
	 */
	public OpenExternalJavadocAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
	}
	
	/**
	 * Opens the given URL in a Browser.
	 * 
	 * <p>
	 * Note: This method is for internal use only. Clients should not call this method.
	 * </p>
	 */
	public static void openInBrowser(final URL url, final Shell shell) {
		IHelp help= WorkbenchHelp.getHelpSupport();
		if (help != null) {
			WorkbenchHelp.getHelpSupport().displayHelpResource(url.toExternalForm());
		} else {
			showMessage(shell, ActionMessages.getString("OpenExternalJavadocAction.help_not_available"), false); //$NON-NLS-1$
		}
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction
	 */
	protected String getDialogTitle() {
		return getTitle();
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
		if (selection.size() != 1)
			return false;
		return selection.getFirstElement() instanceof IJavaElement;
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	protected void run(ITextSelection selection) {
		try {
			run(SelectionConverter.codeResolveOrInput(fEditor, getShell(), getDialogTitle(), ActionMessages.getString("OpenExternalJavadocAction.select_element")));  //$NON-NLS-1$
		} catch(JavaModelException e) {
			ExceptionHandler.handle(e, getShell(), getDialogTitle(), ActionMessages.getString("OpenExternalJavadocAction.code_resolve_failed")); //$NON-NLS-1$
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
		Shell shell= getShell();
		try {
			String labelName= JavaElementLabels.getElementLabel(element, JavaElementLabels.M_PARAMETER_TYPES);
			
			URL baseURL= JavaDocLocations.getJavadocBaseLocation(element);
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
				URL noRefURL= JavaDocLocations.getJavaDocLocation(element, false);
				if (!(new File(noRefURL.getFile())).isFile()) {
					String message= ActionMessages.getString("OpenExternalJavadocAction.no_entry"); //$NON-NLS-1$
					showMessage(shell, MessageFormat.format(message, new String[] { labelName, noRefURL.toExternalForm() }), false);
					return;
				}
			}
		
			URL url= JavaDocLocations.getJavaDocLocation(element, true);
			if (url != null) {
				openInBrowser(url, shell);
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
}
		