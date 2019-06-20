/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFolder;

import org.eclipse.ltk.core.refactoring.RefactoringDescriptorProxy;
import org.eclipse.ltk.core.refactoring.history.RefactoringHistory;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

/**
 * Content provider for Java models.
 *
 * @since 3.2
 */
public final class JavaModelContentProvider extends StandardJavaElementContentProvider {

	/** The name of the settings folder */
	private static final String NAME_SETTINGS_FOLDER= ".settings"; //$NON-NLS-1$

	/**
	 * Creates a new java model content provider.
	 */
	public JavaModelContentProvider() {
		super(true);
	}

	@Override
	public Object[] getChildren(final Object element) {
		if (element instanceof ICompilationUnit)
			return NO_CHILDREN;
		else if (element instanceof RefactoringHistory)
			return ((RefactoringHistory) element).getDescriptors();
		else if (element instanceof IJavaProject) {
			final List<Object> elements= new ArrayList<>();
			elements.add(((IJavaProject) element).getProject().getFolder(NAME_SETTINGS_FOLDER));
			for (Object child : super.getChildren(element)) {
				if (!elements.contains(child)) {
					elements.add(child);
				}
			}
			return elements.toArray();
		} else if (element instanceof IFolder) {
			final IFolder folder= (IFolder) element;
			try {
				return folder.members();
			} catch (CoreException exception) {
				// Do nothing
			}
		}
		return super.getChildren(element);
	}

	@Override
	public boolean hasChildren(final Object element) {
		if (element instanceof ICompilationUnit)
			return false;
		else if (element instanceof RefactoringHistory)
			return true;
		else if (element instanceof RefactoringDescriptorProxy)
			return false;
		else if (element instanceof IFolder)
			return true;
		return super.hasChildren(element);
	}
}
