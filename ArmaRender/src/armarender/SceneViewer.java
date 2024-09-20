/*
 * Copyright (C) 2021-2023 by Jon Taylor Copyright (C) 1999-2011 by Peter Eastman
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 */

package armarender;

import armarender.image.ComplexImage;
import armarender.math.BoundingBox;
import armarender.math.CoordinateSystem;
import armarender.math.Mat4;
import armarender.math.Vec2;
import armarender.math.Vec3;
import armarender.object.CSGObject;
import armarender.object.Curve;
import armarender.object.DimensionLinearObject;
import armarender.object.DimensionObject;
import armarender.object.DirectionalLight;
import armarender.object.Mesh;
import armarender.object.MeshVertex;
import armarender.object.Object3D;
import armarender.object.ObjectInfo;
import armarender.object.SceneCamera;
import armarender.object.SpotLight;
import armarender.object.TriangleMesh;
import armarender.ui.EditingTool;
import armarender.ui.EditingWindow;
import armarender.view.CanvasDrawer;
import armarender.view.ViewerOrientationControl;
import buoy.event.MouseClickedEvent;
import buoy.event.MouseScrolledEvent;
import buoy.event.WidgetMouseEvent;
import buoy.widget.RowContainer;
import buoy.widget.Widget;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
// JProgressBar
import javax.swing.JFrame;
import javax.swing.JProgressBar;

/** The SceneViewer class is a component which displays a view of a Scene. */

public class SceneViewer extends ViewerCanvas {
  Scene theScene;
  EditingWindow parentFrame;
  Vector<ObjectInfo> cameras;
  boolean draggingBox, draggingSelectionBox, squareBox, sentClick, dragging;
  Point clickPoint, dragPoint;
  ObjectInfo clickedObject;
  private ExecutorService highlightExecutor;
  private Future<?> highlightFuture;
  int deselect;
  // int position = 0;
  RowContainer rowContainer;

  String screenText = "";
  ComputationalFluidDynamics cfd = null;
  boolean fsr = false;

  int CURVE_BORDER_CLICK_SIZE = 18;

  boolean drawContouredHighlight = true;

  // PointJoinObject pointJoin = new PointJoinObject();

  public SceneViewer(int position, Scene s, RowContainer p, EditingWindow fr) {
    this(position, s, p, fr, false);

    // CanvasDrawer drawer = super.drawer;
    CanvasDrawer d = getCanvasDrawer();

    d.setScale(s.getScale()); // JDT 2021 12 21 This has failed for some reason.

    parentFrame = fr;
    rowContainer = p;
    fsr = false;
    highlightExecutor = Executors.newSingleThreadExecutor();
  }

  public SceneViewer(int position, Scene s, RowContainer p, EditingWindow fr,
      boolean forceSoftwareRendering) {
    super(ArmaRender.getPreferences().getUseOpenGL() && isOpenGLAvailable()
        && !forceSoftwareRendering);
    this.position = position;
    theScene = s;
    rowContainer = p;
    parentFrame = fr;
    fsr = forceSoftwareRendering;
    addEventLink(MouseClickedEvent.class, this, "mouseClicked");
    draggingBox = draggingSelectionBox = false;
    cameras = new Vector<ObjectInfo>();
    buildChoices(p); // needs to have position set
    rebuildCameraList();
    setRenderMode(ArmaRender.getPreferences().getDefaultDisplayMode());
  }

  public void setCFD(ComputationalFluidDynamics cfd) {
    this.cfd = cfd;
  }

  /** Get the EditingWindow in which this canvas is displayed. */

  public EditingWindow getEditingWindow() {
    return parentFrame;
  }

  /**
   * Get the Scene displayed in this canvas.
   */

  public Scene getScene() {
    return theScene;
  }

  /** Add all SceneCameras in the scene to list of available views. */

  public void rebuildCameraList() {
    cameras.removeAllElements();
    for (int i = 0; i < theScene.getNumObjects(); i++) {
      ObjectInfo obj = theScene.getObject(i);
      if (obj.getObject() instanceof SceneCamera)
        cameras.addElement(obj);
    }
    for (int i = 0; i < theScene.getNumObjects(); i++) {
      ObjectInfo obj = theScene.getObject(i);
      if (obj.getObject() instanceof DirectionalLight || obj.getObject() instanceof SpotLight)
        cameras.addElement(obj);
    }
    for (Iterator iter = getViewerControlWidgets().values().iterator(); iter.hasNext();) {
      Widget w = (Widget) iter.next();
      if (w instanceof ViewerOrientationControl.OrientationChoice)
        ((ViewerOrientationControl.OrientationChoice) w).rebuildCameraList();
    }
  }

  /** Get the list of cameras in the scene which can be used as predefined orientations. */

  public ObjectInfo[] getCameras() {
    return cameras.toArray(new ObjectInfo[cameras.size()]);
  }

  /**
   * Deal with selecting a SceneCamera from the choice menu.
   *
   * Description:
   */
  public void setOrientation(int which) {
    super.setOrientation(which);

    // JDT: if a selection exists, centre view on selection.
    if (theScene != null) {
      int[] selection = theScene.getSelection();
      Vector<ObjectInfo> objects = theScene.getObjects();
      if (selection.length > 0) {
        BoundingBox selectionBounds = null;
        Vec3 target = null;
        for (int i = 0; i < selection.length; i++) {
          ObjectInfo selObj = objects.elementAt(selection[i]);
          BoundingBox bb = selObj.getTranslatedBounds();
          if (selectionBounds == null) {
            selectionBounds = bb;
          } else {
            selectionBounds = selectionBounds.merge(bb);
          }
          target = selectionBounds.getCenter();
        }
        // Tell ViewerVanvas to view selection point.
        if (target != null) {
          super.setViewTarget(which, target);
        }
      }
    }

    //
    //
    // System.out.println("SceneViewer.setOrientation: which " + which + " pos " + position);

    if (which > 5 && which < 6 + cameras.size()) // camera object selected. > front, side, top,
    {
      boundCamera = cameras.elementAt(which - 6);

      CoordinateSystem coords = theCamera.getCameraCoordinates();
      coords.copyCoords(boundCamera.getCoords());
      theCamera.setCameraCoordinates(coords);

      if (boundCamera != null && boundCamera.getObject() instanceof SceneCamera) {
        SceneCamera sc = (SceneCamera) boundCamera.getObject();
        setScale(sc.getScale());

        // Also set/init the perspective if the boundCamera object property isPerspective() is true.
        if (sc.isPerspective()) {
          setPerspective(true);
        } else {
          setPerspective(false);
        }
        // ?? Update object properties dialog
      }

      viewChanged(false);
      repaint();
    } else {
      boundCamera = null;
      viewChanged(false);
    }
  }

  /**
   * Estimate the range of depth values that the camera will need to render. This need not be exact,
   * but should err on the side of returning bounds that are slightly too large.
   * 
   * @return the two element array {minDepth, maxDepth}
   */

