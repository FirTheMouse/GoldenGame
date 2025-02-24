package com.golden.game;

import java.util.*;

public class NameGenerator 
{	
private final List<String> starts = new ArrayList<String>();
private final List<String> middles = new ArrayList<String>();
private final List<String> middlesExtra = new ArrayList<String>();
private final List<String> ends = new ArrayList<String>();

	public String generateNames(int amount, String namebasetitle, int[] titleArrangments, String namebase1, int[] firstArrangements, String namebase2, int[] secondArrangements)
	{
		 
		   Random random = new Random();
		   StringBuilder result = new StringBuilder();	
		   
		   for(int i = 0; i < amount; i++) 
		   {
			    int firstArrange = firstArrangements[random.nextInt(firstArrangements.length)];
		        int secondArrange = secondArrangements[random.nextInt(secondArrangements.length)];
		        int titleArrange = titleArrangments[random.nextInt(titleArrangments.length)];
		        
		        clearComponents();
	            initalizeComponents(namebasetitle);
	            String title = buildName(titleArrange);
	            
	            clearComponents();
	            initalizeComponents(namebase1);
	            String firstName = buildName(firstArrange);
	            
	            clearComponents();
	            initalizeComponents(namebase2);
	            String surname = buildName(secondArrange);
	            
	            result.append(title).append(" ").append(firstName).append(" ").append(surname);
	            if(i < amount - 1) 
	            {
	                result.append("\n");
	            }
	        }
	        return result.toString();
	}

	public String generateNames(int amount, String namebase1, int[] firstArrangements, String namebase2, int[] secondArrangements)
	{
		 
		   Random random = new Random();
		   StringBuilder result = new StringBuilder();	
		   
		   for(int i = 0; i < amount; i++) 
		   {
			    int firstArrange = firstArrangements[random.nextInt(firstArrangements.length)];
		        int secondArrange = secondArrangements[random.nextInt(secondArrangements.length)];
		        
	            clearComponents();
	            initalizeComponents(namebase1);
	            String firstName = buildName(firstArrange);
	            
	            clearComponents();
	            initalizeComponents(namebase2);
	            String surname = buildName(secondArrange);
	            
	            result.append(firstName).append(" ").append(surname);
	            if(i < amount - 1) 
	            {
	                result.append("\n");
	            }
	        }
	        return result.toString();
	}
	
	public String generateNames(int amount, String namebase1, int[] firstArrangements)
	{
		 
		   Random random = new Random();
		   StringBuilder result = new StringBuilder();	
		   
		   for(int i = 0; i < amount; i++) 
		   {
			    int firstArrange = firstArrangements[random.nextInt(firstArrangements.length)];
		        
	            clearComponents();
	            initalizeComponents(namebase1);
	            String firstName = buildName(firstArrange);
	            
	            result.append(firstName);
	            if(i < amount - 1) 
	            {
	                result.append("\n");
	            }
	        }
	        return result.toString();
	}
	
	private void clearComponents() 
	{
	    starts.clear();
	    middles.clear();
	    ends.clear();
	    middlesExtra.clear();
	}

	private void initalizeComponents(String namebase)
	{
		for(int i=0;i<50;i++)
		{
			switch (namebase)
			{
			case "nouns":
			    starts.add(process(i,"Ash,Autumn,Brook,Cloud,Creek,Dawn,Dusk,Echo,Field,Fire,Fox,Glen,Grove,Haven,Hill,Lake,Leaf,Moon,Oak,Pine,Rain,"
			    		+ "River,Rose,Sea,Shadow,Sky,Snow,Star,Storm,Sun,Swift,Vale,Wind,Wolf"));
			    
			    middles.add(process(i,""));
			    
			    //Mouse names
			    middlesExtra.add(process(i,"Chip,Branch,Moss,Fern,Flint,Frost,Glade,Hope,Keene,March,North,Quill,Spark,Pebble,Thorn,Trail,Willow,Root,Daisy,Oak,Pine,Maple,Sap"));
			    
			    ends.add(process(i,"bark,bay,bird,brand,burg,bloom,born,brook,crest,dale,drift,forest,fall,field,ford,flower,gate,garde,glen,grove,hall,haven,hill,hold,light,more,meadow,path,river,rain,ridge,rise,song,"
			    		+ "star,shield,stone,stand,smith,stone,shade,stream,vale,view,ville,way,well,wind,wood,worth"));
			    
			    break;
			default:
				System.out.println("Invalid namebase");
				break;
			}
		}
	}
	
	private String process(int i, String lists)
	{
		String list = lists;
		String bank[]=list.split(",");
		return bank[i % bank.length];
	}
	
	private String buildName(int arrange)
	{
		String start = starts.get((int)(Math.random()*starts.size()));
		String middle = middles.get((int)(Math.random()*middles.size()));
		String middleExtra = middlesExtra.get((int)(Math.random()*middlesExtra.size()));
		String end = ends.get((int)(Math.random()*ends.size()));
		
		switch(arrange)
		{
		case 1111:
			return start+middle+middleExtra+end;
		case 1101:
			return start+middle+end;
		case 1001:
			return start+end;
		case 1100:
			return start+middle;
		case 1110:
			return start+middle+middleExtra;
		case 1011:
			return start+middleExtra+end;
		case 1000:
			return start;
		case 0111:
			return middle+middleExtra+end;
		case 0101:
			return middle+end;
		case 0001:
			return end;
		case 0100:
			return middle;
		case 0011:
			return middleExtra+end;
		case 0110:
			return middle+middleExtra;
		case 0010:
			return middleExtra;
		case 1010:
			return start+middleExtra;
		default:
			return "invalid arrangment";
			
		}
	}

}
