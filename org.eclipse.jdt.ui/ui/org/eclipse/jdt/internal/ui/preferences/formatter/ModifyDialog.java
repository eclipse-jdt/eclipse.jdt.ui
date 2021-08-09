/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentType;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Twistie;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.preferences.FilteredPreferenceTree;
import org.eclipse.jdt.internal.ui.preferences.FilteredPreferenceTree.PreferenceTreeNode;
import org.eclipse.jdt.internal.ui.preferences.PreferenceHighlight;
import org.eclipse.jdt.internal.ui.preferences.PreferencesMessages;
import org.eclipse.jdt.internal.ui.preferences.formatter.IModifyDialogTabPage.IModificationListener;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.CustomProfile;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.Profile;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

public abstract class ModifyDialog extends StatusDialog implements IModificationListener {

	protected static class Section extends PreferenceTreeNode<ExpandableComposite> {
		protected final Composite fInnerComposite;

		private final String fPreviewKey;

		private final Control fToggle;

		public static Section create(Composite parentComposite, String label, String previewKey) {
			final ExpandableComposite excomposite= new FormatterPreferenceSectionComposite(parentComposite, SWT.NONE, ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT);
			excomposite.clientVerticalSpacing= 0;
			excomposite.setText(label);
			excomposite.setExpanded(false);
			excomposite.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
			excomposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, GRID_COLUMNS, 1));

