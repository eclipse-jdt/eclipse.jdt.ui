/*******************************************************************************
 * Copyright (c) 2006, 2025 IBM Corporation and others.
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
package org.eclipse.jdt.ui.text.folding;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.IProjectionListener;
import org.eclipse.jface.text.source.projection.IProjectionPosition;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionViewer;

import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.WhileStatement;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.text.DocumentCharacterIterator;

/**
 * Updates the projection model of a class file or compilation unit.
 * <p>
 * Clients may instantiate or subclass. Subclasses must make sure to always call the superclass'
 * code when overriding methods that are marked with "subclasses may extend".
 * </p>
 *
 * @since 3.0 (internal)
 * @since 3.2 (API)
 */
public class DefaultJavaFoldingStructureProvider implements IJavaFoldingStructureProvider, IJavaFoldingStructureProviderExtension {
	/**
	 * A context that contains the information needed to compute the folding structure of an
	 * {@link ICompilationUnit} or an {@link IClassFile}. Computed folding regions are collected
	 * via
	 * {@linkplain #addProjectionRange(DefaultJavaFoldingStructureProvider.JavaProjectionAnnotation, Position) addProjectionRange}.
	 */
	protected final class FoldingStructureComputationContext {
		private final ProjectionAnnotationModel fModel;
		private final IDocument fDocument;

		private final boolean fAllowCollapsing;

		private IType fFirstType;
		private boolean fHasHeaderComment;
		private LinkedHashMap<JavaProjectionAnnotation, Position> fMap= new LinkedHashMap<>();
		private IScanner fDefaultScanner; // this one may or not be the shared DefaultJavaFoldingStructureProvider.fSharedScanner
		private IScanner fScannerForProject;

		private Deque<Integer> fOpenCustomRegionStartPositions = new ArrayDeque<>();
		private Set<IRegion> fCurrentCustomRegions = new HashSet<>();
		private int fLastScannedIndex;

		private FoldingStructureComputationContext(IDocument document, ProjectionAnnotationModel model, boolean allowCollapsing, IScanner scanner) {
			Assert.isNotNull(document);
			Assert.isNotNull(model);
			fDocument= document;
			fModel= model;
			fAllowCollapsing= allowCollapsing;
			fDefaultScanner= scanner;
		}

		private void setFirstType(IType type) {
			if (hasFirstType())
				throw new IllegalStateException();
			fFirstType= type;
		}

		boolean hasFirstType() {
			return fFirstType != null;
		}

		private IType getFirstType() {
			return fFirstType;
		}

		private boolean hasHeaderComment() {
			return fHasHeaderComment;
		}

		private void setHasHeaderComment() {
			fHasHeaderComment= true;
		}

		/**
		 * Returns <code>true</code> if newly created folding regions may be collapsed,
		 * <code>false</code> if not. This is usually <code>false</code> when updating the
		 * folding structure while typing; it may be <code>true</code> when computing or restoring
		 * the initial folding structure.
		 *
		 * @return <code>true</code> if newly created folding regions may be collapsed,
		 *         <code>false</code> if not
		 */
		public boolean allowCollapsing() {
			return fAllowCollapsing;
		}

		/**
		 * Returns the document which contains the code being folded.
		 *
		 * @return the document which contains the code being folded
		 */
		private IDocument getDocument() {
			return fDocument;
		}

		private ProjectionAnnotationModel getModel() {
			return fModel;
		}

		private IScanner getScanner() {
			if (fScannerForProject != null) {
				return fScannerForProject;
			}
			IJavaProject javaProject= fInput != null ? fInput.getJavaProject(): null;
			if (javaProject != null) {
				String projectSource= javaProject.getOption(JavaCore.COMPILER_SOURCE, true);
				String projectCompliance= javaProject.getOption(JavaCore.COMPILER_COMPLIANCE, true);
				if (!JavaCore.getOption(JavaCore.COMPILER_COMPLIANCE).equals(projectCompliance)
						|| !JavaCore.getOption(JavaCore.COMPILER_SOURCE).equals(projectSource)) {
					return fScannerForProject= ToolFactory.createScanner(true, false, false, projectSource, projectCompliance);
				}
			}
			if (fDefaultScanner == null)
				fDefaultScanner= ToolFactory.createScanner(true, false, false, false);
			return fDefaultScanner;
		}

		private void setSource(char[] source) {
			if (fDefaultScanner != null)
				fDefaultScanner.setSource(source);
			if (fScannerForProject != null)
				fScannerForProject.setSource(source);
		}

		/**
		 * Adds a projection (folding) region to this context. The created annotation / position
		 * pair will be added to the {@link ProjectionAnnotationModel} of the
		 * {@link ProjectionViewer} of the editor.
		 *
		 * @param annotation the annotation to add
		 * @param position the corresponding position
		 */
		public void addProjectionRange(JavaProjectionAnnotation annotation, Position position) {
			fMap.put(annotation, position);
		}

		/**
		 * Returns <code>true</code> if header comments should be collapsed.
		 *
		 * @return <code>true</code> if header comments should be collapsed
		 */
		public boolean collapseHeaderComments() {
			return fAllowCollapsing && fCollapseHeaderComments;
		}

		/**
		 * Returns <code>true</code> if import containers should be collapsed.
		 *
		 * @return <code>true</code> if import containers should be collapsed
		 */
		public boolean collapseImportContainer() {
			return fAllowCollapsing && fCollapseImportContainer;
		}

		/**
		 * Returns <code>true</code> if inner types should be collapsed.
		 *
		 * @return <code>true</code> if inner types should be collapsed
		 */
		public boolean collapseInnerTypes() {
			return fAllowCollapsing && fCollapseInnerTypes;
		}

		/**
		 * Returns <code>true</code> if javadoc comments should be collapsed.
		 *
		 * @return <code>true</code> if javadoc comments should be collapsed
		 */
		public boolean collapseJavadoc() {
			return fAllowCollapsing && fCollapseJavadoc;
		}

		/**
		 * Returns <code>true</code> if methods should be collapsed.
		 *
		 * @return <code>true</code> if methods should be collapsed
		 */
		public boolean collapseMembers() {
			return fAllowCollapsing && fCollapseMembers;
		}

		/**
		 * Returns <code>true</code> if custom regions should be collapsed.
		 *
		 * @return <code>true</code> if custom regions should be collapsed
		 * @since 3.35
		 */
		public boolean collapseCustomRegions() {
			return fAllowCollapsing && fCollapseCustomRegions;
		}

	}

	/**
	 * A {@link ProjectionAnnotation} for java code.
	 */
	protected static final class JavaProjectionAnnotation extends ProjectionAnnotation {

		private IJavaElement fJavaElement;
		private boolean fIsComment;

		/**
		 * Creates a new projection annotation.
		 *
		 * @param isCollapsed <code>true</code> to set the initial state to collapsed,
		 *        <code>false</code> to set it to expanded
		 * @param element the java element this annotation refers to
		 * @param isComment <code>true</code> for a foldable comment, <code>false</code> for a
		 *        foldable code element
		 */
		public JavaProjectionAnnotation(boolean isCollapsed, IJavaElement element, boolean isComment) {
			super(isCollapsed);
			fJavaElement= element;
			fIsComment= isComment;
		}

		IJavaElement getElement() {
			return fJavaElement;
		}

		void setElement(IJavaElement element) {
			fJavaElement= element;
		}

		boolean isComment() {
			return fIsComment;
		}

		void setIsComment(boolean isComment) {
			fIsComment= isComment;
		}

