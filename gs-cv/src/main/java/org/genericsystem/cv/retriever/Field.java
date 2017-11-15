package org.genericsystem.cv.retriever;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.genericsystem.cv.Img;
import org.genericsystem.reinforcer.tools.GSRect;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

public class Field extends AbstractField {

	private Field parent;
	private Set<Field> children;

	private final Predicate<Field> outsideParent = field -> rect.getInsider(field.getRect()).map(r -> !r.equals(field.getRect())).orElse(true);
	private final Predicate<Field> overlapWithSiblings = field -> field.getSiblings().stream().filter(f -> !f.equals(field)).anyMatch(sibling -> field.getRect().isOverlapping(sibling.getRect()));

	private static final int LABELS_SIZE_THRESHOLD = 15;
	private static final double CONFIDENCE_THRESHOLD = 0.92;
	private boolean locked = false;

	public Field(GSRect rect) {
		super(rect);
		this.parent = null;
		this.children = new HashSet<>();
		checkConstraints();
	}

	public String recursiveToString() {
		StringBuffer sb = new StringBuffer();
		recursiveToString(this, sb, 0);
		sb.append("\n");
		return sb.toString();
	}

	private void recursiveToString(Field field, StringBuffer sb, int depth) {
		if (depth > 8)
			return;
		sb.append("depth: ").append(depth).append(": ");
		sb.append(field.getRect());
		if (field.isConsolidated())
			sb.append(" -> ").append(field.getConsolidated());
		if (!field.getChildren().isEmpty()) {
			depth++;
			for (Field child : field.getChildren()) {
				sb.append("\n");
				for (int i = 0; i < depth; ++i)
					sb.append("  ");
				recursiveToString(child, sb, depth);
			}
		}
	}

	@Override
	public void ocr(Img rootImg) {
		super.ocr(rootImg);
		if (attempts <= 3 || attempts % 5 == 0)
			consolidateOcr(false);
	}

	@Override
	public void resetDeadCounter() {
		super.resetDeadCounter();
		setFinal();
	}

	public void drawLockedField(Img display, Mat homography) {
		if (locked)
			drawRect(display, getRectPointsWithHomography(homography), new Scalar(255, 172, 0), 2);
	}

	public void drawTruncatedField(Img display, Mat homography) {
		if (truncated)
			drawRect(display, getRectPointsWithHomography(homography), new Scalar(0, 0, 51), 2);
	}

	public void setFinal() {
		if (!locked)
			if (getLabelsSize() > LABELS_SIZE_THRESHOLD && getConfidence() > CONFIDENCE_THRESHOLD && isOrphan())
				this.locked = true;
	}

	public boolean isLocked() {
		return locked;
	}

	public boolean addChildIfNotPresent(Field child) {
		return children.add(child);
	}

	public boolean removeChild(Field child) {
		return children.remove(child);
	}

	public boolean containsChild(Field field) {
		return children.stream().anyMatch(child -> child.getRect().inclusiveArea(field.getRect()) > 0.95);
	}

	public boolean addChildren(Collection<Field> children) {
		return this.children.addAll(children);
	}

	public void setChildren(Set<Field> children) {
		this.children = children;
	}

	public Set<Field> getChildren() {
		return children;
	}

	public Field getParent() {
		return parent;
	}

	public Set<Field> getSiblings() {
		if (isOrphan())
			return Collections.emptySet();
		return getParent().getChildren().stream().filter(child -> !this.equals(child)).collect(Collectors.toSet());
	}

	public void setParent(Field parent) {
		if (parent == null) {
			if (this.parent == null)
				return;
			logger.info("Resetting parent for field: {}", this.rect);
			this.parent.removeChild(this); // TODO can this method be called here, or should it be handled separately?
			this.parent = null;
		} else {
			if (!overlapWithSiblings.test(this)) {
				if (this.parent != null)
					logger.error("Child already has a parent:\n{}\nParent:\n{}", this, this.parent);
				this.parent = parent;
				if (parent.addChildIfNotPresent(this)) // TODO can this method be called here, or should it be handled separately?
					logger.info("Added {} as parent of {}", parent.getRect(), this.getRect());
				else {
					logger.error("Unable to add {} as a parent of {}, reverting", parent.getRect(), this.getRect());
					this.parent = null;
				}
			} else
				logger.error("New child overlaps with future siblings:\n{}\nSiblings:\n{}", this, this.getSiblings());
		}
	}

	public boolean hasChildren() {
		return children != null && !children.isEmpty();
	}

	public boolean hasSiblings() {
		return !getSiblings().isEmpty();
	}

	public boolean isOrphan() {
		return parent == null;
	}

	@Override
	void updateRect(GSRect rect) {
		super.updateRect(rect);
		checkConstraints();
	}

	private boolean checkConstraints() {
		boolean ok = isOrphan() ? this.checkConstraintsRecursive() : parent.checkConstraintsRecursive();
		if (!ok)
			logger.error("Invalid constraint for:\n{}Tree:\n{}", this, isOrphan() ? this.recursiveToString() : this.parent.recursiveToString());
		return ok;
	}

	private boolean checkConstraintsRecursive() {
		// If the field is orphan and has no children, it validates the constraints
		if (isOrphan() && !hasChildren())
			return true;
		// If the field has children, they must meet the constraint
		if (hasChildren()) {
			for (Field child : children) {
				if (outsideParent.test(child) && overlapWithSiblings.test(child))
					return false;
			}
			// If false was not returned, apply the function to the children
			for (Field child : children) {
				if (!child.checkConstraintsRecursive())
					return false;
			}
		}
		// At this stage, all the constraints should be verified
		return true;
	}

}