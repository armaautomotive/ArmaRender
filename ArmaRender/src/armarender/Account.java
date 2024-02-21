/* Copyright (C) 2022-2023 by Jon Taylor

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender;

import java.util.*;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.JLabel;
import java.awt.Font;
import javax.swing.SwingConstants;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import java.awt.BorderLayout;
import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.JFrame;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import javax.swing.JSplitPane;
import java.awt.GridLayout;
import java.awt.*;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.BorderFactory;
import javax.swing.plaf.basic.*;
import javax.swing.border.*;
import javax.swing.JScrollPane;
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

public class Account extends JFrame implements ActionListener {
    private JPanel leftPanel, rightPanel;
    private JButton saveButton, closeButton;
    private JTextField nameField, companyField, emailField, websiteField, phoneField, countryField;
    private JLabel introLabel, licenseDetailLabel;
    private JComboBox licenseList;
    String[] licenseStrings = { "Free", "Commercial" };
    private String [] licenseDescriptions = new String[] {" ", " "};
    
    private String description = "<html>Arma Render is a CAD rendering and animation application for industrial designin and manufacturing. \n<br>"+
    "ADS runds on Windows, Mac OSX and Linux with new features being planned. \n <br><br>" +
    
    "Basic functions are free to use and include the following: \n <br><br>" +
    
    "* File import/export supporting: .ads, .obj, .gcode, .dxf, .stl, .pdf. \n<br>" +
    "* Modelling with geometric shapes, contoured meshe surfaces and curves, tubes frames with notches and slots, text, and more. \n<br>"+
    "* Modeling tools with object mirroring, boolean operations, surface meshing conform, etc. \n <br>" +
    "* Rendering and visualizations with raytracing. \n<br>"+
    "* Managing large complex scenes with object nesting, group / child visibility, searching, grouping etc. \n<br>"+
    "* Boolean moddeling for additive and subtractive shapes as well as polymesh modelling. \n<br>"+
    "* Layout modes for flat positioning individual parts on CNC tables and 3D printers. \n<br>"+
    "* Export GCode files for 3D Printers, 3-5 axis routers, tube notchers, tube benders, etc.  \n<br>"+
    "* Tube bending PDF instructions export for manual bending.\n <br>"+
    "* Model custom vehicle chassis and body designs to have Arma Automotive Inc. manufacture for you. \n<br>"+
    "* CFD and FEA currently under development. \n<br>"+
    "<br>" +

    "The free features are perfect for individuals and businesses making any number of non commercial designs and parts. \n<br>"+
    "<br><br>"+

    "Industrial features are offered on a subscription licence for users that design for commercial applications. \n<br>"+
    "Features offered under the industrial licence include: \n<br><br>"+
    //"* Version control tools and multi user features. \n<br>"+
    //"* In app deployment and monitoring of cuts and prints on Arma CNC machines. \n<br>"+
    "* License for commercial design and manufacturing file exports to support production operations. \n<br>"+
    //"* Inventory management for generating a bill of materials, lengths, costs and weights for design fabrication. \n<br>"+
    "* Email and phone support services. \n<br>"+
    "<br>"+
     
    "Commercial Price: $30 and $5 for future updates.  \n<br>" +
    "</html>";
    

/*
    Arma Automotive Inc. builds custom supercar platforms and manufacturing automation tools including CNC Tube notching and CNC Tube bending with a 5-Axis router printer under development.

    ADS source code is distributed under an open license. You are free to use the code for your own applications provided attribution and
    your derividive work is also released under an open licence. ";
     */
    
    public Account(){
        // Read config file for existing account. If no account file is present load dialog.
        
     
        System.out.println("Account");
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        
        this.setTitle("Account");
        
        leftPanel = new JPanel();
        leftPanel.setSize(300, dim.height);
        leftPanel.setMinimumSize(new Dimension(300, dim.height));
        leftPanel.setPreferredSize(new Dimension(300, dim.height));
        leftPanel.setBackground(new Color(220,220,220));
        leftPanel.setLayout(null);
        
        rightPanel = new JPanel();
        rightPanel.setPreferredSize(new Dimension(300, dim.height));
        rightPanel.setSize(300, dim.height);
        rightPanel.setMinimumSize(new Dimension(300, dim.height));
        rightPanel.setBackground(new Color(250,250,250)); // 220,220,220
        rightPanel.setLayout(null);
        
        int x = 30;
        int y = 20;
        int ySeperation = 44;
        
        
        introLabel = new JLabel("<html>Enter your account information to unlock your <br>ADS software license. </html>", JLabel.CENTER);
        //Set the position of the text, relative to the icon:
        introLabel.setVerticalTextPosition(JLabel.BOTTOM);
        introLabel.setHorizontalTextPosition(JLabel.LEFT);
        introLabel.setBounds(x, y + 0, 300, 66);
        leftPanel.add(introLabel);
        
        //y+= (ySeperation);
        y+= (ySeperation);
        
        JLabel nameLabel = new JLabel("Name", JLabel.CENTER);
        //Set the position of the text, relative to the icon:
        nameLabel.setVerticalTextPosition(JLabel.BOTTOM);
        nameLabel.setHorizontalTextPosition(JLabel.LEFT);
        nameLabel.setBounds(x, y + ySeperation - 2, 100, 45);
        leftPanel.add(nameLabel);
        
        nameField = new JTextField("");
        nameField.setBounds(x + 100, y + ySeperation, 200, 33);
        leftPanel.add(nameField);
        
        y+= (ySeperation);
        
        
        JLabel companyLabel = new JLabel("Company", JLabel.CENTER);
        //Set the position of the text, relative to the icon:
        companyLabel.setVerticalTextPosition(JLabel.BOTTOM);
        companyLabel.setHorizontalTextPosition(JLabel.LEFT);
        companyLabel.setBounds(x, y + ySeperation - 2, 100, 45);
        leftPanel.add(companyLabel);
        
        companyField = new JTextField("");
        companyField.setBounds(x + 100, y + ySeperation, 200, 33);
        leftPanel.add(companyField);
        
        y+= (ySeperation);
        
        
        JLabel emailLabel = new JLabel("Email", JLabel.CENTER);
        //Set the position of the text, relative to the icon:
        emailLabel.setVerticalTextPosition(JLabel.BOTTOM);
        emailLabel.setHorizontalTextPosition(JLabel.LEFT);
        emailLabel.setBounds(x, y + ySeperation - 2, 100, 45);
        leftPanel.add(emailLabel);
        
        emailField = new JTextField("");
        emailField.setBounds(x + 100, y + ySeperation, 200, 33);
        leftPanel.add(emailField);
        
        y+= (ySeperation);
        
        
        JLabel websiteLabel = new JLabel("Website", JLabel.CENTER);
        //Set the position of the text, relative to the icon:
        websiteLabel.setVerticalTextPosition(JLabel.BOTTOM);
        websiteLabel.setHorizontalTextPosition(JLabel.LEFT);
        websiteLabel.setBounds(x, y + ySeperation - 2, 100, 45);
        leftPanel.add(websiteLabel);
        
        websiteField = new JTextField("");
        websiteField.setBounds(x + 100, y + ySeperation, 200, 33);
        leftPanel.add(websiteField);
        
        y+= (ySeperation);
        
        
        JLabel phoneLabel = new JLabel("Phone", JLabel.CENTER);
        //Set the position of the text, relative to the icon:
        phoneLabel.setVerticalTextPosition(JLabel.BOTTOM);
        phoneLabel.setHorizontalTextPosition(JLabel.LEFT);
        phoneLabel.setBounds(x, y + ySeperation - 2, 100, 45);
        leftPanel.add(phoneLabel);
        
        phoneField = new JTextField("");
        phoneField.setBounds(x + 100, y + ySeperation, 200, 33);
        leftPanel.add(phoneField);
        
        
        y+= (ySeperation);
        
        JLabel countryLabel = new JLabel("Country", JLabel.CENTER);
        //Set the position of the text, relative to the icon:
        countryLabel.setVerticalTextPosition(JLabel.BOTTOM);
        countryLabel.setHorizontalTextPosition(JLabel.LEFT);
        countryLabel.setBounds(x, y + ySeperation - 2, 100, 45);
        leftPanel.add(countryLabel);
        
        countryField = new JTextField("");
        countryField.setBounds(x + 100, y + ySeperation + 0, 200, 33);
        leftPanel.add(countryField);
        
        y+= (ySeperation);
        
        // License
        JLabel licenseLabel = new JLabel("License", JLabel.CENTER);
        //Set the position of the text, relative to the icon:
        licenseLabel.setVerticalTextPosition(JLabel.BOTTOM);
        licenseLabel.setHorizontalTextPosition(JLabel.LEFT);
        licenseLabel.setBounds(x, y + ySeperation - 2, 100, 45);
        leftPanel.add(licenseLabel);
        
        
        licenseList = new JComboBox(licenseStrings);
        licenseList.setBounds(x + 100, y + ySeperation + 2, 200, 33);
        licenseList.setPreferredSize(new Dimension(195, 33));
        //licenseList.setLayout(null);
        //licenseList.setSelectedIndex(4);
        licenseList.addActionListener(this);
        leftPanel.add(licenseList);
        
        
        y+= (ySeperation);
        
        //
        licenseDetailLabel = new JLabel("<html>Get started today.</html>", JLabel.CENTER);
        //Set the position of the text, relative to the icon:
        licenseDetailLabel.setVerticalTextPosition(JLabel.BOTTOM);
        licenseDetailLabel.setHorizontalTextPosition(JLabel.LEFT);
        licenseDetailLabel.setBounds(x, y + ySeperation, 300, 96);
        leftPanel.add(licenseDetailLabel);
        
        
        y+= (ySeperation);
        
        y+= (ySeperation);
        y+= (ySeperation / 4);
        
        saveButton = new JButton("Save");
        saveButton.setBounds(100, y + ySeperation, 90,33);
        saveButton.addActionListener( this );
        leftPanel.add(saveButton);
        
        closeButton = new JButton("Close");
        closeButton.setBounds(100 + 100, y + ySeperation, 90,33);
        closeButton.addActionListener( this );
        leftPanel.add(closeButton);
        
        
        
        y = 0;
        JLabel descriptionLabel = new JLabel(description, JLabel.CENTER); //
        //Set the position of the text, relative to the icon:
        descriptionLabel.setVerticalTextPosition(JLabel.TOP);
        descriptionLabel.setHorizontalTextPosition(JLabel.LEFT);
        descriptionLabel.setBounds(x, y + 0, 700, 600);
        rightPanel.add(descriptionLabel);
        
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(400);
        
        //leftPanel.add(nameLabel);
        
        ImageIcon iconImage = new ImageIcon(getClass().getResource("/armarender/Icons/favicon-32x32.png"));
        this.setIconImage(iconImage.getImage());
        
        flattenJSplitPane(splitPane);
        this.add(splitPane, BorderLayout.CENTER);
        
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
        
        
        // Load saved values
        load();
    }
    
    
    /**
     * displayForm
     *
     * Description:
     */
    public void displayForm(){
        

        
        
        //5. Show it.
        this.setVisible(true);
    }
    
    /**
     * readFile
     *
     * Description: Read file contents.
     */
    static String readFile(String path, Charset encoding) throws IOException
    {
      byte[] encoded = Files.readAllBytes(Paths.get(path));
      return new String(encoded, encoding);
    }
    
    /**
     * Makes a split pane invisible. Only contained components are shown.
     *
     * @param splitPane
     */
    public static void flattenJSplitPane(JSplitPane splitPane) {
        splitPane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        BasicSplitPaneUI flatDividerSplitPaneUI = new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    @Override
                    public void setBorder(Border b) {
                    }
                };
            }
        };
        splitPane.setUI(flatDividerSplitPaneUI);
        splitPane.setBorder(null);
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
            save();
            
            // If commercial, check if paid or launch purchase web url.
            if( licenseList.getSelectedItem().equals("Commercial") ){
                System.out.println("Commercial account selected.");
                
                
            }
        }
    }
    
    /**
     * save
     *
     * Description:
     */
    public void save(){
        try {
            String path = new File(".").getCanonicalPath();
            //System.out.println("path: " + path);
            String propertyFileName = path + System.getProperty("file.separator") + "ads.properties";
            OutputStream output = new FileOutputStream(propertyFileName);
            Properties prop = new Properties();
            // set the properties value
            prop.setProperty("ads.name", nameField.getText());
            prop.setProperty("ads.company", companyField.getText());
            prop.setProperty("ads.email", emailField.getText());
            prop.setProperty("ads.website", websiteField.getText());
            prop.setProperty("ads.phone", phoneField.getText());
            prop.setProperty("ads.country", countryField.getText());
            prop.setProperty("ads.license", String.valueOf(licenseList.getSelectedItem()));
            // save properties to project root folder
            prop.store(output, null);
            //System.out.println(prop);
        } catch (IOException io) {
            io.printStackTrace();
        }
    }
    
    /**
     * load
     *
     * Description:
     */
    public void load(){
        try {
            String path = new File(".").getCanonicalPath();
            //System.out.println("path: " + path);
            String propertyFileName = path + System.getProperty("file.separator") + "ads.properties";
            InputStream input = new FileInputStream(propertyFileName);
            Properties prop = new Properties();
            // load a properties file
            prop.load(input);
            // get the property value and print it out
            nameField.setText( prop.getProperty("ads.name") );
            companyField.setText( prop.getProperty("ads.company") );
            emailField.setText( prop.getProperty("ads.email") );
            websiteField.setText( prop.getProperty("ads.website") );
            phoneField.setText( prop.getProperty("ads.phone") );
            countryField.setText( prop.getProperty("ads.country") );
            licenseList.setSelectedItem( prop.getProperty("ads.license")  ) ;
            // licenseDetailLabel TODO:

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    
    public String getName(){
        return nameField.getText();
    }
    
    public String getCompany(){
        return companyField.getText();
    }
    
    public String getEmail(){
        return emailField.getText();
    }
    
    public String getPhone(){
        return phoneField.getText();
    }
    
    public String getAddress(){
        return countryField.getText();
    }
    
}


