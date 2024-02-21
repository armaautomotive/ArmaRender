/* Copyright (C) 2023 by Jon Taylor
   
   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender.ui;

import armarender.*;
import armarender.animation.*;
import armarender.animation.distortion.*;
import armarender.material.*;
import armarender.math.*;
import armarender.texture.*;
import java.lang.ref.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;

public class ProgressDialog extends Thread
{
    JFrame progressDialog;
    boolean progressDialogRunning = true;
    private JProgressBar dpb;

    public ProgressDialog(String title){
        progressDialog = new JFrame(title); // JDialog
        progressDialog.setLayout(new GridLayout());
        progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        
        //progressDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        progressDialog.setLocation((int)(screenSize.getWidth() / 2) - (300/2), (int) ((screenSize.getHeight()/(float)2) - ((float)75/(float)2.0)));
        
        JPanel panel = new JPanel();
        panel.setSize(400, 175);
        //progressDialog.getContentPane();
          JLabel label = new JLabel(title);
          Dimension size = label.getPreferredSize();
          label.setBounds(150, 100, size.width, size.height);
          //panel.setLayout(null);           // Breaks the JFrame
          panel.add(label);
        
        progressDialog.setSize(400, 175);
        
        dpb = new JProgressBar(0, 100);
        dpb.setValue(50);
        dpb.setSize(new Dimension(80, 20));
        //dpb.setStringPainted(true);
        dpb.setEnabled(true);
        //progressDialog.add(BorderLayout.CENTER, dpb);
        //progressDialog.add(dpb);
        panel.add(dpb);
        
        
        JLabel progressLabel = new JLabel(title);
        progressLabel.setBounds(50, 10, 200, 30);
        //progressDialog.add(BorderLayout.NORTH, progressLabel);
        //progressDialog.getContentPane().add(progressLabel);
        panel.add(progressLabel);
        
        progressDialog.getContentPane().add(panel);
        
        //progressDialog.getContentPane().setBackground( Color.red );
        progressDialog.pack();
        //progressDialog.setVisible(true);
    }

    public void run(){
        int count = 0;
        while(progressDialogRunning){
            if(count > 5){
                progressDialog.setVisible(true);
                
                // Set Progress bar
                
            }
            try {
                Thread.sleep(50);
            } catch (Exception e){
            }
            count++;
        }
    }
    
    public void close(){
        progressDialogRunning = false;
        progressDialog.setVisible(false);
    }
}

