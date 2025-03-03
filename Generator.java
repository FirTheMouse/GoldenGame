package com.golden.game;

import java.util.*;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;

import static com.golden.game.Data.*;

public class Generator 
{
	Random r = new Random();
	NameGenerator namegen = new NameGenerator();	
	public String[] agentTypes = {"Snow","Sprig","Whiskers","weasel"};
	public String[] mouseTypes = {"Snow","Sprig","Whiskers"};
	public String[] predTypes = {"weasel"};
	public String[] pathTypes = {"dev","off","weasel","dirt","wooden"};
	public String[] stats = {"mood","hunger","fatigue"};
	public String[] rNodeTypes = {"tree","branch","grass patch","stones","hollow log","berry bush"};
	ArrayList<String> names = new ArrayList<String>();
	
	private int DELETE=0;
	
	public Data generateAgent(String type)
	{
		HashMap<String,Integer> has = new HashMap<>();
		ArrayList<Data> effects = new ArrayList<>();
		Vector3f pos = new Vector3f(0,0,0);
		String state = "default";
		ArrayList<Data> storagespots = new ArrayList<>();
		String species = "mouse";
		String atype = "civl";
		String name = null;
		int speed = 12;
		float sight = 20f;
		HashMap<Integer,List<String>> pref = new HashMap<>();
		//Agent prefrences, 1=loves, 2=likes, 3=neutral, 4=dislikes, 5=hates
		for(int i=1;i<=5;i++)
		pref.putIfAbsent(i, new ArrayList<String>());
		
		if(toArraylist(mouseTypes).contains(type))
			type="mouse";
		switch(type)
		{
		case "mouse":
			name = namegen.generateNames(1,"nouns",new int[]{0010});
			speed = 12;
			species="mouse";
			atype="civl";
			for(String p : products)
			{
				if(product(p).getNote("isfood", B))
				{
					pref.get(r.nextInt(2,5)).add(p);
				}
			}
		break;
		
		case "weasel":
			name = namegen.generateNames(1,"nouns",new int[]{1000});
			speed=6;
			species="weasel";
			atype="pred";
			sight = 90f;
		break;
		
		default:
			System.out.println("[GA] Invalid agent type!");
		break;
		}
		int speed_left = speed;
		
		Data agent = new Data("Name: ",name,"Hunger: ",0,"Mood: ",100,"position",pos,"species",species,"atype",atype,"remainingpath",new ArrayList<Data>(),
				"Has: ",has,"Speed: ",speed,"Effects: ",effects,"speed_left",speed_left,"processed_turn",false,"fatigue",0,"pref",pref,
				"state",state,"type",type,"storagespots",storagespots,"inbuilding",false,"hunting",null,"preypos",null,"sight",sight,"timeinstate",0);
		names.add(name);
		return agent;
	}
	
	public Data generateRoadNode(Vector3f position, String pathType)
	{
		float spacing=0f;
		boolean visible = true;
		ArrayList<Data> connections = new ArrayList<>();
		double x = position.x;
		double y = position.y;
		ColorRGBA color = ColorRGBA.Magenta;

		
		switch(pathType)
		{
		case "dev":
			spacing=100f;
			visible=false;
		break;
		
		case "off":
			spacing=2f;
			visible=false;
		break;
		
		case "weasel":
			spacing=15f;
			visible=false;
		break;
		
		case "dirt":
			color = new ColorRGBA(0.5f, 0.4f, 0.1f, 1f);
			spacing=5f;
		break;
		
		case "wooden":
			color = new ColorRGBA(0.5f, 0.2f, 0.1f, 1f);
			spacing = 10f;
		break;
		
		default:
			System.out.println("Invalid path type! Defaulting to devPath");
			spacing = 10000f;
			pathType="dev";
			visible=false;
		break;
		}
		
		return new Data("type",pathType,"x",x,"y",y,"position",position,"spacing",spacing,"connections",connections,"visible",visible,"color",color);
	}
	
	public Data generateEffect(int id)
	{
		Data e = null;
		
		switch(id)
		{
		case 0:
			e = new Data("Type: ","hungry","Modifies: ","mood","By: ",-1);
		break;
		
		case 1:
			e = new Data("Type: ","starving","Modifies: ","mood","By: ",-5);
		break;
		
		case 2:
			e = new Data("Type: ","sick","Modifies: ","mood","By: ",-5);
		break;
		
		default:
		break;
		}
		return e;
	}
	
