/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.compiler.AbstractSyntaxTreeVisitorAdapter;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.lookup.*;
import org.eclipse.jdt.internal.compiler.util.CharOperation;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.corext.refactoring.AbstractRefactoringASTAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.DebugUtils;

class RenameTempOccurrenceOffsetFinder extends AbstractRefactoringASTAnalyzer{

	private Set fOffsets;//set of Integer;
	private boolean fUpdateReferences;
	private LocalDeclaration fTempDeclaration;
	
	RenameTempOccurrenceOffsetFinder(LocalDeclaration tempDeclaration, boolean updateReferences){
		fUpdateReferences= updateReferences;
		fTempDeclaration= tempDeclaration;
		fOffsets= new HashSet();
	}

	Integer[] getOccurrenceOffsets(){
		return (Integer[]) fOffsets.toArray(new Integer[fOffsets.size()]);
	}
	
	private boolean visitNameReference(NameReference nameReference){
		if (!fUpdateReferences)
			return true;	
		if (! (nameReference.binding instanceof LocalVariableBinding))
			return true;
		LocalVariableBinding localBinding= (LocalVariableBinding)nameReference.binding;
		if (fTempDeclaration.equals(localBinding.declaration))
			fOffsets.add(new Integer(nameReference.sourceStart()));
		return true;
		
	}
	//------- visit ------
	public boolean visit(LocalDeclaration localDeclaration, BlockScope blockScope){
		if (fTempDeclaration.equals(localDeclaration))
			fOffsets.add(new Integer(localDeclaration.sourceStart()));
		return true;
	}
	
	public boolean visit(SingleNameReference singleNameReference, BlockScope blockScope){
		return visitNameReference(singleNameReference);
	}
		
	public boolean visit(QualifiedNameReference qualifiedNameReference,	BlockScope scope) {
		return visitNameReference(qualifiedNameReference);
	}	
}