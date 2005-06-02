/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Renaud Waldura &lt;renaud+eclipse@waldura.com&gt; - Access to static proposal
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.ISharedImages;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSUtil;
import org.eclipse.jdt.internal.corext.refactoring.surround.ExceptionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryCatchRefactoring;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.refactoring.nls.ExternalizeWizard;
import org.eclipse.jdt.internal.ui.text.correction.ChangeMethodSignatureProposal.ChangeDescription;
import org.eclipse.jdt.internal.ui.text.correction.ChangeMethodSignatureProposal.InsertDescription;
import org.eclipse.jdt.internal.ui.text.correction.ChangeMethodSignatureProposal.RemoveDescription;

/**
  */
public class LocalCorrectionsSubProcessor {

	private static final String ADD_EXCEPTION_TO_THROWS_ID= "org.eclipse.jdt.ui.correction.addThrowsDecl"; //$NON-NLS-1$
	private static final String ADD_NON_NLS_ID= "org.eclipse.jdt.ui.correction.addNonNLS"; //$NON-NLS-1$
	private static final String ADD_FIELD_QUALIFICATION_ID= "org.eclipse.jdt.ui.correction.qualifyField"; //$NON-NLS-1$
	private static final String ADD_STATIC_ACCESS_ID= "org.eclipse.jdt.ui.correction.changeToStatic"; //$NON-NLS-1$
	
	public static void addUncaughtExceptionProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();

		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(astRoot);
		if (selectedNode == null) {
			return;
		}
		while (selectedNode != null && !(selectedNode instanceof Statement)) {
			selectedNode= selectedNode.getParent();
		}
		if (selectedNode == null) {
			return;
		}

		int offset= selectedNode.getStartPosition();
		int length= selectedNode.getLength();
		int selectionEnd= context.getSelectionOffset() + context.getSelectionLength();
		if (selectionEnd > offset + length) {
			// extend the selection if more than one statement is selected (bug 72149)
			length= selectionEnd - offset;
		}

		SurroundWithTryCatchRefactoring refactoring= SurroundWithTryCatchRefactoring.create(cu, offset, length, null);
		if (refactoring == null)
			return;

		refactoring.setLeaveDirty(true);
		if (refactoring.checkActivationBasics(astRoot).isOK()) {
			String label= CorrectionMessages.LocalCorrectionsSubProcessor_surroundwith_description;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
			CUCorrectionProposal proposal= new CUCorrectionProposal(label, cu, (CompilationUnitChange) refactoring.createChange(null), 4, image);
			proposals.add(proposal);
		}

		BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(selectedNode);
		if (decl == null) {
			return;
		}

		ITypeBinding[] uncaughtExceptions= ExceptionAnalyzer.perform(decl, Selection.createFromStartLength(offset, length));
		if (uncaughtExceptions.length == 0) {
			return;
		}

		TryStatement surroundingTry= ASTResolving.findParentTryStatement(selectedNode);
		if (surroundingTry != null && ASTNodes.isParent(selectedNode, surroundingTry.getBody())) {
			ASTRewrite rewrite= ASTRewrite.create(surroundingTry.getAST());
			ImportRewrite imports= new ImportRewrite(cu);

			String label= CorrectionMessages.LocalCorrectionsSubProcessor_addadditionalcatch_description;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
			LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, cu, rewrite, 5, image);
			proposal.setImportRewrite(imports);