		/*
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "JavaProjectionAnnotation:\n" + //$NON-NLS-1$
					"\telement: \t"+ fJavaElement.toString() + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
					"\tcollapsed: \t" + isCollapsed() + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
					"\tcomment: \t" + isComment() + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private static final class Tuple {
		JavaProjectionAnnotation annotation;
		Position position;
		Tuple(JavaProjectionAnnotation annotation, Position position) {
			this.annotation= annotation;
			this.position= position;
		}
	}

	private class FoldingVisitor extends ASTVisitor {

		private FoldingStructureComputationContext ctx;

		public FoldingVisitor(FoldingStructureComputationContext ctx) {
			this.ctx= ctx;
		}

		@Override
		public boolean visit(CompilationUnit node) {
			List<ImportDeclaration> imports= node.imports();
			if (imports.size() > 1) {
				int start= imports.get(0).getStartPosition();
				ImportDeclaration lastImport= imports.get(imports.size() - 1);
				int end= lastImport.getStartPosition() + lastImport.getLength();
				includelastLine = true;
				createFoldingRegion(start, end - start, ctx.collapseMembers());
			}
			return super.visit(node);
		}

		@Override
		public boolean visit(TypeDeclaration node) {
			if (node.isMemberTypeDeclaration() || node.isLocalTypeDeclaration()) {
				int start= node.getName().getStartPosition();
				int end= node.getStartPosition() + node.getLength();
				createFoldingRegion(start, end - start, ctx.collapseMembers());
			}
			return true;
		}

		@Override
		public boolean visit(MethodDeclaration node) {
			int start= node.getName().getStartPosition();
			int end= node.getStartPosition() + node.getLength();
			createFoldingRegion(start, end - start, ctx.collapseMembers());
			return true;
		}

		@Override
		public boolean visit(IfStatement node) {
			int start= node.getStartPosition();
			int end= getEndPosition(node.getThenStatement());
			createFoldingRegion(start, end - start, ctx.collapseMembers());
			node.getThenStatement().accept(this);
			if (node.getElseStatement() != null) {
				if (node.getElseStatement() instanceof IfStatement) {
					Statement elseIfStatement= node.getElseStatement();
					start= findElseKeywordStart(elseIfStatement);
					end= getEndPosition(((IfStatement) elseIfStatement).getThenStatement());
					createFoldingRegion(start, end - start, ctx.collapseMembers());
					node.getElseStatement().accept(this);
				} else {
					start= findElseKeywordStart(node.getElseStatement());
					end= getEndPosition(node.getElseStatement());
					createFoldingRegion(start, end - start, ctx.collapseMembers());
					node.getElseStatement().accept(this);
				}
			}
			return false;
		}

		@Override
		public boolean visit(TryStatement node) {
			createFoldingRegionForTryBlock(node);
			node.getBody().accept(this);
			for (Object obj : node.catchClauses()) {
				CatchClause catchClause= (CatchClause) obj;
				createFoldingRegionForCatchClause(catchClause);
				catchClause.accept(this);
			}
			if (node.getFinally() != null) {
				createFoldingRegionForFinallyBlock(node);
				node.getFinally().accept(this);
			}
			return false;
		}

		@Override
		public boolean visit(WhileStatement node) {
			createFoldingRegionForStatement(node);
			node.getBody().accept(this);
			return false;
		}

		@Override
		public boolean visit(ForStatement node) {
			createFoldingRegionForStatement(node);
			node.getBody().accept(this);
			return false;
		}

		@Override
		public boolean visit(EnhancedForStatement node) {
			createFoldingRegionForStatement(node);
			node.getBody().accept(this);
			return false;
		}

		@Override
		public boolean visit(DoStatement node) {
			createFoldingRegionForStatement(node);
			node.getBody().accept(this);
			return false;
		}

		@Override
		public boolean visit(SynchronizedStatement node) {
			createFoldingRegion(node, ctx.collapseMembers());
			node.getBody().accept(this);
			return false;
		}

		@Override
		public boolean visit(LambdaExpression node) {
			if (node.getBody() instanceof Block) {
				createFoldingRegion(node.getBody(), ctx.collapseMembers());
				node.getBody().accept(this);
			}
			return false;
		}

		@Override
		public boolean visit(AnonymousClassDeclaration node) {
			createFoldingRegion(node, ctx.collapseMembers());
			return true;
		}

		@Override
		public boolean visit(EnumDeclaration node) {
			createFoldingRegion(node, ctx.collapseMembers());
			return true;
		}

		@Override
		public boolean visit(Initializer node) {
			createFoldingRegion(node, ctx.collapseMembers());
			return true;
		}

		private void createFoldingRegion(ASTNode node, boolean collapse) {
			createFoldingRegion(node.getStartPosition(), node.getLength(), collapse);
		}

		private void createFoldingRegion(int start, int length, boolean collapse) {
			if (length > 0) {
				IRegion region= new Region(start, length);
				IRegion aligned= alignRegion(region, ctx);

				if (aligned != null && isMultiline(aligned)) {
					Position position= new Position(aligned.getOffset(), aligned.getLength());
					JavaProjectionAnnotation annotation= new JavaProjectionAnnotation(collapse, null, false);
					ctx.addProjectionRange(annotation, position);
				}
			}
		}

		private boolean isMultiline(IRegion region) {
			try {
				IDocument document= ctx.getDocument();
				int startLine= document.getLineOfOffset(region.getOffset());
				int endLine= document.getLineOfOffset(region.getOffset() + region.getLength());
				return endLine > startLine;
			} catch (BadLocationException e) {
				return false;
			}
		}

		private int findElseKeywordStart(ASTNode node) {
			try {
				IDocument document= ctx.getDocument();
				int startSearch= node.getParent().getStartPosition();
				int endSearch= node.getStartPosition();

				String text= document.get(startSearch, endSearch - startSearch);
				int index= text.lastIndexOf("else"); //$NON-NLS-1$
				if (index >= 0) {
					return startSearch + index;
				}
			} catch (BadLocationException e) {
			}
			return node.getStartPosition();
		}

		private int getEndPosition(Statement statement) {
			if (statement instanceof Block) {
				return statement.getStartPosition() + statement.getLength();
			} else {
				try {
					IDocument document= ctx.getDocument();
					int start= statement.getStartPosition();
					int line= document.getLineOfOffset(start);
					return document.getLineOffset(line + 1);
				} catch (BadLocationException e) {
					return statement.getStartPosition() + statement.getLength();
				}
			}
		}

		private void createFoldingRegionForStatement(ASTNode node) {
			int start= node.getStartPosition();
			int length= node.getLength();
			if (!(node instanceof Block)) {
				try {
					IDocument document= ctx.getDocument();
					int endLine= document.getLineOfOffset(start + length - 1);
					if (endLine + 1 < document.getNumberOfLines()) {
						String currentIndent= getIndentOfLine(document, endLine);
						String nextIndent= getIndentOfLine(document, endLine + 1);
						if (nextIndent.length() > currentIndent.length()) {
							int nextLineEndOffset= document.getLineOffset(endLine + 2) - 1;
							length= nextLineEndOffset - start;
						} else {
							length= document.getLineOffset(endLine + 1) - start;
						}
					} else {
						length= document.getLength() - start;
					}
				} catch (BadLocationException e) {
				}
			}
			createFoldingRegion(start, length, ctx.collapseMembers());
		}

		private String getIndentOfLine(IDocument document, int line) throws BadLocationException {
			IRegion region= document.getLineInformation(line);
			int lineStart= region.getOffset();
			int lineLength= region.getLength();

			int whiteSpaceEnd= lineStart;
			while (whiteSpaceEnd < lineStart + lineLength) {
				char c= document.getChar(whiteSpaceEnd);
				if (!Character.isWhitespace(c)) {
					break;
				}
				whiteSpaceEnd++;
			}
			return document.get(lineStart, whiteSpaceEnd - lineStart);
		}

		private void createFoldingRegionForTryBlock(TryStatement node) {
			int start= node.getStartPosition();
			int end= getEndPosition(node.getBody());
			createFoldingRegion(start, end - start, ctx.collapseMembers());
		}

		private void createFoldingRegionForCatchClause(CatchClause catchClause) {
			int start= catchClause.getStartPosition();
			int end= getEndPosition(catchClause.getBody());
			createFoldingRegion(start, end - start, ctx.collapseMembers());
		}

		private void createFoldingRegionForFinallyBlock(TryStatement node) {
			Block finallyBlock= node.getFinally();
			int start= findFinallyKeywordStart(node);
			int end= getEndPosition(finallyBlock);
			createFoldingRegion(start, end - start, ctx.collapseMembers());
		}

		private int findFinallyKeywordStart(TryStatement node) {
			try {
				IDocument document= ctx.getDocument();
				int startSearch= node.getStartPosition();
				int endSearch= node.getFinally().getStartPosition();
				String text= document.get(startSearch, endSearch - startSearch);
				int index= text.lastIndexOf("finally"); //$NON-NLS-1$

				if (index >= 0) {
					return startSearch + index;
				}
			} catch (BadLocationException e) {
			}
			return node.getFinally().getStartPosition();
		}
	}

	/**
	 * Filter for annotations.
	 */
	private interface Filter {
		boolean match(JavaProjectionAnnotation annotation);
	}

	/**
	 * Matches comments.
	 */
	private static final class CommentFilter implements Filter {
		@Override
		public boolean match(JavaProjectionAnnotation annotation) {
			if (annotation.isComment() && !annotation.isMarkedDeleted()) {
				return true;
			}
			return false;
		}
	}

	/**
	 * Matches members.
	 */
	private static final class MemberFilter implements Filter {
		@Override
		public boolean match(JavaProjectionAnnotation annotation) {
			if (!annotation.isComment() && !annotation.isMarkedDeleted()) {
				IJavaElement element= annotation.getElement();
				if (element instanceof IMember) {
					if (element.getElementType() != IJavaElement.TYPE || ((IMember) element).getDeclaringType() != null) {
						return true;
					}
				}
			}
			return false;
		}
	}

	/**
	 * Matches java elements contained in a certain set.
	 */
	private static final class JavaElementSetFilter implements Filter {
		private final Set<? extends IJavaElement> fSet;
		private final boolean fMatchCollapsed;

		private JavaElementSetFilter(Set<? extends IJavaElement> set, boolean matchCollapsed) {
			fSet= set;
			fMatchCollapsed= matchCollapsed;
		}

		@Override
		public boolean match(JavaProjectionAnnotation annotation) {
			boolean stateMatch= fMatchCollapsed == annotation.isCollapsed();
			if (stateMatch && !annotation.isComment() && !annotation.isMarkedDeleted()) {
				IJavaElement element= annotation.getElement();
				if (fSet.contains(element)) {
					return true;
				}
			}
			return false;
		}
	}

	private class ElementChangedListener implements IElementChangedListener {

