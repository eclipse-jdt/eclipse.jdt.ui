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

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IOrdinaryClassFile;
import org.eclipse.jdt.core.IType;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class HelloWorld extends TestCase {

	private IJavaProject fJProject;


	public static Test suite() {
		TestSuite suite= new TestSuite();
		suite.addTest(new HelloWorld("test1"));
		return suite;
	}

	public HelloWorld(String name) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
			fJProject= JavaProjectHelper.createJavaProject("TestProject1", "bin");
	}


	@Override
	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject);
	}

	public void test1() throws Exception {
		if (JavaProjectHelper.addRTJar(fJProject) == null) {
			assertTrue("jdk not found", false);
			return;
		}

		String name= "java/util/Vector.java";
		IOrdinaryClassFile classfile= (IOrdinaryClassFile) fJProject.findElement(new Path(name));
		assertTrue("classfile not found", classfile != null);

		IType type= classfile.getType();
		System.out.println("methods of Vector");
		IMethod[] methods= type.getMethods();
		for (int i= 0; i < methods.length; i++) {
			System.out.println(methods[i].getElementName());
		}
	}

}
