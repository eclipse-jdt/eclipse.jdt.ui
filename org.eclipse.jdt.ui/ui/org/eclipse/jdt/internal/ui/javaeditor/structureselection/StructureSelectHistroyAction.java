package org.eclipse.jdt.internal.ui.javaeditor.structureselection;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.Assert;

import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.core.ISourceRange;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditorMessages;

public class StructureSelectHistroyAction extends Action implements IUpdate {
	private CompilationUnitEditor fEditor;
	private SelectionHistory fHistory;
	
	public StructureSelectHistroyAction(CompilationUnitEditor editor, SelectionHistory history) {
		super(JavaEditorMessages.getString("StructureSelectHistory.label"));
		setToolTipText(JavaEditorMessages.getString("StructureSelectHistory.tooltip"));
		setDescription(JavaEditorMessages.getString("StructureSelectHistory.description"));
		Assert.isNotNull(history);
		Assert.isNotNull(editor);
		fHistory= history;
		fEditor= editor;
		update();
	}
	
	public void update() {
		setEnabled(!fHistory.isEmpty());
	}
	
	public void run() {
		ISourceRange old= fHistory.getLast();
		if (old != null) {
			try {
				fHistory.ignoreSelectionChanges();
				fEditor.selectAndReveal(old.getOffset(), old.getLength());
			} finally {
				fHistory.listenToSelectionChanges();
			}
		}
	}
}
