/*******************************************************************************
 * Copyright (c) 2005, 2022 IBM Corporation and others.
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUpConfigurationUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.formatter.JavaPreview;
import org.eclipse.jdt.internal.ui.preferences.formatter.ModifyDialogTabPage;

public abstract class CleanUpTabPage extends ModifyDialogTabPage implements ICleanUpConfigurationUI {

	private Map<String, String> fValues;
	private Map<String, String> fInitialValues;
	private JavaPreview fCleanUpPreview;
	private boolean fIsSaveAction;

	private int fCount;
	private int fSelectedCount;
	private List<ButtonPreference> fCheckboxes= new ArrayList<>();

	public CleanUpTabPage() {
		super();
		fCount= 0;
		setSelectedCleanUpCount(0);
		fIsSaveAction= false;
	}

	/**
	 * @param kind the kind of clean up to configure
	 *
	 * @see CleanUpConstants#DEFAULT_CLEAN_UP_OPTIONS
	 * @see CleanUpConstants#DEFAULT_SAVE_ACTION_OPTIONS
	 */
	public void setOptionsKind(int kind) {
		fIsSaveAction= kind == CleanUpConstants.DEFAULT_SAVE_ACTION_OPTIONS;
	}

	@Override
	public void setWorkingValues(Map<String, String> workingValues) {
		super.setWorkingValues(workingValues);
		fValues= workingValues;
	}

	/**
	 * @return is this tab page shown in the save action dialog
	 */
	public boolean isSaveAction() {
		return fIsSaveAction;
	}

	@Override
	public int getCleanUpCount() {
		return fCount;
	}

	@Override
	public int getSelectedCleanUpCount() {
		return fSelectedCount;
	}

	private void setSelectedCleanUpCount(int selectedCount) {
		Assert.isLegal(selectedCount >= 0 && selectedCount <= fCount);
		fSelectedCount= selectedCount;
	}

	@Override
	protected JavaPreview doCreateJavaPreview(Composite parent) {
		fCleanUpPreview= new CleanUpPreview(parent, this);
    	return fCleanUpPreview;
    }

	@Override
	protected void doUpdatePreview() {
		fCleanUpPreview.setWorkingValues(fValues);
		fCleanUpPreview.update();
	}

	@Override
	protected void initializePage() {
		fCleanUpPreview.update();
		fInitialValues= Map.copyOf(fValues);
	}

	protected void registerPreference(final CheckboxPreference preference) {
		fCount++;
		fCheckboxes.add(preference);
		preference.addObserver((o, arg) -> {
			if (preference.getChecked()) {
				setSelectedCleanUpCount(fSelectedCount + 1);
			} else {
				setSelectedCleanUpCount(fSelectedCount - 1);
			}
		});
		if (preference.getChecked()) {
			setSelectedCleanUpCount(fSelectedCount + 1);
		}
	}

	@Override
	public void doSetAll(boolean value) {
		for (ButtonPreference pref : fCheckboxes) {
			if (!(pref instanceof RadioPreference)) {
				pref.setChecked(value);
			}
		}
	}

	@Override
	public void setDefaults() {
		CleanUpOptions settings= JavaPlugin.getDefault().getCleanUpRegistry().getDefaultOptions(CleanUpConstants.DEFAULT_CLEAN_UP_OPTIONS);
		for (ButtonPreference pref : fCheckboxes) {
			String defaultValueString= settings.getValue(pref.getKey());
			boolean defaultValue= Boolean.parseBoolean(defaultValueString);
			pref.setChecked(defaultValue);
		}
	}

	@Override
	public void resetValues() {
		for (ButtonPreference pref : fCheckboxes) {
			String initialValueString= fInitialValues.get(pref.getKey());
			boolean initialValue= Boolean.parseBoolean(initialValueString);
			pref.setChecked(initialValue);
		}

	}

	/* Register a preference that is an option for a cleanup. Checking it does not change the number of clean ups.
	 */
	protected void registerOptionPreference(final CheckboxPreference main, final CheckboxPreference... options) {
		registerPreference(main);
		fCheckboxes.addAll(Arrays.asList(options));
		main.addObserver((source, arg)-> {
			for (CheckboxPreference option : options) {
				option.setEnabled(main.getChecked());
			}
		});
	}

	protected void registerSlavePreference(final CheckboxPreference master, final RadioPreference[] slaves) {
		internalRegisterSlavePreference(master, slaves);
		registerPreference(master);
	}

	protected void registerSlavePreference(final CheckboxPreference master, final CheckboxPreference[] slaves) {
		registerSlavePreference(master, slaves, null);
		registerPreference(master);
	}

	/**
	 * Connects master and slave checkboxes.
	 *
	 * @param master the master
	 * @param slaves direct slaves of the master
	 * @param subSlaves indirect slaves, i.e. a slave is a master of its subSlave).
	 * 		First index into array is the subSlave's master's index. subSlaves can also be <code>null</code>.
	 */
	protected void registerSlavePreference(final CheckboxPreference master, final CheckboxPreference[] slaves, final CheckboxPreference[][] subSlaves) {
		internalRegisterSlavePreference(master, slaves);

		fCount+= slaves.length;

		if (subSlaves != null) {
			for (int i= 0; i < slaves.length; i++) {
				final CheckboxPreference slave= slaves[i];
				for (CheckboxPreference subSlave : subSlaves[i]) {
					master.addObserver((o, arg) -> {
						boolean enabled= master.getChecked() && slave.getChecked();
						subSlave.setEnabled(enabled);
					});
				}
			}
		}

		master.addObserver((o, arg) -> {
			boolean masterChecked= master.getChecked();
			for (int i= 0; i < slaves.length; i++) {
				if (slaves[i].getChecked()) {
					setSelectedCleanUpCount(fSelectedCount + (masterChecked ? 1 : -1));
					if (subSlaves != null) {
						for (CheckboxPreference subSlave : subSlaves[i]) {
							if (subSlave.getChecked()) {
								setSelectedCleanUpCount(fSelectedCount + (masterChecked ? 1 : -1));
							}
						}
					}
				}
			}
		});

		for (CheckboxPreference slave : slaves) {
			slave.addObserver((o, arg) -> {
				if (master.getChecked()) {
					setSelectedCleanUpCount(fSelectedCount + (slave.getChecked() ? 1 : -1));
				}
			});
		}

		if (master.getChecked()) {
			for (CheckboxPreference slave : slaves) {
				if (slave.getChecked() && master.getEnabled()) {
					setSelectedCleanUpCount(fSelectedCount + 1);
				}
			}
		}
	}

	private void internalRegisterSlavePreference(final CheckboxPreference master, final ButtonPreference[] slaves) {
		fCheckboxes.addAll(Arrays.asList(slaves));
    	master.addObserver( (o, arg) -> {
			for (ButtonPreference slave : slaves) {
				slave.setEnabled(master.getChecked());
			}
		});

		for (ButtonPreference slave : slaves) {
			slave.setEnabled(master.getChecked());
		}
	}

	/**
	 * Creates a spacer control with a pre-defined width.
	 *
	 * @param parent the parent composite
	 */
	// should be called "createSpacer"
	protected void intent(Composite parent) {
        Label l= new Label(parent, SWT.NONE);
    	GridData gd= new GridData();
    	gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(4);
    	l.setLayoutData(gd);
    }

}