  public double[] estimateDepthRange() {
    double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
    Mat4 toView = theCamera.getWorldToView();
    for (int i = 0; i < theScene.getNumObjects(); i++) {
      ObjectInfo info = theScene.getObject(i);
      BoundingBox bounds =
          info.getBounds().transformAndOutset(toView.times(info.coords.fromLocal()));
      if (bounds.minz < min)
        min = bounds.minz;
      if (bounds.maxz > max)
        max = bounds.maxz;
    }
    return new double[] {min, max};
  }

  @Override
  public Vec3 getDefaultRotationCenter() {
    int selection[] = null;
    if (parentFrame instanceof LayoutWindow)
      selection = ((LayoutWindow) parentFrame).getSelectedIndices();
    if (selection == null || selection.length == 0) {
      CoordinateSystem coords = theCamera.getCameraCoordinates();
      double distToCenter = -coords.getZDirection().dot(coords.getOrigin());
      return coords.getOrigin().plus(coords.getZDirection().times(distToCenter));
    }
    BoundingBox bounds = null;
    for (int i = 0; i < selection.length; i++) {
      ObjectInfo info = theScene.getObject(selection[i]);
      BoundingBox objBounds = info.getBounds().transformAndOutset(info.getCoords().fromLocal());
      bounds = (i == 0 ? objBounds : bounds.merge(objBounds));
    }
    return bounds.getCenter();
  }

  @Override
  public void viewChanged(boolean selectionOnly) {
    super.viewChanged(selectionOnly);
    if (renderMode == RENDER_RENDERED && !selectionOnly) {
      // Re-render the image.

      Renderer rend = ArmaRender.getPreferences().getObjectPreviewRenderer();
      if (rend == null)
        return;
      adjustCamera(true);
      Camera cam = theCamera.duplicate();
      rend.configurePreview();
      Rectangle bounds = getBounds();
      SceneCamera sceneCamera = new SceneCamera();
      sceneCamera.setFieldOfView(
          Math.atan(0.5 * bounds.height / cam.getViewToScreen().m33) * 360.0 / Math.PI);
      adjustCamera(isPerspective());
      RenderListener listener = new RenderListener() {
        public void imageUpdated(Image image) {
          renderedImage = image;
          getCanvasDrawer().imageChanged(renderedImage);
          repaint();
        }

        public void statusChanged(String status) {}

        public void imageComplete(ComplexImage image) {
          renderedImage = image.getImage();
          getCanvasDrawer().imageChanged(renderedImage);
          repaint();
        }

        public void renderingCanceled() {}
      };
      rend.renderScene(theScene, cam, listener, sceneCamera);
    }
  }

  /**
   * updateImage
   *
   * Description: Called by LayoutWindow to redraw scene objects.
   */
  public synchronized void updateImage() {
    if (renderMode == RENDER_RENDERED) {
      if (renderedImage != null && renderedImage.getWidth(null) > 0)
        drawImage(renderedImage, 0, 0);
      else
        viewChanged(false);
    } else {
      super.updateImage();

      // Draw the objects.
      Vec3 cameraLocation = theCamera.getCameraCoordinates().getOrigin();
      Vec3 viewdir = theCamera.getViewToWorld().timesDirection(Vec3.vz());

      
        long startTime = System.currentTimeMillis();
        
        //
        // Order set of scene objects by distance to the camera.
        // We only need to do this because some objects rendered as transparent polygons don't respect the canvas drawer zbuffer.
        // Works but it's slow. best solution might be to implement zbuffer in transparent poly drawer.
        // 70 ms to 300-400ms
        //
        /*
        TreeMap<Double, ObjectInfo> sortedObjectsMap = new TreeMap<Double, ObjectInfo>();
        for(int i = 0; i < theScene.getNumObjects(); i++){
            ObjectInfo obj = theScene.getObject(i);
            if (obj == boundCamera || !obj.isVisible() || obj.isHiddenByParent())
              continue;
            double dist = cameraLocation.distance(obj.getTranslatedBounds().getCenter());
            sortedObjectsMap.put(-dist, obj);
        }
        Iterator it = sortedObjectsMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            double dist = (Double)pairs.getKey();
            ObjectInfo obj = (ObjectInfo)pairs.getValue();
            theCamera.setObjectTransform(obj.getCoords().fromLocal());
            obj.getObject().renderObject(obj, this, viewdir);
        }
        */
         
	        

      long startTime = System.currentTimeMillis();

      //
      // Order set of scene objects by distance to the camera.
      // We only need to do this because some objects rendered as transparent polygons don't respect
      // the canvas drawer zbuffer.
      // Works but it's slow. best solution might be to implement zbuffer in transparent poly
      // drawer.
      // 70 ms to 300-400ms
      //
      /*
       * TreeMap<Double, ObjectInfo> sortedObjectsMap = new TreeMap<Double, ObjectInfo>(); for(int i
       * = 0; i < theScene.getNumObjects(); i++){ ObjectInfo obj = theScene.getObject(i); if (obj ==
       * boundCamera || !obj.isVisible() || obj.isHiddenByParent()) continue; double dist =
       * cameraLocation.distance(obj.getTranslatedBounds().getCenter()); sortedObjectsMap.put(-dist,
       * obj); } Iterator it = sortedObjectsMap.entrySet().iterator(); while (it.hasNext()) {
       * Map.Entry pairs = (Map.Entry) it.next(); double dist = (Double)pairs.getKey(); ObjectInfo
       * obj = (ObjectInfo)pairs.getValue();
       * theCamera.setObjectTransform(obj.getCoords().fromLocal());
       * obj.getObject().renderObject(obj, this, viewdir); }
       */

      // Multi thread. Do we need a pool and a worker for each screen tile here to renter the
      // objects into each version of the camera angled in a different region?



      // Existing render iteration.
      for (int i = 0; i < theScene.getNumObjects(); i++) {
        ObjectInfo obj = theScene.getObject(i);
        if (obj == boundCamera || !obj.isVisible() || obj.isHiddenByParent()) //
          continue;

        // Note: Mult threaded implementation will need a new 'Camera getCamera( int tile )' for
        // each tile region.
        // getScreenRegionCamera( )

        theCamera.setObjectTransform(obj.getCoords().fromLocal()); // Don't really like camera as
                                                                   // global state, should be passed
                                                                   // into renderObject.
        obj.getObject().renderObject(obj, this, viewdir);
      }


      long endTime = System.currentTimeMillis();
      if ((endTime - startTime) > 500) {
        System.out.println("Time: " + (endTime - startTime));
      }
      // FPS Frame rate.
      if ((endTime - startTime) > 0) {
        fps = 1000 / (endTime - startTime);
      }
    }

    // Hilight the selection.

    if (currentTool.hilightSelection()) {
      ArrayList<Rectangle> selectedBoxes = new ArrayList<Rectangle>();
      ArrayList<Rectangle> parentSelectedBoxes = new ArrayList<Rectangle>();
      for (int i = 0; i < theScene.getNumObjects(); i++) {
        int hsize;
        ArrayList<Rectangle> boxes;
        ObjectInfo obj = theScene.getObject(i);
        if (obj.isLocked())
          continue;
        if (obj.selected) {
          hsize = Scene.HANDLE_SIZE;
          boxes = selectedBoxes;
        } else if (obj.parentSelected) {
          hsize = Scene.HANDLE_SIZE / 2;
          boxes = parentSelectedBoxes;
        } else
          continue;

        theCamera.setObjectTransform(obj.getCoords().fromLocal());
        Rectangle bounds = theCamera.findScreenBounds(obj.getBounds());
        expandBounds(bounds); // expand highlight.
        if (bounds != null) {
          boxes.add(new Rectangle(bounds.x, bounds.y, hsize, hsize)); // add box top left
          boxes.add(new Rectangle(bounds.x + bounds.width - hsize + 1, bounds.y, hsize, hsize));
          boxes.add(new Rectangle(bounds.x, bounds.y + bounds.height - hsize + 1, hsize, hsize));
          boxes.add(new Rectangle(bounds.x + bounds.width - hsize + 1,
              bounds.y + bounds.height - hsize + 1, hsize, hsize));
          boxes.add(new Rectangle(bounds.x + (bounds.width - hsize) / 2, bounds.y, hsize, hsize));
          boxes.add(new Rectangle(bounds.x, bounds.y + (bounds.height - hsize) / 2, hsize, hsize));
          boxes.add(new Rectangle(bounds.x + (bounds.width - hsize) / 2,
              bounds.y + bounds.height - hsize + 1, hsize, hsize));
          boxes.add(new Rectangle(bounds.x + bounds.width - hsize + 1,
              bounds.y + (bounds.height - hsize) / 2, hsize, hsize));
        }
      } // for objects

      drawBoxes(selectedBoxes, handleColor); // red main window, non edit. outline
                                             // drawBoxes(selectedBoxes, new Color(0, 255, 0) ); //
      drawBoxes(parentSelectedBoxes, highlightColor); // ???

      // drawBoxes(selectedBoxes, new Color(0, 255, 0) );
      // Custom highlight renderer
      if (parentFrame instanceof LayoutWindow && drawContouredHighlight == true) {
        highlightFuture = highlightExecutor.submit(() -> {
          ContouredHighlight contouredHighlight = new ContouredHighlight();
          contouredHighlight.setSceneViewer(this);
          contouredHighlight.setLayoutWindow((LayoutWindow) parentFrame);
          contouredHighlight.renderHighlight();
        });
      }


    } // tool highlight

    // Contoured Highlight function.
    // - for each side, shrink until intersect with scene object selection vert.
    //


    // Draw CFD result text on screen ???
    if (cfd != null) {
      cfd.drawText();
    }


    // Finish up.

    drawBorder();
    if (showAxes)
      drawCoordinateAxes();
  }

