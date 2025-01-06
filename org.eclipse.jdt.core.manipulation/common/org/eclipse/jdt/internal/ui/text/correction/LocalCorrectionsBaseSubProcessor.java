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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodReference;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ProvidesDirective;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
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
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
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
import org.eclipse.jdt.internal.corext.dom.TypeRules;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CodeStyleFixCore;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore;
import org.eclipse.jdt.internal.corext.fix.IProposableFix;
import org.eclipse.jdt.internal.corext.fix.SealedClassFixCore;
import org.eclipse.jdt.internal.corext.fix.UnusedCodeFixCore;
import org.eclipse.jdt.internal.corext.refactoring.code.Invocations;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.surround.ExceptionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryCatchAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryCatchRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.NoCommentSourceRangeComputer;
import org.eclipse.jdt.internal.corext.refactoring.util.SurroundWithAnalyzer;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposalCore;

import org.eclipse.jdt.internal.ui.fix.CodeStyleCleanUpCore;
import org.eclipse.jdt.internal.ui.fix.UnnecessaryCodeCleanUpCore;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposalCore.ChangeDescription;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposalCore.InsertDescription;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposalCore.RemoveDescription;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ConstructorFromSuperclassProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CreateNewObjectProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CreateObjectReferenceProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CreateVariableReferenceProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.MissingAnnotationAttributesProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ModifierChangeCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewMethodCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewVariableCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.RefactoringCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ReplaceCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.util.ASTHelper;

public abstract class LocalCorrectionsBaseSubProcessor<T> {

	private final String ADD_EXCEPTION_TO_THROWS_ID= "org.eclipse.jdt.ui.correction.addThrowsDecl"; //$NON-NLS-1$

	private final String ADD_FIELD_QUALIFICATION_ID= "org.eclipse.jdt.ui.correction.qualifyField"; //$NON-NLS-1$

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
						ASTNode parent= selectedNode.getParent();
						ICompilationUnit cu= context.getCompilationUnit();
						// add explicit type arguments if necessary (for 1.8 and later, we're optimistic that inference just works):
						if (Invocations.isInvocationWithArguments(parent) && !JavaModelUtil.is1d8OrHigher(cu.getJavaProject())) {
							IMethodBinding methodBinding= Invocations.resolveBinding(parent);
							if (methodBinding != null) {
								ITypeBinding[] parameterTypes= methodBinding.getParameterTypes();
								int i= Invocations.getArguments(parent).indexOf(selectedNode);
								if (parameterTypes.length >= i && parameterTypes[i].isParameterizedType()) {
									ITypeBinding[] typeArguments= parameterTypes[i].getTypeArguments();
									for (ITypeBinding typeArgument : typeArguments) {
										typeArgument= Bindings.normalizeForDeclarationUse(typeArgument, ast);
										if (!TypeRules.isJavaLangObject(typeArgument)) {
											// add all type arguments if at least one is found to be necessary:
											List<Type> typeArgumentsList= method.typeArguments();
											for (ITypeBinding t : typeArguments) {
												typeArgument= Bindings.normalizeForDeclarationUse(t, ast);
												typeArgumentsList.add(importRewrite.addImport(typeArgument, ast));
											}
											break;
										}
									}
								}
							}
						}

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

			createMissingCaseProposalsBase(context, parent, missingEnumCases, proposals);
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
	public void createMissingCaseProposalsBase(IInvocationContext context, ASTNode parent, ArrayList<String> enumConstNames, Collection<T> proposals) {
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
		for (int i= 0; i < statements.size(); i++) {
			Statement curr= statements.get(i);
			if (curr instanceof SwitchCase) {
				SwitchCase switchCase= (SwitchCase) curr;
				if (ASTHelper.isSwitchCaseExpressionsSupportedInAST(switchCase.getAST())) {
					if (switchCase.expressions().size() == 0) {
						defaultIndex= i;
						break;
					}
				} else if (switchCase.getExpression() == null) {
					defaultIndex= i;
					break;
				}
			}
		}
		boolean hasDefault= defaultIndex < statements.size();

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
			if (!hasDefault) {
				SwitchCase newSwitchCase= ast.newSwitchCase();
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
	protected abstract T astRewriteCorrectionProposalToT(ASTRewriteCorrectionProposalCore core, int uid);
	protected abstract T replaceCorrectionProposalToT(ReplaceCorrectionProposalCore core, int uid);
	protected abstract T cuCorrectionProposalToT(CUCorrectionProposalCore core, int uid);
	protected abstract T newVariableCorrectionProposalToT(NewVariableCorrectionProposalCore core, int uid);
	protected abstract T missingAnnotationAttributesProposalToT(MissingAnnotationAttributesProposalCore core, int uid);
	protected abstract T newMethodCorrectionProposalToT(NewMethodCorrectionProposalCore core, int uid);
	protected abstract T modifierChangeCorrectionProposalToT(ModifierChangeCorrectionProposalCore core, int uid);

}
