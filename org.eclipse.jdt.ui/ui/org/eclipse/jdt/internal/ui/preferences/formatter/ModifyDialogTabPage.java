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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
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
import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.PixelConverter;


public abstract class ModifyDialogTabPage {
	
	private static final int RIGHT_SIDE_WIDTH_HINT_CHARS= 65;
	private static final int LEFT_SIDE_WIDTH_HINT_CHARS= 65;

	
	protected final Observer fUpdater= new Observer() {
		public void update(Observable o, Object arg) {
			doUpdatePreview();
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
	    
	    /**
	     * Returns the main control of a preference, which is mainly used to 
	     * manage the focus. This may be <code>null</code> if the preference doesn't
	     * have a control which can get the focus. 
	     */
	    public abstract Control getControl();
	    
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
		
		public Control getControl() {
			return fCheckbox;
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
		
		public Control getControl() {
			return fCombo;
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
		    if (fSelected != fOldSelected) {
		    	saveSelected();
		    	fNumberText.setText(Integer.toString(fSelected));
		    }
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
		
		public Control getControl() {
			return fNumberText;
		}
	}

	
	/**
	 * This class provides the default way to preserve and re-establish the focus
	 * over multiple modify sessions. Each ModifyDialogTabPage has its own instance,
	 * and it should add all relevant controls upon creation, always in the same sequence.
	 * This established a mapping of controls to indexes, which allows to restore the focus
	 * in a later session. 
	 * The index is saved in the dialog settings, and there is only one common preference for 
	 * all tab pages. It is always the currently active tab page which stores its focus
	 * index. 
	 */
	protected final static class DefaultFocusManager extends FocusAdapter {
		
		private final static String PREF_LAST_FOCUS_INDEX= JavaUI.ID_PLUGIN + "formatter_page.modify_dialog_tab_page.last_focus_index"; //$NON-NLS-1$ 
		
		private final IDialogSettings fDialogSettings;
		
		private final Map fItemMap;
		private final List fItemList;
		
		private int fIndex;
		
		public DefaultFocusManager() {
			fDialogSettings= JavaPlugin.getDefault().getDialogSettings();
			fItemMap= new HashMap();
			fItemList= new ArrayList();
			fIndex= 0;
		}

		public void focusGained(FocusEvent e) {
			fDialogSettings.put(PREF_LAST_FOCUS_INDEX, ((Integer)fItemMap.get(e.widget)).intValue());
		}
		
		public void add(Control control) {
			control.addFocusListener(this);
			fItemList.add(fIndex, control);
			fItemMap.put(control, new Integer(fIndex++));
		}
		
		public void add(Preference preference) {
			final Control control= preference.getControl();
			if (control != null) 
				add(control);
		}
		
		public boolean isUsed() {
			return fIndex != 0;
		}
		
		public void restoreFocus() {
			int index= 0;
			try {
				index= fDialogSettings.getInt(PREF_LAST_FOCUS_INDEX);
				// make sure the value is within the range
				if ((index >= 0) && (index <= fItemList.size() - 1)) {
					((Control)fItemList.get(index)).setFocus();
				}
			} catch (NumberFormatException ex) {
				// this is the first time
			}
		}
	}

	
	protected final DefaultFocusManager fDefaultFocusManager;
	
	
	
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
	
	/**
	 * The Java preview.
	 */
	protected final JavaPreview fJavaPreview;

	/**
	 * The map where the current settings are stored.
	 */
	protected final Map fWorkingValues;
	
	/**
	 * The modify dialog where we can display status messages.
	 */
	private final ModifyDialog fModifyDialog;


	/*
	 * Create a new <code>ModifyDialogTabPage</code>
	 */
	public ModifyDialogTabPage(ModifyDialog modifyDialog, Map workingValues) {
		fWorkingValues= workingValues;
		fModifyDialog= modifyDialog;
		fDefaultFocusManager= new DefaultFocusManager();
		fJavaPreview= new JavaPreview(fWorkingValues);
	}
	
	/**
	 * Create the contents of this tab page. Subclasses cannot override this, 
	 * instead they must implement <code>doCreatePreferences</code>. <code>doCreatePreview</code> may also
	 * be overridden as necessary.
	 */
	public final Composite createContents(Composite parent) {
		
		fPixelConverter = new PixelConverter(parent);
		
		final Composite page = new Composite(parent, SWT.NONE);
		
		final GridLayout pageLayout= createGridLayout(2, true);
		pageLayout.horizontalSpacing= 3 * IDialogConstants.HORIZONTAL_SPACING;
		page.setLayout(pageLayout);
		
		final Composite settingsPane= doCreatePreferences(page);
		settingsPane.setParent(page);
		final GridData settingsGd= new GridData(GridData.FILL_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL);
		settingsGd.widthHint= fPixelConverter.convertWidthInCharsToPixels(LEFT_SIDE_WIDTH_HINT_CHARS);
		settingsPane.setLayoutData(settingsGd);
		
		final Composite previewPane= doCreatePreview(page);
		previewPane.setParent(page);
		final GridData previewGd= new GridData(GridData.FILL_BOTH);
		previewGd.widthHint= fPixelConverter.convertWidthInCharsToPixels(RIGHT_SIDE_WIDTH_HINT_CHARS);
		previewPane.setLayoutData(previewGd);
	
		return page;
	}
	

	/**
	 * Create the left side of the modify dialog. This is meant to be implemented by subclasses. 
	 */
	protected abstract Composite doCreatePreferences(Composite parent);
	

	/**
	 * Create the right side of the modify dialog. By default, the preview is displayed there.
	 */
	protected Composite doCreatePreview(Composite parent) {
		
		final int numColumns= 4;
		
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(createGridLayout(numColumns, false));
		createLabel(numColumns, composite, FormatterMessages.getString("ModifyDialogTabPage.preview.label.text"));  //$NON-NLS-1$
		
		final Control control= fJavaPreview.createContents(composite);
		fDefaultFocusManager.add(control);
		final GridData gd= createGridData(numColumns, GridData.FILL_BOTH);
		gd.widthHint= 0;
		gd.heightHint=0;
		control.setLayoutData(gd);
		
		return composite;
	}

	
	/**
	 * This is called when the page becomes visible. 
	 * Common tasks to do include:
	 * <ul><li>Updating the preview.</li>
	 * <li>Setting the focus</li>
	 * </ul>
	 */
	final public void makeVisible() {
		doUpdatePreview();
	}
	
	/**
	 * Update the preview.
	 */
	protected void doUpdatePreview() {
		fJavaPreview.update();
	}
	
	/**
	 * To be implemented by children. Each children should remember where its last focus was, and
	 * reset it correctly within this method. This method is only called after initialization on the 
	 * first tab page to be displayed in order to restore the focus of the last session.
	 */
	public void setInitialFocus() {
		if (fDefaultFocusManager.isUsed()) {
			fDefaultFocusManager.restoreFocus();
		}
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
		final NumberPreference pref= new NumberPreference(composite, numColumns, fWorkingValues, 
			key, minValue, maxValue, name);
		fDefaultFocusManager.add(pref);
		pref.addObserver(fUpdater);
		return pref;
	}
	
	/**
	 * Create a ComboPreference.
	 */
	protected ComboPreference createComboPref(Composite composite, int numColumns, String name, 
											  String key, String [] values, String [] items) {
		final ComboPreference pref= new ComboPreference(composite, numColumns, 
			fWorkingValues, key, values, name, items);
		fDefaultFocusManager.add(pref);
		pref.addObserver(fUpdater);
		return pref;
	}

	/**
	 * Create a CheckboxPreference.
	 */
	protected CheckboxPreference createCheckboxPref(Composite composite, int numColumns, String name, String key,
													String [] values) {
		final CheckboxPreference pref= new CheckboxPreference(composite, numColumns, 
			fWorkingValues, key, values, name);
		fDefaultFocusManager.add(pref);
		pref.addObserver(fUpdater);
		return pref;
	}
	

	/**
	 * Create the header part for a preview text
	 */
	protected static String createPreviewHeader(String title) {
		return "/**\n* " + title + "\n*/\n\n"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	
}






