  /**
   * Begin dragging a box. The variable square determines whether the box should be constrained to
   * be square.
   */

  public void beginDraggingBox(Point p, boolean square) {
    draggingBox = true;
    clickPoint = p;
    squareBox = square;
    dragPoint = null;
  }

  /**
   * When the user presses the mouse, forward events to the current tool as appropriate. If this is
   * an object based tool, allow them to select or deselect objects.
   * 
   * Not editor window.
   */

  protected void mousePressed(WidgetMouseEvent e) {
    // System.out.println("SceneViewer.mousePressed() ");
    int i, j, k, sel[], minarea;
    Rectangle bounds = null;
    ObjectInfo info;
    Point p;
    p = e.getPoint();
    Vec3 clickVecA = theCamera.convertScreenToWorld(p, 0, false);
    Vec3 clickVecB = theCamera.convertScreenToWorld(p, 1, false);

    requestFocus();
    sentClick = false;
    deselect = -1;
    dragging = true;
    clickPoint = e.getPoint();
    clickedObject = null;

    // Stop rotation
    if (parentFrame instanceof LayoutWindow) {
      ((LayoutWindow) parentFrame).stopRotateScene();
    }

    // Determine which tool is active.
    // metaTool = PAN, altDown = Rotate
    if (metaTool != null && (e.isMetaDown() || e.getButton() == 3)) { // 3 is right mouse button
      activeTool = metaTool;
    } else if (altTool != null && e.isAltDown()) {
      activeTool = altTool;
    } else {
      activeTool = currentTool;
    }

    // If the current tool wants all clicks, just forward the event.

    if ((activeTool.whichClicks() & EditingTool.ALL_CLICKS) != 0) {
      moveToGrid(e);
      activeTool.mousePressed(e, this);
      sentClick = true;
    }
    boolean allowSelectionChange = activeTool.allowSelectionChanges();
    boolean wantHandleClicks = ((activeTool.whichClicks() & EditingTool.HANDLE_CLICKS) != 0);
    if (!allowSelectionChange && !wantHandleClicks)
      return;



    // JDT:
    // Send click event to Object to check if verticies are being modified.
    // Use: CurveViewer.mousePressed(w) or add function to Curve
    // theCamera
    //

    /*
     * PointJoinObject createPointJoin = theScene.getCreatePointJoinObject(); p = e.getPoint(); int
     * HANDLE_SIZE = 5; sel = theScene.getSelection(); boolean pointClicked = false; for (i = 0; i <
     * sel.length; i++) { info = theScene.getObject(sel[i]); if(info.getObject() instanceof Curve){
     * //System.out.println(" curve click check for vertex ");
     * theCamera.setObjectTransform(info.getCoords().fromLocal()); MeshVertex v[] = ((Mesh)
     * info.getObject()).getVertices(); Vec2 pos; for (int iv = 0; iv < v.length; iv++) { pos =
     * theCamera.getObjectToScreen().timesXY(v[iv].r); int x = (int) pos.x; int y = (int) pos.y;
     * //System.out.println(" x " + x + " y " + y); if (x >= p.x-HANDLE_SIZE/2 && x <=
     * p.x+HANDLE_SIZE/2 && y >= p.y-HANDLE_SIZE/2 && y <= p.y+HANDLE_SIZE/2) { pointClicked = true;
     * // Second point selection if(e.isShiftDown() && createPointJoin.objectA > 0){
     * createPointJoin.objectB = info.getId(); createPointJoin.objectBPoint = iv;
     * createPointJoin.objectBSubPoint = -1; } // First point selection if(e.isShiftDown() == false
     * || createPointJoin.objectA <= 0){ createPointJoin.objectA = info.getId();
     * createPointJoin.objectAPoint = iv; createPointJoin.objectASubPoint = -1; }
     * System.out.println(" *** Click  Object: " + info.getName() + " point: " + iv + " *** " );
     * //System.out.println("    A: " + createPointJoin.objectA + " point: " +
     * createPointJoin.objectAPoint + " " ); //System.out.println("    B: " +
     * createPointJoin.objectB + " point: " + createPointJoin.objectBPoint + " " ); } } //
     * subdivided verticies Curve curve = (Curve)info.getObject(); Vec3[] subdividedPoints =
     * curve.getSubdividedVertices(); //System.out.println(" sub points: "+ subdividedPoints.length
     * ); for(int iv = 0; subdividedPoints != null && iv < subdividedPoints.length; iv++){ Vec3 vec
     * = subdividedPoints[iv]; pos = theCamera.getObjectToScreen().timesXY(vec); int x = (int)
     * pos.x; int y = (int) pos.y; if (x >= p.x-HANDLE_SIZE/2 && x <= p.x+HANDLE_SIZE/2 && y >=
     * p.y-HANDLE_SIZE/2 && y <= p.y+HANDLE_SIZE/2) {
     * System.out.println(" click on subdivided point. " ); pointClicked = true; // Second point
     * selection if(e.isShiftDown() && createPointJoin.objectA > 0){ createPointJoin.objectB =
     * info.getId(); createPointJoin.objectBSubPoint = iv; createPointJoin.objectBPoint = -1; } //
     * First point selection if(e.isShiftDown() == false || createPointJoin.objectA <= 0){
     * createPointJoin.objectA = info.getId(); createPointJoin.objectASubPoint = iv;
     * createPointJoin.objectAPoint = -1; } } } } if(info.getObject() instanceof SplineMesh){
     * //System.out.println(" *** Click  Spline mesh: ");
     * theCamera.setObjectTransform(info.getCoords().fromLocal()); MeshVertex v[] = ((Mesh)
     * info.getObject()).getVertices(); Vec2 pos; for (int iv = 0; iv < v.length; iv++) { pos =
     * theCamera.getObjectToScreen().timesXY(v[iv].r); int x = (int) pos.x; int y = (int) pos.y;
     * //System.out.println(" x " + x + " y " + y); if (x >= p.x-HANDLE_SIZE/2 && x <=
     * p.x+HANDLE_SIZE/2 && y >= p.y-HANDLE_SIZE/2 && y <= p.y+HANDLE_SIZE/2) {
     * System.out.println("Mesh point click x "); pointClicked = true; // Second point selection
     * if(e.isShiftDown() && createPointJoin.objectA > 0){ createPointJoin.objectB = info.getId();
     * createPointJoin.objectBPoint = iv; createPointJoin.objectBSubPoint = -1; } // First point
     * selection if(e.isShiftDown() == false || createPointJoin.objectA <= 0){
     * createPointJoin.objectA = info.getId(); createPointJoin.objectAPoint = iv;
     * createPointJoin.objectASubPoint = -1; } } } } } // Deselect points if(pointClicked == false
     * && e.isShiftDown() == false){ createPointJoin.objectA = 0; createPointJoin.objectB = 0;
     * //System.out.println(" clear point selection "); }
     */

    // See whether the click was on a currently selected object.
    p = e.getPoint();
    sel = theScene.getSelection();
    for (i = 0; i < sel.length; i++) {
      info = theScene.getObject(sel[i]);
      theCamera.setObjectTransform(info.getCoords().fromLocal());
      bounds = theCamera.findScreenBounds(info.getBounds());
      expandBounds(bounds);
      if (!info.isLocked() && bounds != null && !info.isChildrenHiddenWhenHidden()
          && pointInRectangle(p, bounds)) {
        boolean clickedOnObject = true;
        if (info.getObject().canConvertToTriangleMesh() != Object3D.CANT_CONVERT //
            && info.getObject() instanceof Curve == false // Not Curve
            && isPerspective() == false) { //
          boolean r = intersectsVector(info, clickVecA, clickVecB);
          if (r == false) {
            clickedOnObject = false;
          }
        }
        if (info.getObject() instanceof Curve
            && info.getObject() instanceof DimensionObject == false && // don't do this for
                                                                       // dimension objects.
            info.getObject() instanceof DimensionLinearObject == false) {
          boolean r = intersectsCurve(theCamera, info, p);
          if (r == false) {
            clickedOnObject = false;
          }
        }
        if (activeTool instanceof MoveObjectTool == false
            && activeTool instanceof MoveClosestObjectTool == false) {
          clickedOnObject = true;
        }
        if (clickedOnObject) {
          clickedObject = info;
          break;
        }
      }
    }
    if (i < sel.length) {
      // The click was on a selected object. If it was a shift-click, the user may want
      // to deselect it, so set a flag.

      if (e.isShiftDown() && allowSelectionChange) {
        deselect = sel[i];
      }

      // If the current tool wants handle clicks, then check to see whether the click
      // was on a handle.

      if ((activeTool.whichClicks() & EditingTool.HANDLE_CLICKS) != 0) {
        if (p.x <= bounds.x + Scene.HANDLE_SIZE)
          j = 0;
        else if (p.x >= bounds.x + (bounds.width - Scene.HANDLE_SIZE) / 2
            && p.x <= bounds.x + (bounds.width - Scene.HANDLE_SIZE) / 2 + Scene.HANDLE_SIZE)
          j = 1;
        else if (p.x >= bounds.x + bounds.width - Scene.HANDLE_SIZE)
          j = 2;
        else
          j = -1;
        if (p.y <= bounds.y + Scene.HANDLE_SIZE)
          k = 0;
        else if (p.y >= bounds.y + (bounds.height - Scene.HANDLE_SIZE) / 2
            && p.y <= bounds.y + (bounds.height - Scene.HANDLE_SIZE) / 2 + Scene.HANDLE_SIZE)
          k = 1;
        else if (p.y >= bounds.y + bounds.height - Scene.HANDLE_SIZE)
          k = 2;
        else
          k = -1;
        if (k == 0) {
          moveToGrid(e);
          activeTool.mousePressedOnHandle(e, this, sel[i], j);
          sentClick = true;
          return;
        }
        if (j == 0 && k == 1) {
          moveToGrid(e);
          activeTool.mousePressedOnHandle(e, this, sel[i], 3);
          sentClick = true;
          return;
        }
        if (j == 2 && k == 1) {
          moveToGrid(e);
          activeTool.mousePressedOnHandle(e, this, sel[i], 4);
          sentClick = true;
          return;
        }
        if (k == 2) {
          moveToGrid(e);
          activeTool.mousePressedOnHandle(e, this, sel[i], j + 5);
          sentClick = true;
          return;
        }
      }
      moveToGrid(e);
      dragging = false;
      if ((activeTool.whichClicks() & EditingTool.OBJECT_CLICKS) != 0) {
        activeTool.mousePressedOnObject(e, this, sel[i]);
        sentClick = true;
      }
      return;
    }
    if (!allowSelectionChange)
      return;

    // The click was not on a selected object. See whether it was on an unselected object.
    // If so, select it. If appropriate, send an event to the current tool.

    // If the click was on top of multiple objects, the conventional thing to do is to select
    // the closest one. I'm trying something different: select the smallest one. This
    // should make it easier to select small objects which are surrounded by larger objects.
    // I may decide to change this, but it seemed like a good idea at the time...

    // JDT: I'm adding a list to explicitly select from the list of objects.
    if (parentFrame instanceof LayoutWindow) {
      ((LayoutWindow) parentFrame).clearSelectionPickerList();
      ((LayoutWindow) parentFrame).stopRotateScene();
    }
    for (i = 0; i < theScene.getNumObjects(); i++) {
      info = theScene.getObject(i);
      if (info.isVisible() && !info.isLocked() && !info.isChildrenHiddenWhenHidden()) //
      {
        theCamera.setObjectTransform(info.getCoords().fromLocal());
        bounds = theCamera.findScreenBounds(info.getBounds());
        expandBounds(bounds);
        if (bounds != null && pointInRectangle(p, bounds)) {

          boolean clickedOnObject = true;

          if (info.getObject().canConvertToTriangleMesh() != Object3D.CANT_CONVERT // Is (or can be)
                                                                                   // Mesh
              && info.getObject() instanceof Curve == false // Not Curve
              && isPerspective() == false) { // Only works in parallel
            boolean r = intersectsVector(info, clickVecA, clickVecB);
            if (r == false) {
              clickedOnObject = false;
              // System.out.println(" intersect mesh FALSE " + info.getName() );
            } else {
              // System.out.println(" intersect mesh TRUE " + info.getName() );
            }
          }

          if (info.getObject() instanceof Curve
              && info.getObject() instanceof DimensionObject == false && // don't do this for
                                                                         // dimension objects.
              info.getObject() instanceof DimensionLinearObject == false) {
            boolean r = intersectsCurve(theCamera, info, p);
            if (r == false) {
              clickedOnObject = false;
              // System.out.println(" intersect curve FALSE " + info.getName() );
            }
          }

          if (activeTool instanceof MoveObjectTool == false
              && activeTool instanceof MoveClosestObjectTool == false) {
            clickedOnObject = true;
          }
          if (clickedOnObject) {
            if (parentFrame instanceof LayoutWindow) {
              ((LayoutWindow) parentFrame).addToSelectionPickerList(i);
            }
          }
        }
      }
    }
    // END JDT

    j = -1;
    minarea = Integer.MAX_VALUE;
    for (i = 0; i < theScene.getNumObjects(); i++) {
      info = theScene.getObject(i);
      if (info.isVisible() && !info.isLocked() && !info.isChildrenHiddenWhenHidden()) // scene obj
                                                                                      // is visible
      {
        theCamera.setObjectTransform(info.getCoords().fromLocal());
        bounds = theCamera.findScreenBounds(info.getBounds()); // obj bounds
        expandBounds(bounds);
        if (bounds != null && pointInRectangle(p, bounds))
          if (bounds.width * bounds.height < minarea) // Chooses smallest object from set selected.
          {
            // If info is mesh type, also filter on point (p) lays on mesh face or edge.
            boolean clickedOnObject = true;
            if (info.getObject().canConvertToTriangleMesh() != Object3D.CANT_CONVERT
                && info.getObject() instanceof Curve == false // Not Curve
                && isPerspective() == false) { // only works in parallel
              boolean r = intersectsVector(info, clickVecA, clickVecB);
              if (r == false) {
                clickedOnObject = false;
              }
            }
            if (info.getObject() instanceof Curve
                && info.getObject() instanceof DimensionObject == false && // don't do this for
                                                                           // dimension objects.
                info.getObject() instanceof DimensionLinearObject == false) {
              boolean r = intersectsCurve(theCamera, info, p);
              if (r == false) {
                clickedOnObject = false;
              }
            }
            if (activeTool instanceof MoveObjectTool == false
                && activeTool instanceof MoveClosestObjectTool == false) {
              clickedOnObject = true;
            }
            if (clickedOnObject) {
              j = i;
              minarea = bounds.width * bounds.height;
            }
          }
      }
    }
    if (j > -1) {
      info = theScene.getObject(j);
      if (!e.isShiftDown()) {
        if (parentFrame instanceof LayoutWindow)
          ((LayoutWindow) parentFrame).clearSelection();
        else
          theScene.clearSelection();
      }
      if (parentFrame instanceof LayoutWindow) {
        parentFrame.setUndoRecord(
            new UndoRecord(parentFrame, false, UndoRecord.SET_SCENE_SELECTION, new Object[] {sel}));
        ((LayoutWindow) parentFrame).addToSelection(j);
      } else
        theScene.addToSelection(j);
      parentFrame.updateMenus();
      parentFrame.updateImage();
      moveToGrid(e);
      if ((activeTool.whichClicks() & EditingTool.OBJECT_CLICKS) != 0 && !e.isShiftDown()) {
        sentClick = true;
        activeTool.mousePressedOnObject(e, this, j);
      }
      clickedObject = info;
      return;
    }

    // The click was not on any object. Start dragging a selection box.

    if (allowSelectionChange) {
      moveToGrid(e);
      draggingSelectionBox = true;
      beginDraggingBox(p, false);
    }
    sentClick = false;
  }


