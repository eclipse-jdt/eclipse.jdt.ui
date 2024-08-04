/*******************************************************************************
 * Copyright (c) 2008, 2024 IBM Corporation and others.
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
 *     Tom Hofmann, Google <eclipse@tom.eicher.name> - [hovering] NPE when hovering over @value reference within a type's javadoc - https://bugs.eclipse.org/bugs/show_bug.cgi?id=320084
 *******************************************************************************/
package org.eclipse.jdt.core.manipulation.internal.javadoc;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IResource;

import org.eclipse.text.html.HTMLBuilder;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MemberRef;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.MethodRefParameter;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TagProperty;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;
import org.eclipse.jdt.core.manipulation.internal.javadoc.CoreJavadocAccess.SuperclassReferenceMethodData;
import org.eclipse.jdt.core.manipulation.internal.javadoc.IJavadocContentFactory.IJavadocAccess;

import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;
import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

import org.eclipse.jdt.internal.ui.viewsupport.CoreJavaElementLinks;


/**
 * Helper to get the content of a Javadoc comment as HTML.
 */
public class CoreJavadocAccessImpl implements IJavadocAccess {
	protected static final String BLOCK_TAG_START= "<dl>"; //$NON-NLS-1$

	protected static final String BLOCK_TAG_END= "</dl>"; //$NON-NLS-1$

	protected static final String BlOCK_TAG_TITLE_START= "<dt>"; //$NON-NLS-1$

	protected static final String BlOCK_TAG_TITLE_END= "</dt>"; //$NON-NLS-1$

	protected static final String BlOCK_TAG_ENTRY_START= "<dd>"; //$NON-NLS-1$

	protected static final String BlOCK_TAG_ENTRY_END= "</dd>"; //$NON-NLS-1$

	protected static final String PARAM_NAME_START= "<b>"; //$NON-NLS-1$

	protected static final String PARAM_NAME_END= "</b> "; //$NON-NLS-1$

	/**
	 * Either an IMember or an IPackageFragment.
	 */
	protected final IJavaElement fElement;

	protected final CoreJavaDocSnippetStringEvaluator fSnippetStringEvaluator;

	/**
	 * The method, or <code>null</code> if {@link #fElement} is not a method where @inheritDoc could
	 * work.
	 */
	protected final IMethod fMethod;

	protected final Javadoc fJavadoc;

	protected final String fSource;

	protected final JavadocLookup fJavadocLookup;

	protected StringBuffer fBuf;

	protected int fLiteralContent;

	protected StringBuffer fMainDescription;

	protected StringBuffer fReturnDescription;

	protected StringBuffer[] fTypeParamDescriptions;

	protected StringBuffer[] fParamDescriptions;

	protected HashMap<String, StringBuffer> fExceptionDescriptions;

	protected int fPreCounter;

	protected int fInPreCodeCounter= -1;

	public CoreJavadocAccessImpl(IJavaElement element, Javadoc javadoc, String source, JavadocLookup lookup) {
		Assert.isNotNull(element);
		Assert.isTrue(element instanceof IMethod || element instanceof ILocalVariable || element instanceof ITypeParameter);
		fElement= element;
		fSnippetStringEvaluator= createSnippetEvaluator(fElement);
		fMethod= (IMethod) ((element instanceof ILocalVariable || element instanceof ITypeParameter) ? element.getParent() : element);
		fJavadoc= javadoc;
		fSource= source;
		fJavadocLookup= lookup;
	}

	public CoreJavadocAccessImpl(IJavaElement element, Javadoc javadoc, String source) {
		Assert.isNotNull(element);
		Assert.isTrue(element instanceof IMember || element instanceof IPackageFragment || element instanceof ILocalVariable || element instanceof ITypeParameter);
		fElement= element;
		fSnippetStringEvaluator= createSnippetEvaluator(fElement);
		fMethod= null;
		fJavadoc= javadoc;
		fSource= source;
		fJavadocLookup= JavadocLookup.NONE;
	}

	protected CoreJavaDocSnippetStringEvaluator createSnippetEvaluator(@SuppressWarnings("unused") IJavaElement element) {
		return new CoreJavaDocSnippetStringEvaluator(fElement);
	}

	@Override
	public String toHTML() {
		fBuf= new StringBuffer();
		fLiteralContent= 0;

		if (fElement instanceof ILocalVariable || fElement instanceof ITypeParameter) {
			parameterToHTML();
		} else {
			elementToHTML();
		}

		String result= fBuf.toString();
		fBuf= null;
		return result;
	}

	protected void parameterToHTML() {
		String elementName= fElement.getElementName();
		List<TagElement> tags= fJavadoc.tags();
		for (TagElement tag : tags) {
			String tagName= tag.getTagName();
			if (TagElement.TAG_PARAM.equals(tagName)) {
				List<? extends ASTNode> fragments= tag.fragments();
				int size= fragments.size();
				if (size > 0) {
					Object first= fragments.get(0);
					if (first instanceof SimpleName) {
						String name= ((SimpleName) first).getIdentifier();
						if (elementName.equals(name)) {
							handleContentElements(fragments.subList(1, size));
							return;
						}
					} else if (size > 2 && fElement instanceof ITypeParameter && first instanceof TextElement) {
						String firstText= ((TextElement) first).getText();
						if ("<".equals(firstText)) { //$NON-NLS-1$
							Object second= fragments.get(1);
							Object third= fragments.get(2);
							if (second instanceof SimpleName && third instanceof TextElement) {
								String name= ((SimpleName) second).getIdentifier();
								String thirdText= ((TextElement) third).getText();
								if (elementName.equals(name) && ">".equals(thirdText)) { //$NON-NLS-1$
									handleContentElements(fragments.subList(3, size));
									return;
								}
							}
						}
					}
				}
			}
		}
		if (fElement instanceof ILocalVariable) {
			List<String> parameterNames= initParameterNames();
			int i= parameterNames.indexOf(elementName);
			if (i != -1) {
				CharSequence inheritedParamDescription= fJavadocLookup.getInheritedParamDescription(fMethod, i);
				handleInherited(inheritedParamDescription);
			}
		} else if (fElement instanceof ITypeParameter) {
			List<String> typeParameterNames= initTypeParameterNames();
			int i= typeParameterNames.indexOf(elementName);
			if (i != -1) {
				CharSequence inheritedTypeParamDescription= fJavadocLookup.getInheritedTypeParamDescription(fMethod, i);
				handleInherited(inheritedTypeParamDescription);
			}
		}
	}

