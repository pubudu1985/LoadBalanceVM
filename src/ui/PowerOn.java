package ui;

import core.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.ArrayList;

class PowerOn {
	JButton on;
	ArrayList<String> vmName = InformationCenter.getOFFVMNameList();
	JCheckBox cb[]=new JCheckBox[vmName.size()]; 


	PowerOn() {
		buildGUI();
		hookUpEvents();
	}

	public void buildGUI() {
		JFrame fr=new JFrame();
		JPanel p=new JPanel();
		on=new JButton("PowerOn");
		for(int i=0;i<vmName.size();i++) {
			cb[i]=new JCheckBox(vmName.get(i));
			cb[i].setVisible(true);
		}
		fr.setTitle("Power ON VM");
		fr.add(p);

		for(int i=0;i<vmName.size();i++) {
			p.add(cb[i],BorderLayout.BEFORE_LINE_BEGINS);
		}
		p.add(on,BorderLayout.AFTER_LINE_ENDS);
		fr.setVisible(true);
		fr.setSize(400,400);
	}
	public void hookUpEvents() {
		on.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				String msg = "";
				for(int i=0;i<vmName.size();i++) {
					cb[i].setVisible(true);
					if (cb[i].isSelected()) {
						msg = msg.concat(cb[i].getText() + ", ");
						VM vm = new VM();
						vm.powerON(cb[i].getText());
					}
				}
				JOptionPane.showMessageDialog(null, "Selected Vm: " + msg + "powered ON");
			}
		});
	}
}
