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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Binding2JavaModel;
import org.eclipse.jdt.internal.corext.dom.JavaElementMapper;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusEntry;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.refactoring.util.WorkingCopyUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.MultiTextEdit;

/**
 * Open items:
 *  - generate import statements for newly generated local variable declarations.
 *  - forbid cases like foo(foo(10)) when inlining foo().
 *  - case ref.foo(); and we want to inline foo. Inline a method in a different context;
 *  - optimize code when the method to be inlined returns an argument and that one is
 *    assigned to a paramter again. No need for a separate local (important to be able
 *    to revers extract method correctly).
 */
public abstract class InlineMethodRefactoring extends Refactoring {

	SourceProvider fSourceProvider;
	CodeGenerationSettings fCodeGenerationSettings;
	TextChangeManager fChangeManager;
	boolean fSaveChanges;
	
	private static final String SOURCE= "source";

	private static class InlineCallRefactoring extends InlineMethodRefactoring {
		private ICompilationUnit fCUnit;
		private MethodInvocation fInvocation;
		public InlineCallRefactoring(ICompilationUnit cu, MethodInvocation invocation, CodeGenerationSettings settings) {
			super(false, settings);
			Assert.isNotNull(cu);
			Assert.isNotNull(invocation);
			fCUnit= cu;
			fInvocation= invocation;
		}
		protected ICompilationUnit[] getAffectedCompilationUnits(IProgressMonitor pm) {
			return new ICompilationUnit[] { fCUnit };
		}
		protected MethodInvocation[] getGroupedInvocations(ICompilationUnit unit, IProgressMonitor pm) {
			if (fInvocation != null) {
				MethodInvocation[] result= new MethodInvocation[] { fInvocation };
				fInvocation= null;
				return result;
			}
			return null;
		}
		public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
			RefactoringStatus result= new RefactoringStatus();
			checkFieldDeclaration(result, fCUnit, fInvocation, RefactoringStatus.FATAL);
			if (result.hasFatalError())
				return result;
			resolveSourceContext(result);
			if (result.hasFatalError())
				return result;
			if (fSourceProvider == null) {
				result.addFatalError("Unable to resolve corresponding method declaration.");
				return result;
			}
			result.merge(super.checkActivation(pm));
			return result;
		}
		private void resolveSourceContext(RefactoringStatus status) throws JavaModelException {
			CompilationUnit root= (CompilationUnit)fInvocation.getRoot();
			IMethodBinding methodBinding= (IMethodBinding)fInvocation.getName().resolveBinding();
			MethodDeclaration declaration= (MethodDeclaration)root.findDeclaringNode(methodBinding);
			if (declaration != null) {
				fSourceProvider= new SourceProvider(fCUnit, declaration);
				return;
			}
			IMethod method= Binding2JavaModel.find(methodBinding, fCUnit.getJavaProject());
			if (method != null) {
				ICompilationUnit source= method.getCompilationUnit();
				if (source == null) {
					status.addFatalError("Can't inline method since it is declared in a class file");
					return;
				}
				declaration= (MethodDeclaration)JavaElementMapper.perform(method, MethodDeclaration.class);
				if (declaration != null) {
					fSourceProvider= new SourceProvider(source, declaration);
				}
			}
		}
	}

	protected InlineMethodRefactoring(boolean saveChanges, CodeGenerationSettings settings) {
		super();
		Assert.isNotNull(settings);
		fCodeGenerationSettings= settings;
		fChangeManager= new TextChangeManager();
		fSaveChanges= saveChanges;
	}

	public static InlineMethodRefactoring create(ICompilationUnit unit, MethodInvocation invocation, CodeGenerationSettings settings) {
		return new InlineCallRefactoring(unit, invocation, settings);
	} 

	protected abstract ICompilationUnit[] getAffectedCompilationUnits(IProgressMonitor pm);
	
	protected abstract MethodInvocation[] getGroupedInvocations(ICompilationUnit unit, IProgressMonitor pm);
	
	public static ASTNode getTargetNode(ICompilationUnit unit, int offset, int length) {
		CompilationUnit root= AST.parseCompilationUnit(unit, true);
		ASTNode node= NodeFinder.perform(root, offset, length);
		if (node.getNodeType() == ASTNode.SIMPLE_NAME) {
			node= node.getParent();
		} else if (node.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
			node= ((ExpressionStatement)node).getExpression();
		}
		int nodeType= node.getNodeType();
		if (nodeType == ASTNode.METHOD_INVOCATION || nodeType == ASTNode.METHOD_DECLARATION) {
			return node;
		}
		return null;
	}	
	
	public String getName() {
		return "Inine Method Refactoring";
	}
	
	public void setSaveChanges(boolean save) {
		fSaveChanges= save;
	}

	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		result.merge(fSourceProvider.checkActivation());
		return result;
	}
	
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		fSourceProvider.initialize();
		ICompilationUnit[] units= getAffectedCompilationUnits(pm);
		for (int i= 0; i < units.length; i++) {
			ICompilationUnit unit= units[i];
			CallInliner inliner= null;
			try {
				MultiTextEdit root= new MultiTextEdit();
				inliner= new CallInliner(unit, fSourceProvider, fCodeGenerationSettings);
				MethodInvocation[] invocations= getGroupedInvocations(unit, pm);
				while (invocations != null) {
					for (int j= 0; j < invocations.length; j++) {
						MethodInvocation invocation= invocations[i];
						result.merge(inliner.initialize(invocation));
						if (!result.hasFatalError()) {
							root.add(inliner.perform());
						}
						inliner.performed();
					}
					invocations= getGroupedInvocations(unit, pm);
				}
				CompilationUnitChange change= (CompilationUnitChange)fChangeManager.get(unit);
				change.setSave(fSaveChanges);
				change.setEdit(root);
			} catch (CoreException e) {
				throw new JavaModelException(e);
			} finally {
				if (inliner != null)
					inliner.dispose();
			}
		}
		return result;
	}
		
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		return new CompositeChange("Inline Call", fChangeManager.getAllChanges());
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
}
