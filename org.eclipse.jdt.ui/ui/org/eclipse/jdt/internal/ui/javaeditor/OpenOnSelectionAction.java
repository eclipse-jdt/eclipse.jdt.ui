package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.util.List;
import java.util.ResourceBundle;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.AbstractOpenJavaElementAction;



/**
 * This action opens a java editor on the element represented by text selection of
 * the connected java source viewer.
 */
public class OpenOnSelectionAction extends AbstractOpenJavaElementAction {
	
	
	class SelectionChangedListener implements ISelectionChangedListener {
		
		/*
		 * @see ISelectionChangedListener#selectionChanged(SelectionChangedEvent)
		 */
		public void selectionChanged(SelectionChangedEvent event) {
			ISelection s= event.getSelection();
			if (s instanceof ITextSelection) {
				ITextSelection ts= (ITextSelection) s;
				setEnabled(ts.getLength() > 0);
			}
		}
	};
	
		
	private String fDialogTitle;
	private String fDialogMessage;
	protected ITextEditor fEditor;
	protected ISelectionChangedListener fListener= new SelectionChangedListener();
	
	
	/**
	 * Creates a new action with the given label and image.
	 */
	protected OpenOnSelectionAction() {
		super();
		
		setText(JavaEditorMessages.getString("OpenOnSelection.label")); //$NON-NLS-1$
		setToolTipText(JavaEditorMessages.getString("OpenOnSelection.tooltip")); //$NON-NLS-1$
		setDescription(JavaEditorMessages.getString("OpenOnSelection.description")); //$NON-NLS-1$
		setDialogTitle(JavaEditorMessages.getString("OpenOnSelection.dialog.title")); //$NON-NLS-1$
		setDialogMessage(JavaEditorMessages.getString("OpenOnSelection.dialog.message")); //$NON-NLS-1$
	}
	
	/**
	 * Creates a new action with the given image.
	 */
	public OpenOnSelectionAction(ImageDescriptor image) {
		this();
		setImageDescriptor(image);
	}
	
	protected void setDialogTitle(String title) {
		fDialogTitle= title;
	}
	
	protected void setDialogMessage(String message) {
		fDialogMessage= message;
	}
	
	public void setContentEditor(ITextEditor editor) {
		
		if (fEditor != null) {
			ISelectionProvider p= fEditor.getSelectionProvider();
			if (p != null) p.removeSelectionChangedListener(fListener);
		}
		
		fEditor= editor;
		
		if (fEditor != null) {
			ISelectionProvider p= fEditor.getSelectionProvider();
			if (p != null) p.addSelectionChangedListener(fListener);
		}
	}
	
	protected ICodeAssist getCodeAssist() {	
		IEditorInput input= fEditor.getEditorInput();
		if (input instanceof IClassFileEditorInput) {
			IClassFileEditorInput cfInput= (IClassFileEditorInput) input;
			return cfInput.getClassFile();
		}
		
		IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();				
		return manager.getWorkingCopy(input);
	}
	
	/**
	 * @see IAction#actionPerformed
	 */
	public void run() {
		
		ICodeAssist resolve= getCodeAssist();
		if (resolve != null && fEditor.getSelectionProvider() != null) {
			ITextSelection selection= (ITextSelection) fEditor.getSelectionProvider().getSelection();
			try {
				if (selection.getLength() > 0) {
					IJavaElement[] result= resolve.codeSelect(selection.getOffset(), selection.getLength());
					if (result != null && result.length > 0) {
						List filtered= filterResolveResults(result);
						IJavaElement selected= selectJavaElement(filtered, getShell(), fDialogTitle, fDialogMessage);
						if (selected != null) {
							open(selected);
							return;
						}
					}
				} else {
					openOnEmptySelection(selection);
					return;
				}
			} catch (JavaModelException x) {
				JavaPlugin.log(x.getStatus());
			} catch (PartInitException x) {
				JavaPlugin.log(x);
			}
		}
		
		getShell().getDisplay().beep();		
	}
	
	protected void openOnEmptySelection(ITextSelection selection) throws JavaModelException, PartInitException {
		getShell().getDisplay().beep();
	}
	
	protected Shell getShell() {
		return fEditor.getSite().getShell();
	}					
}