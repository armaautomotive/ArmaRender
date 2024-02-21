/* Copyright (C) 2021-2023 Jon Taylor

    */

package armarender;

import armarender.math.*;
import armarender.object.*;
import armarender.ui.*;
import armarender.ui.NinePointManipulator.*;
import buoy.event.*;
import java.awt.*;
import java.util.*;

/** SnapToTool is an EditingTool used for .... */

public class SnapToTool extends MeshEditingTool
{
    private Vec3 clickPos;
    private boolean dragInProgress, taperAll, towardCenter;
    private Point clickPoint;
    private HandlePosition whichHandle;
    private double boundsHeight, boundsWidth;
    private Vec3 baseVertPos[];
    private UndoRecord undo;
    //private final NinePointManipulator manipulator;
    //private Scene scene;
    private LayoutWindow window;
    private ObjectInfo objInfo;

    public static final int HANDLE_SIZE = 5;

    public SnapToTool(EditingWindow fr, MeshEditController controller, LayoutWindow window, ObjectInfo objInfo)
    {
        super(fr, controller);
        initButton("snapTo_48");
        //this.scene = scene;
        this.window = window;
        this.objInfo = objInfo;
    }
    
    public int whichClicks()
    {
      return HANDLE_CLICKS; // ALL_CLICKS;
    }

    public boolean allowSelectionChanges()
    {
      return !dragInProgress;
    }

    public String getToolTipText()
    {
      return Translate.text("snapToTool.tipText");
    }
    
    /**
     * mousePressedOnHandle
     *
     * Description: Mouse click on a curve point, snap it to the closest curve point on the camera view plane.
     */
    public void mousePressedOnHandle(WidgetMouseEvent e, ViewerCanvas view, int obj, int handle)
    {
        System.out.println("mousePressedOnHandle ");
        ObjectInfo oi = controller.getObject();
        Mesh mesh = (Mesh) controller.getObject().getObject();
        MeshVertex v[] = mesh.getVertices();

        clickPoint = e.getPoint();
        clickPos = v[handle].r;
        baseVertPos = mesh.getVertexPositions();
        
        
        // Get translated click pos for distance calculations.
        Vec3 translatedClickPos = new Vec3(clickPos);
        CoordinateSystem editingCs = objInfo.getCoords();
        Mat4 editingMat4 = editingCs.duplicate().fromLocal();
        editingMat4.transform(translatedClickPos);
        
        
        //System.out.println("clickPoint: " + clickPoint.x);
        
        
        // Determine camera view angle to use for moving selected point.
        //view.
        // drag = view.getCamera().getCameraCoordinates().getZDirection().times(-dy*0.01);
        //Vec3 zDir = view.getCamera().getCameraCoordinates().getZDirection();
        //Vec3 uDir = view.getCamera().getCameraCoordinates().getUpDirection();
        //System.out.println(" zDir " + zDir.x + " " + zDir.y + " " + zDir.z);
        //System.out.println(" uDir " + uDir.x + " " + uDir.y + " " + uDir.z);
        
        //System.out.println("  clickPos " + clickPos.x + " " + clickPos.y + " " + clickPos.z);
        //System.out.println(" TclickPos " + translatedClickPos.x + " " + translatedClickPos.y + " " + translatedClickPos.z);
        
        CoordinateSystem cameraCoordinates = view.getCamera().getCameraCoordinates();
        Vec3 cemeraOrigin = cameraCoordinates.getOrigin();
        
        // Translate
        double dy = 0.1;
        Vec3 test = view.getCamera().getCameraCoordinates().getZDirection().times(-dy*0.01);
        //System.out.println(" test " + test.x + " " + test.y + " " + test.z);
        
        // TODO: Calculate distance to move.
        
        ObjectInfo parent = oi.getParent();
        System.out.println("editing object " + oi.getName() + "  parent "+ parent);
        //if(parent == null){
            
        double closestDistance = 9999999;
        Vec3 closestVec = null;
        double distanceToCamera = 9999999;
        boolean invertDistance = false;
        
        Vector<ObjectInfo> sceneObjects = window.getScene().getObjects();
        for(int i = 0; i < sceneObjects.size(); i++){
            ObjectInfo sceneOi = sceneObjects.elementAt(i);
            
            if(sceneOi.getObject() instanceof Curve && objInfo != sceneOi){ // oi objInfo
                Curve curve = (Curve)sceneOi.getObject();
                
                // Only process if child is: 1) visible, 2) not disabled, 3) not hidden when children hidden.
                if(sceneOi.isVisible() == true &&
                   sceneOi.isChildrenHiddenWhenHidden() == false
                   //&& curve.isSupportMode() == false
                   ){
                    
                    //System.out.println("curve " + sceneOi.getName());
                    
                    Curve subdiv;
                    subdiv = curve.subdivideCurve().subdivideCurve().subdivideCurve(); // subdivide curve for more points.
                    Vec3 [] subdividedVerts = subdiv.getVertexPositions();
                    for(int vi = 0; vi < subdividedVerts.length; vi++){ // itererate subdivided curve
                        Vec3 curvePoint = new Vec3(subdividedVerts[vi]);
                        
                        // translate
                        CoordinateSystem cs = ((ObjectInfo)sceneOi).getCoords();
                        Mat4 mat4 = cs.duplicate().fromLocal();
                        mat4.transform(curvePoint);
                        
                        double cameraA = cemeraOrigin.distance(translatedClickPos);
                        double cameraB = cemeraOrigin.distance(curvePoint);
                        double dist = translatedClickPos.distance(curvePoint); // clickPos translatedClickPos
                        if(dist < closestDistance){
                            closestDistance = dist;
                            closestVec = curvePoint;
                            if(cameraA > cameraB){
                                invertDistance = true;
                            } else {
                                invertDistance = false;
                            }
                        }
                    }
                }
            }
        }
        
        if(closestDistance > 9999){
            // Abort
            System.out.println("Snap Aborted. No point found.");
            return;
        }
        
        if(invertDistance){
            closestDistance = -closestDistance;
        }
        
        Vec3 moveVec = new Vec3(0, 0, -closestDistance);
        
        CoordinateSystem cs = ((ObjectInfo)controller.getObject()).getCoords().duplicate();
        cs.setOrigin(new Vec3(0, 0, 0));
        cs.setOrigin(clickPos);
        double rot[] = cs.getRotationAngles();
        //cs.setOrientation(-rot[0], -rot[1], -rot[2]);
        //Mat4 mat4 = cs.duplicate().fromLocal();
        //mat4.transform(clickPos);
        
        double cameraRot[] = cameraCoordinates.getRotationAngles();
        
        CoordinateSystem tempCoordinates = new CoordinateSystem();
        tempCoordinates.setOrientation(cameraRot[0], cameraRot[1], cameraRot[2]);
        Mat4 tempMat4 = tempCoordinates.duplicate().fromLocal();
        tempMat4.transform(moveVec);
        
        // -14.545480179165976 8.359089287721188 11.781537787461987
        // -2.43 0.51 20.5
        //System.out.println(" Move Vec " + moveVec.x + " " + moveVec.y + " " + moveVec.z);
        clickPos.subtract(moveVec);
        
        Vec3[] vecList = new Vec3[v.length];        // This is only required to cause model to redraw.
        for(int i = 0; i < vecList.length; i++){
            vecList[i] = v[i].r;
        }
        mesh.setVertexPositions(vecList);
        //Curve editingCurve = ((Curve)objInfo.getObject());
        //editingCurve.setSmoothingMethod( editingCurve.getSmoothingMethod() ); // forces clear cache
        objInfo.clearCachedMeshes();    // doesn't work.
        
        controller.objectChanged();
        theWindow.updateImage();
    }
    


