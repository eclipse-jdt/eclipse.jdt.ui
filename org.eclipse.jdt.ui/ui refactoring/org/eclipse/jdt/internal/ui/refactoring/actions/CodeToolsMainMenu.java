/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;

import org.eclipse.ui.IWorkbench;

import org.eclipse.jdt.internal.core.refactoring.base.IUndoManager;
import org.eclipse.jdt.internal.core.refactoring.base.IUndoManagerListener;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.undo.RedoRefactoringAction;
import org.eclipse.jdt.internal.ui.refactoring.undo.UndoRefactoringAction;

public class CodeToolsMainMenu {

	public static void addToMenuBar() {
		IWorkbench workbench= JavaPlugin.getDefault().getWorkbench();
		
		MenuManager menu= new MenuManager("#codeTools");
		menu.add(registerListener(new UndoRefactoringAction()));
		menu.add(registerListener(new RedoRefactoringAction()));		
	}
	
	private static Action registerListener(Action action) {
		if (action instanceof IUndoManagerListener) {
			IUndoManager undoManager= Refactoring.getUndoManager();
			undoManager.addListener((IUndoManagerListener)action);
		}
		return action;
	}
}
