package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.FileDocumentProvider;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IBufferFactory;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.IResourceLocator;
import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * A document provider for class files. Class files can be either inside 
 */
public class _ClassFileDocumentProvider extends FileDocumentProvider {
		
		/**
		 * Synchronizes the document with external resource changes.
		 */
		protected class ClassFileSynchronizer implements IElementChangedListener {
			
			protected IClassFileEditorInput fInput;
			protected IPackageFragmentRoot fPackageFragmentRoot;
			
			/**
			 * Default constructor.
			 */
			public ClassFileSynchronizer(IClassFileEditorInput input) {
				
				fInput= input;
				
				IJavaElement parent= fInput.getClassFile().getParent();
				while (parent != null && !(parent instanceof IPackageFragmentRoot)) {
					parent= parent.getParent();
				}
				fPackageFragmentRoot= (IPackageFragmentRoot) parent;
			}
			
			/**
			 * Installs the synchronizer.
			 */
			public void install() {
				JavaCore.addElementChangedListener(this);
			}
			
			/**
			 * Uninstalls the synchronizer.
			 */
			public void uninstall() {
				JavaCore.removeElementChangedListener(this);
			}		
			
			/*
			 * @see IElementChangedListener#elementChanged
			 */
			public void elementChanged(ElementChangedEvent e) {
				check(fPackageFragmentRoot, e.getDelta());
			}
				
			/**
			 * Recursively check whether the class file has been deleted. 
			 * Returns true if delta processing can be stopped.
			 */
			protected boolean check(IPackageFragmentRoot input, IJavaElementDelta delta) {
				IJavaElement element= delta.getElement(); 
				
				if ((delta.getKind() & IJavaElementDelta.REMOVED) != 0 || (delta.getFlags() & IJavaElementDelta.F_CLOSED) != 0) { 
					if (element.equals(input.getJavaProject())) {
						handleDeleted(fInput);
						return true;
					}
				}
				
				if (((delta.getFlags() & IJavaElementDelta.F_REMOVED_FROM_CLASSPATH) != 0) && input.equals(element)) {
					handleDeleted(fInput);
					return true;
				}
				
				IJavaElementDelta[] subdeltas= delta.getAffectedChildren();
				for (int i= 0; i < subdeltas.length; i++) {
					if (check(input, subdeltas[i]))
						return true;
				}
				
				return false;
			}
		};
		
		/**
		 * Correcting the visibility of <code>FileSynchronizer</code>.
		 */
		protected class _FileSynchronizer extends FileSynchronizer {
			public _FileSynchronizer(IFileEditorInput fileEditorInput) {
				super(fileEditorInput);
			}
		};
		
		/**
		 * Bundle of all required informations. 
		 */
		protected class ClassFileInfo extends FileInfo {
			
			ClassFileSynchronizer fClassFileSynchronizer= null;
			
			ClassFileInfo(IDocument document, IAnnotationModel model, _FileSynchronizer fileSynchronizer) {
				super(document, model, fileSynchronizer);
			}
			
			ClassFileInfo(IDocument document, IAnnotationModel model, ClassFileSynchronizer classFileSynchronizer) {
				super(document, model, null);
				fClassFileSynchronizer= classFileSynchronizer;
			}
		};
		
		/**
		 * Creates <code>IBuffer</code>s based on documents.
		 */
		protected class BufferFactory implements IBufferFactory {
						
			private IDocument internalCreateDocument(IEditorInput input)  throws CoreException {
				
				IDocument document= createDocument(input);
				
				JavaTextTools tools= JavaPlugin.getDefault().getJavaTextTools();
				IDocumentPartitioner partitioner= tools.createDocumentPartitioner();
				partitioner.connect(document);
				document.setDocumentPartitioner(partitioner);
				
				return document;
			}
			
			private IDocument internalGetDocument(IEditorInput input) throws CoreException {
				IDocument document= getDocument(input);
				if (document != null)
					return document;
					
				return internalCreateDocument(input);
			}
							
			public IBuffer createBuffer(IOpenable owner) {
				if (owner instanceof IClassFile) {
					
					IClassFile classFile= (IClassFile) owner;
					
					try {
						
						IClassFileEditorInput input= null;
						
						IResource resource= classFile.getCorrespondingResource();
						if (resource instanceof IFile)
							input= new ExternalClassFileEditorInput((IFile) resource);
						else
							input= new InternalClassFileEditorInput(classFile);
							
						IDocument document= internalGetDocument(input);
						return new DocumentAdapter(classFile, document, null, null);
						
					} catch (CoreException x) {
						handleCoreException(x, "Problems creating buffer");
					}
				}
				return null;
			}
		};
	
	
	
