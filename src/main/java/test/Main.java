package test;

import java.util.ArrayList;
import java.util.List;

public class Main {

	public static void main(String[] args) {
		EventBus.instance().subscribe(EventType.ADD, new DB());
		EventBus.instance().subscribe(EventType.ADD, new Lucene());

		List<Ad> list = new ArrayList<>();
		list.add(new Ad(1));
		list.add(new Ad(2));
		list.add(new Ad(3));

		EventBus.instance().submit(new AddEvent(list));
	}

}
