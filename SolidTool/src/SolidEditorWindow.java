/*  SolidEditor.java  */

package armadesignstudio;

/*
 * Copyright (C) 2005 by Nik Trevallyn-Jones
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

import armadesignstudio.*;
import armadesignstudio.ui.*;
import armadesignstudio.math.*;
import armadesignstudio.object.*;
import armadesignstudio.animation.*;

import buoy.event.*;
import buoy.widget.*;

import java.text.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import javax.swing.SpinnerNumberModel;

/**
 * Editor for Solids.
 *
 * Derived TriMeshEditorWindow with added features for making the mesh a solid.
 */

public class SolidEditorWindow extends TriMeshEditorWindow
{
    protected Writer message;
    protected HashMap symbol;
    protected Vec3 s1 = new Vec3(), s2 = new Vec3(), s3 = new Vec3();
    protected boolean topology, sel[], statsOpen = true, safety = true;
    protected int map[], selection[], currSelection, maxSelection, lock = 0;
    protected double tol = 1.0e-12;

    protected static final int findSelected	= 0;
    protected static final int findAll		= 1;

    /**
     * set to not conflict (hopefully) with the values in TriMeshEditorWindow
     */    
    protected static final int ALL_FEATURES	= -1;

    protected static final String showCmd = "show-shotgun";
    protected static final String hideCmd = "hide-shotgun";

    BMenu solidMenu;
    BMenuItem[] solidMenuItem;

    BFrame stats;

    static final int POS_INT = ValueField.POSITIVE | ValueField.INTEGER;
    static final int NONEG_INT = ValueField.NONNEGATIVE | ValueField.INTEGER;
 
    ValueField pointField = new ValueField(0, NONEG_INT, 6),
	edgeField = new ValueField(0, NONEG_INT, 6),
	faceField = new ValueField(0, NONEG_INT, 6),
	totalPointField = new ValueField(0, NONEG_INT, 6),
	totalEdgeField = new ValueField(0, NONEG_INT, 6),
	totalFaceField = new ValueField(0, NONEG_INT, 6),

	significantField = new ValueField(12, POS_INT, 2),

	//selIndex = new ValueField(-1, NONEG_INT, 5),
	selField = new ValueField(0, NONEG_INT, 5),

	xField = new ValueField(0, 0, 6),
	yField = new ValueField(0, 0, 6),
	zField = new ValueField(0, 0, 6),

	f1Field = new ValueField(0, ValueField.INTEGER, 6),
	f2Field = new ValueField(0, ValueField.INTEGER, 6),

	v1Field = new ValueField(0, NONEG_INT, 6),
	v2Field = new ValueField(0, NONEG_INT, 6),
	v3Field = new ValueField(0, NONEG_INT, 6),
	v4Field = new ValueField(0, NONEG_INT, 6),
	v5Field = new ValueField(0, NONEG_INT, 6),

	e1Field = new ValueField(0, NONEG_INT, 6),
	e2Field = new ValueField(0, NONEG_INT, 6),
	e3Field = new ValueField(0, NONEG_INT, 6);


    BTextArea messageArea = new BTextArea(10, 45);
    BScrollPane scroll = new BScrollPane(BScrollPane.SCROLLBAR_AS_NEEDED,
					 BScrollPane.SCROLLBAR_AS_NEEDED);

    BSpinner selIndex = new BSpinner(new SpinnerNumberModel());

    RowContainer selrow = new RowContainer();
    ColumnContainer editCont = new ColumnContainer();

    BLabel selLabel = new BLabel();

    FormContainer pointCont;
    FormContainer edgeCont;
    FormContainer faceCont;

    RowContainer editButtons = new RowContainer();

    BButton hideButton = Translate.button("hide", this, "shotgun");
    BButton saveButton = Translate.button("Save", this, "save");
    BButton resetButton = Translate.button("Undo", this, "reset");
    BButton safetyButton = Translate.button("Unlock", this, "safety");

    BMenuItem shotgunItem;

    NumberFormat nf = NumberFormat.getInstance(Locale.US);

    static final String pointLabel = Translate.text("Point");
    static final String edgeLabel = Translate.text("Edge");
    static final String faceLabel = Translate.text("Face");

    static final String UNLOCK = Translate.text("Unlock");
    static final String LOCK = Translate.text("Lock");

    //static final int INVALID = -1;
    static final Integer INVALID = new Integer(-1);

