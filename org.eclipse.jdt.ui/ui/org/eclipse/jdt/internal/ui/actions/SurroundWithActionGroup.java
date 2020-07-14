/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionGroup;

import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.actions.JdtActionConstants;
import org.eclipse.jdt.ui.actions.SurroundWithTryCatchAction;
import org.eclipse.jdt.ui.actions.SurroundWithTryMultiCatchAction;
import org.eclipse.jdt.ui.actions.SurroundWithTryWithResourcesAction;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;

public class SurroundWithActionGroup extends ActionGroup {

	private CompilationUnitEditor fEditor;
	private SurroundWithTryCatchAction fSurroundWithTryCatchAction;
	private SurroundWithTryMultiCatchAction fSurroundWithTryMultiCatchAction;
	private SurroundWithTryWithResourcesAction fSurroundWithTryWithResourcesAction;
	private final String fGroup;

	public SurroundWithActionGroup(CompilationUnitEditor editor, String group) {
		fEditor= editor;
		fGroup= group;
		fSurroundWithTryCatchAction= createSurroundWithTryCatchAction(fEditor);
		fSurroundWithTryMultiCatchAction= createSurroundWithTryMultiCatchAction(fEditor);
		fSurroundWithTryWithResourcesAction= createSurroundWithTryWithResourcesAction(fEditor);
	}

	@Override
	public void fillActionBars(IActionBars actionBar) {
		actionBar.setGlobalActionHandler(JdtActionConstants.SURROUND_WITH_TRY_CATCH, fSurroundWithTryCatchAction);
		actionBar.setGlobalActionHandler(JdtActionConstants.SURROUND_WITH_TRY_MULTI_CATCH, fSurroundWithTryMultiCatchAction);
		actionBar.setGlobalActionHandler(JdtActionConstants.SURROUND_WITH_TRY_WITH_RESOURCES, fSurroundWithTryWithResourcesAction);
	}

	/**
	 * The Menu to show when right click on the editor
	 * {@inheritDoc}
	 */
	@Override
	public void fillContextMenu(IMenuManager menu) {
		ISelectionProvider selectionProvider= fEditor.getSelectionProvider();
		if (selectionProvider == null)
			return;

		ISelection selection= selectionProvider.getSelection();
		if (!(selection instanceof ITextSelection))
			return;

		ITextSelection textSelection= (ITextSelection)selection;
		if (textSelection.getLength() == 0)
			return;

		String menuText= ActionMessages.SurroundWithTemplateMenuAction_SurroundWithTemplateSubMenuName;

		MenuManager subMenu = new MenuManager(menuText, SurroundWithTemplateMenuAction.SURROUND_WITH_QUICK_MENU_ACTION_ID);
		subMenu.setActionDefinitionId(SurroundWithTemplateMenuAction.SURROUND_WITH_QUICK_MENU_ACTION_ID);
		menu.appendToGroup(fGroup, subMenu);
		subMenu.add(new Action() {});
		subMenu.addMenuListener(manager -> {
			manager.removeAll();
			SurroundWithTemplateMenuAction.fillMenu(manager, fEditor, fSurroundWithTryCatchAction,
					fSurroundWithTryMultiCatchAction, fSurroundWithTryWithResourcesAction);
		});
	}

	static SurroundWithTryCatchAction createSurroundWithTryCatchAction(CompilationUnitEditor editor) {
		SurroundWithTryCatchAction result= new SurroundWithTryCatchAction(editor);
		result.setText(ActionMessages.SurroundWithTemplateMenuAction_SurroundWithTryCatchActionName);
		result.setImageDescriptor(JavaPluginImages.getDescriptor(JavaPluginImages.IMG_CORRECTION_CHANGE));
		result.setActionDefinitionId(IJavaEditorActionDefinitionIds.SURROUND_WITH_TRY_CATCH);
		editor.setAction("SurroundWithTryCatch", result); //$NON-NLS-1$
		return result;
	}

	static SurroundWithTryWithResourcesAction createSurroundWithTryWithResourcesAction(CompilationUnitEditor editor) {
		SurroundWithTryWithResourcesAction result= new SurroundWithTryWithResourcesAction(editor);
		result.setText(ActionMessages.SurroundWithTemplateMenuAction_SurroundWithTryWithResourcesActionName);
		result.setImageDescriptor(JavaPluginImages.getDescriptor(JavaPluginImages.IMG_CORRECTION_CHANGE));
		result.setActionDefinitionId(IJavaEditorActionDefinitionIds.SURROUND_WITH_TRY_WITH_RESOURCES);
		editor.setAction("SurroundWithTryWithResources", result); //$NON-NLS-1$
		return result;
	}

	static SurroundWithTryMultiCatchAction createSurroundWithTryMultiCatchAction(CompilationUnitEditor editor) {
		SurroundWithTryMultiCatchAction result= new SurroundWithTryMultiCatchAction(editor);
		result.setText(ActionMessages.SurroundWithTemplateMenuAction_SurroundWithTryMultiCatchActionName);
		result.setImageDescriptor(JavaPluginImages.getDescriptor(JavaPluginImages.IMG_CORRECTION_CHANGE));
		result.setActionDefinitionId(IJavaEditorActionDefinitionIds.SURROUND_WITH_TRY_MULTI_CATCH);
		editor.setAction("SurroundWithTryMultiCatch", result); //$NON-NLS-1$
		return result;
	}
}
