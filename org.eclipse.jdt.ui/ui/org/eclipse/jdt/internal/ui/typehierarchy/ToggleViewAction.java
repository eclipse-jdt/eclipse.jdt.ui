/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.jface.action.Action;

import org.eclipse.ui.help.WorkbenchHelp;

/**
 * Action to switch between the different hierarchy views.
 */
public class ToggleViewAction extends Action {
	
	private TypeHierarchyViewPart fViewPart;
	private int fViewerIndex;
		
	public ToggleViewAction(TypeHierarchyViewPart v, int viewerIndex, String title, String contextHelpId, boolean isChecked) {
		super(title);
		fViewPart= v;
		fViewerIndex= viewerIndex;
		setChecked(isChecked);
		
		WorkbenchHelp.setHelp(this,	new Object[] { contextHelpId });
	}
				
	public int getViewerIndex() {
		return fViewerIndex;
	}

	/*
	 * @see Action#actionPerformed
	 */	
	public void run() {
		fViewPart.setView(fViewerIndex);
	}		
}