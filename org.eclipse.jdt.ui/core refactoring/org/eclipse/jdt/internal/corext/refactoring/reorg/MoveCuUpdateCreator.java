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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

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
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultCollector;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.structure.ReferenceFinderUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.ltk.core.refactoring.TextChange;

public class MoveCuUpdateCreator {
	
	private ICompilationUnit[] fCus;
	private IPackageFragment fDestination;
	
	private Map fImportRewrites; //ICompilationUnit -> ImportEdit
	
	public MoveCuUpdateCreator(ICompilationUnit cu, IPackageFragment pack){
		this(new ICompilationUnit[]{cu}, pack);
	}
	
	public MoveCuUpdateCreator(ICompilationUnit[] cus, IPackageFragment pack){
		Assert.isNotNull(cus);
		Assert.isNotNull(pack);
		fCus= cus;
		fDestination= pack;
		fImportRewrites= new HashMap();
	}
	
	public TextChangeManager createChangeManager(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 5); //$NON-NLS-1$
		try{
			TextChangeManager changeManager= new TextChangeManager();
			addUpdates(changeManager, new SubProgressMonitor(pm, 4));
			addImportRewriteUpdates(changeManager);
			return changeManager;
		} catch (JavaModelException e){
			throw e;
		} catch (CoreException e){	
			throw new JavaModelException(e);
		} finally{
			pm.done();
		}
		
	}

	private void addImportRewriteUpdates(TextChangeManager changeManager) throws CoreException {
		for (Iterator iter= fImportRewrites.keySet().iterator(); iter.hasNext();) {
			ICompilationUnit cu= (ICompilationUnit)iter.next();
			ImportRewrite importRewrite= (ImportRewrite)fImportRewrites.get(cu);
			if (importRewrite != null && ! importRewrite.isEmpty()) {
				TextBuffer buffer= null;
				try {
					buffer= TextBuffer.acquire((IFile) cu.getResource());
					TextChangeCompatibility.addTextEdit(changeManager.get(cu), RefactoringCoreMessages.getString("MoveCuUpdateCreator.update_imports"), importRewrite.createEdit(buffer.getDocument())); //$NON-NLS-1$
				} finally {
					if (buffer != null)
						TextBuffer.release(buffer);
				}
			}
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
			addReferenceUpdates(changeManager, movedUnit, new SubProgressMonitor(pm, 2));
		} finally{
			pm.done();
		}
	}

	private void addReferenceUpdates(TextChangeManager changeManager, ICompilationUnit movedUnit, IProgressMonitor pm) throws JavaModelException, CoreException {
		List cuList= Arrays.asList(fCus);
		SearchResultGroup[] references= getReferences(movedUnit, pm);
		for (int i= 0; i < references.length; i++){
			SearchResultGroup searchResultGroup= references[i];
			ICompilationUnit referencingCu= searchResultGroup.getCompilationUnit();
			if (referencingCu == null)
				continue;
			
			boolean simpleReferencesNeedNewImport= simpleReferencesNeedNewImport(movedUnit, referencingCu, cuList);
			SearchResult[] results= searchResultGroup.getSearchResults();
			for (int j= 0; j < results.length; j++) {
				//TODO: should update type references with results from addImport
				TypeReference reference= (TypeReference)results[j];
				if (reference.isImportDeclaration()) {
					ImportRewrite importEdit= getImportRewrite(referencingCu);
					IImportDeclaration importDecl= (IImportDeclaration)results[j].getEnclosingElement();
					importEdit.removeImport(importDecl.getElementName());
                    importEdit.addImport(createStringForNewImport(movedUnit, importDecl));
				} else if (reference.isQualified()) {
					TextChange textChange= changeManager.get(referencingCu);
					String changeName= RefactoringCoreMessages.getString("MoveCuUpdateCreator.update_references"); //$NON-NLS-1$
					String newPackage= fDestination.isDefaultPackage() ? "" : fDestination.getElementName() + '.'; //$NON-NLS-1$
					TextEdit replaceEdit= new ReplaceEdit(reference.getStart(), reference.getSimpleNameStart() - reference.getStart(), newPackage);
					TextChangeCompatibility.addTextEdit(textChange, changeName, replaceEdit);
				} else if (simpleReferencesNeedNewImport) {
					ImportRewrite importEdit= getImportRewrite(referencingCu);
					String typeName= reference.getSimpleName();
                    importEdit.addImport(getQualifiedType(fDestination.getElementName(), typeName));
				}
			}
		}
	}
	
	private String getQualifiedType(String packageName, String typeName) {
		if (packageName.length() == 0)
			return typeName;
		else
			return packageName + '.' + typeName;
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
	
	private ImportRewrite getImportRewrite(ICompilationUnit cu) throws CoreException{
		if (fImportRewrites.containsKey(cu))	
			return (ImportRewrite)fImportRewrites.get(cu);
		ImportRewrite importEdit= new ImportRewrite(cu);
		fImportRewrites.put(cu, importEdit);
		return importEdit;	
	}
	
	private boolean simpleReferencesNeedNewImport(ICompilationUnit movedUnit, ICompilationUnit referencingCu, List cuList) {
		if (referencingCu.equals(movedUnit))	
			return false;
		if (cuList.contains(referencingCu))	
			return false;
		if (isReferenceInAnotherFragmentOfSamePackage(referencingCu, movedUnit)) {
			/* Destination package is different from source, since
			 * isDestinationAnotherFragmentOfSamePackage(movedUnit) was false in addUpdates(.) */
			return true;
		}
		
		//heuristic	
		if (referencingCu.getImport(movedUnit.getParent().getElementName() + ".*").exists()) //$NON-NLS-1$
			return true; // has old star import
		if (referencingCu.getParent().equals(movedUnit.getParent()))
			return true; //is moved away from same package
		return false; 
	}

	private boolean isDestinationAnotherFragmentOfSamePackage(ICompilationUnit movedUnit) {
		return isInAnotherFragmentOfSamePackage(movedUnit, fDestination);
	}

	private boolean isReferenceInAnotherFragmentOfSamePackage(ICompilationUnit referencingCu, ICompilationUnit movedUnit) {
		if (referencingCu == null)
			return false;
		if (! (referencingCu.getParent() instanceof IPackageFragment))
			return false;
		IPackageFragment pack= (IPackageFragment) referencingCu.getParent();
		return isInAnotherFragmentOfSamePackage(movedUnit, pack);
	}
	
	private static boolean isInAnotherFragmentOfSamePackage(ICompilationUnit cu, IPackageFragment pack) {
		if (! (cu.getParent() instanceof IPackageFragment))
			return false;
		IPackageFragment cuPack= (IPackageFragment) cu.getParent();
		return ! cuPack.equals(pack) && JavaModelUtil.isSamePackage(cuPack, pack);
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
		private IScanner fScanner;
		
		public Collector(IProgressMonitor pm, IPackageFragment source) {
			super(pm);
			fSource= source;
			fScanner= ToolFactory.createScanner(false, false, false, false);
		}
		
		public void accept(IResource res, int start, int end, IJavaElement element, int accuracy) throws CoreException {
			/*
			 * Processing is done in collector to reuse the buffer which was
			 * already required by the search engine to locate the matches.
			 */
			// [start, end[ include qualification.
			if (element.getAncestor(IJavaElement.IMPORT_DECLARATION) != null) {
				getResults().add(TypeReference.createImportReference(res, start, end, element, accuracy));
			} else {
				ICompilationUnit unit= (ICompilationUnit) element.getAncestor(IJavaElement.COMPILATION_UNIT);
				if (unit != null) {
					IBuffer buffer= unit.getBuffer();
					String matchText= buffer.getText(start, end - start);
					if (fSource.isDefaultPackage()) {
						getResults().add(TypeReference.createSimpleReference(res, start, end, element, accuracy, matchText));
					} else {
						int simpleNameStart= getLastSimpleNameStart(matchText);
						if (simpleNameStart != 0) {
							getResults().add(TypeReference.createQualifiedReference(res, start, end, element, accuracy, start + simpleNameStart));
						} else {
							getResults().add(TypeReference.createSimpleReference(res, start, end, element, accuracy, matchText));
						}
					}
				}
			}
		}
		
		private int getLastSimpleNameStart(String reference) {
			fScanner.setSource(reference.toCharArray());
			int lastIdentifierStart= -1;
			try {
				int tokenType= fScanner.getNextToken();
				while (tokenType != ITerminalSymbols.TokenNameEOF) {
					if (tokenType == ITerminalSymbols.TokenNameIdentifier)
						lastIdentifierStart= fScanner.getCurrentTokenStartPosition();
					tokenType= fScanner.getNextToken();
				}
			} catch (InvalidInputException e) {
				JavaPlugin.log(e);
			}
			return lastIdentifierStart;
		}
	}
	
	
	private final static class TypeReference extends SearchResult {
		private String fSimpleTypeName;
		private int fSimpleNameStart;
		
		private TypeReference(IResource resource, int start, int end, IJavaElement enclosingElement, int accuracy,
				int simpleNameStart, String simpleName) {
			super(resource, start, end, enclosingElement, accuracy);
			fSimpleNameStart= simpleNameStart;
			fSimpleTypeName= simpleName;
		}
		
		public static TypeReference createQualifiedReference(IResource resource, int start, int end, IJavaElement enclosingElement, int accuracy,
				int simpleNameStart) {
			Assert.isTrue(start < simpleNameStart && simpleNameStart < end);
			return new TypeReference(resource, start, end, enclosingElement, accuracy, simpleNameStart, null);
		}
		
		public static TypeReference createImportReference(IResource resource, int start, int end, IJavaElement enclosingElement, int accuracy) {
			return new TypeReference(resource, start, end, enclosingElement, accuracy, -1, null);
		}
		
		public static TypeReference createSimpleReference(IResource resource, int start, int end, IJavaElement enclosingElement, int accuracy,
				String simpleName) {
			return new TypeReference(resource, start, end, enclosingElement, accuracy, -1, simpleName);
		}
		
		public boolean isImportDeclaration() {
			return getEnclosingElement().getAncestor(IJavaElement.IMPORT_DECLARATION) != null;
		}
		
		public boolean isQualified() {
			return fSimpleNameStart != -1;
		}
		
		public boolean isSimpleReference() {
			return fSimpleTypeName != null;
		}
		
		/**
		 * @return start offset of simple type name, or -1 iff ! isQualified()
		 */
		public int getSimpleNameStart() {
			return fSimpleNameStart;
		}
		
		/**
		 * @return simple type name, or null iff ! isSimpleName()
		 */
		public String getSimpleName() {
			return fSimpleTypeName;
		}
	}

}
