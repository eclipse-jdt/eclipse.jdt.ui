/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.javadoc;

 
import java.text.BreakIterator;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultAutoIndentStrategy;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ExtendedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.ITextEditorExtension3;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.CodeGeneration;
import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.SuperTypeHierarchyCache;
import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * Auto indent strategy for java doc comments
 */
public class JavaDocAutoIndentStrategy extends DefaultAutoIndentStrategy {

	private String fPartitioning;

	/**
	 * Creates a new Javadoc auto indent strategy for the given document partitioning.
	 * 
	 * @param partitioning the document partitioning
	 */
	public JavaDocAutoIndentStrategy(String partitioning) {
		fPartitioning= partitioning;
	}
	
	private static String getLineDelimiter(IDocument document) {
		try {
			if (document.getNumberOfLines() > 1)
				return document.getLineDelimiter(0);
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}	

		return System.getProperty("line.separator"); //$NON-NLS-1$
	}

	/**
	 * Copies the indentation of the previous line and add a star.
	 * If the javadoc just started on this line add standard method tags
	 * and close the javadoc.
	 *
	 * @param d the document to work on
	 * @param c the command to deal with
	 */
	private void jdocIndentAfterNewLine(IDocument d, DocumentCommand c) {
		
		if (c.offset == -1 || d.getLength() == 0)
			return;
			
		try {
			// find start of line
			int p= (c.offset == d.getLength() ? c.offset  - 1 : c.offset);
			IRegion info= d.getLineInformationOfOffset(p);
			int start= info.getOffset();
			
			// find white spaces
			int end= findEndOfWhiteSpace(d, start, c.offset);
			
			StringBuffer buf= new StringBuffer(c.text);
			if (end >= start) {	// 1GEYL1R: ITPJUI:ALL - java doc edit smartness not work for class comments
				// append to input
				String indentation= jdocExtractLinePrefix(d, d.getLineOfOffset(c.offset));
				buf.append(indentation);
				if (end < c.offset) {
					if (d.getChar(end) == '/') {
						// javadoc started on this line
						buf.append(" * "); //$NON-NLS-1$

						if (JavaPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_CLOSE_JAVADOCS) && isNewComment(d, c.offset, fPartitioning)) {
							String lineDelimiter= getLineDelimiter(d);

							c.doit= false;
							d.replace(c.offset, 0, lineDelimiter + indentation + " */"); //$NON-NLS-1$
							
							// evaluate method signature
							ICompilationUnit unit= getCompilationUnit();

							if (JavaPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_ADD_JAVADOC_TAGS) &&
								unit != null)
							{
								try {
									JavaModelUtil.reconcile(unit);
									String string= createJavaDocTags(d, c, indentation, lineDelimiter, unit);
									if (string != null)
										d.replace(c.offset, 0, string);						
								} catch (CoreException e) {
									// ignore
								}
							}
						}						

					}
				}			
			}
			
