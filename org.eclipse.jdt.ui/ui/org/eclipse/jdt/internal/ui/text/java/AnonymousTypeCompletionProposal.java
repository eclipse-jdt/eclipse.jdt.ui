package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.textmanipulation.TextUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.OverrideMethodQuery;
import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.ImportOrganizePreferencePage;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class AnonymousTypeCompletionProposal extends JavaCompletionProposal {
	
	private IType fDeclaringType;
	private ICompilationUnit fCompilationUnit;
	
	private ImportsStructure fImportStructure;

	public AnonymousTypeCompletionProposal(IJavaProject jproject, ICompilationUnit cu, int start, int length, String constructorCompletion, String displayName, String declaringTypeName) {
		super(constructorCompletion, start, length, null, displayName);
		Assert.isNotNull(declaringTypeName);
		Assert.isNotNull(jproject);
		
		fCompilationUnit= cu; // can be null -> no imports
		
		fDeclaringType= getDeclaringType(jproject, declaringTypeName);
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
	 * @see JavaCompletionProposal#applyImports(IDocument)
	 */
	protected void applyImports(IDocument document) {
		if (fImportStructure != null) {
			try {
				fImportStructure.create(false, null);
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
	}

	/*
	 * @see ICompletionProposalExtension#apply(IDocument, char)
	 */
	public void apply(IDocument document, char trigger, int offset) {
		try {
			if (fCompilationUnit == null) {
				fImportStructure= null;
			} else {
				String[] prefOrder= ImportOrganizePreferencePage.getImportOrderPreference();
				int threshold= ImportOrganizePreferencePage.getImportNumberThreshold();					
				fImportStructure= new ImportsStructure(fCompilationUnit, prefOrder, threshold, true);
			}
			
			String replacementString= getReplacementString();
			
			// construct replacement text
			StringBuffer buf= new StringBuffer();
			buf.append(replacementString);
			if (!replacementString.endsWith(")")) {
				buf.append(')');
			}	
			buf.append(" {\n");
			if (!createStubs(buf, fImportStructure)) {
				return;
			}
			buf.append("}");
			
			// use the code formatter
			String lineDelim= StubUtility.getLineDelimiterFor(document);
			int tabWidth= CodeFormatterPreferencePage.getTabSize();
			IRegion region= document.getLineInformationOfOffset(getReplacementOffset());
			int indent= TextUtil.getIndent(document.get(region.getOffset(), region.getLength()), tabWidth);
			
			String replacement= StubUtility.codeFormat(buf.toString(), indent, lineDelim);
			replacement= TextUtil.removeLeadingWhiteSpaces(replacement);
			
			setReplacementString(replacement);
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
			
		super.apply(document, trigger, offset);
	}
	
	private boolean createStubs(StringBuffer buf, ImportsStructure imports) throws JavaModelException {
		if (fDeclaringType == null) {
			return true;
		}
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();

		ITypeHierarchy hierarchy= fDeclaringType.newSupertypeHierarchy(null);
		OverrideMethodQuery selectionQuery= fDeclaringType.isClass() ? new OverrideMethodQuery(JavaPlugin.getActiveWorkbenchShell(), true) : null;
		String[] unimplemented= StubUtility.evalUnimplementedMethods(fDeclaringType, hierarchy, true, settings, selectionQuery, imports);
		if (unimplemented != null) {
			for (int i= 0; i < unimplemented.length; i++) {
				buf.append(unimplemented[i]);
				buf.append('\n');
			}
			return true;
		}
		return false;
	}

}

