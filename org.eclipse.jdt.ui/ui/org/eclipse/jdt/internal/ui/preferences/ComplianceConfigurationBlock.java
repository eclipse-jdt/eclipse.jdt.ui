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
package org.eclipse.jdt.internal.ui.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;

/**
  */
public class ComplianceConfigurationBlock extends OptionsConfigurationBlock {

	// Preference store keys, see JavaCore.getOptions
	private static final Key PREF_LOCAL_VARIABLE_ATTR=  getJDTCoreKey(JavaCore.COMPILER_LOCAL_VARIABLE_ATTR);
	private static final Key PREF_LINE_NUMBER_ATTR= getJDTCoreKey(JavaCore.COMPILER_LINE_NUMBER_ATTR);
	private static final Key PREF_SOURCE_FILE_ATTR= getJDTCoreKey(JavaCore.COMPILER_SOURCE_FILE_ATTR);
	private static final Key PREF_CODEGEN_UNUSED_LOCAL= getJDTCoreKey(JavaCore.COMPILER_CODEGEN_UNUSED_LOCAL);
	private static final Key PREF_CODEGEN_TARGET_PLATFORM= getJDTCoreKey(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM);
	private static final Key PREF_CODEGEN_INLINE_JSR_BYTECODE= getJDTCoreKey(JavaCore.COMPILER_CODEGEN_INLINE_JSR_BYTECODE);
	
	private static final Key PREF_SOURCE_COMPATIBILITY= getJDTCoreKey(JavaCore.COMPILER_SOURCE);
	private static final Key PREF_COMPLIANCE= getJDTCoreKey(JavaCore.COMPILER_COMPLIANCE);
	private static final Key PREF_PB_ASSERT_AS_IDENTIFIER= getJDTCoreKey(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER);
	private static final Key PREF_PB_ENUM_AS_IDENTIFIER= getJDTCoreKey(JavaCore.COMPILER_PB_ENUM_IDENTIFIER);
	
	private static final Key INTR_DEFAULT_COMPLIANCE= getJDTUIKey("internal.default.compliance"); //$NON-NLS-1$

	// values
	private static final String GENERATE= JavaCore.GENERATE;
	private static final String DO_NOT_GENERATE= JavaCore.DO_NOT_GENERATE;
	
	private static final String PRESERVE= JavaCore.PRESERVE;
	private static final String OPTIMIZE_OUT= JavaCore.OPTIMIZE_OUT;
	
	private static final String VERSION_1_1= JavaCore.VERSION_1_1;
	private static final String VERSION_1_2= JavaCore.VERSION_1_2;
	private static final String VERSION_1_3= JavaCore.VERSION_1_3;
	private static final String VERSION_1_4= JavaCore.VERSION_1_4;
	private static final String VERSION_1_5= JavaCore.VERSION_1_5;
	
	private static final String ERROR= JavaCore.ERROR;
	private static final String WARNING= JavaCore.WARNING;
	private static final String IGNORE= JavaCore.IGNORE;

	private static final String ENABLED= JavaCore.ENABLED;
	private static final String DISABLED= JavaCore.DISABLED;
	
	
	private static final String DEFAULT_CONF= "default"; //$NON-NLS-1$
	private static final String USER_CONF= "user";	 //$NON-NLS-1$

	private ArrayList fComplianceControls;
	private PixelConverter fPixelConverter;

	private String[] fRememberedUserCompliance;
	
	private static final int IDX_ASSERT_AS_IDENTIFIER= 0;
	private static final int IDX_ENUM_AS_IDENTIFIER= 1;
	private static final int IDX_SOURCE_COMPATIBILITY= 2;
	private static final int IDX_CODEGEN_TARGET_PLATFORM= 3;
	private static final int IDX_COMPLIANCE= 4;
	private static final int IDX_INLINE_JSR_BYTECODE= 5;

	private IStatus fComplianceStatus;

