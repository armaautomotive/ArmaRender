
package armarender;

import java.util.*;
import armarender.math.*;
import armarender.object.*;
import java.io.PrintWriter;
import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;

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
import javax.swing.ImageIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class FiveAxisConfig {
    private double minx = 99999;
    private double miny = 99999;
    private double minz = 99999;
    private double maxx = -999999;
    private double maxy = -999999;
    private double maxz = -999999;
    
    private int width = 48;
    private int depth = 96;
    private double accuracy = 0.06125; // 0.0393701; // 0.019685 = 0.5mm,  0.03125; // 1/32" .8mm   grid point length quanta_length
    private double drill_bit = 0.125;   // 0.125 1/8th 3.175mm
    private double drill_bit_angle = 135;
    private double rough_drill_bit = 0.25;
    private double pass_height = 0.5;   // drill cuts this much material per pass
    private double material_height = 10; // 2 - 10 cut scene into layers this thick for seperate parts/files.
    private double nozzleDistance = 10.0; // .5 distance from nozzle to 4/5 axis.
    private double depthPerPass = 1.25; // max material that can be removed in one pass
    private double cAngleOrigin = 0; // c axis (rotation vertically) origin. Machine vs cad coordinate system may be different.
    
    private boolean toolpathMarkup = false;
    private boolean cutOptimization = true;
    private boolean minimizePasses = true;
    private boolean simulateRoute = false;
    
    private Properties prop = new Properties();
    
    private JTextField heightField;
    private JTextField accuracyField;
    private JTextField bitField;
    private JTextField bitAngleField;
    private JTextField nozzleDistanceField;
    private JTextField cAngleOriginField;
    
    private JCheckBox toolpathCheck;
    private JCheckBox optimizationCheck;
    private JCheckBox minimizePassesCheck;
    private JCheckBox simulateCheck;
    
    public FiveAxisConfig(){
        prompt();
    }



    /**
     * prompt
     * Description:
     */
    public void prompt(){
        
        JPanel panel = new JPanel();
        //panel.setBackground(new Color(0, 0, 0));
        panel.setSize(new Dimension(440, 600));
        panel.setLayout(null);
        
        int cellHeight = 20;
        int secondColX = 160;
        
        
        JLabel widthLabel = new JLabel("Bed Width");
        //widthLabel.setForeground(new Color(255, 255, 0));
        widthLabel.setHorizontalAlignment(SwingConstants.CENTER);
        widthLabel.setFont(new Font("Arial", Font.BOLD, 11));
        widthLabel.setBounds(0, cellHeight, 130, 40); // x, y, width, height
        panel.add(widthLabel);
        
        JTextField widthField = new JTextField(new String(width+""));
        widthField.setBounds(130, cellHeight, 130, 40); // x, y, width, height
        panel.add(widthField);
        //widthField.getDocument().addDocumentListener(myListener);
        cellHeight += 40;
        
        
        JLabel labelDepth = new JLabel("Bed Depth");
        //labelHeight.setForeground(new Color(255, 255, 0));
        labelDepth.setHorizontalAlignment(SwingConstants.CENTER);
        labelDepth.setFont(new Font("Arial", Font.BOLD, 11));
        labelDepth.setBounds(0, cellHeight, 130, 40); // x, y, width, height
        panel.add(labelDepth);
        
        JTextField depthtField = new JTextField( new String(depth+""));
        depthtField.setBounds(130, cellHeight, 130, 40); // x, y, width, height
        panel.add(depthtField);
         
        
        
        
        //cellHeight += 40;
        JLabel labelHeight = new JLabel("Material Height");
        //labelHeight.setForeground(new Color(255, 255, 0));
        labelHeight.setHorizontalAlignment(SwingConstants.RIGHT);
        labelHeight.setFont(new Font("Arial", Font.BOLD, 11));
        labelHeight.setBounds(0, cellHeight, 130, 40); // x, y, width, height
        panel.add(labelHeight);
        
        heightField = new JTextField(new String(material_height+""));
        heightField.setBounds(secondColX, cellHeight, 130, 40); // x, y, width, height
        panel.add(heightField);
        heightField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                save();
            }
        });
        
        
        /*
        //depthPerPass
        cellHeight += 40;
        JLabel labelDepthPerPass = new JLabel("Depth Per Pass");
        labelDepthPerPass.setHorizontalAlignment(SwingConstants.CENTER);
        labelDepthPerPass.setFont(new Font("Arial", Font.BOLD, 11));
        labelDepthPerPass.setBounds(0, cellHeight, 130, 40); // x, y, width, height
        panel.add(labelDepthPerPass);
        
        JTextField depthPerPassField = new JTextField(new String(depthPerPass +""));
        depthPerPassField.setBounds(130, cellHeight, 130, 40); // x, y, width, height
        panel.add(depthPerPassField);
        */
        
        // quanta_length
        cellHeight += 40;
        JLabel labelAccuracy = new JLabel("Accracy");
        //labelHeight.setForeground(new Color(255, 255, 0));
        labelAccuracy.setHorizontalAlignment(SwingConstants.RIGHT);
        labelAccuracy.setFont(new Font("Arial", Font.BOLD, 11));
        labelAccuracy.setBounds(0, cellHeight, 130, 40); // x, y, width, height
        panel.add(labelAccuracy);
        
        accuracyField = new JTextField(new String(accuracy + ""));
        accuracyField.setBounds(secondColX, cellHeight, 130, 40); // x, y, width, height
        panel.add(accuracyField);
        accuracyField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                save();
            }
        });
        
    
        cellHeight += 40;
        JLabel labelBit = new JLabel("Finish Drill Bit Diameter");
        //labelHeight.setForeground(new Color(255, 255, 0));
        labelBit.setHorizontalAlignment(SwingConstants.RIGHT);
        labelBit.setFont(new Font("Arial", Font.BOLD, 11));
        labelBit.setBounds(0, cellHeight, 130, 40); // x, y, width, height
        panel.add(labelBit);
        
        bitField = new JTextField("" + drill_bit); //
        bitField.setBounds(secondColX, cellHeight, 130, 40); // x, y, width, height
        panel.add(bitField);
        bitField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                save();
            }
        });
        
        
        /*
        cellHeight += 40;
        JLabel roughLabelBit = new JLabel("Rough Drill Bit Diameter");                  // NEW
        //roughLabelBit.setForeground(new Color(255, 255, 0));
        roughLabelBit.setHorizontalAlignment(SwingConstants.CENTER);
        roughLabelBit.setFont(new Font("Arial", Font.BOLD, 11));
        roughLabelBit.setBounds(0, cellHeight, 130, 40); // x, y, width, height
        panel.add(roughLabelBit);
        
        JTextField roughBitField = new JTextField("" + rough_drill_bit);
        roughBitField.setBounds(130, cellHeight, 130, 40); // x, y, width, height
        panel.add(roughBitField);
        */
        
        cellHeight += 40;
        JLabel labelBitAngle = new JLabel("Drill Bit Angle");
        //labelHeight.setForeground(new Color(255, 255, 0));
        labelBitAngle.setHorizontalAlignment(SwingConstants.RIGHT);
        labelBitAngle.setFont(new Font("Arial", Font.BOLD, 11));
        labelBitAngle.setBounds(0, cellHeight, 130, 40); // x, y, width, height
        panel.add(labelBitAngle);
        
        bitAngleField = new JTextField( new String(drill_bit_angle+""));
        bitAngleField.setBounds(secondColX, cellHeight, 130, 40); // x, y, width, height
        panel.add(bitAngleField);
        bitAngleField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                save();
            }
        });
        
        
        cellHeight += 40;
        JLabel nozzleDistanceLabel = new JLabel("Tip Axis Distance");
        nozzleDistanceLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        nozzleDistanceLabel.setFont(new Font("Arial", Font.BOLD, 11));
        nozzleDistanceLabel.setBounds(0, cellHeight, 130, 40); // x, y, width, height
        panel.add(nozzleDistanceLabel);
        
        nozzleDistanceField = new JTextField( new String(nozzleDistance+""));
        nozzleDistanceField.setBounds(secondColX, cellHeight, 130, 40); // x, y, width, height
        panel.add(nozzleDistanceField);
        nozzleDistanceField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                save();
            }
        });
        
        
        cellHeight += 40;
        JLabel cAngleOriginLabel = new JLabel("C Axis Angle Origin");
        cAngleOriginLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        cAngleOriginLabel.setFont(new Font("Arial", Font.BOLD, 11));
        cAngleOriginLabel.setBounds(0, cellHeight, 130, 40); // x, y, width, height
        panel.add(cAngleOriginLabel);
        
        cAngleOriginField = new JTextField( new String(cAngleOrigin+""));
        cAngleOriginField.setBounds(secondColX, cellHeight, 130, 40); // x, y, width, height
        panel.add(cAngleOriginField);
        cAngleOriginField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                save();
            }
        });
        
        
        // Debug feature.
        cellHeight += 40;
        JLabel pathBit = new JLabel("Tool Path Markup");
        //labelHeight.setForeground(new Color(255, 255, 0));
        pathBit.setHorizontalAlignment(SwingConstants.RIGHT);
        pathBit.setFont(new Font("Arial", Font.BOLD, 11));
        pathBit.setBounds(0, cellHeight, 130, 40); // x, y, width, height
        panel.add(pathBit);
        
        toolpathCheck = new JCheckBox("");
        toolpathCheck.setBounds(secondColX, cellHeight, 130, 40); // x, y, width, height
        toolpathCheck.setSelected(false);
        panel.add(toolpathCheck);
        toolpathCheck.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                save();
                System.out.println("save markup ");
            }
        });
        
        
        /*
        cellHeight += 40;
        JLabel optimizationLabel = new JLabel("Cut Optimization ");
        //labelHeight.setForeground(new Color(255, 255, 0));
        optimizationLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        optimizationLabel.setFont(new Font("Arial", Font.BOLD, 11));
        optimizationLabel.setBounds(0, cellHeight, 130, 40); // x, y, width, height
        panel.add(optimizationLabel);
        
        optimizationCheck = new JCheckBox("");
        optimizationCheck.setBounds(secondColX, cellHeight, 130, 40); // x, y, width, height
        optimizationCheck.setSelected( cutOptimization );
        panel.add(optimizationCheck);
        optimizationCheck.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                save();
                System.out.println("save optimize ");
            }
        });
        */
        
        
        cellHeight += 40;
        JLabel minimizePassesLabel = new JLabel("Minimize Passes");
        //minimizePassesLabel.setForeground(new Color(255, 255, 0));
        minimizePassesLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        minimizePassesLabel.setFont(new Font("Arial", Font.BOLD, 11));
        minimizePassesLabel.setBounds(0, cellHeight, 130, 40); // x, y, width, height
        panel.add(minimizePassesLabel);
        
        minimizePassesCheck = new JCheckBox("");
        minimizePassesCheck.setBounds(secondColX, cellHeight, 130, 40); // x, y, width, height
        minimizePassesCheck.setSelected( minimizePasses );
        panel.add(minimizePassesCheck);
        minimizePassesCheck.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                save();
                System.out.println("save minimize ");
            }
        });
        
        
        /*
        cellHeight += 40;
        JLabel simulateLabel = new JLabel("Simulate Route");
        //simulateLabel.setForeground(new Color(255, 255, 0));
        simulateLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        simulateLabel.setFont(new Font("Arial", Font.BOLD, 11));
        simulateLabel.setBounds(0, cellHeight, 130, 40); // x, y, width, height
        panel.add(simulateLabel);
        
        simulateCheck = new JCheckBox("");
        simulateCheck.setBounds(secondColX, cellHeight, 130, 40); // x, y, width, height
        simulateCheck.setSelected( simulateRoute );
        panel.add(simulateCheck);
        simulateCheck.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                save();
                System.out.println("save minimize ");
            }
        });
        */
        
        cellHeight += 40;
        JLabel warningHeight = new JLabel("Note: 5-Axis routing is not production ready.");
        //warningHeight.setForeground(new Color(255, 255, 0));
        warningHeight.setHorizontalAlignment(SwingConstants.CENTER);
        warningHeight.setFont(new Font("Arial", Font.BOLD, 11));
        warningHeight.setBounds(0, cellHeight, 280, 40); // x, y, width, height
        panel.add(warningHeight);
        
        
        UIManager.put("OptionPane.minimumSize",new Dimension(500, cellHeight + 80 + 40));
        
        load(); // read propertyies and set UI fields.
        
        ImageIcon iconImage = new ImageIcon(getClass().getResource("/armarender/Icons/favicon-32x32.png"));
        
        //int result = JOptionPane.showConfirmDialog(null, panel, "Five Axis CNC Properties", JOptionPane.OK_CANCEL_OPTION);
        int result = JOptionPane.showConfirmDialog(null, panel, "Finishing Five Axis CNC Properties", JOptionPane.OK_CANCEL_OPTION, JOptionPane.OK_CANCEL_OPTION,  iconImage);
        if (result == JOptionPane.OK_OPTION) {
            //System.out.println("width value: " + widthField.getText());
            //System.out.println("depth value: " + depthtField.getText());
            //System.out.println("height value: " + heightField.getText());
            //System.out.println("bit value: " + bitField.getText());
            this.width = 9999; // Integer.parseInt(widthField.getText());
            this.depth = 9999; // Integer.parseInt(depthtField.getText());
            this.material_height = Double.parseDouble(heightField.getText());
            //this.depthPerPass = Double.parseDouble(depthPerPassField.getText());
            this.drill_bit = Double.parseDouble(bitField.getText());
            //this.rough_drill_bit = Double.parseDouble(roughBitField.getText());
            this.accuracy = Double.parseDouble(accuracyField.getText());
            this.drill_bit_angle = Double.parseDouble(bitAngleField.getText());
            this.nozzleDistance = Double.parseDouble(nozzleDistanceField.getText());
            //this.toolpathMarkup = toolpathCheck.isSelected();
            //this.cutOptimization = optimizationCheck.isSelected();
            //this.minimizePasses = minimizePassesCheck.isSelected();
            //this.simulateRoute = simulateCheck.isSelected();
            
            this.cAngleOrigin = Double.parseDouble(cAngleOriginField.getText());
            
            //return true;
        }
        
    }

    
    
    /**
     * load
     *
     * Description: Load property file attributes and populate the UI fields.
     */
    public void load(){
        try {
            //String path = FileSystem.getSettingsPath(); //
        String path = new File(".").getCanonicalPath();
            //System.out.println("path: " + path);
            String propertyFileName = path + System.getProperty("file.separator") + "cam.properties";
            InputStream input = new FileInputStream(propertyFileName);
            // load a properties file
            prop.load(input);
            
            setBooleanProperty(prop, toolpathCheck, "ads.export_mill_5axis_toolpath_markup");
            setBooleanProperty(prop, optimizationCheck, "ads.export_mill_5axis_cut_optimization");
            setBooleanProperty(prop, minimizePassesCheck, "ads.export_mill_5axis_minimize_passes");
            setBooleanProperty(prop, simulateCheck, "ads.export_mill_5axis_simulate");
            
            setStringProperty(prop, heightField, "ads.export_mill_5axis_material_height");
            setStringProperty(prop, accuracyField, "ads.export_mill_5axis_accuracy");
            setStringProperty(prop, bitField, "ads.export_mill_5axis_bit_diameter");
            setStringProperty(prop, bitAngleField, "ads.export_mill_5axis_bit_angle");
            setStringProperty(prop, nozzleDistanceField, "ads.export_mill_5axis_bc_distance");
            setStringProperty(prop, cAngleOriginField, "ads.export_mill_5axis_c_angle_orientation");
           
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public void setStringProperty(Properties prop, JTextField field, String property){
        String value = prop.getProperty(property);
        if(value != null && field != null){
            field.setText(value);
        }
    }

    public void setBooleanProperty(Properties prop, JCheckBox field, String property){
        String value = prop.getProperty(property);
        if(value != null && field != null){
            if(value.equals("false")){
                field.setSelected(false);
            }
            if(value.equals("true")){
                field.setSelected(true);
            }
        }
    }
    
    public String getProperty(String property){
        String value = prop.getProperty(property);
        if(value == null){
            
        }
        return value;
    }
    
    
    /**
     * save
     *
     * Description: Save account information given in the input fields into the property file for persistant storage.
     */
    public void save(){
        try {
            //String path = FileSystem.getSettingsPath();
        String path = new File(".").getCanonicalPath();

            String propertyFileName = path + System.getProperty("file.separator") + "cam.properties";
            InputStream input = new FileInputStream(propertyFileName);
            prop.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        try {
            //String path = FileSystem.getSettingsPath();
        String path = new File(".").getCanonicalPath();

            String propertyFileName = path + System.getProperty("file.separator") + "cam.properties";
            OutputStream output = new FileOutputStream(propertyFileName);
            
            // set the properties value
            
            prop.setProperty("ads.export_mill_5axis_toolpath_markup", ""+toolpathCheck.isSelected());
            prop.setProperty("ads.export_mill_5axis_cut_optimization", ""+optimizationCheck.isSelected());
            prop.setProperty("ads.export_mill_5axis_minimize_passes", ""+minimizePassesCheck.isSelected());
            prop.setProperty("ads.export_mill_5axis_simulate", ""+simulateCheck.isSelected());
            
            prop.setProperty("ads.export_mill_5axis_material_height", ""+heightField.getText());
            prop.setProperty("ads.export_mill_5axis_accuracy", ""+accuracyField.getText());
            prop.setProperty("ads.export_mill_5axis_bit_diameter", ""+bitField.getText());
            prop.setProperty("ads.export_mill_5axis_bit_angle", ""+bitAngleField.getText());
            prop.setProperty("ads.export_mill_5axis_bc_distance", ""+nozzleDistanceField.getText());
            prop.setProperty("ads.export_mill_5axis_c_angle_orientation", ""+cAngleOriginField.getText());
          
            // save properties to project root folder
            prop.store(output, null);
        } catch (IOException io) {
            io.printStackTrace();
        }
    }
    
}
