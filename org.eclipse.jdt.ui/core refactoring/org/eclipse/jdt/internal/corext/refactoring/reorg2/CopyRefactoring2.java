/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.reorg2;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.NullChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;

public class CopyRefactoring2 extends Refactoring{

	//invariant: only 1 of these can ever be not null
	private IJavaElement fJavaElementDestination;
	private IResource fResourceDestination;

	private ICopyPolicy fCopyPolicy;
	private IJavaElement[] fJavaElements;
	private IResource[] fResources;

	public static boolean isAvailable(IResource[] resources, IJavaElement[] javaElements) throws JavaModelException{
		return isAvailable(CopyPolicyFactory.create(resources, javaElements));
	}
	
	public static CopyRefactoring2 create(IResource[] resources, IJavaElement[] javaElements) throws JavaModelException{
		ICopyPolicy copyPolicy= CopyPolicyFactory.create(resources, javaElements);
		if (! isAvailable(copyPolicy))
			return null;
		return new CopyRefactoring2(resources, javaElements, copyPolicy);
	}

	private static boolean isAvailable(ICopyPolicy copyPolicy) throws JavaModelException{
		return copyPolicy.canEnable();
	}
		
	private CopyRefactoring2(IResource[] resources, IJavaElement[] javaElements, ICopyPolicy copyPolicy) {
		Assert.isNotNull(resources);
		Assert.isNotNull(javaElements);
		fResources= resources;
		fJavaElements= javaElements;
		fCopyPolicy= copyPolicy;
	}

	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);
		try {
			return new RefactoringStatus();
		} finally {
			pm.done();
		}
	}
	
	public RefactoringStatus setDestination(IJavaElement destination) throws JavaModelException{
		Assert.isNotNull(destination);
		resetDestinations();
		fJavaElementDestination= destination;
		if (fCopyPolicy.canCopyTo(destination))
			return new RefactoringStatus();
		else
			//TODO do we need better/more specific messages?
			return RefactoringStatus.createFatalErrorStatus("The selected elements cannot be copied to the specified destination");
	}

	public RefactoringStatus setDestination(IResource destination) throws JavaModelException{
		Assert.isNotNull(destination);
		resetDestinations();
		fResourceDestination= destination;
		if (fCopyPolicy.canCopyTo(destination))
			return new RefactoringStatus();
		else
			//TODO do we need better/more specific messages?
			return RefactoringStatus.createFatalErrorStatus("The selected elements cannot be copied to the specified destination");
	}

	private void resetDestinations() {
		fJavaElementDestination= null;
		fResourceDestination= null;
	}
	
	private void checkInvariant(){
		Assert.isTrue(fJavaElementDestination == null || fResourceDestination == null);
	}
	
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		checkInvariant();
		pm.beginTask("", 1);
		try {
			return new RefactoringStatus();
		} finally {
			pm.done();
		}
	}

	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		checkInvariant();
		pm.beginTask("", 1);
		try {
			return new NullChange();
		} finally {
			pm.done();
		}
	}

	public String getName() {
		return "Copy";
	}
	
	private static class CopyPolicyFactory {
		private static final ICopyPolicy NO= new NoCopyPolicy();
		
		public static ICopyPolicy create(IResource[] resources, IJavaElement[] javaElements){
			if (  isNothingToCopy(resources, javaElements) || 
				! canCopyAll(resources, javaElements) ||
				! haveCommonParent(resources, javaElements) ||
				ReorgUtils2.hasElementsOfType(javaElements, IJavaElement.JAVA_PROJECT) ||
				ReorgUtils2.hasElementsOfType(javaElements, IJavaElement.JAVA_MODEL) ||
				ReorgUtils2.hasElementsOfType(resources, IResource.PROJECT | IResource.ROOT))
				return NO;
				
			if (ReorgUtils2.hasElementsOfType(javaElements, IJavaElement.PACKAGE_FRAGMENT)){
				if (resources.length != 0 || ReorgUtils2.hasElementsNotOfType(javaElements, IJavaElement.PACKAGE_FRAGMENT))
					return NO;
				else	
					return new CopyPackagesPolicy(convertToPackageArray(javaElements));
			}
			
			if (ReorgUtils2.hasElementsOfType(javaElements, IJavaElement.PACKAGE_FRAGMENT_ROOT)){
				if (resources.length != 0 || ReorgUtils2.hasElementsNotOfType(javaElements, IJavaElement.PACKAGE_FRAGMENT_ROOT))
					return NO;
				else	
					return new CopyPackageFragmentRootsPolicy(convertToPackageFragmentRootArray(javaElements));				
			}
			
			if (ReorgUtils2.hasElementsOfType(resources, IResource.FILE | IResource.FOLDER) || ReorgUtils2.hasElementsOfType(javaElements, IJavaElement.COMPILATION_UNIT)){
				if (ReorgUtils2.hasElementsNotOfType(javaElements, IJavaElement.COMPILATION_UNIT))
					return NO;
				if (ReorgUtils2.hasElementsNotOfType(resources, IResource.FILE | IResource.FOLDER))
					return NO;
				return new CopyFilesFoldersAndCusPolicy(ReorgUtils2.getFiles(resources), ReorgUtils2.getFolders(resources), convertToCompilationUnitArray(javaElements));
			}
			return new NoCopyPolicy();
		}
		
		private static ICompilationUnit[] convertToCompilationUnitArray(IJavaElement[] javaElements) {
			List result= Arrays.asList(javaElements);
			return (ICompilationUnit[]) result.toArray(new ICompilationUnit[result.size()]);
		}
		
		private static IPackageFragment[] convertToPackageArray(IJavaElement[] javaElements) {
			List result= Arrays.asList(javaElements);
			return (IPackageFragment[]) result.toArray(new IPackageFragment[result.size()]);
		}
		
		private static IPackageFragmentRoot[] convertToPackageFragmentRootArray(IJavaElement[] javaElements) {
			List result= Arrays.asList(javaElements);
			return (IPackageFragmentRoot[]) result.toArray(new IPackageFragmentRoot[result.size()]);
		}

		private static boolean haveCommonParent(IResource[] resources, IJavaElement[] javaElements) {
			return new ParentChecker(resources, javaElements).haveCommonParent();
		}

		private static boolean isNothingToCopy(IResource[] resources, IJavaElement[] javaElements) {
			return resources.length + javaElements.length == 0;
		}

		private static boolean canCopyAll(IResource[] resources, IJavaElement[] javaElements) {
			for (int i= 0; i < resources.length; i++) {
				if (! canCopy(resources[i])) return false;
			}
			for (int i= 0; i < javaElements.length; i++) {
				if (! canCopy(javaElements[i])) return false;
			}
			return true;
		}

		private static boolean canCopy(IResource resource) {
			return resource != null && resource.exists() && ! resource.isPhantom();
		}

		private static boolean canCopy(IJavaElement element) {
			return element != null && element.exists();
		}

		private static class CopyPackageFragmentRootsPolicy implements ICopyPolicy{
			private final IPackageFragmentRoot[] fPackageFragmentRoots;
			public CopyPackageFragmentRootsPolicy(IPackageFragmentRoot[] roots){
				Assert.isNotNull(roots);
				fPackageFragmentRoots= roots;
			}
			public boolean canCopyTo(IJavaElement javaElement) {
				if (javaElement == null || ! javaElement.exists())
					return false;
				return ! javaElement.isReadOnly() && javaElement instanceof IJavaProject;
			}

			public boolean canCopyTo(IResource resource) {
				return false;
			}

			public boolean canEnable() throws JavaModelException {
				for (int i= 0; i < fPackageFragmentRoots.length; i++) {
					if (! ReorgUtils2.isSourceFolder(fPackageFragmentRoots[i]))
						return false;
				}
				return true;
			}
		}
		
		private static class CopyPackagesPolicy implements ICopyPolicy{
			private final IPackageFragment[] fPackageFragments;
			
			public CopyPackagesPolicy(IPackageFragment[] packageFragments){
				Assert.isNotNull(packageFragments);
				fPackageFragments= packageFragments;
			}
			/* (non-Javadoc)
			 * @see org.eclipse.jdt.internal.corext.refactoring.reorg2.ICopyPolicy#canCopyTo(org.eclipse.jdt.core.IJavaElement)
			 */
			public boolean canCopyTo(IJavaElement javaElement) throws JavaModelException {
				if (javaElement == null || ! javaElement.exists())
					return false;
				return !javaElement.isReadOnly() && ReorgUtils2.isSourceFolder(javaElement);
			}

			public boolean canCopyTo(IResource resource) {
				return false;
			}

			public boolean canEnable() throws JavaModelException {
				for (int i= 0; i < fPackageFragments.length; i++) {
					if (JavaElementUtil.isDefaultPackage(fPackageFragments[i]))
						return false;
				}
				return true;
			}
		}
		
		private static class CopyFilesFoldersAndCusPolicy implements ICopyPolicy{
			private final ICompilationUnit[] fCus;
			private final IFolder[] fFolders;
			private final IFile[] fFiles;

			CopyFilesFoldersAndCusPolicy(IFile[] files, IFolder[] folders, ICompilationUnit[] cus){
				fFiles= files;
				fFolders= folders;
				fCus= cus;
			}

			public boolean canCopyTo(IJavaElement javaElement) throws JavaModelException {
				if (javaElement == null || ! javaElement.exists())
					return false;
				if (javaElement instanceof IJavaModel)
					return false;
				if (javaElement.isReadOnly())
					return false;
				if (! javaElement.isStructureKnown())
					return false;
				if (javaElement instanceof IOpenable){
					IOpenable openable= (IOpenable)javaElement;
					if (! openable.isConsistent())
						return false;
				}				
				if (javaElement instanceof IPackageFragmentRoot){
					IPackageFragmentRoot root= (IPackageFragmentRoot)javaElement;
					if (root.isArchive() || root.isExternal())
						return false;
				}
				if (ReorgUtils2.isInsideCompilationUnit(javaElement))
					return false;
				return true;
			}

			public boolean canCopyTo(IResource resource) {
				if (resource == null || ! resource.exists() || resource.isPhantom())
					return false;
				if (resource.getType() == IResource.ROOT)
					return false;
				if (isChildOfOrEqualToAnyFolder(resource))
					return false;
				return true;
			}

			private boolean isChildOfOrEqualToAnyFolder(IResource resource) {
				for (int i= 0; i < fFolders.length; i++) {
					IFolder folder= fFolders[i];
					if (folder.equals(resource) || ParentChecker.isChildOf(resource, folder))
						return true;
				}
				return false;
			}

			public boolean canEnable() throws JavaModelException {
				for (int i= 0; i < fCus.length; i++) {
					ICompilationUnit cu= fCus[i];
					IResource res= cu.getResource();
					if (res == null || ! res.exists())
						return false;
				}
				return true;
			}
		}
		
		private static final class NoCopyPolicy implements ICopyPolicy{
			public boolean canCopyTo(IJavaElement javaElement) {
				return false;
			}

			public boolean canCopyTo(IResource resource) {
				return false;
			}

			public boolean canEnable() throws JavaModelException {
				return false;
			}
		}
	}
}
