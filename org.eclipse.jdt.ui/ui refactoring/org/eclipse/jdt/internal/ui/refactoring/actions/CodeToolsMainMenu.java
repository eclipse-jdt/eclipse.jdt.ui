/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;

import org.eclipse.ui.IWorkbench;

import org.eclipse.jdt.core.refactoring.IUndoManager;
import org.eclipse.jdt.core.refactoring.IUndoManagerListener;
import org.eclipse.jdt.core.refactoring.Refactoring;

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
