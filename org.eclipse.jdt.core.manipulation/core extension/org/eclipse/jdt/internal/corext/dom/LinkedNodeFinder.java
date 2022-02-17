/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.ModuleDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import org.eclipse.jdt.internal.ui.util.ASTHelper;


/**
 * Find all nodes connected to a given binding or node. e.g. Declaration of a field and all references.
 * For types this includes also the constructor declaration, for methods also overridden methods
 * or methods overriding (if existing in the same AST), for constructors also the type and all other constructors.
  */

public class LinkedNodeFinder  {

	private LinkedNodeFinder() {
	}


	public static SimpleName getAssociatedRecordComponentNode(SimpleName simpleName) {
		SimpleName rcName= null;
		if (simpleName == null) {
			return null;
		}
		if (!ASTHelper.isRecordDeclarationNodeSupportedInAST(simpleName.getAST())) {
			return null;
		}
		IBinding binding= simpleName.resolveBinding();
		IVariableBinding rcBinding= null;
		ITypeBinding recordTypeBinding= null;
		ITypeBinding tBinding= null;
		String recCompName= null;
		if (binding instanceof IVariableBinding) {
			IVariableBinding vbinding= (IVariableBinding) binding;
			IVariableBinding bBinding= vbinding.getVariableDeclaration();
			if (bBinding == null) {
				return null;
			}
			if (!bBinding.isField()) {
				if (bBinding.isParameter()) {
					IMethodBinding mBinding= bBinding.getDeclaringMethod();
					if (mBinding != null
							&& (mBinding.isCompactConstructor()
									|| mBinding.isCanonicalConstructor())) {
						ITypeBinding typeBinding= mBinding.getDeclaringClass();
						if (typeBinding != null
								&& typeBinding.isRecord()) {
							recordTypeBinding= typeBinding;
							recCompName= bBinding.getName();
							tBinding= bBinding.getType();
						}
					}
				}
				if (recordTypeBinding == null) {
					return null;
				}
			}
			if (recordTypeBinding == null) {
				ITypeBinding cBinding= vbinding.getDeclaringClass();
				if (cBinding == null) {
					return null;
				}
				ITypeBinding typeBinding= cBinding.getTypeDeclaration();
				if (typeBinding== null || !typeBinding.isRecord()) {
					return null;
				}
				int modifiers= bBinding.getModifiers();
				if (Flags.isFinal(modifiers)
						&& !Flags.isStatic(modifiers)) {
					rcBinding= bBinding;
				}
			}
		} else if (binding instanceof IMethodBinding) {
			IMethodBinding mBinding = (IMethodBinding)binding;
			ITypeBinding cBinding= mBinding.getDeclaringClass();
			if (cBinding == null) {
				return null;
			}
			ITypeBinding typeBinding= cBinding.getTypeDeclaration();
			IMethodBinding bBinding= mBinding.getMethodDeclaration();
			if (bBinding == null ||
					typeBinding == null ||
					bBinding.getParameterTypes().length != 0 ||
					!typeBinding.isRecord()) {
				return null;
			}
			if (typeBinding.isRecord()) {
				recordTypeBinding= typeBinding;
				recCompName= simpleName.getIdentifier();
				tBinding= bBinding.getReturnType();
			}

		} else {
			return null;
		}
		if (recCompName != null
				&& recordTypeBinding != null
				&& tBinding != null) {
			IVariableBinding varBindings[]= recordTypeBinding.getDeclaredFields();
			if (varBindings != null) {
				for (IVariableBinding varBinding : varBindings) {
					String name= varBinding.getName();
					if (simpleName.getIdentifier().equals(name)) {
						int modifiers= varBinding.getModifiers();
						if (tBinding.isEqualTo(varBinding.getType())
								&& Flags.isFinal(modifiers)
								&& !Flags.isStatic(modifiers)) {
							rcBinding= varBinding;
							break;
						}
					}
				}
			}
		}
		RecordDeclaration recordDeclaration= null;
		if (rcBinding != null) {
			ITypeBinding cBinding= rcBinding.getDeclaringClass();
			if (cBinding != null) {
				ITypeBinding typeBinding= cBinding.getTypeDeclaration();
				if (typeBinding != null) {
					ASTNode root= simpleName.getRoot();
					if (root instanceof CompilationUnit) {
						CompilationUnit astRoot= (CompilationUnit) root;
						ASTNode foundNode= astRoot.findDeclaringNode(typeBinding);
						if (foundNode instanceof RecordDeclaration) {
							recordDeclaration= (RecordDeclaration) foundNode;
						}
					}
				}
			}
		}
		if (recordDeclaration != null) {
			List<ASTNode> ff=recordDeclaration.recordComponents();
			if (ff != null) {
				for (ASTNode nd : ff) {
					if (nd instanceof SingleVariableDeclaration) {
						SimpleName name= ((SingleVariableDeclaration) nd).getName();
						if (simpleName.getIdentifier().equals(name.getIdentifier())) {
							rcName= name;
							break;
						}
					}
				}
			}
		}
		return rcName;
	}



