/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.link;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ExtendedModifyEvent;
import org.eclipse.swt.custom.ExtendedModifyListener;
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

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * A user interface for <code>LinkedPositionManager</code>, using <code>ITextViewer</code>.
 */
public class LinkedPositionUI implements LinkedPositionListener,
	ModifyListener, VerifyKeyListener, PaintListener {

	/**
	 * A listener for notification when the user cancelled the edit operation.
	 */
	public interface CancelListener {
		void performCancel();
	}

	private static final String CARET_POSITION= "LinkedPositionUI.caret.position";

	private static final IPositionUpdater fgUpdater= new DefaultPositionUpdater(CARET_POSITION);
	
	private final ITextViewer fViewer;
	private final LinkedPositionManager fManager;	
	private final Color fFrameColor;

	private int fFinalCaretOffset= -1; // no final caret offset

	private Position fFramePosition;
	private int fCaretOffset;;
	
	private CancelListener fCancelListener;
	
	/**
	 * XXX StyledText workaround.
	 */
	public void setReplace(IDocumentExtension.IReplace replace) {
		Display display= fViewer.getTextWidget().getDisplay();
		display.asyncExec((Runnable) replace);
	}

	/**
	 * Creates a user interface for <code>LinkedPositionManager</code>.
	 * 
	 * @param viewer  the text viewer.
	 * @param manager the <code>LinkedPositionManager</code> managing a <code>IDocument</code> of the <code>ITextViewer</code>.
	 */
	public LinkedPositionUI(ITextViewer viewer, LinkedPositionManager manager) {
		Assert.isNotNull(viewer);
		Assert.isNotNull(manager);
		
		fViewer= viewer;
		fManager= manager;
		
		fManager.setLinkedPositionListener(this);
		fFrameColor= viewer.getTextWidget().getDisplay().getSystemColor(SWT.COLOR_RED);				
	}
	
	/**
	 * Sets the final position of the caret when the linked mode is exited
	 * successfully by leaving the last linked position using TAB.
	 */
	public void setFinalCaretOffset(int offset) {
		fFinalCaretOffset= offset;	
	}

	/**
	 * Sets a <code>CancelListener</code> which is notified if the linked mode
	 * is exited unsuccessfully by hitting ESC.
	 */
	public void setCancelListener(CancelListener listener) {
		fCancelListener= listener;
	}

	/*
	 * @see LinkedPositionManager.LinkedPositionListener#setCurrentPositions(Position, int)
	 */
	public void setCurrentPosition(Position position, int caretOffset) {
		if (!fFramePosition.equals(position)) {
			redrawRegion();
			fFramePosition= position;
		}

		fCaretOffset= caretOffset;
	}

	/*
	 * @see LinkedPositionManager.LinkedPositionListener#exit(boolean)
	 */
	public void exit(boolean success) {
		leave2(success);	
	}

	/**
	 * Enters the linked mode. The linked mode can be left by calling
	 * <code>exit</code>.
	 * 
	 * @see exit(boolean)
	 */
	public void enter() {
		// track final caret
		IDocument document= fViewer.getDocument();
		document.addPositionCategory(CARET_POSITION);
		document.addPositionUpdater(fgUpdater);
		try {
			if (fFinalCaretOffset != -1)
				document.addPosition(CARET_POSITION, new Position(fFinalCaretOffset));
		} catch (BadLocationException e) { // ignore
		} catch (BadPositionCategoryException e) {
			JavaPlugin.log(e);
			openErrorDialog(fViewer.getTextWidget().getShell(), e);
		}
		
		StyledText text= fViewer.getTextWidget();			
		text.addModifyListener(this);
		text.addVerifyKeyListener(this);
		text.addPaintListener(this);
		text.showSelection();

		fFramePosition= fManager.getFirstPosition();
		if (fFramePosition == null)
			leave(true);

		// XXX workaround:
		// turn off template editing if visible region
		// is smaller than the whole document
		if (true) {
			IRegion region= fViewer.getVisibleRegion();
			if ((region.getOffset() != 0) || (region.getLength() != document.getLength()))
				leave(true);
		}
	}

	private void leave(boolean accept) {
		fManager.uninstall(accept);
		
		leave2(accept);		
	}
	
	private void leave2(boolean accept) {
		StyledText text= fViewer.getTextWidget();	
		text.removePaintListener(this);
		text.removeVerifyKeyListener(this);
		text.removeModifyListener(this);

		try {
			IRegion region= fViewer.getVisibleRegion();
			IDocument document= fViewer.getDocument();

			if (accept) {
				Position[] positions= document.getPositions(CARET_POSITION);

				if ((positions != null) && (positions.length != 0)) {
					int offset= positions[0].getOffset() - region.getOffset();		
					if ((offset >= 0) && (offset <= region.getLength()))
						text.setSelection(offset, offset);
				}
			}

			document.removePositionUpdater(fgUpdater);
			document.removePositionCategory(CARET_POSITION);
			
			if (!accept && (fCancelListener != null))
				fCancelListener.performCancel();

		} catch (BadPositionCategoryException e) {
			JavaPlugin.log(e);
			Assert.isTrue(false);
		}
		
		text.redraw();
	}

	private void next() {
		redrawRegion();
		
		fFramePosition= fManager.getNextPosition(fFramePosition.getOffset());
		if (fFramePosition == null) {
			leave(true);
		} else {
			selectRegion();
			redrawRegion();
		}
	}
	
	private void previous() {
		redrawRegion();
		
		fFramePosition= fManager.getPreviousPosition(fFramePosition.getOffset());
		if (fFramePosition == null) {
			fViewer.getTextWidget().getDisplay().beep();
		} else {
			selectRegion();
			redrawRegion();
		}				
	}

	/*
	 * @see VerifyKeyListener#verifyKey(VerifyEvent)
	 */
	public void verifyKey(VerifyEvent event) {
		switch (event.character) {
		// [SHIFT-]TAB = hop between edit boxes
		case 0x09:
			{
				Point selection= fViewer.getTextWidget().getSelection();
				IRegion region= fViewer.getVisibleRegion();
				int offset= selection.x + region.getOffset();
				int length= selection.y - selection.x;
				
				// if tab was treated as a document change, would it exceed variable range?
				if (exceeds(fFramePosition, offset, length)) {
					leave(true);
					return;
				}
			}
		
			if (event.stateMask == SWT.SHIFT)
				previous();
			else 
				next();			
			
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
		GC gc= event.gc;

		IRegion region= fViewer.getVisibleRegion();		
		int offset= fFramePosition.getOffset() -  region.getOffset();
		int length= fFramePosition.getLength();
			
		StyledText text= fViewer.getTextWidget();
		Point minLocation= text.getLocationAtOffset(offset);
		Point maxLocation= text.getLocationAtOffset(offset + length);

		int x= minLocation.x;
		int y= minLocation.y;
		int width= maxLocation.x - minLocation.x - 1;
		int height= gc.getFontMetrics().getHeight() - 1;
		
		gc.setForeground(fFrameColor);
		gc.drawLine(x, y + height, x + width, y + height);
	}

	private void redrawRegion() {
		IRegion region= fViewer.getVisibleRegion();		
		int offset= fFramePosition.getOffset() -  region.getOffset();
		int length= fFramePosition.getLength();
		
		fViewer.getTextWidget().redrawRange(offset, length, true);
	}

	private void selectRegion() {
		IRegion region= fViewer.getVisibleRegion();
		int start= fFramePosition.getOffset() - region.getOffset();
		int end= fFramePosition.getLength() + start;	

		fViewer.getTextWidget().setSelection(start, end);		
	}

	private void updateCaret() {
		IRegion region= fViewer.getVisibleRegion();		
		int offset= fFramePosition.getOffset() + fCaretOffset - region.getOffset();
		
		if ((offset >= 0) && (offset <= region.getLength()))	
			fViewer.getTextWidget().setCaretOffset(offset);
	}

	/*
	 * @see ModifyListener#modifyText(ModifyEvent)
	 */	 
	public void modifyText(ModifyEvent e) {
		// XXX workaround
		redrawRegion();
		updateCaret();
	}

	private static boolean exceeds(Position position, int offset, int length) {
		return
			(offset < position.getOffset()) ||
			(offset + length > position.getOffset() + position.getLength());
	}
	
	/**
	 * Returns the cursor selection, after having entered the linked mode.
	 * <code>enter()</code> must be called prior to a call to this method.
	 */
	public IRegion getSelectedRegion() {
		if (fFramePosition == null)
			return new Region(fFinalCaretOffset, 0);
		else
			return new Region(fFramePosition.getOffset(), fFramePosition.getLength());
	}

	private static void openErrorDialog(Shell shell, Exception e) {
		MessageDialog.openError(shell, LinkedPositionMessages.getString("LinkedPositionUI.error.title"), e.getMessage()); //$NON-NLS-1$
	}

}
