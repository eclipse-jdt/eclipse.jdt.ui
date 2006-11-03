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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.fix.UnusedCodeCleanUp;
import org.eclipse.jdt.internal.ui.text.correction.JavadocTagsSubProcessor;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;

/**
 * Fix which removes unused code.
 */
public class UnusedCodeFix extends AbstractFix {
	
	private static class SideEffectFinder extends ASTVisitor {

		private final ArrayList fSideEffectNodes;

		public SideEffectFinder(ArrayList res) {
			fSideEffectNodes= res;
		}

		public boolean visit(Assignment node) {
			fSideEffectNodes.add(node);
			return false;
		}

		public boolean visit(PostfixExpression node) {
			fSideEffectNodes.add(node);
			return false;
		}

		public boolean visit(PrefixExpression node) {
			Object operator= node.getOperator();
			if (operator == PrefixExpression.Operator.INCREMENT || operator == PrefixExpression.Operator.DECREMENT) {
				fSideEffectNodes.add(node);
			}
			return false;
		}

		public boolean visit(MethodInvocation node) {
			fSideEffectNodes.add(node);
			return false;
		}

		public boolean visit(ClassInstanceCreation node) {
			fSideEffectNodes.add(node);
			return false;
		}

		public boolean visit(SuperMethodInvocation node) {
			fSideEffectNodes.add(node);
			return false;
		}
	}
	
	private static class RemoveImportOperation extends AbstractFixRewriteOperation {

		private final ImportDeclaration fImportDeclaration;
		
		public RemoveImportOperation(ImportDeclaration importDeclaration) {
			fImportDeclaration= importDeclaration;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.fix.AbstractFix.IFixRewriteOperation#rewriteAST(org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite, java.util.List)
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups) throws CoreException {
			ImportDeclaration node= fImportDeclaration;
			TextEditGroup group= createTextEditGroup(FixMessages.UnusedCodeFix_RemoveImport_description);
			cuRewrite.getASTRewrite().remove(node, group);
			textEditGroups.add(group);
		}
		
	}
	
	private static class RemoveUnusedMemberOperation extends AbstractFixRewriteOperation {

		private final SimpleName[] fUnusedNames;
		
		public RemoveUnusedMemberOperation(SimpleName[] unusedNames) {
			fUnusedNames= unusedNames;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.fix.AbstractFix.IFixRewriteOperation#rewriteAST(org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite, java.util.List)
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups) throws CoreException {
			for (int i= 0; i < fUnusedNames.length; i++) {
				removeUnusedName(cuRewrite.getASTRewrite(), fUnusedNames[i], cuRewrite.getRoot(), textEditGroups);	
			}
		}
		
		private void removeUnusedName(ASTRewrite rewrite, SimpleName simpleName, CompilationUnit completeRoot, List groups) {
			IBinding binding= simpleName.resolveBinding();
			CompilationUnit root= (CompilationUnit) simpleName.getRoot();
			String displayString= getDisplayString(binding);
			TextEditGroup group= createTextEditGroup(displayString);
			groups.add(group);
			if (binding.getKind() == IBinding.METHOD) {
				IMethodBinding decl= ((IMethodBinding) binding).getMethodDeclaration();
				ASTNode declaration= root.findDeclaringNode(decl);
				rewrite.remove(declaration, group);
			} else if (binding.getKind() == IBinding.TYPE) {
				ITypeBinding decl= ((ITypeBinding) binding).getTypeDeclaration();
				ASTNode declaration= root.findDeclaringNode(decl);
				rewrite.remove(declaration, group);
			} else if (binding.getKind() == IBinding.VARIABLE) {
				SimpleName nameNode= (SimpleName) NodeFinder.perform(completeRoot, simpleName.getStartPosition(), simpleName.getLength());
				SimpleName[] references= LinkedNodeFinder.findByBinding(completeRoot, nameNode.resolveBinding());
				for (int i= 0; i < references.length; i++) {
					removeVariableReferences(rewrite, references[i], group);
				}

				IVariableBinding bindingDecl= ((IVariableBinding) nameNode.resolveBinding()).getVariableDeclaration();
				ASTNode declaringNode= completeRoot.findDeclaringNode(bindingDecl);
				if (declaringNode instanceof SingleVariableDeclaration) {
					removeParamTag(rewrite, (SingleVariableDeclaration) declaringNode, group);
				}
			} else {
				// unexpected
			}
		}
		
