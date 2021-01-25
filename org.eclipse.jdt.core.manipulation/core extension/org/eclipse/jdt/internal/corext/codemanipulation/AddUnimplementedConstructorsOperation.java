/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Microsoft Corporation - refactored to jdt.core.manipulation
 *     Microsoft Corporation - read preferences from the compilation unit
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

/**
 * Workspace runnable to add unimplemented constructors.
 *
 * @since 3.1
 */
public final class AddUnimplementedConstructorsOperation implements IWorkspaceRunnable {

	/** Should the resulting edit be applied? */
	private final boolean fApply;

	/** The qualified names of the generated imports */
	private String[] fCreatedImports;

	/** The method binding keys for which a constructor was generated */
	private final List<String> fCreatedMethods= new ArrayList<>();

	/** Should the import edits be applied? */
	private final boolean fImports;

	/** The insertion point, or <code>-1</code> */
	private final int fInsertPos;

	/** The method bindings to implement */
	private final IMethodBinding[] fConstructorsToImplement;

	/** Should the call to the super constructor be omitted? */
	private boolean fOmitSuper;

	/** Should the compilation unit content be saved? */
	private final boolean fSave;

	/** Specified if comments should be created */
	private boolean fCreateComments;

	/** The type declaration to add the constructors to */
	private final ITypeBinding fType;

	/** The compilation unit AST node */
	private final CompilationUnit fASTRoot;

	/** The visibility flags of the new constructor */
	private int fVisibility;

	/** The formatter settings of the new constructor */
	private Map<String, String> fFormatSettings;

	/** The resulting text edit */
	private TextEdit fResultingEdit= null;

	/**
	 * Creates a new add unimplemented constructors operation.
	 *
	 * @param astRoot the compilation unit AST node
	 * @param type the type to add the methods to
	 * @param constructorsToImplement the method binding keys to implement
	 * @param insertPos the insertion point, or <code>-1</code>
	 * @param imports <code>true</code> if the import edits should be applied, <code>false</code> otherwise
	 * @param apply <code>true</code> if the resulting edit should be applied, <code>false</code> otherwise
	 * @param save <code>true</code> if the changed compilation unit should be saved, <code>false</code> otherwise
	 * @param formatSettings The settings map to use for formatting with the default code formatter. Recognized options are documented on {@link JavaCore#getDefaultOptions()}. If set to <code>null</code>, then use the current project settings {@link IJavaProject#getOptions(boolean)}.
	 */
	public AddUnimplementedConstructorsOperation(CompilationUnit astRoot, ITypeBinding type, IMethodBinding[] constructorsToImplement, int insertPos, final boolean imports, final boolean apply, final boolean save, Map<String, String> formatSettings) {
		if (astRoot == null || !(astRoot.getJavaElement() instanceof ICompilationUnit)) {
			throw new IllegalArgumentException("AST must not be null and has to be created from a ICompilationUnit"); //$NON-NLS-1$
		}
		if (type == null) {
			throw new IllegalArgumentException("The type must not be null"); //$NON-NLS-1$
		}
		ASTNode node= astRoot.findDeclaringNode(type);
		if (!(node instanceof AnonymousClassDeclaration) && !(node instanceof AbstractTypeDeclaration)) {
			throw new IllegalArgumentException("type has to map to a type declaration in the AST"); //$NON-NLS-1$
		}

		fType= type;
		fInsertPos= insertPos;
		fASTRoot= astRoot;
		fConstructorsToImplement= constructorsToImplement;
		fSave= save;
		fApply= apply;
		fImports= imports;

		fCreateComments= StubUtility.doAddComments(astRoot.getJavaElement().getJavaProject());
		fVisibility= Modifier.PUBLIC;
		fOmitSuper= false;
		fFormatSettings= formatSettings;
	}

	/**
	 * Returns the method binding keys for which a constructor has been generated.
	 *
	 * @return the method binding keys
	 */
	public String[] getCreatedConstructors() {
		final String[] keys= new String[fCreatedMethods.size()];
		fCreatedMethods.toArray(keys);
		return keys;
	}

	/**
	 * Returns the qualified names of the generated imports.
	 *
	 * @return the generated imports
	 */
	public String[] getCreatedImports() {
		return fCreatedImports;
	}

	/**
	 * Returns the scheduling rule for this operation.
	 *
	 * @return the scheduling rule
	 */
	public ISchedulingRule getSchedulingRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	/**
	 * Returns the resulting text edit.
	 *
	 * @return the resulting text edit
	 */
	public TextEdit getResultingEdit() {
		return fResultingEdit;
	}

