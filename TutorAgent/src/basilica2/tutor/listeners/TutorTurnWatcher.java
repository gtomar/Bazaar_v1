package basilica2.tutor.listeners;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.COV_Event;
import basilica2.agents.events.COVexternal_Event;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.listeners.BasilicaAdapter;
import basilica2.tutor.events.DoneTutoringEvent;
import basilica2.tutor.events.MoveOnEvent;
import basilica2.tutor.events.StudentTurnsEvent;
import basilica2.tutor.events.TutorTurnsEvent;
import basilica2.tutor.events.TutoringStartedEvent;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;
import edu.cmu.cs.lti.project911.utils.time.Timer;

/**
 * a companion to TutorActor - packages student input into "bursts" and also
 * manages (some) student input timeouts
 * 
 * @author dadamson
 */
public class TutorTurnWatcher extends BasilicaAdapter implements TimeoutReceiver
{
	public static String GENERIC_NAME = "TutoringTurnTakingCordinator";
	public static String GENERIC_TYPE = "Cordinator";
	private double tickSize = 0.1;
	private int burst_ticks = 50; // Equals 5 second bursts
	private int turn_response_timeout = 60;
	private List<String> studentTurns = null;
	private List<String> contributors = null;
	private List<String> annotations = null;
	private boolean isCoordinating = false;
	private int tickCount = 0;
	private String currentConcept = null;
	private Timer responseTimer = null;
	private InputCoordinator source;

	public TutorTurnWatcher(Agent a)
	{
		super(a);

		studentTurns = new ArrayList<String>();
		contributors = new ArrayList<String>();
		annotations = new ArrayList<String>();

		new Timer(tickSize, new TimeoutReceiver()
		{
			@Override
			public void timedOut(String id)
			{
				tick();
				new Timer(tickSize, this).start();
			}

			@Override
			public void log(String from, String level, String msg)
			{}
		}).start();

	}

	public void updateEpisodeLog(String dialog, String logEntry, String annotations)
	{
		try
		{
			String filename = "behaviors" + File.separator + "episodes" + File.separator + getAgent().getName() + ".episodes.txt";
			FileWriter fw = new FileWriter(filename, true);
			fw.write(System.currentTimeMillis() + "\t" + getAgent().getName() + "\t" + dialog + "\t" + logEntry + "\t" + annotations + "\n");
			fw.flush();
			fw.close();
		}
		catch (Exception e)
		{
			log(Logger.LOG_ERROR, "Error while updating Episode Log File (" + e.toString() + ")");
		}
	}

	// TODONE: hook up tick() to timer
	protected void tick()
	{
		if (isCoordinating)
		{
			if (tickCount >= burst_ticks)
			{
				tickCount = 0;
				if (studentTurns.size() > 0)
				{
					StudentTurnsEvent ste = new StudentTurnsEvent(source, studentTurns, contributors, annotations);
					source.pushEvent(ste);
					contributors = new ArrayList<String>();
					studentTurns = new ArrayList<String>();
				}
			}
			else
			{
				tickCount++;
			}
		}
	}

	private void handleTutoringStartedEvent(TutoringStartedEvent tse)
	{
		isCoordinating = true;
		tickCount = 0;
		currentConcept = tse.getScenario();
		studentTurns.clear();
		contributors.clear();
		annotations.clear();
	}

	private void handleTutorTurnsEvent(TutorTurnsEvent tte)
	{
		tickCount = 0;
		studentTurns.clear();
		contributors.clear();
		annotations.clear();
		if (responseTimer != null)
		{
			responseTimer.stopAndQuit();
		}
		responseTimer = new Timer(turn_response_timeout, "RESPONSE_TIMEOUT", this);
		responseTimer.start();
	}

	private void handleDoneTutoringEvent(DoneTutoringEvent dte)
	{
		isCoordinating = false;
		currentConcept = null;
		if (responseTimer != null)
		{
			responseTimer.stopAndQuit();
		}
	}

