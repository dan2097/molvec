package gov.nih.ncats.molvec;

import java.awt.geom.Rectangle2D;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

class ErrorResult implements MolvecResult{

    private final Throwable t;

    public ErrorResult(Throwable t) {
        this.t = Objects.requireNonNull(t);
    }

    @Override
    public Optional<String> getMolfile() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getSDfile() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getSDfile(Map<String, String> properties) {
        return Optional.empty();
    }

    @Override
    public Optional<Rectangle2D> getOriginalBoundingBox() {
        return Optional.empty();
    }

    @Override
    public boolean hasError() {
        return true;
    }

    @Override
    public Optional<Throwable> getError() {
        return Optional.of(t);
    }

	@Override
	public Optional<Map<String, String>> getProperties() {
		
		return Optional.empty();
	}
}
