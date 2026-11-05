/*******************************************************************************
 * Copyright (c) 2017, 2026 Angelo Zerr and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation,
 *         originally in https://github.com/angelozerr/jdt-codemining (EPL)
 *     Kai Winter <kaiwinter@gmx.de> - JUnit 5 ParameterizedTest support in jdt-codemining
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui.codemining;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.javaeditor.codemining.AbstractJavaElementLineHeaderCodeMining;

/**
 * Code mining shown above a test class or test method, which runs or debugs the element when
 * clicked, with the JUnit launch shortcut and the test kind that detected the element. Like the
 * other launch actions of the workbench, the shortcut is called from the UI thread: it only creates
 * or finds the launch configuration, and the launch itself runs in a job. Its errors and choices
 * are reported to the user by the shortcut.
 */
public class JUnitLaunchCodeMining extends AbstractJavaElementLineHeaderCodeMining {

	/**
	 * Creates the code mining.
	 *
	 * @param element the test class or test method
	 * @param label the label of the code mining
	 * @param testKindId the id of the test kind to launch with
	 * @param mode the launch mode, see {@link org.eclipse.debug.core.ILaunchManager}
	 * @param document the document of the editor
	 * @param provider the provider of the code mining
	 * @throws JavaModelException if the position of the element cannot be computed
	 * @throws BadLocationException if the position of the element cannot be computed
	 */
	public JUnitLaunchCodeMining(IJavaElement element, String label, String testKindId, String mode, IDocument document, ICodeMiningProvider provider)
			throws JavaModelException, BadLocationException {
		super(element, document, provider, _ -> new KindLaunchShortcut(testKindId).launch(new StructuredSelection(element), mode));
		setLabel(label);
	}
}