	protected void elementToHTML() {
		// After first loop, non-null entries in the following two lists are missing and need to be inherited:
		List<String> typeParameterNames= initTypeParameterNames();
		List<String> parameterNames= initParameterNames();
		List<String> exceptionNames= initExceptionNames();

		TagElement deprecatedTag= null;
		TagElement start= null;
		List<TagElement> typeParameters= new ArrayList<>();
		List<TagElement> parameters= new ArrayList<>();
		TagElement returnTag= null;
		List<TagElement> exceptions= new ArrayList<>();
		List<TagElement> provides= new ArrayList<>();
		List<TagElement> uses= new ArrayList<>();
		List<TagElement> versions= new ArrayList<>();
		List<TagElement> authors= new ArrayList<>();
		List<TagElement> sees= new ArrayList<>();
		List<TagElement> since= new ArrayList<>();
		List<TagElement> rest= new ArrayList<>();
		List<TagElement> apinote= new ArrayList<>(1);
		List<TagElement> implspec= new ArrayList<>(1);
		List<TagElement> implnote= new ArrayList<>(1);
		List<TagElement> hidden= new ArrayList<>(1);

		List<TagElement> tags= fJavadoc.tags();
		for (TagElement tag : tags) {
			String tagName= tag.getTagName();
			if (tagName == null) {
				start= tag;

			} else {
				switch (tagName) {
					case TagElement.TAG_PARAM:
						List<? extends ASTNode> fragments= tag.fragments();
						int size= fragments.size();
						if (size > 0) {
							Object first= fragments.get(0);
							if (first instanceof SimpleName) {
								String name= ((SimpleName) first).getIdentifier();
								int paramIndex= parameterNames.indexOf(name);
								if (paramIndex != -1) {
									parameterNames.set(paramIndex, null);
								}
								parameters.add(tag);
							} else if (size > 2 && first instanceof TextElement) {
								String firstText= ((TextElement) first).getText();
								if ("<".equals(firstText)) { //$NON-NLS-1$
									Object second= fragments.get(1);
									Object third= fragments.get(2);
									if (second instanceof SimpleName && third instanceof TextElement) {
										String name= ((SimpleName) second).getIdentifier();
										String thirdText= ((TextElement) third).getText();
										if (">".equals(thirdText)) { //$NON-NLS-1$
											int paramIndex= typeParameterNames.indexOf(name);
											if (paramIndex != -1) {
												typeParameterNames.set(paramIndex, null);
											}
											typeParameters.add(tag);
										}
									}
								}
							}
						}
						break;
					case TagElement.TAG_RETURN:
						break;
					case TagElement.TAG_EXCEPTION:
					case TagElement.TAG_THROWS:
						exceptions.add(tag);
						List<? extends ASTNode> fragments2= tag.fragments();
						if (fragments2.size() > 0) {
							Object first= fragments2.get(0);
							if (first instanceof Name) {
								String name= ASTNodes.getSimpleNameIdentifier((Name) first);
								int exceptionIndex= exceptionNames.indexOf(name);
								if (exceptionIndex != -1) {
									exceptionNames.set(exceptionIndex, null);
								}
							}
						}
						break;
					case TagElement.TAG_PROVIDES:
						provides.add(tag);
						break;
					case TagElement.TAG_USES:
						uses.add(tag);
						break;
					case TagElement.TAG_SINCE:
						since.add(tag);
						break;
					case TagElement.TAG_VERSION:
						versions.add(tag);
						break;
					case TagElement.TAG_AUTHOR:
						authors.add(tag);
						break;
					case TagElement.TAG_SEE:
						sees.add(tag);
						break;
					case TagElement.TAG_DEPRECATED:
						if (deprecatedTag == null)
							deprecatedTag= tag; // the Javadoc tool only shows the first deprecated tag
						break;
					case TagElement.TAG_API_NOTE:
						apinote.add(tag);
						break;
					case TagElement.TAG_IMPL_SPEC:
						implspec.add(tag);
						break;
					case TagElement.TAG_IMPL_NOTE:
						implnote.add(tag);
						break;
					case TagElement.TAG_HIDDEN:
						hidden.add(tag);
						break;
					default:
						rest.add(tag);
						break;
				}
			}
		}

		List<TagElement> returnTags= new ArrayList<>();
		findTags(TagElement.TAG_RETURN, returnTags, tags);
		if (!returnTags.isEmpty())
			returnTag= returnTags.get(0); // the Javadoc tool only shows the first return tag

		//TODO: @Documented annotations before header
		if (deprecatedTag != null)
			handleDeprecatedTag(deprecatedTag);
		if (start != null)
			handleContentElements(start.fragments());
		else if (fMethod != null) {
			CharSequence inherited= fJavadocLookup.getInheritedMainDescription(fMethod);
			// The Javadoc tool adds "Description copied from class: ..." (only for the main description).
			// We don't bother doing that.
			handleInherited(inherited);
		}

		CharSequence[] typeParameterDescriptions= new CharSequence[typeParameterNames.size()];
		boolean hasInheritedTypeParameters= inheritTypeParameterDescriptions(typeParameterNames, typeParameterDescriptions);
		boolean hasTypeParameters= typeParameters.size() > 0 || hasInheritedTypeParameters;

		CharSequence[] parameterDescriptions= new CharSequence[parameterNames.size()];
		boolean hasInheritedParameters= inheritParameterDescriptions(parameterNames, parameterDescriptions);
		boolean hasParameters= parameters.size() > 0 || hasInheritedParameters;

		CharSequence returnDescription= null;
		if (returnTag == null && needsReturnTag())
			returnDescription= fJavadocLookup.getInheritedReturnDescription(fMethod);
		boolean hasReturnTag= returnTag != null || returnDescription != null;

		CharSequence[] exceptionDescriptions= new CharSequence[exceptionNames.size()];
		boolean hasInheritedExceptions= inheritExceptionDescriptions(exceptionNames, exceptionDescriptions);
		boolean hasExceptions= exceptions.size() > 0 || hasInheritedExceptions;

		if (hasParameters || hasTypeParameters || hasReturnTag || hasExceptions
				|| versions.size() > 0 || authors.size() > 0 || since.size() > 0 || sees.size() > 0 ||
				apinote.size() > 0 || implnote.size() > 0 || implspec.size() > 0 || uses.size() > 0 ||
				provides.size() > 0 || hidden.size() > 0 || rest.size() > 0
				|| (fBuf.length() > 0 && (parameterDescriptions.length > 0 || exceptionDescriptions.length > 0))) {
			handleSuperMethodReferences();
			fBuf.append(getBlockTagStart());
			handleParameterTags(typeParameters, typeParameterNames, typeParameterDescriptions, true);
			handleParameterTags(parameters, parameterNames, parameterDescriptions, false);
			handleReturnTag(returnTag, returnDescription);
			handleExceptionTags(exceptions, exceptionNames, exceptionDescriptions);
			handleBlockTags(JavaDocMessages.JavaDoc2HTMLTextReader_since_section, since);
			handleBlockTags(JavaDocMessages.JavaDoc2HTMLTextReader_version_section, versions);
			handleBlockTags(JavaDocMessages.JavaDoc2HTMLTextReader_author_section, authors);
			handleBlockTags(JavaDocMessages.JavaDoc2HTMLTextReader_see_section, sees);
			handleBlockTags(JavaDocMessages.JavaDoc2HTMLTextReader_api_note, apinote);
			handleBlockTags(JavaDocMessages.JavaDoc2HTMLTextReader_impl_spec, implspec);
			handleBlockTags(JavaDocMessages.JavaDoc2HTMLTextReader_impl_note, implnote);
			handleBlockTags(JavaDocMessages.JavaDoc2HTMLTextReader_uses, uses);
			handleBlockTags(JavaDocMessages.JavaDoc2HTMLTextReader_provides, provides);
			if (hidden.size() > 0) {
				handleBlockTagsHidden();
			}
			handleBlockTags(rest);
			fBuf.append(getBlockTagEnd());

		} else if (fBuf.length() > 0) {
			handleSuperMethodReferences();
		}
	}