    /**
     *  Ctor - construct a new SolidEditorWindow
     */
    public SolidEditorWindow(EditingWindow parent, String title,
			     ObjectInfo obj, Runnable onClose,
			     boolean allowTopology, Writer message)
    {
	super(parent, title +" '"+obj.name+"'", obj, onClose, allowTopology);

	this.message = message;

	topology = allowTopology;

	createSolidMenu((TriangleMesh) obj.object);

	stats = new BFrame(Translate.text("Solid Shotgun"));

	addEventLink(WidgetWindowEvent.class, this, "active");

	pointField.setEditable(false);
	edgeField.setEditable(false);
	faceField.setEditable(false);
	totalPointField.setEditable(false);
	totalEdgeField.setEditable(false);
	totalFaceField.setEditable(false);
	selField.setEditable(false);

	ColumnContainer cols = new ColumnContainer();
	RowContainer err = new RowContainer();
	FormContainer form = new FormContainer(3, 4);
	RowContainer buttons = new RowContainer();

	pointCont = new FormContainer(2, 3);
	edgeCont = new FormContainer(2, 4);
	faceCont = new FormContainer(2, 6);

	cols.add(form);
	cols.add(err);
	cols.add(editCont);
	cols.add(scroll);
	cols.add(buttons);

	err.add(Translate.label("significant digits"));
	err.add(significantField);

	form.add(new BLabel(""), 0, 0);
	form.add(Translate.label("irregular"), 1, 0);
	form.add(Translate.label("/ total"), 2, 0);

	form.add(new BLabel(Translate.text("points")), 0, 1);
	form.add(pointField, 1, 1);
	form.add(totalPointField, 2, 1);
	
	form.add(new BLabel(Translate.text("edges")), 0, 2);
	form.add(edgeField, 1, 2);
	form.add(totalEdgeField, 2, 2);

	form.add(new BLabel(Translate.text("faces")), 0, 3);
	form.add(faceField, 1, 3);
	form.add(totalFaceField, 2, 3);

	selrow.add(selLabel);
	selrow.add(selIndex);
	selrow.add(selField);

	buttons.add(hideButton);
	buttons.add(safetyButton);

	editButtons.add(saveButton);
	editButtons.add(resetButton);

	pointCont.add(Translate.label("x "), 0, 0);
	pointCont.add(xField, 1, 0);
	pointCont.add(Translate.label("y "), 0, 1);
	pointCont.add(yField, 1, 1);
	pointCont.add(Translate.label("z "), 0, 2);
	pointCont.add(zField, 1, 2);

	edgeCont.add(Translate.label("point "), 0, 0);
	edgeCont.add(v1Field, 1, 0);
	edgeCont.add(Translate.label("point "), 0, 1);
	edgeCont.add(v2Field, 1, 1);
	edgeCont.add(Translate.label("face "), 0, 2);
	edgeCont.add(f1Field, 1, 2);
	edgeCont.add(Translate.label("face "), 0, 3);
	edgeCont.add(f2Field, 1, 3);

	faceCont.add(Translate.label("point "), 0, 0);
	faceCont.add(v3Field, 1, 0);
	faceCont.add(Translate.label("point "), 0, 1);
	faceCont.add(v4Field, 1, 1);
	faceCont.add(Translate.label("point "), 0, 2);
	faceCont.add(v5Field, 1, 2);
	faceCont.add(Translate.label("edge "), 0, 3);
	faceCont.add(e1Field, 1, 3);
	faceCont.add(Translate.label("edge "), 0, 4);
	faceCont.add(e2Field, 1, 4);
	faceCont.add(Translate.label("edge "), 0, 5);
	faceCont.add(e3Field, 1, 5);

	messageArea.setEditable(false);

	hideButton.setActionCommand(hideCmd);

	significantField.addEventLink(ValueChangedEvent.class, this, "values");
	selIndex.addEventLink(ValueChangedEvent.class, this, "values");

	stats.setContent(cols);
	stats.pack();
	stats.setVisible(true);

	nf.setMaximumFractionDigits((int) significantField.getValue());
	nf.setGroupingUsed(false);

	TriangleMesh mesh = (TriangleMesh) objInfo.object;

	int max = mesh.getFaces().length*3;

	symbol = new HashMap(max);
	map = new int[mesh.getVertices().length];
	sel = new boolean[max];

	//findIrregular(sel, ALL_FEATURES);
	validate();
    }

    /**
     *  overridden dispose event
     */
    public void dispose()
    {
	if (stats != null) {
	    stats.setVisible(false);
	    stats.dispose();
	}

	stats = null;

	super.dispose();
    }

    /**
     *  overridden from MeshEditorWindow
     *
     *  local processing whenever the selection changes.
     *  Calls findIrregular to update the shotgun window.
     */
    public void setSelection(boolean[] select)
    {
	super.setSelection(select);

	if (lock <= 0) {
	    if (sel == null || (sel != select && sel.length < select.length)) {
		sel = new boolean[select.length];
	    }

	    System.arraycopy(select, 0, sel, 0, select.length);
	    for (int x = select.length; x < sel.length; x++) sel[x] = false;

	    // clear irregular selection
	    clearSelect();
	    findIrregular(sel, ALL_FEATURES);
	}
    }

    /**
     *  overridden keyPressed from TriMeshEditorWindow
     */
    protected void keyPressed(KeyPressedEvent ev)
    {
	switch (ev.getKeyCode()) {
	case KeyPressedEvent.VK_PAGE_DOWN:
	    changeSelected(1);
	    break;

	case KeyPressedEvent.VK_PAGE_UP:
	    changeSelected(-1);
	    break;

	default:
	    super.keyPressed(ev);
	}
    }

    /**
     *  respond to active events
     */
    public void active(WidgetWindowEvent ev)
    {
	//System.out.println("active: id=" + ev.getID());

	switch (ev.getID()) {
	case WidgetWindowEvent.WINDOW_ACTIVATED:
	case WidgetWindowEvent.WINDOW_GAINED_FOCUS:
	case WidgetWindowEvent.WINDOW_DEICONIFIED:
	    if (statsOpen == true && stats != null && !stats.isVisible()) {
		stats.setVisible(true);
		setVisible(true);
	    }
	    break;

	case WidgetWindowEvent.WINDOW_ICONIFIED:
	    if (stats != null) stats.setVisible(false);
	    break;

	case WidgetWindowEvent.WINDOW_CLOSED:
	case WidgetWindowEvent.WINDOW_CLOSING:
	    if (stats != null) {
		stats.setVisible(false);
		stats.dispose();
	    }
	    stats = null;
	    break;

	default:
	}
    }

    /**
     *  update the GUI values as they change
     */
    public void values(ValueChangedEvent ev)
    {
	nf.setMaximumFractionDigits((int) significantField.getValue());
	tol = 1*Math.pow(10, -significantField.getValue());

	//int newSel = (int) selIndex.getValue() - currSelection;
	int newSel = ((Number) selIndex.getValue()).intValue() - currSelection;
	if (newSel != 0) changeSelected(newSel);

	//System.out.println("tol=" + tol + "; newSel=" + newSel);
    }

    /**
     *  fix all features command.
     *
     *  call fix() with ALL_FEATURES
     */
    public void fixAllCommand()
    { fix(ALL_FEATURES); }

    /**
     *  fix points command.
     *
     *  call fix with POINT_MODE
     */
    public void fixPointCommand()
    { fix(POINT_MODE); }

    /**
     *  fix edges command.
     *
     *  calls fix with EDGE_MODE
     */
    public void fixEdgeCommand()
    { fix(EDGE_MODE); }

    /**
     *  fix faces command.
     *
     *  calls fix with FACE_MODE
     */
    public void fixFaceCommand()
    { fix(FACE_MODE); }

