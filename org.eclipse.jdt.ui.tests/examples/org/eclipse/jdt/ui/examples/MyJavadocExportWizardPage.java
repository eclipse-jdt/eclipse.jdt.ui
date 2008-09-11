/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.examples;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jdt.ui.wizards.JavadocExportWizardPage;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;

/* Adds a page to the Javadoc export wizard

   <extension
         point="org.eclipse.jdt.ui.javadocExportWizardPage">
      <javadocExportWizardPage
            description="My Javadoc Export Wizard Page"
            class="org.eclipse.jdt.ui.examples.MyJavadocExportWizardPage"
            id="org.eclipse.jdt.EXAMPLE_JD_EXPORT_WP">
      </javadocExportWizardPage>
   </extension>

*/
public class MyJavadocExportWizardPage extends JavadocExportWizardPage {

	private Text fText;
	private Text fText2;
	private Button fButton;
	private Label fLabel;
	private Label fLabel2;

	public MyJavadocExportWizardPage() {
	}

	public Control createContents(Composite parent) {
		ModifyListener modifyListener= new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validateInputs();
			}
		};

		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(new GridLayout(2, false));

		fButton= new Button(composite, SWT.CHECK);
		fButton.setLayoutData(new GridData(SWT.LEAD, SWT.TOP, false, false, 2, 1));
		fButton.setText("Use taglet");
		fButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				validateInputs();
			}

			public void widgetSelected(SelectionEvent e) {
				validateInputs();
			}
		});
		fButton.setSelection(false);


		fLabel= new Label(composite, SWT.NONE);
		fLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		fLabel.setText("Tag:");
		fLabel.setEnabled(false);

		fText= new Text(composite, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		fText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		fText.setText("");
		fText.addModifyListener(modifyListener);
		fText.setEnabled(false);

		fLabel2= new Label(composite, SWT.NONE);
		fLabel2.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		fLabel2.setText("Description:");
		fLabel2.setEnabled(false);

		fText2= new Text(composite, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		fText2.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		fText2.setText("");
		fText2.addModifyListener(modifyListener);
		fText2.setEnabled(false);

		return composite;
	}

	protected void validateInputs() {
		boolean isEnabled= fButton.getSelection();

		fLabel.setEnabled(isEnabled);
		fText.setEnabled(isEnabled);
		fLabel2.setEnabled(isEnabled);
		fText2.setEnabled(isEnabled);

		StatusInfo status= new StatusInfo();

		if (isEnabled) {
			String text= fText.getText().trim();
			if (text.length() == 0) {
				status.setError("Enter a tag");
			}

			String text2= fText2.getText().trim();
			if (text2.length() == 0) {
				status.setError("Enter a description");
			}
		}
		setStatus(status);
	}

	public void updateArguments(List vmOptions, List toolOptions) {
		if (fButton.getSelection()) {
			String tag= fText.getText().trim();
			String description= fText2.getText().trim();

			toolOptions.add(0, "-tag");
			toolOptions.add(1, tag + ":a:"+ description); // do not quote here, Javadoc wizard will quote if necessary
		}
	}

	public void updateAntScript(Element javadocXMLElement) {
		if (fButton.getSelection()) {
			String tag= fText.getText().trim();
			String description= fText2.getText().trim();

			Document document= javadocXMLElement.getOwnerDocument();

			Element tagElement= document.createElement("tag");
			tagElement.setAttribute("name", tag);
			tagElement.setAttribute("description", description);

			javadocXMLElement.appendChild(tagElement);
		}
	}

}
