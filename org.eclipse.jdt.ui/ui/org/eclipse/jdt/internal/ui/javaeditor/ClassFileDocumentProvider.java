package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.FileDocumentProvider;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.IResourceLocator;
import org.eclipse.jdt.internal.ui.JavaPlugin;



public class ClassFileDocumentProvider extends FileDocumentProvider {
		
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
		
		/**
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
					handleDelete(fInput);
					return true;
				}
			}
			
			if (((delta.getFlags() & IJavaElementDelta.F_REMOVED_FROM_CLASSPATH) != 0) && input.equals(element)) {
				handleDelete(fInput);
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
		
	protected IAnnotationModel createClassFileAnnotationModel(IClassFile classFile) throws CoreException {
		IResource resource= null;
		try {
			IResourceLocator locator= (IResourceLocator) classFile.getAdapter(IResourceLocator.class);
			if (locator != null)
				resource= locator.getContainingResource(classFile);
		} catch (JavaModelException ex) {
			// resource will be null
		}
		
		return new ClassFileMarkerAnnotationModel(resource, classFile);
	}
	
	protected IDocument createClassFileDocument(IClassFile classFile) throws CoreException {
		try {
			
			Document document= new Document(classFile.getSource());
			JavaTextTools tools= JavaPlugin.getDefault().getJavaTextTools();
			IDocumentPartitioner partitioner= tools.createDocumentPartitioner();
			document.setDocumentPartitioner(partitioner);
			partitioner.connect(document);
			
			return document;
			
		} catch (JavaModelException x) {
			throw new CoreException(x.getStatus());
		}
	}
	
	/**
	 * @see AbstractDocumentProvider#createElementInfo(Object)
	 */
	protected ElementInfo createElementInfo(Object element) throws CoreException {
		
		if (element instanceof IClassFileEditorInput) {
			IClassFileEditorInput input = (IClassFileEditorInput) element;
			IClassFile c= input.getClassFile();
			IDocument d= createClassFileDocument(c);
			IAnnotationModel m= createClassFileAnnotationModel(c);
			
			if (input instanceof InternalClassFileEditorInput) {
				ClassFileSynchronizer s= new ClassFileSynchronizer(input);
				s.install();
				return new ClassFileInfo(d, m, s);			
			} else if (element instanceof ExternalClassFileEditorInput) {
				_FileSynchronizer s= new _FileSynchronizer((ExternalClassFileEditorInput) input);
				s.install();
				return new ClassFileInfo(d, m, s);
			}
		}
		
		return null;
	}
	
	/**
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
	
	/**
	 * @see AbstractDocumentProvider#doSaveDocument(IProgressMonitor, Object, IDocument)
	 */
	protected void doSaveDocument(IProgressMonitor monitor, Object element, IDocument document) throws CoreException {
	}
	
	protected void handleDelete(IClassFileEditorInput input) {
		fireElementDeleted(input);
	}
}