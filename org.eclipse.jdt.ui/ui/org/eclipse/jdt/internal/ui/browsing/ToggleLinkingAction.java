/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.browsing;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.help.WorkbenchHelp;


/**
 * This action toggles whether this package explorer links its selection to the active
 * editor.
 * 
 * @since 2.1
 */
public class ToggleLinkingAction extends Action {
	
	JavaBrowsingPart fJavaBrowsingPart;
	
	/**
	 * Constructs a new action.
	 */
	public ToggleLinkingAction(JavaBrowsingPart part) {
		super(JavaBrowsingMessages.getString("ToggleLinkingAction.label")); //$NON-NLS-1$
		setDescription(JavaBrowsingMessages.getString("ToggleLinkingAction.description")); //$NON-NLS-1$
		setToolTipText(JavaBrowsingMessages.getString("ToggleLinkingAction.tooltip")); //$NON-NLS-1$
		JavaPluginImages.setLocalImageDescriptors(this, "synced.gif"); //$NON-NLS-1$		
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.LINK_EDITOR_ACTION);

		setChecked(part.isLinkingEnabled());
		fJavaBrowsingPart= part;
	}

	/**
	 * Runs the action.
	 */
	public void run() {
		fJavaBrowsingPart.setLinkingEnabled(isChecked());
	}

}