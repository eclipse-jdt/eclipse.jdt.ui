/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.rename;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;

import org.eclipse.jdt.internal.corext.refactoring.AbstractRefactoringASTAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;


public class RenameFieldASTAnalyzer extends AbstractRefactoringASTAnalyzer {
	
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
															new Object[]{cuFullPath(), new Integer(getLineNumber(node)), fNewName}),
						node.sourceStart, node.sourceEnd);
	}
	
	//- type member analysis
	
	/* non java-doc
	 * overriden
	 */ 
	protected boolean checkMemberTypes(){
		return false;
	}
	
}