/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
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
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.fix.UnusedCodeCleanUp;
import org.eclipse.jdt.internal.ui.text.correction.JavadocTagsSubProcessor;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;
import org.eclipse.jdt.internal.ui.viewsupport.BasicElementLabels;

/**
 * Fix which removes unused code.
 */
public class UnusedCodeFix extends CompilationUnitRewriteOperationsFix {

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

	private static class RemoveImportOperation extends CompilationUnitRewriteOperation {

		private final ImportDeclaration fImportDeclaration;

		public RemoveImportOperation(ImportDeclaration importDeclaration) {
			fImportDeclaration= importDeclaration;
		}

		/**
		 * {@inheritDoc}
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModel model) throws CoreException {
			ImportDeclaration node= fImportDeclaration;
			TextEditGroup group= createTextEditGroup(FixMessages.UnusedCodeFix_RemoveImport_description, cuRewrite);
			cuRewrite.getASTRewrite().remove(node, group);
		}

	}

	private static class RemoveUnusedMemberOperation extends CompilationUnitRewriteOperation {

		private final SimpleName[] fUnusedNames;
		private boolean fForceRemove;
		private int fRemovedAssignmentsCount;
		private int fAlteredAssignmentsCount;

		public RemoveUnusedMemberOperation(SimpleName[] unusedNames, boolean removeAllAsignements) {
			fUnusedNames= unusedNames;
			fForceRemove= removeAllAsignements;
		}

		/**
		 * {@inheritDoc}
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModel model) throws CoreException {
			for (int i= 0; i < fUnusedNames.length; i++) {
				removeUnusedName(cuRewrite, fUnusedNames[i]);
			}
		}

		private void removeUnusedName(CompilationUnitRewrite cuRewrite, SimpleName simpleName) {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			CompilationUnit completeRoot= cuRewrite.getRoot();

			IBinding binding= simpleName.resolveBinding();
			CompilationUnit root= (CompilationUnit) simpleName.getRoot();
			String displayString= getDisplayString(binding);
			TextEditGroup group= createTextEditGroup(displayString, cuRewrite);
			if (binding.getKind() == IBinding.METHOD) {
				IMethodBinding decl= ((IMethodBinding) binding).getMethodDeclaration();
				ASTNode declaration= root.findDeclaringNode(decl);
				rewrite.remove(declaration, group);
			} else if (binding.getKind() == IBinding.TYPE) {
				ITypeBinding decl= ((ITypeBinding) binding).getTypeDeclaration();
				ASTNode declaration= root.findDeclaringNode(decl);
				if (declaration.getParent() instanceof TypeDeclarationStatement) {
					declaration= declaration.getParent();
				}
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
		 * @param rewrite the AST rewriter to use
		 * @param reference a reference to the variable to remove
		 * @param group the text edit group to use
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
				Expression initializer = frag.getInitializer();
				if (initializer instanceof CastExpression) {
					initializer= ((CastExpression) initializer).getExpression();
				}
				boolean sideEffectInitializer = initializer instanceof MethodInvocation || initializer instanceof ClassInstanceCreation;
				if (fragments.size() == fUnusedNames.length) {
					if (fForceRemove) {
						rewrite.remove(varDecl, group);
						return;
					}
					if (parent.getParent() instanceof FieldDeclaration) {
						rewrite.remove(varDecl, group);
						return;
					}
					if (sideEffectInitializer){
						Expression movedInit = (Expression) rewrite.createMoveTarget(initializer);
						ExpressionStatement wrapped = rewrite.getAST().newExpressionStatement(movedInit);
						rewrite.replace(varDecl, wrapped, group);
					} else {
						rewrite.remove(varDecl, group);
					}
				} else {
					if (fForceRemove) {
						rewrite.remove(frag, group);
						return;
					}
					//multiple declarations in one line
					ASTNode declaration = parent.getParent();
					if (declaration instanceof FieldDeclaration) {
						rewrite.remove(frag, group);
						return;
					}
					if (declaration instanceof VariableDeclarationStatement) {
						ASTNode lst = declaration.getParent();
						ListRewrite listRewrite= null;
						if (lst instanceof Block) {
							listRewrite= rewrite.getListRewrite(lst, Block.STATEMENTS_PROPERTY);
						} else if (lst instanceof SwitchStatement) {
							listRewrite= rewrite.getListRewrite(lst, SwitchStatement.STATEMENTS_PROPERTY);
						} else {
							Assert.isTrue(false);
						}
						splitUpDeclarations(rewrite, group, frag, listRewrite, (VariableDeclarationStatement) declaration);
						rewrite.remove(frag, group);
						return;
					}
					if (declaration instanceof VariableDeclarationExpression) {
						//keep constructors and method invocations
						if (!sideEffectInitializer){
							rewrite.remove(frag, group);
						}
					}
				}
			}
		}

		private void splitUpDeclarations(ASTRewrite rewrite, TextEditGroup group, VariableDeclarationFragment frag, ListRewrite statementRewrite, VariableDeclarationStatement originalStatement) {
			Expression initializer = frag.getInitializer();
			//keep constructors and method invocations
			if (initializer instanceof MethodInvocation || initializer instanceof ClassInstanceCreation){
				Expression movedInitializer= (Expression) rewrite.createMoveTarget(initializer);

				ExpressionStatement newInitializer= rewrite.getAST().newExpressionStatement( movedInitializer);
				statementRewrite.insertAfter(newInitializer, originalStatement, group);

				VariableDeclarationStatement newDeclaration= null;
				List fragments= originalStatement.fragments();
				int fragIndex= fragments.indexOf(frag);
				ListIterator fragmentIterator= fragments.listIterator(fragIndex+1);
				while (fragmentIterator.hasNext()) {
					VariableDeclarationFragment currentFragment= (VariableDeclarationFragment) fragmentIterator.next();
					VariableDeclarationFragment movedFragment= (VariableDeclarationFragment) rewrite.createMoveTarget(currentFragment);
					if (newDeclaration == null) {
						newDeclaration= rewrite.getAST().newVariableDeclarationStatement(movedFragment);
						Type copiedType= (Type) rewrite.createCopyTarget(originalStatement.getType());
						newDeclaration.setType(copiedType);
					} else
						newDeclaration.fragments().add(movedFragment);
				}
				if (newDeclaration != null){
					statementRewrite.insertAfter(newDeclaration, newInitializer, group);
				}
				if (originalStatement.fragments().size() == newDeclaration.fragments().size() + 1){
					rewrite.remove(originalStatement, group);
				}
			}
		}

		private void removeVariableWithInitializer(ASTRewrite rewrite, ASTNode initializerNode, ASTNode statementNode, TextEditGroup group) {
			boolean performRemove= fForceRemove;
			if (!performRemove) {
				ArrayList sideEffectNodes= new ArrayList();
				initializerNode.accept(new SideEffectFinder(sideEffectNodes));
				performRemove= sideEffectNodes.isEmpty();
			}
			if (performRemove) {
				if (ASTNodes.isControlStatementBody(statementNode.getLocationInParent())) {
					rewrite.replace(statementNode, rewrite.getAST().newBlock(), group);
				} else {
					rewrite.remove(statementNode, group);
				}
				fRemovedAssignmentsCount++;
			} else {
				ASTNode initNode = rewrite.createMoveTarget(initializerNode);
				ExpressionStatement statement = rewrite.getAST().newExpressionStatement((Expression) initNode);
				rewrite.replace(statementNode, statement, null);
				fAlteredAssignmentsCount++;
			}
		}

		public String getAdditionalInfo() {
			StringBuffer sb=new StringBuffer();
			if (fRemovedAssignmentsCount>0){
				sb.append(Messages.format(FixMessages.UnusedCodeFix_RemoveFieldOrLocal_RemovedAssignments_preview,String.valueOf(fRemovedAssignmentsCount)));
			}
			if (fAlteredAssignmentsCount>0){
				sb.append(Messages.format(FixMessages.UnusedCodeFix_RemoveFieldOrLocal_AlteredAssignments_preview,String.valueOf(fAlteredAssignmentsCount)));
			}
			if (sb.length()>0) {
				return sb.toString();
			} else
				return null;
		}
	}

	private static class RemoveCastOperation extends CompilationUnitRewriteOperation {

		private final CastExpression fCast;
		private final ASTNode fSelectedNode;

		public RemoveCastOperation(CastExpression cast, ASTNode selectedNode) {
			fCast= cast;
			fSelectedNode= selectedNode;
		}

		/**
		 * {@inheritDoc}
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModel model) throws CoreException {

			TextEditGroup group= createTextEditGroup(FixMessages.UnusedCodeFix_RemoveCast_description, cuRewrite);

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

	private static class RemoveAllCastOperation extends CompilationUnitRewriteOperation {

		private final HashSet fUnnecessaryCasts;

		public RemoveAllCastOperation(HashSet unnecessaryCasts) {
			fUnnecessaryCasts= unnecessaryCasts;
		}

		/**
		 * {@inheritDoc}
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModel model) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();

			TextEditGroup group= createTextEditGroup(FixMessages.UnusedCodeFix_RemoveCast_description, cuRewrite);

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
		if (isUnusedImport(problem)) {
			ImportDeclaration node= getImportDeclaration(problem, compilationUnit);
			if (node != null) {
				String label= FixMessages.UnusedCodeFix_RemoveImport_description;
				RemoveImportOperation operation= new RemoveImportOperation(node);
				Map options= new Hashtable();
				options.put(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS, CleanUpOptions.TRUE);
				return new UnusedCodeFix(label, compilationUnit, new CompilationUnitRewriteOperation[] {operation}, options);
			}
		}
		return null;
	}

	public static boolean isUnusedImport(IProblemLocation problem) {
		int id= problem.getProblemId();
		return id == IProblem.UnusedImport || id == IProblem.DuplicateImport || id == IProblem.ConflictingImport || id == IProblem.CannotImportPackage || id == IProblem.ImportNotFound;
	}

	public static UnusedCodeFix createUnusedMemberFix(CompilationUnit compilationUnit, IProblemLocation problem, boolean removeAllAssignements) {
		if (isUnusedMember(problem)) {
			SimpleName name= getUnusedName(compilationUnit, problem);
			if (name != null) {
				IBinding binding= name.resolveBinding();
				if (binding != null) {
					if (isFormalParameterInEnhancedForStatement(name))
						return null;

					String label= getDisplayString(name, binding, removeAllAssignements);
					RemoveUnusedMemberOperation operation= new RemoveUnusedMemberOperation(new SimpleName[] { name }, removeAllAssignements);
					return new UnusedCodeFix(label, compilationUnit, new CompilationUnitRewriteOperation[] { operation }, getCleanUpOptions(binding, removeAllAssignements));
				}
			}
		}
		return null;
	}

	public static boolean isUnusedMember(IProblemLocation problem) {
		int id= problem.getProblemId();
		return id == IProblem.UnusedPrivateMethod || id == IProblem.UnusedPrivateConstructor || id == IProblem.UnusedPrivateField || id == IProblem.UnusedPrivateType
				|| id == IProblem.LocalVariableIsNeverUsed || id == IProblem.ArgumentIsNeverUsed;
	}

	public static UnusedCodeFix createRemoveUnusedCastFix(CompilationUnit compilationUnit, IProblemLocation problem) {
		if (problem.getProblemId() != IProblem.UnnecessaryCast)
			return null;

		ASTNode selectedNode= problem.getCoveringNode(compilationUnit);

		ASTNode curr= selectedNode;
		while (curr instanceof ParenthesizedExpression) {
			curr= ((ParenthesizedExpression) curr).getExpression();
		}

		if (!(curr instanceof CastExpression))
			return null;

		return new UnusedCodeFix(FixMessages.UnusedCodeFix_RemoveCast_description, compilationUnit, new CompilationUnitRewriteOperation[] {new RemoveCastOperation((CastExpression)curr, selectedNode)});
	}

	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit,
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

	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit, IProblemLocation[] problems,
			boolean removeUnusedPrivateMethods,
			boolean removeUnusedPrivateConstructors,
			boolean removeUnusedPrivateFields,
			boolean removeUnusedPrivateTypes,
			boolean removeUnusedLocalVariables,
			boolean removeUnusedImports,
			boolean removeUnusedCast) {

		List/*<CompilationUnitRewriteOperation>*/ result= new ArrayList();
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
						result.add(new RemoveUnusedMemberOperation(new SimpleName[] {name}, false));
					}
				}
			}

			if ((removeUnusedLocalVariables && id == IProblem.LocalVariableIsNeverUsed) ||  (removeUnusedPrivateFields && id == IProblem.UnusedPrivateField)) {
				SimpleName name= getUnusedName(compilationUnit, problem);
				if (name != null) {
					IBinding binding= name.resolveBinding();
					if (binding instanceof IVariableBinding && !isFormalParameterInEnhancedForStatement(name) && (!((IVariableBinding) binding).isField() || isSideEffectFree(name, compilationUnit))) {
						VariableDeclarationFragment parent= (VariableDeclarationFragment) ASTNodes.getParent(name, VariableDeclarationFragment.class);
						if (parent != null) {
							ASTNode varDecl= parent.getParent();
							if (!variableDeclarations.containsKey(varDecl)) {
								variableDeclarations.put(varDecl, new ArrayList());
							}
							((List)variableDeclarations.get(varDecl)).add(name);
						} else {
							result.add(new RemoveUnusedMemberOperation(new SimpleName[] {name}, false));
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
			result.add(new RemoveUnusedMemberOperation((SimpleName[])names.toArray(new SimpleName[names.size()]), false));
		}
		if (unnecessaryCasts.size() > 0)
			result.add(new RemoveAllCastOperation(unnecessaryCasts));

		if (result.size() == 0)
			return null;

		return new UnusedCodeFix(FixMessages.UnusedCodeFix_change_name, compilationUnit, (CompilationUnitRewriteOperation[])result.toArray(new CompilationUnitRewriteOperation[result.size()]));
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

	private static String getDisplayString(SimpleName simpleName, IBinding binding, boolean removeAllAssignements) {
		String name= BasicElementLabels.getJavaElementName(simpleName.getIdentifier());
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
				if (removeAllAssignements) {
					return Messages.format(FixMessages.UnusedCodeFix_RemoveFieldOrLocalWithInitializer_description, name);
				} else {
					return Messages.format(FixMessages.UnusedCodeFix_RemoveFieldOrLocal_description, name);
				}
			default:
				return ""; //$NON-NLS-1$
		}
	}

	private static Map getCleanUpOptions(IBinding binding, boolean removeAll) {
		Map result= new Hashtable();

		result.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_MEMBERS, CleanUpOptions.TRUE);
		switch (binding.getKind()) {
			case IBinding.TYPE:
				result.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_TYPES, CleanUpOptions.TRUE);
				break;
			case IBinding.METHOD:
				if (((IMethodBinding) binding).isConstructor()) {
					result.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_CONSTRUCTORS, CleanUpOptions.TRUE);
				} else {
					result.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_METHODS, CleanUpOptions.TRUE);
				}
				break;
			case IBinding.VARIABLE:
				if (removeAll)
					return null;

				result.put(CleanUpConstants.REMOVE_UNUSED_CODE_PRIVATE_FELDS, CleanUpOptions.TRUE);
				result.put(CleanUpConstants.REMOVE_UNUSED_CODE_LOCAL_VARIABLES, CleanUpOptions.TRUE);
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

	private UnusedCodeFix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation[] fixRewriteOperations) {
		this(name, compilationUnit, fixRewriteOperations, null);
	}

	private UnusedCodeFix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation[] fixRewriteOperations, Map options) {
		super(name, compilationUnit, fixRewriteOperations);
		fCleanUpOptions= options;
	}

	public UnusedCodeCleanUp getCleanUp() {
		if (fCleanUpOptions == null)
			return null;

		return new UnusedCodeCleanUp(fCleanUpOptions);
	}

}
