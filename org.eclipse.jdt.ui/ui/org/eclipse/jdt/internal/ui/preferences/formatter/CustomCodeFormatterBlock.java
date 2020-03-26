/*******************************************************************************
 * Copyright (c) 2014 Google Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Harry Terkelsen (het@google.com) - Bug 449262 - Allow the use of third-party Java formatters
 *     IBM Corporation - maintenance
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Observable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;

import org.eclipse.core.resources.IProject;

import org.eclipse.jface.layout.GridLayoutFactory;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.util.CollectionsUtil;

import org.eclipse.jdt.internal.ui.preferences.PreferencesAccess;

/**
 * Shows a combo box to choose the formatter. If there are no contributed
 * formatters, nothing is shown.
 */
public class CustomCodeFormatterBlock extends Observable {

	private static class FormatterContribution {
		public String fId;
		public String fName;

		public FormatterContribution(String id, String name) {
			fId= id;
			fName= name;
		}
	}

	private static final String ATTR_NAME = "name"; //$NON-NLS-1$
	private static final String ATTR_ID = "id"; //$NON-NLS-1$

	private IEclipsePreferences fPrefs;
	private String fDefaultFormatterId;
	private FormatterContribution[] fFormatters;

	private Combo fFormatterCombo;

	public CustomCodeFormatterBlock(IProject project, PreferencesAccess access) {
		final IScopeContext scope;
		final IEclipsePreferences defaults;
		if (project != null) {
			scope= access.getProjectScope(project);
			defaults= access.getInstanceScope().getNode(JavaCore.PLUGIN_ID);
		} else {
			scope= access.getInstanceScope();
			defaults= access.getDefaultScope().getNode(JavaCore.PLUGIN_ID);
		}
		fPrefs= scope.getNode(JavaCore.PLUGIN_ID);
		fDefaultFormatterId= defaults.get(JavaCore.JAVA_FORMATTER, JavaCore.DEFAULT_JAVA_FORMATTER);
		initializeFormatters();
	}

	public void performOk() {
		if (fFormatterCombo == null) {
			return;
		}
		String formatterId= fFormatters[fFormatterCombo.getSelectionIndex()].fId;
		if (!formatterId.equals(fDefaultFormatterId)) {
			fPrefs.put(JavaCore.JAVA_FORMATTER, formatterId);
		} else {
			// Simply reset to the default one.
			performDefaults();
		}
	}

	public void performDefaults() {
		fPrefs.remove(JavaCore.JAVA_FORMATTER);

		if (fFormatterCombo == null) {
			return;
		}
		int index= getFormatterIndex(fDefaultFormatterId);
		fFormatterCombo.select(index);
		handleFormatterChanged();
	}

	public void enableProjectSpecificSettings(boolean useProjectSpecificSettings) {
		if (useProjectSpecificSettings) {
			if (fDefaultFormatterId != null) {
				fPrefs.put(JavaCore.JAVA_FORMATTER, fDefaultFormatterId);
			}
		} else {
			initDefault();
		}
	}

	public void createContents(Composite parent, int numColumns) {
		if (fFormatters.length <= 1) {
			return; // No selector is needed since there is only one formatter.
		}

		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(GridLayoutFactory.fillDefaults().margins(0, 10).numColumns(2).create());
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= numColumns;
		composite.setLayoutData(gd);
		composite.setFont(parent.getFont());

		Label label= new Label(composite, SWT.NONE);
		label.setText(FormatterMessages.CustomCodeFormatterBlock_formatter_name);

		fFormatterCombo= new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
		fFormatterCombo.setFont(composite.getFont());
		fFormatterCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleFormatterChanged();
			}
		});
		for (FormatterContribution formatter : fFormatters) {
			fFormatterCombo.add(formatter.fName);
		}
		initDefault();
	}

	private void initDefault() {
		if (fFormatterCombo == null) {
			return;
		}
		String formatterID= fPrefs.get(JavaCore.JAVA_FORMATTER, fDefaultFormatterId);
		fFormatterCombo.select(getFormatterIndex(formatterID));
	}

	private int getFormatterIndex(String formatterId) {
		if (formatterId != null) {
			for (int i= 0; i < fFormatters.length; i++) {
				if (formatterId.equals(fFormatters[i].fId)) {
					return i;
				}
			}
		}
		return 0;
	}

	private void handleFormatterChanged() {
		setChanged();
		String formatterId= getFormatterId();
		notifyObservers(formatterId);
	}

	/**
	 * @return the currently selected formatter id
	 */
	public String getFormatterId() {
		if (fFormatterCombo == null) {
			return fPrefs.get(JavaCore.JAVA_FORMATTER, fDefaultFormatterId);
		}
		return fFormatters[fFormatterCombo.getSelectionIndex()].fId;
	}

	private void initializeFormatters() {
		ArrayList<FormatterContribution> formatters= new ArrayList<>();
		IExtensionPoint point= Platform.getExtensionRegistry().getExtensionPoint(JavaCore.PLUGIN_ID, JavaCore.JAVA_FORMATTER_EXTENSION_POINT_ID);
		if (point != null) {
			IExtension[] exts= point.getExtensions();
			for (IExtension ext : exts) {
				IConfigurationElement[] elements= ext.getConfigurationElements();
				for (IConfigurationElement element : elements) {
					String name = element.getAttribute(ATTR_NAME);
					String id= element.getAttribute(ATTR_ID);
					formatters.add(new FormatterContribution(id, name));
				}
			}
		}
		Collections.sort(formatters, Comparator.comparing((FormatterContribution o1) -> o1.fName));
		fFormatters= CollectionsUtil.toArray(formatters, FormatterContribution.class);
	}
}
