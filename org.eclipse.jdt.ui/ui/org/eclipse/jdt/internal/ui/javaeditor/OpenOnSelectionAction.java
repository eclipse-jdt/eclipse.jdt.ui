package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.util.List;
import java.util.ResourceBundle;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.jface.viewers.ISelectionChangedListener;import org.eclipse.jface.viewers.ISelectionProvider;import org.eclipse.jface.viewers.SelectionChangedEvent;import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.OpenJavaElementAction;



/**
 * This action opens a java editor on the element represented by text selection of
 * the connected java source viewer.
 */
public class OpenOnSelectionAction extends OpenJavaElementAction {
	
	
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
	
		
	protected ResourceBundle fBundle;
	protected String fPrefix;
	protected ITextEditor fEditor;
	protected ISelectionChangedListener fListener= new SelectionChangedListener();
	
	public OpenOnSelectionAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
		super(bundle, prefix);
		fBundle= bundle;
		fPrefix= prefix;
		fEditor= editor;
		setEnabled(false);
	}
	
	public OpenOnSelectionAction(ResourceBundle bundle, String prefix) {
		this(bundle, prefix, null);
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
	
	protected String getResourceString(String key) {
		return fBundle.getString(fPrefix + key);
	}
	
	
	protected ICodeAssist getCodeAssist() {	
		IEditorInput input= fEditor.getEditorInput();
		if (input instanceof ClassFileEditorInput) {
			ClassFileEditorInput cfInput= (ClassFileEditorInput) input;
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
			ITextSelection selection= (ITextSelection)fEditor.getSelectionProvider().getSelection();
			try {
				if (selection.getLength() > 0) {
					IJavaElement[] result= resolve.codeSelect(selection.getOffset(), selection.getLength());
					if (result != null && result.length > 0) {
						List filtered= filterResolveResults(result);
						ISourceReference chosen= selectSourceReference(filtered, getShell(), getResourceString("title"), getResourceString("message"));
						if (chosen != null) {
							open(chosen);
							return;
						}
					}
				}
			} catch (JavaModelException x) {
			} catch (PartInitException x) {
			}
		}
		
		getShell().getDisplay().beep();		
	}
	
	protected Shell getShell() {
		return fEditor.getSite().getShell();
	}					
}