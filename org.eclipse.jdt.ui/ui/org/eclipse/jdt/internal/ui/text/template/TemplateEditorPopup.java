/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.TypedEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * A popup dialog to request the user to fill the variables of the chosen template.
 */
public class TemplateEditorPopup implements ModifyListener, VerifyKeyListener, ControlListener, FocusListener {

	private static final int BORDER_WIDTH= 1;

	private TemplateContext fContext;
	private TemplateModel fModel;

	private Shell fShell;
	private Composite fComposite;
	private SourceViewer fViewer;
	private EditBox[] fEditBoxes;
	
	private boolean fResult;

	public TemplateEditorPopup(TemplateContext context, TemplateModel model) {
		fContext= context;
		fModel= model;
	}
	
	private void create() {		
		Control control= fContext.getViewer().getTextWidget();
		control.getShell().addControlListener(this);		

		Display display= control.getDisplay();

		// XXX SWT.ON_TOP fobids focus to be gained
		fShell= new Shell(control.getShell(), /*SWT.ON_TOP |*/ SWT.NO_TRIM | SWT.APPLICATION_MODAL);
		fShell.setBackground(display.getSystemColor(SWT.COLOR_RED));
		
		fComposite= new Composite(fShell, SWT.NONE);
		
		int[] indices= fModel.getEditableTexts();
		int count= indices.length;

		fEditBoxes= new EditBox[count];
		
		for (int i= 0; i != count; i++) {
			fEditBoxes[i]= new EditBox(fComposite, fContext, fModel, indices[i]);
			fEditBoxes[i].getText().addFocusListener(this);
		}

		fViewer= createViewer(fComposite);
		fViewer.getTextWidget().setText(fModel.toString());
	}

	private static SourceViewer createViewer(Composite parent) {
		SourceViewer viewer= new SourceViewer(parent, null, SWT.NONE);
		JavaTextTools tools= JavaPlugin.getDefault().getJavaTextTools();
		viewer.configure(new JavaSourceViewerConfiguration(tools, null));
		viewer.setEditable(false);
		viewer.setDocument(new Document());

		Font font= JFaceResources.getFontRegistry().get(JFaceResources.TEXT_FONT);
		viewer.getTextWidget().setFont(font);		

		return viewer;
	}	
	
	private void show() {
		updateSize();
		updatePosition();		

		fShell.open();
	}
	
	public void dispose() {
		if ((fShell != null) && !fShell.isDisposed()) {
			fShell.dispose();

			Control control= fContext.getViewer().getTextWidget();
			control.getShell().removeControlListener(this);
		}

		fShell= null;
	}
	
	public boolean open() {	
		create();
		show();
		
		Display display= fShell.getDisplay();
		
		// XXX flush pending events to win race for gaining focus
		while (display.readAndDispatch());
		fEditBoxes[0].selectAll();				
		
		while ((fShell != null) && !fShell.isDisposed())
			if (!display.readAndDispatch())
				display.sleep();
				
		return fResult;	
	}
	
	private int findEditBoxIndex(TypedEvent e) {
		StyledText text= (StyledText) e.widget;

		for (int i= 0; i != fEditBoxes.length; i++) 
			if (fEditBoxes[i].getText().equals(text))
				return i;
				
		return -1;
	}
	
	private EditBox getEditBox(TypedEvent e) {
		int index= findEditBoxIndex(e);		
		return fEditBoxes[index];
	}

	public void modifyText(ModifyEvent e) {
		EditBox box= getEditBox(e);		
		String modifiedText= box.getText().getText();

		int rangeIndex= box.getRangeIndex();		

		// update model
		fModel.setText(rangeIndex, box.getText().getText());

		// update editable and non-editable text
		for (int i= 0; i != fEditBoxes.length; i++) {
			if (fModel.shareSameModel(fEditBoxes[i].getRangeIndex(), rangeIndex)) {
				// update edit boxes
				if (box.equals(fEditBoxes[i])) {
					// XXX kludge (computeSize does not work as expected)
					StyledText text= fEditBoxes[i].getText();

					int offset= text.getCaretOffset();
					Point selection= text.getSelection();			
					text.removeModifyListener(this);

					text.setText(modifiedText);
					
					text.addModifyListener(this);
					text.setCaretOffset(offset);
					text.setSelection(selection);
				
				} else {
					fEditBoxes[i].getText().setText(modifiedText);
				}			
			}
		}		
		
		fViewer.getTextWidget().setText(fModel.toString());
		
		updateSize();
		updatePosition();
	}

	public void verifyKey(VerifyEvent e) {
		switch (e.character) {
		// [SHIFT-]TAB = hop between edit boxes
		case 0x09:
			int index= findEditBoxIndex(e); 
			
			if (e.stateMask == SWT.SHIFT) { // does not work
				// previous
				if (index == 0) {
					fShell.getDisplay().beep();
				} else {
					fEditBoxes[index].unselect();
					fEditBoxes[index - 1].selectAll();
				}			
			} else {	
				// next
				if (index == fEditBoxes.length - 1) {
					fShell.getDisplay().beep();
				} else {
					fEditBoxes[index].unselect();
					fEditBoxes[index + 1].selectAll();
				}			
			}
			e.doit= false;
			break;

		// ENTER = accept
		case 0x0D:
			dispose();
			fResult= true;
			e.doit= false;
			break;

		// ESC = cancel
		case 0x1B:
			dispose();
			fResult= false;
			e.doit= false;
			break;
		}
	}
	
	public void controlMoved(ControlEvent e) {
		updatePosition();
	}
	
	public void controlResized(ControlEvent e) {
		updatePosition();		
	}	
	
	public void focusGained(FocusEvent e) {
		EditBox box= getEditBox(e);
		box.getText().addModifyListener(this);
		box.getText().addVerifyKeyListener(this);
	}

	public void focusLost(FocusEvent e) {
		EditBox box= getEditBox(e);
		box.getText().removeModifyListener(this);
		box.getText().removeVerifyKeyListener(this);
		
		box.unselect();
	}
	
	private void updatePosition() {
		if ((fShell == null) || fShell.isDisposed())
			return;
		
		StyledText text= fContext.getViewer().getTextWidget();
		Point location= text.getLocationAtOffset(fContext.getStart()); // XXX IAB 5123
		location= text.toDisplay(location);
		
		// XXX bidi safe?
		location.x -= BORDER_WIDTH;
		location.y -= BORDER_WIDTH;

		Point size= fShell.getSize();

		Display display= fShell.getDisplay();
		Rectangle rectangle= display.getBounds();
				
		if (location.x > rectangle.width - size.x)
			location.x = rectangle.width - size.x;

		if (location.y > rectangle.height - size.y)
			location.y = rectangle.height - size.y;
		
		fShell.setLocation(location.x, location.y);	
	}
	
	private void updateSize() {
		if ((fShell == null) || fShell.isDisposed())
			return;
		
		StyledText text= fViewer.getTextWidget();
		Point size= text.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		text.setSize(size);
		
		fComposite.setSize(size);
		fComposite.setLocation(BORDER_WIDTH, BORDER_WIDTH);
		
		size.x += BORDER_WIDTH * 2;
		size.y += BORDER_WIDTH * 2;
		fShell.setSize(size);
		text.setLocation(0, 0);
		
		for (int i= 0; i != fEditBoxes.length; i++)
			fEditBoxes[i].updatePosition(fViewer.getTextWidget());
	}		
	
	public String getText() {
		return fModel.toString();
	}
	
	public int[] getSelection() {
		return fModel.getSelection();
	}

}
