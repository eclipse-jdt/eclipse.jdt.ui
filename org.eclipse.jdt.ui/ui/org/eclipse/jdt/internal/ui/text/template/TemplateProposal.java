/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

import org.eclipse.jdt.internal.core.Assert;
import org.eclipse.jdt.internal.core.refactoring.TextUtilities;
import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage;
import org.eclipse.jdt.internal.ui.refactoring.changes.TextBuffer;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.GapTextStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextStore;
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

		pattern= fInterpolator.interpolate(pattern, fArgumentEvaluator);		
		pattern= fInterpolator.interpolate(pattern, fLocalVariableEvaluator);
		
		pattern= indent(pattern, indentationLevel);

		fCursorSelectionEvaluator.reset();
		pattern= fInterpolator.interpolate(pattern, fCursorSelectionEvaluator);

		// side effect: stores selection
		int start= fCursorSelectionEvaluator.getStart();
		if (start == -1) {
			fSelectionStart= pattern.length();
			fSelectionEnd= fSelectionStart;
		} else {		
			fSelectionStart= fCursorSelectionEvaluator.getStart();
			fSelectionEnd= fCursorSelectionEvaluator.getEnd();			
		}
		
		try {
			document.replace(fStart, fEnd - fStart, pattern);
		} catch (BadLocationException x) {} // ignore
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

	private static String indent(String string, int indentationLevel) {
		StringBuffer buffer = new StringBuffer(string.length());
		
		String indentation= TextUtilities.createIndentString(indentationLevel);
		String[] lines= splitIntoLines(string);		

		buffer.append(lines[0]);
		for (int i= 1; i != lines.length; i++) {
			buffer.append(indentation);
			buffer.append(lines[i]);
		}
		
		return buffer.toString();		
	}
	
	private static String[] splitIntoLines(String text) {
		try {
			ITextStore store= new GapTextStore(0, 1);
			store.set(text);
			
			ILineTracker tracker= new DefaultLineTracker();
			tracker.set(text);
			
			int size= tracker.getNumberOfLines();
			String result[]= new String[size];
			for (int i= 0; i < size; i++) {
				String lineDelimiter= null;
				try {
					lineDelimiter= tracker.getLineDelimiter(i);
				} catch (BadLocationException e) {}
				
				IRegion region= tracker.getLineInformation(i);
				
				if (lineDelimiter == null) {
					result[i]= store.get(region.getOffset(), region.getLength());
				} else {
					result[i]= store.get(region.getOffset(), region.getLength()) +
						lineDelimiter;
				}
			}
			return result;
		} catch (BadLocationException e) {
			return null;
		}
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

