package org.eclipse.jdt.internal.debug.ui.display;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
  
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposal;
import org.eclipse.jdt.internal.ui.text.java.ProposalContextInformation;
import org.eclipse.jdt.internal.ui.text.java.ProposalInfo;
import org.eclipse.jdt.internal.ui.text.java.ResultCollector;
import org.eclipse.swt.graphics.Image;

/**
 * Bin to collect the proposals of the infrastructure on code assist for
 * the display view.
 */
public class DisplayResultCollector extends ResultCollector {
	
	protected int fEditorOffset= -1;
	protected int fStart= -1;
	protected int fEnd= -1;
	
	protected Object createCompletion(int start, int end, String completion, String iconName, String name, String qualification, boolean isKeyWord, boolean placeCursorBehindInsertion, ProposalContextInformation contextInformation, IImportDeclaration importDeclaration, ProposalInfo proposalInfo) {
		return createCompletion(start, end, completion, iconName, name, qualification, placeCursorBehindInsertion, contextInformation, importDeclaration, proposalInfo);
	}
	
	protected Object createCompletion(int start, int end, String completion, String iconName, String name, String qualification, boolean placeCursorBehindInsertion, ProposalContextInformation contextInformation, IImportDeclaration importDeclaration, ProposalInfo proposalInfo) {
		if (qualification != null) {
			name += (" - " + qualification); //$NON-NLS-1$
		}
		int cursorPosition= completion == null ? 0 : completion.length();
		if (!placeCursorBehindInsertion) {
			-- cursorPosition;
		}
		int length= end - start;
		
		int adjustedStart= fStart + (start - fEditorOffset);
		if (fOffset > -1 && fLength > -1) {
			length= fLength + (fOffset - adjustedStart);
			adjustedStart= fOffset;
		}
		Image icon= getIcon(iconName);
		if (contextInformation != null) {
			contextInformation.setImage(icon);
		}
		
		return new JavaCompletionProposal(completion, adjustedStart, length, cursorPosition, icon, name, contextInformation, importDeclaration, proposalInfo);
	} 
	
	/**
	 * Sets the start
	 * @param start The start to set
	 */
	public void setStart(int start) {
		fStart = start;
	}
	
	/**
	 * Sets the end
	 * @param end The end to set
	 */
	public void setEnd(int end) {
		fEnd = end;
	}
	
	public void reset(IJavaProject jproject, ICompilationUnit cu) {
		super.reset(jproject, cu);
		fStart= -1;
		fEnd= -1;
		fEditorOffset= -1;
	}
	/**
	 * Sets the editor offset of the associated Java editor
	 * @param editorStart The editorStart to set
	 */
	public void setEditorOffset(int editorOffset) {
		fEditorOffset = editorOffset;
	}
}