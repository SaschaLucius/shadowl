package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventBus {
	private static EventBus bus = new EventBus();

	private EventBus() {
	}

	public static EventBus instance() {
		return bus;
	}

	private Map<EventType, List<EventListener>> map = new HashMap<EventType, List<EventListener>>();

	void subscribe(EventType event, EventListener object) {
		if (!map.containsKey(event)) {
			map.put(event, new ArrayList<EventListener>());
		}
		List<EventListener> list = map.get(event);
		if (!list.contains(object)) {
			list.add(object);
		}
	}

	void unsubscribe(EventType event, EventListener object) {
		if (!map.containsKey(event)) {
			map.put(event, new ArrayList<EventListener>());
		}
		List<EventListener> list = map.get(event);
		if (list.contains(object)) {
			list.remove(object);
		}
	}

	void submit(Event event) {
		if (map.containsKey(event.type)) {
			for (EventListener o : map.get(event.type)) {
				submit(o.incomingEvent(event));
			}
		}
	}
}
