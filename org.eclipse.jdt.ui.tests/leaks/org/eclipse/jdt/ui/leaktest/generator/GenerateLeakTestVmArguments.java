/*******************************************************************************
 * Copyright (c) 2026 Simeon Andreev and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simeon Andreev - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.leaktest.generator;

import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class GenerateLeakTestVmArguments {

	private static final String ADD_MODULES_ALL_SYSTEM= "--add-modules=ALL-SYSTEM";

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			throw new IllegalArgumentException("Expected exactly one argument: the output file");
		}

		Path output= Path.of(args[0]).toAbsolutePath();
		Path parent= output.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		Files.write(output, createVmArguments(), StandardCharsets.UTF_8);
	}

	static List<String> createVmArguments() {
		List<String> vmArguments= new ArrayList<>();
		vmArguments.add(ADD_MODULES_ALL_SYSTEM);
		ModuleFinder.ofSystem().findAll().stream()
				.sorted(Comparator.comparing(reference -> reference.descriptor().name()))
				.forEach(reference -> {
					String moduleName= reference.descriptor().name();
					reference.descriptor().packages().stream()
							.sorted()
							.map(packageName -> "--add-opens=" + moduleName + "/" + packageName + "=ALL-UNNAMED")
							.forEach(vmArguments::add);
				});
		return vmArguments;
	}

	private GenerateLeakTestVmArguments() {
	}
}
