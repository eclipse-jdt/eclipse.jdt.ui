/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ltk.core.refactoring.Change;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringFileBuffers;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Workspace runnable to add unimplemented methods.
 * 
 * @since 3.1
 */
public final class AddUnimplementedMethodsOperation implements IWorkspaceRunnable {

	/** Should the resulting edit be applied? */
	private final boolean fApply;

	/** The qualified names of the generated imports */
	private String[] fCreatedImports;

	/** The method binding keys for which a method was generated */
	private final List fCreatedMethods= new ArrayList();

	/** The resulting text edit */
	private TextEdit fEdit= null;

	/** Should the import edits be applied? */
	private final boolean fImports;

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

	/** The compilation unit ast node */
	private final CompilationUnit fUnit;

	/**
	 * Creates a new add unimplemented methods operation.
	 * 
	 * @param type the type to add the methods to
	 * @param insert the insertion point, or <code>null</code>
	 * @param unit the compilation unit ast node
	 * @param keys the method binding keys to implement
	 * @param settings the code generation settings to use
	 * @param imports <code>true</code> if the import edits should be applied, <code>false</code> otherwise
	 * @param apply <code>true</code> if the resulting edit should be applied, <code>false</code> otherwise
	 * @param save <code>true</code> if the changed compilation unit should be saved, <code>false</code> otherwise
	 */
	public AddUnimplementedMethodsOperation(final IType type, final IJavaElement insert, final CompilationUnit unit, final String[] keys, final CodeGenerationSettings settings, final boolean imports, final boolean apply, final boolean save) {
		Assert.isNotNull(type);
		Assert.isNotNull(unit);
		Assert.isNotNull(keys);
		Assert.isNotNull(settings);
		fType= type;
		fInsert= insert;
		fUnit= unit;
		fKeys= keys;
		fSettings= settings;
		fSave= save;
		fApply= apply;
		fImports= imports;
	}

	/**
	 * Returns the qualified names of the generated imports.
	 * 
	 * @return the generated imports
	 */
	public final String[] getCreatedImports() {
		if (fCreatedImports != null) {
			return fCreatedImports;
		}
		return new String[0];
	}

	/**
	 * Returns the method binding keys for which a method has been generated.
	 * 
	 * @return the method binding keys
	 */
	public final String[] getCreatedMethods() {
		final String[] keys= new String[fCreatedMethods.size()];
		fCreatedMethods.toArray(keys);
		return keys;
	}

	/**
	 * Returns the resulting text edit.
	 * 
	 * @return the resulting edit
	 */
	public final TextEdit getResultingEdit() {
		return fEdit;
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
			monitor.setTaskName(CodeGenerationMessages.AddUnimplementedMethodsOperation_description);
			fCreatedMethods.clear();
			final ICompilationUnit unit= fType.getCompilationUnit();
			final CompilationUnitRewrite rewrite= new CompilationUnitRewrite(unit, fUnit);
			ITypeBinding binding= null;
			ListRewrite rewriter= null;
			if (fType.isAnonymous()) {
				final IJavaElement parent= fType.getParent();
				if (parent instanceof IField && Flags.isEnum(((IMember) parent).getFlags())) {
					final EnumConstantDeclaration constant= (EnumConstantDeclaration) NodeFinder.perform(rewrite.getRoot(), ((ISourceReference) parent).getSourceRange());
					if (constant != null) {
						final AnonymousClassDeclaration declaration= constant.getAnonymousClassDeclaration();
						if (declaration != null) {
							binding= declaration.resolveBinding();
							if (binding != null)
								rewriter= rewrite.getASTRewrite().getListRewrite(declaration, AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY);
						}
					}
				} else {
					final ClassInstanceCreation creation= (ClassInstanceCreation) ASTNodes.getParent(NodeFinder.perform(rewrite.getRoot(), fType.getNameRange()), ClassInstanceCreation.class);
					if (creation != null) {
						binding= creation.resolveTypeBinding();
						final AnonymousClassDeclaration declaration= creation.getAnonymousClassDeclaration();
						if (declaration != null)
							rewriter= rewrite.getASTRewrite().getListRewrite(declaration, AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY);
					}
				}
			} else {
				final AbstractTypeDeclaration declaration= (AbstractTypeDeclaration) ASTNodes.getParent(NodeFinder.perform(rewrite.getRoot(), fType.getNameRange()), AbstractTypeDeclaration.class);
				if (declaration != null) {
					binding= declaration.resolveBinding();
					rewriter= rewrite.getASTRewrite().getListRewrite(declaration, declaration.getBodyDeclarationsProperty());
				}
			}
			if (binding != null && rewriter != null) {
				final IMethodBinding[] bindings= StubUtility2.getOverridableMethods(rewrite.getAST(), binding, false);
				if (bindings != null && bindings.length > 0) {
					ITextFileBuffer buffer= null;
					IDocument document= null;
					try {
						if (!JavaModelUtil.isPrimary(unit))
							document= new Document(unit.getBuffer().getContents());
						else {
							buffer= RefactoringFileBuffers.acquire(unit);
							document= buffer.getDocument();
						}
						ASTNode insertion= null;
						if (fInsert instanceof IMethod)
							insertion= (MethodDeclaration) ASTNodes.getParent(NodeFinder.perform(rewrite.getRoot(), ((IMethod) fInsert).getNameRange()), MethodDeclaration.class);
						ImportRewrite imports= rewrite.getImportRewrite();
						String key= null;
						MethodDeclaration stub= null;
						for (int index= 0; index < fKeys.length; index++) {
							key= fKeys[index];
							if (monitor.isCanceled())
								break;
							for (int offset= 0; offset < bindings.length; offset++) {
								if (bindings[offset].getKey().equals(key)) {
									stub= StubUtility2.createImplementationStub(rewrite.getCu(), rewrite.getASTRewrite(), imports, rewrite.getAST(), bindings[offset], binding.getName(), fSettings, binding.isInterface());
									if (stub != null) {
										fCreatedMethods.add(key);
										if (insertion != null)
											rewriter.insertBefore(stub, insertion, null);
										else
											rewriter.insertLast(stub, null);
									}
									break;
								}
							}
						}
						imports.createEdit(document, new SubProgressMonitor(monitor, 1));
						if (!fImports)
							rewrite.clearImportRewrites();
						fCreatedImports= imports.getCreatedImports();

						final Change result= rewrite.createChange();
						if (result instanceof CompilationUnitChange) {
							final CompilationUnitChange change= (CompilationUnitChange) result;
							final TextEdit edit= change.getEdit();
							if (edit != null) {
								try {
									fEdit= edit;
									if (fApply)
										edit.apply(document, TextEdit.UPDATE_REGIONS);
									if (fSave) {
										if (buffer != null)
											buffer.commit(new SubProgressMonitor(monitor, 1), true);
										else
											unit.getBuffer().setContents(document.get());
									}
								} catch (Exception exception) {
									throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, exception.getLocalizedMessage(), exception));
								}
							}
						}
					} finally {
						if (buffer != null)
							RefactoringFileBuffers.release(unit);
					}
				}
			}
		} finally {
			monitor.done();
		}
	}
}
