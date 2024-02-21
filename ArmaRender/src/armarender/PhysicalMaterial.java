/* Copyright (C) 2023 by Jon Taylor

    */

package armarender;

import armarender.math.*;
import armarender.object.*;
import armarender.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.net.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;

// Physical Material manages materials cad models are made from.
// Stored in seperate file pulled from an Arma server.
public class PhysicalMaterial extends BDialog {
    
    private BFrame fr;
    Scene scene;
    private LayoutWindow window;
    private ValueField insertRowTextField, insertColumnTextField;
    BTree remoteFileList;
    BScrollPane listWrapper;
    private static String materialFileContent;
    
    Vector<MaterialContainer> tubeMaterials = new Vector<MaterialContainer>();
    Vector<MaterialContainer> plateMaterials = new Vector<MaterialContainer>();
    Vector<MaterialContainer> panelMaterials = new Vector<MaterialContainer>();
    
    public class MaterialContainer {
        int id = -1;
        String name = "";
        String detail = "";
        double price = 0;
        public double weight = 0;
        double od = 0;
        double wall = 0;
        double bendRadius = 0;
        
        public String toString(){
            return detail + " $" + price;
        }
    }
    
    public PhysicalMaterial(BFrame parent, LayoutWindow window, Scene theScene){
        super(parent, "Material Selection", true);
        fr = parent;
        scene = theScene;
        this.window = window;
        remoteFileList = new BTree();
    }
    
    /**
     * display
     *
     * Description: Display dialog to user.
     */
    public void display(){
          FormContainer mainContent = new FormContainer(1, 3);
          FormContainer content = new FormContainer(9, 10);
          //FormContainer content = new FormContainer(new double [] {1}, new double [] {1, 0, 0, 0});
          setContent(BOutline.createEmptyBorder(mainContent, UIUtilities.getStandardDialogInsets()));
          mainContent.add( content, 0, 0 );
          
          //addNode(javax.swing.tree.TreePath parent, javax.swing.tree.MutableTreeNode node)
          TreePath tp = remoteFileList.getRootNode();
          remoteFileList.setRootNodeShown(false); // Have files appear as root nodes.
          remoteFileList.setPreferredVisibleRows(10);
          remoteFileList.setCellRenderer(new FileTreeRenderer());
          
          //TreeCellRenderer renderer = remoteFileList.getCellRenderer();
          
          //tp.
          //DefaultMutableTreeNode x = new DefaultMutableTreeNode("X");
          //remoteFileList.addNode(tp, x);
          //remoteFileList.addNode(tp, new DefaultMutableTreeNode("Y"));
          
          remoteFileList.setMultipleSelectionEnabled(false);
          remoteFileList.addEventLink(SelectionChangedEvent.class, this, "doSelectionChanged");
          remoteFileList.addEventLink(MouseClickedEvent.class, this, "mouseClicked");
          
          //remoteFileList.add
          
          listWrapper = new BScrollPane(remoteFileList, BScrollPane.SCROLLBAR_AS_NEEDED, BScrollPane.SCROLLBAR_AS_NEEDED);
          listWrapper.setBackground(remoteFileList.getBackground());
          listWrapper.setForceWidth(true);
          listWrapper.setPreferredViewSize(new Dimension(600, 350));
          content.add(listWrapper, 0, 0);
          
          //content.add(insertRowTextField = new ValueField(1, ValueField.POSITIVE, 5), 1, 6);
          //content.add(Translate.button("Insert Row", this, "download"), 2, 6);
          //content.add(insertColumnTextField = new ValueField(1, ValueField.POSITIVE, 5), 3, 6);
          //content.add(Translate.button("Insert Column", this, "download"), 4, 6);
          
          //RowContainer choiceRow = new RowContainer();
          //content.add(choiceRow, 0, 1);
          
          RowContainer row = new RowContainer();
          row.add(Translate.button("Set", this, "set"));
          //row.add(Translate.button("ok", this, "download"));
          row.add(Translate.button("Cancel", this, "doCancel"));
          mainContent.add(row, 0, 1 );
          
          //loadFilesAvailable();
        loadMaterials();

        // Show the dialog.
        pack();
        UIUtilities.centerDialog(this, fr);
        setVisible(true);
    }
    
