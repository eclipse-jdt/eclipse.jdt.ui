/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
// AW
package org.eclipse.jdt.internal.ui.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;

import org.eclipse.jface.action.Action;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.ui.JavaElementLabelProvider;


/**
 * Opens a Java element in the specified editor. Subclasses must overwrite
 * <code>IAction.run</code> and knit the methods together.
 */
public abstract class OpenJavaElementAction extends Action {
	
	/**
	 * Creates a new action without label. Initializing is 
	 * subclass responsibility.
	 */
	protected OpenJavaElementAction() {
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
	 * Shows a dialog for resolving an ambigous source reference.
	 * Utility method that can be called by subclassers.
	 */
	protected ISourceReference selectSourceReference(List sourceReferences, Shell shell, String title, String message) {
		
		int nResults= sourceReferences.size();
		
		if (nResults == 0)
			return null;
		
		if (nResults == 1)
			return (ISourceReference) sourceReferences.get(0);
		
		int flags= JavaElementLabelProvider.SHOW_DEFAULT
						| JavaElementLabelProvider.SHOW_QUALIFIED
							| JavaElementLabelProvider.SHOW_ROOT;
						
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(shell, new JavaElementLabelProvider(flags));
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.setElements(sourceReferences.toArray());
		
		if (dialog.open() == dialog.OK) {
			Object[] elements= dialog.getResult();
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
