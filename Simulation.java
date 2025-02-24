package com.golden.game;

import java.util.*;
import java.util.Map.Entry;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial.CullHint;

import static com.golden.game.Data.*;

public class Simulation 
{
	Random r = new Random();
	private Generator g;
	private TransportNetwork net;
	
	public  Map<String,ArrayList<Data>> overmap = new HashMap<>();
	public  ArrayList<Data> agents = new ArrayList<>();
	public  ArrayList<Data> buildings = new ArrayList<>();
	public  ArrayList<Data> rNodes = new ArrayList<>();
	public  Map<String, Data> productMap = new HashMap<>();
	private  Map<String, Data> nameMap = new HashMap<>();
	public  Map<String, Data> buildingMap = new HashMap<>();
	public Map<String,Integer> productQ = new HashMap<>();
	public Map<Data,ArrayList<Data>> agentEffects = new HashMap<>();
	public ArrayList<Data> transportBills = new ArrayList<>();
	public ArrayList<Data> trashCan = new ArrayList<>();
	
	public ArrayList<String> techs = new ArrayList<>();
	
	public int turn=0;
	
	public Simulation(Generator generator, TransportNetwork network)
	{
		this.g = generator;
		this.net = network;
		//Create all the product categories
		for(String t :g.products){productQ.putIfAbsent(t, 0);}
		//Give string names to the hashmaps for easy lookup
		overmap.put("agents", agents);
		overmap.put("buildings", buildings);
		overmap.put("bills", transportBills);
		overmap.put("rNodes", rNodes);
		//Match the product data to a string for easy lookup
		for(String s : g.products)
		productMap.put(s,g.product(s));
		
		for(String s: g.buildingTypes)
		buildingMap.put(s, g.makeBuilding(s));
		
		techs.add("basic");
		techs.add("cutters");
		
	}
	
	public Data search(String type,String key,String value)
	{
		Data x = null;
		int i = 0;
		for(Data d : overmap.get(type))
		{
			if(d.getNote(key, S).equals(value)&&i==0) 
			{
				x=d;
				i++;
			}
			else if(i>0)
			{
				System.out.println("Too many matches: "+i);
			}
		}
		return x;	
	}
    public Data store(String type, Data data) {
        overmap.get(type).add(data);
        return data;
    }
	public void processAll(String type)
	{
		for(Data d : overmap.get(type))
		{
			d.processDTO(d);
		}
	}
	public void simulationOverview()
	{
		for(Data d : agents) System.out.print(d.getNote("likes",S)+" - "+d.getNote("wealth",I)+"$ | ");
		System.out.println();
		for(Data d : buildings)  System.out.print(d.getNote("type",S)+" | ");
		System.out.println();
		Map<String, Integer> prefrences = new HashMap<>();
		for(Data d : agents)
		{
			String likes = d.getNote("likes", S);
			prefrences.putIfAbsent(likes, 0);
			prefrences.put(likes,prefrences.get(likes)+1);
		}
		System.out.println(prefrences);
		
		Map<String, Integer> productions = new HashMap<>();
		for(Data d : buildings)
		{
			String prod = d.getNote("produces",S);
			productions.putIfAbsent(prod, 0);
			productions.put(prod,productions.get(prod)+1);
		}
		for(Entry<String, Integer> p : prefrences.entrySet())
		{
			if(productions.containsKey(p.getKey())) 
			System.out.printf("There are %o buildings producing %s and %o agents who want %s%n"
					,productions.get(p.getKey()),p.getKey(),p.getValue(),p.getKey());
			else
			System.out.println("There are no builidngs producing "+p.getKey()+" and "
			+p.getValue()+" agents who want "+p.getKey());
		}

		
	}
	public Data findType(String type)
	{
		for(Entry<String, Data> p : productMap.entrySet())
		{
			if(type==p.getKey()) return p.getValue();
		}
		for(Entry<String, Data> d : nameMap.entrySet())
		{
			if(type==d.getKey()) return d.getValue();
		}
		return null;
	}
	public Data findData(String type, Data item)
	{
		ArrayList<Data> list = overmap.get(type);
		for(Data d : list)
		{
			if(d==item) return d;
		}
		System.out.println("Data "+item+" not found in "+type);
		return null;
	}
	
	public void refreshNameMap()
	{
		nameMap.clear();
		for(String s : g.names)
		  for(Data n : agents)
		    if(n.getNote("name", S)==s)
			  nameMap.put(s,n);
	}
	
	public void makeOwners()
	{
		for(Data d : buildings)
		{
			for(Data n : agents)
			{
				if(!d.hasNote("owner")&&!n.hasNote("owns"))
				{
				d.addNote("Owner: ", n.getNote("name",S));
				n.addNote("Owns: ", d);
				}
			}
		}
	}
	
	
    public Data makeAgent(String type) {
        Data a = g.generateAgent(type);
        // Set initial 3D position
        a.setPosition(new Vector3f(
            r.nextFloat(-5f,5f),
            0,                      
            r.nextFloat(-5f,5f)
        ));
        a.addNote("works at", g.makeBuilding("road"));
        store("agents", a);
        System.out.println("Agent " + a.getNote("name", S) + " created at " + a.getPosition());
        return a;
    }
    
