/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.activation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

public class JavaActivationTest extends TestCase {
	
	private IJavaProject project;

	protected void setUp() throws Exception {
		project= JavaProjectHelper.createJavaProject("TestProject1", "bin");
	}

	protected void tearDown() throws Exception {
	    getPage().closeAllEditors(false);
		JavaProjectHelper.delete(project);
	}

	private IWorkbenchPage getPage() {
	    IWorkbench workbench= PlatformUI.getWorkbench();
	    return workbench.getActiveWorkbenchWindow().getActivePage();
	}
	
	private ICompilationUnit createTestCU() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(project, "src");
		IPackageFragment pack= sourceFolder.createPackageFragment("pack0", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack0;\n");
		buf.append("public class List1 {\n}\n");
		return pack.createCompilationUnit("List1.java", buf.toString(), false, null);
	}
	
	public void testOpenJavaEditor() throws Exception {
		String[] expectedActivations= {
		        // activated since running tests
		        "org.eclipse.jdt.junit.runtime", 
		        "org.eclipse.pde.junit.runtime",
		        "org.eclipse.jdt.ui.tests", 
		        "org.junit", 
		        
		        // expected activations
		        "org.osgi.framework",
		        "org.eclipse.osgi.services",
		        "org.eclipse.osgi.util",
		        "org.eclipse.core.runtime",
		        "org.eclipse.update.configurator",
		        "org.eclipse.compare",
		        "org.eclipse.core.filebuffers",
		        "org.eclipse.core.resources",
		        "org.eclipse.core.runtime.compatibility",
		        "org.eclipse.debug.core",
		        "org.eclipse.debug.ui", // ???
		        "org.eclipse.help",
		        "org.eclipse.jdt.core",
		        "org.eclipse.jdt.ui",
		        "org.eclipse.jface",
		        "org.eclipse.jface.text",
		        "org.eclipse.swt",
		        "org.eclipse.team.core",
		        "org.eclipse.text",
		        "org.eclipse.ui",
		        "org.eclipse.ui.editors",
		        "org.eclipse.ui.externaltools", // ???
		        "org.eclipse.ui.ide",
		        "org.eclipse.ui.views",
		        "org.eclipse.ui.workbench",
		        "org.eclipse.ui.workbench.texteditor",
		        "org.eclipse.update.core",
		        "org.eclipse.update.scheduler" // ???
		};
		ICompilationUnit unit= createTestCU();
		EditorUtility.openInEditor(unit);
		IPluginDescriptor[] descriptors = Platform.getPluginRegistry().getPluginDescriptors();
		
		Set set= new HashSet();
		set.addAll(Arrays.asList(expectedActivations));
		
		for (int i = 0; i < descriptors.length; i++) {
		    IPluginDescriptor descriptor= descriptors[i];
		    String uniqueIdentifier= descriptor.getUniqueIdentifier();
		    if (descriptor.isPluginActivated())
		        assertTrue ("plugin should not be activated: "+uniqueIdentifier,  set.contains(uniqueIdentifier)) ;
		}
	}
}
