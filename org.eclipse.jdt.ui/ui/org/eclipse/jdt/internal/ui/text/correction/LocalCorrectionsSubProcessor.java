/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Renaud Waldura &lt;renaud+eclipse@waldura.com&gt; - Access to static proposal
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Collection;
import java.util.List;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
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
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.refactoring.nls.ExternalizeWizard;

/**
  */
public class LocalCorrectionsSubProcessor {

	
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
			
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		SurroundWithTryCatchRefactoring refactoring= SurroundWithTryCatchRefactoring.create(cu, selectedNode.getStartPosition(), selectedNode.getLength(), settings, null);
		if (refactoring == null)
			return;
		
		refactoring.setLeaveDirty(true);
		ASTParser parser= ASTParser.newParser(AST.JLS2);
		parser.setSource(cu);
		parser.setResolveBindings(true);
		parser.setFocalPosition(selectedNode.getStartPosition());
		CompilationUnit newRoot= (CompilationUnit) parser.createAST(null);
		if (refactoring.checkActivationBasics(newRoot).isOK()) {
			String label= CorrectionMessages.getString("LocalCorrectionsSubProcessor.surroundwith.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
			CUCorrectionProposal proposal= new CUCorrectionProposal(label, cu, (CompilationUnitChange) refactoring.createChange(null), 4, image);
			proposals.add(proposal);
		}
		
		BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(selectedNode);
		if (decl == null) {
			return;
		}
		ITypeBinding[] uncaughtExceptions= ExceptionAnalyzer.perform(decl, Selection.createFromStartLength(selectedNode.getStartPosition(), selectedNode.getLength()));
		if (uncaughtExceptions.length == 0) {
			return;
		}
		
		TryStatement surroundingTry= ASTResolving.findParentTryStatement(selectedNode);
		if (surroundingTry != null && ASTNodes.isParent(selectedNode, surroundingTry.getBody())) {
			ASTRewrite rewrite= ASTRewrite.create(surroundingTry.getAST());
			ImportRewrite imports= new ImportRewrite(cu);
			
			String label= CorrectionMessages.getString("LocalCorrectionsSubProcessor.addadditionalcatch.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
			LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, cu, rewrite, 5, image);
			proposal.setImportRewrite(imports);
			
			AST ast= astRoot.getAST();
			ListRewrite clausesRewrite= rewrite.getListRewrite(surroundingTry, TryStatement.CATCH_CLAUSES_PROPERTY);
			for (int i= 0; i < uncaughtExceptions.length; i++) {
				ITypeBinding excBinding= uncaughtExceptions[i];
				String varName= PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.CODEGEN_EXCEPTION_VAR_NAME);
				String imp= imports.addImport(excBinding);
				Name name= ASTNodeFactory.newName(ast, imp);
				SingleVariableDeclaration var= ast.newSingleVariableDeclaration();
				var.setName(ast.newSimpleName(varName));
				var.setType(ast.newSimpleType(name));
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
			AST ast= astRoot.getAST();
			ASTRewrite rewrite= ASTRewrite.create(ast);
			ImportRewrite imports= new ImportRewrite(cu);
			
			String label= CorrectionMessages.getString("LocalCorrectionsSubProcessor.addthrows.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
			LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, cu, rewrite, 6, image);
			proposal.setImportRewrite(imports);

			MethodDeclaration methodDecl= (MethodDeclaration) decl;
			List exceptions= methodDecl.thrownExceptions();
			ListRewrite listRewrite= rewrite.getListRewrite(methodDecl, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY);
			for (int i= 0; i < uncaughtExceptions.length; i++) {
				String imp= imports.addImport(uncaughtExceptions[i]);
				Name name= ASTNodeFactory.newName(ast, imp);
				listRewrite.insertLast(name, null);
				String typeKey= "type" + i; //$NON-NLS-1$
				proposal.addLinkedPosition(rewrite.track(name), false, typeKey);
				addExceptionTypeLinkProposals(proposal, uncaughtExceptions[i], typeKey);
			}
			for (int i= 0; i < exceptions.size(); i++) {
				Name elem= (Name) exceptions.get(i);
				if (canRemoveException(elem.resolveTypeBinding(), uncaughtExceptions)) {
					rewrite.remove(elem, null);
				}
			}
			proposals.add(proposal);
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
		if (! NLSRefactoring.isAvailable(cu)){
			return;
		}
		String name= CorrectionMessages.getString("LocalCorrectionsSubProcessor.externalizestrings.description"); //$NON-NLS-1$
		
		ChangeCorrectionProposal proposal= new ChangeCorrectionProposal(name, null, 5, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE)) {
			public void apply(IDocument document) {
				try {
					NLSRefactoring refactoring= NLSRefactoring.create(cu);
					if (refactoring == null)
						return;
					ExternalizeWizard wizard= new ExternalizeWizard(refactoring);
					String dialogTitle= CorrectionMessages.getString("LocalCorrectionsSubProcessor.externalizestrings.dialog.title"); //$NON-NLS-1$
					new RefactoringStarter().activate(refactoring, wizard, JavaPlugin.getActiveWorkbenchShell(), dialogTitle, true);
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
				}
			}
		};
		proposals.add(proposal);
		TextEdit edit= NLSUtil.createNLSEdit(cu, problem.getOffset());
		if (edit != null) {
			String label= CorrectionMessages.getString("LocalCorrectionsSubProcessor.addnon-nls.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_NLS_NEVER_TRANSLATE);
			CUCorrectionProposal nlsProposal= new CUCorrectionProposal(label, cu, 6, image);
			nlsProposal.getTextChange().getEdit().addChild(edit);
			proposals.add(nlsProposal);
		}
	}
	
	/**
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
					ASTRewrite rewrite= ASTRewrite.create(selectedNode.getAST());
					ImportRewrite imports= new ImportRewrite(cu);

					String typeName= imports.addImport(declaringTypeBinding);
					rewrite.replace(qualifier, ASTNodeFactory.newName(astRoot.getAST(), typeName), null);

					String label= CorrectionMessages.getFormattedString("LocalCorrectionsSubProcessor.indirectaccesstostatic.description", declaringTypeBinding.getName()); //$NON-NLS-1$
					Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
					ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 6, image);
					proposal.setImportRewrite(imports);
					proposals.add(proposal);					
				}
			}
			return;
		}

		
		ITypeBinding declaringTypeBinding= null;
		if (accessBinding != null) {
			declaringTypeBinding= getDeclaringTypeBinding(accessBinding);
			if (declaringTypeBinding != null) {
				ASTRewrite rewrite= ASTRewrite.create(selectedNode.getAST());
				ImportRewrite imports= new ImportRewrite(cu);

				String label= CorrectionMessages.getFormattedString("LocalCorrectionsSubProcessor.changeaccesstostaticdefining.description", declaringTypeBinding.getName()); //$NON-NLS-1$
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 6, image);
				proposal.setImportRewrite(imports);
				
				String typeName= imports.addImport(declaringTypeBinding);
				rewrite.replace(qualifier, ASTNodeFactory.newName(astRoot.getAST(), typeName), null);

				proposals.add(proposal);
			}
		}        
		if (qualifier != null) {
			ITypeBinding instanceTypeBinding= Bindings.normalizeTypeBinding(qualifier.resolveTypeBinding());
			if (instanceTypeBinding != null && instanceTypeBinding != declaringTypeBinding) {
				ASTRewrite rewrite= ASTRewrite.create(selectedNode.getAST());
				ImportRewrite imports= new ImportRewrite(cu);
				
				String label= CorrectionMessages.getFormattedString("LocalCorrectionsSubProcessor.changeaccesstostatic.description", instanceTypeBinding.getName()); //$NON-NLS-1$
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 5, image);
				proposal.setImportRewrite(imports);
				
				String typeName= imports.addImport(instanceTypeBinding);
				rewrite.replace(qualifier, ASTNodeFactory.newName(astRoot.getAST(), typeName), null);

				proposals.add(proposal);
			}
		}
		ModifierCorrectionSubProcessor.addNonAccessibleMemberProposal(context, problem, proposals, ModifierCorrectionSubProcessor.TO_NON_STATIC, 4);
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
		if (selectedNode.getNodeType() == ASTNode.SIMPLE_NAME && selectedNode.getParent().getNodeType() == ASTNode.TYPE_DECLARATION) {
			typeNode= selectedNode.getParent();
		} else if (selectedNode.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION) {
			ClassInstanceCreation creation= (ClassInstanceCreation) selectedNode;
			typeNode= creation.getAnonymousClassDeclaration();				
		}
		if (typeNode != null) {
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

			String label= CorrectionMessages.getString("LocalCorrectionsSubProcessor.uninitializedvariable.description"); //$NON-NLS-1$
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
				proposals.add(new RemoveDeclarationCorrectionProposal(context.getCompilationUnit(), name, 5));
			}
		}
	}

	public static void addSuperfluousSemicolonProposal(IInvocationContext context, IProblemLocation problem,  Collection proposals) {
		String label= CorrectionMessages.getString("LocalCorrectionsSubProcessor.removesemicolon.description"); //$NON-NLS-1$
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
			
			String label= CorrectionMessages.getString("LocalCorrectionsSubProcessor.unnecessarycast.description"); //$NON-NLS-1$
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
			
			String label= CorrectionMessages.getString("LocalCorrectionsSubProcessor.unnecessaryinstanceof.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 5, image);
			proposals.add(proposal);
		}
	
	}

	public static void addUnnecessaryThrownExceptionProposal(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode == null) {
			return;
		}
		if (selectedNode.getParent() instanceof MethodDeclaration) {
			MethodDeclaration decl= (MethodDeclaration) selectedNode.getParent();
			List thrownExceptions= decl.thrownExceptions();
			if (!thrownExceptions.contains(selectedNode)) {
				return;
			}
			
			ASTRewrite rewrite= ASTRewrite.create(decl.getAST());
			rewrite.remove(selectedNode, null);
			
			Javadoc javadoc= decl.getJavadoc();
			if (javadoc != null) {
				IBinding binding= ((Name) selectedNode).resolveBinding();
				if (binding != null) {
					TagElement tagElement= findThrowsTag(javadoc, binding);
					if (tagElement != null) {
						rewrite.remove(tagElement, null);
					}
				}
			}
			
			String label= CorrectionMessages.getString("LocalCorrectionsSubProcessor.unnecessarythrow.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 5, image);
			proposals.add(proposal);
		}
	}
	
	private static TagElement findThrowsTag(Javadoc javadoc, IBinding binding) {
		List tags= javadoc.tags();
		for (int i= tags.size() - 1; i >= 0; i--) {
			TagElement curr= (TagElement) tags.get(i);
			String currName= curr.getTagName();
			if ("@throws".equals(currName) || "@exception".equals(currName)) {  //$NON-NLS-1$//$NON-NLS-2$
				List fragments= curr.fragments();
				if (!fragments.isEmpty() && fragments.get(0) instanceof Name) {
					Name name= (Name) fragments.get(0);
					if (name.resolveBinding() == binding) {
						return curr;
					}
				}
			}
		}
		return null;
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
			ITypeBinding currType= Bindings.getBindingOfParentType(name);
			if (Bindings.isSuperType(declaringClass, currType)) {
				qualifier= "this"; //$NON-NLS-1$
			} else {
				String outer= imports.addImport(declaringClass);
				qualifier= outer + ".this"; //$NON-NLS-1$
			}
		}
		
		String replacement= qualifier + '.' + name.getIdentifier();
		rewrite.replace(name, rewrite.createStringPlaceholder(replacement, ASTNode.SIMPLE_NAME), null);

		String label= CorrectionMessages.getFormattedString("LocalCorrectionsSubProcessor.unqualifiedfieldaccess.description", qualifier); //$NON-NLS-1$
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 5, image); //$NON-NLS-1$
		proposal.setImportRewrite(imports);
		proposals.add(proposal);
	}

	public static void addHidingVariablesProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		CompilationUnit root= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(root);
		if (!(selectedNode instanceof SimpleName)) {
			return;
		}
		SimpleName nameNode= (SimpleName) selectedNode;
		
		String name;
		switch (problem.getProblemId()) {
			case IProblem.LocalVariableHidingLocalVariable:
			case IProblem.LocalVariableHidingField:
				name= CorrectionMessages.getFormattedString("LocalCorrectionsSubProcessor.hiding.local.label", nameNode.getIdentifier()); //$NON-NLS-1$
				break;
			case IProblem.FieldHidingLocalVariable:
			case IProblem.FieldHidingField:
				name= CorrectionMessages.getFormattedString("LocalCorrectionsSubProcessor.hiding.field.label", nameNode.getIdentifier()); //$NON-NLS-1$
				break;
			case IProblem.ArgumentHidingLocalVariable:
			case IProblem.ArgumentHidingField:
				name= CorrectionMessages.getFormattedString("LocalCorrectionsSubProcessor.hiding.argument.label", nameNode.getIdentifier()); //$NON-NLS-1$
				break;
			default:
				return;
		}
		LinkedNamesAssistProposal proposal= new LinkedNamesAssistProposal(name, context.getCompilationUnit(), nameNode);
		proposals.add(proposal);
	}

	
}
