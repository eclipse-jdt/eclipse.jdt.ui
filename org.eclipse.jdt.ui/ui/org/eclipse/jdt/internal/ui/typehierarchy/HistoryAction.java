/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.jface.action.Action;

import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;

public class HistoryAction extends Action {
	

	private TypeHierarchyViewPart fViewPart;
	private boolean fIsForward;
	private String fPrefix;
	
	public HistoryAction(TypeHierarchyViewPart viewPart, boolean forward, String prefix) {
		super(JavaPlugin.getResourceString(prefix + "label"));
		fViewPart= viewPart;
		fIsForward= forward;
		fPrefix= prefix;
		
		setDescription(JavaPlugin.getResourceString(prefix + "description"));
		update();
	}
	
	public void setImageDescriptors(String type, String name) {
		JavaPluginImages.setImageDescriptors(this, type, name);
	}		
	
	private void updateToolTip(IType type) {
		String text;
		if (type == null) {
			text= JavaPlugin.getResourceString(fPrefix + "tooltip.noarg");
		} else {
			String typeName= JavaModelUtility.getFullyQualifiedName(type);
			text= JavaPlugin.getFormattedString(fPrefix + "tooltip.arg", typeName);
		}
		setToolTipText(text);
	}
			
	/**
	 * Called by the TypeHierarchyViewPart to update the tooltip
	 * and enable/disable state
	 */
	public void update() {
		IType type= fViewPart.getHistoryEntry(fIsForward);
		updateToolTip(type);
		setEnabled(type != null);
	}
	
	
	/**
	 * @see Action#run()
	 */
	public void run() {
		fViewPart.gotoHistoryEntry(fIsForward);
	}
	
}