	protected List<TagElement> findTags(String tagName, List<TagElement> found, List<? extends ASTNode> tags) {
		for (ASTNode node : tags) {
			if (node instanceof TagElement) {
				TagElement tag= (TagElement) node;
				if (tagName.equals(tag.getTagName())) {
					found.add(tag);
				}

				findTags(tagName, found, tag.fragments());
			}
		}
		return found;
	}

	protected void handleBlockTagsHidden() {
		String replaceAll= fBuf.toString().replaceAll(getBlockTagStart(), "<dl hidden>"); //$NON-NLS-1$
		replaceAll= replaceAll.replaceAll(getBlockTagTitleStart(), "<dt hidden>"); //$NON-NLS-1$
		replaceAll= replaceAll.replaceAll(getBlockTagEntryStart(), "<dd hidden>"); //$NON-NLS-1$
		// For tags like deprecated
		replaceAll= replaceAll.replaceAll(getParamNameStart(), "<b hidden>"); //$NON-NLS-1$
		fBuf.setLength(0);
		fBuf.append(replaceAll);
	}

	protected void handleDeprecatedTag(TagElement tag) {
		fBuf.append("<p><b>"); //$NON-NLS-1$
		fBuf.append(JavaDocMessages.JavaDoc2HTMLTextReader_deprecated_section);
		fBuf.append("</b> <i>"); //$NON-NLS-1$
		handleContentElements(tag.fragments());
		fBuf.append("</i><p>"); //$NON-NLS-1$ TODO: Why not </p>? See https://bugs.eclipse.org/bugs/show_bug.cgi?id=243318 .
	}

	protected void handleSuperMethodReferences() {
		if (fMethod != null) {
			try {
				StringBuffer superMethodReferences= createSuperMethodReferences(fMethod);
				if (superMethodReferences != null)
					fBuf.append(superMethodReferences);
			} catch (JavaModelException e) {
				JavaManipulationPlugin.log(e);
			}
		}
	}

	protected StringBuffer createSuperMethodReferences(IMethod method) throws JavaModelException {
		SuperclassReferenceMethodData data= CoreJavadocAccess.getSuperclassReferenceMethodData(method);
		if (data == null)
			return null;
		return createSuperMethodReferencesHTML(data.superInterfaceMethods(), data.superClassMethod());
	}

	protected StringBuffer createSuperMethodReferencesHTML(ArrayList<IMethod> superInterfaceMethods, IMethod superClassMethod) {
		// jdtls override to return null
		return CoreJavadocAccess.createSuperMethodReferencesHTMLStaticImpl(superInterfaceMethods, superClassMethod);
	}


	protected List<String> initTypeParameterNames() {
		if (fMethod != null) {
			try {
				ArrayList<String> typeParameterNames= new ArrayList<>();
				for (ITypeParameter typeParameter : fMethod.getTypeParameters()) {
					typeParameterNames.add(typeParameter.getElementName());
				}
				return typeParameterNames;
			} catch (JavaModelException e) {
				JavaManipulationPlugin.log(e);
			}
		}
		return Collections.emptyList();
	}

	protected List<String> initParameterNames() {
		if (fMethod != null) {
			try {
				return new ArrayList<>(Arrays.asList(fMethod.getParameterNames()));
			} catch (JavaModelException e) {
				JavaManipulationPlugin.log(e);
			}
		}
		return Collections.emptyList();
	}

	protected List<String> initExceptionNames() {
		if (fMethod != null) {
			try {
				ArrayList<String> exceptionNames= new ArrayList<>();
				for (String exceptionType : fMethod.getExceptionTypes()) {
					exceptionNames.add(Signature.getSimpleName(Signature.toString(exceptionType)));
				}
				return exceptionNames;
			} catch (JavaModelException e) {
				JavaManipulationPlugin.log(e);
			}
		}
		return Collections.emptyList();
	}

	protected boolean needsReturnTag() {
		if (fMethod == null)
			return false;
		try {
			return !Signature.SIG_VOID.equals(fMethod.getReturnType());
		} catch (JavaModelException e) {
			JavaManipulationPlugin.log(e);
			return false;
		}
	}

	protected boolean inheritTypeParameterDescriptions(List<String> typeParameterNames, CharSequence[] typeParameterDescriptions) {
		boolean hasInheritedTypeParameters= false;
		for (int i= 0; i < typeParameterNames.size(); i++) {
			String name= typeParameterNames.get(i);
			if (name != null) {
				typeParameterDescriptions[i]= fJavadocLookup.getInheritedTypeParamDescription(fMethod, i);
				if (typeParameterDescriptions[i] != null)
					hasInheritedTypeParameters= true;
			}
		}
		return hasInheritedTypeParameters;
	}

	protected boolean inheritParameterDescriptions(List<String> parameterNames, CharSequence[] parameterDescriptions) {
		boolean hasInheritedParameters= false;
		for (int i= 0; i < parameterNames.size(); i++) {
			String name= parameterNames.get(i);
			if (name != null) {
				parameterDescriptions[i]= fJavadocLookup.getInheritedParamDescription(fMethod, i);
				if (parameterDescriptions[i] != null)
					hasInheritedParameters= true;
			}
		}
		return hasInheritedParameters;
	}

	protected boolean inheritExceptionDescriptions(List<String> exceptionNames, CharSequence[] exceptionDescriptions) {
		boolean hasInheritedExceptions= false;
		for (int i= 0; i < exceptionNames.size(); i++) {
			String name= exceptionNames.get(i);
			if (name != null) {
				exceptionDescriptions[i]= fJavadocLookup.getInheritedExceptionDescription(fMethod, name);
				if (exceptionDescriptions[i] != null)
					hasInheritedExceptions= true;
			}
		}
		return hasInheritedExceptions;
	}

