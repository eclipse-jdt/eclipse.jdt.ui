/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java.hover;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPartitioningException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.IWorkbenchPartOrientation;

import org.eclipse.ui.editors.text.EditorsUI;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TextBlock;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.util.Strings;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.TokenScanner;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.IJavaPartitions;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.text.JavaCodeReader;
import org.eclipse.jdt.internal.ui.text.JavaPairMatcher;


/**
 * Provides as hover info the source of the selected JavaElement, or the source near the matching
 * opening curly brace.
 */
public class JavaSourceHover extends AbstractJavaEditorTextHover {

	/**
	 * The upward shift in location in lines for the bracket hover.
	 *
	 * @since 3.8
	 */
	private int fUpwardShiftInLines;

	/**
	 * The status text for the bracket hover.
	 *
	 * @since 3.8
	 */
	private String fBracketHoverStatus;

	/**
	 * The hovered Java element to get the source.
	 *
	 * @since 3.14
	 */
	private IJavaElement fJavaElement;

	/** Source range of hovered node within it's origin source content */
	private ISourceRange fNodeRange;

	/** Lines of bracket-type node's content to keep not skipped in hover viewer */
	private List<Integer> fKeptLines;

	static class JavaSourceInformationInput {
		private IJavaElement fElement;

		private String fHoverInfo;

		public JavaSourceInformationInput(IJavaElement javaElement, String hoverInfo) {
			fElement= javaElement;
			fHoverInfo= hoverInfo;
		}

		public IJavaElement getJavaElement() {
			return fElement;
		}

		public String getHoverInfo() {
			return fHoverInfo;
		}
	}

	/**
	 * Java source information input supporting semantic coloring
	 */
	static abstract class JavaSourceSemanticInformationInput extends JavaSourceInformationInput {
		final ITypeRoot fRootElement;
		final String fFullContent;
		final ISourceRange fVisibleRange;

		JavaSourceSemanticInformationInput(IJavaElement javaElement, String hoverInfo, ITypeRoot rootElement, String fullContent, ISourceRange visibleRange) {
			super(javaElement, hoverInfo);
			fRootElement= rootElement;
			fFullContent= fullContent;
			fVisibleRange= visibleRange;
		}

		public boolean shouldTriggerSemanticColoring(SourceViewer sourceViewer) {
			setContentAndVisibleRegion(sourceViewer);
			return true;
		}

		void setContentAndVisibleRegion(SourceViewer sourceViewer) {
			// set viewer content with full content for which semantic coloring will be prepared
			sourceViewer.getDocument().set(fFullContent);
			// then set viewer's visible region
			setVisibleRegion(sourceViewer);
		}

		void setVisibleRegion(SourceViewer sourceViewer) {
			// limit visible region of whole source content to just area of interest
			sourceViewer.setVisibleRegion(fVisibleRange.getOffset(), fVisibleRange.getLength());
		}

		public abstract void postSemanticColoring(SourceViewer sourceViewer);
	}

	/**
	 * Java source information input supporting semantic coloring and line trimming in hover viewer's visible content
	 */
	static abstract class JavaSourceLineTrimmingInformationInput extends JavaSourceSemanticInformationInput {
		private List<IRegion> fTrimRegions;

		JavaSourceLineTrimmingInformationInput(IJavaElement javaElement, String hoverInfo, ITypeRoot rootElement, String fullContent, ISourceRange visibleRange) {
			super(javaElement, hoverInfo, rootElement, fullContent, visibleRange);
		}

		@Override
		void setVisibleRegion(SourceViewer sourceViewer) {
			super.setVisibleRegion(sourceViewer);

			// prepare lines trimming in visible region
			fTrimRegions= new ArrayList<>(0);
			try {
				IDocument document= sourceViewer.getDocument();
				IRegion visibleRegion= sourceViewer.getVisibleRegion(); // fVisibleRange extended to start of first line
				String hoverElementSource= document.get(visibleRegion.getOffset(), visibleRegion.getLength());
				String[] sourceLines= Strings.convertIntoLines(hoverElementSource);
				fTrimRegions= new ArrayList<>(sourceLines.length);
				String[] trimmedLines= sourceLines.clone();
				Strings.trimIndentation(trimmedLines, fRootElement.getJavaProject());
				int line= document.getLineOfOffset(visibleRegion.getOffset());
				int offsetShift= 0;
				for (int i= 0; i < sourceLines.length; i++) {
					int trimChars= sourceLines[i].indexOf(trimmedLines[i]);
					if (trimChars > 0) {
						fTrimRegions.add(new Region(document.getLineOffset(line) - offsetShift, trimChars));
						offsetShift+= trimChars;
					}
					line++;
				}
			} catch (BadLocationException e) {
				JavaPlugin.log(e);
			}
		}