    public Data makePred(String type) {
        Data a = g.generateAgent(type);
        // Set initial 3D position
        float angle = r.nextFloat() * 2f * FastMath.PI;
        float radius = 60f + (r.nextFloat() * (80f - 60f));
        
        a.setPosition(new Vector3f(
            FastMath.cos(angle) * radius,
            0,
            FastMath.sin(angle) * radius
        ));
        a.setNote("state","hunting");
        a.addNote("works at", g.makeBuilding("road"));
        store("agents", a);
        System.out.println("Predetor " + a.getNote("name", S) + " created at " + a.getPosition());
        return a;
    }
	

	public void moveAgent(Data agent, Data target) {
	   System.out.println("[MV] "+agent.getNote("name",S)+" is moving to "+target.getNote("name",S));

	    // Skip if no speed left
	    if(agent.hasNote("speed_left") && agent.getNote("speed_left", I) <= 0) {
	    	System.out.println("[MV] No speed left");
	        return;
	    }
	    // Create new path if needed
	    if ((agent.getNote("remainingpath", PL) == null || agent.getNote("remainingpath", PL).isEmpty())) 
	    {
	        
	    	Vector3f agentPos = agent.getPosition();
	    	boolean transport = agent.getNote("works at", D).getNote("type", S).equalsIgnoreCase("road");
	    	boolean isInBuilding=isAgentAtBuilding(agent,target);
	    	
	    	System.out.println("[MV] " + agent.getNote("name", S) + 
                    " inBuilding: " + isInBuilding + 
                    " hasSpot: " + (agent.getNote("myspot", D) != null));

			  if(!isInBuilding && agent.getNote("myspot", D) != null) {
			      Data spot = agent.getNote("myspot", D);
			      System.out.println("[MV] Clearing spot at " + spot.getPosition() + 
			                        " for " + agent.getNote("name", S));
			      spot.setNote("occupied", false);
			      agent.setNote("myspot", null);
			  }
			  
			  if(isInBuilding&&transport)
			  {
				  System.out.println("[MV] Transport path cleared!");
				  agent.getNote("remainingpath", PL).clear();
			  }
		    List<Data> path = net.moveToBuilding(agentPos, target,agent);

	        System.out.println("Created new path: " + path);
	        agent.setNote("remainingpath", path); 
	    } 
	    else
	    {
	        System.out.println("----> path in place: " + agent.getNote("remainingpath", PL));
	    }
	}

	public void resolveTurn(Data agent) {
	        // Skip if no speed left
		    System.out.println();
	        if(agent.hasNote("speed_left") && agent.getNote("speed_left", I) <= 0) {
	            return;
	        }
	        
		    // Move one node if we have speed
		    if(agent.getNote("speed_left", I) > 0 && agent.hasNote("remainingpath") &&
		            !agent.getNote("remainingpath", PL).isEmpty()&&
		            !(agent.getNote("atype", S).equals("pred")&&!agent.getNote("processed_turn",B))) 
		    {
		         net.updateAgentMovement(agent);
		    }

	        String name = agent.getNote("name", S);
	        int hunger = agent.getNote("hunger", I);
	        ArrayList<Data> effects = agent.getNote("effects", DL);
	        HashMap<String,Integer> inventory = agent.getNote("has", DH);      
	        HashMap<String,Integer> statsMap = new HashMap<>();

			//Only handle these things at the start of the turn!
	        if(!agent.getNote("processed_turn",B)) {

		        for(String stat : g.stats)
			    statsMap.put(stat, agent.getNote(stat, I));
		        
		        incrementStat(agent,"hunger",1,statsMap);
		       // System.out.println("[REST] Stats: "+statsMap+" for "+name);
	        	
				if(hunger>5&&!effects.stream().anyMatch(e -> e.getNote("type", S).equals("hungry"))) 
					effects.add(g.generateEffect(0));
				if(hunger>10&&!effects.stream().anyMatch(e -> e.getNote("type", S).equals("starving"))) 
					effects.add(g.generateEffect(1));
				
				for(Data e : effects)
				{
					String type = e.getNote("type",S);
					String mod = e.getNote("modifies",S);
					int stat = agent.getNote(mod,I);
					int by = e.getNote("by",I);
					
					agent.setNote(mod, stat+by);
					// System.out.println("[REST] Modified "+name+"'s "+mod+" by "+by+" from "+stat+" to "+(stat+by)+" because of "+type);
				}
	        }
	        

	        incrementStat(agent,"fatigue",1,statsMap);
	        agent.setNote("processed_turn", true);
	}
	
	private void incrementStat(Data agent, String stat, int value, HashMap<String,Integer> statsMap)
	{
		int statNum  = agent.getNote(stat, I)+value;
		agent.setNote(stat, statNum);
		statsMap.put(stat, statNum);
	}
	
