/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.compare;

import java.util.ResourceBundle;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.ui.IEditorInput;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.corext.codemanipulation.MemberEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.*;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.compare.*;


public class JavaAddElementFromHistory extends JavaHistoryAction {
	
	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.ui.compare.AddFromHistoryAction"; //$NON-NLS-1$
	
	private JavaEditor fEditor;

	public JavaAddElementFromHistory() {
		super(true);
	}
	
	public void run(IAction action) {
		
		String errorTitle= CompareMessages.getString("AddFromHistory.title"); //$NON-NLS-1$
		String errorMessage= CompareMessages.getString("AddFromHistory.internalErrorMessage"); //$NON-NLS-1$
		
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		// shell can be null; as a result error dialogs won't be parented
		
		ICompilationUnit cu= null;
		IParent parent= null;
		IMember input= null;
		
		// analyse selection
		ISelection selection= getSelection();
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
			} else {
				if (selection instanceof IStructuredSelection) {
					Object o= ((IStructuredSelection)selection).getFirstElement();
					if (o instanceof ICompilationUnit) {
						cu= (ICompilationUnit) o;
						parent= cu;
					}
				}
			}
		}
		
		if (parent == null || cu == null) {
			MessageDialog.openError(shell, errorTitle, errorMessage);
			return;
		}
		
		IFile file= getFile(parent);
		if (file == null) {
			MessageDialog.openError(shell, errorTitle, errorMessage);
			return;
		}
				
		boolean inEditor= beingEdited(file);
		if (inEditor) {
			parent= (IParent) getWorkingCopy((IJavaElement)parent);
			if (input != null)
				input= (IMember) getWorkingCopy(input);
		}

		// get a TextBuffer where to insert the text
		TextBuffer buffer= null;
		try {
			buffer= TextBuffer.acquire(file);
		
			// configure EditionSelectionDialog and let user select an edition
			ITypedElement target= new JavaTextBufferNode(buffer, inEditor);

			ITypedElement[] editions= buildEditions(target, file);
											
			ResourceBundle bundle= ResourceBundle.getBundle(BUNDLE_NAME);
			EditionSelectionDialog d= new EditionSelectionDialog(shell, bundle);
			d.setAddMode(true);
			ITypedElement ti= d.selectEdition(target, editions, parent);
			if (!(ti instanceof IStreamContentAccessor))
				return;	// user cancel
			
			// from the edition get the lines (text) to insert
			String[] lines= null;
			try {
				lines= JavaCompareUtilities.readLines(((IStreamContentAccessor) ti).getContents());								
			} catch (CoreException ex) {
				JavaPlugin.log(ex);
			}
			if (lines == null) {
				MessageDialog.openError(shell, errorTitle, errorMessage);
				return;
			}
			
			// build the TextEdit that inserts the text into the buffer
			MemberEdit edit= null;
			if (input != null)
				edit= new MemberEdit(input, MemberEdit.INSERT_AFTER, lines,
										CodeFormatterPreferencePage.getTabSize());
			else
				edit= createEdit(lines, parent);
			if (edit == null) {
				MessageDialog.openError(shell, errorTitle, errorMessage);
				return;
			}
			edit.setAddLineSeparators(false);
			
			IProgressMonitor nullProgressMonitor= new NullProgressMonitor();

			TextBufferEditor editor= new TextBufferEditor(buffer);
			editor.add(edit);
			editor.performEdits(nullProgressMonitor);
			
			final TextBuffer bb= buffer;
			IRunnableWithProgress r= new IRunnableWithProgress() {
				public void run(IProgressMonitor pm) throws InvocationTargetException {
					try {
						TextBuffer.commitChanges(bb, false, pm);
					} catch (CoreException ex) {
						throw new InvocationTargetException(ex);
					}
				}
			};
			
			if (inEditor) {
				// we don't show progress
				r.run(nullProgressMonitor);
			} else {
				ProgressMonitorDialog pd= new ProgressMonitorDialog(shell);
				pd.run(true, false, r);
			}

	 	} catch(InvocationTargetException ex) {
			ExceptionHandler.handle(ex, shell, errorTitle, errorMessage);
			
		} catch(InterruptedException ex) {
			// shouldn't be called because is not cancable
			
		} catch(CoreException ex) {
			ExceptionHandler.handle(ex, shell, errorTitle, errorMessage);
		} finally {
			if (buffer != null)
				TextBuffer.release(buffer);
		}
	}
	
	/**
	 * Creates a TextEdit for inserting the given lines into the container. 
	 */
	private MemberEdit createEdit(String[] lines, IParent container) {
		
		// find a child where to insert before
		IJavaElement[] children= null;
		try {
			children= container.getChildren();
		} catch(JavaModelException ex) {
		}
		if (children != null) {
			IJavaElement candidate= null;
			for (int i= 0; i < children.length; i++) {
				IJavaElement chld= children[i];
				
				switch (chld.getElementType()) {
				case IJavaElement.PACKAGE_DECLARATION:
				case IJavaElement.IMPORT_CONTAINER:
					// skip these but remember the last of them
					candidate= chld;
					continue;
				default:
					return new MemberEdit(chld, MemberEdit.INSERT_BEFORE, lines,
											CodeFormatterPreferencePage.getTabSize());
				}
			}
			if (candidate != null)
				return new MemberEdit(candidate, MemberEdit.INSERT_AFTER, lines,
											CodeFormatterPreferencePage.getTabSize());
		}
		
		// no children: insert at end (but before closing bracket)
		if (container instanceof IJavaElement)
			return new MemberEdit((IJavaElement)container, MemberEdit.ADD_AT_END, lines,
											CodeFormatterPreferencePage.getTabSize());
											
		return null;
	}
	
	protected boolean isEnabled(ISelection selection) {
		
		if (selection.isEmpty()) {
			if (fEditor != null) {
				// we check whether editor shows CompilationUnit
				IEditorInput editorInput= fEditor.getEditorInput();
				IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
				return manager.getWorkingCopy(editorInput) != null;
			}
			return false;
		}
		
		if (selection instanceof IStructuredSelection) {
			Object o= ((IStructuredSelection)selection).getFirstElement();
			if (o instanceof ICompilationUnit)
				return true;
		}
		
		return super.isEnabled(selection);
	}
}
