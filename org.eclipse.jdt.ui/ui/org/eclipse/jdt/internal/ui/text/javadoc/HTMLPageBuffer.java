package org.eclipse.jdt.internal.ui.text.javadoc;
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
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;


public class HTMLPageBuffer {
	
	private static final String TYPE_BEG= "<!-- ======== START OF CLASS DATA ======== -->";
	private static final String TYPE_END= "<!-- ======== INNER CLASS SUMMARY ======== -->";
	private static final String MEMBER_BEG= "<!-- ============ FIELD DETAIL =========== -->";
	private static final String MEMBER_END= "<!-- ========= END OF CLASS DATA ========= -->";
	private static final String NAME_BEG= "<A NAME=\"";
	
	private IType fInput;
	
	private char[] fContent;
	
	private Range fTypeRange;
	private HashMap fMemberRanges;


	public HTMLPageBuffer(IType type) throws CoreException {
		fInput= type;
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
	
	private Range findTypeRange(BufferedReader rd, StringBuffer buf, String lineDelim) throws IOException {
		Range resRange= new Range();
		String line= rd.readLine();
		while (line != null && !TYPE_BEG.equals(line)) {
			line= rd.readLine();
		}
		if (line != null) {
			resRange.start= buf.length();
			line= rd.readLine();
			while (line != null && !TYPE_END.equals(line)) {
				buf.append(line);
				buf.append(lineDelim);
				line= rd.readLine();
			}
			if (line != null) {
				resRange.end= buf.length();
				return resRange;
			}
		}
		return null;
	}
				
	private HashMap findMemberRanges(BufferedReader rd, StringBuffer buf, String lineDelim) throws IOException {
		String line= rd.readLine();
		while (line != null && !MEMBER_BEG.equals(line)) {
			line= rd.readLine();
		}
		if (line != null) {
			HashMap result= new HashMap();
			Range currRange= null;
			line= rd.readLine();
			while (line != null && !MEMBER_END.equals(line)) {
				if (line.startsWith(NAME_BEG)) {
					if (currRange != null) {
						currRange.end= buf.length();
					}
					currRange= new Range();
					currRange.start= buf.length();
					String name= readSignature(line);
					result.put(name, currRange);
				}
				buf.append(line);
				buf.append(lineDelim);
				line= rd.readLine();
			}
			if (line != null) {
				currRange.end= buf.length();
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
		buf.append(type.getTypeQualifiedName().replace('$', '.'));
		buf.append(".html");
		return buf.toString();
	}
	
	public Reader getJavaDoc(IMember member) {
		if (fContent == null) {
			return null;
		}
		if (fInput.equals(member)) {
			return getReaderForRange(fTypeRange);
		}
		String memberName= getMemberTagName(member);
		return null;
	}
	
	
	
	private Reader getReaderForRange(Range range) {
		return new CharArrayReader(fContent, range.start, range.end - range.start);
	}
	
	private String getMemberTagName(IMember member) {
		StringBuffer buf= new StringBuffer();
		buf.append(member.getElementName());
		return buf.toString();
		/*if (member instanceof IMethod) {
			String[] paramTypes= ((IMethod)member).getParameterTypes();
		}*/
	}
	
	
	public String getContent() {
		return fContent.toString();
	}
	
	public void printContent() {
		if (fContent != null) {
			System.out.println("Type description:");
			System.out.println(new String(fContent, fTypeRange.start, fTypeRange.end - fTypeRange.start));
			
			Object[] keys= fMemberRanges.keySet().toArray();
			for (int i= 0; i < keys.length; i++) {
				System.out.println("Method " + keys[i].toString() + ":");
				Range range= (Range)fMemberRanges.get(keys[i]);			
				System.out.println(new String(fContent, range.start, range.end - range.start));
			}
		}
	}	
	
	private static class Range {
		public int start;
		public int end;
	}
	
	
}