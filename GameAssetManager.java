package com.golden.game;

import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Box;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class GameAssetManager {
    private static final Logger logger = Logger.getLogger(GameAssetManager.class.getName());
    private AssetManager assetManager;
    private Generator gen = new Generator();
    private Map<String,List<Data>> workSpots = new HashMap<>();
    private Map<String,List<Data>> storageSpots = new HashMap<>();
    private Map<String,List<Data>> snapSpots = new HashMap<>();
    
    public GameAssetManager(Generator gen, AssetManager assetManager) {
        this.gen = gen;
        this.assetManager = assetManager;
        setupAssetDirectory();
    }
    
    public void setupAssetDirectory() {
        String userDir = System.getProperty("user.dir");
        File assetsDir = new File(userDir, "assets");
        
        if (!assetsDir.exists()) {
            assetsDir.mkdir();
            new File(assetsDir, "Models").mkdir();
            logger.info("Created assets directory at: " + assetsDir.getAbsolutePath());
        }
        
        assetManager.registerLocator(assetsDir.getAbsolutePath(), FileLocator.class);
    }
    
    public Map<String,List<Data>> getWorkSpots() {
        return workSpots;
    }

    public Map<String,List<Data>> getStorageSpots() {
        return storageSpots;
    }
    
    public Map<String,List<Data>> getSnapSpots() {
        return snapSpots;
    }
    
    
    // Caches all models at the start of the game [LM]
    public void loadModels(Map<String,Spatial> modelCache) {
        try {
            // Load and cache building models
            for (String buildingType : gen.buildingTypes) {
                String modelPath = "Models/" + buildingType + ".obj";
                if (checkModelFile(modelPath)) {
                	Spatial model = loadModelWithMaterials(modelPath);
                	inspectModelGeometry(model, buildingType);
                    modelCache.put(buildingType, model);
                } else {
                    modelCache.put(buildingType, createDefaultModel(buildingType));
                }
            }
            
            for (String rNodeType : gen.rNodeTypes) {
                String modelPath = "Models/" + rNodeType + ".obj";
                if (checkModelFile(modelPath)) {
                	Spatial model = loadModelWithMaterials(modelPath);
                	inspectModelGeometry(model,rNodeType);
                    modelCache.put(rNodeType, model);
                } else {
                    modelCache.put(rNodeType, createDefaultModel(rNodeType));
                }
            }
            
            // Load agent model
            for(String type : gen.agentTypes)
            {
	            String agentPath = "Models/"+type+".obj";
	            System.out.println("[LM] agent path: "+agentPath);
	            if (checkModelFile(agentPath)) 
	            {
	            	Spatial model = loadModelWithMaterials(agentPath);
	            	inspectModelGeometry(model, type);
	                modelCache.put(type, model);
	            } else {
	            	System.out.println("[LM] No agent model found");
	                modelCache.put(type, createDefaultModel("agent"));
	            }
            }
            
            // Load road model
            Spatial roadModel = createRoadModel();
            if (roadModel != null) {
                modelCache.put("road", roadModel);
                System.out.println("Road model loaded successfully");
            } else {
                System.err.println("Failed to create road model");
            }
        } catch (Exception e) {
            System.err.println("Error loading models: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void inspectModelGeometry(Spatial model, String modelType) {
        System.out.println("\n[INSPECT] Model: " + modelType);
        
        if (model instanceof Node) {
            Node node = (Node) model;
            System.out.println("Root node contains " + node.getChildren().size() + " children");
            
            // Recursively inspect child nodes and geometries
            inspectChild(node, 0,model,modelType);
        } else if (model instanceof Geometry) {
            System.out.println("Model is a single geometry: " + model.getName());
            printGeometryDetails((Geometry)model, 0);
        } else {
            System.out.println("Unknown spatial type: " + model.getClass().getName());
        }
    }

    private void inspectChild(Spatial spatial, int depth, Spatial model,String modelType) {
        String indent = "  ".repeat(depth);
        
        if (spatial instanceof Node) {
            Node node = (Node)spatial;
            System.out.println(indent + "Node: " + node.getName());
            for (Spatial child : node.getChildren()) 
            {
            	if(child instanceof Geometry)
            	{
            	System.out.println((indent+"name: "+child.getName()));
                inspectChild(child, depth + 1,model,modelType);
            	}
            	else {inspectChild(child, depth + 1,model,modelType);}
            }
        } else if (spatial instanceof Geometry) {
            Geometry geom = (Geometry)spatial;
            processSpot(geom.getName(),geom,model,modelType);
        }
    }

    private void printGeometryDetails(Geometry geom, int depth) {
        String indent = "  ".repeat(depth);
        System.out.println(indent + "Geometry: " + geom.getName());
        
        // Get mesh details
        Mesh mesh = geom.getMesh();
        FloatBuffer positions = mesh.getFloatBuffer(VertexBuffer.Type.Position);
        positions.rewind();
        
        // Print first vertex position as sample
        System.out.println(indent + "  First vertex at: (" + 
            positions.get() + ", " + 
            positions.get() + ", " + 
            positions.get() + ")");
        positions.rewind();
        
        Vector3f position = new Vector3f(positions.get(),positions.get(),positions.get());
    }
    
    private void processSpot(String name, Geometry geom,Spatial model,String type) {

        String[] parts = name.split("_");
        Vector3f position = null;
        
        if (parts[0].equals("w")) {
        	System.out.println("\n[PSN] Name: "+name);
            // Work spot: w_cut_mouse-fox-raven
            String workType = parts[1];
            String[] species = parts[2].split("-");
            position = getGeometryPosition(geom);
            System.out.println("[PSN] Rotation: "+geom.getLocalRotation());
            System.out.println("[PSN] Position: "+position+"\n work type: "+workType+"\n Allowed species: "+Arrays.asList(species));
            Data workSpot = gen.makeWorkSpot(position, workType, Arrays.asList(species));
            workSpots.putIfAbsent(type, new ArrayList<Data>());
            workSpots.get(type).add(workSpot);
            System.out.println("[PSN] "+workType+" added to "+type);
        }
        else if (parts[0].equals("s")) {
            // Storage spot: s_rootcut_3_A
        	System.out.println("\n[PSN] Name: "+name);
            String product = parts[1];
            int capacity = Integer.parseInt(parts[2]);
            String viscode = parts[3].split("\\.")[0];
            List<String> visuals = findVisualsN(model,viscode,product);
            position = getGeometryPosition(geom);
            System.out.println("[PSN] Rotation: "+geom.getLocalRotation());
            System.out.println("[PSN] Position: "+position+"\n product: "+product+"\n capacity: "+capacity);
            Data storageSpot = gen.makeStorageSpot(position, product, capacity,visuals,viscode);
            storageSpots.putIfAbsent(type, new ArrayList<Data>());
            storageSpots.get(type).add(storageSpot);
            System.out.println("[PSN] "+product+" added to "+type);
        }
        else if (parts[0].equals("n")) {
            // Snap spot: n_branch_A
        	System.out.println("\n[PSN] Name: "+name);
            String nodetype = parts[1];
            String viscode = parts[2].split("\\.")[0];
            position = getGeometryPosition(geom);
            Data snapSpot = gen.makeSnapSpot(position, nodetype, viscode);
            snapSpots.putIfAbsent(type, new ArrayList<Data>());
            snapSpots.get(type).add(snapSpot);
            System.out.println("[PSN] "+nodetype+" "+viscode+" added to "+type);
        }
        else return;
    }
    
    private List<String> findVisualsN(Spatial model,String viscode,String type)
    {
    	List<String> visuals = new ArrayList<>();
    	if(model instanceof Node)
    	{
    		for(Spatial s : ((Node) model).getChildren())
    		{
    			if(s instanceof Node && s.getName().startsWith("#"))
    			{
    				//System.out.println("[FV] name: "+s.getName());
    				String removedDot = s.getName().contains(".") ? s.getName().split("\\.")[0]: s.getName();
    				//if(s.getName().contains(".")) System.out.println("[FV] dot removed: "+removedDot);
    				if(removedDot.split("_").length>2)
    				{
    				//System.out.println("[FV] "+viscode+" : "+removedDot.split("_")[2]);
    				//System.out.println("[FV] "+type+" : "+s.getName().split("_")[1]);
    				if(removedDot.split("_")[2].equals(viscode)&&s.getName().split("_")[1].equalsIgnoreCase(type))
    				{
    					s.setCullHint(CullHint.Never);
    					visuals.add(s.getName());
        				//System.out.println("[FV] Added visual");
    				}
    				}
    				//else System.out.println("	[FV] Anaomly at "+removedDot);
    				
    			}
    		}
    		System.out.println("[FV] "+visuals.size()+" visuals found");
    		return visuals;
    	}
    	System.out.println("[FV] not a Node");
    	return null;
    }
    
    private Spatial loadModelWithMaterials(String modelPath) {
        try {
            Spatial model = assetManager.loadModel(modelPath);
            ensureMaterialsVisible(model);
            adjustModelMaterials(model);
            return model;
        } catch (Exception e) {
            logger.warning("Error loading model: " + modelPath);
            e.printStackTrace();
            return null;
        }
    }
    
    private void ensureMaterialsVisible(Spatial model) {
        if (model instanceof Node) {
            Node node = (Node) model;
            for (Spatial child : node.getChildren()) {
                ensureMaterialsVisible(child);
            }
        } else if (model instanceof Geometry) {
            Geometry geom = (Geometry) model;
            Material existingMat = geom.getMaterial();
            
            // Create new material while preserving colors from MTL
            Material newMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            newMat.setBoolean("UseMaterialColors", true);
            
            // If we have an existing material, copy its colors
            if (existingMat != null) {
                // Try to get colors from existing material
                ColorRGBA diffuse = (ColorRGBA) existingMat.getParam("Diffuse").getValue();
                if (diffuse != null) {
                    newMat.setColor("Diffuse", diffuse);
                    newMat.setColor("Ambient", diffuse.mult(0.5f));
                }
            } else {
                // Fallback colors if no material exists
                newMat.setColor("Diffuse", ColorRGBA.White);
                newMat.setColor("Ambient", ColorRGBA.White.mult(0.5f));
            }
            
            newMat.setColor("Specular", ColorRGBA.White);
            newMat.setFloat("Shininess", 64f);
            newMat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
            
            geom.setMaterial(newMat);
        }
    }
    
    public Vector3f getGeometryPosition(Geometry geom) {
        FloatBuffer positions = geom.getMesh().getFloatBuffer(VertexBuffer.Type.Position);
        positions.rewind();
        return new Vector3f(positions.get(), positions.get(), positions.get());
    }
    
    private void adjustModelMaterials(Spatial model) {
        if (model instanceof Node) {
            Node node = (Node) model;
            node.descendantMatches(Geometry.class).forEach(geometry -> {
                Material mat = geometry.getMaterial();
                if (mat != null) {
                    mat.setBoolean("UseMaterialColors", true);
                    mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
                }
            });
        }
    }
    
    private Spatial createRoadModel() {
        Box road = new Box(0.5f, 0.1f, 0.5f);
        Geometry roadGeom = new Geometry("road", road);
        
        Material roadMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        roadMat.setBoolean("UseMaterialColors", true);
        roadMat.setColor("Ambient", new ColorRGBA(0.2f, 0.2f, 0.2f, 1.0f));
        roadMat.setColor("Diffuse", new ColorRGBA(0.4f, 0.4f, 0.4f, 1.0f));
        
        roadGeom.setMaterial(roadMat);
        return roadGeom;
    }
    
    private Spatial createDefaultModel(String type) {
        Box box = new Box(1, 1, 1);
        Geometry geom = new Geometry(type + "_default", box);
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        
        // Set up material properties
        mat.setBoolean("UseMaterialColors", true);
        
        // Different colors for different types
        ColorRGBA color;
        switch (type) {
            case "agent":
                color = ColorRGBA.Blue;
                break;
            case "road":
                color = ColorRGBA.Gray;
                geom.scale(0.5f, 0.1f, 2f);
                break;
            default: // buildings
                color = ColorRGBA.Brown;
                break;
        }
        
        // Apply colors to material
        mat.setColor("Ambient", color);
        mat.setColor("Diffuse", color);
        mat.setColor("Specular", ColorRGBA.White);
        mat.setFloat("Shininess", 64f);
        
        geom.setMaterial(mat);
        return geom;
    }
    
    private boolean checkModelFile(String path) {
        File file = new File(System.getProperty("user.dir"), "assets/" + path);
        return file.exists();
    }
    
    private void checkMTLFile(String modelPath) {
        String mtlPath = modelPath.substring(0, modelPath.lastIndexOf('.')) + ".mtl";
        File mtlFile = new File(System.getProperty("user.dir"), "assets/" + mtlPath);
        
        System.out.println("\nChecking MTL file: " + mtlFile.getAbsolutePath());
        if (mtlFile.exists()) {
            System.out.println("MTL file exists!");
            try {
                List<String> lines = Files.readAllLines(mtlFile.toPath());
                System.out.println("MTL contents:");
                lines.forEach(System.out::println);
            } catch (IOException e) {
                System.err.println("Error reading MTL file: " + e.getMessage());
            }
        } else {
            System.out.println("MTL file not found!");
        }
    }
    
    private void debugMaterials(Spatial model) {
        System.out.println("Debugging materials for: " + model.getName());
        
        if (model instanceof Node) {
            Node node = (Node) model;
            for (Spatial child : node.getChildren()) {
                if (child instanceof Geometry) {
                    Geometry geom = (Geometry) child;
                    Material mat = geom.getMaterial();
                    System.out.println("Geometry: " + geom.getName());
                    System.out.println("Material: " + mat);
                    if (mat != null) {
                        mat.getParams().forEach(param -> 
                            System.out.println("  Param: " + param.getName() + " = " + param.getValue())
                        );
                    }
                }
            }
        } else if (model instanceof Geometry) {
            Geometry geom = (Geometry) model;
            Material mat = geom.getMaterial();
            System.out.println("Direct Geometry: " + geom.getName());
            System.out.println("Material: " + mat);
        }
    }
    
}
