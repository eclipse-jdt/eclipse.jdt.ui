/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - refactored to base version in jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodReference;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ProvidesDirective;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchExpression;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.manipulation.CUCorrectionProposalCore;
import org.eclipse.jdt.core.manipulation.ChangeCorrectionProposalCore;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.core.manipulation.dom.NecessaryParenthesesChecker;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.core.manipulation.util.Strings;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility2Core;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.BodyDeclarationRewrite;
import org.eclipse.jdt.internal.corext.dom.CodeScopeBuilder;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CodeStyleFixCore;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore;
import org.eclipse.jdt.internal.corext.fix.IProposableFix;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModelCore;
import org.eclipse.jdt.internal.corext.fix.RenameUnusedVariableFixCore;
import org.eclipse.jdt.internal.corext.fix.SealedClassFixCore;
import org.eclipse.jdt.internal.corext.fix.StringFixCore;
import org.eclipse.jdt.internal.corext.fix.TypeParametersFixCore;
import org.eclipse.jdt.internal.corext.fix.UnimplementedCodeFixCore;
import org.eclipse.jdt.internal.corext.fix.UnusedCodeFixCore;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.surround.ExceptionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryCatchAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryCatchRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryWithResourcesAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryWithResourcesRefactoringCore;
import org.eclipse.jdt.internal.corext.refactoring.util.NoCommentSourceRangeComputer;
import org.eclipse.jdt.internal.corext.refactoring.util.SurroundWithAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.util.TightSourceRangeComputer;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposalCore;

import org.eclipse.jdt.internal.ui.fix.CodeStyleCleanUpCore;
import org.eclipse.jdt.internal.ui.fix.StringCleanUpCore;
import org.eclipse.jdt.internal.ui.fix.TypeParametersCleanUpCore;
import org.eclipse.jdt.internal.ui.fix.UnimplementedCodeCleanUpCore;
import org.eclipse.jdt.internal.ui.fix.UnnecessaryCodeCleanUpCore;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.text.correction.proposals.AssignToVariableAssistProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposalCore.ChangeDescription;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposalCore.InsertDescription;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposalCore.RemoveDescription;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ConstructorFromSuperclassProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CreateNewObjectProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CreateObjectReferenceProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CreateVariableReferenceProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.GenerateForLoopAssistProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedNamesAssistProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedOpenDeclarationProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.MissingAnnotationAttributesProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ModifierChangeCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewLocalVariableCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewMethodCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewProviderMethodDeclarationCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewVariableCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.RefactoringCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ReplaceCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.util.ASTHelper;

public abstract class LocalCorrectionsBaseSubProcessor<T> {

	private final String ADD_EXCEPTION_TO_THROWS_ID= "org.eclipse.jdt.ui.correction.addThrowsDecl"; //$NON-NLS-1$

	private final String ADD_FIELD_QUALIFICATION_ID= "org.eclipse.jdt.ui.correction.qualifyField"; //$NON-NLS-1$

	public static final String ASSIGN_IN_TRY_WITH_RESOURCES_ID= "org.eclipse.jdt.ui.correction.assignInTryWithResources.assist"; //$NON-NLS-1$

	public static final String ASSIGN_TO_FIELD_ID= "org.eclipse.jdt.ui.correction.assignToField.assist"; //$NON-NLS-1$

	public static final String ASSIGN_TO_LOCAL_ID= "org.eclipse.jdt.ui.correction.assignToLocal.assist"; //$NON-NLS-1$

	private static final String REMOVE_UNNECESSARY_NLS_TAG_ID= "org.eclipse.jdt.ui.correction.removeNlsTag"; //$NON-NLS-1$

	private static final String ADD_NON_NLS_ID= "org.eclipse.jdt.ui.correction.addNonNLS"; //$NON-NLS-1$

	private static final String ADD_STATIC_ACCESS_ID= "org.eclipse.jdt.ui.correction.changeToStatic"; //$NON-NLS-1$

	public static final int SURROUND_WITH_TRY_CATCH= 0x100;
	public static final int SURROUND_WITH_TRY_MULTI_CATCH= 0x101;
	public static final int ADD_EXCEPTION_TO_CATCH= 0x102;
	public static final int ADD_EXCEPTION_TO_MULTI_CATCH= 0x103;
	public static final int ADD_THROWS= 0x104;
	public static final int ADD_ADDITIONAL_CATCH= 0x105;
	public static final int STATIC_INDIRECT_ACCESS= 0x200;
	public static final int STATIC_NON_STATIC_ACCESS_USING_TYPE= 0x201;
	public static final int STATIC_INSTANCE_ACCESS= 0x202;
	public static final int INITIALIZE_VARIABLE=0x203;
	public static final int ADD_CONSTRUCTOR_FROM_SUPERCLASS= 0x204;

	private static final int ADD_NEW_OBJECT= 0;

	private static final int ADD_NEW_OBJECT_2= 0;

	private static final int ADD_NEW_OBJECT_3= 0;

	private static final int CREATE_OBJECT_REFERENCE= 0;

	private static final int CREATE_VARIABLE_REFERENCE= 0;

	public static final int EXPRESSION_SHOULD_BE_VARIABLE= 0x223;
	public static final int UNUSED_CODE= 0x300;
	public static final int RENAME_CODE= 0x301;
	public static final int REMOVE_REDUNDANT_SUPERINTERFACE= 0x302;
	public static final int REMOVE_SEMICOLON= 0x303;
	public static final int REMOVE_UNNECESSARY_CAST= 0x304;
	public static final int REMOVE_UNNECESSARY_INSTANCEOF= 0x305;
	public static final int UNQUALIFY_ENUM_CONSTANT= 0x306;
	public static final int UNQUALIFIED_FIELD_ACCESS= 0x307;
	public static final int MOVE_ELSE_CLAUSE= 0x308;
	public static final int CHANGE_EXTENDS_TO_IMPLEMENTS= 0x309;
	public static final int CHANGE_TO_INTERFACE= 0x30a;
	public static final int ADD_PERMITTED_TYPE= 0x30b;
	public static final int ADD_SEALED_SUPERTYPE= 0x30c;
	public static final int REMOVE_PROPOSAL= 0x30d;
	public static final int RETURN_ALLOCATED_OBJECT= 0x30e;
	public static final int CREATE_PARAMETER= 0x400;
	public static final int CHANGE_CODE= 0x401;
	public static final int MISSING_ANNOTATION_ATTRIBUTES= 0x401;
	public static final int INSERT_BREAK_STATEMENT= 0x402;
	public static final int INSERT_FALL_THROUGH= 0x403;
	public static final int INSERT_CASES_OMITTED= 0x404;
	public static final int REPLACE_FIELD_ACCESS= 0x405;
	public static final int ADD_MISSING_CASE= 0x406;
	public static final int CREATE_DEFAULT= 0x407;
	public static final int ADD_OVERRIDE= 0x408;
	public static final int CHANGE_MODIFIER= 0x409;
	public static final int CREATE_CONSTRUCTOR= 0x40a;
	public static final int REMOVE_DEFAULT= 0x40b;
	public static final int ADD_PERMITTED_TYPES= 0x40c;
	public static final int REMOVE_REDUNDANT_TYPE_ARGS= 0x40d;
	public static final int REMOVE_UNNECESSARY_NLS= 0x40e;
	public static final int ADD_NLS= 0x40f;
	public static final int ADD_UNIMPLEMENTED_METHODS= 0x410;
	public static final int ADD_STATIC_ACCESS= 0x411;
	public static final int DELETE_ID= 0x412;
	public static final int RENAME_ID= 0x413;
	public static final int INVALID_OPERATOR= 0x414;
	public static final int CORRECTION_CHANGE_ID=0x415;
	public static final int MISC_PUBLIC_ID=0x416;

	private static class CompareInBitWiseOpFinder extends ASTVisitor {

		private InfixExpression fCompareExpression= null;

		private final ASTNode fSelectedNode;

		public CompareInBitWiseOpFinder(ASTNode selectedNode) {
			fSelectedNode= selectedNode;
			selectedNode.accept(this);
		}

		@Override
		public boolean visit(InfixExpression e) {
			InfixExpression.Operator op= e.getOperator();
			if (isBitOperation(op)) {
				return true;
			} else if (op == InfixExpression.Operator.EQUALS || op == InfixExpression.Operator.NOT_EQUALS) {
				fCompareExpression= e;
				return false;
			}
			return false;
		}

		public InfixExpression getCompareExpression() {
			return fCompareExpression;
		}

		public InfixExpression getParentInfixExpression() {
			ASTNode expr= fSelectedNode;
			ASTNode parent= expr.getParent(); // include all parents
			while (parent instanceof InfixExpression && isBitOperation(((InfixExpression) parent).getOperator())) {
				expr= parent;
				parent= expr.getParent();
			}
			return (InfixExpression) expr;
		}
	}

	private static boolean isBitOperation(InfixExpression.Operator op) {
		return op == InfixExpression.Operator.AND || op == InfixExpression.Operator.OR || op == InfixExpression.Operator.XOR;
	}

	public void getTypeArgumentsFromContext(IInvocationContext context, IProblemLocation problem,
			Collection<T> proposals, UnresolvedElementsBaseSubProcessor<T> processor) {
		// similar to UnresolvedElementsSubProcessor.getTypeProposals(context, problem, proposals);

		ICompilationUnit cu= context.getCompilationUnit();

		CompilationUnit root= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(root);
		if (selectedNode == null) {
			return;
		}

		while (selectedNode.getLocationInParent() == QualifiedName.NAME_PROPERTY) {
			selectedNode= selectedNode.getParent();
		}

		Name node= null;
		if (selectedNode instanceof SimpleType) {
			node= ((SimpleType) selectedNode).getName();
		} else if (selectedNode instanceof NameQualifiedType) {
			node= ((NameQualifiedType) selectedNode).getName();
		} else if (selectedNode instanceof ArrayType) {
			Type elementType= ((ArrayType) selectedNode).getElementType();
			if (elementType.isSimpleType()) {
				node= ((SimpleType) elementType).getName();
			} else if (elementType.isNameQualifiedType()) {
				node= ((NameQualifiedType) elementType).getName();
			} else {
				return;
			}
		} else if (selectedNode instanceof Name) {
			node= (Name) selectedNode;
		} else {
			return;
		}

		// try to resolve type in context
		ITypeBinding binding= ASTResolving.guessBindingForTypeReference(node);
		if (binding != null) {
			ASTNode parent= node.getParent();
			if (parent instanceof Type && parent.getLocationInParent() == ClassInstanceCreation.TYPE_PROPERTY && binding.isInterface()) { //bug 351853
				return;
			}
			if (parent instanceof Type && parent.getLocationInParent() != VariableDeclarationStatement.TYPE_PROPERTY
					&& parent.getLocationInParent() != SingleVariableDeclaration.TYPE_PROPERTY
					&& parent.getLocationInParent() != FieldDeclaration.TYPE_PROPERTY) {
				return;
			}
			ITypeBinding simpleBinding= binding;
			if (simpleBinding.isArray()) {
				simpleBinding= simpleBinding.getElementType();
			}
			simpleBinding= simpleBinding.getTypeDeclaration();

			if (!simpleBinding.isRecovered()) {
				if (binding.isParameterizedType() && (node.getParent() instanceof SimpleType || node.getParent() instanceof NameQualifiedType) && !(node.getParent().getParent() instanceof Type)) {
					proposals.add(processor.createTypeRefChangeFullProposal(cu, binding, node, IProposalRelevance.TYPE_ARGUMENTS_FROM_CONTEXT, TypeLocation.TYPE_ARGUMENT));
				}
			}
		} else {
			ASTNode normalizedNode= ASTNodes.getNormalizedNode(node);
			if (!(normalizedNode.getParent() instanceof Type) && node.getParent() != normalizedNode) {
				ITypeBinding normBinding= ASTResolving.guessBindingForTypeReference(normalizedNode);
				if (normBinding != null && !normBinding.isRecovered()) {
					proposals.add(processor.createTypeRefChangeFullProposal(cu, normBinding, normalizedNode, IProposalRelevance.TYPE_ARGUMENTS_FROM_CONTEXT,
							TypeLocation.TYPE_ARGUMENT));
				}
			}
		}
	}

	public boolean getSplitOrConditionProposalsBase(IInvocationContext context, ASTNode node, Collection<T> resultingCollections) {
		Operator orOperator= InfixExpression.Operator.CONDITIONAL_OR;
		// check that user invokes quick assist on infix expression
		if (!(node instanceof InfixExpression)) {
			return false;
		}
		InfixExpression infixExpression= (InfixExpression) node;
		if (infixExpression.getOperator() != orOperator) {
			return false;
		}
		int offset= isOperatorSelected(infixExpression, context.getSelectionOffset(), context.getSelectionLength());
		if (offset == -1) {
			return false;
		}
		// check that infix expression belongs to IfStatement
		Statement statement= ASTResolving.findParentStatement(node);
		if (!(statement instanceof IfStatement)) {
			return false;
		}
		IfStatement ifStatement= (IfStatement) statement;

		// check that infix expression is part of first level || condition of IfStatement
		InfixExpression topInfixExpression= infixExpression;
		while (topInfixExpression.getParent() instanceof InfixExpression && ((InfixExpression) topInfixExpression.getParent()).getOperator() == orOperator) {
			topInfixExpression= (InfixExpression) topInfixExpression.getParent();
		}
		if (ifStatement.getExpression() != topInfixExpression) {
			return false;
		}
		//
		if (resultingCollections == null) {
			return true;
		}
		AST ast= ifStatement.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);

		// prepare left and right conditions
		Expression[] newOperands= { null, null };
		breakInfixOperationAtOperation(rewrite, topInfixExpression, orOperator, offset, true, newOperands);

		Expression leftCondition= newOperands[0];
		Expression rightCondition= newOperands[1];

		// prepare first statement
		rewrite.replace(ifStatement.getExpression(), leftCondition, null);

		IfStatement secondIf= ast.newIfStatement();
		secondIf.setExpression(rightCondition);
		secondIf.setThenStatement((Statement) rewrite.createCopyTarget(ifStatement.getThenStatement()));

		Statement elseStatement= ifStatement.getElseStatement();
		if (elseStatement == null) {
			rewrite.set(ifStatement, IfStatement.ELSE_STATEMENT_PROPERTY, secondIf, null);
		} else {
			rewrite.replace(elseStatement, secondIf, null);
			secondIf.setElseStatement((Statement) rewrite.createMoveTarget(elseStatement));
		}

