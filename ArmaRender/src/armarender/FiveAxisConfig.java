
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
import javax.swing.*;

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
    private double bAngleOrigin = 0;
    private double colleteWidth = 2;
    private double routerHousingDiameter = 4.2;
    private double fulcrumToHousingRear = 6.2;
    
    private double t1_drill_bit_cut_Length = 2;
    private double t1_drill_bit_tip_to_collete_length = 5;
    
    private boolean toolpathMarkup = false;
    private boolean cutOptimization = true;
    private boolean minimizePasses = true;
    private boolean simulateRoute = false;
    
    private static Properties prop = new Properties();
    
    private JTextField heightField;
    private JTextField accuracyField;
    private JTextField bitField;
    //private JTextField bitAngleField; // remove? replace with types (Flat end or ball nose)
    private JTextField nozzleDistanceField; // Collete dist
    private JTextField cAngleOriginField;
    private JTextField bAngleOriginField;
    private JTextField t1BitCutLengthField;
    private JTextField t1BitCutTipToColleteLengthField;
    private JTextField colleteWidthField;
    private JTextField routerHousingDiameterField;
    private JTextField fulcrumToHousingRearField;
    
    private JCheckBox toolpathCheck;
    private JCheckBox optimizationCheck;
    private JCheckBox minimizePassesCheck;
    private JCheckBox simulateCheck;
    private JCheckBox invertBDirectionCheck;
    private JCheckBox invertCDirectionCheck;
    
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
        int secondColX = 190;
        int rowSpacing = 36;
        
        int labelWidth = 170;
        int inputFieldWidth = 130;
        
        
        JLabel widthLabel = new JLabel("Bed Width");
        //widthLabel.setForeground(new Color(255, 255, 0));
        widthLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        widthLabel.setFont(new Font("Arial", Font.BOLD, 11));
        widthLabel.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        panel.add(widthLabel);
        
        JTextField widthField = new JTextField(new String(width+""));
        widthField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        panel.add(widthField);
        //widthField.getDocument().addDocumentListener(myListener);
        cellHeight += rowSpacing;
        
        
        JLabel labelDepth = new JLabel("Bed Depth");
        //labelHeight.setForeground(new Color(255, 255, 0));
        labelDepth.setHorizontalAlignment(SwingConstants.RIGHT);
        labelDepth.setFont(new Font("Arial", Font.BOLD, 11));
        labelDepth.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        panel.add(labelDepth);
        
        JTextField depthtField = new JTextField( new String(depth+""));
        depthtField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        panel.add(depthtField);
        
        
        cellHeight += rowSpacing;
        JLabel labelHeight = new JLabel("Bed Height");
        //labelHeight.setForeground(new Color(255, 255, 0));
        labelHeight.setHorizontalAlignment(SwingConstants.RIGHT);
        labelHeight.setFont(new Font("Arial", Font.BOLD, 11));
        labelHeight.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        panel.add(labelHeight);
        
        heightField = new JTextField(new String(material_height+""));
        heightField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        panel.add(heightField);
        heightField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                save();
            }
        });
        
        
        /*
        //depthPerPass
        cellHeight += rowSpacing;
        JLabel labelDepthPerPass = new JLabel("Depth Per Pass");
        labelDepthPerPass.setHorizontalAlignment(SwingConstants.CENTER);
        labelDepthPerPass.setFont(new Font("Arial", Font.BOLD, 11));
        labelDepthPerPass.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        panel.add(labelDepthPerPass);
        
        JTextField depthPerPassField = new JTextField(new String(depthPerPass +""));
        depthPerPassField.setBounds(130, cellHeight, 130, 40); // x, y, width, height
        panel.add(depthPerPassField);
        */
        
        /*
        // quanta_length
        cellHeight += rowSpacing;
        JLabel labelAccuracy = new JLabel("Default Accracy");
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
         */
    
        
        
        
        /*
        cellHeight += rowSpacing;
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
        
        
        
        //
        // Coordinate system.
        //
        cellHeight += rowSpacing;
        JLabel csHeight = new JLabel("Coordinate System");
        //csHeight.setForeground(new Color(255, 255, 0));
        csHeight.setHorizontalAlignment(SwingConstants.CENTER);
        csHeight.setFont(new Font("Arial", Font.BOLD, 11));
        csHeight.setBounds(0, cellHeight, 280, 40); // x, y, width, height
        panel.add(csHeight);
        
        // C angle
        cellHeight += rowSpacing;
        JLabel cAngleOriginLabel = new JLabel("C Axis Angle Origin");
        cAngleOriginLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        cAngleOriginLabel.setFont(new Font("Arial", Font.BOLD, 11));
        cAngleOriginLabel.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        panel.add(cAngleOriginLabel);
        
        cAngleOriginField = new JTextField( new String(cAngleOrigin+""));
        cAngleOriginField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        panel.add(cAngleOriginField);
        cAngleOriginField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                save();
            }
        });
        
        // Invert C
        cellHeight += rowSpacing;
        JLabel invertCDirectionLabel = new JLabel("Invert C Direction");
        //invertCDirectionLabel.setForeground(new Color(255, 255, 0));
        invertCDirectionLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        invertCDirectionLabel.setFont(new Font("Arial", Font.BOLD, 11));
        invertCDirectionLabel.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        panel.add(invertCDirectionLabel);

        invertCDirectionCheck = new JCheckBox("");
        invertCDirectionCheck.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        invertCDirectionCheck.setSelected( simulateRoute );
        panel.add(invertCDirectionCheck);
        invertCDirectionCheck.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //save();
                //System.out.println("save minimize ");
            }
        });
        
        // Offset B Degrees
        cellHeight += rowSpacing;
        JLabel bAngleOriginLabel = new JLabel("B Axis Angle Origin");
        bAngleOriginLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        bAngleOriginLabel.setFont(new Font("Arial", Font.BOLD, 11));
        bAngleOriginLabel.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        panel.add(bAngleOriginLabel);
        
        bAngleOriginField = new JTextField( new String(bAngleOrigin+""));
        bAngleOriginField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        panel.add(bAngleOriginField);
        bAngleOriginField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                //save();
            }
        });
        
        
        
        // Invert B
        cellHeight += rowSpacing;
        JLabel invertBDirectionLabel = new JLabel("Invert B Direction");
        //invertBDirectionLabel.setForeground(new Color(255, 255, 0));
        invertBDirectionLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        invertBDirectionLabel.setFont(new Font("Arial", Font.BOLD, 11));
        invertBDirectionLabel.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        panel.add(invertBDirectionLabel);

        invertBDirectionCheck = new JCheckBox("");
        invertBDirectionCheck.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        invertBDirectionCheck.setSelected( simulateRoute );
        panel.add(invertBDirectionCheck);
        invertBDirectionCheck.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //save();
                //System.out.println("save minimize ");
            }
        });
        
        
        
        
        //
        // Router size properties.
        //
        cellHeight += rowSpacing;
        JLabel routerDimensionsHeight = new JLabel("Router Dimensions");
        //routerDimensionsHeight.setForeground(new Color(255, 255, 0));
        routerDimensionsHeight.setHorizontalAlignment(SwingConstants.CENTER);
        routerDimensionsHeight.setFont(new Font("Arial", Font.BOLD, 11));
        routerDimensionsHeight.setBounds(0, cellHeight, 280, 40); // x, y, width, height
        panel.add(routerDimensionsHeight);
        
        
        cellHeight += rowSpacing;
        JLabel nozzleDistanceLabel = new JLabel("Fulcrum to Collet Distance"); // TODO
        nozzleDistanceLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        nozzleDistanceLabel.setFont(new Font("Arial", Font.BOLD, 11));
        nozzleDistanceLabel.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        panel.add(nozzleDistanceLabel);
        
        nozzleDistanceField = new JTextField( new String(nozzleDistance+""));
        nozzleDistanceField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        panel.add(nozzleDistanceField);
        nozzleDistanceField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                save();
            }
        });
        
        
        // Collete Width
        cellHeight += rowSpacing;
        JLabel colleteWidthLabel = new JLabel("Collet Width"); // TODO
        colleteWidthLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        colleteWidthLabel.setFont(new Font("Arial", Font.BOLD, 11));
        colleteWidthLabel.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        panel.add(colleteWidthLabel);
        
        colleteWidthField = new JTextField( new String(colleteWidth+""));
        colleteWidthField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        panel.add(colleteWidthField);
        colleteWidthField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                save();
            }
        });
        
        
        // Router Housing Diameter
        cellHeight += rowSpacing;
        JLabel routerHousingDiameterLabel = new JLabel("Router Housing Diameter"); // TODO
        routerHousingDiameterLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        routerHousingDiameterLabel.setFont(new Font("Arial", Font.BOLD, 11));
        routerHousingDiameterLabel.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        panel.add(routerHousingDiameterLabel);
        
        routerHousingDiameterField = new JTextField( new String(routerHousingDiameter+""));
        routerHousingDiameterField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        panel.add(routerHousingDiameterField);
        routerHousingDiameterField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                save();
            }
        });
        
        // Back end length
        cellHeight += rowSpacing;
        JLabel fulcrumToHousingRearLabel = new JLabel("Fulcrum to Housing Rear"); // TODO
        fulcrumToHousingRearLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        fulcrumToHousingRearLabel.setFont(new Font("Arial", Font.BOLD, 11));
        fulcrumToHousingRearLabel.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        panel.add(fulcrumToHousingRearLabel);
        
        fulcrumToHousingRearField = new JTextField( new String(fulcrumToHousingRear+""));
        fulcrumToHousingRearField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        panel.add(fulcrumToHousingRearField);
        fulcrumToHousingRearField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                save();
            }
        });
        
        
        
        //
        // Bit properties.
        //
        cellHeight += rowSpacing;
        JLabel bitLabel = new JLabel("Bits");
        //bitLabel.setForeground(new Color(255, 255, 0));
        bitLabel.setHorizontalAlignment(SwingConstants.CENTER);
        bitLabel.setFont(new Font("Arial", Font.BOLD, 11));
        bitLabel.setBounds(0, cellHeight, 280, 40); // x, y, width, height
        panel.add(bitLabel);
        
        
        cellHeight += rowSpacing;
        JLabel labelBitT1 = new JLabel("T1 Bit Diameter");
        //labelBitT1.setForeground(new Color(255, 255, 0));
        labelBitT1.setHorizontalAlignment(SwingConstants.RIGHT);
        labelBitT1.setFont(new Font("Arial", Font.BOLD, 11));
        labelBitT1.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        panel.add(labelBitT1);
        
        bitField = new JTextField("" + drill_bit); //
        bitField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        panel.add(bitField);
        bitField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                save();
            }
        });
        
        // M1 Type (Ball / Flat End)
        cellHeight += rowSpacing;
        JLabel labelBitT1End = new JLabel("T1 Bit End Type");
        //labelBitT1End.setForeground(new Color(255, 255, 0));
        labelBitT1End.setHorizontalAlignment(SwingConstants.RIGHT);
        labelBitT1End.setFont(new Font("Arial", Font.BOLD, 11));
        labelBitT1End.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        panel.add(labelBitT1End);
        
        String[] bitEndTypesToChoose = {"Ball Nose", "Flat End"};
        JComboBox<String> t1BitEndComboBox = new JComboBox<>(bitEndTypesToChoose);
        t1BitEndComboBox.setBounds(secondColX, cellHeight, inputFieldWidth, 40);
        panel.add(t1BitEndComboBox);
        
        
        // Bit Cut Length
        cellHeight += rowSpacing;
        JLabel labelBitT1CutLength = new JLabel("T1 Bit Cutting Length");
        //labelBitT1CutLength.setForeground(new Color(255, 255, 0));
        labelBitT1CutLength.setHorizontalAlignment(SwingConstants.RIGHT);
        labelBitT1CutLength.setFont(new Font("Arial", Font.BOLD, 11));
        labelBitT1CutLength.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        panel.add(labelBitT1CutLength);
        
        t1BitCutLengthField = new JTextField("" + t1_drill_bit_cut_Length); //
        t1BitCutLengthField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        panel.add(t1BitCutLengthField);
        t1BitCutLengthField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                save();
            }
        });
        
        // Bit Tip to Collete distance
        cellHeight += rowSpacing;
        JLabel labelBitT1TipToColleteLength = new JLabel("T1 Bit Tip To Collete Length");
        //labelBitT1TipToColleteLength.setForeground(new Color(255, 255, 0));
        labelBitT1TipToColleteLength.setHorizontalAlignment(SwingConstants.RIGHT);
        labelBitT1TipToColleteLength.setFont(new Font("Arial", Font.BOLD, 11));
        labelBitT1TipToColleteLength.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        panel.add(labelBitT1TipToColleteLength);
        
        t1BitCutTipToColleteLengthField = new JTextField("" + t1_drill_bit_tip_to_collete_length); //
        t1BitCutTipToColleteLengthField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        panel.add(t1BitCutTipToColleteLengthField);
        t1BitCutTipToColleteLengthField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                save();
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
        
        /*
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
        */
        
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
        
        /*
        cellHeight += 40;
        JLabel warningHeight = new JLabel("Note: 5-Axis routing is not production ready.");
        //warningHeight.setForeground(new Color(255, 255, 0));
        warningHeight.setHorizontalAlignment(SwingConstants.CENTER);
        warningHeight.setFont(new Font("Arial", Font.BOLD, 11));
        warningHeight.setBounds(0, cellHeight, 280, 40); // x, y, width, height
        panel.add(warningHeight);
        */
        
        UIManager.put("OptionPane.minimumSize",new Dimension(500, cellHeight + 80 + 40));
        
        load(); // read propertyies and set UI fields.
        
        ImageIcon iconImage = new ImageIcon(getClass().getResource("/armarender/Icons/favicon-32x32.png"));
        
        //int result = JOptionPane.showConfirmDialog(null, panel, "Five Axis CNC Properties", JOptionPane.OK_CANCEL_OPTION);
        int result = JOptionPane.showConfirmDialog(null, panel, "Five Axis CNC Configuration", JOptionPane.OK_CANCEL_OPTION, JOptionPane.OK_CANCEL_OPTION,  iconImage);
        if (result == JOptionPane.OK_OPTION) {
            //System.out.println("width value: " + widthField.getText());
            //System.out.println("depth value: " + depthtField.getText());
            //System.out.println("height value: " + heightField.getText());
            //System.out.println("bit value: " + bitField.getText());
            this.width = Integer.parseInt(widthField.getText());
            this.depth = Integer.parseInt(depthtField.getText());
            this.material_height = Double.parseDouble(heightField.getText());
            //this.depthPerPass = Double.parseDouble(depthPerPassField.getText());
            this.drill_bit = Double.parseDouble(bitField.getText());
            //this.rough_drill_bit = Double.parseDouble(roughBitField.getText());
            //this.accuracy = Double.parseDouble(accuracyField.getText());
            //this.drill_bit_angle = Double.parseDouble(bitAngleField.getText());
            this.nozzleDistance = Double.parseDouble(nozzleDistanceField.getText());
            //this.toolpathMarkup = toolpathCheck.isSelected();
            //this.cutOptimization = optimizationCheck.isSelected();
            //this.minimizePasses = minimizePassesCheck.isSelected();
            //this.simulateRoute = simulateCheck.isSelected();
            
            this.cAngleOrigin = Double.parseDouble(cAngleOriginField.getText());
            
            
            // Router
            this.fulcrumToHousingRear = Double.parseDouble(fulcrumToHousingRearField.getText());
            this.routerHousingDiameter = Double.parseDouble(routerHousingDiameterField.getText());
            this.colleteWidth = Double.parseDouble(colleteWidthField.getText());
            this.nozzleDistance = Double.parseDouble(nozzleDistanceField.getText()); // collete distance
            
            // Tools T1
            this.drill_bit = Double.parseDouble(bitField.getText()); // T1 Bit Diameter
            
            
            
            
            //return true;
        }
        
    }

    
    public static void loadProperties(){
        try {
            String path = new File(".").getCanonicalPath();
            //System.out.println("path: " + path);
            String propertyFileName = path + System.getProperty("file.separator") + "cam.properties";
            InputStream input = new FileInputStream(propertyFileName);
            // load a properties file
            prop.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
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
            
            //setBooleanProperty(prop, toolpathCheck, "ads.export_mill_5axis_toolpath_markup");
            //setBooleanProperty(prop, optimizationCheck, "ads.export_mill_5axis_cut_optimization");
            //setBooleanProperty(prop, minimizePassesCheck, "ads.export_mill_5axis_minimize_passes");
            //setBooleanProperty(prop, simulateCheck, "ads.export_mill_5axis_simulate");
            
            setStringProperty(prop, heightField, "ads.export_mill_5axis_material_height");
            //setStringProperty(prop, accuracyField, "ads.export_mill_5axis_accuracy"); // moved to other dialog
            setStringProperty(prop, bitField, "ads.export_mill_5axis_bit_diameter");
            //setStringProperty(prop, bitAngleField, "ads.export_mill_5axis_bit_angle");
            setStringProperty(prop, nozzleDistanceField, "ads.export_mill_5axis_bc_distance");
            setStringProperty(prop, cAngleOriginField, "ads.export_mill_5axis_c_angle_orientation");
            
            
            setStringProperty(prop, fulcrumToHousingRearField, "ads.export_mill_5axis_router_rear_length");
            setStringProperty(prop, routerHousingDiameterField, "ads.export_mill_5axis_router_housing_diameter");
            
            
            setStringProperty(prop, colleteWidthField, "ads.export_mill_5axis_collete_diameter");
            setStringProperty(prop, nozzleDistanceField, "ads.export_mill_5axis_collete_distance");
            
           
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
            
            //prop.setProperty("ads.export_mill_5axis_toolpath_markup", ""+toolpathCheck.isSelected());
            //prop.setProperty("ads.export_mill_5axis_cut_optimization", ""+optimizationCheck.isSelected());
            //prop.setProperty("ads.export_mill_5axis_minimize_passes", ""+minimizePassesCheck.isSelected());
            //prop.setProperty("ads.export_mill_5axis_simulate", ""+simulateCheck.isSelected());
            
            prop.setProperty("ads.export_mill_5axis_material_height", ""+heightField.getText());
            //prop.setProperty("ads.export_mill_5axis_accuracy", ""+accuracyField.getText()); // moved to other dialog
            prop.setProperty("ads.export_mill_5axis_bit_diameter", ""+bitField.getText());
            //prop.setProperty("ads.export_mill_5axis_bit_angle", ""+bitAngleField.getText());
            prop.setProperty("ads.export_mill_5axis_bc_distance", ""+nozzleDistanceField.getText());
            prop.setProperty("ads.export_mill_5axis_c_angle_orientation", ""+cAngleOriginField.getText());
            
            
            prop.setProperty("ads.export_mill_5axis_router_rear_length", ""+fulcrumToHousingRearField.getText());
            prop.setProperty("ads.export_mill_5axis_router_housing_diameter", ""+routerHousingDiameterField.getText());
            
            
            prop.setProperty("ads.export_mill_5axis_collete_diameter", ""+colleteWidthField.getText());
            prop.setProperty("ads.export_mill_5axis_collete_distance", ""+nozzleDistanceField.getText());
            
            
          
            // save properties to project root folder
            prop.store(output, null);
        } catch (IOException io) {
            io.printStackTrace();
        }
    }
    
    
    public static double getDoubleProperty(String property){
        double d = 0;
        String strValue = prop.getProperty(property);
        if(strValue == null){
            d = Double.parseDouble(strValue);
        }
        return d;
    }
    
    /**
     * getColleteDistance
     * Description: Load the length value for the B/C fulcrum to the collete length from the property file.
     */
    public static double getColleteDistance(){
        double value = 0;
        loadProperties();
        value = getDoubleProperty("ads.export_mill_5axis_collete_diameter");
        return value;
    }
    
    
    // TODO: Get other peroperties. Used by the router classes generating GCode.
}
