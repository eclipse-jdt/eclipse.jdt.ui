/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
import java.net.URL;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.JavaUI;

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
 *
 * @noextend This class is not intended to be subclassed by clients.
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
	 * Creates a new <code>OpenExternalJavadocAction</code>. The action requires
	 * that the selection provided by the given selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 *
	 * @param site the site providing additional context information for this action
	 * @param provider a special selection provider which is used instead
	 *  of the site's selection provider or <code>null</code> to use the site's
	 *  selection provider
	 *
	 * @since 3.2
	 * @deprecated Use {@link #setSpecialSelectionProvider(ISelectionProvider)} instead. This API will be
	 * removed after 3.2 M5.
     */
    public OpenExternalJavadocAction(IWorkbenchSite site, ISelectionProvider provider) {
        this(site);
        setSpecialSelectionProvider(provider);
    }


	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the Java editor
	 *
	 * @noreference This constructor is not intended to be referenced by clients.
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
			IJavaElement[] elements= SelectionConverter.codeResolveOrInputForked(fEditor);
			if (elements == null || elements.length == 0)
				return;
			IJavaElement candidate= elements[0];
			if (elements.length > 1) {
				candidate= SelectionConverter.selectJavaElement(elements, getShell(), getDialogTitle(), ActionMessages.OpenExternalJavadocAction_select_element);
			}
			if (candidate != null) {
				run(candidate);
			}
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(), getDialogTitle(), ActionMessages.OpenExternalJavadocAction_code_resolve_failed);
		} catch (InterruptedException e) {
			// cancelled
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
					showMessage(shell, Messages.format(message, new String[] { labelName, JavaElementLabels.getElementLabel(root, JavaElementLabels.ALL_DEFAULT) }), false);
				} else {
					IJavaElement annotatedElement= element.getJavaProject();
					String message= ActionMessages.OpenExternalJavadocAction_source_no_location;
					showMessage(shell, Messages.format(message, new String[] { labelName, JavaElementLabels.getElementLabel(annotatedElement, JavaElementLabels.ALL_DEFAULT) }), false);
				}
				return;
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
					MessageDialog.openError(shell, getTitle(), message);
				} else {
					MessageDialog.openInformation(shell, getTitle(), message);
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
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected String getDialogTitle() {
		return getTitle();
	}
}
