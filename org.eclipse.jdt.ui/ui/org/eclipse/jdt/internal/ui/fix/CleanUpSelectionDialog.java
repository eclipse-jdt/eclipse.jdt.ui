/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.fix;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.StatusDialog;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpTabPage;
import org.eclipse.jdt.internal.ui.preferences.formatter.IModifyDialogTabPage;

public abstract class CleanUpSelectionDialog extends StatusDialog implements IModifyDialogTabPage.IModificationListener {

	protected final static class NamedCleanUpTabPage {

		private final String fName;
		private final CleanUpTabPage fPage;

		public NamedCleanUpTabPage(String name, CleanUpTabPage page) {
			fName= name;
			fPage= page;
		}

		public CleanUpTabPage getPage() {
			return fPage;
		}

		public String getName() {
			return fName;
		}

	}

	private static final String DS_KEY_PREFERRED_WIDTH= ".preferred_width"; //$NON-NLS-1$
	private static final String DS_KEY_PREFERRED_HEIGHT= ".preferred_height"; //$NON-NLS-1$
	private static final String DS_KEY_PREFERRED_X= ".preferred_x"; //$NON-NLS-1$
	private static final String DS_KEY_PREFERRED_Y= ".preferred_y"; //$NON-NLS-1$
	private static final String DS_KEY_LAST_FOCUS= ".last_focus"; //$NON-NLS-1$

	private final Map<String, String> fWorkingValues;
	private final List<IModifyDialogTabPage> fTabPages;
	private final IDialogSettings fDialogSettings;
	private TabFolder fTabFolder;
	private CleanUpTabPage[] fPages;
	private Label fCountLabel;

	public CleanUpSelectionDialog(Shell parent, Map<String, String> settings, String title) {
		super(parent);
		setTitle(title);
		fWorkingValues= settings;
		setStatusLineAboveButtons(false);
		fTabPages= new ArrayList<>();
		fDialogSettings= JavaPlugin.getDefault().getDialogSettings();
	}

	/*
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 * @since 3.4
	 */
	@Override
	protected boolean isResizable() {
		return true;
	}

	protected abstract NamedCleanUpTabPage[] createTabPages(Map<String, String> workingValues);

	protected abstract String getPreferenceKeyPrefix();

	protected abstract String getSelectionCountMessage(int selectionCount, int size);

	protected abstract String getEmptySelectionMessage();

	protected Map<String, String> getWorkingValues() {
		return fWorkingValues;
	}

	@Override
	public boolean close() {
		final Rectangle shell= getShell().getBounds();

		fDialogSettings.put(getPreferenceKeyWidth(), shell.width);
		fDialogSettings.put(getPreferenceKeyHeight(), shell.height);
		fDialogSettings.put(getPreferenceKeyPositionX(), shell.x);
		fDialogSettings.put(getPreferenceKeyPositionY(), shell.y);

		return super.close();
	}

	@Override
	public void create() {
		super.create();
		int lastFocusNr= 0;
		try {
			lastFocusNr= fDialogSettings.getInt(getPreferenceKeyFocus());
			if (lastFocusNr < 0)
				lastFocusNr= 0;
			if (lastFocusNr > fTabPages.size() - 1)
				lastFocusNr= fTabPages.size() - 1;
		} catch (NumberFormatException x) {
			lastFocusNr= 0;
		}

		fTabFolder.setSelection(lastFocusNr);
		((IModifyDialogTabPage) fTabFolder.getSelection()[0].getData()).setInitialFocus();
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		Button restoreDefaults= createButton(parent, IDialogConstants.CLIENT_ID + 1,
				MultiFixMessages.CleanUpSelectionDialog_restoreDefaults_label, false);
		restoreDefaults.addSelectionListener(widgetSelectedAdapter((e) -> {
			for (CleanUpTabPage page : fPages) {
				page.setDefaults();
			}
		}));
		Button restoreValues= createButton(parent, IDialogConstants.CLIENT_ID + 2,
				MultiFixMessages.CleanUpSelectionDialog_resetProfile_label, false);
		restoreValues.addSelectionListener(widgetSelectedAdapter((e) -> {
			for (CleanUpTabPage page : fPages) {
				page.resetValues();
			}
		}));

	}
	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite composite= (Composite) super.createDialogArea(parent);

		fTabFolder= new TabFolder(composite, SWT.NONE);
		fTabFolder.setFont(composite.getFont());
		fTabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		NamedCleanUpTabPage[] pages= createTabPages(fWorkingValues);

		fPages= new CleanUpTabPage[pages.length];

		for (int i= 0; i < pages.length; i++) {
			fPages[i]= pages[i].getPage();
			addTabPage(pages[i].getName(), fPages[i]);
		}

		fCountLabel= new Label(composite, SWT.NONE);
		fCountLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		updateCountLabel();

		applyDialogFont(composite);

		fTabFolder.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				final TabItem tabItem= (TabItem) e.item;
				final IModifyDialogTabPage page= (IModifyDialogTabPage) tabItem.getData();
				fDialogSettings.put(getPreferenceKeyFocus(), fTabPages.indexOf(page));
				page.makeVisible();
			}
		});

		updateStatus(StatusInfo.OK_STATUS);

		return composite;
	}

	@Override
	public void updateStatus(IStatus status) {
		int count= 0;
		for (CleanUpTabPage page : fPages) {
			count+= page.getSelectedCleanUpCount();
		}
		if (count == 0) {
			super.updateStatus(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, getEmptySelectionMessage()));
		} else {
			if (status == null) {
				super.updateStatus(StatusInfo.OK_STATUS);
			} else {
				super.updateStatus(status);
			}
		}
	}

	@Override
	protected Point getInitialSize() {
		Point initialSize= super.getInitialSize();
		try {
			int lastWidth= fDialogSettings.getInt(getPreferenceKeyWidth());
			if (initialSize.x > lastWidth)
				lastWidth= initialSize.x;
			int lastHeight= fDialogSettings.getInt(getPreferenceKeyHeight());
			if (initialSize.y > lastHeight)
				lastHeight= initialSize.y;
			return new Point(lastWidth, lastHeight);
		} catch (NumberFormatException ex) {
		}
		return initialSize;
	}

	@Override
	protected Point getInitialLocation(Point initialSize) {
		try {
			return new Point(fDialogSettings.getInt(getPreferenceKeyPositionX()), fDialogSettings.getInt(getPreferenceKeyPositionY()));
		} catch (NumberFormatException ex) {
			return super.getInitialLocation(initialSize);
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
	public void valuesModified() {
		updateCountLabel();
		updateStatus(StatusInfo.OK_STATUS);
	}

	private void updateCountLabel() {
		int size= 0, count= 0;
		for (CleanUpTabPage page : fPages) {
			size+= page.getCleanUpCount();
			count+= page.getSelectedCleanUpCount();
		}

		fCountLabel.setText(getSelectionCountMessage(count, size));
	}

	private String getPreferenceKeyWidth() {
		return getPreferenceKeyPrefix() + DS_KEY_PREFERRED_WIDTH;
	}

	private String getPreferenceKeyHeight() {
		return getPreferenceKeyPrefix() + DS_KEY_PREFERRED_HEIGHT;
	}

	private String getPreferenceKeyPositionY() {
		return getPreferenceKeyPrefix() + DS_KEY_PREFERRED_Y;
	}

	private String getPreferenceKeyFocus() {
		return getPreferenceKeyPrefix() + DS_KEY_LAST_FOCUS;
	}

	private String getPreferenceKeyPositionX() {
		return getPreferenceKeyPrefix() + DS_KEY_PREFERRED_X;
	}

}