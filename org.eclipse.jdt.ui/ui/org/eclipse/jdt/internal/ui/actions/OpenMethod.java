/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;


public class OpenMethod extends SelectionDispatchAction {

	public static final String ACTION_DEFINITION_ID= "org.eclipse.jdt.ui.edit.text.java.open.method"; //$NON-NLS-1$

	public static final String ACTION_HANDLER_ID= "org.eclipse.jdt.ui.actions.OpenMethod"; //$NON-NLS-1$

	private JavaEditor fEditor;

	public OpenMethod(JavaEditor editor) {
		this(editor.getSite());
		fEditor= editor;
		setEnabled(true);
	}

	public OpenMethod(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.OpenMethodAction_ActionName);
		setToolTipText(ActionMessages.OpenMethodAction_ToolTipText);
	}

	@Override
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(canEnable(selection.toArray()));
	}

	private boolean canEnable(Object[] objects) {
		for (Object element : objects) {
			if (isValidElement(element))
				return true;
		}
		return false;
	}

	private boolean isValidElement(Object element) {
		if(element instanceof IMethod) {
			return true;
		}
		return false;
	}

	@Override
	public void run() {

		Object[] elements= getSelectedElements();
		if (elements == null) {
			return;
		}
		if (elements.length == 1) {
			Object element= elements[0];
			if (element instanceof IMethod method) {
				openMethodInClonedEditor(method);
				return;
			}

		}
	}

	private Object[] getSelectedElements() {
		if (fEditor != null) {
			Object element= getSelectedElement(fEditor);
			if (element!= null) {
				if(isValidElement(element)){
					return new Object[] { element };
				}
			}
		}
		MessageDialog.openInformation(getShell(), ActionMessages.OpenMethodAction_InfoDialogTitle, ActionMessages.OpenMethodAction_NoElementToQualify);
		return null;
	}

	private Object getSelectedElement(JavaEditor editor) {
		ISourceViewer viewer= editor.getViewer();
		if (viewer == null)
			return null;

		Point selectedRange= viewer.getSelectedRange();
		int length= selectedRange.y;
		int offset= selectedRange.x;

		ITypeRoot element= JavaUI.getEditorInputTypeRoot(editor.getEditorInput());
		if (element == null)
			return null;

		CompilationUnit ast= SharedASTProviderCore.getAST(element, SharedASTProviderCore.WAIT_YES, null);
		if (ast == null)
			return null;

		NodeFinder finder= new NodeFinder(ast, offset, length);
		ASTNode node= finder.getCoveringNode();
		IBinding binding= null;
		if (node instanceof Name nameNode) {
			binding=nameNode.resolveBinding();
			return binding.getJavaElement();
		}
		return null;
	}

	protected void openMethodInClonedEditor(IMethod method) {
		try {
		IEditorPart target = null;
		IEditorPart existingEditr = EditorUtility.isOpenInEditor(method);
		IJavaElement methodElement= method;
		if(existingEditr!=null) { // Cloning
			IWorkbenchWindow window= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			IWorkbenchPage page= window.getActivePage();
			target = page.openEditor(
						existingEditr.getEditorInput(),
						existingEditr.getSite().getId(),
						true,
						IWorkbenchPage.MATCH_NONE);
		} else {
			target = JavaUI.openInEditor(method, true, false);
		}
		JavaUI.revealInEditor(target, methodElement);
		} catch (PartInitException | JavaModelException e) {
			JavaPlugin.log(e);
		}
	}

}
