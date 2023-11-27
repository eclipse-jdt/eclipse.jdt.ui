/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.jdt.ui;

import java.io.Reader;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.manipulation.internal.javadoc.CoreJavadocContentAccessUtility;

/**
 * Helper needed to get the content of a Javadoc comment.
 *
 * <p>
 * This class is not intended to be subclassed or instantiated by clients.
 * </p>
 *
 * @since 3.1
 *
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 */
public class JavadocContentAccess {

	private JavadocContentAccess() {
		// do not instantiate
	}

	/**
	 * Gets a reader for an IMember's Javadoc comment content from the source attachment.
	 * The content does contain only the text from the comment without the Javadoc leading star characters.
	 * Returns <code>null</code> if the member does not contain a Javadoc comment or if no source is available.
	 * @param member The member to get the Javadoc of.
	 * @param allowInherited For methods with no (Javadoc) comment, the comment of the overridden class
	 * is returned if <code>allowInherited</code> is <code>true</code>.
	 * @return Returns a reader for the Javadoc comment content or <code>null</code> if the member
	 * does not contain a Javadoc comment or if no source is available
	 * @throws JavaModelException is thrown when the elements javadoc can not be accessed
	 */
	public static Reader getContentReader(IMember member, boolean allowInherited) throws JavaModelException {
		return CoreJavadocContentAccessUtility.getContentReader(member, allowInherited);
	}


	/**
	 * Gets a reader for an IMember's Javadoc comment content from the source attachment.
	 * and renders the tags in HTML.
	 * Returns <code>null</code> if the member does not contain a Javadoc comment or if no source is available.
	 *
	 * @param member				the member to get the Javadoc of.
	 * @param allowInherited		for methods with no (Javadoc) comment, the comment of the overridden
	 * 									class is returned if <code>allowInherited</code> is <code>true</code>
	 * @param useAttachedJavadoc	if <code>true</code> Javadoc will be extracted from attached Javadoc
	 * 									if there's no source
	 * @return a reader for the Javadoc comment content in HTML or <code>null</code> if the member
	 * 			does not contain a Javadoc comment or if no source is available
	 * @throws JavaModelException is thrown when the elements Javadoc can not be accessed
	 * @since 3.2
	 */
	public static Reader getHTMLContentReader(IMember member, boolean allowInherited, boolean useAttachedJavadoc) throws JavaModelException {
		return CoreJavadocContentAccessUtility.getHTMLContentReader(member, allowInherited, useAttachedJavadoc);
	}



	/**
	 * Gets a reader for an IMember's Javadoc comment content from the source attachment.
	 * and renders the tags in HTML.
	 * Returns <code>null</code> if the member does not contain a Javadoc comment or if no source is available.
	 *
	 * @param member The member to get the Javadoc of.
	 * @param allowInherited For methods with no (Javadoc) comment, the comment of the overridden class
	 * is returned if <code>allowInherited</code> is <code>true</code>.
	 * @return Returns a reader for the Javadoc comment content in HTML or <code>null</code> if the member
	 * does not contain a Javadoc comment or if no source is available
	 * @throws JavaModelException is thrown when the elements javadoc can not be accessed
	 * @deprecated As of 3.2, replaced by {@link #getHTMLContentReader(IMember, boolean, boolean)}
	 */
	@Deprecated
	public static Reader getHTMLContentReader(IMember member, boolean allowInherited) throws JavaModelException {
		return getHTMLContentReader(member, allowInherited, false);
	}
}
