package org.eclipse.jdt.internal.ui.javaeditor;


/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */



import java.util.Map;
import java.util.ResourceBundle;


import org.eclipse.core.resources.IResource;


import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.AddMarkerAction;
import org.eclipse.ui.texteditor.ITextEditor;


import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;


import org.eclipse.jdt.internal.ui.IResourceLocator;



class AddClassFileMarkerAction extends AddMarkerAction {
	
	
	/**
	 * Creates a marker action.
	 */
	public AddClassFileMarkerAction(String prefix, ITextEditor textEditor, String markerType, boolean askForLabel) {
		super(JavaEditorMessages.getResourceBundle(), prefix, textEditor, markerType, askForLabel);
	}
	
	/**
	 * @see AddMarkerAction#getResource()
	 */
	protected IResource getResource() {
		
		IResource resource= null;
		
		IEditorInput input= getTextEditor().getEditorInput();
		if (input instanceof IClassFileEditorInput) {
			IClassFile c= ((IClassFileEditorInput) input).getClassFile();
			IResourceLocator locator= (IResourceLocator) c.getAdapter(IResourceLocator.class);
			if (locator != null) {
				try {
					resource= locator.getContainingResource(c);
				} catch (JavaModelException x) {
					// ignore but should inform
				}
			}
		}
		
		return resource;
	}
	
	/**
	 * @see AddMarkerAction#getInitialAttributes()
	 */
	protected Map getInitialAttributes() {
		
		Map attributes= super.getInitialAttributes();
		
		IEditorInput input= getTextEditor().getEditorInput();
		if (input instanceof IClassFileEditorInput) {
			
			IClassFile classFile= ((IClassFileEditorInput) input).getClassFile();
			JavaCore.addJavaElementMarkerAttributes(attributes, classFile);
		}
		
		return attributes;
	}
}