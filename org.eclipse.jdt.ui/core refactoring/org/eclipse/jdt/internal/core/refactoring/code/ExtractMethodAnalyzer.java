/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.code;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.BaseTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.core.refactoring.ExtendedBuffer;
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.code.flow.InputFlowAnalyzer;
import org.eclipse.jdt.internal.core.refactoring.code.flow.FlowContext;
import org.eclipse.jdt.internal.core.refactoring.code.flow.FlowInfo;
import org.eclipse.jdt.internal.core.refactoring.code.flow.InOutFlowAnalyzer;

public class ExtractMethodAnalyzer extends StatementAnalyzer {

	public static final int ERROR=					-2;
	public static final int UNDEFINED=				-1;
	public static final int NO=						0;
	public static final int EXPRESSION=				1;
	public static final int ACCESS_TO_LOCAL=		2;
	public static final int RETURN_STATEMENT_VOID=	3;
	public static final int RETURN_STATEMENT_VALUE=	4;
	public static final int MULTIPLE=				5;

	private static final char[] VOID= {'v', 'o', 'i', 'd'};
	
	private ClassScope fClassScope;
	private MethodScope fOuterMostMethodScope;
	private int fReturnKind;
	
	private LocalVariableBinding[] fArguments;
	private LocalVariableBinding[] fMethodLocals;
	
	private LocalVariableBinding fReturnValue;
	private LocalVariableBinding[] fCallerLocals;
	private LocalVariableBinding fReturnLocal;

	public ExtractMethodAnalyzer(ExtendedBuffer buffer, int start, int length, boolean asymetricAssignment) {
		super(buffer, start, length, asymetricAssignment);
	}
	
	public int getReturnKind() {
		return fReturnKind;
	}
	
	public boolean extractsExpression() {
		return fReturnKind == EXPRESSION;
	}
	
	public TypeReference getReturnType() {
		switch (fReturnKind) {
			case ACCESS_TO_LOCAL:
				return fReturnValue.declaration.type;
			case EXPRESSION:
				TypeBinding binding= getExpressionTypeBinding();
				TypeReference result;
				if (isExpressionTypeVisible()) {
					result= new SingleTypeReference(binding.sourceName(), 0);
				} else {
					StringBuffer buffer= new StringBuffer();
					buffer.append(binding.qualifiedPackageName());
					buffer.append('.');
					buffer.append(binding.qualifiedSourceName());
					result= new SingleTypeReference(buffer.toString().toCharArray(), 0);
				}
				result.binding= binding;
				result.sourceStart= -1;
				result.sourceEnd= -1;
				return result;
			case RETURN_STATEMENT_VALUE:
				AbstractMethodDeclaration enclosingMethod= getEnclosingMethod();
				if (enclosingMethod instanceof MethodDeclaration) {
					return ((MethodDeclaration)enclosingMethod).returnType;
				} else {
					// We have a constructor. So no return type
					return null;
				}
			default:
				result= new SingleTypeReference(VOID, 0);
				result.sourceStart= -1;
				result.sourceEnd= -1;
				result.binding= BaseTypeBinding.VoidBinding;
				return result;
		}
	}
	
	public LocalVariableBinding[] getArguments() {
		return fArguments;
	}
	
	public LocalVariableBinding[] getMethodLocals() {
		return fMethodLocals;
	}
	
	public LocalVariableBinding getReturnValue() {
		return fReturnValue;
	}
	
	public LocalVariableBinding[] getCallerLocals() {
		return fCallerLocals;
	}
	
	public LocalVariableBinding getReturnLocal() {
		return fReturnLocal;
	}
	
	public String getReturnTypeAsString() {
		AbstractMethodDeclaration enclosingMethod= getEnclosingMethod();
		// Copy the return type from the enclosing method if possible.
		if (enclosingMethod instanceof MethodDeclaration) {
			TypeReference type= ((MethodDeclaration)enclosingMethod).returnType;
			if (type == null)
				return ""; //$NON-NLS-1$
			else
				return type.toStringExpression(0);
		} else {
			// We have a constructor. So no return type
			return ""; //$NON-NLS-1$
		}
	}
	
	public void checkActivation(RefactoringStatus status) {
		super.checkActivation(status);
		if (!status.hasFatalError()) {
			fReturnKind= UNDEFINED;
			fOuterMostMethodScope= getEnclosingMethod().scope.outerMostMethodScope();
			fClassScope= getClassScope(getEnclosingMethod().scope);
			if (fOuterMostMethodScope == null || fClassScope == null) {
				status.addFatalError(RefactoringCoreMessages.getString("StatementAnalyzer.only_method_body")); //$NON-NLS-1$
				return;
			}
			if (analyzeSelection(status).hasFatalError())
				return;

			int returns= fReturnKind == NO ? 0 : 1;
			if (fReturnValue != null) {
				fReturnKind= ACCESS_TO_LOCAL;
				returns++;
			}
			if (fExpressionTypeBinding != null) {
				fReturnKind= EXPRESSION;
				returns++;
			}
				
			if (returns > 1) {
				status.addFatalError("Ambiguous return value: expression, access to local or return statement extracted."); //$NON-NLS-1$
				fReturnKind= MULTIPLE;
			}
		}
	}
	
