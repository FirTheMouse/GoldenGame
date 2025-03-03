package com.golden.game;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.util.*;
import static com.golden.game.Data.*;

public class TransportNetwork {
    private Generator gen;
    private CollisionGrid col;
    public List<Data> nodes = new ArrayList<>();
    public List<Data> tempNodes = new ArrayList<>();
    public Map<String, Data> pathMap = new HashMap<>();
    public Map<Data, List<Vector3f>> agentMovment = new HashMap<>();
    
    public int namecounter = 0;

    public TransportNetwork(Generator generator, CollisionGrid collisionGrid) {
        this.gen = generator;
        this.col = collisionGrid;
        for(String p : gen.pathTypes) {
            pathMap.put(p, gen.generateRoadNode(new Vector3f(0, 0, 0), p));
        }
    }

    public Data createNode(Vector3f position, String pathType) {
        Data node = gen.generateRoadNode(position, pathType);
        nodes.add(node);
        namecounter++;
        node.addNote("name", namecounter);
        return node;
    }

    public Data createTempNode(Vector3f position, String pathType) {
        Data node = gen.generateRoadNode(position, pathType);
        node.addNote("temp", true);
        tempNodes.add(node);
        return node;
    }

    public void connectNodes(Data node1, Data node2) {
        if (!node1.getNote("connections", DL).contains(node2)) {
            node1.getNote("connections", DL).add(node2);
            node2.getNote("connections", DL).add(node1);
        }
    }
    public void moveAgent(Data agent, Vector3f target) {
        Vector3f start = agent.getPosition();
        agent.setPosition(target);

        // Add the current position
        addToMap(agent, start);
        
        // Add some intermediate points
        Vector3f direction = target.subtract(start);
        float distance = direction.length();
        int steps = Math.max(3, (int)(distance / 2.0f)); // At least 3 points, more for longer distances
        
        for(int i = 1; i < steps; i++) {
            float progress = i / (float)steps;
            Vector3f intermediate = start.add(direction.mult(progress));
            addToMap(agent, intermediate);
        }
        
        // Add the target position
        addToMap(agent, target);
        
        agent.setNote("speed_left", agent.getNote("speed_left", I) - 1);
    }

    
    public void addToMap(Data agent, Vector3f position) {
        agentMovment.putIfAbsent(agent, new ArrayList<Vector3f>());
        // Convert 2D positions to 3D, keeping y constant
        Vector3f pos3D = new Vector3f(position.x, 0, position.z);  // Using z for y-coordinate in 2D
        agentMovment.get(agent).add(pos3D);
    }

    public void updateAgentMovement(Data agent) {
        List<Data> path = agent.getNote("remainingpath", PL);
        if (path == null || path.isEmpty()) return;

        Data currentNode = path.get(0);
        if (path.size() < 2) {
            path.remove(0);
            agent.setNote("remainingpath", path);
            return;
        }

        Data nextNode = path.get(1);

        if (!currentNode.getNote("type", S).equals("dev") &&
            !nextNode.getNote("type", S).equals("dev") &&
            !areNodesConnected(currentNode, nextNode) &&
        	!currentNode.hasNote("temp") &&
        	!nextNode.hasNote("temp")){

        	System.out.println("[UAM] Creating a detour pathway");
            List<Data> detourPath = createPath(
                currentNode.getPosition(),
                nextNode.getPosition(),
                "off"
            );
            path.remove(0);
            detourPath.addAll(path);
            agent.setNote("remainingpath", detourPath);
            return;
        }
        // Get the direction vector from current position to target
        Vector3f currentPos = agent.getModel().getWorldTranslation();
        Vector3f direction = nextNode.getPosition().subtract(currentPos);
        // Zero out the Y component to keep rotation only on XZ plane
        direction.y = 0;
        direction.normalizeLocal();
        // Get the angle between the forward vector (Z-axis) and our direction
        float angle = FastMath.atan2(direction.x, direction.z);
        Quaternion rotation =  new Quaternion().fromAngleAxis(angle, Vector3f.UNIT_Y);
        agent.getModel().setLocalRotation(rotation);

        moveAgent(agent, nextNode.getPosition());
        path.remove(0);
        agent.setNote("remainingpath", path);
    }
    
    
	private float clamp(float value, float min, float max) {
	    return Math.max(min, Math.min(value, max));
	}

