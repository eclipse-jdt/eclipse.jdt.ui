/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.changes;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;

import org.eclipse.jdt.internal.core.codemanipulation.TextBuffer;
import org.eclipse.jdt.internal.core.codemanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.changes.TextChange.TextEditChange;

public class CompilationUnitChange extends TextFileChange {

	private ICompilationUnit fCUnit;

	/**
	 * Creates a new <code>CompilationUnitChange</code>.
	 * 
	 * @param name the change's name mainly used to render the change in the UI
	 * @param cunit the compilation unit this text change works on
	 */
	public CompilationUnitChange(String name, ICompilationUnit cunit) throws CoreException {
		super(name, getFile(cunit));
		fCUnit= cunit;
		Assert.isNotNull(fCUnit);
	}
	
	private static IFile getFile(ICompilationUnit cunit) throws CoreException {
		if (cunit.isWorkingCopy())
			cunit= (ICompilationUnit) cunit.getOriginalElement();
		return (IFile)cunit.getCorrespondingResource();
	}
	
	/* non java-doc
	 * Method declared in IChange.
	 */
	public Object getModifiedLanguageElement(){
		return fCUnit;
	}
	
	/**
	 * Returns the compilation unit this change works on.
	 * 
	 * @return the compilation unit this change works on
	 */
	public ICompilationUnit getCompilationUnit() {
		return fCUnit;
	}
	
	public String getCurrentContent(ISourceReference element) throws CoreException {
		TextBuffer buffer= null;
		try {
			buffer= acquireTextBuffer();
			ISourceRange range= element.getSourceRange();
			int offset= buffer.getLineInformationOfOffset(range.getOffset()).getOffset();
			int length= range.getLength() + range.getOffset() - offset;
			return buffer.getContent(offset, length);
		} finally {
			if (buffer != null)
				releaseTextBuffer(buffer);
		}
	}
	
	public String getPreviewContent(ISourceReference element, TextEditChange[] changes) throws CoreException {
		TextBuffer buffer= createTextBuffer();
		TextBufferEditor editor= new TextBufferEditor(buffer);
		for (int i= 0; i < changes.length; i++) {
			TextEditChange change= changes[i];
			Assert.isTrue(change.getTextChange() == this);
			if (change.isActive())
				editor.addTextEdit(change.getTextEdit().copy());
		}
		int oldLength= buffer.getLength();
		editor.performEdits(new NullProgressMonitor());
		int delta= buffer.getLength() - oldLength;
		ISourceRange range= element.getSourceRange();
		int offset= buffer.getLineInformationOfOffset(range.getOffset()).getOffset();
		int length= range.getLength() + range.getOffset() - offset + delta;
		return buffer.getContent(offset, length);
	}
}

