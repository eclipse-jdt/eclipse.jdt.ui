/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.folding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.projection.IProjectionListener;
import org.eclipse.jface.text.source.projection.IProjectionPosition;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionViewer;

import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.folding.IJavaFoldingStructureProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditor;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.text.DocumentCharacterIterator;


/**
 * Updates the projection model of a class file or compilation unit.
 *
 * @since 3.0
 */
public class DefaultJavaFoldingStructureProvider implements IProjectionListener, IJavaFoldingStructureProvider {

	private static final class JavaProjectionAnnotation extends ProjectionAnnotation {

		private IJavaElement fJavaElement;
		private boolean fIsComment;

		public JavaProjectionAnnotation(IJavaElement element, boolean isCollapsed, boolean isComment) {
			super(isCollapsed);
			fJavaElement= element;
			fIsComment= isComment;
		}

		public IJavaElement getElement() {
			return fJavaElement;
		}

		public void setElement(IJavaElement element) {
			fJavaElement= element;
		}

		public boolean isComment() {
			return fIsComment;
		}

		public void setIsComment(boolean isComment) {
			fIsComment= isComment;
		}

		/*
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "JavaProjectionAnnotation:\n" + //$NON-NLS-1$
					"\telement: \t"+fJavaElement.toString()+"\n" + //$NON-NLS-1$ //$NON-NLS-2$
					"\tcollapsed: \t" + isCollapsed() + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
					"\tcomment: \t" + fIsComment + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
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

	private class ElementChangedListener implements IElementChangedListener {

		/*
		 * @see org.eclipse.jdt.core.IElementChangedListener#elementChanged(org.eclipse.jdt.core.ElementChangedEvent)
		 */
		public void elementChanged(ElementChangedEvent e) {
			IJavaElementDelta delta= findElement(fInput, e.getDelta());
			if (delta != null)
				processDelta(delta);
		}

		private IJavaElementDelta findElement(IJavaElement target, IJavaElementDelta delta) {

			if (delta == null || target == null)
				return null;

			IJavaElement element= delta.getElement();

			if (element.getElementType() > IJavaElement.CLASS_FILE)
				return null;

			if (target.equals(element))
				return delta;

			IJavaElementDelta[] children= delta.getAffectedChildren();

			for (int i= 0; i < children.length; i++) {
				IJavaElementDelta d= findElement(target, children[i]);
				if (d != null)
					return d;
			}

			return null;
		}
	}

	/**
	 * Projection position that will return two foldable regions: one folding away
	 * the region from after the '/**' to the beginning of the content, the other
	 * from after the first content line until after the comment.
	 *
	 * @since 3.1
	 */
	private static final class CommentPosition extends Position implements IProjectionPosition {
		CommentPosition(int offset, int length) {
			super(offset, length);
		}

		/*
		 * @see org.eclipse.jface.text.source.projection.IProjectionPosition#computeFoldingRegions(org.eclipse.jface.text.IDocument)
		 */
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
//				preRegion= new Region(offset + prefixEnd, contentStart - prefixEnd);
				int preOffset= document.getLineOffset(firstLine);
				IRegion preEndLineInfo= document.getLineInformation(captionLine);
				int preEnd= preEndLineInfo.getOffset();
				preRegion= new Region(preOffset, preEnd - preOffset);
			} else {
				preRegion= null;
			}

			if (captionLine < lastLine) {
				int postOffset= document.getLineOffset(captionLine + 1);
				IRegion postRegion= new Region(postOffset, offset + length - postOffset);

				if (preRegion == null)
					return new IRegion[] { postRegion };

				return new IRegion[] { preRegion, postRegion };
			}

			if (preRegion != null)
				return new IRegion[] { preRegion };

			return null;
		}

		/**
		 * Finds the offset of the first identifier part within <code>content</code>.
		 * Returns 0 if none is found.
		 *
		 * @param content the content to search
		 * @return the first index of a unicode identifier part, or zero if none can
		 *         be found
		 */
		private int findFirstContent(final CharSequence content, int prefixEnd) {
			int lenght= content.length();
			for (int i= prefixEnd; i < lenght; i++) {
				if (Character.isUnicodeIdentifierPart(content.charAt(i)))
					return i;
			}
			return 0;
		}

