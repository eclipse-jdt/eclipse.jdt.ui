/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusEntry;

abstract class TargetProvider {

	public abstract ICompilationUnit[] getAffectedCompilationUnits(IProgressMonitor pm);
	
	public abstract BodyDeclaration[] getAffectedBodyDeclarations(ICompilationUnit unit, IProgressMonitor pm);
	
	public abstract MethodInvocation[] getInvocations(BodyDeclaration declaration, IProgressMonitor pm);
	
	public abstract RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException;
	
	public abstract RefactoringStatus checkInvocation(MethodInvocation node, IProgressMonitor pm) throws JavaModelException;

	public static TargetProvider create(ICompilationUnit cu, MethodInvocation invocation) {
		return new SingleCallTargetProvider(cu, invocation);
	}

	public static TargetProvider create(ICompilationUnit cu, MethodDeclaration declaration) {
		ITypeBinding type= declaration.resolveBinding().getDeclaringClass();
		if (type.isLocal())
			return new LocalTypeTargetProvider(cu, declaration);
		else
			return new MemberTypeTargetProvider(cu, declaration);
	}

	static void checkFieldDeclaration(RefactoringStatus result, ICompilationUnit unit, MethodInvocation invocation, int severity) {
		BodyDeclaration decl= (BodyDeclaration)ASTNodes.getParent(invocation, BodyDeclaration.class);
		if (decl instanceof FieldDeclaration) {
			result.addEntry(new RefactoringStatusEntry(
				"Can't inline call that is used inside a field initializer.",
				severity, 
				JavaSourceContext.create(unit, invocation)));
		}
	}
	
	static class SingleCallTargetProvider extends TargetProvider {
		private ICompilationUnit fCUnit;
		private MethodInvocation fInvocation;
		private SourceProvider fSourceProvider;
		public SingleCallTargetProvider(ICompilationUnit cu, MethodInvocation invocation) {
			Assert.isNotNull(cu);
			Assert.isNotNull(invocation);
			fCUnit= cu;
			fInvocation= invocation;
		}
		public ICompilationUnit[] getAffectedCompilationUnits(IProgressMonitor pm) {
			return new ICompilationUnit[] { fCUnit };
		}
		public BodyDeclaration[] getAffectedBodyDeclarations(ICompilationUnit unit, IProgressMonitor pm) {
			Assert.isTrue(unit == fCUnit);
			if (fInvocation == null)
				return new BodyDeclaration[0];
			return new BodyDeclaration[] { 
				(BodyDeclaration)ASTNodes.getParent(fInvocation, BodyDeclaration.class)
			};
		}
	
