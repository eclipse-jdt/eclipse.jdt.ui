/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.tests.refactoring.infra;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChange;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;

import org.eclipse.jdt.internal.ui.refactoring.changes.DocumentTextBufferChange;

public class TextBufferChangeCreator implements ITextBufferChangeCreator{

	public ITextBufferChange create(String name, ICompilationUnit cu) throws JavaModelException {
		return new DocumentTextBufferChange(name, cu, new FileDocumentManager(cu));
	}
}