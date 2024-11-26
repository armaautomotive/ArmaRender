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

import java.awt.event.*;

public class ProgressDialog extends Thread
{
    JFrame progressDialog;
    boolean progressDialogRunning = true;
    private JProgressBar dpb;
    private JProgressBar dpb2;
    //boolean running = true;
    String title = "";
    boolean directionRight = true;

    public ProgressDialog(String title){
        this.title = title;
    }
    
    public void init(){
        progressDialog = new JFrame(title); // JDialog
        //progressDialog.setLayout(new GridLayout());
        //progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        
        progressDialog.setAlwaysOnTop(true); // requires focus be kept.
        
        ImageIcon iconImage = new ImageIcon(getClass().getResource("/armarender/Icons/favicon-32x32.png"));
        progressDialog.setIconImage(iconImage.getImage());
        
        progressDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("ProgressDialog Closing");
                close();
            }
        });
        
        //GroupLayout layout = new GroupLayout(progressDialog.getContentPane());
        //progressDialog.getContentPane().setLayout(layout);                        //
        
        //progressDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        
        //int locationHeight = (int)((screenSize.getHeight()/(float)5) - ((float)75/(float)2.0)); // top
        int locationHeight = (int)( screenSize.getHeight() - (screenSize.getHeight()/(float)5) - ((float)75/(float)2.0)); // bottom
        
        progressDialog.setLocation(
                                   (int)(screenSize.getWidth() / 2) - (300/2),
                                   locationHeight
                                   );
        progressDialog.setSize(500, 80);
        progressDialog.setPreferredSize(new Dimension(300, 80)); // works
        
        JPanel panel = new JPanel();
        //progressDialog.getContentPane().setBackground(Color.black); //
        panel.setSize(300, 80);
        panel.setPreferredSize(new Dimension(300, 80)); // works
        
        //progressDialog.getContentPane();
        JLabel label = new JLabel(title);
        Dimension size = label.getPreferredSize();
        label.setBounds(150, 100, size.width + 20, size.height + 20);
        //panel.add(label); // label optional
        
        //progressDialog.setSize(500, 275); // NO
        //progressDialog.getContentPane().setSize(500, 375); // no
        
        //progressDialog.setUndecorated(true) ; // transparent
        //progressDialog.getRootPane().setOpaque(false); //
        //panel.setOpaque(false);
        
        dpb = new JProgressBar();
        dpb.setValue(1);
        dpb.setSize(new Dimension(250, 40));
        dpb.setPreferredSize(new Dimension(250, 40));
        
        //dpb2 = new JProgressBar();
        
       // dpb.setStringPainted(true);
        
        //dpb.setBorderPainted(true);
        dpb.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8)); // top left bottom right
        //dpb.setOpaque(true);
        
        //dpb.setStringPainted(true);
        dpb.setEnabled(true);
        //progressDialog.add(BorderLayout.CENTER, dpb);
        //progressDialog.add(dpb);
        panel.add(dpb);
        //progressDialog.add(dpb);
        
        //JLabel progressLabel = new JLabel(title);
        //progressLabel.setBounds(100, 100, 200, 300);
        //progressDialog.add(BorderLayout.NORTH, progressLabel);
        //progressDialog.getContentPane().add(progressLabel);
        //panel.add(progressLabel);
        
        //progressDialog.getContentPane().add(panel);
        progressDialog.add(panel);
        
        //progressDialog.getContentPane().setBackground( Color.red ); //
        
        progressDialog.getContentPane().setSize(300, 120); // no
        progressDialog.pack();
        progressDialog.setVisible(false);
        //System.out.println("Visible.");
    }

    public void run(){
        int count = 0;
        
        init();
        
        while(progressDialogRunning){
            if(count > 3 && progressDialog != null){ // 5
                progressDialog.setVisible(true);
                
                // Set Progress bar
                //System.out.println(".");
            }
            try {
                Thread.sleep(50);
            } catch (Exception e){
            }
            count++;
        }
    }
    
    /**
     * close
     *
     * Description: Close progress dialog.
     */
    public void close(){
        progressDialogRunning = false;
        if(progressDialog != null){
            progressDialog.setVisible(false);
            progressDialog.dispose();
            progressDialog = null;
        }
    }
    
    public void setProgress(int i ){
        if(dpb != null){
            dpb.setValue(i);
            //progressDialog.repaint(); // NO
            //System.out.println("i " + i );
        }
    }
    
    public void setTitle(String title){
        progressDialog.setTitle(title);
    }
    
    /**
     * bounceIteration
     *
     * Description: Moves the progress bar position back and forth right and left bouncing off the ends.
     * This indicates somthing is happening but percentage progress is not given.
     */
    public void bounceIteration(){
        if(dpb != null){
            int i = dpb.getValue();
            if(i >= 100 || i <= 0){
                directionRight = !directionRight;
            }
            if( directionRight ){
                i++;
            } else {
                i--;
            }
            if(i > 100){
                i = 100;
            }
            if(i < 0){
                i = 0;
            }
            dpb.setValue(i);
        }
    }
    
    public boolean isRunning(){
        return progressDialogRunning;
    }
}

