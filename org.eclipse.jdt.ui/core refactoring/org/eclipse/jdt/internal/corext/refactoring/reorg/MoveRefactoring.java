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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.ICompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.MoveCompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.MovePackageChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.MovePackageFragmentRootChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.MoveResourceChange;
import org.eclipse.jdt.internal.corext.refactoring.structure.ReferenceFinderUtil;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IQualifiedNameUpdatingRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.QualifiedNameFinder;
import org.eclipse.jdt.internal.corext.refactoring.util.QualifiedNameSearchResult;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

public class MoveRefactoring extends ReorgRefactoring implements IQualifiedNameUpdatingRefactoring {
	
	private boolean fUpdateReferences;
	private boolean fUpdateQualifiedNames;
	private String fFilePatterns;
	
	private TextChangeManager fChangeManager;
	private QualifiedNameSearchResult fQualifiedNameSearchResult;
	
	private final CodeGenerationSettings fSettings;
	private final IPackageFragmentRootManipulationQuery fUpdateClasspathQuery;
	
	private MoveRefactoring(List elements, CodeGenerationSettings settings, IPackageFragmentRootManipulationQuery updateClasspathQuery) {
		super(elements);
		Assert.isNotNull(settings);
		fSettings= settings;
		fUpdateReferences= true;
		fQualifiedNameSearchResult= new QualifiedNameSearchResult();
		fUpdateClasspathQuery= updateClasspathQuery;
	}
	
	public static MoveRefactoring create(List elements, CodeGenerationSettings settings, IPackageFragmentRootManipulationQuery updateClasspathQuery) throws JavaModelException {
		if (! isAvailable(elements))
			return null;
		return new MoveRefactoring(elements, settings, updateClasspathQuery);
	}
	
	/* non java-doc
	 * Method declared in IQualifiedNameUpdatingRefactoring
	 */	
	public boolean canEnableQualifiedNameUpdating() {
		Object destination= getDestination();
		if (!(destination instanceof IPackageFragment))
			return false;
		if (((IPackageFragment)destination).isDefaultPackage())
			return false;
		return canUpdateQualifiedNames();
	}
	
	public boolean canUpdateQualifiedNames() {
		List elements= getElementsToReorg();
		Class last= null;
		for (Iterator iter= elements.iterator(); iter.hasNext();) {
			Object element= iter.next();
			if (last == null) {
				IJavaElement parent= null;
				if (element instanceof ICompilationUnit) {
					parent= ((IJavaElement)element).getParent();
					last= element.getClass();
				} else if (element instanceof IType) {
					parent= ((IJavaElement)element).getParent();
					last= element.getClass();
				} else {
					return false;
				}
				if (JavaElementUtil.isDefaultPackage(parent))
					return false;
			} else {
				if (!last.equals(element.getClass()))
					return false;
			}
		}
		return true;
	}
	
	/* non java-doc
	 * Method declared in IQualifiedNameUpdatingRefactoring
	 */	
	public boolean getUpdateQualifiedNames() {
		return fUpdateQualifiedNames;
	}
	
	/* non java-doc
	 * Method declared in IQualifiedNameUpdatingRefactoring
	 */	
	public void setUpdateQualifiedNames(boolean update) {
		fUpdateQualifiedNames= update;
	}
	
	/* non java-doc
	 * Method declared in IQualifiedNameUpdatingRefactoring
	 */	
	public String getFilePatterns() {
		return fFilePatterns;
	}
	
	/* non java-doc
	 * Method declared in IQualifiedNameUpdatingRefactoring
	 */	
	public void setFilePatterns(String patterns) {
		Assert.isNotNull(patterns);
		fFilePatterns= patterns;
	}
	
