/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dmitry Stalnov (dstalnov@fusionone.com) - contributed fixes for:
 *       o inline call that is used in a field initializer 
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=38137)
 *       o Allow 'this' constructor to be inlined  
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=38093)
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.JavaElementMapper;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;

/*
 * Open items:
 *  - generate import statements for newly generated local variable declarations.
 *  - forbid cases like foo(foo(10)) when inlining foo().
 *  - case ref.foo(); and we want to inline foo. Inline a method in a different context;
 *  - optimize code when the method to be inlined returns an argument and that one is
 *    assigned to a paramter again. No need for a separate local (important to be able
 *    to revers extract method correctly).
 */
public class InlineMethodRefactoring extends Refactoring {

	public static class Mode {
		private Mode() {
		}
		public static final Mode INLINE_ALL= new Mode();
		public static final Mode INLINE_SINGLE= new Mode();
	}

	private ICompilationUnit fInitialCUnit;
	private ASTNode fInitialNode;
	private CodeGenerationSettings fCodeGenerationSettings;
	private TextChangeManager fChangeManager;
	private SourceProvider fSourceProvider;
	private TargetProvider fTargetProvider;
	private boolean fDeleteSource;
	private Mode fCurrentMode;
	private Mode fInitialMode;

	private InlineMethodRefactoring(ICompilationUnit unit, ASTNode node, CodeGenerationSettings settings) {
		Assert.isNotNull(unit);
		Assert.isNotNull(node);
		Assert.isNotNull(settings);
		fInitialCUnit= unit;
		fInitialNode= node;
		fCodeGenerationSettings= settings;
	}

	private InlineMethodRefactoring(ICompilationUnit unit, MethodInvocation node, CodeGenerationSettings settings) {
		this(unit, (ASTNode)node, settings);
		fTargetProvider= TargetProvider.create(unit, node);
		fInitialMode= fCurrentMode= Mode.INLINE_SINGLE;
		fDeleteSource= false;
	}

	private InlineMethodRefactoring(ICompilationUnit unit, SuperMethodInvocation node, CodeGenerationSettings settings) {
		this(unit, (ASTNode)node, settings);
		fTargetProvider= TargetProvider.create(unit, node);
		fInitialMode= fCurrentMode= Mode.INLINE_SINGLE;
		fDeleteSource= false;
	}

	private InlineMethodRefactoring(ICompilationUnit unit, ConstructorInvocation node, CodeGenerationSettings settings) {
		this(unit, (ASTNode)node, settings);
		fTargetProvider= TargetProvider.create(unit, node);
		fInitialMode= fCurrentMode= Mode.INLINE_SINGLE;
		fDeleteSource= false;
	}

	private InlineMethodRefactoring(ICompilationUnit unit, MethodDeclaration node, CodeGenerationSettings settings) {
		this(unit, (ASTNode)node, settings);
		fSourceProvider= new SourceProvider(unit, node);
		fTargetProvider= TargetProvider.create(unit, node);
		fInitialMode= fCurrentMode= Mode.INLINE_ALL;
		fDeleteSource= true;
	}
	
	public static boolean isAvailable(IMethod method) throws JavaModelException {
		return Checks.isAvailable(method);		
	}
	
	public static InlineMethodRefactoring create(ICompilationUnit unit, int offset, int length, CodeGenerationSettings settings) {
		ASTNode node= getTargetNode(unit, offset, length);
		if (node == null)
			return null;
		if (node.getNodeType() == ASTNode.METHOD_INVOCATION) {
			return new InlineMethodRefactoring(unit, (MethodInvocation)node, settings);
		} else if (node.getNodeType() == ASTNode.METHOD_DECLARATION) {
			return new InlineMethodRefactoring(unit, (MethodDeclaration)node, settings);
		} else if (node.getNodeType() == ASTNode.SUPER_METHOD_INVOCATION) {
			return new InlineMethodRefactoring(unit, (SuperMethodInvocation)node, settings);
		} else if (node.getNodeType() == ASTNode.CONSTRUCTOR_INVOCATION) {
			return new InlineMethodRefactoring(unit, (ConstructorInvocation)node, settings);
		}
		return null;
	}
	
	public String getName() {
		return RefactoringCoreMessages.getString("InlineMethodRefactoring.name"); //$NON-NLS-1$
	}
	
	public boolean getDeleteSource() {
		return fDeleteSource;
	}

	public void setDeleteSource(boolean remove) {
		fDeleteSource= remove;
	}
	
	public Mode getInitialMode() {
		return fInitialMode;
	}
	
