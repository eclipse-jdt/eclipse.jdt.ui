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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultCollector;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.Context;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenamePackageChange;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IQualifiedNameUpdatingRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdatingRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.ITextUpdatingRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.QualifiedNameFinder;
import org.eclipse.jdt.internal.corext.refactoring.util.QualifiedNameSearchResult;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;


public class RenamePackageRefactoring extends Refactoring implements IRenameRefactoring, IReferenceUpdatingRefactoring, ITextUpdatingRefactoring, IQualifiedNameUpdatingRefactoring {
	
	private IPackageFragment fPackage;
	private String fNewName;

	/** references to fPackage */
	private SearchResultGroup[] fOccurrences;
	/** other IPackageFragments with fPackage's name (*ex*cluding fPackage) */
	private IPackageFragment[] fNamesakePackages;
	/** references in CUs from fOccurrences and fPackage to types in namesake packages
	 * <p>mutable List of SearchResultGroup */
	private List fReferencesToTypesInNamesakes;
	/** references in namesake packages to types in fPackage
	 * <p>mutable List of SearchResultGroup */
	private List fReferencesToTypesInPackage;

	private TextChangeManager fChangeManager;
	private QualifiedNameSearchResult fQualifiedNameSearchResult;
	
	private boolean fUpdateReferences;
	private boolean fUpdateJavaDoc;
	private boolean fUpdateComments;
	private boolean fUpdateStrings;
	private boolean fUpdateQualifiedNames;
	private String fFilePatterns;
	
	public RenamePackageRefactoring(IPackageFragment pack){
		Assert.isNotNull(pack);
		fPackage= pack;
		fNewName= pack.getElementName();
		fUpdateReferences= true;
		fUpdateJavaDoc= false;
		fUpdateComments= false;
		fUpdateStrings= false;
	}
	
	/* non java-doc
	 * @see IRefactoring#getName
	 */
	public String getName(){
		return RefactoringCoreMessages.getFormattedString("RenamePackageRefactoring.name",  //$NON-NLS-1$
						new String[]{fPackage.getElementName(), fNewName});
	}
	
	public Object getNewElement(){
		IJavaElement parent= fPackage.getParent();
		if (!(parent instanceof IPackageFragmentRoot))
			return fPackage;//??
			
		IPackageFragmentRoot root= (IPackageFragmentRoot)parent;
		return root.getPackageFragment(fNewName);
	}
	
	/*
	 * @see ITextUpdatingRefactoring#canEnableTextUpdating()
	 */
	public boolean canEnableTextUpdating() {
		return true;
	}
	
	/*
	 * @see ITextUpdatingRefactoring#getUpdateJavaDoc()
	 */
	public boolean getUpdateJavaDoc() {
		return fUpdateJavaDoc;
	}

	/*
	 * @see ITextUpdatingRefactoring#getUpdateComments()
	 */
	public boolean getUpdateComments() {
		return fUpdateComments;
	}

	/*
	 * @see ITextUpdatingRefactoring#getUpdateStrings()
	 */
	public boolean getUpdateStrings() {
		return fUpdateStrings;
	}

	/*
	 * @see ITextUpdatingRefactoring#setUpdateJavaDoc(boolean)
	 */
	public void setUpdateJavaDoc(boolean update) {
		fUpdateJavaDoc= update;
	}

	/*
	 * @see ITextUpdatingRefactoring#setUpdateComments(boolean)
	 */
	public void setUpdateComments(boolean update) {
		fUpdateComments= update;
	}

