/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on Apr 24, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.eclipse.jsp.core.search;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;

import javax.servlet.jsp.tagext.TagInfo;
import javax.servlet.jsp.tagext.TagLibraryInfo;

import org.apache.jasper.CommandLineContext;
import org.apache.jasper.JasperException;
import org.apache.jasper.JspCompilationContext;
import org.apache.jasper.Options;
import org.apache.jasper.compiler.*;
import org.apache.jasper.compiler.Compiler;
import org.xml.sax.Attributes;

/**
 * @author weinand
 */
public class Search {
	
	static JspCompilationContext context= new JspCompilationContext() {
		public String getClassPath() {
			// TODO Auto-generated method stub
			return null;
		}

		public JspReader getReader() {
			// TODO Auto-generated method stub
			return null;
		}

		public ServletWriter getWriter() {
			// TODO Auto-generated method stub
			return null;
		}

		public ClassLoader getClassLoader() {
			// TODO Auto-generated method stub
			return null;
		}

		public boolean isErrorPage() {
			// TODO Auto-generated method stub
			return false;
		}

		public String getOutputDir() {
			// TODO Auto-generated method stub
			return null;
		}

		public String getJavacOutputDir() {
			// TODO Auto-generated method stub
			return null;
		}

		public String getJspFile() {
			// TODO Auto-generated method stub
			return null;
		}

		public String getServletClassName() {
			// TODO Auto-generated method stub
			return null;
		}

		public String getServletPackageName() {
			// TODO Auto-generated method stub
			return null;
		}

		public String getServletJavaFileName() {
			// TODO Auto-generated method stub
			return null;
		}

		public boolean keepGenerated() {
			// TODO Auto-generated method stub
			return false;
		}

		public String getContentType() {
			// TODO Auto-generated method stub
			return null;
		}

		public Options getOptions() {
			// TODO Auto-generated method stub
			return null;
		}

		public void setContentType(String arg0) {
			// TODO Auto-generated method stub

		}

		public void setReader(JspReader arg0) {
			// TODO Auto-generated method stub

		}

		public void setWriter(ServletWriter arg0) {
			// TODO Auto-generated method stub

		}

		public void setServletClassName(String arg0) {
			// TODO Auto-generated method stub

		}

		public void setServletPackageName(String arg0) {
			// TODO Auto-generated method stub

		}

		public void setServletJavaFileName(String arg0) {
			// TODO Auto-generated method stub

		}

		public void setErrorPage(boolean arg0) {
			// TODO Auto-generated method stub

		}

		public Compiler createCompiler() throws JasperException {
			// TODO Auto-generated method stub
			return null;
		}

		public String resolveRelativeUri(String arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		public InputStream getResourceAsStream(String arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		public URL getResource(String arg0) throws MalformedURLException {
			// TODO Auto-generated method stub
			return null;
		}

		public String getRealPath(String arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		public String[] getTldLocation(String arg0) throws JasperException {
			// TODO Auto-generated method stub
			return null;
		}
	};
	
	static ParseEventListener pel= new ParseEventListener() {
		public void setReader(JspReader arg0) {
			// TODO Auto-generated method stub

		}

		public void setDefault(boolean arg0) {
			// TODO Auto-generated method stub

		}

		public void setTemplateInfo(Mark arg0, Mark arg1) {
			// TODO Auto-generated method stub

		}

		public void beginPageProcessing() throws JasperException {
			// TODO Auto-generated method stub

		}

		public void handleComment(Mark arg0, Mark arg1, char[] arg2)
			throws JasperException {
			// TODO Auto-generated method stub

		}

		public void handleDirective(
			String arg0,
			Mark arg1,
			Mark arg2,
			Attributes arg3)
			throws JasperException {
			// TODO Auto-generated method stub

		}

		public void handleDeclaration(
			Mark arg0,
			Mark arg1,
			Attributes arg2,
			char[] arg3)
			throws JasperException {
			// TODO Auto-generated method stub

		}

		public void handleScriptlet(
			Mark arg0,
			Mark arg1,
			Attributes arg2,
			char[] arg3)
			throws JasperException {
			// TODO Auto-generated method stub

		}

		public void handleExpression(
			Mark arg0,
			Mark arg1,
			Attributes arg2,
			char[] arg3)
			throws JasperException {
			// TODO Auto-generated method stub

		}

		public void handleBean(Mark arg0, Mark arg1, Attributes arg2)
			throws JasperException {
			// TODO Auto-generated method stub

		}

		public void handleBean(
			Mark arg0,
			Mark arg1,
			Attributes arg2,
			boolean arg3)
			throws JasperException {
			// TODO Auto-generated method stub

		}

		public void handleBeanEnd(Mark arg0, Mark arg1, Attributes arg2)
			throws JasperException {
			// TODO Auto-generated method stub

		}

		public void handleGetProperty(Mark arg0, Mark arg1, Attributes arg2)
			throws JasperException {
			// TODO Auto-generated method stub

		}

		public void handleSetProperty(Mark arg0, Mark arg1, Attributes arg2)
			throws JasperException {
			// TODO Auto-generated method stub

		}

		public void handleSetProperty(
			Mark arg0,
			Mark arg1,
			Attributes arg2,
			boolean arg3)
			throws JasperException {
			// TODO Auto-generated method stub

		}

		public void handlePlugin(
			Mark arg0,
			Mark arg1,
			Attributes arg2,
			Hashtable arg3,
			String arg4)
			throws JasperException {
			// TODO Auto-generated method stub

		}

		public void handlePlugin(
			Mark arg0,
			Mark arg1,
			Attributes arg2,
			Hashtable arg3,
			String arg4,
			boolean arg5)
			throws JasperException {
			// TODO Auto-generated method stub

		}

		public void handleCharData(Mark arg0, Mark arg1, char[] arg2)
			throws JasperException {
			// TODO Auto-generated method stub

		}

		public TagLibraries getTagLibraries() {
			// TODO Auto-generated method stub
			return null;
		}

		public void handleTagBegin(
			Mark arg0,
			Mark arg1,
			Attributes arg2,
			String arg3,
			String arg4,
			TagLibraryInfo arg5,
			TagInfo arg6,
			boolean arg7)
			throws JasperException {
			// TODO Auto-generated method stub

		}

		public void handleTagBegin(
			Mark arg0,
			Mark arg1,
			Attributes arg2,
			String arg3,
			String arg4,
			TagLibraryInfo arg5,
			TagInfo arg6,
			boolean arg7,
			boolean arg8)
			throws JasperException {
			// TODO Auto-generated method stub

		}

		public void handleTagEnd(
			Mark arg0,
			Mark arg1,
			String arg2,
			String arg3,
			Attributes arg4,
			TagLibraryInfo arg5,
			TagInfo arg6,
			boolean arg7)
			throws JasperException {
			// TODO Auto-generated method stub

		}

		public void handleForward(
			Mark arg0,
			Mark arg1,
			Attributes arg2,
			Hashtable arg3)
			throws JasperException {
			// TODO Auto-generated method stub

		}

		public void handleForward(
			Mark arg0,
			Mark arg1,
			Attributes arg2,
			Hashtable arg3,
			boolean arg4)
			throws JasperException {
			// TODO Auto-generated method stub

		}

		public void handleInclude(
			Mark arg0,
			Mark arg1,
			Attributes arg2,
			Hashtable arg3)
			throws JasperException {
			// TODO Auto-generated method stub

		}

		public void handleInclude(
			Mark arg0,
			Mark arg1,
			Attributes arg2,
			Hashtable arg3,
			boolean arg4)
			throws JasperException {
			// TODO Auto-generated method stub

		}

		public void endPageProcessing() throws JasperException {
			// TODO Auto-generated method stub

		}

		public void handleRootBegin(Attributes arg0) throws JasperException {
			// TODO Auto-generated method stub

		}

		public void handleRootEnd() {
			// TODO Auto-generated method stub

		}

		public void handleUninterpretedTagBegin(
			Mark arg0,
			Mark arg1,
			String arg2,
			Attributes arg3)
			throws JasperException {
			// TODO Auto-generated method stub

		}

		public void handleUninterpretedTagEnd(
			Mark arg0,
			Mark arg1,
			String arg2,
			char[] arg3)
			throws JasperException {
			// TODO Auto-generated method stub

		}

		public void handleJspCdata(Mark arg0, Mark arg1, char[] arg2)
			throws JasperException {
			// TODO Auto-generated method stub

		}
	};

	public static void main(String[] args) {
				
		String file= null;
		String encoding= null;
		InputStreamReader reader= null;
		
		try {
			Parser p= new Parser(context, file, encoding, reader, pel);
			p.parse();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JasperException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
