/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.compare;

import java.util.ResourceBundle;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.internal.corext.codemanipulation.MemberEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.*;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.compare.JavaHistoryAction.JavaTextBufferNode;
import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage;

import org.eclipse.compare.*;

/**
 * Provides "Replace from local history" for Java elements.
 */
public class JavaCompareWithEditionAction extends JavaHistoryAction {
	
	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.ui.compare.CompareWithEditionAction"; //$NON-NLS-1$
	
	
	public JavaCompareWithEditionAction() {
	}	

	// CompareMessages.getString("ReplaceFromHistory.action.label")

	public void run(IAction action) {
		
		String errorTitle= CompareMessages.getString("ReplaceFromHistory.title"); //$NON-NLS-1$
		String errorMessage= CompareMessages.getString("ReplaceFromHistory.internalErrorMessage"); //$NON-NLS-1$
		
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		// shell can be null; as a result error dialogs won't be parented
		
		ISelection selection= getSelection();
		IMember input= getEditionElement(selection);
		if (input == null) {
			// shouldn't happen because Action should not be enabled in the first place
			MessageDialog.openInformation(shell, errorTitle, errorMessage);
			return;
		}
		
		IFile file= getFile(input);
		if (file == null) {
			MessageDialog.openError(shell, errorTitle, errorMessage);
			return;
		}
		
		boolean inEditor= beingEdited(file);
		if (inEditor)
			input= (IMember) getWorkingCopy(input);

		// get a TextBuffer where to insert the text
		TextBuffer buffer= null;
		try {
			buffer= TextBuffer.acquire(file);

			ITypedElement target= new JavaTextBufferNode(buffer, inEditor);

			ITypedElement[] editions= buildEditions(target, file);

			ResourceBundle bundle= ResourceBundle.getBundle(BUNDLE_NAME);
			EditionSelectionDialog d= new EditionSelectionDialog(shell, bundle);
			d.setCompareMode(true);

			ITypedElement ti= d.selectEdition(target, editions, input);
						
			if (ti instanceof IStreamContentAccessor) {
				IStreamContentAccessor sca= (IStreamContentAccessor) ti;				
										
				// from the edition get the lines (text) to insert
				String[] lines= null;
				try {
					lines= JavaCompareUtilities.readLines(((IStreamContentAccessor) ti).getContents());								
				} catch (CoreException ex) {
					JavaPlugin.log(ex);
				}
				if (lines == null) {
					MessageDialog.openError(shell, errorTitle, "couldn't get text to insert");
					return;
				}
				
				TextEdit edit= new MemberEdit(input, MemberEdit.REPLACE, lines,
										CodeFormatterPreferencePage.getTabSize());

				TextBufferEditor editor= new TextBufferEditor(buffer);
				editor.add(edit);
				editor.performEdits(new NullProgressMonitor());
				
				TextBuffer.commitChanges(buffer, false, new NullProgressMonitor());
			}

		} catch(CoreException ex) {
			JavaPlugin.log(ex);
			MessageDialog.openError(shell, errorTitle, "error with TextBuffer");
		} finally {
			if (buffer != null)
				TextBuffer.release(buffer);
		}
	}
}

