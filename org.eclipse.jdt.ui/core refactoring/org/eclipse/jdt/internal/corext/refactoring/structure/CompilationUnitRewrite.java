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

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.dom.OldASTRewrite;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.ltk.core.refactoring.TextChange;

public class CompilationUnitRewrite {
	//TODO: add RefactoringStatus fStatus;?
	private ICompilationUnit fCu;
	private CompilationUnit fRoot;
	private List/*<TextEditGroup>*/ fTextEditGroups;
	
	private OldASTRewrite fRewrite; // lazily initialized
	private ImportRewrite fImportRewrite; // lazily initialized
	private ImportRemover fImportRemover;
	
	public CompilationUnitRewrite(ICompilationUnit cu) {
		fCu= cu;
		fRoot= new RefactoringASTParser(AST.JLS3).parse(fCu, true);
		fTextEditGroups= new ArrayList();
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
	 * @return a {@link TextChange}, or <code>null</code> for an empty change
	 * @throws CoreException when text buffer acquisition or import rewrite text edit creation fails
	 * @throws IllegalArgumentException when the ast rewrite encounters problems
	 */
	public TextChange createChange() throws CoreException {
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
				TextEdit rewriteEdit= fRewrite.rewriteAST(document, null);
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
		return fRoot;
	}
	
	/** @deprecated use {@link #getASTRewrite()} */
	public OldASTRewrite getOldRewrite() {
		if (fRewrite == null)
			fRewrite= new OldASTRewrite(fRoot);
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
			fImportRemover= new ImportRemover(fRoot);
		}
		return fImportRemover;
	}
}
