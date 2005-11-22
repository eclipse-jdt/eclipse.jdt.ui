/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
import java.util.Iterator;
import java.util.List;

import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
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
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.fix.UnusedCodeCleanUp;
import org.eclipse.jdt.internal.ui.text.correction.JavadocTagsSubProcessor;

/**
 * Fix which removes unused code.
 * Supported:
 * 		Remove unused import
 */
public class UnusedCodeFix extends AbstractFix {
	
	private static class SideEffectFinder extends ASTVisitor {

		private ArrayList fSideEffectNodes;

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

	private static UnusedCodeCleanUp fCleanUp;
	private final ImportDeclaration[] fImports;
	private final SimpleName[] fUnused;
	private final CompilationUnit fAstRoot;

	public static UnusedCodeFix createFix(CompilationUnit compilationUnit, IProblemLocation problem, 
									boolean removeUnusedImports, 
									boolean removeUnusedPrivateMethod, 
									boolean removeUnusedPrivateConstructor,
									boolean removeUnusedPrivateField,
									boolean removeUnusedPrivateType,
									boolean removeUnusedLocalVariable,
									boolean removeUnusedArgument) {
		
		ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
		if (removeUnusedImports && (problem.getProblemId() == IProblem.UnusedImport || 
									problem.getProblemId() == IProblem.DuplicateImport ||
									problem.getProblemId() == IProblem.ConflictingImport ||
									problem.getProblemId() == IProblem.CannotImportPackage ||
									problem.getProblemId() == IProblem.ImportNotFound
		)
									
		) {
			ImportDeclaration node= getImportDeclaration(problem, compilationUnit);
			if (node != null) {
				return new UnusedCodeFix(FixMessages.UnusedCodeFix_RemoveImport_description, cu, new ImportDeclaration[] {node}, null, null);
			}
		}
		if (
				(removeUnusedPrivateMethod && problem.getProblemId() == IProblem.UnusedPrivateMethod) ||
				(removeUnusedPrivateConstructor && problem.getProblemId() == IProblem.UnusedPrivateConstructor) ||
				(removeUnusedPrivateField && problem.getProblemId() == IProblem.UnusedPrivateField) ||
				(removeUnusedPrivateType && problem.getProblemId() == IProblem.UnusedPrivateType) ||
				(removeUnusedLocalVariable && problem.getProblemId() == IProblem.LocalVariableIsNeverUsed) ||
				(removeUnusedArgument && problem.getProblemId() == IProblem.ArgumentIsNeverUsed)
				) {
			SimpleName name= getUnusedName(compilationUnit, problem);
			if (name != null) {
				IBinding binding= name.resolveBinding();
				if (binding != null) {
					return new UnusedCodeFix(getDisplayString(name, binding), cu, null, new SimpleName[] {name}, compilationUnit);
				}
			}
		}
		return null;
	}
	
	public static SimpleName getUnusedName(CompilationUnit compilationUnit, IProblemLocation problem) {
		ASTNode selectedNode= problem.getCoveringNode(compilationUnit);

		SimpleName name= null;
		if (selectedNode instanceof MethodDeclaration) {
			name= ((MethodDeclaration) selectedNode).getName();
		} else if (selectedNode instanceof SimpleName) {
			name= (SimpleName) selectedNode;
		}
		if (name != null) {
			return name;
		}
		return null;
	}
	
	private static String getDisplayString(SimpleName simpleName, IBinding binding) {
		String name= simpleName.getIdentifier();
		switch (binding.getKind()) {
			case IBinding.TYPE:
				fCleanUp= new UnusedCodeCleanUp(false, false, false, false, true, false);
				return Messages.format(FixMessages.UnusedCodeFix_RemoveType_description, name);
			case IBinding.METHOD:
				if (((IMethodBinding) binding).isConstructor()) {
					fCleanUp= new UnusedCodeCleanUp(false, false, true, false, false, false);
					return Messages.format(FixMessages.UnusedCodeFix_RemoveConstructor_description, name);
				} else {
					fCleanUp= new UnusedCodeCleanUp(false, true, false, false, false, false);
					return Messages.format(FixMessages.UnusedCodeFix_RemoveMethod_description, name);
				}
			case IBinding.VARIABLE:
				fCleanUp= null;
				if (((IVariableBinding) binding).isField()) {
					return Messages.format(FixMessages.UnusedCodeFix_RemoveFieldOrLocal_description, name);
				} else {
					return Messages.format(FixMessages.UnusedCodeFix_RemoveFieldOrLocal_description, name);
				}
			default:
				return ""; //$NON-NLS-1$
		}
	}
	
