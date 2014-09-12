package ui;

import core.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.ArrayList;

class PowerOff {
	JButton off;
	ArrayList<String> vmName = InformationCenter.getONVMNameList();
	JCheckBox cb[]=new JCheckBox[vmName.size()]; 


	PowerOff() {
		buildGUI();
		hookUpEvents();
	}

	public void buildGUI() {
		JFrame fr=new JFrame();
		JPanel p=new JPanel();
		off=new JButton("PowerOff");
		for(int i=0;i<vmName.size();i++) {
			cb[i]=new JCheckBox(vmName.get(i));
			cb[i].setVisible(true);
		}
		fr.setTitle("Power OFF VM");
		fr.add(p);

		for(int i=0;i<vmName.size();i++) {
			p.add(cb[i],BorderLayout.BEFORE_LINE_BEGINS);
		}
		p.add(off,BorderLayout.AFTER_LINE_ENDS);
		fr.setVisible(true);
		fr.setSize(400,400);
	}
	public void hookUpEvents() {
		off.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				String msg = "";
				for(int i=0;i<vmName.size();i++) {
					cb[i].setVisible(true);
					if (cb[i].isSelected()) {
						msg = msg.concat(cb[i].getText() + ", ");
						VM vm = new VM();
						vm.powerOFF(cb[i].getText());
					}
				}
				JOptionPane.showMessageDialog(null, "Selected Vm: " + msg + "powered OFF");
			}
		});
	}
}
