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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;

import org.eclipse.jface.text.Assert;

import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;

import org.eclipse.ui.internal.dialogs.WorkbenchPreferenceDialog;
import org.eclipse.ui.internal.editors.text.CHyperLink;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.util.PixelConverter;

/**
 * Configures Java Editor typing preferences.
 * 
 * @since 3.1
 */
abstract class AbstractConfigurationBlock implements IPreferenceConfigurationBlock {

	protected final class SectionManager {
		private Set fSections= new HashSet();
		private boolean fIsBeingManaged= false;
		private ExpansionAdapter fListener= new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				ExpandableComposite source= (ExpandableComposite) e.getSource();
				updateSectionStyle(source);
				if (fIsBeingManaged)
					return;
				if (e.getState()) {
					try {
						fIsBeingManaged= true;
						for (Iterator iter= fSections.iterator(); iter.hasNext();) {
							ExpandableComposite composite= (ExpandableComposite) iter.next();
							if (composite != source)
								composite.setExpanded(false);
						}
					} finally {
						fIsBeingManaged= false;
					}
				}
				ExpandableComposite exComp= getParentExpandableComposite(source);
				if (exComp != null)
					exComp.layout(true, true);
				ScrolledPageContent parentScrolledComposite= getParentScrolledComposite(source);
				if (parentScrolledComposite != null) {
					parentScrolledComposite.reflow(true);
				}
			}
		};
		private Composite fBody;
		public SectionManager() {
		}
		private void manage(ExpandableComposite section) {
			if (section == null)
				throw new NullPointerException();
			if (fSections.add(section))
				section.addExpansionListener(fListener);
		}
		public Composite createStyleSection(String label) {
			Assert.isNotNull(fBody);
			final ExpandableComposite excomposite= new ExpandableComposite(fBody, SWT.NONE, ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT);
			excomposite.setText(label);
			excomposite.setExpanded(false);
			excomposite.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
			excomposite.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
			
			updateSectionStyle(excomposite);
			manage(excomposite);
			
			Composite contents= new Composite(excomposite, SWT.NONE);
			excomposite.setClient(contents);
			
			return contents;
		}
		public Composite createSectionComposite(Composite parent) {
			Assert.isTrue(fBody == null);
			boolean isNested= isNestedInScrolledComposite(parent);
			Composite composite;
			if (isNested) {
				composite= new Composite(parent, SWT.NONE);
				fBody= composite;
			} else {
				composite= new ScrolledPageContent(parent);
				fBody= ((ScrolledPageContent) composite).getBody();
			}
			
			fBody.setLayout(new GridLayout());
			
			return composite;
		}
	}

	protected static final int INDENT= 0;
	private OverlayPreferenceStore fStore;
	
	private Map fCheckBoxes= new HashMap();
	private SelectionListener fCheckBoxListener= new SelectionListener() {
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		public void widgetSelected(SelectionEvent e) {
			Button button= (Button) e.widget;
			fStore.setValue((String) fCheckBoxes.get(button), button.getSelection());
		}
	};
	
	
	private Map fTextFields= new HashMap();
	private ModifyListener fTextFieldListener= new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			Text text= (Text) e.widget;
			fStore.setValue((String) fTextFields.get(text), text.getText());
		}
	};

	private ArrayList fNumberFields= new ArrayList();
	private ModifyListener fNumberFieldListener= new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			numberFieldChanged((Text) e.widget);
		}
	};
	
	/**
	 * List of master/slave listeners when there's a dependency.
	 * 
	 * @see #createDependency(Button, String, Control)
	 * @since 3.0
	 */
	private ArrayList fMasterSlaveListeners= new ArrayList();
	
	private StatusInfo fStatus;
	private final PreferencePage fMainPage;

	public AbstractConfigurationBlock(OverlayPreferenceStore store) {
		Assert.isNotNull(store);
		fStore= store;
		fMainPage= null;
		
		fStore.addKeys(createOverlayStoreKeys());
	}
	
	public AbstractConfigurationBlock(OverlayPreferenceStore store, PreferencePage mainPreferencePage) {
		Assert.isNotNull(store);
		Assert.isNotNull(mainPreferencePage);
		fStore= store;
		fMainPage= mainPreferencePage;
		
		fStore.addKeys(createOverlayStoreKeys());
	}

	protected abstract OverlayPreferenceStore.OverlayKey[] createOverlayStoreKeys();

	protected final ScrolledPageContent getParentScrolledComposite(Control control) {
		Control parent= control.getParent();
		while (!(parent instanceof ScrolledPageContent) && parent != null) {
			parent= parent.getParent();
		}
		if (parent instanceof ScrolledPageContent) {
			return (ScrolledPageContent) parent;
		}
		return null;
	}

	private final ExpandableComposite getParentExpandableComposite(Control control) {
		Control parent= control.getParent();
		while (!(parent instanceof ExpandableComposite) && parent != null) {
			parent= parent.getParent();
		}
		if (parent instanceof ExpandableComposite) {
			return (ExpandableComposite) parent;
		}
		return null;
	}

	protected void updateSectionStyle(ExpandableComposite excomposite) {
	}
	
	private void makeScrollableCompositeAware(Control control) {
		ScrolledPageContent parentScrolledComposite= getParentScrolledComposite(control);
		if (parentScrolledComposite != null) {
			parentScrolledComposite.adaptChild(control);
		}
	}
	
	private boolean isNestedInScrolledComposite(Composite parent) {
		return getParentScrolledComposite(parent) != null;
	}
	
	protected Control createLinkText(Composite contents, Object[] tokens) {
		Composite description= new Composite(contents, SWT.NONE);
		RowLayout rowLayout= new RowLayout(SWT.HORIZONTAL);
		rowLayout.justify= false;
		rowLayout.fill= true;
		rowLayout.marginBottom= 0;
		rowLayout.marginHeight= 0;
		rowLayout.marginLeft= 0;
		rowLayout.marginRight= 0;
		rowLayout.marginTop= 0;
		rowLayout.marginWidth= 0;
		rowLayout.spacing= 0;
		rowLayout.pack= true;
		rowLayout.wrap= true;
		description.setLayout(rowLayout);
		
		for (int i= 0; i < tokens.length; i++) {
			String text;
			if (tokens[i] instanceof String[]) {
				String[] strings= (String[]) tokens[i];
				text= strings[0];
				final String target= strings[1];
				CHyperLink link= new CHyperLink(description, SWT.NONE);
				link.setText(text);
				link.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
								WorkbenchPreferenceDialog.createDialogOn(target);
						}
				});
				if (strings.length > 2)
					link.setToolTipText(strings[2]);
				continue;
			}
			
			text= (String) tokens[i];
			StringTokenizer tokenizer= new StringTokenizer(text);
			while (tokenizer.hasMoreTokens()) {
				Label label= new Label(description, SWT.NONE);
				String token= tokenizer.nextToken();
				label.setText(token + " "); //$NON-NLS-1$
			}
		}
		
		return description;
	}

	protected Button addCheckBox(Composite parent, String label, String key, int indentation) {		
		Button checkBox= new Button(parent, SWT.CHECK);
		checkBox.setText(label);
		
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent= indentation;
		gd.horizontalSpan= 2;
		checkBox.setLayoutData(gd);
		checkBox.addSelectionListener(fCheckBoxListener);
		makeScrollableCompositeAware(checkBox);

		fCheckBoxes.put(checkBox, key);
		
		return checkBox;
	}

	/**
	 * Returns an array of size 2:
	 *  - first element is of type <code>Label</code>
	 *  - second element is of type <code>Text</code>
	 * Use <code>getLabelControl</code> and <code>getTextControl</code> to get the 2 controls.
	 * 
	 * @param composite 	the parent composite
	 * @param label			the text field's label
	 * @param key			the preference key
	 * @param textLimit		the text limit
	 * @param indentation	the field's indentation
	 * @param isNumber		<code>true</code> iff this text field is used to e4dit a number
	 * @return the controls added
	 */
	protected Control[] addLabelledTextField(Composite composite, String label, String key, int textLimit, int indentation, boolean isNumber) {
		
		PixelConverter pixelConverter= new PixelConverter(composite);
		
		Label labelControl= new Label(composite, SWT.NONE);
		labelControl.setText(label);
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent= indentation;
		labelControl.setLayoutData(gd);
		
		Text textControl= new Text(composite, SWT.BORDER | SWT.SINGLE);		
		gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.widthHint= pixelConverter.convertWidthInCharsToPixels(textLimit + 1);
		textControl.setLayoutData(gd);
		textControl.setTextLimit(textLimit);
		fTextFields.put(textControl, key);
		if (isNumber) {
			fNumberFields.add(textControl);
			textControl.addModifyListener(fNumberFieldListener);
		} else {
			textControl.addModifyListener(fTextFieldListener);
		}
			
		return new Control[]{labelControl, textControl};
	}

	protected void createDependency(final Button master, String masterKey, final Control slave) {
		indent(slave);
		boolean masterState= fStore.getBoolean(masterKey);
		slave.setEnabled(masterState);
		SelectionListener listener= new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				slave.setEnabled(master.getSelection());
			}

			public void widgetDefaultSelected(SelectionEvent e) {}
		};
		master.addSelectionListener(listener);
		fMasterSlaveListeners.add(listener);
	}

	protected static void indent(Control control) {
		((GridData) control.getLayoutData()).horizontalIndent+= INDENT;
	}

	public void initialize() {
		initializeFields();
	}

	protected void initializeFields() {
		
		Iterator iter= fCheckBoxes.keySet().iterator();
		while (iter.hasNext()) {
			Button b= (Button) iter.next();
			String key= (String) fCheckBoxes.get(b);
			b.setSelection(fStore.getBoolean(key));
		}
		
		iter= fTextFields.keySet().iterator();
		while (iter.hasNext()) {
			Text t= (Text) iter.next();
			String key= (String) fTextFields.get(t);
			t.setText(fStore.getString(key));
		}
		
        // Update slaves
        iter= fMasterSlaveListeners.iterator();
        while (iter.hasNext()) {
            SelectionListener listener= (SelectionListener)iter.next();
            listener.widgetSelected(null);
        }
     
        updateStatus(new StatusInfo());
	}

	public void performOk() {
	}

	public void performDefaults() {
		initializeFields();
	}

	IStatus getStatus() {
		if (fStatus == null)
			fStatus= new StatusInfo();
		return fStatus;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.IPreferenceConfigurationBlock#dispose()
	 * @since 3.0
	 */
	public void dispose() {
	}
	
	private void numberFieldChanged(Text textControl) {
		String number= textControl.getText();
		IStatus status= validatePositiveNumber(number);
		if (!status.matches(IStatus.ERROR))
			fStore.setValue((String) fTextFields.get(textControl), number);
		updateStatus(status);
	}
	
	private IStatus validatePositiveNumber(String number) {
		StatusInfo status= new StatusInfo();
		if (number.length() == 0) {
			status.setError(PreferencesMessages.getString("JavaEditorPreferencePage.empty_input")); //$NON-NLS-1$
		} else {
			try {
				int value= Integer.parseInt(number);
				if (value < 0)
					status.setError(PreferencesMessages.getFormattedString("JavaEditorPreferencePage.invalid_input", number)); //$NON-NLS-1$
			} catch (NumberFormatException e) {
				status.setError(PreferencesMessages.getFormattedString("JavaEditorPreferencePage.invalid_input", number)); //$NON-NLS-1$
			}
		}
		return status;
	}

	private void updateStatus(IStatus status) {
		if (fMainPage == null)
			return;
		fMainPage.setValid(status.isOK());
		StatusUtil.applyToStatusLine(fMainPage, status);
	}
	
	protected final OverlayPreferenceStore getPreferenceStore() {
		return fStore;
	}
}
