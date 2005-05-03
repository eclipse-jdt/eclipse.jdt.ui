/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.source.ILineRange;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;

import org.eclipse.jdt.ui.text.IJavaPartitions;

import org.eclipse.jdt.internal.ui.text.JavaHeuristicScanner;
import org.eclipse.jdt.internal.ui.text.JavaIndenter;
import org.eclipse.jdt.internal.ui.text.javadoc.JavaDocAutoIndentStrategy;


/**
 * Utility that indents a number of lines in a document.
 * 
 * @since 3.1
 */
public final class IndentUtil {
	
	/**
	 * The result of an indentation operation. The result may be passed to
	 * subsequent calls to
	 * {@link IndentUtil#indentLines(IDocument, ILineRange, IJavaProject, IndentResult) indentLines}
	 * to obtain consistent results with respect to the indentation of
	 * line-comments.
	 */
	public static final class IndentResult {
		private IndentResult(boolean[] commentLines) {
			commentLinesAtColumnZero= commentLines;
		}
		private boolean[] commentLinesAtColumnZero;
		private boolean hasChanged;
		/**
		 * Returns <code>true</code> if the indentation operation changed the
		 * document, <code>false</code> if not.
		 * @return <code>true</code> if the document was changed
		 */
		public boolean hasChanged() {
			return hasChanged;
		}
	}
	
	private IndentUtil() {
		// do not instantiate
	}

	/**
	 * Indents the line range specified by <code>lines</code> in
	 * <code>document</code>. The passed Java project may be
	 * <code>null</code>, it is used solely to obtain formatter preferences.
	 * 
	 * @param document the document to be changed
	 * @param lines the line range to be indented
	 * @param project the Java project to get the formatter preferences from, or
	 *        <code>null</code> if global preferences should be used
	 * @return an indent result that may be queried for changes and can be
	 *         reused in subsequent indentation operations
	 * @throws BadLocationException if <code>lines</code> is not a valid line
	 *         range on <code>document</code>
	 */
	public static IndentResult indentLines(IDocument document, ILineRange lines, IJavaProject project) throws BadLocationException {
		return indentLines(document, lines, project, null);
	}
	
	/**
	 * Indents the line range specified by <code>lines</code> in
	 * <code>document</code>. The passed Java project may be
	 * <code>null</code>, it is used solely to obtain formatter preferences.
	 * 
	 * @param document the document to be changed
	 * @param lines the line range to be indented
	 * @param project the Java project to get the formatter preferences from, or
	 *        <code>null</code> if global preferences should be used
	 * @param result the result from a previous call to <code>indentLines</code>,
	 *        in order to maintain comment line properties, or <code>null</code>.
	 *        Note that the passed result may be changed by the call.
	 * @return an indent result that may be queried for changes and can be
	 *         reused in subsequent indentation operations
	 * @throws BadLocationException if <code>lines</code> is not a valid line
	 *         range on <code>document</code>
	 */
	public static IndentResult indentLines(IDocument document, ILineRange lines, IJavaProject project, IndentResult result) throws BadLocationException {
		int numberOfLines= lines.getNumberOfLines();
		
		if (numberOfLines < 1)
			return new IndentResult(null);
		
		result= reuseOrCreateToken(result, numberOfLines);
		
		JavaHeuristicScanner scanner= new JavaHeuristicScanner(document);
		JavaIndenter indenter= new JavaIndenter(document, scanner, project);
		boolean changed= false;
		for (int line= lines.getStartLine(), last= line + numberOfLines, i= 0; line < last; line++) {
			changed |= indentLine(document, line, indenter, scanner, result.commentLinesAtColumnZero, i++, project);
		}
		result.hasChanged= changed;
		
		return result;
	}

	private static IndentResult reuseOrCreateToken(IndentResult token, int numberOfLines) {
		if (token == null)
			token= new IndentResult(new boolean[numberOfLines]);
		else if (token.commentLinesAtColumnZero == null)
			token.commentLinesAtColumnZero= new boolean[numberOfLines];
		else if (token.commentLinesAtColumnZero.length != numberOfLines) {
			boolean[] commentBooleans= new boolean[numberOfLines];
			System.arraycopy(token.commentLinesAtColumnZero, 0, commentBooleans, 0, Math.min(numberOfLines, token.commentLinesAtColumnZero.length));
			token.commentLinesAtColumnZero= commentBooleans;
		}
		return token;
	}
	
