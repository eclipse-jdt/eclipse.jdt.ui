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
package org.eclipse.jdt.internal.corext.refactoring.rename;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenamePackageChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.changes.ValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.participants.IResourceModifications;
import org.eclipse.jdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.jdt.internal.corext.refactoring.participants.RenameProcessor;
import org.eclipse.jdt.internal.corext.refactoring.participants.ResourceModifications;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IQualifiedNameUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.ITextUpdating;
import org.eclipse.jdt.internal.corext.refactoring.util.QualifiedNameFinder;
import org.eclipse.jdt.internal.corext.refactoring.util.QualifiedNameSearchResult;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;


public class RenamePackageProcessor extends RenameProcessor implements IReferenceUpdating, ITextUpdating, IQualifiedNameUpdating {
	
	private IPackageFragment fPackage;
	private SearchResultGroup[] fOccurrences;
	private TextChangeManager fChangeManager;
	private QualifiedNameSearchResult fQualifiedNameSearchResult;
	
	private boolean fUpdateReferences;
	private boolean fUpdateJavaDoc;
	private boolean fUpdateComments;
	private boolean fUpdateStrings;
	private boolean fUpdateQualifiedNames;
	private String fFilePatterns;
	
	//---- IRefactoringProcessor ---------------------------------------------------

	public void initialize(Object[] elements) {
		Assert.isTrue(elements != null && elements.length == 1);
		Object element= elements[0];
		if (!(element instanceof IPackageFragment))
			return;
		fPackage= (IPackageFragment)element;
		setNewElementName(fPackage.getElementName());
		fUpdateReferences= true;
		fUpdateJavaDoc= false;
		fUpdateComments= false;
		fUpdateStrings= false;
	}
	
	public boolean isAvailable() throws CoreException {
		if (fPackage == null)
			return false;
		if (! Checks.isAvailable(fPackage))
			return false;
		if (fPackage.isDefaultPackage())
			return false;
		return true;
	}
	
	public String getProcessorName(){
		return RefactoringCoreMessages.getFormattedString("RenamePackageRefactoring.name",  //$NON-NLS-1$
						new String[]{fPackage.getElementName(), fNewElementName});
	}
	
	public IProject[] getAffectedProjects() throws CoreException {
		return JavaProcessors.computeScope(fPackage);
	}
	
	public Object[] getElements() {
		return new Object[] {fPackage};
	}

	public IResourceModifications getResourceModifications() throws CoreException {
		ResourceModifications result= new ResourceModifications();
		IContainer container= (IContainer)fPackage.getResource();
		if (container == null)
			return null;
		IResource[] members= container.members();
		int files= 0;
		for (int i= 0; i < members.length; i++) {
			IResource member= members[i];
			if (member instanceof IFile) {
				result.addMove(member);
				files++;
			}
		}
		IPath path= fPackage.getParent().getPath();
		path= path.append(getNewElementName().replace('.', '/'));
		IFolder target= ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
		if (!target.exists()) {
			result.addCreate(target);
		}
		result.setMoveTarget(target);
		if (files == members.length) {
			result.addDelete(container);
		}
		return result;		
	}
		 
	//---- ITextUpdating -------------------------------------------------

	public boolean canEnableTextUpdating() {
		return true;
	}
	
	public boolean getUpdateJavaDoc() {
		return fUpdateJavaDoc;
	}

	public boolean getUpdateComments() {
		return fUpdateComments;
	}

	public boolean getUpdateStrings() {
		return fUpdateStrings;
	}

	public void setUpdateJavaDoc(boolean update) {
		fUpdateJavaDoc= update;
	}

	public void setUpdateComments(boolean update) {
		fUpdateComments= update;
	}

	public void setUpdateStrings(boolean update) {
		fUpdateStrings= update;
	}
	
	//---- IReferenceUpdating --------------------------------------
		
	public boolean canEnableUpdateReferences() {
		return true;
	}

	public void setUpdateReferences(boolean update) {
		fUpdateReferences= update;
	}	
	
	public boolean getUpdateReferences(){
		return fUpdateReferences;
	}
	
	//---- IQualifiedNameUpdating ----------------------------------

	public boolean canEnableQualifiedNameUpdating() {
		return !fPackage.isDefaultPackage();
	}
	
