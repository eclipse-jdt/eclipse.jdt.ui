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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptor;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public final class RenameEnumConstProcessor extends RenameFieldProcessor {

	public static final String ID_RENAME_ENUM_CONSTANT= "org.eclipse.jdt.ui.rename.enum.constant"; //$NON-NLS-1$
	public static final String IDENTIFIER= "org.eclipse.jdt.ui.renameEnumConstProcessor"; //$NON-NLS-1$

	/**
	 * Creates a new rename enum const processor.
	 * @param field the enum constant, or <code>null</code> if invoked by scripting
	 */
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
	 * @see org.eclipse.jdt.internal.corext.refactoring.rename.RenameFieldProcessor#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Change createChange(final IProgressMonitor monitor) throws CoreException {
		Change change= super.createChange(monitor);
		if (change != null) {
			final CompositeChange composite= new CompositeChange("", new Change[] { change}) { //$NON-NLS-1$

				public final ChangeDescriptor getDescriptor() {
					final Map arguments= new HashMap();
					final IField field= getField();
					String project= null;
					IJavaProject javaProject= field.getJavaProject();
					if (javaProject != null)
						project= javaProject.getElementName();
					int flags= JavaRefactoringDescriptor.JAR_IMPORTABLE | JavaRefactoringDescriptor.JAR_REFACTORABLE | RefactoringDescriptor.STRUCTURAL_CHANGE;
					final IType declaring= field.getDeclaringType();
					try {
						if (!Flags.isPrivate(declaring.getFlags()))
							flags|= RefactoringDescriptor.MULTI_CHANGE;
						if (declaring.isAnonymous() || declaring.isLocal())
							flags|= JavaRefactoringDescriptor.JAR_SOURCE_ATTACHMENT;
					} catch (JavaModelException exception) {
						JavaPlugin.log(exception);
					}
					final JavaRefactoringDescriptor descriptor= new JavaRefactoringDescriptor(ID_RENAME_ENUM_CONSTANT, project, Messages.format(RefactoringCoreMessages.RenameEnumConstProcessor_descriptor_description, new String[] { field.getElementName(), JavaElementLabels.getElementLabel(field.getParent(), JavaElementLabels.ALL_FULLY_QUALIFIED), getNewElementName()}), getComment(), arguments, flags);
					arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_INPUT, descriptor.elementToHandle(field));
					arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_NAME, getNewElementName());
					arguments.put(ATTRIBUTE_REFERENCES, Boolean.valueOf(fUpdateReferences).toString());
					arguments.put(ATTRIBUTE_TEXTUAL_MATCHES, Boolean.valueOf(fUpdateTextualMatches).toString());
					return new RefactoringChangeDescriptor(descriptor);
				}
			};
			composite.markAsSynthetic();
			change= composite;
		}
		return change;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.rename.RenameFieldProcessor#initialize(org.eclipse.ltk.core.refactoring.participants.RefactoringArguments)
	 */
	public RefactoringStatus initialize(RefactoringArguments arguments) {
		if (arguments instanceof JavaRefactoringArguments) {
			final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;
			final String handle= extended.getAttribute(JavaRefactoringDescriptor.ATTRIBUTE_INPUT);
			if (handle != null) {
				final IJavaElement element= JavaRefactoringDescriptor.handleToElement(extended.getProject(), handle);
				if (element == null || element.getElementType() != IJavaElement.FIELD)
					return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_input_not_exists, ID_RENAME_ENUM_CONSTANT));
				else
					fField= (IField) element;
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JavaRefactoringDescriptor.ATTRIBUTE_INPUT));
			final String name= extended.getAttribute(JavaRefactoringDescriptor.ATTRIBUTE_NAME);
			if (name != null && !"".equals(name)) //$NON-NLS-1$
				setNewElementName(name);
			else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, JavaRefactoringDescriptor.ATTRIBUTE_NAME));
			final String references= extended.getAttribute(ATTRIBUTE_REFERENCES);
			if (references != null) {
				setUpdateReferences(Boolean.valueOf(references).booleanValue());
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_REFERENCES));
			final String matches= extended.getAttribute(ATTRIBUTE_TEXTUAL_MATCHES);
			if (matches != null) {
				setUpdateTextualMatches(Boolean.valueOf(matches).booleanValue());
			} else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_TEXTUAL_MATCHES));
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}
}