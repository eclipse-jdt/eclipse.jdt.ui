/*******************************************************************************
 * Copyright (c) 2000, 2025 IBM Corporation and others.
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
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.part.FileEditorInput;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.manipulation.CUCorrectionProposalCore;

import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.core.manipulation.dom.NecessaryParenthesesChecker;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility2Core;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.BodyDeclarationRewrite;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.IProposableFix;
import org.eclipse.jdt.internal.corext.fix.Java50FixCore;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;
import org.eclipse.jdt.ui.actions.GenerateHashCodeEqualsAction;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.actions.InferTypeArgumentsAction;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposalCore;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ChangeCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.fix.Java50CleanUp;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.nls.ExternalizeWizard;
import org.eclipse.jdt.internal.ui.text.correction.proposals.AssignToVariableAssistProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.AssignToVariableAssistProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeMethodSignatureProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ConstructorFromSuperclassProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ConstructorFromSuperclassProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CreateNewObjectProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CreateNewObjectProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CreateObjectReferenceProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CreateObjectReferenceProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CreateVariableReferenceProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CreateVariableReferenceProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.GenerateForLoopAssistProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.GenerateForLoopAssistProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedNamesAssistProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedNamesAssistProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.MissingAnnotationAttributesProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.MissingAnnotationAttributesProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ModifierChangeCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ModifierChangeCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewCUUsingWizardProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewLocalVariableCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewLocalVariableCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewMethodCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewMethodCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewProviderMethodDeclaration;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewProviderMethodDeclarationCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewVariableCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewVariableCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.RefactoringCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.RefactoringCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ReplaceCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ReplaceCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.util.ASTHelper;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

public class LocalCorrectionsSubProcessor extends LocalCorrectionsBaseSubProcessor<ICommandAccess>{

	private static final String RAW_TYPE_REFERENCE_ID= "org.eclipse.jdt.ui.correction.rawTypeReference"; //$NON-NLS-1$

	public static void addUncaughtExceptionProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws CoreException {
		new LocalCorrectionsSubProcessor().getUncaughtExceptionProposals(context, problem, proposals);
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

		new LocalCorrectionsSubProcessor().getAddNLSTagProposalsCore(context, problem, proposals);
	}

	public static void getUnnecessaryNLSTagProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws CoreException {
		new LocalCorrectionsSubProcessor().getUnnecessaryNLSTagProposalsCore(context, problem, proposals);
	}

	/*
	 * Fix instance accesses and indirect (static) accesses to fields/methods
	 */
	public static void addCorrectAccessToStaticProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws CoreException {
		new LocalCorrectionsSubProcessor().getCorrectAccessToStaticProposals(context, problem, proposals, new ModifierCorrectionSubProcessor());
	}

	public static void addUnimplementedMethodsProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().getUnimplementedMethodsProposals(context, problem, proposals);
	}

	public static void addUninitializedLocalVariableProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().getUninitializedLocalVariableProposal(context, problem, proposals);
	}

	public static void addConstructorFromSuperclassProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws CoreException {
		new LocalCorrectionsSubProcessor().getConstructorFromSuperclassProposal(context, problem, proposals);
	}

	public static void addNewObjectProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws CoreException {
		new LocalCorrectionsSubProcessor().getNewObjectProposal(context, problem, proposals);
	}

	public static void addObjectReferenceProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws CoreException {
		new LocalCorrectionsSubProcessor().getObjectReferenceProposal(context, problem, proposals);
	}

	public static void addVariableReferenceProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws CoreException {
		new LocalCorrectionsSubProcessor().getVariableReferenceProposal(context, problem, proposals);
	}

	public static void addUnusedMemberProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().getUnusedMemberProposal(context, problem, proposals,
				new JavadocTagsSubProcessor(), new GetterSetterCorrectionSubProcessor());
	}

	public static void addUnusedTypeParameterProposal(IInvocationContext context, IProblemLocation problemLoc, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().getUnusedTypeParameterProposal(context, problemLoc, proposals, new JavadocTagsSubProcessor());
	}

	public static void addRedundantSuperInterfaceProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().getRedundantSuperInterfaceProposal(context, problem, proposals);
	}

	public static void addSuperfluousSemicolonProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().getSuperfluousSemicolonProposal(context, problem, proposals);
	}

	public static void addUnnecessaryCastProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().getUnnecessaryCastProposal(context, problem, proposals);
	}

	public static void addUnnecessaryInstanceofProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().getUnnecessaryInstanceofProposal(context, problem, proposals);
	}

	public static void addIllegalQualifiedEnumConstantLabelProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().getIllegalQualifiedEnumConstantLabelProposal(context, problem, proposals);
	}

	public static void addUnnecessaryThrownExceptionProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().getUnnecessaryThrownExceptionProposal(context, problem, proposals, new JavadocTagsSubProcessor());
	}

	public static void addUnqualifiedFieldAccessProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().getUnqualifiedFieldAccessProposal(context, problem, proposals);
	}

	public static void addInvalidVariableNameProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().getInvalidVariableNameProposals(context, problem, proposals);
	}

	public static void getInvalidOperatorProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().getInvalidOperatorProposalsBase(context, problem, proposals);
	}

	public static void getUnnecessaryElseProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().getUnnecessaryElseProposalsBase(context, problem, proposals);
	}

	public static void getInterfaceExtendsClassProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().getInterfaceExtendsClassProposalsBase(context, problem, proposals);
	}

	public static void createNewTypeAsPermittedSubTypeProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals, int relevance) throws JavaModelException {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode == null) {
			return;
		}
		if (!ASTHelper.isSealedTypeSupportedInAST(selectedNode.getAST())) {
			return;
		}

		if (selectedNode.getParent() instanceof TypeDeclaration) {
			selectedNode= selectedNode.getParent();
		}
		if (!(selectedNode instanceof TypeDeclaration)){
			return;
		}
		TypeDeclaration type= (TypeDeclaration) selectedNode;
		IJavaElement sealedTypeElement= null;
		ITypeBinding typeBinding= type.resolveBinding();
		if (typeBinding != null) {
			sealedTypeElement= typeBinding.getJavaElement();
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
		IPackageFragment fragment= sealedType.getPackageFragment();

		if (sealedType.isInterface()) {
			proposals.add(new NewCUUsingWizardProposal(compilationUnit, null, NewCUUsingWizardProposal.K_INTERFACE, fragment, typeBinding, relevance + 4, true));
			proposals.add(new NewCUUsingWizardProposal(compilationUnit, null, NewCUUsingWizardProposal.K_INTERFACE, sealedTypeElement,  typeBinding, relevance + 4, false));
			proposals.add(new NewCUUsingWizardProposal(compilationUnit, null, NewCUUsingWizardProposal.K_RECORD, fragment,  typeBinding, relevance + 5, true));
			proposals.add(new NewCUUsingWizardProposal(compilationUnit, null, NewCUUsingWizardProposal.K_RECORD, sealedTypeElement,  typeBinding, relevance + 5, false));
		}
		proposals.add(new NewCUUsingWizardProposal(compilationUnit, null, NewCUUsingWizardProposal.K_CLASS, fragment,  typeBinding, relevance + 6, true));
		proposals.add(new NewCUUsingWizardProposal(compilationUnit, null, NewCUUsingWizardProposal.K_CLASS, sealedTypeElement,  typeBinding, relevance + 6, false));
	}

	public static void addTypeAsPermittedSubTypeProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().getTypeAsPermittedSubTypeProposal(context, problem, proposals);
	}

	public static void addSealedAsDirectSuperTypeProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().getSealedAsDirectSuperTypeProposal(context, problem, proposals);
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
		Image image= ISharedImages.get().getImage(ISharedImages.IMG_TOOL_DELETE);
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
		Image image= ISharedImages.get().getImage(ISharedImages.IMG_TOOL_DELETE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 10, image);
		proposals.add(proposal);
	}

	public static void getUnusedObjectAllocationProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().getUnusedObjectAllocationProposalsBase(context, problem, proposals);
	}

	public static boolean getAssignToVariableProposals(IInvocationContext context, ASTNode node, IProblemLocation[] locations, Collection<ICommandAccess> resultingCollections) {
		return new LocalCorrectionsSubProcessor().getAssignToVariableProposalsBase(context, node, locations, resultingCollections);
	}

	public static void getAssignmentHasNoEffectProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().getAssignmentHasNoEffectProposalsBase(context, problem, proposals);
	}

	public static void getExpressionShouldBeAVariableProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().getExpressionShouldBeAVariableProposalsBase(context, problem, proposals);
	}

	public static void addValueForAnnotationProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().getValueForAnnotationProposals(context, problem, proposals);
	}

	public static void addTypePrametersToRawTypeReference(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		IProposableFix fix= Java50FixCore.createRawTypeReferenceFix(context.getASTRoot(), problem);
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
			if (referencesVar(problem, context.getASTRoot()))  {
				return;
			}
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

	private static boolean referencesVar(IProblemLocation problem, CompilationUnit compilationUnit) {
		ASTNode node= problem.getCoveredNode(compilationUnit);
		if (node instanceof ClassInstanceCreation) {
			Type rawReference= (Type)node.getStructuralProperty(ClassInstanceCreation.TYPE_PROPERTY);
			return rawReference.isVar();
		} else if (node instanceof SimpleName simpleName) {
			SimpleType rawReference= Java50FixCore.getRawReference(simpleName, compilationUnit);
			if (rawReference != null) {
				return rawReference.isVar();
			}
			ASTNode ancestor= ASTNodes.getFirstAncestorOrNull(node, VariableDeclarationStatement.class, FieldDeclaration.class, SingleVariableDeclaration.class, MethodDeclaration.class);
			if (ancestor != null) {
				if (ancestor instanceof VariableDeclarationStatement varStmt) {
					ASTNode result= (ASTNode)varStmt.getStructuralProperty(VariableDeclarationStatement.TYPE_PROPERTY);
					if (Java50FixCore.isRawTypeReference(result)) {
						return ((SimpleType) result).isVar();
					}
				} else if (ancestor instanceof FieldDeclaration fieldDecl) {
					ASTNode result= (ASTNode)fieldDecl.getStructuralProperty(FieldDeclaration.TYPE_PROPERTY);
					if (Java50FixCore.isRawTypeReference(result)) {
						return ((SimpleType) result).isVar();
					}
				} else if (ancestor instanceof SingleVariableDeclaration singleVarDecl) {
					ASTNode result= (ASTNode)singleVarDecl.getStructuralProperty(SingleVariableDeclaration.TYPE_PROPERTY);
					if (Java50FixCore.isRawTypeReference(result)) {
						return ((SimpleType) result).isVar();
					}
				}
			}
		} else if (node instanceof MethodInvocation) {
			MethodInvocation invocation= (MethodInvocation)node;
			SimpleType rawReference= Java50FixCore.getRawReference(invocation, compilationUnit);
			if (rawReference != null) {
				return rawReference.isVar();
			}
		}
		return false;
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
					proposals.add(UnresolvedElementsSubProcessor.getTypeRefChangeFullProposal(cu, binding, node, IProposalRelevance.TYPE_ARGUMENTS_FROM_CONTEXT, TypeLocation.TYPE_ARGUMENT));
				}
			}
		} else {
			ASTNode normalizedNode= ASTNodes.getNormalizedNode(node);
			if (!(normalizedNode.getParent() instanceof Type) && node.getParent() != normalizedNode) {
				ITypeBinding normBinding= ASTResolving.guessBindingForTypeReference(normalizedNode);
				if (normBinding != null && !normBinding.isRecovered()) {
					proposals.add(UnresolvedElementsSubProcessor.getTypeRefChangeFullProposal(cu, normBinding, normalizedNode, IProposalRelevance.TYPE_ARGUMENTS_FROM_CONTEXT,
							TypeLocation.TYPE_ARGUMENT));
				}
			}
		}
	}

	public static void addRemoveRedundantTypeArgumentsProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().getRemoveRedundantTypeArgumentsProposals(context, problem, proposals);
	}

	public static void addFallThroughProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().getFallThroughProposals(context, problem, proposals);
	}

	public static void addCasesOmittedProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().getCasesOmittedProposals(context, problem, proposals);
	}

	public static void addDeprecatedFieldsToMethodsProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().getDeprecatedFieldsToMethodsProposals(context, problem, proposals);
	}

	public static void getMissingEnumConstantCaseProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().getMissingEnumConstantCaseProposalsBase(context, problem, proposals);
	}

	public static boolean evaluateMissingSwitchCases(ITypeBinding enumBindings, List<Statement> switchStatements, ArrayList<String> enumConstNames) {
		return new LocalCorrectionsSubProcessor().evaluateMissingSwitchCasesBase(enumBindings, switchStatements, enumConstNames);
	}

	public static void createMissingCaseProposals(IInvocationContext context, ASTNode parent, ArrayList<String> enumConstNames, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().createMissingCaseProposalsBase(context, parent, null, enumConstNames, proposals);
	}

	public static void removeDefaultCaseProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().removeDefaultCaseProposalBase(context, problem, proposals);
	}

	public static void addPermittedTypesProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().getPermittedTypesProposal(context, problem, proposals);
	}

	public static void addMissingDefaultCaseProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().getMissingDefaultCaseProposal(context, problem, proposals);
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
			MethodDeclaration hashCode= StubUtility2Core.createImplementationStub(cu, rewrite, importRewrite, importContext, superHashCode, binding, settings, false, null);
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
			getGenerateForLoopProposals(context, coveringNode, null, proposals);
		}
	}

	public static boolean getGenerateForLoopProposals(IInvocationContext context, ASTNode coveringNode, IProblemLocation[] problems, Collection<ICommandAccess> proposals) {
		return new LocalCorrectionsSubProcessor().getGenerateForLoopProposalsBase(context, coveringNode, problems, proposals);
	}

	public static void getTryWithResourceProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		ASTNode coveringNode= problem.getCoveringNode(context.getASTRoot());
		if (coveringNode != null) {
			try {
				ArrayList<ASTNode> coveredNodes= AdvancedQuickAssistProcessor.getFullyCoveredNodes(context, coveringNode);
				QuickAssistProcessor.getTryWithResourceProposals(context, coveringNode, coveredNodes, proposals);
			} catch (IllegalArgumentException | CoreException e) {
				// do nothing
			}
		}
	}

	public static void addOverrideDefaultMethodProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new LocalCorrectionsSubProcessor().getOverrideDefaultMethodProposal(context, problem, proposals);
	}

	public static void addServiceProviderProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws CoreException {
		new LocalCorrectionsSubProcessor().getServiceProviderProposal(context, problem, proposals);
	}

	public static void addServiceProviderConstructorProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws CoreException {
		new LocalCorrectionsSubProcessor().getServiceProviderConstructorProposals(context, problem, proposals);
	}

	private LocalCorrectionsSubProcessor() {
	}

	@Override
	protected ICommandAccess refactoringCorrectionProposalToT(RefactoringCorrectionProposalCore core, int uid) {
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
		return new RefactoringCorrectionProposal(core, image);
	}

	@Override
	protected ICommandAccess linkedCorrectionProposalToT(LinkedCorrectionProposalCore core, int uid) {
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
		switch (uid) {
			case INITIALIZE_VARIABLE, RETURN_ALLOCATED_OBJECT, ADD_MISSING_CASE,
				CREATE_DEFAULT, ADD_PERMITTED_TYPES, CORRECTION_CHANGE_ID -> {
				image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			}
			case ADD_OVERRIDE -> {
				image= JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC);
			}
		}
		return new LinkedCorrectionProposal(core, image);
	}

	@Override
	protected ICommandAccess changeMethodSignatureProposalToT(ChangeMethodSignatureProposalCore core, int uid) {
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
		return new ChangeMethodSignatureProposal(core, image);
	}

	@Override
	protected ICommandAccess assignToVariableAssistProposalToT(AssignToVariableAssistProposalCore core) {
		return new AssignToVariableAssistProposal(core);
	}

	@Override
	protected ICommandAccess fixCorrectionProposalToT(FixCorrectionProposalCore core, int uid) {
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		switch (uid) {
			case STATIC_INDIRECT_ACCESS, STATIC_NON_STATIC_ACCESS_USING_TYPE,
				STATIC_INSTANCE_ACCESS, REMOVE_UNNECESSARY_CAST, UNQUALIFIED_FIELD_ACCESS,
				ADD_UNIMPLEMENTED_METHODS, ADD_STATIC_ACCESS -> {
				image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			}
			case UNUSED_CODE, REMOVE_REDUNDANT_TYPE_ARGS, REMOVE_UNNECESSARY_NLS, DELETE_ID -> {
				image= ISharedImages.get().getImage(ISharedImages.IMG_TOOL_DELETE);
			}
			case RENAME_CODE, RENAME_ID -> {
				image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LINKED_RENAME);
			}
			case ADD_NLS -> {
				image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_NLS_NEVER_TRANSLATE);
			}
		}
		return new FixCorrectionProposal(core, image);
	}

	@Override
	protected ICommandAccess constructorFromSuperClassProposalToT(ConstructorFromSuperclassProposalCore constructorFromSuperclassProposalCore, int uid) {
		return new ConstructorFromSuperclassProposal(constructorFromSuperclassProposalCore);
	}

	@Override
	protected ICommandAccess createNewObjectProposalToT(CreateNewObjectProposalCore core, int uid) {
		return new CreateNewObjectProposal(core);
	}

	@Override
	protected ICommandAccess createObjectReferenceProposalToT(CreateObjectReferenceProposalCore core, int uid) {
		return new CreateObjectReferenceProposal(core);
	}

	@Override
	protected ICommandAccess createVariableReferenceProposalToT(CreateVariableReferenceProposalCore core, int uid) {
		return new CreateVariableReferenceProposal(core);
	}

	@Override
	protected ICommandAccess astRewriteCorrectionProposalToT(ASTRewriteCorrectionProposalCore core, int uid) {
		Image image= ISharedImages.get().getImage(ISharedImages.IMG_TOOL_DELETE);
		switch (uid) {
			case REMOVE_REDUNDANT_SUPERINTERFACE, REMOVE_PROPOSAL, DELETE_ID -> {
				image= ISharedImages.get().getImage(ISharedImages.IMG_TOOL_DELETE);
			}
			case INVALID_OPERATOR -> {
				image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CAST);
			}
			case REMOVE_UNNECESSARY_INSTANCEOF, UNQUALIFY_ENUM_CONSTANT,
				MOVE_ELSE_CLAUSE, CHANGE_EXTENDS_TO_IMPLEMENTS, CHANGE_TO_INTERFACE,
				CHANGE_CODE, INSERT_BREAK_STATEMENT, INSERT_FALL_THROUGH, INSERT_CASES_OMITTED,
				REPLACE_FIELD_ACCESS -> {
				image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			}
			case REMOVE_DEFAULT -> {
				image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_REMOVE);
			}
		}
		return new ASTRewriteCorrectionProposal(core, image);
	}

	@Override
	protected ICommandAccess replaceCorrectionProposalToT(ReplaceCorrectionProposalCore core, int uid) {
		return new ReplaceCorrectionProposal(core);
	}


	@Override
	protected ICommandAccess cuCorrectionProposalToT(CUCorrectionProposalCore core, int uid) {
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD);
		switch (uid) {
			case ADD_PERMITTED_TYPE, ADD_SEALED_SUPERTYPE -> {
				image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD);
			}
			case INVALID_OPERATOR -> {
				image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CAST);
			}
		}
		try {
			return new CUCorrectionProposal(core, image);
		} catch (CoreException e) {
			return null;
		}
	}


	@Override
	protected ICommandAccess newVariableCorrectionProposalToT(NewVariableCorrectionProposalCore core, int uid) {
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
		switch (uid) {
			case CREATE_PARAMETER -> {
				image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
			}
		}
		return new NewVariableCorrectionProposal(core, image);
	}

	@Override
	protected ICommandAccess newLocalVariableCorrectionProposalToT(NewLocalVariableCorrectionProposalCore core, int uid) {
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
		return new NewLocalVariableCorrectionProposal(core, image);
	}

	@Override
	protected ICommandAccess missingAnnotationAttributesProposalToT(MissingAnnotationAttributesProposalCore core, int uid) {
		return new MissingAnnotationAttributesProposal(core);
	}


	@Override
	protected ICommandAccess newMethodCorrectionProposalToT(NewMethodCorrectionProposalCore core, int uid) {
		Image image= JavaElementImageProvider.getDecoratedImage(JavaPluginImages.DESC_MISC_PUBLIC, JavaElementImageDescriptor.CONSTRUCTOR, JavaElementImageProvider.SMALL_SIZE);
		switch (uid) {
			case CREATE_CONSTRUCTOR -> {
				image= JavaElementImageProvider.getDecoratedImage(JavaPluginImages.DESC_MISC_PUBLIC, JavaElementImageDescriptor.CONSTRUCTOR, JavaElementImageProvider.SMALL_SIZE);
			}
		}
		return new NewMethodCorrectionProposal(core, image);
	}


	@Override
	protected ICommandAccess modifierChangeCorrectionProposalToT(ModifierChangeCorrectionProposalCore core, int uid) {
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		switch (uid) {
			case CHANGE_MODIFIER -> {
				image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			}
		}
		return new ModifierChangeCorrectionProposal(core, image);
	}

	@Override
	protected ICommandAccess newProviderMethodDeclarationProposalToT(NewProviderMethodDeclarationCore core, int uid) {
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC);
		switch (uid) {
			case MISC_PUBLIC_ID -> {
				image= JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC);
			}
		}
		return new NewProviderMethodDeclaration(core, image);
	}

	@Override
	protected ICommandAccess generateForLoopAssistProposalToT(GenerateForLoopAssistProposalCore core) {
		return new GenerateForLoopAssistProposal(core);
	}

	@Override
	protected ICommandAccess linkedNamesAssistProposalToT(LinkedNamesAssistProposalCore core) {
		return new LinkedNamesAssistProposal(core.getLabel(), core.getContext(), core.getNode(), core.getValueSuggestion());
	}
}