    /**
     *  fix irregular features.
     *
     *  identify and delete:
     *<br>degenerate faces
     *<br>duplicate edges
     *<br>degenerate edges
     *<br>duplicate vertices
     *
     *@param mode integer mode to specify which feature(s) to fix:
     *  may be one of: POINT_MODE, EDGE_MODE, FACE_MODE, ALL_FEATURES.
     */
    public void fix(int mode)
    {
	try {
	    lock++;

	    int currMode = super.getSelectionMode();
	    boolean[] currSel = super.getSelection();

	    TriangleMesh mesh = (TriangleMesh) objInfo.object;
	    TriangleMesh.Vertex[] vert =
		(TriangleMesh.Vertex[]) mesh.getVertices();

	    TriangleMesh.Face[] face = mesh.getFaces();
	    TriangleMesh.Edge[] edge = mesh.getEdges();

	    TriangleMesh.Face tempFace;
	    TriangleMesh.Edge tempEdge, tempEdge2;

	    ArrayList list;
	    Object obj;
	    String key, listkey;
	    Integer idx = null;
	    int fix, fixed, x, y, id, max;

	    msg(null);

	    System.out.println("fixMesh: vert=" + vert.length + "; faces=" +
			       face.length + "; edge=" + edge.length);

	    /* 
	     *  remap duplicate points
	     */

	    if (mode == ALL_FEATURES || mode == POINT_MODE) {
		fix = findIrregularPoints(sel, map, findAll);

		if (fix > 0) {
		    int v = 0;

		    // remap faces
		    max = face.length;
		    for (x = 0; x < max; x++) {
			tempFace = face[x];

			v = tempFace.v1;
			if (map[v] != v) tempFace.v1 = map[v];

			v = tempFace.v2;
			if (map[v] != v) tempFace.v2 = map[v];

			v = tempFace.v3;
			if (map[v] != v) tempFace.v3 = map[v];
		    }

		    // remap edges
		    max = edge.length;
		    for (x = 0; x < max; x++) {
			tempEdge = edge[x];

			v = tempEdge.v1;
			if (map[v] != v) {
			    tempEdge.v1 = map[v];
			    vert[tempEdge.v1].edges++;
			}

			v = tempEdge.v2;
			if (map[v] != v) {
			    tempEdge.v2 = map[v];
			    vert[tempEdge.v2].edges++;
			}
		    }

		    // do not delete the points we've mapped to
		    max = map.length;
		    for (x = 0; x < max; x++) if (map[x] == x) sel[x] = false;

		    // delete duplicate vertices
		    super.setSelectionMode(POINT_MODE);
		    super.setSelection(sel);
		    super.deleteCommand();

		    mesh = (TriangleMesh) objInfo.object;
		    vert = (TriangleMesh.Vertex[]) mesh.getVertices();
		    face = mesh.getFaces();
		    edge = mesh.getEdges();

		    msg("\ndeleted " + fix + " duplicate points (" +
			vert.length + " remain)");
		}
	    }

	    /*
	     * remove degenerate faces
	     */
	    if (mode == ALL_FEATURES || mode == FACE_MODE) {
		fix = findIrregularFaces(sel, findAll);

		System.out.println("fix: faces=" + fix);

		if (fix > 0) {
		    super.setSelectionMode(FACE_MODE);
		    super.setSelection(sel);
		    super.deleteCommand();
	    
		    mesh = (TriangleMesh) objInfo.object;
		    vert = (TriangleMesh.Vertex[]) mesh.getVertices();
		    face = mesh.getFaces();
		    edge = mesh.getEdges();

		    msg("\ndeleted " + fix + " degenerate faces (" +
			face.length + " remain)");
		}
	    }

	    /* 
	     * join/delete duplicate edges
	     */
	    if (mode == ALL_FEATURES || mode == EDGE_MODE) {
		fix = findIrregularEdges(sel, findAll);
		System.out.println("fix: fix edges=" + fix);

		fixed = 0;
		if (fix > 0) {

		    // try to remap edges, so they can be deleted
		    max = edge.length;
		    for (x = 0; x < max; x++) {
			if (sel[x]) {
			    tempEdge = edge[x];

			    sel[x] = false;

			    if (tempEdge.f2 == -1) {

				// try to replace this edge
				key = getKey(vert[tempEdge.v1],
					     vert[tempEdge.v2]);

				list = (ArrayList) symbol.get(key);

				// degenerate edge
				if (list == null) {
				    sel[x] = true;
				    fixed++;
				    continue;
				}

				for (y = list.size()-1; y >= 0; y--) {
				    tempEdge2 = (TriangleMesh.Edge)
					list.get(y);

				    if (tempEdge2.f2 == -1) {
					id=((Integer)symbol.get(tempEdge2))
					    .intValue();

					if (replaceEdge(mesh, x, id)) {
					    sel[x] = true;
					    fixed++;
					    break;
					}
				    }
				}
			    }
			}
		    }

		    // delete those edges we remapped
		    super.setSelectionMode(EDGE_MODE);
		    super.setSelection(sel);
		    super.deleteCommand();

		    mesh = (TriangleMesh) objInfo.object;
		    vert = (TriangleMesh.Vertex[]) mesh.getVertices();
		    face = mesh.getFaces();
		    edge = mesh.getEdges();

		    msg("\ndeleted " + fixed + "/" + fix +
			" duplicate/degenerate edges (" + edge.length +
			" remain)");
		}
	    }

	    super.setSelectionMode(currMode);
	    
	    max = currSel.length;
	    for (x = 0; x < max; x++) currSel[x] = false;
	    super.setSelection(currSel);

	    //findIrregular(sel, ALL_FEATURES);
	    validate();
	}
	finally {
	    lock--;
	}
    }