	public static ImportDeclaration getImportDeclaration(IProblemLocation problem, CompilationUnit compilationUnit) {
		ASTNode selectedNode= problem.getCoveringNode(compilationUnit);
		if (selectedNode != null) {
			ASTNode node= ASTNodes.getParent(selectedNode, ASTNode.IMPORT_DECLARATION);
			if (node instanceof ImportDeclaration) {
				return (ImportDeclaration)node;
			}
		}
		return null;
	}

	public UnusedCodeFix(String name, ICompilationUnit compilationUnit, ImportDeclaration[] imports, SimpleName[] unused, CompilationUnit astRoot) {
		super(name, compilationUnit);
		fImports= imports;
		fUnused= unused;
		fAstRoot= astRoot;
	}

	public TextChange createChange() throws CoreException {
		CompilationUnitChange result= null;
		List/*<TextEditGroup>*/ groups= new ArrayList();
		ASTRewrite rewrite= null;
		if (fImports != null && fImports.length > 0) {
			rewrite= ASTRewrite.create(fImports[0].getAST());
			for (int i= 0; i < fImports.length; i++) {
				ImportDeclaration node= fImports[i];
				TextEditGroup group= new TextEditGroup(FixMessages.UnusedCodeFix_RemoveImport_description + " " + node.getName()); //$NON-NLS-1$
				rewrite.remove(node, group);
				groups.add(group);
			}
		}
		if (fUnused != null && fUnused.length > 0) {
			if (rewrite == null)
				rewrite= ASTRewrite.create(fUnused[0].getAST());
			
			for (int i= 0; i < fUnused.length; i++) {
				SimpleName name= fUnused[i];
				removeUnusedName(rewrite, name, fAstRoot, groups);
			}
		}

		if (rewrite == null)
			return null;
			
		TextEdit edit= applyEdits(getCompilationUnit(), rewrite, null);
			
		result= new CompilationUnitChange("", getCompilationUnit()); //$NON-NLS-1$
		result.setEdit(edit);
			
		for (Iterator iter= groups.iterator(); iter.hasNext();) {
			TextEditGroup group= (TextEditGroup)iter.next();
			result.addTextEditGroup(group);
		}
		
		return result;
	}
	
	private void removeUnusedName(ASTRewrite rewrite, SimpleName simpleName, CompilationUnit completeRoot, List groups) {
		IBinding binding= simpleName.resolveBinding();
		CompilationUnit root= (CompilationUnit) simpleName.getRoot();
		String displayString= getDisplayString(simpleName, binding);
		TextEditGroup group= new TextEditGroup(displayString);
		groups.add(group);
		if (binding.getKind() == IBinding.METHOD) {
			IMethodBinding decl= ((IMethodBinding) binding).getMethodDeclaration();
			ASTNode declaration= root.findDeclaringNode(decl);
			rewrite.remove(declaration, group);
		} else if (binding.getKind() == IBinding.TYPE) {
			ITypeBinding decl= ((ITypeBinding) binding).getTypeDeclaration();
			ASTNode declaration= root.findDeclaringNode(decl);
			rewrite.remove(declaration, group);
		} else { // variable
			SimpleName nameNode= (SimpleName) NodeFinder.perform(completeRoot, simpleName.getStartPosition(), simpleName.getLength());
			SimpleName[] references= LinkedNodeFinder.findByBinding(completeRoot, nameNode.resolveBinding());
			for (int i= 0; i < references.length; i++) {
				removeVariableReferences(rewrite, references[i], group);
			}

			IVariableBinding bindingDecl= Bindings.getVariableDeclaration((IVariableBinding) nameNode.resolveBinding());
			ASTNode declaringNode= completeRoot.findDeclaringNode(bindingDecl);
			if (declaringNode instanceof SingleVariableDeclaration) {
				removeParamTag(rewrite, (SingleVariableDeclaration) declaringNode, group);
			}
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
			if (fragments.size() == 1) {
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

	public UnusedCodeCleanUp getCleanUp() {
		return fCleanUp;
	}
}
