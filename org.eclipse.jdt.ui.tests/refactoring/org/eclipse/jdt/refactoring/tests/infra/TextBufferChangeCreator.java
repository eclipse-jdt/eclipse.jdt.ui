/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.refactoring.tests.infra;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.text.ITextBufferChange;
import org.eclipse.jdt.core.refactoring.text.ITextBufferChangeCreator;

import org.eclipse.jdt.internal.ui.refactoring.changes.DocumentTextBufferChange;

public class TextBufferChangeCreator implements ITextBufferChangeCreator{

	public ITextBufferChange create(String name, ICompilationUnit cu) throws JavaModelException {
		return new DocumentTextBufferChange(name, cu, new FileDocumentManager(cu));
	}
}