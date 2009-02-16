/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dmitry Stalnov (dstalnov@fusionone.com) - contributed fixes for:
 *       o Allow 'this' constructor to be inlined
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=38093)
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.CollectingSearchRequestor;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.base.ReferencesInBinaryContext;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.SearchUtils;

/**
 * A TargetProvider provides all targets that have to be adapted, i.e. all method invocations that should be inlined.
 */
abstract class TargetProvider {

	public abstract void initialize();

	public abstract ICompilationUnit[] getAffectedCompilationUnits(RefactoringStatus status, ReferencesInBinaryContext binaryRefs, IProgressMonitor pm)  throws CoreException;

	public abstract BodyDeclaration[] getAffectedBodyDeclarations(ICompilationUnit unit, IProgressMonitor pm);

	// constructor invocation is not an expression but a statement
	public abstract ASTNode[] getInvocations(BodyDeclaration declaration, IProgressMonitor pm);

	public abstract RefactoringStatus checkActivation() throws JavaModelException;

	public abstract int getStatusSeverity();

	public boolean isSingle() {
		return false;
	}

	public static TargetProvider create(ICompilationUnit cu, MethodInvocation invocation) {
		return new SingleCallTargetProvider(cu, invocation);
	}

	public static TargetProvider create(ICompilationUnit cu, SuperMethodInvocation invocation) {
		return new SingleCallTargetProvider(cu, invocation);
	}

	public static TargetProvider create(ICompilationUnit cu, ConstructorInvocation invocation) {
		return new SingleCallTargetProvider(cu, invocation);
	}