	public void resolveBuilding(Data b,Data agent) {
		//System.out.println("=[RESB]  =================== "+b.getNote("name", S));
	    
		
		     HashMap<String, Integer> capacity = b.getNote("capacity",DH);
		     HashMap<String, Integer> inventory = b.getNote("has", DH);
				Data r = b.getNote("selectedrecipe",D);
			System.out.println("[RESB] Selected recipe: "+r.getNote("name", S));

		if (!r.getNote("uses",DL).isEmpty()) 
		{  
	        ArrayList<Data> useList = r.getNote("uses",DL);
	        ArrayList<Integer> useAmount = r.getNote("useamt", IL);
	       
	        
	        // Check each resource's desired level
	        for (int i = 0; i < useList.size(); i++)
	        {
	        	boolean construction = r.hasNote("construction");
	            Data resource = useList.get(i);
	            int needed = useAmount.get(i);
	            String resourceType = resource.getNote("type", S);
	            int current = inventory.getOrDefault(resourceType, 0);
	            int cap = capacity.getOrDefault(resourceType, 2);
	            int desired = construction? needed : Math.max(cap/2, Math.min(needed*2, cap));
	            int requestAmount = desired - current;
	            double urgency = needed/Math.max(current, 1);
	            
	            if (current < desired) {
	                // Try with full desired amount first
	                Data source = findSourceBuilding(resourceType, b, requestAmount);
	                
	                // If no source found, try with reduced amounts
	                if (source == null) {
	                    // Try with progressively smaller amounts
	                    int[] fallbackAmounts = {
	                        Math.max(needed, requestAmount/2),  // Try half of desired
	                        needed,                             // Try just what we need
	                        Math.max(1, needed/2)               // Try half of what we need
	                    };
	                    
	                    for (int amount : fallbackAmounts) {
	                        source = findSourceBuilding(resourceType, b, amount);
	                        if (source != null) {
	                            requestAmount = amount;
	                            break;
	                        }
	                    }
	                }
	                
	                // Create bill if we found a source
	                if (source != null && !billExists(resourceType, source, b)) {
	                    Data bill = g.generateBill(resourceType, requestAmount, source, b, urgency);
	                    transportBills.add(bill);
	                    System.out.println("[RESB] " + b.getNote("name", S) + " created bill for " + requestAmount + 
	                                     " " + resourceType + " from " + source.getNote("name", S) + 
	                                     (requestAmount < desired ? " (reduced from " + desired + ")" : ""));
	                } else if (source == null) {
	                    System.out.println("[RESB] " + b.getNote("name", S) + 
	                                     " failed to create bill for " + resourceType + 
	                                     " - no source found even with reduced amounts");
	                } else {
	                    System.out.println("[RESB] " + b.getNote("name", S) + 
	                                     " attempted to create bill for " + requestAmount + " " + 
	                                     resourceType + " but a duplicate bill already exists");
	                    findBill(resourceType, source, b).setNote("urgency", urgency);
	                }
	            }
	        }
	    }
	        ArrayList<Data> productList = r.getNote("products", DL);
	        ArrayList<Integer> productAmount = r.getNote("productamt", IL);
	        ArrayList<Data> useList = r.getNote("uses", DL);
	        ArrayList<Integer> useAmount = r.getNote("useamt", IL);
	        
	        int canProduce = 0;
	        int productionThreshold = productList.size()+useList.size();
	        
	        // Check if we have enough resources to produce
	        for (int i = 0; i < useList.size(); i++) {
	            Data resource = useList.get(i);
	            String resourceType = resource.getNote("type", S);
	            int needed = useAmount.get(i);
	            
	            if (inventory.getOrDefault(resourceType, 0) >= needed) {
	                canProduce++;
	            }
	        }
	        
	        //Check if we have the inventory space to produce
	        for (int i = 0; i < productList.size(); i++) {
	            Data product = productList.get(i);
	            String resourceType = product.getNote("type", S);
	            int produced = productAmount.get(i);
	            int inInv = inventory.getOrDefault(resourceType,0);
	            int cap = capacity.getOrDefault(resourceType, 0);
	            
	            if(product.hasNote("istoken"))
                {
            		if(product.hasNote("isyeild"))
            		{
            		String yeildtype = product.getNote("yeildtype", S);
            		produced = product.getNote("yeildamt", I);
            		inInv = inventory.getOrDefault(yeildtype,0);
       	            cap = capacity.getOrDefault(yeildtype, 0);
            		}
                }
	            
	            if ((cap-inInv) >= produced) {
	                canProduce++;
	            }
	        }
	        
	        //Export excess goods
	        for(Entry<String,Integer> product : capacity.entrySet())
	        {
	        	String resourceType = product.getKey();
	        	int inInv = inventory.getOrDefault(resourceType,0);
	            int cap = capacity.getOrDefault(resourceType, 0);
	            double urgency = (double)(inInv/2)/(double)cap;
	            
	            //Can be used to customize building export limits in the future via a UI and note
	            if(inInv*1.5 >= cap)
	            {	
	            	 int export = (int)(inInv*1.5)-cap;
	            	 if(export>0)
	            	 {
	            	 Data storage = findStorageBuilding(resourceType, b, export);
	            	 if(storage!=null && !billExists(resourceType, b, storage))
	            	 {
	            	 Data bill = g.generateBill(resourceType, export, b, storage,urgency);
	            	 System.out.println(" [RESB] "+b.getNote("name",S)+" created a bill to export "+export+" "+resourceType);
	                 transportBills.add(bill);
	            	 }
	            	 else if(storage==null) System.out.println("[RESB] "+b.getNote("name",S)+" attempted to create bill to export "+export+" "+resourceType);
		             else
		                {
		            	Data existingBill = findBill(resourceType, b, storage);
		                System.out.println("[RESB] "+b.getNote("name",S)+" attempted to create bill to export "+export+" "+resourceType+" but a duplicate bill already exists");;
		                existingBill.setNote("urgency", urgency);
		                existingBill.setNote("amount", export);
		                }
	            	 }
	            }
	        }
	        

	        // If we have all needed resources, produce
	        if (canProduce >= productionThreshold) {
	        	int workspeed = r.getNote("workspeed", I);
                System.out.println("[RESB] "+agent.getNote("name", S)+" worked "+b.getNote("name", S));
                System.out.println("[RESB] Setting speed_left to "+(agent.getNote("speed_left", I)-workspeed)+" [-"+workspeed+"] for "+agent.getNote("name", S));
	            agent.setNote("speed_left", agent.getNote("speed_left", I) - workspeed);
	        	
	            // Consume resources
//	            if(r.hasNote("isharvest"))
//	            {
//	              HashMap<String, Integer> rNodeHas = b.getNote("on", D).getNote("has", DH);
//            	  for (int n = 0; n < productList.size(); n++) 
//            	  {
//            		String resourceType = productList.get(n).getNote("type", S);
//      	            int produced = productAmount.get(n);
//      	            rNodeHas.merge(resourceType, -produced, Integer::sum);
//  	              }
//	            }
//	            else
	            for (int i = 0; i < useList.size(); i++) 
	            {
	            	Data used = useList.get(i);
	                String resourceType = used.getNote("type", S);
	                int amount = useAmount.get(i);
	                if(used.hasNote("uses_left")) 
	                if(!(used.getNote("uses_left",I)<=-100)) 
	                {
	                	int uses = used.getNote("uses_left",I);
	                	System.out.println("[RESB] "+resourceType+" has "+uses+" uses left");
	                	if(uses<=0)
	                	{
	                   	   used.setNote("uses_left",used.getNote("uses_total",I));
	                       inventory.merge(resourceType, -amount, Integer::sum);
	                	}
	                	else
	                	   used.setNote("uses_left", uses-(r.getNote("usedmg", I)));
	                }
	                else
	                inventory.merge(resourceType, -amount, Integer::sum);
	            }
	            
	            // Add products
	            for (int i = 0; i < productList.size(); i++) {
	                String productType = productList.get(i).getNote("type", S);
	                int amount = productAmount.get(i);
	                inventory.merge(productType, amount, Integer::sum);
	                System.out.println("[RESB] "+b.getNote("name",S)+" produced "+amount+" "+productType+" ["+inventory.get(productType)+"]");
	                Data product = productList.get(i);
	                
	                if(product.hasNote("istoken"))
	                {
	                	if(inventory.getOrDefault(product.getNote("type",S), 0)>=product.getNote("threshold", I))
	                	{
	                		if(product.hasNote("istech"))
	                		{
		                	techs.add(product.getNote("tech", S));
		                	System.out.println("	[RESB] "+product.getNote("tech", S)+" researched!");
	                		}
	                		
	                		if(product.hasNote("isyeild"))
	                		{
	                		String yeildtype = product.getNote("yeildtype", S);
	                		int yeildamt = product.getNote("yeildamt", I);
	                		inventory.merge(yeildtype, yeildamt, Integer::sum);
	                		 System.out.println("[RESB] "+b.getNote("name",S)+" produced a yeild of "+yeildtype+" "+yeildamt+" ["+inventory.get(yeildtype)+"]");
	                		 inventory.merge(product.getNote("type", S), -product.getNote("threshold",I),Integer::sum);
	         	            if(r.hasNote("isharvest"))
	        	            {
		        	            HashMap<String, Integer> rNodeHas = b.getNote("on", D).getNote("has", DH);
	         	            	for(Data sSpot : b.getNote("on", D).getNote("storagespots", DL))
	         	            	{
	         	            		if(sSpot.getNote("viscode", S).equalsIgnoreCase(b.getNote("onpoint",D).getNote("viscode",S)))
	         	            		{
	         	            			rNodeHas = sSpot.getNote("has", DH);
	         	            		}
	         	            	}
	              	          rNodeHas.merge(yeildtype, -yeildamt, Integer::sum);
	        	            }
	                		}
	                	}
	                }
	            }
	        }
	        else
	        {
	        	 System.out.println("[RESB] "+b.getNote("name", S)+" was unable to be worked");
	        }
	    
	}

