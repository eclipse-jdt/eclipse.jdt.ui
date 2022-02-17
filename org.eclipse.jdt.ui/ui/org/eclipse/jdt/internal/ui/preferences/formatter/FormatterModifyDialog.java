/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.FilteredPreferenceTree;
import org.eclipse.jdt.internal.ui.preferences.FilteredPreferenceTree.PreferenceTreeNode;
import org.eclipse.jdt.internal.ui.preferences.PreferenceHighlight;
import org.eclipse.jdt.internal.ui.preferences.formatter.ModifyDialog.ProfilePreferenceTree.SectionBuilder;
import org.eclipse.jdt.internal.ui.preferences.formatter.ModifyDialog.ProfilePreferenceTree.SimpleTreeBuilder;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.Profile;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.util.StringMatcher;


public class FormatterModifyDialog extends ModifyDialog {

	private static class LineWrapPreference extends Preference<ToolBar> {

		private static final String DATA_IMAGE_DISABLED= "image_disabled"; //$NON-NLS-1$

		@SuppressWarnings("boxing")
		private static final Object[][] WRAP_STYLE= {
				{ FormatterMessages.FormatterModifyDialog_lineWrap_val_do_not_split, JavaPluginImages.DESC_ELCL_WRAP_NOT, JavaPluginImages.DESC_DLCL_WRAP_NOT, DefaultCodeFormatterConstants.WRAP_NO_SPLIT },
				{ FormatterMessages.FormatterModifyDialog_lineWrap_val_wrap_when_necessary, JavaPluginImages.DESC_ELCL_WRAP_NECESSARY, JavaPluginImages.DESC_DLCL_WRAP_NECESSARY, DefaultCodeFormatterConstants.WRAP_COMPACT },
				{ FormatterMessages.FormatterModifyDialog_lineWrap_val_always_wrap_first_others_when_necessary, JavaPluginImages.DESC_ELCL_WRAP_FIRST_NECESSARY, JavaPluginImages.DESC_DLCL_WRAP_FIRST_NECESSARY, DefaultCodeFormatterConstants.WRAP_COMPACT_FIRST_BREAK },
				{ FormatterMessages.FormatterModifyDialog_lineWrap_val_wrap_always, JavaPluginImages.DESC_ELCL_WRAP_ALL, JavaPluginImages.DESC_DLCL_WRAP_ALL, DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE },
				{ FormatterMessages.FormatterModifyDialog_lineWrap_val_wrap_always_indent_all_but_first, JavaPluginImages.DESC_ELCL_WRAP_ALL_INDENT, JavaPluginImages.DESC_DLCL_WRAP_ALL_INDENT, DefaultCodeFormatterConstants.WRAP_NEXT_SHIFTED },
				{ FormatterMessages.FormatterModifyDialog_lineWrap_val_wrap_always_except_first_only_if_necessary, JavaPluginImages.DESC_ELCL_WRAP_ALL_NOT_FIRST, JavaPluginImages.DESC_DLCL_WRAP_ALL_NOT_FIRST, DefaultCodeFormatterConstants.WRAP_NEXT_PER_LINE },
		};

		@SuppressWarnings("boxing")
		private static final Object[][] INDENT_STYLE= {
				{ FormatterMessages.FormatterModifyDialog_lineWrap_val_indentation_default, JavaPluginImages.DESC_ELCL_INDENT_DEFAULT, JavaPluginImages.DESC_DLCL_INDENT_DEFAULT, DefaultCodeFormatterConstants.INDENT_DEFAULT },
				{ FormatterMessages.FormatterModifyDialog_lineWrap_val_indentation_by_one, JavaPluginImages.DESC_ELCL_INDENT_ONE, JavaPluginImages.DESC_DLCL_INDENT_ONE, DefaultCodeFormatterConstants.INDENT_BY_ONE },
				{ FormatterMessages.FormatterModifyDialog_lineWrap_val_indentation_on_column, JavaPluginImages.DESC_ELCL_INDENT_COLUMN, JavaPluginImages.DESC_DLCL_INDENT_COLUMN, DefaultCodeFormatterConstants.INDENT_ON_COLUMN },
		};

		private static final int VALUE_WRAP_BEFORE= 0;

		private static final int VALUE_WRAP_AFTER= 1;

		private static final List<String> WRAP_BEFORE_PREF_VALUES= Arrays.asList(DefaultCodeFormatterConstants.TRUE, DefaultCodeFormatterConstants.FALSE);

		@SuppressWarnings("boxing")
		private static final Object[][] WRAP_BEFORE_AFTER= {
				{ FormatterMessages.FormatterModifyDialog_lineWrap_val_wrap_before_operators, JavaPluginImages.DESC_ELCL_WRAP_BEFORE, JavaPluginImages.DESC_DLCL_WRAP_BEFORE, VALUE_WRAP_BEFORE },
				{ FormatterMessages.FormatterModifyDialog_lineWrap_val_wrap_after_operators, JavaPluginImages.DESC_ELCL_WRAP_AFTER, JavaPluginImages.DESC_DLCL_WRAP_AFTER, VALUE_WRAP_AFTER },
		};

		private static final ValueMatcher<ToolBar> VALUE_MATCHER= (node, matcher) -> ((LineWrapPreference) node).valueMatches(matcher);

		private final ToolItem fWrapStyleDropDown;

		private final List<MenuItem> fWrapStyleItems= new ArrayList<>();

		private final ToolItem fForceSplitItem;

		private final ToolItem fIndentationDropDown;

		private final List<MenuItem> fIndentationItems= new ArrayList<>();

		private final ToolItem fWrapBeforeAfterDropDown;

		private final List<MenuItem> fWrapBeforeAfterItems= new ArrayList<>();

		private final String fWrapBeforeKey;

		public static LineWrapPreference create(Composite parentComposite, String label, String key, String wrapBeforeKey, boolean withIndent, Images images) {
			ToolBar toolBar= new ToolBar(parentComposite, SWT.FLAT);
			toolBar.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

			LineWrapPreference lineWrapPreference= new LineWrapPreference(toolBar, label, key, wrapBeforeKey, withIndent, images);
			lineWrapPreference.addLabel(label, true, 0);

			return lineWrapPreference;
		}

		private LineWrapPreference(ToolBar toolBar, String label, String key, String wrapBeforeKey, boolean withIndent, Images images) {
			super(toolBar, label, key, VALUE_MATCHER);
			fWrapBeforeKey= wrapBeforeKey;
			if (wrapBeforeKey != null) {
				fWrapBeforeAfterDropDown= createDropDown(toolBar, WRAP_BEFORE_AFTER, fWrapBeforeAfterItems, images);
				addSeparator(toolBar);
			} else {
				fWrapBeforeAfterDropDown= null;
			}

			fWrapStyleDropDown= createDropDown(toolBar, WRAP_STYLE, fWrapStyleItems, images);
			addSeparator(toolBar);

			fForceSplitItem= new ToolItem(toolBar, SWT.CHECK);
			fForceSplitItem.setToolTipText(FormatterMessages.FormatterModifyDialog_lineWrap_val_force_split);
			fForceSplitItem.setImage(images.get(JavaPluginImages.DESC_ELCL_WRAP_FORCE));
			fForceSplitItem.setDisabledImage(images.get(JavaPluginImages.DESC_DLCL_WRAP_FORCE));
			addSeparator(toolBar);

			fIndentationDropDown= withIndent ? createDropDown(toolBar, INDENT_STYLE, fIndentationItems, images) : null;
		}

		private void addSeparator(ToolBar toolBar) {
			ToolItem item= new ToolItem(toolBar, SWT.SEPARATOR);
			item.setWidth(4);
			item.setControl(new Label(toolBar, SWT.NONE));
		}

