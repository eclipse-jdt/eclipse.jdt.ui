/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.jdt.internal.corext.refactoring.tagging.IQualifiedNameUpdatingRefactoring;

public class QualifiedNameComponent extends Composite {

	private static final String PATTERNS= "patterns"; //$NON-NLS-1$
	private Text fPatterns;

	public QualifiedNameComponent(Composite parent, int style, final IQualifiedNameUpdatingRefactoring refactoring, IDialogSettings settings) {
		super(parent, style);
		GridLayout layout= new GridLayout();
		layout.marginWidth=0; layout.marginHeight= 0;
		layout.numColumns= 2;
		setLayout(layout);
		Label label= new Label(this, SWT.NONE);
		label.setText(RefactoringMessages.getString("QualifiedNameComponent.patterns.label")); //$NON-NLS-1$
		fPatterns= new Text(this, SWT.BORDER);
		fPatterns.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		label= new Label(this, SWT.NONE);
		label.setText(RefactoringMessages.getString("QualifiedNameComponent.patterns.description"));  //$NON-NLS-1$
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan=2;
		label.setLayoutData(gd);
		String text= refactoring.getFilePatterns();
		if (text == null) 
			text= settings.get(PATTERNS);
		if (text != null) { 
			fPatterns.setText(text);
			refactoring.setFilePatterns(text);
		}
		fPatterns.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				refactoring.setFilePatterns(fPatterns.getText());
			}
		});
	}
	
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		Control[] children= getChildren();
		for (int i= 0; i < children.length; i++) {
			children[i].setEnabled(enabled);
		}
	}
	
	public void savePatterns(IDialogSettings settings) {
		settings.put(PATTERNS, fPatterns.getText());
	}
}