		/*
		 * @see org.eclipse.jdt.core.IElementChangedListener#elementChanged(org.eclipse.jdt.core.ElementChangedEvent)
		 */
		@Override
		public void elementChanged(ElementChangedEvent e) {
			IJavaElementDelta delta= findElement(fInput, e.getDelta());
			if (delta != null && (delta.getFlags() & (IJavaElementDelta.F_CONTENT | IJavaElementDelta.F_CHILDREN)) != 0) {

				if (shouldIgnoreDelta(e.getDelta().getCompilationUnitAST(), delta))
					return;

				fUpdatingCount++;
				try {
					update(createContext(false));
				} finally {
					fUpdatingCount--;
				}
			}
		}

		/**
		 * Ignore the delta if there are errors on the caret line.
		 * <p>
		 * We don't ignore the delta if an import is added and the
		 * caret isn't inside the import container.
		 * </p>
		 *
		 * @param ast the compilation unit AST
		 * @param delta the Java element delta for the given AST element
		 * @return <code>true</code> if the delta should be ignored
		 * @since 3.3
		 */
		private boolean shouldIgnoreDelta(CompilationUnit ast, IJavaElementDelta delta) {
			if (ast == null)
				return false; // can't compute

			IDocument document= getDocument();
			if (document == null)
				return false; // can't compute

			JavaEditor editor= fEditor;
			if (editor == null || editor.getCachedSelectedRange() == null)
				return false; // can't compute

			try {
				if (delta.getAffectedChildren().length == 1 && delta.getAffectedChildren()[0].getElement() instanceof IImportContainer) {
					IJavaElement elem= SelectionConverter.getElementAtOffset(ast.getTypeRoot(), new TextSelection(editor.getCachedSelectedRange().x, editor.getCachedSelectedRange().y));
					if (!(elem instanceof IImportDeclaration))
						return false;
				}
			} catch (JavaModelException e) {
				return false; // can't compute
			}

			int caretLine= 0;
			try {
				caretLine= document.getLineOfOffset(editor.getCachedSelectedRange().x) + 1;
			} catch (BadLocationException x) {
				return false; // can't compute
			}

			if (caretLine > 0) {
				for (IProblem problem : ast.getProblems()) {
					if (problem.isError() && caretLine == problem.getSourceLineNumber()) {
						return true;
					}
				}
			}

			return false;
		}

		private IJavaElementDelta findElement(IJavaElement target, IJavaElementDelta delta) {

			if (delta == null || target == null)
				return null;

			IJavaElement element= delta.getElement();

			if (element.getElementType() > IJavaElement.CLASS_FILE)
				return null;

			if (target.equals(element))
				return delta;

			for (IJavaElementDelta child : delta.getAffectedChildren()) {
				IJavaElementDelta d= findElement(target, child);
				if (d != null)
					return d;
			}

			return null;
		}
	}


	/**
	 * Projection position that will return two foldable regions: one folding away the region from
	 * after the '/**' to the beginning of the content, the other from after the first content line
	 * until after the comment.
	 */
	private static final class CommentPosition extends Position implements IProjectionPosition {
		CommentPosition(int offset, int length) {
			super(offset, length);
		}

		/*
		 * @see org.eclipse.jface.text.source.projection.IProjectionPosition#computeFoldingRegions(org.eclipse.jface.text.IDocument)
		 */
		@Override
		public IRegion[] computeProjectionRegions(IDocument document) throws BadLocationException {
			DocumentCharacterIterator sequence= new DocumentCharacterIterator(document, offset, offset + length);
			int prefixEnd= 0;
			int contentStart= findFirstContent(sequence, prefixEnd);

			int firstLine= document.getLineOfOffset(offset + prefixEnd);
			int captionLine= document.getLineOfOffset(offset + contentStart);
			int lastLine= document.getLineOfOffset(offset + length);

			Assert.isTrue(firstLine <= captionLine, "first folded line is greater than the caption line"); //$NON-NLS-1$
			Assert.isTrue(captionLine <= lastLine, "caption line is greater than the last folded line"); //$NON-NLS-1$

			IRegion preRegion;
			if (firstLine < captionLine) {
				int preOffset= document.getLineOffset(firstLine);
				IRegion preEndLineInfo= document.getLineInformation(captionLine);
				int preEnd= preEndLineInfo.getOffset();
				preRegion= new Region(preOffset, preEnd - preOffset);
			} else {
				preRegion= null;
			}

			if (captionLine < lastLine) {
				int postOffset= document.getLineOffset(captionLine + 1);
				int postLength= offset + length - postOffset;
				if (postLength > 0) {
					IRegion postRegion= new Region(postOffset, postLength);
					if (preRegion == null)
						return new IRegion[] { postRegion };
					return new IRegion[] { preRegion, postRegion };
				}
			}

			if (preRegion != null)
				return new IRegion[] { preRegion };

			return null;
		}

		/**
		 * Finds the offset of the first identifier part within <code>content</code>. Returns 0 if
		 * none is found.
		 *
		 * @param content the content to search
		 * @param prefixEnd the end of the prefix
		 * @return the first index of a unicode identifier part, or zero if none can be found
		 */
		private int findFirstContent(final CharSequence content, int prefixEnd) {
			int lenght= content.length();
			for (int i= prefixEnd; i < lenght; i++) {
				if (Character.isUnicodeIdentifierPart(content.charAt(i)))
					return i;
			}
			return 0;
		}

		/*
		 * @see org.eclipse.jface.text.source.projection.IProjectionPosition#computeCaptionOffset(org.eclipse.jface.text.IDocument)
		 */
		@Override
		public int computeCaptionOffset(IDocument document) throws BadLocationException {
			DocumentCharacterIterator sequence= new DocumentCharacterIterator(document, offset, offset + length);
			return findFirstContent(sequence, 0);
		}
	}

	/**
	 * Projection position that will return two foldable regions: one folding away
	 * the lines before the one containing the simple name of the java element, one
	 * folding away any lines after the caption.
	 */
	private static final class JavaElementPosition extends Position implements IProjectionPosition {

		private IMember fMember;

		public JavaElementPosition(int offset, int length, IMember member) {
			super(offset, length);
			Assert.isNotNull(member);
			fMember= member;
		}

		public void setMember(IMember member) {
			Assert.isNotNull(member);
			fMember= member;
		}

		/*
		 * @see org.eclipse.jface.text.source.projection.IProjectionPosition#computeFoldingRegions(org.eclipse.jface.text.IDocument)
		 */
		@Override
		public IRegion[] computeProjectionRegions(IDocument document) throws BadLocationException {
			int nameStart= offset;
			try {
				/* The member's name range may not be correct. However,
				 * reconciling would trigger another element delta which would
				 * lead to reentrant situations. Therefore, we optimistically
				 * assume that the name range is correct, but double check the
				 * received lines below. */
				ISourceRange nameRange= fMember.getNameRange();
				if (nameRange != null)
					nameStart= nameRange.getOffset();

			} catch (JavaModelException e) {
				// ignore and use default
			}

			int firstLine= document.getLineOfOffset(offset);
			int captionLine= document.getLineOfOffset(nameStart);
			int lastLine= document.getLineOfOffset(offset + length);

			/* see comment above - adjust the caption line to be inside the
			 * entire folded region, and rely on later element deltas to correct
			 * the name range. */
			if (captionLine < firstLine)
				captionLine= firstLine;
			if (captionLine > lastLine)
				captionLine= lastLine;

			IRegion preRegion;
			if (firstLine < captionLine) {
				int preOffset= document.getLineOffset(firstLine);
				IRegion preEndLineInfo= document.getLineInformation(captionLine);
				int preEnd= preEndLineInfo.getOffset();
				preRegion= new Region(preOffset, preEnd - preOffset);
			} else {
				preRegion= null;
			}

			if (captionLine < lastLine) {
				int postOffset= document.getLineOffset(captionLine + 1);
				int postLength= offset + length - postOffset;
				if (postLength > 0) {
					IRegion postRegion= new Region(postOffset, postLength);
					if (preRegion == null)
						return new IRegion[] { postRegion };
					return new IRegion[] { preRegion, postRegion };
				}
			}
			if (preRegion != null)
				return new IRegion[] { preRegion };
			return null;
		}

		/*
		 * @see org.eclipse.jface.text.source.projection.IProjectionPosition#computeCaptionOffset(org.eclipse.jface.text.IDocument)
		 */
		@Override
		public int computeCaptionOffset(IDocument document) throws BadLocationException {
			int nameStart= offset;
			try {
				// need a reconcile here?
				ISourceRange nameRange= fMember.getNameRange();
				if (nameRange != null)
					nameStart= nameRange.getOffset();
			} catch (JavaModelException e) {
				// ignore and use default
			}
			return nameStart - offset;
		}

	}

	/**
	 * Internal projection listener.
	 */
	private final class ProjectionListener implements IProjectionListener {
		private ProjectionViewer fViewer;

		/**
		 * Registers the listener with the viewer.
		 *
		 * @param viewer the viewer to register a listener with
		 */
		public ProjectionListener(ProjectionViewer viewer) {
			Assert.isLegal(viewer != null);
			fViewer= viewer;
			fViewer.addProjectionListener(this);
		}

		/**
		 * Disposes of this listener and removes the projection listener from the viewer.
		 */
		public void dispose() {
			if (fViewer != null) {
				fViewer.removeProjectionListener(this);
				fViewer= null;
			}
		}

