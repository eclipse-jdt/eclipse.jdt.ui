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

package org.eclipse.jdt.internal.junit.refactoring;

import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

public class TypeRenameParticipant extends JUnitRenameParticipant {

	private IType fType;
	
	protected boolean initialize(Object element) {
		fType= (IType)element;
		try {
			return TestSearchEngine.isTestOrTestSuite(fType);
		} catch (JavaModelException e) {
			return false;
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return JUnitMessages.getString("TypeRenameParticipant.name"); //$NON-NLS-1$
	}

	protected void createChangeForConfigs(List changes, ILaunchConfiguration[] configs) throws CoreException {
		String typeName= fType.getFullyQualifiedName();
		for (int i= 0; i < configs.length; i++) {
			String mainType= configs[i].getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, (String)null);
			if (typeName.equals(mainType)) {
				changes.add(new LaunchConfigTypeChange(fType, configs[i], getArguments().getNewName()));
			}
		}
	}
}