  /**
   * intersectsCurve
   *
   * Description: does a click fall on a curve expanded boundary. 1) Find the 2D screen coordinates
   * of the curve points. 2)
   */
  public boolean intersectsCurve(Camera camera, ObjectInfo info, Point p) { //
    boolean result = false;
    int HANDLE_SIZE = 8;
    Object obj = info.getObject();
    if (obj instanceof Curve) {
      Curve curve = (Curve) obj;
      camera.setObjectTransform(info.duplicate().getCoords().fromLocal());
      Mat4 mat4Camera = camera.duplicate().getObjectToScreen();
      MeshVertex v[] = ((Mesh) curve).getVertices();
      Vector curvePoints2d = new Vector();
      for (int i = 0; i < v.length; i++) {
        Vec3 vec = new Vec3(v[i].r);
        Vec2 pos = mat4Camera.timesXY(vec);
        curvePoints2d.addElement(new Point((int) pos.x, (int) pos.y));
      }
      // iterate points
      int closestPoint = 99999;
      for (int i = 1; i < curvePoints2d.size(); i++) {
        Point p1 = (Point) curvePoints2d.elementAt(i - 1);
        Point p2 = (Point) curvePoints2d.elementAt(i - 0);
        float dist = pointDistance(p.x, p.y, p1.x, p1.y, p2.x, p2.y);
        if (dist < closestPoint) {
          closestPoint = (int) dist;
        }
        if (dist < CURVE_BORDER_CLICK_SIZE) {
          return true;
        }
      }
      if (curve.isClosed()) { // Process the closing line segment.
        Point p1 = (Point) curvePoints2d.elementAt(0);
        Point p2 = (Point) curvePoints2d.elementAt(curvePoints2d.size() - 1);
        float dist = pointDistance(p.x, p.y, p1.x, p1.y, p2.x, p2.y);
        if (dist < closestPoint) {
          closestPoint = (int) dist;
        }
        if (dist < CURVE_BORDER_CLICK_SIZE) {
          return true;
        }
      }

      if (closestPoint < CURVE_BORDER_CLICK_SIZE) {
        result = true;
      }

      // System.out.println("intersectsCurve : " + result + " closestPoint: " + closestPoint);
    }
    return result;
  }

