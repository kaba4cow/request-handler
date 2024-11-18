package com.kaba4cow.requesthandler;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.kaba4cow.requesthandler.annotations.ControllerParam;
import com.kaba4cow.requesthandler.annotations.RequestMapping;
import com.kaba4cow.requesthandler.annotations.RequestParam;

/**
 * Handles the registration and execution of controller methods mapped to request paths. This class supports both synchronous
 * and asynchronous request handling.
 */
public class RequestHandler {

	private static final RequestHandlerThreadFactory threadFactory = new RequestHandlerThreadFactory();

	private final Map<String, MethodHandler> handlers;
	private final ExecutorService executor;

	/**
	 * Constructs a new {@code RequestHandler}.
	 */
	public RequestHandler() {
		this.handlers = new ConcurrentHashMap<>();
		this.executor = Executors.newCachedThreadPool(threadFactory);
	}

	/**
	 * Registers all methods of the specified controller that are annotated with {@code @RequestMapping}.
	 * 
	 * @param controller the controller instance containing request-handling methods
	 * 
	 * @throws NullPointerException if {@code controller} is null
	 */
	public void registerController(Object controller) {
		Objects.requireNonNull(controller);
		for (Method method : controller.getClass().getDeclaredMethods())
			if (method.isAnnotationPresent(RequestMapping.class)) {
				MethodHandler handler = new MethodHandler(controller, method, method.getAnnotation(RequestMapping.class));
				handlers.put(handler.path, handler);
			}
	}

	/**
	 * Handles a request and returns the result.
	 * 
	 * @param request the request path
	 * 
	 * @return the result of the request
	 * 
	 * @throws NullPointerException    if {@code request} is null
	 * @throws RequestHandlerException if no matching handler is found or an error occurs during method invocation
	 */
	public Object handleRequest(String request) {
		Objects.requireNonNull(request);
		return handleRequest(request, new HashMap<>());
	}

	/**
	 * Handles a request with additional controller parameters and returns the result.
	 * 
	 * @param request              the request path
	 * @param controllerParameters additional parameters to be passed to the controller method
	 * 
	 * @return the result of the request
	 * 
	 * @throws NullPointerException    if {@code request} or {@code controllerParameters} is null
	 * @throws RequestHandlerException if no matching handler is found or an error occurs during method invocation
	 */
	public Object handleRequest(String request, Map<String, Object> controllerParameters) {
		Objects.requireNonNull(request);
		Objects.requireNonNull(controllerParameters);
		String handlerPath = "";
		String[] pathParts = request.split("/");
		Map<String, String> pathParameters = new HashMap<>();
		for (int i = 0; i < pathParts.length; i++)
			if (pathParts[i].contains("=")) {
				String[] argParts = pathParts[i].split("=");
				if (argParts.length == 2)
					pathParameters.put(argParts[0], argParts[1]);
			} else
				handlerPath += pathParts[i] + "/";
		if (handlers.containsKey(handlerPath))
			try {
				return handlers.get(handlerPath).handle(controllerParameters, pathParameters);
			} catch (Exception exception) {
				throw new RequestHandlerException(exception, "Controller of class %s could not handle request %s",
						handlers.get(handlerPath).instance.getClass().getName(), request);
			}
		else
			throw new RequestHandlerException("No controller found for path %s", handlerPath);
	}

	/**
	 * Handles a request asynchronously and passes the result or error to the specified consumers.
	 * 
	 * @param request the request path
	 * @param result  the consumer to handle the result
	 * @param error   the consumer to handle errors
	 * 
	 * @throws NullPointerException if {@code request} or {@code result} is null
	 */
	public void handleAsyncRequest(String request, Consumer<Object> result, Consumer<Throwable> error) {
		handleAsyncRequest(request, new HashMap<>(), result, error);
	}

	/**
	 * Handles a request asynchronously with additional controller parameters and passes the result or error to the specified
	 * consumers.
	 * 
	 * @param request              the request path
	 * @param controllerParameters additional parameters to be passed to the controller method
	 * @param result               the consumer to handle the result
	 * @param error                the consumer to handle errors
	 * 
	 * @throws NullPointerException if {@code request}, {@code controllerParameters}, or {@code result} is null
	 */
	public void handleAsyncRequest(String request, Map<String, Object> controllerParameters, Consumer<Object> result,
			Consumer<Throwable> error) {
		Objects.requireNonNull(request);
		Objects.requireNonNull(controllerParameters);
		Objects.requireNonNull(result);
		executor.submit(() -> {
			try {
				result.accept(handleRequest(request, controllerParameters));
			} catch (Throwable exception) {
				if (Objects.nonNull(error))
					error.accept(exception);
			}
		});
	}

