/* Copyright (C) 2021 by Jon Taylor
   Copyright (C) 1999-2008 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender;

import armarender.image.*;
import armarender.math.*;
import armarender.object.*;
import armarender.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;

/** The ObjectViewer class is the abstract superclass of components which display
   a single object and allow the user to edit it. */

public abstract class ObjectViewer extends ViewerCanvas
{
  protected MeshEditController controller;
  protected boolean showScene, useWorldCoords, freehandSelection, draggingBox, squareBox, sentClick;
  protected Point clickPoint, dragPoint;
  protected Vector<Point> selectBoundsPoints;
  protected Shape selectBounds;
  protected ObjectInfo thisObjectInScene;
  protected Scene theScene;

  public ObjectViewer(MeshEditController controller, RowContainer p)
  {
    super(ArmaRender.getPreferences().getUseOpenGL() && isOpenGLAvailable());
    this.controller = controller;
    buildChoices(p);
  }

  /** Get the controller which maintains the state for this viewer. */

  public MeshEditController getController()
  {
    return controller;
  }

  /** Estimate the range of depth values that the camera will need to render.  This need not be exact,
      but should err on the side of returning bounds that are slightly too large.
      @return the two element array {minDepth, maxDepth}
   */

  public double[] estimateDepthRange()
  {
    Mat4 toView = theCamera.getWorldToView();

    // Find the depth range for the object being edited.

    ObjectInfo info = getController().getObject();
    BoundingBox bounds = info.getBounds();
    double dx = bounds.maxx-bounds.minx;
    double dy = bounds.maxy-bounds.miny;
    double dz = bounds.maxz-bounds.minz;
    double size = 0.5*Math.sqrt(dx*dx+dy*dy+dz*dz);
    Vec3 origin = getDisplayCoordinates().fromLocal().times(bounds.getCenter());
    double depth = toView.times(origin).z;
    double min = depth-size, max = depth+size;

    // Now check the rest of the scene.

    if (showScene)
    {
      for (int i = 0; i < theScene.getNumObjects(); i++)
      {
        info = theScene.getObject(i);
        if (info == thisObjectInScene)
          continue;
        bounds = info.getBounds();
        dx = bounds.maxx-bounds.minx;
        dy = bounds.maxy-bounds.miny;
        dz = bounds.maxz-bounds.minz;
        size = 0.5*Math.sqrt(dx*dx+dy*dy+dz*dz);
        origin = info.getCoords().fromLocal().times(bounds.getCenter());
        if (!useWorldCoords)
          origin = thisObjectInScene.getCoords().toLocal().times(origin);
        depth = toView.times(origin).z;
        if (depth-size < min)
          min = depth-size;
        if (depth+size > max)
          max = depth+size;
      }
    }
    return new double [] {min, max};
  }

