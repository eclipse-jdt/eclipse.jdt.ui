/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
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
package org.eclipse.jdt.internal.ui.fix;

import java.util.Collections;
import java.util.Map;

/**
 * A fix that replaces a deprecated method call with the recommended alternative if the deprecated method uses said
 * recommended call.  The entire method is inlined in case there is massaging of parameters required.
 * <ul>
 * <li>Deprecated method must have a Javadoc specifying "use" followed by link</li>
 * <li>The deprecated method must use the specified linked method and not reference private methods/fields.</li>
 * </ul>
 */
public class InlineDeprecatedMethodCleanUp extends AbstractMultiFixCoreWrapper<InlineDeprecatedMethodCleanUpCore> {

	public InlineDeprecatedMethodCleanUp(final Map<String, String> options) {
		super(options, new InlineDeprecatedMethodCleanUpCore());
	}

	public InlineDeprecatedMethodCleanUp() {
		this(Collections.EMPTY_MAP);
	}
}