	public String[] buildingTypes = {"building test","cutter pile","stone pile","stonechip pile","rootcut pile","grass pile","wood pile","berry pile","seed pile","mushroom pile","chippery","cuttery",
			"grass workspot","berry catcher","offpoint","cutter maker","research spot","stones workspot"};
	
	public Data makeBuilding(String type)
	{
		String name = type+" "+DELETE;
		DELETE++;
		
		HashMap<String,Integer> has = new HashMap<>();
		ArrayList<Data> uses = new ArrayList<>();
		ArrayList<Data> products = new ArrayList<>();
		ArrayList<Integer> useamt = new ArrayList<>();
		ArrayList<Integer> productamt = new ArrayList<>();
		HashMap<String,Integer> needs = new HashMap<>();
		ArrayList<Data> workspots = new ArrayList<>();
		ArrayList<Data> storagespots = new ArrayList<>();
		HashMap<String,Integer> capacity = new HashMap<>();
		ArrayList<Data> recipes = new ArrayList<>();
		ArrayList<String> techreq = new ArrayList<>();
		Data on = null;
		Vector3f pos = new Vector3f(0,0,0);
		String improves = null;
		int level=0;
		int workSpeed = 0;
		int width = 1;
		int height = 1;
		int ws = 6;
		int maxworkers = 2;
		boolean isStorage = false;
		boolean isBuilt = false;
		
		
		switch(type)
		{
		case "road":
			level=-1;
			workSpeed=ws*2;
			
			break;
			
		case "berry catcher":
			level=0;
			workSpeed=ws;
			maxworkers = 2;
			improves="berry bush";
			capacity.put("berry", 16);
			
			techreq.add("basic");
			recipes.add(new Data(
					"type","catch",
					"name","Berry Catching",
					"workspeed",12,
					"techreq",new ArrayList<>(List.of("basic")),
					"productamt",new ArrayList<>(List.of(1)),
					"uses",new ArrayList<>(),
					"useamt",new ArrayList<>(),
					"products",new ArrayList<>(List.of(new Data(
							"type","berry-catching",
							"istoken",true,
							"isyeild",true,
							"yeildtype","berry",
							"yeildamt",1,
							"threshold",4
							))),
					"isharvest",true
							));
		break;
		
		
		case "chippery":
			level=0;
			workSpeed=ws;
			maxworkers = 2;
			improves="pebbles";
			capacity.put("cutters", 7);
			capacity.put("stonechip", 7);
			
			techreq.add("basic");
			recipes.add(recipe("paw_chip"));
			recipes.add(recipe("basic_chip"));

		break;
		
		case "stones workspot":
			level=0;
			workSpeed=ws;
			maxworkers = 2;
			improves="stones";
			capacity.put("cutters", 7);
			capacity.put("stone", 4);
			
			techreq.add("basic");
			recipes.add(new Data(
					"type","haul",
					"name","Stone Hauling",
					"workspeed",12,
					"techreq",new ArrayList<>(List.of("basic")),
					"productamt",new ArrayList<>(List.of(1)),
					"uses",new ArrayList<>(),
					"useamt",new ArrayList<>(),
					"products",new ArrayList<>(List.of(new Data(
							"type","stone-hauling",
							"istoken",true,
							"isyeild",true,
							"yeildtype","stone",
							"yeildamt",1,
							"threshold",18
							))),
					"isharvest",true
							));

		break;
		
		case "grass workspot":
			level=0;
			workSpeed=ws;
			maxworkers = 2;
			improves="grass patch";
			capacity.put("cutters", 12);
			capacity.put("grass", 10);
			
			techreq.add("basic");
			recipes.add(new Data(
					"type","paw_cut",
					"name","Grass Cutting",
					"workspeed",12,
					"techreq",new ArrayList<>(List.of("basic")),
					"productamt",new ArrayList<>(List.of(1)),
					"uses",new ArrayList<>(),
					"useamt",new ArrayList<>(),
					"products",new ArrayList<>(List.of(new Data(
							"type","grass-cutting",
							"istoken",true,
							"isyeild",true,
							"yeildtype","grass",
							"yeildamt",1,
							"threshold",6
							))),
					"isharvest",true
							));

		break;
		
		case "cuttery":
			level=0;
			workSpeed=ws;
			maxworkers = 2;
			improves="branch";
			capacity.put("cutters", 11);
			capacity.put("wood", 20);
			capacity.put("wood-cutting", 8);
			
			techreq.add("basic");
			
			recipes.add(new Data(
			"type","paw_cut",
			"name","Wood Cutting",
			"workspeed",12,
			"techreq",new ArrayList<>(List.of("basic")),
			"productamt",new ArrayList<>(List.of(1)),
			"uses",new ArrayList<>(),
			"useamt",new ArrayList<>(),
			"products",new ArrayList<>(List.of(new Data(
					"type","wood-cutting",
					"istoken",true,
					"isyeild",true,
					"yeildtype","wood",
					"yeildamt",1,
					"threshold",8
					))),
			"isharvest",true
					));

			recipes.add(new Data(
			"type","basic_cut",
			"name","Wood Cutting with Cutters",
			"workspeed",12,
			"techreq",new ArrayList<>(List.of("basic")),
			"productamt",new ArrayList<>(List.of(1)),
			"uses",new ArrayList<>(List.of(product("cutters"))),
			"useamt",new ArrayList<>(List.of(1)),
			"usedmg",1,
			"products",new ArrayList<>(List.of(new Data(
					"type","wood-cutting",
					"istoken",true,
					"isyeild",true,
					"yeildtype","wood",
					"yeildamt",1,
					"threshold",4
					))),
			"isharvest",true
					));
		break;
		
		case "cutter pile":
			level=1;
			workSpeed=ws;
			maxworkers = 1;
			isStorage = true;
			capacity.put("cutters", 12);
			
			techreq.add("cutters");
			recipes.add(recipe("storage"));

		break;
		
		case "stone pile":
			level=1;
			workSpeed=ws;
			maxworkers = 1;
			isStorage = true;
			capacity.put("stone", 4);
			
			techreq.add("basic");
			recipes.add(recipe("storage"));

		break;
		
		
		case "stonechip pile":
			level=1;
			workSpeed=ws;
			maxworkers = 1;
			isStorage = true;
			capacity.put("stonechip", 12);
			
			techreq.add("basic");
			recipes.add(recipe("storage"));

		break;
		
		case "rootcut pile":
			level=1;
			workSpeed=ws;
			maxworkers = 1;
			isStorage = true;
			capacity.put("rootcut", 12);
			
			techreq.add("basic");
			recipes.add(recipe("storage"));

		break;
		
		case "wood pile":
			level=1;
			workSpeed=ws;
			maxworkers = 1;
			isStorage = true;
			capacity.put("wood", 20);
			
			techreq.add("basic");
			recipes.add(recipe("storage"));

		break;
		
		case "grass pile":
			level=1;
			workSpeed=ws;
			maxworkers = 1;
			isStorage = true;
			capacity.put("grass", 10);
			
			techreq.add("basic");
			recipes.add(recipe("storage"));

		break;
		
		case "berry pile":
			level=1;
			workSpeed=ws;
			maxworkers = 1;
			isStorage = true;
			capacity.put("berry", 16);
			
			techreq.add("foraging");
			recipes.add(recipe("storage"));

		break;
		
		case "seed pile":
			level=1;
			workSpeed=ws;
			maxworkers = 1;
			isStorage = true;
			capacity.put("seed", 10);
			
			techreq.add("foraging");
			recipes.add(recipe("storage"));

		break;
		
		case "mushroom pile":
			level=1;
			workSpeed=ws;
			maxworkers = 1;
			isStorage = true;
			capacity.put("mushroom", 6);
			
			techreq.add("foraging");
			recipes.add(recipe("storage"));

		break;
					
		case "research spot":
			level=2; 	workSpeed=ws/2; 	maxworkers = 1;
			capacity.put("prototype-cutter", 1); capacity.put("grassfiber", 12); capacity.put("cutters", 12); 
			capacity.put("offpoint-model", 1);   capacity.put("stonechip", 12);  capacity.put("rootcut", 12);
			capacity.put("test-progress", 3); 
			
			capacity.put("wood", 9);
			
			techreq.add("basic");
			
			recipes.add(recipe("none"));
								
			recipes.add(new Data(
			"type","test",
			"name","Test Recipe",
			"workspeed",12,
			"techreq",new ArrayList<>(List.of("basic")),
			"obsolete",new ArrayList<>(List.of("test")),
			"uses",new ArrayList<>(List.of("rootcut")),
			"useamt",new ArrayList<>(List.of(1)),
			"productamt",new ArrayList<>(List.of(1)),
			"products",new ArrayList<>(List.of(new Data(
					"type","test-progress",
					"istoken",true,
					"isyeild",true,
					"yeildtype","stonechip",
					"yeildamt",6,
					"threshold",3
							))
					)));
			break;
			
			
		case "building test":
			level=2;
			workSpeed=ws;
			maxworkers = 1;
			capacity.put("wood", 20);
			capacity.put("build",13);
			capacity.put(name+" phase "+1,1);
			capacity.put(name+" phase "+2,1);

			recipes.add(recipe("none"));

			Data phase1 = build(name,1,9,2);
			phase1.addNote("uses",new ArrayList<>(List.of(
			      new Data("type","wood"))));
			phase1.addNote("useamt",new ArrayList<>(List.of(
						   1)));
			recipes.add(phase1);
			
			Data phase2 = build(name,2,4,2);
			phase2.addNote("uses",new ArrayList<>(List.of(
			      new Data("type","wood"))));
			phase2.addNote("useamt",new ArrayList<>(List.of(
						   1)));
			recipes.add(phase2);
		break;		
		
		case "cutter maker":
			level=2;
			workSpeed=ws/2;
			maxworkers = 1;
			capacity.put("cutters", 9);
			capacity.put("grassfiber", 7);
			capacity.put("stonechip", 10);
			
			techreq.add("cutters");
			recipes.add(recipe("basic_cutter"));
			
	
			break;
			
		case "offpoint":
			level=2;
			workSpeed=ws*2;
			maxworkers = 2;
			capacity.put("berry", 16);
			capacity.put("seed", 8);
			capacity.put("mushroom", 4);
			
			techreq.add("foraging");
			recipes.add(recipe("basic_berry"));
			recipes.add(recipe("basic_seed"));
			recipes.add(recipe("basic_mushroom"));
		break;
		
		case "digsite":
			level=3;
			workSpeed=12;
			maxworkers=1;
			capacity.put("burrow",1);
			
			techreq.add("burrowing");
			recipes.add(recipe("dig"));
		break;
	
		
		
		default:
		System.out.println("Invalid building type "+type);
		break;
		}
		
		Data selectedrecipe = recipes.isEmpty() ? null : recipes.getFirst();
		
		Data building = new Data("Name: ",name,"Worked: ",false,"Level: ",level,"Type: ",type,"Uses: ",uses,"Useamt: ",useamt,"position",pos,
				"Produces: ",products,"Prodamt: ",productamt,"Has: ",has,"Needs: ",needs,"width",width,"height",height,"workspeed",workSpeed,
				"on",on,"improves",improves,"workspots",workspots,"storagespots",storagespots,"capacity",capacity,"maxworkers",maxworkers,
				"isStorage",isStorage,"selectedrecipe",selectedrecipe,"recipes",recipes,"techreq",techreq);
		
		return building;

	}
	
	
	
	
	public String[] products = {"null","wood","stonechip","rootcut","root","grass","grassfiber","cutters","berry","mushroom","seed","cloak",
			"prototype-cutter","offpoint-model","burrow","burrow-model"};
	
