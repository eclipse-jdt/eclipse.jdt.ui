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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.NullChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CopyCompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CopyPackageChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CopyPackageFragmentRootChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CopyResourceChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextFileChange;
import org.eclipse.jdt.internal.corext.refactoring.reorg.CreateCopyOfCompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ICopyQueries;
import org.eclipse.jdt.internal.corext.refactoring.reorg.INewNameQuery;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.MultiTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;

public final class CopyRefactoring2 extends Refactoring{

	private ICopyQueries fCopyQueries;
	private ICopyPolicy fCopyPolicy;

	public static boolean isAvailable(IResource[] resources, IJavaElement[] javaElements) throws JavaModelException{
		return isAvailable(CopyPolicyFactory.create(resources, javaElements));
	}
	
	public static CopyRefactoring2 create(IResource[] resources, IJavaElement[] javaElements) throws JavaModelException{
		ICopyPolicy copyPolicy= CopyPolicyFactory.create(resources, javaElements);
		if (! isAvailable(copyPolicy))
			return null;
		return new CopyRefactoring2(copyPolicy);
	}

	private static boolean isAvailable(ICopyPolicy copyPolicy) throws JavaModelException{
		return copyPolicy.canEnable();
	}
		
	private CopyRefactoring2(ICopyPolicy copyPolicy) {
		fCopyPolicy= copyPolicy;
	}
	
	public void setQueries(ICopyQueries copyQueries){
		Assert.isNotNull(copyQueries);
		fCopyQueries= copyQueries;
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
		return fCopyPolicy.setDestination(destination);
	}

