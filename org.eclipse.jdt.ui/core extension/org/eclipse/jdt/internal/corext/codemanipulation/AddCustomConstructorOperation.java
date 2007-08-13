/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
import java.util.Arrays;
import java.util.List;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.Assert;
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
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringFileBuffers;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Workspace runnable to add custom constructors initializing fields.
 * 
 * @since 3.1
 */
public final class AddCustomConstructorOperation implements IWorkspaceRunnable {

	/** Should the resulting edit be applied? */
	private boolean fApply= true;

	/** The super constructor method binding */
	private final IMethodBinding fBinding;

	/** The variable bindings to implement */
	private final IVariableBinding[] fBindings;

	/** The variable binding keys for which a constructor was generated */
	private final List fCreated= new ArrayList();

	/** The resulting text edit */
	private TextEdit fEdit= null;

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

	/** The compilation unit ast node */
	private final CompilationUnit fUnit;

	/** The visibility flags of the new constructor */
	private int fVisibility= 0;

	/**
	 * Creates a new add custom constructor operation.
	 * 
	 * @param type the type to add the methods to
	 * @param insert the insertion point, or <code>null</code>
	 * @param unit the compilation unit ast node
	 * @param bindings the variable bindings to use in the constructor
	 * @param binding the method binding of the super constructor
	 * @param settings the code generation settings to use
	 * @param apply <code>true</code> if the resulting edit should be applied, <code>false</code> otherwise
	 * @param save <code>true</code> if the changed compilation unit should be saved, <code>false</code> otherwise
	 */
	public AddCustomConstructorOperation(final IType type, final IJavaElement insert, final CompilationUnit unit, final IVariableBinding[] bindings, final IMethodBinding binding, final CodeGenerationSettings settings, final boolean apply, final boolean save) {
		Assert.isNotNull(type);
		Assert.isNotNull(unit);
		Assert.isNotNull(bindings);
		Assert.isNotNull(settings);
		fType= type;
		fInsert= insert;
		fUnit= unit;
		fBindings= bindings;
		fBinding= binding;
		fSettings= settings;
		fSave= save;
		fApply= apply;
	}

	/**
	 * Returns the resulting text edit.
	 * 
	 * @return the resulting text edit
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

	/**
	 * Returns the visibility modifier of the generated constructors.
	 * 
	 * @return the visibility modifier
	 */
	public final int getVisibility() {
		return fVisibility;
	}

	/**
	 * Should the call to the super constructor be omitted?
	 * 
	 * @return <code>true</code> to omit the call, <code>false</code> otherwise
	 */
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
			monitor.setTaskName(CodeGenerationMessages.AddCustomConstructorOperation_description); 
			fCreated.clear();
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
						insertion= ASTNodes.getParent(NodeFinder.perform(rewrite.getRoot(), ((IMethod) fInsert).getNameRange()), MethodDeclaration.class);
					MethodDeclaration stub= StubUtility2.createConstructorStub(rewrite.getCu(), rewrite.getASTRewrite(), rewrite.getImportRewrite(), binding, fOmitSuper ? null : fBinding, fBindings, fVisibility, fSettings);
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
		} finally {
			monitor.done();
		}
	}

	/**
	 * Determines whether the call to the super constructor should be omitted.
	 * 
	 * @param omit <code>true</code> to omit the call, <code>false</code> otherwise
	 */
	public final void setOmitSuper(final boolean omit) {
		fOmitSuper= omit;
	}

	/**
	 * Sets the visibility modifier of the generated constructors.
	 * 
	 * @param visibility the visibility modifier
	 */
	public final void setVisibility(final int visibility) {
		fVisibility= visibility;
	}
}
