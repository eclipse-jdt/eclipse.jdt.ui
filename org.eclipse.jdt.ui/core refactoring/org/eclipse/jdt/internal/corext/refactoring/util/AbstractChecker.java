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
 *     Xiaye Chi <xychichina@gmail.com> - [extract local] Improve the Safety by identifying statements that may change the value of the extracted expressions - https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/432
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.util;

public class AbstractChecker {
	class Position {
		int start;

		int length;

		public Position(int start, int length) {
			this.start= start;
			this.length= length;
		}

		@Override
		public int hashCode() {
			final int prime= 31;
			int result= 1;
			result= prime * result + length;
			result= prime * result + start;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof Position)) {
				return false;
			}
			Position other= (Position) obj;
			if (length != other.length) {
				return false;
			}
			if (start != other.start) {
				return false;
			}
			return true;
		}

	}
}
