/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.folding.IJavaFoldingPreferenceBlock;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.OverlayPreferenceStore.OverlayKey;
import org.eclipse.jdt.internal.ui.text.folding.JavaFoldingStructureProviderDescriptor;
import org.eclipse.jdt.internal.ui.text.folding.JavaFoldingStructureProviderRegistry;
import org.eclipse.jdt.internal.ui.util.SWTUtil;


/**
 * Configures Java Editor folding preferences.
 *
 * @since 3.0
 */
class FoldingConfigurationBlock implements IPreferenceConfigurationBlock {

	private static class ErrorPreferences implements IJavaFoldingPreferenceBlock {
		private String fMessage;

		protected ErrorPreferences(String message) {
			fMessage= message;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.text.folding.IJavaFoldingPreferences#createControl(org.eclipse.swt.widgets.Group)
		 */
		@Override
		public Control createControl(Composite composite) {
			Composite inner= new Composite(composite, SWT.NONE);
			inner.setLayout(new FillLayout(SWT.VERTICAL));

			Label label= new Label(inner, SWT.CENTER);
			label.setText(fMessage);

			return inner;
		}

		@Override
		public void initialize() {
		}

		@Override
		public void performOk() {
		}

		@Override
		public void performDefaults() {
		}

		@Override
		public void dispose() {
		}

	}

	/** The overlay preference store. */
	private final OverlayPreferenceStore fStore;

	/* The controls */
	private Combo fProviderCombo;
	private Button fFoldingCheckbox;
	private ComboViewer fProviderViewer;
	private Composite fGroup;
	private StackLayout fStackLayout;

	/* the model */
	private final Map<String, JavaFoldingStructureProviderDescriptor> fProviderDescriptors;
	private final Map<String, IJavaFoldingPreferenceBlock> fProviderPreferences;
	private final Map<String, Control> fProviderControls;


	public FoldingConfigurationBlock(OverlayPreferenceStore store) {
		Assert.isNotNull(store);
		fStore= store;
		fStore.addKeys(createOverlayStoreKeys());
		fProviderDescriptors= createListModel();
		fProviderPreferences= new HashMap<>();
		fProviderControls= new HashMap<>();
	}

	private Map<String, JavaFoldingStructureProviderDescriptor> createListModel() {
		JavaFoldingStructureProviderRegistry reg= JavaPlugin.getDefault().getFoldingStructureProviderRegistry();
		reg.reloadExtensions();
		JavaFoldingStructureProviderDescriptor[] descs= reg.getFoldingProviderDescriptors();
		Map<String, JavaFoldingStructureProviderDescriptor> map= new HashMap<>();
		for (JavaFoldingStructureProviderDescriptor desc : descs) {
			map.put(desc.getId(), desc);
		}
		return map;
	}

