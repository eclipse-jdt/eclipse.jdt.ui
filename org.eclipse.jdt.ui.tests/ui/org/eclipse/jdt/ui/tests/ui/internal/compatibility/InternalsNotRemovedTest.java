/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.ui.internal.compatibility;

import org.eclipse.text.tests.Accessor;


/**
 * Ensures that internal code which is used by a product doesn't get removed.
 * 
 * @deprecated to hide deprecation warnings
 * @since 3.6
 */
public class InternalsNotRemovedTest {

	static final String[] INTERNAL_FIELDS= new String[] {

			// See https://bugs.eclipse.org/296836
			org.eclipse.jdt.internal.ui.actions.ActionMessages.OrganizeImportsAction_summary_added,
			org.eclipse.jdt.internal.ui.actions.ActionMessages.OrganizeImportsAction_summary_removed,

			// See https://bugs.eclipse.org/296836
			org.eclipse.jdt.internal.ui.refactoring.reorg.ReorgMessages.ReorgGroup_paste,
			org.eclipse.jdt.internal.ui.refactoring.reorg.ReorgMessages.ReorgGroup_delete,
			org.eclipse.jdt.internal.ui.refactoring.reorg.ReorgMessages.CutSourceReferencesToClipboardAction_cut,

			org.eclipse.jdt.internal.ui.refactoring.reorg.ReorgMessages.JdtMoveAction_update_references,

			org.eclipse.jdt.internal.ui.JavaUIMessages.TypeSelectionDialog_lowerLabel,
			org.eclipse.jdt.internal.ui.JavaUIMessages.TypeSelectionDialog_upperLabel,

			// See https://bugs.eclipse.org/297529
			org.eclipse.jdt.internal.ui.workingsets.JavaWorkingSetUpdater.ID

	};

	static final Class[] INTERNAL_TYPES= new Class[] {
			org.eclipse.jdt.internal.corext.SourceRange.class,
			org.eclipse.jdt.internal.ui.refactoring.RefactoringSaveHelper.class,
			org.eclipse.jdt.internal.ui.util.PixelConverter.class,
			org.eclipse.jdt.internal.junit.launcher.TestSelectionDialog.class,
			org.eclipse.jdt.internal.corext.dom.NodeFinder.class,
			org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange.class,
			org.eclipse.jdt.internal.ui.dialogs.TypeSelectionDialog2.class
	};

	void internalMethods() throws Exception {
		new org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite(null).createChange();
		new org.eclipse.jdt.internal.corext.refactoring.reorg.JavaMoveProcessor(null).canUpdateReferences();
		org.eclipse.jdt.internal.ui.actions.OpenBrowserUtil.open(null, null, null);
	}


	void testFieldNotRemoved(String className, String fieldName) {
		Accessor classObject= new Accessor(className, getClass().getClassLoader());
		classObject.getField(fieldName);
	}

	void testClassNotRemoved(String className) {
		new Accessor(className, getClass().getClassLoader());
	}
}
