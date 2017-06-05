package org.genericsystem.cv;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class ClassImgBoard extends VBox {
	private final Img model;
	private final ToggleGroup group = new ToggleGroup();
	private final ObservableValue<Img> average;
	private final ObservableValue<Img> variance;

	private final ObservableValue<Img> imgToZone;
	private final ObservableValue<Img> zonedImg;
	private final Runnable action;
	private Zones zones;

	public ClassImgBoard(ImgClass2 imgClass) {
		average = imgClass.getObservableMean();
		model = average.getValue();
		variance = imgClass.getObservableVariance();

		RadioButton averageRadio = new RadioButton("Average");
		averageRadio.setToggleGroup(group);
		averageRadio.setUserData(imgClass.getObservableMean());

		RadioButton varianceRadio = new RadioButton("Variance");
		varianceRadio.setToggleGroup(group);
		varianceRadio.setUserData(imgClass.getObservableVariance());
		varianceRadio.setSelected(true);

		imgToZone = Bindings.createObjectBinding(() -> {
			ObservableValue<Img> o = ((ObservableValue<Img>) group.getSelectedToggle().getUserData());
			return o != null ? o.getValue() : null;
		}, group.selectedToggleProperty(), average, variance);
		zonedImg = Bindings.createObjectBinding(() -> {
			Img zonedMean = new Img(model.getSrc());
			if (imgToZone.getValue() != null) {
				zones = Zones.get(imgToZone.getValue().morphologyEx(Imgproc.MORPH_CLOSE, new StructuringElement(Imgproc.MORPH_RECT, new Size(9, 10))), 300.0, 6.0, 6.0);
				zones.draw(zonedMean, new Scalar(0, 255, 0), 3);
				zones.writeNum(zonedMean, new Scalar(0, 0, 255), 3);
				return zonedMean;
			}
			return zonedMean;
		}, imgToZone);

		VBox vBox = new VBox();
		setPadding(new Insets(40, 40, 40, 40));

		HBox zoneBase = new HBox();
		zoneBase.getChildren().add(averageRadio);
		zoneBase.getChildren().add(varianceRadio);
		zoneBase.setSpacing(15);
		vBox.getChildren().add(zoneBase);
		List<LabelledSpinner> spinners = Arrays.asList(new LabelledSpinner("min hue", 0, 255, 0), new LabelledSpinner("min saturation", 0, 255, 0), new LabelledSpinner("min value", 0, 255, 0), new LabelledSpinner("max hue", 0, 255, 255),
				new LabelledSpinner("max saturation", 0, 255, 255), new LabelledSpinner("max value", 0, 255, 86), new LabelledSpinner("min blue", 0, 255, 0), new LabelledSpinner("min green", 0, 255, 0), new LabelledSpinner("min red", 0, 255, 0),
				new LabelledSpinner("max blue", 0, 255, 76), new LabelledSpinner("max green", 0, 255, 255), new LabelledSpinner("max red", 0, 255, 255), new LabelledSpinner("horizontal dilatation", 1, 60, 20), new LabelledSpinner("vertical dilatation", 1,
						30, 3));
		action = () -> imgClass.setPreprocessor(img -> img.eraseCorners(0.1)
				.range(new Scalar(spinners.get(0).getValue(), spinners.get(1).getValue(), spinners.get(2).getValue()), new Scalar(spinners.get(3).getValue(), spinners.get(4).getValue(), spinners.get(5).getValue()), true)
				.range(new Scalar(spinners.get(6).getValue(), spinners.get(7).getValue(), spinners.get(8).getValue()), new Scalar(spinners.get(9).getValue(), spinners.get(10).getValue(), spinners.get(11).getValue()), false)
				.morphologyEx(Imgproc.MORPH_DILATE, new StructuringElement(Imgproc.MORPH_RECT, new Size(spinners.get(12).getValue(), spinners.get(13).getValue()))));
		spinners.forEach(spinner -> spinner.setListener(action));
		spinners.forEach(vBox.getChildren()::add);
		AwareImageView zonesImageView = new AwareImageView(zonedImg);
		getChildren().add(zonesImageView);
		getChildren().add(vBox);
		Button saveButton = new Button("Save");
		saveButton.setOnAction((e) -> zones.save(new File(imgClass.getDirectory() + "/zones/zones.json")));
		vBox.getChildren().add(saveButton);
		action.run();
	}

	public static class LabelledSpinner extends VBox {
		private final Label label = new Label();
		private final Slider slider = new Slider();

		public LabelledSpinner(String name, double min, double max, double value) {
			slider.setMin(min);
			slider.setMax(max);
			slider.setValue(value);
			label.textProperty().bind(Bindings.createStringBinding(() -> name + " : " + slider.valueProperty().intValue(), slider.valueProperty()));
			getChildren().add(label);
			getChildren().add(slider);
		}

		public double getValue() {
			return slider.getValue();
		}

		public void setListener(Runnable action) {
			slider.setOnMouseReleased((e) -> action.run());
			slider.setOnKeyReleased((e) -> action.run());
		}
	}

}