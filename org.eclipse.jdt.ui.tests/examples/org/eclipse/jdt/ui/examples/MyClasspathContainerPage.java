/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.examples;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.core.runtime.Path;

import org.eclipse.jface.wizard.WizardPage;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;

public class MyClasspathContainerPage extends WizardPage implements IClasspathContainerPage {

	private IClasspathEntry fEntry;

	public MyClasspathContainerPage() {
		super("MyClasspathContainerPage");
		setTitle("My Example Container");
	}

	public void createControl(Composite parent) {
		Label label= new Label(parent, SWT.NONE);
		if (fEntry == null) {
			label.setText("Nothing to configure. Press 'Finish' to add new entry");
		} else {
			label.setText("Nothing to configure.");
			setPageComplete(false);
		}
		setControl(label);
	}

	public boolean finish() {
		if (fEntry == null) { // new entry
			fEntry= JavaCore.newContainerEntry(new Path("org.eclipse.jdt.EXAMPLE_CONTAINER"));
		}
		return true;
	}

	public IClasspathEntry getSelection() {
		return fEntry;
	}

	public void setSelection(IClasspathEntry containerEntry) {
		fEntry= containerEntry;
	}

}