		// add correction proposal
		String label= CorrectionMessages.AdvancedQuickAssistProcessor_splitOrCondition_description;
		ASTRewriteCorrectionProposalCore proposal= new ASTRewriteCorrectionProposalCore(label, context.getCompilationUnit(), rewrite, IProposalRelevance.SPLIT_OR_CONDITION);
		resultingCollections.add(astRewriteCorrectionProposalToT(proposal, CORRECTION_CHANGE_ID));
		return true;
	}

	public boolean getSplitAndConditionProposalsBase(IInvocationContext context, ASTNode node, Collection<T> resultingCollections) {
		Operator andOperator= InfixExpression.Operator.CONDITIONAL_AND;
		// check that user invokes quick assist on infix expression
		if (!(node instanceof InfixExpression)) {
			return false;
		}
		InfixExpression infixExpression= (InfixExpression) node;
		if (infixExpression.getOperator() != andOperator) {
			return false;
		}
		int offset= isOperatorSelected(infixExpression, context.getSelectionOffset(), context.getSelectionLength());
		if (offset == -1) {
			return false;
		}

		// check that infix expression belongs to IfStatement
		Statement statement= ASTResolving.findParentStatement(node);
		if (!(statement instanceof IfStatement)) {
			return false;
		}
		IfStatement ifStatement= (IfStatement) statement;

		// check that infix expression is part of first level && condition of IfStatement
		InfixExpression topInfixExpression= infixExpression;
		while (topInfixExpression.getParent() instanceof InfixExpression && ((InfixExpression) topInfixExpression.getParent()).getOperator() == andOperator) {
			topInfixExpression= (InfixExpression) topInfixExpression.getParent();
		}
		if (ifStatement.getExpression() != topInfixExpression) {
			return false;
		}
		//
		if (resultingCollections == null) {
			return true;
		}
		AST ast= ifStatement.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);

		// prepare left and right conditions
		Expression[] newOperands= { null, null };
		breakInfixOperationAtOperation(rewrite, topInfixExpression, andOperator, offset, true, newOperands);

		Expression leftCondition= newOperands[0];
		Expression rightCondition= newOperands[1];

		// replace conditions in outer IfStatement
		rewrite.set(ifStatement, IfStatement.EXPRESSION_PROPERTY, leftCondition, null);

		// prepare inner IfStatement
		IfStatement innerIf= ast.newIfStatement();

		innerIf.setExpression(rightCondition);
		innerIf.setThenStatement((Statement) rewrite.createMoveTarget(ifStatement.getThenStatement()));
		Block innerBlock= ast.newBlock();
		innerBlock.statements().add(innerIf);

		Statement elseStatement= ifStatement.getElseStatement();
		if (elseStatement != null) {
			innerIf.setElseStatement((Statement) rewrite.createCopyTarget(elseStatement));
		}

		// replace outer thenStatement
		rewrite.replace(ifStatement.getThenStatement(), innerBlock, null);

		// add correction proposal
		String label= CorrectionMessages.AdvancedQuickAssistProcessor_splitAndCondition_description;
		ASTRewriteCorrectionProposalCore proposal= new ASTRewriteCorrectionProposalCore(label, context.getCompilationUnit(), rewrite, IProposalRelevance.SPLIT_AND_CONDITION);
		resultingCollections.add(astRewriteCorrectionProposalToT(proposal, CORRECTION_CHANGE_ID));
		return true;
	}

	/*
	 * Breaks an infix operation with possible extended operators at the given operator and returns the new left and right operands.
	 * a & b & c   ->  [[a' & b' ] & c' ]   (c' == copy of c)
	 */
	private void breakInfixOperationAtOperation(ASTRewrite rewrite, Expression expression, Operator operator, int operatorOffset, boolean removeParentheses, Expression[] res) {
		if (expression.getStartPosition() + expression.getLength() <= operatorOffset) {
			// add to the left
			res[0]= combineOperands(rewrite, res[0], expression, removeParentheses, operator);
			return;
		}
		if (operatorOffset <= expression.getStartPosition()) {
			// add to the right
			res[1]= combineOperands(rewrite, res[1], expression, removeParentheses, operator);
			return;
		}
		if (!(expression instanceof InfixExpression)) {
			throw new IllegalArgumentException("Cannot break up non-infix expression"); //$NON-NLS-1$
		}
		InfixExpression infixExpression= (InfixExpression) expression;
		if (infixExpression.getOperator() != operator) {
			throw new IllegalArgumentException("Incompatible operator"); //$NON-NLS-1$
		}
		breakInfixOperationAtOperation(rewrite, infixExpression.getLeftOperand(), operator, operatorOffset, removeParentheses, res);
		breakInfixOperationAtOperation(rewrite, infixExpression.getRightOperand(), operator, operatorOffset, removeParentheses, res);

		List<Expression> extended= infixExpression.extendedOperands();
		for (Expression element : extended) {
			breakInfixOperationAtOperation(rewrite, element, operator, operatorOffset, removeParentheses, res);
		}
	}

	private Expression combineOperands(ASTRewrite rewrite, Expression existing, Expression originalNode, boolean removeParentheses, Operator operator) {
		if (existing == null && removeParentheses) {
			originalNode= ASTNodes.getUnparenthesedExpression(originalNode);
		}
		Expression newRight= (Expression)rewrite.createMoveTarget(originalNode);
		if (originalNode instanceof InfixExpression) {
			((InfixExpression)newRight).setOperator(((InfixExpression)originalNode).getOperator());
		}

		if (existing == null) {
			return newRight;
		}
		AST ast= rewrite.getAST();
		InfixExpression infix= ast.newInfixExpression();
		infix.setOperator(operator);
		infix.setLeftOperand(existing);
		infix.setRightOperand(newRight);
		return infix;
	}

	private boolean isSelectingOperator(ASTNode n1, ASTNode n2, int offset, int length) {
		// between the nodes
		if (offset + length <= n2.getStartPosition() && offset >= ASTNodes.getExclusiveEnd(n1)) {
			return true;
		}
		// or exactly select the node (but not with infix expressions)
		if (n1.getStartPosition() == offset && ASTNodes.getExclusiveEnd(n2) == offset + length) {
			if (n1 instanceof InfixExpression || n2 instanceof InfixExpression) {
				return false;
			}
			return true;
		}
		return false;
	}

	private int isOperatorSelected(InfixExpression infixExpression, int offset, int length) {
		ASTNode left= infixExpression.getLeftOperand();
		ASTNode right= infixExpression.getRightOperand();

		if (isSelectingOperator(left, right, offset, length)) {
			return ASTNodes.getExclusiveEnd(left);
		}
		List<Expression> extended= infixExpression.extendedOperands();
		for (Expression element : extended) {
			left= right;
			right= element;
			if (isSelectingOperator(left, right, offset, length)) {
				return ASTNodes.getExclusiveEnd(left);
			}
		}
		return -1;
	}

	public void getTryWithResourceProposalsBase(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		ASTNode coveringNode= problem.getCoveringNode(context.getASTRoot());
		if (coveringNode != null) {
			try {
				ArrayList<ASTNode> coveredNodes= QuickAssistProcessorUtil.getFullyCoveredNodes(context, coveringNode);
				getTryWithResourceProposalsBase(context, coveringNode, coveredNodes, proposals);
			} catch (IllegalArgumentException | CoreException e) {
				// do nothing
			}
		}
	}

	@SuppressWarnings({ "null" })
	public boolean getTryWithResourceProposalsBase(IInvocationContext context, ASTNode node, ArrayList<ASTNode> coveredNodes, Collection<T> resultingCollections)
			throws IllegalArgumentException, CoreException {
		ASTNode parentStatement= ASTResolving.findAncestor(node, ASTNode.VARIABLE_DECLARATION_STATEMENT);
		if (!(parentStatement instanceof VariableDeclarationStatement) &&
				!(parentStatement instanceof ExpressionStatement) &&
				!(node instanceof SimpleName)
				&& (coveredNodes == null || coveredNodes.isEmpty())) {
			return false;
		}
		List<ASTNode> coveredStatements= new ArrayList<>();
		if (coveredNodes == null || coveredNodes.isEmpty() && parentStatement != null) {
			coveredStatements.add(parentStatement);
		} else {
			for (ASTNode coveredNode : coveredNodes) {
				Statement statement= ASTResolving.findParentStatement(coveredNode);
				if (statement == null) {
					continue;
				}
				if (!coveredStatements.contains(statement)) {
					coveredStatements.add(statement);
				}
			}
		}
		List<ASTNode> coveredAutoClosableNodes= QuickAssistProcessorUtil.getCoveredAutoClosableNodes(coveredStatements);
		if (coveredAutoClosableNodes.isEmpty()) {
			return false;
		}

		ASTNode parentBodyDeclaration= (node instanceof Block || node instanceof BodyDeclaration)
				? node
				: ASTNodes.getFirstAncestorOrNull(node, Block.class, BodyDeclaration.class);

		int start= coveredAutoClosableNodes.get(0).getStartPosition();
		int end= start;

		for (ASTNode astNode : coveredAutoClosableNodes) {
			int endPosition= QuickAssistProcessorUtil.findEndPostion(astNode);
			end= Math.max(end, endPosition);
		}

		// recursive loop to find all nodes affected by wrapping in try block
		List<ASTNode> nodesInRange= SurroundWithTryWithResourcesRefactoringCore.findNodesInRange(parentBodyDeclaration, start, end);
		int oldEnd= end;
		while (true) {
			int newEnd= oldEnd;
			for (ASTNode astNode : nodesInRange) {
				int endPosition= QuickAssistProcessorUtil.findEndPostion(astNode);
				newEnd= Math.max(newEnd, endPosition);
			}
			if (newEnd > oldEnd) {
				oldEnd= newEnd;
				nodesInRange= SurroundWithTryWithResourcesRefactoringCore.findNodesInRange(parentBodyDeclaration, start, newEnd);
				continue;
			}
			break;
		}
		nodesInRange.removeAll(coveredAutoClosableNodes);

		CompilationUnit cu= (CompilationUnit) node.getRoot();
		IBuffer buffer= context.getCompilationUnit().getBuffer();
		AST ast= node.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		boolean modifyExistingTry= false;
		TryStatement newTryStatement= null;
		Block newTryBody= null;
		TryStatement enclosingTry= (TryStatement) ASTResolving.findAncestor(node, ASTNode.TRY_STATEMENT);
		ListRewrite resourcesRewriter= null;
		ListRewrite clausesRewriter= null;
		if (needNewTryBlock(coveredStatements, enclosingTry)) {
			newTryStatement= ast.newTryStatement();
			newTryBody= ast.newBlock();
			newTryStatement.setBody(newTryBody);
		} else {
			modifyExistingTry= true;
			resourcesRewriter= rewrite.getListRewrite(enclosingTry, TryStatement.RESOURCES2_PROPERTY);
			clausesRewriter= rewrite.getListRewrite(enclosingTry, TryStatement.CATCH_CLAUSES_PROPERTY);
		}
		ICompilationUnit icu= context.getCompilationUnit();
		ASTNode lastNode= nodesInRange.isEmpty()
				? coveredAutoClosableNodes.get(coveredAutoClosableNodes.size() - 1)
				: nodesInRange.get(nodesInRange.size() - 1);
		Selection selection= Selection.createFromStartLength(start, lastNode.getStartPosition() - start + lastNode.getLength());
		SurroundWithTryWithResourcesAnalyzer analyzer= new SurroundWithTryWithResourcesAnalyzer(icu, selection);
		cu.accept(analyzer);
		ITypeBinding[] exceptions= analyzer.getExceptions(analyzer.getSelection());
		List<ITypeBinding> allExceptions= new ArrayList<>(Arrays.asList(exceptions));
		int resourceCount= 0;
		for (ASTNode coveredNode : coveredAutoClosableNodes) {
			ASTNode findAncestor= ASTResolving.findAncestor(coveredNode, ASTNode.VARIABLE_DECLARATION_STATEMENT);
			if (findAncestor == null) {
				findAncestor= ASTResolving.findAncestor(coveredNode, ASTNode.ASSIGNMENT);
			}
			if (findAncestor instanceof VariableDeclarationStatement) {
				VariableDeclarationStatement vds= (VariableDeclarationStatement) findAncestor;
				String commentToken= null;
				int extendedStatementStart= cu.getExtendedStartPosition(vds);
				if (vds.getStartPosition() > extendedStatementStart) {
					commentToken= buffer.getText(extendedStatementStart, vds.getStartPosition() - extendedStatementStart);
				}
				Type type= vds.getType();
				ITypeBinding typeBinding= type.resolveBinding();
				if (typeBinding != null) {
					IMethodBinding close= SurroundWithTryWithResourcesRefactoringCore.findAutocloseMethod(typeBinding);
					if (close != null) {
						for (ITypeBinding exceptionType : close.getExceptionTypes()) {
							if (!allExceptions.contains(exceptionType)) {
								allExceptions.add(exceptionType);
							}
						}
					}
				}
				String typeName= buffer.getText(type.getStartPosition(), type.getLength());

				for (Object object : vds.fragments()) {
					VariableDeclarationFragment variableDeclarationFragment= (VariableDeclarationFragment) object;
					VariableDeclarationFragment newVariableDeclarationFragment= ast.newVariableDeclarationFragment();
					SimpleName name= variableDeclarationFragment.getName();

					if (commentToken == null) {
						int extendedStart= cu.getExtendedStartPosition(variableDeclarationFragment);
						commentToken= buffer.getText(extendedStart, variableDeclarationFragment.getStartPosition() - extendedStart);
					}
					commentToken= Strings.trimTrailingTabsAndSpaces(commentToken);

					newVariableDeclarationFragment.setName(ast.newSimpleName(name.getIdentifier()));
					Expression newExpression= null;
					Expression initializer= variableDeclarationFragment.getInitializer();
					if (initializer == null) {
						rewrite.remove(coveredNode, null);
						continue;
					} else {
						newExpression= (Expression) rewrite.createMoveTarget(initializer);
					}
					newVariableDeclarationFragment.setInitializer(newExpression);
					VariableDeclarationExpression newVariableDeclarationExpression= ast.newVariableDeclarationExpression(newVariableDeclarationFragment);
					newVariableDeclarationExpression.setType(
							(Type) rewrite.createStringPlaceholder(commentToken + typeName, type.getNodeType()));
					resourceCount++;
					if (modifyExistingTry) {
						resourcesRewriter.insertLast(newVariableDeclarationExpression, null);
					} else {
						newTryStatement.resources().add(newVariableDeclarationExpression);
					}
					commentToken= null;
				}
			}
		}

		if (resourceCount == 0) {
			return false;
		}

		String label= CorrectionMessages.QuickAssistProcessor_convert_to_try_with_resource;
		LinkedCorrectionProposalCore proposal= new LinkedCorrectionProposalCore(label, context.getCompilationUnit(), rewrite, IProposalRelevance.SURROUND_WITH_TRY_CATCH);

		ImportRewrite imports= proposal.createImportRewrite(context.getASTRoot());
		ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(node, imports);

		CatchClause catchClause= ast.newCatchClause();
		SingleVariableDeclaration decl= ast.newSingleVariableDeclaration();
		String varName= StubUtility.getExceptionVariableName(icu.getJavaProject());
		parentBodyDeclaration.getRoot().accept(analyzer);
		CodeScopeBuilder.Scope scope= CodeScopeBuilder.perform(analyzer.getEnclosingBodyDeclaration(), selection).findScope(selection.getOffset(), selection.getLength());
		scope.setCursor(selection.getOffset());
		String name= scope.createName(varName, false);
		decl.setName(ast.newSimpleName(name));

		List<ITypeBinding> mustRethrowList= new ArrayList<>();
		List<ITypeBinding> catchExceptions= analyzer.calculateCatchesAndRethrows(ASTNodes.filterSubtypes(allExceptions), mustRethrowList);
		List<ITypeBinding> filteredExceptions= ASTNodes.filterSubtypes(catchExceptions);

		if (catchExceptions.size() > 0) {
			final String GROUP_EXC_NAME= "exc_name"; //$NON-NLS-1$
			final String GROUP_EXC_TYPE= "exc_type"; //$NON-NLS-1$
			LinkedProposalModelCore linkedProposalModel= new LinkedProposalModelCore();

			int i= 0;
			if (!modifyExistingTry) {
				for (ITypeBinding mustThrow : mustRethrowList) {
					CatchClause newClause= ast.newCatchClause();
					SingleVariableDeclaration newDecl= ast.newSingleVariableDeclaration();
					newDecl.setName(ast.newSimpleName(name));
					Type importType= imports.addImport(mustThrow, ast, importRewriteContext, TypeLocation.EXCEPTION);
					newDecl.setType(importType);
					newClause.setException(newDecl);
					ThrowStatement newThrowStatement= ast.newThrowStatement();
					newThrowStatement.setExpression(ast.newSimpleName(name));
					linkedProposalModel.getPositionGroup(GROUP_EXC_NAME + i, true).addPosition(rewrite.track(decl.getName()), false);
					newClause.getBody().statements().add(newThrowStatement);
					newTryStatement.catchClauses().add(newClause);
					++i;
				}
			}
			UnionType unionType= ast.newUnionType();
			List<Type> types= unionType.types();
			for (ITypeBinding exception : filteredExceptions) {
				Type type= imports.addImport(exception, ast, importRewriteContext, TypeLocation.EXCEPTION);
				types.add(type);
				linkedProposalModel.getPositionGroup(GROUP_EXC_TYPE + i, true).addPosition(rewrite.track(type), i == 0);
				i++;
			}

			decl.setType(unionType);
			catchClause.setException(decl);
			linkedProposalModel.getPositionGroup(GROUP_EXC_NAME + 0, true).addPosition(rewrite.track(decl.getName()), false);
			Statement st= null;
			String s= StubUtility.getCatchBodyContent(icu, "Exception", name, coveredStatements.get(0), icu.findRecommendedLineSeparator()); //$NON-NLS-1$
			if (s != null) {
				st= (Statement) rewrite.createStringPlaceholder(s, ASTNode.RETURN_STATEMENT);
			}
			if (st != null) {
				catchClause.getBody().statements().add(st);
			}
			if (modifyExistingTry) {
				clausesRewriter.insertLast(catchClause, null);
			} else {
				newTryStatement.catchClauses().add(catchClause);
			}
		}

		if (modifyExistingTry) {
			for (int i= 0; i < coveredAutoClosableNodes.size(); i++) {
				rewrite.remove(coveredAutoClosableNodes.get(i), null);
			}
		} else {
			if (!nodesInRange.isEmpty()) {
				ASTNode firstNode= nodesInRange.get(0);
				ASTNode methodDeclaration= ASTResolving.findAncestor(firstNode, ASTNode.BLOCK);
				ListRewrite listRewrite= rewrite.getListRewrite(methodDeclaration, Block.STATEMENTS_PROPERTY);
				ASTNode createCopyTarget= listRewrite.createMoveTarget(firstNode, nodesInRange.get(nodesInRange.size() - 1));
				rewrite.getListRewrite(newTryBody, Block.STATEMENTS_PROPERTY).insertFirst(createCopyTarget, null);
			}

			// replace first node and delete the rest of selected nodes
			rewrite.replace(coveredAutoClosableNodes.get(0), newTryStatement, null);
			for (int i= 1; i < coveredAutoClosableNodes.size(); i++) {
				rewrite.remove(coveredAutoClosableNodes.get(i), null);
			}
		}

		resultingCollections.add(linkedCorrectionProposalToT(proposal, CORRECTION_CHANGE_ID));
		return true;
	}

	private boolean needNewTryBlock(List<ASTNode> coveredStatements, TryStatement enclosingTry) {
		if (enclosingTry == null || enclosingTry.getBody() == null) {
			return true;
		}
		List<?> statements= enclosingTry.getBody().statements();
		return statements.size() > 0 && coveredStatements.size() > 0 && statements.get(0) != coveredStatements.get(0);
	}

	public void getUnreachableCodeProposalsBase(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		CompilationUnit root= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(root);
		if (selectedNode == null) {
			return;
		}

		ASTNode parent= selectedNode.getParent();
		while (parent instanceof ExpressionStatement) {
			selectedNode= parent;
			parent= selectedNode.getParent();
		}

		if (parent instanceof WhileStatement) {
			addRemoveIncludingConditionProposal(context, parent, null, proposals);

		} else if (selectedNode.getLocationInParent() == IfStatement.THEN_STATEMENT_PROPERTY) {
			Statement elseStatement= ((IfStatement) parent).getElseStatement();
			addRemoveIncludingConditionProposal(context, parent, elseStatement, proposals);

		} else if (selectedNode.getLocationInParent() == IfStatement.ELSE_STATEMENT_PROPERTY) {
			Statement thenStatement= ((IfStatement) parent).getThenStatement();
			addRemoveIncludingConditionProposal(context, parent, thenStatement, proposals);

		} else if (selectedNode.getLocationInParent() == ForStatement.BODY_PROPERTY) {
			Statement body= ((ForStatement) parent).getBody();
			addRemoveIncludingConditionProposal(context, parent, body, proposals);

		} else if (selectedNode.getLocationInParent() == ConditionalExpression.THEN_EXPRESSION_PROPERTY) {
			Expression elseExpression= ((ConditionalExpression) parent).getElseExpression();
			addRemoveIncludingConditionProposal(context, parent, elseExpression, proposals);

		} else if (selectedNode.getLocationInParent() == ConditionalExpression.ELSE_EXPRESSION_PROPERTY) {
			Expression thenExpression= ((ConditionalExpression) parent).getThenExpression();
			addRemoveIncludingConditionProposal(context, parent, thenExpression, proposals);

		} else if (selectedNode.getLocationInParent() == InfixExpression.RIGHT_OPERAND_PROPERTY) {
			// also offer split && / || condition proposals:
			InfixExpression infixExpression= (InfixExpression) parent;
			Expression leftOperand= infixExpression.getLeftOperand();

			ASTRewrite rewrite= ASTRewrite.create(parent.getAST());

			Expression replacement= ASTNodes.getUnparenthesedExpression(leftOperand);

			Expression toReplace= infixExpression;
			while (toReplace.getLocationInParent() == ParenthesizedExpression.EXPRESSION_PROPERTY) {
				toReplace= (Expression) toReplace.getParent();
			}

			if (NecessaryParenthesesChecker.needsParentheses(replacement, toReplace.getParent(), toReplace.getLocationInParent())) {
				if (leftOperand instanceof ParenthesizedExpression) {
					replacement= (Expression) replacement.getParent();
				} else if (infixExpression.getLocationInParent() == ParenthesizedExpression.EXPRESSION_PROPERTY) {
					toReplace= ((ParenthesizedExpression) toReplace).getExpression();
				}
			}

			rewrite.replace(toReplace, rewrite.createMoveTarget(replacement), null);

			String label= CorrectionMessages.LocalCorrectionsSubProcessor_removeunreachablecode_description;
			addRemoveProposal(context, rewrite, label, proposals);

			IInvocationContext assistContext= new IInvocationContext() {
				@Override
				public ICompilationUnit getCompilationUnit() {
					return context.getCompilationUnit();
				}

				@Override
				public int getSelectionOffset() {
					return infixExpression.getRightOperand().getStartPosition() - 1;
				}

				@Override
				public int getSelectionLength() {
					return 0;
				}

				@Override
				public CompilationUnit getASTRoot() {
					return root;
				}

				@Override
				public ASTNode getCoveredNode() {
					return null;
				}

				@Override
				public ASTNode getCoveringNode() {
					return null;
				}
			};
			getSplitAndConditionProposalsBase(assistContext, infixExpression, proposals);
			getSplitOrConditionProposalsBase(assistContext, infixExpression, proposals);

		} else if (selectedNode instanceof Statement && selectedNode.getLocationInParent().isChildListProperty()) {
			// remove all statements following the unreachable:
			List<Statement> statements= ASTNodes.<Statement>getChildListProperty(selectedNode.getParent(), (ChildListPropertyDescriptor) selectedNode.getLocationInParent());
			int idx= statements.indexOf(selectedNode);

			ASTRewrite rewrite= ASTRewrite.create(selectedNode.getAST());
			String label= CorrectionMessages.LocalCorrectionsSubProcessor_removeunreachablecode_description;

			if (idx > 0) {
				Object prevStatement= statements.get(idx - 1);
				if (prevStatement instanceof IfStatement) {
					IfStatement ifStatement= (IfStatement) prevStatement;
					if (ifStatement.getElseStatement() == null) {
						// remove if (true), see https://bugs.eclipse.org/bugs/show_bug.cgi?id=261519
						Statement thenStatement= ifStatement.getThenStatement();
						label= CorrectionMessages.LocalCorrectionsSubProcessor_removeunreachablecode_including_condition_description;
						if (thenStatement instanceof Block) {
							// add all child nodes from Block node
							List<Statement> thenStatements= ((Block) thenStatement).statements();
							if (thenStatements.isEmpty()) {
								return;
							}
							ASTNode[] thenStatementsArray= new ASTNode[thenStatements.size()];
							for (int i= 0; i < thenStatementsArray.length; i++) {
								thenStatementsArray[i]= thenStatements.get(i);
							}
							ASTNode newThenStatement= rewrite.createGroupNode(thenStatementsArray);

							rewrite.replace(ifStatement, newThenStatement, null);
						} else {
							rewrite.replace(ifStatement, thenStatement, null);
						}
					}
				}
			}

			for (int i= idx; i < statements.size(); i++) {
				ASTNode statement= statements.get(i);
				if (statement instanceof SwitchCase)
					break; // stop at case *: and default:
				rewrite.remove(statement, null);
			}

			addRemoveProposal(context, rewrite, label, proposals);


		} else {
			// no special case, just remove the node:
			addRemoveProposal(context, selectedNode, proposals);
		}
	}


	private void addRemoveIncludingConditionProposal(IInvocationContext context, ASTNode toRemove, ASTNode replacement, Collection<T> proposals) {
		String label= CorrectionMessages.LocalCorrectionsSubProcessor_removeunreachablecode_including_condition_description;
		AST ast= toRemove.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		ASTRewriteCorrectionProposalCore proposal= new ASTRewriteCorrectionProposalCore(label, context.getCompilationUnit(), rewrite, IProposalRelevance.REMOVE_UNREACHABLE_CODE_INCLUDING_CONDITION);

		if (replacement == null
				|| replacement instanceof EmptyStatement
				|| replacement instanceof Block && ((Block) replacement).statements().size() == 0) {
			if (ASTNodes.isControlStatementBody(toRemove.getLocationInParent())) {
				rewrite.replace(toRemove, toRemove.getAST().newBlock(), null);
			} else {
				rewrite.remove(toRemove, null);
			}

		} else if (toRemove instanceof Expression && replacement instanceof Expression) {
			Expression moved= (Expression) rewrite.createMoveTarget(replacement);
			Expression toRemoveExpression= (Expression) toRemove;
			Expression replacementExpression= (Expression) replacement;
			ITypeBinding explicitCast= ASTNodes.getExplicitCast(replacementExpression, toRemoveExpression);
			if (explicitCast != null) {
				CastExpression cast= ast.newCastExpression();
				if (NecessaryParenthesesChecker.needsParentheses(replacementExpression, cast, CastExpression.EXPRESSION_PROPERTY)) {
					ParenthesizedExpression parenthesized= ast.newParenthesizedExpression();
					parenthesized.setExpression(moved);
					moved= parenthesized;
				}
				cast.setExpression(moved);
				ImportRewrite imports= proposal.createImportRewrite(context.getASTRoot());
				ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(toRemove, imports);
				cast.setType(imports.addImport(explicitCast, ast, importRewriteContext, TypeLocation.CAST));
				moved= cast;
			}
			rewrite.replace(toRemove, moved, null);

		} else {
			ASTNode parent= toRemove.getParent();
			ASTNode moveTarget;
			if ((parent instanceof Block || parent instanceof SwitchStatement) && replacement instanceof Block) {
				ListRewrite listRewrite= rewrite.getListRewrite(replacement, Block.STATEMENTS_PROPERTY);
				List<Statement> list= ((Block) replacement).statements();
				int lastIndex= list.size() - 1;
				moveTarget= listRewrite.createMoveTarget(list.get(0), list.get(lastIndex));
			} else {
				moveTarget= rewrite.createMoveTarget(replacement);
			}

			rewrite.replace(toRemove, moveTarget, null);
		}

		proposals.add(astRewriteCorrectionProposalToT(proposal, DELETE_ID));
	}

	public void getInvalidVariableNameProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		// hiding, redefined or future keyword

		CompilationUnit root= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(root);
		if (selectedNode instanceof MethodDeclaration methodDeclaration) {
			if (methodDeclaration.isConstructor()) {
				addRemoveProposal(context, methodDeclaration, proposals);
				return;
			}
			selectedNode= methodDeclaration.getName();
		}
		if (!(selectedNode instanceof SimpleName)) {
			return;
		}
		SimpleName nameNode= (SimpleName) selectedNode;
		String valueSuggestion= null;

		String name;
		switch (problem.getProblemId()) {
			case IProblem.LocalVariableHidingLocalVariable:
			case IProblem.LocalVariableHidingField:
				name= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_hiding_local_label, BasicElementLabels.getJavaElementName(nameNode.getIdentifier()));
				break;
			case IProblem.FieldHidingLocalVariable:
			case IProblem.FieldHidingField:
			case IProblem.DuplicateField:
				name= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_hiding_field_label, BasicElementLabels.getJavaElementName(nameNode.getIdentifier()));
				break;
			case IProblem.ArgumentHidingLocalVariable:
			case IProblem.ArgumentHidingField:
				name= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_hiding_argument_label, BasicElementLabels.getJavaElementName(nameNode.getIdentifier()));
				break;
			case IProblem.DuplicateMethod:
				name= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_renaming_duplicate_method, BasicElementLabels.getJavaElementName(nameNode.getIdentifier()));
				break;

			default:
				name= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_rename_var_label, BasicElementLabels.getJavaElementName(nameNode.getIdentifier()));
		}

		if (problem.getProblemId() == IProblem.UseEnumAsAnIdentifier) {
			valueSuggestion= "enumeration"; //$NON-NLS-1$
		} else {
			valueSuggestion= nameNode.getIdentifier() + '1';
		}

		LinkedNamesAssistProposalCore proposalCore= new LinkedNamesAssistProposalCore(name, context, nameNode, valueSuggestion);
		proposals.add(linkedNamesAssistProposalToT(proposalCore));
		if(nameNode.getParent() instanceof VariableDeclaration) {
			LinkedOpenDeclarationProposalCore showExisting= new LinkedOpenDeclarationProposalCore(CorrectionMessages.OpenExistingDeclaration,context, nameNode);
			proposals.add(LinkedNamesAssistShowDuplicateProposalToT(showExisting));
		}
	}

	private void addRemoveProposal(IInvocationContext context, ASTNode selectedNode, Collection<T> proposals) {
		ASTRewrite rewrite= ASTRewrite.create(selectedNode.getAST());
		rewrite.remove(selectedNode, null);

		String label= CorrectionMessages.LocalCorrectionsSubProcessor_removeunreachablecode_description;
		addRemoveProposal(context, rewrite, label, proposals);
	}

	private void addRemoveProposal(IInvocationContext context, ASTRewrite rewrite, String label, Collection<T> proposals) {
		ASTRewriteCorrectionProposalCore proposal= new ASTRewriteCorrectionProposalCore(label, context.getCompilationUnit(), rewrite, 10);
		proposals.add(astRewriteCorrectionProposalToT(proposal, DELETE_ID));
	}

	public void getServiceProviderProposal(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) throws CoreException {
		ASTNode node= problem.getCoveredNode(context.getASTRoot());
		if (!(node instanceof Name) || !(node.getParent() instanceof ProvidesDirective)) {
			return;
		}

		Name name= (Name) node;
		ProvidesDirective prov= (ProvidesDirective) name.getParent();
		ITypeBinding targetBinding= name.resolveTypeBinding();
		ITypeBinding serviceBinding= prov.getName().resolveTypeBinding();
		if (targetBinding != null && serviceBinding != null) {
			ICompilationUnit targetCU= ASTResolving.findCompilationUnitForBinding(context.getCompilationUnit(), context.getASTRoot(), targetBinding);

			IJavaProject proj= context.getCompilationUnit().getJavaProject();
			IType type= proj.findType(serviceBinding.getQualifiedName());
			NewProviderMethodDeclarationCore proposal= new NewProviderMethodDeclarationCore(
					Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_add_provider_method_description, type.getElementName()),
					targetCU, context.getASTRoot(), targetBinding,
					IProposalRelevance.CREATE_METHOD, type);
			proposals.add(newProviderMethodDeclarationProposalToT(proposal, MISC_PUBLIC_ID));
		}
	}

	public void getUnusedObjectAllocationProposalsBase(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		CompilationUnit root= context.getASTRoot();
		AST ast= root.getAST();
		ASTNode selectedNode= problem.getCoveringNode(root);
		if (selectedNode == null) {
			return;
		}

		ASTNode parent= selectedNode.getParent();

		if (parent instanceof ExpressionStatement) {
			ExpressionStatement expressionStatement= (ExpressionStatement) parent;
			Expression expr= expressionStatement.getExpression();
			ITypeBinding exprType= expr.resolveTypeBinding();

			if (exprType != null && Bindings.isSuperType(ast.resolveWellKnownType("java.lang.Throwable"), exprType)) { //$NON-NLS-1$
				ASTRewrite rewrite= ASTRewrite.create(ast);
				TightSourceRangeComputer sourceRangeComputer= new TightSourceRangeComputer();
				rewrite.setTargetSourceRangeComputer(sourceRangeComputer);

				ThrowStatement throwStatement= ast.newThrowStatement();
				throwStatement.setExpression((Expression) rewrite.createMoveTarget(expr));
				sourceRangeComputer.addTightSourceNode(expressionStatement);
				rewrite.replace(expressionStatement, throwStatement, null);

				String label= CorrectionMessages.LocalCorrectionsSubProcessor_throw_allocated_description;
				LinkedCorrectionProposalCore proposal= new LinkedCorrectionProposalCore(label, context.getCompilationUnit(), rewrite, IProposalRelevance.THROW_ALLOCATED_OBJECT);
				proposal.setEndPosition(rewrite.track(throwStatement));
				proposals.add(linkedCorrectionProposalToT(proposal, CORRECTION_CHANGE_ID));
			}

			MethodDeclaration method= ASTResolving.findParentMethodDeclaration(selectedNode);
			if (method != null && !method.isConstructor()) {
				ASTRewrite rewrite= ASTRewrite.create(ast);
				TightSourceRangeComputer sourceRangeComputer= new TightSourceRangeComputer();
				rewrite.setTargetSourceRangeComputer(sourceRangeComputer);

				ReturnStatement returnStatement= ast.newReturnStatement();
				returnStatement.setExpression((Expression) rewrite.createMoveTarget(expr));
				sourceRangeComputer.addTightSourceNode(expressionStatement);
				rewrite.replace(expressionStatement, returnStatement, null);

				String label= CorrectionMessages.LocalCorrectionsSubProcessor_return_allocated_description;
				int relevance;
				ITypeBinding returnTypeBinding= method.getReturnType2().resolveBinding();
				if (returnTypeBinding != null && exprType != null && exprType.isAssignmentCompatible(returnTypeBinding)) {
					relevance= IProposalRelevance.RETURN_ALLOCATED_OBJECT_MATCH;
				} else if (method.getReturnType2() instanceof PrimitiveType
						&& ((PrimitiveType) method.getReturnType2()).getPrimitiveTypeCode() == PrimitiveType.VOID) {
					relevance= IProposalRelevance.RETURN_ALLOCATED_OBJECT_VOID;
				} else {
					relevance= IProposalRelevance.RETURN_ALLOCATED_OBJECT;
				}
				LinkedCorrectionProposalCore proposal= new LinkedCorrectionProposalCore(label, context.getCompilationUnit(), rewrite, relevance);
				proposal.setEndPosition(rewrite.track(returnStatement));
				proposals.add(linkedCorrectionProposalToT(proposal, CORRECTION_CHANGE_ID));
			}

			{
				ASTRewrite rewrite= ASTRewrite.create(ast);
				rewrite.remove(parent, null);

				String label= CorrectionMessages.LocalCorrectionsSubProcessor_remove_allocated_description;
				ASTRewriteCorrectionProposalCore proposal= new ASTRewriteCorrectionProposalCore(label, context.getCompilationUnit(), rewrite, IProposalRelevance.REMOVE_UNUSED_ALLOCATED_OBJECT);
				proposals.add(astRewriteCorrectionProposalToT(proposal, DELETE_ID));
			}

		}
		getAssignToVariableProposalsBase(context, selectedNode, null, proposals);
	}

	public boolean getAssignToVariableProposalsBase(IInvocationContext context, ASTNode node, IProblemLocation[] locations, Collection<T> resultingCollections) {
		// don't add if already added as quick fix
		if (containsMatchingProblem(locations, IProblem.ParsingErrorInsertToComplete))
			return false;
		Statement statement= ASTResolving.findParentStatement(node);
		if (!(statement instanceof ExpressionStatement)) {
			return false;
		}
		ExpressionStatement expressionStatement= (ExpressionStatement) statement;

		Expression expression= expressionStatement.getExpression();
		if (expression.getNodeType() == ASTNode.ASSIGNMENT) {
			return false; // too confusing and not helpful
		}

		ITypeBinding typeBinding= expression.resolveTypeBinding();
		typeBinding= Bindings.normalizeTypeBinding(typeBinding);
		if (typeBinding == null) {
			return false;
		}
		if (resultingCollections == null) {
			return true;
		}

		// don't add if already added as quick fix
		if (containsMatchingProblem(locations, IProblem.UnusedObjectAllocation))
			return false;

		ICompilationUnit cu= context.getCompilationUnit();

		AssignToVariableAssistProposalCore localProposal= new AssignToVariableAssistProposalCore(cu, AssignToVariableAssistProposalCore.LOCAL, expressionStatement, typeBinding,
				IProposalRelevance.ASSIGN_TO_LOCAL, false);
		localProposal.setCommandId(ASSIGN_TO_LOCAL_ID);
		resultingCollections.add(assignToVariableAssistProposalToT(localProposal));

		if (QuickAssistProcessorUtil.isAutoClosable(typeBinding)) {
			AssignToVariableAssistProposalCore tryWithResourcesProposal= new AssignToVariableAssistProposalCore(cu, AssignToVariableAssistProposalCore.TRY_WITH_RESOURCES, expressionStatement, typeBinding,
					IProposalRelevance.ASSIGN_IN_TRY_WITH_RESOURCES, false);
			tryWithResourcesProposal.setCommandId(ASSIGN_IN_TRY_WITH_RESOURCES_ID);
			resultingCollections.add(assignToVariableAssistProposalToT(tryWithResourcesProposal));
		}

		ASTNode type= ASTResolving.findParentType(expression);
		if (type != null) {
			AssignToVariableAssistProposalCore fieldProposal= new AssignToVariableAssistProposalCore(cu, AssignToVariableAssistProposalCore.FIELD, expressionStatement, typeBinding,
					IProposalRelevance.ASSIGN_TO_FIELD, false);
			fieldProposal.setCommandId(ASSIGN_TO_FIELD_ID);
			resultingCollections.add(assignToVariableAssistProposalToT(fieldProposal));
		}
		return true;
	}

	private static boolean containsMatchingProblem(IProblemLocation[] locations, int problemId) {
		if (locations != null) {
			for (IProblemLocation location : locations) {
				if (IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER.equals(location.getMarkerType())
						&& location.getProblemId() == problemId) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean getGenerateForLoopProposalsBase(IInvocationContext context, ASTNode coveringNode, @SuppressWarnings("unused") IProblemLocation[] locations, Collection<T> resultingCollections) {
//		if (containsMatchingProblem(locations, IProblem.ParsingErrorInsertToComplete))
//			return false;

		Statement statement= ASTResolving.findParentStatement(coveringNode);
		if (!(statement instanceof ExpressionStatement)) {
			return false;
		}

		ExpressionStatement expressionStatement= (ExpressionStatement) statement;
		Expression expression= expressionStatement.getExpression();
		if (expression instanceof Assignment) {
			Assignment assignment= (Assignment) expression;
			Expression leftHandSide= assignment.getLeftHandSide();
			if (leftHandSide instanceof FieldAccess && leftHandSide.getStartPosition() == assignment.getStartPosition() && leftHandSide.getLength() == assignment.getLength()) {
				// "this.fieldname" recovered as "this.fieldname = $missing$"
				expression= leftHandSide;
			}
		}
		ITypeBinding expressionType= null;
		if (expression instanceof MethodInvocation
				|| expression instanceof SimpleName
				|| expression instanceof FieldAccess
				|| expression instanceof QualifiedName) {
			expressionType= expression.resolveTypeBinding();
		} else {
			return false;
		}

		if (expressionType == null)
			return false;

		ICompilationUnit cu= context.getCompilationUnit();
		if (Bindings.findTypeInHierarchy(expressionType, "java.lang.Iterable") != null) { //$NON-NLS-1$
			if (resultingCollections == null)
				return true;
			GenerateForLoopAssistProposalCore proposal= new GenerateForLoopAssistProposalCore(cu, expressionStatement, GenerateForLoopAssistProposalCore.GENERATE_ITERATOR_FOR);
			resultingCollections.add(generateForLoopAssistProposalToT(proposal));
			if (Bindings.findTypeInHierarchy(expressionType, "java.util.List") != null) { //$NON-NLS-1$
				proposal= new GenerateForLoopAssistProposalCore(cu, expressionStatement, GenerateForLoopAssistProposalCore.GENERATE_ITERATE_LIST);
				resultingCollections.add(generateForLoopAssistProposalToT(proposal));
			}
		} else if (expressionType.isArray()) {
			if (resultingCollections == null)
				return true;
			GenerateForLoopAssistProposalCore proposal= new GenerateForLoopAssistProposalCore(cu, expressionStatement, GenerateForLoopAssistProposalCore.GENERATE_ITERATE_ARRAY);
			resultingCollections.add(generateForLoopAssistProposalToT(proposal));
		} else {
			return false;
		}
		GenerateForLoopAssistProposalCore proposal= new GenerateForLoopAssistProposalCore(cu, expressionStatement, GenerateForLoopAssistProposalCore.GENERATE_FOREACH);
		resultingCollections.add(generateForLoopAssistProposalToT(proposal));

		return true;
	}


	public void getInvalidOperatorProposalsBase(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		CompilationUnit root= context.getASTRoot();
		AST ast= root.getAST();

		ASTNode selectedNode= ASTNodes.getUnparenthesedExpression(problem.getCoveringNode(root));

		if (selectedNode instanceof PrefixExpression) {
			// !x instanceof X -> !(x instanceof X)

			PrefixExpression expression= (PrefixExpression) selectedNode;
			if (expression.getOperator() == PrefixExpression.Operator.NOT) {
				ASTNode parent= expression.getParent();

				String label= null;
				switch (parent.getNodeType()) {
					case ASTNode.INSTANCEOF_EXPRESSION:
						label= CorrectionMessages.LocalCorrectionsSubProcessor_setparenteses_instanceof_description;
						break;
					case ASTNode.INFIX_EXPRESSION:
						InfixExpression infixExpression= (InfixExpression) parent;
						label= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_setparenteses_description, infixExpression.getOperator().toString());
						break;
				}

				if (label != null) {
					ASTRewrite rewrite= ASTRewrite.create(ast);
					rewrite.replace(selectedNode, rewrite.createMoveTarget(expression.getOperand()), null);

					ParenthesizedExpression newParentExpr= ast.newParenthesizedExpression();
					newParentExpr.setExpression((Expression) rewrite.createMoveTarget(parent));
					PrefixExpression newPrefixExpr= ast.newPrefixExpression();
					newPrefixExpr.setOperand(newParentExpr);
					newPrefixExpr.setOperator(PrefixExpression.Operator.NOT);

					rewrite.replace(parent, newPrefixExpr, null);

					ASTRewriteCorrectionProposalCore proposal= new ASTRewriteCorrectionProposalCore(label, context.getCompilationUnit(), rewrite, IProposalRelevance.INVALID_OPERATOR);
					proposals.add(astRewriteCorrectionProposalToT(proposal, INVALID_OPERATOR));
				}
			}
		} else if (selectedNode instanceof InfixExpression && isBitOperation((((InfixExpression) selectedNode).getOperator()))) {
			// a & b == c -> (a & b) == c
			final CompareInBitWiseOpFinder opFinder= new CompareInBitWiseOpFinder(selectedNode);
			if (opFinder.getCompareExpression() != null) { // compare operation inside bit operations: set parents
				String label= CorrectionMessages.LocalCorrectionsSubProcessor_setparenteses_bitop_description;
				CUCorrectionProposalCore proposal= new CUCorrectionProposalCore(label, context.getCompilationUnit(), IProposalRelevance.INVALID_OPERATOR) {
					@Override
					public void addEdits(IDocument document, TextEdit edit) throws CoreException {
						InfixExpression compareExpression= opFinder.getCompareExpression();
						InfixExpression expression= opFinder.getParentInfixExpression();
						ASTNode left= compareExpression.getLeftOperand();
						if (expression.getStartPosition() < left.getStartPosition()) {
							edit.addChild(new InsertEdit(expression.getStartPosition(), String.valueOf('(')));
							edit.addChild(new InsertEdit(ASTNodes.getExclusiveEnd(left), String.valueOf(')')));
						}
						ASTNode rigth= compareExpression.getRightOperand();
						int selEnd= ASTNodes.getExclusiveEnd(expression);
						if (selEnd > ASTNodes.getExclusiveEnd(rigth)) {
							edit.addChild(new InsertEdit(rigth.getStartPosition(), String.valueOf('(')));
							edit.addChild(new InsertEdit(selEnd, String.valueOf(')')));
						}
					}
				};
				proposals.add(cuCorrectionProposalToT(proposal, INVALID_OPERATOR));
			}
		}
	}

	public void getUnnecessaryThrownExceptionProposal(IInvocationContext context, IProblemLocation problem,
			Collection<T> proposals, JavadocTagsBaseSubProcessor<T> javadocProcessor) {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		selectedNode= ASTNodes.getNormalizedNode(selectedNode);
		if (selectedNode == null || selectedNode.getLocationInParent() != MethodDeclaration.THROWN_EXCEPTION_TYPES_PROPERTY) {
			return;
		}
		MethodDeclaration decl= (MethodDeclaration) selectedNode.getParent();
		IMethodBinding binding= decl.resolveBinding();
		if (binding != null) {
			List<Type> thrownExceptions= decl.thrownExceptionTypes();
			int index= thrownExceptions.indexOf(selectedNode);
			if (index == -1) {
				return;
			}
			ChangeDescription[] desc= new ChangeDescription[thrownExceptions.size()];
			desc[index]= new RemoveDescription();

			ICompilationUnit cu= context.getCompilationUnit();
			String label= CorrectionMessages.LocalCorrectionsSubProcessor_unnecessarythrow_description;

			ChangeMethodSignatureProposalCore proposal= new ChangeMethodSignatureProposalCore(label, cu, selectedNode, binding, null, desc, IProposalRelevance.UNNECESSARY_THROW);
			proposals.add(changeMethodSignatureProposalToT(proposal, 0));
		}
		javadocProcessor.addUnusedAndUndocumentedParameterOrExceptionProposals(context, problem, proposals);
	}

	public void getUnusedMemberProposal(IInvocationContext context, IProblemLocation problem,
			Collection<T> proposals, JavadocTagsBaseSubProcessor<T> javadocProcessor,
			GetterSetterCorrectionBaseSubProcessor<T> getterProcessor) {
		int problemId= problem.getProblemId();
		if (JavaModelUtil.is22OrHigher(context.getCompilationUnit().getJavaProject()) &&
				(problemId == IProblem.LocalVariableIsNeverUsed || problemId == IProblem.LambdaParameterIsNeverUsed)) {
			RenameUnusedVariableFixCore fix= RenameUnusedVariableFixCore.createRenameToUnnamedFix(context.getASTRoot(), problem);
			if (fix != null) {
				addRenameProposal(context, proposals, fix);
				return;
			}
		}
		if (problemId == IProblem.LambdaParameterIsNeverUsed) {
			return;
		}
		UnusedCodeFixCore fix= UnusedCodeFixCore.createUnusedMemberFix(context.getASTRoot(), problem, false);
		if (fix != null) {
			addProposal(context, proposals, fix);
		}

		if (problemId == IProblem.LocalVariableIsNeverUsed) {
			fix= UnusedCodeFixCore.createUnusedMemberFix(context.getASTRoot(), problem, true);
			addProposal(context, proposals, fix);
		}

		if (problemId == IProblem.ArgumentIsNeverUsed) {
			addProposal(context, proposals, UnusedCodeFixCore.createUnusedParameterFix(context.getASTRoot(), problem));
			javadocProcessor.addUnusedAndUndocumentedParameterOrExceptionProposals(context, problem, proposals);
		}

		if (problemId == IProblem.UnusedPrivateField) {
			getterProcessor.addGetterSetterProposals(context, problem, proposals, IProposalRelevance.GETTER_SETTER_UNUSED_PRIVATE_FIELD);
		}
	}

	public void getUnusedTypeParameterProposal(IInvocationContext context, IProblemLocation problemLoc,
			Collection<T> proposals, JavadocTagsBaseSubProcessor<T> javadocProcessor) {
		UnusedCodeFixCore fix= UnusedCodeFixCore.createUnusedTypeParameterFix(context.getASTRoot(), problemLoc);
		if (fix != null) {
			addProposal(context, proposals, fix);
		}
		javadocProcessor.addUnusedAndUndocumentedParameterOrExceptionProposals(context, problemLoc, proposals);
	}

	private void addProposal(IInvocationContext context, Collection<T> proposals, final UnusedCodeFixCore fix) {
		if (fix != null) {
			FixCorrectionProposalCore proposal= new FixCorrectionProposalCore(fix, fix.getCleanUp(), IProposalRelevance.UNUSED_MEMBER, context);
			proposals.add(fixCorrectionProposalToT(proposal, DELETE_ID));
		}
	}

	private void addRenameProposal(IInvocationContext context, Collection<T> proposals, final RenameUnusedVariableFixCore fix) {
		if (fix != null) {
			FixCorrectionProposalCore proposal= new FixCorrectionProposalCore(fix, fix.getCleanUp(), IProposalRelevance.UNUSED_MEMBER, context);
			proposals.add(fixCorrectionProposalToT(proposal, RENAME_ID));
		}
	}

	public void getCorrectAccessToStaticProposals(IInvocationContext context, IProblemLocation problem,
			Collection<T> proposals, ModifierCorrectionSubProcessorCore<T> modifierProcessor) throws CoreException {
		IProposableFix fix= CodeStyleFixCore.createIndirectAccessToStaticFix(context.getASTRoot(), problem);

		if (fix != null) {
			Map<String, String> options= new HashMap<>();
			options.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS, CleanUpOptions.TRUE);
			options.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS, CleanUpOptions.TRUE);
			FixCorrectionProposalCore proposal= new FixCorrectionProposalCore(fix, new CodeStyleCleanUpCore(options), IProposalRelevance.CREATE_INDIRECT_ACCESS_TO_STATIC, context);
			proposal.setCommandId(ADD_STATIC_ACCESS_ID);
			proposals.add(fixCorrectionProposalToT(proposal, ADD_STATIC_ACCESS));
			return;
		}

		IProposableFix[] fixes= CodeStyleFixCore.createNonStaticAccessFixes(context.getASTRoot(), problem);
		if (fixes != null) {
			IProposableFix fix1= fixes[0];
			Map<String, String> options= new HashMap<>();
			options.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS, CleanUpOptions.TRUE);
			options.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS, CleanUpOptions.TRUE);
			FixCorrectionProposalCore proposal= new FixCorrectionProposalCore(fix1, new CodeStyleCleanUpCore(options), IProposalRelevance.CREATE_NON_STATIC_ACCESS_USING_DECLARING_TYPE, context);
			proposal.setCommandId(ADD_STATIC_ACCESS_ID);
			proposals.add(fixCorrectionProposalToT(proposal, ADD_STATIC_ACCESS));

			if (fixes.length > 1) {
				Map<String, String> options1= new HashMap<>();
				options1.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS, CleanUpOptions.TRUE);
				options1.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS, CleanUpOptions.TRUE);
				options1.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS, CleanUpOptions.TRUE);
				IProposableFix fix2= fixes[1];
				proposal= new FixCorrectionProposalCore(fix2, new CodeStyleCleanUpCore(options1), IProposalRelevance.CREATE_NON_STATIC_ACCESS_USING_INSTANCE_TYPE, context);
				proposals.add(fixCorrectionProposalToT(proposal, ADD_STATIC_ACCESS));
			}
		}
		modifierProcessor.getNonAccessibleReferenceProposal(context, problem, proposals, ModifierCorrectionSubProcessorCore.TO_NON_STATIC, IProposalRelevance.REMOVE_STATIC_MODIFIER);
	}

	public void getUncaughtExceptionProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();

		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(astRoot);
		if (selectedNode == null) {
			return;
		}
		while (selectedNode != null && !(selectedNode instanceof Statement) && !(selectedNode instanceof VariableDeclarationExpression)
				&& (selectedNode.getLocationInParent() != LambdaExpression.BODY_PROPERTY) && !(selectedNode instanceof MethodReference)) {
			selectedNode= selectedNode.getParent();
		}
		if (selectedNode == null) {
			return;
		}
		boolean isSelectedNodeThrowStatement= false;
		if (selectedNode instanceof ThrowStatement) {
			isSelectedNodeThrowStatement= true;
		}

		int offset= selectedNode.getStartPosition();
		int length= selectedNode.getLength();
		int selectionEnd= context.getSelectionOffset() + context.getSelectionLength();
		if (selectionEnd > offset + length) {
			// extend the selection if more than one statement is selected (bug 72149)
			length= selectionEnd - offset;
		}

		//Surround with proposals
		SurroundWithTryCatchRefactoring refactoring= SurroundWithTryCatchRefactoring.create(cu, offset, length);
		if (refactoring == null)
			return;

		List<String> affectedLocals= new ArrayList<>();
		SimpleName vName= null;
		ITypeBinding vType= null;
		if (selectedNode.getAST().apiLevel() >= ASTHelper.JLS10 && (selectedNode instanceof VariableDeclarationStatement)) {
			for (Object o : ((VariableDeclarationStatement) selectedNode).fragments()) {
				VariableDeclarationFragment v= ((VariableDeclarationFragment) o);
				vName= v.getName();
				vType= ((VariableDeclarationStatement) selectedNode).getType().resolveBinding();
			}

			// If no references to 'var' type exist, entire statement will be placed in try block
			SurroundWithTryCatchAnalyzer analyzer= new SurroundWithTryCatchAnalyzer(cu, Selection.createFromStartLength(offset, length));
			astRoot.accept(analyzer);
			affectedLocals= Arrays.asList(analyzer.getAffectedLocals()).stream().map(f -> f.getName().getIdentifier()).collect(Collectors.toList());
		}

		refactoring.setLeaveDirty(true);
		if (refactoring.checkActivationBasics(astRoot).isOK() && !isSelectedNodeThrowStatement) {
			String label;
			if ((vType != null) && (vName != null) && ASTNodes.isVarType(selectedNode, astRoot) && affectedLocals.contains(vName.getIdentifier())) {
				label= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_surroundwith_trycatch_var_description, new Object[] { vName.getIdentifier(), vType.getName() });
			} else {
				label= CorrectionMessages.LocalCorrectionsSubProcessor_surroundwith_trycatch_description;
			}
			RefactoringCorrectionProposalCore proposal= new RefactoringCorrectionProposalCore(label, cu, refactoring, IProposalRelevance.SURROUND_WITH_TRY_CATCH);
			proposal.setLinkedProposalModel(refactoring.getLinkedProposalModel());
			proposals.add(refactoringCorrectionProposalToT(proposal, SURROUND_WITH_TRY_CATCH));
		}

		refactoring= SurroundWithTryCatchRefactoring.create(cu, offset, length, true);
		if (refactoring == null)
			return;

		refactoring.setLeaveDirty(true);
		if (refactoring.checkActivationBasics(astRoot).isOK()) {
			String label;
			if ((vType != null) && (vName != null) && ASTNodes.isVarType(selectedNode, astRoot) && affectedLocals.contains(vName.getIdentifier())) {
				label= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_surroundwith_trymulticatch_var_description, new Object[] { vName.getIdentifier(), vType.getName() });
			} else {
				label= CorrectionMessages.LocalCorrectionsSubProcessor_surroundwith_trymulticatch_description;
			}
			RefactoringCorrectionProposalCore proposal= new RefactoringCorrectionProposalCore(label, cu, refactoring, IProposalRelevance.SURROUND_WITH_TRY_MULTICATCH);
			proposal.setLinkedProposalModel(refactoring.getLinkedProposalModel());
			proposals.add(refactoringCorrectionProposalToT(proposal, SURROUND_WITH_TRY_MULTI_CATCH));
		}

		//Catch exception
		BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(selectedNode);
		if (decl == null) {
			return;
		}

		ASTNode enclosingNode= SurroundWithAnalyzer.getEnclosingNode(selectedNode);
		if (enclosingNode == null) {
			return;
		}

		ITypeBinding[] uncaughtExceptions= ExceptionAnalyzer.perform(enclosingNode, Selection.createFromStartLength(offset, length));
		if (uncaughtExceptions.length == 0) {
			return;
		}

		TryStatement surroundingTry= ASTResolving.findParentTryStatement(selectedNode);
		AST ast= astRoot.getAST();
		if (surroundingTry != null && (ASTNodes.isParent(selectedNode, surroundingTry.getBody()) || selectedNode.getLocationInParent() == TryStatement.RESOURCES2_PROPERTY)) {
			addAdditionalCatchProposal(context, proposals, cu, selectedNode, offset, length, decl, uncaughtExceptions, surroundingTry, ast);

			List<CatchClause> catchClauses= surroundingTry.catchClauses();
			List<ITypeBinding> filteredExceptions= SurroundWithTryCatchRefactoring.filterSubtypeExceptions(uncaughtExceptions);
			ASTRewrite rewrite= ASTRewrite.create(ast);

			if (catchClauses != null && catchClauses.size() == 1) {
				String label= filteredExceptions.size() > 1
						? CorrectionMessages.LocalCorrectionsSubProcessor_addexceptionstoexistingcatch_description
						: CorrectionMessages.LocalCorrectionsSubProcessor_addexceptiontoexistingcatch_description;
				LinkedCorrectionProposalCore proposal= new LinkedCorrectionProposalCore(label, cu, rewrite, IProposalRelevance.ADD_EXCEPTIONS_TO_EXISTING_CATCH);
				ImportRewrite imports= proposal.createImportRewrite(context.getASTRoot());
				ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(decl, imports);

				CatchClause catchClause= catchClauses.get(0);
				Type originalType= catchClause.getException().getType();

				if (originalType instanceof UnionType) {
					UnionType unionType= (UnionType) originalType;
					ListRewrite listRewrite= rewrite.getListRewrite(unionType, UnionType.TYPES_PROPERTY);
					@SuppressWarnings("unchecked")
					List<Type> existingTypes= new ArrayList<>(unionType.types());

					for (int i= 0; i < filteredExceptions.size(); i++) {
						Type addedType= addNewException(ast, filteredExceptions, rewrite, proposal, imports, importRewriteContext, i);
						boolean isReplaced= false;

						for (Type existingType : existingTypes) {
							if (existingType.resolveBinding().isSubTypeCompatible(filteredExceptions.get(i))) {
								listRewrite.replace(existingType, addedType, null);
								isReplaced= true;
								break;
							}
						}

						if (!isReplaced) {
							listRewrite.insertLast(addedType, null);
						}
					}
				} else {
					Type firstType= null;
					List<Type> typesToAdd= new ArrayList<>();

					for (int i= 0; i < filteredExceptions.size(); i++) {
						Type addedType= addNewException(ast, filteredExceptions, rewrite, proposal, imports, importRewriteContext, i);

						if (originalType.resolveBinding().isSubTypeCompatible(filteredExceptions.get(i))) {
							firstType= addedType;
						} else {
							typesToAdd.add(addedType);
						}
					}

					if (!typesToAdd.isEmpty()) {
						UnionType newUnionType= ast.newUnionType();
						List<Type> types= newUnionType.types();

						if (firstType == null) {
							types.add(ASTNodes.createMoveTarget(rewrite, originalType));
						} else {
							types.add(firstType);
						}
						types.addAll(typesToAdd);

						rewrite.replace(originalType, newUnionType, null);
					} else if (firstType != null) {
						rewrite.replace(originalType, firstType, null);
					}
				}

				proposals.add(linkedCorrectionProposalToT(proposal, ADD_EXCEPTION_TO_CATCH));
			} else if (catchClauses != null && catchClauses.isEmpty() && filteredExceptions.size() > 1) {
				String label= CorrectionMessages.LocalCorrectionsSubProcessor_addadditionalmulticatch_description;
				LinkedCorrectionProposalCore proposal= new LinkedCorrectionProposalCore(label, cu, rewrite, IProposalRelevance.ADD_EXCEPTIONS_TO_EXISTING_CATCH);
				ImportRewrite imports= proposal.createImportRewrite(context.getASTRoot());
				ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(decl, imports);

				CodeScopeBuilder.Scope scope= CodeScopeBuilder.perform(decl, Selection.createFromStartLength(offset, length)).findScope(offset, length);
				scope.setCursor(offset);

				CatchClause newCatchClause= ast.newCatchClause();
				String varName= StubUtility.getExceptionVariableName(cu.getJavaProject());
				String name= scope.createName(varName, false);
				SingleVariableDeclaration var= ast.newSingleVariableDeclaration();
				var.setName(ast.newSimpleName(name));

				UnionType newUnionType= ast.newUnionType();
				List<Type> types= newUnionType.types();

				for (int i= 0; i < filteredExceptions.size(); i++) {
					types.add(addNewException(ast, filteredExceptions, rewrite, proposal, imports, importRewriteContext, i));
				}

				String nameKey= "name"; //$NON-NLS-1$
				proposal.addLinkedPosition(rewrite.track(var.getName()), false, nameKey);
				var.setType(newUnionType);
				newCatchClause.setException(var);
				String catchBody= StubUtility.getCatchBodyContent(cu, "Exception", name, selectedNode, String.valueOf('\n')); //$NON-NLS-1$

				if (catchBody != null) {
					ASTNode node= rewrite.createStringPlaceholder(catchBody, ASTNode.RETURN_STATEMENT);
					newCatchClause.getBody().statements().add(node);
				}

				ListRewrite listRewrite= rewrite.getListRewrite(surroundingTry, TryStatement.CATCH_CLAUSES_PROPERTY);
				listRewrite.insertFirst(newCatchClause, null);
				proposals.add(linkedCorrectionProposalToT(proposal, ADD_EXCEPTION_TO_MULTI_CATCH));
			}
		}

		//Add throws declaration
		if (enclosingNode instanceof MethodDeclaration) {
			MethodDeclaration methodDecl= (MethodDeclaration) enclosingNode;
			IMethodBinding binding= methodDecl.resolveBinding();
			boolean isApplicable= binding != null;

			if (isApplicable) {
				IMethodBinding overriddenMethod= Bindings.findOverriddenMethod(binding, true);

				if (overriddenMethod != null) {
					isApplicable= overriddenMethod.getDeclaringClass().isFromSource();

					if (!isApplicable) { // bug 349051
						ITypeBinding[] exceptionTypes= overriddenMethod.getExceptionTypes();
						ArrayList<ITypeBinding> unhandledExceptions= new ArrayList<>(uncaughtExceptions.length);

						for (ITypeBinding curr : uncaughtExceptions) {
							if (isSubtype(curr, exceptionTypes)) {
								unhandledExceptions.add(curr);
							}
						}

						uncaughtExceptions= unhandledExceptions.toArray(new ITypeBinding[unhandledExceptions.size()]);
						isApplicable= uncaughtExceptions.length > 0;
					}
				}

				if (isApplicable && binding != null) {
					ITypeBinding[] methodExceptions= binding.getExceptionTypes();
					ArrayList<ITypeBinding> unhandledExceptions= new ArrayList<>(uncaughtExceptions.length);

					for (ITypeBinding curr : uncaughtExceptions) {
						if (!isSubtype(curr, methodExceptions)) {
							unhandledExceptions.add(curr);
						}
					}

					uncaughtExceptions= unhandledExceptions.toArray(new ITypeBinding[unhandledExceptions.size()]);

					List<Type> exceptions= methodDecl.thrownExceptionTypes();
					int nExistingExceptions= exceptions.size();

					ChangeDescription[] desc= new ChangeDescription[nExistingExceptions + uncaughtExceptions.length];
					for (int i= 0; i < exceptions.size(); i++) {
						Type elem= exceptions.get(i);

						if (isSubtype(elem.resolveBinding(), uncaughtExceptions)) {
							desc[i]= new RemoveDescription();
						}
					}

					for (int i= 0; i < uncaughtExceptions.length; i++) {
						desc[i + nExistingExceptions]= new InsertDescription(uncaughtExceptions[i], ""); //$NON-NLS-1$
					}

					String label= CorrectionMessages.LocalCorrectionsSubProcessor_addthrows_description;

					ChangeMethodSignatureProposalCore proposal= new ChangeMethodSignatureProposalCore(label, cu, astRoot, binding, null, desc, IProposalRelevance.ADD_THROWS_DECLARATION);

					for (int i= 0; i < uncaughtExceptions.length; i++) {
						addExceptionTypeLinkProposals(proposal, uncaughtExceptions[i], proposal.getExceptionTypeGroupId(i + nExistingExceptions));
					}

					proposal.setCommandId(ADD_EXCEPTION_TO_THROWS_ID);
					proposals.add(changeMethodSignatureProposalToT(proposal, ADD_THROWS));
				}
			}
		}
	}

	private Type addNewException(AST ast, List<ITypeBinding> filteredExceptions, ASTRewrite rewrite, LinkedCorrectionProposalCore proposal, ImportRewrite imports,
			ImportRewriteContext importRewriteContext, int i) {
		ITypeBinding excBinding= filteredExceptions.get(i);
		Type type= imports.addImport(excBinding, ast, importRewriteContext, TypeLocation.EXCEPTION);

		String typeKey= "type" + i; //$NON-NLS-1$
		proposal.addLinkedPosition(rewrite.track(type), false, typeKey);
		addExceptionTypeLinkProposals(proposal, excBinding, typeKey);

		return type;
	}

	private void addAdditionalCatchProposal(IInvocationContext context, Collection<T> proposals, ICompilationUnit cu, ASTNode selectedNode, int offset, int length,
			BodyDeclaration decl,
			ITypeBinding[] uncaughtExceptions, TryStatement surroundingTry, AST ast) throws CoreException {
		ASTRewrite rewrite= ASTRewrite.create(surroundingTry.getAST());

		String label= CorrectionMessages.LocalCorrectionsSubProcessor_addadditionalcatch_description;
//		Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
		LinkedCorrectionProposalCore proposal= new LinkedCorrectionProposalCore(label, cu, rewrite, IProposalRelevance.ADD_ADDITIONAL_CATCH);

		ImportRewrite imports= proposal.createImportRewrite(context.getASTRoot());
		ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(decl, imports);

		CodeScopeBuilder.Scope scope= CodeScopeBuilder.perform(decl, Selection.createFromStartLength(offset, length)).findScope(offset, length);
		scope.setCursor(offset);

		ListRewrite clausesRewrite= rewrite.getListRewrite(surroundingTry, TryStatement.CATCH_CLAUSES_PROPERTY);
		for (int i= 0; i < uncaughtExceptions.length; i++) {
			ITypeBinding excBinding= uncaughtExceptions[i];
			String varName= StubUtility.getExceptionVariableName(cu.getJavaProject());
			String name= scope.createName(varName, false);
			SingleVariableDeclaration var= ast.newSingleVariableDeclaration();
			var.setName(ast.newSimpleName(name));
			var.setType(imports.addImport(excBinding, ast, importRewriteContext, TypeLocation.EXCEPTION));
			CatchClause newClause= ast.newCatchClause();
			newClause.setException(var);
			String catchBody= StubUtility.getCatchBodyContent(cu, excBinding.getName(), name, selectedNode, String.valueOf('\n'));
			if (catchBody != null) {
				ASTNode node= rewrite.createStringPlaceholder(catchBody, ASTNode.RETURN_STATEMENT);
				newClause.getBody().statements().add(node);
			}
			clausesRewrite.insertLast(newClause, null);

			String typeKey= "type" + i; //$NON-NLS-1$
			String nameKey= "name" + i; //$NON-NLS-1$
			proposal.addLinkedPosition(rewrite.track(var.getType()), false, typeKey);
			proposal.addLinkedPosition(rewrite.track(var.getName()), false, nameKey);
			addExceptionTypeLinkProposals(proposal, excBinding, typeKey);
		}
		proposals.add(linkedCorrectionProposalToT(proposal, ADD_ADDITIONAL_CATCH));
	}

	private void addExceptionTypeLinkProposals(LinkedCorrectionProposalCore proposal, ITypeBinding exc, String key) {
		// all super classes except Object
		while (exc != null && !"java.lang.Object".equals(exc.getQualifiedName())) { //$NON-NLS-1$
			proposal.addLinkedPositionProposal(key, exc);
			exc= exc.getSuperclass();
		}
	}


	private boolean isSubtype(ITypeBinding curr, ITypeBinding[] addedExceptions) {
		while (curr != null) {
			for (ITypeBinding addedException : addedExceptions) {
				if (curr == addedException) {
					return true;
				}
			}
			curr= curr.getSuperclass();
		}
		return false;
	}


	public void getUninitializedLocalVariableProposal(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		ICompilationUnit cu= context.getCompilationUnit();

		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (!(selectedNode instanceof Name)) {
			return;
		}
		Name name= (Name) selectedNode;
		IBinding binding= name.resolveBinding();
		if (!(binding instanceof IVariableBinding)) {
			return;
		}
		IVariableBinding varBinding= (IVariableBinding) binding;

		CompilationUnit astRoot= context.getASTRoot();
		ASTNode node= astRoot.findDeclaringNode(binding);
		if (node instanceof VariableDeclarationFragment) {
			ASTRewrite rewrite= ASTRewrite.create(node.getAST());

			VariableDeclarationFragment fragment= (VariableDeclarationFragment) node;
			if (fragment.getInitializer() != null) {
				return;
			}
			Expression expression= ASTNodeFactory.newDefaultExpression(astRoot.getAST(), varBinding.getType());
			if (expression == null) {
				return;
			}
			rewrite.set(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, expression, null);

			String label= CorrectionMessages.LocalCorrectionsSubProcessor_uninitializedvariable_description;

			LinkedCorrectionProposalCore proposal= new LinkedCorrectionProposalCore(label, cu, rewrite, IProposalRelevance.INITIALIZE_VARIABLE);
			proposal.addLinkedPosition(rewrite.track(expression), false, "initializer"); //$NON-NLS-1$
			proposals.add(linkedCorrectionProposalToT(proposal, INITIALIZE_VARIABLE));
		}
	}

	public void getConstructorFromSuperclassProposal(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) throws CoreException {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode == null) {
			return;
		}

		TypeDeclaration typeDeclaration= null;
		if (selectedNode.getLocationInParent() == TypeDeclaration.NAME_PROPERTY) {
			typeDeclaration= (TypeDeclaration) selectedNode.getParent();
		} else {
			BodyDeclaration declaration= ASTResolving.findParentBodyDeclaration(selectedNode);
			if (declaration instanceof Initializer && problem.getProblemId() == IProblem.UnhandledExceptionInDefaultConstructor) {
				getUncaughtExceptionProposals(context, problem, proposals);
			}
			return;
		}

		ITypeBinding binding= typeDeclaration.resolveBinding();
		if (binding == null || binding.getSuperclass() == null) {
			return;
		}
		ICompilationUnit cu= context.getCompilationUnit();
		for (IMethodBinding curr : binding.getSuperclass().getDeclaredMethods()) {
			if (curr.isConstructor() && !Modifier.isPrivate(curr.getModifiers())) {
				proposals.add(constructorFromSuperClassProposalToT(new ConstructorFromSuperclassProposalCore(cu, typeDeclaration, curr, IProposalRelevance.ADD_CONSTRUCTOR_FROM_SUPER_CLASS), ADD_CONSTRUCTOR_FROM_SUPERCLASS));
			}
		}
	}

	public void getNewObjectProposal(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) throws CoreException {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode == null) {
			return;
		}
		ASTNode selectedExpressionStatement= ASTNodes.getParent(selectedNode, ASTNode.EXPRESSION_STATEMENT);
		if (selectedExpressionStatement != null) {
			ExpressionStatement expressionStatement= (ExpressionStatement) selectedExpressionStatement;
			Expression expression= expressionStatement.getExpression();
			ITypeBinding binding= null;
			if (expression instanceof MethodInvocation) {
				Expression expression2= ((MethodInvocation) expression).getExpression();
				binding= expression2 == null ? null : expression2.resolveTypeBinding();
			}
			if (binding == null) {
				return;
			}
			ICompilationUnit cu= context.getCompilationUnit();
			CreateNewObjectProposalCore createNewObjectProposal= new CreateNewObjectProposalCore(cu, expressionStatement, binding, IProposalRelevance.ADD_CONSTRUCTOR_FROM_SUPER_CLASS);
			if (createNewObjectProposal.hasProposal()) {
				proposals.add(createNewObjectProposalToT(createNewObjectProposal, ADD_NEW_OBJECT));
			}
			return;
		}

		ASTNode selectedVariableDeclarationFragment= ASTNodes.getParent(selectedNode, ASTNode.VARIABLE_DECLARATION_FRAGMENT);
		if (selectedVariableDeclarationFragment != null) {
			VariableDeclarationFragment vdf= (VariableDeclarationFragment) selectedVariableDeclarationFragment;
			VariableDeclarationStatement vds= (VariableDeclarationStatement) ASTNodes.getParent(selectedNode, ASTNode.VARIABLE_DECLARATION_STATEMENT);
			if (vds == null) {
				return;
			}
			Type type= vds.getType();
			ITypeBinding binding= type == null ? null : type.resolveBinding();
			if (binding == null) {
				return;
			}
			ICompilationUnit cu= context.getCompilationUnit();
			CreateNewObjectProposalCore createNewObjectProposal= new CreateNewObjectProposalCore(cu, vdf, binding, IProposalRelevance.ADD_CONSTRUCTOR_FROM_SUPER_CLASS);
			if (createNewObjectProposal.hasProposal()) {
				proposals.add(createNewObjectProposalToT(createNewObjectProposal, ADD_NEW_OBJECT_2));
			}
			/* create instance of qualifier, ex; X in X.s; */
			Expression initializer= vdf.getInitializer();
			if(initializer instanceof QualifiedName == false) {
				return;
			}
			QualifiedName qualifiedName= (QualifiedName)initializer;
			IBinding resolveBinding= qualifiedName.getName().resolveBinding();
			if(resolveBinding instanceof IVariableBinding == false) {
				return;
			}
			CreateNewObjectProposalCore createNewObjectProposal2= new CreateNewObjectProposalCore(cu, vdf, (IVariableBinding)resolveBinding, IProposalRelevance.ADD_CONSTRUCTOR_FROM_SUPER_CLASS);
			if (createNewObjectProposal2.hasProposal()) {
				proposals.add(createNewObjectProposalToT(createNewObjectProposal2, ADD_NEW_OBJECT_3));
			}
			return;
		}
		if (problem.getProblemId() == IProblem.StaticMethodRequested ||
				problem.getProblemId() == IProblem.NonStaticFieldFromStaticInvocation) {
			getUncaughtExceptionProposals(context, problem, proposals);
		}
	}

	public void getObjectReferenceProposal(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) throws CoreException {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		ITypeBinding binding= null;
		if (selectedNode == null) {
			return;
		}
		if (problem.getProblemId() == IProblem.NonStaticFieldFromStaticInvocation) {
			if (selectedNode instanceof QualifiedName) {
				QualifiedName qualifiedName= (QualifiedName) selectedNode;
				Name qualifier= qualifiedName.getQualifier();
				binding= qualifier.resolveTypeBinding();
			}
		} else {
			selectedNode= ASTNodes.getParent(selectedNode, ASTNode.EXPRESSION_STATEMENT);
			if (selectedNode == null) {
				if (problem.getProblemId() == IProblem.StaticMethodRequested) {
					getUncaughtExceptionProposals(context, problem, proposals);
				}
				return;
			}
			ExpressionStatement expressionStatement= (ExpressionStatement) selectedNode;
			Expression expression= expressionStatement.getExpression();
			if (expression instanceof MethodInvocation) {
				Expression expression2= ((MethodInvocation) expression).getExpression();
				binding= expression2 == null ? null : expression2.resolveTypeBinding();
			}
		}
		if (binding == null) {
			return;
		}
		ICompilationUnit cu= context.getCompilationUnit();
		CreateObjectReferenceProposalCore createObjectReferenceProposal= new CreateObjectReferenceProposalCore(cu, selectedNode, binding, IProposalRelevance.ADD_CONSTRUCTOR_FROM_SUPER_CLASS);
		if (createObjectReferenceProposal.hasProposal()) {
			proposals.add(createObjectReferenceProposalToT(createObjectReferenceProposal, CREATE_OBJECT_REFERENCE));
		}
	}

	public void getVariableReferenceProposal(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) throws CoreException {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode == null) {
			return;
		}
		selectedNode= ASTNodes.getParent(selectedNode, ASTNode.VARIABLE_DECLARATION_FRAGMENT);
		if (selectedNode == null) {
			if (problem.getProblemId() == IProblem.NonStaticFieldFromStaticInvocation) {
				getUncaughtExceptionProposals(context, problem, proposals);
			}
			return;
		}
		VariableDeclarationFragment fragment= (VariableDeclarationFragment) selectedNode;
		Type type= null;
		if(fragment.getParent() instanceof VariableDeclarationStatement) {
			type= ((VariableDeclarationStatement)fragment.getParent()).getType();
		} else if(fragment.getParent() instanceof FieldDeclaration) {
			type= ((FieldDeclaration)fragment.getParent()).getType();
		}
		if (type == null) {
			return;
		}
		ITypeBinding binding= type.resolveBinding();
		if (binding == null) {
			return;
		}
		ICompilationUnit cu= context.getCompilationUnit();
		CreateVariableReferenceProposalCore createVariableReferenceProposal= new CreateVariableReferenceProposalCore(cu, fragment, binding, IProposalRelevance.ADD_CONSTRUCTOR_FROM_SUPER_CLASS);
		if (createVariableReferenceProposal.hasProposal()) {
			proposals.add(createVariableReferenceProposalToT(createVariableReferenceProposal, CREATE_VARIABLE_REFERENCE));
		}
	}

	public void getRedundantSuperInterfaceProposal(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (!(selectedNode instanceof Name)) {
			return;
		}
		ASTNode node= ASTNodes.getNormalizedNode(selectedNode);

		ASTRewrite rewrite= ASTRewrite.create(node.getAST());
		rewrite.remove(node, null);

		String label= CorrectionMessages.LocalCorrectionsSubProcessor_remove_redundant_superinterface;

		ASTRewriteCorrectionProposalCore proposal= new ASTRewriteCorrectionProposalCore(label, context.getCompilationUnit(), rewrite, IProposalRelevance.REMOVE_REDUNDANT_SUPER_INTERFACE);
		proposals.add(astRewriteCorrectionProposalToT(proposal, REMOVE_REDUNDANT_SUPERINTERFACE));

	}

	public void getSuperfluousSemicolonProposal(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		String label= CorrectionMessages.LocalCorrectionsSubProcessor_removesemicolon_description;
		ReplaceCorrectionProposalCore proposal= new ReplaceCorrectionProposalCore(label, context.getCompilationUnit(), problem.getOffset(), problem.getLength(), "", IProposalRelevance.REMOVE_SEMICOLON); //$NON-NLS-1$
		proposals.add(replaceCorrectionProposalToT(proposal, REMOVE_SEMICOLON));
	}

	public void getUnnecessaryCastProposal(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		IProposableFix fix= UnusedCodeFixCore.createRemoveUnusedCastFix(context.getASTRoot(), problem);
		if (fix != null) {
			Map<String, String> options= new Hashtable<>();
			options.put(CleanUpConstants.REMOVE_UNNECESSARY_CASTS, CleanUpOptions.TRUE);
			FixCorrectionProposalCore proposal= new FixCorrectionProposalCore(fix, new UnnecessaryCodeCleanUpCore(options), IProposalRelevance.REMOVE_UNUSED_CAST, context);
			proposals.add(fixCorrectionProposalToT(proposal, REMOVE_UNNECESSARY_CAST));
		}
	}

	public void getUnnecessaryInstanceofProposal(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());

		ASTNode curr= ASTNodes.getUnparenthesedExpression(selectedNode);

		if (curr instanceof InstanceofExpression) {
			AST ast= curr.getAST();

			ASTRewrite rewrite= ASTRewrite.create(ast);

			InstanceofExpression inst= (InstanceofExpression) curr;

			InfixExpression expression= ast.newInfixExpression();
			expression.setLeftOperand((Expression) rewrite.createCopyTarget(inst.getLeftOperand()));
			expression.setOperator(InfixExpression.Operator.NOT_EQUALS);
			expression.setRightOperand(ast.newNullLiteral());

			rewrite.replace(inst, expression, null);

			String label= CorrectionMessages.LocalCorrectionsSubProcessor_unnecessaryinstanceof_description;
			ASTRewriteCorrectionProposalCore proposal= new ASTRewriteCorrectionProposalCore(label, context.getCompilationUnit(), rewrite, IProposalRelevance.UNNECESSARY_INSTANCEOF);
			proposals.add(astRewriteCorrectionProposalToT(proposal, REMOVE_UNNECESSARY_INSTANCEOF));
		}
	}

	public void getIllegalQualifiedEnumConstantLabelProposal(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		ASTNode coveringNode= problem.getCoveringNode(context.getASTRoot());

		ASTNode curr= ASTNodes.getUnparenthesedExpression(coveringNode);

		if (!(curr instanceof QualifiedName)) {
			return;
		}

		SimpleName simpleName= ((QualifiedName) curr).getName();
		final ASTRewrite rewrite= ASTRewrite.create(curr.getAST());
		rewrite.replace(coveringNode, simpleName, null);

		String label= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_replace_with_unqualified_enum_constant, BasicElementLabels.getJavaElementName(simpleName.getIdentifier()));
		ASTRewriteCorrectionProposalCore proposal= new ASTRewriteCorrectionProposalCore(label, context.getCompilationUnit(), rewrite, IProposalRelevance.REPLACE_WITH_UNQUALIFIED_ENUM_CONSTANT);
		proposals.add(astRewriteCorrectionProposalToT(proposal, UNQUALIFY_ENUM_CONSTANT));
	}

	public void getUnqualifiedFieldAccessProposal(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		IProposableFix fix= CodeStyleFixCore.createAddFieldQualifierFix(context.getASTRoot(), problem);
		if (fix != null) {
			Map<String, String> options= new HashMap<>();
			options.put(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS, CleanUpOptions.TRUE);
			options.put(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS, CleanUpOptions.TRUE);
			FixCorrectionProposalCore proposal= new FixCorrectionProposalCore(fix, new CodeStyleCleanUpCore(options), IProposalRelevance.ADD_FIELD_QUALIFIER, context);
			proposal.setCommandId(ADD_FIELD_QUALIFICATION_ID);
			proposals.add(fixCorrectionProposalToT(proposal, UNQUALIFIED_FIELD_ACCESS));
		}
	}

	public void getUnnecessaryElseProposalsBase(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		CompilationUnit root= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(root);
		if (selectedNode == null) {
			return;
		}
		ASTNode parent= selectedNode.getParent();
		if (parent instanceof ExpressionStatement) {
			parent= parent.getParent();
		}
		if (!(parent instanceof IfStatement)) {
			return;
		}
		IfStatement ifStatement= (IfStatement) parent;
		ASTNode ifParent= ifStatement.getParent();
		if (!(ifParent instanceof Block) && !(ifParent instanceof SwitchStatement) && !ASTNodes.isControlStatementBody(ifStatement.getLocationInParent())) {
			return;
		}

		ASTRewrite rewrite= ASTRewrite.create(root.getAST());
		ASTNode placeholder= QuickAssistProcessorUtil.getCopyOfInner(rewrite, ifStatement.getElseStatement(), false);
		if (placeholder == null) {
			return;
		}
		rewrite.remove(ifStatement.getElseStatement(), null);

		if (ifParent instanceof Block) {
			ListRewrite listRewrite= rewrite.getListRewrite(ifParent, Block.STATEMENTS_PROPERTY);
			listRewrite.insertAfter(placeholder, ifStatement, null);
		} else if (ifParent instanceof SwitchStatement) {
			ListRewrite listRewrite= rewrite.getListRewrite(ifParent, SwitchStatement.STATEMENTS_PROPERTY);
			listRewrite.insertAfter(placeholder, ifStatement, null);
		} else {
			Block block= root.getAST().newBlock();
			rewrite.replace(ifStatement, block, null);
			block.statements().add(rewrite.createCopyTarget(ifStatement));
			block.statements().add(placeholder);
		}

		String label= CorrectionMessages.LocalCorrectionsSubProcessor_removeelse_description;
		ASTRewriteCorrectionProposalCore proposal= new ASTRewriteCorrectionProposalCore(label, context.getCompilationUnit(), rewrite, IProposalRelevance.REMOVE_ELSE);
		proposals.add(astRewriteCorrectionProposalToT(proposal, MOVE_ELSE_CLAUSE));
	}


	public void getInterfaceExtendsClassProposalsBase(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		CompilationUnit root= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(root);
		if (selectedNode == null) {
			return;
		}
		while (selectedNode.getParent() instanceof Type) {
			selectedNode= selectedNode.getParent();
		}

		StructuralPropertyDescriptor locationInParent= selectedNode.getLocationInParent();
		if (locationInParent != TypeDeclaration.SUPERCLASS_TYPE_PROPERTY) {
			return;
		}

		TypeDeclaration typeDecl= (TypeDeclaration) selectedNode.getParent();
		{
			ASTRewrite rewrite= ASTRewrite.create(root.getAST());
			ASTNode placeHolder= rewrite.createMoveTarget(selectedNode);
			ListRewrite interfaces= rewrite.getListRewrite(typeDecl, TypeDeclaration.SUPER_INTERFACE_TYPES_PROPERTY);
			interfaces.insertFirst(placeHolder, null);

			String label= CorrectionMessages.LocalCorrectionsSubProcessor_extendstoimplements_description;
			ASTRewriteCorrectionProposalCore proposal= new ASTRewriteCorrectionProposalCore(label, context.getCompilationUnit(), rewrite, IProposalRelevance.CHANGE_EXTENDS_TO_IMPLEMENTS);
			proposals.add(astRewriteCorrectionProposalToT(proposal, CHANGE_EXTENDS_TO_IMPLEMENTS));
		}
		{
			ASTRewrite rewrite= ASTRewrite.create(root.getAST());

			rewrite.set(typeDecl, TypeDeclaration.INTERFACE_PROPERTY, Boolean.TRUE, null);

			String typeName= typeDecl.getName().getIdentifier();
			String label= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_classtointerface_description, BasicElementLabels.getJavaElementName(typeName));
			ASTRewriteCorrectionProposalCore proposal= new ASTRewriteCorrectionProposalCore(label, context.getCompilationUnit(), rewrite, IProposalRelevance.CHANGE_CLASS_TO_INTERFACE);
			proposals.add(astRewriteCorrectionProposalToT(proposal, CHANGE_TO_INTERFACE));
		}
	}

	public void getTypeAsPermittedSubTypeProposal(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		SealedClassFixCore fix= SealedClassFixCore.addTypeAsPermittedSubTypeProposal(context.getASTRoot(), problem);
		if (fix != null) {
			ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
			IType sealedType= SealedClassFixCore.getSealedType(selectedNode);
			ICompilationUnit unit= SealedClassFixCore.getCompilationUnitForSealedType(sealedType);

			CUCorrectionProposalCore proposal= createCorrectionProposalFromCURewriteOperation(unit, fix, fix.getDisplayString(), IProposalRelevance.DECLARE_SEALED_AS_DIRECT_SUPER_TYPE);
			proposals.add(cuCorrectionProposalToT(proposal, ADD_PERMITTED_TYPE));
		}
	}

	public void getSealedAsDirectSuperTypeProposal(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		SealedClassFixCore fix= SealedClassFixCore.addSealedAsDirectSuperTypeProposal(context.getASTRoot(), problem);
		if (fix != null) {
			ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
			IType permittedType= SealedClassFixCore.getPermittedType(selectedNode);
			ICompilationUnit unit= permittedType.getCompilationUnit();

			CUCorrectionProposalCore proposal= createCorrectionProposalFromCURewriteOperation(unit, fix, fix.getDisplayString(), IProposalRelevance.DECLARE_SEALED_AS_DIRECT_SUPER_TYPE);
			proposals.add(cuCorrectionProposalToT(proposal, ADD_SEALED_SUPERTYPE));
		}
	}

	private CUCorrectionProposalCore createCorrectionProposalFromCURewriteOperation(ICompilationUnit unit, CompilationUnitRewriteOperationsFixCore fix, String label, int relevance) {
		try {
			CompilationUnitChange change= fix.createChange(null);
			CUCorrectionProposalCore proposal= new CUCorrectionProposalCore(label, unit, change, relevance);
			return proposal;
		} catch (CoreException e) {
			// do nothing
		}
		return null;
	}

	public void getAssignmentHasNoEffectProposalsBase(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		CompilationUnit root= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(root);
		if (!(selectedNode instanceof Assignment)) {
			return;
		}
		ASTNode assignedNode= ((Assignment) selectedNode).getLeftHandSide();
		ASTNode assignExpression= ((Assignment) selectedNode).getRightHandSide();
		if (!(assignedNode instanceof SimpleName) && !(assignExpression instanceof SimpleName)) {
			return;
		}

		IBinding binding= (assignedNode instanceof SimpleName) ? ((SimpleName) assignedNode).resolveBinding() : ((SimpleName) assignExpression).resolveBinding();
		if (!(binding instanceof IVariableBinding)) {
			return;
		}
		ITypeBinding typeBinding= Bindings.getBindingOfParentType(selectedNode);
		if (typeBinding == null) {
			return;
		}
		IVariableBinding fieldBinding= Bindings.findFieldInHierarchy(typeBinding, binding.getName());
		if (fieldBinding == null || fieldBinding.getDeclaringClass() != typeBinding && Modifier.isPrivate(fieldBinding.getModifiers())) {
			return;
		}

		if (binding != fieldBinding) {
			if (assignedNode instanceof SimpleName) {
				String label= CorrectionMessages.LocalCorrectionsSubProcessor_qualify_left_hand_side_description;
				proposals.add(astRewriteCorrectionProposalToT(createNoSideEffectProposal(context, (SimpleName) assignedNode, fieldBinding, label, IProposalRelevance.QUALIFY_LHS), CHANGE_CODE));
			}
			if (assignExpression instanceof SimpleName) {
				String label= CorrectionMessages.LocalCorrectionsSubProcessor_qualify_right_hand_side_description;
				proposals.add(astRewriteCorrectionProposalToT(createNoSideEffectProposal(context, (SimpleName) assignExpression, fieldBinding, label, IProposalRelevance.QUALIFY_RHS), CHANGE_CODE));
			}
		}

		if (binding == fieldBinding && ASTResolving.findParentBodyDeclaration(selectedNode) instanceof MethodDeclaration) {
			SimpleName simpleName= (SimpleName) ((assignedNode instanceof SimpleName) ? assignedNode : assignExpression);
			String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createparameter_description, BasicElementLabels.getJavaElementName(simpleName.getIdentifier()));
			NewVariableCorrectionProposalCore proposal= new NewVariableCorrectionProposalCore(label, context.getCompilationUnit(), NewVariableCorrectionProposalCore.PARAM, simpleName, null, IProposalRelevance.CREATE_PARAMETER, false);
			proposals.add(newVariableCorrectionProposalToT(proposal, CREATE_PARAMETER));
		}

	}

	public void getExpressionShouldBeAVariableProposalsBase(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		CompilationUnit root= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(root);
		String label= CorrectionMessages.LocalCorrectionsSubProcessor_createLocalVariable_description;
		NewLocalVariableCorrectionProposalCore proposal= new NewLocalVariableCorrectionProposalCore(label, context.getCompilationUnit(), selectedNode, IProposalRelevance.CREATE_LOCAL);
		if (proposal.hasProposal()) {
			proposals.add(newLocalVariableCorrectionProposalToT(proposal, EXPRESSION_SHOULD_BE_VARIABLE));
		}
	}

	private ASTRewriteCorrectionProposalCore createNoSideEffectProposal(IInvocationContext context, SimpleName nodeToQualify, IVariableBinding fieldBinding, String label, int relevance) {
		AST ast= nodeToQualify.getAST();

		Expression qualifier;
		if (Modifier.isStatic(fieldBinding.getModifiers())) {
			ITypeBinding declaringClass= fieldBinding.getDeclaringClass();
			qualifier= ast.newSimpleName(declaringClass.getTypeDeclaration().getName());
		} else {
			qualifier= ast.newThisExpression();
		}

		ASTRewrite rewrite= ASTRewrite.create(ast);
		FieldAccess access= ast.newFieldAccess();
		access.setName((SimpleName) rewrite.createCopyTarget(nodeToQualify));
		access.setExpression(qualifier);
		rewrite.replace(nodeToQualify, access, null);


//		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		return new ASTRewriteCorrectionProposalCore(label, context.getCompilationUnit(), rewrite, relevance);
	}

	public void getValueForAnnotationProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		ICompilationUnit cu= context.getCompilationUnit();
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode instanceof Annotation) {
			Annotation annotation= (Annotation) selectedNode;
			if (annotation.resolveTypeBinding() == null) {
				return;
			}
			MissingAnnotationAttributesProposalCore proposal= new MissingAnnotationAttributesProposalCore(cu, annotation, 10);
			proposals.add(missingAnnotationAttributesProposalToT(proposal, MISSING_ANNOTATION_ATTRIBUTES));
		}
	}

	public void getFallThroughProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode instanceof SwitchCase && selectedNode.getLocationInParent() == SwitchStatement.STATEMENTS_PROPERTY) {
			AST ast= selectedNode.getAST();
			ASTNode parent= selectedNode.getParent();

			// insert break:
			ASTRewrite rewrite= ASTRewrite.create(ast);
			ListRewrite listRewrite= rewrite.getListRewrite(parent, SwitchStatement.STATEMENTS_PROPERTY);
			listRewrite.insertBefore(ast.newBreakStatement(), selectedNode, null);

			String label= CorrectionMessages.LocalCorrectionsSubProcessor_insert_break_statement;
			ASTRewriteCorrectionProposalCore proposal= new ASTRewriteCorrectionProposalCore(label, context.getCompilationUnit(), rewrite, IProposalRelevance.INSERT_BREAK_STATEMENT);
			proposals.add(astRewriteCorrectionProposalToT(proposal, INSERT_BREAK_STATEMENT));

			if (problem.getProblemId() == IProblem.IllegalFallthroughToPattern)
				return;

			// insert //$FALL-THROUGH$:
			rewrite= ASTRewrite.create(ast);
			rewrite.setTargetSourceRangeComputer(new NoCommentSourceRangeComputer());
			listRewrite= rewrite.getListRewrite(parent, SwitchStatement.STATEMENTS_PROPERTY);
			ASTNode fallThroughComment= rewrite.createStringPlaceholder("//$FALL-THROUGH$", ASTNode.EMPTY_STATEMENT); //$NON-NLS-1$
			listRewrite.insertBefore(fallThroughComment, selectedNode, null);

			label= CorrectionMessages.LocalCorrectionsSubProcessor_insert_fall_through;
			proposal= new ASTRewriteCorrectionProposalCore(label, context.getCompilationUnit(), rewrite, IProposalRelevance.INSERT_FALL_THROUGH);
			proposals.add(astRewriteCorrectionProposalToT(proposal, INSERT_FALL_THROUGH));
		}
	}

	public void getCasesOmittedProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode instanceof Expression && selectedNode.getLocationInParent() == SwitchStatement.EXPRESSION_PROPERTY) {
			AST ast= selectedNode.getAST();
			SwitchStatement parent= (SwitchStatement) selectedNode.getParent();

			for (Statement statement : (List<Statement>) parent.statements()) {
				if (statement instanceof SwitchCase && ((SwitchCase) statement).isDefault()) {

					// insert //$CASES-OMITTED$:
					ASTRewrite rewrite= ASTRewrite.create(ast);
					rewrite.setTargetSourceRangeComputer(new NoCommentSourceRangeComputer());
					ListRewrite listRewrite= rewrite.getListRewrite(parent, SwitchStatement.STATEMENTS_PROPERTY);
					ASTNode casesOmittedComment= rewrite.createStringPlaceholder("//$CASES-OMITTED$", ASTNode.EMPTY_STATEMENT); //$NON-NLS-1$
					listRewrite.insertBefore(casesOmittedComment, statement, null);

					String label= CorrectionMessages.LocalCorrectionsSubProcessor_insert_cases_omitted;
					ASTRewriteCorrectionProposalCore proposal= new ASTRewriteCorrectionProposalCore(label, context.getCompilationUnit(), rewrite, IProposalRelevance.INSERT_CASES_OMITTED);
					proposals.add(astRewriteCorrectionProposalToT(proposal, INSERT_CASES_OMITTED));
					break;
				}
			}
		}
	}

	public void getDeprecatedFieldsToMethodsProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode instanceof Name) {
			IBinding binding= ((Name) selectedNode).resolveBinding();
			if (binding instanceof IVariableBinding) {
				IVariableBinding variableBinding= (IVariableBinding) binding;
				if (variableBinding.isField()) {
					String qualifiedName= variableBinding.getDeclaringClass().getTypeDeclaration().getQualifiedName();
					String fieldName= variableBinding.getName();
					String[] methodName= getMethod(JavaModelUtil.concatenateName(qualifiedName, fieldName));
					if (methodName != null) {
						AST ast= selectedNode.getAST();
						ASTRewrite astRewrite= ASTRewrite.create(ast);
						ImportRewrite importRewrite= StubUtility.createImportRewrite(context.getASTRoot(), true);

						MethodInvocation method= ast.newMethodInvocation();
						String qfn= importRewrite.addImport(methodName[0]);
						method.setExpression(ast.newName(qfn));
						method.setName(ast.newSimpleName(methodName[1]));
						ICompilationUnit cu= context.getCompilationUnit();

						astRewrite.replace(selectedNode, method, null);

						String label= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_replacefieldaccesswithmethod_description,
								BasicElementLabels.getJavaElementName(ASTNodes.asString(method)));
						ASTRewriteCorrectionProposalCore proposal= new ASTRewriteCorrectionProposalCore(label, cu, astRewrite, IProposalRelevance.REPLACE_FIELD_ACCESS_WITH_METHOD);
						proposal.setImportRewrite(importRewrite);
						proposals.add(astRewriteCorrectionProposalToT(proposal, REPLACE_FIELD_ACCESS));
					}
				}
			}
		}
	}

	private Map<String, String[]> resolveMap;

	private String[] getMethod(String fieldName) {
		if (resolveMap == null) {
			resolveMap= new HashMap<>();
			resolveMap.put("java.util.Collections.EMPTY_MAP", new String[] { "java.util.Collections", "emptyMap" }); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
			resolveMap.put("java.util.Collections.EMPTY_SET", new String[] { "java.util.Collections", "emptySet" }); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
			resolveMap.put("java.util.Collections.EMPTY_LIST", new String[] { "java.util.Collections", "emptyList" });//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		}
		return resolveMap.get(fieldName);
	}

	public void getMissingEnumConstantCaseProposalsBase(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		for (T proposal : proposals) {
			if (proposal instanceof ChangeCorrectionProposalCore) {
				if (CorrectionMessages.LocalCorrectionsSubProcessor_add_missing_cases_description.equals(((ChangeCorrectionProposalCore) proposal).getName())) {
					return;
				}
			}
		}

		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode instanceof Expression) {
			StructuralPropertyDescriptor locationInParent= selectedNode.getLocationInParent();
			ASTNode parent= selectedNode.getParent();
			ITypeBinding binding;
			List<Statement> statements;

			if (locationInParent == SwitchStatement.EXPRESSION_PROPERTY) {
				SwitchStatement statement= (SwitchStatement) parent;
				binding= statement.getExpression().resolveTypeBinding();
				statements= statement.statements();
			} else if (locationInParent == SwitchExpression.EXPRESSION_PROPERTY) {
				SwitchExpression switchExpression= (SwitchExpression) parent;
				binding= switchExpression.getExpression().resolveTypeBinding();
				statements= switchExpression.statements();
			} else {
				return;
			}

			if (binding == null || !binding.isEnum()) {
				return;
			}

			ArrayList<String> missingEnumCases= new ArrayList<>();
			boolean hasDefault= evaluateMissingSwitchCasesBase(binding, statements, missingEnumCases);
			if (missingEnumCases.size() == 0 && hasDefault)
				return;

			createMissingCaseProposalsBase(context, parent, problem, missingEnumCases, proposals);
		}
	}

	@SuppressWarnings("deprecation")
	public boolean evaluateMissingSwitchCasesBase(ITypeBinding enumBindings, List<Statement> switchStatements, ArrayList<String> enumConstNames) {
		for (IVariableBinding field : enumBindings.getDeclaredFields()) {
			if (field.isEnumConstant()) {
				enumConstNames.add(field.getName());
			}
		}

		boolean hasDefault= false;
		for (Statement curr : switchStatements) {
			if (curr instanceof SwitchCase) {
				SwitchCase switchCase= (SwitchCase) curr;
				if (ASTHelper.isSwitchCaseExpressionsSupportedInAST(switchCase.getAST())) {
					List<Expression> expressions= switchCase.expressions();
					if (expressions.size() == 0) {
						hasDefault= true;
					} else {
						for (Expression expression : expressions) {
							if (expression instanceof SimpleName) {
								enumConstNames.remove(((SimpleName) expression).getFullyQualifiedName());
							}
						}
					}
				} else {
					Expression expression= ((SwitchCase) curr).getExpression();
					if (expression instanceof SimpleName) {
						enumConstNames.remove(((SimpleName) expression).getFullyQualifiedName());
					} else if (expression == null) {
						hasDefault= true;
					}
				}
			}
		}
		return hasDefault;
	}

	@SuppressWarnings("deprecation")
	public void createMissingCaseProposalsBase(IInvocationContext context, ASTNode parent, IProblemLocation problem, ArrayList<String> enumConstNames, Collection<T> proposals) {
		List<Statement> statements;
		Expression expression;
		if (parent instanceof SwitchStatement) {
			SwitchStatement switchStatement= (SwitchStatement) parent;
			statements= switchStatement.statements();
			expression= switchStatement.getExpression();
		} else if (parent instanceof SwitchExpression) {
			SwitchExpression switchExpression= (SwitchExpression) parent;
			statements= switchExpression.statements();
			expression= switchExpression.getExpression();
		} else {
			return;
		}
		int defaultIndex= statements.size();
		boolean newCaseFormat= false;
		boolean hasDefault= false;
		for (int i= 0; i < statements.size(); i++) {
			Statement curr= statements.get(i);
			if (curr instanceof SwitchCase) {
				SwitchCase switchCase= (SwitchCase) curr;
				newCaseFormat= switchCase.isSwitchLabeledRule();
				if (ASTHelper.isSwitchCaseExpressionsSupportedInAST(switchCase.getAST())) {
					if (switchCase.expressions().size() == 0) {
						hasDefault= true;
						defaultIndex= i;
						break;
					}
				} else if (switchCase.getExpression() == null) {
					defaultIndex= i;
					break;
				}
			}
		}
		int originalDefaultIndex= defaultIndex;

		AST ast= parent.getAST();

		if (enumConstNames.size() > 0) {
			ASTRewrite astRewrite= ASTRewrite.create(ast);
			ListRewrite listRewrite;
			if (parent instanceof SwitchStatement) {
				listRewrite= astRewrite.getListRewrite(parent, SwitchStatement.STATEMENTS_PROPERTY);
			} else {
				listRewrite= astRewrite.getListRewrite(parent, SwitchExpression.STATEMENTS_PROPERTY);
			}

			String label= CorrectionMessages.LocalCorrectionsSubProcessor_add_missing_cases_description;
			LinkedCorrectionProposalCore proposal= new LinkedCorrectionProposalCore(label, context.getCompilationUnit(), astRewrite, IProposalRelevance.ADD_MISSING_CASE_STATEMENTS);

			if (newCaseFormat) {
				SwitchCase newSwitchCase= ast.newSwitchCase();
				newSwitchCase.setSwitchLabeledRule(true);
				for (String enumConstName : enumConstNames) {
					Name newName= ast.newName(enumConstName);
					newSwitchCase.expressions().add(newName);
				}
				listRewrite.insertAt(newSwitchCase, defaultIndex, null);
				defaultIndex++;
				if (problem != null && problem.getProblemId() == IProblem.SwitchExpressionMissingEnumConstantCaseDespiteDefault) {
					ThrowStatement newThrowStatement= getThrowForUnsupportedCase(expression, ast, astRewrite);
					listRewrite.insertAt(newThrowStatement, defaultIndex, null);
					defaultIndex++;
				} else {
					if (hasDefault && originalDefaultIndex < statements.size() - 1) {
						List<Statement> originalList= listRewrite.getOriginalList();
						Statement firstStatement= originalList.get(originalDefaultIndex + 1);
						Statement lastStatement= originalList.get(statements.size() - 1);
						try {
							// kludge to get around failure of listRewrite to format added blocks
							// properly when inserted
							String s= context.getCompilationUnit().getSource().substring(firstStatement.getStartPosition(), lastStatement.getStartPosition() + lastStatement.getLength());
							Block defaultCopy= (Block) listRewrite.getASTRewrite().createStringPlaceholder(s, ASTNode.BLOCK);
							listRewrite.insertAt(defaultCopy, defaultIndex, null);
							defaultIndex++;
						} catch (JavaModelException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else {
						ThrowStatement newThrowStatement= getThrowForUnsupportedCase(expression, ast, astRewrite);
						listRewrite.insertAt(newThrowStatement, defaultIndex, null);
						defaultIndex++;
					}
				}
			} else {
				for (String enumConstName : enumConstNames) {
					SwitchCase newSwitchCase= ast.newSwitchCase();
					Name newName= ast.newName(enumConstName);
					if (ASTHelper.isSwitchCaseExpressionsSupportedInAST(ast)) {
						newSwitchCase.expressions().add(newName);
					} else {
						newSwitchCase.setExpression(newName);
					}
					listRewrite.insertAt(newSwitchCase, defaultIndex, null);
					defaultIndex++;
					if (problem != null && problem.getProblemId() == IProblem.SwitchExpressionMissingEnumConstantCaseDespiteDefault) {
						newSwitchCase.setSwitchLabeledRule(true);
						ThrowStatement newThrowStatement= getThrowForUnsupportedCase(expression, ast, astRewrite);
						listRewrite.insertAt(newThrowStatement, defaultIndex, null);
						proposal.addLinkedPosition(astRewrite.track(newThrowStatement), true, enumConstName);
						defaultIndex++;
					}
					if (!hasDefault) {
						if (ASTHelper.isSwitchExpressionNodeSupportedInAST(ast)) {
							if (statements.size() > 0) {
								Statement firstStatement= statements.get(0);
								SwitchCase switchCase= (SwitchCase) firstStatement;
								boolean isArrow= switchCase.isSwitchLabeledRule();
								newSwitchCase.setSwitchLabeledRule(isArrow);
								if (isArrow || parent instanceof SwitchExpression) {
									ThrowStatement newThrowStatement= getThrowForUnsupportedCase(expression, ast, astRewrite);
									listRewrite.insertLast(newThrowStatement, null);
									proposal.addLinkedPosition(astRewrite.track(newThrowStatement), true, enumConstName);
								} else {
									listRewrite.insertAt(ast.newBreakStatement(), defaultIndex, null);
								}
							} else {
								listRewrite.insertAt(ast.newBreakStatement(), defaultIndex, null);
							}
						} else {
							listRewrite.insertAt(ast.newBreakStatement(), defaultIndex, null);
						}

						defaultIndex++;
					}
				}
			}
			if (!hasDefault) {
				SwitchCase newSwitchCase= ast.newSwitchCase();
				newSwitchCase.setSwitchLabeledRule(newCaseFormat);
				listRewrite.insertAt(newSwitchCase, defaultIndex, null);
				defaultIndex++;

				if (ASTHelper.isSwitchExpressionNodeSupportedInAST(ast)) {
					if (statements.size() > 0) {
						Statement firstStatement= statements.get(0);
						SwitchCase switchCase= (SwitchCase) firstStatement;
						boolean isArrow= switchCase.isSwitchLabeledRule();
						newSwitchCase.setSwitchLabeledRule(isArrow);
						if (isArrow || parent instanceof SwitchExpression) {
							ThrowStatement newThrowStatement= getThrowForUnexpectedDefault(expression, ast, astRewrite);
							listRewrite.insertLast(newThrowStatement, null);
							proposal.addLinkedPosition(astRewrite.track(newThrowStatement), true, "defaultCase"); //$NON-NLS-1$
						} else {
							listRewrite.insertAt(ast.newBreakStatement(), defaultIndex, null);
						}
					} else {
						listRewrite.insertAt(ast.newBreakStatement(), defaultIndex, null);
					}
				} else {
					newSwitchCase.setExpression(null);
					listRewrite.insertAt(ast.newBreakStatement(), defaultIndex, null);
				}
			}
			proposals.add(linkedCorrectionProposalToT(proposal, ADD_MISSING_CASE));
		}
		if (!hasDefault) {
			createMissingDefaultProposal(context, parent, proposals);
		}
	}

	private ThrowStatement getThrowForUnsupportedCase(Expression switchExpr, AST ast, ASTRewrite astRewrite) {
		ThrowStatement newThrowStatement= ast.newThrowStatement();
		ClassInstanceCreation newCic= ast.newClassInstanceCreation();
		newCic.setType(ast.newSimpleType(ast.newSimpleName("UnsupportedOperationException"))); //$NON-NLS-1$
		InfixExpression newInfixExpr= ast.newInfixExpression();
		StringLiteral newStringLiteral= ast.newStringLiteral();
		newStringLiteral.setLiteralValue("Unimplemented case: "); //$NON-NLS-1$
		newInfixExpr.setLeftOperand(newStringLiteral);
		newInfixExpr.setOperator(InfixExpression.Operator.PLUS);
		newInfixExpr.setRightOperand((Expression) astRewrite.createCopyTarget(switchExpr));
		newCic.arguments().add(newInfixExpr);
		newThrowStatement.setExpression(newCic);
		return newThrowStatement;
	}

	public void removeDefaultCaseProposalBase(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());

		if (selectedNode instanceof SwitchCase && ((SwitchCase) selectedNode).isDefault()) {
			ASTNode parent= selectedNode.getParent();
			List<Statement> statements;
			if (parent instanceof SwitchStatement) {
				statements= ((SwitchStatement) parent).statements();
			} else if (parent instanceof SwitchExpression) {
				statements= ((SwitchExpression) parent).statements();
			} else {
				return;
			}

			ASTRewrite astRewrite= ASTRewrite.create(parent.getAST());
			ListRewrite listRewrite;
			if (parent instanceof SwitchStatement) {
				listRewrite= astRewrite.getListRewrite(parent, SwitchStatement.STATEMENTS_PROPERTY);
			} else {
				listRewrite= astRewrite.getListRewrite(parent, SwitchExpression.STATEMENTS_PROPERTY);
			}

			int indexOfDefaultCase= statements.indexOf(selectedNode);
			if (indexOfDefaultCase != -1) {
				listRewrite.remove(statements.get(indexOfDefaultCase), null);
				int indexOfDefaultStatement= indexOfDefaultCase + 1;
				if (indexOfDefaultStatement < statements.size()) {
					listRewrite.remove(statements.get(indexOfDefaultStatement), null);
				}
			} else {
				return;
			}

			String label= CorrectionMessages.LocalCorrectionsSubProcessor_remove_default_case_description;
			ASTRewriteCorrectionProposalCore proposal= new ASTRewriteCorrectionProposalCore(label, context.getCompilationUnit(), astRewrite, IProposalRelevance.ADD_MISSING_DEFAULT_CASE);
			proposals.add(astRewriteCorrectionProposalToT(proposal, REMOVE_DEFAULT));
		}
	}

	public void getPermittedTypesProposal(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode instanceof Expression expression) {
			StructuralPropertyDescriptor locationInParent= selectedNode.getLocationInParent();
			ASTNode parent= selectedNode.getParent();
			List<Statement> statements;

			if (locationInParent == SwitchStatement.EXPRESSION_PROPERTY) {
				statements= ((SwitchStatement) parent).statements();
			} else if (locationInParent == SwitchExpression.EXPRESSION_PROPERTY) {
				statements= ((SwitchExpression) parent).statements();
			} else {
				return;
			}

			if (statements.size() != 0) {
				return;
			}
			ITypeBinding typeBinding= expression.resolveTypeBinding();
			if (typeBinding == null) {
				return;
			}
			IType type= (IType) typeBinding.getJavaElement();
			try {
				if (type == null || type.getPermittedSubtypeNames().length == 0) {
					return;
				}
			} catch (JavaModelException e) {
				return;
			}

			createPermittedTypeCasesProposal(context, parent, proposals);
		}
	}


	private void createPermittedTypeCasesProposal(IInvocationContext context, ASTNode parent, Collection<T> proposals) {
		class TypeExtendsSearchRequestor extends SearchRequestor {
			public List<SearchMatch> results= new ArrayList<>();

			public List<SearchMatch> getResults() {
				return results;
			}

			@Override
			public void acceptSearchMatch(SearchMatch match) throws CoreException {
				if (match.getAccuracy() == SearchMatch.A_ACCURATE) {
					results.add(match);
				}
			}
		}

		Expression expression;
		if (parent instanceof SwitchStatement) {
			SwitchStatement switchStatement= (SwitchStatement) parent;
			expression= switchStatement.getExpression();
		} else if (parent instanceof SwitchExpression) {
			SwitchExpression switchExpression= (SwitchExpression) parent;
			expression= switchExpression.getExpression();
		} else {
			return;
		}
		AST ast= parent.getAST();
		ASTRewrite astRewrite= ASTRewrite.create(ast);
		ListRewrite listRewrite;
		String caseCode= "{}"; //$NON-NLS-1$
		if (parent instanceof SwitchStatement) {
			listRewrite= astRewrite.getListRewrite(parent, SwitchStatement.STATEMENTS_PROPERTY);
		} else {
			SwitchExpression switchExpression= (SwitchExpression)parent;
			ASTNode swExpParent= switchExpression.getParent();
			ITypeBinding swExpTypeBinding= null;
			if (swExpParent instanceof VariableDeclarationFragment fragment) {
				IVariableBinding varBinding= fragment.resolveBinding();
				if (varBinding != null) {
					swExpTypeBinding= varBinding.getType();
				}
			} else if (swExpParent instanceof ReturnStatement retStatement) {
				MethodDeclaration methodDecl= ASTNodes.getFirstAncestorOrNull(retStatement, MethodDeclaration.class);
				if (methodDecl != null) {
					Type t= methodDecl.getReturnType2();
					if (t != null) {
						swExpTypeBinding= t.resolveBinding();
					}
				}
			}
			if (swExpTypeBinding == null) {
				return;
			}
			if (swExpTypeBinding.isPrimitive()) {
				if (swExpTypeBinding.getName().equals("boolean")) { //$NON-NLS-1$
					caseCode= "false;"; //$NON-NLS-1$
				} else {
					caseCode= "0;"; //$NON-NLS-1$
				}
			} else {
				caseCode= "null"; //$NON-NLS-1$
			}
			listRewrite= astRewrite.getListRewrite(parent, SwitchExpression.STATEMENTS_PROPERTY);
		}
		ITypeBinding binding= expression.resolveTypeBinding();
		IType sealedType= (IType)binding.getJavaElement();
		Set<String> excludedNames= new HashSet<>();
		CompilationUnit cu= context.getASTRoot();
		PackageDeclaration pkg= cu.getPackage();
		String pkgName= ""; //$NON-NLS-1$
		if (pkg != null) {
			pkgName= pkg.getName().getFullyQualifiedName();
		}
		try {
			excludedNames.addAll(List.of(computeReservedIdentifiers(parent, cu)));
		} catch (JavaModelException e) {
			return;
		}
		String label= CorrectionMessages.LocalCorrectionsSubProcessor_add_permitted_types_description;
		LinkedCorrectionProposalCore proposal= new LinkedCorrectionProposalCore(label, context.getCompilationUnit(), astRewrite, IProposalRelevance.ADD_PERMITTED_TYPES);
		ImportRewrite importRewriter= proposal.createImportRewrite(cu);

		String[] permittedTypeNames;
		try {
			permittedTypeNames= sealedType.getPermittedSubtypeNames();
			for (String permittedTypeName : permittedTypeNames) {
				boolean needImport= false;
				String importName= ""; //$NON-NLS-1$
				String[][] resolvedName= sealedType.resolveType(permittedTypeName);
				for (int i= 0; i < resolvedName.length; ++i) {
					String[] inner= resolvedName[i];
					if (!inner[0].isEmpty()) {
						importName= inner[0] + "." + inner[1]; //$NON-NLS-1$
						if (!inner[0].equals(pkgName)) {
							needImport= true;
						}
					} else {
						importName= inner[1];
					}
					if (permittedTypeName.startsWith(sealedType.getTypeQualifiedName('.'))) {
						needImport= false;
						String name= permittedTypeName.substring(sealedType.getTypeQualifiedName('.').length() + 1);
						IType innerType= sealedType.getType(name);
						if (innerType.exists()) {
							permittedTypeName= sealedType.getElementName() + "." + name; //$NON-NLS-1$
							if (innerType.isRecord()) {
								permittedTypeName += "("; //$NON-NLS-1$
								String separator= ""; //$NON-NLS-1$
								for (IField field : innerType.getRecordComponents()) {
									permittedTypeName += separator + Signature.toString(field.getTypeSignature());
									separator= ", "; //$NON-NLS-1$
									permittedTypeName += " " + field.getElementName(); //$NON-NLS-1$
								}
								permittedTypeName += ")"; //$NON-NLS-1$
							} else {
								String patternName= permittedTypeName.substring(0, 1).toLowerCase();
								String nameToUse= patternName;
								int count= 1;
								while (excludedNames.contains(nameToUse)) {
									nameToUse= patternName + (++count);
								}
								excludedNames.add(nameToUse);
								permittedTypeName += " " + nameToUse; //$NON-NLS-1$
							}
						}
					} else {
						SearchPattern pattern = SearchPattern.createPattern(importName, IJavaSearchConstants.TYPE, IJavaSearchConstants.DECLARATIONS, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
						if (pattern == null) {
							return;
						}
						TypeExtendsSearchRequestor requestor= new TypeExtendsSearchRequestor();
						try {
							search(pattern, SearchEngine.createJavaSearchScope(new IJavaElement[] {sealedType.getJavaProject()}), requestor);
						} catch (CoreException e) {
							return;
						}
						List<SearchMatch> results= requestor.getResults();
						for (SearchMatch result : results) {
							Object obj= result.getElement();
							if (obj instanceof IType resultType) {
								try {
									if (resultType.exists() && resultType.isRecord()) {
										permittedTypeName= inner[1];
										permittedTypeName += "("; //$NON-NLS-1$
										String separator= ""; //$NON-NLS-1$
										for (IField field : resultType.getRecordComponents()) {
											permittedTypeName += separator + Signature.toString(field.getTypeSignature());
											separator= ", "; //$NON-NLS-1$
											permittedTypeName += " " + field.getElementName(); //$NON-NLS-1$
										}
										permittedTypeName += ")"; //$NON-NLS-1$
									} else {
										permittedTypeName= inner[1];
										String patternName= permittedTypeName.substring(0, 1).toLowerCase();
										String nameToUse= patternName;
										int count= 1;
										while (excludedNames.contains(nameToUse)) {
											nameToUse= patternName + (++count);
										}
										excludedNames.add(nameToUse);
										permittedTypeName += " " + nameToUse; //$NON-NLS-1$
									}
								} catch (JavaModelException e) {
									// do nothing
								}
							}
						}

					}
				}
				String caseName= "case " + permittedTypeName + " -> " + caseCode; //$NON-NLS-1$ //$NON-NLS-2$
				SwitchCase newSwitchCase= (SwitchCase) astRewrite.createStringPlaceholder(caseName, ASTNode.SWITCH_CASE);
				listRewrite.insertLast(newSwitchCase, null);
				if (needImport) {
					importRewriter.addImport(importName);
				}
			}
			SwitchCase newNullCase= (SwitchCase) astRewrite.createStringPlaceholder("case null -> " + caseCode, ASTNode.SWITCH_CASE); //$NON-NLS-1$
			listRewrite.insertLast(newNullCase, null);
			SwitchCase defaultCase= (SwitchCase) astRewrite.createStringPlaceholder("default -> " + caseCode, ASTNode.SWITCH_CASE); //$NON-NLS-1$
			listRewrite.insertLast(defaultCase, null);
			proposals.add(linkedCorrectionProposalToT(proposal, ADD_PERMITTED_TYPES));
		} catch (JavaModelException e) {
			// should never occur
		}
	}

	private void search(SearchPattern searchPattern, IJavaSearchScope scope, SearchRequestor requestor) throws CoreException {
		new SearchEngine().search(
			searchPattern,
			new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()},
			scope,
			requestor,
			null);
	}

	/**
	 * Returns the reserved identifiers in the method to move.
	 *
	 * @param node - node to find previous variable names to exclude
	 * @return the reserved identifiers
	 * @throws JavaModelException
	 *             if the method declaration could not be found
	 */
	private String[] computeReservedIdentifiers(ASTNode node, CompilationUnit cu) throws JavaModelException {
		final List<String> names= new ArrayList<>();
		final MethodDeclaration declaration= ASTNodes.getFirstAncestorOrNull(node, MethodDeclaration.class);
		if (declaration != null) {
			final List<SingleVariableDeclaration> parameters= declaration.parameters();
			VariableDeclaration variable= null;
			for (SingleVariableDeclaration parameter : parameters) {
				variable= parameter;
				names.add(variable.getName().getIdentifier());
			}
			final Block body= declaration.getBody();
			if (body != null) {
				for (IBinding binding : new ScopeAnalyzer(cu).getDeclarationsAfter(body.getStartPosition(), ScopeAnalyzer.VARIABLES))
					names.add(binding.getName());
			}
		}
		final String[] result= new String[names.size()];
		names.toArray(result);
		return result;
	}

	public void getMissingDefaultCaseProposal(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode instanceof Expression) {
			StructuralPropertyDescriptor locationInParent= selectedNode.getLocationInParent();
			ASTNode parent= selectedNode.getParent();
			List<Statement> statements;

			if (locationInParent == SwitchStatement.EXPRESSION_PROPERTY) {
				statements= ((SwitchStatement) parent).statements();
			} else if (locationInParent == SwitchExpression.EXPRESSION_PROPERTY) {
				statements= ((SwitchExpression) parent).statements();
			} else {
				return;
			}

			for (Statement statement : statements) {
				if (statement instanceof SwitchCase && ((SwitchCase) statement).isDefault()) {
					return;
				}
			}
			createMissingDefaultProposal(context, parent, proposals);
		}
	}

	private void createMissingDefaultProposal(IInvocationContext context, ASTNode parent, Collection<T> proposals) {
		List<Statement> statements;
		Expression expression;
		if (parent instanceof SwitchStatement) {
			SwitchStatement switchStatement= (SwitchStatement) parent;
			statements= switchStatement.statements();
			expression= switchStatement.getExpression();
		} else if (parent instanceof SwitchExpression) {
			SwitchExpression switchExpression= (SwitchExpression) parent;
			statements= switchExpression.statements();
			expression= switchExpression.getExpression();
		} else {
			return;
		}
		AST ast= parent.getAST();
		ASTRewrite astRewrite= ASTRewrite.create(ast);
		ListRewrite listRewrite;
		if (parent instanceof SwitchStatement) {
			listRewrite= astRewrite.getListRewrite(parent, SwitchStatement.STATEMENTS_PROPERTY);
		} else {
			listRewrite= astRewrite.getListRewrite(parent, SwitchExpression.STATEMENTS_PROPERTY);
		}
		String label= CorrectionMessages.LocalCorrectionsSubProcessor_add_default_case_description;
		LinkedCorrectionProposalCore proposal= new LinkedCorrectionProposalCore(label, context.getCompilationUnit(), astRewrite, IProposalRelevance.ADD_MISSING_DEFAULT_CASE);

		SwitchCase newSwitchCase= ast.newSwitchCase();
		listRewrite.insertLast(newSwitchCase, null);
		boolean offerSwitchLabel= false;

		if (ASTHelper.isSwitchCaseExpressionsSupportedInAST(ast)) {
			boolean isArrow= false;
			if (statements.size() > 0) {
				Statement firstStatement= statements.get(0);
				SwitchCase switchCase= (SwitchCase) firstStatement;
				isArrow= switchCase.isSwitchLabeledRule();
				newSwitchCase.setSwitchLabeledRule(isArrow);
			}
			if (isArrow || parent instanceof SwitchExpression) {
				ThrowStatement newThrowStatement= getThrowForUnexpectedDefault(expression, ast, astRewrite);
				listRewrite.insertLast(newThrowStatement, null);
				proposal.addLinkedPosition(astRewrite.track(newThrowStatement), true, null);
			} else {
				listRewrite.insertLast(ast.newBreakStatement(), null);
				offerSwitchLabel= statements.size() == 0;
			}
		} else {
			listRewrite.insertLast(ast.newBreakStatement(), null);
		}

		proposals.add(linkedCorrectionProposalToT(proposal, CREATE_DEFAULT));

		if (offerSwitchLabel) {
			ASTRewrite astRewrite2= ASTRewrite.create(ast);
			ListRewrite listRewrite2= astRewrite2.getListRewrite(parent, SwitchStatement.STATEMENTS_PROPERTY);
			String label2= CorrectionMessages.LocalCorrectionsSubProcessor_add_default_case_label_description;
			LinkedCorrectionProposalCore proposal2= new LinkedCorrectionProposalCore(label2, context.getCompilationUnit(), astRewrite2, IProposalRelevance.ADD_MISSING_DEFAULT_CASE);
			SwitchCase newSwitchCase2= ast.newSwitchCase();
			listRewrite2.insertLast(newSwitchCase2, null);
			newSwitchCase2.setSwitchLabeledRule(true);
			listRewrite2.insertLast(astRewrite2.createStringPlaceholder("{}", ASTNode.BLOCK), null); //$NON-NLS-1$
			proposals.add(linkedCorrectionProposalToT(proposal2, CREATE_DEFAULT));
		}
	}

	private ThrowStatement getThrowForUnexpectedDefault(Expression switchExpression, AST ast, ASTRewrite astRewrite) {
		ThrowStatement newThrowStatement= ast.newThrowStatement();
		ClassInstanceCreation newCic= ast.newClassInstanceCreation();
		newCic.setType(ast.newSimpleType(ast.newSimpleName("IllegalArgumentException"))); //$NON-NLS-1$
		InfixExpression newInfixExpr= ast.newInfixExpression();
		StringLiteral newStringLiteral= ast.newStringLiteral();
		newStringLiteral.setLiteralValue("Unexpected value: "); //$NON-NLS-1$
		newInfixExpr.setLeftOperand(newStringLiteral);
		newInfixExpr.setOperator(InfixExpression.Operator.PLUS);
		newInfixExpr.setRightOperand((Expression) astRewrite.createCopyTarget(switchExpression));
		newCic.arguments().add(newInfixExpr);
		newThrowStatement.setExpression(newCic);
		return newThrowStatement;
	}

	public void getAddNLSTagProposalsCore(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) throws CoreException {
		IProposableFix fix= StringFixCore.createFix(context.getASTRoot(), problem, false, true);
		if (fix != null) {
			Map<String, String> options= new Hashtable<>();
			options.put(CleanUpConstants.ADD_MISSING_NLS_TAGS, CleanUpOptions.TRUE);
			FixCorrectionProposalCore addNLS= new FixCorrectionProposalCore(fix, new StringCleanUpCore(options), IProposalRelevance.ADD_MISSING_NLS_TAGS, context);
			addNLS.setCommandId(ADD_NON_NLS_ID);
			proposals.add(fixCorrectionProposalToT(addNLS, ADD_NLS));
		}
	}

	public void getUnnecessaryNLSTagProposalsCore(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) throws CoreException {
		IProposableFix fix= StringFixCore.createFix(context.getASTRoot(), problem, true, false);
		if (fix != null) {
			Map<String, String> options= new Hashtable<>();
			options.put(CleanUpConstants.REMOVE_UNNECESSARY_NLS_TAGS, CleanUpOptions.TRUE);
			FixCorrectionProposalCore proposal= new FixCorrectionProposalCore(fix, new StringCleanUpCore(options), IProposalRelevance.UNNECESSARY_NLS_TAG, context);
			proposal.setCommandId(REMOVE_UNNECESSARY_NLS_TAG_ID);
			proposals.add(fixCorrectionProposalToT(proposal, REMOVE_UNNECESSARY_NLS));
		}
	}

	public void getOverrideDefaultMethodProposal(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		CompilationUnit astRoot= context.getASTRoot();

		ASTNode selectedNode= problem.getCoveringNode(astRoot);
		if (selectedNode == null) {
			return;
		}

		StructuralPropertyDescriptor locationInParent= selectedNode.getLocationInParent();
		if (locationInParent != TypeDeclaration.NAME_PROPERTY && locationInParent != EnumDeclaration.NAME_PROPERTY) {
			return;
		}

		ASTNode typeNode= selectedNode.getParent();
		if (typeNode == null) {
			return;
		}

		ITypeBinding typeBinding= ((AbstractTypeDeclaration) typeNode).resolveBinding();
		if (typeBinding == null) {
			return;
		}

		if (problem.getProblemId() == IProblem.DuplicateInheritedDefaultMethods) {
			String[] args= problem.getProblemArguments();
			if (args.length < 5) {
				return;
			}

			String methodName= args[0];
			if (methodName == null) {
				return;
			}

			String[] parameters1= {};
			if (args[1] != null && args[1].length() != 0) {
				parameters1= args[1].split(", "); //$NON-NLS-1$
			}
			String[] parameters2= {};
			if (args[2] != null && args[2].length() != 0) {
				parameters2= args[2].split(", "); //$NON-NLS-1$
			}

			addOverrideProposal(typeNode, typeBinding, methodName, parameters1, args[3], context, proposals);
			addOverrideProposal(typeNode, typeBinding, methodName, parameters2, args[4], context, proposals);

		} else if (problem.getProblemId() == IProblem.InheritedDefaultMethodConflictsWithOtherInherited) {
			String[] args= problem.getProblemArguments();
			if (args.length < 3) {
				return;
			}

			String arg0= args[0];
			if (arg0 == null) {
				return;
			}
			int indexOfLParen= arg0.indexOf('(');
			if (indexOfLParen == -1) {
				return;
			}
			int indexOfRParen= arg0.indexOf(')');
			if (indexOfRParen == -1) {
				return;
			}

			String methodName= arg0.substring(0, indexOfLParen);

			String paramString= arg0.substring(indexOfLParen + 1, indexOfRParen);
			String[] parameters= {};
			if (paramString != null && paramString.length() != 0) {
				parameters= paramString.split(", "); //$NON-NLS-1$
			}

			addOverrideProposal(typeNode, typeBinding, methodName, parameters, args[1], context, proposals);
			addOverrideProposal(typeNode, typeBinding, methodName, parameters, args[2], context, proposals);
		}
	}

	private void addOverrideProposal(ASTNode typeNode, ITypeBinding typeBinding, String methodName, String[] parameters, String superType,
			IInvocationContext context, Collection<T> proposals) {
		ITypeBinding superTypeBinding= null;
		if (superType != null) {
			int i= superType.indexOf('<');
			if (i > 0) {
				superType= superType.substring(0, i);
			}
			superTypeBinding= Bindings.findTypeInHierarchy(typeBinding, superType);
		}
		if (superTypeBinding == null) {
			return;
		}

		IMethodBinding methodToOverride= Bindings.findMethodWithDeclaredParameterTypesInType(superTypeBinding, methodName, parameters);
		if (methodToOverride == null) {
			return;
		}

		String label;
		int modifiers= methodToOverride.getModifiers();
		if (Modifier.isDefault(modifiers)) {
			label= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_override_default_method_description, superTypeBinding.getName());
		} else if (Modifier.isAbstract(modifiers)) {
			label= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_implement_method_description, superTypeBinding.getName());
		} else {
			label= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_override_method_description, superTypeBinding.getName());
		}
