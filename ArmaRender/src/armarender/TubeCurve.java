/* Copyright (C) 2021, 2023 by Jon Taylor

 */

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
import armarender.texture.*;
import armarender.object.TriangleMesh.*;
import java.math.RoundingMode;

public class TubeCurve {
    //Scene theScene;
    LayoutWindow layoutWindow;
    
    public TubeCurve(LayoutWindow layoutWindow){
        this.layoutWindow = layoutWindow;
    }

    /**
     * getTubeFromArc
     *
     * Description: Generate tube fron profile arc line.
     */
    public void getTubeFromArc(Scene theScene){
        System.out.println("getTubeFromArc");
        LayoutModeling layout = new LayoutModeling();
        if(theScene != null){
            
            
        }
    }
    
    /**
     * getTubeFromCurve
     *
     * Description: Generate tube fron profile curve with a circumfrance guide curve as a child object.
     *
     * @param: Scene -
     */
    public void getTubeFromCurve(Scene theScene){
        System.out.println("getCurveFromTube");
        LayoutModeling layout = new LayoutModeling();
        if(theScene != null){
            int sel[] = theScene.getSelection();
            for(int selectionIndex = 0; selectionIndex < sel.length; selectionIndex++){
                
                ObjectInfo info = theScene.getObject(sel[selectionIndex]);
                Object3D obj;
                obj = info.getObject();
                System.out.println("ObjectInfo Type: "+ obj.getClass().getName());
                CoordinateSystem c;
                if((obj instanceof Curve) == true){
                    System.out.println("Curve ");
                    Vec3 prifilePoints[] = ((Curve)obj).getVertexPositions();
                    System.out.println("prifilePoints: " + prifilePoints.length);
                    
                    // Translate profile points to world coordinates.
                    
                    // Find circumfrance reference curve at one of the points.
                    ObjectInfo[] children = info.getChildren();
                    for(int j = 0; j < children.length; j++){
                        ObjectInfo childCircumfrance = children[j];
                        if(childCircumfrance.getObject() instanceof Curve){
                            //Curve circ = (Curve)
                            // Get childCircumfrance location Vec
                            Vec3 circumfranceRed = new Vec3();
                            
                            // Find curve profile closest point this child is closest too.
                            //Vec3 prifilePoints[] = ((Curve)childCircumfrance.getObject()).getVertexPositions();
                            //System.out.println("prifilePoints: " + prifilePoints.length);
                            
                        }
                    }
                }
            }
        }
    }
    
    public class PointPair {
        String key = "";
        Vec3 a;
        Vec3 b;
        Vec3 mid;
        double dist;
        int aIndex = -1;
        int bIndex = -1;
        public PointPair(Vec3 a, Vec3 b, int aIndex, int bIndex, String key){
            this.a = a;
            this.b = b;
            this.aIndex = aIndex;
            this.bIndex = bIndex;
            this.mid = a.midPoint(b);
            this.key = key;
            this.dist = a.distance(b);
        }
    }
    
