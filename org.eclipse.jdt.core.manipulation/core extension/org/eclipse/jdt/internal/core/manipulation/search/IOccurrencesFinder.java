/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.manipulation.search;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

public interface IOccurrencesFinder {

	int K_OCCURRENCE= 5;

	int K_EXCEPTION_OCCURRENCE= 6;
	int K_EXIT_POINT_OCCURRENCE= 7;
	int K_IMPLEMENTS_OCCURRENCE= 8;
	int K_BREAK_TARGET_OCCURRENCE= 9;

	int F_WRITE_OCCURRENCE= 1;
	int F_READ_OCCURRENCE= 2;
	int F_EXCEPTION_DECLARATION= 8;


	/**
	 * Element representing a occurrence
	 */
	public static class OccurrenceLocation {
		private final int fOffset;
		private final int fLength;
		private final int fFlags;
		private final String fDescription;

		public OccurrenceLocation(int offset, int length, int flags, String description) {
			fOffset= offset;
			fLength= length;
			fFlags= flags;
			fDescription= description;
		}

		public int getOffset() {
			return fOffset;
		}

		public int getLength() {
			return fLength;
		}

		public int getFlags() {
			return fFlags;
		}

		public String getDescription() {
			return fDescription;
		}

		@Override
		public String toString() {
			return "[" + fOffset + " / " + fLength + "] " + fDescription; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		}

	}


	String initialize(CompilationUnit root, int offset, int length);

	String initialize(CompilationUnit root, ASTNode node);

	String getJobLabel();

	/**
	 * Returns the plural label for this finder with 3 placeholders:
	 * <ul>
	 * <li>{0} for the {@link #getElementName() element name}</li>
	 * <li>{1} for the number of results found</li>
	 * <li>{2} for the scope (name of the compilation unit)</li>
	 *  </ul>
	 * @return the unformatted label
	 */
	String getUnformattedPluralLabel();

	/**
	 * Returns the singular label for this finder with 2 placeholders:
	 * <ul>
	 * <li>{0} for the {@link #getElementName() element name}</li>
	 * <li>{1} for the scope (name of the compilation unit)</li>
	 *  </ul>
	 * @return the unformatted label
	 */
	String getUnformattedSingularLabel();

	/**
	 * Returns the name of the element to look for or <code>null</code> if the finder hasn't
	 * been initialized yet.
	 * @return the name of the element
	 */
	String getElementName();


	/**
	 * Returns the AST root.
	 *
	 * @return the AST root
	 */
	CompilationUnit getASTRoot();

	/**
	 * Returns the occurrences
	 *
	 * @return the occurrences
	 */
	OccurrenceLocation[] getOccurrences();


	int getSearchKind();

	/**
	 * Returns the id of this finder.
	 * @return returns the id of this finder.
	 */
	String getID();

}