	/**
	 * Find all nodes connected to the given binding. e.g. Declaration of a field and all references.
	 * For types this includes also the constructor declaration, for methods also overridden methods
	 * or methods overriding (if existing in the same AST)
	 * @param root The root of the AST tree to search
	 * @param binding The binding of the searched nodes
	 * @return Return
	 */
	public static SimpleName[] findByBinding(ASTNode root, IBinding binding) {
		ArrayList<SimpleName> res= new ArrayList<>();
		BindingFinder nodeFinder= new BindingFinder(binding, res);
		root.accept(nodeFinder);
		return res.toArray(new SimpleName[res.size()]);
	}

	/**
	 * Find all nodes connected to the given name node. If the node has a binding then all nodes connected
	 * to this binding are returned. If the node has no binding, then all nodes that also miss a binding and have
	 * the same name are returned.
	 * @param root The root of the AST tree to search
	 * @param name The node to find linked nodes for
	 * @return Return
	 */
	public static SimpleName[] findByNode(ASTNode root, SimpleName name) {
		IBinding binding = name.resolveBinding();
		if (binding != null) {
			SimpleName recNode= getAssociatedRecordComponentNode(name);
			if (recNode != null) {
				IBinding recBinding= recNode.resolveBinding();
				return findByBinding(root, recBinding);
			}
			return findByBinding(root, binding);
		}
		SimpleName[] names= findByProblems(root, name);
		if (names != null) {
			return names;
		}
		int parentKind= name.getParent().getNodeType();
		if (parentKind == ASTNode.LABELED_STATEMENT || parentKind == ASTNode.BREAK_STATEMENT || parentKind == ASTNode.CONTINUE_STATEMENT) {
			ArrayList<SimpleName> res= new ArrayList<>();
			LabelFinder nodeFinder= new LabelFinder(name, res);
			root.accept(nodeFinder);
			return res.toArray(new SimpleName[res.size()]);
		}
		return new SimpleName[] { name };
	}

	public static Name[] findByNode(ASTNode root, Name name) {
		Name[] names= findByProblems(root, name);
		if (names != null) {
			return names;
		}
		int parentKind= name.getParent().getNodeType();
		if (parentKind == ASTNode.LABELED_STATEMENT || parentKind == ASTNode.BREAK_STATEMENT || parentKind == ASTNode.CONTINUE_STATEMENT) {
			ArrayList<Name> res= new ArrayList<>();
			QLabelFinder nodeFinder= new QLabelFinder(name, res);
			root.accept(nodeFinder);
			return res.toArray(new Name[res.size()]);
		}
		return new Name[] { name };
	}



	private static final int FIELD= 1;
	private static final int METHOD= 2;
	private static final int TYPE= 4;
	private static final int LABEL= 8;
	private static final int NAME= FIELD | TYPE;

	private static int getProblemKind(IProblem problem) {
		switch (problem.getID()) {
			case IProblem.UndefinedField:
				return FIELD;
			case IProblem.UndefinedMethod:
				return METHOD;
			case IProblem.UndefinedLabel:
				return LABEL;
			case IProblem.UndefinedName:
			case IProblem.UnresolvedVariable:
				return NAME;
			case IProblem.UndefinedType:
				return TYPE;
		}
		return 0;
	}

