/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;


/**
 * Provides the means to open a Java element in the specified editor. Subclasses must overwrite
 * IAction.actionPerformed and knit the methods together.
 */
public abstract class OpenJavaElementAction extends JavaUIAction {

	/**
	 * @deprecated	Use OpenJavaElementAction(String, ImageDescriptor) or OpenJavaElementAction(String) instead
	 */
	public OpenJavaElementAction(ResourceBundle bundle, String prefix) {
		super(bundle, prefix);
	}

	/**
	 * Creates a new action with the given label.
	 */
	public OpenJavaElementAction(String label) {
		super(label);
	}

	/**
	 * Creates a new action with the given label and image.
	 */
	public OpenJavaElementAction(String label, ImageDescriptor image) {
		super(label, image);
	}
	
	/**
	 * Opens the editor on the given source reference and subsequently selects it.
	 */
	protected void open(ISourceReference sourceReference) throws JavaModelException, PartInitException {
		IEditorPart part= EditorUtility.openInEditor(sourceReference);
		EditorUtility.revealInEditor(part, sourceReference);
	}
		
	/**
	 * Filters out source references from the given code resolve results.
	 * A utility method that can be called by subclassers. 
	 */
	protected List filterResolveResults(IJavaElement[] codeResolveResults) {
		int nResults= codeResolveResults.length;
		List refs= new ArrayList(nResults);
		for (int i= 0; i < nResults; i++) {
			if (codeResolveResults[i] instanceof ISourceReference)
				refs.add(codeResolveResults[i]);
		}
		
		return refs;
	}
						
	/**
	 * Shows a dialog to selecting an ambigous source reference.
	 * Utility method that can be called by subclassers.
	 */
	protected ISourceReference selectSourceReference(List sourceReferences, Shell shell, String title, String message) {
		
		int nResults= sourceReferences.size();
		
		if (nResults == 0)
			return null;
		else if (nResults == 1)
			return (ISourceReference) sourceReferences.get(0);
		
		int flags= (JavaElementLabelProvider.SHOW_DEFAULT | 
						JavaElementLabelProvider.SHOW_CONTAINER_QUALIFICATION | 
						JavaElementLabelProvider.SHOW_ROOT);
						
		ElementListSelectionDialog d= new ElementListSelectionDialog(shell, title, null, new JavaElementLabelProvider(flags), true, false);
		d.setMessage(message);
		if (d.open(sourceReferences, null) == d.OK) {
			Object[] elements= d.getResult();
			if (elements != null && elements.length > 0) {
				nResults= elements.length;
				for (int i= 0; i < nResults; i++) {
					Object curr= elements[i];
					if (curr instanceof ISourceReference)
						return (ISourceReference) curr;
				}
			}
		}
		
		return null;
	}
}