  /**
   * pointDistance
   *
   */
  public float pointDistance(float x, float y, float x1, float y1, float x2, float y2) {
    float A = x - x1; // Position of point rel one end of line
    float B = y - y1;
    float C = x2 - x1; // Vector along line
    float D = y2 - y1;
    float E = -D; // Orthogonal vector
    float F = C;
    float dot = A * E + B * F;
    float len_sq = E * E + F * F;
    return (float) Math.abs(dot) / (float) Math.sqrt(len_sq);
  }

  /**
   * intersectsVector
   *
   * Description: Does a face in this object collide with a click vector. NOTE: This only works in
   * parallel projection.
   *
   */
  public boolean intersectsVector(ObjectInfo info, Vec3 a, Vec3 b) {
    boolean result = false;
    LayoutModeling layout = new LayoutModeling();
    Object obj = info.getObject();
    // if(info.getObject() instanceof Mesh){
    if (info.getObject().canConvertToTriangleMesh() != Object3D.CANT_CONVERT
        && info.getObject() instanceof armarender.object.CSGObject == false) {
      Object3D o3d = info.getObject();
      CoordinateSystem c;
      c = layout.getCoords(info);
      // System.out.println(" intersectsVector convertToTriangleMesh: --- " + info.getName() );
      // System.out.println(" object " + info.getObject().getClass().getName() );
      TriangleMesh triangleMesh = null;
      triangleMesh = info.getObject().convertToTriangleMesh(0.05);
      if (triangleMesh == null) {
        // System.
        return true; // If no mesh available, return true and revert to bounds check.
      }
      MeshVertex[] verts = triangleMesh.getVertices();
      TriangleMesh.Face[] faces = triangleMesh.getFaces();
      boolean meshColided = IntStream.range(0, faces.length).parallel().anyMatch(f -> {
        TriangleMesh.Face face = faces[f];
        Vec3 vec1 = new Vec3(verts[face.v1].r);
        Vec3 vec2 = new Vec3(verts[face.v2].r);
        Vec3 vec3 = new Vec3(verts[face.v3].r);
        Mat4 mat4 = c.duplicate().fromLocal();
        mat4.transform(vec1);
        mat4.transform(vec2);
        mat4.transform(vec3);
        double colDist = rayTriangleIntersect(a, b, vec1, vec2, vec3);
        double colDist2 = rayTriangleIntersect(a, b, vec3, vec2, vec1);
        return colDist != Double.MIN_VALUE || colDist2 != Double.MIN_VALUE;
      });

      if (meshColided) {
        result = true;
      }
    } else {
      result = true;
    }

    if (info.getObject() instanceof armarender.object.CSGObject) {
      System.out.println(
          "Warning: Can't accurately pick from CSGObjects. Using bounds instead of polygons.");
      CSGObject csg = (CSGObject) info.getObject();
      // CoordinateSystem c;
      // c = layout.getCoords(info);
      /*
       * SoftReference<RenderingMesh> cachedMesh;
       * 
       * RenderingTriangle triangle[] = csg.triangle; for (i = 0; i < triangle.length; i++) { Vec3
       * v1 = csg.vert[triangle[i].v1]; Vec3 v2 = csg.vert[triangle[i].v2]; Vec3 v3 =
       * csg.vert[triangle[i].v3]; System.out.println(" csg vec " + v1); }
       */

      result = true; // Can't currently process CSG objects for performance reasons.
    }

    return result;
  }