	private Data findSourceBuilding(String product, Data target, int needed) {
	    Data bestSource = null;
	    int maxAvailable = 0;

	    for (Data source : buildings) {
	        if (source == target) continue;  // Skip the target building
	        HashMap<String, Integer> sourceInventory = source.getNote("has", DH);
	        if (sourceInventory != null && sourceInventory.getOrDefault(product, 0) > maxAvailable &&
	        		sourceInventory.getOrDefault(product, 0) > needed) {
	            maxAvailable = sourceInventory.get(product);
	            bestSource = source;
	        }
	    }

	    return bestSource;
	}
	
	private Data findStorageBuilding(String product, Data target, int exporting) {
	    Data bestStorage = null;
	    float minDistance = 999f;

	    for (Data source : buildings) {
	        if (source == target || !source.getNote("isstorage", B)) continue;  // Skip the target building
//	        System.out.println(" [FSB] Checking building "+source.getNote("name",S));
	        Vector3f sPos = source.getPosition();
	        Vector3f tPos = target.getPosition();
	        float dist = tPos.distance(sPos);
	        //System.out.println(" [FSB] Distance "+dist);
		        HashMap<String, Integer> sourceInventory = source.getNote("has", DH);
		        HashMap<String, Integer> sourceCapacity = source.getNote("capacity", DH);
		        int inInv = sourceInventory.getOrDefault(product, 0);
		        int cap = sourceCapacity.getOrDefault(product, 0);
		        int room = cap-inInv;
//		        System.out.println(" [FSB] In inventory "+inInv);
//		        System.out.println(" [FSB] Capcity "+cap);
//		        System.out.println(" [FSB] Room "+room);
		        if (sourceInventory != null && room>0 && (room-exporting)>0 && dist < minDistance) 
		        {
		        	minDistance= dist;
		            bestStorage = source;
		            //System.out.println(" [FSB] Best storage is "+bestStorage.getNote("name",S));
		        }
	    }

	    return bestStorage;
	}
	
