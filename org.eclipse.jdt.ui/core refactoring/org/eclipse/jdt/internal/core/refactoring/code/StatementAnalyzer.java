/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;

import org.eclipse.jdt.internal.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.compiler.problem.ProblemHandler;
import org.eclipse.jdt.internal.compiler.util.CharOperation;
import org.eclipse.jdt.internal.core.refactoring.ASTEndVisitAdapter;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.AstNodeData;
import org.eclipse.jdt.internal.core.refactoring.ExtendedBuffer;
import org.eclipse.jdt.internal.core.refactoring.IParentTracker;
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;

/**
 * Checks whether the source range denoted by <code>start</code> and <code>end</code>
 * selects a set of statements.
 */
/* package */ class StatementAnalyzer extends ASTEndVisitAdapter {
	
	static final int UNDEFINED=   0;
	static final int BEFORE=	1;
	static final int SELECTED=	2;
	static final int AFTER=	      3;
	
	// Parent tracking interface
	private IParentTracker fParentTracker;
	
	// The selection's start and end position
	private ExtendedBuffer fBuffer;
	private Selection fSelection;
	
	// internal state.
	private int fMode;
	private int fCursorPosition;
	private boolean fCompileErrorFound;

	// Error handling	
	private RefactoringStatus fStatus= new RefactoringStatus();
	private int[] fLineSeparatorPositions;	
	private String fMessage;
	
	private AstNode fFirstSelectedNode;
	private AstNode fParentOfFirstSelectedStatment;
	private AstNode fLastSelectedNode;
	private AstNode fLastSelectedNodeWithSameParentAsFirst;
	private boolean fNeedsSemicolon;
	
	private AbstractMethodDeclaration fEnclosingMethod;
	
	private LocalVariableAnalyzer fLocalVariableAnalyzer;
	private LocalTypeAnalyzer fLocalTypeAnalyzer;
	private ExceptionAnalyzer fExceptionAnalyzer;
	
	// Special return statement handling
	private int fAdjustedSelectionEnd= -1;
	private String fPotentialReturnMessage;
	
	// Handling label and branch statements.
	private Stack fImplicitBranchTargets= new Stack();	
	private List fLabeledStatements= new ArrayList(2);
	
	// A hashtable to add additional data to an AstNode
	private AstNodeData fAstNodeData= new AstNodeData();
	
	private static final int BREAK_LENGTH= "break".length(); //$NON-NLS-1$
	private static final int CONTINUE_LENGTH= "continue".length(); //$NON-NLS-1$
	private static final int DO_LENGTH=    "do".length(); //$NON-NLS-1$
	private static final int ELSE_LENGTH=  "else".length(); //$NON-NLS-1$
	private static final int WHILE_LENGTH= "while".length(); //$NON-NLS-1$
	 
	public StatementAnalyzer(ExtendedBuffer buffer, int start, int length, boolean asymetricAssignment) {
		// System.out.println("Start: " + start + " length: " + length);
		fBuffer= buffer;
		Assert.isTrue(fBuffer != null);
		fSelection= new Selection(start, length);
		fLocalVariableAnalyzer= new LocalVariableAnalyzer(this, asymetricAssignment);
		fLocalTypeAnalyzer= new LocalTypeAnalyzer();
		fExceptionAnalyzer= new ExceptionAnalyzer();
	}

	//---- Parent tracking ----------------------------------------------------------
	
	/**
	 * Sets the parent tracker to access the parent of a active
	 * AST node.
	 */
	public void setParentTracker(IParentTracker tracker) {
		fParentTracker= tracker;
	}
	
	private AstNode getParent() {
		return fParentTracker.getParent();
	}
	
	//---- Precondition checking ----------------------------------------------------
	
	/**
	 * Checks if the refactoring can be activated.
	 */
	public void checkActivation(RefactoringStatus status) {
		if (fEnclosingMethod == null || fLastSelectedNode == null) {
			if (fMessage == null && !fStatus.hasFatalError())
				// begin PR: 1GEWDR8: ITPJCORE:WINNT - Refactoring - inconsistent error message for refusing extraction
				fMessage= RefactoringCoreMessages.getString("StatementAnalyzer.only_method_body"); //$NON-NLS-1$
				// end PR
			if (fMessage != null)
				status.addFatalError(fMessage);
		}
		status.merge(fStatus);
		if (fFirstSelectedNode == fLastSelectedNodeWithSameParentAsFirst &&
				fFirstSelectedNode instanceof ReturnStatement) {
			status.addFatalError(RefactoringCoreMessages.getString("StatementAnalyzer.single_return")); //$NON-NLS-1$
		}
		fLocalVariableAnalyzer.checkActivation(status);
		fLocalTypeAnalyzer.checkActivation(status);
	}
	
	/**
	 * Returns the local variable analyzer used by this statement analyzer.
	 */
	public LocalVariableAnalyzer getLocalVariableAnalyzer() {
		return fLocalVariableAnalyzer;
	}
	 
	/**
	 * Returns the method that encloses the text selection. Returns <code>null</code>
	 * is the text selection isn't enclosed by a method or is the text selection doesn't
	 * mark a valid set of statements.
	 * @return the method that encloses the text selection.
	 */
	public AbstractMethodDeclaration getEnclosingMethod() {
		return fEnclosingMethod;
	}
	
	/**
	 * Returns <code>true</code> if the given AST node is selected in the text editor.
	 * Otherwise <code>false</code> is returned.
	 * @return whether or not the given AST node is selected in the editor.
	 */
	public boolean isSelected(AstNode node) {
		return fSelection.start <= node.sourceStart && node.sourceEnd <= fSelection.end;
	}
	
	/**
	 * Returns the ending position of the last selected statement.
	 */
	public int getLastSelectedStatementEnd() {
		return fLastSelectedNode.sourceEnd;
	}
	
	public String getSignature(String methodName) {
		String modifier= ""; //$NON-NLS-1$
		if ((fEnclosingMethod.modifiers & AstNode.AccStatic) != 0)
			modifier= "static "; //$NON-NLS-1$
			
		return modifier + fLocalVariableAnalyzer.getCallSignature(methodName)
		       + fExceptionAnalyzer.getThrowSignature();
	}
	
	/**
	 * Returns true if the last selected statement needs a semicolon to have correct
	 * syntax. For example a block or a try / catch / finnally doesn't need a semicolon
	 * at the end.
	 */
	public boolean getNeedsSemicolon() {
		return fNeedsSemicolon;
	}
	
	/**
	 * Returns the new selection end value, if the selection has to be adjusted. Returns
	 * -1 if the current selection is ok.
	 * 
	 * @return the adjusted selection end or -1 if the selection doesn't have to be
	 *  adjusted
	 */
	public int getAdjustedSelectionEnd() {
		return fAdjustedSelectionEnd;
	}
	
	//---- Helper methods -----------------------------------------------------------------------
	
	private void reset() {
		fMode= UNDEFINED;
		fFirstSelectedNode= null;
		fLastSelectedNode= null;
		fEnclosingMethod= null;
		fStatus= new RefactoringStatus();
		fMessage= null;
		fNeedsSemicolon= true;
	}
	
	private void invalidSelection(String message) {
		reset();
		fMessage= message;
		fCursorPosition= Integer.MAX_VALUE;
	}
	
	private boolean visitNode(AstNode node, Scope scope) {
		return visitRange(node.sourceStart, node.sourceEnd, node, scope);
	}
	
	private boolean visitRange(int start, int end, AstNode node, Scope scope) {
		boolean result= true;
		switch(fMode) {
			case UNDEFINED:
				return false;			
			case BEFORE:
				if (fCursorPosition < fSelection.start && fSelection.covers(start, end)) {
					startFound(node);
				}
				break;
			case SELECTED:
				if (fSelection.endsIn(start, end)) { // Selection ends in the middle of a statement
					invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.ends_middle_of_statement")); //$NON-NLS-1$
					return false;
				} else if (start > fSelection.end) {
					fMode= AFTER;
				} else {
					trackLastSelectedNode(node);
					fNeedsSemicolon= true;
				}
				break;
			case AFTER:
				break;
		}
		// PR: 1GEWDJ4: ITPJCORE:WINNT - Refactoring - invalid variable initialization extraction
		trackLastEnd(end);
		return result;
	}
	
	private void startFound(AstNode node) {
		fMode= SELECTED;
		fFirstSelectedNode= node;
		fParentOfFirstSelectedStatment= getParent();
		fLastSelectedNode= node;
		fLastSelectedNodeWithSameParentAsFirst= node;
	}
	
	private void trackLastSelectedNode(AstNode node) {
		fLastSelectedNode= node;
		if (fParentOfFirstSelectedStatment == getParent())
			fLastSelectedNodeWithSameParentAsFirst= node;
	}
	
	private void trackLastEnd(int end) {
		if (end > fCursorPosition)
			fCursorPosition= end;
	}
	
	private boolean visitAssignment(Assignment assignment, BlockScope scope, boolean compound) {
		if (!visitNode(assignment, scope))
			return false;
			
		fLocalVariableAnalyzer.visitAssignment(assignment, scope, fMode, compound);
		return true;
	}
	
	private boolean visitAbstractMethodDeclaration(AbstractMethodDeclaration node, Scope scope) {
		
		// Skip method generated by the compiler (e.g. not present in source code) like constructors
		if (node.modifiersSourceStart == 0 && node.bodyStart == 0)
			return false;
			
		// Skip methods not covered by the selection
		if (fMode == AFTER || (fMode == UNDEFINED && fSelection.start > node.declarationSourceEnd)) // end doens't include '}'
			return false;
			
		boolean result= false;		
		boolean enclosed= fSelection.coveredBy(node.bodyStart, node.bodyEnd);
		// Do a reset even if we are in BEFORE mode. We can extract a method defined
		// inside a method.
		if (!fCompileErrorFound && enclosed && (fMode == UNDEFINED || fMode == BEFORE)) {
			if (fMode == UNDEFINED) {
				CommentAnalyzer commentAnalyzer= new CommentAnalyzer();
				fStatus.merge(commentAnalyzer.check(fSelection, fBuffer.getCharacters(),
					node.declarationSourceStart, node.declarationSourceEnd));
				if (fStatus.hasFatalError())
					return false;				
			}
			fExceptionAnalyzer.visitAbstractMethodDeclaration(node, scope);
			reset();
			fEnclosingMethod= node;
			fMode= BEFORE;
			fCursorPosition= node.bodyStart - 1;
			result= true;
		} else {
			// treat it as a normal node (e.g. if a whole anonymous inner class is selected.
			result= visitRange(node.declarationSourceStart, node.declarationSourceEnd, node, scope);
			if (fMode == AFTER)
				result= false;		// don't dive into method defined after the method that contains the selection.
		}
		return result;
	}
	
	private void endVisitAbstractMethodDeclaration(AbstractMethodDeclaration node, Scope scope) {
		if (node == fEnclosingMethod) {
			checkLastSelectedStatement();
		}
	}
	
	private boolean visitLocalTypeDeclaration(TypeDeclaration declaration, BlockScope scope) {
		if (!checkLocalTypeDeclaration(declaration))
			return false;
		
		boolean result= visitRange(declaration.declarationSourceStart, declaration.declarationSourceEnd,
			declaration, scope);
		fLocalTypeAnalyzer.visitLocalTypeDeclaration(declaration, scope, fMode);
		return result;
	}
	
	private boolean visitTypeReference(TypeReference reference, BlockScope scope) {
		fLocalTypeAnalyzer.visitTypeReference(reference, scope, fMode);
		return false;
	}
	
	private boolean visitImplicitBranchTarget(Statement statement, BlockScope scope) {
		fImplicitBranchTargets.push(statement);
		return visitNode(statement, scope);
	}
	
	private void endVisitImplicitBranchTarget(Statement statement, BlockScope scope) {
		fImplicitBranchTargets.pop();
	}
	
	private boolean visitBranchStatement(BranchStatement statement, BlockScope scope, String name) {
		boolean result= visitNode(statement, scope);
		Statement target= findTarget(statement);
		String label= "label"; //* new String(statement.label); //$NON-NLS-1$
		if (target != null) {
			if (isSelected(target)) {
				if (fMode != SELECTED)
					fStatus.addFatalError(RefactoringCoreMessages.getFormattedString("StatementAnalyzer.not_all_selected", new String[]{name, name})); //$NON-NLS-1$
			} else {
				if (fMode == SELECTED)
					fStatus.addFatalError(RefactoringCoreMessages.getFormattedString("StatementAnalyzer.targer_not_selected", new String[]{name, name})); //$NON-NLS-1$
			}
		} else {
			fStatus.addFatalError(RefactoringCoreMessages.getString("StatementAnalyzer.no_break_target")); //$NON-NLS-1$
		}
		return result;
	}
	
	private Statement findTarget(BranchStatement statement) {
		if (statement.label == null)
			return (Statement)fImplicitBranchTargets.peek();
		char[] label= statement.label;
		for (Iterator iter= fLabeledStatements.iterator(); iter.hasNext(); ) {
			LabeledStatement ls= (LabeledStatement)iter.next();
			if (CharOperation.equals(label, ls.label))
				return ls;
		}
		return null;
	}
	
	private void checkLastSelectedStatement() {
		if (fLastSelectedNodeWithSameParentAsFirst instanceof ReturnStatement) {
			ReturnStatement statement= (ReturnStatement)fLastSelectedNodeWithSameParentAsFirst;
			if (statement.expression == null) {
				fPotentialReturnMessage= null;
				fLocalVariableAnalyzer.setExtractedReturnStatement(statement);
			} else {
				invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.void_return")); //$NON-NLS-1$
			}
		} else {
			handlePotentialReturnMessage();
			fAdjustedSelectionEnd= -1;
		}
	}
	
	private void handlePotentialReturnMessage() {
		if (fPotentialReturnMessage != null) {
			fStatus.addFatalError(fPotentialReturnMessage);
			fPotentialReturnMessage= null;
		}
	}
	
	private boolean checkLocalTypeDeclaration(TypeDeclaration declaration) {
		if (fSelection.intersects(declaration.declarationSourceStart, declaration.declarationSourceEnd)) {
			invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.middle_of_type_declaration")); //$NON-NLS-1$
			return false;
		}
		return true;
	}	
	
	//--- node management ---------------------------------------------------------
	
	private boolean isPartOfNodeSelected(AstNode node) {
		return fMode == BEFORE && fSelection.end < fBuffer.indexOfStatementCharacter(node.sourceEnd + 1);
	}
	
	//--- Expression / Condition handling -----------------------------------------

	public boolean visitBinaryExpression(BinaryExpression binaryExpression, BlockScope scope, int returnType) {
		int currentScanPosition= fCursorPosition;
		if (!visitNode(binaryExpression, scope))
			return false;
		if (fMode != SELECTED)
			return true;
			
		if (isTopMostNodeInSelection(currentScanPosition, binaryExpression)) {
			if (returnType == TypeIds.T_undefined)
				returnType= binaryExpression.bits & binaryExpression.ReturnTypeIDMASK;
			if (returnType == TypeIds.T_undefined) {
				invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.Cannot_determine_return_type")); //$NON-NLS-1$
				return false;
			}
			fLocalVariableAnalyzer.setExpressionReturnType(TypeReference.baseTypeReference(returnType, 0));
		}
		return true;	
	}

	private boolean isConditionSelected(AstNode node, Expression condition, int scanStart) {
		if (node == null || condition == null || scanStart < 0)
			return false;
		int conditionStart= fBuffer.indexOf('(', scanStart) + 1;
		int conditionEnd= fBuffer.indexOf(')', condition.sourceEnd + 1) - 1;
		if (fSelection.coveredBy(conditionStart, conditionEnd)) {
			fAstNodeData.put(node, new Boolean(true));
			fCursorPosition= conditionStart - 1;
			return true;
		}
		return false;
	}
		
	private boolean isTopMostNodeInSelection(int rangeStart, AstNode node) {
		int nextStart= fBuffer.indexOfStatementCharacter(node.sourceEnd + 1);
		return fSelection.coveredBy(rangeStart, nextStart - 1) && fSelection.covers(node);
	}

	private void endVisitConditionBlock(AstNode node, String statementName) {
		Boolean inCondition= (Boolean)fAstNodeData.get(node);
		if (inCondition != null && inCondition.booleanValue() && fLocalVariableAnalyzer.getExpressionReturnType() == null) {
			invalidSelection(RefactoringCoreMessages.getFormattedString("StatementAnalyzer.cannot_from_condition_part", statementName)); //$NON-NLS-1$
		}	
	}
		
	//---- Problem management -----------------------------------------------------
	
	public void acceptProblem(IProblem problem) {
		if (problem.isWarning())
			return;
			
		reset();
		fCursorPosition= Integer.MAX_VALUE;
		fStatus.addFatalError(RefactoringCoreMessages.getFormattedString("StatementAnalyzer.compilation_error",  //$NON-NLS-1$
								new Object[]{new Integer(problem.getSourceLineNumber()), problem.getMessage()}));
		fCompileErrorFound= true;
	}
	
	private int getLineNumber(AstNode node){
		Assert.isNotNull(fLineSeparatorPositions);
		return ProblemHandler.searchLineNumber(fLineSeparatorPositions, node.sourceStart);
	}
	
	//---- Compilation Unit -------------------------------------------------------
	
	public boolean visit(CompilationUnitDeclaration compilationUnitDeclaration, CompilationUnitScope scope) {
		fLineSeparatorPositions= compilationUnitDeclaration.compilationResult.lineSeparatorPositions;
		return fSelection.enclosedBy(compilationUnitDeclaration);
	}
	
	public boolean visit(ImportReference importRef, CompilationUnitScope scope) {
		return false;
	}
	
	public boolean visit(TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
		return fSelection.enclosedBy(typeDeclaration.declarationSourceStart,
			typeDeclaration.declarationSourceEnd);
	}
	
	//---- Type -------------------------------------------------------------------
	
	public boolean visit(Clinit clinit, ClassScope scope) {
		return visitAbstractMethodDeclaration(clinit, scope);
	}
	
	public void endVisit(Clinit clinit, ClassScope scope) {
		endVisitAbstractMethodDeclaration(clinit, scope);
	}
	
	public boolean visit(TypeDeclaration typeDeclaration, ClassScope scope) {
		return fSelection.enclosedBy(typeDeclaration.declarationSourceStart,
			typeDeclaration.declarationSourceEnd);
	}
	
	public boolean visit(MemberTypeDeclaration memberTypeDeclaration, ClassScope scope) {
		return fSelection.enclosedBy(memberTypeDeclaration.declarationSourceStart,
			memberTypeDeclaration.declarationSourceEnd);
	}
	
	public boolean visit(FieldDeclaration fieldDeclaration, MethodScope scope) {
		return false;
	}
	
	public boolean visit(Initializer initializer, MethodScope scope) {
		return false;
	}
	
	public boolean visit(ConstructorDeclaration constructorDeclaration, ClassScope scope) {
		return visitAbstractMethodDeclaration(constructorDeclaration, scope);
	}
	
	public void endVisit(ConstructorDeclaration constructorDeclaration, ClassScope scope) {
		endVisitAbstractMethodDeclaration(constructorDeclaration, scope);
	}
	
	public boolean visit(MethodDeclaration methodDeclaration, ClassScope scope) {
		return visitAbstractMethodDeclaration(methodDeclaration, scope);
	}
	
	public void endVisit(MethodDeclaration methodDeclaration, ClassScope scope) {
		endVisitAbstractMethodDeclaration(methodDeclaration, scope);
	}
	
	public boolean visit(SingleTypeReference singleTypeReference, ClassScope scope) {
		return false;
	}
	
	public boolean visit(QualifiedTypeReference qualifiedTypeReference, ClassScope scope) {
		return false;
	}
	
	public boolean visit(ArrayTypeReference arrayTypeReference, ClassScope scope) {
		return false;
	}
	
	public boolean visit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, ClassScope scope) {
		return false;
	}
	
	//---- Methods ----------------------------------------------------------------
	
	public boolean visit(LocalTypeDeclaration localTypeDeclaration, MethodScope scope) {
		return visitLocalTypeDeclaration(localTypeDeclaration, scope);
	}
	
	public boolean visit(Argument argument, BlockScope scope) {
		return false;
	}
	
	//---- Methods / Block --------------------------------------------------------
	
	public boolean visit(TypeDeclaration typeDeclaration, BlockScope scope) {
		return visitLocalTypeDeclaration(typeDeclaration, scope);
	}
	
	public boolean visit(AnonymousLocalTypeDeclaration anonymousTypeDeclaration, BlockScope scope) {
		if (!checkLocalTypeDeclaration(anonymousTypeDeclaration))
			return false;
			
		return visitRange(anonymousTypeDeclaration.declarationSourceStart,
			anonymousTypeDeclaration.declarationSourceEnd, anonymousTypeDeclaration, scope);
	}
	
	public boolean visit(LocalDeclaration localDeclaration, BlockScope scope) {
		return visitNode(localDeclaration, scope);

		// XXX: 1GF089K: ITPJCORE:WIN2000 - AbstractLocalDeclaration.declarationSourceStart includes preceeding comment
		// int start= fBuffer.indexOfStatementCharacter(localDeclaration.declarationSourceStart);
		// boolean result= visitRange(start, localDeclaration.declarationSourceEnd, localDeclaration, scope);
		// if (localDeclaration.declarationSourceStart <= fSelection.end && fSelection.end < localDeclaration.declarationSourceEnd) {
		//	invalidSelection("Cannot extract parts from a multiple variable declaration.");
		// }
		// return result;	
	}

	public boolean visit(FieldReference fieldReference, BlockScope scope) {
		return false;
	}

	public boolean visit(ArrayReference arrayReference, BlockScope scope) {
		return visitNode(arrayReference, scope);
	}

	public boolean visit(ArrayTypeReference arrayTypeReference, BlockScope scope) {
		return visitTypeReference(arrayTypeReference, scope);
	}

	public boolean visit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, BlockScope scope) {
		return visitTypeReference(arrayQualifiedTypeReference, scope);
	}

	public boolean visit(SingleTypeReference singleTypeReference, BlockScope scope) {
		return visitTypeReference(singleTypeReference, scope);
	}

	public boolean visit(QualifiedTypeReference qualifiedTypeReference, BlockScope scope) {
		return visitTypeReference(qualifiedTypeReference, scope);
	}

	public boolean visit(QualifiedNameReference qualifiedNameReference, BlockScope scope) {
		boolean result= visitNode(qualifiedNameReference, scope);
		if (result) {
			fLocalVariableAnalyzer.visit(qualifiedNameReference, scope, fMode);
		}
		return result;
	}

	public boolean visit(QualifiedSuperReference qualifiedSuperReference, BlockScope scope) {
		return false;
	}

	public boolean visit(QualifiedThisReference qualifiedThisReference, BlockScope scope) {
		return false;
	}

	public boolean visit(SingleNameReference singleNameReference, BlockScope scope) {
		boolean result= visitNode(singleNameReference, scope);
		if (result) {
			fLocalVariableAnalyzer.visit(singleNameReference, scope, fMode);
			fLocalTypeAnalyzer.visit(singleNameReference, scope, fMode);
		}	
		return result;
	}

	public boolean visit(SuperReference superReference, BlockScope scope) {
		return false;
	}

	public boolean visit(ThisReference thisReference, BlockScope scope) {
		return false;
	}

	//---- Statements -------------------------------------------------------------
	
	public boolean visit(AllocationExpression allocationExpression, BlockScope scope) {
		return visitNode(allocationExpression, scope);
	}
	public boolean visit(AND_AND_Expression and_and_Expression, BlockScope scope) {
		return visitBinaryExpression(and_and_Expression, scope, TypeIds.T_boolean);
	}
	
	public boolean visit(ArrayAllocationExpression arrayAllocationExpression, BlockScope scope) {
		return visitNode(arrayAllocationExpression, scope);
	}
	
	public boolean visit(ArrayInitializer arrayInitializer, BlockScope scope) {
		return visitNode(arrayInitializer, scope);
	}
	
	public boolean visit(Assignment assignment, BlockScope scope) {
		return visitAssignment(assignment, scope, false);
	}

	public boolean visit(BinaryExpression binaryExpression, BlockScope scope) {
		return visitBinaryExpression(binaryExpression, scope, TypeIds.T_undefined);
	}

	public boolean visit(Block block, BlockScope scope) {
		boolean result= visitNode(block, scope);
		
		if (fSelection.intersects(block)) {
			reset();
			fCursorPosition= Integer.MAX_VALUE;
		} else {
			fCursorPosition= block.sourceStart;
		}
		
		return result;
	}

	public void endVisit(Block block, BlockScope scope) {
		// PR: 1GEWDJ4: ITPJCORE:WINNT - Refactoring - invalid variable initialization extraction
		trackLastEnd(block.sourceEnd);
		if (fSelection.covers(block))
			fNeedsSemicolon= false;
	}

	public boolean visit(Break breakStatement, BlockScope scope) {
		if (breakStatement.label == null) {
			breakStatement.sourceEnd= breakStatement.sourceStart + BREAK_LENGTH - 1;
		}
		return visitBranchStatement(breakStatement, scope, "break"); //$NON-NLS-1$
	}

	public boolean visit(Case caseStatement, BlockScope scope) {
		return visitNode(caseStatement, scope);
	}

	public boolean visit(CastExpression castExpression, BlockScope scope) {
		return visitNode(castExpression, scope);
	}

	public boolean visit(CharLiteral charLiteral, BlockScope scope) {
		return visitNode(charLiteral, scope);
	}

	public boolean visit(ClassLiteralAccess classLiteral, BlockScope scope) {
		return visitNode(classLiteral, scope);
	}

	public boolean visit(CompoundAssignment compoundAssignment, BlockScope scope) {
		return visitAssignment(compoundAssignment, scope, true);
	}

	public boolean visit(ConditionalExpression conditionalExpression, BlockScope scope) {
		return visitNode(conditionalExpression, scope);
	}

	public boolean visit(Continue continueStatement, BlockScope scope) {
		if (continueStatement.label == null) {
			continueStatement.sourceEnd= continueStatement.sourceStart + CONTINUE_LENGTH - 1;
		}
		return visitBranchStatement(continueStatement, scope, "continue"); //$NON-NLS-1$
	}

	public boolean visit(DefaultCase defaultCaseStatement, BlockScope scope) {
		return visitNode(defaultCaseStatement, scope);
	}

	public boolean visit(DoStatement doStatement, BlockScope scope) {
		if (!visitImplicitBranchTarget(doStatement, scope))
			return false;
		
		if (isPartOfNodeSelected(doStatement)) {
			
			// Check if condition is selected
			if (isConditionSelected(doStatement, doStatement.condition, doStatement.action.sourceEnd))
				return true;
				
			// skip the string "do"
			int actionStart= doStatement.sourceStart + DO_LENGTH;		
			int actionEnd= fBuffer.indexOfStatementCharacter(doStatement.action.sourceEnd + 1) - 1;
			
			// Check if action part is selected.
			if (fSelection.coveredBy(actionStart + 1, actionEnd)) {
				fCursorPosition= actionStart;
				return true;
			}
				
			if (fSelection.start == actionStart) {
				invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.after_do_keyword")); //$NON-NLS-1$
				return false;
			}
				
			invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.do_while_statement")); //$NON-NLS-1$
			return false;
		}
		return true;
	}
	
	public void endVisit(DoStatement doStatement, BlockScope scope) {
		endVisitImplicitBranchTarget(doStatement, scope);
		endVisitConditionBlock(doStatement, RefactoringCoreMessages.getString("StatementAnalyzer.do_while")); //$NON-NLS-1$
		fCursorPosition= doStatement.sourceEnd;
	}

	public boolean visit(DoubleLiteral doubleLiteral, BlockScope scope) {
		return visitNode(doubleLiteral, scope);
	}

	public boolean visit(EqualExpression equalExpression, BlockScope scope) {
		return visitBinaryExpression(equalExpression, scope, TypeIds.T_boolean);
	}

	public boolean visit(ExplicitConstructorCall explicitConstructor, BlockScope scope) {
		return visitNode(explicitConstructor, scope);
	}

	public boolean visit(ExtendedStringLiteral extendedStringLiteral, BlockScope scope) {
		return visitNode(extendedStringLiteral, scope);
	}

	public boolean visit(FalseLiteral falseLiteral, BlockScope scope) {
		return visitNode(falseLiteral, scope);
	}

	public boolean visit(FloatLiteral floatLiteral, BlockScope scope) {
		return visitNode(floatLiteral, scope);
	}

	public boolean visit(ForStatement forStatement, BlockScope scope) {
		boolean result= visitImplicitBranchTarget(forStatement, scope);
		if (!result)
			return false;
			
		// forStatement.sourceEnd includes the statement's action. Since the
		// selection can be the statements body adjust last end if so.
		if (isPartOfNodeSelected(forStatement)) {
			if (isConditionSelected(forStatement))
				return true;
				
			int start= forStatement.sourceStart;
			if (forStatement.increments != null) {
				start= forStatement.increments[forStatement.increments.length - 1].sourceEnd;
			} else if (forStatement.condition != null) {
				start= forStatement.condition.sourceEnd;
			} else if (forStatement.initializations != null) {
				start= forStatement.initializations[forStatement.initializations.length - 1].sourceEnd;
			}
			fCursorPosition= fBuffer.indexOf(')', start + 1);
		}
		return result;
	}
	
	private boolean isConditionSelected(ForStatement forStatement) {
		if (forStatement.condition == null)
			return false;
			
		int start= forStatement.sourceStart;
		if (forStatement.initializations != null) {
			start= forStatement.initializations[forStatement.initializations.length - 1].sourceEnd;
		}
		int conditionStart= fBuffer.indexOf(';', start) + 1;
		int conditionEnd= fBuffer.indexOf(';', forStatement.condition.sourceEnd) - 1;
		if (fSelection.coveredBy(conditionStart, conditionEnd)) {
			fAstNodeData.put(forStatement, new Boolean(true));
			fCursorPosition= conditionStart - 1;
			return true;
		}	
		return false;
	}

	public void endVisit(ForStatement forStatement, BlockScope scope) {
		endVisitImplicitBranchTarget(forStatement, scope);
		endVisitConditionBlock(forStatement, RefactoringCoreMessages.getString("StatementAnalyzer.a_for")); //$NON-NLS-1$
	}
	
	public boolean visit(IfStatement ifStatement, BlockScope scope) {
		if (!visitNode(ifStatement, scope))
			return false;
			
		int nextStart= fBuffer.indexOfStatementCharacter(ifStatement.sourceEnd + 1);
		if (fMode == BEFORE && fSelection.end < nextStart) {
			if (isConditionSelected(ifStatement, ifStatement.condition, ifStatement.sourceStart))
				return true;

			if ((fCursorPosition= getIfElseBodyStart(ifStatement, nextStart) - 1) >= 0)
				return true;
				
			invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.if_then_else_statement")); //$NON-NLS-1$
			return false;
		}
		return true;
	}
	
	public void endVisit(IfStatement ifStatement, BlockScope scope) {
		endVisitConditionBlock(ifStatement, RefactoringCoreMessages.getString("StatementAnalyzer.if-then-else")); //$NON-NLS-1$
	}

	private int getIfElseBodyStart(IfStatement ifStatement, int nextStart) {
		int sourceStart;
		int sourceEnd;
		if (ifStatement.thenStatement != null) {
			sourceStart= fBuffer.indexOf(')', ifStatement.condition.sourceEnd + 1) + 1;
			if (ifStatement.elseStatement != null) {
				sourceEnd= ifStatement.elseStatement.sourceStart - 1;
			} else {
				sourceEnd= nextStart - 1;
			}
			if (fSelection.coveredBy(sourceStart, sourceEnd))
				return sourceStart;
		}
		if (ifStatement.elseStatement != null) {
			sourceStart= fBuffer.indexOfStatementCharacter(ifStatement.thenStatement.sourceEnd + 1) + ELSE_LENGTH;
			sourceEnd= nextStart - 1;
			if (fSelection.coveredBy(sourceStart, sourceEnd))
				return sourceStart;
		}
		return -1;
	}
	
	public boolean visit(InstanceOfExpression instanceOfExpression, BlockScope scope) {
		return visitNode(instanceOfExpression, scope);
	}

	public boolean visit(IntLiteral intLiteral, BlockScope scope) {
		return visitNode(intLiteral, scope);
	}

	public boolean visit(LabeledStatement labeledStatement, BlockScope scope) {
		fLabeledStatements.add(labeledStatement);
		return visitNode(labeledStatement, scope);
	}

	public boolean visit(LongLiteral longLiteral, BlockScope scope) {
		return visitNode(longLiteral, scope);
	}

	public boolean visit(MessageSend messageSend, BlockScope scope) {
		boolean result= visitNode(messageSend, scope);
		fExceptionAnalyzer.visitMessageSend(messageSend, scope, fMode);
		return result;
	}

	public boolean visit(NullLiteral nullLiteral, BlockScope scope) {
		return visitNode(nullLiteral, scope);
	}

	public boolean visit(OR_OR_Expression or_or_Expression, BlockScope scope) {
		return visitBinaryExpression(or_or_Expression, scope, TypeIds.T_boolean);
	}

	public boolean visit(PostfixExpression postfixExpression, BlockScope scope) {
		boolean result= visitNode(postfixExpression, scope);
		fLocalVariableAnalyzer.visitPostfixPrefixExpression(postfixExpression, scope, fMode);
		return result;
	}

	public boolean visit(PrefixExpression prefixExpression, BlockScope scope) {
		boolean result= visitNode(prefixExpression, scope);
		fLocalVariableAnalyzer.visitPostfixPrefixExpression(prefixExpression, scope, fMode);
		return result;
	}

	public boolean visit(QualifiedAllocationExpression qualifiedAllocationExpression, BlockScope scope) {
		return visitNode(qualifiedAllocationExpression, scope);
	}

	public boolean visit(ReturnStatement returnStatement, BlockScope scope) {
		fAdjustedSelectionEnd= fBuffer.indexOfLastCharacterBeforeLineBreak(fCursorPosition);
		boolean result= visitNode(returnStatement, scope);
		if (fMode == StatementAnalyzer.SELECTED) {
			if (fPotentialReturnMessage != null) {
				fStatus.addFatalError(fPotentialReturnMessage);
				fPotentialReturnMessage= null;
			}
			String message= RefactoringCoreMessages.getFormattedString("StatementAnalyzer.return_statement", Integer.toString(getLineNumber(returnStatement))); //$NON-NLS-1$
			if (fParentOfFirstSelectedStatment != getParent()) {
				fStatus.addFatalError(message);
			} else {
				fPotentialReturnMessage= message;
			}
		}
		return result;
	}

	public boolean visit(StringLiteral stringLiteral, BlockScope scope) {
		return visitNode(stringLiteral, scope);
	}

	public boolean visit(SwitchStatement switchStatement, BlockScope scope) {
		// Include "}" into switch statement
		switchStatement.sourceEnd++;
		if (!visitImplicitBranchTarget(switchStatement, scope))
			return false;
		
		if (isPartOfNodeSelected(switchStatement)) {
			int lastEnd= getCaseBodyStart(switchStatement) - 1;
			if (lastEnd < 0) {
				invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.switch_statement")); //$NON-NLS-1$
				return false;
			} else {
				fCursorPosition= lastEnd;
			}
		}
		return true;
	}

	public void endVisit(SwitchStatement switchStatement, BlockScope scope) {
		endVisitImplicitBranchTarget(switchStatement, scope);
	}

	private int getCaseBodyStart(SwitchStatement switchStatement) {
		if (switchStatement.statements == null)
			return -1;
		List cases= getCases(switchStatement);
		for (Iterator iter= cases.iterator(); iter.hasNext(); ) {
			Statement kase= (Statement)iter.next();
			Statement last= (Statement)iter.next();
			int sourceStart= fBuffer.indexOf(':', kase.sourceEnd + 1) + 1;
			int sourceEnd= fBuffer.indexOfStatementCharacter(last.sourceEnd + 1) - 1;
			if (fSelection.coveredBy(sourceStart, sourceEnd))
				return sourceStart;
		}
		return -1;	
	}
	
	private List getCases(SwitchStatement switchStatement) {
		List result= new ArrayList();
		Statement[] statements= switchStatement.statements;
		if (statements == null)
			return result;
		for (int i= 0; i < statements.length; i++) {
			Statement statement= statements[i];
			if (statement instanceof Case || statement instanceof DefaultCase) {
				if (i > 0)
					result.add(statements[i - 1]);
				result.add(statement);	
			}
		}
		result.add(statements[statements.length - 1]);
		return result;
	}

	public boolean visit(SynchronizedStatement synchronizedStatement, BlockScope scope) {
		if (!visitNode(synchronizedStatement, scope))
			return false;
		
		if (isPartOfNodeSelected(synchronizedStatement)) {
			if (fSelection.enclosedBy(synchronizedStatement.block)) {
				fCursorPosition= synchronizedStatement.block.sourceStart;
				return true;
			}
				
			invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.synchronized_statement")); //$NON-NLS-1$
			return false;
		}
		return true;
	}

	public boolean visit(ThrowStatement throwStatement, BlockScope scope) {
		// Begin PR: 1GEUXTX: ITPJCORE:WINNT - Refactoring - invalid exception when extracting throws statement
		if (!visitNode(throwStatement, scope))
			return false;
		fExceptionAnalyzer.visitThrowStatement(throwStatement, scope, fMode);
		return true;
		// End PR
	}

	public boolean visit(TrueLiteral trueLiteral, BlockScope scope) {
		return visitNode(trueLiteral, scope);
	}

	public boolean visit(TryStatement tryStatement, BlockScope scope) {
		// Include "}" into sourceEnd;
		tryStatement.sourceEnd++;
		
		if (!visitNode(tryStatement, scope))
			return false;
			
		fExceptionAnalyzer.visitTryStatement(tryStatement, scope, fMode);
		
		if (isPartOfNodeSelected(tryStatement)) {
			if (fSelection.intersects(tryStatement)) {
				invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.try_statement")); //$NON-NLS-1$
				return false;
			} else {
				int lastEnd;		
				if (fSelection.covers(tryStatement)) {
					lastEnd= tryStatement.sourceEnd;
				} else if (fSelection.enclosedBy(tryStatement.tryBlock)) {
					lastEnd= tryStatement.tryBlock.sourceStart;
				} else if (tryStatement.catchBlocks != null && (lastEnd= getCatchBodyStart(tryStatement.catchBlocks)) >= 0) {
					// do nothing.
				} else if (tryStatement.finallyBlock != null && fSelection.enclosedBy(tryStatement.finallyBlock)) {
					lastEnd=tryStatement.finallyBlock.sourceStart;
				} else {
					lastEnd= tryStatement.sourceEnd;
				}
				fCursorPosition= lastEnd;
			}
		}
		
		return true;
	}

	public void endVisit(TryStatement tryStatement, BlockScope scope) {
		if (tryStatement.catchArguments != null)
			fExceptionAnalyzer.visitCatchArguments(tryStatement.catchArguments, scope, fMode);
		fExceptionAnalyzer.visitEndTryStatement(tryStatement, scope, fMode);
		if (fSelection.covers(tryStatement))
			fNeedsSemicolon= false;
	}
	
	private int getCatchBodyStart(Block[] catchBlocks) {
		for (int i= 0; i < catchBlocks.length; i++) {
			Block catchBlock= catchBlocks[i];
			if (fSelection.enclosedBy(catchBlock))
				return catchBlock.sourceStart;
		}
		return -1;
	}
	
	public boolean visit(UnaryExpression unaryExpression, BlockScope scope) {
		return visitNode(unaryExpression, scope);
	}

	public boolean visit(WhileStatement whileStatement, BlockScope scope) {
		if (!visitImplicitBranchTarget(whileStatement, scope))
			return false;
			
		if (isPartOfNodeSelected(whileStatement)) {
			if (isConditionSelected(whileStatement, whileStatement.condition, whileStatement.sourceStart + WHILE_LENGTH))
				return true;
				
			fCursorPosition= fBuffer.indexOf(')', whileStatement.condition.sourceEnd);
		}
		return true;
	}

	public void endVisit(WhileStatement whileStatement, BlockScope scope) {
		endVisitImplicitBranchTarget(whileStatement, scope);
		endVisitConditionBlock(whileStatement, RefactoringCoreMessages.getString("StatementAnalyzer.a_while")); //$NON-NLS-1$
	}
}