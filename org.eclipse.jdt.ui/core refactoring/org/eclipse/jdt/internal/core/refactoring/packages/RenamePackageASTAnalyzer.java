/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.packages;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;

import org.eclipse.jdt.internal.compiler.AbstractSyntaxTreeVisitorAdapter;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.ArrayAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.ArrayTypeReference;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.CastExpression;
import org.eclipse.jdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.InstanceOfExpression;
import org.eclipse.jdt.internal.compiler.ast.LocalTypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MemberTypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.problem.ProblemHandler;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.DebugUtils;
import org.eclipse.jdt.internal.core.refactoring.RefactoringASTAnalyzer;
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.core.refactoring.SearchResult;

/*
 * non java-doc
 * not API
 */
class RenamePackageASTAnalyzer extends RefactoringASTAnalyzer {

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
															new Object[]{cuFullPath(), new Integer(getLineNumber(node)), fFirstNameSegment}));
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
		//DebugUtils.dump("sourceRangeOnList:" + astNode.getClass().getName());
		//DebugUtils.dump("sourceRangeOnList:" + astNode);
		
		if (astNode instanceof SingleNameReference)
			return false;
		else if (astNode instanceof SingleTypeReference)
			return false;
		else return super.sourceRangeOnList(astNode);
	}
}
