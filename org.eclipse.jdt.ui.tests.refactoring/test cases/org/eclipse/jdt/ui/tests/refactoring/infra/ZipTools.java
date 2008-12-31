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
package org.eclipse.jdt.ui.tests.refactoring.infra;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import junit.framework.Assert;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;

public class ZipTools {

	private static String getSourceFileName(String fileName) {
		if (fileName.lastIndexOf('/') == -1){
			return fileName;
		} else {
			return fileName.substring(fileName.lastIndexOf('/')+1);
		}
	}

	private static String getPackageName(String fileName) {
		String packageName= null;
		if (fileName.lastIndexOf('/') == -1){
			packageName= "";
		} else {
			packageName= fileName.substring(0, fileName.lastIndexOf('/')).replace('/', '.');
		}
		return packageName;
	}

	public static void compareWithZipped(IPackageFragmentRoot src, ZipInputStream zipInputStream, String zipFileEncoding) throws IOException, UnsupportedEncodingException, JavaModelException {
		try {
			ArrayList zipCus= new ArrayList();
			ZipEntry ze;
			while ((ze= zipInputStream.getNextEntry()) != null){
				String fileName = ze.getName();
				if (fileName.endsWith(".java")){
					String packageName = getPackageName(fileName);
					String sourceFileName= getSourceFileName(fileName);
					zipCus.add(packageName + "/" + sourceFileName);

					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					byte data[] = new byte[10000];
					int count = -1;
					while ( (count = zipInputStream.read(data, 0, data.length)) != -1) {
						bout.write(data, 0, count);
					}
			        String zipContents= bout.toString(zipFileEncoding);

					IPackageFragment pack= src.getPackageFragment(packageName);
					ICompilationUnit cu= pack.getCompilationUnit(sourceFileName);
					String cuContents= cu.getSource();

					RefactoringTest.assertEqualLines(packageName + "/" + sourceFileName, zipContents, cuContents);
				} else {
					//TODO: compare binary files
				}
			}

			IJavaElement[] packageFragments= src.getChildren();
			for (int i= 0; i < packageFragments.length; i++) {
				IPackageFragment packageFragment= (IPackageFragment) packageFragments[i];
				ICompilationUnit[] cus= packageFragment.getCompilationUnits();
				for (int j= 0; j < cus.length; j++) {
					ICompilationUnit cu= cus[j];
					String cuDescr= packageFragment.getElementName() + "/" + cu.getElementName();
					Assert.assertTrue(cuDescr, zipCus.remove(cuDescr));
				}
			}
			Assert.assertEquals(zipCus.toString(), 0, zipCus.size());

		} finally {
			zipInputStream.close();
		}
	}

}
