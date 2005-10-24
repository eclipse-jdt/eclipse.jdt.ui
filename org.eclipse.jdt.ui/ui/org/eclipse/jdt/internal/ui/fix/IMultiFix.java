/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.IFix;

/**
 * A multi fix can solve several differend problems in
 * a given <code>CompilationUnit</code>. The <code>CompilationUnit</code>
 * is compiled by using the compiler options returend by
 * <code>getRequiredOptions</code>. A <code>IMultiFix</code> can have
 * differend options which can be set by using the <code>Control</code>
 * returned by <code>createConfigurationControl</code>
 *
 * @since 3.2
 */
public interface IMultiFix {
	
	/**
	 * Create a <code>IFix</code> which fixes all problems which this multi
	 * fix can fix in <code>CompilationUnit</code>.
	 * 
	 * @param compilationUnit The compilation unit to fix, may be null
	 * @return The fix or null if no fixes possible
	 * @throws CoreException
	 */
	public abstract IFix createFix(CompilationUnit compilationUnit) throws CoreException;
	
	/**
	 * Required compiler options to allow <code>createFix</code> to work
	 * correct.
	 *
	 * @return The options als map or null
	 */
	public abstract Map getRequiredOptions();
	
	/**
	 * Create a controll to configure the options for this multi fix
	 * in a UI.
	 * 
	 * @param parent The composite in which the result is contained in
	 * @return The control, not null.
	 */
	public abstract Control createConfigurationControl(Composite parent);

	/**
	 * Persist current settings of this in <code>settings</code>
	 * 
	 * @param settings The settings to store to, not null
	 */
	public abstract void saveSettings(IDialogSettings settings);
}
