/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

package org.eclipse.jdt.internal.ui.text;


import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.rules.IRule;

import org.eclipse.jdt.ui.text.IColorManager;


public final class SingleTokenJavaScanner extends AbstractJavaScanner{


	private String[] fProperty;

	public SingleTokenJavaScanner(IColorManager manager, IPreferenceStore store, String property) {
		super(manager, store);
		fProperty= new String[] { property };
		initialize();
	}

	/*
	 * @see AbstractJavaScanner#getTokenProperties()
	 */
	@Override
	protected String[] getTokenProperties() {
		return fProperty;
	}

	/*
	 * @see AbstractJavaScanner#createRules()
	 */
	@Override
	protected List<IRule> createRules() {
		setDefaultReturnToken(getToken(fProperty[0]));
		return null;
	}
}