	private RefactoringStatus analyzeSelection(RefactoringStatus status) {
		FlowContext flowContext= new FlowContext(0, fOuterMostMethodScope.analysisIndex);
		flowContext.setConsiderExecutionFlow(true);
		flowContext.setConsiderAccessMode(true);
		flowContext.setComputeMode(FlowContext.ARGUMENTS);
		
		InOutFlowAnalyzer flowAnalyzer= new InOutFlowAnalyzer(flowContext, fSelection);
		FlowInfo flowInfo= flowAnalyzer.analyse(fTopNodes, (BlockScope)getEnclosingScope());
		
		if (flowInfo.branches()) {
			status.addFatalError("Selection contains branch statement but corresponding branch target isn't selected.");
			fReturnKind= ERROR;
			return status;
		}
		if (flowInfo.isValueReturn()) {
			fReturnKind= RETURN_STATEMENT_VALUE;
		} else  if (flowInfo.isVoidReturn()) {
			fReturnKind= RETURN_STATEMENT_VOID;
		} else if (flowInfo.isNoReturn() || flowInfo.isThrow() || flowInfo.isUndefined()) {
			fReturnKind= NO;
		}
		
		if (fReturnKind == UNDEFINED) {
			status.addFatalError(RefactoringCoreMessages.getString("FlowAnalyzer.execution_flow")); //$NON-NLS-1$
			fReturnKind= ERROR;
			return status;
		}
		computeInput(flowInfo, flowContext);
		computeOutput(status);
		return status;
	}
	
	private boolean enclosingMethodReturns() {
		MethodBinding binding= getEnclosingMethod().binding;
		if (binding == null)
			return false;
		else if (binding.returnType == BaseTypeBinding.VoidBinding)
			return false;
			
		return true;	
	}
	
	private void computeInput(FlowInfo flowInfo, FlowContext flowContext) {
		fArguments= flowInfo.getArguments(flowContext);
		removeSelectedDeclarations(fArguments);
		fMethodLocals= flowInfo.get(flowContext, FlowInfo.WRITE | FlowInfo.WRITE_POTENTIAL);
		removeSelectedDeclarations(fMethodLocals);
	}

	private void removeSelectedDeclarations(LocalVariableBinding[] bindings) {
		for (int i= 0; i < bindings.length; i++) {
			LocalDeclaration decl= bindings[i].declaration;
			if (fSelection.covers(decl))
				bindings[i]= null;
		}
	}
	
	private void computeOutput(RefactoringStatus status) {
		// First find all writes inside the selection.
		FlowContext flowContext= new FlowContext(0, fOuterMostMethodScope.analysisIndex);
		flowContext.setConsiderExecutionFlow(false);
		flowContext.setConsiderAccessMode(true);
		flowContext.setComputeMode(FlowContext.RETURN_VALUES);
		FlowInfo flowInfo= new InOutFlowAnalyzer(flowContext, fSelection).analyse(fTopNodes, (BlockScope)getEnclosingScope());
		LocalVariableBinding[] returnValues= flowInfo.getReturnValues(flowContext);
		
		flowContext.setComputeMode(FlowContext.ARGUMENTS);
		flowInfo= new InputFlowAnalyzer(flowContext, fSelection).analyse(getEnclosingMethod(), fClassScope);
		LocalVariableBinding[] reads= flowInfo.getArguments(flowContext);
		int counter= 0;
		outer: for (int i= 0; i < returnValues.length && counter <= 1; i++) {
			LocalVariableBinding binding= returnValues[i];
			for (int x= 0; x < reads.length; x++) {
				if (reads[x] == binding) {
					counter++;
					fReturnValue= binding;
					continue outer;
				}
			}
		}
		switch (counter) {
			case 0:
				fReturnValue= null;
				break;
			case 1:
				break;
			default:
				fReturnValue= null;
				status.addFatalError(RefactoringCoreMessages.getString("ExtractMethodAnalyzer.assignments_to_local"));
				return;
		}
		List callerLocals= new ArrayList(5);
		LocalVariableBinding[] writes= flowInfo.get(flowContext, FlowInfo.WRITE);
		for (int i= 0; i < writes.length; i++) {
			LocalVariableBinding write= writes[i];
			if (fSelection.covers(write.declaration))
				callerLocals.add(write);
		}
		fCallerLocals= (LocalVariableBinding[])callerLocals.toArray(new LocalVariableBinding[callerLocals.size()]);
		if (fReturnValue != null && fSelection.covers(fReturnValue.declaration))
			fReturnLocal= fReturnValue;
	}
	
	private ClassScope getClassScope(Scope scope) {
		while (scope != null) {
			if (scope instanceof ClassScope)
				return (ClassScope) scope;
			scope= scope.parent;
		}
		return null;
	}
}

