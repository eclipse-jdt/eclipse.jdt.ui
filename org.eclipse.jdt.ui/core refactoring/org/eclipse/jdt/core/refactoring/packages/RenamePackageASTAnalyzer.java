/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.core.refactoring.packages;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.Refactoring;
import org.eclipse.jdt.core.refactoring.RefactoringStatus;

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
import org.eclipse.jdt.internal.core.refactoring.SearchResult;
import org.eclipse.jdt.internal.core.util.HackFinder;

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
		HackFinder.fixMeLater("how much too restrictive is it?"); 
		if (newName.indexOf(".") != -1)
			fFirstNameSegment= newName.substring(0, newName.indexOf("."));
		else 	
			fFirstNameSegment= newName;
			
		fFirstNameSegmentArray= fFirstNameSegment.toCharArray();
		
	}

	public boolean visit(MessageSend messageSend, BlockScope scope) {
		if (messageSend.receiver != ThisReference.ThisImplicit && sourceRangeOnList(messageSend.receiver) && nameDefinedInScope(fFirstNameSegment, scope)) {
			addWarning(messageSend);
		}
		return true;
	}

	public boolean visit(QualifiedNameReference qualifiedNameReference, BlockScope scope) {
		if (super.sourceRangeOnList(qualifiedNameReference) && nameDefinedInScope(fFirstNameSegment, scope)) {
			addWarning(qualifiedNameReference);
		}
		return true;
	}
	
	public boolean visit(QualifiedTypeReference qualifiedTypeReference, ClassScope scope) {
		if (super.sourceRangeOnList(qualifiedTypeReference) && nameDefinedInScope(fFirstNameSegment, scope)) {
			addWarning(qualifiedTypeReference);
		}
		return true;
	}
	
	public boolean visit(QualifiedTypeReference qualifiedTypeReference, BlockScope scope) {
		if (super.sourceRangeOnList(qualifiedTypeReference) && nameDefinedInScope(fFirstNameSegment, scope)) {
			addWarning(qualifiedTypeReference);
		}
		return true;
	}

	public boolean visit(CastExpression castExpression, BlockScope scope) {
		if (isNewNameHiddenByAnotherType(castExpression.type, scope))
			addWarning(castExpression);
		return true;
	}

	public boolean visit(ClassLiteralAccess classLiteral, BlockScope scope) {
		if (isNewNameHiddenByAnotherType(classLiteral.type, scope))
			addWarning(classLiteral);
		return true;
	}

	public boolean visit(InstanceOfExpression instanceOfExpression, BlockScope scope) {
		if (isNewNameHiddenByAnotherType(instanceOfExpression.type, scope))
			addWarning(instanceOfExpression);
		return true;
	}

	public boolean visit(ArrayAllocationExpression arrayAllocationExpression, BlockScope scope) {
		if (isNewNameHiddenByAnotherType(arrayAllocationExpression.type, scope))
			addWarning(arrayAllocationExpression);
		return true;
	}

	public boolean visit(MethodDeclaration methodDeclaration, ClassScope scope) {
		//if (methodDeclaration.isNative() && typeUsedAsParameter(methodDeclaration))
		//	addWarning(fType.getFullyQualifiedName() + " is used as a parameter type for a native method " + new String(methodDeclaration.selector) + " in " + cuFullPath()); 
		return true;
	}


	public boolean visit(SingleTypeReference singleTypeReference, BlockScope scope) {
		if (isNewNameHiddenByAnotherType(singleTypeReference, scope))
			addWarning(singleTypeReference);
		return true;
	}

	public boolean visit(ArrayTypeReference arrayTypeReference, BlockScope scope) {
		if (sourceRangeOnList(arrayTypeReference.sourceStart, arrayTypeReference.sourceStart + arrayTypeReference.token.length) && localTypeExists(scope, fFirstNameSegmentArray)) {
			addWarning(arrayTypeReference);
		}
		return true;
	}

	//-----------------------------------------------------------
	
	private void addWarning(AstNode node){
		addWarning("Possible problems in \"" + cuFullPath() + "\" (line number: " + getLineNumber(node) + "). Name " + fFirstNameSegment + " is already used.");
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
	
	
	private static boolean nameDefinedInScope(String newName, CompilationUnitScope scope){
		if (scope.topLevelTypes == null)
			return false;
		for (int i= 0; i < scope.topLevelTypes.length; i++){
			if (newName.equals(new String(scope.topLevelTypes[i].sourceName)))
				return true;
		}		
		return false;
	}
	
	private static boolean nameDefinedInScope(String newName, ClassScope scope){
		HackFinder.fixMeSoon("Rename Type analyzer needs this fix (CompilationUnitScope) too?");
		if (scope == null)
			return false;
		if (nameDefinedInType(newName, scope.referenceContext.binding))
			return true;
		if (scope.parent instanceof ClassScope)	
			return nameDefinedInScope(newName, (ClassScope)scope.parent);
		else if (scope.parent instanceof BlockScope)	
			return nameDefinedInScope(newName, (BlockScope)scope.parent);
		else if (scope.parent instanceof CompilationUnitScope)
			return nameDefinedInScope(newName, (CompilationUnitScope)scope.parent);
		else	
			return false;
	}
	
	private static boolean nameDefinedInScope(String newName, BlockScope scope){
		if (scope == null)
			return false;
		if (scope.locals != null){
			for (int i= 0; i < scope.locals.length; i++){
				if (scope.locals[i] != null){
					String name= (scope.locals[i].name != null) ? new String(scope.locals[i].name) : "";
					if (new String(name).equals(newName))
						return true;
				}	
			}
		}
		if (scope.parent instanceof BlockScope)
			return nameDefinedInScope(newName, (BlockScope)scope.parent);
		else if (scope.parent instanceof ClassScope)
			return nameDefinedInScope(newName, (ClassScope)scope.parent);
		else 
			return false;
	}
	
	private static boolean nameDefinedInType(String newName, SourceTypeBinding sourceTypeBinding){
		if (sourceTypeBinding.fields != null){
			for (int i= 0; i < sourceTypeBinding.fields.length; i++){
				if (newName.equals(new String(sourceTypeBinding.fields[i].name)))
					return true;
			}
		}
		if (sourceTypeBinding.memberTypes != null){
			for (int i= 0; i < sourceTypeBinding.memberTypes.length; i++){
				if (newName.equals(new String(sourceTypeBinding.memberTypes[i].sourceName)))
					return true;
			}
		}
		
		if ((sourceTypeBinding.superclass != null)
			&& (sourceTypeBinding.superclass instanceof SourceTypeBinding)
			&& nameDefinedInType(newName, (SourceTypeBinding)sourceTypeBinding.superclass))
				return true;
				
		if (sourceTypeBinding.superInterfaces != null){
			for (int i= 0; i < sourceTypeBinding.superInterfaces.length; i++){
				if ((sourceTypeBinding.superInterfaces[i] instanceof SourceTypeBinding)
					&& nameDefinedInType(newName, (SourceTypeBinding)sourceTypeBinding.superInterfaces[i]))
						return true;
			}
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
