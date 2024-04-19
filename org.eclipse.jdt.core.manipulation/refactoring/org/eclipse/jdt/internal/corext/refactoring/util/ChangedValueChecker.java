/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
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
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
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
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.fragments.IASTFragment;

public class ChangedValueChecker extends AbstractChecker {

	private ASTNode fNode2;

	private Set<Elem> fDependSet;

	private ASTNode fBodyNode;

	private static final int THRESHOLD= 2500;

	private ArrayList<ASTNode> fMiddleNodes;

	private volatile boolean fConflict;

	private Set<Position> fPosSet;

	private String fEnclosingMethodSignature;

	public ChangedValueChecker(ASTNode selectedExpression, String enclosingMethodSignature) {
		this.fEnclosingMethodSignature= enclosingMethodSignature;
		analyzeSelectedExpression(selectedExpression);
	}

	public void detectConflict(int startOffset, int endOffset, ASTNode node,
			ASTNode bodyNode, ArrayList<IASTFragment> candidateList) {
		fNode2= node;
		fBodyNode= bodyNode;
		fConflict= false;
		fPosSet= ConcurrentHashMap.newKeySet();
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
		fDependSet= ConcurrentHashMap.newKeySet();
		ReadVisitor readVisitor= new ReadVisitor(true);
		selectedExpression.accept(readVisitor);
		fDependSet.addAll(readVisitor.readSet);
	}

	public boolean hasConflict() {
		ExecutorService threadPool= new ThreadPoolExecutor(5, 10, 5, TimeUnit.SECONDS,
				new ArrayBlockingQueue<>(10), new ThreadPoolExecutor.CallerRunsPolicy());
		try {
			for (ASTNode node : fMiddleNodes) {
				if (fConflict) {
					break;
				}
				Position pos= new Position(node.getStartPosition(), node.getLength());
				if (fPosSet.add(pos)) {
					threadPool.execute(() -> {
						UpdateVisitor uv= new UpdateVisitor(fDependSet, true);
						node.accept(uv);
						if (uv.hasConflict()) {
							fConflict= true;
						}
					});
				}
			}
			if (!fConflict) {
				threadPool.shutdown();
				while (!threadPool.isTerminated() && !fConflict) {
					threadPool.awaitTermination(10, TimeUnit.MILLISECONDS);
				}
			}
		} catch (InterruptedException e) {
		} finally {
			threadPool.shutdownNow();
		}
		return fConflict;
	}
	private class FunctionSearchRequestor extends SearchRequestor {

		public List<SearchMatch> results= new ArrayList<>();

		public List<SearchMatch> getResults() {
			return results;
		}

		@Override
		public void acceptSearchMatch(SearchMatch match) throws CoreException {
			if (match.getAccuracy() == SearchMatch.A_ACCURATE) {
				results.add(match);
			}
		}

	}

