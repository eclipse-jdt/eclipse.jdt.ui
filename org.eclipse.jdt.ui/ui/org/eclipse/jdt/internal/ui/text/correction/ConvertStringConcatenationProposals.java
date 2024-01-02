/*******************************************************************************
 *  Copyright (c) 2020 Julian Honnen
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Julian Honnen <julian.honnen@vector.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Collection;

import org.eclipse.jdt.internal.corext.fix.ConvertToMessageFormatFixCore;
import org.eclipse.jdt.internal.corext.fix.ConvertToStringBufferFixCore;
import org.eclipse.jdt.internal.corext.fix.ConvertToStringFormatFixCore;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposal;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;
import org.eclipse.swt.graphics.Image;

final class ConvertStringConcatenationProposals {
	private ConvertStringConcatenationProposals() {
	}

	public static boolean getProposals(IInvocationContext context, Collection<ICommandAccess> resultingCollections) {
		var compilationUnit= context.getASTRoot();
		var node= context.getCoveringNode();

		var convertToStringBufferFix= ConvertToStringBufferFixCore.createConvertToStringBufferFix(compilationUnit, node);
		var convertToMessageFormatFix= ConvertToMessageFormatFixCore.createConvertToMessageFormatFix(compilationUnit, node);
		var convertToStringFormatFix= ConvertToStringFormatFixCore.createConvertToStringFormatFix(compilationUnit, node);

		if (convertToStringBufferFix == null && convertToMessageFormatFix == null && convertToStringFormatFix == null) {
			return false;
		}

		if (resultingCollections == null) {
			return true;
		}

		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);

		if (convertToStringBufferFix != null) {
			FixCorrectionProposal proposal= new FixCorrectionProposal(convertToStringBufferFix, null, IProposalRelevance.CONVERT_TO_STRING_BUFFER, image, context);
			proposal.setCommandId(QuickAssistProcessor.CONVERT_TO_STRING_BUFFER_ID);
			resultingCollections.add(proposal);
		}

		if (convertToMessageFormatFix != null) {
			FixCorrectionProposal proposal= new FixCorrectionProposal(convertToMessageFormatFix, null, IProposalRelevance.CONVERT_TO_MESSAGE_FORMAT, image, context);
			proposal.setCommandId(QuickAssistProcessor.CONVERT_TO_MESSAGE_FORMAT_ID);
			resultingCollections.add(proposal);
		}

		if (convertToStringFormatFix != null) {
			FixCorrectionProposal proposal= new FixCorrectionProposal(convertToStringFormatFix, null, IProposalRelevance.CONVERT_TO_MESSAGE_FORMAT, image, context);
			proposal.setCommandId(QuickAssistProcessor.CONVERT_TO_MESSAGE_FORMAT_ID);
			resultingCollections.add(proposal);
		}

		return true;
	}
}
