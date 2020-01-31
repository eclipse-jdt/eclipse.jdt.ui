/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jsp;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


public class JavaFamilyExamplePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	@Override
	protected Control createContents(Composite parent) {
		Composite c= new Composite(parent, SWT.NULL);
		//c.setLayout(new FillLayout());

		final Button b= new Button(c, SWT.NULL);
		b.setText(getButtonLabel());
		b.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				JspUIPlugin.getDefault().controlJSPIndexing(!JspUIPlugin.getDefault().isJSPIndexingOn());
				b.setText(getButtonLabel());
			}
		});
		b.pack();

		return c;
	}

	private String getButtonLabel() {
		if (JspUIPlugin.getDefault().isJSPIndexingOn())
			return "Stop JSP Indexing"; //$NON-NLS-1$
		return "Start JSP Indexing"; //$NON-NLS-1$
	}

	@Override
	public void init(IWorkbench workbench) {
		// empty implementation
	}
}
