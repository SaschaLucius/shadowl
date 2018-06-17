package test;

public class Lucene implements EventListener {

	@Override
	public Event incomingEvent(Event e) {
		switch (e.type) {
		case ADD:
			System.out.println("lucene");
			return new ResultEvent();
		default:
			throw new UnsupportedOperationException();
		}
	}
}
