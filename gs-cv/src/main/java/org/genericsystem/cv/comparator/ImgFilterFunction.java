package org.genericsystem.cv.comparator;

/**
 * This enum provides a list of filters that will be applied to the Img before going through OCR.
 * 
 * @author Pierrik Lassalas
 */
public enum ImgFilterFunction {
	ORIGINAL("original", i -> i),
	REALITY("reality", i -> i),
	EQUALIZE_HISTO("equalizeHisto", i -> i.equalizeHisto()),
	EQUALIZE_HISTO_ADAPTATIVE("equalizeHistoAdaptative", i -> i.equalizeHistoAdaptative()),
	OTSU_AFTER_GAUSSIAN_BLUR("otsuAfterGaussianBlur", i -> i.otsuAfterGaussianBlur()),
	ADAPTATIVE_GAUSSIAN_THRESHOLD("adaptativeGaussianThreshold", i -> i.adaptativeGaussianThreshold());

	/**
	 * Name of the filter. This will be persisted.
	 */
	private String name;

	/**
	 * Lambda function that will be applied to an image to perform the corresponding treatment.
	 */
	private ImgFunction lambda;

	/**
	 * Default constructor.
	 * 
	 * @param name - the name of the filter, which will be stored in Generic System
	 * @param function - an {@link ImgFunction} that will be called to process the image with this filter
	 */
	private ImgFilterFunction(String name, ImgFunction function) {
		this.name = name;
		this.lambda = function;
	}

	public ImgFunction getLambda() {
		return this.lambda;
	}

	public String getName() {
		return this.name;
	}

}
