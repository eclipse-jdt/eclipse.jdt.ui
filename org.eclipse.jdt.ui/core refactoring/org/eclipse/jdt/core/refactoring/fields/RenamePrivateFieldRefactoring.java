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
public class RenamePrivateFieldRefactoring extends RenameFieldRefactoring{

	public RenamePrivateFieldRefactoring(ITextBufferChangeCreator changeCreator, IField field){
		super(changeCreator, field);
		correctScope();
	}
	
	/* non java-doc
	 * this constructor is only for consistency - should be replaced by:
	 * RenamePrivateFieldRefactoring(ITextBufferChangeCreator, IField, String)
	 */ 
	public RenamePrivateFieldRefactoring(ITextBufferChangeCreator changeCreator, IJavaSearchScope scope, IField field, String newName){
		super(changeCreator, scope, field, newName);
		correctScope();
	}
	
	/* non java-doc
	 * narrow down the scope
	 */ 
	private void correctScope(){
		if (getField().isBinary())
			return;
		try{
			//only the declaring compilation unit
			setScope(SearchEngine.createJavaSearchScope(new IResource[]{getResource(getField())}));
		} catch (JavaModelException e){
			//do nothing
		}
	}
	
	// ---------- Conditions -----------------
		
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
}