	private void handleMessageEvent(MessageEvent me)
	{
		if (isCoordinating)
		{
			updateEpisodeLog(currentConcept, me.getFrom() + "\t" + me.getText(), "ST_TBA");
			studentTurns.add(me.getText());
			contributors.add(me.getFrom());
			annotations.addAll(Arrays.asList(me.getAllAnnotations()));
		}
	}
	private void handleCOVexternal_Event(COVexternal_Event ce)
	{
		if (isCoordinating)
		{
			updateEpisodeLog(currentConcept, ce.getFrom() + "\t" + ce.getMessage(), "ST_COE");
			studentTurns.add(ce.getMessage().toUpperCase());
			contributors.add(ce.getFrom());
			annotations.addAll(Arrays.asList(ce.getMessage().toUpperCase()));
			
			String message = ce.getMessage();
			List<String> query = new ArrayList<String>();
			//0,       1,               2,           3
			//Fitness, Parents' Health, Family Size, Education
			
		    for (String s: message.split(",")){
		          String[] t = s.split("_");
		          query.add(t[1]);
		    }
		    
		    String[] result = checkInCSV("records.csv",query);
		    query.add(result[0]); //Record#
		    query.add(result[5]); //Gender
		    query.add(result[7]); //Performance
		    query.add(result[8]); //Name
		    
		    COV_Event ce_new = new COV_Event(source,"",query);
			source.pushEventProposal(ce_new,1.0,60);
		}
	}
	
	private String[] checkInCSV(String csvFile, List<String> query)
	{

		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";

		try {

			br = new BufferedReader(new FileReader(csvFile));
			while ((line = br.readLine()) != null) {

			    // use comma as separator
				String[] record = line.split(cvsSplitBy);
				//0,        1,       2,               3,            4,          5,      6,              7,           8,
                //Record#,	Fitness, Parents' Health, Family Size,	Education,	Gender,	Home Climate,	Performance, Name
				
				
                if(record[1].equals(query.get(0)) && // Fitness
                   record[2].equals(query.get(1)) && // Parents' Health
                   record[3].equals(query.get(2)) && // Family size
                   record[4].equals(query.get(3))    // Education
                   )
                {
                	return record;
                }
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		System.out.println("Error");
		String [] empty = null;
        return empty;
		
	}
	public void timedOut(String id)
	{
		informObservers("<timedout id=\"" + id + "\" />");
		MoveOnEvent mve = new MoveOnEvent(source);
		source.queueNewEvent(mve);
		contributors = new ArrayList<String>();
		studentTurns = new ArrayList<String>();
	}

	public void log(String from, String level, String msg)
	{
		log(level, from + ": " + msg);
	}

	@Override
	public void processEvent(InputCoordinator source, Event event)
	{
	}

	@Override
	public Class[] getListenerEventClasses()
	{
		return new Class[] {};
	}

	@Override
	public void preProcessEvent(InputCoordinator source, Event e)
	{
		this.source = source;
		if (e instanceof TutoringStartedEvent)
		{
			handleTutoringStartedEvent((TutoringStartedEvent) e);
		}
		else if (e instanceof TutorTurnsEvent)
		{
			handleTutorTurnsEvent((TutorTurnsEvent) e);
		}
		else if (e instanceof DoneTutoringEvent)
		{
			handleDoneTutoringEvent((DoneTutoringEvent) e);
		}		
		else if (e instanceof MessageEvent)
		{
			handleMessageEvent((MessageEvent) e);
		}
		else if(e instanceof COVexternal_Event)
		{
			handleCOVexternal_Event((COVexternal_Event) e);
		}
	}

	@Override
	public Class[] getPreprocessorEventClasses()
	{
		return new Class[] { MessageEvent.class, TutoringStartedEvent.class, TutorTurnsEvent.class, DoneTutoringEvent.class, COVexternal_Event.class, COV_Event.class };
	}
}