	private static int getNameNodeProblemKind(IProblem[] problems, SimpleName nameNode) {
		int nameOffset= nameNode.getStartPosition();
		int nameInclEnd= nameOffset + nameNode.getLength() - 1;

		for (IProblem curr : problems) {
			if (curr.getSourceStart() == nameOffset && curr.getSourceEnd() == nameInclEnd) {
				int kind= getProblemKind(curr);
				if (kind != 0) {
					return kind;
				}
			}
		}
		return 0;
	}

	private static int getNameNodeProblemKind(IProblem[] problems, Name nameNode) {
		int nameOffset= nameNode.getStartPosition();
		int nameInclEnd= nameOffset + nameNode.getLength() - 1;

		for (IProblem curr : problems) {
			if (curr.getSourceStart() == nameOffset && curr.getSourceEnd() == nameInclEnd) {
				int kind= getProblemKind(curr);
				if (kind != 0) {
					return kind;
				}
			}
		}
		return 0;
	}


	public static SimpleName[] findByProblems(ASTNode parent, SimpleName nameNode) {
		if (nameNode.getAST().apiLevel() >= ASTHelper.JLS10 && nameNode.isVar()) {
			return null;
		}
		ArrayList<SimpleName> res= new ArrayList<>();

		ASTNode astRoot = parent.getRoot();
		if (!(astRoot instanceof CompilationUnit)) {
			return null;
		}

		IProblem[] problems= ((CompilationUnit) astRoot).getProblems();
		int nameNodeKind= getNameNodeProblemKind(problems, nameNode);
		if (nameNodeKind == 0) { // no problem on node
			return null;
		}

		int bodyStart= parent.getStartPosition();
		int bodyEnd= bodyStart + parent.getLength();

		String name= nameNode.getIdentifier();

		for (IProblem curr : problems) {
			int probStart= curr.getSourceStart();
			int probEnd= curr.getSourceEnd() + 1;

			if (probStart > bodyStart && probEnd < bodyEnd) {
				int currKind= getProblemKind(curr);
				if ((nameNodeKind & currKind) != 0) {
					ASTNode node= NodeFinder.perform(parent, probStart, (probEnd - probStart));
					if (node instanceof SimpleName && name.equals(((SimpleName) node).getIdentifier())) {
						if (node.getAST().apiLevel() < ASTHelper.JLS10 || !((SimpleName) node).isVar()) {
							res.add((SimpleName) node);
						}
					}
				}
			}
		}
		return res.toArray(new SimpleName[res.size()]);
	}

	public static Name[] findByProblems(ASTNode parent, Name nameNode) {
		ArrayList<Name> res= new ArrayList<>();

		ASTNode astRoot = parent.getRoot();
		if (!(astRoot instanceof CompilationUnit)) {
			return null;
		}

		IProblem[] problems= ((CompilationUnit) astRoot).getProblems();
		int nameNodeKind= getNameNodeProblemKind(problems, nameNode);
		if (nameNodeKind == 0) { // no problem on node
			return null;
		}

		int bodyStart= parent.getStartPosition();
		int bodyEnd= bodyStart + parent.getLength();

		String name= nameNode.getFullyQualifiedName();

		for (IProblem curr : problems) {
			int probStart= curr.getSourceStart();
			int probEnd= curr.getSourceEnd() + 1;

			if (probStart > bodyStart && probEnd < bodyEnd) {
				int currKind= getProblemKind(curr);
				if ((nameNodeKind & currKind) != 0) {
					ASTNode node= NodeFinder.perform(parent, probStart, (probEnd - probStart));
					if (node instanceof Name && name.equals(((Name) node).getFullyQualifiedName())) {
						res.add((Name) node);
					}
				}
			}
		}
		return res.toArray(new Name[res.size()]);
	}

	private static class LabelFinder extends ASTVisitor {

		private SimpleName fLabel;
		private ASTNode fDefiningLabel;
		private ArrayList<SimpleName> fResult;

