/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.BaseTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.ExtendedBuffer;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.FlowContext;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.FlowInfo;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.InOutFlowAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.InputFlowAnalyzer;

/* package */ class ExtractMethodAnalyzer extends StatementAnalyzer {

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
	
	private FlowInfo fInputFlowInfo;
	private FlowContext fInputFlowContext;
	
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
	
	public void checkInput(RefactoringStatus status, String methodName, IJavaProject scope) {
		TypeBinding[] arguments= getArgumentTypes();
		ReferenceBinding type= getEnclosingMethod().binding.declaringClass;
		status.merge(Checks.checkMethodInType(type, methodName, arguments, scope));
		status.merge(Checks.checkMethodInHierarchy(type.superclass(), methodName, arguments, scope));
	}
	
	private TypeBinding[] getArgumentTypes() {
		TypeBinding[] result= new TypeBinding[fArguments.length];
		for (int i= 0; i < fArguments.length; i++) {
			result[i]= fArguments[i].type;
		}
		return result;
	}
	
	private String getMethodName(MethodBinding binding) {
		StringBuffer buffer= new StringBuffer();
		buffer.append(binding.declaringClass.sourceName());
		buffer.append('.');
		buffer.append(binding.selector);
		return buffer.toString();
	}
	
	private RefactoringStatus analyzeSelection(RefactoringStatus status) {
		fInputFlowContext= new FlowContext(0, fOuterMostMethodScope.analysisIndex);
		fInputFlowContext.setConsiderExecutionFlow(true);
		fInputFlowContext.setConsiderAccessMode(true);
		fInputFlowContext.setComputeMode(FlowContext.ARGUMENTS);
		
		InOutFlowAnalyzer flowAnalyzer= new InOutFlowAnalyzer(fInputFlowContext, fSelection);
		fInputFlowInfo= flowAnalyzer.analyse(fTopNodes, (BlockScope)getEnclosingScope());
		
		if (fInputFlowInfo.branches()) {
			status.addFatalError("Selection contains branch statement but corresponding branch target isn't selected.");
			fReturnKind= ERROR;
			return status;
		}
		if (fInputFlowInfo.isValueReturn()) {
			fReturnKind= RETURN_STATEMENT_VALUE;
		} else  if (fInputFlowInfo.isVoidReturn()) {
			fReturnKind= RETURN_STATEMENT_VOID;
		} else if (fInputFlowInfo.isNoReturn() || fInputFlowInfo.isThrow() || fInputFlowInfo.isUndefined()) {
			fReturnKind= NO;
		}
		
		if (fReturnKind == UNDEFINED) {
			status.addFatalError(RefactoringCoreMessages.getString("FlowAnalyzer.execution_flow")); //$NON-NLS-1$
			fReturnKind= ERROR;
			return status;
		}
		computeInput();
		computeOutput(status);
		if (!status.hasFatalError())
			adjustArgumentsAndMethodLocals();
		compressArrays();
		return status;
	}
	
	private void computeInput() {
		int argumentMode= FlowInfo.READ | FlowInfo.READ_POTENTIAL | FlowInfo.WRITE_POTENTIAL | FlowInfo.UNKNOWN;
		fArguments= fInputFlowInfo.get(fInputFlowContext, argumentMode);
		removeSelectedDeclarations(fArguments);
		fMethodLocals= fInputFlowInfo.get(fInputFlowContext, FlowInfo.WRITE | FlowInfo.WRITE_POTENTIAL);
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
		LocalVariableBinding[] returnValues= flowInfo.get(flowContext, FlowInfo.WRITE | FlowInfo.WRITE_POTENTIAL | FlowInfo.UNKNOWN);
		
		flowContext.setComputeMode(FlowContext.ARGUMENTS);
		flowInfo= new InputFlowAnalyzer(flowContext, fSelection).analyse(getEnclosingMethod(), fClassScope);
		LocalVariableBinding[] reads= flowInfo.get(flowContext, FlowInfo.READ | FlowInfo.READ_POTENTIAL | FlowInfo.UNKNOWN);
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
	
	private void adjustArgumentsAndMethodLocals() {
		for (int i= 0; i < fArguments.length; i++) {
			LocalVariableBinding argument= fArguments[i];
			if (fInputFlowInfo.hasAccessMode(fInputFlowContext, argument, FlowInfo.WRITE_POTENTIAL)) {
				if (argument != fReturnValue)
					fArguments[i]= null;
				// We didn't remove the argument. So we have to remove the local declaration
				if (fArguments[i] != null) {
					for (int l= 0; l < fMethodLocals.length; l++) {
						if (fMethodLocals[l] == argument)
							fMethodLocals[l]= null;						
					}
				}
			}
		}
	}
	
	private void compressArrays() {
		fArguments= compressArray(fArguments);
		fCallerLocals= compressArray(fCallerLocals);
		fMethodLocals= compressArray(fMethodLocals);
	}
	
	private LocalVariableBinding[] compressArray(LocalVariableBinding[] array) {
		if (array == null)
			return null;
		int size= 0;
		for (int i= 0; i < array.length; i++) {
			if (array[i] != null)
				size++;	
		}
		if (size == array.length)
			return array;
		LocalVariableBinding[] result= new LocalVariableBinding[size];
		for (int i= 0, r= 0; i < array.length; i++) {
			if (array[i] != null)
				result[r++]= array[i];		
		}
		return result;
	}
}

