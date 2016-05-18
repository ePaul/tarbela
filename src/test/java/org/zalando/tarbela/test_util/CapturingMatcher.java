package org.zalando.tarbela.test_util;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * A matcher implementation which accepts all values of a type, and also stores the matched value for later retrieval.
 * This works similar as Mockito's {@link ArgumentCapturer}, but as a Hamcrest matcher instead of a Mockito matcher. In
 * order to use this class, you should create an anonymous subclass to fix the type argument (though named subclasses
 * work too). <code>
 * <pre>
    private CapturingMatcher<String> contentCapturer = new CapturingMatcher<String>() { };
 * </pre>
 * </code> Alternatively you can give the type argument as a class object to the constructor.
 *
 * @param  <T>  the type of object being matched.
 */
public abstract class CapturingMatcher<T> extends TypeSafeMatcher<T> {

    public CapturingMatcher() {
        super();
    }

    public CapturingMatcher(final Class<T> expectedType) {
        super(expectedType);
    }

    private T matched;

    /**
     * Returns the last matched value, or null if no value was matched.
     */
    public T getValue() {
        return matched;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("anything(capture)");
    }

    @Override
    protected boolean matchesSafely(final T item) {
        this.matched = item;
        return true;
    }
}
