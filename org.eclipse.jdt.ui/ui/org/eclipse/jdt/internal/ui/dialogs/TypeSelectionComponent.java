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
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.util.TypeInfo;

import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

public class TypeSelectionComponent extends Composite {
	
	private Text fFilter;
	private String fInitialFilterText;
	private TypeInfoViewer fViewer;
	
	public TypeSelectionComponent(Composite parent, int style, String message, boolean multi, IJavaSearchScope scope, int elementKind, String initialFilter) {
		super(parent, style);
		setFont(parent.getFont());
		Assert.isNotNull(scope);
		fInitialFilterText= initialFilter;
		createContent(message, multi, scope, elementKind);
	}
	
	public TypeInfo[] getSelection() {
		return fViewer.getSelection();
	}
	
	private void createContent(String message, boolean multi, IJavaSearchScope scope, int elementKind) {
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginWidth= 0; layout.marginHeight= 0;
		setLayout(layout);
		Font font= getFont();
		Label label= new Label(this, SWT.NONE);
		label.setText(message);
		label.setFont(font);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= 2;
		label.setLayoutData(gd);
		fFilter= new Text(this, SWT.BORDER | SWT.FLAT);
		fFilter.setFont(font);
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
		label.setFont(font);
		label.setText(JavaUIMessages.TypeSelectionComponent_label);
		label= new Label(this, SWT.RIGHT);
		label.setFont(font);
		gd= new GridData(GridData.FILL_HORIZONTAL);
		label.setLayoutData(gd);
		fViewer= new TypeInfoViewer(this, multi ? SWT.MULTI : SWT.NONE, label, scope, elementKind, fInitialFilterText);
		gd= new GridData(GridData.FILL_BOTH);
		PixelConverter converter= new PixelConverter(fViewer.getTable());
		gd.widthHint= converter.convertWidthInCharsToPixels(70);
		gd.heightHint= SWTUtil.getTableHeightHint(fViewer.getTable(), 10);
		gd.horizontalSpan= 2;
		fViewer.getTable().setLayoutData(gd);
	}

	public void addSelectionListener(SelectionListener listener) {
		fViewer.getTable().addSelectionListener(listener);
	}
	
	public void populate() {
		if (fInitialFilterText != null) {
			fFilter.setText(fInitialFilterText);
			fFilter.setSelection(0, fInitialFilterText.length());
		}
		fViewer.reset();
		fFilter.setFocus();
	}
	
	private void patternChanged(Text text) {
		fViewer.setSearchPattern(text.getText());
	}
}