		/*
		 * @see org.eclipse.jface.text.source.projection.IProjectionListener#projectionEnabled()
		 */
		@Override
		public void projectionEnabled() {
			handleProjectionEnabled();
		}

		/*
		 * @see org.eclipse.jface.text.source.projection.IProjectionListener#projectionDisabled()
		 */
		@Override
		public void projectionDisabled() {
			handleProjectionDisabled();
		}
	}

	boolean includelastLine = false;

	/* context and listeners */
	private JavaEditor fEditor;
	private ProjectionListener fProjectionListener;
	private IJavaElement fInput;
	private IElementChangedListener fElementListener;
	private IPropertyChangeListener fPropertyChangeListener = new IPropertyChangeListener() {
	    @Override
	    public void propertyChange(PropertyChangeEvent event) {
	        initializePreferences();
	        initialize();
	    }
	};

	/* preferences */
	private boolean fCollapseJavadoc= false;
	private boolean fCollapseImportContainer= true;
	private boolean fCollapseInnerTypes= true;
	private boolean fCollapseMembers= false;
	private boolean fCollapseHeaderComments= true;
	private boolean fCollapseCustomRegions= false;
	private boolean fNewFolding;

	private boolean fCustomFoldingRegionsEnabled;
	private char[] fCustomFoldingRegionBegin;
	private char[] fCustomFoldingRegionEnd;

	/* filters */
	/** Member filter, matches nested members (but not top-level types). */
	private final Filter fMemberFilter = new MemberFilter();
	/** Comment filter, matches comments. */
	private final Filter fCommentFilter = new CommentFilter();

	/**
	 * Reusable scanner.
	 * @since 3.3
	 */
	private IScanner fSharedScanner= ToolFactory.createScanner(true, false, false, false);

	private volatile int fUpdatingCount= 0;