	public ComplianceConfigurationBlock(IStatusChangeListener context, IProject project) {
		super(context, project, getKeys());
		
		fComplianceControls= new ArrayList();
			
		fComplianceStatus= new StatusInfo();
		
		fRememberedUserCompliance= new String[] { // caution: order depends on IDX_* constants
			getValue(PREF_PB_ASSERT_AS_IDENTIFIER),
			getValue(PREF_PB_ENUM_AS_IDENTIFIER),
			getValue(PREF_SOURCE_COMPATIBILITY),
			getValue(PREF_CODEGEN_TARGET_PLATFORM),
			getValue(PREF_COMPLIANCE),
			getValue(PREF_CODEGEN_INLINE_JSR_BYTECODE),
		};
	}
	
	private static Key[] getKeys() {
		return new Key[] {
				PREF_LOCAL_VARIABLE_ATTR, PREF_LINE_NUMBER_ATTR, PREF_SOURCE_FILE_ATTR, PREF_CODEGEN_UNUSED_LOCAL,
				PREF_CODEGEN_INLINE_JSR_BYTECODE,
				PREF_COMPLIANCE, PREF_SOURCE_COMPATIBILITY,
				PREF_CODEGEN_TARGET_PLATFORM, PREF_PB_ASSERT_AS_IDENTIFIER, PREF_PB_ENUM_AS_IDENTIFIER
			};
	}
	
	protected final Map getOptions() {
		Map map= super.getOptions();
		map.put(INTR_DEFAULT_COMPLIANCE, getCurrentCompliance(map));
		return map;
	}
	
