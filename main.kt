import java.io.BufferedReader
import java.net.ServerSocket

fun main() {
    HttpServer(8088).start()
}

class HttpServer(port: Int) {
    private val serverSocket = ServerSocket(port)

    fun start() {
        val router = Router()
        router.get("/") { _ ->
            HttpResponse(Version.HTTP_1_1, 200, "OK")
        }
        router.get("/hello") { _ ->
            HttpResponse(
                Version.HTTP_1_0,
                200,
                "OK",
                headers = mapOf("content-type" to "text/html"),
                body = "<h1>Hello World</h1>"
            )
        }
        router.handle(serverSocket)
    }
}

class Router {
    private val routes = mutableListOf<Route>()

    fun get(path: String, handler: RequestHandler) {
        routes.add(Route(Method.GET, path, handler))
    }

    fun handle(socket: ServerSocket) {
        while (true) {
            val client = socket.accept()
            val reader = client.getInputStream().bufferedReader()
            val writer = client.getOutputStream().bufferedWriter()
            val httpRequest = HttpRequest.parse(reader)
            routes.findLast { it.method == httpRequest.method && it.path == httpRequest.path }?.let {
                val str = it.handler.invoke(httpRequest).toString()
                writer.write(str)
                writer.flush()
            } ?: let {
                writer.write(
                    HttpResponse(
                        Version.HTTP_1_1,
                        404,
                        "Not Found",
                        headers = mapOf("content-type" to "text/html"),
                        body = "<h1>404 Not Found</h1>"
                    ).toString()
                )
                writer.flush()
            }
            client.close()
        }
    }
}

data class Route(
    val method: Method, val path: String, val handler: RequestHandler
)

typealias RequestHandler = (HttpRequest) -> HttpResponse

data class HttpRequest(
    val method: Method, val path: String, val version: Version, val headers: Map<String, String>, val body: String
) {
    companion object {
        fun parse(reader: BufferedReader): HttpRequest {
            var method = Method.GET
            var path = ""
            var version = Version.HTTP_1_0
            val headers = mutableMapOf<String, String>()
            var body = ""

            val request = StringBuilder()
            var readed: String?
            while (reader.readLine().also { readed = it } != null && readed!!.isNotEmpty()) {
                request.append(readed).append("\n")
            }

            request.lines().forEachIndexed() { index, line ->
                if (index == 0) {
                    val parts = line.split(" ")
                    method = Method.parse(parts[0])
                    path = parts[1]
                    version = Version.parse(parts[2])
                } else if (line.contains(": ")) {
                    val (key, value) = line.split(": ")
                    headers[key] = value
                } else if (line.isEmpty()) {
                    // do nothing
                } else {
                    body = line
                }
            }
            return HttpRequest(method, path, version, headers, body)
        }
    }
}

data class HttpResponse(
    val version: Version = Version.HTTP_1_0,
    val statusCode: Int,
    val statusText: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = ""
) {
    override fun toString(): String {
        return """
            $version $statusCode $statusText
            ${headers.map { "${it.key}: ${it.value}" }.joinToString("\n")}
            
            $body
        """.trimIndent()
    }
}

enum class Method {
    GET, POST, PUT, DELETE;

    companion object {
        fun parse(method: String): Method = when (method) {
            "GET" -> GET
            "POST" -> POST
            "PUT" -> PUT
            "DELETE" -> DELETE
            else -> throw IllegalArgumentException("Unknown method: $method")
        }
    }
}

enum class Version {
    HTTP_1_0, HTTP_1_1, HTTP_2;

    companion object {
        fun parse(version: String): Version = when (version) {
            "HTTP/1.0" -> HTTP_1_0
            "HTTP/1.1" -> HTTP_1_1
            "HTTP/2" -> HTTP_2
            else -> throw IllegalArgumentException("Unknown version: $version")
        }
    }

    override fun toString(): String {
        return when (this) {
            HTTP_1_0 -> "HTTP/1.0"
            HTTP_1_1 -> "HTTP/1.1"
            HTTP_2 -> "HTTP/2"
        }
    }
}