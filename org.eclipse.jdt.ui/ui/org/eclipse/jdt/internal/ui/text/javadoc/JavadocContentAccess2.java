/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.javadoc;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MemberRef;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.MethodRefParameter;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.util.MethodOverrideTester;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.JavadocContentAccess;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLinks;


/**
 * Helper needed to get the content of a Javadoc comment.
 * 
 * <p>
 * <strong>This is work in progress. Parts of this will later become
 * API through {@link JavadocContentAccess}</strong>
 * </p>
 *
 * @since 3.4
 */
public class JavadocContentAccess2 {
	
	private final IMember fMember;
	private String fSource;
	
	private StringBuffer fBuf;
	private int fLiteralContent;

	private JavadocContentAccess2(IMember member) {
		fMember= member;
	}
	
	/**
	 * Gets an IMember's Javadoc comment content from the source attachment
	 * and renders the tags and links in HTML.
	 * Returns <code>null</code> if the member does not contain a Javadoc comment or if no source is available.
	 * 
	 * @param member				the member to get the Javadoc of
	 * @param allowInherited		for methods with no (Javadoc) comment, the comment of the overridden
	 * 									class is returned if <code>allowInherited</code> is <code>true</code>
	 * @param useAttachedJavadoc	if <code>true</code> Javadoc will be extracted from attached Javadoc
	 * 									if there's no source
	 * @return the Javadoc comment content in HTML or <code>null</code> if the member
	 * 			does not contain a Javadoc comment or if no source is available
	 * @throws JavaModelException is thrown when the elements Javadoc can not be accessed
	 */
	public static String getHTMLContent(IMember member, boolean allowInherited, boolean useAttachedJavadoc) throws JavaModelException {
		String sourceJavadoc= getHTMLContentFromSource(member, allowInherited);
		if (sourceJavadoc != null)
			return sourceJavadoc;
		
		if (useAttachedJavadoc && member.getOpenable().getBuffer() == null) { // only if no source available
			return member.getAttachedJavadoc(null);
		}
		
		if (allowInherited && (member.getElementType() == IJavaElement.METHOD))
			return findDocInHierarchy((IMethod) member, useAttachedJavadoc);
		
		return null;
	}

	private static String getHTMLContentFromSource(IMember member, boolean allowInherited) throws JavaModelException {
		IBuffer buf= member.getOpenable().getBuffer();
		if (buf == null) {
			return null; // no source attachment found
		}
		
		ISourceRange javadocRange= member.getJavadocRange();
		if (javadocRange != null) {
			String rawJavadoc= buf.getText(javadocRange.getOffset(), javadocRange.getLength());
			String javadoc= javadoc2HTML(member, rawJavadoc);
			if (!containsOnlyInheritDoc(javadoc)) {
				return javadoc;
			}
		}

		if (allowInherited && (member.getElementType() == IJavaElement.METHOD)) {
			return findDocInHierarchy((IMethod) member, false);
		}
		
		return null;
	}

	private static String javadoc2HTML(IMember member, String rawJavadoc) {
		//FIXME: take from SharedASTProvider if available
		
		//FIXME: Javadoc nodes are not available when Javadoc processing has been disabled!
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=212207
		
		ASTParser parser= ASTParser.newParser(AST.JLS3);
		parser.setProject(member.getJavaProject());
		String source= rawJavadoc + "class C{}"; //$NON-NLS-1$
		parser.setSource(source.toCharArray());
		CompilationUnit root= (CompilationUnit) parser.createAST(null);
		if (root == null)
			return null;
		List types= root.types();
		if (types.size() != 1)
			return null;
		AbstractTypeDeclaration type= (AbstractTypeDeclaration) types.get(0);
		Javadoc javadoc= type.getJavadoc();
		if (javadoc == null) {
			// fall back to JavadocContentAccess:
			try {
				Reader contentReader= JavadocContentAccess.getHTMLContentReader(member, false, false);
				if (contentReader != null)
					return getString(contentReader);
				else
					return null;
			} catch (JavaModelException e) {
				return null;
			}
		}
		return new JavadocContentAccess2(member).toHTML(javadoc, source);
	}