	public boolean getUpdateQualifiedNames() {
		return fUpdateQualifiedNames;
	}
	
	public void setUpdateQualifiedNames(boolean update) {
		fUpdateQualifiedNames= update;
	}
	
	public String getFilePatterns() {
		return fFilePatterns;
	}
	
	public void setFilePatterns(String patterns) {
		Assert.isNotNull(patterns);
		fFilePatterns= patterns;
	}
	
	//---- IRenameProcessor ----------------------------------------------
	
	public final String getCurrentElementName(){
		return fPackage.getElementName();
	}
	
	public RefactoringStatus checkNewElementName(String newName) throws CoreException {
		Assert.isNotNull(newName, "new name"); //$NON-NLS-1$
		RefactoringStatus result= Checks.checkPackageName(newName);
		if (Checks.isAlreadyNamed(fPackage, newName))
			result.addFatalError(RefactoringCoreMessages.getString("RenamePackageRefactoring.another_name")); //$NON-NLS-1$
		result.merge(checkPackageInCurrentRoot(newName));
		return result;
	}
	
	public Object getNewElement(){
		IJavaElement parent= fPackage.getParent();
		if (!(parent instanceof IPackageFragmentRoot))
			return fPackage;//??
			
		IPackageFragmentRoot root= (IPackageFragmentRoot)parent;
		return root.getPackageFragment(fNewElementName);
	}
	
	public RefactoringStatus checkActivation() throws CoreException {
		return new RefactoringStatus();
	}
	
