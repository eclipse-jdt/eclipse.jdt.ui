/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import org.eclipse.core.runtime.Assert;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;

import org.eclipse.ltk.core.refactoring.Refactoring;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

/**
 * Expandable composite which displays a short description of the settings being
 * used for a refactoring if collapsed.
 * 
 * @since 3.2
 */
public final class ExpandableSettingSection extends ExpandableComposite {

	/** Expansion state listener */
	private class ExpansionStateListener extends ExpansionAdapter {

		/**
		 * {@inheritDoc}
		 */
		public void expansionStateChanged(final ExpansionEvent event) {
			if (event.getState())
				fDescription.setText(""); //$NON-NLS-1$
			else
				updateDescription();
		}
	}

	/** The description label */
	private final Label fDescription;

	/** The description provider */
	private final IDescriptionProvider fProvider;

	/** The refactoring */
	private final Refactoring fRefactoring;

	/**
	 * Creates a new expandable setting section.
	 * 
	 * @param parent
	 *            the parent control
	 * @param refactoring
	 * @param style
	 *            the style
	 * @param provider
	 *            the description provider
	 */
	public ExpandableSettingSection(final Composite parent, final Refactoring refactoring, final int style, final IDescriptionProvider provider) {
		super(parent, style, ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT | ExpandableComposite.LEFT_TEXT_CLIENT_ALIGNMENT);
		Assert.isNotNull(provider);
		Assert.isNotNull(refactoring);
		fProvider= provider;
		fRefactoring= refactoring;
		fDescription= new Label(this, SWT.LEFT | SWT.WRAP);
		updateDescription();
		setTextClient(fDescription);
		addExpansionListener(new ExpansionStateListener());
	}

	/**
	 * Updates the description.
	 */
	public void updateDescription() {
		final String description= fProvider.getDescription(fRefactoring);
		if (description != null && !"".equals(description)) //$NON-NLS-1$
			fDescription.setText(Messages.format(RefactoringMessages.ExpandableSettingSection_description_pattern, description));
		else
			fDescription.setText(""); //$NON-NLS-1$
	}
}