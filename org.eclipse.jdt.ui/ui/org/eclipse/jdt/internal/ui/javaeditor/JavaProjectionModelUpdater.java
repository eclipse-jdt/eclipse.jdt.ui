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
package org.eclipse.jdt.internal.ui.javaeditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
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

import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * Updates the projection model of a class file or compilation unit.
 * @since 3.0
 */
public class JavaProjectionModelUpdater implements IProjectionListener {
	
	private static class JavaProjectionAnnotation extends ProjectionAnnotation {
		
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
			if (children == null || children.length == 0)
				return null;
				
			for (int i= 0; i < children.length; i++) {
				IJavaElementDelta d= findElement(target, children[i]);
				if (d != null)
					return d;
			}
			
			return null;
		}		
	}
	
	
	private IDocument fCachedDocument;
	
	private JavaEditor fEditor;
	private ProjectionViewer fViewer;
	private IJavaElement fInput;
	private IElementChangedListener fElementListener;
	
	private boolean fAllowCollapsing= false;
	private boolean fCollapseJavadoc= false;
	private boolean fCollapseInportContainer= true;
	private boolean fCollapseInnerTypes= true;
	private boolean fCollapseMethods= false;
	
	public JavaProjectionModelUpdater() {
	}
	
	public void install(JavaEditor editor, ProjectionViewer viewer) {
		fEditor= editor;
		fViewer= viewer;
		fViewer.addProjectionListener(this);
		projectionEnabled();
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
		if (fEditor instanceof CompilationUnitEditor) {
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
		
		try {
			
			IDocumentProvider provider= fEditor.getDocumentProvider();
			fCachedDocument= provider.getDocument(fEditor.getEditorInput());
			fAllowCollapsing= true;
			
			if (fEditor instanceof CompilationUnitEditor) {
				IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
				fInput= manager.getWorkingCopy(fEditor.getEditorInput());
			} else if (fEditor instanceof ClassFileEditor) {
				IClassFileEditorInput editorInput= (IClassFileEditorInput) fEditor.getEditorInput();
				fInput= editorInput.getClassFile();
			}
			
			ProjectionAnnotationModel model= (ProjectionAnnotationModel) fEditor.getAdapter(ProjectionAnnotationModel.class);
			if (model != null) {
				Map additions= computeAdditions((IParent) fInput);
				model.removeAllAnnotations();
				model.replaceAnnotations(null, additions);
			}
			
		} finally {
			fCachedDocument= null;
			fAllowCollapsing= false;
		}
	}

	private Map computeAdditions(IParent parent) {
		Map map= new HashMap();
		try {
			computeAdditions(parent.getChildren(), map);
		} catch (JavaModelException x) {
		}
		return map;
	}

	private void computeAdditions(IJavaElement[] elements, Map map) throws JavaModelException {
		for (int i= 0; i < elements.length; i++) {
			IJavaElement element= elements[i];
			
			try {
				computeAdditions(element, map);
			} catch (JavaModelException x) {
			}
			
			if (element instanceof IParent) {
				IParent parent= (IParent) element;
				computeAdditions(parent.getChildren(), map);
			}
		}
	}

	private void computeAdditions(IJavaElement element, Map map) throws JavaModelException {
		
		boolean createProjection= false;
		
		boolean collapse= false;
		switch (element.getElementType()) {
			
			case IJavaElement.IMPORT_CONTAINER:
				collapse= fAllowCollapsing && fCollapseInportContainer;
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
					if (position != null)
						map.put(new JavaProjectionAnnotation(element, fAllowCollapsing && fCollapseJavadoc, true), position);
				}
				// code
				Position position= createProjectionPosition(regions[regions.length - 1]);
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

	private IRegion[] computeProjectionRanges(IJavaElement element) {
		
		try {
			if (element instanceof ISourceReference) {
				ISourceReference reference= (ISourceReference) element;
				ISourceRange range= reference.getSourceRange();
				String contents= reference.getSource();
				
				IScanner scanner= ToolFactory.createScanner(true, false, false, false);
				scanner.setSource(contents.toCharArray());
				List regions= new ArrayList();
				int shift= range.getOffset();
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
				
				regions.add(new Region(start, range.getOffset() + range.getLength() - start));
				
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
	
	private Position createProjectionPosition(IRegion region) {
		
		if (fCachedDocument == null)
			return null;
		
		try {
			
			int start= fCachedDocument.getLineOfOffset(region.getOffset());
			int end= fCachedDocument.getLineOfOffset(region.getOffset() + region.getLength());
			if (start != end) {
				int offset= fCachedDocument.getLineOffset(start);
				int endOffset= fCachedDocument.getLineOffset(end + 1);
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
							if (!position.equals(p)) {
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
			
			Iterator changesIterator= changes.iterator();
			while (changesIterator.hasNext()) {
				JavaProjectionAnnotation changed= (JavaProjectionAnnotation) changesIterator.next();
				if (deleted.isComment() == changed.isComment()) {
					Position changedPosition= model.getPosition(changed);
					if (deletedPosition.getOffset() == changedPosition.getOffset()) {
						
						deletedPosition.setLength(changedPosition.getLength());
						deleted.setElement(changed.getElement());
						
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
