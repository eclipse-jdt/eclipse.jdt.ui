/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.Assert;
import org.eclipse.jdt.internal.core.refactoring.TextUtilities;
import org.eclipse.jdt.internal.formatter.CodeFormatter;
import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.TemplatePreferencePage;
import org.eclipse.jdt.internal.ui.refactoring.changes.TextBuffer;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * A template proposal.
 */
public class TemplateProposal implements ICompletionProposal {

	private final Template fTemplate;
	private final int fStart;
	private final int fEnd;
	
	private int fSelectionStart;
	private int fSelectionEnd;
	
	private CursorSelectionEvaluator fCursorSelectionEvaluator= new CursorSelectionEvaluator();
	private VariableEvaluator fLocalVariableEvaluator= new LocalVariableEvaluator();
	private TemplateInterpolator fInterpolator= new TemplateInterpolator();
	private ArgumentEvaluator fArgumentEvaluator;	

	/**
	 * Creates a template proposal with a template and the range of its key.
	 * @param template  the template
	 * @param arguments arguments to the template, or <code>null</code> for no arguments
	 * @param start     the starting position of the key.
	 * @param end       the ending position of the key (exclusive).
	 */	
	TemplateProposal(Template template, String[] arguments, int start, int end) {
		Assert.isNotNull(template);
		Assert.isTrue(start >= 0);
		Assert.isTrue(end >= start);
		
		fTemplate= template;
		fArgumentEvaluator= new ArgumentEvaluator(arguments);
		fStart= start;
		fEnd= end;		
	}

	/**
	 * @see ICompletionProposal#apply(IDocument)
	 */
	public void apply(IDocument document) {
		int indentationLevel= guessIndentationLevel(document, fStart);		

		String pattern= fTemplate.getPattern();

		// interpolate
		pattern= fInterpolator.interpolate(pattern, fArgumentEvaluator);		
		pattern= fInterpolator.interpolate(pattern, fLocalVariableEvaluator);
		fCursorSelectionEvaluator.reset();
		pattern= fInterpolator.interpolate(pattern, fCursorSelectionEvaluator);

		// side effect: stores selection
		int start= fCursorSelectionEvaluator.getStart();
		if (start == -1) {
			fSelectionStart= pattern.length();
			fSelectionEnd= fSelectionStart;
		} else {		
			fSelectionStart= start;
			fSelectionEnd= fCursorSelectionEvaluator.getEnd();			
		}

		boolean format= TemplatePreferencePage.useCodeFormatter();
		if (format) {
			CodeFormatter formatter= new CodeFormatter(JavaCore.getOptions());
			formatter.setPositionsToMap(new int[] {fSelectionStart, fSelectionEnd});
			formatter.setInitialIndentationLevel(indentationLevel);

			pattern= formatter.formatSourceString(pattern);
			
			int[] positions= formatter.getMappedPositions();
			fSelectionStart= positions[0];
			fSelectionEnd= positions[1];
		} else {
			CodeIndentator indentator= new CodeIndentator();
			indentator.setPositionsToMap(new int[] {fSelectionStart, fSelectionEnd});
			indentator.setIndentationLevel(indentationLevel);
			
			pattern= indentator.indentate(pattern);

			int[] positions= indentator.getMappedPositions();
			fSelectionStart= positions[0];
			fSelectionEnd= positions[1];	
		}

		// strip indentation on first line
		String finalString= trimBegin(pattern);
		int charactersRemoved= pattern.length() - finalString.length();
		fSelectionStart -= charactersRemoved;
		fSelectionEnd -= charactersRemoved;
		
		try {
			document.replace(fStart, fEnd - fStart, finalString);
		} catch (BadLocationException x) {} // ignore
	}
	
	private static String trimBegin(String string) {
		int i= 0;
		while ((i != string.length()) && Character.isWhitespace(string.charAt(i)))
			i++;
		return string.substring(i);
	}

	/**
	 * @see ICompletionProposal#getSelection(IDocument)
	 */
	public Point getSelection(IDocument document) {
		return new Point(fStart + fSelectionStart, fSelectionEnd - fSelectionStart);
	}

	/**
	 * @see ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		String pattern= fTemplate.getPattern();
		pattern= fInterpolator.interpolate(pattern, fArgumentEvaluator);		
		pattern= fInterpolator.interpolate(pattern, fLocalVariableEvaluator);
		pattern= fInterpolator.interpolate(pattern, fCursorSelectionEvaluator);
		
		return textToHTML(pattern);
	}

	/**
	 * @see ICompletionProposal#getDisplayString()
	 */
	public String getDisplayString() {
		return fTemplate.getName() + " - " + fTemplate.getDescription(); // $NON-NLS-1$
	}

	/**
	 * @see ICompletionProposal#getImage()
	 */
	public Image getImage() {
		return fTemplate.getImage();
	}

	/**
	 * @see ICompletionProposal#getContextInformation()
	 */
	public IContextInformation getContextInformation() {
		return null;
	}
	
	private static int guessIndentationLevel(IDocument document, int offset) {
		TextBuffer buffer= new TextBuffer(document);
		String line= buffer.getLineContentOfOffset(offset);
		return TextUtilities.getIndent(line, CodeFormatterPreferencePage.getTabSize());
	}

	private static String textToHTML(String string) {
		StringBuffer buffer= new StringBuffer(string.length());
		buffer.append("<pre>"); // $NON-NLS-1$
	
		for (int i= 0; i != string.length(); i++) {
			char ch= string.charAt(i);
			
			switch (ch) {
				case '&':
					buffer.append("&amp;"); // $NON-NLS-1$
					break;
					
				case '<':
					buffer.append("&lt;"); // $NON-NLS-1$
					break;

				case '>':
					buffer.append("&gt;"); // $NON-NLS-1$
					break;

				case '\t':
					buffer.append("    "); // $NON-NLS-1$
					break;

				case '\n':
					buffer.append("<br>"); // $NON-NLS-1$
					break;

				default:
					buffer.append(ch);
					break;
			}
		}

		buffer.append("</pre>"); // $NON-NLS-1$	
		return buffer.toString();
	}

}

