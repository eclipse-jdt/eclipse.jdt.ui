/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Renaud Waldura &lt;renaud+eclipse@waldura.com&gt; - Access to static proposal
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.PrimitiveType.Code;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSUtil;
import org.eclipse.jdt.internal.corext.refactoring.surround.ExceptionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryCatchRefactoring;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.refactoring.nls.ExternalizeWizard;

/**
  */
public class LocalCorrectionsSubProcessor {

	public static void addCastProposals(ICorrectionContext context, List proposals) throws CoreException {
		String[] args= context.getProblemArguments();
		if (args.length != 2) {
			return;
		}
			
		ICompilationUnit cu= context.getCompilationUnit();
		String castType= args[1];

		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= context.getCoveredNode();
		if (!(selectedNode instanceof Expression)) {
			return;
		}
		Expression nodeToCast= (Expression) selectedNode;
		
		int parentNodeType= selectedNode.getParent().getNodeType();
		if (parentNodeType == ASTNode.ASSIGNMENT) {
			Assignment assign= (Assignment) selectedNode.getParent();
			if (selectedNode.equals(assign.getLeftHandSide())) {
				nodeToCast= assign.getRightHandSide();
			}
		} else if (parentNodeType == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
			VariableDeclarationFragment frag= (VariableDeclarationFragment) selectedNode.getParent();
			if (selectedNode.equals(frag.getName())) {
				nodeToCast= frag.getInitializer();
			}
		}

		ASTRewriteCorrectionProposal castProposal= getCastProposal(context, castType, nodeToCast);
		if (castProposal != null) {
			proposals.add(castProposal);
		}
		
		// change method return statement to actual type
		if (parentNodeType == ASTNode.RETURN_STATEMENT) {
			ITypeBinding binding= nodeToCast.resolveTypeBinding();
			
			BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(selectedNode);
			if (binding != null && decl instanceof MethodDeclaration) {
				MethodDeclaration methodDeclaration= (MethodDeclaration) decl;
	
				ASTRewrite rewrite= new ASTRewrite(methodDeclaration);
				Type newReturnType= ASTResolving.getTypeFromTypeBinding(astRoot.getAST(), binding);
				rewrite.markAsReplaced(methodDeclaration.getReturnType(), newReturnType);
	
				String label= CorrectionMessages.getFormattedString("LocalCorrectionsSubProcessor.changereturntype.description", binding.getName()); //$NON-NLS-1$
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 2, image);
				proposal.addImport(binding);
				proposal.ensureNoModifications();
				proposals.add(proposal);
			}
		}

