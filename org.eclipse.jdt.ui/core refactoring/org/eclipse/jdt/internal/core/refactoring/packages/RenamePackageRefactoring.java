/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.packages;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChange;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.core.refactoring.text.SimpleReplaceTextChange;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.Checks;
import org.eclipse.jdt.internal.core.refactoring.CompositeChange;
import org.eclipse.jdt.internal.core.refactoring.DebugUtils;
import org.eclipse.jdt.internal.core.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.core.refactoring.SearchResult;
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;

/**
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public class RenamePackageRefactoring extends Refactoring implements IRenameRefactoring{
	
	private IPackageFragment fPackage;
	private String fNewName;
	
	private List fOccurrences;
	private ITextBufferChangeCreator fTextBufferChangeCreator;

	public RenamePackageRefactoring(ITextBufferChangeCreator changeCreator, IPackageFragment pack, String newName){
		super();
		Assert.isNotNull(pack, "package"); //$NON-NLS-1$
		Assert.isNotNull(changeCreator, "change creator"); //$NON-NLS-1$
		Assert.isNotNull(newName, "new name"); //$NON-NLS-1$
		fTextBufferChangeCreator= changeCreator;		
		fPackage= pack;
		fNewName= newName;
	}
	
	public RenamePackageRefactoring(ITextBufferChangeCreator changeCreator, IPackageFragment pack){
		super();
		Assert.isNotNull(pack, "package"); //$NON-NLS-1$
		Assert.isNotNull(changeCreator, "change creator"); //$NON-NLS-1$
		fTextBufferChangeCreator= changeCreator;		
		fPackage= pack;
	}
	
	/**
	 * @see IRefactoring#getName
	 */
	public String getName(){
		return RefactoringCoreMessages.getFormattedString("RenamePackageRefactoring.name",  //$NON-NLS-1$
						new String[]{fPackage.getElementName(), fNewName});
	}

	public RenamePackageRefactoring(IJavaSearchScope scope, IPackageFragment pack, String newName){
		super(scope);
		fPackage= pack;
		Assert.isNotNull(fPackage);
		fNewName= newName;
		Assert.isNotNull(fNewName);
	}
	
	public final void setJavaElement(IJavaElement javaElement){
		Assert.isNotNull(javaElement);
		Assert.isTrue(javaElement.exists(), RefactoringCoreMessages.getString("RenamePackageRefactoring.assert.must_exist"));	 //$NON-NLS-1$
		fPackage= (IPackageFragment)javaElement;
	}

	/**
	 * @see IRenameRefactoring#setNewName
	 */	
	public final void setNewName(String newName){
		Assert.isNotNull(newName);
		fNewName= newName;
	}
	
	/**
	 * @see IRenameRefactoring#getCurrentName
	 */
	public final String getCurrentName(){
		return fPackage.getElementName();
	}
		
	public final String getNewName(){
		return fNewName;
	}
	
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask(RefactoringCoreMessages.getString("RenamePackageRefactoring.checking"), 1); //$NON-NLS-1$
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkAvailability(fPackage));
		
		if (fPackage.isDefaultPackage())
			result.addFatalError(RefactoringCoreMessages.getString("RenamePackageRefactoring.no_default_package")); //$NON-NLS-1$
		pm.done();	
		return result;
	}
	
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException{
		/*
		 * not checked preconditions:
		 *  a. native methods in locally defined types in this package (too expensive - requires AST analysis)
		 */
		pm.beginTask("", 14); //$NON-NLS-1$
		pm.subTask(RefactoringCoreMessages.getString("RenamePackageRefactoring.checking")); //$NON-NLS-1$
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkNewName());
		pm.worked(1);
		result.merge(Checks.checkAffectedResourcesAvailability(getOccurrences(new SubProgressMonitor(pm, 6))));
		pm.subTask(RefactoringCoreMessages.getString("RenamePackageRefactoring.analyzing")); //$NON-NLS-1$
		result.merge(checkForNativeMethods());
		pm.worked(1);
		result.merge(checkForMainMethods());
		pm.worked(1);
		result.merge(analyzeAffectedCompilationUnits(new SubProgressMonitor(pm, 3)));
		result.merge(checkPackageName());
		pm.worked(1);
		pm.done();
		return result;
	}
	
	private List getOccurrences(IProgressMonitor pm) throws JavaModelException{
		pm.subTask(RefactoringCoreMessages.getString("RenamePackageRefactoring.searching"));	 //$NON-NLS-1$
		fOccurrences= RefactoringSearchEngine.search(pm, getScope(), createSearchPattern());
		return fOccurrences;
	}
	
	private RefactoringStatus checkForMainMethods() throws JavaModelException{
		ICompilationUnit[] cus= fPackage.getCompilationUnits();
		if (cus == null)
			return null;
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < cus.length; i++)
			result.merge(Checks.checkForMainMethods(cus[i]));
		return result;
	}
	
	private RefactoringStatus checkForNativeMethods() throws JavaModelException{
		ICompilationUnit[] cus= fPackage.getCompilationUnits();
		if (cus == null)
			return null;
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < cus.length; i++)
			result.merge(Checks.checkForNativeMethods(cus[i]));
		return result;
	}
	
	public RefactoringStatus checkNewName() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		result.merge(Checks.checkPackageName(fNewName));
		if (Checks.isAlreadyNamed(fPackage, fNewName))
			result.addFatalError(RefactoringCoreMessages.getString("RenamePackageRefactoring.another_name")); //$NON-NLS-1$
		result.merge(checkPackageInCurrentRoot());
		return result;
	}

	/*
	 * returns true if the new name is ok if the specified root.
	 * if a package fragment with this name exists and has java resources,
	 * then the name is not ok.
	 */
	private boolean isPackageNameOkInRoot(IPackageFragmentRoot root) throws JavaModelException{
		IPackageFragment pack= root.getPackageFragment(fNewName);
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
	
	private RefactoringStatus checkPackageInCurrentRoot() throws JavaModelException{
		IPackageFragmentRoot root= ((IPackageFragmentRoot)fPackage.getParent());
		IPackageFragment pack= root.getPackageFragment(fNewName);
		if (isPackageNameOkInRoot(root))
			return null;
		else{
			RefactoringStatus result= new RefactoringStatus();
			result.addFatalError(RefactoringCoreMessages.getString("RenamePackageRefactoring.package_exists")); //$NON-NLS-1$
			return result;
		}	
	}

	private ISearchPattern createSearchPattern(){
		return SearchEngine.createSearchPattern(fPackage, IJavaSearchConstants.REFERENCES);
	}
	
	private RefactoringStatus checkPackageName() throws JavaModelException{		
		RefactoringStatus result= new RefactoringStatus();
		
		IPackageFragmentRoot[] roots= fPackage.getJavaProject().getPackageFragmentRoots();
		
		for (int i= 0; i < roots.length; i++) {
			if (! isPackageNameOkInRoot(roots[i])){	
				result.addFatalError(RefactoringCoreMessages.getFormattedString("RenamePackageRefactoring.aleady_exists", fNewName)); //$NON-NLS-1$
				return result;
			}
		}
		return result;	
	}
		
	//-------------- AST visitor-based analysis
	
	/*
	 * (non java-doc)
	 * Analyzes all compilation units in which type is referenced
	 */
	private RefactoringStatus analyzeAffectedCompilationUnits(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= Checks.excludeCompilationUnits(fOccurrences, getUnsavedFileList());
		if (result.hasFatalError())
			return result;
		
		pm.beginTask("", fOccurrences.size());	 //$NON-NLS-1$
		Iterator iter= fOccurrences.iterator();
		RenamePackageASTAnalyzer analyzer= new RenamePackageASTAnalyzer(fNewName);
		while (iter.hasNext()){
			analyzeCompilationUnit(pm, analyzer, (List)iter.next(), result);
			pm.worked(1);
		}
		return result;
	}
	
	private void analyzeCompilationUnit(IProgressMonitor pm, RenamePackageASTAnalyzer analyzer, List searchResults, RefactoringStatus result)  throws JavaModelException {
		SearchResult searchResult= (SearchResult)searchResults.get(0);
		CompilationUnit cu= (CompilationUnit) (JavaCore.create(searchResult.getResource()));
		//pm.subTask("analyzing \"" + cu.getElementName() + "\"");
		pm.subTask(RefactoringCoreMessages.getFormattedString("RenamePackageRefactoring.analyzing_formatted", cu.getElementName())); //$NON-NLS-1$
		if ((! cu.exists()) || (cu.isReadOnly()) || (!cu.isStructureKnown()))
			return;
		result.merge(analyzer.analyze(searchResults, cu));
	}
	
	// ----------- Changes ---------------
	
	public IChange createChange(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask(RefactoringCoreMessages.getString("RenamePackageRefactoring.creating_change"), 1 + fOccurrences.size()); //$NON-NLS-1$
		CompositeChange builder= new CompositeChange();
		addOccurrences(pm, builder);
		builder.addChange(new RenamePackageChange(fPackage, fNewName));
		pm.worked(1);
		pm.done();
		fOccurrences= null; //to prevent memory leak
		return builder;
	}
	
	private SimpleReplaceTextChange createTextChange(SearchResult searchResult) {
		return new SimpleReplaceTextChange(RefactoringCoreMessages.getString("RenamePackageRefactoring.update_reference"), searchResult.getStart(), searchResult.getEnd() - searchResult.getStart(), fNewName); //$NON-NLS-1$
	}
	
	private void addOccurrences(IProgressMonitor pm, CompositeChange builder) throws JavaModelException{
		for (Iterator iter= fOccurrences.iterator(); iter.hasNext();){
			List l= (List)iter.next();
			ITextBufferChange change= fTextBufferChangeCreator.create(RefactoringCoreMessages.getFormattedString("RenamePackageRefactoring.update_references_to", fPackage.getElementName()), (ICompilationUnit)JavaCore.create(((SearchResult)l.get(0)).getResource())); //$NON-NLS-1$
			for (Iterator subIter= l.iterator(); subIter.hasNext();){
				change.addSimpleTextChange(createTextChange((SearchResult)subIter.next()));
			}
			builder.addChange(change);
			pm.worked(1);
		}
	}
	
}