		@Override
		public void postSemanticColoring(SourceViewer sourceViewer) {
			doLineTrimming(sourceViewer);
		}

		void doLineTrimming(SourceViewer sourceViewer) {
			// apply lines trimming in visible region
			try {
				for (IRegion trimRegion : fTrimRegions) {
					sourceViewer.getDocument().replace(trimRegion.getOffset(), trimRegion.getLength(), ""); //$NON-NLS-1$
				}
			} catch (BadLocationException e) {
				JavaPlugin.log(e);
			}
		}
	}

	/**
	 * Java source information input supporting semantic coloring for java elements hover
	 */
	static class JavaElementSourceInformationInput extends JavaSourceLineTrimmingInformationInput {

		JavaElementSourceInformationInput(IJavaElement javaElement, String hoverInfo, ITypeRoot rootElement) throws JavaModelException {
			super(javaElement, hoverInfo, rootElement, rootElement.getSource(), (javaElement instanceof ISourceReference) ? ((ISourceReference) javaElement).getSourceRange() : null);
		}

		JavaElementSourceInformationInput(String hoverInfo, IJavaElement javaElement) throws Exception {
			this(javaElement, hoverInfo, findRootElement(javaElement));
		}

		private static ITypeRoot findRootElement(IJavaElement javaElement) {
			IJavaElement parent= javaElement;
			while (parent != null && !(parent instanceof ITypeRoot)) {
				parent= parent.getParent();
			}
			if (!(parent instanceof ITypeRoot)) {
				throw new IllegalArgumentException("Unable to find ITypeRoot parent "); //$NON-NLS-1$
			}
			return (ITypeRoot) parent;
		}

		@Override
		void setVisibleRegion(SourceViewer sourceViewer) {
			if (fVisibleRange == null) {
				// should never happen since javaElement should always be ISourceReference
				return;
			}
			// set range indication to actual beginning of element ('scrolls' past any JavaDoc if present) before setting visible region
			AbstractDocument doc= (AbstractDocument) sourceViewer.getDocument();
			int offset= fVisibleRange.getOffset();
			int lenght= fVisibleRange.getLength();
			try {
				ITypedRegion partition= null;
				int endOffset= offset + lenght;
				while (offset < endOffset) { // we should never reach end of visible range
					partition= doc.getPartition(IJavaPartitions.JAVA_PARTITIONING, offset, false);
					switch (partition.getType()) {
						case IJavaPartitions.JAVA_DOC:
						case IJavaPartitions.JAVA_MARKDOWN_COMMENT:
						case IJavaPartitions.JAVA_SINGLE_LINE_COMMENT:
						case IJavaPartitions.JAVA_MULTI_LINE_COMMENT:
							offset+= partition.getLength();
							lenght-= partition.getLength();
							break;
						default:
							endOffset= offset; // break out from loop
							break;
					}
				}
				while (Character.isWhitespace(doc.getChar(offset))) {
					offset++;
					lenght--;
				}
			} catch (Exception e) {
				JavaPlugin.log(e);
				offset= fVisibleRange.getOffset();
				lenght= fVisibleRange.getLength();
			}
			sourceViewer.setRangeIndication(offset, lenght, true);

			super.setVisibleRegion(sourceViewer);
		}
	}

	/**
	 * Java source information input supporting semantic coloring for bracket nodes hover
	 */
	static class JavaBracketSourceInformationInput extends JavaSourceLineTrimmingInformationInput {
		private final List<Integer> fKeptLines;

		JavaBracketSourceInformationInput(String hoverInfo, ITypeRoot rootElement, IDocument document, ISourceRange nodeRange, List<Integer> keptLines) {
			super(null, hoverInfo, rootElement, document.get(), adjustVisibleRange(document, nodeRange, keptLines));
			fKeptLines= new ArrayList<>(keptLines);
		}

