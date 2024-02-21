/* Copyright (C) 2021,2022 Jon Taylor

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender;

import armarender.animation.*;
import armarender.image.*;
import armarender.material.*;
import armarender.math.*;
import armarender.object.*;
import armarender.texture.*;
import armarender.ui.*;
import armarender.util.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;
import java.beans.*;

import armarender.object.TriangleMesh.*;
import javax.swing.*; // For JOptionPane

import buoy.event.*;
import buoy.widget.*;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import com.jsevy.jdxf.*;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;


public class ExportBeamNGDrive extends BDialog {
    private LayoutWindow window;
    private ValueField widthDistField, feedRateField, fastRateField;
    private Scene scene;
    private BButton okButton, cancelButton, inchToMMButton, mmToInchButton;
    private String nodeSection, beamSection;
    
    public ExportBeamNGDrive(LayoutWindow window){
        System.out.println("ExportBeamNGDrive");
        this.window = window;
        scene = window.getScene();
        
        /*
        super(window, "Export BeamNG Drive", true);
        
        FormContainer content = new FormContainer(4, 11); // 10
        setContent(BOutline.createEmptyBorder(content, UIUtilities.getStandardDialogInsets()));
        
        content.add(new BLabel("Width:"), 0, 0);
        content.add(widthDistField = new ValueField(0.0, ValueField.NONE, 5), 1, 0);
        widthDistField.setValue(19.24); //
        
        content.add(inchToMMButton = Translate.button("in->mm", this, "inchToMM"), 2, 0);
        content.add(mmToInchButton = Translate.button("mm->in", this, "mmToInch"), 3, 0);
        
        
        content.add(new BLabel("Feed Rate:"), 0, 1);
        content.add(feedRateField = new ValueField(0.0, ValueField.NONE, 5), 1, 1);
        feedRateField.setValue(50.0);
        content.add(new BLabel("units/second"), 2, 1);
        
        content.add(new BLabel("Speed Rate:"), 0, 2);
        content.add(fastRateField = new ValueField(0.0, ValueField.NONE, 5), 1, 2);
        fastRateField.setValue(120.0);
        content.add(new BLabel("units/second"), 2, 2);
        
        
        RowContainer buttons = new RowContainer();
        content.add(buttons, 0, 10, 4, 1, new LayoutInfo());
        buttons.add(okButton = Translate.button("ok", this, "doOk"));
        buttons.add(cancelButton = Translate.button("cancel", this, "dispose"));
        //makeObject();
        pack();
        UIUtilities.centerDialog(this, window);
        //updateComponents();
        setVisible(true);
         */
        exportBeamNGDrive();
    }
    
    
    private void doOk()
    {
        exportBeamNGDrive();
        dispose();
    }
    
    public void inchToMM(){
        widthDistField.setValue(widthDistField.getValue() * 25.4);
    }
    
    public void mmToInch(){
        widthDistField.setValue(widthDistField.getValue() / 25.4);
    }
    
    /**
     * exportBeamNGDrive
     *
     * Description: 
     */
    public void exportBeamNGDrive(){
        System.out.println("exportBeamNGDrive");
        LayoutModeling layout = new LayoutModeling();
        
        extractArcObjects();
        
        String dir = scene.getDirectory() + System.getProperty("file.separator") + scene.getName() + "_beamng_drive";
        File d = new File(dir);
        if(d.exists() == false){
            d.mkdir();
        }
        
        double scale = scene.getScale();
        
        String nodeSectionHeader = "\"nodes\":[\n" +
            "[\"id\", \"posX\", \"posY\", \"posZ\"],\n" +
            "{\"collision\":true},\n" +
            "{\"nodeMaterial\":\"|NM_METAL\"},\n" +
            "{\"nodeWeight\":10},\n";
        
        String nodeSectionFooter = "]";
        
        String beamSectionHeader = "\"beams\":[";
        String beamSectionFooter = "]";
            //["id1:","id2:"],
            //{"beamSpring":1251000,"beamDamp":250},
            //{"beamDeform":16000,"beamStrength":24000},
        
        /*
        for (ObjectInfo info : scene.getObjects()){
            //boolean enabled = layout.isObjectEnabled(obj);
            //ObjectInfo[] children = obj.getChildren();
            Object co = (Object)info.getObject();
            CoordinateSystem c;
            c = layout.getCoords(info);
            if((co instanceof Curve) == true){
                System.out.println(" Curve  " );
                //Curve
            }
            if((co instanceof PointJoinObject) == true){
                System.out.println(" PointJoinObject " );
                PointJoinObject pjo = (PointJoinObject) co;
                c = layout.getCoords(info);
                MeshVertex v[] = pjo.getVertices();
                if(v.length > 0){
                    Vec3 vec = new Vec3(v[0].r);
                    Mat4 mat4 = c.duplicate().fromLocal();
                    mat4.transform(vec);
                    nodeSection += "[\""+ info.getId() + "\"," + vec.x + ","+vec.y+","+vec.z+"],\n";
                }
                // Get curve connected coordinates.
                // pjo.objectA
                // pjo.objectB
            }
        }
        nodeSection += "],\n";
         */
        
        System.out.println("" + nodeSection);
        
        // info.json
        // tutorial.jbeam
        /*
         {
             "MyVehicle":{
                     "information":{
                         "name":"Vehicle Name Here",
                         "authors":"Your Name Here",
                     }
                     "slotType":"main",
                 "nodes":[
                     ["id", "posX", "posY", "posZ"],
                     {"collision":true}
                     {"nodeMaterial":"|NM_METAL"},
                     {"nodeWeight":10},
                     ["c1",-1,1,0],
                     ["c4",1,1,0],
                     ["c2",-1,-1,0],
                     ["c3",1,-1,0],
                     ["top",0,0,1],
                 ],
          
                 "beams":[
                     ["id1:","id2:"],
                     {"beamSpring":1251000,"beamDamp":250},
                     {"beamDeform":16000,"beamStrength":24000},
                     //our content will go here
          
                     //this last line resets important values
                     {"beamType":"|NORMAL","beamPrecompression":1,"beamLongBound":1,"beamShortBound":1}
                 ],
             }
         }
         
         **/
    }
    
    
    /**
     * SegmentInfo
     *
     * Description:
     */
    public class SegmentInfo {
        //public int id = -1;
        public boolean enabled = true;
        public double diameter = 0;
        public Vec3 startPoint = null;
        public Vec3 endPoint = null;
        public int startPointId = -1;
        public int endPointId = -1;
        public Vec3[] subdividedPoints = null;
        
        public SegmentInfo(){
        }
    }
    
    
    /**
     * beams -> extractArcObjects
     *
     * Description: ArcObject to Nodes and beams.
     *
     */
    public void extractArcObjects(){
        LayoutModeling layout = new LayoutModeling();
        
        // Subdivide
        // Get subdivide min distance.
        double snapDistance = 0.04;
        int subdivisions = 8;
        
        
        //
        Vector<Vec3> nodes = new Vector<Vec3>();
        Vector<Vec3[]> beams = new Vector<Vec3[]>();
        Vector points = new Vector();
        Vector<Vec3> endPoints = new Vector<Vec3>(); // segment end points.
        HashMap<Vec3, Double> nodeSizes = new HashMap<Vec3, Double>();
        
        Vector<Vec3[]> subdividedArcs = new Vector<Vec3[]>();
        Vector<Vec3[]> subdividedArcSegments = new Vector<Vec3[]>();
        
        Vector<SegmentInfo> segmentInfos = new Vector<SegmentInfo>(); //
        
       
        //
        // Add arc curve segments to beam list.
        //
        Vector<ObjectInfo> objects = scene.getObjects();
        //for (ObjectInfo obj : objects){
        for(int i = 0; i < objects.size(); i++){
            ObjectInfo obj = objects.elementAt(i);
            boolean enabled = layout.isObjectEnabled(obj);
            
            //ObjectInfo[] children = obj.getChildren();
            
            if(enabled && obj.isVisible() == true &&  obj.getObject() instanceof ArcObject){
                
                System.out.println("   --- Object: " + obj.getName() );
                
                double arcDiameter = ((ArcObject)obj.getObject()).getTubeDiameter(); // ***
                
                //ObjectInfo objClone = obj.duplicate();
                //Object co = (Object)objClone.getObject();
                
                CoordinateSystem c;
                c = layout.getCoords(obj);
                
                Vector subdividedPointsAcc = new Vector();
                Vector subdividedSegmentPoints = new Vector();
                
                
                Mesh mesh = (Mesh) obj.getObject(); // Object3D
                Vec3 [] verts = mesh.getVertexPositions();
                for(int v = 1; v < verts.length; v++){
                    Vec3 vecA = new Vec3((Vec3)verts[v-1]);
                    Vec3 vecB = new Vec3((Vec3)verts[v-0]);
                    
                    Mat4 mat4 = c.duplicate().fromLocal();
                    mat4.transform(vecA);
                    mat4.transform(vecB);
                    
                    Vec3[] tempVec = new Vec3[2];
                    tempVec[0] = vecA;
                    tempVec[1] = vecB;
                    
                    beams.addElement( tempVec );
                    
                    Vector<Vec3> subdividedPoints = new Vector<Vec3>();
                    subdividedPoints = subdividePoints(vecA, vecB, subdivisions, subdividedPoints);
                    subdividedPointsAcc.addAll(subdividedPoints);
                    
                    Vec3[] arcSubPoints = new Vec3[subdividedPoints.size()];
                    for(int x = 0; x < subdividedPoints.size(); x++){
                        Vec3 p = (Vec3)subdividedPoints.elementAt(x);
                        arcSubPoints[x] = p;
                        
                        //System.out.println("  curve: " + v + " x: " + x + " : " + p);
                    }
                    subdividedArcSegments.addElement(arcSubPoints);
                    
                    SegmentInfo segInfo = new SegmentInfo();
                    segInfo.diameter = arcDiameter;
                    segInfo.startPoint = vecA;
                    segInfo.endPoint = vecB;
                    segInfo.subdividedPoints = arcSubPoints;
                    segmentInfos.addElement(segInfo);
                }
                
                for(int v = 0; v < verts.length; v++){
                    Vec3 vecA = new Vec3((Vec3)verts[v]);
                    Mat4 mat4 = c.duplicate().fromLocal();
                    mat4.transform(vecA);
                    points.addElement(vecA);
                    if(nodeExists(nodes, vecA, arcDiameter) == false){
                        addNode(nodes, vecA, arcDiameter);
                        nodeSizes.put(vecA, arcDiameter);
                        System.out.println("  curve " + i + "  v " + v + "  addNode  "   );
                    }
                    if(v == 0){                                 // First
                        endPoints.addElement(vecA);
                        System.out.println("  curve " + i + "  v " + v + "  end point  "   );
                    }
                    if(v == verts.length - 1){
                        endPoints.addElement(vecA);
                        System.out.println("  curve " + i + "  v " + v + "  end point  "   );
                    }
                }
                
                Vec3[] arcSubPoints = new Vec3[subdividedPointsAcc.size()];
                for(int x = 0; x < subdividedPointsAcc.size(); x++){
                    Vec3 p = (Vec3)subdividedPointsAcc.elementAt(x);
                    arcSubPoints[x] = p;
                }
                subdividedArcs.addElement(arcSubPoints);
            }
        }
        
        // print subdivided arc points
        /*
        for(int s = 0; s < subdividedArcs.size(); s++){
            Vec3[] subdivided = subdividedArcs.elementAt(s);
            System.out.print(" Sub Arc " + s);
            for(int p = 0; p < subdivided.length; p++){
                Vec3 point = subdivided[p];
                System.out.println("        p " + point);
            }
        }
         */
        
        HashMap<String, Double> closestIntersectionDist = new HashMap<String, Double>();
        HashMap<String, Vec3> closestIntersectionPoint = new HashMap<String, Vec3>();
        HashMap<String, Integer> closestIntersectionIndex = new HashMap<String, Integer>();
        
        
        boolean r = subdivideSegments(nodes, segmentInfos, subdivisions);
        while(r == true){
            r = subdivideSegments(nodes, segmentInfos, subdivisions);
        }
        
        
        /*
        for(int s = 0; s < subdividedArcSegments.size(); s++){
        
            Vec3[] subdivided = subdividedArcSegments.elementAt(s);
            System.out.print(" Sub Arc Segment " + s);
            
            for(int p = 0; p < subdivided.length; p++){
                Vec3 point = subdivided[p];
                Vec3 firstPoint = subdivided[0];
                Vec3 lastPoint = subdivided[subdivided.length - 1];
                //System.out.println("        p " + point);
                
                for(int s2 = 0; s2 < subdividedArcSegments.size(); s2++){
                    String arcPairKey = s + "_" + s2;
                    //double closestDistance = 99999;
                    //Vec3 closestPoint = null;
                    if(s != s2){
                        Vec3[] subdivided2 = subdividedArcSegments.elementAt(s2);
                        for(int p2 = 0; p2 < subdivided2.length; p2++){
                            Vec3 point2 = subdivided2[p2];
                            
                            boolean notEnd = true;
                            
                            for(int e = 0; e < endPoints.size(); e++){
                                Vec3 endPoint = endPoints.elementAt(e);
                                if(point.distance(endPoint) < ( snapDistance * 2)  ){
                                    //notEnd = false;
                                }
                            }
                            if( p < 4 || p > subdivided.length - 5  ){
                                //notEnd = false;
                            }
                            double firstPDist = firstPoint.distance(point);
                            double lastPDist = lastPoint.distance(point);
                            //System.out.println(" firstPDist " + firstPDist + " " + lastPDist + " snapDistance: " + snapDistance );
                            
                            if( firstPDist <= (snapDistance * 2) || lastPDist <= (snapDistance * 2) ){
                                notEnd = false;
                            }
                            
                            
                            double distance = point.distance(point2);
                            //if( distance < firstPDist || distance < lastPDist ){
                                //notEnd = false;
                            //}
                            
                            if(distance < snapDistance && distance > 0 && notEnd){ // distance < closestDistance &&
                                //closestPoint = point;
                                //closestDistance = distance;
                                
                                //System.out.println(" - " + distance + " < snapDistance " + snapDistance + " i: " + p + " " + p2);
                                
                                Double existDist = closestIntersectionDist.get(arcPairKey);
                                if(existDist == null){
                                    existDist = distance;
                                }
                                if(distance <= existDist){
                                    existDist = distance;
                                    
                                    Vec3 existPoint = closestIntersectionPoint.get(arcPairKey);
                                    if(existPoint == null){
                                        existPoint = point;
                                    }
                                    closestIntersectionPoint.put(arcPairKey, existPoint);
                                    closestIntersectionIndex.put(arcPairKey, p);
                                    closestIntersectionDist.put(arcPairKey, existDist);
                                }
                                
                            }
                        }
                    }
                }
            }
        }
        
        
        for(int s = 0; s < subdividedArcSegments.size(); s++){
            Vec3[] subdivided = subdividedArcSegments.elementAt(s);
            for(int s2 = 0; s2 < subdividedArcSegments.size(); s2++){
                if(s != s2){
                    String arcPairKey = s + "_" + s2;
                    System.out.println("***");
                    
                    Double closestPairDist = closestIntersectionDist.get(arcPairKey);
                    Vec3 closestPairPoint = closestIntersectionPoint.get(arcPairKey);
                    Integer closestPointIndex = closestIntersectionIndex.get(arcPairKey);
                    if(closestPairPoint != null && closestPairDist != null){
                        System.out.println("  " + arcPairKey + " dist: " + closestPairDist + " point: " + closestPairPoint +
                                           " ind: " + closestPointIndex + " len: " + subdivided.length );
                        
                        
                        Vec3[] splitSegA = new Vec3[2];
                        splitSegA[0] = new Vec3(subdivided[0]);
                        splitSegA[1] = new Vec3(subdivided[closestPointIndex]);
                        Vec3[] splitSegB = new Vec3[2];
                        splitSegB[0] = new Vec3(subdivided[closestPointIndex]);
                        splitSegB[1] = new Vec3(subdivided[subdivided.length - 1]);
                        
                        //beams.remove(b);
                        beams.addElement( splitSegA );
                        beams.addElement( splitSegB );
                        
                        // Remove
                        
                        
                        // addNode( nodes, closestMidPoint, snapDistance);
                    }
                }
            }
        }
        */
            
        
        // Detect intersection.
        /*
        for(int s = 0; s < subdividedArcs.size(); s++){
            Vec3[] subdivided = subdividedArcs.elementAt(s);
            System.out.print(" Subdivided Arc " + s);
            //HashMap closest
            for(int p = 0; p < subdivided.length; p++){
                Vec3 point = subdivided[p];
                double closestDistance = 99999;
                Vec3 closestPoint = null;
                for(int s2 = 0; s2 < subdividedArcs.size(); s2++){
                    if(s != s2){
                        String arcPairKey = s + "_" + s2;
                        Vec3[] subdivided2 = subdividedArcs.elementAt(s2);
                        for(int p2 = 0; p2 < subdivided2.length; p2++){
                            Vec3 point2 = subdivided2[p2];
                        
                            double distance = point.distance(point2);
                            if(distance < closestDistance && distance < snapDistance){
                                closestPoint = point;
                                closestDistance = distance;
                            }
                        }
                    }
                }
                if(closestPoint != null){
                    System.out.println("     arc " + s + " closest point " + closestPoint );
                }
            }
        }
        */
        
        //
        // Split segments with intersecting ends not in current
        //
        
        /*
        Vector<Vec3[]> beamsCopy = new Vector<Vec3[]>();
        for(int b = 0; b < beams.size(); b++){
            Vec3[] currCurve = beams.elementAt(b);
            beamsCopy.addElement( currCurve );
        }
        for(int b = 0; b < beamsCopy.size(); b++){                  // For each Beam
            Vec3[] currCurve = beamsCopy.elementAt(b);
            System.out.println(" Subdivide BEAM " + b + " " );
            
            Vector<Vec3> subdividedPoints = new Vector<Vec3>();
            subdividedPoints = subdividePoints(currCurve[0], currCurve[1], subdivisions, subdividedPoints);
            
            Double curveDiameter = nodeSizes.get(currCurve[0]);
            if(curveDiameter == null){
                curveDiameter = snapDistance;
            }
            System.out.println("   curveDiameter: " + curveDiameter);
            
            double closestDistance = 99999;
            int closestIndex = -1;
            Vec3 closestMidPoint = null;
            for(int s = 0; s < subdividedPoints.size(); s++){       // For each subdivided beam point
                Vec3 subPoint = subdividedPoints.elementAt(s);
                Vec3 subPointX = getNode(nodes, subPoint, curveDiameter);
                //System.out.print("            " + subPoint + " " );
                for(int p = 0; p < endPoints.size(); p++){          // does this beam midpoint contact another segment end
                    Vec3 endPoint = endPoints.elementAt(p);
                    Double nodeDistance = nodeSizes.get(endPoint);
                    if(nodeDistance == null){
                        nodeDistance = snapDistance;
                    }
                    
                    Vec3 endPointX = getNode(nodes, endPoint, nodeDistance);
                    
                    double dist = subPointX.distance(endPointX);
                    //System.out.println("     dist  " + dist);
                    
                    if(dist < closestDistance && dist < (nodeDistance * 2)   && dist > 0   ){ // && dist > 0
                        System.out.println("         ** dist: " + dist);
                       
                            closestIndex = s;
                            closestDistance = dist;
                            closestMidPoint = subPointX;
                        
                    }
                }
            }
            if(closestMidPoint != null){
                System.out.println(" *** closest point  a " + currCurve[0] + " - " +
                                   closestMidPoint +  " - " +
                                   currCurve[1]
                                   );
                // Remove this segment and add two new ones that end at the new mid point.
                
                Vec3[] splitSegA = new Vec3[2];
                splitSegA[0] = new Vec3(currCurve[0]);
                splitSegA[1] = new Vec3(closestMidPoint);
                Vec3[] splitSegB = new Vec3[2];
                splitSegB[0] = new Vec3(closestMidPoint);
                splitSegB[1] = new Vec3(currCurve[1]);
                
                //beams.remove(b);
                beams.addElement( splitSegA );
                beams.addElement( splitSegB );
                
                System.out.println(" slip a " + splitSegA[0] + " - " + splitSegA[1]);
                System.out.println(" slip b " + splitSegB[0] + " - " + splitSegB[1]);
                //b++;
                
                // Add a node to this mid point.
                //nodes.addElement(closestMidPoint);
                addNode( nodes, closestMidPoint, snapDistance);
            }
            //
        }
         */
        
        String beamFileNodes = "";
        String beamFileBeams = "";
        
        //
        // Add Nodes.
        //
        for(int n = 0; n < nodes.size(); n++){
            Vec3 node = nodes.elementAt(n);
            //Cube
            Vec3[] nodeMarker = new Vec3[2];
            nodeMarker[0] = new Vec3(node);
            nodeMarker[1] = new Vec3(node.x, node.y + 1, node.z);
            
            Curve tempCurve = getCurve(nodeMarker);
            ObjectInfo testCurveInfo = new ObjectInfo(tempCurve, new CoordinateSystem(), "NODE");
            
            scene.addObject(testCurveInfo, null);
            
            if(beamFileNodes.length() != 0){
                beamFileNodes += ",";
            }
            beamFileNodes += "[\"" + n + "\", "+ node.x +"," + node.y + ","+node.z+"]";
            
            beamFileNodes += "\r\n";
            
        }
        
        //
        // Add Beams
        //
        // Vector<SegmentInfo> segmentInfos
        for(int b = 0; b < segmentInfos.size(); b++){
            SegmentInfo segInfo = segmentInfos.elementAt(b);
            if( segInfo.enabled ){
                Vec3[] currBeam = new Vec3[2];
                currBeam[0] = segInfo.startPoint;
                currBeam[1] = segInfo.endPoint;
                
                Curve tempCurve = getCurve(currBeam);
                ObjectInfo testCurveInfo = new ObjectInfo(tempCurve, new CoordinateSystem(), "BEAM " + b);
                scene.addObject(testCurveInfo, null);
                
                //
                int aIndex = -1;
                int bIndex = -1;
                for(int n = 0; n < nodes.size(); n++){
                    Vec3 node = nodes.elementAt(n);
                    Vec3 aVec = currBeam[0];
                    Vec3 bVec = currBeam[1];
                    
                    //if( nodeExists(nodes, aVec, snapDistance) ){
                    //    Vec3 aNode = getNode(nodes, aVec, snapDistance);
                    //}
                    double aDist = node.distance(aVec);
                    double bDist = node.distance(bVec);
                    
                    if(aDist < snapDistance){
                        aIndex =  n;
                    }
                    if(bDist < snapDistance){
                        bIndex =  n;
                    }
                    
                }
                if(beamFileBeams.length() != 0){
                    beamFileBeams += ",";
                }
                beamFileBeams += "[\"" + aIndex + "\", \""+ bIndex +"\"]\r\n";
            }
        }
        
        /*
        for(int b = 0; b < beams.size(); b++){
            Vec3[] currBeam = beams.elementAt(b);
            Curve tempCurve = getCurve(currBeam);
            ObjectInfo testCurveInfo = new ObjectInfo(tempCurve, new CoordinateSystem(), "BEAM " + b);
            scene.addObject(testCurveInfo, null);
            
            //
            int aIndex = -1;
            int bIndex = -1;
            for(int n = 0; n < nodes.size(); n++){
                Vec3 node = nodes.elementAt(n);
                Vec3 aVec = currBeam[0];
                Vec3 bVec = currBeam[1];
                
                //if( nodeExists(nodes, aVec, snapDistance) ){
                //    Vec3 aNode = getNode(nodes, aVec, snapDistance);
                //}
                double aDist = node.distance(aVec);
                double bDist = node.distance(bVec);
                
                if(aDist < snapDistance){
                    aIndex =  n;
                }
                if(bDist < snapDistance){
                    bIndex =  n;
                }
                
            }
            beamFileBeams += "[\"" + aIndex + "\", \""+ bIndex +"\"]\n";
        }
         */
        
        
        System.out.println("" + beamFileNodes);
        System.out.println("" + beamFileBeams);
        
        nodeSection = beamFileNodes;
        beamSection = beamFileBeams;
        
        window.updateImage();
        window.updateMenus();
        window.rebuildItemList();
        
    }
    
    
    public boolean subdivideSegments(Vector<Vec3> nodes, Vector<SegmentInfo> segmentInfos, int subdivisions){
        boolean result = false;
        
        HashMap<String, Double> closestIntersectionDist = new HashMap<String, Double>();
        HashMap<String, Vec3> closestIntersectionPoint = new HashMap<String, Vec3>();
        HashMap<String, Integer> closestIntersectionIndex = new HashMap<String, Integer>();
        
        
        //for(int s = 0; s < subdividedArcSegments.size(); s++){
        for(int s = 0; s < segmentInfos.size(); s++){
            SegmentInfo segInfo = segmentInfos.elementAt(s);
        
        
            Vec3[] subdivided = segInfo.subdividedPoints; //  subdividedArcSegments.elementAt(s);
            System.out.print(" Sub Arc Segment " + s);
            
            for(int p = 0; p < subdivided.length && segInfo.enabled; p++){
                Vec3 point = subdivided[p];
                Vec3 firstPoint = subdivided[0];
                Vec3 lastPoint = subdivided[subdivided.length - 1];
                //System.out.println("        p " + point);
                
                
                //for(int s2 = 0; s2 < subdividedArcSegments.size(); s2++){
                for(int s2 = 0; s2 < segmentInfos.size(); s2++){
                    SegmentInfo segInfo2 = segmentInfos.elementAt(s2);
                
                    String arcPairKey = s + "_" + s2;
                    //double closestDistance = 99999;
                    //Vec3 closestPoint = null;
                    if(s != s2 && segInfo2.enabled){
                        Vec3[] subdivided2 = segInfo2.subdividedPoints; // subdividedArcSegments.elementAt(s2);
                        for(int p2 = 0; p2 < subdivided2.length; p2++){
                            Vec3 point2 = subdivided2[p2];
                            
                            boolean notEnd = true;
                            
                            //for(int e = 0; e < endPoints.size(); e++){
                            //    Vec3 endPoint = endPoints.elementAt(e);
                            //    if(point.distance(endPoint) < ( snapDistance * 2)  ){
                                    //notEnd = false;
                            //    }
                            //}
                            //if( p < 4 || p > subdivided.length - 5  ){
                                //notEnd = false;
                            //}
                            double firstPDist = firstPoint.distance(point);
                            double lastPDist = lastPoint.distance(point);
                            //System.out.println(" firstPDist " + firstPDist + " " + lastPDist + " snapDistance: " + snapDistance );
                            
                            if( firstPDist <= (segInfo.diameter * 2) || lastPDist <= (segInfo.diameter * 2) ){  // snapDistance segInfo.diameter
                                notEnd = false;
                            }
                            
                            double distance = point.distance(point2);
                            if(distance < segInfo.diameter && distance > 0 && notEnd){ // distance < closestDistance &&
                                //closestPoint = point;
                                //closestDistance = distance;
                                
                                //System.out.println(" - " + distance + " < snapDistance " + snapDistance + " i: " + p + " " + p2);
                                
                                Double existDist = closestIntersectionDist.get(arcPairKey);
                                if(existDist == null){
                                    existDist = distance;
                                }
                                if(distance <= existDist){
                                    existDist = distance;
                                    
                                    Vec3 existPoint = closestIntersectionPoint.get(arcPairKey);
                                    if(existPoint == null){
                                        existPoint = point;
                                    }
                                    closestIntersectionPoint.put(arcPairKey, existPoint);
                                    closestIntersectionIndex.put(arcPairKey, p);
                                    closestIntersectionDist.put(arcPairKey, existDist);
                                }
                                
                                
                            }
                        }
                    }
                }
            }
        }
        
        
        //for(int s = 0; s < subdividedArcSegments.size(); s++){
            //Vec3[] subdivided = subdividedArcSegments.elementAt(s);
        for(int s = 0; s < segmentInfos.size(); s++){
            SegmentInfo segInfo = segmentInfos.elementAt(s);
            Vec3[] subdivided = segInfo.subdividedPoints;
        
            //for(int s2 = 0; s2 < subdividedArcSegments.size(); s2++){
            for(int s2 = 0; s2 < segmentInfos.size(); s2++){
                SegmentInfo segInfo2 = segmentInfos.elementAt(s2);
                
                
                if(s != s2){
                    String arcPairKey = s + "_" + s2;
                    System.out.println("***");
                    
                    Double closestPairDist = closestIntersectionDist.get(arcPairKey);
                    Vec3 closestPairPoint = closestIntersectionPoint.get(arcPairKey);
                    Integer closestPointIndex = closestIntersectionIndex.get(arcPairKey);
                    if(closestPairPoint != null && closestPairDist != null){
                        System.out.println("  " + arcPairKey + " dist: " + closestPairDist + " point: " + closestPairPoint +
                                           " ind: " + closestPointIndex + " len: " + subdivided.length );
                        
                        
                        Vec3[] splitSegA = new Vec3[2];
                        splitSegA[0] = new Vec3(subdivided[0]);
                        splitSegA[1] = new Vec3(subdivided[closestPointIndex]);
                        Vec3[] splitSegB = new Vec3[2];
                        splitSegB[0] = new Vec3(subdivided[closestPointIndex]);
                        splitSegB[1] = new Vec3(subdivided[subdivided.length - 1]);
                        
                        //beams.remove(b);
                        //beams.addElement( splitSegA );
                        //beams.addElement( splitSegB );
                        
                        Vector<Vec3> subdividedPointsA = new Vector<Vec3>();
                        subdividedPointsA = subdividePoints(splitSegA[0], splitSegA[1], subdivisions, subdividedPointsA);
                        
                        SegmentInfo segInfoA = new SegmentInfo();
                        segInfoA.diameter = segInfo.diameter; // arcDiameter;
                        segInfoA.startPoint = splitSegA[0];
                        segInfoA.endPoint = splitSegA[1];
                        segInfoA.subdividedPoints = vectorToArray(subdividedPointsA);
                        segmentInfos.addElement(segInfoA);
                        
                        
                        
                        Vector<Vec3> subdividedPointsB = new Vector<Vec3>();
                        subdividedPointsB = subdividePoints(splitSegB[0], splitSegB[1], subdivisions, subdividedPointsB);
                        
                        SegmentInfo segInfoB = new SegmentInfo();
                        segInfoB.diameter = segInfo.diameter; // arcDiameter;
                        segInfoB.startPoint = splitSegB[0];
                        segInfoB.endPoint = splitSegB[1];
                        segInfoB.subdividedPoints = vectorToArray(subdividedPointsB);
                        segmentInfos.addElement(segInfoB);
                        
                        
                        // Remove / disable old
                        segInfo.enabled = false;
                        
                        
                        // addNode( nodes, closestMidPoint, snapDistance);
                        
                        
                        
                        result = true;
                        return true; // Break out of this iteration.
                    }
                }
            }
        }
        
        
        
        return result;
    }
    
    public Vec3[] vectorToArray(Vector vec){
        Vec3[] arr = new Vec3[vec.size()];
        for(int i = 0; i < vec.size(); i++){
            arr[i] = (Vec3)vec.elementAt(i);
        }
        return arr;
    }
    
    
    public void addNode(Vector<Vec3> nodes, Vec3 nodePoint, double distance){
        boolean found = false;
        for(int n = 0; n < nodes.size(); n++){
            Vec3 existingNode = nodes.elementAt(n);
            double dist = existingNode.distance(nodePoint);
            if( dist < distance ){
                found = true;
            }
        }
        if(found == false){
            nodes.addElement(nodePoint);
        }
    }
    
    public boolean nodeExists(Vector<Vec3> nodes, Vec3 nodePoint, double distance){
        boolean found = false;
        for(int n = 0; n < nodes.size(); n++){
            Vec3 existingNode = nodes.elementAt(n);
            double dist = existingNode.distance(nodePoint);
            if( dist < distance ){
                found = true;
            }
        }
        return found;
    }
    
    public Vec3 getNode(Vector<Vec3> nodes, Vec3 nodePoint, double distance){
        Vec3 result = new Vec3(0,0,0);
        if(nodePoint != null){
            result = nodePoint;
        }
        boolean found = false;
        for(int n = 0; n < nodes.size(); n++){
            Vec3 existingNode = nodes.elementAt(n);
            double dist = existingNode.distance(nodePoint);
            if(dist < distance){
                //found = true;
                result = existingNode;
                return result;
            }
        }
        return result;
    }
    
    
    /**
     *
     *
     */
    public Vector subdividePoints(Vec3 a, Vec3 b, int subdivisions, Vector<Vec3> points){
        subdivisions--;
        //Vector<Vec3> points = new Vector<Vec3>();
        Vec3 midPoint = a.midPoint(b);
        if(points.contains(a) == false){
            points.addElement(a);
        }
        if(subdivisions > 0){
            subdividePoints(a, midPoint, subdivisions, points);
        }
        if(points.contains(midPoint) == false){
            points.addElement(midPoint);
        }
        
        if(subdivisions > 0){
            subdividePoints(midPoint, b, subdivisions, points);
        }
        if(points.contains(b) == false){
            points.addElement(b);
        }
        
        return points;
    }
    
    public Curve getCurve(Vec3[] points){
        float smooths[] = new float[points.length];
        for(int i = 0; i < points.length; i++){
            smooths[i] = 1.0f;
        }
        Curve curve = new Curve(points, smooths, Mesh.APPROXIMATING, false);
        return curve;
    }
    
    
    
    /**
     * exportTubeGCode
     *
     * Description: export gcode for tube notcher.
     *  Straight tubes notch detail will be modeled around a mesh object but tubes to be
     *  bent will be modeled around a Curve.
     *
     *
     */
    public void exportTubeGCode(double width){
        LayoutModeling layout = new LayoutModeling();
        System.out.println("Export tube width: " + width);
        
        int fastRate = (int)fastRateField.getValue();
        int slowRate = (int)feedRateField.getValue();
        
        String dir = scene.getDirectory() + System.getProperty("file.separator") + scene.getName() + "_gCode_t";
        File d = new File(dir);
        if(d.exists() == false){
            d.mkdir();
        }
        
        double scale = scene.getScale();
        /*
        try {
            // Read current scale for this project.
            Properties prop = new Properties();
            InputStream input = null;
            OutputStream output = null;
            
            String dir2 = scene.getDirectory() + System.getProperty("file.separator") + getName() + "_layout_data";
            File d2 = new File(dir2);
            if(d2.exists() == false){
                d2.mkdir();
            }
            dir2 = dir2 + System.getProperty("file.separator") + "scale.properties";
            d2 = new File(dir2);
            if(d2.exists() == false){
                //(works for both Windows and Linux)
                d2.getParentFile().mkdirs();
                d2.createNewFile();
            }
            
            input = new FileInputStream(dir2);
            // load a properties file
            prop.load(input);
            
            String v = prop.getProperty("export_scale");
            if(v != null){
                scale = Double.parseDouble(v);
            }
        } catch (Exception e){
            System.out.println("Error " + e);
            e.printStackTrace();
        }
         */
        
        // Calculate bounds of object to calculate centre for unrolling points around tube circumfrance.
        for (ObjectInfo obj : scene.getObjects()){
            boolean enabled = layout.isObjectEnabled(obj);
            ObjectInfo[] children = obj.getChildren();
            
            if(children.length > 0 && enabled){
                
                System.out.println("   --- Object: " + obj.getName() + " count: " + children.length);
                try {
                    boolean writeFile = false;
                    String gcode2 = "";
                    gcode2 += "; Arma Automotive\n";
                    gcode2 += "; Tube Notcher\n";
                    gcode2 += "; Part: " + obj.getName() + "\n";
                    gcode2 += "G1;\n";
                    Vector polygons = new Vector();
                    
                    double centreX = 0;
                    double centreY = 0;
                    double centreZ = 0;
                    
                    double boundsMinX = 9999;
                    double boundsMaxX = -9999;
                    double boundsMinY = 9999;
                    double boundsMaxY = -9999;
                    double boundsMinZ = 9999;
                    double boundsMaxZ = -9999;
                    
                    double minX = 9999;
                    double minY = 9999;
                    double minZ = 9999;
                    double maxX = -9999;
                    double maxY = -9999;
                    double maxZ = -9999;
                    
                    HashMap<Vector, Integer> polygonOrder = new HashMap<Vector, Integer>();
                    
                    //BoundingBox bounds = obj.getBounds();
                    //System.out.println(" bounds: " +
                    //                   " x: " + bounds.minx + " x " + bounds.maxx +
                    //                   " y" + bounds.miny + " maxy: " + bounds.maxy +
                    //                   " minz: " + bounds.minz + " maxz: " + bounds.maxz );
                    double widthScale = 1;
                    
                    ObjectInfo objClone = obj.duplicate();
                    objClone.setLayoutView(false);
                    Object co = (Object)objClone.getObject();
                    if(co instanceof Mesh && objClone.isVisible() == true){ // && child_enabled
                        CoordinateSystem c;
                        c = layout.getCoords(objClone); // Read cutting coord from file
                        objClone.setCoords(c);
                        Mesh mesh = (Mesh) objClone.getObject(); // Object3D
                        Vec3 [] verts = mesh.getVertexPositions();
                        for (Vec3 vert : verts){
                            // Transform vertex points around object loc/rot.
                            Mat4 mat4 = c.duplicate().fromLocal();
                            mat4.transform(vert);
                            
                            if(vert.x > boundsMaxX){
                                boundsMaxX = vert.x;
                            }
                            if(vert.x < boundsMinX){
                                boundsMinX = vert.x;
                            }
                            if(vert.y > boundsMaxY){
                                boundsMaxY = vert.y;
                            }
                            if(vert.y < boundsMinY){
                                boundsMinY = vert.y;
                            }
                            if(vert.z > boundsMaxZ){
                                boundsMaxZ = vert.z;
                            }
                            if(vert.z < boundsMinZ){
                                boundsMinZ = vert.z;
                            }
                        }
                    
                        centreY = (boundsMinY + ((boundsMaxY - boundsMinY)/2));
                        centreZ = (boundsMinZ + ((boundsMaxZ - boundsMinZ)/2));
                        
                        double radius = ((boundsMaxZ - boundsMinZ)/2);
                        double circumference = Math.PI * 2 * radius;
                        
                         widthScale = width / circumference;   // 20 / 6.28  = 3.18     2 / 6.28 = 0.318471337579618
                        
                        for (ObjectInfo child : children){
                            ObjectInfo childClone = child.duplicate();
                            childClone.setLayoutView(false);
                            co = (Object)childClone.getObject();
                            
                            boolean child_enabled = layout.isObjectEnabled(child);
                            if(co instanceof Mesh && child.isVisible() == true &&
                               co instanceof Curve && child_enabled){
                                
                                writeFile = true;
                                Vector<Vec3> polygon = new Vector<Vec3>();
                                //CoordinateSystem c;
                                c = layout.getCoords(childClone); // Read cutting coord from file
                                childClone.setCoords(c);
                                
                                double cutDepth = layout.getPolyDepth(child);
                                //
                                
                                mesh = (Mesh) childClone.getObject(); // Object3D
                                verts = mesh.getVertexPositions(); // Vec3 []
                                
                                // Calculate distance between vertecies in 3d space.
                                double [] vertDistances = new double[verts.length];
                                for(int v = 0; v < verts.length - 1; v++){
                                    Vec3 vertA = verts[v];
                                    Vec3 vertB = verts[v + 1];
                                    double distance = Math.sqrt(Math.pow(vertA.x - vertB.x, 2) + Math.pow(vertA.y - vertB.y, 2) + Math.pow(vertA.z - vertB.z, 2));
                                    vertDistances[v] = distance;
                                }
                                
                                boolean drillLowered = false;
                                double drillLowerDistance = 0.0;
                                
                                //System.out.println("POLY");

                                Vec3 vec0 = null; // first point up
                                Vec3 vec1 = null; // first poingt down
                                
                                int v = 0;
                                //double previousAngle = -1;
                                for (Vec3 vert : verts){
                                    // Transform vertex points around object loc/rot.
                                    Mat4 mat4 = c.duplicate().fromLocal();
                                    mat4.transform(vert);
                                    
                                    //
                                    // Transform verticies to unroll them around the part circumfrance.
                                    //
                                    double angle = scene.getAngle(vert.z - centreZ, vert.y - centreY);
                                    vert.z = ((angle / 360) * circumference);
                                    vert.y = cutDepth;
                                    if(drillLowered == false){ // Drill is up
                                        //vert.y = vert.y + drillLowerDistance;
                                        drillLowerDistance = 0.5;
                                    }
                                    if(v == 0){
                                        //System.out.println("XXXXXXXXX");
                                        drillLowerDistance = 0.5;
                                        vert.y += drillLowerDistance; // First point is positioning above cut surface.
                                        //drillLowered = true; // next point is lowered.
                                        //drillLowerDistance = 0.0;
                                    }
                                    
                                    // Apply scale
                                    vert.x = vert.x * scale;
                                    vert.y = vert.y * scale;
                                    vert.z = vert.z * scale;
                                    vert.f = slowRate;
                                    if(v == 0 ){ // || v == 1
                                        vert.f = fastRate;
                                    }
                                    
                                    // If two verticies cross 360 - 0 degrees insert a raise cutter operation.
                                    boolean splitPoly = false;
                                    if(v > 0 ){ // && v < verts.length - 1
                                        Vec3 previousVert = verts[v - 1];
                                        Vec3 currVert = verts[v];
                                        Vec3 secondPrevVert = null;
                                        if(v > 1){
                                            secondPrevVert = verts[v - 2];
                                        }
                                        double distance = Math.sqrt(Math.pow(currVert.x - previousVert.x, 2) +
                                                                    Math.pow(currVert.y - previousVert.y, 2) +
                                                                    Math.pow(currVert.z - previousVert.z, 2));
                                        double unrolledDistance = vertDistances[v - 1];
                                        
                                        //System.out.println(" udist " + unrolledDistance + " d: " + distance + "     " + Math.abs( unrolledDistance - distance) );
                                        //System.out.println(" udist " + (unrolledDistance*4) + " d: " + distance + "     " );
                                        if(distance > unrolledDistance * 8){ // *4
                                            //System.out.println(" raise  !!!!!!!!!!! *** " + childClone.getName());
                                            //System.out.println("   distance  " + distance + " unrolledDistance: " + unrolledDistance );
                                            
                                            //System.out.println("prev " + previousVert.x + " " + previousVert.y + " " + previousVert.z);
                                            //System.out.println("curr " + currVert.x + " " + currVert.y + " " + currVert.z);
                                            
                                            if(vert.z > previousVert.z){
                                                //System.out.println("A");
                                                
                                                Vec3 connectSplitInsert = new Vec3( vert.x, vert.y - 0, -circumference + vert.z );
                                                connectSplitInsert.f = 40; // id v == 2
                                                polygon.addElement(connectSplitInsert);
                                                
                                            } else {
                                                //System.out.println("B");
                                                
                                                // secondPrevVert
                                                /*
                                                if(secondPrevVert != null){
                                                    double splitA = 0;
                                                    splitA = circumference - secondPrevVert.z;
                                                    Vec3 connectSplitInsertA = new Vec3( secondPrevVert.x, 0, secondPrevVert.z + splitA);
                                                    //connectSplitInsertA.f = 20; // id v == 2
                                                    polygon.addElement(connectSplitInsertA);
                                                }
                                                */
                                                
                                                Vec3 connectSecondInsert = new Vec3( previousVert.x, previousVert.y - 0, previousVert.z );
                                                //connectSecondInsert.f = 30; // id v == 2
                                                polygon.addElement(connectSecondInsert);
                                                
                                                double split = 0; // circumference /
                                                split = circumference - vert.z;
                                                
                                                Vec3 connectSplitInsert = new Vec3( vert.x, vert.y - 0, vert.z + split);
                                                //connectSplitInsert.f = 40; // id v == 2
                                                polygon.addElement(connectSplitInsert);
                                            }
                                            
                                            
                                            drillLowered = false;
                                            drillLowerDistance = 0.5;
                                            //System.out.println(" Lift ");
                                            splitPoly = true;
                                            //
                                            Vec3 vertInsert = new Vec3( previousVert.x, previousVert.y + drillLowerDistance, previousVert.z); // raise drill
                                            vertInsert.f = fastRate;
                                            polygon.addElement(vertInsert);
                                            
                                            drillLowered = false;
                                            drillLowerDistance = 0.5;
                                            Vec3 vertInsertUp = new Vec3( vert.x, vert.y + drillLowerDistance, vert.z); //
                                            vertInsertUp.f = fastRate;
                                            polygon.addElement(vertInsertUp);
                                            
                                            
                                            
                                            drillLowered = true;
                                            drillLowerDistance = 0.0;
                                        }
                                    }
                                    
                                    polygon.addElement(vert);
                                    
                                    // Lower drill (start of part or after split raise)
                                    if(v == 0 ){ // || (drillLowered == false && v > 0)
                                        //System.out.println(" Lower !!!!!!!!!!! ");
                                        drillLowered = true;
                                        drillLowerDistance = 0.0;
                                        double y = cutDepth * scale; // recalculate y
                                        Vec3 vertInsert = new Vec3(vert.x, y + drillLowerDistance, vert.z); // lower drill
                                        vertInsert.f = slowRate;
                                        polygon.addElement(vertInsert);
                                        
                                        vec0 = vert;
                                        vec1 = vertInsert;
                                    }
                                    
                                    // Raise drill (end of part)
                                    if(v == verts.length - 1){  // Last vert in curve
                                        //System.out.println(" RAISE !!!!!!!!!!!X ");
                                        
                                        // If the last point spans across the rotation, lift before cutting through part.
                                        
                                        // vert is current vert
                                        
                                        // Connect poly (end to start)
                                        Vec3 currVert = vert ; // verts[v];
                                        Vec3 previousVert = verts[v - 1];
                                        double prevDistance = Math.sqrt(Math.pow( currVert.x - previousVert.x, 2) +
                                                                    Math.pow(currVert.y - previousVert.y, 2) +
                                                                    Math.pow(currVert.z - previousVert.z, 2));
                                        double distance = Math.sqrt(Math.pow(vec1.x - currVert.x, 2) +
                                                                    Math.pow(vec1.y - currVert.y, 2) +
                                                                    Math.pow(vec1.z - currVert.z, 2));
                                        double unrolledDistance = vertDistances[v - 1];
                                        
                                        //System.out.println("    - unrolledDistance: " + unrolledDistance + "  distance: " + distance + " pd " + prevDistance );
                                        //System.out.println("    - currVert x " + currVert.x + " 1 " + vec1.x );
                                        //System.out.println("    - currVert y " + currVert.y + " 1 " + vec1.y );
                                        //System.out.println("    - currVert z " + currVert.z + " 1 " + vec1.z );
                                        
                                        if(distance > unrolledDistance * 4){ // not good, comparing prevUnrolled with dist
                                            drillLowered = false;
                                            drillLowerDistance = 0.5;
                                            //System.out.println(" Lift ");
                                            splitPoly = true;
                                            //
                                            //Vec3 vertInsert = new Vec3(previousVert.x, previousVert.y + drillLowerDistance, previousVert.z); // raise drill
                                            Vec3 vertInsert = new Vec3(currVert.x, currVert.y + drillLowerDistance, currVert.z);
                                            vertInsert.f = fastRate;
                                            polygon.addElement(vertInsert);
                                        
                                            System.out.println(" ^ ");
                                            polygon.addElement( new Vec3( vec0.x, vec0.y, vec0.z, fastRate) ); // connect curve to start (up)
                                            
                                        } else {
                                            System.out.println(" | ");
                                            polygon.addElement( new Vec3( vec1.x, vec1.y, vec1.z, slowRate) ); // connect (dn)
                                        }
                                        
                                        // Connect poly raise
                                        //drillLowered = false;
                                        //drillLowerDistance = 0.5;
                                        //double y = cutDepth * scale; // recalculate y
                                        //Vec3 vertConnectRaise = polygon.elementAt(0); // new Vec3( vertConnect.x, vertConnect.y + drillLowerDistance, vertConnect.z); // raise drill
                                        //polygon.addElement(vertConnectRaise);
                                        
                                        polygon.addElement( new Vec3( vec0.x, vec0.y, vec0.z, fastRate) ); // where started (up)
                                    }
                                    
                                    double x = vert.x; // + origin.x;
                                    double y = vert.y; // + origin.y;
                                    double z = vert.z; // + origin.z;
                                    if(x < minX){
                                        minX = x;
                                    }
                                    if(x > maxX){
                                        maxX = x;
                                    }
                                    
                                    if(y < minY){
                                        minY = y;
                                    }
                                    if(y > maxY){
                                        maxY = y;
                                    }
                                    
                                    if(z < minZ){
                                        minZ = z;
                                    }
                                    if(z > maxZ){
                                        maxZ = z;
                                    }
                                    
                                    v++;
                                }
                                // Reverse order
                                if(layout.getReverseOrder(child.getId() + "") == 1){
                                    Collections.reverse(polygon);
                                }
                                
                                //polygons.addElement(polygon);
                                
                                // Cycle polygon start.
                                // Allows cutting faccit deteail before long lines.
                                // polygons (Vector)
                                int start_offset = child.getCncPointOffset(); // layout.getPointOffset(child.getId() + "");
                                //System.out.println(" *** start_offset: " + start_offset);
                                //for(int pt = 0; pt < polygon.size(); pt++){
                                //}
                                while(start_offset > polygon.size() - 1){
                                    start_offset = start_offset - polygon.size();
                                }
                                while(start_offset < 0){
                                    start_offset = start_offset + polygon.size();
                                }
                                if(start_offset != 0 && polygon.size() > 0){
                                    for(int i = 0; i < start_offset; i++){
                                        Vec3 vert = (Vec3)polygon.elementAt(0);
                                        polygon.remove(0);
                                        polygon.add(vert);
                                    }
                                }
                                
                                // Insert this polygon into the correct sorted order.
                                
                                int polyOrder = child.getCncPolyOrder(); // layout.getPolyOrder( "" + child.getId() );
                                int defaultPolyOrder = child.getDefaultPolyOrder(); //
                                polygonOrder.put(polygon, polyOrder);
                                System.out.println("  ****  " + child.getId() + "  polyOrder: " + polyOrder + " default order: " + defaultPolyOrder );
                                
                                int insertAtPos = 0;
                                boolean insertAtEnd = false;
                                
                                for(int pos = 0; pos < polygons.size(); pos++){
                                    Vector poly = (Vector)polygons.elementAt(pos);
                                    if(poly != null && polygonOrder != null){
                                        Object currPolyPos = (Object)polygonOrder.get(poly);
                                        if(currPolyPos != null){
                                            int currPolyPosInt = ((Integer)currPolyPos).intValue();
                                            
                                            // If current poly order is less than the current in the existing list insert it before
                                            if( polyOrder < currPolyPosInt ){
                                                insertAtPos = pos - 0;
                                                pos = polygons.size() + 1; // break
                                                //break;
                                            }
                                        }
                                    }
                                }
                                
                                if(polygons.size() > 0){
                                    Vector lastPoly = (Vector)polygons.elementAt(polygons.size()-1);
                                    Object lastPolyPos =  (Object)polygonOrder.get(lastPoly);
                                    if(lastPolyPos != null){
                                        int lastPolyPosInt = ((Integer)lastPolyPos).intValue();
                                        if(polyOrder > lastPolyPosInt){
                                            insertAtEnd = true;
                                        }
                                    }
                                }
                                
                                //System.out.println("  **** !!!!!!!!  " + child.getId() + "  insertAtPos: " + insertAtPos );
                                
                                if(insertAtEnd){
                                    polygons.addElement(polygon);
                                } else {
                                    //if(insertAtPos > 0){
                                    //    System.out.println("insert at " + insertAtPos);
                                    polygons.insertElementAt(polygon, insertAtPos);
                                    //} else {
                                    //    System.out.println("add element");
                                    //    polygons.addElement(polygon);
                                    //}
                                }
                                
                            }
                        }
                    } // end if mesh and visible
                    
                    /*
                    if(co instanceof Curve && objClone.isVisible() == true ){ // Bent tube.
                        
                        CoordinateSystem c;
                        c = layout.getCoords(objClone); // Read cutting coord from file
                        objClone.setCoords(c);
                        
                        Curve curve = (Curve) objClone.getObject(); // Object3D
                        Vec3 [] verts = curve.getVertexPositions();
                        //for (Vec3 vert : verts){
                            
                        //}
                        
                        // TODO: Bend child objects (notches) around curve of tube vertecies.
                        for (int i = 0; i < verts.length - 2; i++ ){
                            Vec3 vert = verts[i];
                            Vec3 vert2 = verts[i+1];
                            Vec3 vert3 = verts[i+2];
                            
                            float firstPair = scene.getAngle( vert.x - vert2.x, vert.z - vert2.z );
                            float secondPair = scene.getAngle( vert2.x - vert3.x, vert2.z - vert3.z );
                            float angle = Math.abs(secondPair - firstPair);
                            
                            double distance = Math.sqrt(Math.pow(vert.x - vert2.x, 2) + Math.pow(vert.y - vert2.y, 2) + Math.pow(vert.z - vert2.z, 2));
                            distance = distance * scale;
                            
                            System.out.println(" " + i + " d: " + distance + " a: " + angle );
                            
                            
                            
                        }
                        
                    }
                     */
                    
                    // TODO: order polygons
                    
                    for(int p = 0; p < polygons.size(); p++){
                        //System.out.println(" POLYGON ***");
                        gcode2 += "; Polygon \n";
                        //gcode2 += "M4;\n";
                        
                        Vector polygon = (Vector)polygons.elementAt(p);
                        boolean lowered = false;
                        double prevHeight = 99;
                        Vec3 firstPoint = null;
                        for(int pt = 0; pt < polygon.size(); pt++){
                            Vec3 point = (Vec3)polygon.elementAt(pt);
                            //System.out.println("  Point *** " + point.getX() + " " + point.getY());
                            
                            point.x = (point.x + -minX); // shift to align all geometry to 0,0   boundsMinX
                            point.z = (point.z + -minZ); //
                            //point.z = (point.z + -minZ);
                            
                            // start spindle
                            // M3 S4000     ; start spindle
                            // M5           ; stop spindle
                            
                            if (point.y == 0 && prevHeight != point.y){
                                gcode2 += "M4;\n";  // enable torch
                            }
                            
                            gcode2 += "G1 X" +
                            scene.roundThree(point.x) +
                            " Y" +
                            scene.roundThree( point.z * widthScale ) +
                            " Z" +
                            scene.roundThree(point.y);
                            if(point.f > 0){
                                gcode2 += " F"+point.f+"";
                            }
                            gcode2 += ";\n"; // End line
                            
                            //
                            //gcode2 += "M3;\n"; // Start spindle
                            //gcode2 += "M5;\n"; // Stop spindle
                            
                            if (point.y > 0 && prevHeight != point.y){
                                gcode2 += "M5;\n";  // disable torch
                            }
                            
                            if(!lowered){
                                //gcode2 += "G00 Z-0.5 F"+slowRate+";\n"; // Lower router head for cutting.
                                lowered = true;
                                firstPoint = point;
                            }
                            
                            polygon.setElementAt(point, pt);
                            prevHeight = point.y;
                        }
                        gcode2 += "M5;\n";
                    } // for polygons
                    
                    //System.out.println("Width: " + (maxX - minX) + " Height: " + (maxZ - minZ));
                    //System.out.println("Align: x: " + -minX + " y: " + -minZ);
                    
                    
                    // Write gcode to file
                    if(writeFile){
                        try {
                            String gcodeFile = dir + System.getProperty("file.separator") + obj.getName() + ".gcode";
                            //gcodeFile += ".gcode";
                            System.out.println("Writing g code file: " + gcodeFile);
                            PrintWriter writer2 = new PrintWriter(gcodeFile, "UTF-8");
                            writer2.println(gcode2);
                            writer2.close();
                        } catch (Exception e){
                            System.out.println("Error: " + e.toString());
                        }
                        
                        // Multi part file
                        /*
                        String gcode3 = gcode2;
                        int lines = 0; // StringUtils.countMatches(gcode2, "\n");
                        for(int i = 0; i < gcode3.length(); i++){
                            if(gcode3.charAt(i) == '\n'){
                                lines++;
                            }
                        }
                        if(lines > 499){
                            int lineNumber = 0;
                            int fileNumber = 1;
                            lines = 0;
                            for(int i = 0; i < gcode3.length(); i++){
                                if(gcode3.charAt(i) == '\n'){
                                    lines++;
                                    if(lines > 480){
                                        String gCodeSection = gcode3.substring(0, i);
                                        
                                        String gcodeFile = dir + System.getProperty("file.separator") + obj.getName() + "_" + fileNumber;
                                        gcodeFile += ".gcode";
                                        System.out.println("Writing g code file: " + gcodeFile);
                                        PrintWriter writer2 = new PrintWriter(gcodeFile, "UTF-8");
                                        writer2.println(gCodeSection);
                                        writer2.close();
                                        
                                        fileNumber++;
                                        gcode3 = gcode3.substring(i+1, gcode3.length());
                                    }
                                }
                            }
                            String gcodeFile = dir + System.getProperty("file.separator") + obj.getName() + "_" + fileNumber;
                            gcodeFile += ".gcode";
                            System.out.println("Writing g code file: " + gcodeFile);
                            PrintWriter writer2 = new PrintWriter(gcodeFile, "UTF-8");
                            writer2.println(gcode3);
                            writer2.close();
                            System.out.println(" Lines *** " + lines);
                        }
                         */
                    }
                
                } catch (Exception e){
                    System.out.println("Error: " + e);
                    e.printStackTrace();
                }
    
            }
        } // scene object loop
    }
}


