/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Tom Eicher (Avaloq Evolution AG) - block selection mode
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import java.util.ArrayList;
import java.util.ResourceBundle;

import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.IEditorInput;

import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorExtension3;
import org.eclipse.ui.texteditor.TextEditorAction;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.core.formatter.IndentManipulation;

import org.eclipse.jdt.internal.core.manipulation.util.Strings;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;

import org.eclipse.jdt.ui.text.IJavaPartitions;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.text.JavaHeuristicScanner;
import org.eclipse.jdt.internal.ui.text.JavaIndenter;


/**
 * Indents a line or range of lines in a Java document to its correct position. No complete
 * AST must be present, the indentation is computed using heuristics. The algorithm used is fast for
 * single lines, but does not store any information and therefore not so efficient for large line
 * ranges.
 *
 * @see org.eclipse.jdt.internal.ui.text.JavaHeuristicScanner
 * @see org.eclipse.jdt.internal.ui.text.JavaIndenter
 * @since 3.0
 */
public class IndentAction extends TextEditorAction {

	/**
	 * @since 3.4
	 */
	private static final class ReplaceData {

		/**
		 * The replacement
		 */
		public final String indent;

		/**
		 * The start of the replacement
		 */
		public final int offset;

		/**
		 * The end of the replacement
		 */
		public final int end;

		/**
		 * Replace string in document from offset to end with indent
		 * @param offset the start of the replacement
		 * @param end the end of the replacement
		 * @param indent the replacement
		 */
		public ReplaceData(int offset, int end, String indent) {
			this.indent= indent;
			this.end= end;
			this.offset= offset;
		}

	}

	/** The caret offset after an indent operation. */
	private int fCaretOffset;

	/**
	 * Whether this is the action invoked by TAB. When <code>true</code>, indentation behaves
	 * differently to accommodate normal TAB operation.
	 */
	private final boolean fIsTabAction;

	public static final String TEXT_BLOCK_STR= "\"\"\""; //$NON-NLS-1$
	public static final String POTENTIAL_TEXT_BLOCK_STR= "\"\""; //$NON-NLS-1$
	public static final String SPACE_STR= " "; //$NON-NLS-1$
	public static final String EMPTY_STR= ""; //$NON-NLS-1$
	private static String TAB_STR= "\t"; //$NON-NLS-1$

	/**
	 * Creates a new instance.
	 *
	 * @param bundle the resource bundle
	 * @param prefix the prefix to use for keys in <code>bundle</code>
	 * @param editor the text editor
	 * @param isTabAction whether the action should insert tabs if over the indentation
	 */
	public IndentAction(ResourceBundle bundle, String prefix, ITextEditor editor, boolean isTabAction) {
		super(bundle, prefix, editor);
		fIsTabAction= isTabAction;
	}

	/*
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		// update has been called by the framework
		if (!isEnabled() || !validateEditorInputState())
			return;

		ITextSelection selection= getSelection();
		final IDocument document= getDocument();

		if (document != null) {

			final int offset= selection.getOffset();
			final int length= selection.getLength();
			final Position end= new Position(offset + length);
			final int firstLine, nLines;
			fCaretOffset= -1;

			try {
				document.addPosition(end);
				firstLine= selection.getStartLine();
				nLines= selection.getEndLine() - firstLine + 1;
			} catch (BadLocationException e) {
				// will only happen on concurrent modification
				JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, "", e)); //$NON-NLS-1$
				return;
			}

			Runnable runnable= () -> {
				IRewriteTarget target= getTextEditor().getAdapter(IRewriteTarget.class);
				if (target != null)
					target.beginCompoundChange();

				try {
					JavaHeuristicScanner scanner= new JavaHeuristicScanner(document);
					JavaIndenter indenter= new JavaIndenter(document, scanner, getJavaProject());
					final boolean multiLine= nLines > 1;
					boolean hasChanged= false;
					TextBlockInfo textBlockInfo= new TextBlockInfo(-1, -1);
					for (int i= 0; i < nLines; i++) {
						hasChanged |= indentLine(document, firstLine + i, offset, indenter, scanner, multiLine, textBlockInfo);
					}

					// update caret position: move to new position when indenting just one line
					// keep selection when indenting multiple
					int newOffset, newLength;
					if (!fIsTabAction && multiLine) {
						newOffset= offset;
						newLength= end.getOffset() - offset;
					} else {
						newOffset= fCaretOffset;
						newLength= 0;
					}

					// always reset the selection if anything was replaced
					// but not when we had a single line non-tab invocation
					if (newOffset != -1 && (hasChanged || newOffset != offset || newLength != length))
						selectAndReveal(newOffset, newLength);

					document.removePosition(end);
				} catch (BadLocationException e) {
					// will only happen on concurrent modification
					JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, "ConcurrentModification in IndentAction", e)); //$NON-NLS-1$

				} finally {
					if (target != null)
						target.endCompoundChange();
				}
			};

			if (nLines > 50) {
				Display display= getTextEditor().getEditorSite().getWorkbenchWindow().getShell().getDisplay();
				BusyIndicator.showWhile(display, runnable);
			} else
				runnable.run();

		}
	}

	/**
	 * Selects the given range on the editor.
	 *
	 * @param newOffset the selection offset
	 * @param newLength the selection range
	 */
	private void selectAndReveal(int newOffset, int newLength) {
		Assert.isTrue(newOffset >= 0);
		Assert.isTrue(newLength >= 0);
		ITextEditor editor= getTextEditor();
		if (editor instanceof JavaEditor) {
			ISourceViewer viewer= ((JavaEditor)editor).getViewer();
			if (viewer != null)
				viewer.setSelectedRange(newOffset, newLength);
		} else
			// this is too intrusive, but will never get called anyway
			getTextEditor().selectAndReveal(newOffset, newLength);

	}

