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
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModelEvent;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelListener;
import org.eclipse.jface.text.source.IAnnotationModelListenerExtension;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.ResourceMarkerAnnotationModel;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IBufferFactory;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IOpenable;
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
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;
import org.eclipse.jdt.internal.ui.text.java.IProblemRequestorExtension;


public class CompilationUnitDocumentProvider extends FileDocumentProvider {
		
		/**
		 * Here for visibility issues only.
		 */
		protected class _FileSynchronizer extends FileSynchronizer {
			public _FileSynchronizer(IFileEditorInput fileEditorInput) {
				super(fileEditorInput);
			}
		};
		
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
		};
		
		/**
		 * Annotation representating an <code>IProblem</code>.
		 */
		static protected class ProblemAnnotation extends Annotation implements IJavaAnnotation {
			
			private static Image fgQuickFixImage;
			private static Image fgQuickFixErrorImage;
			private static boolean fgQuickFixImagesInitialized= false;
			
			private List fOverlaids;
			private IProblem fProblem;
			private Image fImage;
			private boolean fQuickFixImagesInitialized= false;
			private AnnotationType fType;
			
			
			public ProblemAnnotation(IProblem problem) {
				
				fProblem= problem;
				setLayer(MarkerAnnotation.PROBLEM_LAYER + 1);
				
				if (IProblem.Task == fProblem.getID())
					fType= AnnotationType.TASK;
				else if (fProblem.isWarning())
					fType= AnnotationType.WARNING;
				else
					fType= AnnotationType.ERROR;			
			}
			
			private void initializeImages() {
				// http://bugs.eclipse.org/bugs/show_bug.cgi?id=18936
				if (!fQuickFixImagesInitialized) {
					if (indicateQuixFixableProblems() && JavaCorrectionProcessor.hasCorrections(fProblem.getID())) {
						if (!fgQuickFixImagesInitialized) {
							fgQuickFixImage= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_FIXABLE_PROBLEM);
							fgQuickFixErrorImage= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_FIXABLE_ERROR);
							fgQuickFixImagesInitialized= true;
						}
						if (fType == AnnotationType.ERROR)
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
			public String getMessage() {
				return fProblem.getMessage();
			}

			/*
			 * @see IJavaAnnotation#isTemporary()
			 */
			public boolean isTemporary() {
				return true;
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
				return isProblem() ? fProblem.getID() : -1;
			}

			/*
			 * @see IJavaAnnotation#isProblem()
			 */
			public boolean isProblem() {
				return  fType == AnnotationType.WARNING || fType == AnnotationType.ERROR;
			}
			
			/*
			 * @see IJavaAnnotation#isRelevant()
			 */
			public boolean isRelevant() {
				return true;
			}
			
			/*
			 * @see IJavaAnnotation#hasOverlay()
			 */
			public boolean hasOverlay() {
				return false;
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
			
			public AnnotationType getAnnotationType() {
				return fType;
			}
		};
		
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
			};
			
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
		};
		
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
			private CompilationUnitAnnotationModelEvent fCurrentEvent;

			public CompilationUnitAnnotationModel(IFileEditorInput input) {
				super(input.getFile());
				fInput= input;
				fCurrentEvent= new CompilationUnitAnnotationModelEvent(this, getResource());
			}
			
			protected MarkerAnnotation createMarkerAnnotation(IMarker marker) {
				return new JavaMarkerAnnotation(marker);
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

				if (markerDeltas != null && markerDeltas.length > 0) {
					try {
						ICompilationUnit workingCopy = getWorkingCopy(fInput);
						if (workingCopy != null)
							workingCopy.reconcile(true, null);
					} catch (JavaModelException ex) {
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
				fPreviouslyOverlaid= fCurrentlyOverlaid;
				fCurrentlyOverlaid= new ArrayList();
				
				synchronized (fAnnotations) {
					
					if (fGeneratedAnnotations.size() > 0) {
						temporaryProblemsChanged= true;	
						removeAnnotations(fGeneratedAnnotations, false, true);
						fGeneratedAnnotations.clear();
					}
					
					if (fCollectedProblems != null && fCollectedProblems.size() > 0) {
												
						Iterator e= fCollectedProblems.iterator();
						while (e.hasNext()) {
							
							IProblem problem= (IProblem) e.next();
							
							if (fProgressMonitor != null && fProgressMonitor.isCanceled()) {
								isCanceled= true;
								break;
							}
								
							Position position= createPositionFromProblem(problem);
							if (position != null) {
								
								ProblemAnnotation annotation= new ProblemAnnotation(problem);
								overlayMarkers(position, annotation);								
								fGeneratedAnnotations.add(annotation);
								addAnnotation(annotation, position, false);
								
								temporaryProblemsChanged= true;
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
			 * @see AnnotationModel#fireModelChanged()
			 */
			protected void fireModelChanged() {
				fireModelChanged(fCurrentEvent);
				fCurrentEvent= new CompilationUnitAnnotationModelEvent(this, getResource());
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
			protected void addAnnotation(Annotation annotation, Position position, boolean fireModelChanged) {
				super.addAnnotation(annotation, position, fireModelChanged);
				
				fCurrentEvent.annotationAdded(annotation);
				
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
				for (Iterator iter= getAnnotationIterator(); iter.hasNext();) {
					fCurrentEvent.annotationRemoved((Annotation) iter.next());
				}
				super.removeAllAnnotations(fireModelChanged);
				fReverseMap.clear();
			}
			
			/*
			 * @see AnnotationModel#removeAnnotation(Annotation, boolean)
			 */
			protected void removeAnnotation(Annotation annotation, boolean fireModelChanged) {
				fCurrentEvent.annotationRemoved(annotation);
				
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
		};
		
		
		/**
		 * Creates <code>IBuffer</code>s based on documents.
		 */
		protected class BufferFactory implements IBufferFactory {
			
			private IDocument internalGetDocument(IFileEditorInput input) throws CoreException {
				IDocument document= getDocument(input);
				if (document != null)
					return document;
				return CompilationUnitDocumentProvider.this.createDocument(input);
			}
			
			public IBuffer createBuffer(IOpenable owner) {
				if (owner instanceof ICompilationUnit) {
					
					ICompilationUnit unit= (ICompilationUnit) owner;
					ICompilationUnit original= (ICompilationUnit) unit.getOriginalElement();
					IResource resource= original.getResource();
					if (resource instanceof IFile) {
						IFileEditorInput providerKey= new FileEditorInput((IFile) resource);
						
						IDocument document= null;
						IStatus status= null;
						
						try {
							document= internalGetDocument(providerKey);
						} catch (CoreException x) {
							status= x.getStatus();
							document= new Document();
							initializeDocument(document);
						}
						
						DocumentAdapter adapter= new DocumentAdapter(unit, document, new DefaultLineTracker(), CompilationUnitDocumentProvider.this, providerKey);
						adapter.setStatus(status);
						return adapter;
					}
						
				}
				return DocumentAdapter.NULL;
			}
		};
		
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
		};
			
		
		/**
		 * Document that can also be used by a background reconciler.
		 */
		protected static class PartiallySynchronizedDocument extends Document {
			
			/*
			 * @see IDocumentExtension#startSequentialRewrite(boolean)
			 */
			synchronized public void startSequentialRewrite(boolean normalized) {
				super.startSequentialRewrite(normalized);
		}
		
			/*
			 * @see IDocumentExtension#stopSequentialRewrite()
			 */
			synchronized public void stopSequentialRewrite() {
				super.stopSequentialRewrite();
			}
			
			/*
			 * @see IDocument#get()
			 */
			synchronized public String get() {
				return super.get();
			}
			
			/*
			 * @see IDocument#get(int, int)
			 */
			synchronized public String get(int offset, int length) throws BadLocationException {
				return super.get(offset, length);
			}
			
			/*
			 * @see IDocument#getChar(int)
			 */
			synchronized public char getChar(int offset) throws BadLocationException {
				return super.getChar(offset);
			}
			
			/*
			 * @see IDocument#replace(int, int, String)
			 */
			synchronized public void replace(int offset, int length, String text) throws BadLocationException {
				super.replace(offset, length, text);
			}
			
			/*
			 * @see IDocument#set(String)
			 */
			synchronized public void set(String text) {
				super.set(text);
			}
		};
		
		
		
	/* Preference key for temporary problems */
	private final static String HANDLE_TEMPORARY_PROBLEMS= PreferenceConstants.EDITOR_EVALUTE_TEMPORARY_PROBLEMS;
	
	
	/** The buffer factory */
	private IBufferFactory fBufferFactory= new BufferFactory();
	/** Indicates whether the save has been initialized by this provider */
	private boolean fIsAboutToSave= false;
	/** The save policy used by this provider */
	private ISavePolicy fSavePolicy;
	/** Internal property changed listener */
	private IPropertyChangeListener fPropertyListener;
	
	/** annotation model listener added to all created CU annotation models */
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
	 * Sets the document provider's save policy.
	 */
	public void setSavePolicy(ISavePolicy savePolicy) {
		fSavePolicy= savePolicy;
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
	
	/**
	 * Creates a line tracker working with the same line delimiters as the document
	 * of the given element. Assumes the element to be managed by this document provider.
	 * 
	 * @param element the element serving as blue print
	 * @return a line tracker based on the same line delimiters as the element's document
	 */
	public ILineTracker createLineTracker(Object element) {
		return new DefaultLineTracker();
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
				ICompilationUnit c= (ICompilationUnit) original.getSharedWorkingCopy(getProgressMonitor(), fBufferFactory, r);
				
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
			cuInfo.fCopy.destroy();
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
			
			// update structure, assumes lock on info.fCopy
			info.fCopy.reconcile();
			
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
	
	protected void initializeDocument(IDocument document) {
		if (document != null) {
			JavaTextTools tools= JavaPlugin.getDefault().getJavaTextTools();
			IDocumentPartitioner partitioner= tools.createDocumentPartitioner();
			document.setDocumentPartitioner(partitioner);
			partitioner.connect(document);
		}
	}
	
	/*
	 * @see AbstractDocumentProvider#createDocument(Object)
	 */
	protected IDocument createDocument(Object element) throws CoreException {
		
		if (element instanceof IEditorInput) {
			Document document= new PartiallySynchronizedDocument();
			if (setDocumentContent(document, (IEditorInput) element, getEncoding(element))) {
		initializeDocument(document);
		return document;
	}
		}
	
		return null;
	}
	
	/*
	 * @see AbstractDocumentProvider#resetDocument(Object)
	 */
	public void resetDocument(Object element) throws CoreException {
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
						refreshFile(file);
					} catch (CoreException x) {
						handleCoreException(x, JavaEditorMessages.getString("CompilationUnitDocumentProvider.error.resetDocument")); //$NON-NLS-1$
					}
					
					IFileEditorInput input= new FileEditorInput(file);
					document= super.createDocument(input);
					
				} else {
					document= new Document();
				}
					
			} catch (CoreException x) {
				document= new Document();
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
			super.resetDocument(element);
		}
	}
	
	/**
	 * Saves the content of the given document to the given element.
	 * This is only performed when this provider initiated the save.
	 * 
	 * @param monitor the progress monitor
	 * @param element the element to which to save
	 * @param document the document to save
	 * @param overwrite <code>true</code> if the save should be enforced
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
				IStatus s= new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.OK, x.getMessage(), x);
				throw new CoreException(s);
			}
		}
	}
	
	/**
	 * Returns the underlying resource for the given element.
	 * 
	 * @param the element
	 * @return the underlying resource of the given element
	 */
	public IResource getUnderlyingResource(Object element) {
		if (element instanceof IFileEditorInput) {
			IFileEditorInput input= (IFileEditorInput) element;
			return input.getFile();
		}
		return null;
	}
	
	/**
	 * Returns the working copy this document provider maintains for the given
	 * element.
	 * 
	 * @param element the given element
	 * @return the working copy for the given element
	 */
	ICompilationUnit getWorkingCopy(IEditorInput element) {
		
		ElementInfo elementInfo= getElementInfo(element);		
		if (elementInfo instanceof CompilationUnitInfo) {
			CompilationUnitInfo info= (CompilationUnitInfo) elementInfo;
			return info.fCopy;
		}
		
		return null;
	}
	
	/**
	 * Gets the BufferFactory.
	 */
	public IBufferFactory getBufferFactory() {
		return fBufferFactory;
	}
	
	/**
	 * Shuts down this document provider.
	 */
	public void shutdown() {
		
		JavaPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(fPropertyListener);
		
		Iterator e= getConnectedElements();
		while (e.hasNext())
			disconnect(e.next());
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
	
	/**
	 * Adds a listener that reports changes from all compilation unit annotation models.
	 */
	public void addGlobalAnnotationModelListener(IAnnotationModelListener listener) {
		fGlobalAnnotationModelListener.addListener(listener);
	}

	/**
	 * Removes the listener.
	 */	
	public void removeGlobalAnnotationModelListener(IAnnotationModelListener listener) {
		fGlobalAnnotationModelListener.removeListener(listener);
	}
	
	/**
	 * Returns whether the given element is connected to this document provider.
	 * 
	 * @param element the element
	 * @return <code>true</code> if the element is connected, <code>false</code> otherwise
	 */
	boolean isConnected(Object element) {
		return getElementInfo(element) != null;	
	}
}