  /**
   * rayTriangleIntersect
   *
   * Description: Given a click vector determine if the path intersects a polygon by three points.
   */
  public double rayTriangleIntersect(Vec3 rayA, Vec3 rayB, Vec3 v0, Vec3 v1, Vec3 v2) {
    Vec3 rayNormal = new Vec3(rayB);
    rayNormal.subtract(rayA);
    rayNormal.normalize();
    Vec3 v0v1 = v1.minus(v0);
    Vec3 v0v2 = v2.minus(v0);
    Vec3 pvec = rayNormal.cross(v0v2);
    double det = v0v1.dot(pvec);
    if (det < 0.000001) {
      return Double.MIN_VALUE;
    }
    double invDet = (double) (1.0 / det);
    Vec3 tvec = rayA.minus(v0);
    double u = tvec.dot(pvec) * invDet;
    if (u < 0 || u > 1) {
      return Double.MIN_VALUE;
    }
    Vec3 qvec = tvec.cross(v0v1);
    double v = rayNormal.dot(qvec) * invDet;
    if (v < 0 || u + v > 1) {
      return Double.MIN_VALUE;
    }
    double result = (v0v2.dot(qvec) * invDet);
    return result;
  }


  /**
   * Determine whether a Point falls inside a Rectangle. This method allows a 1 pixel tolerance to
   * make it easier to click on very small objects.
   */

  private boolean pointInRectangle(Point p, Rectangle r) {
    return (r.x - 1 <= p.x && r.y - 1 <= p.y && r.x + r.width + 1 >= p.x
        && r.y + r.height + 1 >= p.y);
  }

  /**
   * mouseDragged
   *
   * Description: Mouse dragged on main viewer canvas.
   */
  protected void mouseDragged(WidgetMouseEvent e) {
    // System.out.println("mouseDragged. "); // option command
    moveToGrid(e);
    if (!dragging) {
      Point p = e.getPoint();
      if (Math.abs(p.x - clickPoint.x) < 2 && Math.abs(p.y - clickPoint.y) < 2)
        return;
    }
    dragging = true;
    deselect = -1;
    if (draggingBox) {
      // We are dragging a box, so erase and redraw it.

      if (dragPoint != null)
        drawDraggedShape(
            new Rectangle(Math.min(clickPoint.x, dragPoint.x), Math.min(clickPoint.y, dragPoint.y),
                Math.abs(dragPoint.x - clickPoint.x), Math.abs(dragPoint.y - clickPoint.y)));
      dragPoint = e.getPoint();
      if (squareBox) {
        if (Math.abs(dragPoint.x - clickPoint.x) > Math.abs(dragPoint.y - clickPoint.y)) {
          if (dragPoint.y < clickPoint.y)
            dragPoint.y = clickPoint.y - Math.abs(dragPoint.x - clickPoint.x);
          else
            dragPoint.y = clickPoint.y + Math.abs(dragPoint.x - clickPoint.x);
        } else {
          if (dragPoint.x < clickPoint.x)
            dragPoint.x = clickPoint.x - Math.abs(dragPoint.y - clickPoint.y);
          else
            dragPoint.x = clickPoint.x + Math.abs(dragPoint.y - clickPoint.y);
        }
      }
      drawDraggedShape(
          new Rectangle(Math.min(clickPoint.x, dragPoint.x), Math.min(clickPoint.y, dragPoint.y),
              Math.abs(dragPoint.x - clickPoint.x), Math.abs(dragPoint.y - clickPoint.y)));
    }

    // Send the event to the current tool, if appropriate.

    if (sentClick)
      activeTool.mouseDragged(e, this);
  }


