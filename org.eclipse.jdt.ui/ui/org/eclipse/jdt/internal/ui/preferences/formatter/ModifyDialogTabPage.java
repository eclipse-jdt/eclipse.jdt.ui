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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.PixelConverter;


public abstract class ModifyDialogTabPage {
	
	protected final Observer fUpdater= new Observer() {
		public void update(Observable o, Object arg) {
			updatePreview();
		}
	};
	
	protected abstract class Preference extends Observable {
	    private final Map fPreferences;
	    private boolean fEnabled;
	    private String fKey;
	    
	    public Preference(Map preferences, String key) {
	        fPreferences= preferences;
	        fEnabled= true;
	        fKey= key;
	    }
	    
	    protected final Map getPreferences() {
	        return fPreferences;
	    }
	    
	    public final void setEnabled(boolean enabled) {
	        fEnabled= enabled;
	        updateWidget();
	    }
	    
	    public final boolean getEnabled() {
	        return fEnabled;
	    }
	    
	    public final void setKey(String key) {
	        if (key == null || !fKey.equals(key)) {
	            fKey= key;
	            updateWidget();
	        }
	    }
	    
	    public final String getKey() {
	        return fKey;
	    }
	    
	    protected abstract void updateWidget();
	}
	
	
	protected final class CheckboxPreference extends Preference {
		private final String[] fValues;
		private final Button fCheckbox;
		
		public CheckboxPreference(Composite composite, int numColumns,
								  Map preferences, String key, 
								  String [] values, String text) {
		    super(preferences, key);
		    if (values == null || text == null) 
		        throw new IllegalArgumentException(FormatterMessages.getString("ModifyDialogTabPage.error_msg.values_text_unassigned")); //$NON-NLS-1$
			fValues= values;

			fCheckbox= new Button(composite, SWT.CHECK);
			fCheckbox.setText(text);
			fCheckbox.setLayoutData(createGridData(numColumns, GridData.FILL_HORIZONTAL));
			
			updateWidget();

			fCheckbox.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					checkboxChecked(((Button)e.widget).getSelection());
				}
			});
		}
		
		protected void checkboxChecked(boolean state) {
			getPreferences().put(getKey(), state ? fValues[1] : fValues[0]);
			setChanged();
			notifyObservers();
		}
		
		protected void updateWidget() {
			if (getKey() != null) {
				fCheckbox.setEnabled(getEnabled());
				fCheckbox.setSelection(getChecked());
			} else {
				fCheckbox.setSelection(false);
				fCheckbox.setEnabled(false);
			}
		}
		
		public boolean getChecked() {
			return fValues[1].equals(getPreferences().get(getKey()));
		}
	}
	
	
	
	protected final class ComboPreference extends Preference {
		private final String [] fItems;
		private final String[] fValues;
		private final Combo fCombo;
		
		public ComboPreference(Composite composite, int numColumns,
								  Map preferences, String key, 
								  String [] values, String text, String [] items) {
		    super(preferences, key);
		    if (values == null || items == null || text == null) 
		        throw new IllegalArgumentException(FormatterMessages.getString("ModifyDialogTabPage.error_msg.values_items_text_unassigned")); //$NON-NLS-1$
			fValues= values;
			fItems= items;
			createLabel(numColumns - 1, composite, text);
			fCombo= new Combo(composite, SWT.SINGLE | SWT.READ_ONLY);
			fCombo.setItems(items);
			fCombo.setLayoutData(createGridData(1, GridData.HORIZONTAL_ALIGN_FILL));			

			updateWidget();

			fCombo.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					comboSelected(((Combo)e.widget).getSelectionIndex());
				}
			});
		}
		
		protected void comboSelected(int index) {
			getPreferences().put(getKey(), fValues[index]);
			setChanged();
			notifyObservers(fValues[index]);
		}
		
		protected void updateWidget() {
			if (getKey() != null) {
				fCombo.setEnabled(getEnabled());
				fCombo.setText(getSelectedItem());
			} else {
				fCombo.setText(""); //$NON-NLS-1$
				fCombo.setEnabled(false);
			}
		}
		
		public String getSelectedItem() {
			final String selected= (String)getPreferences().get(getKey());
			for (int i= 0; i < fValues.length; i++) {
				if (fValues[i].equals(selected)) {
					return fItems[i];
				}
			}
			return ""; //$NON-NLS-1$
		}
	}
	
	
	protected final class NumberPreference extends Preference {
		
		private final int fMinValue, fMaxValue;
		private final Label fNumberLabel;
		private final Text fNumberText;

		protected int fSelected;
        protected int fOldSelected;
        
		public NumberPreference(Composite composite, int numColumns,
							   Map preferences, String key, 
							   int minValue, int maxValue, String text) {
		    super(preferences, key);
		    
			fNumberLabel= createLabel(numColumns - 1, composite, text);
			fNumberText= new Text(composite, SWT.SINGLE | SWT.BORDER | SWT.RIGHT);

			final GridData gd= createGridData(1, GridData.HORIZONTAL_ALIGN_END);
			gd.widthHint= new PixelConverter(composite).convertWidthInCharsToPixels(5);
			fNumberText.setLayoutData(gd);
			
			fMinValue= minValue;
			fMaxValue= maxValue;
			
			updateWidget();
			
			fNumberText.addFocusListener(new FocusListener() {
				public void focusGained(FocusEvent e) {
				    NumberPreference.this.focusGained();
				}
                public void focusLost(FocusEvent e) {
				    NumberPreference.this.focusLost();
				}
			});
			
			fNumberText.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					fieldModified();
				}
			});
		}
		
		private IStatus createErrorStatus() {
		    return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, FormatterMessages.getFormattedString("ModifyDialogTabPage.NumberPreference.error.invalid_value", new String [] {Integer.toString(fMinValue), Integer.toString(fMaxValue)}), null); //$NON-NLS-1$
		    
		}

		protected void focusGained() {
		    fOldSelected= fSelected;
		    fNumberText.setSelection(0, fNumberText.getCharCount());
		}
		
		protected void focusLost() {
		    updateStatus(null);
		    final String input= fNumberText.getText();
		    if (!validInput(input))
		        fSelected= fOldSelected;
		    else
		        fSelected= Integer.parseInt(input);
		    saveSelected();
		    fNumberText.setText(Integer.toString(fSelected));
		}
		
		
		protected void fieldModified() {
		    final String trimInput= fNumberText.getText().trim();
		    final boolean valid= validInput(trimInput);
		    
		    updateStatus(valid ? null : createErrorStatus());

		    if (valid) {
		        final int number= Integer.parseInt(trimInput);
		        if (fSelected != number) {
		            fSelected= number;
		            saveSelected();
		        }
		    }
		}
		
		private boolean validInput(String trimInput) {
		    int number;
		    
		    try {
		        number= Integer.parseInt(trimInput);
		    } catch (Exception x) {
		        return false;
		    }
		    
		    if (number < fMinValue) return false;
		    if (number > fMaxValue) return false;
		    return true;
		}
		
		private void saveSelected() {
			getPreferences().put(getKey(), Integer.toString(fSelected));
			setChanged();
			notifyObservers();
		}
		
