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
import java.util.Map;
import java.util.StringTokenizer;

import org.osgi.service.prefs.BackingStoreException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;

import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;

/**
 * Abstract options configuration block providing a general implementation for setting up
 * an options configuration page.
 * 
 * @since 2.1
 */
public abstract class OptionsConfigurationBlock {
	
	public static final class Key {
		
		private String fQualifier;
		private String fKey;
		
		public Key(String qualifier, String key) {
			fQualifier= qualifier;
			fKey= key;
		}
		
		public String getName() {
			return fKey;
		}
		
		public String getStoredValue(IScopeContext context) {
			return context.getNode(fQualifier).get(fKey, null);
		}
		
		public String getStoredValue(IScopeContext[] lookupOrder) {
			for (int i= 0; i < lookupOrder.length; i++) {
				String value= getStoredValue(lookupOrder[i]);
				if (value != null) {
					return value;
				}
			}
			return null;
		}
		
		public void setStoredValue(IScopeContext context, String value) {
			context.getNode(fQualifier).put(fKey, value);
		}
		
		public void removeFromStore(IScopeContext context) {
			context.getNode(fQualifier).remove(fKey);
		}
			
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return fQualifier + '/' + fKey;
		}

		public String getQualifier() {
			return fQualifier;
		}

	}
	

	protected static class ControlData {
		private Key fKey;
		private String[] fValues;
		
		public ControlData(Key key, String[] values) {
			fKey= key;
			fValues= values;
		}
		
		public Key getKey() {
			return fKey;
		}
		
		public String getValue(boolean selection) {
			int index= selection ? 0 : 1;
			return fValues[index];
		}
		
		public String getValue(int index) {
			return fValues[index];
		}		
		
		public int getSelection(String value) {
			if (value != null) {
				for (int i= 0; i < fValues.length; i++) {
					if (value.equals(fValues[i])) {
						return i;
					}
				}
			}
			return fValues.length -1; // assume the last option is the least severe
		}
	}
	
	
	private Map fWorkingValues;

	protected final ArrayList fCheckBoxes;
	protected final ArrayList fComboBoxes;
	protected final ArrayList fTextBoxes;
	protected final HashMap fLabels;
	protected final ArrayList fExpandedComposites;
	
	private SelectionListener fSelectionListener;
	private ModifyListener fTextModifyListener;

	protected IStatusChangeListener fContext;
	protected final IProject fProject; // project or null
	protected final Key[] fAllKeys;
	
	private IScopeContext[] fLookupOrder;
	
	private Shell fShell;

	public OptionsConfigurationBlock(IStatusChangeListener context, IProject project, Key[] allKeys) {
		fContext= context;
		fProject= project;
		fAllKeys= allKeys;
		if (fProject != null) {
			fLookupOrder= new IScopeContext[] {
				new ProjectScope(fProject),
				new InstanceScope(),
				new DefaultScope()
			};
		} else {
			fLookupOrder= new IScopeContext[] {
				new InstanceScope(),
				new DefaultScope()
			};
		}
		
		fWorkingValues= getOptions();
		testIfOptionsComplete(fWorkingValues, allKeys);
		
		fCheckBoxes= new ArrayList();
		fComboBoxes= new ArrayList();
		fTextBoxes= new ArrayList(2);
		fLabels= new HashMap();
		fExpandedComposites= new ArrayList();
	}
	
	protected static Key getKey(String plugin, String key) {
		return new Key(plugin, key);
	}
	
	protected final static Key getJDTCoreKey(String key) {
		return getKey(JavaCore.PLUGIN_ID, key);
	}
	
	protected final static Key getJDTUIKey(String key) {
		return getKey(JavaUI.ID_PLUGIN, key);
	}
	
		
	private void testIfOptionsComplete(Map workingValues, Key[] allKeys) {
		for (int i= 0; i < allKeys.length; i++) {
			if (workingValues.get(allKeys[i]) == null) {
				JavaPlugin.logErrorMessage("preference option missing: " + allKeys[i] + " (" + this.getClass().getName() +')');  //$NON-NLS-1$//$NON-NLS-2$
			}
		}
	}
	
	protected Map getOptions() {
		Map workingValues= new HashMap();
		for (int i= 0; i < fAllKeys.length; i++) {
			Key curr= fAllKeys[i];
			workingValues.put(curr, curr.getStoredValue(fLookupOrder));
		}
		return workingValues;
	}
	
	protected Map getDefaultOptions() {
		Map workingValues= new HashMap();
		DefaultScope defaultScope= new DefaultScope();
		for (int i= 0; i < fAllKeys.length; i++) {
			Key curr= fAllKeys[i];
			workingValues.put(curr, curr.getStoredValue(defaultScope));
		}
		return workingValues;
	}	
	
	public final boolean hasProjectSpecificOptions() {
		if (fProject != null) {
			IScopeContext projectContext= new ProjectScope(fProject);
			Key[] allKeys= fAllKeys;
			for (int i= 0; i < allKeys.length; i++) {
				if (allKeys[i].getStoredValue(projectContext) != null) {
					return true;
				}
			}
		}
		return false;
	}	
			
	protected Shell getShell() {
		return fShell;
	}
	
	protected void setShell(Shell shell) {
		fShell= shell;
	}	
	
	protected abstract Control createContents(Composite parent);
	
	protected Button addCheckBox(Composite parent, String label, Key key, String[] values, int indent) {
		ControlData data= new ControlData(key, values);
		
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 3;
		gd.horizontalIndent= indent;
		
		Button checkBox= new Button(parent, SWT.CHECK);
		checkBox.setText(label);
		checkBox.setData(data);
		checkBox.setLayoutData(gd);
		checkBox.addSelectionListener(getSelectionListener());
		
		makeScrollableCompositeAware(checkBox);
		
		String currValue= getValue(key);
		checkBox.setSelection(data.getSelection(currValue) == 0);
		
		fCheckBoxes.add(checkBox);
		
		return checkBox;
	}
	
	protected Combo addComboBox(Composite parent, String label, Key key, String[] values, String[] valueLabels, int indent) {
		GridData gd= new GridData(GridData.FILL, GridData.CENTER, true, false, 2, 1);
		gd.horizontalIndent= indent;
				
		Label labelControl= new Label(parent, SWT.LEFT | SWT.WRAP);
		labelControl.setText(label);
		labelControl.setLayoutData(gd);
				
		Combo comboBox= newComboControl(parent, key, values, valueLabels);
		comboBox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

		fLabels.put(comboBox, labelControl);
		
		return comboBox;
	}
	
	protected Combo addInversedComboBox(Composite parent, String label, Key key, String[] values, String[] valueLabels, int indent) {
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent= indent;
		gd.horizontalSpan= 3;
		
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		composite.setLayout(layout);
		composite.setLayoutData(gd);
		
		Combo comboBox= newComboControl(composite, key, values, valueLabels);
		comboBox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		
		Label labelControl= new Label(composite, SWT.LEFT | SWT.WRAP);
		labelControl.setText(label);
		labelControl.setLayoutData(new GridData());
		
		fLabels.put(comboBox, labelControl);
		return comboBox;
	}
	
	protected Combo newComboControl(Composite composite, Key key, String[] values, String[] valueLabels) {
		ControlData data= new ControlData(key, values);
		
		Combo comboBox= new Combo(composite, SWT.READ_ONLY);
		comboBox.setItems(valueLabels);
		comboBox.setData(data);
		comboBox.addSelectionListener(getSelectionListener());
			
		makeScrollableCompositeAware(comboBox);
		
		String currValue= getValue(key);	
		comboBox.select(data.getSelection(currValue));
		
		fComboBoxes.add(comboBox);
		return comboBox;
	}

	protected Text addTextField(Composite parent, String label, Key key, int indent, int widthHint) {	
		Label labelControl= new Label(parent, SWT.WRAP);
		labelControl.setText(label);
		labelControl.setLayoutData(new GridData());
				
		Text textBox= new Text(parent, SWT.BORDER | SWT.SINGLE);
		textBox.setData(key);
		textBox.setLayoutData(new GridData());
		
		makeScrollableCompositeAware(textBox);
		
		fLabels.put(textBox, labelControl);
		
		String currValue= getValue(key);	
		if (currValue != null) {
			textBox.setText(currValue);
		}
		textBox.addModifyListener(getTextModifyListener());

		GridData data= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		if (widthHint != 0) {
			data.widthHint= widthHint;
		}
		data.horizontalIndent= indent;
		data.horizontalSpan= 2;
		textBox.setLayoutData(data);

		fTextBoxes.add(textBox);
		return textBox;
	}
	
	protected ScrolledPageContent getParentScrolledComposite(Control control) {
		Control parent= control.getParent();
		while (!(parent instanceof ScrolledPageContent) && parent != null) {
			parent= parent.getParent();
		}
		if (parent instanceof ScrolledPageContent) {
			return (ScrolledPageContent) parent;
		}
		return null;
	}
	
	private void makeScrollableCompositeAware(Control control) {
		ScrolledPageContent parentScrolledComposite= getParentScrolledComposite(control);
		if (parentScrolledComposite != null) {
			parentScrolledComposite.adaptChild(control);
		}
	}
	
	protected ExpandableComposite createStyleSection(Composite parent, String label, int nColumns) {
		final ExpandableComposite excomposite= new ExpandableComposite(parent, SWT.NONE, ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT);
		excomposite.setText(label);
		excomposite.setExpanded(false);
		excomposite.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
		excomposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, nColumns, 1));
		excomposite.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				updateSectionStyle((ExpandableComposite) e.getSource());
				ScrolledPageContent parentScrolledComposite= getParentScrolledComposite(excomposite);
				if (parentScrolledComposite != null) {
					parentScrolledComposite.reflow(true);
				}
			}
		});
		
		updateSectionStyle(excomposite);
		fExpandedComposites.add(excomposite);
		return excomposite;
	}
	
	protected void updateSectionStyle(ExpandableComposite excomposite) {
		//excomposite.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
		if (excomposite.isExpanded()) {
			for (int i= 0; i < fExpandedComposites.size(); i++) {
				ExpandableComposite curr= (ExpandableComposite) fExpandedComposites.get(i);
				if (curr != excomposite && excomposite.isExpanded()) {
					curr.setExpanded(false);
				}
			}
			
		}
	}
	
	protected ImageHyperlink createHelpLink(Composite parent, final String link) {
		ImageHyperlink info = new ImageHyperlink(parent, SWT.NULL);
		makeScrollableCompositeAware(info);
		Image image = JavaPluginImages.get(JavaPluginImages.IMG_OBJS_HELP);
		info.setImage(image);
		info.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				WorkbenchHelp.displayHelpResource(link);
			}
		});
		return info;
	}
	

	protected SelectionListener getSelectionListener() {
		if (fSelectionListener == null) {
			fSelectionListener= new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent e) {}
	
				public void widgetSelected(SelectionEvent e) {
					controlChanged(e.widget);
				}
			};
		}
		return fSelectionListener;
	}
	
	protected ModifyListener getTextModifyListener() {
		if (fTextModifyListener == null) {
			fTextModifyListener= new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					textChanged((Text) e.widget);
				}
			};
		}
		return fTextModifyListener;
	}		
	
	protected void controlChanged(Widget widget) {
		ControlData data= (ControlData) widget.getData();
		String newValue= null;
		if (widget instanceof Button) {
			newValue= data.getValue(((Button)widget).getSelection());			
		} else if (widget instanceof Combo) {
			newValue= data.getValue(((Combo)widget).getSelectionIndex());
		} else {
			return;
		}
		String oldValue= setValue(data.getKey(), newValue);
		validateSettings(data.getKey(), oldValue, newValue);
	}
	
	protected void textChanged(Text textControl) {
		Key key= (Key) textControl.getData();
		String number= textControl.getText();
		String oldValue= setValue(key, number);
		validateSettings(key, oldValue, number);
	}	

	protected boolean checkValue(Key key, String value) {
		return value.equals(getValue(key));
	}
	
	protected String getValue(Key key) {
		return (String) fWorkingValues.get(key);
	}
	
	protected boolean getBooleanValue(Key key) {
		return Boolean.valueOf(getValue(key)).booleanValue();
	}
	
	protected String setValue(Key key, String value) {
		return (String) fWorkingValues.put(key, value);
	}
	
	protected String setValue(Key key, boolean value) {
		return setValue(key, String.valueOf(value));
	}
	
	/* (non-javadoc)
	 * Update fields and validate.
	 * @param changedKey Key that changed, or null, if all changed.
	 */	
	protected abstract void validateSettings(Key changedKey, String oldValue, String newValue);
	
	
	protected String[] getTokens(String text, String separator) {
		StringTokenizer tok= new StringTokenizer(text, separator); //$NON-NLS-1$
		int nTokens= tok.countTokens();
		String[] res= new String[nTokens];
		for (int i= 0; i < res.length; i++) {
			res[i]= tok.nextToken().trim();
		}
		return res;
	}	

	private boolean hasChanges(IScopeContext currContext, boolean enabled) {
		for (int i= 0; i < fAllKeys.length; i++) {
			Key key= fAllKeys[i];
			String oldVal= key.getStoredValue(currContext);
			String val= null;
			if (enabled) {
				val= getValue(key);
				if (val != null && !val.equals(oldVal)) {
					return true;
				}
			} else {
				if (oldVal != null) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean performOk(boolean enabled) {

		IScopeContext currContext= fLookupOrder[0];
	
		boolean hasChanges= hasChanges(currContext, enabled);
		if (!hasChanges) {
			return true;
		}
		
		boolean doBuild= false;
		String[] strings= getFullBuildDialogStrings(fProject == null);
		if (strings != null) {
			MessageDialog dialog= new MessageDialog(getShell(), strings[0], null, strings[1], MessageDialog.QUESTION, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL }, 2);
			int res= dialog.open();
			if (res == 0) {
				doBuild= true;
			} else if (res != 1) {
				return false; // cancel pressed
			}
		}
		
		MockupPreferenceStore store= JavaPlugin.getDefault().getMockupPreferenceStore();			
		Key[] allKeys= fAllKeys;
		ArrayList modifiedNodes= new ArrayList();
		for (int i= 0; i < allKeys.length; i++) {
			Key key= allKeys[i];
			String oldVal= key.getStoredValue(currContext);
			if (enabled) {
				String val= getValue(key);
				if (val != null && !val.equals(oldVal)) {
					key.setStoredValue(currContext, val);
					if (fProject != null) {
						store.firePropertyChangeEvent(fProject, key.getName(), oldVal, val);
					}
				}
			} else {
				if (oldVal != null) {
					key.removeFromStore(currContext);
					if (fProject != null) {
						store.firePropertyChangeEvent(fProject, key.getName(), oldVal, null);
					}
				}
			}
			modifiedNodes.add(key.getQualifier());
		}
		for (int i= 0; i < modifiedNodes.size(); i++) {
			try {
				String curr= (String) modifiedNodes.get(i);
				currContext.getNode(curr).flush();
			} catch (BackingStoreException e) {
				JavaPlugin.log(e);
			}
		}		

		if (doBuild) {
			boolean res= doFullBuild();
			if (!res) {
				return false;
			}
		}
		return true;
	}
	
	protected abstract String[] getFullBuildDialogStrings(boolean workspaceSettings);
		
	protected boolean doFullBuild() {
		
		Job buildJob = new Job(PreferencesMessages.getString("OptionsConfigurationBlock.job.title")){  //$NON-NLS-1$
			/* (non-Javadoc)
			 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
			 */
			protected IStatus run(IProgressMonitor monitor) {
				try {
					if (fProject != null) {
						monitor.beginTask(PreferencesMessages.getFormattedString("OptionsConfigurationBlock.buildproject.taskname", fProject.getName()), 2); //$NON-NLS-1$
						fProject.build(IncrementalProjectBuilder.FULL_BUILD, new SubProgressMonitor(monitor,1));
						JavaPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new SubProgressMonitor(monitor,1));
					} else {
						monitor.beginTask(PreferencesMessages.getString("OptionsConfigurationBlock.buildall.taskname"), 2); //$NON-NLS-1$
						JavaPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, new SubProgressMonitor(monitor, 2));
					}
				} catch (CoreException e) {
					return e.getStatus();
				} catch (OperationCanceledException e) {
					return Status.CANCEL_STATUS;
				}
				finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
			public boolean belongsTo(Object family) {
				return ResourcesPlugin.FAMILY_MANUAL_BUILD == family;
			}
		};
		
		buildJob.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
		buildJob.setUser(true); 
		buildJob.schedule();
		return true;
	}		
	
	public void performDefaults() {
		fWorkingValues= getDefaultOptions();
		updateControls();
		validateSettings(null, null, null);
	}

	/**
	 * @since 3.1
	 */
	public void performRevert() {
		fWorkingValues= getOptions();
		updateControls();
		validateSettings(null, null, null);
	}
	
	public void dispose() {
	}
	
	protected void updateControls() {
		// update the UI
		for (int i= fCheckBoxes.size() - 1; i >= 0; i--) {
			updateCheckBox((Button) fCheckBoxes.get(i));
		}
		for (int i= fComboBoxes.size() - 1; i >= 0; i--) {
			updateCombo((Combo) fComboBoxes.get(i));
		}
		for (int i= fTextBoxes.size() - 1; i >= 0; i--) {
			updateText((Text) fTextBoxes.get(i));
		}
	}
	
	protected void updateCombo(Combo curr) {
		ControlData data= (ControlData) curr.getData();
		
		String currValue= getValue(data.getKey());	
		curr.select(data.getSelection(currValue));					
	}
	
	protected void updateCheckBox(Button curr) {
		ControlData data= (ControlData) curr.getData();
		
		String currValue= getValue(data.getKey());	
		curr.setSelection(data.getSelection(currValue) == 0);						
	}
	
	protected void updateText(Text curr) {
		Key key= (Key) curr.getData();
		
		String currValue= getValue(key);
		if (currValue != null) {
			curr.setText(currValue);
		}
	}
	
	protected Button getCheckBox(Key key) {
		for (int i= fCheckBoxes.size() - 1; i >= 0; i--) {
			Button curr= (Button) fCheckBoxes.get(i);
			ControlData data= (ControlData) curr.getData();
			if (key.equals(data.getKey())) {
				return curr;
			}
		}
		return null;		
	}
	
	protected Combo getComboBox(Key key) {
		for (int i= fComboBoxes.size() - 1; i >= 0; i--) {
			Combo curr= (Combo) fComboBoxes.get(i);
			ControlData data= (ControlData) curr.getData();
			if (key.equals(data.getKey())) {
				return curr;
			}
		}
		return null;		
	}
	
	protected void setComboEnabled(Key key, boolean enabled) {
		Combo combo= getComboBox(key);
		Label label= (Label) fLabels.get(combo);
		combo.setEnabled(enabled);
		label.setEnabled(enabled);
	}
	
	
	
}
