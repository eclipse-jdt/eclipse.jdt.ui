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

import org.eclipse.jdt.core.IBuffer;
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
import org.eclipse.jdt.internal.core.refactoring.TextUtilities;
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
	private IBuffer fBuffer;
	private Selection fSelection;
	
	// internal state.
	private int fMode;
	private int fLastEnd;
	private int fCheckIntersectStart= -1;
	
	private Statement fFirstSelectedStatement;
	private Statement fLastSelectedStatement;
	
	private boolean fIsCompleteStatementRange;
	
	private AbstractMethodDeclaration fEnclosingMethod;
	// private BlockScope fEnclosingScope;
	// private BlockScope fInnerScope;
	
	private RefactoringStatus fStatus= new RefactoringStatus();
	private LocalVariableAnalyzer fLocalVariableAnalyzer;
	private LocalTypeAnalyzer fLocalTypeAnalyzer;
	
	// Handling label and branch statements.
	private Stack fImplicitBranchTargets= new Stack();	
	private List fLabeledStatements= new ArrayList(2);
	 
	public StatementAnalyzer(IBuffer buffer, int start, int length) {
		// System.out.println("Start: " + start + " length: " + length);
		fBuffer= buffer;
		Assert.isTrue(fBuffer != null);
		fSelection= new Selection(start, start + length - 1);
		fLocalVariableAnalyzer= new LocalVariableAnalyzer(this);
		fLocalTypeAnalyzer= new LocalTypeAnalyzer();
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
	
	private void reset() {
		fMode= UNDEFINED;
		fFirstSelectedStatement= null;
		fLastSelectedStatement= null;
		fEnclosingMethod= null;
		fStatus= new RefactoringStatus();
		fIsCompleteStatementRange= false;
		fCheckIntersectStart= -1;
	}
	
	private boolean visitStatement(Statement statement, BlockScope scope) {
		boolean result= true;
		switch(fMode) {
			case UNDEFINED:
				result= false;
				break;			
			case BEFORE:
				if (fLastEnd < fSelection.start) {
					if (fSelection.covers(statement)) {
						startFound(statement);
						fIsCompleteStatementRange= true;
					} else {
						fCheckIntersectStart= -1;
					}
				}
				break;
			case SELECTED:
				/*
				if (fCheckIntersectStart != -1) {
					if (fSelection.intersects(fCheckIntersectStart, statement.sourceStart - 1)) {
						reset();
						fLastEnd= Integer.MAX_VALUE;
					}
				} else 
				*/
				if (fSelection.endsIn(statement)) { // Selection ends in the middle of a statement
					fMode= AFTER;
					fLastSelectedStatement= statement;
					fIsCompleteStatementRange= false;
				} else if (statement.sourceEnd > fSelection.end) {
					fMode= AFTER;
				} else {
					fLastSelectedStatement= statement;
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
	
	private boolean visitAbstractMethodDeclaration(AbstractMethodDeclaration node, Scope scope) {
		// node.bodyStart is the first character after the {
		// node.bodyEnd is the last character before the }
		HackFinder.fixMeSoon("1GCSJPZ: ITPJCORE:WIN2000 - AbstractMethodDeclaration: bodyStart body End inconsistent");
		node.bodyEnd= node.bodyEnd - 1;
		boolean result= fSelection.enclosedBy(node);
		if (result) {
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
		int realEnd= TextUtilities.indexOfNextStatementCharacter(fBuffer, node.sourceEnd + 1) - 1;
		if (realEnd < 0 || fSelection.intersects(node.sourceStart, realEnd)) {
			reset();
			fLastEnd= Integer.MAX_VALUE;
		}
		fLastEnd= node.sourceEnd;
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
		
		if (result && fSelection.end >= block.sourceStart) {
			fLastEnd= block.sourceStart;
		}
		
		return result;
	}

	public void endVisit(Block block, BlockScope scope) {
		if (fSelection.intersects(block)) {
			reset();
			fLastEnd= Integer.MAX_VALUE;
		} else {
			fLastEnd= block.sourceEnd;
		}
	}

	public boolean visit(Break breakStatement, BlockScope scope) {
		HackFinder.fixMeSoon("1GCU7OH: ITPJCORE:WIN2000 - Break.sourceEnd contains tailing comments");
		if (breakStatement.label == null) {
			breakStatement.sourceEnd= breakStatement.sourceStart + "break".length() - 1;
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
			continueStatement.sourceEnd= continueStatement.sourceStart + "continue".length() - 1;
		}
		return visitBranchStatement(continueStatement, scope, "continue");
	}

	public boolean visit(DefaultCase defaultCaseStatement, BlockScope scope) {
		return visitStatement(defaultCaseStatement, scope);
	}

	public boolean visit(DoStatement doStatement, BlockScope scope) {
		boolean result= visitImplicitBranchTarget(doStatement, scope);
		if (result && fSelection.end >= doStatement.sourceStart) {
			// skip the do.
			fLastEnd= doStatement.sourceStart + 1;
		}
		return result;
	}
	
	public void endVisit(DoStatement doStatement, BlockScope scope) {
		endVisitImplicitBranchTarget(doStatement, scope);
		endVisitCompoundStatement(doStatement, scope);
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
			fLastEnd= TextUtilities.indexOf(fBuffer, start + 1, ')');
		}
		return result;
	}

	public void endVisit(ForStatement forStatement, BlockScope scope) {
		endVisitImplicitBranchTarget(forStatement, scope);
		endVisitCompoundStatement(forStatement, scope);
	}
	
	public boolean visit(IfStatement ifStatement, BlockScope scope) {
		return visitStatement(ifStatement, scope);
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
		return visitStatement(messageSend, scope);
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
		return visitImplicitBranchTarget(switchStatement, scope);
	}

	public void endVisit(SwitchStatement switchStatement, BlockScope scope) {
		endVisitImplicitBranchTarget(switchStatement, scope);
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
		return visitStatement(tryStatement, scope);
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
			fLastEnd= TextUtilities.indexOf(fBuffer, start, ')');
		}
		return result;
	}

	public void endVisit(WhileStatement whileStatement, BlockScope scope) {
		endVisitImplicitBranchTarget(whileStatement, scope);
		endVisitCompoundStatement(whileStatement, scope);
	}
}