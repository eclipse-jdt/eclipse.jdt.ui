/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.actions.RetargetAction;
import org.eclipse.ui.texteditor.BasicTextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.actions.JdtActionConstants;

import org.eclipse.jdt.internal.ui.actions.FoldingActionGroup;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.GoToNextPreviousMemberAction;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.StructureSelectionAction;

/**
 * Common base class for action contributors for Java editors.
 */
public class BasicJavaEditorActionContributor extends BasicTextEditorActionContributor {
	
	private List fPartListeners= new ArrayList();
	
	private TogglePresentationAction fTogglePresentation;
	private ToggleMarkOccurrencesAction fToggleMarkOccurrencesAction;
	private GotoAnnotationAction fPreviousAnnotation;
	private GotoAnnotationAction fNextAnnotation;
	
	private RetargetTextEditorAction fGotoMatchingBracket;
	private RetargetTextEditorAction fShowOutline;
	private RetargetTextEditorAction fOpenStructure;
	private RetargetTextEditorAction fOpenHierarchy;
	
	private RetargetAction fRetargetShowJavaDoc;
	private RetargetTextEditorAction fShowJavaDoc;
	
	private RetargetTextEditorAction fStructureSelectEnclosingAction;
	private RetargetTextEditorAction fStructureSelectNextAction;
	private RetargetTextEditorAction fStructureSelectPreviousAction;
	private RetargetTextEditorAction fStructureSelectHistoryAction;	

	private RetargetTextEditorAction fGotoNextMemberAction;	
	private RetargetTextEditorAction fGotoPreviousMemberAction;

	private RetargetTextEditorAction fRemoveOccurrenceAnnotationsAction;
	
	public BasicJavaEditorActionContributor() {
		super();
		
		ResourceBundle b= JavaEditorMessages.getResourceBundle();
		
		fRetargetShowJavaDoc= new RetargetAction(JdtActionConstants.SHOW_JAVA_DOC, JavaEditorMessages.getString("ShowJavaDoc.label")); //$NON-NLS-1$
		fRetargetShowJavaDoc.setActionDefinitionId(IJavaEditorActionDefinitionIds.SHOW_JAVADOC);
		markAsPartListener(fRetargetShowJavaDoc);
		
		// actions that are "contributed" to editors, they are considered belonging to the active editor
		fTogglePresentation= new TogglePresentationAction();
		
		fToggleMarkOccurrencesAction= new ToggleMarkOccurrencesAction();
		
		fPreviousAnnotation= new GotoAnnotationAction("PreviousAnnotation.", false); //$NON-NLS-1$

		fNextAnnotation= new GotoAnnotationAction("NextAnnotation.", true); //$NON-NLS-1$
		
		fGotoMatchingBracket= new RetargetTextEditorAction(b, "GotoMatchingBracket."); //$NON-NLS-1$
		fGotoMatchingBracket.setActionDefinitionId(IJavaEditorActionDefinitionIds.GOTO_MATCHING_BRACKET);
		
		fShowJavaDoc= new RetargetTextEditorAction(b, "ShowJavaDoc."); //$NON-NLS-1$
		fShowJavaDoc.setActionDefinitionId(IJavaEditorActionDefinitionIds.SHOW_JAVADOC);
		
		fShowOutline= new RetargetTextEditorAction(JavaEditorMessages.getResourceBundle(), "ShowOutline."); //$NON-NLS-1$
		fShowOutline.setActionDefinitionId(IJavaEditorActionDefinitionIds.SHOW_OUTLINE);

		fOpenHierarchy= new RetargetTextEditorAction(JavaEditorMessages.getResourceBundle(), "OpenHierarchy."); //$NON-NLS-1$
		fOpenHierarchy.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_HIERARCHY);
	