  @Override
  public void viewChanged(boolean selectionOnly)
  {
    super.viewChanged(selectionOnly);
    if (renderMode == RENDER_RENDERED && !selectionOnly)
    {
      // Re-render the image.

      Renderer rend = ArmaRender.getPreferences().getObjectPreviewRenderer();
      if (rend == null)
        return;
      adjustCamera(true);
      Scene sc;
      Camera cam = theCamera.duplicate();
      if (showScene && theScene != null)
      {
        sc = new Scene();
        sc.setEnvironmentColor(theScene.getEnvironmentColor());
        sc.setEnvironmentTexture(theScene.getEnvironmentTexture());
        sc.setEnvironmentMapping(theScene.getEnvironmentMapping());
        sc.setEnvironmentParameterValues(theScene.getEnvironmentParameterValues());
        sc.setEnvironmentMode(theScene.getEnvironmentMode());
        sc.setAmbientColor(theScene.getAmbientColor());
        Object3D thisObject = (thisObjectInScene == null ? null : thisObjectInScene.getObject());
        for (int i = 0; i < theScene.getNumObjects(); i++)
        {
          ObjectInfo obj = theScene.getObject(i);
          if (obj.getObject() == thisObject)
            sc.addObject(obj.duplicate(controller.getObject().getObject()), null);
          else
            sc.addObject(obj, null);
        }
        if (!useWorldCoords && thisObjectInScene != null)
        {
          CoordinateSystem newCoords = cam.getCameraCoordinates();
          newCoords.transformCoordinates(thisObjectInScene.getCoords().fromLocal());
          cam.setCameraCoordinates(newCoords);
        }
      }
      else
      {
        sc = new Scene();
        RGBColor background = new RGBColor();
        background.setARGB(backgroundColor.getRGB());
        sc.setEnvironmentColor(background);
        sc.addObject(new DirectionalLight(new RGBColor(1.0f, 1.0f, 1.0f), 0.8f), cam.getCameraCoordinates(), "", null);
        ObjectInfo obj = getController().getObject();
        obj = obj.duplicate(obj.getObject().duplicate());
        obj.getCoords().transformCoordinates(getDisplayCoordinates().fromLocal());
        sc.addObject(obj, null);
      }
      rend.configurePreview();
      Rectangle bounds = getBounds();
      SceneCamera sceneCamera = new SceneCamera();
      sceneCamera.setFieldOfView(Math.atan(0.5*bounds.height/(cam.getDistToScreen()*getScale()))*360.0/Math.PI);
      adjustCamera(isPerspective());
      RenderListener listener = new RenderListener()
      {
        public void imageUpdated(Image image)
        {
          renderedImage = image;
          getCanvasDrawer().imageChanged(renderedImage);
          repaint();
        }
        public void statusChanged(String status)
        {
        }
        public void imageComplete(ComplexImage image)
        {
          renderedImage = image.getImage();
          getCanvasDrawer().imageChanged(renderedImage);
          repaint();
        }
        public void renderingCanceled()
        {
        }
      };
      rend.renderScene(sc, cam, listener, sceneCamera);
    }
  }

  public synchronized void updateImage()
  {
    if (renderMode == RENDER_RENDERED)
    {
      if (renderedImage != null && renderedImage.getWidth(null) > 0)
        drawImage(renderedImage, 0, 0);
      else
        viewChanged(false);
      drawBorder();
      if (showAxes)
        drawCoordinateAxes();
      return;
    }
    super.updateImage();
    if (controller.getObject() == null)
      return;

    // Draw the rest of the objects in the scene.

    if (showScene && theScene != null)
    {
      Vec3 viewdir = getDisplayCoordinates().toLocal().timesDirection(theCamera.getViewToWorld().timesDirection(Vec3.vz()));
      for (int i = 0; i < theScene.getNumObjects(); i++)
      {
        ObjectInfo obj = theScene.getObject(i);
        if (!obj.isVisible() || obj == thisObjectInScene || obj.isHiddenByParent())
          continue;
        Mat4 objectTransform = obj.getCoords().fromLocal();
        if (!useWorldCoords && thisObjectInScene != null)
          objectTransform = thisObjectInScene.getCoords().toLocal().times(objectTransform);
        theCamera.setObjectTransform(objectTransform);
        obj.getObject().renderObject(obj, this, thisObjectInScene.getCoords().fromLocal().timesDirection(viewdir));
      }
    }

    // Draw the object being edited.

    theCamera.setObjectTransform(getDisplayCoordinates().fromLocal());
    drawObject();

    // Finish up.

    drawBorder();
    if (showAxes)
      drawCoordinateAxes();
  }

  protected abstract void drawObject();

  /** Get the coordinate system in which the object is displayed.  This will
      vary depending on whether the user has selected Local or Scene coordinates. */

  public CoordinateSystem getDisplayCoordinates()
  {
    if (useWorldCoords && thisObjectInScene != null)
      return thisObjectInScene.getCoords();
    else
      return controller.getObject().getCoords();
  }

  /** Get whether freehand selection mode is currently in use. */