//		private void updateNumberText() {
//			try { 
//				int old= Integer.parseInt(fNumberText.getText());
//				if (fSelected == old) {
//					return;
//				}
//			} catch (Exception e) {}
//			fNumberText.setText(Integer.toString(fSelected));
//		}
//		
		
		protected void updateWidget() {
		    final boolean hasKey= getKey() != null;

		    fNumberLabel.setEnabled(hasKey && getEnabled());
			fNumberText.setEnabled(hasKey && getEnabled());

			if (hasKey) {
			    String s= (String)getPreferences().get(getKey());
			    try {
			        fSelected= Integer.parseInt(s);
			    } catch (NumberFormatException e) {
			        final String message= FormatterMessages.getFormattedString("ModifyDialogTabPage.NumberPreference.error.invalid_key", getKey()); //$NON-NLS-1$
			        JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, message, e));
			        s= ""; //$NON-NLS-1$
			    }
			    fNumberText.setText(s);
			} else {
			    fNumberText.setText(""); //$NON-NLS-1$
			}
		}
	}

	
	/**
	 * Constant array for boolean selection 
	 */
	
	protected static String[] FALSE_TRUE = {
		DefaultCodeFormatterConstants.FALSE,
		DefaultCodeFormatterConstants.TRUE
	};	
	

	/**
	 * A pixel converter for layout calculations
	 */
	protected PixelConverter fPixelConverter;
	
	protected final JavaPreview fJavaPreview;

	protected final Map fWorkingValues;
	private final ModifyDialog fModifyDialog;
	
	
	/*
	 * Create a new <code>ModifyDialogTabPage</code>
	 */
	public ModifyDialogTabPage(ModifyDialog modifyDialog, Map workingValues) {
		fWorkingValues= workingValues;
		fModifyDialog= modifyDialog;
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
		createLabel(numColumns, composite, FormatterMessages.getString("ModifyDialogTabPage.preview.label.text"));  //$NON-NLS-1$
		
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


	protected void updateStatus(IStatus status) {
	    fModifyDialog.updateStatus(status);
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
		return "/**\n* " + title + "\n*/\n\n"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	
}






















