package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.util.Iterator;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.util.Assert;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.ResourceMarkerAnnotationModel;

import org.eclipse.jdt.core.BufferChangedEvent;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IBufferChangedListener;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.core.JavaModelStatus;
import org.eclipse.jdt.internal.ui.JavaPlugin;



public class CompilationUnitDocumentProvider extends FileDocumentProvider implements IWorkingCopyManager {
	
		/**
		 * Synchronizes the buffer of a working copy with the document representing the buffer content.
		 * It would be more appropriate if the document could also serve as the working copy's buffer.
		 * Listens to buffer changes and translates those into document changes. Also listens to document
		 * changes and translates those into buffer changes, respectively.
		 */
		protected static class BufferSynchronizer implements IDocumentListener, IBufferChangedListener {
			
			protected IDocument fDocument;
			protected IBuffer fBuffer;
			
			public BufferSynchronizer(IDocument document, ICompilationUnit unit) {
				Assert.isNotNull(document);
				Assert.isNotNull(unit);
				fDocument= document;
				try {
					fBuffer= unit.getBuffer();
				} catch (JavaModelException x) {
					Assert.isNotNull(fBuffer);
				}
			}
			
			public void install() {
				fDocument.addDocumentListener(this);
				fBuffer.addBufferChangedListener(this);
			}
			
			public void uninstall() {
				fDocument.removeDocumentListener(this);
				fBuffer.removeBufferChangedListener(this);
			}
			
			/**
			 * @see IDocumentListener#documentChanged
			 */
			public void documentChanged(DocumentEvent event) {
				fBuffer.removeBufferChangedListener(this);
				fBuffer.replace(event.getOffset(), event.getLength(), event.getText());
				fBuffer.addBufferChangedListener(this);
			}
			
			/**
			 * @see IDocumentListener#documentAboutToBeChanged
			 */
			public void documentAboutToBeChanged(DocumentEvent event) {
			}
			
			/**
			 * @see IBufferChangedListener#bufferChanged
			 */
			public void bufferChanged(BufferChangedEvent event) {
				fDocument.removeDocumentListener(this);
				try {
					if (event.getLength() > 0 || event.getText() != null)
						fDocument.replace(event.getOffset(), event.getLength(), event.getText());
				} catch (BadLocationException x) {
					Assert.isTrue(false, "Buffer and Document are out of sync");
				} finally {
					fDocument.addDocumentListener(this);
				}
			}	
		};
		
		
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
			BufferSynchronizer fBufferSynchronizer;
			
			CompilationUnitInfo(IDocument document, IAnnotationModel model, _FileSynchronizer fileSynchronizer, ICompilationUnit copy, BufferSynchronizer bufferSynchronizer) {
				super(document, model, fileSynchronizer);
				fCopy= copy;
				fBufferSynchronizer= bufferSynchronizer;
			}
			
			void setModificationStamp(long timeStamp) {
				fModificationStamp= timeStamp;
			}
		};
		
		protected class CompilationUnitMarkerAnnotationModel extends ResourceMarkerAnnotationModel {
			
			public CompilationUnitMarkerAnnotationModel(IResource resource) {
				super(resource);
			}
			
			protected MarkerAnnotation createMarkerAnnotation(IMarker marker) {
				return new JavaMarkerAnnotation(marker);
			}
		};
		
	/**
	 * Constructor
	 */
	public CompilationUnitDocumentProvider() {
	}
	
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
			throw new CoreException(new JavaModelStatus(IJavaModelStatusConstants.INVALID_RESOURCE_TYPE));
			
		IFileEditorInput input= (IFileEditorInput) element;
		ICompilationUnit original= createCompilationUnit(input.getFile());
		if (original != null) {
				
			try {
				
				try {
					input.getFile().refreshLocal(IResource.DEPTH_INFINITE, null);
				} catch (CoreException x) {
					handleCoreException(x, "CompilationUnitDocumentProvider.createElementInfo");
				}
				
				ICompilationUnit c= (ICompilationUnit) original.getWorkingCopy();
				IDocument d= createCompilationUnitDocument(c);
				IAnnotationModel m= createCompilationUnitAnnotationModel(element);
				_FileSynchronizer f= new _FileSynchronizer(input);
				f.install();
				BufferSynchronizer b= new BufferSynchronizer(d, c);
				b.install();
				
				CompilationUnitInfo info= new CompilationUnitInfo(d, m, f, c, b);
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
			
			if (cuInfo.fBufferSynchronizer != null)
				cuInfo.fBufferSynchronizer.uninstall();
			
			cuInfo.fCopy.destroy();
		}
		
		super.disposeElementInfo(element, info);
	}
		
	/**
	 * @see AbstractDocumentProvider#aboutToChange(Object)
	 */
	public void aboutToChange(Object element) {
		
		ElementInfo elementInfo= getElementInfo(element);		
		if (elementInfo instanceof CompilationUnitInfo) {
			CompilationUnitInfo info= (CompilationUnitInfo) elementInfo;
			
			if (info.fBufferSynchronizer != null)
				info.fBufferSynchronizer.uninstall();
		}
		
		super.aboutToChange(element);
	}

	/**
	 * @see AbstractDocumentProvider#changed(Object)
	 */
	public void changed(Object element) {
		
		ElementInfo elementInfo= getElementInfo(element);		
		if (elementInfo instanceof CompilationUnitInfo) {
			CompilationUnitInfo info= (CompilationUnitInfo) elementInfo;
			if (info.fBufferSynchronizer != null) 
				info.fBufferSynchronizer.install();
		}
		
		super.changed(element);
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
				
				// commit working copy
				info.fCopy.commit(overwrite, monitor);
				
				AbstractMarkerAnnotationModel model= (AbstractMarkerAnnotationModel) info.fModel;
				model.updateMarkers(info.fDocument);
				
				if (resource != null)
					info.setModificationStamp(computeModificationStamp(resource));
										
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
	 * Replaces createDocument of the super class.
	 */
	protected IDocument createCompilationUnitDocument(ICompilationUnit unit) throws CoreException {
		
		String contents= "";
		
		try {
			contents= unit.getSource();
		} catch (JavaModelException x) {
			throw new CoreException(x.getStatus());
		}
		
		Document document= new Document(contents);		
		
		JavaTextTools tools= JavaPlugin.getDefault().getJavaTextTools();
		IDocumentPartitioner partitioner= tools.createDocumentPartitioner();
		partitioner.connect(document);
		document.setDocumentPartitioner(partitioner);
		
		return document;
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
}