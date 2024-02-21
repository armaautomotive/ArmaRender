/* Copyright (C) 2023 Jon Taylor

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender;

import armarender.*;
import armarender.animation.*;
import armarender.animation.distortion.*;
import armarender.material.*;
import armarender.math.*;
import armarender.texture.*;
import armarender.object.*;
import java.lang.ref.*;
import javax.swing.*;
import armarender.math.*;
import armarender.object.*;
import armarender.view.CanvasDrawer;
import java.io.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import buoy.widget.*;
import armarender.ui.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import javax.swing.JSplitPane;
import java.awt.GridLayout;
import java.awt.*;
import java.awt.event.*;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.swing.JComboBox;
import java.util.Properties;
import javax.swing.ImageIcon;
import javax.swing.plaf.basic.BasicSplitPaneUI;

public class Upgrade extends JFrame implements ActionListener {
    private JPanel leftPanel, rightPanel;
    private JButton saveButton, closeButton;
    
    
    public Upgrade(){
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        
        this.setTitle("Update");
        
        leftPanel = new JPanel();
        leftPanel.setSize(300, dim.height);
        leftPanel.setMinimumSize(new Dimension(300, dim.height));
        leftPanel.setPreferredSize(new Dimension(300, dim.height));
        leftPanel.setBackground(new Color(220,220,220));
        leftPanel.setLayout(null);
        
        
        
        //JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        //splitPane.setDividerLocation(400);
        
        
        ImageIcon iconImage = new ImageIcon(getClass().getResource("/armarender/Icons/favicon-32x32.png"));
        this.setIconImage(iconImage.getImage());
        
        //flattenJSplitPane(leftPanel);
        this.add(leftPanel, BorderLayout.CENTER);
        
        int width = 1200;
        int height = 800;
        if(width > dim.width){
            width = dim.width;
        }
        if(height > dim.height){
            height = dim.height;
        }
        this.setLocation((dim.width / 2) - (width / 2), (dim.height / 2) - (height / 2)); // dim.width/2-frame.getSize().width/2, dim.height/2-frame.getSize().height/2);
        //this.setPreferredSize(new Dimension(dim.width, dim.height));
        this.setPreferredSize(new Dimension(width, height));
        this.pack();
        
    }
    
    
    
   
    
    
    /**
     * actionPerformed
     *
     * Description: handle button events.
     */
    public void actionPerformed( ActionEvent event )
    {
        //System.out.println(" " + event.getSource().getClass().getName()  );
        if(event.getSource() == closeButton){
            
            this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        }
        if(event.getSource() == saveButton){
            //save();
            
            // If commercial, check if paid or launch purchase web url.
            //if( licenseList.getSelectedItem().equals("Commercial") ){
            //    System.out.println("Commercial account selected.");
                
                
            //}
        }
    }
    
}
