package org.genericsystem.cv.application;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class GSVideoCapture implements GSCapture {

	private final VideoCapture videoCapture;
	private final double f;

	private final Size size;
	private final Size resize;

	GSVideoCapture(String url, double f, Size size, Size resize) {
		this.f = f;
		this.size = size;
		this.resize = resize;
		videoCapture = new VideoCapture(url);
		videoCapture.set(Videoio.CAP_PROP_FRAME_WIDTH, size.width);
		videoCapture.set(Videoio.CAP_PROP_FRAME_HEIGHT, size.height);
	}

	GSVideoCapture(int index, double f, Size size, Size resize) {
		this.f = f;
		this.size = size;
		this.resize = resize;
		videoCapture = new VideoCapture(index);
		videoCapture.set(Videoio.CAP_PROP_FRAME_WIDTH, size.width);
		videoCapture.set(Videoio.CAP_PROP_FRAME_HEIGHT, size.height);
	}

	@Override
	public Size getResize() {
		return resize;
	}

	@Override
	public SuperFrameImg read() {
		Mat frameMat = new Mat();
		boolean result = videoCapture.read(frameMat);
		if (!result)
			throw new IllegalStateException("Unable to read camera");
		Imgproc.resize(frameMat, frameMat, resize, 1, 1, Imgproc.INTER_LINEAR);
		return new SuperFrameImg(frameMat, new double[] { frameMat.width() / 2, frameMat.height() / 2 }, f);
	}

	@Override
	public void release() {
		videoCapture.release();
	}

}