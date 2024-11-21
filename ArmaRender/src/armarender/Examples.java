/**
 *
 * Nov 1, 2024
 */

package armarender;

import java.util.*;
import armarender.math.*;
import armarender.object.*;
import java.io.PrintWriter;
import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import armarender.ui.ProgressDialog;
import armarender.material.*;
import armarender.texture.*;

public class Examples {
    
    public Examples(){
        
    }
    
    // Container for information representing the router machine geometry.
    public class RouterElementContainer {
        public ObjectInfo element;
        public double location; // Location along axis of B/C cut tip to pivot.
        public double size;
        public boolean affixedToB = true;
        public boolean affixedToC = true;
        public boolean enabled = true;
        public RouterElementContainer(ObjectInfo info, double location, double size){
            this.element = info;
            this.location = location;
            this.size = size;
        }
        public RouterElementContainer(ObjectInfo info, double location, double size, boolean enabled){
            this.element = info;
            this.location = location;
            this.size = size;
            this.enabled = enabled;
        }
    }
    
    public class SurfacePointContainer {
        public Vec3 point;
        public Vec3 normal;
        public int passNumber = 0;
        public boolean onSurface = true;
        public double b = 0;
        public double c = 0;
        public boolean collides = false;
        public boolean finalized = false;
        SurfacePointContainer(Vec3 point, int pasNumber){
            this.point = point;
            this.passNumber = passNumber;
        }
        SurfacePointContainer(Vec3 point, Vec3 normal, int pasNumber){
            this.point = point;
            this.normal = normal;
            this.passNumber = passNumber;
        }
    }
    
    
    
    
    
