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
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.GetterSetterUtil;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.jdt.internal.corext.refactoring.participants.ResourceModifications;
import org.eclipse.jdt.internal.corext.refactoring.participants.ResourceProcessors;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.Resources;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.DeleteArguments;
import org.eclipse.ltk.core.refactoring.participants.DeleteProcessor;
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.ltk.core.refactoring.participants.ValidateEditChecker;

public class JavaDeleteProcessor extends DeleteProcessor {
	
	private boolean fWasCanceled;
	private boolean fSuggestGetterSetterDeletion;
	private Object[] fElements;
	private IResource[] fResources;
	private IJavaElement[] fJavaElements;
	private IReorgQueries fDeleteQueries;	

	private Change fDeleteChange;
	
	public static final String IDENTIFIER= "org.eclipse.jdt.ui.DeleteProcessor"; //$NON-NLS-1$
	
	public JavaDeleteProcessor(Object[] elements) {
		fElements= elements;
		fResources= getResources(elements);
		fJavaElements= getJavaElements(elements);
		fSuggestGetterSetterDeletion= true;
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
			if (!canDelete(fResources[i]))
				return false;
		}
		for (int i= 0; i < fJavaElements.length; i++) {
			if (!canDelete(fJavaElements[i]))
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

	private boolean canDelete(IResource resource) {
		if (!resource.exists() || resource.isPhantom())
			return false;
		if (resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT)
			return false;
		return true;
	}
	
	private boolean canDelete(IJavaElement element) throws CoreException {
		if (! element.exists())
			return false;
		
		if (element instanceof IJavaModel || element instanceof IJavaProject)
			return false;
		
		if (element.getParent() != null && element.getParent().isReadOnly())
			return false;
		
		if (element instanceof IPackageFragmentRoot){
			IPackageFragmentRoot root= (IPackageFragmentRoot)element;
			if (root.isExternal() || Checks.isClasspathDelete(root)) //TODO rename isClasspathDelete
				return false;
		}
		if (element instanceof IPackageFragment && isEmptySuperPackage((IPackageFragment)element))
			return false;
		
		if (isFromExternalArchive(element))
			return false;
					
		if (element instanceof IMember && ((IMember)element).isBinary())
			return false;
		
		if (ReorgUtils.isDeletedFromEditor(element))
			return false;								
				
		return true;
	}
	
	private static boolean isFromExternalArchive(IJavaElement element) {
		return element.getResource() == null && ! isWorkingCopyElement(element);
	}
	
	private static boolean isWorkingCopyElement(IJavaElement element) {
		if (element instanceof ICompilationUnit) 
			return ((ICompilationUnit)element).isWorkingCopy();
		if (ReorgUtils.isInsideCompilationUnit(element))
			return ReorgUtils.getCompilationUnit(element).isWorkingCopy();
		return false;
	}

	private static boolean isEmptySuperPackage(IPackageFragment pack) throws JavaModelException {
		return  pack.hasSubpackages() &&
				pack.getNonJavaResources().length == 0 &&
				pack.getChildren().length == 0;
	}	
	
	
	public String getProcessorName() {
		return RefactoringCoreMessages.getString("DeleteRefactoring.7"); //$NON-NLS-1$
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
				handlePackageFragmentDelete(collected, (IPackageFragment)element, natures, mod, shared);
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

	private void handlePackageFragmentDelete(List collected, IPackageFragment pack, String[] natures, ResourceModifications mod, SharableParticipants shared) throws CoreException {
		collected.add(pack);
		IContainer container= (IContainer)pack.getResource();
		if (container == null)
			return;
		IResource[] members= container.members();
		int files= 0;
		for (int m= 0; m < members.length; m++) {
			IResource member= members[m];
			if (member instanceof IFile) {
				files++;
				IFile file= (IFile)member;
				if ("class".equals(file.getFileExtension()) && file.isDerived()) //$NON-NLS-1$
					continue;
				mod.addDelete(member);
			}
		}
		if (files == members.length) {
			mod.addDelete(container);
		}
	}
	
	private void handleResourceDelete(List collected, IResource element, String[] natures, SharableParticipants shared) throws CoreException {
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

	private static IResource[] getResources(Object[] elements) {
		List result= new ArrayList();
		for (int i= 0; i < elements.length; i++) {
			if (elements[i] instanceof IResource)
				result.add(elements[i]);
		}
		return (IResource[])result.toArray(new IResource[result.size()]);
	}
	
	private static IJavaElement[] getJavaElements(Object[] elements) {
		List result= new ArrayList();
		for (int i= 0; i < elements.length; i++) {
			if (elements[i] instanceof IJavaElement)
				result.add(elements[i]);
		}
		return (IJavaElement[])result.toArray(new IJavaElement[result.size()]);
	}
	
	/* 
	 * This has to be customizable because when drag and drop is performed on a field,
	 * you don't want to suggest deleting getter/setter if only the field was moved.
	 */
	public void setSuggestGetterSetterDeletion(boolean suggest){
		fSuggestGetterSetterDeletion= suggest;
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
		pm.beginTask(RefactoringCoreMessages.getString("DeleteRefactoring.1"), 1); //$NON-NLS-1$
		try{
			fWasCanceled= false;
			RefactoringStatus result= new RefactoringStatus();

			recalculateElementsToDelete();

			TextChangeManager manager= new TextChangeManager();
			fDeleteChange= DeleteChangeCreator.createDeleteChange(manager, fResources, fJavaElements, getProcessorName());
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
	
	/*
	 * The set of elements that will eventually be deleted may be very different from the set
	 * originally selected - there may be fewer, more or different elements.
	 * This method is used to calculate the set of elements that will be deleted - if necessary, 
	 * it asks the user.
	 */
	private void recalculateElementsToDelete() throws CoreException {
		//the sequence is critical here
		
		fJavaElements= ReorgUtils.toWorkingCopies(fJavaElements);
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
	}
	
	//ask for confirmation of deletion of all package fragment roots that are on classpaths of other projects
	private void removeUnconfirmedReferencedArchives() throws JavaModelException {
		String queryTitle= RefactoringCoreMessages.getString("DeleteRefactoring.2"); //$NON-NLS-1$
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
		String question= RefactoringCoreMessages.getFormattedString("DeleteRefactoring.3", root.getElementName()); //$NON-NLS-1$
		return ! query.confirm(question, referencingProjects.toArray());
	}

	private void removeUnconfirmedFoldersThatContainSourceFolders() throws CoreException {
		String queryTitle= RefactoringCoreMessages.getString("DeleteRefactoring.4"); //$NON-NLS-1$
		IConfirmQuery query= fDeleteQueries.createYesYesToAllNoNoToAllQuery(queryTitle, true, IReorgQueries.CONFIRM_DELETE_FOLDERS_CONTAINING_SOURCE_FOLDERS);
		List foldersToSkip= new ArrayList(0);
		for (int i= 0; i < fResources.length; i++) {
			IResource resource= fResources[i];
			if (resource instanceof IFolder){
				IFolder folder= (IFolder)resource;
				if (containsSourceFolder(folder)){
					String question= RefactoringCoreMessages.getFormattedString("DeleteRefactoring.5", folder.getName()); //$NON-NLS-1$
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
		String queryTitle= RefactoringCoreMessages.getString("DeleteRefactoring.8"); //$NON-NLS-1$
		IConfirmQuery getterSetterQuery= fDeleteQueries.createYesYesToAllNoNoToAllQuery(queryTitle, true, IReorgQueries.CONFIRM_DELETE_GETTER_SETTER);
		for (Iterator iter= getterSetterMapping.keySet().iterator(); iter.hasNext();) {
			IField field= (IField) iter.next();
			Assert.isTrue(hasGetter(getterSetterMapping, field) || hasSetter(getterSetterMapping, field));
			String deleteGetterSetter= RefactoringCoreMessages.getFormattedString("DeleteRefactoring.9", JavaElementUtil.createFieldSignature(field)); //$NON-NLS-1$
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