/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.util.CharOperation;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.corext.refactoring.AbstractRefactoringASTAnalyzer;

class ParameterOffsetFinder extends AbstractRefactoringASTAnalyzer{
	
	private IMethod fMethod;
	private char[] fParameterName; 
	private ISourceRange fMethodSourceRange;
	private int fMethodSourceRangeEnd;
	
	private List fOffsetsFound;
	private List fParamBindings;
	private boolean fIncludeReferences;
	
	private ParameterOffsetFinder(IMethod method, String parameterName, boolean includeReferences) throws JavaModelException{ 
		fMethod= method;
		fParameterName= parameterName.toCharArray();
		fMethodSourceRange= method.getSourceRange();
		fMethodSourceRangeEnd= fMethodSourceRange.getOffset() + fMethodSourceRange.getLength();
		fOffsetsFound= new ArrayList();
		fIncludeReferences= includeReferences;
	}

	/**
	 * @param method
	 * @param parameterName
	 * @param includeReferences if it is <code>true</code>, then not only the parameter declaration but also references will be included
	 * @return indices of offsets of the references to the parameter specified in constructor
	 */
	static int[] findOffsets(IMethod method, String parameterName, boolean includeReferences) throws JavaModelException{
		ParameterOffsetFinder instance= new ParameterOffsetFinder(method, parameterName, includeReferences);		
		((CompilationUnit)method.getCompilationUnit()).accept(instance);
		return convertFromIntegerList(instance.fOffsetsFound);
	}
	
	private static int[] convertFromIntegerList(List list){
		int[] result= new int[list.size()];
		Integer[] integerResult= (Integer[])list.toArray(new Integer[list.size()]);
		for (int i= 0; i < integerResult.length; i++){
			result[i]= integerResult[i].intValue();
		}
		return result;
	}
	
	private void addNodeOffset(AstNode node){
		fOffsetsFound.add(new Integer(node.sourceStart));
	}
	
	private boolean withinMethod(AstNode node){
		return (node.sourceStart >= fMethodSourceRange.getOffset()) 
			&& (node.sourceStart <= fMethodSourceRangeEnd);
	}
		
	private boolean isParameterMatch(SingleNameReference singleNameReference){
		if (! withinMethod(singleNameReference))
			return false;
		if (! CharOperation.equals(fParameterName, singleNameReference.token))
			return false;
		if (! fParamBindings.contains(singleNameReference.binding))
			return false;
		return true;	
	}
		
	//-------  visit methods  ---------
	
	public boolean visit(SingleNameReference singleNameReference, BlockScope blockScope){
		if (! fIncludeReferences)
			return true;
		
		if  (isParameterMatch(singleNameReference))
			addNodeOffset(singleNameReference);
		return true;
	}
	
	public boolean visit(LocalDeclaration localDeclaration, BlockScope scope) {
		if (! fIncludeReferences)
			return true;
		
		if (withinMethod(localDeclaration) 
			&&	fParamBindings.contains(localDeclaration.binding))
				fOffsetsFound.add(new Integer(localDeclaration.declarationSourceEnd - localDeclaration.name.length));
		return true;
	}
	
	public boolean visit(MethodDeclaration methodDeclaration, ClassScope scope) {
		if (methodDeclaration.declarationSourceStart == fMethodSourceRange.getOffset())
			fParamBindings= RenameParameterASTAnalyzer.getArgumentBindings(methodDeclaration);
		return true;
	}
	
	public boolean visit(ConstructorDeclaration constructorDeclaration, ClassScope scope) {
		if (constructorDeclaration.declarationSourceStart == fMethodSourceRange.getOffset())
			fParamBindings= RenameParameterASTAnalyzer.getArgumentBindings(constructorDeclaration);
		return true;
	}
		
	public boolean visit(Argument argument, BlockScope scope) {
		if (withinMethod(argument) 
		 	&& fParamBindings.contains(argument.binding)
		 	&& CharOperation.equals(argument.name, fParameterName))
				fOffsetsFound.add(new Integer(argument.declarationSourceEnd - argument.name.length + 1));		
		return true;
	}
		
	public boolean visit(QualifiedNameReference qualifiedNameReference,	BlockScope scope) {
		if (! fIncludeReferences)
			return true;
		
		if (withinMethod(qualifiedNameReference) 
			&& CharOperation.equals(qualifiedNameReference.tokens[0], fParameterName)){
				fOffsetsFound.add(new Integer(qualifiedNameReference.sourceStart));
		}	
		return true;
	}
}