/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;

import org.eclipse.jdt.internal.compiler.AbstractSyntaxTreeVisitorAdapter;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.problem.ProblemHandler;
import org.eclipse.jdt.internal.compiler.util.CharOperation;
import org.eclipse.jdt.internal.core.CompilationUnit;

public abstract class RefactoringASTAnalyzer extends AbstractRefactoringASTAnalyzer{

	private SearchResult[] fSearchResults;
	
	public final RefactoringStatus analyze(SearchResult[] searchResults, ICompilationUnit cu) throws JavaModelException{
		fSearchResults= searchResults;
		return super.analyze(cu);
	}
	
	//-------
		
	protected boolean sourceRangeOnList(int start, int end){
		for (int i= 0; i < fSearchResults.length; i++){
			if (start == fSearchResults[i].getStart() && end == fSearchResults[i].getEnd())
				return true;
		}
		return false;
	}
	
	protected boolean sourceRangeOnList(QualifiedNameReference qualifiedNameReference){
		int start= qualifiedNameReference.sourceStart;
		return sourceRangeOnList(start, start + qualifiedNameReference.tokens[0].length);
	}
	
	protected boolean sourceRangeOnList(QualifiedTypeReference qualifiedTypeReference){
		int start= qualifiedTypeReference.sourceStart;
		return sourceRangeOnList(start, start + qualifiedTypeReference.tokens[0].length);
	}
	
	protected boolean sourceRangeOnList(AstNode astNode){		
		if (astNode instanceof QualifiedNameReference)
			return sourceRangeOnList((QualifiedNameReference)astNode);
		else if (astNode instanceof QualifiedTypeReference)
			return sourceRangeOnList((QualifiedTypeReference)astNode);
		
		return sourceRangeOnList(astNode.sourceStart, astNode.sourceEnd + 1);
	}
	
	// --- other helper methods ---------
	/*
	 * override at will
	 */
	protected boolean nameDefinedInScope(char[] newName, BlockScope scope){
		if (scope == null)
			return false;
		if (scope.locals != null){
			for (int i= 0; i < scope.locals.length; i++){
				if (scope.locals[i] != null){
					char[] name= (scope.locals[i].name != null) ? scope.locals[i].name : new char[0];
					if (CharOperation.equals(newName, name))
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

	/*
	 * override at will
	 */
	protected boolean nameDefinedInScope(char[] newName, ClassScope scope){
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

	protected boolean nameDefinedInScope(char[] newName, CompilationUnitScope scope){
		if (scope.topLevelTypes == null)
			return false;
		for (int i= 0; i < scope.topLevelTypes.length; i++){
			if (CharOperation.equals(newName, scope.topLevelTypes[i].sourceName))
				return true;
		}		
		return false;
	}
	
	//------- type member analysis
	
	/* non java-doc
	 * override to limit analysis
	 */ 
	protected boolean checkFields(){
		return true;
	}
	
	/* non java-doc
	 * override to limit analysis
	 */ 
	protected boolean checkMemberTypes(){
		return true;
	}
	
	/* non java-doc
	 * override to limit analysis
	 */ 
	protected boolean checkSuperClasses(){
		return true;
	}
	
	/* non java-doc
	 * override to limit analysis
	 */ 
	protected boolean checkSuperInterfaces(){
		return true;
	}
	
	protected boolean nameDefinedInType(char[] newName, SourceTypeBinding sourceTypeBinding){
		if (checkFields() && sourceTypeBinding.fields != null){
			for (int i= 0; i < sourceTypeBinding.fields.length; i++){
				if (CharOperation.equals(newName, sourceTypeBinding.fields[i].name))
					return true;
			}
		}
		if (checkMemberTypes() && sourceTypeBinding.memberTypes != null){
			for (int i= 0; i < sourceTypeBinding.memberTypes.length; i++){
				if (CharOperation.equals(newName, sourceTypeBinding.memberTypes[i].sourceName))
					return true;
			}
		}
		
		if (	checkSuperClasses()
			&& (sourceTypeBinding.superclass != null)
			&& (sourceTypeBinding.superclass instanceof SourceTypeBinding)
			&& nameDefinedInType(newName, (SourceTypeBinding)sourceTypeBinding.superclass))
				return true;
				
		if (checkSuperInterfaces() && sourceTypeBinding.superInterfaces != null){
			for (int i= 0; i < sourceTypeBinding.superInterfaces.length; i++){
				if ((sourceTypeBinding.superInterfaces[i] instanceof SourceTypeBinding)
					&& nameDefinedInType(newName, (SourceTypeBinding)sourceTypeBinding.superInterfaces[i]))
						return true;
			}
		}
		
		return false;
	}
}