  public boolean getFreehandSelection()
  {
    return freehandSelection;
  }

  /** Set whether to use freehand selection mode. */

  public void setFreehandSelection(boolean freehand)
  {
    freehandSelection = freehand;
  }

  /** Get the scene this object is part of, or null if there is none. */

  public Scene getScene()
  {
    return theScene;
  }

  /** Set the scene this object is part of. */

  public void setScene(Scene sc, ObjectInfo thisObject)
  {
    theScene = sc;
    thisObjectInScene = thisObject;
  }

  /** Get whether the entire scene is visible. */

  public boolean getSceneVisible()
  {
    return showScene;
  }

  /** Set whether the entire scene is visible. */

  public void setSceneVisible(boolean visible)
  {
    showScene = visible;
    viewChanged(false);
  }

  /** Get whether to use world coordinates. */

  public boolean getUseWorldCoords()
  {
    return useWorldCoords;
  }

  /** Set whether to use world coordinates. */

  public void setUseWorldCoords(boolean use)
  {
    useWorldCoords = use;
    viewChanged(false);
  }

  /** Begin dragging a selection region.  The variable square determines whether
      the region should be constrained to be square. */

  public void beginDraggingSelection(Point p, boolean square)
  {
    draggingBox = true;
    clickPoint = p;
    squareBox = square;
    dragPoint = null;
    if (freehandSelection)
      selectBoundsPoints = new Vector<Point>();
  }

  /** Finish dragging a selection region. */

  public void endDraggingSelection()
  {
    if (!draggingBox || dragPoint == null)
    {
      selectBounds = null;
      return;
    }
    repaint();

    // Construct the selection region.

    if (freehandSelection)
      selectBounds = createPolygonFromSelection();
    else
      selectBounds = new Rectangle(Math.min(clickPoint.x, dragPoint.x), Math.min(clickPoint.y, dragPoint.y),
		Math.abs(dragPoint.x-clickPoint.x), Math.abs(dragPoint.y-clickPoint.y));
  }

  /** Create a Polygon from the selection bounds. */

  private Polygon createPolygonFromSelection()
  {
    int n = selectBoundsPoints.size(), x[] = new int [n], y[] = new int [n];
    for (int i = 0; i < n; i++)
    {
      Point p = selectBoundsPoints.elementAt(i);
      x[i] = p.x;
      y[i] = p.y;
    }
    return new Polygon(x, y, n);
  }

  /** Determine whether the selection region contains the specified point. */

  public boolean selectionRegionContains(Point p)
  {
    if (selectBounds instanceof Rectangle)
      return ((Rectangle) selectBounds).contains(p);
    if (selectBounds instanceof Polygon)
      return ((Polygon) selectBounds).contains(p);
    return false;
  }

  /**
   * Determine whether the selection region intersects the segment specified by the 2 points.
   * <p> The selection intersects the line if either:<ul>
   * <li> one of the points is inside the selection;
   * <li> both points are outside and the line formed by them intersects
   *      any of the boundaries of the selection.
   * </ul>
   */

  public boolean selectionRegionIntersects(Point p1, Point p2)
  {
    if (selectionRegionContains(p1) || selectionRegionContains(p2))
      return true;
    if (selectBounds instanceof Rectangle)
      return ((Rectangle) selectBounds).intersectsLine(p1.x, p1.y, p2.x, p2.y);
    if (selectBounds instanceof Polygon)
    {
      // if any of the edges of the polygon intersect, return true
      final Polygon polygon = ((Polygon) selectBounds);
      final int[] xpoints = polygon.xpoints;
      final int[] ypoints = polygon.ypoints;
      final int npoints = polygon.npoints;
      int lastx = xpoints[npoints - 1];
      int lasty = ypoints[npoints - 1];
      int curx, cury;

      for (int i = 0; i < npoints; i++)
      {
        curx = xpoints[i];
        cury = ypoints[i];
        final boolean intersect =
          Line2D.linesIntersect(
            lastx, lasty, curx, cury, // polygon selection line
            p1.x, p1.y, p2.x, p2.y    // tested line
          );
        if (intersect) return true;
        lastx = curx;
        lasty = cury;
      }
      return false;
    }
    return false;
  }