//		/**
//		 * Finds the offset of the first identifier part within <code>content</code>.
//		 * Returns 0 if none is found.
//		 *
//		 * @param content the content to search
//		 * @return the first index of a unicode identifier part, or zero if none can
//		 *         be found
//		 */
//		private int findPrefixEnd(final CharSequence content) {
//			// return the index after the leading '/*' or '/**'
//			int len= content.length();
//			int i= 0;
//			while (i < len && isWhiteSpace(content.charAt(i)))
//				i++;
//			if (len >= i + 2 && content.charAt(i) == '/' && content.charAt(i + 1) == '*')
//				if (len >= i + 3 && content.charAt(i + 2) == '*')
//					return i + 3;
//				else
//					return i + 2;
//			else
//				return i;
//		}
//
//		private boolean isWhiteSpace(char c) {
//			return c == ' ' || c == '\t';
//		}

		/*
		 * @see org.eclipse.jface.text.source.projection.IProjectionPosition#computeCaptionOffset(org.eclipse.jface.text.IDocument)
		 */
		public int computeCaptionOffset(IDocument document) {
//			return 0;
			DocumentCharacterIterator sequence= new DocumentCharacterIterator(document, offset, offset + length);
			return findFirstContent(sequence, 0);
		}
	}

	/**
	 * Projection position that will return two foldable regions: one folding away
	 * the lines before the one containing the simple name of the java element, one
	 * folding away any lines after the caption.
	 *
	 * @since 3.1
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
				IRegion postRegion= new Region(postOffset, offset + length - postOffset);

				if (preRegion == null)
					return new IRegion[] { postRegion };

				return new IRegion[] { preRegion, postRegion };
			}

			if (preRegion != null)
				return new IRegion[] { preRegion };

			return null;
		}

		/*
		 * @see org.eclipse.jface.text.source.projection.IProjectionPosition#computeCaptionOffset(org.eclipse.jface.text.IDocument)
		 */
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

	private IDocument fCachedDocument;
	private ProjectionAnnotationModel fCachedModel;

	private ITextEditor fEditor;
	private ProjectionViewer fViewer;
	private IJavaElement fInput;
	private IElementChangedListener fElementListener;

	private boolean fAllowCollapsing= false;
	private boolean fCollapseJavadoc= false;
	private boolean fCollapseImportContainer= true;
	private boolean fCollapseInnerTypes= true;
	private boolean fCollapseMethods= false;
	private boolean fCollapseHeaderComments= true;

	/* caches for header comment extraction. */
	private IType fFirstType;
	private boolean fHasHeaderComment;


	public DefaultJavaFoldingStructureProvider() {
	}

	public void install(ITextEditor editor, ProjectionViewer viewer) {
		if (editor instanceof JavaEditor) {
			fEditor= editor;
			fViewer= viewer;
			fViewer.addProjectionListener(this);
		}
	}

	public void uninstall() {
		if (isInstalled()) {
			projectionDisabled();
			fViewer.removeProjectionListener(this);
			fViewer= null;
			fEditor= null;
		}
	}

	protected boolean isInstalled() {
		return fEditor != null;
	}

	/*
	 * @see org.eclipse.jface.text.source.projection.IProjectionListener#projectionEnabled()
	 */
	public void projectionEnabled() {
		// http://home.ott.oti.com/teams/wswb/anon/out/vms/index.html
		// projectionEnabled messages are not always paired with projectionDisabled
		// i.e. multiple enabled messages may be sent out.
		// we have to make sure that we disable first when getting an enable
		// message.
		projectionDisabled();

		if (fEditor instanceof JavaEditor) {
			initialize();
			fElementListener= new ElementChangedListener();
			JavaCore.addElementChangedListener(fElementListener);
		}
	}

	/*
	 * @see org.eclipse.jface.text.source.projection.IProjectionListener#projectionDisabled()
	 */
	public void projectionDisabled() {
		fCachedDocument= null;
		if (fElementListener != null) {
			JavaCore.removeElementChangedListener(fElementListener);
			fElementListener= null;
		}
	}

	public void initialize() {

		if (!isInstalled())
			return;

		initializePreferences();

		try {

			IDocumentProvider provider= fEditor.getDocumentProvider();
			fCachedDocument= provider.getDocument(fEditor.getEditorInput());
			fAllowCollapsing= true;

			fFirstType= null;
			fHasHeaderComment= false;

			if (fEditor instanceof CompilationUnitEditor) {
				IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
				fInput= manager.getWorkingCopy(fEditor.getEditorInput());
			} else if (fEditor instanceof ClassFileEditor) {
				IClassFileEditorInput editorInput= (IClassFileEditorInput) fEditor.getEditorInput();
				fInput= editorInput.getClassFile();
			}

			if (fInput != null) {
				ProjectionAnnotationModel model= (ProjectionAnnotationModel) fEditor.getAdapter(ProjectionAnnotationModel.class);
				if (model != null) {
					fCachedModel= model;
					if (fInput instanceof ICompilationUnit) {
						ICompilationUnit unit= (ICompilationUnit) fInput;
						synchronized (unit) {
							try {
								unit.reconcile(ICompilationUnit.NO_AST, false, null, null);
							} catch (JavaModelException x) {
							}
						}
					}

					Map additions= computeAdditions((IParent) fInput);
					/*
					 *  Minimize the events being sent out - as this happens in the
					 *  UI thread merge everything into one call.
					 */
					List removals= new LinkedList();
					Iterator existing= model.getAnnotationIterator();
					while (existing.hasNext())
						removals.add(existing.next());
					model.replaceAnnotations((Annotation[]) removals.toArray(new Annotation[removals.size()]), additions);
				}
			}

		} finally {
			fCachedDocument= null;
			fCachedModel= null;
			fAllowCollapsing= false;

			fFirstType= null;
			fHasHeaderComment= false;
		}
	}

	private void initializePreferences() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		fCollapseInnerTypes= store.getBoolean(PreferenceConstants.EDITOR_FOLDING_INNERTYPES);
		fCollapseImportContainer= store.getBoolean(PreferenceConstants.EDITOR_FOLDING_IMPORTS);
		fCollapseJavadoc= store.getBoolean(PreferenceConstants.EDITOR_FOLDING_JAVADOC);
		fCollapseMethods= store.getBoolean(PreferenceConstants.EDITOR_FOLDING_METHODS);
		fCollapseHeaderComments= store.getBoolean(PreferenceConstants.EDITOR_FOLDING_HEADERS);
	}

	private Map computeAdditions(IParent parent) {
		Map map= new LinkedHashMap(); // use a linked map to maintain ordering of comments
		try {
			computeAdditions(parent.getChildren(), map);
		} catch (JavaModelException x) {
		}
		return map;
	}

	private void computeAdditions(IJavaElement[] elements, Map map) throws JavaModelException {
		for (int i= 0; i < elements.length; i++) {
			IJavaElement element= elements[i];

			computeAdditions(element, map);

			if (element instanceof IParent) {
				IParent parent= (IParent) element;
				computeAdditions(parent.getChildren(), map);
			}
		}
	}

	private void computeAdditions(IJavaElement element, Map map) {

		boolean createProjection= false;

		boolean collapse= false;
		switch (element.getElementType()) {

			case IJavaElement.IMPORT_CONTAINER:
				collapse= fAllowCollapsing && fCollapseImportContainer;
				createProjection= true;
				break;
			case IJavaElement.TYPE:
				collapse= fAllowCollapsing && fCollapseInnerTypes && isInnerType((IType) element);
				createProjection= true;
				break;
			case IJavaElement.METHOD:
				collapse= fAllowCollapsing && fCollapseMethods;
				createProjection= true;
				break;
		}

		if (createProjection) {
			IRegion[] regions= computeProjectionRanges(element);
			if (regions != null) {
				// comments
				for (int i= 0; i < regions.length - 1; i++) {
					Position position= createProjectionPosition(regions[i], null);
					boolean commentCollapse;
					if (position != null) {
						if (i == 0 && (regions.length > 2 || fHasHeaderComment) && element == fFirstType) {
							commentCollapse= fAllowCollapsing && fCollapseHeaderComments;
						} else {
							commentCollapse= fAllowCollapsing && fCollapseJavadoc;
						}
						map.put(new JavaProjectionAnnotation(element, commentCollapse, true), position);
					}
				}
				// code
				Position position= createProjectionPosition(regions[regions.length - 1], element);
				if (position != null)
					map.put(new JavaProjectionAnnotation(element, collapse, false), position);
			}
		}
	}

	private boolean isInnerType(IType type) {

		try {
			return type.isMember();
		} catch (JavaModelException x) {
			IJavaElement parent= type.getParent();
			if (parent != null) {
				int parentType= parent.getElementType();
				return (parentType != IJavaElement.COMPILATION_UNIT && parentType != IJavaElement.CLASS_FILE);
			}
		}

		return false;
	}

	/**
	 * Computes the projection ranges for a given <code>IJavaElement</code>.
	 * More than one range may be returned if the element has a leading comment
	 * which gets folded separately. If there are no foldable regions,
	 * <code>null</code> is returned.
	 *
	 * @param element the java element that can be folded
	 * @return the regions to be folded, or <code>null</code> if there are
	 *         none
	 */
	private IRegion[] computeProjectionRanges(IJavaElement element) {

		try {
			if (element instanceof ISourceReference) {
				ISourceReference reference= (ISourceReference) element;
				ISourceRange range= reference.getSourceRange();

				String contents= reference.getSource();
				if (contents == null)
					return null;

				List regions= new ArrayList();
				if (fFirstType == null && element instanceof IType) {
					fFirstType= (IType) element;
					IRegion headerComment= computeHeaderComment(fFirstType);
					if (headerComment != null) {
						regions.add(headerComment);
						fHasHeaderComment= true;
					}
				}

				IScanner scanner= ToolFactory.createScanner(true, false, false, false);
				scanner.setSource(contents.toCharArray());
				final int shift= range.getOffset();
				int start= shift;
				while (true) {

					int token= scanner.getNextToken();
					start= shift + scanner.getCurrentTokenStartPosition();

					switch (token) {
						case ITerminalSymbols.TokenNameCOMMENT_JAVADOC:
						case ITerminalSymbols.TokenNameCOMMENT_BLOCK: {
							int end= shift + scanner.getCurrentTokenEndPosition() + 1;
							regions.add(new Region(start, end - start));
						}
						case ITerminalSymbols.TokenNameCOMMENT_LINE:
							continue;
					}

					break;
				}

				regions.add(new Region(start, shift + range.getLength() - start));

				if (regions.size() > 0) {
					IRegion[] result= new IRegion[regions.size()];
					regions.toArray(result);
					return result;
				}
			}
		} catch (JavaModelException e) {
		} catch (InvalidInputException e) {
		}

		return null;
	}

	private IRegion computeHeaderComment(IType type) throws JavaModelException {
		if (fCachedDocument == null)
			return null;

		// search at most up to the first type
		ISourceRange range= type.getSourceRange();
		if (range == null)
			return null;
		int start= 0;
		int end= range.getOffset();

		if (fInput instanceof ISourceReference) {
			String content;
			try {
				content= fCachedDocument.get(start, end - start);
			} catch (BadLocationException e) {
				return null; // ignore header comment in that case
			}

			/* code adapted from CommentFormattingStrategy:
			 * scan the header content up to the first type. Once a comment is
			 * found, accumulate any additional comments up to the stop condition.
			 * The stop condition is reaching a package declaration, import container,
			 * or the end of the input.
			 */
			IScanner scanner= ToolFactory.createScanner(true, false, false, false);
			scanner.setSource(content.toCharArray());

			int headerStart= -1;
			int headerEnd= -1;
			try {
				boolean foundComment= false;
				int terminal= scanner.getNextToken();
				while (terminal != ITerminalSymbols.TokenNameEOF && !(terminal == ITerminalSymbols.TokenNameclass || terminal == ITerminalSymbols.TokenNameinterface || terminal == ITerminalSymbols.TokenNameenum || (foundComment && (terminal == ITerminalSymbols.TokenNameimport || terminal == ITerminalSymbols.TokenNamepackage)))) {

					if (terminal == ITerminalSymbols.TokenNameCOMMENT_JAVADOC || terminal == ITerminalSymbols.TokenNameCOMMENT_BLOCK || terminal == ITerminalSymbols.TokenNameCOMMENT_LINE) {
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
		}
		return null;
	}

	private Position createProjectionPosition(IRegion region, IJavaElement element) {

		if (fCachedDocument == null)
			return null;

		try {

			int start= fCachedDocument.getLineOfOffset(region.getOffset());
			int end= fCachedDocument.getLineOfOffset(region.getOffset() + region.getLength());
			if (start != end) {
				int offset= fCachedDocument.getLineOffset(start);
				int endOffset;
				if (fCachedDocument.getNumberOfLines() > end + 1)
					endOffset= fCachedDocument.getLineOffset(end + 1);
				else if (end > start)
					endOffset= fCachedDocument.getLineOffset(end) + fCachedDocument.getLineLength(end);
				else
					return null;
				if (element instanceof IMember)
					return new JavaElementPosition(offset, endOffset - offset, (IMember) element);
				else
					return new CommentPosition(offset, endOffset - offset);
			}

		} catch (BadLocationException x) {
		}

		return null;
	}

	protected void processDelta(IJavaElementDelta delta) {

		if (!isInstalled())
			return;

		if ((delta.getFlags() & (IJavaElementDelta.F_CONTENT | IJavaElementDelta.F_CHILDREN)) == 0)
			return;

		ProjectionAnnotationModel model= (ProjectionAnnotationModel) fEditor.getAdapter(ProjectionAnnotationModel.class);
		if (model == null)
			return;

		try {

			IDocumentProvider provider= fEditor.getDocumentProvider();
			fCachedDocument= provider.getDocument(fEditor.getEditorInput());
			fCachedModel= model;
			fAllowCollapsing= false;

			fFirstType= null;
			fHasHeaderComment= false;

			Map additions= new HashMap();
			List deletions= new ArrayList();
			List updates= new ArrayList();

			Map updated= computeAdditions((IParent) fInput);
			Map previous= createAnnotationMap(model);


			Iterator e= updated.keySet().iterator();
			while (e.hasNext()) {
				JavaProjectionAnnotation newAnnotation= (JavaProjectionAnnotation) e.next();
				IJavaElement element= newAnnotation.getElement();
				Position newPosition= (Position) updated.get(newAnnotation);

				List annotations= (List) previous.get(element);
				if (annotations == null) {

					additions.put(newAnnotation, newPosition);

				} else {
					Iterator x= annotations.iterator();
					boolean matched= false;
					while (x.hasNext()) {
						Tuple tuple= (Tuple) x.next();
						JavaProjectionAnnotation existingAnnotation= tuple.annotation;
						Position existingPosition= tuple.position;
						if (newAnnotation.isComment() == existingAnnotation.isComment()) {
							if (existingPosition != null && (!newPosition.equals(existingPosition))) {
								existingPosition.setOffset(newPosition.getOffset());
								existingPosition.setLength(newPosition.getLength());
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
						previous.remove(element);
				}
			}

			e= previous.values().iterator();
			while (e.hasNext()) {
				List list= (List) e.next();
				int size= list.size();
				for (int i= 0; i < size; i++)
					deletions.add(((Tuple) list.get(i)).annotation);
			}

			match(deletions, additions, updates);

			Annotation[] removals= new Annotation[deletions.size()];
			deletions.toArray(removals);
			Annotation[] changes= new Annotation[updates.size()];
			updates.toArray(changes);
			model.modifyAnnotations(removals, additions, changes);

		} finally {
			fCachedDocument= null;
			fAllowCollapsing= true;
			fCachedModel= null;

			fFirstType= null;
			fHasHeaderComment= false;
		}
	}

	/**
	 * Matches deleted annotations to changed or added ones. A deleted
	 * annotation/position tuple that has a matching addition / change
	 * is updated and marked as changed. The matching tuple is not added
	 * (for additions) or marked as deletion instead (for changes). The
	 * result is that more annotations are changed and fewer get
	 * deleted/re-added.
	 */
	private void match(List deletions, Map additions, List changes) {
		if (deletions.isEmpty() || (additions.isEmpty() && changes.isEmpty()))
			return;

		List newDeletions= new ArrayList();
		List newChanges= new ArrayList();

		Iterator deletionIterator= deletions.iterator();
		while (deletionIterator.hasNext()) {
			JavaProjectionAnnotation deleted= (JavaProjectionAnnotation) deletionIterator.next();
			Position deletedPosition= fCachedModel.getPosition(deleted);
			if (deletedPosition == null)
				continue;
			
			Tuple deletedTuple= new Tuple(deleted, deletedPosition);

			Tuple match= findMatch(deletedTuple, changes, null);
			boolean addToDeletions= true; 
			if (match == null) {
				match= findMatch(deletedTuple, additions.keySet(), additions);
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
	 * @return a matching tuple or <code>null</code> for no match
	 */
	private Tuple findMatch(Tuple tuple, Collection annotations, Map positionMap) {
		Iterator it= annotations.iterator();
		while (it.hasNext()) {
			JavaProjectionAnnotation annotation= (JavaProjectionAnnotation) it.next();
			if (tuple.annotation.isComment() == annotation.isComment()) {
				Position position= positionMap == null ? fCachedModel.getPosition(annotation) : (Position) positionMap.get(annotation);
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

	private Map createAnnotationMap(IAnnotationModel model) {
		Map map= new HashMap();
		Iterator e= model.getAnnotationIterator();
		while (e.hasNext()) {
			Object annotation= e.next();
			if (annotation instanceof JavaProjectionAnnotation) {
				JavaProjectionAnnotation java= (JavaProjectionAnnotation) annotation;
				Position position= model.getPosition(java);
				Assert.isNotNull(position);
				List list= (List) map.get(java.getElement());
				if (list == null) {
					list= new ArrayList(2);
					map.put(java.getElement(), list);
				}
				list.add(new Tuple(java, position));
			}
		}

		Comparator comparator= new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Tuple) o1).position.getOffset() - ((Tuple) o2).position.getOffset();
			}
		};
		for (Iterator it= map.values().iterator(); it.hasNext();) {
			List list= (List) it.next();
			Collections.sort(list, comparator);
		}
		return map;
	}
}
