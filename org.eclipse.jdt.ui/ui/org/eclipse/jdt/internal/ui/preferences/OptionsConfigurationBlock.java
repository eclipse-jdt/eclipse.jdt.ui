/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.osgi.service.prefs.BackingStoreException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Scrollable;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;

import org.eclipse.jface.action.LegacyActionTools;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;

import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.ui.preferences.IWorkingCopyManager;
import org.eclipse.ui.preferences.WorkingCopyManager;
import org.eclipse.ui.progress.WorkbenchJob;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.CoreUtility;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.util.StringMatcher;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;

/**
 * Abstract options configuration block providing a general implementation for setting up
 * an options configuration page.
 *
 * @since 2.1
 */
public abstract class OptionsConfigurationBlock {

	public static class Key {

		private final String fQualifier;
		private final String fKey;

		public Key(String qualifier, String key) {
			fQualifier= qualifier;
			fKey= key;
		}

		public String getName() {
			return fKey;
		}

		private IEclipsePreferences getNode(IScopeContext context, IWorkingCopyManager manager) {
			IEclipsePreferences node= context.getNode(fQualifier);
			if (manager != null) {
				return manager.getWorkingCopy(node);
			}
			return node;
		}

		public String getStoredValue(IScopeContext context, IWorkingCopyManager manager) {
			return getNode(context, manager).get(fKey, null);
		}

		public String getStoredValue(IScopeContext[] lookupOrder, boolean ignoreTopScope, IWorkingCopyManager manager) {
			for (int i= ignoreTopScope ? 1 : 0; i < lookupOrder.length; i++) {
				String value= getStoredValue(lookupOrder[i], manager);
				if (value != null) {
					return value;
				}
			}
			return null;
		}

		public void setStoredValue(IScopeContext context, String value, IWorkingCopyManager manager) {
			if (value != null) {
				getNode(context, manager).put(fKey, value);
			} else {
				getNode(context, manager).remove(fKey);
			}
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return fQualifier + '/' + fKey;
		}

		public String getQualifier() {
			return fQualifier;
		}

	}

	/**
	 * Key that is only managed locally and not part of preference store.
	 */
	private static class LocalKey extends Key {
		private final HashMap<IScopeContext, String> fValues;

		private LocalKey(String key) {
			super("local", key); //$NON-NLS-1$
			fValues= new HashMap<IScopeContext, String>();
		}

		@Override
		public String getStoredValue(IScopeContext context, IWorkingCopyManager manager) {
			return fValues.get(context);
		}

		@Override
		public void setStoredValue(IScopeContext context, String value, IWorkingCopyManager manager) {
			if (value != null) {
				fValues.put(context, value);
			} else {
				fValues.remove(context);
			}
		}
	}

	protected static class ControlData {
		private final Key fKey;
		private final String[] fValues;

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
	
	protected static class LinkControlData extends ControlData {
		private Link fLink;

		public LinkControlData(Key key, String[] values) {
			super(key, values);
		}

		public void setLink(Link link) {
			fLink= link;
		}
		
		public Link getLink() {
			return fLink;
		}
	}

	/**
	 * A node in <code>FilteredPreferenceTree</code>.
	 */
	protected static class PreferenceTreeNode {

		public static final int NONE= 0;

		public static final int CHECKBOX= 1;

		public static final int COMBO= 2;

		public static final int EXPANDABLE_COMPOSITE= 3;

		public static final int TEXT_CONTROL= 4;

		/**
		 * Tells the type of UI control corresponding to this node. One of
		 * <ul>
		 * <li> <code>NONE</code></li>
		 * <li> <code>CHECKBOX</code></li>
		 * <li> <code>COMBO</code></li>
		 * <li> <code>EXPANDABLE_COMPOSITE</code></li>
		 * <li> <code>TEXT_CONTROL</code></li>
		 * </ul>
		 */
		private final int fControlType;

		/**
		 * Label text of the preference which is used for filtering. This text does not contain
		 * <code>&</code> which is used to indicate mnemonics.
		 */
		private final String fLabel;

		/**
		 * The preference key or the local key to uniquely identify a node's corresponding UI
		 * control. Can be <code>null</code>.
		 */
		private final Key fKey;

		/**
		 * Tells whether all children should be shown even if just one child matches the filter.
		 */
		private final boolean fShowAllChildren;

		/**
		 * Tells whether this node's UI control is visible in the UI for the current filter text.
		 */
		private boolean fVisible;

		/**
		 * List of children nodes.
		 */
		private List<PreferenceTreeNode> fChildren;

		/**
		 * Constructs a new instance of PreferenceTreeNode according to the parameters.
		 * <p>
		 * The <code>label</code> and the <code>key</code> must not be <code>null</code> if the node
		 * has a corresponding UI control.
		 * </p>
		 * 
		 * @param label the label text
		 * @param key the key
		 * @param controlType the type of UI control.
		 * @param showAllChildren tells whether all children should be shown even if just one child
		 *            matches the filter.
		 */
		public PreferenceTreeNode(String label, Key key, int controlType, boolean showAllChildren) {
			super();
			if (controlType != NONE && (label == null || key == null)) {
				throw new IllegalArgumentException();
			}
			if (label == null) {
				label= ""; //$NON-NLS-1$
			}
			fLabel= LegacyActionTools.removeMnemonics(label);
			fKey= key;
			fControlType= controlType;
			fShowAllChildren= showAllChildren;
		}