		// handles case when visible area should end with last kept line (no following JavaSourceHover_skippedLinesSymbol)
		private static ISourceRange adjustVisibleRange(IDocument document, ISourceRange nodeRange, List<Integer> keptLines) {
			try {
				int lastNodeRangeLine= document.getLineOfOffset(nodeRange.getOffset() + nodeRange.getLength());
				boolean noLinesAfterLastKept= keptLines.get(keptLines.size() - 1).intValue() == lastNodeRangeLine - 1;
				if (noLinesAfterLastKept) {
					return new SourceRange(nodeRange.getOffset(), document.getLineOffset(lastNodeRangeLine) - 1 - nodeRange.getOffset());
				}
			} catch (BadLocationException e) {
				JavaPlugin.log(e);
			}
			return nodeRange;
		}

		@Override
		public void postSemanticColoring(SourceViewer sourceViewer) {
			try {
				IDocument document= sourceViewer.getDocument();

				// calculate first & last line from node range (fVisibleRange) before lines trimming invalidates range positions
				int firstLine= document.getLineOfOffset(fVisibleRange.getOffset());
				int lastLine= document.getLineOfOffset(fVisibleRange.getOffset() + fVisibleRange.getLength());

				doLineTrimming(sourceViewer);

				// apply skipped lines substitutions
				List<Integer> keptLines= new ArrayList<>(fKeptLines);
				int currentLine= firstLine;
				int replaceStartOffset= -1;
				int linesToRemove= 0;
				int lineShift= 0;
				for (; currentLine <= lastLine; currentLine++) {
					if (!keptLines.remove(Integer.valueOf(currentLine))) { // line to be replaced
						if (replaceStartOffset == -1) {
							replaceStartOffset= document.getLineOffset(currentLine - lineShift);
						}
						linesToRemove++;
					} else { // kept line
						if (replaceStartOffset != -1) {
							int length= document.getLineOffset(currentLine - lineShift) - 1 - replaceStartOffset;
							if (document.getChar(replaceStartOffset + length - 1) == '\r') {
								length--;
							}
							document.replace(replaceStartOffset, length, JavaHoverMessages.JavaSourceHover_skippedLinesSymbol);
							replaceStartOffset= -1;
							lineShift+= linesToRemove - 1;
							linesToRemove= 0;
						}
					}
				}
				if (replaceStartOffset != -1) {
					// replace remaining lines
					document.replace(replaceStartOffset, document.getLength() - replaceStartOffset, JavaHoverMessages.JavaSourceHover_skippedLinesSymbol);
				}
			} catch (BadLocationException e) {
				JavaPlugin.log(e);
			}
		}
	}

	/**
	 * Java source information input supporting semantic coloring for text blocks hover
	 */
	static class JavaTextBlockSourceInformationInput extends JavaSourceSemanticInformationInput {

		JavaTextBlockSourceInformationInput(String hoverInfo) {
			super(null, hoverInfo, null,
				// add new lines with 3 quote characters as first and last line (out of visible region) to have text rendered with text block color
				"\"\"\"\n" + hoverInfo + "\n\"\"\"",  //$NON-NLS-1$//$NON-NLS-2$
				new SourceRange(4, hoverInfo.length())); // hide first line (3 quote characters + 1 new line character) and last line
		}

		@Override
		public boolean shouldTriggerSemanticColoring(SourceViewer sourceViewer) {
			setContentAndVisibleRegion(sourceViewer);
			// don't trigger semantic coloring job since viewer already renders the content with proper colors
			return false;
		}

		@Override
		public void postSemanticColoring(SourceViewer sourceViewer) {
			// no-op, never called
		}
	}

	@Override
	public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {
		String hoverInfoString= getHoverInfo(textViewer, hoverRegion);
		if (hoverInfoString == null) {
			return null;
		}
		try {
			if (fJavaElement != null) {
				if (JavaModelUtil.isModule(fJavaElement)) {
					// pre-semantic coloring behavior
					return new JavaSourceInformationInput(fJavaElement, hoverInfoString);
				}
				// element hover (source may be from other compilation unit)
				return new JavaElementSourceInformationInput(hoverInfoString, fJavaElement);
			}
			if (!fKeptLines.isEmpty()) {
				// bracket hover (source from visible editor)
				return new JavaBracketSourceInformationInput(hoverInfoString, getEditorInputJavaElement(), textViewer.getDocument(), fNodeRange, fKeptLines);
			}
			// text block hover (source from visible editor)
			return new JavaTextBlockSourceInformationInput(hoverInfoString);
		} catch (Exception e) {
			JavaPlugin.log(e);
			// pre-semantic coloring behavior
			return new JavaSourceInformationInput(fJavaElement, hoverInfoString);
		}
	}

