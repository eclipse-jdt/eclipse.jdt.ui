/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.reorg;

import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenameVirtualMethodProcessor;


public class AbstractRenameMethodPerfTest extends RepeatingRefactoringPerformanceTestCase {

	public AbstractRenameMethodPerfTest(String name) {
		super(name);
	}

	protected void doExecuteRefactoring(int numberOfCus, int numberOfRefs, boolean measure) throws Exception {
		ICompilationUnit cunit= generateSources(numberOfCus, numberOfRefs);
		IMethod method= cunit.findPrimaryType().getMethod("foo", new String[0]);
		RenameVirtualMethodProcessor processor= new RenameVirtualMethodProcessor(method);
		processor.setNewElementName("foo2");
		executeRefactoring(new RenameRefactoring(processor), measure);
	}

	private ICompilationUnit generateSources(int numberOfCus, int numberOfRefs) throws Exception {
		IPackageFragment definition= fTestProject.getSourceFolder().createPackageFragment("def", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package def;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit result= definition.createCompilationUnit("A.java", buf.toString(), false, null);

		IPackageFragment references= fTestProject.getSourceFolder().createPackageFragment("ref", false, null);
		for(int i= 0; i < numberOfCus; i++) {
			createReferenceCu(references, i, numberOfRefs);
		}
		return result;
	}

	private void createReferenceCu(IPackageFragment pack, int index, int numberOfRefs) throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package " + pack.getElementName() + ";\n");
		buf.append("import def.A;\n");
		buf.append("public class Ref" + index + " {\n");
		buf.append("    public void ref(A a) {\n");
		for (int i= 0; i < numberOfRefs; i++) {
			buf.append("        a.foo();\n");
		}
		buf.append("    }\n");
		buf.append("}\n");
		pack.createCompilationUnit("Ref" + index + ".java", buf.toString(), false, null);
	}
}