	private Data findProductBuilding(String product, Data agent, int needed) {
	    Data bestSource = null;
	    float minDistance = 999f;

	    for (Data source : buildings) {
	        Vector3f sPos = source.getPosition();
	        Vector3f tPos = agent.getPosition();
	        float dist = tPos.distance(sPos);
	        HashMap<String, Integer> sourceInventory = source.getNote("has", DH);
	        if (sourceInventory != null && sourceInventory.getOrDefault(product, 0) >= needed &&
	        	dist<minDistance) {
	            minDistance=dist;
	            bestSource = source;
	        }
	    }

	    return bestSource;
	}
	
  public boolean isAgentAtBuilding(Data agent, Data building) {
        Vector3f agentPos = agent.getPosition();
        Vector3f buildingPos = building.getPosition();
        Vector3f buildingBox = building.getNote("dimensions",V3);
        boolean transport = agent.getNote("works at", D).getNote("type", S).equalsIgnoreCase("road") || agent.getNote("state", S).equals("getting_food");
        double radius = transport ? 1.5 : 2.0;
   
        // Check if agent is within building bounds (in XZ plane)
        boolean isInBuilding = Math.abs(agentPos.x - buildingPos.x) <= buildingBox.getX()/radius &&
                Math.abs(agentPos.z - buildingPos.z) <= buildingBox.getZ()/radius;

			return isInBuilding;
    }
		 
	  public void runPredTurn(Data agent)
	  {
		if(agent.getNote("hunting", D)==null)
		{
			List<Data> viablePrey = findPrey(agent);
			if(!viablePrey.isEmpty())
			{
			Data chosen = viablePrey.get(r.nextInt(viablePrey.size()));
			agent.setNote("hunting", chosen);
			agent.setNote("preypos", chosen.getPosition());
			agent.setNote("state", "hunting");
			}
			else
			{
			agent.setNote("state", "searching");
			return;
			}
		}
		
		Data prey = agent.getNote("hunting", D);
		if(agent.getbBox().intersects(prey.getbBox()))
		{
			agent.setNote("state","returning");
			prey.setNote("speed",0);
			prey.getNote("spatial", N).setCullHint(CullHint.Always);
			agent.getNote("has", DH).put("mouse", 1);
		}
		//agent.setNote("status", "hunting");
		movePredetor(agent,prey);
	  }
  
	private List<Data> findPrey(Data pred)
	{
		List<Data> viablePrey = new ArrayList<>();
		for(Data prey : agents)
		{
			if(prey.getNote("species", S).equals("mouse"))
			{
				if(net.checkPath(pred.getPosition(), prey.getPosition()))
				viablePrey.add(prey);
			}
		}
		return viablePrey;
	}
	
	private void movePredetor(Data agent, Data target)
	{
	   System.out.println("[MV] "+agent.getNote("name",S)+" is hunting "+target.getNote("name",S));
	
	    // Skip if no speed left
	    if(agent.hasNote("speed_left") && agent.getNote("speed_left", I) <= 0) {
	    	System.out.println("[MV] No speed left");
	        return;
	    }
	    
		Vector3f agentPos = agent.getPosition();
		Vector3f targetPos = target.getPosition();
		if(agent.getNote("state", S).equals("returning"))
		{
			if(agent.getPosition().distance(agent.getNote("nest", D).getPosition())<1f)
			{
	    		trashCan.add(target);
				agent.getNote("has", DH).put("mouse",0);
				agent.setNote("state", "hunting");
				agent.setNote("hunting", null);
			}
			else
			{
				List<Data> path = net.createPath(agentPos, agent.getNote("nest", D).getPosition(), agent.getNote("species", S));
				agent.setNote("remainingpath", path);
			}
		}
		else
		{
	    List<Data> path = net.createPath(agentPos, targetPos, agent.getNote("species", S));
	    agent.setNote("remainingpath", path);	
		}
	}


