/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.Utils;
import org.eclipse.jdt.internal.corext.refactoring.base.ICompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;

public class MoveCuUpdateCreator {
	
	private ICompilationUnit[] fCus;
	private IPackageFragment fDestination;
	private CodeGenerationSettings fSettings;
	
	public MoveCuUpdateCreator(ICompilationUnit cu, IPackageFragment pack, CodeGenerationSettings settings){
		this(new ICompilationUnit[]{cu}, pack, settings);
	}
	
	public MoveCuUpdateCreator(ICompilationUnit[] cus, IPackageFragment pack, CodeGenerationSettings settings){
		Assert.isNotNull(cus);
		Assert.isNotNull(pack);
		Assert.isNotNull(settings);
		fCus= convertToOriginals(cus);
		fDestination= pack;
		fSettings= settings;
	}
	
	private static ICompilationUnit[] convertToOriginals(ICompilationUnit[] cus){
		ICompilationUnit[] result= new ICompilationUnit[cus.length];
		for (int i= 0; i < cus.length; i++) {
			result[i]= convertToOriginal(cus[i]);
		}
		return result;
	}
	
	private static ICompilationUnit convertToOriginal(ICompilationUnit cu){
		if (! cu.isWorkingCopy())
			return cu;
		return (ICompilationUnit)cu.getOriginalElement();	
	}
	
