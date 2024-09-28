
package armarender;

import java.util.*;
import armarender.math.*;
import armarender.object.*;


public class Examples {
    
    public Examples(){
        
    }
    
    /**
     * demo
     * Description: Example of common tasks.
     */
    public void demo(LayoutWindow window){
        LayoutModeling layout = new LayoutModeling();
        Scene scene = window.getScene();
        
        
        // list objects in the scene.
        Vector<ObjectInfo> sceneObjects = scene.getObjects();
        for(int i = 0; i < sceneObjects.size(); i++){
            ObjectInfo currInfo = sceneObjects.elementAt(i);
            System.out.println(" iterate scene objects: " + currInfo.getName() );
            Object3D currObj = currInfo.getObject();
            
            //
            // Is the object a TriangleMesh?
            //
            TriangleMesh triangleMesh = null;
            if(currObj instanceof TriangleMesh){
                triangleMesh = (TriangleMesh)currObj;
            } else if(currObj.canConvertToTriangleMesh() != Object3D.CANT_CONVERT){
                triangleMesh = currObj.convertToTriangleMesh(0.1);
            }
            if(triangleMesh != null){
                CoordinateSystem c;
                c = layout.getCoords(currInfo);
                
                // Convert object coordinates to world (absolute) coordinates.
                // The object has its own coordinate system with transformations of location, orientation, scale ect. To see them in absolute world coordinates we need to convert.
                Mat4 mat4 = c.duplicate().fromLocal();
                
                MeshVertex[] verts = triangleMesh.getVertices();
                Vector<Vec3> worldVerts = new Vector<Vec3>();
                for(int v = 0; v < verts.length; v++){  // These translated verts will have the same indexes as the object array.
                    Vec3 vert = new Vec3(verts[v].r); // Make a new Vec3 as we don't want to modify the geometry of the object.
                    mat4.transform(vert);
                    worldVerts.addElement(vert); // add the translated vert to our list.
                    //System.out.println("  Vert index: " + v + " - " + vert); // Print vert location XYZ data.
                }
                
                TriangleMesh.Edge[] edges = ((TriangleMesh)triangleMesh).getEdges();
                TriangleMesh.Face[] faces = triangleMesh.getFaces();
                
                for(int f = 0; f < faces.length; f++ ){
                    TriangleMesh.Face face = faces[f];
                    
                    //System.out.println("  Face: A: " + worldVerts.elementAt(face.v1) + " B: " + worldVerts.elementAt(face.v2) + " C: " + worldVerts.elementAt(face.v3)  );
                    
                    // Calculate the center point and its face normal vector.
                    // ...
                    
                    
                    // Add a line object to the scene to represent the normal vector.
                    
                }
                
            }
        }
        
        
        //
        // Add a cube to the scene.
        // This object could be used to represent a collision region or represent the machine .
        //
        // TODO
        
        
        //
        // Detect if a selected object is colliding with other objects in the scene.
        // Note: This is only one possible implementation of collision detection.
        // For a section, capture the locations of the edges, then scan through the objects in the scene,
        // and check if the edges intersect with any of the faces.
        // Collitions on multiple objects constructed to represent the router, collete and machine could be used to detect collisions with
        // The mould part.
        //
        boolean selectionCollides = false;
        int sel[] = window.getSelectedIndices();
        Vector<EdgePoints> selectedEdgePoints = new Vector<EdgePoints>(); // TriangleMesh.Edge
        if(sel.length > 0){
            Vector<ObjectInfo> selectedObjects = new Vector<ObjectInfo>();
            for(int s = 0; s < sel.length; s++){
                ObjectInfo selectedInfo = window.getScene().getObject( sel[s] );
                if(selectedInfo != null){
                    CoordinateSystem c;
                    c = layout.getCoords(selectedInfo);
                    Mat4 mat4 = c.duplicate().fromLocal();
                    
                    Object3D selectedObj = selectedInfo.getObject();
                    TriangleMesh selectedTM = null;
                    if(selectedObj instanceof TriangleMesh){
                        selectedTM = (TriangleMesh)selectedObj;
                    } else if(selectedObj.canConvertToTriangleMesh() != Object3D.CANT_CONVERT){
                        selectedTM = selectedObj.convertToTriangleMesh(0.1);
                    }
                    if(selectedTM != null){
                        selectedObjects.addElement(selectedInfo);   // Save for exclusion later.
                        MeshVertex[] verts = selectedTM.getVertices();
                        Vector<Vec3> worldVerts = new Vector<Vec3>();
                        for(int v = 0; v < verts.length; v++){  // These translated verts will have the same indexes as the object array.
                            Vec3 vert = new Vec3(verts[v].r); // Make a new Vec3 as we don't want to modify the geometry of the object.
                            mat4.transform(vert);
                            worldVerts.addElement(vert); // add the translated vert to our list.
                            //System.out.println("  Vert index: " + v + " - " + vert); // Print vert location XYZ data.
                        }
                        TriangleMesh.Edge[] edges = selectedTM.getEdges();
                        for(int e = 0; e < edges.length; e++){
                            TriangleMesh.Edge edge = edges[e];
                            Vec3 a = worldVerts.elementAt( edge.v1 );
                            Vec3 b = worldVerts.elementAt( edge.v2 );
                            EdgePoints ep = new EdgePoints(a, b);
                            selectedEdgePoints.addElement(ep);
                        }
                    }
                }
            } // for each selection
            if(selectedEdgePoints.size() > 0){
                //System.out.println(" edges " + selectedEdgePoints.size() );
                // Scan through objects in the scene to see if there is a collistion.
                for(int i = 0; i < sceneObjects.size(); i++){
                    ObjectInfo currInfo = sceneObjects.elementAt(i);
                    if( selectedObjects.contains(currInfo) ){          // Don't compare with self
                        continue;
                    }
                    //System.out.println("   Compare with  scene object: " + currInfo.getName() );
                    Object3D currObj = currInfo.getObject();
                    
                    //
                    // Is the object a TriangleMesh or can it be converted?
                    //
                    TriangleMesh triangleMesh = null;
                    if(currObj instanceof TriangleMesh){
                        triangleMesh = (TriangleMesh)currObj;
                    } else if(currObj.canConvertToTriangleMesh() != Object3D.CANT_CONVERT){
                        triangleMesh = currObj.convertToTriangleMesh(0.1);
                    }
                    if(triangleMesh != null){
                        CoordinateSystem c;
                        c = layout.getCoords(currInfo);
                        
                        // Convert object coordinates to world (absolute) coordinates.
                        // The object has its own coordinate system with transformations of location, orientation, scale ect. To see them in absolute world coordinates we need to convert.
                        Mat4 mat4 = c.duplicate().fromLocal();
                        MeshVertex[] verts = triangleMesh.getVertices();
                        Vector<Vec3> worldVerts = new Vector<Vec3>();
                        for(int v = 0; v < verts.length; v++){  // These translated verts will have the same indexes as the object array.
                            Vec3 vert = new Vec3(verts[v].r); // Make a new Vec3 as we don't want to modify the geometry of the object.
                            mat4.transform(vert);
                            worldVerts.addElement(vert); // add the translated vert to our list.
                            //System.out.println("  Vert index: " + v + " - " + vert); // Print vert location XYZ data.
                        }
                        TriangleMesh.Edge[] edges = ((TriangleMesh)triangleMesh).getEdges();
                        TriangleMesh.Face[] faces = triangleMesh.getFaces();
                        for(int f = 0; f < faces.length; f++ ){
                            TriangleMesh.Face face = faces[f];
                            Vec3 faceA = worldVerts.elementAt(face.v1);
                            Vec3 faceB = worldVerts.elementAt(face.v2);
                            Vec3 faceC = worldVerts.elementAt(face.v3);
                            for(int ep = 0; ep < selectedEdgePoints.size(); ep++){
                                EdgePoints edgePoint = selectedEdgePoints.elementAt(ep);
                                if(Intersect2.intersects(edgePoint.a, edgePoint.b, faceA, faceB, faceC)){
                                    selectionCollides = true;
                                    System.out.println("Collision  object: " + currInfo.getName());
                                }
                            }
                        }
                    }
                } // for each object in scene
            }
            System.out.println("Selection collides: " + selectionCollides ); // Print result
        } else {
            System.out.println("No objects selected to check for collisions.");
        }
    
        
        
        //
        // Calculate a working / cutting region based on:
        // A) the objects in the scene representing the shape to create.
        // B) The projection vector or direction of the B/C axis pointing the drill in a direction
        //
        double c = 15; // rotation, 0-360.
        double b = 45; // angle 45 degrees.
        
        Vector<Vec3> regionSurfacePoints = new Vector<Vec3>(); // accumulated surface points
        Vector<Vec3> debugMappingGrid = new Vector<Vec3>();
        Vec3 toolVector = new Vec3(0, 1, 0); //
        Mat4 zRotationMat = Mat4.zrotation(Math.toRadians(b)); // Will be used to orient the inital position of the B axis.
        Mat4 yRotationMat = Mat4.yrotation(Math.toRadians(c)); // Will be used to orient the inital position of the C axis.
        zRotationMat.transform(toolVector); // Apply the B axis transform.
        yRotationMat.transform(toolVector); // Apply the C axis rotation
        toolVector.normalize(); // Normalize to scale the vector to a length of 1.
        System.out.println("toolVector " + toolVector);
        // Calculate the bounds of the scene objects,
        // for each object, get bounds.
        BoundingBox sceneBounds = null ; //new BoundingBox(); // Vec3 p1, Vec3 p2
        for(int i = 0; i < sceneObjects.size(); i++){
            ObjectInfo currInfo = sceneObjects.elementAt(i);
            BoundingBox currBounds = currInfo.getTranslatedBounds();
            if(sceneBounds == null){
                sceneBounds = currBounds;
            } else {
                sceneBounds.extend(currBounds);
            }
        }
        if(sceneBounds != null){
            double sceneSize = Math.max(sceneBounds.maxx - sceneBounds.minx, Math.max(sceneBounds.maxy - sceneBounds.miny, sceneBounds.maxz - sceneBounds.minz));
            //System.out.println("sceneSize " + sceneSize);
            Vec3 sceneCenter = sceneBounds.getCenter();
            
            Vec3 raySubtract = new Vec3(toolVector.times( sceneSize * 2) );
            
            // construct a grid and iterate each coordinate and translate it to the toolVector
        
            // Translate to point in space to project region mapping.
            
            Vec3 regionScan = new Vec3(sceneCenter);
            regionScan.add( toolVector.times(sceneSize) );
            
            // DEBUG Show
            addLineToScene(window, regionScan,  regionScan.minus(raySubtract), "B/C Axis Ray", false ); // debug show ray cast line
            
            int width = 80;
            int height = 80;
            
            // Loop through grid
            for(int x = 0; x < width; x++){
                for(int y = 0; y < height; y++){
                    // xy offset to coords, Translate based on 'toolVector'
                    
                    Vec3 samplePoint = new Vec3(regionScan);
                    
                    //Vec3 currGridPoint = new Vec3( (double)(x-(width/2)) * 0.2 , (double)(y-(height/2)) * 0.2, (double)(y-(height/2)) * 0.2 ); // First try
                    Vec3 currGridPoint = new Vec3( (double)(x-(width/2)) * 0.2, 0, (double)(y-(height/2)) * 0.2 );
                    
                    zRotationMat.transform(currGridPoint); // Apply the B axis transform.
                    yRotationMat.transform(currGridPoint); // Apply the C axis rotation
                    
                    samplePoint.add( currGridPoint ); // shift
                    
                    debugMappingGrid.addElement(currGridPoint);
                    
                    Vec3 samplePointB = samplePoint.minus(raySubtract); // Second point in ray cast
                    // Find collision location
                    Vec3 intersectPoint = null;
                    for(int i = 0; i < sceneObjects.size(); i++){
                        ObjectInfo currInfo = sceneObjects.elementAt(i);
                        Object3D currObj = currInfo.getObject();
                        //System.out.println("Checking for intersect in: " + currInfo.getName());
                        
                        //
                        // Is the object a TriangleMesh?
                        //
                        TriangleMesh triangleMesh = null;
                        if(currObj instanceof TriangleMesh){
                            triangleMesh = (TriangleMesh)currObj;
                        } else if(currObj.canConvertToTriangleMesh() != Object3D.CANT_CONVERT){
                            triangleMesh = currObj.convertToTriangleMesh(0.1);
                        }
                        if(triangleMesh != null){
                            CoordinateSystem cs;
                            cs = layout.getCoords(currInfo);
                            
                            // Convert object coordinates to world (absolute) coordinates.
                            // The object has its own coordinate system with transformations of location, orientation, scale ect. To see them in absolute world coordinates we need to convert.
                            Mat4 mat4 = cs.duplicate().fromLocal();
                            
                            MeshVertex[] verts = triangleMesh.getVertices();
                            Vector<Vec3> worldVerts = new Vector<Vec3>();
                            for(int v = 0; v < verts.length; v++){  // These translated verts will have the same indexes as the object array.
                                Vec3 vert = new Vec3(verts[v].r); // Make a new Vec3 as we don't want to modify the geometry of the object.
                                mat4.transform(vert);
                                worldVerts.addElement(vert); // add the translated vert to our list.
                                //System.out.println("  Vert index: " + v + " - " + vert); // Print vert location XYZ data.
                            }
                            TriangleMesh.Edge[] edges = ((TriangleMesh)triangleMesh).getEdges();
                            TriangleMesh.Face[] faces = triangleMesh.getFaces();
                            for(int f = 0; f < faces.length; f++ ){
                                TriangleMesh.Face face = faces[f];
                                Vec3 faceA = worldVerts.elementAt(face.v1);
                                Vec3 faceB = worldVerts.elementAt(face.v2);
                                Vec3 faceC = worldVerts.elementAt(face.v3);
                                Vec3 samplePointCollision = Intersect2.getIntersection(samplePoint, samplePointB, faceA, faceB, faceC );
                                if(samplePointCollision != null){ // found intersection.
                                    //System.out.println(" *** ");
                                    if(intersectPoint != null){   // existing intersection exists, check if the new one is closer
                                        double existingDist = regionScan.distance(intersectPoint);
                                        double currrentDist = regionScan.distance(samplePointCollision);
                                        if(currrentDist < existingDist){
                                            intersectPoint = samplePointCollision;
                                        }
                                    } else {
                                        intersectPoint = samplePointCollision;
                                    }
                                }
                            }
                        }
                    }
                    if(intersectPoint != null){
                        //System.out.println(" Colision " + intersectPoint );
                        regionSurfacePoints.addElement(intersectPoint);
                        //addLineToScene(window, intersectPoint,  intersectPoint.plus(new Vec3(0,1,0)) );
                    }
                } // Y
            } // X
            // Draw line showing mapped surface
            if(regionSurfacePoints.size() > 1){
                addLineToScene(window, regionSurfacePoints, "Surface Map", true);
            }
            if(debugMappingGrid.size() > 1){
                addLineToScene(window, debugMappingGrid, "Projection Grid", false);
            }
        } // bounds
        
        
        
        //
        // Simulate cutting toolpath using regionSurfacePoints calculated from a particular B/C angle.
        //
        String gCodeExport = "";
        for(int i = 1; i < regionSurfacePoints.size(); i++){
            Vec3 surfacePointA = regionSurfacePoints.elementAt(i-1);
            Vec3 surfacePointB = regionSurfacePoints.elementAt(i);
            
            
            
        }
        
        
        
        // More demos
        
        
        window.updateImage();
        
    } // end demo function
    
    
    
    
    /**
     * addLineToScene
     * Description: Add a line to the scene. Good for debugging 3d scene corrdinates.
     */
    public void addLineToScene(LayoutWindow window, Vec3 start, Vec3 end, String name, boolean visible){
        float[] s_ = new float[2];
        for(int i = 0; i < 2; i++){
            s_[i] = 0;
        }
        Vec3 [] vertex = new Vec3[2];
        vertex[0] = start;
        vertex[1] = end;
        Curve theCurve = new Curve(vertex, s_, 0, false);
        CoordinateSystem coords = new CoordinateSystem();
        ObjectInfo info = new ObjectInfo(theCurve, coords, name);
        info.setVisible(visible);
        UndoRecord undo = new UndoRecord(window, false);
        //existingSelectedInfo.addChild(info, 0);
        //            info.setParent(existingSelectedInfo);
        ((LayoutWindow)window).addObject(info, undo);
    }
    
    
    /**
     * addLineToScene
     * Description:
     */
    public void addLineToScene(LayoutWindow window, Vector<Vec3> points, String name, boolean visible){
        float[] s_ = new float[points.size()];
        for(int i = 0; i < points.size(); i++){
            s_[i] = 0;
        }
        Vec3 [] vertex = new Vec3[points.size()];
        for(int i = 0; i < points.size(); i++){
            
            vertex[i] = points.elementAt(i);
        }
        Curve theCurve = new Curve(vertex, s_, 0, false);
        CoordinateSystem coords = new CoordinateSystem();
        ObjectInfo info = new ObjectInfo(theCurve, coords, name);
        info.setVisible(visible);
        UndoRecord undo = new UndoRecord(window, false);
        //existingSelectedInfo.addChild(info, 0);
        //            info.setParent(existingSelectedInfo);
        ((LayoutWindow)window).addObject(info, undo);
    }
    
}

