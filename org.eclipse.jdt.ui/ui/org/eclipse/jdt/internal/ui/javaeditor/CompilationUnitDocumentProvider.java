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


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.ListenerList;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModelEvent;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelListener;
import org.eclipse.jface.text.source.IAnnotationModelListenerExtension;
import org.eclipse.jface.text.source.IAnnotationPresentation;

import org.eclipse.ui.editors.text.FileDocumentProvider;

import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.eclipse.ui.texteditor.DefaultAnnotation;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.eclipse.ui.texteditor.ResourceMarkerAnnotationModel;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.IJavaStatusConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.JavaUIStatus;
import org.eclipse.jdt.internal.ui.text.IJavaPartitions;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;
import org.eclipse.jdt.internal.ui.text.java.IProblemRequestorExtension;


public class CompilationUnitDocumentProvider extends FileDocumentProvider implements ICompilationUnitDocumentProvider {
		
		/**
		 * Here for visibility issues only.
		 */
		protected class _FileSynchronizer extends FileSynchronizer {
			public _FileSynchronizer(IFileEditorInput fileEditorInput) {
				super(fileEditorInput);
			}
		}
		
		/**
		 * Bundle of all required informations to allow working copy management. 
		 */
		protected class CompilationUnitInfo extends FileInfo {
			
			ICompilationUnit fCopy;
			
			public CompilationUnitInfo(IDocument document, IAnnotationModel model, _FileSynchronizer fileSynchronizer, ICompilationUnit copy) {
				super(document, model, fileSynchronizer);
				fCopy= copy;
			}
			
			public void setModificationStamp(long timeStamp) {
				fModificationStamp= timeStamp;
			}
		}
		
		/**
		 * Annotation representating an <code>IProblem</code>.
		 */
		static protected class ProblemAnnotation extends Annotation implements IJavaAnnotation, IAnnotationPresentation {

			private static Image fgQuickFixImage;
			private static Image fgQuickFixErrorImage;
			private static boolean fgQuickFixImagesInitialized= false;
			
			private ICompilationUnit fCompilationUnit;
			private List fOverlaids;
			private IProblem fProblem;
			private Image fImage;
			private boolean fQuickFixImagesInitialized= false;
			
			
			public ProblemAnnotation(IProblem problem, ICompilationUnit cu) {
				
				fProblem= problem;
				fCompilationUnit= cu;
				
				if (IProblem.Task == fProblem.getID()) {
					setType(JavaMarkerAnnotation.TASK_ANNOTATION_TYPE);
					setLayer(DefaultAnnotation.TASK_LAYER + 1);
				} else if (fProblem.isWarning()) {
					setType(JavaMarkerAnnotation.WARNING_ANNOTATION_TYPE);
					setLayer(DefaultAnnotation.WARNING_LAYER + 1);
				} else if (fProblem.isError()) {
					setType(JavaMarkerAnnotation.ERROR_ANNOTATION_TYPE);
					setLayer(DefaultAnnotation.ERROR_LAYER + 1);
				} else {
					setType(JavaMarkerAnnotation.INFO_ANNOTATION_TYPE);
					setLayer(DefaultAnnotation.INFO_LAYER + 1);
				}
			}
			
