/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000,2001
 */
package org.eclipse.jdt.internal.ui.compare;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

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
 * Provides replace from local history for Java elements.
 */
public class JavaReplaceWithEditionAction extends JavaHistoryAction {
				
	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.ui.compare.ReplaceWithEditionAction";

	/**
	 * 
	 */
	private class DocumentNode implements ITypedElement, IStreamContentAccessor {
	
		private IDocument fDocument;
		private String fType;
		
		DocumentNode(IDocument document, String type) {
			fDocument= document;
			fType= type;
		}
	
		public String getType() {
			return fType;
		}
	
		public Image getImage() {
			return null;
		}
		
		public String getName() {
			return "name??";
		}
		
		public InputStream getContents() {
			return new ByteArrayInputStream(fDocument.get().getBytes());
		}
	}

	public JavaReplaceWithEditionAction(ISelectionProvider sp) {
		super(sp, BUNDLE_NAME);
	}
			
	/**
	 * @see Action#run
	 */
	public final void run() {
		
		Shell parent= JavaPlugin.getActiveWorkbenchShell();
		
		ISelection selection= fSelectionProvider.getSelection();
		IMember input= getEditionElement(selection);
		if (input == null) {
			// shouldn't happen because Action should not be enabled in the first place
			MessageDialog.openInformation(parent, fTitle, "No editions for selection");
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
		}
		if (file == null) {
			MessageDialog.openInformation(parent, fTitle, "Can't find underlying file");
			return;
		}
		
		// setup array of editions
		int numberOfEditions= 1;
		IFileState[] states= null;
		
		// add available editions
		try {
			states= file.getHistory(null);
		} catch (CoreException ex) {
		}
		
		if (states != null)
			numberOfEditions += states.length;
			
		ITypedElement[] editions= new ITypedElement[numberOfEditions];
		editions[0]= new ResourceNode(file);
		if (states != null)		
			for (int i= 0; i < states.length; i++)
				editions[i+1]= new HistoryItem(editions[0], states[i]);
		
		if (editions.length <= 0) {
			MessageDialog.openInformation(parent, fTitle, "No editions available");
			return;
		}
		
		DocumentManager docManager= null;
		try {
			docManager= new DocumentManager(cu);
		} catch(JavaModelException ex) {
			MessageDialog.openInformation(parent, fTitle, "JavaModelException");
			return;
		}
		
		try {
			docManager.connect();
		
			EditionSelectionDialog d= new EditionSelectionDialog(parent, fBundle);
			
			IDocument document= docManager.getDocument();
			String type= file.getFileExtension();
			ITypedElement ti= d.selectEdition(new DocumentNode(document, type), editions, input);
						
			if (ti instanceof IStreamContentAccessor) {
				IStreamContentAccessor sca= (IStreamContentAccessor) ti;				
					
				Position range= null;
				ITypedElement target= d.getTarget();
				if (target instanceof IDocumentRange)
					range= ((IDocumentRange)target).getRange();
		
				String text= JavaCompareUtilities.readString(sca.getContents());	
				if (text != null) {
					document.replace(range.getOffset(), range.getLength(), text);
					//	docManager.save(null);	// should not be necesssary
				}
			}

		} catch(BadLocationException ex) {
			MessageDialog.openInformation(parent, fTitle, "BadLocationException");
		} catch(CoreException ex) {
			MessageDialog.openInformation(parent, fTitle, "CoreException");
		} finally {
			docManager.disconnect();
		}
	}
	
	protected void updateLabel(ISelection selection) {
		String text= "Replace";
		IMember member= null;
		if (!selection.isEmpty()) {
			member= getEditionElement(selection);
			if (member != null) {
				int type= member.getElementType();
				switch (type) {
				case 7:
					text+= " Class";
					break;
				case 8:
					text+= " Field";
					break;
				case 9:
					text+= " Method";
					break;
				case 10:
					text+= " Initializer";
					break;
				default:
					text+= " type("+type+")";
					break;
				}
			}
		}
		setText(text + " from Local History...");
		setEnabled(member != null);
	}
}

