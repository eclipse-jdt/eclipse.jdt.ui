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
package org.eclipse.jdt.ui.tests.refactoring.infra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.eclipse.team.core.RepositoryProvider;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.team.FileModificationValidationContext;
import org.eclipse.core.resources.team.FileModificationValidator;

public class RefactoringTestRepositoryProvider extends RepositoryProvider {

	public static final String PROVIDER_ID= "org.eclipse.jdt.ui.tests.refactoring.refactoringTestRepositoryProvider";

	private static HashMap/*IProject, RefactoringTestFileModificationValidator*/ fgValidators= new HashMap();

	public static Collection getValidatedEditPaths(IProject project) {
		RefactoringTestFileModificationValidator validator= (RefactoringTestFileModificationValidator) fgValidators.get(project);
		if (validator != null) {
			return validator.getValidatedEditPaths();
		}
		return null;
	}

	public static Collection getValidatedSavePaths(IProject project) {
		RefactoringTestFileModificationValidator validator= (RefactoringTestFileModificationValidator) fgValidators.get(project);
		if (validator != null) {
			return validator.getValidatedSavePaths();
		}
		return null;
	}

	private static class RefactoringTestFileModificationValidator extends FileModificationValidator {

		private ArrayList fValidatedEditPaths;
		private ArrayList fValidatedSavePaths;

		public RefactoringTestFileModificationValidator() {
			fValidatedEditPaths= new ArrayList();
			fValidatedSavePaths= new ArrayList();
		}

		public Collection/*IPath*/ getValidatedEditPaths() {
			return fValidatedEditPaths;
		}

		public Collection/*IPath*/ getValidatedSavePaths() {
			return fValidatedSavePaths;
		}

		public IStatus validateEdit(IFile[] files, FileModificationValidationContext context) {
			for (int i= 0; i < files.length; i++) {
				fValidatedEditPaths.add(files[i].getFullPath());
			}
			return makeWritable(files);
		}

		public IStatus validateSave(IFile file) {
			fValidatedSavePaths.add(file.getFullPath());
			return makeWritable(new IFile[] { file });
		}

		private IStatus makeWritable(final IFile[] resources) {
			try {
				ResourcesPlugin.getWorkspace().run(
					new IWorkspaceRunnable() {
						public void run(IProgressMonitor monitor) throws CoreException	{
							for (int i= 0; i < resources.length; i++) {
								IFile resource= resources[i];
								ResourceAttributes resourceAttributes = resource.getResourceAttributes();
								if (resourceAttributes != null) {
									resourceAttributes.setReadOnly(false);
									resource.setResourceAttributes(resourceAttributes);
								}
							}
						}
					},
					null);
			} catch (CoreException e) {
				e.printStackTrace();
				return e.getStatus();
			}
			return Status.OK_STATUS;
		}
	}

	private RefactoringTestFileModificationValidator fValidator;

	public RefactoringTestRepositoryProvider() {
		fValidator= new RefactoringTestFileModificationValidator();
	}

	public String getID() {
		return PROVIDER_ID;
	}

	public FileModificationValidator getFileModificationValidator2() {
		return fValidator;
	}

	public void configureProject() throws CoreException {
		fgValidators.put(getProject(), fValidator);
	}

	public void deconfigure() throws CoreException {
		fgValidators.remove(getProject());
	}

}
