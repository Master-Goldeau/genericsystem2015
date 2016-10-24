package org.genericsystem.security;

import org.genericsystem.common.Root;
import org.genericsystem.reactor.annotations.Children;
import org.genericsystem.reactor.annotations.DependsOnModel;
import org.genericsystem.reactor.annotations.DirectSelect;
import org.genericsystem.reactor.annotations.RunScript;
import org.genericsystem.reactor.annotations.SetText;
import org.genericsystem.reactor.annotations.Style;
import org.genericsystem.reactor.appserver.ApplicationServer;
import org.genericsystem.reactor.appserver.Script;
import org.genericsystem.reactor.gscomponents.GSApp;
import org.genericsystem.reactor.gscomponents3.AppHeader;
import org.genericsystem.reactor.gscomponents3.AppHeader.TitleDiv;
import org.genericsystem.reactor.gscomponents3.DivWithTitle.TitledInstancesTable;
import org.genericsystem.reactor.gscomponents3.Modal.ModalEditor;
import org.genericsystem.reactor.gscomponents3.Monitor;
import org.genericsystem.reactor.gscomponents3.Responsive;
import org.genericsystem.reactor.htmltag.HtmlH1;
import org.genericsystem.security.SecurityApp.ExampleScript;
import org.genericsystem.security.model.Role;
import org.genericsystem.security.model.User;

/**
 * @author Nicolas Feybesse
 *
 */

@RunScript(ExampleScript.class)
@DependsOnModel({ User.class })
@Style(name = "background-color", value = "Red")
@Children({ ModalEditor.class, AppHeader.class, Responsive.class, Monitor.class })
@Children(path = Responsive.class, value = { TitledInstancesTable.class, TitledInstancesTable.class })
@SetText(path = { AppHeader.class, TitleDiv.class, HtmlH1.class }, value = "gs-security")
@DirectSelect(path = { Responsive.class, TitledInstancesTable.class }, value = { User.class, Role.class })
public class SecurityApp extends GSApp {

	public static void main(String[] mainArgs) {
		ApplicationServer.startSimpleGenericApp(mainArgs, SecurityApp.class, "/security");
	}

	public static class ExampleScript implements Script {

		@Override
		public void run(Root engine) {
			// Generic role = engine.find(Role.class);
			engine.getCurrentCache().flush();
		}
	}
}