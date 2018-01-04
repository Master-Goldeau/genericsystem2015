package org.genericsystem.cv.retriever;

import java.util.ArrayList;
import java.util.List;

import org.genericsystem.cv.Img;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.imgproc.Imgproc;

public class ImgDescriptor {
	private static final DescriptorExtractor EXTRACTOR = DescriptorExtractor.create(DescriptorExtractor.ORB);
	private static final DescriptorMatcher MATCHER = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
	private final Img deperspectivedImg;
	private final MatOfKeyPoint keypoints;
	private final Mat descriptors;
	private final Mat homography;

	public ImgDescriptor(Mat frame, Mat deperspectivGraphy) {
		deperspectivedImg = CamLiveRetriever.warpPerspective(frame, deperspectivGraphy);
		keypoints = detect(deperspectivedImg);
		assert keypoints != null && !keypoints.empty();
		descriptors = new Mat();
		EXTRACTOR.compute(deperspectivedImg.getSrc(), keypoints, descriptors);
		this.homography = deperspectivGraphy;

	}

	public Img getDeperspectivedImg() {
		return deperspectivedImg;
	}

	public MatOfKeyPoint getKeypoints() {
		return keypoints;
	}

	public Mat getDescriptors() {
		return descriptors;
	}

	public Mat getHomography() {
		return homography;
	}

	private static MatOfKeyPoint detect(Img frame) {
		Img closed = frame.bilateralFilter(5, 80, 80).adaptativeGaussianInvThreshold(17, 3).morphologyEx(Imgproc.MORPH_CLOSE, Imgproc.MORPH_ELLIPSE, new Size(5, 5));
		List<MatOfPoint> contours = new ArrayList<>();
		Imgproc.findContours(closed.getSrc(), contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
		double minArea = 100;
		List<KeyPoint> keyPoints = new ArrayList<>();
		contours.stream().filter(contour -> Imgproc.contourArea(contour) > minArea).map(Imgproc::boundingRect).forEach(rect -> {
			keyPoints.add(new KeyPoint((float) rect.tl().x, (float) rect.tl().y, 6));
			keyPoints.add(new KeyPoint((float) rect.tl().x, (float) rect.br().y, 6));
			keyPoints.add(new KeyPoint((float) rect.br().x, (float) rect.tl().y, 6));
			keyPoints.add(new KeyPoint((float) rect.br().x, (float) rect.br().y, 6));
		});
		return new MatOfKeyPoint(keyPoints.stream().toArray(KeyPoint[]::new));
	}

	public Mat computeStabilizationGraphy(ImgDescriptor frameDescriptor) {
		MatOfDMatch matches = new MatOfDMatch();
		System.out.println(frameDescriptor.getDescriptors());
		MATCHER.match(getDescriptors(), frameDescriptor.getDescriptors(), matches);
		List<DMatch> goodMatches = new ArrayList<>();
		for (DMatch dMatch : matches.toArray()) {
			if (dMatch.distance <= 30) {
				goodMatches.add(dMatch);
			}
		}

		List<KeyPoint> newKeypoints_ = frameDescriptor.getKeypoints().toList();
		List<KeyPoint> oldKeypoints_ = getKeypoints().toList();
		List<Point> goodNewKeypoints = new ArrayList<>();
		List<Point> goodOldKeypoints = new ArrayList<>();
		for (DMatch goodMatch : goodMatches) {
			goodNewKeypoints.add(newKeypoints_.get(goodMatch.trainIdx).pt);
			goodOldKeypoints.add(oldKeypoints_.get(goodMatch.queryIdx).pt);
		}

		if (goodMatches.size() > 30) {
			// Mat goodNewPoints = Converters.vector_Point2f_to_Mat(goodNewKeypoints);
			// MatOfPoint2f originalNewPoints = new MatOfPoint2f();
			// Core.perspectiveTransform(goodNewPoints, originalNewPoints, frameDescriptor.getHomography().inv());
			Mat result = Calib3d.findHomography(new MatOfPoint2f(goodOldKeypoints.stream().toArray(Point[]::new)), new MatOfPoint2f(goodNewKeypoints.stream().toArray(Point[]::new)), Calib3d.RANSAC, 1);
			if (result.size().empty()) {
				CamLiveRetriever.logger.warn("Stabilization homography is empty");
				return null;
			}
			if (!isValidHomography(result)) {
				CamLiveRetriever.logger.warn("Not a valid homography");
				return null;
			}
			return result;
		} else {
			CamLiveRetriever.logger.warn("Not enough matches ({})", goodMatches.size());
			return null;
		}
	}

	/*
	 * When the homography is applied to a rectangle (clockwise decomposition), the transformed points must be in the same order (clockwise)
	 */
	private boolean isValidHomography(Mat homography) {
		int w = deperspectivedImg.getSrc().width();
		int h = deperspectivedImg.getSrc().height();
		MatOfPoint2f original = new MatOfPoint2f(new Point[] { new Point(0, 0), new Point(w, 0), new Point(w, h), new Point(0, h) });
		MatOfPoint2f dst = new MatOfPoint2f();
		Core.perspectiveTransform(original, dst, homography);
		List<Point> targets = dst.toList();
		return isClockwise(targets.get(0), targets.get(1), targets.get(2));
	}

	private boolean isClockwise(Point a, Point b, Point c) {
		double areaSum = 0;
		areaSum += a.x * (b.y - c.y);
		areaSum += b.x * (c.y - a.y);
		areaSum += c.x * (a.y - b.y);
		return areaSum > 0;
	}

}