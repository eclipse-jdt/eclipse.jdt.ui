/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.compare;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ResourceBundle;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.*;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.DocumentManager;

import org.eclipse.compare.*;
import org.eclipse.compare.contentmergeviewer.IDocumentRange;


/**
 * Provides "Replace from local history" for Java elements.
 */
public class JavaReplaceWithEditionAction extends JavaHistoryAction {
				
	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.ui.compare.ReplaceWithEditionAction"; //$NON-NLS-1$
	
	/**
	 * Makes the IStreamContentAccessor and ITypedElement protocols
	 * available to for Java element within a document.
	 */
	private class DocumentNode implements ITypedElement, IStreamContentAccessor {
	
		private IDocument fDocument;
		private String fName;
		private String fType;
		
		DocumentNode(IDocument document, String name, String type) {
			fDocument= document;
			fName= name;
			fType= type;
		}
	
		public String getType() {
			return fType;
		}
	
		public Image getImage() {
			return null;
		}
		
		public String getName() {
			return fName;
		}
		
		public InputStream getContents() {
			return new ByteArrayInputStream(fDocument.get().getBytes());
		}
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
		
		ISelection selection= fSelectionProvider.getSelection();
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
				
		DocumentManager docManager= null;
		try {
			docManager= new DocumentManager(cu);
		} catch (JavaModelException ex) {
			JavaPlugin.log(ex);
			MessageDialog.openError(shell, errorTitle, errorMessage);
			return;
		}
		
		try {
			docManager.connect();
			ResourceBundle bundle= ResourceBundle.getBundle(BUNDLE_NAME);
			EditionSelectionDialog d= new EditionSelectionDialog(shell, bundle);
			
			IDocument document= docManager.getDocument();
			String type= file.getFileExtension();
			ITypedElement ti= d.selectEdition(new DocumentNode(document, cu.getElementName(), type), editions, input);
						
			if (ti instanceof IStreamContentAccessor) {
				IStreamContentAccessor sca= (IStreamContentAccessor) ti;				
					
				Position range= null;
				ITypedElement target= d.getTarget();
				if (target instanceof IDocumentRange)
					range= ((IDocumentRange)target).getRange();
		
				if (range != null) {	// shouldn't happen
					String text= JavaCompareUtilities.readString(sca.getContents());	
					if (text != null) {
						document.replace(range.getOffset(), range.getLength(), text);
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
	
	protected boolean isEnabled(ISelection selection) {
		return getEditionElement(selection) != null;
	}
}

