/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2001
 */
package org.eclipse.jdt.internal.ui.compare;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jdt.internal.ui.util.DocumentManager;

import org.eclipse.ui.IEditorInput;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.compare.*;
import org.eclipse.compare.contentmergeviewer.IDocumentRange;;


public class JavaAddElementFromHistory extends JavaHistoryAction {
	
	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.ui.compare.AddFromHistoryAction";
	
	private JavaEditor fEditor;
		
	public JavaAddElementFromHistory(JavaEditor editor, ISelectionProvider sp) {
		super(sp, BUNDLE_NAME);
		fEditor= editor;
	}
	
	/**
	 * @see Action#run
	 */
	public final void run() {
		
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		
		ISelection selection= fSelectionProvider.getSelection();
		
		ICompilationUnit cu= null;
		IParent parent= null;
		int insertPosition= -1;		// where to insert the element
		
		if (selection.isEmpty()) {
			if (fEditor != null) {
				IEditorInput editorInput= fEditor.getEditorInput();
				IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
				cu= manager.getWorkingCopy(editorInput);
				parent= cu;
			}
			
		} else {
			IMember input= getEditionElement(selection);
			if (input == null) {
				// shouldn't happen because Action should not be enabled in the first place
				MessageDialog.openError(shell, fTitle, "No editions for selection");
				return;
			}
			cu= input.getCompilationUnit();
			
			if (input instanceof IParent) {
				parent= (IParent)input;
			} else {
				IJavaElement parentElement= input.getParent();
				if (parentElement instanceof IParent) {
					parent= (IParent)parentElement;
					if (input instanceof ISourceReference) {
						ISourceReference sr= (ISourceReference) input;
						try {
							ISourceRange range= sr.getSourceRange();
							insertPosition= range.getOffset() + range.getLength();
						} catch(JavaModelException ex) {
							MessageDialog.openError(shell, fTitle, "Can't find range to replace");
						}
					}
				}
			}
		}
		
		// extract CU from selection
		if (cu.isWorkingCopy())
			cu= (ICompilationUnit) cu.getOriginalElement();

		// find underlying file
		IFile file= null;
		try {
			file= (IFile) cu.getUnderlyingResource();
		} catch (JavaModelException ex) {
		}
		if (file == null) {
			MessageDialog.openError(shell, fTitle, "Can't find underlying file");
			return;
		}
		
		// get available editions
		IFileState[] states= null;
		try {
			states= file.getHistory(null);
		} catch (CoreException ex) {
		}	
		if (states == null || states.length <= 0) {
			MessageDialog.openInformation(shell, fTitle, "No editions available");
			return;
		}
		
		ITypedElement target= new ResourceNode(file);
		ITypedElement[] editions= new ITypedElement[states.length];
		for (int i= 0; i < states.length; i++)
			editions[i]= new HistoryItem(target, states[i]);
				
		DocumentManager docManager= null;
		try {
			docManager= new DocumentManager(cu);
		} catch(JavaModelException ex) {
			MessageDialog.openError(shell, fTitle, "JavaModelException");
			return;
		}
		
		try {
			docManager.connect();
		
			EditionSelectionDialog d= new EditionSelectionDialog(shell, fBundle);
			
			IDocument document= docManager.getDocument();
			String type= file.getFileExtension();
			
			ITypedElement ti= d.selectEdition(target, editions, parent);

			if (ti instanceof IStreamContentAccessor) {
				IStreamContentAccessor sca= (IStreamContentAccessor) ti;				
					
//				Position range= null;
//				ITypedElement target2= d.getTarget();
//				if (target2 instanceof IDocumentRange)
//					range= ((IDocumentRange)target2).getRange();
		
				String text= JavaCompareUtilities.readString(sca.getContents());	
				if (text != null && insertPosition >= 0) {
					document.replace(insertPosition, 0, text);
					//	docManager.save(null);	// should not be necesssary
				}
			}

		} catch(BadLocationException ex) {
			MessageDialog.openError(shell, fTitle, "BadLocationException");
		} catch(CoreException ex) {
			MessageDialog.openError(shell, fTitle, "CoreException");
		} finally {
			docManager.disconnect();
		}
	}
	
	IParent getContainer(ISelection selection) {
		if (selection.isEmpty()) {
			if (fEditor != null) {
				IEditorInput editorInput= fEditor.getEditorInput();
				IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
				return manager.getWorkingCopy(editorInput);
			}
			return null;
		}
		
		IMember input= getEditionElement(selection);
		if (input instanceof IParent)
			return (IParent)input;
				
		if (input != null) {
			IJavaElement parentElement= input.getParent();
			if (parentElement instanceof IParent)
				return (IParent)parentElement;
		}
		
		return null;
	}
	
	protected String getLabelName(ISelection selection) {
		IParent parent= getContainer(selection);
		if (parent instanceof IJavaElement)
			return ((IJavaElement)parent).getElementName();
		return null;
	}
	
//	protected void updateLabel(ISelection selection) {
//		String name= null;
//		IParent parent= getContainer(selection);
//		if (parent instanceof IJavaElement)
//			name= ((IJavaElement)parent).getElementName();
//		if (name != null) {
//			setText("Add to \"" + name + "\" from Local History...");
//			setEnabled(true);
//		} else {
//			setText("Add from Local History...");
//			setEnabled(false);
//		}	
//	}
}
