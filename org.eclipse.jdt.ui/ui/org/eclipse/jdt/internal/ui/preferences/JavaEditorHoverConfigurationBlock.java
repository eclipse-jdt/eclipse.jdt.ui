/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
    IBM Corporation - Initial implementation
**********************************************************************/

package org.eclipse.jdt.internal.ui.preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.text.java.hover.JavaEditorTextHoverDescriptor;

/**
 * Configures Java Editor hover preferences.
 * 
 * @since 2.1
 */
class JavaEditorHoverConfigurationBlock {


	// Data structure to hold the values which are edited by the user
	private static class HoverConfig {
		
		private String fModifierString;
		private boolean fIsEnabled;
		private int fStateMask;

		private HoverConfig(String modifier, int stateMask, boolean enabled) {
			fModifierString= modifier;
			fIsEnabled= enabled;
			fStateMask= stateMask;
		}
	}

	private IPreferenceStore fStore;
	private HoverConfig[] fHoverConfigs;
	private Text fModifierEditor;
	private Button fEnableField;
	private List fHoverList;
	private Text fDescription;
	
	private JavaEditorPreferencePage fMainPreferencePage;

	private StatusInfo fStatus;

	public JavaEditorHoverConfigurationBlock(JavaEditorPreferencePage mainPreferencePage, IPreferenceStore store) {
		Assert.isNotNull(mainPreferencePage);
		Assert.isNotNull(store);
		fMainPreferencePage= mainPreferencePage;
		fStore= store;
	}

	/**
	 * Creates page for hover preferences.
	 */
	public Control createControl(Composite parent) {

		Composite hoverComposite= new Composite(parent, SWT.NULL);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		hoverComposite.setLayout(layout);
		GridData gd= new GridData(GridData.GRAB_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);
		hoverComposite.setLayoutData(gd);

		Label label= new Label(hoverComposite, SWT.NONE);
		label.setText("&Hover key modifier preferences:");
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		gd.horizontalSpan= 2;
		label.setLayoutData(gd);
		gd= new GridData(GridData.GRAB_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);

		// Hover list
		fHoverList= new List(hoverComposite, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
		gd= new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
		gd.heightHint= convertHeightInCharsToPixels(parent, 10);
		fHoverList.setLayoutData(gd);
		fHoverList.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				handleHoverListSelection();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});


		Composite stylesComposite= new Composite(hoverComposite, SWT.NONE);
		layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		stylesComposite.setLayout(layout);
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint= convertHeightInCharsToPixels(parent, 10) + (2 * fHoverList.getBorderWidth());
		stylesComposite.setLayoutData(gd);

		// Enabled checkbox		
		fEnableField= new Button(stylesComposite, SWT.CHECK);
		fEnableField.setText("&Enabled");
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		gd.horizontalSpan= 2;
		fEnableField.setLayoutData(gd);
		fEnableField.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				int i= fHoverList.getSelectionIndex();
				boolean state= fEnableField.getSelection();
				fModifierEditor.setEnabled(state);
				fHoverConfigs[i].fIsEnabled= state;
				handleModifierModified();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		// Text field for modifier string
		label= new Label(stylesComposite, SWT.LEFT);
		label.setText("Key &Modifier:");
		fModifierEditor= new Text(stylesComposite, SWT.BORDER);
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		fModifierEditor.setLayoutData(gd);

		// XXX: work in progress
		fModifierEditor.addKeyListener(new KeyListener() {
			boolean isModifier;
			public void keyPressed(KeyEvent e) {
				isModifier= e.keyCode > 0 && e.character == 0 && e.stateMask == 0;
			}
		
			public void keyReleased(KeyEvent e) {
				if (isModifier && e.stateMask > 0 && e.stateMask == e.stateMask && e.character == 0) {// && e.time -time < 1000) {
					String text= fModifierEditor.getText();
					if (text.length() > 0)
						text= text + " + ";
					text= text + Action.findModifierString(e.stateMask);
					fModifierEditor.setText(text);
					fModifierEditor.setSelection(text.length(), text.length());
				}
			}
		});

