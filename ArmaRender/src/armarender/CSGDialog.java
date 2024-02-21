/* Copyright (C) 2001-2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

// WIP makePreview

package armarender;

import armarender.object.*;
import armarender.texture.*;
import armarender.ui.*;
import armarender.math.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;

/** This dialog box allows the user to specify options for CSG objects. */

public class CSGDialog extends BDialog
{
  private CSGObject theObject;
  private CSGModeller modeller;
  private Texture texture;
  private BComboBox opChoice;
  private ObjectPreviewCanvas preview;
  private int operation[];
  private boolean ok;

  public CSGDialog(EditingWindow window, CSGObject obj)
  {
    super(window.getFrame(), true);
    theObject = obj;
    Scene scene = window.getScene();
    texture = scene.getDefaultTexture();
    
    // Layout the window.
    
    BorderContainer content = new BorderContainer();
    setContent(content);
    RowContainer opRow = new RowContainer();
    content.add(opRow, BorderContainer.NORTH);
    opRow.add(new BLabel(Translate.text("Operation")+":"));
    opRow.add(opChoice = new BComboBox());
    int i = 0;
    operation = new int [4];
    if (obj.getObject1().getObject().isClosed() && obj.getObject2().getObject().isClosed())
    {
      opChoice.add(Translate.text("Union"));
      operation[i++] = CSGObject.UNION;
    }
    opChoice.add(Translate.text("Intersection"));
    operation[i++] = CSGObject.INTERSECTION;
    if (obj.getObject2().getObject().isClosed())
    {
      opChoice.add(Translate.text("firstSecond"));
      operation[i++] = CSGObject.DIFFERENCE12;
    }
    if (obj.getObject1().getObject().isClosed())
    {
      opChoice.add(Translate.text("secondFirst"));
      operation[i++] = CSGObject.DIFFERENCE21;
    }
    for (int j = 0; j < i; j++)
      if (obj.getOperation() == operation[j])
        opChoice.setSelectedIndex(j);
    opChoice.addEventLink(ValueChangedEvent.class, this, "makePreview");

    // Add the preview canvas.
    
    content.add(preview = new ObjectPreviewCanvas(null), BorderContainer.CENTER);
    preview.setPreferredSize(new Dimension(600, 400));
    
    // Add the buttons at the bottom.
    
    RowContainer buttons = new RowContainer();
    buttons.add(Translate.button("ok", this, "doOk"));
    buttons.add(Translate.button("cancel", this, "dispose"));
    content.add(buttons, BorderContainer.SOUTH, new LayoutInfo());
    addEventLink(WindowClosingEvent.class, this, "doOk");
    makePreview();
    pack();
    UIUtilities.centerDialog(this, window.getFrame());
    setVisible(true);
  }
  
  private void doOk()
  {
    theObject.setOperation(operation[opChoice.getSelectedIndex()]);
    ok = true;
    dispose();
  }
  
  // Create a preview object.
  // BUG WITH COORDINATES
  private void makePreview()
  {
      double tol = ArmaRender.getPreferences().getInteractiveSurfaceError();
        if (modeller == null)
        {
            TriangleMesh mesh1, mesh2;

            mesh1 = theObject.getObject1().getObject().convertToTriangleMesh(tol);
            mesh2 = theObject.getObject2().getObject().convertToTriangleMesh(tol);

            modeller = new CSGModeller(mesh1, mesh2, theObject.getObject1().getCoords(), theObject.getObject2().getCoords());

        }
        TriangleMesh trimesh = modeller.getMesh(operation[opChoice.getSelectedIndex()], texture);
        trimesh.setTexture(texture, texture.getDefaultMapping(trimesh));
    
      for(int i = 0; i < theObject.getAdditionalObjects().size(); i++){
          ObjectInfo additionalOI = theObject.getAdditionalObjects().elementAt(i);
          //theScene.addObject(additionalOI.getObject().duplicate(), additionalOI.getCoords().duplicate(), additionalOI.getName(), null);
          
          
          
          
          //CSGObject newobj = new CSGObject(inputObj.elementAt(0), inputObj.elementAt(1), CSGObject.UNION);
          //Vec3 center = newobj.centerObjects();
          Vec3 center = trimesh.getBounds().getCenter();   // BoundingBox
          CoordinateSystem existingCS = new CoordinateSystem(center, Vec3.vz(), Vec3.vy());
          
          //System.out.println(" 1 " + theObject.getObject1().getCoords().getOrigin() );  // Wrong
          //System.out.println(" 2 " + theObject.getObject2().getCoords().getOrigin() );
          //System.out.println(" A " + additionalOI.getCoords().getOrigin()    );         // Correct
          //System.out.println(" C " + existingCS.getOrigin()    );                       // wrong
          
          //CoordinateSystem cs_ = null;
          CoordinateSystem cs_2 = additionalOI.getCoords().duplicate();
          if(theObject.cs != null ){
              //existingCS = theObject.cs;
              cs_2.getOrigin().subtract( theObject.cs.getOrigin() );
              //System.out.println(" theObject.cs is SET ******* ");
          } else {
              //System.out.println(" theObject.cs is null ******* ");
          }
          
          TriangleMesh additionalMesh = additionalOI.getObject().convertToTriangleMesh(tol);
          modeller = new CSGModeller(trimesh,
                                     additionalMesh,
                                     existingCS ,            // *** theObject.getObject1().getCoords()
                                     cs_2                     //   additionalOI.getCoords()   theObject.getObject2().getCoords()
                                     );
          trimesh = modeller.getMesh(  operation[opChoice.getSelectedIndex()]   , texture);
          trimesh.setTexture(texture, texture.getDefaultMapping(trimesh));
      }
      
      
      preview.setObject(trimesh);
    preview.repaint();
  }
  
  // Determine whether the user clicked the OK button.
  
  public boolean clickedOk()
  {
    return ok;
  }
}
