/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui;

import java.io.IOException;
import java.util.concurrent.Executors;

import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.templates.persistence.TemplateStore;

import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;

import org.eclipse.jdt.core.manipulation.JavaManipulation;

/**
 * Initializer for the template store.
 *
 */
public class TemplateStoreInitializer {

	/**
	 * The key to store customized code templates.
	 */
	private static final String CODE_TEMPLATES_KEY= "org.eclipse.jdt.ui.text.custom_code_templates"; //$NON-NLS-1$

	/**
	 * The key to store whether the legacy code templates have been migrated
	 */
	private static final String CODE_TEMPLATES_MIGRATION_KEY= "org.eclipse.jdt.ui.text.code_templates_migrated"; //$NON-NLS-1$

	private volatile Promise<TemplateStore> fCodeTemplateStore;

	public void activate() {
		fCodeTemplateStore = loadTemplateStoreAsync();
	}

	/**
	 * Returns the promise for the template store
	 * @return the promise for the template store
	 */
	public Promise<TemplateStore> getCodeTemplateStore() {
		return fCodeTemplateStore;
	}

	/**
	 * Loads the template store asynchronous. It returns a promise that is either resolved
	 * or not, when an error occurs. In that case
	 * @return a promise of the template store, that is either resolved or not ()
	 */
	private Promise<TemplateStore> loadTemplateStoreAsync() {
		PromiseFactory pf = new PromiseFactory(Executors.newSingleThreadExecutor());
		Promise<TemplateStore> promise = pf.submit(this::loadCodeTemplateStore);
		// set all template store data, when promise is resolved and logs, if an exception occurs
		return promise.onFailure(JavaPlugin::log);
	}

	/**
	 * Sets the template store information
	 * @param templateStore the template store instance
	 */
	private void setTemplateStore(TemplateStore templateStore) {
		JavaManipulation.setCodeTemplateStore(templateStore);
		JavaManipulation.setCodeTemplateContextRegistry(JavaPlugin.getDefault().getCodeTemplateContextRegistry());
	}

	/**
	 * Loads the template store data
	 * @return the template store
	 */
	public TemplateStore loadCodeTemplateStore() {
		JavaPlugin javaPlugin= JavaPlugin.getDefault();
		IPreferenceStore store= javaPlugin.getPreferenceStore();
		boolean alreadyMigrated= store.getBoolean(CODE_TEMPLATES_MIGRATION_KEY);
		TemplateStore templateStore;
		if (alreadyMigrated) {
			templateStore= new ContributionTemplateStore(javaPlugin.getCodeTemplateContextRegistry(), store, CODE_TEMPLATES_KEY);
		} else {
			templateStore= new CompatibilityTemplateStore(javaPlugin.getCodeTemplateContextRegistry(), store, CODE_TEMPLATES_KEY, javaPlugin.getOldCodeTemplateStoreInstance());
			store.setValue(CODE_TEMPLATES_MIGRATION_KEY, true);
		}

		try {
			templateStore.load();
		} catch (IOException e) {
			JavaPlugin.log(e);
		}

		templateStore.startListeningForPreferenceChanges();

		// compatibility / bug fixing code for duplicated templates
		// TODO remove for 3.0
		CompatibilityTemplateStore.pruneDuplicates(templateStore, true);
		setTemplateStore(templateStore);
		return templateStore;
	}

}
