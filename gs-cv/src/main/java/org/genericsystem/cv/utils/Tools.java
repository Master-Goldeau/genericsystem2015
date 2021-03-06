package org.genericsystem.cv.utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.genericsystem.cv.Img;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

public class Tools {

	public static Stream<Img> classImgsStream(String repository) {
		return Arrays.stream(new File(repository).listFiles()).filter(img -> img.getName().endsWith(".png")).map(img -> new Img(img.getPath()));
	}

	public static Img firstImg(String repository) {
		return classImgsStream(repository).findFirst().get();
	}

	public static Stream<Img> classImgsStream(String repository, String... imagePaths) {
		return Arrays.stream(new File(repository).listFiles()).filter(img -> img.getName().endsWith(".png") && Arrays.asList(imagePaths).contains(img.getName())).map(img -> new Img(img.getPath()));
	}

	public static BufferedImage mat2bufferedImage(Mat image) {
		MatOfByte bytemat = new MatOfByte();
		Imgcodecs.imencode(".bmp", image, bytemat);
		try {
			return ImageIO.read(new ByteArrayInputStream(bytemat.toArray()));
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static Image mat2jfxImage(Mat frame) {
		return SwingFXUtils.toFXImage(Tools.mat2bufferedImage(frame), null);
	}

}
