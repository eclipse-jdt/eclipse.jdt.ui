/*
 * (c) Copyright IBM Corp. 2000, 2003.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.swt.custom.BusyIndicator;

import org.eclipse.jface.action.Action;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Action enable / disable showing qualified type names
 */
public class ShowQualifiedTypeNamesAction extends Action {

	private TypeHierarchyViewPart fView;	
	
	public ShowQualifiedTypeNamesAction(TypeHierarchyViewPart v, boolean initValue) {
		super(TypeHierarchyMessages.getString("ShowQualifiedTypeNamesAction.label")); //$NON-NLS-1$
		setDescription(TypeHierarchyMessages.getString("ShowQualifiedTypeNamesAction.description")); //$NON-NLS-1$
		setToolTipText(TypeHierarchyMessages.getString("ShowQualifiedTypeNamesAction.tooltip")); //$NON-NLS-1$
		
		setImageDescriptor(JavaPluginImages.DESC_TOOL_PACKSNIPPET);
		
		fView= v;
		setChecked(initValue);
		
		// JavaPluginImages.setLocalImageDescriptors(this, "impl_co.gif"); //$NON-NLS-1$
		//WorkbenchHelp.setHelp(this, IJavaHelpContextIds.ENABLE_METHODFILTER_ACTION);
	}

	/*
	 * @see Action#actionPerformed
	 */		
	public void run() {
		BusyIndicator.showWhile(fView.getSite().getShell().getDisplay(), new Runnable() {
			public void run() {
				fView.showQualifiedTypeNames(isChecked());
			}
		});
	}
}