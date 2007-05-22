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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.core.resources.ProjectScope;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.ui.dialogs.PreferencesUtil;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CleanUpPreferenceUtil;
import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.CleanUpPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpProfileVersioner;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CodeFormatingTabPage;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CodeStyleTabPage;
import org.eclipse.jdt.internal.ui.preferences.cleanup.MemberAccessesTabPage;
import org.eclipse.jdt.internal.ui.preferences.cleanup.MissingCodeTabPage;
import org.eclipse.jdt.internal.ui.preferences.cleanup.UnnecessaryCodeTabPage;
import org.eclipse.jdt.internal.ui.preferences.formatter.ModifyDialogTabPage;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileStore;
import org.eclipse.jdt.internal.ui.preferences.formatter.ModifyDialogTabPage.IModificationListener;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.CustomProfile;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.Profile;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;

import org.xml.sax.InputSource;

public class CleanUpRefactoringWizard extends RefactoringWizard {
	
	private static final String USE_CUSTOM_PROFILE_KEY= "org.eclipse.jdt.ui.cleanup.use_dialog_profile"; //$NON-NLS-1$
	private static final String CUSTOM_PROFILE_KEY= "org.eclipse.jdt.ui.cleanup.custom_profile"; //$NON-NLS-1$
	private static final String LAST_SELECTED_TAB_PAGE_INDEX= "org.eclipse.jdt.ui.cleanup.selected_tab_page"; //$NON-NLS-1$
	
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
	    		if (instancePreferences.get(CleanUpConstants.CLEANUP_PROFILE, null) != null) {
	    			workbenchProfileId= instancePreferences.get(CleanUpConstants.CLEANUP_PROFILE, null);
	    		} else {
	    			workbenchProfileId= CleanUpConstants.DEFAULT_PROFILE;
	    		}
	    		
