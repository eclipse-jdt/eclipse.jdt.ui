/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.jdt.testplugin.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IOrdinaryClassFile;
import org.eclipse.jdt.core.IType;

public class HelloWorld {
	private IJavaProject fJProject;

	@BeforeEach
	public void setUp() throws Exception {
			fJProject= JavaProjectHelper.createJavaProject("TestProject1", "bin");
	}

	@AfterEach
	public void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject);
	}

	@Test
	public void test1() throws Exception {
		if (JavaProjectHelper.addRTJar(fJProject) == null) {
			fail("jdk not found");
			return;
		}

		String name= "java/util/Vector.java";
		IOrdinaryClassFile classfile= (IOrdinaryClassFile) fJProject.findElement(new Path(name));
		assertNotNull(classfile, "classfile not found");

		IType type= classfile.getType();
		System.out.println("methods of Vector");
		for (IMethod method : type.getMethods()) {
			System.out.println(method.getElementName());
		}
	}
}
