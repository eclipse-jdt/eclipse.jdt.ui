package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;

import org.eclipse.jdt.internal.corext.codemanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.codemanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.TextRange;
import org.eclipse.jdt.internal.corext.refactoring.Assert;

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