	protected final Map getDefaultOptions() {
		Map map= super.getDefaultOptions();
		map.put(INTR_DEFAULT_COMPLIANCE, getCurrentCompliance(map));
		return map;
	}	
	
	
	/*
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		fPixelConverter= new PixelConverter(parent);
		setShell(parent.getShell());
		
		Composite complianceComposite= createComplianceTabContent(parent);
		
		validateSettings(null, null, null);
	
		return complianceComposite;
	}
	
	private Composite createComplianceTabContent(Composite folder) {


		String[] values345= new String[] { VERSION_1_3, VERSION_1_4, VERSION_1_5 };
		String[] values345Labels= new String[] {
			PreferencesMessages.getString("ComplianceConfigurationBlock.version13"),  //$NON-NLS-1$
			PreferencesMessages.getString("ComplianceConfigurationBlock.version14"), //$NON-NLS-1$
			PreferencesMessages.getString("ComplianceConfigurationBlock.version15") //$NON-NLS-1$
		};

		final ScrolledPageContent sc1 = new ScrolledPageContent(folder);
		
		Composite compComposite= sc1.getBody();
		
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 1;
		compComposite.setLayout(layout);

		int nColumns= 3;

		layout= new GridLayout();
		layout.numColumns= nColumns;

		Group group= new Group(compComposite, SWT.NONE);
		group.setText(PreferencesMessages.getString("ComplianceConfigurationBlock.compliance.group.label")); //$NON-NLS-1$
		group.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false));
		group.setLayout(layout);
	
		String label= PreferencesMessages.getString("ComplianceConfigurationBlock.compiler_compliance.label"); //$NON-NLS-1$
		addComboBox(group, label, PREF_COMPLIANCE, values345, values345Labels, 0);

		label= PreferencesMessages.getString("ComplianceConfigurationBlock.default_settings.label"); //$NON-NLS-1$
		addCheckBox(group, label, INTR_DEFAULT_COMPLIANCE, new String[] { DEFAULT_CONF, USER_CONF }, 0);	

		int indent= fPixelConverter.convertWidthInCharsToPixels(2);
		Control[] otherChildren= group.getChildren();	
				
		String[] versions= new String[] { VERSION_1_1, VERSION_1_2, VERSION_1_3, VERSION_1_4, VERSION_1_5 };
		String[] versionsLabels= new String[] {
			PreferencesMessages.getString("ComplianceConfigurationBlock.version11"),  //$NON-NLS-1$
			PreferencesMessages.getString("ComplianceConfigurationBlock.version12"), //$NON-NLS-1$
			PreferencesMessages.getString("ComplianceConfigurationBlock.version13"), //$NON-NLS-1$
			PreferencesMessages.getString("ComplianceConfigurationBlock.version14"), //$NON-NLS-1$
			PreferencesMessages.getString("ComplianceConfigurationBlock.version15") //$NON-NLS-1$
		};
		
		label= PreferencesMessages.getString("ComplianceConfigurationBlock.codegen_targetplatform.label"); //$NON-NLS-1$
		addComboBox(group, label, PREF_CODEGEN_TARGET_PLATFORM, versions, versionsLabels, indent);	

		label= PreferencesMessages.getString("ComplianceConfigurationBlock.source_compatibility.label"); //$NON-NLS-1$
		addComboBox(group, label, PREF_SOURCE_COMPATIBILITY, values345, values345Labels, indent);	

		String[] errorWarningIgnore= new String[] { ERROR, WARNING, IGNORE };
		
		String[] errorWarningIgnoreLabels= new String[] {
			PreferencesMessages.getString("ComplianceConfigurationBlock.error"),  //$NON-NLS-1$
			PreferencesMessages.getString("ComplianceConfigurationBlock.warning"), //$NON-NLS-1$
			PreferencesMessages.getString("ComplianceConfigurationBlock.ignore") //$NON-NLS-1$
		};

		label= PreferencesMessages.getString("ComplianceConfigurationBlock.pb_assert_as_identifier.label"); //$NON-NLS-1$
		addComboBox(group, label, PREF_PB_ASSERT_AS_IDENTIFIER, errorWarningIgnore, errorWarningIgnoreLabels, indent);		

		label= PreferencesMessages.getString("ComplianceConfigurationBlock.pb_enum_as_identifier.label"); //$NON-NLS-1$
		addComboBox(group, label, PREF_PB_ENUM_AS_IDENTIFIER, errorWarningIgnore, errorWarningIgnoreLabels, indent);		

		
		Control[] allChildren= group.getChildren();
		fComplianceControls.addAll(Arrays.asList(allChildren));
		fComplianceControls.removeAll(Arrays.asList(otherChildren));

		layout= new GridLayout();
		layout.numColumns= nColumns;

		group= new Group(compComposite, SWT.NONE);
		group.setText(PreferencesMessages.getString("ComplianceConfigurationBlock.classfiles.group.label")); //$NON-NLS-1$
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setLayout(layout);

		String[] generateValues= new String[] { GENERATE, DO_NOT_GENERATE };
		String[] enableDisableValues= new String[] { ENABLED, DISABLED };

		label= PreferencesMessages.getString("ComplianceConfigurationBlock.variable_attr.label"); //$NON-NLS-1$
		addCheckBox(group, label, PREF_LOCAL_VARIABLE_ATTR, generateValues, 0);
		
		label= PreferencesMessages.getString("ComplianceConfigurationBlock.line_number_attr.label"); //$NON-NLS-1$
		addCheckBox(group, label, PREF_LINE_NUMBER_ATTR, generateValues, 0);		

		label= PreferencesMessages.getString("ComplianceConfigurationBlock.source_file_attr.label"); //$NON-NLS-1$
		addCheckBox(group, label, PREF_SOURCE_FILE_ATTR, generateValues, 0);		

		label= PreferencesMessages.getString("ComplianceConfigurationBlock.codegen_unused_local.label"); //$NON-NLS-1$
		addCheckBox(group, label, PREF_CODEGEN_UNUSED_LOCAL, new String[] { PRESERVE, OPTIMIZE_OUT }, 0);	

		label= PreferencesMessages.getString("ComplianceConfigurationBlock.codegen_inline_jsr_bytecode.label"); //$NON-NLS-1$
		addCheckBox(group, label, PREF_CODEGEN_INLINE_JSR_BYTECODE, enableDisableValues, 0);	
		
		return sc1;
	}
	
	
	/* (non-javadoc)
	 * Update fields and validate.
	 * @param changedKey Key that changed, or null, if all changed.
	 */	
	protected void validateSettings(Key changedKey, String oldValue, String newValue) {
		
		if (changedKey != null) {
			if (INTR_DEFAULT_COMPLIANCE.equals(changedKey)) {
				if (VERSION_1_5.equals(newValue)) {
					MessageDialog.openWarning(getShell(), 
						"Compiler Settings",  //$NON-NLS-1$
						"Please note that the J2SE 5.0 support is still under development."); //$NON-NLS-1$
				}
				
				updateComplianceEnableState();
				updateComplianceDefaultSettings(true, null);
				fComplianceStatus= validateCompliance();
			} else if (PREF_COMPLIANCE.equals(changedKey)) {
			    // set compliance settings to default
			    Object oldDefault= setValue(INTR_DEFAULT_COMPLIANCE, DEFAULT_CONF);
			    updateComplianceEnableState();
				updateComplianceDefaultSettings(USER_CONF.equals(oldDefault), oldValue);
				fComplianceStatus= validateCompliance();
			} else if (PREF_SOURCE_COMPATIBILITY.equals(changedKey)) {
				updateAssertEnumAsIdentifierEnableState();
				fComplianceStatus= validateCompliance();
			} else if (PREF_CODEGEN_TARGET_PLATFORM.equals(changedKey)) {
				updateInlineJSREnableState();
				fComplianceStatus= validateCompliance();
			} else if (PREF_PB_ENUM_AS_IDENTIFIER.equals(changedKey) ||
					PREF_PB_ASSERT_AS_IDENTIFIER.equals(changedKey)) {
				fComplianceStatus= validateCompliance();
			} else {
				return;
			}
		} else {
			updateComplianceEnableState();
			updateAssertEnumAsIdentifierEnableState();
			updateInlineJSREnableState();
			fComplianceStatus= validateCompliance();
		}		
		fContext.statusChanged(fComplianceStatus);
	}
	


