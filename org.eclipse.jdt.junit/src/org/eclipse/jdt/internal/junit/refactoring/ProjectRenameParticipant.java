/*******************************************************************************
 * Copyright (c) 2004 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.jdt.internal.junit.refactoring;

import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

public class ProjectRenameParticipant extends JUnitRenameParticipant {

	private IJavaProject fProject;
	
	protected boolean initialize(Object element) {
		fProject= (IJavaProject)element;
		return true;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return JUnitMessages.getString("TypeRenameParticipant.name");  //$NON-NLS-1$
	}

	protected void createChangeForConfigs(List changes, ILaunchConfiguration[] configs) throws CoreException {
		for (int i= 0; i < configs.length; i++) {
			String projectName= configs[i].getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String)null); 
			if (fProject.getElementName().equals(projectName)) {
				changes.add(new LaunchConfigProjectChange(configs[i], getArguments().getNewName()));  
			}
		}
	}
}