    /**
     * loadMaterials
     *
     * Description:
     */
    public void loadMaterials(){
        try {
            Vector<Integer> selectedMaterialIds = new Vector<Integer>();
            ArrayList<ObjectInfo> selection = (ArrayList<ObjectInfo>)window.getSelectedObjects();
            for(int i = 0; i < selection.size(); i++){
                ObjectInfo selectedInfo = (ObjectInfo)selection.get(i);
                int matId = selectedInfo.getPhysicalMaterialId();
                if(matId != -1){
                    selectedMaterialIds.addElement(matId);
                }
            }
            
            TreePath rootTp = remoteFileList.getRootNode();
            
            if(materialFileContent == null){
                materialFileContent = "";
                String a = "http://arma.ac/download/materials.xml";
                URLConnection connection = new URL(a).openConnection();
                connection.setRequestProperty("User-Agent",
                                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
                connection.connect();
                BufferedReader r = new BufferedReader(new InputStreamReader(connection.getInputStream(), Charset.forName("UTF-8")));
                String line;
                while ((line = r.readLine()) != null) {
                    materialFileContent += line;
                }
            }
            
            //System.out.println(" --- content " + content);
            
            //Vector<MaterialContainer> tubeMaterials = new Vector<MaterialContainer>();
            //Vector<MaterialContainer> plateMaterials = new Vector<MaterialContainer>();
            //Vector<MaterialContainer> panelMaterials = new Vector<MaterialContainer>();
            tubeMaterials.clear();
            plateMaterials.clear();
            panelMaterials.clear();
            
            
            String tubeSection = "";
            int sectionStart = materialFileContent.indexOf("<tube>");
            int sectionEnd = materialFileContent.indexOf("</tube>");
            if(sectionStart != -1 && sectionEnd != -1){
                tubeSection = materialFileContent.substring(sectionStart + 6, sectionEnd);
                int start = tubeSection.indexOf("<material>");
                int end = tubeSection.indexOf("</material>");
                while(start != -1 && end != -1 && start < end &&  start + 10 < tubeSection.length() && end < tubeSection.length()){
                    String materialSection = tubeSection.substring(start + 10, end);
                    String materialName = "";
                    int paramStart = materialSection.indexOf("<name>");
                    int paramEnd = materialSection.indexOf("</name>");
                    if(paramStart != -1 && paramEnd != -1){
                        materialName = materialSection.substring(paramStart + 6, paramEnd);
                    }
                    String materialDetail = "";
                    paramStart = materialSection.indexOf("<detail>");
                    paramEnd = materialSection.indexOf("</detail>");
                    if(paramStart != -1 && paramEnd != -1){
                        materialDetail = materialSection.substring(paramStart + 8, paramEnd);
                    }
                    int materialId = -1;
                    paramStart = materialSection.indexOf("<id>");
                    paramEnd = materialSection.indexOf("</id>");
                    if(paramStart != -1 && paramEnd != -1){
                        materialId = Integer.parseInt(materialSection.substring(paramStart + 4, paramEnd));
                    }
                    double materialPrice = 0;
                    paramStart = materialSection.indexOf("<price>");
                    paramEnd = materialSection.indexOf("</price>");
                    if(paramStart != -1 && paramEnd != -1){
                        materialPrice = Double.parseDouble(materialSection.substring(paramStart + 7, paramEnd));
                    }
                    
                    double materialOD = 0;
                    paramStart = materialSection.indexOf("<od>");
                    paramEnd = materialSection.indexOf("</od>");
                    if(paramStart != -1 && paramEnd != -1){
                        materialOD = Double.parseDouble(materialSection.substring(paramStart + 4, paramEnd));
                    }
                    double materialWall = 0;
                    paramStart = materialSection.indexOf("<wall>");
                    paramEnd = materialSection.indexOf("</wall>");
                    if(paramStart != -1 && paramEnd != -1){
                        materialWall = Double.parseDouble(materialSection.substring(paramStart + 6, paramEnd));
                    }
                    
                    double materialWeight = 0;
                    paramStart = materialSection.indexOf("<weight>");
                    paramEnd = materialSection.indexOf("</weight>");
                    if(paramStart != -1 && paramEnd != -1){
                        materialWeight = Double.parseDouble(materialSection.substring(paramStart + 8, paramEnd));
                    }
                    
                    double materialRadius = 0;
                    paramStart = materialSection.indexOf("<radius>");
                    paramEnd = materialSection.indexOf("</radius>");
                    if(paramStart != -1 && paramEnd != -1){
                        materialRadius = Double.parseDouble(materialSection.substring(paramStart + 8, paramEnd));
                    }
                    
                    MaterialContainer matCon = new MaterialContainer();
                    matCon.name = materialName;
                    matCon.id = materialId;
                    matCon.detail = materialDetail;
                    matCon.price = materialPrice;
                    matCon.od = materialOD;
                    matCon.wall = materialWall;
                    matCon.weight = materialWeight;
                    matCon.bendRadius = materialRadius;
                    tubeMaterials.addElement(matCon);
                    int oldEnd = end;
                    start = tubeSection.indexOf("<material>", oldEnd + 11);
                    end = tubeSection.indexOf("</material>", oldEnd + 11);
                }
            }
            
            String plateSection = "";
            sectionStart = materialFileContent.indexOf("<plate>");
            sectionEnd = materialFileContent.indexOf("</plate>");
            if(sectionStart != -1 && sectionEnd != -1){
                plateSection = materialFileContent.substring(sectionStart + 7, sectionEnd);
                int start = plateSection.indexOf("<material>");
                int end = plateSection.indexOf("</material>");
                while(start != -1 && end != -1 && start < end &&  start + 10 < plateSection.length() && end < plateSection.length()){
                    String materialSection = plateSection.substring(start + 10, end);
                    String materialName = "";
                    int paramStart = materialSection.indexOf("<name>");
                    int paramEnd = materialSection.indexOf("</name>");
                    if(paramStart != -1 && paramEnd != -1){
                        materialName = materialSection.substring(paramStart + 6, paramEnd);
                    }
                    String materialDetail = "";
                    paramStart = materialSection.indexOf("<detail>");
                    paramEnd = materialSection.indexOf("</detail>");
                    if(paramStart != -1 && paramEnd != -1){
                        materialDetail = materialSection.substring(paramStart + 8, paramEnd);
                    }
                    int materialId = -1;
                    paramStart = materialSection.indexOf("<id>");
                    paramEnd = materialSection.indexOf("</id>");
                    if(paramStart != -1 && paramEnd != -1){
                        materialId = Integer.parseInt(materialSection.substring(paramStart + 4, paramEnd));
                    }
                    double materialPrice = 0;
                    paramStart = materialSection.indexOf("<price>");
                    paramEnd = materialSection.indexOf("</price>");
                    if(paramStart != -1 && paramEnd != -1){
                        materialPrice = Double.parseDouble(materialSection.substring(paramStart + 7, paramEnd));
                    }
                    
                    double materialWall = 0;
                    paramStart = materialSection.indexOf("<wall>");
                    paramEnd = materialSection.indexOf("</wall>");
                    if(paramStart != -1 && paramEnd != -1){
                        materialWall = Double.parseDouble(materialSection.substring(paramStart + 6, paramEnd));
                    }
                    
                    MaterialContainer matCon = new MaterialContainer();
                    matCon.name = materialName;
                    matCon.detail = materialDetail;
                    matCon.id = materialId;
                    matCon.price = materialPrice;
                    matCon.wall = materialWall;
                    plateMaterials.addElement(matCon);
                    int oldEnd = end;
                    start = plateSection.indexOf("<material>", oldEnd + 11);
                    end = plateSection.indexOf("</material>", oldEnd + 11);
                }
            }
            
            String panelSection = "";
            sectionStart = materialFileContent.indexOf("<panel>");
            sectionEnd = materialFileContent.indexOf("</panel>");
            if(sectionStart != -1 && sectionEnd != -1 && sectionStart + 7 < materialFileContent.length() ){
                panelSection = materialFileContent.substring(sectionStart + 7, sectionEnd);
                int start = panelSection.indexOf("<material>");
                int end = panelSection.indexOf("</material>");
                while(start != -1 && end != -1 && start < end &&  start + 10 < panelSection.length() && end < panelSection.length()){
                    String materialSection = panelSection.substring(start + 10, end);
                    String materialName = "";
                    int paramStart = materialSection.indexOf("<name>");
                    int paramEnd = materialSection.indexOf("</name>");
                    if(paramStart != -1 && paramEnd != -1){
                        materialName = materialSection.substring(paramStart + 6, paramEnd);
                    }
                    String materialDetail = "";
                    paramStart = materialSection.indexOf("<detail>");
                    paramEnd = materialSection.indexOf("</detail>");
                    if(paramStart != -1 && paramEnd != -1){
                        materialDetail = materialSection.substring(paramStart + 8, paramEnd);
                    }
                    int materialId = -1;
                    paramStart = materialSection.indexOf("<id>");
                    paramEnd = materialSection.indexOf("</id>");
                    if(paramStart != -1 && paramEnd != -1){
                        materialId = Integer.parseInt(materialSection.substring(paramStart + 4, paramEnd));
                    }
                    double materialPrice = 0;
                    paramStart = materialSection.indexOf("<price>");
                    paramEnd = materialSection.indexOf("</price>");
                    if(paramStart != -1 && paramEnd != -1){
                        materialPrice = Double.parseDouble(materialSection.substring(paramStart + 7, paramEnd));
                    }
                    MaterialContainer matCon = new MaterialContainer();
                    matCon.name = materialName;
                    matCon.detail = materialDetail;
                    matCon.id = materialId;
                    matCon.price = materialPrice;
                    panelMaterials.addElement(matCon);
                    int oldEnd = end;
                    start = panelSection.indexOf("<material>", oldEnd + 11);
                    end = panelSection.indexOf("</material>", oldEnd + 11);
                }
            }
            
            
            DefaultMutableTreeNode tubeDmtn = new DefaultMutableTreeNode("Tube");
            TreePath tubeTp = remoteFileList.addNode(rootTp, tubeDmtn);
            remoteFileList.makeNodeVisible(tubeTp);
            for(int i = 0; i < tubeMaterials.size(); i++){
                MaterialContainer mat = (MaterialContainer)tubeMaterials.elementAt(i);
                //System.out.println(" material " + mat.name );
                DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode(mat);
                TreePath tp = remoteFileList.addNode(tubeTp, dmtn);
                remoteFileList.makeNodeVisible(tp);
                
                if(selectedMaterialIds.contains(mat.id)){
                    JTree jTree = remoteFileList.getComponent();
                    jTree.setSelectionPath(tp);
                }
            }
            
            DefaultMutableTreeNode plateDmtn = new DefaultMutableTreeNode("Plate");
            TreePath plateTp = remoteFileList.addNode(rootTp, plateDmtn);
            remoteFileList.makeNodeVisible(plateTp);
            for(int i = 0; i < plateMaterials.size(); i++){
                MaterialContainer mat = (MaterialContainer)plateMaterials.elementAt(i);
                //System.out.println(" material " + mat.name );
                DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode(mat);
                TreePath tp = remoteFileList.addNode(plateTp, dmtn);
                remoteFileList.makeNodeVisible(tp);
                
                if(selectedMaterialIds.contains(mat.id)){
                    JTree jTree = remoteFileList.getComponent();
                    jTree.setSelectionPath(tp);
                }
            }
            
            DefaultMutableTreeNode panelDmtn = new DefaultMutableTreeNode("Panel");
            TreePath panelTp = remoteFileList.addNode(rootTp, panelDmtn);
            remoteFileList.makeNodeVisible(panelTp);
            for(int i = 0; i < panelMaterials.size(); i++){
                MaterialContainer mat = (MaterialContainer)panelMaterials.elementAt(i);
                //System.out.println(" material " + mat.name );
                DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode(mat);
                TreePath tp = remoteFileList.addNode(panelTp, dmtn);
                remoteFileList.makeNodeVisible(tp);
                
                if(selectedMaterialIds.contains(mat.id)){
                    JTree jTree = remoteFileList.getComponent();
                    jTree.setSelectionPath(tp);
                }
            }
            
            
        } catch (Exception e){
            System.out.println("Error: " + e);
            e.printStackTrace();
        }
    }
    
    /**
     * set
     *
     * Description: Handle a material selection.
     *   Will change the selected arc object diameter, wall thickness and bend radius.
     */
    public void set(){
        TreePath selectionPath = remoteFileList.getSelectedNode();
        if(selectionPath != null){
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)selectionPath.getLastPathComponent();
            //System.out.println("selectedNode: "+ selectedElement.getClass().getName() );
            //Object selectedUserObject = (Object)selectedNode.getUserObject();
            //System.out.println("selected user object: "+ selectedUserObject.getClass().getName() );
            
            MaterialContainer selectedMaterialContainer = (MaterialContainer)selectedNode.getUserObject();
            //System.out.println("Selected Material: " + selectedMaterialContainer );
            
            
            // Get selected scene object to assign the material to.
            int sel[] = window.getScene().getSelection();
            Vector<ObjectInfo> sceneObjects = window.getScene().getObjects();
            for(int i = 0; i < sel.length; i++){
                int selectionIndex = sel[i];
                //System.out.println("selectionIndex: " +  selectionIndex);
                ObjectInfo selectedObject = sceneObjects.elementAt(selectionIndex);
                System.out.println("Selected Object: " + selectedObject.getName());
                
                selectedObject.setPhysicalMaterialId(selectedMaterialContainer.id);
                
                //
                // does the material have the correct attributes?
                //
                if( selectedObject.getObject() instanceof ArcObject ){
                    ArcObject arc = (ArcObject)selectedObject.getObject();
                    //System.out.println(" OD " + arc.getTubeDiameter() + " - " + selectedMaterialContainer.od );
                    //System.out.println(" Wall " + arc.getTubeWall() + " - " + selectedMaterialContainer.wall );
                    
                    double odDiff = Math.abs( arc.getTubeDiameter() - selectedMaterialContainer.od );
                    
                    double scaledTubeDiameter = selectedMaterialContainer.od;
                    double scaledTubeWall = selectedMaterialContainer.wall;
                    double scaledBendRadius = selectedMaterialContainer.bendRadius;
                    
                    if(window != null){
                        //window.scaleSettings
                        String symbol = window.scaleSettings.getSymbol();
                        //System.out.println("  --- symbol: " + symbol  + " - " + window.scaleSettings.getInchScaleValue());
                        
                        scaledTubeDiameter = scaledTubeDiameter * window.scaleSettings.getInchScaleValue();
                        scaledTubeWall = scaledTubeWall * window.scaleSettings.getInchScaleValue();
                        scaledBendRadius = scaledBendRadius * window.scaleSettings.getInchScaleValue();
                    }
                    
                    arc.setTubeDiameter(scaledTubeDiameter);
                    arc.setTubeWall(scaledTubeWall);
                    if(scaledBendRadius > 0){
                        arc.setBendRadius(scaledBendRadius);
                    }
                    //selectedObject.clearCachedMeshes();
                    
                    // Update any mirror object replications.
                    ObjectInfo [] children = selectedObject.getChildren();
                    for(int c = 0; c < children.length; c++){
                        ObjectInfo chlid = (ObjectInfo)children[c];
                        if(chlid.getObject() instanceof MirrorPlaneObject){
                            ObjectInfo [] mirrored = chlid.getChildren();
                            for(int d = 0; d < mirrored.length; d++){
                                ObjectInfo replicated = (ObjectInfo)mirrored[d];
                                if(replicated.getObject() instanceof ArcObject){
                                    replicated.setPhysicalMaterialId(selectedMaterialContainer.id);
                                    ArcObject replicatedArc = (ArcObject)replicated.getObject();
                                    //replicatedArc.setTubeDiameter(selectedMaterialContainer.od); // Doesn't use correct value
                                    //replicatedArc.setTubeWall(selectedMaterialContainer.wall);   // Doesn't use correct value
                                    
                                    replicatedArc.setTubeDiameter(scaledTubeDiameter);
                                    replicatedArc.setTubeWall(scaledTubeWall);
                                    if(scaledBendRadius > 0){
                                        replicatedArc.setBendRadius(scaledBendRadius);
                                    }
                                    
                                    replicated.clearCachedMeshes();
                                }
                            }
                        }
                    }
                    
                    window.updateImage();
                }
                
                //
                // if Object3D is Curve representing plate, then set its thickness property.
                //
                if( selectedObject.getObject() instanceof Curve ){
                    Curve curve = (Curve)selectedObject.getObject();
                    curve.setPlateMaterialThickness( selectedMaterialContainer.wall );
                    
                    //curve.clearCachedMesh(); // protected method.
                    curve.setClosed(curve.isClosed()); // calls clear cache
                    window.updateImage();
                }
            }
            
            
            remoteFileList.setEnabled(true);
            dispose();
        }
    }
    