		public MethodInvocation[] getInvocations(BodyDeclaration declaration, IProgressMonitor pm) {
			if (fInvocation != null) {
				MethodInvocation[] result= new MethodInvocation[] { fInvocation };
				fInvocation= null;
				return result;
			}
			return null;
		}
		public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
			return new RefactoringStatus();
		}
		public RefactoringStatus checkInvocation(MethodInvocation node, IProgressMonitor pm) throws JavaModelException {
			RefactoringStatus result= new RefactoringStatus();
			checkFieldDeclaration(result, fCUnit, node, RefactoringStatus.FATAL);
			return result;
		}
	}

	private static final String INVOCATIONS= TargetProvider.class.getName() + ".invocations";
	
	private static class InvocationFinder extends ASTVisitor {
		List result= new ArrayList(2);
		Stack fBodies= new Stack();
		BodyDeclaration fCurrent;
		private IMethodBinding fBinding;
		public InvocationFinder(IMethodBinding binding) {
			fBinding= binding;
		}
		public boolean visit(MethodInvocation node) {
			if (node.getName().resolveBinding() == fBinding && fCurrent != null) {
				List inv= (List)fCurrent.getProperty(INVOCATIONS);
				if (inv != null) {
					inv.add(node);
				} else {
					inv= new ArrayList(2);
					inv.add(node);
					fCurrent.setProperty(INVOCATIONS, inv);
				}
			}
			return true;
		}
		public boolean visit(TypeDeclaration node) {
			fBodies.add(fCurrent);
			fCurrent= null;
			return true;
		}
		public void endVisit(TypeDeclaration node) {
			fCurrent= (BodyDeclaration)fBodies.remove(fBodies.size() - 1);
		}
		public boolean visit(FieldDeclaration node) {
			fBodies.add(fCurrent);
			fCurrent= null;
			return false;
		}
		public void endVisit(FieldDeclaration node) {
			fCurrent= (BodyDeclaration)fBodies.remove(fBodies.size() - 1);
		}
		public boolean visit(MethodDeclaration node) {
			fBodies.add(fCurrent);
			fCurrent= node;
			return true;
		}
		public void endVisit(MethodDeclaration node) {
			Object inv= node.getProperty(INVOCATIONS);
			if (inv != null) {
				result.add(fCurrent);
			}
			fCurrent= (BodyDeclaration)fBodies.remove(fBodies.size() - 1);
			
		}
		public boolean visit(Initializer node) {
			fBodies.add(fCurrent);
			fCurrent= node;
			return true;
		}
		public void endVisit(Initializer node) {
			Object inv= node.getProperty(INVOCATIONS);
			if (inv != null) {
				result.add(fCurrent);
			}
			fCurrent= (BodyDeclaration)fBodies.remove(fBodies.size() - 1);
		}
	}
	
	private static class LocalTypeTargetProvider extends TargetProvider {
		private ICompilationUnit fCUnit;
		private MethodDeclaration fDeclaration;
		private List fBodies;
		private int fIndex;
		public LocalTypeTargetProvider(ICompilationUnit unit, MethodDeclaration declaration) {
			Assert.isNotNull(unit);
			Assert.isNotNull(declaration);
			fCUnit= unit;
			fDeclaration= declaration;
		}
		public ICompilationUnit[] getAffectedCompilationUnits(IProgressMonitor pm) {
			fIndex= 0;
			InvocationFinder finder= new InvocationFinder(fDeclaration.resolveBinding());
			ASTNode type= ASTNodes.getParent(fDeclaration, ASTNode.TYPE_DECLARATION);
			type.accept(finder);
			fBodies= finder.result;
			return new ICompilationUnit[] { fCUnit };
		}
	
		public BodyDeclaration[] getAffectedBodyDeclarations(ICompilationUnit unit, IProgressMonitor pm) {
			Assert.isTrue(unit == fCUnit);
			return (BodyDeclaration[]) fBodies.toArray(new BodyDeclaration[fBodies.size()]);
		}
	
		public MethodInvocation[] getInvocations(BodyDeclaration declaration, IProgressMonitor pm) {
			Assert.isTrue(fBodies.contains(declaration));
			List result= (List)declaration.getProperty(INVOCATIONS);
			return (MethodInvocation[]) result.toArray(new MethodInvocation[result.size()]);
		}
	
		public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
			return new RefactoringStatus();
		}
		
		public RefactoringStatus checkInvocation(MethodInvocation node, IProgressMonitor pm) throws JavaModelException {
			return new RefactoringStatus();
		}
	}
	
	private static class MemberTypeTargetProvider extends TargetProvider {
		public MemberTypeTargetProvider(ICompilationUnit unit, MethodDeclaration declaration) {
			Assert.isNotNull(unit);
			Assert.isNotNull(declaration);
		}
		public ICompilationUnit[] getAffectedCompilationUnits(IProgressMonitor pm) {
			return null;
		}
	
		public BodyDeclaration[] getAffectedBodyDeclarations(ICompilationUnit unit, IProgressMonitor pm) {
			return null;
		}
	
		public MethodInvocation[] getInvocations(BodyDeclaration declaration, IProgressMonitor pm) {
			return null;
		}
	
		public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
			return new RefactoringStatus();
		}
		
		public RefactoringStatus checkInvocation(MethodInvocation node, IProgressMonitor pm) throws JavaModelException {
			return new RefactoringStatus();
		}
	}
}
