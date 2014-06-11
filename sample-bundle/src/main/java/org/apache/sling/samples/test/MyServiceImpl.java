package org.apache.sling.samples.test;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;

@Component
@Service
public class MyServiceImpl implements MyService {

	@Override
	public String getString() {
		return "Uno";
	}
}
