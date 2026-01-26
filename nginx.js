/**
 *
 *
 * // 从命令行参数读取端口和目录
 * // node nginx.js 3000 /www
 * // node nginx.js 3000 c:/cfg
 *
 *
 * @type {{request: {(options: (RequestOptions | string | URL), callback?: (res: IncomingMessage) => void): ClientRequest, (url: (string | URL), options: RequestOptions, callback?: (res: IncomingMessage) => void): ClientRequest}, OutgoingMessage: OutgoingMessage, Server: Server, RequestOptions: RequestOptions, MessageEvent: any, globalAgent: Agent, maxHeaderSize: number, setMaxIdleHTTPParsers(max: number): void, IncomingMessage: IncomingMessage, validateHeaderValue(name: string, value: string): void, ClientRequestArgs: ClientRequestArgs, get: {(options: (RequestOptions | string | URL), callback?: (res: IncomingMessage) => void): ClientRequest, (url: (string | URL), options: RequestOptions, callback?: (res: IncomingMessage) => void): ClientRequest}, OutgoingMessageEventMap: OutgoingMessageEventMap, ProxyEnv: ProxyEnv, ClientRequest: ClientRequest, ServerResponse: ServerResponse, CloseEvent: any, STATUS_CODES: {[p: number]: string | undefined, [p: string]: string | undefined}, Agent: Agent, RequestListener: (request: InstanceType<Request>, response: (InstanceType<Response> & {req: InstanceType<Request>})) => void, IncomingMessageEventMap: IncomingMessageEventMap, ServerEventMap: ServerEventMap, IncomingHttpHeaders: IncomingHttpHeaders, createServer: {<Request=typeof IncomingMessage extends typeof IncomingMessage, Response=typeof ServerResponse extends typeof ServerResponse>(requestListener?: RequestListener<Request, Response>): Server<Request, Response>, <Request=typeof IncomingMessage extends typeof IncomingMessage, Response=typeof ServerResponse extends typeof ServerResponse>(options: ServerOptions<Request, Response>, requestListener?: RequestListener<Request, Response>): Server<Request, Response>}, InformationEvent: InformationEvent, ClientRequestEventMap: ClientRequestEventMap, METHODS: string[], WebSocket: any, OutgoingHttpHeader: number | string | string[], OutgoingHttpHeaders: OutgoingHttpHeaders, validateHeaderName(name: string): void, ServerOptions: ServerOptions, AgentOptions: AgentOptions}}
 */
/**
 * 一、跑 Vue 静态站点的【最低必需清单】
 * 1️⃣ MIME（你已经在做了 👍）
 * 2️⃣ SPA fallback（超级关键）
 * 3️⃣ 正确处理 path / 安全性（别被穿了）
 * 4️⃣ gzip / brotli（不做也能跑，但慢）
 * 5️⃣ Cache-Control
 * @type {{request: {(options: (RequestOptions | string | URL), callback?: (res: IncomingMessage) => void): ClientRequest, (url: (string | URL), options: RequestOptions, callback?: (res: IncomingMessage) => void): ClientRequest}, OutgoingMessage: OutgoingMessage, Server: Server, RequestOptions: RequestOptions, MessageEvent: any, globalAgent: Agent, maxHeaderSize: number, setMaxIdleHTTPParsers(max: number): void, IncomingMessage: IncomingMessage, validateHeaderValue(name: string, value: string): void, ClientRequestArgs: ClientRequestArgs, get: {(options: (RequestOptions | string | URL), callback?: (res: IncomingMessage) => void): ClientRequest, (url: (string | URL), options: RequestOptions, callback?: (res: IncomingMessage) => void): ClientRequest}, OutgoingMessageEventMap: OutgoingMessageEventMap, ProxyEnv: ProxyEnv, ClientRequest: ClientRequest, ServerResponse: ServerResponse, CloseEvent: any, STATUS_CODES: {[p: number]: string | undefined, [p: string]: string | undefined}, Agent: Agent, RequestListener: (request: InstanceType<Request>, response: (InstanceType<Response> & {req: InstanceType<Request>})) => void, IncomingMessageEventMap: IncomingMessageEventMap, ServerEventMap: ServerEventMap, IncomingHttpHeaders: IncomingHttpHeaders, createServer: {<Request=typeof IncomingMessage extends typeof IncomingMessage, Response=typeof ServerResponse extends typeof ServerResponse>(requestListener?: RequestListener<Request, Response>): Server<Request, Response>, <Request=typeof IncomingMessage extends typeof IncomingMessage, Response=typeof ServerResponse extends typeof ServerResponse>(options: ServerOptions<Request, Response>, requestListener?: RequestListener<Request, Response>): Server<Request, Response>}, InformationEvent: InformationEvent, ClientRequestEventMap: ClientRequestEventMap, METHODS: string[], WebSocket: any, OutgoingHttpHeader: number | string | string[], OutgoingHttpHeaders: OutgoingHttpHeaders, validateHeaderName(name: string): void, ServerOptions: ServerOptions, AgentOptions: AgentOptions}}
 */

