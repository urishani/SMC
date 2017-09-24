
/*
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
/*
 *+------------------------------------------------------------------------+
 *|                                   |
 *| Copyright IBM Corp. 2011-2013.
 *|                                                                        |
 *+------------------------------------------------------------------------+
*/

/**
 * Licensed Material - Property of IBM
 * Copyright IBM  2013 All Rights Reserved
 *
 *  The work leading to these results have received funding from the Seventh Framework Programme
 *  SPRINT ICT-2009.1.3  Project Number: 257909
 *
 * The information in this document is provided "as is",
 * and no guarantee or warranty is given that the information is fit for any particular purpose.
 * The user uses the information at its sole risk and liability.
 *
 */

package com.ibm.rhapsody.sm.plugin;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class PasswordDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = 1L;
		private static String OK = "ok";
		private static String CANCEL = "cancel";
		private static String AUTH = "auth";

//		private Frame frame = new Frame();
		private JPasswordField pwdField;
		private JCheckBox useAuth;
		private JTextField userField;
		private String mPwd;
		private String mUser;
		private boolean mCancel = true;
		private String mTitle = "User Credentials";

		/**
		 * Opens up a dialog for a user and password.<br>
		 * To activate, create a new instance which will start up the dialog.
		 * Than, get status of the dialog via the getters:<br><ul>
		 * <li> <i>isOk()</i> which means OK button was clicked and dialog is not cancelled or closed.
		 * <li> <i>getUser()</i> to get final user value.
		 * <li> <i>getPwd()</i> to get final password value.
		 * </ul>
		 * See <i>main()</i> for example below.
		 * @param user String for the initial value for the user. Can be null to mean empty string - no initial value.
		 * @param pwd String for the initial value for the password. Can be null to mean empty string - no initial value.
		 * @param title String for the title of the dialog. If null - default title is "User Credentials".
		 * @param pDoAuth boolean indicating the  initial value of "Use Authendication" checkbox in teh dialog.
		 */
		public PasswordDialog(String user, String pwd, String title, boolean pDoAuth) {
			super(new Frame());
			//Create everything.
			this.mPwd = pwd;
			this.mUser = user;
			if (null != title)
				mTitle = title;
			
			pwdField = new JPasswordField(10);
//			pwdField.setActionCommand(OK);
//			pwdField.addActionListener(this);

			userField = new JTextField(10);
			useAuth = new JCheckBox("Use Authentication", pDoAuth);

			JLabel userLabel = new JLabel("User name: ");
			JLabel pwdLabel = new JLabel("Password: ");
			pwdLabel.setLabelFor(pwdField);
			userLabel.setLabelFor(userField);
			pwdField.setEnabled(pDoAuth);
			userField.setEnabled(pDoAuth);

			JComponent buttonsPane = new JPanel(new GridLayout(0,1));
			JButton okButton = new JButton(OK);
			JButton cancelButton = new JButton(CANCEL);

			okButton.setActionCommand(OK);
			cancelButton.setActionCommand(CANCEL);
			okButton.addActionListener(this);
			cancelButton.addActionListener(this);
			
			useAuth.setActionCommand(AUTH);
			useAuth.addActionListener(this);

			buttonsPane.add(okButton);
			buttonsPane.add(cancelButton);

			//Lay out everything.
			JPanel userPane = new JPanel(new FlowLayout(FlowLayout.TRAILING));
			userPane.add(userLabel);
			userPane.add(userField);
			JPanel pwdPane = new JPanel(new FlowLayout(FlowLayout.TRAILING));
			pwdPane.add(pwdLabel);
			pwdPane.add(pwdField);
			JPanel fieldsPane = new JPanel(new GridLayout(0,1));
			fieldsPane.add(useAuth);
			fieldsPane.add(userPane);
			fieldsPane.add(pwdPane);
			JPanel contentsPane =  new JPanel(new FlowLayout(FlowLayout.TRAILING));
			contentsPane.add(fieldsPane);
			contentsPane.add(buttonsPane);

			setTitle(mTitle);
			add(contentsPane);

			userField.setText(mUser);
			pwdField.setText(mPwd);
			setModal(true);
			pack();
			setVisible(true);
		}

		public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand();
			if (AUTH.equals(cmd)) { // auth checkbox clicked
				boolean doAuth = useAuth.isSelected();
				userField.setEnabled(doAuth);
				pwdField.setEnabled(doAuth);
				return;
			} else if (OK.equals(cmd)) { //Process the password.
				mPwd = new String(pwdField.getPassword());
				mUser = new String(userField.getText());
				mCancel = false;
			} else { //The user has asked for cancel
				mCancel = true;
			}
			dispose();
		}

		public boolean isOk() {
			return mCancel == false;
		}

		public boolean doAuth() {
			return useAuth.isSelected();
		}
		public String getUser() {
			return mUser;
		}

		public String getPwd() {
			return mPwd;
		}

		public static void main(String[] args) {
			PasswordDialog dialog = new PasswordDialog("uri", "pwd", null, true);
					if (dialog.isOk() == false)
						System.out.println("Cancelled");
					else
						System.out.println("user [" + dialog.getUser() + "]\npwd [" + dialog.getPwd() + "]\ndoAuth[" + dialog.doAuth() + "]");
					System.exit(0);
		}
}