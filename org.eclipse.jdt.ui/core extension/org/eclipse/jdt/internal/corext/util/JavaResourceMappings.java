/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

/**
 * Helper class to create resource mapping for the corrsponding Java elements
 */
public class JavaResourceMappings {
	
	private static final class JavaProjectResourceMapping extends JavaElementResourceMapping {
		private final IJavaProject fProject;
		private JavaProjectResourceMapping(IJavaProject project) {
			fProject= project;
		}
		public Object getModelObject() {
			return fProject;
		}
		public IProject[] getProjects() {
			return new IProject[] {fProject.getProject() };
		}
		public ResourceTraversal[] getTraversals(ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
			return new ResourceTraversal[] {
				new ResourceTraversal(new IResource[] {fProject.getProject()}, IResource.DEPTH_INFINITE)
			};
		}
	}
	
	private static final class PackageFragementRootResourceMapping extends JavaElementResourceMapping {
		private final IPackageFragmentRoot fRoot;
		private final IResource fResource;
		private PackageFragementRootResourceMapping(IPackageFragmentRoot root) throws CoreException {
			fRoot= root;
			fResource= root.getCorrespondingResource();
		}
		public Object getModelObject() {
			return fRoot;
		}
		public IProject[] getProjects() {
			return new IProject[] {fResource.getProject() };
		}
		public ResourceTraversal[] getTraversals(ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
			return new ResourceTraversal[] {
				new ResourceTraversal(new IResource[] {fResource}, IResource.DEPTH_INFINITE)
			};
		}
	}
	
	private static final class PackageFragmentResourceMapping extends JavaElementResourceMapping {
		private final IPackageFragment fPack;
		private final IContainer fResource;
		private PackageFragmentResourceMapping(IPackageFragment pack) throws CoreException {
			fPack= pack;
			fResource= (IContainer)pack.getCorrespondingResource();
		}
		public Object getModelObject() {
			return fPack;
		}
		public IProject[] getProjects() {
			return new IProject[] {fResource.getProject() };
		}
		public ResourceTraversal[] getTraversals(ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
			IFile[] files= getPackageContent();
			List result= new ArrayList();
			for (int i= 0; i < files.length; i++) {
				result.add(new ResourceTraversal(new IResource[] {files[i]}, IResource.DEPTH_ONE));
			}
			return (ResourceTraversal[])result.toArray(new ResourceTraversal[result.size()]);
		}
		private IFile[] getPackageContent() throws CoreException {
			List result= new ArrayList();
			if (fResource != null) {
				IResource[] members= fResource.members();
				for (int m= 0; m < members.length; m++) {
					IResource member= members[m];
					if (member instanceof IFile) {
						IFile file= (IFile)member;
						if ("class".equals(file.getFileExtension()) && file.isDerived()) //$NON-NLS-1$
							continue;
						result.add(member);
					}
				}
			}
			return (IFile[])result.toArray(new IFile[result.size()]);
		}
	}
	
	private static final class CompilationUnitResourceMapping extends JavaElementResourceMapping {
		private final ICompilationUnit fUnit;
		private final IResource fResource;
		private CompilationUnitResourceMapping(ICompilationUnit unit) throws CoreException {
			fUnit= unit;
			fResource= unit.getCorrespondingResource();
		}
		public Object getModelObject() {
			return fUnit;
		}
		public IProject[] getProjects() {
			return new IProject[] {fResource.getProject() };
		}
		public ResourceTraversal[] getTraversals(ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
			return new ResourceTraversal[] {
				new ResourceTraversal(new IResource[] {fResource}, IResource.DEPTH_INFINITE)
			};
		}
	}

	public static ResourceMapping create(final IJavaProject project) {
		return new JavaProjectResourceMapping(project);
	}
	
	public static ResourceMapping create(final IPackageFragmentRoot root) throws CoreException {
		return new PackageFragementRootResourceMapping(root);
	}
	
	public static ResourceMapping create(final IPackageFragment pack) throws CoreException {
		return new PackageFragmentResourceMapping(pack);
	}
	
	public static ResourceMapping create(final ICompilationUnit unit) throws CoreException {
		return new CompilationUnitResourceMapping(unit);
	}
}
