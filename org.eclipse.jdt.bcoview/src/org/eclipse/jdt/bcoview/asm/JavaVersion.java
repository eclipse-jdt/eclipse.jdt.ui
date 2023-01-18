/*******************************************************************************
 * Copyright (c) 2023 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.bcoview.asm;

public class JavaVersion {

	private final int version;

	public final int major;

	public final int minor;

	public JavaVersion(int version) {
		this.version = version;
		major = version & 0xFFFF;
		minor = version >>> 16;
	}

	public String humanReadable() {
		// 1.1 is 45, 1.2 is 46 etc.
		int javaV = major % 44;
		String javaVersion;
		if (javaV > 0) {
			if (javaV > 8) {
				javaVersion = javaV + "." + minor; //$NON-NLS-1$
			} else {
				javaVersion = "1." + javaV; //$NON-NLS-1$
			}
		} else {
			javaVersion = "? " + major; //$NON-NLS-1$
		}
		return javaVersion;
	}

	@Override
	public String toString() {
		return version + " : " + humanReadable(); //$NON-NLS-1$
	}
}
