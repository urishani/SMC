/**
  Copyright 2011-2016 IBM
	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at
	http://www.apache.org/licenses/LICENSE-2.0
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.

*/
/**
 *  The work leading to these results have received funding from the Seventh Framework Programme
 *  SPRINT ICT-2009.1.3  Project Number: 257909
 *
 *
 */
package com.ibm.haifa.smc.client;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class ConfirmDialog extends SmDialog {
	private static final long serialVersionUID = 1L;

	private ConfirmDialog(String title, String question, Component parent) {
		super(title);
		JPanel innerPanel = addPlainPanel();
		JLabel label = new JLabel(question);
		innerPanel.add(label);
		addButtons(OK, CANCEL);
		display(parent);
    }

	public static boolean ask(String title, String question, Component parent) {
		ConfirmDialog dialog = new ConfirmDialog(title, question, parent);
		return dialog.ok;
	}

	private boolean ok = false;

	public void actionPerformed(ActionEvent paramActionEvent) {
		ok = "OK".equals(paramActionEvent.getActionCommand());
		dispose();
	}

    public static void main(String[] args) throws Exception {
    	ask("Clear associations", "Clear local remote associations?", null);
    }
}
