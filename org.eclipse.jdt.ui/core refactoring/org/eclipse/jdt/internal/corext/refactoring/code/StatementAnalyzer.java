/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.internal.compiler.IAbstractSyntaxTreeVisitor;
import org.eclipse.jdt.internal.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.jdt.internal.compiler.lookup.BaseTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.BaseTypes;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.problem.ProblemHandler;
import org.eclipse.jdt.internal.compiler.util.CharOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.AstNodeData;
import org.eclipse.jdt.internal.corext.refactoring.ExtendedBuffer;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.util.ASTUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.IParentTracker;
import org.eclipse.jdt.internal.corext.refactoring.util.Selection;

/**
 * Checks whether the source range denoted by <code>start</code> and <code>end</code>
 * selects a set of statements.
 */
public class StatementAnalyzer implements IAbstractSyntaxTreeVisitor {
	
	public static final int UNDEFINED=   0;
	static final Integer UNDEFINDED_OBJECT= new Integer(UNDEFINED);
	public static final int BEFORE=	1;
	static final Integer BEFORE_OBJECT= new Integer(BEFORE);
	public static final int SELECTED=	2;
	static final Integer SELECTED_OBJECT= new Integer(SELECTED);
	public static final int AFTER=	      3;
	static final Integer AFTER_OBJECT= new Integer(AFTER);
	
	// Parent tracking interface
	private IParentTracker fParentTracker;
	
	// Attaching additional data to a node.
	private AstNodeData fNodeData= new AstNodeData();
	
	// The buffer containing the source code.
	private ExtendedBuffer fBuffer;
	
	// Selection state.
	protected Selection fSelection;
	private int fMode;
	private int fCursorPosition;

	// Error handling	
	private boolean fCompileErrorFound;
	private RefactoringStatus fStatus= new RefactoringStatus();
	private int[] fLineSeparatorPositions;	

	// The method that encloses the selection.	
	private Scope fEnclosingScope;
	private AbstractMethodDeclaration fEnclosingMethod;
	private Stack fMethodStack= new Stack();
	
	// Selected nodes
	protected List fTopNodes;
	private AstNode fParentOfFirstSelectedNode;
	private AstNode fFirstSelectedNode;
	private boolean fNeedsSemicolon;
	
	// Helper-Analyzer
	protected LocalTypeAnalyzer fLocalTypeAnalyzer;
	protected ExceptionAnalyzer fExceptionAnalyzer;
	
	// Type binding of the selected expression
	protected TypeBinding fExpressionTypeBinding;
	protected boolean fExpressionIsPartOfOperator;
	protected boolean fExpressionTypeIsVisible;
	
	// Handling label and branch statements.
	private Stack fImplicitBranchTargets= new Stack();
	private List fLabeledStatements= new ArrayList(2);
		
	private static final int BREAK_LENGTH= "break".length(); //$NON-NLS-1$
	private static final int CONTINUE_LENGTH= "continue".length(); //$NON-NLS-1$
	 
	public StatementAnalyzer(ExtendedBuffer buffer, int start, int length, boolean asymetricAssignment, ImportEdit edit) {
		fBuffer= buffer;
		Assert.isTrue(fBuffer != null);
		fSelection= Selection.createFromStartLength(start, length);
		fLocalTypeAnalyzer= new LocalTypeAnalyzer();
		fExceptionAnalyzer= new ExceptionAnalyzer(this, edit);
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
		checkSelection();
		checkExpression();
		if ((fEnclosingMethod == null || fTopNodes == null || fTopNodes.size() == 0) && !fStatus.hasFatalError()) {
			fStatus.addFatalError(RefactoringCoreMessages.getString("StatementAnalyzer.only_method_body")); //$NON-NLS-1$
		}
		status.merge(fStatus);
		if (!status.hasFatalError()) {
			fLocalTypeAnalyzer.checkActivation(status);
		}
	}
	
