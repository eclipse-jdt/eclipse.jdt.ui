/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000, 2001
 */
package org.eclipse.jdt.core.refactoring.code;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.Reference;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;

/* package */ class LocalVariableAnalyzer {

	private StatementAnalyzer fStatementAnalyzer;

	// Reads and writes to locals inside the selected statement block.
	private List fSelectedLocalWrites= new ArrayList(2);
	private List fSelectedLocalReads= new ArrayList(2);
	
	// Reads and writes in the same method which occur after the
	// selected statements.
	private List fFollowingLocalWrites= new ArrayList(2);
	private List fFollowingLocalReads= new ArrayList(2);
	
	private String fLocalDeclaration= null;	
	private String fLocalReturnValueDeclaration= null;
	private String fLhs= "";
	private String fReturnStatement= null;
	private String fReturnType= "void";
	
	private List fUsedLocals= new ArrayList(2);
	
	public LocalVariableAnalyzer(StatementAnalyzer analyzer) {
		fStatementAnalyzer= analyzer;
	}

	//---- Analyzing statements ----------------------------------------------------------------

	public void visit(SingleNameReference singleNameReference, BlockScope scope, int mode) {
		if (isOfInterestForRead(mode))
			processLocalVariableBindingRead(getLocalVariableBindingIfSingleNameReference(singleNameReference), mode);
	}
	
	public void visit(QualifiedNameReference reference, BlockScope scope, int mode) {
		if (isOfInterestForRead(mode))
			processLocalVariableBindingRead(getLocalVariableBindingIfQualifiedNameReference(reference), mode);
	}
	
	public void visitLhsOfAssignment(Reference reference, BlockScope scope, int mode, boolean compound) {
		if (isOfInterestForWrite(mode)) {
			LocalVariableBinding binding= getLocalVariableBindingIfSingleNameReference(reference);
			if (binding != null) {
			    addLocalWrite(binding, mode);
			    if (compound)
			    	addLocalRead(binding, mode);
			} else {
				binding= getLocalVariableBindingIfQualifiedNameReference(reference);
				if (binding != null) {
					// code like this:
					// class A {
					//	public int x;
					// }
					//
					// a.x= 10;
					// 
					// is a read to variable a.
					addLocalRead(binding, mode);
				}
			}
		}
	}
	
	public void visitPostfixPrefixExpression(Assignment assignment, BlockScope scope, int mode) {
		if (isOfInterestForWrite(mode) || isOfInterestForRead(mode)) {
			LocalVariableBinding binding= getLocalVariableBindingIfSingleNameReference(assignment.lhs);
			if (binding != null) {
				addLocalWrite(binding, mode);
				addLocalRead(binding, mode);
			}
		}
	}

	private LocalVariableBinding getLocalVariableBindingIfSingleNameReference(Reference ref) {
		if (ref instanceof SingleNameReference && 
		    ((SingleNameReference)ref).binding instanceof LocalVariableBinding) {
		    	return (LocalVariableBinding)((SingleNameReference)ref).binding;
		}
		return null;
	}
	
	private LocalVariableBinding getLocalVariableBindingIfQualifiedNameReference(Reference ref) {
		if (ref instanceof QualifiedNameReference && 
		    ((QualifiedNameReference)ref).binding instanceof LocalVariableBinding) {
		    	return (LocalVariableBinding)((QualifiedNameReference)ref).binding;
		}
		return null;
	}

	private void processLocalVariableBindingRead(LocalVariableBinding binding, int mode) {
		if (binding != null) {
			addLocalRead(binding, mode);	
		}
	}
	
	private void addLocalWrite(LocalVariableBinding binding, int mode) {
		switch (mode) {
			case StatementAnalyzer.SELECTED:
				if (!fSelectedLocalWrites.contains(binding))
					fSelectedLocalWrites.add(binding);			
				break;
			case StatementAnalyzer.AFTER:
				if (!fFollowingLocalWrites.contains(binding))
					fFollowingLocalWrites.add(binding);
				break;
		}
	}
	
	private void addLocalRead(LocalVariableBinding binding, int mode) {
		switch (mode) {
			case StatementAnalyzer.SELECTED:
				if (!fSelectedLocalReads.contains(binding))
					fSelectedLocalReads.add(binding);			
				break;
			case StatementAnalyzer.AFTER:
				if (!fFollowingLocalReads.contains(binding))
					fFollowingLocalReads.add(binding);
				break;
		}
	}
	
	private boolean isOfInterestForRead(int mode) {
		return mode == StatementAnalyzer.SELECTED || mode == StatementAnalyzer.AFTER;
	}
	
	private boolean isOfInterestForWrite(int mode) {
		return mode == StatementAnalyzer.SELECTED || mode == StatementAnalyzer.AFTER;
	}
	
	//---- Precondition checking ----------------------------------------------------------------

	public void checkActivation(RefactoringStatus status) {
		checkLocalReads(status);
		List followingLocals= new ArrayList(2);
		followingLocals.addAll(fFollowingLocalReads);
		followingLocals.addAll(fFollowingLocalWrites);
		checkLocalWrites(status, followingLocals);
		checkFollowingLocals(status, followingLocals);
	}
	
	//---- Code generation ----------------------------------------------------------------------

	public String getCall(String methodName) {
		StringBuffer result= new StringBuffer(fLhs);
		result.append(methodName);
		result.append("(");
		for (int i= 0; i < fUsedLocals.size(); i++) {
			if (i > 0)
				result.append(", ");
			LocalVariableBinding binding= (LocalVariableBinding)fUsedLocals.get(i);
			result.append(binding.readableName());
		}		
		result.append(")");
		return result.toString();
	}
	
	public String getCallSignature(String methodName) {
		StringBuffer result= new StringBuffer(fReturnType);
		result.append(" ");
		result.append(methodName);
		result.append("(");
		for (int i= 0; i < fUsedLocals.size(); i++) {
			if (i > 0)
				result.append(", ");
			LocalVariableBinding binding= (LocalVariableBinding)fUsedLocals.get(i);
			TypeReference typeRef= binding.declaration.type;
			result.append(typeRef.toStringExpression(0));
			result.append(" ");
			result.append(binding.readableName());
		}
		result.append(")");
		return result.toString();
	}
		
	public String getLocalDeclaration() {
		return fLocalDeclaration;
	}
	
	public String getLocalReturnValueDeclaration() {
		return fLocalReturnValueDeclaration;
	}
	
	public String getReturnStatement() {
		return fReturnStatement;
	}
	
	public String[] getParameterTypes() {
		int size= fUsedLocals.size();
		String[] result= new String[size];
		for (int i= 0; i < size; i++) {
			LocalVariableBinding binding= (LocalVariableBinding)fUsedLocals.get(i);
			TypeReference typeRef= binding.declaration.type;
			result[i]= Signature.createTypeSignature(typeRef.toStringExpression(0), false);
		}
		return result;		
	}
	
	//---- Private Helper methods -------------------------------------------------------------
	
	private void checkLocalReads(RefactoringStatus statue) {
		for (Iterator iter= fSelectedLocalReads.iterator(); iter.hasNext();) {
			LocalVariableBinding binding= (LocalVariableBinding)iter.next();
			if (!fStatementAnalyzer.isSelected(binding.declaration) && !fUsedLocals.contains(binding)) {
				fUsedLocals.add(binding);
			}
		}
	}
	
	private void checkLocalWrites(RefactoringStatus status, List followingLocals) {
		LocalDeclaration returnDeclaration= null;
		boolean isHardReturnDeclaration= false;
		Iterator iter= fSelectedLocalWrites.iterator();
		while (iter.hasNext()) {
			LocalVariableBinding binding= (LocalVariableBinding)iter.next();
			LocalDeclaration declaration= binding.declaration;
			if (!fStatementAnalyzer.isSelected(declaration)) {
				boolean isUsedAfterSelection= followingLocals.contains(binding);
				if (returnDeclaration == null) {
					returnDeclaration= declaration;
					isHardReturnDeclaration= isUsedAfterSelection;
				} else {
					if (isHardReturnDeclaration) {
						status.addFatalError("Ambigious return value: selected block contains more than one assignment to local variable");
						return;
					} else if (isUsedAfterSelection) {
						returnDeclaration= declaration;
						isHardReturnDeclaration= true;
					}
				}
				// The variable is not part of the read accesses. So we have to create a local variable declaration.
				if (!fUsedLocals.contains(binding))
					fLocalReturnValueDeclaration= makeDeclaration(declaration); 
			}
		}
		if (returnDeclaration != null) {
			computeReturnType(returnDeclaration);
		}
	}
	
	private void checkFollowingLocals(RefactoringStatus status, List followingLocals) {
		int count= 0;
		LocalDeclaration returnDeclaration= null;
		Iterator iter= followingLocals.iterator();
		while (iter.hasNext()) {
			LocalVariableBinding binding= (LocalVariableBinding)iter.next();
			LocalDeclaration declaration= binding.declaration;
			if (fStatementAnalyzer.isSelected(declaration)) {
				count++;
				if (count > 1) {
					status.addFatalError("Ambigious return value: more than one reference to selected local declaration found");
					return;
				} else {
					returnDeclaration= declaration;
				}
			}
		}
		if (returnDeclaration != null && ! returnTypeIsVoid()) {
			status.addFatalError("Ambigious return value: assignment to local variable and reference to a selected local declaration found");
			return;
		}
		
		if (returnDeclaration != null) {
			computeReturnType(returnDeclaration);
			fLocalDeclaration= makeDeclaration(returnDeclaration);
		}
	}
	
	private boolean returnTypeIsVoid() {
		return "void".equals(fReturnType);
	}
	
	private void computeReturnType(LocalDeclaration declaration) {
		TypeReference typeRef= declaration.type;
		fReturnType= typeRef.toStringExpression(0);
		fLhs= declaration.name() + "= ";
		fReturnStatement= "return " + declaration.name() + ";";
	}
	
	private String makeDeclaration(LocalDeclaration declaration) {
		TypeReference typeRef= declaration.type;
		return typeRef.toStringExpression(0) + " " + declaration.name() + ";";
	}		
}