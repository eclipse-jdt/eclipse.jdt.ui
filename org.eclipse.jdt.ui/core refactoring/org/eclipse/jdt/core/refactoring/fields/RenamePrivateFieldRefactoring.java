/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.core.refactoring.fields;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.IChange;
import org.eclipse.jdt.core.refactoring.RefactoringStatus;
import org.eclipse.jdt.core.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.core.refactoring.text.ITextBufferChange;
import org.eclipse.jdt.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.core.refactoring.text.SimpleReplaceTextChange;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.Checks;
import org.eclipse.jdt.internal.core.refactoring.CompositeChange;
import org.eclipse.jdt.internal.core.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.core.refactoring.SearchResult;
import org.eclipse.jdt.internal.core.util.HackFinder;

/**
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public class RenamePrivateFieldRefactoring extends FieldRefactoring implements IRenameRefactoring{
	/*
	 * NOTE: This class will be split into RenameFieldRefactoring and RenamePrivateFieldRefactoring
	 */

	private String fNewName;
	private List fOccurrences;
	private ITextBufferChangeCreator fTextBufferChangeCreator;
	
	public RenamePrivateFieldRefactoring(ITextBufferChangeCreator changeCreator, IField field){
		super(field);
		Assert.isNotNull(changeCreator, "change creator");
		fTextBufferChangeCreator= changeCreator;
		correctScope(field);
	}
	
	public RenamePrivateFieldRefactoring(ITextBufferChangeCreator changeCreator, IJavaSearchScope scope, IField field, String newName){
		super(scope, field);
		Assert.isNotNull(changeCreator, "change creator");
		fTextBufferChangeCreator= changeCreator;
		fNewName= newName;
		Assert.isNotNull(newName, "new name");
		correctScope(field);
	}
	
	/* non java-doc
	 * narrow down the scope
	 */ 
	private void correctScope(IField method){
		try{
			//only the declaring compilation unit
			setScope(SearchEngine.createJavaSearchScope(new IResource[]{getResource(getField())}));
		} catch (JavaModelException e){
			//do nothing
		}
	}
	
	/**
	 * @see IRefactoring#getName
	 */
	 public String getName(){
	 	return "RenamePrivateFieldRefactoring: " + getField().getElementName() + "(declared in " + getField().getDeclaringType().getFullyQualifiedName()+ ")" +" to " + fNewName;
	 }
	
	/**
	 * @see IRenameRefactoring#setNewName
	 */
	public void setNewName(String newName) {
		Assert.isNotNull(newName);
		fNewName= newName;
	}

	
	// ---------- Conditions -----------------
	
	/**
	 * @see IRenameRefactoring#checkNewName
	 */
	public RefactoringStatus checkNewName() {
		RefactoringStatus result= new RefactoringStatus();
		
		result.merge(Checks.checkFieldName(fNewName));
			
		if (Checks.isAlreadyNamed(getField(), fNewName))
			result.addFatalError("Choose another name.");
		if (getField().getDeclaringType().getField(fNewName).exists())
			result.addError("Field with this name already defined");
		return result;
	}
	
	/**
	 * @see Refactoring#checkInput
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);
		pm.subTask("checking preconditions");
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkNewName());
		HackFinder.fixMeSoon("must add precondions");
		/*
		 * I don't have to check the affected resources - private fields can be 
		 * referrenced only in the same compilation unit. And this one is checked by
		 * checkActivation. 
		 */
		pm.worked(1);
		result.merge(checkEnclosingHierarchy());
		pm.worked(1);
		result.merge(checkNestedHierarchy(getField().getDeclaringType()));
		pm.worked(1);
		pm.done();
		return result;
	}

	/**
	 * @see Refactoring#checkActivation
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		if (! Flags.isPrivate(getField().getFlags()))
			result.addFatalError("Only applicable to private fields.");
		result.merge(checkAvailability(getField()));	
		return result;
	}
	
	private RefactoringStatus checkNestedHierarchy(IType type) throws JavaModelException {
		IType[] nestedTypes= type.getTypes();
		if (nestedTypes == null)
			return null;
		RefactoringStatus result= new RefactoringStatus();	
		for (int i= 0; i < nestedTypes.length; i++){
			IField otherField= nestedTypes[i].getField(fNewName);
			if (otherField.exists())
				result.addWarning("After renaming, the field " + getField().getElementName() + " will be hidden in the scope of the field "+ fNewName + " declared in type " + nestedTypes[i].getFullyQualifiedName());
			result.merge(checkNestedHierarchy(nestedTypes[i]));	
		}	
		return result;
	}
	
	private RefactoringStatus checkEnclosingHierarchy() throws JavaModelException {
		IType current= getField().getDeclaringType();
		if (Checks.isTopLevel(current))
			return null;
		RefactoringStatus result= new RefactoringStatus();
		while (current != null){
			IField otherField= current.getField(fNewName);
			if (otherField.exists())
				result.addWarning("After renaming, the field named " + fNewName + " declared in type " + current.getFullyQualifiedName() + " will be hidden in the scope of the field " + getField().getElementName());
			current= current.getDeclaringType();
		}
		return result;
	}	

	// ---------- Changes -----------------
	/**
	 * @see IRefactoring#createChange
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("creating rename package change", 7);
		CompositeChange builder= new CompositeChange();
		getOccurrences(new SubProgressMonitor(pm, 6, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));
		addOccurrences(pm, builder);
		pm.worked(1);
		pm.done();
		return builder; 
	}
	
	private List getOccurrences(IProgressMonitor pm) throws JavaModelException{
		if (fOccurrences == null){
			if (pm == null)
				pm= new NullProgressMonitor();
			fOccurrences= RefactoringSearchEngine.search(new SubProgressMonitor(pm, 6, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK), getScope(), createSearchPattern());
		}	
		return fOccurrences;
	}
	
	private ISearchPattern createSearchPattern(){
		return SearchEngine.createSearchPattern(getField(), IJavaSearchConstants.ALL_OCCURRENCES);
	}
	
	private void addOccurrences(IProgressMonitor pm, CompositeChange builder) throws JavaModelException{
		List grouped= getOccurrences(null);
		Assert.isTrue(grouped.size() == 1, "private field referenced outside if its cu");
		for (Iterator iter= grouped.iterator(); iter.hasNext();){
			List l= (List)iter.next();
			ITextBufferChange change= fTextBufferChangeCreator.create("Rename Filed", (ICompilationUnit)JavaCore.create(((SearchResult)l.get(0)).getResource()));
			for (Iterator subIter= l.iterator(); subIter.hasNext();){
				change.addSimpleTextChange(createTextChange((SearchResult)subIter.next()));
			}
			builder.addChange(change);
			pm.worked(1);
		}
		HackFinder.fixMeSoon("maybe add dispose() method?");
		fOccurrences= null; //to prevent memory leak
	}

	private SimpleReplaceTextChange createTextChange(SearchResult searchResult) {
		return new SimpleReplaceTextChange("Field Reference Update", searchResult.getStart(), searchResult.getEnd() - searchResult.getStart(), fNewName);
	}	
}