	public Data recipe(String type)
	{
		String name = null;
		ArrayList<Data> uses = new ArrayList<>();
		ArrayList<Data> products = new ArrayList<>();
		ArrayList<Integer> useamt = new ArrayList<>();
		ArrayList<Integer> productamt = new ArrayList<>();
		ArrayList<String> techreq = new ArrayList<>();
		ArrayList<String> obsolete = new ArrayList<>();
		int workspeed = 0;
		
		switch(type)
		{
		case "storage":
			name = "Storage";
			workspeed=12;
			techreq.add("basic");
			
		products.add(product("null"));
			productamt.add(0);
		uses.add(product("null"));
		 	useamt.add(0);
			
		break;
		
		// Reaserch Recipes
		
		case "none":
			name = "No Reaserch";
			workspeed=12;
			techreq.add("basic");
			
		products.add(product("null"));
			productamt.add(0);
		uses.add(product("null"));
		 	useamt.add(0);
			
		break;
		
		
		case "test":
			name="Test";
			workspeed=12;
			techreq.add("basic");
			obsolete.add("test");
			
		products.add(product("test-progress"));
			productamt.add(1);
		uses.add(product("rootcut"));
		 	useamt.add(1);		 	
			break;
			
		case "cutters":
			name="Cutter Reaserch";
			workspeed=12;
			techreq.add("basic");
			obsolete.add("cutters");
			
		products.add(product("prototype-cutter"));
			productamt.add(1);
		uses.add(product("stonechip"));
		 	useamt.add(4);
		uses.add(product("grassfiber"));
		 	useamt.add(4);
		 	
			break;
			
		case "burrowing":
			name="Burrow Reaserch";
			workspeed=12;
			techreq.add("cutters");
			obsolete.add("burrowing");
			
		products.add(product("burrow-model"));
			productamt.add(1);
		uses.add(product("cutters"));
		 	useamt.add(6);
		 	
			break;

		case "foraging":
			name="Forage Reaserch";
			workspeed=12;
			techreq.add("basic");
			obsolete.add("foraging");
			
		products.add(product("offpoint-model"));
			productamt.add(1);
		uses.add(product("rootcut"));
		 	useamt.add(4);
		uses.add(product("grassfiber"));
		 	useamt.add(4);
		 	
			break;
		
		case "dig":
			name="Burrow Digging";
			workspeed=12;
			techreq.add("burrowing");
			
		products.add(product("burrow"));
			productamt.add(1);
		 	
			break;
			
		// Level 0 recipes
			
		case "paw_chip":
		name="Manual Chipping";
		workspeed=12;
		techreq.add("basic");
		products.add(product("stonechip"));
			productamt.add(1);
		
		break;
		
		case "paw_cut":
		name="Manual Cutting";
		workspeed=12;
		techreq.add("basic");
		products.add(product("rootcut"));
			productamt.add(1);
		
		break;
		
		case "paw_comb":
		name="Manual Combing";
		workspeed=12;
		techreq.add("basic");
		products.add(product("grassfiber"));
			productamt.add(1);
		
		break;
		
		case "basic_chip":
		name="Basic Chipping";
		workspeed=6;
		techreq.add("cutters");
		products.add(product("stonechip"));
			productamt.add(1);
		uses.add(product("cutters"));
		 	useamt.add(1);
		break;
		
		case "basic_comb":
		name="Basic Combing";
		workspeed=6;
		techreq.add("cutters");
		products.add(product("grassfiber"));
			productamt.add(1);
		uses.add(product("cutters"));
		 	useamt.add(1);
		break;
		
		case "basic_cut":
			name="Basic Cutting";
			workspeed=6;
			techreq.add("cutters");
			products.add(product("rootcut"));
				productamt.add(1);
			uses.add(product("cutters"));
			 	useamt.add(1);
		break;
		
		// Level 2 recipes
		
		case "basic_cutter":
			name="Basic Cutter Making";
			workspeed=12;
			techreq.add("cutters");
		products.add(product("cutters"));
			productamt.add(3);
		uses.add(product("stonechip"));
		 	useamt.add(1);
		uses.add(product("grassfiber"));
		 	useamt.add(1);
		break;
		
		
		case "basic_berry":
			name="Basic Berry Foraging";
			workspeed=12;
			techreq.add("foraging");
		products.add(product("berry"));
			productamt.add(2);
		break;
		
		case "basic_seed":
			name="Basic Seed Foraging";
			workspeed=6;
			techreq.add("foraging");
		products.add(product("seed"));
			productamt.add(2);
		break;
		
		case "basic_mushroom":
			name="Basic Mushroom Foraging";
			workspeed=12;
			techreq.add("foraging");
		products.add(product("mushroom"));
			productamt.add(1);
		break;
		
		
		}
	
		
		return new Data ("Name: ",name,"type",type,"uses",uses,"products",products,"useamt",useamt,"productamt",productamt,"techreq",techreq,"workspeed",workspeed,
				"obsolete",obsolete);
	}
	

