/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.compare;

import java.util.ResourceBundle;

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
	
	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.ui.compare.AddFromHistoryAction"; //$NON-NLS-1$
	
	private JavaEditor fEditor;

		
	public JavaAddElementFromHistory(JavaEditor editor, ISelectionProvider sp) {
		super(sp);
		fEditor= editor;
		setText(CompareMessages.getString("AddFromHistory.action.label")); //$NON-NLS-1$
		update();
	}
	
	/**
	 * @see Action#run
	 */
	public final void run() {
		
		String errorTitle= CompareMessages.getString("AddFromHistory.title"); //$NON-NLS-1$
		String errorMessage= CompareMessages.getString("AddFromHistory.internalErrorMessage"); //$NON-NLS-1$
		
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		
		ISelection selection= fSelectionProvider.getSelection();
		
		ICompilationUnit cu= null;
		IParent parent= null;
		IMember input= null;
		
		if (selection.isEmpty()) {
			// no selection: we try to use the editor's input
			if (fEditor != null) {
				IEditorInput editorInput= fEditor.getEditorInput();
				IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
				if (manager != null) {
					cu= manager.getWorkingCopy(editorInput);
					parent= cu;
				}
			}
		} else {
			input= getEditionElement(selection);
			if (input != null) {
				cu= input.getCompilationUnit();
			
				if (input instanceof IParent) {
					parent= (IParent)input;
					input= null;
				} else {
					IJavaElement parentElement= input.getParent();
					if (parentElement instanceof IParent)
						parent= (IParent)parentElement;
				}
			}
		}
		
		if (parent == null || cu == null) {
			MessageDialog.openError(shell, errorTitle, errorMessage);
			return;
		}
				
		// extract CU from selection
		if (cu.isWorkingCopy())
			cu= (ICompilationUnit) cu.getOriginalElement();

		// find underlying file
		IFile file= null;
		try {
			file= (IFile) cu.getUnderlyingResource();
		} catch (JavaModelException ex) {
			JavaPlugin.log(ex);
		}
		if (file == null) {
			MessageDialog.openError(shell, errorTitle, errorMessage);
			return;
		}
		
		// get available editions
		IFileState[] states= null;
		try {
			states= file.getHistory(null);
		} catch (CoreException ex) {
			JavaPlugin.log(ex);
		}	
		if (states == null || states.length <= 0) {
			MessageDialog.openInformation(shell, errorTitle, CompareMessages.getString("AddFromHistory.noHistoryMessage")); //$NON-NLS-1$
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
			JavaPlugin.log(ex);
			MessageDialog.openError(shell, errorTitle, errorMessage);
			return;
		}
		
		try {
			docManager.connect();
		
			ResourceBundle bundle= ResourceBundle.getBundle(BUNDLE_NAME);
			EditionSelectionDialog d= new EditionSelectionDialog(shell, bundle);
			d.setAddMode(true);
			
			IDocument document= docManager.getDocument();
			String type= file.getFileExtension();
			
			ITypedElement ti= d.selectEdition(target, editions, parent);

			if (ti instanceof IStreamContentAccessor) {
				IStreamContentAccessor sca= (IStreamContentAccessor) ti;
				
				int insertPosition= findInsertPosition(parent, input, ti, document);
				if (insertPosition >= 0) {
					String text= JavaCompareUtilities.readString(sca.getContents());	
					if (text != null) {
						String delim= document.getLineDelimiter(0);
						document.replace(insertPosition, 0, text+delim);	// insert text
						//	docManager.save(null);	// should not be necesssary
					}
				}
			}

		} catch(BadLocationException ex) {
			JavaPlugin.log(ex);
			MessageDialog.openError(shell, errorTitle, errorMessage);
		} catch(CoreException ex) {
			JavaPlugin.log(ex);
			MessageDialog.openError(shell, errorTitle, errorMessage);
		} finally {
			docManager.disconnect();
		}
	}
		
	/**
	 * Determines a position where to insert the given element into parent.
	 * If child is an ISourceReference the element is added after the child.
	 */
	int findInsertPosition(IParent parent, IMember child, ITypedElement element, IDocument doc) {
		
		if (child instanceof ISourceReference) {
			ISourceReference sr= (ISourceReference) child;
			try {
				ISourceRange range= sr.getSourceRange();
				// the end of the selected method
				return range.getOffset() + range.getLength();
			} catch(JavaModelException ex) {
				JavaPlugin.log(ex);
			}
		}
						
		// find a child where to insert before
		IJavaElement[] children= null;
		try {
			children= parent.getChildren();
		} catch(JavaModelException ex) {
		}
		if (children != null) {
			for (int i= 0; i < children.length; i++) {
				IJavaElement chld= children[i];
				int type= chld.getElementType();
				
				if (type == IJavaElement.PACKAGE_DECLARATION || type == IJavaElement.IMPORT_CONTAINER)
					continue;
					
				if (chld instanceof ISourceReference) {
					ISourceReference sr= (ISourceReference) chld;
					try {
						ISourceRange range= sr.getSourceRange();
						// the start of the first child
						return range.getOffset();
					} catch(JavaModelException ex) {
						JavaPlugin.log(ex);
					}
				}
			}
		}
		
		// no children: insert at end (but before closing bracket)
		if (parent instanceof IJavaElement && parent instanceof ISourceReference) {
			switch (((IJavaElement)parent).getElementType()) {
			case IJavaElement.TYPE:
				ISourceReference sr= (ISourceReference) parent;
				try {
					ISourceRange range= sr.getSourceRange();
					int start= range.getOffset();
					int pos= start + range.getLength();
					while (pos >= start) {
						char c= doc.getChar(pos);
						if (c == '}')
							return pos;
						pos--;
					}
					return pos;
				} catch(JavaModelException ex) {
					JavaPlugin.log(ex);
				} catch(BadLocationException ex) {
					JavaPlugin.log(ex);
				}
				break;
			case IJavaElement.COMPILATION_UNIT:
				return doc.getLength();
			default:
				break;
			}
		}
						
		return 0;
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
}
