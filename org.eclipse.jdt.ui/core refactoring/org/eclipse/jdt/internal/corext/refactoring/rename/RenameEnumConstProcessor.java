/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

public final class RenameEnumConstProcessor extends RenameFieldProcessor {

	private static final String ID_RENAME_ENUM_CONSTANT= "org.eclipse.jdt.ui.rename.enum.constant"; //$NON-NLS-1$
	public static final String IDENTIFIER= "org.eclipse.jdt.ui.renameEnumConstProcessor"; //$NON-NLS-1$

	public RenameEnumConstProcessor(IField field) {
		super(field);
	}

	public boolean isApplicable() throws CoreException {
		return RefactoringAvailabilityTester.isRenameEnumConstAvailable(getField());
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.rename.RenameFieldProcessor#canEnableGetterRenaming()
	 */
	public String canEnableGetterRenaming() throws CoreException {
		return ""; //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.rename.RenameFieldProcessor#canEnableSetterRenaming()
	 */
	public String canEnableSetterRenaming() throws CoreException {
		return ""; //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating#checkNewElementName(java.lang.String)
	 */
	public RefactoringStatus checkNewElementName(String newName) throws CoreException {
		RefactoringStatus result= Checks.checkEnumConstantName(newName);
		if (Checks.isAlreadyNamed(getField(), newName))
			result.addFatalError(RefactoringCoreMessages.RenameEnumConstRefactoring_another_name); 
		if (getField().getDeclaringType().getField(newName).exists())
			result.addFatalError(RefactoringCoreMessages.RenameEnumConstRefactoring_const_already_defined); 
		return result;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getIdentifier()
	 */
	public String getIdentifier() {
		return IDENTIFIER;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getProcessorName()
	 */
	public String getProcessorName() {
		return Messages.format(RefactoringCoreMessages.RenameEnumConstRefactoring_name, new String[] { getCurrentElementName(), getNewElementName()}); 
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.rename.JavaRenameProcessor#loadDerivedParticipants(org.eclipse.ltk.core.refactoring.RefactoringStatus, java.util.List, java.lang.String[], org.eclipse.ltk.core.refactoring.participants.SharableParticipants)
	 */
	protected void loadDerivedParticipants(RefactoringStatus status, List result, String[] natures, SharableParticipants shared) throws CoreException {
		// Don't load participants to rename getters and setters
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.rename.RenameFieldProcessor#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException {
		Change change= super.createChange(pm);
		if (change != null) {
			final CompositeChange composite= new CompositeChange("", new Change[] { change}) { //$NON-NLS-1$
				public final RefactoringDescriptor getRefactoringDescriptor() {
					final Map arguments= new HashMap();
					arguments.put(ATTRIBUTE_HANDLE, getField().getHandleIdentifier());
					arguments.put(ATTRIBUTE_NAME, getNewElementName());
					arguments.put(ATTRIBUTE_REFERENCES, Boolean.valueOf(fUpdateReferences).toString());
					arguments.put(ATTRIBUTE_TEXTUAL_MATCHES, Boolean.valueOf(fUpdateTextualMatches).toString());
					String project= null;
					IJavaProject javaProject= getField().getJavaProject();
					if (javaProject != null)
						project= javaProject.getElementName();
					return new RefactoringDescriptor(ID_RENAME_ENUM_CONSTANT, project, MessageFormat.format(RefactoringCoreMessages.RenameEnumConstProcessor_descriptor_description, new String[] { getField().getElementName(), getNewElementName()}), null, arguments);
				}
			};
			composite.markAsSynthetic();
			change= composite;
		}
		return change;
	}
}