	@Override
	public CharSequence getMainDescription() {
		if (fMainDescription == null) {
			fMainDescription= new StringBuffer();
			fBuf= fMainDescription;
			fLiteralContent= 0;

			List<TagElement> tags= fJavadoc.tags();
			for (TagElement tag : tags) {
				String tagName= tag.getTagName();
				if (tagName == null) {
					handleContentElements(tag.fragments());
					break;
				}
			}

			fBuf= null;
		}
		return fMainDescription.length() > 0 ? fMainDescription : null;
	}

	@Override
	public CharSequence getReturnDescription() {
		if (fReturnDescription == null) {
			fReturnDescription= new StringBuffer();
			fBuf= fReturnDescription;
			fLiteralContent= 0;

			List<TagElement> tags= fJavadoc.tags();
			List<TagElement> returnTags= new ArrayList<>();
			findTags(TagElement.TAG_RETURN, returnTags, tags);
			if (!returnTags.isEmpty()) {
				TagElement returnTag= returnTags.get(0);
				handleContentElements(returnTag.fragments());
			}

			fBuf= null;
		}
		return fReturnDescription.length() > 0 ? fReturnDescription : null;
	}

	@Override
	public CharSequence getInheritedTypeParamDescription(int typeParamIndex) {
		if (fMethod != null) {
			List<String> typeParameterNames= initTypeParameterNames();
			if (fTypeParamDescriptions == null) {
				fTypeParamDescriptions= new StringBuffer[typeParameterNames.size()];
			} else {
				StringBuffer description= fTypeParamDescriptions[typeParamIndex];
				if (description != null) {
					return description.length() > 0 ? description : null;
				}
			}

			StringBuffer description= new StringBuffer();
			fTypeParamDescriptions[typeParamIndex]= description;
			fBuf= description;
			fLiteralContent= 0;

			String typeParamName= typeParameterNames.get(typeParamIndex);
			List<TagElement> tags= fJavadoc.tags();
			for (TagElement tag : tags) {
				String tagName= tag.getTagName();
				if (TagElement.TAG_PARAM.equals(tagName)) {
					List<? extends ASTNode> fragments= tag.fragments();
					if (fragments.size() > 2) {
						Object first= fragments.get(0);
						Object second= fragments.get(1);
						Object third= fragments.get(2);
						if (first instanceof TextElement && second instanceof SimpleName && third instanceof TextElement) {
							String firstText= ((TextElement) first).getText();
							String thirdText= ((TextElement) third).getText();
							if ("<".equals(firstText) && ">".equals(thirdText)) { //$NON-NLS-1$ //$NON-NLS-2$
								String name= ((SimpleName) second).getIdentifier();
								if (name.equals(typeParamName)) {
									handleContentElements(fragments.subList(3, fragments.size()));
									break;
								}
							}
						}
					}
				}
			}

			fBuf= null;
			return description.length() > 0 ? description : null;
		}
		return null;
	}

	@Override
	public CharSequence getInheritedParamDescription(int paramIndex) throws JavaModelException {
		if (fMethod != null) {
			String[] parameterNames= fMethod.getParameterNames();
			if (fParamDescriptions == null) {
				fParamDescriptions= new StringBuffer[parameterNames.length];
			} else {
				StringBuffer description= fParamDescriptions[paramIndex];
				if (description != null) {
					return description.length() > 0 ? description : null;
				}
			}

			StringBuffer description= new StringBuffer();
			fParamDescriptions[paramIndex]= description;
			fBuf= description;
			fLiteralContent= 0;

			String paramName= parameterNames[paramIndex];
			List<TagElement> tags= fJavadoc.tags();
			for (TagElement tag : tags) {
				String tagName= tag.getTagName();
				if (TagElement.TAG_PARAM.equals(tagName)) {
					List<? extends ASTNode> fragments= tag.fragments();
					if (fragments.size() > 0) {
						Object first= fragments.get(0);
						if (first instanceof SimpleName) {
							String name= ((SimpleName) first).getIdentifier();
							if (name.equals(paramName)) {
								handleContentElements(fragments.subList(1, fragments.size()));
								break;
							}
						}
					}
				}
			}

			fBuf= null;
			return description.length() > 0 ? description : null;
		}
		return null;
	}

	@Override
	public CharSequence getExceptionDescription(String simpleName) {
		if (fMethod != null) {
			if (fExceptionDescriptions == null) {
				fExceptionDescriptions= new HashMap<>();
			} else {
				StringBuffer description= fExceptionDescriptions.get(simpleName);
				if (description != null) {
					return description.length() > 0 ? description : null;
				}
			}

			StringBuffer description= new StringBuffer();
			fExceptionDescriptions.put(simpleName, description);
			fBuf= description;
			fLiteralContent= 0;

			List<TagElement> tags= fJavadoc.tags();
			for (TagElement tag : tags) {
				String tagName= tag.getTagName();
				if (TagElement.TAG_THROWS.equals(tagName) || TagElement.TAG_EXCEPTION.equals(tagName)) {
					List<? extends ASTNode> fragments= tag.fragments();
					if (fragments.size() > 0) {
						Object first= fragments.get(0);
						if (first instanceof Name) {
							String name= ASTNodes.getSimpleNameIdentifier((Name) first);
							if (name.equals(simpleName)) {
								if (fragments.size() > 1)
									handleContentElements(fragments.subList(1, fragments.size()));
								break;
							}
						}
					}
				}
			}

			fBuf= null;
			return description.length() > 0 ? description : null;
		}
		return null;
	}


	protected void handleContentElements(List<? extends ASTNode> nodes) {
		handleContentElements(nodes, false, null);
	}

	protected void handleContentElements(List<? extends ASTNode> nodes, boolean skipLeadingWhiteSpace) {
		handleContentElements(nodes, skipLeadingWhiteSpace, null);
	}