    /**
     *  validate the solid
     */
    public void validate()
    {
	try {
	    lock++;

	    int currMode = super.getSelectionMode();
	    boolean[] currSel = super.getSelection();

	    TriangleMesh mesh = (TriangleMesh) objInfo.object;
	    TriangleMesh.Vertex[] vert =
		(TriangleMesh.Vertex[]) mesh.getVertices();

	    TriangleMesh.Face[] face = mesh.getFaces();
	    TriangleMesh.Edge[] edge = mesh.getEdges();

	    TriangleMesh.Face tempFace;
	    TriangleMesh.Edge tempEdge;

	    if (symbol == null) symbol = new HashMap(face.length*3);

	    int x, max, pts, edgs, fcs;

	    msg(null);

	    // is anything selected?
	    int mode = findAll;
	    max = currSel.length;
	    for (x = 0; x < max; x++) {
		if (currSel[x]) {
		    mode = findSelected;
		    break;
		}
	    }

	    if (sel == null || sel.length < face.length*3)
		sel = new boolean[face.length*3];
	    
	    System.out.println("validate: currSel:" + currSel.length +
			       "; sel:" + sel.length);

	    super.setSelectionMode(FACE_MODE);
	    currSel = super.getSelection();
	    System.arraycopy(currSel, 0, sel, 0, currSel.length);
	    for (x = currSel.length; x < sel.length; x++) sel[x] = false;

	    /*
	    // NTJ: BEBUG
	    if (mode == findSelected) {
		max = currSel.length;

		for (x = 0; x < max; x++) {
		    if (sel[x]) msg("selected face: " + vert[face[x].v1].r +
				    ", " + vert[face[x].v2].r + ", " +
				    vert[face[x].v3].r);
		}
	    }
	    */

	    fcs = findIrregularFaces(sel, mode);
	    if (fcs > 0) {
		msg("\nvalidate: found " + fcs + " irregular faces");

		max = face.length;
		for (x = 0; x < max; x++) {
		    tempFace = face[x];
		    if (sel[x]) msg("face edge: p" + tempFace.v1 + ", p" +
				    tempFace.v2 + ", p" + tempFace.v3);
		}
	    }

	    super.setSelectionMode(EDGE_MODE);
	    currSel = super.getSelection();
	    System.arraycopy(currSel, 0, sel, 0, currSel.length);
	    for (x = currSel.length; x < sel.length; x++) sel[x] = false;

	    /*
	    // NTJ: BEBUG
	    if (mode == findSelected) {
		max = currSel.length;

		for (x = 0; x < max; x++) {
		    if (sel[x]) msg("selected edge: " + vert[edge[x].v1].r +
				    ", " + vert[edge[x].v2].r);
		}
	    }
	    */

	    edgs = findIrregularEdges(sel, mode);
	    if (edgs > 0) {
		msg("\nvalidate: found " + edgs + " irregular edges");

		max = edge.length;
		for (x = 0; x < max; x++) {

		    tempEdge = edge[x];
		    if (sel[x]) msg("edge: p" + tempEdge.v1 + ", p" +
				    tempEdge.v2 + ", f" + tempEdge.f1 +
				    ", f" + tempEdge.f2);
		}
	    }

	    super.setSelectionMode(POINT_MODE);
	    currSel = super.getSelection();
	    System.arraycopy(currSel, 0, sel, 0, currSel.length);
	    for (x = currSel.length; x < sel.length; x++) sel[x] = false;

	    /*
	    // NTJ: BEBUG
	    if (mode == findSelected) {
		max = currSel.length;

		for (x = 0; x < max; x++) {
		    msg("selected: point" + vert[face[x].v1].r + ", " +
			vert[face[x].v2].r + ", " + vert[face[x].v3].r);
		}
	    }
	    */

	    pts = findIrregularPoints(sel, null, mode);
	    if (pts > 0) {
		msg("\nvalidate: found " + pts + " irregular points");

		max = vert.length;
		for (x = 0; x < max; x++) {
		    if (sel[x]) msg("point: p" + x + ": " +
				    vert[x].r.toString());
		}
	    }

	    if (fcs + edgs + pts == 0)
		System.out.println("validate: no errors found");

	    super.setSelectionMode(currMode);
	    
	    pointField.setValue(pts);
	    edgeField.setValue(edgs);
	    faceField.setValue(fcs);

	    totalPointField.setValue(vert.length);
	    totalEdgeField.setValue(edge.length);
	    totalFaceField.setValue(face.length);

	    clearSelect();
	}
	finally {
	    lock--;
	}
    }

    /**
     *  show/hide the shotgun window
     */
    public void shotgun(CommandEvent ev)
    {
	//System.out.println("shotgun: command=" + ev.getActionCommand());

	String cmd;
	if (ev.getActionCommand().equals(showCmd)) {
	    solidMenuItem[1].setEnabled(true);

	    stats.setVisible(true);
	    statsOpen = true;
	}
	else if (ev.getActionCommand().equals(hideCmd)) {
	    solidMenuItem[1].setEnabled(false);

	    stats.setVisible(false);
	    statsOpen = false;
	}
    }

    /**
     *  select irregular objects
     */
    public void select()
    {
	// clear selection
	clearSelect();

	boolean[] select = super.getSelection();
	int max = findIrregular(select, super.getSelectionMode());
	super.setSelection(select);

	safety = true;
	edit(select, max);
    }

    /**
     *  clear the shotgun selection details
     */
    public void clearSelect()
    {
	maxSelection = 0;
	currSelection = -1;

	selIndex.setValue(INVALID);
	selField.setValue(0);

	xField.setValue(0);
	yField.setValue(0);
	zField.setValue(0);

	f1Field.setValue(0);
	f2Field.setValue(0);
	v1Field.setValue(0);
	v2Field.setValue(0);

	v3Field.setValue(0);
	v4Field.setValue(0);
	v5Field.setValue(0);
	e1Field.setValue(0);
	e2Field.setValue(0);
	e3Field.setValue(0);

	editCont.removeAll();

	saveButton.setEnabled(false);
	resetButton.setEnabled(false);

	stats.pack();
    }

    /**
     *  close the mesh.
     *
     *  Finds all selected irregular edges, and attempts to close them, or any
     *  subset of them.
     *
     *  If the current selection is empty, then all edges are checked,
     *  otherwise only the selected edges are checked.
     */
    public void closeCommand()
    {
	try {
	    lock++;

	    // TriMeshEditorWindow.closeBoundaryCommand is package accessible
	    Class type = TriMeshEditorWindow.class;
	    Method close = type.getDeclaredMethod("closeBoundaryCommand",
						  null);
	    close.setAccessible(true);

	    int x, max, fix;
	    boolean selected = false, loop[];

	    int currMode = super.getSelectionMode();

	    // first, try the standard way
	    super.selectObjectBoundaryCommand();
	    boolean[] select = super.getSelection();

	    max = select.length;
	    for (x = 0; x < max; x++) {
		if (select[x]) {
		    selected = true;
		    break;
		}
	    }
	    
	    if (selected) /* super.closeBoundaryCommand(); */
		close.invoke(this, null);

	    // now see if there are any remaining holes

	    TriangleMesh mesh = (TriangleMesh) objInfo.object;
	    TriangleMesh.Vertex[] vert =
		(TriangleMesh.Vertex[]) mesh.getVertices();

	    TriangleMesh.Face[] face = mesh.getFaces();
	    TriangleMesh.Edge[] edge = mesh.getEdges();

	    super.setSelectionMode(EDGE_MODE);
	    select = super.getSelection();

	    fix = findIrregular(select, EDGE_MODE);

	    if (fix > 0) {
		max = select.length;

		if (sel.length < max) sel = new boolean[max];
		System.arraycopy(select, 0, sel, 0, max);
		for (x = 0; x < max; x++) select[x] = false;

		for (x = 0; x < max; x++) {
		    if (sel[x] && edge[x].f2 == -1) {

			select[x] = true;
			loop = TriMeshSelectionUtilities
			    .findEdgeLoops(mesh, select);

			if (loop != null) {
			    super.setSelection(loop);
			    //super.closeBoundaryCommand();
			    close.invoke(this, null);
			}

			select[x] = false;
		    }
		}
	    }

	    for (x = 0; x < max; x++) select[x] = false;
	    super.setSelectionMode(currMode);
	    super.setSelection(select);

	    validate();
	}
	catch (Exception e) {
	    System.out.println("close: " + e);
	}
	finally {
	    lock--;
	}
    }

