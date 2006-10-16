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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.core.resources.ProjectScope;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerComparator;

import org.eclipse.ui.dialogs.PreferencesUtil;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.CleanUpPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpProfileManager;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpProfileVersioner;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileStore;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.Profile;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;

public class CleanUpRefactoringWizard extends RefactoringWizard {
	
	private static class ProjectProfileLableProvider extends LabelProvider implements ITableLabelProvider {

		private Hashtable fProfileIdsTable;

		/**
		 * {@inheritDoc}
		 */
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		public String getColumnText(Object element, int columnIndex) {
			if (columnIndex == 0) {
				return ((IJavaProject)element).getProject().getName();
			} else if (columnIndex == 1) {
				
				if (fProfileIdsTable == null)
		    		fProfileIdsTable= loadProfiles();
				
				InstanceScope instanceScope= new InstanceScope();
	    		IEclipsePreferences instancePreferences= instanceScope.getNode(JavaUI.ID_PLUGIN);
	
	    		final String workbenchProfileId;
	    		if (instancePreferences.get(CleanUpProfileManager.PROFILE_KEY, null) != null) {
	    			workbenchProfileId= instancePreferences.get(CleanUpProfileManager.PROFILE_KEY, null);
	    		} else {
	    			workbenchProfileId= CleanUpProfileManager.DEFAULT_PROFILE;
	    		}
	    		
				return getProjectProfileName((IJavaProject)element, fProfileIdsTable, workbenchProfileId);
			}
			return null;
		}
		
		private Hashtable loadProfiles() {
			InstanceScope instanceScope= new InstanceScope();
			
	        CleanUpProfileVersioner versioner= new CleanUpProfileVersioner();
			ProfileStore profileStore= new ProfileStore(CleanUpConstants.CLEANUP_PROFILES, versioner);
			
    		List list= null;
            try {
	            list= profileStore.readProfiles(instanceScope);
            } catch (CoreException e1) {
	            JavaPlugin.log(e1);
            }
            if (list == null)
            	list= new ArrayList();
            
    		CleanUpProfileManager.addBuiltInProfiles(list, versioner);
    		
    		Hashtable profileIdsTable= new Hashtable();
    		for (Iterator iterator= list.iterator(); iterator.hasNext();) {
	            Profile profile= (Profile)iterator.next();
	            profileIdsTable.put(profile.getID(), profile);
            }
	     
    		return profileIdsTable;
        }
		
		private Profile getProjectProfile(final IJavaProject project, Hashtable profileIdsTable) {
			ProjectScope projectScope= new ProjectScope(project.getProject());
	        IEclipsePreferences node= projectScope.getNode(JavaUI.ID_PLUGIN);
	        if (node.get(CleanUpProfileManager.PROFILE_KEY, null) == null)
	        	return null;
	        
	        String id= node.get(CleanUpProfileManager.PROFILE_KEY, null);
	        return (Profile)profileIdsTable.get(id);
        }

		private String getProjectProfileName(final IJavaProject project, Hashtable profileIdsTable, String workbenchProfileId) {
	        Profile profile= getProjectProfile(project, profileIdsTable); 
	        if (profile == null)
	        	profile= (Profile)profileIdsTable.get(workbenchProfileId);
	        
	        String profileName;
	        if (profile != null) {
	        	profileName= profile.getName();
	        } else {
	        	profileName= MultiFixMessages.CleanUpRefactoringWizard_unknownProfile_Name;
	        }
	        return profileName;
        }

		public void reset() {
			fProfileIdsTable= null;
        }
	}
	
	private static class CleanUpConfigurationPage extends UserInputWizardPage {

		private static final class ProfileTableAdapter implements IListAdapter {
	        private final ProjectProfileLableProvider fProvider;
			private final Shell fShell;

	        private ProfileTableAdapter(ProjectProfileLableProvider provider, Shell shell) {
		        fProvider= provider;
				fShell= shell;
	        }

	        public void customButtonPressed(ListDialogField field, int index) {
	        	openPropertyDialog(field);
	        }

	        public void doubleClicked(ListDialogField field) {
				openPropertyDialog(field);
	        }
	        
	        private void openPropertyDialog(ListDialogField field) {
	            IJavaProject project= (IJavaProject)field.getSelectedElements().get(0);
	        	PreferencesUtil.createPropertyDialogOn(fShell, project, CleanUpPreferencePage.PROP_ID, null, null).open();
	        	List selectedElements= field.getSelectedElements();
	        	fProvider.reset();
	        	field.refresh();
	        	field.selectElements(new StructuredSelection(selectedElements));
            }

