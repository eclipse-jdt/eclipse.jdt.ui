/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.compare;

import java.util.ResourceBundle;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.corext.codemanipulation.*;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage;

import org.eclipse.compare.*;


/**
 * Provides "Replace from local history" for Java elements.
 */
public class JavaReplaceWithEditionAction extends JavaHistoryAction {
				
	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.ui.compare.ReplaceWithEditionAction"; //$NON-NLS-1$
	
	
	public JavaReplaceWithEditionAction() {
	}	

	public JavaReplaceWithEditionAction(ISelectionProvider sp) {
		super(sp);
		
		setText(CompareMessages.getString("ReplaceFromHistory.action.label")); //$NON-NLS-1$
		
		update();
	}
			
	/**
	 * @see Action#run
	 */
	public final void run() {
		
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
		
		// extract CU from selection
		ICompilationUnit cu= input.getCompilationUnit();
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
		
		// setup array of editions
		int numberOfEditions= 1;
		IFileState[] states= null;
		
		// add available editions
		try {
			states= file.getHistory(null);
		} catch (CoreException ex) {
			JavaPlugin.log(ex);
		}
		
		if (states != null)
			numberOfEditions += states.length;
			
		ITypedElement[] editions= new ITypedElement[numberOfEditions];
		editions[0]= new ResourceNode(file);
		if (states != null)		
			for (int i= 0; i < states.length; i++)
				editions[i+1]= new HistoryItem(editions[0], states[i]);
						
		boolean inEditor= JavaCompareUtilities.beingEdited(file);

		// get a TextBuffer where to insert the text
		TextBuffer buffer= null;
		try {
			buffer= TextBuffer.acquire(file);

			ResourceBundle bundle= ResourceBundle.getBundle(BUNDLE_NAME);
			EditionSelectionDialog d= new EditionSelectionDialog(shell, bundle);
			
			ITypedElement ti= d.selectEdition(new JavaTextBufferNode(buffer, inEditor), editions, input);
						
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
	
	protected boolean isEnabled(ISelection selection) {
		return getEditionElement(selection) != null;
	}
}