		private String getDisplayString(IBinding binding) {
			switch (binding.getKind()) {
				case IBinding.TYPE:
					return FixMessages.UnusedCodeFix_RemoveUnusedType_description;
				case IBinding.METHOD:
					if (((IMethodBinding) binding).isConstructor()) {
						return FixMessages.UnusedCodeFix_RemoveUnusedConstructor_description;
					} else {
						return FixMessages.UnusedCodeFix_RemoveUnusedPrivateMethod_description;
					}
				case IBinding.VARIABLE:
					if (((IVariableBinding) binding).isField()) {
						return FixMessages.UnusedCodeFix_RemoveUnusedField_description;
					} else {
						return FixMessages.UnusedCodeFix_RemoveUnusedVariabl_description;
					}
				default:
					return ""; //$NON-NLS-1$
			}
		}

		private void removeParamTag(ASTRewrite rewrite, SingleVariableDeclaration varDecl, TextEditGroup group) {
			if (varDecl.getParent() instanceof MethodDeclaration) {
				Javadoc javadoc= ((MethodDeclaration) varDecl.getParent()).getJavadoc();
				if (javadoc != null) {
					TagElement tagElement= JavadocTagsSubProcessor.findParamTag(javadoc, varDecl.getName().getIdentifier());
					if (tagElement != null) {
						rewrite.remove(tagElement, group);
					}
				}
			}
		}
		
		/**
		 * Remove the field or variable declaration including the initializer.
		 */
		private void removeVariableReferences(ASTRewrite rewrite, SimpleName reference, TextEditGroup group) {
			ASTNode parent= reference.getParent();
			while (parent instanceof QualifiedName) {
				parent= parent.getParent();
			}
			if (parent instanceof FieldAccess) {
				parent= parent.getParent();
			}

			int nameParentType= parent.getNodeType();
			if (nameParentType == ASTNode.ASSIGNMENT) {
				Assignment assignment= (Assignment) parent;
				Expression rightHand= assignment.getRightHandSide();

				ASTNode assignParent= assignment.getParent();
				if (assignParent.getNodeType() == ASTNode.EXPRESSION_STATEMENT && rightHand.getNodeType() != ASTNode.ASSIGNMENT) {
					removeVariableWithInitializer(rewrite, rightHand, assignParent, group);
				}	else {
					rewrite.replace(assignment, rewrite.createCopyTarget(rightHand), group);
				}
			} else if (nameParentType == ASTNode.SINGLE_VARIABLE_DECLARATION) {
				rewrite.remove(parent, group);
			} else if (nameParentType == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
				VariableDeclarationFragment frag= (VariableDeclarationFragment) parent;
				ASTNode varDecl= frag.getParent();
				List fragments;
				if (varDecl instanceof VariableDeclarationExpression) {
					fragments= ((VariableDeclarationExpression) varDecl).fragments();
				} else if (varDecl instanceof FieldDeclaration) {
					fragments= ((FieldDeclaration) varDecl).fragments();
				} else {
					fragments= ((VariableDeclarationStatement) varDecl).fragments();
				}
				if (fragments.size() == fUnusedNames.length) {
					rewrite.remove(varDecl, group);
				} else {
					rewrite.remove(frag, group); // don't try to preserve
				}
			}
		}

		private void removeVariableWithInitializer(ASTRewrite rewrite, ASTNode initializerNode, ASTNode statementNode, TextEditGroup group) {
			ArrayList sideEffectNodes= new ArrayList();
			initializerNode.accept(new SideEffectFinder(sideEffectNodes));
			int nSideEffects= sideEffectNodes.size();
			if (nSideEffects == 0) {
				if (ASTNodes.isControlStatementBody(statementNode.getLocationInParent())) {
					rewrite.replace(statementNode, rewrite.getAST().newBlock(), group);
				} else {
					rewrite.remove(statementNode, group);
				}
			} else {
				// do nothing yet
			}
		}
	}
	
	private static class RemoveCastOperation extends AbstractFixRewriteOperation {

		private final CastExpression fCast;
		private final ASTNode fSelectedNode;

		public RemoveCastOperation(CastExpression cast, ASTNode selectedNode) {
			fCast= cast;
			fSelectedNode= selectedNode;
		}

