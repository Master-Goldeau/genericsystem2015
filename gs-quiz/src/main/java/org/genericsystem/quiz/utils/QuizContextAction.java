package org.genericsystem.quiz.utils;

import org.genericsystem.common.Generic;
import org.genericsystem.quiz.app.pages.components.QuestionDiv.FooterDiv.FinishBtn;
import org.genericsystem.quiz.app.pages.components.QuestionDiv.FooterDiv.NextBtn;
import org.genericsystem.quiz.app.pages.components.QuestionDiv.FooterDiv.PreviousBtn;
import org.genericsystem.reactor.Context;
import org.genericsystem.reactor.Tag;
import org.genericsystem.reactor.context.ContextAction;
import org.genericsystem.security.model.User;

import javafx.beans.property.Property;

public class QuizContextAction {

	public static class NEXT_TAG implements ContextAction {
		@Override
		public void accept(Context context, Tag tagNext) {
			if (QuizStepper.class.isAssignableFrom(tagNext.getClass())) {

				Tag tagParent = tagNext.getParent();
				Tag tagPrevious = tagParent.find(PreviousBtn.class);
				Tag tagFinish = tagParent.find(FinishBtn.class);

				((QuizStepper) tagNext).next(context, tagNext, tagPrevious, tagFinish);

			} else
				log.warn("The NEXT action is applicable only to a tag implementing StepperDefaults.");
		}
	}

	public static class PREVIOUS_TAG implements ContextAction {
		@Override
		public void accept(Context context, Tag tagPrevious) {
			if (QuizStepper.class.isAssignableFrom(tagPrevious.getClass())) {

				Tag tagParent = tagPrevious.getParent();
				Tag tagNext = tagParent.find(NextBtn.class);
				Tag tagFinish = tagParent.find(FinishBtn.class);

				((QuizStepper) tagPrevious).prev(context, tagNext, tagPrevious, tagFinish);

			} else
				log.warn("The PREVIOUS action is applicable only to a tag implementing StepperDefaults.");
		}
	}

	public static class SAVE_QUIZ_RESULT implements ContextAction {

		@Override
		public void accept(Context context, Tag tag) {

			Generic quiz = context.getGeneric();
			Generic sUser = context.find(User.class).getInstance("Anti-Seche");
			Generic loggedUser = tag.getLoggedUserProperty(context).getValue();

			ScoreUtils.setResult(context, quiz, sUser, loggedUser);
			ScoreUtils.getResult(context, quiz, loggedUser);
		}
	}

	// NAVIGATION ENTRE LES PAGES

	public static class CLEAR_PAGES implements ContextAction {

		@Override
		public void accept(Context context, Tag tag) {
			if (tag.getProperty(QuizTagSwitcher.PAGE, context) != null)
				tag.getProperty(QuizTagSwitcher.PAGE, context).setValue(null);
		}

	}

	// Possibilité de factoriser
	public static class CALL_HOME_PAGE implements ContextAction {

		@Override
		public void accept(Context context, Tag tag) {
			Property<String> pageProperty = tag.getProperty(QuizTagSwitcher.PAGE, context);
			if (pageProperty == null)
				tag.createNewInitializedProperty(QuizTagSwitcher.PAGE, context, c -> QuizTagSwitcher.HOME_PAGE);
			else
				pageProperty.setValue(QuizTagSwitcher.HOME_PAGE);
		}
	}

	public static class CALL_RESULT_PAGE implements ContextAction {

		@Override
		public void accept(Context context, Tag tag) {
			Property<String> pageProperty = tag.getProperty(QuizTagSwitcher.PAGE, context);
			if (pageProperty == null)
				tag.createNewInitializedProperty(QuizTagSwitcher.PAGE, context, c -> QuizTagSwitcher.RESULT_PAGE);
			else
				pageProperty.setValue(QuizTagSwitcher.RESULT_PAGE);
		}
	}

	public static class CALL_QUESTION_PAGE implements ContextAction {

		@Override
		public void accept(Context context, Tag tag) {
			Property<String> pageProperty = tag.getProperty(QuizTagSwitcher.PAGE, context);
			if (pageProperty == null)
				tag.createNewInitializedProperty(QuizTagSwitcher.PAGE, context, c -> QuizTagSwitcher.QUESTION_PAGE);
			else
				pageProperty.setValue(QuizTagSwitcher.QUESTION_PAGE);
		}

	}
}
