package org.genericsystem.spring;

import java.util.HashSet;
import java.util.Set;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

//@ApplicationScoped
@Component
@Lazy
public class UserClassesProvider {

	Set<Class<?>> userClasses = new HashSet<>();

	public void addUserClasse(Class<?> userClasse) {
		userClasses.add(userClasse);
	}

	public Class<?>[] getUserClassesArray() {
		return userClasses.toArray(new Class[userClasses.size()]);
	}
}