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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

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
import org.eclipse.jdt.core.IWorkingCopy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.GetterSetterUtil;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;

public class DeleteRefactoring2 extends Refactoring{
	
	private boolean fWasCanceled;
	private boolean fSuggestGetterSetterDeletion;
	private IResource[] fResources;
	private IJavaElement[] fJavaElements;
	private IReorgQueries fDeleteQueries;	

	private IChange fDeleteChange;
	
	private DeleteRefactoring2(IResource[] resources, IJavaElement[] javaElements){
		fResources= resources;
		fJavaElements= javaElements;
		fSuggestGetterSetterDeletion= true;//default
		fWasCanceled= false;
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
	
	public static boolean isAvailable(IResource[] resources, IJavaElement[] javaElements) throws JavaModelException{
		return new DeleteEnablementPolicy(resources, javaElements).canEnable();
	}
	
	public static DeleteRefactoring2 create(IResource[] resources, IJavaElement[] javaElements) throws JavaModelException{
		if (! isAvailable(resources, javaElements))
			return null;
		return new DeleteRefactoring2(resources, javaElements);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkActivation(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		Assert.isNotNull(fDeleteQueries);//must be set before checking activation
		pm.beginTask("", 1); //$NON-NLS-1$
		pm.done();
		return new RefactoringStatus();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkInput(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("Analyzing...", 1);
		try{
			fWasCanceled= false;
			RefactoringStatus result= new RefactoringStatus();

			recalculateElementsToDelete();

			TextChangeManager manager= new TextChangeManager();
			fDeleteChange= DeleteChangeCreator.createDeleteChange(manager, fResources, fJavaElements);
			result.merge(Checks.validateModifiesFiles(ResourceUtil.getFiles(manager.getAllCompilationUnits())));
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
		
		fJavaElements= ReorgUtils2.toWorkingCopies(fJavaElements);
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
		String queryTitle= "Confirm Referenced Package Fragment Root Delete";
		IConfirmQuery query= fDeleteQueries.createYesYesToAllNoNoToAllQuery(queryTitle, true, IReorgQueries.CONFIRM_DELETE_REFERENCED_ARCHIVES);
		List rootsToSkip= new ArrayList(0);
		for (int i= 0; i < fJavaElements.length; i++) {
			IJavaElement element= fJavaElements[i];
			if (! (element instanceof IPackageFragmentRoot))
				continue;
			IPackageFragmentRoot root= (IPackageFragmentRoot)element;
			IJavaProject[] referencing= JavaElementUtil.getReferencingProjects(root);
			if (root.isArchive() && referencing.length > 0){
				String pattern= "Package fragment root ''{0}'' is referenced by the following project(s). Do you still want to delete it?";
				String question= MessageFormat.format(pattern, new String[]{root.getElementName()});
				if (! query.confirm(question, referencing))
					rootsToSkip.add(root);
			}
		}
		removeFromSetToDelete((IJavaElement[]) rootsToSkip.toArray(new IJavaElement[rootsToSkip.size()]));
	}

	private void removeUnconfirmedFoldersThatContainSourceFolders() throws CoreException {
		String queryTitle= "Confirm Folder Delete";
		IConfirmQuery query= fDeleteQueries.createYesYesToAllNoNoToAllQuery(queryTitle, true, IReorgQueries.CONFIRM_DELETE_FOLDERS_CONTAINING_SOURCE_FOLDERS);
		List foldersToSkip= new ArrayList(0);
		for (int i= 0; i < fResources.length; i++) {
			IResource resource= fResources[i];
			if (resource instanceof IFolder){
				IFolder folder= (IFolder)resource;
				if (containsSourceFolder(folder)){
					String pattern= "Folder ''{0}'' contains a Java source folder. Deleting it will delete the source folder as well. Do you still wish to delete it?";
					String question= MessageFormat.format(pattern, new String[]{folder.getName()});
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
			IJavaElement element= JavaCore.create((IFolder)folder);
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);
		pm.done();
		return fDeleteChange;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#getName()
	 */
	public String getName() {
		return "Delete";
	}
	
	private void addToSetToDelete(IJavaElement[] newElements){
		fJavaElements= ReorgUtils2.union(fJavaElements, newElements);		
	}
	
	private void removeFromSetToDelete(IResource[] resourcesToNotDelete) {
		fResources= ReorgUtils2.setMinus(fResources, resourcesToNotDelete);
	}
	
	private void removeFromSetToDelete(IJavaElement[] elementsToNotDelete) {
		fJavaElements= ReorgUtils2.setMinus(fJavaElements, elementsToNotDelete);
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
		String queryTitle= "Confirm Delete of Getters/Setters";
		IConfirmQuery getterSetterQuery= fDeleteQueries.createYesYesToAllNoNoToAllQuery(queryTitle, true, IReorgQueries.CONFIRM_DELETE_GETTER_SETTER);
		for (Iterator iter= getterSetterMapping.keySet().iterator(); iter.hasNext();) {
			IField field= (IField) iter.next();
			Assert.isTrue(hasGetter(getterSetterMapping, field) || hasSetter(getterSetterMapping, field));
			String pattern= "Do you also want to delete getter/setter methods for field ''{0}''?";
			Object[] args= {JavaElementUtil.createFieldSignature(field)};
			String deleteGetterSetter= MessageFormat.format(pattern, args);
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
				getterSetterMapping.remove(field);
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
			ICompilationUnit cu= ReorgUtils2.getCompilationUnit(element);
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

	private static class DeleteEnablementPolicy implements IReorgEnablementPolicy {
	
		private final IResource[] fResources;
		private final IJavaElement[] fJavaElements;
	
		public DeleteEnablementPolicy(IResource[] resources, IJavaElement[] javaElements) {
			Assert.isNotNull(resources);
			Assert.isNotNull(javaElements);
			fResources= resources;
			fJavaElements= javaElements;
		}

		public final IResource[] getResources() {
			return fResources;
		}

		public final IJavaElement[] getJavaElements() {
			return fJavaElements;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.refactoring.reorg2.IReorgEnablementPolicy#canEnable()
		 */
		public boolean canEnable() throws JavaModelException {
			return ! isNothingToDelete() && canDeleteAll();
		}

		private boolean canDeleteAll() throws JavaModelException {
			for (int i= 0; i < fResources.length; i++) {
				if (! canDelete(fResources[i])) return false;
			}
			for (int i= 0; i < fJavaElements.length; i++) {
				if (! canDelete(fJavaElements[i])) return false;
			}
			return true;
		}

		private static boolean canDelete(IResource resource) {
			if (resource == null || ! resource.exists() || resource.isPhantom())
				return false;
			if (resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT)
				return false;
			return true;
		}

		private static boolean canDelete(IJavaElement element) throws JavaModelException {
			if (element == null || ! element.exists())
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
		
			if (ReorgUtils2.isDeletedFromEditor(element))
				return false;								
				
			//XXX workaround for 38450 Delete: Removing default package removes source folder
			if (JavaElementUtil.isDefaultPackage(element))
				return false;

			return true;
		}
	
		private static boolean isFromExternalArchive(IJavaElement element) {
			return element.getResource() == null && ! isWorkingCopyElement(element);
		}

		private static boolean isWorkingCopyElement(IJavaElement element) {
			if (element instanceof IWorkingCopy) 
				return ((IWorkingCopy)element).isWorkingCopy();
			if (ReorgUtils2.isInsideCompilationUnit(element))
				return ReorgUtils2.getCompilationUnit(element).isWorkingCopy();
			return false;
		}

		private static boolean isEmptySuperPackage(IPackageFragment pack) throws JavaModelException {
			return  pack.hasSubpackages() &&
					pack.getNonJavaResources().length == 0 &&
					pack.getChildren().length == 0;
		}

		private boolean isNothingToDelete() {
			return fJavaElements.length + fResources.length == 0;
		}
	}
}