/*  VectorRender.java  */

/*
 * VectorRender: A VectorRender renderer for ArtOfIllusion.
 *
 * Author: Nik Trevallyn-Jones, nik777@users.sourceforge.net
 * $Id: Exp $
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * with this program. If not, the license is available from the
 * GNU project, at http://www.gnu.org.
 */

/*
 *  This is a derivitave work.
 *
 *  This code is based very strongly on the original Raster renderer
 *  (artofillusion.raster.Raster), the wireframe animation
 *  previewer (artofillusion.animation.AnimationPreviewer), and the wireframe
 *  drawing code from (artofillusion.object.Object3D), all of which
 *  were written by and are copyright (C) of Peter Eastman.
 */

package armarender.vector;

import armarender.*;
import armarender.image.*;
import armarender.material.*;
import armarender.math.*;
import armarender.object.*;
import armarender.texture.*;
import armarender.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.io.*;
import java.util.zip.GZIPOutputStream;

/**
 *  VectorRender is a Renderer which generates images using vector operations.
 *
 *  As well as the usual raster (pixel array) output, VectorOutput can also
 *  output vector-operations that can easily be converted into various
 *  vector formats, such as SVG.
 */

public class VectorRender implements Runnable
{

    /** colour scheme enums  */
    public static final int FULL	= 0;
    public static final int GREYSCALE	= 1;
    public static final int INCIDENT	= 2;
    public static final int REFLECTED	= 3;
    public static final int BLACKWHITE	= 4;
    public static final int WHITEBLACK	= 5;
    public static final int RANDOM	= 6;
    public static final int WHATEVER	= 7;

    protected static final int WIREFRAME	= 0;
    protected static final int TRIMESH		= 1;
    protected static final int POLYGON		= 2;

    protected static final int NONE		= 0;
    protected static final int ORDER_OBJECT	= 1;
    protected static final int ORDER_ATOM	= 2;

    protected static final int NORMAL_PARALLEL	= 1;
    protected static final int NORMAL_SPHERICAL	= 2;

    protected static final String FORMAT_EXTENSION[] = {null, "svg"};

  ObjectInfo light[], objarray[];
    double dist[], rdist[];
    long rlist[];
    //FormContainer configPanel;
    ColumnContainer configPanel;
  BCheckBox transparentBox, adaptiveBox, lightBox, reflectBox, emissiveBox, vector2DBox, vectorCompressBox, proportionBox, calcTextureBox, insideBox;
  BComboBox shadeChoice, aliasChoice, sampleChoice, colourChoice, strokeChoice, vectorTypeChoice, modeChoice, zorderChoice, globalChoice;
  ValueField errorField, smoothField, globalXField, globalYField, globalZField;
    BTextField vectorPathField,  insidePreview, vector2DPreview;
    BButton vectorBrowseButton, insideChoose, vector2DChoose;
  int pixel[], imagePixel[], width, height, envMode, imageWidth, imageHeight;
  int samplesPerPixel, subsample, antialias, debug, colourScheme, strokeWidth;
    int vertices[], renderMode, zorder, globalNormal;
    int polyx[]=null, polyy[]=null;
  float zbuffer[], imageZbuffer[];
  long updateTime;
  MemoryImageSource imageSource;
  Scene theScene;
  Camera theCamera;
  RenderListener listener;
  Image img;
    BufferedImage renderImg;
  Thread renderThread;
    Vec3 tempVec[], midPoint, centre, norm, parallel, dist1, dist2, poly[],
	globalVec;
  RGBColor color, tempColor[], ambColor, envColor, fogColor, diffuse,
      specular, grey, insideColour, vector2DColour;

    ArrayList objlist;
  TextureMapping envMapping;
  TextureSpec surfSpec, surfSpec2;
    Texture tex;
  MaterialSpec matSpec;
  double time, smoothing, smoothScale, depthOfField, focalDist, surfaceError, fogDist, texParams[];
  boolean fog, transparentBackground, adaptive, positionNeeded, depthNeeded, fixedColour, lightEffects, lights, reflections, emissive,
      proportional, calcTexture, defaultVals=true, inside;
    Graphics2D g;
    HashMap colours;

    RenderAtom[] atomlist;

    // Vector output variables
    int vectorType = 0;
    boolean vector2D = false, vectorCompress = false;
    String vectorPath, vectorName;
    Graphics vecg = null;

    // visibility
    Visibility visible;

  public static final double TOL = 1e-12;

  public VectorRender()
  {
  }
  
  /* Methods from the Renderer interface. */

  public synchronized void renderScene(Scene theScene, Camera camera, RenderListener rl, SceneCamera sceneCamera)
  {
    Dimension dim = camera.getSize();

    listener = rl;
    this.theScene = theScene;
    theCamera = camera.duplicate();
    if (sceneCamera == null)
      {
        depthOfField = 0.0;
        focalDist = theCamera.getDistToScreen();
        depthNeeded = false;
      }
    else
      {
        depthOfField = sceneCamera.getDepthOfField();
        focalDist = sceneCamera.getFocalDistance();
        depthNeeded = ((sceneCamera.getComponentsForFilters()&ComplexImage.DEPTH) != 0);
      }
    time = theScene.getTime();
    if (imagePixel == null || imageWidth != dim.width || imageHeight != dim.height)
      {
	imageWidth = dim.width;
	imageHeight = dim.height;
	imagePixel = new int [imageWidth*imageHeight];
        imageZbuffer = new float [imageWidth*imageHeight];
	imageSource = new MemoryImageSource(imageWidth, imageHeight, imagePixel, 0, imageWidth);
	imageSource.setAnimated(true);
	img = Toolkit.getDefaultToolkit().createImage(imageSource);
	renderImg = null;
      }

    if (samplesPerPixel <= 0) samplesPerPixel = 1;

    width = imageWidth*samplesPerPixel;
    height = imageHeight*samplesPerPixel;

    if (samplesPerPixel == 1)
      {
	pixel = imagePixel;
	zbuffer = imageZbuffer;
      }
    else if (pixel == null || pixel.length != width*height)
      {
	pixel = new int [width*height];
	zbuffer = new float [width*height];
      }

    theCamera.setSize(width, height);
    theCamera.setDistToScreen(theCamera.getDistToScreen()*samplesPerPixel);
    color = new RGBColor();
    surfSpec = new TextureSpec();
    surfSpec2 = new TextureSpec();

    diffuse = new RGBColor();
    specular = new RGBColor();
    grey = new RGBColor();

    matSpec = new MaterialSpec();
    tempColor = new RGBColor [3];
    for (int i = 0; i < tempColor.length; i++)
      tempColor[i] = new RGBColor(0.0f, 0.0f, 0.0f);
    tempVec = new Vec3 [4];
    poly = new Vec3[3];
    for (int i = 0; i < tempVec.length; i++)
      tempVec[i] = new Vec3();
    midPoint = new Vec3();
    centre = new Vec3();
    norm = new Vec3();
    parallel = new Vec3();

    if (objlist == null)
	objlist = new ArrayList(theScene.getNumObjects());

    renderThread = new Thread(this);
    renderThread.start();
  }

  public synchronized void cancelRendering(Scene sc)
  {
    Thread t = renderThread;
    
    if (theScene != sc)
      return;
    renderThread = null;
    if (t == null)
      return;
    try
      {
        while (t.isAlive())
          {
            Thread.sleep(100);
          }
      }
    catch (InterruptedException ex)
      {
      }
    if (listener != null) listener.renderingCanceled();
    listener = null;
    finish();
  }