	@Override
	@Deprecated
	public String getHoverInfo(ITextViewer textViewer, IRegion region) {
		IJavaElement[] result= getJavaElementsAt(textViewer, region);
		fJavaElement= null;

		fUpwardShiftInLines= 0;
		fBracketHoverStatus= null;
		fNodeRange= null;
		fKeptLines= new ArrayList<>(16);

		if (result == null || result.length == 0) {
			String val= getBracketHoverInfo(textViewer, region);
			if (val == null) {
				return getTextBlockHoverInfo(textViewer, region);
			}
			return val;
		}

		if (result.length > 1)
			return null;

		fJavaElement= result[0];
		if ((fJavaElement instanceof IMember || fJavaElement instanceof ILocalVariable || fJavaElement instanceof ITypeParameter) && fJavaElement instanceof ISourceReference) {
			try {
				String source= ((ISourceReference) fJavaElement).getSource();
				ISourceRange elementRange= ((ISourceReference) fJavaElement).getSourceRange();
				IBuffer elementOriginBuffer= fJavaElement.getOpenable().getBuffer();
				int offset= elementRange.getOffset() - 1;
				int trimmingOffset= 0;
				if (offset >= 0 && elementOriginBuffer.getChar(offset) != '\n') {
					while(offset >= 0 && elementOriginBuffer.getChar(--offset) != '\n') {
						// no-op
					}
					trimmingOffset= elementRange.getOffset() - ++offset;
					// add indentation of first line of element source segment not contained in text returned by getSource()
					source= elementOriginBuffer.getText(offset, elementRange.getLength() + trimmingOffset);
				}

				String[] sourceLines= getTrimmedSource(source, trimmingOffset, fJavaElement);
				if (sourceLines == null)
					return null;

				String delim= StubUtility.getLineDelimiterUsed(fJavaElement);
				source= Strings.concatenate(sourceLines, delim);

				return source;
			} catch (JavaModelException ex) {
				//do nothing
			}
		}
		return null;
	}

