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
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringFileBuffers;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Workspace runnable to add delegate methods.
 * 
 * @since 3.1
 */
public final class AddDelegateMethodsOperation implements IWorkspaceRunnable {

	/** The method binding keys for which a method was generated */
	private final List fCreated= new ArrayList();

	/** The insertion point, or <code>null</code> */
	private final IJavaElement fInsert;

	/** The method binding keys to implement */
	private final String[] fKeys;

	/** Should the compilation unit content be saved? */
	private final boolean fSave;

	/** The code generation settings to use */
	private final CodeGenerationSettings fSettings;

	/** The type declaration to add the methods to */
	private final IType fType;

	/**
	 * Creates a new add delegate methods operation.
	 * 
	 * @param type the type to add the methods to
	 * @param insert the insertion point, or <code>null</code>
	 * @param keys the method binding keys to implement
	 * @param settings the code generation settings to use
	 * @param save <code>true</code> if the changed compilation unit should be saved,
	 *        <code>false</code> otherwise
	 */
	public AddDelegateMethodsOperation(final IType type, final IJavaElement insert, final String[] keys, final CodeGenerationSettings settings, final boolean save) {
		Assert.isNotNull(type);
		Assert.isNotNull(keys);
		Assert.isNotNull(settings);
		fType= type;
		fInsert= insert;
		fKeys= keys;
		fSettings= settings;
		fSave= save;
	}

	/**
	 * Returns the method binding keys for which a method has been generated.
	 * 
	 * @return the method binding keys
	 */
	public final String[] getCreatedMethods() {
		final String[] keys= new String[fCreated.size()];
		fCreated.toArray(keys);
		return keys;
	}

	/**
	 * Returns the scheduling rule for this operation.
	 * 
	 * @return the scheduling rule
	 */
	public final ISchedulingRule getSchedulingRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	/*
	 * @see org.eclipse.core.resources.IWorkspaceRunnable#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final void run(IProgressMonitor monitor) throws CoreException {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(CodeGenerationMessages.getString("AddDelegateMethodsOperation.monitor.message")); //$NON-NLS-1$
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
				final IBinding[][] bindings= StubUtility2.getDelegatableMethods(binding);
				if (bindings != null && bindings.length > 0) {
					try {
						final ITextFileBuffer buffer= RefactoringFileBuffers.acquire(fType.getCompilationUnit());
						ASTNode insertion= null;
						if (fInsert instanceof IMethod)
							insertion= ASTNodeSearchUtil.getMethodDeclarationNode((IMethod) fInsert, rewrite.getRoot());
						String key= null;
						MethodDeclaration stub= null;
						for (int index= 0; index < fKeys.length; index++) {
							key= fKeys[index];
							if (monitor.isCanceled())
								break;
							for (int offset= 0; offset < bindings.length; offset++) {
								if (bindings[offset][1].getKey().equals(key)) {
									stub= StubUtility2.createDelegationStub(rewrite.getCu(), rewrite.getASTRewrite(), rewrite.getImportRewrite(), rewrite.getAST(), bindings[offset], fSettings);
									if (stub != null) {
										fCreated.add(key);
										if (insertion != null)
											rewriter.insertBefore(stub, insertion, null);
										else
											rewriter.insertLast(stub, null);
									}
									break;
								}
							}
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
			}
		} finally {
			monitor.done();
		}
	}
}