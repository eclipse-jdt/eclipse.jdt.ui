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
package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.CustomProfile;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.Profile;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.util.SWTUtil;



/**
 * The code formatter preference page. 
 */

public class CodeFormatterConfigurationBlock {
    
    private static final String DIALOGSTORE_LASTLOADPATH= JavaUI.ID_PLUGIN + ".codeformatter.loadpath"; //$NON-NLS-1$
	private static final String DIALOGSTORE_LASTSAVEPATH= JavaUI.ID_PLUGIN + ".codeformatter.savepath"; //$NON-NLS-1$
	
	
	private class StoreUpdater implements Observer {
		
		public StoreUpdater() {
			fProfileManager.addObserver(this);
		}

		public void update(Observable o, Object arg) {
			final int value= ((Integer)arg).intValue();
			switch (value) {
			case ProfileManager.PROFILE_DELETED_EVENT:
			case ProfileManager.PROFILE_RENAMED_EVENT:
			case ProfileManager.PROFILE_CREATED_EVENT:
			case ProfileManager.SETTINGS_CHANGED_EVENT:
				try {
					ProfileStore.writeProfiles(fProfileManager.getSortedProfiles());
				} catch (CoreException x) {
					JavaPlugin.log(x);
				}
			}
		}
	}
	
	

	private class ProfileComboController implements Observer, SelectionListener {
		
		private final List fSortedProfiles;
		
		public ProfileComboController() {
			fSortedProfiles= fProfileManager.getSortedProfiles();
			fProfileCombo.addSelectionListener(this);
			fProfileManager.addObserver(this);
			updateProfiles();
			updateSelection();
		}
		
		public void widgetSelected(SelectionEvent e) {
			final int index= fProfileCombo.getSelectionIndex();
			fProfileManager.setSelected((Profile)fSortedProfiles.get(index));
		}

		public void widgetDefaultSelected(SelectionEvent e) {}

		public void update(Observable o, Object arg) {
			if (arg == null) return;
			final int value= ((Integer)arg).intValue();
			switch (value) {
				case ProfileManager.PROFILE_CREATED_EVENT:
				case ProfileManager.PROFILE_DELETED_EVENT:
				case ProfileManager.PROFILE_RENAMED_EVENT:
					updateProfiles();
				case ProfileManager.SELECTION_CHANGED_EVENT:
					updateSelection();
			}
		}
		
		private void updateProfiles() {
			fProfileCombo.setItems(fProfileManager.getSortedNames());
		}

		private void updateSelection() {
			fProfileCombo.setText(fProfileManager.getSelected().getName());
		}
	}
	
	private class ButtonController implements Observer, SelectionListener {
		
		public ButtonController() {
			fProfileManager.addObserver(this);
			fNewButton.addSelectionListener(this);
			fRenameButton.addSelectionListener(this);
			fEditButton.addSelectionListener(this);
			fDeleteButton.addSelectionListener(this);
			fSaveButton.addSelectionListener(this);
			fLoadButton.addSelectionListener(this);
			update(fProfileManager, null);
		}

		public void update(Observable o, Object arg) {
			final boolean state= ((ProfileManager)o).getSelected() instanceof CustomProfile;
			fEditButton.setText(state ? FormatterMessages.getString("CodingStyleConfigurationBlock.edit_button.desc")  //$NON-NLS-1$
			    : FormatterMessages.getString("CodingStyleConfigurationBlock.show_button.desc")); //$NON-NLS-1$
			fDeleteButton.setEnabled(state);
			fSaveButton.setEnabled(state);
			fRenameButton.setEnabled(state);
		}

		public void widgetSelected(SelectionEvent e) {
			final Button button= (Button)e.widget;
			if (button == fSaveButton)
				saveButtonPressed();
			else if (button == fEditButton)
				modifyButtonPressed();
			else if (button == fDeleteButton) 
				deleteButtonPressed();
			else if (button == fNewButton)
				newButtonPressed();
			else if (button == fLoadButton)
				loadButtonPressed();
			else if (button == fRenameButton) 
				renameButtonPressed();
		}
		
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		
		private void renameButtonPressed() {
			if (!(fProfileManager.getSelected() instanceof CustomProfile)) return;
			final CustomProfile profile= (CustomProfile)fProfileManager.getSelected();
			final RenameProfileDialog renameDialog= new RenameProfileDialog(fComposite.getShell(), profile, fProfileManager);
			renameDialog.open();
		}
		
