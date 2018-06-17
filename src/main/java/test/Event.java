package test;

public class Event {
	private int id = 0;
	EventType type;

	protected Event(EventType type) {
		this.type = type;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

}
