/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.core.refactoring.methods;

import java.util.ArrayList;import java.util.Arrays;import java.util.List;
import org.eclipse.jdt.core.IMethod;import org.eclipse.jdt.core.ISourceRange;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.compiler.ast.*;import org.eclipse.jdt.internal.compiler.lookup.*;import org.eclipse.jdt.internal.core.refactoring.AbstractRefactoringASTAnalyzer;import org.eclipse.jdt.internal.core.refactoring.RefactoringASTAnalyzer;

/*
 * not api
 */
class RenameParameterASTAnalyzer extends AbstractRefactoringASTAnalyzer{
	
	private String[] fNewParameterNames;
	private String[] fSortedNewParameterNames;
	
	private String[] fOldParameterNames;
	private String[] fSortedOldParameterNames;
	
	private ISourceRange fMethodSourceRange;
	private int fMethodSourceRangeEnd;
	
	private List fParamBindings;
	
	RenameParameterASTAnalyzer(IMethod method, String[] newParameterNames) throws JavaModelException{
		fNewParameterNames= newParameterNames;
		fOldParameterNames= method.getParameterNames();
		fSortedNewParameterNames= getSortedCopy(fNewParameterNames);
		fSortedOldParameterNames= getSortedCopy(fOldParameterNames);
		
		fMethodSourceRange= method.getSourceRange();
		fMethodSourceRangeEnd= fMethodSourceRange.getOffset() + fMethodSourceRange.getLength();
	}
	
	private String[] getSortedCopy(String[] array){
		//should we use arrayCopy?
		String[] copy= (String[])array.clone();
		Arrays.sort(copy);
		return copy;
	}
			
	private void addShadowingError(AstNode node, char[] name){
		addError("Problem in line:" + getLineNumber(node) + " Reference " + new String(name) + " already visible.");
	}
	
	private void addShadowingError(String flag, AstNode node, char[] name){
		addError(flag + " Problem in line:" + getLineNumber(node) + " Reference " + new String(name) + " already visible.");
	}
	
	private boolean withinMethod(AstNode node){
		return     (node.sourceStart >= fMethodSourceRange.getOffset()) 
				&& (node.sourceStart <= fMethodSourceRangeEnd);
	}
	
	private boolean isOneOfOldNames(char[] name){
		return indexInOldNames(name) >= 0;
	}
	
	private boolean isOneOfNewNames(char[] name){
		return indexInNewNames(name) >= 0;
	}
	
	private int indexInOldNames(char[] name){
		return Arrays.binarySearch(fSortedOldParameterNames, new String(name));
	}
	
	//enough to check the renamed names
	//but we check all
	private int indexInNewNames(char[] name){
		return Arrays.binarySearch(fSortedNewParameterNames, new String(name));
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
	static List getArgumentBindings(MethodDeclaration methodDeclaration){
		Argument[] arguments= methodDeclaration.arguments;
		List list= new ArrayList(arguments.length);
		for (int i= 0; i < arguments.length; i++)
			list.add(arguments[i].binding);
		return list;
	}
	
	private void analyzeArgumentReference(SingleNameReference singleNameReference, BlockScope blockScope){
		int index= indexInOldNames(singleNameReference.token);
		if (index < 0 )
			return;
			
		char[] newName= fNewParameterNames[index].toCharArray();
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
	
	//extract fails
	private int foo(){
		throw new NullPointerException();
	}
	
	public boolean visit(LocalDeclaration localDeclaration, BlockScope blockScope){
		if (! withinMethod(localDeclaration))
			return true;
			
		if ((!fParamBindings.contains(localDeclaration.binding))
			&& isInConflict(localDeclaration, blockScope))
				addError("Problem in line:" + getLineNumber(localDeclaration) + " Local variable named " 
						+ new String(localDeclaration.name) + " already exists.");
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
	
	
}