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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryCatchRefactoring;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.refactoring.nls.ExternalizeWizard;

/**
  */
public class LocalCorrectionsSubProcessor {

	public static void addCastProposals(ProblemPosition problemPos, ArrayList proposals) throws CoreException {
		String[] args= problemPos.getArguments();
		if (args.length != 2) {
			return;
		}
			
		ICompilationUnit cu= problemPos.getCompilationUnit();
		String castDestType= args[1];

		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTNode selectedNode= ASTResolving.findSelectedNode(astRoot, problemPos.getOffset(), problemPos.getLength());
		
		int pos= problemPos.getOffset();
		if (selectedNode != null && selectedNode.getParent() != null) {
			int parentNodeType= selectedNode.getParent().getNodeType();
			if (parentNodeType == ASTNode.ASSIGNMENT) {
				Assignment assign= (Assignment) selectedNode.getParent();
				if (selectedNode.equals(assign.getLeftHandSide())) {
					pos= assign.getRightHandSide().getStartPosition();
				}
			} else if (parentNodeType == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
				VariableDeclarationFragment frag= (VariableDeclarationFragment) selectedNode.getParent();
				if (selectedNode.equals(frag.getName())) {
					pos= frag.getInitializer().getStartPosition();
				}
			}
		}
		String simpleCastDestType= Signature.getSimpleName(castDestType);
		
		String cast= '(' + simpleCastDestType + ')';
		String formatted= StubUtility.codeFormat(cast + 'x', 0, "");  //$NON-NLS-1$
		if (formatted.charAt(formatted.length() - 1) == 'x') {
			cast= formatted.substring(0, formatted.length() - 1);
		}
					
		String label= CorrectionMessages.getFormattedString("LocalCorrectionsSubProcessor.addcast.description", castDestType); //$NON-NLS-1$
		InsertCorrectionProposal proposal= new InsertCorrectionProposal(label, cu, pos, cast, 1);
		
        addImportToProposal(cu, castDestType, proposal);
		
		proposals.add(proposal);
		
		if (selectedNode != null && selectedNode.getParent() instanceof VariableDeclarationFragment) {
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
				String castType= args[0];
				String simpleCastType= Signature.getSimpleName(castType);

				label= CorrectionMessages.getFormattedString("LocalCorrectionsSubProcessor.addcast_var.description", simpleCastType); //$NON-NLS-1$
				ReplaceCorrectionProposal varProposal= new ReplaceCorrectionProposal(label, cu, type.getStartPosition(), type.getLength(), simpleCastType, 1);

                addImportToProposal(cu, castType, varProposal);
				
				proposals.add(varProposal);
			}
		}
			
	}
	
	public static void addUncaughtExceptionProposals(ProblemPosition problemPos, ArrayList proposals) throws CoreException {
		ICompilationUnit cu= problemPos.getCompilationUnit();

		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		ASTNode selectedNode= ASTResolving.findSelectedNode(astRoot, problemPos.getOffset(), problemPos.getLength());
		if (selectedNode == null) {
			return;
		}
		while (selectedNode != null && !(selectedNode instanceof Statement)) {
			selectedNode= selectedNode.getParent();
		}
		if (selectedNode != null) {
			CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
			SurroundWithTryCatchRefactoring refactoring= new SurroundWithTryCatchRefactoring(cu, selectedNode.getStartPosition(), selectedNode.getLength(), settings, null);
			refactoring.setSaveChanges(false);
			if (refactoring.checkActivationBasics(astRoot, null).isOK()) {
				String label= CorrectionMessages.getString("LocalCorrectionsSubProcessor.surroundwith.description"); //$NON-NLS-1$
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
				CUCorrectionProposal proposal= new CUCorrectionProposal(label, (CompilationUnitChange) refactoring.createChange(null), 0, image);
				proposals.add(proposal);
			}
		}
		
		BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(selectedNode);
		if (decl instanceof MethodDeclaration) {
			ASTRewrite rewrite= new ASTRewrite(astRoot);
			
			String uncaughtName= problemPos.getArguments()[0];
			
			MethodDeclaration methodDecl= (MethodDeclaration) decl;
			List exceptions= methodDecl.thrownExceptions();
			SimpleName addedException= astRoot.getAST().newSimpleName(Signature.getSimpleName(uncaughtName));
			exceptions.add(addedException);
			
			rewrite.markAsInserted(addedException);
			
			String label= CorrectionMessages.getString("LocalCorrectionsSubProcessor.addthrows.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 0, image);
			proposal.addImport(uncaughtName);
            
			proposals.add(proposal);
		}
	}
	
	public static void addNLSProposals(ProblemPosition problemPos, ArrayList proposals) throws CoreException {
		final ICompilationUnit cu= problemPos.getCompilationUnit();
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
	 * @param problemPos
	 * @param proposals
	 */
	public static void addAccessToStaticProposals(ProblemPosition problemPos, List proposals) throws CoreException {
		ICompilationUnit cu= problemPos.getCompilationUnit();

		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		ASTNode selectedNode= ASTResolving.findSelectedNode(astRoot, problemPos.getOffset(), problemPos.getLength());

		if (selectedNode != null) {
			Expression qualifier= getQualifier(selectedNode);
			if (qualifier != null) {
				ITypeBinding typeBinding= ASTResolving.normalizeTypeBinding(qualifier.resolveTypeBinding());
				if (typeBinding != null) {
					ASTRewrite rewrite= new ASTRewrite(selectedNode.getParent());
					rewrite.markAsReplaced(qualifier, astRoot.getAST().newSimpleName(typeBinding.getName()));
					
					String label= CorrectionMessages.getString("LocalCorrectionsSubProcessor.changeaccesstostatic.description");
					Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
					ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 1, image);
					proposal.addImport(typeBinding);

					proposals.add(proposal);
				}
			}
		}
	}

	private static void addImportToProposal(ICompilationUnit cu, ITypeBinding type, CUCorrectionProposal proposal) throws CoreException {
        addImportToProposal(cu, Bindings.getFullyQualifiedImportName(type), proposal);
    }
            
    private static void addImportToProposal(ICompilationUnit cu, String typeName, CUCorrectionProposal proposal) throws CoreException {
        ImportEdit edit= new ImportEdit(cu, JavaPreferencesSettings.getCodeGenerationSettings());
        edit.addImport(typeName);
        proposal.getRootTextEdit().add(edit);
	}

    private static Expression getQualifier(ASTNode node) {
        if (node instanceof QualifiedName) {
            return ((QualifiedName) node).getQualifier();
        }
        if (node instanceof SimpleName) {
        	ASTNode parent= node.getParent();
        	if (parent instanceof FieldAccess) {
        		return ((FieldAccess) parent).getExpression();
        	}
        	return null;
        }
        if (node instanceof MethodInvocation) {
        	return ((MethodInvocation) node).getExpression();
        }
        return null;
    }

}
