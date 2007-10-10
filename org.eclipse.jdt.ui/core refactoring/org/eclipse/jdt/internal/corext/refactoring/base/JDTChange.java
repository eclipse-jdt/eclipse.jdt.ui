/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.base;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IResource;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.resource.ResourceChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.util.Messages;

/**
 * JDT specific change object. All code moved to ResourceChange. Kept for compatibility.
 */
public abstract class JDTChange extends ResourceChange {

	protected static final int NONE= ResourceChange.VALIDATE_DEFAULT;
	protected static final int READ_ONLY= ResourceChange.VALIDATE_NOT_READ_ONLY;
	protected static final int DIRTY= ResourceChange.VALIDATE_NOT_DIRTY;
	
	protected JDTChange() {
	}
	
	// protected final RefactoringStatus isValid(IProgressMonitor pm, boolean checkReadOnly, boolean checkDirty) throws CoreException {
	protected final RefactoringStatus isValid(IProgressMonitor pm, int flags) throws CoreException {
		setValidationMethod(flags);
		return super.isValid(pm);
	}

	protected static void checkIfModifiable(RefactoringStatus status, Object element, int flags) {
		checkIfModifiable(status, getResource(element), flags);
	}
	
	public abstract Object getModifiedElement();
	
	protected IResource getModifiedResource() {
		return getResource(getModifiedElement());
	}

	protected static void checkExistence(RefactoringStatus status, Object element) {
		if (element == null) {
			status.addFatalError(RefactoringCoreMessages.DynamicValidationStateChange_workspace_changed); 
			
		} else if (element instanceof IResource && !((IResource)element).exists()) {
			status.addFatalError(Messages.format(
				RefactoringCoreMessages.Change_does_not_exist, ((IResource)element).getFullPath().toString())); 
		} else if (element instanceof IJavaElement && !((IJavaElement)element).exists()) {
			status.addFatalError(Messages.format(
				RefactoringCoreMessages.Change_does_not_exist, ((IJavaElement)element).getElementName())); 
		}
	}

	private static IResource getResource(Object element) {
		if (element instanceof IResource) {
			return (IResource)element;
		}
		if (element instanceof ICompilationUnit) {
			return ((ICompilationUnit)element).getPrimary().getResource();
		}
		if (element instanceof IJavaElement) {
			return ((IJavaElement)element).getResource();
		}
		if (element instanceof IAdaptable) {
			return (IResource)((IAdaptable)element).getAdapter(IResource.class);
		}
		return null;
	}
		
}