	public void runCivlTurn(Data agent) {
	    // Skip if no speed left
		//System.out.println("===[WT] ==================== "+agent.getNote("name", S));
	    if(agent.hasNote("speed_left") && agent.getNote("speed_left", I) <= 0) {
	    	//System.out.println("[WT] Skipping turn");
	        return;
	    }
	    
	    if(agent.getNote("hunger",I)>100)
	    {
	    findFood(agent);
	    }
	    else
	    {
	    agent.setNote("state", "working");
	    }
	    if(agent.getNote("state", S).equals("working")) {
	        if(agent.hasNote("works at") && 
	           !agent.getNote("works at", D).getNote("type", S).equals("road")) {
	            
	            Data workplace = agent.getNote("works at", D);
	            if(workplace != null) {
	            	System.out.println(agent.getNote("name",S)+" at workplace: "+isAgentAtBuilding(agent, workplace));
	                if(isAgentAtBuilding(agent, workplace)) {
	                    // Check building's workspeed for production
	                    int workspeed = workplace.getNote("selectedrecipe",D).getNote("workspeed", I);
	                    System.out.println("[WT] "+agent.getNote("name", S)+" is at "+workplace.getNote("name", S)+" workspeed: "+workspeed);
	                    if(agent.getNote("speed_left", I) >= workspeed) {
	                        workplace.setNote("worked", true);
	                        resolveBuilding(workplace,agent);
	                    }
	                    else
	                    {
	                    System.out.println("[WT] "+agent.getNote("name", S)+" does not have enough speed left ["+agent.getNote("speed_left", I)+"] for workspeed: "+workspeed);
	                    }
	                } else {
	                	System.out.println("[WT] "+agent.getNote("name", S)+" has been assigned to "+workplace.getNote("name", S));
	                    moveAgent(agent, workplace);
	                }
	            }
	        } else {
	            // Transport agents
	            handleTransportDuty(agent);
	        }
	    }
	   
	}
	public int getAgentSpeed(Data agent) {
	    // Moving between workplaces - use road speed
	    if(agent.hasNote("remainingpath") && 
	       !agent.getNote("remainingpath", PL).isEmpty()) {
	        return g.makeBuilding("road").getNote("workspeed", I);
	    }
	    
	    // Otherwise use workplace speed
	    if(agent.hasNote("works at")) {
	        Data workplace = agent.getNote("works at", D);
	        return workplace.getNote("selectedrecipe",D).getNote("workspeed", I);
	    }
	    
	    // Fallback to road speed if no workplace
	    return g.makeBuilding("road").getNote("workspeed", I);
	}
	
	private void findFood(Data agent)
	{
		HashMap<Integer,List<String>> pref = agent.getNote("pref", SLH);
		String name = agent.getNote("name", S);
		int requestAmount = 0;
		Data foodPlace = null;
		String onFood = null;
		
		System.out.println(" [FF] "+name+" is looking for food");
		for(int i=1;i<=5;i++)
		{
		List<String> foods = pref.get(i);
		if(!foods.isEmpty())
			for(String food : foods)
			{
//				System.out.println(" [FF] Looking at "+food);
				int foodSat = findType(food).getNote("satiety",I);
				int hunger = agent.getNote("hunger",I);
				int needed = Math.max(hunger/foodSat,1);
//				System.out.println(" [FF] Need "+needed+" to satisfy hunger");
				foodPlace = findProductBuilding(food,agent,needed);
				requestAmount = needed;
				onFood=food;
				if (foodPlace == null) 
				{
					for(int k=0; k<needed-1;k++)
					{
					requestAmount = Math.max(1,requestAmount-k);
//					System.out.println("[FF] Reducing request to "+requestAmount);
                    foodPlace = findProductBuilding(food,agent,requestAmount);
                        if (foodPlace != null) 
                        {
                        	System.out.println("[FF] Foodplace found!");
                            break;
                        }
					}
                }
				else 
				{
					System.out.println(" [FF] Food place found for "+requestAmount+" "+onFood);
					break;
				}
			}
		
				if(foodPlace!=null)
				{
					System.out.println("-	[FF] Found a place with "+requestAmount+" "+onFood+"!");
					agent.setNote("state", "getting_food");
					moveAgent(agent, foodPlace);
					if(isAgentAtBuilding(agent,foodPlace))
					{
						System.out.println("-	[FF] "+name+" has reached food!");

						    HashMap<String, Integer> sourceInventory = foodPlace.getNote("has", DH);
						    int available = sourceInventory.getOrDefault(onFood, 0);
						    int transfer = Math.min(agent.getNote("speed_left", I),Math.min(requestAmount, available));
						    System.out.println("[HTD] "+agent.getNote("name", S)+" ate "+transfer+" "+onFood+" from "+foodPlace.getNote("name", S));
						    sourceInventory.merge(onFood, -transfer, Integer::sum);
						
						int foodSat = findType(onFood).getNote("satiety",I);
						int totalSat = foodSat*transfer;
						agent.setNote("hunger", agent.getNote("hunger",I)-totalSat);
						agent.setNote("state", "working");
						
				        System.out.println("[FF] Setting speed_left to "+(agent.getNote("speed_left", I)-transfer)+" for "+agent.getNote("name", S));
				        agent.setNote("speed_left", agent.getNote("speed_left", I) - transfer);
				        break;
					}
				}
				else 
				{
					System.out.println("-	[FF] No food found");
				    agent.setNote("state", "working");
				}
		}
	}
	
	
	private void handleTransportDuty(Data agent) {
	    // Skip if no speed left
		//System.out.println("==[HTD] ==================== "+agent.getNote("name", S));
	    if(agent.hasNote("speed_left") && agent.getNote("speed_left", I) <= 0) {
	        return;
	    }

	    Data currentBill = agent.getNote("currentbill", D);
	    
	    // No current bill - find one and start moving
	    if (currentBill == null) {
	    	//System.out.println("[HTD] "+agent.getNote("name", S)+" is looking for a bill");
	        Data nearestBill = findBestBill(agent);
	        if (nearestBill != null) {
	        	System.out.println("[HTD] "+agent.getNote("name", S)+" took bill "+nearestBill);
	        	nearestBill.setNote("status", "in_progress");
	            agent.setNote("currentbill", nearestBill);
	            moveAgent(agent, nearestBill.getNote("source", D));
	        }
	        return;
	    }
	    else
	    {
	       agent.setNote("state", "transport");
	    }

	    Data source = currentBill.getNote("source", D);
	    Data destination = currentBill.getNote("destination", D);
	    String product = currentBill.getNote("product", S);
	    HashMap<String, Integer> inventory = agent.getNote("has", DH);

	    // At source - pickup goods
	    if(!inventory.containsKey(product) || inventory.get(product) == 0) {
	        if(isAgentAtBuilding(agent, source)) {
	        	System.out.println("[HTD] "+agent.getNote("name", S)+" arrived at "+source.getNote("name", S));
	            pickupGoods(agent, currentBill);
	            System.out.println("[HTD] Transport path cleared!");
	 		    agent.getNote("remainingpath", PL).clear();
	            moveAgent(agent, destination);
	            System.out.println("[HTD] Setting speed_left to "+(agent.getNote("speed_left", I)-1)+" for "+agent.getNote("name", S));
	            agent.setNote("speed_left", agent.getNote("speed_left", I) - 1);
	            return;
	        }
	        // Move towards source
	        moveAgent(agent, source);
	        return;
	    }

	    // Have goods - handle delivery
	    if(isAgentAtBuilding(agent, destination)) {
	    	System.out.println("[HTD] "+agent.getNote("name", S)+" arrived at "+destination.getNote("name", S));
	        deliverGoods(agent, currentBill);
	  	   System.out.println("[HTD] Transport path cleared!");
		   agent.getNote("remainingpath", PL).clear();
	        // Look for next bill immediately
	    	System.out.println("[HTD] "+agent.getNote("name", S)+" is looking for a new bill");
	        Data nextBill = findBestBill(agent);
	        if (nextBill != null) {
	        	System.out.println("[HTD] "+agent.getNote("name", S)+" took  newbill "+nextBill);
	            agent.setNote("currentbill", nextBill);
	            moveAgent(agent, nextBill.getNote("source", D));
	        } else {
	            agent.setNote("currentbill", null);
	        }
	        System.out.println("[HTD] Setting speed_left to "+(agent.getNote("speed_left", I)-1)+" for "+agent.getNote("name", S));
	        agent.setNote("speed_left", agent.getNote("speed_left", I) - 1);
	        return;
	    }
	    
	    // Moving towards destination
	    moveAgent(agent, destination);
	}