	private OverlayPreferenceStore.OverlayKey[] createOverlayStoreKeys() {

		ArrayList<OverlayKey> overlayKeys= new ArrayList<>();

		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_FOLDING_ENABLED));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_FOLDING_PROVIDER));

		OverlayPreferenceStore.OverlayKey[] keys= new OverlayPreferenceStore.OverlayKey[overlayKeys.size()];
		overlayKeys.toArray(keys);
		return keys;
	}

	/**
	 * Creates page for folding preferences.
	 *
	 * @param parent the parent composite
	 * @return the control for the preference page
	 */
	@Override
	public Control createControl(Composite parent) {

		Composite composite= new Composite(parent, SWT.NULL);
		// assume parent page uses griddata
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_CENTER | GridData.VERTICAL_ALIGN_FILL);
		composite.setLayoutData(gd);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		PixelConverter pc= new PixelConverter(composite);
		layout.verticalSpacing= pc.convertHeightInCharsToPixels(1) / 2;
		composite.setLayout(layout);


		/* check box for new editors */
		fFoldingCheckbox= new Button(composite, SWT.CHECK);
		fFoldingCheckbox.setText(PreferencesMessages.FoldingConfigurationBlock_enable);
		gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING);
		fFoldingCheckbox.setLayoutData(gd);
		fFoldingCheckbox.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean enabled= fFoldingCheckbox.getSelection();
				fStore.setValue(PreferenceConstants.EDITOR_FOLDING_ENABLED, enabled);
				updateCheckboxDependencies();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		Label label= new Label(composite, SWT.CENTER);
		gd= new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		label.setLayoutData(gd);

		if (fProviderDescriptors.size() > 1) {
			/* list */
			Composite comboComp= new Composite(composite, SWT.NONE);
			gd= new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
			GridLayout gridLayout= new GridLayout(2, false);
			gridLayout.marginWidth= 0;
			comboComp.setLayout(gridLayout);

			Label comboLabel= new Label(comboComp, SWT.CENTER);
			gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_CENTER);
			comboLabel.setLayoutData(gd);
			comboLabel.setText(PreferencesMessages.FoldingConfigurationBlock_combo_caption);

			label= new Label(composite, SWT.CENTER);
			gd= new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
			label.setLayoutData(gd);

			fProviderCombo= new Combo(comboComp, SWT.READ_ONLY | SWT.DROP_DOWN);
			SWTUtil.setDefaultVisibleItemCount(fProviderCombo);
			gd= new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_CENTER);
			fProviderCombo.setLayoutData(gd);

			fProviderViewer= createProviderViewer();
		}

		Composite groupComp= new Composite(composite, SWT.NONE);
		gd= new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan= 2;
		groupComp.setLayoutData(gd);
		GridLayout gridLayout= new GridLayout(1, false);
		gridLayout.marginWidth= 0;
		groupComp.setLayout(gridLayout);

		/* contributed provider preferences. */
		fGroup= new Composite(groupComp, SWT.NONE);
		gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING);
		fGroup.setLayoutData(gd);
		fStackLayout= new StackLayout();
		fGroup.setLayout(fStackLayout);

		return composite;
	}

	private ComboViewer createProviderViewer() {
		/* list viewer */
		ComboViewer viewer= new ComboViewer(fProviderCombo);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(new LabelProvider() {
			/*
			 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
			 */
			@Override
			public Image getImage(Object element) {
				return null;
			}

			/*
			 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
			 */
			@Override
			public String getText(Object element) {
				return ((JavaFoldingStructureProviderDescriptor) element).getName();
			}
		});
		viewer.addSelectionChangedListener(event -> {
			IStructuredSelection sel= (IStructuredSelection) event.getSelection();
			if (!sel.isEmpty()) {
				fStore.setValue(PreferenceConstants.EDITOR_FOLDING_PROVIDER, ((JavaFoldingStructureProviderDescriptor) sel.getFirstElement()).getId());
				updateListDependencies();
			}
		});
		viewer.setInput(fProviderDescriptors.values());
		viewer.refresh();

		return viewer;
	}

	private void updateCheckboxDependencies() {
	}

	void updateListDependencies() {
		String id= fStore.getString(PreferenceConstants.EDITOR_FOLDING_PROVIDER);
		JavaFoldingStructureProviderDescriptor desc= fProviderDescriptors.get(id);
		IJavaFoldingPreferenceBlock prefs;

		if (desc == null) {
			// safety in case there is no such descriptor
			String message= Messages.format(PreferencesMessages.FoldingConfigurationBlock_error_not_exist, id);
			JavaPlugin.log(new Status(IStatus.WARNING, JavaPlugin.getPluginId(), IStatus.OK, message, null));
			prefs= new ErrorPreferences(message);
		} else {
			prefs= fProviderPreferences.get(id);
			if (prefs == null) {
				try {
					prefs= desc.createPreferences();
					fProviderPreferences.put(id, prefs);
				} catch (CoreException e) {
					JavaPlugin.log(e);
					prefs= new ErrorPreferences(e.getLocalizedMessage());
				}
			}
		}

		Control control= fProviderControls.get(id);
		if (control == null) {
			control= prefs.createControl(fGroup);
			if (control == null) {
				String message= PreferencesMessages.FoldingConfigurationBlock_info_no_preferences;
				control= new ErrorPreferences(message).createControl(fGroup);
			} else {
				fProviderControls.put(id, control);
			}
		}
		Dialog.applyDialogFont(control);
		fStackLayout.topControl= control;
		control.pack();
		fGroup.layout();
		fGroup.getParent().layout();

		prefs.initialize();
	}

	@Override
	public void initialize() {
		restoreFromPreferences();
	}

	@Override
	public void performOk() {
		for (IJavaFoldingPreferenceBlock prefs : fProviderPreferences.values()) {
			prefs.performOk();
		}
	}

	@Override
	public void performDefaults() {
		restoreFromPreferences();
		for (IJavaFoldingPreferenceBlock prefs : fProviderPreferences.values()) {
			prefs.performDefaults();
		}
	}

	@Override
	public void dispose() {
		for (IJavaFoldingPreferenceBlock prefs : fProviderPreferences.values()) {
			prefs.dispose();
		}
	}

	private void restoreFromPreferences() {
		boolean enabled= fStore.getBoolean(PreferenceConstants.EDITOR_FOLDING_ENABLED);
		fFoldingCheckbox.setSelection(enabled);
		updateCheckboxDependencies();

		String id= fStore.getString(PreferenceConstants.EDITOR_FOLDING_PROVIDER);
		Object provider= fProviderDescriptors.get(id);

		// Fallback to default
		if (provider == null) {
			String message= Messages.format(PreferencesMessages.FoldingConfigurationBlock_warning_providerNotFound_resetToDefault, id);
			JavaPlugin.log(new Status(IStatus.WARNING, JavaPlugin.getPluginId(), IStatus.OK, message, null));

			id= JavaPlugin.getDefault().getPreferenceStore().getDefaultString(PreferenceConstants.EDITOR_FOLDING_PROVIDER);

			provider= fProviderDescriptors.get(id);
			Assert.isNotNull(provider);

			fStore.setToDefault(PreferenceConstants.EDITOR_FOLDING_PROVIDER);
		}

		if (fProviderViewer == null)
			updateListDependencies();
		else
			fProviderViewer.setSelection(new StructuredSelection(provider), true);
	}
}
