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
import java.util.Arrays;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.help.IHelpResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.javadoc.JavaDocLocations;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionChecker;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.OpenActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
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
	public OpenExternalJavadocAction(UnifiedSite site) {
		super(site);
		setText("Open E&xternal Javadoc@Shift+F2");
		setDescription("Opens the Javadoc of the selected element in an external browser");
		setToolTipText("Opens the Javadoc of the selected element in an external browser");
	}
	
	/**
	 * Creates a new <code>OpenExternalJavadocAction</code>.
	 * <p>
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * </p>
	 */
	public OpenExternalJavadocAction(JavaEditor editor) {
		this(UnifiedSite.create(editor.getEditorSite()));
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
//		IHelpResource helpResource= new IHelpResource() {
//			public String getHref() {
//				return url.toExternalForm();
//			}
//
//			public String getLabel() {
//				return url.toExternalForm();
//			}
//		};
		WorkbenchHelp.getHelpSupport().displayHelpResource(url.toExternalForm());
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
	protected void run(ITextSelection selection) throws JavaModelException {
		run(SelectionConverter.codeResolveOrInput(fEditor, getShell(), ActionUtil.getText(this), "&Select or enter the element to open:"));
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
					String message= "The documentation location for ''{0}'' has not been configured. For elements from libraries specify the Javadoc location URL on the property page of the parent JAR (''{1}'').";	
					showError(shell, MessageFormat.format(message, new String[] { labelName, root.getElementName() }));
				} else {
					IJavaElement annotatedElement= element.getJavaProject();
					String message= "The documentation location for ''{0}'' has not been configured. For elements from source specify the Javadoc location URL on the property page of the parent project (''{1}'').";	
					showError(shell, MessageFormat.format(message, new String[] { labelName, annotatedElement.getElementName() }));
				}
				return;
			}
			if ("file".equals(baseURL.getProtocol())) {
				URL noRefURL= JavaDocLocations.getJavaDocLocation(element, false);
				if (!(new File(noRefURL.getFile())).isFile()) {
					String message= "No documentation available for ''{0}'' in ''{1}''.";
					showError(shell, MessageFormat.format(message, new String[] { labelName, baseURL.toExternalForm() }));
					return;
				}
			}
		
			URL url= JavaDocLocations.getJavaDocLocation(element, true);
			if (url != null) {
				openInBrowser(url, shell);
			} 		
		} catch (CoreException e) {
			JavaPlugin.log(e);
			showError(shell, "Opening Javadoc failed. See log for details.");
		}
	}
	
	private static void showError(final Shell shell, final String message) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				MessageDialog.openError(shell, "Open External Javadoc", message);
			}
		});
	}
}
		