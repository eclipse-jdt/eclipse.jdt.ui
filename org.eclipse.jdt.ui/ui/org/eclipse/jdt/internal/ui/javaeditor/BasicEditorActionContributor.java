package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.util.ResourceBundle;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.texteditor.BasicTextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;
import org.eclipse.ui.texteditor.TextEditorAction;

import org.eclipse.jdt.ui.IContextMenuConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;



public class BasicEditorActionContributor extends BasicTextEditorActionContributor {
	
	
	protected static class SelectionAction extends TextEditorAction implements ISelectionChangedListener {
		
		protected int fOperationCode;
		protected ITextOperationTarget fOperationTarget= null;
		
		
		public SelectionAction(String prefix, int operation) {
			super(JavaEditorMessages.getResourceBundle(), prefix, null);
			fOperationCode= operation;
			setEnabled(false);
		}
		
		/**
		 * @see TextEditorAction#setEditor(ITextEditor)
		 */
		public void setEditor(ITextEditor editor) {
			if (getTextEditor() != null) {
				ISelectionProvider p= getTextEditor().getSelectionProvider();
				if (p != null) p.removeSelectionChangedListener(this);
			}
				
			super.setEditor(editor);
			
			if (editor != null) {
				ISelectionProvider p= editor.getSelectionProvider();
				if (p != null) p.addSelectionChangedListener(this);
				fOperationTarget= (ITextOperationTarget) editor.getAdapter(ITextOperationTarget.class);
			} else
				fOperationTarget= null;
				
			selectionChanged(null);
		}
		
		/**
		 * @see ISelectionChangedListener#selectionChanged(SelectionChangedEvent)
		 */
		public void selectionChanged(SelectionChangedEvent event) {
			boolean isEnabled= (fOperationTarget != null && fOperationTarget.canDoOperation(fOperationCode));
			setEnabled(isEnabled);
		}
		
		/**
		 * @see Action#run()
		 */
		public void run() {
			if (fOperationCode != -1 && fOperationTarget != null)
				fOperationTarget.doOperation(fOperationCode);
		}
	};	
	
	protected RetargetTextEditorAction fContentAssist;	
	protected RetargetTextEditorAction fContextInformation;	
	protected SelectionAction fShiftRight;
	protected SelectionAction fShiftLeft;
	
	
	public BasicEditorActionContributor() {
		super();
		
		fContentAssist= new RetargetTextEditorAction(JavaEditorMessages.getResourceBundle(), "ContentAssistProposal."); //$NON-NLS-1$
		fContextInformation= new RetargetTextEditorAction(JavaEditorMessages.getResourceBundle(), "ContentAssistContextInformation."); //$NON-NLS-1$
		fShiftRight= new SelectionAction("ShiftRight.", ITextOperationTarget.SHIFT_RIGHT);		 //$NON-NLS-1$
		fShiftLeft= new SelectionAction("ShiftLeft.", ITextOperationTarget.SHIFT_LEFT); //$NON-NLS-1$
		
		fShiftRight.setImageDescriptor(JavaPluginImages.DESC_MENU_SHIFT_RIGHT);
		fShiftLeft.setImageDescriptor(JavaPluginImages.DESC_MENU_SHIFT_LEFT);
		
		fShiftRight.setAccelerator(0);
		fShiftLeft.setAccelerator(0);
	}
	
	/**
	 * @see EditorActionBarContributor#contributeToMenu(IMenuManager)
	 */
	public void contributeToMenu(IMenuManager menu) {
		
		super.contributeToMenu(menu);
		
		IMenuManager editMenu= menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
		if (editMenu != null) {
			
			editMenu.add(fShiftRight);
			editMenu.add(fShiftLeft);
			
			editMenu.add(new Separator(IContextMenuConstants.GROUP_OPEN));
			editMenu.add(new Separator(IContextMenuConstants.GROUP_GENERATE));
			
			editMenu.appendToGroup(IContextMenuConstants.GROUP_GENERATE, fContentAssist);
			editMenu.appendToGroup(IContextMenuConstants.GROUP_GENERATE, fContextInformation);
		}
		
	}
	
	/**
	 * @see IEditorActionBarContributor#setActiveEditor(IEditorPart)
	 */
	public void setActiveEditor(IEditorPart part) {
		
		super.setActiveEditor(part);
		
		IStatusLineManager manager= getActionBars().getStatusLineManager();
		manager.setMessage(null);
		manager.setErrorMessage(null);
		
		ITextEditor textEditor= null;
		if (part instanceof ITextEditor)
			textEditor= (ITextEditor) part;
			
		fContentAssist.setAction(getAction(textEditor, "ContentAssistProposal")); //$NON-NLS-1$
		fContextInformation.setAction(getAction(textEditor, "ContentAssistContextInformation")); //$NON-NLS-1$
		fShiftRight.setEditor(textEditor);
		fShiftLeft.setEditor(textEditor);
	}
}