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
 *     Xiaye Chi <xychichina@gmail.com> - [extract local] Improve the Safety by Identifying the Side Effect of Selected Expression - https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/348
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class SideEffectChecker extends ASTVisitor {

	public SideEffectChecker(ASTNode astNode, String enclosingMethodSignature) {
		fExpression= astNode;
		fSideEffect= false;
		fEnclosingMethodSignature= enclosingMethodSignature;
	}

	boolean fSideEffect;

	ASTNode fExpression;

	private static final int THRESHOLD= 2500;

	private String fEnclosingMethodSignature;

	static private HashSet<String> MARKEDMETHODSET= new HashSet<>();
	static {
		String[] methodNames= { "java.lang.System.currentTimeMillis", "java.lang.System.nanoTime", //$NON-NLS-1$ //$NON-NLS-2$
				"java.io.PrintStream.print", "java.io.PrintStream.printf", //$NON-NLS-1$ //$NON-NLS-2$
				"java.io.PrintStream.println" }; //$NON-NLS-1$
		MARKEDMETHODSET.addAll(Arrays.asList(methodNames));
	}


	public boolean hasSideEffect() {
		return fSideEffect;
	}

	@Override
	public boolean preVisit2(ASTNode node) {
		if (hasSideEffect()) {
			return false;
		}
		if (selfModied(node)) {
			fSideEffect= true;
			return false;
		}
		if (node instanceof MethodInvocation) {
			MethodInvocation node2= (MethodInvocation) node;
			final IMethodBinding resolveMethodBinding= node2.resolveMethodBinding();
			if (MARKEDMETHODSET.contains(getQualifiedName(resolveMethodBinding))
					|| resolveMethodBinding == null) {
				fSideEffect= true;
				return false;
			}
			if (resolveMethodBinding.getMethodDeclaration() != null && fEnclosingMethodSignature != null &&
					fEnclosingMethodSignature.equals(resolveMethodBinding.getMethodDeclaration().getKey()))
				return super.preVisit2(node);
			MethodDeclaration md= findFunctionDefinition(resolveMethodBinding.getDeclaringClass(), resolveMethodBinding);
			if (md != null && md.getLength() < THRESHOLD) {
				MethodVisitor mv= new MethodVisitor();
				md.accept(mv);
				fSideEffect= mv.hasUpdateNoTemp() == true ? true : fSideEffect;
			}
		}
		return super.preVisit2(node);
	}

	private String getQualifiedName(IMethodBinding imb) {
		if (imb == null || imb.getDeclaringClass() == null)
			return null;
		return imb.getDeclaringClass().getQualifiedName() + "." + imb.getName(); //$NON-NLS-1$
	}

	private MethodDeclaration findFunctionDefinition(ITypeBinding iTypeBinding, IMethodBinding methodBinding) {
		if (methodBinding == null || iTypeBinding == null) {
			fSideEffect= true;
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
				fSideEffect= true;
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
						fSideEffect= true;
						return null;
					}
				}
			}
			fSideEffect= true;
			return null;
		} catch (JavaModelException e) {
			fSideEffect= true;
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

	private boolean selfModied(ASTNode node) {
		if (node instanceof Assignment) {
			Assignment node2= (Assignment) node;
			ASTMatcher match= new ASTMatcher();
			if (!getOriginalExpression(node2.getRightHandSide()).subtreeMatch(match, getOriginalExpression(node2.getLeftHandSide()))) {// like a==a
				AssignmentVisitor visitor= new AssignmentVisitor(node2.getLeftHandSide());
				node2.getRightHandSide().accept(visitor);
				if (visitor.hasDepend()) {
					return true;
				}
			}
		} else if (node instanceof PrefixExpression) {
			PrefixExpression node2= (PrefixExpression) node;
			if (ASTNodes.hasOperator(node2, PrefixExpression.Operator.INCREMENT, PrefixExpression.Operator.DECREMENT)) {
				return true;
			}
		} else if (node instanceof PostfixExpression) {
			return true;
		}
		return false;
	}

	class AssignmentVisitor extends ASTVisitor {
		boolean depend;

		Expression lValue;

		public boolean hasDepend() {
			return depend;
		}

		public AssignmentVisitor(Expression lValue) {
			this.lValue= lValue;
			depend= false;
		}

		@Override
		public boolean preVisit2(ASTNode node) {
			if (depend) {
				return false;
			}
			if (node.subtreeMatch(new ASTMatcher(), lValue)) {
				this.depend= true;
			}
			return super.preVisit2(node);
		}
	}

	class MethodVisitor extends ASTVisitor {
		boolean updateNoTemp;

		public boolean hasUpdateNoTemp() {
			return updateNoTemp;
		}

		public MethodVisitor() {
			updateNoTemp= false;
		}

		@Override
		public boolean preVisit2(ASTNode node) {
			if (updateNoTemp == true) {
				return false;
			}
			if (node instanceof MethodInvocation) {
				MethodInvocation mi= (MethodInvocation) node;
				if (MARKEDMETHODSET.contains(getQualifiedName(mi.resolveMethodBinding()))) {
					updateNoTemp= true;
					return false;
				}

			}
			Expression operand= null;
			if (node instanceof Assignment) {
				Assignment node2= (Assignment) node;
				operand= getOriginalExpression(node2.getLeftHandSide());
				if (!isNoTemp(operand)) {
					return super.preVisit2(node);
				}
				if (!getOriginalExpression(node2.getRightHandSide()).subtreeMatch(new ASTMatcher(), operand)
						|| node2.getOperator() != Assignment.Operator.ASSIGN) {
					AssignmentVisitor visitor= new AssignmentVisitor(node2.getLeftHandSide());
					node2.getRightHandSide().accept(visitor);
					if (visitor.hasDepend()) {
						return updateNoTemp= true;
					}
				}
				return super.preVisit2(node);
			}

			if (node instanceof PrefixExpression) {
				PrefixExpression node2= (PrefixExpression) node;
				if (ASTNodes.hasOperator(node2, PrefixExpression.Operator.INCREMENT, PrefixExpression.Operator.DECREMENT)) {
					operand= node2.getOperand();
				}
			} else if (node instanceof PostfixExpression) {
				PostfixExpression node2= (PostfixExpression) node;
				operand= node2.getOperand();
			}
			if (operand != null && isNoTemp(operand)) {
				updateNoTemp= true;
			}
			return super.preVisit2(node);
		}

		private boolean isNoTemp(Expression e) {
			IVariableBinding ivb= null;
			Expression expr= getOriginalExpression(e);
			if (expr instanceof SimpleName) {
				SimpleName sn= (SimpleName) expr;
				if (sn.resolveBinding() instanceof IVariableBinding) {
					ivb= (IVariableBinding) sn.resolveBinding();
				}
			} else if (expr instanceof FieldAccess) {
				FieldAccess fa= (FieldAccess) expr;
				ivb= fa.resolveFieldBinding();
			} else if (expr instanceof QualifiedName) {
				QualifiedName qn= (QualifiedName) expr;
				if (qn.resolveBinding() instanceof IVariableBinding) {
					ivb= (IVariableBinding) qn.resolveBinding();
				}
				return ivb != null && Modifier.isStatic(ivb.getModifiers())
						|| isNoTemp(qn.getQualifier());
			} else if (expr instanceof ArrayAccess) {
				ArrayAccess aa= (ArrayAccess) expr;
				return isNoTemp(aa.getArray());
			}
			return ivb != null && (ivb.isField() || Modifier.isStatic(ivb.getModifiers()));
		}


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


}
