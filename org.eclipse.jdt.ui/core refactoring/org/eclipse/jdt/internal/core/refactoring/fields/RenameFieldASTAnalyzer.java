/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.fields;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.RefactoringASTAnalyzer;
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;

public class RenameFieldASTAnalyzer extends RefactoringASTAnalyzer {
	
	private String fNewName;
	private char[] fNewNameArray;
	private IField fField;
	
	RenameFieldASTAnalyzer(String newName, IField field) throws JavaModelException {
		Assert.isTrue(field.exists());

		fNewNameArray= newName.toCharArray();
		fNewName= newName;
		fField= field;
	}
	
	// --- visit methods 
	
	public boolean visit(SingleNameReference singleNameReference, BlockScope scope) {
		if (sourceRangeOnList(singleNameReference) && nameDefinedInScope(fNewNameArray, scope))
			addError(singleNameReference);
		return true;
	}

	public boolean visit(QualifiedNameReference qualifiedNameReference, BlockScope scope) {
		if (sourceRangeOnList(qualifiedNameReference) && nameDefinedInScope(fNewNameArray, scope))
			addError(qualifiedNameReference);
		return true;
	}
	
	//-- helper methods 
	
	private void addError(AstNode node){
		addError(RefactoringCoreMessages.getFormattedString("RenameFieldASTAnalyzer.error", //$NON-NLS-1$
															new Object[]{cuFullPath(), new Integer(getLineNumber(node)), fNewName}));
	}
	
	//- type member analysis
	
	/* non java-doc
	 * overriden
	 */ 
	protected boolean checkMemberTypes(){
		return false;
	}
	
}