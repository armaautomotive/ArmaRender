/* Copyright (C) 2018 - 2022 by Jon Taylor
 
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
import javax.swing.*; // For JOptionPane
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

/**
 * ComputationalFluidDynamics
 *
 * Description:
 *
 * Z depth object padding. if polygon collided is thin it passes without detection. padded
 * TODO: fluid dents to rise even when it isn't the closest path around an object.
 */
public class ComputationalFluidDynamics extends Thread {
    boolean running = false;
    private Vector<ObjectInfo> objects;
    LayoutWindow window;
    private double minx = 99999;
    private double miny = 99999;
    private double minz = 99999;
    private double maxx = -999999;
    private double maxy = -999999;
    private double maxz = -999999;
    //private int pointsPerLength = 9; // 13;
    private int pointsPerLength = 12;
    //private int pointsPerLength = 14;
    
    private double atmosphereTemperature = 1.0;
    private double highPressureForce = 0.05;
    private double lowPressureForce = 1;
    
    double width;
    double height;
    double depth;
    
    double xSegmentWidth;
    double ySegmentWidth;
    double zSegmentWidth;
    
    double volume;
    
    private double cod = 0;
    private double codCalc = 0;
    private boolean textCleared = false;
    private boolean calibrate = false;
    
    Vector<FluidPointObject> pointObjects = new Vector<FluidPointObject>();
    
    public ComputationalFluidDynamics(){
        objects = null;
    }
    
    //
    // weightsStruct
    //
    /*
     // A (drag X+Y),
     // B (zPressure)
     // C (drag vaccume)
     // D (Z StepMove)
     // E (Volume)
     // F (Scale)
     */
    String weightDNA = "555555";
    
    
    
    /**
     * getUserInput
     *
     * Description:
     */
    public boolean getUserInput(){
        JPanel panel = new JPanel();
        //panel.setBackground(new Color(0, 0, 0));
        panel.setSize(new Dimension(390, 32));
        panel.setLayout(null);
        
        int yPos = 0;
        
        JLabel widthLabel = new JLabel("Particles Per Width");
        //widthLabel.setForeground(new Color(255, 255, 0));
        widthLabel.setHorizontalAlignment(SwingConstants.CENTER);
        widthLabel.setFont(new Font("Arial", Font.BOLD, 11));
        widthLabel.setBounds(0, 0, 130, 40); // x, y, width, height
        panel.add(widthLabel);
        
        JTextField pointsPerLengthField = new JTextField(new String(pointsPerLength+""));
        pointsPerLengthField.setBounds(130, yPos, 130, 40); // x, y, width, height
        panel.add(pointsPerLengthField);
        //widthField.getDocument().addDocumentListener(myListener);
        
        yPos += 40;
        
        JLabel highPressureLabel = new JLabel("High Pressure Force");
        highPressureLabel.setFont(new Font("Arial", Font.BOLD, 11));
        highPressureLabel.setBounds(0, yPos, 130, 40); // x, y, width, height
        panel.add(highPressureLabel);
        JTextField highPressureField = new JTextField(new String(highPressureForce+""));
        highPressureField.setBounds(130, yPos, 130, 40); // x, y, width, height
        panel.add(highPressureField);
        
        yPos += 40;
        
        JLabel lowPressureLabel = new JLabel("Low Pressure Force");
        lowPressureLabel.setFont(new Font("Arial", Font.BOLD, 11));
        lowPressureLabel.setBounds(0, yPos, 130, 40); // x, y, width, height
        panel.add(lowPressureLabel);
        JTextField lowPressureField = new JTextField(new String(lowPressureForce+""));
        lowPressureField.setBounds(130, yPos, 130, 40); // x, y, width, height
        panel.add(lowPressureField);
        
        
        
        UIManager.put("OptionPane.minimumSize",new Dimension(400, 350 + 80));
        int result = JOptionPane.showConfirmDialog(null, panel, "Computational Fluid Dynamics", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            //System.out.println("width value: " + widthField.getText());
            //System.out.println("depth value: " + depthtField.getText());
            //System.out.println("height value: " + heightField.getText());
            //System.out.println("bit value: " + bitField.getText());
            this.pointsPerLength = Integer.parseInt(pointsPerLengthField.getText());
            
            this.highPressureForce = Double.parseDouble(highPressureField.getText());
            this.lowPressureForce = Double.parseDouble(lowPressureField.getText());
            
            //this.depth = Integer.parseInt(depthtField.getText());
            //this.material_height = Double.parseDouble(heightField.getText());
            //this.drill_bit = Double.parseDouble(bitField.getText());
            //this.accuracy = Double.parseDouble(accuracyField.getText());
            //this.drill_bit_angle = Double.parseDouble(bitAngleField.getText());
            //this.toolpathMarkup = toolpathCheck.isSelected();
            //this.cutOptimization = optimizationCheck.isSelected();
            //this.minimizePasses = minimizePassesCheck.isSelected();
            return true;
        }
        
        return false;
    }

    public void setObjects(Vector<ObjectInfo> objects){
        this.objects = objects;
    }
    
    public void setLayoutWindow(LayoutWindow window){
        this.window = window;
        
        ViewerCanvas canvas = window.getView();
        //Camera theCamera = canvas.getCamera();
        //CanvasDrawer drawer = canvas.getCanvasDrawer();
        if(canvas instanceof SceneViewer){
            ((SceneViewer)canvas).setCFD(this);
        }
    }

    public boolean isRunning(){
        return running;
    }
    
    public void stopCFD(){
        running = false;
        calibrate = false;
        
        // Clear Text
        clearText();
        
        // Remove fluid points
        
        for(int i = 0; i < pointObjects.size(); i++){
            FluidPointObject fluidPoint = pointObjects.elementAt(i);
            
            //fluidPoint = null;
            //int id = ((LayoutWindow)window).addObjectL(fluidPoint); // This is bad. don't add this way later.
            //fluidPoint.setId(id);
            int id = fluidPoint.getId();
            
            ((LayoutWindow)window).removeObjectL( fluidPoint);
            //((LayoutWindow)window).removeObject(id, null);
            
        }
    }
    
