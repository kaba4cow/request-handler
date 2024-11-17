package com.kaba4cow.requesthandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Constructs a request path with optional parameters.
 */
public class RequestBuilder {

	private final StringBuilder path;
	private final Map<String, Object> parameters;

	/**
	 * Constructs a new {@code RequestBuilder}.
	 */
	public RequestBuilder() {
		this.path = new StringBuilder();
		this.parameters = new HashMap<>();
	}

	/**
	 * Appends a path segment to the request.
	 * 
	 * @param path the path segment to append
	 * 
	 * @return this builder instance
	 * 
	 * @throws NullPointerException if {@code path} is null
	 */
	public RequestBuilder path(String path) {
		Objects.requireNonNull(path);
		if (this.path.length() > 0)
			this.path.append("/");
		this.path.append(path);
		return this;
	}

	/**
	 * Adds a parameter to the request.
	 * 
	 * @param name  the name of the parameter
	 * @param value the value of the parameter
	 * 
	 * @return this builder instance
	 */
	public RequestBuilder parameter(String name, Object value) {
		parameters.put(name, value);
		return this;
	}

	/**
	 * Builds and returns the complete request string.
	 * 
	 * @return the constructed request string
	 */
	public String build() {
		StringBuilder builder = new StringBuilder(path);
		for (Map.Entry<String, Object> parameter : parameters.entrySet())
			if (Objects.nonNull(parameter.getValue()))
				builder.append("/").append(parameter.getKey()).append("=").append(parameter.getValue());
		return builder.toString();
	}

	/**
	 * Returns a string representation of this request builder.
	 * 
	 * @return a string representation of this request builder
	 */
	@Override
	public String toString() {
		return String.format("RequestBuilder [path=%s, parameters=%s]", path, parameters);
	}

}
