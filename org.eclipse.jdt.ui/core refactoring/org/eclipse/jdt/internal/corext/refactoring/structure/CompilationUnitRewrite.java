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
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.dom.OldASTRewrite;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * A {@link CompilationUnitRewrite} holds all data structures that are typically
 * required for non-trivial refactorings. All getters are initialized lazily to
 * avoid lengthy processing in
 * {@link org.eclipse.ltk.core.refactoring.Refactoring#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor)}.
 * <p>
 * Bindings are resolved by default, but can be disabled with <code>setResolveBindings(false)</code>.
 * </p>
 */
public class CompilationUnitRewrite {
	//TODO: add RefactoringStatus fStatus;?
	private ICompilationUnit fCu;
	private List/*<TextEditGroup>*/ fTextEditGroups;
	
	private CompilationUnit fRoot; // lazily initialized
	private OldASTRewrite fRewrite; // lazily initialized
	private ImportRewrite fImportRewrite; // lazily initialized
	private ImportRemover fImportRemover; // lazily initialized
	private boolean fResolveBindings;
	
	public CompilationUnitRewrite(ICompilationUnit cu) {
		fCu= cu;
		fTextEditGroups= new ArrayList();
		fResolveBindings= true;
	}
	
	/**
	 * Requests that the compiler should provide binding information for the AST
	 * nodes it creates. To be effective, this method must be called before any
	 * of {@link #getRoot()},{@link #getASTRewrite()},
	 * {@link #getImportRemover()}.
	 * <p>
	 * Defaults to <b><code>true</code> </b> (do resolve bindings).
	 * 
	 * @param resolve
	 *            <code>true</code> if bindings are wanted, and
	 *            <code>false</code> if bindings are not of interest
	 * @see org.eclipse.jdt.core.dom.ASTParser#setResolveBindings(boolean)
	 *      Note: The default value (<code>true</code>) differs from the one of
	 *      the corresponding method in ASTParser.
	 */
	public void setResolveBindings(boolean resolve) {
		fResolveBindings= resolve;
	}
	
	public void clearASTRewrite() {
		if (fRewrite != null)
			fRewrite.removeModifications();
		fTextEditGroups= new ArrayList();
	}

	public void clearImportRewrites() {
		fImportRewrite= null;
	}

	public void clearASTAndImportRewrites() {
		clearASTRewrite();
		fImportRewrite= null;
	}
	
	public TextEditGroup createGroupDescription(String name) {
		TextEditGroup result= new TextEditGroup(name);
		fTextEditGroups.add(result);
		return result;
	}
	
	/**
	 * @return a {@link CompilationUnitChange}, or <code>null</code> for an empty change
	 * @throws CoreException when text buffer acquisition or import rewrite text edit creation fails
	 * @throws IllegalArgumentException when the ast rewrite encounters problems
	 */
	public CompilationUnitChange createChange() throws CoreException {
		boolean needsAstRewrite= fRewrite != null; //TODO: do we need something like ASTRewrite#hasChanges() here? 
		boolean needsImportRemoval= fImportRemover != null && fImportRemover.hasRemovedNodes();
		boolean needsImportRewrite= fImportRewrite != null && ! fImportRewrite.isEmpty();
		if (! needsAstRewrite && ! needsImportRemoval && ! needsImportRewrite)
			return null;
			
		CompilationUnitChange cuChange= new CompilationUnitChange(fCu.getElementName(), fCu);
		TextBuffer buffer= TextBuffer.acquire(getFile(fCu));
		IDocument document= buffer.getDocument();
		MultiTextEdit multiEdit= new MultiTextEdit();
		try {
			cuChange.setEdit(multiEdit);
			if (needsAstRewrite) {
				TextEdit rewriteEdit= fRewrite.rewriteAST(document, fCu.getJavaProject().getOptions(true));
				if (! isEmptyEdit(rewriteEdit)) {
					multiEdit.addChild(rewriteEdit);
					for (Iterator iter= fTextEditGroups.iterator(); iter.hasNext();) {
						cuChange.addTextEditGroup((TextEditGroup) iter.next());
					}
				}
			}
			if (needsImportRemoval) {
				ITypeBinding[] bindings= fImportRemover.getImportsToRemove();
				for (int i= 0; i < bindings.length; i++)
					getImportRewrite().removeImport(bindings[i]);
			}
			if (fImportRewrite != null && ! fImportRewrite.isEmpty()) {
				TextEdit importsEdit= fImportRewrite.createEdit(document);
				if (! isEmptyEdit(importsEdit)) {
					multiEdit.addChild(importsEdit);
					String importUpdateName= RefactoringCoreMessages.getString("ASTData.update_imports"); //$NON-NLS-1$
					cuChange.addTextEditGroup(new TextEditGroup(importUpdateName, importsEdit));
				}
			}
		} finally {
			TextBuffer.release(buffer);
		}
		if (isEmptyEdit(multiEdit))
			return null;
		return cuChange;
	}
	
	private static boolean isEmptyEdit(TextEdit edit) {
		return edit.getClass() == MultiTextEdit.class && ! edit.hasChildren();
	}
	
	private static IFile getFile(ICompilationUnit cu) {
		return (IFile) cu.getPrimary().getResource();
	}

	public ICompilationUnit getCu() {
		return fCu;
	}

	public CompilationUnit getRoot() {
		if (fRoot == null)
			fRoot= new RefactoringASTParser(AST.JLS3).parse(fCu, fResolveBindings);
		return fRoot;
	}
	
	public AST getAST() {
		return getRoot().getAST();
	}
	
	/** @deprecated use {@link #getASTRewrite()} */
	public OldASTRewrite getOldRewrite() {
		if (fRewrite == null)
			fRewrite= new OldASTRewrite(getRoot());
		return fRewrite;
	}
	
	public ASTRewrite getASTRewrite() {
		return getOldRewrite();
	}

	public ImportRewrite getImportRewrite() {
		if (fImportRewrite == null) {
			// lazily initialized to avoid lengthy processing in checkInitialConditions(..)
			try {
				fImportRewrite= new ImportRewrite(fCu);
			} catch (CoreException e) {
				JavaPlugin.log(e);
				throw new IllegalStateException(e.getMessage()); // like ASTParser#createAST(..) does
			}
		}
		return fImportRewrite;
	}
	
	public ImportRemover getImportRemover() {
		if (fImportRemover == null) {
			fImportRemover= new ImportRemover(fCu.getJavaProject(), getRoot());
		}
		return fImportRemover;
	}
}