	/**
	 * Returns the visibility of the constructors.
	 *
	 * @return the visibility
	 */
	public int getVisibility() {
		return fVisibility;
	}

	/**
	 * Returns whether the super call should be omitted.
	 *
	 * @return <code>true</code> to omit the super call, <code>false</code> otherwise
	 */
	public boolean isOmitSuper() {
		return fOmitSuper;
	}

	/**
	 * Determines whether to create comments.
	 * @param comments <code>true</code> to create comments, <code>false</code> otherwise
	 */
	public void setCreateComments(final boolean comments) {
		fCreateComments= comments;
	}

	/*
	 * @see org.eclipse.core.resources.IWorkspaceRunnable#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void run(IProgressMonitor monitor) throws CoreException {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		try {
			monitor.beginTask("", 2); //$NON-NLS-1$
			monitor.setTaskName(CodeGenerationMessages.AddUnimplementedMethodsOperation_description);
			fCreatedMethods.clear();
			ICompilationUnit cu= (ICompilationUnit) fASTRoot.getJavaElement();

			AST ast= fASTRoot.getAST();

			ASTRewrite astRewrite= ASTRewrite.create(ast);
			ImportRewrite importRewrite= StubUtility.createImportRewrite(fASTRoot, true);

			ITypeBinding currTypeBinding= fType;
			ListRewrite memberRewriter= null;

			ASTNode node= fASTRoot.findDeclaringNode(currTypeBinding);
			if (node instanceof AnonymousClassDeclaration) {
				memberRewriter= astRewrite.getListRewrite(node, AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY);
			} else if (node instanceof AbstractTypeDeclaration) {
				ChildListPropertyDescriptor property= ((AbstractTypeDeclaration) node).getBodyDeclarationsProperty();
				memberRewriter= astRewrite.getListRewrite(node, property);
			} else {
				throw new IllegalArgumentException();
				// not possible, we checked this in the constructor
			}

			final CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(cu);
			settings.createComments= fCreateComments;

			ASTNode insertion= getNodeToInsertBefore(memberRewriter);

			IMethodBinding[] toImplement= fConstructorsToImplement;
			if (toImplement == null) {
				toImplement= StubUtility2Core.getVisibleConstructors(currTypeBinding, true, true);
			}

			int deprecationCount= 0;
			for (IMethodBinding curr : toImplement) {
				if (curr.isDeprecated()) {
					deprecationCount++;
				}
			}
			boolean createDeprecated= deprecationCount == toImplement.length;
			for (IMethodBinding curr : toImplement) {
				if (!curr.isDeprecated() || createDeprecated) {
					ImportRewriteContext context= new ContextSensitiveImportRewriteContext(node, importRewrite);
					MethodDeclaration stub= StubUtility2Core.createConstructorStub(cu, astRewrite, importRewrite, context, curr, currTypeBinding.getName(), fVisibility, fOmitSuper, true, settings, fFormatSettings);
					if (stub != null) {
						fCreatedMethods.add(curr.getKey());
						if (insertion != null)
							memberRewriter.insertBefore(stub, insertion, null);
						else
							memberRewriter.insertLast(stub, null);
					}
				}
			}
			fResultingEdit= new MultiTextEdit();

			TextEdit importEdits= importRewrite.rewriteImports(new SubProgressMonitor(monitor, 1));
			fCreatedImports= importRewrite.getCreatedImports();
			if (fImports) {
				fResultingEdit.addChild(importEdits);
			}
			fResultingEdit.addChild(astRewrite.rewriteAST());

			if (fApply) {
				JavaModelUtil.applyEdit(cu, fResultingEdit, fSave, new SubProgressMonitor(monitor, 1));
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Determines whether the super call should be omitted.
	 *
	 * @param omit <code>true</code> to omit the super call, <code>false</code> otherwise
	 */
	public void setOmitSuper(final boolean omit) {
		fOmitSuper= omit;
	}

	/**
	 * Determines the visibility of the constructors.
	 *
	 * @param visibility the visibility
	 */
	public void setVisibility(final int visibility) {
		fVisibility= visibility;
	}

	private ASTNode getNodeToInsertBefore(ListRewrite rewriter) {
		if (fInsertPos != -1) {
			List<?> members= rewriter.getOriginalList();
			for (Object member : members) {
				ASTNode curr= (ASTNode) member;
				if (curr.getStartPosition() >= fInsertPos) {
					return curr;
				}
			}
		}
		return null;
	}
}
