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

	private final Template fTemplate;
	private final TemplateContext fContext;

	// initialized by apply()	
	private int fSelectionStart;
	private int fSelectionEnd;
	
	private TemplateInterpolator fInterpolator= new TemplateInterpolator();
	private CursorSelectionEvaluator fCursorSelectionEvaluator= new CursorSelectionEvaluator();
	
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

	/**
	 * @see ICompletionProposal#apply(IDocument)
	 */
	public void apply(IDocument document) {
		int indentationLevel= guessIndentationLevel(document, fContext.getStart());

		String pattern= fTemplate.getPattern();

		// resolve variables automatically
		pattern= fInterpolator.interpolate(pattern, fContext);

		ModelEvaluator modelEvaluator= new ModelEvaluator();
		fInterpolator.interpolate(pattern, modelEvaluator);
		TemplateModel model= modelEvaluator.getModel();

		if (model.getEditableCount() == 0) {		
			pattern= fInterpolator.interpolate(pattern, fCursorSelectionEvaluator);

			// side effect: stores selection
			int cursorStart= fCursorSelectionEvaluator.getStart();
			if (cursorStart == -1) {
				fSelectionStart= pattern.length();
				fSelectionEnd= fSelectionStart;
			} else {		
				fSelectionStart= cursorStart;
				fSelectionEnd= fCursorSelectionEvaluator.getEnd();			
			}			
		} else {
			// resolve variables manually
			TemplateEditorPopup popup= new TemplateEditorPopup(fContext, model);
	        
			if (!popup.open()) {
				// leave caret offset
				fSelectionStart= fContext.getEnd() - fContext.getStart();
				fSelectionEnd= fSelectionStart;
				return;
			}

   			pattern= popup.getText();    
			
			int[] selection= popup.getSelection();
			if (selection[0] == -1) {
				fSelectionStart= pattern.length();
				fSelectionEnd= fSelectionStart;
			} else {
				fSelectionStart= selection[0];
				fSelectionEnd= selection[1];
			}
		}

		if (TemplatePreferencePage.useCodeFormatter() && fTemplate.getContext().equals("java")) { //$NON-NLS-1$
			// XXX 4360
			// workaround for code formatter limitations
			// handle a special case where cursor position is surrounded by whitespaces
			boolean kludge=
				(fSelectionStart == fSelectionEnd) &&
				(fSelectionStart > 0) && Character.isWhitespace(pattern.charAt(fSelectionStart - 1)) &&
				(fSelectionStart < pattern.length()) && Character.isWhitespace(pattern.charAt(fSelectionStart));

			final String marker= "/*${cursor}*/"; //$NON-NLS-1$

			if (kludge)
				pattern=
					pattern.substring(0, fSelectionStart) + marker +
					pattern.substring(fSelectionStart);

			CodeFormatter formatter= new CodeFormatter(JavaCore.getOptions());
			formatter.setPositionsToMap(new int[] {fSelectionStart, fSelectionEnd});
			formatter.setInitialIndentationLevel(indentationLevel);
			pattern= formatter.formatSourceString(pattern);				
			
			int[] positions= formatter.getMappedPositions();
			fSelectionStart= positions[0];
			fSelectionEnd= positions[1];		

			if (kludge)
				pattern=
					pattern.substring(0, fSelectionStart) +
					pattern.substring(fSelectionStart + marker.length());
			
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

		int start= fContext.getStart();
		int length= fContext.getEnd() - start;

		try {
			document.replace(start, length, finalString);
			fContext.getViewer().revealRange(start, finalString.length());
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
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
		return new Point(fContext.getStart() + fSelectionStart, fSelectionEnd - fSelectionStart);
	}

	/**
	 * @see ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		String pattern= fTemplate.getPattern();
		pattern= fInterpolator.interpolate(pattern, fContext);		
		pattern= fInterpolator.interpolate(pattern, fCursorSelectionEvaluator);
		
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