			AST ast= astRoot.getAST();
			ListRewrite clausesRewrite= rewrite.getListRewrite(surroundingTry, TryStatement.CATCH_CLAUSES_PROPERTY);
			for (int i= 0; i < uncaughtExceptions.length; i++) {
				ITypeBinding excBinding= uncaughtExceptions[i];
				String varName= StubUtility.getExceptionVariableName(cu.getJavaProject());
				SingleVariableDeclaration var= ast.newSingleVariableDeclaration();
				var.setName(ast.newSimpleName(varName));
				var.setType(imports.addImport(excBinding, ast));
				CatchClause newClause= ast.newCatchClause();
				newClause.setException(var);
				String catchBody = StubUtility.getCatchBodyContent(cu, excBinding.getName(), varName, String.valueOf('\n'));
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

		if (decl instanceof MethodDeclaration) {
			MethodDeclaration methodDecl= (MethodDeclaration) decl;
			IMethodBinding binding= methodDecl.resolveBinding();
			if (binding != null) {
				ITypeBinding[] methodExceptions= binding.getExceptionTypes();
				ArrayList unhandledExceptions= new ArrayList(uncaughtExceptions.length);
				for (int i= 0; i < uncaughtExceptions.length; i++) {
					ITypeBinding curr= uncaughtExceptions[i];
					if (!canRemoveException(curr, methodExceptions)) {
						unhandledExceptions.add(curr);
					}
				}
				uncaughtExceptions= (ITypeBinding[]) unhandledExceptions.toArray(new ITypeBinding[unhandledExceptions.size()]);

				List exceptions= methodDecl.thrownExceptions();
				int nExistingExceptions= exceptions.size();
				ChangeDescription[] desc= new ChangeDescription[nExistingExceptions + uncaughtExceptions.length];
				for (int i= 0; i < exceptions.size(); i++) {
					Name elem= (Name) exceptions.get(i);
					if (canRemoveException(elem.resolveTypeBinding(), uncaughtExceptions)) {
						desc[i]= new RemoveDescription();
					}
				}
				for (int i = 0; i < uncaughtExceptions.length; i++) {
					desc[i + nExistingExceptions]= new InsertDescription(uncaughtExceptions[i], ""); //$NON-NLS-1$
				}

				String label= CorrectionMessages.LocalCorrectionsSubProcessor_addthrows_description;
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);

				ChangeMethodSignatureProposal proposal= new ChangeMethodSignatureProposal(label, cu, astRoot, binding, null, desc, 6, image);
				for (int i= 0; i < uncaughtExceptions.length; i++) {
					addExceptionTypeLinkProposals(proposal, uncaughtExceptions[i], proposal.getExceptionTypeGroupId(i + nExistingExceptions));
				}
				proposal.setCommandId(ADD_EXCEPTION_TO_THROWS_ID);
				proposals.add(proposal);
			}
		}
	}

	private static void addExceptionTypeLinkProposals(LinkedCorrectionProposal proposal, ITypeBinding exc, String key) {
		// all super classes except Object
		while (exc != null && !"java.lang.Object".equals(exc.getQualifiedName())) { //$NON-NLS-1$
			proposal.addLinkedPositionProposal(key, exc);
			exc= exc.getSuperclass();
		}
	}


	private static boolean canRemoveException(ITypeBinding curr, ITypeBinding[] addedExceptions) {
		while (curr != null) {
			for (int i= 0; i < addedExceptions.length; i++) {
				if (curr == addedExceptions[i]) {
					return true;
				}
			}
			curr= curr.getSuperclass();
		}
		return false;
	}

	public static void addUnreachableCatchProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode == null) {
			return;
		}

		QuickAssistProcessor.getCatchClauseToThrowsProposals(context, selectedNode, proposals);
	}

	public static void addNLSProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		final ICompilationUnit cu= context.getCompilationUnit();
		if (cu == null || !cu.exists()){
			return;
		}
		String name= CorrectionMessages.LocalCorrectionsSubProcessor_externalizestrings_description;

		ChangeCorrectionProposal proposal= new ChangeCorrectionProposal(name, null, 5, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE)) {
			public void apply(IDocument document) {
				try {
					NLSRefactoring refactoring= NLSRefactoring.create(cu);
					if (refactoring == null)
						return;
					ExternalizeWizard wizard= new ExternalizeWizard(refactoring);
					String dialogTitle= CorrectionMessages.LocalCorrectionsSubProcessor_externalizestrings_dialog_title;
					new RefactoringStarter().activate(refactoring, wizard, JavaPlugin.getActiveWorkbenchShell(), dialogTitle, true);
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
				}
			}
			public String getAdditionalProposalInfo() {
				return CorrectionMessages.LocalCorrectionsSubProcessor_LocalCorrectionsSubProcessor_externalizestrings_additional_info;
			}
			
		};
		proposals.add(proposal);
		TextEdit edit= NLSUtil.createNLSEdit(cu, problem.getOffset());
		if (edit != null) {
			String label= CorrectionMessages.LocalCorrectionsSubProcessor_addnon_nls_description;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_NLS_NEVER_TRANSLATE);
			CUCorrectionProposal nlsProposal= new CUCorrectionProposal(label, cu, 6, image);
			nlsProposal.setCommandId(ADD_NON_NLS_ID);
			nlsProposal.getTextChange().getEdit().addChild(edit);
			proposals.add(nlsProposal);
		}
	}

	/*
	 * Fix instance accesses and indirect (static) accesses to static fields/methods
	 */
	public static void addCorrectAccessToStaticProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();

		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(astRoot);
		if (selectedNode == null) {
			return;
		}
		// fix for 32022
		String[] args= problem.getProblemArguments();
		if (selectedNode instanceof QualifiedName && args.length == 2) {
			String field= args[1];
			QualifiedName qualified= (QualifiedName) selectedNode;
			while (!field.equals(qualified.getName().getIdentifier()) && qualified.getQualifier() instanceof QualifiedName) {
				qualified= (QualifiedName) qualified.getQualifier();
			}
			selectedNode= qualified;
			problem= new ProblemLocation(qualified.getStartPosition(), qualified.getLength(), problem.getProblemId(), args, problem.isError());
		}


		Expression qualifier= null;
		IBinding accessBinding= null;

        if (selectedNode instanceof QualifiedName) {
        	QualifiedName name= (QualifiedName) selectedNode;
        	qualifier= name.getQualifier();
        	accessBinding= name.resolveBinding();
        } else if (selectedNode instanceof SimpleName) {
        	ASTNode parent= selectedNode.getParent();
        	if (parent instanceof FieldAccess) {
        		FieldAccess fieldAccess= (FieldAccess) parent;
        		qualifier= fieldAccess.getExpression();
        		accessBinding= fieldAccess.getName().resolveBinding();
        	}
        } else if (selectedNode instanceof MethodInvocation) {
        	MethodInvocation methodInvocation= (MethodInvocation) selectedNode;
        	qualifier= methodInvocation.getExpression();
        	accessBinding= methodInvocation.getName().resolveBinding();
        } else if (selectedNode instanceof FieldAccess) {
			FieldAccess fieldAccess= (FieldAccess) selectedNode;
			qualifier= fieldAccess.getExpression();
			accessBinding= fieldAccess.getName().resolveBinding();
		}

		if (problem.getProblemId() == IProblem.IndirectAccessToStaticField || problem.getProblemId() == IProblem.IndirectAccessToStaticMethod) {
			// indirectAccessToStaticProposal
			if (accessBinding != null) {
				ITypeBinding declaringTypeBinding= getDeclaringTypeBinding(accessBinding);
				if (declaringTypeBinding != null) {
					declaringTypeBinding= declaringTypeBinding.getTypeDeclaration(); // use generic to avoid any type arguments
					ASTRewrite rewrite= ASTRewrite.create(selectedNode.getAST());
					ImportRewrite imports= new ImportRewrite(cu);

					String typeName= imports.addImport(declaringTypeBinding);
					rewrite.replace(qualifier, ASTNodeFactory.newName(astRoot.getAST(), typeName), null);

					String label= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_indirectaccesstostatic_description, declaringTypeBinding.getName());
					Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
					ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 6, image);
					proposal.setImportRewrite(imports);
					proposal.setCommandId(ADD_STATIC_ACCESS_ID);
					proposals.add(proposal);
				}
			}
			return;
		}


		ITypeBinding declaringTypeBinding= null;
		if (accessBinding != null) {
			declaringTypeBinding= getDeclaringTypeBinding(accessBinding);
			if (declaringTypeBinding != null) {
				declaringTypeBinding= declaringTypeBinding.getTypeDeclaration(); // use generic to avoid any type arguments
				ASTRewrite rewrite= ASTRewrite.create(selectedNode.getAST());
				ImportRewrite imports= new ImportRewrite(cu);

				String label= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_changeaccesstostaticdefining_description, declaringTypeBinding.getName());
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 6, image);
				proposal.setImportRewrite(imports);
				proposal.setCommandId(ADD_STATIC_ACCESS_ID);

				String typeName= imports.addImport(declaringTypeBinding);
				rewrite.replace(qualifier, ASTNodeFactory.newName(astRoot.getAST(), typeName), null);

				proposals.add(proposal);
			}
		}
		if (qualifier != null) {
			ITypeBinding instanceTypeBinding= Bindings.normalizeTypeBinding(qualifier.resolveTypeBinding());
			if (instanceTypeBinding != null) {
				instanceTypeBinding= instanceTypeBinding.getTypeDeclaration();  // use generic to avoid any type arguments
				if (instanceTypeBinding.getTypeDeclaration() != declaringTypeBinding) {
					ASTRewrite rewrite= ASTRewrite.create(selectedNode.getAST());
					ImportRewrite imports= new ImportRewrite(cu);

					String label= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_changeaccesstostatic_description, instanceTypeBinding.getName());
					Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
					ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 5, image);
					proposal.setImportRewrite(imports);

					String typeName= imports.addImport(instanceTypeBinding);
					rewrite.replace(qualifier, ASTNodeFactory.newName(astRoot.getAST(), typeName), null);

					proposals.add(proposal);
				}
			}
		}
		ModifierCorrectionSubProcessor.addNonAccessibleReferenceProposal(context, problem, proposals, ModifierCorrectionSubProcessor.TO_NON_STATIC, 4);
	}

	private static ITypeBinding getDeclaringTypeBinding(IBinding accessBinding) {
		if (accessBinding instanceof IMethodBinding) {
			return ((IMethodBinding) accessBinding).getDeclaringClass();
		} else if (accessBinding instanceof IVariableBinding) {
			return ((IVariableBinding) accessBinding).getDeclaringClass();
		}
		return null;
	}



	public static void addUnimplementedMethodsProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		ICompilationUnit cu= context.getCompilationUnit();
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode == null) {
			return;
		}
		ASTNode typeNode= null;
		ITypeBinding binding= null;
		if (selectedNode.getNodeType() == ASTNode.SIMPLE_NAME && selectedNode.getParent().getNodeType() == ASTNode.TYPE_DECLARATION) {
			TypeDeclaration typeDecl= (TypeDeclaration) selectedNode.getParent();
			binding= typeDecl.resolveBinding();
			typeNode= typeDecl;
		} else if (selectedNode.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION) {
			ClassInstanceCreation creation= (ClassInstanceCreation) selectedNode;
			AnonymousClassDeclaration anonymDecl= creation.getAnonymousClassDeclaration();
			binding= anonymDecl.resolveBinding();
			typeNode= anonymDecl;
		}
		if (typeNode != null && binding != null) {
			UnimplementedMethodsCompletionProposal proposal= new UnimplementedMethodsCompletionProposal(cu, typeNode, 10);
			proposals.add(proposal);
		}
		if (typeNode instanceof TypeDeclaration) {
			TypeDeclaration typeDeclaration= (TypeDeclaration) typeNode;
			ASTRewriteCorrectionProposal proposal= ModifierCorrectionSubProcessor.getMakeTypeAbstractProposal(cu, typeDeclaration, 5);
			proposals.add(proposal);
		}
	}

	public static void addUninitializedLocalVariableProposal(IInvocationContext context, IProblemLocation problem, Collection proposals) {
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

			LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, cu, rewrite, 6, image);
			proposal.addLinkedPosition(rewrite.track(expression), false, "initializer"); //$NON-NLS-1$
			proposals.add(proposal);
		}
	}

	public static void addConstructorFromSuperclassProposal(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (!(selectedNode instanceof Name && selectedNode.getParent() instanceof TypeDeclaration)) {
			return;
		}
		TypeDeclaration typeDeclaration= (TypeDeclaration) selectedNode.getParent();
		ITypeBinding binding= typeDeclaration.resolveBinding();
		if (binding == null || binding.getSuperclass() == null) {
			return;
		}
		ICompilationUnit cu= context.getCompilationUnit();
		IMethodBinding[] methods= binding.getSuperclass().getDeclaredMethods();
		for (int i= 0; i < methods.length; i++) {
			IMethodBinding curr= methods[i];
			if (curr.isConstructor() && !Modifier.isPrivate(curr.getModifiers())) {
				proposals.add(new ConstructorFromSuperclassProposal(cu, typeDeclaration, curr, 5));
			}
		}
	}

	public static void addUnusedMemberProposal(IInvocationContext context, IProblemLocation problem,  Collection proposals) {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());

		SimpleName name= null;
		if (selectedNode instanceof MethodDeclaration) {
			name= ((MethodDeclaration) selectedNode).getName();
		} else if (selectedNode instanceof SimpleName) {
			name= (SimpleName) selectedNode;
		}
		if (name != null) {
			IBinding binding= name.resolveBinding();
			if (binding != null) {
				proposals.add(new RemoveDeclarationCorrectionProposal(context.getCompilationUnit(), name, 10));
			}
		}
	}

	public static void addSuperfluousSemicolonProposal(IInvocationContext context, IProblemLocation problem,  Collection proposals) {
		String label= CorrectionMessages.LocalCorrectionsSubProcessor_removesemicolon_description;
		ReplaceCorrectionProposal proposal= new ReplaceCorrectionProposal(label, context.getCompilationUnit(), problem.getOffset(), problem.getLength(), "", 6); //$NON-NLS-1$
		proposals.add(proposal);
	}

	public static void addUnnecessaryCastProposal(IInvocationContext context, IProblemLocation problem,  Collection proposals) {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());

		ASTNode curr= selectedNode;
		while (curr instanceof ParenthesizedExpression) {
			curr= ((ParenthesizedExpression) curr).getExpression();
		}

		if (curr instanceof CastExpression) {
			ASTRewrite rewrite= ASTRewrite.create(selectedNode.getAST());

			CastExpression cast= (CastExpression) curr;
			Expression expression= cast.getExpression();
			ASTNode placeholder= rewrite.createCopyTarget(expression);

			if (ASTNodes.needsParentheses(expression)) {
				rewrite.replace(curr, placeholder, null);
			} else {
				rewrite.replace(selectedNode, placeholder, null);
			}

			String label= CorrectionMessages.LocalCorrectionsSubProcessor_unnecessarycast_description;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 5, image);
			proposals.add(proposal);
		}
	}

	public static void addUnnecessaryInstanceofProposal(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());

		ASTNode curr= selectedNode;
		while (curr instanceof ParenthesizedExpression) {
			curr= ((ParenthesizedExpression) curr).getExpression();
		}

		if (curr instanceof InstanceofExpression) {
			AST ast= curr.getAST();

			ASTRewrite rewrite= ASTRewrite.create(ast);

			InstanceofExpression inst= (InstanceofExpression) curr;

			InfixExpression expression= ast.newInfixExpression();
			expression.setLeftOperand((Expression) rewrite.createCopyTarget(inst.getLeftOperand()));
			expression.setOperator(InfixExpression.Operator.NOT_EQUALS);
			expression.setRightOperand(ast.newNullLiteral());


			if (false/*ASTNodes.needsParentheses(expression)*/) {
				ParenthesizedExpression parents= ast.newParenthesizedExpression();
				parents.setExpression(expression);
				rewrite.replace(inst, parents, null);
			} else {
				rewrite.replace(inst, expression, null);
			}

			String label= CorrectionMessages.LocalCorrectionsSubProcessor_unnecessaryinstanceof_description;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 5, image);
			proposals.add(proposal);
		}

	}

	public static void addUnnecessaryThrownExceptionProposal(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode == null || !(selectedNode.getParent() instanceof MethodDeclaration)) {
			return;
		}
		MethodDeclaration decl= (MethodDeclaration) selectedNode.getParent();
		IMethodBinding binding= decl.resolveBinding();
		if (binding != null) {
			List thrownExceptions= decl.thrownExceptions();
			int index= thrownExceptions.indexOf(selectedNode);
			if (index == -1) {
				return;
			}
			ChangeDescription[] desc= new ChangeDescription[thrownExceptions.size()];
			desc[index]= new RemoveDescription();

			ICompilationUnit cu= context.getCompilationUnit();
			String label= CorrectionMessages.LocalCorrectionsSubProcessor_unnecessarythrow_description;
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);

			proposals.add(new ChangeMethodSignatureProposal(label, cu, selectedNode, binding, null, desc, 5, image));
		}
	}

	public static void addUnqualifiedFieldAccessProposal(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		while (selectedNode instanceof QualifiedName) {
			selectedNode= ((QualifiedName) selectedNode).getQualifier();
		}
		if (!(selectedNode instanceof SimpleName)) {
			return;
		}
		SimpleName name = (SimpleName) selectedNode;
		IBinding binding= name.resolveBinding();
		if (binding.getKind() != IBinding.VARIABLE) {
			return;
		}

		ASTRewrite rewrite= ASTRewrite.create(name.getAST());
		ImportRewrite imports= new ImportRewrite(context.getCompilationUnit());

		ITypeBinding declaringClass= ((IVariableBinding) binding).getDeclaringClass();
		String qualifier;
		if (Modifier.isStatic(binding.getModifiers())) {
			qualifier= imports.addImport(declaringClass);
		} else {
			ITypeBinding parentType= Bindings.getBindingOfParentType(name);
			ITypeBinding currType= parentType;
			while (currType != null && !Bindings.isSuperType(declaringClass, currType)) {
				currType= currType.getDeclaringClass();
			}
			if (currType != parentType) {
				String outer= imports.addImport(currType);
				qualifier= outer + ".this"; //$NON-NLS-1$
			} else {
				qualifier= "this"; //$NON-NLS-1$
			}
		}

		String replacement= qualifier + '.' + name.getIdentifier();
		rewrite.replace(name, rewrite.createStringPlaceholder(replacement, ASTNode.SIMPLE_NAME), null);

		String label= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_unqualifiedfieldaccess_description, qualifier);
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 5, image); //$NON-NLS-1$
		proposal.setImportRewrite(imports);
		proposal.setCommandId(ADD_FIELD_QUALIFICATION_ID);
		proposals.add(proposal);
	}

	public static void addInvalidVariableNameProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		// hiding, redefined or future keyword

		CompilationUnit root= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(root);
		if (!(selectedNode instanceof SimpleName)) {
			return;
		}
		SimpleName nameNode= (SimpleName) selectedNode;
		String valueSuggestion= null;

		String name;
		switch (problem.getProblemId()) {
			case IProblem.LocalVariableHidingLocalVariable:
			case IProblem.LocalVariableHidingField:
				name= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_hiding_local_label, nameNode.getIdentifier());
				break;
			case IProblem.FieldHidingLocalVariable:
			case IProblem.FieldHidingField:
				name= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_hiding_field_label, nameNode.getIdentifier());
				break;
			case IProblem.ArgumentHidingLocalVariable:
			case IProblem.ArgumentHidingField:
				name= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_hiding_argument_label, nameNode.getIdentifier());
				break;
			default:
				name= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_rename_var_label, nameNode.getIdentifier());
		}

		if (problem.getProblemId() == IProblem.UseEnumAsAnIdentifier) {
			valueSuggestion= "enumeration"; //$NON-NLS-1$
		} else {
			valueSuggestion= nameNode.getIdentifier() + '1';
		}

		LinkedNamesAssistProposal proposal= new LinkedNamesAssistProposal(name, context.getCompilationUnit(), nameNode, valueSuggestion);
		proposals.add(proposal);
	}

	public static void getInvalidOperatorProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		CompilationUnit root= context.getASTRoot();
		AST ast= root.getAST();

		ASTNode selectedNode= problem.getCoveringNode(root);

		while (selectedNode instanceof ParenthesizedExpression) {
			selectedNode= ((ParenthesizedExpression) selectedNode).getExpression();
		}

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
						label= CorrectionMessages.LocalCorrectionsSubProcessor_setparenteses_description;
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

					Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
					ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 5, image); //$NON-NLS-1$
					proposals.add(proposal);
				}
			}
		} else if (selectedNode instanceof InfixExpression && isBitOperation((((InfixExpression) selectedNode).getOperator()))) {
			// a & b == c -> (a & b) == c
			CompareInBitWiseOpFinder visitor= new CompareInBitWiseOpFinder();
			selectedNode.accept(visitor);
			if (visitor.compareExpression != null) { // compare operation inside bit operations: set parents
				ASTNode expr= selectedNode;
				ASTNode parent= expr.getParent(); // include all parents
				while (parent instanceof InfixExpression && isBitOperation(((InfixExpression) parent).getOperator())) {
					expr= parent;
					parent= expr.getParent();
				}
				String label= CorrectionMessages.LocalCorrectionsSubProcessor_LocalCorrectionsSubProcessor_setparenteses_bitop_description;
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CAST);
				CUCorrectionProposal proposal= new CUCorrectionProposal(label, context.getCompilationUnit(), 5, image);
				proposals.add(proposal);
				TextEdit edit= proposal.getTextChange().getEdit();
				ASTNode left= visitor.compareExpression.getLeftOperand();

				if (expr.getStartPosition() < left.getStartPosition()) {
					edit.addChild(new InsertEdit(expr.getStartPosition(), String.valueOf('(')));
					edit.addChild(new InsertEdit(ASTNodes.getExclusiveEnd(left), String.valueOf(')')));
				}
				ASTNode rigth= visitor.compareExpression.getRightOperand();
				int selEnd= ASTNodes.getExclusiveEnd(expr);
				if (selEnd > ASTNodes.getExclusiveEnd(rigth)) {
					edit.addChild(new InsertEdit(rigth.getStartPosition(), String.valueOf('(')));
					edit.addChild(new InsertEdit(selEnd, String.valueOf(')')));
				}
			}
		}
	}

	private static boolean isBitOperation(InfixExpression.Operator op) {
		return op == InfixExpression.Operator.AND || op == InfixExpression.Operator.OR || op == InfixExpression.Operator.XOR;
	}

	private static class CompareInBitWiseOpFinder extends ASTVisitor{
		public InfixExpression compareExpression= null;

		public boolean visit(InfixExpression e) {
			InfixExpression.Operator op= e.getOperator();
			if (isBitOperation(op)) {
				return true;
			} else if (op == InfixExpression.Operator.EQUALS || op == InfixExpression.Operator.NOT_EQUALS) {
				compareExpression= e;
				return false;
			}
			return false;
		}
	}

	public static void getUnnecessaryElseProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		CompilationUnit root= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(root);
		if (selectedNode == null) {
			return;
		}
		if (!(selectedNode.getParent() instanceof IfStatement)) {
			return;
		}
		IfStatement ifStatement= (IfStatement) selectedNode.getParent();
		ASTNode ifParent= ifStatement.getParent();
		if (!(ifParent instanceof Block)) {
			return;
		}

		ASTRewrite rewrite= ASTRewrite.create(root.getAST());
		ASTNode placeholder=QuickAssistProcessor.getCopyOfInner(rewrite, ifStatement.getElseStatement(), false);
		if (placeholder == null) {
			return;
		}
		rewrite.remove(ifStatement.getElseStatement(), null);

		ListRewrite listRewrite= rewrite.getListRewrite(ifParent, Block.STATEMENTS_PROPERTY);
		listRewrite.insertAfter(placeholder, ifStatement, null);

		String label= CorrectionMessages.LocalCorrectionsSubProcessor_removeelse_description;
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 5, image); //$NON-NLS-1$
		proposals.add(proposal);
	}


	public static void getInterfaceExtendsClassProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
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
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 6, image); //$NON-NLS-1$
			proposals.add(proposal);
		}
		{
			ASTRewrite rewrite= ASTRewrite.create(root.getAST());

			rewrite.set(typeDecl, TypeDeclaration.INTERFACE_PROPERTY, Boolean.TRUE, null);

			String typeName= typeDecl.getName().getIdentifier();
			String label= Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_classtointerface_description, typeName);
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 3, image); //$NON-NLS-1$
			proposals.add(proposal);
		}
	}

	public static void getUnreachableCodeProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		CompilationUnit root= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(root);
		if (selectedNode == null) {
			return;
		}
		if (selectedNode instanceof ExpressionStatement) {
			selectedNode= selectedNode.getParent();
		}
		if (selectedNode instanceof Statement) {
			ASTRewrite rewrite= ASTRewrite.create(selectedNode.getAST());
			rewrite.remove(selectedNode, null);
			String label= CorrectionMessages.LocalCorrectionsSubProcessor_removeunreachablecode_description;
			Image image= JavaPlugin.getDefault().getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 3, image); //$NON-NLS-1$
			proposals.add(proposal);
		}
	}


}
