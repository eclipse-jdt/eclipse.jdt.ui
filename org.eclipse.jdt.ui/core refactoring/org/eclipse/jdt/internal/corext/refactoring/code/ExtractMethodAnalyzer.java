/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConditionalExpression;
import org.eclipse.jdt.internal.compiler.ast.DoStatement;
import org.eclipse.jdt.internal.compiler.ast.ExplicitConstructorCall;
import org.eclipse.jdt.internal.compiler.ast.ForStatement;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NullLiteral;
import org.eclipse.jdt.internal.compiler.ast.Reference;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.BaseTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.problem.ProblemHandler;
import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.ExtendedBuffer;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.FlowContext;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.FlowInfo;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.InOutFlowAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.InputFlowAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.util.CodeAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.util.Selection;

/* package */ class ExtractMethodAnalyzer extends CodeAnalyzer {

	public static final int ERROR=					-2;
	public static final int UNDEFINED=				-1;
	public static final int NO=						0;
	public static final int EXPRESSION=				1;
	public static final int ACCESS_TO_LOCAL=		2;
	public static final int RETURN_STATEMENT_VOID=	3;
	public static final int RETURN_STATEMENT_VALUE=	4;
	public static final int MULTIPLE=				5;

	private static final char[] VOID= {'v', 'o', 'i', 'd'};

	private int[] fLineSeparatorPositions;
	
	private boolean fNeedsSemicolon;
	private int fReturnKind;
	
	private FlowInfo fInputFlowInfo;
	private FlowContext fInputFlowContext;
	
	private LocalVariableBinding[] fArguments;
	private LocalVariableBinding[] fMethodLocals;
	
	private LocalVariableBinding fReturnValue;
	private LocalVariableBinding[] fCallerLocals;
	private LocalVariableBinding fReturnLocal;
	
	private TypeBinding[] fExceptions;	
	
	public ExtractMethodAnalyzer(ExtendedBuffer buffer, Selection selection) {
		super(buffer, selection);
	}
	
	public AbstractMethodDeclaration getEnclosingMethod() {
		return fEnclosingMethod;
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
				if (fEnclosingMethod instanceof MethodDeclaration) {
					return ((MethodDeclaration)fEnclosingMethod).returnType;
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
		// Copy the return type from the enclosing method if possible.
		if (fEnclosingMethod instanceof MethodDeclaration) {
			TypeReference type= ((MethodDeclaration)fEnclosingMethod).returnType;
			if (type == null)
				return ""; //$NON-NLS-1$
			else
				return type.toStringExpression(0);
		} else {
			// We have a constructor. So no return type
			return ""; //$NON-NLS-1$
		}
	}
	
	public boolean getNeedsSemicolon() {
		return fNeedsSemicolon;
	}
	
	//---- Activation checking ---------------------------------------------------------------------------
	
	public void checkActivation(RefactoringStatus status) {
		status.merge(getStatus());
		checkExpression(status);
		if (status.hasFatalError())
			return;
			
		fReturnKind= UNDEFINED;
		if (analyzeSelection(status).hasFatalError())
			return;

		int returns= fReturnKind == NO ? 0 : 1;
		if (fReturnValue != null) {
			fReturnKind= ACCESS_TO_LOCAL;
			returns++;
		}
		if (getExpressionTypeBinding() != null) {
			fReturnKind= EXPRESSION;
			returns++;
		}
			
		if (returns > 1) {
			status.addFatalError("Ambiguous return value: expression, access to local or return statement extracted."); //$NON-NLS-1$
			fReturnKind= MULTIPLE;
		}
	}
	
	private void checkExpression(RefactoringStatus status) {
		AstNode[] nodes= getSelectedNodes();
		if (nodes != null && nodes.length == 1) {
			AstNode node= nodes[0];
			if (node instanceof NullLiteral) {
				status.addFatalError("Cannot extract the single keyword null.");
			} else if (node instanceof ArrayInitializer) {
				status.addFatalError("Currently no support to extract an array initializer.");
			} else if (node instanceof TypeReference) {
				status.addFatalError("Currently no support to extract a single type reference.");
			} else if (node instanceof Assignment) {
				if (isExpressionPartOfOperator())
					status.addFatalError("Cannot extract assignment that is part of another expression.");
			} else if (node instanceof ConditionalExpression) {
				status.addFatalError("Currently no support to extract a single conditional expression.");
			} else if (node instanceof Reference) {
				status.addFatalError("Currently no support to extract a single variable or field reference.");
			}
		}
	}
	
	//---- Input checking -----------------------------------------------------------------------------------
		
	public void checkInput(RefactoringStatus status, String methodName, IJavaProject scope) {
		TypeBinding[] arguments= getArgumentTypes();
		ReferenceBinding type= fEnclosingMethod.binding.declaringClass;
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
		
		InOutFlowAnalyzer flowAnalyzer= new InOutFlowAnalyzer(fInputFlowContext, getSelection());
		fInputFlowInfo= flowAnalyzer.analyse(getSelectedNodes(), (BlockScope)getEnclosingScope());
		
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
		computeExceptions();
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
		Selection selection= getSelection();
		for (int i= 0; i < bindings.length; i++) {
			LocalDeclaration decl= bindings[i].declaration;
			if (selection.covers(decl))
				bindings[i]= null;
		}
	}
	
	private void computeOutput(RefactoringStatus status) {
		// First find all writes inside the selection.
		FlowContext flowContext= new FlowContext(0, fOuterMostMethodScope.analysisIndex);
		flowContext.setConsiderExecutionFlow(false);
		flowContext.setConsiderAccessMode(true);
		flowContext.setComputeMode(FlowContext.RETURN_VALUES);
		FlowInfo returnInfo= new InOutFlowAnalyzer(flowContext, getSelection()).analyse(getSelectedNodes(), (BlockScope)getEnclosingScope());
		LocalVariableBinding[] returnValues= returnInfo.get(flowContext, FlowInfo.WRITE | FlowInfo.WRITE_POTENTIAL | FlowInfo.UNKNOWN);
		
		int counter= 0;
		flowContext.setComputeMode(FlowContext.ARGUMENTS);
		FlowInfo argInfo= new InputFlowAnalyzer(flowContext, getSelection()).analyse(fEnclosingMethod, fClassScope);
		LocalVariableBinding[] reads= argInfo.get(flowContext, FlowInfo.READ | FlowInfo.READ_POTENTIAL | FlowInfo.UNKNOWN);
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
		LocalVariableBinding[] writes= argInfo.get(flowContext, FlowInfo.WRITE);
		for (int i= 0; i < writes.length; i++) {
			LocalVariableBinding write= writes[i];
			if (getSelection().covers(write.declaration))
				callerLocals.add(write);
		}
		fCallerLocals= (LocalVariableBinding[])callerLocals.toArray(new LocalVariableBinding[callerLocals.size()]);
		if (fReturnValue != null && getSelection().covers(fReturnValue.declaration))
			fReturnLocal= fReturnValue;
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
	
	//---- Change creation ----------------------------------------------------------------------------------
	
	public void aboutToCreateChange() {
		fNeedsSemicolon= SemicolonAnalyzer.perform(getSelectedNodes(), (BlockScope)getEnclosingScope());
	}

	//---- Exceptions -----------------------------------------------------------------------------------------
	
	public TypeBinding[] getExceptions() {
		return fExceptions;
	}
	
	private void computeExceptions() {
		fExceptions= ExceptionAnalyzer.perform(fEnclosingMethod, getSelectedNodes(), (BlockScope)fEnclosingScope);
	}
	
	//---- Problem management -----------------------------------------------------
	
	/* non Java-doc
	 * @see IAbstractSyntaxTreeVisitor#acceptProblem
	 */
	public void acceptProblem(IProblem problem) {
		if (problem.isWarning())
			return;
			
		reset();
		fStatus.addFatalError(RefactoringCoreMessages.getFormattedString("Refactoring.compilation_error",  //$NON-NLS-1$
								new Object[]{new Integer(problem.getSourceLineNumber()), problem.getMessage()}));
	}
	
	protected int getLineNumber(AstNode node){
		Assert.isNotNull(fLineSeparatorPositions);
		return ProblemHandler.searchLineNumber(fLineSeparatorPositions, node.sourceStart);
	}
	
	//---- Special visitor methods ---------------------------------------------------------------------------

	protected void handleNextSelectedNode(AstNode node) {
		checkParent();
		super.handleNextSelectedNode(node);
	}
	
	protected void handleSelectionEndsIn(AstNode node) {
		invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.ends_middle_of_statement")); //$NON-NLS-1$
		super.handleSelectionEndsIn(node);
	}
		
	private void checkParent() {
		List parents= internalGetParents();
		for (int i= parents.size() - 1; i >= 0; i--) {
			AstNode node= (AstNode)parents.get(i);
			if (node == fParentOfFirstSelectedNode)
				return;
		}
		invalidSelection("Not all selected statements are enclosed by the same parent statement.");
	}
	
	public boolean visit(CompilationUnitDeclaration node, CompilationUnitScope scope) {
		fLineSeparatorPositions= node.compilationResult.lineSeparatorPositions;
		return super.visit(node, scope);
	}
	
	public void endVisit(CompilationUnitDeclaration node, CompilationUnitScope scope) {
		superCall: {
			if (fStatus.hasFatalError())
				break superCall;
			if (fSelectedNodes == null || fSelectedNodes.size() == 0) {
				fStatus.addFatalError(RefactoringCoreMessages.getString("StatementAnalyzer.only_method_body")); //$NON-NLS-1$
				break superCall;
			}
			initialize();
			if (fEnclosingMethod == null)
				fStatus.addFatalError(RefactoringCoreMessages.getString("StatementAnalyzer.only_method_body")); //$NON-NLS-1$
		}
		super.endVisit(node, scope);
	}
	
	protected void checkSelectedNodes() {
		super.checkSelectedNodes();
		if (fStatus.hasFatalError())
			return;
	}
	
	public boolean visit(DoStatement node, BlockScope scope) {
		boolean result= super.visit(node, scope);
		
		int actionStart= getBuffer().indexAfter(Scanner.TokenNamedo, node.sourceStart);
		if (getSelection().start == actionStart) {
			invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.after_do_keyword")); //$NON-NLS-1$
			return false;
		}
		
		return result;
	}
	
	public boolean visit(ExplicitConstructorCall node, BlockScope scope) {
		boolean result= super.visit(node, scope);
		if (getSelection().getVisitSelectionMode(node) == Selection.SELECTED) {
			invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.super_or_this")); //$NON-NLS-1$
			return false;
		}
		return result;
	}
	
	public void endVisit(ForStatement node, BlockScope scope) {
		int mode= getSelection().getEndVisitSelectionMode(node);
		if (mode == Selection.AFTER) {
			if (contains(node.initializations, getFirstSelectedNode())) {
				invalidSelection("Cannot extract initialization part of a for statement.");
				return;
			} else if (contains(node.increments, getLastSelectedNode())) {
				invalidSelection("Cannot extract increment part of a for statement.");
				return;
			}
		}
		super.endVisit(node, scope);
	}		
}