    public void calibrate(){
        calibrate = true;
        start();
    }
    
    
    /**
     * run
     *
     */
    public void run(){
        //if(running){ // Stop existing process
        //    System.out.println("Exiting CFD");
        //    running = false;
        //    return;
        //}
        
        if(calibrate){
            
        }
        
        setup();
        while(running){
            process();
        }
    }
    
    
    /**
     * setup
     *
     */
    public void setup(){
        running = true;
        textCleared = false;
        calculateBounds(objects);
        java.util.Random random = new java.util.Random();
        LayoutModeling layout = new LayoutModeling();
        
        System.out.println(" x " + this.minx + " " + this.maxx +
                           " y " + this.miny + " " + this.maxy +
                           " z " + this.minz + " " + this.maxz + " ");
        
        // Expand Scale
        width = (maxx - minx);
        minx -= (width/1.7);
        maxx += (width/1.7);
        height = (maxy - miny);
        miny -= (height/1.7);
        maxy += (height/1.7);
        //minz -= ((maxz - minz)/ (1.0) ); // Extend
        depth = (maxz - minz);
        minz -= (depth / 1.7) + depth;
        //maxz += ((maxz - minz)/ (1.2) );
        maxz += (depth / 1.7) + depth;
        
        xSegmentWidth = (maxx - minx) / (pointsPerLength-1);
        ySegmentWidth = (maxy - miny) / (pointsPerLength-1);
        zSegmentWidth = (maxz - minz) / (pointsPerLength*1);
        
        // place fluid points
        for(int z = 0; z < pointsPerLength * 1; z++){ // * 2
            for(int x = 0; x < pointsPerLength; x++){
                for(int y = 0; y < pointsPerLength; y++){
                
                    Vec3[] vertex = new Vec3[3];
                    Vec3 vec = new Vec3(minx + (x * xSegmentWidth) , // + (xSegmentWidth/2)
                                        miny + (y * ySegmentWidth) , // + (xSegmentWidth/2)
                                        minz + (z * zSegmentWidth)  ); // - (zSegmentWidth*pointsPerLength)
                    vertex[0] = vec;
                    vertex[1] = vec;
                    vertex[2] = vec;
                    float s[];
                    s = new float[3];
                    s[0] = 0;
                    s[1] = 0;
                    s[2] = 0;
                    int smoothing = Mesh.APPROXIMATING;
                    CoordinateSystem coords;
                    
                    Vec3[] resetLocation = new Vec3[3];
                    Vec3 resetVec = new Vec3(minx + (x * xSegmentWidth) , // + (xSegmentWidth/2)
                                             miny + (y * ySegmentWidth) , // + (xSegmentWidth/2)
                                             maxz);
                    resetLocation[0] = resetVec;
                    resetLocation[1] = resetVec;
                    resetLocation[2] = resetVec;
                    
                    FluidPointObject fluidPoint = new FluidPointObject(vertex, s, smoothing, false);
                    
                    fluidPoint.setResetLocation(resetLocation);
                    
                    pointObjects.addElement(fluidPoint);
                    
                    //
                    ObjectInfo info = new ObjectInfo(fluidPoint, new CoordinateSystem(), "" /*no name*/);
                    //UndoRecord undo = new UndoRecord(window, false);
                    int id = ((LayoutWindow)window).addObjectL(fluidPoint); // This is bad. don't add this way later.
                    fluidPoint.setId(id);
                    // This only reason we want it in the scene is to render we don't want it in the menu
                }
                window.repaint();
            }
            //System.out.print(".");
        }
        System.out.println("Points placed.");
        
        volume = sceneMeshVolume();
    }
    
    
    /**
     * process
     *
     */
    public void process(){
        LayoutModeling layout = new LayoutModeling();
        // Move fluid points through region around objects calculating diflections.
        //while(running){
            
            //cod = 0; // Coeficient of drag.
            codCalc = 0;
            double drag = 0;
            
            //System.out.println(" points: " + pointObjects.size() );
            for(int i = 0; i < pointObjects.size() && running; i++){
                FluidPointObject fluidPoint = pointObjects.elementAt(i);
                Vec3 location = fluidPoint.getLocation();
                // MeshVertex[] getVertices()
                // movePoint
                //fluidPoint.movePoint(  );
                Vec3 [] points = fluidPoint.getVertexPositions();
                
                // Update Prev Points
                fluidPoint.updatePreviousPoints(); // prev points form trail
                
                int v = 0;
                double distanceToFace = 999;
                //for(int v = 0; v < 1; v++ ){ //  points.length
                    
                    // Detect collisions.
                    boolean collide = false;
                    double collideZ = 0;
                    for (ObjectInfo obj : objects){
                        if(obj.getName().indexOf("Camera") < 0 &&
                           obj.getName().indexOf("Light") < 0 &&
                           //obj.getClass() != FluidPointObject.class
                           obj.getName().equals("") == false &&
                           obj.isVisible() &&
                           obj.isHiddenByParent() == false &&
                           running
                        ){
                            //System.out.println("Object Info: ");
                            //Object3D co = (Object3D)obj.getObject();
                            //System.out.println("obj " + obj.getId() + "  " + obj.getName() );
                            // obj.getObject(); // Object3D
                            Object3D o3d = obj.getObject();
                            
                            CoordinateSystem c;
                            c = layout.getCoords(obj);
                            Vec3 objOrigin = c.getOrigin();
                            //System.out.println(" obj origin " + objOrigin.x + " " + objOrigin.y + " " + objOrigin.z );
                            
                            BoundingBox bounds = o3d.getBounds(); // does not include location
                            
                            bounds = new BoundingBox(bounds); // clone bounds
                            
                            //bounds.minx += objOrigin.x; bounds.maxx += objOrigin.x; // NO
                            //bounds.miny += objOrigin.y; bounds.maxy += objOrigin.y;
                            //bounds.minz += objOrigin.z; bounds.maxz += objOrigin.z;
                            
                            //System.out.println(" obj bounds    x: " + bounds.minx + " - " + bounds.maxx + "  y: " +   bounds.miny + " " + bounds.maxy);
                            
                            // If object is TriangleMesh, create bounds from reach face coordinates. Not accurate but better than object bounds.
                            if(obj.getObject().canConvertToTriangleMesh() != Object3D.CANT_CONVERT){
                                
                                // Only if point in object bounds. If it isn't none of the faces can collide.
                                if(location.x >= bounds.minx && location.x <= bounds.maxx &&
                                   location.y >= bounds.miny && location.y <= bounds.maxy &&
                                   location.z >= bounds.minz - 0.01 && location.z <= bounds.maxz + 0.01){
                                
                                    TriangleMesh triangleMesh = null;
                                    triangleMesh = obj.getObject().convertToTriangleMesh(0.05);
                                    MeshVertex[] verts = triangleMesh.getVertices();
                                    //TriangleMesh.Edge[] edges = ((TriangleMesh)triangleMesh).getEdges();
                                    TriangleMesh.Face[] faces = triangleMesh.getFaces();
                                    
                                    for(int f = 0; f < faces.length && running; f++){
                                        TriangleMesh.Face face = faces[f];
                                        Vec3 vec1 = new Vec3(verts[face.v1].r); // duplicate
                                        Vec3 vec2 = new Vec3(verts[face.v2].r);
                                        Vec3 vec3 = new Vec3(verts[face.v3].r);
                                        
                                        Mat4 mat4 = c.duplicate().fromLocal();
                                        mat4.transform(vec1);
                                        mat4.transform(vec2);
                                        mat4.transform(vec3);
                                        
                                        //if(vec1.x < bounds.minx || vec1.x > bounds.maxx){
                                        //    System.out.println(" err  face outside bounds " + vec1.x + " o "+ objOrigin.x + " : " + bounds.minx + " - " +  bounds.maxx);
                                        //}
                                        
                                        BoundingBox faceBounds = new BoundingBox(bounds);
                                        faceBounds.minx = Math.min(Math.min(vec1.x, vec2.x), vec3.x);
                                        faceBounds.maxx = Math.max(Math.max(vec1.x, vec2.x), vec3.x);
                                        faceBounds.miny = Math.min(Math.min(vec1.y, vec2.y), vec3.y);
                                        faceBounds.maxy = Math.max(Math.max(vec1.y, vec2.y), vec3.y);
                                        faceBounds.minz = Math.min(Math.min(vec1.z, vec2.z), vec3.z);
                                        faceBounds.maxz = Math.max(Math.max(vec1.z, vec2.z), vec3.z);
                                        if(location.x >= faceBounds.minx && location.x <= faceBounds.maxx &&
                                           location.y >= faceBounds.miny && location.y <= faceBounds.maxy &&
                                           location.z >= faceBounds.minz - 0.01 && location.z <= faceBounds.maxz + 0.01){
                                            collide = true;
                                        }
                                        
                                        
                                        
                                        // calculate distance to collision surface
                                        //
                                        if(inside_frontal_trigon(location, vec1, vec2, vec3)){
                                            // ***
                                            double currDistance = trigon_depth(location, vec1, vec2, vec3);
                                            if(currDistance < distanceToFace){
                                                distanceToFace = currDistance;
                                            }
                                            
                                            // set distance in variable of FluidPointObject fluidPoint
                                        }
                                         
                                        
                                    } // faces
                                } // fluid point in object bounds (optimization)
                            } else {
                                // Default collision method.
                                
                                // Single object bounds. Simple, not accurate.
                                if(location.x >= bounds.minx && location.x <= bounds.maxx &&
                                   location.y >= bounds.miny && location.y <= bounds.maxy &&
                                   location.z >= bounds.minz - 0.01 && location.z <= bounds.maxz + 0.01){
                                    collide = true;
                                    //System.out.println(" c obj " +obj.getName() );
                                }
                                
                            }
                            
                        } // detectable/usable object
                    } // Objects
                
                    // Collision detection, Mesh objects. collide
                
                    if(distanceToFace > 100){ // ray casting no collide detected. (Bounds detection can be larger in area than actual)
                        collide = false;
                        //System.out.println("distanceToFace: " + distanceToFace);
                    } else {
                        //System.out.println("distanceToFace: " + distanceToFace);
                    }
                    
                    // Detect pressure from adjacent fluid points that are too close or too far.
                    // Too close psi > 1.0, too far psi < 1.0
                    
                    fluidPoint.setPSI(1.0);
                    double pressureAbove = 1.0; // 0.0;
                    double pressureBelow = 1.0;
                    double pressureLeft = 1.0;
                    double pressureRight = 1.0;
                    
                    double vacumeLeft = 1.0;
                    double vacumeRight =  1.0;
                    double vacumeAbove = 1.0;
                    double vacumeBelow = 1.0;
                
                    double vacumeLeftDist = 1.0; // Experimental
                    double vacumeRightDist =  1.0;
                    double vacumeAboveDist = 1.0;
                    double vacumeBelowDist = 1.0;
                
                    drag = 0;
                    
                    // TODO: calculate point psi also based on distance to collision
                
                
                
                    // Calculate point psi values based on proximity of adjacent points.
                    for(int f = 0; f < pointObjects.size() && running; f++){ // optimise later with index data structures.
                        if(f != i){
                            FluidPointObject compareFluidPoint = pointObjects.elementAt(f);
                            double distance = fluidPoint.getLocation().distance(compareFluidPoint.getLocation());
                            boolean ignore = false;
                            if(distance > zSegmentWidth * 2.5){
                                ignore = true;
                            }
                            if(!ignore){
                                double fluidPSI = 1;
                                double xPressure = ((xSegmentWidth / distance) * highPressureForce); // highPressureForce 0.05
                                double yPressure = ((ySegmentWidth / distance) * highPressureForce);
                                double zPressure = ((zSegmentWidth / distance) * highPressureForce);
                                fluidPSI = Math.max(xPressure, Math.max(yPressure, zPressure));
                                if(fluidPSI > fluidPoint.getPSI()){
                                    fluidPoint.setPSI(fluidPSI);
                                    //System.out.println("fluidPSI: " + fluidPSI);
                                }
                            }
                        }
                    }
                
                    // Compare this point (fluidPoint) with other points (compareFluidPoint) too see if they are too close or too far
                    for(int f = 0; f < pointObjects.size() && running; f++){ // optimise later with index data structures.
                        if(f != i){
                            FluidPointObject compareFluidPoint = pointObjects.elementAt(f);
                            double distance = fluidPoint.getLocation().distance(compareFluidPoint.getLocation());
                            
                            boolean vertical = false;
                            boolean horizontal = false;
                            
                            
                            boolean ignore = false;
                            if(distance > zSegmentWidth * 2.5){
                                ignore = true;
                            }
                            //System.out.println(" dist " + distance + " " + zSegmentWidth);
                            
                            if(!ignore){
                                double xDiff = Math.abs(fluidPoint.getLocation().x - compareFluidPoint.getLocation().x);
                                double yDiff = Math.abs(fluidPoint.getLocation().y - compareFluidPoint.getLocation().y);
                                double zDiff = Math.abs(fluidPoint.getLocation().z - compareFluidPoint.getLocation().z);
                                
                                // Left
                                if( compareFluidPoint.getLocation().x < fluidPoint.getLocation().x &&   // compare is too close on the left
                                   distance <= (xSegmentWidth * 3.1) // TODO: not just X but also Y
                                   ){
                                    if(compareFluidPoint.getPSI() > fluidPoint.getPSI() + 0.0){         // ? document this.
                                        //if(distance <= (xSegmentWidth * 1.5)){
                                        //    pressureLeft += compareFluidPoint.getPSI() - fluidPoint.getPSI();
                                            
                                        //}
                                        //if(distance <= (xSegmentWidth * 1.9)){
                                        //    pressureLeft += (compareFluidPoint.getPSI() - fluidPoint.getPSI()) / 3;
                                        //}
                                        
                                        pressureLeft += Math.sqrt(compareFluidPoint.getPSI() - fluidPoint.getPSI());
                                        
                                        //pressureLeft += (compareFluidPoint.getPSI() - fluidPoint.getPSI()) * (distance / xSegmentWidth)   ;
                                        //double pressure = (xSegmentWidth / distance) * 14;
                                        //System.out.println(" pressure left " + pressureLeft + "  d: " + distance + " w " + xSegmentWidth);
                                        //if(pressure > pressureLeft){ // distance < (xSegmentWidth * 1.2) &&
                                        //    pressureLeft = pressure; // Pressure is set relative ambient 14.
                                            //System.out.println(" * ");
                                        //}
                                    }
                                    //fluidPoint.setPSI(fluidPoint.getPSI() + (distance / xSegmentWidth));
                                }
                                // Right
                                if( compareFluidPoint.getLocation().x > fluidPoint.getLocation().x &&
                                   distance < (xSegmentWidth * 3.1)
                                   ){
                                    if(compareFluidPoint.getPSI() > fluidPoint.getPSI() + 0.0){
                                        pressureRight += Math.sqrt(compareFluidPoint.getPSI() - fluidPoint.getPSI());
                                        
                                        //if(distance < (xSegmentWidth * 1.5)){
                                        //    pressureRight += compareFluidPoint.getPSI() - fluidPoint.getPSI();
                                        //}
                                        //if(distance < (xSegmentWidth * 1.9)){
                                        //    pressureRight += (compareFluidPoint.getPSI() - fluidPoint.getPSI()) / 3;
                                        //}
                                        //double pressure = (xSegmentWidth / distance) * 14;
                                        //if(pressure > pressureRight){ // distance < (xSegmentWidth * 1.0) &&
                                        //    pressureRight = pressure; // Pressure is set relative ambient 14.
                                        //}
                                        
                                    }
                                    //fluidPoint.setPSI(fluidPoint.getPSI() + (distance / xSegmentWidth)); // compare is too close on right
                                }
                                // Down
                                if( compareFluidPoint.getLocation().y < fluidPoint.getLocation().y &&
                                   distance < (ySegmentWidth * 3.1)
                                   ){
                                    if(compareFluidPoint.getPSI() > fluidPoint.getPSI() + 0.0){
                                        pressureBelow += Math.sqrt(compareFluidPoint.getPSI() - fluidPoint.getPSI());
                                        //if(distance < (ySegmentWidth * 1.5)){
                                        //    pressureBelow += compareFluidPoint.getPSI() - fluidPoint.getPSI();
                                        //}
                                        //if(distance < (ySegmentWidth * 1.9)){
                                        //    pressureBelow += (compareFluidPoint.getPSI() - fluidPoint.getPSI() / 3);
                                        //}
                                        //double pressure = (ySegmentWidth / distance) * 14;
                                        //if( pressure > pressureBelow){ // distance < (ySegmentWidth * 1.0) &&
                                        //    pressureBelow = pressure; // Pressure is set relative ambient 14.
                                        //}
                                    }
                                    //fluidPoint.setPSI(fluidPoint.getPSI() + (distance / ySegmentWidth));
                                }
                                // Up
                                if( compareFluidPoint.getLocation().y > fluidPoint.getLocation().y &&
                                   distance < (ySegmentWidth * 3.1)
                                   ){
                                    if(compareFluidPoint.getPSI() > fluidPoint.getPSI() + 0.0){
                                        pressureAbove += Math.sqrt(compareFluidPoint.getPSI() - fluidPoint.getPSI());
                                        //if(distance < (ySegmentWidth * 1.5)){
                                        //    pressureAbove += compareFluidPoint.getPSI() - fluidPoint.getPSI();
                                        //}
                                        //if(distance < (ySegmentWidth * 1.9)){
                                        //    pressureAbove += (compareFluidPoint.getPSI() - fluidPoint.getPSI()) / 3;
                                        //}
                                        //double pressure = (ySegmentWidth / distance) * 14;
                                        //if( pressure > pressureAbove){ // distance < (ySegmentWidth * 1.0) &&
                                        //    pressureAbove = pressure; // Pressure is set relative ambient 14.
                                        //}
                                    }
                                    //fluidPoint.setPSI(fluidPoint.getPSI() + (distance / ySegmentWidth)); // compare is too close above
                                }
                                
                                //
                                // Vacume (Buggy. Even if an adjacent point is slightly above canceles out. Should calculate pressure in directions, )
                                //
                                if( compareFluidPoint.getLocation().x < fluidPoint.getLocation().x &&
                                   distance < (xSegmentWidth * 1.7)
                                   && xDiff > yDiff  // compare point is more horizontal than  vertical
                                   && zDiff <= (zSegmentWidth/2)
                                   //&& compareFluidPoint.getPSI() > fluidPoint.getPSI() + 0.01
                                   ){
                                    vacumeLeft = 0.0;
                                }
                                if( compareFluidPoint.getLocation().x > fluidPoint.getLocation().x &&
                                   distance < (xSegmentWidth * 1.7)
                                   && ( xDiff > yDiff ) // compare point is more horizontal than  vertical
                                   && zDiff <= (zSegmentWidth/2)
                                   //&& compareFluidPoint.getPSI() > fluidPoint.getPSI() + 0.01
                                   ){
                                    vacumeRight = 0.0;
                                    
                                    //vacumeRightDist = distance;
                                }
                                if( compareFluidPoint.getLocation().y > fluidPoint.getLocation().y && // above
                                   distance < (ySegmentWidth * 1.7)
                                   && ( xDiff < yDiff ) // vert diff < hor diff
                                   && zDiff <= (zSegmentWidth/2)
                                //   //&& compareFluidPoint.getPSI() > fluidPoint.getPSI() + 0.01
                                   ){
                                    //if(distance < (ySegmentWidth * 1.2)){
                                        vacumeAbove = 0.0;
                                    //}
                                    //pressureAbove -= 0.5;
                                    //System.out.print("+");
                                }
                                if( compareFluidPoint.getLocation().y < fluidPoint.getLocation().y &&
                                   distance < (ySegmentWidth * 1.7)
                                   && ( xDiff < yDiff ) // vert diff < hor diff
                                   && zDiff <= (zSegmentWidth/2)
                                   //   //&& compareFluidPoint.getPSI() > fluidPoint.getPSI() + 0.01
                                   ){
                                    vacumeBelow = 0.0;
                                    //pressureBelow -= 0.5;
                                    //System.out.print("-");
                                }
                                
                                // ***
                                if( compareFluidPoint.getLocation().x < fluidPoint.getLocation().x &&
                                   distance < (xSegmentWidth * 2.5)
                                   && ( xDiff > yDiff ) // compare point is more horizontal than  vertical
                                   && zDiff <= (zSegmentWidth/2)
                                   //&& compareFluidPoint.getPSI() > fluidPoint.getPSI() + 0.01
                                   ){
                                    //if(distance > vacumeLeftDist){
                                        vacumeLeftDist += (distance / xSegmentWidth);
                                    //}
                                }
                                if( compareFluidPoint.getLocation().x > fluidPoint.getLocation().x &&
                                   distance < (xSegmentWidth * 2.5)
                                   && ( xDiff > yDiff ) // compare point is more horizontal than  vertical
                                   && zDiff <= (zSegmentWidth/2)
                                   //&& compareFluidPoint.getPSI() > fluidPoint.getPSI() + 0.01
                                   ){
                                    //if(distance > vacumeRightDist){
                                        vacumeRightDist += (distance / xSegmentWidth);
                                    //}
                                }
                                if( compareFluidPoint.getLocation().y > fluidPoint.getLocation().y && // above
                                   distance < (ySegmentWidth * 2.5)
                                   && ( xDiff < yDiff ) // compare point is more vertical than horizontal
                                   && zDiff <= (zSegmentWidth/2)
                                   //&& compareFluidPoint.getPSI() > fluidPoint.getPSI() + 0.01
                                   ){
                                    //if(distance > vacumeAboveDist){
                                        vacumeAboveDist += (distance / ySegmentWidth);
                                    //}
                                }
                                if( compareFluidPoint.getLocation().y < fluidPoint.getLocation().y &&
                                   distance < (ySegmentWidth * 2.5)
                                   && ( xDiff < yDiff ) // compare point is more below than beside
                                   && zDiff <= (zSegmentWidth/2)
                                   //   //&& compareFluidPoint.getPSI() > fluidPoint.getPSI() + 0.01
                                   ){
                                    //if(distance > vacumeBelowDist){
                                        vacumeBelowDist += (distance / ySegmentWidth);
                                    //}
                                }
                                
                                
                            
                            } // !ignore
                        }
                    }
                    
                    double zStep = 0.054;  //
                    double xStep = 0.0005; // 0.0003;
                    double yStep = 0.0005;
                
                    double zStepMove = 0.0; // rename step -> Tick,  cycle
                    double zStepSlow = 0.0;
                    double xStepMove = 0.0;
                    double yStepMove = 0.0;
                
                    // High Pressure
                    if(collide == false){
                        //System.out.println(" X " + fluidPoint.getPSI() );
                        double psiScale = Math.log10(fluidPoint.getPSI() + 1);
                        psiScale *= getDNAAttribute(weightDNA, 5); //
                        if(psiScale <= 0){
                            psiScale = 0.001;
                            //System.out.println("psi " + fluidPoint.getPSI() + " " + Math.log10(fluidPoint.getPSI() + 1) );
                        }
                        zStepMove = (zStep * (1 - psiScale));
                        //points[v].z -= zStepMove; // ???? not all ar emoving
                        //System.out.println(" fluidPoint.getPSI(): " + psiScale );
                        
                        
                        if(pressureLeft > pressureRight){
                            //points[v].x += xStep * (pressureLeft-pressureRight);  // Push right from pressure on left side
                            xStepMove += xStep * (pressureLeft-pressureRight);      // Push right from pressure on left side
                            
                            
                            
                            //System.out.println("pressureLeft: " + pressureLeft  + " pressureRight " + pressureRight  + " -> " );
                        }
                        if(pressureRight > pressureLeft){
                            //points[v].x -= xStep * (pressureRight-pressureLeft);    // Push left from pressure on right side
                            xStepMove -= xStep * (pressureRight-pressureLeft);      // Push left from pressure on right side
                            
                            //System.out.println(" pressureRight " + pressureRight + "pressureLeft: " + pressureLeft + "  <- "   );
                        }
                        if(pressureLeft == pressureRight){
                            //System.out.print(" e ");
                            if(Math.random() > 0.5){
                                //points[v].x += xStep * (pressureLeft-pressureRight);
                            //    xStepMove += xStep * (pressureLeft-pressureRight);
                            } else {
                                //points[v].x -= xStep * (pressureLeft-pressureRight);
                            //    xStepMove -= xStep * (pressureLeft-pressureRight);
                            }
                            //cod += (pressureLeft);
                        }
                        
                        
                        if(pressureAbove > pressureBelow){
                            //points[v].y -= yStep * (pressureAbove-pressureBelow); // Push down from pressure on upper side
                            yStepMove -= yStep * (pressureAbove-pressureBelow); // Push down from pressure on upper side
                            //cod += (pressureAbove-pressureBelow);
                        }
                        if(pressureBelow > pressureAbove){
                            //points[v].y += yStep * (pressureBelow-pressureAbove); // Push left from pressure on right side
                            yStepMove += yStep * (pressureBelow-pressureAbove); // Push left from pressure on right side
                            //cod += (pressureBelow-pressureAbove);
                        }
                        
                        if(pressureBelow == pressureAbove){
                            //System.out.print(" e ");
                            if(Math.random() > 0.5){
                                //points[v].y -= yStep * (pressureAbove-pressureBelow);
                            //    yStepMove -= yStep * (pressureAbove-pressureBelow);
                            } else {
                                //points[v].y += yStep * (pressureBelow-pressureAbove);
                            //    yStepMove += yStep * (pressureBelow-pressureAbove);
                            }
                            //cod += (pressureBelow);
                        }
                        
                        // Todo: calculate z slowdown using trig based on distance traveled?
                        double zPressure = ((Math.abs(xStepMove) + Math.abs(yStepMove)) / 4);
                        zStepMove -= zPressure; // Slow down from pressure
                        
                        drag += (
                                 (Math.abs(xStepMove) * getDNAAttribute(weightDNA, 0)) +
                                 (Math.abs(yStepMove) * getDNAAttribute(weightDNA, 0)) +
                                 (zPressure * getDNAAttribute(weightDNA, 1))
                                ) * 1.5; //  A (drag X+Y), B (zPressure)
                        //  + Math.abs(zStepMove)
                        
                        //points[v].z -= 0.005;
                        //points[v].y += 0.0005; // TEST
                        //points[v].x += 0.0015; // TEST
                    } else {
                        //points[v].z = collideZ;
                        //System.out.print("c");
                    }
                    
                
                    // Low Pressure (Vacuum)
                    double move = 0.01; // 0.008;
                
                    //if( vacumeLeftDist != 1.0 || vacumeRightDist != 1.0 || vacumeBelowDist != 1.0 || vacumeAboveDist != 1.0){
                    if(vacumeLeft > 0.0 || vacumeRight > 0.0 || vacumeAbove > 0.0 || vacumeBelow > 0.0){
                        double vacumeDrag = 0;
                        
                        //System.out.println("vacumeLeftDist: " + vacumeLeftDist + "  vacumeRightDist " + vacumeRightDist );
                        
                        if( vacumeLeft > 0.0 && points[v].x > minx + (xSegmentWidth)){
                        //if( vacumeLeftDist > vacumeRightDist && points[v].x > minx){ // move left vacumeLeft > 0.0 &&
                            //points[v].x -= 0.010; // * (pressureRight-pressureLeft); // Push left from pressure on right side
                            double factor = lowPressureForce; //  1 + (vacumeLeftDist - vacumeRightDist);
                       //     factor = Math.sqrt(Math.sqrt(Math.abs(pressureRight-pressureLeft)));
                            // 1.6
                            //System.out.println("factor: " + factor + " r " + pressureRight + " l " + pressureLeft);
                            //factor = Math.sqrt(factor);
                            //System.out.println("  factor: " + factor);
                            xStepMove -= (move * factor);     // ??? xSegmentWidth
                            
                            vacumeDrag += (move * factor);
                            //System.out.println("factor: " + factor + " " + xSegmentWidth);
                            //System.out.println("vacume left: " );
                        }
                        if(vacumeRight > 0.0 && points[v].x < maxx - (xSegmentWidth)){
                        //if(vacumeRightDist > vacumeLeftDist && points[v].x < maxx){
                            //points[v].x += 0.010;
                            
                            double factor = lowPressureForce; //  1 + (vacumeRightDist - vacumeRightDist);
                        //    factor = Math.sqrt(Math.sqrt(Math.abs(pressureLeft-pressureRight)));
                            //factor = Math.sqrt(factor);
                            xStepMove += (move * factor);
                            
                            vacumeDrag += (move * factor);
                            //System.out.println("vacume right: " );
                        }
                        if(vacumeAbove > 0.0 && points[v].y < maxy - (ySegmentWidth)){
                        //if(vacumeBelowDist > vacumeAboveDist && points[v].y < maxy){ // vacume above and in bounds -> move up
                            //points[v].y += 0.010;
                            double factor = lowPressureForce; // 1 + (vacumeBelowDist - vacumeAboveDist);
                        //    factor = Math.sqrt(Math.sqrt(Math.abs(pressureBelow-pressureAbove)));
                            //factor = Math.sqrt(factor);
                            yStepMove += (move * factor);
                            
                            vacumeDrag += (move * factor);
                            //System.out.print("v +");
                            
                        }
                        //if(vacumeAboveDist > vacumeBelowDist && points[v].y > miny){ // vacume below and in bounds -> move down
                        if(vacumeBelow > 0.0 && points[v].y > miny + (ySegmentWidth)){ // vacume below and in bounds -> move down
                            //points[v].y -= 0.010;
                            double factor = lowPressureForce; // 1 + (vacumeAboveDist - vacumeBelowDist);
                        //    factor = Math.sqrt(Math.sqrt(Math.abs(pressureAbove-pressureBelow)));
                            //factor = Math.sqrt(factor);
                            yStepMove -= (move * factor);  // vacumeAboveDist
                            
                            vacumeDrag += (move * factor);
                            
                            //System.out.print(" v -");
                            
                        }
                        
                        //zStepSlow += (zStepMove / 3);
                        zStepMove -= (zStepMove / 3) * getDNAAttribute(weightDNA, 3); // Slow down to fill in vacume  // D (Z StepMove)
                         
                        drag += ((vacumeDrag) / 1.0) * getDNAAttribute(weightDNA, 2); // Vacume drag is worth less than pressure // C (vaccume)
                        
                        //fluidPoint.setPSI( 0.2 ); // render as blue
                    }
                
                    
                 
                
                
                    //
                    // Update drag coreffecient based on distance particles have to move divided by volume area.
                    //
                    codCalc += drag;
                    //codCalc += Math.abs(zStepSlow) + Math.abs(xStepMove) + Math.abs(yStepMove);
                
                    //
                    // Move fluid point based on calculated direction
                    //
                    points[v].z -= zStepMove;
                    points[v].x += xStepMove;
                    points[v].y += yStepMove;
                
                    // Bounds check
                    if(points[v].x < minx){
                        points[v].x = minx;
                    }
                    if(points[v].x > maxx){
                        points[v].x = maxx;
                    }
                    if(points[v].y < miny){
                        points[v].y = miny;
                    }
                    if(points[v].y > maxy){
                        points[v].y = maxy;
                    }
                
                    
                    // Reset fluid location
                    if(points[v].z < minz){
                        //fluidPoint.resetLocation();
                        //v = points.length;
                        
                        //points[v].x = fluidPoint.getResetLocation()[0].x;
                        //points[v].y = fluidPoint.getResetLocation()[0].y;
                        //points[v].z = maxz;
                        
                        ((LayoutWindow)window).removeObjectL(fluidPoint);
                        
                        for(int f = 0; f < pointObjects.size(); f++){
                            FluidPointObject fo = (FluidPointObject)pointObjects.elementAt(f);
                            if(fluidPoint == fo){
                                pointObjects.removeElementAt(f);
                                //f = pointObjects.size();
                            }
                        }
                        
                    }
                //}
                
                fluidPoint.setVertexPositions(points);
            } //     Point Objects
            
            // Respawn fluid point at beginning
            for(int x = 0; x < pointsPerLength; x++){
                for(int y = 0; y < pointsPerLength; y++){
                    
                    boolean spawn = true;
                    for(int i = 0; i < pointObjects.size(); i++){
                        FluidPointObject fluidPoint = pointObjects.elementAt(i);
                        Vec3 location = fluidPoint.getLocation();
                        
                        if(
                           location.x > (minx + (x * xSegmentWidth)) - (xSegmentWidth/2) &&
                           location.x < (minx + (x * xSegmentWidth)) + (xSegmentWidth/2) &&
                           location.y > (miny + (y * ySegmentWidth)) - (ySegmentWidth/2) &&
                           location.y < (miny + (y * ySegmentWidth)) + (ySegmentWidth/2) &&
                           location.z > (maxz - (zSegmentWidth))
                        ){
                            spawn = false;
                        }
                    }
                    
                    if(spawn){
                        //System.out.println("SPAWN");
                        Vec3[] vertex = new Vec3[3];
                        Vec3 vec = new Vec3(minx + (x * xSegmentWidth), miny + (y * ySegmentWidth), maxz);
                        vertex[0] = vec;
                        vertex[1] = vec;
                        vertex[2] = vec;
                        float s[];
                        s = new float[3];
                        s[0] = 0;
                        s[1] = 0;
                        s[2] = 0;
                        int smoothing = Mesh.APPROXIMATING;
                        CoordinateSystem coords;
                        
                        //Vec3[] resetLocation = new Vec3[3];
                        //Vec3 resetVec = new Vec3(minx + (x * xSegmentWidth), miny + (y * ySegmentWidth), maxz);
                        //resetLocation[0] = resetVec;
                        //resetLocation[1] = resetVec;
                        //resetLocation[2] = resetVec;
                        
                        FluidPointObject fluidPoint = new FluidPointObject(vertex, s, smoothing, false);
                        
                        //fluidPoint.setResetLocation(resetLocation);
                        
                        pointObjects.addElement(fluidPoint);
                        
                        //
                        ObjectInfo info = new ObjectInfo(fluidPoint, new CoordinateSystem(), "" /*no name*/);
                        //UndoRecord undo = new UndoRecord(window, false);
                        int id = ((LayoutWindow)window).addObjectL(fluidPoint); // This is bad. don't add this way later.
                        //fluidPoint.setId(id);
                        // This only reason we want it in the scene is to render we don't want it in the menu
                    }
                }
                window.repaint();
            }
            
            
            //System.out.println("coefficient of drag: " + cod + "   volume: " + volume + " = " + ( cod / volume ) );
            //cod = ( cod / volume );
            cod = (codCalc / (volume * getDNAAttribute(weightDNA, 4)));   // E (Volume)
            
            // Draw --- ViewerCanvas  renderCFDResults( Camera theCamera )
            // Scene -> ViewerCanvas
            //Scene scene = window.getScene();
            
            //ViewerCanvas canvas = window.getView();
            //Camera theCamera = canvas.getCamera();
            //CanvasDrawer drawer = canvas.getCanvasDrawer();
            //drawer.renderCFDResults( theCamera );  // not the right time to draw
            //((SceneViewer)canvas).setCFD(this);
            
            // SceneViewer setScreenText
            
            
            // Refresh screen
            window.repaint();
            
            //System.out.print(".");
            
            //try {
            //    Thread.sleep(2);
            //} catch (Exception e){  }
        //}
    }
    
