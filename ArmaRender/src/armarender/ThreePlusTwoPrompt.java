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
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


public class ThreePlusTwoPrompt {
    private Properties prop = new Properties();
    public JTextField cPositionField = null;
    public JTextField bPositionField = null;
    public JTextField accuracyField = null;
    public JComboBox toolSelectionField = null;
    public JTextField speedField = null;
    public JTextField depthField = null;
    public JCheckBox simulateCheck = null;

    private ArrayList<String>bitToUse = new ArrayList<>(Arrays.asList("T1", "T2", "T3", "T4", "T5", "T6")); // ArrayList instead of normal array because this might expand in the future
    private int savedToolIndex;

    public JCheckBox restMachiningCheck = null;
    public JLabel optimalDepthLabel = null;
    
    
    public ThreePlusTwoPrompt(boolean roughing){
        //prompt(roughing);
    }
    
    
    /**
     * prompt
     * Description: Prompt user for tool path generation information.
     */
    public boolean prompt(boolean roughing){
        
        JPanel panel = new JPanel();
        //panel.setBackground(new Color(0, 0, 0));
        panel.setSize(new Dimension(440, 600));
        panel.setLayout(null);
        
        int cellHeight = 20;
        int secondColX = 140; // 190
        int rowSpacing = 36;
        
        int labelWidth = 120; // 170
        int inputFieldWidth = 120;
        
        
        JLabel cPositionLabel = new JLabel("C Axis Position");
        //cPositionLabel.setForeground(new Color(255, 255, 0));
        cPositionLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        cPositionLabel.setFont(new Font("Arial", Font.BOLD, 11));
        cPositionLabel.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        panel.add(cPositionLabel);
        
        cPositionField = new JTextField(new String(15+""));
        cPositionField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        panel.add(cPositionField);
        //cPositionField.getDocument().addDocumentListener(myListener);
        
        cellHeight += rowSpacing;
        
        JLabel bPositionLabel = new JLabel("B Axis Position");
        //bPositionLabel.setForeground(new Color(255, 255, 0));
        bPositionLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        bPositionLabel.setFont(new Font("Arial", Font.BOLD, 11));
        bPositionLabel.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        panel.add(bPositionLabel);
        
        bPositionField = new JTextField( new String(45+""));
        bPositionField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        panel.add(bPositionField);
        
        //
        bPositionField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
                        public void insertUpdate(DocumentEvent e) {
                            onChange();
                        }

                        @Override
                        public void removeUpdate(DocumentEvent e) {
                            onChange();
                        }

                        @Override
                        public void changedUpdate(DocumentEvent e) {
                            onChange();
                        }


                    private void onChange() {
                        setOptimalCutDepth();
                    }
                });
        
        
        
        cellHeight += rowSpacing;
        
        JLabel toolSelectionLabel = new JLabel("Tool Selection");
        //toolSelectionLabel.setForeground(new Color(255, 255, 0));
        toolSelectionLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        toolSelectionLabel.setFont(new Font("Arial", Font.BOLD, 11));
        toolSelectionLabel.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        panel.add(toolSelectionLabel);
        
        toolSelectionField = new JComboBox<>(bitToUse.toArray(new String[0]));
        toolSelectionField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        panel.add(toolSelectionField);
        
        
        cellHeight += rowSpacing;
        
        JLabel accuracyLabel = new JLabel("Accuracy");
        //accuracyLabel.setForeground(new Color(255, 255, 0));
        accuracyLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        accuracyLabel.setFont(new Font("Arial", Font.BOLD, 11));
        accuracyLabel.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        panel.add(accuracyLabel);
        
        accuracyField = new JTextField( new String(0.128+""));
        accuracyField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        panel.add(accuracyField);
 
        cellHeight += rowSpacing;
        
        // Speed
        JLabel speedLabel = new JLabel("Speed (IPM)");
        //speedLabel.setForeground(new Color(255, 255, 0));
        speedLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        speedLabel.setFont(new Font("Arial", Font.BOLD, 11));
        speedLabel.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        panel.add(speedLabel);
        
        speedField = new JTextField( new String("50"));
        speedField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
        panel.add(speedField);
        
        
        // Max cut depth
        if(roughing){
            cellHeight += rowSpacing;
            
            JLabel depthLabel = new JLabel("Max Depth");
            //depthLabel.setForeground(new Color(255, 255, 0));
            depthLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            depthLabel.setFont(new Font("Arial", Font.BOLD, 11));
            depthLabel.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
            panel.add(depthLabel);
            
            depthField = new JTextField( new String("0.25"));
            depthField.setBounds(secondColX, cellHeight, inputFieldWidth, 40); // x, y, width, height
            panel.add(depthField);
            
            
            // Calculate optimal depth of cut based on angle of bit (B value) and the length of the cutting
            // portion of the bit.
            //double optimalDepth = 0;
            //double bitCutLength = 1.5; // TODO: get this from the config
            //double bAngle = getBValue();
            //optimalDepth = bitCutLength * Math.cos(bAngle);
            double optimalDepth = setOptimalCutDepth();
            
            optimalDepthLabel = new JLabel("Optimal Depth: " + roundThree(optimalDepth) + "\"");
            optimalDepthLabel.setHorizontalAlignment(SwingConstants.LEFT);
            optimalDepthLabel.setFont(new Font("Arial", Font.BOLD, 11));
            optimalDepthLabel.setBounds(secondColX + inputFieldWidth + 5 , cellHeight, labelWidth + 40, 40); // x, y, width, height
            panel.add(optimalDepthLabel);
        }
        
        
        cellHeight += rowSpacing;
        
        JLabel showMarkupLabel = new JLabel("Show Markup");
        //showMarkupLabel.setForeground(new Color(255, 255, 0));
        showMarkupLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        showMarkupLabel.setFont(new Font("Arial", Font.BOLD, 11));
        showMarkupLabel.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
        panel.add(showMarkupLabel);
        
        boolean simulateRoute = true;
        simulateCheck = new JCheckBox("");
        simulateCheck.setBounds(secondColX, cellHeight, 130, 40); // x, y, width, height
        simulateCheck.setSelected( simulateRoute );
        panel.add(simulateCheck);
        simulateCheck.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                
            }
        });
        
        
        if(!roughing){
            cellHeight += rowSpacing;
            
            JLabel restMachiningLabel = new JLabel("Rest Machining");
            //restMachiningLabel.setForeground(new Color(255, 255, 0));
            restMachiningLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            restMachiningLabel.setFont(new Font("Arial", Font.BOLD, 11));
            restMachiningLabel.setBounds(0, cellHeight, labelWidth, 40); // x, y, width, height
            panel.add(restMachiningLabel);
            
            boolean restMachining = true;
            restMachiningCheck = new JCheckBox("");
            restMachiningCheck.setBounds(secondColX, cellHeight, 130, 40); // x, y, width, height
            restMachiningCheck.setSelected( restMachining );
            panel.add(restMachiningCheck);
            restMachiningCheck.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    
                }
            });
        }
        
     
        
        UIManager.put("OptionPane.minimumSize",new Dimension(500, cellHeight + 80 + 40));
        
        load(roughing); // read propertyies and set UI fields.
        
        ImageIcon iconImage = new ImageIcon(getClass().getResource("/armarender/Icons/favicon-32x32.png"));
        
        //int result = JOptionPane.showConfirmDialog(null, panel, "Five Axis CNC Properties", JOptionPane.OK_CANCEL_OPTION);
        int result = JOptionPane.showConfirmDialog(null, panel, "Three + Two Axis CNC Path Generation", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,  iconImage);
        if (result == JOptionPane.OK_OPTION) {
            
            /*
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
            */
            save(roughing);

            return true;
        }
        
        return false;
    }
 
    
    /**
     * setOptimalCutDepth
     * Description:
     */
    public double setOptimalCutDepth(){
        double optimalDepth = 0;
        double bitCutLength = 1.5; // TODO: get this from the config
        double bAngle = Math.toRadians(getBValue());
        optimalDepth = bitCutLength * Math.cos(bAngle);
        if(optimalDepthLabel != null){
            optimalDepthLabel.setText( "Optimal Depth: " + roundThree( optimalDepth ) + "\"");
        }
        return optimalDepth;
    }
    
    /**
     * load
     *
     * Description: Load property file attributes and populate the UI fields.
     */
    public void load(boolean roughing){
        try {
            String dirPath = new File(".").getCanonicalPath() + System.getProperty("file.separator") + "config";
            File d = new File(dirPath);
            if (d.exists() == false) {
                d.mkdir();
            }

            String propertyFileName = dirPath + System.getProperty("file.separator") + "cam.properties";
            File propFile = new File(propertyFileName);
            if (propFile.exists() == false) {
                propFile.createNewFile();
            }

            InputStream input = new FileInputStream(propertyFileName);
            // load a properties file
            prop.load(input);
            
            /*
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
           */

            setStringProperty(prop, cPositionField, "ads.export_5axis_c_axis");
            setStringProperty(prop, bPositionField, "ads.export_5axis_b_axis");
            setStringProperty(prop, accuracyField, "ads.export_5axis_accuracy");
            setStringProperty(prop, speedField, "ads.export_5axis_speed");
            setBooleanProperty(prop, simulateCheck, "ads.export_5axis_markup");

            // Select saved tool
            String bitIndexStr = prop.getProperty("ads.export_bit_index");
            savedToolIndex = bitIndexStr == null ? 0 : Integer.parseInt(bitIndexStr); // Avoid null read when file does not exist
            toolSelectionField.setSelectedIndex(savedToolIndex);

            if (roughing)
                setStringProperty(prop, depthField, "ads.export_mill_5axis_max_depth");

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void save(boolean roughing) {
        try {
            String propertyFileName = new File(".").getCanonicalPath() + System.getProperty("file.separator") + "config" + System.getProperty("file.separator") + "cam.properties";
            OutputStream output = new FileOutputStream(propertyFileName);

            prop.setProperty("ads.export_5axis_c_axis", ""+cPositionField.getText());
            prop.setProperty("ads.export_5axis_b_axis", ""+bPositionField.getText());
            prop.setProperty("ads.export_5axis_accuracy", ""+accuracyField.getText());
            prop.setProperty("ads.export_5axis_speed", ""+speedField.getText());
            prop.setProperty("ads.export_5axis_markup", ""+simulateCheck.isSelected());

            // Check if user selected other tool
            int selectedToolIndex = toolSelectionField.getSelectedIndex();
            if (savedToolIndex != selectedToolIndex) {
                prop.setProperty("ads.export_bit_index", ""+selectedToolIndex);
                prop.setProperty("ads.export_bit_name", "T" + (selectedToolIndex+1));
            }

            if (roughing)
                prop.setProperty("ads.export_mill_5axis_max_depth", ""+depthField.getText());


            prop.store(output, null);
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
     * getCValue
     * Description:
     */
    public double getCValue(){
        double result = 0;
        if(cPositionField != null){
            result = Double.parseDouble(cPositionField.getText());
        }
        return result;
    }
    
    public double getBValue(){
        double result = 0;
        if(bPositionField != null && bPositionField.getText().length() > 0){
            result = Double.parseDouble(bPositionField.getText());
        }
        return result;
    }
    
    
    public double getAccuracy(){
        double result = 0;
        if(accuracyField != null){
            result = Double.parseDouble(accuracyField.getText());
        }
        return result;
    }

    public double getSpeed(){
        double result = 50;
        if(speedField != null){
            result = Double.parseDouble(speedField.getText()); 
        }
        return result;
    }
    
    
    public double getDepth(){
        double result = 0.25;
        if(depthField != null){
            result = Double.parseDouble(depthField.getText());
        }
        return result;
    }
    
    public boolean getDisplay(){
        boolean result = true;
        if(simulateCheck != null){
            result = simulateCheck.isSelected();
        }
        return result;
    }
    
    
    
    public boolean getRestMachining(){
        boolean result = true;
        if(restMachiningCheck != null){
            result = restMachiningCheck.isSelected();
        }
        return result;
    }
    
    
    String roundThree(double x){
        //double rounded = ((double)Math.round(x * 100000) / 100000);
        
        DecimalFormat df = new DecimalFormat("#");
            df.setMaximumFractionDigits(3);

        return df.format(x);
    }
}