  public Widget getConfigPanel()
  {
    if (configPanel == null)
    {
	configPanel = new ColumnContainer();
	//configPanel = new FormContainer(3, 12);
	FormContainer formPanel = new FormContainer(3, 8);
	configPanel.add(formPanel);

      LayoutInfo leftLayout = new LayoutInfo(LayoutInfo.EAST, LayoutInfo.NONE, new Insets(0, 0, 0, 5), null);
      LayoutInfo rightLayout = new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, null, null);
      /*
      formPanel.add(Translate.label("shadingMethod"), 0, 1, leftLayout);
      */
      formPanel.add(Translate.label("wireframeMode"), 0, 1, leftLayout);
      formPanel.add(Translate.label("colourScheme"), 0, 2, leftLayout);
      formPanel.add(Translate.label("lineWidth"), 0, 3, leftLayout);
      formPanel.add(Translate.label("antialias"), 0, 4, leftLayout);
      formPanel.add(Translate.label("orderByDistance"), 0, 5, leftLayout);

      /*
      formPanel.add(shadeChoice = new BComboBox(new String [] {
        Translate.text("gouraud"),
        Translate.text("hybrid"),
        Translate.text("phong")
      }), 1, 1, rightLayout);
      shadeChoice.setSelectedIndex(1);
      */

      double randname = Math.random();

      formPanel.add(modeChoice = new BComboBox(new String [] {
	  Translate.text("Wireframe"),
	  Translate.text("Mesh"),
	  Translate.text("Polygon"),
      }), 1, 1, rightLayout);

      formPanel.add(colourChoice = new BComboBox(new String [] {
	  Translate.text("FullColour"),
	  Translate.text("GreyScale"),
	  Translate.text("Incident"),
	  Translate.text("Reflections"),
	  Translate.text("BlackOnWhite"),
	  Translate.text("WhiteOnBlack"),
	  Translate.text("Random"),
	  Translate.text(randname < 0.25 ? "Whatever" : randname < 0.5 ? "Confetti" : randname < 0.75 ? "Rainbow" : "Other")
      }), 1, 2, rightLayout);

      //formPanel.add(Translate.button("setColours", this, "showColourWindow"), 2, 2, rightLayout);

      formPanel.add(strokeChoice = new BComboBox(new String []
	  { "1", "2", "3" }), 1, 3, rightLayout);

      formPanel.add(aliasChoice = new BComboBox(new String [] {
        Translate.text("none"),
        Translate.text("lines")
	//        Translate.text("pixels")
      }), 1, 4, rightLayout);

      formPanel.add(zorderChoice = new BComboBox(new String[] {
	  Translate.text("none"),
	  Translate.text("objects"),
	  Translate.text("sub-objects")
      }), 1, 5, rightLayout);

      zorderChoice.setSelectedIndex(1);

      //sampleChoice.setEnabled(false);

      ColumnContainer boxes = new ColumnContainer();
      configPanel.add(boxes);
      boxes.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, null, null));
      //boxes.add(zorderBox = new BCheckBox(Translate.text("orderbyDistance"), true));
      boxes.add(transparentBox = new BCheckBox(Translate.text("transparentBackground"), false));

      /*
      configPanel.add(zorderBox = new BCheckBox(Translate.text("OrderByDistance"), true), 0, 6, 3, 1);

      configPanel.add(transparentBox = new BCheckBox(Translate.text("transparentBackground"), false), 0, 7, 3, 1);

      configPanel.add(lightBox = new BCheckBox(Translate.text("lights"), true), 0, 8, 3, 1);

      configPanel.add(reflectBox = new BCheckBox(Translate.text("reflections"), true), 0, 9, 3, 1);

      configPanel.add(emissiveBox = new BCheckBox(Translate.text("emissive"), true), 0, 10, 3, 1);
      */

      RowContainer buttons = new RowContainer();
      configPanel.add(buttons);
      buttons.add(Translate.button("advanced", this, "showAdvancedWindow"));
      buttons.add(Translate.button("vectorOutput", this, "showVectorWindow"));
 
      errorField = new ValueField(0.08, ValueField.POSITIVE, 6);
      lightBox = new BCheckBox(Translate.text("lighting"), true);
      reflectBox = new BCheckBox(Translate.text("reflections"), false);
      emissiveBox = new BCheckBox(Translate.text("emissive"), true);
      calcTextureBox = new BCheckBox(Translate.text("calculateTextures"), false);
      insideBox = new BCheckBox(Translate.text("insideColour"), false);

      insidePreview = new BTextField("  ", 2);
      insidePreview.setBackground(insideBox.getBackground());
      insidePreview.setEnabled(false);

      insideChoose = Translate.button("Choose", this, "chooseInsideColour");
      insideColour = new RGBColor(0.2, 0.2, 0.2);

      insideBox.addEventLink(ValueChangedEvent.class, new Object() {
	      void processEvent()
	      {
		  insideChoose.setEnabled(insideBox.getState());

		  insidePreview.setBackground(insideBox.getState()
				  ? insideColour.getColor()
				  : insideBox.getBackground());
	      }
	  });

      globalChoice = new BComboBox(new String[] {
	  Translate.text("none"),
	  Translate.text("parallel"),
	  Translate.text("spherical")
      });

      globalXField = new ValueField(0, 0, 3);
      globalYField = new ValueField(0, 0, 3);
      globalZField = new ValueField(0, 0, 3);
      globalVec = new Vec3();

      globalChoice.addEventLink(ValueChangedEvent.class, new Object() {
	      void processEvent()
	      {
		  switch (globalChoice.getSelectedIndex()) {
		  case NORMAL_PARALLEL:
		      globalXField.setValue(0);
		      globalYField.setValue(0);
		      globalZField.setValue(-1);

		      globalXField.setEnabled(true);
		      globalYField.setEnabled(true);
		      globalZField.setEnabled(true);
		      break;

		  case NORMAL_SPHERICAL:
		      globalXField.setValue(0);
		      globalYField.setValue(0);
		      globalZField.setValue(5);

		      globalXField.setEnabled(true);
		      globalYField.setEnabled(true);
		      globalZField.setEnabled(true);
		      break;

		  default:
		      globalXField.setEnabled(false);
		      globalYField.setEnabled(false);
		      globalZField.setEnabled(false);
		  }
	      }
	  });

      sampleChoice = new BComboBox(new String [] {
	  Translate.text("none"), "2x2", "3x3"});

      smoothField = new ValueField(1.0, ValueField.NONNEGATIVE);
      adaptiveBox = new BCheckBox(Translate.text("reduceAccuracyForDistant"), true);
      proportionBox = new BCheckBox(Translate.text("adjustAccuracyBySize"), true);
      vectorTypeChoice = new BComboBox(new String[] {
	  Translate.text("none"),
	  Translate.text("SVG")
      });

      vectorTypeChoice.addEventLink(ValueChangedEvent.class, new Object() {
        void processEvent()
        {
	    int pos = vectorTypeChoice.getSelectedIndex();

	    vectorPathField.setEnabled(pos > 0);
	    vectorBrowseButton.setEnabled(pos > 0);
	    vectorCompressBox.setEnabled(pos > 0);
	}
      });

      vectorPathField = new BTextField("Untitled", 15);
      vectorBrowseButton = Translate.button("browse", this,"showBrowseWindow");
      vectorCompressBox = new BCheckBox(Translate.text("vectorCompress"), true);

      vector2DBox = new BCheckBox(Translate.text("include2D"), false);

      vector2DPreview = new BTextField("  ", 2);
      vector2DPreview.setBackground(vector2DBox.getBackground());
      vector2DPreview.setEnabled(false);

      vector2DChoose = Translate.button("Choose", this, "choose2DColour");
      vector2DColour = new RGBColor(0.9, 0.9, 0.9);

      vector2DBox.addEventLink(ValueChangedEvent.class, new Object() {
	      void processEvent()
	      {
		  vector2DChoose.setEnabled(vector2DBox.getState());

		  vector2DPreview.setBackground(vector2DBox.getState()
				  ? vector2DColour.getColor()
				  : vector2DBox.getBackground());
	      }
	  });

      colourChoice.addEventLink(ValueChangedEvent.class, new Object() {
        void processEvent()
        {
	    int pos = colourChoice.getSelectedIndex();

	    if (pos == INCIDENT || pos == REFLECTED || pos == BLACKWHITE ||
		pos == WHITEBLACK) {

		lightBox.setEnabled(false);
		reflectBox.setEnabled(false);
		emissiveBox.setEnabled(false);
	    }
	    else {
		lightBox.setEnabled(true);
		reflectBox.setEnabled(true);
		emissiveBox.setEnabled(true);
	    }
		
	    double randname = Math.random();
	    colourChoice.replace(7, (randname < 0.25 ? "Whatever" : randname < 0.5 ? "Confetti" : randname < 0.75 ? "Rainbow" : "Other"));
	    colourChoice.setSelectedIndex(pos);
        }
      });

      visible = new Visibility();

      /*
      aliasChoice.addEventLink(ValueChangedEvent.class, new Object() {
        void processEvent()
        {
          sampleChoice.setEnabled(aliasChoice.getSelectedIndex() > 0);
        }
      });
      */
    }

    vectorTypeChoice.setSelectedIndex(vectorType);

    return configPanel;
  }

    private void showColourWindow(WidgetEvent ev)
    {}

    private void showVectorWindow(WidgetEvent ev)
    {
	vectorType = vectorTypeChoice.getSelectedIndex();
	vectorName = vectorPathField.getText();
	vectorCompress = vectorCompressBox.getState();

	vectorPathField.setEnabled(vectorType > 0);
	vectorBrowseButton.setEnabled(vectorType > 0);
	vectorCompressBox.setEnabled(vectorType > 0);

	String slash = System.getProperty("file.separator");
	String suffix = FORMAT_EXTENSION[vectorType];
	if (vectorCompress) suffix = suffix + "z";

	// Show the window.
	
	RowContainer path = new RowContainer();
	path.add(vectorPathField);
	path.add(vectorBrowseButton);

	Widget[] widg = new Widget [] {
	    vectorTypeChoice,
	    path,
	    vectorCompressBox
	};

	String [] titles = new String[] {
	    Translate.text("vectorType"),
	    Translate.text("savePath"),
	    null
	};

	WindowWidget parent = UIUtilities.findWindow(ev.getWidget());
	ComponentsDialog dlg;
	if (parent instanceof BDialog)
	    dlg = new
		ComponentsDialog((BDialog) parent,
				 Translate.text("vectorOutput"), 
				 widg, titles);
	else
	    dlg = new
		ComponentsDialog((BFrame) parent,
				 Translate.text("vectorOutput"), 
				 widg, titles);

	if (dlg.clickedOk()) {
	    vectorType = vectorTypeChoice.getSelectedIndex();
	    vectorName = vectorPathField.getText();

	    // separate out the pathname
	    int pos = vectorName.lastIndexOf(slash);
	    if (pos > 0) {
		vectorPath = vectorName.substring(0, pos);
		vectorName = vectorName.substring(pos+1);
	    }
	    else if (vectorPath == null || vectorPath.length() == 0)
		vectorPath = ModellingApp.currentDirectory;

	    vectorCompress = vectorCompressBox.getState();

	    File file = (vectorPath != null
			 ? new File(vectorPath, vectorName)
			 : ModellingApp.currentDirectory != null
			 ? new File(ModellingApp.currentDirectory, vectorName)
			 : new File(vectorName));

	    if (file.exists()) {
                String options[] = new String [] {
		    Translate.text("Yes"), Translate.text("No")
		};

                int choice = new
		    BStandardDialog(Translate.text("fileExists"),
				    Translate.text("overwriteFile",
						   file.getName()),
				    BStandardDialog.QUESTION).
		    showOptionDialog(parent, options, options[1]);

                if (choice==1)
		    vectorType = 0;
	    }

	}

	else {
	    // Reset the components.
	    vectorTypeChoice.setSelectedIndex(vectorType);
	    vectorPathField.setText(vectorName);
	    vectorCompressBox.setState(vectorCompress);
	}
    }

    private void showBrowseWindow(WidgetEvent ev)
    {
	WindowWidget parent = UIUtilities.findWindow(ev.getWidget());

	BFileChooser fc = new BFileChooser(BFileChooser.SAVE_FILE,
					   Translate.text("saveVectorFile"));

	String filename = vectorPathField.getText();

	File file = (vectorPath != null
		     ? new File(vectorPath, filename)
		     : (ModellingApp.currentDirectory != null
			? new File(ModellingApp.currentDirectory, filename)
			: new File(filename)));

	fc.setSelectedFile(file);
	boolean ok = fc.showDialog(parent);
	if (ok) {
	    file = fc.getSelectedFile();
	    vectorPath = file.getParentFile().getAbsolutePath();
	    vectorName = file.getName();

	    vectorPathField.setText(vectorName);
	}
    }

    private void chooseInsideColour(WidgetEvent ev)
    {
	BFrame parent = UIUtilities.findFrame(ev.getWidget());
	
	ColorChooser dlg = new ColorChooser(parent,
					    "choose inside colour",
					    insideColour);

	insidePreview.setBackground(insideBox.getState()
				    ? insideColour.getColor()
				    : insideBox.getBackground());

    }

    private void choose2DColour(WidgetEvent ev)
    {
	BFrame parent = UIUtilities.findFrame(ev.getWidget());
	
	ColorChooser dlg = new ColorChooser(parent,
					    "choose 2D colour",
					    vector2DColour);

	vector2DPreview.setBackground(vector2DBox.getState()
				    ? vector2DColour.getColor()
				    : vector2DBox.getBackground());

    }

  private void showAdvancedWindow(WidgetEvent ev)
  {
      // set enabled and defaults based on selected mode
      int pos = modeChoice.getSelectedIndex();

      errorField.setEnabled(pos != WIREFRAME);
      adaptiveBox.setEnabled(pos != WIREFRAME);
      proportionBox.setEnabled(pos != WIREFRAME);
      calcTextureBox.setEnabled(pos != WIREFRAME);
      insideChoose.setEnabled(insideBox.getState());
      insidePreview.setBackground(insideBox.getState()
				  ? insideColour.getColor()
				  : insideBox.getBackground());
      
      if (defaultVals) {
	  errorField.setValue(pos == POLYGON ? 0.02 : 0.08);
	  proportionBox.setState(pos == TRIMESH);
	  adaptiveBox.setState(pos == TRIMESH);
	  reflectBox.setState(pos == POLYGON);
      }

      // Record the current settings.
      surfaceError = errorField.getValue();
      lights = lightBox.getState();
      reflections = reflectBox.getState();
      emissive = emissiveBox.getState();
      samplesPerPixel = sampleChoice.getSelectedIndex()+1;
      adaptive = adaptiveBox.getState();
      proportional = proportionBox.getState();
      calcTexture = calcTextureBox.getState();
      vector2D = vector2DBox.getState();
      inside = insideBox.getState();
      insideChoose.setEnabled(inside);

      globalNormal = globalChoice.getSelectedIndex();
      globalVec.set(globalXField.getValue(), globalYField.getValue(),
		    globalZField.getValue());

      globalXField.setEnabled(globalNormal > 0);
      globalYField.setEnabled(globalNormal > 0);
      globalZField.setEnabled(globalNormal > 0);

      vector2DChoose.setEnabled(vector2D);

      visible.set();

      //smoothing = smoothField.getValue();
    
      // Show the window.

      /*
      if (modeChoice.getSelectedIndex() == WIREFRAME) {
	  errorField.setEnabled(false);
	  adaptiveBox.setEnabled(false);
	  proportionBox.setEnabled(false);
      }
      else {
	  errorField.setEnabled(true);
	  adaptiveBox.setEnabled(true);
	  proportionBox.setEnabled(true);
      }

      calcTextureBox.setEnabled(modeChoice.getSelectedIndex == POLYGON);
      */

      RowContainer insidecol = new RowContainer();
      insidecol.add(insideBox);
      insidecol.add(insidePreview);
      insidecol.add(insideChoose);

      RowContainer globalrow = new RowContainer();
      globalrow.add(globalChoice);
      globalrow.add(new BLabel("x"));
      globalrow.add(globalXField);
      globalrow.add(new BLabel("y"));
      globalrow.add(globalYField);
      globalrow.add(new BLabel("z"));
      globalrow.add(globalZField);

      RowContainer vec2drow = new RowContainer();
      vec2drow.add(vector2DBox);
      vec2drow.add(vector2DPreview);
      vec2drow.add(vector2DChoose);

      ColumnContainer boxes = new ColumnContainer();
      boxes.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE,
					    null, null));
      boxes.add(proportionBox);
      boxes.add(adaptiveBox);
      boxes.add(calcTextureBox);
      boxes.add(lightBox);
      boxes.add(emissiveBox);
      boxes.add(reflectBox);
      boxes.add(insidecol);
      boxes.add(vec2drow);
      
      Widget[] widg = new Widget[] {
	  sampleChoice, globalrow, visible.getConfigPanel(), errorField,
	  boxes
      };

      String[] titles = new String[] {
	  Translate.text("supersampling"),
	  Translate.text("globalFace"),
	  Translate.text("show/hide"),
	  Translate.text("surfaceError"),
	  null

      };

      errorField.setEnabled(modeChoice.getSelectedIndex() != 0);

    WindowWidget parent = UIUtilities.findWindow(ev.getWidget());
    ComponentsDialog dlg;
    if (parent instanceof BDialog)
      dlg = new ComponentsDialog((BDialog) parent,
				 Translate.text("advancedOptions"), 
				 widg, titles);
    else
      dlg = new ComponentsDialog((BFrame) parent,
				 Translate.text("advancedOptions"), 
				 widg, titles);

    if (dlg.clickedOk())
	defaultVals = false;
    else {
      // Reset the components.
      
      errorField.setValue(surfaceError);
      lightBox.setState(lights);
      reflectBox.setState(reflections);
      emissiveBox.setState(emissive);
      sampleChoice.setSelectedIndex(samplesPerPixel-1);
      proportionBox.setState(proportional);
      adaptiveBox.setState(adaptive);
      vector2DBox.setState(vector2D);
      calcTextureBox.setState(calcTexture);
      insideBox.setState(inside);

      globalChoice.setSelectedIndex(globalNormal);
      globalXField.setValue(globalVec.x);
      globalYField.setValue(globalVec.y);
      globalZField.setValue(globalVec.z);

      visible.reset();

      //smoothField.setValue(smoothing);
    }
  }

  public boolean recordConfiguration()
  {
      // set defaults based on selected mode
      if (defaultVals) {
	  defaultVals = false;

	  int pos = modeChoice.getSelectedIndex();
	  errorField.setValue(pos == POLYGON ? 0.02 : 0.08);
	  proportionBox.setState(pos == TRIMESH);
	  adaptiveBox.setState(pos == TRIMESH);
	  reflectBox.setState(pos == POLYGON);
      }

      /*
    smoothing = smoothField.getValue();
      */

    surfaceError = errorField.getValue();
    zorder = zorderChoice.getSelectedIndex();
    transparentBackground = transparentBox.getState();
    lights = lightBox.getState();
    reflections = reflectBox.getState();
    emissive = emissiveBox.getState();
    vector2D = vector2DBox.getState();
    calcTexture = calcTextureBox.getState();

    /*
    if (aliasChoice.getSelectedIndex() == 0)
      samplesPerPixel = subsample = 1;
    else if (aliasChoice.getSelectedIndex() == 1)
      samplesPerPixel = subsample = sampleChoice.getSelectedIndex()+2;
    else
      {
    */
    
    renderMode = modeChoice.getSelectedIndex();
    colourScheme = colourChoice.getSelectedIndex();
    strokeWidth = strokeChoice.getSelectedIndex() + 1;
    antialias = aliasChoice.getSelectedIndex();
    samplesPerPixel = sampleChoice.getSelectedIndex()+1;
    adaptive = adaptiveBox.getState();
    proportional = proportionBox.getState();
    inside = insideBox.getState();
    globalNormal = globalChoice.getSelectedIndex();
    globalVec.set(globalXField.getValue(), globalYField.getValue(),
		  globalZField.getValue());

    vectorType = vectorTypeChoice.getSelectedIndex();

    visible.set();

    //subsample = 1;

	/*
      }
    */
    return true;
  }

    
  public Map getConfiguration()
  {
    HashMap map = new HashMap();
    map.put("textureSmoothing", new Double(smoothing));
    map.put("reduceAccuracyForDistant", new Boolean(adaptive));
    map.put("maxSurfaceError", new Double(surfaceError));
    map.put("transparentBackground", new Boolean(transparentBackground));
    int antialiasLevel = 0;
    if (samplesPerPixel == 2)
      antialiasLevel = subsample;
    else if (samplesPerPixel == 3)
      antialiasLevel = (subsample == 1 ? 3 : 4);
    map.put("antialiasing", new Integer(antialiasLevel));
    return map;
  }
  
  public void setConfiguration(String property, Object value)
  {
    if ("textureSmoothing".equals(property))
      smoothing = ((Number) value).doubleValue();
    else if ("reduceAccuracyForDistant".equals(property))
      adaptive = ((Boolean) value).booleanValue();
    else if ("maxSurfaceError".equals(property))
      surfaceError = ((Number) value).doubleValue();
    else if ("transparentBackground".equals(property))
      transparentBackground = ((Boolean) value).booleanValue();
    else if ("antialiasing".equals(property))
    {
      int antialiasLevel = ((Integer) value).intValue();
      switch (antialiasLevel)
      {
        case 0:
          samplesPerPixel = subsample = 1;
          break;
        case 1:
          samplesPerPixel = 2;
          subsample = 1;
          break;
        case 2:
          samplesPerPixel = 2;
          subsample = 2;
          break;
        case 3:
          samplesPerPixel = 3;
          subsample = 1;
          break;
        case 4:
          samplesPerPixel = 3;
          subsample = 3;
          break;
      }
    }
  }

  public void configurePreview()
  {
    colourScheme = 0;
    transparentBackground = false;
    smoothing = 1.0;
    adaptive = proportional = true;
    surfaceError = 0.02;
    samplesPerPixel = subsample = 1;
    lightEffects = false;
    vectorType = 0;
  }
  
  /** Find all the light sources in the scene. */
  
  void findLights()
  {
    Vector lt = new Vector();
    Mat4 trans = theCamera.getWorldToView();
    int i;
    
    positionNeeded = false;
    for (i = 0; i < theScene.getNumObjects(); i++)
      {
        ObjectInfo info = theScene.getObject(i);
        if (info.object instanceof Light && info.visible)
          lt.addElement(info);
      }
    light = new ObjectInfo [lt.size()];
    for (i = 0; i < light.length; i++)
      {
	light[i] = (ObjectInfo) lt.elementAt(i);
	if (!(light[i].object instanceof DirectionalLight))
	  positionNeeded = true;
      }
    //lightPosition = new Vec3 [light.length];
    //lightDirection = new Vec3 [light.length];
  }
  
  /** Main method in which the image is rendered. */
  
  public void run()
  {
    Thread thisThread = Thread.currentThread();
    Vec3 viewdir, orig, center, hvec, vvec;
    Point p;
    ObjectInfo tempObj;
    RenderAtom tempAtom;
    OutputStream out = null;
    boolean done = false;
    int i, j, k;
    double odist;

    if (renderThread != thisThread)
      return;
    updateTime = System.currentTimeMillis();

    // Record information about the scene.

    findLights();
    ambColor = theScene.getAmbientColor();
    envColor = theScene.getEnvironmentColor();
    envMapping = theScene.getEnvironmentMapping();
    envMode = theScene.getEnvironmentMode();
    fogColor = theScene.getFogColor();
    fog = theScene.getFogState();
    fogDist = theScene.getFogDistance();
    for (i = pixel.length-1; i >= 0; i--)
      {
        pixel[i] = 0;
        zbuffer[i] = Float.MAX_VALUE;
      }

    // Determine information about the viewpoint.

    viewdir = theCamera.getViewToWorld().timesDirection(Vec3.vz());
    p = new Point(width/2, height/2);
    orig = theCamera.getCameraCoordinates().getOrigin();
    center = theCamera.convertScreenToWorld(p, focalDist);
    p.x++;
    hvec = theCamera.convertScreenToWorld(p, focalDist).minus(center);
    p.x--;
    p.y++;
    vvec = theCamera.convertScreenToWorld(p, focalDist).minus(center);
    p.y--;
    smoothScale = smoothing*hvec.length()/focalDist;

    // *sigh* I can find no way to set the background transparent, so we just
    // create a new BufferedImage...
    if (renderImg == null || transparentBackground) {
	renderImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	g = (Graphics2D) renderImg.getGraphics();
    }
    else {
	g = (Graphics2D) renderImg.getGraphics();
	g.setColor(Color.black);
	g.fillRect(0, 0, width, height);
    }

    // are we generating vector output?
    switch (vectorType) {
    case 0:
	vecg = null;
	break;

    case 1:
	try {
	    SVGGraphics vg = new SVGGraphics();
	    out = new FileOutputStream(new File(vectorPath, vectorName));

	    if (vectorCompress) out = new GZIPOutputStream(out);

	    vg.open(out, theScene.getName(), imageWidth, imageHeight, true);
	    vecg = vg;
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}
	break;
    }

    if (strokeWidth > 1 || samplesPerPixel > 1)
	g.setStroke(new BasicStroke(strokeWidth*samplesPerPixel));

    if (antialias > 0) {
	g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
			   RenderingHints.VALUE_ANTIALIAS_ON);

	g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
			   RenderingHints.VALUE_STROKE_NORMALIZE);
    }

    // fixed colour schemes force other options
    fixedColour = false;
    if (colourScheme == BLACKWHITE) {
	fixedColour = true;
	lightEffects = false;
	transparentBackground = true;

	g.setColor(Color.white);
	g.fillRect(0, 0, width, height);
	g.setColor(Color.black);

	if (vecg != null) {
	    vecg.setColor(Color.white);
	    vecg.fillRect(0, 0, imageWidth, imageHeight);
	    vecg.setColor(Color.black);
	}
    }
    else if (colourScheme == WHITEBLACK) {
	fixedColour = true;
	lightEffects = false;
	transparentBackground = true;

	g.setColor(Color.black);
	g.fillRect(0, 0, width, height);
	g.setColor(Color.white);

	if (vecg != null) {
	    vecg.setColor(Color.black);
	    vecg.fillRect(0, 0, imageWidth, imageHeight);
	    vecg.setColor(Color.white);
	}
    }

    else {
	if (colourScheme == INCIDENT || colourScheme == REFLECTED)
	    lightEffects = true;

	else lightEffects = (lights || reflections || emissive);

	//if (vecg != null) {
	if (!transparentBackground) {
	    if (envMode == Scene.ENVIRON_SOLID)
		tempColor[0].copy(envColor);
	    else {
		Vec3 dir = tempVec[1];
		double h = (i%width)-width/2.0, v = (i/width)-height/2.0;
		dir.x = center.x + h*hvec.x + v*vvec.x;
		dir.y = center.y + h*hvec.y + v*vvec.y;
		dir.z = center.z + h*hvec.z + v*vvec.z;
		dir.subtract(orig);
		dir.normalize();
		envMapping.getTextureSpec(dir, surfSpec, 1.0, smoothScale, time, null);
		if (envMode == Scene.ENVIRON_DIFFUSE)
		    tempColor[0].copy(surfSpec.diffuse);
		else
		    tempColor[0].copy(surfSpec.emissive);
	    }

	    g.setColor(tempColor[0].getColor());
	    g.fillRect(0, 0, width, height);

	    if (vecg != null) {
		vecg.setColor(tempColor[0].getColor());
		vecg.fillRect(0, 0, imageWidth, imageHeight);
	    }
	}
    }

    // unpack all objects and subobjects
    objlist.clear();
    for (i = theScene.getNumObjects()-1; i >= 0; i--)
	addObject(theScene.getObject(i), objlist);

    //System.out.println("objlist:" + objlist);

    int max = objlist.size();
    if (objarray == null || objarray.length < max) {
	objarray = new ObjectInfo[max];
	dist = new double[max];
    }

    // make a list of the objects, and sort them by distance from the camera
    max = 0;
    for (i = objlist.size()-1; i >= 0; i--) {

	/*
	 * we search the scene's objects from right to left, and insert
	 * into objarray from left to right.
	 */

	tempObj = (ObjectInfo) objlist.get(i);

	//System.out.println("object=" + tempObj.name);

	if (!tempObj.visible ||
	    theCamera.visibility(tempObj.getBounds()) == Camera.NOT_VISIBLE)
	    continue;

	// re-order the objects?
	if (zorder == NONE) {
	    objarray[max++] = tempObj;
	    continue;
	}

	// calculate dist as average of dist to centre, and dist to closest
	// part of bounding box
	tempVec[0].set(tempObj.coords.toLocal().times(orig));
	odist = tempObj.getBounds().distanceToPoint(tempVec[0]);
	//dist[i] = objarray[i].getBounds().distanceToPoint(orig);
	//tempVev[0].set(objarray[i].coords.getOrigin());
	tempVec[1].set(tempObj.getBounds().getCenter());
	tempVec[1].subtract(tempVec[0]);
	odist = (odist + tempVec[1].length()) / 2;

	/*
	  if (objarray[i].name.equals("helmet") ||
	  objarray[i].name.equals("moon"))
	  System.out.println(objarray[i].name + ": " + odist);
	*/

	// move this object left until it is correctly positioned
	for (j = max-1; j > 0 && odist > dist[j]; j--);

	// if we need to move the object, shift the others right and insert
	// at the new location.
	if (j < max-1) {
	    System.arraycopy(objarray, j+1, objarray, j+2, max-j-1);
	    System.arraycopy(dist, j+1, dist, j+2, max-j-1);
	    objarray[j+1] = tempObj;
	    dist[j+1] = odist;
	}
	else {
	    objarray[max] = tempObj;
	    dist[max] = odist;
	}

	max++;
    }

    // Render the objects.
    if (zorder == NONE || zorder == ORDER_OBJECT) {
	RenderAtom atom = new RenderAtom();

	//System.out.println("max=" + max);

	for (i = 0; i < max; i++) {

	    ObjectInfo obj = objarray[i];

	    //System.out.println("obj=" + obj.name);
	    /*
	    theCamera.setObjectTransform(obj.coords.fromLocal());
	    renderObject(obj, orig, viewdir, obj.coords.toLocal());
	    */
	    if (atom.bind(obj, orig, viewdir)) atom.render();
	    //else System.out.println("failed to bind");

	    if (thisThread != renderThread) {
		finish();
		return;
	    }
	    if (System.currentTimeMillis()-updateTime > 5000)
		updateImage();
	}
    }

    // sort the edges/triangles by distance
    else {
	if (atomlist == null || atomlist.length < max)
	    atomlist = new RenderAtom[max];

	tempAtom = null;
	int amax = 0;
	int atom = 0;
	for (i = 0; i < max; i++) {
	    tempAtom = atomlist[atom];
	    if (tempAtom == null) tempAtom = new RenderAtom();
	    if (tempAtom.bind(objarray[i], orig, viewdir)) {
		amax += tempAtom.maxRender;
		atomlist[atom++] = tempAtom;
		tempAtom = null;
	    }
	}

	if (rlist == null || rlist.length < amax) {
	    rlist = new long[amax];
	    rdist = new double[rlist.length];
	}

	//System.out.println("atom=" + atom);

	amax = 0;
	for (i = 0; i < atom; i++) {
	    tempObj = objarray[i];
	    tempAtom = atomlist[i];
	    tempVec[0] = tempAtom.obj.coords.toLocal().times(orig);

	    for (j = tempAtom.maxRender-1; j >= 0; j--) {
		// NTJ: viewdir needs to be transformed to local coords?
		//
		//if (showMode == SHOW_FRONT && tempAtom.from == null &&
		//  tempAtom.faceNorm != null &&
		//  tempAtom.viewdir.dot(tempAtom.faceNorm[j]) > 0.0) {

		//  continue;
		//}

		tempVec[1].set(tempAtom.midPoint(j));
		tempVec[1].subtract(tempVec[0]);
		odist = tempVec[1].length();

		for (k = amax-1; k > 0 && odist > rdist[k]; k--);

		if (k < amax-1) {
		    System.arraycopy(rlist, k+1, rlist, k+2, amax-k-1);
		    System.arraycopy(rdist, k+1, rdist, k+2, amax-k-1);
		    rlist[k+1] = (((long) i) << 32) + j;
		    rdist[k+1] = odist;
		}
		else {
		    rlist[amax] = (((long) i) << 32) + j;
		    rdist[amax] = odist;
		}

		amax++;
	    }
	}

	// render the atoms
	int ax = 0;
	int bx = 0;
	for (i = 0; i < amax; i++) {
	    ax = (int) ((rlist[i] >> 32) & 0xffffffff);
	    bx = (int) (rlist[i] & 0xffffffff);

	    //System.out.println(rlist[i] + " -> ax=" + ax + "; bx=" + bx);

	    tempAtom = atomlist[ax];
	    theCamera.setObjectTransform(tempAtom.obj.coords.fromLocal());
	    visible.bind(tempAtom.obj, tempAtom.viewdir);

	    tempAtom.render(bx);

	    if ((i % 100) == 0 && thisThread != renderThread) {
		finish();
		return;
	    }
	    if (System.currentTimeMillis()-updateTime > 5000)
		updateImage();

	}
    }


    //System.out.println("width=" + renderImg.getData().getWidth() +
    //       "; height=" + renderImg.getData().getHeight() +
    //       "; pixel.length=" + pixel.length);

    //renderImg.getData().getPixels(0, 0, width, height, pixel);

    try {
	int[] data = null;
	PixelGrabber grabber = new PixelGrabber(renderImg, 0, 0, -1, -1, true);
	grabber.grabPixels();
	data = (int []) grabber.getPixels();
	System.arraycopy(data, 0, pixel, 0, data.length);
    }
    catch (InterruptedException ex) {
	return;
    }
    finally {
	g.dispose();
	if (vecg != null) vecg.dispose();
	if (out != null) {
	    try {
		out.close();
	    } catch (Exception e) {}
	}

	vectorType = 0;
    }

    /*
    for (i = 0; i < data.length; i++) {
	if (data[i] == 0xFFFFFFFF)
	    continue;
	int i1 = i/8, i2 = i&7;
	pixel[i1] |= 1<<i2;
    }
    */

    // Apply fog and fill in the background.

    /*
    Vec3 dir = tempVec[1];
    //if (fog)
    if (fog || !transparentBackground)
      for (i = pixel.length-1; i >= 0; i--)
	{
	  float transparency = 1.0f-(((pixel[i] >> 24) & 0xFF)/255.0f);
	  tempColor[0].setARGB(pixel[i]);
	  if (fog && transparency < 1.0f)
	    {
	      float fract1 = (float) Math.exp(-zbuffer[i]/fogDist), fract2 = 1.0f-fract1;
	      tempColor[0].setRGB(fract1*tempColor[0].getRed() + fract2*fogColor.getRed(), 
		fract1*tempColor[0].getGreen() + fract2*fogColor.getGreen(), 
		fract1*tempColor[0].getBlue() + fract2*fogColor.getBlue());
	      transparency *= fract1;
	    }
	  if (!transparentBackground && transparency > 0.0f)
	    {
	      float fract = 1.0f-transparency;
	      if (envMode == Scene.ENVIRON_SOLID)
		tempColor[0].setRGB(fract*tempColor[0].getRed() + transparency*envColor.getRed(), 
		  fract*tempColor[0].getGreen() + transparency*envColor.getGreen(), 
		  fract*tempColor[0].getBlue() + transparency*envColor.getBlue());
	      else
		{
		  double h = (i%width)-width/2.0, v = (i/width)-height/2.0;
		  dir.x = center.x + h*hvec.x + v*vvec.x;
		  dir.y = center.y + h*hvec.y + v*vvec.y;
		  dir.z = center.z + h*hvec.z + v*vvec.z;
                  dir.subtract(orig);
		  dir.normalize();
		  envMapping.getTextureSpec(dir, surfSpec, 1.0, smoothScale, time, null);
		  if (envMode == Scene.ENVIRON_DIFFUSE)
		    tempColor[0].setRGB(fract*tempColor[0].getRed() + transparency*surfSpec.diffuse.getRed(), 
		      fract*tempColor[0].getGreen() + transparency*surfSpec.diffuse.getGreen(), 
		      fract*tempColor[0].getBlue() + transparency*surfSpec.diffuse.getBlue());
		  else
		    tempColor[0].setRGB(fract*tempColor[0].getRed() + transparency*surfSpec.emissive.getRed(), 
		      fract*tempColor[0].getGreen() + transparency*surfSpec.emissive.getGreen(), 
		      fract*tempColor[0].getBlue() + transparency*surfSpec.emissive.getBlue());
		  transparency = 0.0f;
		}
	    }
	  pixel[i] = calcARGB(tempColor[0], transparency);
	}
    */

    createFinalImage();
    finish();
  }

    private void addObject(ObjectInfo obj, ArrayList list)
    {
	Object3D obj3d = obj.object;
	if (obj3d instanceof ObjectCollection) {
	    Enumeration iter = ((ObjectCollection) obj3d).getObjects(obj, false, theScene);
	    while (iter.hasMoreElements())
		addObject((ObjectInfo) iter.nextElement(), list);
	}
	else list.add(obj);
    }

  /** Update the image being displayed. */

  private void updateImage()
  {
    if (imagePixel != pixel)
      {
	for (int i1 = 0, i2 = 0; i1 < imageHeight; i1++, i2 += samplesPerPixel)
	  for (int j1 = 0, j2 = 0; j1 < imageWidth; j1++, j2 += samplesPerPixel)
	    imagePixel[i1*imageWidth+j1] = pixel[i2*width+j2];
      }
    imageSource.newPixels();
    listener.imageUpdated(img);
    updateTime = System.currentTimeMillis();
  }
  
  /** Create the final version of the image. */

  private void createFinalImage()
  {
    int n = samplesPerPixel*samplesPerPixel;
    
    if (imagePixel != pixel)
      {
	for (int i1 = 0, i2 = 0; i1 < imageHeight; i1++, i2 += samplesPerPixel)
	  for (int j1 = 0, j2 = 0; j1 < imageWidth; j1++, j2 += samplesPerPixel)
	    {
	      int a = 0, r = 0, g = 0, b = 0;
	      for (int k = 0; k < samplesPerPixel; k++)
		{
		  int base = width*(i2+k)+j2;
		  for (int m = 0; m < samplesPerPixel; m++)
		    {
		      int color = pixel[base+m];
		      a += (color>>24)&0xFF;
		      r += (color>>16)&0xFF;
		      g += (color>>8)&0xFF;
		      b += color&0xFF;
		    }
		}
	      a /= n;
	      r /= n;
	      g /= n;
	      b /= n;
	      imagePixel[i1*imageWidth+j1] = (a<<24) + (r<<16) + (g<<8) + b;
	    }
        if (depthNeeded)
          for (int i1 = 0, i2 = 0; i1 < imageHeight; i1++, i2 += samplesPerPixel)
            for (int j1 = 0, j2 = 0; j1 < imageWidth; j1++, j2 += samplesPerPixel)
              {
                float minDepth = Float.MAX_VALUE;
                for (int k = 0; k < samplesPerPixel; k++)
                  {
                    int base = width*(i2+k)+j2;
                    for (int m = 0; m < samplesPerPixel; m++)
                      {
                        float z = zbuffer[base+m];
                        if (z < minDepth)
                          minDepth = z;
                      }
                  }
                imageZbuffer[i1*imageWidth+j1] = minDepth;
              }
      }
    imageSource.newPixels();
  }

  /** This routine is called when rendering is finished. */

  private void finish()
  {
    light = null;
    theScene = null;
    theCamera = null;
    envMapping = null;
    renderThread = null;
    RenderListener rl = listener;
    listener = null;
    if (rl != null)
    {
      ComplexImage image = new ComplexImage(img);
      if (depthNeeded)
        image.setComponentValues(ComplexImage.DEPTH, imageZbuffer);
      rl.imageComplete(image);
    }
  }

  /** Given an RGBColor and a transparency value, calculate the ARGB value. */
  
  private int calcARGB(RGBColor color, double t)
  {
    if (!transparentBackground || t <= 0.0)
      return color.getARGB();
    if (t >= 1.0)
      return 0;
    double scale = 255.0/(1.0-t);
    int a, r, g, b;
    a = (int) (255.0*(1.0-t));
    r = (int) (color.getRed()*scale);
    g = (int) (color.getGreen()*scale);
    b = (int) (color.getBlue()*scale);
    if (r < 0) r = 0;
    if (r > 255) r = 255;
    if (g < 0) g = 0;
    if (g > 255) g = 255;
    if (b < 0) b = 0;
    if (b > 255) b = 255;
    return (a<<24) + (r<<16) + (g<<8) + b;
  }

  /** A faster replacement for Math.floor(). */
  
  private static int floor(double d)
  {
    if (d < 0.0)
    {
      int f = (int) d;
      if (f != d)
        f -= 1;
      return f;
    }
    return (int) d;
  }

  /** A faster replacement for Math.ceil(). */
  
  private static int ceil(double d)
  {
    if (d > 0.0)
    {
      int f = (int) d;
      if (f != d)
        f += 1;
      return f;
    }
    return ((int) d);
  }
  
  /** A faster replacement for Math.round(). */
  
  private static int round(double d)
  {
    return floor(d+0.5);
  }
  
    private boolean checkEdge(int p1, int p2)
    {
	int size = 6;

	//System.out.println("checkEdge: p1=" + p1 + "; p2=" + p2);

	for (int j = 0; j < size; j++)
	    if (vertices[p1*size+j] == p2 || vertices[p2*size+j] == p1)
		return false;

	//System.out.println("edge not found, updating vertices");

	for (int j = 0; j < size; j++) {
	    if (vertices[p1*size+j] < 0) {
		vertices[p1*size+j] = p2;

		//System.out.println("vertices[" + (p1*size+j) + "]=" + p2);
		return true;
	    }

	    if (vertices[p2*size+j] < 0) {
		vertices[p2*size+j] = p1;

		//System.out.println("vertices[" + (p2*size+j) + "]=" + p1);
		return true;
	    }
	}

	System.out.println("checkEdge: vertices array overflow. p1=" + p1 +
			   "; p2=" + p2);
	return true;
    }

    /**
     *  fill a polygon
     */
    private void fillPolygon(Graphics g, Vec3[] points, int count,
			     boolean outline)
    {
	double w;
	int x, y;
	final Mat4 m = theCamera.getObjectToScreen();
	Vec3 pt;

	for (int i = 0; i < count; i++) {
	    pt = points[i];

	    w = m.m41*pt.x + m.m42*pt.y + m.m43*pt.z + m.m44;
	    polyx[i] = (int)((m.m11*pt.x + m.m12*pt.y + m.m13*pt.z + m.m14)/w);
	    polyy[i] = (int)((m.m21*pt.x + m.m22*pt.y + m.m23*pt.z + m.m24)/w);
	}

	if (outline) g.drawPolygon(polyx, polyy, count);
	g.fillPolygon(polyx, polyy, count);
    }

  /**
   *  Calculate the lighting model at a point on a surface. If either diffuse
   *  or specular is null, the component will not be calculated.
   *
   *  NTJ: modified to handle parallel rather than normal
   */


    /**
     *  class to hold details of a render atom
     */

    protected class RenderAtom
    {
	ObjectInfo obj;
	Mat4 toLocal;
	TextureSpec spec = new TextureSpec();
	double tol=TOL;
	int vis, maxRender, from[], to[], index[], last;
	Vec3 viewdir, lightPosition[], lightDirection[],
	    vert[], norm[]=null, faceNorm[]=null, tempResult = new Vec3();
	Vec3 p1=tempVec[0], p2=tempVec[1];
	RenderingTriangle triangle[] = null;
	RGBColor colour = null;
	boolean cleanup = false;

	/**
	 *  bind this atom to the specified view of the specified object
	 */
	boolean bind(ObjectInfo obj, Vec3 orig, Vec3 viewdir)
	{
	    RenderingTriangle tri = null;
	    int i, j, k;
	    double odist;

	    if (cleanup) {
		from = null;
		to = null;
		vert = null;
		cleanup = false;
	    }

	    triangle = null;
	    norm = null;
	    faceNorm = null;

	    this.obj = obj;
	    toLocal = obj.coords.toLocal();
	    vis = theCamera.visibility(obj.getBounds());

	    last = -1;

	    if (renderMode != WIREFRAME) {
		if (proportional) {
		    Vec3 size = obj.getBounds().getSize();
		    double maxSize = Math.max(size.x, Math.max(size.y, size.z));
		    tol = maxSize * surfaceError;
		}
		else 
		    tol = surfaceError;

		if (adaptive) {
		    double dist = obj.getBounds().distanceToPoint(toLocal.times(orig));
		    double distToScreen = theCamera.getDistToScreen();
		    if (dist >= distToScreen)
			tol *= dist/(4*distToScreen);
		}
	    }

	    // get the texture properties
	    Texture tex = obj.object.getTexture();
	    if (tex == null) {
		//System.out.println("skip " + obj.object.getClass().getName());
		if (vector2D && obj.object.getClass() == Curve.class)
		    spec.diffuse.copy(vector2DColour);
		else return false;
	    }

	    switch (renderMode) {
	    case WIREFRAME:
	    {
		if (tex != null) tex.getAverageSpec(spec, time, null);

		WireframeMesh wiremesh = obj.getWireframePreview();
		if (wiremesh == null) return false;

		from = wiremesh.from;
		to = wiremesh.to;
		vert = wiremesh.vert;
		maxRender = from.length;
		
		cleanup = true;
	    }
	    break;

	    case TRIMESH:
	    {
		if (calcTexture == false && tex != null)
		    tex.getAverageSpec(spec, time, null);

		RenderingMesh rendermesh = obj.getRenderingMesh(tol);
		if (rendermesh == null) {
		    WireframeMesh wiremesh = obj.getWireframePreview();
		    if (wiremesh == null) return false;

		    from = wiremesh.from;
		    to = wiremesh.to;
		    vert = wiremesh.vert;
		    maxRender = from.length;

		    cleanup = true;
		}
		else {
		    vert = rendermesh.vert;
		    triangle = rendermesh.triangle;

		    //System.out.println("triangles=" + triangle.length);
		    
		    if (from == null || from.length < triangle.length*3) {
			from = new int[triangle.length*3];
			to = new int[from.length];
			index = new int[from.length];
		    }

		    if (vertices == null || vertices.length < from.length*2)
			vertices = new int[from.length*2];

		    cleanup = false;

		    norm = rendermesh.norm;
		    faceNorm = rendermesh.faceNorm;

		    // setup the vertices array, which we use to avoid drawing
		    // a line multiple times
		    for (i = vertices.length-1; i >= 0; i--) vertices[i] = -1;

		    //System.out.println("from=" + from.length + ";vertices=" +
		    //   vertices.length);

		    maxRender = 0;
		    for (i = triangle.length-1; i >= 0; i--) {
			tri = triangle[i];
			
			if (checkEdge(tri.v1, tri.v2)) {
			    from[maxRender] = tri.v1;
			    to[maxRender] = tri.v2;
			    index[maxRender] = i;

			    maxRender++;
			}

			if (checkEdge(tri.v1, tri.v3)) {
			    from[maxRender] = tri.v1;
			    to[maxRender] = tri.v3;
			    index[maxRender] = i;

			    maxRender++;

			}

			if (checkEdge(tri.v2, tri.v3)) {
			    from[maxRender] = tri.v2;
			    to[maxRender] = tri.v3;
			    index[maxRender] = i;

			    maxRender++;
			}
		    }
		}
		//System.out.println("bind: maxRender=" + maxRender);
	    }
	    break;

	    case POLYGON:
	    {
		if (calcTexture == false && tex != null)
		    tex.getAverageSpec(spec, time, null);

		RenderingMesh rendermesh = obj.getRenderingMesh(tol);
		if (rendermesh == null) return false;

		vert = rendermesh.vert;
		triangle = rendermesh.triangle;
		norm = rendermesh.norm;
		faceNorm = rendermesh.faceNorm;

		cleanup = true;

		maxRender = triangle.length;

		if (polyx == null) {
		    polyx = new int[3];
		    polyy = new int[3];
		}
	    }
	    break;
	    }

	    lightPosition = new Vec3[light.length];

	    this.viewdir = toLocal.timesDirection(viewdir);
	    for (i = light.length-1; i >= 0; i--) {
		lightPosition[i] = toLocal.times(light[i].coords.getOrigin());
		if (!(light[i].object instanceof PointLight)) {
		    if (lightDirection == null)
			lightDirection = new Vec3[light.length];

		    lightDirection[i] = toLocal.timesDirection(light[i].coords.getZDirection());
		}
	    }

	    if (colourScheme == RANDOM) {
		colour = new RGBColor(Math.random(), Math.random(), Math.random());
		float bright = colour.getBrightness();
		if (bright > 0.5) colour.scale(0.9);
		else colour.scale(1.1);
	    }

	    return true;
	}

	/**
	 *  return the midpoint for the specified render atom.
	 */
	Vec3 midPoint(int i)
	{
	    try {
		if (renderMode == POLYGON) {
		    RenderingTriangle tri = triangle[i];

		    tempResult.set(vert[tri.v1]);
		    tempResult.add(vert[tri.v2]);
		    tempResult.add(vert[tri.v3]);
		    tempResult.scale(1.0/3);
		}
		else {
		    tempResult.set(vert[from[i]]);
		    tempResult.add(vert[to[i]]);
		    tempResult.scale(0.5);
		}
	    } catch (Exception e) {
		System.out.println("midPoint(): " + e);
		System.out.println("object: " + obj.name + "; maxRender=" +
				   maxRender + "; from.length=" + from.length +
				   "; triangle.length=" +
				   (triangle != null ? triangle.length : -1));
				   
		e.printStackTrace(System.out);
		
	    }

		return tempResult;
	}

	/**
	 *  render the entire set of atoms.
	 */
	void render()
	{
	    last = -1;
	    theCamera.setObjectTransform(obj.coords.fromLocal());
	    visible.bind(obj, viewdir);
	    for (int x = maxRender-1; x >= 0; x--) last = render(x);
	}

	/**
	 *  render the specified atom.
	 */
	int render(int i)
	{

	    //System.out.println("RenderAtom.render: i=" + i +
	    //       "; name=" + obj.name +
	    //       "; from=" + from + "; to=" + to);

	    // work out the show/hide direction
	    //tempVec[0].set(toLocal.timesDirection(showDir));
	    //tempVec[0].set(showDir);
	    //tempVec[0].multiply(showDir);
	    //tempVec[0].normalize();
	    //tempVec[0].set(toLocal.timesDirection(tempVec[0]));

	    float bright = 0;
	    double visibility = 0;
	    RenderingTriangle tri = null;

	    if (renderMode == POLYGON) {
		tri = triangle[i];

		midPoint.set(vert[tri.v1]);
		midPoint.add(vert[tri.v2]);
		midPoint.add(vert[tri.v3]);
		midPoint.scale(1.0/3);

		//System.out.println("tri=" + vert[tri.v1] + ", " + vert[tri.v2] +
		//	       ", " + vert[tri.v3] + "; midpoint=" + midPoint);

		//tempVec[2].set(norm[tri.n1]);
		//tempVec[2].add(norm[tri.n2]);
		//tempVec[2].add(norm[tri.n3]);
		//tempVec[2].normalize();
		//tempVec[2].scale(1.0/3);

		tempVec[2] = faceNorm[i];
		tempVec[3] = faceNorm[i];

		//tempVec[0].set(centre);
		//tempVec[0].subtract(midPoint);

		//System.out.println("viewdir.normal=" + viewdir.dot(tempVec[2]) +
		//       "; viewdir.radius=" + viewdir.dot(tempVec[0]) +
		//		       "\n");

		// NTJ: for show calculations. Does this break calcLight?
		tempVec[2].normalize();

		visibility = visible.visibility(midPoint, tempVec[2],
					       Visibility.FRONT);
	    }
	    else {
		p1 = vert[from[i]];
		p2 = vert[to[i]];

		midPoint.set(p1);
		midPoint.add(p2);
		midPoint.scale(0.5);

		if (triangle != null) {
		    tri = triangle[index[i]];

		    /*
		    if (from[i] == tri.v1)
			tempVec[2].set(norm[tri.n1]);
		    else if (from[i] == tri.v2)
			tempVec[2].set(norm[tri.n2]);
		    else if (from[i] == tri.v3)
			tempVec[2].set(norm[tri.n3]);

		    if (to[i] == tri.v1)
			tempVec[2].add(norm[tri.n1]);
		    else if (to[i] == tri.v2)
			tempVec[2].add(norm[tri.n2]);
		    else if (to[i] == tri.v3)
			tempVec[2].add(norm[tri.n3]);

		    tempVec[2].normalize();
		    */

		    tempVec[2] = faceNorm[index[i]];
		    tempVec[3] = faceNorm[index[i]];

		    tempVec[2].normalize();
		}

		// normal not supplied - so calculate a parallel for the line
		else {
		    // unit vector parallel to the line
		    parallel.set(p1);
		    parallel.subtract(p2);
		    parallel.normalize();

		    /*
		    // one (of many) normals to the wire
		    tempVec[2].set(centre);
		    tempVec[2].subtract(midPoint);
		    tempVec[2].normalize();
		    */

		    // NTJ: see if this fixes some normal flipping
		    tempVec[2].set(midPoint);
		    tempVec[2].subtract(centre);
		    tempVec[2].normalize();
		}

		//System.out.println("showPct=" + showPct +
		//	   "; showDir=" + showDir +
		//	   "; norm=" + tempVec[2] +
		//	   "; dir.dot=" + tempVec[0].dot(tempVec[2]));

		visibility = visible.visibility(midPoint, tempVec[2],
					       Visibility.ALL);

		//if (showBackfaces && viewdir.dot(tempVec[2]) >= 0.0)
		// return -1;
	    }

	    if (visibility <= 0.0) return -1;

	    if (globalNormal == NORMAL_PARALLEL)
		tempVec[2].set(globalVec);

	    else if (globalNormal == NORMAL_SPHERICAL) {
		tempVec[2].set(toLocal.times(globalVec));
		tempVec[2].subtract(midPoint);
		tempVec[2].normalize();
	    }

	    // get the surface spec from the triangle
	    if (calcTexture && tri != null) 
		tri.getTextureSpec(surfSpec, viewdir.dot(tempVec[2]),
				   midPoint.x, midPoint.y, midPoint.z,
				   tol, time);

	    // get it from spec
	    else {
		surfSpec.diffuse.copy(spec.diffuse);
		surfSpec.emissive.copy(spec.emissive);
		surfSpec.specular.copy(spec.specular);
		surfSpec.transparent.copy(spec.transparent);
		surfSpec.hilight.copy(spec.hilight);
		surfSpec.bumpGrad.set(spec.bumpGrad);
		surfSpec.roughness = spec.roughness;
	    }
	    

	    //System.out.println("[" + i + "] from=" + vert[from[i]] +
	    //	   "; to=" + vert[to[i]] + "; mid=" + midPoint +
	    //	   "; norm=" + norm);

	    Object o = null;
	    if (!fixedColour) {

		if (colours != null) {
		    o = colours.get(obj);
		    if (o == null) {
			o = colours.get(p1);
			if (o != null && o.getClass() == HashMap.class)
			    o = ((HashMap) o).get(p2);
		    }
		}

		if (inside && viewdir.dot(tempVec[2]) >= 0.0)
		    tempColor[1].copy(insideColour);

		else if (o != null)
		    tempColor[1].copy((RGBColor) o);

		else if (colourScheme == RANDOM)
		    tempColor[1].copy(tempColor[2]);

		else if (colourScheme == WHATEVER) {
		    tempColor[1].setRGB(Math.random(), Math.random(), Math.random());
		    bright = tempColor[1].getBrightness();
		    if (bright > 0.5) tempColor[1].scale(0.9);
		    else tempColor[1].scale(1.1);
		}
		else tempColor[1].copy(surfSpec.diffuse);

		// calculate the color for this segment
		if (lightEffects) {

		    // calculate the light based on the normal
		    if (globalNormal > 0)
			calcLight(midPoint, tempVec[2], viewdir, null,
				  tempVec[2], surfSpec.roughness, diffuse,
				  specular);

		    else if (tri != null)
			calcLight(midPoint, tempVec[2], viewdir, null,
				  tempVec[3], surfSpec.roughness, diffuse,
				  specular);

		    else
			calcLight(midPoint, null, viewdir, parallel,
				  tempVec[2], surfSpec.roughness, diffuse,
				  specular);
		
		    switch(colourScheme) {

		    case INCIDENT:
			//g.setColor(diffuse.getColor());
			tempColor[1].copy(diffuse);
			break;

		    case REFLECTED:
			//g.setColor(specular.getColor());
			tempColor[1].copy(specular);
			break;

		    default:
			specular.multiply(surfSpec.specular);
			//tempColor[1].scale(2.0);
			if (lights) tempColor[1].multiply(diffuse);
			if (reflections) tempColor[1].add(specular);
			if (emissive) tempColor[1].add(surfSpec.emissive);
			//g.setColor(tempColor[1].getColor());
			break;
			//g.setColor(surfSpec.diffuse.getColor());
		    }
		}
		//else g.setColor(tempColor[1].getColor());
	    }

	    if (colourScheme == GREYSCALE) {
		grey.setRGB(1.0, 1.0, 1.0);
		grey.scale(tempColor[1].getBrightness());
		//g.setColor(grey.getColor());
		tempColor[1].copy(grey);
	    }

	    g.setColor(new Color(Math.min(Math.max(tempColor[1].red, 0), 1),
				 Math.min(Math.max(tempColor[1].green, 0), 1),
				 Math.min(Math.max(tempColor[1].blue, 0), 1),
				 (float) visibility));

	    if (vecg != null) vecg.setColor(g.getColor());

	    //System.out.println("rendering...");

	    // rendering a polygon
	    if (renderMode == POLYGON) {
		poly[0] = vert[tri.v1];
		poly[1] = vert[tri.v2];
		poly[2] = vert[tri.v3];
	    
		fillPolygon(g, poly, 3, true);
		if (vecg != null) fillPolygon(vecg, poly, 3, false);
		return -1;
	    }

	    if (vis == Camera.NEEDS_CLIPPING) {
		if (from[i] == last)
		    theCamera.drawClippedLineTo(g, p2);
		else
		    theCamera.drawClippedLine(g, p1, p2);
		// we're using the same camera as above, so we must supply from,to
		if (vecg != null)
		    theCamera.drawClippedLine(vecg, p1, p2);
	    }
	    else {
		if (from[i] == last)
		    theCamera.drawLineTo(g, p2);
		else 
		    theCamera.drawLine(g, p1, p2);

		// we're using the same camera as above, so we must supply from,to
		if (vecg != null)
		    theCamera.drawLine(vecg, p1, p2);
	    }

	    //last = to[i];
	    return to[i];
	}

	/**
	 * Calculate the lighting model at a point on a surface.
	 *
	 * If either diffuse or specular is null, then that component will not
	 * be calculated.
	 *
	 * NTJ: based on Raster.calcLight(). Added the <code>parallel</code>
	 * argument for approximated normals of wireframes.
	 */
  
	private void calcLight(Vec3 pos, Vec3 norm, Vec3 viewdir,
			       Vec3 parallel, Vec3 faceNorm, double roughness,
			       RGBColor diffuse, RGBColor specular)
	{
	    Vec3 reflectDir = tempVec[0], lightDir = tempVec[1];
	    double viewDot=0.0, faceDot=0.0, ddot=0.0;
	    float fdot=0.0f;
	    //double viewDot = viewdir.dot(norm), faceDot = viewdir.dot(faceNorm);
	    RGBColor outputColor = tempColor[0];
    
	    if (diffuse != null)
		diffuse.copy(ambColor);

	    if (specular != null && norm != null) {
		viewDot = viewdir.dot(norm);
		if (faceNorm != null) faceDot = viewdir.dot(faceNorm);

		if (envMode == Scene.ENVIRON_SOLID)
		    specular.copy(envColor);
		else {
		    // Find the reflection direction and add in the
		    // environment color

		    reflectDir.set(norm);
		    reflectDir.scale(-2.0*viewDot);
		    reflectDir.add(viewdir);
		    theCamera.getViewToWorld().transformDirection(reflectDir);
		    envMapping.getTextureSpec(reflectDir, surfSpec2, 1.0,
					      smoothScale, time, null);
		    if (envMode == Scene.ENVIRON_DIFFUSE)
			specular.copy(surfSpec2.diffuse);
		    else
			specular.copy(surfSpec2.emissive);
		}
	    }
    
	    // Prevent artifacts where the triangle is facing toward the
	    // viewer, but the local interpolated normal is facing away.
	    if (viewDot < 0.0 && faceDot > 0.0)
		viewDot = TOL;
	    else if (viewDot > 0.0 && faceDot < 0.0)
		viewDot = -TOL;

	    // Loop over the lights and add in each one.
	    for (int i = light.length-1; i >= 0; i--) {
		Light lt = (Light) light[i].object;
		Vec3 lightPos = lightPosition[i];
		double distToLight = 0.0, fatt = 0.0, lightDot = 0.0;

		if (lt instanceof PointLight) {
		    lightDir.set(pos);
		    lightDir.subtract(lightPos);
		    distToLight = lightDir.length();
		    lightDir.normalize();
		}
		else if (lt instanceof SpotLight) {
		    lightDir.set(pos);
		    lightDir.subtract(lightPos);
		    distToLight = lightDir.length();
		    lightDir.normalize();
		    fatt = lightDir.dot(lightDirection[i]);
		    if (fatt < ((SpotLight) lt).getAngleCosine())
			continue;
		}
		else if (lt instanceof DirectionalLight)
		    lightDir.set(lightDirection[i]);

		//lt.getLight(outputColor, (float) distToLight); // Error
            lt.getLight(outputColor, new Vec3(distToLight,distToLight,distToLight)); // Error
            
            
		if (lt instanceof SpotLight)
		    outputColor.scale(Math.pow(fatt,
					       ((SpotLight)lt).getExponent()));
		if (lt.getType() == Light.TYPE_AMBIENT) {
		    if (diffuse != null)
			diffuse.add(outputColor.getRed(),
				    outputColor.getGreen(),
				    outputColor.getBlue());
		    continue;
		}

		// NTJ: new algorith from Peter Eastman uses parallel
		if (norm != null) {
		    lightDot = lightDir.dot(norm);

		    //System.out.println("lightdot=" + lightDot);

		    if ((lightDot >= 0.0 && viewDot <= 0.0)
			|| (lightDot <= 0.0 && viewDot >= 0.0))
			
			continue;
		}
		else lightDot = lightDir.dot(parallel);

		if (diffuse != null) {
		    if (norm != null)
			fdot = (float) (lightDot < 0.0 ? -lightDot : lightDot);
		    else
			fdot = (float) Math.sqrt(1 - (lightDot*lightDot));

		    //System.out.println("fdot=" + fdot);

		    diffuse.add(outputColor.getRed()*fdot,
				outputColor.getGreen()*fdot,
				outputColor.getBlue()*fdot);
		}
		if (specular != null) {
		    lightDir.add(viewdir);
		    lightDir.normalize();

		    if (norm != null) {
			ddot = lightDir.dot(norm);
			ddot = (ddot < 0.0 ? -ddot : ddot);
		    }
		    else {
			ddot = lightDir.dot(parallel);
			ddot = (double) Math.sqrt(1 - (ddot*ddot));
		    }
		  
		    outputColor.scale(Math.pow(ddot,
					       (1.0-roughness)*128.0+1.0));
		    specular.add(outputColor);
		}
	    }
	}


    }

}
