/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.core.Assert;
import org.eclipse.jdt.internal.core.refactoring.TextUtilities;
import org.eclipse.jdt.internal.formatter.CodeFormatter;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.TemplatePreferencePage;
import org.eclipse.jdt.internal.ui.refactoring.changes.TextBuffer;

/**
 * A template proposal.
 */
public class TemplateProposal implements ICompletionProposal {

	private static final String MARKER= "/*${cursor}*/"; //$NON-NLS-1$

	private final Template fTemplate;
	private final TemplateContext fContext;

	// initialized by apply()	
	private int fSelectionStart;
	private int fSelectionEnd;
	
	private TemplateInterpolator fInterpolator= new TemplateInterpolator();
	private ModelEvaluator fModelEvaluator= new ModelEvaluator();
	
	/**
	 * Creates a template proposal with a template and its context.
	 * @param template  the template
	 * @param context   the context in which the template was requested.
	 */	
	TemplateProposal(Template template, TemplateContext context) {
		Assert.isNotNull(template);
		Assert.isNotNull(context);
		
		fTemplate= template;
		fContext= context;
	}

	// XXX debug
/*	
	private void print(int[] positions, String message) {
		System.out.print(message + ": ");
		for (int i= 0; i != positions.length; i++)
			System.out.print(":" + positions[i]);	
		System.out.println("");
	}
*/
	/**
	 * @see ICompletionProposal#apply(IDocument)
	 */
	public void apply(IDocument document) {
		String pattern= fTemplate.getPattern();
		pattern= fInterpolator.interpolate(pattern, fContext);
		pattern= fInterpolator.interpolate(pattern, fModelEvaluator);
		TemplateModel model= fModelEvaluator.getModel();		
//		pattern= model.toString();

		int indentationLevel= guessIndentationLevel(document, fContext.getStart());
		if (TemplatePreferencePage.useCodeFormatter() && fTemplate.getContext().equals(TemplateContext.JAVA)) {
			// XXX 4360
			// workaround for code formatter limitations
			// handle a special case where cursor position is surrounded by whitespaces

			int caretOffset= model.getCaretOffset();

			boolean kludge=
				(caretOffset > 0) && Character.isWhitespace(pattern.charAt(caretOffset - 1)) &&
				(caretOffset < pattern.length()) && Character.isWhitespace(pattern.charAt(caretOffset));
				
			if (kludge)
				pattern=
					pattern.substring(0, caretOffset) + MARKER +
					pattern.substring(caretOffset);

			CodeFormatter formatter= new CodeFormatter(JavaCore.getOptions());
			formatter.setPositionsToMap(model.getPositions());
			formatter.setInitialIndentationLevel(indentationLevel);
			pattern= formatter.formatSourceString(pattern);				

			int[] positions= formatter.getMappedPositions();
			
			// XXX workaround: the code formatter does not treat positions at the end of
			// XXX the string properly
			positions[positions.length - 1]= pattern.length(); 
			
			model.update(pattern, positions);
			caretOffset= model.getCaretOffset();
			
			if (kludge) {
				pattern=
					pattern.substring(0, caretOffset) +
					pattern.substring(caretOffset + MARKER.length());
				
				positions= model.getPositions();	
				for (int i= 0; i != positions.length; i++) {
					if (positions[i] > caretOffset)
						positions[i] -= MARKER.length();
				}		
				model.update(pattern, positions);
			}
			
		} else {
			CodeIndentator indentator= new CodeIndentator();			
			indentator.setIndentationLevel(indentationLevel);			
			indentator.setPositionsToMap(model.getPositions());						
			pattern= indentator.indentate(pattern);
			model.update(pattern, indentator.getMappedPositions());
		}

		// strip indentation on first line
		String finalString= trimBegin(pattern);
		int charactersRemoved= pattern.length() - finalString.length();
		int[] positions= model.getPositions();		
		for (int i= 0; i != positions.length; i++) {
			positions[i] -= charactersRemoved;
			if (positions[i] < 0)
				positions[i]= 0;
		}
		model.update(finalString, positions);

		TemplateEditor popup= new TemplateEditor(fContext, model);
		popup.enter();
				
		fSelectionStart= popup.getSelectionStart();
		fSelectionEnd= popup.getSelectionEnd();	
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
		return new Point(fSelectionStart, fSelectionEnd - fSelectionStart);
	}

	/**
	 * @see ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		String pattern= fTemplate.getPattern();
		pattern= fInterpolator.interpolate(pattern, fContext);
		pattern= fInterpolator.interpolate(pattern, fModelEvaluator);
		
		return textToHTML(pattern);
	}

	/**
	 * @see ICompletionProposal#getDisplayString()
	 */
	public String getDisplayString() {
		return fTemplate.getName() + TemplateMessages.getString("TemplateProposal.delimiter") + fTemplate.getDescription(); // $NON-NLS-1$ //$NON-NLS-1$
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
		buffer.append("<pre>"); //$NON-NLS-1$
	
		for (int i= 0; i != string.length(); i++) {
			char ch= string.charAt(i);
			
			switch (ch) {
				case '&':
					buffer.append("&amp;"); //$NON-NLS-1$
					break;
					
				case '<':
					buffer.append("&lt;"); //$NON-NLS-1$
					break;

				case '>':
					buffer.append("&gt;"); //$NON-NLS-1$
					break;

				case '\t':
					buffer.append("    "); //$NON-NLS-1$
					break;

				case '\n':
					buffer.append("<br>"); //$NON-NLS-1$
					break;

				default:
					buffer.append(ch);
					break;
			}
		}

		buffer.append("</pre>"); //$NON-NLS-1$
		return buffer.toString();
	}

}

