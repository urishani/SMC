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

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;

public abstract class SmDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = 1L;

	protected static final String OK = "OK";
	protected static final String CANCEL = "Cancel";
	protected static final String APPLY = "Apply";
	protected static final String TRY = "Try";

	private final JPanel outerPanel;

	protected SmDialog(String title) {
		super(new JFrame());
        setTitle(title);
        outerPanel = new JPanel();
        outerPanel.setLayout(new BoxLayout(outerPanel, BoxLayout.Y_AXIS));
        outerPanel.setBorder(new EmptyBorder(10, 10, 5, 10));
        add(outerPanel);

    }

	protected JPanel addBorderedPanel(String title) {
        JPanel innerPanel = new JPanel(new GridLayout(0, 1));
        innerPanel.setBorder(new TitledBorder(title));
        outerPanel.add(innerPanel);
        return innerPanel;
	}

	protected JPanel addPlainPanel() {
        JPanel innerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        innerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        outerPanel.add(innerPanel);
        return innerPanel;
	}

	protected JComponent makeLabeledField(String label, JComponent field) {
		return makeLabeledField(label, field, -1, 5, 5);
	}

	protected JComponent makeLabeledField(String label, JComponent field, int margin, int hgap, int vgap) {
		JPanel fieldPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, hgap, vgap));
        if (margin > 0)
            fieldPanel.setBorder(new EmptyBorder(0, margin, 0, 0));
        if (label != null) {
            JLabel fieldLabel = new JLabel(label);
            fieldLabel.setLabelFor(field);
            fieldPanel.add(fieldLabel);
        }
        fieldPanel.add(field);
        return fieldPanel;
	}

	private void addField(JComponent owner, String label, JComponent field) {
        owner.add(makeLabeledField(label, field));
	}

	protected void addField(JComponent owner, String label, int size) {
		addField(owner, label, new JTextField(size));
	}

	protected void addPasswordField(JComponent owner, String label, int size) {
		addField(owner, label, new JPasswordField(size));
	}

	protected void addCheckbox(JComponent owner, String label) {
		owner.add(new JCheckBox(label));
	}

	protected JComponent makeField(String label, int size) {
		return makeLabeledField(label, new JTextField(size));
	}

	private JComponent makeRow (LayoutManager layout, JComponent ... children) {
        JPanel rowPanel = new JPanel(layout);
        for (JComponent child : children)
        	rowPanel.add(child);
        return rowPanel;
	}

	protected void append (JComponent owner, JComponent ... children) {
		owner.add(makeRow(new FlowLayout(FlowLayout.LEADING, 0, 0), children));
	}

	protected void appendr (JComponent owner, JComponent ... children) {
		owner.add(makeRow(new FlowLayout(FlowLayout.TRAILING, 0, 0), children));
	}

	protected void appendlr (JComponent owner, JComponent left, JComponent right) {
		appendlr(owner, left, right, 0, 0, 0);
	}

	protected void appendlr (JComponent owner, JComponent left, JComponent right, int margin, int hgap, int vgap) {
        JPanel rowPanel = new JPanel();
        rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.LINE_AXIS));
        if (hgap + vgap > 0)
            rowPanel.setBorder(new EmptyBorder(vgap, 0, 0, 0));
        rowPanel.add(makeRow(new FlowLayout(FlowLayout.LEADING, margin, 0), left));
        rowPanel.add(makeRow(new FlowLayout(FlowLayout.TRAILING, 5, 0), right));
        owner.add(rowPanel);
	}

	protected void addButtons(String ... buttons) {
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        buttonsPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        for (String b : buttons) {
            JButton button = new JButton(b);
            button.setActionCommand(b);
            button.addActionListener(this);
            button.setPreferredSize(new Dimension(80, 22));
            buttonsPanel.add(button);
        }
        outerPanel.add(buttonsPanel);
	}

	protected void display(Component parent) {
        setModal(true);
        setResizable(false);
        pack();
        if (parent != null)
            setLocationRelativeTo(parent);
        else
        	setLocationByPlatform(true);
        setVisible(true);
	}

	protected void setEnabled(JComponent field, boolean enable) {
		field.setEnabled(enable);
		field.setOpaque(enable);
		field.repaint();
	}

	private void align(final AbstractButton cell, final JComponent enable, final JComponent ... disable) {
		setEnabled(enable, cell.isSelected());
		if (cell.isSelected())
			for (JComponent field : disable)
				setEnabled(field, false);
	}

	protected void toggle(final AbstractButton cell, final JComponent enable, final JComponent ... disable) {
		align(cell, enable, disable);
		cell.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent paramActionEvent) {
				align(cell, enable, disable);
			}
		});
	}

	public void actionPerformed(ActionEvent paramActionEvent) {
		System.out.println("ACTION ON '" + getTitle() + "': " + paramActionEvent.getActionCommand());
	}

	protected static void execute(final Class<?> c) {
        Thread t = new Thread() {
            @Override
        	public void run() {
            	try {
                	c.newInstance();
                	System.exit(0);
            	} catch (Exception e) {
            		throw new Error(e);
            	}
            };
        };
        SwingUtilities.invokeLater(t);
	}
}