	        public void selectionChanged(ListDialogField field) {
	        	if (field.getSelectedElements().size() != 1) {
	        		field.enableButton(0, false);
	        	} else {
	        		field.enableButton(0, true);
	        	}
	        }
        }

		private final CleanUpRefactoring fCleanUpRefactoring;

		public CleanUpConfigurationPage(CleanUpRefactoring refactoring) {
			super(MultiFixMessages.CleanUpRefactoringWizard_CleanUpConfigurationPage_title);
			fCleanUpRefactoring= refactoring;
			ICompilationUnit[] cus= fCleanUpRefactoring.getCompilationUnits();
			IJavaProject[] projects= fCleanUpRefactoring.getProjects();
			if (cus.length == 1) {
				setMessage(MultiFixMessages.CleanUpRefactoringWizard_CleaningUp11_Title);
			} else if (projects.length == 1) {
				setMessage(Messages.format(MultiFixMessages.CleanUpRefactoringWizard_CleaningUpN1_Title, new Integer(cus.length)));
			} else {
				setMessage(Messages.format(MultiFixMessages.CleanUpRefactoringWizard_CleaningUpNN_Title, new Object[] {new Integer(cus.length), new Integer(projects.length)}));
			}
        }

		/**
         * {@inheritDoc}
         */
        public void createControl(Composite parent) {
        	Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout(2, false));
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			ProjectProfileLableProvider tableLabelProvider= new ProjectProfileLableProvider();
			IListAdapter listAdapter= new ProfileTableAdapter(tableLabelProvider, getShell());
			String[] buttons= new String[] {
				MultiFixMessages.CleanUpRefactoringWizard_Configure_Button
			};
			final ListDialogField settingsField= new ListDialogField(listAdapter, buttons, tableLabelProvider);
			
			String[] headerNames= new String[] {
					MultiFixMessages.CleanUpRefactoringWizard_Project_TableHeader, 
					MultiFixMessages.CleanUpRefactoringWizard_Profile_TableHeader
			};
			ColumnLayoutData[] columns = new ColumnLayoutData[] {
					new ColumnWeightData(1, 100, true),
					new ColumnWeightData(2, 20, true)
			};
			settingsField.setTableColumns(new ListDialogField.ColumnsDescription(columns , headerNames, true));
			settingsField.setViewerComparator(new ViewerComparator());
			settingsField.setLabelText(MultiFixMessages.CleanUpRefactoringWizard_ConfiguredProfiles_Label);
			
			settingsField.doFillIntoGrid(composite, 3);
			GridData data= (GridData)settingsField.getListControl(null).getLayoutData();
			data.grabExcessHorizontalSpace= true;
			
			data= (GridData)settingsField.getLabelControl(null).getLayoutData();
			data.horizontalSpan= 2;
						
			settingsField.setElements(Arrays.asList(fCleanUpRefactoring.getProjects()));
			settingsField.selectFirstElement();

    		final Text detailField= new Text(composite, SWT.BORDER | SWT.FLAT | SWT.MULTI | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL);
			detailField.setText(getCleanUpsInfo((IJavaProject)settingsField.getSelectedElements().get(0)));
    		data= new GridData(SWT.FILL, SWT.FILL, true, true);
    		detailField.setLayoutData(data);
    		settingsField.getTableViewer().addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					List selection= settingsField.getSelectedElements();
					if (selection.size() != 1) {
						detailField.setText(""); //$NON-NLS-1$
					} else {
						detailField.setText(getCleanUpsInfo((IJavaProject)selection.get(0)));
					}
                }
    		});
    				
			setControl(composite);
        }
        
        private String getCleanUpsInfo(IJavaProject project) {
        	StringBuffer result= new StringBuffer();
        	ICleanUp[] cleanUps= fCleanUpRefactoring.getCleanUps();
        	for (int i= 0; i < cleanUps.length; i++) {
        		ICleanUp cleanUp= cleanUps[i];
        		try {
	                cleanUp.checkPreConditions(project, new ICompilationUnit[] {}, new NullProgressMonitor());
	                String[] descriptions= cleanUp.getDescriptions();
	        		if (descriptions != null) {
	        			for (int j= 0; j < descriptions.length; j++) {
	        				result.append('-').append(' ').append(descriptions[j]).append('\n');
	        			}
	        		}
                } catch (CoreException e) {
	                JavaPlugin.log(e);
                }
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
