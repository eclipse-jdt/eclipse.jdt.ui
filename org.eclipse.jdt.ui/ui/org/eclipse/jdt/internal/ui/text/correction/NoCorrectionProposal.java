
package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.internal.ui.JavaPluginImages;


public class NoCorrectionProposal implements ICompletionProposal {

	private ProblemPosition fProblemPosition;

	public NoCorrectionProposal(ProblemPosition problemPosition) {
		fProblemPosition= problemPosition;
	}

	/*
	 * @see ICompletionProposal#apply(IDocument)
	 */
	public void apply(IDocument document) {
		// do nothing
	}
	

	/*
	 * @see ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		StringBuffer buf= new StringBuffer();
		buf.append("<p><b>");
		buf.append(getErrorCode(fProblemPosition.getId()));
		buf.append("</b></p>");
		buf.append("<p>");
		buf.append(fProblemPosition.getMessage());
		buf.append("</p>");
		String[] arg= fProblemPosition.getArguments();
		if (arg != null) {
			for (int i= 0; i < arg.length; i++) {
				buf.append("<p>");
				buf.append(arg[i]);
				buf.append("</p>");				
			}
		}
	
		return buf.toString();
	}
	
	private String getErrorCode(int code) {
		StringBuffer buf= new StringBuffer();
		
		if ((code & IProblem.TypeRelated) != 0) {
			buf.append("TypeRelated + ");
		}
		if ((code & IProblem.FieldRelated) != 0) {
			buf.append("FieldRelated + ");
		}
		if ((code & IProblem.ConstructorRelated) != 0) {
			buf.append("ConstructorRelated + ");
		}
		if ((code & IProblem.MethodRelated) != 0) {
			buf.append("MethodRelated + ");
		}
		if ((code & IProblem.ImportRelated) != 0) {
			buf.append("ImportRelated + ");
		}
		if ((code & IProblem.Internal) != 0) {
			buf.append("Internal + ");
		}
		if ((code & IProblem.Syntax) != 0) {
			buf.append("Syntax + ");
		}
		buf.append(code & IProblem.IgnoreCategoriesMask);
		
		return buf.toString();
	}

	/*
	 * @see ICompletionProposal#getContextInformation()
	 */
	public IContextInformation getContextInformation() {
		return null;
	}

	/*
	 * @see ICompletionProposal#getDisplayString()
	 */
	public String getDisplayString() {
		return "No correction available";
	}

	/*
	 * @see ICompletionProposal#getImage()
	 */
	public Image getImage() {
		return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_JAVA_MODEL);
	}

	/*
	 * @see ICompletionProposal#getSelection(IDocument)
	 */
	public Point getSelection(IDocument document) {
		return new Point(fProblemPosition.getOffset(), fProblemPosition.getLength());
	}

}
