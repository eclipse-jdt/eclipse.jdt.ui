/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.structure;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.corext.refactoring.AbstractRefactoringASTAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;

class RenameParameterASTAnalyzer extends AbstractRefactoringASTAnalyzer{
	private Map fRenamings;
	private ISourceRange fMethodSourceRange;
	private int fMethodSourceRangeEnd;
	
	private List fParamBindings;
	
	RenameParameterASTAnalyzer(IMethod method, Map renamings) throws JavaModelException{
		fRenamings= renamings;
		fMethodSourceRange= method.getSourceRange();
		fMethodSourceRangeEnd= fMethodSourceRange.getOffset() + fMethodSourceRange.getLength();
	}
	
	private void addShadowingError(AstNode node, char[] name){
		addError(RefactoringCoreMessages.getFormattedString("RenameParameterASTAnalyzer.error",  //$NON-NLS-1$
															new Object[]{new Integer(getLineNumber(node)), new String(name)}),
															node.sourceStart, node.sourceEnd);
	}
	
	private boolean withinMethod(AstNode node){
		return     (node.sourceStart >= fMethodSourceRange.getOffset()) 
				&& (node.sourceStart <= fMethodSourceRangeEnd);
	}
	
	private boolean isOneOfOldNames(char[] name){
		return fRenamings.keySet().contains(new String(name));
	}
	
	private boolean isOneOfNewNames(char[] name){
		return fRenamings.values().contains(new String(name));
	}
	
	private boolean willBeShadowed(SingleNameReference singleNameReference, BlockScope blockScope){
		if (! isOneOfNewNames(singleNameReference.token))
			return false;
		if (isOneOfOldNames(singleNameReference.token))
			return false;
		if (singleNameReference.binding instanceof LocalVariableBinding)
			return false;	
		if (singleNameReference.binding instanceof FieldBinding){	
			ReferenceBinding declaringClass= ((FieldBinding)singleNameReference.binding).declaringClass;
			if (declaringClass instanceof SourceTypeBinding)
				return ! withinMethod(((SourceTypeBinding)declaringClass).scope.referenceContext);
			else
				return false;
		}
		return true;	
	}
	
	private boolean isInConflict(LocalDeclaration localDeclaration, BlockScope blockScope){
		return (isOneOfNewNames(localDeclaration.name)
				&& (! isOneOfOldNames(localDeclaration.name)));
	}
	
	//must be static and not-private
	//used by the OffsetFinder
	static List getArgumentBindings(AbstractMethodDeclaration methodDeclaration){
		Argument[] arguments= methodDeclaration.arguments;
		List list= new ArrayList(arguments.length);
		for (int i= 0; i < arguments.length; i++)
			list.add(arguments[i].binding);
		return list;
	}
	
	private void analyzeArgumentReference(SingleNameReference singleNameReference, BlockScope blockScope){
		String oldName= new String(singleNameReference.token);
		if (! fRenamings.keySet().contains(oldName))
			return;
			
		char[] newName= ((String)fRenamings.get(oldName)).toCharArray();
		Binding newBinding= blockScope.getBinding(newName, blockScope.VARIABLE, singleNameReference);
		
		if (newBinding == null)
			return;
		if (newBinding instanceof ProblemBinding)	
			return;
		if (fParamBindings.contains(newBinding))
			return;
				
		if (newBinding instanceof FieldBinding){	
			ReferenceBinding declaringClass= ((FieldBinding)newBinding).declaringClass;
			if (declaringClass instanceof SourceTypeBinding)
				if (withinMethod(((SourceTypeBinding)declaringClass).scope.referenceContext))
					addShadowingError(singleNameReference, singleNameReference.token);
		} else if (newBinding instanceof LocalVariableBinding){
			if (withinMethod(((LocalVariableBinding)newBinding).declaration))
				addShadowingError(singleNameReference, singleNameReference.token);
		}
	}	
	
	// ----- visit methods -------
	
	public void acceptProblem(IProblem problem) {
		if (problem.isError())
			addFatalError("Compilation error in line " + problem.getSourceLineNumber() 
					+ " " + problem.getMessage(), problem.getSourceStart(), problem.getSourceEnd());
	}
	
	public boolean visit(SingleNameReference singleNameReference, BlockScope blockScope){
		if (! withinMethod(singleNameReference))
			return true;
		
		if (fParamBindings.contains(singleNameReference.binding))
			analyzeArgumentReference(singleNameReference, blockScope);
		else if (willBeShadowed(singleNameReference, blockScope))
			addShadowingError(singleNameReference, singleNameReference.token);
		return true;
	}
	
	public boolean visit(QualifiedNameReference qualifiedNameReference,	BlockScope scope) {
		if (! withinMethod(qualifiedNameReference))
			return true;
		
		if (isOneOfOldNames(qualifiedNameReference.tokens[0]))
			return true;
			
		if (isOneOfNewNames(qualifiedNameReference.tokens[0]))
			addShadowingError(qualifiedNameReference, qualifiedNameReference.tokens[0]);
			
		return true;
	}
	
	public boolean visit(LocalDeclaration localDeclaration, BlockScope blockScope){
		if (! withinMethod(localDeclaration))
			return true;
			
		if ((!fParamBindings.contains(localDeclaration.binding))
			&& isInConflict(localDeclaration, blockScope))
				addError(RefactoringCoreMessages.getFormattedString("RenameParameterASTAnalyzer.error2", //$NON-NLS-1$
																	new Object[]{new Integer(getLineNumber(localDeclaration)),
																				 new String(localDeclaration.name)}),
								localDeclaration.sourceStart, localDeclaration.sourceEnd);
		return true;
	}
	
	public boolean visit(Argument argument, BlockScope blockScope){
		if (! withinMethod(argument))
			return true;
		
		if (isInConflict(argument, blockScope))
			addShadowingError(argument, argument.name);
		return true;
	}
	
	public boolean visit(MethodDeclaration methodDeclaration, ClassScope scope) {
		if (methodDeclaration.declarationSourceStart == fMethodSourceRange.getOffset())
			fParamBindings= getArgumentBindings(methodDeclaration);
		return true;
	}
	
	public boolean visit(ConstructorDeclaration constructorDeclaration, ClassScope scope) {
		if (constructorDeclaration.declarationSourceStart == fMethodSourceRange.getOffset())
			fParamBindings= getArgumentBindings(constructorDeclaration);
		return true;
	}
}