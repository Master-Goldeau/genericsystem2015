package org.genericsystem.cv.application;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.genericsystem.cv.Img;
import org.genericsystem.cv.application.GeneralInterpolator.OrientedPoint;
import org.genericsystem.cv.lm.LevenbergImpl;
import org.genericsystem.cv.utils.NativeLibraryLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;
import org.opencv.ximgproc.Ximgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RadonTransform {

	static {
		NativeLibraryLoader.load();
	}

	private static final Logger logger = LoggerFactory.getLogger(RadonTransform.class);

	public static Mat radonTransform(Mat src, int minAngle, int maxAngle) {
		Mat dst = Mat.zeros(src.rows(), src.rows(), CvType.CV_64FC1);
		int center = dst.rows() / 2;
		src.convertTo(new Mat(dst, new Rect(new Point(center - src.cols() / 2, 0), new Point(center + src.cols() / 2, src.rows()))), CvType.CV_64FC1);
		Mat radon = Mat.zeros(dst.rows(), -minAngle + maxAngle + 1, CvType.CV_64FC1);
		for (int t = minAngle; t <= maxAngle; t++) {
			Mat rotated = new Mat();
			Mat rotation = Imgproc.getRotationMatrix2D(new Point(center, center), t, 1);
			Imgproc.warpAffine(dst, rotated, rotation, new Size(dst.cols(), dst.rows()), Imgproc.INTER_NEAREST);
			Core.reduce(rotated, radon.col(t - minAngle), 1, Core.REDUCE_SUM);
			rotated.release();
			rotation.release();
		}
		dst.release();
		Core.normalize(radon, radon, 0, 255, Core.NORM_MINMAX);
		return radon;
	}

	public static Mat radonRemap(Mat radon, int minAngle) {
		Mat projectionMap = Mat.zeros(radon.rows(), radon.cols(), CvType.CV_64FC1);
		for (int k = 0; k < projectionMap.rows(); k++) {
			for (int tetha = 0; tetha < projectionMap.cols(); tetha++) {
				int p = (int) ((k - projectionMap.rows() / 2) * Math.sin(((double) tetha - minAngle) / 180 * Math.PI) + radon.rows() / 2);
				projectionMap.put(k, tetha, Math.max(projectionMap.get(k, tetha)[0], radon.get(p, tetha)[0]));
			}
		}
		return projectionMap;

		// Mat projectionMap = Mat.zeros(radon.rows(), radon.cols(), CvType.CV_32FC1);
		// Mat map_x = new Mat(radon.size(), CvType.CV_32FC1);
		// Mat map_y = new Mat(radon.size(), CvType.CV_32FC1);
		//
		// for (float col = 0; col < radon.cols(); col++)
		// for (int row = 0; row < radon.rows(); row++) {
		// // float newCol = (int) Math.round((Math.asin(((double) row - radon.rows() / 2) / (col - radon.rows() / 2)) / Math.PI * 180 + minAngle));
		// float newCol = (float) (((double) row - projectionMap.rows() / 2) * Math.sin((col - minAngle) / 180 * Math.PI) + radon.rows() / 2);
		// map_x.put(row, (int) col, newCol);
		// map_y.put(row, (int) col, row);
		// }
		// Imgproc.remap(radon, projectionMap, map_x, map_y, Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, new Scalar(0, 0, 0));
		// return projectionMap;

	}

	public static Mat fastHoughTransform(Mat vStrip) {
		Mat houghTransform = new Mat();
		Ximgproc.FastHoughTransform(vStrip, houghTransform, CvType.CV_64FC1, Ximgproc.ARO_45_135, Ximgproc.FHT_ADD, Ximgproc.HDO_DESKEW);
		Core.transpose(houghTransform, houghTransform);
		Core.normalize(houghTransform, houghTransform, 0, 255, Core.NORM_MINMAX);
		return new Mat(houghTransform, new Range(vStrip.width() / 2, houghTransform.height() - vStrip.width() / 2), new Range(0, houghTransform.width()));
	}

	public static Mat fhtRemap(Mat houghTransform) {
		Mat result = Mat.zeros(houghTransform.rows(), 91, CvType.CV_64FC1);
		// System.out.println(houghTransform);
		for (double col = 0; col < houghTransform.width(); col += 0.25)
			for (int row = 0; row < houghTransform.rows(); row++) {
				int stripSize = (houghTransform.width() + 1) / 2;
				double angle = Math.round(Math.atan((col - stripSize + 1) / (stripSize - 1)) / Math.PI * 180 + 45);
				if (angle < 0 || angle > 90)
					throw new IllegalStateException("Angle : " + angle);
				else
					result.put(row, (int) angle, Math.max(result.get(row, (int) Math.round(angle))[0], houghTransform.get(row, (int) col)[0]));
			}
		return result;
	}

	// public static Function<Double, Double> approxTraject(Mat houghTransform) {
	// List<double[]> values = new ArrayList<>();
	// for (int row = 0; row < houghTransform.rows(); row++) {
	// List<Double> houghLine = new ArrayList<>();
	// Converters.Mat_to_vector_double(houghTransform.row(row).t(), houghLine);
	// houghLine.add((double) row);
	// values.add(houghLine.toArray(new double[houghLine.size() - 1]));
	// }
	// BiFunction<Double, double[], Double> f = (x, params) -> params[0] + params[1] * x + params[2] * x * x + params[3] * x * x * x + params[4] * x * x * x * x + params[5] * x * x * x * x * x;
	// BiFunction<double[], double[], Double> error = (xy, params) -> {
	// double[] magnitudes = new double[houghTransform.rows()];
	// for (int row = 0; row < houghTransform.rows(); row++)
	// magnitudes[row] = params[(int) Math.round(f.apply((double) row, params))];
	//
	// double average = Arrays.stream(magnitudes).average().getAsDouble();
	// double variance = 0;
	//
	// return Math.sqrt(variance);
	// };
	//
	// double[] params = new LevenbergImpl<>(error, values, new double[] { 0, 0, 0, 0, 0, 0 }).getParams();
	// return x -> f.apply(x, params);
	// }

	public static TrajectStep[] bestTraject(Mat projectionMap, double anglePenality) {
		double[][] score = new double[projectionMap.rows()][projectionMap.cols()];
		int[][] thetaPrev = new int[projectionMap.rows()][projectionMap.cols()];
		for (int theta = 0; theta < projectionMap.cols(); theta++)
			score[0][theta] = projectionMap.get(0, theta)[0];
		for (int k = 1; k < projectionMap.rows(); k++) {
			for (int theta = 0; theta < projectionMap.cols(); theta++) {
				double magnitude = projectionMap.get(k, theta)[0];

				double scoreFromPrevTheta = theta != 0 ? score[k - 1][theta - 1] : Double.NEGATIVE_INFINITY;
				double scoreFromSameTheta = score[k - 1][theta];
				double scoreFromNextTheta = theta < projectionMap.cols() - 1 ? score[k - 1][theta + 1] : Double.NEGATIVE_INFINITY;

				double bestScore4Pos = -1;

				if (scoreFromSameTheta >= (scoreFromPrevTheta + anglePenality) && scoreFromSameTheta >= (scoreFromNextTheta + anglePenality)) {
					bestScore4Pos = scoreFromSameTheta;
					thetaPrev[k][theta] = theta;
				} else if ((scoreFromPrevTheta + anglePenality) >= scoreFromSameTheta && ((scoreFromPrevTheta + anglePenality) >= (scoreFromNextTheta + anglePenality))) {
					bestScore4Pos = scoreFromPrevTheta + anglePenality;
					thetaPrev[k][theta] = theta - 1;
				} else {
					bestScore4Pos = scoreFromNextTheta + anglePenality;
					thetaPrev[k][theta] = theta + 1;
				}
				score[k][theta] = magnitude + bestScore4Pos;
			}
		}

		// System.out.println(Arrays.toString(score[projectionMap.rows() - 1]));
		// System.out.println(Arrays.deepToString(thetaPrev));
		double maxScore = Double.NEGATIVE_INFINITY;

		int prevTheta = -1;

		for (int theta = 0; theta < projectionMap.cols(); theta++) {
			double lastScore = score[projectionMap.rows() - 1][theta];
			// System.out.println(lastScore);
			if (lastScore > maxScore) {
				maxScore = lastScore;
				prevTheta = theta;
			}
		}
		assert prevTheta != -1;
		// System.out.println(maxScore + " for theta : " + prevTheta);
		TrajectStep[] thetas = new TrajectStep[projectionMap.rows()];
		for (int k = projectionMap.rows() - 1; k >= 0; k--) {
			thetas[k] = new TrajectStep(k, prevTheta, projectionMap.get(k, prevTheta)[0]);
			// System.out.println(prevTheta);
			prevTheta = thetaPrev[k][prevTheta];
		}

		return thetas;
	}

	public static List<Mat> extractStrips(Mat src, int stripWidth) {
		List<Mat> strips = new ArrayList<>();
		for (int col = 0; col + stripWidth <= src.cols(); col += stripWidth / 2)
			strips.add(extractStrip(src, col, stripWidth));
		return strips;
	}

	public static Mat extractStrip(Mat src, int startX, int width) {
		return new Mat(src, new Range(0, src.rows()), new Range(startX, startX + width));
	}

	public static List<PolynomialSplineFunction> estimateBaselines(Mat image, double anglePenalty, int minAngle, int maxAngle, int yStep) {

		int n = 20;// Number of overlapping vertical strips.
		float r = .5f;// Overlap ratio between two consecutive strips.
		double w = (image.width() / (n * (1 - r) + r));// w = width of a vertical strip.
		double step = (int) ((1 - r) * w);// Image width = [n(1 - r) + r] w

		int x = 0;
		List<Function<Double, Double>> approxFunctions = new ArrayList<>();
		Mat preprocessed = new Img(image, false).adaptativeGaussianInvThreshold(5, 3).getSrc();
		for (int i = 0; i < n; i++) {
			Mat radonTransform = radonTransform(extractStrip(preprocessed, x, (int) w), minAngle, maxAngle);
			Mat projMap = radonRemap(radonTransform, minAngle);
			Imgproc.morphologyEx(projMap, projMap, Imgproc.MORPH_GRADIENT, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(2, 4)));
			TrajectStep[] angles = bestTraject(projMap, anglePenalty);
			projMap.release();
			radonTransform.release();
			approxFunctions.add(approxTraject(angles));
			x += step;
		}

		return toPolynomialSplineFunction(approxFunctions, image.size(), yStep, minAngle, n, r);
	}

	static List<PolynomialSplineFunction> toPolynomialSplineFunction(List<Function<Double, Double>> approxFunctions, Size imageSize, int yMeshStep, int minAngle, int vStripsNumber, float recover) {
		double w = (imageSize.width / (vStripsNumber * (1 - recover) + recover));
		double xStep = (int) ((1 - recover) * w);

		// 0, center of each vertical strip, image.width() - 1
		double[] xs = new double[vStripsNumber + 2];

		for (int i = 0; i < vStripsNumber; i++)
			xs[i + 1] = i * xStep + w / 2;

		xs[vStripsNumber + 1] = imageSize.width - 1;
		int lines = (int) ((imageSize.height - 1) / yMeshStep + 1);
		List<PolynomialSplineFunction> hLines = new ArrayList<>();
		for (int line = 0; line < lines; line++) {
			double[] ys = new double[vStripsNumber + 2];
			ys[vStripsNumber / 2] = line * yMeshStep + yMeshStep / 2;
			for (int j = vStripsNumber / 2; j <= vStripsNumber; j++) {
				double theta = (approxFunctions.get(j - 1).apply(ys[j]) + minAngle) / 180 * Math.PI;
				ys[j + 1] = ys[j] + xStep * Math.tan(theta);
			}
			for (int j = vStripsNumber / 2; j > 0; j--) {
				double theta = (approxFunctions.get(j - 1).apply(ys[j]) + minAngle) / 180 * Math.PI;
				ys[j - 1] = ys[j] - xStep * Math.tan(theta);
			}
			hLines.add(new LinearInterpolator().interpolate(xs, ys));
		}
		return hLines;
	}

	public static Function<Double, Double> approxTraject(TrajectStep[] traj) {
		List<double[]> values = new ArrayList<>();
		for (int k = 0; k < traj.length; k++)
			values.add(new double[] { k, traj[k].theta, traj[k].magnitude });
		double firstTheta = traj[0].theta;
		double k = traj.length - 1;
		double lastTheta = traj[traj.length - 1].theta;
		BiFunction<Double, double[], Double> f = (x, params) -> traj[0].theta + ((lastTheta - firstTheta - params[0] * k * k - params[1] * k * k * k - params[2] * k * k * k) / k) * x + params[0] * x * x + params[1] * x * x * x + params[2] * x * x * x * x;
		BiFunction<double[], double[], Double> error = (xy, params) -> (f.apply(xy[0], params) - xy[1]) * xy[2];
		double[] params = new LevenbergImpl<>(error, values, new double[] { 0, 0, 0 }).getParams();
		return x -> f.apply(x, params);
	}

	public static List<OrientedPoint> toHorizontalOrientedPoints(Function<Double, Double> f, double x, int height, int hStep) {
		List<OrientedPoint> orientedPoints = new ArrayList<>();
		for (int y = hStep; y + hStep <= height; y += hStep) {
			double angle = (f.apply((double) y) - 45) / 180 * Math.PI;
			orientedPoints.add(new OrientedPoint(new Point(x, y), angle, 1));
		}
		return orientedPoints;
	}

	public static List<OrientedPoint> toVerticalOrientedPoints(Function<Double, Double> f, double y, int width, int vStep) {
		List<OrientedPoint> orientedPoints = new ArrayList<>();
		for (int x = vStep; x + vStep <= width; x += vStep) {
			double angle = -(f.apply((double) x) - 45) / 180 * Math.PI;
			orientedPoints.add(new OrientedPoint(new Point(x, y), angle, 1));
		}
		return orientedPoints;
	}

	// public static List<OrientedPoint> toHorizontalFHTOrientedPoints(Function<Double, Double> f, int vStrip, int stripWidth, int height, int step, int minAngle) {
	// List<OrientedPoint> orientedPoints = new ArrayList<>();
	// for (int k = 0; k <= height; k += step) {
	// double angle = ((f.apply((double) k) / (2 * stripWidth - 1) * 90) - minAngle) / 180 * Math.PI;
	// orientedPoints.add(new OrientedPoint(new Point((vStrip + 1) * stripWidth / 2, k), angle, 1));
	// }
	// return orientedPoints;
	// }
	//
	// public static List<OrientedPoint> toVerticalFHTOrientedPoints(Function<Double, Double> f, int hStrip, int stripHeight, int width, int step, int minAngle) {
	// List<OrientedPoint> orientedPoints = new ArrayList<>();
	// for (int k = 0; k <= width; k += step) {
	// double angle = (90 + minAngle - (f.apply((double) k) / (2 * stripHeight - 1) * 90)) / 180 * Math.PI;
	// orientedPoints.add(new OrientedPoint(new Point(k, (hStrip + 1) * stripHeight / 2), angle, 1));
	// }
	// return orientedPoints;
	// }

	private static boolean inImage(Point p, Mat img) {
		return p.x >= 0 && p.y >= 0 && p.x < img.width() && p.y < img.height();
	}

	public static void displayHSplines(List<PolynomialSplineFunction> vRadonSplinesFunctions, Mat image) {
		for (int col = 0; col < image.width(); col++)
			for (PolynomialSplineFunction f : vRadonSplinesFunctions) {
				int row = (int) f.value(col);
				if (row >= 0 && row < image.rows())
					image.put(row, col, 0d, 255d, 0d);
			}
	}

	public static void displayVSplines(List<PolynomialSplineFunction> hRadonSplinesFunctions, Mat image) {
		for (int row = 0; row < image.height(); row++)
			for (PolynomialSplineFunction f : hRadonSplinesFunctions) {
				int col = (int) f.value(row);
				if (col >= 0 && col < image.cols())
					image.put(row, col, 0d, 255d, 0d);
			}
	}

	public static List<Pair> getLocalExtr(Mat fht, double minWeight) {
		List<Pair> weightedPoints = new ArrayList<>();
		for (int y = 0; y < fht.rows(); ++y) {
			List<Double> pLine = new ArrayList<>();
			Converters.Mat_to_vector_double(fht.row(Math.max(y - 1, 0)).t(), pLine);
			List<Double> cLine = new ArrayList<>();
			Converters.Mat_to_vector_double(fht.row(Math.max(y, 0)).t(), cLine);
			List<Double> nLine = new ArrayList<>();
			Converters.Mat_to_vector_double(fht.row(Math.min(y + 1, fht.rows() - 1)).t(), nLine);
			for (int x = 0; x < fht.cols(); ++x) {
				double value = cLine.get(x);
				if (value >= minWeight) {
					int[] isLocalMax = new int[] { 0 };
					for (int xx = Math.max(x - 2, 0); xx <= Math.min(x + 2, fht.cols() - 1); ++xx) {
						if (!incIfGreater(value, pLine.get(xx), isLocalMax) || !incIfGreater(value, cLine.get(xx), isLocalMax) || !incIfGreater(value, nLine.get(xx), isLocalMax)) {
							isLocalMax[0] = 0;
							break;
						}
					}
					if (isLocalMax[0] > 0)
						weightedPoints.add(new Pair(value, new Point(x, y)));
				}
			}
		}
		return weightedPoints;
	}

	private static boolean incIfGreater(double a, double b, int[] value) {
		if (a < b)
			return false;
		if (a > b)
			++value[0];
		return true;
	}

	static class Pair {

		double value;
		Point point;

		private Pair(double value, Point point) {
			this.value = value;
			this.point = point;
		}
	}

}
