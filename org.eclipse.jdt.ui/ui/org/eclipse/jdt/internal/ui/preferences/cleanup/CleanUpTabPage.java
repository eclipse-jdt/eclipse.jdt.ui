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
package org.eclipse.jdt.internal.ui.preferences.cleanup;

import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.cleanup.ICleanUpConfigurationUI;

import org.eclipse.jdt.internal.ui.preferences.formatter.JavaPreview;
import org.eclipse.jdt.internal.ui.preferences.formatter.ModifyDialogTabPage;

public abstract class CleanUpTabPage extends ModifyDialogTabPage implements ICleanUpConfigurationUI {

	private Map fValues;
	private JavaPreview fCleanUpPreview;
	private boolean fIsSaveAction;

	private int fCount;
	private int fSelectedCount;

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

	/**
	 * {@inheritDoc}
	 */
	public void setWorkingValues(Map workingValues) {
		super.setWorkingValues(workingValues);
		fValues= workingValues;
	}

	/**
	 * @return is this tab page shown in the save action dialog
	 */
	public boolean isSaveAction() {
		return fIsSaveAction;
	}

	public int getCleanUpCount() {
		return fCount;
	}

	public int getSelectedCleanUpCount() {
		return fSelectedCount;
	}

	private void setSelectedCleanUpCount(int selectedCount) {
		Assert.isLegal(selectedCount >= 0 && selectedCount <= fCount);
		fSelectedCount= selectedCount;
	}

	protected JavaPreview doCreateJavaPreview(Composite parent) {
		fCleanUpPreview= new CleanUpPreview(parent, this);
    	return fCleanUpPreview;
    }

	protected void doUpdatePreview() {
		fCleanUpPreview.setWorkingValues(fValues);
		fCleanUpPreview.update();
	}

	protected void initializePage() {
		fCleanUpPreview.update();
	}

	protected void registerPreference(final CheckboxPreference preference) {
		fCount++;
		preference.addObserver(new Observer() {
			public void update(Observable o, Object arg) {
				if (preference.getChecked()) {
					setSelectedCleanUpCount(fSelectedCount + 1);
				} else {
					setSelectedCleanUpCount(fSelectedCount - 1);
				}
			}
		});
		if (preference.getChecked()) {
			setSelectedCleanUpCount(fSelectedCount + 1);
		}
	}

	protected void registerSlavePreference(final CheckboxPreference master, final RadioPreference[] slaves) {
		internalRegisterSlavePreference(master, slaves);
		registerPreference(master);
	}

	protected void registerSlavePreference(final CheckboxPreference master, final CheckboxPreference[] slaves) {
		internalRegisterSlavePreference(master, slaves);
		fCount+= slaves.length;

		master.addObserver(new Observer() {
			public void update(Observable o, Object arg) {
				if (master.getChecked()) {
					for (int i= 0; i < slaves.length; i++) {
						if (slaves[i].getChecked()) {
							setSelectedCleanUpCount(fSelectedCount + 1);
						}
					}
				} else {
					for (int i= 0; i < slaves.length; i++) {
						if (slaves[i].getChecked()) {
							setSelectedCleanUpCount(fSelectedCount - 1);
						}
					}
				}
			}
		});

		for (int i= 0; i < slaves.length; i++) {
			final CheckboxPreference slave= slaves[i];
			slave.addObserver(new Observer() {
				public void update(Observable o, Object arg) {
					if (slave.getChecked()) {
						setSelectedCleanUpCount(fSelectedCount + 1);
					} else {
						setSelectedCleanUpCount(fSelectedCount - 1);
					}
				}
			});
		}

		if (master.getChecked()) {
			for (int i= 0; i < slaves.length; i++) {
				if (slaves[i].getChecked()) {
					setSelectedCleanUpCount(fSelectedCount + 1);
				}
			}
		}
	}

	private void internalRegisterSlavePreference(final CheckboxPreference master, final ButtonPreference[] slaves) {
    	master.addObserver( new Observer() {
    		public void update(Observable o, Object arg) {
    			for (int i= 0; i < slaves.length; i++) {
					slaves[i].setEnabled(master.getChecked());
				}
    		}
    	});

    	for (int i= 0; i < slaves.length; i++) {
			slaves[i].setEnabled(master.getChecked());
		}
	}

	protected void intent(Composite group) {
        Label l= new Label(group, SWT.NONE);
    	GridData gd= new GridData();
    	gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(4);
    	l.setLayoutData(gd);
    }

}