	//Ensure you add the uses and useamt notes!
	public Data build(String name, int phase,int ammount,int difficulty)
	{
		return new Data(
		"type","phase-"+phase,
		"name",name+" Phase "+phase,
		"workspeed",12,
		"construction",true,
		"techreq",new ArrayList<>(List.of(phase > 1 ? (name+" phase "+(phase-1)):"basic")),
		"obsolete",new ArrayList<>(List.of(name+" phase "+phase)),
		"productamt",new ArrayList<>(List.of(1,1)),
		"products",new ArrayList<>(List.of(
				new Data("type",name+" phase "+phase,
				"istoken",true,
				"istech",true,
				"tech",name+" phase "+phase,
				"threshold",ammount*difficulty
						),
				new Data("type","build progress",
						"istoken",true,
						"isyeild",true,
						"yeildtype","build",
						"yeildamt",1,
						"threshold",difficulty
						)
				
				)
				));
	}
	
	public Data rNode(String type)
	{
		Vector3f position = new Vector3f(0f,0f,0f);
		ArrayList<Data> buildings = new ArrayList<>();
		ArrayList<Data> snapspots = new ArrayList<>();
		ArrayList<Data> storagespots = new ArrayList<>();
		HashMap<String,Integer> has = new HashMap<>();
		boolean nestable = false;
		String category = null;
		switch(type)
		{
		case "branch":
			category = "treefall";
		break;
		
		case "stones":
			category = "common";
		break;
		
		case "grass patch":
			category = "common";
		break;
		
		case "tree":
			category = "tree";
		break;
		
		case "hollow log":
			category = "nest";
			nestable=true;
		break;
		
		case "berry bush":
			category = "forage";
		break;
		
		default:
			System.out.println("[GRN] Invalid resourceNode type");
		break;
		}
		return new Data("type",type,"improved",false,"category",category,"position",position,"buildings",buildings,"snapspots",snapspots,"storagespots",storagespots,"has",has,
				"nestable",nestable,"nestfor",null,"connectcap",0) ;
	}
	

