package org.genericsystem.cv;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.genericsystem.cv.utils.Tools;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class ImgClass implements AutoCloseable {

	private final static String TEMPLATE_NAME = "template/template.png";
	private Img classModel;
	private Img mean;
	private Img variance;
	private final String directory;
	private final List<Function<Img, Img>> mappers = new ArrayList<>();

	public static ImgClass fromDirectory(String bgrDirectory) {
		return new ImgClass(bgrDirectory);
	}

	private Img applyMappers(Img img) {
		for (Function<Img, Img> mapper : mappers)
			img = mapper.apply(img);
		return img;
	}

	public ImgClass(String bgrDirectory) {
		// TODO: Remove renaming code.
		File oldTemplate = Paths.get(bgrDirectory).resolve("template.png").toFile();
		Path template = Paths.get(bgrDirectory, TEMPLATE_NAME.split(File.separator));
		template.getParent().toFile().mkdirs();
		if (oldTemplate.exists())
			oldTemplate.renameTo(template.toFile());
		if (classModel == null && template.toFile().exists())
			this.classModel = new Img(template.toString());
		this.directory = bgrDirectory;
	}

	public Stream<Img> classImgsStream() {
		return Tools.classImgsStream(directory);
	}

	private synchronized void computeMeanVariance() {
		Iterator<Img> it_ = classImgsStream().iterator();
		if (it_.hasNext()) {
			Img img0 = applyMappers(it_.next());
			boolean gray = img0.channels() == 1;
			int type = gray ? CvType.CV_32S : CvType.CV_32SC3;

			Mat mean = new Mat(img0.size(), type, Scalar.all(0));
			Mat m2 = new Mat(img0.size(), type, Scalar.all(0));
			Mat mask = Mat.ones(img0.size(), CvType.CV_8U);
			img0.close();
			int count = 1;

			Iterator<Img> it = classImgsStream().iterator();
			while (it.hasNext()) {
				Mat img = new Mat();
				applyMappers(it.next()).getSrc().convertTo(img, type);
				Mat delta = new Mat(img.size(), type);
				Core.subtract(img, mean, delta, mask, type);
				Core.addWeighted(mean, 1, delta, 1d / count, 0, mean, type);
				Mat delta2 = new Mat(m2.size(), type);
				Core.subtract(img, mean, delta2, mask, type);
				Mat product = delta.mul(delta2);
				Core.add(m2, product, m2);
				count++;
				img.release();
				delta.release();
				delta2.release();
				product.release();
			}
			Mat variance = new Mat(m2.size(), type);
			Core.multiply(m2, new Scalar(1d / count, 1d / count, 1d / count), variance);
			variance.convertTo(variance, CvType.CV_8U);
			mean.convertTo(mean, CvType.CV_8U);
			m2.release();
			mask.release();
			this.mean = new Img(mean, false);
			this.variance = new Img(variance, false);
		}
	}

	public void addMapper(Function<Img, Img> after) {
		mappers.add(after);
		computeMeanVariance();
	}

	public Img getClassModel() {
		return classModel;
	}

	public Img getMean() {
		if (mean == null)
			computeMeanVariance();
		return mean;
	}

	public Img getVariance() {
		if (variance == null)
			computeMeanVariance();
		return variance;
	}

	public String getDirectory() {
		return directory;
	}

	public Img getClosedMeanZones(Size morphClose) {
		return mean.morphologyEx(Imgproc.MORPH_CLOSE, Imgproc.MORPH_RECT, morphClose);
	}

	public Img getClosedVarianceZones(Size morphClose) {
		return variance.morphologyEx(Imgproc.MORPH_CLOSE, Imgproc.MORPH_RECT, morphClose);
	}

	@Override
	public void close() {
		if (classModel != null)
			classModel.close();
		if (mean != null)
			mean.close();
		if (variance != null)
			variance.close();
	}
}
