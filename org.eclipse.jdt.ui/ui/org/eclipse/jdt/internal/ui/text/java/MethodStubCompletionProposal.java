
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.util.Assert;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility.GenStubSettings;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class MethodStubCompletionProposal extends JavaCompletionProposal {
	
	private String fTypeName;
	private String fMethodName;
	private String[] fParamTypes;
	private ICompilationUnit fCompilationUnit;
	private IJavaProject fJavaProject;
	
	private ImportsStructure fImportStructure;

	public MethodStubCompletionProposal(IJavaProject jproject, ICompilationUnit cu, String declaringTypeName, String methodName, String[] paramTypes, int start, int length, String displayName, String completionProposal) {
		super(completionProposal, start, length, null, displayName, 0);
		Assert.isNotNull(jproject);
		Assert.isNotNull(methodName);
		Assert.isNotNull(declaringTypeName);
		Assert.isNotNull(paramTypes);

		fTypeName= declaringTypeName;
		fParamTypes= paramTypes;
		fMethodName= methodName;

		fJavaProject= jproject;
		fCompilationUnit= cu; // can be null -> no imports
		
		fImportStructure= null;
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
			fImportStructure= null;
			IType declaringType= fJavaProject.findType(fTypeName);
			if (declaringType != null) {
				IMethod method= JavaModelUtil.findMethod(fMethodName, fParamTypes, false, declaringType);
				if (method != null) {
					GenStubSettings settings= new GenStubSettings(JavaPreferencesSettings.getCodeGenerationSettings());
					IType definingType= null;
					if (fCompilationUnit != null) {
						fImportStructure= new ImportsStructure(fCompilationUnit, settings.importOrder, settings.importThreshold, true);
						IJavaElement currElem= fCompilationUnit.getElementAt(offset);
						if (currElem != null) {
							definingType= (IType) currElem.getAncestor(IJavaElement.TYPE);
						}
					}

					settings.noBody= (definingType != null) && definingType.isInterface();
					settings.callSuper= declaringType.isClass() && !Flags.isAbstract(method.getFlags()) && !Flags.isStatic(method.getFlags());
					settings.methodOverwrites= true;

					String stub= StubUtility.genStub(fTypeName, method, settings, fImportStructure);
					
					// use the code formatter
					String lineDelim= StubUtility.getLineDelimiterFor(document);
					IRegion region= document.getLineInformationOfOffset(getReplacementOffset());
					int lineStart= region.getOffset();
					int indent= Strings.computeIndent(document.get(lineStart, getReplacementOffset() - lineStart), settings.tabWidth);
					
					String replacement= StubUtility.codeFormat(stub, indent, lineDelim);
					replacement= Strings.trimLeadingTabsAndSpaces(replacement);
					
					setReplacementString(replacement);
					setCursorPosition(replacement.length());
				}
			}
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
			
		super.apply(document, trigger, offset);
	}
	
}

