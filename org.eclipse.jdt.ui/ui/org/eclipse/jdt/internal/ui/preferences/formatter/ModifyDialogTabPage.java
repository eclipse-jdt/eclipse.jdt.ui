/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.IDialogConstants;

import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.ui.util.PixelConverter;

/**
 * @author sib
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public abstract class ModifyDialogTabPage {
	
	
	protected final Observer fUpdater= new Observer() {
		public void update(Observable o, Object arg) {
			updatePreview();
		}
	};
	
	
	protected static class CheckboxPreference extends Observable {
		private final Map fPreferences;
		private String fKey;
		private boolean fEnabled;
		private final String[] fValues;
		private final Button fCheckbox;
		
		public CheckboxPreference(Composite composite, int numColumns,
								  Map preferences, String key, 
								  String [] values, String text) {
			fEnabled= true;
			fPreferences= preferences;
			fCheckbox= new Button(composite, SWT.CHECK);
			fCheckbox.setText(text);
			fCheckbox.setLayoutData(createGridData(numColumns, GridData.FILL_HORIZONTAL));
			fValues= values;
			setKey(key);

			fCheckbox.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					checkboxChecked(((Button)e.widget).getSelection());
				}

				public void widgetDefaultSelected(SelectionEvent e) {}
			});
		}
		
		protected void checkboxChecked(boolean state) {
			fPreferences.put(fKey, state ? fValues[1] : fValues[0]);
			setChanged();
			notifyObservers();
		}
		
		public void setKey(String key) {
			fKey= key;
			updateWidget();
		}
		
		protected void updateWidget() {
			if (fKey != null) {
				fCheckbox.setEnabled(fEnabled);
				fCheckbox.setSelection(getChecked());
			} else {
				fCheckbox.setSelection(false);
				fCheckbox.setEnabled(false);
			}
		}
		
		public boolean getChecked() {
			return fValues[1].equals(fPreferences.get(fKey));
		}
		public void setEnabled(boolean enabled) {
			fEnabled= enabled;
			updateWidget();
		}
		public boolean getEnabled() {
			return fEnabled;
		}
	}
	
	
	
	protected static class ComboPreference extends Observable {
		private final Map fPreferences;
		private String fKey;
		private final String [] fItems;
		private final String[] fValues;
		private Combo fCombo;
		
		public ComboPreference(Composite composite, int numColumns,
								  Map preferences, String key, 
								  String [] values, String text, String [] items) {
			fPreferences= preferences;
			fValues= values;
			fItems= items;
			createLabel(numColumns - 1, composite, text);
			fCombo= new Combo(composite, SWT.SINGLE | SWT.READ_ONLY);
			fCombo.setItems(items);
			fCombo.setLayoutData(createGridData(1, GridData.HORIZONTAL_ALIGN_FILL));			
			setKey(key);

			fCombo.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					comboSelected(((Combo)e.widget).getSelectionIndex());
				}

				public void widgetDefaultSelected(SelectionEvent e) {}
			});
		}
		
		protected void comboSelected(int index) {
			fPreferences.put(fKey, fValues[index]);
			setChanged();
			notifyObservers(fValues[index]);
		}
		
		public void setKey(String key) {
			fKey= key;
			updateWidget();
		}
		
		protected void updateWidget() {
			if (fKey != null && fPreferences != null && fValues != null) {
				fCombo.setEnabled(true);
				fCombo.setText(getSelectedItem());
			} else {
				fCombo.setText("");
				fCombo.setEnabled(false);
			}
		}
		
		public String getSelectedItem() {
			final String selected= (String)fPreferences.get(fKey);
			for (int i= 0; i < fValues.length; i++) {
				if (fValues[i].equals(selected)) {
					return fItems[i];
				}
			}
			return "";
		}
	}
	
	
	protected static class NumberPreference extends Observable {
		
		private final Map fPreferences;
		protected final int fMinValue, fMaxValue;
		
		private int fSelected;
		private String fKey;
		
		private final Text fNumberText;
		
		public NumberPreference(Composite composite, int numColumns,
							   Map preferences, String key, 
							   int minValue, int maxValue, String text) {
			
			createLabel(numColumns - 1, composite, text);
			
			Composite buttons= new Composite( composite, SWT.NONE);
			buttons.setLayout(createGridLayout(3, false));
			buttons.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

			fNumberText= new Text(buttons, SWT.SINGLE | SWT.BORDER | SWT.RIGHT);
			
			fMinValue= minValue;
			fMaxValue= maxValue;
			
			fPreferences= preferences;
			
			setKey(key); 

			final GridData gd= createGridData(1);
			gd.widthHint= new PixelConverter(composite).convertWidthInCharsToPixels(5);
			fNumberText.setLayoutData(gd);
			
			fNumberText.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					int newNumber;
					try {
						newNumber= Integer.parseInt(((Text)e.widget).getText());
					} catch (Exception ex) { return; }
					numberChanged(newNumber);
				}
			});
		}
		
		
		protected void numberChanged(int number) {

			if (number < fMinValue) return;
			if (number > fMaxValue) return;
			
			if (number != fSelected) {
				fSelected= number;
				updatePreferences();
				updateNumberText();
			}
		}
		
		protected void updatePreferences() {
			fPreferences.put(fKey, Integer.toString(fSelected));
			setChanged();
			notifyObservers();
		}
		
		protected void updateNumberText() {
			try { 
				int old= Integer.parseInt(fNumberText.getText());
				if (fSelected == old) {
					return;
				}
			} catch (Exception e) {}
			fNumberText.setText(Integer.toString(fSelected));
		}
		

		public void setKey(String newKey) {
			if (newKey == fKey) return;
			fKey= newKey;
			
			if (fKey != null) {
				final String s= (String)fPreferences.get(fKey);
				fSelected= Integer.parseInt(s);
				fNumberText.setText(s);
				fNumberText.setEnabled(true);
			} else {
				fNumberText.setEnabled(false);
				fNumberText.setText("");
			}			
		}
	}

	
	/**
	 * Constant array for boolean selection 
	 */
	
	protected static String[] falseTrue = {
		DefaultCodeFormatterConstants.FALSE,
		DefaultCodeFormatterConstants.TRUE
	};	
	

	/**
	 * A pixel converter for layout calculations
	 */
	protected PixelConverter fPixelConverter;
	
	protected final JavaPreview fJavaPreview;

	protected final Map fWorkingValues;
	
	
	/*
	 * Create a new <code>ModifyDialogTabPage</code>
	 */
	public ModifyDialogTabPage(Map workingValues) {
		fWorkingValues= workingValues;
		fJavaPreview= new JavaPreview(fWorkingValues);
	}
	
	/**
	 * Create the contents of this tab page. Subclasses cannot override this, 
	 * instead they must implement <code>doCreatePreferences</code>. <code>doCreatePreview</code> may also
	 * be overridden as necessary.
	 */
	public final Composite createContents(Composite parent) {
		
		GridData gd;
		
		fPixelConverter = new PixelConverter(parent);
		
		final Composite page= new Composite(parent, SWT.NONE);
		
		final GridLayout pageLayout= createGridLayout(2, true);
		pageLayout.horizontalSpacing= 3 * IDialogConstants.HORIZONTAL_SPACING;
		page.setLayout(pageLayout);
		
		final Composite settingsPane= doCreatePreferences(page);
		settingsPane.setParent(page);
		gd= new GridData(GridData.FILL_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL);
		gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(60);
		settingsPane.setLayoutData(gd);
		
		final Composite previewPane= doCreatePreview(page);
		previewPane.setParent(page);
		gd= new GridData(GridData.FILL_BOTH);
		previewPane.setLayoutData(gd);
	
		doInitializeControls();
		
		return page;
	}
	

	/**
	 * Create the left side of the modify dialog. This is meant to be implemented by subclasses. 
	 */
	protected abstract Composite doCreatePreferences(Composite parent);
	
	
	/**
	 * Can be used to initialize controls as well as listeners. This is guaranteed to be called
	 * after <code>createContents</code>.
	 */
	protected void doInitializeControls() {
	}
	

	/**
	 * Create the right side of the modify dialog. By default, the preview is displayed there.
	 */
	protected Composite doCreatePreview(Composite parent) {
		
		final int numColumns= 4;
		
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(createGridLayout(numColumns, false));
		createLabel(numColumns, composite, "Preview:"); 
		
		final Control control= fJavaPreview.createContents(composite);
		final GridData gd= createGridData(numColumns, GridData.FILL_BOTH);
		gd.widthHint= 0;
		gd.heightHint=0;
		control.setLayoutData(gd);
		
		return composite;
	}

	
	/**
	 * Update the preview.
	 */
	protected void updatePreview() {
		fJavaPreview.update();
	}


	
	/**
	 * Factory methods to make GUI construction easier
	 */
	
	/**
	 * Create a GridLayout with the default margin and spacing settings, as
	 * well as the specified number of columns.
	 */
	protected static GridLayout createGridLayout(int numColumns, boolean margins) {
		final GridLayout layout= new GridLayout(numColumns, false);
		layout.verticalSpacing= IDialogConstants.VERTICAL_SPACING;
		layout.horizontalSpacing= IDialogConstants.HORIZONTAL_SPACING;
		if (margins) {
			layout.marginHeight= IDialogConstants.VERTICAL_MARGIN;
			layout.marginWidth= IDialogConstants.HORIZONTAL_MARGIN;
		} else {
			layout.marginHeight= 0;
			layout.marginWidth= 0;
		}
		return layout;
	}

	/**
	 * Create a GridData.
	 */
	protected static GridData createGridData(int numColumns ) {
		final GridData gd= new GridData();
		gd.horizontalSpan= numColumns;
		return gd;
	}
	
	/**
	 * Create a GridData.
	 */
	protected static GridData createGridData(int numColumns, int style) {
		final GridData gd= new GridData(style);
		gd.horizontalSpan= numColumns;
		return gd;		
	}
	
	/** 
	 * Create a label.  
	 */
	protected static Label createLabel(int numColumns, Composite parent, String text) {
		return createLabel( numColumns, parent, text, GridData.FILL_HORIZONTAL);
	}
	
	/** 
	 * Create a label
	 */
	protected static Label createLabel(int numColumns, Composite parent, String text, int gridDataStyle) {
		final Label label= new Label(parent, SWT.WRAP);
		label.setText(text);
		label.setLayoutData(createGridData(numColumns, gridDataStyle));
		return label;
	}

	/**
	 * Create a group
	 */
	protected static Group createGroup(int numColumns, Composite parent, String text ) {
		final Group group= new Group(parent, SWT.NONE);
		group.setLayoutData(createGridData(numColumns, GridData.FILL_HORIZONTAL));
		group.setLayout(createGridLayout(numColumns, true));
		group.setText(text);
		return group;
	}
	

	/**
	 * Create a NumberPreference.
	 */
	protected NumberPreference createNumberPref(Composite composite, int numColumns, String name, String key,
												int minValue, int maxValue) {
		final NumberPreference numPref= new NumberPreference(composite, numColumns, fWorkingValues, 
				key, minValue, maxValue, name);
		numPref.addObserver(fUpdater);
		return numPref;
	}
	
	/**
	 * Create a ComboPreference.
	 */
	protected ComboPreference createComboPref(Composite composite, int numColumns, String name, 
											  String key, String [] values, String [] items) {
		final ComboPreference comboPref= new ComboPreference(composite, numColumns, 
				fWorkingValues, key, values, name, items);
		comboPref.addObserver(fUpdater);
		return comboPref;
	}

	/**
	 * Create a CheckboxPreference.
	 */
	protected CheckboxPreference createCheckboxPref(Composite composite, int numColumns, String name, String key,
													String [] values) {
		final CheckboxPreference cp= new CheckboxPreference(composite, numColumns, 
				fWorkingValues, key, values, name);
		cp.addObserver(fUpdater);
		return cp;
	}
	

	/**
	 * Create the header part for a preview text
	 */
	protected static String createPreviewHeader(String title) {
		return "/**\n* " + title + "\n*/\n\n";
	}

	
}






















