package org.eclipse.jdt.internal.ui.text.javadoc;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import org.eclipse.jdt.internal.ui.codemanipulation.StubUtility;
import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.HashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.w3c.dom.range.Range;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;


public class StandardDocletPageBuffer {
	
	private static final String TYPE_BEG= "<!-- ======== START OF CLASS DATA ======== -->";
	private static final String TYPE_END= "<!-- ======== INNER CLASS SUMMARY ======== -->";
	private static final String MEMBER_BEG= "<!-- ============ FIELD DETAIL =========== -->";
	private static final String MEMBER_END= "<!-- ========= END OF CLASS DATA ========= -->";
	private static final String NAME_BEG= "<A NAME=\"";
	
	private URL fJDocLocation;
	
	private IType fInput;
			
	private char[] fContent;
	
	private Range fTypeRange;
	private HashMap fMemberRanges;

	public StandardDocletPageBuffer(IType input) throws CoreException {
		fInput= input;
		
		fContent= null;
		parse();
	}
	
	private void parse() throws CoreException {
		IPackageFragmentRoot root= JavaModelUtility.getPackageFragmentRoot(fInput);
		URL javaDocRoot= JavaDocAccess.getJavaDocLocation(root);
		if (javaDocRoot == null) {
			return;
		}
		try {
			String fileName= getHTMLFileName(fInput);
			URL pageURL= new URL(javaDocRoot, fileName);
			InputStream is= pageURL.openStream();
			BufferedReader rd= new BufferedReader(new InputStreamReader(is));
			
			findRanges(rd);
		} catch (IOException e) {
			throw new JavaModelException(e, IStatus.ERROR);
		}
	}
	
	private void findRanges(BufferedReader rd) throws IOException {
		String lineDelim= System.getProperty("line.separator", "\n");
		StringBuffer buf= new StringBuffer(10000);
		
		fTypeRange= findTypeRange(rd, buf, lineDelim);
		if (fTypeRange != null) {
			fMemberRanges= findMemberRanges(rd, buf, lineDelim);
			if (fMemberRanges != null) {
				fContent= new char[buf.length()];
				buf.getChars(0, buf.length(), fContent, 0);
			}
		}
	}
	
	private String readUpToLine(BufferedReader rd, String endLine, StringBuffer buf, String lineDelim) throws IOException {
		String line= rd.readLine();
		while (line != null && !endLine.equals(line)) {
			if (buf != null) {
				buf.append(line);
				buf.append(lineDelim);
			}
			line= rd.readLine();
		}
		return line;
	}
	
	private String readUpToLine(BufferedReader rd, String[] endLines, StringBuffer buf, String lineDelim) throws IOException {
		String line= rd.readLine();
		while (line != null) {
			for (int i= 0; i < endLines.length; i++) {
				if (endLines[i].equals(line)) {
					return line;
				}
			}
			if (buf != null) {
				buf.append(line);
				buf.append(lineDelim);
			}
			line= rd.readLine();
		}
		return line;
	}	
	
	
	private Range findTypeRange(BufferedReader rd, StringBuffer buf, String lineDelim) throws IOException {
		Range resRange= new Range();
		String line= readUpToLine(rd, TYPE_BEG, null, null);
		if (line != null) {
			line= readUpToLine(rd, new String[] { TYPE_END, "<P>" }, null, null);
			if ("<P>".equals(line)) {
				resRange.offset= buf.length();
				line= readUpToLine(rd, TYPE_END, buf, lineDelim);
				if (line != null) {
					resRange.length= buf.length() - resRange.offset;
					return resRange;
				}
			}
		}
		return null;
	}
				
	private HashMap findMemberRanges(BufferedReader rd, StringBuffer buf, String lineDelim) throws IOException {
		String line= readUpToLine(rd, MEMBER_BEG, null, null);
		if (line != null) {
			HashMap result= new HashMap();
			Range currRange= null;
			line= rd.readLine();
			while (line != null && !MEMBER_END.equals(line)) {
				if (line.startsWith(NAME_BEG)) {
					if (currRange != null) {
						currRange.length= buf.length() - currRange.offset;
					}
					currRange= new Range();
					currRange.offset= buf.length();
					String name= readSignature(line);					
					result.put(name, currRange);
					line= readUpToLine(rd, "<DL>", null, null);
				}
				buf.append(line);
				buf.append(lineDelim);
				line= rd.readLine();
			}
			if (line != null) {
				currRange.length= buf.length() - currRange.offset;
				return result;
			}
		}
		return null;
	}
	
	private String readSignature(String line) {
		int start= NAME_BEG.length();
		int end= line.indexOf('"', NAME_BEG.length());
		if (end != -1) {
			return line.substring(start, end);
		}
		return "";
	}
	
	private String getHTMLFileName(IType type) {
		StringBuffer buf= new StringBuffer();
		String packageName= type.getPackageFragment().getElementName();
		if (packageName.length() > 0) {
			buf.append(packageName.replace('.', '/'));
			buf.append('/');
		}
		buf.append(JavaModelUtility.getTypeQualifiedName(type));
		buf.append(".html");
		return buf.toString();
	}
	
	public Reader getJavaDoc(IMember member) throws JavaModelException {
		if (fContent == null) {
			return null;
		}
		if (fInput.equals(member)) {
			return getReaderForRange(fTypeRange);
		}
		String memberName= getMemberTagName(member);
		Range range= (Range)fMemberRanges.get(memberName);
		if (range != null) {
			return getReaderForRange(range);
		}
		return null;
	}
	
	
	
	private Reader getReaderForRange(Range range) {
		return new CharArrayReader(fContent, range.offset, range.length);
	}
	
	/*
	 * Gets the link-name of a member as used in the standard doclet
	 */
	private String getMemberTagName(IMember member) throws JavaModelException {
		StringBuffer buf= new StringBuffer();
		buf.append(member.getElementName());
		
		if (member instanceof IMethod) {
			String[] paramTypes= ((IMethod)member).getParameterTypes();
			buf.append('(');
			for (int i= 0; i < paramTypes.length; i++) {
				if (i != 0) {
					buf.append(", ");
				}
				String currType= paramTypes[i];
				int arrayCount= Signature.getArrayCount(currType);
				String fullTypeName= StubUtility.getResolvedTypeName(currType, member.getDeclaringType());
				if (fullTypeName == null) {
					// take the simple type name
					fullTypeName= Signature.toString(currType.substring(arrayCount));
				}
				buf.append(fullTypeName);
				for (int k= 0; k < arrayCount; k++) {
					buf.append("[]");
				}
			}
			buf.append(')');
		}
		return buf.toString();
	}
		
	public void printContent() {
		if (fContent != null) {
			System.out.println("Type description:");
			System.out.println(new String(fContent, fTypeRange.offset, fTypeRange.length));
			
			Object[] keys= fMemberRanges.keySet().toArray();
			for (int i= 0; i < keys.length; i++) {
				System.out.println("Method " + keys[i].toString() + ":");
				Range range= (Range)fMemberRanges.get(keys[i]);			
				System.out.println(new String(fContent, range.offset, range.length));
			}
		}
	}	
	
	private static class Range {
		public int offset;
		public int length;
	}
	
	
}