//		Image image= JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC);

		CompilationUnit astRoot= context.getASTRoot();
		ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
		ICompilationUnit cu= context.getCompilationUnit();
		LinkedCorrectionProposalCore proposal= new LinkedCorrectionProposalCore(label, cu, rewrite, IProposalRelevance.OVERRIDE_DEFAULT_METHOD);

		ImportRewrite importRewrite= proposal.createImportRewrite(astRoot);
		ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(astRoot, typeNode.getStartPosition(), importRewrite);
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(cu);
		try {
			MethodDeclaration stub= StubUtility2Core.createImplementationStub(cu, rewrite, importRewrite, importRewriteContext, methodToOverride, typeBinding, settings,
					typeBinding.isInterface(), new NodeFinder(astRoot, typeNode.getStartPosition(), 0).getCoveringNode());
			BodyDeclarationRewrite.create(rewrite, typeNode).insert(stub, null);

			proposal.setEndPosition(rewrite.track(stub));
		} catch (CoreException e) {
			JavaManipulationPlugin.log(e);
		}

		proposals.add(linkedCorrectionProposalToT(proposal, ADD_OVERRIDE));
	}

	public void getRemoveRedundantTypeArgumentsProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		IProposableFix fix= TypeParametersFixCore.createRemoveRedundantTypeArgumentsFix(context.getASTRoot(), problem);
		if (fix != null) {
			Map<String, String> options= new HashMap<>();
			options.put(CleanUpConstants.REMOVE_REDUNDANT_TYPE_ARGUMENTS, CleanUpOptions.TRUE);
			FixCorrectionProposalCore proposal= new FixCorrectionProposalCore(fix, new TypeParametersCleanUpCore(options), IProposalRelevance.REMOVE_REDUNDANT_TYPE_ARGUMENTS, context);
			proposals.add(fixCorrectionProposalToT(proposal, REMOVE_REDUNDANT_TYPE_ARGS));
		}
	}

	public void getServiceProviderConstructorProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) throws CoreException {
		ASTNode node= problem.getCoveredNode(context.getASTRoot());
		if (!(node instanceof Name) && !(node.getParent() instanceof ProvidesDirective)) {
			return;
		}

		Name name= (Name) node;
		ITypeBinding targetBinding= name.resolveTypeBinding();

		if (targetBinding != null &&
				!targetBinding.isInterface()
				&& !Modifier.isAbstract(targetBinding.getModifiers())) {
			ICompilationUnit targetCU= ASTResolving.findCompilationUnitForBinding(context.getCompilationUnit(), context.getASTRoot(), targetBinding);
			IJavaProject proj= targetCU.getJavaProject();

			// Get the AST Root (CompilationUnit) for target class
			ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.setSource(targetCU);
			parser.setProject(proj);
			parser.setUnitName(targetCU.getPath().toString());
			parser.setResolveBindings(true);
			ASTNode targetRoot= parser.createAST(null);

			if (!(targetRoot instanceof CompilationUnit)) {
				return;
			}

			IType targetType= proj.findType(targetBinding.getQualifiedName());

			// Locate the no-arg constructor binding for the type
			List<IMethodBinding> result= Arrays.asList(targetBinding.getDeclaredMethods()).stream()
					.filter(m -> m.isConstructor() && m.getParameterTypes().length == 0)
					.collect(Collectors.toList());

			// no-arg constructor exists, need to change visibility
			if (!result.isEmpty()) {
				IMethodBinding targetMethodBinding= result.get(0);
				IMethod targetMethod= null;
				for (IMethod m : targetType.getMethods()) {
					if (m.isConstructor() && m.getParameters().length == 0) {
						targetMethod= m;
						break;
					}
				}

				String label= CorrectionMessages.LocalCorrectionsSubProcessor_changeconstructor_public_description;
				int include= Modifier.PUBLIC;
				int exclude= Modifier.PRIVATE | Modifier.PROTECTED | Modifier.PUBLIC;

				// Locate the constructor declaration node in the target AST Node
				MethodDeclaration targetMethodDecl= ASTNodeSearchUtil.getMethodDeclarationNode(targetMethod, (CompilationUnit) targetRoot);

				ModifierChangeCorrectionProposalCore proposal= new ModifierChangeCorrectionProposalCore(label, targetCU, targetMethodBinding, targetMethodDecl.getName(),
						include, exclude, IProposalRelevance.CHANGE_VISIBILITY_TO_NON_PRIVATE);
				proposals.add(modifierChangeCorrectionProposalToT(proposal, CHANGE_MODIFIER));
			} else {
				// no-arg constructor does not exist, need to create it
				String[] args= new String[] { ASTResolving
						.getMethodSignature(ASTResolving.getTypeSignature(targetBinding), new ITypeBinding[0], false) };
				String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createconstructor_description, args);
				NewMethodCorrectionProposalCore proposal= new NewMethodCorrectionProposalCore(label, targetCU, targetRoot, new ArrayList<>(), targetBinding, IProposalRelevance.CREATE_CONSTRUCTOR);
				proposals.add(newMethodCorrectionProposalToT(proposal, CREATE_CONSTRUCTOR));
			}
		}
	}

	public void getUnimplementedMethodsProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		IProposableFix addMethodFix= UnimplementedCodeFixCore.createAddUnimplementedMethodsFix(context.getASTRoot(), problem);
		if (addMethodFix != null) {
			Map<String, String> settings= new Hashtable<>();
			settings.put(CleanUpConstants.ADD_MISSING_METHODES, CleanUpOptions.TRUE);
			ICleanUp cleanUp= new UnimplementedCodeCleanUpCore(settings);
			FixCorrectionProposalCore proposal= new FixCorrectionProposalCore(addMethodFix, cleanUp, IProposalRelevance.ADD_UNIMPLEMENTED_METHODS, context);
			proposals.add(fixCorrectionProposalToT(proposal, ADD_UNIMPLEMENTED_METHODS));
		}

		IProposableFix makeAbstractFix= UnimplementedCodeFixCore.createMakeTypeAbstractFix(context.getASTRoot(), problem);
		if (makeAbstractFix != null) {
			Map<String, String> settings= new Hashtable<>();
			settings.put(UnimplementedCodeCleanUpCore.MAKE_TYPE_ABSTRACT, CleanUpOptions.TRUE);
			ICleanUp cleanUp= new UnimplementedCodeCleanUpCore(settings);
			FixCorrectionProposalCore proposal= new FixCorrectionProposalCore(makeAbstractFix, cleanUp, IProposalRelevance.MAKE_TYPE_ABSTRACT, context);
			proposals.add(fixCorrectionProposalToT(proposal, ADD_UNIMPLEMENTED_METHODS));
		}
	}

	protected LocalCorrectionsBaseSubProcessor() {
	}

	protected abstract T refactoringCorrectionProposalToT(RefactoringCorrectionProposalCore core, int uid);
	protected abstract T linkedCorrectionProposalToT(LinkedCorrectionProposalCore core, int uid);
	protected abstract T changeMethodSignatureProposalToT(ChangeMethodSignatureProposalCore core, int uid);
	protected abstract T fixCorrectionProposalToT(FixCorrectionProposalCore core, int uid);
	protected abstract T constructorFromSuperClassProposalToT(ConstructorFromSuperclassProposalCore core, int uid);
	protected abstract T createNewObjectProposalToT(CreateNewObjectProposalCore core, int uid);
	protected abstract T createObjectReferenceProposalToT(CreateObjectReferenceProposalCore core, int uid);
	protected abstract T createVariableReferenceProposalToT(CreateVariableReferenceProposalCore core, int uid);
	protected abstract T generateForLoopAssistProposalToT(GenerateForLoopAssistProposalCore proposal);
	protected abstract T astRewriteCorrectionProposalToT(ASTRewriteCorrectionProposalCore core, int uid);
	protected abstract T replaceCorrectionProposalToT(ReplaceCorrectionProposalCore core, int uid);
	protected abstract T cuCorrectionProposalToT(CUCorrectionProposalCore core, int uid);
	protected abstract T linkedNamesAssistProposalToT(LinkedNamesAssistProposalCore core);
	protected abstract T LinkedNamesAssistShowDuplicateProposalToT(LinkedOpenDeclarationProposalCore core);
	protected abstract T assignToVariableAssistProposalToT(AssignToVariableAssistProposalCore core);
	protected abstract T newVariableCorrectionProposalToT(NewVariableCorrectionProposalCore core, int uid);
	protected abstract T newLocalVariableCorrectionProposalToT(NewLocalVariableCorrectionProposalCore core, int uid);
	protected abstract T missingAnnotationAttributesProposalToT(MissingAnnotationAttributesProposalCore core, int uid);
	protected abstract T newMethodCorrectionProposalToT(NewMethodCorrectionProposalCore core, int uid);
	protected abstract T newProviderMethodDeclarationProposalToT(NewProviderMethodDeclarationCore core, int uid);
	protected abstract T modifierChangeCorrectionProposalToT(ModifierChangeCorrectionProposalCore core, int uid);

}
