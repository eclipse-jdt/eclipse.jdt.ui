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

package org.eclipse.jdt.internal.junit.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;

import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfiguration;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;

public class TypeRenameParticipant extends RenameParticipant {

	private IType fType;
	
	private static class LaunchConfigChange extends Change {

		private IType fType;
		private ILaunchConfiguration fConfig;
		private String fNewName;

		public LaunchConfigChange(IType type, ILaunchConfiguration config, String newName) {
			fType= type;
			fConfig= config;
			fNewName= newName;
		}
		
		/**
		 * {@inheritDoc}
		 */
		public String getName() {
			return fConfig.getName();
		}
		
		/**
		 * {@inheritDoc}
		 */
		public void initializeValidationData(IProgressMonitor pm) throws CoreException {
			// must be implemented to decide correct value of isValid
		}

		public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
			return new RefactoringStatus();
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.refactoring.base.IChange#perform(org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext, org.eclipse.core.runtime.IProgressMonitor)
		 */
		public Change perform(IProgressMonitor pm) throws CoreException {
			pm.beginTask("", 1); //$NON-NLS-1$
			String current= fConfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, (String)null);
			int index= current.lastIndexOf('.');
			String newTypeName;
			if (index == -1) {
				newTypeName= fNewName;
			} else {
				newTypeName= current.substring(0, index + 1) + fNewName;
			}
			ILaunchConfigurationWorkingCopy copy= fConfig.getWorkingCopy();
			copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, newTypeName);
			if (fConfig.getName().equals(current))
					copy.rename(fNewName);
			copy.doSave();
			pm.worked(1);
			return new LaunchConfigChange(fType, fConfig, (index == -1) ? current : current.substring(index + 1));
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.refactoring.base.IChange#getModifiedLanguageElement()
		 */
		public Object getModifiedElement() {
			return fConfig;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IRenameParticipant#initialize(org.eclipse.jdt.internal.corext.refactoring.participants.RenameRefactoring, java.lang.Object)
	 */
	public void initialize(RefactoringProcessor processor, Object element) {
		setProcessor(processor);
		fType= (IType)element;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IRenameParticipant#canParticipate()
	 */
	public boolean isAvailable() {
		try {
			return TestSearchEngine.isTestOrTestSuite(fType);
		} catch (JavaModelException e) {
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IRenameParticipant#checkActivation()
	 */
	public RefactoringStatus checkActivation() {
		return new RefactoringStatus();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IRenameParticipant#checkInput(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) {
		return new RefactoringStatus();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IRenameParticipant#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException {
		if (!getArguments().getUpdateReferences()) 
			return null;	
		
		ILaunchManager manager= DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type= manager.getLaunchConfigurationType(JUnitLaunchConfiguration.ID_JUNIT_APPLICATION);
		ILaunchConfiguration configs[]= manager.getLaunchConfigurations(type);
		String typeName= fType.getFullyQualifiedName();
		List changes= new ArrayList();
		for (int i= 0; i < configs.length; i++) {
			String mainType= configs[i].getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, (String)null);
			if (typeName.equals(mainType)) {
				changes.add(new LaunchConfigChange(fType, configs[i], getArguments().getNewName()));
			}
		}
		return new CompositeChange(JUnitMessages.getString("TypeRenameParticipant.name"), (Change[]) changes.toArray(new Change[changes.size()]));
	}
}