/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
 *     Red Hat Inc - Moved logic from various UI classes
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.util;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class JavaProjectUtilities {
	private JavaProjectUtilities() {
	}


	public static void createJavaProject(IProject project) throws CoreException {
		if (!project.exists()) {
			ResourcesUtility.createProject(project, null, new NullProgressMonitor());
			JavaProjectUtilities.addJavaNature(project, new NullProgressMonitor());
		}
	}

	/*
	 * Moved from org.eclipse.jdt.internal.corext.refactoring.reorg.LoggedCreateTargetQueries
	 */
	public static void createPackageFragmentRoot(IPackageFragmentRoot root) throws CoreException {
		final IJavaProject project= root.getJavaProject();
		if (!project.exists())
			JavaProjectUtilities.createJavaProject(project.getProject());
		final IFolder folder= project.getProject().getFolder(root.getElementName());
		if (!folder.exists())
			ResourcesUtility.createFolder(folder, true, true, new NullProgressMonitor());
		final List<IClasspathEntry> list= Arrays.asList(project.getRawClasspath());
		list.add(JavaCore.newSourceEntry(folder.getFullPath()));
		project.setRawClasspath(list.toArray(new IClasspathEntry[list.size()]), new NullProgressMonitor());
	}


	/*
	 * Moved from org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock
	 */
	public static void addJavaNature(IProject project, IProgressMonitor monitor) throws CoreException {
		if (monitor != null && monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		if (!project.hasNature(JavaCore.NATURE_ID)) {
			IProjectDescription description = project.getDescription();
			String[] prevNatures= description.getNatureIds();
			String[] newNatures= new String[prevNatures.length + 1];
			System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
			newNatures[prevNatures.length]= JavaCore.NATURE_ID;
			description.setNatureIds(newNatures);
			project.setDescription(description, monitor);
		} else {
			if (monitor != null) {
				monitor.worked(1);
			}
		}
	}

	/*
	 * Moved from org.eclipse.jdt.internal.ui.actions.SelectionConverter
	 */
	public static IJavaElement resolveEnclosingElement(IJavaElement input, int offset, int length) throws JavaModelException {
		IJavaElement atOffset= null;
		if (input instanceof ICompilationUnit) {
			ICompilationUnit cunit= (ICompilationUnit)input;
			JavaModelUtil.reconcile(cunit);
			atOffset= cunit.getElementAt(offset);
		} else if (input instanceof IClassFile) {
			IClassFile cfile= (IClassFile)input;
			atOffset= cfile.getElementAt(offset);
		} else {
			return null;
		}
		if (atOffset == null) {
			return input;
		} else {
			int selectionEnd= offset + length;
			IJavaElement result= atOffset;
			if (atOffset instanceof ISourceReference) {
				ISourceRange range= ((ISourceReference)atOffset).getSourceRange();
				while (range.getOffset() + range.getLength() < selectionEnd) {
					result= result.getParent();
					if (! (result instanceof ISourceReference)) {
						result= input;
						break;
					}
					range= ((ISourceReference)result).getSourceRange();
				}
			}
			return result;
		}
	}
}