		fOpenStructure= new RetargetTextEditorAction(JavaEditorMessages.getResourceBundle(), "OpenStructure."); //$NON-NLS-1$
		fOpenStructure.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_STRUCTURE);
		
		fStructureSelectEnclosingAction= new RetargetTextEditorAction(b, "StructureSelectEnclosing."); //$NON-NLS-1$
		fStructureSelectEnclosingAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_ENCLOSING);
		fStructureSelectNextAction= new RetargetTextEditorAction(b, "StructureSelectNext."); //$NON-NLS-1$
		fStructureSelectNextAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_NEXT);
		fStructureSelectPreviousAction= new RetargetTextEditorAction(b, "StructureSelectPrevious."); //$NON-NLS-1$
		fStructureSelectPreviousAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_PREVIOUS);
		fStructureSelectHistoryAction= new RetargetTextEditorAction(b, "StructureSelectHistory."); //$NON-NLS-1$
		fStructureSelectHistoryAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_LAST);
		
		fGotoNextMemberAction= new RetargetTextEditorAction(b, "GotoNextMember."); //$NON-NLS-1$
		fGotoNextMemberAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.GOTO_NEXT_MEMBER);
		fGotoPreviousMemberAction= new RetargetTextEditorAction(b, "GotoPreviousMember."); //$NON-NLS-1$
		fGotoPreviousMemberAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.GOTO_PREVIOUS_MEMBER);		

		fRemoveOccurrenceAnnotationsAction= new RetargetTextEditorAction(b, "RemoveOccurrenceAnnotations."); //$NON-NLS-1$
		fRemoveOccurrenceAnnotationsAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.REMOVE_OCCURRENCE_ANNOTATIONS);
	}
	
	protected final void markAsPartListener(RetargetAction action) {
		fPartListeners.add(action);
	}
	
	/*
	 * @see IEditorActionBarContributor#init(IActionBars, IWorkbenchPage)
	 */
	public void init(IActionBars bars, IWorkbenchPage page) {
		Iterator e= fPartListeners.iterator();
		while (e.hasNext()) 
			page.addPartListener((RetargetAction) e.next());
			
		super.init(bars, page);
		
		// register actions that have a dynamic editor. 
		bars.setGlobalActionHandler(ITextEditorActionDefinitionIds.GOTO_NEXT_ANNOTATION, fNextAnnotation);
		bars.setGlobalActionHandler(ITextEditorActionDefinitionIds.GOTO_PREVIOUS_ANNOTATION, fPreviousAnnotation);
		bars.setGlobalActionHandler(ITextEditorActionConstants.NEXT, fNextAnnotation);
		bars.setGlobalActionHandler(ITextEditorActionConstants.PREVIOUS, fPreviousAnnotation);
		bars.setGlobalActionHandler(ITextEditorActionDefinitionIds.TOGGLE_SHOW_SELECTED_ELEMENT_ONLY, fTogglePresentation);
		bars.setGlobalActionHandler(IJavaEditorActionDefinitionIds.TOGGLE_MARK_OCCURRENCES, fToggleMarkOccurrencesAction);
		
		bars.setGlobalActionHandler(JdtActionConstants.SHOW_JAVA_DOC, fShowJavaDoc);
	}
	
	/*
	 * @see org.eclipse.ui.part.EditorActionBarContributor#contributeToMenu(org.eclipse.jface.action.IMenuManager)
	 */
	public void contributeToMenu(IMenuManager menu) {
		
		super.contributeToMenu(menu);
		
		IMenuManager editMenu= menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
		if (editMenu != null) {

			editMenu.add(new Separator(IContextMenuConstants.GROUP_OPEN));			
			editMenu.add(new Separator(IContextMenuConstants.GROUP_GENERATE));
			editMenu.add(new Separator(IContextMenuConstants.GROUP_ADDITIONS));
			
			MenuManager structureSelection= new MenuManager(JavaEditorMessages.getString("ExpandSelectionMenu.label"), "expandSelection"); //$NON-NLS-1$ //$NON-NLS-2$ 
			structureSelection.add(fStructureSelectEnclosingAction);
			structureSelection.add(fStructureSelectNextAction);
			structureSelection.add(fStructureSelectPreviousAction);
			structureSelection.add(fStructureSelectHistoryAction);
			editMenu.appendToGroup(IContextMenuConstants.GROUP_OPEN, structureSelection);

//			final MenuManager folding= new MenuManager(JavaEditorMessages.getString("FoldingMenu.label"), "folding"); //$NON-NLS-1$ //$NON-NLS-2$
//			folding.setRemoveAllWhenShown(true);
//			folding.add(new Action("dummy") {}); //$NON-NLS-1$
//			folding.addMenuListener(new IMenuListener() {
//				public void menuAboutToShow(IMenuManager manager) {
//					IEditorPart part= getActiveEditorPart();
//					if (part instanceof JavaEditor) {
//						JavaEditor javaEditor= (JavaEditor)part;
//						FoldingActionGroup foldingActions= javaEditor.getFoldingActionGroup();
//						if (foldingActions != null)
//							foldingActions.fillMenu(manager);
//					}
//				}
//			});
//			editMenu.appendToGroup(IContextMenuConstants.GROUP_OPEN, folding);
			
			editMenu.appendToGroup(IContextMenuConstants.GROUP_GENERATE, fRetargetShowJavaDoc);
		}

		IMenuManager navigateMenu= menu.findMenuUsingPath(IWorkbenchActionConstants.M_NAVIGATE);
		if (navigateMenu != null) {
			navigateMenu.appendToGroup(IWorkbenchActionConstants.SHOW_EXT, fShowOutline);
			navigateMenu.appendToGroup(IWorkbenchActionConstants.SHOW_EXT, fOpenHierarchy);
		}
		
		IMenuManager gotoMenu= menu.findMenuUsingPath("navigate/goTo"); //$NON-NLS-1$
		if (gotoMenu != null) {
			gotoMenu.add(new Separator("additions2"));  //$NON-NLS-1$
			gotoMenu.appendToGroup("additions2", fGotoPreviousMemberAction); //$NON-NLS-1$
			gotoMenu.appendToGroup("additions2", fGotoNextMemberAction); //$NON-NLS-1$
			gotoMenu.appendToGroup("additions2", fGotoMatchingBracket); //$NON-NLS-1$
		}
	}
	
	/*
	 * @see EditorActionBarContributor#setActiveEditor(IEditorPart)
	 */
	public void setActiveEditor(IEditorPart part) {
		
		super.setActiveEditor(part);
		
		IActionBars actionBars= getActionBars();
		IStatusLineManager manager= actionBars.getStatusLineManager();
		manager.setMessage(null);
		manager.setErrorMessage(null);
		
		ITextEditor textEditor= null;
		if (part instanceof ITextEditor)
			textEditor= (ITextEditor) part;
			
		fTogglePresentation.setEditor(textEditor);
		fToggleMarkOccurrencesAction.setEditor(textEditor);
		fPreviousAnnotation.setEditor(textEditor);
		fNextAnnotation.setEditor(textEditor);
		
		fGotoMatchingBracket.setAction(getAction(textEditor, GotoMatchingBracketAction.GOTO_MATCHING_BRACKET));
		fShowJavaDoc.setAction(getAction(textEditor, "ShowJavaDoc")); //$NON-NLS-1$
		fShowOutline.setAction(getAction(textEditor, IJavaEditorActionDefinitionIds.SHOW_OUTLINE));
		fOpenHierarchy.setAction(getAction(textEditor, IJavaEditorActionDefinitionIds.OPEN_HIERARCHY));
		fOpenStructure.setAction(getAction(textEditor, IJavaEditorActionDefinitionIds.OPEN_STRUCTURE));
		
		fStructureSelectEnclosingAction.setAction(getAction(textEditor, StructureSelectionAction.ENCLOSING));
		fStructureSelectNextAction.setAction(getAction(textEditor, StructureSelectionAction.NEXT));
		fStructureSelectPreviousAction.setAction(getAction(textEditor, StructureSelectionAction.PREVIOUS));
		fStructureSelectHistoryAction.setAction(getAction(textEditor, StructureSelectionAction.HISTORY));
				
		fGotoNextMemberAction.setAction(getAction(textEditor, GoToNextPreviousMemberAction.NEXT_MEMBER));
		fGotoPreviousMemberAction.setAction(getAction(textEditor, GoToNextPreviousMemberAction.PREVIOUS_MEMBER));
		
		fRemoveOccurrenceAnnotationsAction.setAction(getAction(textEditor, "RemoveOccurrenceAnnotations")); //$NON-NLS-1$
		
		if (part instanceof JavaEditor) {
			JavaEditor javaEditor= (JavaEditor) part;
			javaEditor.getActionGroup().fillActionBars(getActionBars());
			FoldingActionGroup foldingActions= javaEditor.getFoldingActionGroup();
			if (foldingActions != null)
				foldingActions.updateActionBars();
		}
	}
	
	/*
	 * @see IEditorActionBarContributor#dispose()
	 */
	public void dispose() {
		
		Iterator e= fPartListeners.iterator();
		while (e.hasNext()) 
			getPage().removePartListener((RetargetAction) e.next());
		fPartListeners.clear();
		
		setActiveEditor(null);
		super.dispose();
	}
}
