/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.jface.action.Action;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;

public class HistoryAction extends Action {

	private TypeHierarchyViewPart fViewPart;
	private boolean fIsForward;
	
	public HistoryAction(TypeHierarchyViewPart viewPart, boolean forward) {
		super(getLabel(forward));
		setDescription(getDescription(forward));
		if (forward) {
			JavaPluginImages.setImageDescriptors(this, "lcl16", "forward_nav.gif"); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			JavaPluginImages.setImageDescriptors(this, "lcl16", "bkward_nav.gif"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		fViewPart= viewPart;
		fIsForward= forward;

		update();
	}
	
	private static String getLabel(boolean forward) {
		if (forward) {
			return TypeHierarchyMessages.getString("HistoryAction.forward.label"); //$NON-NLS-1$
		} else {
			return TypeHierarchyMessages.getString("HistoryAction.backward.label"); //$NON-NLS-1$
		}
	}
	
	private static String getDescription(boolean forward) {
		if (forward) {
			return TypeHierarchyMessages.getString("HistoryAction.forward.description"); //$NON-NLS-1$
		} else {
			return TypeHierarchyMessages.getString("HistoryAction.backward.description"); //$NON-NLS-1$
		}
	}	
		
	private static String getToolTip(boolean forward, Object arg) {
		if (forward) {
			if (arg == null) {
				return TypeHierarchyMessages.getString("HistoryAction.forward.tooltip.noarg"); //$NON-NLS-1$
			} else {
				return TypeHierarchyMessages.getFormattedString("HistoryAction.forward.tooltip.arg", arg); //$NON-NLS-1$
			}
		} else {
			if (arg == null) {
				return TypeHierarchyMessages.getString("HistoryAction.backward.tooltip.noarg"); //$NON-NLS-1$
			} else {
				return TypeHierarchyMessages.getFormattedString("HistoryAction.backward.tooltip.arg", arg); //$NON-NLS-1$
			}
		}
	}		
	
	
	public void setImageDescriptors(String type, String name) {
		JavaPluginImages.setImageDescriptors(this, type, name);
	}		
	
	private void updateToolTip(IType type) {
		Object arg= null;
		if (type != null) {
			arg= JavaModelUtility.getFullyQualifiedName(type);
		}
		setToolTipText(getToolTip(fIsForward, arg));
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
