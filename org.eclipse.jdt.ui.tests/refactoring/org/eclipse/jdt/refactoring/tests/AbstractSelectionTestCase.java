/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.refactoring.tests;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.code.ExtractMethodRefactoring;

import org.eclipse.jdt.refactoring.tests.infra.TestExceptionHandler;
import org.eclipse.jdt.refactoring.tests.infra.TextBufferChangeCreator;
import org.eclipse.jdt.testplugin.AbstractCUTestCase;
import org.eclipse.jdt.testplugin.JavaTestSetup;
import org.eclipse.jdt.testplugin.TestPluginLauncher;

public abstract class AbstractSelectionTestCase extends AbstractCUTestCase {

	private static final String SQUARE_BRACKET_OPEN= "/*[*/";
	private static final int    SQUARE_BRACKET_OPEN_LENGTH= SQUARE_BRACKET_OPEN.length();
	private static final String SQUARE_BRACKET_CLOSE=   "/*]*/";
	private static final int    SQUARE_BRACKET_CLOSE_LENGTH= SQUARE_BRACKET_CLOSE.length();
	
	public AbstractSelectionTestCase(String name) {
		super(name);
	}

	protected abstract String getResourceLocation();
			
	protected int[] getSelection(String source) {
		int start= -1;
		int end= -1;
		int includingStart= source.indexOf(SQUARE_BRACKET_OPEN);
		int excludingStart= source.indexOf(SQUARE_BRACKET_CLOSE);
		int includingEnd= source.lastIndexOf(SQUARE_BRACKET_CLOSE);
		int excludingEnd= source.lastIndexOf(SQUARE_BRACKET_OPEN);

		if (includingStart > excludingStart && excludingStart != -1) {
			includingStart= -1;
		} else if (excludingStart > includingStart && includingStart != -1) {
			excludingStart= -1;
		}
		
		if (includingEnd < excludingEnd) {
			includingEnd= -1;
		} else if (excludingEnd < includingEnd) {
			excludingEnd= -1;
		}
		
		if (includingStart != -1) {
			start= includingStart;
		} else {
			start= excludingStart + SQUARE_BRACKET_CLOSE_LENGTH;
		}
		
		if (excludingEnd != -1) {
			end= excludingEnd;
		} else {
			end= includingEnd + SQUARE_BRACKET_CLOSE_LENGTH;
		}
		
		assertTrue("Selection invalid", start >= 0 && end >= 0 && end >= start);
		
		int[] result= new int[] { start, end - start }; 
		// System.out.println("|"+ source.substring(result[0], result[0] + result[1]) + "|");
		return result;
	}
	
	protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, String id) throws Exception {
		return createCU(pack, createCUName(id), getFileContents(pack, id));
	}
	
	private String getTestFileName(String packageName, String id) {
		String result= getResourceLocation() + packageName + "/" + id + "_" + getName() + ".java";
		return result;
	}
	
	private String getFileContents(IPackageFragment pack, String id) throws IOException {
		return getFileContents(getFileInputStream(getTestFileName(pack.getElementName(), id)));
	}
	
	private InputStream getFileInputStream(String fileName) throws IOException {
		IPluginDescriptor plugin= Platform.getPluginRegistry().getPluginDescriptors("Refactoring Tests Resources")[0];
		URL url= new URL(plugin.getInstallURL().toString() + fileName);
		return url.openStream();
	}
	
	private String createCUName(String id) {
		return id + "_" + getName() + ".java";
	}	
}