		private void modifyButtonPressed() {
			final ModifyDialog modifyDialog= new ModifyDialog(fComposite.getShell(), fProfileManager.getSelected(), fProfileManager, false);
			modifyDialog.open();
		}
		
		private void deleteButtonPressed() {
			if (MessageDialog.openQuestion(
				fComposite.getShell(), 
				FormatterMessages.getString("CodingStyleConfigurationBlock.delete_confirmation.title"), //$NON-NLS-1$
				FormatterMessages.getFormattedString("CodingStyleConfigurationBlock.delete_confirmation.question", fProfileManager.getSelected().getName()))) { //$NON-NLS-1$
				fProfileManager.deleteSelected();
			}
		}
		
		private void newButtonPressed() {
			final CreateProfileDialog p= new CreateProfileDialog(fComposite.getShell(), fProfileManager);
			if (p.open() != Window.OK) 
				return;
			if (!p.openEditDialog()) 
				return;
			final ModifyDialog modifyDialog= new ModifyDialog(fComposite.getShell(), p.getCreatedProfile(), fProfileManager, true);
			modifyDialog.open();
		}
		
		private void saveButtonPressed() {
			final FileDialog dialog= new FileDialog(fComposite.getShell(), SWT.SAVE);
			dialog.setText(FormatterMessages.getString("CodingStyleConfigurationBlock.save_profile.dialog.title")); //$NON-NLS-1$
			dialog.setFilterExtensions(new String [] {"*.xml"}); //$NON-NLS-1$
			
			final String lastPath= JavaPlugin.getDefault().getDialogSettings().get(DIALOGSTORE_LASTSAVEPATH);
			if (lastPath != null) {
				dialog.setFilterPath(lastPath);
			}
			final String path= dialog.open();
			if (path == null) 
				return;
			
			JavaPlugin.getDefault().getDialogSettings().put(DIALOGSTORE_LASTSAVEPATH, dialog.getFilterPath());
			
			final File file= new File(path);
			if (file.exists() && !MessageDialog.openQuestion(fComposite.getShell(), FormatterMessages.getString("CodingStyleConfigurationBlock.save_profile.overwrite.title"), FormatterMessages.getFormattedString("CodingStyleConfigurationBlock.save_profile.overwrite.message", path))) { //$NON-NLS-1$ //$NON-NLS-2$
				return;
			}
			
			final Collection profiles= new ArrayList();
			profiles.add(fProfileManager.getSelected());
			try {
				ProfileStore.writeProfilesToFile(profiles, file);
			} catch (CoreException e) {
				final String title= FormatterMessages.getString("CodingStyleConfigurationBlock.save_profile.error.title"); //$NON-NLS-1$
				final String message= FormatterMessages.getString("CodingStyleConfigurationBlock.save_profile.error.message"); //$NON-NLS-1$
				ExceptionHandler.handle(e, fComposite.getShell(), title, message);
			}
		}
		
		private void loadButtonPressed() {
			final FileDialog dialog= new FileDialog(fComposite.getShell(), SWT.OPEN);
			dialog.setText(FormatterMessages.getString("CodingStyleConfigurationBlock.load_profile.dialog.title")); //$NON-NLS-1$
			dialog.setFilterExtensions(new String [] {"*.xml"}); //$NON-NLS-1$
			final String lastPath= JavaPlugin.getDefault().getDialogSettings().get(DIALOGSTORE_LASTLOADPATH);
			if (lastPath != null) {
				dialog.setFilterPath(lastPath);
			}
			final String path= dialog.open();
			if (path == null) 
				return;
			JavaPlugin.getDefault().getDialogSettings().put(DIALOGSTORE_LASTLOADPATH, dialog.getFilterPath());
			
			final File file= new File(path);
			Collection profiles= null;
			try {
				profiles= ProfileStore.readProfilesFromFile(file);
			} catch (CoreException e) {
				final String title= FormatterMessages.getString("CodingStyleConfigurationBlock.load_profile.error.title"); //$NON-NLS-1$
				final String message= FormatterMessages.getString("CodingStyleConfigurationBlock.load_profile.error.message"); //$NON-NLS-1$
				ExceptionHandler.handle(e, fComposite.getShell(), title, message);
			}
			if (profiles == null || profiles.isEmpty())
				return;
			
			final CustomProfile profile= (CustomProfile)profiles.iterator().next();
			
			if (ProfileVersioner.getVersionStatus(profile) > 0) {
				final String title= FormatterMessages.getString("CodingStyleConfigurationBlock.load_profile.error_too_new.title"); //$NON-NLS-1$
				final String message= FormatterMessages.getString("CodingStyleConfigurationBlock.load_profile.error_too_new.message"); //$NON-NLS-1$
			    MessageDialog.openWarning(fComposite.getShell(), title, message);
			}
			
			if (fProfileManager.containsName(profile.getName())) {
				final AlreadyExistsDialog aeDialog= new AlreadyExistsDialog(fComposite.getShell(), profile, fProfileManager);
				if (aeDialog.open() != Window.OK) 
					return;
			}
			ProfileVersioner.updateAndComplete(profile);
			fProfileManager.addProfile(profile);
		}
	}
	
