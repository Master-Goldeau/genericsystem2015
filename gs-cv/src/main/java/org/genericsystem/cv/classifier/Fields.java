package org.genericsystem.cv.classifier;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.genericsystem.cv.Img;
import org.genericsystem.cv.utils.RectangleTools;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.utils.Converters;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class Fields extends AbstractFields {

	private Mat lastHomography;
	private Mat lastRotation;

	public void scanNewRects(Img display, List<Rect> newRects) {
		if (!fields.isEmpty())
			oldFields = fields;
		fields = newRects.stream().map(Field::new).collect(Collectors.toList());
		randomOcrStream().forEach(f -> ((Field) f).ocr(display));
	}

	public void merge() {
		if (lastHomography != null) {
			oldFields = (List) restabilizeFields();

			System.out.println("oldFields transformed (" + oldFields.size() + ")");

			for (int i = 0; i < fields.size(); i++) {
				Field currentField = (Field) fields.get(i);
				if (true || currentField.isConsolidated()) {
					// String currentText = currentField.getConsolidated().get();
					List<Field> matches = (List) findMatchingFieldsWithConfidence(currentField, 0.6); // Compare with fields in oldFields
					System.out.println("matches.size() = " + matches.size());

					if (!matches.isEmpty()) {
						// matches.removeIf(f -> {
						// // Remove the element from the list of matches if it was not consolidated, or if the text was not similar enough
						// Optional<String> optional = f.getConsolidated();
						// return optional.map(s -> !s.equals(currentText) && StringCompare.compare(s, currentText, SIMILARITY.COSINE_CHAR) <= MIN_SIMILARITY).orElse(true);
						// });
						System.out.println("Merge : " + currentField.getConsolidated());
						matches.forEach(f -> currentField.merge(f));
						boolean ok = oldFields.removeAll(matches);
						System.out.println("ok: " + ok);
					} else {
						System.out.println("No matches found");
					}
				} else {
					System.out.print(" . ");
				}
			}
			// At this stage, add all the remaining fields still in oldFields
			fields.addAll(oldFields);
		}

		// List<Field> oldFields = (List) fields;
		// fields = rects.stream().map(Field::new).collect(Collectors.toList());
		//
		// if (lastHomography != null) {
		// List<Field> virtualField = getVirtualFields(oldFields);
		//
		// for (int index = 0; index < virtualField.size(); index++) {
		// if (virtualField.get(index).isConsolidated()) {
		// Field virtualOldField = virtualField.get(index);
		// List<Field> matches = (List) findMatchingFieldsWithConfidence(virtualOldField, 0.95);
		//
		// if (!matches.isEmpty()) {
		// // TODO: if multiple matches are found, it might be better to remove the extra fields (NMS?)
		// System.out.println("Merge : " + virtualOldField.getConsolidated());
		// matches.forEach(f -> f.merge(virtualOldField));
		// } else {
		// System.out.println("No exact matches found");
		// List<Field> containings = (List) findContainingFields(virtualOldField);
		// if (!containings.isEmpty()) {
		// // Check whether virtualOldField's text is found in the containing fields
		// System.out.println("Found new fields containing the virtual old field");
		// } else {
		// System.out.println("No containing fields were found");
		// List<Field> containeds = (List) findContainedFields(virtualOldField);
		// if (!containeds.isEmpty()) {
		// System.out.println("Found new fields contained in the virtual old field");
		// } else {
		// System.out.println("No contained fields were found");
		// }
		// fields.add(virtualOldField);
		// }
		// }
		// }
		// }
		// }
	}

	@Override
	protected Field getIntersection(AbstractField field1, AbstractField field2) {
		Rect rect1 = field1.getRect();
		Rect rect2 = field2.getRect();
		Rect intersect = RectangleTools.getIntersection(rect1, rect2).orElseThrow(() -> new IllegalArgumentException("No intersecting rectangle was found"));
		Field intersection = new Field(intersect);
		intersection.merge(Arrays.asList(field1, field2));
		return intersection;
	}

	@Override
	protected Field getUnion(AbstractField field1, AbstractField field2) {
		Rect rect1 = field1.getRect();
		Rect rect2 = field2.getRect();
		Field union = new Field(RectangleTools.getUnion(rect1, rect2));
		union.merge(Arrays.asList(field1, field2));
		return union;
	}

	private List<Field> restabilizeFields() {
		// Apply the homography + rotation to the oldFields
		List<Rect> virtualRects = oldFields.stream().map(AbstractField::getRect).map(rect -> findNewRect(rect)).collect(Collectors.toList());
		return IntStream.range(0, oldFields.size()).mapToObj(i -> {
			Field f = new Field(virtualRects.get(i));
			f.merge(oldFields.get(i));
			return f;
		}).collect(Collectors.toList());
	}

	private Rect findNewRect(Rect rect) {
		List<Point> points = restabilize(Arrays.asList(rect.tl(), rect.br()));
		return new Rect(points.get(0), points.get(1));
	}

	private List<Point> restabilize(List<Point> originals) {
		Mat original = Converters.vector_Point2f_to_Mat(originals);
		MatOfPoint2f results = new MatOfPoint2f();
		Core.perspectiveTransform(original, results, lastHomography);
		MatOfPoint2f rotated = new MatOfPoint2f();
		Core.transform(results, rotated, lastRotation);
		List<Point> res = rotated.toList();
		original.release();
		results.release();
		rotated.release();
		return res;
	}

	public void storeLastHomography(Mat homography) {
		this.lastHomography = homography;
	}

	public void storeLastRotation(Mat rotation) {
		this.lastRotation = rotation;
	}

	@Override
	public void consolidateOcr(Img rootImg) {
		long TS = System.currentTimeMillis();
		// XXX Replace needOCR with a random index
		stream().filter(AbstractField::needOcr).filter(f -> System.currentTimeMillis() - TS <= 200).forEach(f -> f.ocr(rootImg));
	}

}