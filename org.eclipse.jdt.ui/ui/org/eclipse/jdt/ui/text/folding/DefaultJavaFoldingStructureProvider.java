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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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

import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.preferences.FoldingPreferencePage;
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
			boolean isInnerClass= node.isMemberTypeDeclaration();
			if (isInnerClass) {
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
	        if (PreferenceConstants.EDITOR_NEW_FOLDING_ENABLED.equals(event.getProperty())) {
	            fNewFolding = event.getNewValue().equals(Boolean.TRUE);
	        }
	        initialize();
	    }
	};

	/* preferences */
	private boolean fCollapseJavadoc= false;
	private boolean fCollapseImportContainer= true;
	private boolean fCollapseInnerTypes= true;
	private boolean fCollapseMembers= false;
	private boolean fCollapseHeaderComments= true;
	private boolean fNewFolding;

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
			IPreferenceStore store = FoldingPreferencePage.getFoldingPreferenceStore(fEditor);
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
			IPreferenceStore store = FoldingPreferencePage.getFoldingPreferenceStore(fEditor);
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
		IPreferenceStore store= FoldingPreferencePage.getFoldingPreferenceStore(fEditor);
		fCollapseInnerTypes= store.getBoolean(PreferenceConstants.EDITOR_FOLDING_INNERTYPES);
		fCollapseImportContainer= store.getBoolean(PreferenceConstants.EDITOR_FOLDING_IMPORTS);
		fCollapseJavadoc= store.getBoolean(PreferenceConstants.EDITOR_FOLDING_JAVADOC);
		fCollapseMembers= store.getBoolean(PreferenceConstants.EDITOR_FOLDING_METHODS);
		fCollapseHeaderComments= store.getBoolean(PreferenceConstants.EDITOR_FOLDING_HEADERS);
		fNewFolding= store.getBoolean(PreferenceConstants.EDITOR_NEW_FOLDING_ENABLED);
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

	        ctx.getScanner().setSource(source.toCharArray());
	        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
	        parser.setBindingsRecovery(true);
	        parser.setStatementsRecovery(true);
	        parser.setKind(ASTParser.K_COMPILATION_UNIT);
	        parser.setResolveBindings(true);
	        parser.setUnitName(unit.getElementName());
	        parser.setProject(unit.getJavaProject());
	        parser.setSource(unit);
	        Map<String, String> options = unit.getJavaProject().getOptions(true);
		    options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_23);
		    options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_23);
		    options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_23);
		    options.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
	        parser.setCompilerOptions(options);

	        CompilationUnit ast = (CompilationUnit) parser.createAST(null);
	        ast.accept(new FoldingVisitor(ctx));
	    } catch (JavaModelException | IllegalStateException e) {
	    }
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
				computeFoldingStructure(parent.getChildren(), ctx);
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
				IRegion normalized= alignRegion(regions[i], ctx);
				if (normalized != null) {
					includelastLine= true;
					Position position= createCommentPosition(normalized);
					if (position != null) {
						boolean commentCollapse;
						if (i == 0 && (regions.length > 2 || ctx.hasHeaderComment()) && element == ctx.getFirstType()) {
							commentCollapse= ctx.collapseHeaderComments();
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
				scanner.resetTo(shift, shift + range.getLength());

				int start= shift;
				while (true) {

					int token= scanner.getNextToken();
					start= scanner.getCurrentTokenStartPosition();

					switch (token) {
						case ITerminalSymbols.TokenNameCOMMENT_JAVADOC:
						case ITerminalSymbols.TokenNameCOMMENT_MARKDOWN:
						case ITerminalSymbols.TokenNameCOMMENT_BLOCK: {
							int end= scanner.getCurrentTokenEndPosition() + 1;
							regions.add(new Region(start, end - start));
							continue;
						}
						case ITerminalSymbols.TokenNameCOMMENT_LINE:
							continue;
					}

					break;
				}

				regions.add(new Region(start, shift + range.getLength() - start));

				IRegion[] result= new IRegion[regions.size()];
				regions.toArray(result);
				return result;
		} catch (JavaModelException | InvalidInputException e) {
		}
		return new IRegion[0];
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