		/**
		 * {@inheritDoc}
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups) throws CoreException {
			
			TextEditGroup group= createTextEditGroup(FixMessages.UnusedCodeFix_RemoveCast_description);
			textEditGroups.add(group);
			
			ASTRewrite rewrite= cuRewrite.getASTRewrite();

			CastExpression cast= fCast;
			Expression expression= cast.getExpression();
			ASTNode placeholder= rewrite.createCopyTarget(expression);

			if (ASTNodes.needsParentheses(expression)) {
				rewrite.replace(fCast, placeholder, group);
			} else {
				rewrite.replace(fSelectedNode, placeholder, group);
			}
		}
	}
	
	private static class RemoveAllCastOperation extends AbstractFixRewriteOperation {

		private final HashSet fUnnecessaryCasts;

		public RemoveAllCastOperation(HashSet unnecessaryCasts) {
			fUnnecessaryCasts= unnecessaryCasts;
		}

		/**
		 * {@inheritDoc}
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			
			TextEditGroup group= createTextEditGroup(FixMessages.UnusedCodeFix_RemoveCast_description);
			textEditGroups.add(group);
			
			while (fUnnecessaryCasts.size() > 0) {
				CastExpression castExpression= (CastExpression)fUnnecessaryCasts.iterator().next();
				fUnnecessaryCasts.remove(castExpression);
				CastExpression down= castExpression;
				while (fUnnecessaryCasts.contains(down.getExpression())) {
					down= (CastExpression)down.getExpression();
					fUnnecessaryCasts.remove(down);
				}
				
				ASTNode move= rewrite.createMoveTarget(down.getExpression());
				
				CastExpression top= castExpression;
				while (fUnnecessaryCasts.contains(top.getParent())) {
					top= (CastExpression)top.getParent();
					fUnnecessaryCasts.remove(top);
				}
				
				rewrite.replace(top, move, group);
			}
		}
	}
	
	public static UnusedCodeFix createRemoveUnusedImportFix(CompilationUnit compilationUnit, IProblemLocation problem) {
		int id= problem.getProblemId();
		if (id == IProblem.UnusedImport || id == IProblem.DuplicateImport || id == IProblem.ConflictingImport ||
		    id == IProblem.CannotImportPackage || id == IProblem.ImportNotFound) {
			
			ImportDeclaration node= getImportDeclaration(problem, compilationUnit);
			if (node != null) {
				String label= FixMessages.UnusedCodeFix_RemoveImport_description;
				RemoveImportOperation operation= new RemoveImportOperation(node);
				Map options= new Hashtable();
				options.put(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS, CleanUpConstants.TRUE);
				return new UnusedCodeFix(label, compilationUnit, new IFixRewriteOperation[] {operation}, options);
			}
		}
		return null;
	}
	
	public static UnusedCodeFix createUnusedMemberFix(CompilationUnit compilationUnit, IProblemLocation problem) {
		int id= problem.getProblemId();
		if (id == IProblem.UnusedPrivateMethod || id == IProblem.UnusedPrivateConstructor || id == IProblem.UnusedPrivateField ||
		    id == IProblem.UnusedPrivateType || id == IProblem.LocalVariableIsNeverUsed || id == IProblem.ArgumentIsNeverUsed) {
			
			SimpleName name= getUnusedName(compilationUnit, problem);
			if (name != null) {
				IBinding binding= name.resolveBinding();
				if (binding != null) {
					if (isFormalParameterInEnhancedForStatement(name))
						return null;
						
					String label= getDisplayString(name, binding);
					RemoveUnusedMemberOperation operation= new RemoveUnusedMemberOperation(new SimpleName[] {name});
					return new UnusedCodeFix(label, compilationUnit, new IFixRewriteOperation[] {operation}, getCleanUpOptions(binding));
				}
			}
		}
		return null;
	}
	
	public static IFix createRemoveUnusedCastFix(CompilationUnit compilationUnit, IProblemLocation problem) {
		if (problem.getProblemId() != IProblem.UnnecessaryCast)
			return null;
		
		ASTNode selectedNode= problem.getCoveringNode(compilationUnit);

		ASTNode curr= selectedNode;
		while (curr instanceof ParenthesizedExpression) {
			curr= ((ParenthesizedExpression) curr).getExpression();
		}

		if (!(curr instanceof CastExpression))
			return null;
		
		return new UnusedCodeFix(FixMessages.UnusedCodeFix_RemoveCast_description, compilationUnit, new IFixRewriteOperation[] {new RemoveCastOperation((CastExpression)curr, selectedNode)});
	}
	
	public static IFix createCleanUp(CompilationUnit compilationUnit, 
			boolean removeUnusedPrivateMethods, 
			boolean removeUnusedPrivateConstructors, 
			boolean removeUnusedPrivateFields, 
			boolean removeUnusedPrivateTypes, 
			boolean removeUnusedLocalVariables, 
			boolean removeUnusedImports,
			boolean removeUnusedCast) {

		IProblem[] problems= compilationUnit.getProblems();
		IProblemLocation[] locations= new IProblemLocation[problems.length];
		for (int i= 0; i < problems.length; i++) {
			locations[i]= new ProblemLocation(problems[i]);
		}
		
		return createCleanUp(compilationUnit, locations, 
				removeUnusedPrivateMethods, 
				removeUnusedPrivateConstructors, 
				removeUnusedPrivateFields, 
				removeUnusedPrivateTypes, 
				removeUnusedLocalVariables, 
				removeUnusedImports,
				removeUnusedCast);
	}
	
	public static IFix createCleanUp(CompilationUnit compilationUnit, IProblemLocation[] problems, 
			boolean removeUnusedPrivateMethods, 
			boolean removeUnusedPrivateConstructors, 
			boolean removeUnusedPrivateFields, 
			boolean removeUnusedPrivateTypes, 
			boolean removeUnusedLocalVariables, 
			boolean removeUnusedImports,
			boolean removeUnusedCast) {

		List/*<IFixRewriteOperation>*/ result= new ArrayList();
		Hashtable/*<ASTNode, List>*/ variableDeclarations= new Hashtable();
		HashSet/*/CastExpression>*/ unnecessaryCasts= new HashSet();
		for (int i= 0; i < problems.length; i++) {
			IProblemLocation problem= problems[i];
			int id= problem.getProblemId();
			
			if (removeUnusedImports && (id == IProblem.UnusedImport || id == IProblem.DuplicateImport || id == IProblem.ConflictingImport ||
				    id == IProblem.CannotImportPackage || id == IProblem.ImportNotFound)) 
			{
				ImportDeclaration node= UnusedCodeFix.getImportDeclaration(problem, compilationUnit);
				if (node != null) {
					result.add(new RemoveImportOperation(node));
				}
			}

			if ((removeUnusedPrivateMethods && id == IProblem.UnusedPrivateMethod) || (removeUnusedPrivateConstructors && id == IProblem.UnusedPrivateConstructor) ||
			    (removeUnusedPrivateTypes && id == IProblem.UnusedPrivateType)) {
				
				SimpleName name= getUnusedName(compilationUnit, problem);
				if (name != null) {
					IBinding binding= name.resolveBinding();
					if (binding != null) {
						result.add(new RemoveUnusedMemberOperation(new SimpleName[] {name}));
					}
				}
			}
			
			if ((removeUnusedLocalVariables && id == IProblem.LocalVariableIsNeverUsed) ||  (removeUnusedPrivateFields && id == IProblem.UnusedPrivateField)) {
				SimpleName name= getUnusedName(compilationUnit, problem);
				if (name != null) {
					IBinding binding= name.resolveBinding();
					if (binding != null && !isFormalParameterInEnhancedForStatement(name) && isSideEffectFree(name, compilationUnit)) {
						VariableDeclarationFragment parent= (VariableDeclarationFragment)ASTNodes.getParent(name, VariableDeclarationFragment.class);
						if (parent != null) {
							ASTNode varDecl= parent.getParent();
							if (!variableDeclarations.containsKey(varDecl)) {
								variableDeclarations.put(varDecl, new ArrayList());
							}
							((List)variableDeclarations.get(varDecl)).add(name);
						} else {
							result.add(new RemoveUnusedMemberOperation(new SimpleName[] {name}));
						}
					}
				}
			}
			
			if (removeUnusedCast && id == IProblem.UnnecessaryCast) {
				ASTNode selectedNode= problem.getCoveringNode(compilationUnit);

				ASTNode curr= selectedNode;
				while (curr instanceof ParenthesizedExpression) {
					curr= ((ParenthesizedExpression) curr).getExpression();
				}

				if (curr instanceof CastExpression) {
					unnecessaryCasts.add(curr);
				}
			}
		}
		for (Iterator iter= variableDeclarations.keySet().iterator(); iter.hasNext();) {
			ASTNode node= (ASTNode)iter.next();
			List names= (List)variableDeclarations.get(node);
			result.add(new RemoveUnusedMemberOperation((SimpleName[])names.toArray(new SimpleName[names.size()])));
		}
		if (unnecessaryCasts.size() > 0)
			result.add(new RemoveAllCastOperation(unnecessaryCasts));
		
