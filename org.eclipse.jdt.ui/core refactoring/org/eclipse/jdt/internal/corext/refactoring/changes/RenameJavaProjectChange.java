/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.changes;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.AbstractJavaElementRenameChange;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptor;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.util.Messages;

public class RenameJavaProjectChange extends AbstractJavaElementRenameChange {

	public static final String ID_RENAME_JAVA_PROJECT= "org.eclipse.jdt.ui.rename.java.project"; //$NON-NLS-1$
	private static final String ATTRIBUTE_REFERENCES= "references"; //$NON-NLS-1$

	private boolean fUpdateReferences;
	
	public RenameJavaProjectChange(IJavaProject project, String newName, String comment, boolean updateReferences) {
		this(project.getPath(), project.getElementName(), newName, comment, IResource.NULL_STAMP, updateReferences);
		Assert.isTrue(!project.isReadOnly(), "should not be read only");  //$NON-NLS-1$
	}
	
	private RenameJavaProjectChange(IPath resourcePath, String oldName, String newName, String comment, long stampToRestore, boolean updateReferences) {
		super(resourcePath, oldName, newName, comment);
		
		fUpdateReferences= updateReferences;
	}

	public String getName() {
		return Messages.format(RefactoringCoreMessages.RenameJavaProjectChange_rename, 
			 new String[]{getOldName(), getNewName()});
	}

	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		return isValid(pm, DIRTY);
	}
	
	protected void doRename(IProgressMonitor pm) throws CoreException {
		try{
			pm.beginTask(getName(), 2);
			if (fUpdateReferences)
				modifyClassPaths(new SubProgressMonitor(pm, 1));
			IProject project= getProject();
			if (project != null){
				IProjectDescription description = project.getDescription();
				description.setName(getNewName());
				project.move(description, IResource.FORCE | IResource.SHALLOW, new SubProgressMonitor(pm, 1));
			}
		} finally{
			pm.done();
		}	
	}

	protected IPath createNewPath() {
		return getResourcePath().removeLastSegments(1).append(getNewName());
	}
	
	protected Change createUndoChange(long stampToRestore) throws JavaModelException {
		return new RenameJavaProjectChange(createNewPath(), getNewName(), getOldName(), getComment(), stampToRestore, fUpdateReferences);
	}

	private IProject getProject() {
		IJavaProject jp= getJavaProject();
		if (jp == null)
			return null;
		return jp.getProject();
	}

	private IJavaProject getJavaProject() {
		return  (IJavaProject)getModifiedElement();
	}
	
	private void modifyClassPaths(IProgressMonitor pm) throws JavaModelException{
		IProject[] referencing=getReferencingProjects();
		pm.beginTask(RefactoringCoreMessages.RenameJavaProjectChange_update, referencing.length);	 
		for (int i= 0; i < referencing.length; i++) {
			IJavaProject jp= JavaCore.create(referencing[i]);
			if (jp != null && jp.exists()){
				modifyClassPath(jp, new SubProgressMonitor(pm, 1));
			}	else{
				pm.worked(1);
			}	
		}
		pm.done();		
	}
	
	private void modifyClassPath(IJavaProject referencingProject, IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 1); //$NON-NLS-1$
		IClasspathEntry[] oldEntries= referencingProject.getRawClasspath();
		IClasspathEntry[] newEntries= new IClasspathEntry[oldEntries.length];
		for (int i= 0; i < newEntries.length; i++) {
			if (isOurEntry(oldEntries[i]))
				newEntries[i]= createModifiedEntry(oldEntries[i]);
			else
				newEntries[i]= oldEntries[i];	
		}
		referencingProject.setRawClasspath(newEntries, pm);
		pm.done();
	}
	
	private boolean isOurEntry(IClasspathEntry cpe){
		if (cpe.getEntryKind() != IClasspathEntry.CPE_PROJECT)
			return false;
		if (! cpe.getPath().equals(getResourcePath()))
			return false;
		return true;	
	}
	
	private IClasspathEntry createModifiedEntry(IClasspathEntry oldEntry){
		return JavaCore.newProjectEntry(
				createNewPath(),
				oldEntry.getAccessRules(),
				oldEntry.combineAccessRules(),
				oldEntry.getExtraAttributes(),
				oldEntry.isExported()
		);
	}
	
	private IProject[] getReferencingProjects() {
		return  getProject().getReferencingProjects();
	}

	public final ChangeDescriptor getDescriptor() {
		final Map arguments= new HashMap();
		final IJavaProject project= getJavaProject();
		final JavaRefactoringDescriptor descriptor= new JavaRefactoringDescriptor(ID_RENAME_JAVA_PROJECT, project.getElementName(), Messages.format(RefactoringCoreMessages.RenameJavaProjectChange_descriptor_description, new String[] { getOldName(), getNewName()}), getComment(), arguments, RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE | RefactoringDescriptor.BREAKING_CHANGE);
		arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_INPUT, descriptor.elementToHandle(project));
		arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_NAME, getNewName());
		arguments.put(ATTRIBUTE_REFERENCES, Boolean.valueOf(fUpdateReferences).toString());
		return new RefactoringChangeDescriptor(descriptor);
	}
}