  /**
   * mouseReleased
   *
   * Description: handler for mouse released event.
   */
  protected void mouseReleased(WidgetMouseEvent e) {
    if (1 == 1) { // run threaded version.
      mouseReleasedMT(e);
      return;
    }

    Rectangle r, b;
    int j, sel[] = theScene.getSelection();
    ObjectInfo info;

    moveToGrid(e);

    // Send the event to the current tool, if appropriate.

    if (sentClick) {
      if (!dragging) {
        Point p = e.getPoint();
        e.translatePoint(clickPoint.x - p.x, clickPoint.y - p.y);
      }
      activeTool.mouseReleased(e, this);
    }

    // If the user was dragging a selection box, then select anything it intersects.

    int oldSelection[] = theScene.getSelection();
    if (draggingSelectionBox) {
      dragPoint = e.getPoint();
      r = new Rectangle(Math.min(clickPoint.x, dragPoint.x), Math.min(clickPoint.y, dragPoint.y),
          Math.abs(dragPoint.x - clickPoint.x), Math.abs(dragPoint.y - clickPoint.y));
      if (!e.isShiftDown()) {
        if (parentFrame instanceof LayoutWindow)
          ((LayoutWindow) parentFrame).clearSelection();
        else
          theScene.clearSelection();
        parentFrame.updateMenus();
      }

      //
      // Split scene objects into groups for processing in multiple threads.
      //
      for (int i = 0; i < theScene.getNumObjects(); i++) {
        info = theScene.getObject(i);
        if (info.isVisible() && !info.isLocked() && !info.isChildrenHiddenWhenHidden()) {
          if (i % 25 == 0) {
            double progress = ((double) i / (double) theScene.getNumObjects()) * (double) 100;
            System.out.println("% " + progress);
          }
          theCamera.setObjectTransform(info.getCoords().fromLocal());
          b = theCamera.findScreenBounds(info.getBounds());
          expandBounds(b);
          if (b != null && b.x < r.x + r.width && b.y < r.y + r.height && r.x < b.x + b.width
              && r.y < b.y + b.height) {
            if (!e.isShiftDown()) {
              if (parentFrame instanceof LayoutWindow)
                ((LayoutWindow) parentFrame).addToSelection(i); // addToSelectionFast
              else
                theScene.addToSelection(i);
              parentFrame.updateMenus();
            } else {
              for (j = 0; j < sel.length && sel[j] != i; j++);
              if (j == sel.length) {
                if (parentFrame instanceof LayoutWindow)
                  ((LayoutWindow) parentFrame).addToSelection(i); // addToSelectionFast
                else
                  theScene.addToSelection(i);
                parentFrame.updateMenus();
              }
            }
          }
        }
      } // end
      // jdt reload menus
      parentFrame.updateMenus();
      if (parentFrame instanceof LayoutWindow) {
        ((LayoutWindow) parentFrame).refreshItemTreeSelection();
      }

      if (currentTool.hilightSelection())
        parentFrame.updateImage();
    } // draggingSelectionBox
    draggingBox = draggingSelectionBox = false;

    // If the user shift-clicked a selected object and released the mouse without dragging,
    // then deselect the point.

    if (deselect > -1) {
      if (parentFrame instanceof LayoutWindow)
        ((LayoutWindow) parentFrame).removeFromSelection(deselect);
      else
        theScene.removeFromSelection(deselect);
      parentFrame.updateMenus();
      parentFrame.updateImage();
    }

    // If the selection changed, set an undo record.

    int newSelection[] = theScene.getSelection();
    boolean changed = (oldSelection.length != newSelection.length);
    for (int i = 0; i < newSelection.length && !changed; i++)
      changed = (oldSelection[i] != newSelection[i]);
    if (changed)
      parentFrame.setUndoRecord(new UndoRecord(parentFrame, false, UndoRecord.SET_SCENE_SELECTION,
          new Object[] {oldSelection}));
  }