	private void checkSelection() {
		// We don't have a selected node;
		if (fTopNodes == null || fTopNodes.isEmpty())
			return;
		int pos= fBuffer.indexOfStatementCharacter(fSelection.start);
		AstNode node= (AstNode)fTopNodes.get(0);
		if (ASTUtil.getSourceStart(node) != pos) {
			invalidSelection("Beginning of selection contains characters that do not belong to the selected statement(s).");
			return;
		}	
		
		for (int i= 0; i < fTopNodes.size() - 1; i++) {
			AstNode first= (AstNode)fTopNodes.get(i);
			AstNode second= (AstNode)fTopNodes.get(i + 1);
			pos= fBuffer.indexOfStatementCharacter(ASTUtil.getSourceEnd(first) + 1);
			if (pos != ASTUtil.getSourceStart(second)) {
				invalidSelection("Selected statements do not belong to the same category. For example, a while statement's expression and action are selected.");
				return;
			}
		}
		node= getLastSelectedNode();	
		pos= fBuffer.indexOfStatementCharacter(ASTUtil.getSourceEnd(node) + 1);
		if (pos != -1 && pos <= fSelection.end)
			invalidSelection("End of selection contains characters that do not belong to the selected statement(s).");
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
	 * Returns the scope containing the selected top level nodes.
	 * 
	 * @return the scope covering the selected top level nodes.
	 */
	public Scope getEnclosingScope() {
		return fEnclosingScope;
	}
	
	/**
	 * Checks, if the current method visited by this visitor is the method enclosing the
	 * selection. If this is the case, the method returns <code>true</code>, otherwise 
	 * <code>false</code> is returned.
	 * 
	 * @return <code>true<code> if <code>fMethodStack.peek() == getEnclosingMethod</code> 
	 *  otherwise <code>false</code>.
	 */
	public boolean processesEnclosingMethod() {
		if (fMethodStack.isEmpty())
			return false;
		return getEnclosingMethod() == fMethodStack.peek();
	}
	
	/**
	 * Returns <code>true</code> if the given AST node is selected in the text editor.
	 * Otherwise <code>false</code> is returned.
	 * 
	 * @return whether or not the given AST node is selected in the editor.
	 */
	public boolean isSelected(AstNode node) {
		return fSelection.covers(node);
	}
	
	/**
	 * Returns the last selected node.
	 */
	public AstNode getLastSelectedNode() {
		if (fTopNodes == null || fTopNodes.size() == 0)
			return null;
			
		return (AstNode)fTopNodes.get(fTopNodes.size() - 1);
	}
	
	/**
	 * Returns the end position of the last selected node. Returns -1 if there
	 * is any selected node.
	 * 
	 * @return the end position of the last selected node or -1.
	 */
	public int getEndOfLastSelectedNode() {
		AstNode node= getLastSelectedNode();
		if (node == null)
			return -1;
		return node.sourceEnd;
	}
	
	/**
	 * Returns <code>true</code> if the selection covers a single expression. Otherwise
	 * <code>false</code> is returned.
	 * 
	 * @return <code>true</code> if selection covers expression; otherwise <code>false</code>.
	 */
	public boolean isExpressionSelected() {
		if (fTopNodes == null || fTopNodes.size() != 1)
			return false;
		return (fTopNodes.get(0) instanceof Expression);
	}
	
	/**
	 * Returns true if the last selected statement needs a semicolon to have correct
	 * syntax. For example a block or a try / catch / finnally doesn't need a semicolon
	 * at the end.
	 */
	public boolean getNeedsSemicolon() {
		return fNeedsSemicolon;
	}
	
	public boolean returnsExpressionValue() {
		return (fExpressionTypeBinding != null && fExpressionTypeBinding != BaseTypes.VoidBinding);
	}
	
	public TypeBinding getExpressionTypeBinding() {
		return fExpressionTypeBinding;
	}
	
	public boolean isExpressionTypeVisible() {
		return fExpressionTypeIsVisible;
	}
	
	//--- general helpers ---------------------------------------------------------
	
	private boolean contains(Statement[] statements, AstNode node) {
		if (statements == null)
			return false;
		for (int i= 0; i < statements.length; i++) {
			if (statements[i] == node)
				return true;
		}
		return false;
	}

	//--- node management ---------------------------------------------------------
	
	private void reset() {
		fMode= UNDEFINED;
		fTopNodes= null;
		fParentOfFirstSelectedNode= null;
		fFirstSelectedNode= null;
		fEnclosingMethod= null;
		fNeedsSemicolon= true;
	}
	
	private void invalidSelection(String message) {
		reset();
		fCursorPosition= Integer.MAX_VALUE;
		fStatus.addFatalError(message);
	}
	
	private boolean visitNode(AstNode node, Scope scope) {
		return visitRange(node.sourceStart, node.sourceEnd, node, scope);
	}
	
	private boolean visitRange(int start, int end, AstNode node, Scope scope) {
		boolean result= true;
		switch(fMode) {
			case UNDEFINED:
				result= false;			
			case BEFORE:
				if (fCursorPosition < fSelection.start && fSelection.covers(start, end)) {
					startFound(node, scope);
				}
				break;
			case SELECTED:
				if (fSelection.endsIn(start, end)) { // Selection ends in the middle of a statement
					invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.ends_middle_of_statement")); //$NON-NLS-1$
					result= false;
					break;
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
		return result;
	}
	
	private void startFound(AstNode node, Scope scope) {
		fMode= SELECTED;
		fEnclosingScope= scope;
		fParentOfFirstSelectedNode= getParent();
		fFirstSelectedNode= node;

		fTopNodes= new ArrayList(5);
		fTopNodes.add(node);
	}
	
	private void trackLastSelectedNode(AstNode node) {
		checkParent();
		if (fParentOfFirstSelectedNode == getParent()) {
			fTopNodes.add(node);
		}
	}
	
	private void checkParent() {
		List parents= fParentTracker.getParents();
		for (int i= parents.size() - 1; i >= 0; i--) {
			AstNode node= (AstNode)parents.get(i);
			if (node == fParentOfFirstSelectedNode)
				return;
		}
		invalidSelection("Not all selected top level statements belong to the same parent.");
	}
	
	private void trackCursorPosition(int pos) {
		if (pos > fCursorPosition)
			fCursorPosition= pos;
	}
	
	private void endVisitNode(AstNode node, Scope scope) {
		endVisitRange(node.sourceStart, node.sourceEnd, node, scope);
	}
	
	private void endVisitRange(int start, int end, AstNode node, Scope scope) {
		// Make sure that in cases where the last statement is selected in a compound statement
		// the compound statement doesn't have mode == SELECTED when in endVisit.
		if (fMode == SELECTED && start < fSelection.start && fSelection.end < end)
				fMode= AFTER;
		trackCursorPosition(end);
	}
	
	//---- General visit* methods --------------------------------------------------
	
	private boolean visitAssignment(Assignment assignment, BlockScope scope) {
		if (!visitNode(assignment, scope))
			return false;
		rememberVisitMode(assignment);
		trackExpressionTypeBinding(assignment, null, scope);
		return true;
	}
	
	private void endVisitAssignment(Assignment assignment, BlockScope scope, boolean compound) {
		endVisitNode(assignment, scope);
	}
	
	private void rememberVisitMode(AstNode node) {
		fNodeData.put(node, getModeObject());
	}
	
	private Object getModeObject() {
		switch (fMode) {
			case UNDEFINED:
				return UNDEFINDED_OBJECT;
			case BEFORE:
				return BEFORE_OBJECT;
			case SELECTED:
				return SELECTED_OBJECT;
			case AFTER:
				return AFTER_OBJECT;
			default:
				Assert.isTrue(false, "Should never happen");
				return null;
		}
	}
	
	private int getVisitMode(AstNode node) {
		Object o= fNodeData.remove(node);
		if (!(o instanceof Integer))
			return UNDEFINED;
		return ((Integer)o).intValue();
	}
	
	private boolean visitAbstractMethodDeclaration(AbstractMethodDeclaration node, Scope scope) {
		
		fMethodStack.push(node);
		
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
		Assert.isTrue(node == fMethodStack.pop());
		endVisitRange(node.declarationSourceStart, node.declarationSourceEnd, node, scope);
	}
	
	private boolean visitLocalTypeDeclaration(TypeDeclaration declaration, BlockScope scope) {
		if (!checkLocalTypeDeclaration(declaration))
			return false;
		
		boolean result= visitRange(declaration.declarationSourceStart, declaration.declarationSourceEnd,
			declaration, scope);
		fLocalTypeAnalyzer.visitLocalTypeDeclaration(declaration, scope, fMode);
		return result;
	}
	
	private boolean handleTypeReference(TypeReference reference, BlockScope scope) {
		fLocalTypeAnalyzer.visitTypeReference(reference, scope, fMode);
		return false;
	}
	
	private boolean visitImplicitBranchTarget(Statement statement, BlockScope scope) {
		fImplicitBranchTargets.push(statement);
		return visitNode(statement, scope);
	}
	
	private void endVisitImplicitBranchTarget(Statement statement, BlockScope scope) {
		fImplicitBranchTargets.pop();
		endVisitNode(statement, scope);
	}
	
	private boolean visitBranchStatement(BranchStatement statement, BlockScope scope, String name) {
		boolean result= visitNode(statement, scope);
		Statement target= findTarget(statement);
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
	
	private boolean checkLocalTypeDeclaration(TypeDeclaration declaration) {
		if (fSelection.intersects(declaration.declarationSourceStart, declaration.declarationSourceEnd)) {
			invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.middle_of_type_declaration")); //$NON-NLS-1$
			return false;
		}
		return true;
	}	
	
	//--- Expression / Condition handling -----------------------------------------
	
	private boolean visitLiteral(Literal literal, BlockScope scope) {
		if (!visitNode(literal, scope))
			return false;
		trackExpressionTypeBinding(literal, literal.literalType(scope), scope);
		return true;
	}
	
	private boolean visitBinaryExpression(BinaryExpression binaryExpression, BlockScope scope) {
		if (!visitNode(binaryExpression, scope))
			return false;
		trackExpressionTypeBinding(binaryExpression, getTypeBinding(binaryExpression, scope), scope);
		return true;	
	}
	
	private TypeBinding getTypeBinding(Statement statement, BlockScope scope) {
		int returnType= statement.bits & statement.ReturnTypeIDMASK;
		switch (returnType) {
			case TypeIds.T_boolean :
				return BaseTypes.BooleanBinding;
			case TypeIds.T_byte :
				return BaseTypes.ByteBinding;
			case TypeIds.T_char :
				return BaseTypes.CharBinding;
			case TypeIds.T_double :
				return BaseTypes.DoubleBinding;
			case TypeIds.T_float :
				return BaseTypes.FloatBinding;
			case TypeIds.T_int :
				return BaseTypes.IntBinding;
			case TypeIds.T_long :
				return BaseTypes.LongBinding;
			case TypeIds.T_String :
				return scope.getJavaLangString();
			default:
				return null;
		}
	}
	
	private void trackExpressionTypeBinding(Expression expression, TypeBinding binding, Scope scope) {
		if (fMode == SELECTED && expression == fFirstSelectedNode) {
			fExpressionTypeBinding= binding;
			AstNode parent= getParent();
			fExpressionIsPartOfOperator= expression instanceof Assignment && parent instanceof Expression;
			// Check if the type is visible in the current scope.
			if (binding != null) {
				binding= binding instanceof ArrayBinding ? ((ArrayBinding)binding).leafComponentType : binding;
				TypeBinding checkBinding= scope.getType(binding.sourceName());
				fExpressionTypeIsVisible= checkBinding == binding;
			} else {
				fExpressionTypeIsVisible= false;
			}
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
	
	public void endVisit(CompilationUnitDeclaration compilationUnitDeclaration, CompilationUnitScope scope) {
	}

	private void checkExpression() {
		if (fTopNodes != null && fTopNodes.size() == 1) {
			AstNode node= (AstNode)fTopNodes.get(0);
			if (node instanceof ClassLiteralAccess) {
				invalidSelection("Cannot extract a single access to a class literal.");
			} else if (node instanceof NullLiteral) {
				invalidSelection("Cannot extract the single keyword null.");
			} else if (node instanceof ArrayInitializer) {
				invalidSelection("Cannot extract an array initializer");
			} else if (node instanceof TypeReference) {
				invalidSelection("Cannot extract a single type reference");
			} else if (node instanceof Assignment) {
				if (fExpressionIsPartOfOperator)
					invalidSelection("Cannot extract assignment that is part of another expression.");
				fExpressionTypeBinding= null;
			} else if (node instanceof ConditionalExpression) {
				invalidSelection("Currently no support to extract a single conditional expression.");
			} else if (node instanceof Reference) {
				invalidSelection("Currently no support to extract a single variable or field reference.");
			}
		}
	}
	
	public boolean visit(ImportReference importRef, CompilationUnitScope scope) {
		return false;
	}
	
	public void endVisit(ImportReference importRef, CompilationUnitScope scope) {
	}

	public boolean visit(TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
		return fSelection.enclosedBy(typeDeclaration.declarationSourceStart,
			typeDeclaration.declarationSourceEnd);
	}
	
	public void endVisit(TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
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
	
	public void endVisit(TypeDeclaration typeDeclaration, ClassScope scope) {
	}

	public boolean visit(MemberTypeDeclaration memberTypeDeclaration, ClassScope scope) {
		return fSelection.enclosedBy(memberTypeDeclaration.declarationSourceStart,
			memberTypeDeclaration.declarationSourceEnd);
	}
	
	public void endVisit(MemberTypeDeclaration memberTypeDeclaration, ClassScope scope) {
	}

	public boolean visit(FieldDeclaration fieldDeclaration, MethodScope scope) {
		return false;
	}
	
	public void endVisit(FieldDeclaration fieldDeclaration, MethodScope scope) {
	}

	public boolean visit(Initializer initializer, MethodScope scope) {
		return false;
	}
	
	public void endVisit(Initializer initializer, MethodScope scope) {
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
	
	public void endVisit(SingleTypeReference singleTypeReference, ClassScope scope) {
	}

	public boolean visit(QualifiedTypeReference qualifiedTypeReference, ClassScope scope) {
		return false;
	}
	
	public void endVisit(QualifiedTypeReference qualifiedTypeReference, ClassScope scope) {
	}

	public boolean visit(ArrayTypeReference arrayTypeReference, ClassScope scope) {
		return false;
	}
	
	public void endVisit(ArrayTypeReference arrayTypeReference, ClassScope scope) {
	}

	public boolean visit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, ClassScope scope) {
		return false;
	}
	
	public void endVisit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, ClassScope scope) {
	}

	//---- Methods ----------------------------------------------------------------
	
	public boolean visit(LocalTypeDeclaration localTypeDeclaration, BlockScope scope) {
		return visitLocalTypeDeclaration(localTypeDeclaration, scope);
	}
	
	public void endVisit(LocalTypeDeclaration localTypeDeclaration, BlockScope scope) {
		endVisitRange(localTypeDeclaration.declarationSourceStart, localTypeDeclaration.declarationSourceEnd,
			localTypeDeclaration, scope);
	}

	public boolean visit(Argument argument, BlockScope scope) {
		return false;
	}
	
	public void endVisit(Argument argument, BlockScope scope) {
	}

	//---- Methods / Block --------------------------------------------------------
	
	public boolean visit(TypeDeclaration typeDeclaration, BlockScope scope) {
		return visitLocalTypeDeclaration(typeDeclaration, scope);
	}
	
	public void endVisit(TypeDeclaration typeDeclaration, BlockScope scope) {
		endVisitRange(typeDeclaration.declarationSourceStart, typeDeclaration.declarationSourceEnd,
			typeDeclaration, scope);
	}

	public boolean visit(AnonymousLocalTypeDeclaration anonymousTypeDeclaration, BlockScope scope) {
		if (!checkLocalTypeDeclaration(anonymousTypeDeclaration))
			return false;
			
		return visitRange(anonymousTypeDeclaration.declarationSourceStart,
			anonymousTypeDeclaration.declarationSourceEnd, anonymousTypeDeclaration, scope);
	}
	
	public void endVisit(AnonymousLocalTypeDeclaration anonymousTypeDeclaration, BlockScope scope) {
		endVisitRange(anonymousTypeDeclaration.declarationSourceStart,
			anonymousTypeDeclaration.declarationSourceEnd, anonymousTypeDeclaration, scope);
	}

	public boolean visit(LocalDeclaration localDeclaration, BlockScope scope) {
		// XXX: 1GF089K: ITPJCORE:WIN2000 - AbstractLocalDeclaration.declarationSourceStart includes preceeding comment
		AstNode node= getLastSelectedNode();
		int start= localDeclaration.declarationSourceStart;
		if (node != null && node.sourceEnd < localDeclaration.declarationSourceEnd)
			start= Math.max(start, node.sourceEnd + 1);
		int pos= fBuffer.indexOfStatementCharacter(start);		
		localDeclaration.declarationSourceStart= pos;
		// XXX: end
		
		return visitRange(localDeclaration.declarationSourceStart, localDeclaration.declarationSourceEnd, 
			localDeclaration, scope);
	}

	public void endVisit(LocalDeclaration localDeclaration, BlockScope scope) {
		endVisitRange(localDeclaration.declarationSourceStart, localDeclaration.declarationSourceEnd, 
			localDeclaration, scope);
	}

	public boolean visit(FieldReference fieldReference, BlockScope scope) {
		return false;
	}

	public void endVisit(FieldReference fieldReference, BlockScope scope) {
	}

	public boolean visit(ArrayReference arrayReference, BlockScope scope) {
		return visitNode(arrayReference, scope);
	}

	public void endVisit(ArrayReference arrayReference, BlockScope scope) {
		endVisitNode(arrayReference, scope);
	}

	public boolean visit(ArrayTypeReference arrayTypeReference, BlockScope scope) {
		return handleTypeReference(arrayTypeReference, scope);
	}

	public void endVisit(ArrayTypeReference arrayTypeReference, BlockScope scope) {
		endVisitNode(arrayTypeReference, scope);
	}

	public boolean visit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, BlockScope scope) {
		return handleTypeReference(arrayQualifiedTypeReference, scope);
	}

	public void endVisit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, BlockScope scope) {
		endVisitNode(arrayQualifiedTypeReference, scope);
	}

	public boolean visit(SingleTypeReference singleTypeReference, BlockScope scope) {
		return handleTypeReference(singleTypeReference, scope);
	}

	public void endVisit(SingleTypeReference singleTypeReference, BlockScope scope) {
		endVisitNode(singleTypeReference, scope);
	}

	public boolean visit(QualifiedTypeReference qualifiedTypeReference, BlockScope scope) {
		return handleTypeReference(qualifiedTypeReference, scope);
	}

	public void endVisit(QualifiedTypeReference qualifiedTypeReference, BlockScope scope) {
		endVisitNode(qualifiedTypeReference, scope);
	}

	public boolean visit(QualifiedNameReference qualifiedNameReference, BlockScope scope) {
		return visitNode(qualifiedNameReference, scope);
	}

	public void endVisit(QualifiedNameReference qualifiedNameReference, BlockScope scope) {
		endVisitNode(qualifiedNameReference, scope);
	}

	public boolean visit(QualifiedSuperReference qualifiedSuperReference, BlockScope scope) {
		return false;
	}

	public void endVisit(QualifiedSuperReference qualifiedSuperReference, BlockScope scope) {
	}

	public boolean visit(QualifiedThisReference qualifiedThisReference, BlockScope scope) {
		return false;
	}

	public void endVisit(QualifiedThisReference qualifiedThisReference, BlockScope scope) {
	}

	public boolean visit(SingleNameReference singleNameReference, BlockScope scope) {
		boolean result= visitNode(singleNameReference, scope);
		if (result) {
			fLocalTypeAnalyzer.visit(singleNameReference, scope, fMode);
		}	
		return result;
	}

	public void endVisit(SingleNameReference singleNameReference, BlockScope scope) {
		endVisitNode(singleNameReference, scope);
	}

	public boolean visit(SuperReference superReference, BlockScope scope) {
		return false;
	}

	public void endVisit(SuperReference superReference, BlockScope scope) {
	}

	public boolean visit(ThisReference thisReference, BlockScope scope) {
		return false;
	}

	public void endVisit(ThisReference thisReference, BlockScope scope) {
	}

	//---- Statements -------------------------------------------------------------
	
	public boolean visit(AllocationExpression allocationExpression, BlockScope scope) {
		if (!visitNode(allocationExpression, scope))
			return false;
		trackExpressionTypeBinding(allocationExpression, allocationExpression.type.binding, scope);
		return true;
	}
	
	public void endVisit(AllocationExpression allocationExpression, BlockScope scope) {
		endVisitNode(allocationExpression, scope);
	}

	public boolean visit(AND_AND_Expression and_and_Expression, BlockScope scope) {
		return visitBinaryExpression(and_and_Expression, scope);
	}
	
	public void endVisit(AND_AND_Expression and_and_Expression, BlockScope scope) {
		endVisitNode(and_and_Expression, scope);
	}

	public boolean visit(ArrayAllocationExpression arrayAllocationExpression, BlockScope scope) {
		if (!visitNode(arrayAllocationExpression, scope))
			return false;
		trackExpressionTypeBinding(arrayAllocationExpression, 
			scope.createArray(arrayAllocationExpression.type.binding, arrayAllocationExpression.dimensions.length),
			scope);
		return true;
	}
	
	public void endVisit(ArrayAllocationExpression arrayAllocationExpression, BlockScope scope) {
		endVisitNode(arrayAllocationExpression, scope);
	}

	public boolean visit(ArrayInitializer arrayInitializer, BlockScope scope) {
		return visitNode(arrayInitializer, scope);
	}
	
	public void endVisit(ArrayInitializer arrayInitializer, BlockScope scope) {
		endVisitNode(arrayInitializer, scope);
	}
	
	public boolean visit(AssertStatement node, BlockScope scope) {
		return visitNode(node, scope);
	}

	public void endVisit(AssertStatement node, BlockScope scope) {
		endVisitNode(node, scope);
	}
	
	public boolean visit(Assignment assignment, BlockScope scope) {
		// XXX: 1GK1HWY: ITPJCORE:WIN2000 - Broken sourceEnd in for Assignment and CompoundAssignment
		if (assignment.expression != null)
			assignment.sourceEnd= assignment.expression.sourceEnd;
			
		return visitAssignment(assignment, scope);
	}

	public void endVisit(Assignment assignment, BlockScope scope) {
		endVisitAssignment(assignment, scope, false);
	}
	
	public boolean visit(BinaryExpression binaryExpression, BlockScope scope) {
		return visitBinaryExpression(binaryExpression, scope);
	}

	public void endVisit(BinaryExpression binaryExpression, BlockScope scope) {
		endVisitNode(binaryExpression, scope);
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
		endVisitNode(block, scope);
		if (fSelection.covers(block))
			fNeedsSemicolon= false;
	}

	public boolean visit(Break breakStatement, BlockScope scope) {
		if (breakStatement.label == null) {
			breakStatement.sourceEnd= breakStatement.sourceStart + BREAK_LENGTH - 1;
		}
		return visitBranchStatement(breakStatement, scope, "break"); //$NON-NLS-1$
	}

	public void endVisit(Break breakStatement, BlockScope scope) {
		endVisitNode(breakStatement, scope);
	}

	public boolean visit(Case caseStatement, BlockScope scope) {
		if (!visitNode(caseStatement, scope))
			return false;
		return true;
	}

	public void endVisit(Case caseStatement, BlockScope scope) {
		endVisitNode(caseStatement, scope);
	}

	public boolean visit(CastExpression castExpression, BlockScope scope) {
		if (!visitNode(castExpression, scope))
			return false;
		trackExpressionTypeBinding(castExpression, castExpression.castTb, scope);
		return true;
	}

	public void endVisit(CastExpression castExpression, BlockScope scope) {
		endVisitNode(castExpression, scope);
	}

	public boolean visit(CharLiteral charLiteral, BlockScope scope) {
		return visitLiteral(charLiteral, scope);
	}

	public void endVisit(CharLiteral charLiteral, BlockScope scope) {
		endVisitNode(charLiteral, scope);
	}

	public boolean visit(ClassLiteralAccess classLiteral, BlockScope scope) {
		return visitNode(classLiteral, scope);
	}

	public void endVisit(ClassLiteralAccess classLiteral, BlockScope scope) {
		endVisitNode(classLiteral, scope);
	}

	public boolean visit(CompoundAssignment compoundAssignment, BlockScope scope) {
		// XXX: 1GK1HWY: ITPJCORE:WIN2000 - Broken sourceEnd in for Assignment and CompoundAssignment
		if (compoundAssignment.expression != null)
			compoundAssignment.sourceEnd= compoundAssignment.expression.sourceEnd;
			
		return visitAssignment(compoundAssignment, scope);
	}
	
	public void endVisit(CompoundAssignment compoundAssignment, BlockScope scope) {
		endVisitAssignment(compoundAssignment, scope, true);
	}

	public boolean visit(ConditionalExpression conditionalExpression, BlockScope scope) {
		return visitNode(conditionalExpression, scope);
	}

	public void endVisit(ConditionalExpression conditionalExpression, BlockScope scope) {
		endVisitNode(conditionalExpression, scope);
	}

	public boolean visit(Continue continueStatement, BlockScope scope) {
		if (continueStatement.label == null) {
			continueStatement.sourceEnd= continueStatement.sourceStart + CONTINUE_LENGTH - 1;
		}
		return visitBranchStatement(continueStatement, scope, "continue"); //$NON-NLS-1$
	}

	public void endVisit(Continue continueStatement, BlockScope scope) {
		endVisitNode(continueStatement, scope);
	}

	public boolean visit(DefaultCase defaultCaseStatement, BlockScope scope) {
		if (!visitNode(defaultCaseStatement, scope))
			return false;
		return true;
	}

	public void endVisit(DefaultCase defaultCaseStatement, BlockScope scope) {
		endVisitNode(defaultCaseStatement, scope);
	}

	public boolean visit(DoStatement doStatement, BlockScope scope) {
		if (!visitImplicitBranchTarget(doStatement, scope))
			return false;
		
		int actionStart= fBuffer.indexAfter(Scanner.TokenNamedo, doStatement.sourceStart);
		if (fSelection.start == actionStart) {
			invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.after_do_keyword")); //$NON-NLS-1$
			return false;
		}
		
		return true;
	}
	
	public void endVisit(DoStatement doStatement, BlockScope scope) {
		endVisitImplicitBranchTarget(doStatement, scope);
	}

	public boolean visit(DoubleLiteral doubleLiteral, BlockScope scope) {
		return visitLiteral(doubleLiteral, scope);
	}

	public void endVisit(DoubleLiteral doubleLiteral, BlockScope scope) {
		endVisitNode(doubleLiteral, scope);
	}

	public boolean visit(EmptyStatement node, BlockScope scope) {
		return visitNode(node, scope);
	}
	
	public void endVisit(EmptyStatement node, BlockScope scope) {
		endVisitNode(node, scope);
	}
	
	public boolean visit(EqualExpression equalExpression, BlockScope scope) {
		if (!visitNode(equalExpression, scope))
			return false;
		trackExpressionTypeBinding(equalExpression, BaseTypes.BooleanBinding, scope);
		return true;	
	}

	public void endVisit(EqualExpression equalExpression, BlockScope scope) {
		endVisitNode(equalExpression, scope);
	}

	public boolean visit(ExplicitConstructorCall explicitConstructor, BlockScope scope) {
		if (!visitNode(explicitConstructor, scope))
			return false;
		if (fMode == SELECTED) {
			invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.super_or_this")); //$NON-NLS-1$
			return false;
		}
		return true;
	}

	public void endVisit(ExplicitConstructorCall explicitConstructor, BlockScope scope) {
		endVisitNode(explicitConstructor, scope);
	}
	
	public boolean visit(ExtendedStringLiteral extendedStringLiteral, BlockScope scope) {
		return visitLiteral(extendedStringLiteral, scope);
	}

	public void endVisit(ExtendedStringLiteral extendedStringLiteral, BlockScope scope) {
		endVisitNode(extendedStringLiteral, scope);
	}

	public boolean visit(FalseLiteral falseLiteral, BlockScope scope) {
		return visitLiteral(falseLiteral, scope);
	}

	public void endVisit(FalseLiteral falseLiteral, BlockScope scope) {
		endVisitNode(falseLiteral, scope);
	}

	public boolean visit(FloatLiteral floatLiteral, BlockScope scope) {
		return visitLiteral(floatLiteral, scope);
	}

	public void endVisit(FloatLiteral floatLiteral, BlockScope scope) {
		endVisitNode(floatLiteral, scope);
	}

	public boolean visit(ForStatement forStatement, BlockScope scope) {
		// XXX: 1GIT8SA: ITPJCORE:WIN2000 - AST: wrong sourceEnd if action is Block
		if (forStatement.action instanceof Block)
			forStatement.sourceEnd= forStatement.action.sourceEnd;
			
		// XXX: 1GK1I2J: ITPJCORE:WIN2000 - Broken SourceEnd in ForStatement and WhileStatement
		if (fBuffer.getCharAt(forStatement.sourceEnd) == ';')
			forStatement.sourceEnd--;
			
		if (!visitImplicitBranchTarget(forStatement, scope))
			return false;

		return true;
	}
	
	public void endVisit(ForStatement forStatement, BlockScope scope) {
		endVisitImplicitBranchTarget(forStatement, scope);
		// Check if an initailizer or an increment statement has been selected.
		if (fMode == AFTER) {
			if (contains(forStatement.initializations, fFirstSelectedNode)) {
				invalidSelection("Cannot extract initialization part of a for statement.");
				return;
			} else if (contains(forStatement.increments, getLastSelectedNode())) {
				invalidSelection("Cannot extract increment part of a for statement.");
				return;
			}
		}
	}
	
	public boolean visit(IfStatement ifStatement, BlockScope scope) {
		// XXX: 1GIT8SA: ITPJCORE:WIN2000 - AST: wrong sourceEnd if action is Block
		if (ifStatement.elseStatement != null) {
			if (ifStatement.elseStatement instanceof Block)
				ifStatement.sourceEnd= ifStatement.elseStatement.sourceEnd;
		} else {
			if (ifStatement.thenStatement instanceof Block)
				ifStatement.sourceEnd= ifStatement.thenStatement.sourceEnd;
		}
			
		// XXX: 1GK1I2J: ITPJCORE:WIN2000 - Broken SourceEnd in ForStatement and WhileStatement
		if (fBuffer.getCharAt(ifStatement.sourceEnd) == ';')
			ifStatement.sourceEnd--;
			
		if (!visitNode(ifStatement, scope))
			return false;
			
		return true;	
	}
		
	public void endVisit(IfStatement ifStatement, BlockScope scope) {
		endVisitNode(ifStatement, scope);
	}

	public boolean visit(InstanceOfExpression instanceOfExpression, BlockScope scope) {
		if (!visitNode(instanceOfExpression, scope))
			return false;
		trackExpressionTypeBinding(instanceOfExpression, BaseTypes.BooleanBinding, scope);
		return true;
	}

	public void endVisit(InstanceOfExpression instanceOfExpression, BlockScope scope) {
		endVisitNode(instanceOfExpression, scope);
	}

	public boolean visit(IntLiteral intLiteral, BlockScope scope) {
		return visitLiteral(intLiteral, scope);
	}

	public void endVisit(IntLiteral intLiteral, BlockScope scope) {
		endVisitNode(intLiteral, scope);
	}

	public boolean visit(LabeledStatement labeledStatement, BlockScope scope) {
		// XXX: 1GIT8SA: ITPJCORE:WIN2000 - AST: wrong sourceEnd if action is Block
		if (labeledStatement.statement instanceof Block)
			labeledStatement.sourceEnd= labeledStatement.statement.sourceEnd;
			
		fLabeledStatements.add(labeledStatement);
		if (!visitNode(labeledStatement, scope))
			return false;
		return true;
	}

	public void endVisit(LabeledStatement labeledStatement, BlockScope scope) {
		endVisitNode(labeledStatement, scope);
	}

	public boolean visit(LongLiteral longLiteral, BlockScope scope) {
		return visitLiteral(longLiteral, scope);
	}

	public void endVisit(LongLiteral longLiteral, BlockScope scope) {
		endVisitNode(longLiteral, scope);
	}

	public boolean visit(MessageSend messageSend, BlockScope scope) {
		if (!visitNode(messageSend, scope))
			return false;
		if (messageSend.binding.returnType != BaseTypeBinding.VoidBinding)
			trackExpressionTypeBinding(messageSend, messageSend.binding.returnType, scope);
		fExceptionAnalyzer.visit(messageSend, scope, fMode);
		return true;
	}

	public void endVisit(MessageSend messageSend, BlockScope scope) {
		endVisitNode(messageSend, scope);
	}

	public boolean visit(NullLiteral nullLiteral, BlockScope scope) {
		return visitLiteral(nullLiteral, scope);
	}

	public void endVisit(NullLiteral nullLiteral, BlockScope scope) {
		endVisitNode(nullLiteral, scope);
	}

	public boolean visit(OR_OR_Expression or_or_Expression, BlockScope scope) {
		return visitBinaryExpression(or_or_Expression, scope);
	}

	public void endVisit(OR_OR_Expression or_or_Expression, BlockScope scope) {
		endVisitNode(or_or_Expression, scope);
	}

	public boolean visit(PostfixExpression postfixExpression, BlockScope scope) {
		boolean result= visitNode(postfixExpression, scope);
		trackExpressionTypeBinding(postfixExpression, null, scope);
		return result;
	}

	public void endVisit(PostfixExpression postfixExpression, BlockScope scope) {
		endVisitNode(postfixExpression, scope);
	}

	public boolean visit(PrefixExpression prefixExpression, BlockScope scope) {
		boolean result= visitNode(prefixExpression, scope);
		trackExpressionTypeBinding(prefixExpression, null, scope);
		return result;
	}

	public void endVisit(PrefixExpression prefixExpression, BlockScope scope) {
		endVisitNode(prefixExpression, scope);
	}

	public boolean visit(QualifiedAllocationExpression qualifiedAllocationExpression, BlockScope scope) {
		// XXX http://dev.eclipse.org/bugs/show_bug.cgi?id=4385
		if (qualifiedAllocationExpression.anonymousType instanceof AnonymousLocalTypeDeclaration) {
			AnonymousLocalTypeDeclaration decl= (AnonymousLocalTypeDeclaration)qualifiedAllocationExpression.anonymousType;
			qualifiedAllocationExpression.sourceEnd= decl.declarationSourceEnd;
		}
		// XXX end http://dev.eclipse.org/bugs/show_bug.cgi?id=4385
		if (!visitNode(qualifiedAllocationExpression, scope))
			return false;
		trackExpressionTypeBinding(qualifiedAllocationExpression, qualifiedAllocationExpression.type.binding, scope);
		return true;
	}

	public void endVisit(QualifiedAllocationExpression qualifiedAllocationExpression, BlockScope scope) {
		endVisitNode(qualifiedAllocationExpression, scope);
	}

	public boolean visit(ReturnStatement returnStatement, BlockScope scope) {
		boolean result= visitNode(returnStatement, scope);
		return result;
	}

	public void endVisit(ReturnStatement returnStatement, BlockScope scope) {
		endVisitNode(returnStatement, scope);
	}

	public boolean visit(StringLiteral stringLiteral, BlockScope scope) {
		return visitLiteral(stringLiteral, scope);
	}

	public void endVisit(StringLiteral stringLiteral, BlockScope scope) {
		endVisitNode(stringLiteral, scope);
	}

	public boolean visit(SwitchStatement switchStatement, BlockScope scope) {
		// Include "}" into switch statement
		switchStatement.sourceEnd++;
		if (!visitImplicitBranchTarget(switchStatement, scope))
			return false;
		
		return true;
	}

	public void endVisit(SwitchStatement switchStatement, BlockScope scope) {
		endVisitImplicitBranchTarget(switchStatement, scope);
		switch(fMode) {
			case SELECTED:
				fNeedsSemicolon= false;
				break;
			case AFTER:
				for (Iterator iter= fTopNodes.iterator(); iter.hasNext(); ) {
					AstNode node= (AstNode)iter.next();
					if (node == switchStatement.defaultCase || contains(switchStatement.cases, node)) {
						invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.switch_statement")); //$NON-NLS-1$
						break;
					}
				}
				break;
		}
	}

	public boolean visit(SynchronizedStatement synchronizedStatement, BlockScope scope) {
		return visitNode(synchronizedStatement, scope);
	}

	public void endVisit(SynchronizedStatement synchronizedStatement, BlockScope scope) {
		endVisitNode(synchronizedStatement, scope);
		if (fMode == SELECTED) {
			fNeedsSemicolon= false;
			if (fFirstSelectedNode == synchronizedStatement.block) {
				invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.synchronized_statement")); //$NON-NLS-1$
			}
		}
	}

	public boolean visit(ThrowStatement throwStatement, BlockScope scope) {
		if (!visitNode(throwStatement, scope))
			return false;
		fExceptionAnalyzer.visit(throwStatement, scope, fMode);
		return true;
	}

	public void endVisit(ThrowStatement throwStatement, BlockScope scope) {
		endVisitNode(throwStatement, scope);
	}

	public boolean visit(TrueLiteral trueLiteral, BlockScope scope) {
		return visitLiteral(trueLiteral, scope);
	}

	public void endVisit(TrueLiteral trueLiteral, BlockScope scope) {
		endVisitNode(trueLiteral, scope);
	}

	public boolean visit(TryStatement tryStatement, BlockScope scope) {
		// Include "}" into sourceEnd;
		tryStatement.sourceEnd++;
		
		if (!visitNode(tryStatement, scope))
			return false;
			
		fExceptionAnalyzer.visit(tryStatement, scope, fMode);
		
		return true;
	}

	public void endVisit(TryStatement tryStatement, BlockScope scope) {
		endVisitNode(tryStatement, scope);
		if (tryStatement.catchArguments != null)
			fExceptionAnalyzer.visitCatchArguments(tryStatement.catchArguments, scope, fMode);
		fExceptionAnalyzer.endVisit(tryStatement, scope, fMode);
		switch (fMode) {
			case SELECTED:
				fNeedsSemicolon= false;
				// fall through
			case AFTER:
				if (fFirstSelectedNode == tryStatement.tryBlock || fFirstSelectedNode == tryStatement.finallyBlock ||
						contains(tryStatement.catchBlocks, fFirstSelectedNode))
					invalidSelection(RefactoringCoreMessages.getString("StatementAnalyzer.try_statement"));
		}
	}
		
	public boolean visit(UnaryExpression unaryExpression, BlockScope scope) {
		if (!visitNode(unaryExpression, scope))
			return false;
		trackExpressionTypeBinding(unaryExpression, getTypeBinding(unaryExpression, scope), scope);
		return true;
	}

	public void endVisit(UnaryExpression unaryExpression, BlockScope scope) {
		endVisitNode(unaryExpression, scope);
	}

	public boolean visit(WhileStatement whileStatement, BlockScope scope) {
		// XXX: 1GIT8SA: ITPJCORE:WIN2000 - AST: wrong sourceEnd if action is Block
		if (whileStatement.action instanceof Block)
			whileStatement.sourceEnd= whileStatement.action.sourceEnd;
		
		// XXX: 1GK1I2J: ITPJCORE:WIN2000 - Broken SourceEnd in ForStatement and WhileStatement
		if (fBuffer.getCharAt(whileStatement.sourceEnd) == ';')
			whileStatement.sourceEnd--;
			
		if (!visitImplicitBranchTarget(whileStatement, scope))
			return false;
		
		return true;
	} 

	public void endVisit(WhileStatement whileStatement, BlockScope scope) {
		endVisitImplicitBranchTarget(whileStatement, scope);
	}
}