		private ToolItem createDropDown(ToolBar toolBar, Object[][] menuItemsData, List<MenuItem> outItems, Images images) {
			final ToolItem dropDown= new ToolItem(toolBar, SWT.DROP_DOWN);
			final Menu menu= new Menu(toolBar.getShell());
			dropDown.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					fControl.setFocus();
					Rectangle rect= dropDown.getBounds();
					Point pt= dropDown.getParent().toDisplay(new Point(rect.x, rect.y));
					menu.setLocation(pt.x, pt.y + rect.height);
					menu.setVisible(true);
				}
			});

			for (Object[] itemData : menuItemsData) {
				MenuItem menuItem= new MenuItem(menu, SWT.RADIO);
				menuItem.setText((String) itemData[0]);
				menuItem.setImage(images.get((ImageDescriptor) itemData[1]));
				menuItem.setData(DATA_IMAGE_DISABLED, images.get((ImageDescriptor) itemData[2]));
				menuItem.setData(itemData[3]);
				outItems.add(menuItem);
			}
			return dropDown;
		}

		@Override
		protected void init(Map<String, String> workingValues, Predicate<String> valueValidator, Runnable valueChangeListener) {
			SelectionListener menuItemListener= new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					fControl.setFocus();
					updateValue();
					updateWidget();
				}
			};
			addSelectionListener(fWrapStyleItems, menuItemListener);
			fForceSplitItem.addSelectionListener(menuItemListener);
			addSelectionListener(fIndentationItems, menuItemListener);
			addSelectionListener(fWrapBeforeAfterItems, menuItemListener);
			super.init(workingValues, valueValidator, valueChangeListener);
		}

		@Override
		protected void updateWidget() {
			String value= getPreferences().get(getKey());
			int wrappingStyle= DefaultCodeFormatterConstants.getWrappingStyle(value);
			boolean wrapEnabled= wrappingStyle != DefaultCodeFormatterConstants.WRAP_NO_SPLIT;
			MenuItem wrapStyleItem= updateMenuItem(fWrapStyleItems, wrappingStyle);
			fWrapStyleDropDown.setToolTipText(FormatterMessages.FormatterModifyDialog_lineWrap_wrapping_policy_label + wrapStyleItem.getText());
			fWrapStyleDropDown.setImage(wrapStyleItem.getImage());

			boolean forceWrapping= DefaultCodeFormatterConstants.getForceWrapping(value);
			fForceSplitItem.setSelection(forceWrapping);
			fForceSplitItem.setEnabled(wrapEnabled);

			if (fIndentationDropDown != null) {
				MenuItem indentStyleItem= updateMenuItem(fIndentationItems, DefaultCodeFormatterConstants.getIndentStyle(value));
				fIndentationDropDown.setToolTipText(FormatterMessages.FormatterModifyDialog_lineWrap_indentation_policy_label + indentStyleItem.getText());
				fIndentationDropDown.setImage(indentStyleItem.getImage());
				fIndentationDropDown.setDisabledImage((Image) indentStyleItem.getData(DATA_IMAGE_DISABLED));
				fIndentationDropDown.setEnabled(wrapEnabled);
			}

			if (fWrapBeforeKey != null) {
				int wrapBeforeAfterValue= WRAP_BEFORE_PREF_VALUES.indexOf(getPreferences().get(fWrapBeforeKey));
				MenuItem wrapBeforeAfterItem= updateMenuItem(fWrapBeforeAfterItems, wrapBeforeAfterValue);
				fWrapBeforeAfterDropDown.setToolTipText(wrapBeforeAfterItem.getText());
				fWrapBeforeAfterDropDown.setImage(wrapBeforeAfterItem.getImage());
				fWrapBeforeAfterDropDown.setDisabledImage((Image) wrapBeforeAfterItem.getData(DATA_IMAGE_DISABLED));
				fWrapBeforeAfterDropDown.setEnabled(wrapEnabled);
			}
		}

		@Override
		protected void updateValue() {
			if (fWrapBeforeKey != null) {
				String value= WRAP_BEFORE_PREF_VALUES.get(getSelection(fWrapBeforeAfterItems));
				if (!value.equals(getPreferences().get(fWrapBeforeKey))) {
					getPreferences().put(fWrapBeforeKey, value);
					if (getValue().equals(getPreferences().get(getKey())))
						fValueChangeListener.run();
				}
			}
			super.updateValue();
		}

		@Override
		protected String getValue() {
			int wrapStyle= getSelection(fWrapStyleItems);
			boolean forceSplit= fForceSplitItem.getSelection();
			int indentStyle= fIndentationDropDown == null ? 0 : getSelection(fIndentationItems);
			return DefaultCodeFormatterConstants.createAlignmentValue(forceSplit, wrapStyle, indentStyle);
		}

		protected boolean valueMatches(StringMatcher matcher) {
			String value= getPreferences().get(getKey());
			int wrappingStyle= DefaultCodeFormatterConstants.getWrappingStyle(value);
			MenuItem wrapStyleItem= updateMenuItem(fWrapStyleItems, wrappingStyle);
			if (matcher.match(wrapStyleItem.getText()))
				return true;
			if (wrappingStyle == DefaultCodeFormatterConstants.WRAP_NO_SPLIT)
				return false;

			boolean forceWrapping= DefaultCodeFormatterConstants.getForceWrapping(value);
			if (forceWrapping && matcher.match(fForceSplitItem.getToolTipText()))
				return true;

			MenuItem indentStyleItem= updateMenuItem(fIndentationItems, DefaultCodeFormatterConstants.getIndentStyle(value));
			if (matcher.match(indentStyleItem.getText()))
				return true;

			if (fWrapBeforeKey != null) {
				int wrapBeforeAfterValue= WRAP_BEFORE_PREF_VALUES.indexOf(getPreferences().get(fWrapBeforeKey));
				MenuItem wrapBeforeAfterItem= updateMenuItem(fWrapBeforeAfterItems, wrapBeforeAfterValue);
				if (matcher.match(wrapBeforeAfterItem.getText()))
					return true;
			}

			return false;
		}

		private static void addSelectionListener(List<MenuItem> menuItems, SelectionListener menuItemListener) {
			for (MenuItem item : menuItems)
				item.addSelectionListener(menuItemListener);
		}

		private static MenuItem updateMenuItem(List<MenuItem> items, int currentData) {
			Integer data= currentData;
			MenuItem selected= null;
			for (MenuItem item : items) {
				item.setSelection(false);
				if (data.equals(item.getData()))
					selected= item;
			}
			if (selected == null)
				throw new AssertionError("No item found with data " + currentData); //$NON-NLS-1$
			selected.setSelection(true);
			return selected;
		}

		private static int getSelection(List<MenuItem> items) {
			for (MenuItem item : items) {
				if (item.getSelection())
					return ((Integer) item.getData());
			}
			throw new AssertionError("No item selected"); //$NON-NLS-1$
		}

		public static ModifyAll<ToolBar> addModifyAll(Section section, boolean withIndent, final Images images) {
			return new ModifyAll<ToolBar>(section, images) {
				private LineWrapPreference fPreference;

				@Override
				protected ToolBar createControl(Composite parent) {
					boolean needWrapBefore= false;
					for (LineWrapPreference pref : findPreferences(LineWrapPreference.class))
						needWrapBefore= needWrapBefore || pref.fWrapBeforeKey != null;

					fPreference= create(parent, null, "wrapPolicyKey", needWrapBefore ? "wrapBeforeKey" : null, withIndent, images); //$NON-NLS-1$ //$NON-NLS-2$
					fPreference.fControl.setLayoutData(null);

					addSelectionListener(fPreference.fWrapStyleItems, new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							int selectedValue= (int) ((MenuItem) e.getSource()).getData();
							for (LineWrapPreference pref : findPreferences(LineWrapPreference.class)) {
								updateMenuItem(pref.fWrapStyleItems, selectedValue);
								pref.updateValue();
								pref.updateWidget();
							}
							prepareControl();
						}
					});
					fPreference.fForceSplitItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							boolean selected= fPreference.fForceSplitItem.getSelection();
							for (LineWrapPreference pref : findPreferences(LineWrapPreference.class)) {
								pref.fForceSplitItem.setSelection(selected);
								pref.updateValue();
							}
							prepareControl();
						}
					});
					addSelectionListener(fPreference.fIndentationItems, new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							int selectedValue= (int) ((MenuItem) e.getSource()).getData();
							for (LineWrapPreference pref : findPreferences(LineWrapPreference.class)) {
								updateMenuItem(pref.fIndentationItems, selectedValue);
								pref.updateValue();
								pref.updateWidget();
							}
							prepareControl();
						}
					});
					addSelectionListener(fPreference.fWrapBeforeAfterItems, new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							int selectedValue= (int) ((MenuItem) e.getSource()).getData();
							for (LineWrapPreference pref : findPreferences(LineWrapPreference.class)) {
								if (!pref.fWrapBeforeAfterItems.isEmpty()) {
									updateMenuItem(pref.fWrapBeforeAfterItems, selectedValue);
									pref.updateValue();
									pref.updateWidget();
								}
							}
							prepareControl();
						}
					});

					return fPreference.fControl;
				}

				@Override
				protected void prepareControl() {
					final String STYLE= "style", FORCE= "force", INDENT= "indent", BEFORE= "before"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					List<LineWrapPreference> preferences= findPreferences(LineWrapPreference.class);
					Map<String, Integer> valueCounts= new HashMap<>();
					valueCounts.put(FORCE + true, 0);
					int hasWrapBeforeCount= 0;
					for (LineWrapPreference pref : preferences) {
						increment(STYLE + getSelection(pref.fWrapStyleItems), valueCounts);
						increment(FORCE + pref.fForceSplitItem.getSelection(), valueCounts);
						if (!pref.fIndentationItems.isEmpty()) {
							increment(INDENT + getSelection(pref.fIndentationItems), valueCounts);
						}
						if (!pref.fWrapBeforeAfterItems.isEmpty()) {
							increment(BEFORE + getSelection(pref.fWrapBeforeAfterItems), valueCounts);
							hasWrapBeforeCount++;
						}
					}

					prepareDrowpdown(fPreference.fWrapStyleDropDown, FormatterMessages.FormatterModifyDialog_lineWrap_wrapping_policy_label,
							prepareItems(fPreference.fWrapStyleItems, WRAP_STYLE, STYLE, valueCounts, preferences.size()));

					int forceWrapCount= valueCounts.get(FORCE + true);
					fPreference.fForceSplitItem.setSelection(forceWrapCount == preferences.size());
					fPreference.fForceSplitItem.setToolTipText(FormatterMessages.FormatterModifyDialog_lineWrap_val_force_split
							+ Messages.format(FormatterMessages.ModifyDialog_modifyAll_summary, new Object[] { forceWrapCount, preferences.size() }));

					if (!fPreference.fIndentationItems.isEmpty()) {
						prepareDrowpdown(fPreference.fIndentationDropDown, FormatterMessages.FormatterModifyDialog_lineWrap_indentation_policy_label,
								prepareItems(fPreference.fIndentationItems, INDENT_STYLE, INDENT, valueCounts, preferences.size()));
					}
					if (!fPreference.fWrapBeforeAfterItems.isEmpty()) {
						prepareDrowpdown(fPreference.fWrapBeforeAfterDropDown, "", //$NON-NLS-1$
								prepareItems(fPreference.fWrapBeforeAfterItems, WRAP_BEFORE_AFTER, BEFORE, valueCounts, hasWrapBeforeCount));
					}
				}

				private void increment(String key, Map<String, Integer> valueCounts) {
					valueCounts.merge(key, 1, Integer::sum);
				}

				private MenuItem prepareItems(List<MenuItem> items, Object[][] itemDatas, String keyPrefix, Map<String, Integer> valueCounts, int totalCount) {
					int maxCount= 0;
					int maxCountIndex= 0;
					for (int i= 0; i < itemDatas.length; i++) {
						MenuItem item= items.get(i);
						Integer count= valueCounts.get(keyPrefix + item.getData());
						String text= (String) itemDatas[i][0];
						if (count != null) {
							text+= Messages.format(FormatterMessages.ModifyDialog_modifyAll_summary, new Object[] { count, totalCount });
							if (count > maxCount) {
								maxCount= count;
								maxCountIndex= i;
							}
						}
						item.setText(text);
						item.setSelection(false);
					}
					MenuItem maxCountItem= items.get(maxCountIndex);
					maxCountItem.setSelection(true);
					return maxCountItem;
				}

				private void prepareDrowpdown(ToolItem dropDown, String dropDownLabel, MenuItem mostPopularItem) {
					dropDown.setToolTipText(dropDownLabel + mostPopularItem.getText());
					dropDown.setImage(mostPopularItem.getImage());
					dropDown.setEnabled(true);
				}
			};
		}
	}

	private static class BlankLinesPreference extends Preference<Spinner> {

		static final int MIN_LINES= 0;

		static final int MAX_LINES= 99;

		private final ToolItem fRemoveLinesItem;

		public static BlankLinesPreference create(Composite parentComposite, String label, String key, NumberPreference preserveLinesPref, Images images) {
			Spinner spinner= NumberPreference.createSpinner(parentComposite, MIN_LINES, MAX_LINES);

			ToolBar toolBar= new ToolBar(parentComposite, SWT.FLAT);
			ToolItem item= new ToolItem(toolBar, SWT.CHECK);
			item.setToolTipText(FormatterMessages.FormatterModifyDialog_blankLines_val_remove_extra_lines);
			item.setImage(images.get(JavaPluginImages.DESC_ELCL_REMOVE_EXTRA_LINES));
			item.setDisabledImage(images.get(JavaPluginImages.DESC_DLCL_REMOVE_EXTRA_LINES));

			return new BlankLinesPreference(spinner, toolBar, label, key, preserveLinesPref);
		}

		private BlankLinesPreference(Spinner spinner, ToolBar toolBar, String label, String key, NumberPreference preserveLinesPref) {
			super(spinner, label, key, FilteredPreferenceTree.SPINNER_VALUE_MATCHER);
			fRemoveLinesItem= toolBar.getItem(0);

			PreferenceTreeNode<?> toolBarNode= new PreferenceTreeNode<>(label, toolBar, true);
			addChild(toolBarNode);
			Predicate<String> valueChecker= v -> spinner.getSelection() < Integer.parseInt(preserveLinesPref.getValue());
			this.addDependant(toolBarNode, valueChecker);
			preserveLinesPref.addDependant(toolBarNode, valueChecker);

			Label labelControl= createLabel(GRID_COLUMNS - 3, spinner.getParent(), label, 0);
			labelControl.moveAbove(spinner);
			fHighlight= PreferenceHighlight.addHighlight(labelControl, spinner, false);
			addChild(new PreferenceTreeNode<>(label, labelControl, true));

			SelectionAdapter listener= new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					fControl.setFocus();
					updateValue();
				}
			};
			spinner.addSelectionListener(listener);
			fRemoveLinesItem.addSelectionListener(listener);
		}

		@Override
		protected void updateWidget() {
			int number;
			try {
				String s= getPreferences().get(getKey());
				number= Integer.parseInt(s);
			} catch (NumberFormatException x) {
				final String message= Messages.format(FormatterMessages.ModifyDialogTabPage_NumberPreference_error_invalid_key, getKey());
				JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, message, null));
				number= 0;
			}
			fRemoveLinesItem.setSelection(number < 0);
			if (number < 0)
				number= ~number;
			number= Math.max(fControl.getMinimum(), Math.min(fControl.getMaximum(), number));
			fControl.setSelection(number);
		}

		@Override
		protected String getValue() {
			int number= fControl.getSelection();
			return Integer.toString(fRemoveLinesItem.getSelection() ? ~number : number);
		}

		public static ModifyAll<Spinner> addModifyAll(Section section, final Images images) {
			return new ModifyAll<>(section, images) {

				private Label fLabel;
				private ToolItem fRemoveLinesItem;

				@Override
				protected Spinner createControl(Composite parent) {
					GridLayout layout= new GridLayout(3, false);
					layout.marginWidth= layout.marginHeight= 0;
					parent.setLayout(layout);

					fLabel= createLabel(1, parent, "", 0); //$NON-NLS-1$

					Spinner spinner= NumberPreference.createSpinner(parent, MIN_LINES, MAX_LINES);
					spinner.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							int value= fControl.getSelection();
							for (BlankLinesPreference pref : findPreferences(BlankLinesPreference.class)) {
								pref.getControl().setSelection(value);
								pref.updateValue();
							}
							prepareControl();
						}
					});

					ToolBar toolBar= new ToolBar(parent, SWT.FLAT);
					fRemoveLinesItem= new ToolItem(toolBar, SWT.CHECK);
					fRemoveLinesItem.setImage(images.get(JavaPluginImages.DESC_ELCL_REMOVE_EXTRA_LINES));
					fRemoveLinesItem.setDisabledImage(images.get(JavaPluginImages.DESC_DLCL_REMOVE_EXTRA_LINES));
					fRemoveLinesItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							boolean value= fRemoveLinesItem.getSelection();
							for (BlankLinesPreference pref : findPreferences(BlankLinesPreference.class)) {
								pref.fRemoveLinesItem.setSelection(value);
								pref.updateValue();
							}
							prepareControl();
						}
					});
					return spinner;
				}

				@Override
				protected void prepareControl() {
					int modeValue= 0;
					int modeCount= -1;
					HashMap<Integer, Integer> counts= new HashMap<>();
					int removeLinesCount= 0;
					List<BlankLinesPreference> preferences= findPreferences(BlankLinesPreference.class);
					for (BlankLinesPreference pref : preferences) {
						int value= pref.getControl().getSelection();
						int count= counts.merge(value, 1, Integer::sum);
						if (count > modeCount) {
							modeValue= value;
							modeCount= count;
						}

						if (pref.fRemoveLinesItem.getSelection())
							removeLinesCount++;
					}
					fControl.setSelection(modeValue);
					fLabel.setText(Messages.format(FormatterMessages.ModifyDialog_modifyAll_summary, new Object[] { modeCount, preferences.size() }));
					fLabel.requestLayout();
					fRemoveLinesItem.setToolTipText(FormatterMessages.FormatterModifyDialog_blankLines_val_remove_extra_lines + Messages.format(FormatterMessages.ModifyDialog_modifyAll_summary, new Object[] { removeLinesCount, preferences.size() }));
				}
			};
		}
	}

	private static final String DIALOG_PREFERENCE_KEY= "formatter_page"; //$NON-NLS-1$

	private static final String SHOW_INVISIBLE_PREFERENCE_KEY= JavaUI.ID_PLUGIN + '.' + DIALOG_PREFERENCE_KEY + ".show_invisible_characters"; //$NON-NLS-1$

	private static final String CUSTOM_PREVIEW_TOGGLE_PREFERENCE_KEY= JavaUI.ID_PLUGIN + '.' + DIALOG_PREFERENCE_KEY + ".custom_preview_toggle"; //$NON-NLS-1$
	private static final String CUSTOM_PREVIEW_CONTENT_PREFERENCE_KEY= JavaUI.ID_PLUGIN + '.' + DIALOG_PREFERENCE_KEY + ".custom_preview_content"; //$NON-NLS-1$

	/**
	 * The default preview line width.
	 */
	private static final int DEFAULT_PREVIEW_WINDOW_LINE_WIDTH= 40;

	/**
	 * The key to save the user's preview window width in the dialog settings.
	 */
	private static final String PREVIEW_LINE_WIDTH_PREFERENCE_KEY= JavaUI.ID_PLUGIN + ".codeformatter.line_wrapping_tab_page.preview_line_width"; //$NON-NLS-1$

	private Button fPreviewRawButton;

	public FormatterModifyDialog(Shell parentShell, Profile profile, ProfileManager profileManager, ProfileStore profileStore, boolean newProfile, String lastSavePathKey) {
		super(parentShell, profile, profileManager, profileStore, newProfile, DIALOG_PREFERENCE_KEY, lastSavePathKey);

		loadPreviews("formatter.java"); //$NON-NLS-1$
	}

	@Override
	protected Composite createPreviewPane(Composite parent) {
		Composite previewPane= super.createPreviewPane(parent);

		Composite controlPane= new Composite(previewPane, SWT.NONE);
		controlPane.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createGridLayout(controlPane, 2, false);
		Composite buttonsPane= new Composite(controlPane, SWT.NONE);
		buttonsPane.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false));
		createGridLayout(buttonsPane, 3, false);
		((GridLayout) buttonsPane.getLayout()).makeColumnsEqualWidth= true;

		fPreviewRawButton= new Button(buttonsPane, SWT.TOGGLE | SWT.WRAP);
		fPreviewRawButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		fPreviewRawButton.setText(FormatterMessages.FormatterModifyDialog_preview_show_raw_source_toggle);
		fPreviewRawButton.setFont(previewPane.getFont());
		fPreviewRawButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fPreview.setEditorMode(fPreviewRawButton.getSelection());
			}
		});

		Button customPreviewButton= new Button(buttonsPane, SWT.TOGGLE | SWT.WRAP);
		customPreviewButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		customPreviewButton.setText(FormatterMessages.FormatterModifyDialog_preview_custom_contents_toggle);
		customPreviewButton.setFont(previewPane.getFont());
		customPreviewButton.setSelection(fDialogSettings.getBoolean(CUSTOM_PREVIEW_TOGGLE_PREFERENCE_KEY));
		customPreviewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fDialogSettings.put(CUSTOM_PREVIEW_TOGGLE_PREFERENCE_KEY, customPreviewButton.getSelection());
				updatePreviewCode();
			}
		});
		fPreview.fSourceViewer.getTextWidget().addModifyListener(e -> {
			if (customPreviewButton.getSelection())
				fDialogSettings.put(CUSTOM_PREVIEW_CONTENT_PREFERENCE_KEY, ((StyledText) e.getSource()).getText());
		});

		final Button showInvisibleButton= new Button(buttonsPane, SWT.TOGGLE | SWT.WRAP);
		showInvisibleButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		showInvisibleButton.setText(FormatterMessages.FormatterModifyDialog_preview_show_whitespace_toggle);
		showInvisibleButton.setFont(previewPane.getFont());
		showInvisibleButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fPreview.showInvisibleCharacters(showInvisibleButton.getSelection());
				fDialogSettings.put(SHOW_INVISIBLE_PREFERENCE_KEY, showInvisibleButton.getSelection());
			}
		});
		boolean showInvisible= fDialogSettings.getBoolean(SHOW_INVISIBLE_PREFERENCE_KEY);
		fPreview.showInvisibleCharacters(showInvisible);
		showInvisibleButton.setSelection(showInvisible);

		Composite lineWidthPane= new Composite(controlPane, SWT.NONE);
		GridData lineWidthPaneLayoutData= new GridData(SWT.END, SWT.CENTER, true, false);
		lineWidthPane.setLayoutData(lineWidthPaneLayoutData);
		RowLayout layout= new RowLayout();
		layout.center= true;
		layout.justify= true;
		layout.marginLeft= layout.marginRight= layout.marginTop= layout.marginBottom= 0;
		lineWidthPane.setLayout(layout);
		Label lineWidthLabel= new Label(lineWidthPane, SWT.WRAP | SWT.CENTER);
		lineWidthLabel.setText(FormatterMessages.FormatterModifyDialog_preview_line_width_label);
		lineWidthLabel.setFont(previewPane.getFont());
		Spinner lineWidthSpinner= NumberPreference.createSpinner(lineWidthPane, 0, 9999);
		lineWidthSpinner.setFont(previewPane.getFont());
		lineWidthSpinner.setLayoutData(null);
		lineWidthPaneLayoutData.minimumWidth= lineWidthSpinner.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;

		int previewLineSplit;
		try {
			previewLineSplit= fDialogSettings.getInt(PREVIEW_LINE_WIDTH_PREFERENCE_KEY);
		} catch (NumberFormatException e) {
			previewLineSplit= DEFAULT_PREVIEW_WINDOW_LINE_WIDTH;
			fDialogSettings.put(PREVIEW_LINE_WIDTH_PREFERENCE_KEY, Integer.toString(previewLineSplit));
		}
		lineWidthSpinner.setSelection(previewLineSplit);
		lineWidthSpinner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fDialogSettings.put(PREVIEW_LINE_WIDTH_PREFERENCE_KEY, Integer.toString(lineWidthSpinner.getSelection()));
				valuesModified();
			}
		});
		valuesModified();

		return previewPane;
	}

	@Override
	public void valuesModified() {
		if (fPreview != null) {
			HashMap<String, String> workingValuesForPreview= new HashMap<>(fWorkingValues);
			workingValuesForPreview.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, fDialogSettings.get(PREVIEW_LINE_WIDTH_PREFERENCE_KEY));
			fPreview.setWorkingValues(workingValuesForPreview);

			fPreviewRawButton.setSelection(false);
			fPreview.setEditorMode(false);
			fPreview.update();
		}
	}

	@Override
	protected void createPreferenceTree(Composite parent) {
		super.createPreferenceTree(parent);

		createIndentationTree();
		createBracesTree();
		createParanthesesTree();
		createWhiteSpaceTree();
		createBlankLinesTree();
		createNewLinesTree();
		createLineWrapTree();
		createCommentsTree();
		createOffOnTree();
	}

	private void createIndentationTree() {
		final Section globalSection= fTree.addSection(null, FormatterMessages.FormatterModifyDialog_indentation_tree_indentation, "section-indentation"); //$NON-NLS-1$
		createGeneralIndentationPrefs(globalSection);
		fTree.addComboPref(globalSection, FormatterMessages.FormatterModifyDialog_indentation_pref_text_block_indentation, DefaultCodeFormatterConstants.FORMATTER_TEXT_BLOCK_INDENTATION,
				new String[] {
						String.valueOf(DefaultCodeFormatterConstants.INDENT_PRESERVE),
						String.valueOf(DefaultCodeFormatterConstants.INDENT_BY_ONE),
						String.valueOf(DefaultCodeFormatterConstants.INDENT_DEFAULT),
						String.valueOf(DefaultCodeFormatterConstants.INDENT_ON_COLUMN) },
				new String[] {
						FormatterMessages.FormatterModifyDialog_indentation_val_indentation_preserve,
						FormatterMessages.FormatterModifyDialog_lineWrap_val_indentation_by_one,
						FormatterMessages.FormatterModifyDialog_indentation_val_indentation_default,
						FormatterMessages.FormatterModifyDialog_lineWrap_val_indentation_on_column });
		fTree.addGap(globalSection);

		fTree.builder(FormatterMessages.FormatterModifyDialog_indentation_tree_indented_elements, null, s -> CheckboxPreference.addModifyAll(s, fImages))
				.pref(FormatterMessages.FormatterModifyDialog_indentation_pref_indent_declarations_within_class_body, DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_TYPE_HEADER)
				.pref(FormatterMessages.FormatterModifyDialog_indentation_pref_indent_declarations_within_enum_decl, DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_ENUM_DECLARATION_HEADER)
				.pref(FormatterMessages.FormatterModifyDialog_indentation_pref_indent_declarations_within_enum_const, DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_ENUM_CONSTANT_HEADER)
				.pref(FormatterMessages.FormatterModifyDialog_indentation_pref_indent_declarations_within_annot_decl, DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_ANNOTATION_DECLARATION_HEADER)
				.pref(FormatterMessages.FormatterModifyDialog_indentation_pref_indent_declarations_within_record_decl, DefaultCodeFormatterConstants.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_RECORD_HEADER)
				.gap()
				.pref(FormatterMessages.FormatterModifyDialog_indentation_pref_indent_statements_compare_to_body, DefaultCodeFormatterConstants.FORMATTER_INDENT_STATEMENTS_COMPARE_TO_BODY)
				.pref(FormatterMessages.FormatterModifyDialog_indentation_pref_indent_statements_compare_to_block, DefaultCodeFormatterConstants.FORMATTER_INDENT_STATEMENTS_COMPARE_TO_BLOCK)
				.gap()
				.pref(FormatterMessages.FormatterModifyDialog_indentation_pref_indent_statements_within_switch_body, DefaultCodeFormatterConstants.FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_SWITCH, pref -> {
					fTree.addCheckbox(pref, FormatterMessages.FormatterModifyDialog_indentation_pref_indent_statements_within_case_body,
							DefaultCodeFormatterConstants.FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_CASES, CheckboxPreference.FALSE_TRUE);
					fTree.addCheckbox(pref, FormatterMessages.FormatterModifyDialog_indentation_pref_indent_break_statements,
							DefaultCodeFormatterConstants.FORMATTER_INDENT_BREAKS_COMPARE_TO_CASES, CheckboxPreference.FALSE_TRUE);
				})
				.gap()
				.pref(FormatterMessages.FormatterModifyDialog_indentation_pref_indent_empty_lines, DefaultCodeFormatterConstants.FORMATTER_INDENT_EMPTY_LINES)
				.build(globalSection, (parent, label, key) -> fTree.addCheckbox(parent, label, key, CheckboxPreference.FALSE_TRUE));

		createAlignOnColumnPrefs(globalSection);
	}

	private void createGeneralIndentationPrefs(Section globalSection) {
		final String[] tabPolicyValues= new String[] { JavaCore.SPACE, JavaCore.TAB, DefaultCodeFormatterConstants.MIXED };
		final String[] tabPolicyLabels= new String[] {
				FormatterMessages.FormatterModifyDialog_indentation_tab_policy_SPACE,
				FormatterMessages.FormatterModifyDialog_indentation_tab_policy_TAB,
				FormatterMessages.FormatterModifyDialog_indentation_tab_policy_MIXED
		};
		final ComboPreference tabPolicyPref= fTree.addComboPref(globalSection, FormatterMessages.FormatterModifyDialog_indentation_pref_tab_policy, DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, tabPolicyValues, tabPolicyLabels);
		final CheckboxPreference onlyForLeadingPref= fTree.addCheckbox(globalSection, FormatterMessages.FormatterModifyDialog_indentation_pref_use_tabs_only_for_leading_indentations, DefaultCodeFormatterConstants.FORMATTER_USE_TABS_ONLY_FOR_LEADING_INDENTATIONS, CheckboxPreference.FALSE_TRUE);
		final NumberPreference indentSizePref= fTree.addNumberPref(globalSection, FormatterMessages.FormatterModifyDialog_indentation_pref_indent_size, DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, 0, 32);
		final NumberPreference tabSizePref= fTree.addNumberPref(globalSection, FormatterMessages.FormatterModifyDialog_indentation_pref_tab_size, DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, 0, 32);

		tabPolicyPref.setValueValidator(new Predicate<String>() {
			String fOldTabChar;
			{
				updateTabPreferences();
			}

			@Override
			public boolean test(String t) {
				updateTabPreferences();
				updateStatus(null);
				return true;
			}

			private void updateTabPreferences() {
				String tabPolicy= fWorkingValues.get(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR);
				/*
				 * If the tab-char is SPACE (or TAB), INDENTATION_SIZE
				 * preference is not used by the core formatter. We piggy back the
				 * visual tab length setting in that preference in that case. If the
				 * user selects MIXED, we use the previous TAB_SIZE preference as the
				 * new INDENTATION_SIZE (as this is what it really is) and set the
				 * visual tab size to the value piggy backed in the INDENTATION_SIZE
				 * preference. See also CodeFormatterUtil.
				 */
				if (DefaultCodeFormatterConstants.MIXED.equals(tabPolicy)) {
					if (JavaCore.SPACE.equals(fOldTabChar) || JavaCore.TAB.equals(fOldTabChar))
						swapTabValues();
					tabSizePref.setEnabled(true);
					tabSizePref.setKey(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE);
					indentSizePref.setEnabled(true);
					indentSizePref.setKey(DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE);
					onlyForLeadingPref.setEnabled(true);
				} else if (JavaCore.SPACE.equals(tabPolicy)) {
					if (DefaultCodeFormatterConstants.MIXED.equals(fOldTabChar))
						swapTabValues();
					tabSizePref.setEnabled(true);
					tabSizePref.setKey(DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE);
					indentSizePref.setEnabled(true);
					indentSizePref.setKey(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE);
					onlyForLeadingPref.setEnabled(false);
				} else if (JavaCore.TAB.equals(tabPolicy)) {
					if (DefaultCodeFormatterConstants.MIXED.equals(fOldTabChar))
						swapTabValues();
					tabSizePref.setEnabled(true);
					tabSizePref.setKey(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE);
					indentSizePref.setEnabled(false);
					indentSizePref.setKey(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE);
					onlyForLeadingPref.setEnabled(true);
				} else {
					Assert.isTrue(false);
				}
				fOldTabChar= tabPolicy;
			}

			private void swapTabValues() {
				String tabSize= fWorkingValues.get(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE);
				String indentSize= fWorkingValues.get(DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE);
				fWorkingValues.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, indentSize);
				fWorkingValues.put(DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE, tabSize);
			}
		});
		tabSizePref.setValueValidator(v -> {
			indentSizePref.updateWidget();
			updateStatus(null);
			return true;
		});
	}

	private void createAlignOnColumnPrefs(Section parentSection) {
		class CheckboxSpinnerPreference extends Preference<Button> {

			Spinner fSpinner;

			CheckboxSpinnerPreference(Button checkbox, Spinner spinner, String label, String key) {
				super(checkbox, label, key,
						(node, matcher) -> FilteredPreferenceTree.CHECK_BOX_MATCHER.valueMatches(node, matcher)
							|| matcher.match(Integer.toString(spinner.getSelection())));
				this.fSpinner = spinner;
				PreferenceTreeNode<Spinner> spinnerNode= new PreferenceTreeNode<>(label, spinner, true);
				addChild(spinnerNode);
				addDependant(spinnerNode, v -> !v.equals(String.valueOf(Integer.MAX_VALUE)));

				fHighlight= PreferenceHighlight.addHighlight(checkbox, spinner, false);

				SelectionAdapter listener= new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						updateValue();
					}
				};
				checkbox.addSelectionListener(listener);
				spinner.addSelectionListener(listener);
			}

			@Override
			protected void updateWidget() {
				int number;
				try {
					String s= getPreferences().get(getKey());
					number= Integer.parseInt(s);
					if (number != Integer.MAX_VALUE)
						number= Math.max(fSpinner.getMinimum(), Math.min(fSpinner.getMaximum(), number));
				} catch (NumberFormatException x) {
					final String message= Messages.format(FormatterMessages.ModifyDialogTabPage_NumberPreference_error_invalid_key, getKey());
					JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, message, null));
					number = Integer.MAX_VALUE;
				}
				fControl.setSelection(number != Integer.MAX_VALUE);
				fSpinner.setSelection(number != Integer.MAX_VALUE ? number : 1);
			}

			@Override
			protected String getValue() {
				return Integer.toString(fControl.getSelection() ? fSpinner.getSelection() : Integer.MAX_VALUE);
			}

			@Override
			public void setValueValidator(Predicate<String> valueValidator) {
				super.setValueValidator(valueValidator);
				fSpinner.addFocusListener(new FocusAdapter() {
					@Override
					public void focusLost(FocusEvent e) {
						valueValidator.test(null);
					}
				});
			}
		}

		final Section alignSection= fTree.addSection(parentSection, FormatterMessages.FormatterModifyDialog_indentation_tree_align_items_in_columns, "section-indentation-align-on-column"); //$NON-NLS-1$
		final CheckboxPreference alignFieldsPref= fTree.addCheckbox(alignSection, FormatterMessages.FormatterModifyDialog_indentation_pref_align_fields_in_columns,
				DefaultCodeFormatterConstants.FORMATTER_ALIGN_TYPE_MEMBERS_ON_COLUMNS, CheckboxPreference.FALSE_TRUE);
		final CheckboxPreference alignVariablesPref= fTree.addCheckbox(alignSection, FormatterMessages.FormatterModifyDialog_indentation_pref_align_variable_declarations_on_columns,
				DefaultCodeFormatterConstants.FORMATTER_ALIGN_VARIABLE_DECLARATIONS_ON_COLUMNS, CheckboxPreference.FALSE_TRUE);
		final CheckboxPreference alignAssignmentsPref= fTree.addCheckbox(alignSection, FormatterMessages.FormatterModifyDialog_indentation_pref_align_assignment_statements_on_columns,
				DefaultCodeFormatterConstants.FORMATTER_ALIGN_ASSIGNMENT_STATEMENTS_ON_COLUMNS, CheckboxPreference.FALSE_TRUE);

		fTree.addGap(alignSection);
		final CheckboxPreference useSpacesPref= fTree.addCheckbox(alignSection, FormatterMessages.FormatterModifyDialog_indentation_pref_align_with_spaces,
				DefaultCodeFormatterConstants.FORMATTER_ALIGN_WITH_SPACES, CheckboxPreference.FALSE_TRUE);
		Preference<?> tabCharPref= parentSection.findChildPreference(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR);
		Predicate<String> anyAlignChecker= v -> DefaultCodeFormatterConstants.TRUE.equals(alignFieldsPref.getValue())
				|| DefaultCodeFormatterConstants.TRUE.equals(alignVariablesPref.getValue())
				|| DefaultCodeFormatterConstants.TRUE.equals(alignAssignmentsPref.getValue());
		Predicate<String> spacesChecker= anyAlignChecker.and(v -> !JavaCore.SPACE.equals(tabCharPref.getValue()));
		alignFieldsPref.addDependant(useSpacesPref, spacesChecker);
		alignVariablesPref.addDependant(useSpacesPref, spacesChecker);
		alignAssignmentsPref.addDependant(useSpacesPref, spacesChecker);
		tabCharPref.addDependant(useSpacesPref, spacesChecker);

		Button checkbox = new Button(alignSection.fInnerComposite, SWT.CHECK);
		String label = FormatterMessages.FormatterModifyDialog_indentation_pref_blank_lines_separating_independent_groups;
		checkbox.setText(label);
		GridData gd= createGridData(GRID_COLUMNS - 2, GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_CENTER, SWT.DEFAULT, 0);
		checkbox.setLayoutData(gd);
		checkbox.setFont(alignSection.fInnerComposite.getFont());
		Spinner spinner= NumberPreference.createSpinner(alignSection.fInnerComposite, 1, 99);
		CheckboxSpinnerPreference groupingPref = new CheckboxSpinnerPreference(checkbox, spinner, label, DefaultCodeFormatterConstants.FORMATTER_ALIGN_FIELDS_GROUPING_BLANK_LINES);
		fTree.addChild(alignSection, groupingPref);
		alignFieldsPref.addDependant(groupingPref, anyAlignChecker);
		alignVariablesPref.addDependant(groupingPref, anyAlignChecker);
		alignAssignmentsPref.addDependant(groupingPref, anyAlignChecker);

		groupingPref.setValueValidator(value -> {
			String warningMessage= null;
			int groupingLines= value != null ? Integer.parseInt(value) : Integer.MAX_VALUE;
			if (value != null && groupingLines != Integer.MAX_VALUE) {
				int blankLinesToPreserve= Integer.parseInt(fWorkingValues.get(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE));
				boolean alignFields= Boolean.parseBoolean(fWorkingValues.get(DefaultCodeFormatterConstants.FORMATTER_ALIGN_TYPE_MEMBERS_ON_COLUMNS));
				int blankLinesBeforeField= Integer.parseInt(fWorkingValues.get(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_FIELD));
				if (groupingLines > blankLinesToPreserve) {
					warningMessage= Messages.format(FormatterMessages.FormatterModifyDialog_indentation_info_blank_lines_to_preserve, blankLinesToPreserve);
				} else if (alignFields && groupingLines <= blankLinesBeforeField) {
					warningMessage= Messages.format(FormatterMessages.FormatterModifyDialog_indentation_info_blank_lines_before_field, blankLinesBeforeField);
				} else if (alignFields && blankLinesBeforeField < 0 && groupingLines > ~blankLinesBeforeField) {
					warningMessage= Messages.format(FormatterMessages.FormatterModifyDialog_indentation_info_blank_lines_before_field_delete, ~blankLinesBeforeField);
				}
			}
			updateStatus(warningMessage == null ? null : new Status(IStatus.INFO, JavaPlugin.getPluginId(), 0, warningMessage, null));
			return true;
		});
	}

	private void createBracesTree() {
		final String[] bracePositions= {
				DefaultCodeFormatterConstants.END_OF_LINE,
				DefaultCodeFormatterConstants.NEXT_LINE,
				DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED,
				DefaultCodeFormatterConstants.NEXT_LINE_ON_WRAP
		};
		final String[] bracePositionNames= {
				FormatterMessages.FormatterModifyDialog_braces_val_same_line,
				FormatterMessages.FormatterModifyDialog_braces_val_next_line,
				FormatterMessages.FormatterModifyDialog_braces_val_next_line_indented,
				FormatterMessages.FormatterModifyDialog_braces_val_next_line_on_wrap
		};
		fTree.builder(FormatterMessages.FormatterModifyDialog_braces_tree_brace_positions, "section-braces", s -> ComboPreference.addModifyAll(s, fImages)) //$NON-NLS-1$
				.pref(FormatterMessages.FormatterModifyDialog_braces_pref_class_declaration, DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_TYPE_DECLARATION)
				.pref(FormatterMessages.FormatterModifyDialog_braces_pref_anonymous_class_declaration, DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ANONYMOUS_TYPE_DECLARATION)
				.pref(FormatterMessages.FormatterModifyDialog_braces_pref_constructor_declaration, DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_CONSTRUCTOR_DECLARATION)
				.pref(FormatterMessages.FormatterModifyDialog_braces_pref_method_declaration, DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_METHOD_DECLARATION)
				.pref(FormatterMessages.FormatterModifyDialog_braces_pref_enum_declaration, DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ENUM_DECLARATION)
				.pref(FormatterMessages.FormatterModifyDialog_braces_pref_enumconst_declaration, DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ENUM_CONSTANT)
				.pref(FormatterMessages.FormatterModifyDialog_braces_pref_record_declaration, DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_RECORD_DECLARATION)
				.pref(FormatterMessages.FormatterModifyDialog_braces_pref_record_constructor, DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_RECORD_CONSTRUCTOR)
				.pref(FormatterMessages.FormatterModifyDialog_braces_pref_annotation_type_declaration, DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ANNOTATION_TYPE_DECLARATION)
				.pref(FormatterMessages.FormatterModifyDialog_braces_pref_blocks, DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_BLOCK)
				.pref(FormatterMessages.FormatterModifyDialog_braces_pref_blocks_in_case, DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_BLOCK_IN_CASE)
				.pref(FormatterMessages.FormatterModifyDialog_braces_pref_switch_case, DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_SWITCH)
				.pref(FormatterMessages.FormatterModifyDialog_braces_pref_array_initializer, DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ARRAY_INITIALIZER, pref -> {
					CheckboxPreference emptyOnOneLine= fTree.addCheckbox(pref, FormatterMessages.FormatterModifyDialog_braces_pref_keep_empty_array_initializer_on_one_line,
							DefaultCodeFormatterConstants.FORMATTER_KEEP_EMPTY_ARRAY_INITIALIZER_ON_ONE_LINE, CheckboxPreference.FALSE_TRUE);
					pref.addDependant(emptyOnOneLine, valueAcceptor(Arrays.copyOfRange(bracePositions, 1, 4)));
				})
				.pref(FormatterMessages.FormatterModifyDialog_braces_pref_lambda_body, DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_LAMBDA_BODY)
				.build(null, (parent, label, key) -> fTree.addComboPref(parent, label, key, bracePositions, bracePositionNames));
	}

	private void createParanthesesTree() {
		fTree.builder(FormatterMessages.FormatterModifyDialog_parentheses_tree_parentheses_positions, "section-parentheses", s -> ComboPreference.addModifyAll(s, fImages)) //$NON-NLS-1$
				.pref(FormatterMessages.FormatterModifyDialog_parentheses_pref_method_declaration, DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_METHOD_DECLARATION)
				.pref(FormatterMessages.FormatterModifyDialog_parentheses_pref_method_invocation, DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_METHOD_INVOCATION)
				.pref(FormatterMessages.FormatterModifyDialog_parentheses_pref_enum_constant_declaration, DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_ENUM_CONSTANT_DECLARATION)
				.pref(FormatterMessages.FormatterModifyDialog_parentheses_pref_record_declaration, DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_RECORD_DECLARATION)
				.pref(FormatterMessages.FormatterModifyDialog_parentheses_pref_annotation, DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_ANNOTATION)
				.pref(FormatterMessages.FormatterModifyDialog_parentheses_pref_lambda_declaration, DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_LAMBDA_DECLARATION)
				.gap()
				.pref(FormatterMessages.FormatterModifyDialog_parentheses_pref_if_while_statement, DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_IF_WHILE_STATEMENT)
				.pref(FormatterMessages.FormatterModifyDialog_parentheses_pref_for_statement, DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_FOR_STATEMENT)
				.pref(FormatterMessages.FormatterModifyDialog_parentheses_pref_switch_statement, DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_SWITCH_STATEMENT)
				.pref(FormatterMessages.FormatterModifyDialog_parentheses_pref_try_clause, DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_TRY_CLAUSE)
				.pref(FormatterMessages.FormatterModifyDialog_parentheses_pref_catch_clause, DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_CATCH_CLAUSE)
				.build(null, new PreferenceBuilder() {
					String[] fValues= {
							DefaultCodeFormatterConstants.COMMON_LINES,
							DefaultCodeFormatterConstants.SEPARATE_LINES_IF_WRAPPED,
							DefaultCodeFormatterConstants.SEPARATE_LINES_IF_NOT_EMPTY,
							DefaultCodeFormatterConstants.SEPARATE_LINES,
							DefaultCodeFormatterConstants.PRESERVE_POSITIONS,
					};

					String[] fLabels= {
							FormatterMessages.FormatterModifyDialog_parentheses_val_common_lines,
							FormatterMessages.FormatterModifyDialog_parentheses_val_separate_lines_if_wrapped,
							FormatterMessages.FormatterModifyDialog_parentheses_val_separate_lines_if_not_empty,
							FormatterMessages.FormatterModifyDialog_parentheses_val_separate_lines,
							FormatterMessages.FormatterModifyDialog_parentheses_val_preserve_positions,
					};

					@Override
					public ComboPreference buildPreference(Section parent, String label, String key) {
						if (DefaultCodeFormatterConstants.FORMATTER_PARENTHESES_POSITIONS_IN_IF_WHILE_STATEMENT.equals(key)) {
							fValues= new String[] { fValues[0], fValues[1], fValues[3], fValues[4] };
							fLabels= new String[] { fLabels[0], fLabels[1], fLabels[3], fLabels[4] };
						}
						return fTree.addComboPref(parent, label, key, fValues, fLabels);
					}
				});
	}

	private void createWhiteSpaceTree() {
		Consumer<Section> modAll= s -> CheckboxPreference.addModifyAll(s, fImages);
		fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_whitespace, "section-whitespace", modAll) //$NON-NLS-1$
				.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_declarations, "-declarations", modAll) //$NON-NLS-1$
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_classes, "-classes", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_brace_of_a_class, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_TYPE_DECLARATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_brace_of_anon_class, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ANONYMOUS_TYPE_DECLARATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_comma_implements, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_SUPERINTERFACES)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_comma_implements, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_SUPERINTERFACES)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_comma_in_permits, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_PERMITTED_TYPES)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_comma_in_permits, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_PERMITTED_TYPES))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_fields, "-fields", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_comma_fields, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_MULTIPLE_FIELD_DECLARATIONS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_comma_fields, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MULTIPLE_FIELD_DECLARATIONS))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_localvars, "-localvars", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_comma_localvars, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_MULTIPLE_LOCAL_DECLARATIONS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_comma_localvars, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MULTIPLE_LOCAL_DECLARATIONS))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_constructors, "-constructors", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_CONSTRUCTOR_DECLARATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_opening_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CONSTRUCTOR_DECLARATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_closing_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CONSTRUCTOR_DECLARATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_between_empty_parens, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_CONSTRUCTOR_DECLARATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_brace, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_CONSTRUCTOR_DECLARATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_comma_in_params, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_DECLARATION_PARAMETERS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_comma_in_params, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_DECLARATION_PARAMETERS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_comma_in_throws, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_CONSTRUCTOR_DECLARATION_THROWS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_comma_in_throws, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_DECLARATION_THROWS))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_methods, "-methods", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_DECLARATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_opening_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_DECLARATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_closing_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_DECLARATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_between_empty_parens, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_DECLARATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_brace, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_METHOD_DECLARATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_comma_in_params, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_DECLARATION_PARAMETERS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_comma_in_params, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_DECLARATION_PARAMETERS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_ellipsis, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ELLIPSIS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_ellipsis, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ELLIPSIS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_comma_in_throws, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_DECLARATION_THROWS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_comma_in_throws, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_DECLARATION_THROWS))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_labels, "-labels", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_colon, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_LABELED_STATEMENT)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_colon, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_LABELED_STATEMENT))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_annotations, "-annotations", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_at, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_AT_IN_ANNOTATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_ANNOTATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_opening_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_ANNOTATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_comma, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ANNOTATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_comma, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ANNOTATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_closing_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_ANNOTATION))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_enums, "-enums", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_brace_decl, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ENUM_DECLARATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_comma_decl, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ENUM_DECLARATIONS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_comma_decl, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ENUM_DECLARATIONS)
								.gap()
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_paren_const_arg, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_ENUM_CONSTANT)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_opening_paren_const_arg, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_ENUM_CONSTANT)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_between_empty_parens_const_arg, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_ENUM_CONSTANT)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_comma_const_arg, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ENUM_CONSTANT_ARGUMENTS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_comma_const_arg, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ENUM_CONSTANT_ARGUMENTS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_closing_paren_const_arg, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_ENUM_CONSTANT)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_brace_enum_const, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ENUM_CONSTANT))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_annotation_types, "-annotationtypes", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_at, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_AT_IN_ANNOTATION_TYPE_DECLARATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_at, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_AT_IN_ANNOTATION_TYPE_DECLARATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_brace, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ANNOTATION_TYPE_DECLARATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_paren_annot_type, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_ANNOTATION_TYPE_MEMBER_DECLARATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_between_empty_parens_annot_type, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_ANNOTATION_TYPE_MEMBER_DECLARATION))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_records, "-records", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_RECORD_DECLARATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_opening_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_RECORD_DECLARATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_comma_in_record_components, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_RECORD_COMPONENTS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_comma_in_record_components, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_RECORD_COMPONENTS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_closing_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_RECORD_DECLARATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_brace_decl, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_RECORD_DECLARATION)
								.gap()
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_brace_in_record_constructor, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_RECORD_CONSTRUCTOR))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_lambda, "-lambdas", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_arrow_operator, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_LAMBDA_ARROW)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_arrow_operator, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_LAMBDA_ARROW)))
				.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_statements, "-statements", modAll) //$NON-NLS-1$
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_blocks, "-blocks", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_brace, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_BLOCK)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_closing_brace, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_BRACE_IN_BLOCK))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_if, "-if", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_IF)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_opening_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_IF)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_closing_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_IF))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_for, "-for", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_FOR)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_opening_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_FOR)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_closing_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_FOR)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_comma_init, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_FOR_INITS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_comma_init, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_FOR_INITS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_comma_inc, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_FOR_INCREMENTS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_comma_inc, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_FOR_INCREMENTS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_semicolon, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SEMICOLON_IN_FOR)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_semicolon, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_SEMICOLON_IN_FOR)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_colon, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_FOR)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_colon, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_FOR))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_switch, "-switch", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_colon_case, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_CASE)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_colon_default, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_DEFAULT)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_arrow_in_case, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ARROW_IN_SWITCH_CASE)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_arrow_in_case, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ARROW_IN_SWITCH_CASE)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_arrow_in_default, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ARROW_IN_SWITCH_DEFAULT)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_arrow_in_default, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ARROW_IN_SWITCH_DEFAULT)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_comma_in_case_expressions, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_SWITCH_CASE_EXPRESSIONS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_comma_in_case_expressions, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_SWITCH_CASE_EXPRESSIONS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_SWITCH)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_opening_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_SWITCH)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_closing_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_SWITCH)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_brace, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_SWITCH))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_do, "-while", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_WHILE)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_opening_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_WHILE)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_closing_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_WHILE))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_synchronized, "-synchronized", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_SYNCHRONIZED)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_opening_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_SYNCHRONIZED)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_closing_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_SYNCHRONIZED))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_try_with_resources, "-trywithresources", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_TRY)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_opening_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_TRY)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_semicolon, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SEMICOLON_IN_TRY_RESOURCES)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_semicolon, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_SEMICOLON_IN_TRY_RESOURCES)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_closing_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_TRY))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_catch, "-catch", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_CATCH)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_opening_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CATCH)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_closing_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CATCH))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_assert, "-assert", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_colon, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_ASSERT)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_colon, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_ASSERT))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_return, "-return", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_parenthesized_expressions, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_PARENTHESIZED_EXPRESSION_IN_RETURN))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_throw, "-throw") //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_parenthesized_expressions, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_PARENTHESIZED_EXPRESSION_IN_THROW))
						.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_semicolon, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SEMICOLON))
				.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_expressions, "-expressions", modAll) //$NON-NLS-1$
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_calls, "-calls", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_METHOD_INVOCATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_opening_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_METHOD_INVOCATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_closing_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_METHOD_INVOCATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_between_empty_parens, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_PARENS_IN_METHOD_INVOCATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_comma_in_method_args, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_METHOD_INVOCATION_ARGUMENTS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_comma_in_method_args, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_INVOCATION_ARGUMENTS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_comma_in_alloc, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ALLOCATION_EXPRESSION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_comma_in_alloc, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ALLOCATION_EXPRESSION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_comma_in_qalloc, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_EXPLICIT_CONSTRUCTOR_CALL_ARGUMENTS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_comma_in_qalloc, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_EXPLICIT_CONSTRUCTOR_CALL_ARGUMENTS))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_unary_operators, "-unaryoperators", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_postfix_operators, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_POSTFIX_OPERATOR)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_postfix_operators, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_POSTFIX_OPERATOR)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_prefix_operators, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_PREFIX_OPERATOR)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_prefix_operators, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_PREFIX_OPERATOR)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_unary_operators, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_UNARY_OPERATOR)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_unary_operators, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_UNARY_OPERATOR, pref -> {
									fTree.addCheckbox(pref, FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_not_operator, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_NOT_OPERATOR, CheckboxPreference.DO_NOT_INSERT_INSERT);
								}))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_binary_operators, "-binaryoperators", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_multiplicative_operator, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_MULTIPLICATIVE_OPERATOR)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_multiplicative_operator, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_MULTIPLICATIVE_OPERATOR)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_additive_operator, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ADDITIVE_OPERATOR)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_additive_operator, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ADDITIVE_OPERATOR)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_string_concatenation, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_STRING_CONCATENATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_string_concatenation, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_STRING_CONCATENATION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_shift_operator, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SHIFT_OPERATOR)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_shift_operator, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_SHIFT_OPERATOR)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_relational_operator, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_RELATIONAL_OPERATOR)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_relational_operator, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_RELATIONAL_OPERATOR)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_bitwise_operator, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_BITWISE_OPERATOR)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_bitwise_operator, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_BITWISE_OPERATOR)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_logical_operator, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_LOGICAL_OPERATOR)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_logical_operator, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_LOGICAL_OPERATOR))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_conditionals, "-conditionals", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_question, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_QUESTION_IN_CONDITIONAL)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_question, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_QUESTION_IN_CONDITIONAL)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_colon, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_CONDITIONAL)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_colon, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_CONDITIONAL))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_assignments, "-assignments", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_assignment_operator, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_assignment_operator, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ASSIGNMENT_OPERATOR))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_parenexpr, "-parenexpr", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_opening_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_closing_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_PARENTHESIZED_EXPRESSION))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_typecasts, "-typecasts", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_opening_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_CAST)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_closing_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_CAST)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_closing_paren, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_PAREN_IN_CAST)))
				.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_arrays, "-arrays", modAll) //$NON-NLS-1$
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_arraydecls, "-declarations", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_bracket, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_TYPE_REFERENCE)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_between_empty_brackets, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_BRACKETS_IN_ARRAY_TYPE_REFERENCE))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_arrayalloc, "-allocations", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_bracket, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_ALLOCATION_EXPRESSION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_opening_bracket, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACKET_IN_ARRAY_ALLOCATION_EXPRESSION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_closing_bracket, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACKET_IN_ARRAY_ALLOCATION_EXPRESSION)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_between_empty_brackets, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_BRACKETS_IN_ARRAY_ALLOCATION_EXPRESSION))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_arrayinit, "-initializers", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_brace, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ARRAY_INITIALIZER)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_opening_brace, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACE_IN_ARRAY_INITIALIZER)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_closing_brace, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACE_IN_ARRAY_INITIALIZER)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_comma, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_ARRAY_INITIALIZER)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_comma, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ARRAY_INITIALIZER)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_between_empty_braces, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_BRACES_IN_ARRAY_INITIALIZER))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_arrayelem, "-references", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_bracket, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACKET_IN_ARRAY_REFERENCE)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_opening_bracket, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACKET_IN_ARRAY_REFERENCE)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_closing_bracket, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACKET_IN_ARRAY_REFERENCE)))
				.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_parameterized_types, "-parameterizedtypes", modAll) //$NON-NLS-1$
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_param_type_ref, "-references", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_angle_bracket, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_ANGLE_BRACKET_IN_PARAMETERIZED_TYPE_REFERENCE)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_opening_angle_bracket, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_ANGLE_BRACKET_IN_PARAMETERIZED_TYPE_REFERENCE)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_comma, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_PARAMETERIZED_TYPE_REFERENCE)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_comma, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_PARAMETERIZED_TYPE_REFERENCE)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_closing_angle_bracket, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_ANGLE_BRACKET_IN_PARAMETERIZED_TYPE_REFERENCE))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_type_arguments, "-arguments", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_angle_bracket, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_ANGLE_BRACKET_IN_TYPE_ARGUMENTS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_opening_angle_bracket, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_ANGLE_BRACKET_IN_TYPE_ARGUMENTS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_comma, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_TYPE_ARGUMENTS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_comma, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_TYPE_ARGUMENTS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_closing_angle_bracket, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_ANGLE_BRACKET_IN_TYPE_ARGUMENTS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_closing_angle_bracket, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_ANGLE_BRACKET_IN_TYPE_ARGUMENTS))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_type_parameters, "-parameters", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_opening_angle_bracket, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_ANGLE_BRACKET_IN_TYPE_PARAMETERS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_opening_angle_bracket, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_ANGLE_BRACKET_IN_TYPE_PARAMETERS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_comma_in_params, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COMMA_IN_TYPE_PARAMETERS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_comma_in_params, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_TYPE_PARAMETERS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_closing_angle_bracket, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_ANGLE_BRACKET_IN_TYPE_PARAMETERS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_closing_angle_bracket, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_ANGLE_BRACKET_IN_TYPE_PARAMETERS)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_and_list, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_AND_IN_TYPE_PARAMETER)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_and_list, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_AND_IN_TYPE_PARAMETER))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_whiteSpace_tree_wildcardtype, "-wildcards", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_before_question, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_QUESTION_IN_WILDCARD)
								.pref(FormatterMessages.FormatterModifyDialog_whiteSpace_pref_after_question, DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_QUESTION_IN_WILDCARD)))
				.build(null, (parent, label, key) -> fTree.addCheckbox(parent, label, key, CheckboxPreference.DO_NOT_INSERT_INSERT));
	}

	private void createBlankLinesTree() {
		Consumer<Section> modAll= s -> BlankLinesPreference.addModifyAll(s, fImages);
		fTree.builder(FormatterMessages.FormatterModifyDialog_blankLines_tree_blank_lines, "section-blank-lines") //$NON-NLS-1$
				.pref(FormatterMessages.FormatterModifyDialog_blankLines_pref_empty_lines_to_preserve, DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE)
				.node(fTree.builder(FormatterMessages.FormatterModifyDialog_blankLines_tree_compilation_unit, null, modAll)
						.pref(FormatterMessages.FormatterModifyDialog_blankLines_pref_before_package, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_PACKAGE)
						.pref(FormatterMessages.FormatterModifyDialog_blankLines_pref_after_package, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_PACKAGE)
						.pref(FormatterMessages.FormatterModifyDialog_blankLines_pref_before_import, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_IMPORTS)
						.pref(FormatterMessages.FormatterModifyDialog_blankLines_pref_between_import_groups, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BETWEEN_IMPORT_GROUPS)
						.pref(FormatterMessages.FormatterModifyDialog_blankLines_pref_after_import, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_IMPORTS)
						.pref(FormatterMessages.FormatterModifyDialog_blankLines_pref_between_type_declarations, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BETWEEN_TYPE_DECLARATIONS))
				.node(fTree.builder(FormatterMessages.FormatterModifyDialog_blankLines_tree_class_declarations, null, modAll)
						.pref(FormatterMessages.FormatterModifyDialog_blankLines_pref_before_first_decl, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_FIRST_CLASS_BODY_DECLARATION)
						.pref(FormatterMessages.FormatterModifyDialog_blankLines_pref_after_last_decl, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_LAST_CLASS_BODY_DECLARATION)
						.pref(FormatterMessages.FormatterModifyDialog_blankLines_pref_before_decls_of_same_kind, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_NEW_CHUNK)
						.pref(FormatterMessages.FormatterModifyDialog_blankLines_pref_before_member_class_decls, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_MEMBER_TYPE)
						.pref(FormatterMessages.FormatterModifyDialog_blankLines_pref_before_field_decls, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_FIELD)
						.pref(FormatterMessages.FormatterModifyDialog_blankLines_pref_before_abstract_method_decls, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_ABSTRACT_METHOD)
						.pref(FormatterMessages.FormatterModifyDialog_blankLines_pref_before_method_decls, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_METHOD))
				.node(fTree.builder(FormatterMessages.FormatterModifyDialog_blankLines_tree_method_declarations, null, modAll)
						.pref(FormatterMessages.FormatterModifyDialog_blankLines_pref_at_beginning_of_method_body, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AT_BEGINNING_OF_METHOD_BODY)
						.pref(FormatterMessages.FormatterModifyDialog_blankLines_pref_at_end_of_method_body, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AT_END_OF_METHOD_BODY)
						.pref(FormatterMessages.FormatterModifyDialog_blankLines_pref_at_beginning_of_code_block, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AT_BEGINNING_OF_CODE_BLOCK)
						.pref(FormatterMessages.FormatterModifyDialog_blankLines_pref_at_end_of_code_block, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AT_END_OF_CODE_BLOCK)
						.pref(FormatterMessages.FormatterModifyDialog_blankLines_pref_before_code_block, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_CODE_BLOCK)
						.pref(FormatterMessages.FormatterModifyDialog_blankLines_pref_after_code_block, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_CODE_BLOCK)
						.pref(FormatterMessages.FormatterModifyDialog_blankLines_pref_between_statement_groups_in_switch, DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BETWEEN_STATEMENT_GROUPS_IN_SWITCH))
				.build(null, new PreferenceBuilder() {
					NumberPreference fPreserveLinesPref;

					@Override
					public Preference<?> buildPreference(Section parent, String label, String key) {
						if (DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE.equals(key)) {
							fPreserveLinesPref= fTree.addNumberPref(parent, label, key, BlankLinesPreference.MIN_LINES, BlankLinesPreference.MAX_LINES);
							return fPreserveLinesPref;
						}
						BlankLinesPreference pref= BlankLinesPreference.create(parent.fInnerComposite, label, key, fPreserveLinesPref, fImages);
						fTree.addChild(parent, pref);
						return pref;
					}
				});
	}

	private void createNewLinesTree() {
		Consumer<Section> modAll= s -> CheckboxPreference.addModifyAll(s, fImages);
		fTree.builder(FormatterMessages.FormatterModifyDialog_newLines_tree_new_lines, "section-newlines") //$NON-NLS-1$
				.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_empty_statement, DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE)
				.gap()
				.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_after_opening_brace_of_array_initializer,
						DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_OPENING_BRACE_IN_ARRAY_INITIALIZER)
				.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_before_closing_brace_of_array_initializer,
						DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_CLOSING_BRACE_IN_ARRAY_INITIALIZER)
				.gap()
				.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_end_of_file, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AT_END_OF_FILE_IF_MISSING)
				.node(fTree.builder(FormatterMessages.FormatterModifyDialog_newLines_tree_control_statements, "-controlstatements", modAll) //$NON-NLS-1$
						.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_before_else_statements, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_ELSE_IN_IF_STATEMENT)
						.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_before_catch_statements, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_CATCH_IN_TRY_STATEMENT)
						.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_before_finally_statements, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_FINALLY_IN_TRY_STATEMENT)
						.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_before_while_in_do_statements, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_WHILE_IN_DO_STATEMENT)
						.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_after_labels, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_LABEL)
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_newLines_tree_if_else, "-ifelse", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_keep_then_on_same_line, DefaultCodeFormatterConstants.FORMATTER_KEEP_THEN_STATEMENT_ON_SAME_LINE, pref -> {
									CheckboxPreference child= fTree.addCheckbox(pref, FormatterMessages.FormatterModifyDialog_newLines_pref_keep_simple_if_on_one_line,
											DefaultCodeFormatterConstants.FORMATTER_KEEP_SIMPLE_IF_ON_ONE_LINE, CheckboxPreference.FALSE_TRUE);
									pref.addDependant(child, valueAcceptor(DefaultCodeFormatterConstants.FALSE));
								})
								.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_keep_else_on_same_line, DefaultCodeFormatterConstants.FORMATTER_KEEP_ELSE_STATEMENT_ON_SAME_LINE)
								.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_keep_else_if_on_one_line, DefaultCodeFormatterConstants.FORMATTER_COMPACT_ELSE_IF))
						.node(fTree.builder(FormatterMessages.FormatterModifyDialog_newLines_tree_simple_loops, "-simpleloops", modAll) //$NON-NLS-1$
								.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_keep_simple_for_body_on_one_line, DefaultCodeFormatterConstants.FORMATTER_KEEP_SIMPLE_FOR_BODY_ON_SAME_LINE)
								.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_keep_simple_while_body_on_one_line, DefaultCodeFormatterConstants.FORMATTER_KEEP_SIMPLE_WHILE_BODY_ON_SAME_LINE)
								.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_keep_simple_do_while_body_on_one_line, DefaultCodeFormatterConstants.FORMATTER_KEEP_SIMPLE_DO_WHILE_BODY_ON_SAME_LINE)))
				.node(fTree.builder(FormatterMessages.FormatterModifyDialog_newLines_tree_after_annotations, "-annotations", modAll) //$NON-NLS-1$
						.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_packages, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_PACKAGE)
						.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_types, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_TYPE)
						.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_enum_constants, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_ENUM_CONSTANT)
						.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_fields, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_FIELD)
						.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_methods, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_METHOD)
						.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_local_variables, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_LOCAL_VARIABLE)
						.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_paramters, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_PARAMETER)
						.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_type_annotations, DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_TYPE_ANNOTATION))
				.node(createKeepOnOneLineSection())
				.build(null, (parent, label, key) -> {
					String[] values= CheckboxPreference.DO_NOT_INSERT_INSERT;
					if (parent.getKey().endsWith("-ifelse") || parent.getKey().endsWith("-simpleloops") //$NON-NLS-1$ //$NON-NLS-2$
							|| DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE.equals(key)) {
						values= CheckboxPreference.FALSE_TRUE;
					}
					return fTree.addCheckbox(parent, label, key, values);
				});
	}

	private SimpleTreeBuilder<?> createKeepOnOneLineSection() {
		String[] oneLineOptions= {
				DefaultCodeFormatterConstants.ONE_LINE_NEVER,
				DefaultCodeFormatterConstants.ONE_LINE_IF_EMPTY,
				DefaultCodeFormatterConstants.ONE_LINE_IF_SINGLE_ITEM,
				DefaultCodeFormatterConstants.ONE_LINE_ALWAYS,
				DefaultCodeFormatterConstants.ONE_LINE_PRESERVE,
		};
		String[] oneLineLabels= {
				FormatterMessages.FormatterModifyDialog_newLines_val_one_line_never,
				FormatterMessages.FormatterModifyDialog_newLines_val_one_line_if_empty,
				FormatterMessages.FormatterModifyDialog_newLines_val_one_line_if_single_item,
				FormatterMessages.FormatterModifyDialog_newLines_val_one_line_always,
				FormatterMessages.FormatterModifyDialog_newLines_val_one_line_preserve,
		};
		PreferenceBuilder prefBuilder= (parent, label, key) -> {
			String[] values= oneLineOptions;
			String[] items= oneLineLabels;
			if (DefaultCodeFormatterConstants.FORMATTER_KEEP_CODE_BLOCK_ON_ONE_LINE.equals(key)) {
				values= Arrays.copyOf(values, 2);
				items= Arrays.copyOf(items, 2);
			}
			return fTree.addComboPref(parent, label, key, values, items);
		};
		SectionBuilder sectionBuilder= fTree
				.builder(FormatterMessages.FormatterModifyDialog_newLines_tree_keep_braced_code_on_one_line, "-keepononeline", s -> ComboPreference.addModifyAll(s, fImages)) //$NON-NLS-1$
				.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_keep_loop_body_block_on_one_line, DefaultCodeFormatterConstants.FORMATTER_KEEP_LOOP_BODY_BLOCK_ON_ONE_LINE)
				.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_keep_if_then_body_block_on_one_line, DefaultCodeFormatterConstants.FORMATTER_KEEP_IF_THEN_BODY_BLOCK_ON_ONE_LINE, pref -> {
					CheckboxPreference guardianPref= fTree.addCheckbox(pref, FormatterMessages.FormatterModifyDialog_newLines_pref_keep_guardian_clause_on_one_line,
							DefaultCodeFormatterConstants.FORMATTER_KEEP_GUARDIAN_CLAUSE_ON_ONE_LINE, CheckboxPreference.FALSE_TRUE);
					pref.addDependant(guardianPref, valueAcceptor(oneLineOptions[0], oneLineOptions[1], oneLineOptions[4]));
				})
				.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_keep_lambda_body_block_on_one_line, DefaultCodeFormatterConstants.FORMATTER_KEEP_LAMBDA_BODY_BLOCK_ON_ONE_LINE)
				.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_keep_switch_case_with_arrow_on_one_line, DefaultCodeFormatterConstants.FORMATTER_KEEP_SWITCH_CASE_WITH_ARROW_ON_ONE_LINE)
				.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_keep_switch_body_block_on_one_line, DefaultCodeFormatterConstants.FORMATTER_KEEP_SWITCH_BODY_BLOCK_ON_ONE_LINE)
				.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_keep_code_block_on_one_line, DefaultCodeFormatterConstants.FORMATTER_KEEP_CODE_BLOCK_ON_ONE_LINE)
				.gap()
				.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_keep_method_body_on_one_line, DefaultCodeFormatterConstants.FORMATTER_KEEP_METHOD_BODY_ON_ONE_LINE, pref -> {
					CheckboxPreference getterSetterPref= fTree.addCheckbox(pref, FormatterMessages.FormatterModifyDialog_newLines_pref_keep_simple_getter_setter_on_one_line,
							DefaultCodeFormatterConstants.FORMATTER_KEEP_SIMPLE_GETTER_SETTER_ON_ONE_LINE, CheckboxPreference.FALSE_TRUE);
					pref.addDependant(getterSetterPref, valueAcceptor(oneLineOptions[0], oneLineOptions[1], oneLineOptions[4]));
				})
				.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_keep_type_declaration_on_one_line, DefaultCodeFormatterConstants.FORMATTER_KEEP_TYPE_DECLARATION_ON_ONE_LINE)
				.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_keep_anonymous_type_declaration_on_one_line, DefaultCodeFormatterConstants.FORMATTER_KEEP_ANONYMOUS_TYPE_DECLARATION_ON_ONE_LINE)
				.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_keep_enum_declaration_on_one_line, DefaultCodeFormatterConstants.FORMATTER_KEEP_ENUM_DECLARATION_ON_ONE_LINE)
				.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_keep_enum_constant_declaration_on_one_line, DefaultCodeFormatterConstants.FORMATTER_KEEP_ENUM_CONSTANT_DECLARATION_ON_ONE_LINE)
				.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_keep_record_declaration_on_one_line, DefaultCodeFormatterConstants.FORMATTER_KEEP_RECORD_DECLARATION_ON_ONE_LINE)
				.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_keep_record_constructor_declaration_on_one_line, DefaultCodeFormatterConstants.FORMATTER_KEEP_RECORD_CONSTRUCTOR_ON_ONE_LINE)
				.pref(FormatterMessages.FormatterModifyDialog_newLines_pref_keep_annotation_declaration_on_one_line, DefaultCodeFormatterConstants.FORMATTER_KEEP_ANNOTATION_DECLARATION_ON_ONE_LINE);

		return fTree.new SimpleTreeBuilder<>(null, null, null) {
			@Override
			protected PreferenceTreeNode<?> build(Section parent, PreferenceBuilder ignored) {
				return sectionBuilder.build(parent, prefBuilder);
			}
		};
	}

	private void createLineWrapTree() {
		final Section globalSection= fTree.addSection(null, FormatterMessages.FormatterModifyDialog_lineWrap_tree_line_wrapping, "section-linewrap"); //$NON-NLS-1$
		fTree.addNumberPref(globalSection,
				FormatterMessages.FormatterModifyDialog_lineWrap_pref_max_line_width, DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, 0, 9999);
		fTree.addNumberPref(globalSection,
				FormatterMessages.FormatterModifyDialog_lineWrap_pref_default_indent_wrapped, DefaultCodeFormatterConstants.FORMATTER_CONTINUATION_INDENTATION, 0, 9999);
		fTree.addNumberPref(globalSection,
				FormatterMessages.FormatterModifyDialog_lineWrap_pref_default_indent_array, DefaultCodeFormatterConstants.FORMATTER_CONTINUATION_INDENTATION_FOR_ARRAY_INITIALIZER, 0, 9999);
		fTree.addCheckbox(globalSection,
				FormatterMessages.FormatterModifyDialog_lineWrap_pref_never_join_lines, DefaultCodeFormatterConstants.FORMATTER_JOIN_WRAPPED_LINES, CheckboxPreference.TRUE_FALSE);
		fTree.addCheckbox(globalSection,
				FormatterMessages.FormatterModifyDialog_lineWrap_pref_wrap_outer_expressions_when_nested, DefaultCodeFormatterConstants.FORMATTER_WRAP_OUTER_EXPRESSIONS_WHEN_NESTED, CheckboxPreference.FALSE_TRUE);

		Consumer<Section> modAll= s -> LineWrapPreference.addModifyAll(s, true, fImages);
		fTree.builder(FormatterMessages.FormatterModifyDialog_lineWrap_tree_wrapping_settings, null, modAll)
				.node(fTree.builder(FormatterMessages.FormatterModifyDialog_lineWrap_tree_class_decls, null, modAll)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_extends_clause, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERCLASS_IN_TYPE_DECLARATION)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_implements_clause, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERINTERFACES_IN_TYPE_DECLARATION))
				.node(fTree.builder(FormatterMessages.FormatterModifyDialog_lineWrap_tree_constructor_decls, null, modAll)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_parameters, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_CONSTRUCTOR_DECLARATION)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_throws_clause, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_THROWS_CLAUSE_IN_CONSTRUCTOR_DECLARATION))
				.node(fTree.builder(FormatterMessages.FormatterModifyDialog_lineWrap_tree_method_decls, null, modAll)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_declaration, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_METHOD_DECLARATION)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_parameters, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_METHOD_DECLARATION)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_throws_clause, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_THROWS_CLAUSE_IN_METHOD_DECLARATION))
				.node(fTree.builder(FormatterMessages.FormatterModifyDialog_lineWrap_tree_enum_decls, null, modAll)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_constants, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ENUM_CONSTANTS)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_superinterfaces, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERINTERFACES_IN_ENUM_DECLARATION)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_constant_arguments, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ENUM_CONSTANT))
				.node(fTree.builder(FormatterMessages.FormatterModifyDialog_lineWrap_tree_record_decls, null, modAll)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_record_components, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_RECORD_COMPONENTS)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_superinterfaces, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERINTERFACES_IN_RECORD_DECLARATION))
				.node(fTree.builder(FormatterMessages.FormatterModifyDialog_lineWrap_tree_function_calls, null, modAll)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_arguments, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_METHOD_INVOCATION)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_qualified_invocations, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SELECTOR_IN_METHOD_INVOCATION, p -> {
							CheckboxPreference child= fTree.addCheckbox(p, FormatterMessages.FormatterModifyDialog_lineWrap_pref_qualified_invocations_indent_from_base_expression_first_line,
									DefaultCodeFormatterConstants.FORMATTER_ALIGN_SELECTOR_IN_METHOD_INVOCATION_ON_EXPRESSION_FIRST_LINE, CheckboxPreference.FALSE_TRUE);
							p.addDependant(child, v -> DefaultCodeFormatterConstants.getWrappingStyle(v) != DefaultCodeFormatterConstants.WRAP_NO_SPLIT
									&& DefaultCodeFormatterConstants.getIndentStyle(v) != DefaultCodeFormatterConstants.INDENT_ON_COLUMN);
						})
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_explicit_constructor_invocations, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_EXPLICIT_CONSTRUCTOR_CALL)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_object_allocation_arguments, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ALLOCATION_EXPRESSION)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_qualified_object_allocation_arguments, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_QUALIFIED_ALLOCATION_EXPRESSION))
				.node(fTree.builder(FormatterMessages.FormatterModifyDialog_lineWrap_tree_binary_expressions, "-binary-expressions", modAll) //$NON-NLS-1$
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_multiplicative_operators, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_MULTIPLICATIVE_OPERATOR)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_additive_operators, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ADDITIVE_OPERATOR)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_string_concatenation, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_STRING_CONCATENATION)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_shift_operators, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SHIFT_OPERATOR)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_relational_operators, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_RELATIONAL_OPERATOR)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_bitwise_operators, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_BITWISE_OPERATOR)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_logical_operators, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_LOGICAL_OPERATOR))
				.node(fTree.builder(FormatterMessages.FormatterModifyDialog_lineWrap_tree_expressions, null, modAll)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_conditionals, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_CONDITIONAL_EXPRESSION)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_chained_conditionals, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_CONDITIONAL_EXPRESSION_CHAIN)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_assignments, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ASSIGNMENT)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_array_init, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_EXPRESSIONS_IN_ARRAY_INITIALIZER))
				.node(fTree.builder(FormatterMessages.FormatterModifyDialog_lineWrap_tree_statements, null, modAll)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_for, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_EXPRESSIONS_IN_FOR_LOOP_HEADER)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_compact_if_else, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_COMPACT_IF)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_compact_loops, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_COMPACT_LOOP)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_try, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_RESOURCES_IN_TRY)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_catch, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_UNION_TYPE_IN_MULTICATCH)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_switch_case_with_arrow, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SWITCH_CASE_WITH_ARROW)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_expressions_in_switch_case_with_arrow, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_EXPRESSIONS_IN_SWITCH_CASE_WITH_ARROW)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_expressions_in_switch_case_with_colon, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_EXPRESSIONS_IN_SWITCH_CASE_WITH_COLON)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_assertion_message, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ASSERTION_MESSAGE))
				.node(fTree.builder(FormatterMessages.FormatterModifyDialog_lineWrap_tree_parameterized_types, null, modAll)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_param_type_ref, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERIZED_TYPE_REFERENCES)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_param_type_arguments, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_TYPE_ARGUMENTS)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_param_type_parameters, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_TYPE_PARAMETERS))
				.node(fTree.builder(FormatterMessages.FormatterModifyDialog_lineWrap_tree_annotations, "-annotations", modAll.andThen(s -> LineWrapPreference.addModifyAll(s, false, fImages))) //$NON-NLS-1$
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_annotations_packages, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_PACKAGE)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_annotations_types, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_TYPE)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_annotations_enum_constants, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_ENUM_CONSTANT)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_annotations_fields, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_FIELD)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_annotations_methods, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_METHOD)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_annotations_local_variables, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_LOCAL_VARIABLE)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_annotations_parameters, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_PARAMETER)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_type_annotations, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_TYPE_ANNOTATIONS)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_annotations_arguments, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ANNOTATION))
				.node(fTree.builder(FormatterMessages.FormatterModifyDialog_lineWrap_tree_module_descriptions, null)
						.pref(FormatterMessages.FormatterModifyDialog_lineWrap_pref_module_statements, DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_MODULE_STATEMENTS))
				.build(globalSection, (parent, label, key) -> {
					String wrapBeforeKey= null;
					switch (key) {
						case DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_CONDITIONAL_EXPRESSION:
							wrapBeforeKey= DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_CONDITIONAL_OPERATOR;
							break;
						case DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_MULTIPLICATIVE_OPERATOR:
							wrapBeforeKey= DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_MULTIPLICATIVE_OPERATOR;
							break;
						case DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ADDITIVE_OPERATOR:
							wrapBeforeKey= DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_ADDITIVE_OPERATOR;
							break;
						case DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_STRING_CONCATENATION:
							wrapBeforeKey= DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_STRING_CONCATENATION;
							break;
						case DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SHIFT_OPERATOR:
							wrapBeforeKey= DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_SHIFT_OPERATOR;
							break;
						case DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_RELATIONAL_OPERATOR:
							wrapBeforeKey= DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_RELATIONAL_OPERATOR;
							break;
						case DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_BITWISE_OPERATOR:
							wrapBeforeKey= DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_BITWISE_OPERATOR;
							break;
						case DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_LOGICAL_OPERATOR:
							wrapBeforeKey= DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_LOGICAL_OPERATOR;
							break;
						case DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_UNION_TYPE_IN_MULTICATCH:
							wrapBeforeKey= DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_OR_OPERATOR_MULTICATCH;
							break;
						case DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ASSIGNMENT:
							wrapBeforeKey= DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_ASSIGNMENT_OPERATOR;
							break;
						case DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SWITCH_CASE_WITH_ARROW:
							wrapBeforeKey= DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_SWITCH_CASE_ARROW_OPERATOR;
							break;
						case DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ASSERTION_MESSAGE:
							wrapBeforeKey= DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_ASSERTION_MESSAGE_OPERATOR;
							break;
						default:
					}
					boolean withIndent= !"section-linewrap-annotations".equals(parent.getKey()) || DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ANNOTATION.equals(key); //$NON-NLS-1$
					LineWrapPreference preference= LineWrapPreference.create(parent.fInnerComposite, label, key, wrapBeforeKey, withIndent, fImages);
					return fTree.addChild(parent, preference);
				});
	}

	private void createCommentsTree() {
		Section section= fTree.builder(FormatterMessages.FormatterModifyDialog_comments_tree_comments, "section-comments") //$NON-NLS-1$
				.pref(FormatterMessages.FormatterModifyDialog_comments_pref_line_width, DefaultCodeFormatterConstants.FORMATTER_COMMENT_LINE_LENGTH, pref -> {
					fTree.addCheckbox(pref, FormatterMessages.FormatterModifyDialog_comments_pref_line_width_count_from_starting_position,
							DefaultCodeFormatterConstants.FORMATTER_COMMENT_COUNT_LINE_LENGTH_FROM_STARTING_POSITION, CheckboxPreference.FALSE_TRUE);
				})
				.gap()
				.pref(FormatterMessages.FormatterModifyDialog_comments_pref_enable_javadoc, DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT)
				.pref(FormatterMessages.FormatterModifyDialog_comments_pref_enable_block, DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_BLOCK_COMMENT)
				.pref(FormatterMessages.FormatterModifyDialog_comments_pref_enable_line, DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_LINE_COMMENT, pref -> {
					CheckboxPreference child= fTree.addCheckbox(pref, FormatterMessages.FormatterModifyDialog_comments_pref_format_line_comments_on_first_column,
							DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_LINE_COMMENT_STARTING_ON_FIRST_COLUMN, CheckboxPreference.FALSE_TRUE);
					pref.addDependant(child, valueAcceptor(DefaultCodeFormatterConstants.TRUE));
				})
				.pref(FormatterMessages.FormatterModifyDialog_comments_pref_format_header, DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_HEADER)
				.gap()
				.pref(FormatterMessages.FormatterModifyDialog_comments_pref_preserve_white_space_before_line_comment, DefaultCodeFormatterConstants.FORMATTER_COMMENT_PRESERVE_WHITE_SPACE_BETWEEN_CODE_AND_LINE_COMMENT)
				.pref(FormatterMessages.FormatterModifyDialog_comments_pref_never_indent_line_comments_on_first_column, DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_LINE_COMMENTS_ON_FIRST_COLUMN)
				.pref(FormatterMessages.FormatterModifyDialog_comments_pref_never_indent_block_comments_on_first_column, DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_BLOCK_COMMENTS_ON_FIRST_COLUMN)
				.pref(FormatterMessages.FormatterModifyDialog_comments_pref_never_join_lines, DefaultCodeFormatterConstants.FORMATTER_JOIN_LINES_IN_COMMENTS)
				.node(fTree.builder(FormatterMessages.FormatterModifyDialog_comments_tree_javadocs, "-javadocs") //$NON-NLS-1$
						.pref(FormatterMessages.FormatterModifyDialog_comments_pref_format_html, DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_HTML)
						.pref(FormatterMessages.FormatterModifyDialog_comments_pref_format_code_snippets, DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_SOURCE)
						.pref(FormatterMessages.FormatterModifyDialog_comments_pref_blank_line_before_javadoc_tags, DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BEFORE_ROOT_TAGS)
						.pref(FormatterMessages.FormatterModifyDialog_comments_pref_blank_line_beftween_different_tags, DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BETWEEN_DIFFERENT_TAGS)
						.node(createJavadocAlignOptions())
						.gap()
						.pref(FormatterMessages.FormatterModifyDialog_comments_pref_new_lines_at_javadoc_boundaries, DefaultCodeFormatterConstants.FORMATTER_COMMENT_NEW_LINES_AT_JAVADOC_BOUNDARIES)
						.pref(FormatterMessages.FormatterModifyDialog_comments_pref_remove_blank_lines, DefaultCodeFormatterConstants.FORMATTER_COMMENT_CLEAR_BLANK_LINES_IN_JAVADOC_COMMENT))
				.node(fTree.builder(FormatterMessages.FormatterModifyDialog_comments_tree_block_comments, "-blockcomments") //$NON-NLS-1$
						.pref(FormatterMessages.FormatterModifyDialog_comments_pref_new_lines_at_comment_boundaries, DefaultCodeFormatterConstants.FORMATTER_COMMENT_NEW_LINES_AT_BLOCK_BOUNDARIES)
						.pref(FormatterMessages.FormatterModifyDialog_comments_pref_remove_blank_lines, DefaultCodeFormatterConstants.FORMATTER_COMMENT_CLEAR_BLANK_LINES_IN_BLOCK_COMMENT))
				.build(null, (parent, label, key) -> {
					switch (key) {
						case DefaultCodeFormatterConstants.FORMATTER_COMMENT_LINE_LENGTH:
							return fTree.addNumberPref(parent, label, key, 0, 9999);
						case DefaultCodeFormatterConstants.FORMATTER_JOIN_LINES_IN_COMMENTS:
							return fTree.addCheckbox(parent, label, key, CheckboxPreference.TRUE_FALSE);
						case DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BEFORE_ROOT_TAGS:
						case DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BETWEEN_DIFFERENT_TAGS:
							return fTree.addCheckbox(parent, label, key, CheckboxPreference.DO_NOT_INSERT_INSERT);
						default:
							return fTree.addCheckbox(parent, label, key, CheckboxPreference.FALSE_TRUE);
					}
				});

		Preference<?> javadocMaster= section.findChildPreference(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT);
		Preference<?> blockMaster= section.findChildPreference(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_BLOCK_COMMENT);
		Preference<?> headerMaster= section.findChildPreference(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_HEADER);

		Predicate<String> javadocChecker= v -> DefaultCodeFormatterConstants.TRUE.equals(javadocMaster.getValue()) || DefaultCodeFormatterConstants.TRUE.equals(headerMaster.getValue());
		Predicate<String> blockChecker= v -> DefaultCodeFormatterConstants.TRUE.equals(blockMaster.getValue()) || DefaultCodeFormatterConstants.TRUE.equals(headerMaster.getValue());

		List<PreferenceTreeNode<?>> mainItems= section.getChildren();
		Function<String, Section> sectionFinder= key -> mainItems.stream().filter(n -> n instanceof Section)
				.map(n -> (Section) n).filter(n -> n.getKey().endsWith(key)).findAny().get();
		Section javadocSection= sectionFinder.apply("-javadocs"); //$NON-NLS-1$
		for (PreferenceTreeNode<?> pref : javadocSection.getChildren()) {
			javadocMaster.addDependant(pref, javadocChecker);
			headerMaster.addDependant(pref, javadocChecker);
		}
		Section blockSection= sectionFinder.apply("-blockcomments"); //$NON-NLS-1$
		for (PreferenceTreeNode<?> pref : blockSection.getChildren()) {
			blockMaster.addDependant(pref, blockChecker);
			headerMaster.addDependant(pref, blockChecker);
		}
	}

	private SimpleTreeBuilder<?> createJavadocAlignOptions() {
		String[] items= {
				FormatterMessages.FormatterModifyDialog_comments_pref_javadoc_align_names_and_descriptions,
				FormatterMessages.FormatterModifyDialog_comments_pref_javadoc_align_descriptions_grouped,
				FormatterMessages.FormatterModifyDialog_comments_pref_javadoc_align_descriptions_to_tag,
				FormatterMessages.FormatterModifyDialog_comments_pref_javadoc_align_none,
		};
		List<String> prefs= Arrays.asList(
				DefaultCodeFormatterConstants.FORMATTER_COMMENT_ALIGN_TAGS_NAMES_DESCRIPTIONS,
				DefaultCodeFormatterConstants.FORMATTER_COMMENT_ALIGN_TAGS_DESCREIPTIONS_GROUPED,
				DefaultCodeFormatterConstants.FORMATTER_COMMENT_INDENT_ROOT_TAGS,
				null);

		String[] values= CheckboxPreference.FALSE_TRUE;

		class AlignPreference extends Preference<Combo> {

			AlignPreference(Combo combo, String label) {
				super(combo, label, "", FilteredPreferenceTree.COMBO_VALUE_MATCHER); //$NON-NLS-1$
				combo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						updateValue();
					}
				});
			}

			@Override
			protected void updateWidget() {
				int selected= prefs.indexOf(null);
				for (int i= 0; i < prefs.size(); i++) {
					if (prefs.get(i) != null && values[1].equals(fWorkingValues.get(prefs.get(i))))
						selected= i;
				}
				fControl.select(selected);
			}

			@Override
			protected void updateValue() {
				int selected= fControl.getSelectionIndex();
				for (int i= 0; i < prefs.size(); i++) {
					if (prefs.get(i) != null)
						fWorkingValues.put(prefs.get(i), values[i == selected ? 1 : 0]);
				}
				valuesModified();
			}

			@Override
			protected String getValue() {
				throw new AssertionError("Method not used in this implementation"); //$NON-NLS-1$
			}
		}

		return fTree.new SimpleTreeBuilder<>(null, null, null) {

			@Override
			protected PreferenceTreeNode<?> build(Section parent, PreferenceBuilder prefBuilder) {
				Combo combo= new Combo(parent.fInnerComposite, SWT.SINGLE | SWT.READ_ONLY);
				combo.setItems(items);
				combo.setFont(parent.fInnerComposite.getFont());
				SWTUtil.setDefaultVisibleItemCount(combo);
				combo.setLayoutData(createGridData(1, GridData.HORIZONTAL_ALIGN_FILL, combo.computeSize(SWT.DEFAULT, SWT.DEFAULT).x, 0));

				AlignPreference alignPref= new AlignPreference(combo, FormatterMessages.FormatterModifyDialog_comments_pref_javadoc_align);
				alignPref.addLabel(FormatterMessages.FormatterModifyDialog_comments_pref_javadoc_align, true, fTree.getIndent(parent));
				fTree.addChild(parent, alignPref);

				fTree.addCheckbox(alignPref, FormatterMessages.FormatterModifyDialog_comments_pref_new_line_after_param_tags,
						DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_NEW_LINE_FOR_PARAMETER, CheckboxPreference.DO_NOT_INSERT_INSERT);
				fTree.addCheckbox(alignPref, FormatterMessages.FormatterModifyDialog_comments_pref_indent_description_after_param,
						DefaultCodeFormatterConstants.FORMATTER_COMMENT_INDENT_PARAMETER_DESCRIPTION, CheckboxPreference.FALSE_TRUE);
				fTree.addCheckbox(alignPref, FormatterMessages.FormatterModifyDialog_comments_pref_indent_other_tag_descriptions,
						DefaultCodeFormatterConstants.FORMATTER_COMMENT_INDENT_TAG_DESCRIPTION, CheckboxPreference.FALSE_TRUE);
				return alignPref;
			}
		};
	}

	private void createOffOnTree() {
		Section section= fTree.addSection(null, FormatterMessages.FormatterModifyDialog_offOn_tree_off_on_tags, "section-offon"); //$NON-NLS-1$
		fTree.addGap(section);

		Label description= createLabel(GRID_COLUMNS, section.fInnerComposite, FormatterMessages.FormatterModifyDialog_offOn_description, 0);
		GridData gd= (GridData)description.getLayoutData();
		gd.widthHint= 200;
		gd.horizontalAlignment= SWT.FILL;

		CheckboxPreference checkbox= fTree.addCheckbox(section, FormatterMessages.FormatterModifyDialog_offOn_pref_enable, DefaultCodeFormatterConstants.FORMATTER_USE_ON_OFF_TAGS,
				CheckboxPreference.FALSE_TRUE);

		Predicate<String> validator= value -> {
			String errorText= null;
			if (value != null && !value.isEmpty()) {
				if (Character.isWhitespace(value.charAt(0))) {
					errorText= FormatterMessages.FormatterModifyDialog_offOn_error_startsWithWhitespace;
				} else if (Character.isWhitespace(value.charAt(value.length() - 1))) {
					errorText= FormatterMessages.FormatterModifyDialog_offOn_error_endsWithWhitespace;
				}
			}
			updateStatus(errorText == null ? null : new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, errorText, null));
			return errorText == null;
		};

		StringPreference disableTagPref= fTree.addStringPreference(checkbox, FormatterMessages.FormatterModifyDialog_offOn_pref_off_tag, DefaultCodeFormatterConstants.FORMATTER_DISABLING_TAG);
		StringPreference enableTagPref= fTree.addStringPreference(checkbox, FormatterMessages.FormatterModifyDialog_offOn_pref_on_tag, DefaultCodeFormatterConstants.FORMATTER_ENABLING_TAG);
		disableTagPref.setValueValidator(validator);
		enableTagPref.setValueValidator(validator);

		Predicate<String> checker= valueAcceptor(DefaultCodeFormatterConstants.TRUE);
		checkbox.addDependant(disableTagPref, checker);
		checkbox.addDependant(enableTagPref, checker);

		// for some tree widths, description wrapping seems to cause wrong height calculation
		// and fields may be partially hidden, extra gaps mitigate this
		fTree.addGap(section);
		fTree.addGap(section);
	}

	@Override
	protected Point getInitialSize() {
		try {
			int lastWidth= fDialogSettings.getInt(fKeyPreferredWidth);
			int lastHeight= fDialogSettings.getInt(fKeyPreferredHight);
			return new Point(lastWidth, lastHeight);
		} catch (NumberFormatException ex) {
			// this is the first time
			Point initialSize= super.getInitialSize();
			initialSize.y= 760;
			return initialSize;
		}
	}

	@Override
	protected void updatePreviewCode() {
		if (fDialogSettings.getBoolean(CUSTOM_PREVIEW_TOGGLE_PREFERENCE_KEY)) {
			String previewText= fDialogSettings.get(CUSTOM_PREVIEW_CONTENT_PREFERENCE_KEY);
			if (previewText == null)
				previewText= "// " + FormatterMessages.FormatterModifyDialog_preview_custom_contents_default_comment; //$NON-NLS-1$
			fPreview.setPreviewText(previewText, CodeFormatter.K_UNKNOWN);
		} else {
			super.updatePreviewCode();
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @since 3.5
	 */
	@Override
	protected String getHelpContextId() {
		return IJavaHelpContextIds.CODEFORMATTER_PREFERENCE_PAGE;
	}
}