  protected void mouseDragged(WidgetMouseEvent e)
  {
    moveToGrid(e);
    if (draggingBox && freehandSelection)
    {
      // Add this point to the region boundary and draw a line.

      dragPoint = e.getPoint();
      selectBoundsPoints.addElement(dragPoint);
      drawDraggedShape(createPolygonFromSelection());
    }
    else if (draggingBox)
    {
      // We are dragging a box, so erase and redraw it.

      if (dragPoint != null)
        drawDraggedShape(new Rectangle(Math.min(clickPoint.x, dragPoint.x), Math.min(clickPoint.y, dragPoint.y),
              Math.abs(dragPoint.x-clickPoint.x), Math.abs(dragPoint.y-clickPoint.y)));
      dragPoint = e.getPoint();
      if (squareBox)
      {
        if (Math.abs(dragPoint.x-clickPoint.x) > Math.abs(dragPoint.y-clickPoint.y))
        {
          if (dragPoint.y < clickPoint.y)
            dragPoint.y = clickPoint.y - Math.abs(dragPoint.x-clickPoint.x);
          else
            dragPoint.y = clickPoint.y + Math.abs(dragPoint.x-clickPoint.x);
        }
        else
        {
          if (dragPoint.x < clickPoint.x)
            dragPoint.x = clickPoint.x - Math.abs(dragPoint.y-clickPoint.y);
          else
            dragPoint.x = clickPoint.x + Math.abs(dragPoint.y-clickPoint.y);
        }
      }
      drawDraggedShape(new Rectangle(Math.min(clickPoint.x, dragPoint.x), Math.min(clickPoint.y, dragPoint.y),
              Math.abs(dragPoint.x-clickPoint.x), Math.abs(dragPoint.y-clickPoint.y)));
    }

    // Send the event to the current tool, if appropriate.

    if (sentClick)
      activeTool.mouseDragged(e, this);
  }

  public void previewObject()
  {
	  //System.out.println("ObjectViewer.previewObject() ");

    Renderer rend = ArmaRender.getPreferences().getObjectPreviewRenderer();
    if (rend == null)
      return;
    adjustCamera(true);
    Scene sc;
    CoordinateSystem cameraCoords = theCamera.getCameraCoordinates();
    if (showScene)
    {
      sc = theScene;
      if (!useWorldCoords && thisObjectInScene != null)
      {
        CoordinateSystem newCoords = cameraCoords.duplicate();
        newCoords.transformCoordinates(thisObjectInScene.getCoords().fromLocal());
        theCamera.setCameraCoordinates(newCoords);
      }
    }
    else
    {
      sc = new Scene();
      sc.addObject(new DirectionalLight(new RGBColor(1.0f, 1.0f, 1.0f), 0.8f), theCamera.getCameraCoordinates(), "", null);
      ObjectInfo obj = getController().getObject();
      obj = obj.duplicate(obj.getObject().duplicate());
      obj.getCoords().transformCoordinates(getDisplayCoordinates().fromLocal());
      sc.addObject(obj, null);
    }
    rend.configurePreview();
    SceneCamera sceneCamera = new SceneCamera();
    sceneCamera.setFieldOfView(Math.atan(0.5*getBounds().height/(theCamera.getDistToScreen()*getScale()))*360.0/Math.PI);
    ObjectInfo cameraInfo = new ObjectInfo(sceneCamera, theCamera.getCameraCoordinates(), "");
    new RenderingDialog(UIUtilities.findFrame(this), rend, sc, theCamera, cameraInfo);
    theCamera.setCameraCoordinates(cameraCoords);
    adjustCamera(isPerspective());
  }
}