  /**
   *
   * TODO: Progress bar on large selections and multithred process.
   */
  protected void mouseReleasedMT(WidgetMouseEvent e) {
    Point p = e.getPoint();
    Vec3 clickVecA = theCamera.convertScreenToWorld(p, 0, false);
    Vec3 clickVecB = theCamera.convertScreenToWorld(p, 1, false);
    SceneViewer sceneViewer = this;
    Thread thread = new Thread() {
      public void run() {

        long startTime = System.currentTimeMillis();

        Rectangle r, b;
        int j, sel[] = theScene.getSelection();
        ObjectInfo info;

        moveToGrid(e);

        // Send the event to the current tool, if appropriate.

        if (sentClick) {
          if (!dragging) {
            Point p_ = e.getPoint();
            e.translatePoint(clickPoint.x - p_.x, clickPoint.y - p_.y);
          }
          activeTool.mouseReleased(e, sceneViewer);
        }

        // If the user was dragging a selection box, then select anything it intersects.

        int oldSelection[] = theScene.getSelection();
        if (draggingSelectionBox) {
          dragPoint = e.getPoint();

          int dragRegionX = dragPoint.x - clickPoint.x;
          int dragRegionY = dragPoint.y - clickPoint.y;
          // System.out.println(" dragRegionX " + dragRegionX + " dragRegionY " + dragRegionY);
          if (dragRegionX == 0 || dragRegionY == 0) {
            // return; // abort if drag select region is zero.
          }

          r = new Rectangle(Math.min(clickPoint.x, dragPoint.x),
              Math.min(clickPoint.y, dragPoint.y), Math.abs(dragPoint.x - clickPoint.x),
              Math.abs(dragPoint.y - clickPoint.y));
          if (!e.isShiftDown()) {
            if (parentFrame instanceof LayoutWindow)
              ((LayoutWindow) parentFrame).clearSelection();
            else
              theScene.clearSelection();
            parentFrame.updateMenus();
          }

          ProgressBar m = new ProgressBar();

          // System.out.println(" theScene.getNumObjects()" + theScene.getNumObjects());
          int cores = Runtime.getRuntime().availableProcessors();
          // System.out.println("Cores: " + cores);

          //
          // Split scene objects into groups for processing in multiple threads.
          //

          for (int i = 0; i < theScene.getNumObjects(); i++) {
            info = theScene.getObject(i);
            if (info.isVisible() && !info.isLocked() && !info.isChildrenHiddenWhenHidden()
                && (dragRegionX != 0 && dragRegionY != 0)) {
              if (i % 10 == 0) {
                double progress = ((double) i / (double) theScene.getNumObjects()) * (double) 100;
                // System.out.println("% " + progress);
                m.setProgress((int) progress);

                long currTime = System.currentTimeMillis();
                if (currTime > startTime + 1000 && m.isVisible() == false) { // if selection is
                                                                             // taking time, display
                                                                             // progress bar.
                  m.setVisible(true);
                }
              }
              theCamera.setObjectTransform(info.getCoords().fromLocal());
              b = theCamera.findScreenBounds(info.getBounds());
              expandBounds(b);
              if (b != null && b.x < r.x + r.width && b.y < r.y + r.height && r.x < b.x + b.width
                  && r.y < b.y + b.height) {
                // boolean clickedOnObject = true;
                // if(clickedOnObject){
                if (!e.isShiftDown()) {
                  if (parentFrame instanceof LayoutWindow) {
                    ((LayoutWindow) parentFrame).addToSelectionFast(i);
                  } else {
                    theScene.addToSelection(i);
                  }
                  parentFrame.updateMenus();
                } else {
                  for (j = 0; j < sel.length && sel[j] != i; j++);
                  if (j == sel.length) {
                    if (parentFrame instanceof LayoutWindow)
                      ((LayoutWindow) parentFrame).addToSelectionFast(i);
                    else
                      theScene.addToSelection(i);
                    parentFrame.updateMenus();
                  }
                }
                // } // clickedOnObject
              } // end bound if
            } // end visible locked, child hidden.
          } // end loop scene objects

          // jdt reload menus
          parentFrame.updateMenus();
          if (parentFrame instanceof LayoutWindow) {
            ((LayoutWindow) parentFrame).refreshItemTreeSelection();
          }

          m.setVisible(false); // Hide progress bar.

          if (currentTool.hilightSelection())
            parentFrame.updateImage();
        } // draggingSelectionBox
        draggingBox = draggingSelectionBox = false;

        // If the user shift-clicked a selected object and released the mouse without dragging,
        // then deselect the point.

        if (deselect > -1) {
          if (parentFrame instanceof LayoutWindow)
            ((LayoutWindow) parentFrame).removeFromSelection(deselect);
          else
            theScene.removeFromSelection(deselect);
          parentFrame.updateMenus();
          parentFrame.updateImage();
        }

        // If the selection changed, set an undo record.

        int newSelection[] = theScene.getSelection();
        boolean changed = (oldSelection.length != newSelection.length);
        for (int i = 0; i < newSelection.length && !changed; i++)
          changed = (oldSelection[i] != newSelection[i]);
        if (changed)
          parentFrame.setUndoRecord(new UndoRecord(parentFrame, false,
              UndoRecord.SET_SCENE_SELECTION, new Object[] {oldSelection}));

        // Update Material Selection Indicator
        boolean valid = true;
        int selectedMaterial = -1;
        for (int i = 0; i < newSelection.length; i++) {
          ObjectInfo currInfo = theScene.getObject(newSelection[i]);
          int matId = currInfo.getPhysicalMaterialId();
          if (selectedMaterial == -1) {
            selectedMaterial = matId;
          } else if (selectedMaterial != matId) {
            valid = false;
          }
        }
        if (valid == false) {
          selectedMaterial = -1;
        }
        if (parentFrame instanceof LayoutWindow) {
          LayoutWindow window = (LayoutWindow) parentFrame;
          window.setMaterialSelectionIndicator(selectedMaterial);
        }

        // Duration timer.
        long duration = System.currentTimeMillis() - startTime;
        if (duration > 5) {
          System.out.println("Selection time: " + duration);
        }
      }
    };
    thread.start();
  }


  /** Double-clicking on object should bring up its editor. */

  public void mouseClicked(MouseClickedEvent e) {
    // System.out.println("mouseClicked ");
    if (e.getClickCount() == 2 && (activeTool.whichClicks() & EditingTool.OBJECT_CLICKS) != 0
        && clickedObject != null && clickedObject.getObject().isEditable()) {
      final Object3D obj = clickedObject.getObject();
      parentFrame.setUndoRecord(new UndoRecord(parentFrame, false, UndoRecord.COPY_OBJECT,
          new Object[] {obj, obj.duplicate()}));
      obj.edit(parentFrame, clickedObject, new Runnable() {
        public void run() {
          theScene.objectModified(obj);
          parentFrame.updateImage();
          parentFrame.updateMenus();
        }
      });
    }
  }

  /**
   * processMouseScrolled
   *
   * Description: Mouse Scroll wheel event handler.
   */
  protected void processMouseScrolled(MouseScrolledEvent ev) {
    // if (isPerspective() && boundCamera != null)
    if (boundCamera != null) {
      // We are moving an actual camera in the scene, so we need to set an undo record, move
      // its children, and repaint all views in the window.

      UndoRecord undo = new UndoRecord(getEditingWindow(), false);
      super.processMouseScrolled(ev);
      moveChildren(boundCamera,
          theCamera.getCameraCoordinates().fromLocal().times(boundCamera.getCoords().toLocal()),
          undo);
      getEditingWindow().setUndoRecord(undo);
      getEditingWindow().updateImage();
    } else {
      super.processMouseScrolled(ev);
    }
  }

  /** This is called recursively to move any children of a bound camera. */

  private void moveChildren(ObjectInfo obj, Mat4 transform, UndoRecord undo) {
    CoordinateSystem coords = obj.getCoords();
    CoordinateSystem oldCoords = coords.duplicate();
    coords.transformCoordinates(transform);
    undo.addCommand(UndoRecord.COPY_COORDS, new Object[] {coords, oldCoords});
    for (int i = 0; i < obj.getChildren().length; i++)
      moveChildren(obj.getChildren()[i], transform, undo);
  }


  public void toggleBackgroundGradient() {
    CanvasDrawer d = getCanvasDrawer();
    d.toggleBackgroundGradient();
  }


  public class ProgressBar extends JFrame {
    JProgressBar jb;
    int i = 0, num = 0;

    ProgressBar() {
      super("Progress");
      jb = new JProgressBar(0, 100);
      jb.setBounds(40, 20, 170, 30);
      jb.setValue(0);
      jb.setStringPainted(true);
      add(jb);
      setSize(250, 100);
      setLayout(null);

      Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
      this.setLocation(dim.width / 2 - this.getSize().width / 2,
          dim.height / 2 - this.getSize().height / 2);

      // jb.setStringPainted(true);
    }

    public void setProgress(int p) {
      jb.setValue(p);
    }

  }


  // Expand bounds region to make it easier to select small objects
  public void expandBounds(Rectangle bounds) {
    int expandBy = 3;
    if (bounds != null) {
      // System.out.println(" x " + bounds.x + " y "+ bounds.y );
      bounds.x = bounds.x - expandBy;
      bounds.y = bounds.y - expandBy;
      bounds.width = bounds.width + (expandBy * 2);
      bounds.height = bounds.height + (expandBy * 2);
    }
  }

  public void drawContouredHighlight(boolean v) {
    drawContouredHighlight = v;
    repaint();
  }
}
