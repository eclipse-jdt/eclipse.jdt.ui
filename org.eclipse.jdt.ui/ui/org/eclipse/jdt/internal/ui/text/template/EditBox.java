/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class EditBox {

	private TemplateContext fContext;	
	private TemplateModel fModel;
	private int fRangeIndex;

	private Composite fComposite; // XXX trim hardcoded margin of StyledText
	private SourceViewer fViewer;
	private StyledText fText;
		
	public EditBox(Composite parent, TemplateContext context, TemplateModel model, int rangeIndex) {
		fComposite= new Composite(parent, SWT.NONE);
		
		fViewer= createEditor(fComposite);

		fText= fViewer.getTextWidget();
		fText.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		fText.setEditable(true);
		fText.setText(model.getText(rangeIndex));
		
		fContext= context;
		fModel= model;
		fRangeIndex= rangeIndex;
	}
	
	public StyledText getText() {
		return fText;
	}
	
	public int getRangeIndex() {
		return fRangeIndex;
	}

	private static SourceViewer createEditor(Composite parent) {
		SourceViewer viewer= new SourceViewer(parent, null, SWT.SINGLE);
		JavaTextTools tools= JavaPlugin.getDefault().getJavaTextTools();
		viewer.configure(new JavaSourceViewerConfiguration(tools, null));
		viewer.setEditable(true);
		viewer.setDocument(new Document());

		Font font= JFaceResources.getFontRegistry().get(JFaceResources.TEXT_FONT);
		viewer.getTextWidget().setFont(font);		

		return viewer;
	}

	public void selectAll() {
		fText.setFocus();
		fText.setCaretOffset(0);
		fText.selectAll();
	}
	
	public void unselect() {
		fText.setSelection(0);	
	}

	public void updatePosition(StyledText text) {
		Point size= fText.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		fText.setSize(size);
		fText.setLocation(0, 0); // XXX trim hardcoded margin of StyledText
		
		// XXX trim hardcoded margin of StyledText
		size.x -= 2;
		size.y -= 2;
		fComposite.setSize(size);

		Point location= text.getLocationAtOffset(fModel.getOffset(fRangeIndex));
		fComposite.setLocation(location);				
	}	
}

