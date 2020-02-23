/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.refactoring.infra;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Display;


//have to create this class because of bug 40095
public class MockClipboard extends Clipboard{

	private Map<Transfer, Object> fContents; //Transfer -> Object

	public MockClipboard(Display display) {
		super(display);
		fContents= new HashMap<>();
	}

	@Override
	protected void checkSubclass() {
		//do nothing
	}

	@Override
	public TransferData[] getAvailableTypes() {
		Set<TransferData> result= new HashSet<>();
		for (Transfer transfer : fContents.keySet()) {
			result.addAll(Arrays.asList(transfer.getSupportedTypes()));
		}
		return result.toArray(new TransferData[result.size()]);
	}

	@Override
	public Object getContents(Transfer transfer) {
		return fContents.get(transfer);
	}

	@Override
	public void setContents(Object[] data, Transfer[] dataTypes) {
		if (data == null || dataTypes == null || data.length != dataTypes.length) {
			DND.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		fContents.clear();
		for (int i= 0; i < dataTypes.length; i++) {
			fContents.put(dataTypes[i], data[i]);
		}
	}

	@Override
	public void dispose() {
		fContents.clear();
		super.dispose();
	}
}
