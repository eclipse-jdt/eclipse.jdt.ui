package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.action.IAction;

import org.eclipse.ui.texteditor.IUpdate;

public interface IRefactoringAction extends IAction, IUpdate {

	public void update();
}
