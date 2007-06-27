/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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

import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;

/**
 * Refactoring descriptor for the pull up refactoring.
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
public final class PullUpDescriptor extends JavaRefactoringDescriptor {

	/**
	 * Creates a new refactoring descriptor.
	 */
	public PullUpDescriptor() {
		super(IJavaRefactorings.PULL_UP);
	}
	
	/**
	 * Note: This constructor is experimental and for internal use only. Clients should not call this constructor.
	 *
	 * @param project
	 *            the non-empty name of the project associated with this
	 *            refactoring, or <code>null</code> for a workspace
	 *            refactoring
	 * @param description
	 *            a non-empty human-readable description of the particular
	 *            refactoring instance
	 * @param comment
	 *            the human-readable comment of the particular refactoring
	 *            instance, or <code>null</code> for no comment
	 * @param arguments
	 * 			  a map of arguments that will be persisted and describes
	 * 			  all settings for this refactoring
	 * @param flags
	 *            the flags of the refactoring descriptor
	 *            
	 * @since 3.4
	 */
	public PullUpDescriptor( String project, String description, String comment, Map arguments, int flags) {
		super(IJavaRefactorings.PULL_UP, project, description, comment, arguments, flags);
	}
	
}