	/**
	 * Gets the reader content as a String
	 * 
	 * @param reader the reader
	 * @return the reader content as string
	 */
	private static String getString(Reader reader) {
		StringBuffer buf= new StringBuffer();
		char[] buffer= new char[1024];
		int count;
		try {
			while ((count= reader.read(buffer)) != -1)
				buf.append(buffer, 0, count);
		} catch (IOException e) {
			return null;
		}
		return buf.toString();
	}
	
	private static boolean containsOnlyInheritDoc(String javadoc) {
		//FIXME: improve {@inheritDoc} support
		return javadoc != null && javadoc.trim().equals("{@inheritDoc}"); //$NON-NLS-1$
	}

	private static String findDocInHierarchy(IMethod method, boolean useAttachedJavadoc) throws JavaModelException {
		/*
		 * Catch ExternalJavaProject in which case
		 * no hierarchy can be built.
		 */
		if (!method.getJavaProject().exists())
			return null;
		
		IType type= method.getDeclaringType();
		ITypeHierarchy hierarchy= type.newSupertypeHierarchy(null);
		
		MethodOverrideTester tester= new MethodOverrideTester(type, hierarchy);
		
		IType[] superTypes= hierarchy.getAllSupertypes(type);
		for (int i= 0; i < superTypes.length; i++) {
			IType curr= superTypes[i];
			IMethod overridden= tester.findOverriddenMethodInType(curr, method);
			if (overridden != null) {
				String javadoc= getHTMLContentFromSource(overridden, false);
				if (javadoc != null) {
					return javadoc;
				}
				if (useAttachedJavadoc) {
					if (overridden.getOpenable().getBuffer() == null) { // only if no source available
						return overridden.getAttachedJavadoc(null);
					} else {
						return null;
					}
				}
			}
		}
		return null;
	}

	private String toHTML(Javadoc javadoc, String source) {
		fSource= source;
		
		fBuf= new StringBuffer();
		fLiteralContent= 0;
		
		TagElement start= null;
		List/*<TagElement>*/ parameters= new ArrayList();
		TagElement returnTag= null;
		List/*<TagElement>*/ exceptions= new ArrayList();
		List/*<TagElement>*/ authors= new ArrayList();
		List/*<TagElement>*/ sees= new ArrayList();
		List/*<TagElement>*/ since= new ArrayList();
		List/*<TagElement>*/ rest= new ArrayList();

		List/*<TagElement>*/ tags= javadoc.tags();
		for (Iterator iter= tags.iterator(); iter.hasNext(); ) {
			TagElement tag= (TagElement) iter.next();
			String tagName= tag.getTagName();
			if (tagName == null) {
				start= tag;
			} else if (TagElement.TAG_PARAM.equals(tagName)) {
				parameters.add(tag);
			} else if (TagElement.TAG_RETURN.equals(tagName)) {
				if (returnTag == null)
					returnTag= tag; // the Javadoc tool only shows the first return tag
			} else if (TagElement.TAG_EXCEPTION.equals(tagName) || TagElement.TAG_THROWS.equals(tagName)) {
				exceptions.add(tag); //FIXME: should also add undocumented checked exceptions
			} else if (TagElement.TAG_AUTHOR.equals(tagName)) {
				authors.add(tag);
			} else if (TagElement.TAG_SEE.equals(tagName)) {
				sees.add(tag);
			} else if (TagElement.TAG_SINCE.equals(tagName)) {
				since.add(tag);
			} else {
				rest.add(tag);
			}
		}
		
		if (start != null)
			handleContentElements(start.fragments());
		
		if (sees.size() > 0 || parameters.size() > 0 || returnTag != null || exceptions.size() > 0 || authors.size() > 0 || since.size() > 0 || rest.size() > 0) {
			fBuf.append("<dl>"); //$NON-NLS-1$
			handleBlockTags(JavaDocMessages.JavaDoc2HTMLTextReader_see_section, sees);
			handleBlockTags(JavaDocMessages.JavaDoc2HTMLTextReader_parameters_section, parameters);
			handleBlockTag(JavaDocMessages.JavaDoc2HTMLTextReader_returns_section, returnTag);
			handleBlockTags(JavaDocMessages.JavaDoc2HTMLTextReader_throws_section, exceptions);
			handleBlockTags(JavaDocMessages.JavaDoc2HTMLTextReader_author_section, authors);
			handleBlockTags(JavaDocMessages.JavaDoc2HTMLTextReader_since_section, since);
			handleBlockTags(rest);
			fBuf.append("</dl>"); //$NON-NLS-1$
		}
		
		String result= fBuf.toString();
		fBuf= null;
		return result;
	}

