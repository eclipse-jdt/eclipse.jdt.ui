/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
 package org.eclipse.jdt.internal.ui.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jdt.internal.corext.util.TypeInfo;

public class TypeSelectionComponent extends Composite {
	
	private Text fFilter;
	private TypeInfoViewer2 fViewer;
	
	public TypeSelectionComponent(Composite parent, int style, String message) {
		super(parent, style);
		createContent(message);
	}
	
	public TypeInfo[] getSelection() {
		return fViewer.getSelection();
	}
	
	public void close() {
		fViewer.stop();
	}
	
	private void createContent(String message) {
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginWidth= 0; layout.marginHeight= 0;
		setLayout(layout);
		Label label= new Label(this, SWT.NONE);
		label.setText(message);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= 2;
		label.setLayoutData(gd);
		fFilter= new Text(this, SWT.BORDER | SWT.FLAT);
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= 2;
		fFilter.setLayoutData(gd);
		fFilter.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				patternChanged((Text)e.widget);
			}
		});
		fFilter.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {
			}
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN) {
					fViewer.setFocus();
				}
			}
		});
		label= new Label(this, SWT.NONE);
		label.setText("&Matching types and history:");
		label= new Label(this, SWT.RIGHT);
		gd= new GridData(GridData.FILL_HORIZONTAL);
		label.setLayoutData(gd);
		fViewer= new TypeInfoViewer2(this, label);
		gd= new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan= 2;
		fViewer.getTable().setLayoutData(gd);
	}

	public void addSelectionListener(SelectionListener listener) {
		fViewer.getTable().addSelectionListener(listener);
	}
	
	private void patternChanged(Text text) {
		fViewer.setSearchPattern(text.getText());
	}
}