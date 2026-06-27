const {createProxyMiddleware} = require('http-proxy-middleware');

module.exports = function configureProxy(app) {
    app.use(
        '/api',
        createProxyMiddleware({
            target: process.env.REACT_APP_GATEWAY_URL || 'http://localhost:18080',
            changeOrigin: true,
        })
    );
};
