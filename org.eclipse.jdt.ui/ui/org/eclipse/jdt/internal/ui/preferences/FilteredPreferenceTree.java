/*******************************************************************************
 * Copyright (c) 2018 Mateusz Matela and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mateusz Matela <mateusz.matela@gmail.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.action.LegacyActionTools;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.progress.WorkbenchJob;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.FilteredPreferenceTree.PreferenceTreeNode.ValueMatcher;
import org.eclipse.jdt.internal.ui.util.StringMatcher;

/**
 * The preferences modeled as a filtered tree.
 * <p>
 * The tree consists of an optional description label, a filter text input box, and a scrolled area.
 * The scrolled content contains all the UI controls which participate in filtering.
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
public class FilteredPreferenceTree {

	/**
	 * A node in <code>FilteredPreferenceTree</code>.
	 *
	 * @param <T> type of the node's main control
	 */
	public static class PreferenceTreeNode<T extends Control> {

		/**
		 * @param <T> class of the node's control
		 */
		public interface ValueMatcher<T extends Control> {
			boolean valueMatches(PreferenceTreeNode<T> node, StringMatcher matcher);
		}

		/**
		 * Label text of the preference which is used for filtering. This text does not contain
		 * <code>&</code> which is used to indicate mnemonics.
		 */
		private final String fLabel;

		/**
		 * A control associated with this node
		 */
		protected final T fControl;

		/**
		 * Tells whether all children should be shown even if just one child matches the filter.
		 */
		private final boolean fShowAllChildren;

		private final ValueMatcher<T> fValueMatcher;

		/**
		 * Tells whether this node's UI control is visible in the UI for the current filter text.
		 */
		private boolean fVisible= true;

		/**
		 * List of children nodes.
		 */
		private List<PreferenceTreeNode<?>> fChildren;
		private PreferenceTreeNode<?> fParent;

		/**
		 * Constructs a new instance of PreferenceTreeNode according to the parameters.
		 * <p>
		 * The <code>label</code> and the <code>key</code> must not be <code>null</code> if the node
		 * has a corresponding UI control.
		 * </p>
		 *
		 * @param label the label text
		 * @param control the control associated with this node,
		 * @param showAllChildren tells whether all children should be shown even if just one child
		 *            matches the filter.
		 */
		public PreferenceTreeNode(String label, T control, boolean showAllChildren) {
			this(label, control, showAllChildren, null);
		}

		public PreferenceTreeNode(String label, T control, boolean showAllChildren, ValueMatcher<T> valueMatcher) {
			if (label == null)
				label= ""; //$NON-NLS-1$
			fLabel= LegacyActionTools.removeMnemonics(label);
			fControl= control;
			fShowAllChildren= showAllChildren;
			fValueMatcher= valueMatcher;
		}

		public T getControl() {
			return fControl;
		}

		public List<PreferenceTreeNode<?>> getChildren() {
			return fChildren != null ? fChildren : Collections.<PreferenceTreeNode<?>>emptyList();
		}

		public PreferenceTreeNode<?> getParent() {
			return fParent;
		}

		public boolean isVisible() {
			return fVisible;
		}

		public void addChild(PreferenceTreeNode<?> node) {
			if (fChildren == null)
				fChildren= new ArrayList<>();
			fChildren.add(node);
			node.fParent= this;
		}

		protected boolean filter(StringMatcher labelMatcher, String ancestorsLabel, StringMatcher valueMatcher) {
			String currentLabel= fLabel;
			if (ancestorsLabel != null) {
				ancestorsLabel+= ' ' + currentLabel;
				currentLabel= ancestorsLabel;
			}

			//check this node
			boolean valueMatched= valueMatcher == null || (fValueMatcher != null && fValueMatcher.valueMatches(this, valueMatcher));
			boolean matched= valueMatched && (labelMatcher == null || labelMatcher.match(currentLabel));
			if (matched) {
				if (!valueMatched) {
					// label matched, now filter only by value
					labelMatcher= null;
					matched= false;
				} else {
					setVisible(true, true);
					return true;
				}
			}
			//check children
			if (fChildren != null) {
				for (PreferenceTreeNode<?> child : fChildren)
					matched|= child.filter(labelMatcher, ancestorsLabel, valueMatcher);
			}
			setVisible(matched, fShowAllChildren);
			return matched;
		}

		private void setVisible(boolean visible, boolean recursive) {
			fVisible= visible;
			if (fChildren != null && recursive) {
				for (PreferenceTreeNode<?> node : fChildren)
					node.setVisible(visible, recursive);
			}
		}

		public void setEnabled(boolean enabled) {
			fControl.setEnabled(enabled);
			if (fChildren != null) {
				for (PreferenceTreeNode<?> node : fChildren)
					node.setEnabled(enabled);
			}
		}
	}

	/**
	 * Subclass of {@link ScrolledPageContent} that can disable layout reflows for optimization
	 * purposes.
	 */
	protected static class ReflowControlScrolledPageContent extends ScrolledPageContent {
		private boolean fReflow= true;

		public ReflowControlScrolledPageContent(Composite parent) {
			super(parent);
		}

		public void setReflow(boolean reflow) {
			fReflow= reflow;
			if (reflow)
				reflow(true);
		}

		@Override
		public void reflow(boolean flushCache) {
			if (fReflow)
				super.reflow(flushCache);
		}
	}

	public static final ValueMatcher<Combo> COMBO_VALUE_MATCHER= (node, matcher) -> matcher.match(node.getControl().getText());

	public static final ValueMatcher<Text> TEXT_VALUE_MATCHER= (node, matcher) -> matcher.match(node.getControl().getText());

	public static final ValueMatcher<Spinner> SPINNER_VALUE_MATCHER= (node, matcher) -> matcher.match(Integer.toString(node.getControl().getSelection()));

	public static final ValueMatcher<Button> CHECK_BOX_MATCHER= (node, matcher) -> {
		boolean checked= node.getControl().getSelection();
		if (checked) {
			return matcher.match(PreferencesMessages.OptionsConfigurationBlock_On) || matcher.match(PreferencesMessages.OptionsConfigurationBlock_Enabled);
		} else {
			return matcher.match(PreferencesMessages.OptionsConfigurationBlock_Off) || matcher.match(PreferencesMessages.OptionsConfigurationBlock_Disabled);
		}
	};

	/**
	 * Root node for the tree. It does not have a corresponding UI control.
	 */
	protected final PreferenceTreeNode<Composite> fRoot;

	private boolean fConcatAncestorLabels= false;
	private boolean fExpectMultiWordValueMatch= false;

	/**
	 * The parent composite of <code>FilteredPreferenceTree</code>.
	 */
	private final Composite fParentComposite;

	/**
	 * The scrolled area of the tree.
	 */
	protected ReflowControlScrolledPageContent fScrolledPageContent;

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
	 * The description which can be <code>null</code>
	 */
	private Label fDescription;

	/**
	 * The filter text control.
	 */
	private FilterTextControl fFilterTextControl;

	private ToolItem fExpandAllItem;
	private ToolItem fCollapseAllItem;


	public FilteredPreferenceTree(Composite parentComposite, String label, String hint) {
		this(parentComposite, label, hint, true);
	}

	public FilteredPreferenceTree(Composite parentComposite, String label, String hint, boolean showVerticalBar) {
		fParentComposite= parentComposite;
		fRoot= new PreferenceTreeNode<>(null, null, false);

		createDescription(label);
		createFilterBox(hint);
		createScrolledArea(showVerticalBar);
		createNoMatchFoundLabel();

		fRefreshJob= doCreateRefreshJob();
		fRefreshJob.setSystem(true);
		fParentComposite.addDisposeListener(e -> fRefreshJob.cancel());
	}

	private void createDescription(String label) {
		if (label == null)
			return;

		fDescription= new Label(fParentComposite, SWT.LEFT | SWT.WRAP);
		fDescription.setFont(fParentComposite.getFont());
		fDescription.setText(label);
		fDescription.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
	}

	private void createFilterBox(String hint) {
		Composite composite= new Composite(fParentComposite, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		GridLayout layout= new GridLayout(2, false);
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.horizontalSpacing= 40;
		composite.setLayout(layout);
		composite.setFont(fParentComposite.getFont());

		//TODO: Directly use the hint flags once Bug 293230 is fixed
		fFilterTextControl= new FilterTextControl(composite);

		Text filterBox= fFilterTextControl.getFilterControl();
		filterBox.setMessage(hint);

		filterBox.addModifyListener(new ModifyListener() {
			private String fPrevFilterText;

			@Override
			public void modifyText(ModifyEvent e) {
				String input= filterBox.getText();
				fExpandAllItem.setEnabled(input.isEmpty());
				fCollapseAllItem.setEnabled(input.isEmpty());
				if (!input.equalsIgnoreCase(fPrevFilterText)) {
					fPrevFilterText= input;
					doFilter(input);
				}
			}
		});

		ToolBar toolbar= new ToolBar(composite, SWT.FLAT);
		toolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		fExpandAllItem= createExpansionItem(toolbar, true, JavaPluginImages.DESC_ELCL_EXPANDALL, JavaPluginImages.DESC_DLCL_EXPANDALL,
				PreferencesMessages.FilteredPreferencesTree_expandAll_tooltip);
		fCollapseAllItem= createExpansionItem(toolbar, false, JavaPluginImages.DESC_ELCL_COLLAPSEALL, JavaPluginImages.DESC_DLCL_COLLAPSEALL,
				PreferencesMessages.FilteredPreferencesTree_collapseAll_tooltip);
	}

	private ToolItem createExpansionItem(ToolBar toolBar, final boolean expand, ImageDescriptor image, ImageDescriptor disabledImage, String tooltip) {
		ToolItem item= new ToolItem(toolBar, SWT.PUSH);
		final Image createdImage= image.createImage();
		final Image createdDisabledImage= disabledImage.createImage();
		item.setImage(createdImage);
		item.setDisabledImage(createdDisabledImage);
		item.setToolTipText(tooltip);
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setAllExpanded(null, expand);
			}
		});
		item.addDisposeListener(e -> {
			createdImage.dispose();
			createdDisabledImage.dispose();
		});
		return item;
	}

	private void createScrolledArea(boolean showVerticalBar) {
		fScrolledPageContent= new ReflowControlScrolledPageContent(fParentComposite);
		fScrolledPageContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, ((GridLayout) fParentComposite.getLayout()).numColumns, 0));
		fScrolledPageContent.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				fScrolledPageContent.getVerticalBar().setVisible(showVerticalBar);
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

	/**
	 * @param concat if {@code true}, filtering will work as if each node has labels of its
	 *            ancestors concatenated at the beginning of its own label
	 */
	public void setConcatAncestorLabels(boolean concat) {
		fConcatAncestorLabels= concat;
	}

	/**
	 * @param expect if {@code true}, filter text starting with {@code ~} will be whole treated as value
	 *            filter instead of only the first word
	 */
	public void setExpectMultiWordValueMatch(boolean expect) {
		fExpectMultiWordValueMatch= expect;
	}

	public void doFilter(String filterText) {
		fRefreshJob.cancel();
		fRefreshJob.schedule(getRefreshJobDelay());
		filterText= filterText.trim();
		int index= filterText.indexOf('~');
		StringMatcher labelMatcher= null;
		StringMatcher valueMatcher= null;
		if (index == -1) {
			labelMatcher= createStringMatcher(filterText);
		} else {
			if (index == 0 && !fExpectMultiWordValueMatch) {
				int i= filterText.length();
				for (char ch : filterText.toCharArray()) {
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
		fMatchFound= fRoot.filter(labelMatcher, fConcatAncestorLabels ? "" : null, valueMatcher); //$NON-NLS-1$
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

	protected void updateUI(PreferenceTreeNode<?> node) {
		//update node
		Control control= node.getControl();

		if (control != null) {
			boolean visible= node.isVisible();
			setVisible(control, visible);
			if (control instanceof ExpandableComposite) {
				((ExpandableComposite) control).setExpanded(visible);
			}
		}

		//update children
		List<PreferenceTreeNode<?>> children= node.getChildren();
		if (children != null) {
			for (PreferenceTreeNode<?> element : children) {
				updateUI(element);
			}
		}
	}

	private WorkbenchJob doCreateRefreshJob() {
		return new WorkbenchJob(PreferencesMessages.OptionsConfigurationBlock_RefreshFilter) {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				fScrolledPageContent.setRedraw(false);
				fScrolledPageContent.setReflow(false);
				updateUI(fRoot);
				setVisible(fNoMatchFoundLabel, !fMatchFound);
				fScrolledPageContent.setReflow(true);
				Display.getCurrent().asyncExec(() -> fScrolledPageContent.setRedraw(true));
				return Status.OK_STATUS;
			}
		};
	}

	private void setVisible(Control control, boolean visible) {
		control.setVisible(visible);
		((GridData) control.getLayoutData()).exclude= !visible;
	}

	public <T extends PreferenceTreeNode<?>> T addChild(PreferenceTreeNode<?> parent, T node) {
		parent= (parent == null) ? fRoot : parent;
		parent.addChild(node);
		return node;
	}

	protected void setAllExpanded(PreferenceTreeNode<?> start, boolean expanded) {
		fScrolledPageContent.setRedraw(false);
		fScrolledPageContent.setReflow(false);

		ArrayDeque<PreferenceTreeNode<?>> bfsNodes= new ArrayDeque<>();
		if (start != null) {
			bfsNodes.add(start);
		} else {
			bfsNodes.addAll(fRoot.getChildren());
		}
		while (!bfsNodes.isEmpty()) {
			PreferenceTreeNode<?> node= bfsNodes.remove();
			bfsNodes.addAll(node.getChildren());
			if (node.getControl() instanceof ExpandableComposite)
				((ExpandableComposite) node.getControl()).setExpanded(expanded);
		}

		fScrolledPageContent.setReflow(true);
		Display.getCurrent().asyncExec(() -> fScrolledPageContent.setRedraw(true));
	}

	/**
	 * Enables the filtered preference tree if the argument is <code>true</code>, and disables it
	 * otherwise.
	 *
	 * @param enabled the new enabled state
	 *
	 * @since 3.16
	 */
	public void setEnabled(boolean enabled) {
		if (fDescription != null) {
			fDescription.setEnabled(enabled);
		}
		fFilterTextControl.setEnabled(enabled);
		fCollapseAllItem.setEnabled(enabled);
		fExpandAllItem.setEnabled(enabled);
		fRoot.getChildren().forEach(node -> node.setEnabled(enabled));
	}
}