	private IStatus validateCompliance() {
		StatusInfo status= new StatusInfo();
		String compliance= getValue(PREF_COMPLIANCE);
		String source= getValue(PREF_SOURCE_COMPATIBILITY);
		String target= getValue(PREF_CODEGEN_TARGET_PLATFORM);
		
		if (VERSION_1_3.equals(compliance)) {
			if (VERSION_1_4.equals(source) || VERSION_1_5.equals(source)) {
				status.setError(PreferencesMessages.getString("ComplianceConfigurationBlock.cpl13src145.error")); //$NON-NLS-1$
				return status;
			} 
			if (VERSION_1_4.equals(target) || VERSION_1_5.equals(target)) {
				status.setError(PreferencesMessages.getString("ComplianceConfigurationBlock.cpl13trg145.error")); //$NON-NLS-1$
				return status;
			}
		} else if (VERSION_1_4.equals(compliance)) {
			if (VERSION_1_5.equals(source)) {
				status.setError(PreferencesMessages.getString("ComplianceConfigurationBlock.cpl14src15.error")); //$NON-NLS-1$
				return status;
			} 
			if (VERSION_1_5.equals(target)) {
				status.setError(PreferencesMessages.getString("ComplianceConfigurationBlock.cpl14trg15.error")); //$NON-NLS-1$
				return status;
			}			
		}
		if (VERSION_1_4.equals(source)) {
			if (VERSION_1_1.equals(target) || VERSION_1_2.equals(target) || VERSION_1_3.equals(target)) {
				status.setError(PreferencesMessages.getString("ComplianceConfigurationBlock.src14tgt14.error")); //$NON-NLS-1$
				return status;
			}
		} else if (VERSION_1_5.equals(source)) {
			if (!VERSION_1_5.equals(target)) {
				status.setError(PreferencesMessages.getString("ComplianceConfigurationBlock.src15tgt15.error")); //$NON-NLS-1$
				return status;
			}
		}
		return status;
	}
	
		
	/*
	 * Update the compliance controls' enable state
	 */		
	private void updateComplianceEnableState() {
		boolean enabled= checkValue(INTR_DEFAULT_COMPLIANCE, USER_CONF);
		for (int i= fComplianceControls.size() - 1; i >= 0; i--) {
			Control curr= (Control) fComplianceControls.get(i);
			curr.setEnabled(enabled);
		}
	}
	
