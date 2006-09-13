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

import java.util.Map;

import org.eclipse.core.runtime.Assert;

import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;

/**
 * Partial implementation of a Java refactoring contribution.
 * <p>
 * Note: this class is not intended to be extended outside the refactoring
 * framework.
 * </p>
 * 
 * @since 3.3
 */
public abstract class JavaRefactoringContribution extends RefactoringContribution {

	/**
	 * {@inheritDoc}
	 */
	public RefactoringDescriptor createDescriptor() {
		final String id= getId();

		// Rename refactorings
		if (IJavaRefactorings.RENAME_COMPILATION_UNIT.equals(id))
			return new RenameJavaElementDescriptor(IJavaRefactorings.RENAME_COMPILATION_UNIT);
		else if (IJavaRefactorings.RENAME_ENUM_CONSTANT.equals(id))
			return new RenameJavaElementDescriptor(IJavaRefactorings.RENAME_ENUM_CONSTANT);
		else if (IJavaRefactorings.RENAME_FIELD.equals(id))
			return new RenameJavaElementDescriptor(IJavaRefactorings.RENAME_FIELD);
		else if (IJavaRefactorings.RENAME_JAVA_PROJECT.equals(id))
			return new RenameJavaElementDescriptor(IJavaRefactorings.RENAME_JAVA_PROJECT);
		else if (IJavaRefactorings.RENAME_LOCAL_VARIABLE.equals(id))
			return new RenameJavaElementDescriptor(IJavaRefactorings.RENAME_LOCAL_VARIABLE);
		else if (IJavaRefactorings.RENAME_METHOD.equals(id))
			return new RenameJavaElementDescriptor(IJavaRefactorings.RENAME_METHOD);
		else if (IJavaRefactorings.RENAME_PACKAGE.equals(id))
			return new RenameJavaElementDescriptor(IJavaRefactorings.RENAME_PACKAGE);
		else if (IJavaRefactorings.RENAME_RESOURCE.equals(id))
			return new RenameJavaElementDescriptor(IJavaRefactorings.RENAME_RESOURCE);
		else if (IJavaRefactorings.RENAME_SOURCE_FOLDER.equals(id))
			return new RenameJavaElementDescriptor(IJavaRefactorings.RENAME_SOURCE_FOLDER);
		else if (IJavaRefactorings.RENAME_TYPE.equals(id))
			return new RenameJavaElementDescriptor(IJavaRefactorings.RENAME_TYPE);
		else if (IJavaRefactorings.RENAME_TYPE_PARAMETER.equals(id))
			return new RenameJavaElementDescriptor(IJavaRefactorings.RENAME_TYPE_PARAMETER);

		// Move refactorings
		else if (IJavaRefactorings.MOVE_STATIC_MEMBERS.equals(id))
			return new MoveStaticMembersDescriptor();

		// Use supertype
		else if (IJavaRefactorings.USE_SUPER_TYPE.equals(id))
			return new UseSupertypeDescriptor();

		return super.createDescriptor();
	}

	/**
	 * {@inheritDoc}
	 */
	public final Map retrieveArgumentMap(final RefactoringDescriptor descriptor) {
		Assert.isNotNull(descriptor);
		if (descriptor instanceof JavaRefactoringDescriptor)
			return ((JavaRefactoringDescriptor) descriptor).getArguments();
		return super.retrieveArgumentMap(descriptor);
	}
}