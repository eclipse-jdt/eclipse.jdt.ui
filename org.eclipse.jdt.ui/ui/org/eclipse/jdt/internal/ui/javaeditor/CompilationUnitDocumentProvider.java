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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.source.IAnnotationModel;

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
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.core.JavaModelStatus;
import org.eclipse.jdt.internal.ui.JavaPlugin;



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
		 * Annotation model dealing with java marker annotations.
		 */
		protected class CompilationUnitMarkerAnnotationModel extends ResourceMarkerAnnotationModel {
			
			public CompilationUnitMarkerAnnotationModel(IResource resource) {
				super(resource);
			}
			
			protected MarkerAnnotation createMarkerAnnotation(IMarker marker) {
				return new JavaMarkerAnnotation(marker);
			}
		};
		
		/**
		 * Creates <code>IBuffer</code>s based on documents.
		 */
		protected class BufferFactory implements IBufferFactory {
						
			private IDocument internalCreateDocument(IFileEditorInput input)  throws CoreException {
				
				IDocument document= createDocument(input);
				
				JavaTextTools tools= JavaPlugin.getDefault().getJavaTextTools();
				IDocumentPartitioner partitioner= tools.createDocumentPartitioner();
				partitioner.connect(document);
				document.setDocumentPartitioner(partitioner);
				
				return document;
			}
			
			private IDocument internalGetDocument(IFileEditorInput input) throws CoreException {
				IDocument document= getDocument(input);
				if (document != null)
					return document;
					
				return internalCreateDocument(input);
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
							return new DocumentAdapter(unit, document, CompilationUnitDocumentProvider.this, providerKey);
						}
						
					} catch (CoreException x) {
						handleCoreException(x, "Problems creating buffer");
					}
				}
				return null;
			}
		};
		
	
	/** The buffer factory */
	private IBufferFactory fBufferFactory= new BufferFactory();
	/** Indicates whether the save has been initialized by this provider */
	private boolean fIsAboutToSave= false;
	/** The save policy used by this provider */
	private ISavePolicy fSavePolicy;
	
		
	
	/**
	 * Constructor
	 */
	public CompilationUnitDocumentProvider() {
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
				
				ICompilationUnit c= (ICompilationUnit) original.getWorkingCopy(monitor, fBufferFactory);
				DocumentAdapter a= (DocumentAdapter) c.getBuffer();
				IAnnotationModel m= createCompilationUnitAnnotationModel(input);
				_FileSynchronizer f= new _FileSynchronizer(input); 
				f.install();
				
				CompilationUnitInfo info= new CompilationUnitInfo(a.getDocument(), m, f, c);
				info.setModificationStamp(computeModificationStamp(input.getFile()));
				return info;
				
			} catch (JavaModelException x) {
				throw new CoreException(x.getStatus());
			}
		} else {		
			return super.createElementInfo(element);
		}
	}
	
	/**
	 * @see AbstractDocumentProvider#disposeElementInfo(Object, ElementInfo)
	 */
	protected void disposeElementInfo(Object element, ElementInfo info) {
		
		if (info instanceof CompilationUnitInfo) {
			CompilationUnitInfo cuInfo= (CompilationUnitInfo) info;
			cuInfo.fCopy.destroy();
		}
		
		super.disposeElementInfo(element, info);
	}
	
	/**
	 * @see AbstractDocumentProvider#doSaveDocument(IProgressMonitor, Object, IDocument, boolean)
	 */
	protected void doSaveDocument(IProgressMonitor monitor, Object element, IDocument document, boolean overwrite) throws CoreException {
				
		ElementInfo elementInfo= getElementInfo(element);		
		if (elementInfo instanceof CompilationUnitInfo) {
			CompilationUnitInfo info= (CompilationUnitInfo) elementInfo;
			
			try {					
				// update structure, assumes lock on info.fCopy
				info.fCopy.reconcile();
				
				ICompilationUnit original= (ICompilationUnit) info.fCopy.getOriginalElement();
				IResource resource= original.getUnderlyingResource();
				
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
					
			} catch (JavaModelException x) {
				throw new CoreException(x.getStatus());
			}
			
		} else {
			super.doSaveDocument(monitor, element, document, overwrite);
		}		
	}
		
	/**
	 * Replaces createAnnotionModel of the super class
	 */
	protected IAnnotationModel createCompilationUnitAnnotationModel(Object element) throws CoreException {
		if ( !(element instanceof IFileEditorInput))
			throw new CoreException(new JavaModelStatus(IJavaModelStatusConstants.INVALID_RESOURCE_TYPE));
		
		IFileEditorInput input= (IFileEditorInput) element;
		return new CompilationUnitMarkerAnnotationModel(input.getFile());
	}
	
	/**
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
	
	/**
	 * @see IWorkingCopyManager#connect(IEditorInput)
	 */
	public void connect(IEditorInput input) throws CoreException {
		super.connect(input);
	}
	
	/**
	 * @see IWorkingCopyManager#disconnect(IEditorInput)
	 */
	public void disconnect(IEditorInput input) {
		super.disconnect(input);
	}
	
	/**
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
	 * @see IWorkingCopyManager#shutdown
	 */
	public void shutdown() {
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
}