    public List<Data> moveToBuilding(Vector3f agentPos, Data building,Data agent) {
        Data nearestToAgent = findNearestNode(agentPos);
        Vector3f nearestNodePos = nearestToAgent.getPosition();
        List<Data> completePath = new ArrayList<>();
        boolean transport = agent.getNote("works at", D).getNote("type", S).equalsIgnoreCase("road");
        // If agent is far from nearest node, create approach path
        if (agentPos.distance(nearestNodePos) > 0.5f) {
            List<Data> approachPath = createPath(agentPos, nearestNodePos, "off");
            completePath.addAll(approachPath);
        }

        Vector3f workspot = null;
        for(Data spot : building.getNote("workspots", DL)) {
            if(transport||agent.getNote("state", S).equals("getting_food"))
            {
            	Vector3f dimensions = building.getNote("dimensions",V3);
            	Vector3f targetPoint = findNearestNode(building.getPosition()).getPosition();
            	Vector3f boxCenter = building.getPosition();
            	
            	 Vector3f halfExtents = dimensions.mult(0.5f);
                 Vector3f min = boxCenter.subtract(halfExtents);
                 Vector3f max = boxCenter.add(halfExtents);
                 
                 // Clamp the target point coordinates to the bounding box
                 float x = clamp(targetPoint.x, min.x, max.x);
                 float y = clamp(targetPoint.y, min.y, max.y);
                 float z = clamp(targetPoint.z, min.z, max.z);

                 Random r = new Random();
            	  workspot = new Vector3f(x,y,z);
            	  break;
            }
            System.out.println("[MTB] Finding spot for " + agent.getNote("name", S));
            System.out.println("[MTB] Checking spot at " + spot.getPosition() + 
                    " occupied: " + spot.getNote("occupied", B));
            if(!spot.getNote("occupied", B)) 
            {
            	
                workspot = spot.getPosition();
                spot.setNote("occupied", true);
                agent.setNote("mySpot", spot);  // Simplified to just setNote
                System.out.println("[MTB] Assigned " + agent.getNote("name", S) + 
                                  " to spot at " + workspot);
                break;
            }
        }
        
        if(workspot==null)
        {
        	System.out.println("[MTB] No valid workspots found");
        }
        
        Data nearestToBuilding = findNearestNode(workspot);
        
        
        List<Data> mainPath = findShortestPath(nearestToAgent, nearestToBuilding);
        if (!completePath.isEmpty()) 
        {
            completePath.remove(completePath.size() - 1);
        }
        completePath.addAll(mainPath);
        if (!completePath.isEmpty()) 
        {
            completePath.remove(completePath.size() - 1);
        }
        
        if(nearestToBuilding.getPosition().distance(workspot)>0.5f)
        {
        List<Data> finalApproach = createPath(nearestToBuilding.getPosition(), workspot, "off");
        completePath.addAll(finalApproach);
        }
        else
        {
            completePath.add(nearestToBuilding);
        }
        
        return completePath;
    }

