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
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEditCopier;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;

public final class DeleteSourceReferenceEdit extends SimpleTextEdit {

	private ISourceReference fSourceReference;
	private ICompilationUnit fCu;
	
	public DeleteSourceReferenceEdit(ISourceReference sr, ICompilationUnit unit){
		Assert.isNotNull(sr);
		fSourceReference= sr;
		Assert.isNotNull(unit);
		fCu= unit;
	}

	/* non Java-doc
	 * @see TextEdit#getModifiedElement
	 */
	public Object getModifiedElement() {
		try {
			IJavaElement element= fCu.getElementAt(fSourceReference.getSourceRange().getOffset());
			if (element != null)
				return element.getParent();
		} catch(JavaModelException e) {
		}
		return null;
	}
	
	/*
	 * @see TextEdit#copy0()
	 */
	protected TextEdit copy0(TextEditCopier copier) {
		return new DeleteSourceReferenceEdit(fSourceReference, fCu);
	}
	
	/* non Java-doc
	 * @see TextEdit#connect
	 */
	public void connect(TextBuffer buffer) throws CoreException {		
		setText(""); //$NON-NLS-1$
		ISourceRange range= SourceRangeComputer.computeSourceRange(fSourceReference, fCu.getSource());
		setTextRange(new TextRange(range));
		super.connect(buffer);
	}
}

