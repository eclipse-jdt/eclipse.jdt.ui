/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core.refactoring.descriptors;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringContribution;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.RenameResourceDescriptor;

/**
 * Refactoring contribution for the rename resource refactoring.
 * 
 * @since 1.1
 * @deprecated since 1.2 moved to <code>org.eclipse.ltk.core.refactoring</code>.
 * Contribution still available for backwards compatibility of scripts
 */
public final class RenameResourceRefactoringContribution extends JavaRefactoringContribution {

	/**
	 * {@inheritDoc}
	 */
	public Refactoring createRefactoring(JavaRefactoringDescriptor javaDescriptor, RefactoringStatus status) throws CoreException {
		if (javaDescriptor instanceof RenameResourceDescriptor) {
			RenameResourceDescriptor descriptor= (RenameResourceDescriptor) javaDescriptor;
			
			// use the LTK RenameResourceDescriptor to create the refactoring
			
			RefactoringContribution newContribution= RefactoringCore.getRefactoringContribution(org.eclipse.ltk.core.refactoring.resource.RenameResourceDescriptor.ID);
			if (newContribution != null) {
				RefactoringDescriptor ltkDescriptor= newContribution.createDescriptor();
				if (ltkDescriptor instanceof org.eclipse.ltk.core.refactoring.resource.RenameResourceDescriptor) {
					((org.eclipse.ltk.core.refactoring.resource.RenameResourceDescriptor) ltkDescriptor).setNewName(descriptor.getNewName());
					((org.eclipse.ltk.core.refactoring.resource.RenameResourceDescriptor) ltkDescriptor).setResourcePath(descriptor.getResourcePath());
					return ltkDescriptor.createRefactoring(status);
				}
			}
			status.addFatalError(DescriptorMessages.RenameResourceRefactoringContribution_error_cannot_access);
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringDescriptor createDescriptor() {
		return new RenameResourceDescriptor();
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringDescriptor createDescriptor(String id, String project, String description, String comment, Map arguments, int flags) {
		return new RenameResourceDescriptor(project, description, comment, arguments, flags);
	}
}