	/* non java-doc
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("MoveRefactoring.move_elements"); //$NON-NLS-1$
	}
	
	/* non java-doc
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public final RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask(RefactoringCoreMessages.getString("MoveRefactoring.searching"), 3); //$NON-NLS-1$
		try{
			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkReferencesToNotPublicTypes(new SubProgressMonitor(pm, 1)));
			result.merge(checkPackageVisibileClassReferences(new SubProgressMonitor(pm, 1)));
			fChangeManager= createChangeManager(new SubProgressMonitor(pm, 1));
			result.merge(validateModifiesFiles());
			return result;
		} catch (JavaModelException e){
			throw e;	
		} catch (CoreException e){	
			throw new JavaModelException(e);
		} finally{
			pm.done();
		}	
	}

	private RefactoringStatus checkReferencesToNotPublicTypes(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);//$NON-NLS-1$
		try{
			if (! (getDestination() instanceof IPackageFragment))
				return null;
	
			ICompilationUnit[] movedCus= collectCus();
			List movedCuList= Arrays.asList(movedCus);
			IType[] types= ReferenceFinderUtil.getTypesReferencedIn(movedCus, new SubProgressMonitor(pm, 1));
			RefactoringStatus result= new RefactoringStatus();
			for (int i= 0; i < types.length; i++) {
				IType type= types[i];
				if (JdtFlags.isPublic(type))
					continue;
				if (movedCuList.contains(type.getCompilationUnit()))
					continue;
				if (getDestination().equals(type.getPackageFragment()))
					continue;
				String[] keys= {JavaElementUtil.createSignature(type)};
				String message= RefactoringCoreMessages.getFormattedString("MoveRefactoring.warning.typeWillNotBeAccessible", keys); //$NON-NLS-1$
				result.addWarning(message);				
			}
			return result;
		} finally{
			pm.done();
		}
	}

	private RefactoringStatus checkPackageVisibileClassReferences(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);//$NON-NLS-1$
		try{
			if (! (getDestination() instanceof IPackageFragment))
				return null;
			
			ICompilationUnit[] movedCus= collectCus();
			List movedCuList= Arrays.asList(movedCus);
			IType[] movedPackageVisibleTypes= getMovedPackageVisibleTypes(movedCus);
			if (movedPackageVisibleTypes.length == 0)
				return null;
					
			IJavaSearchScope scope= SearchEngine.createWorkspaceScope();
			ISearchPattern pattern= createSearchPattern(movedPackageVisibleTypes);
			SearchResultGroup[] groups= RefactoringSearchEngine.search(new SubProgressMonitor(pm, 1), scope, pattern);
			
			RefactoringStatus result= new RefactoringStatus();
			for (int i= 0; i < groups.length; i++) {
				SearchResultGroup group= groups[i];
				ICompilationUnit cu= group.getCompilationUnit();
				if (cu == null)
					continue;
				if (cu.getParent().equals(getDestination()))
					continue;
				if (movedCuList.contains(cu))	
					continue;
				String[] keys= 	{cu.getElementName()};
				String message= RefactoringCoreMessages.getFormattedString("MoveRefactoring.warning.containsReferencesToTypeThatWillNotBeVisible", keys); //$NON-NLS-1$
				result.addWarning(message);
			}
			return result;
		} finally{
			pm.done();
		}	
	}

	private static ISearchPattern createSearchPattern(IType[] movedPackageVisibleTypes) {
		return RefactoringSearchEngine.createSearchPattern(movedPackageVisibleTypes, IJavaSearchConstants.REFERENCES);
	}
	
	private static IType[] getMovedPackageVisibleTypes(ICompilationUnit[] movedCus) throws JavaModelException{
		List result= new ArrayList(movedCus.length);
		for (int i= 0; i < movedCus.length; i++) {
			ICompilationUnit cu= movedCus[i];
			result.addAll(getPackageVisibleTypes(cu));
		}
		return (IType[]) result.toArray(new IType[result.size()]);
	}

	//returns list of ITypes	
	private static Collection getPackageVisibleTypes(ICompilationUnit cu) throws JavaModelException {
		IType[] types= cu.getTypes();
		List result= new ArrayList(types.length);
		for (int i= 0; i < types.length; i++) {
				IType type= types[i];
				if (JdtFlags.isPackageVisible(type))
					result.add(type);
				result.addAll(getPackageVisibleTypes(type));
		}
		return result;
	}

	//returns list of ITypes
	private static Collection getPackageVisibleTypes(IType type) throws JavaModelException {
		IType[] types= type.getTypes();
		List result= new ArrayList();
		for (int i= 0; i < types.length; i++) {
			IType innerType= types[i];
			if (JdtFlags.isPrivate(innerType))
				continue;
			if (! JdtFlags.isPublic(innerType))	
				result.add(innerType);
			result.addAll(getPackageVisibleTypes(innerType));	
		}
		return result;
	}
	
	private IFile[] getAllFilesToModify() throws CoreException{
		List result= new ArrayList();
		result.addAll(Arrays.asList(ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits())));
		result.addAll(Arrays.asList(fQualifiedNameSearchResult.getAllFiles()));
		if (getDestination() instanceof IPackageFragment)
			result.addAll(Arrays.asList(ResourceUtil.getFiles(collectCus())));
		return (IFile[]) result.toArray(new IFile[result.size()]);
	}
	
	private RefactoringStatus validateModifiesFiles() throws CoreException{
		return Checks.validateModifiesFiles(getAllFilesToModify());
	}

	/* non java-doc
	 * @see ReorgRefactoring#isValidDestinationForCusAndFiles(Object)
	 */
	boolean isValidDestinationForCusAndFiles(Object dest) throws JavaModelException{
		Object destination= getDestinationForCusAndFiles(dest);
		if (destination instanceof IPackageFragment)
			return canCopyCusAndFiles(dest);	
		
		return canCopyResources(dest);	
	}

