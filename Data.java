package com.golden.game;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import java.util.*;

public class Data {
    // Existing type definitions
    public static final Class<String> S = String.class;
    public static final Class<Integer> I = Integer.class;
    public static final Class<Double> F = Double.class;
    public static final Class<Boolean> B = Boolean.class;
    public static final Class<Data> D = Data.class;
    public static final Class<Float> L = Float.class;
    
    // New type definitions for 3D
    public static final Class<Vector3f> V3 = Vector3f.class;
    public static final Class<Quaternion> Q = Quaternion.class;
    public static final Class<Mesh> M = Mesh.class;
    public static final Class<Node> N = Node.class;
    public static final Class<Spatial> SP = Spatial.class;
    public static final Class<ColorRGBA> RGBA = ColorRGBA.class;
    public static final Class<BoundingBox> BB = BoundingBox.class;
    
    
    // Existing collection types
    @SuppressWarnings("unchecked")
    public static final Class<ArrayList<Node>> NL = (Class<ArrayList<Node>>) (Class<?>) ArrayList.class;
    @SuppressWarnings("unchecked")
    public static final Class<ArrayList<Data>> DL = (Class<ArrayList<Data>>) (Class<?>) ArrayList.class;
    @SuppressWarnings("unchecked")
    public static final Class<ArrayList<Integer>> IL = (Class<ArrayList<Integer>>) (Class<?>) ArrayList.class;
    @SuppressWarnings("unchecked")
    public static final Class<ArrayList<String>> SL = (Class<ArrayList<String>>) (Class<?>) ArrayList.class;
    @SuppressWarnings("unchecked")
    public static final Class<HashMap<String,Integer>> DH = (Class<HashMap<String,Integer>>) (Class<?>) HashMap.class;
    @SuppressWarnings("unchecked")
    public static final Class<HashMap<String,Data>> DS = (Class<HashMap<String,Data>>) (Class<?>) HashMap.class;
    @SuppressWarnings("unchecked")
    public static final Class<List<Data>> PL = (Class<List<Data>>) (Class<?>) List.class;
    @SuppressWarnings("unchecked")
    public static final Class<HashMap<Integer,List<String>>> SLH = (Class<HashMap<Integer,List<String>>>) (Class<?>) HashMap.class;
    @SuppressWarnings("unchecked")
    public static final Class<ArrayList<HashMap<String,Integer>>> DLH = (Class<ArrayList<HashMap<String,Integer>>>) (Class<?>) ArrayList.class;
    public static final Class<Data[]> DA = Data[].class;

    private Object[] notes;
    private final Map<String, Object> notestorage = new HashMap<>();
    
    // Helper methods for 3D coordinates
    public Vector3f getPosition() {
        if (hasNote("position")) {
            return getNote("position", V3);
        } else {
            // Legacy support: construct Vector3f from x,y,z or x,y notes
            float x = hasNote("x") ? ((Number)getNote("x", F)).floatValue() : 0f;
            float y = hasNote("y") ? ((Number)getNote("y", F)).floatValue() : 0f;
            float z = hasNote("z") ? ((Number)getNote("z", F)).floatValue() : 0f;
            return new Vector3f(x, y, z);
        }
    }
    
    public Quaternion getRotation()
    {
    	if(hasNote("rotation"))
    		return getNote("rotation",Q);
    	else
    		return new Quaternion(0f,0f,0f,0f);
    }
    
    public void setRotation(Quaternion rotation)
    {
    	getModel().rotate(rotation);
    }
    
    
    public BoundingBox getbBox()
    {
    	if(hasNote("bbox"))
    		return getNote("bbox",BB);
    	else
    		return null;
    }

    public void setPosition(Vector3f pos) {
        setNote("position", pos);
        // Legacy support: also set individual coordinates
        setNote("x", (double)pos.x);
        setNote("y", (double)pos.y);
        setNote("z", (double)pos.z);
    }
    
    public Spatial getModel()
    {
     	if(hasNote("spatial"))
    		return getNote("spatial",SP);
    	else
    		return null;
    }

    public void setModel(Mesh mesh) {
        setNote("model", mesh);
    }

    public void setNode(Node node) {
        setNote("node", node);
    }

    // Existing constructor
    public Data(Object... notes) {
        this.notes = notes;
        parcelPairs();
    }

    // Modified helper method for type conversion
    @SuppressWarnings("unchecked")
    public <T> T getNote(String label, Class<T> type) {
        if (notestorage.containsKey(label)) {
            Object item = notestorage.get(label);
            
            // Handle numeric type conversions
            if (type == F && item instanceof Number) {
                return (T) Double.valueOf(((Number)item).doubleValue());
            } else if (type == I && item instanceof Number) {
                return (T) Integer.valueOf(((Number)item).intValue());
            } else if (type.isInstance(item)) {
                return (T) item;
            }
        }
        return null;
    }

    // Existing methods with no changes needed
    public Object[] getNotes() {
        return this.notes;
    }

    public void processDTO(Data dto) {
        System.out.println("***********");
        int pos = 0;
        for (Object notes : dto.getNotes()) {
            String displaynote = notes.toString();
            if (displaynote.startsWith("|")) {
                displaynote = "";
            }
            
            if (pos % 2 != 0) {
                System.out.println(displaynote);
                pos++;
            } else {
                System.out.print(displaynote);
                pos++;
            }
        }
        System.out.println("***********");
    }

    public void parcelPairs() {
        for (int i = 0; i < notes.length - 1; i += 2) {
            if (notes[i] instanceof String) {
                packNote((String)notes[i], notes[i + 1]);
            }
        }
    }

    public void packNote(String label, Object info) {
        String newLabel = label.replaceAll(":\\s*", "").toLowerCase();
        notestorage.put(newLabel, info);
    }

    public void setNote(String label, Object info) {
        String newLabel = label.replaceAll(":\\s*", "").toLowerCase();
        notestorage.put(newLabel, info);
        
        for (int i = 0; i < notes.length - 1; i += 2) {
            if (notes[i] instanceof String && 
                ((String)notes[i]).replaceAll(":\\s*", "").toLowerCase().equals(newLabel)) {
                notes[i + 1] = info;
                return;
            }
        }
        
        Object[] newNotes = new Object[notes.length + 2];
        System.arraycopy(notes, 0, newNotes, 0, notes.length);
        newNotes[notes.length] = label;
        newNotes[notes.length + 1] = info;
        notes = newNotes;
    }

    public boolean hasNote(String label) {
        String newLabel = label.replaceAll(":\\s*", "").toLowerCase();
        return notestorage.containsKey(newLabel);
    }

    public void addNote(String label, Object info) {
        String newLabel = label.replaceAll(":\\s*", "").toLowerCase();
        if (!notestorage.containsKey(newLabel)) {
            notestorage.put(newLabel, info);
            
            Object[] newNotes = new Object[notes.length + 2];
            System.arraycopy(notes, 0, newNotes, 0, notes.length);
            newNotes[notes.length] = label;
            newNotes[notes.length + 1] = info;
            notes = newNotes;
        }
    }
}