			private void initializeImages() {
				// http://bugs.eclipse.org/bugs/show_bug.cgi?id=18936
				if (!fQuickFixImagesInitialized) {
					if (isProblem() && indicateQuixFixableProblems() && JavaCorrectionProcessor.hasCorrections(this)) { // no light bulb for tasks
						if (!fgQuickFixImagesInitialized) {
							fgQuickFixImage= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_FIXABLE_PROBLEM);
							fgQuickFixErrorImage= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_FIXABLE_ERROR);
							fgQuickFixImagesInitialized= true;
						}
						if (JavaMarkerAnnotation.ERROR_ANNOTATION_TYPE.equals(getType()))
							fImage= fgQuickFixErrorImage;
						else
							fImage= fgQuickFixImage;
					}
					fQuickFixImagesInitialized= true;
				}
			}

			private boolean indicateQuixFixableProblems() {
				return PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_CORRECTION_INDICATION);
			}
						
			/*
			 * @see Annotation#paint
			 */
			public void paint(GC gc, Canvas canvas, Rectangle r) {
				initializeImages();
				if (fImage != null)
					drawImage(fImage, gc, canvas, r, SWT.CENTER, SWT.TOP);
			}
			
			/*
			 * @see IJavaAnnotation#getImage(Display)
			 */
			public Image getImage(Display display) {
				initializeImages();
				return fImage;
			}
			
			/*
			 * @see IJavaAnnotation#getMessage()
			 */
			public String getText() {
				return fProblem.getMessage();
			}
			
			/*
			 * @see IJavaAnnotation#getArguments()
			 */
			public String[] getArguments() {
				return isProblem() ? fProblem.getArguments() : null;
			}

			/*
			 * @see IJavaAnnotation#getId()
			 */
			public int getId() {
				return fProblem.getID();
			}

			/*
			 * @see IJavaAnnotation#isProblem()
			 */
			public boolean isProblem() {
				String type= getType();
				return  JavaMarkerAnnotation.WARNING_ANNOTATION_TYPE.equals(type)  || JavaMarkerAnnotation.ERROR_ANNOTATION_TYPE.equals(type);
			}
			
			/*
			 * @see IJavaAnnotation#hasOverlay()
			 */
			public boolean hasOverlay() {
				return false;
			}
			
			/*
			 * @see org.eclipse.jdt.internal.ui.javaeditor.IJavaAnnotation#getOverlay()
			 */
			public IJavaAnnotation getOverlay() {
				return null;
			}
			
			/*
			 * @see IJavaAnnotation#addOverlaid(IJavaAnnotation)
			 */
			public void addOverlaid(IJavaAnnotation annotation) {
				if (fOverlaids == null)
					fOverlaids= new ArrayList(1);
				fOverlaids.add(annotation);
			}

			/*
			 * @see IJavaAnnotation#removeOverlaid(IJavaAnnotation)
			 */
			public void removeOverlaid(IJavaAnnotation annotation) {
				if (fOverlaids != null) {
					fOverlaids.remove(annotation);
					if (fOverlaids.size() == 0)
						fOverlaids= null;
				}
			}
			
			/*
			 * @see IJavaAnnotation#getOverlaidIterator()
			 */
			public Iterator getOverlaidIterator() {
				if (fOverlaids != null)
					return fOverlaids.iterator();
				return null;
			}
			
			/*
			 * @see org.eclipse.jdt.internal.ui.javaeditor.IJavaAnnotation#getCompilationUnit()
			 */
			public ICompilationUnit getCompilationUnit() {
				return fCompilationUnit;
			}
		}
		
		/**
		 * Internal structure for mapping positions to some value. 
		 * The reason for this specific structure is that positions can
		 * change over time. Thus a lookup is based on value and not
		 * on hash value.
		 */
		protected static class ReverseMap {
			
			static class Entry {
				Position fPosition;
				Object fValue;
			}
			
			private List fList= new ArrayList(2);
			private int fAnchor= 0;
			
			public ReverseMap() {
			}
			
			public Object get(Position position) {
				
				Entry entry;
				
				// behind anchor
				int length= fList.size();
				for (int i= fAnchor; i < length; i++) {
					entry= (Entry) fList.get(i);
					if (entry.fPosition.equals(position)) {
						fAnchor= i;
						return entry.fValue;
					}
				}
				
				// before anchor
				for (int i= 0; i < fAnchor; i++) {
					entry= (Entry) fList.get(i);
					if (entry.fPosition.equals(position)) {
						fAnchor= i;
						return entry.fValue;
					}
				}
				
				return null;
			}
			
			private int getIndex(Position position) {
				Entry entry;
				int length= fList.size();
				for (int i= 0; i < length; i++) {
					entry= (Entry) fList.get(i);
					if (entry.fPosition.equals(position))
						return i;
				}
				return -1;
			}
			
			public void put(Position position,  Object value) {
				int index= getIndex(position);
				if (index == -1) {
					Entry entry= new Entry();
					entry.fPosition= position;
					entry.fValue= value;
					fList.add(entry);
				} else {
					Entry entry= (Entry) fList.get(index);
					entry.fValue= value;
				}
			}
			
			public void remove(Position position) {
				int index= getIndex(position);
				if (index > -1)
					fList.remove(index);
			}
			
			public void clear() {
				fList.clear();
			}
		}
		
		/**
		 * Annotation model dealing with java marker annotations and temporary problems.
		 * Also acts as problem requestor for its compilation unit. Initialiy inactive. Must explicitly be
		 * activated.
		 */
		protected class CompilationUnitAnnotationModel extends ResourceMarkerAnnotationModel implements IProblemRequestor, IProblemRequestorExtension {
			
			private IFileEditorInput fInput;
			private List fCollectedProblems;
			private List fGeneratedAnnotations;
			private IProgressMonitor fProgressMonitor;
			private boolean fIsActive= false;
			
			private ReverseMap fReverseMap= new ReverseMap();
			private List fPreviouslyOverlaid= null; 
			private List fCurrentlyOverlaid= new ArrayList();
			private boolean fIncludesProblemAnnotationChanges= false;

			public CompilationUnitAnnotationModel(IFileEditorInput input) {
				super(input.getFile());
				fInput= input;
			}
			
			protected MarkerAnnotation createMarkerAnnotation(IMarker marker) {
				String markerType= MarkerUtilities.getMarkerType(marker);
				if (markerType != null && markerType.startsWith(JavaMarkerAnnotation.JAVA_MARKER_TYPE_PREFIX))
					return new JavaMarkerAnnotation(marker);
				return super.createMarkerAnnotation(marker);
				
			}
			
			/*
			 * @see org.eclipse.jface.text.source.AnnotationModel#createAnnotationModelEvent()
			 */
			protected AnnotationModelEvent createAnnotationModelEvent() {
				return new CompilationUnitAnnotationModelEvent(this, getResource());
			}
			
			protected Position createPositionFromProblem(IProblem problem) {
				int start= problem.getSourceStart();
				if (start < 0)
					return null;
					
				int length= problem.getSourceEnd() - problem.getSourceStart() + 1;
				if (length < 0)
					return null;
					
				return new Position(start, length);
			}

			protected void update(IMarkerDelta[] markerDeltas) {
	
				super.update(markerDeltas);

				if (fIncludesProblemAnnotationChanges) {
					try {
						ICompilationUnit workingCopy= getWorkingCopy(fInput);
						if (workingCopy != null)
							workingCopy.reconcile(true, null);
					} catch (JavaModelException ex) {
						if (!ex.isDoesNotExist())
							handleCoreException(ex, ex.getMessage());
					}
				}
			}
			
			/*
			 * @see IProblemRequestor#beginReporting()
			 */
			public void beginReporting() {
				ICompilationUnit unit= getWorkingCopy(fInput);
				if (unit != null && unit.getJavaProject().isOnClasspath(unit))
					fCollectedProblems= new ArrayList();
				else
					fCollectedProblems= null;
			}
			
			/*
			 * @see IProblemRequestor#acceptProblem(IProblem)
			 */
			public void acceptProblem(IProblem problem) {
				if (isActive())
					fCollectedProblems.add(problem);
			}

			/*
			 * @see IProblemRequestor#endReporting()
			 */
			public void endReporting() {
				if (!isActive())
					return;
					
				if (fProgressMonitor != null && fProgressMonitor.isCanceled())
					return;
					
				
				boolean isCanceled= false;
				boolean temporaryProblemsChanged= false;
				
				synchronized (fAnnotations) {
					
					fPreviouslyOverlaid= fCurrentlyOverlaid;
					fCurrentlyOverlaid= new ArrayList();

					if (fGeneratedAnnotations.size() > 0) {
						temporaryProblemsChanged= true;	
						removeAnnotations(fGeneratedAnnotations, false, true);
						fGeneratedAnnotations.clear();
					}
					
					if (fCollectedProblems != null && fCollectedProblems.size() > 0) {
						
						ICompilationUnit cu= getWorkingCopy(fInput);
						Iterator e= fCollectedProblems.iterator();
						while (e.hasNext()) {
							
							IProblem problem= (IProblem) e.next();
							
							if (fProgressMonitor != null && fProgressMonitor.isCanceled()) {
								isCanceled= true;
								break;
							}
								
							Position position= createPositionFromProblem(problem);
							if (position != null) {
								try {
									ProblemAnnotation annotation= new ProblemAnnotation(problem, cu);
									addAnnotation(annotation, position, false);
									overlayMarkers(position, annotation);								
									fGeneratedAnnotations.add(annotation);
									
									temporaryProblemsChanged= true;
								} catch (BadLocationException x) {
									// ignore invalid position
								}
							}
						}
						
						fCollectedProblems.clear();
					}
					
					removeMarkerOverlays(isCanceled);
					fPreviouslyOverlaid.clear();
					fPreviouslyOverlaid= null;
				}
					
				if (temporaryProblemsChanged)
					fireModelChanged();
			}
			
			private void removeMarkerOverlays(boolean isCanceled) {
				if (isCanceled) {
					fCurrentlyOverlaid.addAll(fPreviouslyOverlaid);
				} else if (fPreviouslyOverlaid != null) {
					Iterator e= fPreviouslyOverlaid.iterator();
					while (e.hasNext()) {
						JavaMarkerAnnotation annotation= (JavaMarkerAnnotation) e.next();
						annotation.setOverlay(null);
					}
				}			
			}
			
			/**
			 * Overlays value with problem annotation.
			 * @param problemAnnotation
			 */
			private void setOverlay(Object value, ProblemAnnotation problemAnnotation) {
				if (value instanceof  JavaMarkerAnnotation) {
					JavaMarkerAnnotation annotation= (JavaMarkerAnnotation) value;
					if (annotation.isProblem()) {
						annotation.setOverlay(problemAnnotation);
						fPreviouslyOverlaid.remove(annotation);
						fCurrentlyOverlaid.add(annotation);
					}
				}
			}
			
			private void  overlayMarkers(Position position, ProblemAnnotation problemAnnotation) {
				Object value= getAnnotations(position);
				if (value instanceof List) {
					List list= (List) value;
					for (Iterator e = list.iterator(); e.hasNext();)
						setOverlay(e.next(), problemAnnotation);
				} else {
					setOverlay(value, problemAnnotation);
				}
			}
			
			/**
			 * Tells this annotation model to collect temporary problems from now on.
			 */
			private void startCollectingProblems() {
				fCollectedProblems= new ArrayList();
				fGeneratedAnnotations= new ArrayList();  
			}
			
			/**
			 * Tells this annotation model to no longer collect temporary problems.
			 */
			private void stopCollectingProblems() {
				if (fGeneratedAnnotations != null) {
					removeAnnotations(fGeneratedAnnotations, true, true);
					fGeneratedAnnotations.clear();
				}
				fCollectedProblems= null;
				fGeneratedAnnotations= null;
			}
			
			/*
			 * @see org.eclipse.jface.text.source.AnnotationModel#fireModelChanged(org.eclipse.jface.text.source.AnnotationModelEvent)
			 */
			protected void fireModelChanged(AnnotationModelEvent event) {
				if (event instanceof CompilationUnitAnnotationModelEvent) {
					CompilationUnitAnnotationModelEvent e= (CompilationUnitAnnotationModelEvent) event;
					fIncludesProblemAnnotationChanges= e.includesProblemMarkerAnnotationChanges();
				}
				super.fireModelChanged(event);
			}
			
			/*
			 * @see IProblemRequestor#isActive()
			 */
			public boolean isActive() {
				return fIsActive && (fCollectedProblems != null);
			}
			
			/*
			 * @see IProblemRequestorExtension#setProgressMonitor(IProgressMonitor)
			 */
			public void setProgressMonitor(IProgressMonitor monitor) {
				fProgressMonitor= monitor;
			}
			
			/*
			 * @see IProblemRequestorExtension#setIsActive(boolean)
			 */
			public void setIsActive(boolean isActive) {
				if (fIsActive != isActive) {
					fIsActive= isActive;
					if (fIsActive)
						startCollectingProblems();
					else
						stopCollectingProblems();
				}
			}
			
			private Object getAnnotations(Position position) {
				return fReverseMap.get(position);
			}
						
			/*
			 * @see AnnotationModel#addAnnotation(Annotation, Position, boolean)
			 */
			protected void addAnnotation(Annotation annotation, Position position, boolean fireModelChanged) throws BadLocationException {
				super.addAnnotation(annotation, position, fireModelChanged);
				
				Object cached= fReverseMap.get(position);
				if (cached == null)
					fReverseMap.put(position, annotation);
				else if (cached instanceof List) {
					List list= (List) cached;
					list.add(annotation);
				} else if (cached instanceof Annotation) {
					List list= new ArrayList(2);
					list.add(cached);
					list.add(annotation);
					fReverseMap.put(position, list);
				}
			}
			
			/*
			 * @see AnnotationModel#removeAllAnnotations(boolean)
			 */
			protected void removeAllAnnotations(boolean fireModelChanged) {
				super.removeAllAnnotations(fireModelChanged);
				fReverseMap.clear();
			}
			
			/*
			 * @see AnnotationModel#removeAnnotation(Annotation, boolean)
			 */
			protected void removeAnnotation(Annotation annotation, boolean fireModelChanged) {
				Position position= getPosition(annotation);
				Object cached= fReverseMap.get(position);
				if (cached instanceof List) {
					List list= (List) cached;
					list.remove(annotation);
					if (list.size() == 1) {
						fReverseMap.put(position, list.get(0));
						list.clear();
					}
				} else if (cached instanceof Annotation) {
					fReverseMap.remove(position);
				}
				super.removeAnnotation(annotation, fireModelChanged);
			}
		}
		
		
		protected static class GlobalAnnotationModelListener implements IAnnotationModelListener, IAnnotationModelListenerExtension {
			
			private ListenerList fListenerList;
			
			public GlobalAnnotationModelListener() {
				fListenerList= new ListenerList();
			}
			
			/**
			 * @see IAnnotationModelListener#modelChanged(IAnnotationModel)
			 */
			public void modelChanged(IAnnotationModel model) {
				Object[] listeners= fListenerList.getListeners();
				for (int i= 0; i < listeners.length; i++) {
					((IAnnotationModelListener) listeners[i]).modelChanged(model);
				}
			}

			/**
			 * @see IAnnotationModelListenerExtension#modelChanged(AnnotationModelEvent)
			 */
			public void modelChanged(AnnotationModelEvent event) {
				Object[] listeners= fListenerList.getListeners();
				for (int i= 0; i < listeners.length; i++) {
					Object curr= listeners[i];
					if (curr instanceof IAnnotationModelListenerExtension) {
						((IAnnotationModelListenerExtension) curr).modelChanged(event);
					}
				}
			}
			
			public void addListener(IAnnotationModelListener listener) {
				fListenerList.add(listener);
			}
			
			public void removeListener(IAnnotationModelListener listener) {
				fListenerList.remove(listener);
			}			
		}
			
		
	/** Preference key for temporary problems */
	private final static String HANDLE_TEMPORARY_PROBLEMS= PreferenceConstants.EDITOR_EVALUTE_TEMPORARY_PROBLEMS;
	
	
	/** Indicates whether the save has been initialized by this provider */
	private boolean fIsAboutToSave= false;
	/** The save policy used by this provider */
	private ISavePolicy fSavePolicy;
	/** Internal property changed listener */
	private IPropertyChangeListener fPropertyListener;
	/** Annotation model listener added to all created CU annotation models */
	private GlobalAnnotationModelListener fGlobalAnnotationModelListener;	
	
	/**
	 * Constructor
	 */
	public CompilationUnitDocumentProvider() {
		fPropertyListener= new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (HANDLE_TEMPORARY_PROBLEMS.equals(event.getProperty()))
					enableHandlingTemporaryProblems();
			}
		};
		
		fGlobalAnnotationModelListener= new GlobalAnnotationModelListener();
		
		JavaPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(fPropertyListener);
	}
	
	/**
	 * Creates a compilation unit from the given file.
	 * 
	 * @param file the file from which to create the compilation unit
	 */
	protected ICompilationUnit createCompilationUnit(IFile file) {
		Object element= JavaCore.create(file);
		if (element instanceof ICompilationUnit)
			return (ICompilationUnit) element;
		return null;
	}

	/*
	 * @see AbstractDocumentProvider#createElementInfo(Object)
	 */
	protected ElementInfo createElementInfo(Object element) throws CoreException {
		
		if ( !(element instanceof IFileEditorInput))
			return super.createElementInfo(element);
			
		IFileEditorInput input= (IFileEditorInput) element;
		ICompilationUnit original= createCompilationUnit(input.getFile());
		if (original != null) {
				
			try {
								
				try {
					refreshFile(input.getFile());
				} catch (CoreException x) {
					handleCoreException(x, JavaEditorMessages.getString("CompilationUnitDocumentProvider.error.createElementInfo")); //$NON-NLS-1$
				}
				
				IAnnotationModel m= createCompilationUnitAnnotationModel(input);
				IProblemRequestor r= m instanceof IProblemRequestor ? (IProblemRequestor) m : null;
				
				ICompilationUnit c= null;
				if (JavaPlugin.USE_WORKING_COPY_OWNERS)  {
					original.becomeWorkingCopy(r, getProgressMonitor());
					c= original;
				} else  {
					c= (ICompilationUnit) original.getSharedWorkingCopy(getProgressMonitor(), JavaPlugin.getDefault().getBufferFactory(), r);
				}
				
				DocumentAdapter a= null;
				try {
					a= (DocumentAdapter) c.getBuffer();
				} catch (ClassCastException x) {
					IStatus status= new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IJavaStatusConstants.TEMPLATE_IO_EXCEPTION, "Shared working copy has wrong buffer", x); //$NON-NLS-1$
					throw new CoreException(status);
				}
				
				_FileSynchronizer f= new _FileSynchronizer(input); 
				f.install();
				
				CompilationUnitInfo info= new CompilationUnitInfo(a.getDocument(), m, f, c);
				info.setModificationStamp(computeModificationStamp(input.getFile()));
				info.fStatus= a.getStatus();
				info.fEncoding= getPersistedEncoding(input);
				
				if (r instanceof IProblemRequestorExtension) {
					IProblemRequestorExtension extension= (IProblemRequestorExtension) r;
					extension.setIsActive(isHandlingTemporaryProblems());
				}
				m.addAnnotationModelListener(fGlobalAnnotationModelListener);
				
				return info;
				
			} catch (JavaModelException x) {
				throw new CoreException(x.getStatus());
			}
		} else {		
			return super.createElementInfo(element);
		}
	}
	
	/*
	 * @see AbstractDocumentProvider#disposeElementInfo(Object, ElementInfo)
	 */
	protected void disposeElementInfo(Object element, ElementInfo info) {
		
		if (info instanceof CompilationUnitInfo) {
			CompilationUnitInfo cuInfo= (CompilationUnitInfo) info;
			
			if (JavaPlugin.USE_WORKING_COPY_OWNERS)  {
				
				try  {
					cuInfo.fCopy.discardWorkingCopy();
				} catch (JavaModelException x)  {
					handleCoreException(x, x.getMessage());
				}
			
			} else  {
				cuInfo.fCopy.destroy();
			}
			
			cuInfo.fModel.removeAnnotationModelListener(fGlobalAnnotationModelListener);
		}
		
		super.disposeElementInfo(element, info);
	}
	
	/*
	 * @see AbstractDocumentProvider#doSaveDocument(IProgressMonitor, Object, IDocument, boolean)
	 */
	protected void doSaveDocument(IProgressMonitor monitor, Object element, IDocument document, boolean overwrite) throws CoreException {
				
		ElementInfo elementInfo= getElementInfo(element);		
		if (elementInfo instanceof CompilationUnitInfo) {
			CompilationUnitInfo info= (CompilationUnitInfo) elementInfo;

			synchronized (info.fCopy) {
				info.fCopy.reconcile();
			}
			
			ICompilationUnit original= (ICompilationUnit) info.fCopy.getOriginalElement();
			IResource resource= original.getResource();
			
			if (resource == null) {
				// underlying resource has been deleted, just recreate file, ignore the rest
				super.doSaveDocument(monitor, element, document, overwrite);
				return;
			}
			
			if (resource != null && !overwrite)
				checkSynchronizationState(info.fModificationStamp, resource);
				
			if (fSavePolicy != null)
				fSavePolicy.preSave(info.fCopy);
			
			// inform about the upcoming content change
			fireElementStateChanging(element);	
			try {
				
				fIsAboutToSave= true;
				
				// commit working copy
				if (JavaPlugin.USE_WORKING_COPY_OWNERS)
					info.fCopy.commitWorkingCopy(overwrite, monitor);
				else
					info.fCopy.commit(overwrite, monitor);
					
			} catch (CoreException x) {
				// inform about the failure
				fireElementStateChangeFailed(element);
				throw x;
			} catch (RuntimeException x) {
				// inform about the failure
				fireElementStateChangeFailed(element);
				throw x;
			} finally {
				fIsAboutToSave= false;
			}
			
			// If here, the dirty state of the editor will change to "not dirty".
			// Thus, the state changing flag will be reset.
			
			AbstractMarkerAnnotationModel model= (AbstractMarkerAnnotationModel) info.fModel;
			model.updateMarkers(info.fDocument);
			
			if (resource != null)
				info.setModificationStamp(computeModificationStamp(resource));
				
			if (fSavePolicy != null) {
				ICompilationUnit unit= fSavePolicy.postSave(original);
				if (unit != null) {
					IResource r= unit.getResource();
					IMarker[] markers= r.findMarkers(IMarker.MARKER, true, IResource.DEPTH_ZERO);
					if (markers != null && markers.length > 0) {
						for (int i= 0; i < markers.length; i++)
							model.updateMarker(markers[i], info.fDocument, null);
					}
				}
			}
				
			
		} else {
			super.doSaveDocument(monitor, element, document, overwrite);
		}		
	}
		
	/**
	 * Replaces createAnnotionModel of the super class.
	 */
	protected IAnnotationModel createCompilationUnitAnnotationModel(Object element) throws CoreException {
		if ( !(element instanceof IFileEditorInput))
			throw new CoreException(JavaUIStatus.createError(
				IJavaModelStatusConstants.INVALID_RESOURCE_TYPE, "", null)); //$NON-NLS-1$
		
		IFileEditorInput input= (IFileEditorInput) element;
		return new CompilationUnitAnnotationModel(input);
	}
	
	/*
	 * @see org.eclipse.ui.editors.text.StorageDocumentProvider#createEmptyDocument()
	 */
	protected IDocument createEmptyDocument() {
		return new PartiallySynchronizedDocument();
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.AbstractDocumentProvider#doResetDocument(java.lang.Object, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void doResetDocument(Object element, IProgressMonitor monitor) throws CoreException {
		if (element == null)
			return;
			
		ElementInfo elementInfo= getElementInfo(element);		
		if (elementInfo instanceof CompilationUnitInfo) {
			CompilationUnitInfo info= (CompilationUnitInfo) elementInfo;
			
			IDocument document;
			IStatus status= null;
			
			try {
				
				ICompilationUnit original= (ICompilationUnit) info.fCopy.getOriginalElement();
				IResource resource= original.getResource();
				if (resource instanceof IFile) {
					
					IFile file= (IFile) resource;
					
					try {
						refreshFile(file, monitor);
					} catch (CoreException x) {
						handleCoreException(x, JavaEditorMessages.getString("CompilationUnitDocumentProvider.error.resetDocument")); //$NON-NLS-1$
					}
					
					IFileEditorInput input= new FileEditorInput(file);
					document= super.createDocument(input);
					
				} else {
					document= createEmptyDocument();
				}
					
			} catch (CoreException x) {
				document= createEmptyDocument();
				status= x.getStatus();
			}
			
			fireElementContentAboutToBeReplaced(element);
			
			removeUnchangedElementListeners(element, info);
			info.fDocument.set(document.get());
			info.fCanBeSaved= false;
			info.fStatus= status;
			addUnchangedElementListeners(element, info);
			
			fireElementContentReplaced(element);
			fireElementDirtyStateChanged(element, false);
			
		} else {
			super.doResetDocument(element, monitor);
		}
	}
	
	/**
	 * Returns the preference whether handling temporary problems is enabled.
	 */
	protected boolean isHandlingTemporaryProblems() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		return store.getBoolean(HANDLE_TEMPORARY_PROBLEMS);
	} 
	
	/**
	 * Switches the state of problem acceptance according to the value in the preference store.
	 */
	protected void enableHandlingTemporaryProblems() {
		boolean enable= isHandlingTemporaryProblems();
		for (Iterator iter= getConnectedElements(); iter.hasNext();) {
			ElementInfo element= getElementInfo(iter.next());
			if (element instanceof CompilationUnitInfo) {
				CompilationUnitInfo info= (CompilationUnitInfo)element;
				if (info.fModel instanceof IProblemRequestorExtension) {
					IProblemRequestorExtension  extension= (IProblemRequestorExtension) info.fModel;
					extension.setIsActive(enable);
				}
			}
		}
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.ICompilationUnitDocumentProvider#saveDocumentContent(org.eclipse.core.runtime.IProgressMonitor, java.lang.Object, org.eclipse.jface.text.IDocument, boolean)
	 */
	public void saveDocumentContent(IProgressMonitor monitor, Object element, IDocument document, boolean overwrite) throws CoreException {
		
		if (!fIsAboutToSave)
			return;
		
		if (element instanceof IFileEditorInput) {
			IFileEditorInput input= (IFileEditorInput) element;
			try {
				String encoding= getEncoding(element);
				if (encoding == null)
					encoding= ResourcesPlugin.getEncoding();
				InputStream stream= new ByteArrayInputStream(document.get().getBytes(encoding));
				IFile file= input.getFile();
				file.setContents(stream, overwrite, true, monitor);
			} catch (IOException x)  {
				String message= (x != null ? x.getMessage() : ""); //$NON-NLS-1$
				IStatus s= new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.OK, message, x);
				throw new CoreException(s);
			}
		}
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.ICompilationUnitDocumentProvider#shutdown()
	 */
	public void shutdown() {
		
		JavaPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(fPropertyListener);
		
		Iterator e= getConnectedElements();
		while (e.hasNext())
			disconnect(e.next());
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.ICompilationUnitDocumentProvider#addGlobalAnnotationModelListener(org.eclipse.jface.text.source.IAnnotationModelListener)
	 */
	public void addGlobalAnnotationModelListener(IAnnotationModelListener listener) {
		fGlobalAnnotationModelListener.addListener(listener);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.ICompilationUnitDocumentProvider#removeGlobalAnnotationModelListener(org.eclipse.jface.text.source.IAnnotationModelListener)
	 */
	public void removeGlobalAnnotationModelListener(IAnnotationModelListener listener) {
		fGlobalAnnotationModelListener.removeListener(listener);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.ICompilationUnitDocumentProvider#getWorkingCopy(java.lang.Object)
	 */
	public ICompilationUnit getWorkingCopy(Object element) {
		
		ElementInfo elementInfo= getElementInfo(element);		
		if (elementInfo instanceof CompilationUnitInfo) {
			CompilationUnitInfo info= (CompilationUnitInfo) elementInfo;
			return info.fCopy;
		}
		
		return null;
	}

	/*
	 * @see org.eclipse.ui.texteditor.IDocumentProviderExtension3#createEmptyDocument(java.lang.Object)
	 */
	public IDocument createEmptyDocument(Object element) {
		return createEmptyDocument();
	}
	
	/*
	 * @see org.eclipse.ui.editors.text.StorageDocumentProvider#setupDocument(java.lang.Object, org.eclipse.jface.text.IDocument)
	 */
	protected void setupDocument(Object element, IDocument document) {
		if (document != null) {
			JavaTextTools tools= JavaPlugin.getDefault().getJavaTextTools();
			tools.setupJavaDocumentPartitioner(document, IJavaPartitions.JAVA_PARTITIONING);
		}
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.ICompilationUnitDocumentProvider#createLineTracker(java.lang.Object)
	 */
	public ILineTracker createLineTracker(Object element) {
		return new DefaultLineTracker();
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.ICompilationUnitDocumentProvider#setSavePolicy(org.eclipse.jdt.internal.ui.javaeditor.ISavePolicy)
	 */
	public void setSavePolicy(ISavePolicy savePolicy) {
		fSavePolicy= savePolicy;
	}

	/*
	 * Here for visibility reasons.
	 * @see AbstractDocumentProvider#createDocument(Object)
	 */
	public IDocument createDocument(Object element) throws CoreException {
		return super.createDocument(element);
	}
}