    /**
     * drawText
     *
     * Description: Draw CFD results to the screen. Called by SceneViewer.updateImage().
     */
    public void drawText(){
        
        if(running == false && textCleared == false){
            clearText();
            textCleared = true;
            return;
        }
        
        ViewerCanvas canvas = window.getView();
        Camera theCamera = canvas.getCamera();
        CanvasDrawer drawer = canvas.getCanvasDrawer();
        Vector lines = new Vector();
        
        String codeString = ""+cod;
        if(codeString.length() > 5){
            codeString = codeString.substring(0, 5);
        }
        
        lines.addElement("COD: " + codeString);
        //
        drawer.renderCFDResults(theCamera, lines);
        
        // Draw lift value
        // TODO:
    }
    
    public void clearText(){
        //System.out.println("clear");
        ViewerCanvas canvas = window.getView();
        Camera theCamera = canvas.getCamera();
        CanvasDrawer drawer = canvas.getCanvasDrawer();
        Vector lines = new Vector();
        drawer.renderCFDResults(theCamera, lines);
    }
    
    /**
     * calculateBounds
     *
     * Description: calculate region to simulate flow to be a
     *  relative size larger than the bounds of scene objects.
     */
    public void calculateBounds(Vector<ObjectInfo> objects){
        LayoutModeling layout = new LayoutModeling();
        // Calculate bounds
        for (ObjectInfo obj : objects){
            if(obj.getName().indexOf("Camera") < 0 &&
               obj.getName().indexOf("Light") < 0 &&
               obj.isVisible() &&
               obj.isHiddenByParent() == false
               ){ //obj.selected == true  || selection == false
                //System.out.println("Object Info: ");
                Object3D co = (Object3D)obj.getObject();
                //System.out.println("obj " + obj.getId() + "  " + obj.getName() );
                
                // obj.getObject(); // Object3D
                Object3D o3d = obj.getObject();
                BoundingBox bounds = o3d.getBounds();
                
                // Include object location in bounds values.
                CoordinateSystem c;
                c = layout.getCoords(obj);
                Vec3 objOrigin = c.getOrigin();
                bounds.minx += objOrigin.x; bounds.maxx += objOrigin.x;
                bounds.miny += objOrigin.y; bounds.maxy += objOrigin.y;
                bounds.minz += objOrigin.z; bounds.maxz += objOrigin.z;
                
                
                //System.out.println("  " + bounds.minx + " " + bounds.maxx );
                if(bounds.minx < this.minx){
                    this.minx = bounds.minx;
                }
                if(bounds.maxx > this.maxx){
                    this.maxx = bounds.maxx;
                }
                if(bounds.miny < this.miny){
                    this.miny = bounds.miny;
                }
                if(bounds.maxy > this.maxy){
                    this.maxy = bounds.maxy;
                }
                if(bounds.minz < this.minz){
                    this.minz = bounds.minz;
                }
                if(bounds.maxz > this.maxz){
                    this.maxz = bounds.maxz;
                }
            }
        }
    }
    
    
    /**
     * sceneMeshVolume
     *
     * Description: Calculate mesh volume.
     *  Break bounds into cubes and check from edges inward until it collides with a polygon bounding box.
     */
    public double sceneMeshVolume(){ // ObjectInfo obj TriangleMesh triangleMesh
        double volume = 0;
        LayoutModeling layout = new LayoutModeling();
        BoundingBox bounds = new BoundingBox(99999, -99999, 99999, -99999, 99999, -99999);
        bounds.minx = 99999;
        bounds.maxx = -99999;
        bounds.miny = 99999;
        bounds.maxy = -99999;
        bounds.minz = 99999;
        bounds.maxz = -99999;
        
        //Object3D o3d = obj.getObject();
        //TriangleMesh triangleMesh = null;
        //triangleMesh = obj.getObject().convertToTriangleMesh(0.05);
        
        //Object3D o3d = triangleMesh.getObject();
        //BoundingBox bounds = triangleMesh.getBounds();
        //System.out.println(" bounds " + bounds.minx);
        // ??? add location???
        //bounds.minx += objOrigin.x; bounds.maxx += objOrigin.x;
        //bounds.miny += objOrigin.y; bounds.maxy += objOrigin.y;
        //bounds.minz += objOrigin.z; bounds.maxz += objOrigin.z;
        
        int segments = 12;
        
        //MeshVertex[] verts = triangleMesh.getVertices();
        //TriangleMesh.Edge[] edges = ((TriangleMesh)triangleMesh).getEdges();
        //TriangleMesh.Face[] faces = triangleMesh.getFaces();
        
        // find objects bounds and collect all object face geometry
        Vector sceneTriangles = new Vector();
        for (ObjectInfo obj : objects){
            if(obj.getName().indexOf("Camera") < 0 &&
               obj.getName().indexOf("Light") < 0 &&
               obj.getName().equals("") == false &&
               obj.isVisible()
               ){
                if(obj.getObject().canConvertToTriangleMesh() != Object3D.CANT_CONVERT){
                    CoordinateSystem c;
                    c = layout.getCoords(obj);
                    //Vec3 objOrigin = c.getOrigin();
                    TriangleMesh triangleMesh = null;
                    triangleMesh = obj.getObject().convertToTriangleMesh(0.05); //
                    
                    System.out.println("c" );
                    
                    // scene bounds by verts
                    MeshVertex[] verts = triangleMesh.getVertices();
                    for(int f = 0; f < verts.length; f++){
                        Vec3 vert = new Vec3(verts[f].r);
                        Mat4 mat4 = c.duplicate().fromLocal();
                        mat4.transform(vert);
                        if(vert.x < bounds.minx){ bounds.minx = vert.x; }
                        if(vert.x > bounds.maxx){ bounds.maxx = vert.x; }
                        if(vert.y < bounds.miny){ bounds.miny = vert.y; }
                        if(vert.y > bounds.maxy){ bounds.maxy = vert.y; }
                        if(vert.z < bounds.minz){ bounds.minz = vert.z; }
                        if(vert.z > bounds.maxz){ bounds.maxz = vert.z; }
                    }
                    
                    // faces
                    TriangleMesh.Face[] faces = triangleMesh.getFaces();
                    for(int f = 0; f < faces.length; f++){
                        TriangleMesh.Face face = faces[f];
                        Vec3 vec1 = new Vec3(verts[face.v1].r); // duplicate
                        Vec3 vec2 = new Vec3(verts[face.v2].r);
                        Vec3 vec3 = new Vec3(verts[face.v3].r);
                        Mat4 mat4 = c.duplicate().fromLocal();
                        mat4.transform(vec1);
                        mat4.transform(vec2);
                        mat4.transform(vec3);
                        Vec3[] triangle = new Vec3[3];
                        triangle[0] = vec1;
                        triangle[1] = vec2;
                        triangle[2] = vec3;
                        sceneTriangles.addElement(triangle);
                        
                        //System.out.println(" vec1  " + vec1.x  + " y " + vec1.y + " z " + vec1.z   );
                        
                        // TODO: optimization, put bounding boxes in array instead of triangles
                    }
                }
            }
        }
        System.out.println(" bounds  x " + bounds.minx + " " + bounds.maxx + " y " + bounds.miny + " " + bounds.maxy + " z " + bounds.minz + "  "+ bounds.maxz  );
        volume = ( bounds.maxx - bounds.minx ) * ( bounds.maxy - bounds.miny ) * ( bounds.maxz - bounds.minz );
        
        System.out.println(" volume: " + volume  );
        
        double xSegmentWidth = (bounds.maxx - bounds.minx) / segments;
        double ySegmentWidth = (bounds.maxy - bounds.miny) / segments;
        double zSegmentWidth = (bounds.maxz - bounds.minz) / segments;
        
        double cubeVolume = xSegmentWidth * ySegmentWidth * zSegmentWidth;
        
        double cubesOccupied[][][] = new double[segments][segments][segments];
        
        // init cubes occupied state
        for(int z = 0; z < segments; z++){ //
            for(int x = 0; x < segments; x++){
                for(int y = 0; y < segments; y++){
                    cubesOccupied[x][y][z] = 1;
                }
            }
        }
        
        // Top down pass
        for(int x = 0; x < segments; x++){
            for(int z = 0; z < segments; z++){
                for(int y = segments - 1; y >= 0; y--){
                    BoundingBox cubeBounds = new BoundingBox(bounds);
                    cubeBounds.minx = bounds.minx + (x * xSegmentWidth);
                    cubeBounds.maxx = bounds.minx + ((x+1) * xSegmentWidth);
                    cubeBounds.minz = bounds.minz + (z * zSegmentWidth);
                    cubeBounds.maxz = bounds.minz + ((z+1) * zSegmentWidth);
                    cubeBounds.miny = bounds.miny + (y * zSegmentWidth);
                    cubeBounds.maxy = bounds.miny + ((y + 1) * zSegmentWidth); // ???
                    boolean occupied = false;
                    
                    //System.out.println("     z: " + z + " x " + x + " y: " + y +
                    //                   "   cube x " + cubeBounds.minx + "-" + cubeBounds.maxx  +
                    //                   " y " + cubeBounds.miny +  " "+cubeBounds.maxy + " z " +  cubeBounds.minz + " " + cubeBounds.maxz  );
                    
                    for(int f = 0; f < sceneTriangles.size(); f++){
                        Vec3[] triangle = (Vec3[])sceneTriangles.elementAt(f);
                        Vec3 vec1 = triangle[0];
                        Vec3 vec2 = triangle[1];
                        Vec3 vec3 = triangle[2];
                        
                        //System.out.println(" vec1  " + vec1.x  + " y " + vec1.y + " z " + vec1.z  + "   cube x" + cubeBounds.minx + " " + cubeBounds.maxx );
                        
                        BoundingBox faceBounds = new BoundingBox(bounds);
                        faceBounds.minx = Math.min(Math.min(vec1.x, vec2.x), vec3.x);
                        faceBounds.maxx = Math.max(Math.max(vec1.x, vec2.x), vec3.x);
                        faceBounds.miny = Math.min(Math.min(vec1.y, vec2.y), vec3.y);
                        faceBounds.maxy = Math.max(Math.max(vec1.y, vec2.y), vec3.y);
                        faceBounds.minz = Math.min(Math.min(vec1.z, vec2.z), vec3.z);
                        faceBounds.maxz = Math.max(Math.max(vec1.z, vec2.z), vec3.z);
                        //if(cubeBounds.maxx >= faceBounds.minx && cubeBounds.minx <= faceBounds.maxx &&
                        //   cubeBounds.maxy >= faceBounds.miny && cubeBounds.miny <= faceBounds.maxy &&
                        //   cubeBounds.maxz >= faceBounds.minz && cubeBounds.minz <= faceBounds.maxz
                        //   ){
                        
                        if(
                               (vec1.x >= cubeBounds.minx && vec1.x <= cubeBounds.maxx &&
                                vec1.y >= cubeBounds.miny && vec1.y <= cubeBounds.maxy &&
                                vec1.z >= cubeBounds.minz && vec1.z <= cubeBounds.maxz)
                           ||
                               (vec2.x >= cubeBounds.minx && vec2.x <= cubeBounds.maxx &&
                                vec2.y >= cubeBounds.miny && vec2.y <= cubeBounds.maxy &&
                                vec2.z >= cubeBounds.minz && vec2.z <= cubeBounds.maxz)
                           ||
                               (vec3.x >= cubeBounds.minx && vec3.x <= cubeBounds.maxx &&
                                vec3.y >= cubeBounds.miny && vec3.y <= cubeBounds.maxy &&
                                vec3.z >= cubeBounds.minz && vec3.z <= cubeBounds.maxz)
                           ){ // face vertecy in cube
                            
                            //System.out.println("+     x: " + x + " y: " + y + " z: " + z + " "  );
                            occupied = true;
                        }
                        
                        
                        
                        //boolean inXPlane_1 = vec1.z >= cubeBounds.minz && vec1.z <= cubeBounds.maxz && vec1.y >= cubeBounds.miny && vec1.y <= cubeBounds.maxy;
                        
                        boolean inX =
                        (faceBounds.minx >= cubeBounds.minx || faceBounds.maxx >= cubeBounds.minx) &&   // a face x point is > cube min
                        (faceBounds.minx <= cubeBounds.maxx || faceBounds.maxx <= cubeBounds.maxx);     // a face x point in < cube max
                        boolean inY =
                        (faceBounds.miny >= cubeBounds.miny || faceBounds.maxy >= cubeBounds.miny) &&
                        (faceBounds.miny <= cubeBounds.maxy || faceBounds.maxy <= cubeBounds.maxy);
                        boolean inZ =
                        (faceBounds.minz >= cubeBounds.minz || faceBounds.maxz >= cubeBounds.minz) &&
                        (faceBounds.minz <= cubeBounds.maxz || faceBounds.maxz <= cubeBounds.maxz);
                        
                        boolean inXPlane = inZ && inY;
                        boolean inYPlane = inZ && inX;
                        boolean inZPlane = inY && inX;
                    
                        boolean spanXPlane = (faceBounds.minx <= cubeBounds.minx && faceBounds.maxx >= cubeBounds.maxx);
                        boolean spanYPlane = (faceBounds.miny <= cubeBounds.miny && faceBounds.maxy >= cubeBounds.maxy);
                        boolean spanZPlane = (faceBounds.minz <= cubeBounds.minz && faceBounds.maxz >= cubeBounds.maxz);
                        
                        //System.out.println(" - "+  inXPlane + " , " + inYPlane + " , " + inZPlane + " span " + spanXPlane + " , " + spanYPlane + " , " + spanZPlane);
                        
                        if(inX && inY && inZ){
                            occupied = true;
                        }
                        if(( inXPlane && spanXPlane) || (inYPlane && spanYPlane) || (inZPlane && spanZPlane)){
                            //System.out.println( "    ****** " );
                            occupied = true;
                        }
                        if((spanXPlane && spanYPlane && inZ) || (spanXPlane && spanZPlane && inY) || (spanYPlane && spanZPlane && inX)){
                            //System.out.println( "    XXXXXX " );
                            occupied = true;
                        }
                        if((spanXPlane && spanYPlane && spanZPlane) || (spanXPlane && spanZPlane && spanYPlane) || (spanYPlane && spanZPlane && spanXPlane)){
                            //System.out.println( "    ZZZZZZ " );
                            occupied = true;
                        }
                        
                        if((inX && inY && spanZPlane) || (inX && inZ && spanYPlane) || (inY && inZ && spanXPlane)){
                            //System.out.println( "    ...... " );
                            occupied = true;
                        }
                        
                    }
                    if(!occupied){
                        //System.out.print( "    x: " + x + " z: " + z + " y: " + y   + "   " + occupied );
                        
                        //System.out.println( "            cube: x: " + cubeBounds.minx + " " + cubeBounds.maxx +
                        //                   " y: " + cubeBounds.miny + " " + cubeBounds.maxy +
                        //                   " z: " + cubeBounds.minz  + " " + cubeBounds.maxz );
                        //System.out.println(  );
                        
                        
                        for(int f = 0; f < sceneTriangles.size(); f++){
                            Vec3[] triangle = (Vec3[])sceneTriangles.elementAt(f);
                            Vec3 vec1 = triangle[0];
                            Vec3 vec2 = triangle[1];
                            Vec3 vec3 = triangle[2];
                            
                            //System.out.println(" vec1  " + vec1.x  + " y " + vec1.y + " z " + vec1.z  + "   cube x" + cubeBounds.minx + " " + cubeBounds.maxx );
                            
                            BoundingBox faceBounds = new BoundingBox(bounds);
                            faceBounds.minx = Math.min(Math.min(vec1.x, vec2.x), vec3.x);
                            faceBounds.maxx = Math.max(Math.max(vec1.x, vec2.x), vec3.x);
                            faceBounds.miny = Math.min(Math.min(vec1.y, vec2.y), vec3.y);
                            faceBounds.maxy = Math.max(Math.max(vec1.y, vec2.y), vec3.y);
                            faceBounds.minz = Math.min(Math.min(vec1.z, vec2.z), vec3.z);
                            faceBounds.maxz = Math.max(Math.max(vec1.z, vec2.z), vec3.z);
                            
                            //System.out.println( "            FACE: x: " + faceBounds.minx + " " + faceBounds.maxx +
                            //                   " y: " + faceBounds.miny + " " + faceBounds.maxy +
                            //                   " z: " + faceBounds.minz  + " " + faceBounds.maxz );
                        }
                        
                    }
                    //System.out.println("     z: " + z + " x " + x + " y: " + y +
                    //                   "   cube x " + cubeBounds.minx + "-" + cubeBounds.maxx  +
                    //
                    
                    /*
                    for(int f = 0; f < faces.length; f++){
                        TriangleMesh.Face face = faces[f];
                        Vec3 vec1 = new Vec3(verts[face.v1].r); // duplicate
                        Vec3 vec2 = new Vec3(verts[face.v2].r);
                        Vec3 vec3 = new Vec3(verts[face.v3].r);
                        
                        Mat4 mat4 = c.duplicate().fromLocal();
                        mat4.transform(vec1);
                        mat4.transform(vec2);
                        mat4.transform(vec3);
                        
                        BoundingBox faceBounds = new BoundingBox(bounds);
                        faceBounds.minx = Math.min(Math.min(vec1.x, vec2.x), vec3.x);
                        faceBounds.maxx = Math.max(Math.max(vec1.x, vec2.x), vec3.x);
                        faceBounds.miny = Math.min(Math.min(vec1.y, vec2.y), vec3.y);
                        faceBounds.maxy = Math.max(Math.max(vec1.y, vec2.y), vec3.y);
                        faceBounds.minz = Math.min(Math.min(vec1.z, vec2.z), vec3.z);
                        faceBounds.maxz = Math.max(Math.max(vec1.z, vec2.z), vec3.z);
                        if(cubeBounds.maxx > faceBounds.minx && cubeBounds.minx < faceBounds.maxx &&
                           cubeBounds.maxy > faceBounds.miny && cubeBounds.miny < faceBounds.maxy &&
                           cubeBounds.maxz > faceBounds.minz && cubeBounds.minz < faceBounds.maxz
                           ){
                            //System.out.print(".");
                            //occupied = true;
                        }
                    }
                     */
                    
                    /*
                    for(int f = 0; f < verts.length; f++){
                        Vec3 location = new Vec3(verts[f].r);
                        Mat4 mat4 = c.duplicate().fromLocal();
                        mat4.transform(location);
                        if(location.x > cubeBounds.minx && location.x < cubeBounds.maxx &&
                           location.y > cubeBounds.miny && location.y < cubeBounds.maxy &&
                           location.z > cubeBounds.minz && location.z < cubeBounds.maxz){
                            occupied = true;
                        }
                    }
                     */
                    if(occupied){
                        y = -1; // break downward pass
                    }
                    if(occupied == false){
                        cubesOccupied[x][y][z] = 0;
                    }
                }
            }
        }
        
        // Bottom up pass
        for(int z = 0; z < segments; z++){
            for(int x = 0; x < segments; x++){
                for(int y = 0; y < segments; y++){
                    BoundingBox cubeBounds = new BoundingBox(bounds);
                    cubeBounds.minx = bounds.minx + (x * xSegmentWidth);
                    cubeBounds.maxx = bounds.minx + ((x+1) * xSegmentWidth);
                    cubeBounds.minz = bounds.minz + (z * zSegmentWidth);
                    cubeBounds.maxz = bounds.minz + ((z+1) * zSegmentWidth);
                    cubeBounds.miny = bounds.miny + (y * zSegmentWidth);
                    cubeBounds.maxy = bounds.maxy + ((y + 1) * zSegmentWidth);
                    boolean occupied = false;
                    
                    for(int f = 0; f < sceneTriangles.size(); f++){
                        Vec3[] triangle = (Vec3[])sceneTriangles.elementAt(f);
                        Vec3 vec1 = triangle[0];
                        Vec3 vec2 = triangle[1];
                        Vec3 vec3 = triangle[2];
                        
                        BoundingBox faceBounds = new BoundingBox(bounds);
                        faceBounds.minx = Math.min(Math.min(vec1.x, vec2.x), vec3.x);
                        faceBounds.maxx = Math.max(Math.max(vec1.x, vec2.x), vec3.x);
                        faceBounds.miny = Math.min(Math.min(vec1.y, vec2.y), vec3.y);
                        faceBounds.maxy = Math.max(Math.max(vec1.y, vec2.y), vec3.y);
                        faceBounds.minz = Math.min(Math.min(vec1.z, vec2.z), vec3.z);
                        faceBounds.maxz = Math.max(Math.max(vec1.z, vec2.z), vec3.z);
                        if(cubeBounds.maxx > faceBounds.minx && cubeBounds.minx < faceBounds.maxx &&
                           cubeBounds.maxy > faceBounds.miny && cubeBounds.miny < faceBounds.maxy &&
                           cubeBounds.maxz > faceBounds.minz && cubeBounds.minz < faceBounds.maxz
                           ){
                            //System.out.print(".");
                            //occupied = true;
                        }
                    }
                    
                    /*
                    for(int f = 0; f < faces.length; f++){
                        TriangleMesh.Face face = faces[f];
                        Vec3 vec1 = new Vec3(verts[face.v1].r); // duplicate
                        Vec3 vec2 = new Vec3(verts[face.v2].r);
                        Vec3 vec3 = new Vec3(verts[face.v3].r);
                        
                        Mat4 mat4 = c.duplicate().fromLocal();
                        mat4.transform(vec1);
                        mat4.transform(vec2);
                        mat4.transform(vec3);
                        
                        BoundingBox faceBounds = new BoundingBox(bounds);
                        faceBounds.minx = Math.min(Math.min(vec1.x, vec2.x), vec3.x);
                        faceBounds.maxx = Math.max(Math.max(vec1.x, vec2.x), vec3.x);
                        faceBounds.miny = Math.min(Math.min(vec1.y, vec2.y), vec3.y);
                        faceBounds.maxy = Math.max(Math.max(vec1.y, vec2.y), vec3.y);
                        faceBounds.minz = Math.min(Math.min(vec1.z, vec2.z), vec3.z);
                        faceBounds.maxz = Math.max(Math.max(vec1.z, vec2.z), vec3.z);
                        if(cubeBounds.maxx > faceBounds.minx && cubeBounds.minx < faceBounds.maxx &&
                           cubeBounds.maxy > faceBounds.miny && cubeBounds.miny < faceBounds.maxy &&
                           cubeBounds.maxz > faceBounds.minz && cubeBounds.minz < faceBounds.maxz
                           ){
                            //System.out.print(".");
                            //occupied = true;
                        }
                    }
                     */
                    /*
                    for(int f = 0; f < verts.length; f++){
                        Vec3 location = new Vec3(verts[f].r);
                        Mat4 mat4 = c.duplicate().fromLocal();
                        mat4.transform(location);
                        if(location.x > cubeBounds.minx && location.x < cubeBounds.maxx &&
                           location.y > cubeBounds.miny && location.y < cubeBounds.maxy &&
                           location.z > cubeBounds.minz && location.z < cubeBounds.maxz){
                            occupied = true;
                        }
                    }
                     */
                    /*
                    if(occupied){
                        y = segments; // break upward pass
                    } else {
                        cubesOccupied[x][y][z] = 0;
                    }
                     */
                }
            }
        }
        
        
        System.out.println(" vol 1 " + volume  );
        
        // Subtract unocupied cubes from volume
        for(int z = 0; z < segments; z++){ //
            for(int x = 0; x < segments; x++){
                for(int y = 0; y < segments; y++){
                    if(cubesOccupied[x][y][z] == 0) {
                        //System.out.println(" vol " + volume + " cube " + cubeVolume );
                        volume -= cubeVolume;
                    }
                }
            }
        }
        
        System.out.println(" vol 2 " + volume  );
        
        // window.
        
        return volume;
    }
    