	private String getBracketHoverInfo(final ITextViewer textViewer, IRegion region) {
		boolean isElsePart= false;
		IEditorPart editor= getEditor();
		ITypeRoot editorInput= getEditorInputJavaElement();
		if (!(editor instanceof JavaEditor) || editorInput == null) {
			return null;
		}

		int offset= region.getOffset();
		IDocument document= textViewer.getDocument();
		if (document == null)
			return null;
		try {
			char c= document.getChar(offset);
			if (c != '}')
				return null;
			JavaPairMatcher matcher= ((JavaEditor) editor).getBracketMatcher();
			if (matcher == null)
				return null;
			IRegion match= matcher.match(document, offset);
			if (match == null)
				return null;

			String delim= StubUtility.getLineDelimiterUsed(editorInput);

			CompilationUnit ast= SharedASTProviderCore.getAST(editorInput, SharedASTProviderCore.WAIT_NO, null);
			if (ast == null)
				return null;
			ASTNode bracketNode= NodeFinder.perform(ast, match.getOffset(),
					match.getLength());
			if (bracketNode == null)
				return null;
			ASTNode node;
			ASTNode parent= bracketNode.getParent();
			if (parent instanceof IfStatement) {
				IfStatement parentIfStmt= (IfStatement) parent;
				if ((parentIfStmt.getElseStatement() != null && ASTNodes.getInclusiveEnd(parentIfStmt.getElseStatement()) == offset) // if [else if]* else
						|| (parentIfStmt.getLocationInParent() == IfStatement.ELSE_STATEMENT_PROPERTY && ASTNodes.getInclusiveEnd(parentIfStmt.getThenStatement()) == offset)) { // if [else if]+ else?
					isElsePart= true;
					while (parent.getLocationInParent() == IfStatement.ELSE_STATEMENT_PROPERTY) {
						parent= parent.getParent();
					}
				}
			}
			if (bracketNode instanceof Block && !(parent instanceof Block) && !(parent instanceof SwitchStatement)) {
				node= parent;
			} else {
				node= bracketNode;
			}
			int nodeStart;
			int nodeLength;
			if (node instanceof BodyDeclaration) {
				BodyDeclaration declaration= (BodyDeclaration) node;
				Javadoc javadoc= declaration.getJavadoc();
				int lengthOfJavadoc= javadoc == null ? 0 : javadoc.getLength() +
						delim.length();
				nodeStart= node.getStartPosition() + lengthOfJavadoc;
				nodeLength= node.getLength() - lengthOfJavadoc;
			} else {
				nodeStart= node.getStartPosition();
				nodeLength= ASTNodes.getExclusiveEnd(bracketNode) - nodeStart;
			}
			fNodeRange= new SourceRange(nodeStart, nodeLength);

			int nodeStartLine= document.getLineOfOffset(nodeStart);
			int startLineOffset= document.getLineOffset(nodeStartLine);
			int nodeEndLine= document.getLineOfOffset(nodeStart + nodeLength);
			int hoveredLine= document.getLineOfOffset(offset);
			if (nodeEndLine > hoveredLine)
				nodeEndLine= hoveredLine;

			//check if line1 is visible
			final int[] topIndex= new int[1];
			StyledText textWidget= textViewer.getTextWidget();
			if (textWidget == null)
				return null;

			Display display;
			try {
				display= textWidget.getDisplay();
			} catch (SWTException ex) {
				if (ex.code == SWT.ERROR_WIDGET_DISPOSED)
					return null;
				else
					throw ex;
			}

			display.syncExec(() -> topIndex[0]= textViewer.getTopIndex());

			int topVisibleLine= topIndex[0];
			if (topVisibleLine == -1)
				return null;
			int noOfSourceLines;
			IRegion endLine;
			int skippedLines= 0;
			int wNodeStartLine= ((JavaSourceViewer) textViewer).modelLine2WidgetLine(nodeStartLine);
			int wNodeEndLine= ((JavaSourceViewer) textViewer).modelLine2WidgetLine(nodeEndLine);
			if ((nodeStartLine < topVisibleLine) || (wNodeStartLine != -1 && (wNodeEndLine - wNodeStartLine != nodeEndLine - nodeStartLine))) {
				// match not visible or content is folded - see bug 399997
				if (isElsePart) {
					return getBracketHoverInfo((IfStatement) node, bracketNode, document, editorInput, delim); // see bug 377141, 201850
				}
				noOfSourceLines= 3;
				if ((nodeEndLine - nodeStartLine) < noOfSourceLines) {
					noOfSourceLines= nodeEndLine - nodeStartLine;
				}
				skippedLines= Math.abs(nodeEndLine - nodeStartLine - noOfSourceLines);
				if (skippedLines == 1) {
					noOfSourceLines++;
					skippedLines= 0;
				}
				endLine= document.getLineInformation(nodeStartLine + noOfSourceLines - 1);
				fUpwardShiftInLines= noOfSourceLines;
				if (skippedLines > 0) {
					fBracketHoverStatus= Messages.format(JavaHoverMessages.JavaSourceHover_skippedLines, Integer.valueOf(skippedLines));
				}
			} else {
				return null;
			}
			if (fUpwardShiftInLines == 0)
				return null;

			IntStream.range(nodeStartLine, nodeStartLine + noOfSourceLines).boxed().forEach(fKeptLines::add);
			int sourceLength= (endLine.getOffset() + endLine.getLength()) - startLineOffset;
			String source= document.get(startLineOffset, sourceLength);
			String[] sourceLines= getTrimmedSource(source, 0, editorInput);
			if (sourceLines == null)
				return null;
			String[] str= new String[noOfSourceLines];
			System.arraycopy(sourceLines, 0, str, 0, noOfSourceLines);
			source= Strings.concatenate(str, delim);
			if (skippedLines > 0) {
				source= source.concat(delim).concat(JavaHoverMessages.JavaSourceHover_skippedLinesSymbol);
				fUpwardShiftInLines++;
			}
			return source;
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
			return null;
		}
	}