			Composite inner= new Composite(excomposite, SWT.NONE);
			inner.setFont(parentComposite.getFont());
			GridLayout layout= new GridLayout(GRID_COLUMNS, false);
			layout.marginWidth= 0;
			layout.marginLeft= 5;
			inner.setLayout(layout);
			excomposite.setClient(inner);
			return new Section(label, excomposite, inner, previewKey);
		}

		private Section(String label, ExpandableComposite control, Composite innerComposite, String previewKey) {
			super(label, control, false);
			fInnerComposite= innerComposite;
			fPreviewKey= previewKey;

			Control toggle= null;
			for (Control child : control.getChildren()) {
				if (child instanceof Twistie)
					toggle= child;
			}
			assert toggle != null;
			fToggle= toggle;

			// clicking inside section should bring it to focus
			final MouseAdapter mouseListener= new MouseAdapter() {
				@Override
				public void mouseDown(MouseEvent e) {
					if (e.getSource() == fControl && e.x < fControl.getClient().getLocation().x)
						return;
					if (e.getSource() == fControl.getClient() && e.x < getChildren().get(0).getControl().getLocation().x)
						return;
					fToggle.setFocus();
				}
			};
			control.addMouseListener(mouseListener);
			control.getClient().addMouseListener(mouseListener);
		}

		public String getKey() {
			return fPreviewKey;
		}

		public Control getToggle() {
			return fToggle;
		}

		public Preference<?> findChildPreference(String key) {
			for (PreferenceTreeNode<?> child : getChildren()) {
				if (child instanceof Preference && key.equals(((Preference<?>) child).getKey()))
					return (Preference<?>) child;
			}
			throw new IllegalArgumentException(key);
		}
	}

	protected abstract static class ModifyAll<T extends Control> {
		private final Section fSection;

		private final Composite fModifyAllPanel;

		protected final T fControl;

		public ModifyAll(Section section, Images images) {
			fSection= section;

			final ExpandableComposite excomposite= section.getControl();
			Composite modifyAllParent= new Composite(excomposite, SWT.NONE);
			excomposite.setTextClient(modifyAllParent);
			RowLayout rowLayout= new RowLayout();
			rowLayout.marginTop= rowLayout.marginBottom= rowLayout.marginRight= 0;
			rowLayout.center= true;
			rowLayout.spacing= 1;
			modifyAllParent.setLayout(rowLayout);

			fModifyAllPanel= new Composite(modifyAllParent, SWT.NONE);
			fModifyAllPanel.setLayout(rowLayout);
			fModifyAllPanel.setVisible(false);
			fControl= createControl(fModifyAllPanel);

			ToolItem item= createToolItem(modifyAllParent, images);
			item.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					fModifyAllPanel.setVisible(!fModifyAllPanel.isVisible());
					if (fModifyAllPanel.isVisible()) {
						excomposite.setFocus(); // force preview update
						prepareControl();
						fControl.requestLayout();
						fControl.setFocus();
					}
				}
			});
			fControl.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					fModifyAllPanel.setVisible(false);
				}
			});
		}

		protected abstract T createControl(Composite parent);

		protected abstract void prepareControl();

		protected <P extends Preference<T>> List<P> findPreferences(Class<P> prefClass) {
			List<P> result= new ArrayList<>();
			ArrayDeque<PreferenceTreeNode<?>> queue= new ArrayDeque<>();
			queue.add(fSection);
			while (!queue.isEmpty()) {
				PreferenceTreeNode<?> node= queue.removeFirst();
				if (!node.isVisible())
					continue;
				if (prefClass.isInstance(node))
					result.add(prefClass.cast(node));
				queue.addAll(node.getChildren());
			}
			return result;
		}

		static ToolItem createToolItem(Composite parent, Images images) {
			ToolBar toolBar= new ToolBar(parent, SWT.FLAT);
			ToolItem item= new ToolItem(toolBar, SWT.PUSH);
			item.setToolTipText(FormatterMessages.ModifyDialog_modifyAll_tooltip);
			item.setImage(images.get(JavaPluginImages.DESC_ELCL_MODIFYALL));
			return item;
		}
	}

	protected abstract static class Preference<T extends Control> extends PreferenceTreeNode<T> {

		private Map<String, String> fWorkingValues;

		private Predicate<String> fValueValidator;
		protected Runnable fValueChangeListener;

		private String fKey;

		private Map<PreferenceTreeNode<?>, Predicate<String>> fDependants= new HashMap<>();

		protected PreferenceHighlight fHighlight;

		public Preference(T control, String label, String key, ValueMatcher<T> valueMatcher) {
			super(label, control, true, valueMatcher);
			assert key != null;
			fKey= key;
		}

		protected void init(Map<String, String> workingValues, Predicate<String> valueValidator, Runnable valueChangeListener) {
			fWorkingValues= workingValues;
			fValueValidator= valueValidator;
			fValueChangeListener= valueChangeListener;
			updateWidget();

			fControl.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					fValueValidator.test(null);
				}
			});
		}

		protected Map<String, String> getPreferences() {
			return fWorkingValues;
		}

		public void setValueValidator(Predicate<String> valueValidator) {
			fValueValidator = valueValidator;
		}

		/**
		 * Set the key which is used to store the value.
		 *
		 * @param key New value
		 */
		public final void setKey(String key) {
			assert key != null;
			fKey= key;
			updateWidget();
		}

		/**
		 * @return Gets the currently used key which is used to store the value.
		 */
		public final String getKey() {
			return fKey;
		}

		public void addDependant(PreferenceTreeNode<?> dependentChild, Predicate<String> dependencyChecker) {
			fDependants.put(dependentChild, dependencyChecker);
			if (fWorkingValues != null)
				updateDependants();
		}

		/**
		 * To be implemented in subclasses. Update the SWT widgets when the state of this object has changed
		 * (enabled, key, ...).
		 */
		protected abstract void updateWidget();

		protected abstract String getValue();

		protected void updateValue() {
			String newValue= getValue();
			String oldValue= fWorkingValues.put(fKey, newValue);
			if (fValueValidator.test(newValue)) {
				updateDependants();
				fValueChangeListener.run();
			} else {
				fWorkingValues.put(fKey, oldValue);
			}
		}

		private void updateDependants() {
			for (Entry<PreferenceTreeNode<?>, Predicate<String>> entry : fDependants.entrySet()) {
				PreferenceTreeNode<?> dependant= entry.getKey();
				Predicate<String> dependencyChecker= entry.getValue();
				dependant.setEnabled(dependencyChecker.test(fWorkingValues.get(fKey)));
			}
		}

		@Override
		public void setEnabled(boolean enabled) {
			super.setEnabled(enabled);
			if (enabled)
				updateDependants();
		}

		protected void addLabel(String label, boolean highlight, int indent) {
			if (label == null)
				return;
			Label labelControl= createLabel(GRID_COLUMNS - 2, fControl.getParent(), label, indent);
			labelControl.moveAbove(fControl);
			if (highlight)
				fHighlight= PreferenceHighlight.addHighlight(labelControl, fControl, false);
			addChild(new PreferenceTreeNode<>(label, labelControl, true));
		}
	}

	/**
	 * Wrapper around a checkbox with a label.
	 */
	protected static final class CheckboxPreference extends Preference<Button> {
		/**
		 * Constant array for boolean false/true selection.
		 */
		public static final String[] FALSE_TRUE= { DefaultCodeFormatterConstants.FALSE, DefaultCodeFormatterConstants.TRUE };

		/**
		 * Constant array for boolean true/false selection.
		 */
		public static final String[] TRUE_FALSE= { DefaultCodeFormatterConstants.TRUE, DefaultCodeFormatterConstants.FALSE };

		/**
		 * Constant array for insert / not_insert.
		 */
		public static final String[] DO_NOT_INSERT_INSERT= { JavaCore.DO_NOT_INSERT, JavaCore.INSERT };

		private final String[] fValues;

		/**
		 * Create a new CheckboxPreference.
		 *
		 * @param parentComposite The composite on which the SWT widgets are added.
		 * @param indent how many levels of indentation to apply.
		 * @param label The label text for this Preference.
		 * @param key The key to store the values.
		 * @param values An array of two elements indicating the values to store when unchecked/checked.
		 * @return a newly created CheckboxPreference.
		 */
		public static CheckboxPreference create(Composite parentComposite, int indent, String label, String key, String[] values) {
			Button checkbox= new Button(parentComposite, SWT.CHECK);
			checkbox.setText(label);
			checkbox.setLayoutData(createGridData(GRID_COLUMNS - 1, GridData.FILL_HORIZONTAL, SWT.DEFAULT, indent));
			checkbox.setFont(parentComposite.getFont());
			return new CheckboxPreference(checkbox, label, key, values);
		}

		private CheckboxPreference(Button button, String label, String key, String[] values) {
			super(button, label, key, FilteredPreferenceTree.CHECK_BOX_MATCHER);
			fValues= values;

			button.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					updateValue();
				}
			});
		}

		@Override
		protected String getValue() {
			boolean state= fControl.getSelection();
			return state ? fValues[1] : fValues[0];
		}

		@Override
		protected void updateWidget() {
			boolean checked= fValues[1].equals(getPreferences().get(getKey()));
			fControl.setSelection(checked);
		}

		public static ModifyAll<Button> addModifyAll(Section section, Images images) {
			return new ModifyAll<Button>(section, images) {
				@Override
				protected Button createControl(Composite parent) {
					Button checkBox= new Button(parent, SWT.CHECK);
					checkBox.setFont(parent.getFont());
					checkBox.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							boolean selected= fControl.getSelection();
							for (CheckboxPreference pref : findPreferences(CheckboxPreference.class)) {
								pref.getControl().setSelection(selected);
								pref.updateValue();
							}
							prepareControl();
						}
					});
					return checkBox;
				}

				@Override
				protected void prepareControl() {
					List<CheckboxPreference> preferences= findPreferences(CheckboxPreference.class);
					int count= 0;
					for (CheckboxPreference pref : preferences) {
						if (pref.getControl().getSelection())
							count++;
					}
					fControl.setSelection(count == preferences.size());
					fControl.setText(Messages.format(FormatterMessages.ModifyDialog_modifyAll_checkBox, new Object[] { count, preferences.size() }));
					fControl.requestLayout();
				}
			};
		}
	}

	/**
	 * Wrapper around a combo box and a label.
	 */
	protected static final class ComboPreference extends Preference<Combo> {
		private final List<String> fValues;

		/**
		 * @param parentComposite The composite on which the SWT widgets are added.
		 * @param indent how many levels of indentation to apply.
		 * @param label The label text for this Preference or {@code null} if none.
		 * @param key The key to store the values.
		 * @param values An array of n elements indicating the values to store for each selection.
		 * @param items An array of n elements indicating the text to be written in the combo box.
		 * @param highlight whether highlight arrow should be added.
		 * @return a newly created ComboPreference.
		 */
		public static ComboPreference create(Composite parentComposite, int indent, String label, String key, String[] values, String[] items, boolean highlight) {
			Combo combo= new Combo(parentComposite, SWT.SINGLE | SWT.READ_ONLY);
			combo.setFont(parentComposite.getFont());
			SWTUtil.setDefaultVisibleItemCount(combo);
			combo.setItems(items);
			combo.setLayoutData(createGridData(1, GridData.HORIZONTAL_ALIGN_FILL, combo.computeSize(SWT.DEFAULT, SWT.DEFAULT).x, 0));

			ComboPreference comboPreference= new ComboPreference(combo, label, key, values);
			comboPreference.addLabel(label, highlight, indent);

			return comboPreference;
		}

		private ComboPreference(Combo combo, String label, String key, String[] values) {
			super(combo, label, key, FilteredPreferenceTree.COMBO_VALUE_MATCHER);

			fValues= Arrays.asList(values);

			combo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					updateValue();
				}
			});
		}

		@Override
		protected void updateWidget() {
			int valueIndex= fValues.indexOf(getPreferences().get(getKey()));
			if (valueIndex == -1) {
				final String message= Messages.format(FormatterMessages.ModifyDialog_ComboPreference_error_invalid_key, new Object[] { getKey(), getPreferences().get(getKey()) });
				JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, message, null));
				valueIndex= 0;
			}
			fControl.select(valueIndex);
		}

		@Override
		protected String getValue() {
			return fValues.get(fControl.getSelectionIndex());
		}

		public static ModifyAll<Combo> addModifyAll(Section section, Images images) {
			return new ModifyAll<Combo>(section, images) {
				ArrayList<String> fItems= new ArrayList<>();

				@Override
				protected Combo createControl(Composite parent) {
					Combo combo= new Combo(parent, SWT.SINGLE | SWT.READ_ONLY);
					combo.setFont(parent.getFont());
					SWTUtil.setDefaultVisibleItemCount(combo);
					combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							String selected= fItems.get(fControl.getSelectionIndex());
							for (ComboPreference pref : findPreferences(ComboPreference.class)) {
								int index= pref.fControl.indexOf(selected);
								if (index >= 0)
									pref.fControl.select(index);
								pref.updateValue();
							}
							prepareControl();
						}
					});
					return combo;
				}

				@Override
				protected void prepareControl() {
					LinkedHashMap<String, Integer> itemCounts= new LinkedHashMap<>();
					List<ComboPreference> preferences= findPreferences(ComboPreference.class);
					for (ComboPreference pref : preferences) {
						String[] items= pref.getControl().getItems();
						for (String item : items) {
							if (!itemCounts.containsKey(item))
								itemCounts.put(item, 0);
						}
						String selected= items[pref.getControl().getSelectionIndex()];
						itemCounts.put(selected, itemCounts.get(selected) + 1);
					}

					String[] items= new String[itemCounts.size()];
					fItems.clear();
					int i= 0;
					int maxCount= 0;
					int maxCountIndex= 0;
					for (Entry<String, Integer> entry : itemCounts.entrySet()) {
						String item= entry.getKey();
						int count= entry.getValue();
						fItems.add(item);
						if (count > 0) {
							item+= Messages.format(FormatterMessages.ModifyDialog_modifyAll_summary, new Object[] { count, preferences.size() });
						}
						if (count > maxCount) {
							maxCount= count;
							maxCountIndex= i;
						}
						items[i++]= item;
					}
					fControl.setItems(items);
					fControl.select(maxCountIndex);
					fControl.requestLayout();
				}
			};
		}
	}

	/**
	 * Wrapper around a textfied which requests an integer input of a given range.
	 */
	protected static final class NumberPreference extends Preference<Spinner> {

		/**
		 * @param parentComposite The composite on which the SWT widgets are added.
		 * @param indent how many levels of indentation to apply.
		 * @param label The label text for this Preference or {@code null} if none.
		 * @param key The key to store the values.
		 * @param minValue The minimum value which is valid input.
		 * @param maxValue The maximum value which is valid input.
		 * @param highlight whether highlight arrow should be added.
		 * @return a newly created NumberPreference.
		 */
		public static NumberPreference create(Composite parentComposite, int indent, String label, String key, int minValue, int maxValue, boolean highlight) {
			Spinner spinner= createSpinner(parentComposite, minValue, maxValue);
			NumberPreference numberPreference= new NumberPreference(spinner, label, key);
			numberPreference.addLabel(label, highlight, indent);

			return numberPreference;
		}

		static Spinner createSpinner(Composite parentComposite, int minValue, int maxValue) {
			Spinner spinner= new Spinner(parentComposite, SWT.BORDER);
			spinner.setFont(parentComposite.getFont());
			spinner.setMinimum(minValue);
			spinner.setMaximum(maxValue);

			spinner.setLayoutData(createGridData(1, GridData.HORIZONTAL_ALIGN_END, SWT.DEFAULT, 0));
			return spinner;
		}

		private NumberPreference(Spinner spinner, String label, String key) {
			super(spinner, label, key, FilteredPreferenceTree.SPINNER_VALUE_MATCHER);

			spinner.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					updateValue();
				}
			});
		}

		@Override
		protected void updateWidget() {
			try {
				String s= getPreferences().get(getKey());
				int number= Integer.parseInt(s);
				number= Math.max(fControl.getMinimum(), Math.min(fControl.getMaximum(), number));
				fControl.setSelection(number);
			} catch (NumberFormatException x) {
				final String message= Messages.format(FormatterMessages.ModifyDialogTabPage_NumberPreference_error_invalid_key, getKey());
				JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, message, null));
				fControl.setSelection(fControl.getMinimum());
			}
		}

		@Override
		protected String getValue() {
			return Integer.toString(fControl.getSelection());
		}

		public static ModifyAll<Spinner> addModifyAll(final int minValue, final int maxValue, Section section, Images images) {
			return new ModifyAll<Spinner>(section, images) {
				private Label fLabel;

				@Override
				protected Spinner createControl(Composite parent) {
					GridLayout layout= new GridLayout(2, false);
					layout.marginWidth= layout.marginHeight= 0;
					parent.setLayout(layout);

					fLabel= createLabel(1, parent, "", 0); //$NON-NLS-1$
					Spinner spinner= createSpinner(parent, minValue, maxValue);
					spinner.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							int selected= fControl.getSelection();
							for (NumberPreference pref : findPreferences(NumberPreference.class)) {
								pref.getControl().setSelection(selected);
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
					List<NumberPreference> preferences= findPreferences(NumberPreference.class);
					for (NumberPreference pref : preferences) {
						int value= pref.getControl().getSelection();
						int count= counts.merge(value, 1, Integer::sum);
						if (count > modeCount) {
							modeValue= value;
							modeCount= count;
						}
					}
					fControl.setSelection(modeValue);
					fLabel.setText(Messages.format(FormatterMessages.ModifyDialog_modifyAll_summary, new Object[] { modeCount, preferences.size() }));
					fLabel.requestLayout();
				}
			};
		}
	}

	/**
	 * Wrapper around a text field which requests a string input.
	 */
	protected static final class StringPreference extends Preference<Text> {

		/**
		 * @param parentComposite The composite on which the SWT widgets are added.
		 * @param indent how many levels of indentation to apply.
		 * @param label the label text for this Preference.
		 * @param key the key to store the values.
		 * @param highlight whether highlight arrow should be added
		 * @return a newly created StringPreference
		 */
		public static StringPreference create(Composite parentComposite, int indent, String label, String key, boolean highlight) {
			Text text= new Text(parentComposite, SWT.SINGLE | SWT.BORDER);
			text.setFont(parentComposite.getFont());
			final int length= 30;
			PixelConverter pixelConverter= new PixelConverter(parentComposite);
			GridData gridData= createGridData(1, GridData.HORIZONTAL_ALIGN_BEGINNING, pixelConverter.convertWidthInCharsToPixels(length), 0);
			gridData.grabExcessHorizontalSpace= true;
			text.setLayoutData(gridData);

			StringPreference stringPreference= new StringPreference(text, label, key);
			stringPreference.addLabel(label, highlight, indent);

			return stringPreference;
		}

		private StringPreference(Text control, String label, String key) {
			super(control, label, key, FilteredPreferenceTree.TEXT_VALUE_MATCHER);

			fControl.addModifyListener(e -> updateValue());

			fControl.addFocusListener(new FocusAdapter() {
				@Override
				public void focusGained(FocusEvent e) {
					fControl.setSelection(0, fControl.getCharCount());
				}
			});
		}

		@Override
		protected void updateWidget() {
			String value= getPreferences().get(getKey());
			fControl.setText(value);
		}

		@Override
		protected String getValue() {
			return fControl.getText();
		}
	}

	@FunctionalInterface
	public interface PreferenceBuilder {
		Preference<?> buildPreference(Section parent, String label, String key);
	}

	protected class ProfilePreferenceTree extends FilteredPreferenceTree {

		/**
		 * Helper class for easy, call-chain based building of subtrees that contain only Sections and one
		 * type of preference.
		 *
		 * @param <T> Type of preference or section built
		 */
		public abstract class SimpleTreeBuilder<T extends PreferenceTreeNode<?>> {
			protected final String fLabel, fKey;

			protected boolean fGap;

			protected Consumer<T> fCustomizer;

			protected SimpleTreeBuilder(String label, String key, Consumer<T> customizer) {
				fLabel= label;
				fKey= key;
				fCustomizer= customizer;
			}

			protected abstract T build(Section parent, PreferenceBuilder prefBuilder);
		}

		public class SectionBuilder extends SimpleTreeBuilder<Section> {
			private ArrayList<SimpleTreeBuilder<?>> fChildren= new ArrayList<>();

			SectionBuilder(String label, String key, Consumer<Section> customizer) {
				super(label, key, customizer);
			}

			public SectionBuilder node(SimpleTreeBuilder<?> child) {
				fChildren.add(child);
				return this;
			}

			public SectionBuilder pref(String label, String key) {
				return pref(label, key, null);
			}

			public SectionBuilder pref(String label, String key, Consumer<Preference<?>> customizer) {
				return node(new PrefBuilder(label, key, customizer));
			}

			public SectionBuilder gap() {
				fChildren.get(fChildren.size() - 1).fGap= true;
				return this;
			}

			@Override
			public Section build(Section parent, PreferenceBuilder prefBuilder) {
				String key= fKey;
				if (key != null) {
					Section ancestorWithKey= parent;
					while (ancestorWithKey != null && ancestorWithKey.getKey() == null)
						ancestorWithKey= (Section) ancestorWithKey.getParent();
					if (ancestorWithKey != null)
						key= ancestorWithKey.getKey() + key;
				}

				Section section= addSection(parent, fLabel, key);
				for (SimpleTreeBuilder<?> child : fChildren) {
					child.build(section, prefBuilder);
					if (child.fGap)
						addGap(section);
				}
				if (fCustomizer != null)
					fCustomizer.accept(section);
				return section;
			}
		}

		private class PrefBuilder extends SimpleTreeBuilder<Preference<?>> {
			public PrefBuilder(String label, String key, Consumer<Preference<?>> customizer) {
				super(label, key, customizer);
			}

			@Override
			protected Preference<?> build(Section parent, PreferenceBuilder prefBuilder) {
				Preference<?> pref= prefBuilder.buildPreference(parent, fLabel, fKey);
				if (fCustomizer != null)
					fCustomizer.accept(pref);
				return pref;
			}
		}

		private boolean fFilterEmpty;
		private int fRightMargin;

		public ProfilePreferenceTree(Composite parentComposite) {
			super(parentComposite, FormatterMessages.ModifyDialog_filter_label, FormatterMessages.ModifyDialog_filter_hint);

			// calculate rigth margin width
			ToolBar modifyAllToolbar= ModifyAll.createToolItem(parentComposite, fImages).getParent();
			fRightMargin= modifyAllToolbar.computeSize(SWT.DEFAULT, SWT.DEFAULT).x - 12;
			modifyAllToolbar.dispose();
		}

		public Section addSection(Section parent, String label, String previewKey) {
			Section section= Section.create(getParentComposite(parent), label, previewKey);

			ExpandableComposite excomposite= section.getControl();
			getScrolledPageContent().adaptChild(excomposite);

			Menu expandAllMenu= new Menu(excomposite);
			MenuItem expandAllItem= new MenuItem(expandAllMenu, SWT.NONE);
			expandAllItem.setText(PreferencesMessages.FilteredPreferencesTree_expandAll_tooltip);
			expandAllItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					setAllExpanded(section, true);
				}
			});
			excomposite.setMenu(expandAllMenu);

			return addChild(parent, section);
		}

		public CheckboxPreference addCheckbox(PreferenceTreeNode<?> parent, String label, String key, String[] values) {
			CheckboxPreference button= CheckboxPreference.create(getParentComposite(parent), getIndent(parent), label, key, values);
			return addChild(parent, button);
		}

		public ComboPreference addComboPref(PreferenceTreeNode<?> parent, String label, String key, String[] values, String[] items) {
			ComboPreference combo= ComboPreference.create(getParentComposite(parent), getIndent(parent), label, key, values, items, true);
			return addChild(parent, combo);
		}

		public NumberPreference addNumberPref(PreferenceTreeNode<?> parent, String label, String key, int minValue, int maxValue) {
			NumberPreference number= NumberPreference.create(getParentComposite(parent), getIndent(parent), label, key, minValue, maxValue, true);
			return addChild(parent, number);
		}

		public StringPreference addStringPreference(PreferenceTreeNode<?> parent, String label, String key) {
			StringPreference stringPreference= StringPreference.create(getParentComposite(parent), getIndent(parent), label, key, true);
			return addChild(parent, stringPreference);
		}

		@Override
		public <T extends PreferenceTreeNode<?>> T addChild(PreferenceTreeNode<?> parent, T node) {
			super.addChild(parent, node);
			fFocusManager.add(node);
			if (node instanceof Preference<?>) {
				Predicate<String> validator = v -> {
					doValidate();
					return true;
				};
				((Preference<?>) node).init(fWorkingValues, validator, ModifyDialog.this::valuesModified);
			}

			if (!(node instanceof Section)) {
				Label margin= new Label(node.getControl().getParent(), SWT.NONE);
				margin.setLayoutData(new GridData(fRightMargin, SWT.DEFAULT));
				node.addChild(new PreferenceTreeNode<>("", margin, true)); //$NON-NLS-1$
			}
			return node;
		}

		public void addGap(PreferenceTreeNode<?> parent) {
			Composite gap= new Composite(getParentComposite(parent), SWT.NONE);
			GridData gd= new GridData(0, 4);
			gd.horizontalSpan= GRID_COLUMNS;
			gap.setLayoutData(gd);
			parent.addChild(new PreferenceTreeNode<>("", gap, true)); //$NON-NLS-1$
		}

		public SectionBuilder builder(String label, String key) {
			return builder(label, key, null);
		}

		public SectionBuilder builder(String label, String key, Consumer<Section> customizer) {
			return new SectionBuilder(label, key, customizer);
		}

		private Composite getParentComposite(PreferenceTreeNode<?> parent) {
			while (parent != null) {
				if (parent instanceof Section)
					return ((Section) parent).fInnerComposite;
				parent= parent.getParent();
			}
			return getScrolledPageContent().getBody();
		}

		protected int getIndent(PreferenceTreeNode<?> parent) {
			int indent= 0;
			while (parent != null) {
				if (parent instanceof Section)
					return indent;
				indent++;
				parent= parent.getParent();
			}
			return indent;
		}

		protected void saveState() {
			try (ByteArrayOutputStream out= new ByteArrayOutputStream()) {
				writeExpansionState(fRoot, out);
				fDialogSettings.put(fKeyPreferenceTreeExpansion, out.toString("UTF-8")); //$NON-NLS-1$
			} catch (IOException e) {
				throw new AssertionError(e);
			}

			int scrollPosition= getScrolledPageContent().getOrigin().y;
			fDialogSettings.put(fKeyPreferenceScrollPosition, scrollPosition);
		}

		private void writeExpansionState(PreferenceTreeNode<?> node, OutputStream output) throws IOException {
			for (PreferenceTreeNode<?> child : node.getChildren()) {
				if (child instanceof Section) {
					output.write(((Section) child).getControl().isExpanded() ? '1' : '0');
					writeExpansionState(child, output);
				}
			}
			output.write('.');
		}

		protected void restoreExpansionState() {
			String treeState= fDialogSettings.get(fKeyPreferenceTreeExpansion);
			if (treeState == null)
				return;
			try (ByteArrayInputStream in= new ByteArrayInputStream(treeState.getBytes("UTF-8"))) { //$NON-NLS-1$
				fScrolledPageContent.setReflow(false);
				readExpansionState(fRoot, in);
				fScrolledPageContent.setReflow(true);
			} catch (IOException e) {
				throw new AssertionError(e);
			}
		}

		private void readExpansionState(PreferenceTreeNode<?> node, InputStream in) throws IOException {
			for (PreferenceTreeNode<?> child : node.getChildren()) {
				if (child instanceof Section) {
					int state= in.read();
					((Section) child).getControl().setExpanded(state == '1');
					readExpansionState(child, in);
					if (state == '.' || state == -1)
						return; // some nodes missing in stored tree
				}
			}
			int c= in.read();
			while (c != '.' && c != -1) { // some extra nodes in stored tree
				readExpansionState(new PreferenceTreeNode<>(null, null, false), in);
				c= in.read();
			}
		}

		protected void restoreScrollPosition() {
			try {
				int scrollPosition= fDialogSettings.getInt(fKeyPreferenceScrollPosition);
				getScrolledPageContent().setOrigin(0, scrollPosition);
			} catch (NumberFormatException e) {
				// ignore invalid/undefined value
			}
		}

		@Override
		public void doFilter(String filterText) {
			fFilterEmpty= filterText.trim().isEmpty();
			super.doFilter(filterText);
		}

		@Override
		protected void updateUI(PreferenceTreeNode<?> node) {
			super.updateUI(node);
			if (node == fRoot && fFilterEmpty)
				restoreExpansionState();
		}

		public void unifySectionTitlesHeights(Section section) {
			List<PreferenceTreeNode<?>> children= section == null ? fRoot.getChildren() : section.getChildren();
			int maxHeightDiff= 0;
			for (PreferenceTreeNode<?> child : children) {
				if (child instanceof Section)
					maxHeightDiff= Math.max(maxHeightDiff, ((Section) child).getControl().getTextClientHeightDifference());
			}
			int nextChildIndent= 0;
			for (PreferenceTreeNode<?> child : children) {
				int indent= nextChildIndent;
				nextChildIndent= 0;
				if (child instanceof Section) {
					Section sectionChild= (Section) child;
					int diff= maxHeightDiff - sectionChild.getControl().getTextClientHeightDifference();
					nextChildIndent= diff / 2;
					indent+= diff - diff / 2;

					unifySectionTitlesHeights(sectionChild);
				}
				GridData gd= (GridData) child.getControl().getLayoutData();
				gd.verticalIndent+= indent;
			}
		}
	}

	public static class Images {
		private Map<ImageDescriptor, Image> imagesMap= new HashMap<>();

		protected Images(Composite rootComposite) {
			rootComposite.addDisposeListener(e -> {
				for (Image image : imagesMap.values())
					image.dispose();
				imagesMap.clear();
			});
		}

		public Image get(ImageDescriptor descriptor) {
			return imagesMap.computeIfAbsent(descriptor, ImageDescriptor::createImage);
		}
	}

	/**
	 * The default focus manager. It knows all widgets which can have the focus and listens for
	 * focusGained events, on which it stores the index of the current focus holder. When the dialog
	 * is restarted, <code>restoreFocus()</code> sets the focus to the last control which had it.
	 *
	 * Focus manager also makes sure that proper preview is displayed for currently focused
	 * preference.
	 *
	 * The standard Preference objects are managed by this focus manager if they are created using
	 * the respective factory methods. Other SWT widgets can be added in subclasses when they are
	 * created.
	 */
	protected final class FocusManager implements FocusListener {
		private HashMap<Control, PreferenceTreeNode<?>> fControl2node= new HashMap<>();
		private final Map<Control, Integer> fItemMap= new HashMap<>();
		private final List<Control> fItemList= new ArrayList<>();
		private int fIndex= 0;
		private PreferenceTreeNode<?> fCurrentlyFocused;

		public void add(PreferenceTreeNode<?> node) {
			Control control= node.getControl();
			if (node instanceof Section) {
				// workaround: can't add focus listener directly to ExpadableComposite
				control= ((Section) node).getToggle();
			}
			fControl2node.put(control, node);
			add(control);
		}

		private void add(Control control) {
			control.addFocusListener(this);
			fItemList.add(fIndex, control);
			fItemMap.put(control, fIndex++);
		}

		@Override
		public void focusGained(FocusEvent e) {
			PreferenceTreeNode<?> focusNode= fControl2node.get(e.getSource());
			if (focusNode != null && focusNode != fCurrentlyFocused) {
				highlightCurrent(false);
				fCurrentlyFocused= focusNode;
				highlightCurrent(true);
				updatePreviewCode();
			}

			fDialogSettings.put(fKeyLastFocusIndex, fItemMap.get(e.widget));
		}

		private void highlightCurrent(boolean focus) {
			if (fCurrentlyFocused instanceof Preference) {
				Preference<?> pref= (Preference<?>) fCurrentlyFocused;
				if (pref.fHighlight != null) {
					pref.fHighlight.setFocus(focus);
				} else {
					changeFont(pref.getControl(), focus);
				}
			} else if (fCurrentlyFocused instanceof Section) {
				changeFont(((Section) fCurrentlyFocused).getControl(), focus);
			}
		}

		private void changeFont(Control control, boolean italic) {
			Display.getCurrent().asyncExec(() -> {
				FontData fd= control.getFont().getFontData()[0];
				int style= italic ? (fd.getStyle() | SWT.ITALIC) : (fd.getStyle() & ~SWT.ITALIC);
				FontData fontData= new FontData(fd.getName(), fd.getHeight(), style);
				Font font= new Font(control.getDisplay(), fontData);
				control.addDisposeListener(e -> font.dispose());
				control.setFont(font);
				if (control instanceof Composite)
					((Composite) control).layout();
			});
		}

		@Override
		public void focusLost(FocusEvent e) {
			doValidate();
		}

		public void restoreFocus() {
			try {
				int index= fDialogSettings.getInt(fKeyLastFocusIndex);
				if (index >= 0 && index < fItemList.size()) {
					fItemList.get(index).forceFocus();
				}
			} catch (NumberFormatException ex) {
				// this is the first time
				updatePreviewCode();
			}
		}
	}

	protected static final int GRID_COLUMNS= 4;

	/* The keys to retrieve the preferred area from the dialog settings */
	private static final String DS_KEY_PREFERRED_WIDTH= "modify_dialog.preferred_width"; //$NON-NLS-1$
	private static final String DS_KEY_PREFERRED_HEIGHT= "modify_dialog.preferred_height"; //$NON-NLS-1$
	private static final String DS_KEY_PREFERRED_X= "modify_dialog.preferred_x"; //$NON-NLS-1$
	private static final String DS_KEY_PREFERRED_Y= "modify_dialog.preferred_y"; //$NON-NLS-1$
	private static final String DS_KEY_SASH_FORM_LEFT_WIDTH= "modify_dialog.sash_form_left_width"; //$NON-NLS-1$
	private static final String DS_KEY_SASH_FORM_RIGHT_WIDTH= "modify_dialog.sash_form_rigth_width"; //$NON-NLS-1$

	private static final String DS_KEY_PREFERENCE_TREE_EXPANSION= ".preference_tree_expansion"; //$NON-NLS-1$
	private static final String DS_KEY_PREFERENCE_SCROLL_POSITION= ".preference_scroll_position"; //$NON-NLS-1$
	private static final String DS_KEY_LAST_FOCUS_INDEX= ".last_focus_index"; //$NON-NLS-1$

	private static final int APPLAY_BUTTON_ID= IDialogConstants.CLIENT_ID;
	private static final int SAVE_BUTTON_ID= IDialogConstants.CLIENT_ID + 1;

	protected final Map<String, String> fWorkingValues;
	protected final IDialogSettings fDialogSettings;
	protected final boolean fNewProfile;

	protected final String fKeyPreferredWidth;
	protected final String fKeyPreferredHight;
	private final String fKeyPreferredX;
	private final String fKeyPreferredY;
	private final String fKeySashFormLeftWidth;
	private final String fKeySashFormRightWidth;
	protected final String fKeyPreferenceTreeExpansion;
	protected final String fKeyPreferenceScrollPosition;
	protected final String fKeyLastFocusIndex;
	private final String fLastSaveLoadPathKey;
	private final ProfileStore fProfileStore;
	private Profile fProfile;
	private final ProfileManager fProfileManager;
	private Button fApplyButton;
	private Button fSaveButton;
	private StringDialogField fProfileNameField;

	private SashForm fSashForm;
	protected JavaPreview fPreview;
	protected ProfilePreferenceTree fTree;
	protected final Images fImages;
	protected final FocusManager fFocusManager= new FocusManager();
	private String fPreviewSources= ""; //$NON-NLS-1$
	private int fCurrentPreviewType;

	public ModifyDialog(Shell parentShell, Profile profile, ProfileManager profileManager, ProfileStore profileStore, boolean newProfile, String dialogPreferencesKey, String lastSavePathKey) {
		super(parentShell);

		fProfileStore= profileStore;
		fLastSaveLoadPathKey= lastSavePathKey;

		fKeyPreferredWidth= JavaUI.ID_PLUGIN + dialogPreferencesKey + DS_KEY_PREFERRED_WIDTH;
		fKeyPreferredHight= JavaUI.ID_PLUGIN + dialogPreferencesKey + DS_KEY_PREFERRED_HEIGHT;
		fKeyPreferredX= JavaUI.ID_PLUGIN + dialogPreferencesKey + DS_KEY_PREFERRED_X;
		fKeyPreferredY= JavaUI.ID_PLUGIN + dialogPreferencesKey + DS_KEY_PREFERRED_Y;
		fKeySashFormLeftWidth= JavaUI.ID_PLUGIN + dialogPreferencesKey + DS_KEY_SASH_FORM_LEFT_WIDTH;
		fKeySashFormRightWidth= JavaUI.ID_PLUGIN + dialogPreferencesKey + DS_KEY_SASH_FORM_RIGHT_WIDTH;
		fKeyPreferenceTreeExpansion= JavaUI.ID_PLUGIN + dialogPreferencesKey + DS_KEY_PREFERENCE_TREE_EXPANSION;
		fKeyPreferenceScrollPosition= JavaUI.ID_PLUGIN + dialogPreferencesKey + DS_KEY_PREFERENCE_SCROLL_POSITION;
		fKeyLastFocusIndex= JavaUI.ID_PLUGIN + dialogPreferencesKey + DS_KEY_LAST_FOCUS_INDEX;

		fProfileManager= profileManager;
		fNewProfile= newProfile;

		fProfile= profile;
		setTitle(Messages.format(FormatterMessages.ModifyDialog_dialog_title, profile.getName()));
		fWorkingValues= new HashMap<>(fProfile.getSettings());
		setStatusLineAboveButtons(false);
		fDialogSettings= JavaPlugin.getDefault().getDialogSettings();

		fImages= new Images(parentShell);
	}

	/*
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 * @since 3.4
	 */
	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Control createDialogArea(Composite parent) {

		final Composite composite= (Composite) super.createDialogArea(parent);
		createNameArea(composite);
		createMainArea(composite);

		doValidate();

		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, getHelpContextId());

		fFocusManager.restoreFocus();

		return composite;
	}

	protected void createMainArea(Composite parent) {
		fSashForm= new SashForm(parent, SWT.HORIZONTAL);
		fSashForm.setFont(parent.getFont());
		fSashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createPreferenceTree(fSashForm);
		fTree.unifySectionTitlesHeights(null);
		fTree.restoreExpansionState();
		createPreviewPane(fSashForm);

		try {
			fSashForm.setWeights(new int[] { fDialogSettings.getInt(fKeySashFormLeftWidth), fDialogSettings.getInt(fKeySashFormRightWidth) });
		} catch (NumberFormatException e) {
			Control[] children= fSashForm.getChildren();
			int treeWidth= children[0].computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
			int previewWidth= children[1].computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
			fSashForm.setWeights(new int[] { treeWidth, previewWidth });
		}
	}

	protected void createPreferenceTree(Composite parent) {
		Composite mainComp= new Composite(parent, SWT.NONE);
		mainComp.setFont(parent.getFont());
		mainComp.setLayout(new GridLayout(2, false));

		fTree= new ProfilePreferenceTree(mainComp);
		GridLayout layout= new GridLayout();
		layout.verticalSpacing= 0;
		layout.marginLeft= layout.marginWidth - 1;
		layout.marginWidth= 1;
		fTree.getScrolledPageContent().getBody().setLayout(layout);
		fTree.setConcatAncestorLabels(true);
		fTree.setExpectMultiWordValueMatch(true);

		// restoring scroll position must wait until layout is complete
		Display.getCurrent().asyncExec(fTree::restoreScrollPosition);
	}

	private void createNameArea(final Composite parent) {
		Composite nameComposite= new Composite(parent, SWT.NONE);
		nameComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		nameComposite.setLayout(new GridLayout(3, false));
		nameComposite.setFont(parent.getFont());

		fProfileNameField= new StringDialogField();
		fProfileNameField.setLabelText(FormatterMessages.ModifyDialog_ProfileName_Label);
		fProfileNameField.setText(fProfile.getName());
		fProfileNameField.getLabelControl(nameComposite).setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		fProfileNameField.getTextControl(nameComposite).setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		fProfileNameField.setDialogFieldListener(field -> doValidate());

		fSaveButton= createButton(nameComposite, SAVE_BUTTON_ID, FormatterMessages.ModifyDialog_Export_Button, false);
	}

	/**
	 * Returns the context ID for the Help system
	 *
	 * @return the string used as ID for the Help context
	 * @since 3.5
	 */
	protected abstract String getHelpContextId();

	@Override
	public void updateStatus(IStatus status) {
		if (status == null) {
			doValidate();
		} else {
			super.updateStatus(status);
		}
	}

	@Override
	protected Point getInitialLocation(Point initialSize) {
		try {
			return new Point(fDialogSettings.getInt(fKeyPreferredX), fDialogSettings.getInt(fKeyPreferredY));
		} catch (NumberFormatException ex) {
			return super.getInitialLocation(initialSize);
		}
	}

	@Override
	public boolean close() {
		final Rectangle shell= getShell().getBounds();
		fDialogSettings.put(fKeyPreferredWidth, shell.width);
		fDialogSettings.put(fKeyPreferredHight, shell.height);
		fDialogSettings.put(fKeyPreferredX, shell.x);
		fDialogSettings.put(fKeyPreferredY, shell.y);
		if (fSashForm != null) {
			Control[] children= fSashForm.getChildren();
			fDialogSettings.put(fKeySashFormLeftWidth, children[0].getSize().x);
			fDialogSettings.put(fKeySashFormRightWidth, children[1].getSize().x);
		}

		if (fTree != null)
			fTree.saveState();

		return super.close();
	}

	@Override
	protected void okPressed() {
		applyPressed();
		super.okPressed();
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == APPLAY_BUTTON_ID) {
			applyPressed();
			setTitle(Messages.format(FormatterMessages.ModifyDialog_dialog_title, fProfile.getName()));
		} else if (buttonId == SAVE_BUTTON_ID) {
			saveButtonPressed();
		} else {
			super.buttonPressed(buttonId);
		}
	}

	private void applyPressed() {
		if (!fProfile.getName().equals(fProfileNameField.getText())) {
			fProfile= fProfile.rename(fProfileNameField.getText(), fProfileManager);
		}
		fProfile.setSettings(new HashMap<>(fWorkingValues));
		fProfileManager.setSelected(fProfile);
		doValidate();
	}

	private void saveButtonPressed() {
		Profile selected= new CustomProfile(fProfileNameField.getText(), new HashMap<>(fWorkingValues), fProfile.getVersion(), fProfileManager.getProfileVersioner().getProfileKind());

		final FileDialog dialog= new FileDialog(getShell(), SWT.SAVE | SWT.SHEET);
		dialog.setText(FormatterMessages.CodingStyleConfigurationBlock_save_profile_dialog_title);
		dialog.setFilterExtensions(new String [] {"*.xml"}); //$NON-NLS-1$

		final String lastPath= JavaPlugin.getDefault().getDialogSettings().get(fLastSaveLoadPathKey + ".savepath"); //$NON-NLS-1$
		if (lastPath != null) {
			dialog.setFilterPath(lastPath);
		}
		final String path= dialog.open();
		if (path == null)
			return;

		JavaPlugin.getDefault().getDialogSettings().put(fLastSaveLoadPathKey + ".savepath", dialog.getFilterPath()); //$NON-NLS-1$

		final File file= new File(path);
		if (file.exists() && !MessageDialog.openQuestion(getShell(), FormatterMessages.CodingStyleConfigurationBlock_save_profile_overwrite_title, Messages.format(FormatterMessages.CodingStyleConfigurationBlock_save_profile_overwrite_message, BasicElementLabels.getPathLabel(file)))) {
			return;
		}
		String encoding= ProfileStore.ENCODING;
		final IContentType type= Platform.getContentTypeManager().getContentType("org.eclipse.core.runtime.xml"); //$NON-NLS-1$
		if (type != null)
			encoding= type.getDefaultCharset();
		final Collection<Profile> profiles= new ArrayList<>();
		profiles.add(selected);
		try {
			fProfileStore.writeProfilesToFile(profiles, file, encoding);
		} catch (CoreException e) {
			final String title= FormatterMessages.CodingStyleConfigurationBlock_save_profile_error_title;
			final String message= FormatterMessages.CodingStyleConfigurationBlock_save_profile_error_message;
			ExceptionHandler.handle(e, getShell(), title, message);
		}
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		fApplyButton= createButton(parent, APPLAY_BUTTON_ID, FormatterMessages.ModifyDialog_apply_button, false);
		fApplyButton.setEnabled(false);

		GridLayout layout= (GridLayout) parent.getLayout();
		layout.numColumns++;
		layout.makeColumnsEqualWidth= false;
		Label label= new Label(parent, SWT.NONE);
		GridData data= new GridData();
		data.widthHint= layout.horizontalSpacing;
		label.setLayoutData(data);
		super.createButtonsForButtonBar(parent);
	}

	@Override
	public void valuesModified() {
		doValidate();
		if (fPreview != null)
			fPreview.update();
	}

	@Override
	protected void updateButtonsEnableState(IStatus status) {
		super.updateButtonsEnableState(status);
		if (fApplyButton != null && !fApplyButton.isDisposed()) {
			fApplyButton.setEnabled(hasChanges() && !status.matches(IStatus.ERROR));
		}
		if (fSaveButton != null && !fSaveButton.isDisposed()) {
			fSaveButton.setEnabled(!validateProfileName().matches(IStatus.ERROR));
		}
	}

	private void doValidate() {
		String name= fProfileNameField.getText().trim();
		if (name.equals(fProfile.getName()) && fProfile.hasEqualSettings(fWorkingValues, fWorkingValues.keySet())) {
			updateStatus(StatusInfo.OK_STATUS);
			return;
		}

		IStatus status= validateProfileName();
		if (status.matches(IStatus.ERROR)) {
			updateStatus(status);
			return;
		}

		if (!name.equals(fProfile.getName()) && fProfileManager.containsName(name)) {
			updateStatus(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, FormatterMessages.ModifyDialog_Duplicate_Status));
			return;
		}

		if (fProfile.isBuiltInProfile() || fProfile.isSharedProfile()) {
			updateStatus(new Status(IStatus.INFO, JavaUI.ID_PLUGIN, FormatterMessages.ModifyDialog_NewCreated_Status));
			return;
		}

		updateStatus(StatusInfo.OK_STATUS);
	}

	private IStatus validateProfileName() {
		final String name= fProfileNameField.getText().trim();

		if (fProfile.isBuiltInProfile()) {
			if (fProfile.getName().equals(name)) {
				return new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, FormatterMessages.ModifyDialog_BuiltIn_Status);
			}
		}

		if (fProfile.isSharedProfile()) {
			if (fProfile.getName().equals(name)) {
				return new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, FormatterMessages.ModifyDialog_Shared_Status);
			}
		}

		if (name.length() == 0) {
			return new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, FormatterMessages.ModifyDialog_EmptyName_Status);
		}

		return StatusInfo.OK_STATUS;
	}

	private boolean hasChanges() {
		if (!fProfileNameField.getText().trim().equals(fProfile.getName()))
			return true;

		Iterator<Entry<String, String>> iter= fProfile.getSettings().entrySet().iterator();
		for (;iter.hasNext();) {
			Entry<String, String> curr= iter.next();
			if (!fWorkingValues.get(curr.getKey()).equals(curr.getValue())) {
				return true;
			}
		}
		return false;
	}

	protected Composite createPreviewPane(Composite parent) {
		final Composite previewPane= new Composite(parent, SWT.NONE);
		createGridLayout(previewPane, 1, true);
		previewPane.setFont(parent.getFont());

		createLabel(1, previewPane, FormatterMessages.ModifyDialogTabPage_preview_label_text, 0);

		fPreview= new JavaPreview(fWorkingValues, previewPane);
		fPreview.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		return previewPane;
	}

	protected void updatePreviewCode() {
		if (fPreview != null) {
			String previewCode= getPreviewCode();
			if (previewCode == null || previewCode.trim().isEmpty())
				previewCode= "// " + FormatterMessages.ModifyDialog_previewMissing_comment; //$NON-NLS-1$
			fPreview.setPreviewText(previewCode, fCurrentPreviewType);
		}
	}

	private String getPreviewCode() {
		PreferenceTreeNode<?> currentNode= fFocusManager.fCurrentlyFocused;
		if (currentNode == null)
			return null;

		// try this node
		String previewCode= doGetPreviewCode(currentNode);
		if (previewCode != null)
			return previewCode;

		// combine previews from children, if any
		StringBuilder sb= new StringBuilder();
		List<PreferenceTreeNode<?>> currentLevel= new ArrayList<>(currentNode.getChildren());
		List<PreferenceTreeNode<?>> nextLevel= new ArrayList<>();
		while (!currentLevel.isEmpty()) {
			boolean onlyModuleInfos= true;
			for (PreferenceTreeNode<?> node : currentLevel) {
				previewCode= doGetPreviewCode(node);
				onlyModuleInfos= onlyModuleInfos && fCurrentPreviewType == CodeFormatter.K_MODULE_INFO;
				if (previewCode != null && sb.indexOf(previewCode) == -1)
					sb.append("\n\n").append(previewCode); //$NON-NLS-1$
				nextLevel.addAll(node.getChildren());
			}
			if (sb.length() > 0) {
				fCurrentPreviewType= onlyModuleInfos ? CodeFormatter.K_MODULE_INFO : CodeFormatter.K_UNKNOWN;
				return sb.toString();
			}

			currentLevel= nextLevel;
			nextLevel= new ArrayList<>();
		}

		// try its ancestors
		PreferenceTreeNode<?> node= currentNode.getParent();
		while (node != null) {
			previewCode= doGetPreviewCode(node);
			if (previewCode != null)
				return previewCode;
			node= node.getParent();
		}

		return null;
	}

	private String doGetPreviewCode(PreferenceTreeNode<?> node) {
		if (!(node instanceof Section) && !(node instanceof Preference))
			return null;
		String key= node instanceof Section ? ((Section) node).getKey() : ((Preference<?>) node).getKey();
		final String START_PREFIX= "//--PREVIEW--START--"; //$NON-NLS-1$
		final String END_PREFIX= "//--PREVIEW--END--"; //$NON-NLS-1$
		int startIndex= fPreviewSources.indexOf(START_PREFIX + key);
		if (startIndex < 0)
			return null;

		fCurrentPreviewType= CodeFormatter.K_UNKNOWN;
		int nextPos= startIndex + START_PREFIX.length() + key.length();
		switch (fPreviewSources.charAt(nextPos)) {
			case '\n':
				break;
			case ':':
				if (fPreviewSources.indexOf("COMPILATION_UNIT\n", nextPos) == nextPos + 1) { //$NON-NLS-1$
					fCurrentPreviewType= CodeFormatter.K_COMPILATION_UNIT;
				} else if (fPreviewSources.indexOf("EXPRESSION\n", nextPos) == nextPos + 1) { //$NON-NLS-1$
					fCurrentPreviewType= CodeFormatter.K_EXPRESSION;
				} else if (fPreviewSources.indexOf("CLASS_BODY_DECLARATIONS\n", nextPos) == nextPos + 1) { //$NON-NLS-1$
					fCurrentPreviewType= CodeFormatter.K_CLASS_BODY_DECLARATIONS;
				} else if (fPreviewSources.indexOf("STATEMENTS\n", nextPos) == nextPos + 1) { //$NON-NLS-1$
					fCurrentPreviewType= CodeFormatter.K_STATEMENTS;
				} else if (fPreviewSources.indexOf("MODULE_INFO\n", nextPos) == nextPos + 1) { //$NON-NLS-1$
					fCurrentPreviewType= CodeFormatter.K_MODULE_INFO;
				}
				break;
			default:
				return null;
		}

		int endIndex= fPreviewSources.indexOf(END_PREFIX + key + '\n');
		String previewCode= fPreviewSources.substring(startIndex, endIndex);
		previewCode= previewCode.replaceAll(START_PREFIX + ".*\\R", ""); //$NON-NLS-1$ //$NON-NLS-2$
		previewCode= previewCode.replaceAll(END_PREFIX + ".*\\R", ""); //$NON-NLS-1$ //$NON-NLS-2$
		return previewCode;
	}

	protected void loadPreviews(String previewFile) {
		String resource= "/preview/" + previewFile; //$NON-NLS-1$
		try (Scanner s= new Scanner(getClass().getResourceAsStream(resource), "UTF-8")) { //$NON-NLS-1$
			fPreviewSources= s.useDelimiter("\\A").next(); //$NON-NLS-1$
		}
	}

	protected static Composite createGridComposite(Composite parent, int numColumns) {
		final Composite grid= new Composite(parent, SWT.NONE);
		createGridLayout(parent, numColumns, true);
		grid.setFont(parent.getFont());
		return grid;
	}

	/*
	 * Create a GridLayout with the default margin and spacing settings, as
	 * well as the specified number of columns.
	 */
	protected static void createGridLayout(Composite composite, int numColumns, boolean margins) {
		final GridLayout layout= new GridLayout(numColumns, false);
		PixelConverter pixelConverter= new PixelConverter(composite);
		layout.verticalSpacing= pixelConverter.convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing= pixelConverter.convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		if (margins) {
			layout.marginHeight= pixelConverter.convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
			layout.marginWidth= pixelConverter.convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		} else {
			layout.marginHeight= 0;
			layout.marginWidth= 0;
		}
		composite.setLayout(layout);
	}

	/*
	 * Convenience method to create a GridData.
	 */
	protected static GridData createGridData(int numColumns, int style, int widthHint, int indent) {
		final GridData gd= new GridData(style);
		gd.horizontalSpan= numColumns;
		gd.widthHint= widthHint;
		gd.horizontalIndent= indent * LayoutUtil.getIndent();
		return gd;
	}

	/*
	 * Convenience method to create a label
	 */
	protected static Label createLabel(int numColumns, Composite parent, String text, int indent) {
		final Label label= new Label(parent, SWT.WRAP);
		label.setFont(parent.getFont());
		label.setText(text);
		GridData gd= new GridData(GridData.BEGINNING, GridData.CENTER, true, false, numColumns, 1);
		gd.horizontalIndent= indent * LayoutUtil.getIndent();
		label.setLayoutData(gd);
		return label;
	}

	/*
	 * Helper to be used with Preference.addDependant(), dependency checker that accepts specific values.
	 */
	protected static Predicate<String> valueAcceptor(String... values) {
		final List<String> valuesList= Arrays.asList(values);
		return valuesList::contains;
	}
}
