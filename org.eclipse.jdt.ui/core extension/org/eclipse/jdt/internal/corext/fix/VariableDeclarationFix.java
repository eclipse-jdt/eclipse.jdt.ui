/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.jdt.internal.corext.dom.VariableDeclarationRewrite;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;

public class VariableDeclarationFix extends AbstractFix {
	
	private static class WrittenNamesFinder extends GenericVisitor {
		
		private final HashSet fResult;
	
		public WrittenNamesFinder(HashSet result) {
			fResult= result;
		}
		
		/**
		 * {@inheritDoc}
		 */
		public boolean visit(SimpleName node) {	
			if (node.getParent() instanceof VariableDeclarationFragment)
				return super.visit(node);
			if (node.getParent() instanceof SingleVariableDeclaration)
				return super.visit(node);

			IBinding binding= node.resolveBinding();
			if (!(binding instanceof IVariableBinding))
				return super.visit(node);
			
			binding= ((IVariableBinding)binding).getVariableDeclaration();
			if (fResult.contains(binding))
				return super.visit(node);
			
			if (ASTResolving.isWriteAccess(node)) { 
				fResult.add(binding);
			}
			
			return super.visit(node);
		}
	}

	private static class VariableDeclarationFinder extends GenericVisitor {
		
		private final CompilationUnit fCompilationUnit;
		private final List fResult;
		private final HashSet fWrittenVariables;
		private final boolean fAddFinalFields;
		private final boolean fAddFinalParameters;
		private final boolean fAddFinalLocals;
		
