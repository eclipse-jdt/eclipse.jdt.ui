/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;

public final class DeleteSourceReferenceEdit extends SimpleTextEdit {

	private ISourceReference fSourceReference;
	private ICompilationUnit fCu;
	
	public static SimpleTextEdit create(ISourceReference sr, ICompilationUnit unit) throws CoreException {
		TextRange range= computeTextRange(sr, unit);
		return new DeleteSourceReferenceEdit(sr, unit, range.getOffset(), range.getLength());
	}
	
	private DeleteSourceReferenceEdit(ISourceReference sr, ICompilationUnit unit, int offset, int length) {
		super(offset, length);
		Assert.isNotNull(sr);
		fSourceReference= sr;
		Assert.isNotNull(unit);
		fCu= unit;
	}
	
	public String getText() {
		return ""; //$NON-NLS-1$
	}

	protected TextEdit doCopy() {
		TextRange range= getTextRange();
		return new DeleteSourceReferenceEdit(fSourceReference, fCu, range.getOffset(), range.getLength());
	}
	
	private static TextRange computeTextRange(ISourceReference sr, ICompilationUnit unit) throws CoreException {		
		ISourceRange range= SourceRangeComputer.computeSourceRange(sr, unit.getSource());
		return new TextRange(range);
	}
}

