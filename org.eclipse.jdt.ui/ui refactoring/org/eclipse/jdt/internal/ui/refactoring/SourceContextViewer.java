/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import org.eclipse.jdt.core.ISourceRange;

class SourceContextViewer  extends SourceViewer implements IErrorContextViewer {

	public static class SourceContextInput {
		public IDocument document;
		public SourceViewerConfiguration configuration;
		public ISourceRange sourceRange;
		public SourceContextInput(IDocument d, SourceViewerConfiguration c, ISourceRange r) {
			document= d;
			configuration= c;
			sourceRange= r;
		}
	}

	SourceContextViewer(Composite parent) {
		super(parent, null, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
		setEditable(false);
	}

	/* non Java-doc
	 * @see IErrorContextViewer
	 */	
	public void setInput(Object input) {
		if (input instanceof SourceContextInput) {
			SourceContextInput scinput= (SourceContextInput)input;
			configure(scinput.configuration);
			super.setInput(scinput.document);
			if (scinput.sourceRange != null) {
				int offset= scinput.sourceRange.getOffset();
				int length= scinput.sourceRange.getLength();
				if (offset >= 0 && length >= 0) {
					setSelectedRange(offset, length);
					revealRange(offset, length);
				}
			}
		} else {
			super.setInput(input);
		}
	}	
}

