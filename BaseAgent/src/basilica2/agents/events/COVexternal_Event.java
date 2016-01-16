package basilica2.agents.events;

import basilica2.agents.components.InputCoordinator;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;

public class COVexternal_Event extends Event
{
	public static String GENERIC_NAME = "COVEXTERNAL_EVENT";
	public String message;
	public String from;
	
	public COVexternal_Event(Component source, String message, String from)
	{
		super(source);
		this.message = message;
		this.from = from;
	}

	public String getMessage() {

		return this.message;
	}

	public String getFrom() {

		return this.from;
	}
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return GENERIC_NAME;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}




}