    /**
     * getMaterialPrice
     *
     * Description: get an invoice line item for the material of an object.
     *
     * @param: Vector resulting line items with properties ()
     * @param: ObjectInfo info : object to process.
     * @param: double margin - multiplyer for material price.
     * @return: Vector line items.
     */
    public Vector<Vector<String>> getMaterialPrice(Vector<Vector<String>> lineItems, ObjectInfo info, double margin){
        double scale = window.getScene().getScale();
        //System.out.println(" scale: " + scale );
        ScaleSettings scaleSettings = new ScaleSettings(window);
        double sceneScale = scaleSettings.getInchScaleValue();
        //System.out.println(" sceneScale: " + sceneScale );
        double exportScale = scaleSettings.getExportScaleValue();
        //System.out.println(" exportScale: " + exportScale );
    
        //scale = scale * exportScale;
        scale = scale * (1 / sceneScale); // converts scene units to inch values
        
        //Vector<Vector<String>> lineItems = new Vector<Vector<String>>();
        if(info.getObject() instanceof ArcObject){
            int objectMaterialId = info.getPhysicalMaterialId();
            for(int m = 0; m < tubeMaterials.size(); m++){
                MaterialContainer mat = tubeMaterials.elementAt(m);
                //System.out.println(" obj id " + objectMaterialId+ " " + mat.id  );
                if(objectMaterialId == mat.id){
                    ArcObject arc = (ArcObject)info.getObject();
                    double arcLength = arc.getLength();
                    //double scale = window.scaleSettings.getInchScaleValue();
                    
                    arcLength *= scale;
                    double arcLengthInches = arcLength;
                    double arcLengthFeet = arcLength / 12;
                    
                    //arcLengthFeet *= 1.15; // Add percent for scrap durring fixturing and placement.
                    
                    double price = arcLengthFeet * mat.price;
                    double weight = arcLengthFeet * mat.weight;
                    
                    //System.out.println(" arc " + info.getName() + " len: " + arcLength + " scale: " + scale + " price: " + price + "  det " + mat.detail );
                    boolean found = false;
                    for(int l = 0; l < lineItems.size(); l++){          // accumulate new length to existing line items for this material type.
                        Vector<String> row = lineItems.elementAt(l);
                        
                        if(row.size() > 3 && row.elementAt(0).equals(mat.detail)){
                            double quantity = Double.parseDouble(row.elementAt(1));
                            quantity += arcLengthFeet;
                            row.setElementAt("" + quantity, 1);
                            
                            //row.elementAt(2) // don't need to update unit price.
                            
                            double currPrice = Double.parseDouble(row.elementAt(3));
                            currPrice += price;
                            row.setElementAt("" + currPrice, 3);
                            
                            // Tally Weight
                            double currWeight = Double.parseDouble(row.elementAt(4));
                            currWeight += weight;
                            row.setElementAt("" + currWeight, 4);
                            
                            found = true;
                        }
                    }
                    if(found == false){                 // This type of material is new
                        Vector<String> row = new Vector<String>();
                        row.addElement(mat.detail);
                        row.addElement(""+arcLengthFeet);
                        row.addElement(""+mat.price);
                        row.addElement(""+price);
                        row.addElement(""+weight); //    Weight
                        row.addElement("'");        // Feet units
                        lineItems.addElement(row);
                    }
                }
            }
        }
        return lineItems;
    }
    
    
    /**
     * getMaterial
     *
     * Description: get a struc of properties for the given object.
     */
    public MaterialContainer getMaterial(ObjectInfo info){
        MaterialContainer mat = null;
        if(info.getObject() instanceof ArcObject){
            int objectMaterialId = info.getPhysicalMaterialId();
            for(int m = 0; m < tubeMaterials.size(); m++){
                MaterialContainer currMat = tubeMaterials.elementAt(m);
                if(objectMaterialId == currMat.id){
                    mat = currMat;
                }
            }
            
            for(int m = 0; m < plateMaterials.size(); m++){
                MaterialContainer currMat = plateMaterials.elementAt(m);
                if(objectMaterialId == currMat.id){
                    mat = currMat;
                }
            }
            
            for(int m = 0; m < panelMaterials.size(); m++){
                MaterialContainer currMat = panelMaterials.elementAt(m);
                if(objectMaterialId == currMat.id){
                    mat = currMat;
                }
            }
        }
        return mat;
    }
    
    
    /**
     * getMaterialById
     *
     * Description: Get material information form material id.
     */
    public MaterialContainer getMaterialById(int matId){
        MaterialContainer mat = null;
        for(int m = 0; m < tubeMaterials.size(); m++){
            MaterialContainer currMat = tubeMaterials.elementAt(m);
            if(matId == currMat.id){
                mat = currMat;
            }
        }
        
        for(int m = 0; m < plateMaterials.size(); m++){
            MaterialContainer currMat = plateMaterials.elementAt(m);
            if(matId == currMat.id){
                mat = currMat;
            }
        }
        
        for(int m = 0; m < panelMaterials.size(); m++){
            MaterialContainer currMat = panelMaterials.elementAt(m);
            if(matId == currMat.id){
                mat = currMat;
            }
        }
        
        return mat;
    }
    
