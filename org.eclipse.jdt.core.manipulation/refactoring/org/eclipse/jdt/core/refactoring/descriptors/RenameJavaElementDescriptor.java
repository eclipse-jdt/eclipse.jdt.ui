/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.refactoring.descriptors;

import org.eclipse.core.runtime.Assert;

import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.core.refactoring.descriptors.DescriptorMessages;

/**
 * Refactoring descriptor for the rename java element refactoring.
 * <p>
 * An instance of this refactoring descriptor may be obtained by calling
 * {@link RefactoringContribution#createDescriptor()} on a refactoring
 * contribution requested by invoking
 * {@link RefactoringCore#getRefactoringContribution(String)} with the
 * appropriate refactoring id.
 * </p>
 * <p>
 * Note: this class is not intended to be instantiated by clients.
 * </p>
 * 
 * @since 3.3
 */
public final class RenameJavaElementDescriptor extends JavaRefactoringDescriptor {

	/** The delegate attribute */
	private static final String ATTRIBUTE_DELEGATE= "delegate"; //$NON-NLS-1$

	/** The deprecate attribute */
	private static final String ATTRIBUTE_DEPRECATE= "deprecate"; //$NON-NLS-1$

	/** The references attribute */
	private static final String ATTRIBUTE_REFERENCES= "references"; //$NON-NLS-1$

	/** The rename getter attribute */
	private static final String ATTRIBUTE_RENAME_GETTER= "getter"; //$NON-NLS-1$

	/** The rename setter attribute */
	private static final String ATTRIBUTE_RENAME_SETTER= "setter"; //$NON-NLS-1$

	/** The textual matches attribute */
	private static final String ATTRIBUTE_TEXTUAL_MATCHES= "textual"; //$NON-NLS-1$

	/** The delegate attribute */
	private boolean fDelegate= false;

	/** The deprecate attribute */
	private boolean fDeprecate= false;

	/** The java element attribute */
	private IJavaElement fJavaElement= null;

	/** The name attribute */
	private String fName= null;

	/** The references attribute */
	private boolean fReferences= false;

	/** The rename getter attribute */
	private boolean fRenameGetter= false;

	/** The rename setter attribute */
	private boolean fRenameSetter= false;

	/** The textual attribute */
	private boolean fTextual= false;

