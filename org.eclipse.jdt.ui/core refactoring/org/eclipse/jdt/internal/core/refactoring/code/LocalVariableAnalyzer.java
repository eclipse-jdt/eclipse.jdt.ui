/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.code;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;

import org.eclipse.jdt.internal.compiler.ast.ArrayReference;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.Reference;
import org.eclipse.jdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.Statement;
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

	// References already handled as a LHS of an assignment
	private List fLhsOfAssignment= new ArrayList(2);
		
	// Return statement handling if a return statement has been selected.
	private ReturnStatement fExtractedReturnStatement;
	
	// Return type of an extracted expression
	private TypeReference fExpressionReturnType;

	// Code generation
	private LocalVariableBinding fReturnStatementBinding;
	private boolean fAsymetricAssignment;
	private String fAssignment;
	private String fLocalDeclaration= null;	
	private String fLocalReturnValueDeclaration= null;
	private String fLhs= null;
	private String fReturnStatement= null;	
	private String fReturnType= "void";
	
	private List fUsedLocals= new ArrayList(2);
	
	public LocalVariableAnalyzer(StatementAnalyzer analyzer, boolean asymetricAssignment) {
		fStatementAnalyzer= analyzer;
		fAsymetricAssignment= asymetricAssignment;
		if (fAsymetricAssignment)
			fAssignment= "= ";
		else
			fAssignment= " = ";
	}

	//---- Analyzing statements ----------------------------------------------------------------

	public void visit(SingleNameReference reference, BlockScope scope, int mode) {
		if (isOfInterestForRead(mode, reference))
			processLocalVariableBindingRead(getLocalVariableBindingIfSingleNameReference(reference), mode);
	}
	
	public void visit(QualifiedNameReference reference, BlockScope scope, int mode) {
		if (isOfInterestForRead(mode, reference))
			processLocalVariableBindingRead(getLocalVariableBindingIfQualifiedNameReference(reference), mode);
	}
	
	public void visitAssignment(Assignment assignment, BlockScope scope, int mode, boolean compound) {
		Reference reference= assignment.lhs;
		if (isOfInterestForWrite(mode, reference)) {
			LocalVariableBinding binding= getLocalVariableBindingIfSingleNameReference(reference);
			if (binding != null) {
			    addLocalWrite(binding, mode);
			    fLhsOfAssignment.add(reference);
			    if (compound)
			    	addLocalRead(binding, mode);
			    return;
			}
		}
	}

	public void visitPostfixPrefixExpression(Assignment assignment, BlockScope scope, int mode) {
		Reference lhs= assignment.lhs;
		if (isOfInterestForWrite(mode, lhs) || isOfInterestForRead(mode, lhs)) {
			LocalVariableBinding binding= getLocalVariableBindingIfSingleNameReference(lhs);
			if (binding != null) {
				addLocalWrite(binding, mode);
				addLocalRead(binding, mode);
			}
		}
	}

	public void setExtractedReturnStatement(ReturnStatement returnStatement) {
		fExtractedReturnStatement= returnStatement;
	}
	
	public void setExpressionReturnType(TypeReference returnType) {
		fExpressionReturnType= returnType;
	}
	
	public TypeReference getExpressionReturnType() {
		return fExpressionReturnType;
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
	
	private boolean isOfInterestForRead(int mode, Reference reference) {
		return (mode == StatementAnalyzer.SELECTED || mode == StatementAnalyzer.AFTER) &&
			!fLhsOfAssignment.contains(reference);
	}
	
	private boolean isOfInterestForWrite(int mode, Reference reference) {
		return mode == StatementAnalyzer.SELECTED || mode == StatementAnalyzer.AFTER;
	}
	
	//---- Precondition checking ----------------------------------------------------------------

	public void checkActivation(RefactoringStatus status) {
		checkLocalReads(status);
		List followingLocals= computeFollowingLocals();
		checkLocalWrites(status, followingLocals);
		checkFollowingLocals(status, followingLocals);
		checkReturnStatement(status);
	}

	private List computeFollowingLocals() {
		List result= new ArrayList(2);
		result.addAll(fFollowingLocalReads);
		for (Iterator iter= fFollowingLocalWrites.iterator(); iter.hasNext(); ) {
			Object item= iter.next();
			if (!result.contains(item))
				result.add(item);
			
		}
		return result;
	}

	//---- Code generation ----------------------------------------------------------------------

	public String getCall(String methodName, boolean oneLine) {
		StringBuffer result= new StringBuffer();
		if (fLhs != null) {
			if (!oneLine || fLocalDeclaration == null)
				result.append(fLhs);
			result.append(fAssignment);	
		}
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
		StringBuffer result= new StringBuffer();
		if (fExpressionReturnType != null) {
			result.append(fExpressionReturnType.toStringExpression(0));
		} else {
			result.append(fReturnType);
		}
		result.append(" ");
		result.append(methodName);
		result.append("(");
		for (int i= 0; i < fUsedLocals.size(); i++) {
			if (i > 0)
				result.append(", ");
			LocalVariableBinding binding= (LocalVariableBinding)fUsedLocals.get(i);
			LocalDeclaration declaration= binding.declaration;
			TypeReference typeRef= declaration.type;
			String modifiers= declaration.modifiersString(declaration.modifiers);
			if (modifiers.length() != 0) {
				result.append(modifiers);
			}
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
		LocalVariableBinding returnBinding= null;
		boolean isHardReturnDeclaration= false;
		Iterator iter= fSelectedLocalWrites.iterator();
		while (iter.hasNext()) {
			LocalVariableBinding binding= (LocalVariableBinding)iter.next();
			if (!fStatementAnalyzer.isSelected(binding.declaration)) {
				boolean isUsedAfterSelection= followingLocals.contains(binding);
				if (returnBinding == null) {
					returnBinding= binding;
					isHardReturnDeclaration= isUsedAfterSelection;
				} else {
					if (isHardReturnDeclaration) {
						status.addFatalError("Ambiguous return value: selected block contains more than one assignment to local variable.");
						return;
					} else if (isUsedAfterSelection) {
						returnBinding= binding;
						isHardReturnDeclaration= true;
					}
				}
				// The variable is not part of the read accesses. So we have to create a local variable declaration.
				if (!fUsedLocals.contains(binding))
					fLocalReturnValueDeclaration= makeDeclaration(binding) + ";"; 
			}
		}
		if (returnBinding != null) {
			fReturnStatementBinding= returnBinding;
			computeReturnType(returnBinding, isHardReturnDeclaration, status);
		}
	}
	
	private void checkFollowingLocals(RefactoringStatus status, List followingLocals) {
		int count= 0;
		LocalVariableBinding returnBinding= null;
		Iterator iter= followingLocals.iterator();
		while (iter.hasNext()) {
			LocalVariableBinding binding= (LocalVariableBinding)iter.next();
			if (fStatementAnalyzer.isSelected(binding.declaration)) {
				count++;
				if (count > 1) {
					status.addFatalError("Ambiguous return value: more than one reference to selected local declaration found.");
					return;
				} else {
					returnBinding= binding;
				}
			}
		}
		if (returnBinding != null) {
			if (returnTypeIsVoid()) {
				fReturnStatementBinding= returnBinding;
				computeReturnType(returnBinding, true, status);
				fLocalDeclaration= makeDeclaration(returnBinding);
			} else {
				status.addFatalError("Ambiguous return value: assignment to local variable and reference to a selected local declaration found.");
			}
		}
	}
	
	private void checkReturnStatement(RefactoringStatus status) {
		if (fExtractedReturnStatement != null && fReturnStatementBinding != null && 
				!isSameLocalVaraibleBinding(fExtractedReturnStatement, fReturnStatementBinding)) {
			status.addFatalError("Ambiguous return value: selection contains return statement and a value must be returned from the extracted method.");
		}	
	}
	
	private boolean isSameLocalVaraibleBinding(ReturnStatement statement, LocalVariableBinding binding) {
		if(!(statement.expression instanceof SingleNameReference))
			return false;
		SingleNameReference reference= (SingleNameReference)statement.expression;
		return reference.binding == binding;	
	}
	
	private boolean returnTypeIsVoid() {
		return "void".equals(fReturnType);
	}
	
	private void computeReturnType(LocalVariableBinding binding, boolean isHardReturnType, RefactoringStatus status) {
		if (isHardReturnType && fExpressionReturnType != null) {
			status.addFatalError("Ambiguous return value: expression has return type and a value must be returned from extracted method.");
		}
		LocalDeclaration declaration= binding.declaration;
		TypeReference typeRef= declaration.type;
		fReturnType= typeRef.toStringExpression(0);
		fLhs= declaration.name();
		fReturnStatement= "return " + declaration.name() + ";";
	}
	
	private String makeDeclaration(LocalVariableBinding binding) {
		LocalDeclaration declaration= binding.declaration;
		TypeReference typeRef= declaration.type;
		return typeRef.toStringExpression(0) + " " + declaration.name();
	}		
}