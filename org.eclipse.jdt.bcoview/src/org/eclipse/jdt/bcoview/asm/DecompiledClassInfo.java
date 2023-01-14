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


public class DecompiledClassInfo {
	public final JavaVersion javaVersion;

	public final int accessFlags;

	public final int major;

	public DecompiledClassInfo(JavaVersion javaVersion, int accessFlags) {
		this.javaVersion = javaVersion;
		this.accessFlags = accessFlags;
		major = javaVersion.major;
	}
}