	public RefactoringStatus setCurrentMode(Mode mode) throws JavaModelException {
		Assert.isTrue(getInitialMode() == Mode.INLINE_SINGLE);
		fCurrentMode= mode;
		if (mode == Mode.INLINE_SINGLE) {
			fTargetProvider= TargetProvider.create(fInitialCUnit, (MethodInvocation)fInitialNode);
		} else {
			fTargetProvider= TargetProvider.create(
				fSourceProvider.getCompilationUnit(), fSourceProvider.getDeclaration());
		}
		return fTargetProvider.checkActivation();
	}
	
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		if (fSourceProvider == null && Invocations.isInvocation(fInitialNode)) {
			fSourceProvider= resolveSourceProvider(result, fInitialCUnit, fInitialNode);
			if (result.hasFatalError())
				return result;
		}
		fTargetProvider.setSourceProvider(fSourceProvider);
		result.merge(fSourceProvider.checkActivation());
		result.merge(fTargetProvider.checkActivation());
		return result;
	}
	
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 3); //$NON-NLS-1$
		fChangeManager= new TextChangeManager();
		RefactoringStatus result= new RefactoringStatus();
		fSourceProvider.initialize();
		fTargetProvider.initialize();
		pm.setTaskName(RefactoringCoreMessages.getString("InlineMethodRefactoring.searching")); //$NON-NLS-1$
		ICompilationUnit[] units= fTargetProvider.getAffectedCompilationUnits(new SubProgressMonitor(pm, 1));
		result.merge(Checks.validateModifiesFiles(getFilesToBeModified(units), getValidationContext()));
		if (result.hasFatalError())
			return result;
		checkOverridden(result, new SubProgressMonitor(pm, 1));
		IProgressMonitor sub= new SubProgressMonitor(pm, 1);
		sub.beginTask("", units.length * 3); //$NON-NLS-1$
		for (int c= 0; c < units.length; c++) {
			ICompilationUnit unit= units[c];
			sub.subTask(RefactoringCoreMessages.getFormattedString("InlineMethodRefactoring.processing",  unit.getElementName())); //$NON-NLS-1$
			CallInliner inliner= null;
			try {
				boolean added= false;
				MultiTextEdit root= new MultiTextEdit();
				CompilationUnitChange change= (CompilationUnitChange)fChangeManager.get(unit);
				change.setEdit(root);
				inliner= new CallInliner(unit, fSourceProvider, fCodeGenerationSettings);
				BodyDeclaration[] bodies= fTargetProvider.getAffectedBodyDeclarations(unit, new SubProgressMonitor(pm, 1));
				for (int b= 0; b < bodies.length; b++) {
					BodyDeclaration body= bodies[b];
					inliner.initialize(body);
					ASTNode[] invocations= fTargetProvider.getInvocations(body, new SubProgressMonitor(pm, 1));
					for (int i= 0; i < invocations.length; i++) {
						ASTNode invocation= invocations[i];
						result.merge(inliner.initialize(invocation, fTargetProvider.getStatusSeverity()));
						if (result.hasFatalError())
							break;
						if (result.getSeverity() < fTargetProvider.getStatusSeverity()) {
							added= true;
							TextEdit edit= inliner.perform();
							change.addTextEditGroup( 
								new TextEditGroup(RefactoringCoreMessages.getString("InlineMethodRefactoring.edit.inline"), new TextEdit[] { edit })); //$NON-NLS-1$
							root.addChild(edit);
						} else {
							fDeleteSource= false;
						}
					}
				}
				if (!added) {
					fChangeManager.remove(unit);
				} else {
					ImportRewrite rewrite= inliner.getImportEdit();
					if (!rewrite.isEmpty()) {
						TextEdit edit= rewrite.createEdit(inliner.getBuffer().getDocument());
						root.addChild(edit);
						change.addTextEditGroup(
							new TextEditGroup(RefactoringCoreMessages.getString("InlineMethodRefactoring.edit.import"), new TextEdit[] {edit})); //$NON-NLS-1$
					}
				}
			} finally {
				if (inliner != null)
					inliner.dispose();
			}
			sub.worked(1);
			if (sub.isCanceled())
				throw new OperationCanceledException();
		}
		sub.done();
		pm.done();
		return result;
	}

	public Change createChange(IProgressMonitor pm) throws CoreException {
		if (fDeleteSource && fCurrentMode == Mode.INLINE_ALL) {
			TextChange change= fChangeManager.get(fSourceProvider.getCompilationUnit());
			TextEdit delete= fSourceProvider.getDeleteEdit();
			TextEditGroup description= new TextEditGroup(
				RefactoringCoreMessages.getString("InlineMethodRefactoring.edit.delete"), new TextEdit[] { delete }); //$NON-NLS-1$
			TextEdit root= change.getEdit();
			if (root != null) {
				root.addChild(delete);
			} else {
				change.setEdit(delete);
			}
			change.addTextEditGroup(description);
		}
		return new DynamicValidationStateChange(RefactoringCoreMessages.getString("InlineMethodRefactoring.edit.inlineCall"), fChangeManager.getAllChanges()); //$NON-NLS-1$
	}
	
	private static SourceProvider resolveSourceProvider(RefactoringStatus status, ICompilationUnit unit, ASTNode invocation) throws JavaModelException {
		CompilationUnit root= (CompilationUnit)invocation.getRoot();
		IMethodBinding methodBinding= Invocations.resolveBinding(invocation);
		MethodDeclaration declaration= (MethodDeclaration)root.findDeclaringNode(methodBinding);
		if (declaration != null) {
			return new SourceProvider(unit, declaration);
		}
		IMethod method= Bindings.findMethod(methodBinding, unit.getJavaProject());
		if (method != null) {
			ICompilationUnit source= method.getCompilationUnit();
			if (source == null) {
				status.addFatalError(RefactoringCoreMessages.getFormattedString("InlineMethodRefactoring.error.classFile", method.getElementName())); //$NON-NLS-1$
				return null;
			}
			
			declaration= (MethodDeclaration)JavaElementMapper.perform(method, MethodDeclaration.class);
			if (declaration != null) {
				return new SourceProvider(source, declaration);
			}
		}
		status.addFatalError(RefactoringCoreMessages.getString("InlineMethodRefactoring.error.noMethodDeclaration")); //$NON-NLS-1$
		return null;
	}
	
	private static ASTNode getTargetNode(ICompilationUnit unit, int offset, int length) {
		CompilationUnit root= new RefactoringASTParser(AST.JLS2).parse(unit, true);
		ASTNode node= null;
		try {
			node= checkNode(NodeFinder.perform(root, offset, length, unit));
		} catch(JavaModelException e) {
			node = null;
		}
		if (node != null)
			return node;
		return checkNode(NodeFinder.perform(root, offset, length));
	}
	
	private static ASTNode checkNode(ASTNode node) {
		if (node == null)
			return null;
		if (node.getNodeType() == ASTNode.SIMPLE_NAME) {
			node= node.getParent();
		} else if (node.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
			node= ((ExpressionStatement)node).getExpression();
		}
		switch(node.getNodeType()) {
			case ASTNode.METHOD_INVOCATION:
			case ASTNode.METHOD_DECLARATION:
			case ASTNode.SUPER_METHOD_INVOCATION:
			case ASTNode.CONSTRUCTOR_INVOCATION:
				return node;
		}
		return null;
	}
	
	private IFile[] getFilesToBeModified(ICompilationUnit[] units) {
		List result= new ArrayList(units.length + 1);
		IFile file;
		for (int i= 0; i < units.length; i++) {
			file= getFile(units[i]);
			if (file != null)
				result.add(file);
		}
		file= getFile(fSourceProvider.getCompilationUnit());
		if (file != null && !result.contains(file))
			result.add(file);
		return (IFile[])result.toArray(new IFile[result.size()]);
	}
	
	private IFile getFile(ICompilationUnit unit) {
		unit= JavaModelUtil.toOriginal(unit);
		IResource resource= unit.getResource();
		if (resource != null && resource.getType() == IResource.FILE)
			return (IFile)resource;
		return null;
	}
	
	private void checkOverridden(RefactoringStatus status, IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 9); //$NON-NLS-1$
		pm.setTaskName(RefactoringCoreMessages.getString("InlineMethodRefactoring.checking.overridden")); //$NON-NLS-1$
		MethodDeclaration decl= fSourceProvider.getDeclaration();
		IMethod method= Bindings.findMethod(decl.resolveBinding(), fSourceProvider.getCompilationUnit().getJavaProject());
		if (method == null || Flags.isPrivate(method.getFlags())) {
			pm.worked(8);
			return;
		}
		IType type= method.getDeclaringType();
		ITypeHierarchy hierarchy= type.newTypeHierarchy(new SubProgressMonitor(pm, 6));
		checkSubTypes(status, method, hierarchy.getAllSubtypes(type), new SubProgressMonitor(pm, 1));
		checkSuperClasses(status, method, hierarchy.getAllSuperclasses(type), new SubProgressMonitor(pm, 1));
		checkSuperInterfaces(status, method, hierarchy.getAllSuperInterfaces(type), new SubProgressMonitor(pm, 1));
		pm.setTaskName(""); //$NON-NLS-1$
	}

	private void checkSubTypes(RefactoringStatus result, IMethod method, IType[] types, IProgressMonitor pm) {
		checkTypes(
			result, method, types, 
			"InlineMethodRefactoring.checking.overridden.error", //$NON-NLS-1$
			pm);
	}
	
	private void checkSuperClasses(RefactoringStatus result, IMethod method, IType[] types, IProgressMonitor pm) {
		checkTypes(
			result, method, types, 
			"InlineMethodRefactoring.checking.overrides.error", //$NON-NLS-1$
			pm);
	}

	private void checkSuperInterfaces(RefactoringStatus result, IMethod method, IType[] types, IProgressMonitor pm) {
		checkTypes(
			result, method, types, 
			"InlineMethodRefactoring.checking.implements.error", //$NON-NLS-1$
			pm);
	}
	private void checkTypes(RefactoringStatus result, IMethod method, IType[] types, String key, IProgressMonitor pm) {
		pm.beginTask("", types.length); //$NON-NLS-1$
		for (int i= 0; i < types.length; i++) {
			pm.worked(1);
			IMethod[] overridden= types[i].findMethods(method);
			if (overridden != null && overridden.length > 0) {
				result.addError(
					RefactoringCoreMessages.getFormattedString(key, types[i].getElementName()), 
					JavaStatusContext.create(overridden[0]));
			}
		}
	}
}