	/**
	 * Creates the hover text for 'else if' or 'else' closing bracket in 'if [else if]+ else?' or
	 * 'if [else if]* else' cases by stitching together all the headers when the beginning of the
	 * first 'if' is not visible in the text editor.
	 *
	 * @param ifNode the first 'if' node in the structure
	 * @param bracketNode the node at whose closing bracket the hover text is required
	 * @param document the input document of the text viewer on which the hover popup should be
	 *            shown
	 * @param editorInput the editor's input as {@link ITypeRoot}
	 * @param delim the line delimiter used for the editorInput
	 * @return the hover text for 'else if' or 'else' closing bracket in 'if [else if]+ else?' or
	 *         'if [else if]* else' cases respectively
	 * @throws BadLocationException if an attempt has been performed to access a non-existing
	 *             position in the document
	 * @since 3.9
	 */
	private String getBracketHoverInfo(IfStatement ifNode, ASTNode bracketNode, final IDocument document, final ITypeRoot editorInput, final String delim) throws BadLocationException {
		int totalSkippedLines= 0;
		String hoverText= null;

		Statement currentStatement= ifNode.getThenStatement();
		int nodeStart= ifNode.getStartPosition();
		while (currentStatement != null) {
			int nodeLength= ASTNodes.getExclusiveEnd(currentStatement) - nodeStart;
			int nodeStartLine= document.getLineOfOffset(nodeStart);
			int startLineOffset= document.getLineOffset(nodeStartLine);
			int nodeEndLine= document.getLineOfOffset(nodeStart + nodeLength);
			int nextElseLine= nodeEndLine;
			if (currentStatement != bracketNode && ifNode != null && ifNode.getElseStatement() != null) {
				int elseStartOffset= getNextElseOffset(ifNode.getThenStatement(), editorInput);
				if (elseStartOffset != -1) {
					nextElseLine= document.getLineOfOffset(elseStartOffset); // next 'else'
				}
			}
			int noOfTotalLines= (nodeEndLine == nextElseLine) ? (nodeEndLine - nodeStartLine) : (nodeEndLine - nodeStartLine + 1);
			int noOfSourceLines= 3;

			if (noOfTotalLines < noOfSourceLines) {
				noOfSourceLines= noOfTotalLines;
			}
			int noOfSkippedLines= noOfTotalLines - noOfSourceLines;
			if (noOfSkippedLines == 1) {
				noOfSourceLines++;
				noOfSkippedLines= 0;
			}

			if (noOfSourceLines > 0) {
				IntStream.range(nodeStartLine, nodeStartLine + noOfSourceLines).boxed().forEach(fKeptLines::add);
				IRegion endLine= document.getLineInformation(nodeStartLine + noOfSourceLines - 1);
				int sourceLength= (endLine.getOffset() + endLine.getLength()) - startLineOffset;
				String source= document.get(startLineOffset, sourceLength);
				String[] sourceLines= getTrimmedSource(source, 0, editorInput);
				if (sourceLines == null)
					return null;
				source= Strings.concatenate(sourceLines, delim);
				if (noOfSkippedLines > 0) {
					source= source.concat(delim).concat(JavaHoverMessages.JavaSourceHover_skippedLinesSymbol);
					fUpwardShiftInLines++;
				}

				fUpwardShiftInLines+= noOfSourceLines;
				totalSkippedLines+= noOfSkippedLines;
				if (hoverText == null) {
					hoverText= source;
				} else {
					hoverText= hoverText.concat(delim).concat(source);
				}
			}
			// advance currentStatement to the next 'else if' or 'else' statement; set it to null when no further processing is required
			// advance ifNode to the 'if' in next 'else if'; set it to null if 'else' is reached
			if (currentStatement != bracketNode && ifNode != null) {
				Statement thenStatement= ifNode.getThenStatement();
				Statement nextStatement= ifNode.getElseStatement();
				if (nextStatement instanceof IfStatement) {
					currentStatement= ((IfStatement) nextStatement).getThenStatement();
					ifNode= (IfStatement) nextStatement;
				} else {
					currentStatement= nextStatement;
					ifNode= null;
				}
				// update nodeStart to next 'else' start offset
				int nextStartOffset= getNextElseOffset(thenStatement, editorInput);
				if (nextStartOffset != -1) {
					nodeStart= nextStartOffset;
				} else {
					nodeStart= nextStatement.getStartPosition();
				}
			} else {
				currentStatement= null;
			}
		}

		if (fUpwardShiftInLines == 0)
			return null;
		if ((totalSkippedLines) > 0) {
			fBracketHoverStatus= Messages.format(JavaHoverMessages.JavaSourceHover_skippedLines, Integer.valueOf(totalSkippedLines));
		}
		return hoverText;
	}

