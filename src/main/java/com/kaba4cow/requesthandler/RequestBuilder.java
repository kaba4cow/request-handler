package com.kaba4cow.requesthandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Constructs a request path with optional parameters.
 */
public class RequestBuilder {

	private final List<String> paths;
	private final Map<String, Object> parameters;

	/**
	 * Constructs a new {@code RequestBuilder}.
	 */
	public RequestBuilder() {
		this.paths = new ArrayList<>();
		this.parameters = new ConcurrentHashMap<>();
	}

	private RequestBuilder(RequestBuilder builder) {
		this.paths = new ArrayList<>(builder.paths);
		this.parameters = new ConcurrentHashMap<>(builder.parameters);
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
		this.paths.add(path);
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
		StringBuilder builder = new StringBuilder();
		for (String path : paths)
			builder.append(path).append("/");
		for (Map.Entry<String, Object> parameter : parameters.entrySet())
			if (Objects.nonNull(parameter.getValue()))
				builder.append(parameter.getKey()).append("=").append(parameter.getValue()).append("/");
		return builder.toString();
	}

	/**
	 * Creates a copy of this {@code RequestBuilder}.
	 * 
	 * @return the {@code RequestBuilder} copy
	 */
	public RequestBuilder copy() {
		return new RequestBuilder(this);
	}

	/**
	 * Returns a string representation of this request builder.
	 * 
	 * @return a string representation of this request builder
	 */
	@Override
	public String toString() {
		return String.format("RequestBuilder [paths=%s, parameters=%s]", paths, parameters);
	}

}
