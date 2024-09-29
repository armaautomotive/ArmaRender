
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
        (new Thread() {
            public void run() {
                
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
                // Note: This concept could be used by running the following code example 4 times with the
                // following configurations (C=0, B=45), (C=90, B=45), (C=180, B=45), (C=270, B=45)
                // This way each of the sides are covered by at leas one pass.
                
                Vector<Vec3> debugMappingGrid = new Vector<Vec3>(); // pattern of cutting to be projected onto the scene.
                Vector<Vec3> regionSurfacePoints = new Vector<Vec3>(); // accumulated surface points projected
                Vector<Vec3> generatedCuttingPath = new Vector<Vec3>(); // GCode cutting path
                
                ObjectInfo surfaceMapInfo = null; // Object represents surface map.
                ObjectInfo toolPath = null;
                
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
                            
                            if(x % 2 == 0){ // Alternate scan direction on Y pass every other X. This is more effecient as it cuts the travel distance in half.
                                currGridPoint = new Vec3( (double)(x-(width/2)) * 0.20, 0, (double)((height-y-1) - (height/2)) * 0.2 ); // reversed
                            }
                            
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
                    
                    if(debugMappingGrid.size() > 1){
                        addLineToScene(window, debugMappingGrid, "Projection Grid", false);
                    }
                    
                    // Add entry and exit paths from start position.
                    // Note if this entry or exit collide they would beed to be rerouted.
                    double maxMachineHeight = 0; // TODO calculate entry and exit points based on the capacity of the machine.
                    Vec3 firstRegionSurfacePoint = regionSurfacePoints.elementAt(0);
                    Vec3 lastRegionSurfacePoint = regionSurfacePoints.elementAt(regionSurfacePoints.size() - 1);
                    regionSurfacePoints.add(0, new Vec3(firstRegionSurfacePoint.x, firstRegionSurfacePoint.y + (sceneSize/4), firstRegionSurfacePoint.z)); // insert entry
                    regionSurfacePoints.add(regionSurfacePoints.size(), new Vec3(lastRegionSurfacePoint.x, lastRegionSurfacePoint.y + (sceneSize/4), lastRegionSurfacePoint.z));
                    
                    // Insert/fill points in gaps. Since the router travels in a straight line between points, we need to check each segment for collisions.
                    regionSurfacePoints = fillGapsInPointPath(regionSurfacePoints);
                    
                    // Draw line showing mapped surface
                    if(regionSurfacePoints.size() > 1){
                        surfaceMapInfo = addLineToScene(window, regionSurfacePoints, "Surface Map", true);
                    }
                } // bounds
                
                
                
                //
                // Simulate cutting toolpath using regionSurfacePoints calculated from a particular B/C angle.
                //
                System.out.println("Scanning surface points using B/C Tool angle. ");
                Vec3 firstRegionSurfacePoint = regionSurfacePoints.elementAt(0);
                Vec3 lastRegionSurfacePoint = regionSurfacePoints.elementAt(regionSurfacePoints.size() - 1);
                ObjectInfo avatarCutterLine = addLineToScene(window, firstRegionSurfacePoint, firstRegionSurfacePoint.plus(toolVector.times(4) ), "Cutter", true );
                Curve currCurve = (Curve)avatarCutterLine.getObject();
                
                ObjectInfo drillBodyCubeInfo = addCubeToScene(window, firstRegionSurfacePoint.plus(toolVector.times(2) ), 0.75, "Router Housing" ); // Cube represents a part of the machine
                drillBodyCubeInfo.setPhysicalMaterialId(500); // This is an identifier used to ensure the objects representing the router are not included in collision detection.
                
                ObjectInfo toolPitCubeInfo = addCubeToScene(window, firstRegionSurfacePoint.plus(toolVector.times( 0.2 ) ), 0.08, "Bit Tip" ); // Cube represents tip of bit
                toolPitCubeInfo.setPhysicalMaterialId(500);
        
                
                for(int i = 0; i < regionSurfacePoints.size(); i++){
                    Vec3 surfacePoint = regionSurfacePoints.elementAt(i);
                    
                    //  calculate where the cutter would be to when fit to the current region surface point.
                    Vector<Vec3> updatedPoints = new Vector<Vec3>();
                    updatedPoints.addElement(surfacePoint);
                    updatedPoints.addElement(surfacePoint.plus(toolVector.times(4))  ); // Make the length of the avatar arbitrary, scale later on.
                    
                    // set location of cube
                    CoordinateSystem drillHousingAvatarCS = drillBodyCubeInfo.getModelingCoords();
                    drillHousingAvatarCS.setOrigin(surfacePoint.plus(toolVector.times(1.25))); // In practice the length of this avatar would be scaled to result in the correct length.
                    drillBodyCubeInfo.clearCachedMeshes();
                    
                    CoordinateSystem drillTipAvatarCS = toolPitCubeInfo.getModelingCoords();
                    drillTipAvatarCS.setOrigin(surfacePoint.plus(toolVector.times(0.09))); // position along length of B/C
                    toolPitCubeInfo.clearCachedMeshes();
                    
                    // Check to see if the avatar cutter collides with any object in the scene.
                    
                    // Collision detection
                    // The generatedCuttingPath can still collide with the scene. Perhaps keep filtering the
                    // generatedCuttingPath pulling out more points that collide until there are no more collisions?
                    // It appear the path along a side still collides.
                    // The reason this doesn't work, is because when you detect a collision and pull the cutter away, the path it leaves
                    // still collides, It needs to move over of find a previous point to pull
                    boolean housingCollides = cubeCollidesWithScene( drillBodyCubeInfo, sceneObjects );
                    boolean tipCollides = cubeCollidesWithScene( toolPitCubeInfo, sceneObjects );
                    if(housingCollides || tipCollides){ // TODO: different objects may be best suited to retract different amounts.
                        
                        // This point will collide so:
                        // 1) We can't add the point to the GCode file, and
                        // 2) We must pull the router out along the B/C axis because we can't be sure the next point travel will collide with the scene.
                        Vec3 retractPoint = new Vec3(surfacePoint.plus(toolVector.times(3)));
                        generatedCuttingPath.addElement(retractPoint);
                        try { Thread.sleep(10); } catch(Exception e){} // Wait to show collision
                        // Note: This method of retracting to avoid collisions is simple but moves the machine excessivly in some cases.
                    } else {
                        generatedCuttingPath.addElement(surfacePoint); // No collision, This point can be safely cut on the machine / GCode.
                    }
                    
                    // Update the avatar object to show to the user where it is in space.
                    currCurve.setVertexPositions(vectorToArray(updatedPoints));
                    avatarCutterLine.clearCachedMeshes();
                    
                    // Update the scene
                    window.updateImage();
                    try { Thread.sleep(2); } catch(Exception e){} // Wait
                }
                    
                generatedCuttingPath = fillGapsInPointPath(generatedCuttingPath ); // Fill in gaps in the retration and reentry made
                
                
                if(surfaceMapInfo != null){
                    surfaceMapInfo.setVisible(false); // Hide surface map because we want to show the GCode cut path now.
                }
                
                
                // If we have a GCode tool path add it to the scene.
                if(generatedCuttingPath.size() > 1){
                    toolPath = addLineToScene(window, generatedCuttingPath, "GCode Tool Path", true);
                }
                
                
                
                //
                // Repeat until all collisions resolved.
                //
                boolean running = true;
                while(running){
                    int collisionCount = 0;
                    System.out.println("Modifying tool path to remove collisions.");
                    //Vector<Vec3> updatedCuttingPath = new Vector<Vec3>();
                    for(int i = 0; i < generatedCuttingPath.size(); i++){
                        Vec3 currPoint = generatedCuttingPath.elementAt(i);
                        
                        //  calculate where the cutter would be to when fit to the current region surface point.
                        Vector<Vec3> updatedPoints = new Vector<Vec3>();
                        updatedPoints.addElement(currPoint);
                        updatedPoints.addElement(currPoint.plus(toolVector.times(4))  ); // Make the length of the avatar arbitrary, scale later on.
                        
                        // set location of cube
                        //CoordinateSystem drillTipAvatarCS = cubeInfo.getModelingCoords();
                        //drillTipAvatarCS.setOrigin(currPoint.plus(toolVector.times(1.25))); // In practice the length of this avatar would be scaled to result in the correct length.
                        //cubeInfo.clearCachedMeshes();
                        
                        CoordinateSystem drillHousingAvatarCS = drillBodyCubeInfo.getModelingCoords();
                        drillHousingAvatarCS.setOrigin(currPoint.plus(toolVector.times(1.25))); // In practice the length of this avatar would be scaled to result in the correct length.
                        drillBodyCubeInfo.clearCachedMeshes();
                        
                        CoordinateSystem drillTipAvatarCS = toolPitCubeInfo.getModelingCoords();
                        drillTipAvatarCS.setOrigin(currPoint.plus(toolVector.times(0.09))); // position along length of B/C
                        toolPitCubeInfo.clearCachedMeshes();
                        
                        // Check to see if the avatar cutter collides with any object in the scene.
                        
                        // Collision detection
                        // The generatedCuttingPath can still collide with the scene. Perhaps keep filtering the
                        // generatedCuttingPath pulling out more points that collide until there are no more collisions?
                        // It appear the path along a side still collides.
                        boolean housingCollides = cubeCollidesWithScene( drillBodyCubeInfo, sceneObjects );
                        boolean tipCollides = cubeCollidesWithScene( toolPitCubeInfo, sceneObjects );
                        if(housingCollides || tipCollides){
                            collisionCount++;
                            //System.out.println("  Collide point " );
                            // This point will collide so:
                            // 1) We can't add the point to the GCode file, and
                            // 2) We must pull the router out along the B/C axis because we can't be sure the next point travel will collide with the scene.
                            
                            // create a void region that is a long channel angled along toolVector, and remove all generatedCuttingPath
                            // points that fall in that zone.
                            
                            // Add pull out point
                            Vec3 retractPoint = new Vec3(currPoint);
                            retractPoint.add(toolVector.times(4.0));
                            
                            // NOTE: an optimization would be to take into account which machine object collided and retract the length needed not just the full length.
                            
                            
                            Vec3 voidRegionStart = new Vec3(currPoint.plus(toolVector.times( 100 ) ));
                            Vec3 voidRegionEnd = new Vec3(currPoint.minus(toolVector.times( 100 ) ));
                            //int removedCount = 0;
                            for(int p = generatedCuttingPath.size() -1; p >= 0; p--){
                                Vec3 currP = generatedCuttingPath.elementAt(p);
                                
                                Vec3 closestPoint = Intersect2.closestPointToLineSegment( voidRegionStart, voidRegionEnd, currP);
                                double dist = closestPoint.distance(  currP );
                                //System.out.println("  dist " + dist );
                                double getAvgSpan = getAverageSpan(generatedCuttingPath);
                                //System.out.println("  dist " + dist + " getAvgSpan " + getAvgSpan );
                                if(dist < getAvgSpan / 2){
                                    
                                    generatedCuttingPath.setElementAt(retractPoint, p); //
                                    
                                }
                            }
                            
                            //
                            // Fill in gaps created by changes
                            //
                            generatedCuttingPath = fillGapsInPointPath(generatedCuttingPath ); // Fill in gaps created by moving points.
                            generatedCuttingPath = removeDuplicatePoints(generatedCuttingPath); // Remove duplicates if we move multiple points to the same location.
                            //System.out.println("generatedCuttingPath size " + generatedCuttingPath.size()  );
                            
                            
                            // Update
                            Curve tpCurve = (Curve)toolPath.getObject();
                            tpCurve.setVertexPositions(vectorToArray( generatedCuttingPath ));
                            toolPath.clearCachedMeshes();
                            
                            
                            //try { Thread.sleep(30); } catch(Exception e){} // Wait to show collision
                            // Note: This method of retracting to avoid collisions is simple but moves the machine excessivly in some cases.
                            
                        } else {
                            //updatedCuttingPath.addElement(currPoint); // No collision, This point can be safely cut on the machine / GCode.
                        }
                        
                        // Update the avatar object to show to the user where it is in space.
                        currCurve.setVertexPositions(vectorToArray(updatedPoints)); // represents cutter
                        avatarCutterLine.clearCachedMeshes();
                        
                        // Update the scene
                        window.updateImage();
                        try { Thread.sleep(1); } catch(Exception e){} // Wait
                    } // end loop generatedCuttingPath
                    
                    //System.out.println("size " + generatedCuttingPath.size()  + " collisionCount: " + collisionCount );
                    if(collisionCount == 0){
                        running = false; // we are done.
                    }
                }
                
                
                //
                // TODO: check the tool tip geometry for collision with scene.
                // Modify the tool path to accomodate.
                //
                // TODO: Implement this.
                
                
                
                
                // If we have a GCode tool path add it to the scene.
                //if(generatedCuttingPath.size() > 1){
                //    addLineToScene(window, generatedCuttingPath, "GCode Tool Path", true);
                //}
                
                
                // Now simulate the generated tool path to be written to a file.
                try { Thread.sleep(500); } catch(Exception e){}
                System.out.println("Simulating Tool Path.");
                  
                
                // Shrink
                
                //Cube cubeObj = (Cube)cubeInfo.getObject(); // Shrink Cube slightly (assume actual machine is smaller than representation)
                //cubeObj.setSize( cubeObj.getX()*0.75, cubeObj.getY()*0.75, cubeObj.getZ()*0.75 );
                //cubeInfo.setObject(cubeObj);
                //cubeInfo.clearCachedMeshes();
                
                
                //
                // Now simulate cutting of the new GCode which should result in no collisions.
                //
                String gCodeExport = "";
                int collisions = 0;
                //generatedCuttingPath = fillGapsInPointPath(generatedCuttingPath ); // We don't need to do this for the GCode, This is only for demonstration in the simulator.
                for(int i = 0; i < generatedCuttingPath.size(); i++){
                    Vec3 currPoint = generatedCuttingPath.elementAt(i);
                    
                    //  calculate where the cutter would be to when fit to the current region surface point.
                    Vector<Vec3> updatedPoints = new Vector<Vec3>();
                    updatedPoints.addElement(currPoint);
                    updatedPoints.addElement(currPoint.plus(toolVector.times(4))  ); // Make the length of the avatar arbitrary, scale later on.
                    
                    
                    // set location of cube
                    //CoordinateSystem drillTipAvatarCS = cubeInfo.getModelingCoords();
                    //drillTipAvatarCS.setOrigin(currPoint.plus(toolVector.times(1.25))); // In practice the length of this avatar would be scaled to result in the correct length.
                    //cubeInfo.clearCachedMeshes();
                    CoordinateSystem drillHousingAvatarCS = drillBodyCubeInfo.getModelingCoords();
                    drillHousingAvatarCS.setOrigin(currPoint.plus(toolVector.times(1.25))); // In practice the length of this avatar would be scaled to result in the correct length.
                    drillBodyCubeInfo.clearCachedMeshes();
                    
                    CoordinateSystem drillTipAvatarCS = toolPitCubeInfo.getModelingCoords();
                    drillTipAvatarCS.setOrigin(currPoint.plus(toolVector.times(0.09))); //
                    toolPitCubeInfo.clearCachedMeshes();
                    
                    
                    // Check to see if the avatar cutter collides with any object in the scene.
                    
                    // TODO
                    boolean housingCollides = cubeCollidesWithScene( drillBodyCubeInfo, sceneObjects );
                    boolean tipCollides = cubeCollidesWithScene( toolPitCubeInfo, sceneObjects );
                    if(housingCollides || tipCollides){
                        collisions++;
                        System.out.println("ERROR: GCode collision. ");
                        
                        try { Thread.sleep(18); } catch(Exception e){} // Wait to show collision, This shouldn't happen
                    } else {
                        //generatedCuttingPath.addElement(currPoint); // No collision, This point can be safely cut on the machine / GCode.
                        
                        // NOTE: XYZ need to be translated off of surface or cutting point.
                        Vec3 xyzPoint = new Vec3(currPoint);
                        xyzPoint.plus(toolVector.times(2.4)); // Note this value needs to be calculated based on the BC point to tip length.
                        gCodeExport += "x" + xyzPoint.x + " y"+xyzPoint.y +" z"+xyzPoint.z+" b"+b+" c"+c+" f50;\n";
                    }
                    
                    // Update the avatar object to show to the user where it is in space.
                    currCurve.setVertexPositions(vectorToArray(updatedPoints));
                    avatarCutterLine.clearCachedMeshes();
                    
                    // Update the scene
                    window.updateImage();
                    try { Thread.sleep(6); } catch(Exception e){} // Wait
                } // end simulate GCode toolpoath
                System.out.println("Collsisions: " + collisions);
                
                gCodeExport = gCodeExport.substring(0, Math.min(gCodeExport.length(), 3000));
                System.out.println("GCode: (Trimmed) " +   gCodeExport);
                
                
                // More demos to come
                
                
                window.updateImage(); // Update scene
                
                
            }
        }).start();
        
    } // end demo function
    
    
    /**
     * addCubeToScene
     * Description:
     */
    public ObjectInfo addCubeToScene(LayoutWindow window, Vec3 location, double size, String name){
        Cube cube = new Cube(size, size, size);
        CoordinateSystem coords = new CoordinateSystem();
        ObjectInfo info = new ObjectInfo(cube, coords, name);
        
        UndoRecord undo = new UndoRecord(window, false);
        ((LayoutWindow)window).addObject(info, undo);
        return info;
    }
    
    /**
     * addLineToScene
     * Description: Add a line to the scene. Good for debugging 3d scene corrdinates.
     */
    public ObjectInfo addLineToScene(LayoutWindow window, Vec3 start, Vec3 end, String name, boolean visible){
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
        return info;
    }
    
    
    /**
     * addLineToScene
     * Description:
     */
    public ObjectInfo addLineToScene(LayoutWindow window, Vector<Vec3> points, String name, boolean visible){
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
        return info;
    }
    
    
    /**
     * fillGapsInPointPath
     * Description: Evaluate point list, insert mid points in areas where there are gaps.
     * Used to ensure all paths traveled are processed for collisions.
     * Note: This is demonstration code. It does not check for infinite loop conditions.
     */
    public Vector<Vec3> fillGapsInPointPath(Vector<Vec3> regionSurfacePoints){
        double minSpan = 999999; // TODO fix this.
        double avgSpan = 0;
        for(int i = 1; i < regionSurfacePoints.size(); i++){
            Vec3 a = regionSurfacePoints.elementAt(i-1);
            Vec3 b = regionSurfacePoints.elementAt(i);
            double distance = a.distance(b);
            if(distance < minSpan){
                minSpan = distance;
            }
            avgSpan += distance;
        }
        if(regionSurfacePoints.size() > 1){
            avgSpan = avgSpan / regionSurfacePoints.size();
        }
        for(int i = 1; i < regionSurfacePoints.size(); i++){
            Vec3 a = regionSurfacePoints.elementAt(i-1);
            Vec3 b = regionSurfacePoints.elementAt(i);
            double distance = a.distance(b);
            while(distance > avgSpan * 1.5){ // minSpan
                Vec3 insertMid = a.midPoint(b);
                regionSurfacePoints.add( i, insertMid );
                a = regionSurfacePoints.elementAt(i-1);
                b = regionSurfacePoints.elementAt(i);
                distance = a.distance(b);
            }
        }
        return regionSurfacePoints;
    }
    
    
    public double getAverageSpan(Vector<Vec3> regionSurfacePoints){
        double avgSpan = 0;
        for(int i = 1; i < regionSurfacePoints.size(); i++){
            Vec3 a = regionSurfacePoints.elementAt(i-1);
            Vec3 b = regionSurfacePoints.elementAt(i);
            double distance = a.distance(b);
            //if(distance < minSpan){
            //    minSpan = distance;
            //}
            avgSpan += distance;
        }
        if(regionSurfacePoints.size() > 1){
            avgSpan = avgSpan / regionSurfacePoints.size();
        }
        return avgSpan;
    }
    
    /**
     * removeDuplicatePoints
     * Description:
     */
    public Vector<Vec3> removeDuplicatePoints(Vector<Vec3> points){
        double avgSpan = getAverageSpan(points);
        for(int i = points.size() - 1; i > 0 ; i--){
            Vec3 a = points.elementAt(i-1);
            Vec3 b = points.elementAt(i);
            double distance = a.distance(b);
            if(distance < avgSpan / 100){
                points.removeElementAt(i);
            }
        }
        return points;
    }
    
    
    /**
     * vectorToArray
     */
    public Vec3[] vectorToArray(Vector<Vec3> points){
        Vec3[] array = new Vec3[points.size()];
        for(int i = 0; i < points.size(); i++){
            array[i] = (Vec3)points.elementAt(i);
        }
        return array;
    }
    
    
    /**
     * cubeCollidesWithScene
     * Description: Detect if a given object collides with the scene.
     * Note: This function needs to be updated to support multiple objects representing the machine cutter.
     */
    public boolean cubeCollidesWithScene( ObjectInfo detectInfo, Vector<ObjectInfo> sceneObjects ){
        // WORK IN PROGRESS
        boolean collides = false;
        LayoutModeling layout = new LayoutModeling();
        
        Vector<EdgePoints> selectedEdgePoints = new Vector<EdgePoints>();
        Vector<ObjectInfo> selectedObjects = new Vector<ObjectInfo>(); // TEMP this won't work with multiple objects
        
        CoordinateSystem c;
        c = layout.getCoords(detectInfo);
        Mat4 mat4 = c.duplicate().fromLocal();
        
        Object3D selectedObj = detectInfo.getObject();
        TriangleMesh selectedTM = null;
        if(selectedObj instanceof TriangleMesh){
            selectedTM = (TriangleMesh)selectedObj;
        } else if(selectedObj.canConvertToTriangleMesh() != Object3D.CANT_CONVERT){
            selectedTM = selectedObj.convertToTriangleMesh(0.1);
        }
        if(selectedTM != null){
            selectedObjects.addElement(detectInfo);   // Save for exclusion later.
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
                    CoordinateSystem cc;
                    cc = layout.getCoords(currInfo);
                    
                    // Convert object coordinates to world (absolute) coordinates.
                    // The object has its own coordinate system with transformations of location, orientation, scale ect. To see them in absolute world coordinates we need to convert.
                    mat4 = cc.duplicate().fromLocal();
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
                                collides = true;
                                //System.out.println("Collision  object: " + currInfo.getName());
                                return true;
                            }
                        }
                    }
                }
            } // for each object in scene
        }
        return collides;
    }
}