	private String getTextBlockHoverInfo(final ITextViewer textViewer, IRegion region) {
		String hoverText= null;
		IEditorPart editor= getEditor();
		ITypeRoot input= getEditorInputJavaElement();
		if (!(editor instanceof JavaEditor) || input == null || region == null) {
			return null;
		}
		IDocument document= textViewer.getDocument();
		if (document == null)
			return null;

		IDocumentExtension3 docExtension= null;
		if (document instanceof IDocumentExtension3)
			docExtension= (IDocumentExtension3) document;
		else
			return null;
		IJavaProject javaProject= getProject(editor);
		if (javaProject == null) {
			return null;
		}

		try {
			int selectionOffset= region.getOffset();
			//boolean setCaratPosition= selection.getLength() > 0 ? false : true;
			ITypedRegion partition= docExtension.getPartition(IJavaPartitions.JAVA_PARTITIONING, selectionOffset, false);
			if (partition == null || !isTextBlock( partition, javaProject)) {
				return null;
			}
			CompilationUnit ast= SharedASTProviderCore.getAST(input, SharedASTProviderCore.WAIT_NO, null);
			if (ast == null) {
				return null;
			}
			ASTNode textBlockNode= NodeFinder.perform(ast, partition.getOffset(),
					partition.getLength());
			if (!(textBlockNode instanceof TextBlock)) {
				return null;
			}
			TextBlock textBlock= (TextBlock) textBlockNode;
			return textBlock.getLiteralValue();
		} catch (BadLocationException | BadPartitioningException e) {
			//do nothing
		}
		return hoverText;
	}

	private boolean isTextBlock(ITypedRegion partition, IJavaProject javaProject) {
		if (!JavaModelUtil.is15OrHigher(javaProject)) {
			return false;
		}
		boolean isTextBlock= true;
		String partitionType= partition.getType();
		if (IJavaPartitions.JAVA_MULTI_LINE_STRING.equals(partitionType)) {
			isTextBlock= true;
		}
		return isTextBlock;
	}

	private IJavaProject getProject(IEditorPart editor) {
		IJavaProject javaProject= null;
		if (editor != null) {
			ITypeRoot inputJavaElement= EditorUtility.getEditorInputJavaElement(editor, false);
			javaProject= inputJavaElement.getJavaProject();
		}
		return javaProject;
	}

	private int getNextElseOffset(Statement then, ITypeRoot editorInput) {
		int thenEnd= ASTNodes.getExclusiveEnd(then);
		try {
			TokenScanner scanner= new TokenScanner(editorInput);
			return scanner.getNextStartOffset(thenEnd, true);
		} catch (CoreException e) {
			// ignore
		}
		return -1;
	}

	/**
	 * Returns the trimmed source lines.
	 *
	 * @param source the source string, could be <code>null</code>
	 * @param offset the offset to start leading comments removal from
	 * @param javaElement the java element
	 * @return the trimmed source lines or <code>null</code>
	 */
	private String[] getTrimmedSource(String source, int offset, IJavaElement javaElement) {
		if (source == null)
			return null;
		source= removeLeadingComments(source, offset);
		String[] sourceLines= Strings.convertIntoLines(source);
		Strings.trimIndentation(sourceLines, javaElement.getJavaProject());
		return sourceLines;
	}

	private String removeLeadingComments(String source, int offset) {
		try (JavaCodeReader reader= new JavaCodeReader()) {
			IDocument document= new Document(source);
			reader.configureForwardReader(document, offset, document.getLength() - offset, true, false);
			int c= reader.read();
			while (c != -1 && (c == '\r' || c == '\n')) {
				c= reader.read();
			}
			int i= reader.getOffset();
			if (i < 0) {
				return source;
			}
			if (i == offset) {
				return source;
			}
			return source.substring(i);
		} catch (IOException ex) {
			JavaPlugin.log(ex);
			return source;
		}
	}