	private static class TextBlockInfo {
		private int partitionOffset;
		private int minIndent;

		public TextBlockInfo(int partitionOffset, int minIndent) {
			this.setPartitionOffset(partitionOffset);
			this.setMinIndent(minIndent);
		}

		public int getPartitionOffset() {
			return partitionOffset;
		}

		public void setPartitionOffset(int partitionOffset) {
			this.partitionOffset = partitionOffset;
		}

		public int getMinIndent() {
			return minIndent;
		}

		public void setMinIndent(int minIndent) {
			this.minIndent = minIndent;
		}
	}

	/**
	 * Indent the given <code>document</code> based on the <code>project</code> settings and
	 * return a text edit describing the changes applied to the document. Returns <b>null</b>
	 * if no changes have been applied.
	 * <p>
	 * WARNING: This method does change the content of the given document.
	 * </p>
	 * <p>
	 * This method is for internal use only, it should not be called.
	 * </p>
	 *
	 * @param document the document to indent must have a java partitioning installed
	 * @param project the project to retrieve the indentation settings from, <b>null</b> for workspace settings
	 * @return a text edit describing the changes or <b>null</b> if no changes required
	 * @throws BadLocationException if the document got modified concurrently
	 *
	 * @since 3.4
	 */
	public static TextEdit indent(IDocument document, IJavaProject project) throws BadLocationException {
		int offset= 0;
		int length= document.getLength();

		JavaHeuristicScanner scanner= new JavaHeuristicScanner(document);
		JavaIndenter indenter= new JavaIndenter(document, scanner, project);

		ArrayList<ReplaceEdit> edits= new ArrayList<>();

		int firstLine= document.getLineOfOffset(offset);
		// check for marginal (zero-length) lines
		int minusOne= length == 0 ? 0 : 1;
		int numberOfLines= document.getLineOfOffset(offset + length - minusOne) - firstLine + 1;

		TextBlockInfo textBlockInfo= new TextBlockInfo(-1, -1);

		int shift= 0;
		for (int i= 0; i < numberOfLines; i++) {
			ReplaceData data= computeReplaceData(document, firstLine + i, indenter, scanner, numberOfLines > 1, false, textBlockInfo, project);

			int replaceLength= data.end - data.offset;
			String currentIndent= document.get(data.offset, replaceLength);

			// only change the document if it is a real change
			if (!data.indent.equals(currentIndent)) {
				edits.add(new ReplaceEdit(data.offset + shift, replaceLength, data.indent));
				//We need to change the document, the indenter depends on it.
				document.replace(data.offset, replaceLength, data.indent);
				shift-= data.indent.length() - replaceLength;
			}
		}

		if (edits.isEmpty())
			return null;

		if (edits.size() == 1)
			return edits.get(0);

		MultiTextEdit result= new MultiTextEdit();
		for (ReplaceEdit replaceEdit : edits) {
			result.addChild(replaceEdit);
		}

		return result;
	}