	/**
	 * Creates a new folding provider. It must be
	 * {@link #install(ITextEditor, ProjectionViewer) installed} on an editor/viewer pair before it
	 * can be used, and {@link #uninstall() uninstalled} when not used any longer.
	 * <p>
	 * The projection state may be reset by calling {@link #initialize()}.
	 * </p>
	 */
	public DefaultJavaFoldingStructureProvider() {
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Subclasses may extend.
	 * </p>
	 *
	 * @param editor {@inheritDoc}
	 * @param viewer {@inheritDoc}
	 */
	@Override
	public void install(ITextEditor editor, ProjectionViewer viewer) {
		Assert.isLegal(editor != null);
		Assert.isLegal(viewer != null);

		internalUninstall();

		if (editor instanceof JavaEditor) {
			fProjectionListener= new ProjectionListener(viewer);
			fEditor= (JavaEditor)editor;
			IPreferenceStore store = JavaPlugin.getDefault().getPreferenceStore();
	        store.addPropertyChangeListener(fPropertyChangeListener);
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Subclasses may extend.
	 * </p>
	 */
	@Override
	public void uninstall() {
		internalUninstall();
	}

	/**
	 * Internal implementation of {@link #uninstall()}.
	 */
	private void internalUninstall() {
		if (isInstalled()) {
			handleProjectionDisabled();
			fProjectionListener.dispose();
			fProjectionListener= null;
			fEditor= null;
			IPreferenceStore store = JavaPlugin.getDefault().getPreferenceStore();
	        if (store != null && fPropertyChangeListener != null) {
	            store.removePropertyChangeListener(fPropertyChangeListener);
	        }
		}
	}

	/**
	 * Returns <code>true</code> if the provider is installed, <code>false</code> otherwise.
	 *
	 * @return <code>true</code> if the provider is installed, <code>false</code> otherwise
	 */
	protected final boolean isInstalled() {
		return fEditor != null;
	}

	/**
	 * Called whenever projection is enabled, for example when the viewer issues a
	 * {@link IProjectionListener#projectionEnabled() projectionEnabled} message. When the provider
	 * is already enabled when this method is called, it is first
	 * {@link #handleProjectionDisabled() disabled}.
	 * <p>
	 * Subclasses may extend.
	 * </p>
	 */
	protected void handleProjectionEnabled() {
		// http://home.ott.oti.com/teams/wswb/anon/out/vms/index.html
		// projectionEnabled messages are not always paired with projectionDisabled
		// i.e. multiple enabled messages may be sent out.
		// we have to make sure that we disable first when getting an enable
		// message.
		handleProjectionDisabled();

		if (isInstalled()) {
			initialize();
			fElementListener= new ElementChangedListener();
			JavaCore.addElementChangedListener(fElementListener);
		}
	}

	/**
	 * Called whenever projection is disabled, for example when the provider is
	 * {@link #uninstall() uninstalled}, when the viewer issues a
	 * {@link IProjectionListener#projectionDisabled() projectionDisabled} message and before
	 * {@link #handleProjectionEnabled() enabling} the provider. Implementations must be prepared to
	 * handle multiple calls to this method even if the provider is already disabled.
	 * <p>
	 * Subclasses may extend.
	 * </p>
	 */
	protected void handleProjectionDisabled() {
		if (fElementListener != null) {
			JavaCore.removeElementChangedListener(fElementListener);
			fElementListener= null;
		}
	}

	/*
	 * @see org.eclipse.jdt.ui.text.folding.IJavaFoldingStructureProvider#initialize()
	 */
	@Override
	public final void initialize() {
		fUpdatingCount++;
		try {
			update(createInitialContext());
		} finally {
			fUpdatingCount--;
		}
	}

	private FoldingStructureComputationContext createInitialContext() {
		initializePreferences();
		fInput= getInputElement();
		if (fInput == null)
			return null;
		return createContext(true);
	}

	private FoldingStructureComputationContext createContext(boolean allowCollapse) {
		if (!isInstalled())
			return null;
		ProjectionAnnotationModel model= getModel();
		if (model == null)
			return null;
		IDocument doc= getDocument();
		if (doc == null)
			return null;

		IScanner scanner= null;
		if (fUpdatingCount == 1)
			scanner= fSharedScanner; // reuse scanner
		return new FoldingStructureComputationContext(doc, model, allowCollapse, scanner);
	}

	private IJavaElement getInputElement() {
		if (fEditor == null)
			return null;
		return EditorUtility.getEditorInputJavaElement(fEditor, false);
	}

	private void initializePreferences() {
	    IPreferenceStore store = JavaPlugin.getDefault().getPreferenceStore();
	    fCollapseInnerTypes = store.getBoolean(PreferenceConstants.EDITOR_FOLDING_INNERTYPES);
	    fCollapseImportContainer = store.getBoolean(PreferenceConstants.EDITOR_FOLDING_IMPORTS);
	    fCollapseJavadoc = store.getBoolean(PreferenceConstants.EDITOR_FOLDING_JAVADOC);
	    fCollapseMembers = store.getBoolean(PreferenceConstants.EDITOR_FOLDING_METHODS);
	    fCollapseHeaderComments = store.getBoolean(PreferenceConstants.EDITOR_FOLDING_HEADERS);

		String customFoldingRegionBegin= store.getString(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_START);
		String customFoldingRegionEnd= store.getString(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_END);
		fCustomFoldingRegionBegin=customFoldingRegionBegin.toCharArray();
		fCustomFoldingRegionEnd=customFoldingRegionEnd.toCharArray();
		fCustomFoldingRegionsEnabled = store.getBoolean(PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGIONS_ENABLED) &&
				!customFoldingRegionBegin.isEmpty() && !customFoldingRegionEnd.isEmpty() &&
				!customFoldingRegionBegin.startsWith(customFoldingRegionEnd) && !customFoldingRegionEnd.startsWith(customFoldingRegionBegin);
		fNewFolding = store.getBoolean(PreferenceConstants.EDITOR_NEW_FOLDING_ENABLED);
	}

	private void update(FoldingStructureComputationContext ctx) {
		if (ctx == null)
			return;

		Map<JavaProjectionAnnotation, Position> additions= new HashMap<>();
		List<JavaProjectionAnnotation> deletions= new ArrayList<>();
		List<JavaProjectionAnnotation> updates= new ArrayList<>();

		computeFoldingStructure(ctx);
		Map<JavaProjectionAnnotation, Position> newStructure= ctx.fMap;
		Map<IJavaElement, List<Tuple>> oldStructure= computeCurrentStructure(ctx);

		Iterator<JavaProjectionAnnotation> e= newStructure.keySet().iterator();
		while (e.hasNext()) {
			JavaProjectionAnnotation newAnnotation= e.next();
			Position newPosition= newStructure.get(newAnnotation);

			IJavaElement element= newAnnotation.getElement();
			/*
			 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=130472 and
			 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=127445 In the presence of syntax
			 * errors, anonymous types may have a source range offset of 0. When such a situation is
			 * encountered, we ignore the proposed folding range: if no corresponding folding range
			 * exists, it is silently ignored; if there *is* a matching folding range, we ignore the
			 * position update and keep the old range, in order to keep the folding structure
			 * stable.
			 */
			boolean isMalformedAnonymousType= newPosition.getOffset() == 0 && element != null && element.getElementType() == IJavaElement.TYPE && isInnerType((IType) element);
			List<Tuple> annotations= oldStructure.get(element);
			if (annotations == null) {
				if (!isMalformedAnonymousType)
					additions.put(newAnnotation, newPosition);
			} else {
				Iterator<Tuple> x= annotations.iterator();
				boolean matched= false;
				while (x.hasNext()) {
					Tuple tuple= x.next();
					JavaProjectionAnnotation existingAnnotation= tuple.annotation;
					Position existingPosition= tuple.position;
					if (newAnnotation.isComment() == existingAnnotation.isComment()) {
						boolean updateCollapsedState= ctx.allowCollapsing() && existingAnnotation.isCollapsed() != newAnnotation.isCollapsed();
						if (!isMalformedAnonymousType && existingPosition != null && (!newPosition.equals(existingPosition) || updateCollapsedState)) {
							existingPosition.setOffset(newPosition.getOffset());
							existingPosition.setLength(newPosition.getLength());
							if (updateCollapsedState)
								if (newAnnotation.isCollapsed())
									existingAnnotation.markCollapsed();
								else
									existingAnnotation.markExpanded();
							updates.add(existingAnnotation);
						}
						matched= true;
						x.remove();
						break;
					}
				}
				if (!matched)
					additions.put(newAnnotation, newPosition);

				if (annotations.isEmpty())
					oldStructure.remove(element);
			}
		}

		Iterator<List<Tuple>> iter= oldStructure.values().iterator();
		while (iter.hasNext()) {
			List<Tuple> list= iter.next();
			int size= list.size();
			for (int i= 0; i < size; i++)
				deletions.add(list.get(i).annotation);
		}

		match(deletions, additions, updates, ctx);

		Annotation[] deletedArray= deletions.toArray(new Annotation[deletions.size()]);
		Annotation[] changedArray= updates.toArray(new Annotation[updates.size()]);
		ctx.getModel().modifyAnnotations(deletedArray, additions, changedArray);

		ctx.setSource(null);
	}

	private void computeFoldingStructure(FoldingStructureComputationContext ctx) {
	    if (fNewFolding && fInput instanceof ICompilationUnit) {
	        processCompilationUnit((ICompilationUnit) fInput, ctx);
	        processComments(ctx);
	    } else {
	        processSourceReference(ctx);
	    }
	}

	private void processCompilationUnit(ICompilationUnit unit, FoldingStructureComputationContext ctx) {
	    try {
	        String source = unit.getSource();
	        if (source == null) return;

	        char[] sourceArray= source.toCharArray();
			ctx.getScanner().setSource(sourceArray);
	        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
	        parser.setBindingsRecovery(true);
	        parser.setStatementsRecovery(true);
	        parser.setKind(ASTParser.K_COMPILATION_UNIT);
	        parser.setResolveBindings(true);
	        parser.setUnitName(unit.getElementName());
	        parser.setProject(unit.getJavaProject());
	        parser.setSource(unit);
	        Map<String, String> options = unit.getJavaProject().getOptions(true);
		    options.put(JavaCore.COMPILER_SOURCE, JavaCore.latestSupportedJavaVersion());
		    options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.latestSupportedJavaVersion());
		    options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.latestSupportedJavaVersion());
		    options.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
	        parser.setCompilerOptions(options);

	        CompilationUnit ast = (CompilationUnit) parser.createAST(null);
	        FoldingVisitor visitor= new FoldingVisitor(ctx);
			ast.accept(visitor);

			if (fCustomFoldingRegionsEnabled) {
				List<Comment> comments= ast.getCommentList();
				int currentCommentIndex= 0;

				// Go through all already discovered folding regions in order to compute custom regions
				// Disjoint folding regions are processed sequentially.
				// Nested folding regions are processed depth-first.

				// stack containing the "current" nested folding regions
				// the top of the stack is the innermost region
				Deque<Position> currentFoldingPositions= new ArrayDeque<>();
				// stack containing the position of "open" regions for each element in currentFoldingPositions
				// i.e. regions where the start position has been found but no end position
				// Whenever an element is added/removed from currentFoldingPositions, the same is done with openCustomRegionStartPositions
				Deque<Deque<Integer>> openCustomRegionStartPositions= new ArrayDeque<>();
				openCustomRegionStartPositions.add(new ArrayDeque<>());

				for (Position nonCommentFoldingRegion : List.copyOf(ctx.fMap.values())) {

					// process regions depth-first until reaching the current region
					currentCommentIndex= processFoldingRegionsForCustomCommentFolding(
							sourceArray, visitor, comments, currentCommentIndex,
							currentFoldingPositions, openCustomRegionStartPositions, nonCommentFoldingRegion.getOffset()
					);
					if (currentCommentIndex >= comments.size()) {
						return;
					}

					// process comments before current region starts
					currentCommentIndex= checkCustomFoldingCommitsBeforePosition(sourceArray, visitor, comments, currentCommentIndex, openCustomRegionStartPositions.peekLast(), nonCommentFoldingRegion.getOffset());

					currentFoldingPositions.addLast(nonCommentFoldingRegion);
					openCustomRegionStartPositions.addLast(new ArrayDeque<>());
				}
				// process all leftover comments at the end
				currentCommentIndex= processFoldingRegionsForCustomCommentFolding(sourceArray, visitor, comments, currentCommentIndex, currentFoldingPositions, openCustomRegionStartPositions, Integer.MAX_VALUE);
				checkCustomFoldingCommitsBeforePosition(sourceArray, visitor, comments, currentCommentIndex, openCustomRegionStartPositions.peek(), Integer.MAX_VALUE);
			}
		} catch (JavaModelException | IllegalStateException e) {
		}
	}

	/**
	 * Pops regions before limit and checks for custom folding comments.
	 *
	 * @param sourceArray the content of the processed source file
	 * @param visitor the {@link FoldingVisitor} used for adding folding regions
	 * @param comments all comments in the source file
	 * @param currentCommentIndex the index of the next comment to process
	 * @param currentFoldingPositions stack of current nested folding regions
	 * @param openCustomRegionStartPositions start positions of custom folding regions with the
	 *            start position being found but the end positions still missing
	 * @param limit only regions before this position are scanned
	 * @return the new index of the next comment
	 */
	private int processFoldingRegionsForCustomCommentFolding(char[] sourceArray, FoldingVisitor visitor, List<Comment> comments, int currentCommentIndex,
			Deque<Position> currentFoldingPositions, Deque<Deque<Integer>> openCustomRegionStartPositions, int limit) {
		Position currentFoldingPosition= currentFoldingPositions.peekLast();
		while (currentFoldingPosition != null && currentFoldingPosition.getOffset() + currentFoldingPosition.getLength() < limit) {
			currentFoldingPositions.removeLast();
			Deque<Integer> innerOpenStartPositions= openCustomRegionStartPositions.removeLast();

			currentCommentIndex= checkCustomFoldingCommitsBeforePosition(sourceArray, visitor, comments, currentCommentIndex, innerOpenStartPositions, currentFoldingPosition.getOffset() + currentFoldingPosition.getLength());

			if (currentCommentIndex >= comments.size()) {
				return currentCommentIndex;
			}

			currentFoldingPosition= currentFoldingPositions.peekLast();
		}
		return currentCommentIndex;
	}


	/**
	 * Searches for custom folding comments before a specified position and adds corresponding folding annotations.
	 *
	 * @param sourceArray the content of the processed source file
	 * @param visitor the {@link FoldingVisitor} used for adding folding annotations
	 * @param comments all comments in the source file
	 * @param currentCommentIndex the index of the first comment to process
	 * @param innerOpenStartPositions start positions of custom folding regions with the start position being found but the end positions still missing
	 * @param limit only regions before this position are scanned
	 * @return the new index of the next comment to process (after limit)
	 */
	private int checkCustomFoldingCommitsBeforePosition(char[] sourceArray, FoldingVisitor visitor, List<Comment> comments, int currentCommentIndex,
			Deque<Integer> innerOpenStartPositions, int limit) {
		while (currentCommentIndex < comments.size() && comments.get(currentCommentIndex).getStartPosition() < limit) {
			Comment comment= comments.get(currentCommentIndex);
			checkCustomFolding(innerOpenStartPositions, sourceArray, visitor, comment);
			currentCommentIndex++;
		}
		return currentCommentIndex;
	}

	private void checkCustomFolding(Deque<Integer> openCustomRegionStartPositions, char[] sourceArray, FoldingVisitor visitor, Comment comment) {
		int skip= 2;
		if (comment.isDocComment()) {
			skip= 3;
		}
		int commentTextStart= skipLeadingWhitespace(sourceArray, comment.getStartPosition() + skip);
		IRegion customFoldingRegion= checkCustomFolding(
				openCustomRegionStartPositions, commentTextStart, sourceArray,
				comment.getStartPosition(), comment.getStartPosition() + comment.getLength()
		);
		if (customFoldingRegion != null) {
			if (includeLastLineInCustomFoldingRegion(sourceArray, customFoldingRegion.getOffset() + customFoldingRegion.getLength())) {
				includelastLine= true;
			}
			visitor.createFoldingRegion(customFoldingRegion.getOffset(), customFoldingRegion.getLength(), fCollapseCustomRegions);
		}
	}

	private boolean includeLastLineInCustomFoldingRegion(char[] sourceArray, int regionEnd) {
		char firstCharacter = sourceArray[regionEnd];
		if (firstCharacter == '\n' || firstCharacter == '\r') {
			return true;
		}
		for (int i= regionEnd + 1; i < sourceArray.length; i++) {
			char c= sourceArray[i];
			if (c == '\n' || c == '\r') {
				return true;
			}
			// allow custom regions comments defined in {/* ... */} without that causing the end to be shown
			if (!Character.isWhitespace(c) && c != '}') {
				return false;
			}
		}
		return true;
	}

	private void processComments(FoldingStructureComputationContext ctx) {
	    try {
	        IDocument document = ctx.getDocument();
	        String source = document.get();
	        IScanner scanner = ctx.getScanner();
	        scanner.setSource(source.toCharArray());
	        scanner.resetTo(0, source.length() - 1);

	        int token;
	        while ((token = scanner.getNextToken()) != ITerminalSymbols.TokenNameEOF) {
	            if (token == ITerminalSymbols.TokenNameCOMMENT_BLOCK || token == ITerminalSymbols.TokenNameCOMMENT_JAVADOC) {
	                int start = scanner.getCurrentTokenStartPosition();
	                int end = scanner.getCurrentTokenEndPosition() + 1;
	                try {
	                    int endLine = document.getLineOfOffset(end);
	                    int lineOffset = document.getLineOffset(endLine);
	                    int lineLength = document.getLineLength(endLine);
	                    String lineText = document.get(lineOffset, lineLength);
	                    int commentEndInLine = end - lineOffset;
	                    String afterComment = lineText.substring(commentEndInLine);

	                    if (afterComment.trim().length() > 0) {
	                        end = lineOffset;
	                    } else {
	                        if (endLine + 1 < document.getNumberOfLines()) {
	                            end = document.getLineOffset(endLine + 1);
	                        } else {
	                            end = document.getLength();
	                        }
	                    }
	                } catch (BadLocationException e) {
	                }

	                IRegion region = new Region(start, end - start);
	                includelastLine = true;
	                IRegion aligned = alignRegion(region, ctx);

	                if (aligned != null && isMultiline(aligned, ctx)) {
	                    Position position = createCommentPosition(aligned);
	                    JavaProjectionAnnotation annotation = new JavaProjectionAnnotation(ctx.collapseJavadoc(), null, true);
	                    ctx.addProjectionRange(annotation, position);
	                }
	            }
	        }
	    } catch (InvalidInputException e) {
	    }
	}


	private boolean isMultiline(IRegion region, FoldingStructureComputationContext ctx) {
	    try {
	        IDocument document = ctx.getDocument();
	        int startLine = document.getLineOfOffset(region.getOffset());
	        int endLine = document.getLineOfOffset(region.getOffset() + region.getLength() - 1);
	        return endLine > startLine;
	    } catch (BadLocationException e) {
	        return false;
	    }
	}


	private void processSourceReference(FoldingStructureComputationContext ctx) {
		IParent parent= (IParent) fInput;
		try {
			if (!(fInput instanceof ISourceReference))
				return;
			String source= ((ISourceReference)fInput).getSource();
			if (source == null)
				return;

			ctx.getScanner().setSource(source.toCharArray());
			computeFoldingStructure(parent.getChildren(), ctx);
		} catch (JavaModelException x) {
		}
	}

	/**
	 * Aligns <code>region</code> to start and end at a line offset. The region's start is
	 * decreased to the next line offset, and the end offset increased to the next line start or the
	 * end of the document. <code>null</code> is returned if <code>region</code> is
	 * <code>null</code> itself or does not comprise at least one line delimiter, as a single line
	 * cannot be folded.
	 *
	 * @param region the region to align, may be <code>null</code>
	 * @param ctx the folding context
	 * @return a region equal or greater than <code>region</code> that is aligned with line
	 *         offsets, <code>null</code> if the region is too small to be foldable (e.g. covers
	 *         only one line)
	 */
	protected final IRegion alignRegion(IRegion region, FoldingStructureComputationContext ctx) {
		if (region == null)
			return null;

		IDocument document= ctx.getDocument();

		try {
			int start= document.getLineOfOffset(region.getOffset());
			int end= document.getLineOfOffset(region.getOffset() - 1 + region.getLength());
			if (start >= end)
				return null;
			int offset= document.getLineOffset(start);
			int endOffset;
			if (includelastLine) {
				endOffset= document.getLineOffset(end + 1);
				includelastLine = false;
			}
			else {
				endOffset= document.getLineOffset(end);
			}

			return new Region(offset, endOffset - offset);
		} catch (BadLocationException x) {
			return null;
		}
	}

	/**
	 * Creates a comment folding position from an
	 * {@link #alignRegion(IRegion, DefaultJavaFoldingStructureProvider.FoldingStructureComputationContext) aligned}
	 * region.
	 *
	 * @param aligned an aligned region
	 * @return a folding position corresponding to <code>aligned</code>
	 */
	protected Position createCommentPosition(IRegion aligned) {
		return new CommentPosition(aligned.getOffset(), aligned.getLength());
	}

	private void computeFoldingStructure(IJavaElement[] elements, FoldingStructureComputationContext ctx) throws JavaModelException {
		for (IJavaElement element : elements) {
			computeFoldingStructure(element, ctx);

			if (element instanceof IParent) {
				IParent parent= (IParent) element;
				Deque<Integer> outerOpenRegions = ctx.fOpenCustomRegionStartPositions;
				ctx.fOpenCustomRegionStartPositions = new ArrayDeque<>();
				computeFoldingStructure(parent.getChildren(), ctx);
				ctx.fOpenCustomRegionStartPositions = outerOpenRegions;
			}
			if (fCustomFoldingRegionsEnabled && element instanceof ISourceReference sourceRef) {
				// mark as scanned until end after scanning all children
				ISourceRange sourceRange= sourceRef.getSourceRange();
				ctx.fLastScannedIndex = sourceRange.getLength()+sourceRange.getOffset();
			}
		}
	}

	/**
	 * Computes the folding structure for a given {@link IJavaElement java element}. Computed
	 * projection annotations are
	 * {@link DefaultJavaFoldingStructureProvider.FoldingStructureComputationContext#addProjectionRange(DefaultJavaFoldingStructureProvider.JavaProjectionAnnotation, Position)
	 * added} to the computation context.
	 * <p>
	 * Subclasses may extend or replace. The default implementation creates projection annotations
	 * for the following elements:
	 * </p>
	 * <ul>
	 * <li>true members (not for top-level types)</li>
	 * <li>the javadoc comments of any member</li>
	 * <li>header comments (javadoc or multi-line comments appearing before the first type's javadoc
	 * or before the package or import declarations).</li>
	 * </ul>
	 *
	 * @param element the java element to compute the folding structure for
	 * @param ctx the computation context
	 */
	protected void computeFoldingStructure(IJavaElement element, FoldingStructureComputationContext ctx) {
		boolean collapse= false;
		boolean collapseCode= true;
		switch (element.getElementType()) {

			case IJavaElement.IMPORT_CONTAINER:
				collapse= ctx.collapseImportContainer();
				includelastLine= true;
				break;
			case IJavaElement.TYPE:
				collapseCode= isInnerType((IType) element) && !isAnonymousEnum((IType) element);
				collapse= ctx.collapseInnerTypes() && collapseCode;
				break;
			case IJavaElement.METHOD:
			case IJavaElement.FIELD:
			case IJavaElement.INITIALIZER:
				collapse= ctx.collapseMembers();
				break;
			default:
				return;
		}

		IRegion[] regions= computeProjectionRanges((ISourceReference) element, ctx);
		if (regions.length > 0) {
			// comments
			for (int i= 0; i < regions.length - 1; i++) {
				includelastLine= true;
				IRegion region= regions[i];
				IRegion normalized= alignRegion(region, ctx);
				if (normalized != null) {
					Position position= createCommentPosition(normalized);
					if (position != null) {
						boolean commentCollapse;
						if (i == 0 && (regions.length > 2 || ctx.hasHeaderComment()) && element == ctx.getFirstType()) {
							commentCollapse= ctx.collapseHeaderComments();
						} else if(ctx.fCurrentCustomRegions.contains(region)) {
							commentCollapse= ctx.collapseCustomRegions();
						} else {
							commentCollapse= ctx.collapseJavadoc();
						}
						ctx.addProjectionRange(new JavaProjectionAnnotation(commentCollapse, element, true), position);
					}
				}
			}
			// code
			if (collapseCode) {
				IRegion normalized= alignRegion(regions[regions.length - 1], ctx);
				if (normalized != null) {
					Position position;
					if (element instanceof IMember) {
						position= createMemberPosition(normalized, (IMember) element);
					} else {
						includelastLine= true;
						position= createCommentPosition(normalized);
					}

					if (position != null)
						ctx.addProjectionRange(new JavaProjectionAnnotation(collapse, element, false), position);
				}
			}
		}
	}

	/**
	 * Returns <code>true</code> if <code>type</code> is an anonymous enum declaration,
	 * <code>false</code> otherwise. See also https://bugs.eclipse.org/bugs/show_bug.cgi?id=143276
	 *
	 * @param type the type to test
	 * @return <code>true</code> if <code>type</code> is an anonymous enum declaration
	 * @since 3.3
	 */
	private boolean isAnonymousEnum(IType type) {
		try {
			return type.isEnum() && type.isAnonymous();
		} catch (JavaModelException x) {
			return false; // optimistically
		}
	}

	/**
	 * Returns <code>true</code> if <code>type</code> is not a top-level type, <code>false</code> if it is.
	 *
	 * @param type the type to test
	 * @return <code>true</code> if <code>type</code> is an inner type
	 */
	private boolean isInnerType(IType type) {
		return type.getDeclaringType() != null;
	}

	/**
	 * Computes the projection ranges for a given <code>ISourceReference</code>. More than one
	 * range or none at all may be returned. If there are no foldable regions, an empty array is
	 * returned.
	 * <p>
	 * The last region in the returned array (if not empty) describes the region for the java
	 * element that implements the source reference. Any preceding regions describe javadoc comments
	 * of that java element.
	 * </p>
	 *
	 * @param reference a java element that is a source reference
	 * @param ctx the folding context
	 * @return the regions to be folded
	 */
	protected final IRegion[] computeProjectionRanges(ISourceReference reference, FoldingStructureComputationContext ctx) {
		try {
				ISourceRange range= reference.getSourceRange();
				if (!SourceRange.isAvailable(range))
					return new IRegion[0];

				String contents= reference.getSource();
				if (contents == null)
					return new IRegion[0];

				List<IRegion> regions= new ArrayList<>();
				if (!ctx.hasFirstType() && reference instanceof IType) {
					ctx.setFirstType((IType) reference);
					IRegion headerComment= computeHeaderComment(ctx);
					if (headerComment != null) {
						regions.add(headerComment);
						ctx.setHasHeaderComment();
					}
				}

				final int shift= range.getOffset();
				IScanner scanner= ctx.getScanner();

				if (fCustomFoldingRegionsEnabled &&
						reference instanceof IJavaElement javaElement && javaElement.getParent() != null &&
							javaElement.getParent() instanceof IParent parent && parent instanceof ISourceReference parentSourceReference) {
						// check tokens between the last sibling (or the parent) and start of current sibling
						ISourceRange parentSourceRange= parentSourceReference.getSourceRange();
						if (ctx.fLastScannedIndex >= parentSourceRange.getOffset() && ctx.fLastScannedIndex < parentSourceRange.getOffset() + parentSourceRange.getLength()
								&& ctx.fLastScannedIndex < range.getOffset()) {
							scanner.resetTo(ctx.fLastScannedIndex, range.getOffset());
							checkCustomFoldingUntilScannerEnd(ctx, regions, ctx.fOpenCustomRegionStartPositions, scanner);
						}
					}


				scanner.resetTo(shift, shift + range.getLength());

				int start= shift;

				while (true) {

					int token= scanner.getNextToken();
					start= scanner.getCurrentTokenStartPosition();

					switch (token) {
						case ITerminalSymbols.TokenNameCOMMENT_JAVADOC, ITerminalSymbols.TokenNameCOMMENT_MARKDOWN, ITerminalSymbols.TokenNameCOMMENT_BLOCK: {
							int end= scanner.getCurrentTokenEndPosition() + 1;
							regions.add(new Region(start, end - start));
							checkCustomFolding(ctx, regions, ctx.fOpenCustomRegionStartPositions, scanner, token, regions.size());
							continue;
						}
						case ITerminalSymbols.TokenNameCOMMENT_LINE: {
							checkCustomFolding(ctx, regions, ctx.fOpenCustomRegionStartPositions, scanner, token, regions.size());
							continue;
						}
					}

					break;
				}

				regions.add(new Region(start, shift + range.getLength() - start));

				if (fCustomFoldingRegionsEnabled) {
					if (reference instanceof IParent parent && !parent.hasChildren()) {
						// if the element has no children, check content for custom folding region markers
						checkCustomFoldingUntilScannerEnd(ctx, regions, new ArrayDeque<>(), scanner);
					}
					ctx.fLastScannedIndex= scanner.getCurrentTokenEndPosition();
					if (reference instanceof IJavaElement javaElement && javaElement.getParent() != null &&
							javaElement.getParent() instanceof IParent parent && parent instanceof ISourceReference parentSourceReference) {
						IJavaElement[] siblings= parent.getChildren();
						if (javaElement == siblings[siblings.length - 1]) {
							// if the current element is the last sibling
							// tokens after the current element and before the end of the parent are checked for custom folding region markers
							int regionStart= range.getOffset() + range.getLength();
							ISourceRange parentRange= parentSourceReference.getSourceRange();
							int regionEnd= parentRange.getOffset() + parentRange.getLength();
							scanner.resetTo(regionStart, regionEnd);
							checkCustomFoldingUntilScannerEnd(ctx, regions, ctx.fOpenCustomRegionStartPositions, scanner);
						}
					}
				}

				IRegion[] result= new IRegion[regions.size()];
				regions.toArray(result);
				return result;
		} catch (JavaModelException | InvalidInputException e) {
		}
		return new IRegion[0];
	}

	private void checkCustomFoldingUntilScannerEnd(FoldingStructureComputationContext ctx, List<IRegion> regions, Deque<Integer> openCustomRegionStartPositions, IScanner scanner) throws InvalidInputException {
		for(int token = scanner.getNextToken(); token != ITerminalSymbols.TokenNameEOF; token=scanner.getNextToken()) {
			if(isCommentToken(token)) {
				checkCustomFolding(ctx, regions, openCustomRegionStartPositions, scanner, token, Math.max(regions.size() - 1, 0));
			}
		}
	}

	private boolean isCommentToken(int token) {
		return token == ITerminalSymbols.TokenNameCOMMENT_BLOCK || token == ITerminalSymbols.TokenNameCOMMENT_JAVADOC || token == ITerminalSymbols.TokenNameCOMMENT_MARKDOWN || token == ITerminalSymbols.TokenNameCOMMENT_LINE;
	}

	private void checkCustomFolding(FoldingStructureComputationContext ctx, List<IRegion> regions, Deque<Integer> openCustomRegionStartPositions, IScanner scanner, int token, int regionArrayIndex) {
		if (!fCustomFoldingRegionsEnabled) {
			return;
		}
		int commentTextStart = findPossibleRegionCommentStart(scanner, token);

		char[] source= scanner.getSource();
		int currentTokenStartPosition= scanner.getCurrentTokenStartPosition();
		int currentTokenEndPosition= scanner.getCurrentTokenEndPosition();

		IRegion region= checkCustomFolding(openCustomRegionStartPositions, commentTextStart, source, currentTokenStartPosition, currentTokenEndPosition);
		if (region != null) {
			if (!includeLastLineInCustomFoldingRegion(source, currentTokenEndPosition)) {
				includelastLine= false;
				region= alignRegion(region, ctx);
			}
			regions.add(regionArrayIndex, region);
			ctx.fCurrentCustomRegions.add(region);
		}
	}

	private Region checkCustomFolding(Deque<Integer> openCustomRegionStartPositions, int commentTextStart, char[] source, int currentTokenStartPosition, int currentTokenEndPosition) {
		int currentTokenLengthStartingAtCommentTextStart= currentTokenEndPosition - commentTextStart;

		if (startsWith(source, commentTextStart, currentTokenLengthStartingAtCommentTextStart, fCustomFoldingRegionBegin)) {
			openCustomRegionStartPositions.add(currentTokenStartPosition);
		}

		if (startsWith(source, commentTextStart, currentTokenLengthStartingAtCommentTextStart, fCustomFoldingRegionEnd) && !openCustomRegionStartPositions.isEmpty()) {
			int end= currentTokenEndPosition;
			Integer regionStart= openCustomRegionStartPositions.removeLast();
			return new Region(regionStart, end - regionStart);

		}
		return null;
	}

	private int findPossibleRegionCommentStart(IScanner scanner, int token) {
		char[] source= scanner.getSource();
		int start= scanner.getCurrentTokenStartPosition();
		int skip= switch (token) {
			case ITerminalSymbols.TokenNameCOMMENT_LINE, ITerminalSymbols.TokenNameCOMMENT_BLOCK -> 2;
			case ITerminalSymbols.TokenNameCOMMENT_JAVADOC, ITerminalSymbols.TokenNameCOMMENT_MARKDOWN -> 3;
			default -> 0;
		};
		int newStart= start + skip;
		return skipLeadingWhitespace(source, newStart);
	}

	private int skipLeadingWhitespace(char[] source, int start) {
		while (Character.isWhitespace(source[start])) {
			start++;
		}
		return start;
	}

	private boolean startsWith(char[] source, int offset, int length, char[] prefix) {
		if (length < prefix.length) {
			return false;
		}
		for(int i=0;i<prefix.length;i++) {
			if (source[offset+i] != prefix[i]) {
				return false;
			}
		}
		return true;
	}

	private IRegion computeHeaderComment(FoldingStructureComputationContext ctx) throws JavaModelException {
		// search at most up to the first type
		ISourceRange range= ctx.getFirstType().getSourceRange();
		if (range == null)
			return null;
		int start= 0;
		int end= range.getOffset();


		/* code adapted from CommentFormattingStrategy:
		 * scan the header content up to the first type. Once a comment is
		 * found, accumulate any additional comments up to the stop condition.
		 * The stop condition is reaching a package declaration, import container,
		 * or the end of the input.
		 */
		IScanner scanner= ctx.getScanner();
		scanner.resetTo(start, end);

		int headerStart= -1;
		int headerEnd= -1;
		try {
			boolean foundComment= false;
			int terminal= scanner.getNextToken();
			while (terminal != ITerminalSymbols.TokenNameEOF
					&& (terminal != ITerminalSymbols.TokenNameclass)
					&& (terminal != ITerminalSymbols.TokenNameinterface)
					&& (terminal != ITerminalSymbols.TokenNameenum)
					&& (!foundComment || ((terminal != ITerminalSymbols.TokenNameimport) && (terminal != ITerminalSymbols.TokenNamepackage)))) {

				if (terminal == ITerminalSymbols.TokenNameCOMMENT_JAVADOC
						|| terminal == ITerminalSymbols.TokenNameCOMMENT_BLOCK
						|| terminal == ITerminalSymbols.TokenNameCOMMENT_LINE
						|| terminal == ITerminalSymbols.TokenNameCOMMENT_MARKDOWN) {
					if (!foundComment)
						headerStart= scanner.getCurrentTokenStartPosition();
					headerEnd= scanner.getCurrentTokenEndPosition();
					foundComment= true;
				}
				terminal= scanner.getNextToken();
			}


		} catch (InvalidInputException ex) {
			return null;
		}

		if (headerEnd != -1) {
			return new Region(headerStart, headerEnd - headerStart);
		}
		return null;
	}

	/**
	 * Creates a folding position that remembers its member from an
	 * {@link #alignRegion(IRegion, DefaultJavaFoldingStructureProvider.FoldingStructureComputationContext) aligned}
	 * region.
	 *
	 * @param aligned an aligned region
	 * @param member the member to remember
	 * @return a folding position corresponding to <code>aligned</code>
	 */
	protected final Position createMemberPosition(IRegion aligned, IMember member) {
		return new JavaElementPosition(aligned.getOffset(), aligned.getLength(), member);
	}

	private ProjectionAnnotationModel getModel() {
		return fEditor.getAdapter(ProjectionAnnotationModel.class);
	}

	private IDocument getDocument() {
		JavaEditor editor= fEditor;
		if (editor == null)
			return null;

		IDocumentProvider provider= editor.getDocumentProvider();
		if (provider == null)
			return null;

		return provider.getDocument(editor.getEditorInput());
	}

	/**
	 * Matches deleted annotations to changed or added ones. A deleted
	 * annotation/position tuple that has a matching addition / change
	 * is updated and marked as changed. The matching tuple is not added
	 * (for additions) or marked as deletion instead (for changes). The
	 * result is that more annotations are changed and fewer get
	 * deleted/re-added.
	 *
	 * @param deletions list with deleted annotations
	 * @param additions map with position to annotation mappings
	 * @param changes list with changed annotations
	 * @param ctx	the context
	 */
	private void match(List<JavaProjectionAnnotation> deletions, Map<JavaProjectionAnnotation, Position> additions, List<JavaProjectionAnnotation> changes, FoldingStructureComputationContext ctx) {
		if (deletions.isEmpty() || (additions.isEmpty() && changes.isEmpty()))
			return;

		List<JavaProjectionAnnotation> newDeletions= new ArrayList<>();
		List<JavaProjectionAnnotation> newChanges= new ArrayList<>();

		Iterator<JavaProjectionAnnotation> deletionIterator= deletions.iterator();
		while (deletionIterator.hasNext()) {
			JavaProjectionAnnotation deleted= deletionIterator.next();
			Position deletedPosition= ctx.getModel().getPosition(deleted);
			if (deletedPosition == null)
				continue;

			Tuple deletedTuple= new Tuple(deleted, deletedPosition);

			Tuple match= findMatch(deletedTuple, changes, null, ctx);
			boolean addToDeletions= true;
			if (match == null) {
				match= findMatch(deletedTuple, additions.keySet(), additions, ctx);
				addToDeletions= false;
			}

			if (match != null) {
				IJavaElement element= match.annotation.getElement();
				deleted.setElement(element);
				deletedPosition.setLength(match.position.getLength());
				if (deletedPosition instanceof JavaElementPosition && element instanceof IMember) {
					JavaElementPosition jep= (JavaElementPosition) deletedPosition;
					jep.setMember((IMember) element);
				}

				deletionIterator.remove();
				newChanges.add(deleted);

				if (addToDeletions)
					newDeletions.add(match.annotation);
			}
		}

		deletions.addAll(newDeletions);
		changes.addAll(newChanges);
	}

	/**
	 * Finds a match for <code>tuple</code> in a collection of
	 * annotations. The positions for the
	 * <code>JavaProjectionAnnotation</code> instances in
	 * <code>annotations</code> can be found in the passed
	 * <code>positionMap</code> or <code>fCachedModel</code> if
	 * <code>positionMap</code> is <code>null</code>.
	 * <p>
	 * A tuple is said to match another if their annotations have the
	 * same comment flag and their position offsets are equal.
	 * </p>
	 * <p>
	 * If a match is found, the annotation gets removed from
	 * <code>annotations</code>.
	 * </p>
	 *
	 * @param tuple the tuple for which we want to find a match
	 * @param annotations collection of
	 *        <code>JavaProjectionAnnotation</code>
	 * @param positionMap a <code>Map&lt;Annotation, Position&gt;</code>
	 *        or <code>null</code>
	 * @param ctx the context
	 * @return a matching tuple or <code>null</code> for no match
	 */
	private Tuple findMatch(Tuple tuple, Collection<JavaProjectionAnnotation> annotations, Map<JavaProjectionAnnotation, Position> positionMap, FoldingStructureComputationContext ctx) {
		Iterator<JavaProjectionAnnotation> it= annotations.iterator();
		while (it.hasNext()) {
			JavaProjectionAnnotation annotation= it.next();
			if (tuple.annotation.isComment() == annotation.isComment()) {
				Position position= positionMap == null ? ctx.getModel().getPosition(annotation) : positionMap.get(annotation);
				if (position == null)
					continue;

				if (tuple.position.getOffset() == position.getOffset()) {
					it.remove();
					return new Tuple(annotation, position);
				}
			}
		}

		return null;
	}

	private Map<IJavaElement, List<Tuple>> computeCurrentStructure(FoldingStructureComputationContext ctx) {
		Map<IJavaElement, List<Tuple>> map= new HashMap<>();
		ProjectionAnnotationModel model= ctx.getModel();
		Iterator<Annotation> e= model.getAnnotationIterator();
		while (e.hasNext()) {
			Object annotation= e.next();
			if (annotation instanceof JavaProjectionAnnotation) {
				JavaProjectionAnnotation java= (JavaProjectionAnnotation) annotation;
				Position position= model.getPosition(java);
				Assert.isNotNull(position);
				List<Tuple> list= map.get(java.getElement());
				if (list == null) {
					list= new ArrayList<>(2);
					map.put(java.getElement(), list);
				}
				list.add(new Tuple(java, position));
			}
		}

		Comparator<Tuple> comparator= (o1, o2) -> o1.position.getOffset() - o2.position.getOffset();
		for (List<Tuple> list : map.values()) {
			Collections.sort(list, comparator);
		}
		return map;
	}

	/*
	 * @see IJavaFoldingStructureProviderExtension#collapseMembers()
	 * @since 3.2
	 */
	@Override
	public final void collapseMembers() {
		modifyFiltered(fMemberFilter, false);
	}

	/*
	 * @see IJavaFoldingStructureProviderExtension#collapseComments()
	 * @since 3.2
	 */
	@Override
	public final void collapseComments() {
		modifyFiltered(fCommentFilter, false);
	}

	/*
	 * @see org.eclipse.jdt.ui.text.folding.IJavaFoldingStructureProviderExtension#collapseElements(org.eclipse.jdt.core.IJavaElement[])
	 */
	@Override
	public final void collapseElements(IJavaElement[] elements) {
		Set<IJavaElement> set= new HashSet<>(Arrays.asList(elements));
		modifyFiltered(new JavaElementSetFilter(set, false), false);
	}

	/*
	 * @see org.eclipse.jdt.ui.text.folding.IJavaFoldingStructureProviderExtension#expandElements(org.eclipse.jdt.core.IJavaElement[])
	 */
	@Override
	public final void expandElements(IJavaElement[] elements) {
		Set<IJavaElement> set= new HashSet<>(Arrays.asList(elements));
		modifyFiltered(new JavaElementSetFilter(set, true), true);
	}

	/**
	 * Collapses or expands all annotations matched by the passed filter.
	 *
	 * @param filter the filter to use to select which annotations to collapse
	 * @param expand <code>true</code> to expand the matched annotations, <code>false</code> to
	 *        collapse them
	 */
	private void modifyFiltered(Filter filter, boolean expand) {
		if (!isInstalled())
			return;

		ProjectionAnnotationModel model= getModel();
		if (model == null)
			return;

		List<JavaProjectionAnnotation> modified= new ArrayList<>();
		Iterator<Annotation> iter= model.getAnnotationIterator();
		while (iter.hasNext()) {
			Object annotation= iter.next();
			if (annotation instanceof JavaProjectionAnnotation) {
				JavaProjectionAnnotation java= (JavaProjectionAnnotation) annotation;

				if (expand == java.isCollapsed() && filter.match(java)) {
					if (expand)
						java.markExpanded();
					else
						java.markCollapsed();
					modified.add(java);
				}

			}
		}

		model.modifyAnnotations(null, null, modified.toArray(new Annotation[modified.size()]));
	}
}