	private class PreviewController implements Observer {

		public PreviewController() {
			fProfileManager.addObserver(this);
			fJavaPreview.setWorkingValues(fProfileManager.getSelected().getSettings());
			fJavaPreview.update();
		}
		
		public void update(Observable o, Object arg) {
			final int value= ((Integer)arg).intValue();
			switch (value) {
				case ProfileManager.PROFILE_CREATED_EVENT:
				case ProfileManager.PROFILE_DELETED_EVENT:
				case ProfileManager.SELECTION_CHANGED_EVENT:
				case ProfileManager.SETTINGS_CHANGED_EVENT:
					fJavaPreview.setWorkingValues(((ProfileManager)o).getSelected().getSettings());
					fJavaPreview.update();
			}
		}
		
	}

	
	/**
	 * Some Java source code used for preview.
	 */
	private final static String PREVIEW=
		"/**\n* " + //$NON-NLS-1$
		FormatterMessages.getString("CodingStyleConfigurationBlock.preview.title") + //$NON-NLS-1$
		"\n*/\n\n" + //$NON-NLS-1$
		"package mypackage; import java.util.LinkedList; public class MyIntStack {" + //$NON-NLS-1$
		"private final LinkedList fStack;" + //$NON-NLS-1$
		"public MyIntStack(){fStack= new LinkedList();}" + //$NON-NLS-1$
		"public int pop(){return ((Integer)fStack.removeFirst()).intValue();}" + //$NON-NLS-1$
		"public void push(int elem){fStack.addFirst(new Integer(elem));}" + //$NON-NLS-1$
		"public boolean isEmpty() {return fStack.isEmpty();}" + //$NON-NLS-1$
		"}"; //$NON-NLS-1$
	

	/**
	 * The GUI controls
	 */
	protected Composite fComposite;
	protected Combo fProfileCombo;
	protected Button fEditButton;
	protected Button fRenameButton;
	protected Button fDeleteButton;
	protected Button fNewButton;
	protected Button fLoadButton;
	protected Button fSaveButton;
	
	/**
	 * The ProfileManager, the model of this page.
	 */
	protected final ProfileManager fProfileManager;
	
	/**
	 * The JavaPreview.
	 */
	protected CompilationUnitPreview fJavaPreview;
	private PixelConverter fPixConv;

	private IScopeContext fCurrContext;
	