	/**
	 * Indents a single line using the java heuristic scanner. Javadoc and multiline comments are
	 * indented as specified by the <code>JavaDocAutoIndentStrategy</code>.
	 *
	 * @param document the document
	 * @param line the line to be indented
	 * @param indenter the java indenter
	 * @param scanner the heuristic scanner
	 * @param multiLine <code>true</code> if more than one line is being indented
	 * @param isTabAction <code>true</code> if this action has been invoked by TAB
	 * @param textBlockInfo info about current text block being indented if applicable
	 * @param project the project to retrieve the indentation settings from, <b>null</b> for workspace settings
	 * @return <code>true</code> if <code>document</code> was modified, <code>false</code> otherwise
	 * @throws BadLocationException if the document got changed concurrently
	 */
	private static ReplaceData computeReplaceData(IDocument document, int line, JavaIndenter indenter, JavaHeuristicScanner scanner, boolean multiLine, boolean isTabAction, TextBlockInfo textBlockInfo, IJavaProject project) throws BadLocationException {
		IRegion currentLine= document.getLineInformation(line);
		int offset= currentLine.getOffset();
		int wsStart= offset; // where we start searching for non-WS; after the "//" in single line comments

		String indent= null;
		if (offset < document.getLength()) {
			ITypedRegion partition= TextUtilities.getPartition(document, IJavaPartitions.JAVA_PARTITIONING, offset, true);
			String type= partition.getType();
			ITypedRegion startingPartition= TextUtilities.getPartition(document, IJavaPartitions.JAVA_PARTITIONING, offset, false);
			String startingType= startingPartition.getType();
			boolean isCommentStart= startingPartition.getOffset() == offset;
			if (isDontIndentMultiLineCommentOnFirstColumn(project) && isCommentStart && IJavaPartitions.JAVA_MULTI_LINE_COMMENT.equals(startingType)) {
				indent= ""; //$NON-NLS-1$
			} else if (IJavaPartitions.JAVA_DOC.equals(type) || IJavaPartitions.JAVA_MULTI_LINE_COMMENT.equals(type)) {
				indent= computeJavadocIndent(document, line, scanner, startingPartition);
			} else if (!isTabAction && isCommentStart && IJavaPartitions.JAVA_SINGLE_LINE_COMMENT.equals(startingType)) {
				// line comment starting at position 0
				if (multiLine) {
					//Do what the formatter does
					if (isDontIndentSingleLineCommentOnFirstColumn(project))
						indent= ""; //$NON-NLS-1$
				} else {
					//indent inside -> add/remove indent such that user can start typing at correct position
					int slashes= countLeadingSlashPairs(document, offset) * 2;
					wsStart= offset + slashes;

					StringBuffer computed= indenter.computeIndentation(offset);
					if (computed == null)
						computed= new StringBuffer(0);

					removeIndentations(slashes, getTabSize(project), computed);
					indent= document.get(offset, wsStart - offset) + computed;
				}
			} else if (IJavaPartitions.JAVA_MULTI_LINE_STRING.equals(type)) {
				if (textBlockInfo != null && textBlockInfo.getPartitionOffset() != partition.getOffset()) {
					textBlockInfo.setPartitionOffset(partition.getOffset());
					textBlockInfo.setMinIndent(IndentAction.getTextBlockMinIndent(document, partition.getOffset(), partition.getLength()));
				}
				indent= IndentAction.getTextBlockIndentationString(document, partition.getOffset(), currentLine.getOffset(), textBlockInfo.getMinIndent(), project);
			}
		}

		// standard java indentation
		if (indent == null) {
			StringBuffer computed= indenter.computeIndentation(offset);
			if (computed != null)
				indent= computed.toString();
			else
				indent= ""; //$NON-NLS-1$
		}

		// change document:
		// get current white space
		int lineLength= currentLine.getLength();
		int end= scanner.findNonWhitespaceForwardInAnyPartition(wsStart, offset + lineLength);
		if (end == JavaHeuristicScanner.NOT_FOUND) {
			// an empty line
			end= offset + lineLength;
			if (multiLine && !indentEmptyLines(project))
				indent= ""; //$NON-NLS-1$
		}

		return new ReplaceData(offset, end, indent);
	}

	private static String getLineIndentation(IDocument document, int offset) throws BadLocationException {
		// find start of line
		int adjustedOffset= (offset == document.getLength() ? offset - 1 : offset);
		IRegion line= document.getLineInformationOfOffset(adjustedOffset);
		int start= line.getOffset();

		// find white spaces
		int end= findEndOfWhiteSpace(document, start, offset + line.getLength());

		return document.get(start, end - start);
	}

	private static int findEndOfWhiteSpace(IDocument document, int offset, int end) throws BadLocationException {
		while (offset < end) {
			char c= document.getChar(offset);
			if (c != ' ' && c != '\t') {
				return offset;
			}
			offset++;
		}
		return end;
	}

	public static int measureLengthInSpaces(CharSequence line, int tabWidth) {
		if (tabWidth < 0 || line == null) {
			throw new IllegalArgumentException();
		}

		int length= 0;
		int max= line.length();
		for (int i= 0; i < max; i++) {
			char ch= line.charAt(i);
			if (ch == '\t') {
				length= calculateSpaceEquivalents(tabWidth, length);
			} else {
				length++;
			}
		}
		return length;
	}

	private static int calculateSpaceEquivalents(int tabWidth, int spaceEquivalents) {
		if (tabWidth == 0) {
			return spaceEquivalents;
		}
		int remainder= spaceEquivalents % tabWidth;
		spaceEquivalents+= tabWidth - remainder;
		return spaceEquivalents;
	}

	/**
	 * Removes <code>count</code> indentations from start
	 * of <code>buffer</code>. The size of a space character
	 * is 1 and the size of a tab character is <code>tabSize</code>.
	 *
	 * @param count the number of indentations to remove
	 * @param tabSize the size of a tab character
	 * @param buffer the buffer to modify
	 * @since 3.4
	 */
	private static void removeIndentations(int count, int tabSize, StringBuffer buffer) {
		OUTER:
		while (count > 0 && buffer.length() > 0) {
			char c= buffer.charAt(0);
			switch (c) {
			case '\t':
				if (count > tabSize) {
					count-= tabSize;
				} else {
					break OUTER;
				}
				break;
			case ' ':
				count--;
				break;
			default:
				break OUTER;
			}
			buffer.deleteCharAt(0);
		}
	}

