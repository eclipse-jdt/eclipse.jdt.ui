/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000, 2001
 */
package org.eclipse.jdt.core.refactoring.code;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.internal.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.util.CharOperation;
import org.eclipse.jdt.internal.core.refactoring.ASTEndVisitAdapter;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.ExtendedBuffer;
import org.eclipse.jdt.internal.core.util.HackFinder;

/**
 * Checks whether the source range denoted by <code>start</code> and <code>end</code>
 * selects a set of statements.
 */
/* package */ class StatementAnalyzer extends ASTEndVisitAdapter {
	
	static final int UNDEFINED=   0;
	static final int BEFORE=	1;
	static final int SELECTED=	2;
	static final int AFTER=	      3;
	
	// The selection's start and end position
	private ExtendedBuffer fBuffer;
	private Selection fSelection;
	
	// internal state.
	private int fMode;
	private int fLastEnd;
	// private int fCheckIntersectStart= -1;
	
	private Statement fFirstSelectedStatement;
	private Statement fLastSelectedStatement;
	private boolean fNeedsSemicolon;
	
	private boolean fIsCompleteStatementRange;
	
	private AbstractMethodDeclaration fEnclosingMethod;
	
	private RefactoringStatus fStatus= new RefactoringStatus();
	private LocalVariableAnalyzer fLocalVariableAnalyzer;
	private LocalTypeAnalyzer fLocalTypeAnalyzer;
	private ExceptionAnalyzer fExceptionAnalyzer;
	
	// Handling label and branch statements.
	private Stack fImplicitBranchTargets= new Stack();	
	private List fLabeledStatements= new ArrayList(2);
	
	private static final int BREAK_LENGTH= "break".length();
	private static final int CONTINUE_LENGTH= "continue".length();
	private static final int DO_LENGTH=    "do".length();
	private static final int ELSE_LENGTH=  "else".length();
	 
	public StatementAnalyzer(ExtendedBuffer buffer, int start, int length) {
		// System.out.println("Start: " + start + " length: " + length);
		fBuffer= buffer;
		Assert.isTrue(fBuffer != null);
		fSelection= new Selection(start, length);
		fLocalVariableAnalyzer= new LocalVariableAnalyzer(this);
		fLocalTypeAnalyzer= new LocalTypeAnalyzer();
		fExceptionAnalyzer= new ExceptionAnalyzer();
	}

	/**
	 * Checks if the refactoring can be activated.
	 */
	public void checkActivation(RefactoringStatus status) {
		if (fEnclosingMethod == null || fLastSelectedStatement == null) {
			status.addFatalError("TextSelection doesn't mark a text range that can be extracted");
		} else {
			if (!fIsCompleteStatementRange)
				status.addError("TextSelection doesn't completely cover a set of statements");
		}
		status.merge(fStatus);
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
		return fLastSelectedStatement.sourceEnd;
	}
	
	public String getSignature(String methodName) {
		return fLocalVariableAnalyzer.getCallSignature(methodName)
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
	
	private void reset() {
		fMode= UNDEFINED;
		fFirstSelectedStatement= null;
		fLastSelectedStatement= null;
		fEnclosingMethod= null;
		fStatus= new RefactoringStatus();
		fIsCompleteStatementRange= false;
		fNeedsSemicolon= true;
	}
	
	private boolean visitStatement(Statement statement, BlockScope scope) {
		boolean result= true;
		switch(fMode) {
			case UNDEFINED:
				return false;			
			case BEFORE:
				if (fLastEnd < fSelection.start && fSelection.covers(statement)) {
					startFound(statement);
					fIsCompleteStatementRange= true;
				}
				break;
			case SELECTED:
				if (fSelection.endsIn(statement)) { // Selection ends in the middle of a statement
					fMode= AFTER;
					fLastSelectedStatement= statement;
					fIsCompleteStatementRange= false;
				} else if (statement.sourceEnd > fSelection.end) {
					fMode= AFTER;
				} else {
					fLastSelectedStatement= statement;
					fNeedsSemicolon= true;
				}
				break;
			case AFTER:
				break;
		}
		trackLastEnd(statement);
		return result;
	}
	
	private void startFound(Statement statement) {
		fMode= SELECTED;
		fFirstSelectedStatement= statement;
		fLastSelectedStatement= statement;
	}
	
	private void trackLastEnd(Statement statement) {
		if (statement.sourceEnd > fLastEnd)
			fLastEnd= statement.sourceEnd;
	}
	
	private void trackLastEnd(int end) {
		if (end > fLastEnd)
			fLastEnd= end;
	}
	
	private boolean visitAbstractMethodDeclaration(AbstractMethodDeclaration node, Scope scope) {
		boolean result= fSelection.enclosedBy(node);
		if (result) {
			fExceptionAnalyzer.visitAbstractMethodDeclaration(node, scope);
			reset();
			fEnclosingMethod= node;
			fMode= BEFORE;
		}
		return result;	
	}
	
	private boolean visitLocalTypeDeclaration(TypeDeclaration declaration, BlockScope scope) {
		boolean result= visitStatement(declaration, scope);
		fLocalTypeAnalyzer.visitLocalTypeDeclaration(declaration, scope, fMode);
		return result;
	}
	
	private boolean visitTypeReference(TypeReference reference, BlockScope scope) {
		fLocalTypeAnalyzer.visitTypeReference(reference, scope, fMode);
		return false;
	}
	
	private boolean visitImplicitBranchTarget(Statement statement, BlockScope scope) {
		fImplicitBranchTargets.push(statement);
		return visitStatement(statement, scope);
	}
	
	private void endVisitImplicitBranchTarget(Statement statement, BlockScope scope) {
		fImplicitBranchTargets.pop();
	}
	
	private boolean visitBranchStatement(BranchStatement statement, BlockScope scope, String name) {
		boolean result= visitStatement(statement, scope);
		Statement target= findTarget(statement);
		String label= "label"; //* new String(statement.label);
		if (target != null) {
			if (isSelected(target)) {
				if (fMode != SELECTED)
					fStatus.addError("Selected block contains a " + name + " target but not all corresponding " + name + " statements are selected");
			} else {
				if (fMode == SELECTED)
					fStatus.addError("Selected block contains a " + name + " statement but the corresponding " + name + " target isn't selected");
			}
		} else {
			fStatus.addFatalError("Can not find break target");
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
	
	private void endVisitCompoundStatement(AstNode node, Scope scope) {
		int realEnd= fBuffer.indexOfStatementCharacter(node.sourceEnd + 1) - 1;
		if (fSelection.covers(node) || fSelection.coveredBy(node.sourceStart, realEnd)) {
			fLastEnd= node.sourceEnd;
		} else {
			reset();
			fLastEnd= Integer.MAX_VALUE;
		}
	}
	
	//---- Problem management -----------------------------------------------------
	
	public void acceptProblem(IProblem problem) {
	}
	
	//---- Compilation Unit -------------------------------------------------------
	
	public boolean visit(CompilationUnitDeclaration compilationUnitDeclaration, CompilationUnitScope scope) {
		return fSelection.enclosedBy(compilationUnitDeclaration);
	}
	
	public boolean visit(ImportReference importRef, CompilationUnitScope scope) {
		return false;
	}
	
	public boolean visit(TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
		return fSelection.enclosedBy(typeDeclaration);
	}
	
	//---- Type -------------------------------------------------------------------
	
	public boolean visit(Clinit clinit, ClassScope scope) {
		return fSelection.enclosedBy(clinit);
	}
	
	public boolean visit(TypeDeclaration typeDeclaration, ClassScope scope) {
		return fSelection.enclosedBy(typeDeclaration);
	}
	
	public boolean visit(MemberTypeDeclaration memberTypeDeclaration, ClassScope scope) {
		return fSelection.enclosedBy(memberTypeDeclaration);
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
	
	public boolean visit(MethodDeclaration methodDeclaration, ClassScope scope) {
		return visitAbstractMethodDeclaration(methodDeclaration, scope);
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
		return visitStatement(anonymousTypeDeclaration, scope);
	}
	
	public boolean visit(LocalDeclaration localDeclaration, BlockScope scope) {
		return visitStatement(localDeclaration, scope);
	}

	public boolean visit(FieldReference fieldReference, BlockScope scope) {
		return false;
	}

	public boolean visit(ArrayReference arrayReference, BlockScope scope) {
		return false;
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
		boolean result= visitStatement(qualifiedNameReference, scope);
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
		boolean result= visitStatement(singleNameReference, scope);
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
		return visitStatement(allocationExpression, scope);
	}
	public boolean visit(AND_AND_Expression and_and_Expression, BlockScope scope) {
		return visitStatement(and_and_Expression, scope);
	}
	
	public boolean visit(ArrayAllocationExpression arrayAllocationExpression, BlockScope scope) {
		return visitStatement(arrayAllocationExpression, scope);
	}
	
	public boolean visit(ArrayInitializer arrayInitializer, BlockScope scope) {
		return visitStatement(arrayInitializer, scope);
	}
	
	public boolean visit(Assignment assignment, BlockScope scope) {
		boolean result= visitStatement(assignment, scope);
		fLocalVariableAnalyzer.visitAssignment(assignment, scope, fMode);
		return result;
	}

	public boolean visit(BinaryExpression binaryExpression, BlockScope scope) {
		return visitStatement(binaryExpression, scope);
	}

	public boolean visit(Block block, BlockScope scope) {
		boolean result= visitStatement(block, scope);
		
		if (fSelection.intersects(block)) {
			reset();
			fLastEnd= Integer.MAX_VALUE;
		} else {
			fLastEnd= block.sourceStart;
		}
		
		return result;
	}

	public void endVisit(Block block, BlockScope scope) {
		trackLastEnd(block);
		if (fSelection.covers(block))
			fNeedsSemicolon= false;
	}

	public boolean visit(Break breakStatement, BlockScope scope) {
		HackFinder.fixMeSoon("1GCU7OH: ITPJCORE:WIN2000 - Break.sourceEnd contains tailing comments");
		if (breakStatement.label == null) {
			breakStatement.sourceEnd= breakStatement.sourceStart + BREAK_LENGTH - 1;
		}
		return visitBranchStatement(breakStatement, scope, "break");
	}

	public boolean visit(Case caseStatement, BlockScope scope) {
		return visitStatement(caseStatement, scope);
	}

	public boolean visit(CastExpression castExpression, BlockScope scope) {
		return visitStatement(castExpression, scope);
	}

	public boolean visit(CharLiteral charLiteral, BlockScope scope) {
		return visitStatement(charLiteral, scope);
	}

	public boolean visit(ClassLiteralAccess classLiteral, BlockScope scope) {
		return visitStatement(classLiteral, scope);
	}

	public boolean visit(CompoundAssignment compoundAssignment, BlockScope scope) {
		boolean result= visitStatement(compoundAssignment, scope);
		fLocalVariableAnalyzer.visitAssignment(compoundAssignment, scope, fMode);
		return result;
	}

	public boolean visit(ConditionalExpression conditionalExpression, BlockScope scope) {
		return visitStatement(conditionalExpression, scope);
	}

	public boolean visit(Continue continueStatement, BlockScope scope) {
		HackFinder.fixMeSoon("1GCU7OH: ITPJCORE:WIN2000 - Break.sourceEnd contains tailing comments");
		if (continueStatement.label == null) {
			continueStatement.sourceEnd= continueStatement.sourceStart + CONTINUE_LENGTH - 1;
		}
		return visitBranchStatement(continueStatement, scope, "continue");
	}

	public boolean visit(DefaultCase defaultCaseStatement, BlockScope scope) {
		return visitStatement(defaultCaseStatement, scope);
	}

	public boolean visit(DoStatement doStatement, BlockScope scope) {
		// skip the string "do"
		int actionStart= doStatement.sourceStart + DO_LENGTH;		
		int actionEnd= fBuffer.indexOfStatementCharacter(doStatement.action.sourceEnd + 1);
		// Either the selection covers the whole do statement or the section lies
		// inside the do statement's action.	
		if (fSelection.start != actionStart && // Selection doesn't start right after do. This avoid dofoo();
		    (fSelection.covers(doStatement) || fSelection.coveredBy(actionStart, actionEnd))) {
			boolean result= visitImplicitBranchTarget(doStatement, scope);
			if (result && fSelection.end >= doStatement.sourceStart) {
				// skip the do.
				fLastEnd= actionStart;
			}
			return result;
		} else {
			// Push it anyway since we pop it during end visit.
			fImplicitBranchTargets.push(doStatement);
			reset();
			fLastEnd= Integer.MAX_VALUE;
			return false;
		}
	}
	
	public void endVisit(DoStatement doStatement, BlockScope scope) {
		endVisitImplicitBranchTarget(doStatement, scope);
		fLastEnd= doStatement.sourceEnd;
	}

	public boolean visit(DoubleLiteral doubleLiteral, BlockScope scope) {
		return visitStatement(doubleLiteral, scope);
	}

	public boolean visit(EqualExpression equalExpression, BlockScope scope) {
		return visitStatement(equalExpression, scope);
	}

	public boolean visit(ExplicitConstructorCall explicitConstructor, BlockScope scope) {
		return visitStatement(explicitConstructor, scope);
	}

	public boolean visit(ExtendedStringLiteral extendedStringLiteral, BlockScope scope) {
		return visitStatement(extendedStringLiteral, scope);
	}

	public boolean visit(FalseLiteral falseLiteral, BlockScope scope) {
		return visitStatement(falseLiteral, scope);
	}

	public boolean visit(FloatLiteral floatLiteral, BlockScope scope) {
		return visitStatement(floatLiteral, scope);
	}

	public boolean visit(ForStatement forStatement, BlockScope scope) {
		boolean result= visitImplicitBranchTarget(forStatement, scope);
		// forStatement.sourceEnd includes the statement's action. Since the
		// selection can be the statements body adjust last end if so.
		if (result && fSelection.end >= forStatement.sourceStart) {
			int start= forStatement.sourceStart;
			if (forStatement.increments != null) {
				start= forStatement.increments[forStatement.increments.length - 1].sourceEnd;
			} else if (forStatement.condition != null) {
				start= forStatement.condition.sourceEnd;
			} else if (forStatement.initializations != null) {
				start= forStatement.initializations[forStatement.initializations.length - 1].sourceEnd;
			}
			fLastEnd= fBuffer.indexOf(')', start + 1);
		}
		return result;
	}

	public void endVisit(ForStatement forStatement, BlockScope scope) {
		endVisitImplicitBranchTarget(forStatement, scope);
		endVisitCompoundStatement(forStatement, scope);
	}
	
	public boolean visit(IfStatement ifStatement, BlockScope scope) {
		int lastEnd= ifStatement.sourceEnd;
		boolean result= false;
		if (fSelection.covers(ifStatement) || (lastEnd= getIfElseBodyStart(ifStatement) - 1) >= 0) {
			result= visitStatement(ifStatement, scope);
			fLastEnd= lastEnd;
			
		} else {
			reset();
			fLastEnd= Integer.MAX_VALUE;
		}
		return result;
	}

	private int getIfElseBodyStart(IfStatement ifStatement) {
		int sourceStart;
		int sourceEnd;
		if (ifStatement.thenStatement != null) {
			sourceStart= fBuffer.indexOf(')', ifStatement.condition.sourceEnd + 1) + 1;
			if (ifStatement.elseStatement != null) {
				sourceEnd= ifStatement.elseStatement.sourceStart - 1;
			} else {
				sourceEnd= fBuffer.indexOfStatementCharacter(ifStatement.sourceEnd + 1) - 1;
			}
			if (fSelection.coveredBy(sourceStart, sourceEnd))
				return sourceStart;
		} else if (ifStatement.elseStatement != null) {
			sourceStart= fBuffer.indexOfStatementCharacter(ifStatement.thenStatement.sourceEnd + 1) + ELSE_LENGTH;
			sourceEnd= fBuffer.indexOfStatementCharacter(ifStatement.sourceEnd + 1) - 1;
			if (fSelection.coveredBy(sourceStart, sourceEnd))
				return sourceStart;
		}
		return -1;
	}
	
	public boolean visit(InstanceOfExpression instanceOfExpression, BlockScope scope) {
		return visitStatement(instanceOfExpression, scope);
	}

	public boolean visit(IntLiteral intLiteral, BlockScope scope) {
		return visitStatement(intLiteral, scope);
	}

	public boolean visit(LabeledStatement labeledStatement, BlockScope scope) {
		fLabeledStatements.add(labeledStatement);
		return visitStatement(labeledStatement, scope);
	}

	public boolean visit(LongLiteral longLiteral, BlockScope scope) {
		return visitStatement(longLiteral, scope);
	}

	public boolean visit(MessageSend messageSend, BlockScope scope) {
		boolean result= visitStatement(messageSend, scope);
		fExceptionAnalyzer.visitMessageSend(messageSend, scope, fMode);
		return result;
	}

	public boolean visit(NullLiteral nullLiteral, BlockScope scope) {
		return visitStatement(nullLiteral, scope);
	}

	public boolean visit(OR_OR_Expression or_or_Expression, BlockScope scope) {
		return visitStatement(or_or_Expression, scope);
	}

	public boolean visit(PostfixExpression postfixExpression, BlockScope scope) {
		boolean result= visitStatement(postfixExpression, scope);
		fLocalVariableAnalyzer.visitPostfixPrefixExpression(postfixExpression, scope, fMode);
		return result;
	}

	public boolean visit(PrefixExpression prefixExpression, BlockScope scope) {
		boolean result= visitStatement(prefixExpression, scope);
		fLocalVariableAnalyzer.visitPostfixPrefixExpression(prefixExpression, scope, fMode);
		return result;
	}

	public boolean visit(QualifiedAllocationExpression qualifiedAllocationExpression, BlockScope scope) {
		return visitStatement(qualifiedAllocationExpression, scope);
	}

	public boolean visit(ReturnStatement returnStatement, BlockScope scope) {
		boolean result= visitStatement(returnStatement, scope);
		if (fMode == SELECTED)
			fStatus.addFatalError("Selected block contains a return statement");
		return result;
	}

	public boolean visit(StringLiteral stringLiteral, BlockScope scope) {
		return visitStatement(stringLiteral, scope);
	}

	public boolean visit(SwitchStatement switchStatement, BlockScope scope) {
		// Include "}" into switch statement
		switchStatement.sourceEnd++;
		int lastEnd= switchStatement.sourceEnd;
		boolean result= false;
		if (fSelection.covers(switchStatement) || (lastEnd= getCaseBodyStart(switchStatement) - 1) >= 0) {
			result= visitImplicitBranchTarget(switchStatement, scope);
			fLastEnd= lastEnd;
			
		} else {
			reset();
			fLastEnd= Integer.MAX_VALUE;
			fImplicitBranchTargets.push(switchStatement);
		}
		return result;
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
		return visitStatement(synchronizedStatement, scope);
	}

	public boolean visit(ThrowStatement throwStatement, BlockScope scope) {
		return visitStatement(throwStatement, scope);
	}

	public boolean visit(TrueLiteral trueLiteral, BlockScope scope) {
		return visitStatement(trueLiteral, scope);
	}

	public boolean visit(TryStatement tryStatement, BlockScope scope) {
		// Include "}" into sourceEnd;
		tryStatement.sourceEnd++;
		
		boolean result= visitStatement(tryStatement, scope);
		fExceptionAnalyzer.visitTryStatement(tryStatement, scope, fMode);
		
		if (fSelection.intersects(tryStatement)) {
			reset();
			fLastEnd= Integer.MAX_VALUE;
			result= false;
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
			fLastEnd= lastEnd;
		}
		return result;
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
		return visitStatement(unaryExpression, scope);
	}

	public boolean visit(WhileStatement whileStatement, BlockScope scope) {
		boolean result= visitImplicitBranchTarget(whileStatement, scope);
		if (result && fSelection.end >= whileStatement.sourceStart) {
			int start= whileStatement.sourceStart;
			if (whileStatement.condition != null) {
				start= whileStatement.condition.sourceEnd;
			}
			fLastEnd= fBuffer.indexOf(')', start);
		}
		return result;
	}

	public void endVisit(WhileStatement whileStatement, BlockScope scope) {
		endVisitImplicitBranchTarget(whileStatement, scope);
		endVisitCompoundStatement(whileStatement, scope);
	}
}