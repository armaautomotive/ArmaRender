/* Copyright (C) 2021-2023 by Jon Taylor
   Copyright (C) 1999-2008 by Peter Eastman
 
   Curve and ArcObject Editor.

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender;

import armarender.math.*;
import armarender.object.*;
import armarender.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.util.Vector;
import java.awt.*;
import javax.swing.*;

/** The CurveEditorWindow class represents the window for editing Curve objects. */

public class CurveEditorWindow extends MeshEditorWindow implements EditingWindow
{
  protected BMenu editMenu, meshMenu, smoothMenu;
  protected BMenuItem editMenuItem[], meshMenuItem[];
  protected BCheckBoxMenuItem smoothItem[];
  protected Runnable onClose;
  private int selectionDistance[], maxDistance;
  private boolean topology;
  boolean selected[];
  private Scene scene;
  private ObjectInfo info; // used to access child objects
  private static LayoutWindow window;
    
  private PairDistanceAlign pairDistanceAlignDialog;
  JDialog infoGraphicDialog = null;

  public CurveEditorWindow(EditingWindow parent, String title, ObjectInfo obj, Runnable onClose, boolean allowTopology)
  {
    super(parent, title, obj);
    this.info = obj;
    this.onClose = onClose;
    topology = allowTopology;
    FormContainer content = new FormContainer(new double [] {0, 1}, new double [] {1, 0, 0});
      content.setBackground(new Color(220, 220, 220)); // no effect
    setContent(content);
    content.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));
    content.add(helpText = new BLabel(), 0, 1, 2, 1);
      helpText.setBackground(new Color(220, 220, 220)); // no effect
    content.add(viewsContainer, 1, 0);
    RowContainer buttons = new RowContainer();
      buttons.setBackground(new Color(220, 220, 220)); // no effect
    buttons.add(Translate.button("ok", this, "doOk"));
    buttons.add(Translate.button("cancel", this, "doCancel"));
    content.add(buttons, 0, 2, 2, 1, new LayoutInfo());
    content.add(tools = new ToolPalette(1, 7), 0, 0, new LayoutInfo(LayoutInfo.NORTH, LayoutInfo.NONE, null, null));
    tools.setBackground(new Color(220, 220, 220)); // no effect
    EditingTool metaTool, altTool, compoundTool, snapToTool, tabSlotTool;
    tools.addTool(defaultTool = new ReshapeMeshTool(this, this));
    tools.addTool(new ScaleMeshTool(this, this));
    tools.addTool(new RotateMeshTool(this, this, false));
    tools.addTool(new SkewMeshTool(this, this));
    tools.addTool(new TaperMeshTool(this, this));
    tools.addTool(compoundTool = new MoveScaleRotateMeshTool(this, this));
    if (ArmaRender.getPreferences().getUseCompoundMeshTool())
      defaultTool = compoundTool;
    tools.addTool(metaTool = new MoveViewTool(this));
    tools.addTool(altTool = new RotateViewTool(this));
    tools.addTool(snapToTool = new SnapToTool(this, this, window, obj));
    //tools.addTool(tabSlotTool = new TabSlotTool(this, this, window, obj));
      //System.out.println("Curve editor window");
      
    tools.setDefaultTool(defaultTool);
    tools.selectTool(defaultTool);
    for (int i = 0; i < theView.length; i++)
    {
      MeshViewer view = (MeshViewer) theView[i];
        view.setBackground(new Color(220, 220, 220)); // no effect
      view.setMetaTool(metaTool);
      view.setAltTool(altTool);
      view.setMeshVisible(true);
      view.setScene(parent.getScene(), obj);
    }
    createEditMenu();
      
      this.scene = parent.getScene();
      this.window = (LayoutWindow)parent;
      
      // Tem
      // ObjectInfo
      if(obj.getObject() instanceof DimensionObject){
          
          //Curve converted = new Curve(obj.getObject().getVertexPositions(), obj.getObject().getSmoothness(), true);
          //converted.setVertexPositions(obj.getVertexPositions());
          //converted.setSmoothingMethod(1);
          //obj = converted;
      }
      
    //if(obj.getObject() instanceof SupportCurve ){
    //      createMeshMenu((SupportCurve) obj.getObject());
    
          createMeshMenu((Curve) obj.getObject());
    
    createViewMenu();
    recursivelyAddListeners(this);
    UIUtilities.applyDefaultFont(content);
    //UIUtilities.applyDefaultBackground(content); //
    Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    Dimension windowDim = new Dimension((screenBounds.width), (screenBounds.height));
    setBounds(new Rectangle((screenBounds.width-windowDim.width), (screenBounds.height-windowDim.height), windowDim.width, windowDim.height));
    
      tools.requestFocus();
    selected = new boolean [((Curve) obj.getObject()).getVertices().length];
    findSelectionDistance();
    updateMenus();
      
      javax.swing.JFrame jf = this.getComponent();
      
      ImageIcon iconImage = new ImageIcon(getClass().getResource("/armarender/Icons/favicon-32x32.png"));
      jf.setIconImage(iconImage.getImage());
      
      // Expand window
      //Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      //Dimension screenSize = new Dimension( ,  );
      //jf.setSize (screenSize);
      //jf.setLocation(0,0);
      
      //Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
      //jf.setBounds(screenBounds);
      
      setBackground(new Color(220, 220, 220)); // no effect
  }

  /** This constructor is here to let TubeEditorWindow subclass CurveEditorWindow. */

  protected CurveEditorWindow(EditingWindow parent, String title, ObjectInfo obj)
  {
    super(parent, title, obj);
  }

    /**
     * createEditMenu
     *
     * Description: Create edit menu.
     */
  void createEditMenu()
  {
    editMenu = Translate.menu("edit");
    menubar.add(editMenu);
    editMenuItem = new BMenuItem [3];
    editMenu.add(undoItem = Translate.menuItem("undo", this, "undoCommand"));
    editMenu.add(redoItem = Translate.menuItem("redo", this, "redoCommand"));
    editMenu.add(Translate.menuItem("selectAll", this, "selectAllCommand"));
    editMenu.add(editMenuItem[0] = Translate.menuItem("extendSelection", this, "extendSelectionCommand"));
    editMenu.add(editMenuItem[1] = Translate.menuItem("invertSelection", this, "invertSelectionCommand"));
    editMenu.add(editMenuItem[2] = Translate.checkboxMenuItem("freehandSelection", this, "freehandModeChanged", false));
    editMenu.addSeparator();
    editMenu.add(Translate.menuItem("curveTension", this, "setTensionCommand"));
      
      //editMenu.addSeparator();
      //editMenu.add(Translate.menuItem("numericEditObject", this, "numericEditObjectCommand"));
  }

  void createMeshMenu(Curve obj)
  {
    meshMenu = Translate.menu("curve");
    menubar.add(meshMenu);
    meshMenuItem = new BMenuItem [7];
    meshMenuItem[0] = Translate.menuItem("deletePoints", this, "deleteCommand");
    if (topology)
      meshMenu.add(meshMenuItem[0]);
    meshMenuItem[1] = Translate.menuItem("subdivide", this, "subdivideCommand");
    if (topology)
      meshMenu.add(meshMenuItem[1]);
    meshMenu.add(meshMenuItem[2] = Translate.menuItem("editPoints", this, "setPointsCommand"));
    meshMenu.add(meshMenuItem[3] = Translate.menuItem("transformPoints", this, "transformPointsCommand"));
    meshMenu.add(meshMenuItem[4] = Translate.menuItem("randomize", this, "randomizeCommand"));
    meshMenu.add(Translate.menuItem("centerCurve", this, "centerCommand"));
      
      meshMenu.add(Translate.menuItem("Shift Curve Points", this, "shiftCurvePointsCommand"));
      // Option to shift the other direction?
      
      // Shift to start at Selection
      BMenuItem meshShiftToSelectionMenuItem = Translate.menuItem("Shift to Selection", this, "shiftToSelectionCommand");
      meshShiftToSelectionMenuItem.setName("mesh_shiftToSelection");   // Name is used by event handler for ID
      meshShiftToSelectionMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      meshShiftToSelectionMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      meshMenu.add(meshShiftToSelectionMenuItem);
      
      
      //meshMenu.add( Translate.menuItem("Split Selection", this, "splitSelectionCommand") );
      BMenuItem meshSplitSelectionMenuItem = Translate.menuItem("Split Selection", this, "splitSelectionCommand");
      meshSplitSelectionMenuItem.setName("mesh_splitSelection");   // Name is used by event handler for ID
      meshSplitSelectionMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      meshSplitSelectionMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      meshMenu.add(meshSplitSelectionMenuItem);
      
      // Equalize selection
      BMenuItem meshEqualizeSelectionMenuItem = Translate.menuItem("Equalize Selection", this, "equalizeSelectionCommand");
      meshEqualizeSelectionMenuItem.setName("mesh_equalizeSelection");   // Name is used by event handler for ID
      meshEqualizeSelectionMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      meshEqualizeSelectionMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      meshMenu.add(meshEqualizeSelectionMenuItem);
      
      // Conform curve to mesh
      BMenuItem conformCurveToMeshMenuItem = Translate.menuItem("Conform to Mesh", this, "conformCurveToMeshCommand");
      conformCurveToMeshMenuItem.setName("tools_conformCurveToMesh");   // Name is used by event handler for ID
      conformCurveToMeshMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      conformCurveToMeshMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      meshMenu.add(conformCurveToMeshMenuItem);
      
      
    meshMenu.addSeparator();
    meshMenu.add(meshMenuItem[5] = Translate.menuItem("smoothness", this, "setSmoothnessCommand"));
    meshMenu.add(smoothMenu = Translate.menu("smoothingMethod"));
    smoothItem = new BCheckBoxMenuItem [3];
    smoothMenu.add(smoothItem[0] = Translate.checkboxMenuItem("none", this, "smoothingChanged", obj.getSmoothingMethod() == Curve.NO_SMOOTHING));
    smoothMenu.add(smoothItem[1] = Translate.checkboxMenuItem("interpolating", this, "smoothingChanged", obj.getSmoothingMethod() == Curve.INTERPOLATING));
    smoothMenu.add(smoothItem[2] = Translate.checkboxMenuItem("approximating", this, "smoothingChanged", obj.getSmoothingMethod() == Curve.APPROXIMATING));
    meshMenu.add(meshMenuItem[6] = Translate.menuItem("closedEnds", this, "toggleClosedCommand"));
    if (obj.isClosed())
      meshMenuItem[6].setText(Translate.text("menu.openEnds"));
  }

  protected BMenu createShowMenu()
  {
    BMenu menu = Translate.menu("show");
    showItem = new BCheckBoxMenuItem [4];
    menu.add(showItem[0] = Translate.checkboxMenuItem("curve", this, "shownItemChanged", true));
    menu.add(showItem[3] = Translate.checkboxMenuItem("entireScene", this, "shownItemChanged", ((MeshViewer) theView[currentView]).getSceneVisible()));
    return menu;
  }
  
  /** Get the object being edited in this window. */
  
  public ObjectInfo getObject()
  {
    return objInfo;
  }
  
  /** Set the object being edited in this window. */
  
  public void setObject(Object3D obj)
  {
    objInfo.setObject(obj);
    objInfo.clearCachedMeshes();
      
  }
  
        
    /**
     * setMesh
     */
  public void setMesh(Mesh mesh)
  {
      //System.out.println(" setMesh ");
    Curve obj = (Curve) mesh;
      
      //int smooth = obj.getSmoothingMethod();
      //System.out.println("  setMesh smooth  " + smooth );
      
    setObject(obj); // Issue on curve with mirrow setting smoothing to 0;
      
    if (selected.length != obj.getVertices().length)
      selected = new boolean [obj.getVertices().length];
      
    findSelectionDistance();
    currentTool.getWindow().updateMenus();
  }
  
  /** Get an array of flags telling which vertices are currently selected. */
  
  public boolean[] getSelection()
  {
    return selected;
  }
    
    
    /**
     * keyPressed
     *  Description: Handler to trigger mirror plane update on changes to geometry
     */
    protected void keyPressed(KeyPressedEvent e){
        //System.out.println("CurveEditorWindow.keyPressed.");
        
        // Do the regular key handler routines.
        super.keyPressed(e);
        
        boolean selected[] = getSelection();
        int mirrorIndex = getMirrorIndex(info); //  selected
        conformMirror(mirrorIndex);
        
        // Is this curve a child of a curve/arc that is mirrored and needs to be replicated across the mirror plane?
        mirrorNotchAcrossParent();
        
        // Update arc notch markers
        //window.notchIntersections.updateReferenceMarker(info);
    }
  
    /**
     * setSelection
     *
     * Description:
     */
  public void setSelection(boolean sel[])
  {
    selected = sel;
    findSelectionDistance();
    updateMenus();
    updateImage();
      
      //System.out.println("CurveEditorWindow.setSelection() " + selected.length);
      
      // Mirror plane
      int mirrorIndex = getMirrorIndex(info); // sel
      conformMirror(mirrorIndex);
      
      mirrorNotchAcrossParent();
      
      // Update bend markers if present
      //window.notchIntersections.updateReferenceMarker(info);
      
      // If Mirror plane and reflected point selected, change the selection to the reference points.
      if(mirrorIndex != -1){
          for(int i = 0; i < selected.length; i++){
              boolean pointSel = selected[i];
              if(i > mirrorIndex && pointSel == true){
                  selected[i] = false; // deselect, you can't modify this side of the object
                  int refIndex =  mirrorIndex - (i - mirrorIndex);
                  if(refIndex >= 0 && refIndex < selected.length){ // Bounds check
                      selected[refIndex] = true;    // select the reference side.
                  }
              }
          }
      }
      
      Curve theCurve = (Curve) getObject().getObject();
      
      Vec3 va = null;
      Vec3 vb = null;
      int aindex = -1;
      int bindex = -1;
      int vertSelCount = 0;
      for(int i = 0; i < selected.length; i++){
          boolean pointSel = selected[i];
          //System.out.println(" p " + i + " - " + pointSel );
          if(pointSel){
              if(vertSelCount == 0){
                  
                  MeshVertex v[] = ((Mesh) theCurve).getVertices();
                  va = v[i].r;
                  aindex = i;
                  
                  //va = theCurve.getVertices()[i];
              } else if(vertSelCount == 1) {
                  
                  MeshVertex v[] = ((Mesh) theCurve).getVertices();
                  vb = v[i].r;
                  bindex = i;
                  
                  //vb = theCurve.getVertices()[i];
              }
              vertSelCount++;
          }
      }
      /*
      System.out.println("*** count: " + vertSelCount );
      if(vertSelCount == 2){
          if(pairDistanceAlignDialog == null){
              
              pairDistanceAlignDialog = new PairDistanceAlign(   this, va, vb);
              
              theCurve.setRenderPointIndexHighlight( aindex );
              System.out.println(" setRenderPointIndexHighlight aindex " + aindex);
              //window.updateImage();
              updateImage();
          }
      } else if(pairDistanceAlignDialog != null) {
          theCurve.setRenderPointIndexHighlight(-1);
          pairDistanceAlignDialog.deselectPoints();
          pairDistanceAlignDialog.setVisible(false);
          pairDistanceAlignDialog.dispose();
          pairDistanceAlignDialog = null;
      }
      */
      
      // If a selection of points exists, find centre point and pass to all ViewerCanvas objects.
      Vec3 selectionCentrePoint = new Vec3();
      Curve curve = (Curve)objInfo.getObject();
      MeshVertex v[] = ((Mesh) curve).getVertices();
      Vector<Vec3> selectedPoints = new Vector<Vec3>();
      for(int i = 0; i < selected.length; i++){
          boolean pointSel = selected[i];
          if( pointSel == true){
              Vec3 vec = v[i].r;
              selectedPoints.addElement(vec);
          }
      }
      if(selectedPoints.size() > 0){
          for(int i = 0; i < selectedPoints.size(); i++){
              selectionCentrePoint.add(selectedPoints.elementAt(i));
          }
          selectionCentrePoint.divideBy(selectedPoints.size());
          for (int i = 0; i < theView.length; i++){
              theView[i].setViewTargetPoint(selectionCentrePoint);
          }
      }
  }
    
    
    /**
     * getMirrorIndex
     *
     * Description: if there is a mirror plane object child, get the location information in order to copy geometry across plane.
     *
     * @param sel[]: Selection array. ??? is this parameter needed?
     * @return int: Index in editing object verts.
     */
    public int getMirrorIndex(ObjectInfo info){ // boolean sel[]
        //System.out.println("CurveEditorWindow.getMirrorIndex()");
        int index = -1;
        // does the editing object contain a child object for a mirror plane.
        ObjectInfo[] children = info.getChildren();
        for(int i = 0; i < children.length; i++){
            ObjectInfo child = (ObjectInfo)children[i];
            CoordinateSystem childCS = child.getCoords();
            if(child.getObject() instanceof armarender.object.MirrorPlaneObject && child.isVisible()){
              // Get location and size, then see if it is close to a point on the arc/curve.
                Mesh mirrorMesh = (Mesh) child.getObject();
                Vec3 [] mirrorVerts = mirrorMesh.getVertexPositions();
                if(mirrorVerts.length > 1){
                    Vec3 vertA = new Vec3(mirrorVerts[0].x, mirrorVerts[0].y, mirrorVerts[0].z);
                    Vec3 vertB = new Vec3(mirrorVerts[1].x, mirrorVerts[1].y, mirrorVerts[1].z);
                    Mat4 childMat4 = childCS.duplicate().fromLocal();
                    childMat4.transform(vertA);
                    childMat4.transform(vertB);
                    double mirrorDistance = vertA.distance(vertB);
                    Vec3 mirrorCentre = vertA.midPoint(vertB);
                    
                    double minX = Math.min(vertA.x, vertB.x);
                    double maxX = Math.max(vertA.x, vertB.x);
                    
                    CoordinateSystem curveCS = info.getCoords();
                    Vec3 [] curveVerts = ((Mesh)info.getObject()).getVertexPositions();
                    for(int p = 0; p < curveVerts.length; p++){
                        Vec3 curveVec = (Vec3)curveVerts[p];
                        Mat4 curveMat4 = curveCS.duplicate().fromLocal();
                        curveMat4.transform(curveVec);
                        double dist = curveVec.distance(mirrorCentre);
                        if(dist < mirrorDistance && curveVec.x > minX && curveVec.x < maxX){
                            index = p;
                        }
                    }
                }
            }
        }
        //System.out.println("CurveEditorWindow.getMirrorIndex() end " );
        return index;
    }
    
    /**
     * conformMirror
     *
     *  Description: Modify geometry on mirrored side to conform to the reference points.
     *    This only mirrors on the X axis. Later on i will use the mirror plane normal to create the reflection transformation.
     *
     *  Why is this not in MirrorCurve class?
     *
     *  Bug: Curve looses smothing method.
     *
     *    @param: mirrorIndex: index into editing object verts which is the mirror point.
     */
    public void conformMirror(int mirrorIndex){
        if(mirrorIndex <= 0){
            return;
        }
        Vec3 mirrorPoint = null;
        Vec3 absoluteMirrorPoint = null;
        
        CoordinateSystem curveCS = info.getCoords();
        Vec3 [] curveVerts = ((Mesh)info.getObject()).getVertexPositions();
        
        float[] curveSmoothness = ((Curve)info.getObject()).getSmoothness();
            
        CoordinateSystem inverseCurveCS = info.getCoords().duplicate();
        Vec3 inverseZDir = inverseCurveCS.getZDirection();
        Vec3 inverseUpDir = inverseCurveCS.getUpDirection();
        inverseZDir.x = - inverseZDir.x;
        inverseZDir.y = - inverseZDir.y;
        inverseZDir.z = - inverseZDir.z;
        inverseUpDir.x = - inverseUpDir.x;
        inverseUpDir.y = - inverseUpDir.y;
        inverseUpDir.z = - inverseUpDir.z;
        inverseCurveCS.setOrientation(inverseZDir, inverseUpDir);
        
        //System.out.println("curveCS " + curveCS.getUpDirection() + " " + curveCS.getZDirection() );
        if(curveCS.getUpDirection().equals(new Vec3(0.0, 1.0, 0.0)) == false ||
           curveCS.getZDirection().equals(new Vec3(0.0, 0.0, 1.0)) == false)
        {
            System.out.println("Zero Arc CoordinateSystem.");
            
            // non zero CoordinateSystem on arc creates complex calculations.
            // One option is to zero out the arc Coordinate system translating the points keeping the same absolute location
            // but allowing mirroring with simple code.
            
            // Zero Arc CoordinateSystem.
            // Step 1) Set arc vert points to absolute values
            
            Vec3[] translatedPoints = new Vec3[curveVerts.length];
            for(int p = 0; p < curveVerts.length; p++){
                Vec3 curveVec = new Vec3((Vec3)curveVerts[p]);
                Mat4 curveMat4 = curveCS.duplicate().fromLocal();
                curveMat4.transform(curveVec);
                translatedPoints[p] = curveVec;
            }
            ((Mesh)info.getObject()).setVertexPositions(translatedPoints);
            // Step 2) Zero out CS.
            curveCS = new CoordinateSystem();
            info.setCoords(curveCS);
            info.clearCachedMeshes();
            
            curveVerts = ((Mesh)info.getObject()).getVertexPositions();
        
            if(window != null){
                window.updateImage();
                window.updateTree(); // window update tree
            }
        }
        
        //System.out.println(" conform  mirrorIndex " +  mirrorIndex + " curveVerts.length " + curveVerts.length);
        
        if(((mirrorIndex * 2)+1) > curveVerts.length){                  // Add point
            int addCount = ((mirrorIndex * 2)+1) - curveVerts.length;
            System.out.println("Add to end " +  addCount);
            Vec3 [] alteredCurveVerts = new Vec3[curveVerts.length + addCount];
            for(int i = 0; i < curveVerts.length; i++){
                alteredCurveVerts[i] = curveVerts[i];
            }
            for(int i = 0; i < addCount; i++){
                alteredCurveVerts[curveVerts.length + i] = new Vec3();
            }
            ((Mesh)info.getObject()).setVertexPositions(alteredCurveVerts);
            Object3D obj = (Object3D)info.getObject();
            ((Mesh)obj).setVertexPositions(alteredCurveVerts);
            ((Mesh)getObject().getObject()).setVertexPositions(alteredCurveVerts);
            
            if(obj instanceof ArcObject){
                ArcObject arc = (ArcObject)obj;
                if(arc.getSmoothness().length < alteredCurveVerts.length){
                    float smoothness [] = new float[alteredCurveVerts.length]; // arc.getSmoothness();
                    for(int s = 0; s < alteredCurveVerts.length; s++){
                        smoothness[s] = arc.getSmoothness()[0];
                    }
                    arc.setSmoothness(smoothness);
                }
            }
            
            if(obj instanceof Curve){
                Curve curve = (Curve)obj;
                if(curve.getSmoothness().length < alteredCurveVerts.length){
                    float smoothness [] = new float[alteredCurveVerts.length]; // arc.getSmoothness();
                    for(int s = 0; s < alteredCurveVerts.length; s++){
                        smoothness[s] = 1; // curve.getSmoothness()[s];
                    }
                    curve.setSmoothness(smoothness);
                }
            }
            
            setObject(obj);
            setMesh((Mesh)obj);
            info.setObject(obj);
            curveVerts = ((Mesh)info.getObject()).getVertexPositions();
            
        } else if(((mirrorIndex * 2)+1) < curveVerts.length) {        // Remove point
            
            int removeCount = curveVerts.length - ((mirrorIndex * 2)+1);
            System.out.println("remove from end " +  removeCount    );
            Vec3 [] alteredCurveVerts = new Vec3[curveVerts.length - removeCount];
            for(int i = 0; i < alteredCurveVerts.length; i++){
                alteredCurveVerts[i] = curveVerts[i];
            }
            ((Mesh)info.getObject()).setVertexPositions(alteredCurveVerts);
            Object3D obj = (Object3D)info.getObject();
            ((Mesh)obj).setVertexPositions(alteredCurveVerts);
            ((Mesh)getObject().getObject()).setVertexPositions(alteredCurveVerts);
            setObject(obj);
            setMesh((Mesh)obj);
            info.setObject(obj);
            
            curveVerts = ((Mesh)info.getObject()).getVertexPositions();
        }
        
        for(int p = 0; p < curveVerts.length; p++){
            Vec3 curveVec = new Vec3((Vec3)curveVerts[p]);
            Vec3 absoluteCurveVec = new Vec3((Vec3)curveVerts[p]);
            Mat4 curveMat4 = curveCS.duplicate().fromLocal();
            Mat4 inverseMat4 = inverseCurveCS.duplicate().fromLocal();
            
            //curveMat4.transform(curveVec); // ***
            curveMat4.transform(absoluteCurveVec);
            
            if(p == mirrorIndex){
                mirrorPoint = curveVec;
                absoluteMirrorPoint = absoluteCurveVec;
            }
            if(p > mirrorIndex && mirrorPoint != null){
                int refIndex =  mirrorIndex - (p - mirrorIndex);
                //System.out.println(" --- " + p + " refIndex " + refIndex);
                if(refIndex >= 0 && refIndex < curveVerts.length){ // Bounds check
                    Vec3 refVec = new Vec3((Vec3)curveVerts[refIndex]);
                    Vec3 absoluteRefVec = new Vec3((Vec3)curveVerts[refIndex]); // non translated
                    Vec3 previousAbsoluteRefVec = new Vec3((Vec3)curveVerts[refIndex]);
                    
                    //curveMat4.transform(refVec); // ***
                    curveMat4.transform(absoluteRefVec);
                    curveMat4.transform(previousAbsoluteRefVec);
                    
                    double xDelta = (Math.abs(refVec.x - mirrorPoint.x));
                    //System.out.println("p: " + p + " xDelta: " + xDelta);
                    double absoluteXDelta = (Math.abs(absoluteRefVec.x - absoluteMirrorPoint.x));
                    //System.out.println("abs p: " + p + " xDelta: " + absoluteXDelta);
                    
                    double xPos = 0;
                    if(mirrorPoint.x > refVec.x){
                        xPos = mirrorPoint.x + xDelta;
                    } else {
                        xPos = mirrorPoint.x - xDelta;
                    }
                    Vec3 alteredVec = new Vec3( xPos , refVec.y , refVec.z );
                    
                    double absoluteXPos = 0;
                    if(absoluteMirrorPoint.x > absoluteRefVec.x){
                        absoluteXPos = absoluteMirrorPoint.x + absoluteXDelta;
                    } else {
                        absoluteXPos = absoluteMirrorPoint.x - absoluteXDelta;
                    }
                    Vec3 absoluteAlteredVec = new Vec3( absoluteXPos , absoluteRefVec.y , absoluteRefVec.z );
                    //System.out.println("   absoluteAlteredVec: " + absoluteAlteredVec); // This is the absolute target, but we want the inverse OI CoordSystem that generates this.
                    //inverseMat4.transform(absoluteAlteredVec);
                    //System.out.println("    ---> absoluteAlteredVec: " + absoluteAlteredVec);
                    
                    //Vec3 delta = previousAbsoluteRefVec.minus( absoluteMirrorPoint ); //    // Translate to Mirror point
                    //inverseMat4.transform(delta);
                    
                    //System.out.println("delta: " + delta);
                    
                    //Vec3 moveVec = new Vec3(absoluteXPos, 0, 0);
                    //inverseMat4.transform(moveVec);
                    //System.out.println("   moveVec: " + moveVec);
                    
                    //alteredVec = new Vec3(alteredVec.x + moveVec.x, alteredVec.y + moveVec.y, alteredVec.z + moveVec.z);
                    
                    // Translate inverse
                    //inverseMat4.transform(alteredVec);
                    
                    curveVerts[p] = alteredVec; // Set new point location.
                }
            }
        }
        
        // Update object
        ((Mesh)info.getObject()).setVertexPositions(curveVerts);
        //((Curve)info.getObject()).setSmoothingMethod(3);
    
        //Curve theCurve = (Curve) getObject().getObject();
        //System.out.println(" -- " + theCurve.getSmoothingMethod()  );
        
        //float[] curveSmoothness = ((Curve)info.getObject()).getSmoothness();
        ((Curve)info.getObject()).setSmoothness(curveSmoothness);
        
        setMesh((Mesh)info.getObject()); // BUG Causes Loss smoothing ***
        
        info.clearCachedMeshes();
    }
    
    
    /**
     * mirrorNotchAcrossParent  (mirrorNotchAcrossParent)
     *
     * Description: If this object has a parent that is being mirrored, replicate this object across the parent mirror plane.
     */
    public void mirrorNotchAcrossParent(){
        //System.out.println("mirrorNotchAcrossParent");
        
        // Find the scene object, this editing object is only a copy without parent references.
        ObjectInfo sceneObjectInfo = null;
        Vector<ObjectInfo> objects = scene.getObjects();
        for(int i = 0; i < objects.size(); i++){
            ObjectInfo oi = (ObjectInfo)objects.elementAt(i);
            if(oi.getId() == getObject().getId()){
                //System.out.println("FOUND");
                sceneObjectInfo = oi;
                i = objects.size();
            }
        }
        
        if(sceneObjectInfo != null){
            ObjectInfo parentOI = sceneObjectInfo.getParent();
            if(parentOI != null){
                //System.out.println("parentOI: " + parentOI.getName());
                ObjectInfo[] parentChildren = parentOI.getChildren();
                for(int c = 0; c < parentChildren.length; c++){
                    ObjectInfo pcOi = (ObjectInfo)parentChildren[c];
                    if(pcOi.getId() != sceneObjectInfo.getId() && pcOi.getObject() instanceof MirrorPlaneObject && pcOi.isVisible()){
                        //System.out.println("Yes parent is a mirrored object. ");
                        // Find out the parent mirror point, where is the mirror plane location.
                        
                        int parentMirrorIndex = getMirrorIndex(parentOI);
                        //System.out.println("parentMirrorIndex: " + parentMirrorIndex);
                        
                        // Get the parent mirror location.
                        if(parentOI.getObject() instanceof ArcObject && parentOI.isVisible()){ //
                            ArcObject arc = (ArcObject)parentOI.getObject();
                            MeshVertex v[] = arc.getVertices();
                            if(v.length > parentMirrorIndex && parentMirrorIndex > -1){
                                Vec3 mirrorPoint = new Vec3(v[parentMirrorIndex].r);
                                CoordinateSystem coords = parentOI.getCoords();
                                Mat4 mat4 = coords.duplicate().fromLocal();
                                mat4.transform(mirrorPoint);    // Translate to scene world coordinates.
                                if(mirrorPoint != null){
                                    //System.out.println("Mirror Point " + mirrorPoint);
                                    // X is the value we want.
                                    
                                    // get curve verts
                                    
                                    //Curve editCurve = (Curve)sceneObjectInfo.getObject();
                                    //MeshVertex editVerts[] = editCurve.getVertices();
                                    BoundingBox editingCurveBounds = sceneObjectInfo.getTranslatedBounds();
                                    Vec3 editingCurveCentre = editingCurveBounds.getCenter();
                                    
                                    // Is this object on the source side?
                                    //if( editingCurveCentre.x < mirrorPoint.x ){
                                   
                                        // Does a mirrored object allready exist? if no create one in parent.
                                        boolean found = false;
                                        ObjectInfo existingMirrorCurve = null;
                                        for(int c2 = 0; c2 < parentChildren.length; c2++){
                                            ObjectInfo pcOi_2 = (ObjectInfo)parentChildren[c2];
                                            if(pcOi_2.getName().equals(sceneObjectInfo.getName() + " REPLICATED " + sceneObjectInfo.getId()) &&
                                               pcOi_2.getId() != sceneObjectInfo.getId() )
                                            {
                                                found = true;
                                                existingMirrorCurve = pcOi_2;
                                            }
                                        }
                                        //System.out.println("is replicated: " + found);
                                        
                                        // Calculate Mirror points
                                        Vector<Vec3> mirrorPoints = new Vector<Vec3>();
                                        //Curve curve = (Curve)sceneObjectInfo.getObject(); // don't use this one. ****  info
                                        Curve curve = (Curve)getObject().getObject(); // use the editing object version for changes.
                                        MeshVertex curveVerts[] = curve.getVertices();
                                        for(int i = 0; i < curveVerts.length; i++){
                                            Vec3 mirrorVec = new Vec3( curveVerts[i].r );
                                            CoordinateSystem curveCoords = sceneObjectInfo.getCoords(); // editing object world coordinates
                                            Mat4 curveMat4 = curveCoords.duplicate().fromLocal();
                                            curveMat4.transform(mirrorVec);    // Translate to scene world coordinates.
                                            mirrorVec.x += (mirrorPoint.x - mirrorVec.x) * 2;
                                            //mirrorVec.y += .1;
                                            mirrorPoints.addElement(mirrorVec);
                                        }
                                        
                                        // Add or update mirror object with points.
                                        if(found == false){ // Add replicated object
                                            Curve cur = (Curve)sceneObjectInfo.getObject(); // used for isClosed attribute
                                            addCurveFromPoints(mirrorPoints,
                                                               parentOI,
                                                               sceneObjectInfo.getName() + " REPLICATED " + sceneObjectInfo.getId(),
                                                               cur.isClosed());
                                        } else if(existingMirrorCurve != null){
                                            updateCurvePoints(existingMirrorCurve, mirrorPoints);
                                        }
                                    //}
                                }
                            }
                        }
                    }
                }
            }
            
            
            //
            // Children is a MirrorPlaneObject
            // This handles the case where the mirred curve centre is moved, the mirrored notches need to be updated too.
            //
            ObjectInfo[] children = sceneObjectInfo.getChildren();
            for(int c = 0; c < children.length; c++){
                ObjectInfo pcOi = (ObjectInfo)children[c];
                if(pcOi.getObject() instanceof Curve &&
                   pcOi.getObject() instanceof MirrorPlaneObject == false)
                {
                    if( pcOi.getName().indexOf("REPLICATED") == -1 ){
                        MirrorCurve mirror = new MirrorCurve();
                        mirror.update(pcOi);
                    }
                }
                 
            }
        }
        
        //System.out.println("mirrorNotchAcrossParent end");
    }
    
    /**
     * addCurveFromPoints
     *
     * Description: Add a curve to the scene given points.
     */
    public ObjectInfo addCurveFromPoints(Vector<Vec3> pointChain, ObjectInfo parentObjectInfo, String name, boolean closed){
        float[] s_ = new float[pointChain.size()]; // s_[0] = 0; s_[1] = 0; s_[2] = 0;
        for(int i = 0; i < pointChain.size(); i++){
            s_[i] = 0;
        }
        Vec3[] vertex = new Vec3[pointChain.size()]; // constructed curve geometry.
        for(int i = 0; i < pointChain.size(); i++){
            Vec3 v = pointChain.elementAt(i);
            vertex[i] = new Vec3(v);
        }
        Curve perferationCurve = new Curve(vertex, s_, 0, /* closed */closed ); // false
        CoordinateSystem coords = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
        ObjectInfo perferationInfo = new ObjectInfo(perferationCurve, coords, name);
        perferationInfo.setParent(parentObjectInfo); // Add perferation object to selection.
        parentObjectInfo.addChild(perferationInfo, parentObjectInfo.getChildren().length); // info.getChildren().length+1
        if(window != null){
            window.addObject(perferationInfo, null);
            window.updateImage();
            window.updateTree(); // window update tree
        }
        return perferationInfo;
    }
    
    public void updateCurvePoints(ObjectInfo objectInfo, Vector<Vec3> pointChain){
        Curve curve = (Curve)objectInfo.getObject();
        float[] s_ = new float[pointChain.size()]; // s_[0] = 0; s_[1] = 0; s_[2] = 0;
        for(int i = 0; i < pointChain.size(); i++){
            s_[i] = 0;
        }
        Vec3[] vertex = new Vec3[pointChain.size()]; // constructed curve geometry.
        for(int i = 0; i < pointChain.size(); i++){
            Vec3 v = pointChain.elementAt(i);
            vertex[i] = new Vec3(v);
        }
        curve.setVertexPositions(vertex);
        objectInfo.setObject(curve);
        objectInfo.clearCachedMeshes();
        if(window != null){
            window.updateImage();
            window.updateTree(); // window update tree
        }
    }
    
  //public int[] getSelectedIndices(){
  //    int [] indicies = new int[];
  //    return indicies;
  //}

  public int[] getSelectionDistance()
  {
    if (maxDistance != getTensionDistance())
      findSelectionDistance();
    return selectionDistance;
  }
  
  /** The return value has no meaning, since there is only one selection mode in this window. */
  
  public int getSelectionMode()
  {
    return 0;
  }
  
  /** This is ignored, since there is only one selection mode in this window. */
  
  public void setSelectionMode(int mode)
  {
  }

  /** Calculate the distance (in edges) between each vertex and the nearest selected vertex. */

  void findSelectionDistance()
  {
    Curve theCurve = (Curve) getObject().getObject();
    int i, j, dist[] = new int [theCurve.getVertices().length];
    
    maxDistance = getTensionDistance();
    
    // First, set each distance to 0 or -1, depending on whether that vertex is part of the
    // current selection.
    
    for (i = 0; i < dist.length; i++)
      dist[i] = selected[i] ? 0 : -1;

    // Now extend this outward up to maxDistance.

    for (i = 0; i < maxDistance; i++)
      {
        for (j = 0; j < dist.length-1; j++)
          if (dist[j] == -1 && dist[j+1] == i)
            dist[j] = i+1;
        for (j = 1; j < dist.length; j++)
          if (dist[j] == -1 && dist[j-1] == i)
            dist[j] = i+1;
        if (theCurve.isClosed())
          {
            if (dist[0] == -1 && dist[dist.length-1] == i)
              dist[0] = i+1;
            if (dist[0] == i && dist[dist.length-1] == -1)
              dist[dist.length-1] = i+1;
          }
      }
    selectionDistance = dist;
  }

  /* EditingWindow methods. */

  public void updateMenus()
  {
    super.updateMenus();
    int i;
    for (i = 0; i < selected.length && !selected[i]; i++);
    if (i < selected.length)
    {
      editMenuItem[0].setEnabled(true);
      for (i = 0; i < 6; i++)
        meshMenuItem[i].setEnabled(true);
    }
    else
    {
      editMenuItem[0].setEnabled(false);
      for (i = 0; i < 6; i++)
        meshMenuItem[i].setEnabled(false);
    }
  }
  
    /**
     * doOk
     *
     * Description: Button on curve editor prossed to transfer changes to scene object.
     */
  protected void doOk()
  {
    //System.out.println(" doOk() ");
      
    Curve theMesh = (Curve) objInfo.getObject();
    oldMesh.copyObject(theMesh);
    oldMesh = null;
      
      
      // Update replicated curves.
      Scene theScene = window.getScene();
      ObjectInfo sceneObjectInfo = theScene.getObjectById(objInfo.getId());
      ObjectInfo parent = sceneObjectInfo.getParent();
      ObjectInfo childMirrorPlane = null;
      ObjectInfo[] children = sceneObjectInfo.getChildren();
      for(int i = 0; i < children.length; i++){
          ObjectInfo child = (ObjectInfo)children[i];
          if(child.getObject() instanceof MirrorPlaneObject){
              childMirrorPlane = child;
          }
      }
      //if(childMirrorPlane != null && childMirrorPlane.getObject() instanceof MirrorPlaneObject){
      //    window.mirrorCurve.updateCurve(childMirrorPlane, objInfo, sceneObjectInfo);
      //}
       
      
      // Update copied notches.
      //
      window.mirrorCurve.update(sceneObjectInfo);
      
      //window.notchIntersections.updateReferenceMarker(sceneObjectInfo);
     
    dispose();
    onClose.run();
  }
  
  protected void doCancel()
  {
    oldMesh = null;
    dispose();
  }
  
  protected void freehandModeChanged()
  {
    for (int i = 0; i < theView.length; i++)
      ((CurveViewer) theView[i]).setFreehandSelection(((BCheckBoxMenuItem) editMenuItem[2]).getState());
  }
  
  private void smoothingChanged(CommandEvent ev)
  {
    Widget source = ev.getWidget();
    if (source == smoothItem[0])
      setSmoothingMethod(Mesh.NO_SMOOTHING);
    else if (source == smoothItem[1])
      setSmoothingMethod(Mesh.INTERPOLATING);
    else if (source == smoothItem[2])
      setSmoothingMethod(Mesh.APPROXIMATING);
  }

  /** Select the entire curve. */
  
  public void selectAllCommand()
  {
    //setUndoRecord(new UndoRecord(this, false, UndoRecord.SET_MESH_SELECTION, new Object [] {this, new Integer(0), selected.clone()}));
      setUndoRecord(new UndoRecord(this, false, UndoRecord.SET_MESH_SELECTION, new Object [] {this, 0, selected.clone()}));
    for (int i = 0; i < selected.length; i++)
      selected[i] = true;
    setSelection(selected);
  }

  /** Extend the selection outward by one edge. */
    // JDT
    // How is this called???
  public void extendSelectionCommand()
  {
    int oldDist = tensionDistance;
    tensionDistance = 1;
    int dist[] = getSelectionDistance();
    boolean newSel[] = new boolean [dist.length];
    tensionDistance = oldDist;
    
    setUndoRecord(new UndoRecord(this, false, UndoRecord.SET_MESH_SELECTION, new Object [] {this, new Integer(0), selected.clone()}));
    for (int i = 0; i < dist.length; i++)
      newSel[i] = (dist[i] == 0 || dist[i] == 1);
    setSelection(newSel);
  }
  
  /** Invert the current selection. */
  
  public void invertSelectionCommand()
  {
    boolean newSel[] = new boolean [selected.length];
    for (int i = 0; i < newSel.length; i++)
      newSel[i] = !selected[i];
    setUndoRecord(new UndoRecord(this, false, UndoRecord.SET_MESH_SELECTION, new Object [] {this, new Integer(0), selected}));
    setSelection(newSel);
  }

  public void deleteCommand()
  {
    if (!topology)
      return;
    int i, j, num = 0;
    Curve theCurve = (Curve) objInfo.getObject();
    boolean newsel[];
    MeshVertex vt[] = theCurve.getVertices();
    float s[] = theCurve.getSmoothness(), news[];
    Vec3 v[];

    for (i = 0; i < selected.length; i++)
      if (selected[i])
        num++;
    if (num == 0)
      return;
    if (!theCurve.isClosed() && selected.length-num < 2)
      {
        new BStandardDialog("", Translate.text("curveNeeds2Points"), BStandardDialog.INFORMATION).showMessageDialog(this);
        return;
      }
    if (theCurve.isClosed() && selected.length-num < 3)
      {
        new BStandardDialog("", Translate.text("curveNeeds3Points"), BStandardDialog.INFORMATION).showMessageDialog(this);
        return;
      }
    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theCurve, theCurve.duplicate()}));
    v = new Vec3 [vt.length-num];
    news = new float [vt.length-num];
    newsel = new boolean [vt.length-num];
    for (i = 0, j = 0; i < vt.length; i++)
    {
      if (!selected[i])
      {
        newsel[j] = selected[i];
        news[j] = s[i];
        v[j++] = vt[i].r;
      }
    }
    theCurve.setShape(v, news);
    setSelection(newsel);
  }

    /**
     * subdivideCommand
     *
     * Description: function subdivides selected points.
     */
  public void subdivideCommand()
  {
    Curve theCurve = (Curve) objInfo.getObject();
    MeshVertex vt[] = theCurve.getVertices();
    float s[] = theCurve.getSmoothness(), news[];
    boolean newsel[], split[];
    Vec3 v[], newpos[];
    int i, j, p1, p3, p4, splitcount = 0, method = theCurve.getSmoothingMethod();
    
    v = new Vec3 [vt.length];
    for (i = 0; i < vt.length; i++)
      v[i] = vt[i].r;
    
    // Determine which parts need to be subdivided.
    
    if (theCurve.isClosed())
      split = new boolean [vt.length];
    else
      split = new boolean [vt.length-1];
    for (i = 0; i < split.length; i++)
      if (selected[i] && selected[(i+1)%selected.length])
      {
        split[i] = true;
        splitcount++;
      }
    newpos = new Vec3 [vt.length+splitcount];
    news = new float [vt.length+splitcount];
    newsel = new boolean [vt.length+splitcount];
    
    // Do the subdivision.

    for (i = 0, j = 0; i < split.length; i++)
    {
      newsel[j] = selected[i];
      p1 = i-1;
      if (p1 < 0)
      {
        if (theCurve.isClosed())
          p1 = v.length-1;
        else
          p1 = 0;
      }
      if (i < v.length-1)
        p3 = i+1;
      else
      {
        if (theCurve.isClosed())
          p3 = 0;
        else
          p3 = v.length-1;
      }
      if (selected[i] && method == Mesh.APPROXIMATING)
        newpos[j] = Curve.calcApproxPoint(v, s, p1, i, p3);
      else
        newpos[j] = vt[i].r;
      if (selected[i])
        news[j] = Math.min(s[i]*2.0f, 1.0f);
      else
        news[j] = s[i];
      if (!split[i])
      {
        j++;
        continue;
      }
      if (method == Mesh.NO_SMOOTHING)
        newpos[j+1] = v[i].plus(v[p3]).times(0.5);
      else if (method == Mesh.INTERPOLATING)
      {
        if (i < v.length-2)
          p4 = i+2;
        else
        {
          if (theCurve.isClosed())
            p4 = (i+2)%v.length;
          else
            p4 = v.length-1;
        }
        newpos[j+1] = Curve.calcInterpPoint(v, s, p1, i, p3, p4);
      }
      else
        newpos[j+1] = v[i].plus(v[p3]).times(0.5);
      news[j+1] = 1.0f;
      newsel[j+1] = true;
      j += 2;
    }
    if (!theCurve.isClosed())
    {
      newpos[0] = v[0];
      newpos[j] = v[i];
      news[j] = s[i];
      newsel[j] = selected[i];
    }
    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theCurve, theCurve.duplicate()}));
    theCurve.setShape(newpos, news);
    setSelection(newsel);
  }

  public void setSmoothnessCommand()
  {
    final Curve theCurve = (Curve) objInfo.getObject();
    Curve oldCurve = (Curve) theCurve.duplicate();
    final MeshVertex vt[] = theCurve.getVertices();
    final float s[] = theCurve.getSmoothness();
    float value;
    final ValueSlider smoothness;
    int i;
    
    for (i = 0; i < selected.length && !selected[i]; i++);
    if (i == selected.length)
      return;
    value = 0.001f * (Math.round(s[i]*1000.0f));
    smoothness = new ValueSlider(0.0, 1.0, 100, (double) value);
    smoothness.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        float sm = (float) smoothness.getValue();
        float news[] = new float [vt.length];
        for (int i = 0; i < selected.length; i++)
          news[i] = selected[i] ? sm : s[i];
        theCurve.setSmoothness(news);
        objectChanged();
        for (int i = 0; i < theView.length; i++)
          theView[i].repaint();
      }
    } );
    ComponentsDialog dlg = new ComponentsDialog(this, Translate.text("setPointSmoothness"), new Widget [] {smoothness},
            new String [] {Translate.text("Smoothness")});
    if (dlg.clickedOk())
      setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theCurve, oldCurve}));
    else
    {
      theCurve.copyObject(oldCurve);
      objectChanged();
      for (int j = 0; j < theView.length; j++)
        theView[j].repaint();
    }
  }

  void setSmoothingMethod(int method)
  {
    Curve theCurve = (Curve) objInfo.getObject();

    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theCurve, theCurve.duplicate()}));
    for (int i = 0; i < smoothItem.length; i++)
      smoothItem[i].setState(false);
    if (method == Mesh.NO_SMOOTHING)
      smoothItem[0].setState(true);
    else if (method == Mesh.INTERPOLATING)
      smoothItem[1].setState(true);
    else
      smoothItem[2].setState(true);
    theCurve.setSmoothingMethod(method);    
    objectChanged();
    for (int i = 0; i < theView.length; i++)
      theView[i].repaint();
  }

  public void toggleClosedCommand()
  {
    Curve theCurve = (Curve) objInfo.getObject();

    setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {theCurve, theCurve.duplicate()}));
    if (theCurve.isClosed())
    {
      theCurve.setClosed(false);
      meshMenuItem[6].setText(Translate.text("menu.closedEnds"));
    }
    else
    {
      theCurve.setClosed(true);
      meshMenuItem[6].setText(Translate.text("menu.openEnds"));
    }
    setMesh(theCurve);
    for (int i = 0; i < theView.length; i++)
      theView[i].repaint();
  }

  /** Given a list of deltas which will be added to the selected vertices, calculate the
      corresponding deltas for the unselected vertices according to the mesh tension. */
  
  public void adjustDeltas(Vec3 delta[])
  {
    int dist[] = getSelectionDistance(), count[] = new int [delta.length];
    Curve theCurve = (Curve) objInfo.getObject();
    int maxDistance = getTensionDistance();
    double tension = getMeshTension(), scale[] = new double [maxDistance+1];

    for (int i = 0; i < delta.length; i++)
      if (dist[i] != 0)
        delta[i].set(0.0, 0.0, 0.0);
    for (int i = 0; i < maxDistance; i++)
    {
      for (int j = 0; j < count.length; j++)
        count[j] = 0;
      for (int j = 0; j < dist.length-1; j++)
      {
        if (dist[j] == i && dist[j+1] == i+1)
        {
          count[j+1]++;
          delta[j+1].add(delta[j]);
        }
        else if (dist[j+1] == i && dist[j] == i+1)
        {
          count[j]++;
          delta[j].add(delta[j+1]);
        }
      }
      if (theCurve.isClosed())
      {
        if (dist[0] == i && dist[dist.length-1] == i+1)
        {
          count[dist.length-1]++;
          delta[dist.length-1].add(delta[0]);
        }
        else if (dist[dist.length-1] == i && dist[0] == i+1)
        {
          count[0]++;
          delta[0].add(delta[dist.length-1]);
        }
      }
      for (int j = 0; j < count.length; j++)
        if (count[j] > 1)
          delta[j].scale(1.0/count[j]);
    }
    for (int i = 0; i < scale.length; i++)
      scale[i] = Math.pow((maxDistance-i+1.0)/(maxDistance+1.0), tension);
    for (int i = 0; i < delta.length; i++)
      if (dist[i] > 0)
        delta[i].scale(scale[dist[i]]);
  }
    
    /**
     * shiftCurvePointsCommand
     *
     * Description: Shift points.
     */
    public void shiftCurvePointsCommand(){
        //System.out.println(" shift ");
        if(getObject().getObject() instanceof Curve){
            ObjectInfo info = getObject();
            Curve theCurve = (Curve)info.getObject();
            MeshVertex v[] = ((Mesh)theCurve).getVertices();
            Vec3 v2[] = new Vec3[v.length];
            for(int i = 0; i < v.length; i++){
                Vec3 source = v[i].r;
                if(i < v.length - 1){
                    //System.out.println(" " + i + " <- " + (i+1) );
                    v2[i+1] = source;
                } else {
                    //System.out.println(" " + i + " <- " + 0 );
                    v2[0] = source;
                }
            }
            theCurve.setVertexPositions(v2);
            info.clearCachedMeshes();
            
            //setObject(theCurve);
            setMesh((Mesh)theCurve);
            for (int i = 0; i < theView.length; i++)
              theView[i].repaint();
        }
    }
    
    public void shiftToSelectionCommand(){
        System.out.println("shiftToSelectionCommand Not implemented.");
    }
    
    
    /**
     * splitSelectionCommand
     * Description: Split a curve at the selected point. Shift the points such that the start and end line up with the selected point index.
     *  Used when generating notch profiles across multiple segments.
     *
     *  Bug: In some cases splitting on an existing edge will shift on the wrong point by 1.
     */
    public void splitSelectionCommand(){
        hideInfographic();
        if(getObject().getObject() instanceof Curve){
            ObjectInfo info = getObject();
            Curve theCurve = (Curve)info.getObject();
            theCurve.setClosed(false);      // unclose the curve.
            
            // shift point positions.
            MeshVertex v[] = ((Mesh)theCurve).getVertices();
            Vector splitPoints = new Vector();
            
            // Get point selection points and indexes.
            Vector selectionIndexList = new Vector();
            int firstSelIndex = -1;
            int lastSelIndex = 999999;
            for(int s = 0; s < v.length; s++){
                if(selected[s] == true){
                    selectionIndexList.addElement(s);
                    if( s > firstSelIndex  ){
                        firstSelIndex = s;
                    }
                    if( s < lastSelIndex){
                        lastSelIndex = s;
                    }
                }
            }
            if(selectionIndexList.size() <= 0){
                System.out.println("Error: Please select at least one point to split.");
                JOptionPane.showMessageDialog(null,
                    "Please select points to split this curve.",
                    "Error",
                    JOptionPane.WARNING_MESSAGE);
            } else {
                for(int i = v.length - 1; i > 0; i--){
                    Vec3 currVec = (Vec3)v[i].r;
                    if( selectionIndexList.contains(i) ){
                        //System.out.println(" ---  i " + i);
                    } else {
                        splitPoints.addElement(currVec);
                    }
                }
                if(splitPoints.size() > 1){
                    // Remove selected points from object.
                    Vec3 v2[] = new Vec3[splitPoints.size()];
                    for(int i = 0; i < splitPoints.size(); i++){
                        Vec3 vec = (Vec3)splitPoints.elementAt(i);
                        v2[i] = vec;
                    }
                    // Shift points to start and end at the removed points.
                    int shiftPos = 0;
                    if( firstSelIndex != -1 ){
                        Vec3 [] v3 = new Vec3[v2.length];
                        for(int i = 0; i < v2.length; i++){
                            Vec3 vec = (Vec3)v2[i];
                            int shiftedIndex = i + lastSelIndex - 1; // firstSelIndex lastSelIndex
                            if(shiftedIndex < 0){
                                shiftedIndex = shiftedIndex + v2.length;
                            }
                            if(shiftedIndex >= v2.length){
                                shiftedIndex = shiftedIndex - v2.length;
                            }
                            //System.out.println("   --- i: " + i + "  shiftedIndex: " + shiftedIndex + " len: " + v2.length  + " firstSelIndex: " + firstSelIndex );
                            v3[shiftedIndex] = vec;
                        }
                        v2 = v3;
                    }
                    //System.out.println(" split edge " + firstSelIndex + " - " + lastSelIndex);
                    // Update object geometry
                    theCurve.setVertexPositions(v2);
                    info.clearCachedMeshes();
                    setMesh((Mesh)theCurve);
                    for (int i = 0; i < theView.length; i++)
                      theView[i].repaint();
                }
            }
        }
    }
    
    /**
     * equalizeSelectionCommand
     *
     * Description: 
     */
    public void equalizeSelectionCommand(){
        System.out.println("equalizeSelectionCommand Not implemented.");
        hideInfographic();
        if(getObject().getObject() instanceof Curve){
            ObjectInfo info = getObject();
            Curve theCurve = (Curve)info.getObject();
            MeshVertex v[] = ((Mesh)theCurve).getVertices();
            
        }
    }
    
    /**
     * conformCurveToMesh
     *
     * Description: Conform curve to mesh points in the scene.
     */
    public void conformCurveToMeshCommand(){
        hideInfographic();
        //
        System.out.println("conformCurveToMesh: Not implemented. ");
        //ConformMesh cm = new ConformMesh(window);
        //cm.conformCurveToMesh(scene, info);
    }
    
    /**
     * numericEditObjectCommand
     *
     * Description: Numeric object editor.
     */
    public void numericEditObjectCommand(){
        //NumericObjectEditor numericEditor = new NumericObjectEditor(window, info);
    }
    
    
    /**
     * mouseEnteredEvent
     *  Description: for menus
     */
    public void mouseEnteredEvent(MouseEnteredEvent ev){
        // solidsResolvedViewMenuItem
        java.lang.Object source = ev.getSource();
        if(source instanceof BMenuItem){ // && ((BMenuItem)source) == solidsResolvedViewMenuItem
            BMenuItem menuItem = ((BMenuItem)source);
            String nameID = menuItem.getName();
            //System.out.println(" menu name: " + nameID);
            int offset = 0;
            
            // Offset more on submenus.
            WidgetContainer parent = menuItem.getParent();
            if( parent != null){
                WidgetContainer parent2 = parent.getParent();
                if(parent2 != null){
                    //System.out.println("       Parent2: " + parent2.getClass().getName() + " name: " + parent2.getName()  );
                    //System.out.println("       Parent2: " );
                    WidgetContainer parent3 = parent2.getParent();
                    if(parent3 != null){
                        //System.out.println("          Parent3: " + parent3.getClass().getName() + " name: " + parent3.getName()  );
                        //System.out.println("       Parent3: " );
                        if(parent3 instanceof BMenuBar){
                            offset = 150;
                        }
                    }
                }
            }
            
            // If an info graphic image exists load the dialog.
            String text = menuItem.getText();
            //System.out.println(" text " + text);
            
            if(infoGraphicDialog == null){
                infoGraphicDialog = new JDialog();
                infoGraphicDialog.setLayout(new FlowLayout());
                infoGraphicDialog.setUndecorated(true);
                infoGraphicDialog.setType(Window.Type.POPUP);
                infoGraphicDialog.setFocusableWindowState(false);
                infoGraphicDialog.setPreferredSize(new Dimension(400, 433));
            }
            
            ImageIcon titleImage = new ImageIcon(getClass().getResource("/armarender/infographic/title_400.png"));
            JLabel label = new JLabel(titleImage);
            label.setVerticalAlignment(JLabel.TOP);
            label.setVerticalTextPosition(JLabel.TOP);
            infoGraphicDialog.add(label);
            
            ImageIcon illustrationImage = new ImageIcon(getClass().getResource("/armarender/infographic/"+nameID+".png"));
            JLabel illustrationLabel = new JLabel(illustrationImage);
            //illustrationLabel.setVerticalAlignment(JLabel.TOP);
            //illustrationLabel.setVerticalTextPosition(JLabel.TOP);
            infoGraphicDialog.add(illustrationLabel);
            
            //JTextField descriptionField = new JTextField(20);
            //descriptionField.setText("Description.");
            //infoGraphicDialog.add(descriptionField);
            
            double x = 0;
            double y = 0;
            int width = 0;
            
            java.awt.Rectangle windowBoundsRect = this.getBounds();
            java.awt.Rectangle menuItemBoundsRect = menuItem.getBounds();
            java.awt.Rectangle parentBoundsRect = menuItem.getParent().getBounds();
            java.awt.Rectangle parent2BoundsRect = menuItem.getParent().getParent().getBounds();
            
            x = windowBoundsRect.getX() + menuItemBoundsRect.getX() + parentBoundsRect.getX() + parent2BoundsRect.getX();
            y = windowBoundsRect.getY() + 24 + 4; // menuItemBoundsRect.getY() + parentBoundsRect.getY() +
            width = (int)menuItemBoundsRect.getWidth();
            
            //System.out.println("   boundsRect " + x + " " + y  );
            
            infoGraphicDialog.setLocation((int)x + width + 150 + offset, (int)y);
            
            infoGraphicDialog.pack();
            infoGraphicDialog.setVisible(true);
        }
    }
    
    /**
     * mouseExitedEvent
     */
    public void mouseExitedEvent(MouseExitedEvent ev){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
    }
    
    public void hideInfographic(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
    }
    
    
    /**
     * setCamera
     *
     * Description: Set the camera when editing a curve such that the edit view takes the same visual position / location / orientation as the main viewer.
     *
     * @param: CoordinateSystem
     * @param: double scale
     * @param: boolean perspective
     */
    public void setCamera(CoordinateSystem cs, double scale, boolean perspective){
        ViewerCanvas viewerCanvas = getView(); // Get active ViewerCanvas
        ViewerCanvas[] views = getAllViews();
        
        if(numViewsShown == 4){     // If the editor window has four views, set the view to the forth window.
            viewerCanvas = views[3];
        }
        if(viewerCanvas != null && cs != null){
            Camera camera = viewerCanvas.getCamera();
            camera.setCameraCoordinates(cs);
            viewerCanvas.setScale(scale);
            viewerCanvas.setPerspective(perspective);
            viewerCanvas.setOrientation( 6 ); // Tell forth viewercanvas to display the view drop down as 'Other'.
        }
        // Set scale for all views to that of the source view.
        {
            ViewerCanvas vcA = views[0];
            vcA.setScale(scale);
        }
        {
            ViewerCanvas vcA = views[1];
            vcA.setScale(scale);
        }
        {
            ViewerCanvas vcA = views[2];
            vcA.setScale(scale);
        }
        {
            ViewerCanvas vcA = views[3];
            vcA.setScale(scale);
        }
    }
}
