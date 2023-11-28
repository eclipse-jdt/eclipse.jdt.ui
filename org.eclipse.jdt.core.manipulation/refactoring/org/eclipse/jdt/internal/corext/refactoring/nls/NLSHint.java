/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.refactoring.nls;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.osgi.util.NLS;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Region;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;


/**
 * calculates hints for the nls-refactoring out of a compilation unit.
 * - package fragments of the accessor class and the resource bundle
 * - accessor class name, resource bundle name
 */
public class NLSHint {

	private String fAccessorName;
	private IPackageFragment fAccessorPackage;
	private String fResourceBundleName;
	private IPackageFragment fResourceBundlePackage;
	private NLSSubstitution[] fSubstitutions;

	public NLSHint(ICompilationUnit cu, CompilationUnit astRoot) {
		Assert.isNotNull(cu);
		Assert.isNotNull(astRoot);

		IPackageFragment cuPackage= (IPackageFragment) cu.getAncestor(IJavaElement.PACKAGE_FRAGMENT);

		fAccessorName= NLSRefactoring.DEFAULT_ACCESSOR_CLASSNAME;
		fAccessorPackage= cuPackage;
		fResourceBundleName= NLSRefactoring.DEFAULT_PROPERTY_FILENAME + NLSRefactoring.PROPERTY_FILE_EXT;
		fResourceBundlePackage= cuPackage;

		IJavaProject project= cu.getJavaProject();
		NLSLine[] lines= createRawLines(cu);

		AccessorClassReference accessClassRef= findFirstAccessorReference(lines, astRoot);

		if (accessClassRef == null) {
			// Look for Eclipse NLS approach
			List<NLSLine> eclipseNLSLines= new ArrayList<>();
			accessClassRef= createEclipseNLSLines(getDocument(cu), astRoot, eclipseNLSLines);
			if (!eclipseNLSLines.isEmpty()) {
				NLSLine[] rawLines= lines;
				int rawLinesLength= rawLines.length;
				int eclipseLinesLength= eclipseNLSLines.size();
				lines= new NLSLine[rawLinesLength + eclipseLinesLength];
				System.arraycopy(rawLines, 0, lines, 0, rawLinesLength);
				for (int i= 0; i < eclipseLinesLength; i++)
					lines[i+rawLinesLength]= eclipseNLSLines.get(i);
			}
		}

		Properties props= null;
		if (accessClassRef != null)
			props= NLSHintHelper.getProperties(project, accessClassRef);

		if (props == null)
			props= new Properties();

		fSubstitutions= createSubstitutions(lines, props, astRoot);

		if (accessClassRef != null) {
			fAccessorName= accessClassRef.getName();
			ITypeBinding accessorClassBinding= accessClassRef.getBinding();

			try {
				IPackageFragment accessorPack= NLSHintHelper.getPackageOfAccessorClass(project, accessorClassBinding);
				if (accessorPack != null) {
					fAccessorPackage= accessorPack;
				}

				String fullBundleName= accessClassRef.getResourceBundleName();
				if (fullBundleName != null) {
					fResourceBundleName= Signature.getSimpleName(fullBundleName) + NLSRefactoring.PROPERTY_FILE_EXT;
					String packName= Signature.getQualifier(fullBundleName);

					IPackageFragment pack= NLSHintHelper.getResourceBundlePackage(project, packName, fResourceBundleName);
					if (pack != null) {
						fResourceBundlePackage= pack;
					}
				}
			} catch (JavaModelException e) {
			}
		}
	}

