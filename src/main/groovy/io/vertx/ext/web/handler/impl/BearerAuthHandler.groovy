/*******************************************************************************
 * Copyright (C) 2018 Queensland Cyber Infrastructure Foundation (http://www.qcif.edu.au/)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 ******************************************************************************/
package io.vertx.ext.web.handler.impl

import io.vertx.core.AsyncResult
import io.vertx.core.logging.*
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.impl.AuthorizationAuthHandler
import io.vertx.core.Future;
/**
 * BearerAuthHandler
 *
 * @author <a target='_' href='https://github.com/shilob'>Shilo Banihit</a>
 *
 */
public class BearerAuthHandler extends AuthorizationAuthHandler {

	static logger =  LoggerFactory.getLogger(BearerAuthHandler.class);
	
	BearerAuthHandler(AuthProvider provider) {
		super(provider, AuthorizationAuthHandler.Type.BEARER);
	}
	
	@Override
	public void parseCredentials(RoutingContext context, Handler<AsyncResult<JsonObject>> handler) {
	  
	  this.parseAuthorization(context, false, { parseAuthorization ->
		if (parseAuthorization.failed()) {
		  handler.handle(Future.failedFuture(parseAuthorization.cause()));
		  return;
		}
		
		handler.handle(Future.succeededFuture(new JsonObject().put("token",  parseAuthorization.result())));
	  });
	}
	
	@Override
	protected String authenticateHeader(RoutingContext context) {
	  return "Bearer";
	}
}
