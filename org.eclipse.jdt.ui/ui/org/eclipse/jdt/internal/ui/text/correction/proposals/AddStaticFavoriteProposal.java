/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import org.osgi.service.prefs.BackingStoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.preferences.WorkingCopyManager;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.java.correction.ChangeCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock;
import org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock.Key;

public class AddStaticFavoriteProposal extends ChangeCorrectionProposal {

	final String fImportName;
	final String fChangeDescription;

	public AddStaticFavoriteProposal(String importName, String changeName, String changeDescription, Image image, int relevance) {
		super(changeName, null, relevance, image);
		this.fImportName= importName;
		this.fChangeDescription= changeDescription;
	}

	@Override
	public void apply(IDocument document) {
		WorkingCopyManager manager= new WorkingCopyManager();
		OptionsConfigurationBlock.Key prefKey= new Key(JavaUI.ID_PLUGIN, PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS);
		String favorites= prefKey.getStoredValue(InstanceScope.INSTANCE, manager);
		if (favorites == null || favorites.isEmpty()) {
			favorites= fImportName;
		} else {
			favorites= favorites.concat(";").concat(fImportName); //$NON-NLS-1$
		}
		prefKey.setStoredValue(InstanceScope.INSTANCE, favorites, manager);
		try {
			manager.applyChanges();
		} catch (BackingStoreException e) {
			JavaPlugin.log(e);
		}
	}

	@Override
	public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
		return fChangeDescription;
	}

}
