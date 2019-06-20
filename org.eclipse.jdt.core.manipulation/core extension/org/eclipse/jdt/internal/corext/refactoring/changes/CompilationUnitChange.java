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
 *     Red Hat Inc. - copied to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.ltk.core.refactoring.TextEditBasedChangeGroup;
import org.eclipse.ltk.core.refactoring.TextFileChange;

import org.eclipse.jdt.core.ICompilationUnit;


/**
 * A {@link TextFileChange} that operates on an {@link ICompilationUnit}.
 * <p>
 * DO NOT REMOVE, used in a product.</p>
 * @deprecated As of 3.5, replaced by {@link org.eclipse.jdt.core.refactoring.CompilationUnitChange}
 */
@Deprecated
public class CompilationUnitChange extends org.eclipse.jdt.core.refactoring.CompilationUnitChange {

	/**
	 * Creates a new <code>CompilationUnitChange</code>.
	 *
	 * @param name the change's name, mainly used to render the change in the UI
	 * @param cunit the compilation unit this change works on
	 */
	public CompilationUnitChange(String name, ICompilationUnit cunit) {
		super(name, cunit);
	}

	/**
	 * @param change the change
	 * @since 3.6
	 */
	public CompilationUnitChange(org.eclipse.jdt.core.refactoring.CompilationUnitChange change) {
		super(change.getName(), change.getCompilationUnit());
		setDescriptor(change.getDescriptor());
		TextEdit edit= change.getEdit();
		if (edit != null) {
			setEdit(edit);
		}
		setEnabledShallow(change.isEnabled());
		setKeepPreviewEdits(change.getKeepPreviewEdits());
		setSaveMode(change.getSaveMode());
		setTextType(change.getTextType());
		for (TextEditBasedChangeGroup group : change.getChangeGroups()) {
			addChangeGroup(group);
		}
	}
}