	public RefactoringStatus checkInput(IProgressMonitor pm) throws CoreException {
		try{
			pm.beginTask("", 15); //$NON-NLS-1$
			pm.setTaskName(RefactoringCoreMessages.getString("RenamePackageRefactoring.checking")); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkNewElementName(fNewElementName));
			pm.worked(1);
			result.merge(checkForNativeMethods());
			pm.worked(1);
			result.merge(checkForMainMethods());
			pm.worked(1);
			
			if (fPackage.isReadOnly()){
				String message= RefactoringCoreMessages.getFormattedString("RenamePackageRefactoring.Packagered_only",  //$NON-NLS-1$
									fPackage.getElementName()); 
				result.addFatalError(message);
			} else {
				if (fPackage.getResource().isReadOnly()){
					String message= RefactoringCoreMessages.getFormattedString("RenamePackageRefactoring.resource_read_only", //$NON-NLS-1$
										fPackage.getElementName());
					result.addError(message);
				}	
			}				
				
			if (fUpdateReferences){
				pm.setTaskName(RefactoringCoreMessages.getString("RenamePackageRefactoring.searching"));	 //$NON-NLS-1$
				fOccurrences= getReferences(new SubProgressMonitor(pm, 6));	
				pm.setTaskName(RefactoringCoreMessages.getString("RenamePackageRefactoring.checking")); //$NON-NLS-1$
				result.merge(analyzeAffectedCompilationUnits());
				pm.worked(1);
			} else {
				pm.worked(9);
			}
			result.merge(checkPackageName(fNewElementName));
			if (result.hasFatalError())
				return result;
				
			fChangeManager= createChangeManager(new SubProgressMonitor(pm, 3));
			
			if (fUpdateQualifiedNames)			
				computeQualifiedNameMatches(new SubProgressMonitor(pm, 1));
			else
				pm.worked(1);
				
			result.merge(validateModifiesFiles());
			return result;
		} finally{
			pm.done();
		}	
	}
	
	private IJavaSearchScope createRefactoringScope()  throws CoreException {
		return RefactoringScopeFactory.create(fPackage);
	}
	
	private ISearchPattern createSearchPattern(){
		return SearchEngine.createSearchPattern(fPackage, IJavaSearchConstants.REFERENCES);
	}
	
	private SearchResultGroup[] getReferences(IProgressMonitor pm) throws CoreException {
		return RefactoringSearchEngine.search(pm, createRefactoringScope(), createSearchPattern());
	}
		
	private RefactoringStatus checkForMainMethods() throws CoreException{
		ICompilationUnit[] cus= fPackage.getCompilationUnits();
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < cus.length; i++)
			result.merge(Checks.checkForMainMethods(cus[i]));
		return result;
	}
	
	private RefactoringStatus checkForNativeMethods() throws CoreException {
		ICompilationUnit[] cus= fPackage.getCompilationUnits();
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < cus.length; i++)
			result.merge(Checks.checkForNativeMethods(cus[i]));
		return result;
	}
	
	/*
	 * returns true if the new name is ok if the specified root.
	 * if a package fragment with this name exists and has java resources,
	 * then the name is not ok.
	 */
	public static boolean isPackageNameOkInRoot(String newName, IPackageFragmentRoot root) throws CoreException {
		IPackageFragment pack= root.getPackageFragment(newName);
		if (! pack.exists())
			return true;
		else if (! pack.hasSubpackages()) //leaves are no good
			return false;			
		else if (pack.containsJavaResources())
			return false;
		else if (pack.getNonJavaResources().length != 0)
			return false;
		else 
			return true;	
	}
	
	private RefactoringStatus checkPackageInCurrentRoot(String newName) throws CoreException {
		if (isPackageNameOkInRoot(newName, getPackageFragmentRoot()))
			return null;
		else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("RenamePackageRefactoring.package_exists"));//$NON-NLS-1$
	}

	private IPackageFragmentRoot getPackageFragmentRoot() {
		return ((IPackageFragmentRoot)fPackage.getParent());
	}
	
	private RefactoringStatus checkPackageName(String newName) throws CoreException {		
		RefactoringStatus status= new RefactoringStatus();
		IPackageFragmentRoot[] roots= fPackage.getJavaProject().getPackageFragmentRoots();
		Set topLevelTypeNames= getTopLevelTypeNames();
		for (int i= 0; i < roots.length; i++) {
			if (! isPackageNameOkInRoot(newName, roots[i])){
				String message= RefactoringCoreMessages.getFormattedString("RenamePackageRefactoring.aleady_exists", new Object[]{fNewElementName, roots[i].getElementName()});//$NON-NLS-1$
				status.merge(RefactoringStatus.createWarningStatus(message));
				status.merge(checkTypeNameConflicts(roots[i], newName, topLevelTypeNames)); 
			}
		}
		return status;
	}
	
	private Set getTopLevelTypeNames() throws CoreException {
		ICompilationUnit[] cus= fPackage.getCompilationUnits();
		Set result= new HashSet(2 * cus.length); 
		for (int i= 0; i < cus.length; i++) {
			result.addAll(getTopLevelTypeNames(cus[i]));
		}
		return result;
	}
	
	private static Collection getTopLevelTypeNames(ICompilationUnit iCompilationUnit) throws CoreException {
		IType[] types= iCompilationUnit.getTypes();
		List result= new ArrayList(types.length);
		for (int i= 0; i < types.length; i++) {
			result.add(types[i].getElementName());
		}
		return result;
	}
	
	private RefactoringStatus checkTypeNameConflicts(IPackageFragmentRoot root, String newName, Set topLevelTypeNames) throws CoreException {
		IPackageFragment otherPack= root.getPackageFragment(newName);
		if (fPackage.equals(otherPack))
			return null;
		ICompilationUnit[] cus= otherPack.getCompilationUnits();
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < cus.length; i++) {
			result.merge(checkTypeNameConflicts(cus[i], topLevelTypeNames));
		}
		return result;
	}
	
	private RefactoringStatus checkTypeNameConflicts(ICompilationUnit iCompilationUnit, Set topLevelTypeNames) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		IType[] types= iCompilationUnit.getTypes();
		String packageName= iCompilationUnit.getParent().getElementName();
		for (int i= 0; i < types.length; i++) {
			String name= types[i].getElementName();
			if (topLevelTypeNames.contains(name)){
				String[] keys= {packageName, name};
				String msg= RefactoringCoreMessages.getFormattedString("RenamePackageRefactoring.contains_type", keys); //$NON-NLS-1$
				RefactoringStatusContext context= JavaStatusContext.create(types[i]);
				result.addError(msg, context);
			}	
		}
		return result;
	}
		
	private RefactoringStatus analyzeAffectedCompilationUnits() throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		fOccurrences= Checks.excludeCompilationUnits(fOccurrences, result);
		if (result.hasFatalError())
			return result;
		
		result.merge(Checks.checkCompileErrorsInAffectedFiles(fOccurrences));	
		return result;
	}

	private IFile[] getAllCusInPackageAsFiles() throws CoreException {
		ICompilationUnit[] cus= fPackage.getCompilationUnits();
		List files= new ArrayList(cus.length);
		for (int i= 0; i < cus.length; i++) {
            IResource res= ResourceUtil.getResource(cus[i]);
            if (res != null && res.getType() == IResource.FILE)
            	files.add(res);
        }
		return (IFile[]) files.toArray(new IFile[files.size()]);
	}
	
	private IFile[] getAllFilesToModify() throws CoreException {
		//cannot use Arrays.asList to create this temp - addAll is not supported on list created by Arrays.asList
		List combined= new ArrayList();
		combined.addAll(Arrays.asList(ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits())));
		combined.addAll(Arrays.asList(getAllCusInPackageAsFiles()));
		if (fQualifiedNameSearchResult != null)
			combined.addAll(Arrays.asList(fQualifiedNameSearchResult.getAllFiles()));
		return (IFile[]) combined.toArray(new IFile[combined.size()]);
	}
	
	private RefactoringStatus validateModifiesFiles() throws CoreException {
		return Checks.validateModifiesFiles(getAllFilesToModify());
	}
	
	// ----------- Changes ---------------
	
	public Change createChange(IProgressMonitor pm) throws CoreException {
		try{
			pm.beginTask(RefactoringCoreMessages.getString("RenamePackageRefactoring.creating_change"), 1); //$NON-NLS-1$
			final ValidationStateChange result= new ValidationStateChange(
				RefactoringCoreMessages.getString("Change.javaChanges")); //$NON-NLS-1$
	
			result.addAll(fChangeManager.getAllChanges());
			
			if (fQualifiedNameSearchResult != null)	
				result.addAll(fQualifiedNameSearchResult.getAllChanges());
				
			result.add(new RenamePackageChange(fPackage, fNewElementName));
			pm.worked(1);
			return result;
		} finally{
			pm.done();
		}	
	}
	
	private void addTextMatches(TextChangeManager manager, IProgressMonitor pm) throws CoreException {
		TextMatchFinder.findTextMatches(pm, createRefactoringScope(), this, manager);
	}
	
	private TextEdit createTextChange(SearchResult searchResult) {
		return new ReplaceEdit(searchResult.getStart(), searchResult.getEnd() - searchResult.getStart(), fNewElementName);
	}
	
	private TextChangeManager createChangeManager(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 2); //$NON-NLS-1$
		TextChangeManager manager= new TextChangeManager();
		
		pm.subTask(RefactoringCoreMessages.getString("RenamePackageRefactoring.searching_text")); //$NON-NLS-1$
		addTextMatches(manager, new SubProgressMonitor(pm, 1));
		
		if (fUpdateReferences)
			addReferenceUpdates(manager, new SubProgressMonitor(pm, 1));
		
		return manager;
	}
	
	private void addReferenceUpdates(TextChangeManager manager, IProgressMonitor pm) throws CoreException {
		pm.beginTask("", fOccurrences.length); //$NON-NLS-1$
		for (int i= 0; i < fOccurrences.length; i++){
			ICompilationUnit cu= fOccurrences[i].getCompilationUnit();
			if (cu == null)
				continue;
			String name= RefactoringCoreMessages.getString("RenamePackageRefactoring.update_reference"); //$NON-NLS-1$
			ICompilationUnit wc= 	WorkingCopyUtil.getWorkingCopyIfExists(cu);
			SearchResult[] results= fOccurrences[i].getSearchResults();
			for (int j= 0; j < results.length; j++){
				TextChangeCompatibility.addTextEdit(manager.get(wc), name, createTextChange(results[j]));
			}
			pm.worked(1);
		}	
	}
	
	private void computeQualifiedNameMatches(IProgressMonitor pm) throws CoreException {
		if (fQualifiedNameSearchResult == null)
			fQualifiedNameSearchResult= new QualifiedNameSearchResult();
		QualifiedNameFinder.process(fQualifiedNameSearchResult, fPackage.getElementName(), fNewElementName, 
			fFilePatterns, fPackage.getJavaProject().getProject(), pm);
	}	
}
