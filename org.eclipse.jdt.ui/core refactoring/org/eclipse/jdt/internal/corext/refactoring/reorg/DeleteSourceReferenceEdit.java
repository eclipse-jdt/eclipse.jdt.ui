package org.eclipse.jdt.internal.corext.refactoring.reorg;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceSourceRangeComputer;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;

public class DeleteSourceReferenceEdit extends SimpleTextEdit {

	private ISourceReference fSourceReference;
	private ICompilationUnit fCu;
	
	public DeleteSourceReferenceEdit(ISourceReference sr, ICompilationUnit unit){
		Assert.isNotNull(sr);
		fSourceReference= sr;
		Assert.isNotNull(unit);
		fCu= unit;
	}

	/*
	 * @see TextEdit#copy()
	 */
	public TextEdit copy() {
		return new DeleteSourceReferenceEdit(fSourceReference, fCu);
	}
	
	/* non Java-doc
	 * @see TextEdit#connect
	 */
	public void connect(TextBufferEditor editor) throws CoreException {		
		setText("");
		ISourceRange range= SourceReferenceSourceRangeComputer.computeSourceRange(fSourceReference, fCu);
		setTextRange(new TextRange(range));
	}
}

