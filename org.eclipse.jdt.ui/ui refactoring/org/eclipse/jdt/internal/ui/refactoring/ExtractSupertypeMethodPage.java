/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.refactoring;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractSupertypeProcessor;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Wizard page to select methods to be deleted after extract supertype.
 *
 * @since 3.2
 */
public class ExtractSupertypeMethodPage extends PullUpMethodPage {


	public ExtractSupertypeMethodPage(ExtractSupertypeProcessor processor) {
		super(processor);
	}

	/**
	 * Returns the refactoring processor.
	 *
	 * @return the refactoring processor
	 */
	private ExtractSupertypeProcessor getProcessor() {
		return (ExtractSupertypeProcessor) getPullUpRefactoringProcessor();
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.EXTRACT_SUPERTYPE_WIZARD_PAGE);
	}

	@Override
	public void setVisible(final boolean visible) {
		if (visible) {
			final ExtractSupertypeProcessor processor= getProcessor();
			processor.resetChanges();
			try {
				getWizard().getContainer().run(false, false, monitor -> processor.createWorkingCopyLayer(monitor));
			} catch (InvocationTargetException exception) {
				JavaPlugin.log(exception);
			} catch (InterruptedException exception) {
				// Does not happen
			}
		}
		super.setVisible(visible);
	}
}