    /**
     * finishingThreePlusTwoByFour
     * Description: Routine, run three axis XYZ across the scene with a fixed BC cutter axis fouur times against four directions.
     * Parameter for rest machining will remove cutting passes on following passes.
     * @param: Window - access to scene objects.
     */
    public void finishingThreePlusTwo(LayoutWindow window){
        ProgressDialog progressDialog = new ProgressDialog("Calculating");
        progressDialog.setProgress(1);
        (new Thread() {
            public void run() {
                LayoutModeling layout = new LayoutModeling();
                Scene scene = window.getScene();
                
                // Prompt user for:
                // 1) B angle: default 45
                // 2) enable 0-4 direction pass
                // 3) accuracy value
                // 4) display debug processing.
                // 5) collect tool dimensions.
                // 6) tool end type
                
                ThreePlusTwoPrompt prompt = new ThreePlusTwoPrompt(false);
                if(prompt.prompt(false) == false){
                    return;
                }
                
                
                double accuracy = 0.15;
                
                boolean restMachiningEnabled = true;    // Will only cut regions that have not been cut allready by a previous pass.
                
                boolean ballNoseTipType = true; // the geometry of the tip type. [true = ball, false = flat end]
                boolean display = true; // display intermediate steps.
                
                Vector<SurfacePointContainer> scanedSurfacePoints = new Vector<SurfacePointContainer>(); // used to define surface features, and avoid duplicate routing paths over areas allready cut.
                Vector<RouterElementContainer> routerElements = new Vector<RouterElementContainer>();  // : Make list of objects that construct the tool
                
                //String gCode = calculateFinishingRoutingPassWithBC( window, 45, 15, accuracy, restMachiningEnabled, ballNoseTipType, scanedSurfacePoints, 1, display ); // First Pass
                //gCode += calculateFinishingRoutingPassWithBC( window, 45, 15 + 90, accuracy, restMachiningEnabled, ballNoseTipType, scanedSurfacePoints, 2, display );
                //gCode += calculateFinishingRoutingPassWithBC( window, 45, 15 + 180, accuracy, restMachiningEnabled, ballNoseTipType, scanedSurfacePoints, 3, display ); // Second Pass -> Rotated N degrees
                //gCode += calculateFinishingRoutingPassWithBC( window, 45, 15 + 270, accuracy, restMachiningEnabled, ballNoseTipType, scanedSurfacePoints, 3, display );
                
                double b = prompt.getBValue();
                double c = prompt.getCValue();
                accuracy = prompt.getAccuracy();
                display = prompt.getDisplay();
                double speed = prompt.getSpeed();
                restMachiningEnabled = prompt.getRestMachining();
                
                Vector<SurfacePointContainer> toolPath1 = calculateFinishingRoutingPassWithBC( window, b, c,
                                                                                              accuracy, restMachiningEnabled,
                                                                                              ballNoseTipType, scanedSurfacePoints, 1, display,
                                                                                              progressDialog); // First Pass
                String gCode = toolPathToGCode(window, toolPath1, speed);
                
                double toolPathTime = getToolpathTime( toolPath1, speed );
                System.out.println("Machining Time Estimate: " + scene.roundThree(toolPathTime) + " minutes" );
                
                // Append header and footer
                String header =
                "(Arma Automotive Inc.)\n"+
                "(Date: n/a)\n"+
                    "(Duration Minutes: "+ scene.roundThree(toolPathTime) +")\n"+
                    "()\n"+
                    "G90 (Absolute Positioning)\n" +
                    "G94 (Feed Per Minute. f = units per minute)\n" +
                    "G40 (Disable Cutter Radius Compensation)\n" +
                    "G49 (Cancel Tool Length Offset)\n" +
                    "G20 (Inches Mode)\n"+
                    "G17 (XY Plane or flat to ground)\n"+
                    "(T3 M6) (Switch Tool)\n"+
                    "(S9000 M3) (Set Spindle RPM, Clockwise direction)\n"+
                    "G55 (Work Coordinate System selection)"+
                    "\n" +
                    "G01\n";
                                //"//G28 (Return to Machine Home)
                
                String footer = "\n" +
                    "(M5) (Stop Spindle)\n" +
                    "G28  (Return to Machine Home)\n" +
                    "G90 (Absolute Positioning)\n" +
                    "G0 B0. C0. (Orient Router B/C)\n" +
                    "M30 (Program End and Rest)\n";
                            
                
                gCode = header + gCode + footer;
               
                
                //Vector<SurfacePointContainer> toolPath2 = calculateFinishingRoutingPassWithBC( window, 45, 15 + 180, accuracy, restMachiningEnabled, ballNoseTipType, scanedSurfacePoints, 3, display ); // Second Pass -> Rotated N degrees
                //gCode += toolPathToGCode(window, toolPath2 );
                
                window.updateImage(); // Update scene
                
                
                // Write GCode to file..
                try {
                    String fileName =  window.getScene().getName() ==null?"Untitled.ads":window.getScene().getName();
                    int extensionPos = fileName.indexOf(".ads");
                    if(extensionPos != -1){
                        fileName = fileName.substring(0, extensionPos);
                    }
                    
                    String dirString = window.getScene().getDirectory() + System.getProperty("file.separator") + fileName;
                    File d = new File(dirString);
                    if(d.exists() == false){
                        d.mkdir();
                    }
                    
                    // folder for tube bends
                    dirString = window.getScene().getDirectory() + System.getProperty("file.separator") + fileName + System.getProperty("file.separator") + "mill5axis"; // TODO: put the folder 'mill5axis' in a constant somewhere
                    d = new File(dirString);
                    if(d.exists() == false){
                        d.mkdir();
                    }
                    
                    File f = new File( dirString + System.getProperty("file.separator") + "finishing_3+2_"+b+"_"+c+"_"+accuracy+".gcode" );
                    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)));
                    out.write(gCode);
                    out.close();
                    
                    window.loadExportFolder();
                    
                } catch (Exception e){
                    
                }
                
                // Convert tool path to roughing path.
                
                
                
            }
        }).start();
    } // end demo function
    
    
    
    
    
    
    
    /**
     * calculateFinishingRoutingPassWithBC
     * Desscription:
     * Note: This function is missing some features:
     * - This function does not take into account the bounds of the machine and may generate tool paths that excede the capacity of the machine.
     * // , Vector<RouterElementContainer> routerElements
     * @param: Window - Access to scene data.
     * @param: double b - angle of B axis in degree, zero is down.
     * @param: double c - angle of c axis in degrees. range 0- 359.
     * @param: double accuracy -
     */
    public Vector<SurfacePointContainer> calculateFinishingRoutingPassWithBC( LayoutWindow window,
                                             double b,
                                             double c,
                                             double accuracy,
                                             boolean restMachiningEnabled,
                                             boolean ballNoseTipType,
                                             Vector<SurfacePointContainer> scanedSurfacePoints,
                                             int passNumber,
                                             boolean display,
                                            ProgressDialog progressDialog){
        progressDialog.start();
        LayoutModeling layout = new LayoutModeling();
        Scene scene = window.getScene();
        Vector<ObjectInfo> sceneObjects = scene.getObjects();
        
        long startTime = System.currentTimeMillis();
        
        // Router Size information.
        double routerHousingPosition = 1.25;
        double routerHousingSize = 0.75;
        double bitTipPosition = 0.12;
        double bitTipSize = 0.09;  // 0.08   .25 infinite loop
        
        double backEndLength = 5; // inches router housing extends back from the fulcrum.
        
        
        //  we set the length from B/C pivot to tool tip with two values: 1 from the config collete length + the Particular tool length
        // Length of fulcrum to tool tip
        double fulcrumToToolTipLength = 12.6821; // T3
        
        // Collision Properties
        double retractionValue = 0.60; // 0.5 Hhigher means more change, more pull out, Lower means smoother finish, longer processing time.
        
        // Note: This concept could be used by running the following code example 4 times with the
        // following configurations (C=0, B=45), (C=90, B=45), (C=180, B=45), (C=270, B=45)
        // This way each of the sides are covered by at leas one pass.
        
        Vector<Vec3> debugMappingGrid = new Vector<Vec3>(); // pattern of cutting to be projected onto the scene.
        Vector<SurfacePointContainer> regionSurfacePoints = new Vector<SurfacePointContainer>(); // accumulated surface points projected
        Vector<SurfacePointContainer> generatedCuttingPath = new Vector<SurfacePointContainer>(); // GCode cutting path
        Vector<RouterElementContainer> routerElements = new Vector<RouterElementContainer>();  // : Make list of objects that construct the tool
        
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
            
            if(currInfo.getObject() instanceof SceneCamera ||
               currInfo.getObject() instanceof DirectionalLight ||
               currInfo.getPhysicalMaterialId() == 500 ||
               currInfo.getObject() instanceof Curve
               ){
                continue;
            }
            
            BoundingBox currBounds = currInfo.getTranslatedBounds();
            if(sceneBounds == null){
                sceneBounds = currBounds;
            } else {
                sceneBounds.extend(currBounds);
            }
        }
        if(sceneBounds != null){
            double sceneSize = Math.max(sceneBounds.maxx - sceneBounds.minx, Math.max(sceneBounds.maxy - sceneBounds.miny, sceneBounds.maxz - sceneBounds.minz));
            //System.out.println("sceneSize " + sceneSize );
            //System.out.println("accuracy " + accuracy );
            Vec3 sceneCenter = sceneBounds.getCenter();
            
            Vec3 raySubtract = new Vec3(toolVector.times( sceneSize * (2) ) ); //  This is only for debug to show the direction the drill will be pointing
            
            // construct a grid and iterate each coordinate and translate it to the toolVector
        
            // Translate to point in space to project region mapping.
            
            Vec3 regionScan = new Vec3(sceneCenter);
            regionScan.add( toolVector.times(sceneSize) );
            
            // DEBUG Show
            ObjectInfo bcLineInfo = addLineToScene(window, regionScan,  regionScan.minus(raySubtract), "B/C Axis Ray (" + b + "-" + c + ")", false ); // debug show ray cast line
            bcLineInfo.setPhysicalMaterialId(500);
            
            int width = (int)(sceneSize / accuracy);
            int height = (int)(sceneSize / accuracy);
            //System.out.println("width " + width );
            
            width *= 1.5;           // expand (depending on C/B the rotation can cause the scene to expand beyond the rectangular bounds. )
            height *= 1.5;
            
            // Loop through grid
            // TODO: take user or config data on accuracy units, and calibrate grid spacing to that size.
            for(int x = 0; x < width; x++){
                for(int y = 0; y < height; y++){
                    // xy offset to coords, Translate based on 'toolVector'
                    
                    int progress = (int)(((double)x / (double)width) * (double)100);
                    progressDialog.setProgress(progress);
                    
                    Vec3 samplePoint = new Vec3(regionScan);
                    
                    double scaleFactor = accuracy; //  (sceneSize / accuracy);
                    
                    //Vec3 currGridPoint = new Vec3( (double)(x-(width/2)) * 0.2 , (double)(y-(height/2)) * 0.2, (double)(y-(height/2)) * 0.2 ); // First try
                    Vec3 currGridPoint = new Vec3( (double)(x-(width/2)) * scaleFactor, 0, (double)(y-(height/2)) * scaleFactor );
                    
                    if(x % 2 == 0){ // Alternate scan direction on Y pass every other X. This is more effecient as it cuts the travel distance in half.
                        currGridPoint = new Vec3( (double)(x-(width/2)) * scaleFactor, 0, (double)((height-y-1) - (height/2)) * scaleFactor ); // reversed
                    }
                    
                    zRotationMat.transform(currGridPoint); // Apply the B axis transform.
                    yRotationMat.transform(currGridPoint); // Apply the C axis rotation
                    
                    samplePoint.add( currGridPoint ); // shift
                    
                    debugMappingGrid.addElement(currGridPoint);
                    
                    Vec3 samplePointB = samplePoint.minus(raySubtract); // Second point in ray cast
                    // Find collision location
                    Vec3 intersectPoint = null;
                    Vec3 intersectNormal = null; // normal of surface of intersection.
                    for(int i = 0; i < sceneObjects.size(); i++){
                        ObjectInfo currInfo = sceneObjects.elementAt(i);
                        Object3D currObj = currInfo.getObject();
                        //System.out.println("Checking for intersect in: " + currInfo.getName());
                        
                        if( currInfo.getPhysicalMaterialId() == 500 ){
                            continue;
                        }
                        
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
                                
                                Vec3 normal = faceB.minus(faceA).cross(faceC.minus(faceA));
                                double length = normal.length();
                                if (length > 0.0){
                                    normal.scale(1.0/length);
                                }
                                //System.out.println(" normal " + normal + " length "  + length);
                                
                                Vec3 samplePointCollision = Intersect2.getIntersection(samplePoint, samplePointB, faceA, faceB, faceC );
                                if(samplePointCollision != null){ // found intersection.
                                    //System.out.println(" *** ");
                                    if(intersectPoint != null){   // existing intersection exists, check if the new one is closer
                                        double existingDist = regionScan.distance(intersectPoint);
                                        double currrentDist = regionScan.distance(samplePointCollision);
                                        if(currrentDist < existingDist){ // we only want the closest intersection.
                                            intersectPoint = samplePointCollision;
                                            intersectNormal = normal;
                                        }
                                    } else {
                                        intersectPoint = samplePointCollision;
                                        intersectNormal = normal;
                                    }
                                }
                            } // faces
                        } // tri mesh
                    } // loop scene obects
                    if(intersectPoint != null){
                        //System.out.println(" Colision " + intersectPoint + "    intersectNormal: " + intersectNormal );
                        
                        // TODO: If a close point in scanedSurfacePoints exists, then don't include it in the surface to cut as it has allready been done.
                        boolean skipPointAsDuplicate = false; // only skip point if it was added from a different pass!
                        if(restMachiningEnabled){
                            double closestExistingSufacePoint = 9999999;
                            int existingPointCount = 0;
                            for(int pi = 0; pi < scanedSurfacePoints.size(); pi++){
                                SurfacePointContainer spc = scanedSurfacePoints.elementAt(pi);
                                Vec3 currExistingPoint = spc.point;
                                double currDist = intersectPoint.distance(currExistingPoint);
                                if( currDist < (accuracy * 1.5) && passNumber != spc.passNumber ){ // 1.2 // experiment with threshold
                                    skipPointAsDuplicate = true; // old method, doesn't do any overlap
                                    existingPointCount++;
                                }
                            }
                            //System.out.println("existingPointCount " + existingPointCount);
                            if(existingPointCount >= 3){ // we want overlap - needs work
                                skipPointAsDuplicate = true;
                            }
                        }
                        if(skipPointAsDuplicate == false){
                            //System.out.println("intersectNormal " + intersectNormal);
                            SurfacePointContainer spc = new SurfacePointContainer(intersectPoint, intersectNormal, passNumber);
                            spc.b = b;
                            spc.c = c;
                            regionSurfacePoints.addElement(spc); // local intersectPoint
                            scanedSurfacePoints.addElement(spc); // external
                        }
                        //addLineToScene(window, intersectPoint,  intersectPoint.plus(new Vec3(0,1,0)) );
                    }
                } // Y
            } // X
            
            if(debugMappingGrid.size() > 1){
                ObjectInfo line = addLineToScene(window, debugMappingGrid, "Projection Grid (" + b + "-" + c + ")", false);
                line.setPhysicalMaterialId(500);
            }
            
            // add entry and exit points above scene.
            // todo: check machine bounds
            if(regionSurfacePoints.size() > 0){
                SurfacePointContainer firstSpc = regionSurfacePoints.elementAt(0);
                SurfacePointContainer lastSpc = regionSurfacePoints.elementAt(regionSurfacePoints.size() - 1);
                firstSpc.b = b;
                firstSpc.c = c;
                lastSpc.b = b;
                lastSpc.c = c;
                
                // Add entry and exit paths from start position.
                // Note if this entry or exit collide they would beed to be rerouted.
                double maxMachineHeight = 0; // TODO calculate entry and exit points based on the capacity of the machine.
                Vec3 firstRegionSurfacePoint = firstSpc.point;
                Vec3 lastRegionSurfacePoint = lastSpc.point;
                
                
                SurfacePointContainer insertFirstSpc = new SurfacePointContainer( new Vec3(firstRegionSurfacePoint.x, firstRegionSurfacePoint.y + (sceneSize/4), firstRegionSurfacePoint.z), 0 );
                SurfacePointContainer insertLastSpc = new SurfacePointContainer(  new Vec3(lastRegionSurfacePoint.x, lastRegionSurfacePoint.y + (sceneSize/4), lastRegionSurfacePoint.z) , 0);
                
                regionSurfacePoints.add(0, insertFirstSpc); // insert entry
                regionSurfacePoints.add(regionSurfacePoints.size(), insertLastSpc );
                
                // Insert/fill points in gaps. Since the router travels in a straight line between points, we need to check each segment for collisions.
                regionSurfacePoints = fillGapsInPointPathSPC(regionSurfacePoints, accuracy);
                
                // Draw line showing mapped surface
                if(regionSurfacePoints.size() > 1){
                    surfaceMapInfo = addLineToSceneSPC(window, regionSurfacePoints, "Surface Map (" + b + "-" + c + ")", true);
                    surfaceMapInfo.setPhysicalMaterialId(500);
                }
            }
        } // bounds
        
        //progressDialog.close(); // We can't accurately track progress from here on. But at least updates are visible to user.
        
        if(regionSurfacePoints.size() == 0){
            progressDialog.close();
            return new Vector<SurfacePointContainer>();
        }
        
        //
        // Simulate cutting toolpath using regionSurfacePoints calculated from a particular B/C angle.
        //
        System.out.println("Scanning surface points using B/C Tool angle. ");
        Vec3 firstRegionSurfacePoint = regionSurfacePoints.elementAt(0).point;
        Vec3 lastRegionSurfacePoint = regionSurfacePoints.elementAt(regionSurfacePoints.size() - 1).point;
        
        
        ObjectInfo avatarCutterLine = addLineToScene(window, firstRegionSurfacePoint, firstRegionSurfacePoint.plus(toolVector.times(fulcrumToToolTipLength) ), "Cutter (" + b + "-" + c + ")", true );
        avatarCutterLine.setPhysicalMaterialId(500);
        Curve currCurve = (Curve)avatarCutterLine.getObject();
        
        
        // Router Z height base
        ObjectInfo routerZBaseCubeInfo = addCubeToScene(window, firstRegionSurfacePoint.plus(toolVector.times( fulcrumToToolTipLength ) ), 0.5, "Router Base (" + b + "-" + c + ")" );
        routerZBaseCubeInfo.setPhysicalMaterialId(500);
        //routerZBaseCubeInfo.setPhysicalMaterialId(500);
        setObjectBCOrientation(routerZBaseCubeInfo, c,  0); // only C is applied
        routerElements.addElement(  new  RouterElementContainer( routerZBaseCubeInfo, 4, 0.5) );
        
        //
        // Add motor housing
        //
        // This includes multiple objects that represent the router machine.
        // Use to detect collisions.
        ObjectInfo drillBodyCubeInfo = addCubeToScene(window, firstRegionSurfacePoint.plus(toolVector.times( 2.2) ), 0.8, "Router Housing Base (" + b + "-" + c + ")" ); // Cube represents a part of the machine
        drillBodyCubeInfo.setPhysicalMaterialId(500);
        setObjectBCOrientation(drillBodyCubeInfo, c,  b); // Set orientation
        routerElements.addElement(  new  RouterElementContainer( drillBodyCubeInfo, 2.2, 0.8 ) );
        
        ObjectInfo drillBodyBackEndCubeInfo = addCubeToScene(window, firstRegionSurfacePoint.plus(toolVector.times( fulcrumToToolTipLength + backEndLength) ), 0.8, "Router Back End (" + b + "-" + c + ")" ); // Cube represents a part of the machine
        drillBodyBackEndCubeInfo.setPhysicalMaterialId(500);
        setObjectBCOrientation(drillBodyBackEndCubeInfo, c,  b); // Set orientation
        routerElements.addElement(  new  RouterElementContainer( drillBodyBackEndCubeInfo, 5.0, 0.8 ) );
        
        ObjectInfo drillBodyCilynderInfo = addCylinderToScene(window, firstRegionSurfacePoint.plus(toolVector.times(routerHousingPosition) ), routerHousingSize, routerHousingSize,  "Router Housing (" + b + "-" + c + ")" );
        drillBodyCilynderInfo.setPhysicalMaterialId(500);
        setObjectBCOrientation(drillBodyCilynderInfo, c,  b); // Set orientation
        routerElements.addElement(  new  RouterElementContainer( drillBodyCilynderInfo, routerHousingPosition, routerHousingSize) );
        
        // add Collet
        ObjectInfo drillColletInfo = addCylinderToScene(window, firstRegionSurfacePoint.plus(toolVector.times( 0.5 ) ), 0.2, 0.2,  "Collet (" + b + "-" + c + ")" );
        drillColletInfo.setPhysicalMaterialId(500);
        setObjectBCOrientation(drillColletInfo, c,  b);
        routerElements.addElement(  new  RouterElementContainer( drillColletInfo, 0.5, 0.2) );
        // set color
        {
            Texture texture = null;
            for(int i = 0; i < scene.getNumTextures(); i++){
                Texture currTexture = scene.getTexture(i);
                if(currTexture.getName().equals("Green")){
                    texture = currTexture;
                    i = scene.getNumTextures(); // skip
                }
            }
            if(texture == null){
                texture = new UniformTexture();
                texture.setName("Green");
                ((UniformTexture)texture).diffuseColor = new RGBColor(0.1f, 1.0f, 0.1f);
                scene.addTexture(texture);
            }
            UniformMapping mapping = new UniformMapping(drillColletInfo.getObject(), texture);
            drillColletInfo.setTexture( texture, mapping );
        }
        
        // Add tool tip
        //ObjectInfo toolPitCubeInfo = addCubeToScene(window, firstRegionSurfacePoint.plus(toolVector.times( bitTipPosition ) ), bitTipSize, "Bit Tip" ); // Cube represents tip of bit
        ObjectInfo toolPitCubeInfo = addCylinderToScene(window, firstRegionSurfacePoint.plus(toolVector.times( bitTipPosition ) ), bitTipSize, bitTipSize, "Bit Tip (" + b + "-" + c + ")" );
        toolPitCubeInfo.setPhysicalMaterialId(500);
        setObjectBCOrientation(toolPitCubeInfo, c,  b);
        routerElements.addElement( new  RouterElementContainer( toolPitCubeInfo, bitTipPosition, bitTipSize)  );
        
        // Add tool tip ball nose
        ObjectInfo toolBallNoseInfo = addSphereToScene(window, firstRegionSurfacePoint.plus(toolVector.times( 0.0001 ) ), bitTipSize, "Bit Ball Nose (" + b + "-" + c + ")" );
        toolBallNoseInfo.setPhysicalMaterialId(500);
        setObjectBCOrientation(toolBallNoseInfo, c,  b);
        RouterElementContainer ballNoseREC = new  RouterElementContainer( toolBallNoseInfo, 0.0001, bitTipSize, false); // Last parameter is enable
        routerElements.addElement(  ballNoseREC ); // Disabled collisions because BUGGY
        //System.out.println(" Ball size " + ballNoseREC.size + "  loc " + ballNoseREC.location );
       
        
        //
        // Scan surface mesh to create tool path.
        //
        for(int i = 0; i < regionSurfacePoints.size(); i++){
            //Vec3 surfacePoint = regionSurfacePoints.elementAt(i).point;
            //SurfacePointContainer spc = regionSurfacePoints.elementAt(i);
            generatedCuttingPath.addElement(regionSurfacePoints.elementAt(i));
        }
        
        
        //
        // Retract tool based on drill bit tip and angle delta between BC and the surface.
        // This should be more effecient than using the drill tip geometry to collide with the scene for retraction.
        //
        // If ball nose drill bit type or flat end.
        // Note: This code doesn't appear to work but its a first attempt.
        // BUG: It appears that the jagged pattern is caused by missing surface normal values in the cutting path data structure.
        // This causes some points to be retracted and others not.
        if(ballNoseTipType){
            for(int i = 0; i < generatedCuttingPath.size(); i++){
                SurfacePointContainer spc = generatedCuttingPath.elementAt(i);
                Vec3 surfaceNormal = spc.normal;
                if(surfaceNormal == null){                      // Why do some surface points have missing normals??? Might be points from fillGapsInPointPathSPC()
                    //System.out.println(" Why is surface normal null in regionSurfacePoints? " + i );
                    //surfaceNormal = new Vec3(0, -1, 0);
                    surfaceNormal = new Vec3(0, 1, 0);
                    //System.out.println(" normal is null " + i);
                }
                if(surfaceNormal != null){
                    double angle = Vec3.getAngle( toolVector, new Vec3(), surfaceNormal );
                    //System.out.println(" angle: " + angle);
                    if(angle > 90){
                        //angle = 90 - angle; // Incorrect
                        //angle = angle - 90; // Wrong
                        angle = 180 - angle; // Fixed
                    }
                    angle = 90 - angle; // Rotate 90 degrees.
                    
                    //angle = Math.abs(angle); // No
                    double ballNoseRadius = bitTipSize / 2;
                    angle = 90 - angle; // we want the angle from vertical.
                    double retract = retractSphereByAngle(ballNoseRadius, Math.toRadians(angle));
                    //System.out.println(" i " + i + "   retract: " + retract + "   angle: " + angle + " surfaceNormal: " + surfaceNormal);
                    //spc.point = new Vec3(   spc.point.plus(    toolVector.times( retract  )  ) ); // OLD
                    spc.point.add( toolVector.times( retract ) ); // for the current point in the cut path change the position by moving it along the tool path by the retract length.
                    //System.out.println("normal " + surfaceNormal);
                    //System.out.println(" angle " + angle + " retract: " + retract);
                }
            }
        } else { // Flat end tool tip
            
            for(int i = 0; i < generatedCuttingPath.size(); i++){
                SurfacePointContainer spc = generatedCuttingPath.elementAt(i);
                Vec3 surfaceNormal = spc.normal;
                if(surfaceNormal == null){
                    //surfaceNormal = new Vec3(0, -1, 0);
                    surfaceNormal = new Vec3(0, 1, 0);
                    //System.out.println(" normal is null ");
                }
                if(surfaceNormal  != null){
                    double angle = Vec3.getAngle( toolVector, new Vec3(), surfaceNormal );
                    if(angle > 90){
                        //angle = 90 - angle; // ??? incorrect
                        //angle = angle - 90;
                        angle = 180 - angle; // Fixed
                    }
                    angle = 90 - angle; // Rotate 90 degrees.
                    
                    //angle = Math.abs(angle);
                    double ballNoseRadius = bitTipSize / 2;
                    angle = 90 - angle; // we want the angle from vertical.
                    double retract = retractFlatEndByAngle(ballNoseRadius, Math.toRadians(angle));
                    //spc.point = new Vec3(   spc.point.plus(    toolVector.times( retract  )  ) );
                    spc.point.add( toolVector.times( retract ) );
                    //System.out.println("normal " + surfaceNormal);
                    //System.out.println(" angle " + angle + " retract: " + retract);
                }
            }
        }
        
        
        
        
        if(surfaceMapInfo != null){
            surfaceMapInfo.setVisible(false); // Hide surface map because we want to show the GCode cut path now.
        }
        
        // If we have a GCode tool path add it to the scene.
        if(generatedCuttingPath.size() > 1){
            toolPath = addLineToSceneSPC(window, generatedCuttingPath, "GCode Tool Path (" + b + "-" + c + ")", true);
            toolPath.setPhysicalMaterialId(500);
        }
        
        
        //
        // Resolve collisions. Repeat until all collisions resolved.
        //
        boolean running = true;
        int iterationCount = 0;
        while(running){
            iterationCount++;
            int collisionCount = 0;
            System.out.println("Modifying tool path to remove collisions.");
            //Vector<Vec3> updatedCuttingPath = new Vector<Vec3>();
            
            for(int i = 0; i < generatedCuttingPath.size(); i++){
                SurfacePointContainer currSpc = generatedCuttingPath.elementAt(i);
                Vec3 currPoint = currSpc.point;
                
                if(currSpc.finalized){
                    continue; // If this point is final then don't evaluate it for collisions.
                }
                
                int progress = (int)(((double)i / (double)generatedCuttingPath.size()) * (double)100);
                progressDialog.setProgress(progress);
                
                //  calculate where the cutter would be to when fit to the current region surface point.
                Vector<Vec3> updatedPoints = new Vector<Vec3>();
                updatedPoints.addElement(currPoint);
                updatedPoints.addElement(currPoint.plus(toolVector.times( fulcrumToToolTipLength ))  ); // Make the length of the avatar arbitrary, scale later on.
                
                // Update the avatar object to show to the user where it is in space.
                currCurve.setVertexPositions(vectorToArray(updatedPoints)); // represents cutter
                avatarCutterLine.clearCachedMeshes();
                
                // Update router location
                for(int re = 0; re < routerElements.size(); re++){
                    RouterElementContainer rec = routerElements.elementAt(re);
                    ObjectInfo routerElement = rec.element;
                    CoordinateSystem reCS = routerElement.getModelingCoords();
                    reCS.setOrigin(currPoint.plus(toolVector.times(  rec.location + (rec.size / 2)  )));
                    routerElement.clearCachedMeshes();
                }
                
                // Check to see if the avatar cutter collides with any object in the scene.
                
                // Collision detection
                // The generatedCuttingPath can still collide with the scene. Perhaps keep filtering the
                // generatedCuttingPath pulling out more points that collide until there are no more collisions?
                // It appear the path along a side still collides.
                //boolean housingCollides = cubeCollidesWithScene( drillBodyCubeInfo, sceneObjects );
                //boolean tipCollides = cubeCollidesWithScene( toolPitCubeInfo, sceneObjects );
                
                boolean collides = false;
                double retractDistance = retractionValue;
                retractDistance = 0;
                double maxLocation = 0;
                double maxCollision = 0;
                for(int re = 0; re < routerElements.size(); re++){
                    RouterElementContainer rec = routerElements.elementAt(re);
                    ObjectInfo routerElement = rec.element;
                    if(rec.enabled == false){
                        continue;
                    }
                    // rec.location
                    if(objectCollidesWithScene(routerElement, sceneObjects, routerElements)){
                        collides = true;
                        currSpc.collides = true;
                        double currLocationDistance = rec.location - (rec.size / 2);
                        //currLocationDistance = currLocationDistance * 0.5; // this unit might be too much?
                        //retractDistance += (rec.location - (rec.size / 2)); // minus the size of the object?
                        if(currLocationDistance > maxLocation){
                            maxLocation = currLocationDistance;
                        }
                    }
                    
                    // new method - performance penalty
                    if(collides){ // Only check collision distance if we know there is a collision.
                        double collideDist = objectSceneCollisionOffset( toolVector, routerElement, sceneObjects, routerElements );
                        if(collideDist > 0){
                            //System.out.println("Collide dist found " + collideDist);
                            //retractDistance += collideDist;
                            maxCollision = collideDist;
                        }
                    }
                }
                retractDistance = Math.max(maxLocation, maxCollision); // Max or addition. Possible ideas.
                
                //retractDistance = 0.5;
                
                if(collides){
                //if(housingCollides || tipCollides){
                    collisionCount++;
                    //System.out.println("  Collide point " );
                    // This point will collide so:
                    // 1) We can't add the point to the GCode file, and
                    // 2) We must pull the router out along the B/C axis because we can't be sure the next point travel will collide with the scene.
                    
                    // create a void region that is a long channel angled along toolVector, and remove all generatedCuttingPath
                    // points that fall in that zone.
                    
                    // Add pull out point
                    /*
                     // Will be infinite loop with fill gaps. This method will never pull lower points that when subdivided never resolve
                    Vec3 retractPoint = new Vec3(currPoint);
                    retractPoint.add(toolVector.times(retractDistance)); // was 3  retractDistance  retractionValue
                    generatedCuttingPath.setElementAt(retractPoint, i);
                    */
                     
                    // NOTE: an optimization would be to take into account which machine object collided and retract the length needed not just the full length.
                    
                    
                    //
                    // Pull all points in channel.
                    // We do this because when we call fill gaps we create new points that could collide,
                    // We don't want to reciursivly add and remove points in an infinite loop.
                    //
                    Vec3 currPointRetraction = new Vec3(currPoint);
                    currPointRetraction.add(toolVector.times(retractDistance));
                    // Now we don't want to pull the channel further than currPointRetraction on the BC axis
                    Vec3 voidRegionStart = new Vec3(currPoint.plus(toolVector.times( 100 ) ));
                    Vec3 voidRegionEnd = new Vec3(currPoint.minus(toolVector.times( 100 ) ));
                    //int removedCount = 0;
                    int movedCount = 0;
                    for(int p = generatedCuttingPath.size() -1; p >= 0; p--){
                        SurfacePointContainer currPSpc = generatedCuttingPath.elementAt(p);
                        Vec3 currP = currPSpc.point;
                        
                        Vec3 closestPoint = Intersect2.closestPointToLineSegment( voidRegionStart, voidRegionEnd, currP);
                        double dist = closestPoint.distance(  currP );
                        //System.out.println("  dist " + dist );
                        //double getAvgSpan = getAverageSpan(generatedCuttingPath); // old method
                        //System.out.println("  dist " + dist + " getAvgSpan " + getAvgSpan );
                        //if(dist < getAvgSpan / 2){
                        if(dist < accuracy / 4){ // 2
                            movedCount++;
                            
                            // Calculate retractionDistance to give us the same rertaction as currPointRetraction along BC axis.
                            Vec3 currChannelRetractPoint = new Vec3(currP);
                            currChannelRetractPoint.add(toolVector.times(retractDistance));
                            
                            double axisDist = getAxisDistance(currChannelRetractPoint, currPointRetraction, toolVector);
                            if(axisDist > 0){ // too much. We pulled the channel too far.
                                currChannelRetractPoint.add(toolVector.times( -( retractDistance / 1.4 ))); // correct a little
                            }
                                
                            //generatedCuttingPath.setElementAt(retractPoint, p); // NO This causes points to go off course (BC).
                            //generatedCuttingPath.setElementAt(currRetractPoint, p);
                            currPSpc.point = currChannelRetractPoint;
                            currPSpc.onSurface = false; // This point no longer contacts the surface and doesn not aid in adding detail to the parts being manufactured.
                        }
                    }
                    //System.out.println("movedCount " + movedCount + "   i: " + i);
                    
                    //
                    // Fill in gaps created by changes
                    //
                    generatedCuttingPath = fillGapsInPointPathSPC(generatedCuttingPath, accuracy); // Fill in gaps created by moving points.
                    generatedCuttingPath = removeDuplicatePointsSPC(generatedCuttingPath, accuracy); // Remove duplicates if we move multiple points to the same location.
                    generatedCuttingPath = smoothPointsOnAxisSPC(generatedCuttingPath, toolVector, accuracy);
                    
                    // TOOD: it would make more sense to keep an attribute in the generatedCuttingPath to know which points ...
                    // A better smoothing on axis is possible.
                    
                    //System.out.println("generatedCuttingPath size " + generatedCuttingPath.size()  );
                    
                    
                    // Update line representing toolpath
                    Curve tpCurve = (Curve)toolPath.getObject();
                    tpCurve.setVertexPositions(vectorToArraySPC( generatedCuttingPath ));
                    toolPath.clearCachedMeshes();
                    
                    
                    if(display){
                        //try { Thread.sleep(5); } catch(Exception e){} // Wait to show collision
                    }
                    // Note: This method of retracting to avoid collisions is simple but moves the machine excessivly in some cases.
                    
                } else {
                    //updatedCuttingPath.addElement(currPoint); // No collision, This point can be safely cut on the machine / GCode.
                    currSpc.collides = false;
                }
                
                if(display && i % 2 == 0){
                    // Update the scene
                    window.updateImage();
                    try { Thread.sleep(4); } catch(Exception e){} // Wait
                }
            } // end loop generatedCuttingPath for each point in path
            
            
            //
            // Optimization, flag points with lots of adjacent non collisions as being finalized.
            // Theory: A collision will cause modification to adjacent points to resolve but this
            // may introduce new collisions. But long stretches of non collision points are unlikely
            // to be disturbed and thus can be assumed to be finalized.
            //
            for(int i = 12; i < generatedCuttingPath.size(); i++){
                boolean okToFinalize = true;
                for(int regress = 0; regress < 10; regress++){
                    SurfacePointContainer currSpc = generatedCuttingPath.elementAt(i - regress);
                    if(currSpc.collides){
                        okToFinalize = false;
                    }
                }
                if(okToFinalize){
                    SurfacePointContainer currSpc = generatedCuttingPath.elementAt(i - 6);
                    currSpc.finalized = true;
                }
            }
            
            
            //System.out.println("size " + generatedCuttingPath.size()  + " collisionCount: " + collisionCount );
            System.out.println(" Collisions: " + collisionCount );
            System.out.println(" Points in path: " + generatedCuttingPath.size() );
            
            int pointsOnSurface = 0;
            for(int i = 0; i < generatedCuttingPath.size(); i++){
                if(generatedCuttingPath.elementAt(i).onSurface){
                    pointsOnSurface++;
                }
            }
            System.out.println(" Points on surface: " + pointsOnSurface );
            
            double pathLength = 0;
            for(int i = 1; i < generatedCuttingPath.size(); i++){
                Vec3 ap = generatedCuttingPath.elementAt(i - 1).point;
                Vec3 bp = generatedCuttingPath.elementAt(i).point;
                pathLength += ap.distance(bp);
                
            }
            System.out.println(" Path length: " + pathLength );
            
            if(collisionCount == 0){
                running = false; // we are done.
            }
            
            // Show result of iteration.
            // Update the scene
            window.updateImage();
            try { Thread.sleep(6); } catch(Exception e){} // Wait
            
        } // end loop resolve collisions
        System.out.println("Collisions resolved in passes: " + iterationCount);
        
        
        // Now simulate the generated tool path to be written to a file.
        //try { Thread.sleep(200); } catch(Exception e){}
        System.out.println("Simulating Tool Path.");
        
        
        //
        // TODO: trim points to fit in bounds of cutting area.
        // Primary height.
        //
        
        
        //
        // Now simulate cutting of the new GCode which should result in no collisions.
        // NOTE: This is now redundant or for show only. The gcode is made in a new function
        //
        String gCodeExport = "";
        int collisions = 0;
        //generatedCuttingPath = fillGapsInPointPath(generatedCuttingPath ); // We don't need to do this for the GCode, This is only for demonstration in the simulator.
        for(int i = 0; i < generatedCuttingPath.size(); i++){
            Vec3 currPoint = generatedCuttingPath.elementAt(i).point;
            
            int progress = (int)(((double)i / (double)generatedCuttingPath.size()) * (double)100);
            progressDialog.setProgress(progress);
            
            //  calculate where the cutter would be to when fit to the current region surface point.
            Vector<Vec3> updatedPoints = new Vector<Vec3>();
            updatedPoints.addElement(currPoint);
            updatedPoints.addElement(currPoint.plus(toolVector.times(4 + 6))  ); // Make the length of the avatar arbitrary, scale later on.  ********
            // Update the avatar object to show to the user where it is in space.
            currCurve.setVertexPositions(vectorToArray(updatedPoints));
            avatarCutterLine.clearCachedMeshes();
            
            // Update router location.
            for(int re = 0; re < routerElements.size(); re++){
                RouterElementContainer rec = routerElements.elementAt(re);
                ObjectInfo routerElement = rec.element;
                CoordinateSystem reCS = routerElement.getModelingCoords();
                reCS.setOrigin(currPoint.plus(toolVector.times(  rec.location + (rec.size / 2) )));
                routerElement.clearCachedMeshes();
            }
            
            
            // Check to see if the avatar cutter collides with any object in the scene.
            
            // TODO
            //boolean housingCollides = cubeCollidesWithScene( drillBodyCubeInfo, sceneObjects );
            //boolean tipCollides = cubeCollidesWithScene( toolPitCubeInfo, sceneObjects );
            
            boolean collides = false;
            for(int re = 0; re < routerElements.size(); re++){
                RouterElementContainer rec = routerElements.elementAt(re);
                if(rec.enabled == false){
                    continue;
                }
                ObjectInfo routerElement = rec.element;
                if(objectCollidesWithScene(routerElement, sceneObjects, routerElements)){
                    collides = true;
                }
            }
            
            
            //if(housingCollides || tipCollides){
            if(collides){
                collisions++;
                System.out.println("ERROR: GCode collision. ");
                
                // TODO: Add an object to the scene as a marker for the collision so the user knows.
                
                
                //try { Thread.sleep(12); } catch(Exception e){} // Wait to show collision, This shouldn't happen
            } else {
                //generatedCuttingPath.addElement(currPoint); // No collision, This point can be safely cut on the machine / GCode.
                
                // speed
                double speed = 50;
                if( i > 2 && i < generatedCuttingPath.size() - 6 ){
                    Vec3 oppositeP = generatedCuttingPath.elementAt(i - 1).point;
                    Vec3 oppositeP2 = generatedCuttingPath.elementAt(i - 2).point;
                    Vec3 prevPointA = generatedCuttingPath.elementAt(i + 1).point;
                    Vec3 prevPointB = generatedCuttingPath.elementAt(i + 2).point;
                    Vec3 prevPointC = generatedCuttingPath.elementAt(i + 3).point;
                    Vec3 prevPointD = generatedCuttingPath.elementAt(i + 4).point;
                    double currAngle = 180 - Vec3.getAngle( oppositeP, currPoint, prevPointA );
                    double currAngle2 = 180 - Vec3.getAngle( oppositeP2, oppositeP, currPoint );
                    double angleA = 180 - Vec3.getAngle( currPoint, prevPointA, prevPointB );
                    double angleB = 180 - Vec3.getAngle( prevPointA, prevPointB, prevPointC );
                    double angleC = 180 - Vec3.getAngle( prevPointB, prevPointC, prevPointD );
                    
                    if(currAngle > 20){
                        speed = speed * 0.9;
                    }
                    if(currAngle2 > 20){
                        speed = speed * 0.9;
                    }
                    if(angleA > 20){
                        speed = speed * 0.9;
                    }
                    if(angleB > 20){
                        speed = speed * 0.9;
                    }
                    //if(angleC > 20){
                    //    speed = speed * 0.9;
                    //}
                    
                    
                    if(currAngle > 40){
                        speed = speed * 0.8;
                    }
                    if(currAngle2 > 40){
                        speed = speed * 0.8;
                    }
                    if(angleA > 40){
                        speed = speed * 0.8;
                    }
                    if(angleB > 40){
                        speed = speed * 0.8;
                    }
                    //if(angleC > 40){
                    //    speed = speed * 0.8;
                    //}
                }
                
                // NOTE: XYZ need to be translated off of surface or cutting point.
                Vec3 xyzPoint = new Vec3(currPoint);
                xyzPoint.plus(toolVector.times(2.4)); // Note this value needs to be calculated based on the BC point to tip length.
                gCodeExport += "x" + scene.roundThree(xyzPoint.x) +
                    " y"+scene.roundThree(xyzPoint.y) +
                    " z"+ scene.roundThree(xyzPoint.z)+
                    " b"+ scene.roundThree(b)+
                    " c"+scene.roundThree(c)+" f" + (int)speed + ";\n";
                
                
                // Also calculate TCP GCode
                // Not needed for our machine.
                
            }
            
            
            if(display){
                // Update the scene
                window.updateImage();
                try { Thread.sleep(4); } catch(Exception e){} // Wait
            }
        } // end simulate GCode toolpoath
        
        progressDialog.close();
        
        
        System.out.println("Collsisions: " + collisions);
        
        long endTime = System.currentTimeMillis();
        System.out.println("Completion Time: " + ( (endTime - startTime) / 1000  ));
        
        //String shortGCodeExport = gCodeExport.substring(0, Math.min(gCodeExport.length(), 3000));
        //System.out.println("GCode: (Trimmed) " +   shortGCodeExport);
        
        //scene.removeObjectInfo(avatarCutterLine); // remove line
        
        //return gCodeExport;
        return generatedCuttingPath;
    }
    
    
    /**
     * toolPathToGCode
     * Description: Given a tool path, (points defining a surface to cut) generate GCode to control the couter to cut it.
     * @param: window -
     * @param: Vector<> - path
     * @param: speed -
     */
    public String toolPathToGCode(LayoutWindow window, Vector<SurfacePointContainer> generatedCuttingPath, double speed){
        String gCodeExport = "";
        Scene scene = window.getScene();
        //int collisions = 0;
        //generatedCuttingPath = fillGapsInPointPath(generatedCuttingPath ); // We don't need to do this for the GCode, This is only for demonstration in the simulator.
        for(int i = 0; i < generatedCuttingPath.size(); i++){
            SurfacePointContainer spc = generatedCuttingPath.elementAt(i);
            Vec3 currPoint = generatedCuttingPath.elementAt(i).point;
            
            
            Vec3 toolVector = new Vec3(0, 1, 0); //
            Mat4 zRotationMat = Mat4.zrotation(Math.toRadians(spc.b)); // Will be used to orient the inital position of the B axis.
            Mat4 yRotationMat = Mat4.yrotation(Math.toRadians(spc.c)); // Will be used to orient the inital position of the C axis.
            zRotationMat.transform(toolVector); // Apply the B axis transform.
            yRotationMat.transform(toolVector); // Apply the C axis rotation
            toolVector.normalize(); // Normalize to scale the vector to a length of 1.
            //System.out.println("toolVector " + toolVector);
            
            //generatedCuttingPath.addElement(currPoint); // No collision, This point can be safely cut on the machine / GCode.
            
            // speed
            //double speed = 50;
            if( i > 2 && i < generatedCuttingPath.size() - 6 ){
                Vec3 oppositeP = generatedCuttingPath.elementAt(i - 1).point;
                Vec3 oppositeP2 = generatedCuttingPath.elementAt(i - 2).point;
                Vec3 prevPointA = generatedCuttingPath.elementAt(i + 1).point;
                Vec3 prevPointB = generatedCuttingPath.elementAt(i + 2).point;
                Vec3 prevPointC = generatedCuttingPath.elementAt(i + 3).point;
                Vec3 prevPointD = generatedCuttingPath.elementAt(i + 4).point;
                double currAngle = 180 - Vec3.getAngle( oppositeP, currPoint, prevPointA );
                double currAngle2 = 180 - Vec3.getAngle( oppositeP2, oppositeP, currPoint );
                double angleA = 180 - Vec3.getAngle( currPoint, prevPointA, prevPointB );
                double angleB = 180 - Vec3.getAngle( prevPointA, prevPointB, prevPointC );
                double angleC = 180 - Vec3.getAngle( prevPointB, prevPointC, prevPointD );
                
                if(currAngle > 20){
                    speed = speed * 0.9;
                }
                if(currAngle2 > 20){
                    speed = speed * 0.9;
                }
                if(angleA > 20){
                    speed = speed * 0.9;
                }
                if(angleB > 20){
                    speed = speed * 0.9;
                }
                //if(angleC > 20){
                //    speed = speed * 0.9;
                //}
                
                
                if(currAngle > 40){
                    speed = speed * 0.8;
                }
                if(currAngle2 > 40){
                    speed = speed * 0.8;
                }
                if(angleA > 40){
                    speed = speed * 0.8;
                }
                if(angleB > 40){
                    speed = speed * 0.8;
                }
                //if(angleC > 40){
                //    speed = speed * 0.8;
                //}
            }
            
            // NOTE: XYZ need to be translated off of surface or cutting point.
            Vec3 xyzPoint = new Vec3(currPoint);
            xyzPoint.plus(toolVector.times(2.4)); // Note this value needs to be calculated based on the BC point to tip length.
            gCodeExport +=
                "x" + scene.roundThree(xyzPoint.x) +
                " y"+scene.roundThree(xyzPoint.z) +
                " z"+ scene.roundThree(xyzPoint.y) +
                " b"+ scene.roundThree(spc.b)+
                " c"+scene.roundThree(spc.c)+" f" + (int)speed + ";\n";
            
            
            // Also calculate TCP GCode
            // Not needed for our machine.
        
        } // end simulate GCode toolpoath
        return gCodeExport;
    }
    
    /**
     * getToolpathTime
     * Description: Get time in minutes this tool path will take.
     * Asume units are inches and movement is inches per minite.
     */
    public double getToolpathTime( Vector<SurfacePointContainer> generatedCuttingPath, double speed ){
        double time = 0;
        
        for(int i = 1; i < generatedCuttingPath.size(); i++){
            SurfacePointContainer spcA = generatedCuttingPath.elementAt(i-1);
            Vec3 currPointA = generatedCuttingPath.elementAt(i-1).point;
            
            SurfacePointContainer spcB = generatedCuttingPath.elementAt(i);
            Vec3 currPointB = generatedCuttingPath.elementAt(i).point;
            
            double dist = currPointA.distance(currPointB);
            
            double segmentTime = dist  / (speed);
            time += segmentTime;
        }
        
        
        return time;
    }
    
    
    /**
     * toolPathToGCodeTCP
     * Description: A version of the path to GCode that generates TCP GCode for other machines. Not needed right now but possible.
     */
    public String toolPathToGCodeTCP(LayoutWindow window, Vector<SurfacePointContainer> generatedCuttingPath){
        String gCodeExport = "";
        
        return gCodeExport;
    }
    
    
    /**
     * splitToolPathIntoRooughingLayers
     * Description: Given a tool path, split it into loops of layers for roughing.
     * Note: the surface points contain the B/C info.
     */
    public Vector<SurfacePointContainer> splitToolPathIntoRooughingLayers(LayoutWindow window, Vector<SurfacePointContainer> generatedCuttingPath, double layerHeight){
        Vector<SurfacePointContainer> result = new Vector<SurfacePointContainer>();
        
        // TODO:
        
        return result;
    }
    
    
    /**
     * finishingFourPlusOneByTwo
     *  New strategy - comming soon.
     *
     * Concept is to run passes of macine Y(CAD Z) and then X with a fixed C value moving the B along with the XYZ.
     * This method uses more free movement of the B axis across the surfaces hypothetically with better surface accuracy / machine time? Untested.
     *
     * Not convinced this would be significan'tly better than 3+2X4
     */
    public void finishingFourPlusOneByTwo(LayoutWindow window){
        
        // 1) Firstly we need to map surface detail.
        // Propose we create a data structure of surfacepointobjects as scanned from 6 directional axis and don't do any
        // obstruction filtering, i.e. all surfaces get mapped even if they are obstructed by another surface.
        // We do nt want to filter points by distance though.
        
        
        // 2) run two passes of all surface features using a C value first of zero and a second pass with a value of 90.
        // Each pass will attempt to cover all of the surface detail as long as the machine can fit.
        // The B value will need to be adjusted taking into account the surface normal and any collisions.
        // The more B can come close to the surface normal the better for the quality but the more likely of a collision.
        // for each point many XYZ + B options may be tested.
        
        
        
        // TODO: implement
        
    }
    
    
    /**
     * unifiedFiveAxisVolumePath
     * Description: tool path using five axis, roughing and finishing are one bit i.e. unified.
     * Optimize to minimize tool path / machine movement to achive performance.
     * In theory you could move Y with C at 15deg and oscilate B -45 - + 45 and get better performance than keeping the tip stationary.
     */
    public void unifiedFiveAxisVolumePath(LayoutWindow window){
        
        // Generate volumetric Voxel map of part void to cut.
        // generate surface map and use block height.
        
        // Start in the center top,
        // Traveling sales person problem. How to route the voxel points to minimize the travel distance.
        
        
    }
    
    
    
    /**
     * addCubeToScene
     * Description:
     */
    public ObjectInfo addCubeToScene(LayoutWindow window, Vec3 location, double size, String name){
        Cube cube = new Cube(size, size, size);
        CoordinateSystem coords = new CoordinateSystem();
        ObjectInfo info = new ObjectInfo(cube, coords, name);
        info.setPhysicalMaterialId(500);
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
    
    public ObjectInfo addLineToSceneSPC(LayoutWindow window, Vector<SurfacePointContainer> points, String name, boolean visible){
        float[] s_ = new float[points.size()];
        for(int i = 0; i < points.size(); i++){
            s_[i] = 0;
        }
        Vec3 [] vertex = new Vec3[points.size()];
        for(int i = 0; i < points.size(); i++){
            SurfacePointContainer spc = points.elementAt(i);
            vertex[i] = spc.point;
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
     * addCylinderToScene
     * Description:
     */
    public ObjectInfo addCylinderToScene(LayoutWindow window, Vec3 location, double width, double height, String name){
        Cylinder cylinder = new Cylinder(height, width/2, width/2, 1.0);
        CoordinateSystem coords = new CoordinateSystem();
        ObjectInfo info = new ObjectInfo(cylinder, coords, name);
        info.setPhysicalMaterialId(500);
        UndoRecord undo = new UndoRecord(window, false);
        ((LayoutWindow)window).addObject(info, undo);
        return info;
    }
    
    
    /**
     * addSphereToScene
     * Description: Add a Sphere object to the scene and return a reference.
     * @param: WIndow - access to scene where the object will be added.
     * @param: Vec3 location - location in space object is to be placed.
     * @param: double size - initally - xyz width.
     * @param: String name - name to display in object tree.
     */
    public ObjectInfo addSphereToScene(LayoutWindow window, Vec3 location, double size, String name){
        Sphere sphere = new Sphere(size/2, size/2, size/2);
        CoordinateSystem coords = new CoordinateSystem();
        ObjectInfo info = new ObjectInfo(sphere, coords, name);
        info.setPhysicalMaterialId(500);
        UndoRecord undo = new UndoRecord(window, false);
        ((LayoutWindow)window).addObject(info, undo);
        return info;
    }
    
    
    /**
     * fillGapsInPointPathSPC
     * Description: Insert mid points in path. Used to ensure regions between points are covered for movement.
     * @param:
     * @param:
     * @return:
     */
    public Vector<SurfacePointContainer> fillGapsInPointPathSPC(Vector<SurfacePointContainer> regionSurfacePoints, double accuracy){
        for(int i = 1; i < regionSurfacePoints.size(); i++){
            Vec3 a = regionSurfacePoints.elementAt(i-1).point;
            Vec3 b = regionSurfacePoints.elementAt(i).point;
            double distance = a.distance(b);
            
            Vec3 pointNormal = regionSurfacePoints.elementAt(i).normal;
            
            //while(distance > avgSpan * 1.5){ // minSpan
            while(distance > accuracy * 1.9){
                Vec3 insertMid = a.midPoint(b);
                SurfacePointContainer insertSPC = new SurfacePointContainer( insertMid , regionSurfacePoints.elementAt(i-1).passNumber );
                if(pointNormal != null){
                    insertSPC.normal = pointNormal; // Note: Even though inserted points can't be guarenteed to be on a surface, keep the closest normal anyway.
                }
                insertSPC.collides = true; // just in case
                insertSPC.b = regionSurfacePoints.elementAt(i-1).b;
                insertSPC.c = regionSurfacePoints.elementAt(i-1).c;
                insertSPC.onSurface = false; // Inserted points are assumed to not be on a surface.
                regionSurfacePoints.add( i, insertSPC );
                a = regionSurfacePoints.elementAt(i-1).point;
                b = regionSurfacePoints.elementAt(i).point;
                distance = a.distance(b);
            }
        }
        return regionSurfacePoints;
    }
    
    /**
     * fillGapsInPointPath  -- DEPRICATE ---
     * Description: Evaluate point list, insert mid points in areas where there are gaps.
     * Used to ensure all paths traveled are processed for collisions.
     * Note: This is demonstration code. It does not check for infinite loop conditions.
     */
    public Vector<Vec3> fillGapsInPointPath(Vector<Vec3> regionSurfacePoints, double accuracy){
        /*
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
         */
        
        for(int i = 1; i < regionSurfacePoints.size(); i++){
            Vec3 a = regionSurfacePoints.elementAt(i-1);
            Vec3 b = regionSurfacePoints.elementAt(i);
            double distance = a.distance(b);
            //while(distance > avgSpan * 1.5){ // minSpan
            while(distance > accuracy * 1.9){
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
     * Description: remove duplicate points from list.
     * @param:
     * @param:
     */
    public Vector<Vec3> removeDuplicatePoints(Vector<Vec3> points, double accuracy){
        //double avgSpan = getAverageSpan(points);
        for(int i = points.size() - 1; i > 0 ; i--){
            Vec3 a = points.elementAt(i-1);
            Vec3 b = points.elementAt(i);
            double distance = a.distance(b);
            //if(distance < avgSpan / 100){
            if(distance < accuracy / 4){
                points.removeElementAt(i);
            }
        }
        return points;
    }
    
    public Vector<SurfacePointContainer> removeDuplicatePointsSPC(Vector<SurfacePointContainer> points, double accuracy){
        //double avgSpan = getAverageSpan(points);
        for(int i = points.size() - 1; i > 0 ; i--){
            Vec3 a = points.elementAt(i-1).point;
            Vec3 b = points.elementAt(i).point;
            double distance = a.distance(b);
            //if(distance < avgSpan / 100){
            if(distance < accuracy / 4){
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
    
    
    public Vec3[] vectorToArraySPC(Vector<SurfacePointContainer> points){
        Vec3[] array = new Vec3[points.size()];
        for(int i = 0; i < points.size(); i++){
            array[i] = (Vec3)points.elementAt(i).point;
        }
        return array;
    }
    
    
    /**
     * objectCollidesWithScene
     * Description: Detect if a given object collides with the scene.
     * Note: This function needs to be updated to support multiple objects representing the machine cutter.
     * @param: ObjectInfo info - object which is part of the machine used to test collisions with objects in the scene.
     * @param: Vector<ObjectInfo> - all objects in the scene. Also includes the machine objects which need to be excluded from collision detection.
     * TODO: passing in a list of geometry for the router would be better than tagging with an attribute?
     */
    public boolean objectCollidesWithScene( ObjectInfo detectInfo, Vector<ObjectInfo> sceneObjects, Vector<RouterElementContainer> routerElements ){
        boolean collides = false;
        LayoutModeling layout = new LayoutModeling();
        
        Vector<EdgePoints> selectedEdgePoints = new Vector<EdgePoints>();
        Vector<ObjectInfo> selectedObjects = new Vector<ObjectInfo>(); // TEMP this won't work with multiple objects
        
        CoordinateSystem c;
        c = layout.getCoords(detectInfo);
        Mat4 mat4 = c.duplicate().fromLocal();
        
        BoundingBox detectInfoBounds = detectInfo.getTranslatedBounds();
        
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
                if(currInfo.getPhysicalMaterialId() == 500){ // We designate these object types to be non colliding. i.e. we don't care if the
                    continue;
                }
                if( routerElements.contains(currInfo) ){ // Don't check if this router object is colliding with another router object.
                    continue;
                }
                //System.out.println("   Compare with  scene object: " + currInfo.getName() );
                Object3D currObj = currInfo.getObject();
                
                
                // Performance optimization - check detectInfo if in bounds of current object bounds.
                // This could be good but is untested.
                /*
                BoundingBox currInfoBounds = detectInfo.getTranslatedBounds();
                if( detectInfoBounds.intersects(currInfoBounds) == false ){ // The bounds must collide for there to be any collision.
                    System.out.println(" * " + currInfo.getName() );
                    continue;
                }
                */
                
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
    
    
    /**
     * objectSceneCollisionOffset
     * Description: A more advanced version of objectCollidesWithScene but returns information to communicate the extent of the collision.
     *  I.e. how far does the object need to be moved to resolve the collision.
     */
    public double objectSceneCollisionOffset( Vec3 orientation, ObjectInfo detectInfo, Vector<ObjectInfo> sceneObjects, Vector<RouterElementContainer> routerElements ){
        double result = 0;
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
        Vector<Vec3> detectWorldVerts = new Vector<Vec3>();
        if(selectedTM != null){
            selectedObjects.addElement(detectInfo);   // Save for exclusion later.
            MeshVertex[] verts = selectedTM.getVertices();
            for(int v = 0; v < verts.length; v++){  // These translated verts will have the same indexes as the object array.
                Vec3 vert = new Vec3(verts[v].r); // Make a new Vec3 as we don't want to modify the geometry of the object.
                mat4.transform(vert);
                detectWorldVerts.addElement(vert); // add the translated vert to our list.
                //System.out.println("  Vert index: " + v + " - " + vert); // Print vert location XYZ data.
            }
            TriangleMesh.Edge[] edges = selectedTM.getEdges();
            for(int e = 0; e < edges.length; e++){
                TriangleMesh.Edge edge = edges[e];
                Vec3 a = detectWorldVerts.elementAt( edge.v1 );
                Vec3 b = detectWorldVerts.elementAt( edge.v2 );
                EdgePoints ep = new EdgePoints(a, b);
                selectedEdgePoints.addElement(ep);
            }
        }
        
        // Calculate the point furthest according to the orientation.
        Vec3 furthestPoint = null;
        
        
        double intersectDistance = 0;
        
        
        if(selectedEdgePoints.size() > 0){
            //System.out.println(" edges " + selectedEdgePoints.size() );
            // Scan through objects in the scene to see if there is a collistion.
            for(int i = 0; i < sceneObjects.size(); i++){
                ObjectInfo currInfo = sceneObjects.elementAt(i);
                if( selectedObjects.contains(currInfo) ){          // Don't compare with self
                    continue;
                }
                if(currInfo.getPhysicalMaterialId() == 500){ // We designate these object types to be non colliding. i.e. we don't care if the
                    continue;
                }
                if( routerElements.contains(currInfo) ){ // Don't check if this router object is colliding with another router object.
                    continue;
                }
                //System.out.println("   Compare with  scene object: " + currInfo.getName() );
                Object3D currObj = currInfo.getObject();
                
                double longestCollisionDist = 0;
                
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
                            
                            
                            Vec3 samplePointCollision = Intersect2.getIntersection(edgePoint.a, edgePoint.b, faceA, faceB, faceC );
                            if(samplePointCollision != null && samplePointCollision.x != 0  ){
                                //System.out.println("Collsion ");
                                
                                
                                double collisionDistance = getCollisionDistance( samplePointCollision, edgePoint.a, edgePoint.b, orientation );
                                //double collisionDistance = Vec3.distanceOnAxis(edgePoint.a, edgePoint.b, orientation);
                                
                                if(collisionDistance > longestCollisionDist){
                                    longestCollisionDist = collisionDistance;
                                    result = longestCollisionDist;
                                }
                                
                            }
                        }
                    }
                }
            } // for each object in scene
        }
        return result;
    }
    
    
    /**
     * setObjectBCOrientation
     * Description:
     */
    public void setObjectBCOrientation(ObjectInfo info, double c, double b){
        CoordinateSystem bcCS;
        bcCS = info.getCoords();
        CoordinateSystem zeroCS = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
        Mat4 mat4 = zeroCS.fromLocal();
        mat4 = mat4.zrotation( Math.toRadians(b) );
        bcCS.transformAxes(mat4);
        mat4 = mat4.yrotation( Math.toRadians(c) );
        bcCS.transformAxes(mat4);
        info.setCoords(bcCS);
    }
    
    
    /**
     *
     *
     */
    public double getCollisionDistance( Vec3 samplePointCollision, Vec3 a, Vec3 b, Vec3 orientation ){
        // Find the side we want to move from
        Vec3 targetDirection = new Vec3(samplePointCollision);
        targetDirection.add(orientation);
        double aDist = a.distance( targetDirection );
        double bDist = b.distance( targetDirection );
        Vec3 collidedObjectPoint = null;
        if(aDist < bDist){
            collidedObjectPoint = new Vec3(b);
        } else {
            collidedObjectPoint = new Vec3(a);
        }
        // samplePointCollision and collidedObjectPoint
        
        Vec3 temp = new Vec3(collidedObjectPoint); //
        temp.subtract(samplePointCollision );
        
        
        Vec3 perpendicularOrientation = new Vec3(orientation);
        perpendicularOrientation = perpendicularOrientation.cross(new Vec3(0,1,0));
        perpendicularOrientation.normalize();
        double angle = Vec3.getAngle( orientation, new Vec3(0, 0, 0), new Vec3(0, 1, 0));
        
        Mat4 orientationMat4 = Mat4.axisRotation(perpendicularOrientation, Math.toRadians(angle));

        orientationMat4.transform(temp);
        
        double collisionDistance = -temp.y;
    
        return collisionDistance;
    }
    
    
    /**
     * smoothPointsOnAxis
     * Description: ...
     * NOTE: It would be better to know if a point has been retracted from a surface. Because these points are not useful and we know we can move them without losing surface detail.
     */
    public Vector<SurfacePointContainer> smoothPointsOnAxisSPC(Vector<SurfacePointContainer> generatedCuttingPath, Vec3 orientation, double accuracy){
        for(int i = 2; i < generatedCuttingPath.size(); i++){
            Vec3 pointA = generatedCuttingPath.elementAt(i-2).point;
            Vec3 pointB = generatedCuttingPath.elementAt(i-1).point;
            Vec3 pointC = generatedCuttingPath.elementAt(i).point;
            //double distance = pointA.distance(pointB);
            
            SurfacePointContainer spcA = generatedCuttingPath.elementAt(i-2);
            SurfacePointContainer spcB = generatedCuttingPath.elementAt(i-1);
            SurfacePointContainer spcC = generatedCuttingPath.elementAt(i);
            
            boolean allOffSurface = false;
            if(spcA.onSurface == false && spcB.onSurface == false && spcC.onSurface == false){
                allOffSurface = true;
            }
            
            double segmentAngle = Vec3.getAngle(pointA, pointB, pointC);
            segmentAngle = 180 - segmentAngle;
            if(segmentAngle > 90){
                //System.out.println("angle: " +segmentAngle );
            }
            
            Vec3 tempA = new Vec3(pointA);
            tempA.subtract(pointB);
            
            Vec3 tempC = new Vec3(pointC);
            tempC.subtract(pointB);
            
            Vec3 perpendicularOrientation = new Vec3(orientation);
            perpendicularOrientation = perpendicularOrientation.cross(new Vec3(0,1,0));
            perpendicularOrientation.normalize();
            double angle = Vec3.getAngle( orientation, new Vec3(0, 0, 0), new Vec3(0, 1, 0));
            Mat4 orientationMat4 = Mat4.axisRotation(perpendicularOrientation, Math.toRadians(angle));

            orientationMat4.transform(tempA);
            orientationMat4.transform(tempC);
            
            if( tempA.y < 0 || tempC.y < 0 ){ // Only modify if moving B up.
                continue;
            }
            
            double axisDelta = 0;
            if( tempA.distance(new Vec3()) > tempC.distance(new Vec3()) ){  // choose larger / higher
                axisDelta = tempA.y;
            } else {
                axisDelta = tempC.y;
            }
            
            //if( Math.abs(axisDelta) > accuracy / 4 && segmentAngle > 140 ){ // 2
            if(  allOffSurface  && Math.abs(axisDelta) > accuracy / 16   && segmentAngle > 20  ){
                
                //System.out.println("Smooth correct i " + i  + " " + axisDelta);
                // Transform the lower point
                Vec3 retractPoint = new Vec3(pointB);
                retractPoint.add(orientation.times( Math.abs(axisDelta) ));
                
                //generatedCuttingPath.setElementAt(retractPoint, i-1); // Update pointB
                generatedCuttingPath.elementAt(i-1).point = retractPoint;
            }
        }
        return generatedCuttingPath;
    }
    
    
    /**
     * getAxisDistance
     * Description: Calculate difference between two points as measured along an orientation axis.
     * @param: Vec3 a -
     * @param: Vec3 b -
     * @param: Vec3 orientation -
     */
    public double getAxisDistance(Vec3 a, Vec3 b, Vec3 orientation){
        double result = 0;
        Vec3 temp = new Vec3(a);
        temp.subtract(b);
        Vec3 perpendicularOrientation = new Vec3(orientation);
        perpendicularOrientation = perpendicularOrientation.cross(new Vec3(0,1,0));
        perpendicularOrientation.normalize();
        double angle = Vec3.getAngle(orientation, new Vec3(0, 0, 0), new Vec3(0, 1, 0));
        Mat4 orientationMat4 = Mat4.axisRotation(perpendicularOrientation, Math.toRadians(angle));
        orientationMat4.transform(temp);
        result = temp.y;
        return result;
    }
    
    
    /**
     * calculateCircleDepthByAngle
     * Description: Calculate the depth of a
     * NOTE: Angle values of 90 could produce very large values.
     * @param: double circleRadius -
     * @param: double centerlineAngle -
     * @return: double length the circle decends below.
     */
    public double calculateCircleDepthByAngle(double circleRadius, double centerlineAngle){
        double result = 0;
        double ninty = Math.toRadians(90);
        if(centerlineAngle < 0){
            centerlineAngle = 0;
        }
        if(centerlineAngle > ninty){
            centerlineAngle = ninty;
        }
        double centerToLineVert = circleRadius * Math.sin(ninty - centerlineAngle);
        result = circleRadius - centerToLineVert;
        return result;
    }
    
    
    /**
     * retractSphereByAngle
     * Description: Calculate length the sphere needs to be retracted along an axis to resolve collision.
     *
     * @param: double radius - curcle/ sphere radius.
     * @param: double center line angle - axis of direction as measured from a vertical orientation.
     * @return: double length to retract along axis of angle. Because its a sphere any direction is the same.
     */
    public double retractSphereByAngle(double circleRadius, double centerlineAngle){
        double result = 0;
        double ninty = Math.toRadians(90);
        double height = calculateCircleDepthByAngle(circleRadius,  centerlineAngle);
        double width = height * Math.tan(centerlineAngle);  //
        double distance = Math.sqrt(height * height + width* width); //
        return distance;
    }
    
    
    /**
     * retractFlatEndByAngle
     * Description: Calculate how far a flat tip tool needs to be retracted along an angle given its width when the center is placed on a surface
     * such that the tool will no longer be colliding with the surface.
     * @param: double bitRadius -
     * @param: double centerlineAngle -
     * @param: double length.
     */
    public double retractFlatEndByAngle(double bitRadius, double centerlineAngle){
        double result = 0;
        
        //
        result = bitRadius * Math.tan( centerlineAngle ); // b = a × tan(β)
        
        return result;
    }
    
    
    //
    // Old
    //
    
    /**
     * This example will move a selected object along a orientation vector by the length of detected collision.
     * This should result in the object resting outside the collision.
     */
    public void intersectDistanceDemo(LayoutWindow window){
        LayoutModeling layout = new LayoutModeling();
        Scene scene = window.getScene();
        Vector<ObjectInfo> sceneObjects = scene.getObjects();
        
        Vec3 orientation = new Vec3(-.7, .7, 0);
        orientation.normalize();
        //addLineToScene(window, new Vec3(0,0,0), orientation, "Orientation", true ); // CORRECT
        
        boolean selectionCollides = false;
        int sel[] = window.getSelectedIndices();
        Vector<EdgePoints> selectedEdgePoints = new Vector<EdgePoints>(); // TriangleMesh.Edge
        if(sel.length > 0){
            Vector<ObjectInfo> selectedObjects = new Vector<ObjectInfo>();
            Vector<Vec3> selectionWorldVerts = new Vector<Vec3>();
            
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
                        
                        for(int v = 0; v < verts.length; v++){  // These translated verts will have the same indexes as the object array.
                            Vec3 vert = new Vec3(verts[v].r); // Make a new Vec3 as we don't want to modify the geometry of the object.
                            mat4.transform(vert);
                            selectionWorldVerts.addElement(vert); // add the translated vert to our list.
                            //System.out.println("  Vert index: " + v + " - " + vert); // Print vert location XYZ data.
                        }
                        TriangleMesh.Edge[] edges = selectedTM.getEdges();
                        for(int e = 0; e < edges.length; e++){
                            TriangleMesh.Edge edge = edges[e];
                            Vec3 a = selectionWorldVerts.elementAt( edge.v1 );
                            Vec3 b = selectionWorldVerts.elementAt( edge.v2 );
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
                    
                    double longestCollisionDist = 0;
                    
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
                                
                                Vec3 samplePointCollision = Intersect2.getIntersection(edgePoint.a, edgePoint.b, faceA, faceB, faceC );
                                if(samplePointCollision != null && samplePointCollision.x != 0  ){
                                    //System.out.println("Collsion ");
                                    
                                    //addLineToScene(window, samplePointCollision,  samplePointCollision.minus( new Vec3(0,1,0) ), "Collision", true ); // OK
                                    //addLineToScene(window, samplePointCollision, samplePointCollision.plus(new Vec3(0,2,0)), "Collision", true );
                                    
                                    double collisionDistance = getCollisionDistance( samplePointCollision, edgePoint.a, edgePoint.b, orientation );
                                    //double collisionDistance = Vec3.distanceOnAxis(edgePoint.a, edgePoint.b, orientation);
                                    
                                    if(collisionDistance > longestCollisionDist){
                                        longestCollisionDist = collisionDistance;
                                    }
                                    
                                    
                                    //System.out.println("collisionDistance " + collisionDistance);
                                    
                                    Vec3 showPointer = new Vec3(orientation);
                                    showPointer.normalize();
                                    showPointer = showPointer.times(collisionDistance);
                                    
                                    //System.out.println(" test " +  showPointer.distance(new Vec3())  );
                                    //addLineToScene(window, collidedObjectPoint, collidedObjectPoint.plus( showPointer )  , "-TEST: " + " " + collisionDistance, true );
                                }
                                //if(Intersect2.intersects(edgePoint.a, edgePoint.b, faceA, faceB, faceC)){
                                //    selectionCollides = true;
                                //    System.out.println("Collision  object: " + currInfo.getName());
                                //}
                            }
                        }
                    }
                    if(longestCollisionDist > 0){
                        Vec3 showPointer = new Vec3(orientation);
                        showPointer.normalize();
                        showPointer = showPointer.times(longestCollisionDist);
                        
                        for(int s = 0; s < selectedObjects.size(); s++){
                            ObjectInfo sInfo = selectedObjects.elementAt(s);

                            CoordinateSystem reCS = sInfo.getModelingCoords();
                            Vec3 currOrigin = reCS.getOrigin();
                            
                            reCS.setOrigin( currOrigin.plus(  showPointer  ) );  // showPointer
                            sInfo.clearCachedMeshes();
                            
                            System.out.println("Moving object. distance: " + longestCollisionDist);
                        }
                        window.updateImage();
                    }
                } // for each object in scene
            }
            //System.out.println("Selection collides: " + selectionCollides ); // Print result
        } else {
            System.out.println("No objects selected to check for collisions.");
        }
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
                {   // test each of the approach directions.
                    //c = 0;
                    //c = 90;
                    //c = 180;
                    //c = 270;
                }
                
                double accuracy = 0.2;
                //accuracy = 0.1;
                boolean restMachiningEnabled = true;    // Will only cut regions that have not been cut allready by a previous pass.
                
                boolean ballNoseTipType = true; // the geometry of the tip type.
                boolean display = true; // display intermediate steps.
                
                Vector<SurfacePointContainer> scanedSurfacePoints = new Vector<SurfacePointContainer>(); // used to define surface features, and avoid duplicate routing paths over areas allready cut.
                Vector<RouterElementContainer> routerElements = new Vector<RouterElementContainer>();  // : Make list of objects that construct the tool
                
                //String gCode1 = calculateFinishingRoutingPassWithBC( window, 45, 15, accuracy, restMachiningEnabled, ballNoseTipType, scanedSurfacePoints, 1, display ); // First Pass
                //gCode1 += calculateFinishingRoutingPassWithBC( window, 45, 15 + 180, accuracy, restMachiningEnabled, ballNoseTipType, scanedSurfacePoints, 2, display ); // Second Pass -> Rotated N degrees
                
                
                
                // More demos to come
                
                
                window.updateImage(); // Update scene
            }
        }).start();
    } // end demo function
    
    
    
    // Purpose is to visualize geometry of router shape given user input.
    // not implemented yet
    public void constructRouterGeometry(LayoutWindow window){
        LayoutModeling layout = new LayoutModeling();
        Scene scene = window.getScene();
        double c = 15; // rotation, 0-360.
        double b = 45; // angle 45 degrees.
        
        // Router Size information.
        double routerHousingPosition = 1.25;
        double routerHousingSize = 0.75;
        double bitTipPosition = 0.12;
        double bitTipSize = 0.08;
        
        Vec3 toolVector = new Vec3(0, 1, 0); //
        Mat4 zRotationMat = Mat4.zrotation(Math.toRadians(b)); // Will be used to orient the inital position of the B axis.
        Mat4 yRotationMat = Mat4.yrotation(Math.toRadians(c)); // Will be used to orient the inital position of the C axis.
        zRotationMat.transform(toolVector); // Apply the B axis transform.
        yRotationMat.transform(toolVector); // Apply the C axis rotation
        toolVector.normalize(); // Normalize to scale the vector to a length of 1.
        System.out.println("toolVector " + toolVector);
        
        
        Vec3 firstRegionSurfacePoint = new Vec3();
        Vector<RouterElementContainer> routerElements = new Vector<RouterElementContainer>();
        
        
        
        // Router Z height base
        ObjectInfo routerZBaseCubeInfo = addCubeToScene(window, firstRegionSurfacePoint.plus(toolVector.times( 4 ) ), 0.75, "Router Base (" + b + "-" + c + ")" );
        routerZBaseCubeInfo.setPhysicalMaterialId(500);
        //routerZBaseCubeInfo.setPhysicalMaterialId(500);
        setObjectBCOrientation(routerZBaseCubeInfo, c,  0); // only C is applied
        routerElements.addElement(  new  RouterElementContainer( routerZBaseCubeInfo, 4, 0.75) );
        
        //
        // Add motor housing
        //
        // This includes multiple objects that represent the router machine.
        // Use to detect collisions.
        ObjectInfo drillBodyCubeInfo = addCubeToScene(window, firstRegionSurfacePoint.plus(toolVector.times( 2.2) ), 0.8, "Router Housing Base (" + b + "-" + c + ")" ); // Cube represents a part of the machine
        drillBodyCubeInfo.setPhysicalMaterialId(500);
        setObjectBCOrientation(drillBodyCubeInfo, c,  b); // Set orientation
        routerElements.addElement(  new  RouterElementContainer( drillBodyCubeInfo, 2.2, 0.8 ) );
        
        ObjectInfo drillBodyBackEndCubeInfo = addCubeToScene(window, firstRegionSurfacePoint.plus(toolVector.times( 5.0) ), 0.8, "Router Back End (" + b + "-" + c + ")" ); // Cube represents a part of the machine
        drillBodyBackEndCubeInfo.setPhysicalMaterialId(500);
        setObjectBCOrientation(drillBodyBackEndCubeInfo, c,  b); // Set orientation
        routerElements.addElement(  new  RouterElementContainer( drillBodyBackEndCubeInfo, 5.0, 0.8 ) );
        
        ObjectInfo drillBodyCilynderInfo = addCylinderToScene(window, firstRegionSurfacePoint.plus(toolVector.times(routerHousingPosition) ), routerHousingSize, routerHousingSize,  "Router Housing (" + b + "-" + c + ")" );
        drillBodyCilynderInfo.setPhysicalMaterialId(500);
        setObjectBCOrientation(drillBodyCilynderInfo, c,  b); // Set orientation
        routerElements.addElement(  new  RouterElementContainer( drillBodyCilynderInfo, routerHousingPosition, routerHousingSize) );
        
        // add Collet
        ObjectInfo drillColletInfo = addCylinderToScene(window, firstRegionSurfacePoint.plus(toolVector.times( 0.5 ) ), 0.2, 0.2,  "Collet (" + b + "-" + c + ")" );
        drillColletInfo.setPhysicalMaterialId(500);
        
        // set color
        {
            UniformMaterial redMat = new UniformMaterial();
            redMat.matColor = new RGBColor(1.0f, 0.0f, 0.0f);
            UniformMaterialMapping mapping = new UniformMaterialMapping( drillColletInfo.getObject(), redMat);
            drillColletInfo.getObject().setMaterial( redMat, mapping );
        }
            
        setObjectBCOrientation(drillColletInfo, c,  b);
        routerElements.addElement(  new  RouterElementContainer( drillColletInfo, 0.5, 0.2) );
        
        // Add tool tip
        //ObjectInfo toolPitCubeInfo = addCubeToScene(window, firstRegionSurfacePoint.plus(toolVector.times( bitTipPosition ) ), bitTipSize, "Bit Tip" ); // Cube represents tip of bit
        ObjectInfo toolPitCubeInfo = addCylinderToScene(window, firstRegionSurfacePoint.plus(toolVector.times( bitTipPosition ) ), bitTipSize, bitTipSize, "Bit Tip (" + b + "-" + c + ")" );
        toolPitCubeInfo.setPhysicalMaterialId(500);
        setObjectBCOrientation(toolPitCubeInfo, c,  b);
        routerElements.addElement( new  RouterElementContainer( toolPitCubeInfo, bitTipPosition, bitTipSize)  );
        
        // Add tool tip ball nose
        ObjectInfo toolBallNoseInfo = addSphereToScene(window, firstRegionSurfacePoint.plus(toolVector.times( 0.0001 ) ), bitTipSize, "Bit Ball Nose (" + b + "-" + c + ")" );
        toolBallNoseInfo.setPhysicalMaterialId(500);
        setObjectBCOrientation(toolBallNoseInfo, c,  b);
        RouterElementContainer ballNoseREC = new  RouterElementContainer( toolBallNoseInfo, 0.0001, bitTipSize, false); // Last parameter is enable
        routerElements.addElement(  ballNoseREC ); // Disabled collisions because BUGGY
        //System.out.println(" Ball size " + ballNoseREC.size + "  loc " + ballNoseREC.location );
    }
    
    
    /**
     * roughingThreePlusTwo  
     * Description: generate tool path for roughing pass using 3+2BY4 passes.
     * Enter a pass height by user.
     */
    public void roughingThreePlusTwo(LayoutWindow window){
        ProgressDialog progressDialog = new ProgressDialog("Calculating");
        progressDialog.setProgress(1);
        (new Thread() {
            public void run() {
                LayoutModeling layout = new LayoutModeling();
                Scene scene = window.getScene();
        
                ThreePlusTwoPrompt prompt = new ThreePlusTwoPrompt(true);
                if(prompt.prompt(true) == false){
                    return;
                }
                
                Vector<SurfacePointContainer> scanedSurfacePoints = new Vector<SurfacePointContainer>(); // used to define surface features, and avoid duplicate routing paths over areas allready cut.
                Vector<RouterElementContainer> routerElements = new Vector<RouterElementContainer>();  // : Make list of objects that construct the tool
                
                // Prompt user for:
                // pass height
                // Drill bit width
                double maxCutDepth = 0.5; // only cut depth at one time.
                double drillBitWidth = 0.2;
                double layerHeight = 3; // define the height of the material to be cut. Will be higher than the geometry of the scene.
                double accuracy = 0.15;
                
                boolean restMachiningEnabled = true;    // Will only cut regions that have not been cut allready by a previous pass.
                boolean ballNoseTipType = true; // the geometry of the tip type. [true = ball, false = flat end]
                boolean display = true; // display intermediate steps.
                
                
                // 1) Scan scene geometry into surface map.
                
                double b = prompt.getBValue();
                double c = prompt.getCValue();
                accuracy = prompt.getAccuracy();
                layerHeight = prompt.getDepth();
                display = prompt.getDisplay();
                double speed = prompt.getSpeed();
                
                Vector<SurfacePointContainer> toolPath1 = calculateRoughingRoutingPassWithBC( window, b, c, accuracy, layerHeight, restMachiningEnabled, ballNoseTipType, scanedSurfacePoints, 1, display,
                                                                                             progressDialog); // First Pass
                String gCode = toolPathToGCode(window, toolPath1, speed);
                
                double toolPathTime = getToolpathTime( toolPath1, speed );
                System.out.println("Machining Time Estimate: " + scene.roundThree(toolPathTime) + " minutes" );
                
                // Append header and footer
                String header =
                "(Arma Automotive Inc.)\n"+
                "(Date: n/a)\n"+
                "(Duration Minutes: " + scene.roundThree(toolPathTime) + ")"+
                "()\n"+
                    "G90 (Absolute Positioning)\n" +
                    "G94 (Feed Per Minute. f = units per minute)\n" +
                    "G40 (Disable Cutter Radius Compensation)\n" +
                    "G49 (Cancel Tool Length Offset)\n" +
                    "G20 (Inches Mode)\n"+
                    "G17 (XY Plane or flat to ground)\n"+
                    "(T3 M6) (Switch Tool)\n"+
                    "(S9000 M3) (Set Spindle RPM, Clockwise direction)\n"+
                    "G55 (Work Coordinate System selection)"+
                    "\n" +
                    "G01\n";
                                //"//G28 (Return to Machine Home)
                
                String footer = "\n" +
                    "(M5) (Stop Spindle)\n" +
                    "G28  (Return to Machine Home)\n" +
                    "G90 (Absolute Positioning)\n" +
                    "G0 B0. C0. (Orient Router B/C)\n" +
                    "M30 (Program End and Rest)\n";
                            
                
                gCode = header + gCode + footer;
               
                
                window.updateImage(); // Update scene
                
                
                // Write GCode to file..
                try {
                    String fileName =  window.getScene().getName() ==null?"Untitled.ads":window.getScene().getName();
                    int extensionPos = fileName.indexOf(".ads");
                    if(extensionPos != -1){
                        fileName = fileName.substring(0, extensionPos);
                    }
                    
                    String dirString = window.getScene().getDirectory() + System.getProperty("file.separator") + fileName;
                    File d = new File(dirString);
                    if(d.exists() == false){
                        d.mkdir();
                    }
                    
                    // folder for tube bends
                    dirString = window.getScene().getDirectory() + System.getProperty("file.separator") + fileName + System.getProperty("file.separator") + "mill5axis"; // TODO: put the folder 'mill5axis' in a constant somewhere
                    d = new File(dirString);
                    if(d.exists() == false){
                        d.mkdir();
                    }
                    
                    File f = new File( dirString + System.getProperty("file.separator") + "roughing_3+2_"+b+"_"+c+"_"+accuracy+".gcode" );
                    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)));
                    out.write(gCode);
                    out.close();
                    
                    window.loadExportFolder();
                    
                } catch (Exception e){
                    
                }
                
                
                //window.loadExportFolder();
            }
        }).start();
    }
    
    
    /**
     * calculateRoughingRoutingPassWithBC
     * Description:
     * @param: window -
     * @param: c -
     * @param: b -
     * @param: accuracy -
     * @param: restMachiningEnabled -
     * @param:ballNoseTipType - [ false | true ]
     * @param:scanedSurfacePoints -
     * @param: passNumber -
     */
    public Vector<SurfacePointContainer> calculateRoughingRoutingPassWithBC( LayoutWindow window,
                                             double b,
                                             double c,
                                             double accuracy,
                                            double layerHeight,
                                             boolean restMachiningEnabled,
                                             boolean ballNoseTipType,
                                             Vector<SurfacePointContainer> scanedSurfacePoints,
                                             int passNumber,
                                            boolean display,
                                            ProgressDialog progressDialog){
        progressDialog.start();
        LayoutModeling layout = new LayoutModeling();
        Scene scene = window.getScene();
        Vector<ObjectInfo> sceneObjects = scene.getObjects();
        
        long startTime = System.currentTimeMillis();
        
        // Router Size information.
        double routerHousingPosition = 1.25;
        double routerHousingSize = 0.75;
        double bitTipPosition = 0.12;
        double bitTipSize = 0.09;  // 0.08   .25 infinite loop
        
        double backEndLength = 5; // inches router housing extends back from the fulcrum.
        
        
        //  we set the length from B/C pivot to tool tip with two values: 1 from the config collete length + the Particular tool length
        // Length of fulcrum to tool tip
        double fulcrumToToolTipLength = 12.6821; // T3
        
        // Collision Properties
        double retractionValue = 0.60; // 0.5 Hhigher means more change, more pull out, Lower means smoother finish, longer processing time.
        
        // Note: This concept could be used by running the following code example 4 times with the
        // following configurations (C=0, B=45), (C=90, B=45), (C=180, B=45), (C=270, B=45)
        // This way each of the sides are covered by at leas one pass.
        
        Vector<Vec3> debugMappingGrid = new Vector<Vec3>(); // pattern of cutting to be projected onto the scene.
        Vector<SurfacePointContainer> regionSurfacePoints = new Vector<SurfacePointContainer>(); // accumulated surface points projected
        Vector<SurfacePointContainer> generatedCuttingPath = new Vector<SurfacePointContainer>(); // GCode cutting path
        Vector<RouterElementContainer> routerElements = new Vector<RouterElementContainer>();  // : Make list of objects that construct the tool
        
        Vector<Vec3> layerPoints = new Vector<Vec3>();
        
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
            if(currInfo.getObject() instanceof SceneCamera ||
               currInfo.getObject() instanceof DirectionalLight ||
               currInfo.getPhysicalMaterialId() == 500  ){
                continue;
            }
            BoundingBox currBounds = currInfo.getTranslatedBounds();
            if(sceneBounds == null){
                sceneBounds = currBounds;
            } else {
                sceneBounds.extend(currBounds);
            }
        }
        
        if(sceneBounds != null){
            BoundingBox expandedSceneBounds = new BoundingBox(sceneBounds);
            expandedSceneBounds.expandPercentage(5); // expand by 5%
            double sceneSize = Math.max(sceneBounds.maxx - sceneBounds.minx, Math.max(sceneBounds.maxy - sceneBounds.miny, sceneBounds.maxz - sceneBounds.minz));
            //System.out.println("sceneSize " + sceneSize );
            //System.out.println("accuracy " + accuracy );
            Vec3 sceneCenter = sceneBounds.getCenter();
            
            Vec3 raySubtract = new Vec3(toolVector.times( sceneSize * (2) ) ); //  This is only for debug to show the direction the drill will be pointing
            
            // construct a grid and iterate each coordinate and translate it to the toolVector
        
            // Translate to point in space to project region mapping.
            
            Vec3 regionScan = new Vec3(sceneCenter);
            regionScan.add( toolVector.times(sceneSize) );
            
            // DEBUG Show
            ObjectInfo bcLineInfo = addLineToScene(window, regionScan,  regionScan.minus(raySubtract), "B/C Axis Ray (" + b + "-" + c + ")", false ); // debug show ray cast line
            bcLineInfo.setPhysicalMaterialId(500);
            
            int width = (int)(sceneSize / accuracy);
            int height = (int)(sceneSize / accuracy);
            //System.out.println("width " + width );
             
            width *= 1.5;           // expand (depending on C/B the rotation can cause the scene to expand beyond the rectangular bounds. )
            height *= 1.5;
            
            //
            // Loop through layers
            //
            double currLayerHeight = sceneBounds.maxy;
            double sceneHeight = sceneBounds.maxy - sceneBounds.miny;
            
            //double layerHeight = 0.25; // Pass in from User form.
            
            
            int layersCount = (int)(sceneHeight / layerHeight);
            layersCount++;
            //System.out.println("layersCount: " + layersCount + " " + sceneHeight + " " + layerHeight);
            for(int l = 0; l < layersCount; l++ ){
            
                currLayerHeight = sceneBounds.maxy - ( l * layerHeight );                   // Height for current layer
            
                boolean isFirstPointInLayer = true;
                
                // Loop through grid
                // TODO: take user or config data on accuracy units, and calibrate grid spacing to that size.
                for(int x = 0; x < width; x++){
                    for(int y = 0; y < height; y++){
                        // xy offset to coords, Translate based on 'toolVector'
                        
                        int progress = (int)(((double)((double)x + ((double)width*(double)l)) / ((double)width * (double)layersCount)) * (double)100);
                        progressDialog.setProgress(progress);
                        
                        Vec3 samplePoint = new Vec3(regionScan);
                        
                        double scaleFactor = accuracy; //  (sceneSize / accuracy) ;
                        
                        //Vec3 currGridPoint = new Vec3( (double)(x-(width/2)) * 0.2 , (double)(y-(height/2)) * 0.2, (double)(y-(height/2)) * 0.2 ); // First try
                        Vec3 currGridPoint = new Vec3( (double)(x-(width/2)) * scaleFactor, 0, (double)(y-(height/2)) * scaleFactor );
                        
                        if(x % 2 == 0){ // Alternate scan direction on Y pass every other X. This is more effecient as it cuts the travel distance in half.
                            currGridPoint = new Vec3( (double)(x-(width/2)) * scaleFactor, 0, (double)((height-y-1) - (height/2)) * scaleFactor ); // reversed
                        }
                        
                        zRotationMat.transform(currGridPoint); // Apply the B axis transform.
                        yRotationMat.transform(currGridPoint); // Apply the C axis rotation
                        
                        samplePoint.add( currGridPoint ); // shift
                        
                        debugMappingGrid.addElement(currGridPoint);
                        
                        Vec3 samplePointB = samplePoint.minus(raySubtract); // Second point in ray cast
                        
                        // Loop through roughing layers.
                        
                        Vec3 layourPoint = null;
                        
                        // Where does the ray intersect on the layer plane
                        // First layer starts at the bounds height.
                        
                        // sceneBounds.maxy
                        Vec3 layerFacePointA = new Vec3(-999999, currLayerHeight, -999999);
                        Vec3 layerFacePointB = new Vec3(999999, currLayerHeight, -999999);
                        Vec3 layerFacePointC = new Vec3(0, currLayerHeight, 999999);
                        Vec3 layerPointCollision = Intersect2.getIntersection(samplePoint, samplePointB, layerFacePointA, layerFacePointB, layerFacePointC );
                        //Vec3 samplePointCollision = Intersect2.infiniteLinePassesThrough(samplePoint, samplePointB, layerFacePointA, layerFacePointB, layerFacePointC );
                        
                        //System.out.println("layerPointCollision " + layerPointCollision + " sceneBounds.maxy " + sceneBounds.maxy);
                        
                        // scan scene. If ray collision is above layerPointCollision, then use it instead.
                        
                        // Find collision location
                        Vec3 intersectPoint = null;
                        Vec3 intersectNormal = null; // normal of surface of intersection.
                        for(int i = 0; i < sceneObjects.size(); i++){
                            ObjectInfo currInfo = sceneObjects.elementAt(i);
                            Object3D currObj = currInfo.getObject();
                            //System.out.println("Checking for intersect in: " + currInfo.getName());
                            
                            if( currInfo.getPhysicalMaterialId() == 500 ){
                                continue;
                            }
                            
                            //System.out.println("Checking for intersect in: " + currInfo.getName() + " mat " + currInfo.getPhysicalMaterialId());
                            if(currObj instanceof Curve){
                                continue;
                            }
                            
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
                                    
                                    Vec3 normal = faceB.minus(faceA).cross(faceC.minus(faceA));
                                    double length = normal.length();
                                    if (length > 0.0){
                                        normal.scale(1.0/length);
                                    }
                                    //System.out.println(" normal " + normal + " length "  + length);
                                    
                                    Vec3 samplePointCollision = Intersect2.getIntersection(samplePoint, samplePointB, faceA, faceB, faceC );
                                    if(samplePointCollision != null){ // found intersection.
                                        //System.out.println(" *** ");
                                        if(intersectPoint != null){   // existing intersection exists, check if the new one is closer
                                            double existingDist = regionScan.distance(intersectPoint);
                                            double currrentDist = regionScan.distance(samplePointCollision);
                                            if(currrentDist < existingDist){ // we only want the closest intersection.
                                                intersectPoint = samplePointCollision;
                                                intersectNormal = normal;
                                            }
                                        } else {
                                            intersectPoint = samplePointCollision;
                                            intersectNormal = normal;
                                        }
                                    }
                                }
                            }
                        }
                         
                        if(intersectPoint != null && intersectPoint.y > currLayerHeight){ // Scene collision detected.
                            layerPointCollision = intersectPoint;
                        }
                        
                        // Check to see if this point is within the scene bounds (objects to cut)

                        if(expandedSceneBounds.contains(layerPointCollision)){ // Don't add points outside scene area.
                        
                            if(isFirstPointInLayer){ // Raise between layers
                                if(layerPoints.size() > 1){
                                    Vec3 raisedPoint1 = new Vec3(layerPoints.elementAt(layerPoints.size() - 1));
                                    raisedPoint1.y += layerHeight; // layerHeight sceneHeight
                                    layerPoints.addElement(raisedPoint1);
                                    SurfacePointContainer spc = new SurfacePointContainer(raisedPoint1, intersectNormal, passNumber);
                                    spc.b = b;
                                    spc.c = c;
                                    regionSurfacePoints.addElement(spc); // local intersectPoint
                                    scanedSurfacePoints.addElement(spc); // external
                                }
                                
                                Vec3 raisedPoint2 = new Vec3(layerPointCollision);
                                raisedPoint2.y += layerHeight * 2; // layerHeight sceneHeight
                                layerPoints.addElement(raisedPoint2);
                                SurfacePointContainer spc = new SurfacePointContainer(raisedPoint2, intersectNormal, passNumber);
                                spc.b = b;
                                spc.c = c;
                                regionSurfacePoints.addElement(spc); // local intersectPoint
                                scanedSurfacePoints.addElement(spc); // external
                            }
                            isFirstPointInLayer = false;
                            
                            //
                            // Point for this layer found
                            //
                            layerPoints.addElement(layerPointCollision);
                            // Add SPC
                            //Vec3 intersectNormal = null;
                            SurfacePointContainer spc = new SurfacePointContainer(layerPointCollision, intersectNormal, passNumber);
                            spc.b = b;
                            spc.c = c;
                            regionSurfacePoints.addElement(spc); // local intersectPoint
                            scanedSurfacePoints.addElement(spc); // external
                        
                        }
                        
                        /*
                        if(intersectPoint != null){
                            //System.out.println(" Colision " + intersectPoint );
                            
                            // TODO: If a close point in scanedSurfacePoints exists, then don't include it in the surface to cut as it has allready been done.
                            boolean skipPointAsDuplicate = false; // only skip point if it was added from a different pass!
                            if(restMachiningEnabled){
                                double closestExistingSufacePoint = 9999999;
                                int existingPointCount = 0;
                                for(int pi = 0; pi < scanedSurfacePoints.size(); pi++){
                                    SurfacePointContainer spc = scanedSurfacePoints.elementAt(pi);
                                    Vec3 currExistingPoint = spc.point;
                                    double currDist = intersectPoint.distance(currExistingPoint);
                                    if( currDist < (accuracy * 1.5) && passNumber != spc.passNumber ){ // 1.2 // experiment with threshold
                                        skipPointAsDuplicate = true; // old method, doesn't do any overlap
                                        existingPointCount++;
                                    }
                                }
                                //System.out.println("existingPointCount " + existingPointCount);
                                if(existingPointCount >= 3){ // we want overlap - needs work
                                    skipPointAsDuplicate = true;
                                }
                            }
                            if(skipPointAsDuplicate == false){
                                //System.out.println("intersectNormal " + intersectNormal);
                                SurfacePointContainer spc = new SurfacePointContainer(intersectPoint, intersectNormal, passNumber);
                                spc.b = b;
                                spc.c = c;
                                regionSurfacePoints.addElement(spc); // local intersectPoint
                                scanedSurfacePoints.addElement(spc); // external
                            }
                            //addLineToScene(window, intersectPoint,  intersectPoint.plus(new Vec3(0,1,0)) );
                        }
                         */
                        
                    } // Y
                } // X
                
            } // Layer
            
            
            // TODO: Iterate through layerPoints and remove points out of bounds of the router.
            //
            
            // TODO: Iterate through layerPoints and
            
            if(layerPoints.size() > 1){
                System.out.println("layer points  " );
                
                //ObjectInfo line = addLineToScene(window, layerPoints, "Layer Points (" + b + "-" + c + ")", true);
                //line.setPhysicalMaterialId(500);
                
            }
            
            //
            // debugMappingGrid
            
            
            if(debugMappingGrid.size() > 1){
                ObjectInfo line = addLineToScene(window, debugMappingGrid, "Projection Grid (" + b + "-" + c + ")", false);
                line.setPhysicalMaterialId(500);
            }
            
            // add entry and exit points above scene.
            // todo: check machine bounds
            if(regionSurfacePoints.size() > 0){
                SurfacePointContainer firstSpc = regionSurfacePoints.elementAt(0);
                SurfacePointContainer lastSpc = regionSurfacePoints.elementAt(regionSurfacePoints.size() - 1);
                firstSpc.b = b;
                firstSpc.c = c;
                lastSpc.b = b;
                lastSpc.c = c;
                
                // Add entry and exit paths from start position.
                // Note if this entry or exit collide they would beed to be rerouted.
                double maxMachineHeight = 0; // TODO calculate entry and exit points based on the capacity of the machine.
                Vec3 firstRegionSurfacePoint = firstSpc.point;
                Vec3 lastRegionSurfacePoint = lastSpc.point;
                
                
                SurfacePointContainer insertFirstSpc = new SurfacePointContainer( new Vec3(firstRegionSurfacePoint.x, firstRegionSurfacePoint.y + (sceneSize/4), firstRegionSurfacePoint.z), 0 );
                SurfacePointContainer insertLastSpc = new SurfacePointContainer(  new Vec3(lastRegionSurfacePoint.x, lastRegionSurfacePoint.y + (sceneSize/4), lastRegionSurfacePoint.z) , 0);
                
                regionSurfacePoints.add(0, insertFirstSpc); // insert entry
                regionSurfacePoints.add(regionSurfacePoints.size(), insertLastSpc );
                
                // Insert/fill points in gaps. Since the router travels in a straight line between points, we need to check each segment for collisions.
                regionSurfacePoints = fillGapsInPointPathSPC(regionSurfacePoints, accuracy);
                
                // Draw line showing mapped surface
                if(regionSurfacePoints.size() > 1){
                    surfaceMapInfo = addLineToSceneSPC(window, regionSurfacePoints, "Surface Map (" + b + "-" + c + ")", true);
                    surfaceMapInfo.setPhysicalMaterialId(500);
                }
            }
        } // bounds
        
        //progressDialog.close(); // We can't accurately track progress from here on. But at least updates are visible to user.
        
        if(regionSurfacePoints.size() == 0){
            return new Vector<SurfacePointContainer>();
        }
        
        
        //
        // Simulate cutting toolpath using regionSurfacePoints calculated from a particular B/C angle.
        //
        System.out.println("Scanning surface points using B/C Tool angle. ");
        Vec3 firstRegionSurfacePoint = regionSurfacePoints.elementAt(0).point;
        Vec3 lastRegionSurfacePoint = regionSurfacePoints.elementAt(regionSurfacePoints.size() - 1).point;
        
        
        ObjectInfo avatarCutterLine = addLineToScene(window, firstRegionSurfacePoint, firstRegionSurfacePoint.plus(toolVector.times(fulcrumToToolTipLength) ), "Cutter (" + b + "-" + c + ")", true );
        avatarCutterLine.setPhysicalMaterialId(500);
        Curve currCurve = (Curve)avatarCutterLine.getObject();
        
        
        // Router Z height base
        ObjectInfo routerZBaseCubeInfo = addCubeToScene(window, firstRegionSurfacePoint.plus(toolVector.times( fulcrumToToolTipLength ) ), 0.5, "Router Base (" + b + "-" + c + ")" );
        routerZBaseCubeInfo.setPhysicalMaterialId(500);
        //routerZBaseCubeInfo.setPhysicalMaterialId(500);
        setObjectBCOrientation(routerZBaseCubeInfo, c,  0); // only C is applied
        routerElements.addElement(  new  RouterElementContainer( routerZBaseCubeInfo, 4, 0.5) );
        
        //
        // Add motor housing
        //
        // This includes multiple objects that represent the router machine.
        // Use to detect collisions.
        ObjectInfo drillBodyCubeInfo = addCubeToScene(window, firstRegionSurfacePoint.plus(toolVector.times( 2.2) ), 0.8, "Router Housing Base (" + b + "-" + c + ")" ); // Cube represents a part of the machine
        drillBodyCubeInfo.setPhysicalMaterialId(500);
        setObjectBCOrientation(drillBodyCubeInfo, c,  b); // Set orientation
        routerElements.addElement(  new  RouterElementContainer( drillBodyCubeInfo, 2.2, 0.8 ) );
        
        ObjectInfo drillBodyBackEndCubeInfo = addCubeToScene(window, firstRegionSurfacePoint.plus(toolVector.times( fulcrumToToolTipLength + backEndLength) ), 0.8, "Router Back End (" + b + "-" + c + ")" ); // Cube represents a part of the machine
        drillBodyBackEndCubeInfo.setPhysicalMaterialId(500);
        setObjectBCOrientation(drillBodyBackEndCubeInfo, c,  b); // Set orientation
        routerElements.addElement(  new  RouterElementContainer( drillBodyBackEndCubeInfo, 5.0, 0.8 ) );
        
        ObjectInfo drillBodyCilynderInfo = addCylinderToScene(window, firstRegionSurfacePoint.plus(toolVector.times(routerHousingPosition) ), routerHousingSize, routerHousingSize,  "Router Housing (" + b + "-" + c + ")" );
        drillBodyCilynderInfo.setPhysicalMaterialId(500);
        setObjectBCOrientation(drillBodyCilynderInfo, c,  b); // Set orientation
        routerElements.addElement(  new  RouterElementContainer( drillBodyCilynderInfo, routerHousingPosition, routerHousingSize) );
        
        // add Collet
        ObjectInfo drillColletInfo = addCylinderToScene(window, firstRegionSurfacePoint.plus(toolVector.times( 0.5 ) ), 0.2, 0.2,  "Collet (" + b + "-" + c + ")" );
        drillColletInfo.setPhysicalMaterialId(500);
        setObjectBCOrientation(drillColletInfo, c,  b);
        routerElements.addElement(  new  RouterElementContainer( drillColletInfo, 0.5, 0.2) );
        
        // Add tool tip
        //ObjectInfo toolPitCubeInfo = addCubeToScene(window, firstRegionSurfacePoint.plus(toolVector.times( bitTipPosition ) ), bitTipSize, "Bit Tip" ); // Cube represents tip of bit
        ObjectInfo toolPitCubeInfo = addCylinderToScene(window, firstRegionSurfacePoint.plus(toolVector.times( bitTipPosition ) ), bitTipSize, bitTipSize, "Bit Tip (" + b + "-" + c + ")" );
        toolPitCubeInfo.setPhysicalMaterialId(500);
        setObjectBCOrientation(toolPitCubeInfo, c,  b);
        routerElements.addElement( new  RouterElementContainer( toolPitCubeInfo, bitTipPosition, bitTipSize)  );
        
        // Add tool tip ball nose
        ObjectInfo toolBallNoseInfo = addSphereToScene(window, firstRegionSurfacePoint.plus(toolVector.times( 0.0001 ) ), bitTipSize, "Bit Ball Nose (" + b + "-" + c + ")" );
        toolBallNoseInfo.setPhysicalMaterialId(500);
        setObjectBCOrientation(toolBallNoseInfo, c,  b);
        RouterElementContainer ballNoseREC = new  RouterElementContainer( toolBallNoseInfo, 0.0001, bitTipSize, false); // Last parameter is enable
        routerElements.addElement(  ballNoseREC ); // Disabled collisions because BUGGY
        //System.out.println(" Ball size " + ballNoseREC.size + "  loc " + ballNoseREC.location );
       
        
        //
        // Scan surface mesh to create tool path.
        //
        for(int i = 0; i < regionSurfacePoints.size(); i++){
            //Vec3 surfacePoint = regionSurfacePoints.elementAt(i).point;
            //SurfacePointContainer spc = regionSurfacePoints.elementAt(i);
            generatedCuttingPath.addElement(regionSurfacePoints.elementAt(i));
        }
        
        
        //
        // Retract tool based on drill bit tip and angle delta between BC and the surface.
        // This should be more effecient than using the drill tip geometry to collide with the scene for retraction.
        //
        // If ball nose drill bit type or flat end.
        // Note: This code doesn't appear to work but its a first attempt.
        if(ballNoseTipType){
            for(int i = 0; i < generatedCuttingPath.size(); i++){
                SurfacePointContainer spc = generatedCuttingPath.elementAt(i);
                Vec3 surfaceNormal = spc.normal;
                if(surfaceNormal == null){                      // Why do some surface points have missing normals??? Might be points from fillGapsInPointPathSPC()
                    //System.out.println(" Why is surface normal null in regionSurfacePoints? " + i );
                    //surfaceNormal = new Vec3(0, -1, 0);
                    surfaceNormal = new Vec3(0, 1, 0);
                    //System.out.println(" normal is null ");
                }
                if(surfaceNormal  != null){
                    double angle = Vec3.getAngle( toolVector, new Vec3(), surfaceNormal );
                    if(angle > 90){
                        //angle = 90 - angle; // ??? incorrect
                        //angle = angle - 90; // Wrong
                        angle = 180 - angle; // Fixed
                    }
                    angle = 90 - angle; // Rotate 90 degrees.
                    //angle = Math.abs(angle);
                    double ballNoseRadius = bitTipSize / 2;
                    angle = 90 - angle; // we want the angle from vertical.
                    double retract = retractSphereByAngle(ballNoseRadius, Math.toRadians(angle));
                    //spc.point = new Vec3(   spc.point.plus(    toolVector.times( retract  )  ) );
                    spc.point.add( toolVector.times( retract ) );
                    //System.out.println("normal " + surfaceNormal);
                    //System.out.println(" angle " + angle + " retract: " + retract);
                }
            }
        } else { // Flat end tool tip
            
            for(int i = 0; i < generatedCuttingPath.size(); i++){
                SurfacePointContainer spc = generatedCuttingPath.elementAt(i);
                Vec3 surfaceNormal = spc.normal;
                if(surfaceNormal == null){
                    //surfaceNormal = new Vec3(0, -1, 0);
                    surfaceNormal = new Vec3(0, 1, 0);
                    //System.out.println(" normal is null ");
                }
                if(surfaceNormal  != null){
                    double angle = Vec3.getAngle( toolVector, new Vec3(), surfaceNormal );
                    if(angle > 90){
                        //angle = 90 - angle; // ??? incorrect
                        //angle = angle - 90;
                        angle = 180 - angle; // Fixed
                    }
                    angle = 90 - angle; // Rotate 90 degrees.
                    //angle = Math.abs(angle);
                    double ballNoseRadius = bitTipSize / 2;
                    angle = 90 - angle; // we want the angle from vertical.
                    double retract = retractFlatEndByAngle(ballNoseRadius, Math.toRadians(angle));
                    //spc.point = new Vec3(   spc.point.plus(    toolVector.times( retract  )  ) );
                    spc.point.add( toolVector.times( retract ) );
                    //System.out.println("normal " + surfaceNormal);
                    //System.out.println(" angle " + angle + " retract: " + retract);
                }
            }
        }
        
        
        //
        // Retract Roughing Pass padding margin from surface.
        // because this is the roughing, retract 1/4 width of drill bit to leave material for finishing.
        // The roughing pass will operate as fast as possible and the drill can wobble causing
        // incursions into the part. This margin protects the surface for finishing.
        //
        for(int i = 0; i < generatedCuttingPath.size(); i++){
            SurfacePointContainer spc = generatedCuttingPath.elementAt(i);
            Vec3 surfaceNormal = spc.normal;
            if(surfaceNormal == null){
                //surfaceNormal = new Vec3(0, -1, 0);
                surfaceNormal = new Vec3(0, 1, 0);
                //System.out.println(" normal is null ");
            }
            if(surfaceNormal  != null){
                //double angle = Vec3.getAngle( toolVector, new Vec3(), surfaceNormal );
                //if(angle > 90){
                //    angle = 180 - angle;
                //}
                //angle = 90 - angle; // Rotate 90 degrees.
                double retractDistance = bitTipSize / 4;
                //angle = 90 - angle; // we want the angle from vertical.
                spc.point.add( toolVector.times( retractDistance ) );
            }
        }
        
        
        
        
        if(surfaceMapInfo != null){
            surfaceMapInfo.setVisible(false); // Hide surface map because we want to show the GCode cut path now.
        }
        
        // If we have a GCode tool path add it to the scene.
        if(generatedCuttingPath.size() > 1){
            toolPath = addLineToSceneSPC(window, generatedCuttingPath, "GCode Tool Path (" + b + "-" + c + ")", true);
            toolPath.setPhysicalMaterialId(500);
        }
        
        
        //
        // Resolve collisions. Repeat until all collisions resolved.
        //
        boolean running = true;
        int iterationCount = 0;
        while(running){
            iterationCount++;
            int collisionCount = 0;
            System.out.println("Modifying tool path to remove collisions.");
            //Vector<Vec3> updatedCuttingPath = new Vector<Vec3>();
            
            for(int i = 0; i < generatedCuttingPath.size(); i++){
                SurfacePointContainer currSpc = generatedCuttingPath.elementAt(i);
                Vec3 currPoint = currSpc.point;
                
                if(currSpc.finalized){
                    continue; // If this point is final then don't evaluate it for collisions.
                }
                
                int progress = (int)(((double)i / (double)generatedCuttingPath.size()) * (double)100);
                progressDialog.setProgress(progress);
                
                //  calculate where the cutter would be to when fit to the current region surface point.
                Vector<Vec3> updatedPoints = new Vector<Vec3>();
                updatedPoints.addElement(currPoint);
                updatedPoints.addElement(currPoint.plus(toolVector.times( fulcrumToToolTipLength ))  ); // Make the length of the avatar arbitrary, scale later on.
                
                // Update the avatar object to show to the user where it is in space.
                currCurve.setVertexPositions(vectorToArray(updatedPoints)); // represents cutter
                avatarCutterLine.clearCachedMeshes();
                
                // Update router location
                for(int re = 0; re < routerElements.size(); re++){
                    RouterElementContainer rec = routerElements.elementAt(re);
                    ObjectInfo routerElement = rec.element;
                    CoordinateSystem reCS = routerElement.getModelingCoords();
                    reCS.setOrigin(currPoint.plus(toolVector.times(  rec.location + (rec.size / 2)  )));
                    routerElement.clearCachedMeshes();
                }
                
                // Check to see if the avatar cutter collides with any object in the scene.
                
                // Collision detection
                // The generatedCuttingPath can still collide with the scene. Perhaps keep filtering the
                // generatedCuttingPath pulling out more points that collide until there are no more collisions?
                // It appear the path along a side still collides.
                //boolean housingCollides = cubeCollidesWithScene( drillBodyCubeInfo, sceneObjects );
                //boolean tipCollides = cubeCollidesWithScene( toolPitCubeInfo, sceneObjects );
                
                boolean collides = false;
                double retractDistance = retractionValue;
                retractDistance = 0;
                double maxLocation = 0;
                double maxCollision = 0;
                for(int re = 0; re < routerElements.size(); re++){
                    RouterElementContainer rec = routerElements.elementAt(re);
                    ObjectInfo routerElement = rec.element;
                    if(rec.enabled == false){
                        continue;
                    }
                    // rec.location
                    if(objectCollidesWithScene(routerElement, sceneObjects, routerElements)){
                        collides = true;
                        double currLocationDistance = rec.location - (rec.size / 2);
                        //currLocationDistance = currLocationDistance * 0.5; // this unit might be too much?
                        //retractDistance += (rec.location - (rec.size / 2)); // minus the size of the object?
                        if(currLocationDistance > maxLocation){
                            maxLocation = currLocationDistance;
                        }
                    }
                    
                    // new method - performance penalty
                    if(collides){ // Only check collision distance if we know there is a collision.
                        double collideDist = objectSceneCollisionOffset( toolVector, routerElement, sceneObjects, routerElements );
                        if(collideDist > 0){
                            //System.out.println("Collide dist found " + collideDist);
                            //retractDistance += collideDist;
                            maxCollision = collideDist;
                        }
                    }
                }
                retractDistance = Math.max(maxLocation, maxCollision); // Max or addition. Possible ideas.
                
                //retractDistance = 0.5;
                
                if(collides){
                //if(housingCollides || tipCollides){
                    collisionCount++;
                    //System.out.println("  Collide point " );
                    // This point will collide so:
                    // 1) We can't add the point to the GCode file, and
                    // 2) We must pull the router out along the B/C axis because we can't be sure the next point travel will collide with the scene.
                    
                    // create a void region that is a long channel angled along toolVector, and remove all generatedCuttingPath
                    // points that fall in that zone.
                    
                    // Add pull out point
                    /*
                     // Will be infinite loop with fill gaps. This method will never pull lower points that when subdivided never resolve
                    Vec3 retractPoint = new Vec3(currPoint);
                    retractPoint.add(toolVector.times(retractDistance)); // was 3  retractDistance  retractionValue
                    generatedCuttingPath.setElementAt(retractPoint, i);
                    */
                     
                    // NOTE: an optimization would be to take into account which machine object collided and retract the length needed not just the full length.
                    
                    
                    //
                    // Pull all points in channel.
                    // We do this because when we call fill gaps we create new points that could collide,
                    // We don't want to reciursivly add and remove points in an infinite loop.
                    //
                    Vec3 currPointRetraction = new Vec3(currPoint);
                    currPointRetraction.add(toolVector.times(retractDistance));
                    // Now we don't want to pull the channel further than currPointRetraction on the BC axis
                    Vec3 voidRegionStart = new Vec3(currPoint.plus(toolVector.times( 100 ) ));
                    Vec3 voidRegionEnd = new Vec3(currPoint.minus(toolVector.times( 100 ) ));
                    //int removedCount = 0;
                    int movedCount = 0;
                    for(int p = generatedCuttingPath.size() -1; p >= 0; p--){
                        SurfacePointContainer currPSpc = generatedCuttingPath.elementAt(p);
                        Vec3 currP = currPSpc.point;
                        
                        Vec3 closestPoint = Intersect2.closestPointToLineSegment( voidRegionStart, voidRegionEnd, currP);
                        double dist = closestPoint.distance(  currP );
                        //System.out.println("  dist " + dist );
                        //double getAvgSpan = getAverageSpan(generatedCuttingPath); // old method
                        //System.out.println("  dist " + dist + " getAvgSpan " + getAvgSpan );
                        //if(dist < getAvgSpan / 2){
                        if(dist < accuracy / 4){ // 2
                            movedCount++;
                            
                            // Calculate retractionDistance to give us the same rertaction as currPointRetraction along BC axis.
                            Vec3 currChannelRetractPoint = new Vec3(currP);
                            currChannelRetractPoint.add(toolVector.times(retractDistance));
                            
                            double axisDist = getAxisDistance(currChannelRetractPoint, currPointRetraction, toolVector);
                            if(axisDist > 0){ // too much. We pulled the channel too far.
                                currChannelRetractPoint.add(toolVector.times( -( retractDistance / 1.4 ))); // correct a little
                            }
                                
                            //generatedCuttingPath.setElementAt(retractPoint, p); // NO This causes points to go off course (BC).
                            //generatedCuttingPath.setElementAt(currRetractPoint, p);
                            currPSpc.point = currChannelRetractPoint;
                            currPSpc.onSurface = false; // This point no longer contacts the surface and doesn not aid in adding detail to the parts being manufactured.
                        }
                    }
                    //System.out.println("movedCount " + movedCount + "   i: " + i);
                    
                    //
                    // Fill in gaps created by changes
                    //
                    generatedCuttingPath = fillGapsInPointPathSPC(generatedCuttingPath, accuracy); // Fill in gaps created by moving points.
                    generatedCuttingPath = removeDuplicatePointsSPC(generatedCuttingPath, accuracy); // Remove duplicates if we move multiple points to the same location.
                    generatedCuttingPath = smoothPointsOnAxisSPC(generatedCuttingPath, toolVector, accuracy);
                    
                    // TOOD: it would make more sense to keep an attribute in the generatedCuttingPath to know which points ...
                    // A better smoothing on axis is possible.
                    
                    //System.out.println("generatedCuttingPath size " + generatedCuttingPath.size()  );
                    
                    
                    // Update line representing toolpath
                    Curve tpCurve = (Curve)toolPath.getObject();
                    tpCurve.setVertexPositions(vectorToArraySPC( generatedCuttingPath ));
                    toolPath.clearCachedMeshes();
                    
                    
                    if(display){
                        //try { Thread.sleep(5); } catch(Exception e){} // Wait to show collision
                    }
                    // Note: This method of retracting to avoid collisions is simple but moves the machine excessivly in some cases.
                    
                } else {
                    //updatedCuttingPath.addElement(currPoint); // No collision, This point can be safely cut on the machine / GCode.
                }
                
                if(display && i % 2 == 0){
                    // Update the scene
                    window.updateImage();
                    try { Thread.sleep(4); } catch(Exception e){} // Wait
                }
            } // end loop generatedCuttingPath for each point in path
            
            
            //
            // Optimization, flag points with lots of adjacent non collisions as being finalized.
            // Theory: A collision will cause modification to adjacent points to resolve but this
            // may introduce new collisions. But long stretches of non collision points are unlikely
            // to be disturbed and thus can be assumed to be finalized.
            //
            for(int i = 12; i < generatedCuttingPath.size(); i++){
                boolean okToFinalize = true;
                for(int regress = 0; regress < 10; regress++){
                    SurfacePointContainer currSpc = generatedCuttingPath.elementAt(i - regress);
                    if(currSpc.collides){
                        okToFinalize = false;
                    }
                }
                if(okToFinalize){
                    SurfacePointContainer currSpc = generatedCuttingPath.elementAt(i - 6);
                    currSpc.finalized = true;
                }
            }
            
            
            //System.out.println("size " + generatedCuttingPath.size()  + " collisionCount: " + collisionCount );
            System.out.println(" Collisions: " + collisionCount );
            System.out.println(" Points in path: " + generatedCuttingPath.size() );
            
            int pointsOnSurface = 0;
            for(int i = 0; i < generatedCuttingPath.size(); i++){
                if(generatedCuttingPath.elementAt(i).onSurface){
                    pointsOnSurface++;
                }
            }
            System.out.println(" Points on surface: " + pointsOnSurface );
            
            double pathLength = 0;
            for(int i = 1; i < generatedCuttingPath.size(); i++){
                Vec3 ap = generatedCuttingPath.elementAt(i - 1).point;
                Vec3 bp = generatedCuttingPath.elementAt(i).point;
                pathLength += ap.distance(bp);
                
            }
            System.out.println(" Path length: " + pathLength );
            
            if(collisionCount == 0){
                running = false; // we are done.
            }
            
            // Show result of iteration.
            // Update the scene
            window.updateImage();
            //try { Thread.sleep(6); } catch(Exception e){} // Wait
            
        } // end loop resolve collisions
        System.out.println("Collisions resolved in passes: " + iterationCount);
        
        
        // Now simulate the generated tool path to be written to a file.
        //try { Thread.sleep(200); } catch(Exception e){}
        System.out.println("Simulating Tool Path.");
        
        
        
        //
        // Now simulate cutting of the new GCode which should result in no collisions.
        // NOTE: This is now redundant or for show only. The gcode is made in a new function
        //
        String gCodeExport = "";
        int collisions = 0;
        //generatedCuttingPath = fillGapsInPointPath(generatedCuttingPath ); // We don't need to do this for the GCode, This is only for demonstration in the simulator.
        for(int i = 0; i < generatedCuttingPath.size(); i++){
            Vec3 currPoint = generatedCuttingPath.elementAt(i).point;
            
            int progress = (int)(((double)i / (double)generatedCuttingPath.size()) * (double)100);
            progressDialog.setProgress(progress);
            
            //  calculate where the cutter would be to when fit to the current region surface point.
            Vector<Vec3> updatedPoints = new Vector<Vec3>();
            updatedPoints.addElement(currPoint);
            updatedPoints.addElement(currPoint.plus(toolVector.times(4 + 6))  ); // Make the length of the avatar arbitrary, scale later on.  ********
            // Update the avatar object to show to the user where it is in space.
            currCurve.setVertexPositions(vectorToArray(updatedPoints));
            avatarCutterLine.clearCachedMeshes();
            
            // Update router location.
            for(int re = 0; re < routerElements.size(); re++){
                RouterElementContainer rec = routerElements.elementAt(re);
                ObjectInfo routerElement = rec.element;
                CoordinateSystem reCS = routerElement.getModelingCoords();
                reCS.setOrigin(currPoint.plus(toolVector.times(  rec.location + (rec.size / 2) )));
                routerElement.clearCachedMeshes();
            }
            
            
            // Check to see if the avatar cutter collides with any object in the scene.
            
            // TODO
            //boolean housingCollides = cubeCollidesWithScene( drillBodyCubeInfo, sceneObjects );
            //boolean tipCollides = cubeCollidesWithScene( toolPitCubeInfo, sceneObjects );
            
            boolean collides = false;
            for(int re = 0; re < routerElements.size(); re++){
                RouterElementContainer rec = routerElements.elementAt(re);
                if(rec.enabled == false){
                    continue;
                }
                ObjectInfo routerElement = rec.element;
                if(objectCollidesWithScene(routerElement, sceneObjects, routerElements)){
                    collides = true;
                }
            }
            
            
            //if(housingCollides || tipCollides){
            if(collides){
                collisions++;
                System.out.println("ERROR: GCode collision. ");
                
                if(display){
                    try { Thread.sleep(12); } catch(Exception e){} // Wait to show collision, This shouldn't happen
                }
            } else {
                //generatedCuttingPath.addElement(currPoint); // No collision, This point can be safely cut on the machine / GCode.
                
                // speed
                double speed = 50;
                if( i > 2 && i < generatedCuttingPath.size() - 6 ){
                    Vec3 oppositeP = generatedCuttingPath.elementAt(i - 1).point;
                    Vec3 oppositeP2 = generatedCuttingPath.elementAt(i - 2).point;
                    Vec3 prevPointA = generatedCuttingPath.elementAt(i + 1).point;
                    Vec3 prevPointB = generatedCuttingPath.elementAt(i + 2).point;
                    Vec3 prevPointC = generatedCuttingPath.elementAt(i + 3).point;
                    Vec3 prevPointD = generatedCuttingPath.elementAt(i + 4).point;
                    double currAngle = 180 - Vec3.getAngle( oppositeP, currPoint, prevPointA );
                    double currAngle2 = 180 - Vec3.getAngle( oppositeP2, oppositeP, currPoint );
                    double angleA = 180 - Vec3.getAngle( currPoint, prevPointA, prevPointB );
                    double angleB = 180 - Vec3.getAngle( prevPointA, prevPointB, prevPointC );
                    double angleC = 180 - Vec3.getAngle( prevPointB, prevPointC, prevPointD );
                    
                    if(currAngle > 20){
                        speed = speed * 0.9;
                    }
                    if(currAngle2 > 20){
                        speed = speed * 0.9;
                    }
                    if(angleA > 20){
                        speed = speed * 0.9;
                    }
                    if(angleB > 20){
                        speed = speed * 0.9;
                    }
                    //if(angleC > 20){
                    //    speed = speed * 0.9;
                    //}
                    
                    
                    if(currAngle > 40){
                        speed = speed * 0.8;
                    }
                    if(currAngle2 > 40){
                        speed = speed * 0.8;
                    }
                    if(angleA > 40){
                        speed = speed * 0.8;
                    }
                    if(angleB > 40){
                        speed = speed * 0.8;
                    }
                    //if(angleC > 40){
                    //    speed = speed * 0.8;
                    //}
                }
                
                // NOTE: XYZ need to be translated off of surface or cutting point.
                Vec3 xyzPoint = new Vec3(currPoint);
                xyzPoint.plus(toolVector.times(2.4)); // Note this value needs to be calculated based on the BC point to tip length.
                gCodeExport += "x" + scene.roundThree(xyzPoint.x) +
                    " y" + scene.roundThree(xyzPoint.y) +
                    " z" + scene.roundThree(xyzPoint.z) +
                    " b" + scene.roundThree(b) +
                    " c" + scene.roundThree(c) + " f" + (int)speed + ";\n";
                
                
                // Also calculate TCP GCode
                // Not needed for our machine.
            }
            
            if(display){
                // Update the scene
                window.updateImage();
                try { Thread.sleep(4); } catch(Exception e){} // Wait
            }
        } // end simulate GCode toolpoath
        
        progressDialog.close();
        
        System.out.println("Collsisions: " + collisions);
        
        long endTime = System.currentTimeMillis();
        System.out.println("Completion Time: " + ( (endTime - startTime) / 1000  ));
        
        return generatedCuttingPath;
    }
    

    
    /**
     * loadGCode
     * Description: Load an existing gcode tool path.
     * This serves severa functions:
     * 1) You can preview a toolpath to verify it does the intended operation.
     * 2) You can modify the toolpath by removing areas or otherwise modifying it.
     * 3) You can use it to tell a finishing pass not to cut existing areas allready cut using the 'rest machining' checkbox.
     */
    public void loadGCode(LayoutWindow window){
        
        System.out.println("load GCode. Not implemented. ");
    }
    
    
    /**
     * saveSelectedGCode
     * Description: Save a selected line/curve to a GCode file.
     * TODO: what about the attributes? Capture them from an existing gcode file by the same name?
     */
    public void saveSelectedGCode(LayoutWindow window){
        
        System.out.println("save selected GCode. Not implemented. ");
    }
}