	protected void search(SearchPattern searchPattern, IJavaSearchScope scope, SearchRequestor requestor) throws CoreException {
		new SearchEngine().search(
			searchPattern,
			new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()},
			scope,
			requestor,
			null);
	}

	private MethodDeclaration findFunctionDefinition(ITypeBinding iTypeBinding, IMethodBinding methodBinding) {
		if (methodBinding == null || iTypeBinding == null) {
			return null;
		}
		if (!(iTypeBinding.getJavaElement() instanceof IType)) {
			return null;
		}
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
			IMethod iMethod= (IMethod) methodBinding.getJavaElement();
			if (iMethod == null) {
				return null;
			}
			IType type= iMethod.getDeclaringType();
			if (type == null) {
				return null;
			}
			if (!type.isInterface()) {
				ICompilationUnit icu= iMethod.getCompilationUnit();
				if (icu == null || icu.getSource() == null) {
					return null;
				}
				return getMethodDeclaration(iMethod, icu);
			} else {
				String typeName= type.getFullyQualifiedName();
				SearchPattern pattern = SearchPattern.createPattern(typeName, IJavaSearchConstants.TYPE, IJavaSearchConstants.IMPLEMENTORS, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
				FunctionSearchRequestor requestor= new FunctionSearchRequestor();
				try {
					search(pattern, SearchEngine.createJavaSearchScope(new IJavaElement[] {iMethod.getJavaProject()}), requestor);
				} catch (CoreException e) {
					return null;
				}
				List<SearchMatch> results= requestor.getResults();
				for (SearchMatch result : results) {
					Object obj= result.getElement();
					if (obj instanceof IType resultType) {
						ICompilationUnit icu= resultType.getCompilationUnit();
						if (icu != null && icu.getSource() != null) {
							MethodDeclaration md= getMethodDeclaration(iMethod, icu);
							if (md != null) {
								return md;
							}
						}
					}
				}
			}
		} catch (JavaModelException | OperationCanceledException e) {
			// ignore
		}
		return null;
	}

	private MethodDeclaration getMethodDeclaration(IMethod iMethod, ICompilationUnit icu) throws JavaModelException {
		ASTParser parser= ASTParser.newParser(AST.getJLSLatest());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(icu);
		parser.setResolveBindings(true);
		CompilationUnit compilationUnit= (CompilationUnit) parser.createAST(null);
		ASTNode perform= NodeFinder.perform(compilationUnit, iMethod.getSourceRange());
		if (perform instanceof MethodDeclaration && ((MethodDeclaration) perform).resolveBinding() != null) {
			MethodDeclaration md= (MethodDeclaration) perform;
			if (!Modifier.isAbstract(md.resolveBinding().getModifiers())) {
				return md;
			}
		}
		return null;
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

	enum TraversalStatus {
		NOT_YET_BETWEEN_EXPRESSIONS, BETWEEN_EXPRESSIONS, EXITED_BETWEEN_EXPRESSIONS
	}

	/*
	 * This class calculates the path between two expressions in AST.
	 * It considers loop and conditional statements to ensure that the calculation is not based only on positions.
	 */
	class PathVisitor extends ASTVisitor {
		ArrayList<ASTNode> nodes;

		ArrayList<IASTFragment> candidateList;

		HashSet<Position> posSet;

		int startOffset;

		int endOffset;

		TraversalStatus state;

		ASTNode selectedExpression;

		public PathVisitor(int startOffset, int endOffset, ASTNode node, ArrayList<IASTFragment> candidateList) {
			nodes= new ArrayList<>();
			posSet= new HashSet<>();
			this.startOffset= startOffset;
			this.endOffset= endOffset;
			this.selectedExpression= node;
			this.candidateList= candidateList;
			state= TraversalStatus.NOT_YET_BETWEEN_EXPRESSIONS;
			extend2EndOfLoop(selectedExpression);
		}

		private void extend2EndOfLoop(ASTNode node) {
			ASTNode temp= node;
			while (temp != null && !(temp instanceof MethodDeclaration)) {
				if (temp instanceof EnhancedForStatement || temp instanceof WhileStatement
						|| temp instanceof ForStatement || temp instanceof DoStatement) {
					int offset= temp.getStartPosition();
					int length= temp.getLength();
					int newEndOffset= offset + length;

					// the current offset is too low (< startOffset) and the new one could fall inside it
					boolean cond1= offset < startOffset && newEndOffset > startOffset;

					// the current offset could be inside the range and the new one would fall outside of it (>= endOffset)
					boolean cond2= offset <= endOffset && newEndOffset >= endOffset;
					if (!cond1 && cond2) {
						endOffset= newEndOffset;
					}
				}
				temp= temp.getParent();
			}
		}

		@Override
		public boolean preVisit2(ASTNode node) {
			if (state == TraversalStatus.NOT_YET_BETWEEN_EXPRESSIONS && node.getStartPosition() >= startOffset && node.getStartPosition() + node.getLength() <= endOffset) {
				state= TraversalStatus.BETWEEN_EXPRESSIONS;
			} else if (state == TraversalStatus.BETWEEN_EXPRESSIONS && node.getStartPosition() > endOffset) {
				state= TraversalStatus.EXITED_BETWEEN_EXPRESSIONS;
			}

			if (state != TraversalStatus.BETWEEN_EXPRESSIONS) {
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
			if (resolveMethodBinding == null) {
				return super.visit(methodInvocation);
			}
			if (!this.visitMethodCall || resolveMethodBinding.getMethodDeclaration() != null && fEnclosingMethodSignature != null &&
					fEnclosingMethodSignature.equals(resolveMethodBinding.getMethodDeclaration().getKey())) {
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

		private Set<Elem> dependSet;

		private boolean visitMethodCall;

		public UpdateVisitor(Set<Elem> dependSet, boolean visitMethodCall) {
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
			if (!this.visitMethodCall || resolveMethodBinding.getMethodDeclaration() != null && fEnclosingMethodSignature != null &&
					fEnclosingMethodSignature.equals(resolveMethodBinding.getMethodDeclaration().getKey())) {
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
