/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.structure.ReferenceFinderUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEditCopier;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

class MoveCuUpdateCreator {
	
	private ICompilationUnit[] fCus;
	private IPackageFragment fDestination;
	private CodeGenerationSettings fSettings;
	
	private Map fImportEdits; //ICompilationUnit -> ImportEdit
	
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
		fImportEdits= new HashMap();
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
	
	public TextChangeManager createChangeManager(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 2); //$NON-NLS-1$
		try{
			TextChangeManager changeManager= new TextChangeManager();
			addUpdates(changeManager, new SubProgressMonitor(pm, 1));
			addImportsToDestinationPackage(changeManager, new SubProgressMonitor(pm, 1));
			
			for (Iterator iter= fImportEdits.keySet().iterator(); iter.hasNext();) {
				ICompilationUnit cu= (ICompilationUnit)iter.next();
				ImportEdit importEdit= (ImportEdit)fImportEdits.get(cu);
				if (importEdit != null && ! importEdit.isEmpty())
					changeManager.get(cu).addTextEdit(RefactoringCoreMessages.getString("MoveCuUpdateCreator.update_imports"), importEdit); //$NON-NLS-1$
			}
			return changeManager;
		} catch (CoreException e){	
			throw new JavaModelException(e);
		} finally{
			pm.done();
		}
		
	}

	private void addUpdates(TextChangeManager changeManager, IProgressMonitor pm) throws CoreException {
		pm.beginTask("", fCus.length);  //$NON-NLS-1$
		for (int i= 0; i < fCus.length; i++){
			if (pm.isCanceled())
				throw new OperationCanceledException();
		
			addUpdates(changeManager, fCus[i], new SubProgressMonitor(pm, 1));
		}
	}
	
	private void addUpdates(TextChangeManager changeManager, ICompilationUnit movedUnit, IProgressMonitor pm) throws CoreException{
		try{
			pm.beginTask("", 3);  //$NON-NLS-1$
		  	pm.subTask(RefactoringCoreMessages.getString("MoveCuUpdateCreator.searching") + movedUnit.getElementName()); //$NON-NLS-1$
		  	
		  	addImportToSourcePackageTypes(changeManager, movedUnit, new SubProgressMonitor(pm, 1));
			removedImportsToDestinationPackageTypes(changeManager, movedUnit, new SubProgressMonitor(pm, 1));
			addReferenceUpdates(changeManager, movedUnit, new SubProgressMonitor(pm, 1));
		} finally{
			pm.done();
		}
	}

	private void addReferenceUpdates(TextChangeManager changeManager, ICompilationUnit movedUnit, IProgressMonitor pm) throws JavaModelException, CoreException {
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
					changeManager.get(referencingCu).addTextEdit(RefactoringCoreMessages.getString("MoveCuUpdateCreator.update_references"), edit); //$NON-NLS-1$
				}	
				if (results[j].getEnclosingElement().getElementType() == IJavaElement.IMPORT_DECLARATION){
					ImportEdit importEdit= getImportEdit(referencingCu);
					IType primaryType= movedUnit.findPrimaryType();
					importEdit.removeImport(JavaModelUtil.getFullyQualifiedName(primaryType));
					importEdit.addImport(fDestination.getElementName() + "." + primaryType.getElementName()); //$NON-NLS-1$
				}	
			}
		}
	}
	
	private void removedImportsToDestinationPackageTypes(TextChangeManager changeManager, ICompilationUnit movedUnit, IProgressMonitor pm) throws JavaModelException{
		ImportEdit importEdit= getImportEdit(movedUnit);
		IType[] destinationTypes= getDestinationPackageTypes();
		for (int i= 0; i < destinationTypes.length; i++) {
			importEdit.removeImport(JavaModelUtil.getFullyQualifiedName(destinationTypes[i]));
		}
	}
	
	private IType[] getDestinationPackageTypes() throws JavaModelException{
		List types= new ArrayList();
		ICompilationUnit[] cus= fDestination.getCompilationUnits();
		for (int i= 0; i < cus.length; i++) {
			types.addAll(Arrays.asList(cus[i].getAllTypes()));
		}
		return (IType[]) types.toArray(new IType[types.size()]);
	}
	
	private void addImportToSourcePackageTypes(TextChangeManager changeManager, ICompilationUnit movedUnit, IProgressMonitor pm) throws JavaModelException{
		List cuList= Arrays.asList(fCus);
		IType[] allCuTypes= movedUnit.getAllTypes();
		IType[] referencedTypes= ReferenceFinderUtil.getTypesReferencedIn(allCuTypes, pm);
		ImportEdit importEdit= getImportEdit(movedUnit);
		importEdit.setFilterImplicitImports(false);
		IPackageFragment srcPack= (IPackageFragment)movedUnit.getParent();
		for (int i= 0; i < referencedTypes.length; i++) {
				IType iType= referencedTypes[i];
				if (! iType.exists())
					continue;
				if (! iType.getPackageFragment().equals(srcPack))
					continue;
				if (cuList.contains(iType.getCompilationUnit()))
					continue;
				importEdit.addImport(JavaModelUtil.getFullyQualifiedName(iType));
		}
	}
	
	private void addImportsToDestinationPackage(TextChangeManager changeManager, IProgressMonitor pm) throws CoreException{
		pm.beginTask("", fCus.length); //$NON-NLS-1$
		ICompilationUnit[] cusThatNeedIt= collectCusThatWillImportDestinationPackage(pm);
		for (int i= 0; i < cusThatNeedIt.length; i++) {
			if (pm.isCanceled())
				throw new OperationCanceledException();
			
			ICompilationUnit iCompilationUnit= cusThatNeedIt[i];
			addImport(false, changeManager, fDestination, iCompilationUnit);
			pm.worked(1);
		}
	}
	
	private boolean addImport(boolean force, TextChangeManager changeManager, IPackageFragment pack, ICompilationUnit cu) throws JavaModelException {		
		if (cu.getImport(pack.getElementName() + ".*").exists())  //$NON-NLS-1$
			return false;

		ImportEdit importEdit= getImportEdit(cu);
		importEdit.setFilterImplicitImports(!force);
		importEdit.addImport(pack.getElementName() + ".*"); //$NON-NLS-1$
		return true;
	}
	
	private ImportEdit getImportEdit(ICompilationUnit cu){
		if (fImportEdits.containsKey(cu))	
			return (ImportEdit)fImportEdits.get(cu);
		ImportEdit importEdit= new ImportEdit(cu, fSettings);
		fImportEdits.put(cu, importEdit);
		return importEdit;	
	}
	
	private ICompilationUnit[] collectCusThatWillImportDestinationPackage(IProgressMonitor pm) throws JavaModelException{
		Set collected= new HashSet(); //use set to remove dups
		pm.beginTask("", fCus.length); //$NON-NLS-1$
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
			IJavaElement je= JavaCore.create(references[i].getResource());
			if (je.getElementType() != IJavaElement.COMPILATION_UNIT)
				continue;
			ICompilationUnit referencingCu= (ICompilationUnit)je;
			if (needsImportToDestinationPackage(movedUnit, cuList, searchResultGroup, referencingCu))
				result.add(referencingCu);
		}
		return result;
	}

	private boolean needsImportToDestinationPackage(ICompilationUnit movedUnit, List cuList, SearchResultGroup searchResultGroup, ICompilationUnit referencingCu) throws JavaModelException {
		if (! hasSimpleReference(searchResultGroup))
			return false;
		if (referencingCu.equals(movedUnit))	
			return false;
		if (cuList.contains(referencingCu))	
			return false;
			
		//heuristic	
		if (referencingCu.getImport(movedUnit.getParent().getElementName() + ".*").exists()) //$NON-NLS-1$
			return true;
		if (! referencingCu.getParent().equals(movedUnit.getParent()))	
			return false;
		return true;
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
			return false;

		//XXX needs improvement
		IResource resource= searchResult.getResource();
		IJavaElement element= JavaCore.create(resource);
		if (element == null || ! (element instanceof ICompilationUnit))
			return false;
			
		int offset= searchResult.getStart();
		int end= searchResult.getEnd();
		String source= ((ICompilationUnit)element).getBuffer().getText(offset, end - offset);
		if (source.indexOf(".") != -1) //$NON-NLS-1$
			return true;

		return false;	
	}
	
	private static IPackageFragment getPackage(ICompilationUnit cu){
		return (IPackageFragment)cu.getParent();
	}
	
	private static ISearchPattern createSearchPattern(ICompilationUnit cu) throws JavaModelException{
		return RefactoringSearchEngine.createSearchPattern(cu.getTypes(), IJavaSearchConstants.REFERENCES);
	}

	private final static class UpdateTypeReferenceEdit extends SimpleTextEdit {
		
		private SearchResult fSearchResult;
		private IPackageFragment fDestination;
		private IPackageFragment fSource;
		
		UpdateTypeReferenceEdit(SearchResult searchResult, IPackageFragment source, IPackageFragment destination) {
			fSearchResult= searchResult;
			fDestination= destination;
			fSource= source;
		}
		
		protected TextEdit copy0(TextEditCopier copier) {
			return new UpdateTypeReferenceEdit(fSearchResult, fSource, fDestination);
		}

		public void connect(TextBuffer buffer) throws CoreException {
			int length= fSearchResult.getEnd() - fSearchResult.getStart();
			int offset= fSearchResult.getStart();
			String newText= fDestination.getElementName();
			String oldText= buffer.getContent(offset, length);
			String currectPackageName= fSource.getElementName();
			
			if (fSource.isDefaultPackage()){
				length= 0;
			} else if (! oldText.startsWith(currectPackageName)){
				//no action - simple reference
				length= 0;
				newText= ""; //$NON-NLS-1$
			} else{
				length= currectPackageName.length();
			}
			setText(newText);
			setTextRange(new TextRange(offset, length));
			super.connect(buffer);
		}
	}
}

