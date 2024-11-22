
package armarender;

import java.util.*;
import armarender.math.*;
import armarender.object.*;
import armarender.math.*;
import armarender.object.*;
import armarender.view.CanvasDrawer;
import java.io.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import buoy.widget.*;
import armarender.ui.*;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

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
    private double bed_height = 10; // 2 - 10 cut scene into layers this thick for seperate parts/files.
    private double nozzleDistance = 10.0; // .5 distance from nozzle to 4/5 axis.
    private double depthPerPass = 1.25; // max material that can be removed in one pass
    private double cAngleOrigin = 0; // c axis (rotation vertically) origin. Machine vs cad coordinate system may be different.
    private double bAngleOrigin = 0;
    private double colleteWidth = 2;
    private double routerHousingDiameter = 4.2;
    private double fulcrumToHousingRear = 6.2;
    
    ArrayList<String>bitToUse = new ArrayList<>(Arrays.asList("T1", "T2", "T3", "T4", "T5", "T6")); // ArrayList instead of normal array because this might expand in the future
    ArrayList<String> propFilesForBits = new ArrayList<>(Arrays.asList("t1.properties", "t2.properties", "t3.properties", "t4.properties", "t5.properties", "t6.properties"));  // Not an elegant way, but efficient

    String[] bitEndTypesToChoose = {"Ball Nose", "Flat End"};
    private double t1_drill_bit_cut_Length = 2;
    private double t1_drill_bit_tip_to_collete_length = 5;
    
    private boolean toolpathMarkup = false;
    private boolean cutOptimization = true;
    private boolean minimizePasses = true;
    private boolean simulateRoute = false;
    
    private static Properties prop = new Properties();
    private static Properties bitProps = new Properties();
    
    private JTextField heightField;
    private JTextField widthField;
    private JTextField depthField;

    private JTextField bitField;
    //private JTextField bitAngleField; // remove? replace with types (Flat end or ball nose)
    private JTextField nozzleDistanceField; // Collete dist
    private JTextField cAngleOriginField;
    private JTextField bAngleOriginField;
    private JTextField bitCutLengthField;
    private JTextField bitTipToColleteLengthField;
    private JTextField colleteWidthField;
    private JTextField routerHousingDiameterField;
    private JTextField fulcrumToHousingRearField;

    private JLabel labelBitDiameter;
    private JLabel labelBitEndType;
    private JLabel labelBitCutLength;
    private JLabel labelBitTipToColleteLength;

    private JCheckBox toolpathCheck;
    private JCheckBox optimizationCheck;
    private JCheckBox minimizePassesCheck;
    private JCheckBox simulateCheck;
    private JCheckBox invertBDirectionCheck;
    private JCheckBox invertCDirectionCheck;

    private JComboBox<String> usedBitComboBox;
    private JComboBox<String> bitEndComboBox;
    
    public FiveAxisConfig(){
        prompt();
    }

    /**
     * prompt
     * Description: Some fields are not saved to minimize the number of save
     */
    public void prompt(){
        

        // Different tabs to organize
        JPanel bedPanel = new JPanel();
        JPanel coordPanel = new JPanel();
        JPanel routerPanel = new JPanel();
        JPanel bitsPanel = new JPanel();

        bedPanel.setLayout(null);
        coordPanel.setLayout(null);
        routerPanel.setLayout(null);
        bitsPanel.setLayout(null);

        JTabbedPane tabbedPane = new JTabbedPane();
        
        int cellHeight = 20;
        int maxCellHeight = cellHeight;
        int secondColX = 190;
        int rowSpacing = 36;
        
        int labelWidth = 170;
        int inputFieldWidth = 130;
        
        //
        // Bed Dimensions
        //
        JLabel widthLabel = new JLabel("Bed Width");
        //widthLabel.setForeground(new Color(255, 255, 0));
        widthLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        widthLabel.setFont(new Font("Arial", Font.BOLD, 11));
        widthLabel.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        bedPanel.add(widthLabel);
        
        widthField = new JTextField(new String(width+""));
        widthField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        bedPanel.add(widthField);
        widthField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                save();
            }
        });

        //widthField.getDocument().addDocumentListener(myListener);
        cellHeight += rowSpacing;
             
        JLabel labelDepth = new JLabel("Bed Depth");
        //labelHeight.setForeground(new Color(255, 255, 0));
        labelDepth.setHorizontalAlignment(SwingConstants.RIGHT);
        labelDepth.setFont(new Font("Arial", Font.BOLD, 11));
        labelDepth.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        bedPanel.add(labelDepth);
        
        depthField = new JTextField( new String(depth+""));
        depthField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        bedPanel.add(depthField);
        depthField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                save();
            }
        });
        
        cellHeight += rowSpacing;
        JLabel labelHeight = new JLabel("Bed Height");
        //labelHeight.setForeground(new Color(255, 255, 0));
        labelHeight.setHorizontalAlignment(SwingConstants.RIGHT);
        labelHeight.setFont(new Font("Arial", Font.BOLD, 11));
        labelHeight.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        bedPanel.add(labelHeight);
        
        heightField = new JTextField(new String(bed_height+""));
        heightField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        bedPanel.add(heightField);
        heightField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                save();
            }
        });
        
        // Update maxCellHeight and reset cellHeight
        maxCellHeight = cellHeight;
        cellHeight = 20;

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
        // Offset C angle
        JLabel cAngleOriginLabel = new JLabel("C Axis Angle Origin");
        cAngleOriginLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        cAngleOriginLabel.setFont(new Font("Arial", Font.BOLD, 11));
        cAngleOriginLabel.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        coordPanel.add(cAngleOriginLabel);
        
        cAngleOriginField = new JTextField( new String(cAngleOrigin+""));
        cAngleOriginField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        coordPanel.add(cAngleOriginField);
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
        coordPanel.add(invertCDirectionLabel);

        invertCDirectionCheck = new JCheckBox("");
        invertCDirectionCheck.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        invertCDirectionCheck.setSelected( simulateRoute );
        coordPanel.add(invertCDirectionCheck);
        invertCDirectionCheck.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //save();
                //System.out.println("save minimize ");
            }
        });
        
        // Offset B angle
        cellHeight += rowSpacing;
        JLabel bAngleOriginLabel = new JLabel("B Axis Angle Origin");
        bAngleOriginLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        bAngleOriginLabel.setFont(new Font("Arial", Font.BOLD, 11));
        bAngleOriginLabel.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        coordPanel.add(bAngleOriginLabel);
        
        bAngleOriginField = new JTextField( new String(bAngleOrigin+""));
        bAngleOriginField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        coordPanel.add(bAngleOriginField);
        bAngleOriginField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                save();
            }
        });
          
        // Invert B
        cellHeight += rowSpacing;
        JLabel invertBDirectionLabel = new JLabel("Invert B Direction");
        //invertBDirectionLabel.setForeground(new Color(255, 255, 0));
        invertBDirectionLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        invertBDirectionLabel.setFont(new Font("Arial", Font.BOLD, 11));
        invertBDirectionLabel.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        coordPanel.add(invertBDirectionLabel);

        invertBDirectionCheck = new JCheckBox("");
        invertBDirectionCheck.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        invertBDirectionCheck.setSelected( simulateRoute );
        coordPanel.add(invertBDirectionCheck);
        invertBDirectionCheck.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //save();
                //System.out.println("save minimize ");
            }
        });

        // Update maxCellHeight and reset cellHeight
        maxCellHeight = cellHeight > maxCellHeight ? cellHeight : maxCellHeight; 
        cellHeight = 20;
        
        //
        // Router Dimensions
        //
        JLabel nozzleDistanceLabel = new JLabel("Fulcrum to Collete Distance"); // TODO
        nozzleDistanceLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        nozzleDistanceLabel.setFont(new Font("Arial", Font.BOLD, 11));
        nozzleDistanceLabel.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        routerPanel.add(nozzleDistanceLabel);
        
        nozzleDistanceField = new JTextField( new String(nozzleDistance+""));
        nozzleDistanceField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        routerPanel.add(nozzleDistanceField);
        nozzleDistanceField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                save();
            }
        });
        
        // Collete Width
        cellHeight += rowSpacing;
        JLabel colleteWidthLabel = new JLabel("Collete Width"); // TODO
        colleteWidthLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        colleteWidthLabel.setFont(new Font("Arial", Font.BOLD, 11));
        colleteWidthLabel.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        routerPanel.add(colleteWidthLabel);
        
        colleteWidthField = new JTextField( new String(colleteWidth+""));
        colleteWidthField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        routerPanel.add(colleteWidthField);
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
        routerPanel.add(routerHousingDiameterLabel);
        
        routerHousingDiameterField = new JTextField( new String(routerHousingDiameter+""));
        routerHousingDiameterField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        routerPanel.add(routerHousingDiameterField);
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
        routerPanel.add(fulcrumToHousingRearLabel);
        
        fulcrumToHousingRearField = new JTextField( new String(fulcrumToHousingRear+""));
        fulcrumToHousingRearField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        routerPanel.add(fulcrumToHousingRearField);
        fulcrumToHousingRearField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                save();
            }
        }); 
        
        // Update maxCellHeight and reset cellHeight
        maxCellHeight = cellHeight > maxCellHeight ? cellHeight : maxCellHeight; 
        cellHeight = 20;
        
        //
        // Bit properties.
        //
        JLabel labelUsedBit = new JLabel("Bit to Use");
        //labelBitDiameter.setForeground(new Color(255, 255, 0));
        labelUsedBit.setHorizontalAlignment(SwingConstants.RIGHT);
        labelUsedBit.setFont(new Font("Arial", Font.BOLD, 11));
        labelUsedBit.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        bitsPanel.add(labelUsedBit);

        usedBitComboBox = new JComboBox<>(bitToUse.toArray(new String[0]));
        usedBitComboBox.setBounds(secondColX, cellHeight, inputFieldWidth, 40);
        bitsPanel.add(usedBitComboBox);
        usedBitComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                setBitText();
                save();
            }
        });

        cellHeight += rowSpacing;
        labelBitDiameter = new JLabel("T1 Bit Diameter");
        //labelBitDiameter.setForeground(new Color(255, 255, 0));
        labelBitDiameter.setHorizontalAlignment(SwingConstants.RIGHT);
        labelBitDiameter.setFont(new Font("Arial", Font.BOLD, 11));
        labelBitDiameter.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        bitsPanel.add(labelBitDiameter);

        bitField = new JTextField("" + drill_bit); //
        bitField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        bitsPanel.add(bitField);
        bitField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                saveBitInfo();
            }
        });   
        
        // M1 Type (Ball / Flat End)
        cellHeight += rowSpacing;
        labelBitEndType = new JLabel("T1 Bit End Type");
        //labelBitEndType.setForeground(new Color(255, 255, 0));
        labelBitEndType.setHorizontalAlignment(SwingConstants.RIGHT);
        labelBitEndType.setFont(new Font("Arial", Font.BOLD, 11));
        labelBitEndType.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        bitsPanel.add(labelBitEndType);
        
        bitEndComboBox = new JComboBox<>(bitEndTypesToChoose);
        bitEndComboBox.setBounds(secondColX, cellHeight, inputFieldWidth, 40);
        bitsPanel.add(bitEndComboBox);
        bitEndComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                saveBitInfo();
            }
        });
        
        // Bit Cut Length
        cellHeight += rowSpacing;
        labelBitCutLength = new JLabel("T1 Bit Cutting Length");
        //labelBitCutLength.setForeground(new Color(255, 255, 0));
        labelBitCutLength.setHorizontalAlignment(SwingConstants.RIGHT);
        labelBitCutLength.setFont(new Font("Arial", Font.BOLD, 11));
        labelBitCutLength.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        bitsPanel.add(labelBitCutLength);
        
        bitCutLengthField = new JTextField("" + t1_drill_bit_cut_Length); //
        bitCutLengthField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        bitsPanel.add(bitCutLengthField);
        bitCutLengthField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                saveBitInfo();
            }
        });
        
        // Bit Tip to Collete distance
        cellHeight += rowSpacing;
        labelBitTipToColleteLength = new JLabel("T1 Bit Tip To Collete Length");
        //labelBitTipToColleteLength.setForeground(new Color(255, 255, 0));
        labelBitTipToColleteLength.setHorizontalAlignment(SwingConstants.RIGHT);
        labelBitTipToColleteLength.setFont(new Font("Arial", Font.BOLD, 11));
        labelBitTipToColleteLength.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        bitsPanel.add(labelBitTipToColleteLength);
        
        bitTipToColleteLengthField = new JTextField("" + t1_drill_bit_tip_to_collete_length); //
        bitTipToColleteLengthField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        bitsPanel.add(bitTipToColleteLengthField);
        bitTipToColleteLengthField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                saveBitInfo();
            }
        });

        // Update maxCellHeight and reset cellHeight
        maxCellHeight = cellHeight > maxCellHeight ? cellHeight : maxCellHeight; 
        cellHeight = 20;
        
        tabbedPane.addTab("Bed Dimensions", bedPanel);
        tabbedPane.addTab("Coordinate System", coordPanel);
        tabbedPane.addTab("Router Dimensions", routerPanel);
        tabbedPane.addTab("Bits", bitsPanel);

        // Add Tabbed pane to the main panel
        // panel.add(tabbedPane);
        
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
        int paneHeight = maxCellHeight + 120;
        UIManager.put("OptionPane.minimumSize",new Dimension(500, paneHeight));
        tabbedPane.setPreferredSize(new Dimension(450, paneHeight));

        load(); // read propertyies and set UI fields.
        ImageIcon iconImage = new ImageIcon(getClass().getResource("/armarender/Icons/favicon-32x32.png"));

        //int result = JOptionPane.showConfirmDialog(null, panel, "Five Axis CNC Properties", JOptionPane.OK_CANCEL_OPTION);
        int result = JOptionPane.showConfirmDialog(null, tabbedPane, "Five Axis CNC Configuration", JOptionPane.OK_CANCEL_OPTION, JOptionPane.OK_CANCEL_OPTION,  iconImage);
        if (result == JOptionPane.OK_OPTION) {
            //System.out.println("width value: " + widthField.getText());
            //System.out.println("depth value: " + depthField.getText());
            //System.out.println("height value: " + heightField.getText());
            //System.out.println("bit value: " + bitField.getText());
            this.width = Integer.parseInt(widthField.getText());
            this.depth = Integer.parseInt(depthField.getText());
            this.bed_height = Double.parseDouble(heightField.getText());
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
    
    public void loadProperties(String fileName, Properties prop) {
        try {
            String propertyFileName = new File(".").getCanonicalPath() + System.getProperty("file.separator") + "config" + System.getProperty("file.separator") + fileName;
            //System.out.println("path: " + path);
            InputStream input = new FileInputStream(propertyFileName);
            // load a properties file
            prop.load(input);
        } catch (FileNotFoundException ex) {
            // Check if the directory exists, then create new file
            createEmptyPropFile(fileName);
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

        loadProperties("cam.properties", prop);

        //setBooleanProperty(prop, toolpathCheck, "ads.export_mill_5axis_toolpath_markup");
        //setBooleanProperty(prop, optimizationCheck, "ads.export_mill_5axis_cut_optimization");
        //setBooleanProperty(prop, minimizePassesCheck, "ads.export_mill_5axis_minimize_passes");
        //setBooleanProperty(prop, simulateCheck, "ads.export_mill_5axis_simulate");
        
        // Bed dimension
        setStringProperty(prop, heightField, "ads.export_mill_5axis_bed_height");
        setStringProperty(prop, widthField, "ads.export_mill_5axis_bed_width");
        setStringProperty(prop, depthField, "ads.export_mill_5axis_bed_depth");

        // Coordinate System
        setStringProperty(prop, cAngleOriginField, "ads.export_mill_5axis_c_angle_orientation");
        setStringProperty(prop, bAngleOriginField, "ads.export_mill_5axis_b_angle_orientation");
        setBooleanProperty(prop, invertCDirectionCheck, "ads.export_mill_5axis_c_invert_direction");
        setBooleanProperty(prop, invertBDirectionCheck, "ads.export_mill_5axis_b_invert_direction");
        
        // Router Dimensions
        setStringProperty(prop, nozzleDistanceField, "ads.export_mill_5axis_collete_distance");
        setStringProperty(prop, colleteWidthField, "ads.export_mill_5axis_collete_diameter");            
        setStringProperty(prop, routerHousingDiameterField, "ads.export_mill_5axis_router_housing_diameter");
        setStringProperty(prop, fulcrumToHousingRearField, "ads.export_mill_5axis_router_rear_length");
        
        // Bits
        String bitIndexStr = prop.getProperty("ads.export_bit_index");
        int bitIndex = bitIndexStr == null ? 0 : Integer.parseInt(bitIndexStr); // Avoid null read when file does not exist
        usedBitComboBox.setSelectedIndex(bitIndex);
        setBitText();
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

    public void setJComboProperty(Properties prop, JComboBox field, String property) {
        String value = prop.getProperty(property);
        if (value!= null && field != null) {
            field.setSelectedItem(value);
        }
    }
    
    public String getProperty(String property){
        String value = prop.getProperty(property);
        if(value == null){
            
        }
        return value;
    }

    public void setBitText() {
        // Load data from bit file
        int bitIndex = usedBitComboBox.getSelectedIndex();
        String fileName = propFilesForBits.get(bitIndex);
        loadProperties(fileName, bitProps);
        
        // Update UI
        String usedBit = bitToUse.get(bitIndex);
        labelBitDiameter.setText(usedBit + " Bit Diameter");
        labelBitEndType.setText(usedBit + " Bit End Type");
        labelBitCutLength.setText(usedBit + " Bit Cutting Length");
        labelBitTipToColleteLength.setText(usedBit + " Bit Tip To Collete Length");

        setStringProperty(bitProps, bitField, "ads.export_mill_5axis_bit_diameter");
        setStringProperty(bitProps, bitCutLengthField, "ads.export_mill_5axis_bit_cutting_length");
        setStringProperty(bitProps, bitTipToColleteLengthField, "ads.export_mill_5axis_bit_cuttip_to_collete_length");
        setJComboProperty(bitProps, bitEndComboBox, "ads.export_mill_5axis_bit_end_type");
    }

    public void createEmptyPropFile(String fileName) {
        try {
            String dirPath = new File(".").getCanonicalPath() + System.getProperty("file.separator") + "config";
            File d = new File(dirPath);
            if (d.exists() == false) {
                d.mkdir();
            }

            String propertyFileName = dirPath + System.getProperty("file.separator") + fileName;
            OutputStream output = new FileOutputStream(propertyFileName);

            bitProps.store(output, null);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    } 

    public void saveBitInfo() {
        int bitIndex = usedBitComboBox.getSelectedIndex();
        try {
            String fileName = propFilesForBits.get(bitIndex);
            String propertyFileName = new File(".").getCanonicalPath() + System.getProperty("file.separator") + "config" + System.getProperty("file.separator") + fileName;

            OutputStream output = new FileOutputStream(propertyFileName);

            bitProps.setProperty("ads.export_mill_5axis_bit_diameter", ""+bitField.getText());
            bitProps.setProperty("ads.export_mill_5axis_bit_cutting_length", ""+bitCutLengthField.getText());
            bitProps.setProperty("ads.export_mill_5axis_bit_cuttip_to_collete_length", ""+bitTipToColleteLengthField.getText());
            bitProps.setProperty("ads.export_mill_5axis_bit_end_type", ""+bitEndComboBox.getSelectedItem());

            bitProps.store(output, null);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
    }

    /**
     * save
     *
     * Description: Save account information given in the input fields into the property file for persistant storage.
     */
    public void save(){
        try {
            String propertyFileName = new File(".").getCanonicalPath() + System.getProperty("file.separator") + "config" + System.getProperty("file.separator") + "cam.properties";
            OutputStream output = new FileOutputStream(propertyFileName);
            
            // set the properties value
            
            //prop.setProperty("ads.export_mill_5axis_toolpath_markup", ""+toolpathCheck.isSelected());
            //prop.setProperty("ads.export_mill_5axis_cut_optimization", ""+optimizationCheck.isSelected());
            //prop.setProperty("ads.export_mill_5axis_minimize_passes", ""+minimizePassesCheck.isSelected());
            //prop.setProperty("ads.export_mill_5axis_simulate", ""+simulateCheck.isSelected());
            
            // Bed dimensions
            prop.setProperty("ads.export_mill_5axis_bed_height", ""+heightField.getText());
            prop.setProperty("ads.export_mill_5axis_bed_width", ""+widthField.getText());
            prop.setProperty("ads.export_mill_5axis_bed_depth", ""+depthField.getText());

            // Coordinate system
            prop.setProperty("ads.export_mill_5axis_c_angle_orientation", ""+cAngleOriginField.getText());
            prop.setProperty("ads.export_mill_5axis_b_angle_orientation", ""+bAngleOriginField.getText());
            prop.setProperty("ads.export_mill_5axis_c_invert_direction", invertCDirectionCheck.isSelected() ? "true" : "false");
            prop.setProperty("ads.export_mill_5axis_b_invert_direction", invertBDirectionCheck.isSelected() ? "true" : "false");

            // Router dimensions            
            prop.setProperty("ads.export_mill_5axis_collete_distance", ""+nozzleDistanceField.getText());
            prop.setProperty("ads.export_mill_5axis_collete_diameter", ""+colleteWidthField.getText());
            prop.setProperty("ads.export_mill_5axis_router_housing_diameter", ""+routerHousingDiameterField.getText());
            prop.setProperty("ads.export_mill_5axis_router_rear_length", ""+fulcrumToHousingRearField.getText());

            // Bits
            prop.setProperty("ads.export_bit_index", ""+String.valueOf(usedBitComboBox.getSelectedIndex()));
            prop.setProperty("ads.export_bit_number", ""+String.valueOf(bitToUse.size()));
            prop.setProperty("ads.export_bit_name", ""+usedBitComboBox.getSelectedItem());
            
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
    // public static double getColleteDistance(){
    //     double value = 0;
    //     loadProperties();
    //     value = getDoubleProperty("ads.export_mill_5axis_collete_diameter");
    //     return value;
    // }
    
    
    // TODO: Get other peroperties. Used by the router classes generating GCode.
}