	private void handleContentElements(List nodes) {
		ASTNode lastNode= null;
		for (Iterator iter= nodes.iterator(); iter.hasNext(); ) {
			ASTNode child= (ASTNode) iter.next();
			if (lastNode != null) {
				int lastEnd= lastNode.getStartPosition() + lastNode.getLength();
				int childStart= child.getStartPosition();
				if (lastEnd != childStart) {
					// Need to preserve whitespace before a node that's not
					// directly following the previous node (e.g. on a new line)
					// due to https://bugs.eclipse.org/bugs/show_bug.cgi?id=206518 :
					String textWithStars= fSource.substring(lastEnd, childStart);
					String text= removeDocLineIntros(textWithStars);
					fBuf.append(text);
				}
			}
			lastNode= child;
			if (child instanceof TextElement) {
				handleText(((TextElement) child).getText());
			} else if (child instanceof TagElement) {
				handleInlineTagElement((TagElement) child);
			} else {
				//TODO: This is unexpected. Log something?
				int start= child.getStartPosition();
				String text= fSource.substring(start, start + child.getLength());
				fBuf.append(removeDocLineIntros(text));
			}
		}
	}

	private String removeDocLineIntros(String textWithStars) {
		String lineBreakGroup= "(\\r\\n?|\\n)"; //$NON-NLS-1$
		String noBreakSpace= "[^\r\n&&\\s]"; //$NON-NLS-1$
		return textWithStars.replaceAll(lineBreakGroup + noBreakSpace + "+\\*" /*+ noBreakSpace + '?'*/, "$1"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void handleText(String text) {
		if (fLiteralContent == 0) {
			fBuf.append(text);
		} else {
			appendEscaped(fBuf, text);
		}
	}

	private static void appendEscaped(StringBuffer buf, String text) {
		int nextToCopy= 0;
		int length= text.length();
		for (int i= 0; i < length; i++) {
			char ch= text.charAt(i);
			String rep= null;
			switch (ch) {
				case '&':
					rep= "&amp;"; //$NON-NLS-1$
					break;
				case '"':
					rep= "&quot;"; //$NON-NLS-1$
					break;
				case '<':
					rep= "&lt;"; //$NON-NLS-1$
					break;
				case '>':
					rep= "&gt;"; //$NON-NLS-1$
					break;
			}
			if (rep != null) {
				if (nextToCopy < i)
					buf.append(text.substring(nextToCopy, i));
				buf.append(rep);
				nextToCopy= i + 1;
			}
		}
		if (nextToCopy < length)
			buf.append(text.substring(nextToCopy));
	}

	private void handleInlineTagElement(TagElement node) {
		//TODO: TagElement.TAG_INHERITDOC, TagElement.TAG_VALUE, TagElement.TAG_DOCROOT
		
		String name= node.getTagName();
		
		boolean isLink= TagElement.TAG_LINK.equals(name);
		boolean isLinkplain= TagElement.TAG_LINKPLAIN.equals(name);
		boolean isCode= TagElement.TAG_CODE.equals(name);
		boolean isLiteral= TagElement.TAG_LITERAL.equals(name);
		
		if (isLiteral || isCode)
			fLiteralContent++;
		if (isLink || isCode)
			fBuf.append("<code>"); //$NON-NLS-1$
		
		if (isLink || isLinkplain)
			handleLink(node.fragments());
		else if (isCode || isLiteral)
			handleContentElements(node.fragments());
		else {
			//print uninterpreted source {@tagname ...} for unknown tags
			int start= node.getStartPosition();
			String text= fSource.substring(start, start + node.getLength());
			fBuf.append(removeDocLineIntros(text));
		}
		
		if (isLink || isCode)
			fBuf.append("</code>"); //$NON-NLS-1$
		if (isLiteral || isCode)
			fLiteralContent--;
		
	}
	
	private void handleBlockTags(String title, List tags) {
		if (tags.isEmpty())
			return;
		
		fBuf.append("<dt>"); //$NON-NLS-1$
		fBuf.append(title);
		fBuf.append("</dt>"); //$NON-NLS-1$
		
		for (Iterator iter= tags.iterator(); iter.hasNext(); ) {
			TagElement tag= (TagElement) iter.next();
			fBuf.append("<dd>"); //$NON-NLS-1$
			if (TagElement.TAG_PARAM.equals(tag.getTagName())) {
				handleParamTag(tag);
			} else if (TagElement.TAG_SEE.equals(tag.getTagName())) {
				handleSeeTag(tag);
			} else if (TagElement.TAG_THROWS.equals(tag.getTagName()) || TagElement.TAG_EXCEPTION.equals(tag.getTagName())) {
				handleThrowsTag(tag);
			} else {
				handleContentElements(tag.fragments());
			}
			fBuf.append("</dd>"); //$NON-NLS-1$
		}
	}
	
	private void handleBlockTag(String title, TagElement tag) {
		if (tag == null)
			return;
		
		fBuf.append("<dt>"); //$NON-NLS-1$
		fBuf.append(title);
		fBuf.append("</dt>"); //$NON-NLS-1$
		fBuf.append("<dd>"); //$NON-NLS-1$
		handleContentElements(tag.fragments());
		fBuf.append("</dd>"); //$NON-NLS-1$
	}

	private void handleBlockTags(List tags) {
		for (Iterator iter= tags.iterator(); iter.hasNext(); ) {
			TagElement tag= (TagElement) iter.next();
			fBuf.append("<dt>"); //$NON-NLS-1$
			fBuf.append(tag.getTagName());
			fBuf.append("</dt>"); //$NON-NLS-1$
			fBuf.append("<dd>"); //$NON-NLS-1$
			handleContentElements(tag.fragments());
			fBuf.append("</dd>"); //$NON-NLS-1$
		}
	}
	
	private void handleSeeTag(TagElement tag) {
		handleLink(tag.fragments());
	}
	
	private void handleThrowsTag(TagElement tag) {
		List fragments= tag.fragments();
		int size= fragments.size();
		if (size > 0) {
			handleLink(fragments.subList(0, 1));
			fBuf.append(JavaElementLabels.CONCAT_STRING);
			handleContentElements(fragments.subList(1, size));
		}
	}
	
	private void handleParamTag(TagElement tag) {
		List fragments= tag.fragments();
		int i= 0;
		int size= fragments.size();
		if (size > 0) {
			Object first= fragments.get(0);
			fBuf.append("<b>"); //$NON-NLS-1$
			if (first instanceof Name) {
				String name= ((Name) first).getFullyQualifiedName();
				fBuf.append(name);
				i++;
			} else if (first instanceof TextElement) {
				String firstText= ((TextElement) first).getText();
				if ("<".equals(firstText)) { //$NON-NLS-1$
					fBuf.append("&lt;"); //$NON-NLS-1$
					i++;
					if (size > 1) {
						Object second= fragments.get(1);
						if (second instanceof Name) {
							String name= ((Name) second).getFullyQualifiedName();
							fBuf.append(name);
							i++;
							if (size > 2) {
								Object third= fragments.get(2);
								String thirdText= ((TextElement) third).getText();
								if (">".equals(thirdText)) { //$NON-NLS-1$
									fBuf.append("&gt;"); //$NON-NLS-1$
									i++;
								}
							}
						}
					}
				}
			}
			fBuf.append("</b> "); //$NON-NLS-1$
			
			handleContentElements(fragments.subList(i, fragments.size()));
		}
	}

	private void handleLink(List fragments) {
		//TODO: Javadoc shortens type names to minimal length according to context
		int fs= fragments.size();
		if (fs > 0) {
			Object first= fragments.get(0);
			String refTypeName= null;
			String refMemberName= null;
			String[] refMethodParamTypes= null;
			String[] refMethodParamNames= null;
			if (first instanceof Name) {
				Name name = (Name) first;
				refTypeName= name.getFullyQualifiedName();
			} else if (first instanceof MemberRef) {
				MemberRef memberRef= (MemberRef) first;
				Name qualifier= memberRef.getQualifier();
				refTypeName= qualifier == null ? "" : qualifier.getFullyQualifiedName(); //$NON-NLS-1$
				refMemberName= memberRef.getName().getIdentifier();
			} else if (first instanceof MethodRef) {
				MethodRef methodRef= (MethodRef) first;
				Name qualifier= methodRef.getQualifier();
				refTypeName= qualifier == null ? "" : qualifier.getFullyQualifiedName(); //$NON-NLS-1$
				refMemberName= methodRef.getName().getIdentifier();
				List params= methodRef.parameters();
				int ps= params.size();
				refMethodParamTypes= new String[ps];
				refMethodParamNames= new String[ps];
				for (int i= 0; i < ps; i++) {
					MethodRefParameter param= (MethodRefParameter) params.get(i);
					refMethodParamTypes[i]= ASTNodes.asString(param.getType());
					SimpleName paramName= param.getName();
					if (paramName != null)
						refMethodParamNames[i]= paramName.getIdentifier();
				}
			}
			
			if (refTypeName != null) {
				fBuf.append("<a href='"); //$NON-NLS-1$
				try {
					String scheme= JavaElementLinks.JAVADOC_SCHEME;
					String uri= JavaElementLinks.createURI(scheme, fMember, refTypeName, refMemberName, refMethodParamTypes);
					fBuf.append(uri);
				} catch (URISyntaxException e) {
					JavaPlugin.log(e);
				}
				fBuf.append("'>"); //$NON-NLS-1$
				if (fs > 1) {
					//TODO:
					// - Set fLiteralContent for label? Check spec.
					// - Javadoc of java.util.regex.Pattern has a space in front of link in <pre>
//					if (fs == 2 && fragments.get(1) instanceof TextElement) {
//						String text= removeLeadingWhitespace(((TextElement) fragments.get(1)).getText());
//						if (text.length() != 0)
//							handleText(text);
//						else
//							//TODO: default from below
//					}
					handleContentElements(fragments.subList(1, fs));
				} else {
					fBuf.append(refTypeName);
					if (refMemberName != null) {
						if (refTypeName.length() > 0) {
							fBuf.append('.');
						}
						fBuf.append(refMemberName);
						if (refMethodParamTypes != null) {
							fBuf.append('(');
							for (int i= 0; i < refMethodParamTypes.length; i++) {
								String pType= refMethodParamTypes[i];
								fBuf.append(pType);
								String pName= refMethodParamNames[i];
								if (pName != null) {
									fBuf.append(' ').append(pName);
								}
								if (i < refMethodParamTypes.length - 1) {
									fBuf.append(", "); //$NON-NLS-1$
								}
							}
							fBuf.append(')');
						}
					}
				}
				fBuf.append("</a>"); //$NON-NLS-1$
			} else {
				handleContentElements(fragments);
			}
		}
		
	}

//	private String removeLeadingWhitespace(String text) {
//		int length= text.length();
//		for (int i= 0; i < length; i++) {
//			if (!Character.isWhitespace(text.charAt(i))) {
//				if (i != 0) {
//					return text.substring(i);
//				}
//				return text;
//			}
//		}
//		return ""; //$NON-NLS-1$
//	}
}
