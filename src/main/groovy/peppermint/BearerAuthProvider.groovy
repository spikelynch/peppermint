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
package peppermint

import io.vertx.core.logging.*
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User
import io.vertx.core.Future;
/**
 * BearerAuthProvider
 *
 * @author <a target='_' href='https://github.com/shilob'>Shilo Banihit</a>
 *
 */
class BearerAuthProvider implements AuthProvider {

	/* (non-Javadoc)
	 * @see io.vertx.ext.auth.AuthProvider#authenticate(io.vertx.core.json.JsonObject, io.vertx.core.Handler)
	 */
	static logger =  LoggerFactory.getLogger(BearerAuthProvider.class);
	
	protected String token;
	
	BearerAuthProvider(token) {
		this.token = token;
	}
	
	@Override
	public void authenticate(JsonObject authInfo, Handler<AsyncResult<User>> resultHandler) {
		if (authInfo.getValue('token') == this.token){
			resultHandler.handle(Future.succeededFuture(new BearerAuthUser(authInfo.getValue('token'))));
		} else {
			resultHandler.handle(Future.failedFuture("Invalid Token"));
		}
	}

}
