/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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
 *     Xiaye Chi <xychichina@gmail.com> - [extract local] Improve the Safety by identifying statements that may change the value of the extracted expressions - https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/432
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.util;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.WhileStatement;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.fragments.IASTFragment;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class ChangedValueChecker {

	private ASTNode fNode2;

	private HashSet<Elem> fDependSet;

	private ASTNode fBodyNode;

	private final int THRESHOLD= 2500;

	private ArrayList<ASTNode> fMiddleNodes;

	private boolean fConflict;

	private HashSet<Position> fPosSet;

	public ChangedValueChecker(ASTNode selectedExpression) {
		super();
		analyzeSelectedExpression(selectedExpression);
	}


	public void detectConflict(int startOffset, int endOffset, ASTNode node1, ASTNode node2,
			ASTNode bodyNode, ArrayList<IASTFragment> candidateList) {
		fNode2= node2;
		fBodyNode= bodyNode;
		fConflict= false;
		fPosSet= new HashSet<>();
		PathVisitor pathVisitor= new PathVisitor(startOffset, endOffset, fNode2, candidateList);
		while (fBodyNode != null && (fBodyNode.getStartPosition() + fBodyNode.getLength() < pathVisitor.endOffset
				|| fBodyNode.getStartPosition() > pathVisitor.startOffset)) {
			fBodyNode= fBodyNode.getParent();
		}
		if (fBodyNode != null)
			fBodyNode.accept(pathVisitor);
		fMiddleNodes= pathVisitor.nodes;
	}

	public void analyzeSelectedExpression(ASTNode selectedExpression) {
		fDependSet= new HashSet<>();
		ReadVisitor readVisitor= new ReadVisitor(true);
		selectedExpression.accept(readVisitor);
		fDependSet.addAll(readVisitor.readSet);
	}

	public boolean hasConflict() {
		List<Thread> threadList= new ArrayList<>();
		for (ASTNode node : fMiddleNodes) {
			Position pos= new Position(node.getStartPosition(), node.getLength());
			if (fPosSet.contains(pos)) {
				continue;
			}
			if (fConflict == true) {
				break;
			}
			Thread t= new Thread(() -> {
				fPosSet.add(pos);
				UpdateVisitor uv= new UpdateVisitor(fDependSet, true);
				node.accept(uv);
				if (uv.hasConflict())
					fConflict= true;

			});
			threadList.add(t);
			t.start();
		}


		for (Thread thread : threadList) {
			try {
				thread.join();
			} catch (InterruptedException e) {
			}
		}
		return fConflict;
	}



	private MethodDeclaration findFunctionDefinition(ITypeBinding iTypeBinding, IMethodBinding methodBinding) {
		if (methodBinding == null || iTypeBinding == null) {
			return null;
		}
		if (!(iTypeBinding.getJavaElement() instanceof IType))
			return null;
		IType it= (IType) (iTypeBinding.getJavaElement());
		try {
			IJavaElement root= it.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
			if (root instanceof IPackageFragmentRoot) {
				IClasspathEntry cpEntry= ((IPackageFragmentRoot) root).getRawClasspathEntry();
				if (cpEntry.getEntryKind() == IClasspathEntry.CPE_CONTAINER
						&& cpEntry.getPath().toString().startsWith("org.eclipse.jdt.launching.JRE_CONTAINER")) { //$NON-NLS-1$
					return null;
				}
			}
			ITypeHierarchy ith= it.newTypeHierarchy(iTypeBinding.getJavaElement().getJavaProject(), null);
			IMethod iMethod= (IMethod) methodBinding.getJavaElement();
			if (iMethod == null || ith == null) {
				return null;
			}

			ArrayList<IType> iTypes= new ArrayList<>();
			findTypes(it, ith, iTypes);
			for (IType t : iTypes) {
				IMethod tmp= JavaModelUtil.findMethod(iMethod.getElementName(),
						iMethod.getParameterTypes(), false, t);
				if (tmp != null) {
					ICompilationUnit icu= tmp.getCompilationUnit();
					if (icu == null || icu.getSource() == null) {
						return null;
					}
					ASTParser parser= ASTParser.newParser(AST.getJLSLatest());
					parser.setKind(ASTParser.K_COMPILATION_UNIT);
					parser.setSource(icu);
					parser.setResolveBindings(true);
					CompilationUnit compilationUnit= (CompilationUnit) parser.createAST(null);
					final ASTNode perform= NodeFinder.perform(compilationUnit, tmp.getSourceRange());
					if (perform instanceof MethodDeclaration && ((MethodDeclaration) perform).resolveBinding() != null) {
						MethodDeclaration md= (MethodDeclaration) perform;
						if (Modifier.isAbstract(md.resolveBinding().getModifiers()))
							continue;
						return md;
					} else {
						return null;
					}
				}
			}
			return null;
		} catch (JavaModelException e) {
			;
		}
		return null;
	}

	private static void findTypes(IType it, ITypeHierarchy ith, ArrayList<IType> iTypes) {
		iTypes.add(it);

		for (IType i : ith.getAllSubtypes(it)) {
			iTypes.add(i);
		}
		return;
	}

	private ASTNode getOriginalExpression(ASTNode node) {
		while (node != null) {
			if (node instanceof ParenthesizedExpression) {
				ParenthesizedExpression pe= (ParenthesizedExpression) node;
				node= pe.getExpression();
			} else if (node instanceof CastExpression) {
				CastExpression ce= (CastExpression) node;
				node= ce.getExpression();
			} else
				break;
		}
		return node;
	}

	class Elem {
		Object memberKey;

		Elem e;

		public Elem(ASTNode node, boolean flag) {
			super();
			if (node instanceof SimpleName) {
				SimpleName sn= (SimpleName) node;
				IBinding resolveBinding= sn.resolveBinding();
				if (resolveBinding != null && resolveBinding.getJavaElement() != null) {
					if (resolveBinding.getJavaElement().getElementType() != IJavaElement.LOCAL_VARIABLE
							|| flag)
						this.memberKey= resolveBinding.getKey();
				}
			} else if (node instanceof QualifiedName) {
				QualifiedName qn= (QualifiedName) node;
				SimpleName sn= qn.getName();
				this.memberKey= sn.resolveBinding() != null ? sn.resolveBinding().getKey() : null;
				if (qn.resolveBinding() != null && qn.resolveBinding().getModifiers() != Modifier.STATIC)
					this.e= new Elem(qn.getQualifier(), flag);
			} else if (node instanceof FieldAccess) {
				FieldAccess fa= (FieldAccess) node;
				SimpleName sn= fa.getName();
				this.memberKey= sn.resolveBinding() != null ? sn.resolveBinding().getKey() : null;
				ASTNode expr= getOriginalExpression(fa.getExpression());
				if (expr instanceof MethodInvocation) {
					if (flag) {
						this.e= new Elem((MethodInvocation) expr);
					} else {
						this.memberKey= null;
					}
				} else
					this.e= new Elem(expr, flag);
			} else if (node instanceof MethodInvocation) {
				MethodInvocation mi= (MethodInvocation) node;
				if (flag) {
					this.e= new Elem(mi);
				} else {
					this.memberKey= null;
				}
			}
		}

		public Elem(MethodInvocation expr) { // use String to represent the instances of MethodInvocation
			super();
			this.memberKey= expr.toString();
		}

		public Elem(ASTNode node, boolean flag, Elem e) {
			if (node instanceof MethodInvocation && flag) {
				MethodInvocation mi= (MethodInvocation) node;
				this.e= new Elem(mi.getExpression(), flag);
				this.memberKey= e;
			}
		}

		@Override
		public int hashCode() {
			final int prime= 31;
			int result= 1;
			result= prime * result + getEnclosingInstance().hashCode();
			result= prime * result + this.toString().hashCode();
			return result;
		}

		@Override
		public String toString() {
			String str1= this.e == null ? "" : this.e.toString(); //$NON-NLS-1$
			String str2= ""; //$NON-NLS-1$
			if (this.memberKey instanceof String) {
				str2= (String) this.memberKey;
			} else if (this.memberKey instanceof Elem) {
				str2= ((Elem) this.memberKey).toString();
			}
			if (!str1.equals("")) //$NON-NLS-1$
				return str1 + str2;
			else
				return str2;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof Elem)) {
				return false;
			}
			Elem other= (Elem) obj;
			if (!getEnclosingInstance().equals(other.getEnclosingInstance())) {
				return false;
			}
			String str1= this.toString();
			String str2= obj.toString();
			return str1.equals(str2);
		}

		private ChangedValueChecker getEnclosingInstance() {
			return ChangedValueChecker.this;
		}
	}

	class PathVisitor extends ASTVisitor {
		ArrayList<ASTNode> nodes;

		ArrayList<IASTFragment> candidateList;

		HashSet<Position> posSet;

		int startOffset;

		int endOffset;

		int type;

		ASTNode selectedExpression;

		public PathVisitor(int startOffset, int endOffset, ASTNode node, ArrayList<IASTFragment> candidateList) {
			nodes= new ArrayList<>();
			posSet= new HashSet<>();
			this.startOffset= startOffset;
			this.endOffset= endOffset;
			this.selectedExpression= node;
			this.candidateList= candidateList;
			type= 2;
			extend2EndOfLoop(selectedExpression);
		}

		private void extend2EndOfLoop(ASTNode node) {
			ASTNode temp= node;
			while (temp != null && !(temp instanceof MethodDeclaration)) {
				if (temp instanceof EnhancedForStatement || temp instanceof WhileStatement
						|| temp instanceof ForStatement || temp instanceof DoStatement) {
					int offset= temp.getStartPosition();
					int length= temp.getLength();
					if (offset < startOffset && offset + length > startOffset)
						;
					else if (offset <= endOffset && offset + length >= endOffset) {
						endOffset= offset + length;
					}
				}
				temp= temp.getParent();
			}
		}

		@Override
		public boolean preVisit2(ASTNode node) {
			if (type == 2 && node.getStartPosition() >= startOffset && node.getStartPosition() + node.getLength() <= endOffset) {
				type= 1;
			} else if (type == 1 && node.getStartPosition() > endOffset) {
				type= 0;
			}

			if (type != 1) {
				return super.preVisit2(node);
			}

			if (node instanceof Statement && node.getParent() instanceof IfStatement) {
				IfStatement is= (IfStatement) (node.getParent());
				if (node.getLocationInParent() == IfStatement.THEN_STATEMENT_PROPERTY &&
						is.getElseStatement() != null && ASTNodes.isParent(selectedExpression, is.getElseStatement())) {
					int offset= is.getThenStatement().getStartPosition();
					int length= is.getThenStatement().getLength();
					for (int i= 0; i < candidateList.size(); ++i) {
						if (candidateList.get(i).getStartPosition() >= offset &&
								candidateList.get(i).getStartPosition() <= offset + length) {
							while (i < candidateList.size() - 1 && candidateList.get(1 + i).getStartPosition() >= offset &&
									candidateList.get(1 + i).getStartPosition() <= offset + length)
								i++;
							if (i < candidateList.size()) {
								PathVisitor pv= new PathVisitor(offset, candidateList.get(i).getStartPosition(), candidateList.get(i).getAssociatedNode(), candidateList);
								is.getThenStatement().accept(pv);
								this.nodes.addAll(pv.nodes);
								this.posSet.addAll(pv.posSet);
							}
							break;
						}
					}
					return false;
				}
			}

			if (node.getStartPosition() >= startOffset && node.getStartPosition() + node.getLength() <= endOffset) {
				if (node instanceof Type || node instanceof NumberLiteral || node instanceof StringLiteral
						|| node instanceof NullLiteral) {
					return false;
				}
				Position pos= new Position(node.getStartPosition(), node.getLength());
				if (posSet.add(pos)) {
					nodes.add(node);
				}
				return false;
			}

			return super.preVisit2(node);
		}

	}



	class ReadVisitor extends ASTVisitor {

		private HashSet<Elem> readSet;

		private boolean visitMethodCall;

		public ReadVisitor(boolean visitMethodCall) {
			this.readSet= new HashSet<>();
			this.visitMethodCall= visitMethodCall;
		}

		private void addToList(Elem e) {
			if (e.memberKey == null) {
				return;
			}
			readSet.add(e);
		}

		@Override
		public boolean visit(FieldAccess fieldAccess) {
			Elem e= new Elem(fieldAccess, visitMethodCall);
			addToList(e);
			return false;
		}

		@Override
		public boolean visit(QualifiedName qualifiedName) {
			Elem e= new Elem(qualifiedName, visitMethodCall);
			addToList(e);
			return false;
		}

		@Override
		public boolean visit(SimpleName simpleName) {
			IBinding iBinding= simpleName.resolveBinding();
			if (iBinding instanceof IVariableBinding) {
				Elem e= new Elem(simpleName, visitMethodCall);
				addToList(e);
			}
			return false;
		}

		@Override
		public boolean visit(MethodInvocation methodInvocation) {
			final IMethodBinding resolveMethodBinding= methodInvocation.resolveMethodBinding();
			if (!this.visitMethodCall) {
				return super.visit(methodInvocation);
			}
			MethodDeclaration md= findFunctionDefinition(resolveMethodBinding.getDeclaringClass(), resolveMethodBinding);
			if (md != null && md.getLength() < THRESHOLD) {
				ReadVisitor rv= new ReadVisitor(false);
				md.accept(rv);
				for (Elem e : rv.readSet) {
					addToList(new Elem(methodInvocation, visitMethodCall, e));
				}
			}
			return super.visit(methodInvocation);
		}
	}

	class UpdateVisitor extends ASTVisitor {

		private HashSet<Elem> updateSet;

		private HashSet<Elem> dependSet;

		private boolean visitMethodCall;

		public UpdateVisitor(HashSet<Elem> dependSet, boolean visitMethodCall) {
			this.updateSet= new HashSet<>();
			this.visitMethodCall= visitMethodCall;
			this.dependSet= dependSet;
		}

		private void addToList(Elem e) {
			if (e.memberKey == null) {
				return;
			}
			updateSet.add(e);
		}

		private boolean hasConflict() {
			for (Elem e : dependSet) {
				if (updateSet.contains(e)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean visit(Assignment assignment) {
			ReadVisitor v= new ReadVisitor(visitMethodCall);
			assignment.getLeftHandSide().accept(v);
			for (Elem e : v.readSet) {
				addToList(e);
			}
			return super.visit(assignment);
		}

		@Override
		public boolean visit(PrefixExpression prefixExpression) {
			PrefixExpression.Operator op= prefixExpression.getOperator();
			if (op == PrefixExpression.Operator.INCREMENT || op == PrefixExpression.Operator.DECREMENT) {
				ReadVisitor v= new ReadVisitor(visitMethodCall);
				prefixExpression.getOperand().accept(v);
				for (Elem e : v.readSet) {
					addToList(e);
				}
			}
			return super.visit(prefixExpression);
		}

		@Override
		public boolean visit(PostfixExpression postfixExpression) {
			PostfixExpression.Operator op= postfixExpression.getOperator();
			if (op == PostfixExpression.Operator.INCREMENT || op == PostfixExpression.Operator.DECREMENT) {
				ReadVisitor v= new ReadVisitor(visitMethodCall);
				postfixExpression.getOperand().accept(v);
				for (Elem e : v.readSet) {
					addToList(e);
				}
			}
			return super.visit(postfixExpression);
		}

		@Override
		public boolean visit(MethodInvocation methodInvocation) {
			final IMethodBinding resolveMethodBinding= methodInvocation.resolveMethodBinding();
			if (!this.visitMethodCall) {
				return super.visit(methodInvocation);
			}
			MethodDeclaration md= findFunctionDefinition(resolveMethodBinding.getDeclaringClass(), resolveMethodBinding);
			if (md != null && md.getLength() < THRESHOLD) {
				UpdateVisitor uv= new UpdateVisitor(dependSet, false);
				md.accept(uv);
				for (Elem e : uv.updateSet) {
					addToList(new Elem(methodInvocation, visitMethodCall, e));
				}
			}
			return super.visit(methodInvocation);
		}
	}

}