	private AccessorClassReference createEclipseNLSLines(final IDocument document, CompilationUnit astRoot, List<NLSLine> nlsLines) {

		final AccessorClassReference[] firstAccessor= new AccessorClassReference[1];
		final SortedMap<Integer, NLSLine> lineToNLSLine= new TreeMap<>();

		astRoot.accept(new ASTVisitor() {

			private ICompilationUnit fCache_CU;
			private CompilationUnit fCache_AST;

			@Override
			public boolean visit(QualifiedName node) {
				ITypeBinding type= node.getQualifier().resolveTypeBinding();
				if (type != null) {
					ITypeBinding superType= type.getSuperclass();
					if (superType != null && NLS.class.getName().equals(superType.getQualifiedName())) {
						Integer line;
						try {
							line = document.getLineOfOffset(node.getStartPosition());
						} catch (BadLocationException e) {
							return true; // ignore and continue
						}
						NLSLine nlsLine= lineToNLSLine.get(line);
						if (nlsLine == null) {
							nlsLine=  new NLSLine(line);
							lineToNLSLine.put(line, nlsLine);
						}
						SimpleName name= node.getName();
						NLSElement element= new NLSElement(node.getName().getIdentifier(), name.getStartPosition(),
				                name.getLength(), nlsLine.size() - 1, true);
						nlsLine.add(element);
						String bundleName;
						ICompilationUnit bundleCU= (ICompilationUnit)type.getJavaElement().getAncestor(IJavaElement.COMPILATION_UNIT);
						if (fCache_CU == null || !fCache_CU.equals(bundleCU) || fCache_AST == null) {
							fCache_CU= bundleCU;
							if (fCache_CU != null)
								fCache_AST= SharedASTProviderCore.getAST(fCache_CU, SharedASTProviderCore.WAIT_YES, null);
							else
								fCache_AST= null;
						}
						bundleName= NLSHintHelper.getResourceBundleName(fCache_AST);
						element.setAccessorClassReference(new AccessorClassReference(type, bundleName, new Region(node.getStartPosition(), node.getLength())));

						if (firstAccessor[0] == null)
							firstAccessor[0]= element.getAccessorClassReference();

					}
				}
				return true;
			}
		});

		nlsLines.addAll(lineToNLSLine.values());
		return firstAccessor[0];
	}

	private IDocument getDocument(ICompilationUnit cu) {
		IPath path= cu.getPath();
		ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		try {
			manager.connect(path, LocationKind.NORMALIZE, null);
		} catch (CoreException e) {
			return null;
		}

		try {
			ITextFileBuffer buffer= manager.getTextFileBuffer(path, LocationKind.NORMALIZE);
			if (buffer != null)
				return buffer.getDocument();
		} finally {
			try {
				manager.disconnect(path, LocationKind.NORMALIZE, null);
			} catch (CoreException e) {
				return null;
			}
		}
		return null;
	}

	private NLSSubstitution[] createSubstitutions(NLSLine[] lines, Properties props, CompilationUnit astRoot) {
		List<NLSSubstitution> result= new ArrayList<>();

		for (NLSLine line : lines) {
			for (NLSElement nlsElement : line.getElements()) {
				if (nlsElement.hasTag()) {
					AccessorClassReference accessorClassReference= NLSHintHelper.getAccessorClassReference(astRoot, nlsElement);
					if (accessorClassReference == null) {
						// no accessor class => not translated
						result.add(new NLSSubstitution(NLSSubstitution.IGNORED, stripQuotes(nlsElement.getValue(), fAccessorPackage.getJavaProject()), nlsElement));
					} else {
						String key= stripQuotes(nlsElement.getValue(), fAccessorPackage.getJavaProject());
						String value= props.getProperty(key);
						result.add(new NLSSubstitution(NLSSubstitution.EXTERNALIZED, key, value, nlsElement, accessorClassReference));
					}
				} else if (nlsElement.isEclipseNLS()) {
					String key= nlsElement.getValue();
					result.add(new NLSSubstitution(NLSSubstitution.EXTERNALIZED, key, props.getProperty(key), nlsElement, nlsElement.getAccessorClassReference()));
				} else {
					result.add(new NLSSubstitution(NLSSubstitution.INTERNALIZED, stripQuotes(nlsElement.getValue(), fAccessorPackage.getJavaProject()), nlsElement));
				}
			}
		}
		return result.toArray(new NLSSubstitution[result.size()]);
	}

	private static AccessorClassReference findFirstAccessorReference(NLSLine[] lines, CompilationUnit astRoot) {
		for (NLSLine line : lines) {
			for (NLSElement nlsElement : line.getElements()) {
				if (nlsElement.hasTag()) {
					AccessorClassReference accessorClassReference= NLSHintHelper.getAccessorClassReference(astRoot, nlsElement);
					if (accessorClassReference != null) {
						return accessorClassReference;
					}
				}
			}
		}
		// try to find a access with missing //non-nls tag (bug 75155)
		for (NLSLine line : lines) {
			for (NLSElement nlsElement : line.getElements()) {
				if (!nlsElement.hasTag()) {
					AccessorClassReference accessorClassReference= NLSHintHelper.getAccessorClassReference(astRoot, nlsElement);
					if (accessorClassReference != null) {
						return accessorClassReference;
					}
				}
			}
		}
		return null;
	}

