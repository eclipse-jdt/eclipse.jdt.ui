/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.core.refactoring.fields;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.RefactoringStatus;
import org.eclipse.jdt.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.core.search.IJavaSearchScope;

/**
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public class RenameNonPrivateFieldRefactoring extends RenameFieldRefactoring {

	public RenameNonPrivateFieldRefactoring(ITextBufferChangeCreator changeCreator, IField field){
		super(changeCreator, field);
	}
	
	public RenameNonPrivateFieldRefactoring(ITextBufferChangeCreator changeCreator, IJavaSearchScope scope, IField field, String newName){
		super(changeCreator, scope, field, newName);
	}

	/**
	 * @see Refactoring#checkActivation
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		if (Flags.isPrivate(getField().getFlags()))
			result.addFatalError("Not applicable to private fields.");
		result.merge(checkAvailability(getField()));	
		return result;
	}
}