/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *     Renaud Waldura &lt;renaud+eclipse@waldura.com&gt; - Access to static proposal
 *     Benjamin Muskalla <bmuskalla@innoopract.com> - [quick fix] Shouldn't offer "Add throws declaration" quickfix for overriding signature if result would conflict with overridden signature
 *     Lukas Hanke <hanke@yatta.de> - Bug 241696 [quick fix] quickfix to iterate over a collection - https://bugs.eclipse.org/bugs/show_bug.cgi?id=241696
 *     Sandra Lions <sandra.lions-piron@oracle.com> - [quick fix] for qualified enum constants in switch-case labels - https://bugs.eclipse.org/bugs/90140
 *     Stephan Herrmann - Contribution for Bug 463360 - [override method][null] generating method override should not create redundant null annotations
 *     Microsoft Corporation - read preferences from the compilation unit
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
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
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.core.manipulation.dom.NecessaryParenthesesChecker;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility2;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.BodyDeclarationRewrite;
import org.eclipse.jdt.internal.corext.dom.CodeScopeBuilder;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.TypeRules;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CodeStyleFix;
import org.eclipse.jdt.internal.corext.fix.IProposableFix;
import org.eclipse.jdt.internal.corext.fix.Java50Fix;
import org.eclipse.jdt.internal.corext.fix.StringFix;
import org.eclipse.jdt.internal.corext.fix.TypeParametersFix;
import org.eclipse.jdt.internal.corext.fix.UnimplementedCodeFix;
import org.eclipse.jdt.internal.corext.fix.UnusedCodeFix;
import org.eclipse.jdt.internal.corext.refactoring.code.Invocations;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.surround.ExceptionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryCatchAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryCatchRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.NoCommentSourceRangeComputer;
import org.eclipse.jdt.internal.corext.refactoring.util.SurroundWithAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.util.TightSourceRangeComputer;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;
import org.eclipse.jdt.ui.actions.GenerateHashCodeEqualsAction;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.actions.InferTypeArgumentsAction;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ChangeCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.fix.CodeStyleCleanUp;
import org.eclipse.jdt.internal.ui.fix.Java50CleanUp;
import org.eclipse.jdt.internal.ui.fix.StringCleanUp;
import org.eclipse.jdt.internal.ui.fix.TypeParametersCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnimplementedCodeCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnnecessaryCodeCleanUp;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.nls.ExternalizeWizard;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposal.ChangeDescription;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposal.InsertDescription;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposal.RemoveDescription;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ConstructorFromSuperclassProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CreateNewObjectProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CreateObjectReferenceProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CreateVariableReferenceProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedNamesAssistProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.MissingAnnotationAttributesProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ModifierChangeCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewMethodCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewProviderMethodDeclaration;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewVariableCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.RefactoringCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ReplaceCorrectionProposal;
import org.eclipse.jdt.internal.ui.util.ASTHelper;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

/**
  */
public class LocalCorrectionsSubProcessor {
	private static final String RAW_TYPE_REFERENCE_ID= "org.eclipse.jdt.ui.correction.rawTypeReference"; //$NON-NLS-1$

	private static final String ADD_EXCEPTION_TO_THROWS_ID= "org.eclipse.jdt.ui.correction.addThrowsDecl"; //$NON-NLS-1$

	private static final String ADD_NON_NLS_ID= "org.eclipse.jdt.ui.correction.addNonNLS"; //$NON-NLS-1$

	private static final String ADD_FIELD_QUALIFICATION_ID= "org.eclipse.jdt.ui.correction.qualifyField"; //$NON-NLS-1$

	private static final String ADD_STATIC_ACCESS_ID= "org.eclipse.jdt.ui.correction.changeToStatic"; //$NON-NLS-1$

	private static final String REMOVE_UNNECESSARY_NLS_TAG_ID= "org.eclipse.jdt.ui.correction.removeNlsTag"; //$NON-NLS-1$

	public static void addUncaughtExceptionProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws CoreException {
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
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
			RefactoringCorrectionProposal proposal= new RefactoringCorrectionProposal(label, cu, refactoring, IProposalRelevance.SURROUND_WITH_TRY_CATCH, image);
			proposal.setLinkedProposalModel(refactoring.getLinkedProposalModel());
			proposals.add(proposal);
		}

		if (JavaModelUtil.is1d7OrHigher(cu.getJavaProject())) {
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
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
				RefactoringCorrectionProposal proposal= new RefactoringCorrectionProposal(label, cu, refactoring, IProposalRelevance.SURROUND_WITH_TRY_MULTICATCH, image);
				proposal.setLinkedProposalModel(refactoring.getLinkedProposalModel());
				proposals.add(proposal);
			}
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