    /**
     * getCurveFromTubeMesh
     *
     * Description:
     */
    public void getCurveFromTubeMesh(Scene theScene){
        LayoutModeling layout = new LayoutModeling();
        if(theScene != null){
            int sel[] = theScene.getSelection();
            for(int selectionIndex = 0; selectionIndex < sel.length; selectionIndex++){
                Vec3[] centreLine = null;
                Vector<Vec3> clusterPoints = new Vector<Vec3>();
                Vector<Vec3> tubePoints = new Vector<Vec3>();
                Vector<Vec3> centreLineVec = new Vector<Vec3>();
                ObjectInfo info = theScene.getObject(sel[selectionIndex]);
                Object3D obj;
                obj = info.getObject();
                //System.out.println("ObjectInfo Type: "+ obj.getClass().getName());
                CoordinateSystem c;
                c = layout.getCoords(info);
                TriangleMesh triangleMesh = null;
                if((obj instanceof TriangleMesh) == true){
                    triangleMesh = (TriangleMesh)obj;
                }
                if(info.getObject().canConvertToTriangleMesh() != Object3D.CANT_CONVERT){
                    triangleMesh = obj.duplicate().convertToTriangleMesh(0.05);
                }
                
                //if(obj.canConvertToTriangleMesh() != Object3D.CANT_CONVERT && (obj instanceof TriangleMesh) == true ){
                if(triangleMesh != null){
                    Vec3 [] vertPos = triangleMesh.getVertexPositions();
                    MeshVertex[] verts = triangleMesh.getVertices();
                    //System.out.println(" verts " + verts.length);
                    
                    Edge edges[] = triangleMesh.getEdges();
                    //System.out.println(" edges " + edges.length);
                    //for (Vec3 vert : vertPos){
                    for(MeshVertex mv : verts){
                        Vec3 vert = mv.r;
                        Mat4 mat4 = c.duplicate().fromLocal();
                        mat4.transform(vert);
                        mv.r = vert;
                        //System.out.println(" point: " + vert.x + " " + vert.y + " " + vert.z);
                        tubePoints.addElement(vert); // depricate
                    }
                    
                    // Get sizing for heuristic. Not min or max to every point, only neighbour.
                    double minDistance = 999999;
                    double maxDistance = 0;
                    for(int e = 0; e < edges.length; e++){
                        Edge edge = edges[e];
                        if(verts.length > edge.v1 &&  verts.length > edge.v2 && edge.v1 > -1 && edge.v2 > -1){
                            MeshVertex a = verts[edge.v1];
                            MeshVertex b = verts[edge.v2];
                            Vec3 vecA = a.r;
                            Vec3 vecB = b.r;
                            double distance = vecA.distance(vecB);
                            if(distance > maxDistance){
                                maxDistance = distance;
                            }
                            if(distance < minDistance){
                                minDistance = distance;
                            }
                        }
                    }
                    //double sameMidRange = minDistance / 25; // Distance mid points can vary and still be considered the same.
                    
                    
                    // Strip out duplicate points. Not needed
                    Vector <Vec3>points = new Vector<Vec3>();
                    HashMap<String, Vec3> pointKeys = new HashMap<String, Vec3>();
                    for(int p = 0; p < vertPos.length; p++){
                        Vec3 point = vertPos[p];
                        String key = point.x + "_" + point.y + "_" + point.z;
                        if( pointKeys.get(key) == null ){
                            pointKeys.put(key, point);
                            points.addElement(point);
                            //System.out.println(" *** ");
                        } else {
                            //System.out.println(" --- ");
                        }
                    }
                    //System.out.println(" points " + points.size() + " - " + vertPos.length);
                    
                    
                    // Calculate Point Pairs
                    // get mid points of all possible pairs.
                    HashMap<String, PointPair> pointPairs = new HashMap<String, PointPair>();
                    for(int p = 0; p < vertPos.length; p++){
                        for(int q = 0; q < vertPos.length; q++){
                            if(p != q){
                                int min = Math.min(p, q);
                                int max = Math.max(p, q);
                                String key = min + "_" + max;
                                Vec3 a = vertPos[p];
                                Vec3 b = vertPos[q];
                                if(a.distance(b) > 0 ){
                                    PointPair pair = new PointPair( a, b, p, q, key );
                                    pointPairs.put(key, pair);
                                }
                            }
                        }
                    }
                    //System.out.println("pointPairs: " + pointPairs.size());
                    
                    
                    //
                    // for each point (not used) follow connecting edges such that:
                    //  1) each edge length is close to the same.
                    //  2) the connected edges eventually return to the start point.
                    //
                    double outerDiameter = 0;
                    double innerDiameter = 99999;
                    double minOuterDiameter = 9999999;          // We want the smallest outerDiameter found.
                    int vind = 0;
                    Vector<Vec3> excludePoints = new Vector<Vec3>();
                    Vector<Vec3> centreLinePoints = new Vector<Vec3>();
                    for(MeshVertex mv : verts){
                        Vec3 currPoint = mv.r;
                        
                        if(excludePoints.contains(currPoint) == false){
                            //Vec3 currPoint = (Vec3)points.elementAt(p);
                            Vector<Vec3> usedPoints = new Vector<Vec3>();
                            Vector<Edge> usedEdges = new Vector<Edge>();
                            
                            //
                            Vector<Vec3> eds = followEdges(currPoint, currPoint, verts, edges, usedPoints, usedEdges, -1, 0); //
                            eds = removeDuplicates(eds, 0.0001);
                            //System.out.println(" used points " + eds.size() );
                            if(eds.size() > 1){
                                // Calculate centre
                                Vec3 centrePoint = calculateCentrePoint(eds);
                                
                                // Calculate outer and inner diameter from points.
                                double diameter = calculateDiameter(eds);
                                //System.out.println("diameter: " + diameter);
                                if(diameter > outerDiameter && diameter < minOuterDiameter){
                                    outerDiameter = diameter;
                                    minOuterDiameter = diameter;        // Some edges with notches (longer loops)
                                }
                                if(diameter < innerDiameter){
                                    innerDiameter = diameter;
                                }
                                
                                // Only add one centre point
                                centreLinePoints.addElement(centrePoint);
                                //System.out.println("  start p: " + currPoint );
                                
                                excludePoints.addAll(eds); // store returned points and avoid processing them again.
                            
                                // create centre line
                                /*
                                centreLine = new Vec3[eds.size()];
                                for(int i = 0; i < eds.size(); i++){
                                    Vec3 centreVec = eds.elementAt(i);
                                    centreLine[i] = centreVec;
                                    //System.out.println(" centre " + centreVec.x + " " + centreVec.y + " " + centreVec.z);
                                }
                                Curve tubeCurve = getCurve(centreLine);
                                CoordinateSystem coords = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
                                ObjectInfo centreCurveOI = new ObjectInfo(tubeCurve, coords, "DEBUG"+ vind);
                                UndoRecord undo = new UndoRecord(layoutWindow, false, UndoRecord.DELETE_OBJECT, new Object [] {new Integer(layoutWindow.getScene().getNumObjects()-1)});
                                layoutWindow.addObject(centreCurveOI, undo);
                                //layoutWindow.setSelection(layoutWindow.getScene().getNumObjects()-1);
                                layoutWindow.setUndoRecord(  undo  );
                                layoutWindow.updateImage();
                                layoutWindow.updateTree();
                                 */
                            }
                        
                        }
                        
                        vind++;
                    }
                    // Remove duplicates. (OD and ID will have the same point)
                    centreLinePoints = removeDuplicates(centreLinePoints, 0.0001);
                    
                    
                    //
                    // Find centre point for ends with non flat faces. Tubes with angle cuts or notches need special parsing to extract the centre point.
                    // For each point that has been un used in the previous process.
                    // 1) each point must be unused from previous loop routine
                    // 2) the connected edges eventually return to the start point.
                    //
                    HashMap<Vec3, EndCenterPointContainer> endCenterPointMap = new HashMap<Vec3, EndCenterPointContainer>();
                    HashMap<Vec3, Vector<EndCenterPointContainer>> endCenterPointOptionsMap = new HashMap<Vec3, Vector<EndCenterPointContainer>>();
                    Vector<Vec3> tempPoints = new Vector<Vec3>();
                    for(MeshVertex mv : verts){
                        Vec3 currPoint = mv.r;
                        if(excludePoints.contains(currPoint) == false){
                            tempPoints.addElement(currPoint);
                            // For each point find an edge with this point
                            for(int e = 0; e < edges.length; e++){
                                Edge edge = edges[e];
                                MeshVertex a = verts[edge.v1];
                                MeshVertex b = verts[edge.v2];
                                Vec3 vecA = a.r;
                                Vec3 vecB = b.r;
                                Vec3 outerVec = null;
                                Vec3 innerVec = null;
                                if(vecA == currPoint){
                                    outerVec = vecA;
                                    innerVec = vecB;
                                }
                                if(vecB == currPoint){
                                    outerVec = vecB;
                                    innerVec = vecA;
                                }
                                // Find closest center point to translate edge.
                                if(innerVec != null && outerVec != null){
                                    //System.out.println("innerVec " + innerVec + " out  " + outerVec);
                                    double edgeLength = innerVec.distance(outerVec);
                                    
                                    for(int cc = 0; cc < centreLinePoints.size(); cc++){
                                        Vec3 currCentrePoint = centreLinePoints.elementAt(cc);
                                        double distance = innerVec.distance(currCentrePoint);  // Issue innerVec can be ID or OD from the tube geometry?
                                        if(distance * 1.5 < minOuterDiameter){
                                            //System.out.println(" Found centre " + currCentrePoint);
                                            Vec3 translateVec = innerVec.minus(currCentrePoint);
                                            Vec3 calculatedPoint = new Vec3(outerVec);
                                            calculatedPoint.subtract(translateVec);
                                
                                            //System.out.println("  calculatedPoint: " + calculatedPoint + "  edgeLength: " + edgeLength);
                                            EndCenterPointContainer endCenter = endCenterPointMap.get(currCentrePoint);
                                            if(endCenter == null){                                  // Add first
                                                endCenter = new EndCenterPointContainer();
                                                endCenter.edgeLength = edgeLength;
                                                endCenter.calculatedCenterPoint = calculatedPoint;
                                                endCenter.outerVec = outerVec; // Debug
                                                endCenter.innerVec = innerVec; // Debug
                                                endCenter.existCenter = currCentrePoint; // Debug
                                                endCenter.translateDist = distance;
                                                endCenterPointMap.put(currCentrePoint, endCenter);
                                            } else {                                                // Update existing
                                                if(endCenter.edgeLength < edgeLength){
                                                    endCenter.edgeLength = edgeLength;
                                                    endCenter.calculatedCenterPoint = calculatedPoint;
                                                    endCenter.outerVec = outerVec; // Debug
                                                    endCenter.innerVec = innerVec; // Debug
                                                    endCenter.existCenter = currCentrePoint; // Debug
                                                    endCenter.translateDist = distance;
                                                    endCenterPointMap.put(currCentrePoint, endCenter);
                                                }
                                            }
                                            
                                            Vector<EndCenterPointContainer> centerPoints = endCenterPointOptionsMap.get(currCentrePoint);
                                            if(centerPoints == null){
                                                centerPoints = new Vector<EndCenterPointContainer>();
                                            }
                                            
                                            // Get count
                                            int count = 0;
                                            for(int xx = 0; xx < centerPoints.size(); xx++){
                                                EndCenterPointContainer currEndCenterPointContainer = centerPoints.elementAt(xx);
                                                double dist = currEndCenterPointContainer.calculatedCenterPoint.distance(calculatedPoint);
                                                //System.out.println(" - count dist " + dist + " minDistance: " + minDistance); // 0.006
                                                if(dist <= minDistance * 10){ // 0.08 0.0001 Heuristic is not great.
                                                    count += 1;
                                                }
                                            }
                                            
                                            endCenter = new EndCenterPointContainer();
                                            endCenter.edgeLength = edgeLength;
                                            endCenter.calculatedCenterPoint = calculatedPoint;
                                            endCenter.outerVec = outerVec; // Debug
                                            endCenter.innerVec = innerVec; // Debug
                                            endCenter.existCenter = currCentrePoint; // Debug
                                            endCenter.translateDist = distance;
                                            endCenter.count = count;
                                            endCenter.distToCenter = calculatedPoint.distance(innerVec);
                                            centerPoints.addElement(endCenter);
                                            endCenterPointOptionsMap.put(currCentrePoint, centerPoints);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Find longest distance with at least 2 count (having multiple locations.)
                    // This only works if the edges in the tube geometry are not torsionally rotated.
                    for (Map.Entry<Vec3, Vector<EndCenterPointContainer>> set : endCenterPointOptionsMap.entrySet()) {
                        Vec3 existingCentre = set.getKey();
                        Vector<EndCenterPointContainer> prospectPoints = set.getValue();
                        double farthestDist = 0;
                        EndCenterPointContainer farthestEndCenterPointContainer = null;
                        for(int pp = 0; pp < prospectPoints.size(); pp++){
                            EndCenterPointContainer endCenterPointContainer = prospectPoints.elementAt(pp);
                            //System.out.println(" ---  " + endCenterPointContainer.count + " dist " + endCenterPointContainer.distToCenter );
                            if(endCenterPointContainer.count > 2 && endCenterPointContainer.distToCenter > farthestDist){
                                farthestDist = endCenterPointContainer.distToCenter;
                                farthestEndCenterPointContainer = endCenterPointContainer;
                            }
                        }
                        if(farthestEndCenterPointContainer != null){
                            // Extend ArcTube on end for margin by 1/8 of the tube diameter.
                            Vec3 extendVector = new Vec3(farthestEndCenterPointContainer.existCenter);
                            extendVector.subtract(farthestEndCenterPointContainer.calculatedCenterPoint);
                            double scaleExtend = (outerDiameter / 8) / farthestEndCenterPointContainer.distToCenter;
                            extendVector = extendVector.times(scaleExtend);
                            Vec3 extendedCenterPoint = new Vec3(farthestEndCenterPointContainer.calculatedCenterPoint);
                            extendedCenterPoint.subtract(extendVector);
                            
                            Vec3 firstCenter = centreLinePoints.elementAt(0);
                            Vec3 lastCenter = centreLinePoints.elementAt( centreLinePoints.size() - 1 );
                            double firstDist = farthestEndCenterPointContainer.calculatedCenterPoint.distance(firstCenter);
                            double lastDist = farthestEndCenterPointContainer.calculatedCenterPoint.distance(lastCenter);
                            if(firstDist < lastDist){                                               // Add to front
                                
                                centreLinePoints.insertElementAt(extendedCenterPoint, 0);
                            } else {                                                                // add to end
                                
                                centreLinePoints.insertElementAt(extendedCenterPoint, centreLinePoints.size());
                            }
                            //System.out.println(" extendedCenterPoint:  " + extendedCenterPoint);
                        }
                    }
                    
                    
                    // Order centre line such that the distances are minimized. Set to in order.
                    centreLinePoints = orderPoints(centreLinePoints);
                    
                    // Add centre line to scene.
                    // centreLinePoints
                    centreLine = new Vec3[centreLinePoints.size()];
                    for(int i = 0; i < centreLinePoints.size(); i++){
                        Vec3 centreVec = centreLinePoints.elementAt(i);
                        centreLine[i] = centreVec;
                        //System.out.println(" centre " + centreVec.x + " " + centreVec.y + " " + centreVec.z);
                    }
                    Curve tubeCurve = getCurve(centreLine);
                    //ArcObject tubeCurve = getArc(centreLinePoints, outerDiameter, (outerDiameter-innerDiameter) / 2, bendradius);
                    CoordinateSystem coords = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
                    ObjectInfo centreCurveOI = new ObjectInfo(tubeCurve, coords, info.getName() + "-Curve" + vind);
                    UndoRecord undo = new UndoRecord(layoutWindow, false, UndoRecord.DELETE_OBJECT, new Object [] {new Integer(layoutWindow.getScene().getNumObjects()-1)});
                    layoutWindow.addObject(centreCurveOI, undo);
                    layoutWindow.setSelection(layoutWindow.getScene().getNumObjects()-1);
                    layoutWindow.setUndoRecord(  undo  );
                    layoutWindow.updateImage();
                    layoutWindow.updateTree();
                    
                    Sound.playSound("success.wav");
                    
                } else {
                    System.out.println("Warming: Unsuported object type selected.");
                    Sound.playSound("deny.wav");
                }
            }
        }
    }
    
    /**
     * getCurveFromTube
     *
     * Description: Calculate centre curve from tube geometry.
     *      Buggy when segments are too close together.
     *
     *   TODO: Add notches from tube geometry (This code exists in NotchIntersections.addNotchesFromGeometry2() )
     *
     *  @param: Scene - Scene object
     */
    public void getArcFromTubeMesh(Scene theScene){
        LayoutModeling layout = new LayoutModeling();
        if(theScene != null){
            
            // Get time. If this routine takes too long then print status for debuging.
            double startTime = System.currentTimeMillis();
            
            int sel[] = theScene.getSelection();
            for(int selectionIndex = 0; selectionIndex < sel.length; selectionIndex++){
                Vec3[] centreLine = null;
                Vector<Vec3> clusterPoints = new Vector<Vec3>();
                Vector<Vec3> tubePoints = new Vector<Vec3>();
                Vector<Vec3> centreLineVec = new Vector<Vec3>();
                ObjectInfo info = theScene.getObject(sel[selectionIndex]);
                Object3D obj;
                obj = info.getObject();
                //System.out.println("ObjectInfo Type: "+ obj.getClass().getName());
                CoordinateSystem c;
                c = layout.getCoords(info);
                TriangleMesh triangleMesh = null;
                if((obj instanceof TriangleMesh) == true){
                    triangleMesh = (TriangleMesh)obj;
                }
                if(info.getObject().canConvertToTriangleMesh() != Object3D.CANT_CONVERT){
                    triangleMesh = obj.duplicate().convertToTriangleMesh(0.05);
                }
                
                if(triangleMesh != null){
                    Vec3 [] vertPos = triangleMesh.getVertexPositions();
                    MeshVertex[] verts = triangleMesh.getVertices();
                    Edge edges[] = triangleMesh.getEdges();
                    //System.out.println(" edges " + edges.length);
                    //for (Vec3 vert : vertPos){
                    for(MeshVertex mv : verts){
                        Vec3 vert = mv.r;
                        Mat4 mat4 = c.duplicate().fromLocal();
                        mat4.transform(vert);
                        mv.r = vert;
                        //System.out.println(" point: " + vert.x + " " + vert.y + " " + vert.z);
                        tubePoints.addElement(vert); // depricate
                    }
                    
                    // Get sizing for heuristic. Not min or max to every point, only neighbour.
                    double minDistance = 999999;
                    double maxDistance = 0;
                    for(int e = 0; e < edges.length; e++){
                        Edge edge = edges[e];
                        if(verts.length > edge.v1 &&  verts.length > edge.v2 && edge.v1 > -1 && edge.v2 > -1){
                            MeshVertex a = verts[edge.v1];
                            MeshVertex b = verts[edge.v2];
                            Vec3 vecA = a.r;
                            Vec3 vecB = b.r;
                            double distance = vecA.distance(vecB);
                            if(distance > maxDistance){
                                maxDistance = distance;
                            }
                            if(distance < minDistance){
                                minDistance = distance;
                            }
                        }
                    }
                    //double sameMidRange = minDistance / 25; // Distance mid points can vary and still be considered the same.
                    
                    
                    // Strip out duplicate points. Not needed
                    /*
                    Vector <Vec3>points = new Vector<Vec3>();
                    HashMap<String, Vec3> pointKeys = new HashMap<String, Vec3>();
                    for(int p = 0; p < vertPos.length; p++){
                        Vec3 point = vertPos[p];
                        String key = point.x + "_" + point.y + "_" + point.z;
                        if( pointKeys.get(key) == null ){
                            pointKeys.put(key, point);
                            points.addElement(point);
                            //System.out.println(" *** ");
                        } else {
                            //System.out.println(" --- ");
                        }
                    }
                    //System.out.println(" points " + points.size() + " - " + vertPos.length);
                    */
                   
                    //
                    // Find ArcTube center line points.
                    // for each point (not used) follow connecting edges such that:
                    //  1) each edge length is close to the same.
                    //  2) the connected edges eventually return to the start point.
                    //
                    double outerDiameter = 0;
                    double innerDiameter = 99999;
                    double minOuterDiameter = 9999999;          // We want the smallest outerDiameter found.
                    int vind = 0;
                    Vector<Vec3> excludePoints = new Vector<Vec3>();
                    Vector<Vec3> centreLinePoints = new Vector<Vec3>();
                    for(MeshVertex mv : verts){
                        Vec3 currPoint = mv.r;
                        if(excludePoints.contains(currPoint) == false){
                            Vector<Vec3> usedPoints = new Vector<Vec3>();
                            Vector<Edge> usedEdges = new Vector<Edge>();
                            Vector<Vec3> eds = followEdges(currPoint, currPoint, verts, edges, usedPoints, usedEdges, -1, 0);
                            eds = removeDuplicates(eds, 0.0001);
                            if(eds.size() > 1){
                                
                                //double currTime = System.currentTimeMillis();
                                //System.out.println("Found center line points in time: "  + (currTime - startTime ) + "  points: " + centreLinePoints.size());
                                
                                // Calculate centre
                                Vec3 centrePoint = calculateCentrePoint(eds);
                                
                                // Calculate outer and inner diameter from points.
                                double diameter = calculateDiameter(eds);
                                if(diameter > outerDiameter && diameter < minOuterDiameter){
                                    outerDiameter = diameter;
                                    minOuterDiameter = diameter;        // Some edges with notches (longer loops) can't determine diameter accurately.
                                    //System.out.println(" --- " + eds.size() + " outerDiameter: " + outerDiameter);
                                }
                                if(diameter < innerDiameter){
                                    innerDiameter = diameter;
                                }
                                
                                // Keep the centre point for the curve profile.
                                centreLinePoints.addElement(centrePoint);
                                
                                excludePoints.addAll(eds); // store returned points and avoid processing them again.
                            
                                // Debug draw line where centre points are detected.
                                if(false){
                                    centreLine = new Vec3[eds.size()];
                                    for(int i = 0; i < eds.size(); i++){
                                        Vec3 centreVec = eds.elementAt(i);
                                        centreLine[i] = centreVec;
                                    }
                                    Curve tubeCurve = getCurve(centreLine);
                                    CoordinateSystem coords = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
                                    ObjectInfo centreCurveOI = new ObjectInfo(tubeCurve, coords, "DEBUG" + vind);
                                    UndoRecord undo = new UndoRecord(layoutWindow, false, UndoRecord.DELETE_OBJECT, new Object [] {new Integer(layoutWindow.getScene().getNumObjects()-1)});
                                    layoutWindow.addObject(centreCurveOI, undo);
                                    //layoutWindow.setSelection(layoutWindow.getScene().getNumObjects()-1);
                                    layoutWindow.setUndoRecord(  undo  );
                                    layoutWindow.updateImage();
                                    layoutWindow.updateTree();
                                }
                            }
                        }
                        vind++;
                    }
                    
                    // Remove duplicates. (OD and ID will have the same point)
                    centreLinePoints = removeDuplicates(centreLinePoints, 0.0001);
                    
                    // Order centre line such that the distances are minimized. Set to in order.
                //    centreLinePoints = orderPoints(centreLinePoints);
                    
                    // Get bend radius from point data.
                //    double bendradius = getArcBendRadius(centreLinePoints, outerDiameter);
                    
                    // Simplify centre line.
                    // For shifting set of 5 points, can middle three (2,3,4) be replaced by new point intersection between 1,2 and 4,5.
                 //   centreLinePoints = simplifyArcProfile(centreLinePoints, outerDiameter);
                    
                    
                    //double currTime = System.currentTimeMillis();
                    //System.out.println("Found center line points in time: "  + (currTime - startTime) + "  points: " + centreLinePoints.size());
                    
                    
                    //
                    // Find centre point for ends with non flat faces. Tubes with angle cuts or notches need special parsing to extract the centre point.
                    // For each point that has been un used in the previous process.
                    // 1) each point must be unused from previous loop routine
                    // 2) the connected edges eventually return to the start point.
                    //
                    HashMap<Vec3, EndCenterPointContainer> endCenterPointMap = new HashMap<Vec3, EndCenterPointContainer>();
                    HashMap<Vec3, Vector<EndCenterPointContainer>> endCenterPointOptionsMap = new HashMap<Vec3, Vector<EndCenterPointContainer>>();
                    Vector<Vec3> tempPoints = new Vector<Vec3>();
                    for(MeshVertex mv : verts){
                        Vec3 currPoint = mv.r;
                        if(excludePoints.contains(currPoint) == false){
                            tempPoints.addElement(currPoint);
                            // For each point find an edge with this point
                            for(int e = 0; e < edges.length; e++){
                                Edge edge = edges[e];
                                MeshVertex a = verts[edge.v1];
                                MeshVertex b = verts[edge.v2];
                                Vec3 vecA = a.r;
                                Vec3 vecB = b.r;
                                Vec3 outerVec = null;
                                Vec3 innerVec = null;
                                if(vecA == currPoint){
                                    outerVec = vecA;
                                    innerVec = vecB;
                                }
                                if(vecB == currPoint){
                                    outerVec = vecB;
                                    innerVec = vecA;
                                }
                                // Find closest center point to translate edge.
                                if(innerVec != null && outerVec != null){
                                    //System.out.println("innerVec " + innerVec + " out  " + outerVec);
                                    double edgeLength = innerVec.distance(outerVec);
                                    
                                    for(int cc = 0; cc < centreLinePoints.size(); cc++){
                                        Vec3 currCentrePoint = centreLinePoints.elementAt(cc);
                                        double distance = innerVec.distance(currCentrePoint);  // Issue innerVec can be ID or OD from the tube geometry?
                                        if(distance * 1.5 < minOuterDiameter){
                                            //System.out.println(" Found centre " + currCentrePoint);
                                            Vec3 translateVec = innerVec.minus(currCentrePoint);
                                            Vec3 calculatedPoint = new Vec3(outerVec);
                                            calculatedPoint.subtract(translateVec);
                                
                                            //System.out.println("  calculatedPoint: " + calculatedPoint + "  edgeLength: " + edgeLength);
                                            EndCenterPointContainer endCenter = endCenterPointMap.get(currCentrePoint);
                                            if(endCenter == null){                                  // Add first
                                                endCenter = new EndCenterPointContainer();
                                                endCenter.edgeLength = edgeLength;
                                                endCenter.calculatedCenterPoint = calculatedPoint;
                                                endCenter.outerVec = outerVec; // Debug
                                                endCenter.innerVec = innerVec; // Debug
                                                endCenter.existCenter = currCentrePoint; // Debug
                                                endCenter.translateDist = distance;
                                                endCenterPointMap.put(currCentrePoint, endCenter);
                                            } else {                                                // Update existing
                                                if(endCenter.edgeLength < edgeLength){
                                                    endCenter.edgeLength = edgeLength;
                                                    endCenter.calculatedCenterPoint = calculatedPoint;
                                                    endCenter.outerVec = outerVec; // Debug
                                                    endCenter.innerVec = innerVec; // Debug
                                                    endCenter.existCenter = currCentrePoint; // Debug
                                                    endCenter.translateDist = distance;
                                                    endCenterPointMap.put(currCentrePoint, endCenter);
                                                }
                                            }
                                            
                                            Vector<EndCenterPointContainer> centerPoints = endCenterPointOptionsMap.get(currCentrePoint);
                                            if(centerPoints == null){
                                                centerPoints = new Vector<EndCenterPointContainer>();
                                            }
                                            
                                            // Get count
                                            int count = 0;
                                            for(int xx = 0; xx < centerPoints.size(); xx++){
                                                EndCenterPointContainer currEndCenterPointContainer = centerPoints.elementAt(xx);
                                                double dist = currEndCenterPointContainer.calculatedCenterPoint.distance(calculatedPoint);
                                                //System.out.println(" - count dist " + dist + " minDistance: " + minDistance); // 0.006
                                                if(dist <= minDistance * 10){ // 0.08 0.0001 Heuristic is not great.
                                                    count += 1;
                                                }
                                            }
                                            
                                            endCenter = new EndCenterPointContainer();
                                            endCenter.edgeLength = edgeLength;
                                            endCenter.calculatedCenterPoint = calculatedPoint;
                                            endCenter.outerVec = outerVec; // Debug
                                            endCenter.innerVec = innerVec; // Debug
                                            endCenter.existCenter = currCentrePoint; // Debug
                                            endCenter.translateDist = distance;
                                            endCenter.count = count;
                                            endCenter.distToCenter = calculatedPoint.distance(innerVec);
                                            centerPoints.addElement(endCenter);
                                            endCenterPointOptionsMap.put(currCentrePoint, centerPoints);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Find longest distance with at least 2 count (having multiple locations.)
                    // This only works if the edges in the tube geometry are not torsionally rotated.
                    for (Map.Entry<Vec3, Vector<EndCenterPointContainer>> set : endCenterPointOptionsMap.entrySet()) {
                        Vec3 existingCentre = set.getKey();
                        Vector<EndCenterPointContainer> prospectPoints = set.getValue();
                        double farthestDist = 0;
                        EndCenterPointContainer farthestEndCenterPointContainer = null;
                        for(int pp = 0; pp < prospectPoints.size(); pp++){
                            EndCenterPointContainer endCenterPointContainer = prospectPoints.elementAt(pp);
                            //System.out.println(" ---  " + endCenterPointContainer.count + " dist " + endCenterPointContainer.distToCenter );
                            if(endCenterPointContainer.count > 2 && endCenterPointContainer.distToCenter > farthestDist){
                                farthestDist = endCenterPointContainer.distToCenter;
                                farthestEndCenterPointContainer = endCenterPointContainer;
                            }
                        }
                        if(farthestEndCenterPointContainer != null){
                            // Extend ArcTube on end for margin by 1/8 of the tube diameter.
                            Vec3 extendVector = new Vec3(farthestEndCenterPointContainer.existCenter);
                            extendVector.subtract(farthestEndCenterPointContainer.calculatedCenterPoint);
                            double scaleExtend = (outerDiameter / 8) / farthestEndCenterPointContainer.distToCenter;
                            extendVector = extendVector.times(scaleExtend);
                            Vec3 extendedCenterPoint = new Vec3(farthestEndCenterPointContainer.calculatedCenterPoint);
                            extendedCenterPoint.subtract(extendVector);
                            
                            Vec3 firstCenter = centreLinePoints.elementAt(0);
                            Vec3 lastCenter = centreLinePoints.elementAt( centreLinePoints.size() - 1 );
                            double firstDist = farthestEndCenterPointContainer.calculatedCenterPoint.distance(firstCenter);
                            double lastDist = farthestEndCenterPointContainer.calculatedCenterPoint.distance(lastCenter);
                            if(firstDist < lastDist){                                               // Add to front
                                
                                centreLinePoints.insertElementAt(extendedCenterPoint, 0);
                            } else {                                                                // add to end
                                
                                centreLinePoints.insertElementAt(extendedCenterPoint, centreLinePoints.size());
                            }
                            //System.out.println(" extendedCenterPoint:  " + extendedCenterPoint);
                        }
                    }
                    /*
                     // This does not work because the longest edge in the tube geometry is not the desired vector.
                    // add calculated arc center points to existing ones.
                    for (Map.Entry<Vec3, EndCenterPointContainer> set : endCenterPointMap.entrySet()) {
                        Vec3 existingCentre = set.getKey();
                        EndCenterPointContainer endCenter = set.getValue();
                        //System.out.println("endCenter " + endCenter.calculatedCenterPoint );
                        //System.out.println(" from edge outer " +  endCenter.outerVec + " inner " + endCenter.innerVec );
                        //System.out.println("   existing center: " +  endCenter.existCenter );
                        //System.out.println("   translateDist: " +  endCenter.translateDist  );
                        
                        Vec3 firstCenter = centreLinePoints.elementAt(0);
                        Vec3 lastCenter = centreLinePoints.elementAt( centreLinePoints.size() - 1 );
                        double firstDist = endCenter.calculatedCenterPoint.distance(firstCenter);
                        double lastDist = endCenter.calculatedCenterPoint.distance(lastCenter);
                        if(firstDist < lastDist){                                               // Add to front
                            centreLinePoints.insertElementAt( endCenter.calculatedCenterPoint, 0 );
                        } else {                                                                // add to end
                            centreLinePoints.insertElementAt( endCenter.calculatedCenterPoint, centreLinePoints.size() );
                        }
                    }
                     */
                    
                    
                    
                    // Order centre line such that the distances are minimized. Set to in order.
                    // Special Note. orderPoints() doesn't work unless first point is on an end. Since we are adding the ends the order works.
                    centreLinePoints = orderPoints(centreLinePoints);
                    
                    
                    // Get bend radius from point data.
                    double bendradius = getArcBendRadius(centreLinePoints, outerDiameter);
                    
                    
                    // Simplify centre line.
                    // For shifting set of 5 points, can middle three (2,3,4) be replaced by new point intersection between 1,2 and 4,5.
                    centreLinePoints = simplifyArcProfile(centreLinePoints, outerDiameter);
                    
                    
                    
                    // Add centre line to scene.
                    // centreLinePoints
                    centreLine = new Vec3[centreLinePoints.size()];
                    for(int i = 0; i < centreLinePoints.size(); i++){
                        Vec3 centreVec = centreLinePoints.elementAt(i);
                        centreLine[i] = centreVec;
                        //System.out.println(" centre " + centreVec.x + " " + centreVec.y + " " + centreVec.z);
                    }
                    // ArcObject tubeArc =
                    ArcObject tubeCurve = getArc(centreLinePoints, outerDiameter, (outerDiameter-innerDiameter) / 2, bendradius);
                    CoordinateSystem coords = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
                    ObjectInfo centreCurveOI = new ObjectInfo(tubeCurve, coords, info.getName() + "-Arc Tube" + vind);
                    UndoRecord undo = new UndoRecord(layoutWindow, false, UndoRecord.DELETE_OBJECT, new Object [] {new Integer(layoutWindow.getScene().getNumObjects()-1)});
                    layoutWindow.addObject(centreCurveOI, undo);
                    layoutWindow.setSelection(layoutWindow.getScene().getNumObjects()-1);
                    layoutWindow.setUndoRecord(  undo  );
                    layoutWindow.updateImage();
                    layoutWindow.updateTree();
                    
                    // TODO:
                    // Add notches from geometry to the new ArcTube object.
                    //
                    addNotchesToArcFromMesh(info, centreCurveOI);
                    
                    Sound.playSound("success.wav");
                } else {
                    System.out.println("Unsuported object type.");
                    Sound.playSound("deny.wav");
                }
            }
        }
    }
    
    /**
     * addNotchesToArcFromMesh
     *
     */
    public void addNotchesToArcFromMesh(ObjectInfo info, ObjectInfo centreCurveOI){
        
        
    }
    
    public class EndCenterPointContainer {
        public double edgeLength = 0;
        public Vec3 calculatedCenterPoint = null;
        public Vec3 outerVec = null; // Debug info
        public Vec3 innerVec = null; // Debug info
        public Vec3 existCenter = null; // Debug
        public double translateDist = 0; // Debug
        public double distToCenter = 0;
        public int count = 0;
        EndCenterPointContainer (){}
    }
    
    /**
     * simplifyArcProfile
     *
     * Description: replace points along arc circumfrance with new point that represents arc curve.
     *
     * @param: Vector<Vec3> centerLinePoints
     * @param: double diameter
     * @return:
     */
    public Vector<Vec3> simplifyArcProfile(Vector<Vec3> centerLinePoints, double diameter){
        int arcIndexStart = -1;
        int arcIndexEnd = -1;
        for(int i = centerLinePoints.size() -1; i > 0; i--){        // iterate backwards so we can remove points as we go.
            Vec3 currPoint1 = centerLinePoints.elementAt(i - 1);    // i - 1 starting with last element
            Vec3 currPoint2 = centerLinePoints.elementAt(i - 0);    //
            double segDist = currPoint1.distance(currPoint2);
            
            double minSegDist = diameter * 1.05;
            
            if(segDist < minSegDist && arcIndexStart < 0){          // Min seg distance met, shift simplify start index.
                arcIndexStart = i + 1;
                //System.out.println(" Start index: " + arcIndexStart);
                //System.out.println(" *** segDist: " + segDist + " diameter: " + diameter);
            }
            if(segDist > minSegDist && arcIndexStart > -1 && arcIndexStart < centerLinePoints.size()){  // bounds check
                arcIndexEnd = i + 1;
                //System.out.println(" Start index: " + arcIndexStart + " End index: " + arcIndexEnd );
                
                // 1) Calculate replacement point
                //System.out.println("     Vector 1 " +  (arcIndexEnd -2) + " - " + (arcIndexEnd - 1) );
                //System.out.println("     Vector 2 " +  (arcIndexStart - 1) + " - " + (arcIndexStart) );
                Vec3 a = centerLinePoints.elementAt(arcIndexEnd - 2);
                Vec3 b = centerLinePoints.elementAt(arcIndexEnd - 1);
                Vec3 c = centerLinePoints.elementAt(arcIndexStart - 1);
                Vec3 d = centerLinePoints.elementAt(arcIndexStart);
                
                // Check angle between segments. Don't procede if angle is < .1 degrees
                //double angleAB = a.getAngle(b);
                //double angleCD = c.getAngle(d);
                //System.out.println(" angle  " + angleAB + " angleCD " + angleCD);
                
                //System.out.println(" a " + a + " b " + b + " c" + c + " d " + d);
                Vec3 targetPoint = lineToLineIntersection(a, b, c, d);
                //System.out.println("  tgt: " + targetPoint + " " );
                
                //System.out.println(" ab: " + a.distance(b) + " cd: " + c.distance(d) );
                
                // 2) Remove segment points between start and end.
                for(int r = arcIndexStart - 1; r > arcIndexEnd - 2; r--){
                    centerLinePoints.removeElementAt( r );
                }
                centerLinePoints.insertElementAt(targetPoint, arcIndexEnd - 1 );
                
                arcIndexStart = -1;  //
            }
        }
        return centerLinePoints;
    }
    
    
    // https://stackoverflow.com/questions/45897542/how-to-find-the-intersection-of-two-lines-in-a-3d-space-using-jmonkeyengine3-or
    /**
        * Calculate the line segment that is the shortest route between the two lines
        * determined by the segments.
        *
        * Even though we are passing segments as arguments the result is the intersection of the lines in which
        * the segments are contained, not the intersection of the segments themselves.
        *
        */
       public Vec3 lineToLineIntersection(Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4) { // LineSegment3D segmentA, LineSegment3D segmentB
          Vec3 p43 = new Vec3(p4.x - p3.x, p4.y - p3.y, p4.z - p3.z);
          //checkArgument(!(abs(p43.x) < NUMBERS_SHOULD_BE_DIFFERENT_DELTA &&
          //                  abs(p43.y) < NUMBERS_SHOULD_BE_DIFFERENT_DELTA &&
          //                  abs(p43.z) < NUMBERS_SHOULD_BE_DIFFERENT_DELTA), MSG_INVALID_POINTS_FOR_INTERSECTION_CALCULATION);
           Vec3 p21 = new Vec3(p2.x - p1.x, p2.y - p1.y, p2.z - p1.z);
          //checkArgument(!(abs(p21.x) < NUMBERS_SHOULD_BE_DIFFERENT_DELTA &&
          //                  abs(p21.y) < NUMBERS_SHOULD_BE_DIFFERENT_DELTA &&
          //                  abs(p21.z) < NUMBERS_SHOULD_BE_DIFFERENT_DELTA), MSG_INVALID_POINTS_FOR_INTERSECTION_CALCULATION);
          Vec3 p13 = new Vec3(p1.x - p3.x, p1.y - p3.y, p1.z - p3.z);
          double d1343 = p13.x * p43.x + p13.y * p43.y + p13.z * p43.z;
          double d4321 = p43.x * p21.x + p43.y * p21.y + p43.z * p21.z;
          double d4343 = p43.x * p43.x + p43.y * p43.y + p43.z * p43.z;
          double d2121 = p21.x * p21.x + p21.y * p21.y + p21.z * p21.z;
          double denom = d2121 * d4343 - d4321 * d4321;
          //checkArgument(abs(denom) >= NUMBERS_SHOULD_BE_DIFFERENT_DELTA, MSG_INVALID_POINTS_FOR_INTERSECTION_CALCULATION);
          double d1321 = p13.x * p21.x + p13.y * p21.y + p13.z * p21.z;
          double numer = d1343 * d4321 - d1321 * d4343;

          double mua = numer / denom;
          double mub = (d1343 + d4321 * mua) / d4343;

          //return new LineSegment3D(
          //      new Point3d(p1.x+mua*p21.x, p1.y+mua*p21.y, p1.z+mua*p21.z),
          //      new Point3d(p3.x+mub*p43.x, p3.y+mub*p43.y, p3.z+mub*p43.z));
           return new Vec3(p1.x+mua*p21.x, p1.y+mua*p21.y, p1.z+mua*p21.z);
       }
    
    
    /**
     * getArcBendRadius
     * Description: canculate bend radius given the centre line curve profile.
     * TODO: tally and average results.
     */
    public double getArcBendRadius(Vector<Vec3> centreLinePoints, double diameter){
        double radius = 0;
        for(int i = 2; i < centreLinePoints.size(); i++){
            Vec3 currPoint1 = centreLinePoints.elementAt(i - 2);
            Vec3 currPoint2 = centreLinePoints.elementAt(i - 1);
            Vec3 currPoint3 = centreLinePoints.elementAt(i - 0);
            double dist1 = currPoint1.distance(currPoint2);
            double dist2 = currPoint2.distance(currPoint3);
            if(dist1 < diameter && dist2 < diameter ){
                //System.out.println( "YES   1 " + dist1 + " 2: " + dist2 );
                // Calculate arc radius.
                Vec3 a = new Vec3(currPoint1);
                Vec3 b = new Vec3(currPoint2);
                Vec3 c = new Vec3(currPoint3);
                a.subtract(b);  // align to vertB
                c.subtract(b);  // align to vertB
                double angle = a.getAngle(c);
                double len = b.distance(c);
                //System.out.println(" angle: " + angle +  " dist1: "  + dist1 + "  dist2: " + dist2 );
                double xx =  (dist2 * Math.cos(angle));
                double yy =  (dist2 * Math.sin(angle));
                double r = radiusFromPoints(  new Vec2(dist1, 0), new Vec2(0, 0), new Vec2(xx, yy)  );
                radius = r;
            }
        }
        return radius;
    }
    
    /**
     * radiusFromPoints
     */
    public double radiusFromPoints( Vec2 p1, Vec2 p2, Vec2 p3)
      {
        final double offset = Math.pow(p2.x,2) + Math.pow(p2.y,2);
        final double bc =   ( Math.pow(p1.x,2) + Math.pow(p1.y,2) - offset )/2.0;
        final double cd =   (offset - Math.pow(p3.x, 2) - Math.pow(p3.y, 2))/2.0;
        final double det =  (p1.x - p2.x) * (p2.y - p3.y) - (p2.x - p3.x)* (p1.y - p2.y);
        if (Math.abs(det) < 0.000001) {
            //throw new IllegalArgumentException("Yeah, lazy.");
            return 0.01;
        }
        final double idet = 1/det;
        final double centerx =  (bc * (p2.y - p3.y) - cd * (p1.y - p2.y)) * idet;
        final double centery =  (cd * (p1.x - p2.x) - bc * (p2.x - p3.x)) * idet;
        final double radius =  Math.sqrt( Math.pow(p2.x - centerx,2) + Math.pow(p2.y-centery,2));
        //return new Circle(new Point(centerx,centery),radius);
          return radius;
      }
    
    
    /**
     * orderPoints
     *
     * Description: reorder points to minimize distance. Put them in order.
     * Relies on the fact that the first point is at an edge. This routine can not sort the first point.
     *
     * @param: Vector<Vec3> centreLinePoints - List of detected points that will make up the new ArcTube object.
     * @return: Vector<Vec> sorted list of centre line points.
     */
    public Vector<Vec3> orderPoints(Vector<Vec3> centreLinePoints){
        Vector<Vec3> sorted = new Vector<Vec3>();
        if(centreLinePoints.size() > 1){
            
            Vec3 currPoint = centreLinePoints.elementAt(0);
            sorted.addElement(currPoint);
            while(sorted.size() < centreLinePoints.size()){                                 // keep adding points until they are all added.
                double closestDist = 99999;
                int closestIndex = -1;
                Vec3 closestPoint = null;
                for(int i = 0; i < centreLinePoints.size(); i++){                           //
                    Vec3 nextPoint = centreLinePoints.elementAt(i);
                    if(nextPoint != currPoint && sorted.contains(nextPoint) == false){
                        double distance = currPoint.distance(nextPoint);
                        if(distance < closestDist){
                            closestDist = distance;
                            closestIndex = i;
                            closestPoint = nextPoint;
                        }
                    }
                }
                if(closestIndex != -1){
                    sorted.addElement( closestPoint );
                    currPoint = closestPoint;
                }
            }
            
            /*
             Vec3 firstPoint = centreLinePoints.elementAt(0);
             sorted.addElement(firstPoint);
             while(sorted.size() < centreLinePoints.size()){
                 double closestDist = 999;
                 int closestIndex = -1;
                 Vec3 closestPoint = null;
                 for(int i = 1; i < centreLinePoints.size(); i++){
                     Vec3 currPoint = centreLinePoints.elementAt(i);
                     if(currPoint != firstPoint && sorted.contains(currPoint) == false){
                         double distance = firstPoint.distance(currPoint);
                         if(distance < closestDist){
                             closestDist = distance;
                             closestIndex = i;
                             closestPoint = currPoint;
                         }
                     }
                 }
                 if(closestIndex != -1){
                     sorted.addElement( closestPoint );
                     firstPoint = closestPoint;
                 }
             }
             */
        }
        return sorted;
    }
    
    
    /**
     * calculateCentrePoint
     * Description: Given set of points. 
     */
    public Vec3 calculateCentrePoint(Vector<Vec3> eds){
        Vec3 centrePoint = new Vec3();
        double minX = 999;
        double maxX = -999;
        double minY = 999;
        double maxY = -999;
        double minZ = 999;
        double maxZ = -999;
        for(int ii = 0; ii < eds.size(); ii++){
            Vec3 rp = (Vec3)eds.elementAt(ii);

            if(rp.x < minX){
                minX = rp.x;
            }
            if(rp.x > maxX){
                maxX = rp.x;
            }
            if(rp.y < minY){
                minY = rp.y;
            }
            if(rp.y > maxY){
                maxY = rp.y;
            }
            if(rp.z < minZ){
                minZ = rp.z;
            }
            if(rp.z > maxZ){
                maxZ = rp.z;
            }
            
            if(centrePoint == null){
                centrePoint = new Vec3(rp);
            } else {
                centrePoint.add(rp);
            }
            //System.out.println("       - " + rp );
        }
        //System.out.println("       SIZE " + eds.size() );
        centrePoint.divideBy((int)eds.size());
        //centrePoint.x = (maxX+minX) / 2.0;
        //centrePoint.y = (maxY+minY) / 2.0;
        //centrePoint.z = (maxZ+minZ) / 2.0;
        return centrePoint;
    }
     
     
    public double calculateDiameter(Vector<Vec3> eds){
        double diameter = 0;
        if(eds.size() > 0){
            Vec3 firstPoint = eds.elementAt(0);
            for(int i = 1; i < eds.size(); i++){
                Vec3 currPoint = eds.elementAt(i);
                double currDist = firstPoint.distance(currPoint);
                if(currDist > diameter){
                    diameter = currDist;
                }
            }
        }
        return diameter;
    }
    
    /**
     * followEdges
     * Description: Follow connected points as long as the edge lengths are the same until traveling to the start point.
     *
     * @param: Vec3 startPoint - point given to explore.
     * @param: Vec3 currPoint -
     * @param: MeshVertex [] - vert data.
     * @param: Edge[] - array of edge data.
     * @param: Vector<Vec3> usedPoints - tracks points from geometry that have been used and are included in a return list.
     * @param:
     */
    public Vector<Vec3> followEdges(Vec3 startPoint, Vec3 currPoint, MeshVertex[] verts, Edge edges[], Vector<Vec3> usedPoints, Vector<Edge> usedEdges, double edgeDist, int level){
        double tollerence = 0.005; //  0.005;
        
        Vector<Vec3> results = new Vector<Vec3>(); // usedPoints
        int connectedEdges = 0;
        for(int e = 0; e < edges.length; e++){
            Edge edge = edges[e];
            if(verts.length > edge.v1 && verts.length > edge.v2 && edge.v1 > -1 && edge.v2 > -1){ // bounds check
                MeshVertex a = verts[edge.v1];
                MeshVertex b = verts[edge.v2];
                Vec3 vecA = a.r;
                Vec3 vecB = b.r;
                double currEdgeDist = vecA.distance(vecB);
                Vec3 connectedPoint = null;
                int connectedPointIndex = -1;
                if( vecA == currPoint ){
                    connectedPoint = vecB;
                    connectedPointIndex = edge.v2;
                }
                if( vecB == currPoint ){
                    connectedPoint = vecA;
                    connectedPointIndex = edge.v1;
                }
                if(connectedPoint != null){
                    connectedEdges++;
                    //System.out.println(" connectedPoint: " + connectedPointIndex );
                    boolean ok = true;
                    
                    if(results.size() > 3){  // Sometimes the follow can jump between two bend points. check angle to prevent this.
                        Vec3 aa = new Vec3(results.elementAt(0));
                        Vec3 bb = new Vec3(results.elementAt(1));
                        Vec3 cc = new Vec3(results.elementAt(2));
                        aa.subtract(bb);
                        cc.subtract(bb);
                        double angleAB = aa.getAngle(bb);
                        double angleBC = cc.getAngle(bb);
                        double angle = Math.abs(angleAB - angleBC);
                        angle = Math.toDegrees(angle);
                        if(angle > 10){
                            ok = false;
                        }
                    }
                    
                    if(edgeDist > 0 && isClose(edgeDist, currEdgeDist, tollerence) == false){       // Distance qualify
                        
                        //System.out.println(" Distance not same: " + edgeDist + " curr: " + currEdgeDist );
                        
                        ok = false;
                    } else if(edgeDist > 0 ){
                        //System.out.println("     Distance OK: " +edgeDist + " " + currEdgeDist  + " lev: " + level  );
                        //if(level >= 2){
                            //System.out.println(" Distance OK: " +edgeDist + " " + currEdgeDist );
                        //}
                    }
                    if(ok && usedPoints.contains(connectedPoint)){                                         // Allready followed this path.
                        //ok = false;
                        //if(level == 2){
                        //    System.out.println(" REPEAT ending point used " + connectedPointIndex  );
                        //}
                    }
                    if(ok && edgeDist > 0 && usedEdges.contains(edge) ){
                        ok = false;
                        if(level >= 2){
                            //System.out.println(" REPEAT ending       lev: " + level + " e: " + e + " edge.v1: " + edge.v1 + " edge.v2: " + edge.v2);
                            //System.out.println(" usedPoints " + usedPoints.size() );
                        }
                    } else if( edgeDist > 0 && usedEdges.contains(edge) == false  ) {
                        if(level >= 2){
                            //System.out.println("     Edge new OK:   lev: " + level );
                            //System.out.println(" usedPoints " + usedPoints.size() );
                        }
                    }
                    if(ok && edgeDist > 0 && startPoint == connectedPoint  && usedPoints.size() > 3  ){                                             // Closed loop
                        //System.out.println(" DONE a    used size: " + usedPoints.size() + "   "  + " lev: " + level);
                        //System.out.println(" ************* ");
                        //ok = false;
                        return usedPoints;
                    }
                    if(ok){
                        if(edgeDist > 0){
                            //System.out.println("   FOLLOW points: " + usedPoints.size() + " lev: " + level);
                            //System.out.println("       - start " + startPoint + "    connectedPoint  "  + connectedPoint     );
                            //System.out.println("       - dist: " + currEdgeDist  + "  edgeDist: " + edgeDist);
                            //System.out.println("       - edge.v1: " + edge.v1 +    "  edge.v2: " + edge.v2 );
                            //System.out.println("       - recurse with e: " + e + "    level: " + level );
                            //System.out.println(" ");
                        }
                        //System.out.println("       - recurse with e: " + e + "    level: " + level );
                        Vector<Vec3> currUsedPoints = new Vector<Vec3>(usedPoints);
                        Vector<Edge> currUsedEdges = new Vector<Edge>(usedEdges);
                        currUsedPoints.addElement(connectedPoint);
                        currUsedEdges.addElement(edge);
                        Vector v = followEdges(startPoint, connectedPoint, verts, edges, currUsedPoints, currUsedEdges, currEdgeDist, level + 1);
                        //System.out.println("            follow  v.size() " + v.size() + "  results.size() " + results.size());
                        if(v.size() > 1 && v.size() != results.size()){
                            results = v;                        // take smaller in merge
                            results.addElement(startPoint);     // Add start point
                        } else {
                            results.addAll(v);                  // accumulate points
                            /*
                            // Only add if not allready.
                            for(int ad = 0; ad < v.size(); ad++){
                                Vec3 addPoint = (Vec3)v.elementAt(ad);
                                if(results.contains(addPoint) == false){
                                    results.addElement(addPoint);
                                }
                            }
                            */
                        }
                    }
                }
            }
        }
        //System.out.println("Total connectedEdges: " + connectedEdges);
        return results;
    }
    

    /**
     * removeDuplicates
     */
    public Vector<Vec3> removeDuplicates(Vector<Vec3> eds, double tolerance){
        boolean done = false;
        for(int i = eds.size() - 1; i >= 0; i--){
            Vec3 currPoint = eds.elementAt(i);
            for(int j = 0; j < eds.size(); j++){
                if(i != j){
                    Vec3 comparePoint = eds.elementAt(j);
                    if(
                       //currPoint.x == comparePoint.x &&
                       //currPoint.y == comparePoint.y &&
                       //currPoint.z == comparePoint.z
                       Math.abs(currPoint.x - comparePoint.x) <= tolerance &&
                       Math.abs(currPoint.y - comparePoint.y) <= tolerance &&
                       Math.abs(currPoint.z - comparePoint.z) <= tolerance
                       ){
                        eds.removeElementAt(i);
                        j = eds.size();
                    }
                }
            }
        }
        return eds;
    }
    
    public boolean isClose(double a, double b, double tol){
        double cmp = Math.max(a, b) - Math.min(a, b);  // Double.compare(a, b);
        //System.out.println("Compare: " + cmp);
        if(cmp < tol){
            return true;
        }
        return false;
    }
    
    public Vector<Vec3> removeFromPool(Vec3 vec, Vector<Vec3> tubePoints){
        for(int i = 0; i < tubePoints.size(); i++){
            Vec3 v = tubePoints.elementAt(i);
            if(v.equals(vec)){
                tubePoints.removeElementAt(i);
                return tubePoints;
            }
        }
        return tubePoints;
    }
    
    public Vec3 getConnectedPoint(Vec3 vec, double maxDistance, MeshVertex[] verts, Vector<Vec3> tubePoints, Edge edges[]){
        Vec3 result = null;
        for(int e = 0; e < edges.length; e++){
            Edge edge = edges[e];
            if(edge.v1 >= verts.length || edge.v2 >= verts.length){
                System.out.println("Bounds error");
                return null;
            }
            MeshVertex a = verts[edge.v1];
            MeshVertex b = verts[edge.v2];
            Vec3 vecA = a.r;
            Vec3 vecB = b.r;
            boolean AInPool = false;
            boolean BInPool = false;
            for(int i = 0; i < tubePoints.size(); i++){
                Vec3 v = tubePoints.elementAt(i);
                if(v.equals(vecA)){
                    AInPool = true;
                }
                if(v.equals(vecB)){
                    BInPool = true;
                }
            }
            if(BInPool && vecA.equals(vec)){
                //System.out.println(" A ");
                double distance = vec.distance(vecB);
                if(distance < (maxDistance / 2)){
                    //clusterPoints.addElement(vecB);
                    result = vecB;
                    return result;
                }
            }
            if(AInPool && vecB.equals(vec)){
                //System.out.println(" B ");
                double distance = vec.distance(vecA);
                if(distance < (maxDistance / 2)){
                    //clusterPoints.addElement(vecA);
                    result = vecA;
                    return result;
                }
            }
        }
        return result;
    }
    
    /**
     * getCurve
     *
     */
    public Curve getCurve(Vec3[] points){
        float smooths[] = new float[points.length];
        for(int i = 0; i < points.length; i++){
            smooths[i] = 1.0f;
        }
        Curve curve = new Curve(points, smooths, Mesh.APPROXIMATING, false);
        return curve;
    }
    
    /**
     * getArc
     * Description: Get an arc object from point data.
     */
    public ArcObject getArc(Vector<Vec3> points, double diameter, double wall, double bendRadius){
        Vec3[] pointArray = new Vec3[points.size()];
        float smooths[] = new float[points.size()];
        for(int i = 0; i < points.size(); i++){
            pointArray[i] = points.elementAt(i);
            smooths[i] = 1.0f;
        }
        ArcObject arc = new ArcObject(pointArray, smooths, Mesh.NO_SMOOTHING, false);
        arc.setTubeDiameter(diameter);
        arc.setTubeWall(wall);
        arc.setBendRadius(bendRadius);
        Scene scene = layoutWindow.getScene();
        arc.setScene(scene); // scene is required to draw
        return arc;
    }
    
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
