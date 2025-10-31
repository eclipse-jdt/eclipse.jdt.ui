/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.actions;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;

/**
 * This action reveals the currently selected Java element in the package explorer.
 * <p>
 * The action is applicable to selections containing elements of type <code>IJavaElement</code>.
 *
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 * @since 2.0
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @deprecated As of 3.5, got replaced by generic Navigate &gt; Show In &gt;
 */
@Deprecated
public class ShowInPackageViewAction extends SelectionDispatchAction {

	private JavaEditor fEditor;

	/**
	 * Creates a new <code>ShowInPackageViewAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type
	 * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 *
	 * @param site the site providing context information for this action
	 */
	@Deprecated
	public ShowInPackageViewAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.ShowInPackageViewAction_label);
		setDescription(ActionMessages.ShowInPackageViewAction_description);
		setToolTipText(ActionMessages.ShowInPackageViewAction_tooltip);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.SHOW_IN_PACKAGEVIEW_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the Java editor
	 *
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	@Deprecated
	public ShowInPackageViewAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}

	@Deprecated
	@Override
	public void selectionChanged(ITextSelection selection) {
	}

	@Deprecated
	@Override
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(checkEnabled(selection));
	}

	private boolean checkEnabled(IStructuredSelection selection) {
		if (selection.size() != 1)
			return false;
		return selection.getFirstElement() instanceof IJavaElement;
	}

	@Deprecated
	@Override
	public void run(ITextSelection selection) {
		try {
			IJavaElement element= SelectionConverter.getElementAtOffset(fEditor);
			if (element != null)
				run(element);
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			String message= ActionMessages.ShowInPackageViewAction_error_message;
			ErrorDialog.openError(getShell(), getDialogTitle(), message, e.getStatus());
		}
	}

	@Deprecated
	@Override
	public void run(IStructuredSelection selection) {
		if (!checkEnabled(selection))
			return;
		run((IJavaElement)selection.getFirstElement());
	}

	/**
	 * Tries to reveal the given Java element
	 *
	 * @param element the element to reveal
	 */
	@Deprecated
	public void run(IJavaElement element) {
		if (element == null)
			return;

		PackageExplorerPart view= PackageExplorerPart.openInActivePerspective();
		view.tryToReveal(element);
	}

	private static String getDialogTitle() {
		return ActionMessages.ShowInPackageViewAction_dialog_title;
	}
}
