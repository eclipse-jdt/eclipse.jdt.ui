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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IWorkingCopy;
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
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.textmanipulation.GroupDescription;
import org.eclipse.jdt.internal.corext.textmanipulation.MultiTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;

import org.eclipse.jdt.ui.JavaUI;

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

	public static final int INLINE_ALL= ASTNode.METHOD_DECLARATION;
	public static final int INLINE_SINGLE= ASTNode.METHOD_INVOCATION;

	private ICompilationUnit fInitialCUnit;
	private ASTNode fInitialNode;
	private CodeGenerationSettings fCodeGenerationSettings;
	private TextChangeManager fChangeManager;
	private SourceProvider fSourceProvider;
	private TargetProvider fTargetProvider;
	private boolean fSaveChanges;
	private boolean fRemoveSource;
	private int fCurrentMode;
	
	private static final String SOURCE= "source";

	private InlineMethodRefactoring(ICompilationUnit unit, ASTNode node, CodeGenerationSettings settings) {
		Assert.isNotNull(unit);
		Assert.isNotNull(node);
		Assert.isNotNull(settings);
		fInitialCUnit= unit;
		fInitialNode= node;
		fCurrentMode= node.getNodeType();
		fCodeGenerationSettings= settings;
	}

	private InlineMethodRefactoring(ICompilationUnit unit, MethodInvocation node, CodeGenerationSettings settings) {
		this(unit, (ASTNode)node, settings);
		fTargetProvider= TargetProvider.create(unit, node);
		fSaveChanges= true;
		fRemoveSource= false;
	}

	private InlineMethodRefactoring(ICompilationUnit unit, MethodDeclaration node, CodeGenerationSettings settings) {
		this(unit, (ASTNode)node, settings);
		fSourceProvider= new SourceProvider(unit, node);
		fTargetProvider= TargetProvider.create(unit, node);
		fSaveChanges= true;
		fRemoveSource= true;
	}
	
	public static InlineMethodRefactoring create(ICompilationUnit unit, int offset, int length, CodeGenerationSettings settings) {
		ASTNode node= getTargetNode(unit, offset, length);
		if (node == null)
			return null;
		if (node.getNodeType() == ASTNode.METHOD_INVOCATION) {
			return new InlineMethodRefactoring(unit, (MethodInvocation)node, settings);
		} else if (node.getNodeType() == ASTNode.METHOD_DECLARATION) {
			return new InlineMethodRefactoring(unit, (MethodDeclaration)node, settings);
		}
		return null;
	}
	
	public String getName() {
		return "Inline Method Refactoring";
	}
	
	public void setSaveChanges(boolean save) {
		fSaveChanges= save;
	}
	
	public boolean getRemoveSource() {
		return fRemoveSource;
	}

	public void setDeleteSource(boolean remove) {
		fRemoveSource= remove;
	}
	
	public int getInitialMode() {
		return fInitialNode.getNodeType();
	}
	
	public RefactoringStatus setCurrentMode(int mode) throws JavaModelException {
		Assert.isTrue(getInitialMode() == INLINE_SINGLE);
		fCurrentMode= mode;
		if (mode == INLINE_SINGLE) {
			fTargetProvider= TargetProvider.create(fInitialCUnit, (MethodInvocation)fInitialNode);
		} else {
			fTargetProvider= TargetProvider.create(
				fSourceProvider.getCompilationUnit(), fSourceProvider.getDeclaration());
		}
		return fTargetProvider.checkActivation(new NullProgressMonitor());
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
		pm.beginTask("", 2);
		fChangeManager= new TextChangeManager();
		RefactoringStatus result= new RefactoringStatus();
		fSourceProvider.initialize();
		fTargetProvider.initialize();
		ICompilationUnit[] units= fTargetProvider.getAffectedCompilationUnits(new SubProgressMonitor(pm, 1));
		IProgressMonitor sub= new SubProgressMonitor(pm, 1);
		sub.beginTask("", units.length * 3);
		for (int c= 0; c < units.length; c++) {
			ICompilationUnit unit= units[c];
			sub.subTask("Processing" + unit.getElementName());
			CallInliner inliner= null;
			try {
				MultiTextEdit root= new MultiTextEdit();
				CompilationUnitChange change= (CompilationUnitChange)fChangeManager.get(unit);
				change.setSave(fSaveChanges);
				change.setEdit(root);
				inliner= new CallInliner(unit, fSourceProvider, fCodeGenerationSettings);
				BodyDeclaration[] bodies= fTargetProvider.getAffectedBodyDeclarations(unit, new SubProgressMonitor(pm, 1));
				for (int b= 0; b < bodies.length; b++) {
					BodyDeclaration body= bodies[b];
					inliner.initialize(body);
					MethodInvocation[] invocations= fTargetProvider.getInvocations(body, new SubProgressMonitor(pm, 1));
					for (int i= 0; i < invocations.length; i++) {
						MethodInvocation invocation= invocations[i];
						result.merge(fTargetProvider.checkInvocation(invocation, pm));
						if (result.hasFatalError())
							break;
						result.merge(inliner.initialize(invocation));
						if (!result.hasFatalError()) {
							TextEdit edit= inliner.perform();
							change.addGroupDescription( 
								new GroupDescription("Inline invocation", new TextEdit[] { edit }));
							root.add(edit);
						}
					}
				}
			} catch (CoreException e) {
				throw new JavaModelException(e);
			} finally {
				if (inliner != null)
					inliner.dispose();
			}
			sub.worked(1);
		}
		sub.done();
		pm.done();
		return result;
	}
		
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		if (fRemoveSource && fCurrentMode == INLINE_ALL) {
			try {
				TextChange change= fChangeManager.get(fSourceProvider.getCompilationUnit());
				TextEdit delete= fSourceProvider.getDeleteEdit();
				GroupDescription description= new GroupDescription(
					"Delete method declaration", new TextEdit[] { delete });
				TextEdit root= change.getEdit();
				if (root != null) {
					root.add(delete);
				} else {
					change.setEdit(delete);
				}
				change.addGroupDescription(description);
			} catch (CoreException e) {
				throw new JavaModelException(e);
			}
		}
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
			if (!source.isWorkingCopy()) {
				// try to find a working copy if exists.
				// XXX: This is a layer breaker - should not access jdt.ui
				IWorkingCopy[] workingCopies= JavaUI.getSharedWorkingCopies();
				for (int i= 0; i < workingCopies.length; i++) {
					IWorkingCopy wcopy= workingCopies[i];
					if (source.equals(wcopy.getOriginalElement())) {
						source= (ICompilationUnit)wcopy;
						break;
					}
				}
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