		if (result.size() == 0)
			return null;
		
		return new UnusedCodeFix(FixMessages.UnusedCodeFix_change_name, compilationUnit, (IFixRewriteOperation[])result.toArray(new IFixRewriteOperation[result.size()]));
	}
	
	private static boolean isFormalParameterInEnhancedForStatement(SimpleName name) {
		return name.getParent() instanceof SingleVariableDeclaration && name.getParent().getLocationInParent() == EnhancedForStatement.PARAMETER_PROPERTY;
	}
	
	private static boolean isSideEffectFree(SimpleName simpleName, CompilationUnit completeRoot) {
		SimpleName nameNode= (SimpleName) NodeFinder.perform(completeRoot, simpleName.getStartPosition(), simpleName.getLength());
		SimpleName[] references= LinkedNodeFinder.findByBinding(completeRoot, nameNode.resolveBinding());
		for (int i= 0; i < references.length; i++) {
			if (hasSideEffect(references[i]))
				return false;
		}
		return true;
	}

	private static boolean hasSideEffect(SimpleName reference) {
		ASTNode parent= reference.getParent();
		while (parent instanceof QualifiedName) {
			parent= parent.getParent();
		}
		if (parent instanceof FieldAccess) {
			parent= parent.getParent();
		}

		ASTNode node= null;
		int nameParentType= parent.getNodeType();
		if (nameParentType == ASTNode.ASSIGNMENT) {
			Assignment assignment= (Assignment) parent;
			node= assignment.getRightHandSide();
		} else if (nameParentType == ASTNode.SINGLE_VARIABLE_DECLARATION) {
			SingleVariableDeclaration decl= (SingleVariableDeclaration)parent;
			node= decl.getInitializer();
			if (node == null)
				return false;
		} else if (nameParentType == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
			node= parent;
		} else {
			return false;
		}		

		ArrayList sideEffects= new ArrayList();
		node.accept(new SideEffectFinder(sideEffects));
		return sideEffects.size() > 0;
	}

