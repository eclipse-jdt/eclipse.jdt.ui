/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;

import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextEvent;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * A popup dialog to request the user to fill the variables of the chosen template.
 */
public class TemplateEditor implements IDocumentListener, ModifyListener, VerifyKeyListener, PaintListener {

	private static Map fgActiveEditors= new HashMap();

	private TemplateContext fContext;
	private TemplateModel fModel;
	
	private StyledText fText;
	private Color fForeground;

	private int[] fIndices;
	private int fCurrentIndex;
	
	private String fOldText;
	private int fCaretDelta;

	public TemplateEditor(TemplateContext context, TemplateModel model) {
		fContext= context;
		fModel= model;
		fText= context.getViewer().getTextWidget();
		fForeground= fText.getDisplay().getSystemColor(SWT.COLOR_RED);
		
		fCurrentIndex= 0;
		fIndices= fModel.getEditableTexts();
	}
	
	public static boolean isEditorActive(ITextViewer viewer) {
		return fgActiveEditors.containsKey(viewer);
	}

	public void enter() {
		try {
			int offset= fContext.getStart();
			int length= fContext.getEnd() - offset;
	
			// backup old text
			IDocument document= fContext.getViewer().getDocument();
			fOldText= document.get(offset, length);
	
			// insert new text
			IRegion region= fContext.getViewer().getVisibleRegion();
			fText.replaceTextRange(offset - region.getOffset(),
				length, fModel.toString());
			fText.showSelection();
			
			if (fModel.getEditableCount() == 0)
				return;

			fText.addModifyListener(this);
			fContext.getViewer().getDocument().addDocumentListener(this);
			fText.addVerifyKeyListener(this);
			fText.addPaintListener(this);
			
			fgActiveEditors.put(fContext.getViewer(), this);
			
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
	}
	
	private void leave(boolean accept) {
		fgActiveEditors.remove(fContext.getViewer());

		fText.removePaintListener(this);
		fText.removeVerifyKeyListener(this);
		fContext.getViewer().getDocument().removeDocumentListener(this);
		fText.removeModifyListener(this);
		
		if (accept) {
			int start= fModel.getCaretOffset() + fContext.getStart();
			
			IRegion region= fContext.getViewer().getVisibleRegion();
			start -= region.getOffset();
			
			if (start <= region.getLength());
				fText.setSelection(start, start);

		} else {
			// restore old text
			IRegion region= fContext.getViewer().getVisibleRegion();
			fText.replaceTextRange(fContext.getStart() - region.getOffset(),
				fModel.getTotalSize(), fOldText);				
		}
		
		fText.redraw();
	}
	
	private void next() {
		if (fCurrentIndex == fIndices.length - 1) {
			leave(true);
		} else {
			redraw();
			
			fCurrentIndex++;
			int start= fModel.getOffset(fIndices[fCurrentIndex]) + fContext.getStart();
			int end= fModel.getSize(fIndices[fCurrentIndex]) + start;
			
			IRegion region= fContext.getViewer().getVisibleRegion();
			start -= region.getOffset();
			end -= region.getOffset();
			
			fText.setSelection(start, end);
			redraw();
		}		
	}
	
	private void previous() {
		if (fCurrentIndex == 0) {
			fText.getDisplay().beep();
		} else {
			redraw();
			fCurrentIndex--;
			redraw();
		}					
	}

	public void verifyKey(VerifyEvent event) {
		switch (event.character) {
		// [SHIFT-]TAB = hop between edit boxes
		case 0x09:
			{
				Point selection= fText.getSelection();
				IRegion region= fContext.getViewer().getVisibleRegion();
				int length= selection.y - selection.x;
				int offset= selection.x + region.getOffset() - fContext.getStart();
				
				// if tab was treated as a document change, would it exceed variable range?
				if (fModel.exceeds(fIndices[fCurrentIndex], offset, length)) {
					leave(true);
					return;
				}
			}
		
			if (event.stateMask == SWT.SHIFT) {
				previous();
			} else {	
				next();			
			}
			
			event.doit= false;
			break;

		// ESC = cancel
		case 0x1B:
			leave(false);
			event.doit= false;
			break;
		}
	}

	/*
	 * @see PaintListener#paintControl(PaintEvent)
	 */
	public void paintControl(PaintEvent event) {
		int offset= fModel.getOffset(fIndices[fCurrentIndex]) + fContext.getStart();
		int length= fModel.getSize(fIndices[fCurrentIndex]);

		highlight(event.gc, offset, length);
	}
	
	private void highlight(GC gc, int offset, int length) {
		IRegion region= fContext.getViewer().getVisibleRegion();
		offset -= region.getOffset();

		Point minLocation= fText.getLocationAtOffset(offset);
		Point maxLocation= fText.getLocationAtOffset(offset + length);

		int x= minLocation.x;
		int y= minLocation.y;
		int width= maxLocation.x - minLocation.x - 1;
		int height= gc.getFontMetrics().getHeight() - 1;
		
		gc.setForeground(fForeground);
		gc.drawRectangle(x, y, width, height);
	}

	private void redraw() {
		int offset= fModel.getOffset(fIndices[fCurrentIndex]) + fContext.getStart();
		int length= fModel.getSize(fIndices[fCurrentIndex]);
		
		IRegion region= fContext.getViewer().getVisibleRegion();
		offset -= region.getOffset();
		
		fText.redrawRange(offset, length, true);
	}

	/*
	 * @see IDocumentListener#documentAboutToBeChanged(DocumentEvent)
	 */
	public void documentAboutToBeChanged(DocumentEvent event) {
	}

	/*
	 * @see IDocumentListener#documentChanged(DocumentEvent)
	 */
	public void documentChanged(DocumentEvent event) {
		// check if document change exceeds variable range
		if (fModel.exceeds(fIndices[fCurrentIndex], event.getOffset() - fContext.getStart(), event.getLength())) {
			leave(true);
			return;
		}
		
		IDocument document= event.getDocument();

		// check if document change includes line delimiters
		String[] delimiters= document.getLegalLineDelimiters();
		for (int i= 0; i != delimiters.length; i++) {
			if (event.getText().indexOf(delimiters[i]) != -1) {
				leave(true);
				return;
			}
		}

		int deltaOffset= event.getOffset() - fContext.getStart() - fModel.getOffset(fIndices[fCurrentIndex]);
		int deltaLength= event.getText().length() - event.getLength();

		int size= fModel.getSize(fIndices[fCurrentIndex]);
		fModel.setSize(fIndices[fCurrentIndex], size + deltaLength);

		IRegion region= fContext.getViewer().getVisibleRegion();
		
		fContext.getViewer().getDocument().removeDocumentListener(this);
		fText.removeModifyListener(this);

		// sideeffect
		fCaretDelta= 0;

		for (int i= 0; i != fIndices.length; i++) {
			if (fModel.shareSameModel(fIndices[i], fIndices[fCurrentIndex])) {
				if (fIndices[fCurrentIndex] != fIndices[i]) {
					int offset= fModel.getOffset(fIndices[i]) + fContext.getStart() + deltaOffset;
					offset -= region.getOffset();

					fText.replaceTextRange(offset, event.getLength(), event.getText());
					fModel.setSize(fIndices[i], size + deltaLength);
					
					if (offset < fText.getCaretOffset())
						fCaretDelta += deltaLength;
				}
			}
		}		
		
		fText.addModifyListener(this);		
		fContext.getViewer().getDocument().addDocumentListener(this);
	
		redraw();
	}
	
	public int getSelectionStart() {
		if (fModel.getEditableCount() == 0)
			return fContext.getStart() + fModel.getCaretOffset();
		else
			return fContext.getStart() + fModel.getOffset(fIndices[fCurrentIndex]);
	}
	
	public int getSelectionEnd() {
		if (fModel.getEditableCount() == 0)
			return fContext.getStart() + fModel.getCaretOffset();
		else		
			return fContext.getStart() +
				fModel.getOffset(fIndices[fCurrentIndex]) +
				fModel.getSize(fIndices[fCurrentIndex]);
	}

	/*
	 * @see ModifyListener#modifyText(ModifyEvent)
	 */
	public void modifyText(ModifyEvent e) {
		// fix caret position
		fText.setCaretOffset(fText.getCaretOffset() + fCaretDelta);
	}

}
