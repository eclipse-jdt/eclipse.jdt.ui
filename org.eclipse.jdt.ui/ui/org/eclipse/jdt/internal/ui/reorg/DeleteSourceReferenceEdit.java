package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.parser.InvalidInputException;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalSymbols;
import org.eclipse.jdt.internal.compiler.util.CharOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.codemanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.TextPosition;
import org.eclipse.jdt.internal.corext.codemanipulation.TextRegion;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.DebugUtils;

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
	public void connect(TextBuffer buffer) throws CoreException {		
		setText("");
		ISourceRange range= SourceReferenceSourceRangeComputer.computeSourceRange(fSourceReference, fCu);
		setTextPosition(new TextPosition(range.getOffset(), range.getLength()));
	}
}

