package test;

import java.util.List;

public class AddEvent extends Event {
	List<Ad> ads;

	AddEvent(List<Ad> ads) {
		super(EventType.ADD);
		this.ads = ads;// todo copy
	}
}