	/*
	 * @see ITextUpdatingRefactoring#setUpdateStrings(boolean)
	 */
	public void setUpdateStrings(boolean update) {
		fUpdateStrings= update;
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#setNewName
	 */	
	public final void setNewName(String newName){
		Assert.isNotNull(newName);
		fNewName= newName;
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#getCurrentName
	 */
	public final String getCurrentName(){
		return fPackage.getElementName();
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#getNewName
	 */	
	public final String getNewName(){
		return fNewName;
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#canUpdateReferences()
	 */
	public boolean canEnableUpdateReferences() {
		return true;
	}

	/* non java-doc
	 * @see IRenameRefactoring#setUpdateReferences(boolean)
	 */
	public void setUpdateReferences(boolean update) {
		fUpdateReferences= update;
	}	
	
	/* non java-doc
	 * @see IRenameRefactoring#getUpdateReferences()
	 */	
	public boolean getUpdateReferences(){
		return fUpdateReferences;
	}
	
	/* non java-doc
	 * Method declared in IQualifiedNameUpdatingRefactoring
	 */	
	public boolean canEnableQualifiedNameUpdating() {
		return !fPackage.isDefaultPackage();
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
	
	//--- preconditions
	
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 1); //$NON-NLS-1$
		RefactoringStatus result= new RefactoringStatus();
		result.merge(Checks.checkAvailability(fPackage));
		
		if (fPackage.isDefaultPackage())
			result.addFatalError(""); //$NON-NLS-1$
		pm.done();	
		return result;
	}
	
	public RefactoringStatus checkNewName(String newName) throws JavaModelException{
		Assert.isNotNull(newName, "new name"); //$NON-NLS-1$
		RefactoringStatus result= Checks.checkPackageName(newName);
		if (Checks.isAlreadyNamed(fPackage, newName))
			result.addFatalError(RefactoringCoreMessages.getString("RenamePackageRefactoring.another_name")); //$NON-NLS-1$
		else if (fPackage.getElementName().equalsIgnoreCase(newName))	
			result.addFatalError(RefactoringCoreMessages.getString("RenamePackageRefactoring.different_case")); //$NON-NLS-1$
		result.merge(checkPackageInCurrentRoot(newName));
		return result;
	}
	
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException{
		try{
			pm.beginTask("", 16); //$NON-NLS-1$
			pm.setTaskName(RefactoringCoreMessages.getString("RenamePackageRefactoring.checking")); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkNewName(fNewName));
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
				fOccurrences= getReferences(new SubProgressMonitor(pm, 5));	
				//fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=47509
				fNamesakePackages= getNamesakePackages(fPackage.getElementName(), new SubProgressMonitor(pm, 1));
				fReferencesToTypesInPackage= getReferencesToTypesInPackage(new SubProgressMonitor(pm, 1));
				fReferencesToTypesInNamesakes= getReferencesToTypesInNamesakes(new SubProgressMonitor(pm, 1));
				//END fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=47509
				pm.setTaskName(RefactoringCoreMessages.getString("RenamePackageRefactoring.checking")); //$NON-NLS-1$
				result.merge(analyzeAffectedCompilationUnits());
				pm.worked(1);
			} else {
				pm.worked(9);
			}
			result.merge(checkPackageName(fNewName));
			if (result.hasFatalError())
				return result;
				
			fChangeManager= createChangeManager(new SubProgressMonitor(pm, 3));
			
			if (fUpdateQualifiedNames)			
				computeQualifiedNameMatches(new SubProgressMonitor(pm, 1));
			else
				pm.worked(1);
				
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
	
	private IJavaSearchScope createRefactoringScope()  throws JavaModelException{
		return RefactoringScopeFactory.create(fPackage);
	}
	
	private ISearchPattern createSearchPattern(){
		return SearchEngine.createSearchPattern(fPackage, IJavaSearchConstants.REFERENCES);
	}
	
	private SearchResultGroup[] getReferences(IProgressMonitor pm) throws JavaModelException{
		return RefactoringSearchEngine.search(pm, createRefactoringScope(), createSearchPattern());
	}
		
	private IPackageFragment[] getNamesakePackages(String name, IProgressMonitor pm) throws JavaModelException {
		IJavaSearchScope scope= RefactoringScopeFactory.create(fPackage);
		ISearchPattern pattern= SearchEngine.createSearchPattern(name, IJavaSearchConstants.PACKAGE, IJavaSearchConstants.DECLARATIONS, true);
		SearchResultCollector collector= new SearchResultCollector(pm);
		new SearchEngine().search(ResourcesPlugin.getWorkspace(), pattern, scope, collector);

		List results= collector.getResults();
		List packageFragments= new ArrayList();
		for (Iterator iter= results.iterator(); iter.hasNext();) {
			SearchResult result= (SearchResult) iter.next();
			IJavaElement enclosingElement= result.getEnclosingElement();
			if (enclosingElement instanceof IPackageFragment) {
				IPackageFragment pack= (IPackageFragment) enclosingElement;
				if (! fPackage.equals(pack))
					packageFragments.add(pack);
			}
		}
		return (IPackageFragment[]) packageFragments.toArray(new IPackageFragment[packageFragments.size()]);
	}
	
	private List getReferencesToTypesInNamesakes(IProgressMonitor pm) throws JavaModelException {
		if (fNamesakePackages.length == 0) {
			pm.done();
			return new ArrayList(0);
		}
		IType[] typesToSearch= getTypesInPackages(fNamesakePackages);
		ISearchPattern pattern= RefactoringSearchEngine.createSearchPattern(typesToSearch, IJavaSearchConstants.REFERENCES);
		SearchResultGroup[] results= RefactoringSearchEngine.search(pm, getScopeForReferencesToTypesInNamesakes(), pattern);
		return new ArrayList(Arrays.asList(results));
	}

	private IJavaSearchScope getScopeForReferencesToTypesInNamesakes() {
		List scopeList= new ArrayList();
		List namesakePackages= Arrays.asList(fNamesakePackages);
		for (int i= 0; i < fOccurrences.length; i++) {
			ICompilationUnit cu= fOccurrences[i].getCompilationUnit();
			if (cu == null)
				continue;
			IPackageFragment pack= (IPackageFragment) cu.getParent();
			if (pack == null) //should not happen
				continue;
			if (! namesakePackages.contains(pack))
				scopeList.add(cu);
		}
		scopeList.add(fPackage);
		return SearchEngine.createJavaSearchScope((IJavaElement[]) scopeList.toArray(new IJavaElement[scopeList.size()])); 
	}
	
	private IType[] getTypesInPackages(IPackageFragment[] packageFragments) throws JavaModelException {
		List types= new ArrayList();
		for (int i= 0; i < packageFragments.length; i++) {
			IPackageFragment pack= packageFragments[i];
			addContainedTypes(pack, types);
		}
		return (IType[]) types.toArray(new IType[types.size()]);
	}

	private void addContainedTypes(IPackageFragment pack, List typesCollector) throws JavaModelException {
		IJavaElement[] children= pack.getChildren();
		for (int c= 0; c < children.length; c++) {
			IJavaElement child= children[c];
			if (child instanceof ICompilationUnit) {
				typesCollector.addAll(Arrays.asList(((ICompilationUnit) child).getTypes()));
			}
		}
	}

	private List getReferencesToTypesInPackage(IProgressMonitor pm) throws JavaModelException {
		if (fNamesakePackages.length == 0) {
			pm.done();
			return new ArrayList(0);
		}
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(fNamesakePackages);
		List types= new ArrayList();
		addContainedTypes(fPackage, types);
		IType[] typesToSearch= (IType[]) types.toArray(new IType[types.size()]);
		ISearchPattern pattern= RefactoringSearchEngine.createSearchPattern(typesToSearch, IJavaSearchConstants.REFERENCES);
		SearchResultGroup[] results= RefactoringSearchEngine.search(pm, scope, pattern);
		return new ArrayList(Arrays.asList(results));
	}

	private RefactoringStatus checkForMainMethods() throws JavaModelException{
		ICompilationUnit[] cus= fPackage.getCompilationUnits();
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < cus.length; i++)
			result.merge(Checks.checkForMainMethods(cus[i]));
		return result;
	}
	
	private RefactoringStatus checkForNativeMethods() throws JavaModelException{
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
	public static boolean isPackageNameOkInRoot(String newName, IPackageFragmentRoot root) throws JavaModelException{
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
	
	private RefactoringStatus checkPackageInCurrentRoot(String newName) throws JavaModelException{
		if (isPackageNameOkInRoot(newName, getPackageFragmentRoot()))
			return null;
		else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("RenamePackageRefactoring.package_exists"));//$NON-NLS-1$
	}

	private IPackageFragmentRoot getPackageFragmentRoot() {
		return ((IPackageFragmentRoot)fPackage.getParent());
	}
	
	private RefactoringStatus checkPackageName(String newName) throws JavaModelException{		
		RefactoringStatus status= new RefactoringStatus();
		IPackageFragmentRoot[] roots= fPackage.getJavaProject().getPackageFragmentRoots();
		Set topLevelTypeNames= getTopLevelTypeNames();
		for (int i= 0; i < roots.length; i++) {
			if (! isPackageNameOkInRoot(newName, roots[i])){
				String message= RefactoringCoreMessages.getFormattedString("RenamePackageRefactoring.aleady_exists", new Object[]{fNewName, roots[i].getElementName()});//$NON-NLS-1$
				status.merge(RefactoringStatus.createWarningStatus(message));
				status.merge(checkTypeNameConflicts(roots[i], newName, topLevelTypeNames)); 
			}
		}
		return status;
	}
	
	private Set getTopLevelTypeNames() throws JavaModelException {
		ICompilationUnit[] cus= fPackage.getCompilationUnits();
		Set result= new HashSet(2 * cus.length); 
		for (int i= 0; i < cus.length; i++) {
			result.addAll(getTopLevelTypeNames(cus[i]));
		}
		return result;
	}
	
	private static Collection getTopLevelTypeNames(ICompilationUnit iCompilationUnit) throws JavaModelException {
		IType[] types= iCompilationUnit.getTypes();
		List result= new ArrayList(types.length);
		for (int i= 0; i < types.length; i++) {
			result.add(types[i].getElementName());
		}
		return result;
	}
	
	private RefactoringStatus checkTypeNameConflicts(IPackageFragmentRoot root, String newName, Set topLevelTypeNames) throws JavaModelException {
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
	
	private RefactoringStatus checkTypeNameConflicts(ICompilationUnit iCompilationUnit, Set topLevelTypeNames) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		IType[] types= iCompilationUnit.getTypes();
		String packageName= iCompilationUnit.getParent().getElementName();
		for (int i= 0; i < types.length; i++) {
			String name= types[i].getElementName();
			if (topLevelTypeNames.contains(name)){
				String[] keys= {packageName, name};
				String msg= RefactoringCoreMessages.getFormattedString("RenamePackageRefactoring.contains_type", keys); //$NON-NLS-1$
				Context context= JavaSourceContext.create(types[i]);
				result.addError(msg, context);
			}	
		}
		return result;
	}
		
	private RefactoringStatus analyzeAffectedCompilationUnits() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		fOccurrences= Checks.excludeCompilationUnits(fOccurrences, result);
		if (result.hasFatalError())
			return result;
		
		result.merge(Checks.checkCompileErrorsInAffectedFiles(fOccurrences));	
		return result;
	}

	private IFile[] getAllCusInPackageAsFiles() throws JavaModelException{
		ICompilationUnit[] cus= fPackage.getCompilationUnits();
		List files= new ArrayList(cus.length);
		for (int i= 0; i < cus.length; i++) {
            IResource res= ResourceUtil.getResource(cus[i]);
            if (res != null && res.getType() == IResource.FILE)
            	files.add(res);
        }
		return (IFile[]) files.toArray(new IFile[files.size()]);
	}
	
	private IFile[] getAllFilesToModify() throws CoreException{
		//cannot use Arrays.asList to create this temp - addAll is not supported on list created by Arrays.asList
		List combined= new ArrayList();
		combined.addAll(Arrays.asList(ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits())));
		combined.addAll(Arrays.asList(getAllCusInPackageAsFiles()));
		if (fQualifiedNameSearchResult != null)
			combined.addAll(Arrays.asList(fQualifiedNameSearchResult.getAllFiles()));
		return (IFile[]) combined.toArray(new IFile[combined.size()]);
	}
	
	private RefactoringStatus validateModifiesFiles() throws CoreException{
		return Checks.validateModifiesFiles(getAllFilesToModify());
	}
	
	// ----------- Changes ---------------
	
	public IChange createChange(IProgressMonitor pm) throws JavaModelException{
		try{
			pm.beginTask(RefactoringCoreMessages.getString("RenamePackageRefactoring.creating_change"), 1); //$NON-NLS-1$
			CompositeChange builder= new CompositeChange();
	
			builder.addAll(fChangeManager.getAllChanges());
			
			if (fQualifiedNameSearchResult != null)	
				builder.addAll(fQualifiedNameSearchResult.getAllChanges());
				
			builder.add(new RenamePackageChange(fPackage, fNewName));
			pm.worked(1);
			return builder;
		} finally{
			pm.done();
		}	
	}
	
	private void addTextMatches(TextChangeManager manager, IProgressMonitor pm) throws JavaModelException{
		TextMatchFinder.findTextMatches(pm, createRefactoringScope(), this, manager);
	}
	
	private TextEdit createTextChange(SearchResult searchResult) {
		return SimpleTextEdit.createReplace(searchResult.getStart(), searchResult.getEnd() - searchResult.getStart(), fNewName);
	}
	
	private TextChangeManager createChangeManager(IProgressMonitor pm) throws CoreException{
		pm.beginTask("", 2); //$NON-NLS-1$
		TextChangeManager manager= new TextChangeManager();
		
		pm.subTask(RefactoringCoreMessages.getString("RenamePackageRefactoring.searching_text")); //$NON-NLS-1$
		addTextMatches(manager, new SubProgressMonitor(pm, 1));
		
		if (fUpdateReferences)
			addReferenceUpdates(manager, new SubProgressMonitor(pm, 1));
		
		return manager;
	}
	
	private void addReferenceUpdates(TextChangeManager manager, IProgressMonitor pm) throws CoreException{
		pm.beginTask("", fOccurrences.length + fReferencesToTypesInPackage.size() + fReferencesToTypesInNamesakes.size()); //$NON-NLS-1$
		for (int i= 0; i < fOccurrences.length; i++){
			ICompilationUnit cu= fOccurrences[i].getCompilationUnit();
			if (cu == null)
				continue;
			String name= RefactoringCoreMessages.getString("RenamePackageRefactoring.update_reference"); //$NON-NLS-1$
			ICompilationUnit wc= 	WorkingCopyUtil.getWorkingCopyIfExists(cu);
			SearchResult updatedImport= null; 
			SearchResult[] results= fOccurrences[i].getSearchResults();
			for (int j= 0; j < results.length; j++){
				SearchResult result= results[j];
				manager.get(wc).addTextEdit(name, createTextChange(result));
				if (isImport(result) && updatedImport == null)
					updatedImport= result;
			}
			if (fNamesakePackages.length != 0) {
				SearchResultGroup typeRefsRequiringNewNameImport= extractGroupFor(cu, fReferencesToTypesInPackage);
				SearchResultGroup typeRefsRequiringOldNameImport= extractGroupFor(cu, fReferencesToTypesInNamesakes);
				addImportUpdatesForNamesakes(manager, pm, cu, typeRefsRequiringNewNameImport, typeRefsRequiringOldNameImport, updatedImport);
			}
			pm.worked(1);
		}	

		if (fNamesakePackages.length != 0) {
			for (Iterator iter= fReferencesToTypesInPackage.iterator(); iter.hasNext();) {
				SearchResultGroup namesakeReferencesToPackage= (SearchResultGroup) iter.next();
				ICompilationUnit cu= namesakeReferencesToPackage.getCompilationUnit();
				if (cu == null)
					continue;
				SearchResultGroup referencesToTypesInNamesakes= extractGroupFor(cu, fReferencesToTypesInNamesakes);
				addImportUpdatesForNamesakes(manager, pm, cu, namesakeReferencesToPackage, referencesToTypesInNamesakes, null);
			}
			for (Iterator iter= fReferencesToTypesInNamesakes.iterator(); iter.hasNext();) {
				SearchResultGroup referencesToTypesInNamesakes= (SearchResultGroup) iter.next();
				ICompilationUnit cu= referencesToTypesInNamesakes.getCompilationUnit();
				if (cu == null)
					continue;
				addImportUpdatesForNamesakes(manager, pm, cu, null, referencesToTypesInNamesakes, null);
			}
		}
		pm.done();
	}
	
	/**
	 * Adds necessary qualified imports for unqualified type references before updatedImport.
	 * @param typeRefsRequiringNewNameImport can be null
	 * @param typeRefsRequiringOldNameImport can be null
	 * @param updatedImport SearchResult for IImportDeclaration (insertion point for new updates), can be null
	 */
	private void addImportUpdatesForNamesakes(TextChangeManager manager, IProgressMonitor pm, ICompilationUnit cu,
				SearchResultGroup typeRefsRequiringNewNameImport, SearchResultGroup typeRefsRequiringOldNameImport,
				SearchResult updatedImport) throws CoreException {
		StringBuffer imports= new StringBuffer();
		if (typeRefsRequiringNewNameImport != null) {
			imports.append(getNewImports(cu, fNewName, typeRefsRequiringNewNameImport));
			pm.worked(1);
		}
		if (typeRefsRequiringOldNameImport != null) {
			imports.append(getNewImports(cu, fPackage.getElementName(), typeRefsRequiringOldNameImport));
			pm.worked(1);
		}
		if (imports.length() == 0)
			return;

		String name= RefactoringCoreMessages.getString("MoveCuUpdateCreator.update_imports"); //$NON-NLS-1$
		String delim= StubUtility.getLineDelimiterUsed(cu);
		int start= 0;
		if (updatedImport != null) {
			IImportDeclaration importDecl= (IImportDeclaration) updatedImport.getEnclosingElement();
			start= importDecl.getSourceRange().getOffset();
		} else {
			start= getImportContainerStart(cu);
			if (start == -1) {
				start= getPackageDeclarationEnd(cu);
				if (start != 0) {
					imports.insert(0, delim);
				}
			}
		}
		TextEdit edit= SimpleTextEdit.createInsert(start, imports.toString());
		ICompilationUnit wc= WorkingCopyUtil.getWorkingCopyIfExists(cu);
		manager.get(wc).addTextEdit(name, edit);
	}
	
	/** @return start position, or -1 iff not found */
	private static int getImportContainerStart(ICompilationUnit cu) throws CoreException {
		IImportContainer importContainer= cu.getImportContainer();
		if (importContainer.exists()) {
			ISourceRange range= importContainer.getSourceRange();
			if (range != null)
				return range.getOffset();
		}
		return -1;
	}
	
	private static int getPackageDeclarationEnd(ICompilationUnit cu) throws CoreException {
		IPackageDeclaration[] packDecls= cu.getPackageDeclarations();
		if (packDecls != null && packDecls.length > 0) {
			TextBuffer buffer= TextBuffer.create(cu.getSource());
			ISourceRange range= packDecls[0].getSourceRange();
			int line= buffer.getLineOfOffset(range.getOffset() + range.getLength());
			TextRegion region= buffer.getLineInformation(line + 1);
			if (region != null) {
				return region.getOffset();
			}
		}
		return 0;
	}
	
	private String getNewImports(ICompilationUnit cu, String packageName, SearchResultGroup typeRefs) throws JavaModelException {
		String[] types= getQualifiedTypesToImport(packageName, typeRefs);
		StringBuffer imports= new StringBuffer();
		String delim= StubUtility.getLineDelimiterUsed(cu);
		for (int i= 0; i < types.length; i++) {
			imports.append("import ").append(types[i]).append(';').append(delim);//$NON-NLS-1$
		}
		return imports.toString();
	}
	
	/**
	 * @return String[] of fully qualified types. These types must be added as explicit import statements.
	 */
	private String[] getQualifiedTypesToImport(String qualification, SearchResultGroup group) throws JavaModelException {
		SearchResult[] results= group.getSearchResults();
		ICompilationUnit cu= group.getCompilationUnit();
		if (cu == null)
			return new String[0];
		
		ICompilationUnit wc= WorkingCopyUtil.getWorkingCopyIfExists(cu);
		Set toImport= new HashSet();
		Set alreadyImported= new HashSet();
		for (int i= 0; i < results.length; i++) {
			SearchResult result= results[i];
			IJavaElement element= result.getEnclosingElement();
			if (element instanceof IImportDeclaration) {
				IImportDeclaration importDeclaration= (IImportDeclaration) element;
				if (! importDeclaration.isOnDemand()) {
					alreadyImported.add(importDeclaration.getElementName());
				}
			} else {
				String reference= wc.getBuffer().getText(result.getStart(), result.getEnd() - result.getStart());
				if (! reference.startsWith(fPackage.getElementName())) { // doesn't work if qualification contains comment or \\u characters
					int dotPos= reference.indexOf('.');
					if (dotPos == -1) { 
						toImport.add(qualification + '.' + reference);
					} else {
						toImport.add(qualification + '.' + reference.substring(0, dotPos));
					}
				}
			}
		}
		toImport.removeAll(alreadyImported);
		return (String[]) toImport.toArray(new String[toImport.size()]);
	}
	

	private static boolean isImport(SearchResult searchResult) throws JavaModelException {
		return searchResult.getEnclosingElement() instanceof IImportDeclaration;
	}
		
	/** Removes the found SearchResultGroup from the list iff found.
	 *  @param searchResultGroups List of SearchResultGroup
	 *  @return the SearchResultGroup for cu, or null iff not found */
	private static SearchResultGroup extractGroupFor(ICompilationUnit cu, List searchResultGroups) {
		for (Iterator iter= searchResultGroups.iterator(); iter.hasNext();) {
			SearchResultGroup group= (SearchResultGroup) iter.next();
			if (cu.equals(group.getCompilationUnit())) {
				iter.remove();
				return group;
			}
		}
		return null;
	}

	private void computeQualifiedNameMatches(IProgressMonitor pm) throws JavaModelException {
		fQualifiedNameSearchResult= QualifiedNameFinder.process(fPackage.getElementName(), fNewName, 
			fFilePatterns, fPackage.getJavaProject().getProject(), pm);
	}	
}
