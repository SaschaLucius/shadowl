package test;

public class DB implements EventListener {
	@Override
	public Event incomingEvent(Event e) {
		switch (e.type) {
		case ADD:
			System.out.println("db");
			return new ResultEvent();
		default:
			throw new UnsupportedOperationException();
		}
	}
}