	private void updateAssertEnumAsIdentifierEnableState() {
		if (checkValue(INTR_DEFAULT_COMPLIANCE, USER_CONF)) {
			String compatibility= getValue(PREF_SOURCE_COMPATIBILITY);
			
			boolean isLessThan14= VERSION_1_3.equals(compatibility);
			updateRememberedComplianceOption(PREF_PB_ASSERT_AS_IDENTIFIER, IDX_ASSERT_AS_IDENTIFIER, isLessThan14);
		
			boolean isLessThan15= isLessThan14 || VERSION_1_4.equals(compatibility);
			updateRememberedComplianceOption(PREF_PB_ENUM_AS_IDENTIFIER, IDX_ENUM_AS_IDENTIFIER, isLessThan15);
		}
	}
	
	private void updateRememberedComplianceOption(Key prefKey, int idx, boolean enabled) {
		Combo combo= getComboBox(prefKey);
		combo.setEnabled(enabled);
		
		if (!enabled) {
			String val= getValue(prefKey);
			if (!ERROR.equals(val)) {
				setValue(prefKey, ERROR);
				updateCombo(combo);
				fRememberedUserCompliance[idx]= val;
			}
		} else {
			String val= fRememberedUserCompliance[idx];
			if (!ERROR.equals(val)) {
				setValue(prefKey, val);
				updateCombo(combo);
			}
		}
	}

	private void updateInlineJSREnableState() {
		boolean enabled= !checkValue(PREF_CODEGEN_TARGET_PLATFORM, VERSION_1_5);
		Button checkBox= getCheckBox(PREF_CODEGEN_INLINE_JSR_BYTECODE);
		checkBox.setEnabled(enabled);
		
		if (!enabled) {
			String val= getValue(PREF_CODEGEN_INLINE_JSR_BYTECODE);
			fRememberedUserCompliance[IDX_INLINE_JSR_BYTECODE]= val;
			
			if (!ENABLED.equals(val)) {
				setValue(PREF_CODEGEN_INLINE_JSR_BYTECODE, ENABLED);
				updateCheckBox(checkBox);
			}
		} else {
			String val= fRememberedUserCompliance[IDX_INLINE_JSR_BYTECODE];
			if (!ENABLED.equals(val)) {
				setValue(PREF_CODEGEN_INLINE_JSR_BYTECODE, val);
				updateCheckBox(checkBox);
			}
		}
	}

