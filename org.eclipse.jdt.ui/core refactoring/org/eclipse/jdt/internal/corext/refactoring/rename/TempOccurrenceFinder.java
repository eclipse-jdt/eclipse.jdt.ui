/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NameReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.corext.refactoring.AbstractRefactoringASTAnalyzer;

public class TempOccurrenceFinder extends AbstractRefactoringASTAnalyzer{

	private List fOffsets;//set of Integer;
	private List fReferences; //List of NameReference
	private boolean fIncludeReferences;
	private boolean fIncludeDeclaration;
	private LocalDeclaration fTempDeclaration;
		
	public TempOccurrenceFinder(LocalDeclaration tempDeclaration, boolean includeReferences, boolean includeDeclaration){
		fIncludeReferences= includeReferences;
		fIncludeDeclaration= includeDeclaration;
		fTempDeclaration= tempDeclaration;
		fOffsets= new ArrayList();
		fReferences= new ArrayList();
	}
	
	public Integer[] getOccurrenceOffsets(){
		return (Integer[]) fOffsets.toArray(new Integer[fOffsets.size()]);
	}
	
	public NameReference[] getReferences(){
		return (NameReference[]) fReferences.toArray(new NameReference[fReferences.size()]);
	}
	
	private boolean visitNameReference(NameReference nameReference){
		if (!fIncludeReferences)
			return true;	
		if (! (nameReference.binding instanceof LocalVariableBinding))
			return true;
		LocalVariableBinding localBinding= (LocalVariableBinding)nameReference.binding;
		if (fTempDeclaration.equals(localBinding.declaration)){
			fOffsets.add(new Integer(nameReference.sourceStart));
			fReferences.add(nameReference);
		}	
		return true;
	}
	
	//------- visit ------
	
	public boolean visit(LocalDeclaration localDeclaration, BlockScope blockScope){
		if (! fIncludeDeclaration)
			return true;
			
		if (fTempDeclaration.equals(localDeclaration))
			fOffsets.add(new Integer(localDeclaration.sourceStart));
			
		return true;
	}
	
	public boolean visit(SingleNameReference singleNameReference, BlockScope blockScope){
		return visitNameReference(singleNameReference);
	}
		
	public boolean visit(QualifiedNameReference qualifiedNameReference,	BlockScope scope) {
		return visitNameReference(qualifiedNameReference);
	}	
}