	protected void handleContentElements(List<? extends ASTNode> nodes, boolean skipLeadingWhitespace, TagElement tagElement) {
		ASTNode previousNode= null;
		for (ASTNode child : nodes) {
			if (previousNode != null) {
				int previousEnd= previousNode.getStartPosition() + previousNode.getLength();
				int childStart= child.getStartPosition();
				if (previousEnd > childStart) {
					// should never happen, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=304826
					Exception exception= new Exception("Illegal ASTNode positions: previousEnd=" + previousEnd //$NON-NLS-1$
							+ ", childStart=" + childStart //$NON-NLS-1$
							+ ", element=" + fElement.getHandleIdentifier() //$NON-NLS-1$
							+ ", Javadoc:\n" + fSource); //$NON-NLS-1$
					JavaManipulationPlugin.log(exception);
				} else if (previousEnd != childStart) {
					// Need to preserve whitespace before a node that's not
					// directly following the previous node (e.g. on a new line)
					// due to https://bugs.eclipse.org/bugs/show_bug.cgi?id=206518 :
					String textWithStars= fSource.substring(previousEnd, childStart);
					String text= removeDocLineIntros(textWithStars);
					fBuf.append(text);
				}
			} else if (tagElement != null && fPreCounter >= 1) {
				int childStart= child.getStartPosition();
				int previousEnd= tagElement.getStartPosition() + tagElement.getTagName().length() + 1;
				// Need to preserve whitespace before a node in a <pre> section
				String textWithStars= fSource.substring(previousEnd, childStart);
				String text= removeDocLineIntros(textWithStars);
				fBuf.append(text);
			}
			previousNode= child;
			if (child instanceof TextElement) {
				TextElement te= (TextElement) child;
				handleInLineTextElement(te, skipLeadingWhitespace, tagElement, previousNode);

			} else if (child instanceof TagElement) {
				handleInlineTagElement((TagElement) child);
			} else {
				// This is unexpected. Fail gracefully by just copying the source.
				int start= child.getStartPosition();
				String text= fSource.substring(start, start + child.getLength());
				fBuf.append(removeDocLineIntros(text));
			}
		}
	}


	protected void handleInLineTextElement(TextElement te, boolean skipLeadingWhitespace, TagElement tagElement, ASTNode previousNode) {
		String text= te.getText();
		if (skipLeadingWhitespace) {
			text= text.replaceFirst("^\\s", ""); //$NON-NLS-1$ //$NON-NLS-2$
		}

		// workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=233481 :
		text= text.replaceAll("(\r\n?|\n)([ \t]*\\*)", "$1"); //$NON-NLS-1$ //$NON-NLS-2$
		text= handlePreCounter(tagElement, text);
		handleInLineText(text, previousNode);
	}

	protected String handlePreCounter(TagElement tagElement, String text) {
		if (tagElement == null && text.equals("<pre>")) { //$NON-NLS-1$
			++fPreCounter;
		} else if (tagElement == null && text.equals("</pre>")) { //$NON-NLS-1$
			--fPreCounter;
			if (fPreCounter == fInPreCodeCounter) {
				fInPreCodeCounter= -1;
			}
		} else if (tagElement == null && fPreCounter > 0 && text.matches("}\\s*</pre>")) { //$NON-NLS-1$
			// this is a temporary workaround for https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/316
			// as the parser for @code is treating the first } it finds as the end of the code
			// sequence but this is not the case for a <pre>{@code sequence which goes over
			// multiple lines and may contain }'s that are part of the code
			--fPreCounter;
			if (fPreCounter == fInPreCodeCounter) {
				text= "</code></pre>"; //$NON-NLS-1$
				int lastCodeEnd= fBuf.lastIndexOf("</code>"); //$NON-NLS-1$
				if (lastCodeEnd >= 0) {
					fBuf.replace(lastCodeEnd, lastCodeEnd + 7, ""); //$NON-NLS-1$
				}
				fInPreCodeCounter= -1;
			}
		}
		return text;
	}

	protected void handleInLineText(String text, @SuppressWarnings("unused") ASTNode previousNode) {
		handleText(text);
	}

