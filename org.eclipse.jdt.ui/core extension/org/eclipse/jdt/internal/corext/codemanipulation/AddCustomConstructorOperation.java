/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import org.eclipse.core.filebuffers.ITextFileBuffer;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ltk.core.refactoring.Change;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringFileBuffers;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Workspace runnable to add custom constructors initializing fields.
 * 
 * @since 3.1
 */
public final class AddCustomConstructorOperation implements IWorkspaceRunnable {

	/** The super constructor method binding */
	private final IMethodBinding fBinding;

	/** The variable bindings to implement */
	private final IVariableBinding[] fBindings;

	/** The variable binding keys for which a constructor was generated */
	private final List fCreated= new ArrayList();

	/** The insertion point, or <code>null</code> */
	private final IJavaElement fInsert;

	/** Should the call to the super constructor be omitted? */
	private boolean fOmitSuper= false;

	/** Should the compilation unit content be saved? */
	private final boolean fSave;

	/** The code generation settings to use */
	private final CodeGenerationSettings fSettings;

	/** The type declaration to add the constructors to */
	private final IType fType;

	/** The visibility flags of the new constructor */
	private int fVisibility= 0;

	/**
	 * Creates a new add custom constructor operation.
	 * 
	 * @param type the type to add the methods to
	 * @param insert the insertion point, or <code>null</code>
	 * @param bindings the variable bindings to use in the constructor
	 * @param binding the method binding of the super constructor
	 * @param settings the code generation settings to use
	 * @param save <code>true</code> if the changed compilation unit should be saved,
	 *        <code>false</code> otherwise
	 */
	public AddCustomConstructorOperation(final IType type, final IJavaElement insert, final IVariableBinding[] bindings, final IMethodBinding binding, final CodeGenerationSettings settings, final boolean save) {
		Assert.isNotNull(type);
		Assert.isNotNull(bindings);
		Assert.isNotNull(settings);
		fType= type;
		fInsert= insert;
		fBindings= bindings;
		fBinding= binding;
		fSettings= settings;
		fSave= save;
	}

	/**
	 * Returns the scheduling rule for this operation.
	 * 
	 * @return the scheduling rule
	 */
	public final ISchedulingRule getSchedulingRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	public final int getVisibility() {
		return fVisibility;
	}

	public final boolean isOmitSuper() {
		return fOmitSuper;
	}

	/*
	 * @see org.eclipse.core.resources.IWorkspaceRunnable#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final void run(IProgressMonitor monitor) throws CoreException {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(CodeGenerationMessages.getString("AddCustomConstructorOperation.description")); //$NON-NLS-1$
			fCreated.clear();
			final CompilationUnitRewrite rewrite= new CompilationUnitRewrite(fType.getCompilationUnit());
			ITypeBinding binding= null;
			ListRewrite rewriter= null;
			if (fType.isAnonymous()) {
				final ClassInstanceCreation creation= ASTNodeSearchUtil.getClassInstanceCreationNode(fType, rewrite.getRoot());
				if (creation != null) {
					binding= creation.resolveTypeBinding();
					final AnonymousClassDeclaration declaration= creation.getAnonymousClassDeclaration();
					if (declaration != null)
						rewriter= rewrite.getASTRewrite().getListRewrite(declaration, AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY);
				}
			} else {
				final AbstractTypeDeclaration declaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(fType, rewrite.getRoot());
				if (declaration != null) {
					binding= declaration.resolveBinding();
					rewriter= rewrite.getASTRewrite().getListRewrite(declaration, declaration.getBodyDeclarationsProperty());
				}
			}
			if (binding != null && rewriter != null) {
				try {
					final ITextFileBuffer buffer= RefactoringFileBuffers.acquire(fType.getCompilationUnit());
					ASTNode insertion= null;
					if (fInsert instanceof IMethod)
						insertion= ASTNodeSearchUtil.getMethodDeclarationNode((IMethod) fInsert, rewrite.getRoot());
					MethodDeclaration stub= StubUtility2.createConstructorStub(rewrite.getCu(), rewrite.getASTRewrite(), rewrite.getImportRewrite(), binding, rewrite.getAST(), fOmitSuper ? null : fBinding, fBindings, fVisibility, fSettings);
					if (stub != null) {
						fCreated.addAll(Arrays.asList(fBindings));
						if (insertion != null)
							rewriter.insertBefore(stub, insertion, null);
						else
							rewriter.insertLast(stub, null);
					}
					final Change result= rewrite.createChange();
					if (result instanceof CompilationUnitChange) {
						final CompilationUnitChange change= (CompilationUnitChange) result;
						final TextEdit edit= change.getEdit();
						if (edit != null) {
							try {
								edit.apply(buffer.getDocument(), TextEdit.UPDATE_REGIONS);
								if (fSave)
									buffer.commit(new SubProgressMonitor(monitor, 1), true);
							} catch (Exception exception) {
								throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, exception.getLocalizedMessage(), exception));
							}
						}
					}
				} finally {
					RefactoringFileBuffers.release(fType.getCompilationUnit());
				}
			}
		} finally {
			monitor.done();
		}
	}

	public final void setOmitSuper(final boolean omit) {
		fOmitSuper= omit;
	}

	public final void setVisibility(final int visibility) {
		fVisibility= visibility;
	}
}