--- The http library allows communicating with web servers, sending and
-- receiving data from them.
--
-- #### `http_check` event
--
-- @module http

--- Asynchronously make a HTTP request to the given url.
--
-- This returns immediately, a [`http_success`](#http-success-event) or
-- [`http_failure`](#http-failure-event) will be queued once the request has
-- completed.
--
-- @tparam      string url   The url to request
-- @tparam[opt] string body  An optional string containing the body of the
-- request. If specified, a `POST` request will be made instead.
-- @tparam[opt] { [string] = string } headers Additional headers to send as part
-- of this request.
-- @tparam[opt] boolean binary Whether to make a binary HTTP request. If true,
-- the body will not be UTF-8 encoded, and the received response will not be
-- decoded.
--
-- @tparam[2] {
--   url = string, body? = string, headers? = { [string] = string },
--   binary? = boolean, method? = string, redirect? = boolean,
-- } request Options for the request.
--
-- This table form is an expanded version of the previous syntax. All arguments
-- from above are passed in as fields instead (for instance,
-- `http.request("https://example.com")` becomes `http.request { url =
-- "https://example.com" }`).
--
-- This table also accepts several additional options:
--
--  - `method`: Which HTTP method to use, for instance `"PATCH"` or `"DELETE"`.
--  - `redirect`: Whether to follow HTTP redirects. Defaults to true.
--
-- @see http.get  For a synchronous way to make GET requests.
-- @see http.post For a synchronous way to make POST requests.
function request(...) end

--- Make a HTTP GET request to the given url.
--
-- @tparam string url   The url to request
-- @tparam[opt] { [string] = string } headers Additional headers to send as part
-- of this request.
-- @tparam[opt] boolean binary Whether to make a binary HTTP request. If true,
-- the body will not be UTF-8 encoded, and the received response will not be
-- decoded.
--
-- @tparam[2] {
--   url = string, headers? = { [string] = string },
--   binary? = boolean, method? = string, redirect? = boolean,
-- } request Options for the request. See @{http.request} for details on how
-- these options behave.
--
-- @treturn Response The resulting http response, which can be read from.
-- @treturn[2] nil When the http request failed, such as in the event of a 404
-- error or connection timeout.
-- @treturn string A message detailing why the request failed.
-- @treturn Response|nil The failing http response, if available.
--
-- @usage Make a request to [example.computercraft.cc](https://example.computercraft.cc),
-- and print the returned page.
-- ```lua
-- local request = http.get("https://example.computercraft.cc")
-- print(request.readAll())
-- -- => HTTP is working!
-- request.close()
-- ```
function get(...) end

--- Make a HTTP POST request to the given url.
--
-- @tparam string url   The url to request
-- @tparam string body  The body of the POST request.
-- @tparam[opt] { [string] = string } headers Additional headers to send as part
-- of this request.
-- @tparam[opt] boolean binary Whether to make a binary HTTP request. If true,
-- the body will not be UTF-8 encoded, and the received response will not be
-- decoded.
--
-- @tparam[2] {
--   url = string, body? = string, headers? = { [string] = string },
--   binary? = boolean, method? = string, redirect? = boolean,
-- } request Options for the request. See @{http.request} for details on how
-- these options behave.
--
-- @treturn Response The resulting http response, which can be read from.
-- @treturn[2] nil When the http request failed, such as in the event of a 404
-- error or connection timeout.
-- @treturn string A message detailing why the request failed.
-- @treturn Response|nil The failing http response, if available.
function post(...) end

--- A http response. This acts very much like a @{fs.ReadHandle|file}, though
-- provides some http specific methods.
--
-- #### `http_success` event
-- #### `http_failure` event
--
-- @type Response
-- @see http.request On how to make a http request.
local Response = {}

--- Returns the response code and response message returned by the server
--
-- @treturn number The response code (i.e. 200)
-- @treturn string The response message (i.e. "OK")
function Response.getResponseCode() end

--- Get a table containing the response's headers, in a format similar to that
-- required by @{http.request}. If multiple headers are sent with the same
-- name, they will be combined with a comma.
--
-- @treturn { [string]=string } The response's headers.
-- Make a request to [example.computercraft.cc](https://example.computercraft.cc),
-- and print the returned headers.
-- ```lua
-- local request = http.get("https://example.computercraft.cc")
-- print(textutils.serialize(request.getResponseHeaders()))
-- -- => {
-- --   [ "Content-Type" ] = "text/plain; charset=utf8",
-- --   [ "content-length" ] = 17,
-- --   ...
-- -- }
-- request.close()
-- ```
function Response.getResponseHeaders() end

function Response.read(count) end
function Response.readAll() end
function Response.readLine(with_trailing) end
function Response.seek(whence, offset) end
function Response.close() end

--- Asynchronously determine whether a URL can be requested.
--
-- If this returns `true`, one should also listen for [`http_check`
-- events](#http-check-event) which will container further information about
-- whether the URL is allowed or not.
--
-- @tparam string url The URL to check.
-- @treturn true When this url is not invalid. This does not imply that it is
-- allowed - see the comment above.
-- @treturn[2] false When this url is invalid.
-- @treturn string A reason why this URL is not valid (for instance, if it is
-- malformed, or blocked).
--
-- @see http.checkURL For a synchronous version.
function checkURLAsync(url) end

--- Determine whether a URL can be requested.
--
-- If this returns `true`, one should also listen for [`http_check`
-- events](#http-check-event) which will container further information about
-- whether the URL is allowed or not.
--
-- @tparam string url The URL to check.
-- @treturn true When this url is valid and can be requested via @{http.request}.
-- @treturn[2] false When this url is invalid.
-- @treturn string A reason why this URL is not valid (for instance, if it is
-- malformed, or blocked).
--
-- @see http.checkURLAsync For an asynchronous version.
--
-- @usage
-- ```lua
-- print(http.checkURL("https://example.computercraft.cc/"))
-- -- => true
-- print(http.checkURL("http://localhost/"))
-- -- => false Domain not permitted
-- print(http.checkURL("not a url"))
-- -- => false URL malformed
-- ```
function checkURL(url) end

--- Open a websocket.
--
-- @tparam string url The websocket url to connect to. This should have the
-- `ws://` or `wss://` protocol.
-- @tparam[opt] { [string] = string } headers Additional headers to send as part
-- of the initial websocket connection.
--
-- @treturn Websocket The websocket connection.
-- @treturn[2] false If the websocket connection failed.
-- @treturn string An error message describing why the connection failed.
function websocket(url, headers) end

--- Asynchronously open a websocket.
--
-- This returns immediately, a [`websocket_success`](#websocket-success-event)
-- or [`websocket_failure`](#websocket-failure-event) will be queued once the
-- request has completed.
--
-- @tparam string url The websocket url to connect to. This should have the
-- `ws://` or `wss://` protocol.
-- @tparam[opt] { [string] = string } headers Additional headers to send as part
-- of the initial websocket connection.
function websocketAsync(url, headers) end