    /**
     *  apply a grid to the selected features.
     *
     *  transform each point so it is snapped to a grid
     */
    public void gridCommand()
    {
	ValueField gridField = new ValueField(0.01, ValueField.POSITIVE);
	ValueField thresholdField = new ValueField(10, ValueField.POSITIVE);

	BCheckBox xBox = new BCheckBox(Translate.text("Align X"),
					    true);
	BCheckBox yBox = new BCheckBox(Translate.text("Align Y"),
					    true);
	BCheckBox zBox = new BCheckBox(Translate.text("Align Z"),
					    true);

	BCheckBox adjustBox = new BCheckBox(Translate.text("Adjust mesh"),
					    true);

	ComponentsDialog dlg = new
	    ComponentsDialog(this, "Set Grid",
			     new Widget[] {
				 gridField, thresholdField,
				 xBox, yBox, zBox, adjustBox
			     },
			     new String[] {
				 Translate.text("Grid Spacing"),
				 Translate.text("Snap Threshold%"),
				 null, null, null, null
			     });

	if (!dlg.clickedOk()) return;

	boolean[] select = super.getSelection();

	TriangleMesh mesh = (TriangleMesh) objInfo.object;
	TriangleMesh.Vertex[] vert =
	    (TriangleMesh.Vertex[]) mesh.getVertices();

	TriangleMesh.Face[] face = mesh.getFaces();
	TriangleMesh.Edge[] edge = mesh.getEdges();

	TriangleMesh.Face tempFace, tempFace2;
	TriangleMesh.Edge rootEdge, tempEdge, tempEdge2;
	TriangleMesh.Vertex tempVert;

	setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT,
				     new Object [] {
					 mesh, mesh.duplicate()
				     }));

	int x, y, max, fix = 0;
	double space, thresh;
	boolean adj, axis[], selectAll = true;
	Vec3 p1, p2, p3, p4;

	space = gridField.getValue();
	thresh = space * thresholdField.getValue() / 200;
	adj = adjustBox.getState();

	axis = new boolean[3];
	axis[0] = xBox.getState();
	axis[1] = yBox.getState();
	axis[2] = zBox.getState();

	// is anything selected?
	max = select.length;
	for (x = 0; x < max; x++) {
	    if (select[x]) {
		selectAll = false;
		break;
	    }
	}

	switch (super.getSelectionMode()) {
	case POINT_MODE:
	    for (x = 0; x < max; x++) {
		if (selectAll || select[x])
		    alignPoint(vert[x].r, axis, space, thresh, adj);
	    }
	    break;

	case EDGE_MODE:
	    for (x = 0; x < max; x++) {
		if (selectAll || select[x]) {
		    tempEdge = edge[x];

		    alignPoint(vert[tempEdge.v1].r, axis, space, thresh, adj);
		    alignPoint(vert[tempEdge.v2].r, axis, space, thresh, adj);
		}
	    }
	    break;

	case FACE_MODE:
	    for (x = 0; x < max; x++) {
		if (selectAll || select[x]) {
		    tempFace = face[x];

		    alignPoint(vert[tempFace.v1].r, axis, space, thresh, adj);
		    alignPoint(vert[tempFace.v2].r, axis, space, thresh, adj);
		    alignPoint(vert[tempFace.v3].r, axis, space, thresh, adj);
		}
	    }
	    break;
	}

	updateImage();
    }

    /**
     *  find irregular features matching the criteria.
     *
     *  If <code>select</code> selects features, then the selection is refined
     *  to include only irregular features.
     *
     *@param select an boolean array of selected features.
     *@param selMode integer mode to indicate which features to select.
     *   Value may be one of POINT_MODE, EDGE_MODE, FACE_MODE or ALL_FEATURES.
     */
    public int findIrregular(boolean[] select, int selMode)
    {
	int result = 0;

	TriangleMesh mesh = (TriangleMesh) objInfo.object;
	TriangleMesh.Vertex[] vert =
	    (TriangleMesh.Vertex[]) mesh.getVertices();

	TriangleMesh.Face[] face = mesh.getFaces();
	TriangleMesh.Edge[] edge = mesh.getEdges();

	int x, max, fix;

	totalPointField.setValue(vert.length);
	totalEdgeField.setValue(edge.length);
	totalFaceField.setValue(face.length);

	msg(null);

	// is anything selected?
	int mode = findAll;
	max = select.length;
	for (x = 0; x < max; x++) {
	    if (select[x]) {
		mode = findSelected;
		break;
	    }
	}

	pointField.setValue(-1);
	edgeField.setValue(-1);
	faceField.setValue(-1);
	
	if (selMode == ALL_FEATURES || selMode == FACE_MODE) {
	    fix = findIrregularFaces(select, mode);
	    faceField.setValue(fix);
	    if (fix > 0) msg("\nfindIrregular: " + fix + " irregular faces");
	    result += fix;
	}

	if (selMode == ALL_FEATURES || selMode == EDGE_MODE) {
	    fix = findIrregularEdges(select, mode);
	    edgeField.setValue(fix);
	    if (fix > 0) msg("\nfindIrregular: " + fix + " irregular edges");
	    result += fix;
	}

	if (selMode == ALL_FEATURES || selMode == POINT_MODE) {
	    fix = findIrregularPoints(select, null, mode);
	    pointField.setValue(fix);
	    if (fix > 0) msg("\nfindIrregular: " + fix + " irregular points");
	    result += fix;
	}

	return result;
    }

    /**
     *  find irregular faces
     */
    public int findIrregularFaces(boolean[] select, int mode)
    {
	int result = 0;
	
	TriangleMesh mesh = (TriangleMesh) objInfo.object;
	TriangleMesh.Vertex[] vert =
	    (TriangleMesh.Vertex[]) mesh.getVertices();

	TriangleMesh.Face[] face = mesh.getFaces();
	TriangleMesh.Edge[] edge = mesh.getEdges();

	boolean all = (mode == findAll);
	int x, max;

	max = face.length;

	for (x = max; x < select.length; x++) select[x] = false;

	TriangleMesh.Face tempFace;

	symbol.clear();

	for (x = 0; x < max; x++) {

	    if (! (all || select[x])) continue;

	    select[x] = false;

	    tempFace = face[x];
	    s1 = vert[tempFace.v2].r.minus(vert[tempFace.v1].r);
	    s2 = vert[tempFace.v3].r.minus(vert[tempFace.v1].r);
	    s3 = vert[tempFace.v3].r.minus(vert[tempFace.v2].r);

	    // zero-length sides mean the face is degenerate
	    if (Math.abs(s1.length()) <= tol
		|| Math.abs(s2.length()) <= tol
		|| Math.abs(s3.length()) <= tol
		) {

		//System.out.println("findFace: degenerate face (zero); v1=" +
		//	   tempFace.v1 + "; v2=" + tempFace.v2 +
		//	   "; v3=" + tempFace.v3 + "; e1=" +
		//	   tempFace.e1 + "; e2=" + tempFace.e2);

		select[x] = true;
		result++;
	    }
	    else {
		s1.normalize();
		s2.normalize();
		s3.normalize();

		if (Math.abs(s2.minus(s1).length()) <= tol
		    || Math.abs(s3.minus(s1).length()) <= tol
		    || Math.abs(s3.minus(s2).length()) <= tol) {

		    //System.out.println("findFace: degenerate face (par); v1=" +
		    //	       tempFace.v1 + "; v2=" + tempFace.v2 +
		    //	       "; v3=" + tempFace.v3 + "; e1=" +
		    //	       tempFace.e1 + "; e2=" + tempFace.e2);

		    select[x] = true;
		    result++;

		}
	    }
	}

	return result;
    }

    /**
     *  find irregular edges
     */
    public int findIrregularEdges(boolean[] select, int mode)
    {
	int result = 0;

	TriangleMesh mesh = (TriangleMesh) objInfo.object;
	TriangleMesh.Vertex[] vert =
	    (TriangleMesh.Vertex[]) mesh.getVertices();

	TriangleMesh.Face[] face = mesh.getFaces();
	TriangleMesh.Edge[] edge = mesh.getEdges();

	boolean all = (mode == findAll);

	TriangleMesh.Edge tempEdge;
	String key;
	ArrayList list;
	int x, max, id;

	symbol.clear();

	max = edge.length;

	for (x = max; x < select.length; x++) select[x] = false;

	for (x = 0; x < max; x++) {

	    if (! (all || select[x])) continue;
	    
	    select[x] = false;

	    tempEdge = edge[x];

	    // is it degenerate?
	    if (tempEdge.v1 == tempEdge.v2) {

		//System.out.println("findEdge: degenerate edge: " + x +
		//	   "; v1=" + tempEdge.v1 + "; v2=" +
		//	   tempEdge.v2);

		select[x] = true;
		result++;
		continue;
	    }

	    else {
		s1 = vert[tempEdge.v1].r.minus(vert[tempEdge.v2].r);
		
		if (Math.abs(s1.length()) <= tol) {
		    //System.out.println("findEdge: degenerate edge: " + x +
		    //	       "; len=" + s1.length() +
		    //	       "; v1=" + tempEdge.v1 + "; v2=" +
		    //	       tempEdge.v2);

		    select[x] = true;
		    result++;
		    continue;
		}
	    }


	    // select all open edges
	    select[x] = (tempEdge.f2 == -1);

	    key = getKey(vert[tempEdge.v1], vert[tempEdge.v2]);

	    list = (ArrayList) symbol.get(key);

	    if (list != null) {

		// mark the originator of this list as a duplicate as well
		if (list.size() == 1) {
		    id = ((Integer) symbol.get(list.get(0))).intValue();
		    select[id] = true;
		    result++;
		}

		list.add(tempEdge);
		select[x] = true;
	    }
	    else {
		// create the list
		list = new ArrayList(4);
		list.add(tempEdge);
		symbol.put(key, list);
	    }

	    symbol.put(tempEdge, new Integer(x));

	    if (select[x]) result++;
	}

	return result;
    }

    /**
     *  find irregular points
     */
    public int findIrregularPoints(boolean[] select, int[] map, int mode)
    {
	int result = 0;

	TriangleMesh mesh = (TriangleMesh) objInfo.object;
	TriangleMesh.Vertex[] vert =
	    (TriangleMesh.Vertex[]) mesh.getVertices();

	TriangleMesh.Face[] face = mesh.getFaces();
	TriangleMesh.Edge[] edge = mesh.getEdges();

	String key;
	Integer idx;

	boolean all = (mode == findAll);
	int x, max;

	max = vert.length;

	for (x = max; x < select.length; x++) select[x] = false;

	symbol.clear();

	for (x = 0; x < max; x++) {

	    if (! (all || select[x])) continue;

	    key = getKey(vert[x]);

	    //if (select[x])
		//System.out.println("findPoint: selected: " + key);

	    idx = (Integer) symbol.get(key);
	    if (idx == null) {
		symbol.put(key, new Integer(x));
		if (map != null) map[x] = x;
		select[x] = false;
	    }
	    else {
		if (map != null) map[x] = idx.intValue();

		//System.out.println("findPoint: mapping " + x + " to " +
		//	   idx.intValue());

		select[x] = true;
		result++;
	    }
	}

	return result;
    }

    /**
     *  enable the Solid Tool editing functions on the current selection
     */
    public void editCommand()
    {
	clearSelect();

	boolean[] select = super.getSelection();

	// anything selected?
	int x, max;

	max = select.length;
	int count = 0;
	for (x = 0; x < max; x++) if (select[x]) count++;

	safety = false;
	edit(select, count);
	changeSelected(1);	// goto first feature
    }

    /**
     *  join selected points, edges or faces
     *
     *<p>In POINT mode, make all selected points into a reference to the first
     *  selected point, and delete the remaining points.
     *
     *<p>In FACE mode, join boundary faces to an adjacent face either by
     *  deleting a duplicate edge, or creating a spanning face.
     *
     *<p>In EDGE mode, find the "external" path of faces that connect the
     *  edges, and delete the faces and edges on the "internal" path.
     */
    public void joinCommand()
    {
	boolean[] select = super.getSelection();

	switch (super.getSelectionMode()) {
	case POINT_MODE:
	case EDGE_MODE:
	case FACE_MODE:
	}	
    }

    /**
     *  replace one edge with another.
     *
     *  edge[fromx] is replaced by edge[tox]. Affected faces and vertices are
     *  adjusted to reflect the change. The replaced edge is <em>not</em>
     *  deleted from the mesh.
     */
    public boolean replaceEdge(TriangleMesh mesh, int fromx, int tox)
    {
	TriangleMesh.Edge from, to, tempEdge, edge[];
	edge = mesh.getEdges();
	from = edge[fromx];
	to = edge[tox];

	TriangleMesh.Vertex v, vert[];
	vert = (TriangleMesh.Vertex[]) mesh.getVertices();

	TriangleMesh.Face face;

	if (from.f2 == -1 && to.f2 == -1) {
	    to.f2 = from.f1;

	    // fix the vertices
	    v = vert[from.v1];
	    v.edges--;
	    if (v.firstEdge == fromx) v.firstEdge = tox;

	    v = vert[from.v2];
	    v.edges--;
	    if (v.firstEdge == fromx) v.firstEdge = tox;

	    face = mesh.getFaces()[to.f2];

	    // move the face to the new edge
	    if (face.e1 == fromx) face.e1 = tox;
	    else if (face.e2 == fromx) face.e2 = tox;
	    else if (face.e3 == fromx) face.e3 = tox;

	    if (face.v1 == from.v1) face.v1 = to.v1;
	    else if (face.v1 == from.v2) face.v1 = to.v2;

	    if (face.v2 == from.v1) face.v2 = to.v1;
	    else if (face.v2 == from.v2) face.v2 = to.v2;

	    if (face.v3 == from.v1) face.v3 = to.v1;
	    else if (face.v3 == from.v2) face.v3 = to.v2;

	    return true;
	}

	return false;
    }

    /**
     *  setup the editing fields for this selection
     */
    protected void edit(boolean[] select, int max)
    {
	if (max > 0) {

	    switch (super.getSelectionMode()) {
	    case POINT_MODE:
		pointField.setValue(max);
		editCont.add(selrow);
		selLabel.setText(pointLabel);
		editCont.add(pointCont);
		break;

	    case EDGE_MODE:
		edgeField.setValue(max);
		editCont.add(selrow);
		selLabel.setText(edgeLabel);
		editCont.add(edgeCont);
		break;

	    case FACE_MODE:
		faceField.setValue(max);
		editCont.add(selrow);
		selLabel.setText(faceLabel);
		editCont.add(faceCont);
		break;
	    }

	    editCont.add(editButtons);

	    safety = !safety;
	    safety();

	    stats.pack();

	    maxSelection = max;

	    if (selection == null || selection.length < maxSelection)
		selection = new int[(int) (maxSelection * 1.4)];

	    //System.out.println("max=" + max + "; select=" + select.length +
	    //	       "; selection=" + selection.length);

	    max = select.length;
	    int ptr = 0;
	    for (int x = 0; x < max; x++) if (select[x]) selection[ptr++] = x;
	}
    }

    /**
     *  save changes made in an edit
     */
    protected void save()
    {
	TriangleMesh mesh = (TriangleMesh) objInfo.object;
	TriangleMesh.Vertex[] vert =
	    (TriangleMesh.Vertex[]) mesh.getVertices();

	TriangleMesh.Face[] face = mesh.getFaces();
	TriangleMesh.Edge[] edge = mesh.getEdges();

	TriangleMesh.Edge edg;
	TriangleMesh.Face fac;

	setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT,
				     new Object [] {
					 mesh, mesh.duplicate()
				     }));

	switch (super.getSelectionMode()) {
	case POINT_MODE:
	    s1 = vert[ selection[currSelection] ].r;
	    s1.x = xField.getValue();
	    s1.y = yField.getValue();
	    s1.z = zField.getValue();
	    break;

	case EDGE_MODE:
	    edg = edge[ selection[currSelection] ];
	    edg.v1 = (int) v1Field.getValue();
	    edg.v2 = (int) v2Field.getValue();
	    edg.f1 = (int) f1Field.getValue();
	    edg.f2 = (int) f2Field.getValue();
	    break;

	case FACE_MODE:
	    fac = face[ selection[currSelection] ];
	    fac.v1 = (int) v3Field.getValue();
	    fac.v2 = (int) v4Field.getValue();
	    fac.v3 = (int) v5Field.getValue();
	    fac.e1 = (int) e1Field.getValue();
	    fac.e2 = (int) e2Field.getValue();
	    fac.e3 = (int) e3Field.getValue();
	    break;
	}

	updateImage();
    }

    /**
     *  restore original values
     */
    protected void reset()
    {
	super.undoCommand();
	changeSelected(0);
    }

    /**
     *  toggle the shotgun safety catch...
     */
    protected void safety()
    {
	safety = !safety;

	if (safety) {
	    safetyButton.setText(UNLOCK);
	    saveButton.setEnabled(false);
	    resetButton.setEnabled(false);

	    f1Field.setEnabled(false);
	    f2Field.setEnabled(false);

	    v1Field.setEnabled(false);
	    v2Field.setEnabled(false);
	    v3Field.setEnabled(false);
	    v4Field.setEnabled(false);
	    v5Field.setEnabled(false);

	    e1Field.setEnabled(false);
	    e2Field.setEnabled(false);
	    e3Field.setEnabled(false);
	
	}
	else {
	    safetyButton.setText(LOCK);
	    saveButton.setEnabled(true);
	    resetButton.setEnabled(true);

	    f1Field.setEnabled(true);
	    f2Field.setEnabled(true);

	    v1Field.setEnabled(true);
	    v2Field.setEnabled(true);
	    v3Field.setEnabled(true);
	    v4Field.setEnabled(true);
	    v5Field.setEnabled(true);

	    e1Field.setEnabled(true);
	    e2Field.setEnabled(true);
	    e3Field.setEnabled(true);
	}

	stats.pack();
    }

    protected String getKey(TriangleMesh.Vertex v)
    {
	return nf.format(v.r.x) + "," + nf.format(v.r.y) + "," +
	    nf.format(v.r.z);
    }

    protected String getKey(TriangleMesh.Vertex v1, TriangleMesh.Vertex v2)
    { return getKey(v1) + "-" + getKey(v2); }

    /*
     *  NTJ: overly-simplistic algorithm
    protected void alignPoint(Vec3 p, NumberFormat nf)
    {
	p.x = Double.parseDouble(nf.format(p.x));
	p.y = Double.parseDouble(nf.format(p.y));
	p.z = Double.parseDouble(nf.format(p.z));
    }
    */

    /**
     *  align a point to a grid, if it is within snap-range
     */
    protected void alignPoint(Vec3 p, boolean[] axis, double space,
			      double thresh, boolean adj)
    {
	double lower, centre, upper, diff;

	centre = space / 2;
	lower = thresh;
	upper = space - thresh;

	if (axis[0]) {
	    diff = p.x % space;
	    if (diff <= lower) p.x -= diff;
	    else if (diff >= upper) p.x += (diff - upper);
	    else if (!adj);
	    else if (diff < centre) p.x -= (centre - diff);
	    else p.x += (diff - centre);
	}

	if (axis[1]) {
	    diff = p.y % space;
	    if (diff <= lower) p.y -= diff;
	    else if (diff >= upper) p.y += (diff - upper);
	    else if (!adj);
	    else if (diff < centre) p.y -= (centre - diff);
	    else p.y += (diff - centre);
	}

	if (axis[2]) {
	    diff = p.z % space;
	    if (diff <= lower) p.z -= diff;
	    else if (diff >= upper) p.z += (diff - upper);
	    else if (!adj);
	    else if (diff < centre) p.z -= (centre - diff);
	    else p.z += (diff - centre);
	}
    }

    /**
     *  change the selected feature to one <i>step</i> elements away in the
     *  selection array.
     */
    protected void changeSelected(int step)
    {
	try {
	    lock++;

	    if (selection != null && maxSelection > 0) {

		boolean[] select = super.getSelection();
		if (currSelection < 0)
		    for (int x = 0; x < select.length; x++) sel[x] = false;
		else
		    select[ selection[currSelection] ] = false;

		currSelection = (currSelection + maxSelection + step)
		    % maxSelection;

		//selIndex.setValue(currSelection);
		selIndex.setValue(new Integer(currSelection));
		selField.setValue(selection[currSelection]);

		select[ selection[currSelection] ] = true;
		super.setSelection(select);

		TriangleMesh mesh = (TriangleMesh) objInfo.object;
		TriangleMesh.Vertex[] vert =
		    (TriangleMesh.Vertex[]) mesh.getVertices();

		TriangleMesh.Face[] face = mesh.getFaces();
		TriangleMesh.Edge[] edge = mesh.getEdges();

		TriangleMesh.Edge edg;
		TriangleMesh.Face fac;

		switch (super.getSelectionMode()) {
		case POINT_MODE:
		    s1 = vert[ selection[currSelection] ].r;
		    xField.setValue(s1.x);
		    yField.setValue(s1.y);
		    zField.setValue(s1.z);
		    break;

		case EDGE_MODE:
		    edg = edge[ selection[currSelection] ];
		    v1Field.setValue(edg.v1);
		    v2Field.setValue(edg.v2);
		    f1Field.setValue(edg.f1);
		    f2Field.setValue(edg.f2);
		    break;

		case FACE_MODE:
		    fac = face[ selection[currSelection] ];
		    v3Field.setValue(fac.v1);
		    v4Field.setValue(fac.v2);
		    v5Field.setValue(fac.v3);
		    e1Field.setValue(fac.e1);
		    e2Field.setValue(fac.e2);
		    e3Field.setValue(fac.e3);
		    break;
		}
	    }
	}
	finally {
	    lock--;
	}
    }

    protected void msg(String txt)
    {
	try {
	    if (txt != null) {
		message.write("");
		//message.write(txt);
		//System.out.println(txt);

		messageArea.append(txt);
		if (scroll.getChildCount() == 0) {
		    scroll.setContent(messageArea);
		    stats.pack();
		}
	    }
	    else {
		if (scroll.getChildCount() > 0) {
		    messageArea.setText("");
		    scroll.remove(messageArea);
		    stats.pack();
		}
	    }
	} catch (IOException e) {}
    }

    protected void createSolidMenu(TriangleMesh obj)
    {
	//System.out.println("adding solid menu");

	solidMenu = Translate.menu("Solid");
	menubar.add(solidMenu);
	solidMenuItem = new BMenuItem [11];

	solidMenuItem[0] = Translate.menuItem("show shotgun", this, "shotgun");

	solidMenuItem[0].setActionCommand(showCmd);
	solidMenu.add(solidMenuItem[0]);

	solidMenuItem[1] = Translate.menuItem("hide shotgun", this, "shotgun");

	solidMenuItem[1].setActionCommand(hideCmd);
	solidMenu.add(solidMenuItem[1]);

	solidMenuItem[2] = Translate.menuItem("Validate", this, "validate");
	solidMenu.add(solidMenuItem[2]);

	solidMenuItem[3] = Translate.menuItem("Select irregular", this,
					      "select");
	solidMenu.add(solidMenuItem[3]);


	BMenu fixMenu = Translate.menu("Fix");
	solidMenu.add(fixMenu);

	solidMenuItem[4] = Translate.menuItem("Fix All", this,
					      "fixAllCommand");
	fixMenu.add(solidMenuItem[4]);

	solidMenuItem[5] = Translate.menuItem("Fix Points", this,
					      "fixPointCommand");
	fixMenu.add(solidMenuItem[5]);

	solidMenuItem[6] = Translate.menuItem("Fix Edges", this,
					      "fixEdgeCommand");
	fixMenu.add(solidMenuItem[6]);

	solidMenuItem[7] = Translate.menuItem("Fix Faces", this,
					      "fixFaceCommand");
	fixMenu.add(solidMenuItem[7]);

	solidMenuItem[8] = Translate.menuItem("Close Mesh", this,
					      "closeCommand");
	solidMenu.add(solidMenuItem[8]);

	solidMenuItem[9] = Translate.menuItem("Apply Grid", this,
					      "gridCommand");
	solidMenu.add(solidMenuItem[9]);

	solidMenuItem[10] = Translate.menuItem("Edit Selection", this,
					      "editCommand");
	solidMenu.add(solidMenuItem[10]);

	/*
	solidMenuItem[x] = Translate.menuItem("closeMesh", this,
					      "closeMeshCommand");
	if (topology)
	    solidMenu.add(solidMenuItem[x]);
	*/

    }

}
