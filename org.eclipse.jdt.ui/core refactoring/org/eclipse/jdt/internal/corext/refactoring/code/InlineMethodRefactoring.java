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
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.refactoring.util.WorkingCopyUtil;

/**
 * Open items:
 *  - generate import statements for newly generated local variable declarations.
 *  - forbid cases like foo(foo(10)) when inlining foo().
 *  - case ref.foo(); and we want to inline foo. Inline a method in a different context;
 *  - optimize code when the method to be inlined returns an argument and that one is
 *    assigned to a paramter again. No need for a separate local (important to be able
 *    to revers extract method correctly).
 */
public class InlineMethodRefactoring extends Refactoring {

	private CodeGenerationSettings fCodeGenerationSettings;
	private TextChangeManager fChangeManager;
	private SourceProvider fSourceProvider;
	private Selection fSelection;
	private MethodInvocation fInvocation;
	private boolean fSaveChanges;
	
	private static final String SOURCE= "source";

	public InlineMethodRefactoring(ICompilationUnit cu, MethodInvocation invocation, CodeGenerationSettings settings) {
		super();
		Assert.isNotNull(cu);
		Assert.isNotNull(invocation);
		Assert.isNotNull(settings);
		fCodeGenerationSettings= settings;
		invocation.setProperty(SOURCE, cu);
		fChangeManager= new TextChangeManager();
		fInvocation= invocation;
		fSaveChanges= false;
	}
	
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
		BodyDeclaration decl= (BodyDeclaration)ASTNodes.getParent(fInvocation, BodyDeclaration.class);
		if (decl instanceof FieldDeclaration) {
			result.addFatalError(
				"Can't inline call that is used inside a field initializer.", 
				JavaSourceContext.create(getCorrespondingCompilationUnit(fInvocation), fInvocation));
		}
		if (fSourceProvider == null) {
			resolveSourceContext(result);
		}
		if (result.hasFatalError())
			return result;
		if (fSourceProvider == null)
			result.addFatalError("Unable to resolve corresponding method declaration.");
		result.merge(fSourceProvider.checkActivation());
		return result;
	}

	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		fSourceProvider.initialize();
		ICompilationUnit cu= getCorrespondingCompilationUnit(fInvocation);
		CallInliner inliner= null;
		try {
			inliner= new CallInliner(cu, fSourceProvider, fCodeGenerationSettings);
			result.merge(inliner.initialize(fInvocation));
			if (!result.hasFatalError()) {
				CompilationUnitChange change= (CompilationUnitChange) fChangeManager.get(cu);
				change.setSave(fSaveChanges);
				change.setEdit(inliner.perform());
			}
			inliner.performed();
		} catch (CoreException e) {
			throw new JavaModelException(e);
		} finally {
			if (inliner != null)
				inliner.dispose();
		}
		return result;
	}

	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		return new CompositeChange("Inline Call", fChangeManager.getAllChanges());
	}
	
	private void resolveSourceContext(RefactoringStatus status) throws JavaModelException {
		// First try to find the declaration in the same AST. Use full for anonymous types, private
		// methods, ....
		ICompilationUnit cunit= getCorrespondingCompilationUnit(fInvocation);
		CompilationUnit root= (CompilationUnit)fInvocation.getRoot();
		IMethodBinding methodBinding= (IMethodBinding)fInvocation.getName().resolveBinding();
		MethodDeclaration declaration= (MethodDeclaration)root.findDeclaringNode(methodBinding);
		if (declaration == null || ASTNodes.getParent(fInvocation, ASTNode.TYPE_DECLARATION) != ASTNodes.getParent(declaration, ASTNode.TYPE_DECLARATION)) {
			status.addFatalError("Current limitation: can only inline call if method declaration and call reside in the same type.");
			return;		    	
		}
		fSourceProvider= new SourceProvider(cunit, declaration);
		/*
		IMethod method= Binding2JavaModel.find(methodBinding, cunit.getJavaProject());
		if (method != null) {
			cunit= method.getCompilationUnit();
			if (cunit == null) {
				status.addFatalError("Can't inline method since it is declared in a class file");
				return;
			}
			declaration= (MethodDeclaration)JavaElementMapper.perform(method, MethodDeclaration.class);
			if (declaration != null) {
				fSourceContext= new SourceContext(cunit, declaration);
			}
		}
		*/
	}
	
	private static IFile getFile(ICompilationUnit cu) throws CoreException {
		return (IFile)WorkingCopyUtil.getOriginal(cu).getCorrespondingResource();
	}
	
	private static ICompilationUnit getCorrespondingCompilationUnit(ASTNode node) {
		return (ICompilationUnit)node.getProperty(SOURCE);
	}
}