	//overridden
	boolean canCopySourceFolders(Object dest) throws JavaModelException{
		if (! super.canCopySourceFolders(dest))
			return false;
		IJavaProject javaProject= JavaCore.create(getDestinationForSourceFolders(dest));
		if (javaProject == null)
			return false;
		if (! javaProject.exists()) //can copy - it cannot be parent of any source folder
			return true;
		if (destinationIsParent(getElements(), javaProject))
			return false;
		return true;	
	}
	
	//overridden
	boolean canCopyPackages(Object dest) throws JavaModelException{
		return super.canCopyPackages(dest) && !destinationIsParent(getElements(), getDestinationForPackages(dest));
	}
	
	//overridden
	boolean canCopyResources(Object dest) throws JavaModelException{
		IContainer destination= getDestinationForResources(dest);
		return super.canCopyResources(dest) && 
				! destinationIsOneOfResources(destination) &&
				! destinationIsParentForResources(destination);
	}
	
	private boolean destinationIsParentForResources(IContainer dest){
		if (dest == null)
			return false;
		for (Iterator iter= getElements().iterator(); iter.hasNext(); ){
			Object each= iter.next();
			if (!(each instanceof IResource))
				continue;
				
			IResource resource= (IResource)each;
			if (ReorgUtils.isParent(resource, dest))
				return true;
		}
		return false;
	}
	
	private boolean destinationIsOneOfResources(IContainer dest){
		if (dest == null)
			return false;
		for (Iterator iter= getElements().iterator(); iter.hasNext(); ){
			Object each= iter.next();
			if (dest.equals(each))
				return true;
		}
		return false;		
	}
	
	public boolean canUpdateReferences() throws JavaModelException{
		if (getDestination() == null)
			return false;
		
		if (hasPackages())
			return false;

		if (hasSourceFolders())
			return false;			
			
		if (! hasCus())	
			return false;	
		
		Object dest= getDestinationForCusAndFiles(getDestination());
		if (!(dest instanceof IPackageFragment))
			return false;
			
		if (((IPackageFragment)dest).isDefaultPackage())
			return false;
		
		if (isAnyCUFromDefaultPackage())
			return false;
			
		if (!allExist())
			return false;	
		
		if (isAnyUnsaved())
			return false;
		
		if (anyHasSyntaxErrors())
			return false;	
			
		return true;	
	}
	
	private boolean allExist(){
		for (Iterator iter= getElements().iterator(); iter.hasNext(); ){
			Object each= iter.next();
			if (!(each instanceof IJavaElement))
				continue;
			if (! ((IJavaElement)each).exists())
				return false;
		}
		return true;
	}
	
	private boolean isAnyCUFromDefaultPackage(){
		for (Iterator iter= getElements().iterator(); iter.hasNext(); ){
			Object each= iter.next();
			if (! (each instanceof ICompilationUnit))
				continue;
			if (JavaElementUtil.isDefaultPackage(((ICompilationUnit)each).getParent()))	
				return true;
		}
		return false;
	}
	
