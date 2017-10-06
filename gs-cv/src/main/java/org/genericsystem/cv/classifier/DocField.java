package org.genericsystem.cv.classifier;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.genericsystem.cv.Img;
import org.genericsystem.cv.Ocr;
import org.genericsystem.cv.utils.ModelTools;
import org.genericsystem.cv.utils.OCRPlasty;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class DocField {
	private final Rect rect;
	private final int num;
	private final String uid;

	private Map<String, Integer> labels = new HashMap<>();
	private Optional<String> consolidated;

	public DocField(int num, Rect rect) {
		this.rect = rect;
		this.num = num;
		this.uid = ModelTools.generateZoneUID(rect);
	}

	public Map<String, Integer> getLabels() {
		return labels;
	}

	public boolean contains(Point center) {
		Point localCenter = center();
		return Math.sqrt(Math.pow(localCenter.x - center.x, 2) + Math.pow(localCenter.y - center.y, 2)) <= 10;
	}

	public Point center() {
		return new Point(rect.x + rect.width / 2, rect.y + rect.height / 2);
	}

	public void ocr(final Img rootImg) {
		Mat roi = new Mat(rootImg.getSrc(), getLargeRect(rootImg, 0.03, 0.1));
		String ocr = Ocr.doWork(roi);
		Integer count = labels.get(ocr);
		labels.put(ocr, 1 + (count != null ? count : 0));
		roi.release();
	}

	public void consolidateOcr() {
		if (!labels.isEmpty()) {
			List<String> ocrs = labels.keySet().stream().collect(Collectors.toList());
			consolidated = Optional.of(OCRPlasty.ocrPlasty(ocrs));
		} else
			consolidated = Optional.empty();
	}

	public String doOcr(final Img rootImg) {
		Mat roi = new Mat(rootImg.getSrc(), getLargeRect(rootImg, 0.03, 0.1));
		String ocr = Ocr.doWork(roi);
		roi.release();
		return ocr;
	}

	public Rect getLargeRect(final Img imgRoot, final double deltaW, final double deltaH) {
		int adjustW = 3 + Double.valueOf(Math.floor(rect.width * deltaW)).intValue();
		int adjustH = 3 + Double.valueOf(Math.floor(rect.height * deltaH)).intValue();

		Point tl = new Point(rect.tl().x - adjustW > 0 ? rect.tl().x - adjustW : 0, rect.tl().y - adjustH > 0 ? rect.tl().y - adjustH : 0);
		Point br = new Point(rect.br().x + adjustW > imgRoot.width() ? imgRoot.width() : rect.br().x + adjustW, rect.br().y + adjustH > imgRoot.height() ? imgRoot.height() : rect.br().y + adjustH);

		return new Rect(tl, br);
	}

	public boolean isConsolidated() {
		return consolidated != null;
	}

	public Optional<String> getConsolidated() {
		return consolidated;
	}

	public Rect getRect() {
		return rect;
	}

	public int getNum() {
		return num;
	}

	public String getUid() {
		return uid;
	}

	public boolean needOcr() {
		// TODO: add some logic
		return consolidated == null;
	}

	public void drawOcrPerspectiveInverse(Img display, Scalar color, int thickness) {
		MatOfPoint2f results = new MatOfPoint2f(center(), new Point(rect.x, rect.y), new Point(rect.x + rect.width - 1, rect.y), new Point(rect.x + rect.width - 1, rect.y + rect.height - 1), new Point(rect.x, rect.y + rect.height - 1));
		Point[] targets = results.toArray();
		Imgproc.line(display.getSrc(), targets[1], targets[2], color, thickness);
		Imgproc.line(display.getSrc(), targets[2], targets[3], color, thickness);
		Imgproc.line(display.getSrc(), targets[3], targets[4], color, thickness);
		Imgproc.line(display.getSrc(), targets[4], targets[1], color, thickness);
		Point topCenter = new Point((targets[1].x + targets[2].x) / 2, (targets[1].y + targets[2].y) / 2);
		double l = Math.sqrt(Math.pow(targets[1].x - topCenter.x, 2) + Math.pow(targets[1].y - topCenter.y, 2));
		Imgproc.line(display.getSrc(), new Point(topCenter.x, topCenter.y - 2), new Point(topCenter.x, topCenter.y - 20), new Scalar(0, 255, 0), 1);
		Imgproc.putText(display.getSrc(), Normalizer.normalize(consolidated.orElse(""), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", ""), new Point(topCenter.x - l, topCenter.y - 22), Core.FONT_HERSHEY_TRIPLEX, 1.0, color, thickness);
		results.release();
	}

	public void draw(Img img) {
		Imgproc.rectangle(img.getSrc(), rect.tl(), rect.br(), new Scalar(0, 0, 255));
	}

	@Override
	public String toString() {
		return "DocField [rect=" + rect + ", num=" + num + ", consolidated=" + consolidated + "]";
	}

}