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
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * An abstract super class to describe mappings from a Java element to a
 * set of resources. The class also provides factory methods to create
 * resource mappings.
 * 
 * @since 3.1
 */
public abstract class JavaElementResourceMapping extends ResourceMapping {
	
	/* package */ JavaElementResourceMapping() {
	}
	
	public IJavaElement getJavaElement() {
		Object o= getModelObject();
		if (o instanceof IJavaElement)
			return (IJavaElement)o;
		return null;
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof JavaElementResourceMapping))
			return false;
		return getJavaElement().equals(((JavaElementResourceMapping)obj).getJavaElement());
	}
	
	public int hashCode() {
		return getJavaElement().hashCode();
	}
	
	//---- the factory code ---------------------------------------------------------------
	
	private static final class JavaModelResourceMapping extends JavaElementResourceMapping {
		private final IJavaModel fJavaModel;
		private JavaModelResourceMapping(IJavaModel model) {
			Assert.isNotNull(model);
			fJavaModel= model;
		}
		public Object getModelObject() {
			return fJavaModel;
		}
		public IProject[] getProjects() {
			IJavaProject[] projects= null;
			try {
				projects= fJavaModel.getJavaProjects();
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
				return new IProject[0];
			}
			IProject[] result= new IProject[projects.length];
			for (int i= 0; i < projects.length; i++) {
				result[i]= projects[i].getProject();
			}
			return result;
		}
		public ResourceTraversal[] getTraversals(ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
			IJavaProject[] projects= fJavaModel.getJavaProjects();
			ResourceTraversal[] result= new ResourceTraversal[projects.length];
			for (int i= 0; i < projects.length; i++) {
				result[i]= new ResourceTraversal(new IResource[] {projects[i].getProject()}, IResource.DEPTH_INFINITE, 0);
			}
			return result;
		}
	}
	
	private static final class JavaProjectResourceMapping extends JavaElementResourceMapping {
		private final IJavaProject fProject;
		private JavaProjectResourceMapping(IJavaProject project) {
			Assert.isNotNull(project);
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
				new ResourceTraversal(new IResource[] {fProject.getProject()}, IResource.DEPTH_INFINITE, 0)
			};
		}
	}
	
	private static final class PackageFragementRootResourceMapping extends JavaElementResourceMapping {
		private final IPackageFragmentRoot fRoot;
		private PackageFragementRootResourceMapping(IPackageFragmentRoot root) {
			Assert.isNotNull(root);
			fRoot= root;
		}
		public Object getModelObject() {
			return fRoot;
		}
		public IProject[] getProjects() {
			return new IProject[] {fRoot.getJavaProject().getProject() };
		}
		public ResourceTraversal[] getTraversals(ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
			return new ResourceTraversal[] {
				new ResourceTraversal(new IResource[] {fRoot.getCorrespondingResource()}, IResource.DEPTH_INFINITE, 0)
			};
		}
	}
	
	private static final class TeamPackageFragmentResourceMapping extends JavaElementResourceMapping {
		private final IPackageFragment fPack;
		private TeamPackageFragmentResourceMapping(IPackageFragment pack) {
			Assert.isNotNull(pack);
			fPack= pack;
		}
		public Object getModelObject() {
			return fPack;
		}
		public IProject[] getProjects() {
			return new IProject[] { fPack.getJavaProject().getProject() };
		}
		public ResourceTraversal[] getTraversals(ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
			return new ResourceTraversal[] {
				new ResourceTraversal(new IResource[] {fPack.getCorrespondingResource()}, IResource.DEPTH_ONE, 0)
			};
		}
	}
	
	private static final class PackageFragmentResourceMapping extends JavaElementResourceMapping {
		private final IPackageFragment fPack;
		private PackageFragmentResourceMapping(IPackageFragment pack) {
			Assert.isNotNull(pack);
			fPack= pack;
		}
		public Object getModelObject() {
			return fPack;
		}
		public IProject[] getProjects() {
			return new IProject[] { fPack.getJavaProject().getProject() };
		}
		public ResourceTraversal[] getTraversals(ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
			return new ResourceTraversal[] {
				new ResourceTraversal(new IResource[] {fPack.getCorrespondingResource()}, IResource.DEPTH_ONE, 0)
			};
		}
		public void accept(ResourceMappingContext context, IResourceVisitor visitor, IProgressMonitor monitor) throws CoreException {
			if (context != null) {
				super.accept(context, visitor, monitor);
			}
			// If we don't have a context then we assume that we have a precise iteration.
			IFile[] files= getPackageContent();
			if (monitor == null)
				monitor= new NullProgressMonitor();
			monitor.beginTask("", files.length + 1); //$NON-NLS-1$
			visitor.visit(fPack.getCorrespondingResource());
			monitor.worked(1);
			for (int i= 0; i < files.length; i++) {
				visitor.visit(files[i]);
				monitor.worked(1);
			}
		}
		/* package */ IFile[] getPackageContent() throws CoreException {
			List result= new ArrayList();
			IContainer container= (IContainer)fPack.getCorrespondingResource();
			if (container != null) {
				IResource[] members= container.members();
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
		private CompilationUnitResourceMapping(ICompilationUnit unit) {
			Assert.isNotNull(unit);
			fUnit= unit;
		}
		public Object getModelObject() {
			return fUnit;
		}
		public IProject[] getProjects() {
			return new IProject[] {fUnit.getJavaProject().getProject() };
		}
		public ResourceTraversal[] getTraversals(ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
			return new ResourceTraversal[] {
				new ResourceTraversal(new IResource[] {fUnit.getCorrespondingResource()}, IResource.DEPTH_ONE, 0)
			};
		}
	}

	private static final class ClassFileResourceMapping extends JavaElementResourceMapping {
		private final IClassFile fClassFile;
		private ClassFileResourceMapping(IClassFile classFile) {
			fClassFile= classFile;
		}
		public Object getModelObject() {
			return fClassFile;
		}
		public IProject[] getProjects() {
			return new IProject[] { fClassFile.getJavaProject().getProject() };
		}
		public ResourceTraversal[] getTraversals(ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
			return new ResourceTraversal[] {
				new ResourceTraversal(new IResource[] {fClassFile.getCorrespondingResource()}, IResource.DEPTH_ONE, 0)
			};
		}
	}
	
	public static ResourceMapping create(IJavaElement element) {
		switch (element.getElementType()) {
			case IJavaElement.TYPE:
				return create((IType)element);
			case IJavaElement.COMPILATION_UNIT:
				return create((ICompilationUnit)element);
			case IJavaElement.CLASS_FILE:
				return create((IClassFile)element);
			case IJavaElement.PACKAGE_FRAGMENT:
				return create((IPackageFragment)element);
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				return create((IPackageFragmentRoot)element);
			case IJavaElement.JAVA_PROJECT:
				return create((IJavaProject)element);
			case IJavaElement.JAVA_MODEL:
				return create((IJavaModel)element);
			default:
				return null;
		}		
		
	}

	public static ResourceMapping create(final IJavaModel model) {
		return new JavaModelResourceMapping(model);
	}
	
	public static ResourceMapping create(final IJavaProject project) {
		return new JavaProjectResourceMapping(project);
	}
	
	public static ResourceMapping create(final IPackageFragmentRoot root) {
		return new PackageFragementRootResourceMapping(root);
	}
	
	public static ResourceMapping create(final IPackageFragment pack) {
		// test if in an archive
		IPackageFragmentRoot root= (IPackageFragmentRoot)pack.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
		if (!root.isArchive()) {
			return new PackageFragmentResourceMapping(pack);
		}
		return null;
	}
	
	public static ResourceMapping create(ICompilationUnit unit) {
		unit= JavaModelUtil.toOriginal(unit);
		if (unit == null)
			return null;
		return new CompilationUnitResourceMapping(unit);
	}
	
	public static ResourceMapping create(IClassFile classFile) {
		// test if in a archive
		IPackageFragmentRoot root= (IPackageFragmentRoot)classFile.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
		if (!root.isArchive()) {
			return new ClassFileResourceMapping(classFile);
		}
		return null;
	}
	
	public static ResourceMapping create(IType type) {
		// top level types behave like the CU
		IJavaElement parent= type.getParent();
		if (parent instanceof ICompilationUnit) {
			return create((ICompilationUnit)parent);
		}
		return null;
	}
}
