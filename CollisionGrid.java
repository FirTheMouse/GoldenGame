package com.golden.game;

import java.util.*;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

public class CollisionGrid {
    private Map<GridCell, List<Data>> grid = new HashMap<>();
    private float cellSize;
    
    public CollisionGrid(float cellSize) {
        this.cellSize = cellSize;
    }
    
    private static class GridCell {
        final int x, z;
        
        public GridCell(int x, int z) {
            this.x = x;
            this.z = z;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GridCell cell = (GridCell) o;
            return x == cell.x && z == cell.z;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(x, z);
        }
    }
    
    private GridCell getCell(Vector3f position) {
        int x = (int)(position.x / cellSize);
        int z = (int)(position.z / cellSize);
        return new GridCell(x, z);
    }
    
    public void addSolid(List<Vector3f> points, Data entity) {
        for (Vector3f point : points) {
            GridCell cell = getCell(point);
            grid.computeIfAbsent(cell, k -> new ArrayList<>()).add(entity);
        }
    }

    public void removeSolid(List<Vector3f> points, Data entity) {
        for (Vector3f point : points) {
            GridCell cell = getCell(point);
            if (grid.containsKey(cell)) {
                grid.get(cell).remove(entity);
                if (grid.get(cell).isEmpty()) {
                    grid.remove(cell);
                }
            }
        }
    }
    
    public boolean checkPath(Vector3f start, Vector3f end) {
        // Improved line check with better resolution and margin
        Vector3f dir = end.subtract(start);
        float distance = dir.length();
        dir.normalizeLocal();
        
        // Use smaller step size for more precise collision detection
        float stepSize = cellSize * 0.5f; // Half cell size for better precision
        int steps = (int)(distance / stepSize) + 1; // +1 to ensure we check the end point
        
        System.out.println(" [CP] Checking path from " + start + " to " + end + " with " + steps + " steps");
        
        for (int i = 0; i < steps; i++) {
            // Calculate current point along the path
            Vector3f point = start.add(dir.mult(i * stepSize));
            
            if (isSolid(point)) {
                System.out.println(" [CP] Solid intersection at step " + i + ": " + point);
                return true; // Found collision
            }
            
            // Optional: Check slightly to the sides for wider objects
            Vector3f perpendicular = new Vector3f(-dir.z, 0, dir.x).normalizeLocal();
            float margin = cellSize * 3f; // Adjust based on character width
            
            Vector3f sidePoint1 = point.add(perpendicular.mult(margin));
            Vector3f sidePoint2 = point.add(perpendicular.mult(-margin));
            
            if (isSolid(sidePoint1) || isSolid(sidePoint2)) {
                System.out.println(" [CP] Side collision detected at step " + i);
                return true; // Found side collision
            }
        }
        
        System.out.println(" [CP] Path is clear");
        return false; // No collision found
    }
    
    public boolean isSolid(Vector3f point) {
        GridCell cell = getCell(point);
        return grid.containsKey(cell) && !grid.get(cell).isEmpty();
    }
    
    public boolean isSolidArea(Vector3f point, Vector3f dimensions) {
        // Check area based on dimensions
        Vector3f halfExtents = dimensions.mult(0.5f);
        
        GridCell minCell = getCell(point.subtract(halfExtents));
        GridCell maxCell = getCell(point.add(halfExtents));
        
        for (int x = minCell.x; x <= maxCell.x; x++) {
            for (int z = minCell.z; z <= maxCell.z; z++) {
                GridCell cell = new GridCell(x, z);
                if (grid.containsKey(cell) && !grid.get(cell).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public Vector3f findClearPath(Vector3f start, Vector3f end) {
        // Simple implementation - try offset points until we find a clear one
        Vector3f dir = end.subtract(start);
        Vector3f perpendicular = new Vector3f(-dir.z, 0, dir.x).normalizeLocal();
        System.out.println("   [FCP] Finding a clear path from "+start+" to "+end);
        
        // Determine the total distance between start and end
        float totalDistance = dir.length();
        float stepBack = totalDistance / 10; // Divide path into segments
        
        for (float backDist = 0; backDist < totalDistance * 0.9f; backDist += stepBack) {
            // Calculate a point that's moving backward from end toward start
            Vector3f backPoint = end.subtract(dir.normalize().mult(backDist));
            
            for (float offset = cellSize; offset < cellSize * 30; offset += (cellSize/2)) {
                // Try both sides of the path at this backward point
                Vector3f tryPoint1 = backPoint.add(perpendicular.mult(offset));
                Vector3f tryPoint2 = backPoint.add(perpendicular.mult(-offset));
                
                if (!isSolid(tryPoint1)) {
                    System.out.println(" 	[FCP] Point 1 chosen at back distance "+backDist+" and offset "+offset);
                    return tryPoint1;
                }
                if (!isSolid(tryPoint2)) {
                    System.out.println(" 	[FCP] Point 2 chosen at back distance "+backDist+" and offset "+offset);
                    return tryPoint2;
                }
            }
        }
        
        System.out.println("Impossible pathing");
        return end; // If we can't find a clear path, return original endpoint
    }
    
    public Node createDebugNode(AssetManager assetManager) {
        Node debugNode = new Node("gridDebug");
        
        // Create materials
        Material occupiedMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        occupiedMat.setColor("Color", ColorRGBA.Red);
        
        Material emptyMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        emptyMat.setColor("Color", ColorRGBA.Yellow);
        
        // Create a box for each cell
        for (Map.Entry<GridCell, List<Data>> entry : grid.entrySet()) {
            GridCell cell = entry.getKey();
            boolean isOccupied = !entry.getValue().isEmpty();
            
            // Create cell visual
            Box box = new Box(cellSize/2, 0.1f, cellSize/2);  // Half size because Box extends from center
            Geometry geom = new Geometry("cell_" + cell.x + "_" + cell.z, box);
            
            // Set position
            float worldX = cell.x * cellSize + cellSize/2;  // Center of cell
            float worldZ = cell.z * cellSize + cellSize/2;
            geom.setLocalTranslation(worldX, 0.1f, worldZ);  // Slight Y offset to be visible
            
            // Set material based on occupancy
            geom.setMaterial(isOccupied ? occupiedMat : emptyMat);
            
            debugNode.attachChild(geom);
        }
        
        return debugNode;
    }

    // Method to update the visualization
    public void updateDebugNode(Node debugNode, AssetManager assetManager) {
        // Clear existing debug visuals
        debugNode.detachAllChildren();
        
        // Create materials once
        Material occupiedMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        occupiedMat.setColor("Color", ColorRGBA.Red);
        
        Material emptyMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        emptyMat.setColor("Color", ColorRGBA.Green);
        
        // Create a box for each existing cell in the grid
        for (Map.Entry<GridCell, List<Data>> entry : grid.entrySet()) {
            GridCell cell = entry.getKey();
            boolean isOccupied = !entry.getValue().isEmpty();
            
            // Only create visual if the cell has contents
            if (isOccupied) {
                Box box = new Box(cellSize/2, 0.1f, cellSize/2);
                Geometry geom = new Geometry("cell_" + cell.x + "_" + cell.z, box);
                
                float worldX = cell.x * cellSize + cellSize/2;
                float worldZ = cell.z * cellSize + cellSize/2;
                geom.setLocalTranslation(worldX, 0.1f, worldZ);
                
                geom.setMaterial(occupiedMat);
                debugNode.attachChild(geom);
            }
        }
    }
}
