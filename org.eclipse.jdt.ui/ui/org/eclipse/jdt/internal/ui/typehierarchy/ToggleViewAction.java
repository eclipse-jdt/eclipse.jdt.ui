/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.jface.action.Action;

public class ToggleViewAction extends Action {
	
	private TypeHierarchyViewPart fViewPart;
	private int fViewerIndex;
		
	public ToggleViewAction(TypeHierarchyViewPart v, int viewerIndex, String title, boolean isChecked) {
		super(title);
		fViewPart= v;
		fViewerIndex= viewerIndex;
		setChecked(isChecked);
	}
				
	public int getViewerIndex() {
		return fViewerIndex;
	}
	
	public void run() {
		fViewPart.setView(fViewerIndex);
	}		
}