	public RefactoringStatus setDestination(IResource destination) throws JavaModelException{
		return fCopyPolicy.setDestination(destination);
	}

	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		Assert.isNotNull(fCopyQueries);
		pm.beginTask("", 1);
		try {
			return Checks.validateModifiesFiles(fCopyPolicy.getAllModifiedFiles());
		} finally{
			pm.done();
		}	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		Assert.isNotNull(fCopyQueries);
		try {
			return fCopyPolicy.createChange(pm, fCopyQueries);
		} finally {
			pm.done();
		}
	}

	public String getName() {
		return "Copy";
	}
	
	private static class CopyPolicyFactory {
		public static ICopyPolicy create(IResource[] resources, IJavaElement[] javaElements){
			final ICopyPolicy NO= new NoCopyPolicy();
		
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
			
			if (hasElementsSmallerThanCu(javaElements)){
				if (resources.length != 0 ||
					ReorgUtils2.hasElementsOfType(javaElements, IJavaElement.COMPILATION_UNIT) || 
					hasElementsLargerThanCu(javaElements))
					return NO;
				return new CopySubCuElementsPolicy(javaElements);
			}
			return NO;
		}
		
		private static boolean hasElementsSmallerThanCu(IJavaElement[] javaElements) {
			for (int i= 0; i < javaElements.length; i++) {
				if (ReorgUtils2.isInsideCompilationUnit(javaElements[i]))
					return true;
			}
			return false;
		}

		private static boolean hasElementsLargerThanCu(IJavaElement[] javaElements) {
			for (int i= 0; i < javaElements.length; i++) {
				if (! ReorgUtils2.isInsideCompilationUnit(javaElements[i]))
					return true;
			}
			return false;
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
			return resource != null && resource.exists() && ! resource.isPhantom() && resource.isAccessible();
		}

		private static boolean canCopy(IJavaElement element) {
			return element != null && element.exists();
		}
	
		private static class NewNameProposer{
			private final Set fAutoGeneratedNewNames= new HashSet(2);
			
			public String createNewName(ICompilationUnit cu, IPackageFragment destination){
				if (isNewNameOk(destination, cu.getElementName()))
					return null;
				if (! ReorgUtils.isParent(cu, destination))//TODO need to do something about this
					return null;
				int i= 1;
				while (true){
					String newName;
					if (i == 1)
						newName= RefactoringCoreMessages.getFormattedString("CopyRefactoring.cu.copyOf1", //$NON-NLS-1$
									cu.getElementName());
					else	
						newName= RefactoringCoreMessages.getFormattedString("CopyRefactoring.cu.copyOfMore", //$NON-NLS-1$
									new String[]{String.valueOf(i), cu.getElementName()});
					if (isNewNameOk(destination, newName) && ! fAutoGeneratedNewNames.contains(newName)){
						fAutoGeneratedNewNames.add(newName);
						return newName;
					}
					i++;
				}
			}
	
			public String createNewName(IResource res, IContainer destination){
				if (isNewNameOk(destination, res.getName()))
					return null;
				if (! ReorgUtils.isParent(res, destination))//TODO need to do something about this
					return null;
				int i= 1;
				while (true){
					String newName;
					if (i == 1)
						newName= RefactoringCoreMessages.getFormattedString("CopyRefactoring.resource.copyOf1", //$NON-NLS-1$
									res.getName());
					else
						newName= RefactoringCoreMessages.getFormattedString("CopyRefactoring.resource.copyOfMore", //$NON-NLS-1$
									new String[]{String.valueOf(i), res.getName()});
					if (isNewNameOk(destination, newName) && ! fAutoGeneratedNewNames.contains(newName)){
						fAutoGeneratedNewNames.add(newName);
						return newName;
					}
					i++;
				}	
			}
	
			public String createNewName(IPackageFragment pack, IPackageFragmentRoot destination){
				if (isNewNameOk(destination, pack.getElementName()))
					return null;
				if (! ReorgUtils.isParent(pack, destination))//TODO need to do something about this
					return null;
				int i= 1;
				while (true){
					String newName;
					if (i == 0)
						newName= RefactoringCoreMessages.getFormattedString("CopyRefactoring.package.copyOf1", //$NON-NLS-1$
									pack.getElementName());
					else
						newName= RefactoringCoreMessages.getFormattedString("CopyRefactoring.package.copyOfMore", //$NON-NLS-1$
									new String[]{String.valueOf(i), pack.getElementName()});
					if (isNewNameOk(destination, newName) && ! fAutoGeneratedNewNames.contains(newName)){
						fAutoGeneratedNewNames.add(newName);
						return newName;
					}
					i++;
				}	
			}
			private static boolean isNewNameOk(IPackageFragment dest, String newName) {
				return ! dest.getCompilationUnit(newName).exists();
			}
	
			private static boolean isNewNameOk(IContainer container, String newName) {
				return container.findMember(newName) == null;
			}

			private static boolean isNewNameOk(IPackageFragmentRoot root, String newName) {
				return ! root.getPackageFragment(newName).exists() ;
			}
		}
		private static abstract class CopyPolicy implements ICopyPolicy{
			//invariant: only 1 of these can ever be not null
			private IResource fResourceDestination;
			private IJavaElement fJavaElementDestination;
			
			/* (non-Javadoc)
			 * @see org.eclipse.jdt.internal.corext.refactoring.reorg2.ICopyPolicy#setDestination(org.eclipse.core.resources.IResource)
			 */
			public final RefactoringStatus setDestination(IResource destination) throws JavaModelException {
				Assert.isNotNull(destination);
				resetDestinations();
				fResourceDestination= destination;
				return verifyDestination(destination);
			}
			/* (non-Javadoc)
			 * @see org.eclipse.jdt.internal.corext.refactoring.reorg2.ICopyPolicy#setDestination(org.eclipse.jdt.core.IJavaElement)
			 */
			public final RefactoringStatus setDestination(IJavaElement destination) throws JavaModelException {
				Assert.isNotNull(destination);
				resetDestinations();
				fJavaElementDestination= destination;
				return verifyDestination(destination);
			}
			protected abstract RefactoringStatus verifyDestination(IJavaElement destination) throws JavaModelException;
			protected abstract RefactoringStatus verifyDestination(IResource destination) throws JavaModelException;
			
			private void resetDestinations() {
				fJavaElementDestination= null;
				fResourceDestination= null;
			}
			protected IResource getResourceDestination(){
				return fResourceDestination;
			}
			protected IJavaElement getJavaElementDestination(){
				return fJavaElementDestination;
			}
			public IFile[] getAllModifiedFiles() {
				return new IFile[0];
			}
		}
		private static class CopyPackageFragmentRootsPolicy extends CopyPolicy{
			private final IPackageFragmentRoot[] fPackageFragmentRoots;
			public CopyPackageFragmentRootsPolicy(IPackageFragmentRoot[] roots){
				Assert.isNotNull(roots);
				fPackageFragmentRoots= roots;
			}
			public boolean canEnable() throws JavaModelException {
				for (int i= 0; i < fPackageFragmentRoots.length; i++) {
					if (! ReorgUtils2.isSourceFolder(fPackageFragmentRoots[i]))
						return false;
				}
				return true;
			}
			protected RefactoringStatus verifyDestination(IResource resource) {
				return RefactoringStatus.createFatalErrorStatus("Package fragment roots can only be moved to Java projects");
			}
			
			protected RefactoringStatus verifyDestination(IJavaElement javaElement) {
				Assert.isNotNull(javaElement);
				if (! javaElement.exists())
					return RefactoringStatus.createFatalErrorStatus("The selected destination does not exist");
				if (! (javaElement instanceof IJavaProject))
					return RefactoringStatus.createFatalErrorStatus("Package fragment roots can only be moved to Java projects");
				if (javaElement.isReadOnly())
					return RefactoringStatus.createFatalErrorStatus("Package fragment roots cannot be moved to read-only elements");
				return new RefactoringStatus();
			}
			private IJavaProject getDestinationJavaProject(){
				return (IJavaProject) getJavaElementDestination();
			}
			public IChange createChange(IProgressMonitor pm, ICopyQueries copyQueries) {
				NewNameProposer nameProposer= new NewNameProposer();
				pm.beginTask("", fPackageFragmentRoots.length);
				CompositeChange composite= new CompositeChange();
				for (int i= 0; i < fPackageFragmentRoots.length; i++) {
					composite.add(createChange(fPackageFragmentRoots[i], nameProposer, copyQueries));
					pm.worked(1);
				}
				pm.done();
				return composite;
			}
			private IChange createChange(IPackageFragmentRoot root, NewNameProposer nameProposer, ICopyQueries copyQueries) {
				IJavaProject destination= getDestinationJavaProject();
				Assert.isNotNull(destination);
				IResource res= root.getResource();
				IProject destinationProject= destination.getProject();
				String newName= nameProposer.createNewName(res, destinationProject);
				if (newName == null )
					newName= root.getElementName();
				INewNameQuery nameQuery= copyQueries.createStaticQuery(newName);
				//TODO sounds wrong that this change works on IProjects
				//TODO fix the query problem
				return new CopyPackageFragmentRootChange(root, destinationProject, nameQuery,  null);
			}
		}
		
		private static class CopyPackagesPolicy extends CopyPolicy{
			private final IPackageFragment[] fPackageFragments;
			
			public CopyPackagesPolicy(IPackageFragment[] packageFragments){
				Assert.isNotNull(packageFragments);
				fPackageFragments= packageFragments;
			}

			public boolean canEnable() throws JavaModelException {
				for (int i= 0; i < fPackageFragments.length; i++) {
					if (JavaElementUtil.isDefaultPackage(fPackageFragments[i]))
						return false;
				}
				return true;
			}

			protected RefactoringStatus verifyDestination(IResource resource) {
				Assert.isNotNull(resource);
				return RefactoringStatus.createFatalErrorStatus("Packages can only be moved to source folders or Java projects that do not have source folders");
			}
			
			protected RefactoringStatus verifyDestination(IJavaElement javaElement) throws JavaModelException {
				Assert.isNotNull(javaElement);
				IPackageFragmentRoot destRoot= getDestinationAsPackageFragmentRoot(javaElement);
				if (! ReorgUtils2.isSourceFolder(destRoot))
					return RefactoringStatus.createFatalErrorStatus("Packages can only be moved to source folders or Java projects that do not have source folders");
				return new RefactoringStatus();
			}
			
			private IPackageFragmentRoot getDestinationPackageFragmentRoot() throws JavaModelException{
				return getDestinationAsPackageFragmentRoot(getJavaElementDestination());
			}
			
			private IPackageFragmentRoot getDestinationAsPackageFragmentRoot(IJavaElement javaElement) throws JavaModelException{
				if (javaElement instanceof IPackageFragment){
					IPackageFragment pack= (IPackageFragment)javaElement;
					if (pack.getParent() instanceof IPackageFragmentRoot)
						return (IPackageFragmentRoot) pack.getParent();
				}
				if (javaElement instanceof IPackageFragmentRoot)
					return (IPackageFragmentRoot) javaElement;
				if (javaElement instanceof IJavaProject)
					return ReorgUtils2.getCorrespondingPackageFragmentRoot((IJavaProject) javaElement);				
				return null;
			}

			public IChange createChange(IProgressMonitor pm, ICopyQueries copyQueries) throws JavaModelException {
				NewNameProposer nameProposer= new NewNameProposer();
				pm.beginTask("", fPackageFragments.length);
				CompositeChange composite= new CompositeChange();
				for (int i= 0; i < fPackageFragments.length; i++) {
					composite.add(createChange(fPackageFragments[i], nameProposer, copyQueries));
					pm.worked(1);
				}
				pm.done();
				return composite;
			}

			private IChange createChange(IPackageFragment pack, NewNameProposer nameProposer, ICopyQueries copyQueries) throws JavaModelException {
				IPackageFragmentRoot root= getDestinationPackageFragmentRoot();
				String newName= nameProposer.createNewName(pack, root);
				if (newName == null || JavaConventions.validatePackageName(newName).getSeverity() < IStatus.ERROR){
					INewNameQuery nameQuery;
					if (newName == null)
						nameQuery= copyQueries.createNullQuery();
					else
						nameQuery= copyQueries.createNewPackageNameQuery(pack);
					return new CopyPackageChange(pack, root, nameQuery);
				} else {
					if (root.getResource() instanceof IContainer){
						IContainer dest= (IContainer)root.getResource();
						IResource res= pack.getResource();
						INewNameQuery nameQuery= copyQueries.createNewResourceNameQuery(res);
						return new CopyResourceChange(res, dest, nameQuery);
					}else
						return new NullChange();
				}	
			}
		}
		
		private static class CopyFilesFoldersAndCusPolicy extends CopyPolicy{
			private final ICompilationUnit[] fCus;
			private final IFolder[] fFolders;
			private final IFile[] fFiles;

			CopyFilesFoldersAndCusPolicy(IFile[] files, IFolder[] folders, ICompilationUnit[] cus){
				fFiles= files;
				fFolders= folders;
				fCus= cus;
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

			protected RefactoringStatus verifyDestination(IResource resource) throws JavaModelException {
				Assert.isNotNull(resource);
				if (! resource.exists() || resource.isPhantom())
					return RefactoringStatus.createFatalErrorStatus("The selected destination does not exist or is a phantom resource");			
				if (!resource.isAccessible())
					return RefactoringStatus.createFatalErrorStatus("The selected destination is not accessible");
				Assert.isTrue(resource.getType() != IResource.ROOT);
				
				if (isChildOfOrEqualToAnyFolder(resource))
					return RefactoringStatus.createFatalErrorStatus("The selected resource cannot be used as a destination");
				return new RefactoringStatus();
			}

			protected RefactoringStatus verifyDestination(IJavaElement javaElement) throws JavaModelException {
				Assert.isNotNull(javaElement);
				if (! javaElement.exists())
					return RefactoringStatus.createFatalErrorStatus("The selected destination does not exist");
				Assert.isTrue(! (javaElement instanceof IJavaModel));
				if (javaElement.isReadOnly())
					return RefactoringStatus.createFatalErrorStatus("The selected destination is read-only");
				if (! javaElement.isStructureKnown())
					return RefactoringStatus.createFatalErrorStatus("The structure of the selected destination is not known");
				if (javaElement instanceof IOpenable){
					IOpenable openable= (IOpenable)javaElement;
					if (! openable.isConsistent())
						return RefactoringStatus.createFatalErrorStatus("The selected destination is not consistent with its underlying resource or buffer");
				}				
				if (javaElement instanceof IPackageFragmentRoot){
					IPackageFragmentRoot root= (IPackageFragmentRoot)javaElement;
					if (root.isArchive())
						return RefactoringStatus.createFatalErrorStatus("The selected destination is an archive");
					if (root.isExternal())
						return RefactoringStatus.createFatalErrorStatus("The selected destination is external to the workbench");
				}
				if (ReorgUtils2.isInsideCompilationUnit(javaElement))
					return RefactoringStatus.createFatalErrorStatus("Elements inside compilation units cannot be used as destinations for copying files, folders or compilation units");
				return new RefactoringStatus();
			}
			public IChange createChange(IProgressMonitor pm, ICopyQueries copyQueries) {
				pm.beginTask("", fCus.length + fFiles.length + fFolders.length);
				NewNameProposer nameProposer= new NewNameProposer();
				CompositeChange composite= new CompositeChange();
				for (int i= 0; i < fCus.length; i++) {
					composite.add(createChange(fCus[i], nameProposer, copyQueries));
					pm.worked(1);
				}
				for (int i= 0; i < fFiles.length; i++) {
					composite.add(createChange(fFiles[i], nameProposer, copyQueries));
					pm.worked(1);
				}
				for (int i= 0; i < fFolders.length; i++) {
					composite.add(createChange(fFolders[i], nameProposer, copyQueries));
					pm.worked(1);
				}
				pm.done();
				return composite;
			}

			private IChange createChange(ICompilationUnit unit, NewNameProposer nameProposer, ICopyQueries copyQueries) {
				IPackageFragment pack= getDestinationAsPackageFragment();
				if (pack != null)
					return copyCuToPackage(unit, pack, nameProposer, copyQueries);
				IContainer container= getDestinationAsContainer();
				Assert.isNotNull(container);
				return copyFileToContainer(unit, container, nameProposer, copyQueries);
			}

			private IPackageFragment getDestinationAsPackageFragment() {
				IPackageFragment javaAsPackage= getJavaDestinationAsPackageFragment(getJavaElementDestination());
				if (javaAsPackage != null)
					return javaAsPackage;
				IPackageFragment resourceAsPackage= getResourceDestinationAsPackageFragment(getResourceDestination());
				return resourceAsPackage;
			}
			
			private static IPackageFragment getJavaDestinationAsPackageFragment(IJavaElement javaDest){
				if( javaDest == null || ! javaDest.exists())
					return null;					
				if (javaDest instanceof IPackageFragment)
					return (IPackageFragment) javaDest;
				if (javaDest instanceof IPackageFragmentRoot)
					return ((IPackageFragmentRoot) javaDest).getPackageFragment("");
				if (javaDest instanceof ICompilationUnit)
					return (IPackageFragment)javaDest.getParent();				
				return null;
			}
			
			private static IPackageFragment getResourceDestinationAsPackageFragment(IResource resource){
				if (resource instanceof IFile)
					return getJavaDestinationAsPackageFragment(JavaCore.create(resource.getParent()));
				return null;	
			}

			private static IChange copyFileToContainer(ICompilationUnit cu, IContainer dest, NewNameProposer nameProposer, ICopyQueries copyQueries) {
				IResource resource= ReorgUtils2.getResource(cu);
				return createCopyResourceChange(resource, nameProposer, copyQueries, dest);
			}

			private IChange createChange(IResource resource, NewNameProposer nameProposer, ICopyQueries copyQueries) {
				IContainer dest= getDestinationAsContainer();
				return createCopyResourceChange(resource, nameProposer, copyQueries, dest);
			}
			
			private static IChange createCopyResourceChange(IResource resource, NewNameProposer nameProposer, ICopyQueries copyQueries, IContainer destination) {
				INewNameQuery nameQuery;
				if (nameProposer.createNewName(resource, destination) == null)
					nameQuery= copyQueries.createNullQuery();
				else
					nameQuery= copyQueries.createNewResourceNameQuery(resource);
				return new CopyResourceChange(resource, destination, nameQuery);
			}

			private static IChange copyCuToPackage(ICompilationUnit cu, IPackageFragment dest, NewNameProposer nameProposer, ICopyQueries copyQueries) {
				//XXX workaround for bug 31998 we will have to disable renaming of linked packages (and cus)
				IResource res= ReorgUtils2.getResource(cu);
				if (res != null && res.isLinked()){
					if (ResourceUtil.getResource(dest) instanceof IContainer)
						return copyFileToContainer(cu, (IContainer)ResourceUtil.getResource(dest), nameProposer, copyQueries);
				}
		
				String newName= nameProposer.createNewName(cu, dest);
				Change simpleCopy= new CopyCompilationUnitChange(cu, dest, copyQueries.createStaticQuery(newName));
				if (newName == null || newName.equals(cu.getElementName()))
					return simpleCopy;
		
				try {
					IPath newPath= ResourceUtil.getResource(cu).getParent().getFullPath().append(newName);				
					INewNameQuery nameQuery= copyQueries.createNewCompilationUnitNameQuery(cu);
					return new CreateCopyOfCompilationUnitChange(newPath, cu.getSource(), cu, nameQuery); //XXX
				} catch(CoreException e) {
					return simpleCopy; //fallback - no ui here
				}
			}

			private IContainer getDestinationAsContainer(){
				IResource resDest= getResourceDestination();
				if (resDest != null)
					return getAsContainer(resDest);
				IJavaElement jelDest= getJavaElementDestination();
				Assert.isNotNull(jelDest);				
				return getAsContainer(ReorgUtils2.getResource(jelDest));
			}
			
			private static IContainer getAsContainer(IResource resDest){
				if (resDest instanceof IContainer)
					return (IContainer)resDest;
				if (resDest instanceof IFile)
					return (IContainer)((IFile)resDest).getParent();
				Assert.isTrue(false);//there's nothing else
				return null;				
			}
			
			public IFile[] getAllModifiedFiles() {
				if (getDestinationAsPackageFragment() != null)
					return ReorgUtils2.getFiles(ReorgUtils2.getResources(fCus));
				return new IFile[0];
			}
		}
		
		private static class CopySubCuElementsPolicy extends CopyPolicy{
			private final IJavaElement[] fJavaElements;
			
			CopySubCuElementsPolicy(IJavaElement[] javaElements){
				Assert.isNotNull(javaElements);
				fJavaElements= javaElements;
			}
			protected RefactoringStatus verifyDestination(IJavaElement destination) throws JavaModelException {
				Assert.isNotNull(destination);
				Assert.isTrue(destination.exists());
				if (! (destination instanceof ICompilationUnit) && ! ReorgUtils2.isInsideCompilationUnit(destination))
					return RefactoringStatus.createFatalErrorStatus("The selected element cannot be the destination for this copy operation");

				ICompilationUnit destinationCu= getDestinationCu(destination);
				Assert.isNotNull(destinationCu);
				if (destinationCu.isReadOnly())//the resource read-onliness is handled by validateEdit
					return RefactoringStatus.createFatalErrorStatus("The selected destination element cannot be modified");
				
				if (JavaElementUtil.getMainType(destinationCu) == null){
					if (ReorgUtils2.hasElementsOfType(fJavaElements, IJavaElement.METHOD) || 
						ReorgUtils2.hasElementsOfType(fJavaElements, IJavaElement.FIELD) || 
						ReorgUtils2.hasElementsOfType(fJavaElements, IJavaElement.INITIALIZER))
					return RefactoringStatus.createFatalErrorStatus("The selected element cannot be the destination for this copy operation");
				}
				
				if (destination instanceof IPackageDeclaration)
					return RefactoringStatus.createFatalErrorStatus("Package declarations are not available as copy destinations");
				
				if (destination instanceof IImportContainer){
					if (! canCopyAllToImportContainer(fJavaElements))
						return RefactoringStatus.createFatalErrorStatus("The selected element cannot be the destination for this copy operation");
				}
				
				if (destination instanceof IImportDeclaration){
					if (ReorgUtils2.hasElementsNotOfType(fJavaElements, IJavaElement.IMPORT_DECLARATION))
						return RefactoringStatus.createFatalErrorStatus("The selected element cannot be the destination for this copy operation");
				}
					
				return new RefactoringStatus();
			}

			private static boolean canCopyAllToImportContainer(IJavaElement[] javaElements) {
				for (int i= 0; i < javaElements.length; i++) {
					if (! (javaElements[i] instanceof IImportDeclaration))
						return false;
				}
				return true;
			}
			private static ICompilationUnit getDestinationCu(IJavaElement destination) {
				if (destination instanceof ICompilationUnit)
					return (ICompilationUnit) destination;
				return (ICompilationUnit) destination.getAncestor(IJavaElement.COMPILATION_UNIT);
			}
			
			protected RefactoringStatus verifyDestination(IResource destination) throws JavaModelException {
				return RefactoringStatus.createFatalErrorStatus("A resource cannot be the destination for the selected elements.");
			}

			public IChange createChange(IProgressMonitor pm, ICopyQueries copyQueries) throws JavaModelException {
				try {
					ICompilationUnit sourceCu= getSourceCu();
					CompilationUnit sourceCuNode= AST.parseCompilationUnit(sourceCu, false);
					ICompilationUnit destinationCu= getDestinationCu();
					CompilationUnit destinationCuNode= AST.parseCompilationUnit(destinationCu, false);
					ASTRewrite rewrite= new ASTRewrite(destinationCuNode);
					for (int i= 0; i < fJavaElements.length; i++) {
						copyToDestination(fJavaElements[i], rewrite, sourceCuNode, destinationCuNode);
					}
					return addTextEditFromRewrite(destinationCu, rewrite);
				} catch (JavaModelException e){
					throw e;
				} catch (CoreException e) {
					throw new JavaModelException(e);
				}
			}

			private void copyToDestination(IJavaElement element, ASTRewrite rewrite, CompilationUnit sourceCuNode, CompilationUnit destinationCuNode) {
				// TODO implement me
				
			}
			private ICompilationUnit getSourceCu() {
				//all have a common parent, so all must be in the same cu
				//we checked before that the array in not null and not empty
				return (ICompilationUnit) fJavaElements[0].getAncestor(IJavaElement.COMPILATION_UNIT);
			}
			
			private ICompilationUnit getDestinationCu() {
				return getDestinationCu(getJavaElementDestination());
			}
						
			private static TextChange addTextEditFromRewrite(ICompilationUnit cu, ASTRewrite rewrite) throws CoreException {
				TextBuffer textBuffer= TextBuffer.create(cu.getBuffer().getContents());
				TextEdit resultingEdits= new MultiTextEdit();
				rewrite.rewriteNode(textBuffer, resultingEdits, null);
				
				TextChange textChange= new CompilationUnitChange(cu.getElementName(), cu);
				if (textChange instanceof TextFileChange){
					TextFileChange tfc= (TextFileChange)textChange;
					tfc.setSave(! cu.isWorkingCopy());
				}
				//TODO fix the descriptions
				String message= "Copy elements to " + cu.getElementName();
				textChange.addTextEdit(message, resultingEdits);
				rewrite.removeModifications();
				return textChange;
			}

			public boolean canEnable() throws JavaModelException {
				//TODO we should be able to copy from class files too
				if (getSourceCu() == null)
					return false;
				return true;
			}
			
			public IFile[] getAllModifiedFiles() {
				return ReorgUtils2.getFiles(new IResource[]{ReorgUtils2.getResource(getDestinationCu())});
			}
		}
		private static final class NoCopyPolicy extends CopyPolicy{
			public boolean canEnable() throws JavaModelException {
				return false;
			}

			protected RefactoringStatus verifyDestination(IResource resource) throws JavaModelException {
				return RefactoringStatus.createFatalErrorStatus("Copy is not allowed");
			}

			protected RefactoringStatus verifyDestination(IJavaElement javaElement) throws JavaModelException {
				return RefactoringStatus.createFatalErrorStatus("Copy is not allowed");
			}

			public IChange createChange(IProgressMonitor pm, ICopyQueries copyQueries) {
				return new NullChange();
			}
		}
	}
}