	public static TargetProvider create(MethodDeclaration declaration) {
		IMethodBinding method= declaration.resolveBinding();
		if (method == null)
			return new ErrorTargetProvider(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.TargetProvider_method_declaration_not_unique));
		ITypeBinding type= method.getDeclaringClass();
		if (type.isLocal()) {
			if (((IType) type.getJavaElement()).isBinary()) {
				return new ErrorTargetProvider(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.TargetProvider_cannot_local_method_in_binary));
			} else {
				IType declaringClassOfLocal= (IType) type.getDeclaringClass().getJavaElement();
				return new LocalTypeTargetProvider(declaringClassOfLocal.getCompilationUnit(), declaration);
			}
		} else {
			return new MemberTypeTargetProvider(declaration.resolveBinding());
		}
	}

	public static TargetProvider create(IMethodBinding methodBinding) {
		return new MemberTypeTargetProvider(methodBinding);
	}

	static void fastDone(IProgressMonitor pm) {
		if (pm == null)
			return;
		pm.beginTask("", 1); //$NON-NLS-1$
		pm.worked(1);
		pm.done();
	}

	static class ErrorTargetProvider extends TargetProvider {
		private RefactoringStatus fErrorStatus;
		public ErrorTargetProvider(RefactoringStatus status) {
			fErrorStatus= status;
		}
		public RefactoringStatus checkActivation() throws JavaModelException {
			return fErrorStatus;
		}
		public void initialize() {
		}
		public ICompilationUnit[] getAffectedCompilationUnits(RefactoringStatus status, ReferencesInBinaryContext binaryRefs, IProgressMonitor pm) throws JavaModelException {
			return null;
		}
		public BodyDeclaration[] getAffectedBodyDeclarations(ICompilationUnit unit, IProgressMonitor pm) {
			return null;
		}
		public ASTNode[] getInvocations(BodyDeclaration declaration, IProgressMonitor pm) {
			return null;
		}
		public int getStatusSeverity() {
			return 0;
		}
	}

	static class SingleCallTargetProvider extends TargetProvider {
		private ICompilationUnit fCUnit;
		private ASTNode fInvocation;
		private boolean fIterated;
		public SingleCallTargetProvider(ICompilationUnit cu, ASTNode invocation) {
			Assert.isNotNull(cu);
			Assert.isNotNull(invocation);
			Assert.isTrue(Invocations.isInvocation(invocation));
			fCUnit= cu;
			fInvocation= invocation;
		}
		public void initialize() {
			fIterated= false;
		}
		public ICompilationUnit[] getAffectedCompilationUnits(RefactoringStatus status, ReferencesInBinaryContext binaryRefs, IProgressMonitor pm) {
			return new ICompilationUnit[] { fCUnit };
		}
		public BodyDeclaration[] getAffectedBodyDeclarations(ICompilationUnit unit, IProgressMonitor pm) {
			Assert.isTrue(unit == fCUnit);
			if (fIterated)
				return new BodyDeclaration[0];
			fastDone(pm);
			return new BodyDeclaration[] {
				(BodyDeclaration)ASTNodes.getParent(fInvocation, BodyDeclaration.class)
			};
		}

		public ASTNode[] getInvocations(BodyDeclaration declaration, IProgressMonitor pm) {
			fastDone(pm);
			if (fIterated)
				return null;
			fIterated= true;
			return new ASTNode[] { fInvocation };
		}
		public RefactoringStatus checkActivation() throws JavaModelException {
			return new RefactoringStatus();
		}
		public int getStatusSeverity() {
			return RefactoringStatus.FATAL;
		}
		public boolean isSingle() {
			return true;
		}
	}

	private static class BodyData {
		private List fInvocations;

		public BodyData() {
		}
		public void addInvocation(ASTNode node) {
			if (fInvocations == null)
				fInvocations= new ArrayList(2);
			fInvocations.add(node);
		}
		public ASTNode[] getInvocations() {
			return (ASTNode[])fInvocations.toArray(new ASTNode[fInvocations.size()]);
		}
		public boolean hasInvocations() {
			return fInvocations != null && !fInvocations.isEmpty();
		}
	}

	private static class InvocationFinder extends ASTVisitor {
		Map/*<BodyDeclaration, BodyData>*/ result= new HashMap(2);
		Stack/*<BodyData>*/ fBodies= new Stack();
		BodyData fCurrent;
		private IMethodBinding fBinding;
		public InvocationFinder(IMethodBinding binding) {
			Assert.isNotNull(binding);
			fBinding= binding.getMethodDeclaration();
			Assert.isNotNull(fBinding);
		}
		public boolean visit(MethodInvocation node) {
			if (matches(node.getName().resolveBinding()) && fCurrent != null) {
				fCurrent.addInvocation(node);
			}
			return true;
		}
		public boolean visit(SuperMethodInvocation node) {
			if (matches(node.getName().resolveBinding()) && fCurrent != null) {
				fCurrent.addInvocation(node);
			}
			return true;
		}
		public boolean visit(ConstructorInvocation node) {
			if (matches(node.resolveConstructorBinding()) && fCurrent != null) {
				fCurrent.addInvocation(node);
			}
			return true;
		}
		public boolean visit(ClassInstanceCreation node) {
			if (matches(node.resolveConstructorBinding()) && fCurrent != null) {
				fCurrent.addInvocation(node);
			}
			return true;
		}
		public boolean visit(TypeDeclaration node) {
			return visitType();
		}
		public void endVisit(TypeDeclaration node) {
			endVisitType();
		}
		public boolean visit(EnumDeclaration node) {
			return visitType();
		}
		public void endVisit(EnumDeclaration node) {
			endVisitType();
		}
		public boolean visit(AnnotationTypeDeclaration node) {
			return visitType();
		}
		public void endVisit(AnnotationTypeDeclaration node) {
			endVisitType();
		}
		private boolean visitType() {
			fBodies.add(fCurrent);
			fCurrent= null;
			return true;
		}
		private void endVisitType() {
			fCurrent= (BodyData)fBodies.remove(fBodies.size() - 1);
		}
		public boolean visit(FieldDeclaration node) {
			fBodies.add(fCurrent);
			fCurrent= new BodyData();
			return true;
		}
		public void endVisit(FieldDeclaration node) {
			if (fCurrent.hasInvocations()) {
				result.put(node, fCurrent);
			}
			endVisitType();
		}
		public boolean visit(MethodDeclaration node) {
			fBodies.add(fCurrent);
			fCurrent= new BodyData();
			return true;
		}
		public void endVisit(MethodDeclaration node) {
			if (fCurrent.hasInvocations()) {
				result.put(node, fCurrent);
			}
			endVisitType();

		}
		public boolean visit(Initializer node) {
			fBodies.add(fCurrent);
			fCurrent= new BodyData();
			return true;
		}
		public void endVisit(Initializer node) {
			if (fCurrent.hasInvocations()) {
				result.put(node, fCurrent);
			}
			endVisitType();
		}
		private boolean matches(IBinding binding) {
			if (!(binding instanceof IMethodBinding))
				return false;
			return fBinding.isEqualTo(((IMethodBinding)binding).getMethodDeclaration());
		}
	}

	private static class LocalTypeTargetProvider extends TargetProvider {
		private ICompilationUnit fCUnit;
		private MethodDeclaration fDeclaration;
		private Map fBodies;
		public LocalTypeTargetProvider(ICompilationUnit unit, MethodDeclaration declaration) {
			Assert.isNotNull(unit);
			Assert.isNotNull(declaration);
			fCUnit= unit;
			fDeclaration= declaration;
		}
		public void initialize() {
			InvocationFinder finder= new InvocationFinder(fDeclaration.resolveBinding());
			ASTNode type= ASTNodes.getParent(fDeclaration, AbstractTypeDeclaration.class);
			type.accept(finder);
			fBodies= finder.result;
		}
		public ICompilationUnit[] getAffectedCompilationUnits(RefactoringStatus status, ReferencesInBinaryContext binaryRefs, IProgressMonitor pm) {
			fastDone(pm);
			return new ICompilationUnit[] { fCUnit };
		}

		public BodyDeclaration[] getAffectedBodyDeclarations(ICompilationUnit unit, IProgressMonitor pm) {
			Assert.isTrue(unit == fCUnit);
			Set result= fBodies.keySet();
			fastDone(pm);
			return (BodyDeclaration[])result.toArray(new BodyDeclaration[result.size()]);
		}

		public ASTNode[] getInvocations(BodyDeclaration declaration, IProgressMonitor pm) {
			BodyData data= (BodyData)fBodies.get(declaration);
			Assert.isNotNull(data);
			fastDone(pm);
			return data.getInvocations();
		}

		public RefactoringStatus checkActivation() throws JavaModelException {
			return new RefactoringStatus();
		}

		public int getStatusSeverity() {
			return RefactoringStatus.ERROR;
		}
	}

	private static class MemberTypeTargetProvider extends TargetProvider {
		private final IMethodBinding fMethodBinding;
		private Map fCurrentBodies;
		public MemberTypeTargetProvider(IMethodBinding methodBinding) {
			Assert.isNotNull(methodBinding);
			fMethodBinding= methodBinding;
		}
		public void initialize() {
			// do nothing.
		}

		public ICompilationUnit[] getAffectedCompilationUnits(final RefactoringStatus status, ReferencesInBinaryContext binaryRefs, IProgressMonitor pm) throws CoreException {
			IMethod method= (IMethod)fMethodBinding.getJavaElement();
			Assert.isTrue(method != null);

			SearchPattern pattern= SearchPattern.createPattern(method, IJavaSearchConstants.REFERENCES, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE);
			IJavaSearchScope scope= RefactoringScopeFactory.create(method, true, false);
			final HashSet affectedCompilationUnits= new HashSet();
			CollectingSearchRequestor requestor= new CollectingSearchRequestor(binaryRefs) {
				private ICompilationUnit fLastCU;
				public void acceptSearchMatch(SearchMatch match) throws CoreException {
					if (filterMatch(match))
						return;
					if (match.isInsideDocComment())
						return; // TODO: should warn user (with something like a ReferencesInBinaryContext)

					ICompilationUnit unit= SearchUtils.getCompilationUnit(match);
					if (match.getAccuracy() == SearchMatch.A_INACCURATE) {
						if (unit != null) {
							status.addError(RefactoringCoreMessages.TargetProvider_inaccurate_match,
								JavaStatusContext.create(unit, new SourceRange(match.getOffset(), match.getLength())));
						} else {
							status.addError(RefactoringCoreMessages.TargetProvider_inaccurate_match);
						}
					} else if (unit != null) {
						if (! unit.equals(fLastCU)) {
							fLastCU= unit;
							affectedCompilationUnits.add(unit);
						}
					}
				}
			};
			new SearchEngine().search(pattern, SearchUtils.getDefaultSearchParticipants(), scope, requestor, new SubProgressMonitor(pm, 1));
			return (ICompilationUnit[]) affectedCompilationUnits.toArray(new ICompilationUnit[affectedCompilationUnits.size()]);
		}

		public BodyDeclaration[] getAffectedBodyDeclarations(ICompilationUnit unit, IProgressMonitor pm) {
			ASTNode root= new RefactoringASTParser(AST.JLS3).parse(unit, true);
			InvocationFinder finder= new InvocationFinder(fMethodBinding);
			root.accept(finder);
			fCurrentBodies= finder.result;
			Set result= fCurrentBodies.keySet();
			fastDone(pm);
			return (BodyDeclaration[])result.toArray(new BodyDeclaration[result.size()]);
		}

		public ASTNode[] getInvocations(BodyDeclaration declaration, IProgressMonitor pm) {
			BodyData data= (BodyData)fCurrentBodies.get(declaration);
			Assert.isNotNull(data);
			fastDone(pm);
			return data.getInvocations();
		}

		public RefactoringStatus checkActivation() throws JavaModelException {
			return new RefactoringStatus();
		}

		public int getStatusSeverity() {
			return RefactoringStatus.ERROR;
		}
	}
}
