/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.Assert;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Action to switch between the different hierarchy views.
 */
public class ToggleViewAction extends Action {
	
	private TypeHierarchyViewPart fViewPart;
	private int fViewerIndex;
		
	public ToggleViewAction(TypeHierarchyViewPart v, int viewerIndex) {
		super();
		String contextHelpId= null;
		if (viewerIndex == TypeHierarchyViewPart.VIEW_ID_SUPER) {
			setText(TypeHierarchyMessages.getString("ToggleViewAction.supertypes.label")); //$NON-NLS-1$
			contextHelpId= IJavaHelpContextIds.SHOW_SUPERTYPES;
			setDescription(TypeHierarchyMessages.getString("ToggleViewAction.supertypes.description")); //$NON-NLS-1$
			setToolTipText(TypeHierarchyMessages.getString("ToggleViewAction.supertypes.tooltip")); //$NON-NLS-1$
			JavaPluginImages.setLocalImageDescriptors(this, "super_co.gif"); //$NON-NLS-1$
		} else if (viewerIndex == TypeHierarchyViewPart.VIEW_ID_SUB) {
			setText(TypeHierarchyMessages.getString("ToggleViewAction.subtypes.label")); //$NON-NLS-1$
			contextHelpId= IJavaHelpContextIds.SHOW_SUBTYPES;
			setDescription(TypeHierarchyMessages.getString("ToggleViewAction.subtypes.description")); //$NON-NLS-1$
			setToolTipText(TypeHierarchyMessages.getString("ToggleViewAction.subtypes.tooltip")); //$NON-NLS-1$
			JavaPluginImages.setLocalImageDescriptors(this, "sub_co.gif"); //$NON-NLS-1$
		} else if (viewerIndex == TypeHierarchyViewPart.VIEW_ID_TYPE) {
			setText(TypeHierarchyMessages.getString("ToggleViewAction.vajhierarchy.label")); //$NON-NLS-1$
			contextHelpId= IJavaHelpContextIds.SHOW_HIERARCHY;
			setDescription(TypeHierarchyMessages.getString("ToggleViewAction.vajhierarchy.description")); //$NON-NLS-1$
			setToolTipText(TypeHierarchyMessages.getString("ToggleViewAction.vajhierarchy.tooltip")); //$NON-NLS-1$
			JavaPluginImages.setLocalImageDescriptors(this, "hierarchy_co.gif"); //$NON-NLS-1$
		} else {
			Assert.isTrue(false);
		}		
		
		fViewPart= v;
		fViewerIndex= viewerIndex;
		
		WorkbenchHelp.setHelp(this, contextHelpId);
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