    /**
     *
     *
     */
    public void load(){
        
    }
    
    
    /**
     *
     *
     */
    public void doSelectionChanged()
    {
      TreePath selection = remoteFileList.getSelectedNode();
        
        
    }
    
    /**
     *
     *
     */
    public void mouseClicked(MouseClickedEvent ev)
    {
      if (ev.getClickCount() == 2)
      {
        //doEdit();
      }
      else if (ev.getClickCount() == 1)
      {
        //doSelectionChanged();
      }
    }
    
    
    /**
     *
     *
     */
    private void doCancel()
    {
      dispose();
    }
    
    /**
     * FileTreeRenderer Cell Class
     *
     */
    public class FileTreeRenderer extends DefaultTreeCellRenderer {
      private static final String SPAN_FORMAT = "<span style='color:%s;'>%s</span>";
      private final ImageIcon fileIcon;

      public FileTreeRenderer() {
          fileIcon = new ImageIcon(getClass().getResource("/armarender/Icons/ads.png"));
      }

      @Override
      public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                    boolean leaf, int row, boolean hasFocus) {
          super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
          DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
          Object userObject = node.getUserObject();
          
          this.setBackgroundSelectionColor(Color.LIGHT_GRAY);
          this.setBorderSelectionColor(Color.LIGHT_GRAY);
          
          if (userObject instanceof String) {
              String pp = (String) userObject;
              String text = String.format(SPAN_FORMAT, "black", pp);
              //text += " [" + String.format(SPAN_FORMAT, "orange", pp.()) + "]";
              this.setText("<html>" + text + "</html>");
              this.setIcon(fileIcon);
          }
          return this;
      }
    }
}


