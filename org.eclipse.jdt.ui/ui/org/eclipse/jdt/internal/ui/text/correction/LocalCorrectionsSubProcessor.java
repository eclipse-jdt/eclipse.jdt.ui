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
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.Binding2JavaModel;
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
		String castType= args[1];

		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTNode selectedNode= ASTResolving.findSelectedNode(astRoot, problemPos.getOffset(), problemPos.getLength());
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

				proposals.add(proposal);
			}
		}
	}
	
	/**
	 * Method addStaticMethodRequestedProposal.
	 * @param problemPos
	 * @param proposals
	 */
	public static void addStaticMethodRequestedProposal(ProblemPosition problemPos, ArrayList proposals) throws JavaModelException {
		ICompilationUnit cu= problemPos.getCompilationUnit();

		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		ASTNode selectedNode= ASTResolving.findSelectedNode(astRoot, problemPos.getOffset(), problemPos.getLength());
		
		IBinding binding=null;
		if (selectedNode instanceof SimpleName) {
			binding= ((SimpleName) selectedNode).resolveBinding();
		} else if (selectedNode instanceof QualifiedName) {
			binding= ((QualifiedName) selectedNode).resolveBinding();
		} else if (selectedNode instanceof SimpleType) {
			binding= ((SimpleType) selectedNode).resolveBinding();
		} else if (selectedNode instanceof MethodInvocation) {
			binding= ((MethodInvocation) selectedNode).getName().resolveBinding();
		} else if (selectedNode instanceof SuperMethodInvocation) {
			binding= ((SuperMethodInvocation) selectedNode).getName().resolveBinding();
		} else if (selectedNode instanceof SuperFieldAccess) {
			binding= ((SuperFieldAccess) selectedNode).getName().resolveBinding();	
		} else if (selectedNode instanceof ClassInstanceCreation) {
			binding= ((ClassInstanceCreation) selectedNode).resolveConstructorBinding();	
		} else if (selectedNode instanceof SuperConstructorInvocation) {
			binding= ((SuperConstructorInvocation) selectedNode).resolveConstructorBinding();
		}
		ITypeBinding typeBinding= null;
		String name;
		if (binding instanceof IMethodBinding) {
			typeBinding= ((IMethodBinding) binding).getDeclaringClass();
			name= binding.getName() + "()";
		} else if (binding instanceof IVariableBinding) {
			typeBinding= ((IVariableBinding) binding).getDeclaringClass();
			name= binding.getName();
		} else if (binding instanceof ITypeBinding) {
			typeBinding= (ITypeBinding) binding;
			name= binding.getName();
		} else {
			return;
		}
		if (typeBinding.isFromSource()) {
			int includedModifiers= 0;
			int excludedModifiers= 0;
			String label;
			switch (problemPos.getId()) {
			case IProblem.StaticMethodRequested:
				label= CorrectionMessages.getFormattedString("LocalCorrectionsSubProcessor.changemodifiertostatic.description", name);
				includedModifiers= Modifier.STATIC;
				break;
			case IProblem.NotVisibleMethod:
			case IProblem.NotVisibleConstructor:
			case IProblem.NotVisibleField:
			case IProblem.NotVisibleType:
				excludedModifiers= Modifier.PRIVATE | Modifier.PROTECTED | Modifier.PUBLIC;
				includedModifiers= getNeededVisibility(selectedNode, typeBinding);
				label= CorrectionMessages.getFormattedString("LocalCorrectionsSubProcessor.changevisibility.description", new String[] { name, getVisibilityString(includedModifiers) });
				break;
			default:
				return;
			}
			ICompilationUnit targetCU= Binding2JavaModel.findCompilationUnit(typeBinding, cu.getJavaProject());
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			proposals.add(new ModifierChangeCompletionProposal(label, targetCU, binding, selectedNode, includedModifiers, excludedModifiers, 0, image));
		}
	}
	
	private static String getVisibilityString(int code) {
		if (Modifier.isPublic(code)) {
			return "public";
		}else if (Modifier.isProtected(code)) {
			return "protected";
		}
		return "default";
	}
	
	
	private static int getNeededVisibility(ASTNode currNode, ITypeBinding targetType) {
		ITypeBinding currNodeBinding= ASTResolving.getBindingOfParentType(currNode);
		
		ITypeBinding curr= currNodeBinding;
		while (curr != null) {
			if (curr.getKey().equals(targetType.getKey())) {
				return Modifier.PROTECTED;
			}
			curr= curr.getSuperclass();
		}
		if (currNodeBinding.getPackage().getKey().equals(targetType.getKey())) {
			return 0;
		}
		return Modifier.PUBLIC;
	}


}