		public LabelFinder(SimpleName label, ArrayList<SimpleName> result) {
			super(true);
			fLabel= label;
			fResult= result;
			fDefiningLabel= null;
		}

		private boolean isSameLabel(SimpleName label) {
			return label != null && fLabel.getIdentifier().equals(label.getIdentifier());
		}

		@Override
		public boolean visit(BreakStatement node) {
			SimpleName label= node.getLabel();
			if (fDefiningLabel != null && isSameLabel(label) && ASTNodes.isParent(label, fDefiningLabel)) {
				fResult.add(label);
			}
			return false;
		}

		@Override
		public boolean visit(ContinueStatement node) {
			SimpleName label= node.getLabel();
			if (fDefiningLabel != null && isSameLabel(label) && ASTNodes.isParent(label, fDefiningLabel)) {
				fResult.add(label);
			}
			return false;
		}

		@Override
		public boolean visit(LabeledStatement node) {
			if (fDefiningLabel == null) {
				SimpleName label= node.getLabel();
				if (fLabel == label || isSameLabel(label) && ASTNodes.isParent(fLabel, node)) {
					fDefiningLabel= node;
					fResult.add(label);
				}
			}
			node.getBody().accept(this);
			return false;
		}
	}

	private static class QLabelFinder extends ASTVisitor {

		private Name fLabel;
		private ASTNode fDefiningLabel;
		private ArrayList<Name> fResult;

		public QLabelFinder(Name label, ArrayList<Name> result) {
			super(true);
			fLabel= label;
			fResult= result;
			fDefiningLabel= null;
		}

		private boolean isSameLabel(Name label) {
			return label != null && fLabel.getFullyQualifiedName().equals(label.getFullyQualifiedName());
		}

		@Override
		public boolean visit(ModuleDeclaration node) {
			if (fDefiningLabel == null) {
				Name label= node.getName();
				if (fLabel == label || isSameLabel(label) && ASTNodes.isParent(fLabel, node)) {
					fDefiningLabel= node;
					fResult.add(label);
				}
			}
			node.getName().accept(this);
			return false;
		}
	}

	private static class BindingFinder extends ASTVisitor {

		private IBinding fBinding;
		private ArrayList<SimpleName> fResult;

		public BindingFinder(IBinding binding, ArrayList<SimpleName> result) {
			super(true);
			fBinding= getDeclaration(binding);
			fResult= result;
		}

		@Override
		public boolean visit(SimpleName node) {
			if (node.getAST().apiLevel() >= ASTHelper.JLS10 && node.isVar()) {
				return false;
			}
			IBinding binding= node.resolveBinding();
			if (binding == null) {
				return false;
			}
			binding= getDeclaration(binding);
			SimpleName rcNode= getAssociatedRecordComponentNode(node);
			if (fBinding == binding) {
				fResult.add(node);
			} else if (rcNode != null) {
				IBinding rcBinding= rcNode.resolveBinding();
				if (rcBinding != null) {
					rcBinding= getDeclaration(rcBinding);
					if (fBinding == rcBinding) {
						fResult.add(node);
					}
				}
			} else if (binding.getKind() != fBinding.getKind()) {
				return false;
			} else if (binding.getKind() == IBinding.METHOD) {
				IMethodBinding curr= (IMethodBinding) binding;
				IMethodBinding methodBinding= (IMethodBinding) fBinding;
				if (methodBinding.overrides(curr) || curr.overrides(methodBinding)) {
					fResult.add(node);
				}
			}
			return false;
		}

		private static IBinding getDeclaration(IBinding binding) {
			if (binding instanceof ITypeBinding) {
				return ((ITypeBinding) binding).getTypeDeclaration();
			} else if (binding instanceof IMethodBinding) {
				IMethodBinding methodBinding= (IMethodBinding) binding;
				if (methodBinding.isConstructor()) { // link all constructors with their type
					return methodBinding.getDeclaringClass().getTypeDeclaration();
				} else {
					return methodBinding.getMethodDeclaration();
				}
			} else if (binding instanceof IVariableBinding) {
				return ((IVariableBinding) binding).getVariableDeclaration();
			}
			return binding;
		}
	}
}
