/*******************************************************************************
 * Copyright (c) 2025, Andrey Loskutov (loskutov@gmx.de) and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov (loskutov@gmx.de) - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.editor;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;

import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.internal.IWorkbenchConstants;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditorInputFactory;
import org.eclipse.jdt.internal.ui.javaeditor.HandleEditorInput;
import org.eclipse.jdt.internal.ui.javaeditor.InternalClassFileEditorInput;

public class ClassFileInputTests {

	@SuppressWarnings("restriction")
	private static final String TAG_FACTORY_ID= IWorkbenchConstants.TAG_FACTORY_ID;
	private static final String TYPE_NAME= "HelloWorld";

	private String fileName;
	private String className;
	private IJavaProject javaProject ;

	private ClassFileEditorInputFactory factory;

	@Before
	public void setUp() throws Exception {
		factory = new ClassFileEditorInputFactory();
		fileName= TYPE_NAME + ".java";
		className= TYPE_NAME + ".class";
		javaProject = setUpProject();
	}

	@After
	public void tearDown() throws Exception {
		if (javaProject != null) {
			JavaProjectHelper.delete(javaProject);
		}
	}

	private IJavaProject setUpProject() throws Exception {
		javaProject= JavaProjectHelper.createJavaProject(ClassFileInputTests.class.getSimpleName(), "bin");
		JavaProjectHelper.addSourceContainer(javaProject, "src");
		IPackageFragment fragment= javaProject.findPackageFragment(javaProject.getProject().getFullPath().append("src"));
		String content= """
			public class HelloWorld {
			}
		""";
		fragment.createCompilationUnit(fileName, content, true, new NullProgressMonitor());
		javaProject.getProject().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		return javaProject;
	}

	@Test
	public void testClassFileInputPersistence() throws Exception {
		IProject project= javaProject.getProject();
		IFile file= project.getFile("bin/" + className);
		IClassFile classFile= JavaCore.createClassFileFrom(file);
		InternalClassFileEditorInput input= new InternalClassFileEditorInput(classFile);
		XMLMemento memento = createMemento(input.getFactoryId(), input);

		HandleEditorInput handle= (HandleEditorInput) factory.createElement(memento);
		assertEquals(input.getName(), handle.getName());
		assertEquals(input.getClassFile().getHandleIdentifier(), handle.getHandleIdentifier());
		assertEquals(input.getClassFile(), handle.getElement());
		assertNotNull(handle.getToolTipText());
		assertNotNull(handle.getImageDescriptor());

		XMLMemento mementoFromHandle =createMemento(handle.getFactoryId(), handle);
		assertEquals(memento.toString(), mementoFromHandle.toString());

		HandleEditorInput handle2= (HandleEditorInput) factory.createElement(mementoFromHandle);
		assertEquals(handle, handle2);
		assertEquals(handle.hashCode(), handle2.hashCode());

		mementoFromHandle =createMemento(handle.getFactoryId(), handle.getPersistable());
		assertEquals(memento.toString(), mementoFromHandle.toString());
	}

	static XMLMemento createMemento(String factoryId, IPersistableElement elt) {
		XMLMemento memento = XMLMemento.createWriteRoot("dummy");
		memento.putString(TAG_FACTORY_ID, factoryId);
		elt.saveState(memento);
		return memento;
	}
}
