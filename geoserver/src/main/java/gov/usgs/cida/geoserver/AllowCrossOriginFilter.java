package gov.usgs.cida.geoserver;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author tkunicki
 */
public class AllowCrossOriginFilter implements Filter {
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // nothing to do yet...
    }
    
    @Override
    public void destroy() {
        // nothing to do yet...
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (response instanceof HttpServletResponse) {
            ((HttpServletResponse)response).setHeader("Access-Control-Allow-Origin", "*");
        }
        chain.doFilter(request, response);
    }
}