	private void pickupGoods(Data agent, Data bill) {
	    Data source = bill.getNote("source", D);
	    String product = bill.getNote("product", S);
	    int amount = bill.getNote("amount", I);
	    
	    HashMap<String, Integer> sourceInventory = source.getNote("has", DH);
	    HashMap<String, Integer> agentInventory = agent.getNote("has", DH);
	    
	    int available = sourceInventory.getOrDefault(product, 0);
	    int transfer = Math.min(amount, available);
	    System.out.println("[HTD] "+agent.getNote("name", S)+" took "+amount+" "+product+" from "+source.getNote("name", S));
	    sourceInventory.merge(product, -transfer, Integer::sum);
	    agentInventory.merge(product, transfer, Integer::sum);
	}

	private void deliverGoods(Data agent, Data bill) {
	    Data destination = bill.getNote("destination", D);
	    String product = bill.getNote("product", S);
	    
	    HashMap<String, Integer> agentInventory = agent.getNote("has", DH);
	    HashMap<String, Integer> destInventory = destination.getNote("has", DH);
	   
	    int amount = agentInventory.getOrDefault(product, 0);
	    System.out.println("[HTD] "+agent.getNote("name", S)+" delivered "+amount+" "+product+" to "+destination.getNote("name", S));
	    agentInventory.put(product, 0);
	    destInventory.merge(product, amount, Integer::sum);
	    
	    transportBills.remove(bill);
	}

	private Data findBestBill(Data agent) {
	    Data bestBill = null;
	    double bestScore = Double.MIN_VALUE;
	    Vector3f agentPos = agent.getPosition();
	    
	    // Find max values for normalization
	    double maxDistance = Double.MIN_VALUE;
	    double maxUrgency = Double.MIN_VALUE;
	    
	    for (Data bill : transportBills) {
	        if (!bill.getNote("status", String.class).equals("pending")) continue;
	        
	        Data source = bill.getNote("source", D);
	        double urgency = bill.getNote("urgency", F);
	        Vector3f sourcePos = source.getPosition();
	        double distance = agentPos.distance(sourcePos);
	        
	        maxDistance = Math.max(maxDistance, distance);
	        maxUrgency = Math.max(maxUrgency, urgency);
	    }
	    
	    // Safety check
	    if (maxDistance == Double.MIN_VALUE || maxUrgency == Double.MIN_VALUE) {
	        return null;
	    }
	    
	    // Find Pareto-efficient bill
	    for (Data bill : transportBills) {
	        if (!bill.getNote("status", String.class).equals("pending")) continue;
	        
	        Data source = bill.getNote("source", D);
	        double urgency = bill.getNote("urgency", F);
	        Vector3f sourcePos = source.getPosition();
	        double distance = agentPos.distance(sourcePos);
	        if(urgency<=0.0) urgency=0.1;
	        // Normalize values to [0,1] range
	        double normalizedDistance = distance / maxDistance;
	        double normalizedUrgency = urgency / maxUrgency;
	        System.out.println(" [FTB] Normalized distance: "+normalizedDistance);
	        System.out.println(" [FTB] Normalized urgency: "+normalizedUrgency);
	        
	        // Calculate score with weighted sum
	        // Higher weight (0.7) for urgency, lower weight (0.3) for distance
	        // Distance is inverted since we want shorter distances to score higher
	        double score = (0.7 * normalizedUrgency) + (0.3 * (1.0 - normalizedDistance));
	        System.out.println(" [FTB] Score: "+score);
	        
	        if (score > bestScore) {
	            bestScore = score;
	            bestBill = bill;
	        }
	    }
	    
	    return bestBill;
	}

