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
package org.eclipse.jdt.internal.ui.compare;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.util.Assert;

import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;

public class LocalHistoryActionGroup extends ActionGroup {

	private String fGroupName;

	private JavaHistoryAction fCompareWith;
	private JavaHistoryAction fReplaceWithPrevious;
	private JavaHistoryAction fReplaceWith;
	private JavaHistoryAction fAddFrom;
	
	public LocalHistoryActionGroup(CompilationUnitEditor editor, String groupName) {
		Assert.isNotNull(groupName);
		fGroupName= groupName;
		fCompareWith= new JavaCompareWithEditionAction();
		fCompareWith.init(editor,
			CompareMessages.getString("LocalHistoryActionGroup.action.compare_with"), //$NON-NLS-1$
			CompareMessages.getString("LocalHistoryActionGroup.action.compare_with.title"), //$NON-NLS-1$
			CompareMessages.getString("LocalHistoryActionGroup.action.compare_with.message")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(fCompareWith, IJavaHelpContextIds.COMPARE_WITH_HISTORY_ACTION);

		fReplaceWithPrevious= new JavaReplaceWithPreviousEditionAction();
		fReplaceWithPrevious.init(editor, 
			CompareMessages.getString("LocalHistoryActionGroup.action.replace_with_previous"), //$NON-NLS-1$
			CompareMessages.getString("LocalHistoryActionGroup.action.replace_with_previous.title"), //$NON-NLS-1$
			CompareMessages.getString("LocalHistoryActionGroup.action.replace_with_previous.message")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(fReplaceWithPrevious, IJavaHelpContextIds.REPLACE_WITH_PREVIOUS_FROM_HISTORY_ACTION);
		
		fReplaceWith= new JavaReplaceWithEditionAction();
		fReplaceWith.init(editor,
			CompareMessages.getString("LocalHistoryActionGroup.action.replace_with"), //$NON-NLS-1$
			CompareMessages.getString("LocalHistoryActionGroup.action.replace_with.title"), //$NON-NLS-1$
			CompareMessages.getString("LocalHistoryActionGroup.action.replace_with.message")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(fReplaceWith, IJavaHelpContextIds.REPLACE_WITH_HISTORY_ACTION);

		fAddFrom= new JavaAddElementFromHistory();
		fAddFrom.init(editor, 
			CompareMessages.getString("LocalHistoryActionGroup.action.add"), //$NON-NLS-1$
			CompareMessages.getString("LocalHistoryActionGroup.action.add.title"), //$NON-NLS-1$
			CompareMessages.getString("LocalHistoryActionGroup.action.add.message")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(fAddFrom, IJavaHelpContextIds.ADD_FROM_HISTORY_ACTION);
	}

	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		IMenuManager localMenu= new MenuManager(CompareMessages.getString("LocalHistoryActionGroup.menu.local_history")); //$NON-NLS-1$
		int added= 0;
		added+= addAction(localMenu, fCompareWith);
		added+= addAction(localMenu, fReplaceWithPrevious);
		added+= addAction(localMenu, fReplaceWith);
		added+= addAction(localMenu, fAddFrom);
		if (added > 0)
			menu.appendToGroup(fGroupName, localMenu);
	}
	
	private int addAction(IMenuManager menu, JavaHistoryAction action) {
		action.update();
		if (action.isEnabled()) {
			menu.add(action);
			return 1;
		}
		return 0;
	}
}