				return getProjectProfileName((IJavaProject)element, fProfileIdsTable, workbenchProfileId);
			}
			return null;
		}
		
		private Hashtable loadProfiles() {
    		List list= CleanUpPreferenceUtil.loadProfiles(new InstanceScope());
    		
    		Hashtable profileIdsTable= new Hashtable();
    		for (Iterator iterator= list.iterator(); iterator.hasNext();) {
	            Profile profile= (Profile)iterator.next();
	            profileIdsTable.put(profile.getID(), profile);
            }
	     
    		return profileIdsTable;
        }

		private String getProjectProfileName(final IJavaProject project, Hashtable profileIdsTable, String workbenchProfileId) {
			ProjectScope projectScope= new ProjectScope(project.getProject());
	        IEclipsePreferences node= projectScope.getNode(JavaUI.ID_PLUGIN);
	        String id= node.get(CleanUpConstants.CLEANUP_PROFILE, null);
			if (id == null) {
	        	Profile profile= (Profile)profileIdsTable.get(workbenchProfileId);
		        if (profile != null) {
		        	return profile.getName();
		        } else {
		        	return MultiFixMessages.CleanUpRefactoringWizard_unknownProfile_Name;
		        }
	        } else {
		        Profile profile= (Profile)profileIdsTable.get(id);
		        if (profile != null) {
		        	return profile.getName();
		        } else {
		        	return Messages.format(MultiFixMessages.CleanUpRefactoringWizard_UnmanagedProfileWithName_Name, id.substring(ProfileManager.ID_PREFIX.length()));
		        }
	        }
        }

		public void reset() {
			fProfileIdsTable= null;
        }
	}
	
	private static class CleanUpConfigurationPage extends UserInputWizardPage implements IModificationListener {

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

		private static final String ENCODING= "UTF-8"; //$NON-NLS-1$

		private final CleanUpRefactoring fCleanUpRefactoring;
		private Map fCustomSettings;
		private SelectionButtonDialogField fUseCustomField;

		private ControlEnableState fEnableState;

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
        	boolean isCustom= getDialogSettings().getBoolean(USE_CUSTOM_PROFILE_KEY);
        	
        	final Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout(2, false));
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			composite.setFont(parent.getFont());
			
			SelectionButtonDialogField useProfile= new SelectionButtonDialogField(SWT.RADIO);
			useProfile.setLabelText(MultiFixMessages.CleanUpRefactoringWizard_use_configured_radio);
			useProfile.setSelection(!isCustom);
			useProfile.doFillIntoGrid(composite, 2);
			
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
			
			settingsField.doFillIntoGrid(composite, 3);
			GridData data= (GridData)settingsField.getListControl(null).getLayoutData();
			data.horizontalIndent= 15;
			data.grabExcessHorizontalSpace= true;
			
			data= (GridData)settingsField.getLabelControl(null).getLayoutData();
			data.exclude= true;
						
			settingsField.setElements(Arrays.asList(fCleanUpRefactoring.getProjects()));
			settingsField.selectFirstElement();
			
			fUseCustomField= new SelectionButtonDialogField(SWT.RADIO);
			fUseCustomField.setLabelText(MultiFixMessages.CleanUpRefactoringWizard_use_custom_radio);
			fUseCustomField.setSelection(isCustom);
			fUseCustomField.doFillIntoGrid(composite, 2);
			
			String settings= getDialogSettings().get(CUSTOM_PROFILE_KEY);
			if (settings == null) {
				fCustomSettings= CleanUpConstants.getEclipseDefaultSettings();
			} else {
				try {
	                fCustomSettings= decodeSettings(settings);
                } catch (CoreException e) {
	                JavaPlugin.log(e);
	                fCustomSettings= CleanUpConstants.getEclipseDefaultSettings();
                }
			}
			
			final TabFolder tabFolder= createTabFolder(composite, fCustomSettings);
			tabFolder.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					getDialogSettings().put(LAST_SELECTED_TAB_PAGE_INDEX, tabFolder.getSelectionIndex());
				}
			});
			
			settingsField.getListControl(null).setEnabled(!isCustom);
			if (isCustom) {
				fEnableState= ControlEnableState.disable(settingsField.getButtonBox(null));
			} else {
				fEnableState= ControlEnableState.disable(tabFolder);
			}
			
			fUseCustomField.setDialogFieldListener(new IDialogFieldListener() {
				public void dialogFieldChanged(DialogField field) {
					fEnableState.restore();
					boolean isCustomSelected= fUseCustomField.isSelected();
					settingsField.getListControl(null).setEnabled(!isCustomSelected);
					if (isCustomSelected) {
						fEnableState= ControlEnableState.disable(settingsField.getButtonBox(null));
					} else {
						fEnableState= ControlEnableState.disable(tabFolder);
					}
                }				
			});
			
			Link preferencePageLink= new Link(composite, SWT.WRAP);
			preferencePageLink.setText(MultiFixMessages.CleanUpRefactoringWizard_HideWizard_Link);
			preferencePageLink.setFont(parent.getFont());
			GridData gridData= new GridData(SWT.FILL, SWT.FILL, true, false);
			gridData.widthHint= convertWidthInCharsToPixels(300);
			gridData.horizontalSpan= 2;
			preferencePageLink.setLayoutData(gridData);
			preferencePageLink.addSelectionListener(new SelectionAdapter() {
				/**
				 * {@inheritDoc}
				 */
				public void widgetSelected(SelectionEvent e) {
					PreferencesUtil.createPreferenceDialogOn(composite.getShell(), CleanUpPreferencePage.PREF_ID, null, null).open();
				}
			});
			
			setControl(composite);
        }

		private TabFolder createTabFolder(Composite composite, Map settings) {
	        final TabFolder tabFolder = new TabFolder(composite, SWT.NONE);
			tabFolder.setFont(composite.getFont());
			GridData gridData= new GridData(SWT.FILL, SWT.TOP, true, false);
			gridData.horizontalIndent= 15;
			gridData.horizontalSpan= 2;
			tabFolder.setLayoutData(gridData);
			
			addTabPage(tabFolder, MultiFixMessages.CleanUpRefactoringWizard_code_style_tab, new CodeStyleTabPage(this, settings, false));
			addTabPage(tabFolder, MultiFixMessages.CleanUpRefactoringWizard_member_accesses_tab, new MemberAccessesTabPage(this, settings, false));
			addTabPage(tabFolder, MultiFixMessages.CleanUpRefactoringWizard_unnecessary_code_tab, new UnnecessaryCodeTabPage(this, settings, false));
			addTabPage(tabFolder, MultiFixMessages.CleanUpRefactoringWizard_missing_code_tab, new MissingCodeTabPage(this, settings, false));
			addTabPage(tabFolder, MultiFixMessages.CleanUpRefactoringWizard_code_organizing_tab, new CodeFormatingTabPage(this, settings, false));
			
			try {
	            int lastTabIndex= getDialogSettings().getInt(LAST_SELECTED_TAB_PAGE_INDEX);
	            tabFolder.setSelection(lastTabIndex);
            } catch (NumberFormatException e1) {
            }
            
	        return tabFolder;
        }

		protected final void addTabPage(TabFolder tabFolder, String title, ModifyDialogTabPage tabPage) {
    		final TabItem tabItem= new TabItem(tabFolder, SWT.NONE);
    		tabItem.setText(title);
    		tabItem.setData(tabPage);
    		tabItem.setControl(tabPage.createContents(tabFolder));
    	}
		
        protected boolean performFinish() {
			initializeRefactoring();
			storeSettings();
			return super.performFinish();
		}

		public IWizardPage getNextPage() {
			initializeRefactoring();
			storeSettings();
			return super.getNextPage();
		}
		
		private void storeSettings() {
			getDialogSettings().put(USE_CUSTOM_PROFILE_KEY, fUseCustomField.isSelected());
			try {
	            getDialogSettings().put(CUSTOM_PROFILE_KEY, encodeSettings(fCustomSettings));
            } catch (CoreException e) {
	            JavaPlugin.log(e);
            }
        }

		private void initializeRefactoring() {
			ICleanUp[] cleanups;
			if (fUseCustomField.isSelected()) {
				cleanups= CleanUpRefactoring.createCleanUps(fCustomSettings);
			} else {
				cleanups= CleanUpRefactoring.createCleanUps();
			}

			CleanUpRefactoring refactoring= (CleanUpRefactoring)getRefactoring();
			refactoring.clearCleanUps();
			for (int i= 0; i < cleanups.length; i++) {
	            refactoring.addCleanUp(cleanups[i]);
            }
        }
		
		public String encodeSettings(Map settings) throws CoreException {
			ByteArrayOutputStream stream= new ByteArrayOutputStream(2000);
			try {
				CleanUpProfileVersioner versioner= new CleanUpProfileVersioner();
				CustomProfile profile= new ProfileManager.CustomProfile("custom", settings, versioner.getCurrentVersion(), versioner.getProfileKind()); //$NON-NLS-1$
				ArrayList profiles= new ArrayList();
				profiles.add(profile);
				ProfileStore.writeProfilesToStream(profiles, stream, ENCODING, versioner);
				try {
					return stream.toString(ENCODING);
				} catch (UnsupportedEncodingException e) {
					return stream.toString(); 
				}
			} finally {
				try { stream.close(); } catch (IOException e) { /* ignore */ }
			}
		}
		
		public Map decodeSettings(String settings) throws CoreException {
			byte[] bytes;
			try {
				bytes= settings.getBytes(ENCODING);
			} catch (UnsupportedEncodingException e) {
				bytes= settings.getBytes();
			}
			InputStream is= new ByteArrayInputStream(bytes);
			try {
				List res= ProfileStore.readProfilesFromStream(new InputSource(is));
				if (res == null || res.size() == 0)
					return CleanUpConstants.getEclipseDefaultSettings();
				
				CustomProfile profile= (CustomProfile)res.get(0);
				new CleanUpProfileVersioner().update(profile);
				return profile.getSettings();
			} finally {
				try { is.close(); } catch (IOException e) { /* ignore */ }
			}
		}

		/**
         * {@inheritDoc}
         */
        public void updateStatus(IStatus status) {}

		/**
         * {@inheritDoc}
         */
        public void valuesModified() {}
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
