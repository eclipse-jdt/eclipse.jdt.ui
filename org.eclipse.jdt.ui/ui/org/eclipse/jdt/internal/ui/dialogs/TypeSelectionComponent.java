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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.jdt.internal.corext.util.TypeInfo;

import org.eclipse.jdt.internal.ui.JavaUIMessages;

public class TypeSelectionComponent extends Composite {
	
	private Text fFilter;
	private TypeInfoViewer fViewer;
	private IDialogSettings fSettings;
	
	public TypeSelectionComponent(Composite parent, int style, IDialogSettings settings) {
		super(parent, style);
		fSettings= settings;
		createContent();
	}
	
	public TypeInfo[] getSelection() {
		return fViewer.getSelection();
	}
	
	public void stop() {
		fViewer.stop();
	}
	
	private void createContent() {
		GridLayout layout= new GridLayout();
		layout.numColumns= 1;
		layout.marginWidth= 0; layout.marginHeight= 0;
		setLayout(layout);
		Label label= new Label(this, SWT.NONE);
		label.setText(JavaUIMessages.getString("TypeSelectionDialog.upperLabel")); //$NON-NLS-1$
		fFilter= new Text(this, SWT.BORDER | SWT.FLAT);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
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
					fViewer.getTable().setFocus();
				}
			}
		});
		fViewer= new TypeInfoViewer(this, fSettings);
		gd= new GridData(GridData.FILL_BOTH);
		fViewer.getTable().setLayoutData(gd);
	}
	
	private void patternChanged(Text text) {
		fViewer.setSearchPattern(text.getText());
	}
}