		public VariableDeclarationFinder(boolean addFinalFields, 
				boolean addFinalParameters, 
				boolean addFinalLocals, 
				final CompilationUnit compilationUnit, final List result, final HashSet writtenVariables) {
			
			super();
			fAddFinalFields= addFinalFields;
			fAddFinalParameters= addFinalParameters;
			fAddFinalLocals= addFinalLocals;
			fCompilationUnit= compilationUnit;
			fResult= result;
			fWrittenVariables= writtenVariables;
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean visit(FieldDeclaration node) {
			if (fAddFinalFields)
				return handleFragments(node.fragments(), node);
			
			return false;
		}
		
		/**
		 * {@inheritDoc}
		 */
		public boolean visit(VariableDeclarationStatement node) {
			if (fAddFinalLocals)
				return handleFragments(node.fragments(), node);
			
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean visit(VariableDeclarationExpression node) {
			if (fAddFinalLocals && node.fragments().size() == 1) {
				ModifierChangeOperation op= createAddFinalOperation(((VariableDeclarationFragment)node.fragments().get(0)).getName(), fCompilationUnit, node);
				if (op == null)
					return false;
				
				fResult.add(op);
				return false;
			}
			return false;
		}
		
		private boolean handleFragments(List list, ASTNode declaration) {
			List toChange= new ArrayList();
			
			for (Iterator iter= list.iterator(); iter.hasNext();) {
				VariableDeclarationFragment fragment= (VariableDeclarationFragment)iter.next();
				SimpleName name= fragment.getName();
				IBinding resolveBinding= name.resolveBinding();
				if (!fWrittenVariables.contains(resolveBinding) && canAddFinal(resolveBinding, name, declaration))
					toChange.add(fragment);
			}
			
			if (toChange.size() == 0)
				return false;
			
			ModifierChangeOperation op= new ModifierChangeOperation(declaration, toChange, Modifier.FINAL, Modifier.VOLATILE);
			fResult.add(op);
			return false;
		}
		
		/**
		 * {@inheritDoc}
		 */
		public boolean visit(VariableDeclarationFragment node) {
			ModifierChangeOperation op= createAddFinalOperation(node.getName(), fCompilationUnit, node);
			if (op == null)
				return false;
			
			fResult.add(op);
			return false;
		}
		
		/**
		 * {@inheritDoc}
		 */
		public boolean visit(SingleVariableDeclaration node) {
			if (fAddFinalParameters && fAddFinalLocals) {
				ModifierChangeOperation op= createAddFinalOperation(node.getName(), fCompilationUnit, node);
				if (op == null)
					return false;
				
				fResult.add(op);
				return false;
			} else if (fAddFinalParameters) {
				IBinding binding= node.getName().resolveBinding();
				if (!(binding instanceof IVariableBinding))
					return false;
				
				IVariableBinding varBinding= (IVariableBinding)binding;
				if (!varBinding.isParameter())
					return false;
				
				ModifierChangeOperation op= createAddFinalOperation(node.getName(), fCompilationUnit, node);
				if (op == null)
					return false;
				
				fResult.add(op);
				return false;
			} else if (fAddFinalLocals) {
				IBinding binding= node.getName().resolveBinding();
				if (!(binding instanceof IVariableBinding))
					return false;
				
				IVariableBinding varBinding= (IVariableBinding)binding;
				if (varBinding.isParameter())
					return false;
				
				ModifierChangeOperation op= createAddFinalOperation(node.getName(), fCompilationUnit, node);
				if (op == null)
					return false;
				
				fResult.add(op);
				return false;
			}
			return false;
		}
	}
	
	private static class ModifierChangeOperation extends AbstractFixRewriteOperation {
		
		private final ASTNode fDeclaration;
		private final List fToChange;
		private final int fIncludedModifiers;
		private final int fExcludedModifiers;

		public ModifierChangeOperation(ASTNode declaration, List toChange, int includedModifiers, int excludedModifiers) {
			fDeclaration= declaration;
			fToChange= toChange;
			fIncludedModifiers= includedModifiers;
			fExcludedModifiers= excludedModifiers;	
		}
		
		/**
		 * {@inheritDoc}
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			
			TextEditGroup group= createTextEditGroup(FixMessages.VariableDeclarationFix_changeModifierOfUnknownToFinal_description);
			textEditGroups.add(group);
			
			if (fDeclaration instanceof VariableDeclarationStatement) {
				VariableDeclarationFragment[] toChange= (VariableDeclarationFragment[])fToChange.toArray(new VariableDeclarationFragment[fToChange.size()]);
				VariableDeclarationRewrite.rewriteModifiers((VariableDeclarationStatement)fDeclaration, toChange, fIncludedModifiers, fExcludedModifiers, rewrite, group);
			} else if (fDeclaration instanceof FieldDeclaration) {
				VariableDeclarationFragment[] toChange= (VariableDeclarationFragment[])fToChange.toArray(new VariableDeclarationFragment[fToChange.size()]);
				VariableDeclarationRewrite.rewriteModifiers((FieldDeclaration)fDeclaration, toChange, fIncludedModifiers, fExcludedModifiers, rewrite, group);
			} else if (fDeclaration instanceof SingleVariableDeclaration) {
				VariableDeclarationRewrite.rewriteModifiers((SingleVariableDeclaration)fDeclaration, fIncludedModifiers, fExcludedModifiers, rewrite, group);
			} else if (fDeclaration instanceof VariableDeclarationExpression) {
				VariableDeclarationRewrite.rewriteModifiers((VariableDeclarationExpression)fDeclaration, fIncludedModifiers, fExcludedModifiers, rewrite, group);	
			}
		}
	}
	
	public static IFix createChangeModifierToFinalFix(final CompilationUnit compilationUnit, ASTNode[] selectedNodes) {
		HashSet writtenNames= new HashSet(); 
		WrittenNamesFinder finder= new WrittenNamesFinder(writtenNames);
		compilationUnit.accept(finder);
		List ops= new ArrayList();
		VariableDeclarationFinder visitor= new VariableDeclarationFinder(true, true, true, compilationUnit, ops, writtenNames);
		if (selectedNodes.length == 1) {
			if (selectedNodes[0] instanceof SimpleName) {
				selectedNodes[0]= selectedNodes[0].getParent();
			}
			selectedNodes[0].accept(visitor);
		} else {
			for (int i= 0; i < selectedNodes.length; i++) {
				ASTNode selectedNode= selectedNodes[i];
				selectedNode.accept(visitor);
			}
		}
		if (ops.size() == 0)
			return null;
		
		IFixRewriteOperation[] result= (IFixRewriteOperation[])ops.toArray(new IFixRewriteOperation[ops.size()]);
		String label;
		if (result.length == 1) {
			label= FixMessages.VariableDeclarationFix_changeModifierOfUnknownToFinal_description;
		} else {
			label= FixMessages.VariableDeclarationFix_ChangeMidifiersToFinalWherPossible_description;
		}
		return new VariableDeclarationFix(label, compilationUnit, result);
	}
	
	public static IFix createCleanUp(CompilationUnit compilationUnit,
			boolean addFinalFields, boolean addFinalParameters, boolean addFinalLocals) {
		
		if (!addFinalFields && !addFinalParameters && !addFinalLocals)
			return null;
		
		HashSet writtenNames= new HashSet(); 
		WrittenNamesFinder finder= new WrittenNamesFinder(writtenNames);
		compilationUnit.accept(finder);
		List operations= new ArrayList();
		VariableDeclarationFinder visitor= new VariableDeclarationFinder(addFinalFields, addFinalParameters, addFinalLocals, compilationUnit, operations, writtenNames);
		compilationUnit.accept(visitor);
		
		if (operations.isEmpty())
			return null;
			
		return new VariableDeclarationFix(FixMessages.VariableDeclarationFix_changeModifierOfUnknownToFinal_description, compilationUnit, (IFixRewriteOperation[])operations.toArray(new IFixRewriteOperation[operations.size()]));
	}
	
	private static ModifierChangeOperation createAddFinalOperation(SimpleName name, CompilationUnit compilationUnit, ASTNode decl) {
		if (decl == null)
			return null;
		
		IBinding binding= name.resolveBinding();
		if (!canAddFinal(binding, name, decl))
			return null;
		
		if (isWrittenOutsideInitializer(compilationUnit, (IVariableBinding)binding))
			return null;
		
		if (decl instanceof SingleVariableDeclaration) {
			return new ModifierChangeOperation(decl, new ArrayList(), Modifier.FINAL, Modifier.VOLATILE);
		} else if (decl instanceof VariableDeclarationExpression) {
			return new ModifierChangeOperation(decl, new ArrayList(), Modifier.FINAL, Modifier.VOLATILE);
		} else if (decl instanceof VariableDeclarationFragment){
			VariableDeclarationFragment frag= (VariableDeclarationFragment)decl;
			decl= decl.getParent();
			if (decl instanceof FieldDeclaration || decl instanceof VariableDeclarationStatement) {
				List list= new ArrayList();
				list.add(frag);
				return new ModifierChangeOperation(decl, list, Modifier.FINAL, Modifier.VOLATILE);
			} else if (decl instanceof VariableDeclarationExpression) {
				return new ModifierChangeOperation(decl, new ArrayList(), Modifier.FINAL, Modifier.VOLATILE);
			}
		}
		
		return null;
	}

	private static boolean canAddFinal(IBinding binding, SimpleName name, ASTNode declNode) {
		if (!(binding instanceof IVariableBinding))
			return false;

		IVariableBinding varbinding= (IVariableBinding)binding;
		if (Modifier.isFinal(varbinding.getModifiers()))
			return false;
		
		ASTNode parent= ASTNodes.getParent(declNode, VariableDeclarationExpression.class);
		if (parent != null && ((VariableDeclarationExpression)parent).fragments().size() > 1)
			return false;
		
		if (varbinding.isField() && !Modifier.isPrivate(varbinding.getModifiers())) 
			return false;
		
		if (varbinding.isParameter()) {
			ASTNode varDecl= declNode.getParent();
			if (varDecl instanceof MethodDeclaration) {
				MethodDeclaration declaration= (MethodDeclaration)varDecl;
				if (declaration.getBody() == null)
					return false;
			}
		}
		
		return true;
	}

	private static boolean isWrittenOutsideInitializer(CompilationUnit compilationUnit, IVariableBinding varbinding) {
		SimpleName[] names= LinkedNodeFinder.findByBinding(compilationUnit, varbinding);
		for (int i= 0; i < names.length; i++) {
			SimpleName access= names[i];
			if (access.getParent() instanceof VariableDeclarationFragment)
				continue;
			if (access.getParent() instanceof SingleVariableDeclaration)
				continue;
			
			if (ASTResolving.isWriteAccess(access)) {				
				return true;
			}
		}
		return false;
	}

	protected VariableDeclarationFix(String name, CompilationUnit compilationUnit, IFixRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}

}
