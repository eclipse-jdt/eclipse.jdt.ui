/*******************************************************************************
 * Copyright (c) 2022 Red Hat and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

public class BindingsHierarchyTest {
	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();
	private IPackageFragment fPackage;

	@Before
	public void setUp() throws CoreException {
		IPackageFragmentRoot src= JavaProjectHelper.addSourceContainer(pts.getProject(), "src");
		fPackage= src.createPackageFragment("test1", false, new NullProgressMonitor());
	}

	@After
	public void tearDown() throws Exception {
			JavaProjectHelper.clear(pts.getProject(), pts.getDefaultClasspath());
	}

	@Test
	public void testWalkSuperclassInterface() throws JavaModelException {
		String source= """
			package test1
			interface I1 {
			
			}
			
			interface I2 {
			
			}
			
			class Test1 implements I1 {
			
			}
			
			class Test2 extends Test1 implements I2 {
			
			}""";

		CompilationUnit ast= createAST(fPackage.createCompilationUnit("Test1.java", source, false, new NullProgressMonitor()));

		TypeDeclaration typeDeclaration= (TypeDeclaration) ast.types().get(3);
		ITypeBinding typeBinding= typeDeclaration.resolveBinding();

		Set<String> superTypeBindings= new HashSet<>();

		Bindings.visitHierarchy(typeBinding, type -> {
			superTypeBindings.add(type.getName());
			return true;
		});

		assertEquals(4, superTypeBindings.size());
		assertTrue(superTypeBindings.containsAll(Arrays.asList("Test1", "Object", "I1", "I2")));
	}

	private CompilationUnit createAST(ICompilationUnit compilationUnit) {
		ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setSource(compilationUnit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null);
	}
}



