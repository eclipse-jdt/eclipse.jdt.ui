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
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;

/*
 * Some implementation notes:
 * - it is not necessary to track assignments in local declarations like int i= 10; The
 *   reason is as follows: if the assignment takes place before the selection we are not
 *   interested. If it is selected then we move it to the extracted method anyway and it
 *	 could not have been referenced before. If it appears after the selected code the
 *   declaration can't influence the extracted method at all.
 */
/* package */ class LocalVariableAnalyzer {
	private static class LocalAccess {
		public static final int READ= 		1;
		public static final int WRITE= 		2;
		public LocalVariableBinding binding;
		public int mode;
		public LocalAccess(LocalVariableBinding b, int m) {
			binding= b;
			mode= m;
		}
		public boolean isRead() {
			return mode == READ;
		}
		public boolean isWrite() {
			return mode == WRITE;
		}
		public boolean equals(Object o) {
			if (o.getClass() != LocalAccess.class)
				return false;
			LocalAccess other= (LocalAccess)o;
			return 	binding == other.binding && mode == other.mode;
		}
		public int hashCode() {
			return binding.hashCode();
		}
	}

	private StatementAnalyzer fStatementAnalyzer;
	// Reads and writes to locals inside the selected statement block.
	private List fSelectedLocals= new ArrayList(2);
	
	// Reads and writes in the same method which occur after the
	// selected statements.
	private List fFollowingLocals= new ArrayList(2);

	// References already handled as a LHS of an assignment
	private List fLhsOfAssignment= new ArrayList(2);
		
	// Return type of an extracted expression
	private TypeReference fExpressionReturnType;

	// Caller of the extracted method.
	private String fAssignment;
	private List fExtractedLocals= new ArrayList(1);
	private String fLhs= null;
	
	// Extracted method.	
	private List fArguments= new ArrayList(2);
	private List fLocals= new ArrayList(1);
	private LocalVariableBinding fReturnStatementBinding;
	private String fReturnStatement= null;	
	private String fReturnType= "void"; //$NON-NLS-1$
		
	public LocalVariableAnalyzer(StatementAnalyzer analyzer, boolean asymetricAssignment) {
		fStatementAnalyzer= analyzer;
		if (asymetricAssignment)
			fAssignment= "= "; //$NON-NLS-1$
		else
			fAssignment= " = "; //$NON-NLS-1$
	}

	//---- Analyzing statements ----------------------------------------------------------------

	public void visit(SingleNameReference reference, BlockScope scope, int mode) {
		if (isOfInterestForRead(reference, mode))
			processLocalVariableBindingRead(getLocalVariableBindingIfSingleNameReference(reference), mode);
	}
	
	public void visit(QualifiedNameReference reference, BlockScope scope, int mode) {
		if (isOfInterestForRead(reference, mode))
			processLocalVariableBindingRead(getLocalVariableBindingIfQualifiedNameReference(reference), mode);
	}
	
	public void visitAssignment(Assignment assignment, BlockScope scope, int mode) {
		if (isOfInterest(mode))
			fLhsOfAssignment.add(assignment.lhs);
	}
	
	public void endVisitAssignment(Assignment assignment, BlockScope scope, int mode, boolean compound) {
		Reference reference= assignment.lhs;
		if (isOfInterest(mode)) {
			LocalVariableBinding binding= getLocalVariableBindingIfSingleNameReference(reference);
			if (binding != null) {
			    if (compound)
			    	addLocalRead(binding, mode);
			    addLocalWrite(binding, mode);
			} else {
				binding= getLocalVariableBindingIfQualifiedNameReference(reference);
				if (binding != null) {
					// A qualified name reference on the lhs of an assignment is a
					// read to the outer instance.
					addLocalRead(binding, mode);
				}
			}
		}
	}

	public void visitPostfixPrefixExpression(Assignment assignment, BlockScope scope, int mode) {
		Reference lhs= assignment.lhs;
		if (isOfInterest(mode)) {
			LocalVariableBinding binding= getLocalVariableBindingIfSingleNameReference(lhs);
			if (binding != null) {
				addLocalRead(binding, mode);
				addLocalWrite(binding, mode);
			}
		}
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
				if (!contains(fSelectedLocals, binding, LocalAccess.WRITE))
					fSelectedLocals.add(new LocalAccess(binding, LocalAccess.WRITE));
				break;
			case StatementAnalyzer.AFTER:
				// A write access is not of interest if their is already a read access since
				// we have to return a value from the extracted method anyway.
				if (!contains(fFollowingLocals, binding))
					fFollowingLocals.add(new LocalAccess(binding, LocalAccess.WRITE));
				break;
		}
	}
	
	private boolean contains(List list, LocalVariableBinding binding, int mode) {
		for (Iterator iter= list.iterator(); iter.hasNext();) {
			LocalAccess access= (LocalAccess)iter.next();
			if (access.binding == binding && access.mode == mode)
				return true;
		}
		return false;
	}
	
	private void addLocalRead(LocalVariableBinding binding, int mode) {
		switch (mode) {
			case StatementAnalyzer.SELECTED:
				if (!contains(fSelectedLocals, binding))
					fSelectedLocals.add(new LocalAccess(binding, LocalAccess.READ));
				break;
			case StatementAnalyzer.AFTER:
				// A read access is not of interest if we have already found a write access
				// to the same local. In this case we don't need to return a value from the
				// extracted method since the extracted method doesn't have any effect on
				// the value when read.
				if (!contains(fFollowingLocals, binding))
					fFollowingLocals.add(new LocalAccess(binding, LocalAccess.READ));
				break;
		}
	}
	
	private boolean contains(List list, LocalVariableBinding binding) {
		for (Iterator iter= list.iterator(); iter.hasNext();) {
			LocalAccess access= (LocalAccess)iter.next();
			if (access.binding == binding)
				return true;
		}
		return false;
	}
	
	private boolean isOfInterestForRead(Reference reference, int mode) {
		boolean existed= fLhsOfAssignment.remove(reference);
		return (mode == StatementAnalyzer.SELECTED || mode == StatementAnalyzer.AFTER) && !existed;
	}
	
	private boolean isOfInterest(int mode) {
		return mode == StatementAnalyzer.SELECTED || mode == StatementAnalyzer.AFTER;
	}
	
	//---- Precondition checking ----------------------------------------------------------------

	public void checkActivation(RefactoringStatus status) {
		computeLocals();
		checkSelectedAccesses(status);
		checkFollowingAccesses(status);
	}

	//---- Code generation ----------------------------------------------------------------------

	public boolean hasReturnType() {
		return !returnTypeIsVoid();
	}
	
	public String getReturnType() {
		if (fExpressionReturnType != null) {
			return fExpressionReturnType.toStringExpression(0);
		} else {
			return fReturnType;
		}
	}
	
	public String getCall(String methodName) {
		StringBuffer result= new StringBuffer();
		if (fLhs != null) {
			result.append(fLhs);
			result.append(fAssignment);	
		}
		result.append(methodName);
		result.append("("); //$NON-NLS-1$
		for (int i= 0; i < fArguments.size(); i++) {
			if (i > 0)
				result.append(", "); //$NON-NLS-1$
			LocalVariableBinding binding= (LocalVariableBinding)fArguments.get(i);
			result.append(binding.readableName());
		}		
		result.append(")"); //$NON-NLS-1$
		return result.toString();
	}
	
	public String getCallSignature(String methodName) {
		StringBuffer result= new StringBuffer();
		result.append(methodName);
		result.append("("); //$NON-NLS-1$
		for (int i= 0; i < fArguments.size(); i++) {
			if (i > 0)
				result.append(", "); //$NON-NLS-1$
			LocalVariableBinding binding= (LocalVariableBinding)fArguments.get(i);
			LocalDeclaration declaration= binding.declaration;
			TypeReference typeRef= declaration.type;
			String modifiers= declaration.modifiersString(declaration.modifiers);
			if (modifiers.length() != 0) {
				result.append(modifiers);
			}
			result.append(typeRef.toStringExpression(0));
			result.append(" "); //$NON-NLS-1$
			result.append(binding.readableName());
		}
		result.append(")"); //$NON-NLS-1$
		return result.toString();
	}
		
	public String[] getExtractedLocals() {
		return makeDeclarations(fExtractedLocals);
	}
	
	public String[] getLocals() {
		return makeDeclarations(fLocals);
	}
	
	private String[] makeDeclarations(List localDeclarations) {
		String[] result= new String[localDeclarations.size()];
		for (int i= localDeclarations.size() -1; i >= 0; i--) {
			LocalVariableBinding binding= (LocalVariableBinding)localDeclarations.get(i);
			result[i]= makeDeclaration(binding);
		}
		return result;
	}
	
	public String getReturnStatement() {
		return fReturnStatement;
	}
	
	public String[] getParameterTypes() {
		int size= fArguments.size();
		String[] result= new String[size];
		for (int i= 0; i < size; i++) {
			LocalVariableBinding binding= (LocalVariableBinding)fArguments.get(i);
			TypeReference typeRef= binding.declaration.type;
			result[i]= Signature.createTypeSignature(typeRef.toStringExpression(0), false);
		}
		return result;		
	}
	
	//---- Private Helper methods -------------------------------------------------------------
	
	private void computeLocals() {
		for (Iterator iter= fSelectedLocals.iterator(); iter.hasNext();) {
			LocalAccess access= (LocalAccess)iter.next();
			LocalVariableBinding binding= access.binding;
			if (!fStatementAnalyzer.isSelected(binding.declaration)) {
				if (access.isRead()) {
					fArguments.add(binding);
				} else {
					if (!fArguments.contains(binding))
						fLocals.add(binding);
				}
			}
		}
	}
	
	private void checkSelectedAccesses(RefactoringStatus status) {
		LocalVariableBinding returnBinding= null;
		for (Iterator iter= fSelectedLocals.iterator(); iter.hasNext(); ) {
			LocalAccess access= (LocalAccess)iter.next();
			if (access.isRead())
				continue;
				
			LocalVariableBinding binding= (LocalVariableBinding)access.binding;
			if (!fStatementAnalyzer.isSelected(binding.declaration)) {
				boolean isUsedAfterSelection= contains(fFollowingLocals, binding, LocalAccess.READ);
				if (isUsedAfterSelection) {
					if (returnBinding == null) {
						returnBinding= binding;
					} else {
						status.addFatalError(RefactoringCoreMessages.getString("LocalVariableAnalyzer.assignments_to_local")); //$NON-NLS-1$
						return;
					}
				} 
			}
		}
		if (returnBinding != null) {
			fReturnStatementBinding= returnBinding;
			computeReturnType(returnBinding, status);
		}
	}
	
	private void checkFollowingAccesses(RefactoringStatus status) {
		int count= 0;
		LocalVariableBinding returnBinding= null;
		for (Iterator iter= fFollowingLocals.iterator(); iter.hasNext();) {
			LocalAccess access= (LocalAccess)iter.next();
			LocalVariableBinding binding= access.binding;
			if (fStatementAnalyzer.isSelected(binding.declaration)) {
				if (access.isRead()) {
					count++;
					if (count > 1) {
						status.addFatalError(RefactoringCoreMessages.getString("LocalVariableAnalyzer.references_to_local")); //$NON-NLS-1$
						return;
					} else {
						returnBinding= binding;
					}
				} else {
					fExtractedLocals.add(binding);
				}
			}
		}
		if (returnBinding != null) {
			if (returnTypeIsVoid()) {
				fReturnStatementBinding= returnBinding;
				computeReturnType(returnBinding, status);
				fLhs= makeDeclaration(returnBinding);
			} else {
				status.addFatalError(RefactoringCoreMessages.getString("LocalVariableAnalyzer.assignment_and_reference_to_local")); //$NON-NLS-1$
			}
		}
	}
	
	private boolean isSameLocalVaraibleBinding(ReturnStatement statement, LocalVariableBinding binding) {
		if(!(statement.expression instanceof SingleNameReference))
			return false;
		SingleNameReference reference= (SingleNameReference)statement.expression;
		return reference.binding == binding;	
	}
	
	private boolean returnTypeIsVoid() {
		return "void".equals(fReturnType); //$NON-NLS-1$
	}
	
	private void computeReturnType(LocalVariableBinding binding, RefactoringStatus status) {
		if (fExpressionReturnType != null) {
			status.addFatalError(RefactoringCoreMessages.getString("LocalVariableAnalyzer.return_type")); //$NON-NLS-1$
		}
		LocalDeclaration declaration= binding.declaration;
		TypeReference typeRef= declaration.type;
		fReturnType= typeRef.toStringExpression(0);
		fLhs= declaration.name();
		fReturnStatement= "return " + declaration.name() + ";"; //$NON-NLS-2$ //$NON-NLS-1$
	}
	
	private String makeDeclaration(LocalVariableBinding binding) {
		LocalDeclaration declaration= binding.declaration;
		TypeReference typeRef= declaration.type;
		return typeRef.toStringExpression(0) + " " + declaration.name(); //$NON-NLS-1$
	}		
}