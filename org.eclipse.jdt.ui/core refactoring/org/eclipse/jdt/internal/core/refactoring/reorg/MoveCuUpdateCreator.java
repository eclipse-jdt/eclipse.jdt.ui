/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.reorg;

import java.util.Arrays;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.CompositeChange;
import org.eclipse.jdt.internal.core.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.core.refactoring.SearchResult;
import org.eclipse.jdt.internal.core.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.core.refactoring.Utils;
import org.eclipse.jdt.internal.core.refactoring.base.ICompositeChange;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBuffer;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChange;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.core.refactoring.text.SimpleReplaceTextChange;
import org.eclipse.jdt.internal.core.refactoring.text.SimpleTextChange;
import org.eclipse.jdt.internal.core.refactoring.util.TextBufferChangeManager;

public class MoveCuUpdateCreator {
	
	private ITextBufferChangeCreator fTextBufferChangeCreator;
	private ICompilationUnit[] fCus;
	private IPackageFragment fDestination;
	
	public MoveCuUpdateCreator(ITextBufferChangeCreator changeCreator, ICompilationUnit cu, IPackageFragment pack){
		this(changeCreator, new ICompilationUnit[]{cu}, pack);
	}
	
	public MoveCuUpdateCreator(ITextBufferChangeCreator changeCreator, ICompilationUnit[] cus, IPackageFragment pack){
		Assert.isNotNull(changeCreator);
		Assert.isNotNull(cus);
		Assert.isNotNull(pack);
		fCus= cus;
		fDestination= pack;
		fTextBufferChangeCreator= changeCreator;
	}
	
	public ICompositeChange createUpdateChange(IProgressMonitor pm)throws JavaModelException{
		pm.beginTask("", 2 * fCus.length);
		try{
			CompositeChange composite= new CompositeChange("reorganize elements", fCus.length);
			
			TextBufferChangeManager changeManager= new TextBufferChangeManager(fTextBufferChangeCreator);
			for (int i= 0; i < fCus.length; i++){
				if (pm.isCanceled())
					throw new OperationCanceledException();
					
				addUpdateChanges(changeManager, fCus[i], new SubProgressMonitor(pm, 1));
			}
			
			ITextBufferChange[] allChanges= changeManager.getAllChanges();
			for (int i= 0; i < allChanges.length; i++){
				if (pm.isCanceled())
					throw new OperationCanceledException();
				
				composite.addChange(allChanges[i]);
				pm.worked(1);
			}
			return composite;
		} finally{
			pm.done();
		}
	}
	
	private void addUpdateChanges(TextBufferChangeManager changeManager, ICompilationUnit unit, IProgressMonitor pm)throws JavaModelException{
		try{
			pm.beginTask("", 1); 
		  	pm.subTask("searching for references to types in " + unit.getElementName());
		  	
		  	addImport(changeManager, getPackage(unit), unit);
			SearchResultGroup[] references = getReferences(unit, pm);
		  	
			for (int i= 0; i < references.length; i++){
				ICompilationUnit cu= (ICompilationUnit)JavaCore.create(references[i].getResource());
				boolean hasSimpleReference= false;
				SearchResult[] results= references[i].getSearchResults();
				for (int j= 0; j < results.length; j++){
					boolean isQualified= isQualifiedReference(results[j]);
					hasSimpleReference= hasSimpleReference || (!isQualified);
					if (isQualified)
						changeManager.addSimpleTextChange(cu, createTextChange(results[j], unit));
				}
				if (hasSimpleReference 
					&& (!cu.equals(unit)) 
					&& (!Arrays.asList(fCus).contains(cu))
					&& (!cu.getImport(Utils.getPublicType(unit).getFullyQualifiedName()).exists())){
						addImport(changeManager, fDestination, cu);
				}	
		  }
		  
		} finally{
			pm.done();
		}
	}

	private SearchResultGroup[] getReferences(ICompilationUnit unit, IProgressMonitor pm) throws org.eclipse.jdt.core.JavaModelException {
		IJavaSearchScope scope= SearchEngine.createWorkspaceScope();
		ISearchPattern pattern= createSearchPattern(unit);
		SearchResultGroup[] references= RefactoringSearchEngine.customSearch(new SubProgressMonitor(pm, 1), scope, pattern);
		return references;
	}
	
	/* non java doc
	 * returns <code>true</code> if the import declaration has been added and <code>false</code> otherwise.
	 */
	private static boolean addImport(TextBufferChangeManager changeManager, final IPackageFragment pack, ICompilationUnit cu) throws JavaModelException {
		if (cu.getImport(pack.getElementName() + ".*").exists()) //$NON-NLS-1$
			return false;
					
		int start = computeStart(cu);
		
		changeManager.addSimpleTextChange(cu, new SimpleReplaceTextChange("add import", start + 1){
			public SimpleTextChange[] adjust(ITextBuffer buffer) {
				String lineDelimiter= buffer.getLineDelimiter(buffer.getLineOfOffset(getOffset()));
				setText(lineDelimiter + "import " + pack.getElementName() + ".*;");//$NON-NLS-2$ //$NON-NLS-1$
				return null;
			}
		});
		return true;
	}

	private static boolean isQualifiedReference(SearchResult searchResult){
		if (searchResult.isQualified())
			return true;
		if (searchResult.getEnclosingElement() instanceof IImportDeclaration)
			return true;
		return false;	
	}
	
	private SimpleReplaceTextChange createTextChange(SearchResult searchResult, final ICompilationUnit cu) throws JavaModelException{
		return new SimpleReplaceTextChange("update reference", searchResult.getStart(), 
											searchResult.getEnd() - searchResult.getStart(),
											fDestination.getElementName()) {
			protected SimpleTextChange[] adjust(ITextBuffer buffer) {
				String oldText= buffer.getContent(getOffset(), getLength());
				String packageName= getPackage(cu).getElementName();
				if (getPackage(cu).isDefaultPackage()){
					setLength(0);
				} else if (! oldText.startsWith(packageName)){
					//no action - simple reference
					setLength(0);
					setText(""); //$NON-NLS-1$
				} else{
					setLength(packageName.length());					
				}
				return null;
			}
		};
	}
	
	private static int computeStart(ICompilationUnit cu) throws JavaModelException {
		IImportContainer importContainer= cu.getImportContainer();
		if (importContainer.exists()){
			ISourceRange sr= importContainer.getSourceRange();
			return sr.getOffset() + sr.getLength() - 1;
		}
		
		IPackageDeclaration declars[]= cu.getPackageDeclarations();
		if (declars.length == 0)
			return 0;
		ISourceRange sr= declars[declars.length - 1].getSourceRange();
		return sr.getOffset() + sr.getLength() - 1;
	}
	
	private static IPackageFragment getPackage(ICompilationUnit cu){
		return (IPackageFragment)cu.getParent();
	}
	
	private static ISearchPattern createSearchPattern(ICompilationUnit cu) throws JavaModelException{
		IType publicType= Utils.getPublicType(cu);
		if (publicType == null)
			return null; 
		else
			return SearchEngine.createSearchPattern(publicType, IJavaSearchConstants.REFERENCES);
	}
	
}

