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

import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenamePackageProcessor;

public class AbstractRenamePackagePerfTest extends RepeatingRefactoringPerformanceTestCase {

	public AbstractRenamePackagePerfTest(String name) {
		super(name);
	}

	protected void doExecuteRefactoring(int numberOfCus, int numberOfRefs, boolean measure) throws Exception {
		IPackageFragment pack= generateSources(numberOfCus, numberOfRefs);
		RenamePackageProcessor processor= new RenamePackageProcessor(pack);
		processor.setNewElementName("pack2");
		executeRefactoring(new RenameRefactoring(processor), measure);
	}

	private IPackageFragment generateSources(int numberOfCus, int numberOfRefs) throws Exception {
		IPackageFragment pack= fTestProject.getSourceFolder().createPackageFragment("pack", false, null);
		for (int i= 0; i < numberOfRefs; i++) {
			StringBuffer buf= new StringBuffer();
			buf.append("package pack;\n");
			buf.append("public class A" + i + " {\n");
			buf.append("}\n");
			pack.createCompilationUnit("A" + i + ".java", buf.toString(), false, null);
		}

		IPackageFragment references= fTestProject.getSourceFolder().createPackageFragment("ref", false, null);
		for(int i= 0; i < numberOfCus; i++) {
			createReferenceCu(references, i, numberOfRefs);
		}
		return pack;
	}

	private static void createReferenceCu(IPackageFragment pack, int index, int numberOfRefs) throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package " + pack.getElementName() + ";\n");
		for (int i= 0; i < numberOfRefs; i++) {
			buf.append("import pack.A" + i + ";\n");
		}
		buf.append("public class Ref" + index + " {\n");
		buf.append("}\n");
		pack.createCompilationUnit("Ref" + index + ".java", buf.toString(), false, null);
	}

}
