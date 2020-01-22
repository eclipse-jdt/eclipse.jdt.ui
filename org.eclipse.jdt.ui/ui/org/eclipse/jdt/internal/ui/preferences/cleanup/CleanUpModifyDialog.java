/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.preferences.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CleanUpRegistry.CleanUpTabPageDescriptor;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUpConfigurationUI;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.formatter.IModifyDialogTabPage;
import org.eclipse.jdt.internal.ui.preferences.formatter.ModifyDialog;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.Profile;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileStore;

public class CleanUpModifyDialog extends ModifyDialog {

	/**
	 * The key to store the number (beginning at 0) of the tab page which had the focus last time.
	 */
	private static final String DS_KEY_LAST_FOCUS= "modify_dialog.last_focus"; //$NON-NLS-1$

	private final List<IModifyDialogTabPage> fTabPages= new ArrayList<>();
	private TabFolder fTabFolder;

	private final String fKeyLastFocus;

	/**
	 * Constant array for boolean selection
	 */
	static final String[] FALSE_TRUE = {
		CleanUpOptions.FALSE,
		CleanUpOptions.TRUE
	};

	private Label fCountLabel;
	private ICleanUpConfigurationUI[] fPages;

	public CleanUpModifyDialog(Shell parentShell, Profile profile, ProfileManager profileManager, ProfileStore profileStore, boolean newProfile, String dialogPreferencesKey, String lastSavePathKey) {
	    super(parentShell, profile, profileManager, profileStore, newProfile, dialogPreferencesKey, lastSavePathKey);

		fKeyLastFocus= JavaUI.ID_PLUGIN + dialogPreferencesKey + DS_KEY_LAST_FOCUS;
    }

	@Override
	public void create() {
		super.create();
		if (!fNewProfile) {
			int lastFocusNr= 0;
			try {
				lastFocusNr= fDialogSettings.getInt(fKeyLastFocus);
				if (lastFocusNr < 0) lastFocusNr= 0;
				if (lastFocusNr > fTabPages.size() - 1) lastFocusNr= fTabPages.size() - 1;
			} catch (NumberFormatException x) {
				lastFocusNr= 0;
			}

			fTabFolder.setSelection(lastFocusNr);
			((IModifyDialogTabPage)fTabFolder.getSelection()[0].getData()).setInitialFocus();
		}
	}

	@Override
	protected void createMainArea(Composite parent) {
		fTabFolder = new TabFolder(parent, SWT.NONE);
		fTabFolder.setFont(parent.getFont());
		fTabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		addPages(fWorkingValues);

		applyDialogFont(parent);

		fTabFolder.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
			@Override
			public void widgetSelected(SelectionEvent e) {
				final TabItem tabItem= (TabItem)e.item;
				final IModifyDialogTabPage page= (IModifyDialogTabPage)tabItem.getData();
				fDialogSettings.put(fKeyLastFocus, fTabPages.indexOf(page));
				page.makeVisible();
			}
		});
	}

	private void addPages(final Map<String, String> values) {
		CleanUpTabPageDescriptor[] descriptors= JavaPlugin.getDefault().getCleanUpRegistry().getCleanUpTabPageDescriptors(CleanUpConstants.DEFAULT_CLEAN_UP_OPTIONS);

		fPages= new ICleanUpConfigurationUI[descriptors.length];

		for (int i= 0; i < descriptors.length; i++) {
			String name= descriptors[i].getName();
			CleanUpTabPage page= descriptors[i].createTabPage();

			page.setOptionsKind(CleanUpConstants.DEFAULT_CLEAN_UP_OPTIONS);
			page.setModifyListener(this);
			page.setWorkingValues(values);

			addTabPage(name, page);

			fPages[i]= page;
		}
	}

	private final void addTabPage(String title, IModifyDialogTabPage tabPage) {
		final TabItem tabItem= new TabItem(fTabFolder, SWT.NONE);
		applyDialogFont(tabItem.getControl());
		tabItem.setText(title);
		tabItem.setData(tabPage);
		tabItem.setControl(tabPage.createContents(fTabFolder));
		fTabPages.add(tabPage);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite control= (Composite)super.createDialogArea(parent);

		fCountLabel= new Label(control, SWT.NONE);
		fCountLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		fCountLabel.setFont(parent.getFont());
		updateCountLabel();

		return control;
	}

	@Override
	public void updateStatus(IStatus status) {
		int count= 0;
		for (ICleanUpConfigurationUI fPage : fPages) {
			count+= fPage.getSelectedCleanUpCount();
		}
		if (count == 0) {
			super.updateStatus(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, CleanUpMessages.CleanUpModifyDialog_SelectOne_Error));
		} else {
			super.updateStatus(status);
		}
	}

	@Override
	public void valuesModified() {
		super.valuesModified();
		updateCountLabel();
	}

	private void updateCountLabel() {
		int size= 0, count= 0;
		for (ICleanUpConfigurationUI fPage : fPages) {
			size+= fPage.getCleanUpCount();
			count+= fPage.getSelectedCleanUpCount();
		}

		fCountLabel.setText(Messages.format(CleanUpMessages.CleanUpModifyDialog_XofYSelected_Label, new Object[] {Integer.valueOf(count), Integer.valueOf(size)}));
	}

	/**
	 * {@inheritDoc}
	 * @since 3.5
	 */
	@Override
	protected String getHelpContextId() {
		return IJavaHelpContextIds.CLEAN_UP_PREFERENCE_PAGE;
	}

	@Override
	protected Point getInitialSize() {
		Point initialSize= super.getInitialSize();
		try {
			int lastWidth= fDialogSettings.getInt(fKeyPreferredWidth);
			if (initialSize.x > lastWidth)
				lastWidth= initialSize.x;
			int lastHeight= fDialogSettings.getInt(fKeyPreferredHight);
			if (initialSize.y > lastHeight)
				lastHeight= initialSize.y;
			return new Point(lastWidth, lastHeight);
		} catch (NumberFormatException ex) {
		}
		return initialSize;
	}
}
