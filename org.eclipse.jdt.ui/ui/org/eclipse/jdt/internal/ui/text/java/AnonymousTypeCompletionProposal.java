package org.eclipse.jdt.internal.ui.text.java;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.TextUtilities;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.ui.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage;
import org.eclipse.jdt.internal.ui.util.JavaModelUtil;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.util.Assert;
import org.eclipse.swt.graphics.Image;

public class AnonymousTypeCompletionProposal extends JavaCompletionProposal {
	
	private IType fDeclaringType;
	private ICompilationUnit fCompilationUnit;
	
	private ImportsStructure fImportStructure;

	public AnonymousTypeCompletionProposal(ICompilationUnit cu, int start, int length, String constructorCompletion, String displayName, String declaringTypeName) {
		super(constructorCompletion, start, length, null, displayName);
		Assert.isNotNull(cu);
		Assert.isNotNull(declaringTypeName);
		
		fCompilationUnit= cu;
		fDeclaringType= getDeclaringType(cu.getJavaProject(), declaringTypeName);
		setImage(getImageForType(fDeclaringType));
		setCursorPosition(constructorCompletion.indexOf('(') + 1);
	}

	private Image getImageForType(IType type) {
		String imageName= JavaPluginImages.IMG_OBJS_CLASS; // default
		if (type != null) {
			try {
				if (type.isInterface()) {
					imageName= JavaPluginImages.IMG_OBJS_INTERFACE;
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		return JavaPluginImages.get(imageName);
	}
	
	private IType getDeclaringType(IJavaProject project, String typeName) {
		try {
			return JavaModelUtil.findType(project, typeName);
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return null;
	}
	
	/*
	 * @see JavaCompletionProposal#applyImport(IDocument)
	 */
	protected void applyImport(IDocument document) {
		if (fImportStructure != null) {
			try {
				fImportStructure.create(document, null);
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
	}

	/*
	 * @see ICompletionProposalExtension#apply(IDocument, char)
	 */
	public void apply(IDocument document, char trigger) {
		try {
			fImportStructure= new ImportsStructure(fCompilationUnit);
			
			StringBuffer buf= new StringBuffer();
			buf.append(getReplacementString());
			buf.append(" {\n");
			createStubs(buf, fImportStructure);
			buf.append("};");
			
			String lineDelim= StubUtility.getLineDelimiterFor(document);
			int tabWidth= CodeFormatterPreferencePage.getTabSize();
			IRegion region= document.getLineInformationOfOffset(getReplacementOffset());
			int indent= TextUtilities.getIndent(document.get(region.getOffset(), region.getLength()), tabWidth);
			
			String replacement= StubUtility.codeFormat(buf.toString(), indent, lineDelim);
			replacement= TextUtilities.removeLeadingWhiteSpaces(replacement);
			
			setReplacementString(replacement);
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
			
		super.apply(document, trigger);
	}
	
	private void createStubs(StringBuffer buf, ImportsStructure imports) throws JavaModelException {
		if (fDeclaringType == null) {
			return;
		}
		ArrayList res= new ArrayList();
		ITypeHierarchy hierarchy= fDeclaringType.newSupertypeHierarchy(null);
		StubUtility.evalUnimplementedMethods(fDeclaringType, hierarchy, true, res, imports);
		
		for (int i= 0; i < res.size(); i++) {
			buf.append((String) res.get(i));
			buf.append('\n');
		}
	}

}

