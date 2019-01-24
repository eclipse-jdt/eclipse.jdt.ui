/**
 * Copyright (c) 2011 Stefan Henss.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Henß - initial API and implementation.
 */
package org.eclipse.jdt.internal.ui.text;

import java.util.List;

public class Chain {

	private final List<ChainElement> elements;

	private final int expectedDimensions;

	public Chain(final List<ChainElement> elements, final int expectedDimensions) {
		this.elements= elements;
		this.expectedDimensions= expectedDimensions;
	}

	public List<ChainElement> getElements() {
		return elements;
	}

	public int getExpectedDimensions() {
		return expectedDimensions;
	}
}
