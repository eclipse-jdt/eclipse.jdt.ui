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

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSRefactoring;
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
		
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		String label= CorrectionMessages.getFormattedString("LocalCorrectionsSubProcessor.addcast.description", castType); //$NON-NLS-1$
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 1, image);
		String simpleCastType= proposal.addImport(castType);
	
		Expression expressionCopy= (Expression) rewrite.createCopy(nodeToCast);
		Type typeCopy= (Type) rewrite.createPlaceholder(simpleCastType, ASTRewrite.TYPE);
		CastExpression castExpression= astRoot.getAST().newCastExpression();
		castExpression.setExpression(expressionCopy);
		castExpression.setType(typeCopy);
		
		rewrite.markAsReplaced(nodeToCast, castExpression);
		proposal.ensureNoModifications();
		
		proposals.add(proposal);
		
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

				label= CorrectionMessages.getFormattedString("LocalCorrectionsSubProcessor.addcast_var.description", typeName); //$NON-NLS-1$
				ReplaceCorrectionProposal varProposal= new ReplaceCorrectionProposal(label, cu, type.getStartPosition(), type.getLength(), typeName, 1);
				varProposal.getRootTextEdit().add(edit);	
				proposals.add(varProposal);
			}
		}
			
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
		
		TryStatement surroundingTry= (TryStatement) ASTNodes.getParent(selectedNode, ASTNode.TRY_STATEMENT);
		if (surroundingTry != null) {
			ASTRewrite rewrite= new ASTRewrite(astRoot);
			String label= CorrectionMessages.getString("LocalCorrectionsSubProcessor.addadditionalcatch.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 5, image);
			
			AST ast= astRoot.getAST();
			List catchClauses= surroundingTry.catchClauses();
			for (int i= 0; i < uncaughtExceptions.length; i++) {
				String imp= proposal.addImport(uncaughtExceptions[i]);
				Name name= ASTNodeFactory.newName(ast, imp);
				SingleVariableDeclaration var= ast.newSingleVariableDeclaration();
				var.setName(ast.newSimpleName("e"));
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
			proposal.ensureNoModifications();
			proposals.add(proposal);
		}
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
					NLSRefactoring refactoring= new NLSRefactoring(cu);
					ExternalizeWizard wizard= new ExternalizeWizard(refactoring);
					String dialogTitle= CorrectionMessages.getString("LocalCorrectionsSubProcessor.externalizestrings.dialog.title"); //$NON-NLS-1$
					new RefactoringStarter().activate(refactoring, wizard, dialogTitle, true);
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
				}
			}
		};
		proposals.add(proposal);
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
		
        if (selectedNode instanceof QualifiedName) {
            qualifier= ((QualifiedName) selectedNode).getQualifier();
        } else if (selectedNode instanceof SimpleName) {
        	ASTNode parent= selectedNode.getParent();
        	if (parent instanceof FieldAccess) {
        		qualifier= ((FieldAccess) parent).getExpression();
        	}
        } else if (selectedNode instanceof MethodInvocation) {
        	qualifier= ((MethodInvocation) selectedNode).getExpression();
        }
		if (qualifier != null) {
			ITypeBinding typeBinding= ASTResolving.normalizeTypeBinding(qualifier.resolveTypeBinding());
			if (typeBinding != null) {
				ASTRewrite rewrite= new ASTRewrite(selectedNode.getParent());
				rewrite.markAsReplaced(qualifier, astRoot.getAST().newSimpleName(typeBinding.getName()));
				
				String label= CorrectionMessages.getString("LocalCorrectionsSubProcessor.changeaccesstostatic.description");
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 1, image);
				proposal.addImport(typeBinding);
				proposal.ensureNoModifications();

				proposals.add(proposal);
			}
		}
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
			UnimplementedMethodsCompletionProposal proposal= new UnimplementedMethodsCompletionProposal(cu, typeNode, 0);
			proposals.add(proposal);
		}
		if (typeNode instanceof TypeDeclaration) {
			TypeDeclaration typeDeclaration= (TypeDeclaration) typeNode;
			ASTRewriteCorrectionProposal proposal= ModifierCorrectionSubProcessor.getMakeTypeStaticProposal(cu, typeDeclaration);
			proposals.add(proposal);
		}
	}


}
