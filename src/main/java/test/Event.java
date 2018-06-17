package test;

public class Event {
	EventType type;
	private int id = 0;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	protected Event(EventType type) {
		this.type = type;
	}

}