const http = require('http');
const fs = require('fs');
const path = require('path');

// 常见 MIME 类型映射表
const mimeTypes = {

    '.html': 'text/html; charset=utf-8',
    '.htm': 'text/html; charset=utf-8',
    '.css': 'text/css; charset=utf-8',
    '.js': 'application/javascript; charset=utf-8',
    '.json': 'application/json; charset=utf-8',
    '.txt': 'text/plain; charset=utf-8',



    // 图片
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.jpeg': 'image/jpeg',
    '.gif': 'image/gif',
    '.svg': 'image/svg+xml',

    // 字体
    '.woff': 'font/woff',
    '.woff2': 'font/woff2',
    '.ttf': 'font/ttf',

    // 其他
    '.ico': 'image/x-icon',
    '.pdf': 'application/pdf'





};
/**
 * 1. MIME 类型支持
 * 你的服务器根据文件类型设置正确的 Content-Type，例如：
 *
 * .js
 *
 * .css   .jpg  .png  .jpeg
 *
 * .html
 *
 * .json
 */

function startServer(port, wwwdir2) {

    var wwwdir  = path.resolve(wwwdir2); //同一修正输入的反斜杠写法
    const server = http.createServer((req, res) => {




        // 解析请求路径   默认 index.html
        let webpath = req.url === '/' ? 'index.html' : req.url;
        let filePath = path.join(wwwdir, webpath);

        //================safe  path traversal 防护意识

        // decode URL 并构造真实路径
        var resolved = path.resolve(filePath);

        if (!resolved.startsWith(wwwdir)) {
            res.writeHead(403);
            return res.end('Forbidden');
        }


        // 获取扩展名
        var ext = path.extname(filePath).toLowerCase();
        var contentType = mimeTypes[ext] || 'application/octet-stream';







        // 判断文件是否存在
        fs.stat(filePath, (err, stats) => {

            //if err ,file not exist ne ...   afdaf.htm
            //  2️⃣ SPA fallback（超级关键）
            if (err || !stats.isFile()) {
                filePath = path.join(wwwdir, 'index.html');
                stats = fs.statSync(filePath);
            } else {
                filePath = resolved;
            }
            ext = path.extname(filePath).toLowerCase();
            contentType = mimeTypes[ext] || 'application/octet-stream';
            // 缓存策略（必须在 fallback 后）
            if (filePath.endsWith('index.html')) {
                res.setHeader('Cache-Control', 'no-cache');
            } else {
                res.setHeader('Cache-Control', 'public, max-age=31536000, immutable');
            }

            // HEAD 请求
            if (req.method === 'HEAD') {
                res.writeHead(200, {
                    'Content-Type': contentType,
                    'Content-Length': stats.size
                });
                return res.end();
            }

            // 读取文件并返回
            // 正常读取文件
            res.writeHead(200, { 'Content-Type': contentType });
            const raw = fs.createReadStream(filePath);

            raw.on('error', () => {
                if (!res.headersSent) {
                    res.writeHead(500, { 'Content-Type': 'text/plain' });
                    res.end('500 Server Error');
                } else {
                    res.destroy();
                }
            });

            raw.pipe(res);


        });
    });

    server.listen(port, () => {
        console.log(`Server running at http://localhost:${port}/`);
        console.log(`Serving directory: ${wwwdir}`);
    });
}

//module.exports = startServer;



const port = process.argv[2];
const dir = process.argv[3];

//startServer(3000, 'c:/cfg');
startServer(port,dir );


/**
 * 四、最低能跑 Vue 的 checklist（直接抄）
 *
 * ✅ MIME
 * ✅ 静态文件读取（stream）
 * ✅ SPA fallback → index.html
 * ✅ gzip
 * ✅ Cache-Control
 * ✅ path traversal 防护
 *
 * 👉 做到这 6 个，Vue app 稳稳跑
 */
//todo gzip