	/**
	 * Returns number of continuous slashes pairs ('//') starting at <code>offset</code>
	 * in <code>document</code>
	 *
	 * @param document the document to inspect
	 * @param offset the offset where to start looking for slash pairs
	 * @return the number of slash pairs.
	 * @throws BadLocationException
	 * @since 3.4
	 */
	private static int countLeadingSlashPairs(IDocument document, int offset) throws BadLocationException {
		IRegion lineInfo= document.getLineInformationOfOffset(offset);
		int max= lineInfo.getOffset() + lineInfo.getLength() - 1;

		int pairCount= 0;
		while (offset < max && "//".equals(document.get(offset, 2))) { //$NON-NLS-1$
			pairCount++;
			offset= offset + 2;
		}

		return pairCount;
	}

	/**
	 * Indents a single line using the java heuristic scanner. Javadoc and multiline comments are
	 * indented as specified by the <code>JavaDocAutoIndentStrategy</code>.
	 *
	 * @param document the document
	 * @param line the line to be indented
	 * @param caret the caret position
	 * @param indenter the java indenter
	 * @param scanner the heuristic scanner
	 * @param multiLine <code>true</code> if more than one line is being indented
	 * @param textBlockInfo info about latest Text Block being indented
	 * @return <code>true</code> if <code>document</code> was modified, <code>false</code> otherwise
	 * @throws BadLocationException if the document got changed concurrently
	 */
	private boolean indentLine(IDocument document, int line, int caret, JavaIndenter indenter, JavaHeuristicScanner scanner, boolean multiLine, TextBlockInfo textBlockInfo) throws BadLocationException {
		IJavaProject project= getJavaProject();

		ReplaceData data= computeReplaceData(document, line, indenter, scanner, multiLine, fIsTabAction, textBlockInfo, project);

		String indent= data.indent;
		int end= data.end;
		int offset= data.offset;

		int length= end - offset;
		String currentIndent= document.get(offset, length);

		// if we are right before the text start / line end, and already after the insertion point
		// then just insert a tab.
		if (fIsTabAction && caret == end && whiteSpaceLength(currentIndent, project) >= whiteSpaceLength(indent, project)) {
			String tab= getTabEquivalent(project);
			document.replace(caret, 0, tab);
			fCaretOffset= caret + tab.length();
			return true;
		}

		// set the caret offset so it can be used when setting the selection
		if (caret >= offset && caret <= end)
			fCaretOffset= offset + indent.length();
		else
			fCaretOffset= -1;

		// only change the document if it is a real change
		if (!indent.equals(currentIndent)) {
			document.replace(offset, length, indent);
			return true;
		} else
			return false;
	}

	/**
	 * Computes and returns the indentation for a javadoc line. The line
	 * must be inside a javadoc comment.
	 *
	 * @param document the document
	 * @param line the line in document
	 * @param scanner the scanner
	 * @param partition the javadoc partition
	 * @return the indent, or <code>null</code> if not computable
	 * @throws BadLocationException
	 * @since 3.1
	 */
	private static String computeJavadocIndent(IDocument document, int line, JavaHeuristicScanner scanner, ITypedRegion partition) throws BadLocationException {
		if (line == 0) // impossible - the first line is never inside a javadoc comment
			return null;

		// don't make any assumptions if the line does not start with \s*\* - it might be
		// commented out code, for which we don't want to change the indent
		final IRegion lineInfo= document.getLineInformation(line);
		final int lineStart= lineInfo.getOffset();
		final int lineLength= lineInfo.getLength();
		final int lineEnd= lineStart + lineLength;
		int nonWS= scanner.findNonWhitespaceForwardInAnyPartition(lineStart, lineEnd);
		if (nonWS == JavaHeuristicScanner.NOT_FOUND || document.getChar(nonWS) != '*') {
			if (nonWS == JavaHeuristicScanner.NOT_FOUND)
				return document.get(lineStart, lineLength);
			return document.get(lineStart, nonWS - lineStart);
		}

		// take the indent from the previous line and reuse
		IRegion previousLine= document.getLineInformation(line - 1);
		int previousLineStart= previousLine.getOffset();
		int previousLineLength= previousLine.getLength();
		int previousLineEnd= previousLineStart + previousLineLength;

		StringBuilder buf= new StringBuilder();
		int previousLineNonWS= scanner.findNonWhitespaceForwardInAnyPartition(previousLineStart, previousLineEnd);
		if (previousLineNonWS == JavaHeuristicScanner.NOT_FOUND || document.getChar(previousLineNonWS) != '*') {
			// align with the comment start if the previous line is not an asterisked line
			previousLine= document.getLineInformationOfOffset(partition.getOffset());
			previousLineStart= previousLine.getOffset();
			previousLineLength= previousLine.getLength();
			previousLineEnd= previousLineStart + previousLineLength;
			previousLineNonWS= scanner.findNonWhitespaceForwardInAnyPartition(previousLineStart, previousLineEnd);
			if (previousLineNonWS == JavaHeuristicScanner.NOT_FOUND)
				previousLineNonWS= previousLineEnd;

			// add the initial space
			// TODO this may be controlled by a formatter preference in the future
			buf.append(' ');
		}

		String indentation= document.get(previousLineStart, previousLineNonWS - previousLineStart);
		buf.insert(0, indentation);
		return buf.toString();
	}