	/**
	 * Create a new <code>CodeFormatterConfigurationBlock</code>.
	 */
	public CodeFormatterConfigurationBlock(IProject project) {
		List profiles= null;
		try {
		    profiles= ProfileStore.readProfiles();
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
		
		if (profiles == null) 
		    profiles= new ArrayList();
		
		if (project != null) {
			fCurrContext= new ProjectScope(project);
		} else {
			fCurrContext= new InstanceScope();
		}
		
		fProfileManager= new ProfileManager(profiles, fCurrContext);

		new StoreUpdater();
	}

	/**
	 * Create the contents
	 * @param parent Parent composite
	 * @return Created control
	 */
	public Composite createContents(Composite parent) {

		final int numColumns = 5;
		
		fPixConv = new PixelConverter(parent);
		fComposite = createComposite(parent, numColumns, false);

		fProfileCombo= createProfileCombo(fComposite, numColumns - 3, fPixConv.convertWidthInCharsToPixels(20));
		fEditButton= createButton(fComposite, FormatterMessages.getString("CodingStyleConfigurationBlock.edit_button.desc"), GridData.HORIZONTAL_ALIGN_BEGINNING); //$NON-NLS-1$
		fRenameButton= createButton(fComposite, FormatterMessages.getString("CodingStyleConfigurationBlock.rename_button.desc"), GridData.HORIZONTAL_ALIGN_BEGINNING); //$NON-NLS-1$
		fDeleteButton= createButton(fComposite, FormatterMessages.getString("CodingStyleConfigurationBlock.remove_button.desc"), GridData.HORIZONTAL_ALIGN_BEGINNING); //$NON-NLS-1$

		final Composite group= createComposite(fComposite, 4, false);
		final GridData groupData= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		groupData.horizontalSpan= numColumns;
		group.setLayoutData(groupData);

		fNewButton= createButton(group, FormatterMessages.getString("CodingStyleConfigurationBlock.new_button.desc"), GridData.HORIZONTAL_ALIGN_BEGINNING); //$NON-NLS-1$
		((GridData)createLabel(group, "", 1).getLayoutData()).grabExcessHorizontalSpace= true; //$NON-NLS-1$
		fLoadButton= createButton(group, FormatterMessages.getString("CodingStyleConfigurationBlock.load_button.desc"), GridData.HORIZONTAL_ALIGN_END); //$NON-NLS-1$
		fSaveButton= createButton(group, FormatterMessages.getString("CodingStyleConfigurationBlock.save_button.desc"), GridData.HORIZONTAL_ALIGN_END); //$NON-NLS-1$

		createLabel(fComposite, FormatterMessages.getString("CodingStyleConfigurationBlock.preview_label.text"), numColumns); //$NON-NLS-1$
		configurePreview(fComposite, numColumns);
		
		new ButtonController();
		new ProfileComboController();
		new PreviewController();
		
		return fComposite;
	}

	
	private static Button createButton(Composite composite, String text, final int style) {
		final Button button= new Button(composite, SWT.PUSH);
		button.setText(text);

		final GridData gd= new GridData(style);
		gd.widthHint= SWTUtil.getButtonWidthHint(button);
		button.setLayoutData(gd);
		return button;
	}
	
	private static Combo createProfileCombo(Composite composite, int span, int widthHint) {
		final GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = span;
		gd.widthHint= widthHint;

		final Combo combo= new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY );
		combo.setLayoutData(gd);
		return combo;
	}
	
	private Label createLabel(Composite composite, String text, int numColumns) {
		final GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = numColumns;
		gd.widthHint= 0;

		final Label label = new Label(composite, SWT.WRAP);
		label.setText(text);
		label.setLayoutData(gd);
		return label;		
	}
	
	private Composite createComposite(Composite parent, int numColumns, boolean margins) {
		final Composite composite = new Composite(parent, SWT.NONE);
		final GridLayout layout = new GridLayout(numColumns, false);
		if (margins) {
			layout.marginHeight= fPixConv.convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
			layout.marginWidth= fPixConv.convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		} else {
			layout.marginHeight = 0;
			layout.marginWidth = 0;
		}
		layout.horizontalSpacing= fPixConv.convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		layout.verticalSpacing= fPixConv.convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		composite.setLayout(layout);
		return composite;
	}
	
	private void configurePreview(Composite composite, int numColumns) {
		fJavaPreview= new CompilationUnitPreview(fProfileManager.getSelected().getSettings(), composite);
		fJavaPreview.setPreviewText(PREVIEW);
	    
		final GridData gd = new GridData(GridData.FILL_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = numColumns;
		gd.verticalSpan= 7;
		gd.widthHint = 0;
		gd.heightHint = 0;
		fJavaPreview.getControl().setLayoutData(gd);
	}

	public final boolean hasProjectSpecificOptions() {
		if (fCurrContext.getName() != ProjectScope.SCOPE) {
			return false;
		}
		return fProfileManager.hasProjectSpecificSettings(fCurrContext);
	}
	
	public boolean performOk(boolean enabled) {
		if (enabled) {
			fProfileManager.commitChanges(fCurrContext);
		} else {
			fProfileManager.clearAllSettings(fCurrContext);
		}
		return true;
	}
	
	public void performDefaults() {
		Profile profile= fProfileManager.getProfile(ProfileManager.JAVA_PROFILE);
		if (profile != null) {
			int defaultIndex= fProfileManager.getSortedProfiles().indexOf(profile);
			if (defaultIndex != -1) {
				fProfileCombo.select(defaultIndex);
				fProfileManager.setSelected(profile);
			}
		}
	}
	
	public void dispose() {
	}
	

}