		fModifierEditor.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				handleModifierModified();
			}
		});

		// Description
		Label descriptionLabel= new Label(stylesComposite, SWT.LEFT);
		descriptionLabel.setText("Description:");
		gd= new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		gd.horizontalSpan= 2;
		descriptionLabel.setLayoutData(gd);
		fDescription= new Text(stylesComposite, SWT.LEFT | SWT.WRAP | SWT.MULTI | SWT.READ_ONLY | SWT.BORDER);
		gd= new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan= 2;
		fDescription.setLayoutData(gd);

		initialize();

		return hoverComposite;
	}

	private JavaEditorTextHoverDescriptor[] getContributedHovers() {
		return JavaPlugin.getDefault().getJavaEditorTextHoverDescriptors();		
	}

	void initialize() {
		JavaEditorTextHoverDescriptor[] hoverDescs= getContributedHovers();
		fHoverConfigs= new HoverConfig[hoverDescs.length];
		for (int i= 0; i < hoverDescs.length; i++) {
			fHoverConfigs[i]= new HoverConfig(hoverDescs[i].getModifierString(), hoverDescs[i].getStateMask(), hoverDescs[i].isEnabled());
			fHoverList.add(hoverDescs[i].getLabel());
		}
		initializeFields();
	}

	void initializeFields() {
		fHoverList.getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (fHoverList != null && !fHoverList.isDisposed()) {
					fHoverList.select(0);
					handleHoverListSelection();
				}
			}
		});
	}

	void performOk() {
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < fHoverConfigs.length; i++) {
			buf.append(getContributedHovers()[i].getId());
			buf.append(JavaEditorTextHoverDescriptor.VALUE_SEPARATOR);
			if (!fHoverConfigs[i].fIsEnabled)
				buf.append(JavaEditorTextHoverDescriptor.DISABLED_TAG);
			String modifier= fHoverConfigs[i].fModifierString;
			if (modifier == null || modifier.length() == 0)
				modifier= JavaEditorTextHoverDescriptor.NO_MODIFIER;
			buf.append(modifier);
			buf.append(JavaEditorTextHoverDescriptor.VALUE_SEPARATOR);
		}
		fStore.setValue(PreferenceConstants.EDITOR_TEXT_HOVER_MODIFIERS, buf.toString());
		JavaPlugin.getDefault().resetJavaEditorTextHoverDescriptors();
	}

	void performDefaults() {
		restoreFromPreferences();
		initializeFields();
	}

	private void restoreFromPreferences() {
		String compiledTextHoverModifiers= fStore.getString(PreferenceConstants.EDITOR_TEXT_HOVER_MODIFIERS);
		
		StringTokenizer tokenizer= new StringTokenizer(compiledTextHoverModifiers, JavaEditorTextHoverDescriptor.VALUE_SEPARATOR);
		HashMap idToModifier= new HashMap(tokenizer.countTokens() / 2);

		while (tokenizer.hasMoreTokens()) {
			String id= tokenizer.nextToken();
			if (tokenizer.hasMoreTokens())
				idToModifier.put(id, tokenizer.nextToken());
		}

		for (int i= 0; i < fHoverConfigs.length; i++) {
			String modifierString= (String)idToModifier.get(getContributedHovers()[i].getId());
			boolean enabled= true;
			if (modifierString == null)
				modifierString= JavaEditorTextHoverDescriptor.DISABLED_TAG;
			
			if (modifierString.startsWith(JavaEditorTextHoverDescriptor.DISABLED_TAG)) {
				enabled= false;
				modifierString= modifierString.substring(1);
			}

			if (modifierString.equals(JavaEditorTextHoverDescriptor.NO_MODIFIER))
				modifierString= ""; //$NON-NLS-1$

			fHoverConfigs[i].fModifierString= modifierString;
			fHoverConfigs[i].fIsEnabled= enabled;
			fHoverConfigs[i].fStateMask= JavaEditorTextHoverDescriptor.computeStateMask(modifierString);
		}
	}

	private void handleModifierModified() {
		int i= fHoverList.getSelectionIndex();
		String modifiers= fModifierEditor.getText();
		fHoverConfigs[i].fModifierString= modifiers;
		fHoverConfigs[i].fStateMask= JavaEditorTextHoverDescriptor.computeStateMask(modifiers);
		if (fHoverConfigs[i].fIsEnabled && fHoverConfigs[i].fStateMask == -1)
			fStatus= new StatusInfo(StatusInfo.ERROR, "Modifier '" + fHoverConfigs[i].fModifierString + "' is not valid.");
		else
			fStatus= new StatusInfo();
		updateStatus();
	}

	private void handleHoverListSelection() {	
		int i= fHoverList.getSelectionIndex();
		boolean enabled= fHoverConfigs[i].fIsEnabled;
		fEnableField.setSelection(enabled);
		fModifierEditor.setEnabled(enabled);
		fModifierEditor.setText(fHoverConfigs[i].fModifierString);
		String description= getContributedHovers()[i].getDescription();
		if (description == null)
			description= ""; //$NON-NLS-1$
		fDescription.setText(description);
	}

	private int convertWidthInCharsToPixels(Control control, int chars) {
		GC gc = new GC(control);
		gc.setFont(control.getFont());
		FontMetrics fontMetrics = gc.getFontMetrics();
		gc.dispose();
		if (fontMetrics == null)
			return 0;
		return Dialog.convertWidthInCharsToPixels(fontMetrics, chars);
	}

	private int convertHeightInCharsToPixels(Control control, int chars) {
		GC gc = new GC(control);
		gc.setFont(control.getFont());
		FontMetrics fontMetrics = gc.getFontMetrics();
		gc.dispose();
		if (fontMetrics == null)
			return 0;
		return Dialog.convertHeightInCharsToPixels(fontMetrics, chars);
	}

	IStatus getStatus() {
		if (fStatus == null)
			fStatus= new StatusInfo();
		return fStatus;
	}

	private void updateStatus() {
		int i= 0;
		HashMap stateMasks= new HashMap(fHoverConfigs.length);
		while (fStatus.isOK() && i < fHoverConfigs.length) {
			if (fHoverConfigs[i].fIsEnabled) {
				String label= getContributedHovers()[i].getLabel();
				Integer stateMask= new Integer(fHoverConfigs[i].fStateMask);
				if (fHoverConfigs[i].fStateMask == -1)
					fStatus= new StatusInfo(StatusInfo.ERROR, "Modifier '" + fHoverConfigs[i].fModifierString + "' for '" + label + "' hover is not valid.");
				else if (stateMasks.containsKey(stateMask))
					fStatus= new StatusInfo(StatusInfo.ERROR, "'" + label + "' hover uses the same modifier as '" +  stateMasks.get(stateMask) + "' hover.");
				else
					stateMasks.put(stateMask, label);
			}
			i++;
		}

		if (fStatus.isOK())
			fMainPreferencePage.updateStatus(fStatus);
		else {
			fMainPreferencePage.setValid(false);
			StatusUtil.applyToStatusLine(fMainPreferencePage, fStatus);
		}
	}
}
