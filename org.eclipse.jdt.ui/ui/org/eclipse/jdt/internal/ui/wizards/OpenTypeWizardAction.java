/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.CoreUtility;

public class OpenTypeWizardAction extends AbstractOpenWizardAction {

	private final static String ATT_NAME = "name";//$NON-NLS-1$
	private final static String ATT_CLASS = "class";//$NON-NLS-1$
	private final static String ATT_ICON = "icon";//$NON-NLS-1$
	private static final String TAG_DESCRIPTION = "description";	//$NON-NLS-1$
	
	private IConfigurationElement fConfigurationElement;

	public OpenTypeWizardAction(IConfigurationElement element) {
		fConfigurationElement= element;
		setText(element.getAttribute(ATT_NAME));
		
		String description= getDescriptionFromConfig(fConfigurationElement);
		setDescription(description);
		setToolTipText(description);
		setImageDescriptor(getIconFromConfig(fConfigurationElement));
	}
	
	private String getDescriptionFromConfig(IConfigurationElement config) {
		IConfigurationElement [] children = config.getChildren(TAG_DESCRIPTION);
		if (children.length>=1) {
			return children[0].getValue();
		}
		return ""; //$NON-NLS-1$
	}

	private ImageDescriptor getIconFromConfig(IConfigurationElement config) {
		try {
			String iconName = config.getAttribute(ATT_ICON);
			if (iconName != null) {
				URL pluginInstallUrl = Platform.getBundle(config.getNamespace()).getEntry("/"); //$NON-NLS-1$			
				return ImageDescriptor.createFromURL(new URL(pluginInstallUrl, iconName));
			}
			return null;
		} catch (MalformedURLException exception) {
			JavaPlugin.logErrorMessage("Unable to load wizard icon"); //$NON-NLS-1$
		}
		return ImageDescriptor.getMissingImageDescriptor();
		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.wizards.AbstractOpenWizardAction#createWizard()
	 */
	protected Wizard createWizard() throws CoreException {
		return (Wizard) CoreUtility.createExtension(fConfigurationElement, ATT_CLASS);
	}
}