	/** The buffer factory */
	private IBufferFactory fBufferFactory= new BufferFactory();
	
	
	/**
	 * Creates a new document provider.
	 */
	public _ClassFileDocumentProvider() {
		super();
	}
	
	/*
	 * @see StorageDocumentProvider#setDocumentContent(IDocument, IEditorInput)
	 */
	protected boolean setDocumentContent(IDocument document, IEditorInput editorInput) throws CoreException {
		if (editorInput instanceof IClassFileEditorInput) {
			IClassFile classFile= ((IClassFileEditorInput) editorInput).getClassFile();
			document.set(classFile.getSource());
			return true;
		}
		return super.setDocumentContent(document, editorInput);
	}
	
	/**
	 * Creates an annotation model derrived from the given class file editor input.
	 * @param the editor input from which to query the annotations
	 * @return the created annotation model
	 * @exception CoreException if the editor input could not be accessed
	 */
	protected IAnnotationModel createClassFileAnnotationModel(IClassFileEditorInput classFileEditorInput) throws CoreException {
		IResource resource= null;
		IClassFile classFile= classFileEditorInput.getClassFile();
		
		IResourceLocator locator= (IResourceLocator) classFile.getAdapter(IResourceLocator.class);
		if (locator != null)
			resource= locator.getContainingResource(classFile);
		
		if (resource != null) {
			ClassFileMarkerAnnotationModel model= new ClassFileMarkerAnnotationModel(resource);
			model.setClassFile(classFile);
			return model;
		}
		
		return null;
	}
	
	/*
	 * @see AbstractDocumentProvider#createElementInfo(Object)
	 */
	protected ElementInfo createElementInfo(Object element) throws CoreException {
		
		if (element instanceof IClassFileEditorInput) {
			
			IProgressMonitor monitor= new NullProgressMonitor();
			
			IClassFileEditorInput input = (IClassFileEditorInput) element;
			ExternalClassFileEditorInput external= null;
			if (input instanceof ExternalClassFileEditorInput)
				external= (ExternalClassFileEditorInput) input;
				
			if (external != null) {				
				try {
					external.getFile().refreshLocal(IResource.DEPTH_INFINITE, monitor);
				} catch (CoreException x) {
					handleCoreException(x, JavaEditorMessages.getString("ClassFileDocumentProvider.error.createElementInfo")); //$NON-NLS-1$
				}
			}
			
			IClassFile c= (IClassFile) input.getClassFile().getWorkingCopy(monitor, fBufferFactory);
			DocumentAdapter a= (DocumentAdapter) c.getBuffer();
			IAnnotationModel m= createClassFileAnnotationModel(input);
			
			if (external != null) {
				ClassFileInfo info= new ClassFileInfo(a.getDocument(), m,  (_FileSynchronizer) null);
				info.fModificationStamp= computeModificationStamp(external.getFile());
				return info;
			} else if (input instanceof InternalClassFileEditorInput) {
				ClassFileSynchronizer s= new ClassFileSynchronizer(input);
				s.install();
				return new ClassFileInfo(a.getDocument(), m, s);			
			}
		}
		
		return null;
	}
	
	/*
	 * @see FileDocumentProvider#disposeElementInfo(Object, ElementInfo)
	 */
	protected void disposeElementInfo(Object element, ElementInfo info) {
		ClassFileInfo classFileInfo= (ClassFileInfo) info;
		if (classFileInfo.fClassFileSynchronizer != null) {
			classFileInfo.fClassFileSynchronizer.uninstall();
			classFileInfo.fClassFileSynchronizer= null;
		}
		
		super.disposeElementInfo(element, info);
	}	
	
	/*
	 * @see AbstractDocumentProvider#doSaveDocument(IProgressMonitor, Object, IDocument)
	 */
	protected void doSaveDocument(IProgressMonitor monitor, Object element, IDocument document) throws CoreException {
	}
	
	/**
	 * Handles the deletion of the element underlying the given class file editor input.
	 * @param input the editor input
	 */
	protected void handleDeleted(IClassFileEditorInput input) {
		fireElementDeleted(input);
	}
}