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
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.dom.Binding2JavaModel;
import org.eclipse.jdt.internal.corext.dom.JavaElementMapper;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
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
public class InlineMethodRefactoring extends Refactoring {

	private ICompilationUnit fInitialCUnit;
	private ASTNode fInitialNode;
	private CodeGenerationSettings fCodeGenerationSettings;
	private TextChangeManager fChangeManager;
	private SourceProvider fSourceProvider;
	private TargetProvider fTargetProvider;
	private boolean fSaveChanges;
	
	private static final String SOURCE= "source";

	private InlineMethodRefactoring(ICompilationUnit unit, ASTNode node, CodeGenerationSettings settings) {
		Assert.isNotNull(unit);
		Assert.isNotNull(node);
		Assert.isNotNull(settings);
		fInitialCUnit= unit;
		fInitialNode= node;
		fCodeGenerationSettings= settings;
		fChangeManager= new TextChangeManager();
	}

	public InlineMethodRefactoring(ICompilationUnit unit, MethodInvocation node, CodeGenerationSettings settings) {
		this(unit, (ASTNode)node, settings);
		fTargetProvider= TargetProvider.create(unit, node);
		fSaveChanges= false;
	}

	public InlineMethodRefactoring(ICompilationUnit unit, MethodDeclaration node, CodeGenerationSettings settings) {
		this(unit, (ASTNode)node, settings);
		fSourceProvider= new SourceProvider(unit, node);
		fTargetProvider= TargetProvider.create(unit, node);
		fSaveChanges= true;
	}
	
	public static InlineMethodRefactoring create(ICompilationUnit unit, int offset, int length, CodeGenerationSettings settings) {
		ASTNode node= getTargetNode(unit, offset, length);
		if (node == null)
			return null;
		if (node.getNodeType() == ASTNode.METHOD_INVOCATION) {
			return new InlineMethodRefactoring(unit, (MethodInvocation)node, settings);
		} else if (node.getNodeType() == ASTNode.METHOD_DECLARATION) {
			return null;
			// return new InlineMethodRefactoring(unit, (MethodDeclaration)node, settings);
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
		if (fSourceProvider == null && fInitialNode.getNodeType() == ASTNode.METHOD_INVOCATION) {
			fSourceProvider= resolveSourceProvider(result, fInitialCUnit, (MethodInvocation)fInitialNode);
			if (result.hasFatalError())
				return result;
		}
		result.merge(fSourceProvider.checkActivation(pm));
		result.merge(fTargetProvider.checkActivation(pm));
		return result;
	}
	
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		fSourceProvider.initialize();
		ICompilationUnit[] units= fTargetProvider.getAffectedCompilationUnits(pm);
		for (int c= 0; c < units.length; c++) {
			ICompilationUnit unit= units[c];
			CallInliner inliner= null;
			try {
				MultiTextEdit root= new MultiTextEdit();
				inliner= new CallInliner(unit, fSourceProvider, fCodeGenerationSettings);
				BodyDeclaration[] bodies= fTargetProvider.getAffectedBodyDeclarations(unit, pm);
				for (int b= 0; b < bodies.length; b++) {
					MethodInvocation[] invocations= fTargetProvider.getInvocations(bodies[b], pm);
					for (int i= 0; i < invocations.length; i++) {
						MethodInvocation invocation= invocations[i];
						result.merge(fTargetProvider.checkInvocation(invocation, pm));
						if (result.hasFatalError())
							break;
						result.merge(inliner.initialize(invocation));
						if (!result.hasFatalError()) {
							try {
								root.add(inliner.perform());
							} finally {
								inliner.performed();
							}
						}
					}
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
	
	private static SourceProvider resolveSourceProvider(RefactoringStatus status, ICompilationUnit unit, MethodInvocation invocation) throws JavaModelException {
		CompilationUnit root= (CompilationUnit)invocation.getRoot();
		IMethodBinding methodBinding= (IMethodBinding)invocation.getName().resolveBinding();
		MethodDeclaration declaration= (MethodDeclaration)root.findDeclaringNode(methodBinding);
		if (declaration != null) {
			return new SourceProvider(unit, declaration);
		}
		IMethod method= Binding2JavaModel.find(methodBinding, unit.getJavaProject());
		if (method != null) {
			ICompilationUnit source= method.getCompilationUnit();
			if (source == null) {
				status.addFatalError("Can't inline method since it is declared in a class file");
				return null;
			}
			declaration= (MethodDeclaration)JavaElementMapper.perform(method, MethodDeclaration.class);
			if (declaration != null) {
				return new SourceProvider(source, declaration);
			}
		}
		status.addFatalError("Unable to resolve corresponding method declaration.");
		return null;
	}
	
	private static ASTNode getTargetNode(ICompilationUnit unit, int offset, int length) {
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
}