    public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view) {
        /*
        //manipulator.mouseDragged(e, view);
        MeshViewer mv = (MeshViewer) view;
        Mesh mesh = (Mesh) controller.getObject().getObject();
        Point dragPoint = e.getPoint();
        Vec3 v[], drag;
        int dx, dy;

        if (undo == null)
        undo = new UndoRecord(theWindow, false, UndoRecord.COPY_VERTEX_POSITIONS, new Object [] {mesh, mesh.getVertexPositions()});

        if(clickPoint == null){
          System.out.println("ERROR: clickPoint " + clickPoint);
          return;
        }

        dx = dragPoint.x - clickPoint.x;
        dy = dragPoint.y - clickPoint.y;
        //dz = dx;
        if (e.isShiftDown())
        {
            if (Math.abs(dx) > Math.abs(dy))
                dy = 0;
            else
                dx = 0;
        }
        //v = findDraggedPositions(clickPos, baseVertPos, dx, dy, mv, e.isControlDown(), controller.getSelectionDistance());
        v = findSnapToPositions(clickPos, baseVertPos, dx, dy, mv, e.isControlDown(), controller.getSelectionDistance());

    //    mesh.setVertexPositions(v);
    //    controller.objectChanged();
    //    theWindow.updateImage();
        if (e.isControlDown())
            drag = view.getCamera().getCameraCoordinates().getZDirection().times(-dy*0.01);
        else
            drag = view.getCamera().findDragVector(clickPos, dx, dy);
        //theWindow.setHelpText(Translate.text("reshapeMeshTool.dragText",
        //  Math.round(drag.x*1e5)/1e5+", "+Math.round(drag.y*1e5)/1e5+", "+Math.round(drag.z*1e5)/1e5));
         */
    }

  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
    //manipulator.mouseReleased(e, view);
      /*
      Mesh mesh = (Mesh) controller.getObject().getObject();
      Point dragPoint = e.getPoint();
      int dx, dy;
      Vec3 v[];

      if(clickPoint == null){
          System.out.println("ERROR: clickPoint " + clickPoint);
          return;
      }
      System.out.println("clickPoint " + clickPoint + " dragPoint " + dragPoint);
      dx = dragPoint.x - clickPoint.x;
      dy = dragPoint.y - clickPoint.y;
      if (e.isShiftDown())
      {
        if (Math.abs(dx) > Math.abs(dy))
          dy = 0;
        else
          dx = 0;
      }
      if (dx != 0 || dy != 0)
      {
        if (undo != null)
          theWindow.setUndoRecord(undo);
        v = findDraggedPositions(clickPos, baseVertPos, dx, dy, (MeshViewer) view, e.isControlDown(), controller.getSelectionDistance());
      //  mesh.setVertexPositions(v);
      }
      //controller.objectChanged();
      //theWindow.updateImage();
      //theWindow.setHelpText(Translate.text("reshapeMeshTool.helpText"));
      undo = null;
      baseVertPos = null;
       */
  }
     

  protected void handlePressed(HandlePressedEvent ev)
  {
    Mesh mesh = (Mesh) controller.getObject().getObject();
    clickPoint = ev.getMouseEvent().getPoint();
    whichHandle = ev.getHandle();
    taperAll = ev.getMouseEvent().isShiftDown();
    towardCenter = ev.getMouseEvent().isControlDown();
    Rectangle r = ev.getScreenBounds();
    boundsHeight = (double) r.height;
    boundsWidth = (double) r.width;
    baseVertPos = mesh.getVertexPositions();
      
      System.out.println("SnapToTool  pressed " + baseVertPos.length );
  }
  
  protected void handleDragged(HandleDraggedEvent ev)
  {
    Mesh mesh = (Mesh) controller.getObject().getObject();
    if (undo == null)
      undo = new UndoRecord(theWindow, false, UndoRecord.COPY_VERTEX_POSITIONS, new Object [] {mesh, mesh.getVertexPositions()});
    Vec3 v[] = findTaperedPositions(baseVertPos, ev.getMouseEvent().getPoint(), ev.getSelectionBounds(), (MeshViewer) ev.getView());
    mesh.setVertexPositions(v);
    controller.objectChanged();
    theWindow.updateImage();
  }

  protected void handleReleased(HandleReleasedEvent ev)
  {
    Mesh mesh = (Mesh) controller.getObject().getObject();
    if (undo != null)
      theWindow.setUndoRecord(undo);
    Vec3 v[] = findTaperedPositions(baseVertPos, ev.getMouseEvent().getPoint(), ev.getSelectionBounds(), (MeshViewer) ev.getView());
    mesh.setVertexPositions(v);
    controller.objectChanged();
    theWindow.updateImage();
    undo = null;
    baseVertPos = null;
    dragInProgress = false;
  }

  /**
   * Find the new positions of the vertices after scaling.
   */

  private Vec3 [] findTaperedPositions(Vec3 vert[], Point pos, BoundingBox bounds, MeshViewer view)
  {
    Vec3 v[] = new Vec3 [vert.length];
    int selected[] = controller.getSelectionDistance();
    Camera cam = view.getCamera();
    double clickX, clickY, posX, posY, taper;
    Vec3 center;
    Mat4 m1, m2;
    int i, direction;
    
    clickX = (double) clickPoint.x;
    clickY = (double) clickPoint.y;
    posX = (double) pos.x;
    posY = (double) pos.y;
    
    // Figure out which way to taper the mesh, and toward which point.
    
    direction = (Math.abs(posX-clickX) > Math.abs(posY-clickY)) ? 0 : 1;
    center = new Vec3(0.0, 0.0, (bounds.minz+bounds.maxz)/2.0);
    if (bounds.minx == bounds.maxx || bounds.miny == bounds.maxy)
      taper = 0.0;
    else if (direction == 0)
      {
        if (towardCenter)
          center.x = (bounds.minx+bounds.maxx)/2.0;
        else if (whichHandle.isWest())
          center.x = bounds.minx;
        else
          center.x = bounds.maxx;
        if (whichHandle.isNorth())
          center.y = bounds.miny;
        else
          center.y = bounds.maxy;
        if (whichHandle.isWest())
          taper = (posX-clickX)/boundsWidth;
        else
          taper = (clickX-posX)/boundsWidth;
        if (taper > 1.0)
          taper = 1.0;
        if (whichHandle.isSouth())
          taper *= -1.0;
      }
    else
      {
        if (towardCenter)
          center.y = (bounds.miny+bounds.maxy)/2.0;
        else if (whichHandle.isNorth())
          center.y = bounds.miny;
        else
          center.y = bounds.maxy;
        if (whichHandle.isWest())
          center.x = bounds.minx;
        else
          center.x = bounds.maxx;
        if (whichHandle.isNorth())
          taper = (posY-clickY)/boundsHeight;
        else
          taper = (clickY-posY)/boundsHeight;
        if (taper > 1.0)
          taper = 1.0;
        if (whichHandle.isEast())
          taper *= -1.0;
      }

    // If the points are not being tapered, just copy them over.
    
    if (taper == 0.0)
      {
        for (i = 0; i < vert.length; i++)
          v[i] = new Vec3(vert[i]);
        return v;
      }

    // Find the transformation matrix.
    
    m1 = cam.getObjectToView();
    m1 = Mat4.translation(-center.x, -center.y, -center.z).times(m1);
    m2 = Mat4.translation(center.x, center.y, center.z);
    m2 = cam.getViewToWorld().times(m2);
    m2 = view.getDisplayCoordinates().toLocal().times(m2);
    
    // Determine the deltas.
    
    for (i = 0; i < vert.length; i++)
      {
        if (selected[i] == 0)
          {
            v[i] = m1.times(vert[i]);
            if (direction == 0)
              {
                v[i].x *= 1.0 - taper*v[i].y/(bounds.maxy-bounds.miny);
                if (taperAll)
                  v[i].z *= 1.0 - taper*v[i].y/(bounds.maxy-bounds.miny);
              }
            else
              {
                v[i].y *= 1.0 - taper*v[i].x/(bounds.maxx-bounds.minx);
                if (taperAll)
                  v[i].z *= 1.0 - taper*v[i].x/(bounds.maxx-bounds.minx);
              }
            v[i] = m2.times(v[i]).minus(vert[i]);
          }
        else
          v[i] = new Vec3();
      }
    if (theFrame instanceof MeshEditorWindow)
      ((MeshEditorWindow) theFrame).adjustDeltas(v);
    for (i = 0; i < vert.length; i++)
      v[i].add(vert[i]);
    return v;
  }
    
    private Vec3 [] findDraggedPositions(Vec3 pos, Vec3 vert[], double dx, double dy, MeshViewer view, boolean controlDown, int selectDist[])
    {
      int maxDistance = view.getController().getTensionDistance();
      double tension = view.getController().getMeshTension();
      Vec3 drag[] = new Vec3 [maxDistance+1], v[] = new Vec3 [vert.length];
      
      if (controlDown)
        drag[0] = view.getCamera().getCameraCoordinates().getZDirection().times(-dy*0.01);
      else
        drag[0] = view.getCamera().findDragVector(pos, dx, dy);
      for (int i = 1; i <= maxDistance; i++)
        drag[i] = drag[0].times(Math.pow((maxDistance-i+1.0)/(maxDistance+1.0), tension));
      if (view.getUseWorldCoords())
      {
        Mat4 trans = view.getDisplayCoordinates().toLocal();
        for (int i = 0; i < drag.length; i++)
          trans.transformDirection(drag[i]);
      }
      for (int i = 0; i < vert.length; i++)
      {
        if (selectDist[i] > -1)
          v[i] = vert[i].plus(drag[selectDist[i]]);
        else
          v[i] = new Vec3(vert[i]);
      }
      return v;
    }
    
    
    /**
     * findSnapToPositions
     */
    private Vec3 [] findSnapToPositions(Vec3 pos, Vec3 vert[], double dx, double dy, MeshViewer view, boolean controlDown, int selectDist[]){
        int maxDistance = view.getController().getTensionDistance();
        System.out.println("maxDistance: " + maxDistance + " dx: " + dx + " dy: " + dy);
        
        double tension = view.getController().getMeshTension();
        Vec3 drag[] = new Vec3 [maxDistance+1], v[] = new Vec3 [vert.length];
        
        if (controlDown) {
            System.out.println("controlDown");
          drag[0] = view.getCamera().getCameraCoordinates().getZDirection().times(-dy*0.01);
        } else {
            System.out.println("NOT controlDown");
          drag[0] = view.getCamera().findDragVector(pos, dx, dy);
        }
        
        for (int i = 1; i <= maxDistance; i++) {
          drag[i] = drag[0].times(Math.pow((maxDistance-i+1.0)/(maxDistance+1.0), tension));
        }
        
        if (view.getUseWorldCoords())
        {
          Mat4 trans = view.getDisplayCoordinates().toLocal();
          for (int i = 0; i < drag.length; i++)
            trans.transformDirection(drag[i]);
        }
        for (int i = 0; i < vert.length; i++)
        {
          if (selectDist[i] > -1)
            v[i] = vert[i].plus(drag[selectDist[i]]);
          else
            v[i] = new Vec3(vert[i]);
        }
        return v;
    }
    
}