	/*
	 * Set the default compliance values derived from the chosen level
	 */	
	private void updateComplianceDefaultSettings(boolean rememberOld, String oldComplianceLevel) {
		String assertAsId, enumAsId, source, target;
		boolean isDefault= checkValue(INTR_DEFAULT_COMPLIANCE, DEFAULT_CONF);
		String complianceLevel= getValue(PREF_COMPLIANCE);
		
		if (isDefault) {
			if (rememberOld) {
				if (oldComplianceLevel == null) {
					oldComplianceLevel= complianceLevel;
				}
				
				fRememberedUserCompliance[IDX_ASSERT_AS_IDENTIFIER]= getValue(PREF_PB_ASSERT_AS_IDENTIFIER);
				fRememberedUserCompliance[IDX_ENUM_AS_IDENTIFIER]= getValue(PREF_PB_ENUM_AS_IDENTIFIER);
				fRememberedUserCompliance[IDX_SOURCE_COMPATIBILITY]= getValue(PREF_SOURCE_COMPATIBILITY);
				fRememberedUserCompliance[IDX_CODEGEN_TARGET_PLATFORM]= getValue(PREF_CODEGEN_TARGET_PLATFORM);
				fRememberedUserCompliance[IDX_COMPLIANCE]= oldComplianceLevel;
			}

			if (VERSION_1_4.equals(complianceLevel)) {
				assertAsId= WARNING;
				enumAsId= WARNING;
				source= VERSION_1_3;
				target= VERSION_1_2;
			} else if (VERSION_1_5.equals(complianceLevel)) {
				assertAsId= ERROR;
				enumAsId= ERROR;
				source= VERSION_1_5;
				target= VERSION_1_5;
			} else {
				assertAsId= IGNORE;
				enumAsId= IGNORE;
				source= VERSION_1_3;
				target= VERSION_1_1;
			}
		} else {
			if (rememberOld && complianceLevel.equals(fRememberedUserCompliance[IDX_COMPLIANCE])) {
				assertAsId= fRememberedUserCompliance[IDX_ASSERT_AS_IDENTIFIER];
				enumAsId= fRememberedUserCompliance[IDX_ENUM_AS_IDENTIFIER];
				source= fRememberedUserCompliance[IDX_SOURCE_COMPATIBILITY];
				target= fRememberedUserCompliance[IDX_CODEGEN_TARGET_PLATFORM];
			} else {
				updateInlineJSREnableState();
				updateAssertEnumAsIdentifierEnableState();
				return;
			}
		}
		setValue(PREF_PB_ASSERT_AS_IDENTIFIER, assertAsId);
		setValue(PREF_PB_ENUM_AS_IDENTIFIER, enumAsId);
		setValue(PREF_SOURCE_COMPATIBILITY, source);
		setValue(PREF_CODEGEN_TARGET_PLATFORM, target);
		updateControls();
		updateInlineJSREnableState();
		updateAssertEnumAsIdentifierEnableState();
	}
	
	/*
	 * Evaluate if the current compliance setting correspond to a default setting
	 */
	private static String getCurrentCompliance(Map map) {
		Object complianceLevel= map.get(PREF_COMPLIANCE);
		if ((VERSION_1_3.equals(complianceLevel)
				&& IGNORE.equals(map.get(PREF_PB_ASSERT_AS_IDENTIFIER))
				&& IGNORE.equals(map.get(PREF_PB_ENUM_AS_IDENTIFIER))
				&& VERSION_1_3.equals(map.get(PREF_SOURCE_COMPATIBILITY))
				&& VERSION_1_1.equals(map.get(PREF_CODEGEN_TARGET_PLATFORM)))
			|| (VERSION_1_4.equals(complianceLevel)
				&& WARNING.equals(map.get(PREF_PB_ASSERT_AS_IDENTIFIER))
				&& WARNING.equals(map.get(PREF_PB_ENUM_AS_IDENTIFIER))
				&& VERSION_1_3.equals(map.get(PREF_SOURCE_COMPATIBILITY))
				&& VERSION_1_2.equals(map.get(PREF_CODEGEN_TARGET_PLATFORM)))
			|| (VERSION_1_5.equals(complianceLevel)
				&& ERROR.equals(map.get(PREF_PB_ASSERT_AS_IDENTIFIER))
				&& ERROR.equals(map.get(PREF_PB_ENUM_AS_IDENTIFIER))
				&& VERSION_1_5.equals(map.get(PREF_SOURCE_COMPATIBILITY))
				&& VERSION_1_5.equals(map.get(PREF_CODEGEN_TARGET_PLATFORM)))) {
			return DEFAULT_CONF;
		}
		return USER_CONF;
	}
	
	
	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
		String title= PreferencesMessages.getString("ComplianceConfigurationBlock.needsbuild.title"); //$NON-NLS-1$
		String message;
		if (workspaceSettings) {
			message= PreferencesMessages.getString("ComplianceConfigurationBlock.needsfullbuild.message"); //$NON-NLS-1$
		} else {
			message= PreferencesMessages.getString("ComplianceConfigurationBlock.needsprojectbuild.message"); //$NON-NLS-1$
		}
		return new String[] { title, message };
	}
		
}