	public static String stripQuotes(String str, IJavaProject project) {
		if (JavaModelUtil.is15OrHigher(project)) {
			if (str.startsWith("\"\"\"") && str.endsWith("\"\"\"")) { //$NON-NLS-1$ //$NON-NLS-2$
				return getTextBlock(str.substring(3, str.length() - 3));
			}
		}
		return str.substring(1, str.length() - 1);
	}

	private static NLSLine[] createRawLines(ICompilationUnit cu) {
		try {
			return NLSScanner.scan(cu);
		} catch (JavaModelException | InvalidInputException | BadLocationException x) {
			return new NLSLine[0];
		}
	}


	public String getAccessorClassName() {
		return fAccessorName;
	}

//	public boolean isEclipseNLS() {
//		return fIsEclipseNLS;
//	}

	public IPackageFragment getAccessorClassPackage() {
		return fAccessorPackage;
	}

	public String getResourceBundleName() {
		return fResourceBundleName;
	}

	public IPackageFragment getResourceBundlePackage() {
		return fResourceBundlePackage;
	}

	public NLSSubstitution[] getSubstitutions() {
		return fSubstitutions;
	}

	private static char[] normalize(char[] content) {
		StringBuilder result = new StringBuilder();
		boolean isCR = false;
		for (char c : content) {
			switch (c) {
				case '\r':
					result.append(c);
					isCR = true;
					break;
				case '\n':
					if (!isCR) {
						result.append(c);
					}
					isCR = false;
					break;
				default:
					result.append(c);
					isCR = false;
					break;
			}
		}
		return result.toString().toCharArray();
	}

