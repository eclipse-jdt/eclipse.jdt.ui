package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultAutoIndentStrategy;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Auto indent strategy for java strings
 */
public class JavaStringAutoIndentStrategy extends DefaultAutoIndentStrategy {

	/**
	 * Returns whether the text ends with one of the given search strings.
	 */
	private boolean hasLineDelimiters(IDocument document, String text) throws BadLocationException {
		String[] delimiters= document.getLegalLineDelimiters();
		
		for (int i= 0; i < delimiters.length; i++)
			if (text.indexOf(delimiters[i]) != -1)
				return true;
		
		return false;
	}
	
	private String getLineIndentation(IDocument document, int offset) throws BadLocationException {

		// find start of line
		int adjustedOffset= (offset == document.getLength() ? offset  - 1 : offset);
		IRegion line= document.getLineInformationOfOffset(adjustedOffset);
		int start= line.getOffset();
					
		// find white spaces
		int end= findEndOfWhiteSpace(document, start, offset);
		
		return document.get(start, end - start);
	}

	private String getModifiedText(String string, String lineDelimiter, String indentation) throws BadLocationException {		

		String indentedLine= lineDelimiter + indentation;
		IDocument document= new Document(string);			
		StringBuffer buffer= new StringBuffer();

		IRegion line= document.getLineInformation(0);
		buffer.append(document.get(line.getOffset(), line.getLength()));
		buffer.append("\" +"); //$NON-NLS-1$

		int lineCount= document.getNumberOfLines();
		for (int i= 1; i < lineCount - 1; i++) {
			line= document.getLineInformation(i);
			buffer.append(indentedLine);
			buffer.append('\"');
			buffer.append(document.get(line.getOffset(), line.getLength()));
			buffer.append("\" +"); //$NON-NLS-1$
		}
		
		line= document.getLineInformation(lineCount - 1);
		buffer.append(indentedLine);
		buffer.append('\"');
		buffer.append(document.get(line.getOffset(), line.getLength()));

		return buffer.toString();
	}

	private void javaStringIndentAfterNewLine(IDocument document, DocumentCommand command) throws BadLocationException {

		ITypedRegion partition= document.getPartition(command.offset);
		int offset= partition.getOffset();
		int length= partition.getLength();

		if (command.offset == offset)
			return;
		
		if (command.offset == offset + length && document.getChar(offset + length - 1) == '\"')
			return;

		String[] legalLineDelimiters= document.getLegalLineDelimiters();
		String lineDelimiter= legalLineDelimiters[0];
		String indentation= getLineIndentation(document, command.offset);

		IRegion line= document.getLineInformationOfOffset(offset);
		String string= document.get(line.getOffset(), offset - line.getOffset());
		if (string.trim().length() != 0)
			indentation += String.valueOf('\t');

		command.text= getModifiedText(command.text, lineDelimiter, indentation);		
	}

	/*
	 * @see org.eclipse.jface.text.IAutoIndentStrategy#customizeDocumentCommand(IDocument, DocumentCommand)
	 */
	public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
		try {
			if (command.length != 0 || command.text == null)
				return;

			IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
				
			if (preferenceStore.getBoolean(PreferenceConstants.EDITOR_WRAP_STRINGS) &&
				hasLineDelimiters(document, command.text))
			{
				javaStringIndentAfterNewLine(document, command);
			}
				
		} catch (BadLocationException e) {
		}
	}

}
