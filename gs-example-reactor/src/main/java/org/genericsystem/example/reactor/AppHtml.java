package org.genericsystem.example.reactor;

import io.vertx.core.http.ServerWebSocket;

import org.genericsystem.api.core.ApiStatics;
import org.genericsystem.carcolor.model.Car;
import org.genericsystem.carcolor.model.CarColor;
import org.genericsystem.carcolor.model.Color;
import org.genericsystem.carcolor.model.Diesel;
import org.genericsystem.carcolor.model.Power;
import org.genericsystem.common.Root;
import org.genericsystem.common.Generic;
import org.genericsystem.example.reactor.AppHtml.ExampleReactorScript;
import org.genericsystem.kernel.Engine;
import org.genericsystem.reactor.ReactorStatics;
import org.genericsystem.reactor.annotations.DependsOnModel;
import org.genericsystem.reactor.annotations.RunScript;
import org.genericsystem.reactor.appserver.ApplicationServer;
import org.genericsystem.reactor.appserver.Script;
import org.genericsystem.reactor.composite.CompositeSelect.ColorsSelect;
import org.genericsystem.reactor.generic.FlexDirection;
import org.genericsystem.reactor.generic.GSApp;
import org.genericsystem.reactor.generic.GSTable;
import org.genericsystem.reactor.generic.GSEditor;
import org.genericsystem.reactor.generic.GSMonitor;
import org.genericsystem.reactor.generic.GSComposite.ColorCompositeRadio;
import org.genericsystem.reactor.generic.GSComposite.ColorTitleCompositeFlexElement;
import org.genericsystem.reactor.generic.GSSection.GenericH1Section;
import org.genericsystem.reactor.model.StringExtractor;

@DependsOnModel({ Car.class, Power.class, Diesel.class, Color.class, CarColor.class })
@RunScript(ExampleReactorScript.class)
public class AppHtml extends GSApp {

	public static void main(String[] mainArgs) {
		ApplicationServer.sartSimpleGenericApp(mainArgs, AppHtml.class, "/example-reactor");
	}

	public AppHtml(Root engine, ServerWebSocket webSocket) {
		super(webSocket);
		addStyle("justify-content", "center");
		new ColorsSelect(this).select(StringExtractor.EXTRACTOR, Color.class);
		new ColorTitleCompositeFlexElement(this).select(StringExtractor.MANAGEMENT, Color.class);
		new ColorCompositeRadio(this, FlexDirection.ROW).select(StringExtractor.EXTRACTOR, Color.class);
		new GenericH1Section(this, FlexDirection.COLUMN, "Generic System Reactor Live Demo").addStyle("background-color", "#ffa500");

		select(StringExtractor.SIMPLE_CLASS_EXTRACTOR, gs -> gs[0]);
		createProperty(ReactorStatics.SELECTION);
		new GSTable(this).select(StringExtractor.MANAGEMENT, Car.class);
		new GSTable(this, FlexDirection.ROW).select(StringExtractor.MANAGEMENT, Car.class);
		new GSEditor(this, FlexDirection.ROW) {
			{
				select_(StringExtractor.TYPE_INSTANCE_EXTRACTOR, model -> getProperty(ReactorStatics.SELECTION, model));
				addStyle("justify-content", "center");
			}
		};

		new GSEditor(this, FlexDirection.COLUMN).select_(StringExtractor.TYPE_INSTANCE_EXTRACTOR, model -> getProperty(ReactorStatics.SELECTION, model));
		new GSTable(this).select(StringExtractor.MANAGEMENT, Color.class);

		new GSTable(this).select(StringExtractor.MANAGEMENT, Engine.class);
		new GSMonitor(this).addStyle("background-color", "#ffa500");
	}

	public static class ExampleReactorScript implements Script {

		@Override
		public void run(Root engine) {
			Generic car = engine.find(Car.class);
			Generic power = engine.find(Power.class);
			Generic diesel = engine.find(Diesel.class);
			Generic person = engine.setInstance("Person");
			Generic category = engine.setInstance("Category");
			Generic carColor = engine.find(CarColor.class);
			Generic color = engine.find(Color.class);
			Generic carPerson = car.setRelation("CarDriverOwner", category, person);
			carPerson.enablePropertyConstraint();
			Generic red = color.setInstance("Red");
			Generic black = color.setInstance("Black");
			Generic green = color.setInstance("Green");
			color.setInstance("Blue");
			color.setInstance("Orange");
			color.setInstance("White");
			color.setInstance("Yellow");
			Generic jdoe = person.setInstance("John Doe");
			Generic hoover = person.setInstance("Edgar Hoover");
			Generic jsnow = person.setInstance("Jon Snow");
			Generic driver = category.setInstance("Driver");
			Generic owner = category.setInstance("Owner");
			Generic audiS4 = car.setInstance("Audi S4");
			audiS4.setHolder(power, 333);
			audiS4.setHolder(diesel, false);
			audiS4.setLink(carColor, "Audi S4 Green", green);
			audiS4.setLink(carPerson, "Audi S4 owner", owner, jsnow);
			audiS4.setLink(carPerson, "Audi S4 driver", driver, hoover);
			Generic bmwM3 = car.setInstance("BMW M3");
			bmwM3.setHolder(power, 450);
			bmwM3.setHolder(diesel, false);
			bmwM3.setLink(carColor, "BMW M3 Red", red);
			bmwM3.setLink(carPerson, "BMW M3 owner", owner, jdoe);
			bmwM3.setLink(carPerson, "BMW M3 owner", driver, jdoe);
			Generic ferrariF40 = car.setInstance("Ferrari F40");
			ferrariF40.setHolder(power, 478);
			ferrariF40.setHolder(diesel, false);
			ferrariF40.setLink(carColor, "Ferrari F40 red", red);
			Generic miniCooper = car.setInstance("Mini Cooper");
			miniCooper.setHolder(power, 175);
			miniCooper.setHolder(diesel, true);
			miniCooper.setLink(carColor, "Mini Cooper", black);
			car.setInstance("Audi A4 3.0 TDI").setHolder(power, 233);
			car.setInstance("Peugeot 106 GTI").setHolder(power, 120);
			car.setInstance("Peugeot 206 S16").setHolder(power, 136);
			power.enableRequiredConstraint(ApiStatics.BASE_POSITION);
			engine.getCurrentCache().flush();
		}
	}
}