	private static String getTextBlock(String x) {
		// 1. Normalize line endings
		char[] all = normalize(x.toCharArray());
		// 2. Split into lines. Consider both \n and \r as line separators
		char[][] lines = CharOperation.splitOn('\n', all);
		int size = lines.length;
		List<char[]> list = new ArrayList<>(lines.length);
		for(int i = 1; i < lines.length; i++) {
			char[] line = lines[i];
			if (i + 1 == size && line.length == 0) {
				list.add(line);
				break;
			}
			char[][] sub = CharOperation.splitOn('\r', line);
			if (sub.length == 0) {
				list.add(line);
			} else {
				for (char[] cs : sub) {
					list.add(cs);
				}
			}
		}
		size = list.size();
		lines = list.toArray(new char[size][]);

		// 	3. Handle incidental white space
		//  3.1. Split into lines and identify determining lines
		int prefix = -1;
		for(int i = 0; i < size; i++) {
			char[] line = lines[i];
			boolean blank = true;
			int whitespaces = 0;
	 		for (char c : line) {
				if (blank) {
					if (Character.isWhitespace(c)) {
						whitespaces++;
					} else {
						blank = false;
					}
				}
			}
	 		// The last line with closing delimiter is part of the
	 		// determining line list even if empty
			if (!blank || (i+1 == size)) {
				if (prefix < 0 || whitespaces < prefix) {
	 				prefix = whitespaces;
				}
			}
		}
		// 3.2. Remove the common white space prefix
		// 4. Handle escape sequences  that are not already done in getNextToken0()
		if (prefix == -1)
			prefix = 0;
		StringBuilder result = new StringBuilder();
		boolean newLine = false;
		for(int i = 0; i < lines.length; i++) {
			char[] l  = lines[i];
			// Remove the common prefix from each line
			// And remove all trailing whitespace
			// Finally append the \n at the end of the line (except the last line)
			int length = l.length;
			int trail = length;
			for(;trail > 0;) {
				if (!Character.isWhitespace(l[trail-1])) {
					break;
				}
				trail--;
			}
			if (i >= (size -1)) {
				if (newLine) result.append('\n');
				if (trail < prefix)
					continue;
				newLine = getLineContent(result, l, prefix, trail-1, false, true);
			} else {
				if (i > 0 && newLine)
					result.append('\n');
				if (trail <= prefix) {
					newLine = true;
				} else {
					boolean merge = length > 0 && l[length - 1] == '\\';
					newLine = getLineContent(result, l, prefix, trail-1, merge, false);
				}
			}
		}
		return result.toString();

	}
	// This method is for handling the left over escaped characters during the first
	// scanning (scanForStringLiteral). Admittedly this goes over the text block
	// content again char by char, but this is required in order to correctly
	// treat all the white space and line endings
	private static boolean getLineContent(StringBuilder result, char[] line, int start, int end, boolean merge, boolean lastLine) {
		int lastPointer = 0;
		for(int i = start; i < end;) {
			char c = line[i];
			if (c != '\\') {
				i++;
				continue;
			}
			if (i < end) {
				if (lastPointer + 1 <= i) {
					result.append(CharOperation.subarray(line, lastPointer == 0 ? start : lastPointer, i));
				}
				char next = line[++i];
				switch (next) {
					case '\\' :
						result.append('\\');
						if (i == end)
							merge = false;
						break;
					case 's' :
						result.append(' ');
						break;
					case '"':
						result.append('"');
						break;
					case 'b' :
						result.append('\b');
						break;
					case 'n' :
						result.append('\n');
						break;
					case 'r' :
						result.append('\r');
						break;
					case 't' :
						result.append('\t');
						break;
					case 'f' :
						result.append('\f');
						break;
					default :
						// Direct copy from scanEscapeCharacter
						int pos = i + 1;
						int number = getHexadecimalValue(next);
						if (number >= 0 && number <= 7) {
							boolean zeroToThreeNot = number > 3;
							if (Character.isDigit(next = line[pos])) {
								pos++;
								int digit = getHexadecimalValue(next);
								if (digit >= 0 && digit <= 7) {
									number = (number * 8) + digit;
									if (Character.isDigit(next = line[pos])) {
										pos++;
										if (zeroToThreeNot) {
											// has read \NotZeroToThree OctalDigit Digit --> ignore last character
										} else {
											digit = getHexadecimalValue(next);
											if (digit >= 0 && digit <= 7){ // has read \ZeroToThree OctalDigit OctalDigit
												number = (number * 8) + digit;
											} else {
												// has read \ZeroToThree OctalDigit NonOctalDigit --> ignore last character
											}
										}
									} else {
										// has read \OctalDigit NonDigit--> ignore last character
									}
								} else {
									// has read \OctalDigit NonOctalDigit--> ignore last character
								}
							} else {
								// has read \OctalDigit --> ignore last character
							}
							if (number < 255) {
								next = (char) number;
							}
							result.append(next);
							lastPointer = i = pos;
							continue;
						} else {
							// Dealing with just '\'
							result.append(c);
							lastPointer = i;
							continue;
						}
				}
				lastPointer = ++i;
			}
		}
		end = merge ? end : end >= line.length ? end : end + 1;
		char[] chars = lastPointer == 0 ?
				CharOperation.subarray(line, start, end) :
					CharOperation.subarray(line, lastPointer, end);
		// The below check is because CharOperation.subarray tend to return null when the
		// boundaries produce a zero sized char[]
		if (chars != null && chars.length > 0)
			result.append(chars);
		return (!merge && !lastLine);
	}
	private static int getHexadecimalValue(char c) {
		switch(c) {
			case '0' :
				return 0;
			case '1' :
				return 1;
			case '2' :
				return 2;
			case '3' :
				return 3;
			case '4' :
				return 4;
			case '5' :
				return 5;
			case '6' :
				return 6;
			case '7' :
				return 7;
			case '8' :
				return 8;
			case '9' :
				return 9;
			case 'A' :
			case 'a' :
				return 10;
			case 'B' :
			case 'b' :
				return 11;
			case 'C' :
			case 'c' :
				return 12;
			case 'D' :
			case 'd' :
				return 13;
			case 'E' :
			case 'e' :
				return 14;
			case 'F' :
			case 'f' :
				return 15;
			default:
				return -1;
		}
	}

}
