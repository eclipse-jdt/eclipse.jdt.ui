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
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.search.ui.text.Match;

/**
 * A search match with additional java-specific info.
 */
public class JavaElementMatch extends Match {
	private final int fAccuracy;
	private final int fMatchRule;
	private final boolean fIsWriteAccess;
	private final boolean fIsReadAccess;
	private final boolean fIsJavadoc;
	private final boolean fIsPolymorphic;
	
	JavaElementMatch(Object element, int matchRule, int offset, int length, int accuracy, boolean isReadAccess, boolean isWriteAccess, boolean isJavadoc, boolean isPolymorphic) {
		super(element, offset, length);
		fAccuracy= accuracy;
		fMatchRule= matchRule;
		fIsWriteAccess= isWriteAccess;
		fIsReadAccess= isReadAccess;
		fIsJavadoc= isJavadoc;
		fIsPolymorphic= isPolymorphic;
	}

	public int getAccuracy() {
		return fAccuracy;
	}

	public boolean isWriteAccess() {
		return fIsWriteAccess;
	}

	public boolean isReadAccess() {
		return fIsReadAccess;
	}

	public boolean isJavadoc() {
		return fIsJavadoc;
	}
	
	public boolean isPolymorphic() {
		return fIsPolymorphic;
	}
	
	public int getMatchRule() {
		return fMatchRule;
	}
}