    /**
     * Frontal Area
     *
     * Description: Calculate scene object frontal area.
     *  TODO: use vector collisions.
     */
    public double frontalArea(){
        double area = 0;
        double volume = 0;
        LayoutModeling layout = new LayoutModeling();
        BoundingBox bounds = new BoundingBox(99999, -99999, 99999, -99999, 99999, -99999);
        bounds.minx = 99999;
        bounds.maxx = -99999;
        bounds.miny = 99999;
        bounds.maxy = -99999;
        bounds.minz = 99999;
        bounds.maxz = -99999;
        
        int segments = 50;
        
        // find objects bounds and collect all object face geometry
        Vector sceneTriangles = new Vector();
        for (ObjectInfo obj : objects){
            if(obj.getName().indexOf("Camera") < 0 &&
               obj.getName().indexOf("Light") < 0 &&
               obj.getName().equals("") == false &&
               obj.isVisible()
               ){
                if(obj.getObject().canConvertToTriangleMesh() != Object3D.CANT_CONVERT){
                    CoordinateSystem c;
                    c = layout.getCoords(obj);
                    //Vec3 objOrigin = c.getOrigin();
                    TriangleMesh triangleMesh = null;
                    triangleMesh = obj.getObject().convertToTriangleMesh(0.05);
                    
                    // scene bounds by verts
                    MeshVertex[] verts = triangleMesh.getVertices();
                    for(int f = 0; f < verts.length; f++){
                        Vec3 vert = new Vec3(verts[f].r);
                        Mat4 mat4 = c.duplicate().fromLocal();
                        mat4.transform(vert);
                        if(vert.x < bounds.minx){ bounds.minx = vert.x; }
                        if(vert.x > bounds.maxx){ bounds.maxx = vert.x; }
                        if(vert.y < bounds.miny){ bounds.miny = vert.y; }
                        if(vert.y > bounds.maxy){ bounds.maxy = vert.y; }
                        if(vert.z < bounds.minz){ bounds.minz = vert.z; }
                        if(vert.z > bounds.maxz){ bounds.maxz = vert.z; }
                    }
                    
                    // faces
                    TriangleMesh.Face[] faces = triangleMesh.getFaces();
                    for(int f = 0; f < faces.length; f++){
                        TriangleMesh.Face face = faces[f];
                        Vec3 vec1 = new Vec3(verts[face.v1].r); // duplicate
                        Vec3 vec2 = new Vec3(verts[face.v2].r);
                        Vec3 vec3 = new Vec3(verts[face.v3].r);
                        Mat4 mat4 = c.duplicate().fromLocal();
                        mat4.transform(vec1);
                        mat4.transform(vec2);
                        mat4.transform(vec3);
                        Vec3[] triangle = new Vec3[3];
                        triangle[0] = vec1;
                        triangle[1] = vec2;
                        triangle[2] = vec3;
                        sceneTriangles.addElement(triangle);
                        
                        //System.out.println(" vec1  " + vec1.x  + " y " + vec1.y + " z " + vec1.z   );
                        
                        // TODO: optimization, put bounding boxes in array instead of triangles
                    }
                }
            }
        }
        System.out.println(" bounds  x " + bounds.minx + " " + bounds.maxx + " y " + bounds.miny + " " + bounds.maxy + " z " + bounds.minz + "  "+ bounds.maxz  );
        
        volume = ( bounds.maxx - bounds.minx ) * ( bounds.maxy - bounds.miny ) * ( bounds.maxz - bounds.minz );
        System.out.println(" volume: " + volume  );
        
        double xSegmentWidth = (bounds.maxx - bounds.minx) / segments;
        double ySegmentWidth = (bounds.maxy - bounds.miny) / segments;
        double zSegmentWidth = (bounds.maxz - bounds.minz) / segments;
        System.out.println("xSegmentWidth: " + xSegmentWidth + " ySegmentWidth: " + ySegmentWidth + " zSegmentWidth: " + zSegmentWidth);
        
        double cubeVolume = xSegmentWidth * ySegmentWidth * zSegmentWidth;
        
        double cubesOccupied[][][] = new double[segments][segments][segments];
        
        // init cubes occupied state
        for(int z = 0; z < segments; z++){ //
            for(int x = 0; x < segments; x++){
                for(int y = 0; y < segments; y++){
                    cubesOccupied[x][y][z] = 1;
                }
            }
        }
        
        double frontalAreaGrid[][] = new double[segments][segments];
        for(int x = 0; x < segments; x++){
            for(int y = 0; y < segments; y++){
                frontalAreaGrid[x][y] = 1;
            }
        }
        
        
        // Frontal pass (Depricate this method of calculation.)
        int frontalAreaOccupiedGridCount = 0;
        for(int x = 0; x < segments; x++){
            for(int y = 0; y < segments; y++){
                boolean frontalRegionOccupied = false;
                
                double depth = 0;
                
                for (ObjectInfo obj : objects){
                    if(obj.getName().indexOf("Camera") < 0 &&
                       obj.getName().indexOf("Light") < 0 &&
                       obj.getName().equals("") == false &&
                       obj.isVisible()
                       
                       ){
                        
                        CoordinateSystem c;
                        c = layout.getCoords(obj);
                        Vec3 objOrigin = c.getOrigin();
                
                        BoundingBox cubeBounds = new BoundingBox(bounds);
                        cubeBounds.minx = bounds.minx + (x * xSegmentWidth);
                        cubeBounds.maxx = bounds.minx + ((x+1) * xSegmentWidth);
                        //cubeBounds.minz = bounds.minz + (z * zSegmentWidth);
                        //cubeBounds.maxz = bounds.minz + ((z+1) * zSegmentWidth);
                        cubeBounds.miny = bounds.miny + (y * ySegmentWidth);
                        cubeBounds.maxy = bounds.miny + ((y + 1) * ySegmentWidth); // ???
                        
                        
                        double x_loc = bounds.minx + (x * xSegmentWidth);
                        double y_loc = bounds.miny + (y * ySegmentWidth);
                        Vec3 point_loc = new Vec3(x_loc, y_loc, 0);
                        
                        
                        TriangleMesh triangleMesh = null;
                        triangleMesh = obj.getObject().convertToTriangleMesh(0.05);
                        //triangleMesh = ((TriangleMesh)obj.getObject()).duplicate()  .convertToTriangleMesh(0.05);
                        MeshVertex[] verts = triangleMesh.getVertices();
                        TriangleMesh.Edge[] edges = ((TriangleMesh)triangleMesh).getEdges();
                        
                        TriangleMesh.Face[] faces = triangleMesh.getFaces();
                        
                        //System.out.println("faces: " + faces.length);
                        for(int f = 0; f < faces.length; f++){ //  && running
                            TriangleMesh.Face face = faces[f];
                            Vec3 vec1 = new Vec3(verts[face.v1].r); // duplicate
                            Vec3 vec2 = new Vec3(verts[face.v2].r);
                            Vec3 vec3 = new Vec3(verts[face.v3].r);
                            
                            Mat4 mat4 = c.duplicate().fromLocal();
                            mat4.transform(vec1);
                            mat4.transform(vec2);
                            mat4.transform(vec3);
                            
                            if(inside_frontal_trigon(point_loc, vec1, vec2, vec3)){
                                
                                double currDepth = trigon_depth(point_loc, vec1, vec2, vec3);
                                if(currDepth > depth){
                                    depth = currDepth;
                                }
                                //System.out.println("x " + x +  " y " + y + "  depth: " + depth);
                                frontalRegionOccupied = true;
                                
                                
                            } else {
                                //System.out.println("x " + x +  " y " + y + "  outside " );
                            }
                        }
                                
                        
                    
                    } //
                }
                if(frontalRegionOccupied){
                    frontalAreaOccupiedGridCount++;
                    //System.out.println("x " + x +  " y " + y + "depth: " + depth );
                } else {
                    //System.out.println("x " + x +  " y " + y + " void. "  );
                }
                
            }
        }
        System.out.println("frontalAreaOccupiedGridCount: " + frontalAreaOccupiedGridCount );
        int total = segments * segments;
        area = (xSegmentWidth * ySegmentWidth) * frontalAreaOccupiedGridCount ;
        System.out.println("total cells: "+ total + " occupied cells: " + frontalAreaOccupiedGridCount + " area: " + area);
        
        // Notify dialog.
        JOptionPane.showMessageDialog(null, "Frontal Area: " + area,  "Frontal Area" , JOptionPane.ERROR_MESSAGE );
        
        return area;
    }
    
    
    /**
           // Calculate actual surface of polygons rather than boundary. More accurate but computationally expensive.
     
     TriangleMesh triangleMesh = null;
     triangleMesh = obj.getObject().convertToTriangleMesh(0.05);
     TriangleMesh.Face[] faces = triangleMesh.getFaces();
     
        for(int f = 0; f < faces.length; f++){ //  && running
        TriangleMesh.Face face = faces[f];
        Vec3 vec1 = new Vec3(verts[face.v1].r); // duplicate
        Vec3 vec2 = new Vec3(verts[face.v2].r);
        Vec3 vec3 = new Vec3(verts[face.v3].r);
        
        Mat4 mat4 = c.duplicate().fromLocal();
        mat4.transform(vec1);
        mat4.transform(vec2);
        mat4.transform(vec3);
     
        if(inside_trigon(point_loc, vec1, vec2, vec3)){
                //double currHeight = Math.max(Math.max(vec1.y, vec2.y), vec3.y);  // TODO get actual height
                double currHeight = trigon_height(point_loc, vec1, vec2, vec3);
                if(currHeight > height){
                    height = currHeight;
                }
        }
     }
     */
    
    
    /**
     * trigon_height
     *
     * Description: calculate height on surface of polygon a,b,c given point x,z (s).
     *
     */
    double trigon_height(Vec3 s, Vec3 a, Vec3 b, Vec3 c){
        double height = -10;
        Vec3 planeNormal = calcNormal(a, b, c);
        Vec3 intersect = intersectPoint(new Vec3(0,1,0), s, planeNormal, a);
        height = intersect.y;
        return height;
    }
    
    
    double trigon_depth(Vec3 s, Vec3 a, Vec3 b, Vec3 c){
        double depth = -100;
        
        Vec3 planeNormal = calcNormal(a, b, c);
        Vec3 intersect = intersectPoint(new Vec3(0,0,1), s, planeNormal, a);
        depth = intersect.z;
        
        return depth;
    }
    
