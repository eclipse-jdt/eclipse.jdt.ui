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

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.PrimitiveType.Code;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSUtil;
import org.eclipse.jdt.internal.corext.refactoring.surround.ExceptionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryCatchRefactoring;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.refactoring.nls.ExternalizeWizard;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
  */
public class LocalCorrectionsSubProcessor {

	public static void addCastProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		String[] args= problem.getProblemArguments();
		if (args.length != 2) {
			return;
		}
			
		ICompilationUnit cu= context.getCompilationUnit();
		String castType= args[1];

		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveredNode(astRoot);
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

		ASTRewriteCorrectionProposal castProposal= getCastProposal(context, castType, nodeToCast, 5);
		if (castProposal != null) {
			proposals.add(castProposal);
		}
		
		ITypeBinding currBinding= nodeToCast.resolveTypeBinding();
		
		if (currBinding == null || "void".equals(currBinding.getName())) { //$NON-NLS-1$
			return;
		}
		
		// change method return statement to actual type
		if (parentNodeType == ASTNode.RETURN_STATEMENT) {
			BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(selectedNode);
			if (decl instanceof MethodDeclaration) {
				MethodDeclaration methodDeclaration= (MethodDeclaration) decl;
	
				currBinding= Bindings.normalizeTypeBinding(currBinding);
				if (currBinding == null) {
					currBinding= astRoot.getAST().resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
				}
	
				ASTRewrite rewrite= new ASTRewrite(methodDeclaration);
	
				String label= CorrectionMessages.getFormattedString("LocalCorrectionsSubProcessor.changereturntype.description", currBinding.getName()); //$NON-NLS-1$
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, cu, rewrite, 6, image);
				String returnTypeName= proposal.addImport(currBinding);
				
				Type newReturnType= ASTNodeFactory.newType(astRoot.getAST(), returnTypeName);
				rewrite.markAsReplaced(methodDeclaration.getReturnType(), newReturnType);
				
				String returnKey= "return"; //$NON-NLS-1$
				proposal.markAsLinked(rewrite, newReturnType, true, returnKey);
				ITypeBinding[] typeSuggestions= ASTResolving.getRelaxingTypes(astRoot.getAST(), currBinding);
				for (int i= 0; i < typeSuggestions.length; i++) {
					proposal.addLinkedModeProposal(returnKey, typeSuggestions[i]);
				}
				
				proposal.ensureNoModifications();
				proposals.add(proposal);
			}
		}

		if (parentNodeType == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
			VariableDeclarationFragment fragment= (VariableDeclarationFragment) selectedNode.getParent();
			ASTNode parent= fragment.getParent();

			Type typeNode= null;			
			if (parent instanceof VariableDeclarationStatement) {
				VariableDeclarationStatement stmt= (VariableDeclarationStatement) parent;
				if (stmt.fragments().size() == 1) {
					typeNode= stmt.getType();
				}
			} else if (parent instanceof FieldDeclaration) {
				FieldDeclaration decl= (FieldDeclaration) parent;
				if (decl.fragments().size() == 1) {
					typeNode= decl.getType();
				}
			}
			
			if (typeNode != null) {
				currBinding= Bindings.normalizeTypeBinding(currBinding);
				if (currBinding == null) {
					currBinding= astRoot.getAST().resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
				}
				
				TextBuffer buffer= null;
				try {
					buffer= TextBuffer.acquire((IFile)WorkingCopyUtil.getOriginal(cu).getResource());
					ImportRewrite importRewrite= new ImportRewrite(cu, JavaPreferencesSettings.getCodeGenerationSettings());
					String typeName= importRewrite.addImport(currBinding);
	
					String label= CorrectionMessages.getFormattedString("LocalCorrectionsSubProcessor.changevartype.description", typeName); //$NON-NLS-1$
					ReplaceCorrectionProposal varProposal= new ReplaceCorrectionProposal(label, cu, typeNode.getStartPosition(), typeNode.getLength(), typeName, 5);
					varProposal.getRootTextEdit().addChild(importRewrite.createEdit(buffer));
					proposals.add(varProposal);
				} finally {
					if (buffer != null)
						TextBuffer.release(buffer);
				}	
			}
		}
			
	}	

	private static boolean canCast(String castTarget, ITypeBinding bindingToCast) {
		bindingToCast= Bindings.normalizeTypeBinding(bindingToCast);
		if (bindingToCast == null) {
			return false;
		}
		
		
		int arrStart= castTarget.indexOf('[');
		if (arrStart != -1) {
			if (!bindingToCast.isArray()) {
				return "java.lang.Object".equals(bindingToCast.getQualifiedName()); //$NON-NLS-1$
			}
			castTarget= castTarget.substring(0, arrStart);
			bindingToCast= bindingToCast.getElementType();
			if (bindingToCast.isPrimitive() && !castTarget.equals(bindingToCast.getName())) {
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
	
	

	public static ASTRewriteCorrectionProposal getCastProposal(IInvocationContext context, String castType, Expression nodeToCast, int relevance) throws CoreException {
		ITypeBinding binding= nodeToCast.resolveTypeBinding();
		if (binding != null && !canCast(castType, binding)) {
			return null;
		}
		
		ICompilationUnit cu= context.getCompilationUnit();
		CompilationUnit astRoot= context.getASTRoot();
		
		ASTRewrite rewrite= new ASTRewrite(nodeToCast.getParent());
		
		String label;
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, relevance, image); //$NON-NLS-1$
		String simpleCastType= proposal.addImport(castType);
		
		if (nodeToCast.getNodeType() == ASTNode.CAST_EXPRESSION) {
			label= CorrectionMessages.getFormattedString("LocalCorrectionsSubProcessor.changecast.description", castType); //$NON-NLS-1$
			CastExpression expression= (CastExpression) nodeToCast;
			rewrite.markAsReplaced(expression.getType(), rewrite.createPlaceholder(simpleCastType, ASTRewrite.TYPE));
		} else {
			label= CorrectionMessages.getFormattedString("LocalCorrectionsSubProcessor.addcast.description", castType); //$NON-NLS-1$
			
			Expression expressionCopy= (Expression) rewrite.createCopy(nodeToCast);
			int nodeType= nodeToCast.getNodeType();
			
			if (nodeType == ASTNode.INFIX_EXPRESSION || nodeType == ASTNode.CONDITIONAL_EXPRESSION 
				|| nodeType == ASTNode.ASSIGNMENT || nodeType == ASTNode.INSTANCEOF_EXPRESSION) {
				// nodes have weaker precedence than cast
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
		if (surroundingTry != null && ASTNodes.isParent(selectedNode, surroundingTry.getBody())) {
			ASTRewrite rewrite= new ASTRewrite(surroundingTry);
			String label= CorrectionMessages.getString("LocalCorrectionsSubProcessor.addadditionalcatch.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
			LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, cu, rewrite, 5, image);
			
			AST ast= astRoot.getAST();
			List catchClauses= surroundingTry.catchClauses();
			for (int i= 0; i < uncaughtExceptions.length; i++) {
				ITypeBinding excBinding= uncaughtExceptions[i];
				String varName= "e"; //$NON-NLS-1$
				String imp= proposal.addImport(excBinding);
				Name name= ASTNodeFactory.newName(ast, imp);
				SingleVariableDeclaration var= ast.newSingleVariableDeclaration();
				var.setName(ast.newSimpleName(varName));
				var.setType(ast.newSimpleType(name));
				CatchClause newClause= ast.newCatchClause();
				newClause.setException(var);
				String catchBody = StubUtility.getCatchBodyContent(cu, excBinding.getName(), varName, String.valueOf('\n'));
				if (catchBody != null) {
					ASTNode node= rewrite.createPlaceholder(catchBody, ASTRewrite.STATEMENT);
					newClause.getBody().statements().add(node);
				}
				rewrite.markAsInserted(newClause);
				catchClauses.add(newClause);
				
				String typeKey= "type" + i; //$NON-NLS-1$
				String nameKey= "name" + i; //$NON-NLS-1$
				proposal.markAsLinked(rewrite, var.getType(), false, typeKey); //$NON-NLS-1$
				proposal.markAsLinked(rewrite, var.getName(), false, nameKey); //$NON-NLS-1$
				addExceptionTypeLinkProposals(proposal, excBinding, typeKey);
			}
			proposal.ensureNoModifications();
			proposals.add(proposal);				
		}
		
		if (decl instanceof MethodDeclaration) {
			ASTRewrite rewrite= new ASTRewrite(astRoot);
			String label= CorrectionMessages.getString("LocalCorrectionsSubProcessor.addthrows.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
			LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, cu, rewrite, 6, image);
			
			AST ast= astRoot.getAST();
			MethodDeclaration methodDecl= (MethodDeclaration) decl;
			List exceptions= methodDecl.thrownExceptions();
			for (int i= 0; i < uncaughtExceptions.length; i++) {
				String imp= proposal.addImport(uncaughtExceptions[i]);
				Name name= ASTNodeFactory.newName(ast, imp);
				rewrite.markAsInserted(name);
				exceptions.add(name);
				String typeKey= "type" + i; //$NON-NLS-1$
				proposal.markAsLinked(rewrite, name, false, typeKey); //$NON-NLS-1$
				addExceptionTypeLinkProposals(proposal, uncaughtExceptions[i], typeKey);
			}
			for (int i= 0; i < exceptions.size(); i++) {
				Name elem= (Name) exceptions.get(i);
				if (canRemoveException(elem.resolveTypeBinding(), uncaughtExceptions)) {
					rewrite.markAsRemoved(elem);
				}
			}
			proposal.ensureNoModifications();
			proposals.add(proposal);
		}
	}
	
	private static void addExceptionTypeLinkProposals(LinkedCorrectionProposal proposal, ITypeBinding exc, String key) {
		// all superclasses except Object
		while (exc != null && !"java.lang.Object".equals(exc.getQualifiedName())) { //$NON-NLS-1$
			proposal.addLinkedModeProposal(key, exc);
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
	
	public static void addUnreachableCatchProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode == null) {
			return;
		}

		QuickAssistProcessor.getCatchClauseToThrowsProposals(context, selectedNode, proposals);
		
		if (true) {
			return;
		}
		
		ICompilationUnit cu= context.getCompilationUnit();
		
		if (selectedNode.getNodeType() == ASTNode.BLOCK && selectedNode.getParent().getNodeType() == ASTNode.CATCH_CLAUSE ) {
			CatchClause clause= (CatchClause) selectedNode.getParent();
			TryStatement tryStatement= (TryStatement) clause.getParent();
			ASTRewrite rewrite= new ASTRewrite(tryStatement.getParent());
			
			if (tryStatement.catchClauses().size() > 1 || tryStatement.getFinally() != null) {
				rewrite.markAsRemoved(clause);
			} else {
				List statements= tryStatement.getBody().statements();
				if (statements.size() > 0) {
					ASTNode placeholder= rewrite.collapseNodes(statements, 0, statements.size());
					rewrite.markAsReplaced(tryStatement, rewrite.createCopy(placeholder));
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
	
	public static void addNLSProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		final ICompilationUnit cu= context.getCompilationUnit();
		if (! NLSRefactoring.isAvailable(cu)){
			return;
		}
		String name= CorrectionMessages.getString("LocalCorrectionsSubProcessor.externalizestrings.description"); //$NON-NLS-1$
		
		ChangeCorrectionProposal proposal= new ChangeCorrectionProposal(name, null, 5) {
			public void apply(IDocument document) {
				try {
					NLSRefactoring refactoring= NLSRefactoring.create(cu, JavaPreferencesSettings.getCodeGenerationSettings());
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
			nlsProposal.getRootTextEdit().addChild(edit);
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
					ASTRewrite rewrite= new ASTRewrite(selectedNode.getParent());

					String label= CorrectionMessages.getFormattedString("LocalCorrectionsSubProcessor.indirectaccesstostatic.description", declaringTypeBinding.getName()); //$NON-NLS-1$
					Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
					ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 6, image);
					String typeName= proposal.addImport(declaringTypeBinding);
					rewrite.markAsReplaced(qualifier, ASTNodeFactory.newName(astRoot.getAST(), typeName));
				
					proposal.ensureNoModifications();
					proposals.add(proposal);					
				}
			}
			return;
		}

		
		ITypeBinding declaringTypeBinding= null;
		if (accessBinding != null) {
			declaringTypeBinding= getDeclaringTypeBinding(accessBinding);
			if (declaringTypeBinding != null) {
				ASTRewrite rewrite= new ASTRewrite(selectedNode.getParent());

				String label= CorrectionMessages.getFormattedString("LocalCorrectionsSubProcessor.changeaccesstostaticdefining.description", declaringTypeBinding.getName()); //$NON-NLS-1$
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 6, image);
				String typeName= proposal.addImport(declaringTypeBinding);
				rewrite.markAsReplaced(qualifier, ASTNodeFactory.newName(astRoot.getAST(), typeName));
				
				proposal.ensureNoModifications();

				proposals.add(proposal);
			}
		}        
		if (qualifier != null) {
			ITypeBinding instanceTypeBinding= Bindings.normalizeTypeBinding(qualifier.resolveTypeBinding());
			if (instanceTypeBinding != null && instanceTypeBinding != declaringTypeBinding) {
				ASTRewrite rewrite= new ASTRewrite(selectedNode.getParent());
				
				String label= CorrectionMessages.getFormattedString("LocalCorrectionsSubProcessor.changeaccesstostatic.description", instanceTypeBinding.getName()); //$NON-NLS-1$
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 5, image);
				String typeName= proposal.addImport(instanceTypeBinding);
				rewrite.markAsReplaced(qualifier, ASTNodeFactory.newName(astRoot.getAST(), typeName));

				
				proposal.ensureNoModifications();

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
	
	

	public static void addUnimplementedMethodsProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
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
			ASTRewriteCorrectionProposal proposal= ModifierCorrectionSubProcessor.getMakeTypeStaticProposal(cu, typeDeclaration, 5);
			proposals.add(proposal);
		}
	}

	public static void addUninitializedLocalVariableProposal(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
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
			ASTRewrite rewrite= new ASTRewrite(node.getParent());
			
			VariableDeclarationFragment fragment= (VariableDeclarationFragment) node;
			if (fragment.getInitializer() != null) {
				return;
			}
			Expression expression= ASTNodeFactory.newDefaultExpression(astRoot.getAST(), varBinding.getType());
			if (expression == null) {
				return;
			}
			fragment.setInitializer(expression);
			rewrite.markAsInserted(expression);

			String label= CorrectionMessages.getString("LocalCorrectionsSubProcessor.uninitializedvariable.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			
			LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, cu, rewrite, 6, image);
			proposal.markAsLinked(rewrite, expression, false, "initializer"); //$NON-NLS-1$
			
			proposal.ensureNoModifications();
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

	public static void addUnusedMemberProposal(IInvocationContext context, IProblemLocation problem,  Collection proposals) throws CoreException {
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

	public static void addSuperfluousSemicolonProposal(IInvocationContext context, IProblemLocation problem,  Collection proposals) throws CoreException {
		String label= CorrectionMessages.getString("LocalCorrectionsSubProcessor.removesemicolon.description"); //$NON-NLS-1$
		ReplaceCorrectionProposal proposal= new ReplaceCorrectionProposal(label, context.getCompilationUnit(), problem.getOffset(), problem.getLength(), "", 6); //$NON-NLS-1$
		proposals.add(proposal);
	}
	
	public static void addUnnecessaryCastProposal(IInvocationContext context, IProblemLocation problem,  Collection proposals) throws CoreException {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		
		ASTNode curr= selectedNode;
		while (curr instanceof ParenthesizedExpression) {
			curr= ((ParenthesizedExpression) curr).getExpression();
		}
		
		if (curr instanceof CastExpression) {
			ASTRewrite rewrite= new ASTRewrite(selectedNode.getParent());
			
			CastExpression cast= (CastExpression) curr;
			Expression expression= cast.getExpression();
			ASTNode placeholder= rewrite.createCopy(expression);
			
			if (ASTNodes.needsParentheses(expression)) {
				rewrite.markAsReplaced(curr, placeholder);
			} else {
				rewrite.markAsReplaced(selectedNode, placeholder);
			}
			
			String label= CorrectionMessages.getString("LocalCorrectionsSubProcessor.unnecessarycast.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 5, image);
			proposals.add(proposal);
		}
	}

	public static void addUnnecessaryInstanceofProposal(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
			
		if (selectedNode instanceof InstanceofExpression) {
			ASTRewrite rewrite= new ASTRewrite(selectedNode.getParent());
			
			InstanceofExpression inst= (InstanceofExpression) selectedNode;
			
			AST ast= inst.getAST();
			InfixExpression expression= ast.newInfixExpression();
			expression.setLeftOperand((Expression) rewrite.createCopy(inst.getLeftOperand()));
			expression.setOperator(InfixExpression.Operator.NOT_EQUALS);
			expression.setRightOperand(ast.newNullLiteral());
			
			
			if (false/*ASTNodes.needsParentheses(expression)*/) {
				ParenthesizedExpression parents= ast.newParenthesizedExpression();
				parents.setExpression(expression);
				rewrite.markAsReplaced(inst, parents);
			} else {
				rewrite.markAsReplaced(inst, expression);
			}
			
			String label= CorrectionMessages.getString("LocalCorrectionsSubProcessor.unnecessaryinstanceof.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 5, image);
			proposals.add(proposal);
		}
	
	}



	
}
