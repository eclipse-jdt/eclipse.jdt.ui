/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring.changes;

import org.eclipse.jface.util.Assert;

import org.eclipse.ui.texteditor.IDocumentProvider;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.refactoring.text.ITextBufferChange;
import org.eclipse.jdt.internal.corext.refactoring.text.ITextBufferChangeCreator;

import org.eclipse.jdt.internal.ui.util.DocumentManager;

public class DocumentTextBufferChangeCreator implements ITextBufferChangeCreator {

	private IDocumentProvider fDocumentProvider;
	
	public DocumentTextBufferChangeCreator(IDocumentProvider provider) {
		fDocumentProvider= provider;
		Assert.isNotNull(fDocumentProvider);
	}
	
	public ITextBufferChange create(String name, ICompilationUnit cunit) throws JavaModelException {
		return new DocumentTextBufferChange(name, cunit, new DocumentManager(cunit, fDocumentProvider));
	}
}