		public String getLabel() {
			return fLabel;
		}

		public Key getKey() {
			return fKey;
		}

		public int getControlType() {
			return fControlType;
		}

		public List<PreferenceTreeNode> getChildren() {
			return fChildren;
		}

		public boolean isShowAllChildren() {
			return fShowAllChildren;
		}

		public boolean isVisible() {
			return fVisible;
		}

		private void setVisible(boolean visible, boolean recursive) {
			fVisible= visible;
			if (!recursive)
				return;
			if (fChildren != null) {
				for (int i= 0; i < fChildren.size(); i++) {
					fChildren.get(i).setVisible(visible, recursive);
				}
			}
		}

		public PreferenceTreeNode addChild(String label, Key key, int controlType, boolean showAllChildren) {
			if (fChildren == null) {
				fChildren= new ArrayList<PreferenceTreeNode>();
			}
			PreferenceTreeNode n= new PreferenceTreeNode(label, key, controlType, showAllChildren);
			fChildren.add(n);
			return n;
		}

		public boolean hasValue() {
			if (fControlType == COMBO || fControlType == CHECKBOX || fControlType == TEXT_CONTROL) {
				return true;
			}
			return false;
		}
	}

	/**
	 * The preference page modeled as a filtered tree.
	 * <p>
	 * The tree consists of an optional description label, a filter text input box, and a scrolled
	 * area. The scrolled content contains all the UI controls which participate in filtering.
	 * </p>
	 * <p>
	 * Supports '*' and '?' wildcards. A word in filter text preceded by '~' is used to filter on
	 * preference values, e.g. ~ignore or ~off. Supported filter formats are
	 * <ul>
	 * <li>pattern</li>
	 * <li>~valueFilter</li>
	 * <li>pattern ~valueFilter</li>
	 * <li>~valueFilter pattern</li>
	 * </ul>
	 * </p>
	 */
	protected static class FilteredPreferenceTree {
		/**
		 * Root node for the tree. It does not have a corresponding UI control.
		 */
		private final PreferenceTreeNode fRoot;

		/**
		 * The Options Configuration block.
		 */
		private final OptionsConfigurationBlock fConfigBlock;

		/**
		 * The parent composite of <code>FilteredPreferenceTree</code>.
		 */
		private final Composite fParentComposite;

		/**
		 * The scrolled area of the tree.
		 */
		private ScrolledPageContent fScrolledPageContent;

		/**
		 * Job to update the UI in a separate thread.
		 */
		private final WorkbenchJob fRefreshJob;

		/**
		 * Tells whether the filter text matched at least one element.
		 */
		private boolean fMatchFound;

		/**
		 * Label to indicate that no option matched the filter text.
		 */
		private Label fNoMatchFoundLabel;

