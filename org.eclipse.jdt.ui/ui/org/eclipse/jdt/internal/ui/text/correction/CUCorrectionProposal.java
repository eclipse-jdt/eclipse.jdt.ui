/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange.EditChange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;


public class CUCorrectionProposal extends ChangeCorrectionProposal {

	private ProblemPosition fProblemPosition;
	private CompilationUnitChange fCompilationUnitChange;
	private ICompilationUnit fCompilationUnit;

	public CUCorrectionProposal(String name, ProblemPosition problemPos) throws CoreException {
		super(name, problemPos);
		fCompilationUnit= problemPos.getCompilationUnit();
		fCompilationUnitChange= null;
	}
	
	/*
	 * @see ChangeCorrectionProposal#getChange()
	 */
	protected Change getChange() throws CoreException {
		if (fCompilationUnitChange == null) {
			fCompilationUnitChange= new CompilationUnitChange(getDisplayString(), fCompilationUnit);
			fCompilationUnitChange.setTrackPositionChanges(true);
			fCompilationUnitChange.setSave(false);
			
			addEdits(fCompilationUnitChange);
		}
		return fCompilationUnitChange;
	}
	
	protected void addEdits(CompilationUnitChange change) throws CoreException {
	}
	
	
	/*
	 * @see ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		StringBuffer buf= new StringBuffer();
		
		try {
			CompilationUnitChange change= getCompilationUnitChange();

			TextBuffer textBuffer= change.getPreviewTextBuffer();

			EditChange[] changes= change.getTextEditChanges();		
			for (int i= 0; i < changes.length; i++) {
				TextRange range= change.getNewTextRange(changes[i]);
				buf.append("<p>...</p>");
				appendContent(textBuffer, range, 1, buf);
			}
			if (changes.length > 0) {
				buf.append("<p>...</p>");
			}
			
		} catch(CoreException e) {
			JavaPlugin.log(e);
		}
		return buf.toString();
	}

	private void appendContent(TextBuffer buffer, TextRange range, int surroundingLines, StringBuffer buf) {
		int startLine= Math.max(buffer.getLineOfOffset(range.getOffset()) - surroundingLines, 0);
		int endLine= Math.min(buffer.getLineOfOffset(range.getInclusiveEnd()) + surroundingLines, buffer.getNumberOfLines() - 1);
		
		int rangeStart= range.getOffset();
		int rangeEnd= rangeStart + range.getLength();
		for (int i= startLine; i <= endLine; i++) {
			buf.append("<p>");
			TextRegion lineInfo= buffer.getLineInformation(i);
			int start= lineInfo.getOffset();
			int end= lineInfo.getOffset() + lineInfo.getLength();
			if (rangeStart >= start && rangeStart <= end) {
				buf.append(buffer.getContent(start, rangeStart - start));
				buf.append("<b>");
				start= rangeStart;
			}
			if (rangeEnd >= start && rangeEnd <= end) {
				buf.append(buffer.getContent(start, rangeEnd - start));
				buf.append("</b>");
				start= rangeEnd;
			}
			buf.append(buffer.getContent(start, end - start));
			buf.append("</p>");
		}
	}		
		
	/*
	 * @see ICompletionProposal#getImage()
	 */
	public Image getImage() {
		return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_IMPCONT);
	}

	/**
	 * Gets the compilation unit.
	 * @return Returns a ICompilationUnit
	 */
	public ICompilationUnit getCompilationUnit() {
		return fCompilationUnit;
	}

	/**
	 * Gets the compilationUnitChange.
	 * @return Returns a CompilationUnitChange
	 */
	public CompilationUnitChange getCompilationUnitChange() throws CoreException {
		return (CompilationUnitChange) getChange();
	}


}
