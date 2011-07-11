package com.jbrisbin.async.http.test

import org.glassfish.grizzly.Buffer
import org.glassfish.grizzly.filterchain.BaseFilter
import org.glassfish.grizzly.filterchain.FilterChainContext
import org.glassfish.grizzly.filterchain.NextAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
class EchoFilter extends BaseFilter {

	Logger log = LoggerFactory.getLogger(getClass())

	@Override NextAction handleWrite(FilterChainContext ctx) {
		Buffer buff = ctx.message
		log.debug("buffer: " + new String(buff.toBufferArray()))
		return super.handleWrite(ctx)
	}


}
