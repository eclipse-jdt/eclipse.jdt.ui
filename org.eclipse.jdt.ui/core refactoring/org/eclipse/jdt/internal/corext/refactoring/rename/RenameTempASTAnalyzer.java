/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.rename;

import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NameReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.corext.refactoring.AbstractRefactoringASTAnalyzer;

public class RenameTempASTAnalyzer extends AbstractRefactoringASTAnalyzer{

	private LocalDeclaration fTempDeclaration;
	private String fNewName;
	private boolean fUpdateReferences;
	
	RenameTempASTAnalyzer(LocalDeclaration localDeclaration, String newName, boolean updateReferences) {
		fTempDeclaration= localDeclaration;
		fNewName= newName;
		fUpdateReferences= updateReferences;
	}
	
	private void addShadowingError(AstNode node){
		addError("Renaming will cause in shadowing in line: " + getLineNumber(node) + " Name: '" + fNewName + "' is already visible.", node.sourceStart, node.sourceEnd);
	}
	
	private void analyzeLocalReference(NameReference nameReference, BlockScope blockScope){
		char[] newName= fNewName.toCharArray();
		Binding newBinding= blockScope.getBinding(newName, blockScope.VARIABLE, nameReference);
		
		if (newBinding == null)
			return;
		if (newBinding instanceof ProblemBinding)	
			return;
		
		if (newBinding instanceof FieldBinding){
			FieldBinding fieldBinding= (FieldBinding)newBinding;
			if (fieldBinding == null) 
				return;
			ReferenceBinding declaringClass= fieldBinding.declaringClass;
			if (! (declaringClass instanceof SourceTypeBinding))
				return;
			SourceTypeBinding sourceTypeBinding= (SourceTypeBinding)declaringClass;
			if (sourceTypeBinding == null || sourceTypeBinding.scope == null)
				return;
			TypeDeclaration typeDeclaration= sourceTypeBinding.scope.referenceType();
			if (typeDeclaration.sourceStart <= fTempDeclaration.sourceStart)
				return;
			if (! typeDeclaration.scope.compilationUnitScope().equals(fTempDeclaration.binding.declaringScope.compilationUnitScope()))
				return;	
			addShadowingError(nameReference);	
			return;
		} else if (newBinding instanceof LocalVariableBinding){
			LocalVariableBinding localVariableBinding= (LocalVariableBinding)newBinding;
			if (localVariableBinding.declaration.sourceStart <= fTempDeclaration.sourceStart)
				return;
			addShadowingError(nameReference);
			return;	
		}
	}

	private static FieldDeclaration getFieldDeclaration(FieldBinding fieldBinding) {
		if (fieldBinding == null) 
			return null;
		ReferenceBinding declaringClass= fieldBinding.declaringClass;
		if (! (declaringClass instanceof SourceTypeBinding))
			return null;
		SourceTypeBinding sourceTypeBinding= (SourceTypeBinding)declaringClass;
		if (sourceTypeBinding == null || sourceTypeBinding.scope == null)
			return null;
		TypeDeclaration typeDeclaration= sourceTypeBinding.scope.referenceType();
		FieldDeclaration fieldDeclaration= typeDeclaration.declarationOf(fieldBinding);
		return fieldDeclaration;
	}
	
	//-- visitor methods
	
	public void acceptProblem(IProblem problem) {
		if (problem.isError())
			addFatalError("Compilation error in line " + problem.getSourceLineNumber() 
					+ " " + problem.getMessage(), problem.getSourceStart(), problem.getSourceEnd());
	}
	
	public boolean visit(SingleNameReference singleNameReference, BlockScope blockScope){
		if (singleNameReference.sourceStart < fTempDeclaration.sourceStart)
			return true;
		
		if (fUpdateReferences && singleNameReference.binding instanceof LocalVariableBinding){
			LocalVariableBinding localBinding= (LocalVariableBinding)singleNameReference.binding;
			if (fTempDeclaration.equals(localBinding.declaration)){
				analyzeLocalReference(singleNameReference, blockScope);
				return true;
			}
		} else if (fNewName.equals(new String(singleNameReference.token))){
				if (singleNameReference.binding instanceof FieldBinding){
					FieldDeclaration fieldDeclaration = getFieldDeclaration((FieldBinding)singleNameReference.binding);
					if (fieldDeclaration != null && fieldDeclaration.sourceStart > fTempDeclaration.sourceStart)
						return true;
				}
			char[] newName= fTempDeclaration.name;
			Binding newBinding= blockScope.getBinding(newName, blockScope.VARIABLE, singleNameReference);
			if (fTempDeclaration.binding.equals(newBinding))
				addShadowingError(singleNameReference);
		}		
		return true;				
	}
		
	public boolean visit(QualifiedNameReference qualifiedNameReference, BlockScope blockScope) {
		if (qualifiedNameReference.sourceStart < fTempDeclaration.sourceStart)
			return true;
		
		if (fUpdateReferences && qualifiedNameReference.binding instanceof LocalVariableBinding){
			LocalVariableBinding localBinding= (LocalVariableBinding)qualifiedNameReference.binding;
			if (fTempDeclaration.equals(localBinding.declaration)){
				analyzeLocalReference(qualifiedNameReference, blockScope);
				return true;
			}
		} else if (fNewName.equals(new String(qualifiedNameReference.tokens[0]))){
			if (qualifiedNameReference.binding instanceof FieldBinding){
				FieldDeclaration fieldDeclaration = getFieldDeclaration((FieldBinding)qualifiedNameReference.binding);
				if (fieldDeclaration != null && fieldDeclaration.sourceStart > fTempDeclaration.sourceStart)
					return true;
			}
			
			char[] newName= fTempDeclaration.name;
			Binding newBinding= blockScope.getBinding(newName, blockScope.VARIABLE, qualifiedNameReference);
			if (fTempDeclaration.binding.equals(newBinding))
				addShadowingError(qualifiedNameReference);
		}	
		return true;
	}
}

