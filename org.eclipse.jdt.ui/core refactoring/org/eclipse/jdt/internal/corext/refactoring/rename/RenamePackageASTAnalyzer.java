/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.rename;


import org.eclipse.jdt.internal.compiler.ast.ArrayAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.ArrayTypeReference;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.CastExpression;
import org.eclipse.jdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.jdt.internal.compiler.ast.InstanceOfExpression;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;


/*
 * non java-doc
 * not API
 */
class RenamePackageASTAnalyzer extends RenameRefactoringASTAnalyzer {


	private String fFirstNameSegment;
	private char[] fFirstNameSegmentArray;


	RenamePackageASTAnalyzer(String newName){
		/*
		 * only get the first segment
		 */
		if (newName.indexOf(".") != -1) //$NON-NLS-1$
			fFirstNameSegment= newName.substring(0, newName.indexOf(".")); //$NON-NLS-1$
		else 	
			fFirstNameSegment= newName;
			
		fFirstNameSegmentArray= fFirstNameSegment.toCharArray();
		
	}


	public boolean visit(MessageSend messageSend, BlockScope scope) {
		if (messageSend.receiver != ThisReference.ThisImplicit && sourceRangeOnList(messageSend.receiver) && nameDefinedInScope(fFirstNameSegmentArray, scope)) {
			addError(messageSend);
		}
		return true;
	}


	public boolean visit(QualifiedNameReference qualifiedNameReference, BlockScope scope) {
		if (super.sourceRangeOnList(qualifiedNameReference) && nameDefinedInScope(fFirstNameSegmentArray, scope)) {
			addError(qualifiedNameReference);
		}
		return true;
	}
	
	public boolean visit(QualifiedTypeReference qualifiedTypeReference, ClassScope scope) {
		if (super.sourceRangeOnList(qualifiedTypeReference) && nameDefinedInScope(fFirstNameSegmentArray, scope)) {
			addError(qualifiedTypeReference);
		}
		return true;
	}
	
	public boolean visit(QualifiedTypeReference qualifiedTypeReference, BlockScope scope) {
		if (super.sourceRangeOnList(qualifiedTypeReference) && nameDefinedInScope(fFirstNameSegmentArray, scope)) {
			addError(qualifiedTypeReference);
		}
		return true;
	}


	public boolean visit(CastExpression castExpression, BlockScope scope) {
		if (isNewNameHiddenByAnotherType(castExpression.type, scope))
			addError(castExpression);
		return true;
	}


	public boolean visit(ClassLiteralAccess classLiteral, BlockScope scope) {
		if (isNewNameHiddenByAnotherType(classLiteral.type, scope))
			addError(classLiteral);
		return true;
	}


	public boolean visit(InstanceOfExpression instanceOfExpression, BlockScope scope) {
		if (isNewNameHiddenByAnotherType(instanceOfExpression.type, scope))
			addError(instanceOfExpression);
		return true;
	}


	public boolean visit(ArrayAllocationExpression arrayAllocationExpression, BlockScope scope) {
		if (isNewNameHiddenByAnotherType(arrayAllocationExpression.type, scope))
			addError(arrayAllocationExpression);
		return true;
	}


	public boolean visit(SingleTypeReference singleTypeReference, BlockScope scope) {
		if (isNewNameHiddenByAnotherType(singleTypeReference, scope))
			addError(singleTypeReference);
		return true;
	}


	public boolean visit(ArrayTypeReference arrayTypeReference, BlockScope scope) {
		if (sourceRangeOnList(arrayTypeReference.sourceStart, arrayTypeReference.sourceStart + arrayTypeReference.token.length) && localTypeExists(scope, fFirstNameSegmentArray)) {
			addError(arrayTypeReference);
		}
		return true;
	}


	//-----------------------------------------------------------
	
	private void addError(AstNode node){
		addError(RefactoringCoreMessages.getFormattedString("RenamePackageASTAnalyzer.error",  //$NON-NLS-1$
															new Object[]{cuFullPath(), new Integer(getLineNumber(node)), fFirstNameSegment}),
						node.sourceStart, node.sourceEnd);
	}
	
	private boolean isNewNameHiddenByAnotherType(AstNode node, Scope scope) {
		if (! sourceRangeOnList(node)) 
			return false;
		return (localTypeExists(scope, fFirstNameSegmentArray) || typeExists(scope, fFirstNameSegmentArray));
	}
		
	private static final boolean typeExists(Scope scope, char[] name){
		/*
		 * should maybe do sth with unresolved bindings
		 */
		TypeBinding binding= scope.getType(name);
		return (binding != null && !(binding instanceof ProblemReferenceBinding));
	}
	
	private static final boolean localTypeExists(Scope scope, char[] name){
		Scope current= scope;
		while ((current != null) && ! (current instanceof CompilationUnitScope)){
			if ((current instanceof BlockScope) && ((BlockScope)current).findLocalType(name) != null)
				return true;
			current= current.parent;	
		}
		return false;
	}
	
	
	private boolean typeUsedAsParameter(MethodDeclaration methodDeclaration){
		if (methodDeclaration.arguments == null)
			return false;
		for (int i= 0; i < methodDeclaration.arguments.length; i ++){
			if (sourceRangeOnList(methodDeclaration.arguments[i].type))
				return true;
		}
		return false;
	}
	
	/* overriden
	 */
	protected boolean sourceRangeOnList(AstNode astNode){
		if (astNode instanceof SingleNameReference)
			return false;
		else if (astNode instanceof SingleTypeReference)
			return false;
		else return super.sourceRangeOnList(astNode);
	}
}
