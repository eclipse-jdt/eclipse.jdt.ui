/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import java.util.ResourceBundle;

import org.eclipse.jdt.internal.ui.actions.JavaUIAction;

public class ToggleViewAction extends JavaUIAction {
	
	private TypeHierarchyViewPart fViewPart;
	private int fViewerIndex;
	private ToggleViewAction[] fOtherActions;
		
	public ToggleViewAction(TypeHierarchyViewPart v, int viewerIndex, ResourceBundle bundle, String prefix) {
		super(bundle, prefix);
		fViewPart= v;
		fViewerIndex= viewerIndex;
	}
	
	public void setOthers(ToggleViewAction[] others) {
		fOtherActions= others;
	}
	
	public void setActive(boolean isActive) {
		setChecked(isActive);
	}
	
	public boolean isActive() {
		return isChecked();
	}
	
	public void run() {
		if (isActive()) {
			fViewPart.setView(fViewerIndex);
			for (int i= 0; i < fOtherActions.length; i++) {
				fOtherActions[i].setActive(false);
			}
		} else {
			setActive(true);
		}
	}		
}