	protected String removeDocLineIntros(String textWithStars) {
		String lineBreakGroup= "(\\r\\n?|\\n)"; //$NON-NLS-1$
		String noBreakSpace= "[^\r\n&&\\s]"; //$NON-NLS-1$
		return textWithStars.replaceAll(lineBreakGroup + noBreakSpace + "*\\*" /*+ noBreakSpace + '?'*/, "$1"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected void handleText(String text) {
		if (fLiteralContent == 0) {
			handleUnicode(fBuf, text);
		} else {
			text= appendEscaped(text);
			handleUnicode(fBuf, text);
		}
	}

	protected void handleUnicode(StringBuffer buf, String text) {
		int nextToCopy= 0;
		int length= text.length();
		boolean seenBackSlash= false;
		for (int i= 0; i < length; i++) {
			char ch= text.charAt(i);
			String rep= null;
			switch (ch) {
				case '\\':
					seenBackSlash= true;
					break;
				case 'u':
					if (seenBackSlash) {
						seenBackSlash= false;
						char ch1, ch2, ch3, ch4;
						if (i + 4 < length) {
							ch1= text.charAt(i + 1);
							ch2= text.charAt(i + 2);
							ch3= text.charAt(i + 3);
							ch4= text.charAt(i + 4);
							if (Character.digit(ch1, 16) != -1 &&
									Character.digit(ch2, 16) != -1 &&
									Character.digit(ch3, 16) != -1 &&
									Character.digit(ch4, 16) != -1) {
								rep= "&#x" + ch1 + ch2 + ch3 + ch4 + ";"; //$NON-NLS-1$ //$NON-NLS-2$
							}
						}
					}
					break;
				default:
					seenBackSlash= false;
					break;
			}
			if (rep != null) {
				if (nextToCopy < i)
					buf.append(text.substring(nextToCopy, i - 1));
				buf.append(rep);
				i+= 4;
				nextToCopy= i + 1;
			}
		}
		if (nextToCopy < length)
			buf.append(text.substring(nextToCopy));
	}

	protected String appendEscaped(String text) {
		int nextToCopy= 0;
		int length= text.length();
		StringBuffer buf= new StringBuffer();
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
		return buf.toString();
	}

	protected void handleInlineTagElement(TagElement node) {
		String name= node.getTagName();
		if (TagElement.TAG_VALUE.equals(name) && handleValueTag(node))
			return;

		boolean isLink= TagElement.TAG_LINK.equals(name);
		boolean isLinkplain= TagElement.TAG_LINKPLAIN.equals(name);
		boolean isCode= TagElement.TAG_CODE.equals(name);
		boolean isLiteral= TagElement.TAG_LITERAL.equals(name);
		boolean isSummary= TagElement.TAG_SUMMARY.equals(name);
		boolean isIndex= TagElement.TAG_INDEX.equals(name);
		boolean isSnippet= TagElement.TAG_SNIPPET.equals(name);
		boolean isReturn= TagElement.TAG_RETURN.equals(name);

		if (isLiteral || isCode || isSummary || isIndex)
			fLiteralContent++;
		if (isCode || (isLink && addCodeTagOnLink())) {
			if (isCode && fPreCounter > 0 && fBuf.lastIndexOf("<pre>") == fBuf.length() - 5) { //$NON-NLS-1$
				fInPreCodeCounter= fPreCounter - 1;
			}
			fBuf.append("<code>"); //$NON-NLS-1$
		}
		if (isReturn)
			fBuf.append(JavaDocMessages.JavadocContentAccess2_returns_pre);

		if (isLink || isLinkplain)
			handleLink(node.fragments());
		else if (isSummary)
			handleSummary(node.fragments());
		else if (isIndex)
			handleIndex(node.fragments());
		else if (isCode || isLiteral)
			handleContentElements(node.fragments(), true, node);
		else if (isReturn)
			handleContentElements(node.fragments(), false, node);
		else if (isSnippet) {
			handleSnippet(node);
		} else if (handleInheritDoc(node) || handleDocRoot(node)) {
			// Handled
		} else {
			//print uninterpreted source {@tagname ...} for unknown tags
			int start= node.getStartPosition();
			String text= fSource.substring(start, start + node.getLength());
			fBuf.append(removeDocLineIntros(text));
		}

		if (isReturn)
			fBuf.append(JavaDocMessages.JavadocContentAccess2_returns_post);
		if (isCode || (isLink && addCodeTagOnLink()))
			fBuf.append("</code>"); //$NON-NLS-1$
		if (isSnippet)
			fBuf.append("</code></pre>"); //$NON-NLS-1$
		if (isLiteral || isCode)
			fLiteralContent--;

	}

	protected boolean addCodeTagOnLink() {
		return true;
	}

	protected boolean handleValueTag(TagElement node) {

		List<? extends ASTNode> fragments= node.fragments();
		try {
			if (!(fElement instanceof IMember)) {
				return false;
			}
			if (fragments.isEmpty()) {
				if (fElement instanceof IField && JdtFlags.isStatic((IField) fElement) && JdtFlags.isFinal((IField) fElement)) {
					IField field= (IField) fElement;
					return handleConstantValue(field, false);
				}
			} else if (fragments.size() == 1) {
				Object first= fragments.get(0);
				if (first instanceof MemberRef) {
					MemberRef memberRef= (MemberRef) first;
					IType type= fElement instanceof IType ? (IType) fElement : ((IMember) fElement).getDeclaringType();
					if (memberRef.getQualifier() != null) {
						String[][] qualifierTypes= type.resolveType(memberRef.getQualifier().getFullyQualifiedName());
						if (qualifierTypes != null && qualifierTypes.length == 1) {
							type= type.getJavaProject().findType(String.join(".", qualifierTypes[0]), (IProgressMonitor) null); //$NON-NLS-1$
						}
					}
					SimpleName name= memberRef.getName();
					while (type != null) {
						IField field= type.getField(name.getIdentifier());
						if (field != null && field.exists()) {
							if (JdtFlags.isStatic(field) && JdtFlags.isFinal(field))
								return handleConstantValue(field, true);
							break;
						}
						type= type.getDeclaringType();
					}
				}
			}
		} catch (JavaModelException e) {
			JavaManipulationPlugin.log(e);
		}

		return false;
	}

	protected boolean handleConstantValue(IField field, boolean link) throws JavaModelException {
		String text= null;

		ISourceRange nameRange= field.getNameRange();
		if (SourceRange.isAvailable(nameRange)) {
			CompilationUnit cuNode= SharedASTProviderCore.getAST(field.getTypeRoot(), SharedASTProviderCore.WAIT_ACTIVE_ONLY, null);
			if (cuNode != null) {
				ASTNode nameNode= NodeFinder.perform(cuNode, nameRange);
				if (nameNode instanceof SimpleName) {
					IBinding binding= ((SimpleName) nameNode).resolveBinding();
					if (binding instanceof IVariableBinding) {
						IVariableBinding variableBinding= (IVariableBinding) binding;
						Object constantValue= variableBinding.getConstantValue();
						if (constantValue != null) {
							if (constantValue instanceof String) {
								text= ASTNodes.getEscapedStringLiteral((String) constantValue);
							} else {
								text= constantValue.toString(); // Javadoc tool is even worse for chars...
							}
						}
					}
				}
			}
		}

		if (text == null) {
			Object constant= field.getConstant();
			if (constant != null) {
				text= constant.toString();
			}
		}

		if (text != null) {
			text= HTMLBuilder.convertToHTMLContentWithWhitespace(text);
			if (link) {
				String uri;
				try {
					uri= createLinkURI(CoreJavaElementLinks.JAVADOC_SCHEME, field, null, null, null);
					fBuf.append(CoreJavaElementLinks.createLink(uri, text));
				} catch (URISyntaxException e) {
					JavaManipulationPlugin.log(e);
					return false;
				}
			} else {
				handleText(text);
			}
			return true;
		}
		return false;
	}

	protected boolean handleDocRoot(TagElement node) {
		if (!TagElement.TAG_DOCROOT.equals(node.getTagName()))
			return false;

		try {
			String url= null;
			if (fElement instanceof IMember && ((IMember) fElement).isBinary()) {
				URL javadocBaseLocation= CoreJavaDocLocations.getJavadocBaseLocation(fElement);
				if (javadocBaseLocation != null) {
					url= javadocBaseLocation.toExternalForm();
				}
			} else {
				IPackageFragmentRoot srcRoot= JavaModelUtil.getPackageFragmentRoot(fElement);
				if (srcRoot != null) {
					IResource resource= srcRoot.getResource();
					if (resource != null) {
						/*
						 * Too bad: Browser widget knows nothing about EFS and custom URL handlers,
						 * so IResource#getLocationURI() does not work in all cases.
						 * We only support the local file system for now.
						 * A solution could be https://bugs.eclipse.org/bugs/show_bug.cgi?id=149022 .
						 */
						IPath location= resource.getLocation();
						if (location != null) {
							url= location.toFile().toURI().toASCIIString();
						}
					}

				}
			}
			if (url != null) {
				if (url.endsWith("/")) { //$NON-NLS-1$
					url= url.substring(0, url.length() - 1);
				}
				fBuf.append(url);
				return true;
			}
		} catch (JavaModelException e) {
		}
		return false;
	}


	/**
	 * Handle {&#64;inheritDoc}.
	 *
	 * @param node the node
	 * @return <code>true</code> iff the node was an {&#64;inheritDoc} node and has been handled
	 */
	protected boolean handleInheritDoc(TagElement node) {
		if (!TagElement.TAG_INHERITDOC.equals(node.getTagName()))
			return false;
		try {
			if (fMethod == null)
				return false;

			TagElement blockTag= (TagElement) node.getParent();
			String blockTagName= blockTag.getTagName();

			if (blockTagName == null) {
				CharSequence inherited= fJavadocLookup.getInheritedMainDescription(fMethod);
				return handleInherited(inherited);

			} else
				switch (blockTagName) {
					case TagElement.TAG_PARAM: {
						List<? extends ASTNode> fragments= blockTag.fragments();
						int size= fragments.size();
						if (size > 0) {
							Object first= fragments.get(0);
							if (first instanceof SimpleName) {
								String name= ((SimpleName) first).getIdentifier();
								String[] parameterNames= fMethod.getParameterNames();
								for (int i= 0; i < parameterNames.length; i++) {
									if (name.equals(parameterNames[i])) {
										CharSequence inherited= fJavadocLookup.getInheritedParamDescription(fMethod, i);
										return handleInherited(inherited);
									}
								}
							} else if (size > 2 && first instanceof TextElement) {
								String firstText= ((TextElement) first).getText();
								if ("<".equals(firstText)) { //$NON-NLS-1$
									Object second= fragments.get(1);
									Object third= fragments.get(2);
									if (second instanceof SimpleName && third instanceof TextElement) {
										String thirdText= ((TextElement) third).getText();
										if (">".equals(thirdText)) { //$NON-NLS-1$
											String name= ((SimpleName) second).getIdentifier();
											ITypeParameter[] typeParameters= fMethod.getTypeParameters();
											for (int i= 0; i < typeParameters.length; i++) {
												ITypeParameter typeParameter= typeParameters[i];
												if (name.equals(typeParameter.getElementName())) {
													CharSequence inherited= getInheritedTypeParamDescription(i);
													return handleInherited(inherited);
												}
											}
										}
									}
								}
							}
						}
						break;
					}
					case TagElement.TAG_RETURN: {
						CharSequence inherited= fJavadocLookup.getInheritedReturnDescription(fMethod);
						return handleInherited(inherited);
					}
					case TagElement.TAG_THROWS:
					case TagElement.TAG_EXCEPTION: {
						List<? extends ASTNode> fragments= blockTag.fragments();
						if (fragments.size() > 0) {
							Object first= fragments.get(0);
							if (first instanceof Name) {
								String name= ASTNodes.getSimpleNameIdentifier((Name) first);
								CharSequence inherited= fJavadocLookup.getInheritedExceptionDescription(fMethod, name);
								return handleInherited(inherited);
							}
						}
						break;
					}
					default:
						break;
				}
		} catch (JavaModelException e) {
			JavaManipulationPlugin.log(e);
		}
		return false;
	}

	protected boolean handleInherited(CharSequence inherited) {
		if (inherited == null)
			return false;

		fBuf.append(inherited);
		return true;
	}

	protected void handleBlockTags(String title, List<TagElement> tags) {
		if (tags.isEmpty())
			return;

		handleBlockTagTitle(title);

		for (TagElement tag : tags) {
			handleSingleTag(tag);
		}
	}

	protected void handleSingleTag(TagElement tag) {
		fBuf.append(getBlockTagEntryStart());
		if (TagElement.TAG_SEE.equals(tag.getTagName())) {
			handleSeeTag(tag);
		} else {
			handleContentElements(tag.fragments());
		}
		fBuf.append(getBlockTagEntryEnd());
	}

	protected void handleReturnTag(TagElement tag, CharSequence returnDescription) {
		if (tag == null && returnDescription == null)
			return;

		handleBlockTagTitle(JavaDocMessages.JavaDoc2HTMLTextReader_returns_section);
		handleReturnTagBody(tag, returnDescription);
	}

	protected void handleReturnTagBody(TagElement tag, CharSequence returnDescription) {
		fBuf.append(getBlockTagEntryStart());
		if (tag != null)
			handleContentElements(tag.fragments());
		else
			fBuf.append(returnDescription);
		fBuf.append(getBlockTagEntryEnd());
	}

	protected void handleBlockTags(List<TagElement> tags) {
		for (TagElement tag : tags) {
			handleBlockTagTitle(tag.getTagName());
			handleBlockTagBody(tag);
		}
	}

	protected void handleBlockTagBody(TagElement tag) {
		fBuf.append(getBlockTagEntryStart());
		handleContentElements(tag.fragments());
		fBuf.append(getBlockTagEntryEnd());
	}

	protected void handleBlockTagTitle(String title) {
		fBuf.append(getBlockTagTitleStart());
		fBuf.append(title);
		fBuf.append(getBlockTagTitleEnd());
	}


	protected void handleSeeTag(TagElement tag) {
		handleLink(tag.fragments());
	}

	protected void handleExceptionTags(List<TagElement> tags, List<String> exceptionNames, CharSequence[] exceptionDescriptions) {
		if (tags.isEmpty() && containsOnlyNull(exceptionNames))
			return;

		handleBlockTagTitle(JavaDocMessages.JavaDoc2HTMLTextReader_throws_section);
		handleExceptionTagsBody(tags, exceptionNames, exceptionDescriptions);
	}

	protected void handleExceptionTagsBody(List<TagElement> tags, List<String> exceptionNames, CharSequence[] exceptionDescriptions) {
		for (TagElement tag : tags) {
			fBuf.append(getBlockTagEntryStart());
			handleThrowsTag(tag);
			fBuf.append(getBlockTagEntryEnd());
		}

		for (int i= 0; i < exceptionDescriptions.length; i++) {
			CharSequence description= exceptionDescriptions[i];
			String name= exceptionNames.get(i);
			if (name != null) {
				handleSingleException(name, description);
			}
		}
	}

	protected void handleSingleException(String name, CharSequence description) {
		fBuf.append(getBlockTagEntryStart());
		handleLink(Collections.singletonList(fJavadoc.getAST().newSimpleName(name)));
		if (description != null) {
			fBuf.append(JavaElementLabelsCore.CONCAT_STRING);
			fBuf.append(description);
		}
		fBuf.append(getBlockTagEntryEnd());
	}

	protected void handleThrowsTag(TagElement tag) {
		List<? extends ASTNode> fragments= tag.fragments();
		int size= fragments.size();
		if (size > 0) {
			handleLink(fragments.subList(0, 1));
			if (size > 1) {
				fBuf.append(JavaElementLabelsCore.CONCAT_STRING);
				handleContentElements(fragments.subList(1, size));
			}
		}
	}

	protected void handleSingleParameterTag(TagElement tag) {
		fBuf.append(getBlockTagEntryStart());
		handleParamTag(tag);
		fBuf.append(getBlockTagEntryEnd());
	}

	protected void handleParameterTags(List<TagElement> tags, List<String> parameterNames, CharSequence[] parameterDescriptions, boolean isTypeParameters) {
		if (tags.isEmpty() && containsOnlyNull(parameterNames))
			return;

		String tagTitle= isTypeParameters ? JavaDocMessages.JavaDoc2HTMLTextReader_type_parameters_section : JavaDocMessages.JavaDoc2HTMLTextReader_parameters_section;
		handleBlockTagTitle(tagTitle);

		for (TagElement tag : tags) {
			handleSingleParameterTag(tag);
		}
		for (int i= 0; i < parameterDescriptions.length; i++) {
			CharSequence description= parameterDescriptions[i];
			String name= parameterNames.get(i);
			if (name != null) {
				handleSingleParameterDescription(name, description, isTypeParameters);
			}
		}
	}

	protected void handleSingleParameterDescription(String name, CharSequence description, boolean isTypeParameters) {
		fBuf.append(getBlockTagEntryStart());
		fBuf.append(getParamNameStart());
		if (isTypeParameters) {
			fBuf.append("&lt;"); //$NON-NLS-1$
		}
		fBuf.append(name);
		if (isTypeParameters) {
			fBuf.append("&gt;"); //$NON-NLS-1$
		}
		fBuf.append(getParamNameEnd());
		if (description != null)
			fBuf.append(description);
		fBuf.append(getBlockTagEntryEnd());
	}

	protected void handleParamTag(TagElement tag) {
		List<? extends ASTNode> fragments= tag.fragments();
		int i= 0;
		int size= fragments.size();
		if (size > 0) {
			Object first= fragments.get(0);
			fBuf.append(getParamNameStart());
			if (first instanceof SimpleName) {
				String name= ((SimpleName) first).getIdentifier();
				fBuf.append(name);
				i++;
			} else if (first instanceof TextElement) {
				String firstText= ((TextElement) first).getText();
				if ("<".equals(firstText)) { //$NON-NLS-1$
					fBuf.append("&lt;"); //$NON-NLS-1$
					i++;
					if (size > 1) {
						Object second= fragments.get(1);
						if (second instanceof SimpleName) {
							String name= ((SimpleName) second).getIdentifier();
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
			fBuf.append(getParamNameEnd());

			handleContentElements(fragments.subList(i, fragments.size()));
		}
	}

	protected void handleSummary(List<? extends ASTNode> fragments) {
		int fs= fragments.size();
		if (fs > 0) {
			Object first= fragments.get(0);
			if (first instanceof TextElement) {
				TextElement memberRef= (TextElement) first;
				fBuf.append(getBlockTagTitleStart() + "Summary: " + memberRef.getText() + getBlockTagTitleEnd()); //$NON-NLS-1$
				return;
			}
		}
	}

	protected void handleSnippet(TagElement node) {
		if (node != null) {
			Object val= node.getProperty(TagProperty.TAG_PROPERTY_SNIPPET_IS_VALID);
			Object valError= node.getProperty(TagProperty.TAG_PROPERTY_SNIPPET_ERROR);
			if (val instanceof Boolean
					&& ((Boolean) val).booleanValue() && valError == null) {
				int fs= node.fragments().size();
				if (fs > 0) {
					fBuf.append("<pre>"); //$NON-NLS-1$
					Object valID= node.getProperty(TagProperty.TAG_PROPERTY_SNIPPET_ID);
					if (valID instanceof String && !valID.toString().isBlank()) {
						fBuf.append("<code id=" + valID.toString() + ">"); //$NON-NLS-1$ //$NON-NLS-2$
					} else {
						fBuf.append("<code>");//$NON-NLS-1$
					}
					fBuf.append(getBlockTagEntryStart());
					fSnippetStringEvaluator.AddTagElementString(node, fBuf);
					fBuf.append(getBlockTagEntryEnd());
				}
			} else {
				handleInvalidSnippet(node);
			}
		}
	}

	protected void handleInvalidSnippet(TagElement node) {
		fBuf.append("<pre><code>\n"); //$NON-NLS-1$
		fBuf.append("<mark>invalid @Snippet</mark>"); //$NON-NLS-1$
		Object val= node.getProperty(TagProperty.TAG_PROPERTY_SNIPPET_ERROR);
		if (val instanceof String) {
			fBuf.append("<br><p>" + val + "</p>"); //$NON-NLS-1$ //$NON-NLS-2$

		}
	}

	protected void handleIndex(List<? extends ASTNode> fragments) {
		int fs= fragments.size();
		if (fs > 0) {
			Object first= fragments.get(0);
			if (first instanceof TextElement) {
				TextElement memberRef= (TextElement) first;
				fBuf.append(memberRef.getText());
				return;
			}
		}
	}

	protected void handleLink(List<? extends ASTNode> fragments) {
		//TODO: Javadoc shortens type names to minimal length according to context
		int fs= fragments.size();
		if (fs > 0) {
			Object first= fragments.get(0);
			String refTypeName= null;
			String refMemberName= null;
			String[] refMethodParamTypes= null;
			String[] refMethodParamNames= null;
			if (first instanceof Name) {
				Name name= (Name) first;
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
				List<MethodRefParameter> params= methodRef.parameters();
				int ps= params.size();
				refMethodParamTypes= new String[ps];
				refMethodParamNames= new String[ps];
				for (int i= 0; i < ps; i++) {
					MethodRefParameter param= params.get(i);
					refMethodParamTypes[i]= ASTNodes.asString(param.getType());
					SimpleName paramName= param.getName();
					if (paramName != null)
						refMethodParamNames[i]= paramName.getIdentifier();
				}
			}

			if (refTypeName != null) {
				fBuf.append("<a href='"); //$NON-NLS-1$
				try {
					String scheme= CoreJavaElementLinks.JAVADOC_SCHEME;
					String uri= createLinkURI(scheme, fElement, refTypeName, refMemberName, refMethodParamTypes);
					fBuf.append(uri);
				} catch (URISyntaxException e) {
					JavaManipulationPlugin.log(e);
				}
				fBuf.append("'>"); //$NON-NLS-1$
				if (fs > 1 && ((fs != 2) || !CoreJavadocContentAccessUtility.isWhitespaceTextElement(fragments.get(1)))) {
					handleContentElements(fragments.subList(1, fs), true);
				} else {
					fBuf.append(refTypeName);
					if (refMemberName != null) {
						if (refTypeName.length() > 0) {
							fBuf.append('.');
						}
						fBuf.append(refMemberName);
						if (refMethodParamTypes != null && refMethodParamNames != null) {
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

	protected String createLinkURI(String scheme, @SuppressWarnings("unused") IJavaElement element, String refTypeName, String refMemberName, String[] refParameterTypes) throws URISyntaxException {
		return CoreJavaElementLinks.createURI(scheme, fElement, refTypeName, refMemberName, refParameterTypes);
	}

	protected boolean containsOnlyNull(List<String> parameterNames) {
		for (String string : parameterNames) {
			if (string != null)
				return false;
		}
		return true;
	}

	protected String getBlockTagStart() {
		return BLOCK_TAG_START;
	}
	protected String getBlockTagEnd() {
		return BLOCK_TAG_END;
	}
	protected String getBlockTagTitleStart() {
		return BlOCK_TAG_TITLE_START;
	}
	protected String getBlockTagTitleEnd() {
		return BlOCK_TAG_TITLE_END;
	}
	protected String getBlockTagEntryStart() {
		return BlOCK_TAG_ENTRY_START;
	}
	protected String getBlockTagEntryEnd() {
		return BlOCK_TAG_ENTRY_END;
	}
	protected String getParamNameStart() {
		return PARAM_NAME_START;
	}
	protected String getParamNameEnd() {
		return PARAM_NAME_END;
	}
}
