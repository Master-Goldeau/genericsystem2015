package org.genericsystem.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.genericsystem.cv.Img;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.utils.Converters;

public class Fields {
	private List<Field> fields = new ArrayList<>();
	private Mat lastHomography;
	private Mat lastRotation;

	public void merge(Stream<MatOfPoint> contours) {
		List<Field> oldFields = fields;
		fields = contours.map(Field::new).collect(Collectors.toList());
		if (lastHomography != null) {
			List<Point> newPoints = restabilize(oldFields.stream().map(f -> f.center()).collect(Collectors.toList()));
			for (int index = 0; index < oldFields.size(); index++)
				if (oldFields.get(index).isConsolidated()) {
					Field field = findNewField(newPoints.get(index));
					if (field != null) {
						field.merge(oldFields.get(index));
						System.out.println("Merge : " + oldFields.get(index).getConsolidated());
						System.out.println(newPoints.get(index) + " " + field.center());
					} else
						System.out.println("Can 't merge : " + oldFields.get(index).getConsolidated());
				}
		}
	}

	private Field findNewField(Point pt) {
		for (Field field : fields) {
			if (field.contains(pt))
				return field;
		}
		return null;
	}

	private List<Point> restabilize(List<Point> originals) {
		MatOfPoint2f results = new MatOfPoint2f();
		Core.perspectiveTransform(Converters.vector_Point2f_to_Mat(originals), results, lastHomography);
		MatOfPoint2f rotated = new MatOfPoint2f();
		Core.transform(results, rotated, lastRotation);
		return rotated.toList();
	}

	public void storeLastHomography(Mat homography) {
		this.lastHomography = homography;
	}

	public void storeLastRotation(Mat rotation) {
		this.lastRotation = rotation;
	}

	public void drawOcrPerspectiveInverse(Img display, Mat homography, Scalar color, int thickness) {
		consolidatedFieldStream().forEach(field -> field.drawOcrPerspectiveInverse(display, homography, color, thickness));
	}

	public void drawConsolidated(Img stabilizedDisplay) {
		consolidatedFieldStream().forEach(field -> field.draw(stabilizedDisplay));

	}

	public void consolidateOcr(Img rootImg) {
		fields.stream().filter(Field::needOcr).filter(f -> Math.random() < 1).forEach(f -> f.ocr(rootImg));
	}

	public Stream<Field> consolidatedFieldStream() {
		return fields.stream().filter(f -> f.isConsolidated());
	}
}