/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.folding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
		private int fCaptionOffset;
		
		public JavaProjectionAnnotation(IJavaElement element, boolean isCollapsed, boolean isComment, int captionOffset) {
			super(isCollapsed);
			fJavaElement= element;
			fIsComment= isComment;
			fCaptionOffset= captionOffset;
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
		
		protected int getCaptionOffset() {
			return fCaptionOffset;
		}
		
		void setCaptionOffset(int offset) {
			fCaptionOffset= offset;
		}
		
		/*
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "JavaProjectionAnnotation:\n" + //$NON-NLS-1$
					"\telement: \t"+fJavaElement.toString()+"\n" + //$NON-NLS-1$ //$NON-NLS-2$
					"\tcollapsed: \t" + isCollapsed() + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
					"\tcomment: \t" + fIsComment + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
					"\tcaptionOffset: \t" + fCaptionOffset + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
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
	
	private IDocument fCachedDocument;
	
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
					model.removeAllAnnotations();
					model.replaceAnnotations(null, additions);
				}
			}
			
		} finally {
			fCachedDocument= null;
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
					Position position= createProjectionPosition(regions[i]);
					boolean commentCollapse;
					if (position != null) {
						if (i == 0 && (regions.length > 2 || fHasHeaderComment) && element == fFirstType) {
							commentCollapse= fAllowCollapsing && fCollapseHeaderComments;
						} else {
							commentCollapse= fAllowCollapsing && fCollapseJavadoc;
						}
						map.put(new JavaProjectionAnnotation(element, commentCollapse, true, computeCaptionOffset(position, null)), position);
					}
				}
				// code
				Position position= createProjectionPosition(regions[regions.length - 1]);
				if (position != null)
					map.put(new JavaProjectionAnnotation(element, collapse, false, computeCaptionOffset(position, element)), position);
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
					
					if (terminal == ITerminalSymbols.TokenNameCOMMENT_JAVADOC || terminal == ITerminalSymbols.TokenNameCOMMENT_BLOCK) {
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
	
	/**
	 * Computes the offset of the caption text start relative to
	 * <code>position</code>.
	 * 
	 * @param position the position of the folded region
	 * @param element the java element belonging to the folded region, or <code>null</code>
	 * @return the caption offset relative to the position offset
	 */
	private int computeCaptionOffset(Position position, IJavaElement element) {
		Assert.isNotNull(fCachedDocument); // must be non-null as createPosition succeeded
		if (element instanceof IMember) {
			try {
				ISourceRange nameRange= ((IMember) element).getNameRange();
				if (nameRange != null)
					return nameRange.getOffset() - position.getOffset();
			} catch (JavaModelException e) {
				// ignore and return 0
			}
		}
		
		return findFirstContent(new DocumentCharacterIterator(fCachedDocument, position.getOffset(), position.getOffset() + position.getLength()));
	}
	
	/**
	 * Finds the offset of the first identifier part within <code>content</code>.
	 * Returns 0 if none is found.
	 * 
	 * @param content the content to search
	 * @return the first index of a unicode identifier part, or zero if none can
	 *         be found
	 */
	private int findFirstContent(final CharSequence content) {
		int lenght= content.length();
		for (int i= 0; i < lenght; i++) {
			if (Character.isUnicodeIdentifierPart(content.charAt(i)))
				return i;
			i++;
		}
		return 0;
	}

	private Position createProjectionPosition(IRegion region) {
		
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
					endOffset= fCachedDocument.getLineOffset(end);
				else
					return null;
				return new Position(offset, endOffset - offset);
			}
			
		} catch (BadLocationException x) {
		}
		
		return null;
	}
		
	protected void processDelta(IJavaElementDelta delta) {
		
		if (!isInstalled())
			return;
		
		ProjectionAnnotationModel model= (ProjectionAnnotationModel) fEditor.getAdapter(ProjectionAnnotationModel.class);
		if (model == null)
			return;
		
		try {
			
			IDocumentProvider provider= fEditor.getDocumentProvider();
			fCachedDocument= provider.getDocument(fEditor.getEditorInput());
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
				JavaProjectionAnnotation annotation= (JavaProjectionAnnotation) e.next();
				IJavaElement element= annotation.getElement();
				Position position= (Position) updated.get(annotation);
				
				List annotations= (List) previous.get(element);
				if (annotations == null) {
					
					additions.put(annotation, position);
					
				} else {
					
					Iterator x= annotations.iterator();
					while (x.hasNext()) {
						JavaProjectionAnnotation a= (JavaProjectionAnnotation) x.next();
						if (annotation.isComment() == a.isComment()) {
							Position p= model.getPosition(a);
							if (p != null && !position.equals(p)) {
								p.setOffset(position.getOffset());
								p.setLength(position.getLength());
								updates.add(a);
							}
							x.remove();
							break;
						}
					}
										
					if (annotations.isEmpty())
						previous.remove(element);
				}
			}
			
			e= previous.values().iterator();
			while (e.hasNext()) {
				List list= (List) e.next();
				int size= list.size();
				for (int i= 0; i < size; i++)
					deletions.add(list.get(i));
			}
			
			match(model, deletions, additions, updates);
			
			Annotation[] removals= new Annotation[deletions.size()];
			deletions.toArray(removals);
			Annotation[] changes= new Annotation[updates.size()];
			updates.toArray(changes);
			model.modifyAnnotations(removals, additions, changes);
			
		} finally {
			fCachedDocument= null;
			fAllowCollapsing= true;
			
			fFirstType= null;
			fHasHeaderComment= false;
		}
	}
	
	private void match(ProjectionAnnotationModel model, List deletions, Map additions, List changes) {
		if (deletions.isEmpty() || (additions.isEmpty() && changes.isEmpty()))
			return;
		
		List newDeletions= new ArrayList();
		List newChanges= new ArrayList();
		
		Iterator deletionIterator= deletions.iterator();
		outer: while (deletionIterator.hasNext()) {
			JavaProjectionAnnotation deleted= (JavaProjectionAnnotation) deletionIterator.next();
			Position deletedPosition= model.getPosition(deleted);
			if (deletedPosition == null)
				continue;
			
			Iterator changesIterator= changes.iterator();
			while (changesIterator.hasNext()) {
				JavaProjectionAnnotation changed= (JavaProjectionAnnotation) changesIterator.next();
				if (deleted.isComment() == changed.isComment()) {
					Position changedPosition= model.getPosition(changed);
					if (changedPosition == null)
						continue;
					
					if (deletedPosition.getOffset() == changedPosition.getOffset()) {
						
						deletedPosition.setLength(changedPosition.getLength());
						deleted.setElement(changed.getElement());
						deleted.setCaptionOffset(changed.getCaptionOffset());
						
						deletionIterator.remove();
						newChanges.add(deleted);
						
						changesIterator.remove();
						newDeletions.add(changed);
						
						continue outer;
					}
				}
			}
			
			Iterator additionsIterator= additions.keySet().iterator();
			while (additionsIterator.hasNext()) {
				JavaProjectionAnnotation added= (JavaProjectionAnnotation) additionsIterator.next();
				if (deleted.isComment() == added.isComment()) {
					Position addedPosition= (Position) additions.get(added);
					
					if (deletedPosition.getOffset() == addedPosition.getOffset()) {
						
						deletedPosition.setLength(addedPosition.getLength());
						deleted.setElement(added.getElement());
						deleted.setCaptionOffset(added.getCaptionOffset());
						
						deletionIterator.remove();
						newChanges.add(deleted);
						
						additionsIterator.remove();
						
						break;
					}
				}
			}
		}
		
		deletions.addAll(newDeletions);
		changes.addAll(newChanges);
	}

	private Map createAnnotationMap(IAnnotationModel model) {
		Map map= new HashMap();
		Iterator e= model.getAnnotationIterator();
		while (e.hasNext()) {
			Object annotation= e.next();
			if (annotation instanceof JavaProjectionAnnotation) {
				JavaProjectionAnnotation java= (JavaProjectionAnnotation) annotation;
				List list= (List) map.get(java.getElement());
				if (list == null) {
					list= new ArrayList(2);
					map.put(java.getElement(), list);
				}
				list.add(java);
			}
		}
		return map;
	}
}
