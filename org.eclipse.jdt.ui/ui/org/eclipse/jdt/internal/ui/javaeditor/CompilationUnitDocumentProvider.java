package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

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

import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.core.JavaModelStatus;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.JavaStatusConstants;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.preferences.WorkInProgressPreferencePage;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;
import org.eclipse.jdt.internal.ui.text.java.IProblemRequestorExtension;




public class CompilationUnitDocumentProvider extends FileDocumentProvider implements IWorkingCopyManager {
		
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
			
			CompilationUnitInfo(IDocument document, IAnnotationModel model, _FileSynchronizer fileSynchronizer, ICompilationUnit copy) {
				super(document, model, fileSynchronizer);
				fCopy= copy;
			}
			
			void setModificationStamp(long timeStamp) {
				fModificationStamp= timeStamp;
			}
		};
		
		/**
		 * Annotation representating an <code>IProblem</code>.
		 */
		static protected class ProblemAnnotation extends Annotation implements IProblemAnnotation {
			
			private IProblem fProblem;
			private Image fImage;
			private static Image fgImage;
			
			public ProblemAnnotation(IProblem problem) {
				fProblem= problem;
				setLayer(1);
				
				if (fgImage == null)
					fgImage= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_FIXABLE_PROBLEM);
				
				if (WorkInProgressPreferencePage.showTempProblems() && JavaCorrectionProcessor.hasCorrections(fProblem.getID()))
					fImage= fgImage;
			}
						
			/*
			 * @see Annotation#paint
			 */
			public void paint(GC gc, Canvas canvas, Rectangle r) {
				if (fImage != null)
					drawImage(fImage, gc, canvas, r, SWT.CENTER, SWT.TOP);
			}				

			/*
			 * @see IProblemAnnotation#getMessage()
			 */
			public String getMessage() {
				return fProblem.getMessage();
			}

			/*
			 * @see IProblemAnnotation#isTemporaryProblem()
			 */
			public boolean isTemporaryProblem() {
				return true;
			}

			/*
			 * @see IProblemAnnotation#isWarning()
			 */
			public boolean isWarning() {
				return fProblem.isWarning();
			}

			/*
			 * @see IProblemAnnotation#isError()
			 */
			public boolean isError() {
				return fProblem.isError();
			}
			
			/*
			 * @see IProblemAnnotation#getArguments()
			 */
			public String[] getArguments() {
				return fProblem.getArguments();
			}

			/*
			 * @see IProblemAnnotation#getId()
			 */
			public int getId() {
				return fProblem.getID();
			}

			/*
			 * @see IProblemAnnotation#isProblem()
			 */
			public boolean isProblem() {
				return true;
			}
		};
		
		/**
		 * Annotation model dealing with java marker annotations and temporary problems.
		 * Also acts as problem requestor for its compilation unit. Initialiy inactive. Must explicitly be
		 * activated.
		 */
		protected class CompilationUnitAnnotationModel extends ResourceMarkerAnnotationModel implements IProblemRequestor, IProblemRequestorExtension {
			
			private List fCollectedProblems;
			private List fGeneratedAnnotations;
			private IProgressMonitor fProgressMonitor;
			private boolean fIsActive= false;
			
			
			public CompilationUnitAnnotationModel(IResource resource) {
				super(resource);
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
			
			/*
			 * @see IProblemRequestor#beginReporting()
			 */
			public void beginReporting() {
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
					
				boolean temporaryProblemsChanged= false;
				
				synchronized (fAnnotations) {
					
					if (fGeneratedAnnotations.size() > 0) {
						temporaryProblemsChanged= true;
						
						removeAnnotations(fGeneratedAnnotations, false, true);
						
						fGeneratedAnnotations.clear();
					}

					if (fCollectedProblems.size() > 0) {
						temporaryProblemsChanged= true;
						
						Iterator e= fCollectedProblems.iterator();
						while (e.hasNext()) {
							IProblem problem= (IProblem) e.next();
							ProblemAnnotation annotation= new ProblemAnnotation(problem);
							fGeneratedAnnotations.add(annotation);
							Position p= createPositionFromProblem(problem);
							if (p != null)
								addAnnotation(annotation, p, false);
						}
						
						fCollectedProblems.clear();
					}
				}
					
				if (temporaryProblemsChanged)
					fireModelChanged(new CompilationUnitAnnotationModelEvent(this, false));
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
				fireModelChanged(new CompilationUnitAnnotationModelEvent(this, true));
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
					
					try {
						
						ICompilationUnit original= (ICompilationUnit) unit.getOriginalElement();
						IResource resource= original.getCorrespondingResource();
						if (resource instanceof IFile) {
							IFileEditorInput providerKey= new FileEditorInput((IFile) resource);
							IDocument document= internalGetDocument(providerKey);
							return new DocumentAdapter(unit, document, new DefaultLineTracker(), CompilationUnitDocumentProvider.this, providerKey);
						}
						
					} catch (CoreException x) {
						handleCoreException(x, JavaUIMessages.getString("CompilationUnitDocumentProvider.problemsCreatingBuffer")); //$NON-NLS-1$
					}
				}
				return null;
			}
		};
		
	/* Preference key for temporary problems */
	public final static String HANDLE_TEMPRARY_PROBELMS= "handleTemporaryProblems"; //$NON-NLS-1$
	
	
	/** The buffer factory */
	private IBufferFactory fBufferFactory= new BufferFactory();
	/** Indicates whether the save has been initialized by this provider */
	private boolean fIsAboutToSave= false;
	/** The save policy used by this provider */
	private ISavePolicy fSavePolicy;
	/** Internal property changed listener */
	private IPropertyChangeListener fPropertyListener;
	
		
	
	/**
	 * Constructor
	 */
	public CompilationUnitDocumentProvider() {
		fPropertyListener= new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (HANDLE_TEMPRARY_PROBELMS.equals(event.getProperty()))
					enableHandlingTemporaryProblems();
			}
		};
		
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
				
				IProgressMonitor monitor= new NullProgressMonitor();
				
				try {
					input.getFile().refreshLocal(IResource.DEPTH_INFINITE, monitor);
				} catch (CoreException x) {
					handleCoreException(x, JavaEditorMessages.getString("CompilationUnitDocumentProvider.error.createElementInfo")); //$NON-NLS-1$
				}
				
				IAnnotationModel m= createCompilationUnitAnnotationModel(input);
				IProblemRequestor r= m instanceof IProblemRequestor ? (IProblemRequestor) m : null;
				ICompilationUnit c= (ICompilationUnit) original.getSharedWorkingCopy(monitor, fBufferFactory, r);
				
				DocumentAdapter a= null;
				try {
					a= (DocumentAdapter)c.getBuffer();
				} catch (ClassCastException cce) {
					IStatus status= new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, JavaStatusConstants.TEMPLATE_IO_EXCEPTION, "Shared working copy has wrong buffer", cce); //$NON-NLS-1$
					throw new CoreException(status);
				}
				
				_FileSynchronizer f= new _FileSynchronizer(input); 
				f.install();
				
				CompilationUnitInfo info= new CompilationUnitInfo(a.getDocument(), m, f, c);
				info.setModificationStamp(computeModificationStamp(input.getFile()));
				
				if (r instanceof IProblemRequestorExtension) {
					IProblemRequestorExtension extension= (IProblemRequestorExtension) r;
					extension.setIsActive(isHandlingTemporaryProblems());
				}
				
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
			IResource resource= null;
			try {
				resource= original.getUnderlyingResource();
			} catch (JavaModelException x) {
				// workaround for http://bugs.eclipse.org/bugs/show_bug.cgi?id=14940
			}
			
			if (resource == null) {
				// underlying resource has been deleted, just recreate file, ignore the rest
				super.doSaveDocument(monitor, element, document, overwrite);
				return;
			}
			
			if (resource != null && !overwrite)
				checkSynchronizationState(info.fModificationStamp, resource);
				
			if (fSavePolicy != null)
				fSavePolicy.preSave(info.fCopy);
				
			try {
				fIsAboutToSave= true;
				// commit working copy
				info.fCopy.commit(overwrite, monitor);
			} finally {
				fIsAboutToSave= false;
			}
			
			AbstractMarkerAnnotationModel model= (AbstractMarkerAnnotationModel) info.fModel;
			model.updateMarkers(info.fDocument);
			
			if (resource != null)
				info.setModificationStamp(computeModificationStamp(resource));
				
			if (fSavePolicy != null) {
				ICompilationUnit unit= fSavePolicy.postSave(original);
				if (unit != null) {
					IResource r= unit.getUnderlyingResource();
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
			throw new CoreException(new JavaModelStatus(IJavaModelStatusConstants.INVALID_RESOURCE_TYPE));
		
		IFileEditorInput input= (IFileEditorInput) element;
		return new CompilationUnitAnnotationModel(input.getFile());
	}
	
	/*
	 * @see AbstractDocumentProvider#createDocument(Object)
	 */
	protected IDocument createDocument(Object element) throws CoreException {
		IDocument document= super.createDocument(element);
		if (document != null) {
			JavaTextTools tools= JavaPlugin.getDefault().getJavaTextTools();
			IDocumentPartitioner partitioner= tools.createDocumentPartitioner();
			document.setDocumentPartitioner(partitioner);
			partitioner.connect(document);
		}
		
		return document;
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
			if (info.fCanBeSaved) {
				try {
					
					ICompilationUnit original= (ICompilationUnit) info.fCopy.getOriginalElement();
					
					fireElementContentAboutToBeReplaced(element);
					
					removeUnchangedElementListeners(element, info);
					info.fDocument.set(original.getSource());
					info.fCanBeSaved= false;
					addUnchangedElementListeners(element, info);
					
					fireElementContentReplaced(element);
					
				} catch (JavaModelException x) {
					throw new CoreException(new JavaModelStatus(IJavaModelStatusConstants.INVALID_RESOURCE, x));
				}
			}
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
			InputStream stream= new ByteArrayInputStream(document.get().getBytes());
			
			IFile file= input.getFile();
			file.setContents(stream, overwrite, true, monitor);
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
	
	/*
	 * @see IWorkingCopyManager#connect(IEditorInput)
	 */
	public void connect(IEditorInput input) throws CoreException {
		super.connect(input);
	}
	
	/*
	 * @see IWorkingCopyManager#disconnect(IEditorInput)
	 */
	public void disconnect(IEditorInput input) {
		super.disconnect(input);
	}
	
	/*
	 * @see IWorkingCopyManager#getWorkingCopy(Object)
	 */
	public ICompilationUnit getWorkingCopy(IEditorInput element) {
		
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
	
	public void shutdown() {
		
		JavaPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(fPropertyListener);
		
		Iterator e= getConnectedElements();
		while (e.hasNext())
			disconnect(e.next());
	}

	/**
	 * Returns all working copies manages by this document provider.
	 * 
	 * @return all managed working copies 
	 */
	public ICompilationUnit[] getAllWorkingCopies() {
		List result= new ArrayList(10);
		for (Iterator iter= getConnectedElements(); iter.hasNext();) {
			ElementInfo element= getElementInfo(iter.next());
			if (element instanceof CompilationUnitInfo) {
				CompilationUnitInfo info= (CompilationUnitInfo)element;
				result.add(info.fCopy);
			}
		}
		return (ICompilationUnit[]) result.toArray(new ICompilationUnit[result.size()]);
	}
	
	/**
	 * Returns the preference whether handling temporary problems is enabled.
	 */
	protected boolean isHandlingTemporaryProblems() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		return store.getBoolean(HANDLE_TEMPRARY_PROBELMS);
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
}