	/**
	 * Indents a single line using the java heuristic scanner. Javadoc and multi
	 * line comments are indented as specified by the
	 * <code>JavaDocAutoIndentStrategy</code>.
	 * 
	 * @param document the document
	 * @param line the line to be indented
	 * @param indenter the java indenter
	 * @param scanner the heuristic scanner
	 * @param commentLines the indent token comment booleans
	 * @param lineIndex the zero-based line index
	 * @return <code>true</code> if the document was modified,
	 *         <code>false</code> if not
	 * @throws BadLocationException if the document got changed concurrently
	 */
	private static boolean indentLine(IDocument document, int line, JavaIndenter indenter, JavaHeuristicScanner scanner, boolean[] commentLines, int lineIndex, IJavaProject project) throws BadLocationException {
		IRegion currentLine= document.getLineInformation(line);
		final int offset= currentLine.getOffset();
		int wsStart= offset; // where we start searching for non-WS; after the "//" in single line comments
		
		String indent= null;
		if (offset < document.getLength()) {
			ITypedRegion partition= TextUtilities.getPartition(document, IJavaPartitions.JAVA_PARTITIONING, offset, true);
			ITypedRegion startingPartition= TextUtilities.getPartition(document, IJavaPartitions.JAVA_PARTITIONING, offset, false);
			String type= partition.getType();
			if (type.equals(IJavaPartitions.JAVA_DOC) || type.equals(IJavaPartitions.JAVA_MULTI_LINE_COMMENT)) {
				
				// this is a hack
				// what I'd want to do
//				new JavaDocAutoIndentStrategy().indentLineAtOffset(document, offset);
//				return;

				int start= 0;
				if (line > 0) {

					IRegion previousLine= document.getLineInformation(line - 1);
					start= previousLine.getOffset() + previousLine.getLength();
				}

				DocumentCommand command= new DocumentCommand() {};
				// Newline is ok since the command is adjusted afterwards
				command.text= "\n"; //$NON-NLS-1$
				command.offset= start;
				new JavaDocAutoIndentStrategy(IJavaPartitions.JAVA_PARTITIONING).customizeDocumentCommand(document, command);
				int to= 1;
				while (to < command.text.length() && Character.isWhitespace(command.text.charAt(to)))
					to++;
				indent= command.text.substring(1, to);
				
			} else if (!commentLines[lineIndex] && startingPartition.getOffset() == offset && startingPartition.getType().equals(IJavaPartitions.JAVA_SINGLE_LINE_COMMENT)) {
				
				// line comment starting at position 0 -> indent inside
				int max= document.getLength() - offset;
				int slashes= 2;
				while (slashes < max - 1 && document.get(offset + slashes, 2).equals("//")) //$NON-NLS-1$
					slashes+= 2;
				
				wsStart= offset + slashes;
				
				StringBuffer computed= indenter.computeIndentation(offset);
				int tabSize= CodeFormatterUtil.getTabWidth(project);
				while (slashes > 0 && computed.length() > 0) {
					char c= computed.charAt(0);
					if (c == '\t')
						if (slashes > tabSize)
							slashes-= tabSize;
						else
							break;
					else if (c == ' ')
						slashes--;
					else break;
					
					computed.deleteCharAt(0);
				}
				
				indent= document.get(offset, wsStart - offset) + computed;
				
			}
		} 
		
		// standard java indentation
		if (indent == null) {
			StringBuffer computed= indenter.computeIndentation(offset);
			if (computed != null)
				indent= computed.toString();
			else
				indent= new String();
		}
		
		// change document:
		// get current white space
		int lineLength= currentLine.getLength();
		int end= scanner.findNonWhitespaceForwardInAnyPartition(wsStart, offset + lineLength);
		if (end == JavaHeuristicScanner.NOT_FOUND)
			end= offset + lineLength;
		int length= end - offset;
		String currentIndent= document.get(offset, length);
		
		if (length > 0) {
			ITypedRegion partition= TextUtilities.getPartition(document, IJavaPartitions.JAVA_PARTITIONING, end, false);
			if (partition.getOffset() == end && IJavaPartitions.JAVA_SINGLE_LINE_COMMENT.equals(partition.getType())) {
				commentLines[lineIndex]= true;
			}
		}
		
		// only change the document if it is a real change
		if (!indent.equals(currentIndent)) {
			document.replace(offset, length, indent);
			return true;
		}
		
		return false;
	}
}
