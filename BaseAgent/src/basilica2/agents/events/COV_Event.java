package basilica2.agents.events;

import basilica2.agents.components.InputCoordinator;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;

public class COV_Event extends Event
{
	public static String GENERIC_NAME = "COV_EVENT";
	public String concept;
	
	public COV_Event(Component source, String concept)
	{
		super(source);
		this.concept = concept;
	}

	public String getConcept() {

		return this.concept;
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
