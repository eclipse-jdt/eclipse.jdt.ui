/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Microsoft Corporation - Move CU should not remove import for the inner class - https://bugs.eclipse.org/bugs/show_bug.cgi?id=549674
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IResource;

import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeReferenceMatch;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.refactoring.CollectingSearchRequestor;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.ReferencesInBinaryContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.structure.ReferenceFinderUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.SearchUtils;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class MoveCuUpdateCreator {

	private final String fNewPackage;
	private ICompilationUnit[] fCus;
	private IPackageFragment fDestination;

	private Map<ICompilationUnit, ImportRewrite> fImportRewrites; //ICompilationUnit -> ImportEdit

	public MoveCuUpdateCreator(ICompilationUnit cu, IPackageFragment pack){
		this(new ICompilationUnit[]{cu}, pack);
	}

	public MoveCuUpdateCreator(ICompilationUnit[] cus, IPackageFragment pack){
		Assert.isNotNull(cus);
		Assert.isNotNull(pack);
		fCus= cus;
		fDestination= pack;
		fImportRewrites= new HashMap<>();
		fNewPackage= fDestination.isDefaultPackage() ? "" : fDestination.getElementName() + '.'; //$NON-NLS-1$
	}

	public TextChangeManager createChangeManager(IProgressMonitor pm, RefactoringStatus status) throws JavaModelException{
		pm.beginTask("", 5); //$NON-NLS-1$
		try{
			TextChangeManager changeManager= new TextChangeManager();
			addUpdates(changeManager, new SubProgressMonitor(pm, 4), status);
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
		for (Map.Entry<ICompilationUnit, ImportRewrite> entry : fImportRewrites.entrySet()) {
			ICompilationUnit cu = entry.getKey();
			ImportRewrite importRewrite= entry.getValue();
			if (importRewrite != null && importRewrite.hasRecordedChanges()) {
				TextChangeCompatibility.addTextEdit(changeManager.get(cu), RefactoringCoreMessages.MoveCuUpdateCreator_update_imports, importRewrite.rewriteImports(null));
			}
		}
	}

	private void addUpdates(TextChangeManager changeManager, IProgressMonitor pm, RefactoringStatus status) throws CoreException {
		pm.beginTask("", fCus.length);  //$NON-NLS-1$
		for (ICompilationUnit cu : fCus) {
			if (pm.isCanceled())
				throw new OperationCanceledException();
			addUpdates(changeManager, cu, new SubProgressMonitor(pm, 1), status);
		}
	}

	private void addUpdates(TextChangeManager changeManager, ICompilationUnit movedUnit, IProgressMonitor pm, RefactoringStatus status) throws CoreException{
		try{
			pm.beginTask("", 3);  //$NON-NLS-1$
		  	pm.subTask(Messages.format(RefactoringCoreMessages.MoveCuUpdateCreator_searching, BasicElementLabels.getFileName(movedUnit)));

			if (isInAnotherFragmentOfSamePackage(movedUnit, fDestination)){
				pm.worked(3);
				return;
			}

		  	addImportToSourcePackageTypes(movedUnit, new SubProgressMonitor(pm, 1));
			removeImportsToDestinationPackageTypes(movedUnit);
			addReferenceUpdates(changeManager, movedUnit, new SubProgressMonitor(pm, 2), status);
		} finally{
			pm.done();
		}
	}

	private void addReferenceUpdates(TextChangeManager changeManager, ICompilationUnit movedUnit, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException, CoreException {
		List<ICompilationUnit> cuList= Arrays.asList(fCus);
		for (SearchResultGroup searchResultGroup : getReferences(movedUnit, pm, status)) {
			ICompilationUnit referencingCu= searchResultGroup.getCompilationUnit();
			if (referencingCu == null)
				continue;
			boolean simpleReferencesNeedNewImport= simpleReferencesNeedNewImport(movedUnit, referencingCu, cuList);
			for (SearchMatch result : searchResultGroup.getSearchResults()) {
				// TODO: should update type references with results from addImport
				TypeReference reference= (TypeReference) result;
				if (reference.isImportDeclaration()) {
					ImportRewrite rewrite= getImportRewrite(referencingCu);
					IImportDeclaration importDecl= (IImportDeclaration) SearchUtils.getEnclosingJavaElement(result);
					if (Flags.isStatic(importDecl.getFlags())) {
						rewrite.removeStaticImport(importDecl.getElementName());
						addStaticImport(movedUnit, importDecl, rewrite);
					} else {
						rewrite.removeImport(importDecl.getElementName());
						rewrite.addImport(createStringForNewImport(movedUnit, importDecl));
					}
				} else if (reference.isQualified()) {
					TextChange textChange= changeManager.get(referencingCu);
					String changeName= RefactoringCoreMessages.MoveCuUpdateCreator_update_references;
					TextEdit replaceEdit= new ReplaceEdit(reference.getOffset(), reference.getSimpleNameStart() - reference.getOffset(), fNewPackage);
					TextChangeCompatibility.addTextEdit(textChange, changeName, replaceEdit);
				} else if (simpleReferencesNeedNewImport) {
					ImportRewrite importEdit= getImportRewrite(referencingCu);
					String typeName= reference.getSimpleName();
					importEdit.addImport(getQualifiedType(fDestination.getElementName(), typeName));
				}
			}
		}
	}

	private void addStaticImport(ICompilationUnit movedUnit, IImportDeclaration importDecl, ImportRewrite rewrite) {
		String old= importDecl.getElementName();
		int oldPackLength= movedUnit.getParent().getElementName().length();

		StringBuilder result= new StringBuilder(fDestination.getElementName());
		if (oldPackLength == 0) // move FROM default package
			result.append('.').append(old);
		else if (result.length() == 0) // move TO default package
			result.append(old.substring(oldPackLength + 1)); // cut "."
		else
			result.append(old.substring(oldPackLength));
		int index= result.lastIndexOf("."); //$NON-NLS-1$
		if (index > 0 && index < result.length() - 1)
			rewrite.addStaticImport(result.substring(0, index), result.substring(index + 1, result.length()), true);
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

		StringBuilder result= new StringBuilder(fDestination.getElementName());
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
		for (IType destinationType : getDestinationPackageTypes()) {
			importEdit.removeImport(destinationType.getFullyQualifiedName('.'));
		}
		importEdit.removeImport(fDestination.getElementName().concat(".*")); //$NON-NLS-1$
	}

	private IType[] getDestinationPackageTypes() throws JavaModelException {
		List<IType> types= new ArrayList<>();
		if (fDestination.exists()) {
			for (ICompilationUnit cu : fDestination.getCompilationUnits()) {
				types.addAll(Arrays.asList(cu.getTypes()));
			}
		}
		return types.toArray(new IType[types.size()]);
	}

	private void addImportToSourcePackageTypes(ICompilationUnit movedUnit, IProgressMonitor pm) throws CoreException{
		List<ICompilationUnit> cuList= Arrays.asList(fCus);
		IType[] allCuTypes= movedUnit.getAllTypes();
		ImportRewrite importEdit= getImportRewrite(movedUnit);
		importEdit.setFilterImplicitImports(false);
		IPackageFragment srcPack= (IPackageFragment)movedUnit.getParent();
		for (IType type : ReferenceFinderUtil.getTypesReferencedIn(allCuTypes, pm)) {
			if (! type.exists())
				continue;
			if (!JavaModelUtil.isSamePackage(type.getPackageFragment(), srcPack))
				continue;
			if (cuList.contains(type.getCompilationUnit()))
				continue;
			importEdit.addImport(type.getFullyQualifiedName('.'));
		}
	}

	private ImportRewrite getImportRewrite(ICompilationUnit cu) throws CoreException{
		if (fImportRewrites.containsKey(cu))
			return fImportRewrites.get(cu);
		ImportRewrite importEdit= StubUtility.createImportRewrite(cu, true);
		fImportRewrites.put(cu, importEdit);
		return importEdit;
	}

	private boolean simpleReferencesNeedNewImport(ICompilationUnit movedUnit, ICompilationUnit referencingCu, List<ICompilationUnit> cuList) {
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

	private static SearchResultGroup[] getReferences(ICompilationUnit unit, IProgressMonitor pm, RefactoringStatus status) throws CoreException {
		final SearchPattern pattern= RefactoringSearchEngine.createOrPattern(unit.getTypes(), IJavaSearchConstants.REFERENCES);
		if (pattern != null) {
			String binaryRefsDescription= Messages.format(RefactoringCoreMessages.ReferencesInBinaryContext_ref_in_binaries_description , BasicElementLabels.getFileName(unit));
			ReferencesInBinaryContext binaryRefs= new ReferencesInBinaryContext(binaryRefsDescription);
			Collector requestor= new Collector(((IPackageFragment) unit.getParent()), binaryRefs);
			IJavaSearchScope scope= RefactoringScopeFactory.create(unit, true, false);

			SearchResultGroup[] result= RefactoringSearchEngine.search(pattern, scope, requestor, new SubProgressMonitor(pm, 1), status);
			binaryRefs.addErrorIfNecessary(status);
			return result;
		}
		return new SearchResultGroup[] {};
	}

	private final static class Collector extends CollectingSearchRequestor {
		private IPackageFragment fSource;
		private IScanner fScanner;

		public Collector(IPackageFragment source, ReferencesInBinaryContext binaryRefs) {
			super(binaryRefs);
			fSource= source;
			IJavaProject javaProject= source.getJavaProject();
	        if (javaProject != null) {
	            String sourceLevel = javaProject.getOption(JavaCore.COMPILER_SOURCE, true);
	            String complianceLevel = javaProject.getOption(JavaCore.COMPILER_COMPLIANCE, true);
	            fScanner = ToolFactory.createScanner(false, false, false, sourceLevel, complianceLevel);
	        } else {
	        	fScanner= ToolFactory.createScanner(false, false, false, false);
	        }
		}

		@Override
		public void acceptSearchMatch(SearchMatch match) throws CoreException {
			if (filterMatch(match))
				return;

			/*
			 * Processing is done in collector to reuse the buffer which was
			 * already required by the search engine to locate the matches.
			 */
			// [start, end[ include qualification.
			IJavaElement element= SearchUtils.getEnclosingJavaElement(match);
			int accuracy= match.getAccuracy();
			int start= match.getOffset();
			int length= match.getLength();
			boolean insideDocComment= match.isInsideDocComment();
			IResource res= match.getResource();
			if (element.getAncestor(IJavaElement.IMPORT_DECLARATION) != null) {
				collectMatch(TypeReference.createImportReference(element, accuracy, start, length, insideDocComment, res));
			} else {
				ICompilationUnit unit= (ICompilationUnit) element.getAncestor(IJavaElement.COMPILATION_UNIT);
				if (unit != null) {
					IBuffer buffer= unit.getBuffer();
					String matchText= buffer.getText(start, length);
					if (fSource.isDefaultPackage()) {
						collectMatch(TypeReference.createSimpleReference(element, accuracy, start, length, insideDocComment, res, matchText));
					} else {
						// assert: matchText doesn't start nor end with comment
						int simpleNameStart= getLastSimpleNameStart(matchText);
						if (simpleNameStart != 0) {
							collectMatch(TypeReference.createQualifiedReference(element, accuracy, start, length, insideDocComment, res, start + simpleNameStart));
						} else {
							collectMatch(TypeReference.createSimpleReference(element, accuracy, start, length, insideDocComment, res, matchText));
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
					if (tokenType == ITerminalSymbols.TokenNameIdentifier) // TODO: could we be in the context of module-info.java, conflicting with restricted keywords?
						lastIdentifierStart= fScanner.getCurrentTokenStartPosition();
					tokenType= fScanner.getNextToken();
				}
			} catch (InvalidInputException e) {
				JavaPlugin.log(e);
			}
			return lastIdentifierStart;
		}
	}


	private final static class TypeReference extends TypeReferenceMatch {
		private String fSimpleTypeName;
		private int fSimpleNameStart;

		private TypeReference(IJavaElement enclosingElement, int accuracy, int start, int length,
				boolean insideDocComment, IResource resource, int simpleNameStart, String simpleName) {
			super(enclosingElement, accuracy, start, length,
					insideDocComment, SearchEngine.getDefaultSearchParticipant(), resource);
			fSimpleNameStart= simpleNameStart;
			fSimpleTypeName= simpleName;
		}

		public static TypeReference createQualifiedReference(IJavaElement enclosingElement, int accuracy, int start, int length,
				boolean insideDocComment, IResource resource, int simpleNameStart) {
			Assert.isTrue(start < simpleNameStart && simpleNameStart < start + length);
			return new TypeReference(enclosingElement, accuracy, start, length, insideDocComment, resource, simpleNameStart, null);
		}

		public static TypeReference createImportReference(IJavaElement enclosingElement, int accuracy, int start, int length,
				boolean insideDocComment, IResource resource) {
			return new TypeReference(enclosingElement, accuracy, start, length, insideDocComment, resource, -1, null);
		}

		public static TypeReference createSimpleReference(IJavaElement enclosingElement, int accuracy, int start, int length,
				boolean insideDocComment, IResource resource, String simpleName) {
			return new TypeReference(enclosingElement, accuracy, start, length, insideDocComment, resource, -1, simpleName);
		}

		public boolean isImportDeclaration() {
			return SearchUtils.getEnclosingJavaElement(this).getAncestor(IJavaElement.IMPORT_DECLARATION) != null;
		}

		public boolean isQualified() {
			return fSimpleNameStart != -1;
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
