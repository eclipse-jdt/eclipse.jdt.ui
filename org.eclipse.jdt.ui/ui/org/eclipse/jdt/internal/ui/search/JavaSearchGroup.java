/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;

import org.eclipse.jdt.ui.IContextMenuConstants;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * Contribute Java search specific menu elements.
 */
public class JavaSearchGroup extends ActionGroup  {

	private JavaSearchSubGroup[] fGroups;

	public static final String GROUP_ID= IContextMenuConstants.GROUP_SEARCH;
	public static final String GROUP_NAME= SearchMessages.getString("group.search"); //$NON-NLS-1$

	private boolean fIsGroupForEditor;
	private boolean fInline;

	public JavaSearchGroup(IWorkbenchSite site) {
		fInline= true;
		fIsGroupForEditor= false;
		fGroups= new JavaSearchSubGroup[] {
			new ReferencesSearchGroup(site),
			new DeclarationsSearchGroup(site),
			new ImplementorsSearchGroup(site),
			new ReadReferencesSearchGroup(site),
			new WriteReferencesSearchGroup(site)
		};
	}

	public JavaSearchGroup(JavaEditor editor) {
		fInline= false;
		fIsGroupForEditor= true;
		fGroups= new JavaSearchSubGroup[] {
			new ReferencesSearchGroup(editor),
			new DeclarationsSearchGroup(editor),
			new ImplementorsSearchGroup(editor),
			new ReadReferencesSearchGroup(editor),
			new WriteReferencesSearchGroup(editor)
		};
	}

	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void setContext(ActionContext context) {
		super.setContext(context);
		for (int i= 0; i < fGroups.length; i++)
			fGroups[i].setContext(context);
	}

	public void fillContextMenu(IMenuManager manager) {
		if (fIsGroupForEditor) {
			fillEditorContextMenu(manager, ITextEditorActionConstants.GROUP_FIND);
			return;
		}
		IMenuManager javaSearchMM;
		if (fInline)
			javaSearchMM= manager;
		else
			javaSearchMM= new MenuManager(GROUP_NAME, GROUP_ID); //$NON-NLS-1$

		for (int i= 0; i < fGroups.length; i++)
			fGroups[i].fillContextMenu(javaSearchMM);
		
		if (!fInline && !javaSearchMM.isEmpty())
			manager.appendToGroup(GROUP_ID, javaSearchMM);
	}

	public String getGroupName() {
		return GROUP_NAME;
	}
	
	private void fillEditorContextMenu(IMenuManager manager, String groupId) {
		IMenuManager javaSearchMM;
		if (fInline) {
			javaSearchMM= manager;
			javaSearchMM.appendToGroup(groupId, new GroupMarker(GROUP_ID));
		} else {
			javaSearchMM= new MenuManager(GROUP_NAME, groupId);
			javaSearchMM.add(new GroupMarker(GROUP_ID));
		}
		
		for (int i= 0; i < fGroups.length; i++) {
			IMenuManager subManager= fGroups[i].getMenuManagerForGroup();
			javaSearchMM.appendToGroup(GROUP_ID, subManager);
		}
		if (!fInline)
			manager.appendToGroup(groupId, javaSearchMM);
	}
}
