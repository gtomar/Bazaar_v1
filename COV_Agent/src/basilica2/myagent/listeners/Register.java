package basilica2.myagent.listeners;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;

import edu.cmu.cs.lti.basilica2.core.Event;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.COVexternal_Event;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PresenceEvent;
import basilica2.agents.events.PromptEvent;
import basilica2.agents.listeners.BasilicaPreProcessor;
import basilica2.social.events.DormantStudentEvent;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import basilica2.myagent.Topic;
import basilica2.myagent.User;

public class Register implements BasilicaPreProcessor
{

	public Register() 
    {
    	
    	topicList = new ArrayList<Topic>();
    	userList = new ArrayList<User>();
    	
		String dialogueConfigFile="dialogues/dialogues-config.xml";
    	loadconfiguration(dialogueConfigFile);
	}    
    
	public ArrayList<Topic> topicList;	
	public ArrayList<User> userList;

	private void loadconfiguration(String f)
	{
		try
		{
			DOMParser parser = new DOMParser();
			parser.parse(f);
			Document dom = parser.getDocument();
			NodeList dialogsNodes = dom.getElementsByTagName("dialogs");
			if ((dialogsNodes != null) && (dialogsNodes.getLength() != 0))
			{
				Element conceptNode = (Element) dialogsNodes.item(0);
				NodeList conceptNodes = conceptNode.getElementsByTagName("dialog");
				if ((conceptNodes != null) && (conceptNodes.getLength() != 0))
				{
					for (int i = 0; i < conceptNodes.getLength(); i++)
					{
						Element conceptElement = (Element) conceptNodes.item(i);
						String conceptName = conceptElement.getAttribute("concept");
						//String conceptDetailedName = conceptElement.getAttribute("description"); 
						//Topic topic = new Topic(conceptName, conceptDetailedName);
						//topicList.add(topic);
					}
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}	    
	}
	
	
	public int IsInUserList(String id)
	{
		for (int i = 0; i < userList.size(); i++)
		{
			if (userList.get(i).id.equals(id))
			{
				return i;
			}
		}
		
		return -1;
	}
	
	public void incrementScore(int increment)
	{
		for (int i = 0; i < userList.size(); i++)
		{
			userList.get(i).score += increment;
		}
	}
	/**
	 * @param source the InputCoordinator - to push new events to. (Modified events don't need to be re-pushed).
	 * @param event an incoming event which matches one of this preprocessor's advertised classes (see getPreprocessorEventClasses)
	 * 
	 * Preprocess an incoming event, by modifying this event or creating a new event in response. 
	 * All original and new events will be passed by the InputCoordinator to the second-stage Reactors ("BasilicaListener" instances).
	 */
	@Override
	public void preProcessEvent(InputCoordinator source, Event event)
	{
		if (event instanceof MessageEvent)
		{
			MessageEvent me = (MessageEvent)event;
			String[] annotations = me.getAllAnnotations();
			User user = getUser(me.getFrom());
			
	    }
		else if (event instanceof DormantStudentEvent)
		{
		
			String prompt_message = "It looks like there is no activity.";
			PromptEvent prompt = new PromptEvent(source, prompt_message , "POKING");
			source.queueNewEvent(prompt);
					
		}
		else if (event instanceof COVexternal_Event)
		{
			String prompt_message = "It looks like you made following selections.\n";
			String message = ((COVexternal_Event) event).getMessage();
		    for (String s: message.split(",")){
		          String[] t = s.split("_");
		          prompt_message += t[0] + " ---> " + t[1] + "\n";
		    }
			
			PromptEvent prompt = new PromptEvent(source, prompt_message , "SELECTIONS");
			source.queueNewEvent(prompt);			
		}
		else if (event instanceof PresenceEvent)
		{
			PresenceEvent pe = (PresenceEvent) event;

			if (!pe.getUsername().contains("Agent") && !source.isAgentName(pe.getUsername()))
			{

				String username = pe.getUsername();
				String userid = pe.getUserId();
				
				if(userid == null)
					return;
				
				Date date= new Date();
				Timestamp currentTimestamp= new Timestamp(date.getTime());
				int userIndex = IsInUserList(userid);
				if (pe.getType().equals(PresenceEvent.PRESENT))
				{
					if(userIndex == -1)
					{
						String prompt_message = "Welcome, " + username + "\n";
						
						User newuser = new User(username, userid, currentTimestamp);
						userList.add(newuser);
						
						PromptEvent prompt = new PromptEvent(source, prompt_message , "INTRODUCTION");
						source.queueNewEvent(prompt);
					}
					
				}
				else if (pe.getType().equals(PresenceEvent.ABSENT))
				{
					if(userIndex != -1)
					{
						userList.remove(userIndex);
					}
				}
			}
		}
	}

	
	public User getUser(String name)
	{
		for (int i = 0; i < userList.size(); i++)
		{
			if (userList.get(i).name.equals(name))
			{
				return userList.get(i);
			}
		}
		
		return null;
		
	}

	@Override
	public Class[] getPreprocessorEventClasses() {
		return new Class[]{MessageEvent.class, PresenceEvent.class, DormantStudentEvent.class, COVexternal_Event.class};
	}
	

}