			c.text= buf.toString();
			
		} catch (BadLocationException excp) {
			// stop work
		}
	}

	private String createJavaDocTags(IDocument document, DocumentCommand command, String indentation, String lineDelimiter, ICompilationUnit unit)
		throws CoreException, BadLocationException
	{
		IJavaElement element= unit.getElementAt(command.offset);
		if (element == null)
			return null;

		switch (element.getElementType()) {
		case IJavaElement.TYPE:
			return createTypeTags(document, command, indentation, lineDelimiter, (IType) element);	

		case IJavaElement.METHOD:
			return createMethodTags(document, command, indentation, lineDelimiter, (IMethod) element);

		default:
			return null;
		}		
	}
	
	/*
	 * Removes start and end of a comment and corrects indentation and line delimiters.
	 */
	private String prepareTemplateComment(String comment, String indentation, String lineDelimiter) {
		//	trim comment start and end if any
		if (comment.endsWith("*/")) //$NON-NLS-1$
			comment= comment.substring(0, comment.length() - 2);
		comment= comment.trim();
		if (comment.startsWith("/*")) { //$NON-NLS-1$
			if (comment.length() > 2 && comment.charAt(2) == '*') {
				comment= comment.substring(3); // remove '/**'
			} else {
				comment= comment.substring(2); // remove '/*'
			}
		}
		return Strings.changeIndent(comment, 0, CodeFormatterUtil.getTabWidth(), indentation, lineDelimiter);
	}
	

	private String createTypeTags(IDocument document, DocumentCommand command, String indentation, String lineDelimiter, IType type)
		throws CoreException
	{
		String comment= CodeGeneration.getTypeComment(type.getCompilationUnit(), type.getTypeQualifiedName('.'), lineDelimiter);
		if (comment != null) {
			return prepareTemplateComment(comment.trim(), indentation, lineDelimiter);
		}
		return null;
	}
	
	private String createMethodTags(IDocument document, DocumentCommand command, String indentation, String lineDelimiter, IMethod method)
		throws CoreException, BadLocationException
	{
		IRegion partition= TextUtilities.getPartition(document, fPartitioning, command.offset);
		ISourceRange sourceRange= method.getSourceRange();
		if (sourceRange == null || sourceRange.getOffset() != partition.getOffset())
			return null;
			
		IMethod inheritedMethod= getInheritedMethod(method);
		String comment= CodeGeneration.getMethodComment(method, inheritedMethod, lineDelimiter);
		if (comment != null) {
			comment= comment.trim();
			boolean javadocComment= comment.startsWith("/**"); //$NON-NLS-1$
			boolean isJavaDoc= partition.getLength() >= 3 && document.get(partition.getOffset(), 3).equals("/**"); //$NON-NLS-1$
			if (javadocComment == isJavaDoc) {
				return prepareTemplateComment(comment, indentation, lineDelimiter);
			}
		}
		return null;
	}

	/**
	 * Returns the method inherited from, <code>null</code> if method is newly defined.
	 */
	private static IMethod getInheritedMethod(IMethod method) throws JavaModelException {
		IType declaringType= method.getDeclaringType();
		ITypeHierarchy typeHierarchy= SuperTypeHierarchyCache.getTypeHierarchy(declaringType);
		return JavaModelUtil.findMethodDeclarationInHierarchy(typeHierarchy, declaringType,
			method.getElementName(), method.getParameterTypes(), method.isConstructor());
	}
	
	protected void jdocIndentForCommentEnd(IDocument d, DocumentCommand c) {
		if (c.offset < 2 || d.getLength() == 0) {
			return;
		}
		try {
			if ("* ".equals(d.get(c.offset - 2, 2))) { //$NON-NLS-1$
				// modify document command
				c.length++;
				c.offset--;
			}					
		} catch (BadLocationException excp) {
			// stop work
		}
	}

	/**
	 * Guesses if the command operates within a newly created javadoc comment or not.
	 * If in doubt, it will assume that the javadoc is new.
	 */
	private static boolean isNewComment(IDocument document, int commandOffset, String partitioning) {
		
		try {
			int lineIndex= document.getLineOfOffset(commandOffset) + 1;
			if (lineIndex >= document.getNumberOfLines())
				return true;
			
			IRegion line= document.getLineInformation(lineIndex);
			ITypedRegion partition= TextUtilities.getPartition(document, partitioning, commandOffset);
			int partitionEnd= partition.getOffset() + partition.getLength();
			if (line.getOffset() >= partitionEnd)
				return false;
			
			if (document.getLength() == partitionEnd)
				return true; // partition goes to end of document - probably a new comment
			
			String comment= document.get(partition.getOffset(), partition.getLength());
			if (comment.indexOf("/*", 2) != -1) //$NON-NLS-1$
				return true; // enclosed another comment -> probably a new comment
			
			return false;
			
		} catch (BadLocationException e) {
			return false;
		}			
	}
	
	private boolean isSmartMode() {
		IWorkbenchPage page= JavaPlugin.getActivePage();
		if (page != null)  {
			IEditorPart part= page.getActiveEditor(); 
			if (part instanceof ITextEditorExtension3) {
				ITextEditorExtension3 extension= (ITextEditorExtension3) part;
				return extension.getInsertMode() == ITextEditorExtension3.SMART_INSERT;
			}
		}
		return false;
	}

	/*
	 * @see IAutoIndentStrategy#customizeDocumentCommand
	 */
	public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
		
		if (!isSmartMode())
			return;

		try {
			
			if (command.text != null && command.length == 0) {
				String[] lineDelimiters= document.getLegalLineDelimiters();
				int index= TextUtilities.endsWith(lineDelimiters, command.text);
				if (index > -1) {
					// ends with line delimiter
					if (lineDelimiters[index].equals(command.text))
						// just the line delimiter
						jdocIndentAfterNewLine(document, command);
					return;
				}
			}
			
			if (command.text != null && command.text.equals("/")) { //$NON-NLS-1$
				jdocIndentForCommentEnd(document, command);
				return;
			}

			ITypedRegion partition= TextUtilities.getPartition(document, fPartitioning, command.offset);
			int partitionStart= partition.getOffset();
			int partitionEnd= partition.getLength() + partitionStart;			
			
			String text= command.text;
			int offset= command.offset;
			int length= command.length;

			// partition change
			final int PREFIX_LENGTH= "/*".length(); //$NON-NLS-1$
			final int POSTFIX_LENGTH= "*/".length(); //$NON-NLS-1$
			if ((offset < partitionStart + PREFIX_LENGTH || offset + length > partitionEnd - POSTFIX_LENGTH) ||
				text != null && text.length() >= 2 && ((text.indexOf("*/") != -1) || (document.getChar(offset) == '*' && text.startsWith("/")))) //$NON-NLS-1$ //$NON-NLS-2$
				return;			

			if (command.text == null || command.text.length() == 0)
				jdocHandleBackspaceDelete(document, command);
		
			else if (command.text != null && command.length == 0 && command.text.length() > 0)
				jdocWrapParagraphOnInsert(document, command);

		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
	}

	private void flushCommand(IDocument document, DocumentCommand command) throws BadLocationException {

		if (!command.doit)
			return;
		
		document.replace(command.offset, command.length, command.text);

		command.doit= false;
		if (command.text != null)
			command.offset += command.text.length();
		command.length= 0;
		command.text= null;			
	}

	protected void jdocWrapParagraphOnInsert(IDocument document, DocumentCommand command) throws BadLocationException {

//		Assert.isTrue(command.length == 0);
//		Assert.isTrue(command.text != null && command.text.length() == 1);
		
		if (!getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_FORMAT_JAVADOCS))
			return;

		int line= document.getLineOfOffset(command.offset);
		IRegion region= document.getLineInformation(line);
		int lineOffset= region.getOffset();
		int lineLength= region.getLength();

		String lineContents= document.get(lineOffset, lineLength);						
		StringBuffer buffer= new StringBuffer(lineContents);
		int start= command.offset - lineOffset;
		int end= command.length + start; 
		buffer.replace(start, end, command.text);
		
		// handle whitespace		
		if (command.text != null && command.text.length() != 0 && command.text.trim().length() == 0) {

			String endOfLine= document.get(command.offset, lineOffset + lineLength - command.offset);

			// end of line
			if (endOfLine.length() == 0) {
				// move caret to next line
				flushCommand(document, command);

				if (isLineTooShort(document, line)) { 
					int[] caretOffset= {command.offset};
					jdocWrapParagraphFromLine(document, line, caretOffset, false);
					command.offset= caretOffset[0];
					return;
				}

				// move caret to next line if possible
				if (line < document.getNumberOfLines() - 1 && isJavaDocLine(document, line + 1)) {
					String lineDelimiter= document.getLineDelimiter(line);
					String nextLinePrefix= jdocExtractLinePrefix(document, line + 1);
					command.offset += lineDelimiter.length() + nextLinePrefix.length();
				} 
				return;

			// inside whitespace at end of line
			} else if (endOfLine.trim().length() == 0) {
				// simply insert space
				return;	
			}
		}

		// change in prefix region
		String prefix= jdocExtractLinePrefix(document, line);
		boolean wrapAlways=	command.offset >= lineOffset && command.offset <= lineOffset + prefix.length();

		// must insert the text now because it may include whitepace
		flushCommand(document, command);		

		if (wrapAlways || calculateDisplayedWidth(buffer.toString()) > getMargin() || isLineTooShort(document, line)) {
			int[] caretOffset= {command.offset};								
			jdocWrapParagraphFromLine(document, line, caretOffset, wrapAlways);

			if (!wrapAlways)	
				command.offset= caretOffset[0];
		}
	}

	/**
	 * Method jdocWrapParagraphFromLine.
	 * 
	 * @param document
	 * @param line
	 * @param always
	 */
	private void jdocWrapParagraphFromLine(IDocument document, int line, int[] caretOffset, boolean always) throws BadLocationException {

		String indent= jdocExtractLinePrefix(document, line);
		if (!always) {
			if (!indent.trim().startsWith("*")) //$NON-NLS-1$
				return;

			if (indent.trim().startsWith("*/")) //$NON-NLS-1$
				return;

			if (!isLineTooLong(document, line) && !isLineTooShort(document, line))
				return;
		}
		
		boolean caretRelativeToParagraphOffset= false;
		int caret= caretOffset[0];

		int caretLine= document.getLineOfOffset(caret);
		int lineOffset= document.getLineOffset(line);
		int paragraphOffset= lineOffset + indent.length();
		if (paragraphOffset < caret) {
			caret -= paragraphOffset;
			caretRelativeToParagraphOffset= true;
		} else {
			caret -= lineOffset;
		}

		StringBuffer buffer= new StringBuffer();
		int currentLine= line;
		while (line == currentLine || isJavaDocLine(document, currentLine)) {
			
			if (buffer.length() != 0 && !Character.isWhitespace(buffer.charAt(buffer.length() - 1))) {
				buffer.append(' ');
				if (currentLine <= caretLine) {
					// in this case caretRelativeToParagraphOffset is always true
					++caret;
				}
			}

			String string= getLineContents(document, currentLine);			
			buffer.append(string);
			currentLine++;
		}
		String paragraph= buffer.toString();
		
		if (paragraph.trim().length() == 0)
			return;

		caretOffset[0]= caretRelativeToParagraphOffset ? caret : 0;
		String delimiter= document.getLineDelimiter(0);
		String wrapped= formatParagraph(paragraph, caretOffset, indent, delimiter, getMargin());

		int beginning= document.getLineOffset(line);
		int end= document.getLineOffset(currentLine);
		document.replace(beginning, end - beginning, wrapped.toString());

		caretOffset[0]= caretRelativeToParagraphOffset ? caretOffset[0] + beginning : caret + beginning;
	}
	
	/**
	 * Line break iterator to handle whitespaces as first class citizens. 
	 */
	private static class LineBreakIterator {
		
		private final String fString;
		private final BreakIterator fIterator= BreakIterator.getLineInstance();
				
		private int fStart;
		private int fEnd;
		private int fBufferedEnd;
		
		public LineBreakIterator(String string) {
			fString= string;
			fIterator.setText(string);
		}
		
		public int first() {
			fBufferedEnd= -1;
			fStart= fIterator.first();
			return fStart;	
		}
		
		public int next() {
			
			if (fBufferedEnd != -1) {
				fStart= fEnd;
				fEnd= fBufferedEnd;
				fBufferedEnd= -1;
				return fEnd;
			}

			fStart= fEnd;
			fEnd= fIterator.next();

			if (fEnd == BreakIterator.DONE)
				return fEnd;

			final String string= fString.substring(fStart, fEnd);

			// whitespace
			if (string.trim().length() == 0)
				return fEnd;

			final String word= string.trim();
			if (word.length() == string.length())
				return fEnd;
			
			// suspected whitespace
			fBufferedEnd= fEnd;
			return fStart + word.length();
		}	
	}
	
	/**
	 * Formats a paragraph, using break iterator.  
	 * 
	 * @param offset an offset within the paragraph, which will be updated with respect to formatting.
	 */
	private static String formatParagraph(String paragraph, int[] offset, String prefix, String lineDelimiter, int margin) {

		LineBreakIterator iterator= new LineBreakIterator(paragraph);

		StringBuffer paragraphBuffer= new StringBuffer();
		StringBuffer lineBuffer= new StringBuffer();
		StringBuffer whiteSpaceBuffer= new StringBuffer();
		
		int index= offset[0];
		int indexBuffer= -1;

         // line delimiter could be null
        if (lineDelimiter == null)
            lineDelimiter= ""; //$NON-NLS-1$

		for (int start= iterator.first(), end= iterator.next(); end != BreakIterator.DONE; start= end, end= iterator.next()) {

			String word= paragraph.substring(start, end);

			// word is whitespace
			if (word.trim().length() == 0) {
				whiteSpaceBuffer.append(word);

			// first word of line is always appended			
			} else if (lineBuffer.length() == 0) {
				lineBuffer.append(prefix);
				lineBuffer.append(whiteSpaceBuffer.toString());
				lineBuffer.append(word);

			} else {
				String line= lineBuffer.toString() + whiteSpaceBuffer.toString()  + word.toString();
				
				// margin exceeded 
				if (calculateDisplayedWidth(line) > margin) {
					// flush line buffer and wrap paragraph
					paragraphBuffer.append(lineBuffer.toString());
					paragraphBuffer.append(lineDelimiter);
					lineBuffer.setLength(0);
					lineBuffer.append(prefix);
					lineBuffer.append(word);

					// flush index buffer
					if (indexBuffer != -1) {
						offset[0]= indexBuffer;
						// correct for caret in whitespace at the end of line
						if (whiteSpaceBuffer.length() != 0 && index < start && index >= start - whiteSpaceBuffer.length())
							offset[0] -= (index - (start - whiteSpaceBuffer.length()));
						indexBuffer= -1;
					}

					whiteSpaceBuffer.setLength(0);

				// margin not exceeded
				} else {
					lineBuffer.append(whiteSpaceBuffer.toString());
					lineBuffer.append(word);
					whiteSpaceBuffer.setLength(0);
				}
			}

			if (index >= start && index < end) {
				indexBuffer= paragraphBuffer.length() + lineBuffer.length() + (index - start);
				if (word.trim().length() != 0)
					indexBuffer -= word.length();
			}
		}

		// flush line buffer		
		paragraphBuffer.append(lineBuffer.toString());
		paragraphBuffer.append(lineDelimiter);

		// flush index buffer
		if (indexBuffer != -1)	
			offset[0]= indexBuffer;

		// last position is not returned by break iterator
		else if (offset[0] == paragraph.length())
			offset[0]= paragraphBuffer.length() - lineDelimiter.length();
			
		return paragraphBuffer.toString();
	}

	private static IPreferenceStore getPreferenceStore() {
		return JavaPlugin.getDefault().getPreferenceStore();		
	}

	/**
	 * Returns the displayed width of a string, taking in account the displayed tab width.
	 * The result can be compared against the print margin.
	 */
	private static int calculateDisplayedWidth(String string) {

		final int tabWidth= getPreferenceStore().getInt(PreferenceConstants.EDITOR_TAB_WIDTH); 
		
		int column= 0;
		for (int i= 0; i < string.length(); i++)
			if ('\t' == string.charAt(i))
				column += tabWidth - (column % tabWidth);
			else
				column++;
				
		return column;
	}

	private String jdocExtractLinePrefix(IDocument d, int line) throws BadLocationException {

		IRegion region= d.getLineInformation(line);
		int lineOffset= region.getOffset();
		int index= findEndOfWhiteSpace(d, lineOffset, lineOffset + d.getLineLength(line));
		if (d.getChar(index) == '*') {
			index++;
			if (index != lineOffset + region.getLength() &&d.getChar(index) == ' ')
				index++;
		}
		return d.get(lineOffset, index - lineOffset);
	}

	private String getLineContents(IDocument d, int line) throws BadLocationException {
		int offset = d.getLineOffset(line);
		int length = d.getLineLength(line);
        String lineDelimiter= d.getLineDelimiter(line);
        if (lineDelimiter != null)
            length= length - lineDelimiter.length();
		String lineContents = d.get(offset, length);
		int trim = jdocExtractLinePrefix(d, line).length();
		return lineContents.substring(trim);
	}
	
	private static String getLine(IDocument document, int line) throws BadLocationException {
		IRegion region= document.getLineInformation(line);		
		return document.get(region.getOffset(), region.getLength());		
	}

	/**
	 * Returns <code>true</code> if the javadoc line is too short, <code>false</code> otherwise.
	 */
	private boolean isLineTooShort(IDocument document, int line) throws BadLocationException {

		if (!isJavaDocLine(document, line + 1))
			return false;

		String nextLine= getLineContents(document, line + 1);
		if (nextLine.trim().length() == 0)
			return false;

		return true;
	}

	/**
	 * Returns <code>true</code> if the line is too long, <code>false</code> otherwise.
	 */
	private boolean isLineTooLong(IDocument document, int line) throws BadLocationException {
		String lineContents= getLine(document, line);
		return calculateDisplayedWidth(lineContents) > getMargin();
	}

	private static int getMargin() {
		return getPreferenceStore().getInt(ExtendedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN);
	}

	private static final String[] fgInlineTags= {
		"<b>", "<i>", "<em>", "<strong>", "<code>"  //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$  //$NON-NLS-4$ //$NON-NLS-5$
	};
	
	private boolean isInlineTag(String string) {
		for (int i= 0; i < fgInlineTags.length; i++)
			if (string.startsWith(fgInlineTags[i]))
				return true;	
		return false;
	}
	
	/**
	 * returns true if the specified line is part of a paragraph and should be merged with
	 * the previous line.
	 */
	private boolean isJavaDocLine(IDocument document, int line) throws BadLocationException {

		if (document.getNumberOfLines() < line)
			return false;

		int offset= document.getLineOffset(line);
		int length= document.getLineLength(line);
		int firstChar= findEndOfWhiteSpace(document, offset, offset + length);
		length -= firstChar - offset;
		String lineContents= document.get(firstChar, length);

		String prefix= lineContents.trim();		
		if (!prefix.startsWith("*") || prefix.startsWith("*/")) //$NON-NLS-1$ //$NON-NLS-2$
			return false;
			
		lineContents= lineContents.substring(1).trim().toLowerCase();

		// preserve empty lines
		if (lineContents.length() == 0)
			return false;

		// preserve @TAGS
		if (lineContents.startsWith("@")) //$NON-NLS-1$
			return false;

		// preserve HTML tags which are not inline
		if (lineContents.startsWith("<") && !isInlineTag(lineContents)) //$NON-NLS-1$
			return false;

		return true;
	}

	protected void jdocHandleBackspaceDelete(IDocument document, DocumentCommand c) {
		
		if (!getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_FORMAT_JAVADOCS))
			return;
			
		try {
			String text= document.get(c.offset, c.length);
			int line= document.getLineOfOffset(c.offset);
			int lineOffset= document.getLineOffset(line);

			// erase line delimiter
			String lineDelimiter= document.getLineDelimiter(line);
			if (lineDelimiter != null && lineDelimiter.equals(text)) {
				
				String prefix= jdocExtractLinePrefix(document, line + 1);

				// strip prefix if any
				if (prefix.length() > 0) {
					int length= document.getLineDelimiter(line).length() + prefix.length();
					document.replace(c.offset, length, null);

					c.doit= false;
					c.length= 0;
					return;
				}

			// backspace: beginning of a javadoc line
			} else if (document.getChar(c.offset - 1) == '*' && jdocExtractLinePrefix(document, line).length() - 1 >= c.offset - lineOffset) {

				lineDelimiter= document.getLineDelimiter(line - 1);
				String prefix= jdocExtractLinePrefix(document, line);
				int length= (lineDelimiter != null ? lineDelimiter.length() : 0) + prefix.length();
				document.replace(c.offset - length + 1, length, null);

				c.doit= false;
				c.offset -= length - 1;
				c.length= 0;
				return;

			} else {
				document.replace(c.offset, c.length, null);
				c.doit= false;
				c.length= 0;
			}

		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
				
		try {
			int line= document.getLineOfOffset(c.offset);
			int lineOffset= document.getLineOffset(line);
			String prefix= jdocExtractLinePrefix(document, line);
			boolean always= c.offset > lineOffset && c.offset <= lineOffset + prefix.length();
			int[] caretOffset= {c.offset};
			jdocWrapParagraphFromLine(document, document.getLineOfOffset(c.offset), caretOffset, always);
			c.offset= caretOffset[0];

		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
	}

	/**
	 * Returns the compilation unit of the CompilationUnitEditor invoking the AutoIndentStrategy,
	 * might return <code>null</code> on error.
	 */
	private static ICompilationUnit getCompilationUnit() {

		IWorkbenchWindow window= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null)
			return null;
			
		IWorkbenchPage page= window.getActivePage();
		if (page == null)	
			return null;

		IEditorPart editor= page.getActiveEditor();
		if (editor == null)
			return null;

		IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
		ICompilationUnit unit= manager.getWorkingCopy(editor.getEditorInput());
		if (unit == null)
			return null;

		return unit;
	}
			
}
