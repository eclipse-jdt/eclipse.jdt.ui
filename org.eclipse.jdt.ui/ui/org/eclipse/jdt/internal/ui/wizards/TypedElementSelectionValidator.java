/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.wizards;

import java.util.Collection;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.ui.dialogs.ISelectionStatusValidator;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;

/**
 * Implementation of a <code>ISelectionValidator</code> to validate the
 * type of an element.
 * Empty selections are not accepted.
 */
public class TypedElementSelectionValidator implements ISelectionStatusValidator {

	private IStatus fgErrorStatus= new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
	private IStatus fgOKStatus= new StatusInfo();

	private Class<?>[] fAcceptedTypes;
	private boolean fAllowMultipleSelection;
	private Collection<?> fRejectedElements;

	/**
	 * @param acceptedTypes The types accepted by the validator
	 * @param allowMultipleSelection If set to <code>true</code>, the validator
	 * allows multiple selection.
	 */
	public TypedElementSelectionValidator(Class<?>[] acceptedTypes, boolean allowMultipleSelection) {
		this(acceptedTypes, allowMultipleSelection, null);
	}

	/**
	 * @param acceptedTypes The types accepted by the validator
	 * @param allowMultipleSelection If set to <code>true</code>, the validator
	 * allows multiple selection.
	 * @param rejectedElements A list of elements that are not accepted
	 */
	public TypedElementSelectionValidator(Class<?>[] acceptedTypes, boolean allowMultipleSelection, Collection<?> rejectedElements) {
		Assert.isNotNull(acceptedTypes);
		fAcceptedTypes= acceptedTypes;
		fAllowMultipleSelection= allowMultipleSelection;
		fRejectedElements= rejectedElements;
	}

	/*
	 * @see org.eclipse.ui.dialogs.ISelectionValidator#isValid(java.lang.Object)
	 */
	@Override
	public IStatus validate(Object[] elements) {
		if (isValid(elements)) {
			return fgOKStatus;
		}
		return fgErrorStatus;
	}

	private boolean isOfAcceptedType(Object o) {
		for (Class<?> type : fAcceptedTypes) {
			if (type.isInstance(o)) {
				return true;
			}
		}
		return false;
	}

	private boolean isRejectedElement(Object elem) {
		return (fRejectedElements != null) && fRejectedElements.contains(elem);
	}

	/**
	 * @param elem the element to test
	 * @return returns if the selected element is valid
	 */
	protected boolean isSelectedValid(Object elem) {
		return true;
	}

	private boolean isValid(Object[] selection) {
		if (selection.length == 0) {
			return false;
		}

		if (!fAllowMultipleSelection && selection.length != 1) {
			return false;
		}

		for (Object o : selection) {
			if (!isOfAcceptedType(o) || isRejectedElement(o) || !isSelectedValid(o)) {
				return false;
			}
		}
		return true;
	}
}
