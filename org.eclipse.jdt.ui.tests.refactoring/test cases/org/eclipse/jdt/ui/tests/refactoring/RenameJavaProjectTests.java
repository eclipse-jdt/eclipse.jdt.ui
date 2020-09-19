/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.Path;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;

import org.eclipse.jdt.internal.core.refactoring.descriptors.RefactoringSignatureDescriptorFactory;

import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

public class RenameJavaProjectTests extends GenericRefactoringTest {

	public RenameJavaProjectTests() {
		rts= new RefactoringTestSetup();
	}

	@Override
	public void genericbefore() throws Exception {
		super.genericbefore();
		fIsPreDeltaTest= true;
	}

	@Test
	public void test0() throws Exception {
		IJavaProject p1= null;
		IJavaProject referencing1= null;
		IJavaProject referencing2= null;
		try {
			ParticipantTesting.reset();
			String newProjectName= "newName";
			p1= JavaProjectHelper.createJavaProject("p1", "bin");
			referencing1= JavaProjectHelper.createJavaProject("p2", "bin");
			referencing2= JavaProjectHelper.createJavaProject("p3", "bin");

			JavaProjectHelper.addRTJar(referencing1);
			JavaProjectHelper.addRequiredProject(referencing1, p1);
			JavaProjectHelper.addSourceContainer(referencing1, "src");

			JavaProjectHelper.addRTJar(referencing2);
			JavaProjectHelper.addRequiredProject(referencing2, p1);
			JavaProjectHelper.addSourceContainer(referencing2, "src");

			JavaProjectHelper.addRTJar(p1);

			ParticipantTesting.reset();
			String[] handles= ParticipantTesting.createHandles(p1, p1.getResource());

			RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_JAVA_PROJECT);
			descriptor.setJavaElement(p1);
			descriptor.setUpdateReferences(true);
			descriptor.setNewName(newProjectName);

			RenameRefactoring ref= (RenameRefactoring) createRefactoring(descriptor);
			assertTrue(ref.isApplicable());

			RefactoringStatus result= performRefactoring(ref);

			assertNull("not expected to fail", result);
			assertFalse("p1 is gone", p1.exists());

			ParticipantTesting.testRename(handles,
				new RenameArguments[] {
					new RenameArguments(newProjectName, true),
					new RenameArguments(newProjectName, true)});

			p1= referencing1.getJavaModel().getJavaProject(newProjectName);
			assertTrue("p1 exists", p1.exists());

			//check entries in  referencing1
			IClasspathEntry[] entries= referencing1.getRawClasspath();
			assertEquals("expected entries", 3, entries.length);
			for (int i= 0; i < entries.length; i++) {
				IClasspathEntry iClassPathEntry= entries[i];
				if (i == 1) {
					assertEquals("expected entry name", p1.getProject().getFullPath(), iClassPathEntry.getPath());
					assertEquals("expected entry kind", IClasspathEntry.CPE_PROJECT, iClassPathEntry.getEntryKind());
				}
			}

			//check entries in  referencing2
			entries= referencing2.getRawClasspath();
			assertEquals("expected entries", 3, entries.length);
			for (int i= 0; i < entries.length; i++) {
				IClasspathEntry iClassPathEntry= entries[i];
				if (i == 1) {
					assertEquals("expected entry name", p1.getProject().getFullPath(), iClassPathEntry.getPath());
					assertEquals("expected entry kind", IClasspathEntry.CPE_PROJECT, iClassPathEntry.getEntryKind());
				}
			}

		} finally {
			performDummySearch();

			if (referencing1 != null && referencing1.exists())
				JavaProjectHelper.removeSourceContainer(referencing1, "src");
			if (referencing2 != null && referencing2.exists())
				JavaProjectHelper.removeSourceContainer(referencing2, "src");

			if (p1 != null && p1.exists())
				JavaProjectHelper.delete(p1);
			if (referencing1 != null && referencing1.exists())
				JavaProjectHelper.delete(referencing1);
			if (referencing2 != null && referencing2.exists())
				JavaProjectHelper.delete(referencing2);
		}
	}

	@Test
	public void test1() throws Exception {
		IJavaProject p1= null;
		IJavaProject referencing1= null;
		IJavaProject referencing2= null;
		try {
			ParticipantTesting.reset();
			String newProjectName= "newName";
			p1= JavaProjectHelper.createJavaProject("p1", "bin");
			referencing1= JavaProjectHelper.createJavaProject("p2", "bin");
			referencing2= JavaProjectHelper.createJavaProject("p3", "bin");

			JavaProjectHelper.addRTJar(referencing1);
			IClasspathEntry cpe1= JavaCore.newProjectEntry(p1.getProject().getFullPath(), true);
			JavaProjectHelper.addToClasspath(referencing1, cpe1);
			JavaProjectHelper.addSourceContainer(referencing1, "src");

			JavaProjectHelper.addRTJar(referencing2);
			IAccessRule[] accessRules2= new IAccessRule[] {
					JavaCore.newAccessRule(new Path("accessible"), IAccessRule.K_ACCESSIBLE)
			};
			IClasspathAttribute[] extraAttributes2= new IClasspathAttribute[] {
				JavaCore.newClasspathAttribute("myTestAttribute", "val")
			};
			IClasspathEntry cpe2= JavaCore.newProjectEntry(p1.getProject().getFullPath(), accessRules2, true, extraAttributes2, false);
			JavaProjectHelper.addToClasspath(referencing2, cpe2);
			JavaProjectHelper.addSourceContainer(referencing2, "src");

			JavaProjectHelper.addRTJar(p1);

			ParticipantTesting.reset();
			String[] handles= ParticipantTesting.createHandles(p1, p1.getResource());

			RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_JAVA_PROJECT);
			descriptor.setJavaElement(p1);
			descriptor.setUpdateReferences(true);
			descriptor.setNewName(newProjectName);

			RenameRefactoring ref= (RenameRefactoring) createRefactoring(descriptor);
			assertTrue(ref.isApplicable());

			RefactoringStatus result= performRefactoring(ref);

			assertNull("not expected to fail", result);
			assertFalse("p1 is gone", p1.exists());

			ParticipantTesting.testRename(handles,
				new RenameArguments[] {
					new RenameArguments(newProjectName, true),
					new RenameArguments(newProjectName, true)});

			p1= referencing1.getJavaModel().getJavaProject(newProjectName);
			assertTrue("p1 exists", p1.exists());

			//check entries in  referencing1
			IClasspathEntry[] entries= referencing1.getRawClasspath();
			assertEquals("expected entries", 3, entries.length);
			IClasspathEntry iClassPathEntry= entries[1];
			assertEquals("expected entry name", p1.getProject().getFullPath(), iClassPathEntry.getPath());
			assertEquals("expected entry kind", IClasspathEntry.CPE_PROJECT, iClassPathEntry.getEntryKind());
			assertTrue("expected entry isExported", iClassPathEntry.isExported());
			assertTrue("expected entry combineAccessRules", iClassPathEntry.combineAccessRules());
			assertEquals("expected entry accessRules count", 0, iClassPathEntry.getAccessRules().length);
			assertEquals("expected entry accessRules getExtraAttributes", 0, iClassPathEntry.getExtraAttributes().length);

			//check entries in  referencing2
			entries= referencing2.getRawClasspath();
			assertEquals("expected entries", 3, entries.length);
			iClassPathEntry= entries[1];
			assertEquals("expected entry name", p1.getProject().getFullPath(), iClassPathEntry.getPath());
			assertEquals("expected entry kind", IClasspathEntry.CPE_PROJECT, iClassPathEntry.getEntryKind());
			assertFalse("expected entry isExported", iClassPathEntry.isExported());
			assertTrue("expected entry combineAccessRules", iClassPathEntry.combineAccessRules());
			assertEquals("expected entry accessRules count", accessRules2.length, iClassPathEntry.getAccessRules().length);
			assertEquals("expected entry accessRules getExtraAttributes", extraAttributes2.length, iClassPathEntry.getExtraAttributes().length);

		} finally {
			performDummySearch();

			if (p1 != null && p1.exists())
				JavaProjectHelper.delete(p1);
			if (referencing1 != null && referencing1.exists())
				JavaProjectHelper.delete(referencing1);
			if (referencing2 != null && referencing2.exists())
				JavaProjectHelper.delete(referencing2);
		}
	}
}
