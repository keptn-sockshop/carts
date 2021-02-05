package works.weave.socks.cart.middleware;

import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import io.prometheus.client.Counter;

@Component
public class RequestCounterInterceptor implements HandlerInterceptor {

    private static final String REQ_PARAM_TIMING = "timing";

	private static final Counter requestTotal = Counter.build()
			.name("http_requests_total")
			.labelNames("method", "handler", "status")
			.help("Http Request Total").register();

    private static final Histogram responseTimeInMs = Histogram
            .build()
			      .exponentialBuckets(1.0, 1.5, 20)
            .name("http_response_time_milliseconds")
            .labelNames("method", "handler", "status")
            .help("Request completed time in milliseconds")
            .register();

    public static final Gauge responseTimeInMsGauge = Gauge.build()
			.name("http_response_time_milliseconds_gauge")
			.labelNames("method", "handler", "status")
			.help("Request completed time in milliseconds")
			.register();

	@Override
	public boolean preHandle(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse, final Object o) throws Exception {
        httpServletRequest.setAttribute(REQ_PARAM_TIMING, System.currentTimeMillis());
		return true;
	}

	@Override
	public void postHandle(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse, final Object o, final ModelAndView modelAndView) throws Exception {
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception e) throws Exception {
		String handlerLabel = handler.toString();
		if (handler instanceof HandlerMethod) {
			Method method = ((HandlerMethod) handler).getMethod();
			handlerLabel = method.getDeclaringClass().getSimpleName() + "." + method.getName();
		}
		requestTotal.labels(request.getMethod(), handlerLabel, Integer.toString(response.getStatus())).inc();

        Long timingAttr = (Long) request.getAttribute(REQ_PARAM_TIMING);
        long completedTime = System.currentTimeMillis() - timingAttr;
        responseTimeInMs.labels(request.getMethod(), handlerLabel, Integer.toString(response.getStatus())).observe(
                completedTime);


        // calculate average response time by getting the sum of observations and diding through the number of total requests
        double responseTimeAvg =
				responseTimeInMs.labels(request.getMethod(), handlerLabel, Integer.toString(response.getStatus())).get().sum /
						requestTotal.labels(request.getMethod(), handlerLabel, Integer.toString(response.getStatus())).get();

		responseTimeInMsGauge.labels(request.getMethod(), handlerLabel, Integer.toString(response.getStatus())).set(responseTimeAvg);
	}
}