	/*
	 * @see org.eclipse.jface.text.ITextHoverExtension#getHoverControlCreator()
	 * @since 3.0
	 */
	@Override
	public IInformationControlCreator getHoverControlCreator() {
		if (fUpwardShiftInLines > 0)
			return createInformationControlCreator(false, fBracketHoverStatus, true);
		else
			return createInformationControlCreator(false, EditorsUI.getTooltipAffordanceString(), true);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.hover.AbstractJavaEditorTextHover#getInformationPresenterControlCreator()
	 * @since 3.0
	 */
	@Override
	public IInformationControlCreator getInformationPresenterControlCreator() {
		if (fUpwardShiftInLines > 0)
			return createInformationControlCreator(false, fBracketHoverStatus, true);
		else
			return createInformationControlCreator(true, EditorsUI.getTooltipAffordanceString(), true);
	}

	/**
	 * Returns the information control creator.
	 *
	 * @param isResizable <code>true</code> if resizable
	 * @param statusFieldText the text to be used in the optional status field or <code>null</code>
	 *            if the status field should be hidden
	 * @param doShiftUp <code>true</code> iff {@link #fUpwardShiftInLines} should be considered
	 * @return the information control creator
	 * @since 3.8
	 */
	private IInformationControlCreator createInformationControlCreator(final boolean isResizable, final String statusFieldText, final boolean doShiftUp) {
		return parent -> {
			final IEditorPart editor= getEditor();
			int orientation= SWT.NONE;
			if (editor instanceof IWorkbenchPartOrientation)
				orientation= ((IWorkbenchPartOrientation) editor).getOrientation();

			return createControl(parent, editor, isResizable, orientation, statusFieldText, doShiftUp, null);
		};
	}

	private IInformationControl createControl(final Shell parent, final IEditorPart editor, final boolean isResizable, final int orientation, final String statusFieldText, final boolean doShiftUp, final ISourceViewer mimicSourceViewer) {
		return new SourceViewerInformationControl(parent, editor, isResizable, orientation, statusFieldText) {

			{
				// Just re-use content of displayed hover viewer when 'entering into' it
				if (mimicSourceViewer != null) {
					setContentFrom(mimicSourceViewer);
				}
			}

			@Override
			public void setLocation(Point location) {
				Point loc= location;
				if (doShiftUp && fUpwardShiftInLines > 0) {
					Point size= super.computeSizeConstraints(0, fUpwardShiftInLines + 1);
					//bracket hover is rendered above '}'
					int y= location.y - size.y - 5; //AbstractInformationControlManager.fMarginY = 5
					Rectangle trim= computeTrim();
					loc= new Point(location.x + trim.x - getViewer().getTextWidget().getLeftMargin(), y - trim.height - trim.y);
				}
				super.setLocation(loc);
			}

			@Override
			public Point computeSizeConstraints(int widthInChars, int heightInChars) {
				if (doShiftUp && fUpwardShiftInLines > 0) {
					Point sizeConstraints= super.computeSizeConstraints(widthInChars, heightInChars);
					return new Point(sizeConstraints.x, 0); //set height as 0 to ensure selection of bottom anchor in AbstractInformationControlManager.computeInformationControlLocation(...)
				} else {
					return super.computeSizeConstraints(widthInChars, heightInChars);
				}
			}

			@Override
			public void setSize(int width, int height) {
				if (doShiftUp && fUpwardShiftInLines != 0) {
					//compute the correct height of hover, this was set to 0 in computeSizeConstraints(..)
					Point size= super.computeSizeConstraints(0, fUpwardShiftInLines);
					Rectangle trim= computeTrim();
					super.setSize(width, size.y + trim.height - trim.y);
				} else {
					super.setSize(width, height);
				}
			}

			@SuppressWarnings("hiding")
			@Override
			public IInformationControlCreator getInformationPresenterControlCreator() {
				if (doShiftUp && fUpwardShiftInLines > 0) {
					// Hack: We don't wan't to have auto-enrichment when the mouse moves into the hover,
					// but we do want F2 to persist the hover. The framework has no way to distinguish the
					// two requests, so we have to implement this aspect.
					for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
						if ("canMoveIntoInformationControl".equals(element.getMethodName()) //$NON-NLS-1$
								&& "org.eclipse.jface.text.AbstractHoverInformationControlManager".equals(element.getClassName())) //$NON-NLS-1$
							return null; //do not enrich bracket hover
					}
					return parent -> createControl(parent, null, false, orientation, statusFieldText, false, getViewer());
				} else {
					return parent -> createControl(parent, null, true, orientation, null, false, getViewer());
				}
			}

			@Override
			public void setInput(Object input) {
				// ignore when hover viewer content was re-used
				if (mimicSourceViewer == null) {
					super.setInput(input);
				}
			}

			@Override
			public void setInformation(String content) {
				// ignore when hover viewer content was re-used
				if (mimicSourceViewer == null) {
					super.setInformation(content);
				}
			}
		};
	}
}
