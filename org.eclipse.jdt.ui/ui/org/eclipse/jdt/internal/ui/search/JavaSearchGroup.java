/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IContextMenuConstants;

import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;
import org.eclipse.jdt.internal.ui.actions.GroupContext;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * Contribute Java search specific menu elements.
 */
public class JavaSearchGroup extends ContextMenuGroup  {

	private JavaSearchSubGroup[] fGroups;

	public static final String GROUP_ID= IContextMenuConstants.GROUP_SEARCH;
	public static final String GROUP_NAME= SearchMessages.getString("group.search"); //$NON-NLS-1$

	private boolean fInline;

	public JavaSearchGroup() {
		this(true);
	}

	public JavaSearchGroup(boolean inline) {
		fInline= inline;
		fGroups= new JavaSearchSubGroup[] {
			new ReferencesSearchGroup(),
			new ReadReferencesSearchGroup(),
			new WriteReferencesSearchGroup(),
			new DeclarationsSearchGroup(),
			new ImplementorsSearchGroup()
		};
	}

	public void fill(IMenuManager manager, GroupContext context) {
		IMenuManager javaSearchMM;
		if (fInline)
			javaSearchMM= manager;
		else
			javaSearchMM= new MenuManager(GROUP_NAME, GROUP_ID); //$NON-NLS-1$

		for (int i= 0; i < fGroups.length; i++)
			fGroups[i].fill(javaSearchMM, context);
		
		if (!fInline && !javaSearchMM.isEmpty())
			manager.appendToGroup(GROUP_ID, javaSearchMM);
	}

	public String getGroupName() {
		return GROUP_NAME;
	}
	
	public void fill(IMenuManager manager, String groupId, JavaEditor editor) {
		IStructuredSelection selection;
		try {
			selection= SelectionConverter.getStructuredSelection(editor);
		} catch (JavaModelException ex) {
			selection= StructuredSelection.EMPTY;
		}

		IMenuManager javaSearchMM;
		if (fInline) {
			javaSearchMM= manager;
			javaSearchMM.appendToGroup(groupId, new GroupMarker(GROUP_ID));
		} else {
			javaSearchMM= new MenuManager(GROUP_NAME, groupId);
			javaSearchMM.add(new GroupMarker(GROUP_ID));
		}
		
		for (int i= 0; i < fGroups.length; i++) {
			IMenuManager subManager= fGroups[i].getMenuManagerForGroup(selection);
//			if (!subManager.isEmpty())
				javaSearchMM.appendToGroup(GROUP_ID, subManager);
		}
		if (!fInline)
			manager.appendToGroup(groupId, javaSearchMM);
	}
}
