package org.eclipse.jdt.internal.corext.textmanipulation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;

import org.eclipse.jdt.internal.corext.Assert;

/* package */ abstract class Updater implements IDocumentListener {
	
	public static Updater createDoUpdater() {
		return new DoUpdater();
	}
	
	public static Updater createUndoUpdater() {
		return new UndoUpdater();
	}

	private static class DoUpdater extends Updater {
		private List fProcessedEdits= new ArrayList(10);
		public void setActiveNode(TextEdit edit) {
			if (fActiveEdit != null)
				fProcessedEdits.add(fActiveEdit);
			super.setActiveNode(edit);
		}	
		public void documentChanged(DocumentEvent event) {
			fActiveEdit.checkRange(event);
			int delta= getDelta(event);
			fActiveEdit.updateTextRange(delta, fProcessedEdits);
		}	
		public List getProcessedEdits() {
			return fProcessedEdits;
		}
	}
	
	private static class UndoUpdater extends Updater {
		public void documentChanged(DocumentEvent event) {
			// Do nothing
		}
		public List getProcessedEdits() {
			return null;
		}
	}
	
	protected TextEdit fActiveEdit;
	protected UndoMemento undo= new UndoMemento();
	
	public void documentAboutToBeChanged(DocumentEvent event) {
		int offset= event.getOffset();
		int currentLength= event.getLength();
		String currentText= null;
		try {
			currentText= event.getDocument().get(offset, currentLength);
		} catch (BadLocationException e) {
			Assert.isTrue(false, "Can't happen");
		}

		String newText= event.getText();
		int newLength= newText.length();

		if (currentLength > 0 && newLength == 0) {
			// Delete edit
			undo.add(SimpleTextEdit.createInsert(offset, currentText));
		} else if (currentLength == 0 && newLength > 0) {
			// insert edit
			undo.add(SimpleTextEdit.createDelete(offset, newLength));
		} else {
			// replace edit
			undo.add(SimpleTextEdit.createReplace(offset, newLength, currentText));
		}
	}
	
	public void setActiveNode(TextEdit edit) {
		fActiveEdit= edit;
	}
	
	public abstract List getProcessedEdits();
	
	private static int getDelta(DocumentEvent event) {
		return (event.getText() == null ? 0 : event.getText().length()) - event.getLength();
	}	
}