    /**
     * calcNormal
     *
     * Description: Calculate the normal vector for a three point face.
     */
    private Vec3 calcNormal(Vec3 v0, Vec3 v1, Vec3 v2) {
        Vec3 s1 = new Vec3( v1.x - v0.x, v1.y - v0.y, v1.z - v0.z ); // subtract
        Vec3 s2 = new Vec3( v2.x - v0.x, v2.y - v0.y, v2.z - v0.z ); // subtract
        Vec3 nv = new Vec3(s1.y * s2.z - s1.z*s2.y,
                           s1.z*s2.x - s1.x*s2.z,
                           s1.x*s2.y - s1.y*s2.x); // cross product
        float length = (float) Math.sqrt(nv.x * nv.x + nv.y * nv.y + nv.z * nv.z);
        nv.x /= length;
        nv.y /= length;
        nv.z /= length;
        return nv;
    }
    
    
    /**
     * intersectPoint
     *
     * Description:
     */
    private static Vec3 intersectPoint(Vec3 rayVector, Vec3 rayPoint, Vec3 planeNormal, Vec3 planePoint) {
        //Vec3D diff = rayPoint.minus(planePoint);
        // new Vector3D(x - v.x, y - v.y, z - v.z);
        Vec3 diff = new Vec3(rayPoint.x - planePoint.x,  rayPoint.y - planePoint.y, rayPoint.z - planePoint.z);
        //double prod1 = diff.dot(planeNormal);
        double prod1 = diff.x * planeNormal.x + diff.y * planeNormal.y + diff.z * planeNormal.z;  //  x * v.x + y * v.y + z * v.z;
        //double prod2 = rayVector.dot(planeNormal);
        double prod2 = rayVector.x * planeNormal.x + rayVector.y * planeNormal.y + rayVector.z * planeNormal.z;
        double prod3 = prod1 / prod2;
        //return rayPoint.minus(rayVector.times(prod3));
        Vec3 t = new Vec3(rayVector.x * prod3, rayVector.y * prod3, rayVector.z * prod3);
        return new Vec3( rayPoint.x - t.x, rayPoint.y - t.y, rayPoint.z - t.z );
    }
    
