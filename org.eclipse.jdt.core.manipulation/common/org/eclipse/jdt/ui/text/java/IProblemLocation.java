/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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
 *     Red Hat Inc - noted interface is duplicative
 *******************************************************************************/
package org.eclipse.jdt.ui.text.java;

import org.eclipse.jdt.internal.ui.text.correction.IProblemLocationCore;


/**
 * Problem information for quick fix and quick assist processors.
 * This interface, or the core version, appears duplicative and unnecessary.
 * <p>
 * Note: this interface is not intended to be implemented.
 * </p>
 *
 * @since 1.20
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IProblemLocation extends IProblemLocationCore {
}