		if (parentNodeType == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
			VariableDeclarationFragment fragment= (VariableDeclarationFragment) selectedNode.getParent();
			ASTNode parent= fragment.getParent();
			Type type= null;
			if (parent instanceof VariableDeclarationStatement) {
				VariableDeclarationStatement stmt= (VariableDeclarationStatement) parent;
				if (stmt.fragments().size() == 1) {
					type= stmt.getType();
				}
			} else if (parent instanceof FieldDeclaration) {
				FieldDeclaration decl= (FieldDeclaration) parent;
				if (decl.fragments().size() == 1) {
					type= decl.getType();
				}
			}			
			if (type != null) {
				ImportEdit edit= new ImportEdit(cu, JavaPreferencesSettings.getCodeGenerationSettings());
				String typeName= edit.addImport(args[0]);

				String label= CorrectionMessages.getFormattedString("LocalCorrectionsSubProcessor.addcast_var.description", typeName); //$NON-NLS-1$
				ReplaceCorrectionProposal varProposal= new ReplaceCorrectionProposal(label, cu, type.getStartPosition(), type.getLength(), typeName, 1);
				varProposal.getRootTextEdit().add(edit);	
				proposals.add(varProposal);
			}
		}
			
	}
	

	private static boolean canCast(String castTarget, ITypeBinding bindingToCast) {
		int arrStart= castTarget.indexOf('[');
		if (arrStart != -1) {
			if (!bindingToCast.isArray()) {
				return "java.lang.Object".equals(bindingToCast.getQualifiedName()); //$NON-NLS-1$
			}
			castTarget= castTarget.substring(0, arrStart);
			bindingToCast= bindingToCast.getElementType();
			if (bindingToCast.isPrimitive()) {
				return false; // can't cast arrays of primitive types into each other
			}
		}
		
		Code targetCode= PrimitiveType.toCode(castTarget);
		if (bindingToCast.isPrimitive()) {
			Code castCode= PrimitiveType.toCode(bindingToCast.getName());
			if (castCode == targetCode) {
				return true;
			}
			return (targetCode != null && targetCode != PrimitiveType.BOOLEAN && castCode != PrimitiveType.BOOLEAN);
		} else {
			return targetCode == null;
		}
	}
	
	

	public static ASTRewriteCorrectionProposal getCastProposal(ICorrectionContext context, String castType, Expression nodeToCast) throws CoreException {
		ITypeBinding binding= nodeToCast.resolveTypeBinding();
		if (binding != null && !canCast(castType, binding)) {
			return null;
		}
		
		ICompilationUnit cu= context.getCompilationUnit();
		CompilationUnit astRoot= context.getASTRoot();
		
		ASTRewrite rewrite= new ASTRewrite(nodeToCast.getParent());
		
		String label;
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 1, image); //$NON-NLS-1$
		String simpleCastType= proposal.addImport(castType);
		
		if (nodeToCast.getNodeType() == ASTNode.CAST_EXPRESSION) {
			label= CorrectionMessages.getFormattedString("LocalCorrectionsSubProcessor.changecast.description", castType); //$NON-NLS-1$
			CastExpression expression= (CastExpression) nodeToCast;
			rewrite.markAsReplaced(expression.getType(), rewrite.createPlaceholder(simpleCastType, ASTRewrite.TYPE));
		} else {
			label= CorrectionMessages.getFormattedString("LocalCorrectionsSubProcessor.addcast.description", castType); //$NON-NLS-1$
			
			Expression expressionCopy= (Expression) rewrite.createCopy(nodeToCast);
			if (nodeToCast.getNodeType() == ASTNode.INFIX_EXPRESSION) {
				// infix has weaker precedence than cast
				ParenthesizedExpression parenthesizedExpression= astRoot.getAST().newParenthesizedExpression();
				parenthesizedExpression.setExpression(expressionCopy);
				expressionCopy= parenthesizedExpression;
			}
			
			Type typeCopy= (Type) rewrite.createPlaceholder(simpleCastType, ASTRewrite.TYPE);
			CastExpression castExpression= astRoot.getAST().newCastExpression();
			castExpression.setExpression(expressionCopy);
			castExpression.setType(typeCopy);
			
			rewrite.markAsReplaced(nodeToCast, castExpression);
		}
		proposal.setDisplayName(label);
		proposal.ensureNoModifications();
		
		return proposal;
	}
	
	public static void addUncaughtExceptionProposals(ICorrectionContext context, List proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();

		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= context.getCoveredNode();
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
		SurroundWithTryCatchRefactoring refactoring= new SurroundWithTryCatchRefactoring(cu, selectedNode.getStartPosition(), selectedNode.getLength(), settings, null);
		refactoring.setSaveChanges(false);
		if (refactoring.checkActivationBasics(astRoot, null).isOK()) {
			String label= CorrectionMessages.getString("LocalCorrectionsSubProcessor.surroundwith.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
			CUCorrectionProposal proposal= new CUCorrectionProposal(label, (CompilationUnitChange) refactoring.createChange(null), 4, image);
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
		if (surroundingTry != null && ASTNodes.isParent(surroundingTry.getBody(), selectedNode)) {
			ASTRewrite rewrite= new ASTRewrite(surroundingTry);
			String label= CorrectionMessages.getString("LocalCorrectionsSubProcessor.addadditionalcatch.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 5, image);
			
			AST ast= astRoot.getAST();
			List catchClauses= surroundingTry.catchClauses();
			for (int i= 0; i < uncaughtExceptions.length; i++) {
				String imp= proposal.addImport(uncaughtExceptions[i]);
				Name name= ASTNodeFactory.newName(ast, imp);
				SingleVariableDeclaration var= ast.newSingleVariableDeclaration();
				var.setName(ast.newSimpleName("e")); //$NON-NLS-1$
				var.setType(ast.newSimpleType(name));
				CatchClause newClause= ast.newCatchClause();
				newClause.setException(var);
				rewrite.markAsInserted(newClause);
				catchClauses.add(newClause);
			}
			proposal.ensureNoModifications();
			proposals.add(proposal);				
		}
		
		if (decl instanceof MethodDeclaration) {
			ASTRewrite rewrite= new ASTRewrite(astRoot);
			String label= CorrectionMessages.getString("LocalCorrectionsSubProcessor.addthrows.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 6, image);
			
			AST ast= astRoot.getAST();
			MethodDeclaration methodDecl= (MethodDeclaration) decl;
			List exceptions= methodDecl.thrownExceptions();
			for (int i= 0; i < uncaughtExceptions.length; i++) {
				String imp= proposal.addImport(uncaughtExceptions[i]);
				Name name= ASTNodeFactory.newName(ast, imp);
				rewrite.markAsInserted(name);
				exceptions.add(name);
			}
			for (int i= 0; i < exceptions.size(); i++) {
				Name elem= (Name) exceptions.get(i);
				if (canRemove(elem.resolveTypeBinding(), uncaughtExceptions)) {
					rewrite.markAsRemoved(elem);
				}
			}
			proposal.ensureNoModifications();
			proposals.add(proposal);
		}
	}
	
	private static boolean canRemove(ITypeBinding curr, ITypeBinding[] addedExceptions) {
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
	
	public static void addUnreachableCatchProposals(ICorrectionContext context, List proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();

		ASTNode selectedNode= context.getCoveringNode();
		if (selectedNode == null) {
			return;
		}
		
		if (selectedNode.getNodeType() == ASTNode.BLOCK && selectedNode.getParent().getNodeType() == ASTNode.CATCH_CLAUSE ) {
			CatchClause clause= (CatchClause) selectedNode.getParent();
			TryStatement tryStatement= (TryStatement) clause.getParent();
			ASTRewrite rewrite= new ASTRewrite(tryStatement.getParent());
			
			if (tryStatement.catchClauses().size() > 1 || tryStatement.getFinally() != null) {
				rewrite.markAsRemoved(clause);
			} else {
				List statements= tryStatement.getBody().statements();
				if (statements.size() > 0) {
					ASTNode placeholder= rewrite.createCopy((ASTNode) statements.get(0), (ASTNode) statements.get(statements.size() - 1));
					rewrite.markAsReplaced(tryStatement, placeholder);
				} else {
					rewrite.markAsRemoved(tryStatement);
				}
			}
			String label= CorrectionMessages.getString("LocalCorrectionsSubProcessor.removecatchclause.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 6, image);
			proposal.ensureNoModifications();
			proposals.add(proposal);
		}
	}	
	
	public static void addNLSProposals(ICorrectionContext context, List proposals) throws CoreException {
		final ICompilationUnit cu= context.getCompilationUnit();
		String name= CorrectionMessages.getString("LocalCorrectionsSubProcessor.externalizestrings.description"); //$NON-NLS-1$
		
		ChangeCorrectionProposal proposal= new ChangeCorrectionProposal(name, null, 0) {
			public void apply(IDocument document) {
				try {
					NLSRefactoring refactoring= new NLSRefactoring(cu, JavaPreferencesSettings.getCodeGenerationSettings());
					ExternalizeWizard wizard= new ExternalizeWizard(refactoring);
					String dialogTitle= CorrectionMessages.getString("LocalCorrectionsSubProcessor.externalizestrings.dialog.title"); //$NON-NLS-1$
					new RefactoringStarter().activate(refactoring, wizard, JavaPlugin.getActiveWorkbenchShell(), dialogTitle, true);
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
				}
			}
		};
		proposals.add(proposal);
		TextEdit edit= NLSUtil.createNLSEdit(cu, context.getOffset());
		if (edit != null) {
			String label= CorrectionMessages.getString("LocalCorrectionsSubProcessor.addnon-nls.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_NLS_NEVER_TRANSLATE);
			CUCorrectionProposal nlsProposal= new CUCorrectionProposal(label, cu, 6, image);
			nlsProposal.getRootTextEdit().add(edit);
			proposals.add(nlsProposal);
		}
	}
	
	/**
	 * A static field or method is accessed using a non-static reference. E.g.
     * <pre>
     * File f = new File();
     * f.pathSeparator;
     * </pre>
     * This correction changes <code>f</code> above to <code>File</code>.
     * 
	 * @param context
	 * @param proposals
	 */
	public static void addInstanceAccessToStaticProposals(ICorrectionContext context, List proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();

		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= context.getCoveredNode();
		if (selectedNode == null) {
			return;
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
		ITypeBinding declaringTypeBinding= null;
		if (accessBinding != null) {
			if (accessBinding instanceof IMethodBinding) {
				declaringTypeBinding= ((IMethodBinding) accessBinding).getDeclaringClass();
			} else if (accessBinding instanceof IVariableBinding) {
				declaringTypeBinding= ((IVariableBinding) accessBinding).getDeclaringClass();
			}
			if (declaringTypeBinding != null) {
				ASTRewrite rewrite= new ASTRewrite(selectedNode.getParent());
				rewrite.markAsReplaced(qualifier, astRoot.getAST().newSimpleName(declaringTypeBinding.getName()));

				String label= CorrectionMessages.getFormattedString("LocalCorrectionsSubProcessor.changeaccesstostaticdefining.description", declaringTypeBinding.getName()); //$NON-NLS-1$
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 2, image);
				proposal.addImport(declaringTypeBinding);
				proposal.ensureNoModifications();

				proposals.add(proposal);
			}
		}        
		if (qualifier != null) {
			ITypeBinding instanceTypeBinding= ASTResolving.normalizeTypeBinding(qualifier.resolveTypeBinding());
			if (instanceTypeBinding != null && instanceTypeBinding != declaringTypeBinding) {
				ASTRewrite rewrite= new ASTRewrite(selectedNode.getParent());
				rewrite.markAsReplaced(qualifier, astRoot.getAST().newSimpleName(instanceTypeBinding.getName()));
				
				String label= CorrectionMessages.getFormattedString("LocalCorrectionsSubProcessor.changeaccesstostatic.description", instanceTypeBinding.getName()); //$NON-NLS-1$
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 1, image);
				proposal.addImport(instanceTypeBinding);
				proposal.ensureNoModifications();

				proposals.add(proposal);
			}
		}
		ModifierCorrectionSubProcessor.addNonAccessibleMemberProposal(context, proposals, ModifierCorrectionSubProcessor.TO_NON_STATIC);
	}

	public static void addUnimplementedMethodsProposals(ICorrectionContext context, List proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();
		ASTNode selectedNode= context.getCoveringNode();
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
			ASTRewriteCorrectionProposal proposal= ModifierCorrectionSubProcessor.getMakeTypeStaticProposal(cu, typeDeclaration);
			proposals.add(proposal);
		}
	}

	public static void addUninitializedLocalVariableProposal(ICorrectionContext context, List proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();

		ASTNode selectedNode= context.getCoveringNode();
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
			ASTRewrite rewrite= new ASTRewrite(node.getParent());
			
			VariableDeclarationFragment fragment= (VariableDeclarationFragment) node;
			if (fragment.getInitializer() != null) {
				return;
			}
			Expression expression= ASTResolving.getInitExpression(astRoot.getAST(), varBinding.getType());
			if (expression == null) {
				return;
			}
			fragment.setInitializer(expression);
			rewrite.markAsInserted(expression);

			String label= CorrectionMessages.getString("LocalCorrectionsSubProcessor.uninitializedvariable.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 6, image);
			proposal.ensureNoModifications();
			proposals.add(proposal);			
		}
	}

	public static void addConstructorFromSuperclassProposal(ICorrectionContext context, List proposals) {
		ASTNode selectedNode= context.getCoveringNode();
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
				proposals.add(new ConstructorFromSuperclassProposal(cu, typeDeclaration, curr, 2));
			}
		}
	}

	public static void addUnusedMemberProposal(ICorrectionContext context, List proposals) throws CoreException {
		ASTNode selectedNode= context.getCoveringNode();
		if (selectedNode == null) {
			return;
		}
		BodyDeclaration declaration= ASTResolving.findParentBodyDeclaration(selectedNode);
		if (declaration != null) {
			
			ASTNode nodeToRemove= declaration;
			if (declaration.getNodeType() == ASTNode.FIELD_DECLARATION) {
				List fragments= ((FieldDeclaration) declaration).fragments();
				if (fragments.size() > 1) {
					for (int i= 0; i < fragments.size(); i++) {
						VariableDeclarationFragment node= (VariableDeclarationFragment) fragments.get(i);
						if (ASTNodes.isParent(selectedNode, node)) {
							nodeToRemove= node;
							break;
						}
					}
				}
			}
			
			ASTRewrite rewrite= new ASTRewrite(nodeToRemove.getParent());		
			rewrite.markAsRemoved(nodeToRemove);
			
			String label= CorrectionMessages.getString("LocalCorrectionsSubProcessor.removeunusedmember.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 6, image);
			proposal.ensureNoModifications();
			proposals.add(proposal);			
		}
		
	}
	
}
