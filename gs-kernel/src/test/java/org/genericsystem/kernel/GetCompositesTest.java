package org.genericsystem.kernel;

import org.genericsystem.common.Generic;
import org.testng.annotations.Test;

@Test
public class GetCompositesTest extends AbstractTest {

	public void test001() {
		Engine root = new Engine();
		Generic vehicle = root.addInstance("Vehicle");
		Generic power = vehicle.addAttribute("power");
		vehicle.addAttribute("option");

		assert vehicle.getComposite("power") == power;
	}

	public void test002() {
		Engine root = new Engine();
		Generic vehicle = root.addInstance("Vehicle");
		Generic car = root.addInstance(vehicle, "Car");
		vehicle.addAttribute("power");
		Generic carPower = car.addAttribute("power");
		vehicle.addAttribute("option");

		assert car.getComposites("power").first() == carPower;
	}

}