    /**
     * inside_trigon
     *
     * Description: determine if a point lays with the bounds of a triangle horizontally.
     */
    boolean inside_trigon(Vec3 s, Vec3 a, Vec3 b, Vec3 c)
    {
        double as_x = s.x-a.x;
        double as_z = s.z-a.z;
        boolean s_ab = (b.x-a.x)*as_z-(b.z-a.z)*as_x > 0;
        if((c.x-a.x)*as_z-(c.z-a.z)*as_x > 0 == s_ab) return false;
        if((c.x-b.x)*(s.z-b.z)-(c.z-b.z)*(s.x-b.x) > 0 != s_ab) return false;
        return true;
    }
    
    
    boolean inside_frontal_trigon(Vec3 s, Vec3 a, Vec3 b, Vec3 c)
    {
        double as_x = s.x-a.x;
        double as_y = s.y-a.y;
        boolean s_ab = (b.x-a.x)*as_y-(b.y-a.y)*as_x > 0;
        if((c.x-a.x)*as_y-(c.y-a.y)*as_x > 0 == s_ab) return false;
        if((c.x-b.x)*(s.y-b.y)-(c.y-b.y)*(s.x-b.x) > 0 != s_ab) return false;
        return true;
    }
    
    /**
     * getDNAAttribute
     *
     * Description:
     */
    public double getDNAAttribute(String weightDNA, int index){
        double value = 0;
        if(weightDNA.length() > index){
            char ch = weightDNA.charAt(index);
            value = Character.getNumericValue(ch);
        }
        return (double)value / 10.0;
    }
}