	/**
	 * Returns the size in characters of a string. All characters count one, tabs count the editor's
	 * preference for the tab display
	 *
	 * @param indent the string to be measured.
	 * @param project the project to retrieve the indentation settings from, <b>null</b> for workspace settings
	 * @return the size in characters of a string
	 */
	private static int whiteSpaceLength(String indent, IJavaProject project) {
		if (indent == null)
			return 0;
		else {
			int size= 0;
			int tabSize= getTabSize(project);

			for (char c : indent.toCharArray()) {
				size += c == '\t' ? tabSize : 1;
			}
			return size;
		}
	}

	/**
	 * Returns a tab equivalent, either as a tab character or as spaces, depending on the editor and
	 * formatter preferences.
	 *
	 * @param project the project to retrieve the indentation settings from, <b>null</b> for workspace settings
	 * @return a string representing one tab in the editor, never <code>null</code>
	 */
	private static String getTabEquivalent(IJavaProject project) {
		String tab;
		if (JavaCore.SPACE.equals(getCoreFormatterOption(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, project))) {
			int size= getTabSize(project);

			if (size < 1) {
				tab= ""; //$NON-NLS-1$
			} else {
				tab= String.format("%" + size + "s", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		} else
			tab= "\t"; //$NON-NLS-1$

		return tab;
	}

	/**
	 * Returns the tab size used by the java editor, which is deduced from the
	 * formatter preferences.
	 *
	 * @param project the project to retrieve the indentation settings from, <b>null</b> for workspace settings
	 * @return the tab size as defined in the current formatter preferences
	 */
	private static int getTabSize(IJavaProject project) {
		return getCoreFormatterOption(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, 4, project);
	}

	/**
	 * Returns <code>true</code> if empty lines should be indented, <code>false</code> otherwise.
	 *
	 * @param project the project to retrieve the indentation settings from, <b>null</b> for workspace settings
	 * @return <code>true</code> if empty lines should be indented, <code>false</code> otherwise
	 * @since 3.2
	 */
	private static boolean indentEmptyLines(IJavaProject project) {
		return DefaultCodeFormatterConstants.TRUE.equals(getCoreFormatterOption(DefaultCodeFormatterConstants.FORMATTER_INDENT_EMPTY_LINES, project));
	}

	/**
	 * Returns <code>true</code> if multi line comments which start at first column
	 * should not be indented, <code>false</code> otherwise.
	 *
	 * @param project the project to retrieve the indentation settings from, <b>null</b> for workspace settings
	 * @return <code>true</code> if such multi line comments should be indented, <code>false</code> otherwise
	 * @since 3.4
	 */
	private static boolean isDontIndentMultiLineCommentOnFirstColumn(IJavaProject project) {
		return DefaultCodeFormatterConstants.TRUE.equals(getCoreFormatterOption(DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_BLOCK_COMMENTS_ON_FIRST_COLUMN, project));
	}

	/**
	 * Returns <code>true</code> if single line comments which start at first column
	 * should not be indented, <code>false</code> otherwise.
	 *
	 * @param project the project to retrieve the indentation settings from, <b>null</b> for workspace settings
	 * @return <code>true</code> if such single line comments should be indented, <code>false</code> otherwise
	 * @since 3.4
	 */
	private static boolean isDontIndentSingleLineCommentOnFirstColumn(IJavaProject project) {
		return DefaultCodeFormatterConstants.TRUE.equals(getCoreFormatterOption(DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_LINE_COMMENTS_ON_FIRST_COLUMN, project));
	}

	/**
	 * Returns the possibly project-specific core preference defined under <code>key</code>.
	 *
	 * @param key the key of the preference
	 * @param project the project to retrieve the indentation settings from, <b>null</b> for workspace settings
	 * @return the value of the preference
	 * @since 3.1
	 */
	private static String getCoreFormatterOption(String key, IJavaProject project) {
		if (project == null)
			return JavaCore.getOption(key);
		return project.getOption(key, true);
	}

	/**
	 * Returns the possibly project-specific core preference defined under <code>key</code>, or
	 * <code>def</code> if the value is not a integer.
	 *
	 * @param key the key of the preference
	 * @param def the default value
	 * @param project the project to retrieve the indentation settings from, <b>null</b> for workspace settings
	 * @return the value of the preference
	 * @since 3.1
	 */
	private static int getCoreFormatterOption(String key, int def, IJavaProject project) {
		try {
			return Integer.parseInt(getCoreFormatterOption(key, project));
		} catch (NumberFormatException e) {
			return def;
		}
	}

	/**
	 * Returns the <code>IJavaProject</code> of the current editor input, or
	 * <code>null</code> if it cannot be found.
	 *
	 * @return the <code>IJavaProject</code> of the current editor input, or
	 *         <code>null</code> if it cannot be found
	 * @since 3.1
	 */
	private IJavaProject getJavaProject() {
		ITextEditor editor= getTextEditor();
		if (editor == null)
			return null;

		ICompilationUnit cu= JavaPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(editor.getEditorInput());
		if (cu == null)
			return null;
		return cu.getJavaProject();
	}

	/**
	 * Returns the editor's selection provider.
	 *
	 * @return the editor's selection provider or <code>null</code>
	 */
	private ISelectionProvider getSelectionProvider() {
		ITextEditor editor= getTextEditor();
		if (editor != null) {
			return editor.getSelectionProvider();
		}
		return null;
	}

	/*
	 * @see org.eclipse.ui.texteditor.IUpdate#update()
	 */
	@Override
	public void update() {
		super.update();

		if (isEnabled())
			if (fIsTabAction)
				setEnabled(canModifyEditor() && isSmartMode() && isValidSelection());
			else
				setEnabled(canModifyEditor() && !getSelection().isEmpty());
	}

	/**
	 * Returns if the current selection is valid, i.e. whether it is empty and the caret in the
	 * whitespace at the start of a line, or covers multiple lines.
	 *
	 * @return <code>true</code> if the selection is valid for an indent operation
	 */
	private boolean isValidSelection() {
		ITextSelection selection= getSelection();
		if (selection.isEmpty())
			return false;

		int offset= selection.getOffset();
		int length= selection.getLength();

		IDocument document= getDocument();
		if (document == null)
			return false;

		try {
			IRegion firstLine= document.getLineInformationOfOffset(offset);
			int lineOffset= firstLine.getOffset();

			// either the selection has to be empty and the caret in the WS at the line start
			// or the selection has to extend over multiple lines
			if (length == 0)
				return document.get(lineOffset, offset - lineOffset).trim().length() == 0;
			else
//				return lineOffset + firstLine.getLength() < offset + length;
				return false; // only enable for empty selections for now

		} catch (BadLocationException e) {
		}

		return false;
	}

	/**
	 * Returns the smart preference state.
	 *
	 * @return <code>true</code> if smart mode is on, <code>false</code> otherwise
	 */
	private boolean isSmartMode() {
		ITextEditor editor= getTextEditor();

		if (editor instanceof ITextEditorExtension3)
			return ((ITextEditorExtension3) editor).getInsertMode() == ITextEditorExtension3.SMART_INSERT;

		return false;
	}

	/**
	 * Returns the document currently displayed in the editor, or <code>null</code> if none can be
	 * obtained.
	 *
	 * @return the current document or <code>null</code>
	 */
	private IDocument getDocument() {

		ITextEditor editor= getTextEditor();
		if (editor != null) {

			IDocumentProvider provider= editor.getDocumentProvider();
			IEditorInput input= editor.getEditorInput();
			if (provider != null && input != null)
				return provider.getDocument(input);

		}
		return null;
	}

	/**
	 * Returns the selection on the editor or an invalid selection if none can be obtained. Returns
	 * never <code>null</code>.
	 *
	 * @return the current selection, never <code>null</code>
	 */
	private ITextSelection getSelection() {
		ISelectionProvider provider= getSelectionProvider();
		if (provider != null) {

			ISelection selection= provider.getSelection();
			if (selection instanceof ITextSelection)
				return (ITextSelection) selection;
		}

		// null object
		return TextSelection.emptySelection();
	}

	public static int getTextBlockMinIndent(IDocument document, int offset, int length) throws BadLocationException {
		int indent= Integer.MAX_VALUE;

		int startLine= document.getLineOfOffset(offset);
		int numLines= document.getNumberOfLines(offset, length);
		boolean opened= false;
		for (int i= startLine; i < startLine + numLines; ++i) {
			IRegion line= document.getLineInformation(i);
			String lineStr= document.get(line.getOffset(), line.getLength());
			if (opened) {
				String indentation= getLineIndentation(document, line.getOffset());
				if (!indentation.equals(lineStr) && indentation.length() < indent) {
					indent= indentation.length();
				}
			} else if (lineStr.trim().endsWith(IndentAction.TEXT_BLOCK_STR)) {
				opened= true;
			}
		}
		if (indent == Integer.MAX_VALUE) {
			indent= 0;
		}
		return indent;
	}

	public static String getTextBlockIndentationString(IDocument document, int offset, int commandOffset, int minIndent, IJavaProject javaProject) throws BadLocationException {
		IRegion line= document.getLineInformationOfOffset(offset);
		String fullStrNoTrim= document.get(line.getOffset(), commandOffset - line.getOffset());
		if (!fullStrNoTrim.trim().endsWith(IndentAction.TEXT_BLOCK_STR)) {
			fullStrNoTrim= document.get(line.getOffset(), line.getLength());
		}
		String indentation= getLineIndentation(document, offset);
		int startIndex= fullStrNoTrim.lastIndexOf(IndentAction.TEXT_BLOCK_STR);
		if (startIndex > 0) {
			if (fullStrNoTrim.charAt(startIndex - 1) == '\\') {
				startIndex= -1;
			}
		}
		String tabString= getTabString(javaProject);
		String tabSpaceString= getTabSpaceString(javaProject);
		int textBlockIndentationOption= getTextBlockIndentation(javaProject);
		String formatterTabValue= getFormatterTabValue(javaProject);
		String stringTocalculate= null;
		boolean isTextBlockStarting= false;
		if ((fullStrNoTrim.endsWith(IndentAction.TEXT_BLOCK_STR) || fullStrNoTrim.trim().endsWith(IndentAction.TEXT_BLOCK_STR)) && startIndex != -1) {
			stringTocalculate= fullStrNoTrim.substring(0, startIndex);
			isTextBlockStarting= true;
		} else {
			stringTocalculate= indentation;
		}
		if (isTextBlockStarting) {
			if (textBlockIndentationOption == DefaultCodeFormatterConstants.INDENT_ON_COLUMN) {
				String newStr= indentation;
				int length= IndentAction.measureLengthInSpaces(stringTocalculate, CodeFormatterUtil.getTabWidth(javaProject));
				StringBuilder str= new StringBuilder();
				if (getUseTabsOnlyForLeadingIndentations(javaProject)) {
					int existing= IndentAction.measureLengthInSpaces(indentation, CodeFormatterUtil.getTabWidth(javaProject));
					if (length - existing > 0) {
						for (int i= 0; i < length - existing; i++) {
							newStr+= IndentAction.SPACE_STR;
						}
					}
				} else {
					for (int i= 0; i < length; i++) {
						str.append(IndentAction.SPACE_STR);
					}
					int units= Strings.computeIndentUnits(str.toString(), javaProject);
					newStr= CodeFormatterUtil.createIndentString(units, javaProject);
					int newLength= IndentManipulation.measureIndentInSpaces(newStr, CodeFormatterUtil.getTabWidth(javaProject));
					if (newLength < length) {
						if (DefaultCodeFormatterConstants.MIXED.equals(formatterTabValue) || JavaCore.SPACE.equals(formatterTabValue)) {
							for (int i= newLength; i < length; i++) {
								newStr+= IndentAction.SPACE_STR;
							}
						} else if (JavaCore.TAB.equals(formatterTabValue)) {
							newStr+= tabString;
						}
					}
				}
				indentation= newStr;
			} else if (textBlockIndentationOption == DefaultCodeFormatterConstants.INDENT_BY_ONE) {
				if (JavaCore.TAB.equals(formatterTabValue)
						|| DefaultCodeFormatterConstants.MIXED.equals(formatterTabValue)) {
					indentation+= tabString;
				} else if (JavaCore.SPACE.equals(formatterTabValue)) {
					indentation+= tabSpaceString;
				}
			} else if (textBlockIndentationOption == DefaultCodeFormatterConstants.INDENT_PRESERVE) {
				indentation= IndentAction.EMPTY_STR;
			} else if (textBlockIndentationOption == DefaultCodeFormatterConstants.INDENT_DEFAULT) {
				String indentString= EMPTY_STR;
				if (JavaCore.TAB.equals(formatterTabValue)
						|| DefaultCodeFormatterConstants.MIXED.equals(formatterTabValue)) {
					indentString= tabString;
				} else if (JavaCore.SPACE.equals(formatterTabValue)) {
					indentString= tabSpaceString;
				}
				int count= getFormatterContinuationIndentation(javaProject);
				for (int i= 0; i < count; i++) {
					indentation+= indentString;
				}
			}
		}
		String commandIndent= EMPTY_STR;
		if (minIndent > 0) {
			IRegion commandLine= document.getLineInformationOfOffset(commandOffset);
			// handle extra indentation but ignore empty lines or short lines with only white-space
			if (commandLine.getLength() > minIndent) {
				commandIndent= getLineIndentation(document, commandLine.getOffset()).substring(minIndent);
			}
		}
		return indentation + commandIndent;
	}

	public static String getIndentationAsPerTextBlockSettings(IDocument document, int offset, IJavaProject javaProject) throws BadLocationException {
		IRegion line= document.getLineInformationOfOffset(offset);
		int startLength= document.getLineOffset(document.getLineOfOffset(offset));
		String fullStrNoTrim= document.get(line.getOffset(), offset - startLength);
		String indentation= getLineIndentation(document, offset);
		String tabString= getTabString(javaProject);
		String tabSpaceString= getTabSpaceString(javaProject);
		int textBlockIndentationOption= getTextBlockIndentation(javaProject);
		String formatterTabValue= getFormatterTabValue(javaProject);
		String stringTocalculate= fullStrNoTrim;
		if (textBlockIndentationOption == DefaultCodeFormatterConstants.INDENT_ON_COLUMN) {
			String newStr= indentation;
			int length= IndentAction.measureLengthInSpaces(stringTocalculate, CodeFormatterUtil.getTabWidth(javaProject));
			StringBuilder str= new StringBuilder();
			if (getUseTabsOnlyForLeadingIndentations(javaProject)) {
				int existing= IndentAction.measureLengthInSpaces(indentation, CodeFormatterUtil.getTabWidth(javaProject));
				if (length - existing > 0) {
					for (int i= 0; i < length - existing; i++) {
						newStr+= IndentAction.SPACE_STR;
					}
				}
			} else {
				for (int i= 0; i < length; i++) {
					str.append(IndentAction.SPACE_STR);
				}
				int units= Strings.computeIndentUnits(str.toString(), javaProject);
				newStr= CodeFormatterUtil.createIndentString(units, javaProject);
				int newLength= IndentManipulation.measureIndentInSpaces(newStr, CodeFormatterUtil.getTabWidth(javaProject));
				if (newLength < length) {
					if (DefaultCodeFormatterConstants.MIXED.equals(formatterTabValue) || JavaCore.SPACE.equals(formatterTabValue)) {
						for (int i= newLength; i < length; i++) {
							newStr+= IndentAction.SPACE_STR;
						}
					} else if (JavaCore.TAB.equals(formatterTabValue)) {
						newStr+= tabString;
					}
				}
			}
			indentation= newStr;
		} else if (textBlockIndentationOption == DefaultCodeFormatterConstants.INDENT_BY_ONE) {
			if (JavaCore.TAB.equals(formatterTabValue)
					|| DefaultCodeFormatterConstants.MIXED.equals(formatterTabValue)) {
				indentation+= tabString;
			} else if (JavaCore.SPACE.equals(formatterTabValue)) {
				indentation+= tabSpaceString;
			}
		} else if (textBlockIndentationOption == DefaultCodeFormatterConstants.INDENT_PRESERVE) {
			indentation= IndentAction.EMPTY_STR;
		} else if (textBlockIndentationOption == DefaultCodeFormatterConstants.INDENT_DEFAULT) {
			String indentString= EMPTY_STR;
			if (JavaCore.TAB.equals(formatterTabValue)
					|| DefaultCodeFormatterConstants.MIXED.equals(formatterTabValue)) {
				indentString= tabString;
			} else if (JavaCore.SPACE.equals(formatterTabValue)) {
				indentString= tabSpaceString;
			}
			int count= getFormatterContinuationIndentation(javaProject);
			for (int i= 0; i < count; i++) {
				indentation+= indentString;
			}
		}
		return indentation;
	}

	private static int getTextBlockIndentation(IJavaProject javaProject) {
		String textBlockIndentationOption= getCoreFormatterOption(DefaultCodeFormatterConstants.FORMATTER_TEXT_BLOCK_INDENTATION, javaProject);
		int val= DefaultCodeFormatterConstants.INDENT_DEFAULT;
		if (textBlockIndentationOption != null) {
			Integer iVal= Integer.valueOf(textBlockIndentationOption);
			if (iVal != null) {
				val= iVal;
			}
		}
		return val;
	}

	private static boolean getUseTabsOnlyForLeadingIndentations(IJavaProject javaProject) {
		boolean val= false;
		String useTabsOnlyForLeadingIndentations= getCoreFormatterOption(DefaultCodeFormatterConstants.FORMATTER_USE_TABS_ONLY_FOR_LEADING_INDENTATIONS, javaProject);
		if (useTabsOnlyForLeadingIndentations != null) {
			val= Boolean.parseBoolean(useTabsOnlyForLeadingIndentations);
		}
		return val;
	}

	private static String getTabString(IJavaProject javaProject) {
		String tabString= TAB_STR;
		if (getUseTabsOnlyForLeadingIndentations(javaProject)) {
			int tabWidth= CodeFormatterUtil.getTabWidth(javaProject);
			tabString= EMPTY_STR;
			for (int i= 0; i < tabWidth; i++) {
				tabString+= SPACE_STR;
			}
		}
		return tabString;
	}

	private static String getTabSpaceString(IJavaProject javaProject) {
		int tabWidth= CodeFormatterUtil.getTabWidth(javaProject);
		String tabString= EMPTY_STR;
		for (int i= 0; i < tabWidth; i++) {
			tabString+= SPACE_STR;
		}
		return tabString;
	}

	private static int getFormatterContinuationIndentation(IJavaProject javaProject) {
		int formatterContinuationIndentationSize= 2;
		try {
			formatterContinuationIndentationSize= Integer.parseInt(getCoreFormatterOption(
					DefaultCodeFormatterConstants.FORMATTER_CONTINUATION_INDENTATION, javaProject));
		} catch (NumberFormatException ex) {
			// Ignore, use default of 2
		}
		return formatterContinuationIndentationSize;
	}

	private static String getFormatterTabValue(IJavaProject javaProject) {
		String formatterTabValue= getCoreFormatterOption(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, javaProject);
		if (formatterTabValue == null) {
			formatterTabValue= JavaCore.TAB;
		}
		return formatterTabValue;
	}
}
