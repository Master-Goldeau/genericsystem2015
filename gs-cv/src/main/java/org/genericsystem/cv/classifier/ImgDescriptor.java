package org.genericsystem.cv.classifier;

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
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

public class ImgDescriptor {
	private final Img deperspectivedImg;
	private final MatOfKeyPoint keypoints;
	private final Mat descriptors;
	private final Mat homography;

	public ImgDescriptor(Mat frame, Mat deperspectivGraphy) {
		deperspectivedImg = CamLiveRetriever.warpPerspective(frame, deperspectivGraphy);
		keypoints = detect(deperspectivedImg);
		assert keypoints != null && !keypoints.empty();
		descriptors = new Mat();
		CamLiveRetriever.EXTRACTOR.compute(deperspectivedImg.getSrc(), keypoints, descriptors);
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
		Img closed = frame.adaptativeGaussianInvThreshold(17, 3).morphologyEx(Imgproc.MORPH_CLOSE, Imgproc.MORPH_ELLIPSE, new Size(5, 5));
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

	Mat computeStabilizationGraphy(ImgDescriptor frameDescriptor) {
		MatOfDMatch matches = new MatOfDMatch();

		CamLiveRetriever.MATCHER.match(getDescriptors(), frameDescriptor.getDescriptors(), matches);
		List<DMatch> goodMatches = new ArrayList<>();
		for (DMatch dMatch : matches.toArray()) {
			if (dMatch.distance <= 40) {
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
			Mat goodNewPoints = Converters.vector_Point2f_to_Mat(goodNewKeypoints);
			MatOfPoint2f originalNewPoints = new MatOfPoint2f();
			Core.perspectiveTransform(goodNewPoints, originalNewPoints, frameDescriptor.getHomography().inv());
			return Calib3d.findHomography(originalNewPoints, new MatOfPoint2f(goodOldKeypoints.stream().toArray(Point[]::new)), Calib3d.RANSAC, 5);
		} else {
			CamLiveRetriever.logger.warn("Not enough matches ({})", goodMatches.size());
			return null;
		}
	}

}