	public ICompositeChange createUpdateChange(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 2);
		try{
			TextChangeManager changeManager= new TextChangeManager();
			addUpdates(changeManager, new SubProgressMonitor(pm, 1));
			addImportsToDestinationPackage(changeManager, new SubProgressMonitor(pm, 1));
			return new CompositeChange("reorganize elements", changeManager.getAllChanges());
		} catch (CoreException e){	
			throw new JavaModelException(e);
		} finally{
			pm.done();
		}
	}

	private void addUpdates(TextChangeManager changeManager, IProgressMonitor pm) throws CoreException {
		pm.beginTask("", fCus.length); 
		for (int i= 0; i < fCus.length; i++){
			if (pm.isCanceled())
				throw new OperationCanceledException();
		
			addUpdates(changeManager, fCus[i], new SubProgressMonitor(pm, 1));
		}
	}
	
	private void addUpdates(TextChangeManager changeManager, ICompilationUnit movedUnit, IProgressMonitor pm) throws CoreException{
		try{
			pm.beginTask("", 1); 
		  	pm.subTask("searching for references to types in " + movedUnit.getElementName());
		  	
		  	//must force this import - otherwise it'd be filtered out
		  	addImport(true, changeManager, getPackage(movedUnit), movedUnit, fSettings);
			
			SearchResultGroup[] references = getReferences(movedUnit, pm);
			for (int i= 0; i < references.length; i++){
				IJavaElement el= JavaCore.create(references[i].getResource());
				if (! (el instanceof ICompilationUnit))
					continue;
				ICompilationUnit referencingCu= (ICompilationUnit)el;
				SearchResult[] results= references[i].getSearchResults();
				for (int j= 0; j < results.length; j++){
					if (isQualifiedReference(results[j])){
						TextEdit edit= new UpdateTypeReferenceEdit(results[j], getPackage(movedUnit), fDestination);
						changeManager.get(referencingCu).addTextEdit("update references", edit);
					}	
				}
			}
		} finally{
			pm.done();
		}
	}
	
	private void addImportsToDestinationPackage(TextChangeManager changeManager, IProgressMonitor pm) throws CoreException{
		pm.beginTask("", fCus.length);
		ICompilationUnit[] cusThatNeedIt= collectCusThatWillImportDestinationPackage(pm);
		for (int i= 0; i < cusThatNeedIt.length; i++) {
			if (pm.isCanceled())
				throw new OperationCanceledException();
			
			ICompilationUnit iCompilationUnit= cusThatNeedIt[i];
			addImport(false, changeManager, fDestination, iCompilationUnit, fSettings);
			pm.worked(1);
		}
	}
	
	private static boolean addImport(boolean force, TextChangeManager changeManager, IPackageFragment pack, ICompilationUnit cu, CodeGenerationSettings settings) throws JavaModelException {		
		try{
			if (cu.getImport(pack.getElementName() + ".*").exists()) 
				return false;
	
			ImportEdit importEdit= new ImportEdit(cu, settings);
			importEdit.addImport(pack.getElementName() + ".*");
			importEdit.setFilterImplicitImports(!force);
			changeManager.get(cu).addTextEdit("add import", importEdit);
			return true;
		} catch (CoreException e){
			throw new JavaModelException(e);
		}	
	}
	
	private ICompilationUnit[] collectCusThatWillImportDestinationPackage(IProgressMonitor pm) throws JavaModelException{
		Set collected= new HashSet(); //use set to remove dups
		pm.beginTask("", fCus.length);
		for (int i= 0; i < fCus.length; i++) {
			collected.addAll(collectCusThatWillImportDestinationPackage(fCus[i], new SubProgressMonitor(pm, 1)));
		}
		return (ICompilationUnit[]) collected.toArray(new ICompilationUnit[collected.size()]);
	}
	
	private Set collectCusThatWillImportDestinationPackage(ICompilationUnit movedUnit, IProgressMonitor pm) throws JavaModelException{
		SearchResultGroup[] references = getReferences(movedUnit, pm);
		Set result= new HashSet();
		List cuList= Arrays.asList(fCus);
		for (int i= 0; i < references.length; i++) {
			SearchResultGroup searchResultGroup= references[i];
			ICompilationUnit referencingCu= (ICompilationUnit)JavaCore.create(references[i].getResource());
			if (hasSimpleReference(searchResultGroup) 
				&& (!referencingCu.equals(movedUnit)) 
				&& (!cuList.contains(referencingCu))){
					result.add(referencingCu);
				}	
		}
		return result;
	}
	
	private static boolean hasSimpleReference(SearchResultGroup searchResultGroup) throws JavaModelException{
		SearchResult[] results= searchResultGroup.getSearchResults();
		for (int i= 0; i < results.length; i++) {
			if (! isQualifiedReference(results[i]))
				return true;
		}
		return false;
	}

	private static SearchResultGroup[] getReferences(ICompilationUnit unit, IProgressMonitor pm) throws org.eclipse.jdt.core.JavaModelException {
		IJavaSearchScope scope= SearchEngine.createWorkspaceScope();
		ISearchPattern pattern= createSearchPattern(unit);
		SearchResultGroup[] references= RefactoringSearchEngine.search(new SubProgressMonitor(pm, 1), scope, pattern);
		return references;
	}

	private static boolean isQualifiedReference(SearchResult searchResult) throws JavaModelException{
		if (searchResult.getEnclosingElement() instanceof IImportDeclaration)
			return true;

		//XXX very nasty
		IResource resource= searchResult.getResource();
		IJavaElement element= JavaCore.create(resource);
		if (element == null || ! (element instanceof ICompilationUnit))
			return false;
			
		int offset= searchResult.getStart();
		int end= searchResult.getEnd();
		String source= ((ICompilationUnit)element).getBuffer().getText(offset, end - offset);
		if (source.indexOf(".") != -1)
			return true;
		//--
		return false;	
	}
	
	private static IPackageFragment getPackage(ICompilationUnit cu){
		return (IPackageFragment)cu.getParent();
	}
	
	private static ISearchPattern createSearchPattern(ICompilationUnit cu) throws JavaModelException{
		IType[] types= cu.getTypes();
		if (types.length == 0)
			return null;
		ISearchPattern pattern= SearchEngine.createSearchPattern(types[0], IJavaSearchConstants.REFERENCES);
		for(int i= 1; i < types.length; i++){ //not-idiomatic loop
			pattern= SearchEngine.createOrSearchPattern(pattern, SearchEngine.createSearchPattern(types[i], IJavaSearchConstants.REFERENCES));
		}
		return pattern;
	}

	private static class UpdateTypeReferenceEdit extends SimpleTextEdit{
		
		private SearchResult fSearchResult;
		private IPackageFragment fDestination;
		private IPackageFragment fSource;
		
		UpdateTypeReferenceEdit(SearchResult searchResult, IPackageFragment source, IPackageFragment destination){
			fSearchResult= searchResult;
			fDestination= destination;
			fSource= source;
		}
		
		public TextEdit copy() throws CoreException {
			return new UpdateTypeReferenceEdit(fSearchResult, fSource, fDestination);
		}

		public void connect(TextBufferEditor editor) throws CoreException {
			int length= fSearchResult.getEnd() - fSearchResult.getStart();
			int offset= fSearchResult.getStart();
			String newText= fDestination.getElementName();
			String oldText= editor.getTextBuffer().getContent(offset, length);
			String currectPackageName= fSource.getElementName();
			
			if (fSource.isDefaultPackage()){
				length= 0;
			} else if (! oldText.startsWith(currectPackageName)){
				//no action - simple reference
				length= 0;
				newText= "";
			} else{
				length= currectPackageName.length();
			}
			setText(newText);
			setTextRange(new TextRange(offset, length));
		}
	}
}

