/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.help.WorkbenchHelp;


/**
 * This action toggles whether the type hierarchy links its selection to the active
 * editor.
 * 
 * @since 2.1
 */
public class ToggleLinkingAction extends Action {
	
	TypeHierarchyViewPart fHierarchyViewPart;
	
	/**
	 * Constructs a new action.
	 */
	public ToggleLinkingAction(TypeHierarchyViewPart part) {
		super(TypeHierarchyMessages.getString("ToggleLinkingAction.label")); //$NON-NLS-1$
		setDescription(TypeHierarchyMessages.getString("ToggleLinkingAction.description")); //$NON-NLS-1$
		setToolTipText(TypeHierarchyMessages.getString("ToggleLinkingAction.tooltip")); //$NON-NLS-1$
		JavaPluginImages.setLocalImageDescriptors(this, "synced.gif"); //$NON-NLS-1$		
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.LINK_EDITOR_ACTION);

		setChecked(part.isLinkingEnabled());
		fHierarchyViewPart= part;
	}

	/**
	 * Runs the action.
	 */
	public void run() {
		fHierarchyViewPart.setLinkingEnabled(isChecked());
	}

}