	public Data makeWorkSpot(Vector3f position, String workType, List<String> allowed) {
	
		Data spot = new Data("position",position,"type",workType,"allowed",allowed,"occupied",false);
		return spot;
	}

	public Data makeStorageSpot(Vector3f position, String product, int capacity,List<String> visuals,String viscode) {
		
		Data spot = new Data("position",position,"type",product,"capacity",capacity,"occupied",false,"visualNames",visuals,"viscode",viscode);
		return spot;
	}
	
	public Data makeSnapSpot(Vector3f position,String nodetype,String viscode) {
		
		Data spot = new Data("position",position,"nodetype",nodetype,"viscode",viscode,"occupied",false,"building",null);
		return spot;
	}
	
	
	public Data product(String type)
	{
		int value =0;
		int satiety = 0;
		boolean isfood = false;
		boolean istech = false;
		boolean istoken = false;
		boolean isyeild = false;
		int threshold = 0;
		String tech = null;
		String yeildtype = null;
		int yeildamt = 0;
		int uses_left = 0;
		int uses_total = 0;
		
		switch(type)
		{
		case "cutters":
			uses_left=24;
			uses_total=24;
		break;
		
		case "rootcut":
			satiety=4;
			isfood=true;
		break;
		case "berry":
			satiety=4;
			isfood=true;
		break;
		case "mushroom":
			satiety=12;
			isfood=true;
		break;
		case "seed":
			satiety=6;
			isfood=true;
		break;
		
		case "prototype-cutter":
			istech=true;
			tech="cutters";
		break;
		
		case "offpoint-model":
			istech=true;
			tech="foraging";
		break;
		
		case "burrow-model":
			istech=true;
			tech="burrowing";
		break;
		
		case "root":
		
		break;
		
		case "test-progress":
			istoken=true;
			isyeild=true;
			yeildtype="stonechip";
			yeildamt=6;
			threshold=3;
		break;
		}
		
		Data product = new Data("Type: ",type,"Value: ",value,"Satiety: ",satiety,"isfood",isfood,"istech",istech,"tech",tech,
				"istoken",istoken,"threshold",threshold,"isyeild",isyeild,"yeildtype",yeildtype,"yeildamt",yeildamt,"uses_left",uses_left,
				"uses_total",uses_total);
		return product;
	}
	
	public Data generateBill(String product, int amount, Data source, Data destination, double urgency) {
	    return new Data(
	        "product: ", product,
	        "amount: ", amount,
	        "source: ", source,
	        "destination: ", destination,
	        "urgency: ", urgency,
	        "status:", "pending"  // pending, in_progress, completed
	    );
	}
	
	
	public ArrayList<Data> toArraylist(Data[] list)
	{
		ArrayList<Data> aList = new ArrayList<>();
		for(Data d : list)
		{
			aList.add(d);
		}
		return aList;
	}
	
	public ArrayList<String> toArraylist(String[] list)
	{
		ArrayList<String> aList = new ArrayList<>();
		for(String d : list)
		{
			aList.add(d);
		}
		return aList;
	}

}

