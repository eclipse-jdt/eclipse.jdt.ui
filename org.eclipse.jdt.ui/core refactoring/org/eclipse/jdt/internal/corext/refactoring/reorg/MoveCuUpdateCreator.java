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

import org.eclipse.text.edits.ReplaceEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultCollector;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.structure.ReferenceFinderUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.CommentAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;
import org.eclipse.ltk.core.refactoring.TextChange;

public class MoveCuUpdateCreator {
	
	private ICompilationUnit[] fCus;
	private IPackageFragment fDestination;
	private CodeGenerationSettings fSettings;
	
	private Map fImportRewrites; //ICompilationUnit -> ImportEdit
	
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
		fImportRewrites= new HashMap();
	}
	
	private static ICompilationUnit[] convertToOriginals(ICompilationUnit[] cus){
		ICompilationUnit[] result= new ICompilationUnit[cus.length];
		for (int i= 0; i < cus.length; i++) {
			result[i]= convertToOriginal(cus[i]);
		}
		return result;
	}
	
	private static ICompilationUnit convertToOriginal(ICompilationUnit cu){
		return JavaModelUtil.toOriginal(cu);
	}
	
	public TextChangeManager createChangeManager(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 2); //$NON-NLS-1$
		try{
			TextChangeManager changeManager= new TextChangeManager();
			addUpdates(changeManager, new SubProgressMonitor(pm, 1));
			addImportsToDestinationPackage(new SubProgressMonitor(pm, 1));
			
			for (Iterator iter= fImportRewrites.keySet().iterator(); iter.hasNext();) {
				ICompilationUnit cu= (ICompilationUnit)iter.next();
				ImportRewrite importRewrite= (ImportRewrite)fImportRewrites.get(cu);
				if (importRewrite != null && ! importRewrite.isEmpty()) {
					TextBuffer buffer= null;
					try {
						buffer= TextBuffer.acquire((IFile)WorkingCopyUtil.getOriginal(cu).getResource());
						TextChangeCompatibility.addTextEdit(changeManager.get(cu), RefactoringCoreMessages.getString("MoveCuUpdateCreator.update_imports"), importRewrite.createEdit(buffer.getDocument())); //$NON-NLS-1$
					} finally {
						if (buffer != null)
							TextBuffer.release(buffer);
					}
				}
			}
			return changeManager;
		} catch (JavaModelException e){
			throw e;
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
		  	pm.subTask(RefactoringCoreMessages.getFormattedString("MoveCuUpdateCreator.searching", movedUnit.getElementName())); //$NON-NLS-1$
		  	
			if (isDestinationAnotherFragmentOfSamePackage(movedUnit)){
				pm.worked(3);
				return;
			}

		  	addImportToSourcePackageTypes(movedUnit, new SubProgressMonitor(pm, 1));
			removeImportsToDestinationPackageTypes(movedUnit);
			addReferenceUpdates(changeManager, movedUnit, new SubProgressMonitor(pm, 1));
		} finally{
			pm.done();
		}
	}

	private void addReferenceUpdates(TextChangeManager changeManager, ICompilationUnit movedUnit, IProgressMonitor pm) throws JavaModelException, CoreException {
		SearchResultGroup[] references = getReferences(movedUnit, pm);
		for (int i= 0; i < references.length; i++){
			ICompilationUnit referencingCu= references[i].getCompilationUnit();
			if (referencingCu == null)
				continue;
			SearchResult[] results= references[i].getSearchResults();
			for (int j= 0; j < results.length; j++) {
				TypeReference reference= (TypeReference)results[j];
				if (!reference.isImportDeclaration()) {
					reference.addEdit(changeManager.get(referencingCu), fDestination.getElementName());
				} else {	
					ImportRewrite importEdit= getImportRewrite(referencingCu);
					IImportDeclaration importDecl= (IImportDeclaration)results[j].getEnclosingElement();
					importEdit.removeImport(importDecl.getElementName());
                    importEdit.addImport(createStringForNewImport(movedUnit, importDecl));
				}	
			}
		}
	}

    private String createStringForNewImport(ICompilationUnit movedUnit, IImportDeclaration importDecl) {
    	String old= importDecl.getElementName();
		int oldPackLength= movedUnit.getParent().getElementName().length();
		
		StringBuffer result= new StringBuffer(fDestination.getElementName());
		if (oldPackLength == 0) // move FROM default package
			result.append('.').append(old);
		else if (result.length() == 0) // move TO default package
			result.append(old.substring(oldPackLength + 1)); // cut "."
		else
			result.append(old.substring(oldPackLength));
		return result.toString();
    }
	
	private void removeImportsToDestinationPackageTypes(ICompilationUnit movedUnit) throws CoreException{
		ImportRewrite importEdit= getImportRewrite(movedUnit);
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
	
	private void addImportToSourcePackageTypes(ICompilationUnit movedUnit, IProgressMonitor pm) throws CoreException{
		List cuList= Arrays.asList(fCus);
		IType[] allCuTypes= movedUnit.getAllTypes();
		IType[] referencedTypes= ReferenceFinderUtil.getTypesReferencedIn(allCuTypes, pm);
		ImportRewrite importEdit= getImportRewrite(movedUnit);
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
	
	private void addImportsToDestinationPackage(IProgressMonitor pm) throws CoreException{
		pm.beginTask("", fCus.length); //$NON-NLS-1$
		ICompilationUnit[] cusThatNeedIt= collectCusThatWillImportDestinationPackage(pm);
		for (int i= 0; i < cusThatNeedIt.length; i++) {
			if (pm.isCanceled())
				throw new OperationCanceledException();
			
			ICompilationUnit iCompilationUnit= cusThatNeedIt[i];
			addImport(false, fDestination, iCompilationUnit);
			pm.worked(1);
		}
	}
	
	private boolean addImport(boolean force, IPackageFragment pack, ICompilationUnit cu) throws CoreException {		
		if (pack.isDefaultPackage())
			return false;

		if (cu.getImport(pack.getElementName() + ".*").exists())  //$NON-NLS-1$
			return false;

		ImportRewrite importEdit= getImportRewrite(cu);
		importEdit.setFilterImplicitImports(!force);
		importEdit.addImport(pack.getElementName() + ".*"); //$NON-NLS-1$
		return true;
	}
	
	private ImportRewrite getImportRewrite(ICompilationUnit cu) throws CoreException{
		if (fImportRewrites.containsKey(cu))	
			return (ImportRewrite)fImportRewrites.get(cu);
		ImportRewrite importEdit= new ImportRewrite(cu, fSettings);
		fImportRewrites.put(cu, importEdit);
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
		SearchResultGroup[] references= getReferences(movedUnit, pm);
		Set result= new HashSet();
		List cuList= Arrays.asList(fCus);
		for (int i= 0; i < references.length; i++) {
			SearchResultGroup searchResultGroup= references[i];
			ICompilationUnit referencingCu= references[i].getCompilationUnit();
			if (referencingCu == null)
				continue;
			if (needsImportToDestinationPackage(movedUnit, cuList, searchResultGroup, referencingCu))
				result.add(referencingCu);
		}
		return result;
	}

	private boolean needsImportToDestinationPackage(ICompilationUnit movedUnit, List cuList, SearchResultGroup searchResultGroup, ICompilationUnit referencingCu) {
		if (! hasSimpleReference(searchResultGroup))
			return false;
		if (referencingCu.equals(movedUnit))	
			return false;
		if (cuList.contains(referencingCu))	
			return false;
		if (isDestinationAnotherFragmentOfSamePackage(movedUnit))
			return false;
		
		if (isReferenceInAnotherFragmentOfSamePackage(searchResultGroup, movedUnit))
			return true;
		
		//heuristic	
		if (referencingCu.getImport(movedUnit.getParent().getElementName() + ".*").exists()) //$NON-NLS-1$
			return true;
		if (! referencingCu.getParent().equals(movedUnit.getParent()))	
			return false;
		return true;
	}
	
	private boolean isDestinationAnotherFragmentOfSamePackage(ICompilationUnit movedUnit) {
		return isInAnotherFragmentOfSamePackage(movedUnit, fDestination);
	}

	private boolean isReferenceInAnotherFragmentOfSamePackage(SearchResultGroup searchResultGroup, ICompilationUnit movedUnit) {
		ICompilationUnit cu= searchResultGroup.getCompilationUnit();
		if (cu == null)
			return false;
		if (! (cu.getParent() instanceof IPackageFragment))
			return false;
		IPackageFragment pack= (IPackageFragment) cu.getParent();
		return isInAnotherFragmentOfSamePackage(movedUnit, pack);
	}
	
	private static boolean isInAnotherFragmentOfSamePackage(ICompilationUnit cu, IPackageFragment pack) {
		if (! (cu.getParent() instanceof IPackageFragment))
			return false;
		IPackageFragment cuPack= (IPackageFragment) cu.getParent();
		return ! cuPack.equals(pack) && JavaModelUtil.isSamePackage(cuPack, pack);
	}
	
	private static boolean hasSimpleReference(SearchResultGroup searchResultGroup) {
		SearchResult[] results= searchResultGroup.getSearchResults();
		for (int i= 0; i < results.length; i++) {
			if (! ((TypeReference)results[i]).isQualified())
				return true;
		}
		return false;
	}

	private static SearchResultGroup[] getReferences(ICompilationUnit unit, IProgressMonitor pm) throws JavaModelException {
		IJavaSearchScope scope= RefactoringScopeFactory.create(unit);
		ISearchPattern pattern= createSearchPattern(unit);
		return RefactoringSearchEngine.search(scope, pattern, 
			new Collector(new SubProgressMonitor(pm, 1), getPackage(unit)));
	}

	private static IPackageFragment getPackage(ICompilationUnit cu){
		return (IPackageFragment)cu.getParent();
	}
	
	private static ISearchPattern createSearchPattern(ICompilationUnit cu) throws JavaModelException{
		return RefactoringSearchEngine.createSearchPattern(cu.getTypes(), IJavaSearchConstants.REFERENCES);
	}

	private final static class Collector extends SearchResultCollector {
		private IPackageFragment fSource;
		public Collector(IProgressMonitor pm, IPackageFragment source) {
			super(pm);
			fSource= source;
		}
		public void accept(IResource res, int start, int end, IJavaElement element, int accuracy) throws CoreException {
			if (element.getAncestor(IJavaElement.IMPORT_DECLARATION) != null) {
				getResults().add(new TypeReference(res, start, end, element, accuracy, true));
			} else {
				if (fSource.isDefaultPackage()) {
					end= start + 1; // this means a length of zero which is an insert;
					getResults().add(new TypeReference(res, start, end, element, accuracy, false));
				} else {
					ICompilationUnit unit= (ICompilationUnit)element.getAncestor(IJavaElement.COMPILATION_UNIT);
					if (unit != null) {
						IBuffer buffer= unit.getBuffer();
						String currectPackageName= fSource.getElementName();
						String match= buffer.getText(start, end - start);
						//TODO: must know where package reference ends
						//match= CommentAnalyzer.normalizeReference(match);
						if (match.startsWith(currectPackageName)) {
							end= start + currectPackageName.length();
							getResults().add(new TypeReference(res, start, end, element, accuracy, true));
						} else {
							// start, end never read iff not qualified
							getResults().add(new TypeReference(res, start, end, element, accuracy, false));
						}
					}
				}
			}
		}
	}
	
	private final static class TypeReference extends SearchResult {
		private boolean fQualified;
		public TypeReference(IResource resource, int start, int end, IJavaElement enclosingElement, int accuracy, boolean qualified) {
			super(resource, start, end, enclosingElement, accuracy);
			fQualified= qualified;
		}
		
		public boolean isImportDeclaration() {
			return getEnclosingElement().getAncestor(IJavaElement.IMPORT_DECLARATION) != null;
		}
		
		public void addEdit(TextChange change, String text) {
			if (fQualified) {
				TextChangeCompatibility.addTextEdit(change, RefactoringCoreMessages.getString("MoveCuUpdateCreator.update_references"), new ReplaceEdit(getStart(), getEnd() - getStart(), text)); //$NON-NLS-1$
			}
		}
		public boolean isQualified() {
			return fQualified;
		}
	}

}