	private static SimpleName getUnusedName(CompilationUnit compilationUnit, IProblemLocation problem) {
		ASTNode selectedNode= problem.getCoveringNode(compilationUnit);

		if (selectedNode instanceof MethodDeclaration) {
			return ((MethodDeclaration) selectedNode).getName();
		} else if (selectedNode instanceof SimpleName) {
			return (SimpleName) selectedNode;
		}
		
		return null;
	}
	
	private static String getDisplayString(SimpleName simpleName, IBinding binding) {
		String name= simpleName.getIdentifier();
		switch (binding.getKind()) {
			case IBinding.TYPE:
				return Messages.format(FixMessages.UnusedCodeFix_RemoveType_description, name);
			case IBinding.METHOD:
				if (((IMethodBinding) binding).isConstructor()) {
					return Messages.format(FixMessages.UnusedCodeFix_RemoveConstructor_description, name);
				} else {
					return Messages.format(FixMessages.UnusedCodeFix_RemoveMethod_description, name);
				}
			case IBinding.VARIABLE:
				if (((IVariableBinding) binding).isField()) {
					return Messages.format(FixMessages.UnusedCodeFix_RemoveFieldOrLocal_description, name);
				} else {
					return Messages.format(FixMessages.UnusedCodeFix_RemoveFieldOrLocal_description, name);
				}
			default:
				return ""; //$NON-NLS-1$
		}
	}
	
	private static Map getCleanUpOptions(IBinding binding) {
		Map result= new Hashtable();
		
		result.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS, CleanUpConstants.TRUE);		
		switch (binding.getKind()) {
			case IBinding.TYPE:
				result.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES, CleanUpConstants.TRUE);
				break;
			case IBinding.METHOD:
				if (((IMethodBinding) binding).isConstructor()) {
					result.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS, CleanUpConstants.TRUE);
				} else {
					result.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_METHODS, CleanUpConstants.TRUE);
				}
				break;
			case IBinding.VARIABLE:
				result.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS, CleanUpConstants.TRUE);
				result.put(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES, CleanUpConstants.TRUE);
				break;
		}

		return result;
	}
	
	private static ImportDeclaration getImportDeclaration(IProblemLocation problem, CompilationUnit compilationUnit) {
		ASTNode selectedNode= problem.getCoveringNode(compilationUnit);
		if (selectedNode != null) {
			ASTNode node= ASTNodes.getParent(selectedNode, ASTNode.IMPORT_DECLARATION);
			if (node instanceof ImportDeclaration) {
				return (ImportDeclaration)node;
			}
		}
		return null;
	}
	
	private final Map fCleanUpOptions;
	
	private UnusedCodeFix(String name, CompilationUnit compilationUnit, IFixRewriteOperation[] fixRewriteOperations) {
		this(name, compilationUnit, fixRewriteOperations, null);
	}
	
	private UnusedCodeFix(String name, CompilationUnit compilationUnit, IFixRewriteOperation[] fixRewriteOperations, Map options) {
		super(name, compilationUnit, fixRewriteOperations);
		if (options == null) {
			fCleanUpOptions= new Hashtable();			
		} else {
			fCleanUpOptions= options;
		}
	}

	public UnusedCodeCleanUp getCleanUp() {
		return new UnusedCodeCleanUp(fCleanUpOptions);
	}

}
