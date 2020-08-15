package junit.swingui;

/*-
 * #%L
 * org.eclipse.jdt.ui.junit.sampleproject
 * %%
 * Copyright (C) 2020 Eclipse Foundation
 * %%
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 * #L%
 */

import java.awt.*;

import javax.swing.*;
import javax.swing.border.BevelBorder;

/**
 * A status line component.
 */
public class StatusLine extends JTextField {
	public static final Font PLAIN_FONT = new Font("dialog", Font.PLAIN, 12);
	public static final Font BOLD_FONT = new Font("dialog", Font.BOLD, 12);

	public StatusLine(int preferredWidth) {
		super();
		setFont(BOLD_FONT);
		setEditable(false);
		setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		Dimension d = getPreferredSize();
		d.width = preferredWidth;
		setPreferredSize(d);
	}

	public void showInfo(String message) {
		setFont(PLAIN_FONT);
		setForeground(Color.black);
		setText(message);
	}

	public void showError(String status) {
		setFont(BOLD_FONT);
		setForeground(Color.red);
		setText(status);
		setToolTipText(status);
	}

	public void clear() {
		setText("");
		setToolTipText(null);
	}
}
