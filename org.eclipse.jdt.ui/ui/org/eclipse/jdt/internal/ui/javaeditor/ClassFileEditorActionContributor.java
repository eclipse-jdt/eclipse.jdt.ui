package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.ResourceBundle;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.texteditor.BasicTextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;


public class ClassFileEditorActionContributor extends BasicTextEditorActionContributor {
	
	protected RetargetTextEditorAction fShowJavaDoc;
	protected TogglePresentationAction fTogglePresentationAction;
		/* 1GEYIIA: ITPJUI:WINNT - Hover Toggle not available for classfile editors */
	protected ToggleTextHoverAction fToggleTextHover;
	
	//protected RetargetTextEditorAction fDisplay;
	//protected RetargetTextEditorAction fInspect;
	
	public ClassFileEditorActionContributor() {
		super();
		
		ResourceBundle bundle= JavaEditorMessages.getResourceBundle();
		fShowJavaDoc= new RetargetTextEditorAction(JavaEditorMessages.getResourceBundle(), "ShowJavaDoc."); //$NON-NLS-1$
		fTogglePresentationAction= new TogglePresentationAction();				
		/* 1GEYIIA: ITPJUI:WINNT - Hover Toggle not available for classfile editors */
		fToggleTextHover= new ToggleTextHoverAction();
		
		//fDisplay= new RetargetTextEditorAction(bundle, "DisplayAction."); //$NON-NLS-1$	
		//fInspect= new RetargetTextEditorAction(bundle, "InpsectAction."); //$NON-NLS-1$
	}
	
	/**
	 * @see EditorActionBarContributor#contributeToMenu(IMenuManager)
	 */
	public void contributeToMenu(IMenuManager menu) {
		
		super.contributeToMenu(menu);
		
		IMenuManager editMenu= menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
		if (editMenu != null) {

			editMenu.add(new Separator(IContextMenuConstants.GROUP_OPEN));			
			editMenu.add(new Separator(IContextMenuConstants.GROUP_GENERATE));
			editMenu.add(new Separator(IContextMenuConstants.GROUP_ADDITIONS));
				
			editMenu.appendToGroup(IContextMenuConstants.GROUP_GENERATE, fShowJavaDoc);
			
			//editMenu.appendToGroup(IContextMenuConstants.GROUP_ADDITIONS, fInspect);		
			//editMenu.appendToGroup(IContextMenuConstants.GROUP_ADDITIONS, fDisplay);
		}
	}
	
	/**
	 * @see EditorActionBarContributor#contributeToToolBar(IToolBarManager)
	 */
	public void contributeToToolBar(IToolBarManager tbm) {
		tbm.add(new Separator());
		tbm.add(fTogglePresentationAction);		
		/* 1GEYIIA: ITPJUI:WINNT - Hover Toggle not available for classfile editors */
		tbm.add(fToggleTextHover);
	}
	
	/**
	 * @see IEditorActionBarContributor#setActiveEditor(IEditorPart)
	 */
	public void setActiveEditor(IEditorPart part) {
		
		super.setActiveEditor(part);
		
		ITextEditor textEditor= null;
		if (part instanceof ITextEditor)
			textEditor= (ITextEditor) part;
			
		ClassFileEditor classFileEditor= null;	
		if (part instanceof ClassFileEditor)
			classFileEditor= (ClassFileEditor)part;
		
		fShowJavaDoc.setAction(getAction(textEditor, "ShowJavaDoc")); //$NON-NLS-1$
		fTogglePresentationAction.setEditor(textEditor);		
		/* 1GEYIIA: ITPJUI:WINNT - Hover Toggle not available for classfile editors */
		fToggleTextHover.setEditor(textEditor);

		if (classFileEditor != null) {
			IActionBars bars= getActionBars();
			classFileEditor.fStandardActionGroups.fillActionBars(bars);
		}

		
		//IAction updateAction= getAction(textEditor, "Display"); //$NON-NLS-1$
		//if (updateAction instanceof IUpdate) {
		//	((IUpdate)updateAction).update();
		//}
		//fDisplay.setAction(updateAction); 
		//updateAction= getAction(textEditor, "Inspect"); //$NON-NLS-1$
		//if (updateAction instanceof IUpdate) {
		//	((IUpdate)updateAction).update();
		//}
		//fInspect.setAction(updateAction);
	}
}