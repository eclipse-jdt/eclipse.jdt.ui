/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.IDialogConstants;

import org.eclipse.ui.dialogs.PreferencesUtil;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.CleanUpPreferencePage;

public class CleanUpRefactoringWizard extends RefactoringWizard {
	
	private static class CleanUpConfigurationPage extends UserInputWizardPage {

		private final CleanUpRefactoring fCleanUpRefactoring;

		public CleanUpConfigurationPage(CleanUpRefactoring refactoring) {
			super(MultiFixMessages.CleanUpRefactoringWizard_CleanUpConfigurationPage_title);
			fCleanUpRefactoring= refactoring;
			setMessage(MultiFixMessages.CleanUpRefactoringWizard_CleanUpConfigurationPage_message);
        }

		/**
         * {@inheritDoc}
         */
        public void createControl(Composite parent) {
        	Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout(1, true));
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			Link link= new Link(composite, SWT.WRAP | SWT.RIGHT);
    		link.setText(MultiFixMessages.CleanUpRefactoringWizard_CleanUpConfigurationPage_link); 
    		link.setToolTipText(MultiFixMessages.CleanUpRefactoringWizard_CleanUpConfigurationPage_linkToolTip); 
    		GridData gridData= new GridData(GridData.FILL, GridData.CENTER, true, false);
    		gridData.widthHint= convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
    		link.setLayoutData(gridData);
    		link.setFont(composite.getFont());
    		
    		final Label label = new Label(composite, SWT.WRAP);
    		label.setFont(composite.getFont());
    		label.setText(MultiFixMessages.CleanUpRefactoringWizard_CleanUpConfigurationPage_detaileTitle);
    		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
    		
    		final Text detailField= new Text(composite, SWT.BORDER | SWT.FLAT | SWT.MULTI | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL);
    		detailField.setText(getCleanUpsInfo());
    		final GridData data= new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
    		data.heightHint= convertHeightInCharsToPixels(20);
    		detailField.setLayoutData(data);
    		
    		link.addSelectionListener(new SelectionAdapter() {
    			public void widgetSelected(SelectionEvent e) {
    				IJavaProject[] projects= fCleanUpRefactoring.getProjects();
    				if (projects.length == 1) {
    					PreferencesUtil.createPropertyDialogOn(getShell(), projects[0], CleanUpPreferencePage.PROP_ID, null, null).open();
    				} else {
    					PreferencesUtil.createPreferenceDialogOn(getShell(), CleanUpPreferencePage.PREF_ID, null, null).open();	
    				}
    				detailField.setText(getCleanUpsInfo());	
    			}
    		});
    			
			setControl(composite);
        }

        private String getCleanUpsInfo() {
        	StringBuffer result= new StringBuffer();
        	
        	IJavaProject[] projects= fCleanUpRefactoring.getProjects();
        	for (int p= 0; p < projects.length; p++) {
	            result.append(Messages.format(MultiFixMessages.CleanUpRefactoringWizard_CleanUpConfigurationPage_SettingsForProjectX, projects[p].getElementName())).append('\n');
	            ICleanUp[] cleanUps= fCleanUpRefactoring.getCleanUps();
	        	for (int i= 0; i < cleanUps.length; i++) {
		            ICleanUp cleanUp= cleanUps[i];
		            if (cleanUp instanceof AbstractCleanUp)
		            	((AbstractCleanUp)cleanUp).loadSettings(projects[p]);
		            
					String[] descriptions= cleanUp.getDescriptions();
		            if (descriptions != null) {
	    	            for (int j= 0; j < descriptions.length; j++) {
	    	                result.append('\t').append('-').append(' ').append(descriptions[j]).append('\n');
	                    }
		            }
	            }
	        	result.append('\n');
            }
        	
        	
	        return result.toString();
        }
	}
	
	public CleanUpRefactoringWizard(CleanUpRefactoring refactoring, int flags) {
		super(refactoring, flags);
		setDefaultPageTitle(MultiFixMessages.CleanUpRefactoringWizard_PageTitle);
		setWindowTitle(MultiFixMessages.CleanUpRefactoringWizard_WindowTitle);
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_CLEAN_UP);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.ui.refactoring.RefactoringWizard#addUserInputPages()
	 */
	protected void addUserInputPages() {
		addPage(new CleanUpConfigurationPage((CleanUpRefactoring)getRefactoring()));
	}

}
