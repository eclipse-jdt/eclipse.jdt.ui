/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.DeleteArguments;
import org.eclipse.ltk.core.refactoring.participants.DeleteProcessor;
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.ltk.core.refactoring.participants.ValidateEditChecker;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.GetterSetterUtil;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.jdt.internal.corext.refactoring.participants.ResourceModifications;
import org.eclipse.jdt.internal.corext.refactoring.participants.ResourceProcessors;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.Resources;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public final class JavaDeleteProcessor extends DeleteProcessor {
	
	private boolean fWasCanceled;
	private boolean fSuggestGetterSetterDeletion;
	private Object[] fElements;
	private IResource[] fResources;
	private IJavaElement[] fJavaElements;
	private IReorgQueries fDeleteQueries;	

	private Change fDeleteChange;
	private boolean fDeleteSubPackages;
	
	public static final String IDENTIFIER= "org.eclipse.jdt.ui.DeleteProcessor"; //$NON-NLS-1$
	
	public JavaDeleteProcessor(Object[] elements) {
		fElements= elements;
		fResources= RefactoringAvailabilityTester.getResources(elements);
		fJavaElements= RefactoringAvailabilityTester.getJavaElements(elements);
		fSuggestGetterSetterDeletion= true;
		fDeleteSubPackages= false;
		fWasCanceled= false;
	}
	
	//---- IRefactoringProcessor ---------------------------------------------------

	public String getIdentifier() {
		return IDENTIFIER;
	}
	
	public boolean isApplicable() throws CoreException {
		if (fElements.length == 0)
			return false;
		if (fElements.length != fResources.length + fJavaElements.length)
			return false;
		for (int i= 0; i < fResources.length; i++) {
			if (!RefactoringAvailabilityTester.isDeleteAvailable(fResources[i]))
				return false;
		}
		for (int i= 0; i < fJavaElements.length; i++) {
			if (!RefactoringAvailabilityTester.isDeleteAvailable(fJavaElements[i]))
				return false;
		}
		return true;
	}
	
	public boolean needsProgressMonitor() {
		if (fResources != null && fResources.length > 0)
			return true;
		if (fJavaElements != null) {
			for (int i= 0; i < fJavaElements.length; i++) {
				int type= fJavaElements[i].getElementType();
				if (type <= IJavaElement.CLASS_FILE)
					return true;
			}
		}
		return false;
		
	}

	public String getProcessorName() {
		return RefactoringCoreMessages.DeleteRefactoring_7; 
	}
	
	public Object[] getElements() {
		return fElements;
	}
	
	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants shared) throws CoreException {
		String[] natures= getAffectedProjectNatures();
		ResourceModifications mod= new ResourceModifications();
		List collected= new ArrayList();
		for (int i= 0; i < fJavaElements.length; i++) {
			handleJavaElementDelete(collected, fJavaElements[i], natures, mod, shared);
		}
		for (int i= 0; i < fResources.length; i++) {
			handleResourceDelete(collected, fResources[i], natures, shared);
		}
		List result= new ArrayList();
		for (Iterator iter= collected.iterator(); iter.hasNext();) {
			result.addAll(Arrays.asList(ParticipantManager.loadDeleteParticipants(status, 
				this, iter.next(), 
				new DeleteArguments(), natures, shared)));
		}
		result.addAll(Arrays.asList(mod.getParticipants(status, this, natures, shared)));
		return (RefactoringParticipant[]) result.toArray(new RefactoringParticipant[result.size()]);
	}
	
	private void handleJavaElementDelete(List collected, IJavaElement element, String[] natures, ResourceModifications mod, SharableParticipants shared) throws CoreException {
		switch(element.getElementType()) {
			case IJavaElement.JAVA_MODEL:
				return;
			case IJavaElement.JAVA_PROJECT:
				collected.add(element);
				if (element.getResource() != null)
					mod.addDelete(element.getResource());
				return;
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				collected.add(element);
				IPackageFragmentRoot root= (IPackageFragmentRoot)element;
				if (!root.isArchive() && element.getResource() != null)
					mod.addDelete(element.getResource());
				return;
			case IJavaElement.PACKAGE_FRAGMENT:
				handlePackageFragmentDelete(collected, (IPackageFragment)element, mod);
				return;
			case IJavaElement.COMPILATION_UNIT:
				collected.add(element);
				IType[] types= ((ICompilationUnit)element).getTypes();
				collected.addAll(Arrays.asList(types));
				if (element.getResource() != null)
					mod.addDelete(element.getResource());
				return;
			case IJavaElement.TYPE:
				collected.add(element);
				IType type= (IType)element;
				ICompilationUnit unit= type.getCompilationUnit();
				if (type.getDeclaringType() == null && unit.getElementName().endsWith(type.getElementName())) {
					if (unit.getTypes().length == 1) {
						collected.add(unit);
						if (unit.getResource() != null)
							mod.addDelete(unit.getResource());
					}
				}
				return;
			default:
				collected.add(element);
		}
	}

	/**
	 * This method collects file and folder deletion for notifying
	 * participants. Participants will get notified of 
	 * 
	 * * deletion of the package (in any case)
	 * * deletion of files within the package if only the files are deleted without 
	 *   the package folder ("package cleaning")
	 * * deletion of the package folder if it is not only cleared and if its parent
	 *   is not removed as well.
	 *   
	 */
	private void handlePackageFragmentDelete(List collected, IPackageFragment pack, ResourceModifications mod) throws CoreException {

		// The package is reported in any case.
		collected.add(pack);
		
		final List/* <IPackageFragment */packagesToDelete= ReorgUtils.getElementsOfType(fJavaElements, IJavaElement.PACKAGE_FRAGMENT);
		
		final IContainer container= (IContainer)pack.getResource();
		if (container == null)
			return;
		
		final IResource[] members= container.members();

		/*
		 * Check whether this package is removed completely or only cleared.
		 * The default package can never be removed completely.
		 */
		if (!pack.isDefaultPackage() && canRemoveCompletely(pack, packagesToDelete)) {
			// This package is removed completely, which means its folder will be
			// deleted as well. We only notify participants of the folder deletion
			// if the parent folder of this folder will not be deleted as well:
			boolean parentIsMarked= false;
			final IPackageFragment parent= JavaElementUtil.getParentSubpackage(pack);
			if (parent != null && parent.isDefaultPackage()) {
				// Parent is the default package which will never be
				// deleted physically
				parentIsMarked= false;
			} else {
				// Parent is marked if it is in the list
				parentIsMarked= packagesToDelete.contains(parent);
			}
			
			if (parentIsMarked) {
				// Parent is marked, but is it really deleted or only cleared?
				if (canRemoveCompletely(parent, packagesToDelete)) {
					// Parent can be removed completely, so we do not add
					// this folder to the list.
				} else {
					// Parent cannot be removed completely, but as this folder
					// can be removed, we notify the participant
					mod.addDelete(container);
				}
			} else {
				// Parent will not be removed, but we will 
				mod.addDelete(container);
			}
		} else {
			// This package is only cleared because it has subpackages (=subfolders)
			// which are not deleted. As the package is only cleared, its folder
			// will not be removed and so we must notify the participant of the deleted children.
			for (int m= 0; m < members.length; m++) {
				IResource member= members[m];
				if (member instanceof IFile) {
					IFile file= (IFile)member;
					if ("class".equals(file.getFileExtension()) && file.isDerived()) //$NON-NLS-1$
						continue;
					if (!"java".equals(file.getFileExtension()) && pack.isDefaultPackage()) //$NON-NLS-1$
						continue;
					mod.addDelete(member);
				}
				if (!pack.isDefaultPackage() && member instanceof IFolder) {
					// Normally, folder children of packages are packages
					// as well, but in case they have been removed from the build
					// path, notify the participant
					IPackageFragment frag= (IPackageFragment) JavaCore.create(member);
					if (frag == null)
						mod.addDelete(member);
				}
			}
		}
	}
	
	private void handleResourceDelete(List collected, IResource element, String[] natures, SharableParticipants shared) {
		collected.add(element);
	}

	private String[] getAffectedProjectNatures() throws CoreException {
		String[] jNatures= JavaProcessors.computeAffectedNaturs(fJavaElements);
		String[] rNatures= ResourceProcessors.computeAffectedNatures(fResources);
		Set result= new HashSet();
		result.addAll(Arrays.asList(jNatures));
		result.addAll(Arrays.asList(rNatures));
		return (String[])result.toArray(new String[result.size()]);
	}

	/* 
	 * This has to be customizable because when drag and drop is performed on a field,
	 * you don't want to suggest deleting getter/setter if only the field was moved.
	 */
	public void setSuggestGetterSetterDeletion(boolean suggest){
		fSuggestGetterSetterDeletion= suggest;
	}
	
	public void setDeleteSubPackages(boolean selection) {
		fDeleteSubPackages= selection;
	}
	
	public boolean getDeleteSubPackages() {
		return fDeleteSubPackages;
	}
	
	public boolean hasSubPackagesToDelete() {
		try {
			for (int i= 0; i < fJavaElements.length; i++) {
				if (fJavaElements[i] instanceof IPackageFragment && ((IPackageFragment) fJavaElements[i]).hasSubpackages())
					return true;
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return false;
	}

	public void setQueries(IReorgQueries queries){
		Assert.isNotNull(queries);
		fDeleteQueries= queries;
	}
	
	public IJavaElement[] getJavaElementsToDelete(){
		return fJavaElements;
	}

	public boolean wasCanceled() {
		return fWasCanceled;
	}
	
	public IResource[] getResourcesToDelete(){
		return fResources;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkActivation(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		Assert.isNotNull(fDeleteQueries);//must be set before checking activation
		RefactoringStatus result= new RefactoringStatus();
		result.merge(RefactoringStatus.create(Resources.checkInSync(ReorgUtils.getNotLinked(fResources))));
		IResource[] javaResources= ReorgUtils.getResources(fJavaElements);
		result.merge(RefactoringStatus.create(Resources.checkInSync(ReorgUtils.getNotNulls(javaResources))));
		for (int i= 0; i < fJavaElements.length; i++) {
			IJavaElement element= fJavaElements[i];
			if (element instanceof IType && ((IType)element).isAnonymous()) {
				// work around for bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=44450
				// result.addFatalError("Currently, there isn't any support to delete an anonymous type.");
			}
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkInput(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException {
		pm.beginTask(RefactoringCoreMessages.DeleteRefactoring_1, 1); 
		try{
			fWasCanceled= false;
			RefactoringStatus result= new RefactoringStatus();

			recalculateElementsToDelete();

			TextChangeManager manager= new TextChangeManager();
			fDeleteChange= DeleteChangeCreator.createDeleteChange(manager, fResources, fJavaElements, getProcessorName());
			checkDirtyCompilationUnits(result);
			checkDirtyResources(result);
			ValidateEditChecker checker= (ValidateEditChecker)context.getChecker(ValidateEditChecker.class);
			IFile[] classPathFiles= getClassPathFiles();
			checker.addFiles(ResourceUtil.getFiles(manager.getAllCompilationUnits()));
			checker.addFiles(classPathFiles);
			return result;
		} catch (OperationCanceledException e) {
			fWasCanceled= true;
			throw e;
		} catch (JavaModelException e){
			throw e;
		} catch (CoreException e) {
			throw new JavaModelException(e);
		} finally{
			pm.done();
		}
	}
	
	private void checkDirtyCompilationUnits(RefactoringStatus result) throws CoreException {
		if (fJavaElements == null || fJavaElements.length == 0)
			return;
		for (int je= 0; je < fJavaElements.length; je++) {
			IJavaElement element= fJavaElements[je];
			if (element instanceof ICompilationUnit) {
				checkDirtyCompilationUnit(result, (ICompilationUnit)element);	
			} else if (element instanceof IPackageFragment) {
				ICompilationUnit[] units= ((IPackageFragment)element).getCompilationUnits();
				for (int u = 0; u < units.length; u++) {
					checkDirtyCompilationUnit(result, units[u]);
				}
			}
		}
	}
	
	private void checkDirtyCompilationUnit(RefactoringStatus result, ICompilationUnit cunit) {
		IResource resource= cunit.getResource();
		if (resource == null || resource.getType() != IResource.FILE)
			return;
		checkDirtyFile(result, (IFile)resource);
	}

	private void checkDirtyResources(final RefactoringStatus result) throws CoreException {
		for (int i= 0; i < fResources.length; i++) {
			IResource resource= fResources[i];
			resource.accept(new IResourceVisitor() {
				public boolean visit(IResource visitedResource) throws CoreException {
					if (visitedResource instanceof IFile) {
						checkDirtyFile(result, (IFile)visitedResource);
					}
					return true;
				}
			}, IResource.DEPTH_INFINITE, false);
		}
	}
	
	private void checkDirtyFile(RefactoringStatus result, IFile file) {
		if (file == null || !file.exists())
			return;
		ITextFileBuffer buffer= FileBuffers.getTextFileBufferManager().getTextFileBuffer(file.getFullPath());
		if (buffer != null && buffer.isDirty()) {
			if (buffer.isStateValidated() && buffer.isSynchronized()) {
				result.addWarning(Messages.format(
					RefactoringCoreMessages.JavaDeleteProcessor_unsaved_changes, 
					file.getFullPath().toString()));
			} else {
				result.addFatalError(Messages.format(
					RefactoringCoreMessages.JavaDeleteProcessor_unsaved_changes, 
					file.getFullPath().toString()));
			}
		}
	}

	/*
	 * The set of elements that will eventually be deleted may be very different from the set
	 * originally selected - there may be fewer, more or different elements.
	 * This method is used to calculate the set of elements that will be deleted - if necessary, 
	 * it asks the user.
	 */
	private void recalculateElementsToDelete() throws CoreException {
		//the sequence is critical here
		
		if (fDeleteSubPackages) /* add subpackages first, to allow removing elements with parents in selection etc. */
			addSubPackages();
		
		removeElementsWithParentsInSelection(); /*ask before adding empty cus - you don't want to ask if you, for example delete 
												 *the package, in which the cus live*/
		removeUnconfirmedFoldersThatContainSourceFolders(); /* a selected folder may be a parent of a source folder
															 * we must inform the user about it and ask if ok to delete the folder*/
		removeUnconfirmedReferencedArchives();
		addEmptyCusToDelete();
		removeJavaElementsChildrenOfJavaElements();/*because adding cus may create elements (types in cus)
												    *whose parents are in selection*/
		confirmDeletingReadOnly();   /*after empty cus - you want to ask for all cus that are to be deleted*/
	
		if (fSuggestGetterSetterDeletion)
			addGettersSetters();/*at the end - this cannot invalidate anything*/
		
		addDeletableParentPackagesOnPackageDeletion(); /* do not change the sequence in fJavaElements after this method */
	}

	/**
	 * Adds all subpackages of the selected packages to the list of items to be
	 * deleted.
	 * 
	 * @throws JavaModelException
	 */
	private void addSubPackages() throws JavaModelException {

		final Set javaElements= new HashSet();
		for (int i= 0; i < fJavaElements.length; i++) {
			if (fJavaElements[i] instanceof IPackageFragment) {
				javaElements.addAll(Arrays.asList(JavaElementUtil.getPackageAndSubpackages((IPackageFragment) fJavaElements[i])));
			} else {
				javaElements.add(fJavaElements[i]);
			}
		}

		fJavaElements= (IJavaElement[]) javaElements.toArray(new IJavaElement[javaElements.size()]);
	}

	/**
	 * Add deletable parent packages to the list of items to delete.
	 * 
	 * @throws CoreException
	 */
	private void addDeletableParentPackagesOnPackageDeletion() throws CoreException {

		final List/* <IPackageFragment */initialPackagesToDelete= ReorgUtils.getElementsOfType(fJavaElements, IJavaElement.PACKAGE_FRAGMENT);

		if (initialPackagesToDelete.size() == 0)
			return;

		// Move from inner to outer packages
		Collections.sort(initialPackagesToDelete, new Comparator() {
			public int compare(Object arg0, Object arg1) {
				IPackageFragment one= (IPackageFragment) arg0;
				IPackageFragment two= (IPackageFragment) arg1;
				return two.getElementName().compareTo(one.getElementName());
			}
		});

		// Get resources and java elements which will be deleted as well
		final Set/* <IResource> */deletedChildren= new HashSet();
		deletedChildren.addAll(Arrays.asList(fResources));
		for (int i= 0; i < fJavaElements.length; i++) {
			if (!ReorgUtils.isInsideCompilationUnit(fJavaElements[i]))
				deletedChildren.add(fJavaElements[i].getResource());
		}

		// new package list in the right sequence
		final List/* <IPackageFragment */allFragmentsToDelete= new ArrayList();

		for (Iterator outerIter= initialPackagesToDelete.iterator(); outerIter.hasNext();) {
			final IPackageFragment currentPackageFragment= (IPackageFragment) outerIter.next();
			
			// The package will at least be cleared
			allFragmentsToDelete.add(currentPackageFragment);

			if (canRemoveCompletely(currentPackageFragment, initialPackagesToDelete)) {
				
				final IPackageFragment parent= JavaElementUtil.getParentSubpackage(currentPackageFragment);
				if (parent != null && !parent.isDefaultPackage() && !initialPackagesToDelete.contains(parent)) {

					final List/* <IPackageFragment> */emptyParents= new ArrayList();
					addDeletableParentPackages(parent, initialPackagesToDelete, deletedChildren, emptyParents);

					// Add parents in the right sequence (inner to outer)
					allFragmentsToDelete.addAll(emptyParents);
				}
			}
		}

		// Remove resources in deleted packages; and the packages as well
		final List/* <IJavaElement> */javaElements= new ArrayList();
		for (int i= 0; i < fJavaElements.length; i++) {
			if (!(fJavaElements[i] instanceof IPackageFragment)) {
				// remove children of deleted packages
				final IPackageFragment frag= (IPackageFragment) fJavaElements[i].getAncestor(IJavaElement.PACKAGE_FRAGMENT);
				if (!allFragmentsToDelete.contains(frag))
					javaElements.add(fJavaElements[i]);
			}
		}
		// Re-add deleted packages - note the (new) sequence
		javaElements.addAll(allFragmentsToDelete);

		// Remove resources in deleted folders
		final List/* <IResource> */resources= new ArrayList();
		for (int i= 0; i < fResources.length; i++) {
			IResource parent= fResources[i];
			if (parent.getType() == IResource.FILE)
				parent= parent.getParent();
			if (!deletedChildren.contains(parent))
				resources.add(fResources[i]);
		}

		fJavaElements= (IJavaElement[]) javaElements.toArray(new IJavaElement[javaElements.size()]);
		fResources= (IResource[]) resources.toArray(new IResource[resources.size()]);
	}

	/**
	 * Returns true if this initially selected package is really deletable
	 * (if it has non-selected subpackages, it may only be cleared).
	 * 
	 */
	private boolean canRemoveCompletely(IPackageFragment pack, List packagesToDelete) throws JavaModelException {
		final IPackageFragment[] subPackages= JavaElementUtil.getPackageAndSubpackages(pack);
		for (int i= 0; i < subPackages.length; i++) {
			if (!(subPackages[i].equals(pack)) && !(packagesToDelete.contains(subPackages[i])))
				return false;
		}
		return true;
	}

	/**
	 * Adds deletable parent packages of the fragment "frag" to the list
	 * "deletableParentPackages"; also adds the resources of those packages to the
	 * set "resourcesToDelete".
	 * 
	 */
	private void addDeletableParentPackages(IPackageFragment frag, List initialPackagesToDelete, Set resourcesToDelete, List deletableParentPackages)
			throws CoreException {

		if (frag.getResource().isLinked()) {
			final IConfirmQuery query= fDeleteQueries.createYesNoQuery(RefactoringCoreMessages.JavaDeleteProcessor_confirm_linked_folder_delete, false, IReorgQueries.CONFIRM_DELETE_LINKED_PARENT);
			if (!query.confirm(Messages.format(RefactoringCoreMessages.JavaDeleteProcessor_delete_linked_folder_question, new String[] { frag.getResource().getName() })))
					return;
		}
		
		final IResource[] children= (((IContainer) frag.getResource())).members();
		for (int i= 0; i < children.length; i++) {
			// Child must be a package fragment already in the list,
			// or a resource which is deleted as well.
			if (!resourcesToDelete.contains(children[i]))
				return;
		}
		resourcesToDelete.add(frag.getResource());
		deletableParentPackages.add(frag);

		final IPackageFragment parent= JavaElementUtil.getParentSubpackage(frag);
		if (parent != null && !parent.isDefaultPackage() && !initialPackagesToDelete.contains(parent))
			addDeletableParentPackages(parent, initialPackagesToDelete, resourcesToDelete, deletableParentPackages);
	}

	// ask for confirmation of deletion of all package fragment roots that are
	// on classpaths of other projects
	private void removeUnconfirmedReferencedArchives() throws JavaModelException {
		String queryTitle= RefactoringCoreMessages.DeleteRefactoring_2; 
		IConfirmQuery query= fDeleteQueries.createYesYesToAllNoNoToAllQuery(queryTitle, true, IReorgQueries.CONFIRM_DELETE_REFERENCED_ARCHIVES);
		removeUnconfirmedReferencedPackageFragmentRoots(query);
		removeUnconfirmedReferencedArchiveFiles(query);
	}

	private void removeUnconfirmedReferencedArchiveFiles(IConfirmQuery query) throws JavaModelException, OperationCanceledException {
		List filesToSkip= new ArrayList(0);
		for (int i= 0; i < fResources.length; i++) {
			IResource resource= fResources[i];
			if (! (resource instanceof IFile))
				continue;
		
			IJavaProject project= JavaCore.create(resource.getProject());
			if (project == null || ! project.exists())
				continue;
			IPackageFragmentRoot root= project.findPackageFragmentRoot(resource.getFullPath());
			if (root == null)
				continue;
			List referencingProjects= new ArrayList(1);
			referencingProjects.add(root.getJavaProject());
			referencingProjects.addAll(Arrays.asList(JavaElementUtil.getReferencingProjects(root)));
			if (skipDeletingReferencedRoot(query, root, referencingProjects))
				filesToSkip.add(resource);
		}
		removeFromSetToDelete((IFile[]) filesToSkip.toArray(new IFile[filesToSkip.size()]));
	}

	private void removeUnconfirmedReferencedPackageFragmentRoots(IConfirmQuery query) throws JavaModelException, OperationCanceledException {
		List rootsToSkip= new ArrayList(0);
		for (int i= 0; i < fJavaElements.length; i++) {
			IJavaElement element= fJavaElements[i];
			if (! (element instanceof IPackageFragmentRoot))
				continue;
			IPackageFragmentRoot root= (IPackageFragmentRoot)element;
			List referencingProjects= Arrays.asList(JavaElementUtil.getReferencingProjects(root));
			if (skipDeletingReferencedRoot(query, root, referencingProjects))
				rootsToSkip.add(root);
		}
		removeFromSetToDelete((IJavaElement[]) rootsToSkip.toArray(new IJavaElement[rootsToSkip.size()]));
	}

	private static boolean skipDeletingReferencedRoot(IConfirmQuery query, IPackageFragmentRoot root, List referencingProjects) throws OperationCanceledException {
		if (referencingProjects.isEmpty() || root == null || ! root.exists() ||! root.isArchive())
			return false;
		String question= Messages.format(RefactoringCoreMessages.DeleteRefactoring_3, root.getElementName()); 
		return ! query.confirm(question, referencingProjects.toArray());
	}

	private void removeUnconfirmedFoldersThatContainSourceFolders() throws CoreException {
		String queryTitle= RefactoringCoreMessages.DeleteRefactoring_4; 
		IConfirmQuery query= fDeleteQueries.createYesYesToAllNoNoToAllQuery(queryTitle, true, IReorgQueries.CONFIRM_DELETE_FOLDERS_CONTAINING_SOURCE_FOLDERS);
		List foldersToSkip= new ArrayList(0);
		for (int i= 0; i < fResources.length; i++) {
			IResource resource= fResources[i];
			if (resource instanceof IFolder){
				IFolder folder= (IFolder)resource;
				if (containsSourceFolder(folder)){
					String question= Messages.format(RefactoringCoreMessages.DeleteRefactoring_5, folder.getName()); 
					if (! query.confirm(question))
						foldersToSkip.add(folder);
				}
			}
		}
		removeFromSetToDelete((IResource[]) foldersToSkip.toArray(new IResource[foldersToSkip.size()]));
	}

	private static boolean containsSourceFolder(IFolder folder) throws CoreException {
		IResource[] subFolders= folder.members();
		for (int i = 0; i < subFolders.length; i++) {
			if (! (subFolders[i] instanceof IFolder))
				continue;
			IJavaElement element= JavaCore.create(folder);
			if (element instanceof IPackageFragmentRoot)	
				return true;
			if (element instanceof IPackageFragment)	
				continue;
			if (containsSourceFolder((IFolder)subFolders[i]))
				return true;
		}
		return false;
	}

	private void removeElementsWithParentsInSelection() {
		ParentChecker parentUtil= new ParentChecker(fResources, fJavaElements);
		parentUtil.removeElementsWithAncestorsOnList(false);
		fJavaElements= parentUtil.getJavaElements();
		fResources= parentUtil.getResources();
	}

	private void removeJavaElementsChildrenOfJavaElements(){
		ParentChecker parentUtil= new ParentChecker(fResources, fJavaElements);
		parentUtil.removeElementsWithAncestorsOnList(true);
		fJavaElements= parentUtil.getJavaElements();
	}
	
	private IFile[] getClassPathFiles() {
		List result= new ArrayList();
		for (int i= 0; i < fJavaElements.length; i++) {
			IJavaElement element= fJavaElements[i];
			if (element instanceof IPackageFragmentRoot) {
				IProject project= element.getJavaProject().getProject();
				IFile classPathFile= project.getFile(".classpath"); //$NON-NLS-1$
				if (classPathFile.exists())
					result.add(classPathFile);
			}
		}
		return (IFile[])result.toArray(new IFile[result.size()]);
	}
	
	public Change createChange(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		pm.done();
		return fDeleteChange;
	}

	private void addToSetToDelete(IJavaElement[] newElements){
		fJavaElements= ReorgUtils.union(fJavaElements, newElements);		
	}
	
	private void removeFromSetToDelete(IResource[] resourcesToNotDelete) {
		fResources= ReorgUtils.setMinus(fResources, resourcesToNotDelete);
	}
	
	private void removeFromSetToDelete(IJavaElement[] elementsToNotDelete) {
		fJavaElements= ReorgUtils.setMinus(fJavaElements, elementsToNotDelete);
	}
	
	//--- getter setter related methods
	private void addGettersSetters() throws JavaModelException {
		IField[] fields= getFields(fJavaElements);
		if (fields.length == 0)
			return;
		//IField -> IMethod[]
		Map getterSetterMapping= createGetterSetterMapping(fields);
		if (getterSetterMapping.isEmpty())
			return;
		removeAlreadySelectedMethods(getterSetterMapping);
		if (getterSetterMapping.isEmpty())
			return;
			
		List gettersSettersToAdd= getGettersSettersToDelete(getterSetterMapping);
		addToSetToDelete((IMethod[]) gettersSettersToAdd.toArray(new IMethod[gettersSettersToAdd.size()]));
	}

	private List getGettersSettersToDelete(Map getterSetterMapping) {
		List gettersSettersToAdd= new ArrayList(getterSetterMapping.size());
		String queryTitle= RefactoringCoreMessages.DeleteRefactoring_8; 
		IConfirmQuery getterSetterQuery= fDeleteQueries.createYesYesToAllNoNoToAllQuery(queryTitle, true, IReorgQueries.CONFIRM_DELETE_GETTER_SETTER);
		for (Iterator iter= getterSetterMapping.keySet().iterator(); iter.hasNext();) {
			IField field= (IField) iter.next();
			Assert.isTrue(hasGetter(getterSetterMapping, field) || hasSetter(getterSetterMapping, field));
			String deleteGetterSetter= Messages.format(RefactoringCoreMessages.DeleteRefactoring_9, JavaElementUtil.createFieldSignature(field)); 
			if (getterSetterQuery.confirm(deleteGetterSetter)){
				if (hasGetter(getterSetterMapping, field))
					gettersSettersToAdd.add(getGetter(getterSetterMapping, field));
				if (hasSetter(getterSetterMapping, field))
					gettersSettersToAdd.add(getSetter(getterSetterMapping, field));
			}
		}
		return gettersSettersToAdd;
	}

	//note: modifies the mapping
	private void removeAlreadySelectedMethods(Map getterSetterMapping) {
		List elementsToDelete= Arrays.asList(fJavaElements);
		for (Iterator iter= getterSetterMapping.keySet().iterator(); iter.hasNext();) {
			IField field= (IField) iter.next();
			//remove getter
			IMethod getter= getGetter(getterSetterMapping, field);
			if (getter != null && elementsToDelete.contains(getter))
				removeGetterFromMapping(getterSetterMapping, field);

			//remove setter
			IMethod setter= getSetter(getterSetterMapping, field);
			if (setter != null && elementsToDelete.contains(setter))
				removeSetterFromMapping(getterSetterMapping, field);

			//both getter and setter already included
			if (! hasGetter(getterSetterMapping, field) && ! hasSetter(getterSetterMapping, field))
				iter.remove();
		}
	}

	/*
	 * IField -> IMethod[] (array of 2 - [getter, setter], one of which can be null)
	 */
	private static Map createGetterSetterMapping(IField[] fields) throws JavaModelException {
		Map result= new HashMap();
		for (int i= 0; i < fields.length; i++) {
			IField field= fields[i];
			IMethod[] getterSetter= getGetterSetter(field);
			if (getterSetter != null)
				result.put(field, getterSetter);
		}		
		return result;
	}
	private static boolean hasSetter(Map getterSetterMapping, IField field){
		return getterSetterMapping.containsKey(field) && 
			   getSetter(getterSetterMapping, field) != null;
	}
	private static boolean hasGetter(Map getterSetterMapping, IField field){
		return getterSetterMapping.containsKey(field) && 
			   getGetter(getterSetterMapping, field) != null;
	}
	private static void removeGetterFromMapping(Map getterSetterMapping, IField field){
		((IMethod[])getterSetterMapping.get(field))[0]= null;
	}
	private static void removeSetterFromMapping(Map getterSetterMapping, IField field){
		((IMethod[])getterSetterMapping.get(field))[1]= null;
	}
	private static IMethod getGetter(Map getterSetterMapping, IField field){
		return ((IMethod[])getterSetterMapping.get(field))[0];
	}
	private static IMethod getSetter(Map getterSetterMapping, IField field){
		return ((IMethod[])getterSetterMapping.get(field))[1];
	}
	private static IField[] getFields(IJavaElement[] elements){
		List fields= new ArrayList(3);
		for (int i= 0; i < elements.length; i++) {
			if (elements[i] instanceof IField)
				fields.add(elements[i]);
		}
		return (IField[]) fields.toArray(new IField[fields.size()]);
	}

	/*
	 * returns an array of 2 [getter, setter] or null if no getter or setter exists
	 */
	private static IMethod[] getGetterSetter(IField field) throws JavaModelException {
		IMethod getter= GetterSetterUtil.getGetter(field);
		IMethod setter= GetterSetterUtil.getSetter(field);
		if ((getter != null && getter.exists()) || 	(setter != null && setter.exists()))
			return new IMethod[]{getter, setter};
		else
			return null;
	}

	//----------- read-only confirmation business ------
	private void confirmDeletingReadOnly() throws CoreException {
		if (! ReadOnlyResourceFinder.confirmDeleteOfReadOnlyElements(fJavaElements, fResources, fDeleteQueries))
			throw new OperationCanceledException(); //saying 'no' to this one is like cancelling the whole operation
	}

	//----------- empty CUs related method
	private void addEmptyCusToDelete() throws JavaModelException {
		Set cusToEmpty= getCusToEmpty();
		addToSetToDelete((ICompilationUnit[]) cusToEmpty.toArray(new ICompilationUnit[cusToEmpty.size()]));
	}

	private Set getCusToEmpty() throws JavaModelException {
		Set result= new HashSet();
		for (int i= 0; i < fJavaElements.length; i++) {
			IJavaElement element= fJavaElements[i];	
			ICompilationUnit cu= ReorgUtils.getCompilationUnit(element);
			if (cu != null && ! result.contains(cu) && willHaveAllTopLevelTypesDeleted(cu))
				result.add(cu);
		}
		return result;
	}

	private boolean willHaveAllTopLevelTypesDeleted(ICompilationUnit cu) throws JavaModelException {
		Set elementSet= new HashSet(Arrays.asList(fJavaElements));
		IType[] topLevelTypes= cu.getTypes();
		for (int i= 0; i < topLevelTypes.length; i++) {
			if (! elementSet.contains(topLevelTypes[i]))
				return false;
		}
		return true;
	}
}