		/**
		 * Constructs a new instance of FilteredPreferenceTree according to the parameters.
		 * 
		 * @param configBlock the Options Configuration block
		 * @param parentComposite the parent composite
		 * @param label the label, or <code>null</code> if none
		 */
		public FilteredPreferenceTree(OptionsConfigurationBlock configBlock, Composite parentComposite, String label) {
			fRoot= new PreferenceTreeNode(null, null, PreferenceTreeNode.NONE, false);
			fConfigBlock= configBlock;
			fParentComposite= parentComposite;

			createDescription(label);
			createFilterBox();
			createScrolledArea();
			createNoMatchFoundLabel();

			fRefreshJob= doCreateRefreshJob();
			fRefreshJob.setSystem(true);
			fParentComposite.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					fRefreshJob.cancel();
				}
			});
		}

		private void createDescription(String label) {
			if (label == null)
				return;
			
			Label description= new Label(fParentComposite, SWT.LEFT | SWT.WRAP);
			description.setFont(fParentComposite.getFont());
			description.setText(label);
			description.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false));
		}

		private void createFilterBox() {
			//TODO: Directly use the hint flags once Bug 293230 is fixed
			FilterTextControl filterTextControl= new FilterTextControl(fParentComposite);

			final Text filterBox= filterTextControl.getFilterControl();
			filterBox.setMessage(PreferencesMessages.OptionsConfigurationBlock_TypeFilterText);
			
			filterBox.addModifyListener(new ModifyListener() {
				private String fPrevFilterText;

				public void modifyText(ModifyEvent e) {
					String input= filterBox.getText();
					if (input != null && input.equalsIgnoreCase(fPrevFilterText))
						return;
					fPrevFilterText= input;
					doFilter(input);
				}
			});
		}

		private void createScrolledArea() {
			fScrolledPageContent= new ScrolledPageContent(fParentComposite);
			fScrolledPageContent.addControlListener(new ControlAdapter() {
				@Override
				public void controlResized(ControlEvent e) {
					fScrolledPageContent.getVerticalBar().setVisible(true);
				}
			});
		}

		public ScrolledPageContent getScrolledPageContent() {
			return fScrolledPageContent;
		}

		private void createNoMatchFoundLabel() {
			fNoMatchFoundLabel= new Label(fScrolledPageContent.getBody(), SWT.NONE);
			GridData gd= new GridData(SWT.BEGINNING, SWT.CENTER, true, false);
			gd.horizontalSpan= 3;
			fNoMatchFoundLabel.setLayoutData(gd);
			fNoMatchFoundLabel.setFont(fScrolledPageContent.getFont());
			fNoMatchFoundLabel.setText(PreferencesMessages.OptionsConfigurationBlock_NoOptionMatchesTheFilter);
			setVisible(fNoMatchFoundLabel, false);
		}

		public PreferenceTreeNode addChild(PreferenceTreeNode parent, String label, Key key, int controlType, boolean showAllChildren) {
			parent= (parent == null) ? fRoot : parent;
			return parent.addChild(label, key, controlType, showAllChildren);
		}

		public PreferenceTreeNode addCheckBox(Composite parentComposite, String label, Key key, String[] values, int indent, PreferenceTreeNode parentNode, boolean showAllChildren) {
			fConfigBlock.addCheckBox(parentComposite, label, key, values, indent);
			return addChild(parentNode, label, key, PreferenceTreeNode.CHECKBOX, showAllChildren);
		}

		public PreferenceTreeNode addCheckBox(Composite parentComposite, String label, Key key, String[] values, int indent, PreferenceTreeNode parentNode) {
			return addCheckBox(parentComposite, label, key, values, indent, parentNode, true);
		}

		public PreferenceTreeNode addComboBox(Composite parentComposite, String label, Key key, String[] values, String[] valueLabels, int indent, PreferenceTreeNode parentNode, boolean showAllChildren) {
			fConfigBlock.addComboBox(parentComposite, label, key, values, valueLabels, indent);
			return addChild(parentNode, label, key, PreferenceTreeNode.COMBO, showAllChildren);
		}

		public PreferenceTreeNode addComboBox(Composite parentComposite, String label, Key key, String[] values, String[] valueLabels, int indent, PreferenceTreeNode parentNode) {
			return addComboBox(parentComposite, label, key, values, valueLabels, indent, parentNode, true);
		}

		public PreferenceTreeNode addTextField(Composite parentComposite, String label, Key key, int indent, int widthHint, PreferenceTreeNode parentNode, boolean showAllChildren) {
			fConfigBlock.addTextField(parentComposite, label, key, indent, widthHint);
			return addChild(parentNode, label, key, PreferenceTreeNode.TEXT_CONTROL, showAllChildren);
		}

		public PreferenceTreeNode addExpandableComposite(Composite parentComposite, String label, int nColumns, Key key, PreferenceTreeNode parentNode, boolean showAllChildren) {
			fConfigBlock.createStyleSection(parentComposite, label, nColumns, key);
			return addChild(parentNode, label, key, PreferenceTreeNode.EXPANDABLE_COMPOSITE, showAllChildren);
		}

		private boolean match(PreferenceTreeNode node, StringMatcher labelMatcher, StringMatcher valueMatcher) {
			if (node.getKey() == null) {
				return false;
			}
			boolean valueMatched= true;
			boolean labelMatched= true;
			if (labelMatcher != null) {
				labelMatched= labelMatcher.match(node.getLabel());
			}
			if (valueMatcher != null) {
				if (node.getControlType() == PreferenceTreeNode.COMBO) {
					valueMatched= valueMatcher.match(fConfigBlock.getComboBox(node.getKey()).getText());
				} else if (node.getControlType() == PreferenceTreeNode.CHECKBOX) {
					boolean checked= fConfigBlock.getCheckBox(node.getKey()).getSelection();
					if (checked) {
						valueMatched= valueMatcher.match(PreferencesMessages.OptionsConfigurationBlock_On) || valueMatcher.match(PreferencesMessages.OptionsConfigurationBlock_Enabled);
					} else {
						valueMatched= valueMatcher.match(PreferencesMessages.OptionsConfigurationBlock_Off) || valueMatcher.match(PreferencesMessages.OptionsConfigurationBlock_Disabled);
					}
				}
			}
			return labelMatched && valueMatched;
		}

		public boolean filter(PreferenceTreeNode node, StringMatcher labelMatcher, StringMatcher valueMatcher) {
			//check this node
			boolean visible= match(node, labelMatcher, valueMatcher);
			if (visible) {
				if (valueMatcher != null && !node.hasValue()) { //see bug 321818
					labelMatcher= null;
					visible= false;
				} else {
					node.setVisible(visible, true);
					fMatchFound= true;
					return visible;
				}
			}
			//check children
			List<PreferenceTreeNode> children= node.getChildren();
			if (children != null) {
				for (int i= 0; i < children.size(); i++) {
					visible|= filter(children.get(i), labelMatcher, valueMatcher);
				}
				if (node.isShowAllChildren()) {
					for (int i= 0; i < children.size(); i++) {
						children.get(i).setVisible(visible, false);
					}
				}
			}
			node.setVisible(visible, false);
			return visible;
		}

		public void doFilter(String filterText) {
			fRefreshJob.cancel();
			fRefreshJob.schedule(getRefreshJobDelay());
			filterText= filterText.trim();
			int index= filterText.indexOf("~"); //$NON-NLS-1$
			StringMatcher labelMatcher= null;
			StringMatcher valueMatcher= null;
			if (index == -1) {
				labelMatcher= createStringMatcher(filterText);
			} else {
				if (index == 0) {
					int i= 0;
					for (; i < filterText.length(); i++) {
						char ch= filterText.charAt(i);
						if (ch == ' ' || ch == '\t') {
							break;
						}
					}
					valueMatcher= createStringMatcher(filterText.substring(1, i));
					labelMatcher= createStringMatcher(filterText.substring(i));
				} else {
					labelMatcher= createStringMatcher(filterText.substring(0, index));
					if (index < filterText.length())
						valueMatcher= createStringMatcher(filterText.substring(index + 1));
				}
			}
			fMatchFound= false;
			filter(fRoot, labelMatcher, valueMatcher);
		}

		private StringMatcher createStringMatcher(String filterText) {
			filterText= filterText.trim();
			if (filterText.length() > 0)
				return new StringMatcher("*" + filterText + "*", true, false); //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}

		/**
		 * Return the time delay that should be used when scheduling the filter refresh job.
		 * 
		 * @return a time delay in milliseconds before the job should run
		 */
		private long getRefreshJobDelay() {
			return 200;
		}

		private void updateUI(PreferenceTreeNode node) {
			//update node
			int controlType= node.getControlType();
			Control control= null;
			if (controlType == PreferenceTreeNode.CHECKBOX) {
				control= fConfigBlock.getCheckBox(node.getKey());
			} else if (controlType == PreferenceTreeNode.COMBO) {
				control= fConfigBlock.getComboBox(node.getKey());
			} else if (controlType == PreferenceTreeNode.TEXT_CONTROL) {
				control= fConfigBlock.getTextControl(node.getKey());
			} else if (controlType == PreferenceTreeNode.EXPANDABLE_COMPOSITE) {
				control= fConfigBlock.getExpandableComposite(node.getKey());
			}

			if (control != null) {
				boolean visible= node.isVisible();
				setVisible(control, visible);
				if (control instanceof Combo || control instanceof Text) {
					Label label= (fConfigBlock.fLabels.get(control));
					setVisible(label, visible);
				}
				if (control instanceof ExpandableComposite) {
					((ExpandableComposite)control).setExpanded(visible);
				}
			}

			//update children
			List<PreferenceTreeNode> children= node.getChildren();
			if (children != null) {
				for (int i= 0; i < children.size(); i++) {
					updateUI(children.get(i));
				}
			}
		}

		private WorkbenchJob doCreateRefreshJob() {
			return new WorkbenchJob(PreferencesMessages.OptionsConfigurationBlock_RefreshFilter) {
				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					updateUI(fRoot);
					fParentComposite.layout(true, true); //relayout
					if (fScrolledPageContent != null) {
						setVisible(fNoMatchFoundLabel, !fMatchFound);
						fScrolledPageContent.reflow(true);
					}
					return Status.OK_STATUS;
				}
			};
		}

		private void setVisible(Control control, boolean visible) {
			control.setVisible(visible);
			((GridData)control.getLayoutData()).exclude= !visible;
		}
	}

	private static final String REBUILD_COUNT_KEY= "preferences_build_requested"; //$NON-NLS-1$

	private static final String SETTINGS_EXPANDED= "expanded"; //$NON-NLS-1$

	protected final ArrayList<Button> fCheckBoxes;
	protected final ArrayList<Combo> fComboBoxes;
	protected final ArrayList<Text> fTextBoxes;
	protected final HashMap<Scrollable, Label> fLabels;
	protected final ArrayList<ExpandableComposite> fExpandableComposites;

	private SelectionListener fSelectionListener;
	private ModifyListener fTextModifyListener;

	protected IStatusChangeListener fContext;
	protected final IProject fProject; // project or null
	protected final Key[] fAllKeys;

	private IScopeContext[] fLookupOrder;

	private Shell fShell;

	private final IWorkingCopyManager fManager;
	private final IWorkbenchPreferenceContainer fContainer;

	private Map<Key, String> fDisabledProjectSettings; // null when project specific settings are turned off

	private int fRebuildCount; /// used to prevent multiple dialogs that ask for a rebuild

	public OptionsConfigurationBlock(IStatusChangeListener context, IProject project, Key[] allKeys, IWorkbenchPreferenceContainer container) {
		fContext= context;
		fProject= project;
		fAllKeys= allKeys;
		fContainer= container;
		if (container == null) {
			fManager= new WorkingCopyManager();
		} else {
			fManager= container.getWorkingCopyManager();
		}

		if (fProject != null) {
			fLookupOrder= new IScopeContext[] {
				new ProjectScope(fProject),
				InstanceScope.INSTANCE,
				DefaultScope.INSTANCE
			};
		} else {
			fLookupOrder= new IScopeContext[] {
				InstanceScope.INSTANCE,
				DefaultScope.INSTANCE
			};
		}
		testIfOptionsComplete(allKeys);
		if (fProject == null || hasProjectSpecificOptions(fProject)) {
			fDisabledProjectSettings= null;
		} else {
			fDisabledProjectSettings= new IdentityHashMap<Key, String>();
			for (int i= 0; i < allKeys.length; i++) {
				Key curr= allKeys[i];
				fDisabledProjectSettings.put(curr, curr.getStoredValue(fLookupOrder, false, fManager));
			}
		}

		settingsUpdated();

		fCheckBoxes= new ArrayList<Button>();
		fComboBoxes= new ArrayList<Combo>();
		fTextBoxes= new ArrayList<Text>(2);
		fLabels= new HashMap<Scrollable, Label>();
		fExpandableComposites= new ArrayList<ExpandableComposite>();

		fRebuildCount= getRebuildCount();
	}

	protected final IWorkbenchPreferenceContainer getPreferenceContainer() {
		return fContainer;
	}

	protected static Key getKey(String plugin, String key) {
		return new Key(plugin, key);
	}

	protected final static Key getJDTLaunchingKey(String key) {
		return getKey(JavaRuntime.ID_PLUGIN, key);
	}

	protected final static Key getJDTCoreKey(String key) {
		return getKey(JavaCore.PLUGIN_ID, key);
	}

	protected final static Key getJDTUIKey(String key) {
		return getKey(JavaUI.ID_PLUGIN, key);
	}

	protected final static Key getLocalKey(String key) {
		return new LocalKey(key);
	}

	private void testIfOptionsComplete(Key[] allKeys) {
		for (int i= 0; i < allKeys.length; i++) {
			Key key= allKeys[i];
			if (!(key instanceof LocalKey)) {
				if (key.getStoredValue(fLookupOrder, false, fManager) == null) {
					JavaPlugin.logErrorMessage("preference option missing: " + key + " (" + this.getClass().getName() + ')'); //$NON-NLS-1$//$NON-NLS-2$
				}
			}
		}
	}

	private int getRebuildCount() {
		return fManager.getWorkingCopy(DefaultScope.INSTANCE.getNode(JavaUI.ID_PLUGIN)).getInt(REBUILD_COUNT_KEY, 0);
	}

	private void incrementRebuildCount() {
		fRebuildCount++;
		fManager.getWorkingCopy(DefaultScope.INSTANCE.getNode(JavaUI.ID_PLUGIN)).putInt(REBUILD_COUNT_KEY, fRebuildCount);
	}


	protected void settingsUpdated() {
	}


	public void selectOption(String key, String qualifier) {
		for (int i= 0; i < fAllKeys.length; i++) {
			Key curr= fAllKeys[i];
			if (curr.getName().equals(key) && curr.getQualifier().equals(qualifier)) {
				selectOption(curr);
			}
		}
	}

	public void selectOption(Key key) {
		Control control= findControl(key);
		if (control != null) {
			if (!fExpandableComposites.isEmpty()) {
				ExpandableComposite expandable= getParentExpandableComposite(control);
				if (expandable != null) {
					for (int i= 0; i < fExpandableComposites.size(); i++) {
						ExpandableComposite curr= fExpandableComposites.get(i);
						curr.setExpanded(curr == expandable);
					}
					expandedStateChanged(expandable);
				}
			}
			control.setFocus();
		}
	}

	public boolean hasProjectSpecificOptions(IProject project) {
		return hasProjectSpecificOptions(project, fAllKeys, fManager);
	}

	public static boolean hasProjectSpecificOptions(IProject project, Key[] allKeys, IWorkingCopyManager manager) {
		if (project != null) {
			IScopeContext projectContext= new ProjectScope(project);
			for (int i= 0; i < allKeys.length; i++) {
				if (allKeys[i].getStoredValue(projectContext, manager) != null) {
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
		checkBox.setFont(JFaceResources.getDialogFont());
		checkBox.setText(label);
		checkBox.setData(data);
		checkBox.setLayoutData(gd);
		checkBox.addSelectionListener(getSelectionListener());

		makeScrollableCompositeAware(checkBox);

		updateCheckBox(checkBox);

		fCheckBoxes.add(checkBox);

		return checkBox;
	}

	protected Button addCheckBoxWithLink(Composite parent, final String label, Key key, String[] values, int indent, int widthHint, final SelectionListener listener) {
		LinkControlData data= new LinkControlData(key, values);

		GridData gd= new GridData(GridData.FILL, GridData.FILL, true, false);
		gd.horizontalSpan= 3;
		gd.horizontalIndent= indent;

		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		composite.setLayout(layout);
		composite.setLayoutData(gd);

		final Button checkBox= new Button(composite, SWT.CHECK);
		checkBox.setFont(JFaceResources.getDialogFont());
		checkBox.setData(data);
		checkBox.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false));
		checkBox.addSelectionListener(getSelectionListener());
		checkBox.getAccessible().addAccessibleListener(new AccessibleAdapter() {
			@Override
			public void getName(AccessibleEvent e) {
				e.result = LegacyActionTools.removeMnemonics(label.replaceAll("</?[aA][^>]*>", "")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});

		gd= new GridData(GridData.FILL, GridData.CENTER, true, false);
		gd.widthHint= widthHint;
		gd.horizontalIndent= -2;

		Link link= new Link(composite, SWT.NONE);
		link.setText(label);
		link.setLayoutData(gd);
		data.setLink(link);
		
		// toggle checkbox when user clicks unlinked text in link:
		final boolean[] linkSelected= { false };
		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				linkSelected[0]= true;
				if (listener != null) {
					listener.widgetSelected(e);
				}
			}
		});
		link.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				linkSelected[0]= false;
			}
			@Override
			public void mouseUp(MouseEvent e) {
				if (!linkSelected[0]) {
					checkBox.setSelection(!checkBox.getSelection());
					checkBox.setFocus();
					linkSelected[0]= false;
					controlChanged(checkBox);
				}
			}
		});
		link.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_MNEMONIC && e.doit == true) {
					e.detail= SWT.TRAVERSE_NONE;
					checkBox.setSelection(!checkBox.getSelection());
					checkBox.setFocus();
					linkSelected[0]= false;
					controlChanged(checkBox);
				}
			}
		});

		makeScrollableCompositeAware(link);
		makeScrollableCompositeAware(checkBox);

		updateCheckBox(checkBox);

		fCheckBoxes.add(checkBox);

		return checkBox;
	}

	protected Combo addComboBox(Composite parent, String label, Key key, String[] values, String[] valueLabels, int indent) {
		GridData gd= new GridData(GridData.FILL, GridData.CENTER, true, false, 2, 1);
		gd.horizontalIndent= indent;

		Label labelControl= new Label(parent, SWT.LEFT);
		labelControl.setFont(JFaceResources.getDialogFont());
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
		comboBox.setFont(JFaceResources.getDialogFont());
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
		comboBox.setFont(JFaceResources.getDialogFont());
		SWTUtil.setDefaultVisibleItemCount(comboBox);

		makeScrollableCompositeAware(comboBox);

		updateCombo(comboBox);

		fComboBoxes.add(comboBox);
		return comboBox;
	}

	protected Text addTextField(Composite parent, String label, Key key, int indent, int widthHint) {
		Label labelControl= new Label(parent, SWT.WRAP);
		labelControl.setText(label);
		labelControl.setFont(JFaceResources.getDialogFont());
		labelControl.setLayoutData(new GridData());

		Text textBox= new Text(parent, SWT.BORDER | SWT.SINGLE);
		textBox.setData(key);
		textBox.setLayoutData(new GridData());

		makeScrollableCompositeAware(textBox);

		fLabels.put(textBox, labelControl);

		updateText(textBox);
		
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

	protected ExpandableComposite getParentExpandableComposite(Control control) {
		Control parent= control.getParent();
		while (!(parent instanceof ExpandableComposite) && parent != null) {
			parent= parent.getParent();
		}
		if (parent instanceof ExpandableComposite) {
			return (ExpandableComposite) parent;
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
		return createStyleSection(parent, label, nColumns, null);
	}

	protected ExpandableComposite createStyleSection(Composite parent, String label, int nColumns, Key key) {
		ExpandableComposite excomposite= new ExpandableComposite(parent, SWT.NONE, ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT);
		excomposite.setText(label);
		if (key != null) {
			excomposite.setData(key);
		}
		excomposite.setExpanded(false);
		excomposite.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
		excomposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, nColumns, 1));
		excomposite.addExpansionListener(new ExpansionAdapter() {
			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				expandedStateChanged((ExpandableComposite) e.getSource());
			}
		});
		fExpandableComposites.add(excomposite);
		makeScrollableCompositeAware(excomposite);
		return excomposite;
	}

	protected final void expandedStateChanged(ExpandableComposite expandable) {
		ScrolledPageContent parentScrolledComposite= getParentScrolledComposite(expandable);
		if (parentScrolledComposite != null) {
			parentScrolledComposite.reflow(true);
		}
	}

	protected void restoreSectionExpansionStates(IDialogSettings settings) {
		for (int i= 0; i < fExpandableComposites.size(); i++) {
			ExpandableComposite excomposite= fExpandableComposites.get(i);
			if (settings == null) {
				excomposite.setExpanded(i == 0); // only expand the first node by default
			} else {
				excomposite.setExpanded(settings.getBoolean(SETTINGS_EXPANDED + String.valueOf(i)));
			}
		}
	}

	protected void storeSectionExpansionStates(IDialogSettings settings) {
		for (int i= 0; i < fExpandableComposites.size(); i++) {
			ExpandableComposite curr= fExpandableComposites.get(i);
			settings.put(SETTINGS_EXPANDED + String.valueOf(i), curr.isExpanded());
		}
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

	/**
	 * Checks a setting.
	 * 
	 * @param key a key
	 * @param value an assumed value for the key
	 * @return <code>true</code> iff the given key's value is equal to the given value
	 */
	protected boolean checkValue(Key key, String value) {
		return value.equals(getValue(key));
	}

	/**
	 * Returns the value for the key.
	 * 
	 * @param key the key
	 * @return the stored value
	 */
	protected String getValue(Key key) {
		if (fDisabledProjectSettings != null) {
			return fDisabledProjectSettings.get(key);
		}
		return key.getStoredValue(fLookupOrder, false, fManager);
	}


	protected boolean getBooleanValue(Key key) {
		return Boolean.valueOf(getValue(key)).booleanValue();
	}

	/**
	 * Sets the option <code>key</code> to the value <code>value</code>.
	 * Note that callers have to make sure the corresponding controls are updated afterwards.
	 * 
	 * @param key the option key
	 * @param value the new value
	 * @return the old value
	 * 
	 * @see #updateControls()
	 * @see #updateCheckBox(Button)
	 * @see #updateCombo(Combo)
	 * @see #updateText(Text)
	 */
	protected String setValue(Key key, String value) {
		if (fDisabledProjectSettings != null) {
			return fDisabledProjectSettings.put(key, value);
		}
		String oldValue= getValue(key);
		key.setStoredValue(fLookupOrder[0], value, fManager);
		return oldValue;
	}

	/**
	 * Sets the option <code>key</code> to the value <code>value</code>.
	 * Note that callers have to make sure the corresponding controls are updated afterwards.
	 * 
	 * @param key the option key
	 * @param value the new value
	 * @return the old value
	 * 
	 * @see #updateControls()
	 * @see #updateCheckBox(Button)
	 * @see #updateCombo(Combo)
	 * @see #updateText(Text)
	 */
	protected String setValue(Key key, boolean value) {
		return setValue(key, String.valueOf(value));
	}

	protected final void setDefaultValue(Key key, String value) {
		IScopeContext instanceScope= fLookupOrder[fLookupOrder.length - 1];
		key.setStoredValue(instanceScope, value, fManager);
	}

	/**
	 * Returns the value as stored in the preference store.
	 *
	 * @param key the key
	 * @return the value
	 */
	protected String getStoredValue(Key key) {
		return key.getStoredValue(fLookupOrder, false, fManager);
	}
	
	/**
	 * Returns the value as actually stored in the preference store, without considering
	 * the working copy store.
	 *
	 * @param key the key
	 * @return the value as actually stored in the preference store
	 */
	protected String getOriginalStoredValue(Key key) {
		return key.getStoredValue(fLookupOrder, false, null);
	}

	/**
	 * Reverts the given options to the stored values.
	 * 
	 * @param keys the options to revert
	 * @since 3.5
	 */
	protected void revertValues(Key[] keys) {
		for (int i= 0; i < keys.length; i++) {
			Key curr= keys[i];
			String origValue= curr.getStoredValue(fLookupOrder, false, null);
			setValue(curr, origValue);
		}
	}

	/**
	 * Updates fields and validates settings.
	 * 
	 * @param changedKey key that changed, or <code>null</code>, if all changed.
	 * @param oldValue old value or <code>null</code>
	 * @param newValue new value or <code>null</code>
	 */
	protected abstract void validateSettings(Key changedKey, String oldValue, String newValue);


	protected String[] getTokens(String text, String separator) {
		StringTokenizer tok= new StringTokenizer(text, separator);
		int nTokens= tok.countTokens();
		String[] res= new String[nTokens];
		for (int i= 0; i < res.length; i++) {
			res[i]= tok.nextToken().trim();
		}
		return res;
	}

	private boolean getChanges(IScopeContext currContext, List<Key> changedSettings) {
		boolean completeSettings= fProject != null && fDisabledProjectSettings == null; // complete when project settings are enabled
		boolean needsBuild= false;
		for (int i= 0; i < fAllKeys.length; i++) {
			Key key= fAllKeys[i];
			String oldVal= key.getStoredValue(currContext, null);
			String val= key.getStoredValue(currContext, fManager);
			if (val == null) {
				if (oldVal != null) {
					changedSettings.add(key);
					needsBuild |= !oldVal.equals(key.getStoredValue(fLookupOrder, true, fManager));
				} else if (completeSettings) {
					key.setStoredValue(currContext, key.getStoredValue(fLookupOrder, true, fManager), fManager);
					changedSettings.add(key);
					// no build needed
				}
			} else if (!val.equals(oldVal)) {
				changedSettings.add(key);
				needsBuild |= oldVal != null || !val.equals(key.getStoredValue(fLookupOrder, true, fManager));
			}
		}
		return needsBuild;
	}

	public void useProjectSpecificSettings(boolean enable) {
		boolean hasProjectSpecificOption= fDisabledProjectSettings == null;
		if (enable != hasProjectSpecificOption && fProject != null) {
			if (enable) {
				for (int i= 0; i < fAllKeys.length; i++) {
					Key curr= fAllKeys[i];
					String val= fDisabledProjectSettings.get(curr);
					curr.setStoredValue(fLookupOrder[0], val, fManager);
				}
				fDisabledProjectSettings= null;
				updateControls();
				validateSettings(null, null, null);
			} else {
				fDisabledProjectSettings= new IdentityHashMap<Key, String>();
				for (int i= 0; i < fAllKeys.length; i++) {
					Key curr= fAllKeys[i];
					String oldSetting= curr.getStoredValue(fLookupOrder, false, fManager);
					fDisabledProjectSettings.put(curr, oldSetting);
					curr.setStoredValue(fLookupOrder[0], null, fManager); // clear project settings
				}
			}
		}
	}

	public boolean areSettingsEnabled() {
		return fDisabledProjectSettings == null || fProject == null;
	}


	public boolean performOk() {
		return processChanges(fContainer);
	}

	public boolean performApply() {
		return processChanges(null); // apply directly
	}

	protected boolean processChanges(IWorkbenchPreferenceContainer container) {
		IScopeContext currContext= fLookupOrder[0];


		List<Key> changedOptions= new ArrayList<Key>();
		boolean needsBuild= getChanges(currContext, changedOptions);
		if (changedOptions.isEmpty()) {
			return true;
		}
		if (needsBuild) {
			int count= getRebuildCount();
			if (count > fRebuildCount) {
				needsBuild= false; // build already requested
				fRebuildCount= count;
			}
		}

		boolean doBuild= false;
		if (needsBuild) {
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
		}
		if (container != null) {
			// no need to apply the changes to the original store: will be done by the page container
			if (doBuild) { // post build
				incrementRebuildCount();
				container.registerUpdateJob(CoreUtility.getBuildJob(fProject));
			}
		} else {
			// apply changes right away
			try {
				fManager.applyChanges();
			} catch (BackingStoreException e) {
				JavaPlugin.log(e);
				return false;
			}
			if (doBuild) {
				CoreUtility.getBuildJob(fProject).schedule();
			}

		}
		return true;
	}

	protected abstract String[] getFullBuildDialogStrings(boolean workspaceSettings);


	public void performDefaults() {
		for (int i= 0; i < fAllKeys.length; i++) {
			Key curr= fAllKeys[i];
			String defValue= curr.getStoredValue(fLookupOrder, true, fManager);
			setValue(curr, defValue);
		}

		settingsUpdated();
		updateControls();
		validateSettings(null, null, null);
	}

	/**
	 * @since 3.1
	 */
	public void performRevert() {
		revertValues(fAllKeys);

		settingsUpdated();
		updateControls();
		validateSettings(null, null, null);
	}

	public void dispose() {
	}

	/**
	 * Updates the UI from the current settings. Must be called whenever a setting has been changed
	 * by code.
	 */
	protected void updateControls() {
		for (int i= fCheckBoxes.size() - 1; i >= 0; i--) {
			updateCheckBox(fCheckBoxes.get(i));
		}
		for (int i= fComboBoxes.size() - 1; i >= 0; i--) {
			updateCombo(fComboBoxes.get(i));
		}
		for (int i= fTextBoxes.size() - 1; i >= 0; i--) {
			updateText(fTextBoxes.get(i));
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

	protected ExpandableComposite getExpandableComposite(Key key) {
		for (int i= fExpandableComposites.size() - 1; i >= 0; i--) {
			ExpandableComposite curr= fExpandableComposites.get(i);
			Key data= (Key)curr.getData();
			if (key.equals(data)) {
				return curr;
			}
		}
		return null;
	}

	protected Button getCheckBox(Key key) {
		for (int i= fCheckBoxes.size() - 1; i >= 0; i--) {
			Button curr= fCheckBoxes.get(i);
			ControlData data= (ControlData) curr.getData();
			if (key.equals(data.getKey())) {
				return curr;
			}
		}
		return null;
	}

	protected Link getCheckBoxLink(Key key) {
		if (fCheckBoxes == null)
			return null;
		
		for (int i= fCheckBoxes.size() - 1; i >= 0; i--) {
			Button curr= fCheckBoxes.get(i);
			ControlData data= (ControlData) curr.getData();
			if (key.equals(data.getKey()) && data instanceof LinkControlData) {
				return ((LinkControlData)data).getLink();
			}
		}
		return null;
	}
	
	protected Combo getComboBox(Key key) {
		for (int i= fComboBoxes.size() - 1; i >= 0; i--) {
			Combo curr= fComboBoxes.get(i);
			ControlData data= (ControlData) curr.getData();
			if (key.equals(data.getKey())) {
				return curr;
			}
		}
		return null;
	}

	protected Text getTextControl(Key key) {
		for (int i= fTextBoxes.size() - 1; i >= 0; i--) {
			Text curr= fTextBoxes.get(i);
			Key data= (Key)curr.getData();
			if (key.equals(data)) {
				return curr;
			}
		}
		return null;
	}

	protected Control findControl(Key key) {
		Combo comboBox= getComboBox(key);
		if (comboBox != null) {
			return comboBox;
		}
		Button checkBox= getCheckBox(key);
		if (checkBox != null) {
			return checkBox;
		}
		Text text= getTextControl(key);
		if (text != null) {
			return text;
		}
		return null;
	}

	protected void setComboEnabled(Key key, boolean enabled) {
		Combo combo= getComboBox(key);
		Label label= fLabels.get(combo);
		combo.setEnabled(enabled);
		label.setEnabled(enabled);
	}
}