	/**
	 * Initiates an orderly shutdown of the {@code RequestHandler}'s internal {@code ExecutorService}.
	 * <p>
	 * This method prevents the executor from accepting new tasks but allows previously submitted tasks to complete. After
	 * calling this method, new asynchronous requests will be rejected.
	 * <p>
	 * If the executor does not terminate within the specified timeout, it will forcefully shut down any currently executing
	 * tasks. Additionally, if the current thread is interrupted while waiting, the shutdown process will be interrupted, and
	 * any remaining tasks will be terminated.
	 */
	public void shutdown() {
		executor.shutdown();
		try {
			if (!executor.awaitTermination(1L, TimeUnit.SECONDS))
				executor.shutdownNow();
		} catch (InterruptedException e) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	private static class MethodHandler {

		private final Object instance;
		private final Method method;
		private String path;

		private MethodHandler(Object instance, Method method, RequestMapping annotation) {
			this.instance = instance;
			this.method = method;
			this.path = annotation.path();
			if (!this.path.endsWith("/"))
				this.path += "/";
			if (instance.getClass().isAnnotationPresent(RequestMapping.class)) {
				String parentPath = instance.getClass().getAnnotation(RequestMapping.class).path();
				if (!parentPath.endsWith("/"))
					parentPath += "/";
				this.path = parentPath + this.path;
			}
			if (!this.path.endsWith("/"))
				this.path += "/";
		}

		private Object handle(Map<String, Object> controllerParameters, Map<String, String> pathParameters) {
			Parameter[] parameters = method.getParameters();
			List<Object> arguments = new ArrayList<>();
			for (String name : controllerParameters.keySet())
				if (pathParameters.containsKey(name))
					throw new RequestHandlerException("Duplicate parameter %s", name);
			for (int i = 0; i < parameters.length; i++) {
				Parameter parameter = parameters[i];
				String name = parameter.getName();
				if (parameter.isAnnotationPresent(RequestParam.class)) {
					RequestParam annotation = parameter.getAnnotation(RequestParam.class);
					if (pathParameters.containsKey(name))
						arguments.add(parseParameter(parameter.getType(), pathParameters.get(name)));
					else if (annotation.required())
						throw new RequestHandlerException("No value provided for path parameter %s", parameter.getName());
					else
						arguments.add(parseParameter(parameter.getType(), annotation.defaultValue()));
				} else if (parameter.isAnnotationPresent(ControllerParam.class)) {
					ControllerParam annotation = parameter.getAnnotation(ControllerParam.class);
					if (controllerParameters.containsKey(name))
						arguments.add(controllerParameters.get(name));
					else if (annotation.required())
						throw new RequestHandlerException("No value provided for controller parameter %s", parameter.getName());
					else
						arguments.add(parseParameter(parameter.getType(), annotation.defaultValue()));
				} else
					arguments.add(null);
			}
			try {
				return method.invoke(instance, arguments.toArray());
			} catch (Exception exception) {
				throw new RequestHandlerException(exception, "Could not invoke controller method %s with args %s", method,
						arguments);
			}
		}

		private Object parseParameter(Class<?> type, String value) {
			if (type.isEnum())
				try {
					for (Object constant : type.getEnumConstants())
						if (Objects.equals(((Enum<?>) constant).ordinal(), Integer.parseInt(value)))
							return constant;
				} catch (NumberFormatException exception) {
					for (Object constant : type.getEnumConstants())
						if (Objects.equals(constant.toString(), value))
							return constant;
					throw new RequestHandlerException("No enum of type %s found for value %s", type.getName(), value);
				}
			else if (type == int.class || type == Integer.class)
				return Integer.parseInt(value);
			if (type == long.class || type == Long.class)
				return Long.parseLong(value);
			if (type == float.class || type == Float.class)
				return Float.parseFloat(value);
			if (type == double.class || type == Double.class)
				return Double.parseDouble(value);
			else if (type == boolean.class || type == Boolean.class)
				return Boolean.parseBoolean(value);
			else if (type == String.class)
				return value;
			else
				throw new RequestHandlerException("Unsupported parameter type %s", type.getName());
		}

	}

	private static class RequestHandlerThreadFactory implements ThreadFactory {

		private int counter;

		private RequestHandlerThreadFactory() {
			this.counter = 0;
		}

		@Override
		public Thread newThread(Runnable runnable) {
			return new Thread(runnable, String.format("RequestHandlerThread-%s", counter++));
		}

	}

	/**
	 * Thrown to indicate an error occured during the request handling in {@code RequestHandler}.
	 */
	public static class RequestHandlerException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		private RequestHandlerException(String format, Object... args) {
			super(String.format(format, args));
		}

		private RequestHandlerException(Throwable cause, String format, Object... args) {
			super(String.format(format, args), cause);
		}

	}

}
