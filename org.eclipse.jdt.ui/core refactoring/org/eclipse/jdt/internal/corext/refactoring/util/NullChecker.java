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
 *     Xiaye Chi <xychichina@gmail.com> - [extract local] Extract to local variable may result in NullPointerException. - https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/39
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;

import org.eclipse.jdt.internal.corext.dom.fragments.ASTFragmentFactory;
import org.eclipse.jdt.internal.corext.dom.fragments.IASTFragment;

public class NullChecker {
	private ASTNode fExpression;

	private int fStartOffset;

	private int fEndOffset;

	private ASTNode fCommonNode;

	private Set<IBinding> fInvocationSet;

	private CompilationUnit fCompilationUnitNode;

	private ICompilationUnit fCu;

	private Set<Integer> fMatchNodePosSet;

	public NullChecker(CompilationUnit fCompilationUnitNode, ICompilationUnit fCu, ASTNode commonNode, ASTNode expression, int startOffset, int endOffset) {
		this.fCompilationUnitNode= fCompilationUnitNode;
		this.fCu= fCu;
		this.fCommonNode= commonNode;
		this.fExpression= expression;
		this.fStartOffset= startOffset;
		this.fEndOffset= endOffset;
		InvocationVisitor iv= new InvocationVisitor();
		this.fExpression.accept(iv);
		this.fInvocationSet= iv.invocationSet;
		this.fMatchNodePosSet= iv.matchNodePosSet;
	}

	public boolean hasNullCheck() {
		NullMiddleCodeVisitor nullMiddleCodeVisitor= new NullMiddleCodeVisitor(this.fInvocationSet, this.fMatchNodePosSet, this.fStartOffset, this.fEndOffset);
		this.fCommonNode.accept(nullMiddleCodeVisitor);
		return nullMiddleCodeVisitor.hasNullCheck();
	}

	private IASTFragment getIASTFragment(ASTNode astNode) throws JavaModelException {
		int length= astNode.getLength();
		int startPosition= astNode.getStartPosition();
		return ASTFragmentFactory.createFragmentForSourceRange(new SourceRange(startPosition, length), this.fCompilationUnitNode, this.fCu);

	}

	private ASTNode getEnclosingBodyNode(ASTNode node) {
		StructuralPropertyDescriptor location= null;
		while (node != null && !(node instanceof BodyDeclaration)) {
			location= node.getLocationInParent();
			node= node.getParent();
			if (node instanceof LambdaExpression) {
				break;
			}
		}
		if (node != null) {
			if (location == MethodDeclaration.BODY_PROPERTY || location == Initializer.BODY_PROPERTY
					|| (location == LambdaExpression.BODY_PROPERTY && ((LambdaExpression) node).resolveMethodBinding() != null)) {
				return (ASTNode) node.getStructuralProperty(location);
			}
		}
		return null;
	}

	private Expression getOriginalExpression(Expression expr) {
		while (expr instanceof ParenthesizedExpression || expr instanceof CastExpression) {
			if (expr instanceof ParenthesizedExpression) {
				ParenthesizedExpression pe= (ParenthesizedExpression) expr;
				expr= pe.getExpression();
			} else {
				CastExpression ce= (CastExpression) expr;
				expr= ce.getExpression();
			}
		}
		return expr;

	}

	private class InvocationVisitor extends ASTVisitor {
		Set<IBinding> invocationSet;

		Set<Integer> matchNodePosSet;

		InvocationVisitor() {
			this.invocationSet= new HashSet<>();
			this.matchNodePosSet= new HashSet<>();
		}

		@Override
		public void preVisit(ASTNode node) {
			Expression temp= null;
			if (node instanceof MethodInvocation) {
				MethodInvocation mi= (MethodInvocation) node;
				temp= mi.getExpression();
			} else if (node instanceof FieldAccess) {
				FieldAccess fa= (FieldAccess) node;
				temp= fa.getExpression();
			} else if (node instanceof QualifiedName) {
				QualifiedName qn= (QualifiedName) node;
				temp= qn.getQualifier();
			} else if (node instanceof ArrayAccess) {
				ArrayAccess aa= (ArrayAccess) node;
				temp= aa.getArray();
			}

			if (temp != null) {
				try {
					temp= getOriginalExpression(temp);
					IASTFragment[] allMatches= ASTFragmentFactory.createFragmentForFullSubtree(getEnclosingBodyNode(temp)).getSubFragmentsMatching(getIASTFragment(temp));
					for (IASTFragment match : allMatches) {
						if (match.getAssociatedNode() != null) {
							this.matchNodePosSet.add(match.getAssociatedNode().getStartPosition());
						}
					}
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
				IBinding resolveBinding= null;
				if (temp instanceof Name)
					resolveBinding= ((Name) temp).resolveBinding();
				if (resolveBinding != null)
					this.invocationSet.add(resolveBinding);
			}
		}

	}

	private class NullMiddleCodeVisitor extends ASTVisitor {
		int startPosition;

		int endPosition;

		boolean nullFlag;

		Set<IBinding> invocationSet;

		Set<Integer> matchNodePosSet;

		public NullMiddleCodeVisitor(Set<IBinding> invocationSet, Set<Integer> matchNodePosSet, int startPosition, int endPosition) {
			this.invocationSet= invocationSet;
			this.matchNodePosSet= matchNodePosSet;
			this.startPosition= startPosition;
			this.endPosition= endPosition;
			this.nullFlag= false;
		}

		public boolean hasNullCheck() {
			return this.nullFlag;
		}

		@Override
		public boolean preVisit2(ASTNode node) {
			int sl= node.getStartPosition();
			int el= node.getStartPosition() + node.getLength();
			if (el < startPosition || sl > endPosition || this.nullFlag == true) {
				return false;
			}
			Expression target= null;
			if (sl >= startPosition && el <= endPosition && node instanceof InfixExpression) {
				InfixExpression infixExpression= (InfixExpression) node;
				Operator op= infixExpression.getOperator();
				if (Operator.toOperator(op.toString()) == Operator.EQUALS || Operator.toOperator(op.toString()) == Operator.NOT_EQUALS) {
					Expression leftExpression= infixExpression.getLeftOperand();
					Expression rightExpression= infixExpression.getRightOperand();
					if (rightExpression.getNodeType() == ASTNode.NULL_LITERAL) {
						target= leftExpression;
					} else if (leftExpression.getNodeType() == ASTNode.NULL_LITERAL) {
						target= rightExpression;
					}
				}
			}

			if (target != null) {
				target= getOriginalExpression(target);
				IBinding targetBinding= null;
				if (target instanceof Name && (targetBinding= ((Name)target).resolveBinding()) != null && this.invocationSet.contains(targetBinding)) {
					this.nullFlag= true;
					return false;
				} else if (this.matchNodePosSet.contains(target.getStartPosition())) {
					this.nullFlag= true;
					return false;
				}
			}
			return super.preVisit2(node);
		}
	}

}