			if (JavaModelUtil.is1d7OrHigher(cu.getJavaProject())) {
				List<CatchClause> catchClauses= surroundingTry.catchClauses();
				List<ITypeBinding> filteredExceptions= SurroundWithTryCatchRefactoring.filterSubtypeExceptions(uncaughtExceptions);
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
				ASTRewrite rewrite= ASTRewrite.create(ast);

				if (catchClauses != null && catchClauses.size() == 1) {
					String label= filteredExceptions.size() > 1
							? CorrectionMessages.LocalCorrectionsSubProcessor_addexceptionstoexistingcatch_description
							: CorrectionMessages.LocalCorrectionsSubProcessor_addexceptiontoexistingcatch_description;
					LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, cu, rewrite, IProposalRelevance.ADD_EXCEPTIONS_TO_EXISTING_CATCH, image);
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

					proposals.add(proposal);
				} else if (catchClauses != null && catchClauses.isEmpty() && filteredExceptions.size() > 1) {
					String label= CorrectionMessages.LocalCorrectionsSubProcessor_addadditionalmulticatch_description;
					LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, cu, rewrite, IProposalRelevance.ADD_EXCEPTIONS_TO_EXISTING_CATCH, image);
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
					proposals.add(proposal);
				}
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

				if (isApplicable) {
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
					Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);

					ChangeMethodSignatureProposal proposal= new ChangeMethodSignatureProposal(label, cu, astRoot, binding, null, desc, IProposalRelevance.ADD_THROWS_DECLARATION, image);

					for (int i= 0; i < uncaughtExceptions.length; i++) {
						addExceptionTypeLinkProposals(proposal, uncaughtExceptions[i], proposal.getExceptionTypeGroupId(i + nExistingExceptions));
					}

					proposal.setCommandId(ADD_EXCEPTION_TO_THROWS_ID);
					proposals.add(proposal);
				}
			}
		}
	}

	private static Type addNewException(AST ast, List<ITypeBinding> filteredExceptions, ASTRewrite rewrite, LinkedCorrectionProposal proposal, ImportRewrite imports,
			ImportRewriteContext importRewriteContext, int i) {
		ITypeBinding excBinding= filteredExceptions.get(i);
		Type type= imports.addImport(excBinding, ast, importRewriteContext, TypeLocation.EXCEPTION);

		String typeKey= "type" + i; //$NON-NLS-1$
		proposal.addLinkedPosition(rewrite.track(type), false, typeKey);
		addExceptionTypeLinkProposals(proposal, excBinding, typeKey);

		return type;
	}

	private static void addAdditionalCatchProposal(IInvocationContext context, Collection<ICommandAccess> proposals, ICompilationUnit cu, ASTNode selectedNode, int offset, int length,
			BodyDeclaration decl,
			ITypeBinding[] uncaughtExceptions, TryStatement surroundingTry, AST ast) throws CoreException {
		ASTRewrite rewrite= ASTRewrite.create(surroundingTry.getAST());

		String label= CorrectionMessages.LocalCorrectionsSubProcessor_addadditionalcatch_description;
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
		LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, cu, rewrite, IProposalRelevance.ADD_ADDITIONAL_CATCH, image);

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
		proposals.add(proposal);
	}

	private static void addExceptionTypeLinkProposals(LinkedCorrectionProposal proposal, ITypeBinding exc, String key) {
		// all super classes except Object
		while (exc != null && !"java.lang.Object".equals(exc.getQualifiedName())) { //$NON-NLS-1$
			proposal.addLinkedPositionProposal(key, exc);
			exc= exc.getSuperclass();
		}
	}


	private static boolean isSubtype(ITypeBinding curr, ITypeBinding[] addedExceptions) {
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

	public static void addUnreachableCatchProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode == null) {
			return;
		}

		QuickAssistProcessor.getCatchClauseToThrowsProposals(context, selectedNode, proposals);
	}

	public static void addNLSProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws CoreException {
		final ICompilationUnit cu= context.getCompilationUnit();
		if (cu == null || !cu.exists()) {
			return;
		}
		String name= CorrectionMessages.LocalCorrectionsSubProcessor_externalizestrings_description;

		ChangeCorrectionProposal proposal= new ChangeCorrectionProposal(name, null, IProposalRelevance.EXTERNALIZE_STRINGS, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE)) {
			@Override
			public void apply(IDocument document) {
				ExternalizeWizard.open(cu, JavaPlugin.getActiveWorkbenchShell());
			}

			@Override
			public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
				return CorrectionMessages.LocalCorrectionsSubProcessor_externalizestrings_additional_info;
			}

		};
		proposals.add(proposal);

		IProposableFix fix= StringFix.createFix(context.getASTRoot(), problem, false, true);
		if (fix != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_NLS_NEVER_TRANSLATE);
			Map<String, String> options= new Hashtable<>();
			options.put(CleanUpConstants.ADD_MISSING_NLS_TAGS, CleanUpOptions.TRUE);
			FixCorrectionProposal addNLS= new FixCorrectionProposal(fix, new StringCleanUp(options), IProposalRelevance.ADD_MISSING_NLS_TAGS, image, context);
			addNLS.setCommandId(ADD_NON_NLS_ID);
			proposals.add(addNLS);
		}
	}

	public static void getUnnecessaryNLSTagProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws CoreException {
		IProposableFix fix= StringFix.createFix(context.getASTRoot(), problem, true, false);
		if (fix != null) {
			Image image= PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
			Map<String, String> options= new Hashtable<>();
			options.put(CleanUpConstants.REMOVE_UNNECESSARY_NLS_TAGS, CleanUpOptions.TRUE);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, new StringCleanUp(options), IProposalRelevance.UNNECESSARY_NLS_TAG, image, context);
			proposal.setCommandId(REMOVE_UNNECESSARY_NLS_TAG_ID);
			proposals.add(proposal);
		}
	}


	/*
	 * Fix instance accesses and indirect (static) accesses to static fields/methods
	 */
	public static void addCorrectAccessToStaticProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws CoreException {
		IProposableFix fix= CodeStyleFix.createIndirectAccessToStaticFix(context.getASTRoot(), problem);
		if (fix != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			Map<String, String> options= new HashMap<>();
			options.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS, CleanUpOptions.TRUE);
			options.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS, CleanUpOptions.TRUE);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, new CodeStyleCleanUp(options), IProposalRelevance.CREATE_INDIRECT_ACCESS_TO_STATIC, image, context);
			proposal.setCommandId(ADD_STATIC_ACCESS_ID);
			proposals.add(proposal);
			return;
		}

		IProposableFix[] fixes= CodeStyleFix.createNonStaticAccessFixes(context.getASTRoot(), problem);
		if (fixes != null) {
			IProposableFix fix1= fixes[0];
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			Map<String, String> options= new HashMap<>();
			options.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS, CleanUpOptions.TRUE);
			options.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS, CleanUpOptions.TRUE);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix1, new CodeStyleCleanUp(options), IProposalRelevance.CREATE_NON_STATIC_ACCESS_USING_DECLARING_TYPE, image, context);
			proposal.setCommandId(ADD_STATIC_ACCESS_ID);
			proposals.add(proposal);

			if (fixes.length > 1) {
				Map<String, String> options1= new HashMap<>();
				options1.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS, CleanUpOptions.TRUE);
				options1.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_SUBTYPE_ACCESS, CleanUpOptions.TRUE);
				options1.put(CleanUpConstants.MEMBER_ACCESSES_STATIC_QUALIFY_WITH_DECLARING_CLASS_INSTANCE_ACCESS, CleanUpOptions.TRUE);
				IProposableFix fix2= fixes[1];
				proposal= new FixCorrectionProposal(fix2, new CodeStyleCleanUp(options1), IProposalRelevance.CREATE_NON_STATIC_ACCESS_USING_INSTANCE_TYPE, image, context);
				proposals.add(proposal);
			}
		}
		ModifierCorrectionSubProcessor.addNonAccessibleReferenceProposal(context, problem, proposals, ModifierCorrectionSubProcessor.TO_NON_STATIC, IProposalRelevance.REMOVE_STATIC_MODIFIER);
	}

	public static void addUnimplementedMethodsProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		IProposableFix addMethodFix= UnimplementedCodeFix.createAddUnimplementedMethodsFix(context.getASTRoot(), problem);
		if (addMethodFix != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);

			Map<String, String> settings= new Hashtable<>();
			settings.put(CleanUpConstants.ADD_MISSING_METHODES, CleanUpOptions.TRUE);
			ICleanUp cleanUp= new UnimplementedCodeCleanUp(settings);

			proposals.add(new FixCorrectionProposal(addMethodFix, cleanUp, IProposalRelevance.ADD_UNIMPLEMENTED_METHODS, image, context));
		}

		IProposableFix makeAbstractFix= UnimplementedCodeFix.createMakeTypeAbstractFix(context.getASTRoot(), problem);
		if (makeAbstractFix != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);

			Map<String, String> settings= new Hashtable<>();
			settings.put(UnimplementedCodeCleanUp.MAKE_TYPE_ABSTRACT, CleanUpOptions.TRUE);
			ICleanUp cleanUp= new UnimplementedCodeCleanUp(settings);

			proposals.add(new FixCorrectionProposal(makeAbstractFix, cleanUp, IProposalRelevance.MAKE_TYPE_ABSTRACT, image, context));
		}
	}

	public static void addUninitializedLocalVariableProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
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
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);

			LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, cu, rewrite, IProposalRelevance.INITIALIZE_VARIABLE, image);
			proposal.addLinkedPosition(rewrite.track(expression), false, "initializer"); //$NON-NLS-1$
			proposals.add(proposal);
		}
	}

	public static void addConstructorFromSuperclassProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws CoreException {
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
				addUncaughtExceptionProposals(context, problem, proposals);
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
				proposals.add(new ConstructorFromSuperclassProposal(cu, typeDeclaration, curr, IProposalRelevance.ADD_CONSTRUCTOR_FROM_SUPER_CLASS));
			}
		}
	}

	public static void addNewObjectProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws CoreException {
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
			CreateNewObjectProposal createNewObjectProposal= new CreateNewObjectProposal(cu, expressionStatement, binding, IProposalRelevance.ADD_CONSTRUCTOR_FROM_SUPER_CLASS);
			if (createNewObjectProposal.hasProposal()) {
				proposals.add(createNewObjectProposal);
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
			ITypeBinding binding= null;
			binding= type == null ? null : type.resolveBinding();
			if (binding == null) {
				return;
			}
			ICompilationUnit cu= context.getCompilationUnit();
			CreateNewObjectProposal createNewObjectProposal= new CreateNewObjectProposal(cu, vdf, binding, IProposalRelevance.ADD_CONSTRUCTOR_FROM_SUPER_CLASS);
			if (createNewObjectProposal.hasProposal()) {
				proposals.add(createNewObjectProposal);
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
			CreateNewObjectProposal createNewObjectProposal2= new CreateNewObjectProposal(cu, vdf, (IVariableBinding)resolveBinding, IProposalRelevance.ADD_CONSTRUCTOR_FROM_SUPER_CLASS);
			if (createNewObjectProposal2.hasProposal()) {
				proposals.add(createNewObjectProposal2);
			}
			return;
		}
		if (problem.getProblemId() == IProblem.StaticMethodRequested ||
				problem.getProblemId() == IProblem.NonStaticFieldFromStaticInvocation) {
			addUncaughtExceptionProposals(context, problem, proposals);
		}
	}

	public static void addObjectReferenceProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws CoreException {
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
					addUncaughtExceptionProposals(context, problem, proposals);
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
		CreateObjectReferenceProposal createObjectReferenceProposal= new CreateObjectReferenceProposal(cu, selectedNode, binding, IProposalRelevance.ADD_CONSTRUCTOR_FROM_SUPER_CLASS);
		if (createObjectReferenceProposal.hasProposal()) {
			proposals.add(createObjectReferenceProposal);
		}
	}

	public static void addVariableReferenceProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws CoreException {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode == null) {
			return;
		}
		selectedNode= ASTNodes.getParent(selectedNode, ASTNode.VARIABLE_DECLARATION_FRAGMENT);
		if (selectedNode == null) {
			if (problem.getProblemId() == IProblem.NonStaticFieldFromStaticInvocation) {
				addUncaughtExceptionProposals(context, problem, proposals);
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
		CreateVariableReferenceProposal createVariableReferenceProposal= new CreateVariableReferenceProposal(cu, fragment, binding, IProposalRelevance.ADD_CONSTRUCTOR_FROM_SUPER_CLASS);
		if (createVariableReferenceProposal.hasProposal()) {
			proposals.add(createVariableReferenceProposal);
		}
	}

	public static void addUnusedMemberProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		int problemId= problem.getProblemId();
		UnusedCodeFix fix= UnusedCodeFix.createUnusedMemberFix(context.getASTRoot(), problem, false);
		if (fix != null) {
			addProposal(context, proposals, fix);
		}

		if (problemId == IProblem.LocalVariableIsNeverUsed) {
			fix= UnusedCodeFix.createUnusedMemberFix(context.getASTRoot(), problem, true);
			addProposal(context, proposals, fix);
		}

		if (problemId == IProblem.ArgumentIsNeverUsed) {
			JavadocTagsSubProcessor.getUnusedAndUndocumentedParameterOrExceptionProposals(context, problem, proposals);
		}

		if (problemId == IProblem.UnusedPrivateField) {
			GetterSetterCorrectionSubProcessor.addGetterSetterProposal(context, problem, proposals, IProposalRelevance.GETTER_SETTER_UNUSED_PRIVATE_FIELD);
		}
	}

	public static void addUnusedTypeParameterProposal(IInvocationContext context, IProblemLocation problemLoc, Collection<ICommandAccess> proposals) {
		UnusedCodeFix fix= UnusedCodeFix.createUnusedTypeParameterFix(context.getASTRoot(), problemLoc);
		if (fix != null) {
			addProposal(context, proposals, fix);
		}

		JavadocTagsSubProcessor.getUnusedAndUndocumentedParameterOrExceptionProposals(context, problemLoc, proposals);
	}

	public static void addRedundantSuperInterfaceProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (!(selectedNode instanceof Name)) {
			return;
		}
		ASTNode node= ASTNodes.getNormalizedNode(selectedNode);

		ASTRewrite rewrite= ASTRewrite.create(node.getAST());
		rewrite.remove(node, null);

		String label= CorrectionMessages.LocalCorrectionsSubProcessor_remove_redundant_superinterface;
		Image image= PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.REMOVE_REDUNDANT_SUPER_INTERFACE, image);
		proposals.add(proposal);

	}

	private static void addProposal(IInvocationContext context, Collection<ICommandAccess> proposals, final UnusedCodeFix fix) {
		if (fix != null) {
			Image image= PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, fix.getCleanUp(), IProposalRelevance.UNUSED_MEMBER, image, context);
			proposals.add(proposal);
		}
	}

	public static void addSuperfluousSemicolonProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		String label= CorrectionMessages.LocalCorrectionsSubProcessor_removesemicolon_description;
		ReplaceCorrectionProposal proposal= new ReplaceCorrectionProposal(label, context.getCompilationUnit(), problem.getOffset(), problem.getLength(), "", IProposalRelevance.REMOVE_SEMICOLON); //$NON-NLS-1$
		proposals.add(proposal);
	}

	public static void addUnnecessaryCastProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		IProposableFix fix= UnusedCodeFix.createRemoveUnusedCastFix(context.getASTRoot(), problem);
		if (fix != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			Map<String, String> options= new Hashtable<>();
			options.put(CleanUpConstants.REMOVE_UNNECESSARY_CASTS, CleanUpOptions.TRUE);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, new UnnecessaryCodeCleanUp(options), IProposalRelevance.REMOVE_UNUSED_CAST, image, context);
			proposals.add(proposal);
		}
	}

	public static void addUnnecessaryInstanceofProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
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
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.UNNECESSARY_INSTANCEOF, image);
			proposals.add(proposal);
		}

	}

	public static void addIllegalQualifiedEnumConstantLabelProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		ASTNode coveringNode= problem.getCoveringNode(context.getASTRoot());

		ASTNode curr= ASTNodes.getUnparenthesedExpression(coveringNode);

		if (!(curr instanceof QualifiedName)) {
			return;
		}

		SimpleName simpleName= ((QualifiedName) curr).getName();
		final ASTRewrite rewrite= ASTRewrite.create(curr.getAST());
		rewrite.replace(coveringNode, simpleName, null);

		String label= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_replace_with_unqualified_enum_constant, BasicElementLabels.getJavaElementName(simpleName.getIdentifier()));
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.REPLACE_WITH_UNQUALIFIED_ENUM_CONSTANT, image);
		proposals.add(proposal);
	}

	public static void addUnnecessaryThrownExceptionProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
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
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);

			proposals.add(new ChangeMethodSignatureProposal(label, cu, selectedNode, binding, null, desc, IProposalRelevance.UNNECESSARY_THROW, image));
		}

		JavadocTagsSubProcessor.getUnusedAndUndocumentedParameterOrExceptionProposals(context, problem, proposals);
	}

	public static void addUnqualifiedFieldAccessProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		IProposableFix fix= CodeStyleFix.createAddFieldQualifierFix(context.getASTRoot(), problem);
		if (fix != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			Map<String, String> options= new HashMap<>();
			options.put(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS, CleanUpOptions.TRUE);
			options.put(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS, CleanUpOptions.TRUE);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, new CodeStyleCleanUp(options), IProposalRelevance.ADD_FIELD_QUALIFIER, image, context);
			proposal.setCommandId(ADD_FIELD_QUALIFICATION_ID);
			proposals.add(proposal);
		}
	}

	public static void addInvalidVariableNameProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		// hiding, redefined or future keyword

		CompilationUnit root= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(root);
		if (selectedNode instanceof MethodDeclaration) {
			selectedNode= ((MethodDeclaration) selectedNode).getName();
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

		LinkedNamesAssistProposal proposal= new LinkedNamesAssistProposal(name, context, nameNode, valueSuggestion);
		proposals.add(proposal);
	}

	public static void getInvalidOperatorProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
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

					Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CAST);
					ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.INVALID_OPERATOR, image);
					proposals.add(proposal);
				}
			}
		} else if (selectedNode instanceof InfixExpression && isBitOperation((((InfixExpression) selectedNode).getOperator()))) {
			// a & b == c -> (a & b) == c
			final CompareInBitWiseOpFinder opFinder= new CompareInBitWiseOpFinder(selectedNode);
			if (opFinder.getCompareExpression() != null) { // compare operation inside bit operations: set parents
				String label= CorrectionMessages.LocalCorrectionsSubProcessor_setparenteses_bitop_description;
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CAST);
				CUCorrectionProposal proposal= new CUCorrectionProposal(label, context.getCompilationUnit(), IProposalRelevance.INVALID_OPERATOR, image) {
					@Override
					protected void addEdits(IDocument document, TextEdit edit) throws CoreException {
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
				proposals.add(proposal);
			}
		}
	}

	private static boolean isBitOperation(InfixExpression.Operator op) {
		return op == InfixExpression.Operator.AND || op == InfixExpression.Operator.OR || op == InfixExpression.Operator.XOR;
	}

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

	public static void getUnnecessaryElseProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
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
		ASTNode placeholder= QuickAssistProcessor.getCopyOfInner(rewrite, ifStatement.getElseStatement(), false);
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
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.REMOVE_ELSE, image);
		proposals.add(proposal);
	}


	public static void getInterfaceExtendsClassProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
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
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.CHANGE_EXTENDS_TO_IMPLEMENTS, image);
			proposals.add(proposal);
		}
		{
			ASTRewrite rewrite= ASTRewrite.create(root.getAST());

			rewrite.set(typeDecl, TypeDeclaration.INTERFACE_PROPERTY, Boolean.TRUE, null);

			String typeName= typeDecl.getName().getIdentifier();
			String label= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_classtointerface_description, BasicElementLabels.getJavaElementName(typeName));
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.CHANGE_CLASS_TO_INTERFACE, image);
			proposals.add(proposal);
		}
	}

	public static void addTypeAsPermittedSubTypeProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws JavaModelException {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode == null) {
			return;
		}
		if (!ASTHelper.isSealedTypeSupportedInAST(selectedNode.getAST())) {
			return;
		}

		while (selectedNode.getParent() instanceof Type) {
			selectedNode= selectedNode.getParent();
		}
		if (selectedNode.getLocationInParent() != TypeDeclaration.SUPERCLASS_TYPE_PROPERTY
				&& selectedNode.getLocationInParent() != TypeDeclaration.SUPER_INTERFACE_TYPES_PROPERTY) {
			return;
		}
		TypeDeclaration subType= (TypeDeclaration) selectedNode.getParent();
		ITypeBinding subTypeBinding= subType.resolveBinding();
		IJavaElement sealedTypeElement= null;
		if (selectedNode instanceof SimpleType) {
			ITypeBinding typeBinding= ((SimpleType) selectedNode).resolveBinding();
			if (typeBinding != null) {
				sealedTypeElement= typeBinding.getJavaElement();
			}
		}
		if (!(sealedTypeElement instanceof IType)) {
			return;
		}
		IType sealedType= (IType) sealedTypeElement;
		if (sealedType.isBinary() || !sealedType.isSealed()) {
			return;
		}
		ICompilationUnit compilationUnit= sealedType.getCompilationUnit();
		CompilationUnitRewrite cuRewrite= new CompilationUnitRewrite(compilationUnit);
		TypeDeclaration declaration= ASTNodeSearchUtil.getTypeDeclarationNode(sealedType, cuRewrite.getRoot());
		if (declaration == null) {
			return;
		}

		AST ast= declaration.getAST();
		String subTypeName= subType.getName().getIdentifier();
		Type type= ast.newSimpleType(ast.newSimpleName(subTypeName));

		ASTRewrite astRewrite= cuRewrite.getASTRewrite();
		astRewrite.getListRewrite(declaration, TypeDeclaration.PERMITS_TYPES_PROPERTY).insertLast(type, null);

		String sealedTypeName= problem.getProblemArguments()[1];
		String label= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_declareSubClassAsPermitsSealedClass_description, new String[] { subTypeName, sealedTypeName });
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, compilationUnit, astRewrite, IProposalRelevance.DECLARE_SEALED_AS_DIRECT_SUPER_TYPE, image);
		ImportRewrite importRewrite= proposal.getImportRewrite();
		if (importRewrite == null) {
			importRewrite= StubUtility.createImportRewrite(compilationUnit, true);
		}

		ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(declaration.getRoot(), importRewrite);
		importRewrite.addImport(subTypeBinding, astRewrite.getAST(), importRewriteContext);
		proposal.setImportRewrite(importRewrite);
		proposals.add(proposal);
	}

	public static void addSealedAsDirectSuperTypeProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws JavaModelException {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode == null) {
			return;
		}
		if (!ASTHelper.isSealedTypeSupportedInAST(selectedNode.getAST())) {
			return;
		}

		while (selectedNode.getParent() instanceof Type) {
			selectedNode= selectedNode.getParent();
		}
		if (selectedNode.getLocationInParent() != TypeDeclaration.PERMITS_TYPES_PROPERTY) {
			return;
		}
		TypeDeclaration sealedType= (TypeDeclaration) selectedNode.getParent();

		IJavaElement permittedTypeElement= null;
		if (selectedNode instanceof SimpleType) {
			ITypeBinding typeBinding= ((SimpleType) selectedNode).resolveBinding();
			if (typeBinding != null) {
				permittedTypeElement= typeBinding.getJavaElement();
			}
		}
		if (!(permittedTypeElement instanceof IType)) {
			return;
		}

		ICompilationUnit compilationUnit= ((IType) permittedTypeElement).getCompilationUnit();
		if (compilationUnit == null) {
			return;
		}

		CompilationUnitRewrite cuRewrite= new CompilationUnitRewrite(compilationUnit);
		TypeDeclaration permittedTypeDeclaration= ASTNodeSearchUtil.getTypeDeclarationNode((IType) permittedTypeElement, cuRewrite.getRoot());
		if (permittedTypeDeclaration == null) {
			return;
		}

		AST ast= permittedTypeDeclaration.getAST();
		String sealedTypeName= sealedType.getName().getIdentifier();
		Type type= ast.newSimpleType(ast.newSimpleName(sealedTypeName));

		boolean isSealedInterface= sealedType.isInterface();

		ASTRewrite astRewrite= cuRewrite.getASTRewrite();
		if (isSealedInterface) {
			astRewrite.getListRewrite(permittedTypeDeclaration, TypeDeclaration.SUPER_INTERFACE_TYPES_PROPERTY).insertLast(type, null);
		} else {
			astRewrite.set(permittedTypeDeclaration, TypeDeclaration.SUPERCLASS_TYPE_PROPERTY, type, null);
		}

		String permittedTypeName= problem.getProblemArguments()[0];
		String label;
		if (isSealedInterface) {
			label= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_declareSealedAsDirectSuperInterface_description, new String[] { sealedTypeName, permittedTypeName });
		} else {
			label= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_declareSealedAsDirectSuperClass_description, new String[] { sealedTypeName, permittedTypeName });
		}
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, compilationUnit, astRewrite, IProposalRelevance.DECLARE_SEALED_AS_DIRECT_SUPER_TYPE, image);

		ImportRewrite importRewrite= proposal.getImportRewrite();
		if (importRewrite == null) {
			importRewrite= StubUtility.createImportRewrite(compilationUnit, true);
		}

		ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(permittedTypeDeclaration.getRoot(), importRewrite);
		importRewrite.addImport(sealedType.resolveBinding(), astRewrite.getAST(), importRewriteContext);
		proposal.setImportRewrite(importRewrite);

		proposals.add(proposal);
	}

	public static void getUnreachableCodeProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
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

			AssistContext assistContext= new AssistContext(context.getCompilationUnit(), infixExpression.getRightOperand().getStartPosition() - 1, 0);
			assistContext.setASTRoot(root);
			AdvancedQuickAssistProcessor.getSplitAndConditionProposals(assistContext, infixExpression, proposals);
			AdvancedQuickAssistProcessor.getSplitOrConditionProposals(assistContext, infixExpression, proposals);

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

	private static void addRemoveProposal(IInvocationContext context, ASTNode selectedNode, Collection<ICommandAccess> proposals) {
		ASTRewrite rewrite= ASTRewrite.create(selectedNode.getAST());
		rewrite.remove(selectedNode, null);

		String label= CorrectionMessages.LocalCorrectionsSubProcessor_removeunreachablecode_description;
		addRemoveProposal(context, rewrite, label, proposals);
	}

	private static void addRemoveIncludingConditionProposal(IInvocationContext context, ASTNode toRemove, ASTNode replacement, Collection<ICommandAccess> proposals) {
		Image image= PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
		String label= CorrectionMessages.LocalCorrectionsSubProcessor_removeunreachablecode_including_condition_description;
		AST ast= toRemove.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.REMOVE_UNREACHABLE_CODE_INCLUDING_CONDITION, image);

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

		proposals.add(proposal);
	}

	private static void addRemoveProposal(IInvocationContext context, ASTRewrite rewrite, String label, Collection<ICommandAccess> proposals) {
		Image image= PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 10, image);
		proposals.add(proposal);
	}

	public static void getUnusedObjectAllocationProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
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
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.THROW_ALLOCATED_OBJECT, image);
				proposal.setEndPosition(rewrite.track(throwStatement));
				proposals.add(proposal);
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
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
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
				LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, context.getCompilationUnit(), rewrite, relevance, image);
				proposal.setEndPosition(rewrite.track(returnStatement));
				proposals.add(proposal);
			}

			{
				ASTRewrite rewrite= ASTRewrite.create(ast);
				rewrite.remove(parent, null);

				String label= CorrectionMessages.LocalCorrectionsSubProcessor_remove_allocated_description;
				Image image= PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
				ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.REMOVE_UNUSED_ALLOCATED_OBJECT, image);
				proposals.add(proposal);
			}

		}

		QuickAssistProcessor.getAssignToVariableProposals(context, selectedNode, null, proposals);
	}

	public static void getAssignmentHasNoEffectProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
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
				proposals.add(createNoSideEffectProposal(context, (SimpleName) assignedNode, fieldBinding, label, IProposalRelevance.QUALIFY_LHS));
			}
			if (assignExpression instanceof SimpleName) {
				String label= CorrectionMessages.LocalCorrectionsSubProcessor_qualify_right_hand_side_description;
				proposals.add(createNoSideEffectProposal(context, (SimpleName) assignExpression, fieldBinding, label, IProposalRelevance.QUALIFY_RHS));
			}
		}

		if (binding == fieldBinding && ASTResolving.findParentBodyDeclaration(selectedNode) instanceof MethodDeclaration) {
			SimpleName simpleName= (SimpleName) ((assignedNode instanceof SimpleName) ? assignedNode : assignExpression);
			String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createparameter_description, BasicElementLabels.getJavaElementName(simpleName.getIdentifier()));
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
			proposals.add(new NewVariableCorrectionProposal(label, context.getCompilationUnit(), NewVariableCorrectionProposal.PARAM, simpleName, null, IProposalRelevance.CREATE_PARAMETER, image));
		}


	}

	private static ASTRewriteCorrectionProposal createNoSideEffectProposal(IInvocationContext context, SimpleName nodeToQualify, IVariableBinding fieldBinding, String label, int relevance) {
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


		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		return new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, relevance, image);
	}

	public static void addValueForAnnotationProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		ICompilationUnit cu= context.getCompilationUnit();
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode instanceof Annotation) {
			Annotation annotation= (Annotation) selectedNode;
			if (annotation.resolveTypeBinding() == null) {
				return;
			}
			MissingAnnotationAttributesProposal proposal= new MissingAnnotationAttributesProposal(cu, annotation, 10);
			proposals.add(proposal);
		}
	}

	public static void addTypePrametersToRawTypeReference(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		IProposableFix fix= Java50Fix.createRawTypeReferenceFix(context.getASTRoot(), problem);
		if (fix != null) {
			for (ICommandAccess element : proposals) {
				if (element instanceof FixCorrectionProposal) {
					FixCorrectionProposal fixProp= (FixCorrectionProposal) element;
					if (RAW_TYPE_REFERENCE_ID.equals(fixProp.getCommandId())) {
						return;
					}
				}
			}
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			Map<String, String> options= new Hashtable<>();
			options.put(CleanUpConstants.VARIABLE_DECLARATION_USE_TYPE_ARGUMENTS_FOR_RAW_TYPE_REFERENCES, CleanUpOptions.TRUE);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, new Java50CleanUp(options), IProposalRelevance.RAW_TYPE_REFERENCE, image, context);
			proposal.setCommandId(RAW_TYPE_REFERENCE_ID);
			proposals.add(proposal);
		}

		//Infer Generic Type Arguments... proposal
		boolean hasInferTypeArgumentsProposal= false;
		for (ICommandAccess completionProposal : proposals) {
			if (completionProposal instanceof ChangeCorrectionProposal) {
				if (IJavaEditorActionDefinitionIds.INFER_TYPE_ARGUMENTS_ACTION.equals(((ChangeCorrectionProposal) completionProposal).getCommandId())) {
					hasInferTypeArgumentsProposal= true;
					break;
				}
			}
		}
		if (!hasInferTypeArgumentsProposal) {
			final ICompilationUnit cu= context.getCompilationUnit();
			ChangeCorrectionProposal proposal= new ChangeCorrectionProposal(CorrectionMessages.LocalCorrectionsSubProcessor_InferGenericTypeArguments, null,
					IProposalRelevance.INFER_GENERIC_TYPE_ARGUMENTS, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE)) {
				@Override
				public void apply(IDocument document) {
					IEditorInput input= new FileEditorInput((IFile) cu.getResource());
					IWorkbenchPage p= JavaPlugin.getActivePage();
					if (p == null)
						return;

					IEditorPart part= p.findEditor(input);
					if (!(part instanceof JavaEditor))
						return;

					IEditorSite site= ((JavaEditor) part).getEditorSite();
					InferTypeArgumentsAction action= new InferTypeArgumentsAction(site);
					action.run(new StructuredSelection(cu));
				}

				@Override
				public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
					return CorrectionMessages.LocalCorrectionsSubProcessor_InferGenericTypeArguments_description;
				}
			};
			proposal.setCommandId(IJavaEditorActionDefinitionIds.INFER_TYPE_ARGUMENTS_ACTION);
			proposals.add(proposal);
		}

		addTypeArgumentsFromContext(context, problem, proposals);
	}

	private static void addTypeArgumentsFromContext(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
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
			ITypeBinding simpleBinding= binding;
			if (simpleBinding.isArray()) {
				simpleBinding= simpleBinding.getElementType();
			}
			simpleBinding= simpleBinding.getTypeDeclaration();

			if (!simpleBinding.isRecovered()) {
				if (binding.isParameterizedType() && (node.getParent() instanceof SimpleType || node.getParent() instanceof NameQualifiedType) && !(node.getParent().getParent() instanceof Type)) {
					proposals.add(UnresolvedElementsSubProcessor.createTypeRefChangeFullProposal(cu, binding, node, IProposalRelevance.TYPE_ARGUMENTS_FROM_CONTEXT, TypeLocation.TYPE_ARGUMENT));
				}
			}
		} else {
			ASTNode normalizedNode= ASTNodes.getNormalizedNode(node);
			if (!(normalizedNode.getParent() instanceof Type) && node.getParent() != normalizedNode) {
				ITypeBinding normBinding= ASTResolving.guessBindingForTypeReference(normalizedNode);
				if (normBinding != null && !normBinding.isRecovered()) {
					proposals.add(UnresolvedElementsSubProcessor.createTypeRefChangeFullProposal(cu, normBinding, normalizedNode, IProposalRelevance.TYPE_ARGUMENTS_FROM_CONTEXT,
							TypeLocation.TYPE_ARGUMENT));
				}
			}
		}
	}

	public static void addRemoveRedundantTypeArgumentsProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		IProposableFix fix= TypeParametersFix.createRemoveRedundantTypeArgumentsFix(context.getASTRoot(), problem);
		if (fix != null) {
			Image image= PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
			Map<String, String> options= new HashMap<>();
			options.put(CleanUpConstants.REMOVE_REDUNDANT_TYPE_ARGUMENTS, CleanUpOptions.TRUE);
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, new TypeParametersCleanUp(options), IProposalRelevance.REMOVE_REDUNDANT_TYPE_ARGUMENTS, image, context);
			proposals.add(proposal);
		}
	}

	public static void addFallThroughProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode instanceof SwitchCase && selectedNode.getLocationInParent() == SwitchStatement.STATEMENTS_PROPERTY) {
			AST ast= selectedNode.getAST();
			ASTNode parent= selectedNode.getParent();

			// insert break:
			ASTRewrite rewrite= ASTRewrite.create(ast);
			ListRewrite listRewrite= rewrite.getListRewrite(parent, SwitchStatement.STATEMENTS_PROPERTY);
			listRewrite.insertBefore(ast.newBreakStatement(), selectedNode, null);

			String label= CorrectionMessages.LocalCorrectionsSubProcessor_insert_break_statement;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.INSERT_BREAK_STATEMENT, image);
			proposals.add(proposal);

			// insert //$FALL-THROUGH$:
			rewrite= ASTRewrite.create(ast);
			rewrite.setTargetSourceRangeComputer(new NoCommentSourceRangeComputer());
			listRewrite= rewrite.getListRewrite(parent, SwitchStatement.STATEMENTS_PROPERTY);
			ASTNode fallThroughComment= rewrite.createStringPlaceholder("//$FALL-THROUGH$", ASTNode.EMPTY_STATEMENT); //$NON-NLS-1$
			listRewrite.insertBefore(fallThroughComment, selectedNode, null);

			label= CorrectionMessages.LocalCorrectionsSubProcessor_insert_fall_through;
			image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.INSERT_FALL_THROUGH, image);
			proposals.add(proposal);
		}
	}

	public static void addCasesOmittedProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
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
					Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
					ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, IProposalRelevance.INSERT_CASES_OMITTED, image);
					proposals.add(proposal);
					break;
				}
			}
		}
	}

	public static void addDeprecatedFieldsToMethodsProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
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
						Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
						ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, astRewrite, IProposalRelevance.REPLACE_FIELD_ACCESS_WITH_METHOD, image);
						proposal.setImportRewrite(importRewrite);
						proposals.add(proposal);
					}
				}
			}
		}
	}

	private static Map<String, String[]> resolveMap;

	private static String[] getMethod(String fieldName) {
		if (resolveMap == null) {
			resolveMap= new HashMap<>();
			resolveMap.put("java.util.Collections.EMPTY_MAP", new String[] { "java.util.Collections", "emptyMap" }); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
			resolveMap.put("java.util.Collections.EMPTY_SET", new String[] { "java.util.Collections", "emptySet" }); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
			resolveMap.put("java.util.Collections.EMPTY_LIST", new String[] { "java.util.Collections", "emptyList" });//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		}
		return resolveMap.get(fieldName);
	}

	public static void getMissingEnumConstantCaseProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		for (ICommandAccess proposal : proposals) {
			if (proposal instanceof ChangeCorrectionProposal) {
				if (CorrectionMessages.LocalCorrectionsSubProcessor_add_missing_cases_description.equals(((ChangeCorrectionProposal) proposal).getName())) {
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
			boolean hasDefault= evaluateMissingSwitchCases(binding, statements, missingEnumCases);
			if (missingEnumCases.size() == 0 && hasDefault)
				return;

			createMissingCaseProposals(context, parent, missingEnumCases, proposals);
		}
	}

	@SuppressWarnings("deprecation")
	public static boolean evaluateMissingSwitchCases(ITypeBinding enumBindings, List<Statement> switchStatements, ArrayList<String> enumConstNames) {
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
	public static void createMissingCaseProposals(IInvocationContext context, ASTNode parent, ArrayList<String> enumConstNames, Collection<ICommandAccess> proposals) {
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
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);

		if (enumConstNames.size() > 0) {
			ASTRewrite astRewrite= ASTRewrite.create(ast);
			ListRewrite listRewrite;
			if (parent instanceof SwitchStatement) {
				listRewrite= astRewrite.getListRewrite(parent, SwitchStatement.STATEMENTS_PROPERTY);
			} else {
				listRewrite= astRewrite.getListRewrite(parent, SwitchExpression.STATEMENTS_PROPERTY);
			}

			String label= CorrectionMessages.LocalCorrectionsSubProcessor_add_missing_cases_description;
			LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, context.getCompilationUnit(), astRewrite, IProposalRelevance.ADD_MISSING_CASE_STATEMENTS, image);

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
			proposals.add(proposal);
		}
		if (!hasDefault) {
			createMissingDefaultProposal(context, parent, image, proposals);
		}
	}

	private static ThrowStatement getThrowForUnsupportedCase(Expression switchExpr, AST ast, ASTRewrite astRewrite) {
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

	public static void addMissingDefaultCaseProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
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
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			createMissingDefaultProposal(context, parent, image, proposals);
		}
	}

	@SuppressWarnings("deprecation")
	private static void createMissingDefaultProposal(IInvocationContext context, ASTNode parent, Image image, Collection<ICommandAccess> proposals) {
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
		LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, context.getCompilationUnit(), astRewrite, IProposalRelevance.ADD_MISSING_DEFAULT_CASE, image);

		SwitchCase newSwitchCase= ast.newSwitchCase();
		listRewrite.insertLast(newSwitchCase, null);

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
			}
		} else {
			newSwitchCase.setExpression(null);
			listRewrite.insertLast(ast.newBreakStatement(), null);
		}

		proposals.add(proposal);
	}

	private static ThrowStatement getThrowForUnexpectedDefault(Expression switchExpression, AST ast, ASTRewrite astRewrite) {
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

	public static void addMissingHashCodeProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		final ICompilationUnit cu= context.getCompilationUnit();

		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(astRoot);
		if (!(selectedNode instanceof Name)) {
			return;
		}

		StructuralPropertyDescriptor locationInParent= selectedNode.getLocationInParent();
		if (locationInParent != TypeDeclaration.NAME_PROPERTY && locationInParent != EnumDeclaration.NAME_PROPERTY) {
			return;
		}

		AbstractTypeDeclaration typeDeclaration= (AbstractTypeDeclaration) selectedNode.getParent();

		ITypeBinding binding= typeDeclaration.resolveBinding();
		if (binding == null || binding.getSuperclass() == null) {
			return;
		}
		final IType type= (IType) binding.getJavaElement();

		boolean hasInstanceFields= false;
		for (IVariableBinding declaredField : binding.getDeclaredFields()) {
			if (!Modifier.isStatic(declaredField.getModifiers())) {
				hasInstanceFields= true;
				break;
			}
		}
		if (hasInstanceFields) {
			//Generate hashCode() and equals()... proposal
			String label= CorrectionMessages.LocalCorrectionsSubProcessor_generate_hashCode_equals_description;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ChangeCorrectionProposal proposal= new ChangeCorrectionProposal(label, null, IProposalRelevance.GENERATE_HASHCODE_AND_EQUALS, image) {
				@Override
				public void apply(IDocument document) {
					IEditorInput input= new FileEditorInput((IFile) cu.getResource());
					IWorkbenchPage p= JavaPlugin.getActivePage();
					if (p == null)
						return;

					IEditorPart part= p.findEditor(input);
					if (!(part instanceof JavaEditor))
						return;

					IEditorSite site= ((JavaEditor) part).getEditorSite();
					GenerateHashCodeEqualsAction action= new GenerateHashCodeEqualsAction(site);
					action.run(new StructuredSelection(type));
				}

				@Override
				public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
					return CorrectionMessages.LocalCorrectionsSubProcessor_generate_hashCode_equals_additional_info;
				}
			};
			proposals.add(proposal);
		}


		//Override hashCode() proposal
		IMethodBinding superHashCode= Bindings.findMethodInHierarchy(binding, "hashCode", new ITypeBinding[0]); //$NON-NLS-1$
		if (superHashCode == null) {
			return;
		}

		String label= CorrectionMessages.LocalCorrectionsSubProcessor_override_hashCode_description;
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC);

		ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
		LinkedCorrectionProposal proposal2= new LinkedCorrectionProposal(label, cu, rewrite, IProposalRelevance.OVERRIDE_HASHCODE, image);
		ImportRewrite importRewrite= proposal2.createImportRewrite(astRoot);

		final CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(cu);

		try {
			ImportRewriteContext importContext= new ContextSensitiveImportRewriteContext(astRoot, problem.getOffset(), importRewrite);
			MethodDeclaration hashCode= StubUtility2.createImplementationStub(cu, rewrite, importRewrite, importContext, superHashCode, binding, settings, false, null);
			BodyDeclarationRewrite.create(rewrite, typeDeclaration).insert(hashCode, null);

			proposal2.setEndPosition(rewrite.track(hashCode));

		} catch (CoreException e) {
			JavaPlugin.log(e);
		}


		proposals.add(proposal2);
	}

	public static void getGenerateForLoopProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		ASTNode coveringNode= problem.getCoveringNode(context.getASTRoot());
		if (coveringNode != null) {
			QuickAssistProcessor.getGenerateForLoopProposals(context, coveringNode, null, proposals);
		}
	}

	public static void getConvertLambdaToAnonymousClassCreationsProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		ASTNode coveringNode= problem.getCoveringNode(context.getASTRoot());
		if (coveringNode != null) {
			QuickAssistProcessor.getConvertLambdaToAnonymousClassCreationsProposals(context, coveringNode, proposals);
		}
	}

	public static void addOverrideDefaultMethodProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
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

	private static void addOverrideProposal(ASTNode typeNode, ITypeBinding typeBinding, String methodName, String[] parameters, String superType,
			IInvocationContext context, Collection<ICommandAccess> proposals) {
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
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC);

		CompilationUnit astRoot= context.getASTRoot();
		ASTRewrite rewrite= ASTRewrite.create(astRoot.getAST());
		ICompilationUnit cu= context.getCompilationUnit();
		LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, cu, rewrite, IProposalRelevance.OVERRIDE_DEFAULT_METHOD, image);

		ImportRewrite importRewrite= proposal.createImportRewrite(astRoot);
		ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(astRoot, typeNode.getStartPosition(), importRewrite);
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(cu);
		try {
			MethodDeclaration stub= StubUtility2.createImplementationStub(cu, rewrite, importRewrite, importRewriteContext, methodToOverride, typeBinding, settings,
					typeBinding.isInterface(), new NodeFinder(astRoot, typeNode.getStartPosition(), 0).getCoveringNode());
			BodyDeclarationRewrite.create(rewrite, typeNode).insert(stub, null);

			proposal.setEndPosition(rewrite.track(stub));
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}

		proposals.add(proposal);
	}

	public static void addServiceProviderProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws CoreException {
		ASTNode node= problem.getCoveredNode(context.getASTRoot());
		if (!(node instanceof Name) && !(node.getParent() instanceof ProvidesDirective)) {
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
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC);
			proposals.add(new NewProviderMethodDeclaration(
					Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_add_provider_method_description, type.getElementName()),
					targetCU, context.getASTRoot(), targetBinding,
					IProposalRelevance.CREATE_METHOD, image, type));
		}
	}

	public static void addServiceProviderConstructorProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws CoreException {
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
				proposals.add(new ModifierChangeCorrectionProposal(label, targetCU, targetMethodBinding, targetMethodDecl.getName(),
						include, exclude, IProposalRelevance.CHANGE_VISIBILITY_TO_NON_PRIVATE,
						JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE)));
			} else {
				// no-arg constructor does not exist, need to create it
				String[] args= new String[] { org.eclipse.jdt.internal.ui.text.correction.ASTResolving
						.getMethodSignature(org.eclipse.jdt.internal.ui.text.correction.ASTResolving.getTypeSignature(targetBinding), new ITypeBinding[0], false) };
				String label= Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_createconstructor_description, args);
				Image image= JavaElementImageProvider.getDecoratedImage(JavaPluginImages.DESC_MISC_PUBLIC, JavaElementImageDescriptor.CONSTRUCTOR, JavaElementImageProvider.SMALL_SIZE);
				proposals.add(new NewMethodCorrectionProposal(label, targetCU, targetRoot, new ArrayList<>(), targetBinding, IProposalRelevance.CREATE_CONSTRUCTOR, image));
			}
		}
	}

	private LocalCorrectionsSubProcessor() {
	}

}
