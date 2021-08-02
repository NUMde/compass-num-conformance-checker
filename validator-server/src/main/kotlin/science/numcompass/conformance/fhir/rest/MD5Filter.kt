package science.numcompass.conformance.fhir.rest

import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.util.DigestUtils
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

const val MD5_REQUEST_ATTRIBUTE = "md5"

/**
 * This filter adds a MD5 hash value as request attribute to be injected in the REST controllers.
 */
@Order(0)
@Component
class MD5Filter : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val cachedRequest = ContentCachingRequestWrapper(request).apply {
            setAttribute(MD5_REQUEST_ATTRIBUTE, DigestUtils.md5DigestAsHex(contentAsByteArray))
        }
        chain.doFilter(cachedRequest, response)
    }
}