	/**
	 * Creates a new refactoring descriptor.
	 * 
	 * @param id
	 *            the unique id of the refactoring
	 */
	public RenameJavaElementDescriptor(final String id) {
		super(id);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void populateArgumentMap() {
		super.populateArgumentMap();
		fArguments.put(JavaRefactoringDescriptor.ATTRIBUTE_NAME, fName);
		fArguments.put(JavaRefactoringDescriptor.ATTRIBUTE_INPUT, elementToHandle(getProject(), fJavaElement));
		final int type= fJavaElement.getElementType();
		if (type != IJavaElement.PACKAGE_FRAGMENT_ROOT)
			fArguments.put(ATTRIBUTE_REFERENCES, Boolean.toString(fReferences));
		if (type == IJavaElement.FIELD) {
			fArguments.put(ATTRIBUTE_RENAME_GETTER, Boolean.toString(fRenameGetter));
			fArguments.put(ATTRIBUTE_RENAME_SETTER, Boolean.toString(fRenameSetter));
		}
		switch (type) {
			case IJavaElement.PACKAGE_FRAGMENT:
			case IJavaElement.TYPE:
			case IJavaElement.FIELD:
				fArguments.put(ATTRIBUTE_TEXTUAL_MATCHES, Boolean.toString(fTextual));
			default:
				break;
		}
		switch (type) {
			case IJavaElement.METHOD:
			case IJavaElement.FIELD:
				fArguments.put(ATTRIBUTE_DEPRECATE, Boolean.toString(fDeprecate));
				fArguments.put(ATTRIBUTE_DELEGATE, Boolean.toString(fDelegate));
			default:
				break;
		}
	}

	/**
	 * Determines whether the delegate for a Java element should be declared as
	 * deprecated.
	 * <p>
	 * Note: Deprecation of the delegate is applicable to the Java elements
	 * {@link IMethod} and {@link IField}. The default is to not deprecate the
	 * delegate.
	 * </p>
	 * 
	 * @param deprecate
	 *            <code>true</code> to deprecate the delegate,
	 *            <code>false</code> otherwise
	 */
	public void setDeprecateDelegate(final boolean deprecate) {
		fDeprecate= deprecate;
	}

	/**
	 * Sets the Java element to be renamed.
	 * <p>
	 * Note: If the Java element to be renamed is of type
	 * {@link IJavaElement#JAVA_PROJECT}, clients are required to to set the
	 * project name to <code>null</code>.
	 * </p>
	 * 
	 * @param element
	 *            the Java element to be renamed
	 */
	public void setJavaElement(final IJavaElement element) {
		Assert.isNotNull(element);
		fJavaElement= element;
	}

	/**
	 * Determines whether the the original Java element should be kept as
	 * delegate to the renamed one.
	 * <p>
	 * Note: Keeping of original elements as delegates is applicable to the Java
	 * elements {@link IMethod} and {@link IField}. The default is to not keep
	 * the original as delegate.
	 * </p>
	 * 
	 * @param delegate
	 *            <code>true</code> to keep the original, <code>false</code>
	 *            otherwise
	 */
	public void setKeepOriginal(final boolean delegate) {
		fDelegate= delegate;
	}

	/**
	 * Sets the new name to rename the Java element to.
	 * 
	 * @param name
	 *            the non-empty new name to set
	 */
	public void setNewName(final String name) {
		Assert.isNotNull(name);
		Assert.isLegal(!"".equals(name), "Name must not be empty"); //$NON-NLS-1$//$NON-NLS-2$
		fName= name;
	}

	/**
	 * Sets the project name of this refactoring.
	 * <p>
	 * Note: If the Java element to be renamed is of type
	 * {@link IJavaElement#JAVA_PROJECT}, clients are required to to set the
	 * project name to <code>null</code>.
	 * </p>
	 * <p>
	 * The default is to associate the refactoring with the workspace.
	 * </p>
	 * 
	 * @param project
	 *            the non-empty project name to set, or <code>null</code> for
	 *            the workspace
	 * 
	 * @see #getProject()
	 */
	public void setProject(final String project) {
		super.setProject(project);
	}

	/**
	 * Determines whether getter methods for the Java element should be renamed.
	 * <p>
	 * Note: Renaming of getter methods is applicable for {@link IField}
	 * elements which do not represent enum constants only. The default is to
	 * not rename any getter methods.
	 * </p>
	 * 
	 * @param rename
	 *            <code>true</code> to rename getter methods,
	 *            <code>false</code> otherwise
	 */
	public void setRenameGetters(final boolean rename) {
		fRenameGetter= rename;
	}

	/**
	 * Determines whether setter methods for the Java element should be renamed.
	 * <p>
	 * Note: Renaming of setter methods is applicable for {@link IField}
	 * elements which do not represent enum constants only. The default is to
	 * not rename any setter methods.
	 * </p>
	 * 
	 * @param rename
	 *            <code>true</code> to rename setter methods,
	 *            <code>false</code> otherwise
	 */
	public void setRenameSetters(final boolean rename) {
		fRenameSetter= rename;
	}

	/**
	 * Determines whether references to the Java element should be renamed.
	 * <p>
	 * Note: Reference updating is applicable to all Java element types except
	 * {@link IPackageFragmentRoot}. The default is to not update references.
	 * </p>
	 * 
	 * @param update
	 *            <code>true</code> to update references, <code>false</code>
	 *            otherwise
	 */
	public void setUpdateReferences(final boolean update) {
		fReferences= update;
	}

	/**
	 * Determines whether textual occurrences of the Java element should be
	 * renamed.
	 * <p>
	 * Note: Textual occurrence updating is applicable to the Java elements
	 * {@link IPackageFragment}, {@link IType} and {@link IField}. The default
	 * is to not rename textual occurrences.
	 * </p>
	 * 
	 * @param update
	 *            <code>true</code> to update occurrences, <code>false</code>
	 *            otherwise
	 */
	public void setUpdateTextualOccurrences(final boolean update) {
		fTextual= update;
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus validateDescriptor() {
		RefactoringStatus status= super.validateDescriptor();
		if (fName == null || "".equals(fName)) //$NON-NLS-1$
			status.merge(RefactoringStatus.createFatalErrorStatus(DescriptorMessages.RenameResourceDescriptor_no_new_name));
		if (fJavaElement == null)
			status.merge(RefactoringStatus.createFatalErrorStatus(DescriptorMessages.RenameJavaElementDescriptor_no_java_element));
		else {
			final int type= fJavaElement.getElementType();
			if (type == IJavaElement.PACKAGE_FRAGMENT_ROOT && fReferences)
				status.merge(RefactoringStatus.createFatalErrorStatus(DescriptorMessages.RenameJavaElementDescriptor_reference_constraint));
			if (fTextual) {
				switch (type) {
					case IJavaElement.PACKAGE_FRAGMENT:
					case IJavaElement.TYPE:
					case IJavaElement.FIELD:
						break;
					default:
						status.merge(RefactoringStatus.createFatalErrorStatus(DescriptorMessages.RenameJavaElementDescriptor_textual_constraint));
				}
			}
			if (fDeprecate) {
				switch (type) {
					case IJavaElement.METHOD:
					case IJavaElement.FIELD:
						break;
					default:
						status.merge(RefactoringStatus.createFatalErrorStatus(DescriptorMessages.RenameJavaElementDescriptor_deprecation_constraint));
				}
			}
			if (fDelegate) {
				switch (type) {
					case IJavaElement.METHOD:
					case IJavaElement.FIELD:
						break;
					default:
						status.merge(RefactoringStatus.createFatalErrorStatus(DescriptorMessages.RenameJavaElementDescriptor_delegate_constraint));
				}
			}
			if (fRenameGetter || fRenameSetter) {
				if (type != IJavaElement.FIELD)
					status.merge(RefactoringStatus.createFatalErrorStatus(DescriptorMessages.RenameJavaElementDescriptor_accessor_constraint));
			}
		}
		return status;
	}
}