	private boolean billExists(String product, Data source, Data target) {
	    for (Data bill : transportBills) {
	        if (bill.getNote("product", S).equals(product) &&
	            bill.getNote("source", D).equals(source) &&
	            bill.getNote("destination", D).equals(target) &&
	            (bill.getNote("status", S).equals("pending") || bill.getNote("status", S).equals("in_progress"))) {
	            return true;  // A similar bill already exists
	        }
	    }
	    return false;
	}
	
	private Data findBill(String product, Data source, Data target) {
	    for (Data bill : transportBills) {
	        if (bill.getNote("product", S).equals(product) &&
	            bill.getNote("source", D).equals(source) &&
	            bill.getNote("destination", D).equals(target) &&
	            (bill.getNote("status", S).equals("pending") || bill.getNote("status", S).equals("in_progress"))) {
	            return bill;  // A similar bill already exists
	        }
	    }
	    return null;
	}
	
	public List<Data> findBuildingBills(Data building) {
		List<Data> bills = new ArrayList<>();
	    for (Data bill : transportBills)
	    	{
	        if (bill.getNote("source", D).equals(building) ||
	            bill.getNote("destination", D).equals(building))
	            bills.add(bill);
	    	}	
	    return bills;
	}




	
	private boolean likes(Data n,String product){return n.getNote("likes", S).equals(product);}
	//Buy method: Supplier, Purchaser, Product Name, Quantity of Product, Quantity to buy, Product value
	private void buy(Data n, Data n2,String prodS,int prodV,int quant,int val) 
	{
		//This lets an agent buy something from a building or another agent
		boolean person = n.hasNote("likes");
		int total = quant*val;
		n.getNote("has",DH).merge(prodS, -quant, Integer::sum);
		n2.getNote("has", DH).merge(prodS, quant, Integer::sum);
		n2.setNote("wealth", n2.getNote("wealth",I)-total);
		if(person) n.setNote("wealth", n.getNote("wealth", I)+(findType(prodS).getNote("value", I)*prodV));
		else if (n.hasNote("owner"))
		{		
		Data o = findType(n.getNote("owner", S));
		o.setNote("wealth", o.getNote("wealth", I)+(findType(prodS).getNote("value", I)*prodV));
		}
	}
	private int thinkBuy(Data n,String prodS,int prodV,int val)
	{
		int nWealth = n.getNote("wealth", I);
		int q = 0;
		q= prodV*val!=0 ? nWealth/(prodV*val) : 0;
		//System.out.println("q: "+q+" prodV*val: "+prodV*val+" nWealth: "+nWealth);
		return q;
	}
	
	public void runTradeTurn()
	{
	for(Data n : agents)
	{
		String name = n.getNote("name", S); 
		//Go through all the products that the agent has
		for(Entry<String, Integer> s : n.getNote("has", DH).entrySet())
		{	
			//Read the has list for the type of the product (prodS) and the ammonut (prodV)
			String prodS = s.getKey(); int prodV = s.getValue(); Data prod = findType(prodS);
			int val = prod.getNote("value", I);
			//In here, we can put any logic we want for trade and exchange, n has the goods, n2 wants them
			for(Data n2 : agents)
			{ 
				String name2 = n2.getNote("name", S);
				if( likes(n2,prodS) && prodV > 0 && !name.equals(name2) && 
				   !likes(n,prodS) && thinkBuy(n2,prodS,prodV,val)>0)
				{
					//The agent will calculate the quanity they want with the thinkBuy method, then buy it.
					int quant = thinkBuy(n2,prodS,prodV,val); 
					buy(n,n2,prodS,prodV,quant,val);
					System.out.printf("%s purchased %o %s from %s for %o$ %n",
							n2.getNote("name", S),quant,prodS,n.getNote("name", S),quant*val);

				}
			}
		}
	}
	}
	
	public void runMarketTurn()
	{
		for(Data b : buildings)
		{
			for(Entry<String, Integer> s : b.getNote("has", DH).entrySet())
			{
				String prodS = s.getKey(); int prodV = s.getValue(); Data prod = findType(prodS);
				int val = prod.getNote("value", I);
				
				for(Data n : agents)
				{
					int quant = thinkBuy(n,prodS,prodV,val);
					//Ensure the agent likes the product, the building has the product, and the agent can afford it.
					if(likes(n,prodS) && prodV > 0 && quant>0)
					{
						buy(b,n,prodS,prodV,quant,val);
						if (!n.hasNote("shopping at")) n.addNote("Shopping at: ", b);
						else n.setNote("shopping at", b);
						System.out.printf("%s purchased %o %s from %s for %o$ %n",
						n.getNote("name", S),quant,prodS,b.getNote("name", S),quant*val);
					}
				}
			}
		}
	}
	
}