    public Data findNearestNode(Vector3f point) {
        Data nearest = null;
        float minDistance = Float.MAX_VALUE;

        for (Data node : nodes) {
            float distance = node.getPosition().distance(point);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = node;
            }
        }
        return nearest;
    }

    public List<Data> createPath(Vector3f start, Vector3f end, String pathType) {
        List<Data> pathNodes = new ArrayList<>();
        List<Vector3f> waypoints = new ArrayList<>();
        float spacing = pathMap.get(pathType).getNote("spacing",L);
        waypoints.add(start);
        
        // Use an iterative approach to find waypoints around obstacles
        Vector3f current = start;
        int maxAttempts = 50; // Prevent infinite loops
        int attempts = 0;
        
        while (current.distance(end) > spacing && attempts < maxAttempts) {
            Vector3f direction = end.subtract(current).normalizeLocal();
            Vector3f nextPoint = current.add(direction.mult(spacing));
            
            if (col.checkPath(current, nextPoint)) {
                // Find a detour point
                Vector3f detour = col.findClearPath(current, nextPoint);
                if (detour != nextPoint) { // If we found a valid detour
                    waypoints.add(detour);
                    current = detour;
                } else {
                    // Could not find detour, try a shorter segment
                    nextPoint = current.add(direction.mult(spacing * 0.5f));
                    if (!col.checkPath(current, nextPoint)) {
                        waypoints.add(nextPoint);
                        current = nextPoint;
                    } else {
                        // Unable to progress, break to avoid infinite loop
                        break;
                    }
                }
            } else {
                waypoints.add(nextPoint);
                current = nextPoint;
            }
            attempts++;
        }
        
        waypoints.add(end);
        
        // Convert waypoints to path nodes
        for (Vector3f point : waypoints) {
            Data node = createTempNode(point, pathType);
            pathNodes.add(node);
            
            // Connect to previous node
            if (pathNodes.size() > 1) {
                connectNodes(pathNodes.get(pathNodes.size() - 2), node);
            }
        }
        
        return pathNodes;
    }
    
    public boolean checkPath(Vector3f start, Vector3f end)
    {
    	return col.checkPath(start, end);
    }

    public List<Data> findShortestPath(Data start, Data end) {
        List<List<Data>> allPaths = new ArrayList<>();
        Set<Data> visited = new HashSet<>();
        findPaths(start, end, visited, new ArrayList<>(), allPaths);

        if (allPaths.isEmpty()) {
            return createPath(start.getPosition(), end.getPosition(), "off");
        }

        return Collections.min(allPaths, (p1, p2) -> p1.size() - p2.size());
    }

    private void findPaths(Data current, Data end, Set<Data> visited, 
                          List<Data> currentPath, List<List<Data>> allPaths) {
        visited.add(current);
        currentPath.add(current);

        if (current == end) {
            allPaths.add(new ArrayList<>(currentPath));
        } else {
            for (Data next : getConnectedNodes(current)) {
                if (!visited.contains(next)) {
                    findPaths(next, end, visited, currentPath, allPaths);
                }
            }
        }

        visited.remove(current);
        currentPath.remove(currentPath.size() - 1);
    }
    
    @SuppressWarnings("unused")
	private void debugPath(List<Data> path) {
        System.out.println("\nDebug Path:");
        if (path == null || path.isEmpty()) {
            System.out.println("Path is empty!");
            return;
        }
        
        for (int i = 0; i < path.size(); i++) {
            Data node = path.get(i);
            System.out.println("Node " + i + ": " + node.getPosition() + 
                             " Type: " + node.getNote("type", S));
            
            List<Data> connections = node.getNote("connections", DL);
            System.out.println("  Connections: " + 
                (connections != null ? connections.size() : 0));
        }
    }
    
    public class SnapResult {
        public final Data node;
        public final boolean isExisting;
        public final Data snapPoint;
        public final boolean legal;
        
        public SnapResult(Data node, boolean isExisting, Data snapPoint, boolean legal) {
            this.node = node;
            this.isExisting = isExisting;
            this.snapPoint = snapPoint;
            this.legal = legal;
        }
    }

    // Enhanced findSnapPoint method to restore building connection point handling
    public SnapResult findSnapPoint(Vector3f point, float maxDistance, Data excludeNode, 
        Map<Data,List<Vector3f>> buildingConnectionPoints) {
        
        Data nearest = null;
        boolean isExisting = false;
        float snapRadius = maxDistance / 4;
        float bestDistance = snapRadius;
        boolean legal = true;

        if (col.isSolid(point)) {
            return new SnapResult(null, false, null,false);
        }
        // Check existing nodes first
        for (Data node : nodes) {
            if (node == excludeNode) continue;

            if (excludeNode != null) {
                Vector3f nodePoint = node.getPosition();
                Vector3f excludePoint = excludeNode.getPosition();
                if (nodePoint.distance(excludePoint) > maxDistance) continue;
            }

            Vector3f nodePos = node.getPosition();
            float distance = point.distance(nodePos);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = node;
                isExisting = true;
            }
        }

        // Check building connections if no existing node found
        if (!isExisting) {
            for (Map.Entry<Data, List<Vector3f>> entry : buildingConnectionPoints.entrySet()) {
                for (Vector3f connectionPoint : entry.getValue()) {
                    if (excludeNode != null) {
                        Vector3f excludePoint = excludeNode.getPosition();
                        if (connectionPoint.distance(excludePoint) > maxDistance) continue;
                    }

                    float distance = point.distance(connectionPoint);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        nearest = new Data(
                            "type:", "connection",
                            "position", connectionPoint,
                            "connections:", new ArrayList<Data>()
                        );
                        isExisting = false;
                    }
                }
            }
        }

        return new SnapResult(nearest, isExisting,null,legal);
    }
    
    public SnapResult findRnodeSnap(Vector3f point, float maxDistance, Data excludeNode, 
            String improves, ArrayList<Data> rNodes,BoundingBox bBox) {
            
            Data nearest = null;
            Data snapPoint = null;
            boolean isExisting = false;
            float snapRadius = maxDistance;
            float bestDistance = snapRadius;
            boolean legal = true;

            
            // Check existing nodes first
            for (Data node : rNodes) 
            {
                if (node == excludeNode) continue;

                if (excludeNode != null) 
                {
                    Vector3f nodePoint = node.getPosition();
                    Vector3f excludePoint = excludeNode.getPosition();
                    if (nodePoint.distance(excludePoint) > maxDistance) continue;
                }
                
                if(improves!=null)
                {
                	if(node.getNote("type", S).equals(improves))
                	{
                		ArrayList<Data> snapspots = node.getNote("snapspots",DL);
                		for(Data snapspot : snapspots)
                		{
                        Vector3f nodePos = snapspot.getPosition();
                        float distance = point.distance(nodePos);
                        if (distance < bestDistance) 
                        {
                            bestDistance = distance;
                            nearest = node;
                            snapPoint = snapspot;
                            isExisting = true;
                        }
                		}
                	}
                }
                else if (col.isSolid(point)||col.isSolidArea(point, new Vector3f(bBox.getXExtent(),bBox.getYExtent(),bBox.getZExtent())))
                {
                    return new SnapResult(null, false, null,false);
                }
                else
                {
                Vector3f nodePos = node.getPosition();
                float distance = point.distance(nodePos);
                if (distance < bestDistance) 
                {
                    bestDistance = distance;
                    nearest = node;
                    isExisting = true;
                }
                }
            }
            return new SnapResult(nearest, isExisting, snapPoint,legal);
        }

    public boolean canConnect(Data node1, Data node2) {
        float spacing = pathMap.get(node1.getNote("type", S)).getNote("spacing", L);
        return node1.getPosition().distance(node2.getPosition()) <= spacing;
    }

    public float getNodeDistance(Data node1, Data node2) {
        return node1.getPosition().distance(node2.getPosition());
    }

    public List<Data> getConnectedNodes(Data node) {
        return node.getNote("connections", DL);
    }

    private boolean areNodesConnected(Data node1, Data node2) {
        List<Data> connections = node1.getNote("connections", DL);
        return connections != null && connections.contains(node2);
    }
}