	private boolean anyHasSyntaxErrors() throws JavaModelException{
		for (Iterator iter= getElements().iterator(); iter.hasNext(); ){
			Object each= iter.next();
			if (! (each instanceof ICompilationUnit))
				continue;
			
			if (hasSyntaxErrors((ICompilationUnit)JavaCore.create(ResourceUtil.getResource((ICompilationUnit)each))))
					return true;
		}
		return false;
	}
	
	private static boolean hasSyntaxErrors(ICompilationUnit cu) throws JavaModelException{
		Assert.isTrue(!cu.isWorkingCopy());
		return ! cu.isStructureKnown();
	}
	
	private boolean isAnyUnsaved(){
		List elements= getElements();
		IFile[] unsaved= getUnsavedFiles();
		for (int i= 0; i < unsaved.length; i++){
			if (elements.contains(unsaved[i]))
				return true;
			if (elements.contains(JavaCore.create(unsaved[i])))	
				return true;
		}
		return false;
	}
	
	public void setUpdateReferences(boolean update){
		fUpdateReferences= update;
	}
	
	public boolean getUpdateReferences() {
		return fUpdateReferences;
	}
	
	//---- changes 

	/* non java-doc
	 * @see IRefactoring#creteChange(IProgressMonitor)
	 */	
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		if (! fUpdateReferences) {
			if (! fUpdateQualifiedNames) {
				return super.createChange(pm);
			} else {
				pm.beginTask("", 2); //$NON-NLS-1$
				IChange change= super.createChange(new SubProgressMonitor(pm, 1));
				CompositeChange result= new CompositeChange(RefactoringCoreMessages.getString("MoveRefactoring.reorganize_elements"), 2) { //$NON-NLS-1$
					public boolean isUndoable(){ //XX this can be undoable in some cases. should enable it at some point.
						return false; 
					}
				};
				if (change instanceof ICompositeChange) {
					addAllChildren(result, (ICompositeChange)change);		
				} else {
					result.add(change); 
				}
				computeQualifiedNameMatches(new SubProgressMonitor(pm, 1));
				result.addAll(fQualifiedNameSearchResult.getAllChanges());
				
				return result;
			}
		} else {
			pm.beginTask("", 2 + (fUpdateQualifiedNames ? 1 : 0)); //$NON-NLS-1$
			try{
				CompositeChange composite= new CompositeChange(RefactoringCoreMessages.getString("MoveRefactoring.reorganize_elements"), 2){ //$NON-NLS-1$
					public boolean isUndoable(){ //XX this can be undoable in some cases. should enable it at some point.
						return false; 
					}
				};
			
				//XX workaround for bug 13558
				//<workaround>
				if (fChangeManager == null){
					fChangeManager= createChangeManager(new SubProgressMonitor(pm, 1));
					try {
						RefactoringStatus status= validateModifiesFiles();
						if (status.hasFatalError())
							fChangeManager= new TextChangeManager();
					} catch (JavaModelException e){
						throw e;
					} catch(CoreException e) {
						throw new JavaModelException(e);
					}
				}	
				//</workaround>
				
				addAllChildren(composite, new CompositeChange(RefactoringCoreMessages.getString("MoveRefactoring.reorganize_elements"), fChangeManager.getAllChanges())); //$NON-NLS-1$
			
				if (fUpdateQualifiedNames) {
					computeQualifiedNameMatches(new SubProgressMonitor(pm, 1));
					composite.addAll(fQualifiedNameSearchResult.getAllChanges());
				}
				
				IChange fileMove= super.createChange(new SubProgressMonitor(pm, 1));
				if (fileMove instanceof ICompositeChange) {
					addAllChildren(composite, (ICompositeChange)fileMove);		
				} else{
					composite.add(fileMove);
				}
				return composite;
			} finally{
				pm.done();
			}
		}
	}
	
	private TextChangeManager createChangeManager(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);//$NON-NLS-1$
		try{
			if (! fUpdateReferences)
				return new TextChangeManager();
				
			Object dest= getDestinationForCusAndFiles(getDestination());
			if (dest instanceof IPackageFragment){			
				MoveCuUpdateCreator creator= new MoveCuUpdateCreator(collectCus(), (IPackageFragment)dest, fSettings);
				return creator.createChangeManager(new SubProgressMonitor(pm, 1));
			} else 
				return new TextChangeManager();
		} finally {
			pm.done();
		}		
	}
	
	private static void addAllChildren(CompositeChange collector, ICompositeChange composite){
		collector.addAll(composite.getChildren());
	}
			
	/*
	 * @see ReorgRefactoring#createChange(ICompilationUnit)
	 */	
	IChange createChange(IProgressMonitor pm, ICompilationUnit cu) throws JavaModelException{
		Object dest= getDestinationForCusAndFiles(getDestination());
		if (dest instanceof IPackageFragment)
			return moveCuToPackage(cu, (IPackageFragment)dest);
		else	
			return moveFileToContainer(cu, (IContainer)dest);//cast should be checked before - in preconditions
	}
	
	private IChange moveFileToContainer(ICompilationUnit cu, IContainer dest) {
		return new MoveResourceChange(ResourceUtil.getResource(cu), dest);
	}
	
	private IChange moveCuToPackage(ICompilationUnit cu, IPackageFragment dest) {
		//XXX workaround for bug 31998 we will have to disable renaming of linked packages (and cus)
		IResource resource= ResourceUtil.getResource(cu);
		if (resource != null && resource.isLinked()){
			if (ResourceUtil.getResource(dest) instanceof IContainer)
				return moveFileToContainer(cu, (IContainer)ResourceUtil.getResource(dest));
		}
		return new MoveCompilationUnitChange(cu, dest);
	}

	/*
	 * @see ReorgRefactoring#createChange(IPackageFragmentRoot)
	 */
	IChange createChange(IProgressMonitor pm, IPackageFragmentRoot root) throws JavaModelException{
		IProject destinationProject= getDestinationForSourceFolders(getDestination());
		return new MovePackageFragmentRootChange(root, destinationProject, fUpdateClasspathQuery);
	}

	/*
	 * @see ReorgRefactoring#createChange(IPackageFragment)
	 */		
	IChange createChange(IProgressMonitor pm, IPackageFragment pack) throws JavaModelException{
		return new MovePackageChange(pack, getDestinationForPackages(getDestination()));
	}
	
	/*
	 * @see ReorgRefactoring#createChange(IResource)
	 */	
	IChange createChange(IProgressMonitor pm, IResource res) throws JavaModelException{
		return new MoveResourceChange(res, getDestinationForResources(getDestination()));
	}
	
	private void computeQualifiedNameMatches(IProgressMonitor pm) throws JavaModelException {
		if (!fUpdateQualifiedNames)
			return;
		IPackageFragment destination= (IPackageFragment)getDestination();
		List elements= getElements();
		pm.beginTask("", elements.size()); //$NON-NLS-1$
		pm.subTask(RefactoringCoreMessages.getString("MoveRefactoring.scanning_qualified_names")); //$NON-NLS-1$
		for (Iterator iter= elements.iterator(); iter.hasNext();) {
			Object element= (Object)iter.next();
			if (element instanceof ICompilationUnit) {
				IType[] types= ((ICompilationUnit)element).getTypes();
				IProgressMonitor typesMonitor= new SubProgressMonitor(pm, 1);
				typesMonitor.beginTask("", types.length); //$NON-NLS-1$
				for (int i= 0; i < types.length; i++) {
					handleType(types[i], destination, new SubProgressMonitor(typesMonitor, 1));
				}
				typesMonitor.done();
			} else if (element instanceof IType) {
				handleType((IType)element, destination, new SubProgressMonitor(pm, 1));
			}
		}
		pm.done();
	}
	
	private void handleType(IType type, IPackageFragment destination, IProgressMonitor pm) throws JavaModelException {
		fQualifiedNameSearchResult=
			QualifiedNameFinder.process(type.getFullyQualifiedName(),  destination.getElementName() + "." + type.getTypeQualifiedName(), //$NON-NLS-1$
					fFilePatterns, type.getJavaProject().getProject(), pm);
	}	
}

