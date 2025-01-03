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
package org.eclipse.jdt.internal.ui.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.ISetSelectionTarget;

public class SelectionUtil {

	public static List<?> toList(ISelection selection) {
		if (selection instanceof IStructuredSelection)
			return ((IStructuredSelection) selection).toList();
		return null;
	}

	/**
	 * Returns the selected element if the selection consists of a single
	 * element only.
	 *
	 * @param s the selection
	 * @return the selected first element or null
	 */
	public static Object getSingleElement(ISelection s) {
		if (! (s instanceof IStructuredSelection))
			return null;
		IStructuredSelection selection= (IStructuredSelection) s;
		if (selection.size() != 1)
			return null;

		return selection.getFirstElement();
	}


	/**
	 * Attempts to select and reveal the specified resources in all parts within
	 * the supplied workbench window's active page.
	 * <p>
	 * Checks all parts in the active page to see if they implement
	 * <code>ISetSelectionTarget</code>, either directly or as an adapter. If
	 * so, tells the part to select and reveal the specified resources.
	 * </p>
	 *
	 * @param resources the resources to be selected and revealed
	 * @param window the workbench window to select and reveal the resource
	 *
	 * @see ISetSelectionTarget
	 *
	 * @see org.eclipse.ui.wizards.newresource.BasicNewResourceWizard#selectAndReveal(IResource,
	 *      IWorkbenchWindow)
	 */
	public static void selectAndReveal(IResource[] resources, IWorkbenchWindow window) {
		// validate the input
		if (window == null || resources == null || Arrays.asList(resources).contains(null)) {
			return;
		}
		IWorkbenchPage page= window.getActivePage();
		if (page == null) {
			return;
		}

		// get all the view and editor parts
		List<IWorkbenchPart> parts= new ArrayList<>();
		for (IWorkbenchPartReference ref : page.getViewReferences()) {
			IWorkbenchPart part= ref.getPart(false);
			if (part != null) {
				parts.add(part);
			}
		}
		for (IWorkbenchPartReference ref : page.getEditorReferences()) {
			if (ref.getPart(false) != null) {
				parts.add(ref.getPart(false));
			}
		}

		final ISelection selection= new StructuredSelection(resources);
		Iterator<IWorkbenchPart> itr= parts.iterator();
		while (itr.hasNext()) {
			IWorkbenchPart part= itr.next();

			// get the part's ISetSelectionTarget implementation
			ISetSelectionTarget target= null;
			if (part instanceof ISetSelectionTarget) {
				target= (ISetSelectionTarget) part;
			} else {
				target= part.getAdapter(ISetSelectionTarget.class);
			}

			if (target != null) {
				// select and reveal resource
				final ISetSelectionTarget finalTarget= target;
				window.getShell().getDisplay().asyncExec(() -> finalTarget.selectReveal(selection));
			}
		}
	}

	private SelectionUtil() {
	}

	private static String lastErrorMsg;

	public static void logException(String action, RuntimeException e, String title, IDocument document, int offset) {
		if (e instanceof OperationCanceledException ) {
			//  Be silent if operation is canceled see
			//  https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/1827
			return;
		}

		// log error and keep going
		String errorMsg= e.getClass().getSimpleName() + " " + action; //$NON-NLS-1$
		if (title != null) {
			errorMsg+= " in " + title; //$NON-NLS-1$
		}
		errorMsg+= " at offset " + offset; //$NON-NLS-1$
		try {
			int lineOfOffset= document.getLineOfOffset(offset);
			String source= "Source line " + (lineOfOffset + 1); //$NON-NLS-1$
			int CONTEXT_LINES= 10;
			int startLineOffset= document.getLineOffset(Math.max(0, lineOfOffset - CONTEXT_LINES));
			source+= " :"+System.lineSeparator(); //$NON-NLS-1$
			source+= "-----" + System.lineSeparator(); //$NON-NLS-1$
			source+= document.get(startLineOffset, offset - startLineOffset); // source until offset
			source+= '|'; // cursor
			source+= document.get(offset, document.getLineOffset(lineOfOffset) + document.getLineLength(lineOfOffset) - offset); // rest of line
			source+= "-----"; //$NON-NLS-1$
			e.addSuppressed(new Throwable(source));
		} catch (BadLocationException ble) {
			e.addSuppressed(ble);
		}
		if (!Objects.equals(lastErrorMsg, errorMsg)) { // avoid repetitive logging
			lastErrorMsg= errorMsg;
			ILog.get().error(errorMsg, e);
		}
	}

}
